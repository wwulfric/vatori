import Foundation

private struct FlowIpcAutomationFlagDefinition {
    let keyPath: WritableKeyPath<HostConfig, Bool>
    let environmentNames: [String]
    let windowFlagNames: [String]
}

private func flowIpcAutomationFlag(
    _ keyPath: WritableKeyPath<HostConfig, Bool>,
    _ environmentName: String,
    aliases: [String] = [],
    windowFlags: [String]? = nil
) -> FlowIpcAutomationFlagDefinition {
    FlowIpcAutomationFlagDefinition(
        keyPath: keyPath,
        environmentNames: [environmentName] + aliases,
        windowFlagNames: windowFlags ?? [environmentName])
}

private let flowIpcAutomationFlagDefinitions: [FlowIpcAutomationFlagDefinition] = [
    flowIpcAutomationFlag(\.flowIpcAutoClick, "VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK"),
    flowIpcAutomationFlag(\.flowIpcAutoResync, "VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC"),
    flowIpcAutomationFlag(\.flowIpcAutoError, "VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR"),
    flowIpcAutomationFlag(
        \.flowIpcAutoFileDialogCancel,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL",
        windowFlags: []),
    flowIpcAutomationFlag(
        \.flowIpcAutoVaadinTextField,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD",
        aliases: ["VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL"],
        windowFlags: [
            "VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD",
            "VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL"
        ]),
    flowIpcAutomationFlag(\.flowIpcAutoFrameworkUpdate, "VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoFrameworkUpdateRequested,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE",
        windowFlags: ["VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE_REQUESTED"]),
    flowIpcAutomationFlag(\.flowIpcAutoComponentMatrix, "VAADIN_DESKTOP_FLOW_IPC_AUTO_COMPONENT_MATRIX"),
    flowIpcAutomationFlag(\.flowIpcAutoDataComponents, "VAADIN_DESKTOP_FLOW_IPC_AUTO_DATA_COMPONENTS"),
    flowIpcAutomationFlag(\.flowIpcAutoInputComponents, "VAADIN_DESKTOP_FLOW_IPC_AUTO_INPUT_COMPONENTS"),
    flowIpcAutomationFlag(\.flowIpcAutoNavigationComponents, "VAADIN_DESKTOP_FLOW_IPC_AUTO_NAVIGATION_COMPONENTS"),
    flowIpcAutomationFlag(\.flowIpcAutoOverlayComponents, "VAADIN_DESKTOP_FLOW_IPC_AUTO_OVERLAY_COMPONENTS"),
    flowIpcAutomationFlag(\.flowIpcAutoRichContentComponents, "VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_CONTENT_COMPONENTS"),
    flowIpcAutomationFlag(\.flowIpcAutoRichTextEditor, "VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR"),
    flowIpcAutomationFlag(
        \.flowIpcAutoRichTextEditorBehavior,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_BEHAVIOR"),
    flowIpcAutomationFlag(
        \.flowIpcAutoRichTextEditorEditing,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_EDITING"),
    flowIpcAutomationFlag(\.flowIpcAutoRichTextEditorTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_RICH_TEXT_EDITOR_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoNativeClipboardRead, "VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_CLIPBOARD_READ"),
    flowIpcAutomationFlag(\.flowIpcAutoButtonTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_BUTTON_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoTextFieldTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_FIELD_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoSelectTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_SELECT_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoComboBoxTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_COMBO_BOX_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoDatePickerTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_DATE_PICKER_THEME"),
    flowIpcAutomationFlag(
        \.flowIpcAutoMultiSelectComboBoxTheme,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_SELECT_COMBO_BOX_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoTimePickerTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_TIME_PICKER_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoTextAreaTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_TEXT_AREA_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoEmailFieldTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_EMAIL_FIELD_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoIntegerFieldTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_INTEGER_FIELD_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoPasswordFieldTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_PASSWORD_FIELD_THEME"),
    flowIpcAutomationFlag(
        \.flowIpcAutoBigDecimalFieldTheme,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_BIG_DECIMAL_FIELD_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoCheckboxTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_CHECKBOX_THEME"),
    flowIpcAutomationFlag(
        \.flowIpcAutoRadioButtonGroupTheme,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_RADIO_BUTTON_GROUP_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoTabsTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_TABS_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoDetailsTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_DETAILS_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoMenuBarTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_MENU_BAR_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoGridTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoAvatarTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoAvatarGroupTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_AVATAR_GROUP_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoMessageInputTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_INPUT_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoSideNavTheme, "VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDE_NAV_THEME"),
    flowIpcAutomationFlag(\.flowIpcAutoMessageListMarkdown, "VAADIN_DESKTOP_FLOW_IPC_AUTO_MESSAGE_LIST_MARKDOWN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayProperties,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_PROPERTIES"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayForgotPassword,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_FORGOT_PASSWORD"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginOverlayLogin, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_LOGIN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayInvalidLogin,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_INVALID_LOGIN"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginOverlayClose, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_CLOSE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayRecoveryLogin,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_RECOVERY_LOGIN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayServerReopen,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SERVER_REOPEN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayShowError,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHOW_ERROR"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayErrorA11y,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_A11Y"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayErrorFocus,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayErrorFocusRetry,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayErrorFocusRetryTarget,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayActionPost,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ACTION_POST"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlaySubmitButton,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SUBMIT_BUTTON"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayEnterSubmit,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ENTER_SUBMIT"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayFullI18n,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_I18N_FULL"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayNoAutofocus,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_NO_AUTOFOCUS"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayAutofocusControl,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFOCUS_CONTROL"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayEscClose,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_ESC_CLOSE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayOutsideClose,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_OUTSIDE_CLOSE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayTabTraversal,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_TAB_TRAVERSAL"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayShiftTabTraversal,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayAutofillAttributes,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayA11ySemantics,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_A11Y_SEMANTICS"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayDisabledGuard,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_GUARD"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginOverlayDisabledKeyboardGuard,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormProperties, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_PROPERTIES"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormLogin, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_LOGIN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormInvalidLogin,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_INVALID_LOGIN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormForgotPassword,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_FORGOT_PASSWORD"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormActionPost, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ACTION_POST"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormSubmitButton,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SUBMIT_BUTTON"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormEnterSubmit, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENTER_SUBMIT"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormRecoveryLogin,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_RECOVERY_LOGIN"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormEnabledLifecycle,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ENABLED_LIFECYCLE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormDisabledGuard,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_GUARD"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormDisabledKeyboardGuard,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_DISABLED_KEYBOARD_GUARD"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormNoAutofocus,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_NO_AUTOFOCUS"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormAutofocusControl,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFOCUS_CONTROL"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormTabTraversal, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_TAB_TRAVERSAL"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormShiftTabTraversal,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHIFT_TAB_TRAVERSAL"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormAutofillAttributes,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_AUTOFILL_ATTRIBUTES"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormA11ySemantics, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_A11Y_SEMANTICS"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormFullI18n, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_I18N_FULL"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormShowError, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_SHOW_ERROR"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormErrorA11y, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_A11Y"),
    flowIpcAutomationFlag(\.flowIpcAutoLoginFormErrorFocus, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetry,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTarget,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetState,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetLayout,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT"),
    flowIpcAutomationFlag(
        \.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability,
        "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY"),
    flowIpcAutomationFlag(\.flowIpcAutoLifecycleFocus, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LIFECYCLE_FOCUS"),
    flowIpcAutomationFlag(\.flowIpcAutoLazyData, "VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_DATA"),
    flowIpcAutomationFlag(\.flowIpcAutoGridScroll, "VAADIN_DESKTOP_FLOW_IPC_AUTO_GRID_SCROLL"),
    flowIpcAutomationFlag(\.flowIpcAutoStreamResource, "VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE"),
    flowIpcAutomationFlag(\.flowIpcAutoStreamResourceRange, "VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_RANGE"),
    flowIpcAutomationFlag(\.flowIpcAutoStreamResourceChunks, "VAADIN_DESKTOP_FLOW_IPC_AUTO_STREAM_RESOURCE_CHUNKS"),
    flowIpcAutomationFlag(\.flowIpcAutoUploadReceiver, "VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER"),
    flowIpcAutomationFlag(\.flowIpcAutoUploadMultipart, "VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_MULTIPART"),
    flowIpcAutomationFlag(\.flowIpcAutoDownloadSaveOpen, "VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN"),
    flowIpcAutomationFlag(\.flowIpcAutoShutdown, "VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN", windowFlags: [])
]

extension HostConfig {
    mutating func applyFlowIpcAutomationEnvironment(_ environment: [String: String]) {
        for definition in flowIpcAutomationFlagDefinitions {
            if definition.environmentNames.contains(where: { environment[$0] == "1" }) {
                self[keyPath: definition.keyPath] = true
            }
        }
    }

    var flowIpcAutomationWindowFlags: [String] {
        var flags: [String] = []
        for definition in flowIpcAutomationFlagDefinitions where self[keyPath: definition.keyPath] {
            flags.append(contentsOf: definition.windowFlagNames)
        }
        return flags
    }
}
