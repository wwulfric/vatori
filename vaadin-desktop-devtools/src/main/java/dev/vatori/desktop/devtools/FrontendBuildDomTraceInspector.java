package dev.vatori.desktop.devtools;

final class FrontendBuildDomTraceInspector {
    private FrontendBuildDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrontendBuildPipelineDomTraceInspector.inspectLine(summary, line);
        GeneratedLazyBuildDomTraceInspector.inspectLine(summary, line);
        CurrentAppBuildDomTraceInspector.inspectLine(summary, line);
        FrontendBuildOwnerPackagingDomTraceInspector.inspectLine(summary, line);
        FrontendBuildResolutionDomTraceInspector.inspectLine(summary, line);
    }
}
