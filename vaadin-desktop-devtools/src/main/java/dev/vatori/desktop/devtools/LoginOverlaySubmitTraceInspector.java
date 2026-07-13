package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlaySubmitTraceInspector {
    private LoginOverlaySubmitTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginOverlayActionPostMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayActionPostMatrixVersion",
                        "flow-ipc-login-overlay-action-post-v1_116")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixActionObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixMethodObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixSubmitIntercepted", "true")
                && containsStringField(line,
                        "loginOverlayActionPostMatrixSubmitClassification",
                        "no-localhost-browser-post-intercepted")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayActionPostMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayActionPostMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlaySubmitButtonMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlaySubmitButtonMatrixVersion",
                        "flow-ipc-login-overlay-submit-button-v1_117")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixInnerFormObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixSubmitButtonObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixCredentialsWritten", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixClickAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlaySubmitButtonMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlaySubmitButtonMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayEnterSubmitMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayEnterSubmitMatrixVersion",
                        "flow-ipc-login-overlay-enter-submit-v1_118")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixOpenEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixOpenPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixInnerFormObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixPasswordControlObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixCredentialsWritten", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixFocusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixEnterKeyAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayEnterSubmitMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayEnterSubmitMatrixDomObserved = true;
        }
    }
}
