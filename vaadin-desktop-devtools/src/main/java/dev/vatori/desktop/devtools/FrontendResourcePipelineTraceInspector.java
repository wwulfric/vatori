package dev.vatori.desktop.devtools;

final class FrontendResourcePipelineTraceInspector {
    private FrontendResourcePipelineTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrontendResourceManifestTraceInspector.inspectLine(summary, line);
        GeneratedLazyResourceTraceInspector.inspectLine(summary, line);
        FrontendResourceSchemeTraceInspector.inspectLine(summary, line);
        FrontendResourcePipelineDomTraceInspector.inspectLine(summary, line);
        FrontendResourcePipelineStageTraceInspector.inspectLine(summary, line);
    }
}
