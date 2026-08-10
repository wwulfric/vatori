import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func schedulePushDeliveryAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcPushDeliveryAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcPushDeliveryAcceptance {
    private enum Phase {
        case waitingForBaseline
        case deliveryRequested
    }

    private static let deliveredMarker = "Standard push delivery READY"

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.waitingForBaseline

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
                self.retryOrFail(code: "pushDeliveryInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "pushDeliveryBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .waitingForBaseline:
                guard snapshot.baselineReady else {
                    self.retryOrFail(
                        code: "pushDeliveryBaselineTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.context.webView.evaluateJavaScript(
                    Self.dropAndRequestPushScript
                ) { result, error in
                    guard error == nil, result as? Bool == true else {
                        self.retryOrFail(
                            code: "pushDeliveryFaultInjectionFailed",
                            details: [
                                "message": error?.localizedDescription ?? "",
                                "result": String(describing: result)
                            ])
                        return
                    }
                    self.phase = .deliveryRequested
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                        self.poll()
                    }
                }
            case .deliveryRequested:
                guard snapshot.complete else {
                    self.retryOrFail(
                        code: "pushDeliveryAcknowledgementTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
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
            "baselineAcknowledged": snapshot.baselineAcknowledged,
            "firstDeliveryDropped": snapshot.firstDeliveryDropped,
            "sameSequenceRedelivered": snapshot.sameSequenceRedelivered,
            "flowLastSeenAcknowledged": snapshot.flowLastSeenAcknowledged,
            "terminalDomObserved": snapshot.terminalDomObserved,
            "connectionPreserved": snapshot.connectionPreserved,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowPushDelivery.frameworkAcceptance", payload)
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
        let activeConnectionId: String
        let initialConnectionId: String
        let pushStatus: String
        let ackCount: Int
        let initialAckCount: Int
        let redeliveryCount: Int
        let initialRedeliveryCount: Int
        let lastPushSequence: Int
        let initialPushSequence: Int
        let lastAckSequence: Int
        let lastAckServerSyncId: Int
        let lastDeliveryAttempt: Int
        let requestIssued: Bool
        let dropped: Bool
        let droppedSequence: Int
        let droppedFlowServerSyncId: Int
        let error: String

        var errorFree: Bool {
            error.isEmpty
        }

        var baselineReady: Bool {
            rootRendered
                && firstMeaningfulPaint
                && flowClientCount > 0
                && requestCount >= 2
                && responseCount >= 2
                && pushBridgeInstalled
                && pushConnected
                && !activeConnectionId.isEmpty
                && ackCount >= 2
                && pushStatus.contains("Standard push 2 READY")
        }

        var firstDeliveryDropped: Bool {
            requestIssued
                && dropped
                && droppedSequence > initialPushSequence
                && droppedFlowServerSyncId >= 0
        }

        var baselineAcknowledged: Bool {
            initialAckCount >= 2
                && initialPushSequence >= 2
                && !initialConnectionId.isEmpty
        }

        var sameSequenceRedelivered: Bool {
            firstDeliveryDropped
                && redeliveryCount > initialRedeliveryCount
                && lastDeliveryAttempt > 1
                && lastPushSequence == droppedSequence
        }

        var flowLastSeenAcknowledged: Bool {
            sameSequenceRedelivered
                && ackCount > initialAckCount
                && lastAckSequence == droppedSequence
                && lastAckServerSyncId >= droppedFlowServerSyncId
        }

        var terminalDomObserved: Bool {
            pushStatus == FlowIpcPushDeliveryAcceptance.deliveredMarker
        }

        var connectionPreserved: Bool {
            !initialConnectionId.isEmpty
                && initialConnectionId == activeConnectionId
        }

        var complete: Bool {
            baselineAcknowledged
                && flowLastSeenAcknowledged
                && terminalDomObserved
                && connectionPreserved
                && pushConnected
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
                "activeConnectionId": activeConnectionId,
                "initialConnectionId": initialConnectionId,
                "pushStatus": pushStatus,
                "ackCount": ackCount,
                "initialAckCount": initialAckCount,
                "redeliveryCount": redeliveryCount,
                "initialRedeliveryCount": initialRedeliveryCount,
                "lastPushSequence": lastPushSequence,
                "initialPushSequence": initialPushSequence,
                "lastAckSequence": lastAckSequence,
                "lastAckServerSyncId": lastAckServerSyncId,
                "lastDeliveryAttempt": lastDeliveryAttempt,
                "requestIssued": requestIssued,
                "dropped": dropped,
                "droppedSequence": droppedSequence,
                "droppedFlowServerSyncId": droppedFlowServerSyncId,
                "error": error
            ]
        }
    }

    private static let dropAndRequestPushScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const push = window.VaadinDesktopPush;
      const status = document.querySelector('#ipc-standard-push-status');
      if (!push?.receive || !status?.$server?.triggerPush
          || diagnostics.pushDeliveryAcceptanceInstalled === true) {
        return false;
      }
      const originalReceive = push.receive.bind(push);
      diagnostics.pushDeliveryAcceptanceInstalled = true;
      diagnostics.pushDeliveryAcceptanceDropArmed = true;
      diagnostics.pushDeliveryAcceptanceRequestIssued = true;
      diagnostics.pushDeliveryAcceptanceInitialConnectionId =
        diagnostics.standardBundlePushActiveConnectionId || '';
      diagnostics.pushDeliveryAcceptanceInitialAckCount =
        Number(diagnostics.standardBundlePushAckCount || 0);
      diagnostics.pushDeliveryAcceptanceInitialRedeliveryCount =
        Number(diagnostics.standardBundlePushRedeliveryCount || 0);
      diagnostics.pushDeliveryAcceptanceInitialPushSequence =
        Number(diagnostics.standardBundlePushLastSequence || 0);
      push.receive = (envelope) => {
        const payload = envelope?.payload || {};
        if (diagnostics.pushDeliveryAcceptanceDropArmed === true
            && envelope?.type === 'opaquePush'
            && payload.resync !== true
            && Number(payload.deliveryAttempt || 1) === 1) {
          diagnostics.pushDeliveryAcceptanceDropArmed = false;
          diagnostics.pushDeliveryAcceptanceDropped = true;
          diagnostics.pushDeliveryAcceptanceDroppedSequence =
            Number(payload.pushSequence || 0);
          diagnostics.pushDeliveryAcceptanceDroppedFlowServerSyncId =
            Number(payload.flowServerSyncId ?? -1);
          return true;
        }
        return originalReceive(envelope);
      };
      status.$server.triggerPush('Standard push delivery READY');
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
        activeConnectionId: diagnostics.standardBundlePushActiveConnectionId || '',
        initialConnectionId:
          diagnostics.pushDeliveryAcceptanceInitialConnectionId || '',
        pushStatus:
          document.querySelector('#ipc-standard-push-status')?.textContent || '',
        ackCount: Number(diagnostics.standardBundlePushAckCount || 0),
        initialAckCount:
          Number(diagnostics.pushDeliveryAcceptanceInitialAckCount || 0),
        redeliveryCount:
          Number(diagnostics.standardBundlePushRedeliveryCount || 0),
        initialRedeliveryCount:
          Number(diagnostics.pushDeliveryAcceptanceInitialRedeliveryCount || 0),
        lastPushSequence:
          Number(diagnostics.standardBundlePushLastSequence || 0),
        initialPushSequence:
          Number(diagnostics.pushDeliveryAcceptanceInitialPushSequence || 0),
        lastAckSequence:
          Number(diagnostics.standardBundlePushLastAckSequence || 0),
        lastAckServerSyncId:
          Number(diagnostics.standardBundlePushLastAckServerSyncId ?? -1),
        lastDeliveryAttempt:
          Number(diagnostics.standardBundlePushLastDeliveryAttempt || 0),
        requestIssued:
          diagnostics.pushDeliveryAcceptanceRequestIssued === true,
        dropped: diagnostics.pushDeliveryAcceptanceDropped === true,
        droppedSequence:
          Number(diagnostics.pushDeliveryAcceptanceDroppedSequence || 0),
        droppedFlowServerSyncId:
          Number(diagnostics.pushDeliveryAcceptanceDroppedFlowServerSyncId ?? -1),
        error
      });
    })()
    """
}
