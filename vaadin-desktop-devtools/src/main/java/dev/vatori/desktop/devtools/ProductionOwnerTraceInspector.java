package dev.vatori.desktop.devtools;

final class ProductionOwnerTraceInspector {
    private ProductionOwnerTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        ProductionOwnerManifestTraceInspector.inspectLine(summary, line);
        ProductionOwnerResourceExchangeTraceInspector.inspectLine(summary, line);
        ProductionOwnerStageTraceInspector.inspectLine(summary, line);
        ProductionOwnerResourceLoadTraceInspector.inspectLine(summary, line);
        ProductionOwnerDomTraceInspector.inspectLine(summary, line);
    }
}
