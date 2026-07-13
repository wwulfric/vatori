package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.TraceRequirementChain.cumulative;
import static dev.vatori.desktop.devtools.TraceRequirementChain.entry;

final class AuthenticationOverlayCoreRequirementChecks {
    private static final TraceRequirementChain OVERLAY_CORE_CHAIN = cumulative(
            "authentication overlay core trace gate",
            summary -> true,
            entry(FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX, FLOW_IPC_V1_90,
                    summary -> summary.authentication.loginOverlayPropertyMatrixDomObserved),
            entry(FLOW_IPC_V1_91_LOGIN_OVERLAY_FORGOT_PASSWORD_EVENT, FLOW_IPC_V1_91,
                    summary -> summary.authentication.loginOverlayForgotPasswordEventMatrixDomObserved),
            entry(FLOW_IPC_V1_92_LOGIN_OVERLAY_LOGIN_EVENT, FLOW_IPC_V1_92,
                    summary -> summary.authentication.loginOverlayLoginEventMatrixDomObserved),
            entry(FLOW_IPC_V1_93_LOGIN_OVERLAY_INVALID_LOGIN_EVENT, FLOW_IPC_V1_93,
                    summary -> summary.authentication.loginOverlayInvalidLoginEventMatrixDomObserved),
            entry(FLOW_IPC_V1_94_LOGIN_OVERLAY_CLOSE_LIFECYCLE, FLOW_IPC_V1_94,
                    summary -> summary.authentication.loginOverlayCloseLifecycleMatrixDomObserved),
            entry(FLOW_IPC_V1_95_LOGIN_OVERLAY_RECOVERY_LOGIN, FLOW_IPC_V1_95,
                    summary -> summary.authentication.loginOverlayRecoveryLoginMatrixDomObserved),
            entry(FLOW_IPC_V1_96_LOGIN_OVERLAY_SERVER_REOPEN, FLOW_IPC_V1_96,
                    summary -> summary.authentication.loginOverlayServerReopenMatrixDomObserved));

    private AuthenticationOverlayCoreRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return OVERLAY_CORE_CHAIN.isSatisfied(gate, summary);
    }

    static boolean hasRequiredFlowIpcV1_96Types(TraceSummary summary) {
        return OVERLAY_CORE_CHAIN.isSatisfied(FLOW_IPC_V1_96, summary);
    }
}
