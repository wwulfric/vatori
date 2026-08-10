import Cocoa
import Darwin
import Foundation
import WebKit

private enum FlowIpcStandardBundleSidecarRestartPhase {
    case waitingForInitialReady
    case waitingForInitialInteraction
    case waitingForUnacknowledgedPush
    case waitingForFirstRecovery
    case waitingForFirstRecoveryNonReplay
    case waitingForFirstRecoveredInteraction
    case waitingForSecondRecovery
    case waitingForSecondRecoveredInteraction
    case waitingForExhaustion
    case waitingForStableBudget
    case waitingForPostStableRecovery
    case waitingForFinalRecoveredInteraction
}

private var flowIpcStandardBundleSidecarRestartScheduled = false
private var flowIpcStandardBundleSidecarRestartPhase =
    FlowIpcStandardBundleSidecarRestartPhase.waitingForInitialReady
private var flowIpcStandardBundleSidecarRestartDeadline = Date.distantPast
private var flowIpcStandardBundleInitialWindowIdentifier: ObjectIdentifier?
private var flowIpcStandardBundleInitialWebViewIdentifier: ObjectIdentifier?
private var flowIpcStandardBundleInitialProcessIdentifier: Int32?
private var flowIpcStandardBundleFailedProcessIdentifier: Int32?
private var flowIpcStandardBundleObservedProcessIdentifiers: [Int32] = []
private var flowIpcStandardBundleStableBudgetDeadline = Date.distantPast
private var flowIpcStandardBundleNonReplayDeadline = Date.distantPast
private var flowIpcStandardBundleUnacknowledgedPushSequence = 0
private var flowIpcStandardBundleUnacknowledgedPushFlowServerSyncId = -1
private var flowIpcStandardBundleUnacknowledgedPushInitialAckCount = 0
private var flowIpcStandardBundleUnacknowledgedPushUnacked = false
private var flowIpcStandardBundleStalePushNotReplayed = false
private var flowIpcStandardBundleFreshPushSequenceAfterReplacement = false

private let flowIpcStandardBundleStalePushMarker =
    "Standard push stale generation MUST NOT REPLAY"

extension WebViewHost {
    func scheduleStandardBundleSidecarRestartAcceptance(
        _ context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        guard config.transport == .flowIpc,
              config.flowIpcAutoSidecarRestart
                || config.flowIpcAutoSidecarRecoveryExhaustion,
              !flowIpcStandardBundleSidecarRestartScheduled else {
            return
        }
        flowIpcStandardBundleSidecarRestartScheduled = true
        flowIpcStandardBundleSidecarRestartPhase = .waitingForInitialReady
        flowIpcStandardBundleSidecarRestartDeadline = Date().addingTimeInterval(
            timeoutSeconds > 0 ? timeoutSeconds : 60)
        flowIpcStandardBundleInitialWindowIdentifier = ObjectIdentifier(context.window)
        flowIpcStandardBundleInitialWebViewIdentifier = ObjectIdentifier(context.webView)
        flowIpcStandardBundleInitialProcessIdentifier = sidecarProcessIdentifier
        flowIpcStandardBundleFailedProcessIdentifier = nil
        flowIpcStandardBundleObservedProcessIdentifiers =
            sidecarProcessIdentifier.map { [$0] } ?? []
        flowIpcStandardBundleStableBudgetDeadline = Date.distantPast
        flowIpcStandardBundleNonReplayDeadline = Date.distantPast
        flowIpcStandardBundleUnacknowledgedPushSequence = 0
        flowIpcStandardBundleUnacknowledgedPushFlowServerSyncId = -1
        flowIpcStandardBundleUnacknowledgedPushInitialAckCount = 0
        flowIpcStandardBundleUnacknowledgedPushUnacked = false
        flowIpcStandardBundleStalePushNotReplayed = false
        flowIpcStandardBundleFreshPushSequenceAfterReplacement = false
        pollStandardBundleSidecarRestartAcceptance(context)
    }

    private func pollStandardBundleSidecarRestartAcceptance(
        _ context: FlowIpcWindowContext
    ) {
        evaluateStandardBundleSidecarRestartState(context) { state in
            guard let state else {
                self.retryOrFailStandardBundleSidecarRestartAcceptance(
                    context,
                    reason: "state-inspection-failed")
                return
            }
            if !self.sidecarRestartString(state, "error").isEmpty {
                self.finishStandardBundleSidecarRestartAcceptance(
                    context,
                    success: false,
                    reason: "browser-runtime-error",
                    state: state)
                return
            }

            switch flowIpcStandardBundleSidecarRestartPhase {
            case .waitingForInitialReady:
                guard state["ready"] as? Bool == true,
                      context.recoveryGeneration == 0 else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "initial-runtime-not-ready",
                        state: state)
                    return
                }
                self.dispatchStandardBundleSidecarRestartValue(
                    context.webView,
                    value: "before-restart") { dispatched in
                        guard dispatched else {
                            self.finishStandardBundleSidecarRestartAcceptance(
                                context,
                                success: false,
                                reason: "initial-interaction-dispatch-failed",
                                state: state)
                            return
                        }
                        flowIpcStandardBundleSidecarRestartPhase = .waitingForInitialInteraction
                        self.retryStandardBundleSidecarRestartAcceptance(context)
                    }
            case .waitingForInitialInteraction:
                guard self.sidecarRestartString(state, "textFieldStatus")
                    .contains("before-restart") else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "initial-interaction-patch-not-observed",
                        state: state)
                    return
                }
                self.armStandardBundleUnacknowledgedPush(context.webView) { armed in
                    guard armed else {
                        self.finishStandardBundleSidecarRestartAcceptance(
                            context,
                            success: false,
                            reason: "unacknowledged-push-injection-failed",
                            state: state)
                        return
                    }
                    flowIpcStandardBundleSidecarRestartPhase =
                        .waitingForUnacknowledgedPush
                    self.retryStandardBundleSidecarRestartAcceptance(
                        context,
                        delaySeconds: 0.1)
                }
            case .waitingForUnacknowledgedPush:
                let initialAckCount = self.sidecarRestartInteger(
                    state, "unacknowledgedPushInitialAckCount")
                let ackCount = self.sidecarRestartInteger(state, "pushAckCount")
                let sequence = self.sidecarRestartInteger(
                    state, "unacknowledgedPushSequence")
                let flowServerSyncId = self.sidecarRestartInteger(
                    state, "unacknowledgedPushFlowServerSyncId")
                let deliveryAttempt = self.sidecarRestartInteger(
                    state, "unacknowledgedPushDeliveryAttempt")
                guard state["unacknowledgedPushDropped"] as? Bool == true,
                      sequence > self.sidecarRestartInteger(state, "pushLastSequence"),
                      flowServerSyncId >= 0,
                      deliveryAttempt >= 1,
                      initialAckCount >= 2,
                      ackCount == initialAckCount else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "unacknowledged-push-not-observed",
                        state: state)
                    return
                }
                flowIpcStandardBundleUnacknowledgedPushSequence = sequence
                flowIpcStandardBundleUnacknowledgedPushFlowServerSyncId =
                    flowServerSyncId
                flowIpcStandardBundleUnacknowledgedPushInitialAckCount =
                    initialAckCount
                flowIpcStandardBundleUnacknowledgedPushUnacked = true
                var boundary = context.scope.tracePayload
                boundary.merge([
                    "observed": true,
                    "unacknowledged": true,
                    "processIdentifier": self.sidecarProcessIdentifier ?? 0,
                    "recoveryGeneration": context.recoveryGeneration,
                    "connectionId": self.sidecarRestartString(
                        state, "unacknowledgedPushConnectionId"),
                    "flowUiId": self.sidecarRestartInteger(
                        state, "unacknowledgedPushFlowUiId"),
                    "pushSequence": sequence,
                    "flowServerSyncId": flowServerSyncId,
                    "deliveryAttempt": deliveryAttempt,
                    "initialAckCount": initialAckCount,
                    "ackCount": ackCount
                ]) { _, replacement in replacement }
                trace(
                    "recv",
                    "standardBundle.sidecarUnacknowledgedPushBoundary",
                    boundary)
                guard self.failStandardBundleSidecarForAcceptance(
                    reason: "unacknowledged-push-replacement"
                ) else {
                    self.finishStandardBundleSidecarRestartAcceptance(
                        context,
                        success: false,
                        reason: "sidecar-failure-injection-failed",
                        state: state)
                    return
                }
                flowIpcStandardBundleSidecarRestartPhase = .waitingForFirstRecovery
                self.retryStandardBundleSidecarRestartAcceptance(context)
            case .waitingForFirstRecovery:
                let process = self.sidecarAcceptanceProcessSnapshot(
                    previous: flowIpcStandardBundleFailedProcessIdentifier)
                let serverUiStateReset = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("TextField: value=Initial title")
                guard context.recoveryGeneration >= 1,
                      process.replaced,
                      state["ready"] as? Bool == true,
                      serverUiStateReset,
                      self.sidecarRestartInteger(state, "pushMessageCount") == 2,
                      self.sidecarRestartInteger(state, "pushAckCount") == 2,
                      self.sidecarRestartInteger(state, "pushFirstSequence") == 1,
                      self.sidecarRestartInteger(state, "pushLastSequence") == 2,
                      state["stalePushMarkerPresent"] as? Bool == false else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "recovered-runtime-not-ready",
                        state: state)
                    return
                }
                flowIpcStandardBundleNonReplayDeadline =
                    Date().addingTimeInterval(1.25)
                flowIpcStandardBundleSidecarRestartPhase =
                    .waitingForFirstRecoveryNonReplay
                self.retryStandardBundleSidecarRestartAcceptance(context)
            case .waitingForFirstRecoveryNonReplay:
                guard Date() >= flowIpcStandardBundleNonReplayDeadline else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "stale-push-non-replay-observation-pending",
                        state: state)
                    return
                }
                guard state["ready"] as? Bool == true,
                      self.sidecarRestartInteger(state, "pushMessageCount") == 2,
                      self.sidecarRestartInteger(state, "pushAckCount") == 2,
                      self.sidecarRestartInteger(state, "pushFirstSequence") == 1,
                      self.sidecarRestartInteger(state, "pushLastSequence") == 2,
                      state["stalePushMarkerPresent"] as? Bool == false else {
                    self.finishStandardBundleSidecarRestartAcceptance(
                        context,
                        success: false,
                        reason: "stale-push-replayed-after-replacement",
                        state: state)
                    return
                }
                flowIpcStandardBundleStalePushNotReplayed = true
                flowIpcStandardBundleFreshPushSequenceAfterReplacement = true
                self.dispatchStandardBundleSidecarRestartValue(
                    context.webView,
                    value: "after-restart-1") { dispatched in
                        guard dispatched else {
                            self.finishStandardBundleSidecarRestartAcceptance(
                                context,
                                success: false,
                                reason: "recovered-interaction-dispatch-failed",
                                state: state)
                            return
                        }
                        self.recordStandardBundleSidecarProcess()
                        flowIpcStandardBundleSidecarRestartPhase =
                            .waitingForFirstRecoveredInteraction
                        self.retryStandardBundleSidecarRestartAcceptance(context)
                    }
            case .waitingForFirstRecoveredInteraction:
                let interactionRecovered = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("after-restart-1")
                guard interactionRecovered else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "recovered-interaction-patch-not-observed",
                        state: state)
                    return
                }
                guard self.failStandardBundleSidecarForAcceptance(
                    reason: "rapid-failure-2"
                ) else {
                    self.finishStandardBundleSidecarRestartAcceptance(
                        context,
                        success: false,
                        reason: "second-sidecar-failure-injection-failed",
                        state: state)
                    return
                }
                flowIpcStandardBundleSidecarRestartPhase = .waitingForSecondRecovery
                self.retryStandardBundleSidecarRestartAcceptance(context)
            case .waitingForSecondRecovery:
                let process = self.sidecarAcceptanceProcessSnapshot(
                    previous: flowIpcStandardBundleFailedProcessIdentifier)
                let serverUiStateReset = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("TextField: value=Initial title")
                guard context.recoveryGeneration >= 2,
                      process.replaced,
                      state["ready"] as? Bool == true,
                      serverUiStateReset,
                      self.sidecarRestartInteger(state, "pushFirstSequence") == 1 else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "second-recovered-runtime-not-ready",
                        state: state)
                    return
                }
                self.dispatchStandardBundleSidecarRestartValue(
                    context.webView,
                    value: "after-restart-2") { dispatched in
                        guard dispatched else {
                            self.finishStandardBundleSidecarRestartAcceptance(
                                context,
                                success: false,
                                reason: "second-recovered-interaction-dispatch-failed",
                                state: state)
                            return
                        }
                        self.recordStandardBundleSidecarProcess()
                        flowIpcStandardBundleSidecarRestartPhase =
                            .waitingForSecondRecoveredInteraction
                        self.retryStandardBundleSidecarRestartAcceptance(context)
                    }
            case .waitingForSecondRecoveredInteraction:
                guard self.sidecarRestartString(state, "textFieldStatus")
                    .contains("after-restart-2") else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "second-recovered-interaction-patch-not-observed",
                        state: state)
                    return
                }
                if self.config.flowIpcAutoSidecarRecoveryExhaustion {
                    let process = self.sidecarAcceptanceProcessSnapshot(
                        previous: flowIpcStandardBundleInitialProcessIdentifier)
                    trace(
                        "recv",
                        "standardBundle.sidecarRecoveryExhaustionAcceptance",
                        [
                            "armed": true,
                            "rapidFailuresRecovered":
                                context.recoveryGeneration >= 2,
                            "recoveryGeneration": context.recoveryGeneration,
                            "processRestarted": process.replaced,
                            "windowPreserved":
                                flowIpcStandardBundleInitialWindowIdentifier
                                    == ObjectIdentifier(context.window),
                            "webViewPreserved":
                                flowIpcStandardBundleInitialWebViewIdentifier
                                    == ObjectIdentifier(context.webView)
                        ])
                    flowIpcStandardBundleSidecarRestartPhase =
                        .waitingForExhaustion
                    guard self.failStandardBundleSidecarForAcceptance(
                        reason: "recovery-budget-exhaustion"
                    ) else {
                        trace(
                            "send",
                            "standardBundle.sidecarRecoveryExhaustionAcceptance",
                            [
                                "armed": false,
                                "reason": "failure-injection-failed"
                            ])
                        context.window.close()
                        return
                    }
                    return
                }
                flowIpcStandardBundleStableBudgetDeadline =
                    Date().addingTimeInterval(
                        self.config.flowIpcSidecarRecoveryStableRunSeconds + 0.75)
                flowIpcStandardBundleSidecarRestartPhase = .waitingForStableBudget
                self.retryStandardBundleSidecarRestartAcceptance(context)
            case .waitingForExhaustion:
                self.retryOrFailStandardBundleSidecarRestartAcceptance(
                    context,
                    reason: "host-did-not-exit-after-recovery-exhaustion",
                    state: state)
            case .waitingForStableBudget:
                guard Date() >= flowIpcStandardBundleStableBudgetDeadline else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "stable-budget-deadline-not-reached",
                        state: state)
                    return
                }
                guard self.failStandardBundleSidecarForAcceptance(
                    reason: "post-stability-failure"
                ) else {
                    self.finishStandardBundleSidecarRestartAcceptance(
                        context,
                        success: false,
                        reason: "post-stability-failure-injection-failed",
                        state: state)
                    return
                }
                flowIpcStandardBundleSidecarRestartPhase =
                    .waitingForPostStableRecovery
                self.retryStandardBundleSidecarRestartAcceptance(context)
            case .waitingForPostStableRecovery:
                let process = self.sidecarAcceptanceProcessSnapshot(
                    previous: flowIpcStandardBundleFailedProcessIdentifier)
                let serverUiStateReset = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("TextField: value=Initial title")
                guard context.recoveryGeneration >= 3,
                      process.replaced,
                      state["ready"] as? Bool == true,
                      serverUiStateReset,
                      self.sidecarRestartInteger(state, "pushFirstSequence") == 1 else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "post-stability-runtime-not-ready",
                        state: state)
                    return
                }
                self.dispatchStandardBundleSidecarRestartValue(
                    context.webView,
                    value: "after-stable-restart") { dispatched in
                        guard dispatched else {
                            self.finishStandardBundleSidecarRestartAcceptance(
                                context,
                                success: false,
                                reason: "final-recovered-interaction-dispatch-failed",
                                state: state)
                            return
                        }
                        self.recordStandardBundleSidecarProcess()
                        flowIpcStandardBundleSidecarRestartPhase =
                            .waitingForFinalRecoveredInteraction
                        self.retryStandardBundleSidecarRestartAcceptance(context)
                    }
            case .waitingForFinalRecoveredInteraction:
                let interactionRecovered = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("after-stable-restart")
                guard interactionRecovered else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "final-recovered-interaction-patch-not-observed",
                        state: state)
                    return
                }
                let process = self.sidecarAcceptanceProcessSnapshot(
                    previous: flowIpcStandardBundleInitialProcessIdentifier)
                self.finishStandardBundleSidecarRestartAcceptance(
                    context,
                    success: true,
                    reason: "complete",
                    state: state,
                    additionalDetails: [
                        "processRestarted": process.replaced,
                        "previousProcessIdentifier": process.previous,
                        "processIdentifier": process.current,
                        "windowPreserved": flowIpcStandardBundleInitialWindowIdentifier
                            == ObjectIdentifier(context.window),
                        "webViewPreserved": flowIpcStandardBundleInitialWebViewIdentifier
                            == ObjectIdentifier(context.webView),
                        "webViewReloaded": context.recoveryGeneration >= 1,
                        "serverUiStateReset": true,
                        "requestBridgeRecovered": self.sidecarRestartInteger(state, "requestCount") >= 2
                            && self.sidecarRestartInteger(state, "responseCount") >= 2,
                        "pushRecovered": self.sidecarRestartInteger(state, "pushMessageCount") >= 2
                            && self.sidecarRestartInteger(state, "pushFirstSequence") == 1,
                        "unacknowledgedPushObservedBeforeReplacement":
                            flowIpcStandardBundleUnacknowledgedPushSequence > 0,
                        "unacknowledgedPushUnackedBeforeReplacement":
                            flowIpcStandardBundleUnacknowledgedPushUnacked,
                        "stalePushNotReplayed":
                            flowIpcStandardBundleStalePushNotReplayed,
                        "freshPushSequenceAfterReplacement":
                            flowIpcStandardBundleFreshPushSequenceAfterReplacement,
                        "unacknowledgedPushSequence":
                            flowIpcStandardBundleUnacknowledgedPushSequence,
                        "unacknowledgedPushFlowServerSyncId":
                            flowIpcStandardBundleUnacknowledgedPushFlowServerSyncId,
                        "firstRecoveredPushMessageCount": 2,
                        "firstRecoveredPushAckCount": 2,
                        "firstRecoveredPushFirstSequence": 1,
                        "firstRecoveredPushLastSequence": 2,
                        "interactionRecovered": true,
                        "rapidFailuresRecovered": context.recoveryGeneration >= 2,
                        "stableBudgetResetExercised": context.recoveryGeneration >= 3,
                        "processReplacementCount": max(
                            flowIpcStandardBundleObservedProcessIdentifiers.count - 1,
                            0)
                    ])
            }
        }
    }

    private func evaluateStandardBundleSidecarRestartState(
        _ context: FlowIpcWindowContext,
        completion: @escaping ([String: Any]?) -> Void
    ) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const clients = window.Vaadin?.Flow?.clients
            ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
            : [];
          const pushStatus = document.querySelector('#ipc-standard-push-status')?.textContent || '';
          const textFieldStatus =
            document.querySelector('#framework-text-field-status')?.textContent || '';
          const error = diagnostics.standardBundleBrowserError
            || diagnostics.standardBundleOpaqueRequestError
            || diagnostics.standardBundleOpaqueResponseError
            || diagnostics.standardBundlePushError
            || '';
          return JSON.stringify({
            ready: diagnostics.standardBundleFirstMeaningfulPaintReported === true
              && diagnostics.standardBundlePushBridgeInstalled === true
              && diagnostics.standardBundlePushConnected === true
              && Number(diagnostics.standardBundlePushMessageCount || 0) >= 2
              && pushStatus.includes('Standard push 2 READY')
              && Number(diagnostics.standardBundleOpaqueRequestCount || 0) >= 2
              && Number(diagnostics.standardBundleOpaqueResponseCount || 0) >= 2
              && clients.length > 0
              && !!document.querySelector('#framework-root')
              && !!document.querySelector('#framework-case-title')
              && !error,
            recoveryGeneration: Number(window.VaadinDesktopHostRecoveryGeneration || 0),
            requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
            pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
            pushFirstSequence: Number(diagnostics.standardBundlePushFirstSequence || 0),
            pushLastSequence: Number(diagnostics.standardBundlePushLastSequence || 0),
            pushAckCount: Number(diagnostics.standardBundlePushAckCount || 0),
            pushStatus,
            textFieldStatus,
            stalePushMarkerPresent:
              (document.body?.textContent || '').includes(
                '\(flowIpcStandardBundleStalePushMarker)'),
            unacknowledgedPushDropped:
              diagnostics.sidecarRestartUnacknowledgedPushDropped === true,
            unacknowledgedPushSequence:
              Number(diagnostics.sidecarRestartUnacknowledgedPushSequence || 0),
            unacknowledgedPushFlowServerSyncId:
              Number(diagnostics.sidecarRestartUnacknowledgedPushFlowServerSyncId ?? -1),
            unacknowledgedPushDeliveryAttempt:
              Number(diagnostics.sidecarRestartUnacknowledgedPushDeliveryAttempt || 0),
            unacknowledgedPushInitialAckCount:
              Number(diagnostics.sidecarRestartUnacknowledgedPushInitialAckCount || 0),
            unacknowledgedPushConnectionId:
              diagnostics.sidecarRestartUnacknowledgedPushConnectionId || '',
            unacknowledgedPushFlowUiId:
              Number(diagnostics.sidecarRestartUnacknowledgedPushFlowUiId ?? -1),
            error
          });
        })()
        """
        context.webView.evaluateJavaScript(script) { result, _ in
            guard let text = result as? String,
                  let data = text.data(using: .utf8) else {
                completion(nil)
                return
            }
            completion(try? JSONSerialization.jsonObject(with: data) as? [String: Any])
        }
    }

    private func armStandardBundleUnacknowledgedPush(
        _ webView: WKWebView,
        completion: @escaping (Bool) -> Void
    ) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const push = window.VaadinDesktopPush;
          const status = document.querySelector('#ipc-standard-push-status');
          if (!push?.receive || !status?.$server?.triggerPush
              || diagnostics.sidecarRestartUnacknowledgedPushInstalled === true) {
            return false;
          }
          const originalReceive = push.receive.bind(push);
          diagnostics.sidecarRestartUnacknowledgedPushInstalled = true;
          diagnostics.sidecarRestartUnacknowledgedPushDropArmed = true;
          diagnostics.sidecarRestartUnacknowledgedPushInitialAckCount =
            Number(diagnostics.standardBundlePushAckCount || 0);
          push.receive = (envelope) => {
            const payload = envelope?.payload || {};
            const sequence = Number(payload.pushSequence || 0);
            const droppedSequence = Number(
              diagnostics.sidecarRestartUnacknowledgedPushSequence || 0);
            const shouldDropFirst =
              diagnostics.sidecarRestartUnacknowledgedPushDropArmed === true
                && envelope?.type === 'opaquePush'
                && payload.resync !== true;
            const shouldKeepUnacknowledged =
              diagnostics.sidecarRestartUnacknowledgedPushDropped === true
                && envelope?.type === 'opaquePush'
                && sequence === droppedSequence;
            if (shouldDropFirst || shouldKeepUnacknowledged) {
              diagnostics.sidecarRestartUnacknowledgedPushDropArmed = false;
              diagnostics.sidecarRestartUnacknowledgedPushDropped = true;
              diagnostics.sidecarRestartUnacknowledgedPushSequence = sequence;
              diagnostics.sidecarRestartUnacknowledgedPushFlowServerSyncId =
                Number(payload.flowServerSyncId ?? -1);
              diagnostics.sidecarRestartUnacknowledgedPushDeliveryAttempt =
                Math.max(
                  Number(diagnostics.sidecarRestartUnacknowledgedPushDeliveryAttempt || 0),
                  Number(payload.deliveryAttempt || 1));
              diagnostics.sidecarRestartUnacknowledgedPushConnectionId =
                payload.connectionId || '';
              diagnostics.sidecarRestartUnacknowledgedPushFlowUiId =
                Number(payload.flowUiId ?? -1);
              return true;
            }
            return originalReceive(envelope);
          };
          status.$server.triggerPush('\(flowIpcStandardBundleStalePushMarker)');
          return true;
        })()
        """
        webView.evaluateJavaScript(script) { result, error in
            completion(error == nil && result as? Bool == true)
        }
    }

    private func dispatchStandardBundleSidecarRestartValue(
        _ webView: WKWebView,
        value: String,
        completion: @escaping (Bool) -> Void
    ) {
        let script = """
        (() => {
          const element = document.querySelector('#framework-case-title');
          if (!element) return false;
          element.value = '\(value)';
          if (element.inputElement) element.inputElement.value = '\(value)';
          element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
          return true;
        })()
        """
        webView.evaluateJavaScript(script) { result, _ in
            completion(result as? Bool == true)
        }
    }

    private func sidecarAcceptanceProcessSnapshot(
        previous: Int32?
    ) -> (
        previous: Int32,
        current: Int32,
        replaced: Bool
    ) {
        let previous = previous ?? 0
        let current = sidecarProcessIdentifier ?? 0
        return (previous, current, previous > 0 && current > 0 && previous != current)
    }

    private func recordStandardBundleSidecarProcess() {
        guard let processIdentifier = sidecarProcessIdentifier,
              !flowIpcStandardBundleObservedProcessIdentifiers.contains(
                processIdentifier) else {
            return
        }
        flowIpcStandardBundleObservedProcessIdentifiers.append(processIdentifier)
    }

    private func failStandardBundleSidecarForAcceptance(
        reason: String
    ) -> Bool {
        guard let processIdentifier = sidecarProcessIdentifier,
              Darwin.kill(processIdentifier, SIGKILL) == 0 else {
            return false
        }
        flowIpcStandardBundleFailedProcessIdentifier = processIdentifier
        trace("send", "acceptance.sidecarFailureInjected", [
            "processIdentifier": processIdentifier,
            "reason": reason,
            "signal": SIGKILL
        ])
        return true
    }

    private func retryOrFailStandardBundleSidecarRestartAcceptance(
        _ context: FlowIpcWindowContext,
        reason: String,
        state: [String: Any] = [:]
    ) {
        if Date() >= flowIpcStandardBundleSidecarRestartDeadline {
            finishStandardBundleSidecarRestartAcceptance(
                context,
                success: false,
                reason: reason,
                state: state)
        } else {
            retryStandardBundleSidecarRestartAcceptance(context)
        }
    }

    private func retryStandardBundleSidecarRestartAcceptance(
        _ context: FlowIpcWindowContext,
        delaySeconds: Double = 0.5
    ) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySeconds) {
            self.pollStandardBundleSidecarRestartAcceptance(context)
        }
    }

    private func finishStandardBundleSidecarRestartAcceptance(
        _ context: FlowIpcWindowContext,
        success: Bool,
        reason: String,
        state: [String: Any] = [:],
        additionalDetails: [String: Any] = [:]
    ) {
        var payload: [String: Any] = [
            "complete": success,
            "reason": reason,
            "standardBundleMode": true,
            "recoveryGeneration": context.recoveryGeneration,
            "errorFree": sidecarRestartString(state, "error").isEmpty,
            "requestCount": sidecarRestartInteger(state, "requestCount"),
            "responseCount": sidecarRestartInteger(state, "responseCount"),
            "pushMessageCount": sidecarRestartInteger(state, "pushMessageCount"),
            "recoveredPushFirstSequence": sidecarRestartInteger(state, "pushFirstSequence"),
            "recoveredPushLastSequence": sidecarRestartInteger(state, "pushLastSequence"),
            "recoveredPushAckCount": sidecarRestartInteger(state, "pushAckCount"),
            "textFieldStatus": sidecarRestartString(state, "textFieldStatus")
        ]
        payload.merge(additionalDetails) { _, replacement in replacement }
        trace(success ? "recv" : "send", "standardBundle.sidecarRestartAcceptance", payload)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            trace("recv", "window.closeRequested", [
                "label": context.scope.windowId,
                "reason": success
                    ? "standard-bundle-sidecar-restart-acceptance-complete"
                    : "standard-bundle-sidecar-restart-acceptance-failed"
            ])
            context.window.close()
        }
    }

    private func sidecarRestartString(_ state: [String: Any], _ key: String) -> String {
        state[key] as? String ?? ""
    }

    private func sidecarRestartInteger(_ state: [String: Any], _ key: String) -> Int {
        (state[key] as? NSNumber)?.intValue ?? state[key] as? Int ?? 0
    }
}
