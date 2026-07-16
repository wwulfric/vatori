import Foundation
import WebKit

private enum FlowIpcMultiWindowPhase {
    case waitingForReady
    case waitingForMainMutation
    case waitingForSecondaryMutation
}

private var flowIpcMultiWindowScheduled = false
private var flowIpcMultiWindowPhase = FlowIpcMultiWindowPhase.waitingForReady
private var flowIpcMultiWindowDeadline = Date.distantPast

extension WebViewHost {
    func scheduleMultiWindowAcceptanceIfReady() {
        guard config.flowIpcAutoMultiWindow,
              config.transport == .flowIpc,
              let main = flowIpcWindowContext("main"),
              let secondary = flowIpcWindowContext("secondary"),
              main.navigationFinished,
              secondary.navigationFinished,
              !flowIpcMultiWindowScheduled else {
            return
        }
        flowIpcMultiWindowScheduled = true
        flowIpcMultiWindowPhase = .waitingForReady
        let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
            ? config.flowIpcAutoShutdownTimeoutSeconds
            : 35
        flowIpcMultiWindowDeadline = Date().addingTimeInterval(timeout)
        pollMultiWindowAcceptance()
    }

    private func pollMultiWindowAcceptance() {
        guard let main = flowIpcWindowContext("main"),
              let secondary = flowIpcWindowContext("secondary") else {
            finishMultiWindowAcceptance(success: false, reason: "window-registry-incomplete")
            return
        }
        evaluateMultiWindowState(main, secondary) { mainState, secondaryState in
            guard let mainState, let secondaryState else {
                self.retryOrFailMultiWindowAcceptance(reason: "state-inspection-failed")
                return
            }
            switch flowIpcMultiWindowPhase {
            case .waitingForReady:
                guard self.multiWindowReady(mainState),
                      self.multiWindowReady(secondaryState) else {
                    self.retryOrFailMultiWindowAcceptance(reason: "windows-not-ready")
                    return
                }
                self.mutateMultiWindowField(main, value: "window-main") { mutated in
                    if mutated {
                        flowIpcMultiWindowPhase = .waitingForMainMutation
                        self.retryMultiWindowAcceptance()
                    } else {
                        self.finishMultiWindowAcceptance(
                            success: false,
                            reason: "main-mutation-dispatch-failed")
                    }
                }
            case .waitingForMainMutation:
                guard self.status(mainState).contains("window-main"),
                      self.status(secondaryState).contains("pending") else {
                    self.retryOrFailMultiWindowAcceptance(reason: "main-state-not-isolated")
                    return
                }
                self.mutateMultiWindowField(secondary, value: "window-secondary") { mutated in
                    if mutated {
                        flowIpcMultiWindowPhase = .waitingForSecondaryMutation
                        self.retryMultiWindowAcceptance()
                    } else {
                        self.finishMultiWindowAcceptance(
                            success: false,
                            reason: "secondary-mutation-dispatch-failed")
                    }
                }
            case .waitingForSecondaryMutation:
                guard self.status(mainState).contains("window-main"),
                      self.status(secondaryState).contains("window-secondary") else {
                    self.retryOrFailMultiWindowAcceptance(reason: "secondary-state-not-isolated")
                    return
                }
                let identityIsolated = self.string(mainState, "windowId") == "main"
                    && self.string(secondaryState, "windowId") == "secondary"
                    && self.string(mainState, "uiId") == "main"
                    && self.string(secondaryState, "uiId") == "secondary"
                    && self.string(mainState, "sessionId") != self.string(secondaryState, "sessionId")
                let pushIsolated = self.integer(mainState, "pushMessageCount") >= 1
                    && self.integer(secondaryState, "pushMessageCount") >= 1
                    && self.integer(mainState, "pushFirstSequence") == 1
                    && self.integer(secondaryState, "pushFirstSequence") == 1
                    && self.integer(mainState, "pushLastSequence")
                        == self.integer(mainState, "pushMessageCount")
                    && self.integer(secondaryState, "pushLastSequence")
                        == self.integer(secondaryState, "pushMessageCount")
                let errorFree = self.string(mainState, "error").isEmpty
                    && self.string(secondaryState, "error").isEmpty
                guard identityIsolated, pushIsolated, errorFree else {
                    self.finishMultiWindowAcceptance(
                        success: false,
                        reason: "identity-or-push-isolation-failed",
                        details: [
                            "identityIsolated": identityIsolated,
                            "pushSequencesIsolated": pushIsolated,
                            "errorFree": errorFree,
                            "mainPushMessageCount": self.integer(mainState, "pushMessageCount"),
                            "secondaryPushMessageCount": self.integer(secondaryState, "pushMessageCount"),
                            "mainPushFirstSequence": self.integer(mainState, "pushFirstSequence"),
                            "secondaryPushFirstSequence": self.integer(secondaryState, "pushFirstSequence"),
                            "mainPushLastSequence": self.integer(mainState, "pushLastSequence"),
                            "secondaryPushLastSequence": self.integer(secondaryState, "pushLastSequence")
                        ])
                    return
                }
                trace("recv", "multiWindow.acceptance", [
                    "complete": true,
                    "windowCount": 2,
                    "identityIsolated": identityIsolated,
                    "pushSequencesIsolated": pushIsolated,
                    "uiStateIsolated": true,
                    "mainPushFirstSequence": self.integer(mainState, "pushFirstSequence"),
                    "secondaryPushFirstSequence": self.integer(secondaryState, "pushFirstSequence"),
                    "mainPushLastSequence": self.integer(mainState, "pushLastSequence"),
                    "secondaryPushLastSequence": self.integer(secondaryState, "pushLastSequence"),
                    "mainWindowId": "main",
                    "secondaryWindowId": "secondary"
                ])
                secondary.window.close()
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    let mainSurvivedSecondaryClose = self.flowIpcWindowContext("main") != nil
                        && self.flowIpcWindowContext("secondary") == nil
                    trace("recv", "multiWindow.closeIsolation", [
                        "complete": mainSurvivedSecondaryClose,
                        "mainSurvivedSecondaryClose": mainSurvivedSecondaryClose,
                        "remainingWindowCount": mainSurvivedSecondaryClose ? 1 : 0
                    ])
                    main.window.close()
                }
            }
        }
    }

    private func evaluateMultiWindowState(
        _ main: FlowIpcWindowContext,
        _ secondary: FlowIpcWindowContext,
        completion: @escaping ([String: Any]?, [String: Any]?) -> Void
    ) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const context = window.VaadinDesktopHostContext || {};
          const clients = window.Vaadin?.Flow?.clients
            ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
            : [];
          return JSON.stringify({
            windowId: context.windowId || '',
            sessionId: context.sessionId || '',
            uiId: context.uiId || '',
            ready: diagnostics.standardBundleFirstMeaningfulPaintReported === true
              && diagnostics.standardBundlePushConnected === true
              && clients.length > 0,
            cookieCount: Number(diagnostics.standardBundleCookieCount || 0),
            pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
            pushFirstSequence: Number(diagnostics.standardBundlePushFirstSequence || 0),
            pushLastSequence: Number(diagnostics.standardBundlePushLastSequence || 0),
            status: document.querySelector('#ipc-vaadin-text-field-status')?.textContent || '',
            fieldPresent: !!document.querySelector('#ipc-vaadin-text-field'),
            error: diagnostics.standardBundleBrowserError
              || diagnostics.standardBundleOpaqueRequestError
              || diagnostics.standardBundleOpaqueResponseError
              || diagnostics.standardBundlePushError
              || ''
          });
        })()
        """
        var mainState: [String: Any]?
        var secondaryState: [String: Any]?
        let group = DispatchGroup()
        group.enter()
        main.webView.evaluateJavaScript(script) { result, _ in
            mainState = self.jsonObject(result)
            group.leave()
        }
        group.enter()
        secondary.webView.evaluateJavaScript(script) { result, _ in
            secondaryState = self.jsonObject(result)
            group.leave()
        }
        group.notify(queue: .main) {
            completion(mainState, secondaryState)
        }
    }

    private func mutateMultiWindowField(
        _ context: FlowIpcWindowContext,
        value: String,
        completion: @escaping (Bool) -> Void
    ) {
        let script = """
        (() => {
          const field = document.querySelector('#ipc-vaadin-text-field');
          if (!field) return false;
          field.value = \(javaScriptString(value));
          field.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
          return true;
        })()
        """
        context.webView.evaluateJavaScript(script) { result, _ in
            completion(result as? Bool == true)
        }
    }

    private func retryOrFailMultiWindowAcceptance(reason: String) {
        if Date() >= flowIpcMultiWindowDeadline {
            finishMultiWindowAcceptance(success: false, reason: reason)
        } else {
            retryMultiWindowAcceptance()
        }
    }

    private func retryMultiWindowAcceptance() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.pollMultiWindowAcceptance()
        }
    }

    private func finishMultiWindowAcceptance(
        success: Bool,
        reason: String,
        details: [String: Any] = [:]
    ) {
        var payload = details
        payload.merge([
            "complete": success,
            "reason": reason
        ]) { _, replacement in replacement }
        trace(success ? "recv" : "send", "multiWindow.acceptance", payload)
        flowIpcWindowContext("secondary")?.window.close()
        flowIpcWindowContext("main")?.window.close()
    }

    private func multiWindowReady(_ state: [String: Any]) -> Bool {
        state["ready"] as? Bool == true
            && state["fieldPresent"] as? Bool == true
            && status(state).contains("pending")
    }

    private func status(_ state: [String: Any]) -> String {
        string(state, "status")
    }

    private func string(_ state: [String: Any], _ key: String) -> String {
        state[key] as? String ?? ""
    }

    private func integer(_ state: [String: Any], _ key: String) -> Int {
        (state[key] as? NSNumber)?.intValue ?? state[key] as? Int ?? 0
    }

    private func jsonObject(_ value: Any?) -> [String: Any]? {
        guard let text = value as? String,
              let data = text.data(using: .utf8) else {
            return nil
        }
        return try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    }

    private func javaScriptString(_ value: String) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: [value]),
              let json = String(data: data, encoding: .utf8) else {
            return "\"\""
        }
        return String(json.dropFirst().dropLast())
    }
}
