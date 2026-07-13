package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ThemeMatrixInputTraceInspector {
    private ThemeMatrixInputTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsThemeVariantMatrix(line,
                "textFieldThemeVariantMatrix",
                "flow-ipc-text-field-theme-variant-matrix-v1_68",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.textFieldDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "selectThemeVariantMatrix",
                "flow-ipc-select-theme-variant-matrix-v1_69",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.selectDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "comboBoxThemeVariantMatrix",
                "flow-ipc-combo-box-theme-variant-matrix-v1_70",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.comboBoxDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "datePickerThemeVariantMatrix",
                "flow-ipc-date-picker-theme-variant-matrix-v1_71",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.datePickerDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "multiSelectComboBoxThemeVariantMatrix",
                "flow-ipc-multi-select-combo-box-theme-variant-matrix-v1_72",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.multiSelectComboBoxDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "timePickerThemeVariantMatrix",
                "flow-ipc-time-picker-theme-variant-matrix-v1_73",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.timePickerDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "textAreaThemeVariantMatrix",
                "flow-ipc-text-area-theme-variant-matrix-v1_74",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.textAreaDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "emailFieldThemeVariantMatrix",
                "flow-ipc-email-field-theme-variant-matrix-v1_75",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.emailFieldDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "integerFieldThemeVariantMatrix",
                "flow-ipc-integer-field-theme-variant-matrix-v1_76",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.integerFieldDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "passwordFieldThemeVariantMatrix",
                "flow-ipc-password-field-theme-variant-matrix-v1_77",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.passwordFieldDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "bigDecimalFieldThemeVariantMatrix",
                "flow-ipc-big-decimal-field-theme-variant-matrix-v1_78",
                "small",
                "SmallObserved")) {
            summary.themeMatrix.bigDecimalFieldDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "checkboxThemeVariantMatrix",
                "flow-ipc-checkbox-theme-variant-matrix-v1_79",
                "helper-above-field",
                "HelperAboveObserved")) {
            summary.themeMatrix.checkboxDomObserved = true;
        }
        if (containsThemeVariantMatrix(line,
                "radioButtonGroupThemeVariantMatrix",
                "flow-ipc-radio-button-group-theme-variant-matrix-v1_80",
                "vertical",
                "VerticalObserved")) {
            summary.themeMatrix.radioButtonGroupDomObserved = true;
        }
    }

    private static boolean containsThemeVariantMatrix(
            String line,
            String fieldPrefix,
            String version,
            String theme,
            String observedFieldSuffix) {
        return containsLiteralField(line, fieldPrefix + "Complete", "true")
                && containsStringField(line, fieldPrefix + "Version", version)
                && containsStringField(line, fieldPrefix + "Theme", theme)
                && containsLiteralField(line, fieldPrefix + observedFieldSuffix, "true")
                && containsLiteralField(line, fieldPrefix + "NoLocalhost", "true")
                && containsLiteralField(line, fieldPrefix + "FixtureSubstituted", "false");
    }
}
