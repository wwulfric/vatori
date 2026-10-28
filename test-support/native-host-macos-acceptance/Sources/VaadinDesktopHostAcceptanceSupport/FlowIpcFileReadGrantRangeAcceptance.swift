import Cocoa
import Foundation

private var flowIpcFileReadGrantRangeAcceptance: FlowIpcFileReadGrantRangeAcceptance?

extension WebViewHost {
    func scheduleFileReadGrantRangeAcceptanceIfReady() {
        guard config.flowIpcAutoFileReadGrantRange,
              config.transport == .flowIpc else { return }
        if flowIpcFileReadGrantRangeAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 600
            flowIpcFileReadGrantRangeAcceptance = FlowIpcFileReadGrantRangeAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileReadGrantRangeAcceptance?.resume()
    }
}

/// Real native file selection followed by public typed API range operations.
/// Neither this driver nor its diagnostics ever reads the private grant token.
private final class FlowIpcFileReadGrantRangeAcceptance {
    private enum Phase: String { case root, route, click, panel, operation, drain }
    private static let prefix = "framework-flow-file-read-grant-range-"
    private static let panelTitle = "Flow file read grant bounded ranges"
    private static let byteCount = 65_573
    private static let stages = [
        "select", "negative", "zero", "oversized", "maximum",
        "tail", "eof", "beyond", "seek", "release"
    ]

    private struct ReadExpectation {
        let requestedOffset: Int
        let requestedMaxBytes: Int
        let bytesRead: Int
        let offset: Int
        let nextOffset: Int
        let endOfFile: Bool
        let invalid: Bool
        var outcome: String {
            invalid ? "INVALID_RANGE" : bytesRead > 0 ? "DATA" : "END_OF_FILE"
        }
    }

    private static let reads: [String: ReadExpectation] = [
        "negative": .init(requestedOffset: -1, requestedMaxBytes: 16,
            bytesRead: 0, offset: -1, nextOffset: -1, endOfFile: false, invalid: true),
        "zero": .init(requestedOffset: 0, requestedMaxBytes: 0,
            bytesRead: 0, offset: -1, nextOffset: -1, endOfFile: false, invalid: true),
        "oversized": .init(requestedOffset: 0, requestedMaxBytes: 65_537,
            bytesRead: 0, offset: -1, nextOffset: -1, endOfFile: false, invalid: true),
        "maximum": .init(requestedOffset: 0, requestedMaxBytes: 65_536,
            bytesRead: 65_536, offset: 0, nextOffset: 65_536, endOfFile: false, invalid: false),
        "tail": .init(requestedOffset: 65_536, requestedMaxBytes: 65_536,
            bytesRead: 37, offset: 65_536, nextOffset: 65_573, endOfFile: true, invalid: false),
        "eof": .init(requestedOffset: 65_573, requestedMaxBytes: 1,
            bytesRead: 0, offset: 65_573, nextOffset: 65_573, endOfFile: true, invalid: false),
        "beyond": .init(requestedOffset: 65_590, requestedMaxBytes: 16,
            bytesRead: 0, offset: 65_590, nextOffset: 65_590, endOfFile: true, invalid: false),
        "seek": .init(requestedOffset: 257, requestedMaxBytes: 113,
            bytesRead: 113, offset: 257, nextOffset: 370, endOfFile: false, invalid: false)
    ]

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var terminal = false
    private var pollScheduled = false
    private var pollInFlight = false
    private var fixtureURL: URL?
    private var fixtureCreated = false
    private var windowIdentity: ObjectIdentifier?
    private var webViewIdentity: ObjectIdentifier?
    private var initialPID: Int32 = 0
    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var mainPushBaseline = 0
    private var previousStageRequestCount = 0
    private var securityScoped: Bool?
    private var dispatchedStages: [String] = []
    private var receivedStages: [String] = []
    private var trustedStages: [String] = []
    private var observedStages: [String] = []
    private var currentStageIndex = 0
    private var routeNavigationObserved = false
    private var observedPanel: NSOpenPanel?
    private var panelURLVerified = false
    private var drainKey = ""
    private var drainPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        let root = ProcessInfo.processInfo.environment[
            "VAADIN_DESKTOP_FLOW_IPC_FILE_READ_GRANT_FIXTURE_ROOT"
        ].map { URL(fileURLWithPath: $0, isDirectory: true) }
            ?? FileManager.default.temporaryDirectory
        let directory = root.appendingPathComponent(
            "vatori-file-read-grant-range-\(UUID().uuidString.lowercased())",
            isDirectory: true)
        let file = directory.appendingPathComponent("vatori-file-read-grant-range-fixture.bin")
        do {
            var buffer: [UInt8] = []
            buffer.reserveCapacity(Self.byteCount)
            for index in 0..<Self.byteCount {
                let mixed = index * 31 + index / 251 + 17
                buffer.append(UInt8(mixed % 256))
            }
            let bytes: Data = Data(buffer)
            guard bytes.count == Self.byteCount else { return }
            try FileManager.default.createDirectory(
                at: directory, withIntermediateDirectories: false)
            try bytes.write(to: file, options: Data.WritingOptions.atomic)
            fixtureURL = file
            fixtureCreated = true
        } catch {
            // Generated files remain runner-owned until the host has exited.
            fixtureCreated = false
        }
    }

    func resume() {
        guard !pollInFlight else { return }
        schedule()
    }

    private func schedule(_ delay: Double = 0.1) {
        pollInFlight = false
        guard !terminal, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal, !pollInFlight else { return }
        pollInFlight = true
        guard Date() < deadline else { fail("fileReadGrantRangeDeadline"); return }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else { schedule(); return }
        if phase != .root && !retained(main) {
            fail("fileReadGrantRangeIdentityChanged"); return
        }
        inspect(main) { state in
            guard let state else { self.schedule(); return }
            guard state.errorFree, state.windowId == "main",
                  state.sessionId == "flow-ipc-session", state.uiId == "main" else {
                self.fail("fileReadGrantRangeScopeOrRuntimeError"); return
            }
            self.advance(main, state)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        switch phase {
        case .root:
            guard fixtureCreated, state.initialRootReady,
                  state.firstMeaningfulPaint, state.flowClientCount > 0, state.balanced,
                  main.recoveryGeneration == 0,
                  let process = host.sidecarProcessIdentifier, process > 0 else {
                schedule(); return
            }
            initialPID = process
            windowIdentity = ObjectIdentifier(main.window)
            webViewIdentity = ObjectIdentifier(main.webView)
            main.webView.evaluateJavaScript(Self.navigationScript) { value, error in
                guard error == nil, value as? Bool == true else { self.schedule(); return }
                self.routeNavigationObserved = true
                self.phase = .route
                self.schedule(0.3)
            }
        case .route:
            guard state.routeReady, state.allPending, state.balanced,
                  main.window.attachedSheet == nil, Self.panels.isEmpty else { schedule(); return }
            mainRequestBaseline = state.requestCount
            mainResponseBaseline = state.responseCount
            mainPushBaseline = state.pushMessageCount
            previousStageRequestCount = state.requestCount
            currentStageIndex = 0
            dispatchCurrent(main)
        case .click:
            guard state.routeReady else { schedule(); return }
            dispatchCurrent(main)
        case .panel:
            observePanel(main, state)
        case .operation:
            let stage = Self.stages[currentStageIndex]
            guard state.routeReady, record(stage, state),
                  statusMatches(state, stage),
                  observedStages.allSatisfy({ statusMatches(state, $0) }),
                  Self.stages.dropFirst(currentStageIndex + 1).allSatisfy({
                    state.status($0).hasSuffix(": pending")
                  }),
                  main.window.attachedSheet == nil, Self.panels.isEmpty,
                  state.balanced, state.requestCount > previousStageRequestCount else {
                resetDrain(); schedule(); return
            }
            if stage == "select" {
                securityScoped = Self.bool("securityScoped", state.status(stage))
            }
            let key = "\(stage):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            emitStage(main, state, stage)
            observedStages.append(stage)
            previousStageRequestCount = state.requestCount
            resetDrain()
            if currentStageIndex == Self.stages.count - 1 {
                phase = .drain
                schedule(0.2)
            } else {
                currentStageIndex += 1
                dispatchCurrent(main)
            }
        case .drain:
            guard state.routeReady, state.balanced,
                  observedStages == Self.stages,
                  observedStages.allSatisfy({ statusMatches(state, $0) }),
                  main.window.attachedSheet == nil, Self.panels.isEmpty else {
                resetDrain(); schedule(); return
            }
            let key = "final:\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            finish(main, state)
        }
    }

    private func dispatchCurrent(_ main: FlowIpcWindowContext) {
        // Failed geometry preparation retries the still-undispatched stage,
        // rather than polling for a result from a click that never happened.
        phase = .click
        let stage = Self.stages[currentStageIndex]
        click(main, stage, stage, currentStageIndex == 0 ? .panel : .operation)
    }

    private func retained(_ main: FlowIpcWindowContext) -> Bool {
        windowIdentity == ObjectIdentifier(main.window)
            && webViewIdentity == ObjectIdentifier(main.webView)
            && main.recoveryGeneration == 0 && host.sidecarProcessIdentifier == initialPID
            && main.scope.windowId == "main" && main.scope.sessionId == "flow-ipc-session"
            && main.scope.uiId == "main"
    }

    private func inspect(_ main: FlowIpcWindowContext,
                         _ completion: @escaping (Snapshot?) -> Void) {
        main.webView.evaluateJavaScript(Self.inspectionScript) { value, error in
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                completion(nil); return
            }
            completion(state)
        }
    }

    private func observePanel(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        guard Self.field("outcome", state.status("select")) == nil else {
            fail("fileReadGrantRangePanelCompletedBeforeObservation"); return
        }
        guard record("select", state), let file = fixtureURL,
              let panel = main.window.attachedSheet as? NSOpenPanel,
              panel.title == Self.panelTitle, panel.sheetParent === main.window,
              panel.isVisible, Self.panels.count == 1,
              Self.panels.first === panel else { schedule(0.05); return }
        if let previous = observedPanel, previous !== panel {
            fail("fileReadGrantRangeUnexpectedPanelReplacement"); return
        }
        if observedPanel == nil {
            observedPanel = panel
            trace("recv", "flowFileReadGrantRange.panelReady", [
                "stage": "file", "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        }
        // Passive observation only; an external native UI driver or operator
        // selects this generated file and confirms the actual Open control.
        guard panel.url?.resolvingSymlinksInPath().standardizedFileURL
                == file.resolvingSymlinksInPath().standardizedFileURL else {
            schedule(0.05); return
        }
        panelURLVerified = true
        trace("recv", "flowFileReadGrantRange.panelObserved", [
            "complete": true, "stage": "file", "title": Self.panelTitle,
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true, "inputMode": "external-native-panel-selection",
            "physicalInput": false, "windowId": main.scope.windowId,
            "sessionId": main.scope.sessionId, "uiId": main.scope.uiId
        ])
        phase = .operation
        schedule()
    }

    private func matches(_ state: Snapshot, _ stage: String,
                         _ expected: [String: String]) -> Bool {
        expected.allSatisfy { Self.field($0.key, state.status(stage)) == $0.value }
    }

    private func statusMatches(_ state: Snapshot, _ stage: String) -> Bool {
        guard matches(state, stage, [
            "client": "true", "change": "1", "capabilityContextCaptured": "true",
            "backgroundSessionAbsent": "true", "publicTokenHidden": "true",
            "permissionAllowed": "true", "grantPresent": "true",
            "byteCount": String(Self.byteCount), "maxReadBytes": "65536", "filenameMatches": "true"
        ]), let scoped = Self.bool("securityScoped", state.status(stage)),
              securityScoped == nil || scoped == securityScoped else { return false }
        if stage == "select" {
            return matches(state, stage, ["outcome": "SELECTED", "noPath": "true"])
        }
        if stage == "release" {
            return matches(state, stage, [
                "outcome": "RELEASED", "released": "true", "sameGrantFacade": "true",
                "securityScopeStopped": String(scoped)
            ])
        }
        guard let read = Self.reads[stage] else { return false }
        return matches(state, stage, [
            "outcome": read.outcome, "sameGrantFacade": "true", "grantActive": "true",
            "requestedOffset": String(read.requestedOffset),
            "requestedMaxBytes": String(read.requestedMaxBytes),
            "bytesRead": String(read.bytesRead), "offset": String(read.offset),
            "nextOffset": String(read.nextOffset), "eof": String(read.endOfFile),
            "contentMatches": String(!read.invalid), "noContent": String(read.invalid)
        ])
    }

    private func emitStage(_ main: FlowIpcWindowContext, _ state: Snapshot, _ stage: String) {
        var payload: [String: Any] = [
            "complete": true, "stage": stage,
            "outcome": Self.field("outcome", state.status(stage)) ?? "",
            "client": true, "change": 1, "capabilityContextCaptured": true,
            "backgroundSessionAbsent": true, "publicTokenHidden": true, "permissionAllowed": true,
            "grantPresent": true, "byteCount": Self.byteCount, "maxReadBytes": 65_536,
            "securityScoped": securityScoped ?? false, "filenameMatches": true,
            "processIdentifier": host.sidecarProcessIdentifier ?? 0,
            "windowId": main.scope.windowId, "sessionId": main.scope.sessionId, "uiId": main.scope.uiId,
            "requestCount": state.requestCount, "responseCount": state.responseCount,
            "pushCount": state.pushMessageCount, "balancedDrainObserved": drainPolls >= 2,
            "nativePointerObserved": true, "nativePointerTrustedObserved": true
        ]
        if let read = Self.reads[stage] {
            payload.merge([
                "requestedOffset": read.requestedOffset, "requestedMaxBytes": read.requestedMaxBytes,
                "bytesRead": read.bytesRead, "offset": read.offset, "nextOffset": read.nextOffset,
                "endOfFile": read.endOfFile, "sameGrantFacade": true, "grantActive": true,
                "contentMatches": !read.invalid, "noContent": read.invalid
            ]) { _, value in value }
        } else if stage == "select" {
            payload["noPath"] = true
        } else {
            payload["released"] = true
            payload["sameGrantFacade"] = true
            payload["securityScopeStopped"] = securityScoped ?? false
        }
        trace("recv", "flowFileReadGrantRange.stageObserved", payload)
    }

    private func quietPoll(_ key: String) -> Bool {
        drainPolls = key == drainKey ? drainPolls + 1 : 1
        drainKey = key
        return drainPolls >= 2
    }

    private func resetDrain() { drainKey = ""; drainPolls = 0 }

    private static var panels: [NSOpenPanel] {
        NSApplication.shared.windows.compactMap { $0 as? NSOpenPanel }.filter(\.isVisible)
    }

    private static func field(_ name: String, _ status: String) -> String? {
        let prefix = name + "="
        return status.split(separator: " ").first { $0.hasPrefix(prefix) }
            .map { String($0.dropFirst(prefix.count)) }
    }

    private static func bool(_ name: String, _ status: String) -> Bool? {
        switch field(name, status) {
        case "true": return true
        case "false": return false
        default: return nil
        }
    }

    @discardableResult
    private func record(_ stage: String, _ state: Snapshot) -> Bool {
        guard let pointer = state.pointerStages[stage] else { return false }
        if pointer.receipt && !receivedStages.contains(stage) { receivedStages.append(stage) }
        if pointer.trusted && !trustedStages.contains(stage) { trustedStages.append(stage) }
        return pointer.receipt && pointer.trusted
    }

    private func fail(_ code: String) {
        guard !terminal else { return }
        terminal = true
        trace("send", "error", [
            "code": code, "phase": phase.rawValue, "stage": Self.stages[currentStageIndex]
        ])
        host.flowIpcWindowContext("main")?.window.close()
    }

    private func finish(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        let pointers = dispatchedStages == Self.stages && receivedStages == Self.stages
        let trusted = trustedStages == Self.stages
        let statuses = observedStages == Self.stages
        let complete = statuses && pointers && trusted && retained(main)
            && drainPolls >= 2 && panelURLVerified && securityScoped != nil
            && routeNavigationObserved && state.errorFree && state.balanced
            && state.pushMessageCount > mainPushBaseline
        trace("recv", "flowFileReadGrantRange.frameworkAcceptance", [
            "complete": complete, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.rootRendered, "flowClientOwned": state.flowClientCount > 0,
            "fixtureCreated": fixtureCreated, "fixtureByteCount": Self.byteCount, "maxReadBytes": 65_536,
            "allStageStatusesObserved": statuses, "observedStages": observedStages.joined(separator: ","),
            "stageCount": observedStages.count, "typedGrantApiObserved": statuses,
            "permissionAllowedObserved": statuses, "sameGrantFacadeObserved": statuses,
            "publicTokenHiddenObserved": statuses, "clientEventsObserved": statuses,
            "invalidRangesRejectedObserved": observedStages.contains("oversized"),
            "grantRetainedAfterInvalidRanges": observedStages.contains("maximum"),
            "maximumReadObserved": observedStages.contains("maximum"),
            "tailReadObserved": observedStages.contains("tail"),
            "exactEofObserved": observedStages.contains("eof"),
            "beyondEofObserved": observedStages.contains("beyond"),
            "nonzeroSeekObserved": observedStages.contains("seek"),
            "releaseObserved": observedStages.contains("release"), "contentMatches": statuses,
            "securityScopedValueObserved": securityScoped != nil,
            "securityScopedObserved": securityScoped ?? false,
            "initialProcessIdentifier": initialPID,
            "currentProcessIdentifier": host.sidecarProcessIdentifier ?? 0,
            "windowRetained": retained(main), "webViewRetained": retained(main),
            "mainWindowId": state.windowId, "mainSessionId": state.sessionId, "mainUiId": state.uiId,
            "mainRequestBaseline": mainRequestBaseline, "mainResponseBaseline": mainResponseBaseline,
            "mainRequestCount": state.requestCount, "mainResponseCount": state.responseCount,
            "mainPushBaseline": mainPushBaseline, "mainPushCount": state.pushMessageCount,
            "mainUidlRoundTripObserved": state.requestCount > mainRequestBaseline,
            "opaqueRequestObserved": state.requestCount > mainRequestBaseline,
            "opaqueResponseObserved": state.responseCount > mainResponseBaseline,
            "opaquePushObserved": state.pushMessageCount > mainPushBaseline,
            "perStageBalancedDrainObserved": statuses, "balancedDrainObserved": drainPolls >= 2,
            "resultStableAfterDrain": complete,
            "realOpenPanelObserved": observedPanel != nil, "panelCount": observedPanel == nil ? 0 : 1,
            "panelsAttachedObserved": observedPanel != nil, "panelsVisibleObserved": observedPanel != nil,
            "panelURLsVerified": panelURLVerified, "externalPanelSelection": panelURLVerified,
            "externalPanelSelectionStages": "file", "nativePointerObserved": pointers,
            "nativePointerTrustedObserved": trusted, "nativePointerEventCount": dispatchedStages.count,
            "nativePointerTrustedEventCount": trustedStages.count,
            "nativePointerStages": dispatchedStages.joined(separator: ","),
            "browserProgrammaticSetupObserved": routeNavigationObserved,
            "browserProgrammaticStages": "route-navigate", "physicalInput": false, "sandboxEnforced": false,
            "errorFree": state.errorFree, "gracefulCloseRequested": true,
            "recoveryGeneration": main.recoveryGeneration
        ])
        terminal = true
        main.window.close()
    }

    private struct PointerGeometry: Decodable {
        let ready: Bool
        let centerX: Double
        let centerY: Double
    }

    private struct PointerObservation: Decodable {
        let receipt: Bool
        let trusted: Bool
    }

    private struct Snapshot: Decodable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let rootRendered: Bool
        let initialRootReady: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let statuses: [String: String]
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let pointerStages: [String: PointerObservation]
        let errorFree: Bool
        var balanced: Bool { requestCount == responseCount }
        func status(_ operation: String) -> String { statuses[operation] ?? "" }
        var allPending: Bool {
            statuses.count == 10 && statuses.values.allSatisfy { $0.hasSuffix(": pending") }
        }
    }

    private func click(_ context: FlowIpcWindowContext, _ button: String,
                       _ stage: String, _ next: Phase) {
        context.webView.evaluateJavaScript(Self.pointerScript(button, stage)) { value, error in
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(PointerGeometry.self, from: data),
                  point.ready else { self.schedule(); return }
            FlowIpcNativeInputActivation.prepare(context: context, deadline: self.deadline,
                onFailure: { _ in self.fail("fileReadGrantRangeActivationFailed") }) {
                let location = context.webView.convert(NSPoint(x: point.centerX,
                    y: context.webView.isFlipped ? point.centerY
                        : context.webView.bounds.height - point.centerY), to: nil)
                guard context.window.contentLayoutRect.contains(location) else {
                    self.fail("fileReadGrantRangePointerOutsideWindow"); return
                }
                let number = Int(Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location,
                    modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
                    windowNumber: context.window.windowNumber, context: nil,
                    eventNumber: number, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location,
                    modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime + 0.05,
                    windowNumber: context.window.windowNumber, context: nil,
                    eventNumber: number + 1, clickCount: 1, pressure: 0) else {
                    self.fail("fileReadGrantRangePointerCreationFailed"); return
                }
                trace("recv", "flowFileReadGrantRange.nativePointerDispatch", [
                    "inputMode": "appkit-window-pointer-event", "button": "primary", "stage": stage,
                    "webViewX": point.centerX, "webViewY": point.centerY,
                    "windowX": location.x, "windowY": location.y,
                    "activeApplication": NSApplication.shared.isActive,
                    "keyWindow": context.window.isKeyWindow,
                    "windowId": context.scope.windowId, "sessionId": context.scope.sessionId,
                    "uiId": context.scope.uiId
                ])
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    context.window.sendEvent(up)
                    self.dispatchedStages.append(stage)
                    self.phase = next
                    self.schedule()
                }
            }
        }
    }

    private static func pointerScript(_ button: String, _ stage: String) -> String {
        """
        (() => {
          const button = document.querySelector('#\(prefix)\(button)');
          if (!button) return JSON.stringify({ready:false});
          button.scrollIntoView({block:'center',inline:'center'});
          const rect = button.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2, centerY = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(centerX,centerY);
          if (!(rect.width > 0 && rect.height > 0) || !(hit === button || button.contains(hit))) {
            return JSON.stringify({ready:false,centerX,centerY});
          }
          const marker = window.VatoriGrantRangePointer ||= {stages:{},pending:{}};
          if (marker.pending['\(stage)']) document.removeEventListener('click',marker.pending['\(stage)'],true);
          marker.stages['\(stage)'] = {receipt:false,trusted:false};
          const handler = event => {
            if (event.button !== 0 || !event.composedPath().includes(button)) return;
            marker.stages['\(stage)'] = {receipt:true,trusted:event.isTrusted === true};
            document.removeEventListener('click',handler,true);
            delete marker.pending['\(stage)'];
          };
          marker.pending['\(stage)'] = handler;
          document.addEventListener('click',handler,true);
          return JSON.stringify({ready:true,centerX,centerY});
        })()
        """
    }

    private static let navigationScript = """
    (() => {
      const button = document.querySelector('#framework-flow-file-read-grant-range-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const d = window.VaadinDesktopIpcDiagnostics || {};
      const c = window.VaadinDesktopHostContext || {};
      const prefix = 'framework-flow-file-read-grant-range-';
      const route = document.querySelector('#'+prefix+'route');
      const operations = ['select','negative','zero','oversized','maximum','tail','eof','beyond','seek','release'];
      const statuses = Object.fromEntries(operations.map(name => [name,
        String(document.querySelector('#'+prefix+name+'-status')?.textContent || '')]));
      return JSON.stringify({
        windowId:String(c.windowId || ''),sessionId:String(c.sessionId || ''),uiId:String(c.uiId || ''),
        rootRendered:!!document.querySelector('#framework-root') || !!route,
        initialRootReady:!!document.querySelector('#framework-root') && !route,
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:!!route && window.location.pathname.endsWith('/flow-file-read-grant-range')
          && operations.every(name => !!document.querySelector('#'+prefix+name)),
        statuses,
        requestCount:Number(d.standardBundleOpaqueRequestCount || 0),
        responseCount:Number(d.standardBundleOpaqueResponseCount || 0),
        pushMessageCount:Number(d.standardBundlePushMessageCount || 0),
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript').length,
        pointerStages:window.VatoriGrantRangePointer?.stages || {},
        errorFree:!d.standardBundleBrowserError && !d.standardBundleOpaqueRequestError
          && !d.standardBundleOpaqueResponseError && !d.standardBundlePushError
      });
    })()
    """
}
