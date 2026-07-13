package dev.vatori.desktop.devtools;

final class FrontendBuildResourceExchangeTraceInspector {
    private FrontendBuildResourceExchangeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrontendBuildResolverResourceExchangeTraceInspector.inspectLine(summary, line);
        FrontendBuildGeneratedResourceExchangeTraceInspector.inspectLine(summary, line);
        FrontendBuildCurrentAppResourceExchangeTraceInspector.inspectLine(summary, line);
    }
}
