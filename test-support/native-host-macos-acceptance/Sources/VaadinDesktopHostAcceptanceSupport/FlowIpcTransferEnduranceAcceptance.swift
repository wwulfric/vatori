import AppKit
import Foundation

private var transferEnduranceStarted = false
extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleTransferEndurance(host: WebViewHost, context: FlowIpcWindowContext) {
        guard context.scope == .main, !transferEnduranceStarted else { return }
        transferEnduranceStarted = true
        FlowIpcTransferEnduranceAcceptance(host, context).start()
    }
}

private final class FlowIpcTransferEnduranceAcceptance {
    let host: WebViewHost
    let main: FlowIpcWindowContext
    let pid: Int32
    let runId = UUID().uuidString
    var secondary: FlowIpcWindowContext?
    var started: Set<String> = []
    var emitted: [String: Int] = [:]
    var finished: Set<String> = []
    var terminal = false
    var openRequested = false
    var draining = false
    var confirmed = false
    init(_ host: WebViewHost, _ main: FlowIpcWindowContext) {
        self.host=host; self.main=main; pid=host.sidecarProcessIdentifier ?? 0
    }
    func start() {
        DispatchQueue.main.asyncAfter(deadline: .now()+2100) { self.fail("deadline") }
        poll()
    }
    func valid() -> Bool {
        !terminal && pid>0 && host.sidecarProcessIdentifier==pid && main.recoveryGeneration==0
            && host.flowIpcWindowContext("main") === main
            && (secondary == nil || host.flowIpcWindowContext("transfer-secondary") === secondary)
    }
    func poll() {
        guard valid() else { fail("owner-changed"); return }
        if secondary == nil {
            if let context=host.flowIpcWindowContext("transfer-secondary"), context.navigationFinished {
                secondary=context
            } else if !openRequested {
                main.webView.evaluateJavaScript("(() => {const b=document.getElementById('transfer-open');if(!b)return false;b.click();return true;})()") { value,error in
                    if error != nil { self.fail("open"); return }
                    self.openRequested=value as? Bool == true
                    self.later()
                }
                return
            } else { later(); return }
        }
        guard let secondary else { fail("secondary"); return }
        inspect([main,secondary], index:0) { self.later() }
    }
    func inspect(_ owners: [FlowIpcWindowContext], index: Int, done: @escaping () -> Void) {
        guard valid() else { fail("owner-changed"); return }
        if index == owners.count {
            if finished.count==2 {
                if !draining {
                    draining=true
                    for owner in owners { owner.webView.evaluateJavaScript("document.getElementById('transfer-confirm').click()",completionHandler:nil) }
                } else if confirmed { finish() }
            }
            done(); return
        }
        let owner=owners[index], key=owner.scope.windowId
        let script=started.contains(key) ? Self.snapshot : FlowIpcTransferEnduranceScript.source
        owner.webView.evaluateJavaScript(script) { value,error in
            guard self.valid(), self.host.flowIpcWindowContext(key) === owner, error == nil else { self.fail("inspection"); return }
            if !self.started.contains(key) {
                if value as? Bool == true { self.started.insert(key) }
            } else {
                guard let text=value as? String, let data=text.data(using:.utf8),
                      let state=try? JSONSerialization.jsonObject(with:data) as? [String:Any] else { self.fail("snapshot"); return }
                if let error=state["error"] as? String, !error.isEmpty { self.fail(error); return }
                let receipts=state["receipts"] as? [[String:Any]] ?? []
                let previous=self.emitted[key] ?? 0
                guard receipts.count>=previous,receipts.count<=2000 else { self.fail("receipt-count"); return }
                for receipt in receipts.dropFirst(previous) {
                    var payload=owner.scope.applying(to:receipt)
                    payload["runId"]=self.runId; payload["processIdentifier"]=self.pid
                    trace("recv","transferEndurance.batch",payload)
                }
                self.emitted[key]=receipts.count
                if state["done"] as? Bool == true && !self.finished.contains(key) {
                    self.finished.insert(key)
                    trace("recv","transferEndurance.ownerCompleted",owner.scope.applying(to:[
                        "runId":self.runId,"processIdentifier":self.pid,"batches":receipts.count,
                        "elapsed":state["elapsed"] ?? 0,"inputMode":"browser-programmatic"]))
                }
                if self.draining {
                    guard let status=state["status"] as? String,
                          status.contains(";tempEmpty=true;"),status.contains(";active=0;"),
                          state["settled"] as? Bool == true else { self.later(); return }
                    if index==1 { self.confirmed=true }
                    trace("recv","transferEndurance.drained",owner.scope.applying(to:["runId":self.runId,"status":status]))
                }
            }
            self.inspect(owners,index:index+1,done:done)
        }
    }
    func later() {
        guard !terminal else { return }
        DispatchQueue.main.asyncAfter(deadline:.now()+1) { self.poll() }
    }
    func finish() {
        guard !terminal else { return };terminal=true
        trace("recv","transferEndurance.completed",["runId":runId,"windows":2,"independentGatePassed":false])
        secondary?.window.close();main.window.close()
    }
    func fail(_ reason:String) {
        guard !terminal else { return };terminal=true
        trace("send","error",["code":"transferEndurance","reason":reason,"runId":runId])
        host.flowIpcWindowContext("transfer-secondary")?.window.close();main.window.close()
    }
    static let snapshot = #"""
    (()=>{
      const s=window.VatoriTransferEndurance||{},d=window.VaadinDesktopIpcDiagnostics||{};
      const clients=Object.values(window.Vaadin?.Flow?.clients||{}).filter(c=>typeof c.isActive==='function');
      return JSON.stringify({...s,status:document.getElementById('transfer-status')?.textContent||'',
        settled:clients.length>0&&clients.every(c=>!c.isActive()),
        error:s.error||d.standardBundleBrowserError||d.standardBundleOpaqueRequestError||d.standardBundleOpaqueResponseError||''});
    })()
    """#
}
