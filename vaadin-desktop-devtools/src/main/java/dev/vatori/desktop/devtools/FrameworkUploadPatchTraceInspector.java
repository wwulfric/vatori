package dev.vatori.desktop.devtools;

final class FrameworkUploadPatchTraceInspector {
    private FrameworkUploadPatchTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"upload\"")
                && line.contains("\"eventDispatch\":\"flow-ipc-upload-resource\"")
                && line.contains("Upload received: flow-ipc-upload-v1_53.txt")
                && line.contains("marker=true")) {
            summary.uploads.receiverV153PatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"upload\"")
                && line.contains("\"eventDispatch\":\"flow-ipc-upload-resource\"")
                && line.contains("Multipart upload received: flow-ipc-upload-multipart-v1_54.txt")
                && line.contains("marker=true")) {
            summary.uploads.multipartV154PatchObserved = true;
        }
    }
}
