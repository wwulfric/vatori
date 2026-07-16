import Cocoa
import Foundation
import WebKit

private enum FlowIpcStandardBundleFrameworkPhase {
    case waitingForReady
    case waitingForBudget
    case waitingForExpedite
    case waitingForPriority
    case waitingForDueDate
    case waitingForDialog
    case waitingForValidation
    case waitingForApply
}

private var flowIpcStandardBundleFrameworkScheduled = false
private var flowIpcStandardBundleFrameworkPhase =
    FlowIpcStandardBundleFrameworkPhase.waitingForReady
private var flowIpcStandardBundleFrameworkDeadline = Date.distantPast

extension WebViewHost {
    func scheduleStandardBundleFrameworkAcceptance(
        _ context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        guard config.transport == .flowIpc,
              config.flowIpcAutoFrameworkUpdate,
              !flowIpcStandardBundleFrameworkScheduled else {
            return
        }
        flowIpcStandardBundleFrameworkScheduled = true
        flowIpcStandardBundleFrameworkPhase = .waitingForReady
        flowIpcStandardBundleFrameworkDeadline = Date().addingTimeInterval(
            timeoutSeconds > 0 ? timeoutSeconds : 45)
        pollStandardBundleFrameworkAcceptance(context)
    }

    private func pollStandardBundleFrameworkAcceptance(_ context: FlowIpcWindowContext) {
        evaluateStandardBundleFrameworkState(context.webView) { state in
            guard let state else {
                self.retryOrFailStandardBundleFrameworkAcceptance(
                    context,
                    reason: "state-inspection-failed")
                return
            }
            if !self.standardFrameworkString(state, "error").isEmpty {
                self.finishStandardBundleFrameworkAcceptance(
                    context,
                    success: false,
                    reason: "browser-runtime-error",
                    state: state)
                return
            }

            switch flowIpcStandardBundleFrameworkPhase {
            case .waitingForReady:
                guard state["ready"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "standard-bundle-not-ready",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkBudgetAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForBudget,
                            failureReason: "budget-event-dispatch-failed")
                    }
            case .waitingForBudget:
                guard self.standardFrameworkString(state, "componentStatus")
                    .contains("Budget changed: 84.5") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "budget-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkExpediteAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForExpedite,
                            failureReason: "checkbox-event-dispatch-failed")
                    }
            case .waitingForExpedite:
                guard self.standardFrameworkString(state, "componentStatus")
                    .contains("Expedite changed: yes") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "checkbox-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPriorityAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPriority,
                            failureReason: "select-event-dispatch-failed")
                    }
            case .waitingForPriority:
                guard self.standardFrameworkString(state, "componentStatus")
                    .contains("Priority changed: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "select-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkDueDateAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForDueDate,
                            failureReason: "due-date-event-dispatch-failed")
                    }
            case .waitingForDueDate:
                guard self.standardFrameworkString(state, "matrixStatus")
                    .contains("Due date changed: 2026-07-14") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "due-date-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkDialogAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForDialog,
                            failureReason: "dialog-event-dispatch-failed")
                    }
            case .waitingForDialog:
                let expectedDialog = "Dialog opened: platform due 2026-07-14"
                guard self.standardFrameworkString(state, "matrixStatus")
                    .contains(expectedDialog),
                      self.standardFrameworkString(state, "dialogContent")
                    .contains(expectedDialog),
                      state["dialogOpened"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "dialog-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkValidationAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForValidation,
                            failureReason: "validation-event-dispatch-failed")
                    }
            case .waitingForValidation:
                guard self.standardFrameworkString(state, "validationStatus")
                    .contains("Validation accepted: platform") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "validation-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkApplyAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForApply,
                            failureReason: "apply-event-dispatch-failed")
                    }
            case .waitingForApply:
                let expectedUpdate = "Revision 1: budget 84.5, mode expedited, priority audit"
                let serverStateUpdated = self.standardFrameworkString(state, "status")
                    .contains(expectedUpdate)
                    && self.standardFrameworkString(state, "latestUpdate")
                        .contains(expectedUpdate)
                let nativeWindowTitleApplied = context.window.title
                    == "Flow IPC Framework App - 1"
                let nativeClipboardApplied = NSPasteboard.general
                    .string(forType: .string)?.contains(expectedUpdate) == true
                guard serverStateUpdated,
                      nativeWindowTitleApplied,
                      nativeClipboardApplied else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "apply-outcome-not-observed",
                        state: state)
                    return
                }
                self.finishStandardBundleFrameworkAcceptance(
                    context,
                    success: true,
                    reason: "complete",
                    state: state,
                    additionalDetails: [
                        "budgetEventObserved": true,
                        "checkboxEventObserved": true,
                        "selectEventObserved": true,
                        "datePickerEventObserved": true,
                        "dialogEventObserved": true,
                        "formValidationObserved": true,
                        "applyEventObserved": true,
                        "serverStateUpdated": true,
                        "nativeWindowTitleApplied": true,
                        "nativeClipboardApplied": true
                    ])
            }
        }
    }

    private func evaluateStandardBundleFrameworkState(
        _ webView: WKWebView,
        completion: @escaping ([String: Any]?) -> Void
    ) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const clients = window.Vaadin?.Flow?.clients
            ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
            : [];
          if (Number(diagnostics.standardBundlePushMessageCount || 0) >= 1
              && Number(diagnostics.standardBundlePushReconnectCount || 0) === 0
              && !diagnostics.standardBundlePushReconnectTriggered) {
            diagnostics.standardBundlePushReconnectTriggered = true;
            const push = window.VaadinDesktopPush;
            push?.reconnect?.();
            const connectionId = diagnostics.standardBundlePushActiveConnectionId;
            if (connectionId) {
              push?.receive?.({
                type: 'opaquePush',
                payload: {
                  connectionId,
                  pushSequence: Number(diagnostics.standardBundlePushLastSequence || 0) + 2,
                  uidl: '',
                  resync: false
                }
              });
            }
          }
          const pushStatus = document.querySelector('#ipc-standard-push-status')?.textContent || '';
          const error = diagnostics.standardBundleBrowserError
            || diagnostics.standardBundleOpaqueRequestError
            || diagnostics.standardBundleOpaqueResponseError
            || diagnostics.standardBundlePushError
            || '';
          const root = document.querySelector('#framework-root');
          return JSON.stringify({
            ready: diagnostics.standardBundleFirstMeaningfulPaintReported === true
              && diagnostics.standardBundleCookieBridgeInstalled === true
              && diagnostics.standardBundlePushBridgeInstalled === true
              && diagnostics.standardBundlePushConnected === true
              && Number(diagnostics.standardBundlePushMessageCount || 0) >= 3
              && Number(diagnostics.standardBundlePushReconnectCount || 0) >= 1
              && Number(diagnostics.standardBundlePushResyncCount || 0) >= 1
              && pushStatus.includes('Standard push 2 READY')
              && Number(diagnostics.standardBundleOpaqueRequestCount || 0) >= 2
              && Number(diagnostics.standardBundleOpaqueResponseCount || 0) >= 2
              && clients.length > 0
              && !!root
              && !!document.querySelector('#framework-budget')
              && !!document.querySelector('#framework-expedite')
              && !!document.querySelector('#framework-priority')
              && !!document.querySelector('#framework-due-date')
              && !!document.querySelector('#framework-dialog-open')
              && !!document.querySelector('#framework-validation-action')
              && !!document.querySelector('#framework-apply')
              && !error,
            rootRendered: !!root,
            flowClientCount: clients.length,
            requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
            pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
            pushReconnectCount: Number(diagnostics.standardBundlePushReconnectCount || 0),
            pushResyncCount: Number(diagnostics.standardBundlePushResyncCount || 0),
            pushStatus,
            componentStatus: document.querySelector('#framework-component-status')?.textContent || '',
            matrixStatus: document.querySelector('#framework-matrix-status')?.textContent || '',
            dialogContent: document.querySelector('#framework-dialog-content')?.textContent || '',
            dialogOpened: document.querySelector('#framework-dialog')?.opened === true,
            validationStatus: document.querySelector('#framework-validation-status')?.textContent || '',
            status: document.querySelector('#framework-status')?.textContent || '',
            latestUpdate: document.querySelector('#framework-latest-update')?.textContent || '',
            budgetValue: String(document.querySelector('#framework-budget')?.value || ''),
            expedited: document.querySelector('#framework-expedite')?.checked === true,
            priorityValue: String(document.querySelector('#framework-priority')?.value || ''),
            error
          });
        })()
        """
        webView.evaluateJavaScript(script) { result, _ in
            completion(self.standardFrameworkJsonObject(result))
        }
    }

    private func dispatchStandardBundleFrameworkAction(
        _ webView: WKWebView,
        script: String,
        completion: @escaping (Bool) -> Void
    ) {
        webView.evaluateJavaScript(script) { result, _ in
            completion(result as? Bool == true)
        }
    }

    private func advanceStandardBundleFrameworkAcceptance(
        _ context: FlowIpcWindowContext,
        dispatched: Bool,
        next: FlowIpcStandardBundleFrameworkPhase,
        failureReason: String
    ) {
        guard dispatched else {
            finishStandardBundleFrameworkAcceptance(
                context,
                success: false,
                reason: failureReason)
            return
        }
        flowIpcStandardBundleFrameworkPhase = next
        retryStandardBundleFrameworkAcceptance(context)
    }

    private func retryOrFailStandardBundleFrameworkAcceptance(
        _ context: FlowIpcWindowContext,
        reason: String,
        state: [String: Any] = [:]
    ) {
        if Date() >= flowIpcStandardBundleFrameworkDeadline {
            finishStandardBundleFrameworkAcceptance(
                context,
                success: false,
                reason: reason,
                state: state)
        } else {
            retryStandardBundleFrameworkAcceptance(context)
        }
    }

    private func retryStandardBundleFrameworkAcceptance(_ context: FlowIpcWindowContext) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.pollStandardBundleFrameworkAcceptance(context)
        }
    }

    private func finishStandardBundleFrameworkAcceptance(
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
            "rootRendered": state["rootRendered"] as? Bool == true,
            "flowClientOwned": standardFrameworkInteger(state, "flowClientCount") > 0,
            "opaqueRequestBridgeObserved": standardFrameworkInteger(state, "requestCount") >= 2,
            "opaqueResponseBridgeObserved": standardFrameworkInteger(state, "responseCount") >= 2,
            "pushRecoveryObserved": standardFrameworkInteger(state, "pushMessageCount") >= 3
                && standardFrameworkInteger(state, "pushReconnectCount") >= 1
                && standardFrameworkInteger(state, "pushResyncCount") >= 1,
            "errorFree": standardFrameworkString(state, "error").isEmpty,
            "requestCount": standardFrameworkInteger(state, "requestCount"),
            "responseCount": standardFrameworkInteger(state, "responseCount"),
            "pushMessageCount": standardFrameworkInteger(state, "pushMessageCount"),
            "pushReconnectCount": standardFrameworkInteger(state, "pushReconnectCount"),
            "pushResyncCount": standardFrameworkInteger(state, "pushResyncCount"),
            "componentStatus": standardFrameworkString(state, "componentStatus"),
            "status": standardFrameworkString(state, "status")
        ]
        payload.merge(additionalDetails) { _, replacement in replacement }
        trace(success ? "recv" : "send", "standardBundle.frameworkAcceptance", payload)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            trace("recv", "window.closeRequested", [
                "label": context.scope.windowId,
                "reason": success
                    ? "standard-bundle-framework-acceptance-complete"
                    : "standard-bundle-framework-acceptance-failed"
            ])
            context.window.close()
        }
    }

    private func standardFrameworkString(_ state: [String: Any], _ key: String) -> String {
        state[key] as? String ?? ""
    }

    private func standardFrameworkInteger(_ state: [String: Any], _ key: String) -> Int {
        (state[key] as? NSNumber)?.intValue ?? state[key] as? Int ?? 0
    }

    private func standardFrameworkJsonObject(_ value: Any?) -> [String: Any]? {
        guard let text = value as? String,
              let data = text.data(using: .utf8) else {
            return nil
        }
        return try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    }

    private static let standardFrameworkBudgetAction = """
    (() => {
      const element = document.querySelector('#framework-budget');
      if (!element) return false;
      element.value = '84.5';
      if (element.inputElement) element.inputElement.value = '84.5';
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkExpediteAction = """
    (() => {
      const element = document.querySelector('#framework-expedite');
      if (!element) return false;
      element.checked = true;
      if (element.inputElement) element.inputElement.checked = true;
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkPriorityAction = """
    (() => {
      const element = document.querySelector('#framework-priority');
      if (!element) return false;
      element.value = 'audit';
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkDueDateAction = """
    (() => {
      const element = document.querySelector('#framework-due-date');
      if (!element) return false;
      element.value = '2026-07-14';
      if (element.inputElement) element.inputElement.value = '2026-07-14';
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkDialogAction = """
    (() => {
      const element = document.querySelector('#framework-dialog-open');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkValidationAction = """
    (() => {
      const element = document.querySelector('#framework-validation-action');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkApplyAction = """
    (() => {
      const element = document.querySelector('#framework-apply');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """
}
