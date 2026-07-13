package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ThemeMatrixCoreTraceInspector {
    private ThemeMatrixCoreTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (containsLiteralField(line, "nativeClipboardReadMatrixComplete", "true")
                && containsStringField(line,
                        "nativeClipboardReadMatrixVersion",
                        "flow-ipc-native-clipboard-read-matrix-v1_65")
                && containsStringField(line,
                        "nativeClipboardReadMatrixMode",
                        "native-nspasteboard")
                && containsLiteralField(line,
                        "nativeClipboardReadMatrixPermissionObserved", "true")
                && containsLiteralField(line,
                        "nativeClipboardReadMatrixTextObserved", "true")
                && line.contains("Flow IPC native clipboard v1.65")) {
            summary.frameworkApp.nativeClipboardReadMatrixDomObserved = true;
        }
        if (containsLiteralField(line,
                "richTextEditorThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "richTextEditorThemeVariantMatrixVersion",
                        "flow-ipc-rich-text-editor-theme-variant-matrix-v1_66")
                && containsStringField(line,
                        "richTextEditorThemeVariantMatrixTheme",
                        "compact")
                && containsLiteralField(line,
                        "richTextEditorThemeVariantMatrixCompactObserved", "true")
                && containsLiteralField(line,
                        "richTextEditorThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "richTextEditorThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.richTextEditorDomObserved = true;
        }
        if (containsLiteralField(line, "buttonThemeVariantMatrixComplete", "true")
                && containsStringField(line,
                        "buttonThemeVariantMatrixVersion",
                        "flow-ipc-button-theme-variant-matrix-v1_67")
                && containsStringField(line,
                        "buttonThemeVariantMatrixTheme",
                        "primary")
                && containsLiteralField(line,
                        "buttonThemeVariantMatrixPrimaryObserved", "true")
                && containsLiteralField(line,
                        "buttonThemeVariantMatrixNoLocalhost", "true")
                && containsLiteralField(line,
                        "buttonThemeVariantMatrixFixtureSubstituted", "false")) {
            summary.themeMatrix.buttonDomObserved = true;
        }
    }
}
