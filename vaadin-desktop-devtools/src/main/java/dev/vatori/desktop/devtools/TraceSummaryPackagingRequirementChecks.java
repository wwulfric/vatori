package dev.vatori.desktop.devtools;

final class TraceSummaryPackagingRequirementChecks {
    private TraceSummaryPackagingRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_43GeneratedLazySpiDecisionTypes(TraceSummary summary) {
        return summary.generatedLazySpiDecision.hasDecisionV1_43Signals();
    }

    static boolean hasRequiredFlowIpcV1_43Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_43GeneratedLazySpiDecisionTypes(summary)
                && TraceSummaryCurrentAppBuildRequirementChecks
                        .hasRequiredFlowIpcV1_42GeneratedImportsOwnerRuntimeContractTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_44PackagingFrontendInventoryTypes(
            TraceSummary summary) {
        return summary.packaging.hasFrontendInventoryV1_44Signals();
    }

    static boolean hasRequiredFlowIpcV1_44Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_44PackagingFrontendInventoryTypes(summary)
                && TraceSummaryCurrentAppBuildRequirementChecks
                        .hasRequiredFlowIpcV1_42GeneratedImportsOwnerRuntimeContractTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_45PackagingRuntimeHandoffReuseTypes(
            TraceSummary summary) {
        return summary.packaging.hasRuntimeHandoffReuseV1_45Signals();
    }

    static boolean hasRequiredFlowIpcV1_45FrameworkPackagingRuntimeHandoffReuseTypes(
            TraceSummary summary) {
        return summary.packaging.hasFrameworkRuntimeHandoffReuseV1_45Signals()
                && TraceSummaryFrameworkRequirementChecks
                        .hasRequiredFlowIpcV1_3FrameworkTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_45Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_45PackagingRuntimeHandoffReuseTypes(summary)
                && hasRequiredFlowIpcV1_44Types(summary);
    }
}
