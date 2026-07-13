package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayInitialFocusTraceInspector {
    private LoginOverlayInitialFocusTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginOverlayNoAutofocusMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayNoAutofocusMatrixVersion",
                        "flow-ipc-login-overlay-no-autofocus-v1_113")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixOpenClickSent", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixAttributeObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixUsernameControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixInitialFocusSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixManualFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixManualFocusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayNoAutofocusMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayNoAutofocusMatrixDomObserved = true;
        }
        boolean loginOverlayAutofocusControlDefaultFocusObserved =
                containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixDefaultFocusObserved", "true");
        boolean loginOverlayAutofocusControlAcceptedBlockerObserved =
                containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixAcceptedBlockerObserved", "true")
                && containsStringField(line,
                        "loginOverlayAutofocusControlMatrixFocusClassification",
                        "default-focus-not-observed-in-framework-validation-view");
        if (containsLiteralField(line,
                "loginOverlayAutofocusControlMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayAutofocusControlMatrixVersion",
                        "flow-ipc-login-overlay-autofocus-control-v1_120")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixUsernameControlObserved", "true")
                && (loginOverlayAutofocusControlDefaultFocusObserved
                        || loginOverlayAutofocusControlAcceptedBlockerObserved)
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayAutofocusControlMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayAutofocusControlMatrixDomObserved = true;
        }
    }
}
