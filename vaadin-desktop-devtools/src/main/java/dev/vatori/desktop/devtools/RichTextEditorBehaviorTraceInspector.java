package dev.vatori.desktop.devtools;

final class RichTextEditorBehaviorTraceInspector {
    private RichTextEditorBehaviorTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        RichTextEditorBehaviorEventTraceInspector.inspectLine(summary, line);
        RichTextEditorBehaviorResourceTraceInspector.inspectLine(summary, line);
        RichTextEditorBehaviorDomTraceInspector.inspectLine(summary, line);
    }
}
