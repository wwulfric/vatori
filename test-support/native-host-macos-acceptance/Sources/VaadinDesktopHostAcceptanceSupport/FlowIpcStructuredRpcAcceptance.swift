import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleStructuredRpcAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcStructuredRpcAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcStructuredRpcAcceptance {
    private enum Phase {
        case ready
        case clientCallable
        case clientCallableTypeShapes
        case pendingJavaScript
        case pendingJavaScriptTypeShapes
        case cancellation
        case sentCancellationReady
        case sentCancellation
        case disabledClientCallable
        case invisibleOwnerQueued
        case invisibleOwnerResolved
        case detachedOwnerQueued
        case detachedOwnerDetached
        case detachedOwnerReattached
        case detachedOwnerConfirmed
        case futureSuccess
        case futureFailure
        case subscriptions
        case lateSubscriptionReady
        case lateSubscription
        case futureCancellation
        case inertModalAdded
        case inertClientCallable
        case inertModalRemoved
        case inertRecovery
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready
    private var clientCallableInputObserved = false
    private var clientCallableResultObserved = false
    private var clientCallableTypeShapeInputObserved = false
    private var clientCallableTypeShapeResultObserved = false
    private var pendingJavaScriptObserved = false
    private var pendingJavaScriptTypeShapesObserved = false
    private var cancellationObserved = false
    private var sentCancellationObserved = false
    private var disabledDefaultBlockedObserved = false
    private var disabledAlwaysAllowedObserved = false
    private var invisibleOwnerDeferredObserved = false
    private var invisibleOwnerResolvedObserved = false
    private var detachedOwnerQueuedObserved = false
    private var detachedOwnerDetachedObserved = false
    private var detachedOwnerReattachedObserved = false
    private var detachedOwnerCleanupObserved = false
    private var futureSuccessObserved = false
    private var futureDeadlockGuardObserved = false
    private var futureFailureObserved = false
    private var multipleSubscriptionsObserved = false
    private var lateSubscriptionSentObserved = false
    private var lateSubscriptionRejectedObserved = false
    private var futureCancellationObserved = false
    private var inertDefaultBlockedObserved = false
    private var inertAllowedObserved = false
    private var inertRecoveryObserved = false

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
                self.retryOrFail(code: "structuredRpcInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "structuredRpcBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.ready else {
                    self.retryOrFail(
                        code: "structuredRpcReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.clientCallableAction,
                    next: .clientCallable,
                    failureCode: "structuredRpcClientCallableDispatchFailed")
            case .clientCallable:
                guard snapshot.clientCallableInputObserved,
                      snapshot.clientCallableResultObserved else {
                    self.retryOrFail(
                        code: "structuredRpcClientCallableTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.clientCallableInputObserved = true
                self.clientCallableResultObserved = true
                self.dispatch(
                    Self.clientCallableTypeShapesAction,
                    next: .clientCallableTypeShapes,
                    failureCode:
                        "structuredRpcClientCallableTypeShapesDispatchFailed")
            case .clientCallableTypeShapes:
                guard snapshot.clientCallableTypeShapeInputObserved,
                      snapshot.clientCallableTypeShapeResultObserved else {
                    self.retryOrFail(
                        code: "structuredRpcClientCallableTypeShapesTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.clientCallableTypeShapeInputObserved = true
                self.clientCallableTypeShapeResultObserved = true
                self.dispatch(
                    Self.pendingJavaScriptAction,
                    next: .pendingJavaScript,
                    failureCode: "structuredRpcPendingJavaScriptDispatchFailed")
            case .pendingJavaScript:
                guard snapshot.pendingJavaScriptObserved else {
                    self.retryOrFail(
                        code: "structuredRpcPendingJavaScriptTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.pendingJavaScriptObserved = true
                self.dispatch(
                    Self.pendingJavaScriptTypeShapesAction,
                    next: .pendingJavaScriptTypeShapes,
                    failureCode:
                        "structuredRpcPendingJavaScriptTypeShapesDispatchFailed")
            case .pendingJavaScriptTypeShapes:
                guard snapshot.pendingJavaScriptTypeShapesObserved else {
                    self.retryOrFail(
                        code: "structuredRpcPendingJavaScriptTypeShapesTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.pendingJavaScriptTypeShapesObserved = true
                self.dispatch(
                    Self.cancellationAction,
                    next: .cancellation,
                    failureCode: "structuredRpcCancellationDispatchFailed")
            case .cancellation:
                guard snapshot.cancellationObserved else {
                    self.retryOrFail(
                        code: "structuredRpcCancellationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.cancellationObserved = true
                self.dispatch(
                    Self.sentCancellationStartAction,
                    next: .sentCancellationReady,
                    failureCode:
                        "structuredRpcSentCancellationStartFailed")
            case .sentCancellationReady:
                guard snapshot.sentExecutionObserved else {
                    self.retryOrFail(
                        code: "structuredRpcSentExecutionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.sentCancellationAction,
                    next: .sentCancellation,
                    failureCode:
                        "structuredRpcSentCancellationDispatchFailed")
            case .sentCancellation:
                guard snapshot.sentCancellationObserved else {
                    self.retryOrFail(
                        code: "structuredRpcSentCancellationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.sentCancellationObserved = true
                self.dispatch(
                    Self.disabledClientCallableAction,
                    next: .disabledClientCallable,
                    failureCode:
                        "structuredRpcDisabledClientCallableDispatchFailed")
            case .disabledClientCallable:
                guard snapshot.disabledDefaultBlockedObserved,
                      snapshot.disabledAlwaysAllowedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcDisabledClientCallableTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.disabledDefaultBlockedObserved = true
                self.disabledAlwaysAllowedObserved = true
                self.dispatch(
                    Self.invisibleOwnerQueueAction,
                    next: .invisibleOwnerQueued,
                    failureCode:
                        "structuredRpcInvisibleOwnerQueueDispatchFailed")
            case .invisibleOwnerQueued:
                guard snapshot.invisibleOwnerDeferredObserved else {
                    self.retryOrFail(
                        code: "structuredRpcInvisibleOwnerDeferralTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.invisibleOwnerDeferredObserved = true
                self.dispatch(
                    Self.invisibleOwnerRevealAction,
                    next: .invisibleOwnerResolved,
                    failureCode:
                        "structuredRpcInvisibleOwnerRevealDispatchFailed")
            case .invisibleOwnerResolved:
                guard snapshot.invisibleOwnerResolvedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcInvisibleOwnerResolutionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.invisibleOwnerResolvedObserved = true
                self.dispatch(
                    Self.detachedOwnerQueueAction,
                    next: .detachedOwnerQueued,
                    failureCode:
                        "structuredRpcDetachedOwnerQueueDispatchFailed")
            case .detachedOwnerQueued:
                guard snapshot.detachedOwnerQueuedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcDetachedOwnerQueueTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.detachedOwnerQueuedObserved = true
                self.dispatch(
                    Self.detachedOwnerDetachAction,
                    next: .detachedOwnerDetached,
                    failureCode:
                        "structuredRpcDetachedOwnerDetachDispatchFailed")
            case .detachedOwnerDetached:
                guard snapshot.detachedOwnerDetachedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcDetachedOwnerDetachTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.detachedOwnerDetachedObserved = true
                self.dispatch(
                    Self.detachedOwnerReattachAction,
                    next: .detachedOwnerReattached,
                    failureCode:
                        "structuredRpcDetachedOwnerReattachDispatchFailed")
            case .detachedOwnerReattached:
                guard snapshot.detachedOwnerReattachedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcDetachedOwnerReattachTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.detachedOwnerReattachedObserved = true
                self.dispatch(
                    Self.detachedOwnerConfirmAction,
                    next: .detachedOwnerConfirmed,
                    failureCode:
                        "structuredRpcDetachedOwnerConfirmDispatchFailed")
            case .detachedOwnerConfirmed:
                guard snapshot.detachedOwnerCleanupObserved else {
                    self.retryOrFail(
                        code: "structuredRpcDetachedOwnerCleanupTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.detachedOwnerCleanupObserved = true
                self.dispatch(
                    Self.futureSuccessAction,
                    next: .futureSuccess,
                    failureCode:
                        "structuredRpcFutureSuccessDispatchFailed")
            case .futureSuccess:
                guard snapshot.futureSuccessObserved,
                      snapshot.futureDeadlockGuardObserved else {
                    self.retryOrFail(
                        code: "structuredRpcFutureSuccessTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.futureSuccessObserved = true
                self.futureDeadlockGuardObserved = true
                self.dispatch(
                    Self.futureFailureAction,
                    next: .futureFailure,
                    failureCode:
                        "structuredRpcFutureFailureDispatchFailed")
            case .futureFailure:
                guard snapshot.futureFailureObserved else {
                    self.retryOrFail(
                        code: "structuredRpcFutureFailureTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.futureFailureObserved = true
                self.dispatch(
                    Self.subscriptionsAction,
                    next: .subscriptions,
                    failureCode:
                        "structuredRpcSubscriptionsDispatchFailed")
            case .subscriptions:
                guard snapshot.multipleSubscriptionsObserved else {
                    self.retryOrFail(
                        code: "structuredRpcSubscriptionsTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.multipleSubscriptionsObserved = true
                self.dispatch(
                    Self.lateSubscriptionStartAction,
                    next: .lateSubscriptionReady,
                    failureCode:
                        "structuredRpcLateSubscriptionStartFailed")
            case .lateSubscriptionReady:
                guard snapshot.lateSubscriptionSentObserved else {
                    self.retryOrFail(
                        code: "structuredRpcLateSubscriptionReadyTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.lateSubscriptionSentObserved = true
                self.dispatch(
                    Self.lateSubscriptionAttemptAction,
                    next: .lateSubscription,
                    failureCode:
                        "structuredRpcLateSubscriptionAttemptFailed")
            case .lateSubscription:
                guard snapshot.lateSubscriptionRejectedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcLateSubscriptionTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.lateSubscriptionRejectedObserved = true
                self.dispatch(
                    Self.futureCancellationAction,
                    next: .futureCancellation,
                    failureCode:
                        "structuredRpcFutureCancellationDispatchFailed")
            case .futureCancellation:
                guard snapshot.futureCancellationObserved else {
                    self.retryOrFail(
                        code: "structuredRpcFutureCancellationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.futureCancellationObserved = true
                self.dispatch(
                    Self.inertModalAddAction,
                    next: .inertModalAdded,
                    failureCode:
                        "structuredRpcInertModalAddDispatchFailed")
            case .inertModalAdded:
                guard snapshot.inertModalPresent else {
                    self.retryOrFail(
                        code: "structuredRpcInertModalAddTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.inertClientCallableAction,
                    next: .inertClientCallable,
                    failureCode:
                        "structuredRpcInertClientCallableDispatchFailed")
            case .inertClientCallable:
                guard snapshot.inertDefaultBlockedObserved,
                      snapshot.inertAllowedObserved else {
                    self.retryOrFail(
                        code: "structuredRpcInertClientCallableTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.inertDefaultBlockedObserved = true
                self.inertAllowedObserved = true
                self.dispatch(
                    Self.inertModalRemoveAction,
                    next: .inertModalRemoved,
                    failureCode:
                        "structuredRpcInertModalRemoveDispatchFailed")
            case .inertModalRemoved:
                guard !snapshot.inertModalPresent else {
                    self.retryOrFail(
                        code: "structuredRpcInertModalRemoveTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.inertRecoveryAction,
                    next: .inertRecovery,
                    failureCode:
                        "structuredRpcInertRecoveryDispatchFailed")
            case .inertRecovery:
                guard snapshot.inertRecoveryObserved else {
                    self.retryOrFail(
                        code: "structuredRpcInertRecoveryTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.inertRecoveryObserved = true
                self.finish(snapshot)
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(Snapshot.self, from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func dispatch(
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
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                self.poll()
            }
        }
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        if Date() >= deadline {
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
            "complete": snapshot.errorFree
                && clientCallableInputObserved
                && clientCallableResultObserved
                && clientCallableTypeShapeInputObserved
                && clientCallableTypeShapeResultObserved
                && pendingJavaScriptObserved
                && pendingJavaScriptTypeShapesObserved
                && cancellationObserved
                && sentCancellationObserved
                && disabledDefaultBlockedObserved
                && disabledAlwaysAllowedObserved
                && invisibleOwnerDeferredObserved
                && invisibleOwnerResolvedObserved
                && detachedOwnerQueuedObserved
                && detachedOwnerDetachedObserved
                && detachedOwnerReattachedObserved
                && detachedOwnerCleanupObserved
                && !snapshot.detachedOwnerSideEffectExecuted
                && futureSuccessObserved
                && futureDeadlockGuardObserved
                && futureFailureObserved
                && multipleSubscriptionsObserved
                && lateSubscriptionSentObserved
                && lateSubscriptionRejectedObserved
                && futureCancellationObserved
                && !snapshot.futureCanceledSideEffectExecuted
                && inertDefaultBlockedObserved
                && inertAllowedObserved
                && inertRecoveryObserved
                && !snapshot.inertDefaultSettled,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 2,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 2,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "errorFree": snapshot.errorFree,
            "structuredRpcClientCallableInputObserved":
                clientCallableInputObserved,
            "structuredRpcClientCallableResultObserved":
                clientCallableResultObserved,
            "structuredRpcClientCallableTypeShapeInputObserved":
                clientCallableTypeShapeInputObserved,
            "structuredRpcClientCallableTypeShapeResultObserved":
                clientCallableTypeShapeResultObserved,
            "structuredRpcPendingJavaScriptObserved":
                pendingJavaScriptObserved,
            "structuredRpcPendingJavaScriptTypeShapesObserved":
                pendingJavaScriptTypeShapesObserved,
            "structuredRpcCancellationObserved": cancellationObserved,
            "structuredRpcSentCancellationObserved":
                sentCancellationObserved,
            "structuredRpcDisabledDefaultBlockedObserved":
                disabledDefaultBlockedObserved,
            "structuredRpcDisabledAlwaysAllowedObserved":
                disabledAlwaysAllowedObserved,
            "structuredRpcInvisibleOwnerDeferredObserved":
                invisibleOwnerDeferredObserved,
            "structuredRpcInvisibleOwnerResolvedObserved":
                invisibleOwnerResolvedObserved,
            "structuredRpcInvisibleOwnerSideEffectExecuted":
                snapshot.invisibleOwnerSideEffectExecuted,
            "structuredRpcDetachedOwnerQueuedObserved":
                detachedOwnerQueuedObserved,
            "structuredRpcDetachedOwnerDetachedObserved":
                detachedOwnerDetachedObserved,
            "structuredRpcDetachedOwnerReattachedObserved":
                detachedOwnerReattachedObserved,
            "structuredRpcDetachedOwnerCleanupObserved":
                detachedOwnerCleanupObserved,
            "structuredRpcDetachedOwnerSideEffectExecuted":
                snapshot.detachedOwnerSideEffectExecuted,
            "structuredRpcFutureSuccessObserved":
                futureSuccessObserved,
            "structuredRpcFutureDeadlockGuardObserved":
                futureDeadlockGuardObserved,
            "structuredRpcFutureSuccessSideEffectExecuted":
                snapshot.futureSuccessSideEffectExecuted,
            "structuredRpcFutureFailureObserved":
                futureFailureObserved,
            "structuredRpcMultipleSubscriptionsObserved":
                multipleSubscriptionsObserved,
            "structuredRpcLateSubscriptionSentObserved":
                lateSubscriptionSentObserved,
            "structuredRpcLateSubscriptionRejectedObserved":
                lateSubscriptionRejectedObserved,
            "structuredRpcFutureCancellationObserved":
                futureCancellationObserved,
            "structuredRpcFutureCanceledSideEffectExecuted":
                snapshot.futureCanceledSideEffectExecuted,
            "structuredRpcInertDefaultBlockedObserved":
                inertDefaultBlockedObserved,
            "structuredRpcInertAllowedObserved":
                inertAllowedObserved,
            "structuredRpcInertRecoveryObserved":
                inertRecoveryObserved,
            "structuredRpcInertDefaultSettled":
                snapshot.inertDefaultSettled
        ]) { _, replacement in replacement }
        trace("recv", "structuredRpc.frameworkAcceptance", payload)
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
        let probePresent: Bool
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let clientCallableInputObserved: Bool
        let clientCallableResultObserved: Bool
        let clientCallableTypeShapeInputObserved: Bool
        let clientCallableTypeShapeResultObserved: Bool
        let pendingJavaScriptObserved: Bool
        let pendingJavaScriptTypeShapesObserved: Bool
        let cancellationObserved: Bool
        let canceledSideEffectExecuted: Bool
        let sentExecutionObserved: Bool
        let sentCancellationObserved: Bool
        let disabledDefaultBlockedObserved: Bool
        let disabledAlwaysAllowedObserved: Bool
        let invisibleOwnerDeferredObserved: Bool
        let invisibleOwnerResolvedObserved: Bool
        let invisibleOwnerSideEffectExecuted: Bool
        let detachedOwnerQueuedObserved: Bool
        let detachedOwnerDetachedObserved: Bool
        let detachedOwnerReattachedObserved: Bool
        let detachedOwnerCleanupObserved: Bool
        let detachedOwnerSideEffectExecuted: Bool
        let futureSuccessObserved: Bool
        let futureDeadlockGuardObserved: Bool
        let futureSuccessSideEffectExecuted: Bool
        let futureFailureObserved: Bool
        let multipleSubscriptionsObserved: Bool
        let lateSubscriptionSentObserved: Bool
        let lateSubscriptionRejectedObserved: Bool
        let futureCancellationObserved: Bool
        let futureCanceledSideEffectExecuted: Bool
        let inertModalPresent: Bool
        let inertDefaultBlockedObserved: Bool
        let inertAllowedObserved: Bool
        let inertRecoveryObserved: Bool
        let inertDefaultSettled: Bool
        let clientCallableStatus: String
        let clientCallablePayload: String
        let clientCallableTypeShapesStatus: String
        let clientCallableTypeShapesPayload: String
        let pendingJavaScriptStatus: String
        let pendingJavaScriptTypeShapesStatus: String
        let cancellationStatus: String
        let sentCancellationStatus: String
        let disabledClientCallableStatus: String
        let invisibleOwnerStatus: String
        let detachedOwnerStatus: String
        let futureSuccessStatus: String
        let futureFailureStatus: String
        let subscriptionsStatus: String
        let lateSubscriptionStatus: String
        let futureCancellationStatus: String
        let inertClientCallableStatus: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
        }

        var ready: Bool {
            rootRendered
                && probePresent
                && firstMeaningfulPaint
                && requestCount >= 2
                && responseCount >= 2
                && flowClientCount > 0
                && errorFree
        }

        var tracePayload: [String: Any] {
            [
                "rootRendered": rootRendered,
                "probePresent": probePresent,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "structuredRpcClientCallableInputObserved":
                    clientCallableInputObserved,
                "structuredRpcClientCallableResultObserved":
                    clientCallableResultObserved,
                "structuredRpcClientCallableTypeShapeInputObserved":
                    clientCallableTypeShapeInputObserved,
                "structuredRpcClientCallableTypeShapeResultObserved":
                    clientCallableTypeShapeResultObserved,
                "structuredRpcPendingJavaScriptObserved":
                    pendingJavaScriptObserved,
                "structuredRpcPendingJavaScriptTypeShapesObserved":
                    pendingJavaScriptTypeShapesObserved,
                "structuredRpcCancellationObserved": cancellationObserved,
                "structuredRpcCanceledSideEffectExecuted":
                    canceledSideEffectExecuted,
                "structuredRpcSentExecutionObserved":
                    sentExecutionObserved,
                "structuredRpcSentCancellationObserved":
                    sentCancellationObserved,
                "structuredRpcDisabledDefaultBlockedObserved":
                    disabledDefaultBlockedObserved,
                "structuredRpcDisabledAlwaysAllowedObserved":
                    disabledAlwaysAllowedObserved,
                "structuredRpcInvisibleOwnerDeferredObserved":
                    invisibleOwnerDeferredObserved,
                "structuredRpcInvisibleOwnerResolvedObserved":
                    invisibleOwnerResolvedObserved,
                "structuredRpcInvisibleOwnerSideEffectExecuted":
                    invisibleOwnerSideEffectExecuted,
                "structuredRpcDetachedOwnerQueuedObserved":
                    detachedOwnerQueuedObserved,
                "structuredRpcDetachedOwnerDetachedObserved":
                    detachedOwnerDetachedObserved,
                "structuredRpcDetachedOwnerReattachedObserved":
                    detachedOwnerReattachedObserved,
                "structuredRpcDetachedOwnerCleanupObserved":
                    detachedOwnerCleanupObserved,
                "structuredRpcDetachedOwnerSideEffectExecuted":
                    detachedOwnerSideEffectExecuted,
                "structuredRpcFutureSuccessObserved":
                    futureSuccessObserved,
                "structuredRpcFutureDeadlockGuardObserved":
                    futureDeadlockGuardObserved,
                "structuredRpcFutureSuccessSideEffectExecuted":
                    futureSuccessSideEffectExecuted,
                "structuredRpcFutureFailureObserved":
                    futureFailureObserved,
                "structuredRpcMultipleSubscriptionsObserved":
                    multipleSubscriptionsObserved,
                "structuredRpcLateSubscriptionSentObserved":
                    lateSubscriptionSentObserved,
                "structuredRpcLateSubscriptionRejectedObserved":
                    lateSubscriptionRejectedObserved,
                "structuredRpcFutureCancellationObserved":
                    futureCancellationObserved,
                "structuredRpcFutureCanceledSideEffectExecuted":
                    futureCanceledSideEffectExecuted,
                "structuredRpcInertModalPresent": inertModalPresent,
                "structuredRpcInertDefaultBlockedObserved":
                    inertDefaultBlockedObserved,
                "structuredRpcInertAllowedObserved":
                    inertAllowedObserved,
                "structuredRpcInertRecoveryObserved":
                    inertRecoveryObserved,
                "structuredRpcInertDefaultSettled":
                    inertDefaultSettled,
                "structuredRpcClientCallableStatus": clientCallableStatus,
                "structuredRpcClientCallablePayload": clientCallablePayload,
                "structuredRpcClientCallableTypeShapesStatus":
                    clientCallableTypeShapesStatus,
                "structuredRpcClientCallableTypeShapesPayload":
                    clientCallableTypeShapesPayload,
                "structuredRpcPendingJavaScriptStatus":
                    pendingJavaScriptStatus,
                "structuredRpcPendingJavaScriptTypeShapesStatus":
                    pendingJavaScriptTypeShapesStatus,
                "structuredRpcCancellationStatus": cancellationStatus,
                "structuredRpcSentCancellationStatus":
                    sentCancellationStatus,
                "structuredRpcDisabledClientCallableStatus":
                    disabledClientCallableStatus,
                "structuredRpcInvisibleOwnerStatus":
                    invisibleOwnerStatus,
                "structuredRpcDetachedOwnerStatus":
                    detachedOwnerStatus,
                "structuredRpcFutureSuccessStatus":
                    futureSuccessStatus,
                "structuredRpcFutureFailureStatus":
                    futureFailureStatus,
                "structuredRpcSubscriptionsStatus":
                    subscriptionsStatus,
                "structuredRpcLateSubscriptionStatus":
                    lateSubscriptionStatus,
                "structuredRpcFutureCancellationStatus":
                    futureCancellationStatus,
                "structuredRpcInertClientCallableStatus":
                    inertClientCallableStatus,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const probe = document.querySelector('#framework-flow-rpc-probe');
      const clientCallableStatus =
        document.querySelector('#framework-client-callable-structured-status')
          ?.textContent || '';
      const clientCallableTypeShapesStatus =
        document.querySelector(
          '#framework-client-callable-type-shapes-status')
          ?.textContent || '';
      const pendingJavaScriptStatus =
        document.querySelector('#framework-pending-javascript-structured-status')
          ?.textContent || '';
      const pendingJavaScriptTypeShapesStatus =
        document.querySelector(
          '#framework-pending-javascript-type-shapes-status')
          ?.textContent || '';
      const cancellationStatus =
        document.querySelector('#framework-pending-javascript-cancellation-status')
          ?.textContent || '';
      const sentCancellationStatus =
        document.querySelector(
          '#framework-pending-javascript-sent-cancellation-status')
          ?.textContent || '';
      const disabledClientCallableStatus =
        document.querySelector('#framework-disabled-client-callable-status')
          ?.textContent || '';
      const invisibleOwnerStatus =
        document.querySelector(
          '#framework-pending-javascript-invisible-owner-status')
          ?.textContent || '';
      const detachedOwnerStatus =
        document.querySelector(
          '#framework-pending-javascript-detached-owner-status')
          ?.textContent || '';
      const futureSuccessStatus =
        document.querySelector(
          '#framework-pending-javascript-future-success-status')
          ?.textContent || '';
      const futureFailureStatus =
        document.querySelector(
          '#framework-pending-javascript-future-failure-status')
          ?.textContent || '';
      const subscriptionsStatus =
        document.querySelector(
          '#framework-pending-javascript-subscription-status')
          ?.textContent || '';
      const lateSubscriptionStatus =
        document.querySelector(
          '#framework-pending-javascript-late-subscription-status')
          ?.textContent || '';
      const futureCancellationStatus =
        document.querySelector(
          '#framework-pending-javascript-future-cancellation-status')
          ?.textContent || '';
      const inertClientCallableProbe =
        document.querySelector('#framework-inert-client-callable-probe');
      const inertClientCallableModal =
        document.querySelector('#framework-inert-client-callable-modal');
      const inertClientCallableStatus =
        document.querySelector('#framework-inert-client-callable-status')
          ?.textContent || '';
      const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients
        ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
        : [];
      const canceledSideEffectExecuted =
        window.VaadinDesktopCanceledRpcExecuted === true;
      const sentExecutionObserved =
        window.VaadinDesktopSentRpcExecuted === true;
      const invisibleOwnerSideEffectExecuted =
        window.VaadinDesktopInvisibleOwnerRpcExecuted === true;
      const detachedOwnerSideEffectExecuted =
        window.VaadinDesktopDetachedOwnerRpcExecuted === true;
      const futureSuccessSideEffectExecuted =
        window.VaadinDesktopFutureRpcExecuted === true;
      const lateSubscriptionSideEffectExecuted =
        window.VaadinDesktopLateSubscriptionRpcExecuted === true;
      const futureCanceledSideEffectExecuted =
        window.VaadinDesktopFutureCanceledRpcExecuted === true;
      const disabledModesObserved =
        disabledClientCallableStatus.includes('default=0')
          && disabledClientCallableStatus.includes('always=1');
      const inertDefaultSettled =
        inertClientCallableProbe?.dataset?.inertDefaultSettled === 'true';
      const inertAllowedObserved =
        inertClientCallableProbe?.dataset?.inertAllowedResolved
          === 'inert-allowed:while-inert:1'
          && inertClientCallableStatus.includes('default=0')
          && inertClientCallableStatus.includes('allowed=1')
          && inertClientCallableStatus.includes(
            'last=inert-allowed:while-inert:1');
      const inertRecoveryObserved =
        inertClientCallableProbe?.dataset?.inertRecoveryResolved
          === 'inert-default:after-close:1'
          && inertClientCallableStatus.includes('default=1')
          && inertClientCallableStatus.includes('allowed=1')
          && inertClientCallableStatus.includes(
            'last=inert-default:after-close:1');
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root'),
        probePresent: !!probe,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        clientCallableInputObserved:
          clientCallableStatus.includes(
            'ClientCallable structured: request=browser-structured')
            && clientCallableStatus.includes('active=true')
            && clientCallableStatus.includes('items=alpha:2,beta:3')
            && clientCallableStatus.includes('total=5'),
        clientCallableResultObserved:
          probe?.dataset?.clientCallableStructuredResolved === 'true'
            && !probe?.dataset?.clientCallableStructuredError,
        clientCallableTypeShapeInputObserved:
          clientCallableTypeShapesStatus.includes('number=264')
            && clientCallableTypeShapesStatus.includes('enum=BETA')
            && clientCallableTypeShapesStatus.includes(
              'samples=[2, 4, 6]')
            && clientCallableTypeShapesStatus.includes('items=gamma:5')
            && clientCallableTypeShapesStatus.includes(
              'primitiveDefault=0')
            && clientCallableTypeShapesStatus.includes('nullable=null')
            && clientCallableTypeShapesStatus.includes(
              'labels=[left, right]'),
        clientCallableTypeShapeResultObserved:
          probe?.dataset?.clientCallableTypeShapesResolved === 'true'
            && !probe?.dataset?.clientCallableTypeShapesError,
        pendingJavaScriptObserved:
          pendingJavaScriptStatus.includes(
            'Pending JavaScript structured: alpha-client:2,beta-client:4 total=6'),
        pendingJavaScriptTypeShapesObserved:
          pendingJavaScriptTypeShapesStatus.includes('callbacks=5')
            && pendingJavaScriptTypeShapesStatus.includes('number=42')
            && pendingJavaScriptTypeShapesStatus.includes('enum=BETA')
            && pendingJavaScriptTypeShapesStatus.includes(
              'array=[3, 5, 8]')
            && pendingJavaScriptTypeShapesStatus.includes('nullable=null')
            && pendingJavaScriptTypeShapesStatus.includes(
              'nested=[13, 21]')
            && pendingJavaScriptTypeShapesStatus.includes('error=none'),
        cancellationObserved:
          cancellationStatus.includes(
            'Pending JavaScript cancellation: canceled=true')
            && cancellationStatus.includes('sentBefore=false')
            && cancellationStatus.includes('error=Execution canceled')
            && !canceledSideEffectExecuted,
        canceledSideEffectExecuted,
        sentExecutionObserved,
        sentCancellationObserved:
          sentExecutionObserved
            && sentCancellationStatus.includes(
              'Pending JavaScript sent cancellation: sent=true')
            && sentCancellationStatus.includes('canceled=false')
            && sentCancellationStatus.includes('result=sent-result'),
        disabledDefaultBlockedObserved: disabledModesObserved,
        disabledAlwaysAllowedObserved: disabledModesObserved,
        invisibleOwnerDeferredObserved:
          invisibleOwnerStatus.includes(
            'Pending JavaScript invisible owner: queued')
            && !invisibleOwnerSideEffectExecuted,
        invisibleOwnerResolvedObserved:
          invisibleOwnerStatus.includes(
            'Pending JavaScript invisible owner: resolved=invisible-owner-result')
            && invisibleOwnerSideEffectExecuted,
        invisibleOwnerSideEffectExecuted,
        detachedOwnerQueuedObserved:
          detachedOwnerStatus.includes(
            'Pending JavaScript detached owner: queued')
            && !detachedOwnerSideEffectExecuted,
        detachedOwnerDetachedObserved:
          detachedOwnerStatus.includes(
            'Pending JavaScript detached owner: detached')
            && !detachedOwnerSideEffectExecuted,
        detachedOwnerReattachedObserved:
          detachedOwnerStatus.includes(
            'Pending JavaScript detached owner: reattached')
            && !detachedOwnerSideEffectExecuted,
        detachedOwnerCleanupObserved:
          detachedOwnerStatus.includes(
            'Pending JavaScript detached owner: confirmed')
            && !detachedOwnerSideEffectExecuted,
        detachedOwnerSideEffectExecuted,
        futureSuccessObserved:
          futureSuccessSideEffectExecuted
            && futureSuccessStatus.includes('initialDone=false')
            && futureSuccessStatus.includes('deadlockGuard=true')
            && futureSuccessStatus.includes('guardError=none')
            && futureSuccessStatus.includes(
              'items=future-alpha:7,future-beta:9')
            && futureSuccessStatus.includes('total=16'),
        futureDeadlockGuardObserved:
          futureSuccessStatus.includes('initialDone=false')
            && futureSuccessStatus.includes('deadlockGuard=true')
            && futureSuccessStatus.includes('guardError=none'),
        futureSuccessSideEffectExecuted,
        futureFailureObserved:
          futureFailureStatus.includes(
            'Pending JavaScript future failure: type=JavaScriptException')
            && futureFailureStatus.includes('future-rejected'),
        multipleSubscriptionsObserved:
          subscriptionsStatus.includes(
            'Pending JavaScript subscriptions: callbacks=2')
            && subscriptionsStatus.includes('first=subscription-result')
            && subscriptionsStatus.includes('second=subscription-result')
            && subscriptionsStatus.includes('future=subscription-result')
            && subscriptionsStatus.includes('error=none'),
        lateSubscriptionSentObserved:
          lateSubscriptionSideEffectExecuted
            && lateSubscriptionStatus.includes(
              'Pending JavaScript late subscription: started'),
        lateSubscriptionRejectedObserved:
          lateSubscriptionSideEffectExecuted
            && lateSubscriptionStatus.includes('sent=true')
            && lateSubscriptionStatus.includes('rejected=true')
            && lateSubscriptionStatus.includes('lateCallbacks=0')
            && lateSubscriptionStatus.includes('result=late-result')
            && lateSubscriptionStatus.includes(
              'Cannot subscribe to the return value after the execution has been sent to the client.'),
        futureCancellationObserved:
          futureCancellationStatus.includes('canceled=true')
            && futureCancellationStatus.includes('done=true')
            && futureCancellationStatus.includes('exceptional=true')
            && futureCancellationStatus.includes(
              'type=JavaScriptException')
            && futureCancellationStatus.includes(
              'message=Execution canceled')
            && !futureCanceledSideEffectExecuted,
        futureCanceledSideEffectExecuted,
        inertModalPresent: inertClientCallableModal?.isConnected === true,
        inertDefaultBlockedObserved:
          inertAllowedObserved && !inertDefaultSettled,
        inertAllowedObserved,
        inertRecoveryObserved:
          inertRecoveryObserved && !inertDefaultSettled,
        inertDefaultSettled,
        clientCallableStatus,
        clientCallablePayload:
          probe?.dataset?.clientCallableStructuredPayload || '',
        clientCallableTypeShapesStatus,
        clientCallableTypeShapesPayload:
          probe?.dataset?.clientCallableTypeShapesPayload || '',
        pendingJavaScriptStatus,
        pendingJavaScriptTypeShapesStatus,
        cancellationStatus,
        sentCancellationStatus,
        disabledClientCallableStatus,
        invisibleOwnerStatus,
        detachedOwnerStatus,
        futureSuccessStatus,
        futureFailureStatus,
        subscriptionsStatus,
        lateSubscriptionStatus,
        futureCancellationStatus,
        inertClientCallableStatus,
        browserError: diagnostics.standardBundleBrowserError || '',
        requestError: diagnostics.standardBundleOpaqueRequestError || '',
        responseError: diagnostics.standardBundleOpaqueResponseError || '',
        pushError: diagnostics.standardBundlePushError || ''
      });
    })()
    """

    private static let clientCallableAction = """
    (() => {
      const probe = document.querySelector('#framework-flow-rpc-probe');
      if (!probe || typeof probe.$server?.roundTripStructured !== 'function') {
        return false;
      }
      delete probe.dataset.clientCallableStructuredResolved;
      delete probe.dataset.clientCallableStructuredPayload;
      delete probe.dataset.clientCallableStructuredError;
      const result = probe.$server.roundTripStructured({
        requestId: 'browser-structured',
        active: true,
        items: [
          { code: 'alpha', quantity: 2 },
          { code: 'beta', quantity: 3 }
        ]
      });
      if (!result || typeof result.then !== 'function') return false;
      result.then(
        (value) => {
          const valid = value?.requestId === 'server-browser-structured'
            && value?.accepted === true
            && value?.totalQuantity === 5
            && value?.items?.length === 2
            && value.items[0]?.code === 'alpha'
            && value.items[0]?.quantity === 2
            && value.items[1]?.code === 'beta'
            && value.items[1]?.quantity === 3
            && value?.quantities?.alpha === 2
            && value?.quantities?.beta === 3;
          probe.dataset.clientCallableStructuredResolved = String(valid);
          probe.dataset.clientCallableStructuredPayload = JSON.stringify(value);
        },
        (error) => {
          probe.dataset.clientCallableStructuredError = String(error);
        }
      );
      return true;
    })()
    """

    private static let pendingJavaScriptAction = """
    (() => {
      const button =
        document.querySelector('#framework-pending-javascript-structured');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let clientCallableTypeShapesAction = """
    (() => {
      const probe = document.querySelector('#framework-flow-rpc-probe');
      if (!probe || typeof probe.$server?.roundTripTypeShapes !== 'function') {
        return false;
      }
      delete probe.dataset.clientCallableTypeShapesResolved;
      delete probe.dataset.clientCallableTypeShapesPayload;
      delete probe.dataset.clientCallableTypeShapesError;
      const result = probe.$server.roundTripTypeShapes(
        '264',
        'BETA',
        [2, 4, 6],
        [{ code: 'gamma', quantity: 5 }],
        null,
        null,
        'left',
        'right'
      );
      if (!result || typeof result.then !== 'function') return false;
      result.then(
        (value) => {
          const valid = value?.number === 265
            && value?.kind === 'ALPHA'
            && Array.isArray(value?.samples)
            && value.samples.join(',') === '3,5,7'
            && value?.items?.length === 1
            && value.items[0]?.code === 'gamma'
            && value.items[0]?.quantity === 5
            && value?.primitiveDefault === 0
            && value?.nullableValue === null
            && Array.isArray(value?.labels)
            && value.labels.join(',') === 'left,right';
          probe.dataset.clientCallableTypeShapesResolved = String(valid);
          probe.dataset.clientCallableTypeShapesPayload =
            JSON.stringify(value);
        },
        (error) => {
          probe.dataset.clientCallableTypeShapesError = String(error);
        }
      );
      return true;
    })()
    """

    private static let pendingJavaScriptTypeShapesAction = """
    (() => {
      const button =
        document.querySelector('#framework-pending-javascript-type-shapes');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let cancellationAction = """
    (() => {
      const button =
        document.querySelector('#framework-pending-javascript-cancellation');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopCanceledRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let sentCancellationStartAction = """
    (() => {
      const button =
        document.querySelector('#framework-pending-javascript-sent-start');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopSentRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let sentCancellationAction = """
    (() => {
      const button =
        document.querySelector('#framework-pending-javascript-sent-cancel');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let disabledClientCallableAction = """
    (() => {
      const probe =
        document.querySelector('#framework-disabled-client-callable-probe');
      if (!probe
          || typeof probe.$server?.invokeWhileDisabledDefault !== 'function'
          || typeof probe.$server?.invokeWhileDisabledAlways !== 'function') {
        return false;
      }
      probe.$server.invokeWhileDisabledDefault();
      probe.$server.invokeWhileDisabledAlways();
      return true;
    })()
    """

    private static let invisibleOwnerQueueAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-invisible-owner-queue');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopInvisibleOwnerRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let invisibleOwnerRevealAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-invisible-owner-reveal');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let detachedOwnerQueueAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-detached-owner-queue');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopDetachedOwnerRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let detachedOwnerDetachAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-detached-owner-detach');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let detachedOwnerReattachAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-detached-owner-reattach');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let detachedOwnerConfirmAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-detached-owner-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let futureSuccessAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-future-success');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopFutureRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let futureFailureAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-future-failure');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let subscriptionsAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-subscriptions');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let lateSubscriptionStartAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-late-subscription-start');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopLateSubscriptionRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let lateSubscriptionAttemptAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-late-subscription-attempt');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let futureCancellationAction = """
    (() => {
      const button = document.querySelector(
        '#framework-pending-javascript-future-cancellation');
      if (!button || typeof button.click !== 'function') return false;
      window.VaadinDesktopFutureCanceledRpcExecuted = false;
      button.click();
      return true;
    })()
    """

    private static let inertModalAddAction = """
    (() => {
      const button = document.querySelector(
        '#framework-inert-client-callable-modal-open');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let inertClientCallableAction = """
    (() => {
      const probe =
        document.querySelector('#framework-inert-client-callable-probe');
      if (!probe
          || typeof probe.$server?.invokeDefault !== 'function'
          || typeof probe.$server?.invokeAllowed !== 'function') {
        return false;
      }
      delete probe.dataset.inertDefaultSettled;
      delete probe.dataset.inertDefaultError;
      delete probe.dataset.inertAllowedResolved;
      delete probe.dataset.inertAllowedError;
      delete probe.dataset.inertRecoveryResolved;
      delete probe.dataset.inertRecoveryError;
      const blocked = probe.$server.invokeDefault('while-inert');
      const allowed = probe.$server.invokeAllowed('while-inert');
      if (!blocked || typeof blocked.then !== 'function'
          || !allowed || typeof allowed.then !== 'function') {
        return false;
      }
      blocked.then(
        (value) => {
          probe.dataset.inertDefaultSettled = 'true';
          probe.dataset.inertDefaultError =
            `unexpected-value:${String(value)}`;
        },
        (error) => {
          probe.dataset.inertDefaultSettled = 'true';
          probe.dataset.inertDefaultError =
            `unexpected-error:${String(error)}`;
        }
      );
      allowed.then(
        (value) => {
          probe.dataset.inertAllowedResolved = String(value);
        },
        (error) => {
          probe.dataset.inertAllowedError = String(error);
        }
      );
      return true;
    })()
    """

    private static let inertModalRemoveAction = """
    (() => {
      const button = document.querySelector(
        '#framework-inert-client-callable-modal-close');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let inertRecoveryAction = """
    (() => {
      const probe =
        document.querySelector('#framework-inert-client-callable-probe');
      if (!probe || typeof probe.$server?.invokeDefault !== 'function') {
        return false;
      }
      const result = probe.$server.invokeDefault('after-close');
      if (!result || typeof result.then !== 'function') return false;
      result.then(
        (value) => {
          probe.dataset.inertRecoveryResolved = String(value);
        },
        (error) => {
          probe.dataset.inertRecoveryError = String(error);
        }
      );
      return true;
    })()
    """
}
