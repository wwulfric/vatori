package dev.vatori.desktop.devtools;

final class ComponentResourceTraceRequirementChecks {
    private ComponentResourceTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return switch (gate) {
            case FLOW_IPC_V1_46_BROADER_COMPONENT_RESOURCE_MATRIX,
                    FLOW_IPC_V1_46,
                    FLOW_IPC_V1_47_DATA_COMPONENT_RESOURCE_MATRIX,
                    FLOW_IPC_V1_47,
                    FLOW_IPC_V1_48_GRID_LAZY_DATA_PROVIDER,
                    FLOW_IPC_V1_48,
                    FLOW_IPC_V1_49_GRID_SCROLL_VIRTUALIZATION,
                    FLOW_IPC_V1_49,
                    FLOW_IPC_V1_50_STREAM_RESOURCE,
                    FLOW_IPC_V1_50,
                    FLOW_IPC_V1_51_STREAM_RESOURCE_RANGE,
                    FLOW_IPC_V1_51,
                    FLOW_IPC_V1_52_STREAM_RESOURCE_CHUNKS,
                    FLOW_IPC_V1_52,
                    FLOW_IPC_V1_53_UPLOAD_RECEIVER,
                    FLOW_IPC_V1_53,
                    FLOW_IPC_V1_54_UPLOAD_MULTIPART,
                    FLOW_IPC_V1_54,
                    FLOW_IPC_V1_55_DOWNLOAD_SAVE_OPEN,
                    FLOW_IPC_V1_55,
                    FLOW_IPC_V1_56_FRONTEND_RESOURCE_PIPELINE_BASIC_CLOSURE,
                    FLOW_IPC_V1_56 ->
                ComponentResourcePipelineTraceRequirementChecks.isSatisfied(gate, summary);
            case FLOW_IPC_V1_57_INPUT_COMPONENT_PARITY_MATRIX,
                    FLOW_IPC_V1_57,
                    FLOW_IPC_V1_58_MULTI_VALUE_NAVIGATION_DISCLOSURE_MATRIX,
                    FLOW_IPC_V1_58,
                    FLOW_IPC_V1_59_OVERLAY_CONTEXT_MENU_MATRIX,
                    FLOW_IPC_V1_59,
                    FLOW_IPC_V1_60_CONTENT_APP_SHELL_LOGIN_MATRIX,
                    FLOW_IPC_V1_60,
                    FLOW_IPC_V1_61_LIFECYCLE_FOCUS_BEHAVIOR_MATRIX,
                    FLOW_IPC_V1_61 ->
                ComponentParityTraceRequirementChecks.isSatisfied(gate, summary);
            case FLOW_IPC_V1_62_RICH_TEXT_EDITOR_MATRIX,
                    FLOW_IPC_V1_62,
                    FLOW_IPC_V1_63_RICH_TEXT_EDITOR_BEHAVIOR_MATRIX,
                    FLOW_IPC_V1_63,
                    FLOW_IPC_V1_64_RICH_TEXT_EDITOR_EDITING_MATRIX,
                    FLOW_IPC_V1_64,
                    FLOW_IPC_V1_65_NATIVE_CLIPBOARD_READ_MATRIX,
                    FLOW_IPC_V1_65 ->
                RichTextNativeTraceRequirementChecks.isSatisfied(gate, summary);
            default -> throw new IllegalArgumentException("Unsupported component/resource gate: " + gate);
        };
    }
}
