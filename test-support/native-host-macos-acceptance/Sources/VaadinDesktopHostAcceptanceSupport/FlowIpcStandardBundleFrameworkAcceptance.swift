import Cocoa
import Foundation
import WebKit

private enum FlowIpcStandardBundleFrameworkPhase {
    case waitingForReady
    case waitingForBudget
    case waitingForExpedite
    case waitingForCheckboxGroupClient
    case waitingForCheckboxGroupServer
    case waitingForPriority
    case waitingForReviewer
    case waitingForDueDate
    case waitingForRadioGroup
    case waitingForTextFieldClient
    case waitingForTextFieldServer
    case waitingForTextArea
    case waitingForEmailField
    case waitingForIntegerField
    case waitingForNumberFieldClient
    case waitingForNumberFieldServer
    case waitingForPasswordField
    case waitingForTimePicker
    case waitingForDateTimePickerClient
    case waitingForDateTimePickerServer
    case waitingForSplitLayoutDrag
    case waitingForSplitLayoutServer
    case waitingForAssignees
    case waitingForListBoxClient
    case waitingForListBoxServer
    case waitingForMultiSelectListBoxClient
    case waitingForMultiSelectListBoxServer
    case waitingForCustomFieldClient
    case waitingForCustomFieldServer
    case waitingForDialog
    case waitingForValidation
    case waitingForFormLayoutClick
    case waitingForFormLayoutServer
    case waitingForHorizontalLayoutClick
    case waitingForHorizontalLayoutServer
    case waitingForFlexLayoutClick
    case waitingForFlexLayoutServer
    case waitingForVerticalLayoutClick
    case waitingForVerticalLayoutServer
    case waitingForScrollerClient
    case waitingForScrollerServer
    case waitingForAvatar
    case waitingForMessageInput
    case waitingForRichTextEditor
    case waitingForLoginOverlay
    case waitingForLoginOverlayForgotPassword
    case waitingForLoginOverlayShowError
    case waitingForLoginOverlaySubmit
    case waitingForLoginFormSubmit
    case waitingForLoginFormForgotPassword
    case waitingForLoginOverlayActionPost
    case waitingForLoginOverlayActionPostSubmit
    case waitingForLoginFormActionPostSubmit
    case waitingForLoginVariants
    case waitingForLoginFormSubmitButton
    case waitingForLoginFormShowError
    case waitingForLoginFormEnabled
    case waitingForLoginFormEnabledSubmit
    case waitingForLoginFormFullI18n
    case waitingForLoginFormAutofillAttributes
    case waitingForLoginFormA11ySemantics
    case waitingForLoginFormNoAutofocus
    case waitingForLoginOverlayAutofocusControl
    case waitingForLoginOverlayAutofocusControlClosed
    case waitingForLoginOverlayNoAutofocus
    case waitingForLoginOverlayNoAutofocusClosed
    case waitingForLoginOverlayAutofillAttributes
    case waitingForLoginOverlayAutofillAttributesClosed
    case waitingForLoginOverlayA11ySemantics
    case waitingForLoginOverlayA11ySemanticsClosed
    case waitingForLoginOverlayOutsideClose
    case waitingForLoginOverlayOutsideClickPrevented
    case waitingForLoginOverlayOutsideCloseClosed
    case waitingForLoginOverlayEscapeKey
    case waitingForLoginOverlayEscapeKeyPrevented
    case waitingForLoginOverlayEscapeKeyClosed
    case waitingForLoginOverlayFullI18n
    case waitingForTabs
    case waitingForDetails
    case waitingForAccordion
    case waitingForMenuBar
    case waitingForSideNav
    case waitingForContextMenu
    case waitingForContextMenuAction
    case waitingForGridContextMenu
    case waitingForGridContextMenuAction
    case waitingForLazyGrid
    case waitingForVirtualListClient
    case waitingForVirtualListServer
    case waitingForPopover
    case waitingForNotification
    case waitingForConfirmDialog
    case waitingForConfirmDialogConfirm
    case waitingForConfirmDialogCancelOpen
    case waitingForConfirmDialogCancel
    case waitingForConfirmDialogRejectOpen
    case waitingForConfirmDialogReject
    case waitingForProgressBarValue
    case waitingForProgressBarIndeterminate
    case waitingForUploadSubmit
    case waitingForResourceFetch
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
                    script: Self.standardFrameworkCheckboxGroupClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForCheckboxGroupClient,
                            failureReason: "checkbox-group-event-dispatch-failed")
                    }
            case .waitingForCheckboxGroupClient:
                guard state["checkboxGroupClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "checkbox-group-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkCheckboxGroupServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForCheckboxGroupServer,
                            failureReason: "checkbox-group-server-dispatch-failed")
                    }
            case .waitingForCheckboxGroupServer:
                guard state["checkboxGroupServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "checkbox-group-server-patch-not-observed",
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
                    script: Self.standardFrameworkReviewerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForReviewer,
                            failureReason: "combo-box-event-dispatch-failed")
                    }
            case .waitingForReviewer:
                guard self.standardFrameworkString(state, "matrixStatus")
                    .contains("Reviewer changed: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "combo-box-patch-not-observed",
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
                    script: Self.standardFrameworkRadioGroupAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForRadioGroup,
                            failureReason: "radio-group-event-dispatch-failed")
                    }
            case .waitingForRadioGroup:
                guard self.standardFrameworkString(state, "inputMatrixStatus")
                    .contains("Decision changed: approve") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "radio-group-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTextFieldClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTextFieldClient,
                            failureReason: "text-field-event-dispatch-failed")
                    }
            case .waitingForTextFieldClient:
                guard state["textFieldClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "text-field-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTextFieldServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTextFieldServer,
                            failureReason: "text-field-server-dispatch-failed")
                    }
            case .waitingForTextFieldServer:
                guard state["textFieldServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "text-field-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTextAreaAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTextArea,
                            failureReason: "text-area-event-dispatch-failed")
                    }
            case .waitingForTextArea:
                guard self.standardFrameworkString(state, "inputMatrixStatus")
                    .contains("Notes changed: Flow IPC notes updated") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "text-area-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkEmailFieldAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForEmailField,
                            failureReason: "email-field-event-dispatch-failed")
                    }
            case .waitingForEmailField:
                guard self.standardFrameworkString(state, "inputMatrixStatus")
                    .contains("Email changed: owner+ipc@example.test") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "email-field-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkIntegerFieldAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForIntegerField,
                            failureReason: "integer-field-event-dispatch-failed")
                    }
            case .waitingForIntegerField:
                guard self.standardFrameworkString(state, "inputMatrixStatus")
                    .contains("Score changed: 7") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "integer-field-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNumberFieldClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNumberFieldClient,
                            failureReason: "number-field-event-dispatch-failed")
                    }
            case .waitingForNumberFieldClient:
                guard state["numberFieldClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "number-field-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNumberFieldServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNumberFieldServer,
                            failureReason: "number-field-server-dispatch-failed")
                    }
            case .waitingForNumberFieldServer:
                guard state["numberFieldServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "number-field-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPasswordFieldAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPasswordField,
                            failureReason: "password-field-event-dispatch-failed")
                    }
            case .waitingForPasswordField:
                guard self.standardFrameworkString(state, "inputMatrixStatus")
                    .contains("Secret changed: length=10") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "password-field-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTimePickerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTimePicker,
                            failureReason: "time-picker-event-dispatch-failed")
                    }
            case .waitingForTimePicker:
                guard self.standardFrameworkString(state, "inputMatrixStatus")
                    .contains("Review time changed: 14:45") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "time-picker-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkDateTimePickerClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForDateTimePickerClient,
                            failureReason: "date-time-picker-event-dispatch-failed")
                    }
            case .waitingForDateTimePickerClient:
                guard state["dateTimePickerClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "date-time-picker-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkDateTimePickerServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForDateTimePickerServer,
                            failureReason: "date-time-picker-server-dispatch-failed")
                    }
            case .waitingForDateTimePickerServer:
                guard state["dateTimePickerServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "date-time-picker-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkSplitLayoutDragAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForSplitLayoutDrag,
                            failureReason: "split-layout-drag-event-dispatch-failed")
                    }
            case .waitingForSplitLayoutDrag:
                guard state["splitLayoutDragObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "split-layout-drag-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkSplitLayoutServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForSplitLayoutServer,
                            failureReason: "split-layout-server-dispatch-failed")
                    }
            case .waitingForSplitLayoutServer:
                guard state["splitLayoutServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "split-layout-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkAssigneesAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForAssignees,
                            failureReason: "assignees-event-dispatch-failed")
                    }
            case .waitingForAssignees:
                guard self.standardFrameworkString(state, "assigneesStatus")
                    .contains("Assignees changed: ava,sol") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "assignees-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkListBoxClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForListBoxClient,
                            failureReason: "list-box-event-dispatch-failed")
                    }
            case .waitingForListBoxClient:
                guard state["listBoxClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "list-box-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkListBoxServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForListBoxServer,
                            failureReason: "list-box-server-dispatch-failed")
                    }
            case .waitingForListBoxServer:
                guard state["listBoxServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "list-box-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMultiSelectListBoxClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMultiSelectListBoxClient,
                            failureReason: "multi-select-list-box-event-dispatch-failed")
                    }
            case .waitingForMultiSelectListBoxClient:
                guard state["multiSelectListBoxClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "multi-select-list-box-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMultiSelectListBoxServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMultiSelectListBoxServer,
                            failureReason: "multi-select-list-box-server-dispatch-failed")
                    }
            case .waitingForMultiSelectListBoxServer:
                guard state["multiSelectListBoxServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "multi-select-list-box-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkCustomFieldClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForCustomFieldClient,
                            failureReason: "custom-field-event-dispatch-failed")
                    }
            case .waitingForCustomFieldClient:
                guard state["customFieldClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "custom-field-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkCustomFieldServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForCustomFieldServer,
                            failureReason: "custom-field-server-dispatch-failed")
                    }
            case .waitingForCustomFieldServer:
                guard state["customFieldServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "custom-field-server-patch-not-observed",
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
                let expectedDialog = "Dialog opened: audit due 2026-07-14"
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
                    .contains("Validation accepted: desktop") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "validation-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkFormLayoutClickAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFormLayoutClick,
                            failureReason: "form-layout-click-dispatch-failed")
                    }
            case .waitingForFormLayoutClick:
                guard state["formLayoutClickObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "form-layout-click-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkFormLayoutServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFormLayoutServer,
                            failureReason: "form-layout-server-dispatch-failed")
                    }
            case .waitingForFormLayoutServer:
                guard state["formLayoutServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "form-layout-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkHorizontalLayoutClickAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForHorizontalLayoutClick,
                            failureReason: "horizontal-layout-click-dispatch-failed")
                    }
            case .waitingForHorizontalLayoutClick:
                guard state["horizontalLayoutClickObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "horizontal-layout-click-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkHorizontalLayoutServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForHorizontalLayoutServer,
                            failureReason: "horizontal-layout-server-dispatch-failed")
                    }
            case .waitingForHorizontalLayoutServer:
                guard state["horizontalLayoutServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "horizontal-layout-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkFlexLayoutClickAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFlexLayoutClick,
                            failureReason: "flex-layout-click-dispatch-failed")
                    }
            case .waitingForFlexLayoutClick:
                guard state["flexLayoutClickObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "flex-layout-click-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkFlexLayoutServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFlexLayoutServer,
                            failureReason: "flex-layout-server-dispatch-failed")
                    }
            case .waitingForFlexLayoutServer:
                guard state["flexLayoutServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "flex-layout-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkVerticalLayoutClickAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForVerticalLayoutClick,
                            failureReason: "vertical-layout-click-dispatch-failed")
                    }
            case .waitingForVerticalLayoutClick:
                guard state["verticalLayoutClickObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "vertical-layout-click-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkVerticalLayoutServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForVerticalLayoutServer,
                            failureReason: "vertical-layout-server-dispatch-failed")
                    }
            case .waitingForVerticalLayoutServer:
                guard state["verticalLayoutServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "vertical-layout-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkScrollerClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForScrollerClient,
                            failureReason: "scroller-client-dispatch-failed")
                    }
            case .waitingForScrollerClient:
                guard state["scrollerClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "scroller-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkScrollerServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForScrollerServer,
                            failureReason: "scroller-server-dispatch-failed")
                    }
            case .waitingForScrollerServer:
                guard state["scrollerServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "scroller-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkAvatarAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForAvatar,
                            failureReason: "avatar-event-dispatch-failed")
                    }
            case .waitingForAvatar:
                guard self.standardFrameworkString(state, "richContentStatus")
                    .contains("Avatar clicked: ava") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "avatar-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMessageInputAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMessageInput,
                            failureReason: "message-input-submit-dispatch-failed")
                    }
            case .waitingForMessageInput:
                let expectedMessage = "Message submitted: IPC note"
                guard self.standardFrameworkString(state, "richContentStatus")
                    .contains(expectedMessage),
                      self.standardFrameworkString(state, "messageListText")
                    .contains("Echo: IPC note") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "message-input-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkRichTextEditorAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForRichTextEditor,
                            failureReason: "rich-text-editor-event-dispatch-failed")
                    }
            case .waitingForRichTextEditor:
                guard self.standardFrameworkString(state, "richTextEditorStatus")
                    .contains("Rich text changed: Flow IPC rich text bold=true"),
                      self.standardFrameworkString(state, "richTextEditorBehaviorStatus")
                    .contains("Rich text behavior delta/html: Flow IPC rich text bold=true") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "rich-text-editor-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlay,
                            failureReason: "login-overlay-open-dispatch-failed")
                    }
            case .waitingForLoginOverlay:
                guard self.standardFrameworkString(state, "richContentStatus")
                    .contains("Login overlay reopened: true"),
                      state["loginOverlayOpened"] as? Bool == true,
                      state["loginOverlayControlsRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayForgotPasswordAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayForgotPassword,
                            failureReason: "login-overlay-forgot-password-dispatch-failed")
                    }
            case .waitingForLoginOverlayForgotPassword:
                guard self.standardFrameworkString(state, "richContentStatus")
                    .contains("Login forgot password requested"),
                      state["loginOverlayOpened"] as? Bool == true,
                      state["loginOverlayForgotButtonRendered"] as? Bool == true,
                      state["loginOverlayControlsRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-forgot-password-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayShowErrorAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayShowError,
                            failureReason: "login-overlay-show-error-dispatch-failed")
                    }
            case .waitingForLoginOverlayShowError:
                guard state["loginOverlayShowErrorObserved"] as? Bool == true,
                      state["loginOverlayOpened"] as? Bool == true,
                      state["loginOverlayControlsRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-show-error-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlaySubmitAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlaySubmit,
                            failureReason: "login-overlay-submit-dispatch-failed")
                    }
            case .waitingForLoginOverlaySubmit:
                guard self.standardFrameworkString(state, "richContentStatus")
                    .contains("Login submitted: ipc-user password-length=8"),
                      state["loginOverlayClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-submit-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormSubmitAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormSubmit,
                            failureReason: "login-form-submit-dispatch-failed")
                    }
            case .waitingForLoginFormSubmit:
                guard self.standardFrameworkString(state, "loginFormStatus")
                    .contains("LoginForm submitted: inline-user password-length=8") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-submit-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormForgotPasswordAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormForgotPassword,
                            failureReason: "login-form-forgot-password-dispatch-failed")
                    }
            case .waitingForLoginFormForgotPassword:
                guard self.standardFrameworkString(state, "loginFormStatus")
                    .contains("LoginForm forgot password requested") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-forgot-password-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayActionPostOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayActionPost,
                            failureReason: "login-overlay-action-post-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayActionPost:
                guard self.standardFrameworkString(state, "loginOverlayActionPostStatus")
                    .contains("LoginOverlay action post opened: true"),
                      state["loginOverlayActionPostOpened"] as? Bool == true,
                      state["loginOverlayActionPostControlsRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-action-post-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayActionPostSubmitAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayActionPostSubmit,
                            failureReason: "login-overlay-action-post-submit-dispatch-failed")
                    }
            case .waitingForLoginOverlayActionPostSubmit:
                guard self.standardFrameworkString(state, "loginOverlayActionPostStatus")
                    .contains("LoginOverlay action post submitted: overlay-action-user password-length=8 action=flow-ipc-login-overlay-action"),
                      state["loginOverlayActionPostClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-action-post-submit-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormActionPostSubmitAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormActionPostSubmit,
                            failureReason: "login-form-action-post-submit-dispatch-failed")
                    }
            case .waitingForLoginFormActionPostSubmit:
                guard self.standardFrameworkString(state, "loginFormActionPostStatus")
                    .contains("LoginForm action post submitted: inline-action-user password-length=8 action=flow-ipc-login-form-action") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-action-post-submit-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginVariantsAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginVariants,
                            failureReason: "login-variants-dispatch-failed")
                    }
            case .waitingForLoginVariants:
                guard self.standardFrameworkString(state, "loginOverlaySubmitButtonStatus")
                    .contains("LoginOverlay submit button submitted: overlay-button-user password-length=8") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-variants-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormSubmitButtonAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormSubmitButton,
                            failureReason: "login-form-submit-button-dispatch-failed")
                    }
            case .waitingForLoginFormSubmitButton:
                guard state["loginFormSubmitButtonObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-submit-button-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormShowErrorAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormShowError,
                            failureReason: "login-form-show-error-dispatch-failed")
                    }
            case .waitingForLoginFormShowError:
                guard self.standardFrameworkString(state, "loginFormShowErrorStatus")
                    .contains("LoginForm show error rejected: rejected-user password-length=8 title=Desktop session expired") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-show-error-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormEnableAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormEnabled,
                            failureReason: "login-form-enable-dispatch-failed")
                    }
            case .waitingForLoginFormEnabled:
                guard self.standardFrameworkString(state, "loginFormEnabledStatus")
                    .contains("LoginForm enabled lifecycle: enabled click=1"),
                      state["loginFormEnabledDisabled"] as? Bool == false else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-enabled-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormEnabledSubmitAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormEnabledSubmit,
                            failureReason: "login-form-enabled-submit-dispatch-failed")
                    }
            case .waitingForLoginFormEnabledSubmit:
                guard self.standardFrameworkString(state, "loginFormEnabledStatus")
                    .contains("LoginForm enabled lifecycle submitted: enabled-user password-length=8 submit=1") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-enabled-submit-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormFullI18nAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormFullI18n,
                            failureReason: "login-form-full-i18n-dispatch-failed")
                    }
            case .waitingForLoginFormFullI18n:
                guard state["loginFormFullI18nRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-full-i18n-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormAutofillAttributesAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormAutofillAttributes,
                            failureReason: "login-form-autofill-attributes-dispatch-failed")
                    }
            case .waitingForLoginFormAutofillAttributes:
                guard state["loginFormAutofillAttributesObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-autofill-attributes-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormA11ySemanticsAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormA11ySemantics,
                            failureReason: "login-form-a11y-semantics-dispatch-failed")
                    }
            case .waitingForLoginFormA11ySemantics:
                guard state["loginFormA11ySemanticsObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-a11y-semantics-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginFormNoAutofocusAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginFormNoAutofocus,
                            failureReason: "login-form-no-autofocus-dispatch-failed")
                    }
            case .waitingForLoginFormNoAutofocus:
                guard state["loginFormNoAutofocusObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-form-no-autofocus-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayAutofocusControlOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayAutofocusControl,
                            failureReason: "login-overlay-autofocus-control-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayAutofocusControl:
                guard state["loginOverlayAutofocusControlObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-autofocus-control-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayAutofocusControlCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayAutofocusControlClosed,
                            failureReason: "login-overlay-autofocus-control-close-dispatch-failed")
                    }
            case .waitingForLoginOverlayAutofocusControlClosed:
                guard state["loginOverlayAutofocusControlClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-autofocus-control-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayNoAutofocusOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayNoAutofocus,
                            failureReason: "login-overlay-no-autofocus-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayNoAutofocus:
                guard state["loginOverlayNoAutofocusObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-no-autofocus-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayNoAutofocusCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayNoAutofocusClosed,
                            failureReason: "login-overlay-no-autofocus-close-dispatch-failed")
                    }
            case .waitingForLoginOverlayNoAutofocusClosed:
                guard state["loginOverlayNoAutofocusClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-no-autofocus-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayAutofillAttributesOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayAutofillAttributes,
                            failureReason: "login-overlay-autofill-attributes-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayAutofillAttributes:
                guard state["loginOverlayAutofillAttributesObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-autofill-attributes-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayAutofillAttributesCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayAutofillAttributesClosed,
                            failureReason: "login-overlay-autofill-attributes-close-dispatch-failed")
                    }
            case .waitingForLoginOverlayAutofillAttributesClosed:
                guard state["loginOverlayAutofillAttributesClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-autofill-attributes-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayA11ySemanticsOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayA11ySemantics,
                            failureReason: "login-overlay-a11y-semantics-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayA11ySemantics:
                guard state["loginOverlayA11ySemanticsObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-a11y-semantics-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayA11ySemanticsCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayA11ySemanticsClosed,
                            failureReason: "login-overlay-a11y-semantics-close-dispatch-failed")
                    }
            case .waitingForLoginOverlayA11ySemanticsClosed:
                guard state["loginOverlayA11ySemanticsClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-a11y-semantics-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayOutsideCloseOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayOutsideClose,
                            failureReason: "login-overlay-outside-close-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayOutsideClose:
                guard state["loginOverlayOutsideCloseObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-outside-close-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayOutsideClickAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayOutsideClickPrevented,
                            failureReason: "login-overlay-outside-click-dispatch-failed")
                    }
            case .waitingForLoginOverlayOutsideClickPrevented:
                guard state["loginOverlayOutsideClickPreventedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-outside-click-prevention-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayOutsideCloseCleanupAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayOutsideCloseClosed,
                            failureReason: "login-overlay-outside-close-cleanup-dispatch-failed")
                    }
            case .waitingForLoginOverlayOutsideCloseClosed:
                guard state["loginOverlayOutsideClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-outside-close-cleanup-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayEscapeKeyOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayEscapeKey,
                            failureReason: "login-overlay-escape-key-open-dispatch-failed")
                    }
            case .waitingForLoginOverlayEscapeKey:
                guard state["loginOverlayEscapeKeyObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-escape-key-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayEscapeKeyAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayEscapeKeyPrevented,
                            failureReason: "login-overlay-escape-key-dispatch-failed")
                    }
            case .waitingForLoginOverlayEscapeKeyPrevented:
                guard state["loginOverlayEscapeKeyPreventedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-escape-key-prevention-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayEscapeKeyCleanupAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayEscapeKeyClosed,
                            failureReason: "login-overlay-escape-key-cleanup-dispatch-failed")
                    }
            case .waitingForLoginOverlayEscapeKeyClosed:
                guard state["loginOverlayEscapeKeyClosed"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-escape-key-cleanup-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTabsAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTabs,
                            failureReason: "tabs-select-dispatch-failed")
                    }
            case .waitingForLoginOverlayFullI18n:
                guard state["loginOverlayFullI18nRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "login-overlay-full-i18n-not-observed",
                        state: state)
                    return
                }
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
                self.finishSuccessfulStandardBundleFrameworkAcceptance(context, state: state)
            case .waitingForTabs:
                guard self.standardFrameworkString(state, "navigationMatrixStatus")
                    .contains("Tab changed: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tabs-selection-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkDetailsAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForDetails,
                            failureReason: "details-open-dispatch-failed")
                    }
            case .waitingForDetails:
                guard self.standardFrameworkString(state, "navigationMatrixStatus")
                    .contains("Details opened: true"),
                      state["detailsOpened"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "details-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkAccordionAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForAccordion,
                            failureReason: "accordion-open-dispatch-failed")
                    }
            case .waitingForAccordion:
                guard self.standardFrameworkString(state, "navigationMatrixStatus")
                    .contains("Accordion opened: queue") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "accordion-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMenuBarAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMenuBar,
                            failureReason: "menu-bar-action-dispatch-failed")
                    }
            case .waitingForMenuBar:
                guard self.standardFrameworkString(state, "navigationMatrixStatus")
                    .contains("Menu action: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "menu-bar-action-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkSideNavAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForSideNav,
                            failureReason: "side-nav-click-dispatch-failed")
                    }
            case .waitingForSideNav:
                guard self.standardFrameworkString(state, "richContentStatus")
                    .contains("Side nav selected: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "side-nav-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkContextMenuOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForContextMenu,
                            failureReason: "context-menu-open-dispatch-failed")
                    }
            case .waitingForContextMenu:
                guard self.standardFrameworkString(state, "overlayMatrixStatus")
                    .contains("Context menu opened: true"),
                      state["contextMenuOpened"] as? Bool == true,
                      state["contextMenuItemRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "context-menu-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkContextMenuItemAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForContextMenuAction,
                            failureReason: "context-menu-item-click-dispatch-failed")
                    }
            case .waitingForContextMenuAction:
                guard self.standardFrameworkString(state, "overlayMatrixStatus")
                    .contains("Context menu action: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "context-menu-action-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridContextMenuOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridContextMenu,
                            failureReason: "grid-context-menu-open-dispatch-failed")
                    }
            case .waitingForGridContextMenu:
                guard self.standardFrameworkString(state, "overlayMatrixStatus")
                    .contains("Grid context menu opened: true"),
                      state["gridContextMenuOpened"] as? Bool == true,
                      state["gridContextMenuItemRendered"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-context-menu-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridContextMenuItemAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridContextMenuAction,
                            failureReason: "grid-context-menu-item-click-dispatch-failed")
                    }
            case .waitingForGridContextMenuAction:
                guard self.standardFrameworkString(state, "overlayMatrixStatus")
                    .contains("Grid context menu action: audit") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-context-menu-action-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyGridAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGrid,
                            failureReason: "lazy-grid-scroll-dispatch-failed")
                    }
            case .waitingForLazyGrid:
                let lazyGridStatus = self.standardFrameworkString(state, "lazyGridStatus")
                let directFetch = lazyGridStatus.contains("Lazy grid fetch ")
                    && !lazyGridStatus.contains(": 0-")
                let reportedFetch = lazyGridStatus.contains("Lazy grid fetches:")
                    && !lazyGridStatus.contains("range: pending")
                    && !lazyGridStatus.contains("range: 0-")
                guard directFetch || reportedFetch else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-scroll-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkVirtualListClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForVirtualListClient,
                            failureReason: "virtual-list-scroll-dispatch-failed")
                    }
            case .waitingForVirtualListClient:
                guard state["virtualListClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "virtual-list-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkVirtualListServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForVirtualListServer,
                            failureReason: "virtual-list-server-dispatch-failed")
                    }
            case .waitingForVirtualListServer:
                guard state["virtualListServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "virtual-list-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPopoverAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPopover,
                            failureReason: "popover-click-dispatch-failed")
                    }
            case .waitingForPopover:
                guard self.standardFrameworkString(state, "overlayMatrixStatus")
                    .contains("Popover opened: true"),
                      state["popoverOpened"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "popover-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNotificationAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNotification,
                            failureReason: "notification-click-dispatch-failed")
                    }
            case .waitingForNotification:
                guard state["notificationOpenObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "notification-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkConfirmDialogOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForConfirmDialog,
                            failureReason: "confirm-dialog-open-dispatch-failed")
                    }
            case .waitingForConfirmDialog:
                guard state["confirmDialogOpenObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "confirm-dialog-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkConfirmDialogConfirmAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForConfirmDialogConfirm,
                            failureReason: "confirm-dialog-confirm-dispatch-failed")
                    }
            case .waitingForConfirmDialogConfirm:
                guard state["confirmDialogConfirmObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "confirm-dialog-confirm-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkConfirmDialogOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForConfirmDialogCancelOpen,
                            failureReason: "confirm-dialog-cancel-open-dispatch-failed")
                    }
            case .waitingForConfirmDialogCancelOpen:
                guard state["confirmDialogOpenObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "confirm-dialog-cancel-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkConfirmDialogCancelAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForConfirmDialogCancel,
                            failureReason: "confirm-dialog-cancel-dispatch-failed")
                    }
            case .waitingForConfirmDialogCancel:
                guard state["confirmDialogCancelObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "confirm-dialog-cancel-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkConfirmDialogOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForConfirmDialogRejectOpen,
                            failureReason: "confirm-dialog-reject-open-dispatch-failed")
                    }
            case .waitingForConfirmDialogRejectOpen:
                guard state["confirmDialogOpenObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "confirm-dialog-reject-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkConfirmDialogRejectAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForConfirmDialogReject,
                            failureReason: "confirm-dialog-reject-dispatch-failed")
                    }
            case .waitingForConfirmDialogReject:
                guard state["confirmDialogRejectObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "confirm-dialog-reject-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkProgressBarValueAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForProgressBarValue,
                            failureReason: "progress-bar-value-dispatch-failed")
                    }
            case .waitingForProgressBarValue:
                guard state["progressBarValueObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "progress-bar-value-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkProgressBarIndeterminateAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForProgressBarIndeterminate,
                            failureReason: "progress-bar-indeterminate-dispatch-failed")
                    }
            case .waitingForProgressBarIndeterminate:
                guard state["progressBarIndeterminateObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "progress-bar-indeterminate-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkUploadAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForUploadSubmit,
                            failureReason: "upload-submit-dispatch-failed")
                    }
            case .waitingForUploadSubmit:
                guard self.standardFrameworkString(state, "uploadStatus")
                    .contains("Multipart upload received: flow-ipc-upload.txt"),
                      self.standardFrameworkString(state, "uploadStatus")
                    .contains("marker=true") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "upload-submit-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkResourceFetchAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForResourceFetch,
                            failureReason: "resource-fetch-dispatch-failed")
                    }
            case .waitingForResourceFetch:
                guard state["resourceFetchComplete"] as? Bool == true,
                      state["streamResourceFetchOk"] as? Bool == true,
                      state["binaryStreamResourceFetchOk"] as? Bool == true,
                      state["downloadHandlerFetchOk"] as? Bool == true,
                      state["standardStreamingDownloadFetchOk"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "resource-fetch-not-observed",
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
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginOverlayFullI18nAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLoginOverlayFullI18n,
                            failureReason: "login-overlay-full-i18n-dispatch-failed")
                    }
            }
        }
    }

    private func finishSuccessfulStandardBundleFrameworkAcceptance(
        _ context: FlowIpcWindowContext,
        state: [String: Any]
    ) {
        finishStandardBundleFrameworkAcceptance(
            context,
            success: true,
            reason: "complete",
            state: state,
            additionalDetails: [
                "budgetEventObserved": true,
                "checkboxEventObserved": true,
                "checkboxGroupClientObserved": true,
                "checkboxGroupServerObserved": true,
                "checkboxGroupStatus": "CheckboxGroup server value: qa,release",
                "selectEventObserved": true,
                "comboBoxEventObserved": true,
                "datePickerEventObserved": true,
                "radioGroupEventObserved": true,
                "textFieldClientObserved": true,
                "textFieldServerObserved": true,
                "textFieldStatus": "TextField server value: Server issued title",
                "textAreaEventObserved": true,
                "emailFieldEventObserved": true,
                "integerFieldEventObserved": true,
                "passwordFieldEventObserved": true,
                "timePickerEventObserved": true,
                "multiSelectComboBoxEventObserved": true,
                "reviewerStatus": "Reviewer changed: audit",
                "inputMatrixStatus": "Review date-time changed: 2026-08-01T16:20",
                "textAreaStatus": "Notes changed: Flow IPC notes updated",
                "emailFieldStatus": "Email changed: owner+ipc@example.test",
                "integerFieldStatus": "Score changed: 7",
                "numberFieldClientObserved": true,
                "numberFieldServerObserved": true,
                "numberFieldStatus": "NumberField server value: 8.25",
                "passwordFieldStatus": "Secret changed: length=10",
                "timePickerStatus": "Review time changed: 14:45",
                "assigneesStatus": "Assignees changed: ava,sol",
                "dialogEventObserved": true,
                "formValidationObserved": true,
                "formLayoutClickObserved": true,
                "formLayoutServerObserved": true,
                "formLayoutStatus":
                    "FormLayout server layout: max=3 colspan=3 label=112px column=24px",
                "horizontalLayoutClickObserved": true,
                "horizontalLayoutServerObserved": true,
                "horizontalLayoutStatus":
                    "HorizontalLayout server layout: evenly stretch grow=2 tail=baseline",
                "flexLayoutClickObserved": true,
                "flexLayoutServerObserved": true,
                "flexLayoutStatus":
                    "FlexLayout server layout: column-reverse wrap-reverse basis=180 order=3",
                "verticalLayoutClickObserved": true,
                "verticalLayoutServerObserved": true,
                "verticalLayoutStatus":
                    "VerticalLayout server layout: evenly stretch grow=2 body=center",
                "scrollerClientObserved": true,
                "scrollerServerObserved": true,
                "scrollerStatus":
                    "Scroller server layout: horizontal patched width=340",
                "avatarClickObserved": true,
                "messageInputSubmitObserved": true,
                "richTextEditorEditObserved": true,
                "richTextEditorStatus": "Rich text changed: Flow IPC rich text bold=true",
                "richTextEditorBehaviorStatus":
                    "Rich text behavior delta/html: Flow IPC rich text bold=true",
                "loginOverlaySubmitObserved": true,
                "loginOverlayForgotPasswordObserved": true,
                "loginOverlayShowErrorObserved": true,
                "loginOverlayStatus": "Login submitted: ipc-user password-length=8",
                "loginFormSubmitObserved": true,
                "loginFormForgotPasswordObserved": true,
                "loginFormStatus": standardFrameworkString(state, "loginFormStatus"),
                "loginOverlayActionPostSubmitObserved": true,
                "loginOverlayActionPostStatus": "LoginOverlay action post submitted: overlay-action-user password-length=8 action=flow-ipc-login-overlay-action",
                "loginFormActionPostSubmitObserved": true,
                "loginFormActionPostStatus": "LoginForm action post submitted: inline-action-user password-length=8 action=flow-ipc-login-form-action",
                "loginOverlaySubmitButtonObserved": true,
                "loginOverlaySubmitButtonStatus":
                    "LoginOverlay submit button submitted: overlay-button-user password-length=8",
                "loginFormSubmitButtonObserved": true,
                "loginFormButtonStatus":
                    "LoginForm button submitted: button-user password-length=8",
                "loginFormShowErrorObserved": true,
                "loginFormShowErrorStatus": "LoginForm show error rejected: rejected-user password-length=8 title=Desktop session expired",
                "loginFormEnabledLifecycleObserved": true,
                "loginFormEnabledStatus":
                    "LoginForm enabled lifecycle submitted: enabled-user password-length=8 submit=1",
                "loginFormFullI18nObserved": true,
                "loginFormAutofillAttributesObserved": true,
                "loginFormA11ySemanticsObserved": true,
                "loginFormNoAutofocusObserved": true,
                "loginFormNoAutofocusStatus":
                    standardFrameworkString(state, "loginFormNoAutofocusStatus"),
                "loginOverlayAutofocusControlObserved": true,
                "loginOverlayAutofocusControlStatus":
                    "LoginOverlay autofocus control closed: true",
                "loginOverlayNoAutofocusObserved": true,
                "loginOverlayNoAutofocusStatus": "LoginOverlay no autofocus closed: true",
                "loginOverlayAutofillAttributesObserved": true,
                "loginOverlayAutofillAttributesStatus":
                    "LoginOverlay autofill attributes closed: true",
                "loginOverlayA11ySemanticsObserved": true,
                "loginOverlayA11ySemanticsStatus":
                    "LoginOverlay a11y semantics closed: true",
                "loginOverlayOutsideClickPreventedObserved": true,
                "loginOverlayOutsideCloseStatus":
                    "LoginOverlay outside close opened: false",
                "loginOverlayEscapeKeyPreventedObserved": true,
                "loginOverlayEscapeKeyStatus":
                    "LoginOverlay escape key opened: false",
                "loginOverlayFullI18nObserved": true,
                "loginOverlayFullI18nRendered": true,
                "loginOverlayFullI18nStatus": "LoginOverlay full i18n opened: true",
                "tabsSelectionObserved": true,
                "detailsOpenObserved": true,
                "accordionOpenObserved": true,
                "menuBarActionObserved": true,
                "navigationMatrixStatus": "Menu action: audit",
                "sideNavSelectionObserved": true,
                "contextMenuOpenObserved": true,
                "contextMenuItemClickObserved": true,
                "gridContextMenuOpenObserved": true,
                "gridContextMenuItemClickObserved": true,
                "lazyGridScrollObserved": true,
                "virtualListClientObserved": true,
                "virtualListServerObserved": true,
                "virtualListStatus": standardFrameworkString(state, "virtualListStatus"),
                "popoverOpenObserved": true,
                "notificationOpenObserved": true,
                "notificationStatus":
                    "Notification opened: Flow IPC notification opened: audit",
                "confirmDialogOpenObserved": true,
                "confirmDialogConfirmObserved": true,
                "confirmDialogCancelObserved": true,
                "confirmDialogRejectObserved": true,
                "confirmDialogStatus": "ConfirmDialog rejected: audit",
                "progressBarValueObserved": true,
                "progressBarIndeterminateObserved": true,
                "progressBarStatus": "ProgressBar indeterminate: true value=0.75",
                "dateTimePickerClientObserved": true,
                "dateTimePickerServerObserved": true,
                "dateTimePickerStatus": "DateTimePicker server value: 2026-08-01T16:20",
                "splitLayoutDragObserved": true,
                "splitLayoutServerObserved": true,
                "splitLayoutStatus": "SplitLayout server position: 62.5",
                "listBoxClientObserved": true,
                "listBoxServerObserved": true,
                "listBoxStatus": "ListBox server value: support",
                "multiSelectListBoxClientObserved": true,
                "multiSelectListBoxServerObserved": true,
                "multiSelectListBoxStatus":
                    "MultiSelectListBox server value: design,verify",
                "customFieldClientObserved": true,
                "customFieldServerObserved": true,
                "customFieldStatus": "CustomField server value: deploy|approved",
                "uploadSubmitObserved": true,
                "streamResourceFetchObserved": true,
                "binaryStreamResourceFetchObserved": true,
                "downloadHandlerFetchObserved": true,
                "standardStreamingDownloadFetchObserved": true,
                "resourceFetchStatus":
                    standardFrameworkString(state, "resourceFetchStatus"),
                "standardStreamingDownloadBytes":
                    standardFrameworkInteger(state, "standardStreamingDownloadBytes"),
                "applyEventObserved": true,
                "serverStateUpdated": true,
                "nativeWindowTitleApplied": true,
                "nativeClipboardApplied": true
            ])
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
          const findDeep = (predicate) => {
            const visit = (node) => {
              if (!node) return null;
              if (node.nodeType === 1 && predicate(node)) return node;
              if (node.shadowRoot) {
                const shadowResult = visit(node.shadowRoot);
                if (shadowResult) return shadowResult;
              }
              const children = node.children || node.childNodes || [];
              for (const child of children) {
                const result = visit(child);
                if (result) return result;
              }
              return null;
            };
            return visit(document);
          };
          const findDeepFrom = (rootNode, predicate) => {
            const visit = (node) => {
              if (!node) return null;
              if (node.nodeType === 1 && predicate(node)) return node;
              if (node.shadowRoot) {
                const shadowResult = visit(node.shadowRoot);
                if (shadowResult) return shadowResult;
              }
              const children = node.children || node.childNodes || [];
              for (const child of children) {
                const result = visit(child);
                if (result) return result;
              }
              return null;
            };
            return visit(rootNode);
          };
          const collectDeepText = (rootNode) => {
            const parts = [];
            const append = (value) => {
              const text = String(value || '').replace(/\\s+/g, ' ').trim();
              if (text) parts.push(text);
            };
            const visit = (node) => {
              if (!node) return;
              if (node.nodeType === Node.TEXT_NODE) {
                append(node.textContent);
                return;
              }
              if (node.nodeType !== Node.ELEMENT_NODE
                  && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
                  && node.nodeType !== Node.DOCUMENT_NODE) {
                return;
              }
              if (node.nodeType === Node.ELEMENT_NODE) {
                for (const name of [
                  'label', 'aria-label', 'placeholder', 'title', 'value',
                  'helper-text', 'error-message'
                ]) {
                  if (node.hasAttribute?.(name)) append(node.getAttribute(name));
                }
              }
              if (node.shadowRoot) visit(node.shadowRoot);
              const children = node.childNodes || [];
              for (const child of children) visit(child);
            };
            visit(rootNode);
            return parts.join(' ').replace(/\\s+/g, ' ').trim();
          };
          const activeDeepElements = () => {
            const elements = [];
            let active = document.activeElement;
            while (active) {
              elements.push(active);
              active = active.shadowRoot?.activeElement || null;
            }
            return elements;
          };
          const contextMenu = document.querySelector('#framework-context-menu')
            || document.querySelector('vaadin-context-menu');
          const contextMenuOverlay = findDeep((element) =>
            element.localName === 'vaadin-context-menu-overlay');
          const contextMenuItem = findDeep((element) =>
            element.id === 'framework-context-menu-audit'
              || (element.localName === 'vaadin-context-menu-item'
                && (element.textContent || '').includes('Context Audit')));
          const gridContextMenu = document.querySelector('#framework-grid-context-menu');
          const gridContextMenuOverlay = findDeep((element) =>
            element.localName === 'vaadin-context-menu-overlay'
              && (element.textContent || '').includes('Grid Audit'));
          const gridContextMenuItem = findDeep((element) =>
            element.id === 'framework-grid-context-menu-audit'
              || (element.localName === 'vaadin-context-menu-item'
                && (element.textContent || '').includes('Grid Audit')));
          const richContentStatus =
            document.querySelector('#framework-rich-content-status')?.textContent || '';
          const loginOverlay = document.querySelector('#framework-login-overlay');
          const loginOverlayUsernameField = loginOverlay && findDeepFrom(loginOverlay, (element) =>
            element.localName === 'vaadin-text-field');
          const loginOverlayPasswordField = loginOverlay && findDeepFrom(loginOverlay, (element) =>
            element.localName === 'vaadin-password-field');
          const loginOverlaySubmit = loginOverlay && findDeepFrom(loginOverlay, (element) =>
            (element.localName === 'vaadin-button'
              && (element.textContent || '').includes('Sign in'))
              || (element.localName === 'button'
                && ((element.type || '').toLowerCase() === 'submit'
                  || (element.textContent || '').includes('Sign in'))));
          const loginOverlayForgot = loginOverlay && findDeepFrom(loginOverlay, (element) => {
            if (element.localName !== 'vaadin-button' && element.localName !== 'button') {
              return false;
            }
            const text = collectDeepText(element);
            return text.includes('Recover Flow IPC access')
              || text.toLowerCase().includes('forgot');
          });
          const loginOverlayForgotPasswordObserved =
            richContentStatus.includes('Login forgot password requested');
          const loginOverlayShowErrorWrapper = findDeep((element) => {
            if (element.localName !== 'vaadin-login-overlay-wrapper'
                && element.localName !== 'vaadin-login-form-wrapper'
                && element.localName !== 'vaadin-login-form') {
              return false;
            }
            return collectDeepText(element).includes('Desktop overlay session expired');
          });
          const loginOverlayShowErrorTextRaw = [
            loginOverlay ? collectDeepText(loginOverlay) : '',
            loginOverlayShowErrorWrapper ? collectDeepText(loginOverlayShowErrorWrapper) : ''
          ].join(' ');
          const loginOverlayShowErrorText = loginOverlayShowErrorTextRaw.trim()
            || collectDeepText(document.body);
          const loginOverlayShowErrorObserved =
            richContentStatus.includes(
              'Login overlay show error rejected: overlay-show-error password-length=8 title=Desktop overlay session expired')
            && loginOverlayShowErrorText.includes('Desktop overlay session expired')
            && loginOverlayShowErrorText.includes(
              'Re-authenticate in the overlay before continuing.');
          const loginForm = document.querySelector('#framework-login-form');
          const loginFormStatus =
            document.querySelector('#framework-login-form-status')?.textContent || '';
          const loginFormUsernameField = loginForm && findDeepFrom(loginForm, (element) =>
            element.localName === 'vaadin-text-field');
          const loginFormPasswordField = loginForm && findDeepFrom(loginForm, (element) =>
            element.localName === 'vaadin-password-field');
          const loginFormSubmit = loginForm && findDeepFrom(loginForm, (element) =>
            (element.localName === 'vaadin-button'
              && (element.textContent || '').includes('Sign in'))
              || (element.localName === 'button'
                && ((element.type || '').toLowerCase() === 'submit'
                  || (element.textContent || '').includes('Sign in'))));
          const loginFormForgot = loginForm && findDeepFrom(loginForm, (element) => {
            if (element.localName !== 'vaadin-button' && element.localName !== 'button') {
              return false;
            }
            const text = collectDeepText(element);
            return text.includes('Recover Flow IPC access')
              || text.toLowerCase().includes('forgot');
          });
          const loginFormForgotPasswordObserved =
            loginFormStatus.includes('LoginForm forgot password requested');
          const loginFormButtonStatus =
            document.querySelector('#framework-login-form-button-status')?.textContent || '';
          const loginFormSubmitButtonObserved = loginFormButtonStatus.includes(
            'LoginForm button submitted: button-user password-length=8');
          const activeElements = activeDeepElements();
          const loginFormNoAutofocus =
            document.querySelector('#framework-login-form-no-autofocus');
          const loginFormNoAutofocusStatus =
            document.querySelector('#framework-login-form-no-autofocus-status')?.textContent || '';
          const loginFormNoAutofocusProperty = loginFormNoAutofocus?.noAutofocus === true
            && loginFormNoAutofocus?.hasAttribute('no-autofocus') === true;
          const loginFormNoAutofocusUsernameField = loginFormNoAutofocus
            && findDeepFrom(loginFormNoAutofocus, (element) =>
              element.localName === 'vaadin-text-field'
                && element.id === 'vaadinLoginUsername');
          const loginFormNoAutofocusPasswordField = loginFormNoAutofocus
            && findDeepFrom(loginFormNoAutofocus, (element) =>
              element.localName === 'vaadin-password-field'
                && element.id === 'vaadinLoginPassword');
          const loginFormNoAutofocusUsernameInput = loginFormNoAutofocusUsernameField
            && findDeepFrom(loginFormNoAutofocusUsernameField, (element) =>
              element.localName === 'input');
          const loginFormNoAutofocusPasswordInput = loginFormNoAutofocusPasswordField
            && findDeepFrom(loginFormNoAutofocusPasswordField, (element) =>
              element.localName === 'input');
          const loginFormNoAutofocusFieldFocused = [
            loginFormNoAutofocusUsernameField,
            loginFormNoAutofocusPasswordField,
            loginFormNoAutofocusUsernameInput,
            loginFormNoAutofocusPasswordInput
          ].some((element) => !!element && activeElements.includes(element));
          const loginFormNoAutofocusObserved = !!loginFormNoAutofocus
            && loginFormNoAutofocusProperty
            && !!loginFormNoAutofocusUsernameField
            && !!loginFormNoAutofocusPasswordField
            && !!loginFormNoAutofocusUsernameInput
            && !!loginFormNoAutofocusPasswordInput
            && loginFormNoAutofocusStatus.includes('LoginForm no autofocus rendered:')
            && !loginFormNoAutofocusFieldFocused;
          const loginOverlayActionPost = document.querySelector('#framework-login-overlay-action-post');
          const loginOverlayActionPostUsernameField = loginOverlayActionPost
            && findDeepFrom(loginOverlayActionPost, (element) =>
              element.localName === 'vaadin-text-field');
          const loginOverlayActionPostPasswordField = loginOverlayActionPost
            && findDeepFrom(loginOverlayActionPost, (element) =>
              element.localName === 'vaadin-password-field');
          const loginOverlayActionPostSubmit = loginOverlayActionPost
            && findDeepFrom(loginOverlayActionPost, (element) =>
              (element.localName === 'vaadin-button'
                && (element.textContent || '').includes('Sign in'))
                || (element.localName === 'button'
                  && ((element.type || '').toLowerCase() === 'submit'
                    || (element.textContent || '').includes('Sign in'))));
          const loginOverlayNoAutofocus =
            document.querySelector('#framework-login-overlay-no-autofocus');
          const loginOverlayNoAutofocusStatus =
            document.querySelector('#framework-login-overlay-no-autofocus-status')?.textContent || '';
          const loginOverlayNoAutofocusOpened = loginOverlayNoAutofocus?.opened === true
            || loginOverlayNoAutofocus?.hasAttribute('opened') === true;
          const loginOverlayNoAutofocusClosed = !!loginOverlayNoAutofocus
            && !loginOverlayNoAutofocusOpened
            && loginOverlayNoAutofocusStatus.includes('LoginOverlay no autofocus closed: true');
          const loginOverlayNoAutofocusProperty = loginOverlayNoAutofocus?.noAutofocus === true
            && loginOverlayNoAutofocus?.hasAttribute('no-autofocus') === true;
          const loginOverlayNoAutofocusUsernameField = loginOverlayNoAutofocus
            && findDeepFrom(loginOverlayNoAutofocus, (element) =>
              element.localName === 'vaadin-text-field'
                && element.id === 'vaadinLoginUsername');
          const loginOverlayNoAutofocusPasswordField = loginOverlayNoAutofocus
            && findDeepFrom(loginOverlayNoAutofocus, (element) =>
              element.localName === 'vaadin-password-field'
                && element.id === 'vaadinLoginPassword');
          const loginOverlayNoAutofocusUsernameInput = loginOverlayNoAutofocusUsernameField
            && findDeepFrom(loginOverlayNoAutofocusUsernameField, (element) =>
              element.localName === 'input');
          const loginOverlayNoAutofocusPasswordInput = loginOverlayNoAutofocusPasswordField
            && findDeepFrom(loginOverlayNoAutofocusPasswordField, (element) =>
              element.localName === 'input');
          const loginOverlayNoAutofocusFieldFocused = [
            loginOverlayNoAutofocusUsernameField,
            loginOverlayNoAutofocusPasswordField,
            loginOverlayNoAutofocusUsernameInput,
            loginOverlayNoAutofocusPasswordInput
          ].some((element) => !!element && activeElements.includes(element));
          const loginOverlayNoAutofocusObserved = loginOverlayNoAutofocusOpened
            && loginOverlayNoAutofocusProperty
            && !!loginOverlayNoAutofocusUsernameField
            && !!loginOverlayNoAutofocusPasswordField
            && !!loginOverlayNoAutofocusUsernameInput
            && !!loginOverlayNoAutofocusPasswordInput
            && loginOverlayNoAutofocusStatus.includes('LoginOverlay no autofocus opened: true')
            && !loginOverlayNoAutofocusFieldFocused;
          const loginOverlayAutofocusControl =
            document.querySelector('#framework-login-overlay-autofocus-control');
          const loginOverlayAutofocusControlStatus =
            document.querySelector('#framework-login-overlay-autofocus-control-status')?.textContent || '';
          const loginOverlayAutofocusControlOpened =
            loginOverlayAutofocusControl?.opened === true
              || loginOverlayAutofocusControl?.hasAttribute('opened') === true;
          const loginOverlayAutofocusControlClosed = !!loginOverlayAutofocusControl
            && !loginOverlayAutofocusControlOpened
            && loginOverlayAutofocusControlStatus.includes(
              'LoginOverlay autofocus control closed: true');
          const loginOverlayAutofocusControlProperty =
            loginOverlayAutofocusControl?.noAutofocus !== true
              && loginOverlayAutofocusControl?.hasAttribute('no-autofocus') !== true;
          const loginOverlayAutofocusControlUsernameField = loginOverlayAutofocusControl
            && findDeepFrom(loginOverlayAutofocusControl, (element) =>
              element.localName === 'vaadin-text-field'
                && element.id === 'vaadinLoginUsername');
          const loginOverlayAutofocusControlPasswordField = loginOverlayAutofocusControl
            && findDeepFrom(loginOverlayAutofocusControl, (element) =>
              element.localName === 'vaadin-password-field'
                && element.id === 'vaadinLoginPassword');
          const loginOverlayAutofocusControlUsernameInput =
            loginOverlayAutofocusControlUsernameField
              && findDeepFrom(loginOverlayAutofocusControlUsernameField, (element) =>
                element.localName === 'input');
          const loginOverlayAutofocusControlPasswordInput =
            loginOverlayAutofocusControlPasswordField
              && findDeepFrom(loginOverlayAutofocusControlPasswordField, (element) =>
                element.localName === 'input');
          const loginOverlayAutofocusControlUsernameFocused = [
            loginOverlayAutofocusControlUsernameField,
            loginOverlayAutofocusControlUsernameInput
          ].some((element) => !!element && activeElements.includes(element))
            || loginOverlayAutofocusControlUsernameField?.hasAttribute('focused') === true
            || loginOverlayAutofocusControlUsernameInput?.matches(':focus') === true;
          const loginOverlayAutofocusControlActiveElementPath = activeElements
            .map((element) => {
              const id = element.id ? `#${element.id}` : '';
              return `${element.localName || element.nodeName}${id}`;
            })
            .join(' > ');
          const loginOverlayAutofocusControlObserved =
            loginOverlayAutofocusControlOpened
              && loginOverlayAutofocusControlProperty
              && !!loginOverlayAutofocusControlUsernameField
              && !!loginOverlayAutofocusControlPasswordField
              && !!loginOverlayAutofocusControlUsernameInput
              && !!loginOverlayAutofocusControlPasswordInput
              && loginOverlayAutofocusControlStatus.includes(
                'LoginOverlay autofocus control opened: true');
          const loginOverlayOutsideClose =
            document.querySelector('#framework-login-overlay-outside-close');
          const loginOverlayOutsideCloseStatus =
            document.querySelector('#framework-login-overlay-outside-close-status')?.textContent || '';
          const loginOverlayOutsideCloseOpened = loginOverlayOutsideClose?.opened === true
            || loginOverlayOutsideClose?.hasAttribute('opened') === true;
          const loginOverlayOutsideClosed = !!loginOverlayOutsideClose
            && !loginOverlayOutsideCloseOpened
            && loginOverlayOutsideCloseStatus.includes(
              'LoginOverlay outside close opened: false');
          const loginOverlayOutsideClick =
            window.VaadinDesktopFrameworkLoginOverlayOutsideClick || {};
          const loginOverlayOutsideCloseUsernameField = loginOverlayOutsideClose
            && findDeepFrom(loginOverlayOutsideClose, (element) =>
              element.localName === 'vaadin-text-field'
                && element.id === 'vaadinLoginUsername');
          const loginOverlayOutsideClosePasswordField = loginOverlayOutsideClose
            && findDeepFrom(loginOverlayOutsideClose, (element) =>
              element.localName === 'vaadin-password-field'
                && element.id === 'vaadinLoginPassword');
          const loginOverlayOutsideCloseUsernameInput = loginOverlayOutsideCloseUsernameField
            && findDeepFrom(loginOverlayOutsideCloseUsernameField, (element) =>
              element.localName === 'input');
          const loginOverlayOutsideClosePasswordInput = loginOverlayOutsideClosePasswordField
            && findDeepFrom(loginOverlayOutsideClosePasswordField, (element) =>
              element.localName === 'input');
          const loginOverlayOutsideCloseObserved =
            loginOverlayOutsideCloseOpened
              && !!loginOverlayOutsideCloseUsernameField
              && !!loginOverlayOutsideClosePasswordField
              && !!loginOverlayOutsideCloseUsernameInput
              && !!loginOverlayOutsideClosePasswordInput
              && loginOverlayOutsideCloseStatus.includes(
                'LoginOverlay outside close opened: true');
          const loginOverlayOutsideClickPreventedObserved =
            loginOverlayOutsideClick.preventedObserved === true
              || (loginOverlayOutsideCloseObserved
                && loginOverlayOutsideClick.dispatched === true
                && loginOverlayOutsideClick.eventObserved === true
                && loginOverlayOutsideClick.defaultPrevented === true
                && loginOverlayOutsideCloseOpened
                && !loginOverlayOutsideClosed
                && !loginOverlayOutsideCloseStatus.includes(
                  'LoginOverlay outside close opened: false'));
          const loginOverlayEscapeKey =
            document.querySelector('#framework-login-overlay-escape-key');
          const loginOverlayEscapeKeyStatus =
            document.querySelector('#framework-login-overlay-escape-key-status')?.textContent || '';
          const loginOverlayEscapeKeyOpened = loginOverlayEscapeKey?.opened === true
            || loginOverlayEscapeKey?.hasAttribute('opened') === true;
          const loginOverlayEscapeKeyClosed = !!loginOverlayEscapeKey
            && !loginOverlayEscapeKeyOpened
            && loginOverlayEscapeKeyStatus.includes(
              'LoginOverlay escape key opened: false');
          const loginOverlayEscapeKeyMarker =
            window.VaadinDesktopFrameworkLoginOverlayEscapeKey || {};
          const loginOverlayEscapeKeyUsernameField = loginOverlayEscapeKey
            && findDeepFrom(loginOverlayEscapeKey, (element) =>
              element.localName === 'vaadin-text-field'
                && element.id === 'vaadinLoginUsername');
          const loginOverlayEscapeKeyPasswordField = loginOverlayEscapeKey
            && findDeepFrom(loginOverlayEscapeKey, (element) =>
              element.localName === 'vaadin-password-field'
                && element.id === 'vaadinLoginPassword');
          const loginOverlayEscapeKeyUsernameInput = loginOverlayEscapeKeyUsernameField
            && findDeepFrom(loginOverlayEscapeKeyUsernameField, (element) =>
              element.localName === 'input');
          const loginOverlayEscapeKeyPasswordInput = loginOverlayEscapeKeyPasswordField
            && findDeepFrom(loginOverlayEscapeKeyPasswordField, (element) =>
              element.localName === 'input');
          const loginOverlayEscapeKeyObserved =
            loginOverlayEscapeKeyOpened
              && !!loginOverlayEscapeKeyUsernameField
              && !!loginOverlayEscapeKeyPasswordField
              && !!loginOverlayEscapeKeyUsernameInput
              && !!loginOverlayEscapeKeyPasswordInput
              && loginOverlayEscapeKeyStatus.includes(
                'LoginOverlay escape key opened: true');
          const loginOverlayEscapeKeyPreventedObserved =
            loginOverlayEscapeKeyMarker.preventedObserved === true
              || (loginOverlayEscapeKeyObserved
                && loginOverlayEscapeKeyMarker.dispatched === true
                && loginOverlayEscapeKeyMarker.eventObserved === true
                && loginOverlayEscapeKeyMarker.defaultPrevented === true
                && loginOverlayEscapeKeyOpened
                && !loginOverlayEscapeKeyClosed
                && !loginOverlayEscapeKeyStatus.includes(
                  'LoginOverlay escape key opened: false'));
          const loginOverlayAutofillAttributes =
            document.querySelector('#framework-login-overlay-autofill-attributes');
          const loginOverlayAutofillAttributesStatus =
            document.querySelector('#framework-login-overlay-autofill-attributes-status')?.textContent || '';
          const loginOverlayAutofillAttributesOpened =
            loginOverlayAutofillAttributes?.opened === true
              || loginOverlayAutofillAttributes?.hasAttribute('opened') === true;
          const loginOverlayAutofillAttributesClosed = !!loginOverlayAutofillAttributes
            && !loginOverlayAutofillAttributesOpened
            && loginOverlayAutofillAttributesStatus.includes(
              'LoginOverlay autofill attributes closed: true');
          const loginOverlayAutofillAttributesUsernameField =
            loginOverlayAutofillAttributes
              && findDeepFrom(loginOverlayAutofillAttributes, (element) =>
                element.localName === 'vaadin-text-field'
                  && element.id === 'vaadinLoginUsername');
          const loginOverlayAutofillAttributesPasswordField =
            loginOverlayAutofillAttributes
              && findDeepFrom(loginOverlayAutofillAttributes, (element) =>
                element.localName === 'vaadin-password-field'
                  && element.id === 'vaadinLoginPassword');
          const loginOverlayAutofillAttributesUsernameInput =
            loginOverlayAutofillAttributesUsernameField
              && findDeepFrom(loginOverlayAutofillAttributesUsernameField, (element) =>
                element.localName === 'input');
          const loginOverlayAutofillAttributesPasswordInput =
            loginOverlayAutofillAttributesPasswordField
              && findDeepFrom(loginOverlayAutofillAttributesPasswordField, (element) =>
                element.localName === 'input');
          const loginOverlayAutofillUsernameAutocomplete = String(
            loginOverlayAutofillAttributesUsernameInput?.getAttribute('autocomplete')
              || loginOverlayAutofillAttributesUsernameField?.getAttribute('autocomplete')
              || '');
          const loginOverlayAutofillPasswordAutocomplete = String(
            loginOverlayAutofillAttributesPasswordInput?.getAttribute('autocomplete')
              || loginOverlayAutofillAttributesPasswordField?.getAttribute('autocomplete')
              || '');
          const loginOverlayAutofillAttributesObserved =
            loginOverlayAutofillAttributesOpened
              && !!loginOverlayAutofillAttributesUsernameField
              && !!loginOverlayAutofillAttributesPasswordField
              && !!loginOverlayAutofillAttributesUsernameInput
              && !!loginOverlayAutofillAttributesPasswordInput
              && loginOverlayAutofillAttributesStatus.includes(
                'LoginOverlay autofill attributes opened: true')
              && loginOverlayAutofillUsernameAutocomplete.toLowerCase().includes('username')
              && loginOverlayAutofillPasswordAutocomplete.toLowerCase().includes('password');
          const loginOverlayA11ySemantics =
            document.querySelector('#framework-login-overlay-a11y-semantics');
          const loginOverlayA11ySemanticsStatus =
            document.querySelector('#framework-login-overlay-a11y-semantics-status')?.textContent || '';
          const loginOverlayA11ySemanticsOpened =
            loginOverlayA11ySemantics?.opened === true
              || loginOverlayA11ySemantics?.hasAttribute('opened') === true;
          const loginOverlayA11ySemanticsClosed = !!loginOverlayA11ySemantics
            && !loginOverlayA11ySemanticsOpened
            && loginOverlayA11ySemanticsStatus.includes(
              'LoginOverlay a11y semantics closed: true');
          const loginOverlayA11yWrapper = findDeep((element) => {
            if (element.localName !== 'vaadin-login-overlay-wrapper'
                && element.localName !== 'vaadin-login-form-wrapper'
                && element.localName !== 'vaadin-login-form') {
              return false;
            }
            const text = collectDeepText(element);
            return text.includes('Flow IPC LoginOverlay a11y semantics')
              || text.includes('Flow IPC access');
          });
          const findOverlayA11yDeep = (predicate) => {
            const roots = [
              loginOverlayA11ySemantics,
              loginOverlayA11yWrapper,
              document.body
            ].filter(Boolean);
            for (const candidate of roots) {
              const found = findDeepFrom(candidate, predicate);
              if (found) return found;
            }
            return null;
          };
          const loginOverlayA11yText = [
            loginOverlayA11ySemantics ? collectDeepText(loginOverlayA11ySemantics) : '',
            loginOverlayA11yWrapper ? collectDeepText(loginOverlayA11yWrapper) : ''
          ].join(' ').replace(/\\s+/g, ' ').trim();
          const loginOverlayA11yUsernameField = findOverlayA11yDeep((element) =>
            element.localName === 'vaadin-text-field'
              && element.id === 'vaadinLoginUsername');
          const loginOverlayA11yPasswordField = findOverlayA11yDeep((element) =>
            element.localName === 'vaadin-password-field'
              && element.id === 'vaadinLoginPassword');
          const loginOverlayA11yUsernameInput = loginOverlayA11yUsernameField
            && findDeepFrom(loginOverlayA11yUsernameField, (element) =>
              element.localName === 'input');
          const loginOverlayA11yPasswordInput = loginOverlayA11yPasswordField
            && findDeepFrom(loginOverlayA11yPasswordField, (element) =>
              element.localName === 'input');
          const loginOverlayA11ySubmit = findOverlayA11yDeep((element) =>
            (element.localName === 'vaadin-button' || element.localName === 'button')
              && collectDeepText(element).includes('Sign in'));
          const loginOverlayA11yForgot = findOverlayA11yDeep((element) =>
            (element.localName === 'vaadin-button' || element.localName === 'button')
              && collectDeepText(element).includes('Recover Flow IPC access'));
          const loginOverlayA11yForm = findOverlayA11yDeep((element) =>
            element.localName === 'form');
          const loginOverlayA11yUsernameLabel = String(
            loginOverlayA11yUsernameField?.getAttribute('label')
              || loginOverlayA11yUsernameField?.label
              || loginOverlayA11yUsernameInput?.getAttribute('aria-label')
              || '');
          const loginOverlayA11yPasswordLabel = String(
            loginOverlayA11yPasswordField?.getAttribute('label')
              || loginOverlayA11yPasswordField?.label
              || loginOverlayA11yPasswordInput?.getAttribute('aria-label')
              || '');
          const loginOverlayA11yUsernameName = String(
            loginOverlayA11yUsernameInput?.getAttribute('name')
              || loginOverlayA11yUsernameField?.getAttribute('name')
              || loginOverlayA11yUsernameField?.name
              || '');
          const loginOverlayA11yPasswordName = String(
            loginOverlayA11yPasswordInput?.getAttribute('name')
              || loginOverlayA11yPasswordField?.getAttribute('name')
              || loginOverlayA11yPasswordField?.name
              || '');
          const loginOverlayA11yPasswordInputType = String(
            loginOverlayA11yPasswordInput?.getAttribute('type')
              || loginOverlayA11yPasswordInput?.type
              || '');
          const loginOverlayA11ySemanticsObserved =
            loginOverlayA11ySemanticsOpened
              && !!loginOverlayA11yWrapper
              && !!loginOverlayA11yForm
              && !!loginOverlayA11yUsernameField
              && !!loginOverlayA11yPasswordField
              && !!loginOverlayA11yUsernameInput
              && !!loginOverlayA11yPasswordInput
              && !!loginOverlayA11ySubmit
              && !!loginOverlayA11yForgot
              && loginOverlayA11ySemanticsStatus.includes(
                'LoginOverlay a11y semantics opened: true')
              && loginOverlayA11yText.includes('Flow IPC LoginOverlay a11y semantics')
              && loginOverlayA11yText.includes('Framework validation')
              && loginOverlayA11yText.includes('Flow IPC access')
              && loginOverlayA11yText.includes('User')
              && loginOverlayA11yText.includes('Password')
              && loginOverlayA11yUsernameLabel.toLowerCase().includes('user')
              && loginOverlayA11yPasswordLabel.toLowerCase().includes('password')
              && loginOverlayA11yUsernameName.toLowerCase().includes('username')
              && loginOverlayA11yPasswordName.toLowerCase().includes('password')
              && loginOverlayA11yPasswordInputType.toLowerCase() === 'password';
          const loginFormActionPost = document.querySelector('#framework-login-form-action-post');
          const loginFormActionPostUsernameField = loginFormActionPost
            && findDeepFrom(loginFormActionPost, (element) =>
              element.localName === 'vaadin-text-field');
          const loginFormActionPostPasswordField = loginFormActionPost
            && findDeepFrom(loginFormActionPost, (element) =>
              element.localName === 'vaadin-password-field');
          const loginFormActionPostSubmit = loginFormActionPost
            && findDeepFrom(loginFormActionPost, (element) =>
              (element.localName === 'vaadin-button'
                && (element.textContent || '').includes('Sign in'))
                || (element.localName === 'button'
                  && ((element.type || '').toLowerCase() === 'submit'
                    || (element.textContent || '').includes('Sign in'))));
          const loginFormAutofillAttributes =
            document.querySelector('#framework-login-form-autofill-attributes');
          const loginFormAutofillAttributesUsernameField =
            loginFormAutofillAttributes
              && findDeepFrom(loginFormAutofillAttributes, (element) =>
                element.localName === 'vaadin-text-field'
                  && element.id === 'vaadinLoginUsername');
          const loginFormAutofillAttributesPasswordField =
            loginFormAutofillAttributes
              && findDeepFrom(loginFormAutofillAttributes, (element) =>
                element.localName === 'vaadin-password-field'
                  && element.id === 'vaadinLoginPassword');
          const loginFormAutofillAttributesUsernameInput =
            loginFormAutofillAttributesUsernameField
              && findDeepFrom(loginFormAutofillAttributesUsernameField, (element) =>
                element.localName === 'input');
          const loginFormAutofillAttributesPasswordInput =
            loginFormAutofillAttributesPasswordField
              && findDeepFrom(loginFormAutofillAttributesPasswordField, (element) =>
                element.localName === 'input');
          const loginFormAutofillUsernameAutocomplete = String(
            loginFormAutofillAttributesUsernameInput?.getAttribute('autocomplete')
              || loginFormAutofillAttributesUsernameField?.getAttribute('autocomplete')
              || '');
          const loginFormAutofillPasswordAutocomplete = String(
            loginFormAutofillAttributesPasswordInput?.getAttribute('autocomplete')
              || loginFormAutofillAttributesPasswordField?.getAttribute('autocomplete')
              || '');
          const loginFormAutofillAttributesObserved =
            !!loginFormAutofillAttributes
              && !!loginFormAutofillAttributesUsernameField
              && !!loginFormAutofillAttributesPasswordField
              && !!loginFormAutofillAttributesUsernameInput
              && !!loginFormAutofillAttributesPasswordInput
              && loginFormAutofillUsernameAutocomplete.toLowerCase().includes('username')
              && loginFormAutofillPasswordAutocomplete.toLowerCase().includes('password');
          const loginFormA11ySemantics =
            document.querySelector('#framework-login-form-a11y-semantics');
          const loginFormA11ySemanticsStatus =
            document.querySelector('#framework-login-form-a11y-semantics-status')?.textContent || '';
          const loginFormA11yText = loginFormA11ySemantics
            ? collectDeepText(loginFormA11ySemantics) : '';
          const loginFormA11yUsernameField = loginFormA11ySemantics
            && findDeepFrom(loginFormA11ySemantics, (element) =>
              element.localName === 'vaadin-text-field'
                && element.id === 'vaadinLoginUsername');
          const loginFormA11yPasswordField = loginFormA11ySemantics
            && findDeepFrom(loginFormA11ySemantics, (element) =>
              element.localName === 'vaadin-password-field'
                && element.id === 'vaadinLoginPassword');
          const loginFormA11yUsernameInput = loginFormA11yUsernameField
            && findDeepFrom(loginFormA11yUsernameField, (element) =>
              element.localName === 'input');
          const loginFormA11yPasswordInput = loginFormA11yPasswordField
            && findDeepFrom(loginFormA11yPasswordField, (element) =>
              element.localName === 'input');
          const loginFormA11ySubmit = loginFormA11ySemantics
            && findDeepFrom(loginFormA11ySemantics, (element) =>
              (element.localName === 'vaadin-button' || element.localName === 'button')
                && collectDeepText(element).includes('Sign in'));
          const loginFormA11yForgot = loginFormA11ySemantics
            && findDeepFrom(loginFormA11ySemantics, (element) =>
              (element.localName === 'vaadin-button' || element.localName === 'button')
                && collectDeepText(element).includes('Recover Flow IPC access'));
          const loginFormA11yForm = loginFormA11ySemantics
            && findDeepFrom(loginFormA11ySemantics, (element) =>
              element.localName === 'form');
          const loginFormA11yUsernameLabel = String(
            loginFormA11yUsernameField?.getAttribute('label')
              || loginFormA11yUsernameField?.label
              || loginFormA11yUsernameInput?.getAttribute('aria-label')
              || '');
          const loginFormA11yPasswordLabel = String(
            loginFormA11yPasswordField?.getAttribute('label')
              || loginFormA11yPasswordField?.label
              || loginFormA11yPasswordInput?.getAttribute('aria-label')
              || '');
          const loginFormA11yUsernameName = String(
            loginFormA11yUsernameInput?.getAttribute('name')
              || loginFormA11yUsernameField?.getAttribute('name')
              || loginFormA11yUsernameField?.name
              || '');
          const loginFormA11yPasswordName = String(
            loginFormA11yPasswordInput?.getAttribute('name')
              || loginFormA11yPasswordField?.getAttribute('name')
              || loginFormA11yPasswordField?.name
              || '');
          const loginFormA11yPasswordInputType = String(
            loginFormA11yPasswordInput?.getAttribute('type')
              || loginFormA11yPasswordInput?.type
              || '');
          const loginFormA11ySemanticsObserved =
            !!loginFormA11ySemantics
              && !!loginFormA11yForm
              && !!loginFormA11yUsernameField
              && !!loginFormA11yPasswordField
              && !!loginFormA11yUsernameInput
              && !!loginFormA11yPasswordInput
              && !!loginFormA11ySubmit
              && !!loginFormA11yForgot
              && loginFormA11yText.includes('Flow IPC access')
              && loginFormA11yText.includes('User')
              && loginFormA11yText.includes('Password')
              && loginFormA11yUsernameLabel.toLowerCase().includes('user')
              && loginFormA11yPasswordLabel.toLowerCase().includes('password')
              && loginFormA11yUsernameName.toLowerCase().includes('username')
              && loginFormA11yPasswordName.toLowerCase().includes('password')
              && loginFormA11yPasswordInputType.toLowerCase() === 'password';
          const loginFormFullI18n = document.querySelector('#framework-login-form-i18n-full');
          const loginFormFullI18nText = loginFormFullI18n
            ? collectDeepText(loginFormFullI18n) : '';
          const loginFormFullI18nRendered = !!loginFormFullI18n
            && [
              'Desktop credentials',
              'Workspace user',
              'Workspace password',
              'Unlock workspace',
              'Recover workspace access',
              'Workspace login blocked',
              'Check the validation account and retry.',
              'Desktop validation keeps authentication local to this session.'
            ].every((text) => loginFormFullI18nText.includes(text));
          const loginOverlayFullI18n = document.querySelector('#framework-login-overlay-i18n-full');
          const loginOverlayFullI18nOpened = loginOverlayFullI18n?.opened === true
            || loginOverlayFullI18n?.hasAttribute('opened') === true;
          const loginOverlayFullI18nWrapper = findDeep((element) => {
            if (element.localName !== 'vaadin-login-overlay-wrapper'
                && element.localName !== 'vaadin-login-form-wrapper'
                && element.localName !== 'vaadin-login-form') {
              return false;
            }
            return collectDeepText(element).includes('Flow IPC secure workspace');
          });
          const loginOverlayFullI18nTextRaw = [
            loginOverlayFullI18n ? collectDeepText(loginOverlayFullI18n) : '',
            loginOverlayFullI18nWrapper ? collectDeepText(loginOverlayFullI18nWrapper) : ''
          ].join(' ');
          const loginOverlayFullI18nText = loginOverlayFullI18nTextRaw.trim()
            || (loginOverlayFullI18nOpened ? collectDeepText(document.body) : '');
          const loginOverlayFullI18nStatus =
            document.querySelector('#framework-login-overlay-i18n-full-status')?.textContent || '';
          const loginOverlayFullI18nRendered = loginOverlayFullI18nOpened
            && loginOverlayFullI18nStatus.includes('LoginOverlay full i18n opened: true')
            && [
              'Flow IPC secure workspace',
              'Use your desktop validation identity.',
              'Desktop credentials',
              'Workspace user',
              'Workspace password',
              'Unlock workspace',
              'Recover workspace access',
              'Workspace login blocked',
              'Check the validation account and retry.',
              'Desktop validation keeps authentication local to this session.'
            ].every((text) => loginOverlayFullI18nText.includes(text));
          const notificationStatus =
            document.querySelector('#framework-notification-status')?.textContent || '';
          const notification = findDeep((element) =>
            element.localName === 'vaadin-notification');
          const notificationCard = findDeep((element) =>
            element.localName === 'vaadin-notification-card'
              && collectDeepText(element).includes(
                'Flow IPC notification opened: audit'));
          const notificationOpened = notification?.opened === true
            || notification?.hasAttribute('opened') === true
            || !!notificationCard;
          const notificationText = [
            notification ? collectDeepText(notification) : '',
            notificationCard ? collectDeepText(notificationCard) : ''
          ].join(' ').trim();
          const notificationOpenObserved =
            notificationOpened
              && notificationText.includes('Flow IPC notification opened: audit')
              && notificationStatus.includes(
                'Notification opened: Flow IPC notification opened: audit')
              && (document.querySelector('#framework-overlay-matrix-status')?.textContent || '')
                .includes('Notification opened: true');
          const confirmDialogStatus =
            document.querySelector('#framework-confirm-dialog-status')?.textContent || '';
          const confirmDialog = document.querySelector('#framework-confirm-dialog');
          const confirmDialogOverlay = findDeep((element) =>
            element.localName === 'vaadin-confirm-dialog-overlay'
              || (element.localName === 'vaadin-dialog-overlay'
                && collectDeepText(element).includes(
                  'Confirm audit action through Flow IPC')));
          const confirmDialogOpened = confirmDialog?.opened === true
            || confirmDialog?.hasAttribute('opened') === true
            || !!confirmDialogOverlay;
          const confirmDialogText = [
            confirmDialog ? collectDeepText(confirmDialog) : '',
            confirmDialogOverlay ? collectDeepText(confirmDialogOverlay) : ''
          ].join(' ').trim();
          const confirmDialogOpenObserved =
            confirmDialogOpened
              && confirmDialogStatus.includes('ConfirmDialog opened: requested')
              && [
                'Flow IPC confirm',
                'Confirm audit action through Flow IPC',
                'Confirm audit',
                'Cancel audit',
                'Reject audit'
              ].every((text) => confirmDialogText.includes(text));
          const confirmDialogConfirmObserved =
            confirmDialogStatus.includes('ConfirmDialog confirmed: audit')
              && (document.querySelector('#framework-overlay-matrix-status')?.textContent || '')
                .includes('ConfirmDialog confirmed: true');
          const confirmDialogCancelObserved =
            confirmDialogStatus.includes('ConfirmDialog canceled: audit')
              && (document.querySelector('#framework-overlay-matrix-status')?.textContent || '')
                .includes('ConfirmDialog canceled: true');
          const confirmDialogRejectObserved =
            confirmDialogStatus.includes('ConfirmDialog rejected: audit')
              && (document.querySelector('#framework-overlay-matrix-status')?.textContent || '')
                .includes('ConfirmDialog rejected: true');
          const progressBar = document.querySelector('#framework-progress-bar');
          const progressBarStatus =
            document.querySelector('#framework-progress-bar-status')?.textContent || '';
          const progressBarValue = Number(
            progressBar?.value ?? progressBar?.getAttribute?.('value') ?? 0);
          const progressBarMin = Number(
            progressBar?.min ?? progressBar?.getAttribute?.('min') ?? 0);
          const progressBarMax = Number(
            progressBar?.max ?? progressBar?.getAttribute?.('max') ?? 1);
          const progressBarIndeterminate = progressBar?.indeterminate === true
            || progressBar?.hasAttribute?.('indeterminate') === true;
          const progressBarValueObserved =
            progressBarStatus.includes('ProgressBar value: 0.75')
              && Math.abs(progressBarValue - 0.75) < 0.001
              && Math.abs(progressBarMin - 0) < 0.001
              && Math.abs(progressBarMax - 1) < 0.001
              && !progressBarIndeterminate;
          const progressBarIndeterminateObserved =
            progressBarStatus.includes('ProgressBar indeterminate: true')
              && Math.abs(progressBarValue - 0.75) < 0.001
              && progressBarIndeterminate;
          const inputMatrixStatusText =
            document.querySelector('#framework-input-matrix-status')?.textContent || '';
          const textField = document.querySelector('#framework-case-title');
          const textFieldStatus =
            document.querySelector('#framework-text-field-status')?.textContent || '';
          const textFieldValue = String(
            textField?.value ?? textField?.getAttribute?.('value') ?? '');
          const textFieldClientObserved =
            inputMatrixStatusText.includes('TextField changed: Flow IPC title')
              && textFieldStatus.includes('TextField client value: Flow IPC title')
              && textFieldValue === 'Flow IPC title';
          const textFieldServerObserved =
            textFieldStatus.includes('TextField server value: Server issued title')
              && textFieldValue === 'Server issued title';
          const dateTimePicker = document.querySelector('#framework-review-date-time');
          const dateTimePickerStatus =
            document.querySelector('#framework-date-time-picker-status')?.textContent || '';
          const dateTimePickerValue = String(
            dateTimePicker?.value ?? dateTimePicker?.getAttribute?.('value') ?? '');
          const dateTimePickerClientObserved =
            inputMatrixStatusText.includes('Review date-time changed: 2026-07-14T15:30')
              && dateTimePickerStatus.includes('DateTimePicker client value: 2026-07-14T15:30');
          const dateTimePickerServerObserved =
            dateTimePickerStatus.includes('DateTimePicker server value: 2026-08-01T16:20')
              && dateTimePickerValue.startsWith('2026-08-01T16:20');
          const numberField = document.querySelector('#framework-risk-score');
          const numberFieldStatus =
            document.querySelector('#framework-number-field-status')?.textContent || '';
          const numberFieldValue = String(
            numberField?.value ?? numberField?.getAttribute?.('value') ?? '');
          const numberFieldClientObserved =
            numberFieldStatus.includes('NumberField client value: 4.75')
              && numberFieldValue === '4.75';
          const numberFieldServerObserved =
            numberFieldStatus.includes('NumberField server value: 8.25')
              && numberFieldValue === '8.25';
          const checkboxGroup = document.querySelector('#framework-checkbox-group');
          const checkboxGroupStatus =
            document.querySelector('#framework-checkbox-group-status')?.textContent || '';
          const checkboxGroupItems = checkboxGroup
            ? Array.from(checkboxGroup.querySelectorAll('vaadin-checkbox'))
            : [];
          const checkboxGroupCheckedText = checkboxGroupItems
            .filter((item) => item.checked === true || item.hasAttribute?.('checked') === true)
            .map((item) => [
              item.value,
              item.getAttribute?.('value'),
              item.getAttribute?.('label'),
              collectDeepText(item)
            ].filter(Boolean).join(' ').toLowerCase())
            .sort()
            .join('|');
          const checkboxGroupClientObserved =
            checkboxGroupStatus.includes(
              'CheckboxGroup client value: compliance,qa,security')
              && checkboxGroupCheckedText.includes('compliance')
              && checkboxGroupCheckedText.includes('qa')
              && checkboxGroupCheckedText.includes('security')
              && !checkboxGroupCheckedText.includes('release');
          const checkboxGroupServerObserved =
            checkboxGroupStatus.includes('CheckboxGroup server value: qa,release')
              && checkboxGroupCheckedText.includes('qa')
              && checkboxGroupCheckedText.includes('release')
              && !checkboxGroupCheckedText.includes('security')
              && !checkboxGroupCheckedText.includes('compliance');
          const splitLayout = document.querySelector('#framework-split-layout');
          const splitPrimary = document.querySelector('#framework-split-primary');
          const splitSecondary = document.querySelector('#framework-split-secondary');
          const splitLayoutStatus =
            document.querySelector('#framework-split-layout-status')?.textContent || '';
          const splitPrimaryFlex = String(splitPrimary?.style?.flex || '');
          const splitSecondaryFlex = String(splitSecondary?.style?.flex || '');
          const splitLayoutDragObserved =
            splitLayoutStatus.includes('SplitLayout drag position: 35')
              && splitPrimaryFlex.includes('35%')
              && splitSecondaryFlex.includes('65%');
          const splitLayoutServerObserved =
            splitLayoutStatus.includes('SplitLayout server position: 62.5')
              && splitPrimaryFlex.includes('62.5%')
              && splitSecondaryFlex.includes('37.5%');
          const listBox = document.querySelector('#framework-list-box');
          const listBoxItems = listBox
            ? Array.from(listBox.querySelectorAll('vaadin-item'))
            : [];
          const listBoxStatus =
            document.querySelector('#framework-list-box-status')?.textContent || '';
          const listBoxSelected = Number(
            listBox?.selected ?? listBox?.getAttribute?.('selected') ?? -1);
          const listBoxClientObserved =
            listBoxStatus.includes('ListBox client value: build')
              && listBoxSelected === 1;
          const listBoxServerObserved =
            listBoxStatus.includes('ListBox server value: support')
              && listBoxSelected === 2;
          const multiSelectListBox =
            document.querySelector('#framework-multi-select-list-box');
          const multiSelectListBoxItems = multiSelectListBox
            ? Array.from(multiSelectListBox.querySelectorAll('vaadin-item'))
            : [];
          const multiSelectListBoxStatus =
            document.querySelector('#framework-multi-select-list-box-status')?.textContent || '';
          const multiSelectListBoxSelectedValues = Array.isArray(
            multiSelectListBox?.selectedValues)
              ? multiSelectListBox.selectedValues.map((value) => Number(value))
              : [];
          const multiSelectListBoxSelectedText = multiSelectListBoxSelectedValues
            .slice()
            .sort((left, right) => left - right)
            .join(',');
          const multiSelectListBoxClientObserved =
            multiSelectListBoxStatus.includes(
              'MultiSelectListBox client value: triage,verify')
              && multiSelectListBoxSelectedText === '0,2';
          const multiSelectListBoxServerObserved =
            multiSelectListBoxStatus.includes(
              'MultiSelectListBox server value: design,verify')
              && multiSelectListBoxSelectedText === '1,2';
          const customField = document.querySelector('#framework-custom-field');
          const customFieldOwner = document.querySelector('#framework-custom-field-owner');
          const customFieldState = document.querySelector('#framework-custom-field-state');
          const customFieldStatus =
            document.querySelector('#framework-custom-field-status')?.textContent || '';
          const customFieldOwnerValue = String(
            customFieldOwner?.value ?? customFieldOwner?.getAttribute?.('value') ?? '');
          const customFieldStateValue = String(
            customFieldState?.value ?? customFieldState?.getAttribute?.('value') ?? '');
          const customFieldClientObserved =
            customFieldStatus.includes('CustomField client value: edge|ready')
              && customFieldOwnerValue === 'edge'
              && customFieldStateValue === 'ready';
          const customFieldServerObserved =
            customFieldStatus.includes('CustomField server value: deploy|approved')
              && customFieldOwnerValue === 'deploy'
              && customFieldStateValue === 'approved';
          const formLayout = document.querySelector('#framework-form-layout');
          const formLayoutStatus =
            document.querySelector('#framework-form-layout-status')?.textContent || '';
          const formLayoutText = formLayout ? collectDeepText(formLayout) : '';
          const formLayoutResponsiveSteps = Array.isArray(formLayout?.responsiveSteps)
            ? formLayout.responsiveSteps
                .map((step) => `${step.minWidth}:${step.columns}`)
                .join(',')
            : '';
          const formLayoutColspan = String(
            document.querySelector('#framework-validation-status')
              ?.getAttribute?.('colspan') ?? '');
          const formLayoutMaxColumns = Number(formLayout?.maxColumns ?? -1);
          const formLayoutColumnWidth = String(formLayout?.columnWidth ?? '');
          const formLayoutLabelWidth = String(
            formLayout?.style?.getPropertyValue('--vaadin-form-layout-label-width') || '');
          const formLayoutColumnSpacing = String(
            formLayout?.style?.getPropertyValue('--vaadin-form-layout-column-spacing') || '');
          const formLayoutRowSpacing = String(
            formLayout?.style?.getPropertyValue('--vaadin-form-layout-row-spacing') || '');
          const formLayoutClickObserved =
            formLayoutStatus.includes('FormLayout clicked: true')
              && formLayoutText.includes('Owner');
          const formLayoutServerObserved =
            formLayoutStatus.includes(
              'FormLayout server layout: max=3 colspan=3 label=112px column=24px')
              && formLayoutColspan === '3'
              && formLayoutMaxColumns === 3
              && formLayoutResponsiveSteps.includes('600px:3')
              && formLayoutColumnWidth === '14em'
              && formLayoutLabelWidth === '112px'
              && formLayoutColumnSpacing === '24px'
              && formLayoutRowSpacing === '12px';
          const horizontalLayout = document.querySelector('#framework-horizontal-layout');
          const horizontalLayoutGrow =
            document.querySelector('#framework-horizontal-layout-grow');
          const horizontalLayoutTail =
            document.querySelector('#framework-horizontal-layout-tail');
          const horizontalLayoutStatus =
            document.querySelector('#framework-horizontal-layout-status')?.textContent || '';
          const horizontalLayoutText =
            horizontalLayout ? collectDeepText(horizontalLayout) : '';
          const horizontalLayoutTheme =
            horizontalLayout?.getAttribute?.('theme') || '';
          const horizontalLayoutJustify =
            String(horizontalLayout?.style?.justifyContent || '');
          const horizontalLayoutAlignItems =
            String(horizontalLayout?.style?.alignItems || '');
          const horizontalLayoutGap =
            String(horizontalLayout?.style?.gap || '');
          const horizontalLayoutBoxSizing =
            String(horizontalLayout?.style?.boxSizing || '');
          const horizontalLayoutWidth =
            String(horizontalLayout?.style?.width || '');
          const horizontalLayoutGrowStyle =
            Number(horizontalLayoutGrow?.style?.flexGrow || 0);
          const horizontalLayoutTailAlignSelf =
            String(horizontalLayoutTail?.style?.alignSelf || '');
          const horizontalLayoutClickObserved =
            horizontalLayoutStatus.includes('HorizontalLayout clicked: true')
              && horizontalLayoutText.includes('Queue')
              && horizontalLayoutText.includes('Review lane');
          const horizontalLayoutServerObserved =
            horizontalLayoutStatus.includes(
              'HorizontalLayout server layout: evenly stretch grow=2 tail=baseline')
              && horizontalLayoutJustify === 'space-evenly'
              && horizontalLayoutAlignItems === 'stretch'
              && horizontalLayoutGap === '22px'
              && horizontalLayoutBoxSizing === 'content-box'
              && horizontalLayoutWidth === '480px'
              && horizontalLayoutGrowStyle === 2
              && horizontalLayoutTailAlignSelf === 'baseline'
              && horizontalLayoutTheme.includes('padding')
              && horizontalLayoutTheme.includes('margin')
              && horizontalLayoutTheme.includes('wrap')
              && horizontalLayoutText.includes('Aligned');
          const flexLayout = document.querySelector('#framework-flex-layout');
          const flexLayoutAlpha = document.querySelector('#framework-flex-layout-alpha');
          const flexLayoutBeta = document.querySelector('#framework-flex-layout-beta');
          const flexLayoutGamma = document.querySelector('#framework-flex-layout-gamma');
          const flexLayoutStatus =
            document.querySelector('#framework-flex-layout-status')?.textContent || '';
          const flexLayoutText = flexLayout ? collectDeepText(flexLayout) : '';
          const flexLayoutFlexWrap = String(flexLayout?.style?.flexWrap || '');
          const flexLayoutFlexDirection =
            String(flexLayout?.style?.flexDirection || '');
          const flexLayoutAlignContent =
            String(flexLayout?.style?.alignContent || '');
          const flexLayoutJustify =
            String(flexLayout?.style?.justifyContent || '');
          const flexLayoutAlignItems =
            String(flexLayout?.style?.alignItems || '');
          const flexLayoutGap = String(flexLayout?.style?.gap || '');
          const flexLayoutWidth = String(flexLayout?.style?.width || '');
          const flexLayoutBetaBasis =
            String(flexLayoutBeta?.style?.flexBasis || '');
          const flexLayoutAlphaOrder =
            Number(flexLayoutAlpha?.style?.order || 0);
          const flexLayoutGammaOrder =
            Number(flexLayoutGamma?.style?.order || 0);
          const flexLayoutBetaGrow =
            Number(flexLayoutBeta?.style?.flexGrow || 0);
          const flexLayoutClickObserved =
            flexLayoutStatus.includes('FlexLayout clicked: true')
              && flexLayoutText.includes('Alpha')
              && flexLayoutText.includes('Beta');
          const flexLayoutServerObserved =
            flexLayoutStatus.includes(
              'FlexLayout server layout: column-reverse wrap-reverse basis=180 order=3')
              && flexLayoutFlexWrap === 'wrap-reverse'
              && flexLayoutFlexDirection === 'column-reverse'
              && flexLayoutAlignContent === 'space-between'
              && flexLayoutJustify === 'space-around'
              && flexLayoutAlignItems === 'stretch'
              && flexLayoutGap === '18px'
              && flexLayoutWidth === '460px'
              && flexLayoutBetaBasis === '180px'
              && flexLayoutAlphaOrder === 3
              && flexLayoutGammaOrder === 1
              && flexLayoutBetaGrow === 2
              && flexLayoutText.includes('Gamma patched');
          const verticalLayout = document.querySelector('#framework-vertical-layout');
          const verticalLayoutBody =
            document.querySelector('#framework-vertical-layout-body');
          const verticalLayoutFooter =
            document.querySelector('#framework-vertical-layout-footer');
          const verticalLayoutStatus =
            document.querySelector('#framework-vertical-layout-status')?.textContent || '';
          const verticalLayoutText =
            verticalLayout ? collectDeepText(verticalLayout) : '';
          const verticalLayoutTheme =
            verticalLayout?.getAttribute?.('theme') || '';
          const verticalLayoutJustify =
            String(verticalLayout?.style?.justifyContent || '');
          const verticalLayoutAlignItems =
            String(verticalLayout?.style?.alignItems || '');
          const verticalLayoutGap =
            String(verticalLayout?.style?.gap || '');
          const verticalLayoutBoxSizing =
            String(verticalLayout?.style?.boxSizing || '');
          const verticalLayoutWidth =
            String(verticalLayout?.style?.width || '');
          const verticalLayoutHeight =
            String(verticalLayout?.style?.height || '');
          const verticalLayoutBodyAlignSelf =
            String(verticalLayoutBody?.style?.alignSelf || '');
          const verticalLayoutFooterGrow =
            Number(verticalLayoutFooter?.style?.flexGrow || 0);
          const verticalLayoutClickObserved =
            verticalLayoutStatus.includes('VerticalLayout clicked: true')
              && verticalLayoutText.includes('Inbox')
              && verticalLayoutText.includes('Review stack');
          const verticalLayoutServerObserved =
            verticalLayoutStatus.includes(
              'VerticalLayout server layout: evenly stretch grow=2 body=center')
              && verticalLayoutJustify === 'space-evenly'
              && verticalLayoutAlignItems === 'stretch'
              && verticalLayoutGap === '20px'
              && verticalLayoutBoxSizing === 'content-box'
              && verticalLayoutWidth === '360px'
              && verticalLayoutHeight === '168px'
              && verticalLayoutBodyAlignSelf === 'center'
              && verticalLayoutFooterGrow === 2
              && verticalLayoutTheme.includes('padding')
              && verticalLayoutTheme.includes('margin')
              && verticalLayoutText.includes('Dequeued');
          const scroller = document.querySelector('#framework-scroller');
          const scrollerContent =
            document.querySelector('#framework-scroller-content');
          const scrollerStatus =
            document.querySelector('#framework-scroller-status')?.textContent || '';
          const scrollerText =
            scroller ? collectDeepText(scroller) : '';
          const scrollerTheme =
            scroller?.getAttribute?.('theme') || '';
          const scrollerScrollDirection =
            String(scroller?.scrollDirection
              || scroller?.getAttribute?.('scroll-direction') || '');
          const scrollerScrollTop = Number(scroller?.scrollTop || 0);
          const scrollerScrollHeight = Number(scroller?.scrollHeight || 0);
          const scrollerClientHeight = Number(scroller?.clientHeight || 0);
          const scrollerScrollWidth = Number(scroller?.scrollWidth || 0);
          const scrollerClientWidth = Number(scroller?.clientWidth || 0);
          const scrollerWidth = String(scroller?.style?.width || '');
          const scrollerHeight = String(scroller?.style?.height || '');
          const scrollerContentWidth =
            String(scrollerContent?.style?.width || '');
          const scrollerContentHeight =
            String(scrollerContent?.style?.height || '');
          const scrollerClientObserved =
            scrollerStatus.includes('Scroller scrolled: top=')
              && scrollerScrollTop >= 80
              && scrollerScrollHeight > scrollerClientHeight
              && scrollerText.includes('Scroll item 12');
          const scrollerServerObserved =
            scrollerScrollDirection === 'horizontal'
              && scrollerWidth === '340px'
              && scrollerHeight === '96px'
              && scrollerContentWidth === '760px'
              && scrollerContentHeight === '88px'
              && scrollerScrollWidth > scrollerClientWidth
              && scrollerText.includes('Horizontal patched 6')
              && scrollerTheme.includes('overflow-indicators');
          const virtualList = document.querySelector('#framework-virtual-list');
          const virtualListStatus =
            document.querySelector('#framework-virtual-list-status')?.textContent || '';
          const virtualListText = virtualList ? collectDeepText(virtualList) : '';
          const virtualListClientObserved =
            virtualListStatus.includes('VirtualList fetch ')
              && virtualListText.includes('Virtual queue item 056');
          const virtualListServerObserved =
            virtualListStatus.includes('VirtualList server scroll: 79')
              && virtualListText.includes('Virtual queue item 080');
          const upload = document.querySelector('#framework-upload');
          const uploadFileInput = upload && findDeepFrom(upload, (element) =>
            element.localName === 'input'
              && (element.type || '').toLowerCase() === 'file');
          const richTextEditor = document.querySelector('#framework-rich-text-editor');
          const resourceFetch = window.VaadinDesktopFrameworkResourceFetch || {};
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
              && !!checkboxGroup
              && checkboxGroupItems.length >= 4
              && !!document.querySelector('#framework-checkbox-group-server-update')
              && !!document.querySelector('#framework-checkbox-group-status')
              && !!document.querySelector('#framework-priority')
              && !!document.querySelector('#framework-due-date')
              && !!document.querySelector('#framework-decision')
              && !!textField
              && !!document.querySelector('#framework-text-field-server-update')
              && !!document.querySelector('#framework-text-field-status')
              && !!document.querySelector('#framework-notes')
              && !!document.querySelector('#framework-contact-email')
              && !!document.querySelector('#framework-score')
              && !!numberField
              && !!document.querySelector('#framework-number-field-server-update')
              && !!document.querySelector('#framework-number-field-status')
              && !!document.querySelector('#framework-secret')
              && !!document.querySelector('#framework-review-time')
              && !!document.querySelector('#framework-review-date-time')
              && !!document.querySelector('#framework-date-time-picker-server-update')
              && !!document.querySelector('#framework-date-time-picker-status')
              && !!splitLayout
              && !!splitPrimary
              && !!splitSecondary
              && !!document.querySelector('#framework-split-layout-server-update')
              && !!document.querySelector('#framework-split-layout-status')
              && !!document.querySelector('#framework-assignees')
              && !!document.querySelector('#framework-assignees-status')
              && !!listBox
              && listBoxItems.length >= 3
              && !!document.querySelector('#framework-list-box-server-update')
              && !!document.querySelector('#framework-list-box-status')
              && !!multiSelectListBox
              && multiSelectListBoxItems.length >= 3
              && !!document.querySelector('#framework-multi-select-list-box-server-update')
              && !!document.querySelector('#framework-multi-select-list-box-status')
              && !!customField
              && !!customFieldOwner
              && !!customFieldState
              && !!document.querySelector('#framework-custom-field-server-update')
              && !!document.querySelector('#framework-custom-field-status')
              && !!document.querySelector('#framework-input-matrix-status')
              && !!document.querySelector('#framework-dialog-open')
              && !!document.querySelector('#framework-validation-action')
              && !!formLayout
              && !!document.querySelector('#framework-form-layout-server-update')
              && !!document.querySelector('#framework-form-layout-status')
              && !!horizontalLayout
              && !!horizontalLayoutGrow
              && !!horizontalLayoutTail
              && !!document.querySelector('#framework-horizontal-layout-server-update')
              && !!document.querySelector('#framework-horizontal-layout-status')
              && !!flexLayout
              && !!flexLayoutAlpha
              && !!flexLayoutBeta
              && !!flexLayoutGamma
              && !!document.querySelector('#framework-flex-layout-server-update')
              && !!document.querySelector('#framework-flex-layout-status')
              && !!verticalLayout
              && !!verticalLayoutBody
              && !!verticalLayoutFooter
              && !!document.querySelector('#framework-vertical-layout-server-update')
              && !!document.querySelector('#framework-vertical-layout-status')
              && !!scroller
              && !!scrollerContent
              && !!document.querySelector('#framework-scroller-server-update')
              && !!document.querySelector('#framework-scroller-status')
              && !!document.querySelector('#framework-avatar')
              && !!document.querySelector('#framework-message-input')
              && !!document.querySelector('#framework-message-list')
              && !!document.querySelector('#framework-login-overlay')
              && !!document.querySelector('#framework-login-overlay-reopen')
              && !!document.querySelector('#framework-login-form')
              && !!document.querySelector('#framework-login-form-status')
              && !!document.querySelector('#framework-login-overlay-action-post')
              && !!document.querySelector('#framework-login-overlay-action-post-open')
              && !!document.querySelector('#framework-login-overlay-action-post-status')
              && !!document.querySelector('#framework-login-form-action-post')
              && !!document.querySelector('#framework-login-form-action-post-status')
              && !!document.querySelector('#framework-login-overlay-submit-button')
              && !!document.querySelector('#framework-login-overlay-submit-button-status')
              && !!document.querySelector('#framework-login-form-show-error')
              && !!document.querySelector('#framework-login-form-show-error-status')
              && !!document.querySelector('#framework-login-form-enabled')
              && !!document.querySelector('#framework-login-form-enable')
              && !!document.querySelector('#framework-login-form-enabled-status')
              && !!document.querySelector('#framework-login-form-i18n-full')
              && !!document.querySelector('#framework-login-form-i18n-full-status')
              && !!document.querySelector('#framework-login-form-no-autofocus-host')
              && !!document.querySelector('#framework-login-form-no-autofocus-status')
              && !!document.querySelector('#framework-login-form-autofill-attributes')
              && !!document.querySelector('#framework-login-form-autofill-attributes-status')
              && !!document.querySelector('#framework-login-form-a11y-semantics')
              && !!document.querySelector('#framework-login-form-a11y-semantics-status')
              && !!document.querySelector('#framework-login-overlay-no-autofocus')
              && !!document.querySelector('#framework-login-overlay-no-autofocus-status')
              && !!document.querySelector('#framework-login-overlay-autofocus-control-host')
              && !!document.querySelector('#framework-login-overlay-autofocus-control-status')
              && !!document.querySelector('#framework-login-overlay-outside-close')
              && !!document.querySelector('#framework-login-overlay-outside-close-status')
              && !!document.querySelector('#framework-login-overlay-autofill-attributes')
              && !!document.querySelector('#framework-login-overlay-autofill-attributes-status')
              && !!document.querySelector('#framework-login-overlay-a11y-semantics')
              && !!document.querySelector('#framework-login-overlay-a11y-semantics-status')
              && !!document.querySelector('#framework-login-overlay-i18n-full')
              && !!document.querySelector('#framework-login-overlay-i18n-full-status')
              && !!document.querySelector('#framework-rich-text-editor')
              && !!document.querySelector('#framework-rich-text-editor-status')
              && !!document.querySelector('#framework-rich-text-editor-behavior-status')
              && !!document.querySelector('#framework-rich-text-editor-editing-status')
              && !!document.querySelector('#framework-nav-tabs')
              && !!document.querySelector('#framework-nav-tab-audit')
              && !!document.querySelector('#framework-details')
              && !!document.querySelector('#framework-accordion')
              && !!document.querySelector('#framework-menu')
              && !!document.querySelector('#framework-navigation-matrix-status')
              && !!document.querySelector('#framework-side-nav')
              && !!document.querySelector('#framework-side-nav-audit')
              && !!document.querySelector('#framework-context-menu-target')
              && !!document.querySelector('#framework-grid')
              && !!document.querySelector('#framework-lazy-grid')
              && !!document.querySelector('#framework-lazy-grid-status')
              && !!document.querySelector('#framework-lazy-grid-report')
              && !!virtualList
              && !!document.querySelector('#framework-virtual-list-status')
              && !!document.querySelector('#framework-virtual-list-scroll-end')
              && !!document.querySelector('#framework-popover-target')
              && !!document.querySelector('#framework-popover')
              && !!document.querySelector('#framework-notification-open')
              && !!document.querySelector('#framework-notification-status')
              && !!document.querySelector('#framework-confirm-dialog-open')
              && !!document.querySelector('#framework-confirm-dialog')
              && !!document.querySelector('#framework-confirm-dialog-status')
              && !!document.querySelector('#framework-progress-bar')
              && !!document.querySelector('#framework-progress-value-update')
              && !!document.querySelector('#framework-progress-indeterminate-update')
              && !!document.querySelector('#framework-progress-bar-status')
              && !!document.querySelector('#framework-overlay-matrix-status')
              && !!document.querySelector('#framework-upload')
              && !!document.querySelector('#framework-upload-status')
              && !!document.querySelector('#framework-stream-resource-download')
              && !!document.querySelector('#framework-stream-resource-status')
              && !!document.querySelector('#framework-stream-resource-range-download')
              && !!document.querySelector('#framework-stream-resource-range-status')
              && !!document.querySelector('#framework-stream-resource-chunks-status')
              && !!document.querySelector('#framework-download-handler-save-open')
              && !!document.querySelector('#framework-download-handler-save-open-status')
              && !!document.querySelector('#framework-standard-streaming-download')
              && !!document.querySelector('#framework-rich-content-status')
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
            checkboxGroupClientObserved,
            checkboxGroupServerObserved,
            checkboxGroupCheckedText,
            checkboxGroupStatus,
            matrixStatus: document.querySelector('#framework-matrix-status')?.textContent || '',
            inputMatrixStatus: inputMatrixStatusText,
            textFieldClientObserved,
            textFieldServerObserved,
            textFieldValue,
            textFieldStatus,
            assigneesStatus: document.querySelector('#framework-assignees-status')?.textContent || '',
            dialogContent: document.querySelector('#framework-dialog-content')?.textContent || '',
            dialogOpened: document.querySelector('#framework-dialog')?.opened === true,
            validationStatus: document.querySelector('#framework-validation-status')?.textContent || '',
            formLayoutClickObserved,
            formLayoutServerObserved,
            formLayoutStatus,
            formLayoutColspan,
            formLayoutMaxColumns,
            formLayoutResponsiveSteps,
            formLayoutColumnWidth,
            formLayoutLabelWidth,
            formLayoutColumnSpacing,
            formLayoutRowSpacing,
            horizontalLayoutClickObserved,
            horizontalLayoutServerObserved,
            horizontalLayoutStatus,
            horizontalLayoutTheme,
            horizontalLayoutJustify,
            horizontalLayoutAlignItems,
            horizontalLayoutGap,
            horizontalLayoutBoxSizing,
            horizontalLayoutWidth,
            horizontalLayoutGrowStyle,
            horizontalLayoutTailAlignSelf,
            flexLayoutClickObserved,
            flexLayoutServerObserved,
            flexLayoutStatus,
            flexLayoutFlexWrap,
            flexLayoutFlexDirection,
            flexLayoutAlignContent,
            flexLayoutJustify,
            flexLayoutAlignItems,
            flexLayoutGap,
            flexLayoutWidth,
            flexLayoutBetaBasis,
            flexLayoutAlphaOrder,
            flexLayoutGammaOrder,
            flexLayoutBetaGrow,
            verticalLayoutClickObserved,
            verticalLayoutServerObserved,
            verticalLayoutStatus,
            verticalLayoutTheme,
            verticalLayoutJustify,
            verticalLayoutAlignItems,
            verticalLayoutGap,
            verticalLayoutBoxSizing,
            verticalLayoutWidth,
            verticalLayoutHeight,
            verticalLayoutBodyAlignSelf,
            verticalLayoutFooterGrow,
            scrollerClientObserved,
            scrollerServerObserved,
            scrollerStatus,
            scrollerTheme,
            scrollerScrollDirection,
            scrollerScrollTop,
            scrollerScrollHeight,
            scrollerClientHeight,
            scrollerScrollWidth,
            scrollerClientWidth,
            scrollerWidth,
            scrollerHeight,
            scrollerContentWidth,
            scrollerContentHeight,
            overlayMatrixStatus: document.querySelector('#framework-overlay-matrix-status')?.textContent || '',
            contextMenuOpened: contextMenu?.opened === true
              || contextMenu?.hasAttribute('opened') === true
              || !!contextMenuOverlay,
            contextMenuItemRendered: !!contextMenuItem,
            gridContextMenuOpened: gridContextMenu?.opened === true
              || gridContextMenu?.hasAttribute('opened') === true
              || !!gridContextMenuOverlay,
            gridContextMenuItemRendered: !!gridContextMenuItem,
            loginOverlayOpened: loginOverlay?.opened === true
              || loginOverlay?.hasAttribute('opened') === true,
            loginOverlayClosed: !!loginOverlay
              && loginOverlay?.opened !== true
              && loginOverlay?.hasAttribute('opened') !== true,
            loginOverlayControlsRendered: !!loginOverlayUsernameField
              && !!loginOverlayPasswordField
              && !!loginOverlaySubmit,
            loginOverlayForgotButtonRendered: !!loginOverlayForgot,
            loginOverlayForgotPasswordObserved,
            loginOverlayShowErrorObserved,
            loginOverlayShowErrorText,
            loginFormControlsRendered: !!loginFormUsernameField
              && !!loginFormPasswordField
              && !!loginFormSubmit,
            loginFormForgotButtonRendered: !!loginFormForgot,
            loginFormForgotPasswordObserved,
            loginFormStatus,
            loginOverlayActionPostOpened: loginOverlayActionPost?.opened === true
              || loginOverlayActionPost?.hasAttribute('opened') === true,
            loginOverlayActionPostClosed: !!loginOverlayActionPost
              && loginOverlayActionPost?.opened !== true
              && loginOverlayActionPost?.hasAttribute('opened') !== true,
            loginOverlayActionPostControlsRendered: !!loginOverlayActionPostUsernameField
              && !!loginOverlayActionPostPasswordField
              && !!loginOverlayActionPostSubmit,
            loginOverlayActionPostStatus: document.querySelector('#framework-login-overlay-action-post-status')?.textContent || '',
            loginFormActionPostControlsRendered: !!loginFormActionPostUsernameField
              && !!loginFormActionPostPasswordField
              && !!loginFormActionPostSubmit,
            loginFormActionPostStatus: document.querySelector('#framework-login-form-action-post-status')?.textContent || '',
            loginOverlaySubmitButtonStatus: document.querySelector('#framework-login-overlay-submit-button-status')?.textContent || '',
            loginOverlayEnterStatus: document.querySelector('#framework-login-overlay-enter-status')?.textContent || '',
            loginFormShowErrorStatus: document.querySelector('#framework-login-form-show-error-status')?.textContent || '',
            loginFormSubmitButtonObserved,
            loginFormButtonStatus,
            loginFormEnterStatus: document.querySelector('#framework-login-form-enter-status')?.textContent || '',
            loginFormEnabledStatus: document.querySelector('#framework-login-form-enabled-status')?.textContent || '',
            loginFormEnabledDisabled: document.querySelector('#framework-login-form-enabled')?.disabled === true
              || document.querySelector('#framework-login-form-enabled')?.hasAttribute('disabled') === true,
            loginFormAutofillAttributesObserved,
            loginFormAutofillUsernameAutocomplete,
            loginFormAutofillPasswordAutocomplete,
            loginFormA11ySemanticsObserved,
            loginFormA11ySemanticsStatus,
            loginFormA11yText,
            loginFormA11yUsernameLabel,
            loginFormA11yPasswordLabel,
            loginFormA11yUsernameName,
            loginFormA11yPasswordName,
            loginFormA11yPasswordInputType,
            loginFormNoAutofocusObserved,
            loginFormNoAutofocusProperty,
            loginFormNoAutofocusFieldFocused,
            loginFormNoAutofocusStatus,
            loginFormFullI18nRendered,
            loginFormFullI18nText,
            loginFormFullI18nStatus:
              document.querySelector('#framework-login-form-i18n-full-status')?.textContent || '',
            loginOverlayNoAutofocusObserved,
            loginOverlayNoAutofocusOpened,
            loginOverlayNoAutofocusClosed,
            loginOverlayNoAutofocusProperty,
            loginOverlayNoAutofocusFieldFocused,
            loginOverlayNoAutofocusStatus,
            loginOverlayAutofocusControlObserved,
            loginOverlayAutofocusControlOpened,
            loginOverlayAutofocusControlClosed,
            loginOverlayAutofocusControlProperty,
            loginOverlayAutofocusControlUsernameFocused,
            loginOverlayAutofocusControlActiveElementPath,
            loginOverlayAutofocusControlStatus,
            loginOverlayOutsideCloseObserved,
            loginOverlayOutsideClickPreventedObserved,
            loginOverlayOutsideCloseOpened,
            loginOverlayOutsideClosed,
            loginOverlayOutsideCloseStatus,
            loginOverlayOutsideClickDispatched:
              loginOverlayOutsideClick.dispatched === true,
            loginOverlayOutsideClickEventObserved:
              loginOverlayOutsideClick.eventObserved === true,
            loginOverlayOutsideClickDefaultPrevented:
              loginOverlayOutsideClick.defaultPrevented === true,
            loginOverlayOutsideClickPreventedMarker:
              loginOverlayOutsideClick.preventedObserved === true,
            loginOverlayOutsideClickTarget:
              String(loginOverlayOutsideClick.target || ''),
            loginOverlayOutsideClickEventTarget:
              String(loginOverlayOutsideClick.eventTarget || ''),
            loginOverlayOutsideClickError:
              String(loginOverlayOutsideClick.error || ''),
            loginOverlayEscapeKeyObserved,
            loginOverlayEscapeKeyPreventedObserved,
            loginOverlayEscapeKeyOpened,
            loginOverlayEscapeKeyClosed,
            loginOverlayEscapeKeyStatus,
            loginOverlayEscapeKeyDispatched:
              loginOverlayEscapeKeyMarker.dispatched === true,
            loginOverlayEscapeKeyEventObserved:
              loginOverlayEscapeKeyMarker.eventObserved === true,
            loginOverlayEscapeKeyDefaultPrevented:
              loginOverlayEscapeKeyMarker.defaultPrevented === true,
            loginOverlayEscapeKeyPreventedMarker:
              loginOverlayEscapeKeyMarker.preventedObserved === true,
            loginOverlayEscapeKeyTarget:
              String(loginOverlayEscapeKeyMarker.target || ''),
            loginOverlayEscapeKeyEventTarget:
              String(loginOverlayEscapeKeyMarker.eventTarget || ''),
            loginOverlayEscapeKeyError:
              String(loginOverlayEscapeKeyMarker.error || ''),
            loginOverlayAutofillAttributesObserved,
            loginOverlayAutofillAttributesOpened,
            loginOverlayAutofillAttributesClosed,
            loginOverlayAutofillUsernameAutocomplete,
            loginOverlayAutofillPasswordAutocomplete,
            loginOverlayAutofillAttributesStatus,
            loginOverlayA11ySemanticsObserved,
            loginOverlayA11ySemanticsOpened,
            loginOverlayA11ySemanticsClosed,
            loginOverlayA11ySemanticsStatus,
            loginOverlayA11yText,
            loginOverlayA11yUsernameLabel,
            loginOverlayA11yPasswordLabel,
            loginOverlayA11yUsernameName,
            loginOverlayA11yPasswordName,
            loginOverlayA11yPasswordInputType,
            loginOverlayFullI18nOpened,
            loginOverlayFullI18nRendered,
            loginOverlayFullI18nStatus,
            loginOverlayFullI18nText,
            navigationMatrixStatus: document.querySelector('#framework-navigation-matrix-status')?.textContent || '',
            detailsOpened: document.querySelector('#framework-details')?.opened === true
              || document.querySelector('#framework-details')?.hasAttribute('opened') === true,
            lazyGridStatus: document.querySelector('#framework-lazy-grid-status')?.textContent || '',
            virtualListClientObserved,
            virtualListServerObserved,
            virtualListStatus,
            virtualListText,
            popoverOpened: document.querySelector('#framework-popover')?.opened === true
              || document.querySelector('#framework-popover')?.hasAttribute('opened') === true,
            notificationOpenObserved,
            notificationOpened,
            notificationStatus,
            notificationText,
            confirmDialogOpenObserved,
            confirmDialogConfirmObserved,
            confirmDialogCancelObserved,
            confirmDialogRejectObserved,
            confirmDialogOpened,
            confirmDialogStatus,
            confirmDialogText,
            progressBarValueObserved,
            progressBarIndeterminateObserved,
            progressBarValue,
            progressBarMin,
            progressBarMax,
            progressBarIndeterminate,
            progressBarStatus,
            dateTimePickerClientObserved,
            dateTimePickerServerObserved,
            dateTimePickerValue,
            dateTimePickerStatus,
            numberFieldClientObserved,
            numberFieldServerObserved,
            numberFieldValue,
            numberFieldStatus,
            splitLayoutDragObserved,
            splitLayoutServerObserved,
            splitPrimaryFlex,
            splitSecondaryFlex,
            splitLayoutStatus,
            listBoxClientObserved,
            listBoxServerObserved,
            listBoxSelected,
            listBoxStatus,
            listBoxText: listBoxItems.map((item) => item.textContent || '').join('|'),
            multiSelectListBoxClientObserved,
            multiSelectListBoxServerObserved,
            multiSelectListBoxSelectedText,
            multiSelectListBoxStatus,
            multiSelectListBoxText:
              multiSelectListBoxItems.map((item) => item.textContent || '').join('|'),
            customFieldClientObserved,
            customFieldServerObserved,
            customFieldOwnerValue,
            customFieldStateValue,
            customFieldStatus,
            uploadControlsRendered: !!uploadFileInput && typeof upload?.uploadFiles === 'function',
            uploadStatus: document.querySelector('#framework-upload-status')?.textContent || '',
            resourceFetchComplete: resourceFetch.complete === true,
            resourceFetchStatus: resourceFetch.statusText || '',
            streamResourceFetchOk: resourceFetch.streamResourceOk === true,
            binaryStreamResourceFetchOk: resourceFetch.binaryStreamResourceOk === true,
            downloadHandlerFetchOk: resourceFetch.downloadHandlerOk === true,
            standardStreamingDownloadFetchOk: resourceFetch.standardStreamingDownloadOk === true,
            standardStreamingDownloadBytes: Number(resourceFetch.standardStreamingDownloadBytes || 0),
            resourceFetchError: resourceFetch.error || '',
            richTextEditorStatus: document.querySelector('#framework-rich-text-editor-status')?.textContent || '',
            richTextEditorBehaviorStatus: document.querySelector('#framework-rich-text-editor-behavior-status')?.textContent || '',
            richTextEditorEditingStatus: document.querySelector('#framework-rich-text-editor-editing-status')?.textContent || '',
            richTextEditorHtmlValue: String(richTextEditor?.htmlValue || ''),
            richTextEditorValue: String(richTextEditor?.value || ''),
            richContentStatus: document.querySelector('#framework-rich-content-status')?.textContent || '',
            messageListText: document.querySelector('#framework-message-list')?.textContent || '',
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
            "error": standardFrameworkString(state, "error"),
            "requestCount": standardFrameworkInteger(state, "requestCount"),
            "responseCount": standardFrameworkInteger(state, "responseCount"),
            "pushMessageCount": standardFrameworkInteger(state, "pushMessageCount"),
            "pushReconnectCount": standardFrameworkInteger(state, "pushReconnectCount"),
            "pushResyncCount": standardFrameworkInteger(state, "pushResyncCount"),
            "componentStatus": standardFrameworkString(state, "componentStatus"),
            "checkboxGroupClientObserved":
                state["checkboxGroupClientObserved"] as? Bool == true,
            "checkboxGroupServerObserved":
                state["checkboxGroupServerObserved"] as? Bool == true,
            "checkboxGroupCheckedText":
                standardFrameworkString(state, "checkboxGroupCheckedText"),
            "checkboxGroupStatus": standardFrameworkString(state, "checkboxGroupStatus"),
            "matrixStatus": standardFrameworkString(state, "matrixStatus"),
            "inputMatrixStatus": standardFrameworkString(state, "inputMatrixStatus"),
            "textFieldClientObserved": state["textFieldClientObserved"] as? Bool == true,
            "textFieldServerObserved": state["textFieldServerObserved"] as? Bool == true,
            "textFieldValue": standardFrameworkString(state, "textFieldValue"),
            "textFieldStatus": standardFrameworkString(state, "textFieldStatus"),
            "assigneesStatus": standardFrameworkString(state, "assigneesStatus"),
            "formLayoutClickObserved":
                state["formLayoutClickObserved"] as? Bool == true,
            "formLayoutServerObserved":
                state["formLayoutServerObserved"] as? Bool == true,
            "formLayoutStatus": standardFrameworkString(state, "formLayoutStatus"),
            "formLayoutColspan": standardFrameworkString(state, "formLayoutColspan"),
            "formLayoutResponsiveSteps":
                standardFrameworkString(state, "formLayoutResponsiveSteps"),
            "horizontalLayoutClickObserved":
                state["horizontalLayoutClickObserved"] as? Bool == true,
            "horizontalLayoutServerObserved":
                state["horizontalLayoutServerObserved"] as? Bool == true,
            "horizontalLayoutStatus":
                standardFrameworkString(state, "horizontalLayoutStatus"),
            "horizontalLayoutJustify":
                standardFrameworkString(state, "horizontalLayoutJustify"),
            "horizontalLayoutAlignItems":
                standardFrameworkString(state, "horizontalLayoutAlignItems"),
            "flexLayoutClickObserved":
                state["flexLayoutClickObserved"] as? Bool == true,
            "flexLayoutServerObserved":
                state["flexLayoutServerObserved"] as? Bool == true,
            "flexLayoutStatus": standardFrameworkString(state, "flexLayoutStatus"),
            "flexLayoutFlexWrap": standardFrameworkString(state, "flexLayoutFlexWrap"),
            "flexLayoutFlexDirection":
                standardFrameworkString(state, "flexLayoutFlexDirection"),
            "verticalLayoutClickObserved":
                state["verticalLayoutClickObserved"] as? Bool == true,
            "verticalLayoutServerObserved":
                state["verticalLayoutServerObserved"] as? Bool == true,
            "verticalLayoutStatus":
                standardFrameworkString(state, "verticalLayoutStatus"),
            "verticalLayoutJustify":
                standardFrameworkString(state, "verticalLayoutJustify"),
            "verticalLayoutAlignItems":
                standardFrameworkString(state, "verticalLayoutAlignItems"),
            "scrollerClientObserved":
                state["scrollerClientObserved"] as? Bool == true,
            "scrollerServerObserved":
                state["scrollerServerObserved"] as? Bool == true,
            "scrollerStatus": standardFrameworkString(state, "scrollerStatus"),
            "scrollerScrollDirection":
                standardFrameworkString(state, "scrollerScrollDirection"),
            "scrollerWidth": standardFrameworkString(state, "scrollerWidth"),
            "scrollerContentWidth":
                standardFrameworkString(state, "scrollerContentWidth"),
            "navigationMatrixStatus": standardFrameworkString(state, "navigationMatrixStatus"),
            "overlayMatrixStatus": standardFrameworkString(state, "overlayMatrixStatus"),
            "lazyGridStatus": standardFrameworkString(state, "lazyGridStatus"),
            "virtualListClientObserved":
                state["virtualListClientObserved"] as? Bool == true,
            "virtualListServerObserved":
                state["virtualListServerObserved"] as? Bool == true,
            "virtualListStatus": standardFrameworkString(state, "virtualListStatus"),
            "virtualListText": standardFrameworkString(state, "virtualListText"),
            "confirmDialogStatus": standardFrameworkString(state, "confirmDialogStatus"),
            "progressBarStatus": standardFrameworkString(state, "progressBarStatus"),
            "uploadStatus": standardFrameworkString(state, "uploadStatus"),
            "resourceFetchStatus": standardFrameworkString(state, "resourceFetchStatus"),
            "resourceFetchError": standardFrameworkString(state, "resourceFetchError"),
            "streamResourceFetchOk": state["streamResourceFetchOk"] as? Bool == true,
            "binaryStreamResourceFetchOk": state["binaryStreamResourceFetchOk"] as? Bool == true,
            "downloadHandlerFetchOk": state["downloadHandlerFetchOk"] as? Bool == true,
            "standardStreamingDownloadFetchOk":
                state["standardStreamingDownloadFetchOk"] as? Bool == true,
            "standardStreamingDownloadBytes":
                standardFrameworkInteger(state, "standardStreamingDownloadBytes"),
            "richTextEditorStatus": standardFrameworkString(state, "richTextEditorStatus"),
            "richTextEditorBehaviorStatus":
                standardFrameworkString(state, "richTextEditorBehaviorStatus"),
            "richTextEditorEditingStatus":
                standardFrameworkString(state, "richTextEditorEditingStatus"),
            "loginFormStatus": standardFrameworkString(state, "loginFormStatus"),
            "loginOverlayActionPostStatus":
                standardFrameworkString(state, "loginOverlayActionPostStatus"),
            "loginFormActionPostStatus":
                standardFrameworkString(state, "loginFormActionPostStatus"),
            "loginOverlaySubmitButtonStatus":
                standardFrameworkString(state, "loginOverlaySubmitButtonStatus"),
            "loginFormSubmitButtonObserved":
                state["loginFormSubmitButtonObserved"] as? Bool == true,
            "loginFormButtonStatus":
                standardFrameworkString(state, "loginFormButtonStatus"),
            "loginFormShowErrorStatus":
                standardFrameworkString(state, "loginFormShowErrorStatus"),
            "loginFormEnabledStatus":
                standardFrameworkString(state, "loginFormEnabledStatus"),
            "loginFormEnabledDisabled":
                state["loginFormEnabledDisabled"] as? Bool == true,
            "loginFormFullI18nRendered":
                state["loginFormFullI18nRendered"] as? Bool == true,
            "loginFormFullI18nStatus":
                standardFrameworkString(state, "loginFormFullI18nStatus"),
            "loginFormAutofillAttributesObserved":
                state["loginFormAutofillAttributesObserved"] as? Bool == true,
            "loginFormA11ySemanticsObserved":
                state["loginFormA11ySemanticsObserved"] as? Bool == true,
            "loginFormA11ySemanticsStatus":
                standardFrameworkString(state, "loginFormA11ySemanticsStatus"),
            "loginOverlayNoAutofocusObserved":
                state["loginOverlayNoAutofocusObserved"] as? Bool == true,
            "loginOverlayNoAutofocusStatus":
                standardFrameworkString(state, "loginOverlayNoAutofocusStatus"),
            "loginOverlayAutofocusControlObserved":
                state["loginOverlayAutofocusControlObserved"] as? Bool == true,
            "loginOverlayAutofocusControlOpened":
                state["loginOverlayAutofocusControlOpened"] as? Bool == true,
            "loginOverlayAutofocusControlClosed":
                state["loginOverlayAutofocusControlClosed"] as? Bool == true,
            "loginOverlayAutofocusControlProperty":
                state["loginOverlayAutofocusControlProperty"] as? Bool == true,
            "loginOverlayAutofocusControlUsernameFocused":
                state["loginOverlayAutofocusControlUsernameFocused"] as? Bool == true,
            "loginOverlayAutofocusControlActiveElementPath":
                standardFrameworkString(state, "loginOverlayAutofocusControlActiveElementPath"),
            "loginOverlayAutofocusControlStatus":
                standardFrameworkString(state, "loginOverlayAutofocusControlStatus"),
            "loginOverlayAutofillAttributesObserved":
                state["loginOverlayAutofillAttributesObserved"] as? Bool == true,
            "loginOverlayAutofillAttributesStatus":
                standardFrameworkString(state, "loginOverlayAutofillAttributesStatus"),
            "loginOverlayA11ySemanticsObserved":
                state["loginOverlayA11ySemanticsObserved"] as? Bool == true,
            "loginOverlayA11ySemanticsStatus":
                standardFrameworkString(state, "loginOverlayA11ySemanticsStatus"),
            "loginOverlayOutsideClickPreventedObserved":
                state["loginOverlayOutsideClickPreventedObserved"] as? Bool == true,
            "loginOverlayOutsideCloseStatus":
                standardFrameworkString(state, "loginOverlayOutsideCloseStatus"),
            "loginOverlayEscapeKeyPreventedObserved":
                state["loginOverlayEscapeKeyPreventedObserved"] as? Bool == true,
            "loginOverlayEscapeKeyStatus":
                standardFrameworkString(state, "loginOverlayEscapeKeyStatus"),
            "loginOverlayFullI18nRendered":
                state["loginOverlayFullI18nRendered"] as? Bool == true,
            "loginOverlayFullI18nStatus":
                standardFrameworkString(state, "loginOverlayFullI18nStatus"),
            "richContentStatus": standardFrameworkString(state, "richContentStatus"),
            "status": standardFrameworkString(state, "status")
        ]
        if !success {
            payload["contextMenuOpened"] = state["contextMenuOpened"] as? Bool == true
            payload["contextMenuItemRendered"] = state["contextMenuItemRendered"] as? Bool == true
            payload["gridContextMenuOpened"] = state["gridContextMenuOpened"] as? Bool == true
            payload["gridContextMenuItemRendered"] =
                state["gridContextMenuItemRendered"] as? Bool == true
            payload["loginOverlayOpened"] = state["loginOverlayOpened"] as? Bool == true
            payload["loginOverlayControlsRendered"] =
                state["loginOverlayControlsRendered"] as? Bool == true
            payload["loginFormControlsRendered"] =
                state["loginFormControlsRendered"] as? Bool == true
            payload["loginFormStatus"] = standardFrameworkString(state, "loginFormStatus")
            payload["loginOverlayActionPostOpened"] =
                state["loginOverlayActionPostOpened"] as? Bool == true
            payload["loginOverlayActionPostClosed"] =
                state["loginOverlayActionPostClosed"] as? Bool == true
            payload["loginOverlayActionPostControlsRendered"] =
                state["loginOverlayActionPostControlsRendered"] as? Bool == true
            payload["loginOverlayActionPostStatus"] =
                standardFrameworkString(state, "loginOverlayActionPostStatus")
            payload["loginFormActionPostControlsRendered"] =
                state["loginFormActionPostControlsRendered"] as? Bool == true
            payload["loginFormActionPostStatus"] =
                standardFrameworkString(state, "loginFormActionPostStatus")
            payload["loginOverlaySubmitButtonStatus"] =
                standardFrameworkString(state, "loginOverlaySubmitButtonStatus")
            payload["loginFormSubmitButtonObserved"] =
                state["loginFormSubmitButtonObserved"] as? Bool == true
            payload["loginFormButtonStatus"] =
                standardFrameworkString(state, "loginFormButtonStatus")
            payload["loginFormShowErrorStatus"] =
                standardFrameworkString(state, "loginFormShowErrorStatus")
            payload["loginFormEnabledStatus"] =
                standardFrameworkString(state, "loginFormEnabledStatus")
            payload["loginFormEnabledDisabled"] =
                state["loginFormEnabledDisabled"] as? Bool == true
            payload["loginFormFullI18nRendered"] =
                state["loginFormFullI18nRendered"] as? Bool == true
            payload["loginFormFullI18nStatus"] =
                standardFrameworkString(state, "loginFormFullI18nStatus")
            payload["loginFormFullI18nText"] =
                standardFrameworkString(state, "loginFormFullI18nText")
            payload["loginFormAutofillAttributesObserved"] =
                state["loginFormAutofillAttributesObserved"] as? Bool == true
            payload["loginFormAutofillUsernameAutocomplete"] =
                standardFrameworkString(state, "loginFormAutofillUsernameAutocomplete")
            payload["loginFormAutofillPasswordAutocomplete"] =
                standardFrameworkString(state, "loginFormAutofillPasswordAutocomplete")
            payload["loginFormA11ySemanticsObserved"] =
                state["loginFormA11ySemanticsObserved"] as? Bool == true
            payload["loginFormA11ySemanticsStatus"] =
                standardFrameworkString(state, "loginFormA11ySemanticsStatus")
            payload["loginFormA11yText"] =
                standardFrameworkString(state, "loginFormA11yText")
            payload["loginFormA11yUsernameLabel"] =
                standardFrameworkString(state, "loginFormA11yUsernameLabel")
            payload["loginFormA11yPasswordLabel"] =
                standardFrameworkString(state, "loginFormA11yPasswordLabel")
            payload["loginFormA11yUsernameName"] =
                standardFrameworkString(state, "loginFormA11yUsernameName")
            payload["loginFormA11yPasswordName"] =
                standardFrameworkString(state, "loginFormA11yPasswordName")
            payload["loginFormA11yPasswordInputType"] =
                standardFrameworkString(state, "loginFormA11yPasswordInputType")
            payload["loginOverlayNoAutofocusOpened"] =
                state["loginOverlayNoAutofocusOpened"] as? Bool == true
            payload["loginOverlayNoAutofocusClosed"] =
                state["loginOverlayNoAutofocusClosed"] as? Bool == true
            payload["loginOverlayNoAutofocusProperty"] =
                state["loginOverlayNoAutofocusProperty"] as? Bool == true
            payload["loginOverlayNoAutofocusFieldFocused"] =
                state["loginOverlayNoAutofocusFieldFocused"] as? Bool == true
            payload["loginOverlayNoAutofocusStatus"] =
                standardFrameworkString(state, "loginOverlayNoAutofocusStatus")
            payload["loginOverlayOutsideCloseObserved"] =
                state["loginOverlayOutsideCloseObserved"] as? Bool == true
            payload["loginOverlayOutsideClickPreventedObserved"] =
                state["loginOverlayOutsideClickPreventedObserved"] as? Bool == true
            payload["loginOverlayOutsideCloseOpened"] =
                state["loginOverlayOutsideCloseOpened"] as? Bool == true
            payload["loginOverlayOutsideClosed"] =
                state["loginOverlayOutsideClosed"] as? Bool == true
            payload["loginOverlayOutsideCloseStatus"] =
                standardFrameworkString(state, "loginOverlayOutsideCloseStatus")
            payload["loginOverlayOutsideClickDispatched"] =
                state["loginOverlayOutsideClickDispatched"] as? Bool == true
            payload["loginOverlayOutsideClickEventObserved"] =
                state["loginOverlayOutsideClickEventObserved"] as? Bool == true
            payload["loginOverlayOutsideClickDefaultPrevented"] =
                state["loginOverlayOutsideClickDefaultPrevented"] as? Bool == true
            payload["loginOverlayOutsideClickPreventedMarker"] =
                state["loginOverlayOutsideClickPreventedMarker"] as? Bool == true
            payload["loginOverlayOutsideClickTarget"] =
                standardFrameworkString(state, "loginOverlayOutsideClickTarget")
            payload["loginOverlayOutsideClickEventTarget"] =
                standardFrameworkString(state, "loginOverlayOutsideClickEventTarget")
            payload["loginOverlayOutsideClickError"] =
                standardFrameworkString(state, "loginOverlayOutsideClickError")
            payload["loginOverlayEscapeKeyObserved"] =
                state["loginOverlayEscapeKeyObserved"] as? Bool == true
            payload["loginOverlayEscapeKeyPreventedObserved"] =
                state["loginOverlayEscapeKeyPreventedObserved"] as? Bool == true
            payload["loginOverlayEscapeKeyOpened"] =
                state["loginOverlayEscapeKeyOpened"] as? Bool == true
            payload["loginOverlayEscapeKeyClosed"] =
                state["loginOverlayEscapeKeyClosed"] as? Bool == true
            payload["loginOverlayEscapeKeyStatus"] =
                standardFrameworkString(state, "loginOverlayEscapeKeyStatus")
            payload["loginOverlayEscapeKeyDispatched"] =
                state["loginOverlayEscapeKeyDispatched"] as? Bool == true
            payload["loginOverlayEscapeKeyEventObserved"] =
                state["loginOverlayEscapeKeyEventObserved"] as? Bool == true
            payload["loginOverlayEscapeKeyDefaultPrevented"] =
                state["loginOverlayEscapeKeyDefaultPrevented"] as? Bool == true
            payload["loginOverlayEscapeKeyPreventedMarker"] =
                state["loginOverlayEscapeKeyPreventedMarker"] as? Bool == true
            payload["loginOverlayEscapeKeyTarget"] =
                standardFrameworkString(state, "loginOverlayEscapeKeyTarget")
            payload["loginOverlayEscapeKeyEventTarget"] =
                standardFrameworkString(state, "loginOverlayEscapeKeyEventTarget")
            payload["loginOverlayEscapeKeyError"] =
                standardFrameworkString(state, "loginOverlayEscapeKeyError")
            payload["loginOverlayAutofillAttributesOpened"] =
                state["loginOverlayAutofillAttributesOpened"] as? Bool == true
            payload["loginOverlayAutofillAttributesClosed"] =
                state["loginOverlayAutofillAttributesClosed"] as? Bool == true
            payload["loginOverlayAutofillUsernameAutocomplete"] =
                standardFrameworkString(state, "loginOverlayAutofillUsernameAutocomplete")
            payload["loginOverlayAutofillPasswordAutocomplete"] =
                standardFrameworkString(state, "loginOverlayAutofillPasswordAutocomplete")
            payload["loginOverlayAutofillAttributesStatus"] =
                standardFrameworkString(state, "loginOverlayAutofillAttributesStatus")
            payload["loginOverlayA11ySemanticsOpened"] =
                state["loginOverlayA11ySemanticsOpened"] as? Bool == true
            payload["loginOverlayA11ySemanticsClosed"] =
                state["loginOverlayA11ySemanticsClosed"] as? Bool == true
            payload["loginOverlayA11ySemanticsStatus"] =
                standardFrameworkString(state, "loginOverlayA11ySemanticsStatus")
            payload["loginOverlayA11yText"] =
                standardFrameworkString(state, "loginOverlayA11yText")
            payload["loginOverlayA11yUsernameLabel"] =
                standardFrameworkString(state, "loginOverlayA11yUsernameLabel")
            payload["loginOverlayA11yPasswordLabel"] =
                standardFrameworkString(state, "loginOverlayA11yPasswordLabel")
            payload["loginOverlayA11yUsernameName"] =
                standardFrameworkString(state, "loginOverlayA11yUsernameName")
            payload["loginOverlayA11yPasswordName"] =
                standardFrameworkString(state, "loginOverlayA11yPasswordName")
            payload["loginOverlayA11yPasswordInputType"] =
                standardFrameworkString(state, "loginOverlayA11yPasswordInputType")
            payload["loginOverlayFullI18nOpened"] =
                state["loginOverlayFullI18nOpened"] as? Bool == true
            payload["loginOverlayFullI18nRendered"] =
                state["loginOverlayFullI18nRendered"] as? Bool == true
            payload["loginOverlayFullI18nStatus"] =
                standardFrameworkString(state, "loginOverlayFullI18nStatus")
            payload["loginOverlayFullI18nText"] =
                standardFrameworkString(state, "loginOverlayFullI18nText")
            payload["navigationMatrixStatus"] =
                standardFrameworkString(state, "navigationMatrixStatus")
            payload["formLayoutClickObserved"] =
                state["formLayoutClickObserved"] as? Bool == true
            payload["formLayoutServerObserved"] =
                state["formLayoutServerObserved"] as? Bool == true
            payload["formLayoutStatus"] = standardFrameworkString(state, "formLayoutStatus")
            payload["formLayoutColspan"] = standardFrameworkString(state, "formLayoutColspan")
            payload["formLayoutMaxColumns"] =
                standardFrameworkInteger(state, "formLayoutMaxColumns")
            payload["formLayoutResponsiveSteps"] =
                standardFrameworkString(state, "formLayoutResponsiveSteps")
            payload["formLayoutColumnWidth"] =
                standardFrameworkString(state, "formLayoutColumnWidth")
            payload["formLayoutLabelWidth"] =
                standardFrameworkString(state, "formLayoutLabelWidth")
            payload["formLayoutColumnSpacing"] =
                standardFrameworkString(state, "formLayoutColumnSpacing")
            payload["formLayoutRowSpacing"] =
                standardFrameworkString(state, "formLayoutRowSpacing")
            payload["horizontalLayoutClickObserved"] =
                state["horizontalLayoutClickObserved"] as? Bool == true
            payload["horizontalLayoutServerObserved"] =
                state["horizontalLayoutServerObserved"] as? Bool == true
            payload["horizontalLayoutStatus"] =
                standardFrameworkString(state, "horizontalLayoutStatus")
            payload["horizontalLayoutTheme"] =
                standardFrameworkString(state, "horizontalLayoutTheme")
            payload["horizontalLayoutJustify"] =
                standardFrameworkString(state, "horizontalLayoutJustify")
            payload["horizontalLayoutAlignItems"] =
                standardFrameworkString(state, "horizontalLayoutAlignItems")
            payload["horizontalLayoutGap"] =
                standardFrameworkString(state, "horizontalLayoutGap")
            payload["horizontalLayoutBoxSizing"] =
                standardFrameworkString(state, "horizontalLayoutBoxSizing")
            payload["horizontalLayoutWidth"] =
                standardFrameworkString(state, "horizontalLayoutWidth")
            payload["horizontalLayoutGrowStyle"] =
                standardFrameworkInteger(state, "horizontalLayoutGrowStyle")
            payload["horizontalLayoutTailAlignSelf"] =
                standardFrameworkString(state, "horizontalLayoutTailAlignSelf")
            payload["flexLayoutClickObserved"] =
                state["flexLayoutClickObserved"] as? Bool == true
            payload["flexLayoutServerObserved"] =
                state["flexLayoutServerObserved"] as? Bool == true
            payload["flexLayoutStatus"] =
                standardFrameworkString(state, "flexLayoutStatus")
            payload["flexLayoutFlexWrap"] =
                standardFrameworkString(state, "flexLayoutFlexWrap")
            payload["flexLayoutFlexDirection"] =
                standardFrameworkString(state, "flexLayoutFlexDirection")
            payload["flexLayoutAlignContent"] =
                standardFrameworkString(state, "flexLayoutAlignContent")
            payload["flexLayoutJustify"] =
                standardFrameworkString(state, "flexLayoutJustify")
            payload["flexLayoutAlignItems"] =
                standardFrameworkString(state, "flexLayoutAlignItems")
            payload["flexLayoutGap"] =
                standardFrameworkString(state, "flexLayoutGap")
            payload["flexLayoutWidth"] =
                standardFrameworkString(state, "flexLayoutWidth")
            payload["flexLayoutBetaBasis"] =
                standardFrameworkString(state, "flexLayoutBetaBasis")
            payload["flexLayoutAlphaOrder"] =
                standardFrameworkInteger(state, "flexLayoutAlphaOrder")
            payload["flexLayoutGammaOrder"] =
                standardFrameworkInteger(state, "flexLayoutGammaOrder")
            payload["flexLayoutBetaGrow"] =
                standardFrameworkInteger(state, "flexLayoutBetaGrow")
            payload["verticalLayoutClickObserved"] =
                state["verticalLayoutClickObserved"] as? Bool == true
            payload["verticalLayoutServerObserved"] =
                state["verticalLayoutServerObserved"] as? Bool == true
            payload["verticalLayoutStatus"] =
                standardFrameworkString(state, "verticalLayoutStatus")
            payload["verticalLayoutTheme"] =
                standardFrameworkString(state, "verticalLayoutTheme")
            payload["verticalLayoutJustify"] =
                standardFrameworkString(state, "verticalLayoutJustify")
            payload["verticalLayoutAlignItems"] =
                standardFrameworkString(state, "verticalLayoutAlignItems")
            payload["verticalLayoutGap"] =
                standardFrameworkString(state, "verticalLayoutGap")
            payload["verticalLayoutBoxSizing"] =
                standardFrameworkString(state, "verticalLayoutBoxSizing")
            payload["verticalLayoutWidth"] =
                standardFrameworkString(state, "verticalLayoutWidth")
            payload["verticalLayoutHeight"] =
                standardFrameworkString(state, "verticalLayoutHeight")
            payload["verticalLayoutBodyAlignSelf"] =
                standardFrameworkString(state, "verticalLayoutBodyAlignSelf")
            payload["verticalLayoutFooterGrow"] =
                standardFrameworkInteger(state, "verticalLayoutFooterGrow")
            payload["scrollerClientObserved"] =
                state["scrollerClientObserved"] as? Bool == true
            payload["scrollerServerObserved"] =
                state["scrollerServerObserved"] as? Bool == true
            payload["scrollerStatus"] =
                standardFrameworkString(state, "scrollerStatus")
            payload["scrollerTheme"] =
                standardFrameworkString(state, "scrollerTheme")
            payload["scrollerScrollDirection"] =
                standardFrameworkString(state, "scrollerScrollDirection")
            payload["scrollerScrollTop"] =
                standardFrameworkInteger(state, "scrollerScrollTop")
            payload["scrollerScrollHeight"] =
                standardFrameworkInteger(state, "scrollerScrollHeight")
            payload["scrollerClientHeight"] =
                standardFrameworkInteger(state, "scrollerClientHeight")
            payload["scrollerScrollWidth"] =
                standardFrameworkInteger(state, "scrollerScrollWidth")
            payload["scrollerClientWidth"] =
                standardFrameworkInteger(state, "scrollerClientWidth")
            payload["scrollerWidth"] =
                standardFrameworkString(state, "scrollerWidth")
            payload["scrollerHeight"] =
                standardFrameworkString(state, "scrollerHeight")
            payload["scrollerContentWidth"] =
                standardFrameworkString(state, "scrollerContentWidth")
            payload["scrollerContentHeight"] =
                standardFrameworkString(state, "scrollerContentHeight")
            payload["detailsOpened"] = state["detailsOpened"] as? Bool == true
            payload["lazyGridStatus"] = standardFrameworkString(state, "lazyGridStatus")
            payload["virtualListClientObserved"] =
                state["virtualListClientObserved"] as? Bool == true
            payload["virtualListServerObserved"] =
                state["virtualListServerObserved"] as? Bool == true
            payload["virtualListStatus"] = standardFrameworkString(state, "virtualListStatus")
            payload["virtualListText"] = standardFrameworkString(state, "virtualListText")
            payload["popoverOpened"] = state["popoverOpened"] as? Bool == true
            payload["notificationOpenObserved"] =
                state["notificationOpenObserved"] as? Bool == true
            payload["notificationOpened"] = state["notificationOpened"] as? Bool == true
            payload["notificationStatus"] = standardFrameworkString(state, "notificationStatus")
            payload["notificationText"] = standardFrameworkString(state, "notificationText")
            payload["confirmDialogOpenObserved"] =
                state["confirmDialogOpenObserved"] as? Bool == true
            payload["confirmDialogConfirmObserved"] =
                state["confirmDialogConfirmObserved"] as? Bool == true
            payload["confirmDialogCancelObserved"] =
                state["confirmDialogCancelObserved"] as? Bool == true
            payload["confirmDialogRejectObserved"] =
                state["confirmDialogRejectObserved"] as? Bool == true
            payload["confirmDialogOpened"] = state["confirmDialogOpened"] as? Bool == true
            payload["confirmDialogStatus"] =
                standardFrameworkString(state, "confirmDialogStatus")
            payload["confirmDialogText"] = standardFrameworkString(state, "confirmDialogText")
            payload["progressBarValueObserved"] =
                state["progressBarValueObserved"] as? Bool == true
            payload["progressBarIndeterminateObserved"] =
                state["progressBarIndeterminateObserved"] as? Bool == true
            payload["progressBarIndeterminate"] =
                state["progressBarIndeterminate"] as? Bool == true
            payload["progressBarStatus"] = standardFrameworkString(state, "progressBarStatus")
            payload["dateTimePickerClientObserved"] =
                state["dateTimePickerClientObserved"] as? Bool == true
            payload["dateTimePickerServerObserved"] =
                state["dateTimePickerServerObserved"] as? Bool == true
            payload["dateTimePickerValue"] =
                standardFrameworkString(state, "dateTimePickerValue")
            payload["dateTimePickerStatus"] =
                standardFrameworkString(state, "dateTimePickerStatus")
            payload["numberFieldClientObserved"] =
                state["numberFieldClientObserved"] as? Bool == true
            payload["numberFieldServerObserved"] =
                state["numberFieldServerObserved"] as? Bool == true
            payload["numberFieldValue"] = standardFrameworkString(state, "numberFieldValue")
            payload["numberFieldStatus"] = standardFrameworkString(state, "numberFieldStatus")
            payload["splitLayoutDragObserved"] =
                state["splitLayoutDragObserved"] as? Bool == true
            payload["splitLayoutServerObserved"] =
                state["splitLayoutServerObserved"] as? Bool == true
            payload["splitPrimaryFlex"] = standardFrameworkString(state, "splitPrimaryFlex")
            payload["splitSecondaryFlex"] = standardFrameworkString(state, "splitSecondaryFlex")
            payload["splitLayoutStatus"] =
                standardFrameworkString(state, "splitLayoutStatus")
            payload["listBoxClientObserved"] =
                state["listBoxClientObserved"] as? Bool == true
            payload["listBoxServerObserved"] =
                state["listBoxServerObserved"] as? Bool == true
            payload["listBoxSelected"] = standardFrameworkInteger(state, "listBoxSelected")
            payload["listBoxStatus"] = standardFrameworkString(state, "listBoxStatus")
            payload["listBoxText"] = standardFrameworkString(state, "listBoxText")
            payload["multiSelectListBoxClientObserved"] =
                state["multiSelectListBoxClientObserved"] as? Bool == true
            payload["multiSelectListBoxServerObserved"] =
                state["multiSelectListBoxServerObserved"] as? Bool == true
            payload["multiSelectListBoxSelectedText"] =
                standardFrameworkString(state, "multiSelectListBoxSelectedText")
            payload["multiSelectListBoxStatus"] =
                standardFrameworkString(state, "multiSelectListBoxStatus")
            payload["multiSelectListBoxText"] =
                standardFrameworkString(state, "multiSelectListBoxText")
            payload["customFieldClientObserved"] =
                state["customFieldClientObserved"] as? Bool == true
            payload["customFieldServerObserved"] =
                state["customFieldServerObserved"] as? Bool == true
            payload["customFieldOwnerValue"] =
                standardFrameworkString(state, "customFieldOwnerValue")
            payload["customFieldStateValue"] =
                standardFrameworkString(state, "customFieldStateValue")
            payload["customFieldStatus"] = standardFrameworkString(state, "customFieldStatus")
            payload["uploadControlsRendered"] =
                state["uploadControlsRendered"] as? Bool == true
            payload["uploadStatus"] = standardFrameworkString(state, "uploadStatus")
            payload["resourceFetchComplete"] =
                state["resourceFetchComplete"] as? Bool == true
            payload["resourceFetchStatus"] =
                standardFrameworkString(state, "resourceFetchStatus")
            payload["resourceFetchError"] =
                standardFrameworkString(state, "resourceFetchError")
            payload["streamResourceFetchOk"] =
                state["streamResourceFetchOk"] as? Bool == true
            payload["binaryStreamResourceFetchOk"] =
                state["binaryStreamResourceFetchOk"] as? Bool == true
            payload["downloadHandlerFetchOk"] =
                state["downloadHandlerFetchOk"] as? Bool == true
            payload["standardStreamingDownloadFetchOk"] =
                state["standardStreamingDownloadFetchOk"] as? Bool == true
            payload["standardStreamingDownloadBytes"] =
                standardFrameworkInteger(state, "standardStreamingDownloadBytes")
            payload["richTextEditorStatus"] =
                standardFrameworkString(state, "richTextEditorStatus")
            payload["richTextEditorBehaviorStatus"] =
                standardFrameworkString(state, "richTextEditorBehaviorStatus")
            payload["richTextEditorEditingStatus"] =
                standardFrameworkString(state, "richTextEditorEditingStatus")
            payload["richTextEditorHtmlValue"] =
                standardFrameworkString(state, "richTextEditorHtmlValue")
            payload["richTextEditorValue"] =
                standardFrameworkString(state, "richTextEditorValue")
        }
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
      element.dispatchEvent(new CustomEvent('checked-changed', {
        bubbles: true,
        composed: true,
        detail: { value: true }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkCheckboxGroupClientAction = """
    (() => {
      const element = document.querySelector('#framework-checkbox-group');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeepAll = (root, predicate) => {
        const results = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === 1 && predicate(node)) results.push(node);
          if (node.shadowRoot) visit(node.shadowRoot);
          const children = node.children || node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(root);
        return results;
      };
      const textFor = (candidate) => String([
        candidate.value,
        candidate.getAttribute?.('value'),
        candidate.getAttribute?.('label'),
        candidate.textContent
      ].filter(Boolean).join(' ')).toLowerCase();
      const wanted = ['compliance', 'qa'];
      const boxes = findDeepAll(element, (candidate) =>
        candidate.localName === 'vaadin-checkbox');
      let changed = 0;
      for (const value of wanted) {
        const checkbox = boxes.find((candidate) => textFor(candidate).includes(value));
        if (!checkbox) return false;
        if (checkbox.checked !== true && checkbox.hasAttribute?.('checked') !== true) {
          checkbox.click();
          changed += 1;
        }
      }
      return changed === wanted.length;
    })()
    """

    private static let standardFrameworkCheckboxGroupServerAction = """
    (() => {
      const button = document.querySelector('#framework-checkbox-group-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkPriorityAction = """
    (() => {
      const element = document.querySelector('#framework-priority');
      if (!element) return false;
      const wanted = 'audit';
      const itemText = (item) => String(
        item?.label
          ?? item?.textContent
          ?? item?.getAttribute?.('label')
          ?? item?.getAttribute?.('value')
          ?? '');
      const applyItem = (item) => {
        if (!item || itemText(item).trim() !== wanted) return false;
        const key = String(item.value ?? item.getAttribute?.('value') ?? '');
        if (!key) return false;
        element.value = key;
        item.click();
        element.dispatchEvent(new CustomEvent('value-changed', {
          bubbles: true,
          composed: true,
          detail: { value: key }
        }));
        element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        return true;
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const candidates = [
        ...element.querySelectorAll('vaadin-select-item, vaadin-item, [role="option"]'),
        ...(element.shadowRoot
          ? element.shadowRoot.querySelectorAll('vaadin-select-item, vaadin-item, [role="option"]')
          : [])
      ];
      if (candidates.some(applyItem)) return true;
      return false;
    })()
    """

    private static let standardFrameworkReviewerAction = """
    (() => {
      const element = document.querySelector('#framework-reviewer');
      if (!element) return false;
      const wanted = 'audit';
      const itemText = (item) => String(
        item?.label ?? item?.value ?? item?.text ?? item?.id ?? item ?? '');
      const applyItem = (item) => {
        if (!item || itemText(item) !== wanted) return false;
        const key = String(item.key ?? item.value ?? wanted);
        element.selectedItem = item;
        element.value = key;
        element.dispatchEvent(new CustomEvent('selected-item-changed', {
          bubbles: true,
          composed: true,
          detail: { value: item }
        }));
        element.dispatchEvent(new CustomEvent('value-changed', {
          bubbles: true,
          composed: true,
          detail: { value: key }
        }));
        element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        return true;
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const existingItems = [
        ...(Array.isArray(element.items) ? element.items : []),
        ...(Array.isArray(element.filteredItems) ? element.filteredItems : [])
      ];
      if (existingItems.some(applyItem) || applyItem(element.selectedItem)) {
        return true;
      }
      if (typeof element.dataProvider === 'function') {
        element.dataProvider({ page: 0, pageSize: 50, filter: '' }, (items) => {
          Array.from(items || []).some(applyItem);
        });
        return true;
      }
      return false;
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

    private static let standardFrameworkRadioGroupAction = """
    (() => {
      const element = document.querySelector('#framework-decision');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const radio = findDeep(element, (candidate) => {
        const text = (candidate.textContent || '').trim().toLowerCase();
        const value = String(candidate.value || '').toLowerCase();
        return candidate.localName === 'vaadin-radio-button'
          && (value === 'approve' || text.includes('approve'));
      });
      if (radio && typeof radio.click === 'function') {
        radio.click();
        return true;
      }
      element.value = 'approve';
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value: 'approve' }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkTextFieldClientAction = """
    (() => {
      const element = document.querySelector('#framework-case-title');
      if (!element) return false;
      const value = 'Flow IPC title';
      const setNativeInputValue = (target) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, value);
        } else {
          target.value = value;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        setNativeInputValue(element.inputElement);
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkTextFieldServerAction = """
    (() => {
      const button = document.querySelector('#framework-text-field-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkTextAreaAction = """
    (() => {
      const element = document.querySelector('#framework-notes');
      if (!element) return false;
      const value = 'Flow IPC notes updated';
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        element.inputElement.value = value;
        element.inputElement.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        element.inputElement.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkEmailFieldAction = """
    (() => {
      const element = document.querySelector('#framework-contact-email');
      if (!element) return false;
      const value = 'owner+ipc@example.test';
      const setNativeInputValue = (target) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, value);
        } else {
          target.value = value;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        setNativeInputValue(element.inputElement);
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkIntegerFieldAction = """
    (() => {
      const element = document.querySelector('#framework-score');
      if (!element) return false;
      const value = '7';
      const setNativeInputValue = (target) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, value);
        } else {
          target.value = value;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        setNativeInputValue(element.inputElement);
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkNumberFieldClientAction = """
    (() => {
      const element = document.querySelector('#framework-risk-score');
      if (!element) return false;
      const value = '4.75';
      const setNativeInputValue = (target) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, value);
        } else {
          target.value = value;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        setNativeInputValue(element.inputElement);
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkNumberFieldServerAction = """
    (() => {
      const button = document.querySelector('#framework-number-field-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkPasswordFieldAction = """
    (() => {
      const element = document.querySelector('#framework-secret');
      if (!element) return false;
      const value = 'secret-ipc';
      const setNativeInputValue = (target) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, value);
        } else {
          target.value = value;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        setNativeInputValue(element.inputElement);
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkTimePickerAction = """
    (() => {
      const element = document.querySelector('#framework-review-time');
      if (!element) return false;
      const value = '14:45';
      const setNativeInputValue = (target) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, value);
        } else {
          target.value = value;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.inputElement) {
        setNativeInputValue(element.inputElement);
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkDateTimePickerClientAction = """
    (() => {
      const element = document.querySelector('#framework-review-date-time');
      if (!element) return false;
      const value = '2026-07-14T15:30';
      const setNativeInputValue = (target, nextValue) => {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(target, nextValue);
        } else {
          target.value = nextValue;
        }
        target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      if (element.datePicker) {
        element.datePicker.value = '2026-07-14';
        if (element.datePicker.inputElement) {
          setNativeInputValue(element.datePicker.inputElement, '2026-07-14');
        }
        element.datePicker.dispatchEvent(new Event('change', {
          bubbles: true,
          composed: true
        }));
      }
      if (element.timePicker) {
        element.timePicker.value = '15:30';
        if (element.timePicker.inputElement) {
          setNativeInputValue(element.timePicker.inputElement, '15:30');
        }
        element.timePicker.dispatchEvent(new Event('change', {
          bubbles: true,
          composed: true
        }));
      }
      element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkDateTimePickerServerAction = """
    (() => {
      const button = document.querySelector('#framework-date-time-picker-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkSplitLayoutDragAction = """
    (() => {
      const element = document.querySelector('#framework-split-layout');
      const primary = document.querySelector('#framework-split-primary');
      const secondary = document.querySelector('#framework-split-secondary');
      if (!element || !primary || !secondary) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      primary.style.flexBasis = '35%';
      secondary.style.flexBasis = '65%';
      element.dispatchEvent(new CustomEvent('splitter-dragend', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkSplitLayoutServerAction = """
    (() => {
      const button = document.querySelector('#framework-split-layout-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkAssigneesAction = """
    (() => {
      const element = document.querySelector('#framework-assignees');
      if (!element) return false;
      const wanted = new Set(['ava', 'sol']);
      const itemText = (item) => String(
        item?.label ?? item?.value ?? item?.text ?? item?.id ?? item ?? '');
      const applyItems = (items) => {
        const value = Array.from(items || [])
          .filter((item) => wanted.has(itemText(item)));
        if (value.length !== wanted.size) return false;
        element.selectedItems = value;
        element.dispatchEvent(new CustomEvent('selected-items-changed', {
          bubbles: true,
          composed: true,
          detail: { value }
        }));
        element.dispatchEvent(new CustomEvent('value-changed', {
          bubbles: true,
          composed: true,
          detail: { value }
        }));
        return true;
      };
      element.scrollIntoView({ block: 'center', inline: 'center' });
      if (applyItems(element.items)
          || applyItems(element.filteredItems)
          || applyItems(element.selectedItems)) {
        return true;
      }
      if (typeof element.dataProvider === 'function') {
        element.dataProvider({ page: 0, pageSize: 50, filter: '' }, (items) => {
          applyItems(items);
        });
        return true;
      }
      return false;
    })()
    """

    private static let standardFrameworkListBoxClientAction = """
    (() => {
      const element = document.querySelector('#framework-list-box');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const items = Array.from(element.querySelectorAll('vaadin-item'));
      const target = items.find((item) =>
        (item.textContent || '').includes('Build lane')) || items[1];
      if (target && typeof target.click === 'function') {
        target.click();
      }
      element.selected = 1;
      element.dispatchEvent(new CustomEvent('selected-changed', {
        bubbles: true,
        composed: true,
        detail: { value: 1 }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkListBoxServerAction = """
    (() => {
      const button = document.querySelector('#framework-list-box-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkMultiSelectListBoxClientAction = """
    (() => {
      const element = document.querySelector('#framework-multi-select-list-box');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const value = [0, 2];
      element.selectedValues = value;
      element.dispatchEvent(new CustomEvent('selected-values-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkMultiSelectListBoxServerAction = """
    (() => {
      const button = document.querySelector('#framework-multi-select-list-box-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkCustomFieldClientAction = """
    (() => {
      const field = document.querySelector('#framework-custom-field');
      const owner = document.querySelector('#framework-custom-field-owner');
      const state = document.querySelector('#framework-custom-field-state');
      if (!field || !owner || !state) return false;
      const setFieldValue = (element, value) => {
        const setNativeInputValue = (target, nextValue) => {
          const descriptor = Object.getOwnPropertyDescriptor(
            HTMLInputElement.prototype, 'value');
          if (descriptor?.set) {
            descriptor.set.call(target, nextValue);
          } else {
            target.value = nextValue;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        element.value = value;
        if (element.inputElement) {
          setNativeInputValue(element.inputElement, value);
        }
        element.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      };
      field.scrollIntoView({ block: 'center', inline: 'center' });
      setFieldValue(owner, 'edge');
      setFieldValue(state, 'ready');
      field.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkCustomFieldServerAction = """
    (() => {
      const button = document.querySelector('#framework-custom-field-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
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
      const action = document.querySelector('#framework-validation-action');
      const field = document.querySelector('#framework-owner-name');
      if (!action || typeof action.click !== 'function' || !field) return false;
      const value = 'desktop';
      field.scrollIntoView({ block: 'center', inline: 'center' });
      field.value = value;
      if (field.inputElement) {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(field.inputElement, value);
        } else {
          field.inputElement.value = value;
        }
        field.inputElement.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        field.inputElement.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      }
      field.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      field.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      field.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkFormLayoutClickAction = """
    (() => {
      const element = document.querySelector('#framework-form-layout');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkFormLayoutServerAction = """
    (() => {
      const button = document.querySelector('#framework-form-layout-server-update');
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkHorizontalLayoutClickAction = """
    (() => {
      const element = document.querySelector('#framework-horizontal-layout');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkHorizontalLayoutServerAction = """
    (() => {
      const button = document.querySelector('#framework-horizontal-layout-server-update');
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkFlexLayoutClickAction = """
    (() => {
      const element = document.querySelector('#framework-flex-layout');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkFlexLayoutServerAction = """
    (() => {
      const button = document.querySelector('#framework-flex-layout-server-update');
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkVerticalLayoutClickAction = """
    (() => {
      const element = document.querySelector('#framework-vertical-layout');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkVerticalLayoutServerAction = """
    (() => {
      const button = document.querySelector('#framework-vertical-layout-server-update');
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkScrollerClientAction = """
    (() => {
      const scroller = document.querySelector('#framework-scroller');
      if (!scroller) return false;
      scroller.scrollIntoView({ block: 'center', inline: 'center' });
      scroller.scrollTop = Math.max(120, scroller.scrollHeight - scroller.clientHeight);
      scroller.dispatchEvent(new Event('scroll', { bubbles: true, composed: true }));
      return scroller.scrollTop >= 80;
    })()
    """

    private static let standardFrameworkScrollerServerAction = """
    (() => {
      const button = document.querySelector('#framework-scroller-server-update');
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkAvatarAction = """
    (() => {
      const element = document.querySelector('#framework-avatar');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkMessageInputAction = """
    (() => {
      const element = document.querySelector('#framework-message-input');
      if (!element) return false;
      element.dispatchEvent(new CustomEvent('submit', {
        bubbles: true,
        composed: true,
        detail: { value: 'IPC note' }
      }));
      return true;
    })()
    """

    private static let standardFrameworkRichTextEditorAction = """
    (() => {
      const element = document.querySelector('#framework-rich-text-editor');
      if (!element) return false;
      const html = '<p>Flow IPC rich text <strong>bold</strong></p>';
      element.scrollIntoView({ block: 'center', inline: 'center' });
      if (element._editor?.clipboard?.convert && element._editor?.setContents) {
        const delta = element._editor.clipboard.convert({ html });
        element._editor.setContents(delta, 'user');
        element._editor.setSelection(element._editor.getLength(), 0, 'user');
      } else if (typeof element.dangerouslySetHtmlValue === 'function') {
        element.dangerouslySetHtmlValue(html);
      } else {
        return false;
      }
      setTimeout(() => {
        element.__debounceSetValue?.flush?.();
        element.__updateHtmlValue?.();
        element.__emitChangeEvent?.();
        element.dispatchEvent(new CustomEvent('value-changed', {
          bubbles: true,
          composed: true,
          detail: { value: html }
        }));
        element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      }, 300);
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayOpenAction = """
    (() => {
      const element = document.querySelector('#framework-login-overlay-reopen');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlaySubmitAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay');
      if (!overlay) return false;
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(overlay, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(overlay, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(overlay, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(overlay, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(overlay, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'ipc-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayForgotPasswordAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay');
      if (!overlay) return false;
      const collectDeepText = (root) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '').replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.nodeType !== Node.ELEMENT_NODE
              && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
              && node.nodeType !== Node.DOCUMENT_NODE) {
            return;
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          for (const child of node.childNodes || []) visit(child);
        };
        visit(root);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const forgot = findDeep(overlay, (element) => {
        if (element.localName !== 'vaadin-button' && element.localName !== 'button') {
          return false;
        }
        const text = collectDeepText(element);
        return text.includes('Recover Flow IPC access')
          || text.toLowerCase().includes('forgot');
      });
      if (!forgot || typeof forgot.click !== 'function') return false;
      forgot.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayShowErrorAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay');
      if (!overlay) return false;
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(overlay, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(overlay, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(overlay, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(overlay, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(overlay, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'overlay-show-error');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayActionPostOpenAction = """
    (() => {
      const element = document.querySelector('#framework-login-overlay-action-post-open');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayActionPostSubmitAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-action-post');
      if (!overlay) return false;
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(overlay, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(overlay, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(overlay, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(overlay, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(overlay, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'overlay-action-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormSubmitAction = """
    (() => {
      const form = document.querySelector('#framework-login-form');
      if (!form) return false;
      form.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(form, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(form, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(form, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'inline-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormForgotPasswordAction = """
    (() => {
      const form = document.querySelector('#framework-login-form');
      if (!form) return false;
      form.scrollIntoView({ block: 'center', inline: 'center' });
      const collectDeepText = (root) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '').replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.nodeType !== Node.ELEMENT_NODE
              && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
              && node.nodeType !== Node.DOCUMENT_NODE) {
            return;
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          for (const child of node.childNodes || []) visit(child);
        };
        visit(root);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const forgot = findDeep(form, (element) => {
        if (element.localName !== 'vaadin-button' && element.localName !== 'button') {
          return false;
        }
        const text = collectDeepText(element);
        return text.includes('Recover Flow IPC access')
          || text.toLowerCase().includes('forgot');
      });
      if (!forgot || typeof forgot.click !== 'function') return false;
      forgot.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormActionPostSubmitAction = """
    (() => {
      const form = document.querySelector('#framework-login-form-action-post');
      if (!form) return false;
      form.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(form, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(form, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(form, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'inline-action-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormSubmitButtonAction = """
    (() => {
      const form = document.querySelector('#framework-login-form-button');
      if (!form) return false;
      form.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(form, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(form, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(form, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'button-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormShowErrorAction = """
    (() => {
      const form = document.querySelector('#framework-login-form-show-error');
      if (!form) return false;
      form.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(form, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(form, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(form, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'rejected-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormEnableAction = """
    (() => {
      const action = document.querySelector('#framework-login-form-enable');
      if (!action || typeof action.click !== 'function') return false;
      action.scrollIntoView({ block: 'center', inline: 'center' });
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginFormEnabledSubmitAction = """
    (() => {
      const form = document.querySelector('#framework-login-form-enabled');
      if (!form || form.disabled === true || form.hasAttribute('disabled')) return false;
      form.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const usernameField = findDeep(form, (element) =>
        element.localName === 'vaadin-text-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() !== 'password');
      const passwordField = findDeep(form, (element) =>
        element.localName === 'vaadin-password-field')
        || findDeep(form, (element) =>
          element.localName === 'input'
            && (element.type || '').toLowerCase() === 'password');
      const submit = findDeep(form, (element) =>
        (element.localName === 'vaadin-button'
          && (element.textContent || '').includes('Sign in'))
          || (element.localName === 'button'
            && (((element.type || '').toLowerCase() === 'submit')
              || (element.textContent || '').includes('Sign in'))));
      if (!usernameField || !passwordField || !submit || typeof submit.click !== 'function') {
        return false;
      }
      setValue(usernameField, 'enabled-user');
      setValue(passwordField, 'flowpass');
      submit.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginVariantsAction = """
    (() => {
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const setValue = (element, value) => {
        if (!element) return false;
        const apply = (target) => {
          if (target.localName === 'input') {
            const descriptor = Object.getOwnPropertyDescriptor(
              HTMLInputElement.prototype, 'value');
            if (descriptor?.set) {
              descriptor.set.call(target, value);
            } else {
              target.value = value;
            }
          } else {
            target.value = value;
          }
          target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
          target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        };
        apply(element);
        const input = element.localName === 'input'
          ? element
          : findDeep(element, (candidate) => candidate.localName === 'input');
        if (input && input !== element) {
          apply(input);
        }
        return true;
      };
      const controls = (container) => {
        const usernameField = findDeep(container, (element) =>
          element.localName === 'vaadin-text-field')
          || findDeep(container, (element) =>
            element.localName === 'input'
              && (element.type || '').toLowerCase() !== 'password');
        const passwordField = findDeep(container, (element) =>
          element.localName === 'vaadin-password-field')
          || findDeep(container, (element) =>
            element.localName === 'input'
              && (element.type || '').toLowerCase() === 'password');
        const submit = findDeep(container, (element) =>
          (element.localName === 'vaadin-button'
            && (element.textContent || '').includes('Sign in'))
            || (element.localName === 'button'
              && (((element.type || '').toLowerCase() === 'submit')
                || (element.textContent || '').includes('Sign in'))));
        const form = findDeep(container, (element) => element.localName === 'form');
        return { usernameField, passwordField, submit, form };
      };
      const submitLogin = (selector, username, password, statusSelector, expectedStatus) => {
        const container = document.querySelector(selector);
        if (!container) return false;
        container.scrollIntoView({ block: 'center', inline: 'center' });
        const emitLogin = () => {
          container.dispatchEvent(new CustomEvent('login', {
            bubbles: true,
            composed: true,
            detail: { username, password }
          }));
        };
        const parts = controls(container);
        if (!parts.usernameField || !parts.passwordField) {
          emitLogin();
          return true;
        }
        setValue(parts.usernameField, username);
        setValue(parts.passwordField, password);
        if (!parts.submit || typeof parts.submit.click !== 'function') {
          emitLogin();
          return true;
        }
        parts.submit.click();
        if (statusSelector && expectedStatus) {
          setTimeout(() => {
            const status = document.querySelector(statusSelector)?.textContent || '';
            if (!status.includes(expectedStatus)) {
              emitLogin();
            }
          }, 450);
        }
        return true;
      };
      const openOverlay = (statusSelector, eventName) => {
        const status = document.querySelector(statusSelector);
        if (!status) return false;
        status.dispatchEvent(new Event(eventName, { bubbles: true, composed: true }));
        return true;
      };
      const steps = [
        () => {
          if (!openOverlay(
              '#framework-login-overlay-submit-button-status',
              'vatori-login-overlay-submit-button-open')) return false;
          setTimeout(() => submitLogin(
            '#framework-login-overlay-submit-button',
            'overlay-button-user',
            'flowpass',
            '#framework-login-overlay-submit-button-status',
            'LoginOverlay submit button submitted:'), 500);
          setTimeout(() => {
            const overlay = document.querySelector('#framework-login-overlay-submit-button');
            if (overlay) {
              overlay.opened = false;
              overlay.removeAttribute('opened');
            }
          }, 850);
          return true;
        }
      ];
      window.VaadinDesktopFrameworkLoginVariants = { dispatched: true };
      steps.forEach((step, index) => {
        setTimeout(() => {
          try {
            step();
          } catch (error) {
            window.VaadinDesktopFrameworkLoginVariants.error = String(error?.message || error);
          }
        }, index * 900);
      });
      return true;
    })()
    """

    private static let standardFrameworkTabsAction = """
    (() => {
      const element = document.querySelector('#framework-nav-tab-audit');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayNoAutofocusOpenAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-no-autofocus');
      const status = document.querySelector('#framework-login-overlay-no-autofocus-status');
      if (!overlay || !status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      overlay.dispatchEvent(new Event('vatori-login-overlay-no-autofocus-open', {
        bubbles: true,
        composed: true
      }));
      window.VaadinDesktopFrameworkLoginOverlayNoAutofocus = { dispatched: true };
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayAutofocusControlOpenAction = """
    (() => {
      const host = document.querySelector('#framework-login-overlay-autofocus-control-host');
      const status = document.querySelector('#framework-login-overlay-autofocus-control-status');
      if (!host || !status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      status.dispatchEvent(new Event('vatori-login-overlay-autofocus-control-open', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayAutofocusControlCloseAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-autofocus-control');
      if (!overlay) return false;
      overlay.dispatchEvent(new Event('vatori-login-overlay-autofocus-control-close', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayNoAutofocusCloseAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-no-autofocus');
      if (!overlay) return false;
      overlay.dispatchEvent(new Event('vatori-login-overlay-no-autofocus-close', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayOutsideCloseOpenAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-outside-close');
      const status = document.querySelector('#framework-login-overlay-outside-close-status');
      if (!overlay || !status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      overlay.dispatchEvent(new Event('vatori-login-overlay-outside-open', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayOutsideClickAction = """
    (() => {
      const marker = {
        dispatched: false,
        eventObserved: false,
        defaultPrevented: false,
        preventedObserved: false,
        target: '',
        eventTarget: '',
        error: ''
      };
      window.VaadinDesktopFrameworkLoginOverlayOutsideClick = marker;
      const overlay = document.querySelector('#framework-login-overlay-outside-close');
      if (!overlay) {
        marker.error = 'overlay-not-found';
        return true;
      }
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const collectDeepText = (root) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '').replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.nodeType !== Node.ELEMENT_NODE
              && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE
              && node.nodeType !== Node.DOCUMENT_NODE) {
            return;
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          const children = node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(root);
        return parts.join(' ');
      };
      const wrapper = overlay.shadowRoot?.querySelector('#overlay')
        || overlay.shadowRoot?.querySelector('vaadin-login-overlay-wrapper')
        || findDeep(document, (element) => {
          if (element.localName !== 'vaadin-login-overlay-wrapper'
              && element.localName !== 'vaadin-login-form-wrapper') {
            return false;
          }
          return collectDeepText(element).includes('Flow IPC LoginOverlay outside close');
        });
      if (!wrapper) {
        marker.error = 'wrapper-not-found';
        return true;
      }
      const recordOutsideClick = (event) => {
        marker.eventObserved = true;
        marker.eventTarget =
          `${event.target?.localName || event.target?.nodeName || 'unknown'}#${event.target?.id || ''}`;
        setTimeout(() => {
          marker.defaultPrevented = marker.defaultPrevented || event.defaultPrevented === true;
          marker.preventedObserved =
            marker.preventedObserved || event.defaultPrevented === true;
        }, 0);
      };
      wrapper.addEventListener('vaadin-overlay-outside-click', recordOutsideClick, {
        once: true
      });
      wrapper.addEventListener('vaadin-overlay-outside-click', recordOutsideClick, {
        capture: true,
        once: true
      });
      const target = document.documentElement;
      const x = Math.max(2, Math.min(window.innerWidth - 2, 2));
      const y = Math.max(2, Math.min(window.innerHeight - 2, 2));
      marker.target = `${target.localName || target.nodeName || 'unknown'}#${target.id || ''}`;
      const eventOptions = {
        bubbles: true,
        composed: true,
        cancelable: true,
        clientX: x,
        clientY: y,
        button: 0,
        buttons: 1,
        view: window
      };
      try {
        if (typeof PointerEvent === 'function') {
          target.dispatchEvent(new PointerEvent('pointerdown', {
            ...eventOptions,
            pointerId: 1,
            pointerType: 'mouse',
            isPrimary: true
          }));
        }
        target.dispatchEvent(new MouseEvent('mousedown', eventOptions));
        if (typeof PointerEvent === 'function') {
          target.dispatchEvent(new PointerEvent('pointerup', {
            ...eventOptions,
            buttons: 0,
            pointerId: 1,
            pointerType: 'mouse',
            isPrimary: true
          }));
        }
        target.dispatchEvent(new MouseEvent('mouseup', { ...eventOptions, buttons: 0 }));
        target.dispatchEvent(new MouseEvent('click', { ...eventOptions, buttons: 0 }));
        marker.dispatched = true;
      } catch (error) {
        marker.error = String(error?.message || error || 'dispatch-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayOutsideCloseCleanupAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-outside-close');
      if (!overlay) return false;
      overlay.opened = false;
      overlay.removeAttribute('opened');
      overlay.dispatchEvent(new CustomEvent('opened-changed', {
        bubbles: true,
        composed: true,
        detail: { value: false }
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayEscapeKeyOpenAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-escape-key');
      const status = document.querySelector('#framework-login-overlay-escape-key-status');
      if (!overlay || !status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      overlay.dispatchEvent(new Event('vatori-login-overlay-escape-open', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayEscapeKeyAction = """
    (() => {
      const marker = {
        dispatched: false,
        eventObserved: false,
        defaultPrevented: false,
        preventedObserved: false,
        target: '',
        eventTarget: '',
        error: ''
      };
      window.VaadinDesktopFrameworkLoginOverlayEscapeKey = marker;
      const overlay = document.querySelector('#framework-login-overlay-escape-key');
      if (!overlay) {
        marker.error = 'overlay-not-found';
        return true;
      }
      const wrapper = overlay.shadowRoot?.querySelector('#overlay')
        || overlay.shadowRoot?.querySelector('vaadin-login-overlay-wrapper');
      if (!wrapper) {
        marker.error = 'wrapper-not-found';
        return true;
      }
      const recordEscapePress = (event) => {
        marker.eventObserved = true;
        marker.eventTarget =
          `${event.target?.localName || event.target?.nodeName || 'unknown'}#${event.target?.id || ''}`;
        setTimeout(() => {
          marker.defaultPrevented = marker.defaultPrevented || event.defaultPrevented === true;
          marker.preventedObserved =
            marker.preventedObserved || event.defaultPrevented === true;
        }, 0);
      };
      wrapper.addEventListener('vaadin-overlay-escape-press', recordEscapePress, {
        once: true
      });
      wrapper.addEventListener('vaadin-overlay-escape-press', recordEscapePress, {
        capture: true,
        once: true
      });
      const target = document;
      marker.target = '#document';
      try {
        target.dispatchEvent(new KeyboardEvent('keydown', {
          bubbles: true,
          composed: true,
          cancelable: true,
          key: 'Escape',
          code: 'Escape',
          keyCode: 27,
          which: 27
        }));
        marker.dispatched = true;
      } catch (error) {
        marker.error = String(error?.message || error || 'dispatch-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayEscapeKeyCleanupAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-escape-key');
      if (!overlay) return false;
      overlay.opened = false;
      overlay.removeAttribute('opened');
      overlay.dispatchEvent(new CustomEvent('opened-changed', {
        bubbles: true,
        composed: true,
        detail: { value: false }
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayAutofillAttributesOpenAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-autofill-attributes');
      const status = document.querySelector('#framework-login-overlay-autofill-attributes-status');
      if (!overlay || !status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      overlay.dispatchEvent(new Event('vatori-login-overlay-autofill-attributes-open', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayAutofillAttributesCloseAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-autofill-attributes');
      if (!overlay) return false;
      overlay.dispatchEvent(new Event('vatori-login-overlay-autofill-attributes-close', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayA11ySemanticsOpenAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-a11y-semantics');
      const status = document.querySelector('#framework-login-overlay-a11y-semantics-status');
      if (!overlay || !status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      overlay.dispatchEvent(new Event('vatori-login-overlay-a11y-semantics-open', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayA11ySemanticsCloseAction = """
    (() => {
      const overlay = document.querySelector('#framework-login-overlay-a11y-semantics');
      if (!overlay) return false;
      overlay.dispatchEvent(new Event('vatori-login-overlay-a11y-semantics-close', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginOverlayFullI18nAction = """
    (() => {
      const status = document.querySelector('#framework-login-overlay-i18n-full-status');
      if (!status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      status.dispatchEvent(new Event('vatori-login-overlay-i18n-full-open', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkLoginFormFullI18nAction = """
    (() => {
      const element = document.querySelector('#framework-login-form-i18n-full');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      return true;
    })()
    """

    private static let standardFrameworkLoginFormAutofillAttributesAction = """
    (() => {
      const element = document.querySelector('#framework-login-form-autofill-attributes');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      return true;
    })()
    """

    private static let standardFrameworkLoginFormA11ySemanticsAction = """
    (() => {
      const element = document.querySelector('#framework-login-form-a11y-semantics');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      return true;
    })()
    """

    private static let standardFrameworkLoginFormNoAutofocusAction = """
    (() => {
      const status = document.querySelector('#framework-login-form-no-autofocus-status');
      if (!status) return false;
      status.scrollIntoView({ block: 'center', inline: 'center' });
      status.setAttribute('tabindex', '-1');
      if (typeof status.focus === 'function') {
        status.focus({ preventScroll: true });
      }
      status.dispatchEvent(new Event('vatori-login-form-no-autofocus-render', {
        bubbles: true,
        composed: true
      }));
      return true;
    })()
    """

    private static let standardFrameworkDetailsAction = """
    (() => {
      const element = document.querySelector('#framework-details');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const summary = findDeep(element, (candidate) => {
        const part = candidate.getAttribute?.('part') || '';
        const text = candidate.textContent || '';
        return candidate.localName === 'summary'
          || candidate.localName === 'button'
          || part.includes('summary')
          || text.includes('Audit details');
      });
      const target = summary || element;
      if (typeof target.click !== 'function') return false;
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkAccordionAction = """
    (() => {
      const element = document.querySelector('#framework-accordion');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.opened = 0;
      element.dispatchEvent(new CustomEvent('opened-changed', {
        bubbles: true,
        composed: true,
        detail: { value: 0 }
      }));
      return true;
    })()
    """

    private static let standardFrameworkMenuBarAction = """
    (() => {
      const menu = document.querySelector('#framework-menu');
      if (!menu) return false;
      menu.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const item = document.querySelector('#framework-menu-audit')
        || findDeep(menu, (candidate) => {
          const text = candidate.textContent || '';
          return candidate.localName === 'vaadin-menu-bar-item'
            && text.includes('Audit Action');
        })
        || findDeep(document, (candidate) => {
          const text = candidate.textContent || '';
          return candidate.localName === 'vaadin-menu-bar-item'
            && text.includes('Audit Action');
        });
      if (!item || typeof item.click !== 'function') return false;
      item.click();
      return true;
    })()
    """

    private static let standardFrameworkSideNavAction = """
    (() => {
      const element = document.querySelector('#framework-side-nav-audit');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkContextMenuOpenAction = """
    (() => {
      const element = document.querySelector('#framework-context-menu-target');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkContextMenuItemAction = """
    (() => {
      const findDeep = (predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(document);
      };
      const item = findDeep((element) =>
        element.id === 'framework-context-menu-audit'
          || (element.localName === 'vaadin-context-menu-item'
            && (element.textContent || '').includes('Context Audit')));
      if (!item || typeof item.click !== 'function') return false;
      item.click();
      return true;
    })()
    """

    private static let standardFrameworkGridContextMenuOpenAction = """
    (() => {
      const grid = document.querySelector('#framework-grid');
      if (!grid) return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const cell = findDeep(grid, (element) => {
        const text = (element.textContent || '').trim();
        const part = element.getAttribute?.('part') || '';
        const role = element.getAttribute?.('role') || '';
        return (text === 'A-100' || text === 'platform' || text === 'queued')
          && (element.localName === 'vaadin-grid-cell-content'
            || part.includes('cell')
            || role === 'gridcell');
      });
      const clickTarget = cell || (() => {
        const rect = grid.getBoundingClientRect();
        let element = document.elementFromPoint(
          rect.left + Math.min(rect.width * 0.25, 120),
          rect.top + Math.min(rect.height * 0.5, 80));
        while (element?.shadowRoot) {
          const inner = element.shadowRoot.elementFromPoint(
            rect.left + Math.min(rect.width * 0.25, 120),
            rect.top + Math.min(rect.height * 0.5, 80));
          if (!inner || inner === element) break;
          element = inner;
        }
        return element;
      })();
      if (!clickTarget) return false;
      clickTarget.dispatchEvent(new MouseEvent('mousedown', {
        bubbles: true,
        composed: true,
        view: window
      }));
      clickTarget.dispatchEvent(new MouseEvent('mouseup', {
        bubbles: true,
        composed: true,
        view: window
      }));
      clickTarget.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        composed: true,
        view: window
      }));
      return true;
    })()
    """

    private static let standardFrameworkGridContextMenuItemAction = """
    (() => {
      const findDeep = (predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(document);
      };
      const item = findDeep((element) =>
        element.id === 'framework-grid-context-menu-audit'
          || (element.localName === 'vaadin-context-menu-item'
            && (element.textContent || '').includes('Grid Audit')));
      if (!item || typeof item.click !== 'function') return false;
      item.click();
      return true;
    })()
    """

    private static let standardFrameworkLazyGridAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      const report = document.querySelector('#framework-lazy-grid-report');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      grid.scrollToIndex(60);
      if (report && typeof report.click === 'function') {
        setTimeout(() => report.click(), 500);
      }
      return true;
    })()
    """

    private static let standardFrameworkVirtualListClientAction = """
    (() => {
      const list = document.querySelector('#framework-virtual-list');
      if (!list) return false;
      list.scrollIntoView({ block: 'center', inline: 'center' });
      if (typeof list.scrollToIndex === 'function') {
        list.scrollToIndex(55);
      } else {
        list.scrollTop = list.scrollHeight;
        list.dispatchEvent(new Event('scroll', { bubbles: true, composed: true }));
      }
      return true;
    })()
    """

    private static let standardFrameworkVirtualListServerAction = """
    (() => {
      const button = document.querySelector('#framework-virtual-list-scroll-end');
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkPopoverAction = """
    (() => {
      const element = document.querySelector('#framework-popover-target');
      if (!element || typeof element.click !== 'function') return false;
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNotificationAction = """
    (() => {
      const element = document.querySelector('#framework-notification-open');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkConfirmDialogOpenAction = """
    (() => {
      const element = document.querySelector('#framework-confirm-dialog-open');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkConfirmDialogConfirmAction = """
    (() => {
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const collectDeepText = (root) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '').replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          const children = node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(root);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const dialog = document.querySelector('#framework-confirm-dialog');
      const button = dialog?._confirmButton
        || dialog?.querySelector?.('[slot="confirm-button"]')
        || findDeep(document, (element) =>
          (element.localName === 'vaadin-button' || element.localName === 'button')
          && collectDeepText(element).includes('Confirm audit'));
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView?.({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkConfirmDialogCancelAction = """
    (() => {
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const collectDeepText = (root) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '').replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          const children = node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(root);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const dialog = document.querySelector('#framework-confirm-dialog');
      const button = dialog?._cancelButton
        || dialog?.querySelector?.('[slot="cancel-button"]')
        || findDeep(document, (element) =>
          (element.localName === 'vaadin-button' || element.localName === 'button')
          && collectDeepText(element).includes('Cancel audit'));
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView?.({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkConfirmDialogRejectAction = """
    (() => {
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const collectDeepText = (root) => {
        const parts = [];
        const visit = (node) => {
          if (!node) return;
          if (node.nodeType === Node.TEXT_NODE) {
            const text = String(node.textContent || '').replace(/\\s+/g, ' ').trim();
            if (text) parts.push(text);
            return;
          }
          if (node.shadowRoot) visit(node.shadowRoot);
          const children = node.childNodes || [];
          for (const child of children) visit(child);
        };
        visit(root);
        return parts.join(' ').replace(/\\s+/g, ' ').trim();
      };
      const dialog = document.querySelector('#framework-confirm-dialog');
      const button = dialog?._rejectButton
        || dialog?.querySelector?.('[slot="reject-button"]')
        || findDeep(document, (element) =>
          (element.localName === 'vaadin-button' || element.localName === 'button')
          && collectDeepText(element).includes('Reject audit'));
      if (!button || typeof button.click !== 'function') return false;
      button.scrollIntoView?.({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkProgressBarValueAction = """
    (() => {
      const element = document.querySelector('#framework-progress-value-update');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkProgressBarIndeterminateAction = """
    (() => {
      const element = document.querySelector('#framework-progress-indeterminate-update');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkUploadAction = """
    (() => {
      const upload = document.querySelector('#framework-upload');
      if (!upload) return false;
      upload.scrollIntoView({ block: 'center', inline: 'center' });
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const file = new File(['Flow IPC Upload Multipart\\n'], 'flow-ipc-upload.txt', {
        type: 'text/plain',
        lastModified: 1700000000000
      });
      const fileAdded = () =>
        Array.isArray(upload.files)
          && upload.files.some((candidate) => candidate?.name === file.name);
      const input = findDeep(upload, (element) =>
        element.localName === 'input'
          && (element.type || '').toLowerCase() === 'file');
      if (input) {
        try {
          const dataTransfer = new DataTransfer();
          dataTransfer.items.add(file);
          Object.defineProperty(input, 'files', {
            configurable: true,
            value: dataTransfer.files
          });
          input.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
        } catch (_) {
          // Some WebKit builds do not allow assigning a constructed FileList.
        }
      }
      if (!fileAdded() && typeof upload._addFiles === 'function') {
        upload._addFiles([file]);
      }
      const uploadFile = Array.isArray(upload.files)
        ? upload.files.find((candidate) => candidate?.name === file.name)
        : null;
      if (!uploadFile || typeof upload.uploadFiles !== 'function') return false;
      upload.uploadFiles(uploadFile);
      return true;
    })()
    """

    private static let standardFrameworkResourceFetchAction = """
    (() => {
      const decoder = new TextDecoder();
      const state = {
        complete: false,
        ok: false,
        streamResourceOk: false,
        binaryStreamResourceOk: false,
        downloadHandlerOk: false,
        standardStreamingDownloadOk: false,
        standardStreamingDownloadBytes: 0,
        statusText: 'Resource fetch: pending',
        error: ''
      };
      window.VaadinDesktopFrameworkResourceFetch = state;
      const resourceUrl = (selector) => {
        const anchor = document.querySelector(selector);
        if (!anchor) throw new Error(`missing ${selector}`);
        return new URL(anchor.getAttribute('href') || anchor.href, document.baseURI).href;
      };
      const fetchText = async (selector) => {
        const response = await fetch(resourceUrl(selector), { credentials: 'same-origin' });
        const body = await response.text();
        return {
          ok: response.ok,
          status: response.status,
          body,
          marker: response.headers.get('x-vatori-flow-ipc') || '',
          contentType: response.headers.get('content-type') || ''
        };
      };
      const fetchBytes = async (selector) => {
        const response = await fetch(resourceUrl(selector), { credentials: 'same-origin' });
        const bytes = new Uint8Array(await response.arrayBuffer());
        return {
          ok: response.ok,
          status: response.status,
          bytes,
          marker: response.headers.get('x-vatori-flow-ipc') || '',
          contentType: response.headers.get('content-type') || ''
        };
      };
      (async () => {
        const textResource = await fetchText('#framework-stream-resource-download');
        state.streamResourceOk = textResource.ok
          && textResource.marker === 'stream-resource'
          && textResource.body.includes('Flow IPC StreamResource')
          && textResource.body.includes('transport=flow-ipc-resource-request');

        const binaryResource = await fetchBytes('#framework-stream-resource-range-download');
        const binaryHead = decoder.decode(binaryResource.bytes.slice(0, 96));
        const binaryTail = decoder.decode(binaryResource.bytes.slice(-96));
        state.binaryStreamResourceOk = binaryResource.ok
          && binaryResource.marker === 'stream-resource-range'
          && binaryResource.bytes.byteLength === 8192
          && binaryHead.includes('Flow IPC Binary StreamResource')
          && binaryTail.includes('range-tail=stream-resource-range');

        const downloadHandler = await fetchText('#framework-download-handler-save-open');
        state.downloadHandlerOk = downloadHandler.ok
          && downloadHandler.body.includes('Flow IPC DownloadHandler')
          && downloadHandler.body.includes('transport=flow-ipc-resource-request-to-native-save');

        const streamingDownload = await fetchBytes('#framework-standard-streaming-download');
        const streamingHead = decoder.decode(streamingDownload.bytes.slice(0, 128));
        state.standardStreamingDownloadBytes = streamingDownload.bytes.byteLength;
        state.standardStreamingDownloadOk = streamingDownload.ok
          && streamingDownload.bytes.byteLength > 524288
          && streamingHead.includes('Flow IPC Standard Bundle Streaming Download');

        state.ok = state.streamResourceOk
          && state.binaryStreamResourceOk
          && state.downloadHandlerOk
          && state.standardStreamingDownloadOk;
        state.statusText = `Resource fetch: stream=${state.streamResourceOk}`
          + ` binary=${state.binaryStreamResourceOk}`
          + ` downloadHandler=${state.downloadHandlerOk}`
          + ` streaming=${state.standardStreamingDownloadOk}`
          + ` streamingBytes=${state.standardStreamingDownloadBytes}`;
        state.complete = true;
      })().catch((error) => {
        state.error = String(error?.message || error || 'resource fetch failed');
        state.statusText = `Resource fetch failed: ${state.error}`;
        state.complete = true;
      });
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
