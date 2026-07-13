package dev.vatori.desktop.devtools;

import java.util.List;

final class TraceRequiredTypes {
    static final List<String> FLOW_IPC_BASE = List.of(
            "launch",
            "ready",
            "window.create",
            "bootstrapRequest",
            "bootstrapResponse",
            "resourceRequest",
            "resourceResponse",
            "push",
            "event",
            "resync",
            "command",
            "capabilityResult",
            "patch",
            "error",
            "webview.dom",
            "webview.snapshot",
            "shutdown");

    private TraceRequiredTypes() {
    }

    static boolean hasBaseTypes(TraceSummary summary) {
        return summary.types.keySet().containsAll(FLOW_IPC_BASE);
    }

    static List<String> missingBaseTypes(TraceSummary summary) {
        return FLOW_IPC_BASE.stream()
                .filter(type -> !summary.types.containsKey(type))
                .toList();
    }
}
