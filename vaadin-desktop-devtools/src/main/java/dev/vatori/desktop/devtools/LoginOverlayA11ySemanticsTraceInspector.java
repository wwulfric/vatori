package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayA11ySemanticsTraceInspector {
    private LoginOverlayA11ySemanticsTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        boolean loginOverlayA11ySemanticsObserved =
                containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixDialogRoleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixModalAttributeObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixTitleTextObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixUsernameLabelObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixPasswordLabelObserved", "true")
                && containsStringField(line,
                        "loginOverlayA11ySemanticsMatrixA11yClassification",
                        "modal-a11y-semantics-observed");
        boolean loginOverlayA11ySemanticsAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayA11ySemanticsMatrixA11yClassification",
                        "modal-a11y-semantics-incomplete-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayA11ySemanticsMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayA11ySemanticsMatrixVersion",
                        "flow-ipc-login-overlay-a11y-semantics-v1_129")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixOpenStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixPasswordControlObserved", "true")
                && (loginOverlayA11ySemanticsObserved
                        || loginOverlayA11ySemanticsAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayA11ySemanticsMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayA11ySemanticsMatrixDomObserved = true;
        }
        boolean loginOverlayErrorA11yObserved =
                containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixAlertRoleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixAriaLiveObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixErrorTitleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixAnnouncementTextObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorA11yMatrixA11yClassification",
                        "error-announcement-a11y-observed");
        boolean loginOverlayErrorA11yAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayErrorA11yMatrixA11yClassification",
                        "error-announcement-a11y-incomplete-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayErrorA11yMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayErrorA11yMatrixVersion",
                        "flow-ipc-login-overlay-error-a11y-v1_131")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixShowErrorComplete", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixErrorObserved", "true")
                && (loginOverlayErrorA11yObserved
                        || loginOverlayErrorA11yAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayErrorA11yMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayErrorA11yMatrixDomObserved = true;
        }
    }
}
