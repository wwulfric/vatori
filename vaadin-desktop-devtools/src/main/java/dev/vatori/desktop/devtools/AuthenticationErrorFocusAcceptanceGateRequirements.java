package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.AcceptanceGateExpansion.*;

import java.util.EnumSet;

final class AuthenticationErrorFocusAcceptanceGateRequirements {
    private AuthenticationErrorFocusAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_129_LOGIN_OVERLAY_A11Y_SEMANTICS ->
                single(FLOW_IPC_V1_129_LOGIN_OVERLAY_A11Y_SEMANTICS);
            case FLOW_IPC_V1_129 -> pair(
                    FLOW_IPC_V1_129_LOGIN_OVERLAY_A11Y_SEMANTICS,
                    FLOW_IPC_V1_129);
            case FLOW_IPC_V1_130_LOGIN_FORM_A11Y_SEMANTICS ->
                single(FLOW_IPC_V1_130_LOGIN_FORM_A11Y_SEMANTICS);
            case FLOW_IPC_V1_130 -> pair(
                    FLOW_IPC_V1_130_LOGIN_FORM_A11Y_SEMANTICS,
                    FLOW_IPC_V1_130);
            case FLOW_IPC_V1_131_LOGIN_OVERLAY_ERROR_A11Y ->
                single(FLOW_IPC_V1_131_LOGIN_OVERLAY_ERROR_A11Y);
            case FLOW_IPC_V1_131 -> pair(
                    FLOW_IPC_V1_131_LOGIN_OVERLAY_ERROR_A11Y,
                    FLOW_IPC_V1_131);
            case FLOW_IPC_V1_132_LOGIN_FORM_ERROR_A11Y ->
                single(FLOW_IPC_V1_132_LOGIN_FORM_ERROR_A11Y);
            case FLOW_IPC_V1_132 -> pair(
                    FLOW_IPC_V1_132_LOGIN_FORM_ERROR_A11Y,
                    FLOW_IPC_V1_132);
            case FLOW_IPC_V1_133_LOGIN_FORM_ERROR_FOCUS ->
                single(FLOW_IPC_V1_133_LOGIN_FORM_ERROR_FOCUS);
            case FLOW_IPC_V1_133 -> pair(
                    FLOW_IPC_V1_133_LOGIN_FORM_ERROR_FOCUS,
                    FLOW_IPC_V1_133);
            case FLOW_IPC_V1_134_LOGIN_OVERLAY_ERROR_FOCUS ->
                single(FLOW_IPC_V1_134_LOGIN_OVERLAY_ERROR_FOCUS);
            case FLOW_IPC_V1_134 -> pair(
                    FLOW_IPC_V1_134_LOGIN_OVERLAY_ERROR_FOCUS,
                    FLOW_IPC_V1_134);
            case FLOW_IPC_V1_135_LOGIN_FORM_ERROR_FOCUS_RETRY ->
                single(FLOW_IPC_V1_135_LOGIN_FORM_ERROR_FOCUS_RETRY);
            case FLOW_IPC_V1_135 -> pair(
                    FLOW_IPC_V1_135_LOGIN_FORM_ERROR_FOCUS_RETRY,
                    FLOW_IPC_V1_135);
            case FLOW_IPC_V1_136_LOGIN_OVERLAY_ERROR_FOCUS_RETRY ->
                single(FLOW_IPC_V1_136_LOGIN_OVERLAY_ERROR_FOCUS_RETRY);
            case FLOW_IPC_V1_136 -> pair(
                    FLOW_IPC_V1_136_LOGIN_OVERLAY_ERROR_FOCUS_RETRY,
                    FLOW_IPC_V1_136);
            case FLOW_IPC_V1_137_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET ->
                single(FLOW_IPC_V1_137_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET);
            case FLOW_IPC_V1_137 -> pair(
                    FLOW_IPC_V1_137_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET,
                    FLOW_IPC_V1_137);
            case FLOW_IPC_V1_138_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET ->
                single(FLOW_IPC_V1_138_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET);
            case FLOW_IPC_V1_138 -> pair(
                    FLOW_IPC_V1_138_LOGIN_OVERLAY_ERROR_FOCUS_RETRY_TARGET,
                    FLOW_IPC_V1_138);
            case FLOW_IPC_V1_139_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE ->
                single(FLOW_IPC_V1_139_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE);
            case FLOW_IPC_V1_139 -> pair(
                    FLOW_IPC_V1_139_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_STATE,
                    FLOW_IPC_V1_139);
            case FLOW_IPC_V1_140_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT ->
                single(FLOW_IPC_V1_140_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT);
            case FLOW_IPC_V1_140 -> pair(
                    FLOW_IPC_V1_140_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_LAYOUT,
                    FLOW_IPC_V1_140);
            case FLOW_IPC_V1_141_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE ->
                single(FLOW_IPC_V1_141_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE);
            case FLOW_IPC_V1_141 -> pair(
                    FLOW_IPC_V1_141_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_HIDDEN_SOURCE,
                    FLOW_IPC_V1_141);
            case FLOW_IPC_V1_142_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_APP_ROOT_HIDDEN_ANCESTOR ->
                single(FLOW_IPC_V1_142_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_APP_ROOT_HIDDEN_ANCESTOR);
            case FLOW_IPC_V1_142 -> pair(
                    FLOW_IPC_V1_142_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_APP_ROOT_HIDDEN_ANCESTOR,
                    FLOW_IPC_V1_142);
            case FLOW_IPC_V1_143_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER ->
                single(FLOW_IPC_V1_143_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER);
            case FLOW_IPC_V1_143 -> pair(
                    FLOW_IPC_V1_143_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER,
                    FLOW_IPC_V1_143);
            case FLOW_IPC_V1_144_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS ->
                single(FLOW_IPC_V1_144_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS);
            case FLOW_IPC_V1_144 -> pair(
                    FLOW_IPC_V1_144_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS,
                    FLOW_IPC_V1_144);
            case FLOW_IPC_V1_145_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT ->
                single(FLOW_IPC_V1_145_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT);
            case FLOW_IPC_V1_145 -> pair(
                    FLOW_IPC_V1_145_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT,
                    FLOW_IPC_V1_145);
            case FLOW_IPC_V1_146_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY ->
                single(FLOW_IPC_V1_146_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY);
            case FLOW_IPC_V1_146 -> pair(
                    FLOW_IPC_V1_146_LOGIN_FORM_ERROR_FOCUS_RETRY_TARGET_VISIBLE_OWNER_FOCUS_CONTAINMENT_STABILITY,
                    FLOW_IPC_V1_146);
            default -> throw new IllegalArgumentException(
                    "Unsupported authentication error focus gate: " + gate);
        };
    }
}
