package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.AcceptanceGateExpansion.*;

import java.util.EnumSet;

final class AuthenticationOverlayBehaviorAcceptanceGateRequirements {
    private AuthenticationOverlayBehaviorAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_111_LOGIN_OVERLAY_SHOW_ERROR ->
                single(FLOW_IPC_V1_111_LOGIN_OVERLAY_SHOW_ERROR);
            case FLOW_IPC_V1_111 -> pair(
                    FLOW_IPC_V1_111_LOGIN_OVERLAY_SHOW_ERROR,
                    FLOW_IPC_V1_111);
            case FLOW_IPC_V1_112_LOGIN_OVERLAY_I18N_FULL ->
                single(FLOW_IPC_V1_112_LOGIN_OVERLAY_I18N_FULL);
            case FLOW_IPC_V1_112 -> pair(
                    FLOW_IPC_V1_112_LOGIN_OVERLAY_I18N_FULL,
                    FLOW_IPC_V1_112);
            case FLOW_IPC_V1_113_LOGIN_OVERLAY_NO_AUTOFOCUS ->
                single(FLOW_IPC_V1_113_LOGIN_OVERLAY_NO_AUTOFOCUS);
            case FLOW_IPC_V1_113 -> pair(
                    FLOW_IPC_V1_113_LOGIN_OVERLAY_NO_AUTOFOCUS,
                    FLOW_IPC_V1_113);
            case FLOW_IPC_V1_114_LOGIN_OVERLAY_DISABLED_GUARD ->
                single(FLOW_IPC_V1_114_LOGIN_OVERLAY_DISABLED_GUARD);
            case FLOW_IPC_V1_114 -> pair(
                    FLOW_IPC_V1_114_LOGIN_OVERLAY_DISABLED_GUARD,
                    FLOW_IPC_V1_114);
            case FLOW_IPC_V1_115_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD ->
                single(FLOW_IPC_V1_115_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD);
            case FLOW_IPC_V1_115 -> pair(
                    FLOW_IPC_V1_115_LOGIN_OVERLAY_DISABLED_KEYBOARD_GUARD,
                    FLOW_IPC_V1_115);
            case FLOW_IPC_V1_116_LOGIN_OVERLAY_ACTION_POST ->
                single(FLOW_IPC_V1_116_LOGIN_OVERLAY_ACTION_POST);
            case FLOW_IPC_V1_116 -> pair(
                    FLOW_IPC_V1_116_LOGIN_OVERLAY_ACTION_POST,
                    FLOW_IPC_V1_116);
            case FLOW_IPC_V1_117_LOGIN_OVERLAY_SUBMIT_BUTTON ->
                single(FLOW_IPC_V1_117_LOGIN_OVERLAY_SUBMIT_BUTTON);
            case FLOW_IPC_V1_117 -> pair(
                    FLOW_IPC_V1_117_LOGIN_OVERLAY_SUBMIT_BUTTON,
                    FLOW_IPC_V1_117);
            case FLOW_IPC_V1_118_LOGIN_OVERLAY_ENTER_SUBMIT ->
                single(FLOW_IPC_V1_118_LOGIN_OVERLAY_ENTER_SUBMIT);
            case FLOW_IPC_V1_118 -> pair(
                    FLOW_IPC_V1_118_LOGIN_OVERLAY_ENTER_SUBMIT,
                    FLOW_IPC_V1_118);
            case FLOW_IPC_V1_119_LOGIN_FORM_AUTOFOCUS_CONTROL ->
                single(FLOW_IPC_V1_119_LOGIN_FORM_AUTOFOCUS_CONTROL);
            case FLOW_IPC_V1_119 -> pair(
                    FLOW_IPC_V1_119_LOGIN_FORM_AUTOFOCUS_CONTROL,
                    FLOW_IPC_V1_119);
            case FLOW_IPC_V1_120_LOGIN_OVERLAY_AUTOFOCUS_CONTROL ->
                single(FLOW_IPC_V1_120_LOGIN_OVERLAY_AUTOFOCUS_CONTROL);
            case FLOW_IPC_V1_120 -> pair(
                    FLOW_IPC_V1_120_LOGIN_OVERLAY_AUTOFOCUS_CONTROL,
                    FLOW_IPC_V1_120);
            case FLOW_IPC_V1_121_LOGIN_OVERLAY_ESC_CLOSE ->
                single(FLOW_IPC_V1_121_LOGIN_OVERLAY_ESC_CLOSE);
            case FLOW_IPC_V1_121 -> pair(
                    FLOW_IPC_V1_121_LOGIN_OVERLAY_ESC_CLOSE,
                    FLOW_IPC_V1_121);
            case FLOW_IPC_V1_122_LOGIN_OVERLAY_OUTSIDE_CLOSE ->
                single(FLOW_IPC_V1_122_LOGIN_OVERLAY_OUTSIDE_CLOSE);
            case FLOW_IPC_V1_122 -> pair(
                    FLOW_IPC_V1_122_LOGIN_OVERLAY_OUTSIDE_CLOSE,
                    FLOW_IPC_V1_122);
            case FLOW_IPC_V1_123_LOGIN_OVERLAY_TAB_TRAVERSAL ->
                single(FLOW_IPC_V1_123_LOGIN_OVERLAY_TAB_TRAVERSAL);
            case FLOW_IPC_V1_123 -> pair(
                    FLOW_IPC_V1_123_LOGIN_OVERLAY_TAB_TRAVERSAL,
                    FLOW_IPC_V1_123);
            case FLOW_IPC_V1_124_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL ->
                single(FLOW_IPC_V1_124_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL);
            case FLOW_IPC_V1_124 -> pair(
                    FLOW_IPC_V1_124_LOGIN_OVERLAY_SHIFT_TAB_TRAVERSAL,
                    FLOW_IPC_V1_124);
            case FLOW_IPC_V1_125_LOGIN_FORM_TAB_TRAVERSAL ->
                single(FLOW_IPC_V1_125_LOGIN_FORM_TAB_TRAVERSAL);
            case FLOW_IPC_V1_125 -> pair(
                    FLOW_IPC_V1_125_LOGIN_FORM_TAB_TRAVERSAL,
                    FLOW_IPC_V1_125);
            case FLOW_IPC_V1_126_LOGIN_FORM_SHIFT_TAB_TRAVERSAL ->
                single(FLOW_IPC_V1_126_LOGIN_FORM_SHIFT_TAB_TRAVERSAL);
            case FLOW_IPC_V1_126 -> pair(
                    FLOW_IPC_V1_126_LOGIN_FORM_SHIFT_TAB_TRAVERSAL,
                    FLOW_IPC_V1_126);
            case FLOW_IPC_V1_127_LOGIN_FORM_AUTOFILL_ATTRIBUTES ->
                single(FLOW_IPC_V1_127_LOGIN_FORM_AUTOFILL_ATTRIBUTES);
            case FLOW_IPC_V1_127 -> pair(
                    FLOW_IPC_V1_127_LOGIN_FORM_AUTOFILL_ATTRIBUTES,
                    FLOW_IPC_V1_127);
            case FLOW_IPC_V1_128_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES ->
                single(FLOW_IPC_V1_128_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES);
            case FLOW_IPC_V1_128 -> pair(
                    FLOW_IPC_V1_128_LOGIN_OVERLAY_AUTOFILL_ATTRIBUTES,
                    FLOW_IPC_V1_128);
            default -> throw new IllegalArgumentException(
                    "Unsupported authentication overlay behavior gate: " + gate);
        };
    }
}
