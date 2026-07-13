package dev.vatori.desktop.devtools;

final class RichTextNativeTraceRequirementChecks {
    private RichTextNativeTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        return switch (gate) {
            case FLOW_IPC_V1_62_RICH_TEXT_EDITOR_MATRIX ->
                hasRequiredFlowIpcV1_62RichTextEditorMatrixTypes(summary);
            case FLOW_IPC_V1_62 -> hasRequiredFlowIpcV1_62Types(summary);
            case FLOW_IPC_V1_63_RICH_TEXT_EDITOR_BEHAVIOR_MATRIX ->
                hasRequiredFlowIpcV1_63RichTextEditorBehaviorMatrixTypes(summary);
            case FLOW_IPC_V1_63 -> hasRequiredFlowIpcV1_63Types(summary);
            case FLOW_IPC_V1_64_RICH_TEXT_EDITOR_EDITING_MATRIX ->
                hasRequiredFlowIpcV1_64RichTextEditorEditingMatrixTypes(summary);
            case FLOW_IPC_V1_64 -> hasRequiredFlowIpcV1_64Types(summary);
            case FLOW_IPC_V1_65_NATIVE_CLIPBOARD_READ_MATRIX ->
                hasRequiredFlowIpcV1_65NativeClipboardReadMatrixTypes(summary);
            case FLOW_IPC_V1_65 -> hasRequiredFlowIpcV1_65Types(summary);
            default -> throw new IllegalArgumentException("Unsupported rich text/native gate: " + gate);
        };
    }

    static boolean hasRequiredFlowIpcV1_62RichTextEditorMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkRichTextEditor.hasMatrixGateSignals()
                && summary.frameworkRichTextEditor.hasMatrixSignals();
    }

    static boolean hasRequiredFlowIpcV1_62Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_62RichTextEditorMatrixTypes(summary)
                && ComponentParityTraceRequirementChecks
                        .hasRequiredFlowIpcV1_61LifecycleFocusBehaviorMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_63RichTextEditorBehaviorMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkRichTextEditor.hasBehaviorGateSignals()
                && summary.frameworkRichTextEditor.hasBehaviorSignals();
    }

    static boolean hasRequiredFlowIpcV1_63Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_63RichTextEditorBehaviorMatrixTypes(summary)
                && hasRequiredFlowIpcV1_62RichTextEditorMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_64RichTextEditorEditingMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkRichTextEditor.hasEditingGateSignals()
                && summary.frameworkRichTextEditor.hasEditingSignals();
    }

    static boolean hasRequiredFlowIpcV1_64Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_64RichTextEditorEditingMatrixTypes(summary)
                && hasRequiredFlowIpcV1_63RichTextEditorBehaviorMatrixTypes(summary)
                && hasRequiredFlowIpcV1_62RichTextEditorMatrixTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_65NativeClipboardReadMatrixTypes(
            TraceSummary summary) {
        return summary.frameworkApp.hasNativeClipboardReadMatrixSignals();
    }

    static boolean hasRequiredFlowIpcV1_65Types(TraceSummary summary) {
        return hasRequiredFlowIpcV1_65NativeClipboardReadMatrixTypes(summary)
                && hasRequiredFlowIpcV1_64Types(summary);
    }
}
