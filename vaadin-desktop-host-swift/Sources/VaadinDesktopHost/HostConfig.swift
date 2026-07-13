import Foundation

struct HostConfig {
    var module: String = "examples/counter-app"
    var repoRoot: String = FileManager.default.currentDirectoryPath
    var mavenRepo: String = ""
    var mavenExecutable: String = "mvn"
    var transport: String = "compatibility-http"
    var flowIpcAutoClick: Bool = false
    var flowIpcAutoResync: Bool = false
    var flowIpcAutoError: Bool = false
    var flowIpcAutoFileDialogCancel: Bool = false
    var flowIpcAutoVaadinTextField: Bool = false
    var flowIpcAutoFrameworkUpdate: Bool = false
    var flowIpcAutoFrameworkUpdateRequested: Bool = false
    var flowIpcAutoComponentMatrix: Bool = false
    var flowIpcAutoDataComponents: Bool = false
    var flowIpcAutoInputComponents: Bool = false
    var flowIpcAutoNavigationComponents: Bool = false
    var flowIpcAutoOverlayComponents: Bool = false
    var flowIpcAutoRichContentComponents: Bool = false
    var flowIpcAutoRichTextEditor: Bool = false
    var flowIpcAutoRichTextEditorBehavior: Bool = false
    var flowIpcAutoRichTextEditorEditing: Bool = false
    var flowIpcAutoRichTextEditorTheme: Bool = false
    var flowIpcAutoNativeClipboardRead: Bool = false
    var flowIpcAutoButtonTheme: Bool = false
    var flowIpcAutoTextFieldTheme: Bool = false
    var flowIpcAutoSelectTheme: Bool = false
    var flowIpcAutoComboBoxTheme: Bool = false
    var flowIpcAutoDatePickerTheme: Bool = false
    var flowIpcAutoMultiSelectComboBoxTheme: Bool = false
    var flowIpcAutoTimePickerTheme: Bool = false
    var flowIpcAutoTextAreaTheme: Bool = false
    var flowIpcAutoEmailFieldTheme: Bool = false
    var flowIpcAutoIntegerFieldTheme: Bool = false
    var flowIpcAutoPasswordFieldTheme: Bool = false
    var flowIpcAutoBigDecimalFieldTheme: Bool = false
    var flowIpcAutoCheckboxTheme: Bool = false
    var flowIpcAutoRadioButtonGroupTheme: Bool = false
    var flowIpcAutoTabsTheme: Bool = false
    var flowIpcAutoDetailsTheme: Bool = false
    var flowIpcAutoMenuBarTheme: Bool = false
    var flowIpcAutoGridTheme: Bool = false
    var flowIpcAutoAvatarTheme: Bool = false
    var flowIpcAutoAvatarGroupTheme: Bool = false
    var flowIpcAutoMessageInputTheme: Bool = false
    var flowIpcAutoSideNavTheme: Bool = false
    var flowIpcAutoMessageListMarkdown: Bool = false
    var flowIpcAutoLoginOverlayProperties: Bool = false
    var flowIpcAutoLoginOverlayForgotPassword: Bool = false
    var flowIpcAutoLoginOverlayLogin: Bool = false
    var flowIpcAutoLoginOverlayInvalidLogin: Bool = false
    var flowIpcAutoLoginOverlayClose: Bool = false
    var flowIpcAutoLoginOverlayRecoveryLogin: Bool = false
    var flowIpcAutoLoginOverlayServerReopen: Bool = false
    var flowIpcAutoLoginOverlayShowError: Bool = false
    var flowIpcAutoLoginOverlayErrorA11y: Bool = false
    var flowIpcAutoLoginOverlayErrorFocus: Bool = false
    var flowIpcAutoLoginOverlayErrorFocusRetry: Bool = false
    var flowIpcAutoLoginOverlayErrorFocusRetryTarget: Bool = false
    var flowIpcAutoLoginOverlayActionPost: Bool = false
    var flowIpcAutoLoginOverlaySubmitButton: Bool = false
    var flowIpcAutoLoginOverlayEnterSubmit: Bool = false
    var flowIpcAutoLoginOverlayFullI18n: Bool = false
    var flowIpcAutoLoginOverlayNoAutofocus: Bool = false
    var flowIpcAutoLoginOverlayAutofocusControl: Bool = false
    var flowIpcAutoLoginOverlayEscClose: Bool = false
    var flowIpcAutoLoginOverlayOutsideClose: Bool = false
    var flowIpcAutoLoginOverlayTabTraversal: Bool = false
    var flowIpcAutoLoginOverlayShiftTabTraversal: Bool = false
    var flowIpcAutoLoginOverlayAutofillAttributes: Bool = false
    var flowIpcAutoLoginOverlayA11ySemantics: Bool = false
    var flowIpcAutoLoginOverlayDisabledGuard: Bool = false
    var flowIpcAutoLoginOverlayDisabledKeyboardGuard: Bool = false
    var flowIpcAutoLoginFormProperties: Bool = false
    var flowIpcAutoLoginFormLogin: Bool = false
    var flowIpcAutoLoginFormInvalidLogin: Bool = false
    var flowIpcAutoLoginFormForgotPassword: Bool = false
    var flowIpcAutoLoginFormActionPost: Bool = false
    var flowIpcAutoLoginFormSubmitButton: Bool = false
    var flowIpcAutoLoginFormEnterSubmit: Bool = false
    var flowIpcAutoLoginFormRecoveryLogin: Bool = false
    var flowIpcAutoLoginFormEnabledLifecycle: Bool = false
    var flowIpcAutoLoginFormDisabledGuard: Bool = false
    var flowIpcAutoLoginFormDisabledKeyboardGuard: Bool = false
    var flowIpcAutoLoginFormNoAutofocus: Bool = false
    var flowIpcAutoLoginFormAutofocusControl: Bool = false
    var flowIpcAutoLoginFormTabTraversal: Bool = false
    var flowIpcAutoLoginFormShiftTabTraversal: Bool = false
    var flowIpcAutoLoginFormAutofillAttributes: Bool = false
    var flowIpcAutoLoginFormA11ySemantics: Bool = false
    var flowIpcAutoLoginFormFullI18n: Bool = false
    var flowIpcAutoLoginFormShowError: Bool = false
    var flowIpcAutoLoginFormErrorA11y: Bool = false
    var flowIpcAutoLoginFormErrorFocus: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetry: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTarget: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetState: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetLayout: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment: Bool = false
    var flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability: Bool = false
    var flowIpcAutoLifecycleFocus: Bool = false
    var flowIpcAutoLazyData: Bool = false
    var flowIpcAutoGridScroll: Bool = false
    var flowIpcAutoStreamResource: Bool = false
    var flowIpcAutoStreamResourceRange: Bool = false
    var flowIpcAutoStreamResourceChunks: Bool = false
    var flowIpcAutoUploadReceiver: Bool = false
    var flowIpcAutoUploadMultipart: Bool = false
    var flowIpcAutoDownloadSaveOpen: Bool = false
    var flowIpcAutoShutdown: Bool = false
    var flowIpcAutoShutdownTimeoutSeconds: Double = 0
    var hostNoActivate: Bool = false
    var title: String = "Vaadin Desktop"
    var width: Double = 1100
    var height: Double = 780

    static func parse(
        arguments commandLineArguments: [String] = Array(CommandLine.arguments.dropFirst()),
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> HostConfig {
        var config = HostConfig()
        var arguments = commandLineArguments.makeIterator()
        while let argument = arguments.next() {
            switch argument {
            case "--module":
                config.module = arguments.next() ?? config.module
            case "--repo-root":
                config.repoRoot = arguments.next() ?? config.repoRoot
            case "--maven-repo":
                config.mavenRepo = arguments.next() ?? config.mavenRepo
            case "--maven-executable":
                config.mavenExecutable = arguments.next() ?? config.mavenExecutable
            case "--transport":
                config.transport = arguments.next() ?? config.transport
            case "--title":
                config.title = arguments.next() ?? config.title
            case "--width":
                config.width = Double(arguments.next() ?? "") ?? config.width
            case "--height":
                config.height = Double(arguments.next() ?? "") ?? config.height
            case "--flow-ipc-auto-login-form-enabled-lifecycle":
                config.flowIpcAutoLoginFormEnabledLifecycle = true
            case "--flow-ipc-auto-login-form-disabled-guard":
                config.flowIpcAutoLoginFormDisabledGuard = true
            case "--flow-ipc-auto-login-form-disabled-keyboard-guard":
                config.flowIpcAutoLoginFormDisabledKeyboardGuard = true
            case "--flow-ipc-auto-login-form-no-autofocus":
                config.flowIpcAutoLoginFormNoAutofocus = true
            case "--flow-ipc-auto-login-form-autofocus-control":
                config.flowIpcAutoLoginFormAutofocusControl = true
            case "--flow-ipc-auto-login-form-tab-traversal":
                config.flowIpcAutoLoginFormTabTraversal = true
            case "--flow-ipc-auto-login-form-shift-tab-traversal":
                config.flowIpcAutoLoginFormShiftTabTraversal = true
            case "--flow-ipc-auto-login-form-autofill-attributes":
                config.flowIpcAutoLoginFormAutofillAttributes = true
            case "--flow-ipc-auto-login-form-a11y-semantics":
                config.flowIpcAutoLoginFormA11ySemantics = true
            case "--flow-ipc-auto-login-form-i18n-full":
                config.flowIpcAutoLoginFormFullI18n = true
            case "--flow-ipc-auto-login-form-show-error":
                config.flowIpcAutoLoginFormShowError = true
            case "--flow-ipc-auto-login-form-error-a11y":
                config.flowIpcAutoLoginFormErrorA11y = true
            case "--flow-ipc-auto-login-form-error-focus":
                config.flowIpcAutoLoginFormErrorFocus = true
            case "--flow-ipc-auto-login-form-error-focus-retry":
                config.flowIpcAutoLoginFormErrorFocusRetry = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target":
                config.flowIpcAutoLoginFormErrorFocusRetryTarget = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-state":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetState = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-layout":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetLayout = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-hidden-source":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-visible-owner":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-visible-owner-focus":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-visible-owner-focus-containment":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment = true
            case "--flow-ipc-auto-login-form-error-focus-retry-target-visible-owner-focus-containment-stability":
                config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability = true
            case "--flow-ipc-auto-login-overlay-error-a11y":
                config.flowIpcAutoLoginOverlayErrorA11y = true
            case "--flow-ipc-auto-login-overlay-error-focus":
                config.flowIpcAutoLoginOverlayErrorFocus = true
            case "--flow-ipc-auto-login-overlay-error-focus-retry":
                config.flowIpcAutoLoginOverlayErrorFocusRetry = true
            case "--flow-ipc-auto-login-overlay-error-focus-retry-target":
                config.flowIpcAutoLoginOverlayErrorFocusRetryTarget = true
            case "--flow-ipc-auto-login-overlay-i18n-full":
                config.flowIpcAutoLoginOverlayFullI18n = true
            case "--flow-ipc-auto-login-overlay-no-autofocus":
                config.flowIpcAutoLoginOverlayNoAutofocus = true
            case "--flow-ipc-auto-login-overlay-autofocus-control":
                config.flowIpcAutoLoginOverlayAutofocusControl = true
            case "--flow-ipc-auto-login-overlay-esc-close":
                config.flowIpcAutoLoginOverlayEscClose = true
            case "--flow-ipc-auto-login-overlay-outside-close":
                config.flowIpcAutoLoginOverlayOutsideClose = true
            case "--flow-ipc-auto-login-overlay-tab-traversal":
                config.flowIpcAutoLoginOverlayTabTraversal = true
            case "--flow-ipc-auto-login-overlay-shift-tab-traversal":
                config.flowIpcAutoLoginOverlayShiftTabTraversal = true
            case "--flow-ipc-auto-login-overlay-autofill-attributes":
                config.flowIpcAutoLoginOverlayAutofillAttributes = true
            case "--flow-ipc-auto-login-overlay-a11y-semantics":
                config.flowIpcAutoLoginOverlayA11ySemantics = true
            case "--flow-ipc-auto-login-overlay-disabled-guard":
                config.flowIpcAutoLoginOverlayDisabledGuard = true
            case "--flow-ipc-auto-login-overlay-disabled-keyboard-guard":
                config.flowIpcAutoLoginOverlayDisabledKeyboardGuard = true
            case "--flow-ipc-auto-login-overlay-action-post":
                config.flowIpcAutoLoginOverlayActionPost = true
            case "--flow-ipc-auto-login-overlay-submit-button":
                config.flowIpcAutoLoginOverlaySubmitButton = true
            case "--flow-ipc-auto-login-overlay-enter-submit":
                config.flowIpcAutoLoginOverlayEnterSubmit = true
            default:
                continue
            }
        }

        if config.mavenRepo.isEmpty {
            config.mavenRepo = "\(config.repoRoot)/.m2/repository"
        }
        if !config.module.hasPrefix("/") {
            config.module = "\(config.repoRoot)/\(config.module)"
        }
        // Real-window acceptance hooks; keep them opt-in and documented in docs/acceptance-tools.md.
        config.applyFlowIpcAutomationEnvironment(environment)
        if config.flowIpcAutoClick {
            config.flowIpcAutoFileDialogCancel = true
            config.flowIpcAutoVaadinTextField = true
        }
        if config.flowIpcAutoLoginFormDisabledKeyboardGuard {
            config.flowIpcAutoLoginFormDisabledGuard = true
        }
        if config.flowIpcAutoLoginFormDisabledGuard {
            config.flowIpcAutoLoginFormEnabledLifecycle = true
        }
        if config.flowIpcAutoLoginFormRecoveryLogin {
            config.flowIpcAutoLoginFormInvalidLogin = true
        }
        if config.flowIpcAutoLoginOverlayServerReopen {
            config.flowIpcAutoLoginOverlayRecoveryLogin = true
        }
        if config.flowIpcAutoLoginOverlayShowError {
            config.flowIpcAutoLoginOverlayProperties = true
        }
        if config.flowIpcAutoLoginOverlayErrorA11y {
            config.flowIpcAutoLoginOverlayShowError = true
            config.flowIpcAutoLoginOverlayProperties = true
        }
        if config.flowIpcAutoLoginOverlayErrorFocusRetryTarget {
            config.flowIpcAutoLoginOverlayErrorFocusRetry = true
        }
        if config.flowIpcAutoLoginOverlayErrorFocusRetry {
            config.flowIpcAutoLoginOverlayErrorFocus = true
        }
        if config.flowIpcAutoLoginOverlayErrorFocus {
            config.flowIpcAutoLoginOverlayShowError = true
            config.flowIpcAutoLoginOverlayProperties = true
        }
        if config.flowIpcAutoLoginFormErrorA11y {
            config.flowIpcAutoLoginFormShowError = true
            config.flowIpcAutoLoginFormProperties = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource {
            config.flowIpcAutoLoginFormErrorFocusRetryTargetLayout = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability {
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment {
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus {
            config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner {
            config.flowIpcAutoLoginFormErrorFocus = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetLayout {
            config.flowIpcAutoLoginFormErrorFocusRetryTargetState = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTargetState {
            config.flowIpcAutoLoginFormErrorFocusRetryTarget = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetryTarget {
            config.flowIpcAutoLoginFormErrorFocusRetry = true
        }
        if config.flowIpcAutoLoginFormErrorFocusRetry {
            config.flowIpcAutoLoginFormErrorFocus = true
        }
        if config.flowIpcAutoLoginFormErrorFocus {
            config.flowIpcAutoLoginFormShowError = true
            config.flowIpcAutoLoginFormProperties = true
        }
        if config.flowIpcAutoLoginOverlayClose || config.flowIpcAutoLoginOverlayRecoveryLogin {
            config.flowIpcAutoLoginOverlayInvalidLogin = true
        }
        if config.flowIpcAutoLoginOverlayForgotPassword || config.flowIpcAutoLoginOverlayLogin || config.flowIpcAutoLoginOverlayInvalidLogin || config.flowIpcAutoLoginOverlayClose || config.flowIpcAutoLoginOverlayRecoveryLogin || config.flowIpcAutoLoginOverlayServerReopen || config.flowIpcAutoLoginOverlayShowError || config.flowIpcAutoLoginOverlayErrorA11y || config.flowIpcAutoLoginOverlayErrorFocus || config.flowIpcAutoLoginOverlayErrorFocusRetry || config.flowIpcAutoLoginOverlayErrorFocusRetryTarget || config.flowIpcAutoLoginOverlayActionPost || config.flowIpcAutoLoginOverlaySubmitButton || config.flowIpcAutoLoginOverlayEnterSubmit || config.flowIpcAutoLoginOverlayFullI18n || config.flowIpcAutoLoginOverlayNoAutofocus || config.flowIpcAutoLoginOverlayAutofocusControl || config.flowIpcAutoLoginOverlayEscClose || config.flowIpcAutoLoginOverlayOutsideClose || config.flowIpcAutoLoginOverlayTabTraversal || config.flowIpcAutoLoginOverlayShiftTabTraversal || config.flowIpcAutoLoginOverlayAutofillAttributes || config.flowIpcAutoLoginOverlayA11ySemantics || config.flowIpcAutoLoginOverlayDisabledGuard || config.flowIpcAutoLoginOverlayDisabledKeyboardGuard {
            config.flowIpcAutoFrameworkUpdate = true
            config.flowIpcAutoLoginOverlayProperties = true
        }
        if config.flowIpcAutoLoginFormLogin || config.flowIpcAutoLoginFormInvalidLogin || config.flowIpcAutoLoginFormForgotPassword || config.flowIpcAutoLoginFormActionPost || config.flowIpcAutoLoginFormSubmitButton || config.flowIpcAutoLoginFormEnterSubmit || config.flowIpcAutoLoginFormRecoveryLogin || config.flowIpcAutoLoginFormEnabledLifecycle || config.flowIpcAutoLoginFormDisabledGuard || config.flowIpcAutoLoginFormDisabledKeyboardGuard || config.flowIpcAutoLoginFormNoAutofocus || config.flowIpcAutoLoginFormAutofocusControl || config.flowIpcAutoLoginFormTabTraversal || config.flowIpcAutoLoginFormShiftTabTraversal || config.flowIpcAutoLoginFormAutofillAttributes || config.flowIpcAutoLoginFormA11ySemantics || config.flowIpcAutoLoginFormFullI18n || config.flowIpcAutoLoginFormShowError || config.flowIpcAutoLoginFormErrorA11y || config.flowIpcAutoLoginFormErrorFocus || config.flowIpcAutoLoginFormErrorFocusRetry || config.flowIpcAutoLoginFormErrorFocusRetryTarget || config.flowIpcAutoLoginFormErrorFocusRetryTargetState || config.flowIpcAutoLoginFormErrorFocusRetryTargetLayout || config.flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability {
            config.flowIpcAutoLoginFormProperties = true
        }
        if config.flowIpcAutoLoginFormProperties || config.flowIpcAutoLoginFormLogin || config.flowIpcAutoLoginFormInvalidLogin || config.flowIpcAutoLoginFormForgotPassword || config.flowIpcAutoLoginFormActionPost || config.flowIpcAutoLoginFormSubmitButton || config.flowIpcAutoLoginFormEnterSubmit || config.flowIpcAutoLoginFormRecoveryLogin || config.flowIpcAutoLoginFormEnabledLifecycle || config.flowIpcAutoLoginFormDisabledGuard || config.flowIpcAutoLoginFormDisabledKeyboardGuard || config.flowIpcAutoLoginFormNoAutofocus || config.flowIpcAutoLoginFormAutofocusControl || config.flowIpcAutoLoginFormTabTraversal || config.flowIpcAutoLoginFormShiftTabTraversal || config.flowIpcAutoLoginFormAutofillAttributes || config.flowIpcAutoLoginFormA11ySemantics || config.flowIpcAutoLoginFormFullI18n || config.flowIpcAutoLoginFormShowError || config.flowIpcAutoLoginFormErrorA11y || config.flowIpcAutoLoginFormErrorFocus || config.flowIpcAutoLoginFormErrorFocusRetry || config.flowIpcAutoLoginFormErrorFocusRetryTarget || config.flowIpcAutoLoginFormErrorFocusRetryTargetState || config.flowIpcAutoLoginFormErrorFocusRetryTargetLayout || config.flowIpcAutoLoginFormErrorFocusRetryTargetHiddenSource || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwner || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocus || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainment || config.flowIpcAutoLoginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStability {
            config.flowIpcAutoFrameworkUpdate = true
        }
        if config.flowIpcAutoRichTextEditorTheme {
            config.flowIpcAutoFrameworkUpdate = true
            config.flowIpcAutoRichTextEditor = true
        }
        if config.flowIpcAutoNativeClipboardRead {
            config.flowIpcAutoFrameworkUpdate = true
            config.flowIpcAutoRichTextEditorEditing = true
        }
        if config.flowIpcAutoRichTextEditorEditing {
            config.flowIpcAutoRichTextEditorBehavior = true
        }
        if config.flowIpcAutoRichTextEditorBehavior {
            config.flowIpcAutoRichTextEditor = true
        }
        if let timeoutText = environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS"],
           let timeout = Double(timeoutText),
           timeout > 0 {
            config.flowIpcAutoShutdownTimeoutSeconds = timeout
        }
        config.hostNoActivate = environment["VAADIN_DESKTOP_HOST_NO_ACTIVATE"] == "1"
        return config
    }
}
