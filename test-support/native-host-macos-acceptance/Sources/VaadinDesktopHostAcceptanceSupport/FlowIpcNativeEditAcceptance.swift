import AppKit
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleNativeEditAcceptance(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        FlowIpcNativeEditAcceptance(host: host, context: context, timeoutSeconds: timeoutSeconds).start()
    }
}

// Generated AppKit input tests the normal application/responder-chain path.
// Browser scripts only navigate, position and observe; they never edit values,
// invoke undo/redo, or construct synthetic DOM input events.
private final class FlowIpcNativeEditAcceptance {
    private enum Phase { case root, route, focused, waiting, drain }
    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let timeoutSeconds: Double
    private let deadline: Date
    private var phase = Phase.root
    private var finished = false
    private var processIdentifier: Int32 = 0
    private var keyIndex = 0
    private var requestBaseline = 0
    private var responseBaseline = 0
    private var observedStages: [String] = []
    private var drainCounts: [Int] = []
    private var drainQuietPollCount = 0
    private var lastDiagnostic = ""
    private var lastSnapshot: Snapshot?

    private static let prefix = "framework-flow-native-edit-"
    private static let config = "Native edit config: component=TextField, mode=EAGER, initial=empty, initialLength=0"
    private static let initial = "Native edit value: value=empty, length=0, client=false, change=0, oldValue=none, oldLength=-1"
    private static let changes = [
        "value=x, length=1, client=true, change=1, oldValue=empty, oldLength=0",
        "value=empty, length=0, client=true, change=2, oldValue=x, oldLength=1",
        "value=x, length=1, client=true, change=3, oldValue=empty, oldLength=0"
    ]
    private static let stageNames = ["initial", "inserted", "undone", "redone"]
    private static let keys = [
        Key(stage: "insert", name: "x", code: 7, command: false, shift: false, inputType: "insertText", value: "x"),
        Key(stage: "undo", name: "z", code: 6, command: true, shift: false, inputType: "historyUndo", value: ""),
        Key(stage: "redo", name: "Z", code: 6, command: true, shift: true, inputType: "historyRedo", value: "x")
    ]

    init(host: WebViewHost, context: FlowIpcWindowContext, timeoutSeconds: Double) {
        self.host = host
        self.context = context
        self.timeoutSeconds = timeoutSeconds
        self.deadline = Date().addingTimeInterval(timeoutSeconds)
    }

    func start() {
        guard timeoutSeconds.isFinite, timeoutSeconds > 0, timeoutSeconds <= 300 else {
            fail("invalid-timeout"); return
        }
        // Independent of evaluateJavaScript completion. This is bounded while
        // the main queue remains runnable, not a hung-main-thread watchdog.
        DispatchQueue.main.asyncAfter(deadline: .now() + timeoutSeconds) {
            guard !self.finished else { return }
            self.fail("deadline", details: self.lastSnapshot?.payload ?? [:])
        }
        poll()
    }

    private func poll() {
        guard !finished else { return }
        guard Date() < deadline else { fail("deadline"); return }
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnostic("snapshot-unavailable"); self.schedule(); return
            }
            self.lastSnapshot = state
            guard state.errorFree else { self.fail("browser-runtime", details: state.payload); return }
            guard state.browserWindowId == self.context.scope.windowId,
                  state.browserSessionId == self.context.scope.sessionId,
                  state.browserUiId == self.context.scope.uiId,
                  self.context.recoveryGeneration == 0,
                  self.processIdentifier == 0 || self.host.sidecarProcessIdentifier == self.processIdentifier else {
                self.fail("runtime-identity", details: state.payload); return
            }
            self.advance(state)
        }
    }

    private func advance(_ state: Snapshot) {
        switch phase {
        case .root:
            guard context.navigationFinished, state.firstMeaningfulPaint, state.rootRouteAvailable,
                  state.flowClientCount > 0, let pid = host.sidecarProcessIdentifier, pid > 0 else {
                diagnostic("root-not-ready"); schedule(); return
            }
            processIdentifier = pid
            evaluate("""
            (() => {
              const trigger = document.getElementById('\(Self.prefix)open-route');
              if (!trigger) return false;
              window.VaadinDesktopNativeEdit = {browserProgrammaticStages:['navigate']};
              trigger.click();
              return true;
            })()
            """) { self.phase = .route; self.schedule() }
        case .route:
            guard state.routeReady, state.balanced, state.displayValue.isEmpty, state.fieldValue.isEmpty,
                  state.valueStatus == Self.initial, state.historyStatus == "Native edit history: pending",
                  inputContract(state, count: 0, focused: false) else {
                diagnostic("initial-not-ready"); schedule(); return
            }
            recordStage(0, state: state)
            click()
        case .focused:
            guard state.routeReady, state.ownerFocused, state.balanced, state.displayValue.isEmpty,
                  state.fieldValue.isEmpty, state.valueStatus == Self.initial,
                  state.historyStatus == "Native edit history: pending",
                  inputContract(state, count: 0, focused: true) else {
                diagnostic("field-focus"); schedule(); return
            }
            dispatchNext(state)
        case .waiting:
            let count = keyIndex + 1
            guard committed(state, count: count), state.balanced, roundTrip(state) else {
                diagnostic("await-" + Self.keys[keyIndex].stage); schedule(); return
            }
            recordStage(count, state: state)
            keyIndex += 1
            if keyIndex < Self.keys.count { dispatchNext(state) }
            else { phase = .drain; schedule() }
        case .drain:
            guard committed(state, count: 3), state.balanced else {
                drainCounts = []; drainQuietPollCount = 0
                diagnostic("final-drain"); schedule(); return
            }
            let counts = [state.requestCount, state.responseCount, state.pushMessageCount]
            if counts == drainCounts { drainQuietPollCount += 1 }
            else { drainCounts = counts; drainQuietPollCount = 1 }
            guard drainQuietPollCount >= 3 else { schedule(after: 0.3); return }
            finish(state)
        }
    }

    private func committed(_ state: Snapshot, count: Int) -> Bool {
        let expected = Self.keys[count - 1].value
        return state.routeReady && state.ownerFocused && state.displayValue == expected && state.fieldValue == expected
            && state.valueStatus == "Native edit value: " + Self.changes[count - 1]
            && state.historyStatus == "Native edit history: " + Self.changes.prefix(count).joined(separator: " | ")
            && inputContract(state, count: count, focused: true)
    }

    private func inputContract(_ state: Snapshot, count: Int, focused: Bool) -> Bool {
        let dispatched = Array(Self.keys.prefix(count))
        let actualKeys = dispatched.filter { !$0.command || state.keyboardStages.contains($0.stage) }
        var order = focused ? ["pointer:focus"] : []
        for key in dispatched {
            if actualKeys.contains(where: { $0.stage == key.stage }) { order.append("key:" + key.stage) }
            order.append("input:" + key.stage)
        }
        return state.nativePointerEventCount == (focused ? 1 : 0)
            && state.nativePointerTrustedEventCount == (focused ? 1 : 0)
            && state.pointerStages == (focused ? ["focus"] : [])
            && state.nativeKeyboardEventCount == actualKeys.count
            && state.nativeKeyboardTrustedEventCount == actualKeys.count
            && state.keyboardStages == actualKeys.map(\.stage) && state.keyboardKeys == actualKeys.map(\.name)
            && state.nativeInputEventCount == count && state.nativeInputTrustedEventCount == count
            && state.inputStages == dispatched.map(\.stage) && state.inputTypes == dispatched.map(\.inputType)
            && state.inputValues == dispatched.map(\.value) && state.domEventOrder == order
    }

    private func roundTrip(_ state: Snapshot) -> Bool {
        state.requestCount > requestBaseline && state.responseCount > responseBaseline
    }

    private func recordStage(_ index: Int, state: Snapshot) {
        observedStages.append(Self.stageNames[index])
        var payload = state.payload
        payload.merge(frame) { _, replacement in replacement }
        payload["stage"] = Self.stageNames[index]
        payload["requestBaseline"] = index == 0 ? state.requestCount : requestBaseline
        payload["responseBaseline"] = index == 0 ? state.responseCount : responseBaseline
        payload["uidlRoundTripObserved"] = index > 0
        trace("recv", "nativeEdit.stageObserved", payload)
    }

    private var frame: [String: Any] {
        context.scope.applying(to: ["hostProcessIdentifier": getpid(), "processIdentifier": processIdentifier,
            "recoveryGeneration": context.recoveryGeneration])
    }

    private var inputFrame: [String: Any] {
        var payload = frame
        payload["activeApplication"] = NSApplication.shared.isActive
        payload["keyWindow"] = context.window.isKeyWindow
        payload["windowNumber"] = context.window.windowNumber
        payload["eventCount"] = 2
        return payload
    }

    private func finish(_ state: Snapshot) {
        guard observedStages == Self.stageNames, state.errorFree,
              state.browserProgrammaticStages == ["navigate", "position-focus"], state.pushMessageCount >= 0 else {
            fail("final-contract", details: state.payload); return
        }
        var payload = state.payload
        payload.merge(frame) { _, replacement in replacement }
        let flags: [String: Any] = [
            "complete": true, "errorFree": true, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.routeReady, "flowClientOwned": state.flowClientCount > 0,
            "opaqueRequestBridgeObserved": state.requestCount > 0, "opaqueResponseBridgeObserved": state.responseCount > 0,
            "pushObserved": false, "initialObserved": true, "insertObserved": true, "undoObserved": true, "redoObserved": true,
            "nativeInputCausalObserved": true, "generatedAppKitInput": true, "physicalInputClaimed": false,
            "imeClaimed": false, "assistiveTechnologyClaimed": false, "observedStages": observedStages,
            "requestBaseline": requestBaseline, "responseBaseline": responseBaseline, "uidlRoundTripObserved": true,
            "drainQuietPollCount": drainQuietPollCount]
        payload.merge(flags) { _, replacement in replacement }
        finished = true
        trace("recv", "nativeEdit.frameworkAcceptance", payload)
        context.window.close()
    }

    private func click() {
        context.webView.evaluateJavaScript("""
        (() => {
          \(Self.instrumentation)
          input.scrollIntoView({block:'center',inline:'nearest'});
          const rect = input.getBoundingClientRect();
          const x = rect.left + rect.width / 2, y = rect.top + rect.height / 2;
          let hit = document.elementFromPoint(x,y);
          while (hit?.shadowRoot?.elementFromPoint) {
            const inner = hit.shadowRoot.elementFromPoint(x,y);
            if (!inner || inner === hit) break;
            hit = inner;
          }
          const targetMatched = hit === input && rect.width > 0 && rect.height > 0
            && Number.isFinite(x) && Number.isFinite(y) && x >= 0 && y >= 0 && x < innerWidth && y < innerHeight;
          if (targetMatched) { marker.expectedPointer = true; marker.browserProgrammaticStages.push('position-focus'); }
          return JSON.stringify({x,y,targetMatched});
        })()
        """) { result, error in
            guard !self.finished else { return }
            guard error == nil, let json = result as? String, let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: data), geometry.targetMatched else {
                self.fail("pointer-geometry"); return
            }
            self.activate {
                guard !self.finished else { return }
                let point = NSPoint(x: geometry.x, y: self.context.webView.isFlipped
                    ? geometry.y : self.context.webView.bounds.height - geometry.y)
                let location = self.context.webView.convert(point, to: nil)
                guard self.context.window.contentLayoutRect.contains(location) else { self.fail("pointer-outside-window"); return }
                let time = ProcessInfo.processInfo.systemUptime
                guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [], timestamp: time,
                    windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 0, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [], timestamp: time + 0.05,
                    windowNumber: self.context.window.windowNumber, context: nil, eventNumber: 1, clickCount: 1, pressure: 0) else {
                    self.fail("pointer-event-creation"); return
                }
                var payload = self.inputFrame
                payload["stage"] = "focus"; payload["button"] = "primary"; payload["inputMode"] = "appkit-window-pointer-event"
                trace("recv", "nativeEdit.nativePointerDispatch", payload)
                self.context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard !self.finished else { return }
                    self.context.window.sendEvent(up)
                    self.phase = .focused; self.schedule(after: 0.15)
                }
            }
        }
    }

    private func dispatchNext(_ state: Snapshot) {
        requestBaseline = state.requestCount
        responseBaseline = state.responseCount
        let key = Self.keys[keyIndex]
        let spec: [String: Any] = ["stage": key.stage, "key": key.name, "command": key.command,
            "shift": key.shift, "inputType": key.inputType, "value": key.value]
        guard let data = try? JSONSerialization.data(withJSONObject: spec), let json = String(data: data, encoding: .utf8) else {
            fail("key-specification"); return
        }
        evaluate("""
        (() => {
          const marker = window.VaadinDesktopNativeEdit;
          const input = document.getElementById('\(Self.prefix)field')?.inputElement;
          let active = document.activeElement;
          while (active?.shadowRoot?.activeElement) active = active.shadowRoot.activeElement;
          if (!marker || !input || active !== input) return false;
          marker.expectedKey = \(json);
          marker.keyObserved = false;
          return true;
        })()
        """) {
            self.activate {
                guard !self.finished else { return }
                let time = ProcessInfo.processInfo.systemUptime
                guard let down = key.event(.keyDown, window: self.context.window.windowNumber, timestamp: time),
                      let up = key.event(.keyUp, window: self.context.window.windowNumber, timestamp: time + 0.01) else {
                    self.fail("key-event-creation"); return
                }
                var payload = self.inputFrame
                payload.merge(["stage": key.stage, "key": key.name, "keyCode": Int(key.code),
                    "modifiers": key.shift ? "command+shift" : key.command ? "command" : "none",
                    "inputMode": "appkit-application-key-event"]) { _, replacement in replacement }
                trace("recv", "nativeEdit.nativeKeyDispatch", payload)
                NSApplication.shared.sendEvent(down)
                NSApplication.shared.sendEvent(up)
                self.phase = .waiting; self.schedule(after: 0.1)
            }
        }
    }

    private func activate(_ completion: @escaping () -> Void) {
        FlowIpcNativeInputActivation.prepare(context: context, deadline: deadline,
            onFailure: { self.fail("activation", details: $0) }, completion: completion)
    }

    private func evaluate(_ script: String, completion: @escaping () -> Void) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard !self.finished else { return }
            guard error == nil, result as? Bool == true else { self.fail("preparation-script"); return }
            completion()
        }
    }

    private func schedule(after delay: Double = 0.2) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { self.poll() }
    }

    private func diagnostic(_ reason: String) {
        let signature = "\(phase):\(keyIndex):\(reason):\(NSApplication.shared.isActive):\(context.window.isKeyWindow)"
        guard signature != lastDiagnostic else { return }
        lastDiagnostic = signature
        var payload = lastSnapshot?.payload ?? [:]
        payload.merge(inputFrame) { _, replacement in replacement }
        payload["phase"] = "flow-ipc-native-edit-" + String(describing: phase)
        payload["reason"] = reason; payload["keyIndex"] = keyIndex
        trace("recv", "nativeEdit.driverDiagnostic", payload)
    }

    private func fail(_ reason: String, details: [String: Any] = [:]) {
        guard !finished else { return }
        finished = true
        var payload = details
        payload.merge(frame) { _, replacement in replacement }
        payload["code"] = "flow-ipc-native-edit-" + reason
        payload["phase"] = "flow-ipc-native-edit-" + String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Geometry: Decodable { let x: Double; let y: Double; let targetMatched: Bool }
    private struct Key {
        let stage: String; let name: String; let code: UInt16
        let command: Bool; let shift: Bool; let inputType: String; let value: String
        func event(_ type: NSEvent.EventType, window: Int, timestamp: TimeInterval) -> NSEvent? {
            var flags: NSEvent.ModifierFlags = []
            if command { flags.insert(.command) }; if shift { flags.insert(.shift) }
            return NSEvent.keyEvent(with: type, location: .zero, modifierFlags: flags, timestamp: timestamp,
                windowNumber: window, context: nil, characters: name, charactersIgnoringModifiers: name,
                isARepeat: false, keyCode: code)
        }
    }

    private struct Snapshot: Codable {
        let rootRouteAvailable: Bool; let routeReady: Bool; let firstMeaningfulPaint: Bool
        let requestCount: Int; let responseCount: Int; let pushMessageCount: Int; let flowClientCount: Int
        let browserWindowId: String; let browserSessionId: String; let browserUiId: String
        let displayValue: String; let fieldValue: String
        let configStatus: String; let valueStatus: String; let historyStatus: String
        let invalid: Bool; let readOnly: Bool; let enabled: Bool
        let inputMode: String; let inputType: String; let label: String; let ownerFocused: Bool
        let selectionStart: Int; let selectionEnd: Int
        let nativePointerEventCount: Int; let nativePointerTrustedEventCount: Int
        let nativeKeyboardEventCount: Int; let nativeKeyboardTrustedEventCount: Int
        let nativeInputEventCount: Int; let nativeInputTrustedEventCount: Int
        let pointerStages: [String]; let keyboardStages: [String]; let keyboardKeys: [String]
        let inputStages: [String]; let inputTypes: [String]; let inputValues: [String]; let domEventOrder: [String]
        let browserProgrammaticStages: [String]
        let browserError: String; let requestError: String; let responseError: String; let pushError: String; let markerError: String
        var balanced: Bool { requestCount > 0 && requestCount == responseCount }
        var errorFree: Bool { browserError.isEmpty && requestError.isEmpty && responseError.isEmpty && pushError.isEmpty && markerError.isEmpty }
        var payload: [String: Any] {
            guard let data = try? JSONEncoder().encode(self),
                  let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [:] }
            return object
        }
    }

    private static let instrumentation = """
    const marker = window.VaadinDesktopNativeEdit;
    const field = document.getElementById('framework-flow-native-edit-field');
    const input = field?.inputElement;
    if (!marker || !input) return '';
    if (!marker.installed) {
      marker.installed = true;
      for (const name of ['pointerStages','keyboardStages','keyboardKeys','inputStages','inputTypes','inputValues','domEventOrder']) marker[name] = [];
      marker.nativePointerTrustedEventCount = 0; marker.nativeKeyboardTrustedEventCount = 0; marker.nativeInputTrustedEventCount = 0;
      document.addEventListener('click', event => {
        if (!event.composedPath().includes(input) || !event.composedPath().includes(field)) return;
        if (!marker.expectedPointer || event.button !== 0) { marker.markerError = 'unexpected-owned-click'; return; }
        marker.pointerStages.push('focus'); marker.domEventOrder.push('pointer:focus');
        marker.nativePointerTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.expectedPointer = false;
      }, true);
      document.addEventListener('keydown', event => {
        const path = event.composedPath(), expected = marker.expectedKey;
        if (!path.includes(field) || !path.includes(input)) return;
        if (!expected || event.key !== expected.key || event.metaKey !== expected.command || event.shiftKey !== expected.shift
            || event.ctrlKey || event.altKey || event.repeat || marker.keyObserved) {
          marker.markerError = 'unexpected-owned-keydown'; return;
        }
        marker.keyObserved = true;
        marker.keyboardStages.push(expected.stage); marker.keyboardKeys.push(event.key);
        marker.nativeKeyboardTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.domEventOrder.push('key:' + expected.stage);
      }, true);
      document.addEventListener('input', event => {
        const path = event.composedPath(), expected = marker.expectedKey;
        if (!path.includes(field) || !path.includes(input)) return;
        if (!expected || (!expected.command && !marker.keyObserved) || event.isComposing
            || event.inputType !== expected.inputType || input.value !== expected.value || marker.inputStages.includes(expected.stage)) {
          marker.markerError = 'unexpected-owned-input'; return;
        }
        marker.inputStages.push(expected.stage); marker.inputTypes.push(event.inputType); marker.inputValues.push(input.value);
        marker.nativeInputTrustedEventCount += event.isTrusted === true ? 1 : 0;
        marker.domEventOrder.push('input:' + expected.stage);
      }, true);
    }
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {}, marker = window.VaadinDesktopNativeEdit || {};
      const scope = window.VaadinDesktopHostContext || {};
      const field = document.getElementById('framework-flow-native-edit-field'), input = field?.inputElement;
      const text = suffix => String(document.getElementById('framework-flow-native-edit-' + suffix)?.textContent || '').trim();
      const clients = Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript');
      const array = name => Array.isArray(marker[name]) ? marker[name] : [];
      let active = document.activeElement;
      while (active?.shadowRoot?.activeElement) active = active.shadowRoot.activeElement;
      return JSON.stringify({
        rootRouteAvailable: !!document.getElementById('framework-flow-native-edit-open-route'),
        routeReady: !!document.getElementById('framework-flow-native-edit-route') && !!input && text('config-status') === '\(config)'
          && !field.disabled && !field.readonly && !field.invalid && input.type === 'text' && field.label === 'Native edit text',
        firstMeaningfulPaint: diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0), responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0), flowClientCount: clients.length,
        browserWindowId: String(scope.windowId || ''), browserSessionId: String(scope.sessionId || ''), browserUiId: String(scope.uiId || ''),
        displayValue: String(input?.value || ''), fieldValue: String(field?.value || ''),
        configStatus: text('config-status'), valueStatus: text('value-status'), historyStatus: text('history-status'),
        invalid: field?.invalid === true, readOnly: field?.readonly === true, enabled: !!field && field.disabled !== true,
        inputMode: String(input?.inputMode || ''), inputType: String(input?.type || ''), label: String(field?.label || ''), ownerFocused: !!input && active === input,
        selectionStart: Number(input?.selectionStart ?? -1), selectionEnd: Number(input?.selectionEnd ?? -1),
        nativePointerEventCount: array('pointerStages').length, nativePointerTrustedEventCount: Number(marker.nativePointerTrustedEventCount || 0),
        nativeKeyboardEventCount: array('keyboardStages').length, nativeKeyboardTrustedEventCount: Number(marker.nativeKeyboardTrustedEventCount || 0),
        nativeInputEventCount: array('inputStages').length, nativeInputTrustedEventCount: Number(marker.nativeInputTrustedEventCount || 0),
        pointerStages: array('pointerStages'), keyboardStages: array('keyboardStages'), keyboardKeys: array('keyboardKeys'),
        inputStages: array('inputStages'), inputTypes: array('inputTypes'), inputValues: array('inputValues'), domEventOrder: array('domEventOrder'),
        browserProgrammaticStages: array('browserProgrammaticStages'), browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(diagnostics.standardBundleOpaqueRequestError || ''), responseError: String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || ''), markerError: String(marker.markerError || '')
      });
    })()
    """
}
