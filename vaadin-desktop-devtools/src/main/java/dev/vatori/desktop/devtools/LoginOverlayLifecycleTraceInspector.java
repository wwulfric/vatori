package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class LoginOverlayLifecycleTraceInspector {
    private LoginOverlayLifecycleTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line,
                "loginOverlayCloseLifecycleMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayCloseLifecycleMatrixVersion",
                        "flow-ipc-login-overlay-close-lifecycle-v1_94")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixClosedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayCloseLifecycleMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayCloseLifecycleMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayRecoveryLoginMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayRecoveryLoginMatrixVersion",
                        "flow-ipc-login-overlay-recovery-login-v1_95")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixErrorClearedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixClosedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayRecoveryLoginMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayRecoveryLoginMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayServerReopenMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayServerReopenMatrixVersion",
                        "flow-ipc-login-overlay-server-reopen-v1_96")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixButtonRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixOverlayRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayServerReopenMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayServerReopenMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "loginOverlayShowErrorMatrixComplete", "true")
                && containsStringField(line,
                        "loginOverlayShowErrorMatrixVersion",
                        "flow-ipc-login-overlay-show-error-v1_111")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixRendered", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixEventSent", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixEventRpcObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixPatchObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixStatusObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixPasswordLengthObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixErrorObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixOpenedObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixErrorTitleObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixErrorMessageObserved", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "loginOverlayShowErrorMatrixFixtureSubstituted", "false")) {
            summary.authentication.loginOverlayShowErrorMatrixDomObserved = true;
        }
    }
}
