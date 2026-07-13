package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.TraceRequirementChain.cumulative;
import static dev.vatori.desktop.devtools.TraceRequirementChain.entry;

final class AuthenticationErrorFocusRequirementChecks {
    private static final TraceRequirementChain ERROR_FOCUS_CHAIN = cumulative(
            "authentication error/focus trace gate",
            AuthenticationOverlayBehaviorRequirementChecks::hasRequiredFlowIpcV1_128Types,
            entry(FLOW_IPC_V1_129_LOGIN_OVERLAY_A11Y_SEMANTICS, FLOW_IPC_V1_129,
                    summary -> summary.authentication.loginOverlayA11ySemanticsMatrixDomObserved),
            entry(FLOW_IPC_V1_130_LOGIN_FORM_A11Y_SEMANTICS, FLOW_IPC_V1_130,
                    summary -> summary.authentication.loginFormA11ySemanticsMatrixDomObserved),
            entry(FLOW_IPC_V1_131_LOGIN_OVERLAY_ERROR_A11Y, FLOW_IPC_V1_131,
                    summary -> summary.authentication.loginOverlayErrorA11yMatrixDomObserved),
            entry(FLOW_IPC_V1_132_LOGIN_FORM_ERROR_A11Y, FLOW_IPC_V1_132,
                    summary -> summary.authentication.loginFormErrorA11yMatrixDomObserved),
            entry(FLOW_IPC_V1_133_LOGIN_FORM_ERROR_FOCUS, FLOW_IPC_V1_133,
                    summary -> summary.authentication.loginFormErrorFocusMatrixDomObserved),
            entry(FLOW_IPC_V1_134_LOGIN_OVERLAY_ERROR_FOCUS, FLOW_IPC_V1_134,
                    summary -> summary.authentication.loginOverlayErrorFocusMatrixDomObserved),
            entry(FLOW_IPC_V1_135_LOGIN_FORM_ERROR_FOCUS_RETRY, FLOW_IPC_V1_135,
                    summary -> summary.authentication.loginFormErrorFocusRetryMatrixDomObserved),
            entry(FLOW_IPC_V1_136_LOGIN_OVERLAY_ERROR_FOCUS_RETRY, FLOW_IPC_V1_136,
                    summary -> summary.authentication.loginOverlayErrorFocusRetryMatrixDomObserved),
            entry(FLOW_IPC_V1_137_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET, FLOW_IPC_V1_137,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetMatrixDomObserved),
            entry(FLOW_IPC_V1_138_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET, FLOW_IPC_V1_138,
                    summary -> summary.authentication.loginOverlayErrorFocusRetryTargetMatrixDomObserved),
            entry(FLOW_IPC_V1_139_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE, FLOW_IPC_V1_139,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetStateMatrixDomObserved),
            entry(FLOW_IPC_V1_140_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT, FLOW_IPC_V1_140,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetLayoutMatrixDomObserved),
            entry(FLOW_IPC_V1_141_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE, FLOW_IPC_V1_141,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetHiddenSourceMatrixDomObserved),
            entry(FLOW_IPC_V1_142_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_APP_ROOT_HIDDEN_ANCESTOR, FLOW_IPC_V1_142,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetAppRootHiddenAncestorMatrixDomObserved),
            entry(FLOW_IPC_V1_143_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER, FLOW_IPC_V1_143,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerMatrixDomObserved),
            entry(FLOW_IPC_V1_144_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS, FLOW_IPC_V1_144,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrixDomObserved),
            entry(FLOW_IPC_V1_145_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT, FLOW_IPC_V1_145,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrixDomObserved),
            entry(FLOW_IPC_V1_146_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY,
                    FLOW_IPC_V1_146,
                    summary -> summary.authentication.loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrixDomObserved));

    private AuthenticationErrorFocusRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return ERROR_FOCUS_CHAIN.isSatisfied(gate, summary);
    }
}
