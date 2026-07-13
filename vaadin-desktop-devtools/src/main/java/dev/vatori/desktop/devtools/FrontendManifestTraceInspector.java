package dev.vatori.desktop.devtools;

final class FrontendManifestTraceInspector {
    private FrontendManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrontendCoreManifestTraceInspector.inspectLine(summary, line);
        FrontendBuildPipelineManifestTraceInspector.inspectLine(summary, line);
        CurrentAppManifestTraceInspector.inspectLine(summary, line);
        ComponentResourceManifestTraceInspector.inspectLine(summary, line);
    }
}
