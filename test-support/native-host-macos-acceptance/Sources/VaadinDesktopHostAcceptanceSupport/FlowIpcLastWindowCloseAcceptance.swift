import Cocoa
import Darwin
import Foundation

private var lastWindowCloseAcceptance: FlowIpcLastWindowCloseAcceptance?

extension WebViewHost {
    func scheduleLastWindowCloseAcceptanceIfReady() {
        guard config.transport == .flowIpc else { return }
        if lastWindowCloseAcceptance == nil {
            lastWindowCloseAcceptance = FlowIpcLastWindowCloseAcceptance(host: self)
        }
        lastWindowCloseAcceptance?.resume()
    }

    func observeLastWindowCloseTermination() {
        lastWindowCloseAcceptance?.observeTermination()
    }
}

private final class FlowIpcLastWindowCloseAcceptance {
    private enum Phase { case root, route, preparing, closing, failed }
    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var scheduled = false
    private var processIdentifier: Int32 = 0
    private var prepared = false
    private var finalPointerDispatched = false
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var quietKey = ""
    private var quietPolls = 0

    init(host: WebViewHost) {
        self.host = host
        deadline = Date().addingTimeInterval(45)
    }

    func resume() {
        guard !scheduled, phase != .closing, phase != .failed else { return }
        scheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            self.scheduled = false
            self.poll()
        }
    }

    private var identity: [String: Any] {
        ["windowId": "main", "sessionId": "flow-ipc-session", "uiId": "main",
         "processIdentifier": processIdentifier,
         "hostProcessIdentifier": ProcessInfo.processInfo.processIdentifier,
         "recoveryGeneration": 0]
    }

    func observeTermination() {
        var payload = identity
        // AppKit may call this inside windowWillClose, before the closing
        // NSWindow becomes invisible. Registry retirement is the ownership proof.
        payload["visibleWindowCount"] = NSApp.windows.filter { $0.isVisible }.count
        errno = 0
        let sidecarAbsent = processIdentifier > 0 && host.sidecarProcessIdentifier == nil
            && Darwin.kill(processIdentifier, 0) == -1 && errno == ESRCH
        payload["currentProcessIdentifier"] = host.sidecarProcessIdentifier ?? 0
        payload["originalSidecarAbsent"] = sidecarAbsent
        payload["originalWindowRetained"] = host.flowIpcWindowContext("main") != nil
        payload["preparationObserved"] = prepared
        payload["finalPointerDispatched"] = finalPointerDispatched
        payload["complete"] = prepared && finalPointerDispatched
            && (payload["originalWindowRetained"] as? Bool) == false
            && sidecarAbsent
        trace("recv", "flowLastWindowClose.applicationWillTerminate", payload)
    }

    private func poll() {
        guard Date() < deadline else { fail("deadline"); return }
        guard let context = host.mainWindowContext, context.navigationFinished else {
            resume()
            return
        }
        if processIdentifier == 0 { processIdentifier = host.sidecarProcessIdentifier ?? 0 }
        guard processIdentifier > 0, host.sidecarProcessIdentifier == processIdentifier,
              context.recoveryGeneration == 0 else { fail("continuity"); return }
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard error == nil, let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data),
                  snapshot.windowId == "main", snapshot.sessionId == "flow-ipc-session",
                  snapshot.uiId == "main", snapshot.errors.isEmpty else {
                self.fail("inspection")
                return
            }
            switch self.phase {
            case .root:
                guard snapshot.firstMeaningfulPaint, snapshot.flowClientCount == 1 else {
                    self.resume(); return
                }
                context.webView.evaluateJavaScript("""
                    (() => {
                      const b = document.querySelector('#framework-flow-last-window-close-open-route');
                      if (!b) return false;
                      b.click(); return true;
                    })()
                    """) { result, error in
                    guard error == nil, result as? Bool == true else {
                        self.resume(); return
                    }
                    self.phase = .route
                    self.resume()
                }
            case .route:
                guard snapshot.routeReady, snapshot.status == "Last window close: pending" else {
                    self.resume(); return
                }
                self.requestBaseline = snapshot.requestCount
                self.responseBaseline = snapshot.responseCount
                self.phase = .preparing
                self.click(context, stage: "prepare", suffix: "prepare") { self.resume() }
            case .preparing:
                guard snapshot.status == "Last window close: prepared=true client=true change=1",
                      snapshot.receipt, snapshot.trusted,
                      snapshot.requestCount > self.requestBaseline,
                      snapshot.responseCount > self.responseBaseline,
                      snapshot.requestCount == snapshot.responseCount else {
                    self.resume(); return
                }
                let key = "\(snapshot.requestCount):\(snapshot.responseCount)"
                self.quietPolls = key == self.quietKey ? self.quietPolls + 1 : 0
                self.quietKey = key
                guard self.quietPolls >= 2 else { self.resume(); return }
                var payload = self.identity
                payload["windowCount"] = NSApp.windows.filter { $0.isVisible }.count
                payload["routeReady"] = snapshot.routeReady
                payload["firstMeaningfulPaint"] = snapshot.firstMeaningfulPaint
                payload["flowClientCount"] = snapshot.flowClientCount
                payload["preparationStatus"] = snapshot.status
                payload["requestCount"] = snapshot.requestCount
                payload["responseCount"] = snapshot.responseCount
                payload["nativePointerEventCount"] = 1
                payload["nativePointerTrustedEventCount"] = 1
                payload["browserProgrammaticStages"] = "open-route"
                payload["originalSidecarRunning"] = Darwin.kill(self.processIdentifier, 0) == 0
                self.prepared = true
                trace("recv", "flowLastWindowClose.prepared", payload)
                self.phase = .closing
                self.click(context, stage: "close-last", suffix: "final") {
                    self.finalPointerDispatched = true
                    // Independent of both main dispatch and the product scheduler
                    // under test: the old nested-loop regression must fail boundedly.
                    // Fatal test-only exit closes the channel; Java handles stdin EOF.
                    DispatchQueue.global(qos: .utility).asyncAfter(deadline: .now() + 5) {
                        trace("send", "error", [
                            "code": "flowLastWindowCloseFailed",
                            "reason": "public-close-did-not-terminate"
                        ])
                        fflush(stdout)
                        Darwin.exit(1)
                    }
                }
            case .closing, .failed: break
            }
        }
    }

    private func click(
        _ context: FlowIpcWindowContext, stage: String, suffix: String,
        completion: @escaping () -> Void
    ) {
        let script = """
            (() => {
              const b = document.querySelector('#framework-flow-last-window-close-\(suffix)');
              if (!b || b.disabled) return JSON.stringify({ready:false,x:0,y:0});
              const r = b.getBoundingClientRect(), x = r.left+r.width/2, y = r.top+r.height/2;
              const hit = document.elementFromPoint(x,y);
              if (!(r.width>0 && r.height>0 && (hit===b || b.contains(hit))))
                return JSON.stringify({ready:false,x,y});
              if ('\(stage)' === 'prepare') {
                window.vatoriFinalClosePointer = {receipt:false,trusted:false};
                b.addEventListener('click', e => {
                  window.vatoriFinalClosePointer = {receipt:true,trusted:e.isTrusted===true};
                }, {capture:true,once:true});
              }
              return JSON.stringify({ready:true,x,y});
            })()
            """
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: data),
                  geometry.ready else { self.fail("pointer-geometry"); return }
            FlowIpcNativeInputActivation.prepare(context: context, deadline: self.deadline,
                onFailure: { _ in self.fail("activation") }) {
                let point = context.webView.convert(NSPoint(x: geometry.x,
                    y: context.webView.isFlipped ? geometry.y
                        : context.webView.bounds.height - geometry.y), to: nil)
                guard context.window.contentLayoutRect.contains(point),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: point,
                        modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                        windowNumber: context.window.windowNumber, context: nil,
                        eventNumber: 1, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: point,
                        modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime + 0.05,
                        windowNumber: context.window.windowNumber, context: nil,
                        eventNumber: 2, clickCount: 1, pressure: 0) else {
                    self.fail("pointer-dispatch"); return
                }
                var payload = context.scope.tracePayload
                payload["stage"] = stage
                payload["eventCount"] = 2
                payload["source"] = "appkit-window-mouse-events"
                trace("recv", "flowLastWindowClose.nativePointerDispatch", payload)
                context.window.sendEvent(down)
                context.window.sendEvent(up)
                completion()
            }
        }
    }

    private func fail(_ reason: String) {
        guard phase != .failed else { return }
        phase = .failed
        trace("send", "error", ["code": "flowLastWindowCloseFailed", "reason": reason])
        NSApp.perform(#selector(NSApplication.terminate(_:)), with: nil, afterDelay: 0)
    }

    private struct Geometry: Decodable { let ready: Bool; let x: Double; let y: Double }
    private struct Snapshot: Decodable {
        let windowId: String; let sessionId: String; let uiId: String
        let routeReady: Bool; let firstMeaningfulPaint: Bool; let flowClientCount: Int
        let status: String; let requestCount: Int; let responseCount: Int
        let receipt: Bool; let trusted: Bool; let errors: String
    }
    private static let inspection = """
        (() => {
          const c=window.VaadinDesktopHostContext||{}, d=window.VaadinDesktopIpcDiagnostics||{};
          const p=window.vatoriFinalClosePointer||{};
          const clients=Object.keys(window.Vaadin?.Flow?.clients||{}).filter(k=>k!=='TypeScript');
          return JSON.stringify({
            windowId:String(c.windowId||''),sessionId:String(c.sessionId||''),uiId:String(c.uiId||''),
            routeReady:location.pathname.endsWith('/flow-last-window-close')
              && !!document.querySelector('#framework-flow-last-window-close-route'),
            firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
            flowClientCount:clients.length,
            status:String(document.querySelector('#framework-flow-last-window-close-status')?.textContent||''),
            requestCount:Number(d.standardBundleOpaqueRequestCount||0),
            responseCount:Number(d.standardBundleOpaqueResponseCount||0),
            receipt:p.receipt===true,trusted:p.trusted===true,
            errors:[d.standardBundleBrowserError,d.standardBundleOpaqueRequestError,
              d.standardBundleOpaqueResponseError,d.standardBundlePushError].filter(Boolean).join('|')
          });
        })()
        """
}
