package dev.vatori.desktop.devtools;

final class TraceSummaryFrontendOwnerPipelineRequirementChecks {
    private TraceSummaryFrontendOwnerPipelineRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_13ProductionOwnerTypes(TraceSummary summary) {
        return summary.productionOwnerRuntime.hasV1_13Signals();
    }

    static boolean hasRequiredFlowIpcV1_13Types(TraceSummary summary) {
        return TraceSummaryFrontendPipelineEarlyRequirementChecks
                .hasRequiredFlowIpcV1_12Types(summary)
                && hasRequiredFlowIpcV1_13ProductionOwnerTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_14ProductionOwnerImportTypes(TraceSummary summary) {
        return summary.productionOwnerRuntime.hasV1_14Signals();
    }

    static boolean hasRequiredFlowIpcV1_14Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_13Types(summary)
                && hasRequiredFlowIpcV1_14ProductionOwnerImportTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes(TraceSummary summary) {
        return summary.productionOwnerRuntime.hasV1_15Signals();
    }

    static boolean hasRequiredFlowIpcV1_15Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_14Types(summary)
                && hasRequiredFlowIpcV1_15ProductionOwnerDependencyTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_16ProductionOwnerOrderingTypes(TraceSummary summary) {
        return summary.productionOwnerRuntime.hasV1_16Signals();
    }

    static boolean hasRequiredFlowIpcV1_16Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_15Types(summary)
                && hasRequiredFlowIpcV1_16ProductionOwnerOrderingTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_17ResourceSchemeTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasResourceSchemeV1_17Signals();
    }

    static boolean hasRequiredFlowIpcV1_17Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_16Types(summary)
                && hasRequiredFlowIpcV1_17ResourceSchemeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes(
            TraceSummary summary) {
        return summary.productionOwnerRuntime.hasResourceSchemeV1_18Signals();
    }

    static boolean hasRequiredFlowIpcV1_18Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_17Types(summary)
                && hasRequiredFlowIpcV1_18ProductionOwnerResourceSchemeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_19ProductionOwnerSideEffectTypes(TraceSummary summary) {
        return summary.productionOwnerRuntime.hasSideEffectV1_19Signals();
    }

    static boolean hasRequiredFlowIpcV1_19Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_18Types(summary)
                && hasRequiredFlowIpcV1_19ProductionOwnerSideEffectTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes(
            TraceSummary summary) {
        return summary.productionOwnerRuntime.hasOwnedRuntimeV1_20Signals();
    }

    static boolean hasRequiredFlowIpcV1_20Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_19Types(summary)
                && hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_21FrontendResolverContractTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasResolverContractV1_21Signals();
    }

    static boolean hasRequiredFlowIpcV1_21Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_20Types(summary)
                && hasRequiredFlowIpcV1_21FrontendResolverContractTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_22GeneratedChunkFixtureTypes(TraceSummary summary) {
        return TraceSummaryFrontendPipelineEarlyRequirementChecks
                .hasRequiredFlowIpcV1_12LoadOnDemandChunkFileTypes(summary)
                && summary.frontendBuildPipeline.hasGeneratedChunkFileFixtureV1_22Signals();
    }

    static boolean hasRequiredFlowIpcV1_22Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_21Types(summary)
                && hasRequiredFlowIpcV1_22GeneratedChunkFixtureTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_23ProductionOwnerPositiveFixtureTypes(
            TraceSummary summary) {
        return hasRequiredFlowIpcV1_20ProductionOwnerOwnedRuntimeTypes(summary)
                && summary.productionOwnerRuntime.hasPositiveFixtureV1_23Signals();
    }

    static boolean hasRequiredFlowIpcV1_23Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_22Types(summary)
                && hasRequiredFlowIpcV1_23ProductionOwnerPositiveFixtureTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_24FrontendPipelineClosureTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasPipelineClosureV1_24Signals();
    }

    static boolean hasRequiredFlowIpcV1_24Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_23Types(summary)
                && hasRequiredFlowIpcV1_24FrontendPipelineClosureTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_25PackagedFrontendBaselineTypes(TraceSummary summary) {
        return summary.frontendBuildPipeline.hasPackagedFrontendBaselineV1_25Signals();
    }

    static boolean hasRequiredFlowIpcV1_25Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_24Types(summary)
                && hasRequiredFlowIpcV1_25PackagedFrontendBaselineTypes(summary);
    }
}
