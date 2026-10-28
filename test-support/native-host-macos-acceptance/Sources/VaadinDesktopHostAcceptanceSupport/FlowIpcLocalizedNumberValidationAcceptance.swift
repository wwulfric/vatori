import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLocalizedNumberValidationAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        guard context.scope == .main else { return }
        FlowIpcLocalizedNumberValidationAcceptance(host: host, context: context, timeout: timeoutSeconds).start()
    }
}

// Read-only browser instrumentation surrounds native input. Model conversion,
// validation and last-valid bean state belong entirely to the ordinary Binder.
private final class FlowIpcLocalizedNumberValidationAcceptance {
    private enum Phase { case root, initial, focus, typing, committed, drain }
    private static let prefix = "framework-flow-localized-number-validation-"
    private static let config = "Localized number validation config: component=BigDecimalField, mode=ON_CHANGE, locale=de-DE, required=true, defaultValidators=true, validationStatusListeners=true, maximum=100, initialPlain=42.50, initialScale=2"
    private static let edits = ["empty", "malformed", "recovered", "custom", "final"]
    private static let texts = ["", "1,,2", "12,50", "150,00", "7,125"]
    private static let stages = ["initial", "required-empty", "malformed", "recovered", "custom-rejected", "final-recovered"]
    private static let plains = ["42.50", "none", "none", "12.50", "150.00", "7.125"]
    private static let scales = ["2", "none", "none", "2", "2", "3"]
    private static let beans = ["42.50", "42.50", "42.50", "12.50", "12.50", "7.125"]
    private static let beanScales = ["2", "2", "2", "2", "2", "3"]
    private static let errors = ["none", "Amount is required", "Amount is malformed", "none", "Amount must be at most 100", "none"]
    private static let valueChanges = [0, 1, 1, 2, 3, 4]
    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let windowIdentity: ObjectIdentifier
    private let webViewIdentity: ObjectIdentifier
    private let timeout: Double
    private let deadline: Date
    private var phase = Phase.root
    private var edit = 0
    private var keyIndex = 0
    private var keyPending = false
    private var finished = false
    private var pid: Int32 = 0
    private var latest: Snapshot?
    private var settled: Snapshot?
    private var selectionBefore: Snapshot?
    private var selectedAll: [String] = []
    private var dispatchedKeys: [Key] = []
    private var dispatchedPointers: [String] = []
    private var observed: [String] = []
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var quietSignature = ""
    private var quietCount = 0
    private var diagnosticSignature = ""

    init(host: WebViewHost, context: FlowIpcWindowContext, timeout: Double) {
        self.host = host; self.context = context; self.timeout = timeout
        deadline = Date().addingTimeInterval(timeout)
        windowIdentity = ObjectIdentifier(context.window); webViewIdentity = ObjectIdentifier(context.webView)
    }
    func start() {
        guard timeout.isFinite, timeout > 0, timeout <= 300 else { fail("invalid-timeout"); return }
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { guard !self.finished else { return }; self.fail("deadline") }
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
        guard pid == 0 || retained else { fail("runtime-identity"); return }
        context.webView.evaluateJavaScript(Self.inspection) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else { self.diagnose("snapshot-unavailable"); self.schedule(); return }
            self.latest = state
            guard state.errorFree, state.browserWindowId == self.context.scope.windowId,
                  state.browserSessionId == self.context.scope.sessionId, state.browserUiId == self.context.scope.uiId else {
                self.fail("snapshot-scope-or-error", details: ["rejectedSnapshot": state.payload]); return
            }
            self.advance(state)
        }
    }
    private func advance(_ state: Snapshot) {
        switch phase {
        case .root:
            guard context.navigationFinished, state.rootRouteAvailable, state.firstMeaningfulPaint,
                  state.flowClientCount == 1, let actualPid = host.sidecarProcessIdentifier, actualPid > 0 else { diagnose("root-ready"); schedule(); return }
            pid = actualPid
            evaluate("""
            (()=>{const button=document.getElementById('\(Self.prefix)open-route');if(!button)return false;
              window.VaadinDesktopLocalizedNumberValidation={browserProgrammaticStages:['navigate']};button.click();return true;})()
            """) { self.phase = .initial; self.schedule() }
        case .initial:
            guard committed(state, stage: 0), inputContract(state), quiet(state, polls: 2) else { diagnose("initial-state"); schedule(); return }
            requestBaseline = state.requestCount; responseBaseline = state.responseCount
            record(0, state); settled = state; clickField()
        case .focus:
            guard state.routeReady, state.ownerFocused, nativeOwner, inputContract(state), sameJava(state, settled) else { diagnose("field-focus"); schedule(); return }
            keyIndex = 0; keyPending = false; phase = .typing; advanceTyping(state)
        case .typing: advanceTyping(state)
        case .committed:
            let stage = edit + 1
            guard committed(state, stage: stage), !state.ownerFocused, inputContract(state),
                  state.requestCount > requestBaseline, state.responseCount > responseBaseline,
                  validationAdvanced(state, stage: stage), quiet(state, polls: 2) else { diagnose("commit-state"); schedule(); return }
            record(stage, state); settled = state
            if edit < 4 { edit += 1; clickField() }
            else { resetQuiet(); phase = .drain; schedule() }
        case .drain:
            guard committed(state, stage: 5), !state.ownerFocused, inputContract(state), sameJava(state, settled), quiet(state, polls: 3) else { diagnose("final-drain"); schedule(); return }
            finish(state)
        }
    }
    private func committed(_ state: Snapshot, stage: Int) -> Bool {
        let display = stage == 0 ? "42,50" : Self.texts[stage - 1]
        let invalid = Self.errors[stage] != "none"
        let rawError = state.errorMessage.isEmpty ? "none" : state.errorMessage
        guard let binderEvents = Self.history(state.binderHistory, prefix: "Localized number validation binder history: ", key: "errors"),
              let componentEvents = Self.history(state.validationHistory, prefix: "Localized number validation component history: ", key: "valid") else { return false }
        let binder = "Localized number validation binder: errors=\(invalid), beanPlain=\(Self.beans[stage]), beanScale=\(Self.beanScales[stage]), fieldPlain=\(Self.plains[stage]), fieldScale=\(Self.scales[stage]), invalid=\(invalid), error=\(Self.errors[stage]), rawError=\(rawError), change=\(binderEvents.count)"
        let component = "Localized number validation component: valid=\(componentEvents.last ?? true), change=\(componentEvents.count)"
        let expectedHistory = Self.valueHistory(stage)
        return retained && state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1 && state.balanced
            && state.configStatus == Self.config && state.displayValue == display && state.fieldValue == display && state.decimalSeparator == ","
            && state.valueStatus == Self.valueStatus(stage) && state.valueHistory == expectedHistory
            && state.binderStatus == binder && state.validationStatus == component
            && (stage == 0 ? binderEvents.isEmpty && componentEvents.isEmpty : binderEvents.last == invalid)
            && state.invalid == invalid && state.enabled && !state.readOnly
            && (invalid ? state.inputAriaInvalid == "true" && state.errorMessage == Self.errors[stage]
                && state.renderedErrorText == Self.errors[stage] && state.errorMessageVisible
                : ["", "false"].contains(state.inputAriaInvalid) && !state.errorMessageVisible)
    }
    private func validationAdvanced(_ state: Snapshot, stage: Int) -> Bool {
        guard let before = settled,
              let priorBinder = Self.history(before.binderHistory, prefix: "Localized number validation binder history: ", key: "errors"),
              let binder = Self.history(state.binderHistory, prefix: "Localized number validation binder history: ", key: "errors"),
              let priorComponent = Self.history(before.validationHistory, prefix: "Localized number validation component history: ", key: "valid"),
              let component = Self.history(state.validationHistory, prefix: "Localized number validation component history: ", key: "valid"),
              binder.count > priorBinder.count, Array(binder.prefix(priorBinder.count)) == priorBinder,
              component.count >= priorComponent.count, Array(component.prefix(priorComponent.count)) == priorComponent else { return false }
        if stage == 2 {
            return component.count > priorComponent.count && component.dropFirst(priorComponent.count).allSatisfy { !$0 }
                && state.valueStatus == before.valueStatus && state.valueHistory == before.valueHistory
        }
        return true
    }
    private static func history(_ value: String, prefix: String, key: String) -> [Bool]? {
        guard value.hasPrefix(prefix) else { return nil }
        let body = String(value.dropFirst(prefix.count)); if body == "pending" { return [] }
        var result: [Bool] = []
        for (index, entry) in body.components(separatedBy: " | ").enumerated() {
            if entry == "\(key)=true, change=\(index + 1)" { result.append(true) }
            else if entry == "\(key)=false, change=\(index + 1)" { result.append(false) }
            else { return nil }
        }
        return result
    }
    private static func valueEntry(_ stage: Int) -> String {
        let old: Int = stage == 0 ? -1 : stage == 2 ? 0 : stage - 1
        return "plain=\(plains[stage]), scale=\(scales[stage]), locale=de-DE, client=\(stage != 0), change=\(valueChanges[stage]), oldPlain=\(old < 0 ? "none" : plains[old]), oldScale=\(old < 0 ? "none" : scales[old])"
    }
    private static func valueStatus(_ stage: Int) -> String { "Localized number validation value: " + valueEntry(stage == 2 ? 1 : stage) }
    private static func valueHistory(_ stage: Int) -> String {
        let entries = [1, 3, 4, 5].filter { $0 <= stage }.map(valueEntry)
        return "Localized number validation value history: " + (entries.isEmpty ? "pending" : entries.joined(separator: " | "))
    }
    private func sameJava(_ state: Snapshot, _ prior: Snapshot?) -> Bool {
        guard let prior else { return false }
        return state.valueStatus == prior.valueStatus && state.valueHistory == prior.valueHistory
            && state.validationStatus == prior.validationStatus && state.validationHistory == prior.validationHistory
            && state.binderStatus == prior.binderStatus && state.binderHistory == prior.binderHistory
    }
    private func advanceTyping(_ state: Snapshot) {
        guard sameJava(state, settled) else { fail("java-state-before-commit", details: state.payload); return }
        let keys = Self.keys(edit)
        if keyPending {
            let key = keys[keyIndex]
            if key.command {
                let oldDisplay = edit == 0 ? "42,50" : Self.texts[edit - 1]
                guard state.ownerFocused, state.displayValue == oldDisplay, state.fieldValue == oldDisplay,
                      state.selectionStart == 0, state.selectionEnd == oldDisplay.count else { diagnose("full-selection-effect"); schedule(); return }
                guard let before = selectionBefore, before.ownerFocused, before.displayValue == oldDisplay, before.fieldValue == oldDisplay,
                      before.selectionStart == before.selectionEnd, before.selectionStart >= 0, before.selectionEnd <= oldDisplay.count,
                      inputContract(state) else { fail("full-selection-causality", details: state.payload); return }
                selectedAll.append(Self.edits[edit])
                var payload = snapshotPayload(state)
                payload["stage"] = key.stage; payload["domKeyDownObserved"] = state.keyboardStages.contains(key.stage)
                payload["beforeSelectionStart"] = before.selectionStart; payload["beforeSelectionEnd"] = before.selectionEnd
                payload["selectAllObserved"] = selectedAll
                trace("recv", "localizedNumberValidation.selectionObserved", payload); selectionBefore = nil
            } else {
                guard state.keyboardStages.last == key.stage else { diagnose("key-receipt"); schedule(); return }
            }
            if let value = key.input {
                guard state.inputStages.last == key.stage, state.inputValues.last == value, state.inputTypes.last == key.inputType,
                      state.displayValue == value, state.fieldValue == value else { diagnose("input-receipt"); schedule(); return }
            }
            guard inputContract(state) else { diagnose("input-prefix"); schedule(); return }
            keyPending = false; keyIndex += 1
        }
        guard keyIndex < keys.count, state.ownerFocused, nativeOwner else { fail("typing-owner-or-index", details: state.payload); return }
        let key = keys[keyIndex]
        if key.command { selectionBefore = state }
        if key.name == "Tab" { requestBaseline = state.requestCount; responseBaseline = state.responseCount }
        dispatch(key) {
            if key.name == "Tab" { self.resetQuiet(); self.phase = .committed }
            else { self.keyPending = true }
            self.schedule(after: 0.08)
        }
    }
    private func inputContract(_ state: Snapshot) -> Bool {
        let keys = dispatchedKeys.filter { !$0.command || state.keyboardStages.contains($0.stage) }
        let inputs = dispatchedKeys.filter { $0.input != nil }
        var order: [String] = []
        for (index, pointer) in dispatchedPointers.enumerated() {
            order.append("pointer:" + pointer)
            for key in keys.filter({ $0.stage.hasPrefix(Self.edits[index] + "-") }) {
                order.append("key:" + key.stage); if key.input != nil { order.append("input:" + key.stage) }
            }
        }
        return state.pointerStages == dispatchedPointers && state.nativePointerEventCount == dispatchedPointers.count && state.nativePointerTrustedEventCount == dispatchedPointers.count
            && state.keyboardStages == keys.map(\.stage) && state.keyboardKeys == keys.map(\.name)
            && state.nativeKeyboardEventCount == keys.count && state.nativeKeyboardTrustedEventCount == keys.count
            && state.inputStages == inputs.map(\.stage) && state.inputValues == inputs.compactMap(\.input) && state.inputTypes == inputs.compactMap(\.inputType)
            && state.nativeInputEventCount == inputs.count && state.nativeInputTrustedEventCount == inputs.count && state.domEventOrder == order
    }
    private var nativeOwner: Bool {
        let responder = context.window.firstResponder as? NSView
        return NSApplication.shared.isActive && context.window.isKeyWindow && NSApplication.shared.keyWindow === context.window
            && (responder === context.webView || responder?.isDescendant(of: context.webView) == true)
    }
    private func quiet(_ state: Snapshot, polls: Int) -> Bool {
        let signature = "\(phase):\(edit):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount):\(state.binderStatus):\(state.validationStatus)"
        quietCount = quietSignature == signature ? quietCount + 1 : 1; quietSignature = signature
        return quietCount >= polls
    }
    private func resetQuiet() { quietSignature = ""; quietCount = 0 }
    private func snapshotPayload(_ state: Snapshot) -> [String: Any] { var result = frame; result.merge(state.payload) { _, replacement in replacement }; return result }
    private func record(_ stage: Int, _ state: Snapshot) {
        observed.append(Self.stages[stage]); var payload = snapshotPayload(state)
        payload["stage"] = Self.stages[stage]; payload["requestBaseline"] = requestBaseline; payload["responseBaseline"] = responseBaseline
        payload["uidlRoundTripObserved"] = stage > 0; payload["quietPollCount"] = quietCount; payload["selectAllObserved"] = selectedAll
        trace("recv", "localizedNumberValidation.stageObserved", payload)
    }
    private func finish(_ state: Snapshot) {
        guard observed == Self.stages, dispatchedPointers == Self.edits.map({ "focus-" + $0 }), dispatchedKeys.count == 30,
              state.nativeInputEventCount == 21, selectedAll == ["empty", "recovered", "custom", "final"],
              state.browserProgrammaticStages == ["navigate"] + Self.edits.map({ "position-focus-" + $0 }) else { fail("final-contract"); return }
        var payload = snapshotPayload(state)
        payload.merge(["stage": "final-recovered", "complete": true, "errorFree": true, "standardBundleMode": true, "rootRendered": true, "flowClientOwned": true,
            "opaqueRequestBridgeObserved": true, "opaqueResponseBridgeObserved": true, "pushObserved": false, "generatedAppKitInput": true,
            "physicalInputClaimed": false, "imeClaimed": false, "assistiveTechnologyClaimed": false, "nativeInputCausalObserved": true,
            "requiredValidationObserved": true, "defaultValidationObserved": true, "validationOnlyTransitionObserved": true,
            "customValidationObserved": true, "lastValidBeanRetainedObserved": true, "recoveryObserved": true,
            "observedStages": observed, "selectAllObserved": selectedAll, "nativeKeyDispatchCount": dispatchedKeys.count,
            "drainQuietPollCount": quietCount]) { _, replacement in replacement }
        finished = true; trace("recv", "localizedNumberValidation.frameworkAcceptance", payload); context.window.close()
    }
    private func clickField() {
        resetQuiet(); let stage = "focus-" + Self.edits[edit]
        context.webView.evaluateJavaScript("""
        (()=>{\(Self.instrumentation)
          input.scrollIntoView({block:'center',inline:'nearest'});const r=input.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2;
          let hit=document.elementFromPoint(x,y);while(hit?.shadowRoot?.elementFromPoint){const inner=hit.shadowRoot.elementFromPoint(x,y);if(!inner || inner===hit)break;hit=inner;}
          let node=hit,owned=false;const visited=new Set();while(node && !visited.has(node)){if(node===input){owned=true;break;}visited.add(node);node=node.assignedSlot || node.parentNode || node.getRootNode?.().host || null;}
          if(!owned || r.width<=0 || r.height<=0 || x<0 || y<0 || x>=innerWidth || y>=innerHeight)return '';
          marker.expectedPointer='\(stage)';marker.browserProgrammaticStages.push('position-\(stage)');return JSON.stringify({x,y});})()
        """) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8), let point = try? JSONDecoder().decode(Geometry.self, from: data) else { self.fail("pointer-geometry"); return }
            self.activate {
                guard !self.finished, self.retained else { return }
                let p = NSPoint(x: point.x, y: self.context.webView.isFlipped ? point.y : self.context.webView.bounds.height - point.y)
                let location = self.context.webView.convert(p, to: nil), time = ProcessInfo.processInfo.systemUptime
                guard self.context.window.contentLayoutRect.contains(location),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [], timestamp: time, windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [], timestamp: time + 0.05, windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else { self.fail("pointer-event"); return }
                var payload = self.inputFrame; payload.merge(["stage": stage, "inputMode": "appkit-window-pointer-event", "button": "primary", "eventCount": 2]) { _, replacement in replacement }
                trace("recv", "localizedNumberValidation.nativePointerDispatch", payload); self.dispatchedPointers.append(stage); self.context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { guard !self.finished else { return }; self.context.window.sendEvent(up); self.phase = .focus; self.schedule() }
            }
        }
    }
    private var inputFrame: [String: Any] {
        var payload = frame; payload["activeApplication"] = NSApplication.shared.isActive; payload["keyWindow"] = context.window.isKeyWindow; payload["nativeResponderOwned"] = nativeOwner; return payload
    }
    private func dispatch(_ key: Key, completion: @escaping () -> Void) {
        let spec: [String: Any] = ["stage": key.stage, "key": key.name, "command": key.command,
            "input": key.input as Any? ?? NSNull(), "inputType": key.inputType as Any? ?? NSNull()]
        guard let data = try? JSONSerialization.data(withJSONObject: spec), let json = String(data: data, encoding: .utf8) else { fail("key-spec"); return }
        evaluate("""
        (()=>{const marker=window.VaadinDesktopLocalizedNumberValidation,input=document.getElementById('\(Self.prefix)field')?.inputElement;
          let active=document.activeElement;while(active?.shadowRoot?.activeElement)active=active.shadowRoot.activeElement;
          if(!marker || !input || active!==input)return false;marker.expectedKey=\(json);marker.keyObserved=false;return true;})()
        """) {
            self.activate {
                guard !self.finished, self.nativeOwner else { self.fail("key-owner"); return }
                let time = ProcessInfo.processInfo.systemUptime
                guard let down = key.event(.keyDown, window: self.context.window.windowNumber, time: time),
                      let up = key.event(.keyUp, window: self.context.window.windowNumber, time: time + 0.01) else { self.fail("key-event"); return }
                var payload = self.inputFrame
                payload.merge(["stage": key.stage, "key": key.name, "keyCode": Int(key.code), "modifiers": key.command ? "command" : "none", "inputMode": "appkit-application-key-event", "eventCount": 2]) { _, replacement in replacement }
                trace("recv", "localizedNumberValidation.nativeKeyDispatch", payload); self.dispatchedKeys.append(key)
                NSApplication.shared.sendEvent(down); NSApplication.shared.sendEvent(up); completion()
            }
        }
    }
    private func activate(_ completion: @escaping () -> Void) {
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline, onFailure: { self.fail("activation", details: $0) }, completion: completion)
    }
    private func evaluate(_ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard !self.finished else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }; completion()
        }
    }
    private func schedule(after delay: Double = 0.2) { DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() } }
    private func diagnose(_ reason: String) {
        let signature = "\(phase):\(edit):\(keyIndex):\(reason):\(latest?.displayValue ?? "")"
        guard signature != diagnosticSignature else { return }; diagnosticSignature = signature
        var payload = frame; payload["reason"] = reason; payload["phase"] = "flow-ipc-localized-number-validation-" + String(describing: phase)
        payload["editIndex"] = edit; payload["keyIndex"] = keyIndex; payload["snapshot"] = latest?.payload
        trace("recv", "localizedNumberValidation.driverDiagnostic", payload)
    }
    private func fail(_ code: String, details: [String: Any] = [:]) {
        guard !finished else { return }; finished = true
        var payload = frame; payload.merge(details) { _, replacement in replacement }
        payload["code"] = "flow-ipc-localized-number-validation-" + code; payload["phase"] = "flow-ipc-localized-number-validation-" + String(describing: phase)
        payload["snapshot"] = latest?.payload; trace("send", "error", payload); context.window.close()
    }
    private struct Geometry: Decodable { let x: Double; let y: Double }
    private struct Key {
        let stage: String; let name: String; let code: UInt16; let command: Bool; let input: String?; let inputType: String?
        func event(_ type: NSEvent.EventType, window: Int, time: TimeInterval) -> NSEvent? {
            let characters = name == "Tab" ? "\t" : name == "Backspace" ? "\u{7f}" : name
            return NSEvent.keyEvent(with: type, location: .zero, modifierFlags: command ? [.command] : [], timestamp: time,
                windowNumber: window, context: nil, characters: characters, charactersIgnoringModifiers: characters, isARepeat: false, keyCode: code)
        }
    }
    private static func keys(_ edit: Int) -> [Key] {
        let stage = edits[edit]; var keys: [Key] = []
        if edit != 1 { keys.append(Key(stage: stage + "-select-all", name: "a", code: 0, command: true, input: nil, inputType: nil)) }
        if edit == 0 { keys.append(Key(stage: stage + "-delete", name: "Backspace", code: 51, command: false, input: "", inputType: "deleteContentBackward")) }
        else {
            let codes: [Character: UInt16] = ["0": 29, "1": 18, "2": 19, "3": 20, "4": 21, "5": 23, "6": 22, "7": 26, "8": 28, "9": 25, ",": 43]
            var prefix = ""
            for (index, character) in texts[edit].enumerated() {
                prefix.append(character); keys.append(Key(stage: String(format: "%@-type-%02d", stage, index + 1), name: String(character), code: codes[character]!, command: false, input: prefix, inputType: "insertText"))
            }
        }
        keys.append(Key(stage: stage + "-commit", name: "Tab", code: 48, command: false, input: nil, inputType: nil)); return keys
    }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let displayValue: String; let fieldValue: String; let decimalSeparator: String
        let configStatus: String; let valueStatus: String; let valueHistory: String; let validationStatus: String; let validationHistory: String; let binderStatus: String; let binderHistory: String
        let invalid: Bool; let readOnly: Bool; let enabled: Bool; let inputMode: String; let inputType: String; let label: String; let ownerFocused: Bool
        let selectionStart: Int; let selectionEnd: Int; let inputAriaInvalid: String; let errorMessage: String; let renderedErrorText: String; let errorMessageVisible: Bool
        let fieldAutocorrect: String; let inputAutocorrect: String; let fieldAutocapitalize: String; let inputAutocapitalize: String
        let nativePointerEventCount: Int; let nativePointerTrustedEventCount: Int; let nativeKeyboardEventCount: Int; let nativeKeyboardTrustedEventCount: Int; let nativeInputEventCount: Int; let nativeInputTrustedEventCount: Int
        let pointerStages: [String]; let keyboardStages: [String]; let keyboardKeys: [String]; let inputStages: [String]; let inputValues: [String]; let inputTypes: [String]; let domEventOrder: [String]; let browserProgrammaticStages: [String]
        let browserError: String; let requestError: String; let responseError: String; let pushError: String; let markerError: String
        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var errorFree: Bool { browserError.isEmpty && requestError.isEmpty && responseError.isEmpty && pushError.isEmpty && markerError.isEmpty }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self), let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }; return value
        }
    }
    private static let instrumentation = """
    const marker=window.VaadinDesktopLocalizedNumberValidation,field=document.getElementById('framework-flow-localized-number-validation-field'),input=field?.inputElement;
    if(!marker || !input)return '';
    if(!marker.installed){marker.installed=true;for(const name of ['pointerStages','keyboardStages','keyboardKeys','inputStages','inputValues','inputTypes','domEventOrder'])marker[name]=[];
      marker.nativePointerTrustedEventCount=0;marker.nativeKeyboardTrustedEventCount=0;marker.nativeInputTrustedEventCount=0;
      document.addEventListener('click',event=>{if(!event.composedPath().includes(input))return;
        if(!marker.expectedPointer || event.button!==0){marker.markerError='unexpected-owned-click';return;}
        marker.pointerStages.push(marker.expectedPointer);marker.domEventOrder.push('pointer:'+marker.expectedPointer);marker.nativePointerTrustedEventCount+=event.isTrusted===true?1:0;marker.expectedPointer=null;},true);
      document.addEventListener('keydown',event=>{const path=event.composedPath(),expected=marker.expectedKey;if(!path.includes(field)||!path.includes(input))return;
        if(!expected||event.key!==expected.key||event.metaKey!==expected.command||event.ctrlKey||event.altKey||event.shiftKey||event.repeat||marker.keyObserved){marker.markerError='unexpected-owned-keydown';return;}
        marker.keyObserved=true;marker.keyboardStages.push(expected.stage);marker.keyboardKeys.push(event.key);marker.nativeKeyboardTrustedEventCount+=event.isTrusted===true?1:0;marker.domEventOrder.push('key:'+expected.stage);},true);
      document.addEventListener('input',event=>{const path=event.composedPath(),expected=marker.expectedKey;if(!path.includes(field)||!path.includes(input))return;
        if(!expected||expected.input===null||!marker.keyObserved||event.isComposing||event.inputType!==expected.inputType||input.value!==expected.input||marker.inputStages.includes(expected.stage)){
          marker.markerError='unexpected-owned-input:'+JSON.stringify({stage:expected?.stage || '',value:input.value,inputType:event.inputType,isTrusted:event.isTrusted});return;}
        marker.inputStages.push(expected.stage);marker.inputValues.push(input.value);marker.inputTypes.push(event.inputType);marker.nativeInputTrustedEventCount+=event.isTrusted===true?1:0;marker.domEventOrder.push('input:'+expected.stage);},true);
    }
    """
    private static let inspection = """
    (()=>{const d=window.VaadinDesktopIpcDiagnostics || {},scope=window.VaadinDesktopHostContext || {},marker=window.VaadinDesktopLocalizedNumberValidation || {};
      const field=document.getElementById('framework-flow-localized-number-validation-field'),input=field?.inputElement;
      const text=suffix=>String(document.getElementById('framework-flow-localized-number-validation-'+suffix)?.textContent || '').trim();
      const array=name=>Array.isArray(marker[name])?marker[name]:[];
      let active=document.activeElement;while(active?.shadowRoot?.activeElement)active=active.shadowRoot.activeElement;
      const error=field?.querySelector('[slot="error-message"]') || field?.shadowRoot?.querySelector('[part="error-message"]');
      const er=error?.getBoundingClientRect(),es=error?getComputedStyle(error):null;
      const errorVisible=!!er && er.width>0 && er.height>0 && es?.display!=='none' && es?.visibility!=='hidden' && error?.getAttribute('aria-hidden')!=='true';
      const clients=Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name=>name!=='TypeScript');
      return JSON.stringify({rootRouteAvailable:!!document.getElementById('framework-flow-localized-number-validation-open-route'),
        routeReady:!!document.getElementById('framework-flow-localized-number-validation-route') && !!input && text('config-status')==='\(config)'
          && !field.disabled && !field.readonly && input.type==='text' && input.inputMode==='decimal' && field.label==='Validated decimal amount',
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,requestCount:Number(d.standardBundleOpaqueRequestCount || 0),responseCount:Number(d.standardBundleOpaqueResponseCount || 0),pushMessageCount:Number(d.standardBundlePushMessageCount || 0),flowClientCount:clients.length,
        browserWindowId:String(scope.windowId || ''),browserSessionId:String(scope.sessionId || ''),browserUiId:String(scope.uiId || ''),displayValue:String(input?.value || ''),fieldValue:String(field?.value || ''),decimalSeparator:String(field?._decimalSeparator || ''),
        configStatus:text('config-status'),valueStatus:text('value-status'),valueHistory:text('value-history'),validationStatus:text('validation-status'),validationHistory:text('validation-history'),binderStatus:text('binder-status'),binderHistory:text('binder-history'),
        invalid:field?.invalid===true,readOnly:field?.readonly===true,enabled:!!field && field.disabled!==true,inputMode:String(input?.inputMode || ''),inputType:String(input?.type || ''),label:String(field?.label || ''),ownerFocused:!!input && active===input,
        selectionStart:Number(input?.selectionStart ?? -1),selectionEnd:Number(input?.selectionEnd ?? -1),inputAriaInvalid:input?.getAttribute('aria-invalid') || '',errorMessage:String(field?.errorMessage || ''),renderedErrorText:String(error?.textContent || '').trim(),errorMessageVisible:errorVisible,
        fieldAutocorrect:field?.getAttribute('autocorrect') || '',inputAutocorrect:input?.getAttribute('autocorrect') || '',fieldAutocapitalize:field?.getAttribute('autocapitalize') || '',inputAutocapitalize:input?.getAttribute('autocapitalize') || '',
        nativePointerEventCount:array('pointerStages').length,nativePointerTrustedEventCount:Number(marker.nativePointerTrustedEventCount || 0),nativeKeyboardEventCount:array('keyboardStages').length,nativeKeyboardTrustedEventCount:Number(marker.nativeKeyboardTrustedEventCount || 0),nativeInputEventCount:array('inputStages').length,nativeInputTrustedEventCount:Number(marker.nativeInputTrustedEventCount || 0),
        pointerStages:array('pointerStages'),keyboardStages:array('keyboardStages'),keyboardKeys:array('keyboardKeys'),inputStages:array('inputStages'),inputValues:array('inputValues'),inputTypes:array('inputTypes'),domEventOrder:array('domEventOrder'),browserProgrammaticStages:array('browserProgrammaticStages'),
        browserError:String(d.standardBundleBrowserError || ''),requestError:String(d.standardBundleOpaqueRequestError || ''),responseError:String(d.standardBundleOpaqueResponseError || ''),pushError:String(d.standardBundlePushError || ''),markerError:String(marker.markerError || '')});})()
    """
}
