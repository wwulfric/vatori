import Cocoa
import Foundation
import UserNotifications

private var notificationOutcomesAcceptance: FlowIpcNotificationOutcomesAcceptance?

extension WebViewHost {
    func scheduleNotificationOutcomesAcceptanceIfReady() {
        guard config.flowIpcAutoNotificationOutcomes, config.transport == .flowIpc else {
            return
        }
        if notificationOutcomesAcceptance == nil {
            notificationOutcomesAcceptance = FlowIpcNotificationOutcomesAcceptance(
                host: self,
                deadline: Date().addingTimeInterval(
                    max(config.flowIpcAutoShutdownTimeoutSeconds, 60)),
                expectedMode: config.flowIpcNotificationExpectedOutcome)
        }
        notificationOutcomesAcceptance?.resume()
    }
}

/// Test-only driver. Never requests authorization, installs a notification delegate,
/// or observes notification content. OS permission responses belong to external UI.
private final class FlowIpcNotificationOutcomesAcceptance {
    private enum Phase: String {
        case root, route, authorizationBefore, invalid, show, authorizationAfter
        case secondaryOpening, denied, drain
    }
    private static let secondaryId = "notification-outcomes-secondary"
    private static let stages = ["main-invalid", "main-show", "secondary-denied"]
    private let host: WebViewHost
    private let deadline: Date
    private let expectedMode: String
    private let expectedOutcome: String
    private let sidecarPID: Int32
    private var phase = Phase.root
    private var terminal = false
    private var pollScheduled = false
    private var mainOwner: FlowIpcWindowContext?
    private var secondaryOwner: FlowIpcWindowContext?
    private var mainGeneration = -1
    private var secondaryGeneration = -1
    private var baseline = (requests: 0, responses: 0, pushes: 0)
    private var stageIndex = 0
    private var observedStages: [String] = []
    private var browserStages: [String] = []
    private var authorizationBefore = ""
    private var authorizationAfter = ""
    private var authorizationPending = false
    private var drainKey = ""
    private var drainPolls = 0

    init(host: WebViewHost, deadline: Date, expectedMode: String) {
        self.host = host
        self.deadline = deadline
        self.expectedMode = expectedMode
        self.sidecarPID = host.sidecarProcessIdentifier ?? 0
        switch expectedMode {
        case "authorization-required": expectedOutcome = "AUTHORIZATION_REQUIRED"
        case "os-denied": expectedOutcome = "OS_DENIED"
        case "scheduled": expectedOutcome = "SCHEDULED"
        default: expectedOutcome = ""
        }
    }

    func resume() { schedulePoll() }

    private func schedulePoll(_ delay: Double = 0.1) {
        guard !terminal, !pollScheduled else { return }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard !terminal else { return }
        guard !expectedOutcome.isEmpty, sidecarPID > 0,
              (expectedMode == "authorization-required")
                == host.config.hostNoActivate else {
            fail("notificationOutcomesInvalidConfiguration")
            return
        }
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retry("notificationOutcomesMainNavigationTimeout")
            return
        }
        if mainOwner == nil {
            mainOwner = main
            mainGeneration = main.recoveryGeneration
        }
        guard current(main) else {
            fail("notificationOutcomesMainIdentityChanged")
            return
        }
        inspect(main) { snapshot in
            guard let snapshot, snapshot.errorFree, self.validScope(snapshot, main) else {
                self.retry("notificationOutcomesMainInspectionFailed")
                return
            }
            self.advance(main, snapshot)
        }
    }

    private func advance(_ main: FlowIpcWindowContext, _ snapshot: Snapshot) {
        switch phase {
        case .root:
            guard snapshot.rootRendered, snapshot.firstPaint, snapshot.clients > 0 else {
                retry("notificationOutcomesRootTimeout")
                return
            }
            browserClick(main, button: "open-route", stage: "route-navigate", next: .route)
        case .route:
            guard snapshot.routeReady, snapshot.pending("invalid"),
                  snapshot.pending("show"), snapshot.pending("denied") else {
                retry("notificationOutcomesRouteTimeout")
                return
            }
            phase = .authorizationBefore
            observeAuthorization(main, before: true)
        case .authorizationBefore:
            guard !authorizationPending else {
                retry("notificationOutcomesAuthorizationBeforeTimeout")
                return
            }
            guard authorizationBefore == "not-determined" else {
                fail("notificationOutcomesRequiresFreshIdentity")
                return
            }
            beginStage(main, snapshot, index: 0, next: .invalid)
        case .invalid:
            guard observeStage(main, snapshot, index: 0),
                  snapshot.pending("show"), snapshot.pending("denied") else {
                retry("notificationOutcomesInvalidResultTimeout")
                return
            }
            beginStage(main, snapshot, index: 1, next: .show)
        case .show:
            if observeUnexpectedNativeError(main, snapshot) { return }
            guard observeStage(main, snapshot, index: 1),
                  snapshot.statuses["invalid"] == expectedStatus(0),
                  snapshot.pending("denied") else {
                retry("notificationOutcomesShowResultTimeout")
                return
            }
            phase = .authorizationAfter
            observeAuthorization(main, before: false)
        case .authorizationAfter:
            guard !authorizationPending else {
                retry("notificationOutcomesAuthorizationAfterTimeout")
                return
            }
            let authorizationValid: Bool
            switch expectedMode {
            case "authorization-required":
                authorizationValid = authorizationAfter == "not-determined"
            case "os-denied":
                authorizationValid = authorizationAfter == "denied"
            default:
                authorizationValid = ["authorized", "provisional", "ephemeral"]
                    .contains(authorizationAfter)
            }
            guard authorizationValid else {
                fail("notificationOutcomesAuthorizationMismatch")
                return
            }
            browserClick(main, button: "open-secondary",
                         stage: "secondary-open", next: .secondaryOpening)
        case .secondaryOpening:
            guard let secondary = host.flowIpcWindowContext(Self.secondaryId),
                  secondary.navigationFinished else {
                retry("notificationOutcomesSecondaryNavigationTimeout")
                return
            }
            if secondaryOwner == nil {
                secondaryOwner = secondary
                secondaryGeneration = secondary.recoveryGeneration
            }
            inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot, secondarySnapshot.errorFree,
                      self.validScope(secondarySnapshot, secondary),
                      secondarySnapshot.routeReady,
                      secondarySnapshot.pending("invalid"),
                      secondarySnapshot.pending("show"),
                      secondarySnapshot.pending("denied"),
                      secondary.scope.sessionId != main.scope.sessionId,
                      snapshot.windowOpened else {
                    self.retry("notificationOutcomesSecondaryRouteTimeout")
                    return
                }
                self.beginStage(secondary, secondarySnapshot, index: 2, next: .denied)
            }
        case .denied, .drain:
            guard let secondary = secondaryOwner, current(secondary) else {
                fail("notificationOutcomesSecondaryIdentityChanged")
                return
            }
            inspect(secondary) { secondarySnapshot in
                guard let secondarySnapshot, secondarySnapshot.errorFree,
                      self.validScope(secondarySnapshot, secondary),
                      self.observeStage(secondary, secondarySnapshot, index: 2),
                      self.finalStatuses(main: snapshot, secondary: secondarySnapshot) else {
                    self.retry("notificationOutcomesDeniedResultTimeout")
                    return
                }
                self.phase = .drain
                let key = "\(snapshot.requests):\(snapshot.responses):\(snapshot.pushes):"
                    + "\(secondarySnapshot.requests):\(secondarySnapshot.responses):\(secondarySnapshot.pushes)"
                if snapshot.requests == snapshot.responses,
                   secondarySnapshot.requests == secondarySnapshot.responses {
                    self.drainPolls = self.drainKey == key ? self.drainPolls + 1 : 1
                    self.drainKey = key
                } else {
                    self.drainKey = ""
                    self.drainPolls = 0
                }
                guard self.drainPolls >= 2 else {
                    self.retry("notificationOutcomesDrainTimeout", delay: 0.5)
                    return
                }
                self.finish(main: snapshot, secondary: secondarySnapshot)
            }
        }
    }

    private func observeAuthorization(_ context: FlowIpcWindowContext, before: Bool) {
        authorizationPending = true
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let status: String
            switch settings.authorizationStatus {
            case .notDetermined: status = "not-determined"
            case .denied: status = "denied"
            case .authorized: status = "authorized"
            case .provisional: status = "provisional"
            @unknown default: status = "unknown"
            }
            DispatchQueue.main.async {
                guard self.current(context), !self.terminal else { return }
                self.authorizationPending = false
                if before { self.authorizationBefore = status }
                else { self.authorizationAfter = status }
                var payload = context.scope.tracePayload
                payload["phase"] = before ? "before" : "after"
                payload["authorizationStatus"] = status
                payload["expectedOutcome"] = self.expectedOutcome
                payload["sidecarProcessIdentifier"] = self.sidecarPID
                payload["recoveryGeneration"] = context.recoveryGeneration
                payload["readOnly"] = true
                trace("recv", "flowNotificationOutcomes.authorizationObserved", payload)
                self.schedulePoll()
            }
        }
    }

    private func beginStage(_ context: FlowIpcWindowContext, _ snapshot: Snapshot,
                            index: Int, next: Phase) {
        stageIndex = index
        baseline = (snapshot.requests, snapshot.responses, snapshot.pushes)
        nativeClick(context, name: operation(index), stage: Self.stages[index], next: next)
    }

    private func observeStage(_ context: FlowIpcWindowContext,
                              _ snapshot: Snapshot, index: Int) -> Bool {
        let stage = Self.stages[index]
        guard snapshot.statuses[operation(index)] == expectedStatus(index),
              let pointer = snapshot.pointers[stage],
              pointer.receiptCount == 1, pointer.trustedCount == 1 else { return false }
        if observedStages.contains(stage) { return true }
        guard index == observedStages.count,
              snapshot.requests > baseline.requests,
              snapshot.responses > baseline.responses,
              snapshot.pushes > baseline.pushes else { return false }
        var payload = context.scope.tracePayload
        payload["index"] = index
        payload["stage"] = stage
        payload["outcome"] = outcome(index)
        payload["command"] = "notification.show"
        payload["success"] = scheduled(index)
        payload["scheduled"] = scheduled(index)
        payload["notificationIdPresent"] = scheduled(index)
        payload["errorCode"] = errorCode(index)
        payload["client"] = true
        payload["change"] = 1
        payload["capabilityContextCaptured"] = true
        payload["backgroundSessionAbsent"] = true
        payload["permissionMetadataValid"] = true
        payload["nativePointerReceipt"] = true
        payload["nativePointerTrusted"] = true
        payload["nativePointerReceiptCount"] = pointer.receiptCount
        payload["nativePointerTrustedCount"] = pointer.trustedCount
        payload["requestBaseline"] = baseline.requests
        payload["responseBaseline"] = baseline.responses
        payload["pushBaseline"] = baseline.pushes
        payload["requestCount"] = snapshot.requests
        payload["responseCount"] = snapshot.responses
        payload["pushMessageCount"] = snapshot.pushes
        payload["recoveryGeneration"] = context.recoveryGeneration
        payload["sidecarProcessIdentifier"] = sidecarPID
        trace("recv", "flowNotificationOutcomes.stageObserved", payload)
        observedStages.append(stage)
        return true
    }

    private func operation(_ index: Int) -> String { ["invalid", "show", "denied"][index] }

    /// A rendered native API error ends an OS-choice run as failure, never as a
    /// substitute permission outcome. Emit only fixed test metadata, no status
    /// text or NSError content, after the client/listener/push path is visible.
    private func observeUnexpectedNativeError(_ context: FlowIpcWindowContext,
                                              _ snapshot: Snapshot) -> Bool {
        for code in ["notification-authorization-failed", "notification-scheduling-failed"] {
            let status = "Notification show: outcome=ERROR success=false scheduled=false"
                + " notificationIdPresent=false errorCode=\(code)"
                + " client=true change=1 capabilityContextCaptured=true"
                + " backgroundSessionAbsent=true permissionMetadataValid=true"
            guard snapshot.statuses["show"] == status,
                  let pointer = snapshot.pointers[Self.stages[1]],
                  pointer.receiptCount == 1, pointer.trustedCount == 1,
                  snapshot.requests > baseline.requests,
                  snapshot.responses > baseline.responses,
                  snapshot.pushes > baseline.pushes else { continue }
            var payload = context.scope.tracePayload
            payload["outcome"] = "ERROR"
            payload["errorCode"] = code
            payload["client"] = true
            payload["nativePointerTrusted"] = true
            payload["visibleStatusObserved"] = true
            payload["requestCount"] = snapshot.requests
            payload["responseCount"] = snapshot.responses
            payload["pushMessageCount"] = snapshot.pushes
            trace("recv", "flowNotificationOutcomes.unexpectedErrorObserved", payload)
            fail("notificationOutcomesUnexpectedNativeError")
            return true
        }
        return false
    }

    private func outcome(_ index: Int) -> String {
        index == 0 ? "ERROR" : index == 1 ? expectedOutcome : "DENIED"
    }
    private func scheduled(_ index: Int) -> Bool {
        index == 1 && expectedOutcome == "SCHEDULED"
    }
    private func errorCode(_ index: Int) -> String {
        if index == 0 { return "notification-title-invalid" }
        if index == 2 || expectedOutcome == "SCHEDULED" { return "" }
        return expectedOutcome == "OS_DENIED"
            ? "notification-authorization-denied" : "notification-authorization-required"
    }
    private func expectedStatus(_ index: Int) -> String {
        let code = errorCode(index)
        return "Notification \(operation(index)): outcome=\(outcome(index))"
            + " success=\(scheduled(index)) scheduled=\(scheduled(index))"
            + " notificationIdPresent=\(scheduled(index))"
            + " errorCode=\(code.isEmpty ? "none" : code)"
            + " client=true change=1 capabilityContextCaptured=true"
            + " backgroundSessionAbsent=true permissionMetadataValid=true"
    }

    private func finalStatuses(main: Snapshot, secondary: Snapshot) -> Bool {
        main.statuses["invalid"] == expectedStatus(0)
            && main.statuses["show"] == expectedStatus(1)
            && main.pending("denied")
            && secondary.pending("invalid") && secondary.pending("show")
            && secondary.statuses["denied"] == expectedStatus(2)
            && Self.stages.prefix(2).allSatisfy {
                main.pointers[$0]?.receiptCount == 1
                    && main.pointers[$0]?.trustedCount == 1
            }
    }

    private func current(_ context: FlowIpcWindowContext) -> Bool {
        guard host.sidecarProcessIdentifier == sidecarPID,
              host.flowIpcWindowContext(context.scope.windowId) === context else {
            return false
        }
        if context.scope.windowId == "main" {
            return mainOwner === context && context.recoveryGeneration == mainGeneration
        }
        return secondaryOwner === context
            && context.recoveryGeneration == secondaryGeneration
    }

    private func validScope(_ snapshot: Snapshot, _ context: FlowIpcWindowContext) -> Bool {
        guard current(context), snapshot.windowId == context.scope.windowId,
              snapshot.sessionId == context.scope.sessionId,
              snapshot.uiId == context.scope.uiId else { return false }
        if context.scope.windowId == "main" {
            return snapshot.sessionId == "flow-ipc-session" && snapshot.uiId == "main"
        }
        let prefix = "flow-ipc-window:"
        return snapshot.windowId == Self.secondaryId && snapshot.uiId == Self.secondaryId
            && snapshot.sessionId.hasPrefix(prefix)
            && UUID(uuidString: String(snapshot.sessionId.dropFirst(prefix.count))) != nil
    }

    private func inspect(_ context: FlowIpcWindowContext,
                         completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard !self.terminal, self.current(context), error == nil,
                  let json = result as? String, let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func browserClick(_ context: FlowIpcWindowContext, button: String,
                              stage: String, next: Phase) {
        context.webView.evaluateJavaScript("""
        (() => {
          const button = document.querySelector(
            '#framework-flow-notification-outcomes-\(button)');
          if (!button || typeof button.click !== 'function') return false;
          button.click(); return true;
        })()
        """) { result, error in
            guard self.current(context), error == nil, result as? Bool == true else {
                self.retry("notificationOutcomesSetupFailed")
                return
            }
            self.browserStages.append(stage)
            self.phase = next
            self.schedulePoll(0.2)
        }
    }

    private func nativeClick(_ context: FlowIpcWindowContext, name: String,
                             stage: String, next: Phase) {
        context.webView.evaluateJavaScript(Self.preparePointer(name, stage)) { result, error in
            guard self.current(context), error == nil,
                  let json = result as? String, let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: data),
                  geometry.ready else {
                self.retry("notificationOutcomesPointerPreparationFailed")
                return
            }
            FlowIpcNativeInputActivation.prepare(
                context: context, deadline: self.deadline,
                onFailure: { diagnostics in
                    self.fail("notificationOutcomesActivationFailed", activation: diagnostics)
                }
            ) {
                guard self.current(context), !self.terminal else { return }
                let point = context.webView.convert(NSPoint(
                    x: geometry.x,
                    y: context.webView.isFlipped ? geometry.y
                        : context.webView.bounds.height - geometry.y), to: nil)
                guard context.window.contentLayoutRect.contains(point) else {
                    self.fail("notificationOutcomesPointerOutsideWindow")
                    return
                }
                let number = Int(Date().timeIntervalSince1970 * 1000) & 0x7fff
                guard let down = NSEvent.mouseEvent(
                    with: .leftMouseDown, location: point, modifierFlags: [],
                    timestamp: ProcessInfo.processInfo.systemUptime,
                    windowNumber: context.window.windowNumber, context: nil,
                    eventNumber: number, clickCount: 1, pressure: 1),
                      let up = NSEvent.mouseEvent(
                        with: .leftMouseUp, location: point, modifierFlags: [],
                        timestamp: ProcessInfo.processInfo.systemUptime + 0.05,
                        windowNumber: context.window.windowNumber, context: nil,
                        eventNumber: number + 1, clickCount: 1, pressure: 0) else {
                    self.fail("notificationOutcomesPointerCreationFailed")
                    return
                }
                var payload = context.scope.tracePayload
                payload["stage"] = stage
                payload["index"] = self.stageIndex
                payload["inputMode"] = "appkit-window-pointer-event"
                payload["button"] = "primary"
                payload["activeApplication"] = NSApplication.shared.isActive
                payload["keyWindow"] = context.window.isKeyWindow
                payload["webViewX"] = geometry.x
                payload["webViewY"] = geometry.y
                payload["windowX"] = point.x
                payload["windowY"] = point.y
                payload["recoveryGeneration"] = context.recoveryGeneration
                payload["sidecarProcessIdentifier"] = self.sidecarPID
                trace("recv", "flowNotificationOutcomes.nativePointerDispatch", payload)
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard self.current(context), !self.terminal else { return }
                    context.window.sendEvent(up)
                    self.phase = next
                    self.schedulePoll()
                }
            }
        }
    }

    private func retry(_ code: String, delay: Double = 0.1) {
        guard Date() < deadline else { fail(code); return }
        schedulePoll(delay)
    }

    private func fail(_ code: String, activation: [String: Any] = [:]) {
        guard !terminal else { return }
        terminal = true
        var payload: [String: Any] = [
            "code": code, "phase": phase.rawValue,
            "expectedOutcome": expectedOutcome,
            "authorizationBefore": authorizationBefore,
            "authorizationAfter": authorizationAfter,
            "observedStages": observedStages.joined(separator: ",")
        ]
        payload["activeApplication"] = activation["activeApplication"] as? Bool
        payload["keyWindow"] = activation["keyWindow"] as? Bool
        trace("send", "error", payload)
        host.flowIpcWindowContext(Self.secondaryId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private func finish(main: Snapshot, secondary: Snapshot) {
        guard let mainOwner, let secondaryOwner,
              current(mainOwner), current(secondaryOwner),
              observedStages == Self.stages,
              browserStages == ["route-navigate", "secondary-open"] else {
            fail("notificationOutcomesFinalInvariantFailed")
            return
        }
        terminal = true
        trace("recv", "flowNotificationOutcomes.frameworkAcceptance", [
            "complete": true,
            "expectedOutcome": expectedOutcome,
            "invalidOutcome": "ERROR", "deniedOutcome": "DENIED",
            "standardBundleMode": main.firstPaint && secondary.firstPaint,
            "mainFlowClientOwned": main.clients > 0,
            "secondaryFlowClientOwned": secondary.clients > 0,
            "scopeIsolationObserved": main.sessionId != secondary.sessionId,
            "nativePointerObserved": true, "nativePointerTrustedObserved": true,
            "nativePointerEventCount": 3, "nativePointerTrustedEventCount": 3,
            "nativePointerStages": observedStages.joined(separator: ","),
            "browserProgrammaticStages": browserStages.joined(separator: ","),
            "capabilityContextCapturedObserved": true,
            "backgroundSessionAbsentObserved": true,
            "permissionPayloadPreservedObserved": true,
            "mainJavaListenerObserved": true, "secondaryJavaListenerObserved": true,
            "mainUidlPatchObserved": true, "secondaryUidlPatchObserved": true,
            "mainPushObserved": true, "secondaryPushObserved": true,
            "balancedDrainObserved": drainPolls >= 2,
            "resultStableAfterDrain": finalStatuses(main: main, secondary: secondary),
            "errorFree": main.errorFree && secondary.errorFree,
            "authorizationBefore": authorizationBefore,
            "authorizationAfter": authorizationAfter,
            "authorizationReadOnlyObserved": true,
            "bannerVisibilityClaimed": false,
            "mainWindowId": main.windowId, "mainSessionId": main.sessionId,
            "mainUiId": main.uiId, "secondaryWindowId": secondary.windowId,
            "secondarySessionId": secondary.sessionId, "secondaryUiId": secondary.uiId,
            "mainRequestCount": main.requests, "mainResponseCount": main.responses,
            "mainPushMessageCount": main.pushes,
            "secondaryRequestCount": secondary.requests,
            "secondaryResponseCount": secondary.responses,
            "secondaryPushMessageCount": secondary.pushes,
            "sidecarProcessIdentifier": sidecarPID,
            "mainRecoveryGeneration": mainGeneration,
            "secondaryRecoveryGeneration": secondaryGeneration,
            "sameMainWindow": current(mainOwner),
            "sameMainWebView": current(mainOwner),
            "sameSecondaryWindow": current(secondaryOwner),
            "sameSecondaryWebView": current(secondaryOwner)
        ])
        secondaryOwner.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            mainOwner.window.close()
        }
    }

    private struct Geometry: Decodable { let ready: Bool; let x: Double; let y: Double }
    private struct Pointer: Decodable { let receiptCount: Int; let trustedCount: Int }
    private struct Snapshot: Decodable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let rootRendered: Bool
        let firstPaint: Bool
        let clients: Int
        let routeReady: Bool
        let windowOpened: Bool
        let statuses: [String: String]
        let pointers: [String: Pointer]
        let requests: Int
        let responses: Int
        let pushes: Int
        let errorFree: Bool
        func pending(_ name: String) -> Bool {
            statuses[name] == "Notification \(name): pending"
        }
    }

    private static func preparePointer(_ name: String, _ stage: String) -> String {
        """
        (() => {
          const button = document.querySelector(
            '#framework-flow-notification-outcomes-invoke-\(name)');
          if (!button) return JSON.stringify({ready:false, x:0, y:0});
          button.scrollIntoView({block:'center'});
          const rect = button.getBoundingClientRect();
          const x = rect.left + rect.width / 2, y = rect.top + rect.height / 2;
          const hit = document.elementFromPoint(x,y);
          if (!(rect.width > 0 && rect.height > 0)
              || !(hit === button || button.contains(hit)))
            return JSON.stringify({ready:false,x,y});
          const state = window.VatoriNotificationPointers ||= {};
          if (!state['\(stage)']) {
            state['\(stage)'] = {receiptCount:0, trustedCount:0};
            document.addEventListener('click', event => {
              if (event.button !== 0 || !event.composedPath().includes(button)) return;
              state['\(stage)'].receiptCount++;
              if (event.isTrusted) state['\(stage)'].trustedCount++;
            }, true);
          }
          return JSON.stringify({ready:true,x,y});
        })()
        """
    }

    private static let inspectionScript = """
    (() => {
      const context = window.VaadinDesktopHostContext || {};
      const d = window.VaadinDesktopIpcDiagnostics || {};
      const prefix = '#framework-flow-notification-outcomes-';
      const statuses = {};
      for (const name of ['invalid','show','denied'])
        statuses[name] = String(document.querySelector(prefix+name+'-status')?.textContent||'');
      const route = !!document.querySelector(prefix+'route');
      return JSON.stringify({
        windowId:String(context.windowId||''), sessionId:String(context.sessionId||''),
        uiId:String(context.uiId||''),
        rootRendered:route || !!document.querySelector('#framework-root'),
        firstPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        clients:Object.keys(window.Vaadin?.Flow?.clients||{})
          .filter(name=>name!=='TypeScript').length,
        routeReady:route && location.pathname.endsWith('/flow-notification-outcomes')
          && ['invalid','show','denied'].every(name=>document.querySelector(prefix+'invoke-'+name)),
        windowOpened:document.querySelector(prefix+'window-status')?.textContent
          === 'Notification window: opened=true client=true',
        statuses, pointers:window.VatoriNotificationPointers||{},
        requests:Number(d.standardBundleOpaqueRequestCount||0),
        responses:Number(d.standardBundleOpaqueResponseCount||0),
        pushes:Number(d.standardBundlePushMessageCount||0),
        errorFree:!d.standardBundleBrowserError && !d.standardBundleOpaqueRequestError
          && !d.standardBundleOpaqueResponseError && !d.standardBundlePushError
      });
    })()
    """
}
