package dev.vatori.desktop.devtools;

final class TraceSummaryGeneratedLazyBuildRequirementChecks {
    private TraceSummaryGeneratedLazyBuildRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_26GeneratedLazySourceBoundaryTypes(
            TraceSummary summary) {
        return summary.generatedLazyBuild.hasSourceBoundaryV1_26Signals(
                TraceSummaryCurrentAppBuildRequirementChecks
                        .hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_26Types(TraceSummary summary) {
        return TraceSummaryFrontendOwnerPipelineRequirementChecks
                .hasRequiredFlowIpcV1_25Types(summary)
                && hasRequiredFlowIpcV1_26GeneratedLazySourceBoundaryTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_27GeneratedLazyRealBuildProbeTypes(
            TraceSummary summary) {
        return summary.generatedLazyBuild.hasRealBuildProbeV1_27Signals(
                TraceSummaryCurrentAppBuildRequirementChecks
                        .hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_27Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_26Types(summary)
                && hasRequiredFlowIpcV1_27GeneratedLazyRealBuildProbeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_28GeneratedLazyBuildLifecycleTypes(
            TraceSummary summary) {
        return summary.generatedLazyBuild.hasBuildLifecycleV1_28Signals(
                TraceSummaryCurrentAppBuildRequirementChecks
                        .hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_28Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_27Types(summary)
                && hasRequiredFlowIpcV1_28GeneratedLazyBuildLifecycleTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_29GeneratedLazyAppBuildOutputTypes(
            TraceSummary summary) {
        return summary.generatedLazyBuild.hasAppBuildOutputV1_29Signals(
                TraceSummaryCurrentAppBuildRequirementChecks
                        .hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_29Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_28Types(summary)
                && hasRequiredFlowIpcV1_29GeneratedLazyAppBuildOutputTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_30CurrentAppFrontendBuildOutputTypes(
            TraceSummary summary) {
        return summary.generatedLazyBuild.hasCurrentAppFrontendBuildOutputV1_30Signals(
                TraceSummaryCurrentAppBuildRequirementChecks
                        .hasCurrentAppBuildFrontendOutputSuccessorEvidence(summary));
    }

    static boolean hasRequiredFlowIpcV1_30Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_29Types(summary)
                && hasRequiredFlowIpcV1_30CurrentAppFrontendBuildOutputTypes(summary);
    }
}
