import AppKit
import Foundation

private var windowChurnStarted = false

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleWindowChurn(host: WebViewHost, context: FlowIpcWindowContext, timeout: Double) {
        guard context.scope == .main, !windowChurnStarted else { return }
        windowChurnStarted = true
        FlowIpcWindowChurnAcceptance(host, context, timeout).start()
    }
}

// Browser-programmatic public API scenario, not physical/native input evidence.
// The driver records observations; an independent wire verifier must certify them.
private final class FlowIpcWindowChurnAcceptance {
    let host: WebViewHost
    let main: FlowIpcWindowContext
    let pid: Int32
    let timeout: Double
    let runId = UUID().uuidString
    var terminal = false
    var sibling: FlowIpcWindowContext?
    var retiringSibling = false
    var owners: [FlowIpcWindowContext] = [] // retain identities, not a deallocation test
    var documents: Set<String> = []
    var mainDocument = ""
    var siblingDocument = ""
    var cycle = 0
    var tasks: [(@escaping () -> Void) -> Void] = []

    init(_ host: WebViewHost, _ main: FlowIpcWindowContext, _ timeout: Double) {
        self.host = host; self.main = main; self.timeout = timeout
        pid = host.sidecarProcessIdentifier ?? 0
    }
    func valid() -> Bool {
        !terminal && host.sidecarProcessIdentifier == pid && main.recoveryGeneration == 0
            && host.flowIpcWindowContext("main") === main
            && (sibling == nil || host.flowIpcWindowContext("churn-sibling") === sibling
                || (retiringSibling && host.flowIpcWindowContext("churn-sibling") == nil))
    }
    func start() {
        guard pid > 0, timeout > 0, timeout <= 600 else { fail("configuration"); return }
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { self.fail("deadline") }
        tasks = [
            { next in self.waitState(self.main, { self.ready($0) }) { s in
                self.mainDocument = s["document"] as? String ?? ""; next()
            } },
            { next in self.click(self.main, "window-churn-mutate", next) },
            { next in self.waitState(self.main, { $0["state"] as? String == "change=1 client=true" }) { _ in next() } },
            { next in self.click(self.main, "window-churn-open-churn-sibling", next) },
            { next in self.waitOwner("churn-sibling") { context in
                self.sibling = context
                self.waitState(context, { self.ready($0) }) { s in
                    self.siblingDocument = s["document"] as? String ?? ""; next()
                }
            } },
            { next in self.click(self.sibling!, "window-churn-mutate", next) },
            { next in self.waitState(self.sibling!, { $0["state"] as? String == "change=1 client=true" }) { _ in next() } }
        ]
        advance()
    }
    func advance() {
        guard valid() else { fail("owner-changed"); return }
        if !tasks.isEmpty { let task = tasks.removeFirst(); task { self.advance() }; return }
        openCycle()
    }
    func openCycle() {
        click(main, "window-churn-open-churn-secondary") {
            self.waitOwner("churn-secondary") { context in
                guard !self.owners.contains(where: { $0 === context || $0.window === context.window || $0.webView === context.webView }),
                      !self.owners.contains(where: { $0.scope == context.scope }) else {
                    self.fail("owner-reused"); return
                }
                self.owners.append(context)
                self.waitState(context, { self.ready($0) }) { s in
                    guard let doc = s["document"] as? String, UUID(uuidString: doc) != nil,
                          doc != self.mainDocument, doc != self.siblingDocument,
                          self.documents.insert(doc).inserted else { self.fail("document-reused"); return }
                    self.click(context, "window-churn-mutate") {
                        self.waitState(context, { $0["state"] as? String == "change=1 client=true" }) { _ in
                            self.click(context, "window-churn-probe") {
                                let expected = "cycle=\(self.cycle) before=\(self.cycle) after=\(self.cycle) fresh=true background=true client=true"
                                self.waitState(context, { $0["probe"] as? String == expected }) { _ in
                                    self.observeCycle(context)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    func observeCycle(_ context: FlowIpcWindowContext) {
        guard let sibling else { fail("missing-sibling"); return }
        let observed = [main, sibling, context]
        func observe(_ index: Int) {
            if index == observed.count {
                self.click(self.main, "window-churn-close-churn-secondary") {
                    self.waitClosed("churn-secondary") {
                        if self.cycle < 10 { self.cycle += 1; self.openCycle() }
                        else { self.closeSiblings() }
                    }
                }
                return
            }
            let owner = observed[index]
            self.waitState(owner, { s in
                s["state"] as? String == "change=1 client=true"
            }) { s in
                let expectedDocument = index == 0 ? self.mainDocument : index == 1 ? self.siblingDocument : s["document"] as? String
                guard s["document"] as? String == expectedDocument,
                      index == 2 || s["probe"] as? String == "pending" else { self.fail("sibling-state-changed"); return }
                var payload = owner.scope.applying(to: s)
                payload["runId"] = self.runId; payload["cycle"] = self.cycle
                payload["processIdentifier"] = self.pid; payload["inputMode"] = "browser-programmatic"
                payload["windowNumber"] = owner.window.windowNumber
                trace("recv", "windowChurn.stageObserved", payload)
                observe(index + 1)
            }
        }
        observe(0)
    }
    func closeSiblings() {
        retiringSibling = true
        click(main, "window-churn-close-churn-sibling") {
            // The retained sibling is now intentionally retired.
            self.sibling = nil
            self.waitClosed("churn-sibling") {
                self.terminal = true
                trace("recv", "windowChurn.browserRunCompleted", self.main.scope.applying(to: [
                    "runId": self.runId, "cycles": 10, "incarnations": self.owners.count,
                    "processIdentifier": self.pid, "independentGatePassed": false,
                    "inputMode": "browser-programmatic"]))
                self.main.window.close()
            }
        }
    }
    func ready(_ s: [String: Any]) -> Bool {
        s["state"] as? String == "change=0" && s["probe"] as? String == "pending"
            && UUID(uuidString: s["document"] as? String ?? "") != nil
    }
    func later(_ action: @escaping () -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            guard self.valid() else { self.fail("owner-changed"); return }; action()
        }
    }
    func waitOwner(_ id: String, _ done: @escaping (FlowIpcWindowContext) -> Void) {
        guard valid() else { fail("owner-changed"); return }
        if let owner = host.flowIpcWindowContext(id), owner.navigationFinished { done(owner) }
        else { later { self.waitOwner(id, done) } }
    }
    func waitClosed(_ id: String, _ done: @escaping () -> Void) {
        guard valid() else { fail("owner-changed"); return }
        if host.flowIpcWindowContext(id) == nil { done() }
        else { later { self.waitClosed(id, done) } }
    }
    func click(_ owner: FlowIpcWindowContext, _ id: String, _ done: @escaping () -> Void) {
        guard valid(), host.flowIpcWindowContext(owner.scope.windowId) === owner else { fail("click-owner"); return }
        trace("send", "windowChurn.action", owner.scope.applying(to: [
            "runId": runId, "cycle": cycle, "element": id, "processIdentifier": pid,
            "inputMode": "browser-programmatic"]))
        owner.webView.evaluateJavaScript("(() => { const b=document.getElementById('\(id)'); if(!b) return false; b.click(); return true; })()") { value, error in
            guard self.valid(), error == nil, value as? Bool == true else { self.fail("click-dispatch"); return }
            done()
        }
    }
    func waitState(_ owner: FlowIpcWindowContext, _ predicate: @escaping ([String: Any]) -> Bool,
                   _ done: @escaping ([String: Any]) -> Void) {
        guard valid(), host.flowIpcWindowContext(owner.scope.windowId) === owner else { fail("inspection-owner"); return }
        owner.webView.evaluateJavaScript(FlowIpcRecoveryPushSettlement.source + Self.snapshot) { value, error in
            guard self.valid(), self.host.flowIpcWindowContext(owner.scope.windowId) === owner,
                  error == nil, let text = value as? String, let data = text.data(using: .utf8),
                  let state = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                self.fail("inspection"); return
            }
            guard (state["error"] as? String ?? "").isEmpty else { self.fail("browser-error"); return }
            if state["settled"] as? Bool == true && predicate(state) { done(state) }
            else { self.later { self.waitState(owner, predicate, done) } }
        }
    }
    func fail(_ reason: String) {
        guard !terminal else { return }; terminal = true
        trace("send", "error", main.scope.applying(to: ["code": "windowChurn-" + reason, "cycle": cycle, "runId": runId]))
        host.flowIpcWindowContext("churn-secondary")?.window.close()
        host.flowIpcWindowContext("churn-sibling")?.window.close()
        main.window.close()
    }
    static let snapshot = """
    (() => {
      const d=window.VaadinDesktopIpcDiagnostics||{};
      const text=id=>document.getElementById(id)?.textContent||'';
      const clients=Object.values(window.Vaadin?.Flow?.clients||{}).filter(c=>typeof c.isActive==='function');
      const probe=text('window-churn-probe-result');
      const probeConsumed=window.VatoriRecoveryPushSettlement?.consumed()===true;
      return JSON.stringify({document:text('window-churn-document'),state:text('window-churn-state'),
        probe,probeConsumed,operation:text('window-churn-operation'),
        requestCount:Number(d.standardBundleOpaqueRequestCount||0),responseCount:Number(d.standardBundleOpaqueResponseCount||0),
        pushMessageCount:Number(d.standardBundlePushMessageCount||0),
        settled:clients.length>0&&clients.every(c=>!c.isActive())&&d.standardBundlePushConnected===true
          &&(!probe.startsWith('cycle=')||probeConsumed)
          &&Number(d.standardBundleOpaqueRequestCount||0)===Number(d.standardBundleOpaqueResponseCount||0),
        error:d.standardBundleBrowserError||d.standardBundleOpaqueRequestError||d.standardBundleOpaqueResponseError||d.standardBundlePushError||''});
    })()
    """
}
