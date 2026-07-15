import Cocoa
import Foundation
import WebKit

private enum FlowIpcStandardBundleSidecarRestartPhase {
    case waitingForInitialReady
    case waitingForInitialInteraction
    case waitingForRecovery
    case waitingForRecoveredInteraction
}

private var flowIpcStandardBundleSidecarRestartScheduled = false
private var flowIpcStandardBundleSidecarRestartPhase =
    FlowIpcStandardBundleSidecarRestartPhase.waitingForInitialReady
private var flowIpcStandardBundleSidecarRestartDeadline = Date.distantPast
private var flowIpcStandardBundleInitialWindowIdentifier: ObjectIdentifier?
private var flowIpcStandardBundleInitialWebViewIdentifier: ObjectIdentifier?

extension WebViewHost {
    func scheduleStandardBundleSidecarRestartAcceptance(
        _ context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        guard config.flowIpcStandardBundleEnabled,
              config.flowIpcAutoSidecarRestart,
              !flowIpcStandardBundleSidecarRestartScheduled else {
            return
        }
        flowIpcStandardBundleSidecarRestartScheduled = true
        flowIpcStandardBundleSidecarRestartPhase = .waitingForInitialReady
        flowIpcStandardBundleSidecarRestartDeadline = Date().addingTimeInterval(
            timeoutSeconds > 0 ? timeoutSeconds : 60)
        flowIpcStandardBundleInitialWindowIdentifier = ObjectIdentifier(context.window)
        flowIpcStandardBundleInitialWebViewIdentifier = ObjectIdentifier(context.webView)
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
                guard self.terminateSidecarForRecoveryAcceptance() else {
                    self.finishStandardBundleSidecarRestartAcceptance(
                        context,
                        success: false,
                        reason: "sidecar-failure-injection-failed",
                        state: state)
                    return
                }
                flowIpcStandardBundleSidecarRestartPhase = .waitingForRecovery
                self.retryStandardBundleSidecarRestartAcceptance(context)
            case .waitingForRecovery:
                let process = self.sidecarRecoveryProcessSnapshotForAcceptance()
                let serverUiStateReset = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("Vaadin TextField: pending")
                guard context.recoveryGeneration >= 1,
                      process.replaced,
                      state["ready"] as? Bool == true,
                      serverUiStateReset,
                      self.sidecarRestartInteger(state, "pushFirstSequence") == 1 else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "recovered-runtime-not-ready",
                        state: state)
                    return
                }
                self.dispatchStandardBundleSidecarRestartValue(
                    context.webView,
                    value: "after-restart") { dispatched in
                        guard dispatched else {
                            self.finishStandardBundleSidecarRestartAcceptance(
                                context,
                                success: false,
                                reason: "recovered-interaction-dispatch-failed",
                                state: state)
                            return
                        }
                        flowIpcStandardBundleSidecarRestartPhase = .waitingForRecoveredInteraction
                        self.retryStandardBundleSidecarRestartAcceptance(context)
                    }
            case .waitingForRecoveredInteraction:
                let interactionRecovered = self.sidecarRestartString(state, "textFieldStatus")
                    .contains("after-restart")
                guard interactionRecovered else {
                    self.retryOrFailStandardBundleSidecarRestartAcceptance(
                        context,
                        reason: "recovered-interaction-patch-not-observed",
                        state: state)
                    return
                }
                let process = self.sidecarRecoveryProcessSnapshotForAcceptance()
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
                        "interactionRecovered": true
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
          const textFieldStatus = document.querySelector('#ipc-vaadin-text-field-status')?.textContent || '';
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
              && !!document.querySelector('#ipc-root')
              && !!document.querySelector('#ipc-vaadin-text-field')
              && !error,
            recoveryGeneration: Number(window.VaadinDesktopHostRecoveryGeneration || 0),
            requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
            pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
            pushFirstSequence: Number(diagnostics.standardBundlePushFirstSequence || 0),
            pushStatus,
            textFieldStatus,
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

    private func dispatchStandardBundleSidecarRestartValue(
        _ webView: WKWebView,
        value: String,
        completion: @escaping (Bool) -> Void
    ) {
        let script = """
        (() => {
          const element = document.querySelector('#ipc-vaadin-text-field');
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
        _ context: FlowIpcWindowContext
    ) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
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
