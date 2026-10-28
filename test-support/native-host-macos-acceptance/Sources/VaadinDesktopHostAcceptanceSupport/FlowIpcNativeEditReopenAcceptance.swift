import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleNativeEditReopenAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        guard context.scope == .main else { return }
        FlowIpcNativeEditReopenAcceptance(host: host, main: context, timeoutSeconds: timeoutSeconds).start()
    }
}

// A is retained here for identity comparison only. Registry retirement and a
// hidden old window do not assert object deallocation or destroyed WebKit state.
private final class FlowIpcNativeEditReopenAcceptance {
    private enum Phase { case root, waiting, focusing, drain }
    private static let ownerId = "native-edit-secondary"
    private static let prefix = "framework-flow-native-edit-"
    private static let config = "Native edit config: component=TextField, mode=EAGER, initial=empty, initialLength=0"
    private static let initial = "Native edit value: value=empty, length=0, client=false, change=0, oldValue=none, oldLength=-1"
    private static let stages = ["main-initial", "main-insert", "opened-a", "a-insert", "a-retired", "opened-b", "b-empty-undo", "b-insert", "b-undo", "b-redo", "main-undo", "main-redo"]
    private static let pointerStages = ["main-insert", "open-a", "a-insert", "close-a", "open-b", "b-empty-undo", "b-insert", "b-undo", "b-redo", "main-undo", "main-redo"]
    private static let keyStages = ["main-insert", "a-insert", "b-empty-undo", "b-insert", "b-undo", "b-redo", "main-undo", "main-redo"]
    private let host: WebViewHost
    private let main: FlowIpcWindowContext
    private var a: FlowIpcWindowContext?
    private var b: FlowIpcWindowContext?
    private let mainWindow: ObjectIdentifier
    private let mainWebView: ObjectIdentifier
    private let mainNumber: Int
    private var aWindow: ObjectIdentifier?
    private var aWebView: ObjectIdentifier?
    private var aScope: FlowIpcWindowScope?
    private var aNumber = 0
    private var bWindow: ObjectIdentifier?
    private var bWebView: ObjectIdentifier?
    private var bScope: FlowIpcWindowScope?
    private var bNumber = 0
    private let timeout: Double
    private let deadline: Date
    private var pid: Int32 = 0
    private var finished = false
    private var phase = Phase.root
    private var target = 0
    private var mainBaseline: Snapshot?
    private var secondaryBaseline: Snapshot?
    private var latestMain: Snapshot?
    private var latestSecondary: Snapshot?
    private var retiredA: [String: Any]?
    private var aInsertSnapshot: [String: Any]?
    private var observed: [String] = []
    private var pointers: [String] = []
    private var keys: [String] = []
    private var quietSignature = ""
    private var quietCount = 0
    private var diagnosticSignature = ""
    private var noOpStarted: TimeInterval?
    private var noOpElapsed = 0
    private var noOpQuiet = 0

    init(host: WebViewHost, main: FlowIpcWindowContext, timeoutSeconds: Double) {
        self.host = host; self.main = main
        mainWindow = ObjectIdentifier(main.window); mainWebView = ObjectIdentifier(main.webView)
        mainNumber = main.window.windowNumber
        timeout = timeoutSeconds; deadline = Date().addingTimeInterval(timeoutSeconds)
    }
    func start() {
        guard timeout.isFinite, timeout > 0, timeout <= 300 else { fail("invalid-timeout"); return }
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) {
            guard !self.finished else { return }; self.fail("deadline")
        }
        poll()
    }
    private func retained(_ context: FlowIpcWindowContext) -> Bool {
        let expectedWindow = context === main ? mainWindow : context === a ? aWindow : context === b ? bWindow : nil
        let expectedWebView = context === main ? mainWebView : context === a ? aWebView : context === b ? bWebView : nil
        let expectedScope = context === main ? FlowIpcWindowScope.main : context === a ? aScope : context === b ? bScope : nil
        return host.flowIpcWindowContext(context.scope.windowId) === context
            && ObjectIdentifier(context.window) == expectedWindow && ObjectIdentifier(context.webView) == expectedWebView
            && context.scope == expectedScope && context.recoveryGeneration == 0
            && (pid == 0 || host.sidecarProcessIdentifier == pid)
    }
    // All asynchronous native-input paths are fenced by exact context identity,
    // not the reusable public window ID.
    private func live(_ context: FlowIpcWindowContext) -> Bool {
        guard !finished else { return false }
        guard Date() < deadline else { fail("deadline"); return false }
        guard retained(context) else { fail("incarnation-changed"); return false }
        return true
    }
    private func inspect(_ context: FlowIpcWindowContext, completion: @escaping (Snapshot) -> Void) {
        guard live(context) else { return }
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard self.live(context) else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnose("snapshot-unavailable"); self.schedule(); return
            }
            guard state.errorFree, state.browserWindowId == context.scope.windowId,
                  state.browserSessionId == context.scope.sessionId, state.browserUiId == context.scope.uiId else {
                self.fail("snapshot-scope-or-error", details: ["inspectedContext": context.scope.applying(to: [:]),
                    "rejectedSnapshot": state.payload]); return
            }
            completion(state)
        }
    }
    private var activeSecondary: FlowIpcWindowContext? {
        if target >= 5 { return b }
        if target == 2 || target == 3 { return a }
        return nil
    }
    private func bindSecondary() -> Bool {
        guard target == 2 || target == 5 else { return true }
        guard let owner = host.flowIpcWindowContext(Self.ownerId), owner.navigationFinished else { return false }
        if target == 2 && a == nil || target == 5 && b == nil {
            let prefix = "flow-ipc-window:"
            guard owner.scope.windowId == Self.ownerId, owner.scope.uiId == Self.ownerId,
                  owner.scope.sessionId.hasPrefix(prefix), UUID(uuidString: String(owner.scope.sessionId.dropFirst(prefix.count))) != nil,
                  owner.window !== main.window, owner.webView !== main.webView,
                  owner.window.windowNumber > 0, owner.window.windowNumber != mainNumber else { fail("new-owner-identity"); return false }
            if target == 2 {
                a = owner; aWindow = ObjectIdentifier(owner.window); aWebView = ObjectIdentifier(owner.webView)
                aScope = owner.scope; aNumber = owner.window.windowNumber
            } else {
                guard let a, owner !== a, owner.window !== a.window, owner.webView !== a.webView,
                      owner.scope.sessionId != a.scope.sessionId, owner.window.windowNumber != aNumber,
                      !a.window.isVisible, retiredA != nil else { fail("reopened-owner-not-distinct"); return false }
                b = owner; bWindow = ObjectIdentifier(owner.window); bWebView = ObjectIdentifier(owner.webView)
                bScope = owner.scope; bNumber = owner.window.windowNumber
            }
        }
        return true
    }
    private func poll() {
        guard live(main) else { return }
        inspect(main) { m in
            self.latestMain = m
            if self.phase == .root {
                guard self.main.navigationFinished, m.rootRouteAvailable, m.firstMeaningfulPaint,
                      m.flowClientCount == 1, let pid = self.host.sidecarProcessIdentifier, pid > 0 else { self.schedule(); return }
                self.pid = pid
                self.evaluate(self.main, """
                (() => {
                  const button=document.getElementById('\(Self.prefix)open-route');
                  if (!button) return false;
                  window.VaadinDesktopNativeEditReopen={browserProgrammaticStages:['navigate']};
                  button.click(); return true;
                })()
                """) { self.phase = .waiting; self.schedule() }
                return
            }
            guard self.bindSecondary() else { self.schedule(); return }
            if self.target == 4 {
                // Never inspect the retired WebView. Main's real close-status UIDL
                // and production registry removal are independent requirements.
                guard self.host.flowIpcWindowContext(Self.ownerId) == nil, self.a?.window.isVisible == false else {
                    self.resetQuiet(); self.diagnose("await-retirement"); self.schedule(); return
                }
            }
            if let owner = self.activeSecondary {
                guard self.retained(owner) else { self.fail("owner-identity"); return }
                self.inspect(owner) { s in
                    guard self.live(self.main), self.live(owner) else { return }
                    self.latestSecondary = s; self.advance(m, s)
                }
            } else {
                self.latestSecondary = nil; self.advance(m, nil)
            }
        }
    }
    private func changes(_ stage: Int, owner: String) -> Int {
        if owner == "main" { return stage == 0 ? 0 : stage < 10 ? 1 : stage == 10 ? 2 : 3 }
        if owner == "a" { return stage < 3 ? 0 : 1 }
        return stage < 7 ? 0 : stage == 7 ? 1 : stage == 8 ? 2 : 3
    }
    private func ownerName(_ stage: String) -> String {
        if stage.hasPrefix("a-") { return "a" }
        if stage.hasPrefix("b-") { return "b" }
        return "main"
    }
    private func contextForAction() -> FlowIpcWindowContext? {
        switch ownerName(Self.stages[target]) { case "a": return target == 4 ? main : a; case "b": return b; default: return main }
    }
    private func advance(_ m: Snapshot, _ s: Snapshot?) {
        let expectedStage = phase == .focusing ? target - 1 : target
        let owner = target >= 5 ? "b" : "a"
        let openCount = expectedStage < 2 ? 0 : expectedStage < 5 ? 1 : 2
        let closeStatus = expectedStage < 4 ? "Native edit close: pending" : "Native edit close: closing=true, client=true, count=1, error=none"
        let windowStatus = openCount == 0 ? "Native edit window: pending" : "Native edit window: opened=true, client=true, count=\(openCount), error=none"
        guard stateMatches(m, owner: "main", changes: changes(expectedStage, owner: "main")),
              m.windowStatus == windowStatus, m.closeStatus == closeStatus, m.balanced,
              s == nil || (stateMatches(s!, owner: owner, changes: changes(expectedStage, owner: owner))
                && s!.windowStatus == "Native edit window: pending" && s!.closeStatus == "Native edit close: pending" && s!.balanced) else {
            resetQuiet(); diagnose("state-" + String(expectedStage)); schedule(); return
        }
        if phase == .focusing {
            guard let context = contextForAction(), let state = context === main ? m : s,
                  state.ownerFocused, nativeOwner(context) else { diagnose("field-focus"); schedule(); return }
            mainBaseline = m; secondaryBaseline = s; dispatchKey(context); return
        }
        if target > 0 {
            let keyAction = Self.keyStages.contains(Self.stages[target])
            let context = contextForAction()
            if keyAction {
                guard let context, (context === main ? m : s)?.ownerFocused == true, nativeOwner(context) else {
                    resetQuiet(); diagnose("key-owner"); schedule(); return
                }
            }
            if target != 6 {
                let state = context === main ? m : s, before = context === main ? mainBaseline : secondaryBaseline
                guard let state, let before, state.requestCount > before.requestCount,
                      state.responseCount > before.responseCount else { resetQuiet(); diagnose("action-round-trip"); schedule(); return }
            }
        }
        let requiredQuiet = phase == .drain || target == 6 ? 3 : 2
        guard quiet(m, s, polls: requiredQuiet) else { schedule(); return }
        if target == 6 {
            guard let started = noOpStarted else { fail("noop-timestamp-missing"); return }
            let elapsed = Int((ProcessInfo.processInfo.systemUptime - started) * 1000)
            guard elapsed >= 1000 else { schedule(); return }
            noOpElapsed = elapsed; noOpQuiet = quietCount
        }
        if phase == .drain { finish(m, s); return }
        if target == 0 { mainBaseline = m }
        if target == 2 || target == 5 { secondaryBaseline = s }
        if target == 4 {
            guard let archived = aInsertSnapshot else { fail("retired-snapshot-missing"); return }
            retiredA = archived
        }
        record(m, s)
        if target == 11 { phase = .drain; resetQuiet(); schedule(); return }
        mainBaseline = m; secondaryBaseline = s
        target += 1; resetQuiet()
        let pointer = Self.pointerStages[target - 1]
        let button = target == 2 || target == 4 || target == 5
        guard let context = button ? main : contextForAction() else { fail("action-context"); return }
        click(context, stage: pointer, button: button) {
            self.phase = button ? .waiting : .focusing; self.schedule()
        }
    }
    private func stateMatches(_ state: Snapshot, owner: String, changes: Int) -> Bool {
        let letter = owner == "main" ? "m" : owner == "a" ? "x" : "y"
        let localPointers = pointers.filter { ownerName($0) == owner }
        let localKeys = keys.filter { ownerName($0) == owner }
        let keyStages = localKeys.filter { $0.hasSuffix("-insert") || state.keyboardStages.contains($0) }
        let keyNames = keyStages.map { $0.hasSuffix("-insert") ? letter : $0.hasSuffix("-undo") ? "z" : "Z" }
        let inputStages = localKeys.filter { $0 != "b-empty-undo" }
        let values = [letter, "", letter], types = ["insertText", "historyUndo", "historyRedo"]
        var order: [String] = []
        for pointer in localPointers {
            order.append("pointer:" + pointer)
            if keyStages.contains(pointer) { order.append("key:" + pointer) }
            if inputStages.contains(pointer) { order.append("input:" + pointer) }
        }
        let entries = (1...3).prefix(changes).map { Self.valueEntry(letter, change: $0) }
        let display = changes == 0 || changes == 2 ? "" : letter
        let value = changes == 0 ? Self.initial : "Native edit value: " + Self.valueEntry(letter, change: changes)
        return state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1
            && state.configStatus == Self.config && state.displayValue == display && state.fieldValue == display
            && state.valueStatus == value && state.historyStatus == "Native edit history: " + (changes == 0 ? "pending" : entries.joined(separator: " | "))
            && state.fieldAutocorrect == "off" && state.inputAutocorrect == "off"
            && state.fieldAutocapitalize == "none" && state.inputAutocapitalize == "none"
            && !state.invalid && !state.readOnly && state.enabled && state.inputType == "text" && state.label == "Native edit text"
            && state.pointerStages == localPointers && state.nativePointerEventCount == localPointers.count && state.nativePointerTrustedEventCount == localPointers.count
            && state.keyboardStages == keyStages && state.keyboardKeys == keyNames
            && state.nativeKeyboardEventCount == keyStages.count && state.nativeKeyboardTrustedEventCount == keyStages.count
            && inputStages.count == changes && state.inputStages == inputStages
            && state.inputValues == Array(values.prefix(changes)) && state.inputTypes == Array(types.prefix(changes))
            && state.nativeInputEventCount == changes && state.nativeInputTrustedEventCount == changes && state.domEventOrder == order
            && state.browserProgrammaticStages == (owner == "main" ? ["navigate"] : []) + localPointers.map { "position-" + $0 }
    }
    private static func valueEntry(_ letter: String, change: Int) -> String {
        let undo = change == 2
        return "value=\(undo ? "empty" : letter), length=\(undo ? 0 : 1), client=true, change=\(change), oldValue=\(undo ? letter : "empty"), oldLength=\(undo ? 1 : 0)"
    }
    private func nativeOwner(_ context: FlowIpcWindowContext) -> Bool {
        let responder = context.window.firstResponder as? NSView
        return NSApplication.shared.isActive && context.window.isKeyWindow && NSApplication.shared.keyWindow === context.window
            && (responder === context.webView || responder?.isDescendant(of: context.webView) == true)
            && [main, a, b].compactMap { $0 }.filter { $0 !== context }.allSatisfy { !$0.window.isKeyWindow }
    }
    private func quiet(_ m: Snapshot, _ s: Snapshot?, polls: Int) -> Bool {
        let signature = "\(phase):\(target):\(m.requestCount):\(m.responseCount):\(m.pushMessageCount):\(s?.requestCount ?? 0):\(s?.responseCount ?? 0):\(s?.pushMessageCount ?? 0)"
        quietCount = signature == quietSignature ? quietCount + 1 : 1; quietSignature = signature
        return quietCount >= polls
    }
    private func resetQuiet() { quietSignature = ""; quietCount = 0 }
    private var frame: [String: Any] {
        var value: [String: Any] = ["hostProcessIdentifier": getpid(), "processIdentifier": pid, "recoveryGeneration": 0,
            "mainContextRetained": host.flowIpcWindowContext("main") === main,
            "mainWindowRetained": ObjectIdentifier(main.window) == mainWindow, "mainWebViewRetained": ObjectIdentifier(main.webView) == mainWebView,
            "mainWindowId": main.scope.windowId, "mainSessionId": main.scope.sessionId, "mainUiId": main.scope.uiId,
            "mainWindowNumber": mainNumber, "mainKeyWindow": main.window.isKeyWindow]
        for (name, context, number, window, webView) in [("a", a, aNumber, aWindow, aWebView), ("b", b, bNumber, bWindow, bWebView)] {
            guard let context else { continue }
            value[name + "WindowId"] = context.scope.windowId; value[name + "SessionId"] = context.scope.sessionId; value[name + "UiId"] = context.scope.uiId
            value[name + "WindowNumber"] = number; value[name + "KeyWindow"] = context.window.isKeyWindow
            value[name + "ContextRegistered"] = host.flowIpcWindowContext(Self.ownerId) === context
            value[name + "ReferencesRetained"] = ObjectIdentifier(context.window) == window && ObjectIdentifier(context.webView) == webView
            value[name + "WindowHidden"] = !context.window.isVisible
        }
        if let a, let b {
            value["bContextDistinctFromA"] = b !== a; value["bWindowDistinctFromA"] = b.window !== a.window
            value["bWebViewDistinctFromA"] = b.webView !== a.webView; value["bSessionDistinctFromA"] = b.scope.sessionId != a.scope.sessionId
        }
        return value
    }
    private func nested(_ state: Snapshot, baseline: Snapshot?) -> [String: Any] {
        var value = state.payload
        value["requestBaseline"] = baseline?.requestCount ?? state.requestCount
        value["responseBaseline"] = baseline?.responseCount ?? state.responseCount
        return value
    }
    private func payload(_ m: Snapshot, _ s: Snapshot?) -> [String: Any] {
        var value = frame
        value["main"] = nested(m, baseline: mainBaseline)
        value["secondary"] = s.map { nested($0, baseline: secondaryBaseline) } as Any? ?? NSNull()
        value["retiredA"] = retiredA as Any? ?? NSNull()
        value["stage"] = Self.stages[target]
        let name = target == 4 ? "main" : ownerName(Self.stages[target])
        value["incarnation"] = name; value["actionWindowId"] = name == "main" ? "main" : Self.ownerId
        value["balancedDrainObserved"] = m.balanced && (s?.balanced ?? true)
        value["peerStateUnchanged"] = true; value["actionUidlRoundTripObserved"] = target != 0 && target != 6
        value["quietPollCount"] = quietCount; value["noOpElapsedMillis"] = noOpElapsed; value["noOpQuietPollCount"] = noOpQuiet
        return value
    }
    private func record(_ m: Snapshot, _ s: Snapshot?) {
        let value = payload(m, s)
        if target == 3 { aInsertSnapshot = value["secondary"] as? [String: Any] }
        observed.append(Self.stages[target]); trace("recv", "nativeEditReopen.stageObserved", value)
    }
    private func finish(_ m: Snapshot, _ s: Snapshot?) {
        guard observed == Self.stages, pointers == Self.pointerStages, keys == Self.keyStages,
              let retiredA, let aPointers = retiredA["nativePointerTrustedEventCount"] as? Int,
              let aInputs = retiredA["nativeInputEventCount"] as? Int,
              let aTrustedInputs = retiredA["nativeInputTrustedEventCount"] as? Int,
              let b, let s else { fail("final-contract"); return }
        var value = payload(m, s)
        value.merge(["complete": true, "errorFree": true, "standardBundleMode": true, "flowClientOwned": true,
            "opaqueRequestBridgeObserved": true, "opaqueResponseBridgeObserved": true, "pushObserved": false,
            "generatedAppKitInput": true, "physicalInputClaimed": false, "imeClaimed": false, "assistiveTechnologyClaimed": false,
            "nativeInputCausalObserved": true, "oldOwnerRetiredObserved": true, "freshOwnerDistinctObserved": true,
            "freshUndoNoOpObserved": true, "freshUndoRedoObserved": true, "mainHistoryRetainedObserved": true,
            "observedStages": observed, "nativePointerStages": pointers, "nativeKeyStages": keys,
            "nativePointerEventCount": pointers.count, "nativePointerTrustedEventCount": m.nativePointerTrustedEventCount + s.nativePointerTrustedEventCount + aPointers,
            "nativeKeyDispatchCount": keys.count, "nativeInputEventCount": m.nativeInputEventCount + s.nativeInputEventCount + aInputs,
            "nativeInputTrustedEventCount": m.nativeInputTrustedEventCount + s.nativeInputTrustedEventCount + aTrustedInputs,
            "drainQuietPollCount": quietCount]) { _, replacement in replacement }
        finished = true; trace("recv", "nativeEditReopen.frameworkAcceptance", value)
        b.window.close(); main.window.close()
    }
    private func dispatchFrame(_ context: FlowIpcWindowContext, stage: String) -> [String: Any] {
        var value = context.scope.applying(to: frame)
        value["stage"] = stage; value["windowNumber"] = context.window.windowNumber
        value["activeApplication"] = NSApplication.shared.isActive; value["keyWindow"] = context.window.isKeyWindow
        value["nativeResponderOwned"] = nativeOwner(context); value["eventCount"] = 2
        return value
    }

    private func click(_ context: FlowIpcWindowContext, stage: String, button: Bool, completion: @escaping () -> Void) {
        guard live(context) else { return }
        context.webView.evaluateJavaScript("""
        (() => {
          \(Self.instrumentation)
          const target = \(button ? "document.getElementById('framework-flow-native-edit-\(stage == "close-a" ? "close-secondary" : "open-secondary")')" : "input");
          if (!target) return '';
          target.scrollIntoView({block:'center',inline:'nearest'});
          const rect=target.getBoundingClientRect(), x=rect.left+rect.width/2, y=rect.top+rect.height/2;
          let hit=document.elementFromPoint(x,y);
          while (hit?.shadowRoot?.elementFromPoint) { const inner=hit.shadowRoot.elementFromPoint(x,y); if (!inner || inner===hit) break; hit=inner; }
          let node=hit, owned=false; const seen=new Set();
          while (node && !seen.has(node)) { if (node===target) { owned=true; break; } seen.add(node); node=node.assignedSlot || node.parentNode || node.getRootNode?.().host || null; }
          const targetMatched=owned && rect.width>0 && rect.height>0 && Number.isFinite(x) && Number.isFinite(y) && x>=0 && y>=0 && x<innerWidth && y<innerHeight;
          if (targetMatched) { marker.expectedPointer={stage:'\(stage)',target:'\(button ? (stage == "close-a" ? "close-secondary" : "open-secondary") : "field")'}; marker.browserProgrammaticStages.push('position-\(stage)'); }
          return JSON.stringify({x,y,targetMatched});
        })()
        """) { result, error in
            guard self.live(context) else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(Geometry.self, from: data), point.targetMatched else { self.fail("pointer-geometry"); return }
            self.activate(context) {
                guard self.live(context) else { return }
                guard self.nativeOwner(context) else { self.fail("pointer-native-owner"); return }
                let p = NSPoint(x: point.x, y: context.webView.isFlipped ? point.y : context.webView.bounds.height - point.y)
                let location = context.webView.convert(p, to: nil), time = ProcessInfo.processInfo.systemUptime
                guard context.window.contentLayoutRect.contains(location),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [], timestamp: time,
                        windowNumber: context.window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [], timestamp: time + 0.05,
                        windowNumber: context.window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else { self.fail("pointer-event"); return }
                var payload = self.dispatchFrame(context, stage: stage)
                payload["button"] = "primary"; payload["inputMode"] = "appkit-window-pointer-event"
                trace("recv", "nativeEditReopen.nativePointerDispatch", payload)
                self.pointers.append(stage); context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard self.live(context) else { return }
                    guard self.nativeOwner(context) else { self.fail("pointer-owner-changed"); return }
                    context.window.sendEvent(up); completion()
                }
            }
        }
    }


    private func dispatchKey(_ context: FlowIpcWindowContext) {
        let stage = Self.stages[target], name = ownerName(stage)
        let letter = name == "main" ? "m" : name == "a" ? "x" : "y"
        let inserting = stage.hasSuffix("-insert"), redo = stage.hasSuffix("-redo"), noOp = stage == "b-empty-undo"
        let key = inserting ? letter : redo ? "Z" : "z"
        let specification: [String: Any] = ["stage": stage, "key": key, "command": !inserting, "shift": redo,
            "value": inserting || redo ? letter : "", "inputType": noOp ? "" : inserting ? "insertText" : redo ? "historyRedo" : "historyUndo"]
        guard let data = try? JSONSerialization.data(withJSONObject: specification), let json = String(data: data, encoding: .utf8) else { fail("key-spec"); return }
        evaluate(context, """
        (() => {
          const marker=window.VaadinDesktopNativeEditReopen, input=document.getElementById('framework-flow-native-edit-field')?.inputElement;
          let active=document.activeElement; while (active?.shadowRoot?.activeElement) active=active.shadowRoot.activeElement;
          if (!marker || !input || active!==input) return false;
          marker.expectedKey=\(json); marker.keyObserved=false; return true;
        })()
        """) {
            self.activate(context) {
                guard self.live(context) else { return }
                guard self.nativeOwner(context) else { self.fail("key-native-owner"); return }
                var flags: NSEvent.ModifierFlags = []; if !inserting { flags.insert(.command) }; if redo { flags.insert(.shift) }
                let time = ProcessInfo.processInfo.systemUptime, code: UInt16 = !inserting ? 6 : letter == "m" ? 46 : letter == "x" ? 7 : 16
                guard let down = NSEvent.keyEvent(with: .keyDown, location: .zero, modifierFlags: flags, timestamp: time,
                        windowNumber: context.window.windowNumber, context: nil, characters: key, charactersIgnoringModifiers: key, isARepeat: false, keyCode: code),
                      let up = NSEvent.keyEvent(with: .keyUp, location: .zero, modifierFlags: flags, timestamp: time + 0.01,
                        windowNumber: context.window.windowNumber, context: nil, characters: key, charactersIgnoringModifiers: key, isARepeat: false, keyCode: code) else { self.fail("key-event"); return }
                var payload = self.dispatchFrame(context, stage: stage)
                payload.merge(["key": key, "keyCode": Int(code), "modifiers": redo ? "command+shift" : inserting ? "none" : "command",
                    "inputMode": "appkit-application-key-event"]) { _, replacement in replacement }
                trace("recv", "nativeEditReopen.nativeKeyDispatch", payload)
                self.keys.append(stage)
                if noOp { self.noOpStarted = ProcessInfo.processInfo.systemUptime }
                NSApplication.shared.sendEvent(down); NSApplication.shared.sendEvent(up)
                self.resetQuiet(); self.phase = .waiting; self.schedule()
            }
        }
    }
    private func activate(_ context: FlowIpcWindowContext, completion: @escaping () -> Void) {
        guard live(context) else { return }
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline,
            onFailure: { details in
                guard self.live(context) else { return }; self.fail("activation", details: details)
            }, completion: {
                guard self.live(context) else { return }; completion()
            })
    }
    private func evaluate(_ context: FlowIpcWindowContext, _ script: String, completion: @escaping () -> Void) {
        guard live(context) else { return }
        context.webView.evaluateJavaScript(script) { result, error in
            guard self.live(context) else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }; completion()
        }
    }
    private func schedule(after delay: Double = 0.2) { DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() } }
    private func diagnose(_ reason: String) {
        let signature = "\(phase):\(target):\(reason):\(NSApplication.shared.isActive):\(main.window.isKeyWindow):\(activeSecondary?.window.isKeyWindow == true)"
        guard signature != diagnosticSignature else { return }; diagnosticSignature = signature
        var value = frame
        value["phase"] = "flow-ipc-native-edit-reopen-" + String(describing: phase); value["reason"] = reason; value["targetIndex"] = target
        value["main"] = latestMain?.payload; value["secondary"] = latestSecondary?.payload
        trace("recv", "nativeEditReopen.driverDiagnostic", value)
    }
    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }; finished = true
        var value = frame; value.merge(details) { _, replacement in replacement }
        value["code"] = "flow-ipc-native-edit-reopen-" + code; value["phase"] = "flow-ipc-native-edit-reopen-" + String(describing: phase)
        value["targetIndex"] = target; value["main"] = latestMain?.payload; value["secondary"] = latestSecondary?.payload
        trace("send", "error", value)
        host.flowIpcWindowContext(Self.ownerId)?.window.close(); main.window.close()
    }
    private struct Geometry: Decodable { let x: Double; let y: Double; let targetMatched: Bool }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let displayValue: String; let fieldValue: String; let configStatus: String; let valueStatus: String; let historyStatus: String; let windowStatus: String; let closeStatus: String; let selectionStart: Int; let selectionEnd: Int
        let invalid: Bool; let readOnly: Bool; let enabled: Bool; let inputMode: String; let inputType: String; let label: String; let ownerFocused: Bool
        let fieldAutocorrect: String; let inputAutocorrect: String; let fieldAutocapitalize: String; let inputAutocapitalize: String
        let nativePointerEventCount: Int; let nativePointerTrustedEventCount: Int; let nativeKeyboardEventCount: Int; let nativeKeyboardTrustedEventCount: Int
        let nativeInputEventCount: Int; let nativeInputTrustedEventCount: Int
        let pointerStages: [String]; let keyboardStages: [String]; let keyboardKeys: [String]
        let inputStages: [String]; let inputTypes: [String]; let inputValues: [String]; let domEventOrder: [String]; let browserProgrammaticStages: [String]
        let browserError: String; let requestError: String; let responseError: String; let pushError: String; let markerError: String
        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var errorFree: Bool { browserError.isEmpty && requestError.isEmpty && responseError.isEmpty && pushError.isEmpty && markerError.isEmpty }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self), let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }
            return value
        }
    }
    private static let instrumentation = """
    const marker=window.VaadinDesktopNativeEditReopen ||= {browserProgrammaticStages:[]};
    const field=document.getElementById('framework-flow-native-edit-field'), input=field?.inputElement;
    if (!input) return '';
    if (!marker.installed) {
      marker.installed=true;
      for (const name of ['pointerStages','keyboardStages','keyboardKeys','inputStages','inputTypes','inputValues','domEventOrder']) marker[name]=[];
      marker.nativePointerTrustedEventCount=0; marker.nativeKeyboardTrustedEventCount=0; marker.nativeInputTrustedEventCount=0;
      document.addEventListener('click',event=>{
        const path=event.composedPath(), expected=marker.expectedPointer;
        const open=document.getElementById('framework-flow-native-edit-open-secondary'), close=document.getElementById('framework-flow-native-edit-close-secondary');
        const target=expected?.target==='field'?input:document.getElementById('framework-flow-native-edit-'+(expected?.target || ''));
        if (!path.includes(input) && !path.includes(open) && !path.includes(close)) return;
        if (!expected || event.button!==0 || !path.includes(target)) {
          marker.markerError='unexpected-owned-click:'+JSON.stringify({expectedStage:expected?.stage || '',button:event.button,
            isTrusted:event.isTrusted,inputOwned:path.includes(input),openOwned:path.includes(open)}); return;
        }
        marker.pointerStages.push(expected.stage); marker.domEventOrder.push('pointer:'+expected.stage);
        marker.nativePointerTrustedEventCount+=event.isTrusted===true?1:0; marker.expectedPointer=null;
      },true);
      document.addEventListener('keydown',event=>{
        const path=event.composedPath(), expected=marker.expectedKey;
        if (!path.includes(field) || !path.includes(input)) return;
        if (!expected || event.key!==expected.key || event.metaKey!==expected.command || event.shiftKey!==expected.shift
            || event.ctrlKey || event.altKey || event.repeat || marker.keyObserved) {
          marker.markerError='unexpected-owned-keydown:'+JSON.stringify({expectedStage:expected?.stage || '',key:event.key,
            metaKey:event.metaKey,shiftKey:event.shiftKey,ctrlKey:event.ctrlKey,altKey:event.altKey,repeat:event.repeat,
            isTrusted:event.isTrusted,keyAlreadyObserved:marker.keyObserved===true}); return;
        }
        marker.keyObserved=true; marker.keyboardStages.push(expected.stage); marker.keyboardKeys.push(event.key);
        marker.nativeKeyboardTrustedEventCount+=event.isTrusted===true?1:0; marker.domEventOrder.push('key:'+expected.stage);
      },true);
      document.addEventListener('input',event=>{
        const path=event.composedPath(), expected=marker.expectedKey;
        if (!path.includes(field) || !path.includes(input)) return;
        if (!expected || !expected.inputType || (!expected.command && !marker.keyObserved) || event.isComposing || event.inputType!==expected.inputType
            || input.value!==expected.value || marker.inputStages.includes(expected.stage)) {
          marker.markerError='unexpected-owned-input:'+JSON.stringify({expectedStage:expected?.stage || '',inputType:event.inputType,
            value:input.value,isTrusted:event.isTrusted,isComposing:event.isComposing,
            keyObserved:marker.keyObserved===true,inputStages:marker.inputStages.slice()}); return;
        }
        marker.inputStages.push(expected.stage); marker.inputTypes.push(event.inputType); marker.inputValues.push(input.value);
        marker.nativeInputTrustedEventCount+=event.isTrusted===true?1:0; marker.domEventOrder.push('input:'+expected.stage);
      },true);
    }
    """
    private static let inspection = """
    (()=>{
      const marker=window.VaadinDesktopNativeEditReopen || {}, d=window.VaadinDesktopIpcDiagnostics || {}, scope=window.VaadinDesktopHostContext || {};
      const field=document.getElementById('framework-flow-native-edit-field'), input=field?.inputElement;
      const text=suffix=>String(document.getElementById('framework-flow-native-edit-'+suffix)?.textContent || '').trim();
      const array=name=>Array.isArray(marker[name])?marker[name]:[];
      let active=document.activeElement; while (active?.shadowRoot?.activeElement) active=active.shadowRoot.activeElement;
      const clients=Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name=>name!=='TypeScript');
      return JSON.stringify({
        rootRouteAvailable:!!document.getElementById('framework-flow-native-edit-open-route'),
        routeReady:!!document.getElementById('framework-flow-native-edit-route') && !!input && text('config-status')==='\(config)'
          && !field.disabled && !field.readonly && !field.invalid && input.type==='text' && field.label==='Native edit text'
          && field.getAttribute('autocorrect')==='off' && input.getAttribute('autocorrect')==='off'
          && field.getAttribute('autocapitalize')==='none' && input.getAttribute('autocapitalize')==='none',
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        requestCount:Number(d.standardBundleOpaqueRequestCount || 0),responseCount:Number(d.standardBundleOpaqueResponseCount || 0),pushMessageCount:Number(d.standardBundlePushMessageCount || 0),flowClientCount:clients.length,
        browserWindowId:String(scope.windowId || ''),browserSessionId:String(scope.sessionId || ''),browserUiId:String(scope.uiId || ''),
        displayValue:String(input?.value || ''),fieldValue:String(field?.value || ''),configStatus:text('config-status'),valueStatus:text('value-status'),historyStatus:text('history-status'),windowStatus:text('window-status'),closeStatus:text('close-status'),selectionStart:Number(input?.selectionStart ?? -1),selectionEnd:Number(input?.selectionEnd ?? -1),
        invalid:field?.invalid===true,readOnly:field?.readonly===true,enabled:!!field && field.disabled!==true,inputMode:String(input?.inputMode || ''),inputType:String(input?.type || ''),label:String(field?.label || ''),ownerFocused:!!input && active===input,
        fieldAutocorrect:field?.getAttribute('autocorrect') || '',inputAutocorrect:input?.getAttribute('autocorrect') || '',
        fieldAutocapitalize:field?.getAttribute('autocapitalize') || '',inputAutocapitalize:input?.getAttribute('autocapitalize') || '',
        nativePointerEventCount:array('pointerStages').length,nativePointerTrustedEventCount:Number(marker.nativePointerTrustedEventCount || 0),
        nativeKeyboardEventCount:array('keyboardStages').length,nativeKeyboardTrustedEventCount:Number(marker.nativeKeyboardTrustedEventCount || 0),
        nativeInputEventCount:array('inputStages').length,nativeInputTrustedEventCount:Number(marker.nativeInputTrustedEventCount || 0),
        pointerStages:array('pointerStages'),keyboardStages:array('keyboardStages'),keyboardKeys:array('keyboardKeys'),inputStages:array('inputStages'),inputTypes:array('inputTypes'),inputValues:array('inputValues'),domEventOrder:array('domEventOrder'),browserProgrammaticStages:array('browserProgrammaticStages'),
        browserError:String(d.standardBundleBrowserError || ''),requestError:String(d.standardBundleOpaqueRequestError || ''),responseError:String(d.standardBundleOpaqueResponseError || ''),pushError:String(d.standardBundlePushError || ''),markerError:String(marker.markerError || '')
      });
    })()
    """
}
