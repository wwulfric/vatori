import Cocoa
import Darwin
import Foundation
import Security

private var fileWriteGrantIsolationSandboxAcceptance: FlowIpcFileWriteGrantIsolationSandboxAcceptance?

extension WebViewHost {
    func scheduleFileWriteGrantIsolationSandboxAcceptanceIfReady() {
        guard config.flowIpcAutoFileWriteGrantIsolationSandbox, config.transport == .flowIpc else { return }
        if fileWriteGrantIsolationSandboxAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 300
            fileWriteGrantIsolationSandboxAcceptance = FlowIpcFileWriteGrantIsolationSandboxAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        fileWriteGrantIsolationSandboxAcceptance?.resume()
    }
}

/// Exercises two independently captured public Java bridge contexts. This driver
/// never sees the token or writes fixtures; the external runner owns both paths.
private final class FlowIpcFileWriteGrantIsolationSandboxAcceptance {
    private enum Phase: String { case root, route, click, panel, observing }
    private static let tracePrefix = "flowFileWriteGrantIsolationSandbox"
    private static let prefix = "framework-flow-file-write-grant-isolation-"
    private static let ownerId = "file-write-grant-isolation-secondary"
    private static let panelTitle = "Flow file write grant owner isolation"
    private static let stages = [
        "main-open-secondary", "secondary-select", "secondary-owner-write-first",
        "main-foreign-write", "main-foreign-release", "secondary-owner-write-tail",
        "secondary-owner-release", "main-close-secondary"
    ]
    private static let buttons = [
        "open-secondary", "select", "owner-write-first", "foreign-write",
        "foreign-release", "owner-write-tail", "owner-release", "close-secondary"
    ]
    private static let ownerStages: Set<Int> = [1, 2, 5, 6]
    private static let callbackFields = [
        "capabilityContextCaptured": "true", "backgroundSessionAbsent": "true",
        "client": "true", "change": "1", "listenerCount": "1", "permissionAllowed": "true"
    ]
    private static let expected: [[String: String]] = [
        ["opened": "true"],
        ["outcome": "SELECTED", "grantPresent": "true", "maxWriteBytes": "65536",
         "securityScoped": "true", "filenameMatches": "true", "noPath": "true", "createNewOnly": "true"],
        ["outcome": "WRITTEN", "success": "true", "sameTokenUsed": "true", "requestedOffset": "0",
         "requestedBytes": "65536", "bytesWritten": "65536", "offset": "0", "nextOffset": "65536",
         "maxWriteBytes": "65536", "grantUnavailable": "false", "noPath": "true"],
        ["outcome": "UNAVAILABLE", "unavailable": "true", "grantUnavailable": "true", "noContent": "true",
         "noWriteProgress": "true", "sameTokenUsed": "true", "ownerTokenUnchanged": "true",
         "requestedOffset": "65536", "requestedBytes": "6"],
        ["outcome": "UNAVAILABLE", "unavailable": "true", "grantUnavailable": "true", "noContent": "true",
         "noWriteProgress": "true", "sameTokenUsed": "true", "ownerTokenUnchanged": "true"],
        ["outcome": "WRITTEN", "success": "true", "sameTokenUsed": "true", "requestedOffset": "65536",
         "requestedBytes": "6", "bytesWritten": "6", "offset": "65536", "nextOffset": "65542",
         "maxWriteBytes": "65536", "grantUnavailable": "false", "noPath": "true"],
        ["outcome": "RELEASED", "success": "true", "sameTokenUsed": "true", "released": "true",
         "securityScopeStopped": "true", "nextOffset": "65542", "tokenCleared": "true"],
        ["closed": "true", "sharedStateCleared": "true"]
    ]

    private let host: WebViewHost
    private let deadline: Date
    private let selectedURL: URL?
    private let siblingURL: URL?
    private var phase = Phase.root
    private var index = 0
    private var terminal = false
    private var scheduled = false
    private var inFlight = false
    private var initialPID: Int32 = 0
    private var mainWindowIdentity: ObjectIdentifier?
    private var mainWebViewIdentity: ObjectIdentifier?
    private var ownerWindowIdentity: ObjectIdentifier?
    private var ownerWebViewIdentity: ObjectIdentifier?
    private var ownerScope: FlowIpcWindowScope?
    private var mainBaseline: Snapshot?
    private var ownerBaseline: Snapshot?
    private var ownerFinal: Snapshot?
    private var previousMainRequests = 0
    private var previousOwnerRequests = 0
    private var dispatched: [String] = []
    private var observed: [String] = []
    private var routeNavigated = false
    private var policyObserved = false
    private var preselectionDenied = false
    private var siblingDeniedAfterWrite = false
    private var liveDrainObserved = false
    private var observedPanel: NSSavePanel?
    private var panelVerified = false
    private var quietKey = ""
    private var quietCount = 0
    private var diagnosticSignatures: [String: String] = [:]

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        let environment = ProcessInfo.processInfo.environment
        selectedURL = Self.externalURL(environment["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SELECTED_PATH"])
        siblingURL = Self.externalURL(environment["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SIBLING_PATH"])
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
        diagnose("poll", [
            "mainContextPresent": host.flowIpcWindowContext("main") != nil,
            "mainNavigationFinished": host.flowIpcWindowContext("main")?.navigationFinished == true,
            "secondaryContextPresent": host.flowIpcWindowContext(Self.ownerId) != nil,
            "secondaryNavigationFinished": host.flowIpcWindowContext(Self.ownerId)?.navigationFinished == true
        ])
        guard let main = host.flowIpcWindowContext("main"), main.navigationFinished else { schedule(); return }
        if phase != .root && !mainRetained(main) { fail("main-identity"); return }
        inspect(main) { state in
            guard let state else { self.schedule(); return }
            guard self.valid(state, main), state.errorFree else { self.fail("main-scope-or-runtime"); return }
            if self.phase == .root {
                self.navigate(main, state)
                return
            }
            if self.phase == .route {
                guard state.routeReady, state.allPending, state.balanced else { self.schedule(); return }
                self.mainBaseline = state
                self.previousMainRequests = state.requestCount
                self.click(main)
                return
            }
            if let owner = self.host.flowIpcWindowContext(Self.ownerId), owner.navigationFinished {
                if self.ownerScope != nil && !self.ownerRetained(owner) { self.fail("owner-identity"); return }
                self.inspect(owner) { ownerState in
                    guard let ownerState else { self.schedule(); return }
                    guard self.valid(ownerState, owner), ownerState.errorFree else {
                        self.fail("owner-scope-or-runtime"); return
                    }
                    // WK navigation completion precedes the official Flow client's
                    // initial route render. The opening stage must wait for it.
                    if self.index == 0 && !ownerState.routeReady { self.schedule(); return }
                    guard ownerState.routeReady else { self.fail("owner-route-missing"); return }
                    self.advance(main, state, owner, ownerState)
                }
            } else if self.index == 0 || self.index == 7 {
                self.advance(main, state, nil, nil)
            } else {
                self.fail("owner-context-missing")
            }
        }
    }

    private func navigate(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        guard state.initialRootReady, state.firstMeaningfulPaint, state.flowClientCount == 1,
              state.balanced, main.recoveryGeneration == 0,
              let pid = host.sidecarProcessIdentifier, pid > 0 else { schedule(); return }
        initialPID = pid
        mainWindowIdentity = ObjectIdentifier(main.window)
        mainWebViewIdentity = ObjectIdentifier(main.webView)
        main.webView.evaluateJavaScript("""
            (() => {
              const b=document.querySelector('#framework-flow-file-write-grant-isolation-open');
              if (!b) return false;
              b.click(); return true;
            })()
            """) { value, error in
            guard error == nil, value as? Bool == true else { self.schedule(); return }
            self.routeNavigated = true
            self.phase = .route
            self.schedule(0.3)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ mainState: Snapshot,
                         _ owner: FlowIpcWindowContext?, _ ownerState: Snapshot?) {
        if phase == .click {
            guard let target = Self.ownerStages.contains(index) ? owner : main else { schedule(); return }
            click(target)
            return
        }
        if phase == .panel {
            guard let owner, let ownerState else { fail("panel-owner-missing"); return }
            observePanel(owner, ownerState)
            return
        }
        if index == 0 {
            guard let owner, let ownerState, ownerState.allPending,
                  ownerState.firstMeaningfulPaint, ownerState.flowClientCount == 1,
                  ownerState.balanced, owner.recoveryGeneration == 0,
                  owner.scope.windowId == Self.ownerId, owner.scope.uiId == Self.ownerId,
                  Self.opaqueSession(owner.scope.sessionId) else { schedule(); return }
            if ownerScope == nil {
                ownerScope = owner.scope
                ownerWindowIdentity = ObjectIdentifier(owner.window)
                ownerWebViewIdentity = ObjectIdentifier(owner.webView)
                ownerBaseline = ownerState
                previousOwnerRequests = ownerState.requestCount
            }
        }
        let retainedOwnerState = ownerState ?? (index == 7 ? ownerFinal : nil)
        guard let retainedOwnerState, let ownerScope,
              mainState.routeReady, mainState.balanced, retainedOwnerState.balanced,
              main.window.attachedSheet == nil, Self.panels.isEmpty,
              owner?.window.attachedSheet == nil,
              allStatusesMatch(mainState, retainedOwnerState),
              (index != 7 || host.flowIpcWindowContext(Self.ownerId) == nil) else {
            resetQuiet(); schedule(); return
        }
        let actionState = Self.ownerStages.contains(index) ? retainedOwnerState : mainState
        guard actionState.pointerStages[Self.stages[index]]?.receipt == true,
              actionState.pointerStages[Self.stages[index]]?.trusted == true,
              actionState.requestCount > (Self.ownerStages.contains(index) ? previousOwnerRequests : previousMainRequests) else {
            resetQuiet(); schedule(); return
        }
        let key = "\(index):\(mainState.requestCount):\(mainState.responseCount):\(mainState.pushMessageCount)"
            + ":\(retainedOwnerState.requestCount):\(retainedOwnerState.responseCount):\(retainedOwnerState.pushMessageCount)"
        quietCount = key == quietKey ? quietCount + 1 : 1
        quietKey = key
        guard quietCount >= 2 else { schedule(0.2); return }
        let actionScope = Self.ownerStages.contains(index) ? ownerScope : main.scope
        emitStage(actionScope, actionState, mainState, retainedOwnerState)
        observed.append(Self.stages[index])
        previousMainRequests = mainState.requestCount
        previousOwnerRequests = retainedOwnerState.requestCount
        if index == 0 {
            guard verifyPolicy(), let selectedURL, let siblingURL,
                  denied(selectedURL, "target-before", ownerScope),
                  denied(siblingURL, "sibling-before", ownerScope) else { return }
            preselectionDenied = true
        }
        if index == 5 {
            guard let siblingURL, denied(siblingURL, "sibling-after", ownerScope) else { return }
            siblingDeniedAfterWrite = true
        }
        if index == 6 {
            guard let owner, ownerRetained(owner), mainRetained(main),
                  let baseline = ownerBaseline, retainedOwnerState.pushMessageCount > baseline.pushMessageCount else {
                fail("release-without-live-owner-or-push"); return
            }
            ownerFinal = retainedOwnerState
            liveDrainObserved = true
        }
        if index == 7 {
            finish(main, mainState, retainedOwnerState)
            return
        }
        index += 1
        resetQuiet()
        guard let target = Self.ownerStages.contains(index) ? owner : main else { fail("next-owner-missing"); return }
        click(target)
    }

    private func allStatusesMatch(_ main: Snapshot, _ owner: Snapshot) -> Bool {
        for stage in Self.buttons.indices {
            let active = Self.ownerStages.contains(stage) ? owner : main
            let inactive = Self.ownerStages.contains(stage) ? main : owner
            guard inactive.status(Self.buttons[stage]).hasSuffix(": pending") else { return false }
            if stage <= index {
                let expected = Self.expected[stage].merging(Self.callbackFields) { _, value in value }
                guard expected.allSatisfy({ Self.field($0.key, active.status(Self.buttons[stage])) == $0.value }) else { return false }
            } else if !active.status(Self.buttons[stage]).hasSuffix(": pending") { return false }
        }
        return true
    }

    private func mainRetained(_ context: FlowIpcWindowContext) -> Bool {
        ObjectIdentifier(context.window) == mainWindowIdentity && ObjectIdentifier(context.webView) == mainWebViewIdentity
            && context.scope == .main && context.recoveryGeneration == 0 && host.sidecarProcessIdentifier == initialPID
    }

    private func ownerRetained(_ context: FlowIpcWindowContext) -> Bool {
        ObjectIdentifier(context.window) == ownerWindowIdentity && ObjectIdentifier(context.webView) == ownerWebViewIdentity
            && context.scope == ownerScope && context.recoveryGeneration == 0 && host.sidecarProcessIdentifier == initialPID
    }

    private static func opaqueSession(_ session: String) -> Bool {
        let prefix = "flow-ipc-window:"
        return session.hasPrefix(prefix) && UUID(uuidString: String(session.dropFirst(prefix.count))) != nil
    }

    private func valid(_ snapshot: Snapshot, _ context: FlowIpcWindowContext) -> Bool {
        snapshot.windowId == context.scope.windowId && snapshot.sessionId == context.scope.sessionId
            && snapshot.uiId == context.scope.uiId
    }

    private func inspect(_ context: FlowIpcWindowContext, _ done: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspection) { value, error in
            let state: Snapshot?
            if error == nil, let json = value as? String, let data = json.data(using: .utf8) {
                state = try? JSONDecoder().decode(Snapshot.self, from: data)
            } else { state = nil }
            self.diagnose(context.scope.windowId == "main" ? "snapshot-main" : "snapshot-secondary", [
                "javascriptError": error != nil, "stringResult": value is String, "snapshotDecoded": state != nil,
                "initialRootReady": state?.initialRootReady == true,
                "firstMeaningfulPaint": state?.firstMeaningfulPaint == true,
                "routeReady": state?.routeReady == true, "statusCount": state?.statuses.count ?? -1,
                "allPending": state?.allPending == true, "balanced": state?.balanced == true,
                "requestCount": state?.requestCount ?? -1, "responseCount": state?.responseCount ?? -1,
                "pushCount": state?.pushMessageCount ?? -1, "flowClientCount": state?.flowClientCount ?? -1,
                "errorFree": state?.errorFree == true,
                "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow
            ])
            guard let state else { done(nil); return }
            done(state)
        }
    }

    private func observePanel(_ owner: FlowIpcWindowContext, _ state: Snapshot) {
        guard Self.field("outcome", state.status("select")) == nil else { fail("panel-completed-before-observation"); return }
        guard state.pointerStages[Self.stages[index]]?.receipt == true,
              state.pointerStages[Self.stages[index]]?.trusted == true,
              let selectedURL, let panel = owner.window.attachedSheet as? NSSavePanel,
              !(panel is NSOpenPanel), panel.title == Self.panelTitle,
              panel.sheetParent === owner.window, panel.isVisible,
              Self.panels.count == 1, Self.panels.first === panel else { schedule(0.05); return }
        if let previous = observedPanel, previous !== panel { fail("panel-replaced"); return }
        if observedPanel == nil {
            observedPanel = panel
            trace("recv", "\(Self.tracePrefix).panelReady", [
                "stage": "secondary-select", "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        }
        // Passive lexical URL check; only the external operator chooses/saves.
        guard panel.url?.standardizedFileURL == selectedURL else { schedule(0.05); return }
        panelVerified = true
        var payload = owner.scope.tracePayload
        payload.merge(["complete": true, "stage": "secondary-select", "title": Self.panelTitle,
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true, "inputMode": "external-native-panel-selection",
            "physicalInput": false]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).panelObserved", payload)
        phase = .observing
        schedule()
    }

    private static var panels: [NSSavePanel] {
        NSApp.windows.compactMap { $0 as? NSSavePanel }.filter(\.isVisible)
    }

    private func frame(_ main: Snapshot, _ owner: Snapshot) -> [String: Any] {
        ["mainWindowId": main.windowId, "mainSessionId": main.sessionId, "mainUiId": main.uiId,
         "secondaryWindowId": owner.windowId, "secondarySessionId": owner.sessionId, "secondaryUiId": owner.uiId,
         "mainRequestCount": main.requestCount, "mainResponseCount": main.responseCount, "mainPushCount": main.pushMessageCount,
         "secondaryRequestCount": owner.requestCount, "secondaryResponseCount": owner.responseCount,
         "secondaryPushCount": owner.pushMessageCount]
    }

    private func emitStage(_ scope: FlowIpcWindowScope, _ state: Snapshot, _ main: Snapshot, _ owner: Snapshot) {
        var payload = frame(main, owner)
        payload.merge(scope.tracePayload) { _, value in value }
        for (name, value) in Self.expected[index].merging(Self.callbackFields, uniquingKeysWith: { _, value in value }) {
            if value == "true" || value == "false" { payload[name] = value == "true" }
            else if let number = Int(value) { payload[name] = number }
            else { payload[name] = value }
        }
        payload.merge(["complete": true, "stage": Self.stages[index], "hostProcessIdentifier": getpid(),
            "processIdentifier": initialPID, "recoveryGeneration": 0,
            "requestCount": state.requestCount, "responseCount": state.responseCount, "pushCount": state.pushMessageCount,
            "balancedDrainObserved": true, "nativePointerObserved": true, "nativePointerTrustedObserved": true,
            "mainWindowRetained": true, "mainWebViewRetained": true,
            "secondaryWindowRetained": index != 7, "secondaryWebViewRetained": index != 7]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).stageObserved", payload)
    }

    private func finish(_ main: FlowIpcWindowContext, _ state: Snapshot, _ owner: Snapshot) {
        guard let mainBaseline, let ownerBaseline else { fail("missing-baselines"); return }
        let complete = observed == Self.stages && dispatched == Self.stages && mainRetained(main)
            && host.flowIpcWindowContext(Self.ownerId) == nil && liveDrainObserved && quietCount >= 2
            && policyObserved && preselectionDenied && siblingDeniedAfterWrite && panelVerified && routeNavigated
        var payload = frame(state, owner)
        payload.merge(["complete": complete, "hostProcessIdentifier": getpid(),
            "initialProcessIdentifier": initialPID, "currentProcessIdentifier": host.sidecarProcessIdentifier ?? 0,
            "recoveryGeneration": 0, "mainRequestBaseline": mainBaseline.requestCount,
            "mainResponseBaseline": mainBaseline.responseCount, "mainPushBaseline": mainBaseline.pushMessageCount,
            "secondaryRequestBaseline": ownerBaseline.requestCount, "secondaryResponseBaseline": ownerBaseline.responseCount,
            "secondaryPushBaseline": ownerBaseline.pushMessageCount, "stageCount": observed.count,
            "observedStages": observed.joined(separator: ","), "nativePointerEventCount": dispatched.count,
            "nativePointerTrustedEventCount": observed.count, "nativePointerStages": dispatched.joined(separator: ","),
            "panelCount": observedPanel == nil ? 0 : 1, "writtenByteCount": 65_542,
            "mainWindowRetained": mainRetained(main), "mainWebViewRetained": mainRetained(main),
            "secondaryContextRemoved": host.flowIpcWindowContext(Self.ownerId) == nil,
            "liveOwnerRetainedThroughRelease": liveDrainObserved, "liveBalancedDrainObserved": liveDrainObserved,
            "balancedDrainObserved": quietCount >= 2, "resultStableAfterDrain": complete,
            "exactEntitlementsObserved": policyObserved, "preselectionDenied": preselectionDenied,
            "siblingDeniedAfterWrite": siblingDeniedAfterWrite, "sandboxEnforced": complete,
            "rawBridgeApiObserved": complete, "sameTokenUsedObserved": complete,
            "foreignNoWriteProgressObserved": complete, "ownerTokenUnchangedObserved": complete,
            "ownerPositiveControlObserved": complete, "sharedStateCleared": complete,
            "panelURLsVerified": panelVerified, "externalPanelSelection": panelVerified,
            "nativePointerObserved": dispatched == Self.stages, "nativePointerTrustedObserved": observed == Self.stages,
            "errorFree": state.errorFree && owner.errorFree, "standardBundleMode": state.firstMeaningfulPaint,
            "flowClientOwned": state.flowClientCount == 1, "secondaryRenderingObserved": owner.firstMeaningfulPaint,
            "browserProgrammaticSetupObserved": routeNavigated, "browserProgrammaticStages": "route-navigate",
            "physicalInput": false, "gracefulCloseRequested": true]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).frameworkAcceptance", payload)
        terminal = true
        main.window.close()
    }

    private func fail(_ reason: String) {
        guard !terminal else { return }
        terminal = true
        trace("send", "error", ["code": "fileWriteGrantIsolationSandboxFailed", "reason": reason,
            "phase": phase.rawValue, "stage": Self.stages[index]])
        host.flowIpcWindowContext(Self.ownerId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { self.host.flowIpcWindowContext("main")?.window.close() }
    }

    private func resetQuiet() { quietKey = ""; quietCount = 0 }

    /// Test-only wait diagnostics: repeated states emit once; no status text,
    /// scope UUID, path, token or content is included, and no guard is relaxed.
    private func diagnose(_ reason: String, _ values: [String: Any]) {
        var payload: [String: Any] = ["reason": reason, "phase": phase.rawValue,
            "stageIndex": index, "stage": Self.stages[index]]
        payload.merge(values) { _, value in value }
        guard let data = try? JSONSerialization.data(withJSONObject: payload, options: [.sortedKeys]),
              let signature = String(data: data, encoding: .utf8),
              diagnosticSignatures[reason] != signature else { return }
        diagnosticSignatures[reason] = signature
        trace("recv", "\(Self.tracePrefix).driverDiagnostic", payload)
    }

    private static func field(_ name: String, _ status: String) -> String? {
        let prefix = name + "="
        return status.split(separator: " ").first { $0.hasPrefix(prefix) }.map { String($0.dropFirst(prefix.count)) }
    }

    // Same policy and actual POSIX probes as the existing write Sandbox gate;
    // kept local so the established single-owner contracts remain unchanged.
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
        guard let selectedURL, let siblingURL, selectedURL.lastPathComponent == "vatori-file-write-grant-fixture.bin",
              siblingURL.lastPathComponent == "unselected-sibling.txt", selectedURL != siblingURL,
              selectedURL.deletingLastPathComponent() == siblingURL.deletingLastPathComponent(),
              [selectedURL, siblingURL].allSatisfy({ url in
                [FileManager.default.homeDirectoryForCurrentUser, Bundle.main.bundleURL].allSatisfy { parent in
                    let path = parent.standardizedFileURL.path
                    return url.path != path && !url.path.hasPrefix(path + "/")
                }
              }) else { fail("external-fixtures"); return false }
        policyObserved = true
        trace("recv", "\(Self.tracePrefix).policyObserved", [
            "complete": true, "appSandbox": true, "userSelectedReadWrite": true, "networkClient": true,
            "exactEntitlements": true, "entitlementCount": 3, "hostProcessIdentifier": getpid(), "userIdentifier": getuid(),
            "externalFixture": true, "targetOutsideContainer": true, "siblingOutsideContainer": true, "distinctSibling": true
        ])
        return true
    }

    private func denied(_ url: URL, _ stage: String, _ scope: FlowIpcWindowScope) -> Bool {
        errno = 0
        let creating = stage == "target-before"
        let descriptor = creating
            ? Darwin.open(url.path, O_WRONLY | O_CREAT | O_EXCL | O_CLOEXEC | O_NOFOLLOW, mode_t(0o600))
            : Darwin.open(url.path, O_RDONLY | O_CLOEXEC | O_NOFOLLOW)
        let errorNumber = descriptor < 0 ? errno : 0
        if descriptor >= 0 { Darwin.close(descriptor) }
        let rejected = descriptor < 0 && errorNumber == EPERM
        var payload = scope.tracePayload
        payload.merge(["complete": rejected, "stage": stage, "operation": creating ? "create-new" : "open-read",
            "denied": rejected, "errno": errorNumber, "noFollow": true, "hostProcessIdentifier": getpid()]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).accessProbe", payload)
        guard rejected else { fail("access-not-denied"); return false }
        return true
    }

    private struct Geometry: Decodable { let ready: Bool; let centerX: Double; let centerY: Double }
    private struct Pointer: Decodable { let receipt: Bool; let trusted: Bool }
    private struct Snapshot: Decodable {
        let windowId: String; let sessionId: String; let uiId: String
        let initialRootReady: Bool; let firstMeaningfulPaint: Bool; let routeReady: Bool
        let statuses: [String: String]; let requestCount: Int; let responseCount: Int
        let pushMessageCount: Int; let flowClientCount: Int; let pointerStages: [String: Pointer]; let errorFree: Bool
        var balanced: Bool { requestCount == responseCount }
        var allPending: Bool { statuses.count == 8 && statuses.values.allSatisfy { $0.hasSuffix(": pending") } }
        func status(_ operation: String) -> String { statuses[operation] ?? "" }
    }

    private func click(_ context: FlowIpcWindowContext) {
        phase = .click
        let stage = Self.stages[index], button = Self.buttons[index]
        context.webView.evaluateJavaScript(Self.pointerScript(button, stage)) { value, error in
            let point: Geometry?
            if error == nil, let json = value as? String, let data = json.data(using: .utf8) {
                point = try? JSONDecoder().decode(Geometry.self, from: data)
            } else { point = nil }
            self.diagnose("pointer-geometry", [
                "javascriptError": error != nil, "stringResult": value is String,
                "geometryDecoded": point != nil, "pointerReady": point?.ready == true,
                "centerX": point?.centerX ?? 0, "centerY": point?.centerY ?? 0,
                "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow
            ])
            guard let point, point.ready else { self.schedule(); return }
            self.diagnose("activation-enter", [
                "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow
            ])
            FlowIpcNativeInputActivation.prepare(context: context, deadline: self.deadline,
                onFailure: { _ in
                    self.diagnose("activation-failed", [
                        "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow
                    ])
                    self.fail("activation")
                }) {
                self.diagnose("activation-ready", [
                    "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow
                ])
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
                var payload = context.scope.tracePayload
                payload.merge(["stage": stage, "inputMode": "appkit-window-pointer-event", "button": "primary",
                    "webViewX": point.centerX, "webViewY": point.centerY, "windowX": location.x, "windowY": location.y,
                    "activeApplication": NSApp.isActive, "keyWindow": context.window.isKeyWindow]) { _, value in value }
                trace("recv", "\(Self.tracePrefix).nativePointerDispatch", payload)
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    context.window.sendEvent(up)
                    self.dispatched.append(stage)
                    self.phase = self.index == 1 ? .panel : .observing
                    self.schedule()
                }
            }
        }
    }

    private static func pointerScript(_ suffix: String, _ stage: String) -> String {
        """
        (() => {
          const button=document.querySelector('#\(prefix)\(suffix)');
          if (!button || button.disabled) return JSON.stringify({ready:false,centerX:0,centerY:0});
          button.scrollIntoView({block:'center',inline:'center'});
          const r=button.getBoundingClientRect(), centerX=r.left+r.width/2, centerY=r.top+r.height/2;
          const hit=document.elementFromPoint(centerX,centerY);
          if (!(r.width>0 && r.height>0 && (hit===button || button.contains(hit))))
            return JSON.stringify({ready:false,centerX,centerY});
          const marker=window.VatoriWriteIsolationPointer ||= {stages:{},pending:{}};
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

    private static let inspection = """
    (() => {
      const d=window.VaadinDesktopIpcDiagnostics||{}, c=window.VaadinDesktopHostContext||{};
      const prefix='framework-flow-file-write-grant-isolation-', route=document.querySelector('#'+prefix+'route');
      const operations=['open-secondary','select','owner-write-first','foreign-write','foreign-release','owner-write-tail','owner-release','close-secondary'];
      return JSON.stringify({windowId:String(c.windowId||''),sessionId:String(c.sessionId||''),uiId:String(c.uiId||''),
        initialRootReady:!!document.querySelector('#framework-root')&&!route,
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        routeReady:!!route&&location.pathname.endsWith('/flow-file-write-grant-isolation')
          &&operations.every(n=>!!document.querySelector('#'+prefix+n)),
        statuses:Object.fromEntries(operations.map(n=>[n,String(document.querySelector('#'+prefix+n+'-status')?.textContent||'')])),
        requestCount:Number(d.standardBundleOpaqueRequestCount||0),responseCount:Number(d.standardBundleOpaqueResponseCount||0),
        pushMessageCount:Number(d.standardBundlePushMessageCount||0),
        flowClientCount:Object.keys(window.Vaadin?.Flow?.clients||{}).filter(n=>n!=='TypeScript').length,
        pointerStages:window.VatoriWriteIsolationPointer?.stages||{},
        errorFree:!d.standardBundleBrowserError&&!d.standardBundleOpaqueRequestError&&!d.standardBundleOpaqueResponseError&&!d.standardBundlePushError});
    })()
    """
}
