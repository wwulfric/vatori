package dev.vatori.desktop.devtools;

final class FrontendBuildResourceStageTraceInspector {
    private FrontendBuildResourceStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrontendBuildResolverStageTraceInspector.inspectLine(summary, line);
        FrontendBuildGeneratedStageTraceInspector.inspectLine(summary, line);
        FrontendBuildCurrentAppStageTraceInspector.inspectLine(summary, line);
    }
}
