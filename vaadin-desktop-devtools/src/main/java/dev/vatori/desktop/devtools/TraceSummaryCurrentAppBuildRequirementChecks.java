package dev.vatori.desktop.devtools;

final class TraceSummaryCurrentAppBuildRequirementChecks {
    private TraceSummaryCurrentAppBuildRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_31FrontendBuildPreflightAvailabilityTypes(
            TraceSummary summary) {
        return summary.frontendBuildPreflight.hasAvailabilityV1_31Signals();
    }

    static boolean hasRequiredFlowIpcV1_31Types(TraceSummary summary) {
        return TraceSummaryGeneratedLazyBuildRequirementChecks
                .hasRequiredFlowIpcV1_30Types(summary)
                && hasRequiredFlowIpcV1_31FrontendBuildPreflightAvailabilityTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_32BuildFrontendOutputTypes(TraceSummary summary) {
        return summary.currentAppBuildFrontend.hasOutputV1_32Signals();
    }

    static boolean hasRequiredFlowIpcV1_32Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_31Types(summary)
                && hasRequiredFlowIpcV1_32BuildFrontendOutputTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_33CurrentAppGeneratedChunkPackageEdgeTypes(
            TraceSummary summary) {
        return summary.currentAppBuildFrontend.hasGeneratedChunkPackageEdgeV1_33Signals();
    }

    static boolean hasRequiredFlowIpcV1_33Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_32Types(summary)
                && hasRequiredFlowIpcV1_33CurrentAppGeneratedChunkPackageEdgeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_34CurrentAppGeneratedImportsRootPackageGraphTypes(
            TraceSummary summary) {
        return summary.currentAppBuildFrontend.hasGeneratedImportsRootPackageGraphV1_34Signals();
    }

    static boolean hasRequiredFlowIpcV1_34Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_33Types(summary)
                && hasRequiredFlowIpcV1_34CurrentAppGeneratedImportsRootPackageGraphTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_35CurrentAppProductionRuntimeOwnershipTypes(
            TraceSummary summary) {
        return summary.currentAppProductionOwner.hasRuntimeOwnershipV1_35Signals();
    }

    static boolean hasRequiredFlowIpcV1_35Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_34Types(summary)
                && hasRequiredFlowIpcV1_35CurrentAppProductionRuntimeOwnershipTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_36CurrentAppProductionOwnerImportTypes(
            TraceSummary summary) {
        return summary.currentAppProductionOwner.hasOwnerImportV1_36Signals();
    }

    static boolean hasRequiredFlowIpcV1_36Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_35Types(summary)
                && hasRequiredFlowIpcV1_36CurrentAppProductionOwnerImportTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_37CurrentAppProductionOwnerComponentReuseTypes(
            TraceSummary summary) {
        return summary.currentAppOwnerComponentReuse.hasProductionOwnerComponentReuseV1_37Signals();
    }

    static boolean hasRequiredFlowIpcV1_37Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_37CurrentAppProductionOwnerComponentReuseTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_38CurrentAppOwnerComponentReuseMatrixTypes(
            TraceSummary summary) {
        return summary.currentAppOwnerComponentReuse.hasMatrixV1_38Signals();
    }

    static boolean hasRequiredFlowIpcV1_38Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_38CurrentAppOwnerComponentReuseMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_39CurrentAppOwnerEarlyImportTypes(
            TraceSummary summary) {
        return summary.currentAppOwnerEarlyImport.hasEarlyImportV1_39Signals();
    }

    static boolean hasRequiredFlowIpcV1_39Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_39CurrentAppOwnerEarlyImportTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_40CurrentAppGeneratedImportsOwnerSideEffectTypes(
            TraceSummary summary) {
        return summary.generatedImportsOwner.hasSideEffectV1_40Signals();
    }

    static boolean hasRequiredFlowIpcV1_40Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_40CurrentAppGeneratedImportsOwnerSideEffectTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_41CurrentAppGeneratedImportsSelectOwnerCoverageTypes(
            TraceSummary summary) {
        return summary.generatedImportsOwner.hasSelectOwnerCoverageV1_41Signals();
    }

    static boolean hasRequiredFlowIpcV1_41Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_41CurrentAppGeneratedImportsSelectOwnerCoverageTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_42GeneratedImportsOwnerRuntimeContractTypes(
            TraceSummary summary) {
        return summary.generatedImportsOwner.hasRuntimeContractV1_42Signals();
    }

    static boolean hasRequiredFlowIpcV1_42Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_42GeneratedImportsOwnerRuntimeContractTypes(summary);
    }

    static boolean hasCurrentAppBuildFrontendOutputSuccessorEvidence(TraceSummary summary) {
        return summary.currentAppBuildFrontend.hasOutputV1_32Signals();
    }
}
