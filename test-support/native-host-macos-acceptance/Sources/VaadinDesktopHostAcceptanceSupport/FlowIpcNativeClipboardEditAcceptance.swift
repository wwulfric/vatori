import AppKit
import Foundation

private var nativeClipboardEditAcceptance: FlowIpcNativeClipboardEditAcceptance?

func restoreNativeClipboardEditPasteboardOnTermination() {
    nativeClipboardEditAcceptance?.restoreOnTermination()
}

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleNativeClipboardEditAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        guard context.scope == .main, nativeClipboardEditAcceptance == nil else { return }
        let acceptance = FlowIpcNativeClipboardEditAcceptance(host: host, context: context, timeout: timeoutSeconds)
        nativeClipboardEditAcceptance = acceptance
        acceptance.start()
    }
}

// Browser code only observes the normal TextField, clipboard events and DOM
// input. The sole clipboard mutation before cleanup is WebKit's native Cut.
private final class FlowIpcNativeClipboardEditAcceptance {
    private enum Phase { case root, route, focused, waiting, drain }
    private static let prefix = "framework-flow-native-edit-"
    private static let config = "Native edit config: component=TextField, mode=EAGER, initial=empty, initialLength=0"
    private static let initial = "Native edit value: value=empty, length=0, client=false, change=0, oldValue=none, oldLength=-1"
    private static let changes = [
        "value=x, length=1, client=true, change=1, oldValue=empty, oldLength=0",
        "value=empty, length=0, client=true, change=2, oldValue=x, oldLength=1",
        "value=x, length=1, client=true, change=3, oldValue=empty, oldLength=0"
    ]
    private static let stages = ["initial", "inserted", "cut", "pasted"]
    private static let keys = [
        Key(stage: "insert", name: "x", code: 7, command: false, inputType: "insertText", value: "x"),
        Key(stage: "select-all", name: "a", code: 0, command: true, inputType: nil, value: nil),
        Key(stage: "cut", name: "x", code: 7, command: true, inputType: "deleteByCut", value: ""),
        Key(stage: "paste", name: "v", code: 9, command: true, inputType: "insertFromPaste", value: "x")
    ]
    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let windowIdentity: ObjectIdentifier
    private let webViewIdentity: ObjectIdentifier
    private let timeout: Double
    private let deadline: Date
    private let pasteboard = NativeClipboardEditPasteboardGuard()
    private var phase = Phase.root
    private var finished = false
    private var pid: Int32 = 0
    private var keyIndex = 0
    private var dispatchedKeyCount = 0
    private var pointerDispatched = false
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var selectionBefore: Snapshot?
    private var selectedAll = false
    private var cutEpochObserved = false
    private var restorationVerified = false
    private var latest: Snapshot?
    private var observedStages: [String] = []
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
        // A runnable-main-queue deadline, not a watchdog for a hung AppKit call.
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) {
            guard !self.finished else { return }; self.fail("deadline")
        }
        poll()
    }
    func restoreOnTermination() {
        finished = true
        if !restorationVerified { _ = restore(reason: "termination") }
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
    private var nativeOwner: Bool {
        let view = context.window.firstResponder as? NSView
        return NSApplication.shared.isActive && context.window.isKeyWindow && NSApplication.shared.keyWindow === context.window
            && (view === context.webView || view?.isDescendant(of: context.webView) == true)
    }
    private var inputFrame: [String: Any] {
        var value = frame; value["activeApplication"] = NSApplication.shared.isActive
        value["keyWindow"] = context.window.isKeyWindow; value["nativeResponderOwned"] = nativeOwner; value["eventCount"] = 2
        return value
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
                self.fail("snapshot-scope-or-error"); return
            }
            if self.cutEpochObserved && !self.pasteboard.checkOwned() { self.fail("pasteboard-contended"); return }
            self.advance(state)
        }
    }
    private func advance(_ state: Snapshot) {
        switch phase {
        case .root:
            guard context.navigationFinished, state.rootRouteAvailable, state.firstMeaningfulPaint, state.flowClientCount == 1,
                  let actualPid = host.sidecarProcessIdentifier, actualPid > 0 else { diagnose("root-ready"); schedule(); return }
            pid = actualPid
            evaluate("""
            (()=>{const button=document.getElementById('\(Self.prefix)open-route');if(!button)return false;
              window.VaadinDesktopNativeClipboardEdit={browserProgrammaticStages:['navigate']};button.click();return true;})()
            """) { self.phase = .route; self.schedule() }
        case .route:
            guard committed(state, count: 0), inputContract(state), quiet(state, polls: 2) else { diagnose("initial-state"); schedule(); return }
            record(0, state); click()
        case .focused:
            guard committed(state, count: 0), state.ownerFocused, nativeOwner, inputContract(state) else { diagnose("field-focus"); schedule(); return }
            dispatch(state)
        case .waiting:
            if keyIndex == 1 {
                guard committed(state, count: 1), state.ownerFocused, inputContract(state), state.selectionStart == 0, state.selectionEnd == 1 else {
                    diagnose("full-selection"); schedule(); return
                }
                guard let before = selectionBefore, before.selectionStart == 1, before.selectionEnd == 1,
                      before.displayValue == "x", before.fieldValue == "x", before.ownerFocused else { fail("selection-before"); return }
                selectedAll = true
                var payload = snapshotPayload(state); payload["stage"] = "select-all"
                payload["beforeSelectionStart"] = before.selectionStart; payload["beforeSelectionEnd"] = before.selectionEnd
                payload["domKeyDownObserved"] = state.keyboardStages.contains("select-all")
                trace("recv", "nativeClipboardEdit.selectionObserved", payload)
                keyIndex += 1; dispatch(state); return
            }
            if keyIndex == 2 && !cutEpochObserved && cutEvidence(state) {
                guard pasteboard.adoptCut(causalEvidence: true) else {
                    if pasteboard.contended { fail("pasteboard-cut-contended") }
                    else { diagnose("cut-native-epoch"); schedule() }
                    return
                }
                cutEpochObserved = true
                var payload = snapshotPayload(state); payload["stage"] = "cut"
                trace("recv", "nativeClipboardEdit.pasteboardObserved", payload)
            }
            let count = keyIndex == 0 ? 1 : keyIndex == 2 ? 2 : 3
            guard committed(state, count: count), state.ownerFocused, inputContract(state),
                  state.requestCount > requestBaseline, state.responseCount > responseBaseline,
                  (count < 2 || cutEpochObserved), quiet(state, polls: 2) else { diagnose("edit-round-trip"); schedule(); return }
            record(count, state)
            if keyIndex < 3 { keyIndex += 1; dispatch(state) }
            else { resetQuiet(); phase = .drain; schedule() }
        case .drain:
            guard committed(state, count: 3), state.ownerFocused, inputContract(state), quiet(state, polls: 3) else { diagnose("final-drain"); schedule(); return }
            finish(state)
        }
    }
    private func committed(_ state: Snapshot, count: Int) -> Bool {
        let value = count == 1 || count == 3 ? "x" : ""
        let status = count == 0 ? Self.initial : "Native edit value: " + Self.changes[count - 1]
        let history = "Native edit history: " + (count == 0 ? "pending" : Self.changes.prefix(count).joined(separator: " | "))
        return retained && state.routeReady && state.firstMeaningfulPaint && state.flowClientCount == 1 && state.balanced
            && state.displayValue == value && state.fieldValue == value && state.configStatus == Self.config
            && state.valueStatus == status && state.historyStatus == history && state.windowStatus == "Native edit window: pending"
            && !state.invalid && !state.readOnly && state.enabled
            && state.fieldAutocorrect == "off" && state.inputAutocorrect == "off" && state.fieldAutocapitalize == "none" && state.inputAutocapitalize == "none"
    }
    private func cutEvidence(_ state: Snapshot) -> Bool {
        state.ownerFocused && state.cutEventCount == 1 && state.cutTrustedEventCount == 1
            && state.cutSelectionStart == 0 && state.cutSelectionEnd == 1 && state.cutSelectedValueMatches
            && state.inputStages == ["insert", "cut"] && state.inputTypes == ["insertText", "deleteByCut"]
            && state.inputValues == ["x", ""] && state.nativeInputTrustedEventCount == 2
            && state.displayValue.isEmpty && state.fieldValue.isEmpty && inputContract(state)
    }
    private func inputContract(_ state: Snapshot) -> Bool {
        let dispatched = Array(Self.keys.prefix(dispatchedKeyCount))
        let keys = dispatched.filter { !$0.command || state.keyboardStages.contains($0.stage) }
        let inputs = dispatched.filter { $0.inputType != nil }
        let clipboard = dispatched.filter { $0.stage == "cut" || $0.stage == "paste" }.map(\.stage)
        var order = pointerDispatched ? ["pointer:focus"] : []
        for key in dispatched {
            if keys.contains(where: { $0.stage == key.stage }) { order.append("key:" + key.stage) }
            if key.stage == "cut" || key.stage == "paste" { order.append("clipboard:" + key.stage) }
            if key.inputType != nil { order.append("input:" + key.stage) }
        }
        let cut = clipboard.contains("cut") ? 1 : 0, paste = clipboard.contains("paste") ? 1 : 0
        return state.pointerStages == (pointerDispatched ? ["focus"] : [])
            && state.nativePointerEventCount == (pointerDispatched ? 1 : 0) && state.nativePointerTrustedEventCount == (pointerDispatched ? 1 : 0)
            && state.keyboardStages == keys.map(\.stage) && state.keyboardKeys == keys.map(\.name)
            && state.nativeKeyboardEventCount == keys.count && state.nativeKeyboardTrustedEventCount == keys.count
            && state.inputStages == inputs.map(\.stage) && state.inputTypes == inputs.compactMap(\.inputType) && state.inputValues == inputs.compactMap(\.value)
            && state.nativeInputEventCount == inputs.count && state.nativeInputTrustedEventCount == inputs.count
            && state.clipboardEventStages == clipboard && state.clipboardEventTypes == clipboard
            && state.clipboardTargetOwned == clipboard.map { _ in true }
            && state.cutEventCount == cut && state.cutTrustedEventCount == cut && state.pasteEventCount == paste && state.pasteTrustedEventCount == paste
            && (cut == 0 ? state.cutSelectionStart == -1 && state.cutSelectionEnd == -1 && !state.cutSelectedValueMatches : state.cutSelectionStart == 0 && state.cutSelectionEnd == 1 && state.cutSelectedValueMatches)
            && state.pasteTextMatches == (paste == 1) && state.pasteTextLength == paste && state.domEventOrder == order
    }
    private func quiet(_ state: Snapshot, polls: Int) -> Bool {
        let signature = "\(phase):\(keyIndex):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
        quietCount = quietSignature == signature ? quietCount + 1 : 1; quietSignature = signature
        return quietCount >= polls
    }
    private func resetQuiet() { quietSignature = ""; quietCount = 0 }
    private func snapshotPayload(_ state: Snapshot) -> [String: Any] {
        var value = frame; value.merge(state.payload) { _, replacement in replacement }
        value.merge(pasteboard.payload) { _, replacement in replacement }
        value["selectAllObserved"] = selectedAll ? ["select-all"] : [String]()
        return value
    }
    private func record(_ count: Int, _ state: Snapshot) {
        observedStages.append(Self.stages[count]); var payload = snapshotPayload(state)
        payload["stage"] = Self.stages[count]; payload["requestBaseline"] = count == 0 ? state.requestCount : requestBaseline
        payload["responseBaseline"] = count == 0 ? state.responseCount : responseBaseline
        payload["uidlRoundTripObserved"] = count > 0; payload["quietPollCount"] = quietCount
        trace("recv", "nativeClipboardEdit.stageObserved", payload)
    }
    private func finish(_ state: Snapshot) {
        guard observedStages == Self.stages, selectedAll, cutEpochObserved, dispatchedKeyCount == 4,
              state.browserProgrammaticStages == ["navigate", "position-focus"], pasteboard.checkOwned() else { fail("final-contract"); return }
        guard restore(reason: "success") else { fail("pasteboard-restore"); return }
        var payload = snapshotPayload(state)
        payload.merge(["stage": "pasted", "complete": true, "errorFree": true, "standardBundleMode": true, "rootRendered": true,
            "flowClientOwned": true, "opaqueRequestBridgeObserved": true, "opaqueResponseBridgeObserved": true,
            "pushObserved": false, "generatedAppKitInput": true, "physicalInputClaimed": false, "imeClaimed": false,
            "assistiveTechnologyClaimed": false, "nativeInputCausalObserved": true, "cutObserved": true, "pasteObserved": true,
            "nativePasteboardCutObserved": true, "nativePasteboardRestored": true, "observedStages": observedStages,
            "nativeKeyDispatchCount": dispatchedKeyCount, "drainQuietPollCount": quietCount]) { _, replacement in replacement }
        finished = true; trace("recv", "nativeClipboardEdit.frameworkAcceptance", payload); context.window.close()
    }
    private func restore(reason: String) -> Bool {
        let result = pasteboard.restore(reason: reason).payload
        var payload = frame; payload.merge(result) { _, replacement in replacement }; payload["reason"] = reason
        trace("recv", "nativeClipboardEdit.pasteboardRestored", payload)
        restorationVerified = result["snapshotRestored"] as? Bool == true && result["snapshotVerified"] as? Bool == true
        return restorationVerified
    }
    private func click() {
        context.webView.evaluateJavaScript("""
        (()=>{\(Self.instrumentation)
          input.scrollIntoView({block:'center',inline:'nearest'});const r=input.getBoundingClientRect(),x=r.left+r.width/2,y=r.top+r.height/2;
          let hit=document.elementFromPoint(x,y);while(hit?.shadowRoot?.elementFromPoint){const next=hit.shadowRoot.elementFromPoint(x,y);if(!next||next===hit)break;hit=next;}
          if(hit!==input||r.width<=0||r.height<=0||x<0||y<0||x>=innerWidth||y>=innerHeight)return '';
          marker.expectedPointer=true;marker.browserProgrammaticStages.push('position-focus');return JSON.stringify({x,y});})()
        """) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8), let point = try? JSONDecoder().decode(Geometry.self, from: data) else { self.fail("pointer-geometry"); return }
            self.activate {
                guard !self.finished, self.retained else { return }
                let point = NSPoint(x: point.x, y: self.context.webView.isFlipped ? point.y : self.context.webView.bounds.height - point.y)
                let location = self.context.webView.convert(point, to: nil), time = ProcessInfo.processInfo.systemUptime
                guard self.context.window.contentLayoutRect.contains(location),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [], timestamp: time, windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [], timestamp: time + 0.05, windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else { self.fail("pointer-event"); return }
                var payload = self.inputFrame; payload.merge(["stage": "focus", "inputMode": "appkit-window-pointer-event", "button": "primary"]) { _, replacement in replacement }
                trace("recv", "nativeClipboardEdit.nativePointerDispatch", payload); self.pointerDispatched = true; self.context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { guard !self.finished else { return }; self.context.window.sendEvent(up); self.phase = .focused; self.schedule() }
            }
        }
    }
    private func dispatch(_ state: Snapshot) {
        resetQuiet(); let key = Self.keys[keyIndex]
        requestBaseline = state.requestCount; responseBaseline = state.responseCount
        if key.stage == "select-all" { selectionBefore = state }
        let spec: [String: Any] = ["stage": key.stage, "key": key.name, "command": key.command,
            "inputType": key.inputType as Any? ?? NSNull(), "value": key.value as Any? ?? NSNull()]
        guard let data = try? JSONSerialization.data(withJSONObject: spec), let json = String(data: data, encoding: .utf8) else { fail("key-spec"); return }
        evaluate("""
        (()=>{const marker=window.VaadinDesktopNativeClipboardEdit,input=document.getElementById('\(Self.prefix)field')?.inputElement;
          let active=document.activeElement;while(active?.shadowRoot?.activeElement)active=active.shadowRoot.activeElement;
          if(!marker||!input||active!==input)return false;marker.expectedKey=\(json);marker.keyObserved=false;return true;})()
        """) {
            self.activate {
                guard !self.finished, self.nativeOwner else { self.fail("key-owner"); return }
                if key.stage == "cut" {
                    guard self.pasteboard.capture() else { self.fail("pasteboard-capture"); return }
                } else if key.stage == "paste" {
                    guard self.cutEpochObserved, self.pasteboard.checkOwned() else { self.fail("pasteboard-before-paste"); return }
                }
                // Materializing promised pasteboard representations may take
                // time. Never arm or send an edit using pre-read focus state.
                guard !self.finished, Date() < self.deadline, self.retained, self.nativeOwner else {
                    self.fail(Date() >= self.deadline ? "deadline" : "key-owner-after-pasteboard-read"); return
                }
                let time = ProcessInfo.processInfo.systemUptime
                guard let down = key.event(.keyDown, window: self.context.window.windowNumber, time: time),
                      let up = key.event(.keyUp, window: self.context.window.windowNumber, time: time + 0.01) else { self.fail("key-event"); return }
                if key.stage == "cut" {
                    guard self.pasteboard.armCut() else { self.fail("pasteboard-arm"); return }
                }
                var payload = self.inputFrame
                payload.merge(self.pasteboard.payload) { _, replacement in replacement }
                payload.merge(["stage": key.stage, "key": key.name, "keyCode": Int(key.code), "modifiers": key.command ? "command" : "none", "inputMode": "appkit-application-key-event"]) { _, replacement in replacement }
                trace("recv", "nativeClipboardEdit.nativeKeyDispatch", payload); self.dispatchedKeyCount += 1
                NSApplication.shared.sendEvent(down); NSApplication.shared.sendEvent(up); self.phase = .waiting; self.schedule(after: 0.1)
            }
        }
    }
    private func activate(_ completion: @escaping () -> Void) {
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline, onFailure: { _ in self.fail("activation") }, completion: completion)
    }
    private func evaluate(_ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard !self.finished else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }; completion()
        }
    }
    private func schedule(after delay: Double = 0.2) { DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() } }
    private func diagnose(_ reason: String) {
        let signature = "\(phase):\(keyIndex):\(reason)"; guard diagnosticSignature != signature else { return }; diagnosticSignature = signature
        var payload = frame; payload["reason"] = reason; payload["phase"] = "flow-ipc-native-clipboard-edit-" + String(describing: phase)
        payload["keyIndex"] = keyIndex; payload["snapshot"] = latest?.payload
        trace("recv", "nativeClipboardEdit.driverDiagnostic", payload)
    }
    private func fail(_ reason: String) {
        guard !finished else { return }; finished = true
        _ = restore(reason: reason == "deadline" ? "deadline" : "failure")
        var payload = frame; payload.merge(pasteboard.payload) { _, replacement in replacement }
        payload["code"] = "flow-ipc-native-clipboard-edit-" + reason; payload["phase"] = "flow-ipc-native-clipboard-edit-" + String(describing: phase)
        payload["snapshot"] = latest?.payload; trace("send", "error", payload); context.window.close()
    }
    private struct Geometry: Decodable { let x: Double; let y: Double }
    private struct Key {
        let stage: String; let name: String; let code: UInt16; let command: Bool; let inputType: String?; let value: String?
        func event(_ type: NSEvent.EventType, window: Int, time: TimeInterval) -> NSEvent? {
            NSEvent.keyEvent(with: type, location: .zero, modifierFlags: command ? [.command] : [], timestamp: time,
                windowNumber: window, context: nil, characters: name, charactersIgnoringModifiers: name, isARepeat: false, keyCode: code)
        }
    }
    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let displayValue: String; let fieldValue: String; let configStatus: String; let valueStatus: String; let historyStatus: String; let windowStatus: String
        let invalid: Bool; let readOnly: Bool; let enabled: Bool; let inputMode: String; let inputType: String; let label: String; let ownerFocused: Bool
        let selectionStart: Int; let selectionEnd: Int
        let fieldAutocorrect: String; let inputAutocorrect: String; let fieldAutocapitalize: String; let inputAutocapitalize: String
        let nativePointerEventCount: Int; let nativePointerTrustedEventCount: Int; let nativeKeyboardEventCount: Int; let nativeKeyboardTrustedEventCount: Int; let nativeInputEventCount: Int; let nativeInputTrustedEventCount: Int
        let pointerStages: [String]; let keyboardStages: [String]; let keyboardKeys: [String]; let inputStages: [String]; let inputTypes: [String]; let inputValues: [String]; let domEventOrder: [String]; let browserProgrammaticStages: [String]
        let cutEventCount: Int; let cutTrustedEventCount: Int; let pasteEventCount: Int; let pasteTrustedEventCount: Int
        let clipboardEventStages: [String]; let clipboardEventTypes: [String]; let clipboardTargetOwned: [Bool]
        let cutSelectionStart: Int; let cutSelectionEnd: Int; let cutSelectedValueMatches: Bool; let pasteTextMatches: Bool; let pasteTextLength: Int
        let browserError: String; let requestError: String; let responseError: String; let pushError: String; let markerError: String
        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var errorFree: Bool { browserError.isEmpty && requestError.isEmpty && responseError.isEmpty && pushError.isEmpty && markerError.isEmpty }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self), let result = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }; return result
        }
    }
    private static let instrumentation = """
    const marker=window.VaadinDesktopNativeClipboardEdit,field=document.getElementById('framework-flow-native-edit-field'),input=field?.inputElement;
    if(!marker||!input)return '';
    if(!marker.installed){marker.installed=true;for(const key of ['pointerStages','keyboardStages','keyboardKeys','inputStages','inputTypes','inputValues','domEventOrder','clipboardEventStages','clipboardEventTypes','clipboardTargetOwned'])marker[key]=[];
      for(const key of ['nativePointerTrustedEventCount','nativeKeyboardTrustedEventCount','nativeInputTrustedEventCount','cutEventCount','cutTrustedEventCount','pasteEventCount','pasteTrustedEventCount','pasteTextLength'])marker[key]=0;
      marker.cutSelectionStart=-1;marker.cutSelectionEnd=-1;marker.cutSelectedValueMatches=false;marker.pasteTextMatches=false;
      document.addEventListener('click',event=>{const path=event.composedPath();if(!path.includes(field)||!path.includes(input))return;
        if(!marker.expectedPointer||event.button!==0){marker.markerError='unexpected-owned-click';return;}
        marker.pointerStages.push('focus');marker.domEventOrder.push('pointer:focus');marker.nativePointerTrustedEventCount+=event.isTrusted===true?1:0;marker.expectedPointer=false;},true);
      document.addEventListener('keydown',event=>{const path=event.composedPath(),expected=marker.expectedKey;if(!path.includes(field)||!path.includes(input))return;
        if(!expected||event.key!==expected.key||event.metaKey!==expected.command||event.shiftKey||event.ctrlKey||event.altKey||event.repeat||marker.keyObserved){marker.markerError='unexpected-owned-keydown';return;}
        marker.keyObserved=true;marker.keyboardStages.push(expected.stage);marker.keyboardKeys.push(event.key);marker.nativeKeyboardTrustedEventCount+=event.isTrusted===true?1:0;marker.domEventOrder.push('key:'+expected.stage);},true);
      for(const type of ['cut','paste'])document.addEventListener(type,event=>{const path=event.composedPath(),expected=marker.expectedKey;if(!path.includes(field)||!path.includes(input))return;
        if(!expected||expected.stage!==type||marker.clipboardEventStages.includes(type)){marker.markerError='unexpected-owned-clipboard-event';return;}
        marker.clipboardEventStages.push(type);marker.clipboardEventTypes.push(event.type);marker.clipboardTargetOwned.push(true);marker.domEventOrder.push('clipboard:'+type);
        marker[type+'EventCount']++;marker[type+'TrustedEventCount']+=event.isTrusted===true?1:0;
        if(type==='cut'){marker.cutSelectionStart=Number(input.selectionStart ?? -1);marker.cutSelectionEnd=Number(input.selectionEnd ?? -1);marker.cutSelectedValueMatches=input.value==='x';}
        else {const pasted=event.clipboardData?.getData('text/plain') ?? '';marker.pasteTextMatches=pasted==='x';marker.pasteTextLength=pasted.length;if(!marker.pasteTextMatches)marker.markerError='unexpected-paste-text';}
      },true);
      document.addEventListener('input',event=>{const path=event.composedPath(),expected=marker.expectedKey;if(!path.includes(field)||!path.includes(input))return;
        if(!expected||expected.inputType===null||(!expected.command&&!marker.keyObserved)||event.isComposing||event.inputType!==expected.inputType||input.value!==expected.value||marker.inputStages.includes(expected.stage)
          ||(['cut','paste'].includes(expected.stage)&&!marker.clipboardEventStages.includes(expected.stage))){marker.markerError='unexpected-owned-input';return;}
        marker.inputStages.push(expected.stage);marker.inputTypes.push(event.inputType);marker.inputValues.push(input.value);marker.nativeInputTrustedEventCount+=event.isTrusted===true?1:0;marker.domEventOrder.push('input:'+expected.stage);},true);
    }
    """
    private static let inspection = """
    (()=>{const d=window.VaadinDesktopIpcDiagnostics || {},scope=window.VaadinDesktopHostContext || {},marker=window.VaadinDesktopNativeClipboardEdit || {};
      const field=document.getElementById('framework-flow-native-edit-field'),input=field?.inputElement;
      const text=suffix=>String(document.getElementById('framework-flow-native-edit-'+suffix)?.textContent || '').trim();
      const entries=['\(changes[0])','\(changes[1])','\(changes[2])'];
      const values=['\(initial)',...entries.map(e=>'Native edit value: '+e)];
      const histories=['Native edit history: pending',...entries.map((_,i)=>'Native edit history: '+entries.slice(0,i+1).join(' | '))];
      const safe=(value,allowed)=>allowed.includes(value)?value:'<redacted>',safeValue=value=>safe(String(value ?? ''),['','x']);
      const array=key=>Array.isArray(marker[key])?marker[key]:[];const issue=value=>value?'observed-error':'';
      let active=document.activeElement;while(active?.shadowRoot?.activeElement)active=active.shadowRoot.activeElement;
      const clients=Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name=>name!=='TypeScript');
      return JSON.stringify({rootRouteAvailable:!!document.getElementById('framework-flow-native-edit-open-route'),
        routeReady:!!document.getElementById('framework-flow-native-edit-route')&&!!input&&text('config-status')==='\(config)'&&!field.disabled&&!field.readonly&&!field.invalid&&input.type==='text'&&field.label==='Native edit text',
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,requestCount:Number(d.standardBundleOpaqueRequestCount || 0),responseCount:Number(d.standardBundleOpaqueResponseCount || 0),pushMessageCount:Number(d.standardBundlePushMessageCount || 0),flowClientCount:clients.length,
        browserWindowId:String(scope.windowId || ''),browserSessionId:String(scope.sessionId || ''),browserUiId:String(scope.uiId || ''),displayValue:safeValue(input?.value),fieldValue:safeValue(field?.value),
        configStatus:safe(text('config-status'),['','\(config)']),valueStatus:safe(text('value-status'),['',...values]),historyStatus:safe(text('history-status'),['',...histories]),windowStatus:safe(text('window-status'),['','Native edit window: pending']),
        invalid:field?.invalid===true,readOnly:field?.readonly===true,enabled:!!field&&field.disabled!==true,inputMode:safe(String(input?.inputMode || ''),['']),inputType:safe(String(input?.type || ''),['','text']),label:safe(String(field?.label || ''),['','Native edit text']),ownerFocused:!!input&&active===input,
        selectionStart:Number(input?.selectionStart ?? -1),selectionEnd:Number(input?.selectionEnd ?? -1),fieldAutocorrect:safe(field?.getAttribute('autocorrect') || '',['','off']),inputAutocorrect:safe(input?.getAttribute('autocorrect') || '',['','off']),fieldAutocapitalize:safe(field?.getAttribute('autocapitalize') || '',['','none']),inputAutocapitalize:safe(input?.getAttribute('autocapitalize') || '',['','none']),
        nativePointerEventCount:array('pointerStages').length,nativePointerTrustedEventCount:Number(marker.nativePointerTrustedEventCount || 0),nativeKeyboardEventCount:array('keyboardStages').length,nativeKeyboardTrustedEventCount:Number(marker.nativeKeyboardTrustedEventCount || 0),nativeInputEventCount:array('inputStages').length,nativeInputTrustedEventCount:Number(marker.nativeInputTrustedEventCount || 0),
        pointerStages:array('pointerStages'),keyboardStages:array('keyboardStages'),keyboardKeys:array('keyboardKeys'),inputStages:array('inputStages'),inputTypes:array('inputTypes'),inputValues:array('inputValues'),domEventOrder:array('domEventOrder'),browserProgrammaticStages:array('browserProgrammaticStages'),
        cutEventCount:Number(marker.cutEventCount || 0),cutTrustedEventCount:Number(marker.cutTrustedEventCount || 0),pasteEventCount:Number(marker.pasteEventCount || 0),pasteTrustedEventCount:Number(marker.pasteTrustedEventCount || 0),clipboardEventStages:array('clipboardEventStages'),clipboardEventTypes:array('clipboardEventTypes'),clipboardTargetOwned:array('clipboardTargetOwned'),
        cutSelectionStart:Number(marker.cutSelectionStart ?? -1),cutSelectionEnd:Number(marker.cutSelectionEnd ?? -1),cutSelectedValueMatches:marker.cutSelectedValueMatches===true,pasteTextMatches:marker.pasteTextMatches===true,pasteTextLength:Number(marker.pasteTextLength || 0),
        browserError:issue(d.standardBundleBrowserError),requestError:issue(d.standardBundleOpaqueRequestError),responseError:issue(d.standardBundleOpaqueResponseError),pushError:issue(d.standardBundlePushError),markerError:issue(marker.markerError)});})()
    """
}
