package dev.vatori.desktop.devtools;

final class ComponentResourceMatrixTraceRequirementChecks {
    private ComponentResourceMatrixTraceRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_46BroaderComponentResourceMatrixTypes(
            TraceSummary summary) {
        return summary.componentResources.hasBroaderMatrixV1_46Signals()
                && summary.frameworkComponentMatrix.hasBasicSignals()
                && TraceSummaryPackagingRequirementChecks
                        .hasRequiredFlowIpcV1_45FrameworkPackagingRuntimeHandoffReuseTypes(
                                summary);
    }

    static boolean hasRequiredFlowIpcV1_47DataComponentResourceMatrixTypes(
            TraceSummary summary) {
        return summary.componentResources.hasDataMatrixV1_47Signals()
                && summary.frameworkComponentMatrix.hasDataSignals()
                && hasRequiredFlowIpcV1_46BroaderComponentResourceMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_48GridLazyDataProviderTypes(
            TraceSummary summary) {
        return summary.gridResources.hasLazyDataProviderGateSignals()
                && summary.gridResources.hasLazyDataProviderSignals()
                && hasRequiredFlowIpcV1_47DataComponentResourceMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_49GridScrollVirtualizationTypes(
            TraceSummary summary) {
        return summary.gridResources.hasScrollVirtualizationGateSignals()
                && summary.gridResources.hasScrollVirtualizationSignals(
                        summary.gridResources.scrollVirtualizationV149DomObserved)
                && hasRequiredFlowIpcV1_48GridLazyDataProviderTypes(summary);
    }
}
