package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormErrorTargetStateDiagnosticsTraceInspector {
    private LoginFormErrorTargetStateDiagnosticsTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean targetStateObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixFocusabilityClassification",
                        "retry-target-state-username-observed");
        boolean focusableBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixAcceptedBlockerObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameControlConnected", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameControlDisabled", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameControlFocusFunctionObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameControlRectObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixFocusabilityClassification",
                        "retry-target-state-focusable-input-not-focused-in-framework-validation-view");
        boolean notFocusableBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixFocusabilityClassification",
                        "retry-target-state-control-not-focusable-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormErrorFocusRetryTargetStateMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-state-v1_139")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixRetryTargetComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameControlObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixUsernameControlTagName", "input")
                && (targetStateObserved || focusableBlockerObserved || notFocusableBlockerObserved)
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetStateMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetStateMatrixDomObserved = true;
        }
    }
}
