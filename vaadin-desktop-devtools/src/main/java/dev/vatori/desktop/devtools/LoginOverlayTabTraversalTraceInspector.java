package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayTabTraversalTraceInspector {
    private LoginOverlayTabTraversalTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginOverlayTabTraversalObserved =
                containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixPasswordFocusObserved", "true")
                && containsStringField(line,
                        "loginOverlayTabTraversalMatrixFocusClassification",
                        "tab-focus-password-observed");
        boolean loginOverlayTabTraversalAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayTabTraversalMatrixFocusClassification",
                        "synthetic-tab-did-not-move-focus-to-password-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayTabTraversalMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayTabTraversalMatrixVersion",
                        "flow-ipc-login-overlay-tab-traversal-v1_123")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixOpenStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixUsernameFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixTabKeyAttempted", "true")
                && (loginOverlayTabTraversalObserved
                        || loginOverlayTabTraversalAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayTabTraversalMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayTabTraversalMatrixDomObserved = true;
        }
        boolean loginOverlayShiftTabTraversalObserved =
                containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixUsernameFocusObserved", "true")
                && containsStringField(line,
                        "loginOverlayShiftTabTraversalMatrixFocusClassification",
                        "shift-tab-focus-username-observed");
        boolean loginOverlayShiftTabTraversalAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayShiftTabTraversalMatrixFocusClassification",
                        "synthetic-shift-tab-did-not-move-focus-to-username-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayShiftTabTraversalMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayShiftTabTraversalMatrixVersion",
                        "flow-ipc-login-overlay-shift-tab-traversal-v1_124")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixOpenStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixPasswordFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixShiftTabKeyAttempted", "true")
                && (loginOverlayShiftTabTraversalObserved
                        || loginOverlayShiftTabTraversalAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayShiftTabTraversalMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayShiftTabTraversalMatrixDomObserved = true;
        }
    }
}
