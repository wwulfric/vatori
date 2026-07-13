package dev.vatori.desktop.devtools;

final class GeneratedLazyResourceTraceInspector {
    private GeneratedLazyResourceTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        GeneratedLazyResourceRequestTraceInspector.inspectLine(summary, line);
        GeneratedLazyResourceStageTraceInspector.inspectLine(summary, line);
        GeneratedLazyResourceDomTraceInspector.inspectLine(summary, line);
    }
}
