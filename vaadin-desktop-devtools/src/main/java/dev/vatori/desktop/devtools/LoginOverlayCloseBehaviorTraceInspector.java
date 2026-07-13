package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayCloseBehaviorTraceInspector {
    private LoginOverlayCloseBehaviorTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginOverlayEscCloseObserved =
                containsLiteralField(line,
                        "loginOverlayEscCloseMatrixCloseEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixClosePatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixClosedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixCloseStatusObserved", "true")
                && containsStringField(line,
                        "loginOverlayEscCloseMatrixCloseClassification",
                        "esc-close-observed");
        boolean loginOverlayEscCloseAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayEscCloseMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayEscCloseMatrixCloseClassification",
                        "synthetic-escape-did-not-close-overlay-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayEscCloseMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayEscCloseMatrixVersion",
                        "flow-ipc-login-overlay-esc-close-v1_121")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixOpenStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixEscKeyAttempted", "true")
                && (loginOverlayEscCloseObserved
                        || loginOverlayEscCloseAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayEscCloseMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayEscCloseMatrixDomObserved = true;
        }
        boolean loginOverlayOutsideCloseObserved =
                containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixCloseEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixClosePatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixClosedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixCloseStatusObserved", "true")
                && containsStringField(line,
                        "loginOverlayOutsideCloseMatrixCloseClassification",
                        "outside-click-close-observed");
        boolean loginOverlayOutsideCloseAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayOutsideCloseMatrixCloseClassification",
                        "synthetic-outside-click-did-not-close-overlay-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayOutsideCloseMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayOutsideCloseMatrixVersion",
                        "flow-ipc-login-overlay-outside-click-close-v1_122")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixOpenStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixClickAttempted", "true")
                && (loginOverlayOutsideCloseObserved
                        || loginOverlayOutsideCloseAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayOutsideCloseMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayOutsideCloseMatrixDomObserved = true;
        }
    }
}
