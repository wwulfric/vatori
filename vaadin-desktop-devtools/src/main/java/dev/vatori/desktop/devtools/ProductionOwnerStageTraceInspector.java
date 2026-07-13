package dev.vatori.desktop.devtools;

final class ProductionOwnerStageTraceInspector {
    private ProductionOwnerStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        ProductionOwnerRuntimeStageTraceInspector.inspectLine(summary, line);
        ProductionOwnerResourceRuntimeStageTraceInspector.inspectLine(summary, line);
        ProductionOwnerPositiveFixtureStageTraceInspector.inspectLine(summary, line);
    }
}
