import Cocoa
import Darwin
import Foundation

private var flowIpcFileReadGrantReplayAcceptance: FlowIpcFileReadGrantReplayAcceptance?

extension WebViewHost {
    func scheduleFileReadGrantReplayAcceptanceIfReady() {
        guard config.flowIpcAutoFileReadGrantReplay,
              config.transport == .flowIpc else { return }
        if flowIpcFileReadGrantReplayAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 600
            flowIpcFileReadGrantReplayAcceptance = FlowIpcFileReadGrantReplayAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileReadGrantReplayAcceptance?.resume()
    }
}

/// Test-only native choreography. Only the Java fixture writes and reloads the
/// private handoff token; this driver sees boolean and numeric receipts only.
private final class FlowIpcFileReadGrantReplayAcceptance {
    private enum Phase: String {
        case root, route, opening, selectingOld, selectedOld, recovering, recoveredRoute
        case oldRead, oldRelease, selectingFresh, selectedFresh, freshRead, freshRelease
        case closing, drain
    }

    private static let prefix = "framework-flow-file-read-grant-replay-"
    private static let secondaryId = "file-read-grant-replay-secondary"
    private static let oldPanelTitle = "Flow file read grant before recovery"
    private static let freshPanelTitle = "Flow file read grant after recovery"
    private static let stages = [
        "main-open-secondary", "secondary-select-old", "recovered-old-read",
        "recovered-old-release", "recovered-select-fresh", "secondary-fresh-read",
        "secondary-fresh-release", "main-close-secondary"
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
    private var failedPID: Int32 = 0
    private var recoveredPID: Int32 = 0
    private var handoffProducerPID: Int32 = 0
    private var restoredCurrentPID: Int32 = 0
    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var ownerRequestBaseline = 0
    private var ownerResponseBaseline = 0
    private var ownerPushBaseline = 0
    private var oldOwnerRequestBaseline = 0
    private var oldOwnerResponseBaseline = 0
    private var oldOwnerRequestCount = 0
    private var oldOwnerResponseCount = 0
    private var ownerFinalRequestCount = 0
    private var ownerFinalResponseCount = 0
    private var ownerFinalPushCount = 0
    private var oldSecurityScoped: Bool?
    private var freshSecurityScoped: Bool?
    private var dispatchedStages: [String] = []
    private var receivedStages: [String] = []
    private var trustedStages: [String] = []
    private var browserStages: [String] = []
    private var observedPanels: [NSOpenPanel] = []
    private var panelStages: [String] = []
    private var oldSelectionObserved = false
    private var oldReadObserved = false
    private var oldReleaseObserved = false
    private var freshSelectionObserved = false
    private var freshReadObserved = false
    private var freshReleaseObserved = false
    private var secondaryRenderingObserved = false
    private var recoveredStateResetObserved = false
    private var liveContextsRetained = false
    private var preFailureDrainObserved = false
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
        let file = directory.appendingPathComponent("vatori-file-read-grant-fixture.txt")
        do {
            let bytes = Data("vatori-file-read-grant-fixture::0123456789::tail".utf8)
            guard bytes.count == 48 else { return }
            try FileManager.default.createDirectory(
                at: directory, withIntermediateDirectories: false)
            try bytes.write(to: file, options: .atomic)
            fixtureURL = file
            fixtureCreated = true
        } catch {
            // The runner removes only its generated root after the host exits.
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
        guard Date() < deadline else {
            fail("fileReadGrantReplayDeadline")
            return
        }
        if phase == .recovering { awaitRecovery(); return }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else { schedule(); return }
        if phase != .root && !mainRetained(main) {
            fail("fileReadGrantReplayMainIdentityChanged")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot else { self.schedule(); return }
            guard snapshot.errorFree, self.mainScope(snapshot) else {
                self.fail("fileReadGrantReplayMainScopeOrRuntimeError")
                return
            }
            self.advance(main, snapshot)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        switch phase {
        case .root:
            guard fixtureCreated, state.initialRootReady,
                  state.firstMeaningfulPaint, state.flowClientCount > 0,
                  state.balanced, main.recoveryGeneration == 0,
                  let process = host.sidecarProcessIdentifier, process > 0 else {
                schedule(); return
            }
            initialPID = process
            mainWindowIdentity = ObjectIdentifier(main.window)
            mainWebViewIdentity = ObjectIdentifier(main.webView)
            navigate(main, recovered: false)
        case .route:
            guard state.routeReady, state.allPending else { schedule(); return }
            click(main, "open-secondary", "main-open-secondary", .opening)
        case .opening:
            guard record("main-open-secondary", state),
                  matches(state, "open-secondary", [
                    "opened": "true", "client": "true", "change": "1"
                  ]) else { schedule(); return }
            guard let owner = host.flowIpcWindowContext(Self.secondaryId),
                  owner.navigationFinished else { schedule(); return }
            inspect(owner) { ownerState in
                guard let ownerState, ownerState.errorFree, ownerState.routeReady,
                      ownerState.firstMeaningfulPaint, ownerState.flowClientCount > 0,
                      ownerState.allPending, ownerState.balanced,
                      ownerState.windowId == Self.secondaryId,
                      ownerState.uiId == Self.secondaryId,
                      self.opaqueSession(ownerState.sessionId),
                      ownerState.sessionId != state.sessionId,
                      owner.recoveryGeneration == 0,
                      owner.window.attachedSheet == nil, Self.panels.isEmpty else {
                    self.schedule(); return
                }
                self.ownerSession = ownerState.sessionId
                self.ownerWindow = owner.window
                self.ownerWindowIdentity = ObjectIdentifier(owner.window)
                self.ownerWebViewIdentity = ObjectIdentifier(owner.webView)
                self.oldOwnerRequestBaseline = ownerState.requestCount
                self.oldOwnerResponseBaseline = ownerState.responseCount
                self.secondaryRenderingObserved = true
                self.click(owner, "select-old", "secondary-select-old", .selectingOld)
            }
        case .selectingOld:
            withOwner { owner, ownerState in
                self.observePanel(owner, ownerState, old: true)
            }
        case .selectedOld:
            withOwner { owner, ownerState in
                guard self.oldSelectionMatches(ownerState),
                      owner.window.attachedSheet == nil, Self.panels.isEmpty,
                      self.pending(ownerState, except: ["select-old"]),
                      self.pending(state, except: ["open-secondary"]),
                      state.balanced, ownerState.balanced,
                      ownerState.requestCount > self.oldOwnerRequestBaseline else {
                    self.resetDrain(); self.schedule(); return
                }
                self.oldSecurityScoped = Self.bool("securityScoped", ownerState.status("select-old"))
                self.oldSelectionObserved = true
                self.handoffProducerPID = Self.pid("producerProcessId", ownerState.status("select-old"))
                guard self.quietPoll(self.drainFingerprint("before-crash", state, ownerState)) else {
                    self.schedule(0.2); return
                }
                self.preFailureDrainObserved = true
                self.oldOwnerRequestCount = ownerState.requestCount
                self.oldOwnerResponseCount = ownerState.responseCount
                self.resetDrain()
                guard self.initialContextsRetained(main, owner),
                      main.window.isVisible || main.window.isMiniaturized,
                      self.dispatchedStages == Array(Self.stages.prefix(2)),
                      self.receivedStages == self.dispatchedStages,
                      self.trustedStages == self.dispatchedStages else {
                    self.fail("fileReadGrantReplayFailureInjectionRejected"); return
                }
                trace("recv", "flowFileReadGrantReplay.beforeFailureObserved", [
                    "complete": true,
                    "handoffWritten": true, "handoffVerified": true, "handoffProtected": true,
                    "producerProcessIdentifier": self.handoffProducerPID,
                    "currentProcessIdentifier": self.host.sidecarProcessIdentifier ?? 0,
                    "windowId": owner.scope.windowId, "sessionId": owner.scope.sessionId,
                    "uiId": owner.scope.uiId,
                    "mainWindowId": main.scope.windowId, "mainSessionId": main.scope.sessionId,
                    "mainUiId": main.scope.uiId,
                    "oldSecondaryRequestBaseline": self.oldOwnerRequestBaseline,
                    "oldSecondaryResponseBaseline": self.oldOwnerResponseBaseline,
                    "oldSecondaryRequestCount": self.oldOwnerRequestCount,
                    "oldSecondaryResponseCount": self.oldOwnerResponseCount,
                    "mainRequestCount": state.requestCount, "mainResponseCount": state.responseCount,
                    "preFailureBalancedDrainObserved": self.preFailureDrainObserved,
                    "windowRetained": true, "webViewRetained": true,
                    "secondaryWindowRetained": true, "secondaryWebViewRetained": true,
                    "ownerWindowsOpen": true, "oldSecurityScoped": self.oldSecurityScoped ?? false,
                    "nativePointerEventCount": self.dispatchedStages.count,
                    "nativePointerTrustedEventCount": self.trustedStages.count,
                    "nativePointerStages": self.dispatchedStages.joined(separator: ",")
                ])
                guard self.killOriginalSidecar() else {
                    self.fail("fileReadGrantReplayFailureInjectionRejected"); return
                }
                self.phase = .recovering
                self.schedule()
            }
        case .recovering:
            awaitRecovery()
        case .recoveredRoute:
            guard state.routeReady, state.allPending, state.balanced,
                  state.firstMeaningfulPaint, state.flowClientCount > 0,
                  state.pointerStages.isEmpty else { schedule(); return }
            withOwner { owner, ownerState in
                guard ownerState.allPending, ownerState.balanced,
                      ownerState.firstMeaningfulPaint, ownerState.flowClientCount > 0,
                      ownerState.pointerStages.isEmpty,
                      owner.window.attachedSheet == nil, Self.panels.isEmpty else {
                    self.schedule(); return
                }
                self.recoveredStateResetObserved = true
                self.mainRequestBaseline = state.requestCount
                self.mainResponseBaseline = state.responseCount
                self.ownerRequestBaseline = ownerState.requestCount
                self.ownerResponseBaseline = ownerState.responseCount
                self.ownerPushBaseline = ownerState.pushMessageCount
                self.click(owner, "old-read", "recovered-old-read", .oldRead)
            }
        case .oldRead:
            withOwner { owner, ownerState in
                guard self.record("recovered-old-read", ownerState),
                      self.oldResultMatches(ownerState, "old-read"),
                      self.pending(ownerState, except: ["old-read"]) else {
                    self.schedule(); return
                }
                self.oldReadObserved = true
                self.restoredCurrentPID = Self.pid("currentProcessId", ownerState.status("old-read"))
                self.click(owner, "old-release", "recovered-old-release", .oldRelease)
            }
        case .oldRelease:
            withOwner { owner, ownerState in
                guard self.oldResultMatches(ownerState, "old-read"),
                      self.record("recovered-old-release", ownerState),
                      self.oldResultMatches(ownerState, "old-release"),
                      self.matches(ownerState, "old-release", ["handoffRetained": "true"]),
                      self.pending(ownerState, except: ["old-read", "old-release"]) else {
                    self.schedule(); return
                }
                self.oldReleaseObserved = true
                self.click(owner, "select-fresh", "recovered-select-fresh", .selectingFresh)
            }
        case .selectingFresh:
            withOwner { owner, ownerState in
                self.observePanel(owner, ownerState, old: false)
            }
        case .selectedFresh:
            withOwner { owner, ownerState in
                guard self.oldResultMatches(ownerState, "old-read"),
                      self.oldResultMatches(ownerState, "old-release"),
                      self.freshSelectionMatches(ownerState),
                      owner.window.attachedSheet == nil, Self.panels.isEmpty,
                      self.pending(ownerState, except: ["old-read", "old-release", "select-fresh"]) else {
                    self.schedule(); return
                }
                self.freshSelectionObserved = true
                self.freshSecurityScoped = Self.bool("securityScoped", ownerState.status("select-fresh"))
                self.click(owner, "fresh-read", "secondary-fresh-read", .freshRead)
            }
        case .freshRead:
            withOwner { owner, ownerState in
                guard self.freshSelectionMatches(ownerState),
                      self.record("secondary-fresh-read", ownerState),
                      self.freshReadMatches(ownerState),
                      self.pending(ownerState, except: [
                        "old-read", "old-release", "select-fresh", "fresh-read"
                      ]) else { self.schedule(); return }
                self.freshReadObserved = true
                self.click(owner, "fresh-release", "secondary-fresh-release", .freshRelease)
            }
        case .freshRelease:
            withOwner { _, ownerState in
                guard self.allResultsMatch(ownerState),
                      self.record("secondary-fresh-release", ownerState),
                      self.pending(state, except: []),
                      state.balanced, ownerState.balanced,
                      ownerState.requestCount > self.ownerRequestBaseline,
                      ownerState.pushMessageCount > self.ownerPushBaseline else {
                    self.resetDrain(); self.schedule(); return
                }
                self.freshReleaseObserved = true
                guard self.quietPoll(self.drainFingerprint("live", state, ownerState)) else {
                    self.schedule(0.2); return
                }
                self.liveContextsRetained = true
                self.liveDrainObserved = true
                self.ownerFinalRequestCount = ownerState.requestCount
                self.ownerFinalResponseCount = ownerState.responseCount
                self.ownerFinalPushCount = ownerState.pushMessageCount
                self.resetDrain()
                self.click(main, "close-secondary", "main-close-secondary", .closing)
            }
        case .closing, .drain:
            guard record("main-close-secondary", state),
                  matches(state, "close-secondary", [
                    "closed": "true", "client": "true", "change": "1", "sharedStateCleared": "true"
                  ]),
                  pending(state, except: ["close-secondary"]),
                  host.flowIpcWindowContext(Self.secondaryId) == nil,
                  ownerWindow == nil || ownerWindow?.isVisible == false,
                  state.balanced, Self.panels.isEmpty else {
                resetDrain(); schedule(); return
            }
            phase = .drain
            let key = "final:\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            finish(main, state)
        }
    }

    private func navigate(_ main: FlowIpcWindowContext, recovered: Bool) {
        main.webView.evaluateJavaScript(Self.navigationScript) { value, error in
            guard error == nil, value as? Bool == true else {
                self.schedule(); return
            }
            self.browserStages.append(recovered ? "recovered-route-navigate" : "route-navigate")
            self.phase = recovered ? .recoveredRoute : .route
            self.schedule(0.3)
        }
    }

    private func killOriginalSidecar() -> Bool {
        guard initialPID > 0, failedPID == 0,
              host.sidecarProcessIdentifier == initialPID,
              Darwin.kill(initialPID, SIGKILL) == 0 else { return false }
        failedPID = initialPID
        trace("send", "acceptance.sidecarFailureInjected", [
            "reason": "stale-file-read-grant-replay", "command": "fileDialog.openReadGrant",
            "processIdentifier": initialPID
        ])
        return true
    }

    private func awaitRecovery() {
        guard let main = host.flowIpcWindowContext("main"),
              let owner = host.flowIpcWindowContext(Self.secondaryId) else {
            fail("fileReadGrantReplayRecoveryWindowsLost"); return
        }
        guard mainWindowIdentity == ObjectIdentifier(main.window),
              mainWebViewIdentity == ObjectIdentifier(main.webView),
              ownerWindowIdentity == ObjectIdentifier(owner.window),
              ownerWebViewIdentity == ObjectIdentifier(owner.webView),
              main.scope.windowId == "main", main.scope.sessionId == "flow-ipc-session",
              main.scope.uiId == "main", owner.scope.windowId == Self.secondaryId,
              owner.scope.uiId == Self.secondaryId, owner.scope.sessionId == ownerSession,
              main.recoveryGeneration <= 1, owner.recoveryGeneration <= 1 else {
            fail("fileReadGrantReplayRecoveryIdentityChanged"); return
        }
        guard let process = host.sidecarProcessIdentifier,
              process > 0, process != initialPID,
              main.recoveryGeneration == 1, owner.recoveryGeneration == 1,
              main.navigationFinished, owner.navigationFinished else { schedule(); return }
        recoveredPID = process
        inspect(main) { state in
            guard let state, state.initialRootReady, state.errorFree,
                  state.firstMeaningfulPaint, state.flowClientCount > 0,
                  state.balanced, state.pointerStages.isEmpty, self.mainScope(state) else {
                self.schedule(); return
            }
            self.navigate(main, recovered: true)
        }
    }

    private func withOwner(_ body: @escaping (FlowIpcWindowContext, Snapshot) -> Void) {
        guard let owner = host.flowIpcWindowContext(Self.secondaryId),
              ownerWindowIdentity == ObjectIdentifier(owner.window),
              ownerWebViewIdentity == ObjectIdentifier(owner.webView),
              owner.recoveryGeneration == (failedPID == 0 ? 0 : 1),
              owner.scope.windowId == Self.secondaryId, owner.scope.uiId == Self.secondaryId,
              owner.scope.sessionId == ownerSession,
              owner.window.isVisible || owner.window.isMiniaturized else {
            fail("fileReadGrantReplayOwnerIdentityChanged"); return
        }
        inspect(owner) { state in
            guard let state else { self.schedule(); return }
            guard state.errorFree, state.routeReady, state.windowId == Self.secondaryId,
                  state.uiId == Self.secondaryId, state.sessionId == self.ownerSession else {
                self.fail("fileReadGrantReplayOwnerScopeOrRuntimeError"); return
            }
            body(owner, state)
        }
    }

    private func mainRetained(_ main: FlowIpcWindowContext) -> Bool {
        mainWindowIdentity == ObjectIdentifier(main.window)
            && mainWebViewIdentity == ObjectIdentifier(main.webView)
            && main.recoveryGeneration == (failedPID == 0 ? 0 : 1)
            && host.sidecarProcessIdentifier == (failedPID == 0 ? initialPID : recoveredPID)
    }

    private func initialContextsRetained(
        _ main: FlowIpcWindowContext, _ owner: FlowIpcWindowContext
    ) -> Bool {
        mainRetained(main) && failedPID == 0
            && ownerWindowIdentity == ObjectIdentifier(owner.window)
            && ownerWebViewIdentity == ObjectIdentifier(owner.webView)
            && owner.recoveryGeneration == 0
            && owner.scope.windowId == Self.secondaryId && owner.scope.uiId == Self.secondaryId
            && owner.scope.sessionId == ownerSession
    }

    private func mainScope(_ state: Snapshot) -> Bool {
        state.windowId == "main" && state.sessionId == "flow-ipc-session" && state.uiId == "main"
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

    private func observePanel(_ owner: FlowIpcWindowContext, _ state: Snapshot, old: Bool) {
        let operation = old ? "select-old" : "select-fresh"
        let pointer = old ? "secondary-select-old" : "recovered-select-fresh"
        let stage = old ? "old" : "fresh"
        let title = old ? Self.oldPanelTitle : Self.freshPanelTitle
        guard Self.field("outcome", state.status(operation)) == nil else {
            fail("fileReadGrantReplayPanelCompletedBeforeObservation"); return
        }
        guard record(pointer, state), let file = fixtureURL,
              let panel = owner.window.attachedSheet as? NSOpenPanel,
              panel.title == title, panel.sheetParent === owner.window,
              panel.isVisible, Self.panels.count == 1,
              Self.panels.first === panel else { schedule(0.05); return }
        if !observedPanels.contains(where: { $0 === panel }) {
            guard observedPanels.count == (old ? 0 : 1) else {
                fail("fileReadGrantReplayUnexpectedPanel"); return
            }
            observedPanels.append(panel)
            trace("recv", "flowFileReadGrantReplay.panelReady", [
                "stage": stage, "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        }
        // Passive observation only. The external UI selects this generated file
        // and confirms the real Open control after panelObserved is emitted.
        guard panel.url?.resolvingSymlinksInPath().standardizedFileURL
                == file.resolvingSymlinksInPath().standardizedFileURL else {
            schedule(0.05); return
        }
        panelStages.append(stage)
        trace("recv", "flowFileReadGrantReplay.panelObserved", [
            "complete": true, "stage": stage, "title": title,
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true, "inputMode": "external-native-panel-selection",
            "physicalInput": false, "windowId": owner.scope.windowId,
            "sessionId": owner.scope.sessionId, "uiId": owner.scope.uiId
        ])
        phase = old ? .selectedOld : .selectedFresh
        schedule()
    }

    private func matches(_ state: Snapshot, _ operation: String,
                         _ expected: [String: String]) -> Bool {
        expected.allSatisfy { Self.field($0.key, state.status(operation)) == $0.value }
    }

    private func callbackMatches(_ state: Snapshot, _ operation: String) -> Bool {
        matches(state, operation, [
            "client": "true", "change": "1", "capabilityContextCaptured": "true",
            "backgroundSessionAbsent": "true", "permissionAllowed": "true"
        ])
    }

    private func selectionMatches(_ state: Snapshot, _ operation: String) -> Bool {
        callbackMatches(state, operation) && matches(state, operation, [
            "outcome": "SELECTED", "grantPresent": "true", "byteCount": "48",
            "maxReadBytes": "65536", "noPath": "true"
        ]) && Self.bool("securityScoped", state.status(operation)) != nil
    }

    private func oldSelectionMatches(_ state: Snapshot) -> Bool {
        selectionMatches(state, "select-old") && matches(state, "select-old", [
            "handoffWritten": "true", "handoffVerified": "true", "handoffProtected": "true"
        ]) && Self.pid("producerProcessId", state.status("select-old")) == initialPID
            && (oldSecurityScoped == nil
                || Self.bool("securityScoped", state.status("select-old")) == oldSecurityScoped)
    }

    private func oldResultMatches(_ state: Snapshot, _ operation: String) -> Bool {
        callbackMatches(state, operation) && matches(state, operation, [
            "outcome": "UNAVAILABLE", "unavailable": "true", "grantUnavailable": "true",
            "noContent": "true", "sameTokenUsed": "true", "oldTokenUnchanged": "true",
            "recoveredProcess": "true"
        ]) && Self.pid("producerProcessId", state.status(operation)) == initialPID
            && Self.pid("currentProcessId", state.status(operation)) == recoveredPID
            && recoveredPID > 0 && recoveredPID != initialPID
    }

    private func freshSelectionMatches(_ state: Snapshot) -> Bool {
        selectionMatches(state, "select-fresh") && matches(state, "select-fresh", [
            "freshTokenDistinct": "true", "recoveredProcess": "true"
        ]) && (freshSecurityScoped == nil
            || Self.bool("securityScoped", state.status("select-fresh")) == freshSecurityScoped)
    }

    private func freshReadMatches(_ state: Snapshot) -> Bool {
        callbackMatches(state, "fresh-read") && matches(state, "fresh-read", [
            "outcome": "DATA", "success": "true", "sameTokenUsed": "true",
            "requestedOffset": "0", "requestedBytes": "19", "bytesRead": "19",
            "offset": "0", "nextOffset": "19", "eof": "false", "contentMatches": "true"
        ])
    }

    private func freshReleaseMatches(_ state: Snapshot) -> Bool {
        callbackMatches(state, "fresh-release") && matches(state, "fresh-release", [
            "outcome": "RELEASED", "success": "true", "sameTokenUsed": "true",
            "released": "true", "handoffRemoved": "true"
        ])
    }

    private func allResultsMatch(_ owner: Snapshot) -> Bool {
        oldResultMatches(owner, "old-read") && oldResultMatches(owner, "old-release")
            && matches(owner, "old-release", ["handoffRetained": "true"])
            && freshSelectionMatches(owner) && freshReadMatches(owner) && freshReleaseMatches(owner)
            && pending(owner, except: ["old-read", "old-release", "select-fresh", "fresh-read", "fresh-release"])
    }

    private func pending(_ state: Snapshot, except completed: Set<String>) -> Bool {
        state.statuses.count == 8 && state.statuses.allSatisfy {
            completed.contains($0.key) || $0.value.hasSuffix(": pending")
        }
    }

    private func drainFingerprint(_ phase: String, _ main: Snapshot, _ owner: Snapshot) -> String {
        "\(phase):\(main.requestCount):\(main.responseCount):\(main.pushMessageCount)"
            + ":\(owner.requestCount):\(owner.responseCount):\(owner.pushMessageCount)"
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

    private static func pid(_ name: String, _ status: String) -> Int32 {
        field(name, status).flatMap(Int32.init) ?? 0
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
        let statuses = oldSelectionObserved && oldReadObserved && oldReleaseObserved
            && freshSelectionObserved && freshReadObserved && freshReleaseObserved
        let retained = liveContextsRetained && mainRetained(main)
        let replaced = failedPID == initialPID && recoveredPID > 0 && recoveredPID != failedPID
        let panels = observedPanels.count == 2 && panelStages == ["old", "fresh"]
        let browser = browserStages == ["route-navigate", "recovered-route-navigate"]
        let security = oldSecurityScoped != nil && freshSecurityScoped != nil
        let complete = statuses && pointers && trusted && retained && liveDrainObserved
            && preFailureDrainObserved && drainPolls >= 2 && panels && security && browser
            && replaced && recoveredStateResetObserved && secondaryRenderingObserved
        trace("recv", "flowFileReadGrantReplay.frameworkAcceptance", [
            "complete": complete, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.rootRendered, "flowClientOwned": state.flowClientCount > 0,
            "secondaryRenderingObserved": secondaryRenderingObserved,
            "fixtureCreated": fixtureCreated, "fixtureByteCount": 48,
            "oldSelectionStatusObserved": oldSelectionObserved,
            "oldReadStatusObserved": oldReadObserved, "oldReleaseStatusObserved": oldReleaseObserved,
            "freshSelectionStatusObserved": freshSelectionObserved,
            "freshReadStatusObserved": freshReadObserved, "freshReleaseStatusObserved": freshReleaseObserved,
            "oldSelectionOutcome": "SELECTED", "oldReadOutcome": "UNAVAILABLE",
            "oldReleaseOutcome": "UNAVAILABLE", "freshSelectionOutcome": "SELECTED",
            "freshReadOutcome": "DATA", "freshReleaseOutcome": "RELEASED", "freshReadBytes": 19,
            "contentMatches": freshReadObserved,
            "handoffWrittenObserved": oldSelectionObserved,
            "handoffVerifiedObserved": oldSelectionObserved,
            "handoffProtectedObserved": oldSelectionObserved,
            "handoffRetainedThroughOldReleaseObserved": oldReleaseObserved,
            "handoffRemovedObserved": freshReleaseObserved,
            "handoffProducerProcessIdentifier": handoffProducerPID,
            "restoredCurrentProcessIdentifier": restoredCurrentPID,
            "sameOldTokenUsedObserved": oldReadObserved && oldReleaseObserved,
            "oldTokenUnchangedObserved": oldReadObserved && oldReleaseObserved,
            "oldNoContentObserved": oldReadObserved && oldReleaseObserved,
            "sameFreshTokenUsedObserved": freshReadObserved && freshReleaseObserved,
            "freshTokenDistinctObserved": freshSelectionObserved,
            "permissionAllowedObserved": statuses,
            "recoveredProcessObserved": oldReadObserved && oldReleaseObserved,
            "securityScopedValueObserved": security,
            "oldSecurityScoped": oldSecurityScoped ?? false,
            "freshSecurityScoped": freshSecurityScoped ?? false,
            "scopeIsolatedObserved": opaqueSession(ownerSession),
            "initialProcessIdentifier": initialPID, "failedProcessIdentifier": failedPID,
            "recoveredProcessIdentifier": recoveredPID, "processReplaced": replaced,
            "windowRetained": mainRetained(main), "webViewRetained": mainRetained(main),
            "secondaryWindowRetained": liveContextsRetained,
            "secondaryWebViewRetained": liveContextsRetained,
            "liveOwnerRetainedThroughRelease": liveContextsRetained,
            "recoveredStateResetObserved": recoveredStateResetObserved,
            "mainWindowId": state.windowId, "mainSessionId": state.sessionId,
            "mainUiId": state.uiId, "secondaryWindowId": Self.secondaryId,
            "secondarySessionId": ownerSession, "secondaryUiId": Self.secondaryId,
            "oldSecondaryRequestBaseline": oldOwnerRequestBaseline,
            "oldSecondaryResponseBaseline": oldOwnerResponseBaseline,
            "oldSecondaryRequestCount": oldOwnerRequestCount,
            "oldSecondaryResponseCount": oldOwnerResponseCount,
            "mainRequestBaseline": mainRequestBaseline, "mainResponseBaseline": mainResponseBaseline,
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
            "preFailureBalancedDrainObserved": preFailureDrainObserved,
            "liveBalancedDrainObserved": liveDrainObserved,
            "balancedDrainObserved": drainPolls >= 2, "resultStableAfterDrain": complete,
            "secondaryContextRemoved": host.flowIpcWindowContext(Self.secondaryId) == nil,
            "secondaryWindowClosedObserved": ownerWindow == nil || ownerWindow?.isVisible == false,
            "realOpenPanelObserved": panels, "panelCount": observedPanels.count,
            "panelsAttachedObserved": panels, "panelsVisibleObserved": panels,
            "panelURLsVerified": panels, "externalPanelSelection": panels,
            "externalPanelSelectionStages": panelStages.joined(separator: ","),
            "nativePointerObserved": pointers, "nativePointerTrustedObserved": trusted,
            "nativePointerEventCount": dispatchedStages.count,
            "nativePointerTrustedEventCount": trustedStages.count,
            "nativePointerStages": dispatchedStages.joined(separator: ","),
            "browserProgrammaticSetupObserved": browser,
            "browserProgrammaticStages": browserStages.joined(separator: ","),
            "physicalInput": false, "sandboxEnforced": false, "errorFree": state.errorFree,
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
            statuses.count == 8 && statuses.values.allSatisfy { $0.hasSuffix(": pending") }
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
                onFailure: { _ in self.fail("fileReadGrantReplayActivationFailed") }) {
                let location = context.webView.convert(NSPoint(x: point.centerX,
                    y: context.webView.isFlipped ? point.centerY
                        : context.webView.bounds.height - point.centerY), to: nil)
                guard context.window.contentLayoutRect.contains(location) else {
                    self.fail("fileReadGrantReplayPointerOutsideWindow"); return
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
                    self.fail("fileReadGrantReplayPointerCreationFailed"); return
                }
                trace("recv", "flowFileReadGrantReplay.nativePointerDispatch", [
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
          const marker = window.VatoriGrantReplayPointer ||= {stages:{},pending:{}};
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
      const button = document.querySelector('#framework-flow-file-read-grant-replay-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const d = window.VaadinDesktopIpcDiagnostics || {};
      const c = window.VaadinDesktopHostContext || {};
      const prefix = 'framework-flow-file-read-grant-replay-';
      const route = document.querySelector('#'+prefix+'route');
      const operations = ['open-secondary','select-old','old-read','old-release','select-fresh','fresh-read','fresh-release','close-secondary'];
      const statuses = Object.fromEntries(operations.map(name => [name,
        String(document.querySelector('#'+prefix+name+'-status')?.textContent || '')]));
      return JSON.stringify({
        windowId:String(c.windowId || ''),sessionId:String(c.sessionId || ''),uiId:String(c.uiId || ''),
        rootRendered:!!document.querySelector('#framework-root') || !!route,
        initialRootReady:!!document.querySelector('#framework-root') && !route,
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:!!route && window.location.pathname.endsWith('/flow-file-read-grant-replay')
          && operations.every(name => !!document.querySelector('#'+prefix+name)),
        statuses,
        requestCount:Number(d.standardBundleOpaqueRequestCount || 0),
        responseCount:Number(d.standardBundleOpaqueResponseCount || 0),
        pushMessageCount:Number(d.standardBundlePushMessageCount || 0),
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript').length,
        pointerStages:window.VatoriGrantReplayPointer?.stages || {},
        errorFree:!d.standardBundleBrowserError && !d.standardBundleOpaqueRequestError
          && !d.standardBundleOpaqueResponseError && !d.standardBundlePushError
      });
    })()
    """
}
