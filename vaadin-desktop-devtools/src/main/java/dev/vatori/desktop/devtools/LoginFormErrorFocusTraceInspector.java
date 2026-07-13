package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormErrorFocusTraceInspector {
    private LoginFormErrorFocusTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginFormErrorFocusObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusMatrixFocusClassification",
                        "error-focus-username-observed");
        boolean loginFormErrorFocusAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusMatrixFocusClassification",
                        "error-focus-username-not-observed-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormErrorFocusMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusMatrixVersion",
                        "flow-ipc-login-form-error-focus-v1_133")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixShowErrorComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixFocusAttempted", "true")
                && (loginFormErrorFocusObserved
                        || loginFormErrorFocusAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusMatrixDomObserved = true;
        }
        boolean loginFormErrorFocusRetryObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryMatrixFocusClassification",
                        "error-focus-retry-username-observed");
        boolean loginFormErrorFocusRetryAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryMatrixFocusClassification",
                        "error-focus-retry-username-not-observed-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormErrorFocusRetryMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-v1_135")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixShowErrorComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixErrorFocusComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixRetryAttempted", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixRetryCountObserved", "true")
                && (loginFormErrorFocusRetryObserved
                        || loginFormErrorFocusRetryAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryMatrixDomObserved = true;
        }
        boolean loginFormErrorFocusRetryTargetObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetMatrixFocusClassification",
                        "retry-target-username-observed");
        boolean loginFormErrorFocusRetryTargetAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetMatrixFocusClassification",
                        "retry-target-active-element-not-username-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormErrorFocusRetryTargetMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-v1_137")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixRetryComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixRetryAttempted", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixRetryCountObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixActiveElementObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixActiveElementTagObserved", "true")
                && (loginFormErrorFocusRetryTargetObserved
                        || loginFormErrorFocusRetryTargetAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetMatrixDomObserved = true;
        }
    }
}
