package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormPropertyEventTraceInspector {
    private LoginFormPropertyEventTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginFormPropertyMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormPropertyMatrixVersion",
                        "flow-ipc-login-form-property-matrix-v1_100")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixI18nFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixForgotPasswordVisibleObserved", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixEnabledObserved", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixErrorClearedObserved", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormPropertyMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormPropertyMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormLoginEventMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormLoginEventMatrixVersion",
                        "flow-ipc-login-form-login-event-v1_97")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormLoginEventMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormLoginEventMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormInvalidLoginEventMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormInvalidLoginEventMatrixVersion",
                        "flow-ipc-login-form-invalid-login-event-v1_98")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormInvalidLoginEventMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormInvalidLoginEventMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormForgotPasswordEventMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormForgotPasswordEventMatrixVersion",
                        "flow-ipc-login-form-forgot-password-event-v1_99")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormForgotPasswordEventMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormForgotPasswordEventMatrixDomObserved = true;
        }
    }
}
