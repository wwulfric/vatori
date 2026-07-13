package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormSubmitRecoveryTraceInspector {
    private LoginFormSubmitRecoveryTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginFormActionPostMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormActionPostMatrixVersion",
                        "flow-ipc-login-form-action-post-v1_101")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixActionObserved", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixMethodObserved", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixSubmitIntercepted", "true")
                && containsStringField(line,
                        "loginFormActionPostMatrixSubmitClassification",
                        "no-localhost-browser-post-intercepted")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormActionPostMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormActionPostMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormSubmitButtonMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormSubmitButtonMatrixVersion",
                        "flow-ipc-login-form-submit-button-v1_102")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixInnerFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixSubmitButtonObserved", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixCredentialsWritten", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixClickAttempted", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormSubmitButtonMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormSubmitButtonMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormEnterSubmitMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormEnterSubmitMatrixVersion",
                        "flow-ipc-login-form-enter-submit-v1_103")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixInnerFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixCredentialsWritten", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixFocusObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixEnterKeyAttempted", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormEnterSubmitMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormEnterSubmitMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormRecoveryLoginMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormRecoveryLoginMatrixVersion",
                        "flow-ipc-login-form-recovery-login-v1_104")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixErrorClearedObserved", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormRecoveryLoginMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormRecoveryLoginMatrixDomObserved = true;
        }
    }
}
