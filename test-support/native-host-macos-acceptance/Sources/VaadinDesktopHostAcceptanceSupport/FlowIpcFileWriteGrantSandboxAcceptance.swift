import Cocoa
import Darwin
import Foundation
import Security

private var flowIpcFileWriteGrantSandboxAcceptance: FlowIpcFileWriteGrantSandboxAcceptance?

extension WebViewHost {
    func scheduleFileWriteGrantSandboxAcceptanceIfReady() {
        guard config.flowIpcAutoFileWriteGrantSandbox
                || config.flowIpcAutoFileWriteGrantCancelSandbox
                || config.flowIpcAutoFileWriteGrantOwnerCloseSandbox,
              config.transport == .flowIpc else { return }
        if flowIpcFileWriteGrantSandboxAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 300
            flowIpcFileWriteGrantSandboxAcceptance = FlowIpcFileWriteGrantSandboxAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileWriteGrantSandboxAcceptance?.resume()
    }

    func observeFileWriteGrantOwnerCloseSandboxTermination() {
        flowIpcFileWriteGrantSandboxAcceptance?.observeOwnerCloseTermination()
    }
}

/// The unsandboxed runner owns these external fixtures. This driver must never
/// copy, pre-read, remove, or obtain an extension for either path. The create-new
/// denial probe deliberately attempts creation; an unexpected success fails closed
/// and leaves that generated file for runner-owned cleanup.
private final class FlowIpcFileWriteGrantSandboxAcceptance {
    private enum Phase: String { case root, owner, route, click, panel, operation, drain, closing }
    private static let prefix = "framework-flow-file-write-grant-"
    private static let panelTitles = [
        "select-new": "Flow file write grant new file",
        "select-canceled": "Flow file write grant canceled"
    ]
    private static let filename = "vatori-file-write-grant-fixture.bin"
    private static let stages = [
        "select-new", "write-first", "invalid-range", "write-tail",
        "release", "write-after-release", "select-canceled"
    ]
    private static let outcomes = [
        "SELECTED", "WRITTEN", "INVALID_RANGE", "WRITTEN", "RELEASED", "UNAVAILABLE", "CANCELED"
    ]
    private static let cancelStages = [
        "select-new", "write-first", "invalid-range", "write-tail",
        "cancel", "cancel-again", "write-after-cancel"
    ]
    private static let cancelOutcomes = [
        "SELECTED", "WRITTEN", "INVALID_RANGE", "WRITTEN", "CANCELED", "ALREADY_CANCELED", "UNAVAILABLE"
    ]
    private static let entitlementNames: Set<String> = [
        "com.apple.security.app-sandbox",
        "com.apple.security.files.user-selected.read-write",
        "com.apple.security.network.client"
    ]

    private let host: WebViewHost
    private let deadline: Date
    private let selectedURL: URL?
    private let siblingURL: URL?
    private var ownerCloseMode: Bool { host.config.flowIpcAutoFileWriteGrantOwnerCloseSandbox }
    private var cancelMode: Bool { host.config.flowIpcAutoFileWriteGrantCancelSandbox }
    private var tracePrefix: String {
        ownerCloseMode ? "flowFileWriteGrantOwnerCloseSandbox"
            : cancelMode ? "flowFileWriteGrantCancelSandbox" : "flowFileWriteGrantSandbox"
    }
    private var inspectionStages: [String] { cancelMode ? Self.cancelStages : Self.stages }
    private var stages: [String] { ownerCloseMode ? Array(Self.stages.prefix(4)) : inspectionStages }
    private var ownerScope: FlowIpcWindowScope?
    private var ownerClosePrepared = false
    private var finalPointerDispatched = false
    private var diagnosticSignatures: [String: String] = [:]
    private var phase = Phase.root
    private var terminal = false
    private var pollScheduled = false
    private var pollInFlight = false
    private var policyObserved = false
    private var preselectionDenied = false
    private var siblingDeniedAfterWrite = false
    private var windowIdentity: ObjectIdentifier?
    private var webViewIdentity: ObjectIdentifier?
    private var initialPID: Int32 = 0
    private var mainRequestBaseline = 0
    private var mainResponseBaseline = 0
    private var mainPushBaseline = 0
    private var previousStageRequestCount = 0
    private var dispatchedStages: [String] = []
    private var receivedStages: [String] = []
    private var trustedStages: [String] = []
    private var observedStages: [String] = []
    private var currentStageIndex = 0
    private var routeNavigationObserved = false
    private var observedPanels: [NSSavePanel] = []
    private var verifiedPanelStages: [String] = []
    private var drainKey = ""
    private var drainPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        let environment = ProcessInfo.processInfo.environment
        // Runner passes canonical absolute paths; lexical normalization does not
        // access filesystem metadata or consume a Powerbox extension.
        selectedURL = Self.externalURL(environment["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SELECTED_PATH"])
        siblingURL = Self.externalURL(environment["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SIBLING_PATH"])
    }

    private static func externalURL(_ path: String?) -> URL? {
        guard let path, path.hasPrefix("/"), !path.contains("\0") else { return nil }
        return URL(fileURLWithPath: path).standardizedFileURL
    }

    func resume() {
        guard !pollInFlight else { return }
        schedule()
    }

    private func schedule(_ delay: Double = 0.1) {
        pollInFlight = false
        guard !terminal, phase != .closing, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal, !pollInFlight else { return }
        pollInFlight = true
        guard Date() < deadline else { fail("fileWriteGrantSandboxDeadline"); return }
        let windowId = ownerCloseMode && phase != .root ? "file-write-owner" : "main"
        diagnose("poll", [
            "contextPresent": host.flowIpcWindowContext(windowId) != nil,
            "navigationFinished": host.flowIpcWindowContext(windowId)?.navigationFinished == true
        ])
        guard let main = host.flowIpcWindowContext(windowId),
              main.navigationFinished else { schedule(); return }
        if phase != .root && phase != .owner && !retained(main) {
            fail("fileWriteGrantSandboxIdentityChanged"); return
        }
        main.webView.evaluateJavaScript(Self.inspectionScript(cancelMode: cancelMode)) { value, error in
            guard !self.terminal else { return }
            if self.phase != .root && self.phase != .owner && !self.retained(main) {
                self.fail("fileWriteGrantSandboxIdentityChanged"); return
            }
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.diagnose("inspection-unavailable", [
                    "javascriptError": error != nil, "stringResult": value is String
                ])
                self.schedule(); return
            }
            self.diagnose("snapshot", [
                "routeReady": state.routeReady, "allPending": state.allPending,
                "statusCount": state.statuses.count, "existingSelectionPending": state.existingSelectionPending,
                "balanced": state.balanced, "requestCount": state.requestCount,
                "responseCount": state.responseCount, "pushCount": state.pushMessageCount,
                "sheetAttached": main.window.attachedSheet != nil, "panelCount": Self.panels.count,
                "errorFree": state.errorFree
            ])
            guard state.errorFree, state.windowId == main.scope.windowId,
                  state.sessionId == main.scope.sessionId, state.uiId == main.scope.uiId else {
                self.fail("fileWriteGrantSandboxScopeOrRuntimeError"); return
            }
            self.advance(main, state)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        switch phase {
        case .root:
            guard state.initialRootReady, state.firstMeaningfulPaint,
                  state.flowClientCount > 0, state.balanced,
                  main.recoveryGeneration == 0,
                  let process = host.sidecarProcessIdentifier, process > 0 else {
                schedule(); return
            }
            if !ownerCloseMode && !policyObserved {
                guard verifyPolicy(), let selectedURL, let siblingURL else { return }
                guard denied(selectedURL, stage: "target-before", main: main),
                      denied(siblingURL, stage: "sibling-before", main: main) else { return }
                preselectionDenied = true
            }
            initialPID = process
            windowIdentity = ObjectIdentifier(main.window)
            webViewIdentity = ObjectIdentifier(main.webView)
            let navigation = ownerCloseMode ? Self.ownerNavigationScript : Self.navigationScript
            main.webView.evaluateJavaScript(navigation) { value, error in
                guard !self.terminal else { return }
                guard error == nil, value as? Bool == true else { self.schedule(); return }
                self.routeNavigationObserved = true
                self.phase = self.ownerCloseMode ? .owner : .route
                self.schedule(0.3)
            }
        case .owner:
            guard host.flowIpcWindowContext("main") == nil,
                  state.routeReady, state.allPending, state.firstMeaningfulPaint,
                  state.flowClientCount == 1, state.balanced,
                  main.recoveryGeneration == 0,
                  host.sidecarProcessIdentifier == initialPID,
                  main.scope.windowId == "file-write-owner", main.scope.uiId == "file-write-owner",
                  main.scope.sessionId.hasPrefix("flow-ipc-window:"),
                  main.window.attachedSheet == nil, Self.panels.isEmpty else { schedule(); return }
            ownerScope = main.scope
            windowIdentity = ObjectIdentifier(main.window)
            webViewIdentity = ObjectIdentifier(main.webView)
            guard verifyPolicy(), let selectedURL, let siblingURL,
                  denied(selectedURL, stage: "target-before", main: main),
                  denied(siblingURL, stage: "sibling-before", main: main) else { return }
            preselectionDenied = true
            var payload = ownerIdentity
            payload["complete"] = true
            payload["mainWindowRetained"] = false
            payload["ownerWindowRetained"] = true
            payload["firstMeaningfulPaint"] = state.firstMeaningfulPaint
            payload["flowClientCount"] = state.flowClientCount
            payload["requestCount"] = state.requestCount
            payload["responseCount"] = state.responseCount
            payload["pushCount"] = state.pushMessageCount
            payload["browserProgrammaticStages"] = "open-owner-close-main"
            trace("recv", "\(tracePrefix).ownerReady", payload)
            phase = .route
            schedule()
        case .route:
            guard state.routeReady, state.allPending, state.inactiveBranchPending, state.balanced,
                  main.window.attachedSheet == nil, Self.panels.isEmpty else { schedule(); return }
            mainRequestBaseline = state.requestCount
            mainResponseBaseline = state.responseCount
            mainPushBaseline = state.pushMessageCount
            previousStageRequestCount = state.requestCount
            dispatchCurrent(main)
        case .click:
            guard state.routeReady else { schedule(); return }
            dispatchCurrent(main)
        case .panel:
            observePanel(main, state)
        case .operation:
            let stage = stages[currentStageIndex]
            guard state.routeReady, record(stage, state), statusMatches(state, stage),
                  observedStages.allSatisfy({ statusMatches(state, $0) }),
                  state.existingSelectionPending, state.inactiveBranchPending,
                  inspectionStages.dropFirst(currentStageIndex + 1).allSatisfy({
                    state.status($0) == "\($0): pending"
                  }), main.window.attachedSheet == nil, Self.panels.isEmpty,
                  state.balanced, state.requestCount > previousStageRequestCount else {
                resetDrain(); schedule(); return
            }
            let key = "\(stage):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            emitStage(main, state, stage)
            observedStages.append(stage)
            if stage == "write-tail" {
                guard let siblingURL,
                      denied(siblingURL, stage: "sibling-after", main: main) else { return }
                siblingDeniedAfterWrite = true
            }
            previousStageRequestCount = state.requestCount
            resetDrain()
            if currentStageIndex == stages.count - 1 {
                phase = .drain
                schedule(0.2)
            } else {
                currentStageIndex += 1
                dispatchCurrent(main)
            }
        case .drain:
            guard state.routeReady, state.balanced, state.existingSelectionPending, state.inactiveBranchPending,
                  observedStages == stages,
                  observedStages.allSatisfy({ statusMatches(state, $0) }),
                  main.window.attachedSheet == nil, Self.panels.isEmpty else {
                resetDrain(); schedule(); return
            }
            let key = "final:\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            if ownerCloseMode { prepareOwnerClose(main, state) }
            else { finish(main, state) }
        case .closing:
            break
        }
    }

    private func verifyPolicy() -> Bool {
        var code: SecCode?
        var staticCode: SecStaticCode?
        var information: CFDictionary?
        guard SecCodeCopySelf(SecCSFlags(), &code) == errSecSuccess,
              let code,
              SecCodeCopyStaticCode(code, SecCSFlags(), &staticCode) == errSecSuccess,
              let staticCode,
              SecCodeCopySigningInformation(staticCode,
                SecCSFlags(rawValue: kSecCSSigningInformation), &information) == errSecSuccess,
              let details = information as? [String: Any],
              let entitlements = details[kSecCodeInfoEntitlementsDict as String] as? [String: Any],
              Set(entitlements.keys) == Self.entitlementNames,
              entitlements.values.allSatisfy({
                CFGetTypeID($0 as CFTypeRef) == CFBooleanGetTypeID() && ($0 as? Bool) == true
              }) else {
            fail("fileWriteGrantSandboxEntitlementsMismatch"); return false
        }
        guard let selectedURL, let siblingURL,
              selectedURL.lastPathComponent == Self.filename,
              siblingURL.lastPathComponent == "unselected-sibling.txt",
              selectedURL != siblingURL,
              selectedURL.deletingLastPathComponent() == siblingURL.deletingLastPathComponent(),
              outside(selectedURL, parent: FileManager.default.homeDirectoryForCurrentUser),
              outside(siblingURL, parent: FileManager.default.homeDirectoryForCurrentUser),
              outside(selectedURL, parent: Bundle.main.bundleURL),
              outside(siblingURL, parent: Bundle.main.bundleURL) else {
            fail("fileWriteGrantSandboxExternalFixtureInvalid"); return false
        }
        policyObserved = true
        trace("recv", "\(tracePrefix).policyObserved", [
            "complete": true, "appSandbox": true, "userSelectedReadWrite": true,
            "networkClient": true, "exactEntitlements": true, "entitlementCount": 3,
            "hostProcessIdentifier": getpid(), "userIdentifier": getuid(),
            "externalFixture": true, "targetOutsideContainer": true,
            "siblingOutsideContainer": true, "distinctSibling": true
        ])
        return true
    }

    private func outside(_ url: URL, parent: URL) -> Bool {
        let path = parent.standardizedFileURL.path
        return url.path != path && !url.path.hasPrefix(path + "/")
    }

    private func denied(_ url: URL, stage: String, main: FlowIpcWindowContext) -> Bool {
        errno = 0
        let creating = stage == "target-before"
        let descriptor = creating
            ? Darwin.open(url.path, O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC | O_NOFOLLOW, mode_t(0o600))
            : Darwin.open(url.path, O_RDONLY | O_CLOEXEC | O_NOFOLLOW)
        let errorNumber = descriptor < 0 ? errno : 0
        if descriptor >= 0 { Darwin.close(descriptor) }
        let rejected = descriptor < 0 && errorNumber == EPERM
        trace("recv", "\(tracePrefix).accessProbe", [
            "complete": rejected, "stage": stage, "operation": creating ? "create-new" : "open-read",
            "denied": rejected, "errno": errorNumber, "noFollow": true,
            "hostProcessIdentifier": getpid(), "windowId": main.scope.windowId,
            "sessionId": main.scope.sessionId, "uiId": main.scope.uiId
        ])
        guard rejected else { fail("fileWriteGrantSandboxAccessNotDenied"); return false }
        return true
    }

    private func dispatchCurrent(_ main: FlowIpcWindowContext) {
        phase = .click
        let stage = stages[currentStageIndex]
        click(main, stage, Self.panelTitles[stage] != nil ? .panel : .operation)
    }

    private func retained(_ main: FlowIpcWindowContext) -> Bool {
        let expected = ownerCloseMode ? ownerScope : FlowIpcWindowScope.main
        return windowIdentity == ObjectIdentifier(main.window)
            && webViewIdentity == ObjectIdentifier(main.webView)
            && main.recoveryGeneration == 0 && host.sidecarProcessIdentifier == initialPID
            && main.scope.windowId == expected?.windowId && main.scope.sessionId == expected?.sessionId
            && main.scope.uiId == expected?.uiId
            && (!ownerCloseMode || host.flowIpcWindowContext("main") == nil)
    }

    private func observePanel(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        let stage = stages[currentStageIndex]
        guard Self.field("outcome", state.status(stage)) == nil else {
            fail("fileWriteGrantSandboxPanelCompletedBeforeObservation"); return
        }
        guard record(stage, state), let selectedURL,
              let panel = main.window.attachedSheet as? NSSavePanel,
              !(panel is NSOpenPanel), panel.title == Self.panelTitles[stage],
              panel.sheetParent === main.window, panel.isVisible,
              Self.panels.count == 1, Self.panels.first === panel else { schedule(0.05); return }
        if observedPanels.count == verifiedPanelStages.count {
            observedPanels.append(panel)
            trace("recv", "\(tracePrefix).panelReady", [
                "stage": stage, "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        } else if observedPanels.last !== panel {
            fail("fileWriteGrantSandboxUnexpectedPanelReplacement"); return
        }
        let expectedURL = stage == "select-new" ? selectedURL
            : selectedURL.deletingLastPathComponent().appendingPathComponent("canceled-write-grant.bin")
        // Passive URL comparison only. The external operator saves the new
        // target or cancels the second panel; no programmatic confirmation.
        guard panel.url?.standardizedFileURL == expectedURL else { schedule(0.05); return }
        verifiedPanelStages.append(stage)
        trace("recv", "\(tracePrefix).panelObserved", [
            "complete": true, "stage": stage, "title": Self.panelTitles[stage] ?? "",
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true, "inputMode": "external-native-panel-selection",
            "physicalInput": false, "windowId": main.scope.windowId,
            "sessionId": main.scope.sessionId, "uiId": main.scope.uiId
        ])
        phase = .operation
        schedule()
    }

    private func matches(_ state: Snapshot, _ stage: String, _ expected: [String: String]) -> Bool {
        if cancelMode {
            guard state.status(stage).hasPrefix(stage + ": ") else { return false }
            let names = state.status(stage).split(separator: " ").filter { $0.contains("=") }
                .map { String($0.prefix { $0 != "=" }) }
            guard Set(names).count == names.count else { return false }
        }
        return expected.allSatisfy { Self.field($0.key, state.status(stage)) == $0.value }
    }

    private func statusMatches(_ state: Snapshot, _ stage: String) -> Bool {
        guard let index = inspectionStages.firstIndex(of: stage), matches(state, stage, [
            "outcome": (cancelMode ? Self.cancelOutcomes : Self.outcomes)[index], "client": "true", "change": "1", "listenerCount": "1",
            "capabilityContextCaptured": "true", "backgroundSessionAbsent": "true",
            "permissionAllowed": "true", "publicTokenHidden": "true"
        ]) else { return false }
        let canceled = stage == "select-canceled"
        guard matches(state, stage, [
            "grantPresent": String(!canceled), "filenameMatches": String(!canceled),
            "maxWriteBytes": canceled ? "-1" : "65536", "securityScoped": String(!canceled)
        ]) else { return false }
        if Self.panelTitles[stage] != nil {
            return matches(state, stage, [
                "noPath": "true", "createNewOnly": "true", "originalGrantRetained": "true"
            ])
        }
        guard matches(state, stage, ["sameGrantFacade": "true"]) else { return false }
        if stage == "cancel" || stage == "cancel-again" {
            return matches(state, stage, [
                "canceled": "true", "cancellationRequested": "true", "grantUnavailable": "true",
                "grantState": "unavailable", "nextOffset": "65542", "noPath": "true", "securityScopeStopped": "true"
            ])
        }
        if stage == "release" {
            return matches(state, stage, ["released": "true", "securityScopeStopped": "true"])
        }
        guard let write = Self.writeExpectations[stage] else { return false }
        return matches(state, stage, [
            "requestedOffset": String(write[0]), "requestedBytes": String(write[1]),
            "bytesWritten": String(write[2]), "offset": String(write[3]), "nextOffset": String(write[4])
        ])
    }

    private static let writeExpectations: [String: [Int]] = [
        "write-first": [0, 65_536, 65_536, 0, 65_536],
        "invalid-range": [-1, 1, 0, -1, -1],
        "write-tail": [65_536, 6, 6, 65_536, 65_542],
        "write-after-release": [65_542, 1, 0, -1, -1],
        "write-after-cancel": [65_542, 1, 0, -1, -1]
    ]

    private func emitStage(_ main: FlowIpcWindowContext, _ state: Snapshot, _ stage: String) {
        let canceled = stage == "select-canceled"
        var payload: [String: Any] = [
            "complete": true, "stage": stage,
            "outcome": Self.field("outcome", state.status(stage)) ?? "",
            "client": true, "change": 1, "listenerCount": 1,
            "capabilityContextCaptured": true, "backgroundSessionAbsent": true,
            "permissionAllowed": true, "publicTokenHidden": true,
            "grantPresent": !canceled, "maxWriteBytes": canceled ? -1 : 65_536,
            "filenameMatches": !canceled, "securityScoped": !canceled,
            "hostProcessIdentifier": getpid(), "processIdentifier": host.sidecarProcessIdentifier ?? 0,
            "windowId": main.scope.windowId, "sessionId": main.scope.sessionId, "uiId": main.scope.uiId,
            "requestCount": state.requestCount, "responseCount": state.responseCount,
            "pushCount": state.pushMessageCount, "balancedDrainObserved": drainPolls >= 2,
            "nativePointerObserved": true, "nativePointerTrustedObserved": true
        ]
        if Self.panelTitles[stage] != nil {
            payload["noPath"] = true
            payload["createNewOnly"] = true
            payload["originalGrantRetained"] = true
        } else {
            payload["sameGrantFacade"] = true
            if stage == "cancel" || stage == "cancel-again" {
                payload["canceled"] = true
                payload["cancellationRequested"] = true
                payload["grantUnavailable"] = true
                payload["grantState"] = "unavailable"
                payload["nextOffset"] = 65_542
                payload["noPath"] = true
                payload["securityScopeStopped"] = true
                payload["localRepeat"] = stage == "cancel-again"
            } else if stage == "release" {
                payload["released"] = true
                payload["securityScopeStopped"] = true
            } else if let write = Self.writeExpectations[stage] {
                payload["requestedOffset"] = write[0]
                payload["requestedBytes"] = write[1]
                payload["bytesWritten"] = write[2]
                payload["offset"] = write[3]
                payload["nextOffset"] = write[4]
            }
        }
        trace("recv", "\(tracePrefix).stageObserved", payload)
    }

    private func quietPoll(_ key: String) -> Bool {
        drainPolls = key == drainKey ? drainPolls + 1 : 1
        drainKey = key
        return drainPolls >= 2
    }

    private func resetDrain() { drainKey = ""; drainPolls = 0 }

    private static var panels: [NSSavePanel] {
        NSApplication.shared.windows.compactMap { $0 as? NSSavePanel }.filter(\.isVisible)
    }

    private static func field(_ name: String, _ status: String) -> String? {
        let prefix = name + "="
        return status.split(separator: " ").first { $0.hasPrefix(prefix) }
            .map { String($0.dropFirst(prefix.count)) }
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
            "code": code, "phase": phase.rawValue, "stage": stages[currentStageIndex]
        ])
        if ownerCloseMode {
            NSApp.perform(#selector(NSApplication.terminate(_:)), with: nil, afterDelay: 0)
        } else {
            host.flowIpcWindowContext("main")?.window.close()
        }
    }

    /// Bounded, path-free diagnostics for the independent owner-close mode.
    /// A repeated waiting state emits once, without weakening any acceptance guard.
    private func diagnose(_ reason: String, _ values: [String: Any]) {
        guard ownerCloseMode else { return }
        var payload = ownerIdentity
        payload["phase"] = phase.rawValue
        payload["stage"] = stages[currentStageIndex]
        payload["reason"] = reason
        values.forEach { payload[$0.key] = $0.value }
        guard let data = try? JSONSerialization.data(withJSONObject: payload, options: [.sortedKeys]),
              let signature = String(data: data, encoding: .utf8),
              diagnosticSignatures[reason] != signature else { return }
        diagnosticSignatures[reason] = signature
        trace("recv", "\(tracePrefix).driverDiagnostic", payload)
    }

    private var ownerIdentity: [String: Any] {
        var payload = ownerScope?.tracePayload ?? [:]
        payload["hostProcessIdentifier"] = getpid()
        payload["processIdentifier"] = initialPID
        payload["recoveryGeneration"] = 0
        return payload
    }

    private var originalSidecarRunning: Bool {
        initialPID > 0 && host.sidecarProcessIdentifier == initialPID
            && Darwin.kill(initialPID, 0) == 0
    }

    private func prepareOwnerClose(_ owner: FlowIpcWindowContext, _ state: Snapshot) {
        let unusedPending = Self.stages.dropFirst(4).allSatisfy {
            state.status($0) == "\($0): pending"
        }
        guard retained(owner), state.closeActiveEnabled == true, unusedPending,
              observedStages == stages, dispatchedStages == stages,
              receivedStages == stages, trustedStages == stages,
              verifiedPanelStages == ["select-new"], observedPanels.count == 1,
              policyObserved, preselectionDenied, siblingDeniedAfterWrite,
              routeNavigationObserved, state.pushMessageCount > mainPushBaseline,
              state.flowClientCount == 1, state.firstMeaningfulPaint,
              originalSidecarRunning else { schedule(); return }
        var payload = ownerIdentity
        payload["complete"] = true
        payload["mainWindowRetained"] = false
        payload["ownerWindowRetained"] = true
        payload["originalSidecarRunning"] = true
        payload["routeReady"] = state.routeReady
        payload["firstMeaningfulPaint"] = state.firstMeaningfulPaint
        payload["flowClientCount"] = state.flowClientCount
        payload["requestBaseline"] = mainRequestBaseline
        payload["responseBaseline"] = mainResponseBaseline
        payload["pushBaseline"] = mainPushBaseline
        payload["requestCount"] = state.requestCount
        payload["responseCount"] = state.responseCount
        payload["pushCount"] = state.pushMessageCount
        payload["balancedDrainObserved"] = state.balanced && drainPolls >= 2
        payload["stageCount"] = observedStages.count
        payload["observedStages"] = observedStages.joined(separator: ",")
        payload["nativePointerEventCount"] = dispatchedStages.count
        payload["nativePointerTrustedEventCount"] = trustedStages.count
        payload["panelCount"] = observedPanels.count
        payload["panelURLsVerified"] = true
        payload["externalPanelSelection"] = true
        payload["exactEntitlementsObserved"] = policyObserved
        payload["preselectionDenied"] = preselectionDenied
        payload["siblingDeniedAfterWrite"] = siblingDeniedAfterWrite
        payload["writtenByteCount"] = 65_542
        payload["releasePending"] = true
        payload["writeAfterReleasePending"] = true
        payload["existingSelectionPending"] = state.existingSelectionPending
        payload["canceledSelectionPending"] = true
        payload["closeActiveEnabled"] = true
        payload["browserProgrammaticStages"] = "open-owner-close-main"
        payload["physicalInput"] = false
        ownerClosePrepared = true
        trace("recv", "\(tracePrefix).prepared", payload)
        // No final DOM observation: the public Java listener closes this owner.
        // The title/close command results and termination observer are the proof.
        phase = .closing
        click(owner, "close-active", .closing)
    }

    func observeOwnerCloseTermination() {
        guard ownerCloseMode else { return }
        var payload = ownerIdentity
        let mainRetained = host.flowIpcWindowContext("main") != nil
        let ownerRetained = host.flowIpcWindowContext("file-write-owner") != nil
        payload["mainWindowRetained"] = mainRetained
        payload["ownerWindowRetained"] = ownerRetained
        errno = 0
        let sidecarAbsent = initialPID > 0 && host.sidecarProcessIdentifier == nil
            && Darwin.kill(initialPID, 0) == -1 && errno == ESRCH
        payload["currentProcessIdentifier"] = host.sidecarProcessIdentifier ?? 0
        payload["originalSidecarAbsent"] = sidecarAbsent
        payload["preparationObserved"] = ownerClosePrepared
        payload["finalPointerDispatched"] = finalPointerDispatched
        // AppKit can still list the closing window inside windowWillClose.
        payload["visibleWindowCount"] = NSApp.windows.filter { $0.isVisible }.count
        payload["complete"] = ownerClosePrepared && finalPointerDispatched
            && !mainRetained && !ownerRetained && sidecarAbsent && !terminal
        trace("recv", "\(tracePrefix).applicationWillTerminate", payload)
        terminal = true
    }

    private func finish(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        let pointers = dispatchedStages == stages && receivedStages == stages
        let trusted = trustedStages == stages
        let statuses = observedStages == stages
        let expectedPanels = cancelMode ? ["select-new"] : ["select-new", "select-canceled"]
        let complete = statuses && pointers && trusted && retained(main)
            && drainPolls >= 2 && verifiedPanelStages == expectedPanels && observedPanels.count == expectedPanels.count && routeNavigationObserved
            && state.errorFree && state.balanced && state.pushMessageCount > mainPushBaseline
            && policyObserved && preselectionDenied && siblingDeniedAfterWrite && state.inactiveBranchPending
        var payload: [String: Any] = [
            "complete": complete, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.rootRendered, "flowClientOwned": state.flowClientCount > 0,
            "externalFixture": true, "writtenByteCount": 65_542,
            "exactEntitlementsObserved": policyObserved, "preselectionDenied": preselectionDenied,
            "siblingDeniedAfterWrite": siblingDeniedAfterWrite, "sandboxEnforced": complete,
            "securityScopedObserved": statuses,
            "allStageStatusesObserved": statuses, "observedStages": observedStages.joined(separator: ","),
            "stageCount": observedStages.count, "typedGrantApiObserved": statuses, "boundedWritesObserved": statuses,
            "existingSelectionPending": state.existingSelectionPending, "existingTargetPanelObserved": false,
            "hostProcessIdentifier": getpid(), "initialProcessIdentifier": initialPID,
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
            "balancedDrainObserved": drainPolls >= 2, "resultStableAfterDrain": complete,
            "realSavePanelObserved": observedPanels.count == expectedPanels.count, "panelCount": observedPanels.count,
            "panelURLsVerified": verifiedPanelStages == expectedPanels,
            "externalPanelSelection": verifiedPanelStages == expectedPanels,
            "nativePointerObserved": pointers, "nativePointerTrustedObserved": trusted,
            "nativePointerEventCount": dispatchedStages.count,
            "nativePointerTrustedEventCount": trustedStages.count,
            "nativePointerStages": dispatchedStages.joined(separator: ","),
            "browserProgrammaticSetupObserved": routeNavigationObserved,
            "browserProgrammaticStages": "route-navigate", "physicalInput": false,
            "errorFree": state.errorFree, "gracefulCloseRequested": true,
            "recoveryGeneration": main.recoveryGeneration
        ]
        if cancelMode {
            payload["settledCancellationObserved"] = statuses
            payload["localRepeatCancellationObserved"] = statuses
            payload["writeAfterCancelUnavailableObserved"] = statuses
            payload["cancelFinalOffset"] = 65_542
            payload["releasePending"] = state.releasePending
            payload["writeAfterReleasePending"] = state.writeAfterReleasePending
            payload["canceledSelectionPending"] = state.canceledSelectionPending
            payload["inFlightCancellationClaimed"] = false
            payload["resumeClaimed"] = false
        }
        trace("recv", "\(tracePrefix).frameworkAcceptance", payload)
        terminal = true
        main.window.close()
    }

    private struct PointerGeometry: Decodable {
        let ready: Bool
        let centerX: Double
        let centerY: Double
        let failure: String?
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
        let existingSelectionPending: Bool
        let inactiveBranchPending: Bool
        let releasePending: Bool
        let writeAfterReleasePending: Bool
        let canceledSelectionPending: Bool
        let closeActiveEnabled: Bool?
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let pointerStages: [String: PointerObservation]
        let errorFree: Bool
        var balanced: Bool { requestCount == responseCount }
        func status(_ operation: String) -> String { statuses[operation] ?? "" }
        var allPending: Bool {
            existingSelectionPending && statuses.count == 7 && statuses.values.allSatisfy { $0.hasSuffix(": pending") }
        }
    }

    private func click(_ context: FlowIpcWindowContext, _ stage: String, _ next: Phase) {
        context.webView.evaluateJavaScript(Self.pointerScript(stage)) { value, error in
            guard !self.terminal, Date() < self.deadline, self.retained(context) else { return }
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(PointerGeometry.self, from: data),
                  point.ready else {
                let geometry: PointerGeometry?
                if let json = value as? String, let data = json.data(using: .utf8) {
                    geometry = try? JSONDecoder().decode(PointerGeometry.self, from: data)
                } else { geometry = nil }
                self.diagnose("pointer-unavailable", [
                    "javascriptError": error != nil, "stringResult": value is String,
                    "geometryDecoded": geometry != nil, "pointerReady": geometry?.ready == true,
                    "pointerFailure": geometry?.failure ?? "decode-failed",
                    "centerX": geometry?.centerX ?? 0, "centerY": geometry?.centerY ?? 0
                ])
                if next == .closing { self.fail("fileWriteGrantOwnerClosePointerNotReady") }
                else { self.schedule() }
                return
            }
            self.diagnose("pointer-ready", ["centerX": point.centerX, "centerY": point.centerY,
                "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow])
            FlowIpcNativeInputActivation.prepare(context: context, deadline: self.deadline,
                onFailure: { state in
                    self.diagnose("activation-failed", [
                        "activeApplication": state["activeApplication"] as? Bool ?? false,
                        "keyWindow": state["keyWindow"] as? Bool ?? false
                    ])
                    self.fail("fileWriteGrantSandboxActivationFailed")
                }) {
                guard !self.terminal, Date() < self.deadline, self.retained(context) else { return }
                self.diagnose("activation-ready", [
                    "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow
                ])
                let location = context.webView.convert(NSPoint(x: point.centerX,
                    y: context.webView.isFlipped ? point.centerY
                        : context.webView.bounds.height - point.centerY), to: nil)
                guard context.window.contentLayoutRect.contains(location) else {
                    self.fail("fileWriteGrantSandboxPointerOutsideWindow"); return
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
                    self.fail("fileWriteGrantSandboxPointerCreationFailed"); return
                }
                trace("recv", "\(self.tracePrefix).nativePointerDispatch", [
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
                    guard !self.terminal, Date() < self.deadline, self.retained(context) else { return }
                    context.window.sendEvent(up)
                    self.dispatchedStages.append(stage)
                    self.phase = next
                    if next == .closing {
                        self.finalPointerDispatched = true
                        // This independent test-only deadline also catches a
                        // starved main run loop during deferred AppKit termination.
                        DispatchQueue.global(qos: .utility).asyncAfter(deadline: .now() + 5) {
                            trace("send", "error", [
                                "code": "fileWriteGrantOwnerCloseFailed",
                                "reason": "public-owner-close-did-not-terminate"
                            ])
                            fflush(stdout)
                            Darwin.exit(1)
                        }
                    } else {
                        self.schedule()
                    }
                }
            }
        }
    }

    private static func pointerScript(_ stage: String) -> String {
        """
        (() => {
          const button = document.querySelector('#\(prefix)\(stage)');
          if (!button || ('\(stage)' === 'close-active' && button.disabled))
            return JSON.stringify({ready:false,centerX:0,centerY:0,
              failure:!button?'missing-button':'disabled'});
          button.scrollIntoView({block:'center',inline:'center'});
          const rect = button.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2, centerY = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(centerX,centerY);
          if (!(rect.width > 0 && rect.height > 0) || !(hit === button || button.contains(hit))) {
            return JSON.stringify({ready:false,centerX,centerY,
              failure:!(rect.width>0 && rect.height>0)?'empty-rect':'occluded'});
          }
          const marker = window.VatoriGrantSandboxPointer ||= {stages:{},pending:{}};
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
      const button = document.querySelector('#framework-flow-file-write-grant-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let ownerNavigationScript = """
    (() => {
      const button = document.querySelector('#framework-flow-file-write-grant-owner-close-open');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static func inspectionScript(cancelMode: Bool) -> String {
        let operations = (cancelMode ? cancelStages : stages).map { "'\($0)'" }.joined(separator: ",")
        return """
    (() => {
      const d = window.VaadinDesktopIpcDiagnostics || {};
      const c = window.VaadinDesktopHostContext || {};
      const prefix = 'framework-flow-file-write-grant-';
      const route = document.querySelector('#'+prefix+'route');
      const operations = [\(operations)];
      const statuses = Object.fromEntries(operations.map(name => [name,
        String(document.querySelector('#'+prefix+name+'-status')?.textContent || '')]));
      const inactive = \(cancelMode ? "['release','write-after-release','select-canceled']" : "['cancel','cancel-again','write-after-cancel']");
      const inactiveBranchPending = inactive.every(name =>
        document.querySelector('#'+prefix+name+'-status')?.textContent === name+': pending');
      return JSON.stringify({
        windowId:String(c.windowId || ''),sessionId:String(c.sessionId || ''),uiId:String(c.uiId || ''),
        rootRendered:!!document.querySelector('#framework-root') || !!route,
        initialRootReady:!!document.querySelector('#framework-root') && !route,
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:!!route && window.location.pathname.endsWith('/flow-file-write-grant')
          && operations.every(name => !!document.querySelector('#'+prefix+name)),
        statuses,
        inactiveBranchPending,
        releasePending:document.querySelector('#'+prefix+'release-status')?.textContent === 'release: pending',
        writeAfterReleasePending:document.querySelector('#'+prefix+'write-after-release-status')?.textContent === 'write-after-release: pending',
        canceledSelectionPending:document.querySelector('#'+prefix+'select-canceled-status')?.textContent === 'select-canceled: pending',
        existingSelectionPending:document.querySelector('#'+prefix+'select-existing-status')?.textContent === 'select-existing: pending',
        closeActiveEnabled:!!document.querySelector('#'+prefix+'close-active')
          && document.querySelector('#'+prefix+'close-active').disabled === false,
        requestCount:Number(d.standardBundleOpaqueRequestCount || 0),
        responseCount:Number(d.standardBundleOpaqueResponseCount || 0),
        pushMessageCount:Number(d.standardBundlePushMessageCount || 0),
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients || {}).filter(name => name !== 'TypeScript').length,
        pointerStages:window.VatoriGrantSandboxPointer?.stages || {},
        errorFree:!d.standardBundleBrowserError && !d.standardBundleOpaqueRequestError
          && !d.standardBundleOpaqueResponseError && !d.standardBundlePushError
      });
    })()
    """
    }
}
