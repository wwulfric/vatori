package dev.vatori.desktop.devtools;

final class ProductionOwnerDomTraceInspector {
    private ProductionOwnerDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        ProductionOwnerLegacyDomTraceInspector.inspectLine(summary, line);
        ProductionOwnerRuntimeDomTraceInspector.inspectLine(summary, line);
        ProductionOwnerFixtureDomTraceInspector.inspectLine(summary, line);
    }
}
