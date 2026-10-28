import Cocoa
import Darwin
import Foundation

private var flowIpcMainWindowCloseRecoveryAcceptance:
    FlowIpcMainWindowCloseRecoveryAcceptance?

extension WebViewHost {
    func scheduleMainWindowCloseRecoveryAcceptanceIfReady() {
        guard config.flowIpcAutoMainWindowCloseRecovery,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcMainWindowCloseRecoveryAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 75
            flowIpcMainWindowCloseRecoveryAcceptance =
                FlowIpcMainWindowCloseRecoveryAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcMainWindowCloseRecoveryAcceptance?.resume()
    }
}

private final class FlowIpcMainWindowCloseRecoveryAcceptance {
    private enum Phase: String {
        case root
        case route
        case secondaryReady
        case closeMainInvoked
        case waitingMainRelease
        case waitingRecovery
        case recoveredSecondary
        case probeInvoked
        case finishDrain
    }

    private static let secondaryWindowId =
        "main-close-recovery-secondary"
    private static let initialWindowStatus =
        "Main close recovery window: pending"
    private static let openedWindowStatus =
        "Main close recovery window: opened=true client=true error=none"
    private static let closingMainWindowStatus =
        "Main close recovery window: mainClosing=true client=true error=none"
    private static let initialProbeStatus =
        "Main close recovery probe: pending"
    private static let recoveredProbeStatus =
        "Main close recovery probe: backgroundSession=false "
        + "backgroundSuccess=false backgroundCanceled=true "
        + "backgroundInvalidated=true "
        + "backgroundReason=owner-window-closed "
        + "backgroundTimedOut=false secondarySuccess=true "
        + "secondaryError=none client=true change=1"
    private static let recoveredSecondaryTitle =
        "Flow main-close recovery secondary READY"

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var initialProcessIdentifier: Int32 = 0
    private var failedProcessIdentifier: Int32 = 0
    private var recoveredProcessIdentifier: Int32 = 0
    private var secondarySessionId = ""
    private var secondaryWindowIdentifier: ObjectIdentifier?
    private var secondaryWebViewIdentifier: ObjectIdentifier?
    private var secondaryOpenedObserved = false
    private var scopeIsolatedObserved = false
    private var mainCloseCommandSucceeded = false
    private var mainContextReleasedObserved = false
    private var secondaryContextRetainedObserved = false
    private var secondaryWindowPreserved = false
    private var secondaryWebViewPreserved = false
    private var secondaryScopePreserved = false
    private var processReplacedObserved = false
    private var mainStillAbsentAfterRecovery = false
    private var backgroundNoVaadinSessionObserved = false
    private var backgroundProbeSettledObserved = false
    private var backgroundMainCanceledObserved = false
    private var backgroundMainInvalidatedObserved = false
    private var secondaryNativeResultObserved = false
    private var secondaryTitleAppliedObserved = false
    private var recoveredJavaListenerObserved = false
    private var recoveredUidlPatchObserved = false
    private var nativePointerStages: [String] = []
    private var nativePointerReceiptStages: [String] = []
    private var nativePointerTrustedStages: [String] = []
    private var browserProgrammaticStages: [String] = []
    private var probeRequestBaseline = 0
    private var probeResponseBaseline = 0
    private var drainKey = ""
    private var drainBalancedPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
    }

    func resume() {
        schedulePoll(after: 0.1)
    }

    private func schedulePoll(after delay: Double) {
        guard !terminal, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal else { return }
        switch phase {
        case .root, .route, .secondaryReady:
            pollBeforeMainClose()
        case .closeMainInvoked, .waitingMainRelease,
                .waitingRecovery, .recoveredSecondary,
                .probeInvoked, .finishDrain:
            pollWithSecondary()
        }
    }

    private func pollBeforeMainClose() {
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "mainWindowCloseRecoveryMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(
                    code: "mainWindowCloseRecoveryMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "mainWindowCloseRecoveryMainRuntimeError",
                    details: mainSnapshot.tracePayload())
                return
            }
            guard let secondary = self.host.flowIpcWindowContext(
                Self.secondaryWindowId) else {
                self.advanceBeforeMainClose(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: nil,
                    secondarySnapshot: nil)
                return
            }
            guard secondary.navigationFinished else {
                self.retryOrFail(
                    code: "mainWindowCloseRecoverySecondaryNavigationTimeout")
                return
            }
            self.inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot else {
                    self.retryOrFail(
                        code: "mainWindowCloseRecoverySecondaryInspectionFailed")
                    return
                }
                guard secondarySnapshot.errorFree else {
                    self.fail(
                        code: "mainWindowCloseRecoverySecondaryRuntimeError",
                        details: secondarySnapshot.tracePayload())
                    return
                }
                self.advanceBeforeMainClose(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: secondary,
                    secondarySnapshot: secondarySnapshot)
            }
        }
    }

    private func advanceBeforeMainClose(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        secondary: FlowIpcWindowContext?,
        secondarySnapshot: Snapshot?
    ) {
        switch phase {
        case .root:
            guard mainSnapshot.rootRendered,
                  mainSnapshot.firstMeaningfulPaint,
                  mainSnapshot.flowClientCount > 0,
                  main.recoveryGeneration == 0 else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryInitialRootTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            initialProcessIdentifier = host.sidecarProcessIdentifier ?? 0
            guard initialProcessIdentifier > 0 else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryInitialProcessTimeout")
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-main-window-close-recovery-open-route",
                stage: "route-navigate",
                next: .route,
                failureCode: "mainWindowCloseRecoveryNavigationFailed")
        case .route:
            guard mainSnapshot.routeReady,
                  mainSnapshot.windowStatus == Self.initialWindowStatus,
                  mainSnapshot.probeStatus == Self.initialProbeStatus,
                  exactMainScope(mainSnapshot) else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryRouteTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-main-window-close-recovery-open-window",
                stage: "secondary-open",
                next: .secondaryReady,
                failureCode: "mainWindowCloseRecoveryOpenSecondaryFailed")
        case .secondaryReady:
            guard mainSnapshot.routeReady,
                  mainSnapshot.windowStatus == Self.openedWindowStatus,
                  let secondary,
                  let secondarySnapshot,
                  secondarySnapshot.routeReady,
                  secondarySnapshot.windowStatus
                    == Self.initialWindowStatus,
                  secondarySnapshot.probeStatus
                    == Self.initialProbeStatus else {
                retryOrFail(
                    code: "mainWindowCloseRecoverySecondaryReadyTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            secondaryOpenedObserved = true
            secondarySessionId = secondary.scope.sessionId
            scopeIsolatedObserved = exactMainScope(mainSnapshot)
                && exactSecondaryScope(secondarySnapshot)
                && mainSnapshot.sessionId != secondarySnapshot.sessionId
                && opaqueSecondarySession(secondarySessionId)
            guard scopeIsolatedObserved else {
                fail(
                    code: "mainWindowCloseRecoveryScopeIsolationFailed",
                    details: secondarySnapshot.tracePayload())
                return
            }
            secondaryWindowIdentifier = ObjectIdentifier(secondary.window)
            secondaryWebViewIdentifier = ObjectIdentifier(secondary.webView)
            performNativeClick(
                in: secondary,
                buttonId:
                    "framework-flow-main-window-close-recovery-close-main",
                stage: "secondary-close-main",
                next: .closeMainInvoked,
                failureCode: "mainWindowCloseRecoveryCloseMainFailed")
        case .closeMainInvoked, .waitingMainRelease, .waitingRecovery,
                .recoveredSecondary, .probeInvoked, .finishDrain:
            break
        }
    }

    private func pollWithSecondary() {
        guard let secondary = host.flowIpcWindowContext(
            Self.secondaryWindowId) else {
            retryOrFail(code: "mainWindowCloseRecoverySecondaryLost")
            return
        }
        if phase == .waitingRecovery {
            inspectRecovery(secondary)
            return
        }
        guard secondary.navigationFinished else {
            retryOrFail(
                code: "mainWindowCloseRecoverySecondaryNavigationTimeout")
            return
        }
        inspect(secondary) { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "mainWindowCloseRecoverySecondaryInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "mainWindowCloseRecoverySecondaryRuntimeError",
                    details: snapshot.tracePayload())
                return
            }
            self.advanceWithSecondary(
                secondary: secondary,
                snapshot: snapshot)
        }
    }

    private func advanceWithSecondary(
        secondary: FlowIpcWindowContext,
        snapshot: Snapshot
    ) {
        switch phase {
        case .closeMainInvoked:
            recordPointer(
                stage: "secondary-close-main",
                snapshot: snapshot)
            guard pointerObserved(
                    stage: "secondary-close-main",
                    snapshot: snapshot),
                  snapshot.windowStatus
                    == Self.closingMainWindowStatus else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryCloseResultTimeout",
                    details: snapshot.tracePayload())
                return
            }
            mainCloseCommandSucceeded = true
            phase = .waitingMainRelease
            schedulePoll(after: 0.05)
        case .waitingMainRelease:
            guard host.flowIpcWindowContext("main") == nil,
                  snapshot.windowStatus == Self.closingMainWindowStatus,
                  exactSecondaryScope(snapshot),
                  host.sidecarProcessIdentifier
                    == initialProcessIdentifier else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryMainReleaseTimeout",
                    details: snapshot.tracePayload(),
                    delay: 0.05)
                return
            }
            mainContextReleasedObserved = true
            secondaryContextRetainedObserved = true
            trace("recv", "flowMainWindowCloseRecovery.mainReleased", [
                "complete": true,
                "mainContextReleased": true,
                "secondaryContextRetained": true,
                "windowId": secondary.scope.windowId,
                "sessionId": secondary.scope.sessionId,
                "uiId": secondary.scope.uiId,
                "processIdentifier": initialProcessIdentifier,
                "recoveryGeneration": secondary.recoveryGeneration
            ])
            guard failSidecar(secondary: secondary) else {
                fail(
                    code: "mainWindowCloseRecoveryFailureInjectionFailed")
                return
            }
            phase = .waitingRecovery
            schedulePoll(after: 0.05)
        case .recoveredSecondary:
            guard snapshot.routeReady,
                  snapshot.firstMeaningfulPaint,
                  snapshot.flowClientCount > 0,
                  snapshot.windowStatus == Self.initialWindowStatus,
                  snapshot.probeStatus == Self.initialProbeStatus,
                  exactSecondaryScope(snapshot),
                  secondary.recoveryGeneration == 1,
                  host.flowIpcWindowContext("main") == nil else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryRecoveredRouteTimeout",
                    details: snapshot.tracePayload())
                return
            }
            mainStillAbsentAfterRecovery = true
            probeRequestBaseline = snapshot.requestCount
            probeResponseBaseline = snapshot.responseCount
            performNativeClick(
                in: secondary,
                buttonId:
                    "framework-flow-main-window-close-recovery-probe",
                stage: "recovered-probe",
                next: .probeInvoked,
                failureCode: "mainWindowCloseRecoveryProbeDispatchFailed")
        case .probeInvoked:
            recordPointer(stage: "recovered-probe", snapshot: snapshot)
            guard pointerObserved(
                    stage: "recovered-probe",
                    snapshot: snapshot),
                  snapshot.probeStatus == Self.recoveredProbeStatus else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryProbeResultTimeout",
                    details: snapshot.tracePayload())
                return
            }
            backgroundNoVaadinSessionObserved = true
            backgroundProbeSettledObserved = true
            backgroundMainCanceledObserved = true
            backgroundMainInvalidatedObserved = true
            secondaryNativeResultObserved = true
            secondaryTitleAppliedObserved =
                secondary.window.title == Self.recoveredSecondaryTitle
            recoveredJavaListenerObserved = true
            recoveredUidlPatchObserved =
                snapshot.requestCount > probeRequestBaseline
                && snapshot.responseCount > probeResponseBaseline
            guard secondaryTitleAppliedObserved,
                  recoveredUidlPatchObserved else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryProbeUidlTimeout",
                    details: [
                        "nativeTitle": secondary.window.title,
                        "requestCount": snapshot.requestCount,
                        "responseCount": snapshot.responseCount
                    ])
                return
            }
            phase = .finishDrain
            schedulePoll(after: 0.5)
        case .finishDrain:
            guard snapshot.probeStatus == Self.recoveredProbeStatus,
                  secondary.window.title == Self.recoveredSecondaryTitle,
                  host.flowIpcWindowContext("main") == nil else {
                fail(
                    code: "mainWindowCloseRecoveryResultChangedDuringDrain",
                    details: snapshot.tracePayload())
                return
            }
            let balanced = snapshot.requestCount == snapshot.responseCount
            let key = "\(snapshot.requestCount):\(snapshot.responseCount)"
            if balanced {
                if drainKey == key {
                    drainBalancedPolls += 1
                } else {
                    drainKey = key
                    drainBalancedPolls = 1
                }
            } else {
                drainKey = ""
                drainBalancedPolls = 0
            }
            guard drainBalancedPolls >= 2 else {
                retryOrFail(
                    code: "mainWindowCloseRecoveryDrainTimeout",
                    details: [
                        "requestCount": snapshot.requestCount,
                        "responseCount": snapshot.responseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            finish(secondary: secondary, snapshot: snapshot)
        case .waitingRecovery:
            break
        case .root, .route, .secondaryReady:
            break
        }
    }

    private func inspectRecovery(_ secondary: FlowIpcWindowContext) {
        let processIdentifier = host.sidecarProcessIdentifier ?? 0
        if secondary.recoveryGeneration > 1 {
            fail(
                code: "mainWindowCloseRecoveryUnexpectedExtraRecovery",
                details: [
                    "recoveryGeneration": secondary.recoveryGeneration
                ])
            return
        }
        guard secondary.recoveryGeneration == 1,
              processIdentifier > 0,
              processIdentifier != failedProcessIdentifier,
              secondary.navigationFinished,
              host.flowIpcWindowContext("main") == nil else {
            retryOrFail(
                code: "mainWindowCloseRecoveryRecoveryTimeout",
                details: [
                    "processIdentifier": processIdentifier,
                    "failedProcessIdentifier": failedProcessIdentifier,
                    "recoveryGeneration": secondary.recoveryGeneration,
                    "navigationFinished": secondary.navigationFinished,
                    "mainContextPresent":
                        host.flowIpcWindowContext("main") != nil
                ],
                delay: 0.1)
            return
        }
        recoveredProcessIdentifier = processIdentifier
        secondaryWindowPreserved = secondaryWindowIdentifier
            == ObjectIdentifier(secondary.window)
        secondaryWebViewPreserved = secondaryWebViewIdentifier
            == ObjectIdentifier(secondary.webView)
        secondaryScopePreserved = secondary.scope.windowId
            == Self.secondaryWindowId
            && secondary.scope.sessionId == secondarySessionId
            && secondary.scope.uiId == Self.secondaryWindowId
        processReplacedObserved = initialProcessIdentifier > 0
            && recoveredProcessIdentifier > 0
            && initialProcessIdentifier != recoveredProcessIdentifier
        guard secondaryWindowPreserved,
              secondaryWebViewPreserved,
              secondaryScopePreserved,
              processReplacedObserved else {
            fail(
                code: "mainWindowCloseRecoveryContinuityFailed",
                details: [
                    "secondaryWindowPreserved": secondaryWindowPreserved,
                    "secondaryWebViewPreserved": secondaryWebViewPreserved,
                    "secondaryScopePreserved": secondaryScopePreserved,
                    "processReplaced": processReplacedObserved
                ])
            return
        }
        phase = .recoveredSecondary
        schedulePoll(after: 0.1)
    }

    private func failSidecar(
        secondary: FlowIpcWindowContext
    ) -> Bool {
        guard let processIdentifier = host.sidecarProcessIdentifier,
              processIdentifier == initialProcessIdentifier,
              processIdentifier > 0,
              Darwin.kill(processIdentifier, SIGKILL) == 0 else {
            return false
        }
        failedProcessIdentifier = processIdentifier
        trace("send", "acceptance.sidecarFailureInjected", [
            "reason": "main-window-closed-recovery",
            "processIdentifier": processIdentifier,
            "windowId": secondary.scope.windowId
        ])
        return true
    }

    private func dispatchBrowserClick(
        in context: FlowIpcWindowContext,
        buttonId: String,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        let script = """
        (() => {
          const button = document.querySelector('#\(buttonId)');
          if (!button || typeof button.click !== 'function') return false;
          button.click();
          return true;
        })()
        """
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result),
                        "stage": stage
                    ])
                return
            }
            if !self.browserProgrammaticStages.contains(stage) {
                self.browserProgrammaticStages.append(stage)
            }
            self.phase = next
            self.schedulePoll(after: 0.2)
        }
    }

    private func performNativeClick(
        in context: FlowIpcWindowContext,
        buttonId: String,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(
            Self.pointerPreparationScript(
                buttonId: buttonId,
                stage: stage)) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(
                    PointerGeometry.self,
                    from: data),
                  geometry.ready else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result),
                        "stage": stage
                    ])
                return
            }
            FlowIpcNativeInputActivation.prepare(
                context: context,
                deadline: self.deadline,
                onFailure: { details in
                    self.fail(code: failureCode, details: details)
                }
            ) {
                let point = context.webView.convert(
                    NSPoint(
                        x: geometry.centerX,
                        y: context.webView.isFlipped
                            ? geometry.centerY
                            : context.webView.bounds.height
                                - geometry.centerY),
                    to: nil)
                guard context.window.contentLayoutRect.contains(point) else {
                    self.retryOrFail(
                        code: failureCode,
                        details: [
                            "message": "Pointer target is outside the window",
                            "windowX": point.x,
                            "windowY": point.y,
                            "stage": stage
                        ])
                    return
                }
                let eventNumber = Int(
                    Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard let down = NSEvent.mouseEvent(
                    with: .leftMouseDown,
                    location: point,
                    modifierFlags: [],
                    timestamp: ProcessInfo.processInfo.systemUptime,
                    windowNumber: context.window.windowNumber,
                    context: nil,
                    eventNumber: eventNumber,
                    clickCount: 1,
                    pressure: 1),
                      let up = NSEvent.mouseEvent(
                        with: .leftMouseUp,
                        location: point,
                        modifierFlags: [],
                        timestamp:
                            ProcessInfo.processInfo.systemUptime + 0.05,
                        windowNumber: context.window.windowNumber,
                        context: nil,
                        eventNumber: eventNumber + 1,
                        clickCount: 1,
                        pressure: 0) else {
                    self.fail(code: failureCode)
                    return
                }
                trace(
                    "recv",
                    "flowMainWindowCloseRecovery.nativePointerDispatch",
                    [
                        "inputMode": "appkit-window-pointer-event",
                        "button": "primary",
                        "stage": stage,
                        "webViewX": geometry.centerX,
                        "webViewY": geometry.centerY,
                        "windowX": point.x,
                        "windowY": point.y,
                        "activeApplication": NSApplication.shared.isActive,
                        "keyWindow": context.window.isKeyWindow,
                        "windowId": context.scope.windowId,
                        "sessionId": context.scope.sessionId,
                        "uiId": context.scope.uiId,
                        "recoveryGeneration": context.recoveryGeneration
                    ])
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    context.window.sendEvent(up)
                    if !self.nativePointerStages.contains(stage) {
                        self.nativePointerStages.append(stage)
                    }
                    self.phase = next
                    self.schedulePoll(after: 0.1)
                }
            }
        }
    }

    private func inspect(
        _ context: FlowIpcWindowContext,
        completion: @escaping (Snapshot?) -> Void
    ) {
        context.webView.evaluateJavaScript(Self.inspectionScript) {
            result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(
                    Snapshot.self,
                    from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func recordPointer(
        stage: String,
        snapshot: Snapshot
    ) {
        guard let observation = snapshot.pointerStages[stage] else {
            return
        }
        if observation.receipt,
           !nativePointerReceiptStages.contains(stage) {
            nativePointerReceiptStages.append(stage)
        }
        if observation.trusted,
           !nativePointerTrustedStages.contains(stage) {
            nativePointerTrustedStages.append(stage)
        }
    }

    private func pointerObserved(
        stage: String,
        snapshot: Snapshot
    ) -> Bool {
        snapshot.pointerStages[stage]?.receipt == true
            && snapshot.pointerStages[stage]?.trusted == true
    }

    private func exactMainScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == "main"
            && snapshot.sessionId == "flow-ipc-session"
            && snapshot.uiId == "main"
    }

    private func exactSecondaryScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == Self.secondaryWindowId
            && !secondarySessionId.isEmpty
            && snapshot.sessionId == secondarySessionId
            && snapshot.uiId == Self.secondaryWindowId
    }

    private func opaqueSecondarySession(_ sessionId: String) -> Bool {
        let prefix = "flow-ipc-window:"
        guard sessionId.hasPrefix(prefix) else {
            return false
        }
        return UUID(uuidString: String(sessionId.dropFirst(prefix.count)))
            != nil
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:],
        delay: Double = 0.1
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        schedulePoll(after: delay)
    }

    private func fail(
        code: String,
        details: [String: Any] = [:]
    ) {
        guard !terminal else { return }
        terminal = true
        var payload = details
        payload["code"] = code
        payload["phase"] = phase.rawValue
        trace("send", "error", payload)
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(
        secondary: FlowIpcWindowContext,
        snapshot: Snapshot
    ) {
        let expectedPointerStages = [
            "secondary-close-main",
            "recovered-probe"
        ]
        let expectedBrowserStages = [
            "route-navigate",
            "secondary-open"
        ]
        let nativePointerObserved =
            nativePointerStages == expectedPointerStages
            && nativePointerReceiptStages == expectedPointerStages
        let nativePointerTrustedObserved =
            nativePointerTrustedStages == expectedPointerStages
        let browserProgrammaticSetupObserved =
            browserProgrammaticStages == expectedBrowserStages
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestObserved": recoveredUidlPatchObserved,
            "opaqueResponseObserved": recoveredUidlPatchObserved,
            "secondaryOpenedObserved": secondaryOpenedObserved,
            "scopeIsolatedObserved": scopeIsolatedObserved,
            "mainCloseCommandSucceeded": mainCloseCommandSucceeded,
            "mainContextReleasedObserved": mainContextReleasedObserved,
            "secondaryContextRetainedObserved":
                secondaryContextRetainedObserved,
            "secondaryWindowPreserved": secondaryWindowPreserved,
            "secondaryWebViewPreserved": secondaryWebViewPreserved,
            "secondaryScopePreserved": secondaryScopePreserved,
            "processReplacedObserved": processReplacedObserved,
            "mainStillAbsentAfterRecovery": mainStillAbsentAfterRecovery,
            "backgroundNoVaadinSessionObserved":
                backgroundNoVaadinSessionObserved,
            "backgroundProbeSettledObserved":
                backgroundProbeSettledObserved,
            "backgroundProbeTimedOut": false,
            "backgroundMainSuccess": false,
            "backgroundMainCanceledObserved":
                backgroundMainCanceledObserved,
            "backgroundMainInvalidatedObserved":
                backgroundMainInvalidatedObserved,
            "backgroundMainCancelReason": "owner-window-closed",
            "secondaryNativeResultObserved":
                secondaryNativeResultObserved,
            "secondaryTitleAppliedObserved":
                secondaryTitleAppliedObserved,
            "recoveredJavaListenerObserved":
                recoveredJavaListenerObserved,
            "recoveredUidlPatchObserved": recoveredUidlPatchObserved,
            "nativePointerObserved": nativePointerObserved,
            "nativePointerTrustedObserved":
                nativePointerTrustedObserved,
            "browserProgrammaticSetupObserved":
                browserProgrammaticSetupObserved,
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain":
                snapshot.probeStatus == Self.recoveredProbeStatus,
            "errorFree": snapshot.errorFree,
            "pushObserved": false,
            "mainWindowId": "main",
            "mainSessionId": "flow-ipc-session",
            "mainUiId": "main",
            "secondaryWindowId": secondary.scope.windowId,
            "secondarySessionId": secondary.scope.sessionId,
            "secondaryUiId": secondary.scope.uiId,
            "secondaryTitle": secondary.window.title,
            "status": snapshot.probeStatus,
            "initialProcessIdentifier": initialProcessIdentifier,
            "failedProcessIdentifier": failedProcessIdentifier,
            "recoveredProcessIdentifier": recoveredProcessIdentifier,
            "recoveryGeneration": secondary.recoveryGeneration,
            "probeRequestBaseline": probeRequestBaseline,
            "probeResponseBaseline": probeResponseBaseline,
            "requestCount": snapshot.requestCount,
            "responseCount": snapshot.responseCount,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                nativePointerTrustedStages.count,
            "nativePointerStages":
                nativePointerStages.joined(separator: ","),
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ",")
        ]
        terminal = true
        trace(
            "recv",
            "flowMainWindowCloseRecovery.frameworkAcceptance",
            payload)
        secondary.window.close()
    }

    private struct PointerGeometry: Decodable {
        let ready: Bool
        let centerX: Double
        let centerY: Double
    }

    private struct PointerObservation: Decodable {
        let receipt: Bool
        let trusted: Bool
    }

    private struct Snapshot: Decodable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let windowStatus: String
        let probeStatus: String
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let pointerStages: [String: PointerObservation]
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            browserError.isEmpty && requestError.isEmpty
                && responseError.isEmpty && pushError.isEmpty
        }

        func tracePayload() -> [String: Any] {
            [
                "windowId": windowId,
                "sessionId": sessionId,
                "uiId": uiId,
                "rootRendered": rootRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "routeReady": routeReady,
                "windowStatus": windowStatus,
                "probeStatus": probeStatus,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "pointerStages": pointerStages.keys.sorted()
                    .joined(separator: ","),
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static func pointerPreparationScript(
        buttonId: String,
        stage: String
    ) -> String {
        """
        (() => {
          const button = document.querySelector('#\(buttonId)');
          if (!button) return JSON.stringify({ready:false});
          const rect = button.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2;
          const centerY = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(centerX, centerY);
          if (!(rect.width > 0 && rect.height > 0)
              || !(hit === button || button.contains(hit))) {
            return JSON.stringify({ready:false, centerX, centerY});
          }
          const marker = window.VaadinDesktopMainWindowCloseRecoveryPointer
            ||= {stages: {}, pendingHandlers: {}};
          const previous = marker.pendingHandlers['\(stage)'];
          if (typeof previous === 'function') {
            document.removeEventListener('click', previous, true);
          }
          marker.stages['\(stage)'] = {receipt:false, trusted:false};
          const handler = (event) => {
            if (event.button !== 0 || !event.composedPath().includes(button)) {
              return;
            }
            marker.stages['\(stage)'] = {
              receipt:true,
              trusted:event.isTrusted === true
            };
            document.removeEventListener('click', handler, true);
            delete marker.pendingHandlers['\(stage)'];
          };
          marker.pendingHandlers['\(stage)'] = handler;
          document.addEventListener('click', handler, true);
          return JSON.stringify({ready:true, centerX, centerY});
        })()
        """
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const context = window.VaadinDesktopHostContext || {};
      const route = document.querySelector(
        '#framework-flow-main-window-close-recovery-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer =
        window.VaadinDesktopMainWindowCloseRecoveryPointer || {stages:{}};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith(
            '/flow-main-window-close-recovery')
          && !!route
          && !!document.querySelector(
            '#framework-flow-main-window-close-recovery-open-window')
          && !!document.querySelector(
            '#framework-flow-main-window-close-recovery-close-main')
          && !!document.querySelector(
            '#framework-flow-main-window-close-recovery-probe'),
        windowStatus: String(document.querySelector(
          '#framework-flow-main-window-close-recovery-window-status')
          ?.textContent || ''),
        probeStatus: String(document.querySelector(
          '#framework-flow-main-window-close-recovery-probe-status')
          ?.textContent || ''),
        requestCount: Number(
          diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(
          diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(
          diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        pointerStages: pointer.stages || {},
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(
          diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(
          diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
