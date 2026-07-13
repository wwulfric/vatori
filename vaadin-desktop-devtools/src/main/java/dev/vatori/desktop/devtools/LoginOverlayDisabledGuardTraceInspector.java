package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayDisabledGuardTraceInspector {
    private LoginOverlayDisabledGuardTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginOverlayDisabledGuardMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayDisabledGuardMatrixVersion",
                        "flow-ipc-login-overlay-disabled-guard-v1_114")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixOpenClickSent", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixControlsDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixSubmitButtonObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixSubmitButtonDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixCredentialsWriteAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixClickAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixLoginEventAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixEventRpcSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixStatusUnchangedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixNoPrematurePatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixUnexpectedLoginRpcObserved", "false")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledGuardMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayDisabledGuardMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayDisabledKeyboardGuardMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixVersion",
                        "flow-ipc-login-overlay-disabled-keyboard-guard-v1_115")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixOpenTriggerObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixOpenClickSent", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixOpenEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixControlsDisabledObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixFocusTargetObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixFocusAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixFocusSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixTabKeyAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixEnterKeyAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixSubmitAttempted", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixEventRpcSuppressedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixStatusUnchangedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixNoPrematurePatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixUnexpectedLoginRpcObserved", "false")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayDisabledKeyboardGuardMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayDisabledKeyboardGuardMatrixDomObserved = true;
        }
    }
}
