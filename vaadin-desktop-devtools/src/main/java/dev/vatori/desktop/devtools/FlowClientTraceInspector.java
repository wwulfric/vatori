package dev.vatori.desktop.devtools;

final class FlowClientTraceInspector {
    private FlowClientTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FlowClientEventRpcTraceInspector.inspectLine(summary, line);
        FlowClientResourceGraphTraceInspector.inspectLine(summary, line);
        FlowClientScopeTraceInspector.inspectLine(summary, line);
        FlowClientUidlTransportTraceInspector.inspectLine(summary, line);
    }
}
