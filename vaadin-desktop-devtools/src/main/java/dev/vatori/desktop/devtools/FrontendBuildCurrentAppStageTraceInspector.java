package dev.vatori.desktop.devtools;

final class FrontendBuildCurrentAppStageTraceInspector {
    private FrontendBuildCurrentAppStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrontendBuildCurrentAppPrimaryStageTraceInspector.inspectLine(summary, line);
        FrontendBuildCurrentAppCompletionStageTraceInspector.inspectLine(summary, line);
    }
}
