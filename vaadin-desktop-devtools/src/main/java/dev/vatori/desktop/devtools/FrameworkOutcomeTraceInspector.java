package dev.vatori.desktop.devtools;

final class FrameworkOutcomeTraceInspector {
    private FrameworkOutcomeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrameworkEventPatchTraceInspector.inspectLine(summary, line);
        FrameworkLazyGridPatchTraceInspector.inspectLine(summary, line);
        FrameworkUploadPatchTraceInspector.inspectLine(summary, line);
        FrameworkNativeCommandOutcomeTraceInspector.inspectLine(summary, line);
    }
}
