package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.AcceptanceGateExpansion.*;

import java.util.EnumSet;

final class AuthenticationLoginFormCoreAcceptanceGateRequirements {
    private AuthenticationLoginFormCoreAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_97_LOGIN_FORM_LOGIN_EVENT ->
                single(FLOW_IPC_V1_97_LOGIN_FORM_LOGIN_EVENT);
            case FLOW_IPC_V1_97 -> pair(
                    FLOW_IPC_V1_97_LOGIN_FORM_LOGIN_EVENT,
                    FLOW_IPC_V1_97);
            case FLOW_IPC_V1_98_LOGIN_FORM_INVALID_LOGIN_EVENT ->
                single(FLOW_IPC_V1_98_LOGIN_FORM_INVALID_LOGIN_EVENT);
            case FLOW_IPC_V1_98 -> pair(
                    FLOW_IPC_V1_98_LOGIN_FORM_INVALID_LOGIN_EVENT,
                    FLOW_IPC_V1_98);
            case FLOW_IPC_V1_99_LOGIN_FORM_FORGOT_PASSWORD_EVENT ->
                single(FLOW_IPC_V1_99_LOGIN_FORM_FORGOT_PASSWORD_EVENT);
            case FLOW_IPC_V1_99 -> pair(
                    FLOW_IPC_V1_99_LOGIN_FORM_FORGOT_PASSWORD_EVENT,
                    FLOW_IPC_V1_99);
            case FLOW_IPC_V1_100_LOGIN_FORM_PROPERTY_MATRIX ->
                single(FLOW_IPC_V1_100_LOGIN_FORM_PROPERTY_MATRIX);
            case FLOW_IPC_V1_100 -> pair(
                    FLOW_IPC_V1_100_LOGIN_FORM_PROPERTY_MATRIX,
                    FLOW_IPC_V1_100);
            case FLOW_IPC_V1_101_LOGIN_FORM_ACTION_POST ->
                single(FLOW_IPC_V1_101_LOGIN_FORM_ACTION_POST);
            case FLOW_IPC_V1_101 -> pair(
                    FLOW_IPC_V1_101_LOGIN_FORM_ACTION_POST,
                    FLOW_IPC_V1_101);
            case FLOW_IPC_V1_102_LOGIN_FORM_SUBMIT_BUTTON ->
                single(FLOW_IPC_V1_102_LOGIN_FORM_SUBMIT_BUTTON);
            case FLOW_IPC_V1_102 -> pair(
                    FLOW_IPC_V1_102_LOGIN_FORM_SUBMIT_BUTTON,
                    FLOW_IPC_V1_102);
            case FLOW_IPC_V1_103_LOGIN_FORM_ENTER_SUBMIT ->
                single(FLOW_IPC_V1_103_LOGIN_FORM_ENTER_SUBMIT);
            case FLOW_IPC_V1_103 -> pair(
                    FLOW_IPC_V1_103_LOGIN_FORM_ENTER_SUBMIT,
                    FLOW_IPC_V1_103);
            case FLOW_IPC_V1_104_LOGIN_FORM_RECOVERY_LOGIN ->
                single(FLOW_IPC_V1_104_LOGIN_FORM_RECOVERY_LOGIN);
            case FLOW_IPC_V1_104 -> pair(
                    FLOW_IPC_V1_104_LOGIN_FORM_RECOVERY_LOGIN,
                    FLOW_IPC_V1_104);
            case FLOW_IPC_V1_105_LOGIN_FORM_ENABLED_LIFECYCLE ->
                single(FLOW_IPC_V1_105_LOGIN_FORM_ENABLED_LIFECYCLE);
            case FLOW_IPC_V1_105 -> pair(
                    FLOW_IPC_V1_105_LOGIN_FORM_ENABLED_LIFECYCLE,
                    FLOW_IPC_V1_105);
            case FLOW_IPC_V1_106_LOGIN_FORM_DISABLED_GUARD ->
                single(FLOW_IPC_V1_106_LOGIN_FORM_DISABLED_GUARD);
            case FLOW_IPC_V1_106 -> pair(
                    FLOW_IPC_V1_106_LOGIN_FORM_DISABLED_GUARD,
                    FLOW_IPC_V1_106);
            case FLOW_IPC_V1_107_LOGIN_FORM_DISABLED_KEYBOARD_GUARD ->
                single(FLOW_IPC_V1_107_LOGIN_FORM_DISABLED_KEYBOARD_GUARD);
            case FLOW_IPC_V1_107 -> pair(
                    FLOW_IPC_V1_107_LOGIN_FORM_DISABLED_KEYBOARD_GUARD,
                    FLOW_IPC_V1_107);
            case FLOW_IPC_V1_108_LOGIN_FORM_NO_AUTOFOCUS ->
                single(FLOW_IPC_V1_108_LOGIN_FORM_NO_AUTOFOCUS);
            case FLOW_IPC_V1_108 -> pair(
                    FLOW_IPC_V1_108_LOGIN_FORM_NO_AUTOFOCUS,
                    FLOW_IPC_V1_108);
            case FLOW_IPC_V1_109_LOGIN_FORM_I18N_FULL ->
                single(FLOW_IPC_V1_109_LOGIN_FORM_I18N_FULL);
            case FLOW_IPC_V1_109 -> pair(
                    FLOW_IPC_V1_109_LOGIN_FORM_I18N_FULL,
                    FLOW_IPC_V1_109);
            case FLOW_IPC_V1_110_LOGIN_FORM_SHOW_ERROR ->
                single(FLOW_IPC_V1_110_LOGIN_FORM_SHOW_ERROR);
            case FLOW_IPC_V1_110 -> pair(
                    FLOW_IPC_V1_110_LOGIN_FORM_SHOW_ERROR,
                    FLOW_IPC_V1_110);
            default -> throw new IllegalArgumentException(
                    "Unsupported authentication login form core gate: " + gate);
        };
    }
}
