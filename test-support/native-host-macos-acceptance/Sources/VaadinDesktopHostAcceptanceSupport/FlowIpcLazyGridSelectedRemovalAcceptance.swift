import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLazyGridSelectedRemovalAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        guard context.scope == .main else { return }
        FlowIpcLazyGridSelectedRemovalAcceptance(host: host, context: context, timeout: timeoutSeconds).start()
    }
}

// This application explicitly deselects before deleting. It does not claim
// that a lazy provider automatically prunes selection under PRESERVE_ALL.
private final class FlowIpcLazyGridSelectedRemovalAcceptance {
    private enum Phase { case root, initial, target, selected, offViewport, removed, removalRevisited, survivorSelected, survivorOffViewport, survivorRevisited, drain }
    private static let prefix = "framework-flow-lazy-grid-selection-"
    private static let stages = ["initial", "selected", "off-viewport", "removed", "removal-revisited", "survivor-selected", "survivor-off-viewport", "survivor-revisited"]
    private static let pointerStages = ["select-L-060", "remove-selected-L-060", "select-L-061"]
    private static let pointerLabels = ["L-060", "delete-selected-L-060", "L-061"]
    private static let browserStages = ["navigate", "scroll-select-target", "position-select-L-060", "scroll-selected-off-viewport", "position-remove-selected-L-060", "scroll-removed-neighborhood", "position-select-L-061", "scroll-survivor-off-viewport", "scroll-survivor-revisit"]
    private static let selection = [
        "Lazy grid selection: none; change=0, added=none, removed=none, client=false",
        "Lazy grid selection: L-060; change=1, added=L-060, removed=none, client=true",
        "Lazy grid selection: none; change=2, added=none, removed=L-060, client=false",
        "Lazy grid selection: L-061; change=3, added=L-061, removed=none, client=true"]
    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let windowIdentity: ObjectIdentifier
    private let webViewIdentity: ObjectIdentifier
    private let timeout: Double
    private let deadline: Date
    private var pid: Int32 = 0
    private var phase = Phase.root
    private var finished = false
    private var observed: [String] = []
    private var dispatched: [String] = []
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var quietSignature = ""
    private var quietCount = 0
    private var diagnosticSignature = ""
    private var latest: Snapshot?

    init(host: WebViewHost, context: FlowIpcWindowContext, timeout: Double) {
        self.host = host; self.context = context; self.timeout = timeout
        deadline = Date().addingTimeInterval(timeout)
        windowIdentity = ObjectIdentifier(context.window); webViewIdentity = ObjectIdentifier(context.webView)
    }
    func start() {
        guard timeout.isFinite, timeout > 0, timeout <= 300 else { fail("invalid-timeout"); return }
        // An independent main-queue deadline still fires if WebKit never calls back.
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) {
            guard !self.finished else { return }; self.fail("deadline")
        }
        poll()
    }
    private var retained: Bool {
        host.flowIpcWindowContext("main") === context && context.scope == .main
            && ObjectIdentifier(context.window) == windowIdentity && ObjectIdentifier(context.webView) == webViewIdentity
            && context.recoveryGeneration == 0 && host.sidecarProcessIdentifier == pid
    }
    private var frame: [String: Any] {
        context.scope.applying(to: ["hostProcessIdentifier": getpid(), "processIdentifier": pid,
            "recoveryGeneration": context.recoveryGeneration, "windowNumber": context.window.windowNumber,
            "contextRetained": host.flowIpcWindowContext("main") === context,
            "windowRetained": ObjectIdentifier(context.window) == windowIdentity,
            "webViewRetained": ObjectIdentifier(context.webView) == webViewIdentity])
    }
    private func poll() {
        guard !finished else { return }
        guard Date() < deadline else { fail("deadline"); return }
        guard pid == 0 || retained else { fail("owner-identity"); return }
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnose("snapshot-unavailable"); self.schedule(); return
            }
            self.latest = state
            guard state.errorFree, state.browserWindowId == self.context.scope.windowId,
                  state.browserSessionId == self.context.scope.sessionId, state.browserUiId == self.context.scope.uiId else {
                self.fail("snapshot-scope-or-error", details: ["rejectedSnapshot": state.payload]); return
            }
            self.advance(state)
        }
    }
    private func advance(_ state: Snapshot) {
        if phase == .root {
            guard context.navigationFinished, state.rootRouteAvailable, state.firstMeaningfulPaint,
                  state.flowClientCount == 1, let actualPid = host.sidecarProcessIdentifier, actualPid > 0 else {
                diagnose("root-ready"); schedule(); return
            }
            pid = actualPid
            evaluate("""
            (()=>{ const button=document.getElementById('\(Self.prefix)open-route'); if(!button)return false;
              window.VaadinDesktopLazyGridSelectedRemoval={browserProgrammaticStages:['navigate']}; button.click(); return true; })()
            """) { self.phase = .initial; self.schedule() }
            return
        }
        let changes: Int
        switch phase {
        case .initial, .target: changes = 0
        case .selected, .offViewport: changes = 1
        case .removed, .removalRevisited: changes = 2
        default: changes = 3
        }
        guard validState(state, changes: changes), state.balanced else {
            resetQuiet(); diagnose("state-or-drain"); schedule(); return
        }
        switch phase {
        case .initial:
            guard state.visibleLabels.contains("L-001"), quiet(state, polls: 2) else { schedule(); return }
            requestBaseline = state.requestCount; responseBaseline = state.responseCount
            record("initial", state, roundTrip: false)
            scroll(index: 58, stage: "scroll-select-target", next: .target)
        case .target:
            guard state.visibleLabels.contains("L-060") else { diagnose("selection-target"); schedule(); return }
            click(index: 0, next: .selected)
        case .selected:
            guard selectedDOM(state, label: "L-060"), pointerRoundTrip(state), quiet(state, polls: 2) else {
                diagnose("selected-round-trip"); schedule(); return
            }
            record("selected", state, roundTrip: true)
            scroll(index: 0, stage: "scroll-selected-off-viewport", next: .offViewport)
        case .offViewport:
            guard state.visibleLabels.contains("L-001"), !state.renderedLabels.contains("L-060"), emptyRenderedSelection(state),
                  quiet(state, polls: 2) else { diagnose("selected-off-viewport"); schedule(); return }
            record("off-viewport", state, roundTrip: false)
            click(index: 1, next: .removed)
        case .removed:
            guard emptyRenderedSelection(state), !state.renderedLabels.contains("L-060"), pointerRoundTrip(state),
                  state.queryCallCount > state.refreshFetchBaseline, state.countCallCount > state.refreshCountBaseline,
                  state.queryStatus.hasSuffix("matches=79 selected=none"), state.countStatus.hasSuffix("matches=79 selected=none"),
                  quiet(state, polls: 2) else { diagnose("removal-refresh-round-trip"); schedule(); return }
            record("removed", state, roundTrip: true)
            scroll(index: 58, stage: "scroll-removed-neighborhood", next: .removalRevisited)
        case .removalRevisited:
            guard neighborhood(state), emptyRenderedSelection(state), quiet(state, polls: 2) else {
                diagnose("removed-neighborhood"); schedule(); return
            }
            record("removal-revisited", state, roundTrip: false)
            click(index: 2, next: .survivorSelected)
        case .survivorSelected:
            guard neighborhood(state), selectedDOM(state, label: "L-061"), pointerRoundTrip(state), quiet(state, polls: 2) else {
                diagnose("survivor-round-trip"); schedule(); return
            }
            record("survivor-selected", state, roundTrip: true)
            scroll(index: 0, stage: "scroll-survivor-off-viewport", next: .survivorOffViewport)
        case .survivorOffViewport:
            guard state.visibleLabels.contains("L-001"), !state.renderedLabels.contains("L-061"), !state.renderedLabels.contains("L-060"),
                  emptyRenderedSelection(state), quiet(state, polls: 2) else { diagnose("survivor-off-viewport"); schedule(); return }
            record("survivor-off-viewport", state, roundTrip: false)
            scroll(index: 58, stage: "scroll-survivor-revisit", next: .survivorRevisited)
        case .survivorRevisited:
            guard neighborhood(state), selectedDOM(state, label: "L-061"), quiet(state, polls: 2) else {
                diagnose("survivor-revisit"); schedule(); return
            }
            record("survivor-revisited", state, roundTrip: false)
            resetQuiet(); phase = .drain; schedule()
        case .drain:
            guard neighborhood(state), selectedDOM(state, label: "L-061"), quiet(state, polls: 3) else { schedule(); return }
            finish(state)
        default: fail("phase")
        }
    }
    private func validState(_ state: Snapshot, changes: Int) -> Bool {
        let expectedHistory = "Lazy grid selection history: " + (changes == 0 ? "pending" : Self.selection[1...changes].joined(separator: " | "))
        let expectedPointers = Array(Self.pointerStages.prefix(changes)), expectedLabels = Array(Self.pointerLabels.prefix(changes))
        let old = changes < 2
        // Flow connector.clear drops selected objects for evicted cache pages
        // without a server deselect event. Java history remains authoritative;
        // the later visible row/checkbox/ARIA receipt proves rehydration.
        let selectionEvicted = phase == .offViewport || phase == .survivorOffViewport
        let expectedClientSelectionCount = (changes == 1 || changes == 3) && !selectionEvicted ? 1 : 0
        let removal = "Lazy grid selected removal: operation=deselect>remove>refreshAll; policy=PRESERVE_ALL, removed=L-060, removedSelected=true, beforeTotal=80, afterTotal=79, beforeSelected=L-060, afterDeselect=none, afterRefresh=none, selectionChangesBefore=1, selectionChangesAfterDeselect=2, selectionChangesAfterRefresh=2, removeSucceeded=true, removalChanges=1, dataChangeEvents=1, dataRefreshEvents=0, fetchBaseline=\(state.refreshFetchBaseline), countBaseline=\(state.refreshCountBaseline), client=true"
        return retained && state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1
            && state.filterValue.isEmpty && state.selectionColumnObserved && state.ariaMultiSelectableObserved
            && state.gridSize == (old ? 80 : 79) && state.selectedItemsCount == expectedClientSelectionCount
            && state.selectionStatus == Self.selection[changes] && state.selectionHistory == expectedHistory
            && state.refreshStatus == "Lazy grid refresh: pending" && state.refreshAllStatus == "Lazy grid refresh all: pending"
            && state.removalStatus == (old ? "Lazy grid selected removal: pending" : removal)
            && (old || (state.removeButtonDisabled && state.refreshFetchBaseline > 0 && state.refreshCountBaseline > 0))
            && state.nativePointerEventCount == changes && state.nativePointerTrustedEventCount == changes
            && state.pointerStages == expectedPointers && state.pointerLabels == expectedLabels
            && state.domEventOrder == expectedPointers.map { "pointer:" + $0 }
            && state.queryCallCount > 0 && state.countCallCount > 0
    }
    private func emptyRenderedSelection(_ state: Snapshot) -> Bool {
        state.selectedRenderedRowCount == 0 && state.checkedRenderedRowCount == 0 && state.ariaSelectedRenderedRowCount == 0
            && state.selectedRowLabels.isEmpty && state.checkedRowLabels.isEmpty && state.ariaSelectedRowLabels.isEmpty
    }
    private func selectedDOM(_ state: Snapshot, label: String) -> Bool {
        state.visibleLabels.contains(label) && state.selectedRenderedRowCount == 1 && state.checkedRenderedRowCount == 1
            && state.ariaSelectedRenderedRowCount == 1 && state.selectedRowLabels == [label]
            && state.checkedRowLabels == [label] && state.ariaSelectedRowLabels == [label]
    }
    private func neighborhood(_ state: Snapshot) -> Bool {
        guard state.visibleLabels.contains("L-059"), state.visibleLabels.contains("L-061"), !state.renderedLabels.contains("L-060"),
              let before = state.renderedLabels.firstIndex(of: "L-059"), let after = state.renderedLabels.firstIndex(of: "L-061") else { return false }
        return after == before + 1
    }
    private func pointerRoundTrip(_ state: Snapshot) -> Bool { state.requestCount > requestBaseline && state.responseCount > responseBaseline }
    private func quiet(_ state: Snapshot, polls: Int) -> Bool {
        let signature = "\(phase):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount):\(state.queryCallCount):\(state.countCallCount)"
        quietCount = signature == quietSignature ? quietCount + 1 : 1; quietSignature = signature
        return quietCount >= polls
    }
    private func resetQuiet() { quietSignature = ""; quietCount = 0 }
    private func receipt(_ state: Snapshot, stage: String, roundTrip: Bool) -> [String: Any] {
        var payload = frame; payload.merge(state.payload) { _, replacement in replacement }
        payload["stage"] = stage; payload["requestBaseline"] = requestBaseline; payload["responseBaseline"] = responseBaseline
        payload["actionUidlRoundTripObserved"] = roundTrip; payload["balancedDrainObserved"] = state.balanced; payload["quietPollCount"] = quietCount
        return payload
    }
    private func record(_ stage: String, _ state: Snapshot, roundTrip: Bool) {
        observed.append(stage)
        trace("recv", "lazyGridSelectedRemoval.stageObserved", receipt(state, stage: stage, roundTrip: roundTrip))
    }
    private func finish(_ state: Snapshot) {
        guard observed == Self.stages, dispatched == Self.pointerStages, state.browserProgrammaticStages == Self.browserStages else { fail("final-contract"); return }
        var payload = receipt(state, stage: "survivor-revisited", roundTrip: false)
        payload.merge(["complete": true, "errorFree": true, "standardBundleMode": true, "rootRendered": true, "flowClientOwned": true,
            "opaqueRequestBridgeObserved": true, "opaqueResponseBridgeObserved": true, "generatedAppKitInput": true,
            "explicitApplicationDeselectionObserved": true, "removedItemAbsentObserved": true, "survivorSelectionObserved": true,
            "recycledSurvivorSelectionObserved": true, "pushObserved": false, "physicalInputClaimed": false,
            "assistiveTechnologyClaimed": false, "observedStages": observed, "drainQuietPollCount": quietCount]) { _, replacement in replacement }
        finished = true; trace("recv", "lazyGridSelectedRemoval.frameworkAcceptance", payload); context.window.close()
    }
    private func scroll(index: Int, stage: String, next: Phase) {
        resetQuiet()
        evaluate("""
        (()=>{const grid=document.getElementById('\(Self.prefix)grid');if(!grid || typeof grid.scrollToIndex!=='function')return false;
          window.VaadinDesktopLazyGridSelectedRemoval.browserProgrammaticStages.push('\(stage)');
          grid.scrollIntoView({block:'center',inline:'nearest'});grid.scrollToIndex(\(index));return true;})()
        """) { self.phase = next; self.schedule(after: 0.35) }
    }
    private func click(index: Int, next: Phase) {
        let stage = Self.pointerStages[index], label = Self.pointerLabels[index]
        context.webView.evaluateJavaScript("""
        (()=>{
          \(Self.domHelpers)
          const marker=window.VaadinDesktopLazyGridSelectedRemoval;
          const target=\(index == 1 ? "document.getElementById('framework-flow-lazy-grid-selection-remove-selected')" : "checkboxForLabel('\(label)')");
          if(!target || target.disabled===true)return '';
          target.scrollIntoView({block:'nearest',inline:'nearest'});
          const r=target.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2;
          let hit=document.elementFromPoint(x,y);while(hit?.shadowRoot?.elementFromPoint){const inner=hit.shadowRoot.elementFromPoint(x,y);if(!inner || inner===hit)break;hit=inner;}
          let node=hit,owned=false;const visited=new Set();while(node && !visited.has(node)){if(node===target){owned=true;break;}visited.add(node);node=node.assignedSlot || node.parentNode || node.getRootNode?.().host || null;}
          if(!owned || r.width<=0 || r.height<=0 || x<0 || y<0 || x>=innerWidth || y>=innerHeight)return '';
          for(const name of ['pointerStages','pointerLabels','domEventOrder'])marker[name] ||= [];
          if(marker.pendingPointerHandler)document.removeEventListener('click',marker.pendingPointerHandler,true);
          const handler=event=>{if(event.button!==0 || !event.composedPath().includes(target))return;
            document.removeEventListener('click',handler,true);marker.pendingPointerHandler=null;
            marker.pointerStages.push('\(stage)');marker.pointerLabels.push('\(label)');marker.domEventOrder.push('pointer:\(stage)');
            marker.nativePointerTrustedEventCount=Number(marker.nativePointerTrustedEventCount || 0)+(event.isTrusted===true?1:0);};
          marker.pendingPointerHandler=handler;document.addEventListener('click',handler,true);
          marker.browserProgrammaticStages.push('position-\(stage)');
          const d=window.VaadinDesktopIpcDiagnostics || {};
          return JSON.stringify({x,y,requestCount:Number(d.standardBundleOpaqueRequestCount || 0),responseCount:Number(d.standardBundleOpaqueResponseCount || 0)});
        })()
        """) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(Geometry.self, from: data) else { self.fail("pointer-geometry"); return }
            self.requestBaseline = point.requestCount; self.responseBaseline = point.responseCount
            FlowIpcNativeInputActivation.prepare(context: self.context, deadline: self.deadline,
                onFailure: { self.fail("activation", details: $0) }) {
                guard !self.finished, self.retained else { return }
                let p = NSPoint(x: point.x, y: self.context.webView.isFlipped ? point.y : self.context.webView.bounds.height - point.y)
                let location = self.context.webView.convert(p, to: nil), time = ProcessInfo.processInfo.systemUptime
                guard self.context.window.contentLayoutRect.contains(location),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [], timestamp: time,
                        windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [], timestamp: time + 0.05,
                        windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else { self.fail("pointer-event"); return }
                let responder = self.context.window.firstResponder as? NSView
                var payload = self.frame
                payload.merge(["stage": stage, "label": label, "inputMode": "appkit-window-pointer-event", "button": "primary", "eventCount": 2,
                    "activeApplication": NSApplication.shared.isActive, "keyWindow": self.context.window.isKeyWindow,
                    "nativeResponderOwned": responder === self.context.webView || responder?.isDescendant(of: self.context.webView) == true,
                    "webViewX": point.x, "webViewY": point.y, "windowX": location.x, "windowY": location.y]) { _, replacement in replacement }
                trace("recv", "lazyGridSelectedRemoval.nativePointerDispatch", payload)
                self.dispatched.append(stage); self.context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard !self.finished else { return }; self.context.window.sendEvent(up)
                    self.resetQuiet(); self.phase = next; self.schedule()
                }
            }
        }
    }
    private func evaluate(_ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard !self.finished else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }; completion()
        }
    }
    private func schedule(after delay: Double = 0.25) { DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() } }
    private func diagnose(_ reason: String) {
        let signature = "\(phase):\(reason):\(latest?.selectionStatus ?? ""):\(latest?.gridSize ?? 0)"
        guard signature != diagnosticSignature else { return }; diagnosticSignature = signature
        var payload = frame; payload["phase"] = "flow-ipc-lazy-grid-selected-removal-" + String(describing: phase)
        payload["reason"] = reason; payload["snapshot"] = latest?.payload
        trace("recv", "lazyGridSelectedRemoval.driverDiagnostic", payload)
    }
    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }; finished = true
        var payload = frame; payload.merge(details) { _, replacement in replacement }
        payload["code"] = "flow-ipc-lazy-grid-selected-removal-" + code
        payload["phase"] = "flow-ipc-lazy-grid-selected-removal-" + String(describing: phase); payload["snapshot"] = latest?.payload
        trace("send", "error", payload); context.window.close()
    }
    private struct Geometry: Decodable { let x: Double; let y: Double; let requestCount: Int; let responseCount: Int }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let queryStatus: String; let countStatus: String; let selectionStatus: String; let selectionHistory: String; let removalStatus: String
        let refreshStatus: String; let refreshAllStatus: String; let filterValue: String
        let gridSize: Int; let selectedItemsCount: Int; let selectionColumnObserved: Bool; let ariaMultiSelectableObserved: Bool
        let renderedRowCount: Int; let renderedLabels: [String]; let visibleLabels: [String]
        let selectedRowLabels: [String]; let checkedRowLabels: [String]; let ariaSelectedRowLabels: [String]
        let selectedRenderedRowCount: Int; let checkedRenderedRowCount: Int; let ariaSelectedRenderedRowCount: Int
        let removeButtonDisabled: Bool; let queryCallCount: Int; let countCallCount: Int; let refreshFetchBaseline: Int; let refreshCountBaseline: Int
        let nativePointerEventCount: Int; let nativePointerTrustedEventCount: Int
        let pointerStages: [String]; let pointerLabels: [String]; let domEventOrder: [String]; let browserProgrammaticStages: [String]
        let browserError: String; let requestError: String; let responseError: String; let pushError: String; let markerError: String
        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var errorFree: Bool { browserError.isEmpty && requestError.isEmpty && responseError.isEmpty && pushError.isEmpty && markerError.isEmpty }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self), let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }; return value
        }
    }
    private static let domHelpers = """
    const grid=document.getElementById('framework-flow-lazy-grid-selection-grid');
    const visit=(node,predicate,seen=new Set())=>{if(!node || seen.has(node))return null;seen.add(node);
      if(node.nodeType===Node.ELEMENT_NODE && predicate(node))return node;
      if(node.shadowRoot){const found=visit(node.shadowRoot,predicate,seen);if(found)return found;}
      if(typeof node.assignedElements==='function')for(const child of node.assignedElements({flatten:true})){const found=visit(child,predicate,seen);if(found)return found;}
      for(const child of node.children || node.childNodes || []){const found=visit(child,predicate,seen);if(found)return found;}return null;};
    const cellForLabel=label=>visit(grid,node=>node.localName==='vaadin-grid-cell-content' && String(node.textContent || '').trim()===label);
    const rowForCell=cell=>{const slot=cell?.assignedSlot;return slot?.closest?.('tr') || Array.from(grid?.shadowRoot?.querySelectorAll('tr') || []).find(row=>slot && row.contains(slot)) || null;};
    const rowForLabel=label=>rowForCell(cellForLabel(label));
    const checkboxForRow=row=>visit(row,node=>node.localName==='vaadin-checkbox');
    const checkboxForLabel=label=>checkboxForRow(rowForLabel(label));
    """
    private static let inspection = """
    (()=>{
      \(domHelpers)
      const d=window.VaadinDesktopIpcDiagnostics || {},scope=window.VaadinDesktopHostContext || {},marker=window.VaadinDesktopLazyGridSelectedRemoval || {};
      const text=suffix=>String(document.getElementById('framework-flow-lazy-grid-selection-'+suffix)?.textContent || '').trim();
      const queryStatus=text('query-status'),countStatus=text('count-status'),removalStatus=text('removal-status');
      const rows=Array.from(grid?._getRenderedRows?.() || grid?.shadowRoot?.querySelectorAll('tr[part~="body-row"]') || []);
      // Preserve actual row order, with identity read from rendered cells, not
      // guesses about Flow's opaque selectedItems object shape.
      const labels=[];for(let i=1;i<=80;i++){const label='L-'+String(i).padStart(3,'0');if(rows.includes(rowForLabel(label)))labels.push(label);}
      // Virtual row DOM order can wrap as rows are recycled. Compare actual
      // laid-out positions instead of treating the reusable row pool as order.
      const ordered=labels.slice().sort((a,b)=>cellForLabel(a).getBoundingClientRect().top-cellForLabel(b).getBoundingClientRect().top);
      const visible=label=>{const cell=cellForLabel(label);if(!cell || !grid)return false;const r=cell.getBoundingClientRect(),g=grid.getBoundingClientRect();return r.width>0 && r.height>0 && r.bottom>g.top && r.top<g.bottom && r.bottom>0 && r.top<innerHeight;};
      const selected=row=>!!row && (row.hasAttribute('selected') || row.selected===true);
      const checked=row=>checkboxForRow(row)?.checked===true;
      const aria=row=>row?.getAttribute('aria-selected')==='true';
      const selectedRows=rows.filter(selected),checkedRows=rows.filter(checked),ariaRows=rows.filter(aria);
      const array=name=>Array.isArray(marker[name])?marker[name]:[];
      const baseline=name=>{const matches=Array.from(removalStatus.matchAll(new RegExp('(?:^|, )'+name+'=(\\\\d+)(?=,|$)','g')));return matches.length===1?Number(matches[0][1]):0;};
      // Vaadin's Grid table uses treegrid in this installed bundle even for
      // flat data. Both ARIA grid roles still require explicit multiselectable.
      const ariaGrid=visit(grid,node=>['grid','treegrid'].includes(node.getAttribute?.('role')) && node.getAttribute?.('aria-multiselectable')==='true');
      const clients=Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name=>name!=='TypeScript');
      return JSON.stringify({rootRouteAvailable:!!document.getElementById('framework-flow-lazy-grid-selection-open-route'),
        routeReady:!!document.getElementById('framework-flow-lazy-grid-selection-route') && !!grid && !!document.getElementById('framework-flow-lazy-grid-selection-remove-selected'),
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        requestCount:Number(d.standardBundleOpaqueRequestCount || 0),responseCount:Number(d.standardBundleOpaqueResponseCount || 0),pushMessageCount:Number(d.standardBundlePushMessageCount || 0),flowClientCount:clients.length,
        browserWindowId:String(scope.windowId || ''),browserSessionId:String(scope.sessionId || ''),browserUiId:String(scope.uiId || ''),
        queryStatus,countStatus,selectionStatus:text('status'),selectionHistory:text('selection-history'),removalStatus,
        refreshStatus:text('refresh-status'),refreshAllStatus:text('refresh-all-status'),filterValue:String(document.getElementById('framework-flow-lazy-grid-selection-filter')?.value || ''),
        gridSize:Number(grid?.size || 0),selectedItemsCount:Array.isArray(grid?.selectedItems)?grid.selectedItems.length:-1,
        selectionColumnObserved:!!grid?.querySelector('vaadin-grid-flow-selection-column'),ariaMultiSelectableObserved:grid?.getAttribute('aria-multiselectable')==='true' || !!ariaGrid,
        renderedRowCount:rows.length,renderedLabels:ordered,visibleLabels:ordered.filter(visible),
        selectedRowLabels:ordered.filter(label=>selected(rowForLabel(label))),checkedRowLabels:ordered.filter(label=>checked(rowForLabel(label))),ariaSelectedRowLabels:ordered.filter(label=>aria(rowForLabel(label))),
        selectedRenderedRowCount:selectedRows.length,checkedRenderedRowCount:checkedRows.length,ariaSelectedRenderedRowCount:ariaRows.length,
        removeButtonDisabled:document.getElementById('framework-flow-lazy-grid-selection-remove-selected')?.disabled===true,
        queryCallCount:Number(queryStatus.match(/^Lazy grid query (\\d+):/)?.[1] || 0),countCallCount:Number(countStatus.match(/^Lazy Grid selection count (\\d+):/)?.[1] || 0),
        refreshFetchBaseline:baseline('fetchBaseline'),refreshCountBaseline:baseline('countBaseline'),
        nativePointerEventCount:array('pointerStages').length,nativePointerTrustedEventCount:Number(marker.nativePointerTrustedEventCount || 0),pointerStages:array('pointerStages'),pointerLabels:array('pointerLabels'),domEventOrder:array('domEventOrder'),browserProgrammaticStages:array('browserProgrammaticStages'),
        browserError:String(d.standardBundleBrowserError || ''),requestError:String(d.standardBundleOpaqueRequestError || ''),responseError:String(d.standardBundleOpaqueResponseError || ''),pushError:String(d.standardBundlePushError || ''),markerError:String(marker.markerError || '')});
    })()
    """
}
