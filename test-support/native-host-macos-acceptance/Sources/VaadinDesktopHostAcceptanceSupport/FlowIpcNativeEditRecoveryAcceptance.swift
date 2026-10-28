import AppKit
import Darwin
import Foundation

private var nativeEditRecoveryAcceptanceStarted = false

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleNativeEditRecoveryAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        guard context.scope == .main, context.recoveryGeneration == 0, !nativeEditRecoveryAcceptanceStarted else { return }
        // didFinish also runs after both retained WebViews reload. There is only
        // one coordinator, and it owns its polling and independent deadline.
        nativeEditRecoveryAcceptanceStarted = true
        FlowIpcNativeEditRecoveryAcceptance(host: host, main: context, timeoutSeconds: timeoutSeconds).start()
    }
}

private final class FlowIpcNativeEditRecoveryAcceptance {
    private enum Phase { case root, waiting, focusing, recovering, drain }
    private static let ownerId = "native-edit-secondary"
    private static let prefix = "framework-flow-native-edit-"
    private static let config = "Native edit config: component=TextField, mode=EAGER, initial=empty, initialLength=0"
    private static let initial = "Native edit value: value=empty, length=0, client=false, change=0, oldValue=none, oldLength=-1"
    private static let stages = ["main-initial", "opened-secondary", "old-insert", "recovered-ready", "fresh-empty-undo", "fresh-insert", "fresh-undo", "fresh-redo"]
    private static let pointerStages = ["open-secondary", "old-insert", "fresh-empty-undo", "fresh-insert", "fresh-undo", "fresh-redo"]
    private static let keyStages = ["old-insert", "fresh-empty-undo", "fresh-insert", "fresh-undo", "fresh-redo"]
    private let host: WebViewHost
    private let main: FlowIpcWindowContext
    private var secondary: FlowIpcWindowContext?
    private let mainWindow: ObjectIdentifier
    private let mainWebView: ObjectIdentifier
    private let mainNumber: Int
    private var secondaryWindow: ObjectIdentifier?
    private var secondaryWebView: ObjectIdentifier?
    private var secondaryScope: FlowIpcWindowScope?
    private var secondaryNumber = 0
    private let timeout: Double
    private let deadline: Date
    private var pid: Int32 = 0
    private var initialPID: Int32 = 0
    private var failedPID: Int32 = 0
    private var recoveredPID: Int32 = 0
    private var generation = 0
    private var finished = false
    private var phase = Phase.root
    private var target = 0
    private var mainBaseline: Snapshot?
    private var secondaryBaseline: Snapshot?
    private var latestMain: Snapshot?
    private var latestSecondary: Snapshot?
    private var oldMain: [String: Any]?
    private var oldSecondary: [String: Any]?
    private var origins: [String: Double] = [:]
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
    private func identityRetained(_ context: FlowIpcWindowContext) -> Bool {
        guard context === main || context === secondary else { return false }
        return host.flowIpcWindowContext(context.scope.windowId) === context
            && ObjectIdentifier(context.window) == (context === main ? mainWindow : secondaryWindow)
            && ObjectIdentifier(context.webView) == (context === main ? mainWebView : secondaryWebView)
            && context.scope == (context === main ? .main : secondaryScope)
            && context.window.windowNumber == (context === main ? mainNumber : secondaryNumber)
    }
    private func live(_ context: FlowIpcWindowContext) -> Bool {
        guard !finished else { return false }
        guard Date() < deadline else { fail("deadline"); return false }
        guard identityRetained(context), context.recoveryGeneration == generation,
              pid == 0 || host.sidecarProcessIdentifier == pid else { fail("owner-epoch-changed"); return false }
        return true
    }
    // The objects and opaque scope survive recovery. Capture the document epoch
    // and process as well: an old evaluate/activation completion is not fresh DOM.
    private func callbackLive(_ context: FlowIpcWindowContext, epoch: Int, process: Int32) -> Bool {
        guard !finished, epoch == generation, process == pid, phase != .recovering else { return false }
        return live(context)
    }
    private func inspect(_ context: FlowIpcWindowContext, completion: @escaping (Snapshot) -> Void) {
        guard live(context) else { return }
        let epoch = generation, process = pid
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard self.callbackLive(context, epoch: epoch, process: process) else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnose("snapshot-unavailable"); self.schedule(); return
            }
            guard state.errorFree, state.browserWindowId == context.scope.windowId,
                  state.browserSessionId == context.scope.sessionId, state.browserUiId == context.scope.uiId,
                  state.browserRecoveryGeneration == epoch, state.documentTimeOriginMillis.isFinite,
                  state.documentTimeOriginMillis > 0 else {
                self.fail("snapshot-scope-or-error", details: ["inspectedContext": context.scope.applying(to: [:]),
                    "rejectedSnapshot": state.payload]); return
            }
            let originKey = "\(epoch):\(context.scope.windowId)"
            if let origin = self.origins[originKey], origin != state.documentTimeOriginMillis {
                self.fail("document-changed-within-epoch"); return
            }
            self.origins[originKey] = state.documentTimeOriginMillis
            completion(state)
        }
    }
    private func poll() {
        guard !finished else { return }
        guard Date() < deadline else { fail("deadline"); return }
        if phase == .recovering { awaitRecovery(); return }
        guard live(main) else { return }
        inspect(main) { m in
            self.latestMain = m
            if self.phase == .root {
                guard self.main.navigationFinished, m.rootRouteAvailable, m.firstMeaningfulPaint, m.flowClientCount == 1,
                      let pid = self.host.sidecarProcessIdentifier, pid > 0 else { self.schedule(); return }
                self.pid = pid; self.initialPID = pid
                self.evaluate(self.main, """
                (() => {
                  const button=document.getElementById('\(Self.prefix)open-route');
                  if (!button) return false;
                  const marker=window.VaadinDesktopNativeEditRecovery ||= {browserProgrammaticStages:[]};
                  marker.browserProgrammaticStages.push('navigate'); button.click(); return true;
                })()
                """) { self.phase = .waiting; self.schedule() }
                return
            }
            if self.target == 0 { self.advance(m, nil); return }
            guard let owner = self.host.flowIpcWindowContext(Self.ownerId), owner.navigationFinished else {
                if self.target == 1 { self.schedule() } else { self.fail("secondary-missing") }; return
            }
            if self.secondary == nil {
                let prefix = "flow-ipc-window:"
                guard owner.scope.windowId == Self.ownerId, owner.scope.uiId == Self.ownerId,
                      owner.scope.sessionId.hasPrefix(prefix), UUID(uuidString: String(owner.scope.sessionId.dropFirst(prefix.count))) != nil,
                      owner.window !== self.main.window, owner.webView !== self.main.webView,
                      owner.window.windowNumber > 0, owner.window.windowNumber != self.mainNumber else { self.fail("secondary-identity"); return }
                self.secondary = owner; self.secondaryWindow = ObjectIdentifier(owner.window)
                self.secondaryWebView = ObjectIdentifier(owner.webView); self.secondaryScope = owner.scope
                self.secondaryNumber = owner.window.windowNumber
            }
            guard owner === self.secondary, self.live(owner) else { return }
            self.inspect(owner) { s in
                guard self.live(self.main), self.live(owner) else { return }
                self.latestSecondary = s; self.advance(m, s)
            }
        }
    }
    private func advance(_ m: Snapshot, _ s: Snapshot?) {
        let changes = target < 2 ? 0 : generation == 0 ? (phase == .focusing ? 0 : 1)
            : target <= 4 ? 0 : target - (phase == .focusing ? 5 : 4)
        let expectedMainWindow = target == 0 ? "Native edit window: pending" : "Native edit window: opened=true, client=true, count=1, error=none"
        let mainMatches = generation == 0
            ? editMatches(m, owner: false, changes: 0) && m.windowStatus == expectedMainWindow && m.closeStatus == "Native edit close: pending"
            : rootMatches(m)
        guard mainMatches, m.balanced,
              s == nil || (editMatches(s!, owner: true, changes: changes) && s!.balanced
                && s!.windowStatus == "Native edit window: pending" && s!.closeStatus == "Native edit close: pending") else {
            resetQuiet(); diagnose("paired-state"); schedule(); return
        }
        if phase == .focusing {
            guard let secondary, let s, s.ownerFocused, nativeOwner(secondary) else { diagnose("field-focus"); schedule(); return }
            mainBaseline = m; secondaryBaseline = s; dispatchKey(secondary); return
        }
        if target == 3 {
            guard let oldMain, let oldSecondary, let s,
                  (oldMain["documentTimeOriginMillis"] as? Double) != m.documentTimeOriginMillis,
                  (oldSecondary["documentTimeOriginMillis"] as? Double) != s.documentTimeOriginMillis else {
                fail("new-document-not-observed"); return
            }
        }
        let keyAction = target == 2 || target >= 4
        if keyAction {
            guard let secondary, let s, s.ownerFocused, nativeOwner(secondary) else {
                resetQuiet(); diagnose("key-owner"); schedule(); return
            }
        }
        if target == 1 || target == 2 || target >= 5 {
            let state = target == 1 ? m : s, before = target == 1 ? mainBaseline : secondaryBaseline
            guard let state, let before, state.requestCount > before.requestCount, state.responseCount > before.responseCount else {
                resetQuiet(); diagnose("action-round-trip"); schedule(); return
            }
        }
        guard quiet(m, s, polls: phase == .drain || target == 2 || target == 4 ? 3 : 2) else { schedule(); return }
        if target == 4 {
            guard let started = noOpStarted else { fail("noop-timestamp-missing"); return }
            let elapsed = Int((ProcessInfo.processInfo.systemUptime - started) * 1000)
            guard elapsed >= 1000 else { schedule(); return }
            noOpElapsed = elapsed; noOpQuiet = quietCount
        }
        if phase == .drain { finish(m, s); return }
        if target == 0 || target == 3 { mainBaseline = m; secondaryBaseline = s }
        if target == 1 { secondaryBaseline = s }
        let value = payload(m, s)
        observed.append(Self.stages[target]); trace("recv", "nativeEditRecovery.stageObserved", value)
        if target == 2 { prepareExit(value); return }
        if target == 7 { phase = .drain; resetQuiet(); schedule(); return }
        mainBaseline = m; secondaryBaseline = s; target += 1; resetQuiet()
        let pointer = target == 1 ? "open-secondary" : Self.stages[target]
        guard let context = target == 1 ? main : secondary else { fail("action-context"); return }
        click(context, stage: pointer, button: target == 1) {
            self.phase = self.target == 1 ? .waiting : .focusing; self.schedule()
        }
    }
    private func prepareExit(_ settled: [String: Any]) {
        guard let secondary, identityRetained(main), identityRetained(secondary),
              let process = host.sidecarProcessIdentifier, process == initialPID, process == pid, process > 0, failedPID == 0 else {
            fail("failure-injection-owner"); return
        }
        oldMain = settled["main"] as? [String: Any]; oldSecondary = settled["secondary"] as? [String: Any]
        var prepared = settled
        prepared.merge(["complete": true, "settledEditObserved": true, "resultStableAfterDrain": true]) { _, replacement in replacement }
        trace("recv", "nativeEditRecovery.exitPrepared", prepared)
        guard Darwin.kill(process, SIGKILL) == 0 else { fail("sidecar-failure-injection"); return }
        failedPID = process
        trace("send", "acceptance.sidecarFailureInjected", ["reason": "settled-native-edit", "processIdentifier": process])
        phase = .recovering; target = 3; resetQuiet(); schedule()
    }
    private func awaitRecovery() {
        guard identityRetained(main), let secondary, identityRetained(secondary) else { fail("recovery-owner-lost"); return }
        guard main.recoveryGeneration <= 1, secondary.recoveryGeneration <= 1 else { fail("extra-recovery"); return }
        guard let process = host.sidecarProcessIdentifier, process > 0, process != initialPID,
              main.recoveryGeneration == 1, secondary.recoveryGeneration == 1,
              main.navigationFinished, secondary.navigationFinished else { schedule(); return }
        recoveredPID = process; pid = process; generation = 1; phase = .waiting
        // No navigation or marker reset here. Secondary's immutable reload URL
        // is its editor route; main intentionally remains at the framework root.
        resetQuiet(); schedule()
    }
    private func rootMatches(_ state: Snapshot) -> Bool {
        return state.rootRouteAvailable && !state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1
            && state.displayValue.isEmpty && state.fieldValue.isEmpty && state.configStatus.isEmpty && state.valueStatus.isEmpty
            && state.historyStatus.isEmpty && state.windowStatus.isEmpty && state.closeStatus.isEmpty
            && state.pointerStages.isEmpty && state.keyboardStages.isEmpty && state.keyboardKeys.isEmpty
            && state.inputStages.isEmpty && state.inputTypes.isEmpty && state.inputValues.isEmpty && state.domEventOrder.isEmpty
            && state.browserProgrammaticStages.isEmpty && state.nativePointerEventCount == 0 && state.nativePointerTrustedEventCount == 0
            && state.nativeKeyboardEventCount == 0 && state.nativeKeyboardTrustedEventCount == 0
            && state.nativeInputEventCount == 0 && state.nativeInputTrustedEventCount == 0
    }
    private func editMatches(_ state: Snapshot, owner: Bool, changes: Int) -> Bool {
        let localPointers = generation == 0 ? (owner ? pointers.filter { $0 == "old-insert" } : pointers.filter { $0 == "open-secondary" })
            : (owner ? pointers.filter { $0.hasPrefix("fresh-") } : [])
        let localKeys = generation == 0 ? (owner ? keys.filter { $0 == "old-insert" } : [])
            : (owner ? keys.filter { $0.hasPrefix("fresh-") } : [])
        let letter = generation == 0 ? "x" : "y"
        let keyStages = localKeys.filter { $0.hasSuffix("-insert") || state.keyboardStages.contains($0) }
        let keyNames = keyStages.map { $0.hasSuffix("-insert") ? letter : $0.hasSuffix("-undo") ? "z" : "Z" }
        let inputStages = localKeys.filter { $0 != "fresh-empty-undo" }
        var order: [String] = []
        for pointer in localPointers {
            order.append("pointer:" + pointer)
            if keyStages.contains(pointer) { order.append("key:" + pointer) }
            if inputStages.contains(pointer) { order.append("input:" + pointer) }
        }
        let entries = (1...3).prefix(changes).map { Self.valueEntry(letter, change: $0) }
        let display = changes == 0 || changes == 2 ? "" : letter
        return state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1
            && state.configStatus == Self.config && state.displayValue == display && state.fieldValue == display
            && state.valueStatus == (changes == 0 ? Self.initial : "Native edit value: " + Self.valueEntry(letter, change: changes))
            && state.historyStatus == "Native edit history: " + (changes == 0 ? "pending" : entries.joined(separator: " | "))
            && state.fieldAutocorrect == "off" && state.inputAutocorrect == "off"
            && state.fieldAutocapitalize == "none" && state.inputAutocapitalize == "none"
            && !state.invalid && !state.readOnly && state.enabled && state.inputType == "text" && state.label == "Native edit text"
            && state.pointerStages == localPointers && state.nativePointerEventCount == localPointers.count && state.nativePointerTrustedEventCount == localPointers.count
            && state.keyboardStages == keyStages && state.keyboardKeys == keyNames
            && state.nativeKeyboardEventCount == keyStages.count && state.nativeKeyboardTrustedEventCount == keyStages.count
            && inputStages.count == changes && state.inputStages == inputStages
            && state.inputValues == Array([letter, "", letter].prefix(changes)) && state.inputTypes == Array(["insertText", "historyUndo", "historyRedo"].prefix(changes))
            && state.nativeInputEventCount == changes && state.nativeInputTrustedEventCount == changes && state.domEventOrder == order
            && state.browserProgrammaticStages == (owner ? [] : ["navigate"]) + localPointers.map { "position-" + $0 }
    }
    private static func valueEntry(_ letter: String, change: Int) -> String {
        let undo = change == 2
        return "value=\(undo ? "empty" : letter), length=\(undo ? 0 : 1), client=true, change=\(change), oldValue=\(undo ? letter : "empty"), oldLength=\(undo ? 1 : 0)"
    }
    private func nativeOwner(_ context: FlowIpcWindowContext) -> Bool {
        let responder = context.window.firstResponder as? NSView
        return NSApplication.shared.isActive && context.window.isKeyWindow && NSApplication.shared.keyWindow === context.window
            && (responder === context.webView || responder?.isDescendant(of: context.webView) == true)
            && (context === main ? secondary?.window.isKeyWindow != true : !main.window.isKeyWindow)
    }
    private func quiet(_ m: Snapshot, _ s: Snapshot?, polls: Int) -> Bool {
        let signature = "\(phase):\(target):\(m.requestCount):\(m.responseCount):\(m.pushMessageCount):\(s?.requestCount ?? 0):\(s?.responseCount ?? 0):\(s?.pushMessageCount ?? 0)"
        quietCount = signature == quietSignature ? quietCount + 1 : 1; quietSignature = signature
        return quietCount >= polls
    }
    private func resetQuiet() { quietSignature = ""; quietCount = 0 }
    private var frame: [String: Any] {
        var value: [String: Any] = ["hostProcessIdentifier": getpid(), "processIdentifier": pid,
            "initialProcessIdentifier": initialPID, "failedProcessIdentifier": failedPID, "recoveredProcessIdentifier": recoveredPID,
            "sidecarGeneration": generation + 1, "recoveryGeneration": generation,
            "mainContextRetained": host.flowIpcWindowContext("main") === main,
            "mainWindowRetained": ObjectIdentifier(main.window) == mainWindow, "mainWebViewRetained": ObjectIdentifier(main.webView) == mainWebView,
            "mainWindowId": main.scope.windowId, "mainSessionId": main.scope.sessionId, "mainUiId": main.scope.uiId,
            "mainWindowNumber": mainNumber, "mainKeyWindow": main.window.isKeyWindow]
        if let secondary {
            value.merge(["secondaryContextRetained": host.flowIpcWindowContext(Self.ownerId) === secondary,
                "secondaryWindowRetained": ObjectIdentifier(secondary.window) == secondaryWindow,
                "secondaryWebViewRetained": ObjectIdentifier(secondary.webView) == secondaryWebView,
                "secondaryWindowId": secondary.scope.windowId, "secondarySessionId": secondary.scope.sessionId, "secondaryUiId": secondary.scope.uiId,
                "secondaryWindowNumber": secondaryNumber, "secondaryKeyWindow": secondary.window.isKeyWindow]) { _, replacement in replacement }
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
        value["oldMain"] = generation == 1 ? oldMain as Any? ?? NSNull() : NSNull()
        value["oldSecondary"] = generation == 1 ? oldSecondary as Any? ?? NSNull() : NSNull()
        value["stage"] = Self.stages[target]
        value["actionWindowId"] = target == 0 || target == 1 || target == 3 ? "main" : Self.ownerId
        value["balancedDrainObserved"] = m.balanced && (s?.balanced ?? true)
        value["actionUidlRoundTripObserved"] = target == 1 || target == 2 || target >= 5
        value["quietPollCount"] = quietCount; value["noOpElapsedMillis"] = noOpElapsed; value["noOpQuietPollCount"] = noOpQuiet
        return value
    }
    private func finish(_ m: Snapshot, _ s: Snapshot?) {
        guard observed == Self.stages, pointers == Self.pointerStages, keys == Self.keyStages, let secondary, let s,
              let oldMain, let oldSecondary, let oldMainPointers = oldMain["nativePointerTrustedEventCount"] as? Int,
              let oldOwnerPointers = oldSecondary["nativePointerTrustedEventCount"] as? Int,
              let oldInputs = oldSecondary["nativeInputEventCount"] as? Int, let oldTrustedInputs = oldSecondary["nativeInputTrustedEventCount"] as? Int,
              let oldMainPush = oldMain["pushMessageCount"] as? Int, let oldOwnerPush = oldSecondary["pushMessageCount"] as? Int else {
            fail("final-contract"); return
        }
        var value = payload(m, s)
        value.merge(["complete": true, "errorFree": true, "standardBundleMode": true, "flowClientOwned": true,
            "opaqueRequestBridgeObserved": true, "opaqueResponseBridgeObserved": true,
            "pushObserved": oldMainPush + oldOwnerPush + m.pushMessageCount + s.pushMessageCount > 0, "focusedRoutePushClaimed": false,
            "generatedAppKitInput": true, "physicalInputClaimed": false, "imeClaimed": false, "assistiveTechnologyClaimed": false,
            "nativeInputCausalObserved": true, "retainedOwnerObserved": true, "freshUndoNoOpObserved": true, "freshUndoRedoObserved": true,
            "processReplaced": recoveredPID > 0 && recoveredPID != initialPID, "recoveredStateResetObserved": true, "oldHistoryNotReplayedObserved": true,
            "observedStages": observed, "nativePointerStages": pointers, "nativeKeyStages": keys,
            "nativePointerEventCount": pointers.count, "nativePointerTrustedEventCount": oldMainPointers + oldOwnerPointers + m.nativePointerTrustedEventCount + s.nativePointerTrustedEventCount,
            "nativeKeyDispatchCount": keys.count, "nativeInputEventCount": oldInputs + m.nativeInputEventCount + s.nativeInputEventCount,
            "nativeInputTrustedEventCount": oldTrustedInputs + m.nativeInputTrustedEventCount + s.nativeInputTrustedEventCount,
            "drainQuietPollCount": quietCount]) { _, replacement in replacement }
        finished = true; trace("recv", "nativeEditRecovery.frameworkAcceptance", value)
        secondary.window.close(); main.window.close()
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
        let epoch = generation, process = pid
        context.webView.evaluateJavaScript("""
        (() => {
          \(Self.instrumentation)
          const target = \(button ? "document.getElementById('framework-flow-native-edit-open-secondary')" : "input");
          if (!target) return '';
          target.scrollIntoView({block:'center',inline:'nearest'});
          const rect=target.getBoundingClientRect(), x=rect.left+rect.width/2, y=rect.top+rect.height/2;
          let hit=document.elementFromPoint(x,y);
          while (hit?.shadowRoot?.elementFromPoint) { const inner=hit.shadowRoot.elementFromPoint(x,y); if (!inner || inner===hit) break; hit=inner; }
          let node=hit, owned=false; const seen=new Set();
          while (node && !seen.has(node)) { if (node===target) { owned=true; break; } seen.add(node); node=node.assignedSlot || node.parentNode || node.getRootNode?.().host || null; }
          const targetMatched=owned && rect.width>0 && rect.height>0 && Number.isFinite(x) && Number.isFinite(y) && x>=0 && y>=0 && x<innerWidth && y<innerHeight;
          if (targetMatched) { marker.expectedPointer={stage:'\(stage)',target:'\(button ? "open-secondary" : "field")'}; marker.browserProgrammaticStages.push('position-\(stage)'); }
          return JSON.stringify({x,y,targetMatched});
        })()
        """) { result, error in
            guard self.callbackLive(context, epoch: epoch, process: process) else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(Geometry.self, from: data), point.targetMatched else { self.fail("pointer-geometry"); return }
            self.activate(context) {
                guard self.callbackLive(context, epoch: epoch, process: process) else { return }
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
                trace("recv", "nativeEditRecovery.nativePointerDispatch", payload)
                self.pointers.append(stage); context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard self.callbackLive(context, epoch: epoch, process: process) else { return }
                    guard self.nativeOwner(context) else { self.fail("pointer-owner-changed"); return }
                    context.window.sendEvent(up); completion()
                }
            }
        }
    }


    private func dispatchKey(_ context: FlowIpcWindowContext) {
        let stage = Self.stages[target]
        let letter = generation == 0 ? "x" : "y"
        let inserting = stage.hasSuffix("-insert"), redo = stage.hasSuffix("-redo"), noOp = stage == "fresh-empty-undo"
        let key = inserting ? letter : redo ? "Z" : "z"
        let specification: [String: Any] = ["stage": stage, "key": key, "command": !inserting, "shift": redo,
            "value": inserting || redo ? letter : "", "inputType": noOp ? "" : inserting ? "insertText" : redo ? "historyRedo" : "historyUndo"]
        guard let data = try? JSONSerialization.data(withJSONObject: specification), let json = String(data: data, encoding: .utf8) else { fail("key-spec"); return }
        evaluate(context, """
        (() => {
          const marker=window.VaadinDesktopNativeEditRecovery, input=document.getElementById('framework-flow-native-edit-field')?.inputElement;
          let active=document.activeElement; while (active?.shadowRoot?.activeElement) active=active.shadowRoot.activeElement;
          if (!marker || !input || active!==input) return false;
          marker.expectedKey=\(json); marker.keyObserved=false; return true;
        })()
        """) {
            self.activate(context) {
                guard self.live(context) else { return }
                guard self.nativeOwner(context) else { self.fail("key-native-owner"); return }
                var flags: NSEvent.ModifierFlags = []; if !inserting { flags.insert(.command) }; if redo { flags.insert(.shift) }
                let time = ProcessInfo.processInfo.systemUptime, code: UInt16 = !inserting ? 6 : letter == "x" ? 7 : 16
                guard let down = NSEvent.keyEvent(with: .keyDown, location: .zero, modifierFlags: flags, timestamp: time,
                        windowNumber: context.window.windowNumber, context: nil, characters: key, charactersIgnoringModifiers: key, isARepeat: false, keyCode: code),
                      let up = NSEvent.keyEvent(with: .keyUp, location: .zero, modifierFlags: flags, timestamp: time + 0.01,
                        windowNumber: context.window.windowNumber, context: nil, characters: key, charactersIgnoringModifiers: key, isARepeat: false, keyCode: code) else { self.fail("key-event"); return }
                var payload = self.dispatchFrame(context, stage: stage)
                payload.merge(["key": key, "keyCode": Int(code), "modifiers": redo ? "command+shift" : inserting ? "none" : "command",
                    "inputMode": "appkit-application-key-event"]) { _, replacement in replacement }
                trace("recv", "nativeEditRecovery.nativeKeyDispatch", payload)
                self.keys.append(stage)
                if noOp { self.noOpStarted = ProcessInfo.processInfo.systemUptime }
                NSApplication.shared.sendEvent(down); NSApplication.shared.sendEvent(up)
                self.resetQuiet(); self.phase = .waiting; self.schedule()
            }
        }
    }

    private func activate(_ context: FlowIpcWindowContext, completion: @escaping () -> Void) {
        guard live(context) else { return }
        let epoch = generation, process = pid
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline,
            onFailure: { details in
                guard self.callbackLive(context, epoch: epoch, process: process) else { return }; self.fail("activation", details: details)
            }, completion: {
                guard self.callbackLive(context, epoch: epoch, process: process) else { return }; completion()
            })
    }
    private func evaluate(_ context: FlowIpcWindowContext, _ script: String, completion: @escaping () -> Void) {
        guard live(context) else { return }
        let epoch = generation, process = pid
        context.webView.evaluateJavaScript(script) { result, error in
            guard self.callbackLive(context, epoch: epoch, process: process) else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }; completion()
        }
    }
    private func schedule(after delay: Double = 0.2) { DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() } }
    private func diagnose(_ reason: String) {
        let signature = "\(phase):\(target):\(reason):\(NSApplication.shared.isActive):\(main.window.isKeyWindow):\(secondary?.window.isKeyWindow == true)"
        guard signature != diagnosticSignature else { return }; diagnosticSignature = signature
        var value = frame
        value["phase"] = "flow-ipc-native-edit-recovery-" + String(describing: phase); value["reason"] = reason; value["targetIndex"] = target
        value["main"] = latestMain?.payload; value["secondary"] = latestSecondary?.payload
        trace("recv", "nativeEditRecovery.driverDiagnostic", value)
    }
    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }; finished = true
        var value = frame; value.merge(details) { _, replacement in replacement }
        value["code"] = "flow-ipc-native-edit-recovery-" + code; value["phase"] = "flow-ipc-native-edit-recovery-" + String(describing: phase)
        value["targetIndex"] = target; value["main"] = latestMain?.payload; value["secondary"] = latestSecondary?.payload
        trace("send", "error", value)
        secondary?.window.close(); main.window.close()
    }
    private struct Geometry: Decodable { let x: Double; let y: Double; let targetMatched: Bool }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let browserRecoveryGeneration: Int; let documentTimeOriginMillis: Double
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
    const marker=window.VaadinDesktopNativeEditRecovery ||= {browserProgrammaticStages:[]};
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
      const marker=window.VaadinDesktopNativeEditRecovery || {}, d=window.VaadinDesktopIpcDiagnostics || {}, scope=window.VaadinDesktopHostContext || {};
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
        browserRecoveryGeneration:Number(window.VaadinDesktopHostRecoveryGeneration || 0),documentTimeOriginMillis:Number(performance.timeOrigin),
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
