import Cocoa
import Foundation

private var flowIpcFileReadGrantIsolationAcceptance:
    FlowIpcFileReadGrantIsolationAcceptance?

extension WebViewHost {
    func scheduleFileReadGrantIsolationAcceptanceIfReady() {
        guard config.flowIpcAutoFileReadGrantIsolation,
              config.transport == .flowIpc else { return }
        if flowIpcFileReadGrantIsolationAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 600
            flowIpcFileReadGrantIsolationAcceptance =
                FlowIpcFileReadGrantIsolationAcceptance(
                    host: self, deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileReadGrantIsolationAcceptance?.resume()
    }
}

/// Test-only choreography. The token is never inspected by this driver: the
/// external Java fixture exercises the public API and reports semantic results.
private final class FlowIpcFileReadGrantIsolationAcceptance {
    private enum Phase: String {
        case root, route, opening, selecting, selected
        case foreignRead, foreignRelease, ownerRead, ownerRelease, closing, drain
    }

    private static let prefix = "framework-flow-file-read-grant-isolation-"
    private static let secondaryId = "file-read-grant-isolation-secondary"
    private static let panelTitle = "Flow file read grant owner isolation"
    private static let fixtureFilename = "vatori-file-read-grant-fixture.txt"
    private static let stages = [
        "main-open-secondary", "secondary-select", "main-foreign-read",
        "main-foreign-release", "secondary-owner-read",
        "secondary-owner-release", "main-close-secondary"
    ]

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var terminal = false
    private var pollScheduled = false
    private var pollInFlight = false
    private var fixtureURL: URL?
    private var fixtureCreated = false
    private var mainWindowIdentity: ObjectIdentifier?
    private var mainWebViewIdentity: ObjectIdentifier?
    private var ownerWindowIdentity: ObjectIdentifier?
    private var ownerWebViewIdentity: ObjectIdentifier?
    private weak var ownerWindow: NSWindow?
    private var ownerSession = ""
    private var initialPID: Int32 = 0
    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var ownerRequestBaseline = 0
    private var ownerResponseBaseline = 0
    private var ownerPushBaseline = 0
    private var ownerFinalRequestCount = 0
    private var ownerFinalResponseCount = 0
    private var ownerFinalPushCount = 0
    private var securityScoped: Bool?
    private var dispatchedStages: [String] = []
    private var receivedStages: [String] = []
    private var trustedStages: [String] = []
    private var routeNavigationObserved = false
    private var observedPanel: NSOpenPanel?
    private var panelURLVerified = false
    private var selectionObserved = false
    private var foreignReadObserved = false
    private var foreignReleaseObserved = false
    private var ownerReadObserved = false
    private var ownerReleaseObserved = false
    private var secondaryRenderingObserved = false
    private var liveContextsRetained = false
    private var liveDrainObserved = false
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
            "vatori-file-read-grant-\(UUID().uuidString.lowercased())",
            isDirectory: true)
        let file = directory.appendingPathComponent(Self.fixtureFilename)
        do {
            let bytes = Data("vatori-file-read-grant-fixture::0123456789::tail".utf8)
            guard bytes.count == 48 else { return }
            try FileManager.default.createDirectory(
                at: directory, withIntermediateDirectories: false)
            try bytes.write(to: file, options: .atomic)
            fixtureURL = file
            fixtureCreated = true
        } catch {
            // The runner owns this generated root and removes it after exit.
            fixtureCreated = false
        }
    }

    func resume() {
        // Navigation notifications may arrive while an inspection or native
        // activation is outstanding; never dispatch the same action twice.
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
        guard Date() < deadline else {
            fail("fileReadGrantIsolationDeadline")
            return
        }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else { schedule(); return }
        if phase != .root && !mainRetained(main) {
            fail("fileReadGrantIsolationMainIdentityChanged")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot else { self.schedule(); return }
            guard snapshot.errorFree else {
                self.fail("fileReadGrantIsolationMainRuntimeError")
                return
            }
            guard self.mainScope(snapshot) else {
                self.fail("fileReadGrantIsolationMainScopeChanged")
                return
            }
            self.advance(main, snapshot)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ mainState: Snapshot) {
        switch phase {
        case .root:
            guard fixtureCreated, mainState.initialRootReady,
                  mainState.firstMeaningfulPaint, mainState.flowClientCount > 0,
                  mainState.balanced, main.recoveryGeneration == 0,
                  let process = host.sidecarProcessIdentifier, process > 0 else {
                schedule(); return
            }
            initialPID = process
            mainWindowIdentity = ObjectIdentifier(main.window)
            mainWebViewIdentity = ObjectIdentifier(main.webView)
            mainRequestBaseline = mainState.requestCount
            mainResponseBaseline = mainState.responseCount
            main.webView.evaluateJavaScript(Self.navigationScript) { value, error in
                guard error == nil, value as? Bool == true else {
                    self.schedule(); return
                }
                self.routeNavigationObserved = true
                self.phase = .route
                self.schedule(0.3)
            }
        case .route:
            guard mainState.routeReady, mainState.allPending else {
                schedule(); return
            }
            click(main, "open-secondary", "main-open-secondary", .opening)
        case .opening:
            guard record("main-open-secondary", mainState),
                  matches(mainState, "open-secondary", [
                    "opened": "true", "client": "true", "change": "1"
                  ]) else { schedule(); return }
            guard let owner = host.flowIpcWindowContext(Self.secondaryId),
                  owner.navigationFinished else { schedule(); return }
            inspect(owner) { state in
                guard let state, state.errorFree, state.routeReady,
                      state.firstMeaningfulPaint, state.flowClientCount > 0,
                      state.allPending, state.balanced,
                      state.windowId == Self.secondaryId,
                      state.uiId == Self.secondaryId,
                      self.opaqueSession(state.sessionId),
                      state.sessionId != mainState.sessionId,
                      owner.recoveryGeneration == 0,
                      owner.window.attachedSheet == nil,
                      Self.panels.isEmpty else { self.schedule(); return }
                self.ownerSession = state.sessionId
                self.secondaryRenderingObserved = true
                self.ownerWindow = owner.window
                self.ownerWindowIdentity = ObjectIdentifier(owner.window)
                self.ownerWebViewIdentity = ObjectIdentifier(owner.webView)
                self.ownerRequestBaseline = state.requestCount
                self.ownerResponseBaseline = state.responseCount
                self.ownerPushBaseline = state.pushMessageCount
                self.click(owner, "select", "secondary-select", .selecting)
            }
        case .selecting:
            withOwner { owner, state in self.observePanel(owner, state) }
        case .selected:
            withOwner { owner, state in
                guard self.selectionMatches(state),
                      owner.window.attachedSheet == nil, Self.panels.isEmpty,
                      self.ownerOperationsPending(state),
                      self.foreignOperationsPending(mainState) else {
                    self.schedule(); return
                }
                self.securityScoped = Self.bool("securityScoped", state.status("select"))
                self.selectionObserved = true
                self.click(main, "foreign-read", "main-foreign-read", .foreignRead)
            }
        case .foreignRead:
            withOwner { _, state in
                guard self.selectionMatches(state), self.ownerOperationsPending(state),
                      self.record("main-foreign-read", mainState),
                      self.foreignMatches(mainState, "foreign-read"),
                      mainState.status("foreign-release").hasSuffix(": pending") else {
                    self.schedule(); return
                }
                self.foreignReadObserved = true
                self.click(main, "foreign-release", "main-foreign-release", .foreignRelease)
            }
        case .foreignRelease:
            withOwner { owner, state in
                guard self.selectionMatches(state), self.ownerOperationsPending(state),
                      self.foreignMatches(mainState, "foreign-read"),
                      self.record("main-foreign-release", mainState),
                      self.foreignMatches(mainState, "foreign-release") else {
                    self.schedule(); return
                }
                self.foreignReleaseObserved = true
                self.click(owner, "owner-read", "secondary-owner-read", .ownerRead)
            }
        case .ownerRead:
            withOwner { owner, state in
                guard self.selectionMatches(state),
                      self.foreignMatches(mainState, "foreign-read"),
                      self.foreignMatches(mainState, "foreign-release"),
                      self.record("secondary-owner-read", state),
                      self.ownerReadMatches(state),
                      state.status("owner-release").hasSuffix(": pending") else {
                    self.schedule(); return
                }
                self.ownerReadObserved = true
                self.click(owner, "owner-release", "secondary-owner-release", .ownerRelease)
            }
        case .ownerRelease:
            withOwner { _, state in
                guard self.allResultsMatch(mainState, state),
                      self.record("secondary-owner-release", state),
                      mainState.balanced, state.balanced,
                      mainState.requestCount > self.mainRequestBaseline,
                      state.requestCount > self.ownerRequestBaseline,
                      state.pushMessageCount > self.ownerPushBaseline else {
                    self.resetDrain(); self.schedule(); return
                }
                self.ownerReleaseObserved = true
                let key = "live:\(mainState.requestCount):\(mainState.responseCount)"
                    + ":\(mainState.pushMessageCount):\(state.requestCount)"
                    + ":\(state.responseCount):\(state.pushMessageCount)"
                guard self.quietPoll(key) else { self.schedule(0.2); return }
                self.liveContextsRetained = true
                self.liveDrainObserved = true
                self.ownerFinalRequestCount = state.requestCount
                self.ownerFinalResponseCount = state.responseCount
                self.ownerFinalPushCount = state.pushMessageCount
                self.resetDrain()
                self.click(main, "close-secondary", "main-close-secondary", .closing)
            }
        case .closing, .drain:
            guard record("main-close-secondary", mainState),
                  matches(mainState, "close-secondary", [
                    "closed": "true", "client": "true", "change": "1",
                    "sharedStateCleared": "true"
                  ]),
                  foreignMatches(mainState, "foreign-read"),
                  foreignMatches(mainState, "foreign-release"),
                  host.flowIpcWindowContext(Self.secondaryId) == nil,
                  ownerWindow == nil || ownerWindow?.isVisible == false,
                  mainState.balanced, Self.panels.isEmpty else {
                resetDrain(); schedule(); return
            }
            phase = .drain
            let key = "final:\(mainState.requestCount):\(mainState.responseCount)"
                + ":\(mainState.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            finish(main, mainState)
        }
    }

    private func withOwner(_ body: @escaping (FlowIpcWindowContext, Snapshot) -> Void) {
        guard let owner = host.flowIpcWindowContext(Self.secondaryId),
              ownerWindowIdentity == ObjectIdentifier(owner.window),
              ownerWebViewIdentity == ObjectIdentifier(owner.webView),
              owner.recoveryGeneration == 0,
              owner.scope.windowId == Self.secondaryId,
              owner.scope.uiId == Self.secondaryId,
              owner.scope.sessionId == ownerSession,
              owner.window.isVisible || owner.window.isMiniaturized else {
            fail("fileReadGrantIsolationOwnerIdentityChanged"); return
        }
        inspect(owner) { state in
            guard let state else { self.schedule(); return }
            guard state.errorFree, state.routeReady,
                  state.windowId == Self.secondaryId,
                  state.uiId == Self.secondaryId,
                  state.sessionId == self.ownerSession else {
                self.fail("fileReadGrantIsolationOwnerScopeOrRuntimeError"); return
            }
            body(owner, state)
        }
    }

    private func mainRetained(_ context: FlowIpcWindowContext) -> Bool {
        mainWindowIdentity == ObjectIdentifier(context.window)
            && mainWebViewIdentity == ObjectIdentifier(context.webView)
            && context.recoveryGeneration == 0
            && host.sidecarProcessIdentifier == initialPID
    }

    private func mainScope(_ state: Snapshot) -> Bool {
        state.windowId == "main" && state.sessionId == "flow-ipc-session"
            && state.uiId == "main"
    }

    private func opaqueSession(_ session: String) -> Bool {
        let prefix = "flow-ipc-window:"
        return session.hasPrefix(prefix)
            && UUID(uuidString: String(session.dropFirst(prefix.count))) != nil
    }

    private func inspect(_ context: FlowIpcWindowContext,
                         _ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { value, error in
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                completion(nil); return
            }
            completion(state)
        }
    }

    private func observePanel(_ owner: FlowIpcWindowContext, _ state: Snapshot) {
        guard Self.field("outcome", state.status("select")) == nil else {
            fail("fileReadGrantIsolationPanelCompletedBeforeObservation"); return
        }
        guard record("secondary-select", state), let file = fixtureURL,
              let panel = owner.window.attachedSheet as? NSOpenPanel,
              panel.title == Self.panelTitle, panel.sheetParent === owner.window,
              panel.isVisible, Self.panels.count == 1,
              Self.panels.first === panel else { schedule(0.05); return }
        if let previous = observedPanel, previous !== panel {
            fail("fileReadGrantIsolationUnexpectedPanelReplacement"); return
        }
        if observedPanel == nil {
            observedPanel = panel
            trace("recv", "flowFileReadGrantIsolation.panelReady", [
                "stage": "owner", "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        }
        // Only observe. The external system UI must select the generated file
        // and activate the real Open control after this diagnostic is emitted.
        guard panel.url?.resolvingSymlinksInPath().standardizedFileURL
                == file.resolvingSymlinksInPath().standardizedFileURL else {
            schedule(0.05); return
        }
        panelURLVerified = true
        trace("recv", "flowFileReadGrantIsolation.panelObserved", [
            "complete": true, "stage": "owner", "title": Self.panelTitle,
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true,
            "inputMode": "external-native-panel-selection", "physicalInput": false,
            "windowId": owner.scope.windowId, "sessionId": owner.scope.sessionId,
            "uiId": owner.scope.uiId
        ])
        phase = .selected
        schedule()
    }

    private func matches(_ state: Snapshot, _ operation: String,
                         _ expected: [String: String]) -> Bool {
        let status = state.status(operation)
        return expected.allSatisfy { Self.field($0.key, status) == $0.value }
    }

    private func callbackMatches(_ state: Snapshot, _ operation: String) -> Bool {
        matches(state, operation, [
            "client": "true", "change": "1", "capabilityContextCaptured": "true",
            "backgroundSessionAbsent": "true", "permissionAllowed": "true"
        ])
    }

    private func selectionMatches(_ state: Snapshot) -> Bool {
        callbackMatches(state, "select") && matches(state, "select", [
            "outcome": "SELECTED", "grantPresent": "true", "byteCount": "48",
            "maxReadBytes": "65536", "noPath": "true"
        ]) && Self.bool("securityScoped", state.status("select")) != nil
            && (securityScoped == nil
                || Self.bool("securityScoped", state.status("select")) == securityScoped)
    }

    private func foreignMatches(_ state: Snapshot, _ operation: String) -> Bool {
        callbackMatches(state, operation) && matches(state, operation, [
            "outcome": "UNAVAILABLE", "unavailable": "true", "grantUnavailable": "true",
            "noContent": "true", "sameTokenUsed": "true", "ownerTokenUnchanged": "true"
        ])
    }

    private func ownerReadMatches(_ state: Snapshot) -> Bool {
        callbackMatches(state, "owner-read") && matches(state, "owner-read", [
            "outcome": "DATA", "success": "true", "sameTokenUsed": "true",
            "requestedOffset": "0", "requestedBytes": "19", "bytesRead": "19",
            "offset": "0", "nextOffset": "19", "eof": "false", "contentMatches": "true"
        ])
    }

    private func ownerReleaseMatches(_ state: Snapshot) -> Bool {
        callbackMatches(state, "owner-release") && matches(state, "owner-release", [
            "outcome": "RELEASED", "success": "true", "sameTokenUsed": "true",
            "released": "true"
        ])
    }

    private func allResultsMatch(_ main: Snapshot, _ owner: Snapshot) -> Bool {
        selectionMatches(owner) && foreignMatches(main, "foreign-read")
            && foreignMatches(main, "foreign-release") && ownerReadMatches(owner)
            && ownerReleaseMatches(owner) && foreignOperationsPending(owner)
            && ownerOperationsPending(main) && main.status("select").hasSuffix(": pending")
    }

    private func ownerOperationsPending(_ state: Snapshot) -> Bool {
        state.status("owner-read").hasSuffix(": pending")
            && state.status("owner-release").hasSuffix(": pending")
    }

    private func foreignOperationsPending(_ state: Snapshot) -> Bool {
        state.status("foreign-read").hasSuffix(": pending")
            && state.status("foreign-release").hasSuffix(": pending")
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
        trace("send", "error", ["code": code, "phase": phase.rawValue])
        host.flowIpcWindowContext(Self.secondaryId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        let pointers = dispatchedStages == Self.stages && receivedStages == Self.stages
        let trusted = trustedStages == Self.stages
        let statuses = selectionObserved && foreignReadObserved && foreignReleaseObserved
            && ownerReadObserved && ownerReleaseObserved
        let retained = liveContextsRetained && mainRetained(main)
        let complete = statuses && pointers && trusted && retained && liveDrainObserved
            && drainPolls >= 2 && panelURLVerified && securityScoped != nil
            && routeNavigationObserved && secondaryRenderingObserved
        trace("recv", "flowFileReadGrantIsolation.frameworkAcceptance", [
            "complete": complete, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.rootRendered, "flowClientOwned": state.flowClientCount > 0,
            "secondaryRenderingObserved": secondaryRenderingObserved,
            "fixtureCreated": fixtureCreated, "fixtureByteCount": 48,
            "selectionStatusObserved": selectionObserved,
            "foreignReadStatusObserved": foreignReadObserved,
            "foreignReleaseStatusObserved": foreignReleaseObserved,
            "ownerReadStatusObserved": ownerReadObserved,
            "ownerReleaseStatusObserved": ownerReleaseObserved,
            "selectionOutcome": "SELECTED", "foreignReadOutcome": "UNAVAILABLE",
            "foreignReleaseOutcome": "UNAVAILABLE", "ownerReadOutcome": "DATA",
            "ownerReleaseOutcome": "RELEASED", "ownerReadBytes": 19,
            "contentMatches": ownerReadObserved,
            "sameTokenUsedObserved": statuses, "permissionAllowedObserved": statuses,
            "foreignNoContentObserved": foreignReadObserved && foreignReleaseObserved,
            "ownerTokenUnchangedObserved": foreignReadObserved && foreignReleaseObserved,
            "securityScopedValueObserved": securityScoped != nil,
            "securityScopedObserved": securityScoped ?? false,
            "scopeIsolatedObserved": opaqueSession(ownerSession),
            "initialProcessIdentifier": initialPID,
            "currentProcessIdentifier": host.sidecarProcessIdentifier ?? 0,
            "windowRetained": mainRetained(main), "webViewRetained": mainRetained(main),
            "secondaryWindowRetained": liveContextsRetained,
            "secondaryWebViewRetained": liveContextsRetained,
            "liveOwnerRetainedThroughRelease": liveContextsRetained,
            "mainWindowId": state.windowId, "mainSessionId": state.sessionId,
            "mainUiId": state.uiId, "secondaryWindowId": Self.secondaryId,
            "secondarySessionId": ownerSession, "secondaryUiId": Self.secondaryId,
            "mainRequestBaseline": mainRequestBaseline,
            "mainResponseBaseline": mainResponseBaseline,
            "mainRequestCount": state.requestCount, "mainResponseCount": state.responseCount,
            "secondaryRequestBaseline": ownerRequestBaseline,
            "secondaryResponseBaseline": ownerResponseBaseline,
            "secondaryPushBaseline": ownerPushBaseline,
            "secondaryRequestCount": ownerFinalRequestCount,
            "secondaryResponseCount": ownerFinalResponseCount,
            "secondaryPushCount": ownerFinalPushCount,
            "mainUidlRoundTripObserved": state.requestCount > mainRequestBaseline,
            "secondaryUidlRoundTripObserved": ownerFinalRequestCount > ownerRequestBaseline,
            "opaqueRequestObserved": state.requestCount > mainRequestBaseline
                && ownerFinalRequestCount > ownerRequestBaseline,
            "opaqueResponseObserved": state.responseCount > mainResponseBaseline
                && ownerFinalResponseCount > ownerResponseBaseline,
            "liveBalancedDrainObserved": liveDrainObserved,
            "balancedDrainObserved": drainPolls >= 2, "resultStableAfterDrain": complete,
            "secondaryContextRemoved": host.flowIpcWindowContext(Self.secondaryId) == nil,
            "secondaryWindowClosedObserved": ownerWindow == nil || ownerWindow?.isVisible == false,
            "realOpenPanelObserved": observedPanel != nil, "panelCount": observedPanel == nil ? 0 : 1,
            "panelsAttachedObserved": observedPanel != nil,
            "panelsVisibleObserved": observedPanel != nil, "panelURLsVerified": panelURLVerified,
            "externalPanelSelection": panelURLVerified, "externalPanelSelectionStages": "owner",
            "nativePointerObserved": pointers, "nativePointerTrustedObserved": trusted,
            "nativePointerEventCount": dispatchedStages.count,
            "nativePointerTrustedEventCount": trustedStages.count,
            "nativePointerStages": dispatchedStages.joined(separator: ","),
            "browserProgrammaticSetupObserved": routeNavigationObserved,
            "browserProgrammaticStages": "route-navigate", "physicalInput": false,
            "sandboxEnforced": false, "errorFree": state.errorFree,
            "gracefulCloseRequested": true, "recoveryGeneration": main.recoveryGeneration
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
            statuses.count == 7 && statuses.values.allSatisfy { $0.hasSuffix(": pending") }
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
                onFailure: { _ in self.fail("fileReadGrantIsolationActivationFailed") }) {
                let location = context.webView.convert(NSPoint(x: point.centerX,
                    y: context.webView.isFlipped ? point.centerY
                        : context.webView.bounds.height - point.centerY), to: nil)
                guard context.window.contentLayoutRect.contains(location) else {
                    self.fail("fileReadGrantIsolationPointerOutsideWindow"); return
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
                    self.fail("fileReadGrantIsolationPointerCreationFailed"); return
                }
                trace("recv", "flowFileReadGrantIsolation.nativePointerDispatch", [
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
          const marker = window.VatoriGrantIsolationPointer ||= {stages:{},pending:{}};
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
      const button = document.querySelector('#framework-flow-file-read-grant-isolation-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const d = window.VaadinDesktopIpcDiagnostics || {};
      const c = window.VaadinDesktopHostContext || {};
      const prefix = 'framework-flow-file-read-grant-isolation-';
      const route = document.querySelector('#'+prefix+'route');
      const operations = ['open-secondary','select','foreign-read','foreign-release','owner-read','owner-release','close-secondary'];
      const statuses = Object.fromEntries(operations.map(name => [name,
        String(document.querySelector('#'+prefix+name+'-status')?.textContent || '')]));
      return JSON.stringify({
        windowId:String(c.windowId || ''),sessionId:String(c.sessionId || ''),uiId:String(c.uiId || ''),
        rootRendered:!!document.querySelector('#framework-root') || !!route,
        initialRootReady:!!document.querySelector('#framework-root') && !route,
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:!!route && window.location.pathname.endsWith('/flow-file-read-grant-isolation')
          && operations.every(name => !!document.querySelector('#'+prefix+name)),
        statuses,
        requestCount:Number(d.standardBundleOpaqueRequestCount || 0),
        responseCount:Number(d.standardBundleOpaqueResponseCount || 0),
        pushMessageCount:Number(d.standardBundlePushMessageCount || 0),
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript').length,
        pointerStages:window.VatoriGrantIsolationPointer?.stages || {},
        errorFree:!d.standardBundleBrowserError && !d.standardBundleOpaqueRequestError
          && !d.standardBundleOpaqueResponseError && !d.standardBundlePushError
      });
    })()
    """
}
