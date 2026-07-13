package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayErrorFocusRetryTraceInspector {
    private LoginOverlayErrorFocusRetryTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginOverlayErrorFocusObserved =
                containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusMatrixFocusClassification",
                        "error-focus-username-observed");
        boolean loginOverlayErrorFocusAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusMatrixFocusClassification",
                        "error-focus-username-not-observed-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayErrorFocusMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusMatrixVersion",
                        "flow-ipc-login-overlay-error-focus-v1_134")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixShowErrorComplete", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixFocusAttempted", "true")
                && (loginOverlayErrorFocusObserved
                        || loginOverlayErrorFocusAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayErrorFocusMatrixDomObserved = true;
        }
        boolean loginOverlayErrorFocusRetryObserved =
                containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusRetryMatrixFocusClassification",
                        "error-focus-retry-username-observed");
        boolean loginOverlayErrorFocusRetryAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusRetryMatrixFocusClassification",
                        "error-focus-retry-username-not-observed-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayErrorFocusRetryMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusRetryMatrixVersion",
                        "flow-ipc-login-overlay-error-focus-retry-v1_136")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixShowErrorComplete", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixErrorFocusComplete", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixRetryAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixRetryCountObserved", "true")
                && (loginOverlayErrorFocusRetryObserved
                        || loginOverlayErrorFocusRetryAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayErrorFocusRetryMatrixDomObserved = true;
        }
        boolean loginOverlayErrorFocusRetryTargetObserved =
                containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixFocusClassification",
                        "retry-target-username-observed");
        boolean loginOverlayErrorFocusRetryTargetAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixFocusClassification",
                        "retry-target-active-element-not-username-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayErrorFocusRetryTargetMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixVersion",
                        "flow-ipc-login-overlay-error-focus-retry-target-v1_138")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixRetryComplete", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixRetryAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixRetryCountObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixActiveElementObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixActiveElementTagObserved", "true")
                && (loginOverlayErrorFocusRetryTargetObserved
                        || loginOverlayErrorFocusRetryTargetAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorFocusRetryTargetMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayErrorFocusRetryTargetMatrixDomObserved = true;
        }
    }
}
