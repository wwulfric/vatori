package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.AcceptanceGateExpansion.*;

import java.util.EnumSet;

final class AuthenticationOverlayCoreAcceptanceGateRequirements {
    private AuthenticationOverlayCoreAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX ->
                single(FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX);
            case FLOW_IPC_V1_90 -> pair(
                    FLOW_IPC_V1_90_LOGIN_OVERLAY_PROPERTY_MATRIX,
                    FLOW_IPC_V1_90);
            case FLOW_IPC_V1_91_LOGIN_OVERLAY_FORGOT_PASSWORD_EVENT ->
                single(FLOW_IPC_V1_91_LOGIN_OVERLAY_FORGOT_PASSWORD_EVENT);
            case FLOW_IPC_V1_91 -> pair(
                    FLOW_IPC_V1_91_LOGIN_OVERLAY_FORGOT_PASSWORD_EVENT,
                    FLOW_IPC_V1_91);
            case FLOW_IPC_V1_92_LOGIN_OVERLAY_LOGIN_EVENT ->
                single(FLOW_IPC_V1_92_LOGIN_OVERLAY_LOGIN_EVENT);
            case FLOW_IPC_V1_92 -> pair(
                    FLOW_IPC_V1_92_LOGIN_OVERLAY_LOGIN_EVENT,
                    FLOW_IPC_V1_92);
            case FLOW_IPC_V1_93_LOGIN_OVERLAY_INVALID_LOGIN_EVENT ->
                single(FLOW_IPC_V1_93_LOGIN_OVERLAY_INVALID_LOGIN_EVENT);
            case FLOW_IPC_V1_93 -> pair(
                    FLOW_IPC_V1_93_LOGIN_OVERLAY_INVALID_LOGIN_EVENT,
                    FLOW_IPC_V1_93);
            case FLOW_IPC_V1_94_LOGIN_OVERLAY_CLOSE_LIFECYCLE ->
                single(FLOW_IPC_V1_94_LOGIN_OVERLAY_CLOSE_LIFECYCLE);
            case FLOW_IPC_V1_94 -> pair(
                    FLOW_IPC_V1_94_LOGIN_OVERLAY_CLOSE_LIFECYCLE,
                    FLOW_IPC_V1_94);
            case FLOW_IPC_V1_95_LOGIN_OVERLAY_RECOVERY_LOGIN ->
                single(FLOW_IPC_V1_95_LOGIN_OVERLAY_RECOVERY_LOGIN);
            case FLOW_IPC_V1_95 -> pair(
                    FLOW_IPC_V1_95_LOGIN_OVERLAY_RECOVERY_LOGIN,
                    FLOW_IPC_V1_95);
            case FLOW_IPC_V1_96_LOGIN_OVERLAY_SERVER_REOPEN ->
                single(FLOW_IPC_V1_96_LOGIN_OVERLAY_SERVER_REOPEN);
            case FLOW_IPC_V1_96 -> pair(
                    FLOW_IPC_V1_96_LOGIN_OVERLAY_SERVER_REOPEN,
                    FLOW_IPC_V1_96);
            default -> throw new IllegalArgumentException(
                    "Unsupported authentication overlay core gate: " + gate);
        };
    }
}
