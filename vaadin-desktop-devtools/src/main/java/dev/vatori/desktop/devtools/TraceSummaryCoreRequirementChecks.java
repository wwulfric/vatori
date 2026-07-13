package dev.vatori.desktop.devtools;

final class TraceSummaryCoreRequirementChecks {
    private TraceSummaryCoreRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcTypes(TraceSummary summary) {
        return TraceRequiredTypes.hasBaseTypes(summary)
                && summary.core.hasBaseSignals()
                && summary.coreCapabilities.hasBaseSignals()
                && summary.flowClient.hasBaseSignals();
    }

    static boolean hasRequiredFlowIpcV1_1Types(TraceSummary summary) {
        return hasRequiredFlowIpcTypes(summary)
                && summary.coreCapabilities.hasV1_1Signals()
                && summary.core.hasV1_1Signals()
                && summary.flowClient.hasV1_1Signals();
    }

    static boolean hasRequiredFlowIpcV1_2Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_1Types(summary)
                && summary.vaadinComponents.hasV1_2Signals();
    }

    static boolean hasRequiredFlowIpcV1_3Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_2Types(summary)
                && summary.flowClient.hasV1_3Signals()
                && summary.vaadinComponents.hasV1_3Signals();
    }
}
