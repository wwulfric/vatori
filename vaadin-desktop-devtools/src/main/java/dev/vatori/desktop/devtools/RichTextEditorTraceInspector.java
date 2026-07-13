package dev.vatori.desktop.devtools;

final class RichTextEditorTraceInspector {
    private RichTextEditorTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        RichTextEditorMatrixTraceInspector.inspectLine(summary, line);
        RichTextEditorBehaviorTraceInspector.inspectLine(summary, line);
        RichTextEditorEditingTraceInspector.inspectLine(summary, line);
    }
}
