package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormLifecycleGuardTraceInspector {
    private LoginFormLifecycleGuardTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginFormEnabledLifecycleMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormEnabledLifecycleMatrixVersion",
                        "flow-ipc-login-form-enabled-lifecycle-v1_105")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixEnableButtonRendered", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixInitiallyDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixEnableClickSent", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixEnableEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixEnablePatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixEnabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixCredentialsWritten", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixLoginEventSent", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixLoginEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixLoginPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormEnabledLifecycleMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormEnabledLifecycleMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormDisabledGuardMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormDisabledGuardMatrixVersion",
                        "flow-ipc-login-form-disabled-guard-v1_106")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixInitiallyDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixControlsDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixSubmitButtonObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixSubmitButtonDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixCredentialsWriteAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixClickAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixEventRpcSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixStatusUnchangedObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixNoPrematurePatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormDisabledGuardMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormDisabledGuardMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormDisabledKeyboardGuardMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormDisabledKeyboardGuardMatrixVersion",
                        "flow-ipc-login-form-disabled-keyboard-guard-v1_107")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixInitiallyDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixControlsDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixFocusTargetObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixFocusSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixTabKeyAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixEnterKeyAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixEventRpcSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixStatusUnchangedObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixNoPrematurePatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormDisabledKeyboardGuardMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormDisabledKeyboardGuardMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormNoAutofocusMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormNoAutofocusMatrixVersion",
                        "flow-ipc-login-form-no-autofocus-v1_108")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixAttributeObserved", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixAutofocusControlRendered", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixAutofocusControlUsernameObserved", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixInitialFocusSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixManualFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixManualFocusObserved", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormNoAutofocusMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormNoAutofocusMatrixDomObserved = true;
        }
    }
}
