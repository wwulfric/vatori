import Cocoa
import Darwin
import Foundation
import Security

private var fileWriteGrantHostShutdownSandboxAcceptance: FlowIpcFileWriteGrantHostShutdownSandboxAcceptance?

extension WebViewHost {
    func scheduleFileWriteGrantHostShutdownSandboxAcceptanceIfReady() {
        guard config.flowIpcAutoFileWriteGrantHostShutdownSandbox, config.transport == .flowIpc else { return }
        if fileWriteGrantHostShutdownSandboxAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 300
            fileWriteGrantHostShutdownSandboxAcceptance = FlowIpcFileWriteGrantHostShutdownSandboxAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        fileWriteGrantHostShutdownSandboxAcceptance?.resume()
    }

    func observeFileWriteGrantHostShutdownSandboxTermination() {
        fileWriteGrantHostShutdownSandboxAcceptance?.applicationWillTerminate()
    }
}

/// Settled public writes retain their authorization until an external AppKit
/// Quit action. This driver neither releases the grant nor supplies cleanup proof.
private final class FlowIpcFileWriteGrantHostShutdownSandboxAcceptance {
    private enum Phase: String { case root, route, click, panel, observing, hostShutdownWaiting }
    private static let tracePrefix = "flowFileWriteGrantHostShutdownSandbox"
    private static let mainPrefix = "framework-flow-file-write-grant-recovery-"
    private static let ownerPrefix = "framework-flow-file-write-grant-"
    private static let ownerId = "file-write-grant-recovery-secondary"
    private static let panelTitle = "Flow file write grant new file"
    private static let stages = ["main-open-secondary", "owner-select-new", "owner-write-first",
        "owner-invalid-range", "owner-write-tail"]
    private static let operations = ["select-new", "write-first", "invalid-range", "write-tail",
        "release", "write-after-release", "select-existing", "select-canceled"]
    private static let buttons = ["open-secondary", "select-new", "write-first", "invalid-range", "write-tail"]
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
         "offset": "65536", "nextOffset": "65542", "sameGrantFacade": "true"]
    ]

    private let host: WebViewHost
    private let deadline: Date
    private let selectedURL: URL?
    private let siblingURL: URL?
    private var phase = Phase.root
    private var index = 0
    private var scheduled = false
    private var inFlight = false
    private var terminal = false
    private var initialPID: Int32 = 0
    private var mainContextIdentity: ObjectIdentifier?
    private var mainWindowIdentity: ObjectIdentifier?
    private var mainWebViewIdentity: ObjectIdentifier?
    private var ownerContextIdentity: ObjectIdentifier?
    private var ownerWindowIdentity: ObjectIdentifier?
    private var ownerWebViewIdentity: ObjectIdentifier?
    private var ownerScope: FlowIpcWindowScope?
    private var mainBaseline: Snapshot?
    private var ownerBaseline: Snapshot?
    private var lastMainSnapshot: Snapshot?
    private var lastOwnerSnapshot: Snapshot?
    private var previousMainRequests = 0
    private var previousOwnerRequests = 0
    private var dispatched: [String] = []
    private var observed: [String] = []
    private var navigated = false
    private var observedPanel: NSSavePanel?
    private var panelVerified = false
    private var preselectionDenied = false
    private var siblingDeniedAfterWrite = false
    private var policyObserved = false
    private var hostShutdownPrepared = false
    private var acceptanceQuitMenuRoot: NSMenuItem?
    private var acceptanceQuitMenuItem: NSMenuItem?
    private var quietKey = ""
    private var quietCount = 0
    private var diagnosticSignatures: [String: String] = [:]
    private var ownerAction: Bool { index != 0 }

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        let env = ProcessInfo.processInfo.environment
        selectedURL = Self.externalURL(env["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SELECTED_PATH"])
        siblingURL = Self.externalURL(env["VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SIBLING_PATH"])
    }

    private static func externalURL(_ path: String?) -> URL? {
        guard let path, path.hasPrefix("/"), !path.contains("\0") else { return nil }
        return URL(fileURLWithPath: path).standardizedFileURL
    }

    func resume() { if !inFlight { schedule() } }

    private var awaitingRequestedHostTermination: Bool {
        // Keep auditing until external Quit, then leave teardown observation
        // to the lifecycle receipts instead of probing a stopping Flow runtime.
        hostShutdownPrepared && phase == .hostShutdownWaiting && host.isTerminationRequested
    }

    private func schedule(_ delay: Double = 0.1) {
        inFlight = false
        guard !terminal, !awaitingRequestedHostTermination, !scheduled else { return }
        scheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.scheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal, !awaitingRequestedHostTermination, !inFlight else { return }
        inFlight = true
        guard Date() < deadline else { fail("deadline"); return }
        diagnose("poll", ["mainContextPresent": host.flowIpcWindowContext("main") != nil,
            "secondaryContextPresent": host.flowIpcWindowContext(Self.ownerId) != nil,
            "mainNavigationFinished": host.flowIpcWindowContext("main")?.navigationFinished == true,
            "secondaryNavigationFinished": host.flowIpcWindowContext(Self.ownerId)?.navigationFinished == true])
        guard let main = host.flowIpcWindowContext("main"), main.navigationFinished else {
            if hostShutdownPrepared { fail("prepared-main-context-missing") } else { schedule() }
            return
        }
        if phase != .root && !retained(main, main: true) { fail("main-identity"); return }
        inspect(main) { state in
            guard !self.terminal else { return }
            guard let state else { self.schedule(); return }
            guard self.valid(state, main), state.errorFree else { self.fail("main-scope-or-runtime"); return }
            if self.phase == .root { self.navigate(main, state); return }
            if self.phase == .route {
                guard state.routeReady, state.allPending, state.balanced else { self.schedule(); return }
                self.mainBaseline = state
                self.previousMainRequests = state.requestCount
                self.click(main)
                return
            }
            if let owner = self.host.flowIpcWindowContext(Self.ownerId), owner.navigationFinished {
                if self.ownerScope != nil && !self.retained(owner, main: false) { self.fail("owner-identity"); return }
                self.inspect(owner) { ownerState in
                    guard !self.terminal else { return }
                    guard let ownerState else { self.schedule(); return }
                    guard self.valid(ownerState, owner), ownerState.errorFree else { self.fail("owner-scope-or-runtime"); return }
                    if self.index == 0 && !ownerState.routeReady { self.schedule(); return }
                    guard ownerState.routeReady else { self.fail("owner-route-missing"); return }
                    self.advance(main, state, owner, ownerState)
                }
            } else if self.index == 0 { self.advance(main, state, nil, nil) }
            else { self.fail("owner-context-missing") }
        }
    }

    private func navigate(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        guard state.initialRootReady, state.firstMeaningfulPaint, state.flowClientCount == 1,
              state.balanced, let pid = host.sidecarProcessIdentifier, pid > 0 else { schedule(); return }
        initialPID = pid
        mainContextIdentity = ObjectIdentifier(main)
        mainWindowIdentity = ObjectIdentifier(main.window)
        mainWebViewIdentity = ObjectIdentifier(main.webView)
        main.webView.evaluateJavaScript("""
            (() => {
              const b=document.querySelector('#framework-flow-file-write-grant-recovery-open-route');
              if (!b) return false;
              b.click(); return true;
            })()
            """) { value, error in
            guard !self.terminal else { return }
            guard error == nil, value as? Bool == true else { self.schedule(); return }
            self.navigated = true
            self.phase = .route
            self.schedule(0.3)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ mainState: Snapshot,
                         _ owner: FlowIpcWindowContext?, _ ownerState: Snapshot?) {
        if phase == .hostShutdownWaiting {
            guard let owner, let ownerState,
                  stablePreparation(main, mainState, owner, ownerState), acceptanceQuitMenuInstalled else {
                fail("prepared-state-changed"); return
            }
            lastMainSnapshot = mainState
            lastOwnerSnapshot = ownerState
            // Ordinary heartbeat traffic may advance counters or be in flight.
            // The inspector checks the actual final opaque stream at shutdown.
            schedule(0.5)
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
                ownerContextIdentity = ObjectIdentifier(owner)
                ownerWindowIdentity = ObjectIdentifier(owner.window)
                ownerWebViewIdentity = ObjectIdentifier(owner.webView)
                ownerBaseline = ownerState
                previousOwnerRequests = ownerState.requestCount
            }
        }
        guard let owner, let ownerState, let ownerScope, mainState.routeReady,
              main.window.attachedSheet == nil, owner.window.attachedSheet == nil, Self.panels.isEmpty,
              statusesMatch(mainState, ownerState) else { resetQuiet(); schedule(); return }
        let action = ownerAction ? ownerState : mainState
        guard action.pointerStages[Self.stages[index]]?.receipt == true,
              action.pointerStages[Self.stages[index]]?.trusted == true,
              action.requestCount > (ownerAction ? previousOwnerRequests : previousMainRequests),
              settled(mainState, ownerState) else { schedule(); return }
        emitStage(ownerAction ? ownerScope : main.scope, action, mainState, ownerState)
        observed.append(Self.stages[index])
        previousMainRequests = mainState.requestCount
        previousOwnerRequests = ownerState.requestCount
        if index == 0 {
            guard verifyPolicy(), probesBeforeSelection() else { return }
        }
        if index == 4 {
            guard let siblingURL, denied(siblingURL, stage: "sibling-after") else { return }
            siblingDeniedAfterWrite = true
            prepareShutdown(main, mainState, owner, ownerState)
            return
        }
        index += 1
        resetQuiet()
        click(owner)
    }

    private func settled(_ main: Snapshot, _ owner: Snapshot) -> Bool {
        guard main.balanced, owner.balanced else { resetQuiet(); return false }
        let key = "\(index):\(main.requestCount):\(main.responseCount):\(main.pushMessageCount)"
            + ":\(owner.requestCount):\(owner.responseCount):\(owner.pushMessageCount)"
        quietCount = key == quietKey ? quietCount + 1 : 1
        quietKey = key
        return quietCount >= 2
    }

    private func statusesMatch(_ main: Snapshot, _ owner: Snapshot) -> Bool {
        guard Self.matches(main.status("open-secondary"), Self.mainExpected),
              main.status("close-secondary") == "close-secondary: pending" else { return false }
        for operation in Self.operations.indices {
            let name = Self.operations[operation]
            if operation < index {
                guard Self.matches(owner.status(name), Self.expectedOwner(operation)) else { return false }
            } else if owner.status(name) != "\(name): pending" { return false }
        }
        return true
    }

    private static let mainExpected = ["opened": "true", "client": "true", "change": "1", "listenerCount": "1"]

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

    private func retained(_ context: FlowIpcWindowContext, main: Bool, requireRunningSidecar: Bool = true) -> Bool {
        let expectedScope = main ? FlowIpcWindowScope.main : ownerScope
        return ObjectIdentifier(context) == (main ? mainContextIdentity : ownerContextIdentity)
            && ObjectIdentifier(context.window) == (main ? mainWindowIdentity : ownerWindowIdentity)
            && ObjectIdentifier(context.webView) == (main ? mainWebViewIdentity : ownerWebViewIdentity)
            && context.scope == expectedScope && context.recoveryGeneration == 0
            && (!requireRunningSidecar || host.sidecarProcessIdentifier == initialPID) && initialPID > 0
    }

    private static func opaqueSession(_ session: String) -> Bool {
        let prefix = "flow-ipc-window:"
        return session.hasPrefix(prefix) && UUID(uuidString: String(session.dropFirst(prefix.count))) != nil
    }

    private func valid(_ state: Snapshot, _ context: FlowIpcWindowContext) -> Bool {
        state.windowId == context.scope.windowId && state.sessionId == context.scope.sessionId && state.uiId == context.scope.uiId
    }

    private func stablePreparation(_ main: FlowIpcWindowContext, _ mainState: Snapshot,
                                   _ owner: FlowIpcWindowContext, _ ownerState: Snapshot) -> Bool {
        retained(main, main: true) && retained(owner, main: false)
            && ownerWindowOpen(main.window) && ownerWindowOpen(owner.window)
            && valid(mainState, main) && valid(ownerState, owner)
            && mainState.routeReady && ownerState.routeReady && statusesMatch(mainState, ownerState)
            && mainState.firstMeaningfulPaint && ownerState.firstMeaningfulPaint
            && mainState.flowClientCount == 1 && ownerState.flowClientCount == 1
            && mainState.errorFree && ownerState.errorFree
            && main.window.attachedSheet == nil && owner.window.attachedSheet == nil && Self.panels.isEmpty
            && dispatched == Self.stages && observed == Self.stages
            && mainState.pointerStages[Self.stages[0]]?.receipt == true
            && mainState.pointerStages[Self.stages[0]]?.trusted == true
            && Self.stages.dropFirst().allSatisfy {
                ownerState.pointerStages[$0]?.receipt == true && ownerState.pointerStages[$0]?.trusted == true
            }
            && navigated && panelVerified && policyObserved && preselectionDenied && siblingDeniedAfterWrite
    }

    private func prepareShutdown(_ main: FlowIpcWindowContext, _ mainState: Snapshot,
                                 _ owner: FlowIpcWindowContext, _ ownerState: Snapshot) {
        guard stablePreparation(main, mainState, owner, ownerState), mainState.balanced, ownerState.balanced,
              quietCount >= 2, let ownerBaseline, ownerState.pushMessageCount > ownerBaseline.pushMessageCount,
              installAcceptanceQuitMenu() else { fail("shutdown-preparation"); return }
        lastMainSnapshot = mainState
        lastOwnerSnapshot = ownerState
        hostShutdownPrepared = true
        phase = .hostShutdownWaiting
        var payload = frame(mainState, ownerState)
        payload.merge(baselines()) { _, value in value }
        payload.merge(["complete": true, "settledGrantObserved": true, "releasePending": true,
            "writeAfterReleasePending": true, "closeSecondaryPending": true, "unusedSelectionsPending": true,
            "bothWindowsRetained": true, "bothWebViewsRetained": true, "ownerWindowsOpen": true,
            "balancedDrainObserved": true, "resultStableAfterDrain": true, "writtenByteCount": 65_542,
            "stageCount": observed.count, "observedStages": observed.joined(separator: ","),
            "nativePointerEventCount": dispatched.count, "nativePointerTrustedEventCount": observed.count,
            "nativePointerStages": dispatched.joined(separator: ","), "nativePointerObserved": true,
            "nativePointerTrustedObserved": true, "panelCount": 1, "accessProbeCount": 3,
            "panelURLsVerified": true, "externalPanelSelection": true,
            "browserProgrammaticSetupObserved": true, "browserProgrammaticStages": "route-navigate",
            "acceptanceQuitMenuInstalled": acceptanceQuitMenuInstalled, "exactEntitlementsObserved": true,
            "preselectionDenied": true, "siblingDeniedAfterWrite": true, "typedGrantApiObserved": true,
            "sandboxEnforced": true, "standardBundleMode": true, "flowClientOwned": true,
            "secondaryRenderingObserved": true, "errorFree": true, "physicalInput": false]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).hostShutdownPrepared", payload)
        // Installing a standard AppKit menu is not invoking it. Selection and
        // Quit remain external native UI actions; both owners stay alive here.
        schedule(0.5)
    }

    private var acceptanceQuitMenuInstalled: Bool {
        guard let root = acceptanceQuitMenuRoot, let item = acceptanceQuitMenuItem,
              let menu = NSApp.mainMenu, let submenu = root.submenu else { return false }
        return menu.items.contains { $0 === root } && submenu.items.contains { $0 === item }
            && item.target === NSApp && item.action == #selector(NSApplication.terminate(_:))
            && item.keyEquivalent == "q" && item.keyEquivalentModifierMask == .command
    }

    private func installAcceptanceQuitMenu() -> Bool {
        if acceptanceQuitMenuInstalled { return true }
        let menu = NSApp.mainMenu ?? NSMenu(title: "")
        let root = NSMenuItem(title: "Acceptance", action: nil, keyEquivalent: "")
        let submenu = NSMenu(title: "Acceptance")
        let quit = NSMenuItem(title: "Quit Vaadin Desktop Host",
            action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        quit.target = NSApp
        quit.keyEquivalentModifierMask = .command
        submenu.addItem(quit)
        root.submenu = submenu
        menu.addItem(root)
        NSApp.mainMenu = menu
        acceptanceQuitMenuRoot = root
        acceptanceQuitMenuItem = quit
        return acceptanceQuitMenuInstalled
    }

    private func ownerWindowOpen(_ window: NSWindow) -> Bool {
        NSApp.windows.contains { $0 === window } && (window.isVisible || window.isMiniaturized)
    }

    func applicationWillTerminate() {
        let main = host.flowIpcWindowContext("main")
        let owner = host.flowIpcWindowContext(Self.ownerId)
        let mainRetained = main.map { retained($0, main: true, requireRunningSidecar: false) } == true
        let ownerRetained = owner.map { retained($0, main: false, requireRunningSidecar: false) } == true
        let windowsOpen = main.map { ownerWindowOpen($0.window) } == true
            && owner.map { ownerWindowOpen($0.window) } == true
        // This synchronous observer cannot inspect the WebView again. Pending
        // state comes from the latest audit; actual transport/lifecycle receipts
        // independently prove that no release/close command intervened.
        let pending = lastMainSnapshot.flatMap { mainState in
            lastOwnerSnapshot.map { statusesMatch(mainState, $0) }
        } == true
        let expected = hostShutdownPrepared && phase == .hostShutdownWaiting && !terminal
            && acceptanceQuitMenuInstalled && pending
        errno = 0
        let sidecarAbsent = initialPID > 0 && host.sidecarProcessIdentifier == nil
            && Darwin.kill(initialPID, 0) == -1 && errno == ESRCH
        terminal = true
        var payload = epoch()
        let termination: [String: Any] = ["complete": expected && mainRetained && ownerRetained && windowsOpen && sidecarAbsent,
            "preparedObserved": hostShutdownPrepared, "terminationSource": "applicationWillTerminate",
            "externalTerminationExpected": expected, "acceptanceQuitMenuInstalled": acceptanceQuitMenuInstalled,
            "initialProcessIdentifier": initialPID, "currentProcessIdentifier": host.sidecarProcessIdentifier ?? 0,
            "originalSidecarAbsent": sidecarAbsent,
            "mainWindowId": main?.scope.windowId ?? "", "mainSessionId": main?.scope.sessionId ?? "",
            "mainUiId": main?.scope.uiId ?? "", "secondaryWindowId": owner?.scope.windowId ?? "",
            "secondarySessionId": owner?.scope.sessionId ?? "", "secondaryUiId": owner?.scope.uiId ?? "",
            "mainWindowRetained": mainRetained, "mainWebViewRetained": mainRetained,
            "secondaryWindowRetained": ownerRetained, "secondaryWebViewRetained": ownerRetained,
            "ownerWindowsOpen": windowsOpen, "releasePending": pending, "writeAfterReleasePending": pending,
            "closeSecondaryPending": pending, "unusedSelectionsPending": pending, "physicalInput": false]
        payload.merge(termination) { _, value in value }
        trace("recv", "\(Self.tracePrefix).hostShutdownObserved", payload)
        // Production resource-drain and shutdown events provide cleanup proof.
        // The external runner checks bytes and removes fixtures after host exit.
    }

    private func inspect(_ context: FlowIpcWindowContext, _ done: @escaping (Snapshot?) -> Void) {
        guard !terminal, !awaitingRequestedHostTermination else { return }
        context.webView.evaluateJavaScript(Self.inspection(main: context.scope.windowId == "main")) { value, error in
            guard !self.terminal, !self.awaitingRequestedHostTermination else { return }
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
              let selectedURL, let panel = owner.window.attachedSheet as? NSSavePanel,
              !(panel is NSOpenPanel), panel.title == Self.panelTitle, panel.sheetParent === owner.window,
              panel.isVisible, Self.panels.count == 1, Self.panels.first === panel else { schedule(0.05); return }
        if let observedPanel, observedPanel !== panel { fail("panel-replaced"); return }
        if observedPanel == nil {
            observedPanel = panel
            var ready = scoped(owner.scope)
            ready.merge(["stage": Self.stages[index], "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"]) { _, value in value }
            trace("recv", "\(Self.tracePrefix).panelReady", ready)
        }
        guard panel.url?.standardizedFileURL == selectedURL else { schedule(0.05); return }
        panelVerified = true
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
        ["sidecarGeneration": 1, "recoveryGeneration": 0,
         "hostProcessIdentifier": getpid(), "processIdentifier": initialPID]
    }

    private func scoped(_ scope: FlowIpcWindowScope) -> [String: Any] {
        epoch().merging(scope.tracePayload) { _, value in value }
    }

    private func frame(_ main: Snapshot, _ owner: Snapshot) -> [String: Any] {
        var value = epoch()
        value.merge(["mainWindowId": main.windowId, "mainSessionId": main.sessionId, "mainUiId": main.uiId,
            "secondaryWindowId": owner.windowId, "secondarySessionId": owner.sessionId, "secondaryUiId": owner.uiId,
            "mainRequestCount": main.requestCount, "mainResponseCount": main.responseCount,
            "mainPushCount": main.pushMessageCount, "secondaryRequestCount": owner.requestCount,
            "secondaryResponseCount": owner.responseCount, "secondaryPushCount": owner.pushMessageCount]) { _, value in value }
        return value
    }

    private func baselines() -> [String: Any] {
        guard let main = mainBaseline, let owner = ownerBaseline else { return [:] }
        return ["mainRequestBaseline": main.requestCount, "mainResponseBaseline": main.responseCount,
            "mainPushBaseline": main.pushMessageCount, "secondaryRequestBaseline": owner.requestCount,
            "secondaryResponseBaseline": owner.responseCount, "secondaryPushBaseline": owner.pushMessageCount]
    }

    private func emitStage(_ scope: FlowIpcWindowScope, _ state: Snapshot, _ main: Snapshot, _ owner: Snapshot) {
        var payload = frame(main, owner)
        payload.merge(scope.tracePayload) { _, value in value }
        let fields = ownerAction ? Self.expectedOwner(index - 1) : Self.mainExpected
        for (name, value) in fields {
            if value == "true" || value == "false" { payload[name] = value == "true" }
            else if let number = Int(value) { payload[name] = number }
            else { payload[name] = value }
        }
        payload.merge(["complete": true, "stage": Self.stages[index], "requestCount": state.requestCount,
            "responseCount": state.responseCount, "pushCount": state.pushMessageCount,
            "balancedDrainObserved": true, "nativePointerObserved": true, "nativePointerTrustedObserved": true,
            "mainWindowRetained": true, "mainWebViewRetained": true,
            "secondaryWindowRetained": true, "secondaryWebViewRetained": true]) { _, value in value }
        trace("recv", "\(Self.tracePrefix).stageObserved", payload)
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
        guard let target = selectedURL, let sibling = siblingURL,
              target.lastPathComponent == "vatori-file-write-grant-fixture.bin", sibling.lastPathComponent == "unselected-sibling.txt",
              target != sibling, target.deletingLastPathComponent() == sibling.deletingLastPathComponent(),
              [target, sibling].allSatisfy({ url in
                [FileManager.default.homeDirectoryForCurrentUser, Bundle.main.bundleURL].allSatisfy { parent in
                    let path = parent.standardizedFileURL.path
                    return url.path != path && !url.path.hasPrefix(path + "/")
                }
              }) else { fail("external-fixtures"); return false }
        policyObserved = true
        trace("recv", "\(Self.tracePrefix).policyObserved", ["complete": true, "appSandbox": true,
            "userSelectedReadWrite": true, "networkClient": true, "exactEntitlements": true, "entitlementCount": 3,
            "hostProcessIdentifier": getpid(), "userIdentifier": getuid(), "externalFixture": true,
            "targetOutsideContainer": true, "siblingOutsideContainer": true, "distinctSibling": true])
        return true
    }

    private func probesBeforeSelection() -> Bool {
        guard let selectedURL, let siblingURL else { fail("missing-external-fixtures"); return false }
        guard denied(selectedURL, stage: "target-before"), denied(siblingURL, stage: "sibling-before") else { return false }
        preselectionDenied = true
        return true
    }

    private func denied(_ url: URL, stage: String) -> Bool {
        guard let ownerScope else { fail("probe-owner-missing"); return false }
        errno = 0
        let creating = stage == "target-before"
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
        trace("send", "error", ["code": "fileWriteGrantHostShutdownSandboxFailed", "reason": reason,
            "phase": phase.rawValue, "stage": Self.stages[index]])
        host.flowIpcWindowContext(Self.ownerId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { self.host.flowIpcWindowContext("main")?.window.close() }
    }

    private func resetQuiet() { quietKey = ""; quietCount = 0 }

    private func diagnose(_ reason: String, _ values: [String: Any]) {
        var payload: [String: Any] = ["reason": reason, "phase": phase.rawValue, "stageIndex": index,
            "stage": Self.stages[index], "sidecarGeneration": 1]
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
            guard !self.terminal else { return }
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
                guard !self.terminal else { return }
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
                    guard !self.terminal else { return }
                    context.window.sendEvent(up)
                    self.dispatched.append(stage)
                    self.phase = self.index == 1 ? .panel : .observing
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
          const marker=window.VatoriWriteShutdownPointer ||= {stages:{},pending:{}};
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
            pointerStages:window.VatoriWriteShutdownPointer?.stages||{},
            errorFree:!d.standardBundleBrowserError&&!d.standardBundleOpaqueRequestError&&!d.standardBundleOpaqueResponseError&&!d.standardBundlePushError});
        })()
        """
    }
}
