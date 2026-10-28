import Cocoa
import Foundation

private var flowIpcCapabilityPermissionAcceptance:
    FlowIpcCapabilityPermissionAcceptance?

extension WebViewHost {
    func scheduleCapabilityPermissionAcceptanceIfReady() {
        guard config.flowIpcAutoCapabilityPermission,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcCapabilityPermissionAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcCapabilityPermissionAcceptance =
                FlowIpcCapabilityPermissionAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcCapabilityPermissionAcceptance?.resume()
    }
}

private final class FlowIpcCapabilityPermissionAcceptance {
    private enum Phase: String {
        case root
        case mainReady
        case mainInvoked
        case secondaryOpening
        case secondaryReady
        case secondaryInvoked
        case finishDrain
    }

    private static let secondaryWindowId = "permission-secondary"
    private static let mainNativeTitle = "Capability permission allowed title"
    private static let secondaryNativeTitle = "Capability permission secondary"
    private static let mainStatus = "Capability permission result: success=true error=none permissionMode=manifest-enforced permissionDecision=allowed permissionReason=manifest-allow permissionScope=window:main command=window.setTitle client=true change=1"
    private static let secondaryStatus = "Capability permission result: success=false error=Command not allowed by desktop-capabilities.json permissionMode=manifest-enforced permissionDecision=denied permissionReason=manifest-denied permissionScope=window:permission-secondary command=window.setTitle client=true change=1"

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var secondaryRequestBaseline = 0
    private var secondaryResponseBaseline = 0
    private var mainListenerObserved = false
    private var secondaryListenerObserved = false
    private var mainUidlRoundTripObserved = false
    private var secondaryUidlRoundTripObserved = false
    private var mainTitleChangedObserved = false
    private var secondaryTitleUnchangedObserved = false
    private var secondaryInitialTitle = ""
    private var mainPointerReceiptObserved = false
    private var mainPointerTrustedObserved = false
    private var secondaryPointerReceiptObserved = false
    private var secondaryPointerTrustedObserved = false
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
        guard !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "capabilityPermissionMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(code: "capabilityPermissionMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "capabilityPermissionMainRuntimeError",
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
            if mainSnapshot.routeReady {
                phase = .mainReady
                schedulePoll(after: 0.1)
                return
            }
            guard mainSnapshot.rootRendered else {
                retryOrFail(
                    code: "capabilityPermissionRootTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            if !browserProgrammaticStages.contains("navigate") {
                browserProgrammaticStages.append("navigate")
            }
            dispatch(
                Self.navigationScript,
                in: main,
                next: .mainReady,
                failureCode: "capabilityPermissionNavigationFailed")
        case .mainReady:
            guard mainSnapshot.routeReady,
                  mainSnapshot.status == "Capability permission result: pending" else {
                retryOrFail(
                    code: "capabilityPermissionMainRouteTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainRequestBaseline = mainSnapshot.requestCount
            mainResponseBaseline = mainSnapshot.responseCount
            performNativeInvoke(
                in: main,
                stage: "main-invoke",
                next: .mainInvoked,
                failureCode: "capabilityPermissionMainInvokeFailed")
        case .mainInvoked:
            guard mainSnapshot.status == Self.mainStatus else {
                retryOrFail(
                    code: "capabilityPermissionMainResultTimeout",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }
            mainPointerReceiptObserved = mainSnapshot.pointerReceipt
            mainPointerTrustedObserved = mainSnapshot.pointerTrusted
            mainListenerObserved = true
            mainUidlRoundTripObserved =
                mainSnapshot.requestCount > mainRequestBaseline
                && mainSnapshot.responseCount > mainResponseBaseline
            mainTitleChangedObserved = main.window.title == Self.mainNativeTitle
            guard mainUidlRoundTripObserved, mainTitleChangedObserved,
                  mainPointerReceiptObserved, mainPointerTrustedObserved else {
                retryOrFail(
                    code: "capabilityPermissionMainEffectTimeout",
                    details: mergedPayload(mainSnapshot: mainSnapshot))
                return
            }
            if !browserProgrammaticStages.contains("open-secondary") {
                browserProgrammaticStages.append("open-secondary")
            }
            dispatch(
                Self.openSecondaryScript,
                in: main,
                next: .secondaryOpening,
                failureCode: "capabilityPermissionSecondaryOpenFailed")
        case .secondaryOpening:
            guard let secondary = host.flowIpcWindowContext(
                Self.secondaryWindowId),
                  secondary.navigationFinished else {
                retryOrFail(code: "capabilityPermissionSecondaryWindowTimeout")
                return
            }
            inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot,
                      secondarySnapshot.routeReady,
                      secondarySnapshot.errorFree else {
                    self.retryOrFail(
                        code: "capabilityPermissionSecondaryRouteTimeout",
                        details: secondarySnapshot?.tracePayload(
                            prefix: "secondary") ?? [:])
                    return
                }
                guard self.identityIsolated(mainSnapshot, secondarySnapshot) else {
                    self.fail(
                        code: "capabilityPermissionScopeIsolationFailed",
                        details: self.mergedPayload(
                            mainSnapshot: mainSnapshot,
                            secondarySnapshot: secondarySnapshot))
                    return
                }
                self.secondaryInitialTitle = secondary.window.title
                guard self.secondaryInitialTitle == Self.secondaryNativeTitle,
                      secondarySnapshot.status
                        == "Capability permission result: pending" else {
                    self.retryOrFail(
                        code: "capabilityPermissionSecondaryInitialStateTimeout",
                        details: self.mergedPayload(
                            mainSnapshot: mainSnapshot,
                            secondarySnapshot: secondarySnapshot))
                    return
                }
                self.phase = .secondaryReady
                self.schedulePoll(after: 0.1)
            }
        case .secondaryReady:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                self.secondaryRequestBaseline = secondarySnapshot.requestCount
                self.secondaryResponseBaseline = secondarySnapshot.responseCount
                self.performNativeInvoke(
                    in: secondary,
                    stage: "secondary-invoke",
                    next: .secondaryInvoked,
                    failureCode: "capabilityPermissionSecondaryInvokeFailed")
            }
        case .secondaryInvoked:
            withSecondary(mainSnapshot: mainSnapshot) {
                secondary, secondarySnapshot in
                guard secondarySnapshot.status == Self.secondaryStatus else {
                    self.retryOrFail(
                        code: "capabilityPermissionDeniedResultTimeout",
                        details: self.mergedPayload(
                            mainSnapshot: mainSnapshot,
                            secondarySnapshot: secondarySnapshot))
                    return
                }
                self.secondaryPointerReceiptObserved =
                    secondarySnapshot.pointerReceipt
                self.secondaryPointerTrustedObserved =
                    secondarySnapshot.pointerTrusted
                self.secondaryListenerObserved = true
                self.secondaryUidlRoundTripObserved =
                    secondarySnapshot.requestCount > self.secondaryRequestBaseline
                    && secondarySnapshot.responseCount
                        > self.secondaryResponseBaseline
                self.secondaryTitleUnchangedObserved =
                    self.secondaryInitialTitle == Self.secondaryNativeTitle
                    && secondary.window.title == Self.secondaryNativeTitle
                guard self.secondaryUidlRoundTripObserved,
                      self.secondaryTitleUnchangedObserved,
                      self.secondaryPointerReceiptObserved,
                      self.secondaryPointerTrustedObserved,
                      main.window.title == Self.mainNativeTitle,
                      mainSnapshot.status == Self.mainStatus else {
                    self.retryOrFail(
                        code: "capabilityPermissionDeniedEffectTimeout",
                        details: self.mergedPayload(
                            mainSnapshot: mainSnapshot,
                            secondarySnapshot: secondarySnapshot))
                    return
                }
                self.phase = .finishDrain
                self.schedulePoll(after: 0.5)
            }
        case .finishDrain:
            withSecondary(mainSnapshot: mainSnapshot) {
                _, secondarySnapshot in
                let requests = mainSnapshot.requestCount
                    + secondarySnapshot.requestCount
                let responses = mainSnapshot.responseCount
                    + secondarySnapshot.responseCount
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
                        code: "capabilityPermissionFinishDrainTimeout",
                        details: [
                            "requestCount": requests,
                            "responseCount": responses,
                            "balancedPolls": self.drainBalancedPolls
                        ],
                        delay: 0.5)
                    return
                }
                self.finish(
                    mainSnapshot: mainSnapshot,
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
            retryOrFail(code: "capabilityPermissionSecondaryWindowLost")
            return
        }
        inspect(secondary) { snapshot in
            guard let snapshot, snapshot.errorFree else {
                self.retryOrFail(
                    code: "capabilityPermissionSecondaryInspectionFailed",
                    details: snapshot?.tracePayload(prefix: "secondary") ?? [:])
                return
            }
            completion(secondary, snapshot)
        }
    }

    private func identityIsolated(_ main: Snapshot, _ secondary: Snapshot) -> Bool {
        main.windowId == "main"
            && secondary.windowId == Self.secondaryWindowId
            && main.uiId == "main"
            && secondary.uiId == Self.secondaryWindowId
            && !main.sessionId.isEmpty
            && !secondary.sessionId.isEmpty
            && main.sessionId != secondary.sessionId
    }

    private func performNativeInvoke(
        in context: FlowIpcWindowContext,
        stage: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(Self.pointerPreparationScript) {
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
                            "message": "Capability permission pointer target is outside the window",
                            "windowX": point.x,
                            "windowY": point.y,
                            "stage": stage
                        ])
                    return
                }
                let eventNumber = Int(Date().timeIntervalSince1970 * 1000) & 0x7fff
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
                trace("recv", "flowCapabilityPermission.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "stage": stage,
                    "webViewX": geometry.centerX,
                    "webViewY": geometry.centerY,
                    "windowX": point.x,
                    "windowY": point.y,
                    "activeApplication": NSApplication.shared.isActive,
                    "keyWindow": context.window.isKeyWindow,
                    "windowId": context.scope.windowId
                ])
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    context.window.sendEvent(up)
                    if !self.nativePointerStages.contains(stage) {
                        self.nativePointerStages.append(stage)
                    }
                    self.phase = next
                    self.schedulePoll(after: 0.2)
                }
            }
        }
    }

    private func inspect(
        _ context: FlowIpcWindowContext,
        completion: @escaping (Snapshot?) -> Void
    ) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
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

    private func dispatch(
        _ script: String,
        in context: FlowIpcWindowContext,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(script) { result, error in
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
        var payload = details
        payload["code"] = code
        payload["phase"] = phase.rawValue
        trace("send", "error", payload)
        closeWindows()
    }

    private func finish(
        mainSnapshot: Snapshot,
        secondarySnapshot: Snapshot
    ) {
        let identityIsolated = identityIsolated(mainSnapshot, secondarySnapshot)
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode": mainSnapshot.firstMeaningfulPaint
                && secondarySnapshot.firstMeaningfulPaint,
            "rootRendered": mainSnapshot.rootRendered
                && secondarySnapshot.rootRendered,
            "mainFlowClientOwned": mainSnapshot.flowClientCount > 0,
            "secondaryFlowClientOwned": secondarySnapshot.flowClientCount > 0,
            "mainOpaqueRequestObserved": mainUidlRoundTripObserved,
            "mainOpaqueResponseObserved": mainUidlRoundTripObserved,
            "secondaryOpaqueRequestObserved": secondaryUidlRoundTripObserved,
            "secondaryOpaqueResponseObserved": secondaryUidlRoundTripObserved,
            "pushObserved": mainSnapshot.pushMessageCount > 0
                && secondarySnapshot.pushMessageCount > 0,
            "errorFree": mainSnapshot.errorFree && secondarySnapshot.errorFree,
            "mainAllowedResultObserved": mainListenerObserved,
            "secondaryDeniedResultObserved": secondaryListenerObserved,
            "permissionPayloadPreservedObserved":
                secondarySnapshot.status == Self.secondaryStatus,
            "mainTitleAppliedObserved": mainTitleChangedObserved,
            "secondaryTitleUnchangedObserved": secondaryTitleUnchangedObserved,
            "mainJavaListenerObserved": mainListenerObserved,
            "secondaryJavaListenerObserved": secondaryListenerObserved,
            "mainUidlPatchObserved": mainUidlRoundTripObserved,
            "secondaryUidlPatchObserved": secondaryUidlRoundTripObserved,
            "scopeIsolationObserved": identityIsolated,
            "mainWindowId": mainSnapshot.windowId,
            "mainSessionId": mainSnapshot.sessionId,
            "mainUiId": mainSnapshot.uiId,
            "secondaryWindowId": secondarySnapshot.windowId,
            "secondarySessionId": secondarySnapshot.sessionId,
            "secondaryUiId": secondarySnapshot.uiId,
            "command": "window.setTitle",
            "permissionMode": "manifest-enforced",
            "mainPermissionDecision": "allowed",
            "mainPermissionReason": "manifest-allow",
            "mainPermissionScope": "window:main",
            "secondaryPermissionDecision": "denied",
            "secondaryPermissionReason": "manifest-denied",
            "secondaryPermissionScope": "window:permission-secondary",
            "mainStatus": mainSnapshot.status,
            "secondaryStatus": secondarySnapshot.status,
            "mainNativeTitle": Self.mainNativeTitle,
            "secondaryInitialNativeTitle": secondaryInitialTitle,
            "secondaryFinalNativeTitle": Self.secondaryNativeTitle,
            "nativePointerObserved": nativePointerStages.count == 2,
            "nativePointerTrustedObserved": mainPointerTrustedObserved
                && secondaryPointerTrustedObserved,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                (mainPointerTrustedObserved ? 1 : 0)
                + (secondaryPointerTrustedObserved ? 1 : 0),
            "nativePointerStages": nativePointerStages.joined(separator: ","),
            "browserProgrammaticSetupObserved":
                browserProgrammaticStages.count == 2,
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ",")
        ]
        trace("recv", "flowCapabilityPermission.frameworkAcceptance", payload)
        closeWindows()
    }

    private func mergedPayload(
        mainSnapshot: Snapshot,
        secondarySnapshot: Snapshot? = nil
    ) -> [String: Any] {
        var payload = mainSnapshot.tracePayload(prefix: "main")
        if let secondarySnapshot {
            payload.merge(secondarySnapshot.tracePayload(prefix: "secondary")) {
                _, replacement in replacement
            }
        }
        payload["mainWindowTitle"] = host.flowIpcWindowContext("main")?.window.title ?? ""
        payload["secondaryWindowTitle"] = host.flowIpcWindowContext(
            Self.secondaryWindowId)?.window.title ?? ""
        payload["nativePointerStages"] = nativePointerStages.joined(separator: ",")
        return payload
    }

    private func closeWindows() {
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            self.host.flowIpcWindowContext("main")?.window.close()
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

        func tracePayload(prefix: String) -> [String: Any] {
            [
                "\(prefix)WindowId": windowId,
                "\(prefix)SessionId": sessionId,
                "\(prefix)UiId": uiId,
                "\(prefix)RootRendered": rootRendered,
                "\(prefix)FirstMeaningfulPaint": firstMeaningfulPaint,
                "\(prefix)RouteReady": routeReady,
                "\(prefix)Status": status,
                "\(prefix)RequestCount": requestCount,
                "\(prefix)ResponseCount": responseCount,
                "\(prefix)PushMessageCount": pushMessageCount,
                "\(prefix)FlowClientCount": flowClientCount,
                "\(prefix)PointerReceipt": pointerReceipt,
                "\(prefix)PointerTrusted": pointerTrusted,
                "\(prefix)BrowserError": browserError,
                "\(prefix)RequestError": requestError,
                "\(prefix)ResponseError": responseError,
                "\(prefix)PushError": pushError
            ]
        }
    }

    private static let navigationScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-capability-permission-open-route');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let openSecondaryScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-capability-permission-open-window');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let pointerPreparationScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-capability-permission-invoke');
      if (!button) return JSON.stringify({ready:false});
      const rect = button.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const hit = document.elementFromPoint(centerX, centerY);
      if (!(rect.width > 0 && rect.height > 0)
          || !(hit === button || button.contains(hit))) {
        return JSON.stringify({ready:false, centerX, centerY});
      }
      const marker = window.VaadinDesktopCapabilityPermissionPointer ||= {
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
      return JSON.stringify({
        ready:true,
        centerX,
        centerY
      });
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const context = window.VaadinDesktopHostContext || {};
      const route = document.querySelector(
        '#framework-flow-capability-permission-route');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      const pointer = window.VaadinDesktopCapabilityPermissionPointer || {};
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-capability-permission')
          && !!route
          && !!document.querySelector(
            '#framework-flow-capability-permission-invoke'),
        status: String(document.querySelector(
          '#framework-flow-capability-permission-result')?.textContent || ''),
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
