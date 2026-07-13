package dev.vatori.desktop.devtools;

final class ThemeMatrixTraceRequirementChecks {
    private ThemeMatrixTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return switch (gate) {
            case FLOW_IPC_V1_66_RICH_TEXT_EDITOR_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_66,
                    FLOW_IPC_V1_67_BUTTON_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_67,
                    FLOW_IPC_V1_68_TEXT_FIELD_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_68,
                    FLOW_IPC_V1_69_SELECT_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_69,
                    FLOW_IPC_V1_70_COMBO_BOX_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_70,
                    FLOW_IPC_V1_71_DATE_PICKER_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_71,
                    FLOW_IPC_V1_72_MULTI_SELECT_COMBO_BOX_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_72,
                    FLOW_IPC_V1_73_TIME_PICKER_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_73,
                    FLOW_IPC_V1_74_TEXT_AREA_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_74,
                    FLOW_IPC_V1_75_EMAIL_FIELD_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_75,
                    FLOW_IPC_V1_76_INTEGER_FIELD_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_76,
                    FLOW_IPC_V1_77_PASSWORD_FIELD_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_77,
                    FLOW_IPC_V1_78_BIG_DECIMAL_FIELD_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_78,
                    FLOW_IPC_V1_79_CHECKBOX_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_79,
                    FLOW_IPC_V1_80_RADIO_BUTTON_GROUP_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_80 ->
                ThemeMatrixInputTraceRequirementChecks.isSatisfied(gate, summary);
            case FLOW_IPC_V1_81_TABS_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_81,
                    FLOW_IPC_V1_82_DETAILS_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_82,
                    FLOW_IPC_V1_83_MENU_BAR_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_83,
                    FLOW_IPC_V1_84_GRID_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_84,
                    FLOW_IPC_V1_85_AVATAR_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_85,
                    FLOW_IPC_V1_86_AVATAR_GROUP_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_86,
                    FLOW_IPC_V1_87_MESSAGE_INPUT_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_87,
                    FLOW_IPC_V1_88_SIDE_NAV_THEME_VARIANT_MATRIX,
                    FLOW_IPC_V1_88,
                    FLOW_IPC_V1_89_MESSAGE_LIST_MARKDOWN_MATRIX,
                    FLOW_IPC_V1_89 ->
                ThemeMatrixNavigationContentTraceRequirementChecks.isSatisfied(gate, summary);
            default -> throw new IllegalArgumentException("Unsupported theme matrix trace gate: " + gate);
        };
    }
}
