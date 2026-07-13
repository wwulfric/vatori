package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static dev.vatori.desktop.devtools.TraceRequirementChain.cumulative;
import static dev.vatori.desktop.devtools.TraceRequirementChain.entry;

final class ThemeMatrixInputTraceRequirementChecks {
    private static final TraceRequirementChain INPUT_THEME_CHAIN = cumulative(
            "input theme matrix trace gate",
            RichTextNativeTraceRequirementChecks::hasRequiredFlowIpcV1_65Types,
            entry(FLOW_IPC_V1_66_RICH_TEXT_EDITOR_THEME_VARIANT_MATRIX, FLOW_IPC_V1_66,
                    summary -> summary.themeMatrix.richTextEditorDomObserved),
            entry(FLOW_IPC_V1_67_BUTTON_THEME_VARIANT_MATRIX, FLOW_IPC_V1_67,
                    summary -> summary.themeMatrix.buttonDomObserved),
            entry(FLOW_IPC_V1_68_TEXT_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_68,
                    summary -> summary.themeMatrix.textFieldDomObserved),
            entry(FLOW_IPC_V1_69_SELECT_THEME_VARIANT_MATRIX, FLOW_IPC_V1_69,
                    summary -> summary.themeMatrix.selectDomObserved),
            entry(FLOW_IPC_V1_70_COMBO_BOX_THEME_VARIANT_MATRIX, FLOW_IPC_V1_70,
                    summary -> summary.themeMatrix.comboBoxDomObserved),
            entry(FLOW_IPC_V1_71_DATE_PICKER_THEME_VARIANT_MATRIX, FLOW_IPC_V1_71,
                    summary -> summary.themeMatrix.datePickerDomObserved),
            entry(FLOW_IPC_V1_72_MULTI_SELECT_COMBO_BOX_THEME_VARIANT_MATRIX, FLOW_IPC_V1_72,
                    summary -> summary.themeMatrix.multiSelectComboBoxDomObserved),
            entry(FLOW_IPC_V1_73_TIME_PICKER_THEME_VARIANT_MATRIX, FLOW_IPC_V1_73,
                    summary -> summary.themeMatrix.timePickerDomObserved),
            entry(FLOW_IPC_V1_74_TEXT_AREA_THEME_VARIANT_MATRIX, FLOW_IPC_V1_74,
                    summary -> summary.themeMatrix.textAreaDomObserved),
            entry(FLOW_IPC_V1_75_EMAIL_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_75,
                    summary -> summary.themeMatrix.emailFieldDomObserved),
            entry(FLOW_IPC_V1_76_INTEGER_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_76,
                    summary -> summary.themeMatrix.integerFieldDomObserved),
            entry(FLOW_IPC_V1_77_PASSWORD_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_77,
                    summary -> summary.themeMatrix.passwordFieldDomObserved),
            entry(FLOW_IPC_V1_78_BIG_DECIMAL_FIELD_THEME_VARIANT_MATRIX, FLOW_IPC_V1_78,
                    summary -> summary.themeMatrix.bigDecimalFieldDomObserved),
            entry(FLOW_IPC_V1_79_CHECKBOX_THEME_VARIANT_MATRIX, FLOW_IPC_V1_79,
                    summary -> summary.themeMatrix.checkboxDomObserved),
            entry(FLOW_IPC_V1_80_RADIO_BUTTON_GROUP_THEME_VARIANT_MATRIX, FLOW_IPC_V1_80,
                    summary -> summary.themeMatrix.radioButtonGroupDomObserved));

    private ThemeMatrixInputTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return INPUT_THEME_CHAIN.isSatisfied(gate, summary);
    }

    static boolean hasRequiredFlowIpcV1_80Types(TraceSummary summary) {
        return INPUT_THEME_CHAIN.isSatisfied(FLOW_IPC_V1_80, summary);
    }
}
