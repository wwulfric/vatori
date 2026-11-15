import Cocoa
import Foundation

private var flowIpcFileDialogOutcomesAcceptance:
    FlowIpcFileDialogOutcomesAcceptance?

extension WebViewHost {
    func scheduleFileDialogOutcomesAcceptanceIfReady() {
        guard config.flowIpcAutoFileDialogOutcomes,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcFileDialogOutcomesAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcFileDialogOutcomesAcceptance =
                FlowIpcFileDialogOutcomesAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileDialogOutcomesAcceptance?.resume()
    }
}

private final class FlowIpcFileDialogOutcomesAcceptance {
    private enum Phase: String {
        case root
        case mainReady
        case mainInvoked
        case concurrentInvoked
        case normalCanceled
        case secondaryOpening
        case secondaryReady
        case deniedInvoked
        case finishDrain
    }

    private static let secondaryWindowId =
        "file-dialog-outcomes-secondary"
    private static let firstTitle = "Flow file dialog normal cancel"
    private var firstStatus: String { Self.resultStatus(
        prefix: "File dialog first",
        outcome: "CANCELED",
        success: true,
        canceled: "true",
        error: "none",
        decision: "allowed",
        reason: "manifest-allow",
        scope: "window:main",
        commands: mainManifestCommands)
        + " capabilityContextCaptured=true"
        + " backgroundSessionAbsent=true" }
    private var concurrentStatus: String { Self.resultStatus(
        prefix: "File dialog concurrent",
        outcome: "ERROR",
        success: false,
        canceled: "true",
        error: "File dialog already in progress",
        decision: "allowed",
        reason: "manifest-allow",
        scope: "window:main",
        commands: mainManifestCommands) + " client=true" }
    private static let deniedStatus = resultStatus(
        prefix: "File dialog denied",
        outcome: "DENIED",
        success: false,
        canceled: "missing",
        error: "Command not allowed by desktop-capabilities.json",
        decision: "denied",
        reason: "manifest-denied",
        scope: "window:file-dialog-outcomes-secondary",
        commands: "window.close,window.open")
        + " capabilityContextCaptured=true"
        + " backgroundSessionAbsent=true client=true"
    private var mainManifestCommands: String {
        host.config.capabilityManifest.commandsByWindow["main", default: []]
            .sorted().joined(separator: ",")
    }

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var panel: NSOpenPanel?
    private var panelIdentifier: ObjectIdentifier?
    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var mainPushBaseline = 0
    private var secondaryRequestBaseline = 0
    private var secondaryResponseBaseline = 0
    private var secondaryPushBaseline = 0
    private var secondarySessionId = ""
    private var firstPanelObserved = false
    private var firstPanelAttachedObserved = false
    private var firstPanelVisibleObserved = false
    private var singlePanelDuringConcurrentObserved = false
    private var secondPanelAbsentObserved = false
    private var normalCancelInvoked = false
    private var firstPanelDetachedObserved = false
    private var firstPanelHiddenObserved = false
    private var firstCanceledOutcomeObserved = false
    private var concurrentErrorOutcomeObserved = false
    private var deniedOutcomeObserved = false
    private var deniedPanelAbsentObserved = false
    private var mainCapabilityContextCapturedObserved = false
    private var mainBackgroundSessionAbsentObserved = false
    private var secondaryCapabilityContextCapturedObserved = false
    private var secondaryBackgroundSessionAbsentObserved = false
    private var mainUidlPatchObserved = false
    private var mainPushObserved = false
    private var secondaryUidlPatchObserved = false
    private var secondaryPushObserved = false
    private var nativePointerStages: [String] = []
    private var nativePointerReceiptStages: [String] = []
    private var nativePointerTrustedStages: [String] = []
    private var browserProgrammaticStages: [String] = []
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
            retryOrFail(code: "fileDialogOutcomesMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(
                    code: "fileDialogOutcomesMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "fileDialogOutcomesMainRuntimeError",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            self.advance(main: main, mainSnapshot: mainSnapshot)
        }
    }

    private func advance(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot
    ) {
        switch phase {
        case .root:
            guard mainSnapshot.rootRendered,
                  mainSnapshot.firstMeaningfulPaint,
                  mainSnapshot.flowClientCount > 0 else {
                retryOrFail(
                    code: "fileDialogOutcomesRootTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId: "framework-flow-file-dialog-outcomes-open-route",
                stage: "route-navigate",
                next: .mainReady,
                failureCode: "fileDialogOutcomesNavigationFailed")
        case .mainReady:
            guard mainSnapshot.mainRouteReady,
                  mainSnapshot.startStatus == "File dialog start: pending",
                  mainSnapshot.firstStatus == "File dialog first: pending",
                  mainSnapshot.concurrentStatus
                    == "File dialog concurrent: pending" else {
                retryOrFail(
                    code: "fileDialogOutcomesMainRouteTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainRequestBaseline = mainSnapshot.requestCount
            mainResponseBaseline = mainSnapshot.responseCount
            mainPushBaseline = mainSnapshot.pushMessageCount
            performNativeClick(
                in: main,
                buttonId:
                    "framework-flow-file-dialog-outcomes-invoke-first",
                stage: "main-first",
                next: .mainInvoked,
                failureCode: "fileDialogOutcomesMainInvokeFailed")
        case .mainInvoked:
            recordPointer(stage: "main-first", snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-first",
                    snapshot: mainSnapshot),
                  mainSnapshot.startStatus
                    == "File dialog start: client=true "
                        + "capabilityContextCaptured=true change=1",
                  mainSnapshot.concurrentStatus
                    == "File dialog concurrent: pending",
                  let openPanel = main.window.attachedSheet as? NSOpenPanel,
                  openPanel.title == Self.firstTitle,
                  openPanel.sheetParent === main.window,
                  openPanel.isVisible else {
                retryOrFail(
                    code: "fileDialogOutcomesFirstPanelTimeout",
                    details: mergedPayload(
                        mainSnapshot: mainSnapshot,
                        main: main))
                return
            }
            let visiblePanels = Self.visibleOpenPanels()
            guard visiblePanels.count == 1,
                  visiblePanels.first === openPanel else {
                fail(
                    code: "fileDialogOutcomesDuplicatePanel",
                    details: ["visiblePanelCount": visiblePanels.count])
                return
            }
            panel = openPanel
            panelIdentifier = ObjectIdentifier(openPanel)
            firstPanelObserved = true
            firstPanelAttachedObserved = true
            firstPanelVisibleObserved = true
            mainCapabilityContextCapturedObserved = true
            trace("recv", "flowFileDialogOutcomes.panelObserved", [
                "complete": true,
                "title": openPanel.title ?? "",
                "visiblePanelCount": visiblePanels.count,
                "sheetAttached": openPanel.sheetParent === main.window,
                "panelVisible": openPanel.isVisible,
                "windowId": main.scope.windowId,
                "sessionId": main.scope.sessionId,
                "uiId": main.scope.uiId
            ])
            performNativeClick(
                in: main,
                buttonId:
                    "framework-flow-file-dialog-outcomes-invoke-concurrent",
                stage: "main-concurrent",
                next: .concurrentInvoked,
                failureCode: "fileDialogOutcomesConcurrentInvokeFailed")
        case .concurrentInvoked:
            recordPointer(stage: "main-concurrent", snapshot: mainSnapshot)
            guard pointerObserved(
                    stage: "main-concurrent",
                    snapshot: mainSnapshot),
                  mainSnapshot.concurrentStatus == concurrentStatus,
                  let openPanel = main.window.attachedSheet as? NSOpenPanel,
                  panelIdentifier == ObjectIdentifier(openPanel),
                  openPanel.title == Self.firstTitle,
                  openPanel.sheetParent === main.window,
                  openPanel.isVisible else {
                retryOrFail(
                    code: "fileDialogOutcomesConcurrentResultTimeout",
                    details: mergedPayload(
                        mainSnapshot: mainSnapshot,
                        main: main))
                return
            }
            let visiblePanels = Self.visibleOpenPanels()
            guard visiblePanels.count == 1,
                  visiblePanels.first === openPanel else {
                fail(
                    code: "fileDialogOutcomesDuplicatePanel",
                    details: ["visiblePanelCount": visiblePanels.count])
                return
            }
            concurrentErrorOutcomeObserved = true
            singlePanelDuringConcurrentObserved = true
            secondPanelAbsentObserved = true
            normalCancelInvoked = true
            trace("send", "acceptance.nativePanelCancelInjected", [
                "complete": true,
                "command": "fileDialog.openFile",
                "mode": "appkit-panel-cancel-action",
                "reason": "file-dialog-outcomes-normal-cancel",
                "stage": "normal-cancel",
                "title": openPanel.title ?? "",
                "windowId": main.scope.windowId,
                "sessionId": main.scope.sessionId,
                "uiId": main.scope.uiId
            ])
            openPanel.cancel(nil)
            phase = .normalCanceled
            schedulePoll(after: 0.1)
        case .normalCanceled:
            guard mainSnapshot.firstStatus == firstStatus,
                  let panel,
                  panelIdentifier == ObjectIdentifier(panel),
                  panel.sheetParent == nil,
                  main.window.attachedSheet !== panel,
                  !panel.isVisible,
                  Self.visibleOpenPanels().isEmpty else {
                retryOrFail(
                    code: "fileDialogOutcomesNormalCancelTimeout",
                    details: mergedPayload(
                        mainSnapshot: mainSnapshot,
                        main: main))
                return
            }
            firstCanceledOutcomeObserved = true
            firstPanelDetachedObserved = true
            firstPanelHiddenObserved = true
            mainBackgroundSessionAbsentObserved = true
            mainUidlPatchObserved =
                mainSnapshot.requestCount > mainRequestBaseline
                && mainSnapshot.responseCount > mainResponseBaseline
            mainPushObserved =
                mainSnapshot.pushMessageCount > mainPushBaseline
            guard mainUidlPatchObserved, mainPushObserved else {
                retryOrFail(
                    code: "fileDialogOutcomesMainUidlTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            dispatchBrowserClick(
                in: main,
                buttonId:
                    "framework-flow-file-dialog-outcomes-open-secondary",
                stage: "secondary-open",
                next: .secondaryOpening,
                failureCode: "fileDialogOutcomesSecondaryOpenFailed")
        case .secondaryOpening:
            guard let secondary = host.flowIpcWindowContext(
                    Self.secondaryWindowId),
                  secondary.navigationFinished else {
                retryOrFail(code: "fileDialogOutcomesSecondaryWindowTimeout")
                return
            }
            inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot,
                      secondarySnapshot.errorFree,
                      secondarySnapshot.deniedRouteReady,
                      self.scopeIsolated(mainSnapshot, secondarySnapshot),
                      secondarySnapshot.deniedStatus
                        == "File dialog denied: pending",
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty else {
                    self.retryOrFail(
                        code: "fileDialogOutcomesSecondaryRouteTimeout",
                        details: secondarySnapshot?.tracePayload(
                            prefix: "secondary") ?? [:])
                    return
                }
                self.secondarySessionId = secondarySnapshot.sessionId
                self.secondaryRequestBaseline = secondarySnapshot.requestCount
                self.secondaryResponseBaseline = secondarySnapshot.responseCount
                self.secondaryPushBaseline =
                    secondarySnapshot.pushMessageCount
                self.phase = .secondaryReady
                self.schedulePoll(after: 0.1)
            }
        case .secondaryReady:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard self.exactSecondaryScope(secondarySnapshot),
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty else {
                    self.retryOrFail(
                        code: "fileDialogOutcomesSecondaryScopeTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.performNativeClick(
                    in: secondary,
                    buttonId:
                        "framework-flow-file-dialog-outcomes-invoke-denied",
                    stage: "secondary-denied",
                    next: .deniedInvoked,
                    failureCode: "fileDialogOutcomesDeniedInvokeFailed")
            }
        case .deniedInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.recordPointer(
                    stage: "secondary-denied",
                    snapshot: secondarySnapshot)
                guard self.pointerObserved(
                        stage: "secondary-denied",
                        snapshot: secondarySnapshot),
                      secondarySnapshot.deniedStatus == Self.deniedStatus,
                      secondary.window.attachedSheet == nil,
                      Self.visibleOpenPanels().isEmpty,
                      mainSnapshot.firstStatus == self.firstStatus,
                      mainSnapshot.concurrentStatus == self.concurrentStatus else {
                    self.retryOrFail(
                        code: "fileDialogOutcomesDeniedResultTimeout",
                        details: self.mergedPayload(
                            mainSnapshot: mainSnapshot,
                            main: main,
                            secondarySnapshot: secondarySnapshot))
                    return
                }
                self.deniedOutcomeObserved = true
                self.deniedPanelAbsentObserved = true
                self.secondaryCapabilityContextCapturedObserved = true
                self.secondaryBackgroundSessionAbsentObserved = true
                self.secondaryUidlPatchObserved =
                    secondarySnapshot.requestCount
                        > self.secondaryRequestBaseline
                    && secondarySnapshot.responseCount
                        > self.secondaryResponseBaseline
                self.secondaryPushObserved =
                    secondarySnapshot.pushMessageCount
                        > self.secondaryPushBaseline
                guard self.secondaryUidlPatchObserved,
                      self.secondaryPushObserved else {
                    self.retryOrFail(
                        code: "fileDialogOutcomesSecondaryPushTimeout",
                        details: secondarySnapshot.tracePayload(
                            prefix: "secondary"))
                    return
                }
                self.phase = .finishDrain
                self.schedulePoll(after: 0.5)
            }
        case .finishDrain:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                let balanced = mainSnapshot.requestCount
                        == mainSnapshot.responseCount
                    && secondarySnapshot.requestCount
                        == secondarySnapshot.responseCount
                let key = "\(mainSnapshot.requestCount):\(mainSnapshot.responseCount):\(secondarySnapshot.requestCount):\(secondarySnapshot.responseCount)"
                if balanced {
                    if self.drainKey == key {
                        self.drainBalancedPolls += 1
                    } else {
                        self.drainKey = key
                        self.drainBalancedPolls = 1
                    }
                } else {
                    self.drainKey = ""
                    self.drainBalancedPolls = 0
                }
                guard self.drainBalancedPolls >= 2 else {
                    self.retryOrFail(
                        code: "fileDialogOutcomesDrainTimeout",
                        details: [
                            "mainRequestCount": mainSnapshot.requestCount,
                            "mainResponseCount": mainSnapshot.responseCount,
                            "secondaryRequestCount":
                                secondarySnapshot.requestCount,
                            "secondaryResponseCount":
                                secondarySnapshot.responseCount,
                            "balancedPolls": self.drainBalancedPolls
                        ],
                        delay: 0.5)
                    return
                }
                self.finish(
                    main: main,
                    mainSnapshot: mainSnapshot,
                    secondary: secondary,
                    secondarySnapshot: secondarySnapshot)
            }
        }
    }

    private func withSecondary(
        mainSnapshot: Snapshot,
        completion: @escaping (FlowIpcWindowContext, Snapshot) -> Void
    ) {
        guard let secondary = host.flowIpcWindowContext(Self.secondaryWindowId),
              secondary.navigationFinished else {
            retryOrFail(code: "fileDialogOutcomesSecondaryWindowLost")
            return
        }
        inspect(secondary) { snapshot in
            guard let snapshot, snapshot.errorFree else {
                self.retryOrFail(
                    code: "fileDialogOutcomesSecondaryInspectionFailed",
                    details: snapshot?.tracePayload(prefix: "secondary") ?? [:])
                return
            }
            completion(secondary, snapshot)
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
            let dispatchPointer = {
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
                let directToWebView = stage == "main-concurrent"
                    && context.window.attachedSheet != nil
                trace("recv", "flowFileDialogOutcomes.nativePointerDispatch", [
                    "inputMode": directToWebView
                        ? "appkit-webview-pointer-event"
                        : "appkit-window-pointer-event",
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
                if directToWebView {
                    context.webView.mouseDown(with: down)
                } else {
                    context.window.sendEvent(down)
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    if directToWebView {
                        context.webView.mouseUp(with: up)
                    } else {
                        context.window.sendEvent(up)
                    }
                    if !self.nativePointerStages.contains(stage) {
                        self.nativePointerStages.append(stage)
                    }
                    self.phase = next
                    self.schedulePoll(after: 0.1)
                }
            }
            if stage == "main-concurrent",
               context.window.attachedSheet != nil {
                dispatchPointer()
            } else {
                FlowIpcNativeInputActivation.prepare(
                    context: context,
                    deadline: self.deadline,
                    onFailure: { details in
                        self.fail(code: failureCode, details: details)
                    },
                    completion: dispatchPointer)
            }
        }
    }

    private func recordPointer(stage: String, snapshot: Snapshot) {
        guard let observation = snapshot.pointerStages[stage] else { return }
        if observation.receipt,
           !nativePointerReceiptStages.contains(stage) {
            nativePointerReceiptStages.append(stage)
        }
        if observation.trusted,
           !nativePointerTrustedStages.contains(stage) {
            nativePointerTrustedStages.append(stage)
        }
    }

    private func pointerObserved(stage: String, snapshot: Snapshot) -> Bool {
        snapshot.pointerStages[stage]?.receipt == true
            && snapshot.pointerStages[stage]?.trusted == true
    }

    private func scopeIsolated(_ main: Snapshot, _ secondary: Snapshot) -> Bool {
        exactMainScope(main)
            && secondary.windowId == Self.secondaryWindowId
            && secondary.uiId == Self.secondaryWindowId
            && opaqueSession(secondary.sessionId)
            && main.sessionId != secondary.sessionId
    }

    private func exactMainScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == "main"
            && snapshot.sessionId == "flow-ipc-session"
            && snapshot.uiId == "main"
    }

    private func exactSecondaryScope(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == Self.secondaryWindowId
            && snapshot.uiId == Self.secondaryWindowId
            && snapshot.sessionId == secondarySessionId
            && opaqueSession(snapshot.sessionId)
    }

    private func opaqueSession(_ sessionId: String) -> Bool {
        let prefix = "flow-ipc-window:"
        guard sessionId.hasPrefix(prefix) else { return false }
        return UUID(uuidString: String(sessionId.dropFirst(prefix.count))) != nil
    }

    private static func visibleOpenPanels() -> [NSOpenPanel] {
        NSApplication.shared.windows.compactMap { $0 as? NSOpenPanel }
            .filter(\.isVisible)
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
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(
        main: FlowIpcWindowContext,
        mainSnapshot: Snapshot,
        secondary: FlowIpcWindowContext,
        secondarySnapshot: Snapshot
    ) {
        let expectedPointerStages = [
            "main-first", "main-concurrent", "secondary-denied"
        ]
        let expectedBrowserStages = ["route-navigate", "secondary-open"]
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode": mainSnapshot.firstMeaningfulPaint
                && secondarySnapshot.firstMeaningfulPaint,
            "rootRendered": mainSnapshot.rootRendered
                && secondarySnapshot.rootRendered,
            "mainFlowClientOwned": mainSnapshot.flowClientCount > 0,
            "secondaryFlowClientOwned": secondarySnapshot.flowClientCount > 0,
            "mainOpaqueRequestObserved": mainUidlPatchObserved,
            "mainOpaqueResponseObserved": mainUidlPatchObserved,
            "secondaryOpaqueRequestObserved": secondaryUidlPatchObserved,
            "secondaryOpaqueResponseObserved": secondaryUidlPatchObserved,
            "mainJavaListenerObserved":
                mainSnapshot.startStatus
                    == "File dialog start: client=true "
                        + "capabilityContextCaptured=true change=1",
            "secondaryJavaListenerObserved": deniedOutcomeObserved,
            "capabilityContextCapturedObserved":
                mainCapabilityContextCapturedObserved
                && secondaryCapabilityContextCapturedObserved,
            "backgroundSessionAbsentObserved":
                mainBackgroundSessionAbsentObserved
                && secondaryBackgroundSessionAbsentObserved,
            "mainCapabilityContextCapturedObserved":
                mainCapabilityContextCapturedObserved,
            "mainBackgroundSessionAbsentObserved":
                mainBackgroundSessionAbsentObserved,
            "secondaryCapabilityContextCapturedObserved":
                secondaryCapabilityContextCapturedObserved,
            "secondaryBackgroundSessionAbsentObserved":
                secondaryBackgroundSessionAbsentObserved,
            "mainUidlPatchObserved": mainUidlPatchObserved,
            "mainPushObserved": mainPushObserved,
            "secondaryUidlPatchObserved": secondaryUidlPatchObserved,
            "secondaryPushObserved": secondaryPushObserved,
            "firstPanelObserved": firstPanelObserved,
            "firstPanelAttachedObserved": firstPanelAttachedObserved,
            "firstPanelVisibleObserved": firstPanelVisibleObserved,
            "singlePanelDuringConcurrentObserved":
                singlePanelDuringConcurrentObserved,
            "secondPanelAbsentObserved": secondPanelAbsentObserved,
            "normalCancelInvoked": normalCancelInvoked,
            "testPanelCancelInvoked": true,
            "testAutoCancelHookEnabled": false,
            "firstPanelDetachedObserved": firstPanelDetachedObserved,
            "firstPanelHiddenObserved": firstPanelHiddenObserved,
            "firstCanceledOutcomeObserved": firstCanceledOutcomeObserved,
            "concurrentErrorOutcomeObserved": concurrentErrorOutcomeObserved,
            "deniedOutcomeObserved": deniedOutcomeObserved,
            "deniedPanelAbsentObserved": deniedPanelAbsentObserved,
            "permissionPayloadPreservedObserved":
                mainSnapshot.firstStatus == firstStatus
                && mainSnapshot.concurrentStatus == concurrentStatus
                && secondarySnapshot.deniedStatus == Self.deniedStatus,
            "scopeIsolationObserved":
                scopeIsolated(mainSnapshot, secondarySnapshot),
            "nativePointerObserved":
                nativePointerStages == expectedPointerStages
                && nativePointerReceiptStages == expectedPointerStages,
            "nativePointerTrustedObserved":
                nativePointerTrustedStages == expectedPointerStages,
            "browserProgrammaticSetupObserved":
                browserProgrammaticStages == expectedBrowserStages,
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain":
                mainSnapshot.firstStatus == firstStatus
                && mainSnapshot.concurrentStatus == concurrentStatus
                && secondarySnapshot.deniedStatus == Self.deniedStatus,
            "errorFree": mainSnapshot.errorFree
                && secondarySnapshot.errorFree,
            "command": "fileDialog.openFile",
            "normalCancelOutcome": "CANCELED",
            "concurrentOutcome": "ERROR",
            "deniedOutcome": "DENIED",
            "mainWindowId": mainSnapshot.windowId,
            "mainSessionId": mainSnapshot.sessionId,
            "mainUiId": mainSnapshot.uiId,
            "secondaryWindowId": secondarySnapshot.windowId,
            "secondarySessionId": secondarySnapshot.sessionId,
            "secondaryUiId": secondarySnapshot.uiId,
            "firstStatus": mainSnapshot.firstStatus,
            "concurrentStatus": mainSnapshot.concurrentStatus,
            "deniedStatus": secondarySnapshot.deniedStatus,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                nativePointerTrustedStages.count,
            "nativePointerStages": nativePointerStages.joined(separator: ","),
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ","),
            "mainRequestCount": mainSnapshot.requestCount,
            "mainResponseCount": mainSnapshot.responseCount,
            "mainPushBaseline": mainPushBaseline,
            "mainPushMessageCount": mainSnapshot.pushMessageCount,
            "secondaryRequestCount": secondarySnapshot.requestCount,
            "secondaryResponseCount": secondarySnapshot.responseCount,
            "secondaryPushBaseline": secondaryPushBaseline,
            "secondaryPushMessageCount":
                secondarySnapshot.pushMessageCount,
            "mainRecoveryGeneration": main.recoveryGeneration,
            "secondaryRecoveryGeneration": secondary.recoveryGeneration
        ]
        terminal = true
        trace("recv", "flowFileDialogOutcomes.frameworkAcceptance", payload)
        secondary.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            main.window.close()
        }
    }

    private func mergedPayload(
        mainSnapshot: Snapshot,
        main: FlowIpcWindowContext,
        secondarySnapshot: Snapshot? = nil
    ) -> [String: Any] {
        var payload = mainSnapshot.tracePayload(prefix: "main")
        if let secondarySnapshot {
            payload.merge(secondarySnapshot.tracePayload(prefix: "secondary")) {
                _, replacement in replacement
            }
        }
        payload["mainAttachedSheet"] = main.window.attachedSheet != nil
        payload["visiblePanelCount"] = Self.visibleOpenPanels().count
        payload["nativePointerStages"] = nativePointerStages.joined(separator: ",")
        return payload
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
        let mainRouteReady: Bool
        let deniedRouteReady: Bool
        let startStatus: String
        let firstStatus: String
        let concurrentStatus: String
        let windowStatus: String
        let deniedStatus: String
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

        func tracePayload(prefix: String) -> [String: Any] {
            [
                "\(prefix)WindowId": windowId,
                "\(prefix)SessionId": sessionId,
                "\(prefix)UiId": uiId,
                "\(prefix)RootRendered": rootRendered,
                "\(prefix)FirstMeaningfulPaint": firstMeaningfulPaint,
                "\(prefix)MainRouteReady": mainRouteReady,
                "\(prefix)DeniedRouteReady": deniedRouteReady,
                "\(prefix)StartStatus": startStatus,
                "\(prefix)FirstStatus": firstStatus,
                "\(prefix)ConcurrentStatus": concurrentStatus,
                "\(prefix)WindowStatus": windowStatus,
                "\(prefix)DeniedStatus": deniedStatus,
                "\(prefix)RequestCount": requestCount,
                "\(prefix)ResponseCount": responseCount,
                "\(prefix)PushMessageCount": pushMessageCount,
                "\(prefix)FlowClientCount": flowClientCount,
                "\(prefix)PointerStages": pointerStages.keys.sorted()
                    .joined(separator: ","),
                "\(prefix)BrowserError": browserError,
                "\(prefix)RequestError": requestError,
                "\(prefix)ResponseError": responseError,
                "\(prefix)PushError": pushError
            ]
        }
    }

    private static func resultStatus(
        prefix: String,
        outcome: String,
        success: Bool,
        canceled: String,
        error: String,
        decision: String,
        reason: String,
        scope: String,
        commands: String
    ) -> String {
        "\(prefix): outcome=\(outcome) selected=none cancelReason=none "
            + "success=\(success) canceled=\(canceled) error=\(error) "
            + "permissionMode=manifest-enforced "
            + "permissionDecision=\(decision) permissionReason=\(reason) "
            + "permissionScope=\(scope) permissionManifestLoaded=true "
            + "permissionManifestCommands=\(commands) "
            + "command=fileDialog.openFile"
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
          const marker = window.VaadinDesktopFileDialogOutcomesPointer ||= {
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
      const mainRoute = document.querySelector(
        '#framework-flow-file-dialog-outcomes-route');
      const deniedRoute = document.querySelector(
        '#framework-flow-file-dialog-denied-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer =
        window.VaadinDesktopFileDialogOutcomesPointer || {stages:{}};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root')
          || !!mainRoute || !!deniedRoute,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        mainRouteReady:
          window.location.pathname.endsWith('/flow-file-dialog-outcomes')
          && !!mainRoute
          && !!document.querySelector(
            '#framework-flow-file-dialog-outcomes-invoke-first')
          && !!document.querySelector(
            '#framework-flow-file-dialog-outcomes-invoke-concurrent')
          && !!document.querySelector(
            '#framework-flow-file-dialog-outcomes-open-secondary'),
        deniedRouteReady:
          window.location.pathname.endsWith('/flow-file-dialog-denied')
          && !!deniedRoute
          && !!document.querySelector(
            '#framework-flow-file-dialog-outcomes-invoke-denied')
          && !document.querySelector(
            '#framework-flow-file-dialog-outcomes-invoke-first')
          && !document.querySelector(
            '#framework-flow-file-dialog-outcomes-invoke-concurrent'),
        startStatus: String(document.querySelector(
          '#framework-flow-file-dialog-outcomes-start-status')
          ?.textContent || ''),
        firstStatus: String(document.querySelector(
          '#framework-flow-file-dialog-outcomes-first-status')
          ?.textContent || ''),
        concurrentStatus: String(document.querySelector(
          '#framework-flow-file-dialog-outcomes-concurrent-status')
          ?.textContent || ''),
        windowStatus: String(document.querySelector(
          '#framework-flow-file-dialog-outcomes-window-status')
          ?.textContent || ''),
        deniedStatus: String(document.querySelector(
          '#framework-flow-file-dialog-outcomes-denied-status')
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
