package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.AcceptanceGateExpansion.*;

import java.util.EnumSet;

final class ThemeMatrixAcceptanceGateRequirements {
    private ThemeMatrixAcceptanceGateRequirements() {
    }

    static EnumSet<AcceptanceGate> expand(AcceptanceGate gate) {
        return switch (gate) {
            case FLOW_IPC_V1_66_RICH_TEXT_EDITOR_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_66_RICH_TEXT_EDITOR_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_66 -> pair(
                    FLOW_IPC_V1_66_RICH_TEXT_EDITOR_THEME_VARIANT_MATRIX, FLOW_IPC_V1_66);
            case FLOW_IPC_V1_67_BUTTON_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_67_BUTTON_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_67 -> pair(
                    FLOW_IPC_V1_67_BUTTON_THEME_VARIANT_MATRIX, FLOW_IPC_V1_67);
            case FLOW_IPC_V1_68_TEXT_FIELD_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_68_TEXT_FIELD_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_68 -> pair(
                    FLOW_IPC_V1_68_TEXT_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_68);
            case FLOW_IPC_V1_69_SELECT_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_69_SELECT_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_69 -> pair(
                    FLOW_IPC_V1_69_SELECT_THEME_VARIANT_MATRIX, FLOW_IPC_V1_69);
            case FLOW_IPC_V1_70_COMBO_BOX_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_70_COMBO_BOX_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_70 -> pair(
                    FLOW_IPC_V1_70_COMBO_BOX_THEME_VARIANT_MATRIX, FLOW_IPC_V1_70);
            case FLOW_IPC_V1_71_DATE_PICKER_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_71_DATE_PICKER_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_71 -> pair(
                    FLOW_IPC_V1_71_DATE_PICKER_THEME_VARIANT_MATRIX, FLOW_IPC_V1_71);
            case FLOW_IPC_V1_72_MULTI_SELECT_COMBO_BOX_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_72_MULTI_SELECT_COMBO_BOX_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_72 -> pair(
                    FLOW_IPC_V1_72_MULTI_SELECT_COMBO_BOX_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_72);
            case FLOW_IPC_V1_73_TIME_PICKER_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_73_TIME_PICKER_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_73 -> pair(
                    FLOW_IPC_V1_73_TIME_PICKER_THEME_VARIANT_MATRIX, FLOW_IPC_V1_73);
            case FLOW_IPC_V1_74_TEXT_AREA_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_74_TEXT_AREA_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_74 -> pair(
                    FLOW_IPC_V1_74_TEXT_AREA_THEME_VARIANT_MATRIX, FLOW_IPC_V1_74);
            case FLOW_IPC_V1_75_EMAIL_FIELD_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_75_EMAIL_FIELD_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_75 -> pair(
                    FLOW_IPC_V1_75_EMAIL_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_75);
            case FLOW_IPC_V1_76_INTEGER_FIELD_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_76_INTEGER_FIELD_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_76 -> pair(
                    FLOW_IPC_V1_76_INTEGER_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_76);
            case FLOW_IPC_V1_77_PASSWORD_FIELD_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_77_PASSWORD_FIELD_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_77 -> pair(
                    FLOW_IPC_V1_77_PASSWORD_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_77);
            case FLOW_IPC_V1_78_BIG_DECIMAL_FIELD_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_78_BIG_DECIMAL_FIELD_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_78 -> pair(
                    FLOW_IPC_V1_78_BIG_DECIMAL_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_78);
            case FLOW_IPC_V1_79_CHECKBOX_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_79_CHECKBOX_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_79 -> pair(
                    FLOW_IPC_V1_79_CHECKBOX_THEME_VARIANT_MATRIX, FLOW_IPC_V1_79);
            case FLOW_IPC_V1_80_RADIO_BUTTON_GROUP_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_80_RADIO_BUTTON_GROUP_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_80 -> pair(
                    FLOW_IPC_V1_80_RADIO_BUTTON_GROUP_THEME_VARIANT_MATRIX, FLOW_IPC_V1_80);
            case FLOW_IPC_V1_81_TABS_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_81_TABS_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_81 -> pair(
                    FLOW_IPC_V1_81_TABS_THEME_VARIANT_MATRIX, FLOW_IPC_V1_81);
            case FLOW_IPC_V1_82_DETAILS_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_82_DETAILS_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_82 -> pair(
                    FLOW_IPC_V1_82_DETAILS_THEME_VARIANT_MATRIX, FLOW_IPC_V1_82);
            case FLOW_IPC_V1_83_MENU_BAR_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_83_MENU_BAR_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_83 -> pair(
                    FLOW_IPC_V1_83_MENU_BAR_THEME_VARIANT_MATRIX, FLOW_IPC_V1_83);
            case FLOW_IPC_V1_84_GRID_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_84_GRID_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_84 -> pair(
                    FLOW_IPC_V1_84_GRID_THEME_VARIANT_MATRIX, FLOW_IPC_V1_84);
            case FLOW_IPC_V1_85_AVATAR_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_85_AVATAR_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_85 -> pair(
                    FLOW_IPC_V1_85_AVATAR_THEME_VARIANT_MATRIX, FLOW_IPC_V1_85);
            case FLOW_IPC_V1_86_AVATAR_GROUP_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_86_AVATAR_GROUP_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_86 -> pair(
                    FLOW_IPC_V1_86_AVATAR_GROUP_THEME_VARIANT_MATRIX, FLOW_IPC_V1_86);
            case FLOW_IPC_V1_87_MESSAGE_INPUT_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_87_MESSAGE_INPUT_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_87 -> pair(
                    FLOW_IPC_V1_87_MESSAGE_INPUT_THEME_VARIANT_MATRIX, FLOW_IPC_V1_87);
            case FLOW_IPC_V1_88_SIDE_NAV_THEME_VARIANT_MATRIX ->
                single(FLOW_IPC_V1_88_SIDE_NAV_THEME_VARIANT_MATRIX);
            case FLOW_IPC_V1_88 -> pair(
                    FLOW_IPC_V1_88_SIDE_NAV_THEME_VARIANT_MATRIX, FLOW_IPC_V1_88);
            case FLOW_IPC_V1_89_MESSAGE_LIST_MARKDOWN_MATRIX ->
                single(FLOW_IPC_V1_89_MESSAGE_LIST_MARKDOWN_MATRIX);
            case FLOW_IPC_V1_89 -> pair(
                    FLOW_IPC_V1_89_MESSAGE_LIST_MARKDOWN_MATRIX, FLOW_IPC_V1_89);
            default -> throw new IllegalArgumentException("Unsupported theme matrix gate: " + gate);
        };
    }
}
