package dev.vatori.desktop.devtools;

final class RichTextEditorEditingTraceInspector {
    private RichTextEditorEditingTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        RichTextEditorEditingEventTraceInspector.inspectLine(summary, line);
        RichTextEditorEditingResourceTraceInspector.inspectLine(summary, line);
        RichTextEditorEditingDomTraceInspector.inspectLine(summary, line);
    }
}
