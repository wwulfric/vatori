import Cocoa
import Foundation

private var flowIpcCapabilityDefaultDenyAcceptance:
    FlowIpcCapabilityDefaultDenyAcceptance?

extension WebViewHost {
    func scheduleCapabilityDefaultDenyAcceptanceIfReady() {
        guard config.flowIpcAutoCapabilityDefaultDeny,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcCapabilityDefaultDenyAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 60
            flowIpcCapabilityDefaultDenyAcceptance =
                FlowIpcCapabilityDefaultDenyAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcCapabilityDefaultDenyAcceptance?.resume()
    }
}

private final class FlowIpcCapabilityDefaultDenyAcceptance {
    private enum Phase: String {
        case root
        case ready
        case invoked
        case finishDrain
    }

    private static let initialNativeTitle = "Flow IPC Framework App"
    private static let deniedStatus = "Capability permission result: success=false error=Command not allowed by desktop-capabilities.json permissionMode=manifest-enforced permissionDecision=denied permissionReason=manifest-denied permissionScope=window:main command=window.setTitle client=true change=1"

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var terminal = false
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var initialNativeTitle = ""
    private var javaListenerObserved = false
    private var uidlPatchObserved = false
    private var titleUnchangedObserved = false
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
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "capabilityDefaultDenyMainWindowTimeout")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "capabilityDefaultDenyMainInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "capabilityDefaultDenyRuntimeError",
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
            if snapshot.routeReady {
                phase = .ready
                schedulePoll(after: 0.1)
                return
            }
            guard snapshot.rootRendered else {
                retryOrFail(
                    code: "capabilityDefaultDenyRootTimeout",
                    details: snapshot.tracePayload())
                return
            }
            if !browserProgrammaticStages.contains("navigate") {
                browserProgrammaticStages.append("navigate")
            }
            dispatchNavigation(in: main)
        case .ready:
            guard snapshot.routeReady,
                  snapshot.status == "Capability permission result: pending" else {
                retryOrFail(
                    code: "capabilityDefaultDenyRouteTimeout",
                    details: snapshot.tracePayload())
                return
            }
            initialNativeTitle = main.window.title
            guard initialNativeTitle == Self.initialNativeTitle else {
                retryOrFail(
                    code: "capabilityDefaultDenyInitialTitleTimeout",
                    details: mergedPayload(snapshot: snapshot))
                return
            }
            requestBaseline = snapshot.requestCount
            responseBaseline = snapshot.responseCount
            performNativeInvoke(in: main)
        case .invoked:
            guard snapshot.status == Self.deniedStatus else {
                retryOrFail(
                    code: "capabilityDefaultDenyResultTimeout",
                    details: mergedPayload(snapshot: snapshot))
                return
            }
            javaListenerObserved = true
            uidlPatchObserved = snapshot.requestCount > requestBaseline
                && snapshot.responseCount > responseBaseline
            titleUnchangedObserved = main.window.title == initialNativeTitle
            pointerReceiptObserved = snapshot.pointerReceipt
            pointerTrustedObserved = snapshot.pointerTrusted
            guard uidlPatchObserved,
                  titleUnchangedObserved,
                  pointerReceiptObserved,
                  pointerTrustedObserved,
                  scopeObserved(snapshot) else {
                retryOrFail(
                    code: "capabilityDefaultDenyEvidenceTimeout",
                    details: mergedPayload(snapshot: snapshot))
                return
            }
            phase = .finishDrain
            schedulePoll(after: 0.5)
        case .finishDrain:
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
                    code: "capabilityDefaultDenyFinishDrainTimeout",
                    details: [
                        "requestCount": snapshot.requestCount,
                        "responseCount": snapshot.responseCount,
                        "balancedPolls": drainBalancedPolls
                    ],
                    delay: 0.5)
                return
            }
            finish(main: main, snapshot: snapshot)
        }
    }

    private func performNativeInvoke(in context: FlowIpcWindowContext) {
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
                    code: "capabilityDefaultDenyInvokeFailed",
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
                    self.fail(
                        code: "capabilityDefaultDenyInvokeFailed",
                        details: details)
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
                        code: "capabilityDefaultDenyInvokeFailed",
                        details: [
                            "message": "Capability default-deny pointer target is outside the window",
                            "windowX": point.x,
                            "windowY": point.y
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
                    self.fail(code: "capabilityDefaultDenyInvokeFailed")
                    return
                }
                trace("recv", "flowCapabilityDefaultDeny.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event",
                    "button": "primary",
                    "stage": "invoke",
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
                    if !self.nativePointerStages.contains("invoke") {
                        self.nativePointerStages.append("invoke")
                    }
                    self.phase = .invoked
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

    private func dispatchNavigation(in context: FlowIpcWindowContext) {
        context.webView.evaluateJavaScript(Self.navigationScript) {
            result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: "capabilityDefaultDenyNavigationFailed",
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = .ready
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
        let manifest = host.config.capabilityManifest
        let manifestCommands = manifest
            .commandsByWindow["main", default: []]
            .sorted()
            .joined(separator: ",")
        let manifestFallbackObserved = manifest.loaded
            && manifest.permissionMode == "manifest-enforced"
            && manifest.commandsByWindow.isEmpty
            && manifest.parseError.isEmpty
            && manifest.path
                == "$APP/Contents/Resources/desktop-capabilities.json"
        let scopeObserved = scopeObserved(snapshot)
        let browserSetupObserved = browserProgrammaticStages == ["navigate"]
        let nativePointerObserved = nativePointerStages == ["invoke"]
        let payload: [String: Any] = [
            "complete": true,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestObserved": uidlPatchObserved,
            "opaqueResponseObserved": uidlPatchObserved,
            "javaListenerObserved": javaListenerObserved,
            "uidlPatchObserved": uidlPatchObserved,
            "deniedResultObserved": snapshot.status == Self.deniedStatus,
            "titleUnchangedObserved": titleUnchangedObserved,
            "permissionPayloadPreservedObserved":
                snapshot.status == Self.deniedStatus,
            "scopeObserved": scopeObserved,
            "nativePointerObserved": nativePointerObserved,
            "nativePointerTrustedObserved": pointerTrustedObserved,
            "browserProgrammaticSetupObserved": browserSetupObserved,
            "balancedDrainObserved": drainBalancedPolls >= 2,
            "manifestFallbackObserved": manifestFallbackObserved,
            "errorFree": snapshot.errorFree,
            "pushObserved": false,
            "windowId": snapshot.windowId,
            "sessionId": snapshot.sessionId,
            "uiId": snapshot.uiId,
            "command": "window.setTitle",
            "permissionMode": manifest.permissionMode,
            "permissionDecision": "denied",
            "permissionReason": "manifest-denied",
            "permissionScope": "window:main",
            "permissionManifestLoaded": manifest.loaded,
            "permissionManifestPath": manifest.path,
            "permissionManifestCommands": manifestCommands,
            "status": snapshot.status,
            "initialNativeTitle": initialNativeTitle,
            "finalNativeTitle": main.window.title,
            "requestBaseline": requestBaseline,
            "responseBaseline": responseBaseline,
            "requestCount": snapshot.requestCount,
            "responseCount": snapshot.responseCount,
            "pushMessageCount": snapshot.pushMessageCount,
            "nativePointerEventCount": nativePointerStages.count,
            "nativePointerTrustedEventCount":
                pointerTrustedObserved ? 1 : 0,
            "nativePointerStages": nativePointerStages.joined(separator: ","),
            "browserProgrammaticStages":
                browserProgrammaticStages.joined(separator: ",")
        ]
        terminal = true
        trace("recv", "flowCapabilityDefaultDeny.frameworkAcceptance", payload)
        main.window.close()
    }

    private func scopeObserved(_ snapshot: Snapshot) -> Bool {
        snapshot.windowId == "main"
            && snapshot.uiId == "main"
            && !snapshot.sessionId.isEmpty
    }

    private func mergedPayload(snapshot: Snapshot) -> [String: Any] {
        var payload = snapshot.tracePayload()
        payload["windowTitle"] = host.flowIpcWindowContext("main")?.window.title
            ?? ""
        payload["initialNativeTitle"] = initialNativeTitle
        payload["nativePointerStages"] = nativePointerStages.joined(separator: ",")
        payload["browserProgrammaticStages"] =
            browserProgrammaticStages.joined(separator: ",")
        return payload
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
        '#framework-flow-capability-permission-open-route');
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
      const marker = window.VaadinDesktopCapabilityDefaultDenyPointer ||= {
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
      const pointer = window.VaadinDesktopCapabilityDefaultDenyPointer || {};
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
