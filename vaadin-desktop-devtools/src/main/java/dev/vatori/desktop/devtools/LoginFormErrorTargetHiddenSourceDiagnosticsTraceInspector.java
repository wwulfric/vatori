package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormErrorTargetHiddenSourceDiagnosticsTraceInspector {
    private LoginFormErrorTargetHiddenSourceDiagnosticsTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean directObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixDirectHiddenObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-direct-control-hidden");
        boolean ancestorObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenAncestorObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-hidden-ancestor");
        boolean directAriaObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixAriaHiddenSelfObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-direct-aria-hidden");
        boolean ariaAncestorObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixAriaHiddenAncestorObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-aria-hidden-ancestor");
        boolean cssDisplayObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixCssDisplayNoneObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-css-display-none");
        boolean cssVisibilityHiddenObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixCssVisibilityHiddenObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-css-visibility-hidden");
        boolean cssVisibilityCollapseObserved =
                containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixCssVisibilityCollapseObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-css-visibility-collapse");
        if (containsLiteralField(line,
                "loginFormErrorFocusRetryTargetHiddenSourceMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-hidden-source-v1_141")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixLayoutComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixUsernameControlObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixUsernameControlTagName", "input")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenMarkerObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixAcceptedBlockerObserved", "true")
                && (directObserved
                        || ancestorObserved
                        || directAriaObserved
                        || ariaAncestorObserved
                        || cssDisplayObserved
                        || cssVisibilityHiddenObserved
                        || cssVisibilityCollapseObserved)
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetHiddenSourceMatrixDomObserved = true;
        }
        if (containsLiteralField(line, "frameworkAppRendered", "true")
                && containsLiteralField(line, "frameworkAppAutoUpdateDone", "true")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixVersion",
                        "flow-ipc-login-form-error-focus-retry-target-hidden-source-v1_141")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixDirectHiddenObserved", "false")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenAncestorObserved", "true")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenAncestorTagName", "main")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenAncestorId", "app")
                && containsStringField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixHiddenSourceClassification",
                        "retry-target-hidden-source-hidden-ancestor")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorFocusRetryTargetHiddenSourceMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorFocusRetryTargetAppRootHiddenAncestorMatrixDomObserved = true;
        }
    }
}
