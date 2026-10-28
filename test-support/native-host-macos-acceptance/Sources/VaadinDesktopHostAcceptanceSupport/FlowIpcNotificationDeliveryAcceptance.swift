import Cocoa
import CryptoKit
import Foundation
import UserNotifications

private var notificationDeliveryAcceptance: FlowIpcNotificationDeliveryAcceptance?

extension WebViewHost {
    func scheduleNotificationDeliveryAcceptanceIfReady() {
        guard config.flowIpcAutoNotificationDelivery, config.transport == .flowIpc else {
            return
        }
        if notificationDeliveryAcceptance == nil {
            notificationDeliveryAcceptance = FlowIpcNotificationDeliveryAcceptance(
                host: self,
                deadline: Date().addingTimeInterval(
                    max(config.flowIpcAutoShutdownTimeoutSeconds, 60)))
        }
        notificationDeliveryAcceptance?.resume()
    }
}

func cleanupNotificationDeliveryOnTermination() {
    notificationDeliveryAcceptance?.terminate()
}

/// Test-only observation of the unchanged production notification path. Never
/// installs a notification delegate, requests authorization, reads content, or
/// claims that an OS-delivered notification was displayed as a visible banner.
private final class FlowIpcNotificationDeliveryAcceptance {
    private enum Phase: String {
        case root, route, authorizationBefore, prepare, authorizationAfter
        case probe, delivery, observationWindow, drain
    }
    private static let stages = ["prepare", "probe"]
    private let host: WebViewHost
    private let deadline: Date
    private let sidecarPID: Int32
    private var owner: FlowIpcWindowContext?
    private var generation = -1
    private var phase = Phase.root
    private var terminal = false
    private var timeoutWorkItem: DispatchWorkItem?
    private var pollScheduled = false
    private var authorizationPending = false
    private var deliveryPending = false
    private var authorizationBefore = ""
    private var authorizationAfter = ""
    private var baseline = (requests: 0, responses: 0, pushes: 0)
    private var observedStages: [String] = []
    private var browserStages: [String] = []
    private var digests: [String: String] = [:]
    // The only raw identifiers are read from the real OS query and retained in
    // memory after exact digest matching against a public Java API result.
    private var cleanupIdentifiers: Set<String> = []
    private var resignObservers: [NSObjectProtocol] = []
    private var applicationResignActiveCount = 0
    private var windowResignKeyCount = 0
    private var foregroundIntervalStarted = false
    private var foregroundIntervalCompleted = false
    private var probeDispatchDate: Date?
    private var deliveredObserved = false
    private var observationWindowEnds: Date?
    private var drainKey = ""
    private var drainPolls = 0

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
        self.sidecarPID = host.sidecarProcessIdentifier ?? 0
    }

    func resume() {
        if timeoutWorkItem == nil && !terminal {
            let timeout = DispatchWorkItem { [weak self] in
                self?.fail("notificationDeliveryTimeout")
            }
            timeoutWorkItem = timeout
            DispatchQueue.main.asyncAfter(
                deadline: .now() + max(0, deadline.timeIntervalSinceNow), execute: timeout)
        }
        schedulePoll()
    }

    func terminate() {
        terminal = true
        timeoutWorkItem?.cancel()
        removeForegroundObservers()
        requestTestNotificationCleanup()
    }

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
        guard Date() < deadline else {
            fail("notificationDeliveryTimeout")
            return
        }
        guard sidecarPID > 0, !host.config.hostNoActivate,
              UNUserNotificationCenter.current().delegate == nil else {
            fail("notificationDeliveryInvalidConfiguration")
            return
        }
        guard let context = host.flowIpcWindowContext("main"),
              context.navigationFinished else {
            schedulePoll()
            return
        }
        if owner == nil {
            owner = context
            generation = context.recoveryGeneration
        }
        guard current(context) else {
            fail("notificationDeliveryOwnerChanged")
            return
        }
        if foregroundIntervalStarted && !foregroundIntervalCompleted
                && !foregroundUninterrupted(context) {
            fail("notificationDeliveryForegroundInterrupted")
            return
        }
        inspect(context) { snapshot in
            guard let snapshot, snapshot.errorFree, self.validScope(snapshot, context) else {
                self.schedulePoll()
                return
            }
            self.advance(context, snapshot)
        }
    }

    private func advance(_ context: FlowIpcWindowContext, _ snapshot: Snapshot) {
        switch phase {
        case .root:
            guard snapshot.rootRendered, snapshot.firstPaint, snapshot.clients > 0 else {
                schedulePoll()
                return
            }
            navigate(context)
        case .route:
            guard snapshot.routeReady, Self.stages.allSatisfy(snapshot.pending) else {
                schedulePoll()
                return
            }
            phase = .authorizationBefore
            observeAuthorization(context, before: true)
        case .authorizationBefore:
            guard !authorizationPending else { schedulePoll(); return }
            guard authorizationBefore == "not-determined" else {
                fail("notificationDeliveryRequiresFreshIdentity")
                return
            }
            beginStage(context, snapshot, index: 0, next: .prepare)
        case .prepare:
            guard observeStage(context, snapshot, index: 0) else { return }
            phase = .authorizationAfter
            observeAuthorization(context, before: false)
        case .authorizationAfter:
            guard !authorizationPending else { schedulePoll(); return }
            guard authorizationAfter == "authorized" else {
                fail("notificationDeliveryRequiresAuthorization")
                return
            }
            beginStage(context, snapshot, index: 1, next: .probe)
        case .probe:
            guard observeStage(context, snapshot, index: 1) else { return }
            phase = .delivery
            queryDelivered(context)
        case .delivery:
            guard finalStatuses(snapshot) else {
                fail("notificationDeliveryResultChanged")
                return
            }
            queryDelivered(context)
        case .observationWindow:
            // Human observation is intentionally not part of the machine gate.
            // Do not reactivate the app or prolong foreground tracking here.
            guard finalStatuses(snapshot), let observationWindowEnds else {
                fail("notificationDeliveryResultChanged")
                return
            }
            if Date() < observationWindowEnds { schedulePoll(0.25); return }
            phase = .drain
            schedulePoll()
        case .drain:
            guard finalStatuses(snapshot) else {
                fail("notificationDeliveryResultChanged")
                return
            }
            let key = "\(snapshot.requests):\(snapshot.responses):\(snapshot.pushes)"
            if snapshot.requests == snapshot.responses {
                drainPolls = drainKey == key ? drainPolls + 1 : 1
                drainKey = key
            } else {
                drainKey = ""
                drainPolls = 0
            }
            guard drainPolls >= 2 else { schedulePoll(0.5); return }
            finish(context, snapshot)
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
                guard !self.terminal, self.current(context) else { return }
                self.authorizationPending = false
                if before { self.authorizationBefore = status }
                else { self.authorizationAfter = status }
                var payload = self.identity(context)
                payload["phase"] = before ? "before" : "after"
                payload["authorizationStatus"] = status
                payload["readOnly"] = true
                payload["notificationDelegateNil"] =
                    UNUserNotificationCenter.current().delegate == nil
                trace("recv", "flowNotificationDelivery.authorizationObserved", payload)
                self.schedulePoll()
            }
        }
    }

    private func beginStage(_ context: FlowIpcWindowContext, _ snapshot: Snapshot,
                            index: Int, next: Phase) {
        baseline = (snapshot.requests, snapshot.responses, snapshot.pushes)
        nativeClick(context, index: index, next: next)
    }

    private func observeStage(_ context: FlowIpcWindowContext,
                              _ snapshot: Snapshot, index: Int) -> Bool {
        let stage = Self.stages[index]
        let status = snapshot.statuses[stage] ?? ""
        if !snapshot.pending(stage) && status != expectedStatus(stage) {
            // Never log the rendered status: it may contain an unexpected error.
            fail("notificationDeliveryUnexpectedPublicResult")
            return false
        }
        guard status == expectedStatus(stage),
              let digest = snapshot.digests[stage], Self.validDigest(digest),
              let pointer = snapshot.pointers[stage],
              pointer.receiptCount == 1, pointer.trustedCount == 1,
              snapshot.requests > baseline.requests,
              snapshot.responses > baseline.responses,
              snapshot.pushes > baseline.pushes else {
            schedulePoll()
            return false
        }
        guard index == observedStages.count,
              !digests.values.contains(digest) else {
            fail("notificationDeliveryDuplicateStageOrIdentifier")
            return false
        }
        var payload = identity(context)
        payload["index"] = index
        payload["stage"] = stage
        payload["outcome"] = "SCHEDULED"
        payload["command"] = "notification.show"
        payload["success"] = true
        payload["scheduled"] = true
        payload["notificationIdPresent"] = true
        payload["errorCode"] = ""
        payload["notificationDigest"] = digest
        payload["digestAlgorithm"] = "sha256"
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
        trace("recv", "flowNotificationDelivery.stageObserved", payload)
        digests[stage] = digest
        observedStages.append(stage)
        if stage == "prepare" { capturePreparationCleanupIdentifier(digest) }
        return true
    }

    private func capturePreparationCleanupIdentifier(_ digest: String) {
        // Observe early so failure before the probe query can still clean up a
        // preparation notification. This is best effort: a later delivery may
        // not yet be returned by this read-only query.
        UNUserNotificationCenter.current().getDeliveredNotifications { notifications in
            let identifiers = notifications.compactMap { notification -> String? in
                let identifier = notification.request.identifier
                return Self.digest(identifier) == digest ? identifier : nil
            }
            DispatchQueue.main.async {
                guard self.digests["prepare"] == digest else { return }
                self.cleanupIdentifiers.formUnion(identifiers)
                if self.terminal { self.requestTestNotificationCleanup() }
            }
        }
    }

    private func queryDelivered(_ context: FlowIpcWindowContext) {
        guard !deliveryPending, let probeDigest = digests["probe"],
              let probeDispatchDate else { return }
        deliveryPending = true
        let queryStartedAt = Date()
        UNUserNotificationCenter.current().getDeliveredNotifications { notifications in
            // Read identifiers and the OS notification date only, never content.
            let observations = notifications.map {
                (identifier: $0.request.identifier,
                 digest: Self.digest($0.request.identifier), date: $0.date)
            }
            DispatchQueue.main.async {
                self.deliveryPending = false
                let allowedDigests = Set(self.digests.values)
                for item in observations where allowedDigests.contains(item.digest) {
                    self.cleanupIdentifiers.insert(item.identifier)
                }
                // A late callback may improve best-effort cleanup, but must
                // never revive acceptance or emit a post-terminal success.
                if self.terminal {
                    self.requestTestNotificationCleanup()
                    return
                }
                guard self.current(context) else {
                    self.fail("notificationDeliveryOwnerChanged")
                    return
                }
                guard UNUserNotificationCenter.current().delegate == nil,
                      self.foregroundUninterrupted(context) else {
                    self.fail("notificationDeliveryForegroundInterrupted")
                    return
                }
                let matches = observations.filter { $0.digest == probeDigest }
                guard matches.count <= 1 else {
                    self.fail("notificationDeliveryDuplicateSystemMatch")
                    return
                }
                guard let match = matches.first else { self.schedulePoll(0.2); return }
                let observedAt = Date()
                guard match.date >= probeDispatchDate, match.date <= observedAt else {
                    self.fail("notificationDeliveryDateOutsideProbeInterval")
                    return
                }
                var payload = self.identity(context)
                payload["stage"] = "probe"
                payload["notificationDigest"] = probeDigest
                payload["digestAlgorithm"] = "sha256"
                payload["matchingCount"] = matches.count
                payload["exactPublicIdentifierDigestMatched"] = true
                payload["notificationDateAtOrAfterProbeDispatch"] = true
                payload["probeDispatchAt"] = probeDispatchDate.timeIntervalSince1970 * 1000
                payload["queryStartedAt"] = queryStartedAt.timeIntervalSince1970 * 1000
                // UNNotification.date is the notification's date, not an exact
                // callback or banner-display timestamp.
                payload["notificationDateAt"] = match.date.timeIntervalSince1970 * 1000
                payload["observedAt"] = observedAt.timeIntervalSince1970 * 1000
                payload["activeApplication"] = NSApplication.shared.isActive
                payload["keyWindow"] = context.window.isKeyWindow
                payload["notificationDelegateNil"] = true
                payload["applicationResignActiveCount"] = self.applicationResignActiveCount
                payload["windowResignKeyCount"] = self.windowResignKeyCount
                payload["foregroundIntervalUninterrupted"] = true
                payload["readOnly"] = true
                trace("recv", "flowNotificationDelivery.deliveryObserved", payload)
                self.deliveredObserved = true
                self.foregroundIntervalCompleted = true
                self.removeForegroundObservers()
                self.observationWindowEnds = observedAt.addingTimeInterval(8)
                self.phase = .observationWindow
                self.schedulePoll(0.25)
            }
        }
    }

    private func startForegroundInterval(_ context: FlowIpcWindowContext) {
        let center = NotificationCenter.default
        foregroundIntervalStarted = true
        // AppKit posts application/window lifecycle events on its main thread.
        // nil preserves synchronous delivery on that posting thread: a queued
        // observer must not let an earlier resignation arrive after our query.
        resignObservers = [
            center.addObserver(forName: NSApplication.didResignActiveNotification,
                               object: NSApplication.shared, queue: nil) { [weak self] _ in
                self?.applicationResignActiveCount += 1
            },
            center.addObserver(forName: NSWindow.didResignKeyNotification,
                               object: context.window, queue: nil) { [weak self] _ in
                self?.windowResignKeyCount += 1
            }
        ]
    }

    private func foregroundUninterrupted(_ context: FlowIpcWindowContext) -> Bool {
        foregroundIntervalStarted && applicationResignActiveCount == 0
            && windowResignKeyCount == 0 && NSApplication.shared.isActive
            && context.window.isKeyWindow
    }

    private func removeForegroundObservers() {
        for observer in resignObservers { NotificationCenter.default.removeObserver(observer) }
        resignObservers.removeAll()
    }

    @discardableResult
    private func requestTestNotificationCleanup() -> Int {
        let identifiers = Array(cleanupIdentifiers)
        guard !identifiers.isEmpty else { return 0 }
        let center = UNUserNotificationCenter.current()
        // This is scoped test cleanup, not a public cancellation guarantee or
        // confirmation that the OS removed an already displayed notification.
        // Product notifications have no trigger and hence no pending request to
        // remove. Only exact, already observed delivered identifiers are used.
        center.removeDeliveredNotifications(withIdentifiers: identifiers)
        cleanupIdentifiers.removeAll()
        return identifiers.count
    }

    private func current(_ context: FlowIpcWindowContext) -> Bool {
        host.sidecarProcessIdentifier == sidecarPID && owner === context
            && host.flowIpcWindowContext("main") === context
            && context.recoveryGeneration == generation
    }

    private func validScope(_ snapshot: Snapshot, _ context: FlowIpcWindowContext) -> Bool {
        current(context) && snapshot.windowId == context.scope.windowId
            && snapshot.sessionId == context.scope.sessionId
            && snapshot.uiId == context.scope.uiId && snapshot.windowId == "main"
            && snapshot.sessionId == "flow-ipc-session" && snapshot.uiId == "main"
    }

    private func identity(_ context: FlowIpcWindowContext) -> [String: Any] {
        var payload = context.scope.tracePayload
        payload["sidecarProcessIdentifier"] = sidecarPID
        payload["recoveryGeneration"] = context.recoveryGeneration
        return payload
    }

    private func expectedStatus(_ stage: String) -> String {
        "Notification \(stage): outcome=SCHEDULED success=true scheduled=true"
            + " notificationIdPresent=true client=true change=1"
            + " capabilityContextCaptured=true backgroundSessionAbsent=true"
            + " permissionMetadataValid=true"
    }

    private func finalStatuses(_ snapshot: Snapshot) -> Bool {
        observedStages == Self.stages && Self.stages.allSatisfy {
            snapshot.statuses[$0] == expectedStatus($0)
                && snapshot.digests[$0] == digests[$0]
                && snapshot.pointers[$0]?.receiptCount == 1
                && snapshot.pointers[$0]?.trustedCount == 1
        }
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

    private func navigate(_ context: FlowIpcWindowContext) {
        context.webView.evaluateJavaScript("""
        (() => {
          const button = document.querySelector('#framework-flow-notification-delivery-open-route');
          if (!button || typeof button.click !== 'function') return false;
          button.click(); return true;
        })()
        """) { result, error in
            guard !self.terminal, self.current(context), error == nil,
                  result as? Bool == true else { self.schedulePoll(); return }
            self.browserStages.append("route-navigate")
            self.phase = .route
            self.schedulePoll(0.2)
        }
    }

    private func nativeClick(_ context: FlowIpcWindowContext, index: Int, next: Phase) {
        let stage = Self.stages[index]
        context.webView.evaluateJavaScript(Self.preparePointer(stage)) { result, error in
            guard !self.terminal, self.current(context), error == nil,
                  let json = result as? String, let data = json.data(using: .utf8),
                  let geometry = try? JSONDecoder().decode(Geometry.self, from: data),
                  geometry.ready else { self.schedulePoll(); return }
            FlowIpcNativeInputActivation.prepare(
                context: context, deadline: self.deadline,
                onFailure: { _ in self.fail("notificationDeliveryActivationFailed") }
            ) {
                guard !self.terminal, self.current(context) else { return }
                let point = context.webView.convert(NSPoint(
                    x: geometry.x,
                    y: context.webView.isFlipped ? geometry.y
                        : context.webView.bounds.height - geometry.y), to: nil)
                guard context.window.contentLayoutRect.contains(point) else {
                    self.fail("notificationDeliveryPointerOutsideWindow")
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
                    self.fail("notificationDeliveryPointerCreationFailed")
                    return
                }
                if stage == "probe" {
                    self.startForegroundInterval(context)
                }
                let dispatchDate = Date()
                if stage == "probe" { self.probeDispatchDate = dispatchDate }
                var payload = self.identity(context)
                payload["index"] = index
                payload["stage"] = stage
                payload["inputMode"] = "appkit-window-pointer-event"
                payload["button"] = "primary"
                payload["activeApplication"] = NSApplication.shared.isActive
                payload["keyWindow"] = context.window.isKeyWindow
                payload["dispatchedAt"] = dispatchDate.timeIntervalSince1970 * 1000
                payload["webViewX"] = geometry.x
                payload["webViewY"] = geometry.y
                payload["windowX"] = point.x
                payload["windowY"] = point.y
                trace("recv", "flowNotificationDelivery.nativePointerDispatch", payload)
                context.window.sendEvent(down)
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                    guard !self.terminal, self.current(context) else { return }
                    context.window.sendEvent(up)
                    self.phase = next
                    self.schedulePoll()
                }
            }
        }
    }

    private func fail(_ code: String) {
        guard !terminal else { return }
        terminal = true
        timeoutWorkItem?.cancel()
        removeForegroundObservers()
        let cleaned = requestTestNotificationCleanup()
        trace("send", "error", [
            "code": code, "phase": phase.rawValue,
            "observedStages": observedStages.joined(separator: ","),
            "authorizationBefore": authorizationBefore,
            "authorizationAfter": authorizationAfter,
            "applicationResignActiveCount": applicationResignActiveCount,
            "windowResignKeyCount": windowResignKeyCount,
            "testCleanupIdentifierCount": cleaned
        ])
        host.flowIpcWindowContext("main")?.window.close()
    }

    private func finish(_ context: FlowIpcWindowContext, _ snapshot: Snapshot) {
        guard current(context), deliveredObserved, foregroundIntervalCompleted,
              applicationResignActiveCount == 0, windowResignKeyCount == 0,
              finalStatuses(snapshot), browserStages == ["route-navigate"],
              UNUserNotificationCenter.current().delegate == nil else {
            fail("notificationDeliveryFinalInvariantFailed")
            return
        }
        terminal = true
        timeoutWorkItem?.cancel()
        let cleaned = requestTestNotificationCleanup()
        trace("recv", "flowNotificationDelivery.frameworkAcceptance", [
            "complete": true, "standardBundleMode": snapshot.firstPaint,
            "mainFlowClientOwned": snapshot.clients > 0,
            "nativePointerObserved": true, "nativePointerTrustedObserved": true,
            "nativePointerEventCount": 2, "nativePointerTrustedEventCount": 2,
            "nativePointerStages": observedStages.joined(separator: ","),
            "browserProgrammaticStages": browserStages.joined(separator: ","),
            "capabilityContextCapturedObserved": true,
            "backgroundSessionAbsentObserved": true,
            "permissionPayloadPreservedObserved": true,
            "mainJavaListenerObserved": true, "mainUidlPatchObserved": true,
            "mainPushObserved": true, "balancedDrainObserved": drainPolls >= 2,
            "resultStableAfterDrain": finalStatuses(snapshot), "errorFree": snapshot.errorFree,
            "authorizationBefore": authorizationBefore, "authorizationAfter": authorizationAfter,
            "authorizationReadOnlyObserved": true, "notificationDelegateNil": true,
            "foregroundIntervalUninterrupted": true, "deliveredNotificationMatched": true,
            "applicationResignActiveCount": applicationResignActiveCount,
            "windowResignKeyCount": windowResignKeyCount, "bannerVisibilityClaimed": false,
            "prepareNotificationDigest": digests["prepare"] ?? "",
            "probeNotificationDigest": digests["probe"] ?? "",
            "mainWindowId": snapshot.windowId, "mainSessionId": snapshot.sessionId,
            "mainUiId": snapshot.uiId, "mainRequestCount": snapshot.requests,
            "mainResponseCount": snapshot.responses, "mainPushMessageCount": snapshot.pushes,
            "sidecarProcessIdentifier": sidecarPID, "mainRecoveryGeneration": generation,
            "sameMainWindow": current(context), "sameMainWebView": current(context),
            "testCleanupRequested": cleaned > 0, "testCleanupIdentifierCount": cleaned
        ])
        context.window.close()
    }

    private static func validDigest(_ value: String) -> Bool {
        value.count == 64 && value.utf8.allSatisfy {
            (48...57).contains($0) || (97...102).contains($0)
        }
    }

    private static func digest(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined()
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
        let statuses: [String: String]
        let digests: [String: String]
        let pointers: [String: Pointer]
        let requests: Int
        let responses: Int
        let pushes: Int
        let errorFree: Bool
        func pending(_ stage: String) -> Bool {
            statuses[stage] == "Notification \(stage): pending"
        }
    }

    private static func preparePointer(_ stage: String) -> String {
        """
        (() => {
          const button = document.querySelector('#framework-flow-notification-delivery-invoke-\(stage)');
          if (!button) return JSON.stringify({ready:false,x:0,y:0});
          button.scrollIntoView({block:'center'});
          const rect = button.getBoundingClientRect();
          const x = rect.left + rect.width/2, y = rect.top + rect.height/2;
          const hit = document.elementFromPoint(x,y);
          if (!(rect.width>0 && rect.height>0) || !(hit===button || button.contains(hit)))
            return JSON.stringify({ready:false,x,y});
          const state = window.VatoriNotificationDeliveryPointers ||= {};
          if (!state['\(stage)']) {
            state['\(stage)'] = {receiptCount:0,trustedCount:0};
            document.addEventListener('click', event => {
              if (event.button!==0 || !event.composedPath().includes(button)) return;
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
      const prefix = '#framework-flow-notification-delivery-';
      const statuses = {}, digests = {};
      for (const stage of ['prepare','probe']) {
        const status = document.querySelector(prefix+stage+'-status');
        statuses[stage] = String(status?.textContent||'');
        digests[stage] = String(status?.getAttribute('data-notification-digest')||'');
      }
      const route = !!document.querySelector(prefix+'route');
      return JSON.stringify({
        windowId:String(context.windowId||''), sessionId:String(context.sessionId||''),
        uiId:String(context.uiId||''), rootRendered:route || !!document.querySelector('#framework-root'),
        firstPaint:d.standardBundleFirstMeaningfulPaintReported===true,
        clients:Object.keys(window.Vaadin?.Flow?.clients||{}).filter(name=>name!=='TypeScript').length,
        routeReady:route && location.pathname.endsWith('/flow-notification-delivery')
          && ['prepare','probe'].every(stage=>document.querySelector(prefix+'invoke-'+stage)),
        statuses,digests,pointers:window.VatoriNotificationDeliveryPointers||{},
        requests:Number(d.standardBundleOpaqueRequestCount||0),
        responses:Number(d.standardBundleOpaqueResponseCount||0),
        pushes:Number(d.standardBundlePushMessageCount||0),
        errorFree:!d.standardBundleBrowserError && !d.standardBundleOpaqueRequestError
          && !d.standardBundleOpaqueResponseError && !d.standardBundlePushError
      });
    })()
    """
}
