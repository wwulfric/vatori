package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormErrorTargetLayoutDiagnosticsTraceInspector {
    private LoginFormErrorTargetLayoutDiagnosticsTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean usernameRectObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlRectObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLayoutClassification",
                        "retry-target-layout-username-rect-observed");
        boolean hiddenBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixAcceptedBlockerObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlRectObserved", "false")
                && (containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlHiddenObserved", "true")
                        || containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlAriaHiddenObserved", "true")
                        || containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlDisplay", "none")
                        || containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlVisibility", "hidden")
                        || containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlVisibility", "collapse"))
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLayoutClassification",
                        "retry-target-layout-control-hidden-in-framework-validation-view");
        boolean formVisibleZeroRectBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixAcceptedBlockerObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLoginFormRectObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlRectObserved", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlHiddenObserved", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlAriaHiddenObserved", "false")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLayoutClassification",
                        "retry-target-layout-form-visible-username-zero-rect-in-framework-validation-view");
        boolean formZeroRectBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixAcceptedBlockerObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLoginFormRectObserved", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlRectObserved", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlHiddenObserved", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlAriaHiddenObserved", "false")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLayoutClassification",
                        "retry-target-layout-form-zero-rect-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormErrorFocusRetryTargetLayoutMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-layout-v1_140")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixTargetStateComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixLoginFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixUsernameControlObserved", "true")
                && (usernameRectObserved
                        || hiddenBlockerObserved
                        || formVisibleZeroRectBlockerObserved
                        || formZeroRectBlockerObserved)
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetLayoutMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetLayoutMatrixDomObserved = true;
        }
    }
}
