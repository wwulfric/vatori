import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleNativeEditIsolationAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        guard context.scope == .main else { return }
        FlowIpcNativeEditIsolationAcceptance(host: host, main: context, timeoutSeconds: timeoutSeconds).start()
    }
}

// One coordinator owns both windows. Activating a window is not an edit receipt:
// each key requires trusted DOM input, its own Java history and scoped UIDL.
private final class FlowIpcNativeEditIsolationAcceptance {
    private enum Phase { case root, route, opening, focusing, editing, returning, drain }
    private static let ownerId = "native-edit-secondary"
    private static let prefix = "framework-flow-native-edit-"
    private static let config = "Native edit config: component=TextField, mode=EAGER, initial=empty, initialLength=0"
    private static let initial = "Native edit value: value=empty, length=0, client=false, change=0, oldValue=none, oldLength=-1"
    private static let opened = "Native edit window: opened=true, client=true, count=1, error=none"
    private static let actions = ["main-insert", "secondary-insert", "main-undo", "secondary-undo", "main-redo", "secondary-redo"]
    private static let pairs = ["initial"] + actions + ["returned-main"]
    private let host: WebViewHost
    private let main: FlowIpcWindowContext
    private var secondary: FlowIpcWindowContext?
    private let mainWindow: ObjectIdentifier
    private let mainWebView: ObjectIdentifier
    private var secondaryWindow: ObjectIdentifier?
    private var secondaryWebView: ObjectIdentifier?
    private var secondaryScope: FlowIpcWindowScope?
    private let timeout: Double
    private let deadline: Date
    private var pid: Int32 = 0
    private var finished = false
    private var phase = Phase.root
    private var action = 0
    private var mainBaseline: Snapshot?
    private var secondaryBaseline: Snapshot?
    private var latestMain: Snapshot?
    private var latestSecondary: Snapshot?
    private var observed: [String] = []
    private var pointers: [String] = []
    private var keys: [String] = []
    private var quietSignature = ""
    private var quietCount = 0
    private var diagnosticSignature = ""

    init(host: WebViewHost, main: FlowIpcWindowContext, timeoutSeconds: Double) {
        self.host = host; self.main = main
        mainWindow = ObjectIdentifier(main.window); mainWebView = ObjectIdentifier(main.webView)
        timeout = timeoutSeconds; deadline = Date().addingTimeInterval(timeoutSeconds)
    }

    func start() {
        guard timeout.isFinite, timeout > 0, timeout <= 300 else { fail("invalid-timeout"); return }
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) {
            guard !self.finished else { return }; self.fail("deadline")
        }
        poll()
    }

    private func retained(_ context: FlowIpcWindowContext, isMain: Bool) -> Bool {
        host.flowIpcWindowContext(context.scope.windowId) === context
            && ObjectIdentifier(context.window) == (isMain ? mainWindow : secondaryWindow)
            && ObjectIdentifier(context.webView) == (isMain ? mainWebView : secondaryWebView)
            && context.scope == (isMain ? .main : secondaryScope)
            && context.recoveryGeneration == 0 && host.sidecarProcessIdentifier == pid
    }

    private func inspect(_ context: FlowIpcWindowContext, completion: @escaping (Snapshot) -> Void) {
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnose("snapshot-unavailable"); self.schedule(); return
            }
            guard state.errorFree, state.browserWindowId == context.scope.windowId,
                  state.browserSessionId == context.scope.sessionId, state.browserUiId == context.scope.uiId else {
                // Preserve the rejected read, not the preceding successful pair.
                // Focus transfer may expose a marker or browser error in either UI.
                self.fail("snapshot-scope-or-error", details: [
                    "inspectedContext": context.scope.applying(to: [
                        "windowNumber": context.window.windowNumber,
                        "keyWindow": context.window.isKeyWindow,
                        "recoveryGeneration": context.recoveryGeneration]),
                    "rejectedSnapshot": state.payload,
                    "snapshotErrorFree": state.errorFree,
                    "snapshotWindowMatches": state.browserWindowId == context.scope.windowId,
                    "snapshotSessionMatches": state.browserSessionId == context.scope.sessionId,
                    "snapshotUiMatches": state.browserUiId == context.scope.uiId]); return
            }
            completion(state)
        }
    }

    private func poll() {
        guard !finished else { return }
        guard Date() < deadline else { fail("deadline"); return }
        if pid > 0 && !retained(main, isMain: true) { fail("main-identity"); return }
        inspect(main) { state in
            self.latestMain = state
            if self.phase == .root {
                guard self.main.navigationFinished, state.rootRouteAvailable, state.firstMeaningfulPaint,
                      state.flowClientCount == 1, let pid = self.host.sidecarProcessIdentifier, pid > 0 else {
                    self.diagnose("root-ready"); self.schedule(); return
                }
                self.pid = pid
                self.evaluate(self.main, """
                (() => {
                  const button=document.getElementById('\(Self.prefix)open-route');
                  if (!button) return false;
                  window.VaadinDesktopNativeEditIsolation={browserProgrammaticStages:['navigate']};
                  button.click(); return true;
                })()
                """) { self.phase = .route; self.schedule() }
                return
            }
            if self.phase == .route {
                guard self.stateMatches(state, isMain: true, changes: 0, pointers: []), state.balanced,
                      state.windowStatus == "Native edit window: pending" else {
                    self.diagnose("main-route-ready"); self.schedule(); return
                }
                self.click(self.main, stage: "open-secondary", button: true) { self.phase = .opening; self.schedule() }
                return
            }
            guard let owner = self.host.flowIpcWindowContext(Self.ownerId), owner.navigationFinished else {
                if self.phase == .opening { self.schedule() } else { self.fail("secondary-missing") }
                return
            }
            if self.secondary == nil {
                let sessionPrefix = "flow-ipc-window:"
                guard owner.scope.windowId == Self.ownerId, owner.scope.uiId == Self.ownerId,
                      owner.scope.sessionId.hasPrefix(sessionPrefix),
                      UUID(uuidString: String(owner.scope.sessionId.dropFirst(sessionPrefix.count))) != nil,
                      owner.window !== self.main.window, owner.webView !== self.main.webView,
                      owner.window.windowNumber > 0, owner.window.windowNumber != self.main.window.windowNumber else {
                    self.fail("secondary-initial-identity"); return
                }
                self.secondary = owner; self.secondaryScope = owner.scope
                self.secondaryWindow = ObjectIdentifier(owner.window); self.secondaryWebView = ObjectIdentifier(owner.webView)
            }
            guard self.secondary === owner, self.retained(owner, isMain: false) else { self.fail("secondary-identity"); return }
            self.inspect(owner) { other in
                self.latestSecondary = other
                self.advance(state, other)
            }
        }
    }

    private func advance(_ m: Snapshot, _ s: Snapshot) {
        guard let secondary, retained(main, isMain: true), retained(secondary, isMain: false) else { fail("paired-identity"); return }
        let completed = phase == .opening ? 0 : phase == .focusing ? action : phase == .editing ? action + 1 : 6
        let mainChanges = (completed + 1) / 2, secondaryChanges = completed / 2
        let mainPointers = pointers.filter { $0 == "open-secondary" || $0.hasPrefix("main-") || $0 == "returned-main" }
        let secondaryPointers = pointers.filter { $0.hasPrefix("secondary-") }
        guard stateMatches(m, isMain: true, changes: mainChanges, pointers: mainPointers),
              stateMatches(s, isMain: false, changes: secondaryChanges, pointers: secondaryPointers),
              m.windowStatus == Self.opened, s.windowStatus == "Native edit window: pending",
              m.balanced, s.balanced else {
            resetQuiet(); diagnose("paired-state-" + String(completed)); schedule(); return
        }
        switch phase {
        case .opening:
            guard quiet(m, s, polls: 2) else { schedule(); return }
            mainBaseline = m; secondaryBaseline = s
            record("initial", main: m, secondary: s, roundTrip: false)
            focusNext()
        case .focusing:
            let owner = action % 2 == 0 ? main : secondary
            let state = action % 2 == 0 ? m : s
            guard state.ownerFocused, nativeOwner(owner) else { resetQuiet(); diagnose("focus-owner"); schedule(); return }
            mainBaseline = m; secondaryBaseline = s
            dispatchKey(owner)
        case .editing:
            let owner = action % 2 == 0 ? main : secondary
            let state = action % 2 == 0 ? m : s, before = action % 2 == 0 ? mainBaseline : secondaryBaseline
            guard let before, state.ownerFocused, nativeOwner(owner), state.requestCount > before.requestCount,
                  state.responseCount > before.responseCount, quiet(m, s, polls: 2) else { diagnose("key-round-trip"); schedule(); return }
            record(Self.actions[action], main: m, secondary: s, roundTrip: true)
            action += 1
            if action < 6 { focusNext() }
            else {
                mainBaseline = m; secondaryBaseline = s
                click(main, stage: "returned-main", button: false) { self.phase = .returning; self.schedule() }
            }
        case .returning:
            guard m.ownerFocused, nativeOwner(main), quiet(m, s, polls: 2) else { diagnose("final-return"); schedule(); return }
            record("returned-main", main: m, secondary: s, roundTrip: false)
            resetQuiet(); phase = .drain; schedule()
        case .drain:
            guard m.ownerFocused, nativeOwner(main), quiet(m, s, polls: 3) else { schedule(after: 0.3); return }
            finish(m, s)
        default: fail("phase")
        }
    }

    private func stateMatches(_ state: Snapshot, isMain: Bool, changes: Int, pointers: [String]) -> Bool {
        let letter = isMain ? "x" : "y", owner = isMain ? "main" : "secondary"
        let localActions = [owner + "-insert", owner + "-undo", owner + "-redo"]
        let completed = Array(localActions.prefix(changes))
        let keyStages = completed.filter { $0.hasSuffix("-insert") || state.keyboardStages.contains($0) }
        let keyNames = keyStages.map { $0.hasSuffix("-insert") ? letter : $0.hasSuffix("-undo") ? "z" : "Z" }
        let values = [letter, "", letter], types = ["insertText", "historyUndo", "historyRedo"]
        var order: [String] = []
        for pointer in pointers {
            order.append("pointer:" + pointer)
            if completed.contains(pointer) {
                if keyStages.contains(pointer) { order.append("key:" + pointer) }
                order.append("input:" + pointer)
            }
        }
        let display = changes == 0 || changes == 2 ? "" : letter
        let entries = (1...3).prefix(changes).map { Self.valueEntry(letter, change: $0) }
        let value = changes == 0 ? Self.initial : "Native edit value: " + Self.valueEntry(letter, change: changes)
        return state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1
            && state.configStatus == Self.config && state.displayValue == display && state.fieldValue == display
            && state.fieldAutocorrect == "off" && state.inputAutocorrect == "off"
            && state.fieldAutocapitalize == "none" && state.inputAutocapitalize == "none"
            && state.valueStatus == value && state.historyStatus == "Native edit history: " + (changes == 0 ? "pending" : entries.joined(separator: " | "))
            && state.pointerStages == pointers && state.nativePointerEventCount == pointers.count && state.nativePointerTrustedEventCount == pointers.count
            && state.keyboardStages == keyStages && state.keyboardKeys == keyNames
            && state.nativeKeyboardEventCount == keyStages.count && state.nativeKeyboardTrustedEventCount == keyStages.count
            && state.inputStages == completed && state.inputValues == Array(values.prefix(changes)) && state.inputTypes == Array(types.prefix(changes))
            && state.nativeInputEventCount == changes && state.nativeInputTrustedEventCount == changes && state.domEventOrder == order
    }

    private static func valueEntry(_ letter: String, change: Int) -> String {
        let undo = change == 2
        return "value=\(undo ? "empty" : letter), length=\(undo ? 0 : 1), client=true, change=\(change), oldValue=\(undo ? letter : "empty"), oldLength=\(undo ? 1 : 0)"
    }

    private func focusNext() {
        guard let secondary else { fail("focus-secondary-missing"); return }
        resetQuiet()
        click(action % 2 == 0 ? main : secondary, stage: Self.actions[action], button: false) {
            self.phase = .focusing; self.schedule()
        }
    }

    private func nativeOwner(_ context: FlowIpcWindowContext) -> Bool {
        let responder = context.window.firstResponder as? NSView
        let owned = responder === context.webView || responder?.isDescendant(of: context.webView) == true
        return NSApplication.shared.isActive && context.window.isKeyWindow && NSApplication.shared.keyWindow === context.window && owned
            && (context === main ? secondary?.window.isKeyWindow != true : !main.window.isKeyWindow)
    }

    private func quiet(_ main: Snapshot, _ secondary: Snapshot, polls: Int) -> Bool {
        let signature = "\(phase):\(action):\(main.requestCount):\(main.responseCount):\(main.pushMessageCount):\(secondary.requestCount):\(secondary.responseCount):\(secondary.pushMessageCount)"
        quietCount = signature == quietSignature ? quietCount + 1 : 1; quietSignature = signature
        return quietCount >= polls
    }
    private func resetQuiet() { quietSignature = ""; quietCount = 0 }

    private var frame: [String: Any] {
        var value: [String: Any] = ["hostProcessIdentifier": getpid(), "processIdentifier": pid, "recoveryGeneration": 0,
            "mainContextRetained": host.flowIpcWindowContext("main") === main,
            "mainWindowRetained": ObjectIdentifier(main.window) == mainWindow,
            "mainWebViewRetained": ObjectIdentifier(main.webView) == mainWebView,
            "mainWindowId": main.scope.windowId, "mainSessionId": main.scope.sessionId, "mainUiId": main.scope.uiId,
            "mainWindowNumber": main.window.windowNumber, "mainKeyWindow": main.window.isKeyWindow]
        if let secondary {
            value.merge(["secondaryContextRetained": host.flowIpcWindowContext(Self.ownerId) === secondary,
                "secondaryWindowRetained": ObjectIdentifier(secondary.window) == secondaryWindow,
                "secondaryWebViewRetained": ObjectIdentifier(secondary.webView) == secondaryWebView,
                "secondaryWindowId": secondary.scope.windowId, "secondarySessionId": secondary.scope.sessionId,
                "secondaryUiId": secondary.scope.uiId, "secondaryWindowNumber": secondary.window.windowNumber,
                "secondaryKeyWindow": secondary.window.isKeyWindow]) { _, replacement in replacement }
        }
        return value
    }

    private func pairPayload(_ m: Snapshot, _ s: Snapshot, stage: String, roundTrip: Bool) -> [String: Any] {
        var payload = frame, mainValue = m.payload, secondaryValue = s.payload
        mainValue["requestBaseline"] = mainBaseline?.requestCount ?? m.requestCount
        mainValue["responseBaseline"] = mainBaseline?.responseCount ?? m.responseCount
        secondaryValue["requestBaseline"] = secondaryBaseline?.requestCount ?? s.requestCount
        secondaryValue["responseBaseline"] = secondaryBaseline?.responseCount ?? s.responseCount
        payload["main"] = mainValue; payload["secondary"] = secondaryValue
        payload["stage"] = stage; payload["actionWindowId"] = stage.hasPrefix("secondary-") ? Self.ownerId : "main"
        payload["balancedDrainObserved"] = m.balanced && s.balanced
        payload["peerStateUnchanged"] = true; payload["actionUidlRoundTripObserved"] = roundTrip
        payload["quietPollCount"] = quietCount
        return payload
    }
    private func record(_ stage: String, main: Snapshot, secondary: Snapshot, roundTrip: Bool) {
        observed.append(stage)
        trace("recv", "nativeEditIsolation.pairObserved", pairPayload(main, secondary, stage: stage, roundTrip: roundTrip))
    }
    private func finish(_ m: Snapshot, _ s: Snapshot) {
        guard observed == Self.pairs, pointers == ["open-secondary"] + Self.actions + ["returned-main"], keys == Self.actions,
              m.browserProgrammaticStages == ["navigate", "position-open-secondary", "position-main-insert", "position-main-undo", "position-main-redo", "position-returned-main"],
              s.browserProgrammaticStages == ["position-secondary-insert", "position-secondary-undo", "position-secondary-redo"],
              let secondary else { fail("final-contract"); return }
        var payload = pairPayload(m, s, stage: "returned-main", roundTrip: false)
        let proof: [String: Any] = ["complete": true, "errorFree": true, "standardBundleMode": true, "flowClientOwned": true,
            "opaqueRequestBridgeObserved": true, "opaqueResponseBridgeObserved": true, "pushObserved": false,
            "generatedAppKitInput": true, "physicalInputClaimed": false, "imeClaimed": false, "assistiveTechnologyClaimed": false,
            "ownerIsolationObserved": true, "undoRedoIndependentObserved": true, "returnedMainUnchangedObserved": true,
            "nativeInputCausalObserved": true, "observedStages": observed, "nativePointerStages": pointers,
            "nativeKeyStages": keys, "nativePointerEventCount": pointers.count,
            "nativePointerTrustedEventCount": m.nativePointerTrustedEventCount + s.nativePointerTrustedEventCount,
            "nativeKeyDispatchCount": keys.count, "nativeInputEventCount": m.nativeInputEventCount + s.nativeInputEventCount,
            "nativeInputTrustedEventCount": m.nativeInputTrustedEventCount + s.nativeInputTrustedEventCount, "drainQuietPollCount": quietCount]
        payload.merge(proof) { _, replacement in replacement }
        finished = true
        trace("recv", "nativeEditIsolation.frameworkAcceptance", payload)
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
          if (targetMatched) { marker.expectedPointer={stage:'\(stage)',button:\(button)}; marker.browserProgrammaticStages.push('position-\(stage)'); }
          return JSON.stringify({x,y,targetMatched});
        })()
        """) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(Geometry.self, from: data), point.targetMatched else { self.fail("pointer-geometry"); return }
            self.activate(context) {
                guard !self.finished else { return }
                let p = NSPoint(x: point.x, y: context.webView.isFlipped ? point.y : context.webView.bounds.height - point.y)
                let location = context.webView.convert(p, to: nil), time = ProcessInfo.processInfo.systemUptime
                guard context.window.contentLayoutRect.contains(location),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [], timestamp: time,
                        windowNumber: context.window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [], timestamp: time + 0.05,
                        windowNumber: context.window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else { self.fail("pointer-event"); return }
                var payload = self.dispatchFrame(context, stage: stage)
                payload["button"] = "primary"; payload["inputMode"] = "appkit-window-pointer-event"
                trace("recv", "nativeEditIsolation.nativePointerDispatch", payload)
                self.pointers.append(stage); context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard !self.finished else { return }; context.window.sendEvent(up); completion()
                }
            }
        }
    }

    private func dispatchKey(_ context: FlowIpcWindowContext) {
        let stage = Self.actions[action], operation = action / 2, letter = action % 2 == 0 ? "x" : "y"
        let key = operation == 0 ? letter : operation == 1 ? "z" : "Z"
        let value = operation == 1 ? "" : letter, type = ["insertText", "historyUndo", "historyRedo"][operation]
        let command = operation > 0, shift = operation == 2
        let specification: [String: Any] = ["stage": stage, "key": key, "command": command, "shift": shift, "value": value, "inputType": type]
        guard let data = try? JSONSerialization.data(withJSONObject: specification), let json = String(data: data, encoding: .utf8) else { fail("key-spec"); return }
        evaluate(context, """
        (() => {
          const marker=window.VaadinDesktopNativeEditIsolation, input=document.getElementById('framework-flow-native-edit-field')?.inputElement;
          let active=document.activeElement; while (active?.shadowRoot?.activeElement) active=active.shadowRoot.activeElement;
          if (!marker || !input || active!==input) return false;
          marker.expectedKey=\(json); marker.keyObserved=false; return true;
        })()
        """) {
            self.activate(context) {
                guard !self.finished, self.nativeOwner(context) else { self.fail("key-native-owner"); return }
                var flags: NSEvent.ModifierFlags = []; if command { flags.insert(.command) }; if shift { flags.insert(.shift) }
                let time = ProcessInfo.processInfo.systemUptime, code: UInt16 = command ? 6 : letter == "x" ? 7 : 16
                guard let down = NSEvent.keyEvent(with: .keyDown, location: .zero, modifierFlags: flags, timestamp: time,
                        windowNumber: context.window.windowNumber, context: nil, characters: key, charactersIgnoringModifiers: key, isARepeat: false, keyCode: code),
                      let up = NSEvent.keyEvent(with: .keyUp, location: .zero, modifierFlags: flags, timestamp: time + 0.01,
                        windowNumber: context.window.windowNumber, context: nil, characters: key, charactersIgnoringModifiers: key, isARepeat: false, keyCode: code) else { self.fail("key-event"); return }
                var payload = self.dispatchFrame(context, stage: stage)
                payload.merge(["key": key, "keyCode": Int(code), "modifiers": shift ? "command+shift" : command ? "command" : "none",
                    "inputMode": "appkit-application-key-event"]) { _, replacement in replacement }
                trace("recv", "nativeEditIsolation.nativeKeyDispatch", payload)
                self.keys.append(stage); NSApplication.shared.sendEvent(down); NSApplication.shared.sendEvent(up)
                self.resetQuiet(); self.phase = .editing; self.schedule()
            }
        }
    }

    private func activate(_ context: FlowIpcWindowContext, completion: @escaping () -> Void) {
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline,
            onFailure: { self.fail("activation", details: $0) }, completion: completion)
    }
    private func evaluate(_ context: FlowIpcWindowContext, _ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard !self.finished else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }; completion()
        }
    }
    private func schedule(after delay: Double = 0.2) { DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() } }
    private func diagnose(_ reason: String) {
        let signature = "\(phase):\(action):\(reason):\(NSApplication.shared.isActive):\(main.window.isKeyWindow):\(secondary?.window.isKeyWindow == true)"
        guard signature != diagnosticSignature else { return }; diagnosticSignature = signature
        var payload = frame
        payload["phase"] = "flow-ipc-native-edit-isolation-" + String(describing: phase); payload["reason"] = reason; payload["actionIndex"] = action
        payload["main"] = latestMain?.payload; payload["secondary"] = latestSecondary?.payload
        trace("recv", "nativeEditIsolation.driverDiagnostic", payload)
    }
    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }; finished = true
        var payload = frame; payload.merge(details) { _, replacement in replacement }
        payload["code"] = "flow-ipc-native-edit-isolation-" + code
        payload["phase"] = "flow-ipc-native-edit-isolation-" + String(describing: phase)
        trace("send", "error", payload)
        secondary?.window.close(); main.window.close()
    }
    private struct Geometry: Decodable { let x: Double; let y: Double; let targetMatched: Bool }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let displayValue: String; let fieldValue: String; let configStatus: String; let valueStatus: String; let historyStatus: String; let windowStatus: String
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
    const marker=window.VaadinDesktopNativeEditIsolation ||= {browserProgrammaticStages:[]};
    const field=document.getElementById('framework-flow-native-edit-field'), input=field?.inputElement;
    if (!input) return '';
    if (!marker.installed) {
      marker.installed=true;
      for (const name of ['pointerStages','keyboardStages','keyboardKeys','inputStages','inputTypes','inputValues','domEventOrder']) marker[name]=[];
      marker.nativePointerTrustedEventCount=0; marker.nativeKeyboardTrustedEventCount=0; marker.nativeInputTrustedEventCount=0;
      document.addEventListener('click',event=>{
        const path=event.composedPath(), expected=marker.expectedPointer;
        const open=document.getElementById('framework-flow-native-edit-open-secondary');
        if (!path.includes(input) && !path.includes(open)) return;
        if (!expected || event.button!==0 || !path.includes(expected.button?open:input)) {
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
        if (!expected || (!expected.command && !marker.keyObserved) || event.isComposing || event.inputType!==expected.inputType
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
      const marker=window.VaadinDesktopNativeEditIsolation || {}, d=window.VaadinDesktopIpcDiagnostics || {}, scope=window.VaadinDesktopHostContext || {};
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
        displayValue:String(input?.value || ''),fieldValue:String(field?.value || ''),configStatus:text('config-status'),valueStatus:text('value-status'),historyStatus:text('history-status'),windowStatus:text('window-status'),
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
