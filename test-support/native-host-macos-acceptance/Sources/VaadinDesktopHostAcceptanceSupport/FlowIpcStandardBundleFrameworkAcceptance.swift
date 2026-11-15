import Cocoa
import Foundation
import WebKit

private enum FlowIpcStandardBundleFrameworkPhase {
    case waitingForReady
    case waitingForRuntimeStatus
    case waitingForProductReplacement
    case waitingForProductRejection
    case waitingForProductRejectionStatus
    case waitingForProductReplacementResult
    case waitingForProductGenerationConflict
    case waitingForProductGenerationConflictStatus
    case waitingForRuntimeObserverReady
    case waitingForRuntimeObserverStatus
    case waitingForRuntimeOwnerStatus
    case waitingForRuntimeObserverClosed
    case waitingForRuntimeRequesterResult
    case waitingForStaleRuntimeResult
    case waitingForActiveRuntimeOwnerClose
    case waitingForLazyFrontendRoute
    case waitingForLazyFrontendClient
    case waitingForLazyFrontendServer
    case waitingForLazyFrontendReturn
    case waitingForNestedLazyRoute
    case waitingForNestedLazyClient
    case waitingForNestedLazyServer
    case waitingForNestedLazyReturn
    case waitingForAppLayoutClient
    case waitingForAppLayoutServer
    case waitingForMasterDetailClient
    case waitingForMasterDetailContent
    case waitingForMasterDetailServer
    case waitingForCardClient
    case waitingForCardServer
    case waitingForMarkdownClient
    case waitingForMarkdownServer
    case waitingForBadgeClient
    case waitingForBadgeServer
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
    case waitingForSliderClient
    case waitingForSliderServer
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
    case waitingForNestedDialog
    case waitingForNestedDialogKeyboardClose
    case waitingForDialogKeyboardClose
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
    case waitingForIconClient
    case waitingForIconServer
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
    case waitingForLoginVariantsOpen
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
    case waitingForLazyGridFarViewport
    case waitingForLazyGridSelection
    case waitingForLazyGridReturnViewport
    case waitingForLazyGridSort
    case waitingForLazyGridFilter
    case waitingForLazyGridFilterReset
    case waitingForTreeGridPlatformExpand
    case waitingForTreeGridRuntimeExpand
    case waitingForTreeGridSelection
    case waitingForTreeGridRefresh
    case waitingForTreeGridCollapse
    case waitingForGridEditorOpen
    case waitingForGridEditorSave
    case waitingForGridEditorCancelOpen
    case waitingForGridEditorCancel
    case waitingForGridEditorValidationOpen
    case waitingForGridEditorValidation
    case waitingForGridEditorValidationCancel
    case waitingForLitRendererFunction
    case waitingForClientCallableResolve
    case waitingForClientCallableReject
    case waitingForPendingJavaScriptResolve
    case waitingForPendingJavaScriptReject
    case waitingForFlowDndDragStart
    case waitingForFlowDndDrop
    case waitingForFlowDndDragEnd
    case waitingForVirtualListClient
    case waitingForVirtualListServer
    case waitingForPopover
    case waitingForPopoverKeyboardClose
    case waitingForPopoverOutsideOpen
    case waitingForPopoverOutsideClose
    case waitingForMixedOverlayDialog
    case waitingForMixedOverlayPopover
    case waitingForMixedOverlayPopoverKeyboardClose
    case waitingForMixedOverlayPopoverOutsideOpen
    case waitingForMixedOverlayPopoverOutsideClose
    case waitingForMixedOverlayDialogClose
    case waitingForNestedPopoverOuter
    case waitingForNestedPopoverInner
    case waitingForNestedPopoverKeyboardClose
    case waitingForNestedPopoverOuterKeyboardClose
    case waitingForNestedPopoverOutsideOuterOpen
    case waitingForNestedPopoverOutsideInnerOpen
    case waitingForNestedPopoverOutsideTopClose
    case waitingForNestedPopoverOutsideParentClose
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
private var flowIpcStandardBundleFrameworkTracedPhase = ""
private var flowIpcRuntimeOwnerReopened = false
private var flowIpcRuntimeOriginalOwnerSession = ""

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
        flowIpcStandardBundleFrameworkTracedPhase = ""
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
            let phaseName = String(describing: flowIpcStandardBundleFrameworkPhase)
            if phaseName.contains("NestedDialog"),
               phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "openedObserved":
                        state["modalOverlayStackOpenedObserved"] as? Bool == true,
                    "initialFocusObserved":
                        state["modalOverlayStackInitialFocusObserved"] as? Bool == true,
                    "focusTrapObserved":
                        state["modalOverlayStackFocusTrapObserved"] as? Bool == true,
                    "topEscapeObserved":
                        state["modalOverlayStackTopEscapeObserved"] as? Bool == true,
                    "parentRemainsOpen":
                        state["modalOverlayStackParentRemainsOpenObserved"] as? Bool == true,
                    "focusRestoredObserved":
                        state["modalOverlayStackFocusRestoredObserved"] as? Bool == true,
                    "activeBeforeTab":
                        self.standardFrameworkString(
                            state, "modalOverlayStackActiveBeforeTab"),
                    "activeAfterTab":
                        self.standardFrameworkString(
                            state, "modalOverlayStackActiveAfterTab"),
                    "activeAfterShiftTab":
                        self.standardFrameworkString(
                            state, "modalOverlayStackActiveAfterShiftTab"),
                    "tabDefaultPrevented":
                        state["modalOverlayStackTabDefaultPrevented"] as? Bool == true,
                    "keyboardError":
                        self.standardFrameworkString(
                            state, "modalOverlayStackKeyboardError")
                ])
            } else if phaseName.contains("GridEditor"),
               phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "componentRendererObserved":
                        state["componentRendererObserved"] as? Bool == true,
                    "openedObserved":
                        state["gridEditorOpenedObserved"] as? Bool == true,
                    "activeFieldVisible":
                        state["gridEditorActiveFieldVisible"] as? Bool == true,
                    "saveObserved":
                        state["gridEditorSaveObserved"] as? Bool == true,
                    "cancelOpenedObserved":
                        state["gridEditorCancelOpenedObserved"] as? Bool == true,
                    "cancelObserved":
                        state["gridEditorCancelObserved"] as? Bool == true,
                    "validationOpenedObserved":
                        state["gridEditorValidationOpenedObserved"] as? Bool == true,
                    "validationObserved":
                        state["gridEditorValidationObserved"] as? Bool == true,
                    "activeFieldInvalid":
                        state["gridEditorActiveFieldInvalid"] as? Bool == true,
                    "validationCancelObserved":
                        state["gridEditorValidationCancelObserved"] as? Bool == true,
                    "rendererStatus":
                        self.standardFrameworkString(state, "editorRendererStatus"),
                    "openStatus":
                        self.standardFrameworkString(state, "editorOpenStatus"),
                    "saveStatus":
                        self.standardFrameworkString(state, "editorSaveStatus"),
                    "cancelStatus":
                        self.standardFrameworkString(state, "editorCancelStatus"),
                    "validationStatus":
                        self.standardFrameworkString(state, "editorValidationStatus")
                ])
            } else if phaseName.contains("LitRenderer"),
               phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "propertiesObserved":
                        state["litRendererPropertiesObserved"] as? Bool == true,
                    "functionObserved":
                        state["litRendererFunctionObserved"] as? Bool == true,
                    "refreshObserved":
                        state["litRendererRefreshObserved"] as? Bool == true,
                    "status":
                        self.standardFrameworkString(state, "litRendererStatus")
                ])
            } else if (phaseName.contains("ClientCallable")
                       || phaseName.contains("PendingJavaScript")),
                      phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "clientCallableResolveObserved":
                        state["clientCallableResolveObserved"] as? Bool == true,
                    "clientCallableRejectObserved":
                        state["clientCallableRejectObserved"] as? Bool == true,
                    "pendingJavaScriptResolveObserved":
                        state["pendingJavaScriptResolveObserved"] as? Bool == true,
                    "pendingJavaScriptRejectObserved":
                        state["pendingJavaScriptRejectObserved"] as? Bool == true,
                    "clientCallableStatus":
                        self.standardFrameworkString(
                            state, "clientCallableStatus"),
                    "pendingJavaScriptSuccessStatus":
                        self.standardFrameworkString(
                            state, "pendingJavaScriptSuccessStatus"),
                    "pendingJavaScriptFailureStatus":
                        self.standardFrameworkString(
                            state, "pendingJavaScriptFailureStatus")
                ])
            } else if phaseName.contains("FlowDnd"),
                      phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "configuredObserved":
                        state["flowDndConfiguredObserved"] as? Bool == true,
                    "dragStartObserved":
                        state["flowDndDragStartObserved"] as? Bool == true,
                    "dropObserved":
                        state["flowDndDropObserved"] as? Bool == true,
                    "dragEndObserved":
                        state["flowDndDragEndObserved"] as? Bool == true,
                    "dragStartStatus":
                        self.standardFrameworkString(
                            state, "flowDndDragStartStatus"),
                    "dropStatus":
                        self.standardFrameworkString(
                            state, "flowDndDropStatus"),
                    "dragEndStatus":
                        self.standardFrameworkString(
                            state, "flowDndDragEndStatus")
                ])
            } else if phaseName.contains("NestedPopover"),
               phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "outerOpened": state["nestedPopoverOuterOpened"] as? Bool == true,
                    "innerOpened": state["nestedPopoverInnerOpened"] as? Bool == true,
                    "stackOpened":
                        state["nestedAnchoredOverlayStackOpenedObserved"] as? Bool == true,
                    "a11yObserved":
                        state["nestedAnchoredOverlayStackA11yObserved"] as? Bool == true,
                    "tabOrderObserved":
                        state["nestedAnchoredOverlayStackTabOrderObserved"] as? Bool == true,
                    "topEscapeObserved":
                        state["nestedAnchoredOverlayStackTopEscapeObserved"] as? Bool == true,
                    "parentRemainsOpen":
                        state[
                            "nestedAnchoredOverlayStackParentRemainsOpenObserved"
                        ] as? Bool == true,
                    "innerFocusRestored":
                        state[
                            "nestedAnchoredOverlayStackInnerFocusRestoredObserved"
                        ] as? Bool == true,
                    "parentEscapeObserved":
                        state[
                            "nestedAnchoredOverlayStackParentEscapeObserved"
                        ] as? Bool == true,
                    "layeredFocusRestored":
                        state[
                            "nestedAnchoredOverlayStackLayeredFocusRestoredObserved"
                        ] as? Bool == true,
                    "outsideTopClose":
                        state[
                            "nestedAnchoredOverlayStackOutsideTopCloseObserved"
                        ] as? Bool == true,
                    "outsideParentRemainsOpen":
                        state[
                            "nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved"
                        ] as? Bool == true,
                    "outsideFocusPreserved":
                        state[
                            "nestedAnchoredOverlayStackOutsideFocusPreservedObserved"
                        ] as? Bool == true,
                    "outsideParentClose":
                        state[
                            "nestedAnchoredOverlayStackOutsideParentCloseObserved"
                        ] as? Bool == true,
                    "outerLifecycle": self.standardFrameworkString(
                        state, "nestedPopoverOuterLifecycleStatus"),
                    "innerLifecycle": self.standardFrameworkString(
                        state, "nestedPopoverInnerLifecycleStatus"),
                    "activePath": self.standardFrameworkString(
                        state, "nestedAnchoredOverlayStackActivePath"),
                    "keyboardError": self.standardFrameworkString(
                        state, "nestedAnchoredOverlayStackKeyboardError"),
                    "outsideError": self.standardFrameworkString(
                        state, "nestedAnchoredOverlayStackOutsideError")
                ])
            } else if phaseName.contains("MixedOverlay"),
               phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "dialogOpened":
                        state["mixedOverlayStackDialogOpenedObserved"] as? Bool == true,
                    "popoverOpened":
                        state["mixedOverlayPopoverOpened"] as? Bool == true,
                    "stackOpened":
                        state["mixedOverlayStackOpenedObserved"] as? Bool == true,
                    "a11yObserved":
                        state["mixedOverlayStackA11yObserved"] as? Bool == true,
                    "tabOrderObserved":
                        state["mixedOverlayStackTabOrderObserved"] as? Bool == true,
                    "topEscapeObserved":
                        state["mixedOverlayStackTopEscapeObserved"] as? Bool == true,
                    "parentRemainsOpen":
                        state["mixedOverlayStackParentRemainsOpenObserved"] as? Bool == true,
                    "popoverFocusRestored":
                        state["mixedOverlayStackPopoverFocusRestoredObserved"] as? Bool == true,
                    "outsideCloseObserved":
                        state["mixedOverlayStackOutsideCloseObserved"] as? Bool == true,
                    "outsideFocusPreserved":
                        state["mixedOverlayStackOutsideFocusPreservedObserved"] as? Bool == true,
                    "parentEscapeObserved":
                        state["mixedOverlayStackParentEscapeObserved"] as? Bool == true,
                    "layeredFocusRestored":
                        state["mixedOverlayStackLayeredFocusRestoredObserved"] as? Bool == true,
                    "dialogLifecycle": self.standardFrameworkString(
                        state, "mixedOverlayDialogLifecycleStatus"),
                    "popoverLifecycle": self.standardFrameworkString(
                        state, "mixedOverlayPopoverLifecycleStatus"),
                    "activePath": self.standardFrameworkString(
                        state, "mixedOverlayStackActivePath"),
                    "keyboardError": self.standardFrameworkString(
                        state, "mixedOverlayStackKeyboardError"),
                    "outsideError": self.standardFrameworkString(
                        state, "mixedOverlayStackOutsideError"),
                    "dialogError": self.standardFrameworkString(
                        state, "mixedOverlayStackDialogError")
                ])
            } else if phaseName.contains("Popover"),
               phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName,
                    "popoverOpened": state["popoverOpened"] as? Bool == true,
                    "popoverLifecycleStatus":
                        self.standardFrameworkString(state, "popoverLifecycleStatus"),
                    "anchoredObserved":
                        state["anchoredModelessOverlayObserved"] as? Bool == true,
                    "a11yObserved":
                        state["anchoredModelessOverlayA11yObserved"] as? Bool == true,
                    "tabOrderObserved":
                        state["anchoredModelessOverlayTabOrderObserved"] as? Bool == true,
                    "escapeCloseObserved":
                        state["anchoredModelessOverlayEscapeCloseObserved"] as? Bool == true,
                    "escapeFocusRestoredObserved":
                        state["anchoredModelessOverlayEscapeFocusRestoredObserved"] as? Bool == true,
                    "outsideCloseObserved":
                        state["anchoredModelessOverlayOutsideCloseObserved"] as? Bool == true,
                    "outsideFocusPreservedObserved":
                        state["anchoredModelessOverlayOutsideFocusPreservedObserved"] as? Bool == true,
                    "role": self.standardFrameworkString(
                        state, "anchoredModelessOverlayRole"),
                    "ariaLabel": self.standardFrameworkString(
                        state, "anchoredModelessOverlayAriaLabel"),
                    "ariaExpanded": self.standardFrameworkString(
                        state, "anchoredModelessOverlayTargetAriaExpanded"),
                    "ariaControls": self.standardFrameworkString(
                        state, "anchoredModelessOverlayTargetAriaControls"),
                    "ariaHasPopup": self.standardFrameworkString(
                        state, "anchoredModelessOverlayTargetAriaHasPopup"),
                    "activePath": self.standardFrameworkString(
                        state, "anchoredModelessOverlayActivePath"),
                    "keyboardError": self.standardFrameworkString(
                        state, "anchoredModelessOverlayKeyboardError"),
                    "outsideError": self.standardFrameworkString(
                        state, "anchoredModelessOverlayOutsideError")
                ])
            } else if phaseName != flowIpcStandardBundleFrameworkTracedPhase {
                flowIpcStandardBundleFrameworkTracedPhase = phaseName
                trace("recv", "standardBundle.frameworkAcceptance.phase", [
                    "phase": phaseName
                ])
            }

            let ownerReopenMode = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "owner-reopen"
            let activeOwnerCloseMode = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "owner-active-close"
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
                    script: "(() => { const button = document.querySelector('#framework-runtime-replacement-query'); if (!button) return false; button.click(); return true; })()") { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context, dispatched: dispatched, next: .waitingForRuntimeStatus,
                            failureReason: "runtime-status-query-dispatch-failed")
                    }
            case .waitingForRuntimeStatus:
                guard state["runtimeReplacementStatus"] as? String == "none" else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context, reason: "runtime-status-query-not-settled", state: state)
                    return
                }
                trace("recv", "standardBundle.runtimeReplacementStatus", ["status": "none", "observedInDom": true])
                if ownerReopenMode || activeOwnerCloseMode {
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-observer-open'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForRuntimeObserverReady, failureReason: "requester-open-dispatch-failed")
                        }
                    return
                }
                if ["1", "rollback", "rejection", "burst", "generation-conflict", "owner-isolation"].contains(ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] ?? "") {
                    let requestButton = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "burst"
                        ? "framework-runtime-replacement-burst" : "framework-runtime-replacement-request"
                    self.dispatchStandardBundleFrameworkAction(
                        context.webView,
                        script: "(() => { const button = document.querySelector('#\(requestButton)'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "rejection"
                                    ? .waitingForProductRejection : .waitingForProductReplacement,
                                failureReason: "product-replacement-dispatch-failed")
                        }
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyFrontendOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForLazyFrontendRoute, failureReason: "lazy-frontend-navigation-dispatch-failed")
                    }
            case .waitingForProductRejection:
                guard state["runtimeReplacementRequestResult"] as? String == "invalid-candidate",
                      context.recoveryGeneration == 0 else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context,
                        reason: "product-rejection-not-observed", state: state)
                    return
                }
                trace("recv", "standardBundle.productReplacementRejected", [
                    "errorCode": "invalid-candidate", "observedInDom": true, "recoveryGeneration": 0])
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: "(() => { const button = document.querySelector('#framework-runtime-replacement-query'); if (!button) return false; button.click(); return true; })()") { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForProductRejectionStatus, failureReason: "rejection-status-query-dispatch-failed")
                    }
            case .waitingForProductRejectionStatus:
                guard state["runtimeReplacementStatus"] as? String == "none",
                      context.recoveryGeneration == 0 else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context,
                        reason: "rejection-status-not-observed", state: state)
                    return
                }
                trace("recv", "standardBundle.productRejectionStatus", ["status": "none", "observedInDom": true])
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: Self.standardFrameworkLazyFrontendOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForLazyFrontendRoute, failureReason: "lazy-frontend-navigation-dispatch-failed")
                    }
            case .waitingForProductReplacement:
                guard context.recoveryGeneration > 0, state["ready"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context, reason: "product-replacement-reload-not-observed", state: state)
                    return
                }
                if ownerReopenMode {
                    self.advanceStandardBundleFrameworkAcceptance(context, dispatched: true,
                        next: .waitingForRuntimeObserverReady, failureReason: "requester-reload-not-ready")
                    return
                }
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: "(() => { const button = document.querySelector('#framework-runtime-replacement-query'); if (!button) return false; button.click(); return true; })()") { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForProductReplacementResult, failureReason: "replacement-result-query-dispatch-failed")
                    }
            case .waitingForProductReplacementResult:
                let expected = activeOwnerCloseMode ? "none" : (ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "rollback"
                    ? "rolled-back" : "replaced")
                guard state["runtimeReplacementStatus"] as? String == expected else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context, reason: "product-replacement-result-not-observed", state: state)
                    return
                }
                trace("recv", "standardBundle.productReplacement", ["status": expected, "observedInDom": true,
                    "recoveryGeneration": context.recoveryGeneration])
                if ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "owner-isolation" {
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-observer-open'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForRuntimeObserverReady, failureReason: "runtime-observer-open-dispatch-failed")
                        }
                    return
                }
                if ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_PRODUCT_REPLACEMENT"] == "generation-conflict" {
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-replacement-request'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForProductGenerationConflict, failureReason: "generation-conflict-dispatch-failed")
                        }
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyFrontendOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyFrontendRoute,
                            failureReason: "lazy-frontend-navigation-dispatch-failed")
                    }
            case .waitingForProductGenerationConflict:
                guard state["runtimeReplacementRequestResult"] as? String == "request-conflict",
                      context.recoveryGeneration == 1 else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context,
                        reason: "generation-conflict-not-observed", state: state)
                    return
                }
                trace("recv", "standardBundle.productGenerationConflict", [
                    "errorCode": "request-conflict", "observedInDom": true, "recoveryGeneration": 1])
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: "(() => { const button = document.querySelector('#framework-runtime-replacement-query'); if (!button) return false; button.click(); return true; })()") { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForProductGenerationConflictStatus, failureReason: "conflict-status-query-dispatch-failed")
                    }
            case .waitingForProductGenerationConflictStatus:
                guard state["runtimeReplacementStatus"] as? String == "replaced",
                      context.recoveryGeneration == 1 else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context,
                        reason: "conflict-terminal-status-not-observed", state: state)
                    return
                }
                trace("recv", "standardBundle.productConflictStatus", ["status": "replaced", "observedInDom": true])
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: Self.standardFrameworkLazyFrontendOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForLazyFrontendRoute, failureReason: "lazy-frontend-navigation-dispatch-failed")
                    }
            case .waitingForRuntimeObserverReady:
                guard let observer = self.flowIpcWindowContext("runtime-observer") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "runtime-observer-not-open", state: state)
                    return
                }
                self.evaluateStandardBundleFrameworkState(observer.webView) { observerState in
                    if let error = observerState?["error"] as? String, !error.isEmpty {
                        self.finishStandardBundleFrameworkAcceptance(context, success: false,
                            reason: "runtime-observer-browser-error", state: observerState ?? [:])
                        return
                    }
                    guard observerState?["ready"] as? Bool == true else {
                        self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "runtime-observer-not-ready", state: observerState ?? [:])
                        return
                    }
                    if (ownerReopenMode && !flowIpcRuntimeOwnerReopened) || activeOwnerCloseMode {
                        if context.recoveryGeneration == 0 {
                            flowIpcRuntimeOriginalOwnerSession = observer.scope.sessionId
                        } else if observer.scope.sessionId != flowIpcRuntimeOriginalOwnerSession {
                            self.finishStandardBundleFrameworkAcceptance(context, success: false,
                                reason: "requester-scope-not-retained", state: observerState ?? [:])
                            return
                        }
                        let request = context.recoveryGeneration == 0
                        let button = request ? "framework-runtime-replacement-request" : "framework-runtime-replacement-query"
                        self.dispatchStandardBundleFrameworkAction(observer.webView,
                            script: "(() => { const button = document.querySelector('#\(button)'); if (!button) return false; button.click(); return true; })()") { dispatched in
                                self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                    next: activeOwnerCloseMode ? .waitingForActiveRuntimeOwnerClose : (request ? .waitingForProductReplacement : .waitingForRuntimeRequesterResult),
                                    failureReason: "requester-command-dispatch-failed")
                            }
                        return
                    }
                    if ownerReopenMode && observer.scope.sessionId == flowIpcRuntimeOriginalOwnerSession {
                        self.finishStandardBundleFrameworkAcceptance(context, success: false,
                            reason: "reopened-owner-reused-session", state: observerState ?? [:])
                        return
                    }
                    self.dispatchStandardBundleFrameworkAction(observer.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-replacement-query'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForRuntimeObserverStatus, failureReason: "observer-status-query-dispatch-failed")
                        }
                }
            case .waitingForActiveRuntimeOwnerClose:
                guard context.recoveryGeneration == 0,
                      let observer = self.flowIpcWindowContext("runtime-observer") else {
                    self.finishStandardBundleFrameworkAcceptance(context, success: false,
                        reason: "active-owner-close-window-missed", state: state)
                    return
                }
                context.webView.evaluateJavaScript("window.VaadinDesktopIpcDiagnostics?.standardBundleSidecarDisconnected === true") { result, error in
                    guard error == nil, result as? Bool == true else {
                        self.retryOrFailStandardBundleFrameworkAcceptance(context,
                            reason: "replacement-disconnection-not-observed", state: state)
                        return
                    }
                    trace("recv", "standardBundle.activeRuntimeOwnerClose", observer.scope.tracePayload)
                    observer.window.performClose(nil)
                    self.advanceStandardBundleFrameworkAcceptance(context,
                        dispatched: self.flowIpcWindowContext("runtime-observer") == nil,
                        next: .waitingForProductReplacement, failureReason: "active-owner-close-failed")
                }
            case .waitingForRuntimeRequesterResult:
                guard let observer = self.flowIpcWindowContext("runtime-observer") else {
                    self.finishStandardBundleFrameworkAcceptance(context, success: false, reason: "requester-disappeared", state: state)
                    return
                }
                self.evaluateStandardBundleFrameworkState(observer.webView) { observerState in
                    guard observerState?["runtimeReplacementStatus"] as? String == "replaced",
                          observerState?["runtimeCaptured"] as? String == "captured",
                          (observerState?["error"] as? String ?? "").isEmpty else {
                        self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "requester-result-not-observed", state: observerState ?? [:])
                        return
                    }
                    var receipt = observer.scope.tracePayload
                    receipt["status"] = "replaced"
                    receipt["observedInDom"] = true
                    receipt["recoveryGeneration"] = observer.recoveryGeneration
                    trace("recv", "standardBundle.productReplacement", receipt)
                    trace("recv", "standardBundle.runtimeContextCaptured", observer.scope.tracePayload)
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-observer-close'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForRuntimeObserverClosed, failureReason: "requester-close-dispatch-failed")
                        }
                }
            case .waitingForRuntimeObserverStatus:
                guard let observer = self.flowIpcWindowContext("runtime-observer") else {
                    self.finishStandardBundleFrameworkAcceptance(context, success: false, reason: "runtime-observer-disappeared", state: state)
                    return
                }
                self.evaluateStandardBundleFrameworkState(observer.webView) { observerState in
                    guard observerState?["runtimeReplacementStatus"] as? String == "none",
                          (observerState?["error"] as? String ?? "").isEmpty else {
                        self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "foreign-runtime-status-not-none", state: observerState ?? [:])
                        return
                    }
                    var receipt = observer.scope.tracePayload
                    receipt["status"] = "none"
                    receipt["observedInDom"] = true
                    trace("recv", "standardBundle.runtimeObserverIsolation", receipt)
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-replacement-query'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForRuntimeOwnerStatus, failureReason: "owner-status-requery-dispatch-failed")
                        }
                }
            case .waitingForRuntimeOwnerStatus:
                let expected = ownerReopenMode ? "none" : "replaced"
                guard state["runtimeReplacementStatus"] as? String == expected else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "owner-terminal-not-preserved", state: state)
                    return
                }
                trace("recv", "standardBundle.runtimeOwnerPreserved", ["status": expected, "observedInDom": true])
                if ownerReopenMode {
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-stale-probe'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForStaleRuntimeResult, failureReason: "stale-runtime-probe-dispatch-failed")
                        }
                    return
                }
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: "(() => { const button = document.querySelector('#framework-runtime-observer-close'); if (!button) return false; button.click(); return true; })()") { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForRuntimeObserverClosed, failureReason: "observer-close-dispatch-failed")
                    }
            case .waitingForStaleRuntimeResult:
                let result = state["runtimeStaleStatus"] as? String ?? ""
                guard result == "query=true request=true background=true client=true" else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "stale-runtime-not-invalidated", state: state)
                    return
                }
                trace("recv", "standardBundle.runtimeStaleContext", ["result": result, "observedInDom": true,
                    "retiredSessionId": flowIpcRuntimeOriginalOwnerSession])
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: "(() => { const button = document.querySelector('#framework-runtime-observer-close'); if (!button) return false; button.click(); return true; })()") { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForRuntimeObserverClosed, failureReason: "stale-probe-observer-close-failed")
                    }
            case .waitingForRuntimeObserverClosed:
                guard self.flowIpcWindowContext("runtime-observer") == nil else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(context, reason: "runtime-observer-still-open", state: state)
                    return
                }
                if ownerReopenMode && !flowIpcRuntimeOwnerReopened {
                    flowIpcRuntimeOwnerReopened = true
                    self.dispatchStandardBundleFrameworkAction(context.webView,
                        script: "(() => { const button = document.querySelector('#framework-runtime-observer-open'); if (!button) return false; button.click(); return true; })()") { dispatched in
                            self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                                next: .waitingForRuntimeObserverReady, failureReason: "requester-reopen-dispatch-failed")
                        }
                    return
                }
                self.dispatchStandardBundleFrameworkAction(context.webView,
                    script: Self.standardFrameworkLazyFrontendOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(context, dispatched: dispatched,
                            next: .waitingForLazyFrontendRoute, failureReason: "lazy-frontend-navigation-dispatch-failed")
                    }
            case .waitingForLazyFrontendRoute:
                guard state["lazyFrontendRouteObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-frontend-route-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyFrontendClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyFrontendClient,
                            failureReason: "lazy-frontend-client-dispatch-failed")
                    }
            case .waitingForLazyFrontendClient:
                guard state["lazyFrontendClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-frontend-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyFrontendServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyFrontendServer,
                            failureReason: "lazy-frontend-server-dispatch-failed")
                    }
            case .waitingForLazyFrontendServer:
                guard state["lazyFrontendServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-frontend-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyFrontendReturnAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyFrontendReturn,
                            failureReason: "lazy-frontend-return-dispatch-failed")
                    }
            case .waitingForLazyFrontendReturn:
                guard state["ready"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-frontend-return-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedLazyOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedLazyRoute,
                            failureReason: "nested-lazy-navigation-dispatch-failed")
                    }
            case .waitingForNestedLazyRoute:
                guard state["nestedLazyLayoutObserved"] as? Bool == true,
                      state["nestedLazyRouteObserved"] as? Bool == true,
                      state["nestedLazyTransitiveModuleObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-lazy-dependency-graph-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedLazyClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedLazyClient,
                            failureReason: "nested-lazy-client-dispatch-failed")
                    }
            case .waitingForNestedLazyClient:
                guard state["nestedLazyClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-lazy-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedLazyServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedLazyServer,
                            failureReason: "nested-lazy-server-dispatch-failed")
                    }
            case .waitingForNestedLazyServer:
                guard state["nestedLazyServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-lazy-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedLazyReturnAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedLazyReturn,
                            failureReason: "nested-lazy-return-dispatch-failed")
                    }
            case .waitingForNestedLazyReturn:
                guard state["ready"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-lazy-return-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkAppLayoutClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForAppLayoutClient,
                            failureReason: "app-layout-client-dispatch-failed")
                    }
            case .waitingForAppLayoutClient:
                guard state["appLayoutClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "app-layout-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkAppLayoutServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForAppLayoutServer,
                            failureReason: "app-layout-server-dispatch-failed")
                    }
            case .waitingForAppLayoutServer:
                guard state["appLayoutServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "app-layout-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMasterDetailClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMasterDetailClient,
                            failureReason: "master-detail-client-dispatch-failed")
                    }
            case .waitingForMasterDetailClient:
                guard state["masterDetailClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "master-detail-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMasterDetailServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMasterDetailContent,
                            failureReason: "master-detail-server-dispatch-failed")
                    }
            case .waitingForMasterDetailContent:
                guard state["masterDetailContentObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "master-detail-content-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMasterDetailAnimationAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMasterDetailServer,
                            failureReason: "master-detail-animation-dispatch-failed")
                    }
            case .waitingForMasterDetailServer:
                guard state["masterDetailServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "master-detail-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkCardClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForCardClient,
                            failureReason: "card-client-dispatch-failed")
                    }
            case .waitingForCardClient:
                guard state["cardClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "card-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkCardServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForCardServer,
                            failureReason: "card-server-dispatch-failed")
                    }
            case .waitingForCardServer:
                guard state["cardServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "card-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMarkdownClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMarkdownClient,
                            failureReason: "markdown-client-dispatch-failed")
                    }
            case .waitingForMarkdownClient:
                guard state["markdownClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "markdown-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMarkdownServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMarkdownServer,
                            failureReason: "markdown-server-dispatch-failed")
                    }
            case .waitingForMarkdownServer:
                guard state["markdownServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "markdown-server-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkBadgeClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForBadgeClient,
                            failureReason: "badge-client-dispatch-failed")
                    }
            case .waitingForBadgeClient:
                guard state["badgeClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "badge-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkBadgeServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForBadgeServer,
                            failureReason: "badge-server-dispatch-failed")
                    }
            case .waitingForBadgeServer:
                guard state["badgeServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "badge-server-patch-not-observed",
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
                    script: Self.standardFrameworkSliderClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForSliderClient,
                            failureReason: "slider-event-dispatch-failed")
                    }
            case .waitingForSliderClient:
                guard state["sliderClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "slider-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkSliderServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForSliderServer,
                            failureReason: "slider-server-dispatch-failed")
                    }
            case .waitingForSliderServer:
                guard state["sliderServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "slider-server-patch-not-observed",
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
                      state["dialogOpened"] as? Bool == true,
                      state["dialogInitialFocusObserved"] as? Bool == true,
                      state["dialogA11yObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "dialog-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedDialogAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedDialog,
                            failureReason: "nested-dialog-open-dispatch-failed")
                    }
            case .waitingForNestedDialog:
                guard state["modalOverlayStackOpenedObserved"] as? Bool == true,
                      state["modalOverlayStackInitialFocusObserved"] as? Bool == true,
                      state["modalOverlayStackA11yObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-dialog-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedDialogKeyboardCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedDialogKeyboardClose,
                            failureReason: "nested-dialog-keyboard-close-dispatch-failed")
                    }
            case .waitingForNestedDialogKeyboardClose:
                guard state["modalOverlayStackFocusTrapObserved"] as? Bool == true,
                      state["modalOverlayStackTopEscapeObserved"] as? Bool == true,
                      state["modalOverlayStackParentRemainsOpenObserved"] as? Bool == true,
                      state["modalOverlayStackFocusRestoredObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-dialog-keyboard-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkDialogKeyboardCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForDialogKeyboardClose,
                            failureReason: "dialog-keyboard-close-dispatch-failed")
                    }
            case .waitingForDialogKeyboardClose:
                guard state["dialogOpened"] as? Bool == false,
                      state["dialogFocusTrapObserved"] as? Bool == true,
                      state["dialogEscapeCloseObserved"] as? Bool == true,
                      state["dialogFocusRestoredObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "dialog-keyboard-close-not-observed",
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
                    script: Self.standardFrameworkIconClientAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForIconClient,
                            failureReason: "icon-client-dispatch-failed")
                    }
            case .waitingForIconClient:
                guard state["iconClientObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "icon-client-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkIconServerAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForIconServer,
                            failureReason: "icon-server-dispatch-failed")
                    }
            case .waitingForIconServer:
                guard state["iconServerObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "icon-server-patch-not-observed",
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
                            next: .waitingForLoginVariantsOpen,
                            failureReason: "login-variants-dispatch-failed")
                    }
            case .waitingForLoginVariantsOpen:
                guard self.standardFrameworkString(state, "loginOverlaySubmitButtonStatus")
                    .contains("LoginOverlay submit button opened: true"),
                      state["loginOverlaySubmitButtonReady"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context, reason: "login-variants-open-not-observed", state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLoginVariantsSubmitAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context, dispatched: dispatched, next: .waitingForLoginVariants,
                            failureReason: "login-variants-submit-dispatch-failed")
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
                    script: Self.standardFrameworkLazyGridFarViewportAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGridFarViewport,
                            failureReason: "lazy-grid-far-viewport-dispatch-failed")
                    }
            case .waitingForLazyGridFarViewport:
                guard state["lazyGridFarViewportObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-far-viewport-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyGridSelectionAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGridSelection,
                            failureReason: "lazy-grid-selection-dispatch-failed")
                    }
            case .waitingForLazyGridSelection:
                guard state["lazyGridSelectionObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-selection-round-trip-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyGridReturnViewportAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGridReturnViewport,
                            failureReason: "lazy-grid-return-viewport-dispatch-failed")
                    }
            case .waitingForLazyGridReturnViewport:
                guard state["lazyGridReturnViewportObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-return-viewport-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyGridSortAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGridSort,
                            failureReason: "lazy-grid-sort-dispatch-failed")
                    }
            case .waitingForLazyGridSort:
                guard state["lazyGridSortObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-sort-query-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyGridFilterAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGridFilter,
                            failureReason: "lazy-grid-filter-dispatch-failed")
                    }
            case .waitingForLazyGridFilter:
                guard state["lazyGridFilterObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-filter-query-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLazyGridFilterResetAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLazyGridFilterReset,
                            failureReason: "lazy-grid-filter-reset-dispatch-failed")
                    }
            case .waitingForLazyGridFilterReset:
                guard state["lazyGridFilterResetObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lazy-grid-filter-reset-query-not-observed",
                        state: state)
                    return
                }
                guard state["treeGridRootLoadedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tree-grid-root-query-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTreeGridPlatformExpandAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTreeGridPlatformExpand,
                            failureReason: "tree-grid-platform-expand-dispatch-failed")
                    }
            case .waitingForTreeGridPlatformExpand:
                guard state["treeGridPlatformExpandedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tree-grid-platform-expand-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTreeGridRuntimeExpandAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTreeGridRuntimeExpand,
                            failureReason: "tree-grid-runtime-expand-dispatch-failed")
                    }
            case .waitingForTreeGridRuntimeExpand:
                guard state["treeGridRuntimeExpandedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tree-grid-runtime-expand-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTreeGridSelectionAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTreeGridSelection,
                            failureReason: "tree-grid-selection-dispatch-failed")
                    }
            case .waitingForTreeGridSelection:
                guard state["treeGridSelectionObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tree-grid-selection-round-trip-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTreeGridRefreshAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTreeGridRefresh,
                            failureReason: "tree-grid-refresh-dispatch-failed")
                    }
            case .waitingForTreeGridRefresh:
                guard state["treeGridRefreshObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tree-grid-refresh-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkTreeGridCollapseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForTreeGridCollapse,
                            failureReason: "tree-grid-collapse-dispatch-failed")
                    }
            case .waitingForTreeGridCollapse:
                guard state["treeGridCollapseObserved"] as? Bool == true,
                      state["componentRendererObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "tree-grid-collapse-or-component-renderer-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorOpen,
                            failureReason: "grid-editor-open-dispatch-failed")
                    }
            case .waitingForGridEditorOpen:
                guard state["gridEditorOpenedObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorSaveAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorSave,
                            failureReason: "grid-editor-save-dispatch-failed")
                    }
            case .waitingForGridEditorSave:
                guard state["gridEditorSaveObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool != true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-save-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorCancelOpenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorCancelOpen,
                            failureReason: "grid-editor-cancel-open-dispatch-failed")
                    }
            case .waitingForGridEditorCancelOpen:
                guard state["gridEditorCancelOpenedObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-cancel-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorCancelAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorCancel,
                            failureReason: "grid-editor-cancel-dispatch-failed")
                    }
            case .waitingForGridEditorCancel:
                guard state["gridEditorCancelObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool != true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-cancel-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorValidationOpenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorValidationOpen,
                            failureReason:
                                "grid-editor-validation-open-dispatch-failed")
                    }
            case .waitingForGridEditorValidationOpen:
                guard state["gridEditorValidationOpenedObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-validation-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorValidationAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorValidation,
                            failureReason: "grid-editor-validation-dispatch-failed")
                    }
            case .waitingForGridEditorValidation:
                guard state["gridEditorValidationObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool == true,
                      state["gridEditorActiveFieldInvalid"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-validation-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkGridEditorValidationCancelAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForGridEditorValidationCancel,
                            failureReason:
                                "grid-editor-validation-cancel-dispatch-failed")
                    }
            case .waitingForGridEditorValidationCancel:
                guard state["gridEditorValidationCancelObserved"] as? Bool == true,
                      state["gridEditorActiveFieldVisible"] as? Bool != true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "grid-editor-validation-cancel-not-observed",
                        state: state)
                    return
                }
                guard state["litRendererPropertiesObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lit-renderer-properties-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkLitRendererFunctionAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForLitRendererFunction,
                            failureReason: "lit-renderer-function-dispatch-failed")
                    }
            case .waitingForLitRendererFunction:
                guard state["litRendererFunctionObserved"] as? Bool == true,
                      state["litRendererRefreshObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "lit-renderer-function-refresh-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkClientCallableResolveAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForClientCallableResolve,
                            failureReason:
                                "client-callable-resolve-dispatch-failed")
                    }
            case .waitingForClientCallableResolve:
                guard state["clientCallableResolveObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "client-callable-resolve-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkClientCallableRejectAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForClientCallableReject,
                            failureReason:
                                "client-callable-reject-dispatch-failed")
                    }
            case .waitingForClientCallableReject:
                guard state["clientCallableRejectObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "client-callable-reject-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPendingJavaScriptResolveAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPendingJavaScriptResolve,
                            failureReason:
                                "pending-javascript-resolve-dispatch-failed")
                    }
            case .waitingForPendingJavaScriptResolve:
                guard state["pendingJavaScriptResolveObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "pending-javascript-resolve-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPendingJavaScriptRejectAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPendingJavaScriptReject,
                            failureReason:
                                "pending-javascript-reject-dispatch-failed")
                    }
            case .waitingForPendingJavaScriptReject:
                guard state["pendingJavaScriptRejectObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "pending-javascript-reject-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: FlowIpcDndAcceptanceScripts.dragStartAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFlowDndDragStart,
                            failureReason:
                                "flow-dnd-drag-start-dispatch-failed")
                    }
            case .waitingForFlowDndDragStart:
                guard state["flowDndConfiguredObserved"] as? Bool == true,
                      state["flowDndDragStartObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "flow-dnd-drag-start-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: FlowIpcDndAcceptanceScripts.dropAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFlowDndDrop,
                            failureReason: "flow-dnd-drop-dispatch-failed")
                    }
            case .waitingForFlowDndDrop:
                guard state["flowDndDropObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "flow-dnd-drop-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: FlowIpcDndAcceptanceScripts.dragEndAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForFlowDndDragEnd,
                            failureReason:
                                "flow-dnd-drag-end-dispatch-failed")
                    }
            case .waitingForFlowDndDragEnd:
                guard state["flowDndDragEndObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "flow-dnd-drag-end-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkVirtualListClientAction) {
                        dispatched in
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
                      state["popoverOpened"] as? Bool == true,
                      state["anchoredModelessOverlayObserved"] as? Bool == true,
                      state["anchoredModelessOverlayA11yObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "popover-open-patch-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPopoverKeyboardCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPopoverKeyboardClose,
                            failureReason: "popover-keyboard-close-dispatch-failed")
                    }
            case .waitingForPopoverKeyboardClose:
                guard state["anchoredModelessOverlayTabOrderObserved"] as? Bool == true,
                      state["anchoredModelessOverlayEscapeCloseObserved"] as? Bool == true,
                      state["anchoredModelessOverlayEscapeFocusRestoredObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "popover-keyboard-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPopoverReopenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPopoverOutsideOpen,
                            failureReason: "popover-reopen-dispatch-failed")
                    }
            case .waitingForPopoverOutsideOpen:
                guard state["popoverOpened"] as? Bool == true,
                      self.standardFrameworkString(state, "popoverLifecycleStatus")
                        .contains("Popover lifecycle opened: client=true") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "popover-reopen-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkPopoverOutsideCloseAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForPopoverOutsideClose,
                            failureReason: "popover-outside-close-dispatch-failed")
                    }
            case .waitingForPopoverOutsideClose:
                guard state["anchoredModelessOverlayOutsideCloseObserved"] as? Bool == true,
                      state["anchoredModelessOverlayOutsideFocusPreservedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "popover-outside-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMixedOverlayDialogOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMixedOverlayDialog,
                            failureReason: "mixed-overlay-dialog-open-dispatch-failed")
                    }
            case .waitingForMixedOverlayDialog:
                guard state["mixedOverlayStackDialogOpenedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "mixed-overlay-dialog-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMixedOverlayPopoverOpenAction) { dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMixedOverlayPopover,
                            failureReason: "mixed-overlay-popover-open-dispatch-failed")
                    }
            case .waitingForMixedOverlayPopover:
                guard state["mixedOverlayStackOpenedObserved"] as? Bool == true,
                      state["mixedOverlayStackA11yObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "mixed-overlay-stack-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMixedOverlayPopoverKeyboardCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMixedOverlayPopoverKeyboardClose,
                            failureReason:
                                "mixed-overlay-popover-keyboard-close-dispatch-failed")
                    }
            case .waitingForMixedOverlayPopoverKeyboardClose:
                guard state["mixedOverlayStackTabOrderObserved"] as? Bool == true,
                      state["mixedOverlayStackTopEscapeObserved"] as? Bool == true,
                      state["mixedOverlayStackParentRemainsOpenObserved"] as? Bool == true,
                      state["mixedOverlayStackPopoverFocusRestoredObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "mixed-overlay-popover-keyboard-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMixedOverlayPopoverReopenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMixedOverlayPopoverOutsideOpen,
                            failureReason: "mixed-overlay-popover-reopen-dispatch-failed")
                    }
            case .waitingForMixedOverlayPopoverOutsideOpen:
                guard state["mixedOverlayPopoverOpened"] as? Bool == true,
                      state["mixedOverlayStackOpenedObserved"] as? Bool == true,
                      self.standardFrameworkString(
                        state, "mixedOverlayPopoverLifecycleStatus")
                        .contains("Mixed overlay Popover lifecycle opened: client=true") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "mixed-overlay-popover-reopen-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMixedOverlayPopoverOutsideCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMixedOverlayPopoverOutsideClose,
                            failureReason:
                                "mixed-overlay-popover-outside-close-dispatch-failed")
                    }
            case .waitingForMixedOverlayPopoverOutsideClose:
                guard state["mixedOverlayStackOutsideCloseObserved"] as? Bool == true,
                      state["mixedOverlayStackOutsideFocusPreservedObserved"] as? Bool == true,
                      state["mixedOverlayStackParentRemainsOpenObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "mixed-overlay-popover-outside-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkMixedOverlayDialogCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForMixedOverlayDialogClose,
                            failureReason: "mixed-overlay-dialog-close-dispatch-failed")
                    }
            case .waitingForMixedOverlayDialogClose:
                guard state["mixedOverlayStackParentEscapeObserved"] as? Bool == true,
                      state["mixedOverlayStackLayeredFocusRestoredObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "mixed-overlay-dialog-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverOuterOpenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverOuter,
                            failureReason: "nested-popover-outer-open-dispatch-failed")
                    }
            case .waitingForNestedPopoverOuter:
                guard state["nestedAnchoredOverlayStackOuterOpenedObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-outer-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverInnerOpenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverInner,
                            failureReason: "nested-popover-inner-open-dispatch-failed")
                    }
            case .waitingForNestedPopoverInner:
                guard state["nestedAnchoredOverlayStackOpenedObserved"] as? Bool == true,
                      state["nestedAnchoredOverlayStackA11yObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-stack-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverKeyboardCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverKeyboardClose,
                            failureReason: "nested-popover-keyboard-close-dispatch-failed")
                    }
            case .waitingForNestedPopoverKeyboardClose:
                guard state["nestedAnchoredOverlayStackTabOrderObserved"] as? Bool == true,
                      state["nestedAnchoredOverlayStackTopEscapeObserved"] as? Bool == true,
                      state["nestedAnchoredOverlayStackParentRemainsOpenObserved"] as? Bool == true,
                      state["nestedAnchoredOverlayStackInnerFocusRestoredObserved"] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-keyboard-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverOuterKeyboardCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverOuterKeyboardClose,
                            failureReason:
                                "nested-popover-outer-keyboard-close-dispatch-failed")
                    }
            case .waitingForNestedPopoverOuterKeyboardClose:
                guard state["nestedAnchoredOverlayStackParentEscapeObserved"] as? Bool == true,
                      state["nestedAnchoredOverlayStackLayeredFocusRestoredObserved"] as? Bool
                        == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-outer-keyboard-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverOutsideOuterOpenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverOutsideOuterOpen,
                            failureReason:
                                "nested-popover-outside-outer-open-dispatch-failed")
                    }
            case .waitingForNestedPopoverOutsideOuterOpen:
                guard state["nestedPopoverOuterOpened"] as? Bool == true,
                      self.standardFrameworkString(
                        state, "nestedPopoverOuterLifecycleStatus")
                        .contains("Nested outer Popover lifecycle opened: client=true") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-outside-outer-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverOutsideInnerOpenAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverOutsideInnerOpen,
                            failureReason:
                                "nested-popover-outside-inner-open-dispatch-failed")
                    }
            case .waitingForNestedPopoverOutsideInnerOpen:
                guard state["nestedPopoverOuterOpened"] as? Bool == true,
                      state["nestedPopoverInnerOpened"] as? Bool == true,
                      state["nestedAnchoredOverlayStackOpenedObserved"] as? Bool == true,
                      self.standardFrameworkString(
                        state, "nestedPopoverInnerLifecycleStatus")
                        .contains("Nested inner Popover lifecycle opened: client=true") else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-outside-inner-open-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverOutsideTopCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverOutsideTopClose,
                            failureReason:
                                "nested-popover-outside-top-close-dispatch-failed")
                    }
            case .waitingForNestedPopoverOutsideTopClose:
                guard state["nestedAnchoredOverlayStackOutsideTopCloseObserved"] as? Bool == true,
                      state[
                        "nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved"
                      ] as? Bool == true,
                      state[
                        "nestedAnchoredOverlayStackOutsideFocusPreservedObserved"
                      ] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-outside-top-close-not-observed",
                        state: state)
                    return
                }
                self.dispatchStandardBundleFrameworkAction(
                    context.webView,
                    script: Self.standardFrameworkNestedPopoverOutsideParentCloseAction) {
                        dispatched in
                        self.advanceStandardBundleFrameworkAcceptance(
                            context,
                            dispatched: dispatched,
                            next: .waitingForNestedPopoverOutsideParentClose,
                            failureReason:
                                "nested-popover-outside-parent-close-dispatch-failed")
                    }
            case .waitingForNestedPopoverOutsideParentClose:
                guard state[
                        "nestedAnchoredOverlayStackOutsideParentCloseObserved"
                      ] as? Bool == true,
                      state[
                        "nestedAnchoredOverlayStackOutsideFocusPreservedObserved"
                      ] as? Bool == true else {
                    self.retryOrFailStandardBundleFrameworkAcceptance(
                        context,
                        reason: "nested-popover-outside-parent-close-not-observed",
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
                "lazyFrontendRouteObserved": true,
                "lazyFrontendClientObserved": true,
                "lazyFrontendServerObserved": true,
                "lazyFrontendReturned": true,
                "nestedLazyLayoutObserved": true,
                "nestedLazyRouteObserved": true,
                "nestedLazyTransitiveModuleObserved": true,
                "nestedLazyClientObserved": true,
                "nestedLazyServerObserved": true,
                "nestedLazyReturned": true,
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
                "sliderClientObserved": true,
                "sliderServerObserved": true,
                "sliderStatus": "Slider server value: 9.0 range=2.0-12.0 step=1.0",
                "passwordFieldStatus": "Secret changed: length=10",
                "timePickerStatus": "Review time changed: 14:45",
                "cardClientObserved": true,
                "cardServerObserved": true,
                "cardStatus":
                    "Card clicked: count=1 | Card server state: elevated horizontal patched",
                "markdownClientObserved": true,
                "markdownServerObserved": true,
                "markdownStatus":
                    "Markdown clicked: count=1 | Markdown server state: patched appended",
                "badgeClientObserved": true,
                "badgeServerObserved": true,
                "badgeStatus":
                    "Badge clicked: count=1 | Badge server state: approved number=12 success filled icon=cog role=mark",
                "assigneesStatus": "Assignees changed: ava,sol",
                "dialogEventObserved": true,
                "dialogInitialFocusObserved": true,
                "dialogFocusTrapObserved": true,
                "dialogEscapeCloseObserved": true,
                "dialogFocusRestoredObserved": true,
                "dialogA11yObserved": true,
                "dialogLifecycleStatus": "Dialog lifecycle closed: client=true",
                "modalOverlayStackOpenedObserved": true,
                "modalOverlayStackInitialFocusObserved": true,
                "modalOverlayStackFocusTrapObserved": true,
                "modalOverlayStackTopEscapeObserved": true,
                "modalOverlayStackParentRemainsOpenObserved": true,
                "modalOverlayStackFocusRestoredObserved": true,
                "modalOverlayStackA11yObserved": true,
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
                "masterDetailClientObserved": true,
                "masterDetailContentObserved": true,
                "masterDetailServerObserved": true,
                "masterDetailStatus":
                    "MasterDetail backdrop clicked: count=1 | MasterDetail server state: vertical stack overlay viewport patched animation=enabled",
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
                "lazyGridFarViewportObserved": true,
                "lazyGridSelectionObserved": true,
                "lazyGridReturnViewportObserved": true,
                "lazyGridSortObserved": true,
                "lazyGridFilterObserved": true,
                "lazyGridFilterResetObserved": true,
                "lazyGridQueryStatus":
                    standardFrameworkString(state, "lazyGridQueryStatus"),
                "lazyGridSelectionStatus":
                    standardFrameworkString(state, "lazyGridSelectionStatus"),
                "treeGridRootLoadedObserved": true,
                "treeGridPlatformExpandedObserved": true,
                "treeGridRuntimeExpandedObserved": true,
                "treeGridSelectionObserved": true,
                "treeGridRefreshObserved": true,
                "treeGridCollapseObserved": true,
                "treeGridQueryStatus":
                    standardFrameworkString(state, "treeGridQueryStatus"),
                "treeGridCountStatus":
                    standardFrameworkString(state, "treeGridCountStatus"),
                "treeGridLifecycleStatus":
                    standardFrameworkString(state, "treeGridLifecycleStatus"),
                "treeGridSelectionStatus":
                    standardFrameworkString(state, "treeGridSelectionStatus"),
                "treeGridRefreshStatus":
                    standardFrameworkString(state, "treeGridRefreshStatus"),
                "componentRendererObserved": true,
                "gridEditorOpenedObserved": true,
                "gridEditorSaveObserved": true,
                "gridEditorCancelObserved": true,
                "gridEditorValidationObserved": true,
                "gridEditorValidationCancelObserved": true,
                "litRendererPropertiesObserved": true,
                "litRendererFunctionObserved": true,
                "litRendererRefreshObserved": true,
                "clientCallableResolveObserved": true,
                "clientCallableRejectObserved": true,
                "pendingJavaScriptResolveObserved": true,
                "pendingJavaScriptRejectObserved": true,
                "flowDndConfiguredObserved": true,
                "flowDndDragStartObserved": true,
                "flowDndDropObserved": true,
                "flowDndDragEndObserved": true,
                "editorRendererStatus":
                    standardFrameworkString(state, "editorRendererStatus"),
                "editorOpenStatus":
                    standardFrameworkString(state, "editorOpenStatus"),
                "editorSaveStatus":
                    standardFrameworkString(state, "editorSaveStatus"),
                "editorCancelStatus":
                    standardFrameworkString(state, "editorCancelStatus"),
                "editorValidationStatus":
                    standardFrameworkString(state, "editorValidationStatus"),
                "litRendererStatus":
                    standardFrameworkString(state, "litRendererStatus"),
                "clientCallableStatus":
                    standardFrameworkString(state, "clientCallableStatus"),
                "pendingJavaScriptSuccessStatus":
                    standardFrameworkString(
                        state, "pendingJavaScriptSuccessStatus"),
                "pendingJavaScriptFailureStatus":
                    standardFrameworkString(
                        state, "pendingJavaScriptFailureStatus"),
                "flowDndDragStartStatus":
                    standardFrameworkString(state, "flowDndDragStartStatus"),
                "flowDndDropStatus":
                    standardFrameworkString(state, "flowDndDropStatus"),
                "flowDndDragEndStatus":
                    standardFrameworkString(state, "flowDndDragEndStatus"),
                "virtualListClientObserved": true,
                "virtualListServerObserved": true,
                "virtualListStatus": standardFrameworkString(state, "virtualListStatus"),
                "popoverOpenObserved": true,
                "anchoredModelessOverlayObserved": true,
                "anchoredModelessOverlayA11yObserved": true,
                "anchoredModelessOverlayTabOrderObserved": true,
                "anchoredModelessOverlayEscapeCloseObserved": true,
                "anchoredModelessOverlayEscapeFocusRestoredObserved": true,
                "anchoredModelessOverlayOutsideCloseObserved": true,
                "anchoredModelessOverlayOutsideFocusPreservedObserved": true,
                "mixedOverlayStackDialogOpenedObserved": true,
                "mixedOverlayStackOpenedObserved": true,
                "mixedOverlayStackA11yObserved": true,
                "mixedOverlayStackTabOrderObserved": true,
                "mixedOverlayStackTopEscapeObserved": true,
                "mixedOverlayStackParentRemainsOpenObserved": true,
                "mixedOverlayStackPopoverFocusRestoredObserved": true,
                "mixedOverlayStackOutsideCloseObserved": true,
                "mixedOverlayStackOutsideFocusPreservedObserved": true,
                "mixedOverlayStackParentEscapeObserved": true,
                "mixedOverlayStackLayeredFocusRestoredObserved": true,
                "nestedAnchoredOverlayStackOuterOpenedObserved": true,
                "nestedAnchoredOverlayStackOpenedObserved": true,
                "nestedAnchoredOverlayStackA11yObserved": true,
                "nestedAnchoredOverlayStackTabOrderObserved": true,
                "nestedAnchoredOverlayStackTopEscapeObserved": true,
                "nestedAnchoredOverlayStackParentRemainsOpenObserved": true,
                "nestedAnchoredOverlayStackInnerFocusRestoredObserved": true,
                "nestedAnchoredOverlayStackParentEscapeObserved": true,
                "nestedAnchoredOverlayStackLayeredFocusRestoredObserved": true,
                "nestedAnchoredOverlayStackOutsideTopCloseObserved": true,
                "nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved": true,
                "nestedAnchoredOverlayStackOutsideFocusPreservedObserved": true,
                "nestedAnchoredOverlayStackOutsideParentCloseObserved": true,
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
          const clientsIdle = clients.length > 0 && clients.every((name) => {
            const client = window.Vaadin.Flow.clients[name];
            return typeof client.isActive === 'function' && client.isActive() === false;
          });
          // Read-only acceptance diagnostics: upstream server objects retain
          // pending Promise callbacks even when resync replaces their DOM nodes.
          const rpcOwners = diagnostics.frameworkRpcOwners ||= [];
          const rememberRpcOwners = (root) => {
            for (const element of root.querySelectorAll('*')) {
              if (element.$server && !rpcOwners.some((owner) => owner.server === element.$server)) {
                const owner = {server: element.$server, tag: element.localName,
                  id: element.id, methods: Object.keys(element.$server), calls: {}};
                rpcOwners.push(owner);
                // Acceptance-only observation; preserve return identity, thrown
                // errors and receiver. Never attach handlers to returned Promises.
                const callbacks = () => Object.getOwnPropertyNames(owner.server)
                  .map((key) => owner.server[key]?.promises).find(Array.isArray);
                for (const method of owner.methods) {
                  const descriptor = Object.getOwnPropertyDescriptor(owner.server, method);
                  const original = descriptor?.value;
                  if (typeof original !== 'function' || !descriptor.writable) continue;
                  Object.defineProperty(owner.server, method, {...descriptor, value: function(...args) {
                    const before = callbacks()?.length || 0;
                    try { return Reflect.apply(original, this, args); }
                    finally {
                      const current = callbacks();
                      if (current) for (let index = before; index < current.length; index++) {
                        if (current[index]) owner.calls[index] = method;
                      }
                      // Keep only live callbacks; never retain arguments/results.
                      for (const index of Object.keys(owner.calls)) {
                        if (!current?.[index]) delete owner.calls[index];
                      }
                    }
                  }});
                }
              }
              if (element.shadowRoot) rememberRpcOwners(element.shadowRoot);
            }
          };
          rememberRpcOwners(document);
          const pendingRpcOwners = rpcOwners.flatMap((owner) => {
            const pending = Object.getOwnPropertyNames(owner.server).flatMap((key) => {
              const callbacks = owner.server[key]?.promises;
              return Array.isArray(callbacks) ? callbacks.flatMap((entry, index) =>
                Array.isArray(entry) && typeof entry[0] === 'function' && typeof entry[1] === 'function'
                  ? [index] : []) : [];
            });
            return pending.length ? [{tag: owner.tag, id: owner.id, methods: owner.methods, pending,
              calls: pending.map((index) => ({index, method: owner.calls[index] || 'unobserved'}))}] : [];
          });
          diagnostics.frameworkPendingRpcOwners = pendingRpcOwners;
          if (clientsIdle && Number(diagnostics.standardBundlePushMessageCount || 0) >= 1
              && Number(diagnostics.standardBundlePushReconnectCount || 0) === 0
              && !diagnostics.standardBundlePushReconnectTriggered) {
            diagnostics.standardBundlePushReconnectTriggered = true;
            diagnostics.frameworkResyncRpcRequests = [...(diagnostics.frameworkRpcRequests || [])];
            const push = window.VaadinDesktopPush;
            if (push && typeof push.receive === 'function') {
              const originalReceive = push.receive;
              push.receive = function(envelope) {
                if (envelope?.type !== 'opaquePush' || envelope.payload?.resync !== true) {
                  return Reflect.apply(originalReceive, this, arguments);
                }
                push.receive = originalReceive;
                const result = Reflect.apply(originalReceive, this, arguments);
                const before = {
                  messages: Number(diagnostics.standardBundlePushMessageCount || 0),
                  resyncs: Number(diagnostics.standardBundlePushResyncCount || 0),
                  duplicates: Number(diagnostics.standardBundlePushDuplicateCount || 0),
                  acks: Number(diagnostics.standardBundlePushAckCount || 0)
                };
                // Replay an unchanged, actual server snapshot. No UIDL parsing,
                // synthetic snapshot, delayed ACK or Promise interception.
                Reflect.apply(originalReceive, this, [envelope]);
                diagnostics.frameworkResyncReplay = {
                  source: 'test-replay-of-server-envelope',
                  connectionId: envelope.payload.connectionId,
                  sequence: envelope.payload.pushSequence,
                  messagesDelta: Number(diagnostics.standardBundlePushMessageCount || 0) - before.messages,
                  resyncsDelta: Number(diagnostics.standardBundlePushResyncCount || 0) - before.resyncs,
                  duplicatesDelta: Number(diagnostics.standardBundlePushDuplicateCount || 0) - before.duplicates,
                  acksDelta: Number(diagnostics.standardBundlePushAckCount || 0) - before.acks
                };
                const replay = diagnostics.frameworkResyncReplay;
                replay.observed = replay.messagesDelta === 0 && replay.resyncsDelta === 0
                  && replay.duplicatesDelta === 1 && replay.acksDelta === 1;
                return result;
              };
            }
            push?.reconnect?.();
            const connectionId = diagnostics.standardBundlePushActiveConnectionId;
            if (connectionId) {
              push?.receive?.({
                version: 1,
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
          const detailsDialog = document.querySelector('#framework-dialog');
          const dialogOpenTrigger = document.querySelector('#framework-dialog-open');
          const dialogFocusField = document.querySelector('#framework-dialog-focus-field');
          const dialogLifecycleStatus =
            document.querySelector('#framework-dialog-lifecycle-status')?.textContent || '';
          const dialogOverlay = findDeep((element) =>
            element.localName === 'vaadin-dialog-overlay');
          const dialogOpened = detailsDialog?.opened === true
            || detailsDialog?.hasAttribute('opened') === true
            || dialogOverlay?.opened === true
            || dialogOverlay?.hasAttribute?.('opened') === true;
          const dialogActiveElements = activeDeepElements();
          const dialogInitialFocusObserved = dialogOpened
            && !!dialogFocusField
            && dialogActiveElements.some((element) =>
              element === dialogFocusField
                || element === dialogFocusField.inputElement
                || dialogFocusField.contains?.(element));
          const dialogRoleElement =
            (['dialog', 'alertdialog'].includes(
              String(detailsDialog?.getAttribute?.('role') || '').toLowerCase())
              ? detailsDialog : null)
            || (dialogOverlay && (
              (['dialog', 'alertdialog'].includes(
                String(dialogOverlay.getAttribute?.('role') || '').toLowerCase())
                ? dialogOverlay : null)
              || findDeepFrom(dialogOverlay, (element) =>
                ['dialog', 'alertdialog'].includes(
                  String(element.getAttribute?.('role') || '').toLowerCase()))
            ));
          const dialogAccessibleText = [
            detailsDialog?.getAttribute?.('aria-label') || '',
            dialogRoleElement?.getAttribute?.('aria-label') || '',
            dialogOverlay ? collectDeepText(dialogOverlay) : ''
          ].join(' ');
          const dialogAriaModal = String(
            dialogRoleElement?.getAttribute?.('aria-modal')
              || dialogOverlay?.getAttribute?.('aria-modal')
              || '');
          const dialogA11yObserved = dialogOpened
            && !!dialogRoleElement
            && dialogAriaModal === 'true'
            && dialogAccessibleText.includes('Flow IPC keyboard dialog');
          const dialogKeyboardMarker =
            window.VaadinDesktopFrameworkDialogKeyboard || {};
          const dialogFocusTrapObserved =
            dialogKeyboardMarker.focusTrapObserved === true;
          const dialogEscapeCloseObserved =
            dialogKeyboardMarker.escapeDispatched === true
              && !dialogOpened
              && dialogLifecycleStatus.includes(
                'Dialog lifecycle closed: client=true');
          const dialogFocusRestoredObserved =
            dialogKeyboardMarker.escapeDispatched === true
              && !dialogOpened
              && !!dialogOpenTrigger
              && activeDeepElements().includes(dialogOpenTrigger);
          const nestedDialog = document.querySelector('#framework-nested-dialog');
          const nestedDialogOpenTrigger =
            document.querySelector('#framework-nested-dialog-open');
          const nestedDialogFocusField =
            document.querySelector('#framework-nested-dialog-focus-field');
          const nestedDialogLifecycleStatus =
            document.querySelector('#framework-nested-dialog-lifecycle-status')
              ?.textContent || '';
          const nestedDialogOpened = nestedDialog?.opened === true
            || nestedDialog?.hasAttribute?.('opened') === true;
          const nestedDialogActiveElements = activeDeepElements();
          const nestedDialogInitialFocusObserved = dialogOpened
            && nestedDialogOpened
            && !!nestedDialogFocusField
            && nestedDialogActiveElements.some((element) =>
              element === nestedDialogFocusField
                || element === nestedDialogFocusField.inputElement
                || nestedDialogFocusField.contains?.(element));
          const nestedDialogRole =
            String(nestedDialog?.getAttribute?.('role') || '').toLowerCase();
          const nestedDialogAriaModal =
            String(nestedDialog?.getAttribute?.('aria-modal') || '');
          const nestedDialogAccessibleText = [
            nestedDialog?.getAttribute?.('aria-label') || '',
            collectDeepText(nestedDialog)
          ].join(' ');
          const nestedDialogA11yObserved = nestedDialogOpened
            && ['dialog', 'alertdialog'].includes(nestedDialogRole)
            && nestedDialogAriaModal === 'true'
            && nestedDialogAccessibleText.includes(
              'Flow IPC nested keyboard dialog');
          const nestedDialogKeyboardMarker =
            window.VaadinDesktopFrameworkNestedDialogKeyboard || {};
          const modalOverlayStackOpenedObserved =
            (dialogOpened && nestedDialogOpened)
              || nestedDialogKeyboardMarker.openedObserved === true;
          const modalOverlayStackInitialFocusObserved =
            nestedDialogInitialFocusObserved
              || nestedDialogKeyboardMarker.initialFocusObserved === true;
          const modalOverlayStackA11yObserved =
            nestedDialogA11yObserved
              || nestedDialogKeyboardMarker.a11yObserved === true;
          const modalOverlayStackFocusTrapObserved =
            nestedDialogKeyboardMarker.focusTrapObserved === true;
          const modalOverlayStackTopEscapeObserved =
            nestedDialogKeyboardMarker.escapeDispatched === true
              && !nestedDialogOpened
              && dialogOpened
              && nestedDialogLifecycleStatus.includes(
                'Nested dialog lifecycle closed: client=true');
          const modalOverlayStackParentRemainsOpenObserved =
            nestedDialogKeyboardMarker.parentOpenedBeforeEscape === true
              && !nestedDialogOpened
              && dialogOpened;
          const modalOverlayStackFocusRestoredObserved =
            nestedDialogKeyboardMarker.escapeDispatched === true
              && !nestedDialogOpened
              && dialogOpened
              && !!nestedDialogOpenTrigger
              && activeDeepElements().includes(nestedDialogOpenTrigger);
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
          const popover = document.querySelector('#framework-popover');
          const popoverTarget =
            document.querySelector('#framework-popover-target');
          const popoverFocusField =
            document.querySelector('#framework-popover-focus-field');
          const popoverOutsideTarget =
            document.querySelector('#framework-popover-outside-target');
          const popoverLifecycleStatus =
            document.querySelector('#framework-popover-lifecycle-status')
              ?.textContent || '';
          const popoverOpened = popover?.opened === true
            || popover?.hasAttribute?.('opened') === true;
          const popoverOverlay =
            popover?.shadowRoot?.querySelector('vaadin-popover-overlay');
          const popoverAriaTarget =
            popoverTarget?.ariaTarget || popoverTarget;
          const anchoredPopoverMarker =
            window.VaadinDesktopFrameworkAnchoredPopover || {};
          const anchoredModelessOverlayCurrent =
            popoverOpened
              && !!popover
              && !!popoverTarget
              && !!popoverOverlay
              && popover.target === popoverTarget
              && popoverOverlay.positionTarget === popoverTarget
              && popoverOverlay.modeless === true
              && popoverOverlay.focusTrap !== true;
          const anchoredModelessOverlayA11yCurrent =
            anchoredModelessOverlayCurrent
              && String(popover.getAttribute?.('role') || '').toLowerCase()
                === 'dialog'
              && String(popover.getAttribute?.('aria-label') || '')
                .includes('Flow IPC anchored modeless popover')
              && popoverAriaTarget?.getAttribute?.('aria-expanded') === 'true'
              && popoverAriaTarget?.getAttribute?.('aria-controls') === popover.id
              && popoverAriaTarget?.getAttribute?.('aria-haspopup') === 'dialog';
          const anchoredModelessOverlayObserved =
            anchoredModelessOverlayCurrent
              || anchoredPopoverMarker.anchoredObserved === true;
          const anchoredModelessOverlayA11yObserved =
            anchoredModelessOverlayA11yCurrent
              || anchoredPopoverMarker.a11yObserved === true;
          const anchoredModelessOverlayTabOrderObserved =
            anchoredPopoverMarker.tabForwardObserved === true
              && anchoredPopoverMarker.tabBackwardObserved === true;
          const anchoredModelessOverlayEscapeCloseObserved =
            anchoredPopoverMarker.escapeDispatched === true
              && !popoverOpened
              && (anchoredPopoverMarker.escapeCloseObserved === true
                || popoverLifecycleStatus.includes(
                  'Popover lifecycle closed: client=true'));
          const anchoredModelessOverlayEscapeFocusRestoredObserved =
            anchoredPopoverMarker.escapeFocusRestoredObserved === true
              || (anchoredPopoverMarker.escapeDispatched === true
                && !popoverOpened
                && !!popoverAriaTarget
                && activeDeepElements().includes(popoverAriaTarget));
          const anchoredModelessOverlayOutsideCloseObserved =
            anchoredPopoverMarker.outsideDispatched === true
              && !popoverOpened
              && popoverAriaTarget?.getAttribute?.('aria-expanded') === 'false'
              && !popoverAriaTarget?.hasAttribute?.('aria-controls')
              && popoverLifecycleStatus.includes(
                'Popover lifecycle closed: client=true');
          const anchoredModelessOverlayOutsideFocusPreservedObserved =
            anchoredPopoverMarker.outsideFocusPreservedObserved === true
              || (!!popoverOutsideTarget
                && activeDeepElements().includes(popoverOutsideTarget));
          const mixedOverlayDialog =
            document.querySelector('#framework-mixed-overlay-dialog');
          const mixedOverlayDialogOpenTrigger =
            document.querySelector('#framework-mixed-overlay-dialog-open');
          const mixedOverlayDialogLifecycleStatus =
            document.querySelector(
              '#framework-mixed-overlay-dialog-lifecycle-status')
              ?.textContent || '';
          const mixedOverlayPopover =
            document.querySelector('#framework-mixed-overlay-popover');
          const mixedOverlayPopoverTarget =
            document.querySelector('#framework-mixed-overlay-popover-target');
          const mixedOverlayPopoverFocusField =
            document.querySelector(
              '#framework-mixed-overlay-popover-focus-field');
          const mixedOverlayOutsideTarget =
            document.querySelector('#framework-mixed-overlay-outside-target');
          const mixedOverlayPopoverLifecycleStatus =
            document.querySelector(
              '#framework-mixed-overlay-popover-lifecycle-status')
              ?.textContent || '';
          const mixedOverlayDialogOverlay = findDeep((element) =>
            element.localName === 'vaadin-dialog-overlay'
              && collectDeepText(element).includes(
                'Flow IPC mixed overlay stack'));
          const mixedOverlayDialogOpened =
            mixedOverlayDialog?.opened === true
              || mixedOverlayDialog?.hasAttribute?.('opened') === true
              || mixedOverlayDialogOverlay?.opened === true
              || mixedOverlayDialogOverlay?.hasAttribute?.('opened') === true;
          const mixedOverlayDialogRoleElement =
            (['dialog', 'alertdialog'].includes(
              String(
                mixedOverlayDialog?.getAttribute?.('role') || '')
                .toLowerCase())
              ? mixedOverlayDialog : null)
            || (mixedOverlayDialogOverlay && (
              (['dialog', 'alertdialog'].includes(
                String(
                  mixedOverlayDialogOverlay.getAttribute?.('role') || '')
                  .toLowerCase())
                ? mixedOverlayDialogOverlay : null)
              || findDeepFrom(mixedOverlayDialogOverlay, (element) =>
                ['dialog', 'alertdialog'].includes(
                  String(element.getAttribute?.('role') || '')
                    .toLowerCase()))
            ));
          const mixedOverlayDialogA11yCurrent =
            mixedOverlayDialogOpened
              && !!mixedOverlayDialogRoleElement
              && String(
                mixedOverlayDialogRoleElement.getAttribute?.('aria-modal')
                  || mixedOverlayDialogOverlay?.getAttribute?.('aria-modal')
                  || '') === 'true'
              && [
                mixedOverlayDialog?.getAttribute?.('aria-label') || '',
                mixedOverlayDialogRoleElement.getAttribute?.('aria-label') || '',
                mixedOverlayDialogOverlay
                  ? collectDeepText(mixedOverlayDialogOverlay) : ''
              ].join(' ').includes('Flow IPC mixed overlay stack');
          const mixedOverlayDialogInitialFocusCurrent =
            mixedOverlayDialogOpened
              && !!mixedOverlayPopoverTarget
              && activeDeepElements().includes(mixedOverlayPopoverTarget);
          const mixedOverlayStackDialogOpenedObserved =
            mixedOverlayDialogOpened
              && mixedOverlayDialogInitialFocusCurrent
              && mixedOverlayDialogA11yCurrent
              && mixedOverlayDialogLifecycleStatus.includes(
                'Mixed overlay Dialog lifecycle opened: client=false');
          const mixedOverlayPopoverOpened =
            mixedOverlayPopover?.opened === true
              || mixedOverlayPopover?.hasAttribute?.('opened') === true;
          const mixedOverlayPopoverOverlay =
            mixedOverlayPopover?.shadowRoot
              ?.querySelector('vaadin-popover-overlay');
          const mixedOverlayPopoverAriaTarget =
            mixedOverlayPopoverTarget?.ariaTarget || mixedOverlayPopoverTarget;
          const mixedOverlayMarker =
            window.VaadinDesktopFrameworkMixedOverlay || {};
          const mixedOverlayStackCurrent =
            mixedOverlayDialogOpened
              && mixedOverlayPopoverOpened
              && !!mixedOverlayPopover
              && !!mixedOverlayPopoverTarget
              && !!mixedOverlayPopoverOverlay
              && mixedOverlayPopover.target === mixedOverlayPopoverTarget
              && mixedOverlayPopoverOverlay.positionTarget
                === mixedOverlayPopoverTarget
              && mixedOverlayPopoverOverlay.modeless === true
              && mixedOverlayPopoverOverlay.focusTrap !== true;
          const mixedOverlayStackA11yCurrent =
            mixedOverlayStackCurrent
              && mixedOverlayDialogA11yCurrent
              && String(
                mixedOverlayPopover.getAttribute?.('role') || '')
                .toLowerCase() === 'dialog'
              && String(
                mixedOverlayPopover.getAttribute?.('aria-label') || '')
                .includes('Flow IPC mixed modeless popover')
              && mixedOverlayPopoverAriaTarget
                ?.getAttribute?.('aria-expanded') === 'true'
              && mixedOverlayPopoverAriaTarget
                ?.getAttribute?.('aria-controls') === mixedOverlayPopover.id
              && mixedOverlayPopoverAriaTarget
                ?.getAttribute?.('aria-haspopup') === 'dialog';
          const mixedOverlayStackOpenedObserved =
            mixedOverlayStackCurrent
              || mixedOverlayMarker.openedObserved === true;
          const mixedOverlayStackA11yObserved =
            mixedOverlayStackA11yCurrent
              || mixedOverlayMarker.a11yObserved === true;
          const mixedOverlayStackTabOrderObserved =
            mixedOverlayMarker.tabForwardObserved === true
              && mixedOverlayMarker.tabBackwardObserved === true;
          const mixedOverlayStackTopEscapeObserved =
            mixedOverlayMarker.topEscapeDispatched === true
              && mixedOverlayPopoverOpened !== true
              && mixedOverlayPopoverLifecycleStatus.includes(
                'Mixed overlay Popover lifecycle closed: client=true');
          const mixedOverlayStackParentRemainsOpenObserved =
            mixedOverlayMarker.parentRemainsAfterTopEscape === true
              && (mixedOverlayMarker.outsideDispatched !== true
                || mixedOverlayMarker.parentRemainsAfterOutside === true);
          const mixedOverlayStackPopoverFocusRestoredObserved =
            mixedOverlayMarker.popoverFocusRestoredObserved === true
              || (mixedOverlayMarker.topEscapeDispatched === true
                && mixedOverlayPopoverOpened !== true
                && !!mixedOverlayPopoverTarget
                && activeDeepElements().includes(
                  mixedOverlayPopoverTarget));
          const mixedOverlayStackOutsideCloseObserved =
            mixedOverlayMarker.outsideDispatched === true
              && mixedOverlayPopoverOpened !== true
              && mixedOverlayPopoverLifecycleStatus.includes(
                'Mixed overlay Popover lifecycle closed: client=true');
          const mixedOverlayStackOutsideFocusPreservedObserved =
            mixedOverlayMarker.outsideFocusPreservedObserved === true
              || (mixedOverlayMarker.outsideDispatched === true
                && !!mixedOverlayOutsideTarget
                && activeDeepElements().includes(mixedOverlayOutsideTarget));
          const mixedOverlayStackParentEscapeObserved =
            mixedOverlayMarker.parentEscapeDispatched === true
              && mixedOverlayDialogOpened !== true
              && mixedOverlayDialogLifecycleStatus.includes(
                'Mixed overlay Dialog lifecycle closed: client=true');
          const mixedOverlayStackLayeredFocusRestoredObserved =
            mixedOverlayMarker.layeredFocusRestoredObserved === true
              || (mixedOverlayMarker.parentEscapeDispatched === true
                && mixedOverlayDialogOpened !== true
                && !!mixedOverlayDialogOpenTrigger
                && activeDeepElements().includes(
                  mixedOverlayDialogOpenTrigger));
          const nestedPopoverOuter =
            document.querySelector('#framework-nested-popover-outer');
          const nestedPopoverOuterTarget =
            document.querySelector('#framework-nested-popover-outer-target');
          const nestedPopoverOuterLifecycleStatus =
            document.querySelector(
              '#framework-nested-popover-outer-lifecycle-status')
              ?.textContent || '';
          const nestedPopoverInner =
            document.querySelector('#framework-nested-popover-inner');
          const nestedPopoverInnerTarget =
            document.querySelector('#framework-nested-popover-inner-target');
          const nestedPopoverInnerFocusField =
            document.querySelector(
              '#framework-nested-popover-inner-focus-field');
          const nestedPopoverInnerLifecycleStatus =
            document.querySelector(
              '#framework-nested-popover-inner-lifecycle-status')
              ?.textContent || '';
          const nestedPopoverOutsideTarget =
            document.querySelector('#framework-nested-popover-outside-target');
          const nestedPopoverOuterOpened =
            nestedPopoverOuter?.opened === true
              || nestedPopoverOuter?.hasAttribute?.('opened') === true;
          const nestedPopoverInnerOpened =
            nestedPopoverInner?.opened === true
              || nestedPopoverInner?.hasAttribute?.('opened') === true;
          const nestedPopoverOuterOverlay =
            nestedPopoverOuter?.shadowRoot
              ?.querySelector('vaadin-popover-overlay');
          const nestedPopoverInnerOverlay =
            nestedPopoverInner?.shadowRoot
              ?.querySelector('vaadin-popover-overlay');
          const nestedPopoverOuterAriaTarget =
            nestedPopoverOuterTarget?.ariaTarget || nestedPopoverOuterTarget;
          const nestedPopoverInnerAriaTarget =
            nestedPopoverInnerTarget?.ariaTarget || nestedPopoverInnerTarget;
          const nestedPopoverMarker =
            window.VaadinDesktopFrameworkNestedPopover || {};
          const nestedPopoverOuterCurrent =
            nestedPopoverOuterOpened
              && !!nestedPopoverOuter
              && !!nestedPopoverOuterTarget
              && !!nestedPopoverOuterOverlay
              && nestedPopoverOuter.target === nestedPopoverOuterTarget
              && nestedPopoverOuterOverlay.positionTarget
                === nestedPopoverOuterTarget
              && nestedPopoverOuterOverlay.modeless === true
              && nestedPopoverOuterOverlay.focusTrap !== true;
          const nestedPopoverOuterA11yCurrent =
            nestedPopoverOuterCurrent
              && String(
                nestedPopoverOuter.getAttribute?.('role') || '')
                .toLowerCase() === 'dialog'
              && String(
                nestedPopoverOuter.getAttribute?.('aria-label') || '')
                .includes('Flow IPC nested outer popover')
              && nestedPopoverOuterAriaTarget
                ?.getAttribute?.('aria-expanded') === 'true'
              && nestedPopoverOuterAriaTarget
                ?.getAttribute?.('aria-controls') === nestedPopoverOuter.id
              && nestedPopoverOuterAriaTarget
                ?.getAttribute?.('aria-haspopup') === 'dialog';
          const nestedAnchoredOverlayStackOuterOpenedObserved =
            nestedPopoverOuterCurrent
              || nestedPopoverMarker.outerOpenedObserved === true;
          const nestedAnchoredOverlayStackCurrent =
            nestedPopoverOuterCurrent
              && nestedPopoverInnerOpened
              && !!nestedPopoverInner
              && !!nestedPopoverInnerTarget
              && !!nestedPopoverInnerOverlay
              && nestedPopoverOuter.contains?.(nestedPopoverInner)
              && nestedPopoverOuter.contains?.(nestedPopoverInnerTarget)
              && nestedPopoverInner.target === nestedPopoverInnerTarget
              && nestedPopoverInnerOverlay.positionTarget
                === nestedPopoverInnerTarget
              && nestedPopoverInnerOverlay.modeless === true
              && nestedPopoverInnerOverlay.focusTrap !== true;
          const nestedAnchoredOverlayStackA11yCurrent =
            nestedAnchoredOverlayStackCurrent
              && nestedPopoverOuterA11yCurrent
              && String(
                nestedPopoverInner.getAttribute?.('role') || '')
                .toLowerCase() === 'dialog'
              && String(
                nestedPopoverInner.getAttribute?.('aria-label') || '')
                .includes('Flow IPC nested inner popover')
              && nestedPopoverInnerAriaTarget
                ?.getAttribute?.('aria-expanded') === 'true'
              && nestedPopoverInnerAriaTarget
                ?.getAttribute?.('aria-controls') === nestedPopoverInner.id
              && nestedPopoverInnerAriaTarget
                ?.getAttribute?.('aria-haspopup') === 'dialog';
          const nestedAnchoredOverlayStackOpenedObserved =
            nestedAnchoredOverlayStackCurrent
              || nestedPopoverMarker.stackOpenedObserved === true;
          const nestedAnchoredOverlayStackA11yObserved =
            nestedAnchoredOverlayStackA11yCurrent
              || nestedPopoverMarker.a11yObserved === true;
          const nestedAnchoredOverlayStackTabOrderObserved =
            nestedPopoverMarker.tabForwardObserved === true
              && nestedPopoverMarker.tabBackwardObserved === true;
          const nestedAnchoredOverlayStackTopEscapeObserved =
            nestedPopoverMarker.topEscapeDispatched === true
              && nestedPopoverInnerOpened !== true
              && nestedPopoverInnerAriaTarget
                ?.getAttribute?.('aria-expanded') === 'false'
              && !nestedPopoverInnerAriaTarget?.hasAttribute?.('aria-controls')
              && nestedPopoverInnerLifecycleStatus.includes(
                'Nested inner Popover lifecycle closed: client=true');
          const nestedAnchoredOverlayStackParentRemainsOpenObserved =
            nestedPopoverMarker.parentRemainsAfterTopEscape === true;
          const nestedAnchoredOverlayStackInnerFocusRestoredObserved =
            nestedPopoverMarker.innerFocusRestoredObserved === true
              || (nestedPopoverMarker.topEscapeDispatched === true
                && nestedPopoverInnerOpened !== true
                && !!nestedPopoverInnerTarget
                && activeDeepElements().includes(nestedPopoverInnerTarget));
          const nestedAnchoredOverlayStackParentEscapeObserved =
            nestedPopoverMarker.parentEscapeDispatched === true
              && nestedPopoverOuterOpened !== true
              && nestedPopoverOuterLifecycleStatus.includes(
                'Nested outer Popover lifecycle closed: client=true');
          const nestedAnchoredOverlayStackLayeredFocusRestoredObserved =
            nestedPopoverMarker.layeredFocusRestoredObserved === true
              || (nestedPopoverMarker.parentEscapeDispatched === true
                && nestedPopoverOuterOpened !== true
                && !!nestedPopoverOuterTarget
                && activeDeepElements().includes(nestedPopoverOuterTarget));
          const nestedAnchoredOverlayStackOutsideTopCloseObserved =
            nestedPopoverMarker.outsideTopDispatched === true
              && nestedPopoverInnerOpened !== true
              && nestedPopoverInnerLifecycleStatus.includes(
                'Nested inner Popover lifecycle closed: client=true');
          const nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved =
            nestedPopoverMarker.parentRemainsAfterOutsideTop === true;
          const nestedAnchoredOverlayStackOutsideFocusPreservedObserved =
            nestedPopoverMarker.outsideFocusPreservedObserved === true
              || (nestedPopoverMarker.outsideTopDispatched === true
                && !!nestedPopoverOutsideTarget
                && activeDeepElements().includes(nestedPopoverOutsideTarget));
          const nestedAnchoredOverlayStackOutsideParentCloseObserved =
            nestedPopoverMarker.outsideParentDispatched === true
              && nestedPopoverOuterOpened !== true
              && nestedPopoverOuterAriaTarget
                ?.getAttribute?.('aria-expanded') === 'false'
              && !nestedPopoverOuterAriaTarget?.hasAttribute?.('aria-controls')
              && nestedPopoverOuterLifecycleStatus.includes(
                'Nested outer Popover lifecycle closed: client=true');
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
          const slider = document.querySelector('#framework-slider');
          const sliderStatus =
            document.querySelector('#framework-slider-status')?.textContent || '';
          const sliderValue = Number(
            slider?.value ?? slider?.getAttribute?.('value') ?? Number.NaN);
          const sliderMin = Number(
            slider?.min ?? slider?.getAttribute?.('min') ?? Number.NaN);
          const sliderMax = Number(
            slider?.max ?? slider?.getAttribute?.('max') ?? Number.NaN);
          const sliderStep = Number(
            slider?.step ?? slider?.getAttribute?.('step') ?? Number.NaN);
          const sliderMinMaxVisible =
            slider?.minMaxVisible === true
              || slider?.hasAttribute?.('min-max-visible') === true
              || slider?.hasAttribute?.('minMaxVisible') === true;
          const sliderValueAlwaysVisible =
            slider?.valueAlwaysVisible === true
              || slider?.hasAttribute?.('value-always-visible') === true
              || slider?.hasAttribute?.('valueAlwaysVisible') === true;
          const sliderClientObserved =
            sliderStatus.includes('Slider client value: 6.5')
              && sliderValue === 6.5;
          const sliderServerObserved =
            sliderStatus.includes('Slider server value: 9.0 range=2.0-12.0 step=1.0')
              && sliderValue === 9
              && sliderMin === 2
              && sliderMax === 12
              && sliderStep === 1
              && sliderMinMaxVisible === false
              && sliderValueAlwaysVisible === false;
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
          const icon = document.querySelector('#framework-icon');
          const iconStatus =
            document.querySelector('#framework-icon-status')?.textContent || '';
          const iconName = String(icon?.getAttribute?.('icon') || icon?.icon || '');
          const iconFill = String(icon?.style?.fill || '');
          const iconWidth = String(icon?.style?.width || '');
          const iconHeight = String(icon?.style?.height || '');
          const iconAriaLabel = String(icon?.getAttribute?.('aria-label') || '');
          const iconClickMatch = iconStatus.match(
            /Icon clicked: icon=vaadin:clipboard-check count=(\\d+)/);
          const iconClickCount = iconClickMatch ? Number(iconClickMatch[1]) : 0;
          const iconInitialShapeObserved =
            iconName === 'vaadin:clipboard-check'
              && iconFill === 'var(--lumo-primary-color)'
              && iconWidth === '24px'
              && iconHeight === '24px'
              && iconAriaLabel === 'Framework icon';
          const iconServerObserved =
            iconStatus.includes('Icon server state: vaadin:cog color=success size=32')
              && iconName === 'vaadin:cog'
              && iconFill === 'var(--lumo-success-color)'
              && iconWidth === '32px'
              && iconHeight === '32px'
              && iconAriaLabel === 'Framework icon';
          const iconClientObserved =
            iconClickCount > 0 && (iconInitialShapeObserved || iconServerObserved);
          const appLayout = document.querySelector('#framework-app-layout');
          const appLayoutStatus =
            document.querySelector('#framework-app-layout-status')?.textContent || '';
          const appLayoutContent =
            document.querySelector('#framework-app-layout-content')?.textContent || '';
          const appLayoutTitle =
            document.querySelector('#framework-app-layout-title')?.textContent || '';
          const appLayoutPrimarySection = String(
            appLayout?.primarySection
              || appLayout?.getAttribute?.('primary-section')
              || appLayout?.getAttribute?.('primarySection')
              || '');
          const appLayoutDrawerOpened =
            appLayout?.drawerOpened === true
              || appLayout?.hasAttribute?.('drawer-opened') === true
              || appLayout?.hasAttribute?.('drawerOpened') === true;
          const appLayoutToggleMatch = appLayoutStatus.match(
            /AppLayout toggle clicked: count=(\\d+)/);
          const appLayoutToggleCount =
            appLayoutToggleMatch ? Number(appLayoutToggleMatch[1]) : 0;
          const appLayoutClientObserved =
            appLayoutToggleCount > 0
              && appLayoutTitle.includes('IPC Shell')
              && !!document.querySelector('#framework-app-layout-drawer-action')
              && (appLayoutContent.includes('AppLayout content: overview')
                || appLayoutContent.includes('AppLayout content: patched navbar closed'));
          const appLayoutServerObserved =
            appLayoutStatus.includes('AppLayout server state: primary=navbar drawer=false')
              && appLayoutContent.includes('AppLayout content: patched navbar closed')
              && appLayoutPrimarySection.toLowerCase().includes('navbar')
              && appLayoutDrawerOpened === false;
          const masterDetailLayout =
            document.querySelector('#framework-master-detail-layout');
          const masterDetailBackdrop =
            masterDetailLayout?.shadowRoot?.querySelector?.('[part="backdrop"]');
          const masterDetailBackdropRendered = !!masterDetailBackdrop;
          const masterDetailStatus =
            document.querySelector('#framework-master-detail-status')?.textContent || '';
          const masterDetailText = collectDeepText(masterDetailLayout);
          const masterDetailTheme = String(
            masterDetailLayout?.getAttribute?.('theme') || '').toLowerCase();
          const masterDetailMasterSize = String(
            masterDetailLayout?.masterSize
              ?? masterDetailLayout?.getAttribute?.('master-size')
              ?? masterDetailLayout?.getAttribute?.('masterSize')
              ?? '');
          const masterDetailMasterMinSize = String(
            masterDetailLayout?.masterMinSize
              ?? masterDetailLayout?.getAttribute?.('master-min-size')
              ?? masterDetailLayout?.getAttribute?.('masterMinSize')
              ?? '');
          const masterDetailDetailSize = String(
            masterDetailLayout?.detailSize
              ?? masterDetailLayout?.getAttribute?.('detail-size')
              ?? masterDetailLayout?.getAttribute?.('detailSize')
              ?? '');
          const masterDetailDetailMinSize = String(
            masterDetailLayout?.detailMinSize
              ?? masterDetailLayout?.getAttribute?.('detail-min-size')
              ?? masterDetailLayout?.getAttribute?.('detailMinSize')
              ?? '');
          const masterDetailOrientation = String(
            masterDetailLayout?.orientation
              ?? masterDetailLayout?.getAttribute?.('orientation')
              ?? '').toLowerCase();
          const masterDetailContainment = String(
            masterDetailLayout?.containment
              ?? masterDetailLayout?.getAttribute?.('containment')
              ?? '').toLowerCase();
          const masterDetailForceOverlay =
            masterDetailLayout?.forceOverlay === true
              || masterDetailLayout?.hasAttribute?.('force-overlay') === true
              || masterDetailLayout?.hasAttribute?.('forceOverlay') === true;
          const masterDetailStackOverlay =
            masterDetailLayout?.stackOverlay === true
              || masterDetailLayout?.hasAttribute?.('stack-overlay') === true
              || masterDetailLayout?.hasAttribute?.('stackOverlay') === true;
          const masterDetailNoAnimation =
            masterDetailLayout?.noAnimation === true
              || masterDetailLayout?.hasAttribute?.('no-animation') === true
              || masterDetailLayout?.hasAttribute?.('noAnimation') === true;
          const masterDetailClientObserved =
            masterDetailStatus.includes('MasterDetail backdrop clicked: count=1')
              && masterDetailText.includes('Master queue seeded')
              && masterDetailText.includes('Detail virtual child ready')
              && masterDetailTheme.includes('inset-drawer')
              && masterDetailMasterSize === '150px'
              && masterDetailDetailSize === '270px'
              && masterDetailOrientation === 'horizontal'
              && masterDetailContainment === 'layout'
              && masterDetailForceOverlay === false
              && masterDetailStackOverlay === false
              && masterDetailNoAnimation === true;
          const masterDetailPatchedContentObserved =
            masterDetailStatus.includes(
              'MasterDetail server state: vertical stack overlay viewport patched')
              && masterDetailText.includes('Master queue patched')
              && masterDetailText.includes('Queue item IPC-202')
              && masterDetailText.includes('Detail virtual child replaced')
              && masterDetailMasterSize === '180px'
              && masterDetailMasterMinSize === '140px'
              && masterDetailDetailSize === '260px'
              && masterDetailDetailMinSize === '200px'
              && masterDetailOrientation === 'vertical'
              && masterDetailContainment === 'viewport'
              && masterDetailForceOverlay === true
              && masterDetailStackOverlay === true;
          const masterDetailContentObserved =
            masterDetailPatchedContentObserved
              && masterDetailStatus.includes('animation=disabled')
              && masterDetailNoAnimation === true;
          const masterDetailServerObserved =
            masterDetailPatchedContentObserved
              && masterDetailStatus.includes('animation=enabled')
              && masterDetailNoAnimation === false;
          const card = document.querySelector('#framework-card');
          const cardStatus =
            document.querySelector('#framework-card-status')?.textContent || '';
          const cardTheme = String(card?.getAttribute?.('theme') || '').toLowerCase();
          const cardAriaRole = String(
            card?.getAttribute?.('role') || card?.getAttribute?.('aria-role') || '');
          const cardAriaLabel = String(card?.getAttribute?.('aria-label') || '');
          const cardSlotText = (slot) =>
            collectDeepText(card?.querySelector?.(`[slot="${slot}"]`));
          const cardText = collectDeepText(card);
          const cardTitle = cardSlotText('title');
          const cardSubtitle = cardSlotText('subtitle');
          const cardMedia = cardSlotText('media');
          const cardHeaderPrefix = cardSlotText('header-prefix');
          const cardHeaderSuffix = cardSlotText('header-suffix');
          const cardFooter = cardSlotText('footer');
          const cardClientObserved =
            cardStatus.includes('Card clicked: count=1')
              && cardText.includes('Flow IPC card')
              && cardTheme.includes('outlined')
              && cardAriaRole === 'region'
              && cardAriaLabel === 'Framework audit card';
          const cardServerObserved =
            cardStatus.includes('Card server state: elevated horizontal patched')
              && cardTitle.includes('Flow IPC patched card')
              && cardSubtitle.includes('Card subtitle: server patched')
              && cardMedia.includes('Card media: server patched')
              && cardHeaderPrefix.includes('P2')
              && cardHeaderSuffix.includes('approved')
              && cardText.includes('Card body: server patched')
              && cardFooter.includes('Card footer: closed')
              && cardTheme.includes('elevated')
              && cardTheme.includes('horizontal')
              && cardAriaRole === 'article'
              && cardAriaLabel === 'Framework audit card patched';
          const markdown = document.querySelector('#framework-markdown');
          const markdownStatus =
            document.querySelector('#framework-markdown-status')?.textContent || '';
          const markdownContent = String(
            markdown?.content ?? markdown?.getAttribute?.('content') ?? '');
          const markdownText = collectDeepText(markdown);
          const markdownClientObserved =
            markdownStatus.includes('Markdown clicked: count=1')
              && markdownContent.includes('# Flow IPC Markdown')
              && markdownText.includes('Flow IPC Markdown')
              && markdownText.includes('client ready')
              && markdownText.includes('audit link');
          const markdownServerObserved =
            markdownStatus.includes('Markdown server state: patched appended')
              && markdownContent.includes('## Flow IPC Markdown patched')
              && markdownContent.includes('appended quote through Flow IPC')
              && markdownText.includes('Flow IPC Markdown patched')
              && markdownText.includes('server patch')
              && markdownText.includes('inline-code')
              && markdownText.includes('appended quote through Flow IPC');
          const badge = document.querySelector('#framework-badge');
          const badgeStatus =
            document.querySelector('#framework-badge-status')?.textContent || '';
          const badgeTheme = String(badge?.getAttribute?.('theme') || '').toLowerCase();
          const badgeText = collectDeepText(badge);
          const badgeNumber = String(
            badge?.number ?? badge?.getAttribute?.('number') ?? '');
          const badgeRole = String(badge?.getAttribute?.('role') || '');
          const badgeIcon = badge?.querySelector?.('[slot="icon"]');
          const badgeIconName = String(
            badgeIcon?.getAttribute?.('icon') || badgeIcon?.icon || '');
          const badgeClientObserved =
            badgeStatus.includes('Badge clicked: count=1')
              && badgeText.includes('queued')
              && badgeNumber === '7'
              && badgeTheme.includes('contrast')
              && badgeTheme.includes('small')
              && badgeRole === 'status'
              && badgeIconName === 'vaadin:clipboard-check';
          const badgeServerObserved =
            badgeStatus.includes('Badge server state: approved number=12')
              && badgeText.includes('approved')
              && badgeNumber === '12'
              && badgeTheme.includes('success')
              && badgeTheme.includes('filled')
              && badgeRole === 'mark'
              && badgeIconName === 'vaadin:cog';
          const lazyGrid = document.querySelector('#framework-lazy-grid');
          const lazyGridStatus =
            document.querySelector('#framework-lazy-grid-status')?.textContent || '';
          const lazyGridSelectionStatus =
            document.querySelector('#framework-lazy-grid-selection-status')?.textContent || '';
          const lazyGridQueryStatus =
            document.querySelector('#framework-lazy-grid-query-status')?.textContent || '';
          const lazyGridFilterValue = String(
            document.querySelector('#framework-lazy-grid-filter')?.value || '');
          const lazyGridHasVisibleItem = (label) => {
            if (!lazyGrid) return false;
            const gridRect = lazyGrid.getBoundingClientRect();
            return !!findDeepFrom(lazyGrid, (element) => {
              if (element.localName !== 'vaadin-grid-cell-content'
                  || collectDeepText(element).trim() !== label) {
                return false;
              }
              const rect = element.getBoundingClientRect();
              return rect.width > 0
                && rect.height > 0
                && rect.bottom > gridRect.top
                && rect.top < gridRect.bottom;
            });
          };
          const lazyGridFarViewportObserved =
            lazyGridHasVisibleItem('L-061')
              && ((lazyGridStatus.includes('Lazy grid fetch ')
                    && !lazyGridStatus.includes(': 0-'))
                || (lazyGridStatus.includes('Lazy grid fetches:')
                    && !lazyGridStatus.includes('range: pending')
                    && !lazyGridStatus.includes('range: 0-')));
          const lazyGridSelectionObserved =
            lazyGridSelectionStatus.includes('Lazy grid selection: L-061');
          const lazyGridReturnViewportObserved =
            lazyGridHasVisibleItem('L-001');
          const lazyGridSortObserved =
            lazyGridQueryStatus.includes('sort=owner:ascending')
              && lazyGridQueryStatus.includes('filter=all')
              && lazyGridQueryStatus.includes('matches=80')
              && lazyGridHasVisibleItem('L-002');
          const lazyGridFilterObserved =
            lazyGridFilterValue === 'platform'
              && lazyGridQueryStatus.includes('sort=owner:ascending')
              && lazyGridQueryStatus.includes('filter=platform')
              && lazyGridQueryStatus.includes('matches=26')
              && lazyGridHasVisibleItem('L-003');
          const lazyGridFilterResetObserved =
            lazyGridFilterValue === ''
              && lazyGridQueryStatus.includes('sort=owner:ascending')
              && lazyGridQueryStatus.includes('filter=all')
              && lazyGridQueryStatus.includes('matches=80')
              && lazyGridHasVisibleItem('L-002');
          const treeGrid = document.querySelector('#framework-tree-grid');
          const treeGridQueryStatus =
            document.querySelector('#framework-tree-grid-query-status')
              ?.textContent || '';
          const treeGridCountStatus =
            document.querySelector('#framework-tree-grid-count-status')
              ?.textContent || '';
          const treeGridLifecycleStatus =
            document.querySelector('#framework-tree-grid-lifecycle-status')
              ?.textContent || '';
          const treeGridSelectionStatus =
            document.querySelector('#framework-tree-grid-selection-status')
              ?.textContent || '';
          const treeGridRefreshStatus =
            document.querySelector('#framework-tree-grid-refresh-status')
              ?.textContent || '';
          const treeGridText = treeGrid ? collectDeepText(treeGrid) : '';
          const treeGridToggle = (label) => treeGrid && findDeepFrom(
            treeGrid,
            (element) =>
              element.localName === 'vaadin-grid-tree-toggle'
                && collectDeepText(element).includes(label));
          const treeGridPlatformToggle = treeGridToggle('Platform');
          const treeGridRuntimeToggle = treeGridToggle('Runtime');
          const treeGridRootLoadedObserved =
            treeGridText.includes('Platform')
              && treeGridText.includes('Product')
              && treeGridQueryStatus.includes('parent=root')
              && treeGridCountStatus.includes('parent=root, count=2');
          const treeGridPlatformExpandedObserved =
            treeGridPlatformToggle?.expanded === true
              && treeGridText.includes('Runtime')
              && treeGridText.includes('Native host')
              && treeGridQueryStatus.includes('parent=domain-platform')
              && treeGridLifecycleStatus.includes(
                'Tree grid expand: domain-platform client=true');
          const treeGridRuntimeExpandedObserved =
            treeGridPlatformToggle?.expanded === true
              && treeGridRuntimeToggle?.expanded === true
              && treeGridText.includes('Opaque IPC')
              && treeGridText.includes('Push')
              && treeGridQueryStatus.includes('parent=area-runtime')
              && treeGridLifecycleStatus.includes(
                'Tree grid expand: area-runtime client=true');
          const treeGridSelectionObserved =
            treeGridSelectionStatus.includes(
              'Tree grid selection: capability-opaque-ipc');
          const treeGridRefreshObserved =
            treeGridRefreshStatus.includes(
              'Tree grid refresh: area-runtime children=3')
              && treeGridQueryStatus.includes('parent=area-runtime')
              && treeGridText.includes('Trace inspector');
          const treeGridCollapseObserved =
            treeGridLifecycleStatus.includes(
              'Tree grid collapse: domain-platform client=true')
              && treeGridPlatformToggle?.expanded !== true;
          const editorGrid = document.querySelector('#framework-editor-grid');
          const editorRendererStatus =
            document.querySelector('#framework-editor-renderer-status')
              ?.textContent || '';
          const editorOpenStatus =
            document.querySelector('#framework-editor-open-status')
              ?.textContent || '';
          const editorSaveStatus =
            document.querySelector('#framework-editor-save-status')
              ?.textContent || '';
          const editorCancelStatus =
            document.querySelector('#framework-editor-cancel-status')
              ?.textContent || '';
          const editorValidationStatus =
            document.querySelector('#framework-editor-validation-status')
              ?.textContent || '';
          const litRendererStatus =
            document.querySelector('#framework-lit-renderer-status')
              ?.textContent || '';
          const flowRpcProbe =
            document.querySelector('#framework-flow-rpc-probe');
          const clientCallableStatus =
            document.querySelector('#framework-client-callable-status')
              ?.textContent || '';
          const pendingJavaScriptSuccessStatus =
            document.querySelector(
              '#framework-pending-javascript-success-status')
              ?.textContent || '';
          const pendingJavaScriptFailureStatus =
            document.querySelector(
              '#framework-pending-javascript-failure-status')
              ?.textContent || '';
          const editorGridText = editorGrid ? collectDeepText(editorGrid) : '';
          const editorOpenButton = (id) => editorGrid && findDeepFrom(
            editorGrid,
            (element) => element.id === `framework-editor-open-${id}`);
          const editorOwnerField = editorGrid && findDeepFrom(
            editorGrid,
            (element) => element.id === 'framework-editor-owner-field');
          const editorOwnerRect = editorOwnerField?.getBoundingClientRect?.();
          const gridEditorActiveFieldVisible =
            !!editorOwnerRect
              && editorOwnerRect.width > 0
              && editorOwnerRect.height > 0;
          const gridEditorActiveFieldInvalid =
            editorOwnerField?.invalid === true
              || editorOwnerField?.hasAttribute?.('invalid') === true;
          const componentRendererObserved =
            !!editorOpenButton('E-100')
              && !!editorOpenButton('E-101')
              && !!editorOpenButton('E-102')
              && editorRendererStatus.includes('E-100')
              && editorRendererStatus.includes('E-101')
              && editorRendererStatus.includes('E-102');
          const gridEditorOpenedObserved =
            editorOpenStatus.includes('E-100');
          const gridEditorSaveObserved =
            editorSaveStatus.includes(
              'Grid editor save: E-100 owner=desktop-updated')
              && editorGridText.includes('desktop-updated');
          const gridEditorCancelOpenedObserved =
            editorOpenStatus.includes('E-101');
          const gridEditorCancelObserved =
            editorCancelStatus.includes('E-101 owner=desktop')
              && editorGridText.includes('desktop')
              && !editorGridText.includes('should-not-stick');
          const gridEditorValidationOpenedObserved =
            editorOpenStatus.includes('E-102');
          const gridEditorValidationObserved =
            editorValidationStatus.includes(
              'Grid editor validation blocked: E-102');
          const gridEditorValidationCancelObserved =
            editorCancelStatus.includes('E-102 owner=audit')
              && editorGridText.includes('audit');
          const litRendererButton = (id, state) => editorGrid && findDeepFrom(
            editorGrid,
            (element) => element.localName === 'button'
              && (element.textContent || '').replace(/\\s+/g, ' ')
                .includes(`Lit ${id}: ${state}`));
          const litRendererInitialE100 =
            litRendererButton('E-100', 'queued');
          const litRendererUpdatedE100 =
            litRendererButton('E-100', 'lit-updated');
          const litRendererPropertiesObserved =
            !!(litRendererInitialE100 || litRendererUpdatedE100)
              && !!litRendererButton('E-101', 'review')
              && !!litRendererButton('E-102', 'ready');
          const litRendererFunctionObserved =
            litRendererStatus.includes(
              'LitRenderer function: E-100 state=lit-updated');
          const litRendererRefreshObserved =
            litRendererFunctionObserved && !!litRendererUpdatedE100;
          const clientCallableResolveObserved =
            clientCallableStatus.includes(
              'ClientCallable invoked: desktop-rpc')
              && flowRpcProbe?.dataset?.clientCallableResolved
                === 'ClientCallable response: desktop-rpc'
              && !flowRpcProbe?.dataset?.clientCallableResolveError;
          const clientCallableRejectObserved =
            flowRpcProbe?.dataset?.clientCallableRejected === 'true'
              && !!flowRpcProbe?.dataset?.clientCallableRejectError
              && !flowRpcProbe?.dataset?.clientCallableUnexpectedValue;
          const pendingJavaScriptResolveObserved =
            pendingJavaScriptSuccessStatus.includes(
              'Pending JavaScript resolved: pending-js:desktop-rpc');
          const pendingJavaScriptRejectObserved =
            pendingJavaScriptFailureStatus.includes(
              'Pending JavaScript rejected:')
              && pendingJavaScriptFailureStatus.includes(
                'pending-js-rejected');
          const flowDndSource =
            document.querySelector('#framework-flow-dnd-source');
          const flowDndTarget =
            document.querySelector('#framework-flow-dnd-target');
          const flowDndRecord = window.VaadinDesktopFlowDnd || {};
          const flowDndDragStartStatus =
            document.querySelector('#framework-flow-dnd-start-status')
              ?.textContent || '';
          const flowDndDropStatus =
            document.querySelector('#framework-flow-dnd-drop-status')
              ?.textContent || '';
          const flowDndDragEndStatus =
            document.querySelector('#framework-flow-dnd-end-status')
              ?.textContent || '';
          const flowDndConfiguredObserved =
            flowDndSource?.draggable === true
              && flowDndSource?.__effectAllowed === 'move'
              && flowDndTarget?.__active === true
              && flowDndTarget?.__dropEffect === 'move';
          const flowDndDragStartObserved =
            flowDndDragStartStatus.includes(
              'Flow DnD drag start: source=framework-flow-dnd-source')
              && flowDndDragStartStatus.includes('data=flow-dnd-payload')
              && flowDndDragStartStatus.includes('effectAllowed=MOVE')
              && flowDndRecord.dragStartClassObserved === true
              && flowDndRecord.effectAllowedAfterStart === 'move';
          const flowDndDropObserved =
            flowDndDropStatus.includes(
              'Flow DnD drop: source=framework-flow-dnd-source')
              && flowDndDropStatus.includes('data=flow-dnd-payload')
              && flowDndDropStatus.includes('effectAllowed=MOVE')
              && flowDndDropStatus.includes('dropEffect=MOVE')
              && flowDndRecord.dragOverClassObserved === true
              && flowDndRecord.dropDefaultPrevented === true
              && flowDndRecord.dropEffectAfterDrop === 'move'
              && flowDndRecord.dragOverClassCleared === true;
          const flowDndDragEndObserved =
            flowDndDragEndStatus.includes(
              'Flow DnD drag end: dropEffect=MOVE successful=true')
              && flowDndRecord.draggedClassCleared === true;
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
          const lazyFrontendRoute =
            document.querySelector('#framework-lazy-frontend-route');
          const lazyFrontendProbe =
            document.querySelector('#framework-lazy-frontend-probe');
          const lazyFrontendStatus =
            document.querySelector('#framework-lazy-frontend-status')?.textContent || '';
          const lazyFrontendText =
            lazyFrontendProbe ? collectDeepText(lazyFrontendProbe) : '';
          const lazyFrontendRouteObserved =
            window.location.pathname.endsWith('/lazy-frontend')
              && !!lazyFrontendRoute
              && !!lazyFrontendProbe
              && !!customElements.get('vatori-lazy-probe')
              && lazyFrontendStatus.includes('Lazy frontend route ready')
              && lazyFrontendText.includes('Lazy frontend module: module ready');
          const lazyFrontendClientObserved =
            lazyFrontendStatus.includes('Lazy frontend client event: count=1')
              && lazyFrontendText.includes(
                'Lazy frontend module: client event acknowledged');
          const lazyFrontendServerObserved =
            lazyFrontendStatus.includes('Lazy frontend server state: patched')
              && lazyFrontendText.includes('Lazy frontend module: server patched');
          const nestedLazyLayout =
            document.querySelector('#framework-nested-lazy-layout');
          const nestedLazyLayoutProbe =
            document.querySelector('#framework-nested-lazy-layout-probe');
          const nestedLazyRoute =
            document.querySelector('#framework-nested-lazy-route');
          const nestedLazyProbe =
            document.querySelector('#framework-nested-lazy-probe');
          const nestedLazyStatus =
            document.querySelector('#framework-nested-lazy-status')?.textContent || '';
          const nestedLazyLayoutText =
            nestedLazyLayoutProbe ? collectDeepText(nestedLazyLayoutProbe) : '';
          const nestedLazyText =
            nestedLazyProbe ? collectDeepText(nestedLazyProbe) : '';
          const nestedLazyLayoutStyleToken = nestedLazyLayout
            ? getComputedStyle(nestedLazyLayout)
              .getPropertyValue('--vatori-lazy-layout-loaded').trim()
            : '';
          const nestedLazyRouteStyleToken = nestedLazyRoute
            ? getComputedStyle(nestedLazyRoute)
              .getPropertyValue('--vatori-lazy-route-loaded').trim()
            : '';
          const nestedLazyPathObserved =
            window.location.pathname.endsWith('/lazy-nested');
          const nestedLazyLayoutObserved =
            nestedLazyPathObserved
              && !!nestedLazyLayout
              && !!nestedLazyLayoutProbe
              && !!customElements.get('vatori-lazy-layout-probe')
              && nestedLazyLayoutText.includes(
                'Nested lazy layout module: layout ready')
              && nestedLazyLayoutStyleToken === 'layout-ready';
          const nestedLazyRouteObserved =
            nestedLazyPathObserved
              && !!nestedLazyRoute
              && !!nestedLazyProbe
              && !!customElements.get('vatori-lazy-nested-probe')
              && nestedLazyStatus.includes('Nested lazy route ready')
              && nestedLazyRouteStyleToken === 'route-ready';
          const nestedLazyTransitiveModuleObserved =
            nestedLazyText.includes(
              'Nested lazy route module: transitive module ready');
          const nestedLazyClientObserved =
            nestedLazyStatus.includes('Nested lazy client event: count=1')
              && nestedLazyText.includes(
                'Nested lazy route module: transitive client event acknowledged');
          const nestedLazyServerObserved =
            nestedLazyStatus.includes('Nested lazy server state: patched')
              && nestedLazyText.includes(
                'Nested lazy route module: transitive server patched');
          const resourceFetch = window.VaadinDesktopFrameworkResourceFetch || {};
          return JSON.stringify({
            pendingRpcOwners,
            resyncReplay: diagnostics.frameworkResyncReplay || {},
            rpcRequests: diagnostics.frameworkRpcRequests || [],
            resyncRpcRequests: diagnostics.frameworkResyncRpcRequests || [],
            rpcObservedSends: diagnostics.frameworkRpcObservedSends || 0,
            rpcRequestObserverInstalled: diagnostics.frameworkRpcRequestObserverInstalled === true,
            ready: diagnostics.standardBundleFirstMeaningfulPaintReported === true
              && diagnostics.frameworkResyncReplay?.observed === true
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
              && !!slider
              && !!document.querySelector('#framework-slider-server-update')
              && !!document.querySelector('#framework-slider-status')
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
              && !!document.querySelector('#framework-dialog')
              && !!document.querySelector('#framework-dialog-focus-field')
              && !!document.querySelector('#framework-dialog-last-action')
              && !!document.querySelector('#framework-dialog-lifecycle-status')
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
              && !!icon
              && !!document.querySelector('#framework-icon-server-update')
              && !!document.querySelector('#framework-icon-status')
              && !!appLayout
              && !!document.querySelector('#framework-app-layout-toggle')
              && !!document.querySelector('#framework-app-layout-title')
              && !!document.querySelector('#framework-app-layout-content')
              && !!document.querySelector('#framework-app-layout-drawer-action')
              && !!document.querySelector('#framework-app-layout-server-update')
              && !!document.querySelector('#framework-app-layout-status')
              && !!masterDetailLayout
              && !!document.querySelector('#framework-master-detail-master')
              && !!document.querySelector('#framework-master-detail-detail')
              && !!document.querySelector('#framework-master-detail-server-update')
              && !!document.querySelector('#framework-master-detail-animation-update')
              && !!document.querySelector('#framework-master-detail-status')
              && !!card
              && !!document.querySelector('#framework-card-server-update')
              && !!document.querySelector('#framework-card-status')
              && !!markdown
              && !!document.querySelector('#framework-markdown-server-update')
              && !!document.querySelector('#framework-markdown-status')
              && !!badge
              && !!document.querySelector('#framework-badge-server-update')
              && !!document.querySelector('#framework-badge-status')
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
              && !!document.querySelector('#framework-popover-focus-field')
              && !!document.querySelector('#framework-popover-last-action')
              && !!document.querySelector('#framework-popover-lifecycle-status')
              && !!document.querySelector('#framework-popover-outside-target')
              && !!document.querySelector('#framework-mixed-overlay-dialog-open')
              && !!document.querySelector('#framework-mixed-overlay-dialog')
              && !!document.querySelector(
                '#framework-mixed-overlay-dialog-lifecycle-status')
              && !!document.querySelector(
                '#framework-mixed-overlay-popover-target')
              && !!document.querySelector('#framework-mixed-overlay-popover')
              && !!document.querySelector(
                '#framework-mixed-overlay-popover-focus-field')
              && !!document.querySelector(
                '#framework-mixed-overlay-popover-last-action')
              && !!document.querySelector(
                '#framework-mixed-overlay-popover-lifecycle-status')
              && !!document.querySelector(
                '#framework-mixed-overlay-outside-target')
              && !!document.querySelector(
                '#framework-nested-popover-outer-target')
              && !!document.querySelector('#framework-nested-popover-outer')
              && !!document.querySelector(
                '#framework-nested-popover-outer-last-action')
              && !!document.querySelector(
                '#framework-nested-popover-outer-lifecycle-status')
              && !!document.querySelector(
                '#framework-nested-popover-inner-target')
              && !!document.querySelector('#framework-nested-popover-inner')
              && !!document.querySelector(
                '#framework-nested-popover-inner-focus-field')
              && !!document.querySelector(
                '#framework-nested-popover-inner-last-action')
              && !!document.querySelector(
                '#framework-nested-popover-inner-lifecycle-status')
              && !!document.querySelector(
                '#framework-nested-popover-outside-target')
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
              && !!document.querySelector('#framework-lazy-frontend-open')
              && !!document.querySelector('#framework-nested-lazy-open')
              && !!document.querySelector('#framework-apply')
              && !error,
            runtimeReplacementStatus: document.querySelector('#framework-runtime-replacement-status')?.textContent || '',
            runtimeCaptured: document.querySelector('#framework-runtime-captured')?.textContent || '',
            runtimeStaleStatus: document.querySelector('#framework-runtime-stale-status')?.textContent || '',
            runtimeReplacementRequestResult: document.querySelector('#framework-runtime-replacement-request-result')?.textContent || '',
            lazyFrontendRouteObserved,
            lazyFrontendClientObserved,
            lazyFrontendServerObserved,
            lazyFrontendPath: window.location.pathname,
            lazyFrontendRouteRendered: !!lazyFrontendRoute,
            lazyFrontendProbeRendered: !!lazyFrontendProbe,
            lazyFrontendModuleDefined: !!customElements.get('vatori-lazy-probe'),
            lazyFrontendStatus,
            lazyFrontendText,
            nestedLazyLayoutObserved,
            nestedLazyRouteObserved,
            nestedLazyTransitiveModuleObserved,
            nestedLazyClientObserved,
            nestedLazyServerObserved,
            nestedLazyPath: window.location.pathname,
            nestedLazyLayoutRendered: !!nestedLazyLayout,
            nestedLazyRouteRendered: !!nestedLazyRoute,
            nestedLazyLayoutModuleDefined:
              !!customElements.get('vatori-lazy-layout-probe'),
            nestedLazyRouteModuleDefined:
              !!customElements.get('vatori-lazy-nested-probe'),
            nestedLazyLayoutStyleToken,
            nestedLazyRouteStyleToken,
            nestedLazyStatus,
            nestedLazyLayoutText,
            nestedLazyText,
            rootRendered: !!root,
            flowClientCount: clients.length,
            requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
            pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
            pushReconnectCount: Number(diagnostics.standardBundlePushReconnectCount || 0),
            pushResyncCount: Number(diagnostics.standardBundlePushResyncCount || 0),
            hiddenViewTransitionSkipped,
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
            dialogOpened,
            dialogInitialFocusObserved,
            dialogFocusTrapObserved,
            dialogEscapeCloseObserved,
            dialogFocusRestoredObserved,
            dialogA11yObserved,
            dialogLifecycleStatus,
            dialogOverlayRendered: !!dialogOverlay,
            dialogRole: String(dialogRoleElement?.getAttribute?.('role') || ''),
            dialogAriaModal,
            dialogAccessibleText,
            dialogActiveElementPath: activeDeepElements()
              .map((element) =>
                `${element.localName || element.nodeName || 'unknown'}#${element.id || ''}`)
              .join(' > '),
            dialogActiveBeforeTab:
              String(dialogKeyboardMarker.activeBeforeTab || ''),
            dialogActiveAfterTab:
              String(dialogKeyboardMarker.activeAfterTab || ''),
            dialogActiveAfterShiftTab:
              String(dialogKeyboardMarker.activeAfterShiftTab || ''),
            dialogTabDefaultPrevented:
              dialogKeyboardMarker.tabDefaultPrevented === true,
            dialogKeyboardError: String(dialogKeyboardMarker.error || ''),
            modalOverlayStackOpenedObserved,
            modalOverlayStackInitialFocusObserved,
            modalOverlayStackFocusTrapObserved,
            modalOverlayStackTopEscapeObserved,
            modalOverlayStackParentRemainsOpenObserved,
            modalOverlayStackFocusRestoredObserved,
            modalOverlayStackA11yObserved,
            modalOverlayStackNestedLifecycleStatus:
              nestedDialogLifecycleStatus,
            modalOverlayStackNestedRole: nestedDialogRole,
            modalOverlayStackNestedAriaModal: nestedDialogAriaModal,
            modalOverlayStackNestedAccessibleText:
              nestedDialogAccessibleText,
            modalOverlayStackActiveBeforeTab:
              String(nestedDialogKeyboardMarker.activeBeforeTab || ''),
            modalOverlayStackActiveAfterTab:
              String(nestedDialogKeyboardMarker.activeAfterTab || ''),
            modalOverlayStackActiveAfterShiftTab:
              String(nestedDialogKeyboardMarker.activeAfterShiftTab || ''),
            modalOverlayStackTabDefaultPrevented:
              nestedDialogKeyboardMarker.tabDefaultPrevented === true,
            modalOverlayStackKeyboardError:
              String(nestedDialogKeyboardMarker.error || ''),
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
            iconClientObserved,
            iconServerObserved,
            iconStatus,
            iconName,
            iconFill,
            iconWidth,
            iconHeight,
            iconAriaLabel,
            appLayoutClientObserved,
            appLayoutServerObserved,
            appLayoutStatus,
            appLayoutContent,
            appLayoutPrimarySection,
            appLayoutDrawerOpened,
            masterDetailClientObserved,
            masterDetailContentObserved,
            masterDetailServerObserved,
            masterDetailStatus,
            masterDetailText,
            masterDetailTheme,
            masterDetailMasterSize,
            masterDetailMasterMinSize,
            masterDetailDetailSize,
            masterDetailDetailMinSize,
            masterDetailOrientation,
            masterDetailContainment,
            masterDetailForceOverlay,
            masterDetailStackOverlay,
            masterDetailNoAnimation,
            masterDetailBackdropRendered,
            cardClientObserved,
            cardServerObserved,
            cardStatus,
            cardTheme,
            cardAriaRole,
            cardAriaLabel,
            cardTitle,
            cardSubtitle,
            cardMedia,
            cardHeaderPrefix,
            cardHeaderSuffix,
            cardFooter,
            cardText,
            markdownClientObserved,
            markdownServerObserved,
            markdownStatus,
            markdownContent,
            markdownText,
            badgeClientObserved,
            badgeServerObserved,
            badgeStatus,
            badgeTheme,
            badgeText,
            badgeNumber,
            badgeRole,
            badgeIconName,
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
            loginOverlaySubmitButtonReady: (() => {
              const overlay = document.querySelector('#framework-login-overlay-submit-button');
              if (!overlay?.opened) return false;
              return ['vaadin-text-field', 'vaadin-password-field', 'vaadin-button'].every(tag => {
                const control = findDeepFrom(overlay, element => element.localName === tag
                  && (tag !== 'vaadin-button' || (element.textContent || '').includes('Sign in')));
                return !!control && !control.disabled && control.getBoundingClientRect().height > 0;
              });
            })(),
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
            lazyGridFarViewportObserved,
            lazyGridSelectionObserved,
            lazyGridReturnViewportObserved,
            lazyGridSortObserved,
            lazyGridFilterObserved,
            lazyGridFilterResetObserved,
            lazyGridStatus,
            lazyGridSelectionStatus,
            lazyGridQueryStatus,
            lazyGridFilterValue,
            treeGridRootLoadedObserved,
            treeGridPlatformExpandedObserved,
            treeGridRuntimeExpandedObserved,
            treeGridSelectionObserved,
            treeGridRefreshObserved,
            treeGridCollapseObserved,
            treeGridQueryStatus,
            treeGridCountStatus,
            treeGridLifecycleStatus,
            treeGridSelectionStatus,
            treeGridRefreshStatus,
            treeGridText,
            componentRendererObserved,
            gridEditorOpenedObserved,
            gridEditorActiveFieldVisible,
            gridEditorSaveObserved,
            gridEditorCancelOpenedObserved,
            gridEditorCancelObserved,
            gridEditorValidationOpenedObserved,
            gridEditorValidationObserved,
            gridEditorActiveFieldInvalid,
            gridEditorValidationCancelObserved,
            editorRendererStatus,
            editorOpenStatus,
            editorSaveStatus,
            editorCancelStatus,
            editorValidationStatus,
            editorGridText,
            litRendererPropertiesObserved,
            litRendererFunctionObserved,
            litRendererRefreshObserved,
            litRendererStatus,
            clientCallableResolveObserved,
            clientCallableRejectObserved,
            pendingJavaScriptResolveObserved,
            pendingJavaScriptRejectObserved,
            clientCallableStatus,
            pendingJavaScriptSuccessStatus,
            pendingJavaScriptFailureStatus,
            flowDndConfiguredObserved,
            flowDndDragStartObserved,
            flowDndDropObserved,
            flowDndDragEndObserved,
            flowDndDragStartStatus,
            flowDndDropStatus,
            flowDndDragEndStatus,
            virtualListClientObserved,
            virtualListServerObserved,
            virtualListStatus,
            virtualListText,
            popoverOpened,
            popoverLifecycleStatus,
            anchoredModelessOverlayObserved,
            anchoredModelessOverlayA11yObserved,
            anchoredModelessOverlayTabOrderObserved,
            anchoredModelessOverlayEscapeCloseObserved,
            anchoredModelessOverlayEscapeFocusRestoredObserved,
            anchoredModelessOverlayOutsideCloseObserved,
            anchoredModelessOverlayOutsideFocusPreservedObserved,
            anchoredModelessOverlayRole:
              String(popover?.getAttribute?.('role') || ''),
            anchoredModelessOverlayAriaLabel:
              String(popover?.getAttribute?.('aria-label') || ''),
            anchoredModelessOverlayTargetAriaExpanded:
              String(popoverAriaTarget?.getAttribute?.('aria-expanded') || ''),
            anchoredModelessOverlayTargetAriaControls:
              String(popoverAriaTarget?.getAttribute?.('aria-controls') || ''),
            anchoredModelessOverlayTargetAriaHasPopup:
              String(popoverAriaTarget?.getAttribute?.('aria-haspopup') || ''),
            anchoredModelessOverlayActivePath: activeDeepElements()
              .map((element) =>
                `${element.localName || element.nodeName || 'unknown'}#${element.id || ''}`)
              .join(' > '),
            anchoredModelessOverlayKeyboardError:
              String(anchoredPopoverMarker.keyboardError || ''),
            anchoredModelessOverlayOutsideError:
              String(anchoredPopoverMarker.outsideError || ''),
            mixedOverlayStackDialogOpenedObserved,
            mixedOverlayPopoverOpened,
            mixedOverlayDialogLifecycleStatus,
            mixedOverlayPopoverLifecycleStatus,
            mixedOverlayStackOpenedObserved,
            mixedOverlayStackA11yObserved,
            mixedOverlayStackTabOrderObserved,
            mixedOverlayStackTopEscapeObserved,
            mixedOverlayStackParentRemainsOpenObserved,
            mixedOverlayStackPopoverFocusRestoredObserved,
            mixedOverlayStackOutsideCloseObserved,
            mixedOverlayStackOutsideFocusPreservedObserved,
            mixedOverlayStackParentEscapeObserved,
            mixedOverlayStackLayeredFocusRestoredObserved,
            mixedOverlayStackActivePath: activeDeepElements()
              .map((element) =>
                `${element.localName || element.nodeName || 'unknown'}#${element.id || ''}`)
              .join(' > '),
            mixedOverlayStackKeyboardError:
              String(mixedOverlayMarker.keyboardError || ''),
            mixedOverlayStackOutsideError:
              String(mixedOverlayMarker.outsideError || ''),
            mixedOverlayStackDialogError:
              String(mixedOverlayMarker.dialogError || ''),
            nestedPopoverOuterOpened,
            nestedPopoverInnerOpened,
            nestedPopoverOuterLifecycleStatus,
            nestedPopoverInnerLifecycleStatus,
            nestedAnchoredOverlayStackOuterOpenedObserved,
            nestedAnchoredOverlayStackOpenedObserved,
            nestedAnchoredOverlayStackA11yObserved,
            nestedAnchoredOverlayStackTabOrderObserved,
            nestedAnchoredOverlayStackTopEscapeObserved,
            nestedAnchoredOverlayStackParentRemainsOpenObserved,
            nestedAnchoredOverlayStackInnerFocusRestoredObserved,
            nestedAnchoredOverlayStackParentEscapeObserved,
            nestedAnchoredOverlayStackLayeredFocusRestoredObserved,
            nestedAnchoredOverlayStackOutsideTopCloseObserved,
            nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved,
            nestedAnchoredOverlayStackOutsideFocusPreservedObserved,
            nestedAnchoredOverlayStackOutsideParentCloseObserved,
            nestedAnchoredOverlayStackActivePath: activeDeepElements()
              .map((element) =>
                `${element.localName || element.nodeName || 'unknown'}#${element.id || ''}`)
              .join(' > '),
            nestedAnchoredOverlayStackKeyboardError:
              String(nestedPopoverMarker.keyboardError || ''),
            nestedAnchoredOverlayStackOutsideError:
              String(nestedPopoverMarker.outsideError || ''),
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
            sliderClientObserved,
            sliderServerObserved,
            sliderValue,
            sliderMin,
            sliderMax,
            sliderStep,
            sliderMinMaxVisible,
            sliderValueAlwaysVisible,
            sliderStatus,
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
            "pendingRpcOwners": state["pendingRpcOwners"] ?? [],
            "resyncReplay": state["resyncReplay"] ?? [:],
            "rpcRequests": state["rpcRequests"] ?? [],
            "resyncRpcRequests": state["resyncRpcRequests"] ?? [],
            "rpcObservedSends": state["rpcObservedSends"] ?? 0,
            "rpcRequestObserverInstalled": state["rpcRequestObserverInstalled"] as? Bool == true,
            "requestCount": standardFrameworkInteger(state, "requestCount"),
            "responseCount": standardFrameworkInteger(state, "responseCount"),
            "pushMessageCount": standardFrameworkInteger(state, "pushMessageCount"),
            "pushReconnectCount": standardFrameworkInteger(state, "pushReconnectCount"),
            "pushResyncCount": standardFrameworkInteger(state, "pushResyncCount"),
            "hiddenViewTransitionSkipped":
                state["hiddenViewTransitionSkipped"] as? Bool == true,
            "lazyFrontendRouteObserved":
                state["lazyFrontendRouteObserved"] as? Bool == true,
            "lazyFrontendClientObserved":
                state["lazyFrontendClientObserved"] as? Bool == true,
            "lazyFrontendServerObserved":
                state["lazyFrontendServerObserved"] as? Bool == true,
            "lazyFrontendPath": standardFrameworkString(state, "lazyFrontendPath"),
            "lazyFrontendRouteRendered":
                state["lazyFrontendRouteRendered"] as? Bool == true,
            "lazyFrontendProbeRendered":
                state["lazyFrontendProbeRendered"] as? Bool == true,
            "lazyFrontendModuleDefined":
                state["lazyFrontendModuleDefined"] as? Bool == true,
            "lazyFrontendStatus":
                standardFrameworkString(state, "lazyFrontendStatus"),
            "lazyFrontendText":
                standardFrameworkString(state, "lazyFrontendText"),
            "nestedLazyLayoutObserved":
                state["nestedLazyLayoutObserved"] as? Bool == true,
            "nestedLazyRouteObserved":
                state["nestedLazyRouteObserved"] as? Bool == true,
            "nestedLazyTransitiveModuleObserved":
                state["nestedLazyTransitiveModuleObserved"] as? Bool == true,
            "nestedLazyClientObserved":
                state["nestedLazyClientObserved"] as? Bool == true,
            "nestedLazyServerObserved":
                state["nestedLazyServerObserved"] as? Bool == true,
            "nestedLazyPath": standardFrameworkString(state, "nestedLazyPath"),
            "nestedLazyLayoutRendered":
                state["nestedLazyLayoutRendered"] as? Bool == true,
            "nestedLazyRouteRendered":
                state["nestedLazyRouteRendered"] as? Bool == true,
            "nestedLazyLayoutModuleDefined":
                state["nestedLazyLayoutModuleDefined"] as? Bool == true,
            "nestedLazyRouteModuleDefined":
                state["nestedLazyRouteModuleDefined"] as? Bool == true,
            "nestedLazyLayoutStyleToken":
                standardFrameworkString(state, "nestedLazyLayoutStyleToken"),
            "nestedLazyRouteStyleToken":
                standardFrameworkString(state, "nestedLazyRouteStyleToken"),
            "nestedLazyStatus":
                standardFrameworkString(state, "nestedLazyStatus"),
            "nestedLazyLayoutText":
                standardFrameworkString(state, "nestedLazyLayoutText"),
            "nestedLazyText":
                standardFrameworkString(state, "nestedLazyText"),
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
            "dialogOpened": state["dialogOpened"] as? Bool == true,
            "dialogInitialFocusObserved":
                state["dialogInitialFocusObserved"] as? Bool == true,
            "dialogFocusTrapObserved":
                state["dialogFocusTrapObserved"] as? Bool == true,
            "dialogEscapeCloseObserved":
                state["dialogEscapeCloseObserved"] as? Bool == true,
            "dialogFocusRestoredObserved":
                state["dialogFocusRestoredObserved"] as? Bool == true,
            "dialogA11yObserved":
                state["dialogA11yObserved"] as? Bool == true,
            "dialogLifecycleStatus":
                standardFrameworkString(state, "dialogLifecycleStatus"),
            "dialogOverlayRendered":
                state["dialogOverlayRendered"] as? Bool == true,
            "dialogRole":
                standardFrameworkString(state, "dialogRole"),
            "dialogAriaModal":
                standardFrameworkString(state, "dialogAriaModal"),
            "dialogAccessibleText":
                standardFrameworkString(state, "dialogAccessibleText"),
            "dialogActiveElementPath":
                standardFrameworkString(state, "dialogActiveElementPath"),
            "dialogActiveBeforeTab":
                standardFrameworkString(state, "dialogActiveBeforeTab"),
            "dialogActiveAfterTab":
                standardFrameworkString(state, "dialogActiveAfterTab"),
            "dialogActiveAfterShiftTab":
                standardFrameworkString(state, "dialogActiveAfterShiftTab"),
            "dialogTabDefaultPrevented":
                state["dialogTabDefaultPrevented"] as? Bool == true,
            "dialogKeyboardError":
                standardFrameworkString(state, "dialogKeyboardError"),
            "modalOverlayStackOpenedObserved":
                state["modalOverlayStackOpenedObserved"] as? Bool == true,
            "modalOverlayStackInitialFocusObserved":
                state["modalOverlayStackInitialFocusObserved"] as? Bool == true,
            "modalOverlayStackFocusTrapObserved":
                state["modalOverlayStackFocusTrapObserved"] as? Bool == true,
            "modalOverlayStackTopEscapeObserved":
                state["modalOverlayStackTopEscapeObserved"] as? Bool == true,
            "modalOverlayStackParentRemainsOpenObserved":
                state["modalOverlayStackParentRemainsOpenObserved"] as? Bool == true,
            "modalOverlayStackFocusRestoredObserved":
                state["modalOverlayStackFocusRestoredObserved"] as? Bool == true,
            "modalOverlayStackA11yObserved":
                state["modalOverlayStackA11yObserved"] as? Bool == true,
            "modalOverlayStackNestedLifecycleStatus":
                standardFrameworkString(state, "modalOverlayStackNestedLifecycleStatus"),
            "modalOverlayStackNestedRole":
                standardFrameworkString(state, "modalOverlayStackNestedRole"),
            "modalOverlayStackNestedAriaModal":
                standardFrameworkString(state, "modalOverlayStackNestedAriaModal"),
            "modalOverlayStackNestedAccessibleText":
                standardFrameworkString(state, "modalOverlayStackNestedAccessibleText"),
            "modalOverlayStackActiveBeforeTab":
                standardFrameworkString(state, "modalOverlayStackActiveBeforeTab"),
            "modalOverlayStackActiveAfterTab":
                standardFrameworkString(state, "modalOverlayStackActiveAfterTab"),
            "modalOverlayStackActiveAfterShiftTab":
                standardFrameworkString(state, "modalOverlayStackActiveAfterShiftTab"),
            "modalOverlayStackTabDefaultPrevented":
                state["modalOverlayStackTabDefaultPrevented"] as? Bool == true,
            "modalOverlayStackKeyboardError":
                standardFrameworkString(state, "modalOverlayStackKeyboardError"),
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
            "iconClientObserved": state["iconClientObserved"] as? Bool == true,
            "iconServerObserved": state["iconServerObserved"] as? Bool == true,
            "iconStatus": standardFrameworkString(state, "iconStatus"),
            "iconName": standardFrameworkString(state, "iconName"),
            "iconFill": standardFrameworkString(state, "iconFill"),
            "iconWidth": standardFrameworkString(state, "iconWidth"),
            "iconHeight": standardFrameworkString(state, "iconHeight"),
            "appLayoutClientObserved":
                state["appLayoutClientObserved"] as? Bool == true,
            "appLayoutServerObserved":
                state["appLayoutServerObserved"] as? Bool == true,
            "appLayoutStatus": standardFrameworkString(state, "appLayoutStatus"),
            "appLayoutContent": standardFrameworkString(state, "appLayoutContent"),
            "appLayoutPrimarySection":
                standardFrameworkString(state, "appLayoutPrimarySection"),
            "appLayoutDrawerOpened": state["appLayoutDrawerOpened"] as? Bool == true,
            "masterDetailClientObserved":
                state["masterDetailClientObserved"] as? Bool == true,
            "masterDetailContentObserved":
                state["masterDetailContentObserved"] as? Bool == true,
            "masterDetailServerObserved":
                state["masterDetailServerObserved"] as? Bool == true,
            "masterDetailStatus": standardFrameworkString(state, "masterDetailStatus"),
            "masterDetailText": standardFrameworkString(state, "masterDetailText"),
            "masterDetailTheme": standardFrameworkString(state, "masterDetailTheme"),
            "masterDetailMasterSize":
                standardFrameworkString(state, "masterDetailMasterSize"),
            "masterDetailMasterMinSize":
                standardFrameworkString(state, "masterDetailMasterMinSize"),
            "masterDetailDetailSize":
                standardFrameworkString(state, "masterDetailDetailSize"),
            "masterDetailDetailMinSize":
                standardFrameworkString(state, "masterDetailDetailMinSize"),
            "masterDetailOrientation":
                standardFrameworkString(state, "masterDetailOrientation"),
            "masterDetailContainment":
                standardFrameworkString(state, "masterDetailContainment"),
            "masterDetailForceOverlay":
                state["masterDetailForceOverlay"] as? Bool == true,
            "masterDetailStackOverlay":
                state["masterDetailStackOverlay"] as? Bool == true,
            "masterDetailNoAnimation":
                state["masterDetailNoAnimation"] as? Bool == true,
            "masterDetailBackdropRendered":
                state["masterDetailBackdropRendered"] as? Bool == true,
            "cardClientObserved": state["cardClientObserved"] as? Bool == true,
            "cardServerObserved": state["cardServerObserved"] as? Bool == true,
            "cardStatus": standardFrameworkString(state, "cardStatus"),
            "cardTheme": standardFrameworkString(state, "cardTheme"),
            "cardAriaRole": standardFrameworkString(state, "cardAriaRole"),
            "cardAriaLabel": standardFrameworkString(state, "cardAriaLabel"),
            "cardTitle": standardFrameworkString(state, "cardTitle"),
            "cardSubtitle": standardFrameworkString(state, "cardSubtitle"),
            "cardMedia": standardFrameworkString(state, "cardMedia"),
            "cardHeaderPrefix": standardFrameworkString(state, "cardHeaderPrefix"),
            "cardHeaderSuffix": standardFrameworkString(state, "cardHeaderSuffix"),
            "cardFooter": standardFrameworkString(state, "cardFooter"),
            "cardText": standardFrameworkString(state, "cardText"),
            "markdownClientObserved": state["markdownClientObserved"] as? Bool == true,
            "markdownServerObserved": state["markdownServerObserved"] as? Bool == true,
            "markdownStatus": standardFrameworkString(state, "markdownStatus"),
            "markdownContent": standardFrameworkString(state, "markdownContent"),
            "markdownText": standardFrameworkString(state, "markdownText"),
            "badgeClientObserved": state["badgeClientObserved"] as? Bool == true,
            "badgeServerObserved": state["badgeServerObserved"] as? Bool == true,
            "badgeStatus": standardFrameworkString(state, "badgeStatus"),
            "badgeTheme": standardFrameworkString(state, "badgeTheme"),
            "badgeText": standardFrameworkString(state, "badgeText"),
            "badgeNumber": standardFrameworkString(state, "badgeNumber"),
            "badgeRole": standardFrameworkString(state, "badgeRole"),
            "badgeIconName": standardFrameworkString(state, "badgeIconName"),
            "navigationMatrixStatus": standardFrameworkString(state, "navigationMatrixStatus"),
            "overlayMatrixStatus": standardFrameworkString(state, "overlayMatrixStatus"),
            "lazyGridFarViewportObserved":
                state["lazyGridFarViewportObserved"] as? Bool == true,
            "lazyGridSelectionObserved":
                state["lazyGridSelectionObserved"] as? Bool == true,
            "lazyGridReturnViewportObserved":
                state["lazyGridReturnViewportObserved"] as? Bool == true,
            "lazyGridSortObserved":
                state["lazyGridSortObserved"] as? Bool == true,
            "lazyGridFilterObserved":
                state["lazyGridFilterObserved"] as? Bool == true,
            "lazyGridFilterResetObserved":
                state["lazyGridFilterResetObserved"] as? Bool == true,
            "lazyGridStatus": standardFrameworkString(state, "lazyGridStatus"),
            "lazyGridSelectionStatus":
                standardFrameworkString(state, "lazyGridSelectionStatus"),
            "lazyGridQueryStatus":
                standardFrameworkString(state, "lazyGridQueryStatus"),
            "lazyGridFilterValue":
                standardFrameworkString(state, "lazyGridFilterValue"),
            "treeGridRootLoadedObserved":
                state["treeGridRootLoadedObserved"] as? Bool == true,
            "treeGridPlatformExpandedObserved":
                state["treeGridPlatformExpandedObserved"] as? Bool == true,
            "treeGridRuntimeExpandedObserved":
                state["treeGridRuntimeExpandedObserved"] as? Bool == true,
            "treeGridSelectionObserved":
                state["treeGridSelectionObserved"] as? Bool == true,
            "treeGridRefreshObserved":
                state["treeGridRefreshObserved"] as? Bool == true,
            "treeGridCollapseObserved":
                state["treeGridCollapseObserved"] as? Bool == true,
            "treeGridQueryStatus":
                standardFrameworkString(state, "treeGridQueryStatus"),
            "treeGridCountStatus":
                standardFrameworkString(state, "treeGridCountStatus"),
            "treeGridLifecycleStatus":
                standardFrameworkString(state, "treeGridLifecycleStatus"),
            "treeGridSelectionStatus":
                standardFrameworkString(state, "treeGridSelectionStatus"),
            "treeGridRefreshStatus":
                standardFrameworkString(state, "treeGridRefreshStatus"),
            "componentRendererObserved":
                state["componentRendererObserved"] as? Bool == true,
            "gridEditorOpenedObserved":
                state["gridEditorOpenedObserved"] as? Bool == true,
            "gridEditorSaveObserved":
                state["gridEditorSaveObserved"] as? Bool == true,
            "gridEditorCancelObserved":
                state["gridEditorCancelObserved"] as? Bool == true,
            "gridEditorValidationObserved":
                state["gridEditorValidationObserved"] as? Bool == true,
            "gridEditorValidationCancelObserved":
                state["gridEditorValidationCancelObserved"] as? Bool == true,
            "litRendererPropertiesObserved":
                state["litRendererPropertiesObserved"] as? Bool == true,
            "litRendererFunctionObserved":
                state["litRendererFunctionObserved"] as? Bool == true,
            "litRendererRefreshObserved":
                state["litRendererRefreshObserved"] as? Bool == true,
            "clientCallableResolveObserved":
                state["clientCallableResolveObserved"] as? Bool == true,
            "clientCallableRejectObserved":
                state["clientCallableRejectObserved"] as? Bool == true,
            "pendingJavaScriptResolveObserved":
                state["pendingJavaScriptResolveObserved"] as? Bool == true,
            "pendingJavaScriptRejectObserved":
                state["pendingJavaScriptRejectObserved"] as? Bool == true,
            "flowDndConfiguredObserved":
                state["flowDndConfiguredObserved"] as? Bool == true,
            "flowDndDragStartObserved":
                state["flowDndDragStartObserved"] as? Bool == true,
            "flowDndDropObserved":
                state["flowDndDropObserved"] as? Bool == true,
            "flowDndDragEndObserved":
                state["flowDndDragEndObserved"] as? Bool == true,
            "editorRendererStatus":
                standardFrameworkString(state, "editorRendererStatus"),
            "editorOpenStatus":
                standardFrameworkString(state, "editorOpenStatus"),
            "editorSaveStatus":
                standardFrameworkString(state, "editorSaveStatus"),
            "editorCancelStatus":
                standardFrameworkString(state, "editorCancelStatus"),
            "editorValidationStatus":
                standardFrameworkString(state, "editorValidationStatus"),
            "litRendererStatus":
                standardFrameworkString(state, "litRendererStatus"),
            "clientCallableStatus":
                standardFrameworkString(state, "clientCallableStatus"),
            "pendingJavaScriptSuccessStatus":
                standardFrameworkString(
                    state, "pendingJavaScriptSuccessStatus"),
            "pendingJavaScriptFailureStatus":
                standardFrameworkString(
                    state, "pendingJavaScriptFailureStatus"),
            "flowDndDragStartStatus":
                standardFrameworkString(state, "flowDndDragStartStatus"),
            "flowDndDropStatus":
                standardFrameworkString(state, "flowDndDropStatus"),
            "flowDndDragEndStatus":
                standardFrameworkString(state, "flowDndDragEndStatus"),
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
            payload["iconClientObserved"] =
                state["iconClientObserved"] as? Bool == true
            payload["iconServerObserved"] =
                state["iconServerObserved"] as? Bool == true
            payload["iconStatus"] =
                standardFrameworkString(state, "iconStatus")
            payload["iconName"] =
                standardFrameworkString(state, "iconName")
            payload["iconFill"] =
                standardFrameworkString(state, "iconFill")
            payload["iconWidth"] =
                standardFrameworkString(state, "iconWidth")
            payload["iconHeight"] =
                standardFrameworkString(state, "iconHeight")
            payload["iconAriaLabel"] =
                standardFrameworkString(state, "iconAriaLabel")
            payload["appLayoutClientObserved"] =
                state["appLayoutClientObserved"] as? Bool == true
            payload["appLayoutServerObserved"] =
                state["appLayoutServerObserved"] as? Bool == true
            payload["appLayoutStatus"] =
                standardFrameworkString(state, "appLayoutStatus")
            payload["appLayoutContent"] =
                standardFrameworkString(state, "appLayoutContent")
            payload["appLayoutPrimarySection"] =
                standardFrameworkString(state, "appLayoutPrimarySection")
            payload["appLayoutDrawerOpened"] =
                state["appLayoutDrawerOpened"] as? Bool == true
            payload["cardClientObserved"] =
                state["cardClientObserved"] as? Bool == true
            payload["cardServerObserved"] =
                state["cardServerObserved"] as? Bool == true
            payload["cardStatus"] =
                standardFrameworkString(state, "cardStatus")
            payload["cardTheme"] =
                standardFrameworkString(state, "cardTheme")
            payload["cardAriaRole"] =
                standardFrameworkString(state, "cardAriaRole")
            payload["cardAriaLabel"] =
                standardFrameworkString(state, "cardAriaLabel")
            payload["cardTitle"] =
                standardFrameworkString(state, "cardTitle")
            payload["cardSubtitle"] =
                standardFrameworkString(state, "cardSubtitle")
            payload["cardMedia"] =
                standardFrameworkString(state, "cardMedia")
            payload["cardHeaderPrefix"] =
                standardFrameworkString(state, "cardHeaderPrefix")
            payload["cardHeaderSuffix"] =
                standardFrameworkString(state, "cardHeaderSuffix")
            payload["cardFooter"] =
                standardFrameworkString(state, "cardFooter")
            payload["cardText"] =
                standardFrameworkString(state, "cardText")
            payload["markdownClientObserved"] =
                state["markdownClientObserved"] as? Bool == true
            payload["markdownServerObserved"] =
                state["markdownServerObserved"] as? Bool == true
            payload["markdownStatus"] =
                standardFrameworkString(state, "markdownStatus")
            payload["markdownContent"] =
                standardFrameworkString(state, "markdownContent")
            payload["markdownText"] =
                standardFrameworkString(state, "markdownText")
            payload["badgeClientObserved"] =
                state["badgeClientObserved"] as? Bool == true
            payload["badgeServerObserved"] =
                state["badgeServerObserved"] as? Bool == true
            payload["badgeStatus"] =
                standardFrameworkString(state, "badgeStatus")
            payload["badgeTheme"] =
                standardFrameworkString(state, "badgeTheme")
            payload["badgeText"] =
                standardFrameworkString(state, "badgeText")
            payload["badgeNumber"] =
                standardFrameworkString(state, "badgeNumber")
            payload["badgeRole"] =
                standardFrameworkString(state, "badgeRole")
            payload["badgeIconName"] =
                standardFrameworkString(state, "badgeIconName")
            payload["detailsOpened"] = state["detailsOpened"] as? Bool == true
            payload["lazyGridFarViewportObserved"] =
                state["lazyGridFarViewportObserved"] as? Bool == true
            payload["lazyGridSelectionObserved"] =
                state["lazyGridSelectionObserved"] as? Bool == true
            payload["lazyGridReturnViewportObserved"] =
                state["lazyGridReturnViewportObserved"] as? Bool == true
            payload["lazyGridSortObserved"] =
                state["lazyGridSortObserved"] as? Bool == true
            payload["lazyGridFilterObserved"] =
                state["lazyGridFilterObserved"] as? Bool == true
            payload["lazyGridFilterResetObserved"] =
                state["lazyGridFilterResetObserved"] as? Bool == true
            payload["lazyGridStatus"] = standardFrameworkString(state, "lazyGridStatus")
            payload["lazyGridSelectionStatus"] =
                standardFrameworkString(state, "lazyGridSelectionStatus")
            payload["lazyGridQueryStatus"] =
                standardFrameworkString(state, "lazyGridQueryStatus")
            payload["lazyGridFilterValue"] =
                standardFrameworkString(state, "lazyGridFilterValue")
            payload["treeGridRootLoadedObserved"] =
                state["treeGridRootLoadedObserved"] as? Bool == true
            payload["treeGridPlatformExpandedObserved"] =
                state["treeGridPlatformExpandedObserved"] as? Bool == true
            payload["treeGridRuntimeExpandedObserved"] =
                state["treeGridRuntimeExpandedObserved"] as? Bool == true
            payload["treeGridSelectionObserved"] =
                state["treeGridSelectionObserved"] as? Bool == true
            payload["treeGridRefreshObserved"] =
                state["treeGridRefreshObserved"] as? Bool == true
            payload["treeGridCollapseObserved"] =
                state["treeGridCollapseObserved"] as? Bool == true
            payload["treeGridQueryStatus"] =
                standardFrameworkString(state, "treeGridQueryStatus")
            payload["treeGridCountStatus"] =
                standardFrameworkString(state, "treeGridCountStatus")
            payload["treeGridLifecycleStatus"] =
                standardFrameworkString(state, "treeGridLifecycleStatus")
            payload["treeGridSelectionStatus"] =
                standardFrameworkString(state, "treeGridSelectionStatus")
            payload["treeGridRefreshStatus"] =
                standardFrameworkString(state, "treeGridRefreshStatus")
            payload["componentRendererObserved"] =
                state["componentRendererObserved"] as? Bool == true
            payload["gridEditorOpenedObserved"] =
                state["gridEditorOpenedObserved"] as? Bool == true
            payload["gridEditorSaveObserved"] =
                state["gridEditorSaveObserved"] as? Bool == true
            payload["gridEditorCancelObserved"] =
                state["gridEditorCancelObserved"] as? Bool == true
            payload["gridEditorValidationObserved"] =
                state["gridEditorValidationObserved"] as? Bool == true
            payload["gridEditorValidationCancelObserved"] =
                state["gridEditorValidationCancelObserved"] as? Bool == true
            payload["litRendererPropertiesObserved"] =
                state["litRendererPropertiesObserved"] as? Bool == true
            payload["litRendererFunctionObserved"] =
                state["litRendererFunctionObserved"] as? Bool == true
            payload["litRendererRefreshObserved"] =
                state["litRendererRefreshObserved"] as? Bool == true
            payload["clientCallableResolveObserved"] =
                state["clientCallableResolveObserved"] as? Bool == true
            payload["clientCallableRejectObserved"] =
                state["clientCallableRejectObserved"] as? Bool == true
            payload["pendingJavaScriptResolveObserved"] =
                state["pendingJavaScriptResolveObserved"] as? Bool == true
            payload["pendingJavaScriptRejectObserved"] =
                state["pendingJavaScriptRejectObserved"] as? Bool == true
            payload["flowDndConfiguredObserved"] =
                state["flowDndConfiguredObserved"] as? Bool == true
            payload["flowDndDragStartObserved"] =
                state["flowDndDragStartObserved"] as? Bool == true
            payload["flowDndDropObserved"] =
                state["flowDndDropObserved"] as? Bool == true
            payload["flowDndDragEndObserved"] =
                state["flowDndDragEndObserved"] as? Bool == true
            payload["editorRendererStatus"] =
                standardFrameworkString(state, "editorRendererStatus")
            payload["editorOpenStatus"] =
                standardFrameworkString(state, "editorOpenStatus")
            payload["editorSaveStatus"] =
                standardFrameworkString(state, "editorSaveStatus")
            payload["editorCancelStatus"] =
                standardFrameworkString(state, "editorCancelStatus")
            payload["editorValidationStatus"] =
                standardFrameworkString(state, "editorValidationStatus")
            payload["litRendererStatus"] =
                standardFrameworkString(state, "litRendererStatus")
            payload["clientCallableStatus"] =
                standardFrameworkString(state, "clientCallableStatus")
            payload["pendingJavaScriptSuccessStatus"] =
                standardFrameworkString(
                    state, "pendingJavaScriptSuccessStatus")
            payload["pendingJavaScriptFailureStatus"] =
                standardFrameworkString(
                    state, "pendingJavaScriptFailureStatus")
            payload["flowDndDragStartStatus"] =
                standardFrameworkString(state, "flowDndDragStartStatus")
            payload["flowDndDropStatus"] =
                standardFrameworkString(state, "flowDndDropStatus")
            payload["flowDndDragEndStatus"] =
                standardFrameworkString(state, "flowDndDragEndStatus")
            payload["virtualListClientObserved"] =
                state["virtualListClientObserved"] as? Bool == true
            payload["virtualListServerObserved"] =
                state["virtualListServerObserved"] as? Bool == true
            payload["virtualListStatus"] = standardFrameworkString(state, "virtualListStatus")
            payload["virtualListText"] = standardFrameworkString(state, "virtualListText")
            payload["popoverOpened"] = state["popoverOpened"] as? Bool == true
            payload["popoverLifecycleStatus"] =
                standardFrameworkString(state, "popoverLifecycleStatus")
            payload["anchoredModelessOverlayObserved"] =
                state["anchoredModelessOverlayObserved"] as? Bool == true
            payload["anchoredModelessOverlayA11yObserved"] =
                state["anchoredModelessOverlayA11yObserved"] as? Bool == true
            payload["anchoredModelessOverlayTabOrderObserved"] =
                state["anchoredModelessOverlayTabOrderObserved"] as? Bool == true
            payload["anchoredModelessOverlayEscapeCloseObserved"] =
                state["anchoredModelessOverlayEscapeCloseObserved"] as? Bool == true
            payload["anchoredModelessOverlayEscapeFocusRestoredObserved"] =
                state["anchoredModelessOverlayEscapeFocusRestoredObserved"] as? Bool == true
            payload["anchoredModelessOverlayOutsideCloseObserved"] =
                state["anchoredModelessOverlayOutsideCloseObserved"] as? Bool == true
            payload["anchoredModelessOverlayOutsideFocusPreservedObserved"] =
                state["anchoredModelessOverlayOutsideFocusPreservedObserved"] as? Bool == true
            payload["anchoredModelessOverlayRole"] =
                standardFrameworkString(state, "anchoredModelessOverlayRole")
            payload["anchoredModelessOverlayAriaLabel"] =
                standardFrameworkString(state, "anchoredModelessOverlayAriaLabel")
            payload["anchoredModelessOverlayTargetAriaExpanded"] =
                standardFrameworkString(state, "anchoredModelessOverlayTargetAriaExpanded")
            payload["anchoredModelessOverlayTargetAriaControls"] =
                standardFrameworkString(state, "anchoredModelessOverlayTargetAriaControls")
            payload["anchoredModelessOverlayTargetAriaHasPopup"] =
                standardFrameworkString(state, "anchoredModelessOverlayTargetAriaHasPopup")
            payload["anchoredModelessOverlayActivePath"] =
                standardFrameworkString(state, "anchoredModelessOverlayActivePath")
            payload["anchoredModelessOverlayKeyboardError"] =
                standardFrameworkString(state, "anchoredModelessOverlayKeyboardError")
            payload["anchoredModelessOverlayOutsideError"] =
                standardFrameworkString(state, "anchoredModelessOverlayOutsideError")
            payload["mixedOverlayStackDialogOpenedObserved"] =
                state["mixedOverlayStackDialogOpenedObserved"] as? Bool == true
            payload["mixedOverlayPopoverOpened"] =
                state["mixedOverlayPopoverOpened"] as? Bool == true
            payload["mixedOverlayDialogLifecycleStatus"] =
                standardFrameworkString(state, "mixedOverlayDialogLifecycleStatus")
            payload["mixedOverlayPopoverLifecycleStatus"] =
                standardFrameworkString(state, "mixedOverlayPopoverLifecycleStatus")
            payload["mixedOverlayStackOpenedObserved"] =
                state["mixedOverlayStackOpenedObserved"] as? Bool == true
            payload["mixedOverlayStackA11yObserved"] =
                state["mixedOverlayStackA11yObserved"] as? Bool == true
            payload["mixedOverlayStackTabOrderObserved"] =
                state["mixedOverlayStackTabOrderObserved"] as? Bool == true
            payload["mixedOverlayStackTopEscapeObserved"] =
                state["mixedOverlayStackTopEscapeObserved"] as? Bool == true
            payload["mixedOverlayStackParentRemainsOpenObserved"] =
                state["mixedOverlayStackParentRemainsOpenObserved"] as? Bool == true
            payload["mixedOverlayStackPopoverFocusRestoredObserved"] =
                state["mixedOverlayStackPopoverFocusRestoredObserved"] as? Bool == true
            payload["mixedOverlayStackOutsideCloseObserved"] =
                state["mixedOverlayStackOutsideCloseObserved"] as? Bool == true
            payload["mixedOverlayStackOutsideFocusPreservedObserved"] =
                state["mixedOverlayStackOutsideFocusPreservedObserved"] as? Bool == true
            payload["mixedOverlayStackParentEscapeObserved"] =
                state["mixedOverlayStackParentEscapeObserved"] as? Bool == true
            payload["mixedOverlayStackLayeredFocusRestoredObserved"] =
                state["mixedOverlayStackLayeredFocusRestoredObserved"] as? Bool == true
            payload["mixedOverlayStackActivePath"] =
                standardFrameworkString(state, "mixedOverlayStackActivePath")
            payload["mixedOverlayStackKeyboardError"] =
                standardFrameworkString(state, "mixedOverlayStackKeyboardError")
            payload["mixedOverlayStackOutsideError"] =
                standardFrameworkString(state, "mixedOverlayStackOutsideError")
            payload["mixedOverlayStackDialogError"] =
                standardFrameworkString(state, "mixedOverlayStackDialogError")
            payload["nestedPopoverOuterOpened"] =
                state["nestedPopoverOuterOpened"] as? Bool == true
            payload["nestedPopoverInnerOpened"] =
                state["nestedPopoverInnerOpened"] as? Bool == true
            payload["nestedPopoverOuterLifecycleStatus"] =
                standardFrameworkString(state, "nestedPopoverOuterLifecycleStatus")
            payload["nestedPopoverInnerLifecycleStatus"] =
                standardFrameworkString(state, "nestedPopoverInnerLifecycleStatus")
            payload["nestedAnchoredOverlayStackOuterOpenedObserved"] =
                state["nestedAnchoredOverlayStackOuterOpenedObserved"] as? Bool == true
            payload["nestedAnchoredOverlayStackOpenedObserved"] =
                state["nestedAnchoredOverlayStackOpenedObserved"] as? Bool == true
            payload["nestedAnchoredOverlayStackA11yObserved"] =
                state["nestedAnchoredOverlayStackA11yObserved"] as? Bool == true
            payload["nestedAnchoredOverlayStackTabOrderObserved"] =
                state["nestedAnchoredOverlayStackTabOrderObserved"] as? Bool == true
            payload["nestedAnchoredOverlayStackTopEscapeObserved"] =
                state["nestedAnchoredOverlayStackTopEscapeObserved"] as? Bool == true
            payload["nestedAnchoredOverlayStackParentRemainsOpenObserved"] =
                state[
                    "nestedAnchoredOverlayStackParentRemainsOpenObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackInnerFocusRestoredObserved"] =
                state[
                    "nestedAnchoredOverlayStackInnerFocusRestoredObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackParentEscapeObserved"] =
                state["nestedAnchoredOverlayStackParentEscapeObserved"] as? Bool == true
            payload["nestedAnchoredOverlayStackLayeredFocusRestoredObserved"] =
                state[
                    "nestedAnchoredOverlayStackLayeredFocusRestoredObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackOutsideTopCloseObserved"] =
                state[
                    "nestedAnchoredOverlayStackOutsideTopCloseObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved"] =
                state[
                    "nestedAnchoredOverlayStackOutsideParentRemainsOpenObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackOutsideFocusPreservedObserved"] =
                state[
                    "nestedAnchoredOverlayStackOutsideFocusPreservedObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackOutsideParentCloseObserved"] =
                state[
                    "nestedAnchoredOverlayStackOutsideParentCloseObserved"
                ] as? Bool == true
            payload["nestedAnchoredOverlayStackActivePath"] =
                standardFrameworkString(state, "nestedAnchoredOverlayStackActivePath")
            payload["nestedAnchoredOverlayStackKeyboardError"] =
                standardFrameworkString(
                    state, "nestedAnchoredOverlayStackKeyboardError")
            payload["nestedAnchoredOverlayStackOutsideError"] =
                standardFrameworkString(
                    state, "nestedAnchoredOverlayStackOutsideError")
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
            payload["sliderClientObserved"] =
                state["sliderClientObserved"] as? Bool == true
            payload["sliderServerObserved"] =
                state["sliderServerObserved"] as? Bool == true
            payload["sliderValue"] = standardFrameworkDouble(state, "sliderValue")
            payload["sliderMin"] = standardFrameworkDouble(state, "sliderMin")
            payload["sliderMax"] = standardFrameworkDouble(state, "sliderMax")
            payload["sliderStep"] = standardFrameworkDouble(state, "sliderStep")
            payload["sliderMinMaxVisible"] =
                state["sliderMinMaxVisible"] as? Bool == true
            payload["sliderValueAlwaysVisible"] =
                state["sliderValueAlwaysVisible"] as? Bool == true
            payload["sliderStatus"] = standardFrameworkString(state, "sliderStatus")
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

    private func standardFrameworkDouble(_ state: [String: Any], _ key: String) -> Double {
        (state[key] as? NSNumber)?.doubleValue ?? state[key] as? Double ?? 0
    }

    private func standardFrameworkJsonObject(_ value: Any?) -> [String: Any]? {
        guard let text = value as? String,
              let data = text.data(using: .utf8) else {
            return nil
        }
        return try? JSONSerialization.jsonObject(with: data) as? [String: Any]
    }

    private static let standardFrameworkLazyFrontendOpenAction = """
    (() => {
      const element = document.querySelector('#framework-lazy-frontend-open');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkLazyFrontendClientAction = """
    (() => {
      const element = document.querySelector('#framework-lazy-frontend-probe');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkLazyFrontendServerAction = """
    (() => {
      const element = document.querySelector('#framework-lazy-frontend-server-update');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkLazyFrontendReturnAction = """
    (() => {
      const element = document.querySelector('#framework-lazy-frontend-return');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedLazyOpenAction = """
    (() => {
      const element = document.querySelector('#framework-nested-lazy-open');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedLazyClientAction = """
    (() => {
      const element = document.querySelector('#framework-nested-lazy-probe');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedLazyServerAction = """
    (() => {
      const element = document.querySelector('#framework-nested-lazy-server-update');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedLazyReturnAction = """
    (() => {
      const element = document.querySelector('#framework-nested-lazy-return');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkAppLayoutClientAction = """
    (() => {
      const element = document.querySelector('#framework-app-layout-toggle');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkAppLayoutServerAction = """
    (() => {
      const element = document.querySelector('#framework-app-layout-server-update');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.click();
      return true;
    })()
    """

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

    private static let standardFrameworkSliderClientAction = """
    (() => {
      const element = document.querySelector('#framework-slider');
      if (!element) return false;
      const value = 6.5;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.value = value;
      element.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      element.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkSliderServerAction = """
    (() => {
      const button = document.querySelector('#framework-slider-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkCardClientAction = """
    (() => {
      const element = document.querySelector('#framework-card');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        composed: true,
        view: window
      }));
      return true;
    })()
    """

    private static let standardFrameworkMasterDetailClientAction = """
    (() => {
      const element = document.querySelector('#framework-master-detail-layout');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      if (typeof element.__onBackdropClick === 'function') {
        element.__onBackdropClick();
        return true;
      }
      const backdrop = element.shadowRoot?.querySelector?.('[part="backdrop"]');
      if (!backdrop) return false;
      backdrop.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        composed: true,
        view: window
      }));
      return true;
    })()
    """

    private static let standardFrameworkMasterDetailServerAction = """
    (() => {
      const button = document.querySelector('#framework-master-detail-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkMasterDetailAnimationAction = """
    (() => {
      const button = document.querySelector('#framework-master-detail-animation-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkCardServerAction = """
    (() => {
      const button = document.querySelector('#framework-card-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkMarkdownClientAction = """
    (() => {
      const element = document.querySelector('#framework-markdown');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        composed: true,
        view: window
      }));
      return true;
    })()
    """

    private static let standardFrameworkMarkdownServerAction = """
    (() => {
      const button = document.querySelector('#framework-markdown-server-update');
      if (!button) return false;
      button.scrollIntoView({ block: 'center', inline: 'center' });
      button.click();
      return true;
    })()
    """

    private static let standardFrameworkBadgeClientAction = """
    (() => {
      const element = document.querySelector('#framework-badge');
      if (!element) return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      element.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        composed: true,
        view: window
      }));
      return true;
    })()
    """

    private static let standardFrameworkBadgeServerAction = """
    (() => {
      const button = document.querySelector('#framework-badge-server-update');
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
      element.scrollIntoView({ block: 'center', inline: 'center' });
      if (typeof element.focus === 'function') {
        element.focus({ preventScroll: true });
      }
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedDialogAction = """
    (() => {
      const element = document.querySelector('#framework-nested-dialog-open');
      if (!element || typeof element.click !== 'function') return false;
      element.scrollIntoView({ block: 'center', inline: 'center' });
      if (typeof element.focus === 'function') {
        element.focus({ preventScroll: true });
      }
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedDialogKeyboardCloseAction = """
    (() => {
      const marker = {
        openedObserved: false,
        initialFocusObserved: false,
        a11yObserved: false,
        focusTrapObserved: false,
        parentOpenedBeforeEscape: false,
        escapeDispatched: false,
        tabDefaultPrevented: false,
        activeBeforeTab: '',
        activeAfterTab: '',
        activeAfterShiftTab: '',
        error: ''
      };
      window.VaadinDesktopFrameworkNestedDialogKeyboard = marker;
      const parentDialog = document.querySelector('#framework-dialog');
      const nestedDialog = document.querySelector('#framework-nested-dialog');
      const trigger = document.querySelector('#framework-nested-dialog-open');
      const focusField =
        document.querySelector('#framework-nested-dialog-focus-field');
      const lastAction =
        document.querySelector('#framework-nested-dialog-last-action');
      if (!parentDialog || !nestedDialog || !trigger || !focusField || !lastAction) {
        marker.error = 'nested-dialog-keyboard-elements-missing';
        return true;
      }
      const activeDeepElements = () => {
        const elements = [];
        let active = document.activeElement;
        while (active) {
          elements.push(active);
          active = active.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const activePath = () => activeDeepElements()
        .map((element) =>
          `${element.localName || element.nodeName || 'unknown'}#${element.id || ''}`)
        .join(' > ');
      const belongsToNestedDialog = (element) =>
        element === nestedDialog
          || element === focusField
          || element === focusField.inputElement
          || element === lastAction
          || nestedDialog.contains?.(element);
      const activeAtStart = activeDeepElements();
      marker.openedObserved =
        parentDialog.opened === true && nestedDialog.opened === true;
      marker.initialFocusObserved = activeAtStart.some((element) =>
        element === focusField
          || element === focusField.inputElement
          || focusField.contains?.(element));
      marker.a11yObserved =
        ['dialog', 'alertdialog'].includes(
          String(nestedDialog.getAttribute?.('role') || '').toLowerCase())
          && String(nestedDialog.getAttribute?.('aria-modal') || '') === 'true'
          && String(nestedDialog.getAttribute?.('aria-label') || '')
            .includes('Flow IPC nested keyboard dialog');
      marker.activeBeforeTab = activePath();
      try {
        const dispatchWhenFocusTrapReady = (remainingAttempts) => {
          try {
            const tab = new KeyboardEvent('keydown', {
              bubbles: true,
              composed: true,
              cancelable: true,
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9
            });
            document.dispatchEvent(tab);
            if (!tab.defaultPrevented && remainingAttempts > 0) {
              setTimeout(
                () => dispatchWhenFocusTrapReady(remainingAttempts - 1),
                50);
              return;
            }
            marker.activeAfterTab = activePath();
            const shiftTab = new KeyboardEvent('keydown', {
              bubbles: true,
              composed: true,
              cancelable: true,
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9,
              shiftKey: true
            });
            document.dispatchEvent(shiftTab);
            marker.activeAfterShiftTab = activePath();
            marker.tabDefaultPrevented =
              tab.defaultPrevented === true && shiftTab.defaultPrevented === true;
            const active = activeDeepElements();
            marker.focusTrapObserved =
              !active.includes(trigger)
                && active.some(belongsToNestedDialog);
            marker.parentOpenedBeforeEscape = parentDialog.opened === true;
            (activeDeepElements().at(-1) || document).dispatchEvent(
              new KeyboardEvent('keydown', {
              bubbles: true,
              composed: true,
              cancelable: true,
              key: 'Escape',
              code: 'Escape',
              keyCode: 27,
              which: 27
            }));
            marker.escapeDispatched = true;
          } catch (error) {
            marker.error =
              String(error?.message || error || 'nested-dialog-keyboard-timeout-error');
          }
        };
        dispatchWhenFocusTrapReady(0);
      } catch (error) {
        marker.error =
          String(error?.message || error || 'nested-dialog-keyboard-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkDialogKeyboardCloseAction = """
    (() => {
      const marker = {
        focusTrapObserved: false,
        escapeDispatched: false,
        tabDefaultPrevented: false,
        activeBeforeTab: '',
        activeAfterTab: '',
        activeAfterShiftTab: '',
        error: ''
      };
      window.VaadinDesktopFrameworkDialogKeyboard = marker;
      const dialog = document.querySelector('#framework-dialog');
      const trigger = document.querySelector('#framework-dialog-open');
      const focusField = document.querySelector('#framework-dialog-focus-field');
      const lastAction = document.querySelector('#framework-dialog-last-action');
      if (!dialog || !trigger || !focusField || !lastAction) {
        marker.error = 'dialog-keyboard-elements-missing';
        return true;
      }
      const activeDeepElements = () => {
        const elements = [];
        let active = document.activeElement;
        while (active) {
          elements.push(active);
          active = active.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const activePath = () => activeDeepElements()
        .map((element) =>
          `${element.localName || element.nodeName || 'unknown'}#${element.id || ''}`)
        .join(' > ');
      const belongsToDialog = (element) =>
        element === dialog
          || element === focusField
          || element === focusField.inputElement
          || element === lastAction
          || dialog.contains?.(element);
      marker.activeBeforeTab = activePath();
      try {
        const dispatchWhenFocusTrapReady = (remainingAttempts) => {
          try {
            const tab = new KeyboardEvent('keydown', {
              bubbles: true,
              composed: true,
              cancelable: true,
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9
            });
            document.dispatchEvent(tab);
            if (!tab.defaultPrevented && remainingAttempts > 0) {
              setTimeout(
                () => dispatchWhenFocusTrapReady(remainingAttempts - 1),
                50);
              return;
            }
            marker.activeAfterTab = activePath();
            const shiftTab = new KeyboardEvent('keydown', {
              bubbles: true,
              composed: true,
              cancelable: true,
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9,
              shiftKey: true
            });
            document.dispatchEvent(shiftTab);
            marker.activeAfterShiftTab = activePath();
            marker.tabDefaultPrevented =
              tab.defaultPrevented === true && shiftTab.defaultPrevented === true;
            const active = activeDeepElements();
            marker.focusTrapObserved =
              !active.includes(trigger)
                && active.some(belongsToDialog);
            (activeDeepElements().at(-1) || document).dispatchEvent(
              new KeyboardEvent('keydown', {
              bubbles: true,
              composed: true,
              cancelable: true,
              key: 'Escape',
              code: 'Escape',
              keyCode: 27,
              which: 27
            }));
            marker.escapeDispatched = true;
          } catch (error) {
            marker.error = String(error?.message || error || 'dialog-keyboard-timeout-error');
          }
        };
        dispatchWhenFocusTrapReady(0);
      } catch (error) {
        marker.error = String(error?.message || error || 'dialog-keyboard-error');
      }
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

    private static let standardFrameworkIconClientAction = """
    (() => {
      const icon = document.querySelector('#framework-icon');
      if (!icon || typeof icon.click !== 'function') return false;
      icon.scrollIntoView({ block: 'center', inline: 'center' });
      icon.click();
      return true;
    })()
    """

    private static let standardFrameworkIconServerAction = """
    (() => {
      const button = document.querySelector('#framework-icon-server-update');
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
      const status = document.querySelector('#framework-login-overlay-submit-button-status');
      if (!status) return false;
      status.dispatchEvent(new Event('vatori-login-overlay-submit-button-open',
        { bubbles: true, composed: true }));
      return true;
    })()
    """

    private static let standardFrameworkLoginVariantsSubmitAction =
        standardFrameworkLoginOverlayActionPostSubmitAction
            .replacingOccurrences(of: "#framework-login-overlay-action-post",
                                  with: "#framework-login-overlay-submit-button")
            .replacingOccurrences(of: "overlay-action-user", with: "overlay-button-user")

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

    private static let standardFrameworkLazyGridFarViewportAction = """
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

    private static let standardFrameworkLazyGridSelectionAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-cell-content'
            && (node.textContent || '').trim() === 'L-061') {
          return node;
        }
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
      const cell = visit(grid);
      if (!cell || typeof cell.click !== 'function') return false;
      cell.click();
      return true;
    })()
    """

    private static let standardFrameworkLazyGridReturnViewportAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid || typeof grid.scrollToIndex !== 'function') return false;
      grid.scrollToIndex(0);
      return true;
    })()
    """

    private static let standardFrameworkLazyGridSortAction = """
    (() => {
      const grid = document.querySelector('#framework-lazy-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-sorter'
            && (node.textContent || '').includes('Lazy owner')) {
          return node;
        }
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
      const sorter = visit(grid);
      if (!sorter || typeof sorter.click !== 'function') return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      sorter.click();
      return true;
    })()
    """

    private static let standardFrameworkLazyGridFilterAction = """
    (() => {
      const field = document.querySelector('#framework-lazy-grid-filter');
      const grid = document.querySelector('#framework-lazy-grid');
      if (!field || !grid) return false;
      const value = 'platform';
      const nativeInput = field.inputElement;
      field.scrollIntoView({ block: 'center', inline: 'center' });
      field.value = value;
      if (nativeInput) {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(nativeInput, value);
        } else {
          nativeInput.value = value;
        }
        nativeInput.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        nativeInput.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      }
      field.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      field.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      field.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      setTimeout(() => grid.scrollToIndex?.(0), 500);
      return true;
    })()
    """

    private static let standardFrameworkLazyGridFilterResetAction = """
    (() => {
      const field = document.querySelector('#framework-lazy-grid-filter');
      const grid = document.querySelector('#framework-lazy-grid');
      if (!field || !grid) return false;
      const value = '';
      const nativeInput = field.inputElement;
      field.scrollIntoView({ block: 'center', inline: 'center' });
      field.value = value;
      if (nativeInput) {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(nativeInput, value);
        } else {
          nativeInput.value = value;
        }
        nativeInput.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
        nativeInput.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      }
      field.dispatchEvent(new Event('input', { bubbles: true, composed: true }));
      field.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      field.dispatchEvent(new Event('change', { bubbles: true, composed: true }));
      setTimeout(() => grid.scrollToIndex?.(0), 500);
      return true;
    })()
    """

    private static let standardFrameworkTreeGridPlatformExpandAction = """
    (() => {
      const grid = document.querySelector('#framework-tree-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-tree-toggle'
            && (node.textContent || '').includes('Platform')) {
          return node;
        }
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
      const toggle = visit(grid);
      if (!toggle) return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      if (toggle.expanded !== true) {
        const target =
          toggle.shadowRoot?.querySelector('[part="toggle"]') || toggle;
        target.click();
      }
      return true;
    })()
    """

    private static let standardFrameworkTreeGridRuntimeExpandAction = """
    (() => {
      const grid = document.querySelector('#framework-tree-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-tree-toggle'
            && (node.textContent || '').includes('Runtime')) {
          return node;
        }
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
      const toggle = visit(grid);
      if (!toggle) return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      if (toggle.expanded !== true) {
        const target =
          toggle.shadowRoot?.querySelector('[part="toggle"]') || toggle;
        target.click();
      }
      return true;
    })()
    """

    private static let standardFrameworkTreeGridSelectionAction = """
    (() => {
      const grid = document.querySelector('#framework-tree-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-cell-content'
            && (node.textContent || '').trim() === 'Opaque IPC') {
          return node;
        }
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
      const cell = visit(grid);
      if (!cell || typeof cell.click !== 'function') return false;
      cell.click();
      return true;
    })()
    """

    private static let standardFrameworkTreeGridRefreshAction = """
    (() => {
      const action = document.querySelector('#framework-tree-grid-refresh');
      if (!action || typeof action.click !== 'function') return false;
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkTreeGridCollapseAction = """
    (() => {
      const grid = document.querySelector('#framework-tree-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'vaadin-grid-tree-toggle'
            && (node.textContent || '').includes('Platform')) {
          return node;
        }
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
      const toggle = visit(grid);
      if (!toggle || toggle.expanded !== true) return false;
      const target =
        toggle.shadowRoot?.querySelector('[part="toggle"]') || toggle;
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkGridEditorOpenAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const action = visit(grid, 'framework-editor-open-E-100');
      if (!action || typeof action.click !== 'function') return false;
      grid.scrollIntoView({ block: 'center', inline: 'center' });
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkGridEditorSaveAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const field = visit(grid, 'framework-editor-owner-field');
      const save = visit(grid, 'framework-editor-save');
      if (!field || !save || typeof save.click !== 'function') return false;
      const value = 'desktop-updated';
      const nativeInput = field.inputElement;
      field.value = value;
      if (nativeInput) {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(nativeInput, value);
        } else {
          nativeInput.value = value;
        }
        nativeInput.dispatchEvent(
          new Event('input', { bubbles: true, composed: true }));
        nativeInput.dispatchEvent(
          new Event('change', { bubbles: true, composed: true }));
      }
      field.dispatchEvent(
        new Event('input', { bubbles: true, composed: true }));
      field.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      field.dispatchEvent(
        new Event('change', { bubbles: true, composed: true }));
      setTimeout(() => save.click(), 120);
      return true;
    })()
    """

    private static let standardFrameworkGridEditorCancelOpenAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const action = visit(grid, 'framework-editor-open-E-101');
      if (!action || typeof action.click !== 'function') return false;
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkGridEditorCancelAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const field = visit(grid, 'framework-editor-owner-field');
      const cancel = visit(grid, 'framework-editor-cancel');
      if (!field || !cancel || typeof cancel.click !== 'function') return false;
      const value = 'should-not-stick';
      const nativeInput = field.inputElement;
      field.value = value;
      if (nativeInput) {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(nativeInput, value);
        } else {
          nativeInput.value = value;
        }
        nativeInput.dispatchEvent(
          new Event('input', { bubbles: true, composed: true }));
        nativeInput.dispatchEvent(
          new Event('change', { bubbles: true, composed: true }));
      }
      field.dispatchEvent(
        new Event('input', { bubbles: true, composed: true }));
      field.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      field.dispatchEvent(
        new Event('change', { bubbles: true, composed: true }));
      setTimeout(() => cancel.click(), 120);
      return true;
    })()
    """

    private static let standardFrameworkGridEditorValidationOpenAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const action = visit(grid, 'framework-editor-open-E-102');
      if (!action || typeof action.click !== 'function') return false;
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkGridEditorValidationAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const field = visit(grid, 'framework-editor-owner-field');
      const save = visit(grid, 'framework-editor-save');
      if (!field || !save || typeof save.click !== 'function') return false;
      const value = '';
      const nativeInput = field.inputElement;
      field.value = value;
      if (nativeInput) {
        const descriptor = Object.getOwnPropertyDescriptor(
          HTMLInputElement.prototype, 'value');
        if (descriptor?.set) {
          descriptor.set.call(nativeInput, value);
        } else {
          nativeInput.value = value;
        }
        nativeInput.dispatchEvent(
          new Event('input', { bubbles: true, composed: true }));
        nativeInput.dispatchEvent(
          new Event('change', { bubbles: true, composed: true }));
      }
      field.dispatchEvent(
        new Event('input', { bubbles: true, composed: true }));
      field.dispatchEvent(new CustomEvent('value-changed', {
        bubbles: true,
        composed: true,
        detail: { value }
      }));
      field.dispatchEvent(
        new Event('change', { bubbles: true, composed: true }));
      setTimeout(() => save.click(), 120);
      return true;
    })()
    """

    private static let standardFrameworkGridEditorValidationCancelAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node, id) => {
        if (!node) return null;
        if (node.nodeType === 1 && node.id === id) return node;
        if (node.shadowRoot) {
          const shadowResult = visit(node.shadowRoot, id);
          if (shadowResult) return shadowResult;
        }
        const children = node.children || node.childNodes || [];
        for (const child of children) {
          const result = visit(child, id);
          if (result) return result;
        }
        return null;
      };
      const cancel = visit(grid, 'framework-editor-cancel');
      if (!cancel || typeof cancel.click !== 'function') return false;
      cancel.click();
      return true;
    })()
    """

    private static let standardFrameworkLitRendererFunctionAction = """
    (() => {
      const grid = document.querySelector('#framework-editor-grid');
      if (!grid) return false;
      const visit = (node) => {
        if (!node) return null;
        if (node.nodeType === 1
            && node.localName === 'button'
            && (node.textContent || '').includes('Lit E-100: queued')) {
          return node;
        }
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
      const action = visit(grid);
      if (!action || typeof action.click !== 'function') return false;
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkClientCallableResolveAction = """
    (() => {
      const probe = document.querySelector('#framework-flow-rpc-probe');
      if (!probe || typeof probe.$server?.resolveClientCall !== 'function') {
        return false;
      }
      delete probe.dataset.clientCallableResolved;
      delete probe.dataset.clientCallableResolveError;
      const result = probe.$server.resolveClientCall('desktop-rpc');
      if (!result || typeof result.then !== 'function') return false;
      result.then(
        (value) => {
          probe.dataset.clientCallableResolved = String(value);
        },
        (error) => {
          probe.dataset.clientCallableResolveError = String(error);
        });
      return true;
    })()
    """

    private static let standardFrameworkClientCallableRejectAction = """
    (() => {
      const probe = document.querySelector('#framework-flow-rpc-probe');
      if (!probe || typeof probe.$server?.rejectClientCall !== 'function') {
        return false;
      }
      delete probe.dataset.clientCallableRejected;
      delete probe.dataset.clientCallableRejectError;
      delete probe.dataset.clientCallableUnexpectedValue;
      const result = probe.$server.rejectClientCall('desktop-rpc-error');
      if (!result || typeof result.then !== 'function') return false;
      result.then(
        (value) => {
          probe.dataset.clientCallableUnexpectedValue = String(value);
        },
        (error) => {
          probe.dataset.clientCallableRejected = 'true';
          probe.dataset.clientCallableRejectError = String(error);
        });
      return true;
    })()
    """

    private static let standardFrameworkPendingJavaScriptResolveAction = """
    (() => {
      const action = document.querySelector(
        '#framework-pending-javascript-success');
      if (!action || typeof action.click !== 'function') return false;
      action.click();
      return true;
    })()
    """

    private static let standardFrameworkPendingJavaScriptRejectAction = """
    (() => {
      const action = document.querySelector(
        '#framework-pending-javascript-failure');
      if (!action || typeof action.click !== 'function') return false;
      action.click();
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
      window.VaadinDesktopFrameworkAnchoredPopover = {};
      element.scrollIntoView({ block: 'center', inline: 'center' });
      if (typeof element.focus === 'function') {
        element.focus({ preventScroll: true });
      }
      element.click();
      return true;
    })()
    """

    private static let standardFrameworkPopoverKeyboardCloseAction = """
    (() => {
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const deepActiveElement = () => activeDeepElements().at(-1) || null;
      const marker = window.VaadinDesktopFrameworkAnchoredPopover || {};
      window.VaadinDesktopFrameworkAnchoredPopover = marker;
      try {
        const popover = document.querySelector('#framework-popover');
        const target = document.querySelector('#framework-popover-target');
        const focusField =
          document.querySelector('#framework-popover-focus-field');
        const overlay =
          popover?.shadowRoot?.querySelector('vaadin-popover-overlay');
        const ariaTarget = target?.ariaTarget || target;
        if (!popover || !target || !focusField || !overlay || !ariaTarget) {
          marker.keyboardError = 'required-popover-elements-missing';
          return true;
        }
        marker.anchoredObserved =
          popover.opened === true
            && popover.target === target
            && overlay.positionTarget === target
            && overlay.modeless === true
            && overlay.focusTrap !== true;
        marker.a11yObserved =
          String(popover.getAttribute('role') || '').toLowerCase() === 'dialog'
            && String(popover.getAttribute('aria-label') || '')
              .includes('Flow IPC anchored modeless popover')
            && ariaTarget.getAttribute('aria-expanded') === 'true'
            && ariaTarget.getAttribute('aria-controls') === popover.id
            && ariaTarget.getAttribute('aria-haspopup') === 'dialog';

        target.focus({ preventScroll: true });
        const tabEvent = new KeyboardEvent('keydown', {
          key: 'Tab',
          code: 'Tab',
          bubbles: true,
          composed: true,
          cancelable: true
        });
        target.dispatchEvent(tabEvent);
        const tabActive = deepActiveElement();
        marker.tabForwardObserved =
          tabEvent.defaultPrevented
            && (tabActive === popover || popover.contains(tabActive));

        const shiftTabEvent = new KeyboardEvent('keydown', {
          key: 'Tab',
          code: 'Tab',
          shiftKey: true,
          bubbles: true,
          composed: true,
          cancelable: true
        });
        (deepActiveElement() || popover).dispatchEvent(shiftTabEvent);
        marker.tabBackwardObserved =
          shiftTabEvent.defaultPrevented
            && activeDeepElements().includes(target);

        focusField.focus({ preventScroll: true });
        const escapeTarget = deepActiveElement() || focusField;
        marker.focusInsideBeforeEscape =
          escapeTarget === focusField
            || escapeTarget === focusField.inputElement
            || focusField.contains(escapeTarget);
        const escapeEvent = new KeyboardEvent('keydown', {
          key: 'Escape',
          code: 'Escape',
          bubbles: true,
          composed: true,
          cancelable: true
        });
        escapeTarget.dispatchEvent(escapeEvent);
        marker.escapeDispatched = true;
        marker.escapeClosedSynchronously = popover.opened !== true;
      } catch (error) {
        marker.keyboardError = String(error);
      }
      return true;
    })()
    """

    private static let standardFrameworkPopoverReopenAction = """
    (() => {
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const popover = document.querySelector('#framework-popover');
      const target = document.querySelector('#framework-popover-target');
      const lifecycle =
        document.querySelector('#framework-popover-lifecycle-status')
          ?.textContent || '';
      const marker = window.VaadinDesktopFrameworkAnchoredPopover || {};
      window.VaadinDesktopFrameworkAnchoredPopover = marker;
      if (!popover || !target || typeof target.click !== 'function') return false;
      marker.escapeCloseObserved =
        popover.opened !== true
          && lifecycle.includes('Popover lifecycle closed: client=true');
      marker.escapeFocusRestoredObserved =
        activeDeepElements().includes(target);
      target.focus({ preventScroll: true });
      target.click();
      marker.reopenDispatched = true;
      return true;
    })()
    """

    private static let standardFrameworkPopoverOutsideCloseAction = """
    (() => {
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const marker = window.VaadinDesktopFrameworkAnchoredPopover || {};
      window.VaadinDesktopFrameworkAnchoredPopover = marker;
      try {
        const popover = document.querySelector('#framework-popover');
        const outside =
          document.querySelector('#framework-popover-outside-target');
        if (!popover || !outside || popover.opened !== true) {
          marker.outsideError = 'open-popover-or-outside-target-missing';
          return true;
        }
        outside.focus({ preventScroll: true });
        marker.outsideFocusBeforeClick =
          activeDeepElements().includes(outside);
        for (const type of ['mousedown', 'mouseup', 'click']) {
          outside.dispatchEvent(new MouseEvent(type, {
            bubbles: true,
            composed: true,
            cancelable: true,
            button: 0
          }));
        }
        marker.outsideDispatched = true;
        marker.outsideClosedSynchronously = popover.opened !== true;
        marker.outsideFocusPreservedObserved =
          activeDeepElements().includes(outside);
      } catch (error) {
        marker.outsideError = String(error);
      }
      return true;
    })()
    """

    private static let standardFrameworkMixedOverlayDialogOpenAction = """
    (() => {
      const trigger =
        document.querySelector('#framework-mixed-overlay-dialog-open');
      if (!trigger || typeof trigger.click !== 'function') return false;
      window.VaadinDesktopFrameworkMixedOverlay = {
        openedObserved: false,
        a11yObserved: false,
        tabForwardObserved: false,
        tabBackwardObserved: false,
        topEscapeDispatched: false,
        parentRemainsAfterTopEscape: false,
        popoverFocusRestoredObserved: false,
        outsideDispatched: false,
        parentRemainsAfterOutside: false,
        outsideFocusPreservedObserved: false,
        parentEscapeDispatched: false,
        layeredFocusRestoredObserved: false,
        keyboardError: '',
        outsideError: '',
        dialogError: ''
      };
      trigger.scrollIntoView({ block: 'center', inline: 'center' });
      trigger.focus({ preventScroll: true });
      trigger.click();
      return true;
    })()
    """

    private static let standardFrameworkMixedOverlayPopoverOpenAction = """
    (() => {
      const dialog =
        document.querySelector('#framework-mixed-overlay-dialog');
      const target =
        document.querySelector('#framework-mixed-overlay-popover-target');
      if (!dialog || dialog.opened !== true
          || !target || typeof target.click !== 'function') return false;
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkMixedOverlayPopoverKeyboardCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkMixedOverlay || {};
      window.VaadinDesktopFrameworkMixedOverlay = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const deepestActiveElement = () =>
        activeDeepElements().at(-1) || document;
      try {
        const dialog =
          document.querySelector('#framework-mixed-overlay-dialog');
        const popover =
          document.querySelector('#framework-mixed-overlay-popover');
        const target =
          document.querySelector('#framework-mixed-overlay-popover-target');
        const focusField =
          document.querySelector(
            '#framework-mixed-overlay-popover-focus-field');
        const lastAction =
          document.querySelector(
            '#framework-mixed-overlay-popover-last-action');
        const overlay =
          popover?.shadowRoot?.querySelector('vaadin-popover-overlay');
        const ariaTarget = target?.ariaTarget || target;
        if (!dialog || !popover || !target || !focusField || !lastAction
            || !overlay || !ariaTarget) {
          marker.keyboardError = 'mixed-overlay-elements-missing';
          return true;
        }
        marker.openedObserved =
          dialog.opened === true
            && popover.opened === true
            && popover.target === target
            && overlay.positionTarget === target
            && overlay.modeless === true
            && overlay.focusTrap !== true;
        marker.a11yObserved =
          marker.openedObserved === true
            && String(popover.getAttribute('role') || '').toLowerCase()
              === 'dialog'
            && String(popover.getAttribute('aria-label') || '')
              .includes('Flow IPC mixed modeless popover')
            && ariaTarget.getAttribute('aria-expanded') === 'true'
            && ariaTarget.getAttribute('aria-controls') === popover.id
            && ariaTarget.getAttribute('aria-haspopup') === 'dialog';

        const dispatchWhenFocusTrapReady = (remainingAttempts) => {
          try {
            target.focus({ preventScroll: true });
            const tabEvent = new KeyboardEvent('keydown', {
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9,
              bubbles: true,
              composed: true,
              cancelable: true
            });
            target.dispatchEvent(tabEvent);
            if (!tabEvent.defaultPrevented && remainingAttempts > 0) {
              setTimeout(
                () => dispatchWhenFocusTrapReady(remainingAttempts - 1),
                50);
              return;
            }
            const tabActive = activeDeepElements();
            marker.tabForwardObserved =
              tabEvent.defaultPrevented === true
                && tabActive.some((element) =>
                  element === popover
                    || element === focusField
                    || element === focusField.inputElement
                    || element === lastAction
                    || popover.contains?.(element));

            const shiftTabEvent = new KeyboardEvent('keydown', {
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9,
              shiftKey: true,
              bubbles: true,
              composed: true,
              cancelable: true
            });
            deepestActiveElement().dispatchEvent(shiftTabEvent);
            marker.tabBackwardObserved =
              shiftTabEvent.defaultPrevented === true
                && activeDeepElements().includes(target);

            focusField.focus({ preventScroll: true });
            deepestActiveElement().dispatchEvent(new KeyboardEvent('keydown', {
              key: 'Escape',
              code: 'Escape',
              keyCode: 27,
              which: 27,
              bubbles: true,
              composed: true,
              cancelable: true
            }));
            marker.topEscapeDispatched = true;
            const captureCloseState = () => {
              marker.parentRemainsAfterTopEscape =
                dialog.opened === true && popover.opened !== true;
              marker.popoverFocusRestoredObserved =
                popover.opened !== true
                  && activeDeepElements().includes(target);
            };
            captureCloseState();
            setTimeout(captureCloseState, 50);
          } catch (error) {
            marker.keyboardError =
              String(error?.message || error || 'mixed-overlay-keyboard-timeout-error');
          }
        };
        dispatchWhenFocusTrapReady(20);
      } catch (error) {
        marker.keyboardError =
          String(error?.message || error || 'mixed-overlay-keyboard-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkMixedOverlayPopoverReopenAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkMixedOverlay || {};
      window.VaadinDesktopFrameworkMixedOverlay = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const dialog =
        document.querySelector('#framework-mixed-overlay-dialog');
      const popover =
        document.querySelector('#framework-mixed-overlay-popover');
      const target =
        document.querySelector('#framework-mixed-overlay-popover-target');
      if (!dialog || dialog.opened !== true || !popover || popover.opened === true
          || !target || typeof target.click !== 'function') return false;
      marker.parentRemainsAfterTopEscape =
        dialog.opened === true && popover.opened !== true;
      marker.popoverFocusRestoredObserved =
        activeDeepElements().includes(target);
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkMixedOverlayPopoverOutsideCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkMixedOverlay || {};
      window.VaadinDesktopFrameworkMixedOverlay = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      try {
        const dialog =
          document.querySelector('#framework-mixed-overlay-dialog');
        const popover =
          document.querySelector('#framework-mixed-overlay-popover');
        const outside =
          document.querySelector('#framework-mixed-overlay-outside-target');
        if (!dialog || dialog.opened !== true
            || !popover || popover.opened !== true || !outside) {
          marker.outsideError =
            'mixed-overlay-open-popover-or-outside-target-missing';
          return true;
        }
        outside.focus({ preventScroll: true });
        for (const type of ['mousedown', 'mouseup', 'click']) {
          outside.dispatchEvent(new MouseEvent(type, {
            bubbles: true,
            composed: true,
            cancelable: true,
            button: 0
          }));
        }
        marker.outsideDispatched = true;
        const captureCloseState = () => {
          marker.parentRemainsAfterOutside =
            dialog.opened === true && popover.opened !== true;
          marker.outsideFocusPreservedObserved =
            popover.opened !== true
              && activeDeepElements().includes(outside);
        };
        captureCloseState();
        setTimeout(captureCloseState, 50);
      } catch (error) {
        marker.outsideError =
          String(error?.message || error || 'mixed-overlay-outside-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkMixedOverlayDialogCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkMixedOverlay || {};
      window.VaadinDesktopFrameworkMixedOverlay = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      try {
        const dialog =
          document.querySelector('#framework-mixed-overlay-dialog');
        const popover =
          document.querySelector('#framework-mixed-overlay-popover');
        const trigger =
          document.querySelector('#framework-mixed-overlay-dialog-open');
        const outside =
          document.querySelector('#framework-mixed-overlay-outside-target');
        if (!dialog || dialog.opened !== true
            || !popover || popover.opened === true || !trigger || !outside) {
          marker.dialogError = 'mixed-overlay-dialog-close-elements-missing';
          return true;
        }
        outside.focus({ preventScroll: true });
        (activeDeepElements().at(-1) || outside).dispatchEvent(
          new KeyboardEvent('keydown', {
            key: 'Escape',
            code: 'Escape',
            keyCode: 27,
            which: 27,
            bubbles: true,
            composed: true,
            cancelable: true
          }));
        marker.parentEscapeDispatched = true;
        const captureCloseState = () => {
          marker.layeredFocusRestoredObserved =
            dialog.opened !== true
              && activeDeepElements().includes(trigger);
        };
        captureCloseState();
        setTimeout(captureCloseState, 50);
      } catch (error) {
        marker.dialogError =
          String(error?.message || error || 'mixed-overlay-dialog-close-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverOuterOpenAction = """
    (() => {
      const target =
        document.querySelector('#framework-nested-popover-outer-target');
      if (!target || typeof target.click !== 'function') return false;
      window.VaadinDesktopFrameworkNestedPopover = {
        outerOpenedObserved: false,
        stackOpenedObserved: false,
        a11yObserved: false,
        tabForwardObserved: false,
        tabBackwardObserved: false,
        topEscapeDispatched: false,
        parentRemainsAfterTopEscape: false,
        innerFocusRestoredObserved: false,
        parentEscapeDispatched: false,
        layeredFocusRestoredObserved: false,
        outsideTopDispatched: false,
        parentRemainsAfterOutsideTop: false,
        outsideFocusPreservedObserved: false,
        outsideParentDispatched: false,
        keyboardError: '',
        outsideError: ''
      };
      target.scrollIntoView({ block: 'center', inline: 'center' });
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverInnerOpenAction = """
    (() => {
      const outer =
        document.querySelector('#framework-nested-popover-outer');
      const target =
        document.querySelector('#framework-nested-popover-inner-target');
      if (!outer || outer.opened !== true
          || !target || typeof target.click !== 'function') return false;
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverKeyboardCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkNestedPopover || {};
      window.VaadinDesktopFrameworkNestedPopover = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      const deepestActiveElement = () =>
        activeDeepElements().at(-1) || document;
      try {
        const outer =
          document.querySelector('#framework-nested-popover-outer');
        const outerTarget =
          document.querySelector('#framework-nested-popover-outer-target');
        const inner =
          document.querySelector('#framework-nested-popover-inner');
        const innerTarget =
          document.querySelector('#framework-nested-popover-inner-target');
        const focusField =
          document.querySelector(
            '#framework-nested-popover-inner-focus-field');
        const lastAction =
          document.querySelector(
            '#framework-nested-popover-inner-last-action');
        const outerOverlay =
          outer?.shadowRoot?.querySelector('vaadin-popover-overlay');
        const innerOverlay =
          inner?.shadowRoot?.querySelector('vaadin-popover-overlay');
        const outerAriaTarget = outerTarget?.ariaTarget || outerTarget;
        const innerAriaTarget = innerTarget?.ariaTarget || innerTarget;
        if (!outer || !outerTarget || !inner || !innerTarget
            || !focusField || !lastAction || !outerOverlay || !innerOverlay
            || !outerAriaTarget || !innerAriaTarget) {
          marker.keyboardError = 'nested-popover-elements-missing';
          return true;
        }
        marker.outerOpenedObserved =
          outer.opened === true
            && outer.target === outerTarget
            && outerOverlay.positionTarget === outerTarget
            && outerOverlay.modeless === true
            && outerOverlay.focusTrap !== true;
        marker.stackOpenedObserved =
          marker.outerOpenedObserved === true
            && inner.opened === true
            && outer.contains(inner)
            && outer.contains(innerTarget)
            && inner.target === innerTarget
            && innerOverlay.positionTarget === innerTarget
            && innerOverlay.modeless === true
            && innerOverlay.focusTrap !== true;
        marker.a11yObserved =
          marker.stackOpenedObserved === true
            && String(outer.getAttribute('role') || '').toLowerCase()
              === 'dialog'
            && String(outer.getAttribute('aria-label') || '')
              .includes('Flow IPC nested outer popover')
            && outerAriaTarget.getAttribute('aria-expanded') === 'true'
            && outerAriaTarget.getAttribute('aria-controls') === outer.id
            && outerAriaTarget.getAttribute('aria-haspopup') === 'dialog'
            && String(inner.getAttribute('role') || '').toLowerCase()
              === 'dialog'
            && String(inner.getAttribute('aria-label') || '')
              .includes('Flow IPC nested inner popover')
            && innerAriaTarget.getAttribute('aria-expanded') === 'true'
            && innerAriaTarget.getAttribute('aria-controls') === inner.id
            && innerAriaTarget.getAttribute('aria-haspopup') === 'dialog';

        const dispatchWhenReady = (remainingAttempts) => {
          try {
            innerTarget.focus({ preventScroll: true });
            const tabEvent = new KeyboardEvent('keydown', {
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9,
              bubbles: true,
              composed: true,
              cancelable: true
            });
            innerTarget.dispatchEvent(tabEvent);
            if (!tabEvent.defaultPrevented && remainingAttempts > 0) {
              setTimeout(() => dispatchWhenReady(remainingAttempts - 1), 50);
              return;
            }
            const tabActive = activeDeepElements();
            marker.tabForwardObserved =
              tabEvent.defaultPrevented === true
                && tabActive.some((element) =>
                  element === inner
                    || element === focusField
                    || element === focusField.inputElement
                    || element === lastAction
                    || inner.contains?.(element));

            const shiftTabEvent = new KeyboardEvent('keydown', {
              key: 'Tab',
              code: 'Tab',
              keyCode: 9,
              which: 9,
              shiftKey: true,
              bubbles: true,
              composed: true,
              cancelable: true
            });
            deepestActiveElement().dispatchEvent(shiftTabEvent);
            marker.tabBackwardObserved =
              shiftTabEvent.defaultPrevented === true
                && activeDeepElements().includes(innerTarget);

            focusField.focus({ preventScroll: true });
            deepestActiveElement().dispatchEvent(new KeyboardEvent('keydown', {
              key: 'Escape',
              code: 'Escape',
              keyCode: 27,
              which: 27,
              bubbles: true,
              composed: true,
              cancelable: true
            }));
            marker.topEscapeDispatched = true;
            const captureCloseState = () => {
              marker.parentRemainsAfterTopEscape =
                outer.opened === true && inner.opened !== true;
              marker.innerFocusRestoredObserved =
                inner.opened !== true
                  && activeDeepElements().includes(innerTarget);
            };
            captureCloseState();
            setTimeout(captureCloseState, 50);
          } catch (error) {
            marker.keyboardError =
              String(error?.message || error || 'nested-popover-keyboard-timeout-error');
          }
        };
        dispatchWhenReady(20);
      } catch (error) {
        marker.keyboardError =
          String(error?.message || error || 'nested-popover-keyboard-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverOuterKeyboardCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkNestedPopover || {};
      window.VaadinDesktopFrameworkNestedPopover = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      try {
        const outer =
          document.querySelector('#framework-nested-popover-outer');
        const outerTarget =
          document.querySelector('#framework-nested-popover-outer-target');
        const inner =
          document.querySelector('#framework-nested-popover-inner');
        const innerTarget =
          document.querySelector('#framework-nested-popover-inner-target');
        if (!outer || outer.opened !== true || !outerTarget
            || !inner || inner.opened === true || !innerTarget) {
          marker.keyboardError =
            'nested-popover-outer-keyboard-close-elements-missing';
          return true;
        }
        marker.parentRemainsAfterTopEscape = true;
        marker.innerFocusRestoredObserved =
          activeDeepElements().includes(innerTarget);
        innerTarget.focus({ preventScroll: true });
        (activeDeepElements().at(-1) || innerTarget).dispatchEvent(
          new KeyboardEvent('keydown', {
            key: 'Escape',
            code: 'Escape',
            keyCode: 27,
            which: 27,
            bubbles: true,
            composed: true,
            cancelable: true
          }));
        marker.parentEscapeDispatched = true;
        const captureCloseState = () => {
          marker.layeredFocusRestoredObserved =
            outer.opened !== true
              && activeDeepElements().includes(outerTarget);
        };
        captureCloseState();
        setTimeout(captureCloseState, 50);
      } catch (error) {
        marker.keyboardError =
          String(error?.message || error || 'nested-popover-outer-keyboard-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverOutsideOuterOpenAction = """
    (() => {
      const outer =
        document.querySelector('#framework-nested-popover-outer');
      const target =
        document.querySelector('#framework-nested-popover-outer-target');
      if (!outer || outer.opened === true
          || !target || typeof target.click !== 'function') return false;
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverOutsideInnerOpenAction = """
    (() => {
      const outer =
        document.querySelector('#framework-nested-popover-outer');
      const inner =
        document.querySelector('#framework-nested-popover-inner');
      const target =
        document.querySelector('#framework-nested-popover-inner-target');
      if (!outer || outer.opened !== true || !inner || inner.opened === true
          || !target || typeof target.click !== 'function') return false;
      target.focus({ preventScroll: true });
      target.click();
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverOutsideTopCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkNestedPopover || {};
      window.VaadinDesktopFrameworkNestedPopover = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      try {
        const outer =
          document.querySelector('#framework-nested-popover-outer');
        const inner =
          document.querySelector('#framework-nested-popover-inner');
        const outside =
          document.querySelector('#framework-nested-popover-outside-target');
        if (!outer || outer.opened !== true || !inner || inner.opened !== true
            || !outside) {
          marker.outsideError =
            'nested-popover-outside-top-close-elements-missing';
          return true;
        }
        outside.focus({ preventScroll: true });
        for (const type of ['mousedown', 'mouseup', 'click']) {
          outside.dispatchEvent(new MouseEvent(type, {
            bubbles: true,
            composed: true,
            cancelable: true,
            button: 0
          }));
        }
        marker.outsideTopDispatched = true;
        const captureCloseState = () => {
          marker.parentRemainsAfterOutsideTop =
            outer.opened === true && inner.opened !== true;
          marker.outsideFocusPreservedObserved =
            inner.opened !== true
              && activeDeepElements().includes(outside);
        };
        captureCloseState();
        setTimeout(captureCloseState, 50);
      } catch (error) {
        marker.outsideError =
          String(error?.message || error || 'nested-popover-outside-top-error');
      }
      return true;
    })()
    """

    private static let standardFrameworkNestedPopoverOutsideParentCloseAction = """
    (() => {
      const marker = window.VaadinDesktopFrameworkNestedPopover || {};
      window.VaadinDesktopFrameworkNestedPopover = marker;
      const activeDeepElements = () => {
        const elements = [];
        let current = document.activeElement;
        while (current) {
          elements.push(current);
          current = current.shadowRoot?.activeElement || null;
        }
        return elements;
      };
      try {
        const outer =
          document.querySelector('#framework-nested-popover-outer');
        const inner =
          document.querySelector('#framework-nested-popover-inner');
        const outside =
          document.querySelector('#framework-nested-popover-outside-target');
        if (!outer || outer.opened !== true || !inner || inner.opened === true
            || !outside) {
          marker.outsideError =
            'nested-popover-outside-parent-close-elements-missing';
          return true;
        }
        outside.focus({ preventScroll: true });
        for (const type of ['mousedown', 'mouseup', 'click']) {
          outside.dispatchEvent(new MouseEvent(type, {
            bubbles: true,
            composed: true,
            cancelable: true,
            button: 0
          }));
        }
        marker.outsideParentDispatched = true;
        const captureCloseState = () => {
          marker.outsideFocusPreservedObserved =
            outer.opened !== true
              && activeDeepElements().includes(outside);
        };
        captureCloseState();
        setTimeout(captureCloseState, 50);
      } catch (error) {
        marker.outsideError =
          String(error?.message || error || 'nested-popover-outside-parent-error');
      }
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
