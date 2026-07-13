package dev.vatori.desktop.devtools;

final class FlowClientScopeTraceInspector {
    private FlowClientScopeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"securityKey\"")
                && line.contains("Vaadin-Security-Key")
                && line.contains("\"containsSecurityKey\":true")) {
            summary.core.securityKeyObserved = true;
        }
        if (line.contains("\"type\":\"resync\"")
                && line.contains("\"securityKey\"")) {
            summary.core.resyncSecurityKeyObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapRequest\"")
                && line.contains("\"sessionId\":\"flow-ipc-session\"")
                && line.contains("\"uiId\":\"main\"")) {
            summary.core.bootstrapScopeObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"sessionId\":\"flow-ipc-session\"")
                && line.contains("\"uiId\":\"main\"")) {
            summary.core.eventScopeObserved = true;
        }
        if (line.contains("\"type\":\"resync\"")
                && line.contains("\"sessionId\":\"flow-ipc-session\"")
                && line.contains("\"uiId\":\"main\"")) {
            summary.core.resyncScopeObserved = true;
        }
    }
}
