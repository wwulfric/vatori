package dev.vatori.desktop.devtools;

final class TraceSummaryFrontendPipelineEarlyRequirementChecks {
    private TraceSummaryFrontendPipelineEarlyRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_4Types(TraceSummary summary) {
        return TraceSummaryCoreRequirementChecks.hasRequiredFlowIpcV1_3Types(summary)
                && summary.frontendBuildPipeline.hasV1_4Signals();
    }

    static boolean hasRequiredFlowIpcV1_5Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_4Types(summary)
                && summary.frontendBuildPipeline.hasV1_5Signals(
                        hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_6Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_5Types(summary)
                && summary.frontendBuildPipeline.hasTextFieldV1_6Signals();
    }

    static boolean hasRequiredFlowIpcV1_7Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_5Types(summary)
                && summary.frontendBuildPipeline.hasTextFieldV1_7Signals()
                && summary.vaadinComponents.hasTextFieldRuntimeSignals();
    }

    static boolean hasRequiredFlowIpcV1_8Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_7Types(summary)
                && summary.frontendBuildPipeline.hasGeneratedLazyV1_8Signals(
                        hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_9LoadOnDemandTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasLoadOnDemandV1_9Signals(
                hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_9Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_8Types(summary)
                && hasRequiredFlowIpcV1_9LoadOnDemandTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_10LazyDependencyTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasLazyDependencyV1_10Signals();
    }

    static boolean hasRequiredFlowIpcV1_10ProductionRuntimeTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasProductionRuntimeV1_10Signals();
    }

    static boolean hasRequiredFlowIpcV1_10Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_9Types(summary)
                && hasRequiredFlowIpcV1_10LazyDependencyTypes(summary)
                && hasRequiredFlowIpcV1_10ProductionRuntimeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_11LoadOnDemandChunkTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasLoadOnDemandChunkV1_11Signals(
                hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_11Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_10Types(summary)
                && hasRequiredFlowIpcV1_11LoadOnDemandChunkTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasLoadOnDemandChunkFileV1_12Signals(
                hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_12Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_11Types(summary)
                && hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes(summary);
    }

    private static boolean hasCurrentAppBuildFrontendOutputSuccessorEvidence(
            TraceSummary summary) {
        return TraceSummaryCurrentAppBuildRequirementChecks
                .hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary);
    }
}
