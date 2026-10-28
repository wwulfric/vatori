import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLocalizedNumberAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLocalizedNumberAcceptance(
            host: host, context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)).start()
    }
}

// Test-only generated AppKit input. This does not simulate a physical keyboard,
// an input method, or assistive technology, and never assigns the field's value.
private final class FlowIpcLocalizedNumberAcceptance {
    private enum Phase {
        case ready, route, focusedDE, typingDE, committedDE
        case switchedUS, focusedUS, typingUS, committedUS, drain
    }

    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready
    private var finished = false
    private var processIdentifier: Int32 = 0
    private var keyIndex = 0
    private var keyPending = false
    private var selectionBefore: Snapshot?
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var observedStages: [String] = []
    private var selectedAll: [String] = []
    private var drainCounts: [Int] = []
    private var drainQuietPollCount = 0
    private var lastDiagnostic = ""

    private static let prefix = "framework-flow-localized-number-"
    private static let deText = "1234,567890123456789"
    private static let usText = "9876.125"
    private static let config = "Localized number config: component=BigDecimalField, mode=ON_CHANGE, initialLocale=de-DE, initialPlain=42.50, initialScale=2"
    private static let initialValue = "Localized number value: plain=42.50, scale=2, locale=de-DE, client=false, change=0, oldPlain=none, oldScale=none"
    private static let deValue = "Localized number value: plain=1234.567890123456789, scale=15, locale=de-DE, client=true, change=1, oldPlain=42.50, oldScale=2"
    private static let usValue = "Localized number value: plain=9876.125, scale=3, locale=en-US, client=true, change=2, oldPlain=1234.567890123456789, oldScale=15"
    private static let switched = "Localized number switch: beforeLocale=de-DE, afterLocale=en-US, beforePlain=1234.567890123456789, beforeScale=15, afterPlain=1234.567890123456789, afterScale=15, beforeChanges=1, afterChanges=1, client=true, switches=1, sameValue=true, sameInstance=true"
    private static let pendingSwitch = "Localized number switch: pending"
    private static let pendingHistory = "Localized number history: pending"
    private static var deHistory: String {
        "Localized number history: " + deValue.replacingOccurrences(
            of: "Localized number value: ", with: "")
    }
    private static var usHistory: String {
        deHistory + " | " + usValue.replacingOccurrences(
            of: "Localized number value: ", with: "")
    }

    init(host: WebViewHost, context: FlowIpcWindowContext, deadline: Date) {
        self.host = host
        self.context = context
        self.deadline = deadline
    }

    func start() { poll() }

    private func poll() {
        guard !finished else { return }
        guard Date() < deadline else { fail("deadline"); return }
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String,
                  let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnostic("snapshot-unavailable")
                self.schedule()
                return
            }
            guard state.errorFree else {
                self.fail("browser-runtime", details: state.payload)
                return
            }
            if self.processIdentifier > 0,
               (self.host.sidecarProcessIdentifier != self.processIdentifier
                    || self.context.recoveryGeneration != 0) {
                self.fail("runtime-identity")
                return
            }
            self.advance(state)
        }
    }

    private func advance(_ state: Snapshot) {
        switch phase {
        case .ready:
            guard context.navigationFinished, state.firstMeaningfulPaint,
                  state.rootRouteAvailable, state.flowClientCount > 0 else {
                diagnostic("root-not-ready", state: state); schedule(); return
            }
            evaluate("""
            (() => {
              const trigger = document.getElementById('\(Self.prefix)open-route');
              if (!trigger) return false;
              window.VaadinDesktopLocalizedNumber = { browserProgrammaticStages: ['navigate'] };
              trigger.click();
              return true;
            })()
            """) {
                self.phase = .route
                self.schedule()
            }
        case .route:
            guard state.routeReady, state.displayValue == "42,50",
                  state.fieldValue == "42,50", state.decimalSeparator == ",",
                  state.valueStatus == Self.initialValue,
                  state.switchStatus == Self.pendingSwitch,
                  state.historyStatus == Self.pendingHistory,
                  state.balanced, let pid = host.sidecarProcessIdentifier, pid > 0 else {
                diagnostic("initial-route-not-ready", state: state); schedule(); return
            }
            processIdentifier = pid
            guard inputContract(state, pointers: 0, keyboards: 0, inputs: 0) else {
                fail("initial-input-contract", details: state.payload); return
            }
            recordStage("initial", state: state, roundTrip: false)
            click(stage: "focus-de", target: "field", next: .focusedDE)
        case .focusedDE:
            guard state.ownerFocused, state.pointerStages == ["focus-de"],
                  inputContract(state, pointers: 1, keyboards: 0, inputs: 0) else {
                diagnostic("de-field-focus", state: state); schedule(); return
            }
            baseline(state)
            keyIndex = 0
            phase = .typingDE
            advanceTyping(state, locale: "de")
        case .typingDE:
            advanceTyping(state, locale: "de")
        case .committedDE:
            guard committed(state, locale: "de"), roundTrip(state), state.balanced,
                  inputContract(state, pointers: 1, keyboards: 22, inputs: 20) else {
                diagnostic("de-commit", state: state); schedule(); return
            }
            recordStage("de-committed", state: state, roundTrip: true)
            baseline(state)
            click(stage: "switch-us", target: "switch", next: .switchedUS)
        case .switchedUS:
            guard state.routeReady, state.displayValue == "1234.567890123456789",
                  state.fieldValue == state.displayValue,
                  state.decimalSeparator == ".", state.valueStatus == Self.deValue,
                  state.switchStatus == Self.switched, state.historyStatus == Self.deHistory,
                  state.balanced, roundTrip(state),
                  inputContract(state, pointers: 2, keyboards: 22, inputs: 20) else {
                diagnostic("locale-switch", state: state); schedule(); return
            }
            recordStage("locale-switched", state: state, roundTrip: true)
            click(stage: "focus-us", target: "field", next: .focusedUS)
        case .focusedUS:
            guard state.ownerFocused,
                  state.pointerStages == ["focus-de", "switch-us", "focus-us"],
                  inputContract(state, pointers: 3, keyboards: 22, inputs: 20) else {
                diagnostic("us-field-focus", state: state); schedule(); return
            }
            baseline(state)
            keyIndex = 0
            phase = .typingUS
            advanceTyping(state, locale: "us")
        case .typingUS:
            advanceTyping(state, locale: "us")
        case .committedUS:
            guard committed(state, locale: "us"), roundTrip(state), state.balanced,
                  inputContract(state, pointers: 3, keyboards: 32, inputs: 28) else {
                diagnostic("us-commit", state: state); schedule(); return
            }
            recordStage("us-committed", state: state, roundTrip: true)
            phase = .drain
            schedule()
        case .drain:
            guard committed(state, locale: "us"), state.balanced,
                  inputContract(state, pointers: 3, keyboards: 32, inputs: 28) else {
                drainQuietPollCount = 0
                drainCounts = []
                diagnostic("final-drain", state: state); schedule(); return
            }
            let counts = [state.requestCount, state.responseCount, state.pushMessageCount]
            if drainCounts == counts { drainQuietPollCount += 1 }
            else { drainCounts = counts; drainQuietPollCount = 1 }
            guard drainQuietPollCount >= 3 else { schedule(after: 0.3); return }
            finish(state)
        }
    }

    private func advanceTyping(_ state: Snapshot, locale: String) {
        let keys = Self.keys(locale)
        let priorValue = locale == "de" ? Self.initialValue : Self.deValue
        let priorHistory = locale == "de" ? Self.pendingHistory : Self.deHistory
        guard state.valueStatus == priorValue, state.historyStatus == priorHistory else {
            fail("value-changed-before-native-tab", details: state.payload); return
        }
        if keyPending {
            let key = keys[keyIndex]
            if key.command {
                // AppKit may consume a menu key equivalent without dispatching
                // a DOM keydown. Prove its editing effect, not that routing detail.
                let priorDisplay = locale == "de" ? "42,50" : "1234.567890123456789"
                guard state.selectionStart == 0, state.selectionEnd == priorDisplay.count,
                      state.ownerFocused, state.displayValue == priorDisplay,
                      state.fieldValue == priorDisplay else {
                    diagnostic("await-native-select-all", state: state); schedule(); return
                }
                guard let before = selectionBefore,
                      before.selectionStart == before.selectionEnd,
                      before.selectionStart >= 0, before.selectionEnd <= priorDisplay.count,
                      before.ownerFocused, before.displayValue == priorDisplay,
                      before.fieldValue == priorDisplay,
                      inputContract(state, pointers: locale == "de" ? 1 : 3,
                        keyboards: locale == "de" ? 1 : 23, inputs: locale == "de" ? 0 : 20) else {
                    fail("select-all-causal-contract", details: state.payload); return
                }
                selectedAll.append(locale)
                var payload = state.payload
                payload.merge(frame) { _, replacement in replacement }
                payload["stage"] = key.stage
                payload["domKeyDownObserved"] = state.keyboardStages.contains(key.stage)
                payload["beforeSelectionStart"] = before.selectionStart
                payload["beforeSelectionEnd"] = before.selectionEnd
                payload["selectAllObserved"] = selectedAll
                trace("recv", "localizedNumber.selectionObserved", payload)
                selectionBefore = nil
            } else {
                guard state.keyboardStages.last == key.stage else {
                    diagnostic("await-key-\(key.stage)", state: state); schedule(); return
                }
            }
            if let expected = key.expectedInput {
                guard state.inputStages.last == key.stage,
                      state.inputValues.last == expected,
                      state.displayValue == expected else {
                    diagnostic("await-input-\(key.stage)", state: state); schedule(); return
                }
            }
            keyPending = false
            keyIndex += 1
        }
        guard keyIndex < keys.count else { fail("keyboard-index"); return }
        let key = keys[keyIndex]
        guard state.ownerFocused else {
            fail("keyboard-owner-focus-lost", details: state.payload); return
        }
        if key.command { selectionBefore = state }
        dispatchKey(key) {
            if key.name == "Tab" {
                self.phase = locale == "de" ? .committedDE : .committedUS
            } else {
                self.keyPending = true
            }
            self.schedule(after: 0.08)
        }
    }

    private func committed(_ state: Snapshot, locale: String) -> Bool {
        let de = locale == "de"
        return state.routeReady && !state.ownerFocused
            && state.displayValue == (de ? Self.deText : Self.usText)
            && state.fieldValue == state.displayValue
            && state.decimalSeparator == (de ? "," : ".")
            && state.valueStatus == (de ? Self.deValue : Self.usValue)
            && state.switchStatus == (de ? Self.pendingSwitch : Self.switched)
            && state.historyStatus == (de ? Self.deHistory : Self.usHistory)
    }

    private func inputContract(_ state: Snapshot, pointers: Int, keyboards: Int, inputs: Int) -> Bool {
        let expectedKeys = (Self.keys("de") + Self.keys("us")).prefix(keyboards)
            .filter { !$0.command || state.keyboardStages.contains($0.stage) }
        let expectedInputs = expectedKeys.filter { $0.expectedInput != nil }
        return state.nativePointerEventCount == pointers
            && state.nativePointerTrustedEventCount == pointers
            && state.nativeKeyboardEventCount == expectedKeys.count
            && state.nativeKeyboardTrustedEventCount == expectedKeys.count
            && state.nativeInputEventCount == inputs
            && state.nativeInputTrustedEventCount == inputs
            && state.pointerStages == Array(["focus-de", "switch-us", "focus-us"].prefix(pointers))
            && state.keyboardStages == expectedKeys.map(\.stage)
            && state.keyboardKeys == expectedKeys.map(\.name)
            && state.inputStages == expectedInputs.map(\.stage)
            && state.inputValues == expectedInputs.compactMap(\.expectedInput)
            && state.domEventOrder == Self.eventOrder(pointers: pointers,
                keyboards: keyboards, observedKeyStages: state.keyboardStages)
    }

    private func baseline(_ state: Snapshot) {
        requestBaseline = state.requestCount
        responseBaseline = state.responseCount
    }

    private func roundTrip(_ state: Snapshot) -> Bool {
        state.requestCount > requestBaseline && state.responseCount > responseBaseline
    }

    private func recordStage(_ stage: String, state: Snapshot, roundTrip: Bool) {
        observedStages.append(stage)
        var payload = state.payload
        payload.merge(frame) { _, replacement in replacement }
        payload["stage"] = stage
        payload["requestBaseline"] = roundTrip ? requestBaseline : state.requestCount
        payload["responseBaseline"] = roundTrip ? responseBaseline : state.responseCount
        payload["uidlRoundTripObserved"] = roundTrip
        payload["selectAllObserved"] = selectedAll
        trace("recv", "localizedNumber.stageObserved", payload)
    }

    private var frame: [String: Any] {
        context.scope.applying(to: [
            "hostProcessIdentifier": getpid(), "processIdentifier": processIdentifier,
            "recoveryGeneration": context.recoveryGeneration])
    }

    private func finish(_ state: Snapshot) {
        let complete = observedStages == ["initial", "de-committed", "locale-switched", "us-committed"]
            && selectedAll == ["de", "us"] && state.errorFree
            && state.browserProgrammaticStages == ["navigate", "position-focus-de", "position-switch-us", "position-focus-us"]
            && state.pushMessageCount >= 0 && inputContract(state, pointers: 3, keyboards: 32, inputs: 28)
        guard complete else { fail("final-contract", details: state.payload); return }
        var payload = state.payload
        payload.merge(frame) { _, replacement in replacement }
        payload.merge([
            "complete": true, "errorFree": true, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.routeReady, "flowClientOwned": state.flowClientCount > 0,
            "opaqueRequestBridgeObserved": state.requestCount > 0,
            "opaqueResponseBridgeObserved": state.responseCount > 0, "pushObserved": false,
            "initialObserved": true, "deCommittedObserved": true, "localeSwitchedObserved": true,
            "usCommittedObserved": true, "localeSwitchPreservedValueObserved": true,
            "noExtraValueChangeObserved": true, "nativeInputCausalObserved": true,
            "generatedAppKitInput": true, "physicalInputClaimed": false,
            "imeClaimed": false, "assistiveTechnologyClaimed": false,
            "selectAllObserved": selectedAll, "drainQuietPollCount": drainQuietPollCount
        ]) { _, replacement in replacement }
        finished = true
        trace("recv", "localizedNumber.frameworkAcceptance", payload)
        context.window.close()
    }

    private func click(stage: String, target: String, next: Phase) {
        let script = """
        (() => {
          \(Self.instrumentation)
          const element = document.getElementById('\(Self.prefix)\(target)');
          const target = '\(target)' === 'field' ? element?.inputElement : element;
          if (!target) return '';
          target.scrollIntoView({block:'center', inline:'nearest'});
          const rect = target.getBoundingClientRect();
          const x = rect.left + rect.width / 2, y = rect.top + rect.height / 2;
          let hit = document.elementFromPoint(x, y);
          while (hit?.shadowRoot?.elementFromPoint) {
            const inner = hit.shadowRoot.elementFromPoint(x, y);
            if (!inner || inner === hit) break;
            hit = inner;
          }
          const composedOwner = node => {
            const visited = new Set();
            while (node && !visited.has(node)) {
              if (node === target) return true;
              visited.add(node);
              node = node.assignedSlot || node.parentNode || node.getRootNode?.().host || null;
            }
            return false;
          };
          const targetMatched = !!hit && composedOwner(hit)
            && rect.width > 0 && rect.height > 0 && Number.isFinite(x) && Number.isFinite(y)
            && x >= 0 && y >= 0 && x < innerWidth && y < innerHeight;
          if (targetMatched) {
            marker.expectedPointer = '\(stage)';
            marker.browserProgrammaticStages.push('position-\(stage)');
          }
          return JSON.stringify({x,y,targetMatched,width:rect.width,height:rect.height,
            hitTag:String(hit?.localName || ''),hitId:String(hit?.id || ''),
            targetTag:String(target.localName || ''),targetId:String(target.id || '')});
        })()
        """
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, let json = result as? String,
                  let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: data) else {
                self.fail("pointer-geometry-\(stage)", details: [
                    "message": error?.localizedDescription ?? "",
                    "geometryResult": result as? String ?? ""]); return
            }
            guard geometry.targetMatched else {
                self.fail("pointer-hit-ownership-\(stage)", details: [
                    "webViewX": geometry.x, "webViewY": geometry.y,
                    "targetWidth": geometry.width, "targetHeight": geometry.height,
                    "hitTag": geometry.hitTag, "hitId": geometry.hitId,
                    "targetTag": geometry.targetTag, "targetId": geometry.targetId]); return
            }
            self.activate {
                let point = NSPoint(x: geometry.x, y: self.context.webView.isFlipped
                    ? geometry.y : self.context.webView.bounds.height - geometry.y)
                let location = self.context.webView.convert(point, to: nil)
                guard self.context.window.contentLayoutRect.contains(location) else {
                    self.fail("pointer-outside-window"); return
                }
                let timestamp = ProcessInfo.processInfo.systemUptime
                guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location,
                        modifierFlags: [], timestamp: timestamp,
                        windowNumber: self.context.window.windowNumber, context: nil,
                        eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location,
                        modifierFlags: [], timestamp: timestamp + 0.05,
                        windowNumber: self.context.window.windowNumber, context: nil,
                        eventNumber: 1, clickCount: 1, pressure: 0) else {
                    self.fail("pointer-event-creation"); return
                }
                var payload = self.inputFrame
                payload["stage"] = stage
                payload["inputMode"] = "appkit-window-pointer-event"
                payload["button"] = "primary"
                trace("recv", "localizedNumber.nativePointerDispatch", payload)
                self.context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    self.context.window.sendEvent(up)
                    self.phase = next
                    self.schedule(after: 0.15)
                }
            }
        }
    }

    private func dispatchKey(_ key: Key, completion: @escaping () -> Void) {
        let spec: [String: Any] = ["stage": key.stage, "key": key.name,
            "command": key.command, "input": key.expectedInput as Any? ?? NSNull()]
        guard let encoded = try? JSONSerialization.data(withJSONObject: spec),
              let json = String(data: encoded, encoding: .utf8) else {
            fail("key-instrumentation-encoding"); return
        }
        evaluate("""
        (() => {
          const marker = window.VaadinDesktopLocalizedNumber;
          const field = document.getElementById('\(Self.prefix)field');
          const input = field?.inputElement;
          let active = document.activeElement;
          while (active?.shadowRoot?.activeElement) active = active.shadowRoot.activeElement;
          if (!marker || !input || active !== input) return false;
          marker.expectedKey = \(json);
          marker.keyObserved = false;
          return true;
        })()
        """) {
            self.activate {
                let timestamp = ProcessInfo.processInfo.systemUptime
                guard let down = key.event(.keyDown, window: self.context.window.windowNumber, timestamp: timestamp),
                      let up = key.event(.keyUp, window: self.context.window.windowNumber, timestamp: timestamp + 0.01) else {
                    self.fail("keyboard-event-creation"); return
                }
                var payload = self.inputFrame
                payload.merge(["stage": key.stage, "key": key.name, "keyCode": Int(key.code),
                    "modifiers": key.command ? "command" : "none",
                    "inputMode": "appkit-application-key-event"]) { _, replacement in replacement }
                trace("recv", "localizedNumber.nativeKeyDispatch", payload)
                // Application dispatch runs AppKit's key-equivalent path for
                // Command-modified editing keys before ordinary window input.
                NSApplication.shared.sendEvent(down)
                NSApplication.shared.sendEvent(up)
                completion()
            }
        }
    }

    private var inputFrame: [String: Any] {
        var payload = frame
        payload["activeApplication"] = NSApplication.shared.isActive
        payload["keyWindow"] = context.window.isKeyWindow
        payload["windowNumber"] = context.window.windowNumber
        return payload
    }

    private func activate(_ completion: @escaping () -> Void) {
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline,
            onFailure: { self.fail("activation", details: $0) }, completion: completion)
    }

    private func evaluate(_ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.fail("preparation-script"); return
            }
            completion()
        }
    }

    private func schedule(after delay: Double = 0.2) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() }
    }

    private func diagnostic(_ reason: String, state: Snapshot? = nil) {
        let signature = "\(phase):\(keyIndex):\(reason):\(NSApplication.shared.isActive):\(context.window.isKeyWindow)"
        guard signature != lastDiagnostic else { return }
        lastDiagnostic = signature
        var payload = inputFrame
        payload["phase"] = String(describing: phase)
        payload["reason"] = reason
        payload["keyIndex"] = keyIndex
        if let state {
            payload["routeReady"] = state.routeReady
            payload["ownerFocused"] = state.ownerFocused
            payload["balanced"] = state.balanced
            payload["requestCount"] = state.requestCount
            payload["responseCount"] = state.responseCount
            payload["displayValue"] = state.displayValue
            payload["fieldValue"] = state.fieldValue
            payload["selectionStart"] = state.selectionStart
            payload["selectionEnd"] = state.selectionEnd
            payload["lastKeyboardStage"] = state.keyboardStages.last ?? ""
            payload["lastInputStage"] = state.inputStages.last ?? ""
            payload["markerError"] = state.markerError
            payload["valueStatus"] = state.valueStatus
        }
        trace("recv", "localizedNumber.driverDiagnostic", payload)
    }

    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }
        finished = true
        var payload = details
        payload["code"] = "localizedNumber-" + code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Geometry: Decodable {
        let x: Double
        let y: Double
        let targetMatched: Bool
        let width: Double
        let height: Double
        let hitTag: String
        let hitId: String
        let targetTag: String
        let targetId: String
    }

    private struct Key {
        let stage: String
        let name: String
        let code: UInt16
        let command: Bool
        let expectedInput: String?

        func event(_ type: NSEvent.EventType, window: Int, timestamp: TimeInterval) -> NSEvent? {
            NSEvent.keyEvent(with: type, location: .zero,
                modifierFlags: command ? [.command] : [], timestamp: timestamp,
                windowNumber: window, context: nil,
                characters: name == "Tab" ? "\t" : name,
                charactersIgnoringModifiers: name == "Tab" ? "\t" : name,
                isARepeat: false, keyCode: code)
        }
    }

    private static func keys(_ locale: String) -> [Key] {
        let text = locale == "de" ? deText : usText
        let codes: [Character: UInt16] = ["0": 29, "1": 18, "2": 19, "3": 20,
            "4": 21, "5": 23, "6": 22, "7": 26, "8": 28, "9": 25, ",": 43, ".": 47]
        var result = [Key(stage: "\(locale)-select-all", name: "a", code: 0,
            command: true, expectedInput: nil)]
        var prefix = ""
        for (index, character) in text.enumerated() {
            prefix.append(character)
            result.append(Key(stage: String(format: "%@-type-%02d", locale, index + 1),
                name: String(character), code: codes[character]!, command: false,
                expectedInput: prefix))
        }
        result.append(Key(stage: "\(locale)-commit", name: "Tab", code: 48,
            command: false, expectedInput: nil))
        return result
    }

    private static func eventOrder(
        pointers: Int, keyboards: Int, observedKeyStages: [String]
    ) -> [String] {
        var events: [String] = []
        if pointers >= 1 { events.append("pointer:focus-de") }
        for key in keys("de").prefix(min(keyboards, 22)) {
            if key.command && !observedKeyStages.contains(key.stage) { continue }
            events.append("key:" + key.stage)
            if key.expectedInput != nil { events.append("input:" + key.stage) }
        }
        if pointers >= 2 { events.append("pointer:switch-us") }
        if pointers >= 3 { events.append("pointer:focus-us") }
        if keyboards > 22 {
            for key in keys("us").prefix(keyboards - 22) {
                if key.command && !observedKeyStages.contains(key.stage) { continue }
                events.append("key:" + key.stage)
                if key.expectedInput != nil { events.append("input:" + key.stage) }
            }
        }
        return events
    }

    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool
        let routeReady: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let displayValue: String
        let fieldValue: String
        let decimalSeparator: String
        let configStatus: String
        let valueStatus: String
        let switchStatus: String
        let historyStatus: String
        let invalid: Bool
        let readOnly: Bool
        let enabled: Bool
        let inputMode: String
        let inputType: String
        let label: String
        let ownerFocused: Bool
        let selectionStart: Int
        let selectionEnd: Int
        let nativePointerEventCount: Int
        let nativePointerTrustedEventCount: Int
        let nativeKeyboardEventCount: Int
        let nativeKeyboardTrustedEventCount: Int
        let nativeInputEventCount: Int
        let nativeInputTrustedEventCount: Int
        let pointerStages: [String]
        let keyboardStages: [String]
        let keyboardKeys: [String]
        let inputStages: [String]
        let inputValues: [String]
        let domEventOrder: [String]
        let browserProgrammaticStages: [String]
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String
        let markerError: String

        var errorFree: Bool {
            browserError.isEmpty && requestError.isEmpty && responseError.isEmpty
                && pushError.isEmpty && markerError.isEmpty
        }
        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self),
                  let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return [:]
            }
            return object
        }
    }

    private static let instrumentation = """
    const marker = window.VaadinDesktopLocalizedNumber;
    const field = document.getElementById('framework-flow-localized-number-field');
    const input = field?.inputElement;
    if (!marker || !input) return '';
    if (!marker.installed) {
      marker.installed = true;
      for (const name of ['pointerStages','keyboardStages','keyboardKeys','inputStages','inputValues','domEventOrder']) marker[name] = [];
      marker.nativePointerTrustedEventCount = 0;
      marker.nativeKeyboardTrustedEventCount = 0;
      marker.nativeInputTrustedEventCount = 0;
      document.addEventListener('click', event => {
        const path = event.composedPath();
        const stage = marker.expectedPointer;
        if (!stage) return;
        const target = document.getElementById('framework-flow-localized-number-' + (stage === 'switch-us' ? 'switch' : 'field'));
        if (!path.includes(target) || event.button !== 0) return;
        marker.pointerStages.push(stage);
        marker.nativePointerTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.domEventOrder.push('pointer:' + stage);
        marker.expectedPointer = null;
      }, true);
      document.addEventListener('keydown', event => {
        const path = event.composedPath();
        if (!path.includes(field) || !path.includes(input)) return;
        const expected = marker.expectedKey;
        if (!expected || event.key !== expected.key || event.metaKey !== expected.command
            || event.ctrlKey || event.altKey || event.shiftKey || marker.keyObserved) {
          marker.markerError = 'unexpected-owned-keydown'; return;
        }
        marker.keyObserved = true;
        marker.keyboardStages.push(expected.stage);
        marker.keyboardKeys.push(event.key);
        marker.nativeKeyboardTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.domEventOrder.push('key:' + expected.stage);
      }, true);
      document.addEventListener('input', event => {
        const path = event.composedPath();
        if (!path.includes(field) || !path.includes(input)) return;
        const expected = marker.expectedKey;
        if (!expected || expected.input === null || !marker.keyObserved
            || input.value !== expected.input || marker.inputStages.includes(expected.stage)) {
          marker.markerError = 'unexpected-owned-input'; return;
        }
        marker.inputStages.push(expected.stage);
        marker.inputValues.push(input.value);
        marker.nativeInputTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.domEventOrder.push('input:' + expected.stage);
      }, true);
    }
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const marker = window.VaadinDesktopLocalizedNumber || {};
      const field = document.getElementById('framework-flow-localized-number-field');
      const input = field?.inputElement;
      const text = suffix => String(document.getElementById('framework-flow-localized-number-' + suffix)?.textContent || '').replace(/\\s+/g,' ').trim();
      const clients = Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript');
      let active = document.activeElement;
      while (active?.shadowRoot?.activeElement) active = active.shadowRoot.activeElement;
      const array = name => Array.isArray(marker[name]) ? marker[name] : [];
      return JSON.stringify({
        rootRouteAvailable: !!document.getElementById('framework-flow-localized-number-open-route'),
        routeReady: !!document.getElementById('framework-flow-localized-number-route') && !!input
          && text('config-status') === '\(config)' && !field.disabled && !field.readonly && !field.invalid
          && input.inputMode === 'decimal' && input.type === 'text' && field.label === 'Localized decimal amount',
        firstMeaningfulPaint: diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        displayValue: String(input?.value || ''), fieldValue: String(field?.value || ''),
        decimalSeparator: String(field?._decimalSeparator || ''),
        configStatus: text('config-status'), valueStatus: text('value-status'),
        switchStatus: text('switch-status'), historyStatus: text('history-status'),
        invalid: field?.invalid === true, readOnly: field?.readonly === true,
        enabled: !!field && field.disabled !== true,
        inputMode: String(input?.inputMode || ''), inputType: String(input?.type || ''),
        label: String(field?.label || ''), ownerFocused: !!input && active === input,
        selectionStart: Number(input?.selectionStart ?? -1), selectionEnd: Number(input?.selectionEnd ?? -1),
        nativePointerEventCount: array('pointerStages').length,
        nativePointerTrustedEventCount: Number(marker.nativePointerTrustedEventCount || 0),
        nativeKeyboardEventCount: array('keyboardStages').length,
        nativeKeyboardTrustedEventCount: Number(marker.nativeKeyboardTrustedEventCount || 0),
        nativeInputEventCount: array('inputStages').length,
        nativeInputTrustedEventCount: Number(marker.nativeInputTrustedEventCount || 0),
        pointerStages: array('pointerStages'), keyboardStages: array('keyboardStages'), keyboardKeys: array('keyboardKeys'),
        inputStages: array('inputStages'), inputValues: array('inputValues'), domEventOrder: array('domEventOrder'),
        browserProgrammaticStages: array('browserProgrammaticStages'),
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || ''), markerError: String(marker.markerError || '')
      });
    })()
    """
}
