package dev.vatori.desktop.devtools;

import java.util.List;

final class TraceSummaryFrameworkRequirementChecks {
    private TraceSummaryFrameworkRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_3FrameworkTypes(TraceSummary summary) {
        return summary.types.keySet().containsAll(List.of(
                "launch",
                "ready",
                "capabilityManifest",
                "window.create",
                "bootstrapRequest",
                "bootstrapResponse",
                "resourceRequest",
                "resourceResponse",
                "event",
                "patch",
                "command",
                "capabilityResult",
                "webview.firstMeaningfulPaint",
                "webview.dom",
                "webview.snapshot",
                "shutdown"))
                && summary.core.flowIpcTransport
                && summary.core.compatibilityFalse
                && summary.core.serverSequenceObserved
                && summary.core.uidlWriterObserved
                && summary.flowClient.hasFrameworkBaseSignals()
                && summary.core.firstMeaningfulPaintObserved
                && summary.vaadinComponents.hasFrameworkBaseSignals()
                && summary.frameworkApp.hasFrameworkBaseSignals();
    }
}
