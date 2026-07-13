package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;

final class AuthenticationTraceRequirementChecks {
    private AuthenticationTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        if (AcceptanceGateMilestones.between(
                gate, FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX, FLOW_IPC_V1_96)) {
            return AuthenticationOverlayCoreRequirementChecks.isSatisfied(gate, summary);
        }
        if (AcceptanceGateMilestones.between(
                gate, FLOW_IPC_V1_97_LOGIN_FORM_LOGIN_EVENT, FLOW_IPC_V1_110)) {
            return AuthenticationLoginFormCoreRequirementChecks.isSatisfied(gate, summary);
        }
        if (AcceptanceGateMilestones.between(
                gate, FLOW_IPC_V1_111_LOGIN_OVERLAY_SHOW_ERROR, FLOW_IPC_V1_128)) {
            return AuthenticationOverlayBehaviorRequirementChecks.isSatisfied(gate, summary);
        }
        if (AcceptanceGateMilestones.between(
                gate, FLOW_IPC_V1_129_LOGIN_OVERLAY_A11Y_SEMANTICS, FLOW_IPC_V1_146)) {
            return AuthenticationErrorFocusRequirementChecks.isSatisfied(gate, summary);
        }
        throw new IllegalArgumentException("Unsupported authentication trace gate: " + gate);
    }
}
