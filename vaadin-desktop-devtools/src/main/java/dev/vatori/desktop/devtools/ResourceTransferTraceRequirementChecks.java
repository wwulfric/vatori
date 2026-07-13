package dev.vatori.desktop.devtools;

final class ResourceTransferTraceRequirementChecks {
    private ResourceTransferTraceRequirementChecks() {
    }

    static boolean hasRequiredFlowIpcV1_50StreamResourceTypes(TraceSummary summary) {
        return summary.streamResources.hasBasicGateSignals()
                && summary.streamResources.hasBasicSignals();
    }

    static boolean hasRequiredFlowIpcV1_51StreamResourceRangeTypes(
            TraceSummary summary) {
        return summary.streamResources.hasRangeGateSignals()
                && summary.streamResources.hasRangeSignals()
                && hasRequiredFlowIpcV1_50StreamResourceTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_52StreamResourceChunksTypes(
            TraceSummary summary) {
        return summary.streamResources.hasChunksGateSignals()
                && hasRequiredFlowIpcV1_51StreamResourceRangeTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_53UploadReceiverTypes(TraceSummary summary) {
        return summary.uploads.hasReceiverGateSignals()
                && summary.uploads.hasReceiverSignals()
                && hasRequiredFlowIpcV1_52StreamResourceChunksTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_54UploadMultipartTypes(TraceSummary summary) {
        return summary.uploads.hasMultipartGateSignals()
                && summary.uploads.hasMultipartSignals()
                && hasRequiredFlowIpcV1_53UploadReceiverTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_55DownloadSaveOpenTypes(TraceSummary summary) {
        return summary.downloads.hasSaveOpenGateSignals()
                && summary.downloads.hasSaveOpenSignals()
                && hasRequiredFlowIpcV1_54UploadMultipartTypes(summary);
    }

    static boolean hasRequiredFlowIpcV1_56FrontendResourcePipelineBasicClosureTypes(
            TraceSummary summary) {
        return summary.frontendResourceClosure.domObserved;
    }
}
