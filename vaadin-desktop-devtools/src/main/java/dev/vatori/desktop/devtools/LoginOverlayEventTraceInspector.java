package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayEventTraceInspector {
    private LoginOverlayEventTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginOverlayForgotPasswordEventMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayForgotPasswordEventMatrixVersion",
                        "flow-ipc-login-overlay-forgot-password-event-v1_91")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayForgotPasswordEventMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayForgotPasswordEventMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayLoginEventMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayLoginEventMatrixVersion",
                        "flow-ipc-login-overlay-login-event-v1_92")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayLoginEventMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayLoginEventMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayInvalidLoginEventMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayInvalidLoginEventMatrixVersion",
                        "flow-ipc-login-overlay-invalid-login-event-v1_93")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayInvalidLoginEventMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayInvalidLoginEventMatrixDomObserved = true;
        }
    }
}
