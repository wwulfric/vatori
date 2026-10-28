import Cocoa
import Foundation

private var flowIpcCapabilityOwnerWindowCloseAcceptance:
    FlowIpcCapabilityOwnerWindowCloseAcceptance?

extension WebViewHost {
    func scheduleCapabilityOwnerWindowCloseAcceptanceIfReady() {
        guard config.flowIpcAutoCapabilityOwnerWindowClose,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcCapabilityOwnerWindowCloseAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcCapabilityOwnerWindowCloseAcceptance =
                FlowIpcCapabilityOwnerWindowCloseAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcCapabilityOwnerWindowCloseAcceptance?.resume()
    }
}

private final class FlowIpcCapabilityOwnerWindowCloseAcceptance {
    private enum Phase: String {
        case root
        case mainRoute
        case secondaryReady
        case dialogInvoked
        case ownerCloseInvoked
        case waitingHealthDelay
        case healthInvoked
        case finishDrain
    }

    private static let ownerWindowId = "owner-close-secondary"
    private static let ownerUiId = "owner-close-secondary"
    private static let dialogTitle =
        "Flow owner window close file dialog"
    private static let initialWindowStatus = "Owner close window: pending"
    private static let closingWindowStatus =
        "Owner close window: closing=true client=true error=none"
    private static let initialJavaResult =
        "Owner close Java result: outcome=pending selected=none "
        + "cancelReason=none operationInvalidated=false "
        + "capabilityContextCaptured=false "
        + "backgroundSessionAbsent=false "
        + "secondaryClient=false change=0"
    private static let expectedHealthStatus =
        "Owner close main health: result=outcome=INVALIDATED selected=none "
        + "cancelReason=owner-window-closed operationInvalidated=true "
        + "capabilityContextCaptured=true "
        + "backgroundSessionAbsent=true "
        + "secondaryClient=true change=1 client=true change=1"

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var initialMainWindowIdentifier: ObjectIdentifier?
    private var initialMainWebViewIdentifier: ObjectIdentifier?
    private var initialProcessIdentifier: Int32 = 0
    private var ownerWindowIdentifier: ObjectIdentifier?
    private var ownerWebViewIdentifier: ObjectIdentifier?
    private var ownerSessionId = ""
    private weak var ownerWindow: NSWindow?
    private var ownerPanel: NSOpenPanel?
    private var ownerPanelObserved = false
    private var ownerPanelAttachedObserved = false
    private var ownerPanelVisibleObserved = false
    private var ownerPanelProductInvalidatedObserved = false
    private var ownerPanelDetachedObserved = false
    private var ownerPanelHiddenObserved = false
    private var ownerWindowClosedObserved = false
    private var ownerWindowContextReleasedObserved = false
    private var ownerSecondaryOpenedObserved = false
    private var secondaryScopeIsolatedObserved = false
    private var windowCloseCommandObserved = false
    private var javaListenerObserved = false
    private var javaTypedInvalidatedOutcomeObserved = false
    private var capabilityContextCapturedObserved = false
    private var backgroundSessionAbsentObserved = false
    private var ownerCloseResultVisibleInMain = false
    private var mainWindowRetainedObserved = false
    private var mainWebViewRetainedObserved = false
    private var mainProcessRetainedObserved = false
    private var mainHealthJavaListenerObserved = false
    private var mainHealthUidlPatchObserved = false
    private var nativePointerStages: [String] = []
    private var nativePointerReceiptStages: [String] = []
    private var nativePointerTrustedStages: [String] = []
    private var browserProgrammaticStages: [String] = []
    private var healthNotBefore = Date.distantPast
    private var healthRequestBaseline = 0
    private var healthResponseBaseline = 0
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
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "capabilityOwnerCloseMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(
                    code: "capabilityOwnerCloseMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "capabilityOwnerCloseMainRuntimeError",
                    details: mainSnapshot.tracePayload())
                return
            }
            guard let owner = self.host.flowIpcWindowContext(
                Self.ownerWindowId) else {
                self.advance(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    owner: nil,
                    ownerSnapshot: nil)
                return
            }
            self.inspect(owner) { ownerSnapshot in
                guard let ownerSnapshot else {
                    self.retryOrFail(
                        code: "capabilityOwnerCloseOwnerInspectionFailed")
                    return
                }
                guard ownerSnapshot.errorFree else {
                    self.fail(
                        code: "capabilityOwnerCloseOwnerRuntimeError",
                        details: ownerSnapshot.tracePayload())
                    return
                }
                self.advance(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    owner: owner,
                    ownerSnapshot: ownerSnapshot)
            }
        }
    }

    private func advance(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        owner: FlowIpcWindowContext?,
        ownerSnapshot: Snapshot?
    ) {
        switch phase {
        case .root:
            guard mainSnapshot.rootRendered,
                  mainSnapshot.firstMeaningfulPaint,
                  mainSnapshot.flowClientCount > 0,
                  main.recoveryGeneration == 0 else {
                retryOrFail(
                    code: "capabilityOwnerCloseInitialRootTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            initialMainWindowIdentifier = ObjectIdentifier(main.window)
            initialMainWebViewIdentifier = ObjectIdentifier(main.webView)
            initialProcessIdentifier = host.sidecarProcessIdentifier ?? 0
            guard initialProcessIdentifier > 0 else {
                retryOrFail(
                    code: "capabilityOwnerCloseInitialProcessTimeout")
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-file-dialog-owner-close-open-route",
                stage: "route-navigate",
                next: .mainRoute,
                failureCode: "capabilityOwnerCloseNavigationFailed")
        case .mainRoute:
            guard mainSnapshot.routeReady,
                  mainSnapshot.windowStatus == Self.initialWindowStatus,
                  mainSnapshot.javaResult == Self.initialJavaResult,
                  mainSnapshot.healthStatus
                    == "Owner close main health: pending" else {
                retryOrFail(
                    code: "capabilityOwnerCloseMainRouteTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-file-dialog-owner-close-open-window",
                stage: "secondary-open",
                next: .secondaryReady,
                failureCode: "capabilityOwnerCloseOpenWindowFailed")
        case .secondaryReady:
            guard mainSnapshot.routeReady,
                  mainSnapshot.windowStatus
                    == "Owner close window: opened=true client=true error=none",
                  let owner,
                  owner.navigationFinished,
                  let ownerSnapshot,
                  ownerSnapshot.routeReady,
                  ownerSnapshot.javaResult == Self.initialJavaResult else {
                retryOrFail(
                    code: "capabilityOwnerCloseSecondaryReadyTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            ownerSecondaryOpenedObserved = true
            ownerSessionId = owner.scope.sessionId
            secondaryScopeIsolatedObserved = exactMainScope(mainSnapshot)
                && exactOwnerScope(ownerSnapshot)
                && mainSnapshot.sessionId != ownerSnapshot.sessionId
                && opaqueOwnerSession(ownerSessionId)
            guard secondaryScopeIsolatedObserved else {
                fail(
                    code: "capabilityOwnerCloseScopeIsolationFailed",
                    details: ownerSnapshot.tracePayload())
                return
            }
            ownerWindow = owner.window
            ownerWindowIdentifier = ObjectIdentifier(owner.window)
            ownerWebViewIdentifier = ObjectIdentifier(owner.webView)
            performNativeClick(
                in: owner,
                buttonId: "framework-flow-file-dialog-owner-close-invoke",
                stage: "secondary-dialog-open",
                next: .dialogInvoked,
                failureCode: "capabilityOwnerCloseDialogInvokeFailed")
        case .dialogInvoked:
            guard let owner,
                  let ownerSnapshot,
                  exactOwnerScope(ownerSnapshot) else {
                retryOrFail(
                    code: "capabilityOwnerCloseOwnerLostBeforeClose")
                return
            }
            recordPointer(
                stage: "secondary-dialog-open",
                snapshot: ownerSnapshot)
            guard pointerObserved(
                    stage: "secondary-dialog-open",
                    snapshot: ownerSnapshot),
                  let panel = owner.window.attachedSheet as? NSOpenPanel,
                  panel.title == Self.dialogTitle,
                  panel.sheetParent === owner.window,
                  panel.isVisible else {
                retryOrFail(
                    code: "capabilityOwnerClosePanelTimeout",
                    details: ownerSnapshot.tracePayload())
                return
            }
            ownerPanel = panel
            ownerPanelObserved = true
            ownerPanelAttachedObserved = true
            ownerPanelVisibleObserved = true
            trace("recv", "flowCapabilityOwnerWindowClose.panelObserved", [
                "complete": true,
                "title": panel.title ?? "",
                "sheetAttached": panel.sheetParent === owner.window,
                "panelVisible": panel.isVisible,
                "windowId": owner.scope.windowId,
                "sessionId": owner.scope.sessionId,
                "uiId": owner.scope.uiId
            ])
            performNativeClick(
                in: main,
                buttonId:
                    "framework-flow-file-dialog-owner-close-close-window",
                stage: "main-owner-close",
                next: .ownerCloseInvoked,
                failureCode: "capabilityOwnerCloseCloseInvokeFailed")
        case .ownerCloseInvoked:
            recordPointer(
                stage: "main-owner-close",
                snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-owner-close",
                    snapshot: mainSnapshot),
                  mainSnapshot.windowStatus == Self.closingWindowStatus else {
                retryOrFail(
                    code: "capabilityOwnerCloseCommandResultTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            windowCloseCommandObserved = true
            guard owner == nil,
                  let panel = ownerPanel else {
                retryOrFail(
                    code: "capabilityOwnerCloseContextReleaseTimeout")
                return
            }
            let panelDetached = panel.sheetParent == nil
                && ownerWindow?.attachedSheet !== panel
            let panelHidden = !panel.isVisible
            let windowClosed = ownerWindow?.isVisible != true
            guard panelDetached, panelHidden, windowClosed else {
                retryOrFail(
                    code: "capabilityOwnerCloseNativeReleaseTimeout",
                    details: [
                        "panelDetached": panelDetached,
                        "panelHidden": panelHidden,
                        "windowClosed": windowClosed
                    ],
                    delay: 0.05)
                return
            }
            ownerPanelProductInvalidatedObserved = true
            ownerPanelDetachedObserved = true
            ownerPanelHiddenObserved = true
            ownerWindowClosedObserved = true
            ownerWindowContextReleasedObserved = true
            mainWindowRetainedObserved = initialMainWindowIdentifier
                == ObjectIdentifier(main.window)
            mainWebViewRetainedObserved = initialMainWebViewIdentifier
                == ObjectIdentifier(main.webView)
            mainProcessRetainedObserved = initialProcessIdentifier > 0
                && host.sidecarProcessIdentifier == initialProcessIdentifier
                && main.recoveryGeneration == 0
            guard mainWindowRetainedObserved,
                  mainWebViewRetainedObserved,
                  mainProcessRetainedObserved else {
                fail(
                    code: "capabilityOwnerCloseMainContinuityFailed",
                    details: [
                        "mainWindowRetained": mainWindowRetainedObserved,
                        "mainWebViewRetained": mainWebViewRetainedObserved,
                        "mainProcessRetained": mainProcessRetainedObserved
                    ])
                return
            }
            trace("recv", "flowCapabilityOwnerWindowClose.ownerReleased", [
                "complete": true,
                "contextReleased": true,
                "panelDetached": panelDetached,
                "panelHidden": panelHidden,
                "windowClosed": windowClosed,
                "mainWindowRetained": mainWindowRetainedObserved,
                "mainWebViewRetained": mainWebViewRetainedObserved,
                "mainProcessRetained": mainProcessRetainedObserved,
                "windowId": Self.ownerWindowId,
                "ownerWindowIdentityObserved": ownerWindowIdentifier != nil,
                "ownerWebViewIdentityObserved": ownerWebViewIdentifier != nil
            ])
            healthRequestBaseline = mainSnapshot.requestCount
            healthResponseBaseline = mainSnapshot.responseCount
            healthNotBefore = Date().addingTimeInterval(0.5)
            phase = .waitingHealthDelay
            schedulePoll(after: 0.1)
        case .waitingHealthDelay:
            guard Date() >= healthNotBefore else {
                retryOrFail(
                    code: "capabilityOwnerCloseHealthDelay",
                    delay: 0.1)
                return
            }
            performNativeClick(
                in: main,
                buttonId: "framework-flow-file-dialog-owner-close-health",
                stage: "main-health",
                next: .healthInvoked,
                failureCode: "capabilityOwnerCloseHealthInvokeFailed")
        case .healthInvoked:
            recordPointer(stage: "main-health", snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-health",
                    snapshot: mainSnapshot),
                  mainSnapshot.healthStatus == Self.expectedHealthStatus,
                  mainSnapshot.windowStatus == Self.closingWindowStatus else {
                retryOrFail(
                    code: "capabilityOwnerCloseHealthResultTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            javaListenerObserved = true
            javaTypedInvalidatedOutcomeObserved = true
            capabilityContextCapturedObserved = true
            backgroundSessionAbsentObserved = true
            ownerCloseResultVisibleInMain = true
            mainHealthJavaListenerObserved = true
            mainHealthUidlPatchObserved =
                mainSnapshot.requestCount > healthRequestBaseline
                && mainSnapshot.responseCount > healthResponseBaseline
            guard mainHealthUidlPatchObserved else {
                retryOrFail(
                    code: "capabilityOwnerCloseHealthUidlTimeout",
                    details: mainSnapshot.tracePayload())
                return
            }
            phase = .finishDrain
            schedulePoll(after: 0.5)
        case .finishDrain:
            guard mainSnapshot.healthStatus == Self.expectedHealthStatus,
                  mainSnapshot.windowStatus == Self.closingWindowStatus,
                  owner == nil,
                  ownerPanel?.sheetParent == nil,
                  ownerPanel?.isVisible == false else {
                fail(
                    code: "capabilityOwnerCloseResultChangedDuringDrain",
                    details: mainSnapshot.tracePayload())
                return
            }
            let balanced = mainSnapshot.requestCount
                == mainSnapshot.responseCount
            let key = "\(mainSnapshot.requestCount):\(mainSnapshot.responseCount)"
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
                    code: "capabilityOwnerCloseDrainTimeout",
                    details: [
                        "requestCount": mainSnapshot.requestCount,
                        "responseCount": mainSnapshot.responseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            finish(main: main, snapshot: mainSnapshot)
        }
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
                    "flowCapabilityOwnerWindowClose.nativePointerDispatch",
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
                        "uiId": context.scope.uiId
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

    private func recordPointer(stage: String, snapshot: Snapshot) {
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

    private func exactOwnerScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == Self.ownerWindowId
            && !ownerSessionId.isEmpty
            && snapshot.sessionId == ownerSessionId
            && snapshot.uiId == Self.ownerUiId
    }

    private func opaqueOwnerSession(_ sessionId: String) -> Bool {
        let prefix = "flow-ipc-window:"
        guard sessionId.hasPrefix(prefix) else {
            return false
        }
        return UUID(uuidString: String(sessionId.dropFirst(prefix.count))) != nil
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

    private func fail(code: String, details: [String: Any] = [:]) {
        guard !terminal else { return }
        terminal = true
        var payload = details
        payload["code"] = code
        payload["phase"] = phase.rawValue
        trace("send", "error", payload)
        host.flowIpcWindowContext(Self.ownerWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(
        main: FlowIpcWindowContext,
        snapshot: Snapshot
    ) {
        let expectedPointerStages = [
            "secondary-dialog-open",
            "main-owner-close",
            "main-health"
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
            "opaqueRequestObserved": mainHealthUidlPatchObserved,
            "opaqueResponseObserved": mainHealthUidlPatchObserved,
            "ownerSecondaryOpenedObserved": ownerSecondaryOpenedObserved,
            "secondaryScopeIsolatedObserved":
                secondaryScopeIsolatedObserved,
            "ownerPanelObserved": ownerPanelObserved,
            "ownerPanelAttachedObserved": ownerPanelAttachedObserved,
            "ownerPanelVisibleObserved": ownerPanelVisibleObserved,
            "ownerPanelProductInvalidatedObserved":
                ownerPanelProductInvalidatedObserved,
            "ownerPanelDetachedObserved": ownerPanelDetachedObserved,
            "ownerPanelHiddenObserved": ownerPanelHiddenObserved,
            "ownerPanelTestCancelInjected": false,
            "ownerWindowClosedObserved": ownerWindowClosedObserved,
            "ownerWindowContextReleasedObserved":
                ownerWindowContextReleasedObserved,
            "windowCloseCommandObserved": windowCloseCommandObserved,
            "javaListenerObserved": javaListenerObserved,
            "javaTypedInvalidatedOutcomeObserved":
                javaTypedInvalidatedOutcomeObserved,
            "capabilityContextCapturedObserved":
                capabilityContextCapturedObserved,
            "backgroundSessionAbsentObserved":
                backgroundSessionAbsentObserved,
            "ownerCloseResultVisibleInMain":
                ownerCloseResultVisibleInMain,
            "mainWindowRetainedObserved": mainWindowRetainedObserved,
            "mainWebViewRetainedObserved": mainWebViewRetainedObserved,
            "mainProcessRetainedObserved": mainProcessRetainedObserved,
            "mainHealthJavaListenerObserved":
                mainHealthJavaListenerObserved,
            "mainHealthUidlPatchObserved": mainHealthUidlPatchObserved,
            "nativePointerObserved": nativePointerObserved,
            "nativePointerTrustedObserved":
                nativePointerTrustedObserved,
            "browserProgrammaticSetupObserved":
                browserProgrammaticSetupObserved,
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain":
                snapshot.healthStatus == Self.expectedHealthStatus,
            "errorFree": snapshot.errorFree,
            "pushObserved": false,
            "command": "fileDialog.openFile",
            "typedOutcome": "INVALIDATED",
            "cancelReason": "owner-window-closed",
            "ownerWindowId": Self.ownerWindowId,
            "ownerSessionId": ownerSessionId,
            "ownerUiId": Self.ownerUiId,
            "mainWindowId": snapshot.windowId,
            "mainSessionId": snapshot.sessionId,
            "mainUiId": snapshot.uiId,
            "windowStatus": snapshot.windowStatus,
            "javaOutcome": snapshot.healthStatus,
            "requestCount": snapshot.requestCount,
            "responseCount": snapshot.responseCount,
            "healthRequestBaseline": healthRequestBaseline,
            "healthResponseBaseline": healthResponseBaseline,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                nativePointerTrustedStages.count,
            "nativePointerStages":
                nativePointerStages.joined(separator: ","),
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ","),
            "initialProcessIdentifier": initialProcessIdentifier,
            "currentProcessIdentifier":
                host.sidecarProcessIdentifier ?? 0,
            "recoveryGeneration": main.recoveryGeneration
        ]
        terminal = true
        trace(
            "recv",
            "flowCapabilityOwnerWindowClose.frameworkAcceptance",
            payload)
        main.window.close()
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
        let javaResult: String
        let healthStatus: String
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
                "javaResult": javaResult,
                "healthStatus": healthStatus,
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
          const marker = window.VaadinDesktopOwnerWindowClosePointer ||= {
            stages: {}, pendingHandlers: {}
          };
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
        '#framework-flow-file-dialog-owner-close-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer =
        window.VaadinDesktopOwnerWindowClosePointer || {stages:{}};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith(
            '/flow-file-dialog-owner-window-close')
          && !!route
          && !!document.querySelector(
            '#framework-flow-file-dialog-owner-close-open-window')
          && !!document.querySelector(
            '#framework-flow-file-dialog-owner-close-close-window')
          && !!document.querySelector(
            '#framework-flow-file-dialog-owner-close-invoke')
          && !!document.querySelector(
            '#framework-flow-file-dialog-owner-close-health'),
        windowStatus: String(document.querySelector(
          '#framework-flow-file-dialog-owner-close-window-status')
          ?.textContent || ''),
        javaResult: String(document.querySelector(
          '#framework-flow-file-dialog-owner-close-java-result')
          ?.textContent || ''),
        healthStatus: String(document.querySelector(
          '#framework-flow-file-dialog-owner-close-health-status')
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
