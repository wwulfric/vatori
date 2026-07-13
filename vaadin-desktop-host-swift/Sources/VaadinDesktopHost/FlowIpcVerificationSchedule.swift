import Foundation

struct FlowIpcVerificationSchedule {
    let extendedVerificationEnabled: Bool
    let inspectionDelaysSeconds: [Double]
    let snapshotDelaySeconds: Double
    let autoShutdownTimeoutSeconds: Double

    init(config: HostConfig) {
        extendedVerificationEnabled = Self.requiresExtendedVerification(config)

        var delays = [2.0, 4.5]
        if extendedVerificationEnabled {
            delays += [9.0, 16.0, 24.0, 34.0, 42.0]
        }
        if config.flowIpcAutoFrameworkUpdate {
            delays += [20.0, 28.0, 56.0, 72.0, 96.0, 132.0]
        }
        inspectionDelaysSeconds = delays

        snapshotDelaySeconds = config.flowIpcAutoFrameworkUpdate
            ? 150.0
            : (extendedVerificationEnabled ? 42.6 : 5.0)
        autoShutdownTimeoutSeconds = config.flowIpcAutoShutdownTimeoutSeconds > 0
            ? config.flowIpcAutoShutdownTimeoutSeconds
            : (config.flowIpcAutoFrameworkUpdate ? 180.0 : 120.0)
    }

    private static func requiresExtendedVerification(_ config: HostConfig) -> Bool {
        [
            config.flowIpcAutoFrameworkUpdate,
            config.flowIpcAutoVaadinTextField,
            config.flowIpcAutoResync,
            config.flowIpcAutoError,
            config.flowIpcAutoComponentMatrix,
            config.flowIpcAutoDataComponents,
            config.flowIpcAutoInputComponents,
            config.flowIpcAutoNavigationComponents,
            config.flowIpcAutoOverlayComponents,
            config.flowIpcAutoRichContentComponents,
            config.flowIpcAutoRichTextEditor,
            config.flowIpcAutoRichTextEditorBehavior,
            config.flowIpcAutoRichTextEditorEditing,
            config.flowIpcAutoRichTextEditorTheme,
            config.flowIpcAutoNativeClipboardRead,
            config.flowIpcAutoButtonTheme,
            config.flowIpcAutoTextFieldTheme,
            config.flowIpcAutoSelectTheme,
            config.flowIpcAutoComboBoxTheme,
            config.flowIpcAutoDatePickerTheme,
            config.flowIpcAutoMultiSelectComboBoxTheme,
            config.flowIpcAutoTimePickerTheme,
            config.flowIpcAutoTextAreaTheme,
            config.flowIpcAutoEmailFieldTheme,
            config.flowIpcAutoIntegerFieldTheme,
            config.flowIpcAutoPasswordFieldTheme,
            config.flowIpcAutoBigDecimalFieldTheme,
            config.flowIpcAutoCheckboxTheme,
            config.flowIpcAutoRadioButtonGroupTheme,
            config.flowIpcAutoTabsTheme,
            config.flowIpcAutoDetailsTheme,
            config.flowIpcAutoMenuBarTheme,
            config.flowIpcAutoGridTheme,
            config.flowIpcAutoAvatarTheme,
            config.flowIpcAutoAvatarGroupTheme,
            config.flowIpcAutoMessageInputTheme,
            config.flowIpcAutoSideNavTheme,
            config.flowIpcAutoMessageListMarkdown,
            config.flowIpcAutoLoginOverlayProperties,
            config.flowIpcAutoLoginOverlayForgotPassword,
            config.flowIpcAutoLoginOverlayLogin,
            config.flowIpcAutoLoginOverlayInvalidLogin,
            config.flowIpcAutoLoginOverlayClose,
            config.flowIpcAutoLoginOverlayRecoveryLogin,
            config.flowIpcAutoLoginOverlayServerReopen,
            config.flowIpcAutoLoginOverlayShowError,
            config.flowIpcAutoLoginOverlayErrorA11y,
            config.flowIpcAutoLoginOverlayErrorFocus,
            config.flowIpcAutoLoginOverlayErrorFocusRetry,
            config.flowIpcAutoLoginOverlayErrorFocusRetryTarget,
            config.flowIpcAutoLoginOverlayActionPost,
            config.flowIpcAutoLoginOverlaySubmitButton,
            config.flowIpcAutoLoginOverlayEnterSubmit,
            config.flowIpcAutoLoginOverlayFullI18n,
            config.flowIpcAutoLoginOverlayNoAutofocus,
            config.flowIpcAutoLoginOverlayAutofocusControl,
            config.flowIpcAutoLoginOverlayEscClose,
            config.flowIpcAutoLoginOverlayOutsideClose,
            config.flowIpcAutoLoginOverlayTabTraversal,
            config.flowIpcAutoLoginOverlayShiftTabTraversal,
            config.flowIpcAutoLoginOverlayAutofillAttributes,
            config.flowIpcAutoLoginOverlayA11ySemantics,
            config.flowIpcAutoLoginOverlayDisabledGuard,
            config.flowIpcAutoLoginOverlayDisabledKeyboardGuard,
            config.flowIpcAutoLoginFormProperties,
            config.flowIpcAutoLoginFormLogin,
            config.flowIpcAutoLoginFormInvalidLogin,
            config.flowIpcAutoLoginFormForgotPassword,
            config.flowIpcAutoLoginFormActionPost,
            config.flowIpcAutoLoginFormSubmitButton,
            config.flowIpcAutoLoginFormEnterSubmit,
            config.flowIpcAutoLoginFormRecoveryLogin,
            config.flowIpcAutoLoginFormEnabledLifecycle,
            config.flowIpcAutoLoginFormDisabledGuard,
            config.flowIpcAutoLoginFormDisabledKeyboardGuard,
            config.flowIpcAutoLoginFormNoAutofocus,
            config.flowIpcAutoLoginFormAutofocusControl,
            config.flowIpcAutoLoginFormTabTraversal,
            config.flowIpcAutoLoginFormShiftTabTraversal,
            config.flowIpcAutoLoginFormAutofillAttributes,
            config.flowIpcAutoLoginFormA11ySemantics,
            config.flowIpcAutoLoginFormFullI18n,
            config.flowIpcAutoLoginFormShowError,
            config.flowIpcAutoLoginFormErrorA11y,
            config.flowIpcAutoLoginFormErrorFocus,
            config.flowIpcAutoLoginFormErrorFocusRetry,
            config.flowIpcAutoLoginFormErrorFocusRetryTarget,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetState,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetLayout,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment,
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability,
            config.flowIpcAutoLifecycleFocus,
            config.flowIpcAutoLazyData,
            config.flowIpcAutoGridScroll,
            config.flowIpcAutoStreamResource,
            config.flowIpcAutoStreamResourceRange,
            config.flowIpcAutoStreamResourceChunks,
            config.flowIpcAutoUploadReceiver,
            config.flowIpcAutoUploadMultipart,
            config.flowIpcAutoDownloadSaveOpen
        ].contains(true)
    }
}
