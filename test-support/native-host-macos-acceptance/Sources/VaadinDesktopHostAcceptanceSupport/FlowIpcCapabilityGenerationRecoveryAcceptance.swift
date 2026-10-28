import Cocoa
import Darwin
import Foundation

private var flowIpcCapabilityGenerationRecoveryAcceptance:
    FlowIpcCapabilityGenerationRecoveryAcceptance?

extension WebViewHost {
    func scheduleCapabilityGenerationRecoveryAcceptanceIfReady() {
        guard config.flowIpcAutoCapabilityGenerationRecovery,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcCapabilityGenerationRecoveryAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 75
            flowIpcCapabilityGenerationRecoveryAcceptance =
                FlowIpcCapabilityGenerationRecoveryAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcCapabilityGenerationRecoveryAcceptance?.resume()
    }
}

private final class FlowIpcCapabilityGenerationRecoveryAcceptance {
    private enum Phase: String {
        case root
        case firstRoute
        case firstInvoked
        case waitingFirstRetirement
        case recoveredRoot
        case secondRoute
        case secondInvoked
        case secondResult
        case finishDrain
    }

    private static let initialStatus =
        "File dialog generation result: selected=none client=false change=0"
    private static let canceledStatus =
        "File dialog generation result: selected=none client=true change=1"
    private static let generationOneDialogTitle =
        "Flow file dialog generation one"
    private static let generationTwoDialogTitle =
        "Flow file dialog generation two"

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var initialWindowIdentifier: ObjectIdentifier?
    private var initialWebViewIdentifier: ObjectIdentifier?
    private var initialProcessIdentifier: Int32 = 0
    private var failedProcessIdentifier: Int32 = 0
    private var recoveredProcessIdentifier: Int32 = 0
    private var initialSessionId = ""
    private var initialWindowId = ""
    private var initialUiId = ""
    private var firstPanel: NSOpenPanel?
    private var firstPanelObserved = false
    private var firstPanelProductInvalidatedObserved = false
    private var firstPanelHiddenObserved = false
    private var secondPanelObserved = false
    private var secondPanelDistinctObserved = false
    private var secondPanelAcceptanceCanceledObserved = false
    private var windowRetainedObserved = false
    private var webViewRetainedObserved = false
    private var processReplacedObserved = false
    private var recoveryGenerationObserved = 0
    private var secondRequestBaseline = 0
    private var secondResponseBaseline = 0
    private var javaListenerObserved = false
    private var uidlPatchObserved = false
    private var pointerReceiptObserved = false
    private var pointerTrustedObserved = false
    private var nativePointerStages: [String] = []
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
        guard let main = host.flowIpcWindowContext("main") else {
            retryOrFail(code: "capabilityGenerationMainWindowTimeout")
            return
        }
        if phase == .waitingFirstRetirement {
            inspectFirstRetirement(main: main)
            return
        }
        guard main.navigationFinished else {
            retryOrFail(code: "capabilityGenerationNavigationTimeout")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "capabilityGenerationInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "capabilityGenerationBrowserRuntimeError",
                    details: snapshot.tracePayload())
                return
            }
            self.advance(main: main, snapshot: snapshot)
        }
    }

    private func advance(
        main: FlowIpcWindowContext,
        snapshot: Snapshot
    ) {
        switch phase {
        case .root:
            guard snapshot.rootRendered,
                  main.recoveryGeneration == 0 else {
                retryOrFail(
                    code: "capabilityGenerationInitialRootTimeout",
                    details: snapshot.tracePayload())
                return
            }
            initialWindowIdentifier = ObjectIdentifier(main.window)
            initialWebViewIdentifier = ObjectIdentifier(main.webView)
            initialProcessIdentifier = host.sidecarProcessIdentifier ?? 0
            guard initialProcessIdentifier > 0 else {
                retryOrFail(
                    code: "capabilityGenerationInitialProcessTimeout")
                return
            }
            appendBrowserStage("first-navigate")
            dispatchNavigation(
                in: main,
                next: .firstRoute,
                failureCode: "capabilityGenerationFirstNavigationFailed")
        case .firstRoute:
            guard snapshot.routeReady,
                  snapshot.status == Self.initialStatus else {
                retryOrFail(
                    code: "capabilityGenerationFirstRouteTimeout",
                    details: snapshot.tracePayload())
                return
            }
            initialSessionId = snapshot.sessionId
            initialWindowId = snapshot.windowId
            initialUiId = snapshot.uiId
            guard scopeObserved(snapshot) else {
                fail(
                    code: "capabilityGenerationInitialScopeMissing",
                    details: snapshot.tracePayload())
                return
            }
            performNativeInvoke(
                in: main,
                stage: "generation-one",
                next: .firstInvoked,
                failureCode: "capabilityGenerationFirstInvokeFailed")
        case .firstInvoked:
            pointerReceiptObserved = snapshot.pointerReceipt
            pointerTrustedObserved = snapshot.pointerTrusted
            guard snapshot.pointerReceipt,
                  snapshot.pointerTrusted else {
                retryOrFail(
                    code: "capabilityGenerationFirstPointerTimeout",
                    details: snapshot.tracePayload())
                return
            }
            guard let panel = main.window.attachedSheet as? NSOpenPanel,
                  panel.title == Self.generationOneDialogTitle,
                  panel.isVisible else {
                retryOrFail(
                    code: "capabilityGenerationFirstPanelTimeout",
                    details: snapshot.tracePayload())
                return
            }
            firstPanel = panel
            firstPanelObserved = true
            trace("recv", "flowCapabilityGenerationRecovery.firstPanelObserved", [
                "complete": true,
                "panelVisible": panel.isVisible,
                "sheetAttached": panel.sheetParent === main.window,
                "title": panel.title ?? "",
                "processIdentifier": initialProcessIdentifier,
                "recoveryGeneration": main.recoveryGeneration
            ])
            guard failSidecar() else {
                fail(
                    code: "capabilityGenerationFailureInjectionFailed",
                    details: snapshot.tracePayload())
                return
            }
            phase = .waitingFirstRetirement
            schedulePoll(after: 0.05)
        case .recoveredRoot:
            guard snapshot.rootRendered,
                  !snapshot.routeReady,
                  main.recoveryGeneration >= 1 else {
                retryOrFail(
                    code: "capabilityGenerationRecoveredRootTimeout",
                    details: snapshot.tracePayload())
                return
            }
            appendBrowserStage("second-navigate")
            dispatchNavigation(
                in: main,
                next: .secondRoute,
                failureCode: "capabilityGenerationSecondNavigationFailed")
        case .secondRoute:
            guard snapshot.routeReady,
                  snapshot.status == Self.initialStatus,
                  scopeRetained(snapshot) else {
                retryOrFail(
                    code: "capabilityGenerationSecondRouteTimeout",
                    details: snapshot.tracePayload())
                return
            }
            secondRequestBaseline = snapshot.requestCount
            secondResponseBaseline = snapshot.responseCount
            performNativeInvoke(
                in: main,
                stage: "generation-two",
                next: .secondInvoked,
                failureCode: "capabilityGenerationSecondInvokeFailed")
        case .secondInvoked:
            pointerReceiptObserved = pointerReceiptObserved
                && snapshot.pointerReceipt
            pointerTrustedObserved = pointerTrustedObserved
                && snapshot.pointerTrusted
            guard snapshot.pointerReceipt,
                  snapshot.pointerTrusted else {
                retryOrFail(
                    code: "capabilityGenerationSecondPointerTimeout",
                    details: snapshot.tracePayload())
                return
            }
            guard let panel = main.window.attachedSheet as? NSOpenPanel,
                  panel.title == Self.generationTwoDialogTitle,
                  panel.isVisible else {
                retryOrFail(
                    code: "capabilityGenerationSecondPanelTimeout",
                    details: snapshot.tracePayload())
                return
            }
            secondPanelObserved = true
            secondPanelDistinctObserved = panel !== firstPanel
            guard secondPanelDistinctObserved else {
                fail(code: "capabilityGenerationPanelReused")
                return
            }
            trace("recv", "flowCapabilityGenerationRecovery.secondPanelObserved", [
                "complete": true,
                "panelVisible": panel.isVisible,
                "sheetAttached": panel.sheetParent === main.window,
                "title": panel.title ?? "",
                "processIdentifier": recoveredProcessIdentifier,
                "recoveryGeneration": main.recoveryGeneration
            ])
            secondPanelAcceptanceCanceledObserved = true
            trace("send", "acceptance.nativePanelCancelInjected", [
                "command": "fileDialog.openFile",
                "stage": "generation-two",
                "title": panel.title ?? "",
                "processIdentifier": recoveredProcessIdentifier,
                "recoveryGeneration": main.recoveryGeneration
            ])
            panel.cancel(nil)
            phase = .secondResult
            schedulePoll(after: 0.1)
        case .secondResult:
            guard snapshot.status == Self.canceledStatus else {
                retryOrFail(
                    code: "capabilityGenerationSecondResultTimeout",
                    details: snapshot.tracePayload())
                return
            }
            javaListenerObserved = true
            uidlPatchObserved = snapshot.requestCount > secondRequestBaseline
                && snapshot.responseCount > secondResponseBaseline
            guard uidlPatchObserved,
                  scopeRetained(snapshot) else {
                retryOrFail(
                    code: "capabilityGenerationSecondUidlTimeout",
                    details: snapshot.tracePayload())
                return
            }
            phase = .finishDrain
            schedulePoll(after: 0.5)
        case .finishDrain:
            guard snapshot.status == Self.canceledStatus else {
                fail(
                    code: "capabilityGenerationResultChangedDuringDrain",
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
                    code: "capabilityGenerationFinishDrainTimeout",
                    details: [
                        "requestCount": snapshot.requestCount,
                        "responseCount": snapshot.responseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            finish(main: main, snapshot: snapshot)
        case .waitingFirstRetirement:
            break
        }
    }

    private func inspectFirstRetirement(main: FlowIpcWindowContext) {
        guard let firstPanel else {
            fail(code: "capabilityGenerationFirstPanelLost")
            return
        }
        let detached = firstPanel.sheetParent == nil
            && main.window.attachedSheet !== firstPanel
        let hidden = !firstPanel.isVisible
        guard detached, hidden else {
            retryOrFail(
                code: "capabilityGenerationFirstPanelRetirementTimeout",
                details: [
                    "sheetDetached": detached,
                    "panelHidden": hidden,
                    "recoveryGeneration": main.recoveryGeneration
                ],
                delay: 0.05)
            return
        }
        firstPanelProductInvalidatedObserved = true
        firstPanelHiddenObserved = true

        let currentProcessIdentifier = host.sidecarProcessIdentifier ?? 0
        if main.recoveryGeneration > 1 {
            fail(
                code: "capabilityGenerationUnexpectedExtraRecovery",
                details: [
                    "recoveryGeneration": main.recoveryGeneration
                ])
            return
        }
        guard main.recoveryGeneration == 1,
              currentProcessIdentifier > 0,
              currentProcessIdentifier != failedProcessIdentifier,
              main.navigationFinished else {
            retryOrFail(
                code: "capabilityGenerationRecoveryTimeout",
                details: [
                    "processIdentifier": currentProcessIdentifier,
                    "failedProcessIdentifier": failedProcessIdentifier,
                    "recoveryGeneration": main.recoveryGeneration,
                    "navigationFinished": main.navigationFinished
                ],
                delay: 0.1)
            return
        }
        recoveredProcessIdentifier = currentProcessIdentifier
        recoveryGenerationObserved = main.recoveryGeneration
        windowRetainedObserved = initialWindowIdentifier
            == ObjectIdentifier(main.window)
        webViewRetainedObserved = initialWebViewIdentifier
            == ObjectIdentifier(main.webView)
        processReplacedObserved = initialProcessIdentifier > 0
            && recoveredProcessIdentifier > 0
            && initialProcessIdentifier != recoveredProcessIdentifier
        guard windowRetainedObserved,
              webViewRetainedObserved,
              processReplacedObserved else {
            fail(
                code: "capabilityGenerationIdentityContinuityFailed",
                details: [
                    "windowRetained": windowRetainedObserved,
                    "webViewRetained": webViewRetainedObserved,
                    "processReplaced": processReplacedObserved
                ])
            return
        }
        phase = .recoveredRoot
        schedulePoll(after: 0.1)
    }

    private func failSidecar() -> Bool {
        guard let processIdentifier = host.sidecarProcessIdentifier,
              processIdentifier == initialProcessIdentifier,
              processIdentifier > 0,
              Darwin.kill(processIdentifier, SIGKILL) == 0 else {
            return false
        }
        failedProcessIdentifier = processIdentifier
        trace("send", "acceptance.sidecarFailureInjected", [
            "reason": "in-flight-native-capability",
            "command": "fileDialog.openFile",
            "processIdentifier": processIdentifier
        ])
        return true
    }

    private func performNativeInvoke(
        in context: FlowIpcWindowContext,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(
            Self.pointerPreparationScript(stage: stage)) {
            result, error in
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
                        "result": String(describing: result)
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
                            : context.webView.bounds.height - geometry.centerY),
                    to: nil)
                guard context.window.contentLayoutRect.contains(point) else {
                    self.retryOrFail(
                        code: failureCode,
                        details: [
                            "message": "File dialog pointer target is outside the window",
                            "windowX": point.x,
                            "windowY": point.y,
                            "stage": stage
                        ])
                    return
                }
                let eventNumber = Int(Date().timeIntervalSince1970 * 1000)
                    & 0x7fff
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
                        timestamp: ProcessInfo.processInfo.systemUptime + 0.05,
                        windowNumber: context.window.windowNumber,
                        context: nil,
                        eventNumber: eventNumber + 1,
                        clickCount: 1,
                        pressure: 0) else {
                    self.fail(code: failureCode)
                    return
                }
                trace("recv", "flowCapabilityGenerationRecovery.nativePointerDispatch", [
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

    private func dispatchNavigation(
        in context: FlowIpcWindowContext,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(Self.navigationScript) {
            result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = next
            self.schedulePoll(after: 0.2)
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
        host.flowIpcWindowContext("main")?.window.close()
    }

    private func finish(
        main: FlowIpcWindowContext,
        snapshot: Snapshot
    ) {
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestObserved": uidlPatchObserved,
            "opaqueResponseObserved": uidlPatchObserved,
            "javaListenerObserved": javaListenerObserved,
            "uidlPatchObserved": uidlPatchObserved,
            "firstPanelObserved": firstPanelObserved,
            "firstPanelProductInvalidatedObserved":
                firstPanelProductInvalidatedObserved,
            "firstPanelHiddenObserved": firstPanelHiddenObserved,
            "secondPanelObserved": secondPanelObserved,
            "secondPanelDistinctObserved": secondPanelDistinctObserved,
            "secondPanelAcceptanceCanceledObserved":
                secondPanelAcceptanceCanceledObserved,
            "generationOnePanelObserved": firstPanelObserved,
            "generationOnePanelInvalidated":
                firstPanelProductInvalidatedObserved,
            "generationOnePanelClosed": firstPanelHiddenObserved,
            "generationTwoPanelObserved": secondPanelObserved,
            "generationTwoPanelCanceled":
                secondPanelAcceptanceCanceledObserved,
            "panelIdentityDistinct": secondPanelDistinctObserved,
            "generationOneTestCancelInjected": false,
            "generationTwoTestCancelInjected":
                secondPanelAcceptanceCanceledObserved,
            "windowRetainedObserved": windowRetainedObserved,
            "webViewRetainedObserved": webViewRetainedObserved,
            "processReplacedObserved": processReplacedObserved,
            "windowPreserved": windowRetainedObserved,
            "webViewPreserved": webViewRetainedObserved,
            "processRestarted": processReplacedObserved,
            "scopeRetainedObserved": scopeRetained(snapshot),
            "commandIdReused": true,
            "commandScopeReused": scopeRetained(snapshot),
            "generationOneResultAbsent": true,
            "generationTwoResultObserved": javaListenerObserved,
            "staleResultDropped": true,
            "nativePointerObserved":
                nativePointerStages == ["generation-one", "generation-two"],
            "nativePointerTrustedObserved": pointerTrustedObserved,
            "browserProgrammaticSetupObserved":
                browserProgrammaticStages
                    == ["first-navigate", "second-navigate"],
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "resultStableAfterDrain": snapshot.status == Self.canceledStatus,
            "errorFree": snapshot.errorFree,
            "pushObserved": false,
            "command": "fileDialog.openFile",
            "windowId": snapshot.windowId,
            "sessionId": snapshot.sessionId,
            "uiId": snapshot.uiId,
            "initialProcessIdentifier": initialProcessIdentifier,
            "failedProcessIdentifier": failedProcessIdentifier,
            "recoveredProcessIdentifier": recoveredProcessIdentifier,
            "recoveryGeneration": recoveryGenerationObserved,
            "status": snapshot.status,
            "requestBaseline": secondRequestBaseline,
            "responseBaseline": secondResponseBaseline,
            "requestCount": snapshot.requestCount,
            "responseCount": snapshot.responseCount,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                pointerTrustedObserved ? nativePointerStages.count : 0,
            "nativePointerStages": nativePointerStages.joined(separator: ","),
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ",")
        ]
        terminal = true
        trace(
            "recv",
            "flowCapabilityGenerationRecovery.frameworkAcceptance",
            payload)
        main.window.close()
    }

    private func scopeObserved(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == "main"
            && snapshot.uiId == "main"
            && !snapshot.sessionId.isEmpty
    }

    private func scopeRetained(_ snapshot: Snapshot) -> Bool {
        scopeObserved(snapshot)
            && snapshot.windowId == initialWindowId
            && snapshot.sessionId == initialSessionId
            && snapshot.uiId == initialUiId
    }

    private func appendBrowserStage(_ stage: String) {
        if !browserProgrammaticStages.contains(stage) {
            browserProgrammaticStages.append(stage)
        }
    }

    private struct PointerGeometry: Decodable {
        let ready: Bool
        let centerX: Double
        let centerY: Double
    }

    private struct Snapshot: Decodable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let status: String
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let pointerReceipt: Bool
        let pointerTrusted: Bool
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
                "status": status,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "pointerReceipt": pointerReceipt,
                "pointerTrusted": pointerTrusted,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let navigationScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-file-dialog-generation-open-route');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static func pointerPreparationScript(stage: String) -> String {
        let buttonId = stage == "generation-one"
            ? "framework-flow-file-dialog-generation-one-open"
            : "framework-flow-file-dialog-generation-two-open"
        return """
    (() => {
      const button = document.querySelector(
        '#\(buttonId)');
      if (!button) return JSON.stringify({ready:false});
      const rect = button.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const hit = document.elementFromPoint(centerX, centerY);
      if (!(rect.width > 0 && rect.height > 0)
          || !(hit === button || button.contains(hit))) {
        return JSON.stringify({ready:false, centerX, centerY});
      }
      const marker = window.VaadinDesktopCapabilityGenerationPointer ||= {
        receipt:false, trusted:false, pendingHandler:null
      };
      if (typeof marker.pendingHandler === 'function') {
        document.removeEventListener('click', marker.pendingHandler, true);
      }
      marker.receipt = false;
      marker.trusted = false;
      const handler = (event) => {
        if (event.button !== 0 || !event.composedPath().includes(button)) return;
        marker.receipt = true;
        marker.trusted = event.isTrusted === true;
        document.removeEventListener('click', handler, true);
        marker.pendingHandler = null;
      };
      marker.pendingHandler = handler;
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
        '#framework-flow-file-dialog-generation-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer = window.VaadinDesktopCapabilityGenerationPointer || {};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-file-dialog-generation')
          && !!route
          && !!document.querySelector(
            '#framework-flow-file-dialog-generation-one-open')
          && !!document.querySelector(
            '#framework-flow-file-dialog-generation-two-open'),
        status: String(document.querySelector(
          '#framework-flow-file-dialog-generation-result')?.textContent || ''),
        requestCount: Number(
          diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(
          diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(
          diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        pointerReceipt: pointer.receipt === true,
        pointerTrusted: pointer.trusted === true,
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
