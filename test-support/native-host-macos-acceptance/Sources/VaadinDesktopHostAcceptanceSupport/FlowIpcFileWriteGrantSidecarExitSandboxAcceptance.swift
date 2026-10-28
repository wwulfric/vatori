import Cocoa
import Darwin
import Foundation
import Security

private var fileWriteGrantSidecarExitSandboxAcceptance: FlowIpcFileWriteGrantSidecarExitSandboxAcceptance?

extension WebViewHost {
    func scheduleFileWriteGrantSidecarExitSandboxAcceptanceIfReady() {
        guard config.flowIpcAutoFileWriteGrantSidecarExitSandbox, config.transport == .flowIpc else { return }
        if fileWriteGrantSidecarExitSandboxAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 600
            fileWriteGrantSidecarExitSandboxAcceptance = FlowIpcFileWriteGrantSidecarExitSandboxAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        fileWriteGrantSidecarExitSandboxAcceptance?.resume()
    }
}

/// Settled writes, one real JVM failure, and fresh public authorization. The
/// driver never carries a token between JVMs or chooses a native panel URL.
private final class FlowIpcFileWriteGrantSidecarExitSandboxAcceptance {
    private enum Phase: String { case root, route, click, panel, observing, recovering, recoveredRoute }
    private static let tracePrefix = "flowFileWriteGrantSidecarExitSandbox"
    private static let mainPrefix = "framework-flow-file-write-grant-recovery-"
    private static let ownerPrefix = "framework-flow-file-write-grant-"
    private static let ownerId = "file-write-grant-recovery-secondary"
    private static let panelTitle = "Flow file write grant new file"
    private static let stages = ["main-open-secondary", "old-select-new", "old-write-first",
        "old-invalid-range", "old-write-tail", "fresh-select-new", "fresh-write-first",
        "fresh-invalid-range", "fresh-write-tail", "fresh-release", "fresh-write-after-release",
        "main-close-secondary"]
    private static let operations = ["select-new", "write-first", "invalid-range", "write-tail",
        "release", "write-after-release", "select-existing", "select-canceled"]
    private static let buttons = ["open-secondary", "select-new", "write-first", "invalid-range",
        "write-tail", "select-new", "write-first", "invalid-range", "write-tail", "release",
        "write-after-release", "close-secondary"]
    private static let callbackFields = ["capabilityContextCaptured": "true", "backgroundSessionAbsent": "true",
        "client": "true", "change": "1", "listenerCount": "1", "permissionAllowed": "true",
        "publicTokenHidden": "true", "grantPresent": "true", "maxWriteBytes": "65536",
        "securityScoped": "true", "filenameMatches": "true"]
    private static let ownerExpected: [[String: String]] = [
        ["outcome": "SELECTED", "noPath": "true", "createNewOnly": "true", "originalGrantRetained": "true"],
        ["outcome": "WRITTEN", "requestedOffset": "0", "requestedBytes": "65536", "bytesWritten": "65536",
         "offset": "0", "nextOffset": "65536", "sameGrantFacade": "true"],
        ["outcome": "INVALID_RANGE", "requestedOffset": "-1", "requestedBytes": "1", "bytesWritten": "0",
         "offset": "-1", "nextOffset": "-1", "sameGrantFacade": "true"],
        ["outcome": "WRITTEN", "requestedOffset": "65536", "requestedBytes": "6", "bytesWritten": "6",
         "offset": "65536", "nextOffset": "65542", "sameGrantFacade": "true"],
        ["outcome": "RELEASED", "released": "true", "sameGrantFacade": "true", "securityScopeStopped": "true"],
        ["outcome": "UNAVAILABLE", "requestedOffset": "65542", "requestedBytes": "1", "bytesWritten": "0",
         "offset": "-1", "nextOffset": "-1", "sameGrantFacade": "true"]
    ]

    private let host: WebViewHost
    private let deadline: Date
    private let selectedURLs: [URL?]
    private let siblingURLs: [URL?]
    private var phase = Phase.root
    private var index = 0
    private var scheduled = false
    private var inFlight = false
    private var terminal = false
    private var initialPID: Int32 = 0
    private var failedPID: Int32 = 0
    private var recoveredPID: Int32 = 0
    private var recoveryDeadline: Date?
    private var mainWindowIdentity: ObjectIdentifier?
    private var mainWebViewIdentity: ObjectIdentifier?
    private var ownerWindowIdentity: ObjectIdentifier?
    private var ownerWebViewIdentity: ObjectIdentifier?
    private var ownerScope: FlowIpcWindowScope?
    private var mainBaselines: [Int: Snapshot] = [:]
    private var ownerBaselines: [Int: Snapshot] = [:]
    private var oldMainFinal: Snapshot?
    private var oldOwnerFinal: Snapshot?
    private var ownerFinal: Snapshot?
    private var previousMainRequests = 0
    private var previousOwnerRequests = 0
    private var dispatched: [String] = []
    private var observed: [String] = []
    private var navigated: [String] = []
    private var observedPanels: [Int: NSSavePanel] = [:]
    private var verifiedPanels: Set<Int> = []
    private var preselectionDenied: Set<Int> = []
    private var siblingDeniedAfterWrite: Set<Int> = []
    private var policyObserved = false
    private var recoveredResetObserved = false
    private var secondaryRetainedThroughRelease = false
    private var quietKey = ""
    private var quietCount = 0
    private var diagnosticSignatures: [String: String] = [:]
    private var generation: Int { index < 5 ? 1 : 2 }
    private var processIdentifier: Int32 { generation == 1 ? initialPID : recoveredPID }
    private var ownerAction: Bool { index != 0 && index != 11 }

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        let env = ProcessInfo.processInfo.environment
        selectedURLs = [Self.externalURL(env["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SELECTED_PATH"]),
            Self.externalURL(env["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_RECOVERED_SELECTED_PATH"])]
        siblingURLs = [Self.externalURL(env["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SIBLING_PATH"]),
            Self.externalURL(env["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_RECOVERED_SIBLING_PATH"])]
    }

    private static func externalURL(_ path: String?) -> URL? {
        guard let path, path.hasPrefix("/"), !path.contains("\0") else { return nil }
        return URL(fileURLWithPath: path).standardizedFileURL
    }

    func resume() { if !inFlight { schedule() } }

    private func schedule(_ delay: Double = 0.1) {
        inFlight = false
        guard !terminal, !scheduled else { return }
        scheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.scheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal, !inFlight else { return }
        inFlight = true
        guard Date() < deadline else { fail("deadline"); return }
        if phase == .recovering || phase == .recoveredRoute {
            guard let recoveryDeadline, Date() < recoveryDeadline else { fail("recovery-deadline"); return }
        }
        diagnose("poll", ["mainContextPresent": host.flowIpcWindowContext("main") != nil,
            "secondaryContextPresent": host.flowIpcWindowContext(Self.ownerId) != nil,
            "mainNavigationFinished": host.flowIpcWindowContext("main")?.navigationFinished == true,
            "secondaryNavigationFinished": host.flowIpcWindowContext(Self.ownerId)?.navigationFinished == true])
        if phase == .recovering { awaitRecovery(); return }
        guard let main = host.flowIpcWindowContext("main"), main.navigationFinished else { schedule(); return }
        if phase != .root && !retained(main, main: true) { fail("main-identity"); return }
        inspect(main) { state in
            guard let state else { self.schedule(); return }
            guard self.valid(state, main), state.errorFree else { self.fail("main-scope-or-runtime"); return }
            if self.phase == .root { self.navigate(main, state, recovered: false); return }
            if self.phase == .route {
                guard state.routeReady, state.allPending, state.balanced else { self.schedule(); return }
                self.mainBaselines[1] = state
                self.previousMainRequests = state.requestCount
                self.click(main)
                return
            }
            if let owner = self.host.flowIpcWindowContext(Self.ownerId), owner.navigationFinished {
                if self.ownerScope != nil && !self.retained(owner, main: false) { self.fail("owner-identity"); return }
                self.inspect(owner) { ownerState in
                    guard let ownerState else { self.schedule(); return }
                    guard self.valid(ownerState, owner), ownerState.errorFree else { self.fail("owner-scope-or-runtime"); return }
                    if (self.index == 0 || self.phase == .recoveredRoute) && !ownerState.routeReady {
                        self.schedule(); return
                    }
                    guard ownerState.routeReady else { self.fail("owner-route-missing"); return }
                    self.advance(main, state, owner, ownerState)
                }
            } else if self.index == 0 || self.index == 11 {
                self.advance(main, state, nil, nil)
            } else if self.phase == .recoveredRoute { self.schedule() }
            else { self.fail("owner-context-missing") }
        }
    }

    private func navigate(_ main: FlowIpcWindowContext, _ state: Snapshot, recovered: Bool) {
        guard state.initialRootReady, state.firstMeaningfulPaint, state.flowClientCount == 1,
              state.balanced, let pid = host.sidecarProcessIdentifier, pid > 0 else { schedule(); return }
        if !recovered {
            initialPID = pid
            mainWindowIdentity = ObjectIdentifier(main.window)
            mainWebViewIdentity = ObjectIdentifier(main.webView)
        }
        main.webView.evaluateJavaScript("""
            (() => {
              const b=document.querySelector('#framework-flow-file-write-grant-recovery-open-route');
              if (!b) return false;
              b.click(); return true;
            })()
            """) { value, error in
            guard error == nil, value as? Bool == true else { self.schedule(); return }
            self.navigated.append(recovered ? "recovered-route-navigate" : "route-navigate")
            self.phase = recovered ? .recoveredRoute : .route
            self.schedule(0.3)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ mainState: Snapshot,
                         _ owner: FlowIpcWindowContext?, _ ownerState: Snapshot?) {
        if phase == .recoveredRoute {
            guard let owner, let ownerState, mainState.routeReady, mainState.allPending, ownerState.allPending,
                  mainState.pointerStages.isEmpty, ownerState.pointerStages.isEmpty,
                  ownerState.firstMeaningfulPaint, ownerState.flowClientCount == 1,
                  retained(main, main: true), retained(owner, main: false), main.window.attachedSheet == nil,
                  owner.window.attachedSheet == nil, Self.panels.isEmpty else { resetQuiet(); schedule(); return }
            guard settled(mainState, ownerState) else { schedule(0.2); return }
            mainBaselines[2] = mainState
            ownerBaselines[2] = ownerState
            previousMainRequests = mainState.requestCount
            previousOwnerRequests = ownerState.requestCount
            recoveredResetObserved = true
            var payload = frame(mainState, ownerState)
            payload.merge(baselines(2)) { _, value in value }
            payload.merge(["complete": true, "initialProcessIdentifier": initialPID,
                "failedProcessIdentifier": failedPID, "currentProcessIdentifier": recoveredPID,
                "processReplaced": true, "mainWindowRetained": true, "mainWebViewRetained": true,
                "secondaryWindowRetained": true, "secondaryWebViewRetained": true,
                "recoveredStateResetObserved": true, "pointerStateResetObserved": true,
                "balancedDrainObserved": true, "recoveryCount": 1, "recoveryTimeoutSeconds": 60]) { _, value in value }
            trace("recv", "\(Self.tracePrefix).recoveryObserved", payload)
            guard probesBeforeSelection() else { return }
            resetQuiet()
            click(owner)
            return
        }
        if phase == .click {
            guard let target = ownerAction ? owner : main else { schedule(); return }
            click(target)
            return
        }
        if phase == .panel {
            guard let owner, let ownerState else { fail("panel-owner-missing"); return }
            observePanel(owner, ownerState)
            return
        }
        if index == 0 {
            guard let owner, let ownerState, ownerState.allPending, ownerState.firstMeaningfulPaint,
                  ownerState.flowClientCount == 1, ownerState.balanced, owner.recoveryGeneration == 0,
                  owner.scope.windowId == Self.ownerId, owner.scope.uiId == Self.ownerId,
                  Self.opaqueSession(owner.scope.sessionId) else { schedule(); return }
            if ownerScope == nil {
                ownerScope = owner.scope
                ownerWindowIdentity = ObjectIdentifier(owner.window)
                ownerWebViewIdentity = ObjectIdentifier(owner.webView)
                ownerBaselines[1] = ownerState
                previousOwnerRequests = ownerState.requestCount
            }
        }
        let ownerSnapshot = ownerState ?? (index == 11 ? ownerFinal : nil)
        guard let ownerSnapshot, let ownerScope, mainState.routeReady,
              main.window.attachedSheet == nil, owner?.window.attachedSheet == nil, Self.panels.isEmpty,
              statusesMatch(mainState, ownerSnapshot),
              (index != 11 || host.flowIpcWindowContext(Self.ownerId) == nil) else { resetQuiet(); schedule(); return }
        let action = ownerAction ? ownerSnapshot : mainState
        guard action.pointerStages[Self.stages[index]]?.receipt == true,
              action.pointerStages[Self.stages[index]]?.trusted == true,
              action.requestCount > (ownerAction ? previousOwnerRequests : previousMainRequests),
              settled(mainState, ownerSnapshot) else { schedule(); return }
        emitStage(ownerAction ? ownerScope : main.scope, action, mainState, ownerSnapshot)
        observed.append(Self.stages[index])
        previousMainRequests = mainState.requestCount
        previousOwnerRequests = ownerSnapshot.requestCount
        if index == 0 {
            guard verifyPolicy(), probesBeforeSelection() else { return }
        }
        if index == 4 || index == 8 {
            guard let sibling = siblingURLs[generation - 1],
                  denied(sibling, stage: generation == 1 ? "old-sibling-after" : "fresh-sibling-after") else { return }
            siblingDeniedAfterWrite.insert(generation)
        }
        if index == 4 {
            prepareExit(mainState, ownerSnapshot)
            return
        }
        if index == 10 {
            guard let owner, retained(owner, main: false), retained(main, main: true),
                  let baseline = ownerBaselines[2], ownerSnapshot.pushMessageCount > baseline.pushMessageCount else {
                fail("fresh-owner-release-continuity"); return
            }
            ownerFinal = ownerSnapshot
            secondaryRetainedThroughRelease = true
        }
        if index == 11 { finish(main, mainState, ownerSnapshot); return }
        index += 1
        resetQuiet()
        guard let target = ownerAction ? owner : main else { fail("next-owner-missing"); return }
        click(target)
    }

    private func settled(_ main: Snapshot, _ owner: Snapshot) -> Bool {
        guard main.balanced, owner.balanced else { resetQuiet(); return false }
        let key = "\(generation):\(index):\(main.requestCount):\(main.responseCount):\(main.pushMessageCount)"
            + ":\(owner.requestCount):\(owner.responseCount):\(owner.pushMessageCount)"
        quietCount = key == quietKey ? quietCount + 1 : 1
        quietKey = key
        return quietCount >= 2
    }

    private func statusesMatch(_ main: Snapshot, _ owner: Snapshot) -> Bool {
        if generation == 1 {
            guard Self.matches(main.status("open-secondary"), Self.mainExpected(opening: true)),
                  main.status("close-secondary") == "close-secondary: pending" else { return false }
        } else {
            guard main.status("open-secondary") == "open-secondary: pending" else { return false }
            if index == 11 {
                guard Self.matches(main.status("close-secondary"), Self.mainExpected(opening: false)) else { return false }
            } else if main.status("close-secondary") != "close-secondary: pending" { return false }
        }
        let completed = generation == 1 ? index - 1 : min(index - 5, 5)
        for operation in Self.operations.indices {
            let name = Self.operations[operation]
            if operation <= completed {
                guard Self.matches(owner.status(name), Self.expectedOwner(operation)) else { return false }
            } else if owner.status(name) != "\(name): pending" { return false }
        }
        return true
    }

    private static func mainExpected(opening: Bool) -> [String: String] {
        [opening ? "opened" : "closed": "true", "client": "true", "change": "1", "listenerCount": "1"]
    }

    private static func expectedOwner(_ operation: Int) -> [String: String] {
        ownerExpected[operation].merging(callbackFields) { _, value in value }
    }

    private static func matches(_ status: String, _ expected: [String: String]) -> Bool {
        expected.allSatisfy { field($0.key, status) == $0.value }
    }

    private static func field(_ name: String, _ status: String) -> String? {
        let prefix = name + "="
        return status.split(separator: " ").first { $0.hasPrefix(prefix) }.map { String($0.dropFirst(prefix.count)) }
    }

    private func retained(_ context: FlowIpcWindowContext, main: Bool) -> Bool {
        let expectedScope = main ? FlowIpcWindowScope.main : ownerScope
        return ObjectIdentifier(context.window) == (main ? mainWindowIdentity : ownerWindowIdentity)
            && ObjectIdentifier(context.webView) == (main ? mainWebViewIdentity : ownerWebViewIdentity)
            && context.scope == expectedScope
            && context.recoveryGeneration == generation - 1
            && host.sidecarProcessIdentifier == processIdentifier && processIdentifier > 0
    }

    private static func opaqueSession(_ session: String) -> Bool {
        let prefix = "flow-ipc-window:"
        return session.hasPrefix(prefix) && UUID(uuidString: String(session.dropFirst(prefix.count))) != nil
    }

    private func valid(_ state: Snapshot, _ context: FlowIpcWindowContext) -> Bool {
        state.windowId == context.scope.windowId && state.sessionId == context.scope.sessionId && state.uiId == context.scope.uiId
    }

    private func prepareExit(_ main: Snapshot, _ owner: Snapshot) {
        oldMainFinal = main
        oldOwnerFinal = owner
        var payload = frame(main, owner)
        payload.merge(baselines(1)) { _, value in value }
        payload.merge(["complete": true, "settledGrantObserved": true, "releasePending": true,
            "writeAfterReleasePending": true, "bothWindowsRetained": true, "bothWebViewsRetained": true,
            "balancedDrainObserved": true, "resultStableAfterDrain": true, "writtenByteCount": 65_542,
            "nativePointerEventCount": dispatched.count, "nativePointerTrustedEventCount": observed.count]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).exitPrepared", payload)
        guard let pid = host.sidecarProcessIdentifier, pid == initialPID, pid > 0, failedPID == 0,
              Darwin.kill(pid, SIGKILL) == 0 else { fail("sidecar-failure-injection"); return }
        failedPID = pid
        trace("send", "acceptance.sidecarFailureInjected", ["reason": "settled-file-write-grant",
            "command": "fileGrant.write", "processIdentifier": pid])
        recoveryDeadline = Date().addingTimeInterval(60)
        index = 5
        phase = .recovering
        resetQuiet()
        schedule()
    }

    private func awaitRecovery() {
        guard let main = host.flowIpcWindowContext("main"), let owner = host.flowIpcWindowContext(Self.ownerId) else {
            fail("recovery-window-lost"); return
        }
        guard main.recoveryGeneration <= 1, owner.recoveryGeneration <= 1 else { fail("extra-recovery"); return }
        guard let pid = host.sidecarProcessIdentifier, pid > 0, pid != initialPID,
              main.recoveryGeneration == 1, owner.recoveryGeneration == 1,
              main.navigationFinished, owner.navigationFinished else { schedule(); return }
        recoveredPID = pid
        guard retained(main, main: true), retained(owner, main: false) else { fail("recovery-identity"); return }
        inspect(main) { state in
            guard let state, self.valid(state, main), state.errorFree, state.pointerStages.isEmpty,
                  state.initialRootReady, state.firstMeaningfulPaint, state.flowClientCount == 1, state.balanced else {
                self.schedule(); return
            }
            self.navigate(main, state, recovered: true)
        }
    }

    private func inspect(_ context: FlowIpcWindowContext, _ done: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspection(main: context.scope.windowId == "main")) { value, error in
            let state: Snapshot?
            if error == nil, let json = value as? String, let data = json.data(using: .utf8) {
                state = try? JSONDecoder().decode(Snapshot.self, from: data)
            } else { state = nil }
            self.diagnose(context.scope.windowId == "main" ? "snapshot-main" : "snapshot-secondary", [
                "javascriptError": error != nil, "stringResult": value is String, "snapshotDecoded": state != nil,
                "initialRootReady": state?.initialRootReady == true, "routeReady": state?.routeReady == true,
                "statusCount": state?.statuses.count ?? -1, "allPending": state?.allPending == true,
                "balanced": state?.balanced == true, "requestCount": state?.requestCount ?? -1,
                "responseCount": state?.responseCount ?? -1, "pushCount": state?.pushMessageCount ?? -1,
                "flowClientCount": state?.flowClientCount ?? -1, "errorFree": state?.errorFree == true,
                "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow])
            done(state)
        }
    }

    private func observePanel(_ owner: FlowIpcWindowContext, _ state: Snapshot) {
        guard Self.field("outcome", state.status("select-new")) == nil else { fail("panel-completed-before-observation"); return }
        guard state.pointerStages[Self.stages[index]]?.receipt == true,
              state.pointerStages[Self.stages[index]]?.trusted == true,
              let selected = selectedURLs[generation - 1], let panel = owner.window.attachedSheet as? NSSavePanel,
              !(panel is NSOpenPanel), panel.title == Self.panelTitle, panel.sheetParent === owner.window,
              panel.isVisible, Self.panels.count == 1, Self.panels.first === panel else { schedule(0.05); return }
        if let previous = observedPanels[generation], previous !== panel { fail("panel-replaced"); return }
        if observedPanels[generation] == nil {
            observedPanels[generation] = panel
            var ready = scoped(owner.scope)
            ready.merge(["stage": Self.stages[index], "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"]) { _, value in value }
            trace("recv", "\(Self.tracePrefix).panelReady", ready)
        }
        guard panel.url?.standardizedFileURL == selected else { schedule(0.05); return }
        verifiedPanels.insert(generation)
        var payload = scoped(owner.scope)
        payload.merge(["complete": true, "stage": Self.stages[index], "title": Self.panelTitle,
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true, "inputMode": "external-native-panel-selection",
            "physicalInput": false]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).panelObserved", payload)
        phase = .observing
        schedule()
    }

    private static var panels: [NSSavePanel] { NSApp.windows.compactMap { $0 as? NSSavePanel }.filter(\.isVisible) }

    private func epoch() -> [String: Any] {
        ["sidecarGeneration": generation, "recoveryGeneration": generation - 1,
         "hostProcessIdentifier": getpid(), "processIdentifier": processIdentifier]
    }

    private func scoped(_ scope: FlowIpcWindowScope) -> [String: Any] {
        epoch().merging(scope.tracePayload) { _, value in value }
    }

    private func counts(_ main: Snapshot, _ owner: Snapshot, old: Bool = false) -> [String: Any] {
        let mainPrefix = old ? "oldMain" : "main", ownerPrefix = old ? "oldSecondary" : "secondary"
        return [mainPrefix + "RequestCount": main.requestCount, mainPrefix + "ResponseCount": main.responseCount,
            mainPrefix + "PushCount": main.pushMessageCount, ownerPrefix + "RequestCount": owner.requestCount,
            ownerPrefix + "ResponseCount": owner.responseCount, ownerPrefix + "PushCount": owner.pushMessageCount]
    }

    private func frame(_ main: Snapshot, _ owner: Snapshot) -> [String: Any] {
        var value = epoch()
        value.merge(["mainWindowId": main.windowId, "mainSessionId": main.sessionId, "mainUiId": main.uiId,
            "secondaryWindowId": owner.windowId, "secondarySessionId": owner.sessionId, "secondaryUiId": owner.uiId]) { _, value in value }
        value.merge(counts(main, owner)) { _, value in value }
        return value
    }

    private func baselines(_ generation: Int, old: Bool = false) -> [String: Any] {
        guard let main = mainBaselines[generation], let owner = ownerBaselines[generation] else { return [:] }
        let mainPrefix = old ? "oldMain" : "main", ownerPrefix = old ? "oldSecondary" : "secondary"
        return [mainPrefix + "RequestBaseline": main.requestCount, mainPrefix + "ResponseBaseline": main.responseCount,
            mainPrefix + "PushBaseline": main.pushMessageCount, ownerPrefix + "RequestBaseline": owner.requestCount,
            ownerPrefix + "ResponseBaseline": owner.responseCount, ownerPrefix + "PushBaseline": owner.pushMessageCount]
    }

    private func emitStage(_ scope: FlowIpcWindowScope, _ state: Snapshot, _ main: Snapshot, _ owner: Snapshot) {
        var payload = frame(main, owner)
        payload.merge(scope.tracePayload) { _, value in value }
        let fields = ownerAction ? Self.expectedOwner(index < 5 ? index - 1 : index - 5) : Self.mainExpected(opening: index == 0)
        for (name, value) in fields {
            if value == "true" || value == "false" { payload[name] = value == "true" }
            else if let number = Int(value) { payload[name] = number }
            else { payload[name] = value }
        }
        payload.merge(["complete": true, "stage": Self.stages[index], "requestCount": state.requestCount,
            "responseCount": state.responseCount, "pushCount": state.pushMessageCount,
            "balancedDrainObserved": true, "nativePointerObserved": true, "nativePointerTrustedObserved": true,
            "mainWindowRetained": true, "mainWebViewRetained": true,
            "secondaryWindowRetained": index != 11, "secondaryWebViewRetained": index != 11]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).stageObserved", payload)
    }

    private func finish(_ main: FlowIpcWindowContext, _ state: Snapshot, _ owner: Snapshot) {
        guard let oldMainFinal, let oldOwnerFinal else { fail("missing-old-snapshots"); return }
        let complete = observed == Self.stages && dispatched == Self.stages && retained(main, main: true)
            && host.flowIpcWindowContext(Self.ownerId) == nil && secondaryRetainedThroughRelease && quietCount >= 2
            && verifiedPanels == [1, 2] && preselectionDenied == [1, 2] && siblingDeniedAfterWrite == [1, 2]
            && policyObserved && recoveredResetObserved && failedPID == initialPID && recoveredPID != initialPID
            && navigated == ["route-navigate", "recovered-route-navigate"]
        var payload = frame(state, owner)
        payload.merge(baselines(2)) { _, value in value }
        payload.merge(baselines(1, old: true)) { _, value in value }
        payload.merge(counts(oldMainFinal, oldOwnerFinal, old: true)) { _, value in value }
        payload.merge(["complete": complete, "initialProcessIdentifier": initialPID, "failedProcessIdentifier": failedPID,
            "currentProcessIdentifier": recoveredPID, "stageCount": observed.count,
            "observedStages": observed.joined(separator: ","), "nativePointerEventCount": dispatched.count,
            "nativePointerTrustedEventCount": observed.count, "nativePointerStages": dispatched.joined(separator: ","),
            "panelCount": observedPanels.count, "accessProbeCount": 6, "writtenByteCount": 65_542, "oldWrittenByteCount": 65_542,
            "mainWindowRetained": retained(main, main: true), "mainWebViewRetained": retained(main, main: true),
            "secondaryContextRemoved": host.flowIpcWindowContext(Self.ownerId) == nil,
            "secondaryRetainedThroughRelease": secondaryRetainedThroughRelease,
            "processReplaced": failedPID == initialPID && recoveredPID > 0 && recoveredPID != initialPID,
            "recoveredStateResetObserved": recoveredResetObserved, "pointerStateResetObserved": recoveredResetObserved,
            "balancedDrainObserved": quietCount >= 2, "resultStableAfterDrain": complete,
            "exactEntitlementsObserved": policyObserved, "oldPreselectionDenied": preselectionDenied.contains(1),
            "freshPreselectionDenied": preselectionDenied.contains(2), "oldSiblingDeniedAfterWrite": siblingDeniedAfterWrite.contains(1),
            "freshSiblingDeniedAfterWrite": siblingDeniedAfterWrite.contains(2), "sandboxEnforced": complete,
            "typedGrantApiObserved": complete, "panelURLsVerified": verifiedPanels == [1, 2],
            "externalPanelSelection": verifiedPanels == [1, 2], "nativePointerObserved": dispatched == Self.stages,
            "nativePointerTrustedObserved": observed == Self.stages, "errorFree": state.errorFree && owner.errorFree,
            "standardBundleMode": state.firstMeaningfulPaint, "flowClientOwned": state.flowClientCount == 1,
            "secondaryRenderingObserved": owner.firstMeaningfulPaint, "browserProgrammaticSetupObserved": navigated.count == 2,
            "browserProgrammaticStages": navigated.joined(separator: ","), "physicalInput": false,
            "gracefulCloseRequested": true]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).frameworkAcceptance", payload)
        terminal = true
        main.window.close()
    }

    private func verifyPolicy() -> Bool {
        var code: SecCode?
        var staticCode: SecStaticCode?
        var information: CFDictionary?
        let names: Set<String> = ["com.apple.security.app-sandbox",
            "com.apple.security.files.user-selected.read-write", "com.apple.security.network.client"]
        guard SecCodeCopySelf(SecCSFlags(), &code) == errSecSuccess, let code,
              SecCodeCopyStaticCode(code, SecCSFlags(), &staticCode) == errSecSuccess, let staticCode,
              SecCodeCopySigningInformation(staticCode, SecCSFlags(rawValue: kSecCSSigningInformation), &information) == errSecSuccess,
              let details = information as? [String: Any],
              let entitlements = details[kSecCodeInfoEntitlementsDict as String] as? [String: Any],
              Set(entitlements.keys) == names,
              entitlements.values.allSatisfy({ CFGetTypeID($0 as CFTypeRef) == CFBooleanGetTypeID() && ($0 as? Bool) == true }) else {
            fail("entitlements"); return false
        }
        guard let first = selectedURLs[0], let second = selectedURLs[1],
              first.deletingLastPathComponent() != second.deletingLastPathComponent() else { fail("distinct-targets"); return false }
        for epoch in 0...1 {
            guard let target = selectedURLs[epoch], let sibling = siblingURLs[epoch],
                  target.lastPathComponent == "vatori-file-write-grant-fixture.bin", sibling.lastPathComponent == "unselected-sibling.txt",
                  target != sibling, target.deletingLastPathComponent() == sibling.deletingLastPathComponent(),
                  [target, sibling].allSatisfy({ url in
                    [FileManager.default.homeDirectoryForCurrentUser, Bundle.main.bundleURL].allSatisfy { parent in
                        let path = parent.standardizedFileURL.path
                        return url.path != path && !url.path.hasPrefix(path + "/")
                    }
                  }) else { fail("external-fixtures"); return false }
        }
        policyObserved = true
        trace("recv", "\(Self.tracePrefix).policyObserved", ["complete": true, "appSandbox": true,
            "userSelectedReadWrite": true, "networkClient": true, "exactEntitlements": true, "entitlementCount": 3,
            "hostProcessIdentifier": getpid(), "userIdentifier": getuid(), "externalFixture": true,
            "targetOutsideContainer": true, "siblingOutsideContainer": true, "distinctSibling": true,
            "distinctRecoveredTarget": true, "distinctFixtureDirectories": true])
        return true
    }

    private func probesBeforeSelection() -> Bool {
        guard let target = selectedURLs[generation - 1], let sibling = siblingURLs[generation - 1] else {
            fail("missing-external-fixtures"); return false
        }
        let prefix = generation == 1 ? "old" : "fresh"
        guard denied(target, stage: prefix + "-target-before"), denied(sibling, stage: prefix + "-sibling-before") else { return false }
        preselectionDenied.insert(generation)
        return true
    }

    private func denied(_ url: URL, stage: String) -> Bool {
        guard let ownerScope else { fail("probe-owner-missing"); return false }
        errno = 0
        let creating = stage.hasSuffix("target-before")
        let descriptor = creating
            ? Darwin.open(url.path, O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC | O_NOFOLLOW, mode_t(0o600))
            : Darwin.open(url.path, O_RDONLY | O_CLOEXEC | O_NOFOLLOW)
        let errorNumber = descriptor < 0 ? errno : 0
        if descriptor >= 0 { Darwin.close(descriptor) }
        let rejected = descriptor < 0 && errorNumber == EPERM
        var payload = scoped(ownerScope)
        payload.merge(["complete": rejected, "stage": stage, "operation": creating ? "create-new" : "open-read",
            "denied": rejected, "errno": errorNumber, "noFollow": true]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).accessProbe", payload)
        guard rejected else { fail("access-not-denied"); return false }
        return true
    }

    private func fail(_ reason: String) {
        guard !terminal else { return }
        terminal = true
        trace("send", "error", ["code": "fileWriteGrantSidecarExitSandboxFailed", "reason": reason,
            "phase": phase.rawValue, "stage": Self.stages[index]])
        host.flowIpcWindowContext(Self.ownerId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { self.host.flowIpcWindowContext("main")?.window.close() }
    }

    private func resetQuiet() { quietKey = ""; quietCount = 0 }

    private func diagnose(_ reason: String, _ values: [String: Any]) {
        var payload: [String: Any] = ["reason": reason, "phase": phase.rawValue, "stageIndex": index,
            "stage": Self.stages[index], "sidecarGeneration": generation]
        payload.merge(values) { _, value in value }
        guard let data = try? JSONSerialization.data(withJSONObject: payload, options: [.sortedKeys]),
              let signature = String(data: data, encoding: .utf8), diagnosticSignatures[reason] != signature else { return }
        diagnosticSignatures[reason] = signature
        trace("recv", "\(Self.tracePrefix).driverDiagnostic", payload)
    }

    private struct Geometry: Decodable { let ready: Bool; let centerX: Double; let centerY: Double }
    private struct Pointer: Decodable { let receipt: Bool; let trusted: Bool }
    private struct Snapshot: Decodable {
        let windowId: String; let sessionId: String; let uiId: String
        let initialRootReady: Bool; let firstMeaningfulPaint: Bool; let routeReady: Bool
        let statuses: [String: String]; let requestCount: Int; let responseCount: Int
        let pushMessageCount: Int; let flowClientCount: Int; let pointerStages: [String: Pointer]; let errorFree: Bool
        var balanced: Bool { requestCount == responseCount }
        var allPending: Bool { !statuses.isEmpty && statuses.values.allSatisfy { $0.hasSuffix(": pending") } }
        func status(_ name: String) -> String { statuses[name] ?? "" }
    }

    private func click(_ context: FlowIpcWindowContext) {
        phase = .click
        let stage = Self.stages[index], prefix = ownerAction ? Self.ownerPrefix : Self.mainPrefix
        context.webView.evaluateJavaScript(Self.pointerScript(prefix + Self.buttons[index], stage)) { value, error in
            let point: Geometry?
            if error == nil, let json = value as? String, let data = json.data(using: .utf8) {
                point = try? JSONDecoder().decode(Geometry.self, from: data)
            } else { point = nil }
            self.diagnose("pointer-geometry", ["javascriptError": error != nil, "stringResult": value is String,
                "geometryDecoded": point != nil, "pointerReady": point?.ready == true,
                "centerX": point?.centerX ?? 0, "centerY": point?.centerY ?? 0,
                "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow])
            guard let point, point.ready else { self.schedule(); return }
            self.diagnose("activation-enter", ["activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow])
            FlowIpcNativeInputActivation.prepare(context: context, deadline: self.deadline,
                onFailure: { _ in
                    self.diagnose("activation-failed", ["activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow])
                    self.fail("activation")
                }) {
                self.diagnose("activation-ready", ["activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow])
                let location = context.webView.convert(NSPoint(x: point.centerX,
                    y: context.webView.isFlipped ? point.centerY : context.webView.bounds.height - point.centerY), to: nil)
                let number = Int(Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard context.window.contentLayoutRect.contains(location),
                      let down = NSEvent.mouseEvent(with: .leftMouseDown, location: location, modifierFlags: [],
                        timestamp: ProcessInfo.processInfo.systemUptime, windowNumber: context.window.windowNumber,
                        context: nil, eventNumber: number, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(with: .leftMouseUp, location: location, modifierFlags: [],
                        timestamp: ProcessInfo.processInfo.systemUptime + 0.05, windowNumber: context.window.windowNumber,
                        context: nil, eventNumber: number + 1, clickCount: 1, pressure: 0) else { self.fail("pointer"); return }
                var payload = self.scoped(context.scope)
                payload.merge(["stage": stage, "inputMode": "appkit-window-pointer-event", "button": "primary",
                    "webViewX": point.centerX, "webViewY": point.centerY, "windowX": location.x, "windowY": location.y,
                    "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow]) { _, value in value }
                trace("recv", "\(Self.tracePrefix).nativePointerDispatch", payload)
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    context.window.sendEvent(up)
                    self.dispatched.append(stage)
                    self.phase = self.index == 1 || self.index == 5 ? .panel : .observing
                    self.schedule()
                }
            }
        }
    }

    private static func pointerScript(_ id: String, _ stage: String) -> String {
        """
        (() => {
          const button=document.querySelector('#\(id)');
          if (!button || button.disabled) return JSON.stringify({ready:false,centerX:0,centerY:0});
          button.scrollIntoView({block:'center',inline:'center'});
          const r=button.getBoundingClientRect(), centerX=r.left+r.width/2, centerY=r.top+r.height/2;
          const hit=document.elementFromPoint(centerX,centerY);
          if (!(r.width>0 && r.height>0 && (hit===button || button.contains(hit))))
            return JSON.stringify({ready:false,centerX,centerY});
          const marker=window.VatoriWriteExitPointer ||= {stages:{},pending:{}};
          if (marker.pending['\(stage)']) document.removeEventListener('click',marker.pending['\(stage)'],true);
          marker.stages['\(stage)']={receipt:false,trusted:false};
          const handler=e=>{
            if (e.button!==0 || !e.composedPath().includes(button)) return;
            marker.stages['\(stage)']={receipt:true,trusted:e.isTrusted===true};
            document.removeEventListener('click',handler,true); delete marker.pending['\(stage)'];
          };
          marker.pending['\(stage)']=handler; document.addEventListener('click',handler,true);
          return JSON.stringify({ready:true,centerX,centerY});
        })()
        """
    }

    private static func inspection(main: Bool) -> String {
        let prefix = main ? mainPrefix : ownerPrefix
        let route = main ? "flow-file-write-grant-recovery" : "flow-file-write-grant"
        let operations = main ? "['open-secondary','close-secondary']"
            : "['select-new','write-first','invalid-range','write-tail','release','write-after-release','select-existing','select-canceled']"
        return """
        (() => {
          const d=window.VaadinDesktopIpcDiagnostics||{}, c=window.VaadinDesktopHostContext||{};
          const prefix='\(prefix)', route=document.querySelector('#'+prefix+'route'), operations=\(operations);
          return JSON.stringify({windowId:String(c.windowId||''),sessionId:String(c.sessionId||''),uiId:String(c.uiId||''),
            initialRootReady:!!document.querySelector('#framework-root')&&!route,
            firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
            routeReady:!!route&&location.pathname.endsWith('/\(route)')&&operations.every(n=>!!document.querySelector('#'+prefix+n)),
            statuses:Object.fromEntries(operations.map(n=>[n,String(document.querySelector('#'+prefix+n+'-status')?.textContent||'')])),
            requestCount:Number(d.standardBundleOpaqueRequestCount||0),responseCount:Number(d.standardBundleOpaqueResponseCount||0),
            pushMessageCount:Number(d.standardBundlePushMessageCount||0),
            flowClientCount:Object.keys(window.Vaadin?.Flow?.clients||{}).filter(n=>n!=='TypeScript').length,
            pointerStages:window.VatoriWriteExitPointer?.stages||{},
            errorFree:!d.standardBundleBrowserError&&!d.standardBundleOpaqueRequestError&&!d.standardBundleOpaqueResponseError&&!d.standardBundlePushError});
        })()
        """
    }
}
