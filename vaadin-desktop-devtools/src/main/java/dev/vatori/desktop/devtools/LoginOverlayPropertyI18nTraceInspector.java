package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayPropertyI18nTraceInspector {
    private LoginOverlayPropertyI18nTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginOverlayPropertyMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayPropertyMatrixVersion",
                        "flow-ipc-login-overlay-property-matrix-v1_90")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixTitleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixDescriptionObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixI18nFormObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixForgotPasswordVisibleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayPropertyMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayPropertyMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayFullI18nMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayFullI18nMatrixVersion",
                        "flow-ipc-login-overlay-i18n-full-v1_112")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixHeaderObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixFormObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixAdditionalInformationObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixForgotPasswordVisibleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayFullI18nMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayFullI18nMatrixDomObserved = true;
        }
    }
}
