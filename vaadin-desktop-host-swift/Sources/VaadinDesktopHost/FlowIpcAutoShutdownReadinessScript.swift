let flowIpcAutoShutdownReadinessScript = """
        (() => {
          const d = window.VaadinDesktopIpcDiagnostics || {};
          const componentEventsNeeded =
            !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__;
          const smokeComplete = !!d.flowClientEngineEventPatchConsumed
            && (!componentEventsNeeded || (
              !!d.flowClientEngineTextInputEventObserved
              && !!d.flowClientEngineCheckboxEventObserved
              && !!d.vaadinWebComponentEventRpcObserved
              && !!d.vaadinTextFieldEventRpcObserved
              && !!d.vaadinSecondWebComponentEventRpcObserved
              && !!d.vaadinSelectEventRpcObserved
            ))
            && (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ || !!d.flowClientEngineResyncPatchConsumed)
            && (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR__ || !!d.flowClientEngineErrorReported)
            && (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__ || (
              d.frontendArtifactTextFieldExecutionVersion === 'flow-ipc-textfield-v1_7'
              && !!d.frontendArtifactTextFieldExecuted
              && !d.frontendArtifactTextFieldShimUsed
            ))
            && !!d.flowClientResourceExecutionClassificationComplete
            && !!d.vaadinWebComponentResourceGraphVerified
            && !!d.frontendStagingObserved;
          const componentMatrixComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX__
            || !!d.frameworkComponentMatrixComplete;
          const dataComponentMatrixComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS__
            || !!d.frameworkDataComponentMatrixComplete;
          const inputComponentMatrixComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_INPUT_COMPONENTS__
            || !!d.frameworkInputComponentMatrixComplete;
          const navigationComponentMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_NAVIGATION_COMPONENTS__
            || !!d.frameworkNavigationComponentMatrixComplete;
          const overlayComponentMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_OVERLAY_COMPONENTS__
            || !!d.frameworkOverlayComponentMatrixComplete;
          const richContentComponentMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_CONTENT_COMPONENTS__
            || !!d.frameworkRichContentComponentMatrixComplete;
          const richTextEditorMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR__
            || !!d.frameworkRichTextEditorMatrixComplete;
          const richTextEditorBehaviorMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_BEHAVIOR__
            || !!d.frameworkRichTextEditorBehaviorMatrixComplete;
          const richTextEditorEditingMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_EDITING__
            || !!d.frameworkRichTextEditorEditingMatrixComplete;
          const richTextEditorThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_THEME__
            || !!d.richTextEditorThemeVariantMatrixComplete;
          const nativeClipboardReadMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_CLIPBOARD_READ__
            || !!d.nativeClipboardReadMatrixComplete;
          const buttonThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_BUTTON_THEME__
            || !!d.buttonThemeVariantMatrixComplete;
          const textFieldThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_FIELD_THEME__
            || !!d.textFieldThemeVariantMatrixComplete;
          const selectThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_SELECT_THEME__
            || !!d.selectThemeVariantMatrixComplete;
          const comboBoxThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_COMBO_BOX_THEME__
            || !!d.comboBoxThemeVariantMatrixComplete;
          const datePickerThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DATE_PICKER_THEME__
            || !!d.datePickerThemeVariantMatrixComplete;
          const multiSelectComboBoxThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_SELECT_COMBO_BOX_THEME__
            || !!d.multiSelectComboBoxThemeVariantMatrixComplete;
          const timePickerThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TIME_PICKER_THEME__
            || !!d.timePickerThemeVariantMatrixComplete;
          const textAreaThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_AREA_THEME__
            || !!d.textAreaThemeVariantMatrixComplete;
          const emailFieldThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_EMAIL_FIELD_THEME__
            || !!d.emailFieldThemeVariantMatrixComplete;
          const integerFieldThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_INTEGER_FIELD_THEME__
            || !!d.integerFieldThemeVariantMatrixComplete;
          const passwordFieldThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_PASSWORD_FIELD_THEME__
            || !!d.passwordFieldThemeVariantMatrixComplete;
          const bigDecimalFieldThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_BIG_DECIMAL_FIELD_THEME__
            || !!d.bigDecimalFieldThemeVariantMatrixComplete;
          const checkboxThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CHECKBOX_THEME__
            || !!d.checkboxThemeVariantMatrixComplete;
          const radioButtonGroupThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RADIO_BUTTON_GROUP_THEME__
            || !!d.radioButtonGroupThemeVariantMatrixComplete;
          const tabsThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TABS_THEME__
            || !!d.tabsThemeVariantMatrixComplete;
          const detailsThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DETAILS_THEME__
            || !!d.detailsThemeVariantMatrixComplete;
          const menuBarThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MENU_BAR_THEME__
            || !!d.menuBarThemeVariantMatrixComplete;
          const gridThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_THEME__
            || !!d.gridThemeVariantMatrixComplete;
          const avatarThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_THEME__
            || !!d.avatarThemeVariantMatrixComplete;
          const avatarGroupThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_GROUP_THEME__
            || !!d.avatarGroupThemeVariantMatrixComplete;
          const messageInputThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_INPUT_THEME__
            || !!d.messageInputThemeVariantMatrixComplete;
          const sideNavThemeVariantMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDE_NAV_THEME__
            || !!d.sideNavThemeVariantMatrixComplete;
          const messageListMarkdownMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_LIST_MARKDOWN__
            || !!d.messageListMarkdownMatrixComplete;
          const loginOverlayPropertyMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_PROPERTIES__
            || !!d.loginOverlayPropertyMatrixComplete;
          const loginOverlayForgotPasswordEventMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_FORGOT_PASSWORD__
            || !!d.loginOverlayForgotPasswordEventMatrixComplete;
          const loginOverlayLoginEventMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_LOGIN__
            || !!d.loginOverlayLoginEventMatrixComplete;
          const loginOverlayInvalidLoginEventMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_INVALID_LOGIN__
            || !!d.loginOverlayInvalidLoginEventMatrixComplete;
          const loginOverlayCloseLifecycleMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_CLOSE__
            || !!d.loginOverlayCloseLifecycleMatrixComplete;
          const loginOverlayRecoveryLoginMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_RECOVERY_LOGIN__
            || !!d.loginOverlayRecoveryLoginMatrixComplete;
          const loginOverlayServerReopenMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SERVER_REOPEN__
            || !!d.loginOverlayServerReopenMatrixComplete;
          const loginOverlayShowErrorMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHOW_ERROR__
            || !!d.loginOverlayShowErrorMatrixComplete;
          const loginOverlayErrorA11yMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_A11Y__
            || !!d.loginOverlayErrorA11yMatrixComplete;
          const loginOverlayErrorFocusMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS__
            || !!d.loginOverlayErrorFocusMatrixComplete;
          const loginOverlayErrorFocusRetryMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY__
            || !!d.loginOverlayErrorFocusRetryMatrixComplete;
          const loginOverlayErrorFocusRetryTargetMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET__
            || !!d.loginOverlayErrorFocusRetryTargetMatrixComplete;
          const loginOverlayFullI18nMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_I18N_FULL__
            || !!d.loginOverlayFullI18nMatrixComplete;
          const loginOverlayNoAutofocusMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_NO_AUTOFOCUS__
            || !!d.loginOverlayNoAutofocusMatrixComplete;
          const loginOverlayAutofocusControlMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFOCUS_CONTROL__
            || !!d.loginOverlayAutofocusControlMatrixComplete;
          const loginOverlayEscCloseMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ESC_CLOSE__
            || !!d.loginOverlayEscCloseMatrixComplete;
          const loginOverlayOutsideCloseMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_OUTSIDE_CLOSE__
            || !!d.loginOverlayOutsideCloseMatrixComplete;
          const loginOverlayTabTraversalMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_TAB_TRAVERSAL__
            || !!d.loginOverlayTabTraversalMatrixComplete;
          const loginOverlayShiftTabTraversalMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL__
            || !!d.loginOverlayShiftTabTraversalMatrixComplete;
          const loginOverlayAutofillAttributesMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES__
            || !!d.loginOverlayAutofillAttributesMatrixComplete;
          const loginOverlayA11ySemanticsMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_A11Y_SEMANTICS__
            || !!d.loginOverlayA11ySemanticsMatrixComplete;
          const loginOverlayDisabledGuardMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_GUARD__
            || !!d.loginOverlayDisabledGuardMatrixComplete;
          const loginOverlayDisabledKeyboardGuardMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD__
            || !!d.loginOverlayDisabledKeyboardGuardMatrixComplete;
          const loginOverlayActionPostMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ACTION_POST__
            || !!d.loginOverlayActionPostMatrixComplete;
          const loginOverlaySubmitButtonMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SUBMIT_BUTTON__
            || !!d.loginOverlaySubmitButtonMatrixComplete;
          const loginOverlayEnterSubmitMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ENTER_SUBMIT__
            || !!d.loginOverlayEnterSubmitMatrixComplete;
          const loginFormPropertyMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_PROPERTIES__
            || !!d.loginFormPropertyMatrixComplete;
          const loginFormLoginEventMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_LOGIN__
            || !!d.loginFormLoginEventMatrixComplete;
          const loginFormInvalidLoginEventMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_INVALID_LOGIN__
            || !!d.loginFormInvalidLoginEventMatrixComplete;
          const loginFormForgotPasswordEventMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_FORGOT_PASSWORD__
            || !!d.loginFormForgotPasswordEventMatrixComplete;
          const loginFormActionPostMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ACTION_POST__
            || !!d.loginFormActionPostMatrixComplete;
          const loginFormSubmitButtonMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SUBMIT_BUTTON__
            || !!d.loginFormSubmitButtonMatrixComplete;
          const loginFormEnterSubmitMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENTER_SUBMIT__
            || !!d.loginFormEnterSubmitMatrixComplete;
          const loginFormRecoveryLoginMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_RECOVERY_LOGIN__
            || !!d.loginFormRecoveryLoginMatrixComplete;
          const loginFormEnabledLifecycleMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENABLED_LIFECYCLE__
            || !!d.loginFormEnabledLifecycleMatrixComplete;
          const loginFormDisabledGuardMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_GUARD__
            || !!d.loginFormDisabledGuardMatrixComplete;
          const loginFormDisabledKeyboardGuardMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_KEYBOARD_GUARD__
            || !!d.loginFormDisabledKeyboardGuardMatrixComplete;
          const loginFormNoAutofocusMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_NO_AUTOFOCUS__
            || !!d.loginFormNoAutofocusMatrixComplete;
          const loginFormAutofocusControlMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFOCUS_CONTROL__
            || !!d.loginFormAutofocusControlMatrixComplete;
          const loginFormTabTraversalMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_TAB_TRAVERSAL__
            || !!d.loginFormTabTraversalMatrixComplete;
          const loginFormShiftTabTraversalMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHIFT_TAB_TRAVERSAL__
            || !!d.loginFormShiftTabTraversalMatrixComplete;
          const loginFormAutofillAttributesMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFILL_ATTRIBUTES__
            || !!d.loginFormAutofillAttributesMatrixComplete;
          const loginFormA11ySemanticsMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_A11Y_SEMANTICS__
            || !!d.loginFormA11ySemanticsMatrixComplete;
          const loginFormFullI18nMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_I18N_FULL__
            || !!d.loginFormFullI18nMatrixComplete;
          const loginFormShowErrorMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHOW_ERROR__
            || !!d.loginFormShowErrorMatrixComplete;
          const loginFormErrorA11yMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_A11Y__
            || !!d.loginFormErrorA11yMatrixComplete;
          const loginFormErrorFocusMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS__
            || !!d.loginFormErrorFocusMatrixComplete;
          const loginFormErrorFocusRetryMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY__
            || !!d.loginFormErrorFocusRetryMatrixComplete;
          const loginFormErrorFocusRetryTargetMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET__
            || !!d.loginFormErrorFocusRetryTargetMatrixComplete;
          const loginFormErrorFocusRetryTargetStateMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE__
            || !!d.loginFormErrorFocusRetryTargetStateMatrixComplete;
          const loginFormErrorFocusRetryTargetLayoutMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT__
            || !!d.loginFormErrorFocusRetryTargetLayoutMatrixComplete;
          const loginFormErrorFocusRetryTargetHiddenSourceMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE__
            || !!d.loginFormErrorFocusRetryTargetHiddenSourceMatrixComplete;
          const loginFormErrorFocusRetryTargetVisibleOwnerMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER__
            || !!d.loginFormErrorFocusRetryTargetVisibleOwnerMatrixComplete;
          const loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS__
            || !!d.loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixComplete;
          const loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT__
            || !!d.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixComplete;
          const loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY__
            || !!d.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixComplete;
          const lifecycleFocusBehaviorMatrixComplete =
            !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LIFECYCLE_FOCUS__
            || !!d.frameworkLifecycleFocusBehaviorMatrixComplete;
          const lazyDataProviderComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA__
            || !!d.frameworkLazyDataProviderComplete;
          const gridScrollVirtualizationComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL__
            || !!d.frameworkGridScrollVirtualizationComplete
            || !!d.frameworkGridScrollVirtualizationAcceptedBlockerObserved;
          const streamResourceComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE__
            || !!d.frameworkStreamResourceComplete;
          const streamResourceRangeComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE__
            || !!d.frameworkStreamResourceRangeComplete;
          const streamResourceChunksComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS__
            || !!d.frameworkStreamResourceChunksComplete;
          const uploadReceiverComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER__
            || !!d.frameworkUploadReceiverComplete;
          const uploadMultipartComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART__
            || !!d.frameworkUploadMultipartComplete;
          const downloadHandlerSaveOpenComplete = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN__
            || !!d.frameworkDownloadHandlerSaveOpenComplete;
          const frontendResourcePipelineBasicClosureComplete =
            !!d.frontendResourcePipelineBasicClosureComplete;
          const frameworkBaseInteractionRequired =
            !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE_REQUESTED__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN__
            || (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_INPUT_COMPONENTS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_NAVIGATION_COMPONENTS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_OVERLAY_COMPONENTS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_CONTENT_COMPONENTS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_BEHAVIOR__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_EDITING__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_CLIPBOARD_READ__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_BUTTON_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_FIELD_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_SELECT_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_COMBO_BOX_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DATE_PICKER_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_SELECT_COMBO_BOX_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TIME_PICKER_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_AREA_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_EMAIL_FIELD_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_INTEGER_FIELD_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_PASSWORD_FIELD_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_BIG_DECIMAL_FIELD_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CHECKBOX_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RADIO_BUTTON_GROUP_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TABS_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DETAILS_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MENU_BAR_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_GROUP_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_INPUT_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDE_NAV_THEME__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_LIST_MARKDOWN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_PROPERTIES__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_FORGOT_PASSWORD__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_LOGIN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_INVALID_LOGIN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_CLOSE__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_RECOVERY_LOGIN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SERVER_REOPEN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHOW_ERROR__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_A11Y__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_I18N_FULL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_NO_AUTOFOCUS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFOCUS_CONTROL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ESC_CLOSE__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_OUTSIDE_CLOSE__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_TAB_TRAVERSAL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_A11Y_SEMANTICS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_GUARD__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ACTION_POST__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SUBMIT_BUTTON__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ENTER_SUBMIT__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_PROPERTIES__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_LOGIN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_INVALID_LOGIN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_FORGOT_PASSWORD__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ACTION_POST__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SUBMIT_BUTTON__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENTER_SUBMIT__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_RECOVERY_LOGIN__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENABLED_LIFECYCLE__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_GUARD__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_KEYBOARD_GUARD__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_NO_AUTOFOCUS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFOCUS_CONTROL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_TAB_TRAVERSAL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHIFT_TAB_TRAVERSAL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFILL_ATTRIBUTES__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_A11Y_SEMANTICS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_I18N_FULL__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHOW_ERROR__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_A11Y__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT__
              && !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY__);
          const frameworkBaseInteractionComplete =
            !frameworkBaseInteractionRequired || !!d.frameworkAppInteractionComplete;
          const frameworkComplete = frameworkBaseInteractionComplete
            && componentMatrixComplete
            && dataComponentMatrixComplete
            && inputComponentMatrixComplete
            && navigationComponentMatrixComplete
            && overlayComponentMatrixComplete
            && richContentComponentMatrixComplete
            && richTextEditorMatrixComplete
            && richTextEditorBehaviorMatrixComplete
            && richTextEditorEditingMatrixComplete
            && richTextEditorThemeVariantMatrixComplete
            && nativeClipboardReadMatrixComplete
            && buttonThemeVariantMatrixComplete
            && textFieldThemeVariantMatrixComplete
            && selectThemeVariantMatrixComplete
            && comboBoxThemeVariantMatrixComplete
            && datePickerThemeVariantMatrixComplete
            && multiSelectComboBoxThemeVariantMatrixComplete
            && timePickerThemeVariantMatrixComplete
            && textAreaThemeVariantMatrixComplete
            && emailFieldThemeVariantMatrixComplete
            && integerFieldThemeVariantMatrixComplete
            && passwordFieldThemeVariantMatrixComplete
            && bigDecimalFieldThemeVariantMatrixComplete
            && checkboxThemeVariantMatrixComplete
            && radioButtonGroupThemeVariantMatrixComplete
            && tabsThemeVariantMatrixComplete
            && detailsThemeVariantMatrixComplete
            && menuBarThemeVariantMatrixComplete
            && gridThemeVariantMatrixComplete
            && avatarThemeVariantMatrixComplete
            && avatarGroupThemeVariantMatrixComplete
            && messageInputThemeVariantMatrixComplete
            && sideNavThemeVariantMatrixComplete
            && messageListMarkdownMatrixComplete
            && loginOverlayPropertyMatrixComplete
            && loginOverlayForgotPasswordEventMatrixComplete
            && loginOverlayLoginEventMatrixComplete
            && loginOverlayInvalidLoginEventMatrixComplete
            && loginOverlayCloseLifecycleMatrixComplete
            && loginOverlayRecoveryLoginMatrixComplete
            && loginOverlayServerReopenMatrixComplete
            && loginOverlayShowErrorMatrixComplete
            && loginOverlayErrorA11yMatrixComplete
            && loginOverlayErrorFocusMatrixComplete
            && loginOverlayErrorFocusRetryMatrixComplete
            && loginOverlayErrorFocusRetryTargetMatrixComplete
            && loginOverlayFullI18nMatrixComplete
            && loginOverlayNoAutofocusMatrixComplete
            && loginOverlayAutofocusControlMatrixComplete
            && loginOverlayEscCloseMatrixComplete
            && loginOverlayOutsideCloseMatrixComplete
            && loginOverlayTabTraversalMatrixComplete
            && loginOverlayShiftTabTraversalMatrixComplete
            && loginOverlayAutofillAttributesMatrixComplete
            && loginOverlayA11ySemanticsMatrixComplete
            && loginOverlayDisabledGuardMatrixComplete
            && loginOverlayDisabledKeyboardGuardMatrixComplete
            && loginOverlayActionPostMatrixComplete
            && loginOverlaySubmitButtonMatrixComplete
            && loginOverlayEnterSubmitMatrixComplete
            && loginFormPropertyMatrixComplete
            && loginFormLoginEventMatrixComplete
            && loginFormInvalidLoginEventMatrixComplete
            && loginFormForgotPasswordEventMatrixComplete
            && loginFormActionPostMatrixComplete
            && loginFormSubmitButtonMatrixComplete
            && loginFormEnterSubmitMatrixComplete
            && loginFormRecoveryLoginMatrixComplete
            && loginFormEnabledLifecycleMatrixComplete
            && loginFormDisabledGuardMatrixComplete
            && loginFormDisabledKeyboardGuardMatrixComplete
            && loginFormNoAutofocusMatrixComplete
            && loginFormAutofocusControlMatrixComplete
            && loginFormTabTraversalMatrixComplete
            && loginFormShiftTabTraversalMatrixComplete
            && loginFormAutofillAttributesMatrixComplete
            && loginFormA11ySemanticsMatrixComplete
            && loginFormFullI18nMatrixComplete
            && loginFormShowErrorMatrixComplete
            && loginFormErrorA11yMatrixComplete
            && loginFormErrorFocusMatrixComplete
            && loginFormErrorFocusRetryMatrixComplete
            && loginFormErrorFocusRetryTargetMatrixComplete
            && loginFormErrorFocusRetryTargetStateMatrixComplete
            && loginFormErrorFocusRetryTargetLayoutMatrixComplete
            && loginFormErrorFocusRetryTargetHiddenSourceMatrixComplete
            && loginFormErrorFocusRetryTargetVisibleOwnerMatrixComplete
            && loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixComplete
            && loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixComplete
            && loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixComplete
            && lifecycleFocusBehaviorMatrixComplete
            && lazyDataProviderComplete
            && gridScrollVirtualizationComplete
            && streamResourceComplete
            && streamResourceRangeComplete
            && streamResourceChunksComplete
            && uploadReceiverComplete
            && uploadMultipartComplete
            && downloadHandlerSaveOpenComplete;
          const frameworkVerificationRequired =
            !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_INPUT_COMPONENTS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_NAVIGATION_COMPONENTS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_OVERLAY_COMPONENTS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_CONTENT_COMPONENTS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_BEHAVIOR__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_EDITING__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_CLIPBOARD_READ__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_BUTTON_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_FIELD_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_SELECT_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_COMBO_BOX_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DATE_PICKER_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_SELECT_COMBO_BOX_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TIME_PICKER_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_AREA_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_EMAIL_FIELD_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_INTEGER_FIELD_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_PASSWORD_FIELD_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_BIG_DECIMAL_FIELD_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CHECKBOX_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RADIO_BUTTON_GROUP_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_TABS_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DETAILS_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MENU_BAR_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_GROUP_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_INPUT_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDE_NAV_THEME__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_LIST_MARKDOWN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_PROPERTIES__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_FORGOT_PASSWORD__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_LOGIN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_INVALID_LOGIN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_CLOSE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_RECOVERY_LOGIN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SERVER_REOPEN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHOW_ERROR__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_I18N_FULL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_NO_AUTOFOCUS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFOCUS_CONTROL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ESC_CLOSE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_OUTSIDE_CLOSE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_TAB_TRAVERSAL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_A11Y_SEMANTICS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_GUARD__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ACTION_POST__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SUBMIT_BUTTON__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ENTER_SUBMIT__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_PROPERTIES__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_LOGIN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_INVALID_LOGIN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_FORGOT_PASSWORD__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ACTION_POST__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SUBMIT_BUTTON__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENTER_SUBMIT__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_RECOVERY_LOGIN__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENABLED_LIFECYCLE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_GUARD__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_KEYBOARD_GUARD__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_NO_AUTOFOCUS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFOCUS_CONTROL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_TAB_TRAVERSAL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHIFT_TAB_TRAVERSAL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFILL_ATTRIBUTES__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_A11Y_SEMANTICS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_I18N_FULL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHOW_ERROR__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_A11Y__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LIFECYCLE_FOCUS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN__;
          const complete = frameworkVerificationRequired
            ? frameworkComplete
            : smokeComplete;
          return JSON.stringify({
            complete,
            smokeComplete,
            frameworkComplete,
            componentMatrixComplete,
            dataComponentMatrixComplete,
            inputComponentMatrixComplete,
            navigationComponentMatrixComplete,
            overlayComponentMatrixComplete,
            richContentComponentMatrixComplete,
            richTextEditorMatrixComplete,
            richTextEditorBehaviorMatrixComplete,
            richTextEditorEditingMatrixComplete,
            richTextEditorThemeVariantMatrixComplete,
            nativeClipboardReadMatrixComplete,
            buttonThemeVariantMatrixComplete,
            textFieldThemeVariantMatrixComplete,
            selectThemeVariantMatrixComplete,
            comboBoxThemeVariantMatrixComplete,
            datePickerThemeVariantMatrixComplete,
            multiSelectComboBoxThemeVariantMatrixComplete,
            timePickerThemeVariantMatrixComplete,
            textAreaThemeVariantMatrixComplete,
            emailFieldThemeVariantMatrixComplete,
            integerFieldThemeVariantMatrixComplete,
            passwordFieldThemeVariantMatrixComplete,
            bigDecimalFieldThemeVariantMatrixComplete,
            checkboxThemeVariantMatrixComplete,
            radioButtonGroupThemeVariantMatrixComplete,
            tabsThemeVariantMatrixComplete,
            detailsThemeVariantMatrixComplete,
            menuBarThemeVariantMatrixComplete,
            gridThemeVariantMatrixComplete,
            avatarThemeVariantMatrixComplete,
            avatarGroupThemeVariantMatrixComplete,
            messageInputThemeVariantMatrixComplete,
            sideNavThemeVariantMatrixComplete,
            messageListMarkdownMatrixComplete,
            loginOverlayPropertyMatrixComplete,
            loginOverlayForgotPasswordEventMatrixComplete,
            loginOverlayLoginEventMatrixComplete,
            loginOverlayInvalidLoginEventMatrixComplete,
            loginOverlayCloseLifecycleMatrixComplete,
            loginOverlayRecoveryLoginMatrixComplete,
            loginOverlayServerReopenMatrixComplete,
            loginOverlayShowErrorMatrixComplete,
            loginOverlayErrorA11yMatrixComplete,
            loginOverlayErrorFocusMatrixComplete,
            loginOverlayErrorFocusRetryMatrixComplete,
            loginOverlayErrorFocusRetryTargetMatrixComplete,
            loginOverlayFullI18nMatrixComplete,
            loginOverlayNoAutofocusMatrixComplete,
            loginOverlayAutofocusControlMatrixComplete,
            loginOverlayEscCloseMatrixComplete,
            loginOverlayOutsideCloseMatrixComplete,
            loginOverlayTabTraversalMatrixComplete,
            loginOverlayShiftTabTraversalMatrixComplete,
            loginOverlayAutofillAttributesMatrixComplete,
            loginOverlayA11ySemanticsMatrixComplete,
            loginOverlayDisabledGuardMatrixComplete,
            loginOverlayDisabledKeyboardGuardMatrixComplete,
            loginOverlayActionPostMatrixComplete,
            loginOverlaySubmitButtonMatrixComplete,
            loginOverlayEnterSubmitMatrixComplete,
            loginFormPropertyMatrixComplete,
            loginFormLoginEventMatrixComplete,
            loginFormInvalidLoginEventMatrixComplete,
            loginFormForgotPasswordEventMatrixComplete,
            loginFormActionPostMatrixComplete,
            loginFormSubmitButtonMatrixComplete,
            loginFormEnterSubmitMatrixComplete,
            loginFormRecoveryLoginMatrixComplete,
            loginFormEnabledLifecycleMatrixComplete,
            loginFormDisabledGuardMatrixComplete,
            loginFormDisabledKeyboardGuardMatrixComplete,
            loginFormNoAutofocusMatrixComplete,
            loginFormAutofocusControlMatrixComplete,
            loginFormTabTraversalMatrixComplete,
            loginFormShiftTabTraversalMatrixComplete,
            loginFormAutofillAttributesMatrixComplete,
            loginFormA11ySemanticsMatrixComplete,
            loginFormFullI18nMatrixComplete,
            loginFormShowErrorMatrixComplete,
            loginFormErrorA11yMatrixComplete,
            loginFormErrorFocusMatrixComplete,
            loginFormErrorFocusRetryMatrixComplete,
            loginFormErrorFocusRetryTargetMatrixComplete,
            loginFormErrorFocusRetryTargetStateMatrixComplete,
            loginFormErrorFocusRetryTargetLayoutMatrixComplete,
            loginFormErrorFocusRetryTargetHiddenSourceMatrixComplete,
            loginFormErrorFocusRetryTargetVisibleOwnerMatrixComplete,
            loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixComplete,
            loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixComplete,
            loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixComplete,
            lifecycleFocusBehaviorMatrixComplete,
            lazyDataProviderComplete,
            gridScrollVirtualizationComplete,
            streamResourceComplete,
            streamResourceRangeComplete,
            streamResourceChunksComplete,
            uploadReceiverComplete,
            uploadMultipartComplete,
            downloadHandlerSaveOpenComplete,
            frontendResourcePipelineBasicClosureComplete,
            click: !!d.flowClientEngineEventPatchConsumed,
            text: !!d.flowClientEngineTextInputEventObserved,
            checkbox: !!d.flowClientEngineCheckboxEventObserved,
            bigDecimal: !!d.vaadinWebComponentEventRpcObserved,
            textField: !!d.vaadinTextFieldEventRpcObserved,
            vaadinCheckbox: !!d.vaadinSecondWebComponentEventRpcObserved,
            vaadinSelect: !!d.vaadinSelectEventRpcObserved,
            resync: !!d.flowClientEngineResyncPatchConsumed,
            error: !!d.flowClientEngineErrorReported,
            flowGraph: !!d.flowClientResourceExecutionClassificationComplete,
            componentGraph: !!d.vaadinWebComponentResourceGraphVerified,
            staging: !!d.frontendStagingObserved,
            textFieldBlocker: !!d.frontendArtifactTextFieldStagedBlocker,
            textFieldBlockerVersion: d.frontendArtifactTextFieldBlockerVersion || '',
            textFieldExecutionVersion: d.frontendArtifactTextFieldExecutionVersion || '',
            textFieldExecuted: !!d.frontendArtifactTextFieldExecuted
          });
        })();
        """
