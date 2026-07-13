package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class ThemeMatrixTraceSummaryPrinter {
    private ThemeMatrixTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        ThemeMatrixTraceFields themeMatrix = summary.themeMatrix;
        printLine("RichTextEditor theme variant matrix DOM observed",
                themeMatrix.richTextEditorDomObserved);
        printLine("Button theme variant matrix DOM observed", themeMatrix.buttonDomObserved);
        printLine("TextField theme variant matrix DOM observed", themeMatrix.textFieldDomObserved);
        printLine("Select theme variant matrix DOM observed", themeMatrix.selectDomObserved);
        printLine("ComboBox theme variant matrix DOM observed", themeMatrix.comboBoxDomObserved);
        printLine("DatePicker theme variant matrix DOM observed", themeMatrix.datePickerDomObserved);
        printLine("MultiSelectComboBox theme variant matrix DOM observed",
                themeMatrix.multiSelectComboBoxDomObserved);
        printLine("TimePicker theme variant matrix DOM observed", themeMatrix.timePickerDomObserved);
        printLine("TextArea theme variant matrix DOM observed", themeMatrix.textAreaDomObserved);
        printLine("EmailField theme variant matrix DOM observed", themeMatrix.emailFieldDomObserved);
        printLine("IntegerField theme variant matrix DOM observed", themeMatrix.integerFieldDomObserved);
        printLine("PasswordField theme variant matrix DOM observed", themeMatrix.passwordFieldDomObserved);
        printLine("BigDecimalField theme variant matrix DOM observed", themeMatrix.bigDecimalFieldDomObserved);
        printLine("Checkbox theme variant matrix DOM observed", themeMatrix.checkboxDomObserved);
        printLine("RadioButtonGroup theme variant matrix DOM observed", themeMatrix.radioButtonGroupDomObserved);
        printLine("Tabs theme variant matrix DOM observed", themeMatrix.tabsDomObserved);
        printLine("Details theme variant matrix DOM observed", themeMatrix.detailsDomObserved);
        printLine("MenuBar theme variant matrix DOM observed", themeMatrix.menuBarDomObserved);
        printLine("Grid theme variant matrix DOM observed", themeMatrix.gridDomObserved);
        printLine("Avatar theme variant matrix DOM observed", themeMatrix.avatarDomObserved);
        printLine("AvatarGroup theme variant matrix DOM observed", themeMatrix.avatarGroupDomObserved);
        printLine("MessageInput theme variant matrix DOM observed", themeMatrix.messageInputDomObserved);
        printLine("SideNav theme variant matrix DOM observed", themeMatrix.sideNavDomObserved);
        printLine("MessageList markdown matrix DOM observed", themeMatrix.messageListMarkdownDomObserved);
    }
}
