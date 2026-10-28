import Cocoa
import Darwin
import Foundation
import Security

private var flowIpcFileReadGrantSandboxAcceptance: FlowIpcFileReadGrantSandboxAcceptance?

extension WebViewHost {
    func scheduleFileReadGrantSandboxAcceptanceIfReady() {
        guard config.flowIpcAutoFileReadGrantSandbox,
              config.transport == .flowIpc else { return }
        if flowIpcFileReadGrantSandboxAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds : 300
            flowIpcFileReadGrantSandboxAcceptance = FlowIpcFileReadGrantSandboxAcceptance(
                host: self, deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcFileReadGrantSandboxAcceptance?.resume()
    }
}

/// The unsandboxed runner owns these external fixtures. This driver must never
/// create, copy, pre-read, remove, or obtain an extension for either path.
private final class FlowIpcFileReadGrantSandboxAcceptance {
    private enum Phase: String { case root, route, click, panel, operation, drain }
    private static let prefix = "framework-flow-file-read-grant-"
    private static let panelTitle = "Flow file read grant explicit release"
    private static let filename = "vatori-file-read-grant-fixture.txt"
    private static let stages = ["select-first", "read-first", "release-first", "read-after-release"]
    private static let outcomes = ["SELECTED", "DATA", "RELEASED", "UNAVAILABLE"]
    private static let entitlementNames: Set<String> = [
        "com.apple.security.app-sandbox",
        "com.apple.security.files.user-selected.read-only",
        "com.apple.security.network.client"
    ]

    private let host: WebViewHost
    private let deadline: Date
    private let selectedURL: URL?
    private let siblingURL: URL?
    private var phase = Phase.root
    private var terminal = false
    private var pollScheduled = false
    private var pollInFlight = false
    private var policyObserved = false
    private var preselectionDenied = false
    private var siblingDeniedAfterRead = false
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
    private var observedPanel: NSOpenPanel?
    private var panelURLVerified = false
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
        guard Date() < deadline else { fail("fileReadGrantSandboxDeadline"); return }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else { schedule(); return }
        if phase != .root && !retained(main) {
            fail("fileReadGrantSandboxIdentityChanged"); return
        }
        main.webView.evaluateJavaScript(Self.inspectionScript) { value, error in
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let state = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                self.schedule(); return
            }
            guard state.errorFree, state.windowId == "main",
                  state.sessionId == "flow-ipc-session", state.uiId == "main" else {
                self.fail("fileReadGrantSandboxScopeOrRuntimeError"); return
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
            if !policyObserved {
                guard verifyPolicy(), let selectedURL, let siblingURL else { return }
                guard denied(selectedURL, stage: "target-before", main: main),
                      denied(siblingURL, stage: "sibling-before", main: main) else { return }
                preselectionDenied = true
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
            dispatchCurrent(main)
        case .click:
            guard state.routeReady else { schedule(); return }
            dispatchCurrent(main)
        case .panel:
            observePanel(main, state)
        case .operation:
            let stage = Self.stages[currentStageIndex]
            guard state.routeReady, record(stage, state), statusMatches(state, stage),
                  observedStages.allSatisfy({ statusMatches(state, $0) }),
                  Self.stages.dropFirst(currentStageIndex + 1).allSatisfy({
                    state.status($0).hasSuffix(": pending")
                  }), main.window.attachedSheet == nil, Self.panels.isEmpty,
                  state.balanced, state.requestCount > previousStageRequestCount else {
                resetDrain(); schedule(); return
            }
            let key = "\(stage):\(state.requestCount):\(state.responseCount):\(state.pushMessageCount)"
            guard quietPoll(key) else { schedule(0.2); return }
            emitStage(main, state, stage)
            observedStages.append(stage)
            if stage == "read-first" {
                guard let siblingURL,
                      denied(siblingURL, stage: "sibling-after", main: main) else { return }
                siblingDeniedAfterRead = true
            }
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
            fail("fileReadGrantSandboxEntitlementsMismatch"); return false
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
            fail("fileReadGrantSandboxExternalFixtureInvalid"); return false
        }
        policyObserved = true
        trace("recv", "flowFileReadGrantSandbox.policyObserved", [
            "complete": true, "appSandbox": true, "userSelectedReadOnly": true,
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
        let descriptor = Darwin.open(url.path, O_RDONLY | O_CLOEXEC | O_NOFOLLOW)
        let errorNumber = descriptor < 0 ? errno : 0
        if descriptor >= 0 { Darwin.close(descriptor) }
        let rejected = descriptor < 0 && errorNumber == EPERM
        trace("recv", "flowFileReadGrantSandbox.accessProbe", [
            "complete": rejected, "stage": stage, "operation": "open-read",
            "denied": rejected, "errno": errorNumber, "noFollow": true,
            "hostProcessIdentifier": getpid(), "windowId": main.scope.windowId,
            "sessionId": main.scope.sessionId, "uiId": main.scope.uiId
        ])
        guard rejected else { fail("fileReadGrantSandboxAccessNotDenied"); return false }
        return true
    }

    private func dispatchCurrent(_ main: FlowIpcWindowContext) {
        phase = .click
        let stage = Self.stages[currentStageIndex]
        click(main, stage, currentStageIndex == 0 ? .panel : .operation)
    }

    private func retained(_ main: FlowIpcWindowContext) -> Bool {
        windowIdentity == ObjectIdentifier(main.window)
            && webViewIdentity == ObjectIdentifier(main.webView)
            && main.recoveryGeneration == 0 && host.sidecarProcessIdentifier == initialPID
            && main.scope.windowId == "main" && main.scope.sessionId == "flow-ipc-session"
            && main.scope.uiId == "main"
    }

    private func observePanel(_ main: FlowIpcWindowContext, _ state: Snapshot) {
        guard Self.field("outcome", state.status("select-first")) == nil else {
            fail("fileReadGrantSandboxPanelCompletedBeforeObservation"); return
        }
        guard record("select-first", state), let selectedURL,
              let panel = main.window.attachedSheet as? NSOpenPanel,
              panel.title == Self.panelTitle, panel.sheetParent === main.window,
              panel.isVisible, Self.panels.count == 1,
              Self.panels.first === panel else { schedule(0.05); return }
        if let previous = observedPanel, previous !== panel {
            fail("fileReadGrantSandboxUnexpectedPanelReplacement"); return
        }
        if observedPanel == nil {
            observedPanel = panel
            trace("recv", "flowFileReadGrantSandbox.panelReady", [
                "stage": "file", "sheetAttached": true, "panelVisible": true,
                "inputMode": "external-native-panel-selection"
            ])
        }
        // Passive URL comparison only. The external operator must select the
        // exact file and then confirm the genuine system Open control.
        guard panel.url?.standardizedFileURL == selectedURL else { schedule(0.05); return }
        panelURLVerified = true
        trace("recv", "flowFileReadGrantSandbox.panelObserved", [
            "complete": true, "stage": "file", "title": Self.panelTitle,
            "sheetAttached": true, "panelVisible": true, "panelURLVerified": true,
            "externalPanelSelection": true, "inputMode": "external-native-panel-selection",
            "physicalInput": false, "windowId": main.scope.windowId,
            "sessionId": main.scope.sessionId, "uiId": main.scope.uiId
        ])
        phase = .operation
        schedule()
    }

    private func matches(_ state: Snapshot, _ stage: String, _ expected: [String: String]) -> Bool {
        expected.allSatisfy { Self.field($0.key, state.status(stage)) == $0.value }
    }

    private func statusMatches(_ state: Snapshot, _ stage: String) -> Bool {
        guard let index = Self.stages.firstIndex(of: stage), matches(state, stage, [
            "outcome": Self.outcomes[index], "client": "true", "change": "1",
            "grantPresent": "true", "filename": Self.filename,
            "byteCount": "48", "maxReadBytes": "65536", "securityScoped": "true"
        ]) else { return false }
        switch stage {
        case "select-first":
            return matches(state, stage, [
                "capabilityContextCaptured": "true", "backgroundSessionAbsent": "true"
            ])
        case "read-first":
            return matches(state, stage, [
                "requestedOffset": "0", "requestedBytes": "19", "bytesRead": "19",
                "offset": "0", "nextOffset": "19", "eof": "false", "contentMatches": "true"
            ])
        case "release-first": return true
        case "read-after-release":
            return matches(state, stage, [
                "requestedOffset": "0", "requestedBytes": "19", "bytesRead": "0",
                "offset": "-1", "nextOffset": "-1", "eof": "false"
            ])
        default: return false
        }
    }

    private func emitStage(_ main: FlowIpcWindowContext, _ state: Snapshot, _ stage: String) {
        var payload: [String: Any] = [
            "complete": true, "stage": stage,
            "outcome": Self.field("outcome", state.status(stage)) ?? "",
            "client": true, "change": 1, "grantPresent": true,
            "byteCount": 48, "maxReadBytes": 65_536, "securityScoped": true,
            "hostProcessIdentifier": getpid(), "processIdentifier": host.sidecarProcessIdentifier ?? 0,
            "windowId": main.scope.windowId, "sessionId": main.scope.sessionId, "uiId": main.scope.uiId,
            "requestCount": state.requestCount, "responseCount": state.responseCount,
            "pushCount": state.pushMessageCount, "balancedDrainObserved": drainPolls >= 2,
            "nativePointerObserved": true, "nativePointerTrustedObserved": true
        ]
        if stage == "select-first" {
            payload["capabilityContextCaptured"] = true
            payload["backgroundSessionAbsent"] = true
        } else if stage == "read-first" || stage == "read-after-release" {
            let readable = stage == "read-first"
            payload.merge([
                "requestedOffset": 0, "requestedBytes": 19, "bytesRead": readable ? 19 : 0,
                "offset": readable ? 0 : -1, "nextOffset": readable ? 19 : -1, "endOfFile": false
            ]) { _, value in value }
            if readable { payload["contentMatches"] = true }
        }
        trace("recv", "flowFileReadGrantSandbox.stageObserved", payload)
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
            && drainPolls >= 2 && panelURLVerified && routeNavigationObserved
            && state.errorFree && state.balanced && state.pushMessageCount > mainPushBaseline
            && policyObserved && preselectionDenied && siblingDeniedAfterRead
        trace("recv", "flowFileReadGrantSandbox.frameworkAcceptance", [
            "complete": complete, "standardBundleMode": state.firstMeaningfulPaint,
            "rootRendered": state.rootRendered, "flowClientOwned": state.flowClientCount > 0,
            "externalFixture": true, "fixtureByteCount": 48,
            "exactEntitlementsObserved": policyObserved, "preselectionDenied": preselectionDenied,
            "siblingDeniedAfterRead": siblingDeniedAfterRead, "sandboxEnforced": complete,
            "securityScopedObserved": statuses,
            "allStageStatusesObserved": statuses, "observedStages": observedStages.joined(separator: ","),
            "stageCount": observedStages.count, "typedGrantApiObserved": statuses, "contentMatches": statuses,
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
            "realOpenPanelObserved": observedPanel != nil, "panelCount": observedPanel == nil ? 0 : 1,
            "panelURLsVerified": panelURLVerified, "externalPanelSelection": panelURLVerified,
            "nativePointerObserved": pointers, "nativePointerTrustedObserved": trusted,
            "nativePointerEventCount": dispatchedStages.count,
            "nativePointerTrustedEventCount": trustedStages.count,
            "nativePointerStages": dispatchedStages.joined(separator: ","),
            "browserProgrammaticSetupObserved": routeNavigationObserved,
            "browserProgrammaticStages": "route-navigate", "physicalInput": false,
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
            statuses.count == 4 && statuses.values.allSatisfy { $0.hasSuffix(": pending") }
        }
    }

    private func click(_ context: FlowIpcWindowContext, _ stage: String, _ next: Phase) {
        context.webView.evaluateJavaScript(Self.pointerScript(stage)) { value, error in
            guard error == nil, let json = value as? String,
                  let data = json.data(using: .utf8),
                  let point = try? JSONDecoder().decode(PointerGeometry.self, from: data),
                  point.ready else { self.schedule(); return }
            FlowIpcNativeInputActivation.prepare(context: context, deadline: self.deadline,
                onFailure: { _ in self.fail("fileReadGrantSandboxActivationFailed") }) {
                let location = context.webView.convert(NSPoint(x: point.centerX,
                    y: context.webView.isFlipped ? point.centerY
                        : context.webView.bounds.height - point.centerY), to: nil)
                guard context.window.contentLayoutRect.contains(location) else {
                    self.fail("fileReadGrantSandboxPointerOutsideWindow"); return
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
                    self.fail("fileReadGrantSandboxPointerCreationFailed"); return
                }
                trace("recv", "flowFileReadGrantSandbox.nativePointerDispatch", [
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

    private static func pointerScript(_ stage: String) -> String {
        """
        (() => {
          const button = document.querySelector('#\(prefix)\(stage)');
          if (!button) return JSON.stringify({ready:false});
          button.scrollIntoView({block:'center',inline:'center'});
          const rect = button.getBoundingClientRect();
          const centerX = rect.left + rect.width / 2, centerY = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(centerX,centerY);
          if (!(rect.width > 0 && rect.height > 0) || !(hit === button || button.contains(hit))) {
            return JSON.stringify({ready:false,centerX,centerY});
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
      const button = document.querySelector('#framework-flow-file-read-grant-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const d = window.VaadinDesktopIpcDiagnostics || {};
      const c = window.VaadinDesktopHostContext || {};
      const prefix = 'framework-flow-file-read-grant-';
      const route = document.querySelector('#'+prefix+'route');
      const operations = ['select-first','read-first','release-first','read-after-release'];
      const statuses = Object.fromEntries(operations.map(name => [name,
        String(document.querySelector('#'+prefix+name+'-status')?.textContent || '')]));
      return JSON.stringify({
        windowId:String(c.windowId || ''),sessionId:String(c.sessionId || ''),uiId:String(c.uiId || ''),
        rootRendered:!!document.querySelector('#framework-root') || !!route,
        initialRootReady:!!document.querySelector('#framework-root') && !route,
        firstMeaningfulPaint:d.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:!!route && window.location.pathname.endsWith('/flow-file-read-grant')
          && operations.every(name => !!document.querySelector('#'+prefix+name)),
        statuses,
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
