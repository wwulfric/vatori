package dev.vatori.desktop.devtools;

final class ComponentParityTraceRequirementChecks {
    private ComponentParityTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return switch (gate) {
            case FLOW_IPC_V1_57_INPUT_COMPONENT_PARITY_MATRIX ->
                hasRequiredFlowIpcV1_57InputComponentParityMatrixTypes(summary);
            case FLOW_IPC_V1_57 -> hasRequiredFlowIpcV1_57Types(summary);
            case FLOW_IPC_V1_58_MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX ->
                hasRequiredFlowIpcV1_58MultiValueNavigationDisclosureMatrixTypes(summary);
            case FLOW_IPC_V1_58 -> hasRequiredFlowIpcV1_58Types(summary);
            case FLOW_IPC_V1_59_OVERLAY_CONTEXT_MENU_MATRIX ->
                hasRequiredFlowIpcV1_59OverlayContextMenuMatrixTypes(summary);
            case FLOW_IPC_V1_59 -> hasRequiredFlowIpcV1_59Types(summary);
            case FLOW_IPC_V1_60_CONTENT_APP_SHELL_LOGIN_MATRIX ->
                hasRequiredFlowIpcV1_60ContentAppShellLoginMatrixTypes(summary);
            case FLOW_IPC_V1_60 -> hasRequiredFlowIpcV1_60Types(summary);
            case FLOW_IPC_V1_61_LIFECYCLE_FOCUS_BEHAVIOR_MATRIX ->
                hasRequiredFlowIpcV1_61LifecycleFocusBehaviorMatrixTypes(summary);
            case FLOW_IPC_V1_61 -> hasRequiredFlowIpcV1_61Types(summary);
            default -> throw new IllegalArgumentException("Unsupported component parity gate: " + gate);
        };
    }

    static boolean hasRequiredFlowIpcV1_57InputComponentParityMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkInputComponentMatrix.hasInputParityGateSignals()
                && summary.frameworkInputComponentMatrix.hasFrameworkSignals()
                && ComponentResourcePipelineTraceRequirementChecks
                        .hasRequiredFlowIpcV1_56FrontendResourcePipelineBasicClosureTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_57Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_57InputComponentParityMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_58MultiValueNavigationDisclosureMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkNavigationComponentMatrix.hasMultiValueDisclosureGateSignals()
                && summary.frameworkNavigationComponentMatrix.hasFrameworkSignals()
                && hasRequiredFlowIpcV1_57InputComponentParityMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_58Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_58MultiValueNavigationDisclosureMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_59OverlayContextMenuMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkOverlayComponentMatrix.hasContextMenuGateSignals()
                && summary.frameworkOverlayComponentMatrix.hasFrameworkSignals()
                && hasRequiredFlowIpcV1_58MultiValueNavigationDisclosureMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_59Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_59OverlayContextMenuMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_60ContentAppShellLoginMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkContentAppShellLoginMatrix.hasAppShellLoginGateSignals()
                && summary.frameworkContentAppShellLoginMatrix.hasFrameworkSignals()
                && hasRequiredFlowIpcV1_59OverlayContextMenuMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_60Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_60ContentAppShellLoginMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_61LifecycleFocusBehaviorMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkLifecycleFocusBehaviorMatrix.hasFrameworkSignals()
                && hasRequiredFlowIpcV1_60ContentAppShellLoginMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_61Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_61LifecycleFocusBehaviorMatrixTypes(summary);
    }
}
