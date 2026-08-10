import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func schedulePushRecoveryAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcPushRecoveryAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcPushRecoveryAcceptance {
    private enum Phase {
        case waitingForInitialPush
        case recoveryDispatched
        case postRecoveryPushRequested
    }

    private static let recoveredMarker = "Standard push recovery READY"

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.waitingForInitialPush

    init(context: FlowIpcWindowContext, deadline: Date) {
        self.context = context
        self.deadline = deadline
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "pushRecoveryInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "pushRecoveryBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .waitingForInitialPush:
                guard snapshot.initialPushReady else {
                    self.retryOrFail(
                        code: "pushRecoveryInitialPushTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.dispatchRecoveryScript,
                    next: .recoveryDispatched,
                    failureCode: "pushRecoveryDispatchFailed")
            case .recoveryDispatched:
                guard snapshot.recoveryReady else {
                    self.retryOrFail(
                        code: "pushRecoveryResyncTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.perform(
                    Self.requestPostRecoveryPushScript,
                    next: .postRecoveryPushRequested,
                    failureCode: "pushRecoveryPostPushRequestFailed")
            case .postRecoveryPushRequested:
                guard snapshot.complete else {
                    self.retryOrFail(
                        code: "pushRecoveryPostPushTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func perform(
        _ script: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = next
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                self.poll()
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(
                    Snapshot.self,
                    from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.poll()
        }
    }

    private func finish(_ snapshot: Snapshot) {
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": snapshot.complete,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 2,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 2,
            "pushBridgeInstalled": snapshot.pushBridgeInstalled,
            "initialPushObserved": snapshot.initialPushReady,
            "reconnectObserved": snapshot.reconnectObserved,
            "gapDetected": snapshot.gapDetected,
            "resyncObserved": snapshot.resyncObserved,
            "connectionPreserved": snapshot.connectionPreserved,
            "postResyncPushObserved": snapshot.postResyncPushObserved,
            "postRecoveryDomObserved": snapshot.postRecoveryDomObserved,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowPushRecovery.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Snapshot: Decodable {
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let flowClientCount: Int
        let requestCount: Int
        let responseCount: Int
        let pushBridgeInstalled: Bool
        let pushConnected: Bool
        let pushMessageCount: Int
        let pushReconnectCount: Int
        let pushResyncCount: Int
        let pushGapCount: Int
        let activeConnectionId: String
        let initialConnectionId: String
        let initialMessageCount: Int
        let initialReconnectCount: Int
        let initialResyncCount: Int
        let initialPushSequence: Int
        let resyncMessageCount: Int
        let resyncPushSequence: Int
        let lastPushSequence: Int
        let postRecoveryPushRequested: Bool
        let pushStatus: String
        let error: String

        var errorFree: Bool {
            error.isEmpty
        }

        var connectionPreserved: Bool {
            !initialConnectionId.isEmpty
                && initialConnectionId == activeConnectionId
        }

        var initialPushReady: Bool {
            rootRendered
                && firstMeaningfulPaint
                && flowClientCount > 0
                && pushBridgeInstalled
                && pushConnected
                && !activeConnectionId.isEmpty
                && pushMessageCount >= 1
                && lastPushSequence >= 1
                && !pushStatus.contains("pending")
        }

        var reconnectObserved: Bool {
            pushReconnectCount > initialReconnectCount
        }

        var gapDetected: Bool {
            pushGapCount >= 1
        }

        var resyncObserved: Bool {
            pushResyncCount > initialResyncCount
                && pushMessageCount > initialMessageCount
                && lastPushSequence > initialPushSequence
        }

        var recoveryReady: Bool {
            reconnectObserved
                && gapDetected
                && resyncObserved
                && connectionPreserved
                && pushConnected
        }

        var postRecoveryDomObserved: Bool {
            pushStatus == FlowIpcPushRecoveryAcceptance.recoveredMarker
        }

        var postResyncPushObserved: Bool {
            postRecoveryPushRequested
                && postRecoveryDomObserved
                && pushMessageCount > resyncMessageCount
                && lastPushSequence > resyncPushSequence
        }

        var complete: Bool {
            recoveryReady && postResyncPushObserved
        }

        var tracePayload: [String: Any] {
            [
                "rootRendered": rootRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "flowClientCount": flowClientCount,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushBridgeInstalled": pushBridgeInstalled,
                "pushConnected": pushConnected,
                "pushMessageCount": pushMessageCount,
                "pushReconnectCount": pushReconnectCount,
                "pushResyncCount": pushResyncCount,
                "pushGapCount": pushGapCount,
                "activeConnectionId": activeConnectionId,
                "initialConnectionId": initialConnectionId,
                "initialMessageCount": initialMessageCount,
                "initialReconnectCount": initialReconnectCount,
                "initialResyncCount": initialResyncCount,
                "initialPushSequence": initialPushSequence,
                "resyncMessageCount": resyncMessageCount,
                "resyncPushSequence": resyncPushSequence,
                "lastPushSequence": lastPushSequence,
                "postRecoveryPushRequested": postRecoveryPushRequested,
                "pushStatus": pushStatus,
                "error": error
            ]
        }
    }

    private static let dispatchRecoveryScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const push = window.VaadinDesktopPush;
      const connectionId = diagnostics.standardBundlePushActiveConnectionId || '';
      const lastSequence = Number(diagnostics.standardBundlePushLastSequence || 0);
      if (!push?.reconnect || !push?.receive || !connectionId || lastSequence < 1) {
        return false;
      }
      diagnostics.pushRecoveryAcceptanceInitialConnectionId = connectionId;
      diagnostics.pushRecoveryAcceptanceInitialMessageCount =
        Number(diagnostics.standardBundlePushMessageCount || 0);
      diagnostics.pushRecoveryAcceptanceInitialReconnectCount =
        Number(diagnostics.standardBundlePushReconnectCount || 0);
      diagnostics.pushRecoveryAcceptanceInitialResyncCount =
        Number(diagnostics.standardBundlePushResyncCount || 0);
      diagnostics.pushRecoveryAcceptanceInitialPushSequence = lastSequence;
      push.reconnect();
      push.receive({
        version: 1,
        type: 'opaquePush',
        payload: {
          connectionId,
          pushSequence: lastSequence + 2,
          uidl: '',
          resync: false
        }
      });
      return true;
    })()
    """

    private static let requestPostRecoveryPushScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const status = document.querySelector('#ipc-standard-push-status');
      if (!status?.$server?.triggerPush) {
        return false;
      }
      diagnostics.pushRecoveryAcceptanceResyncMessageCount =
        Number(diagnostics.standardBundlePushMessageCount || 0);
      diagnostics.pushRecoveryAcceptanceResyncPushSequence =
        Number(diagnostics.standardBundlePushLastSequence || 0);
      diagnostics.pushRecoveryAcceptancePostPushRequested = true;
      status.$server.triggerPush('Standard push recovery READY');
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
        : [];
      const browserError = diagnostics.standardBundleBrowserError || '';
      const hiddenViewTransitionSkipped =
        document.visibilityState === 'hidden'
          && browserError.includes('View transition was skipped')
          && browserError.includes('visibility state is hidden');
      const error = diagnostics.standardBundleHistoryError
        || (hiddenViewTransitionSkipped ? '' : browserError)
        || diagnostics.standardBundleOpaqueRequestError
        || diagnostics.standardBundleOpaqueResponseError
        || diagnostics.standardBundlePushError
        || diagnostics.standardBundlePushProtocolError
        || '';
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root'),
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        flowClientCount: clients.length,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushBridgeInstalled: diagnostics.standardBundlePushBridgeInstalled === true,
        pushConnected: diagnostics.standardBundlePushConnected === true,
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        pushReconnectCount: Number(diagnostics.standardBundlePushReconnectCount || 0),
        pushResyncCount: Number(diagnostics.standardBundlePushResyncCount || 0),
        pushGapCount: Number(diagnostics.standardBundlePushGapCount || 0),
        activeConnectionId: diagnostics.standardBundlePushActiveConnectionId || '',
        initialConnectionId:
          diagnostics.pushRecoveryAcceptanceInitialConnectionId || '',
        initialMessageCount:
          Number(diagnostics.pushRecoveryAcceptanceInitialMessageCount || 0),
        initialReconnectCount:
          Number(diagnostics.pushRecoveryAcceptanceInitialReconnectCount || 0),
        initialResyncCount:
          Number(diagnostics.pushRecoveryAcceptanceInitialResyncCount || 0),
        initialPushSequence:
          Number(diagnostics.pushRecoveryAcceptanceInitialPushSequence || 0),
        resyncMessageCount:
          Number(diagnostics.pushRecoveryAcceptanceResyncMessageCount || 0),
        resyncPushSequence:
          Number(diagnostics.pushRecoveryAcceptanceResyncPushSequence || 0),
        lastPushSequence: Number(diagnostics.standardBundlePushLastSequence || 0),
        postRecoveryPushRequested:
          diagnostics.pushRecoveryAcceptancePostPushRequested === true,
        pushStatus:
          document.querySelector('#ipc-standard-push-status')?.textContent || '',
        error
      });
    })()
    """
}
