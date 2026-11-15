import AppKit
import Foundation

private var lazyDataBaselineStarted = false

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLazyDataBaseline(host: WebViewHost, context: FlowIpcWindowContext, timeout: Double) {
        guard context.scope == .main, !lazyDataBaselineStarted else { return }
        lazyDataBaselineStarted = true
        FlowIpcLazyDataBaselineAcceptance(host: host, context: context, timeout: timeout).start()
    }
}

// Browser-programmatic coverage only. Completion is not an independent trace-gate PASS.
private final class FlowIpcLazyDataBaselineAcceptance {
    let host: WebViewHost
    let context: FlowIpcWindowContext
    let timeout: Double
    let pid: Int32
    var terminal = false
    var emitted = 0
    init(host: WebViewHost, context: FlowIpcWindowContext, timeout: Double) {
        self.host = host; self.context = context; self.timeout = timeout
        pid = host.sidecarProcessIdentifier ?? 0
    }
    func start() {
        guard timeout > 0, timeout <= 600, pid > 0 else { finish("invalid-configuration"); return }
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { self.finish("deadline") }
        poll()
    }
    func poll() {
        guard !terminal else { return }
        guard host.flowIpcWindowContext("main") === context,
              context.recoveryGeneration == 0, host.sidecarProcessIdentifier == pid else {
            finish("owner-changed"); return
        }
        context.webView.evaluateJavaScript(Self.script) { value, error in
            guard !self.terminal else { return }
            guard self.host.flowIpcWindowContext("main") === self.context,
                  self.context.recoveryGeneration == 0,
                  self.host.sidecarProcessIdentifier == self.pid else {
                self.finish("callback-owner-changed"); return
            }
            guard error == nil, let text = value as? String, let data = text.data(using: .utf8),
                  let state = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                self.finish("inspection-failed"); return
            }
            if let failure = state["error"] as? String, !failure.isEmpty { self.finish(failure); return }
            if let receipts = state["receipts"] as? [[String: Any]] {
                guard receipts.count >= self.emitted, receipts.count <= 50 else { self.finish("receipt-count"); return }
                for receipt in receipts.dropFirst(self.emitted) {
                    var payload = self.context.scope.applying(to: receipt)
                    payload["processIdentifier"] = self.pid
                    payload["inputMode"] = "browser-programmatic"
                    trace("recv", "lazyDataBaseline.stageObserved", payload)
                }
                self.emitted = receipts.count
            }
            if state["done"] as? Bool == true {
                guard self.emitted == 50 else { self.finish("incomplete-receipts"); return }
                self.terminal = true
                trace("recv", "lazyDataBaseline.browserRunCompleted", self.context.scope.applying(to: [
                    "processIdentifier": self.pid, "receiptCount": self.emitted,
                    "inputMode": "browser-programmatic", "independentGatePassed": false]))
                self.context.window.close()
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { self.poll() }
        }
    }
    func finish(_ reason: String) {
        guard !terminal else { return }; terminal = true
        trace("send", "error", context.scope.applying(to: ["code": "lazyDataBaseline-" + reason, "receiptCount": emitted]))
        context.window.close()
    }

    static let script = """
    (() => {
      if (!document.getElementById('lazy-data-baseline')) {
        const open=document.getElementById('lazy-data-baseline-open');
        if (open && !window.VatoriLazyBaselineNavigating) {
          window.VatoriLazyBaselineNavigating=true; open.click();
        }
        return JSON.stringify({receipts:[],done:false,error:''});
      }
      if (!window.VatoriLazyBaselineRun) {
        const run=window.VatoriLazyBaselineRun={receipts:[],done:false,error:''};
        const sleep=()=>new Promise(resolve=>setTimeout(resolve,100));
        const parse=text=>Object.fromEntries(String(text).split(';').map(part=>{
          const at=part.indexOf('='); return [part.slice(0,at),part.slice(at+1)];
        }));
        const number=(o,k)=>Number(o[k]);
        const expected=(cycle,removed=0)=>Array.from({length:1000},(_,i)=>i+1)
          .filter(id=>id!==removed && (cycle===0 || id%11!==cycle))
          .sort((a,b)=>cycle%2?b-a:a-b);
        async function sample(mode) {
          const p='lazy-baseline-'+mode+'-', grid=document.getElementById(p+'grid');
          const d=window.VaadinDesktopIpcDiagnostics||{};
          if (d.standardBundleBrowserError||d.standardBundleOpaqueRequestError||d.standardBundleOpaqueResponseError)
            throw Error('transport-or-browser');
          const status=parse(document.getElementById(p+'status').textContent);
          const query=parse(document.getElementById(p+'query').textContent);
          const bounds=grid.getBoundingClientRect();
          const cells=Array.from(grid.querySelectorAll('vaadin-grid-cell-content')).filter(cell=>{
            const id=Number(cell.textContent.trim()), r=cell.getBoundingClientRect();
            return Number.isInteger(id)&&id>0&&id<=1000&&r.width>0&&r.height>0
              &&r.bottom>bounds.top&&r.top<bounds.bottom&&r.bottom>0&&r.top<innerHeight;
          }).sort((a,b)=>a.getBoundingClientRect().top-b.getBoundingClientRect().top);
          return {mode,status,query,size:Number(grid.size),visible:cells.map(cell=>Number(cell.textContent.trim())),
            requestCount:Number(d.standardBundleOpaqueRequestCount||0),responseCount:Number(d.standardBundleOpaqueResponseCount||0),
            selectionClient:document.getElementById(p+'status').getAttribute('data-selection-client'),
            removedId:Number(document.getElementById(p+'status').getAttribute('data-removed-id')||0)};
        }
        async function wait(mode, predicate, action=()=>{}) {
          for(let attempt=0;attempt<200;attempt++) {
            action(); const s=await sample(mode);
            if(s.requestCount===s.responseCount && predicate(s)) return s;
            await sleep();
          }
          throw Error('state-timeout-'+mode+'-'+run.receipts.length);
        }
        function receipt(stage,s) {
          run.receipts.push(JSON.parse(JSON.stringify({index:run.receipts.length,stage,...s})));
        }
        (async()=>{
          for(const mode of ['exact','unknown']) {
            const p='lazy-baseline-'+mode+'-',grid=document.getElementById(p+'grid');
            grid.scrollIntoView({block:'center'});
            for(let cycle=0;cycle<=10;cycle++) {
              const before=await sample(mode);
              if(cycle>0) document.getElementById(p+'advance').click();
              const ids=expected(cycle);
              const settled=s=>number(s.status,'cycle')===cycle && number(s.status,'total')===1000
                &&number(s.status,'matches')===ids.length && number(s.status,'fetches')>number(s.status,'fetchBaseline')
                &&(mode==='unknown'?number(s.status,'counts')===0:number(s.status,'counts')>number(s.status,'countBaseline'))
                &&(cycle===0 || (s.status.client==='true'&&s.responseCount>before.responseCount));
              const top=await wait(mode,s=>settled(s)&&s.visible.includes(ids[0]),()=>grid.scrollToIndex(0));
              receipt('top',top);
              const tail=await wait(mode,s=>settled(s)&&s.size===ids.length&&s.visible.includes(ids.at(-1)),
                ()=>grid.scrollToIndex(Math.min(ids.length-1,Math.max(0,grid.size-1))));
              receipt('tail',tail);
            }
            const ids=expected(10);
            await wait(mode,s=>s.visible.includes(ids[0]),()=>grid.scrollToIndex(0));
            const clickId=id=>{
              const cell=Array.from(grid.querySelectorAll('vaadin-grid-cell-content'))
                .find(cell=>cell.textContent.trim()===String(id)&&cell.getBoundingClientRect().height>0);
              if(!cell)throw Error('missing-cell'); cell.click();
            };
            const beforeSelection=await sample(mode); clickId(ids[0]);
            const selected=await wait(mode,s=>s.status.selected==='['+ids[0]+']'&&s.selectionClient==='true'
              &&s.responseCount>beforeSelection.responseCount);
            receipt('selected',selected);
            document.getElementById(p+'remove').click();
            const survivors=expected(10,ids[0]);
            const removed=await wait(mode,s=>s.status.selected==='[]'&&number(s.status,'total')===999
              &&number(s.status,'removals')===1&&s.removedId===ids[0]&&s.status.client==='true'
              &&s.responseCount>selected.responseCount&&!s.visible.includes(ids[0])&&s.visible.includes(survivors[0]));
            receipt('removed',removed);
            clickId(survivors[0]);
            const survivor=await wait(mode,s=>s.status.selected==='['+survivors[0]+']'&&s.selectionClient==='true'
              &&s.responseCount>removed.responseCount);
            receipt('survivor',survivor);
          }
          run.done=true;
        })().catch(error=>{run.error=String(error.message||error)});
      }
      return JSON.stringify(window.VatoriLazyBaselineRun);
    })()
    """
}
