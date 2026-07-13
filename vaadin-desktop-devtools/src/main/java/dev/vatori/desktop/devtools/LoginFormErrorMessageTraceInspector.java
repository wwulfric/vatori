package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginFormErrorMessageTraceInspector {
    private LoginFormErrorMessageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginFormFullI18nMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormFullI18nMatrixVersion",
                        "flow-ipc-login-form-i18n-full-v1_109")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixHeaderNotApplicableObserved", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixFormObserved", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixAdditionalInformationObserved", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixForgotPasswordVisibleObserved", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormFullI18nMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormFullI18nMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginFormShowErrorMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormShowErrorMatrixVersion",
                        "flow-ipc-login-form-show-error-v1_110")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixErrorTitleObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormShowErrorMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormShowErrorMatrixDomObserved = true;
        }
        boolean loginFormErrorA11yObserved =
                containsLiteralField(line,
                        "loginFormErrorA11yMatrixAlertRoleObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixAriaLiveObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixErrorTitleObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixAnnouncementTextObserved", "true")
                && containsStringField(line,
                        "loginFormErrorA11yMatrixA11yClassification",
                        "error-announcement-a11y-observed");
        boolean loginFormErrorA11yAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginFormErrorA11yMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginFormErrorA11yMatrixA11yClassification",
                        "error-announcement-a11y-incomplete-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginFormErrorA11yMatrixComplete", "true")
                && containsStringField(line,
                        "loginFormErrorA11yMatrixVersion",
                        "flow-ipc-login-form-error-a11y-v1_132")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixShowErrorComplete", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixErrorObserved", "true")
                && (loginFormErrorA11yObserved
                        || loginFormErrorA11yAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginFormErrorA11yMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginFormErrorA11yMatrixDomObserved = true;
        }
    }
}
