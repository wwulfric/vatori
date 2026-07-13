package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.AcceptanceGate.*;
import static java.util.Map.entry;

import java.util.Map;
import java.util.function.Predicate;

final class ComponentResourcePipelineTraceRequirementChecks {
    private static final Map<AcceptanceGate, Predicate<TraceSummary>> CHECKS = Map.ofEntries(
            entry(FLOW_IPC_V1_46_BROADER_COMPONENT_RESOURCE_MATRIX,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_46BroaderComponentResourceMatrixTypes),
            entry(FLOW_IPC_V1_46,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_46BroaderComponentResourceMatrixTypes),
            entry(FLOW_IPC_V1_47_DATA_COMPONENT_RESOURCE_MATRIX,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_47DataComponentResourceMatrixTypes),
            entry(FLOW_IPC_V1_47,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_47DataComponentResourceMatrixTypes),
            entry(FLOW_IPC_V1_48_GRID_LAZY_DATA_PROVIDER,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_48GridLazyDataProviderTypes),
            entry(FLOW_IPC_V1_48,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_48GridLazyDataProviderTypes),
            entry(FLOW_IPC_V1_49_GRID_SCROLL_VIRTUALIZATION,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_49GridScrollVirtualizationTypes),
            entry(FLOW_IPC_V1_49,
                    ComponentResourceMatrixTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_49GridScrollVirtualizationTypes),
            entry(FLOW_IPC_V1_50_STREAM_RESOURCE,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_50StreamResourceTypes),
            entry(FLOW_IPC_V1_50,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_50StreamResourceTypes),
            entry(FLOW_IPC_V1_51_STREAM_RESOURCE_RANGE,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_51StreamResourceRangeTypes),
            entry(FLOW_IPC_V1_51,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_51StreamResourceRangeTypes),
            entry(FLOW_IPC_V1_52_STREAM_RESOURCE_CHUNKS,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_52StreamResourceChunksTypes),
            entry(FLOW_IPC_V1_52,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_52StreamResourceChunksTypes),
            entry(FLOW_IPC_V1_53_UPLOAD_RECEIVER,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_53UploadReceiverTypes),
            entry(FLOW_IPC_V1_53,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_53UploadReceiverTypes),
            entry(FLOW_IPC_V1_54_UPLOAD_MULTIPART,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_54UploadMultipartTypes),
            entry(FLOW_IPC_V1_54,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_54UploadMultipartTypes),
            entry(FLOW_IPC_V1_55_DOWNLOAD_SAVE_OPEN,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_55DownloadSaveOpenTypes),
            entry(FLOW_IPC_V1_55,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_55DownloadSaveOpenTypes),
            entry(FLOW_IPC_V1_56_FRONTEND_RESOURCE_PIPELINE_BASIC_CLOSURE,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_56FrontendResourcePipelineBasicClosureTypes),
            entry(FLOW_IPC_V1_56,
                    ResourceTransferTraceRequirementChecks
                            ::hasRequiredFlowIpcV1_56FrontendResourcePipelineBasicClosureTypes));

    private ComponentResourcePipelineTraceRequirementChecks() {
    }

    static boolean isSatisfied(AcceptanceGate gate, TraceSummary summary) {
        Predicate<TraceSummary> check = CHECKS.get(gate);
        if (check == null) {
            throw new IllegalArgumentException("Unsupported resource pipeline gate: " + gate);
        }
        return check.test(summary);
    }

    static boolean hasRequiredFlowIpcV1_56FrontendResourcePipelineBasicClosureTypes(
            TraceSummary summary) {
        return ResourceTransferTraceRequirementChecks
                .hasRequiredFlowIpcV1_56FrontendResourcePipelineBasicClosureTypes(summary);
    }
}
