package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.GridResourceTraceFields.*;
import static dev.vatori.desktop.devtools.StreamResourceTraceFields.*;
import static dev.vatori.desktop.devtools.UploadTraceFields.*;
import static dev.vatori.desktop.devtools.DownloadTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkResourceTraceInspector {
    private FrameworkResourceTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceRequest\"")
                && containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-v1_50")
                && containsStringField(line, "purpose", "framework-stream-resource-v1_50")
                && line.contains("VAADIN/dynamic/resource/")
                && line.contains("flow-ipc-framework-v1_50.txt")) {
            summary.streamResources.basicV150RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && containsStreamResourceV150Response(line)) {
            summary.streamResources.basicV150ResponseObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-range-v1_51")
                && containsStringField(line, "purpose", "framework-stream-resource-range-v1_51")
                && containsStringField(line, "streamResourceRangeHeader", "bytes=0-255")
                && line.contains("VAADIN/dynamic/resource/")
                && line.contains("flow-ipc-framework-v1_51.bin")) {
            summary.streamResources.rangeV151RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && containsStreamResourceRangeV151Response(line)) {
            summary.streamResources.rangeV151ResponseObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-chunks-v1_52")
                && containsStringField(line, "purpose", "framework-stream-resource-chunks-v1_52")
                && containsStringField(line, "streamResourceRangeHeader", "bytes=0-255")
                && containsLiteralField(line, "streamResourceChunkedDownload", "true")
                && line.contains("VAADIN/dynamic/resource/")
                && line.contains("flow-ipc-framework-v1_51.bin")) {
            summary.streamResources.chunksV152RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && containsStreamResourceChunksV152Response(line)) {
            summary.streamResources.chunksV152ResponseObserved = true;
        }
        if (line.contains("\"type\":\"uploadRequest\"")
                && containsStringField(line, "uploadReceiverVersion",
                        "flow-ipc-upload-receiver-v1_53")
                && containsStringField(line, "purpose", "framework-upload-receiver-v1_53")
                && containsStringField(line, "uploadFileName",
                        "flow-ipc-upload-v1_53.txt")
                && containsStringField(line, "uploadContentType",
                        "text/plain; charset=UTF-8")
                && containsLiteralField(line, "dynamicUploadResource", "true")
                && containsPositiveIntegerField(line, "uploadBodyBytes")
                && line.contains("VAADIN/dynamic/resource/")) {
            summary.uploads.receiverV153RequestObserved = true;
        }
        if (line.contains("\"type\":\"uploadResponse\"")
                && containsUploadReceiverV153Response(line)) {
            summary.uploads.receiverV153ResponseObserved = true;
        }
        if (line.contains("\"type\":\"uploadRequest\"")
                && containsStringField(line, "uploadMultipartVersion",
                        "flow-ipc-upload-multipart-v1_54")
                && containsStringField(line, "purpose", "framework-upload-multipart-v1_54")
                && containsStringField(line, "uploadBodyMode", "multipart-form-data")
                && containsStringField(line, "uploadFileName",
                        "flow-ipc-upload-multipart-v1_54.txt")
                && containsStringField(line, "uploadFileContentType",
                        "text/plain; charset=UTF-8")
                && containsStringField(line, "uploadMultipartBoundary",
                        "----VatoriFlowIpcUploadMultipartV154")
                && containsPositiveIntegerField(line, "uploadFileBytes")
                && containsPositiveIntegerField(line, "uploadMultipartBytes")
                && containsPositiveIntegerField(line, "uploadProgressTotalBytes")
                && containsPositiveIntegerField(line, "uploadProgressDeliveredBytes")
                && containsLiteralField(line, "uploadCancelProbeAttempted", "true")
                && line.contains("uploadFileSha256")
                && line.contains("VAADIN/dynamic/resource/")) {
            summary.uploads.multipartV154RequestObserved = true;
        }
        if (line.contains("\"type\":\"uploadResponse\"")
                && containsUploadMultipartV154Response(line)) {
            summary.uploads.multipartV154ResponseObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && containsStringField(line, "downloadHandlerVersion",
                        "flow-ipc-download-handler-save-open-v1_55")
                && containsStringField(line, "purpose",
                        "framework-download-handler-save-open-v1_55")
                && containsStringField(line, "downloadSaveTargetPath",
                        ".build/vaadin-desktop-host-swift/downloads/flow-ipc-download-handler-v1_55.txt")
                && containsLiteralField(line, "dynamicDownloadHandlerResource", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpen", "true")
                && line.contains("VAADIN/dynamic/resource/")
                && line.contains("flow-ipc-download-handler-v1_55.txt")) {
            summary.downloads.saveOpenV155RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && containsDownloadHandlerSaveOpenV155Response(line)) {
            summary.downloads.saveOpenV155ResponseObserved = true;
        }
        if (line.contains("\"type\":\"capabilityResult\"")
                && containsDownloadHandlerSaveOpenV155CapabilityResult(line)) {
            summary.downloads.saveOpenV155CapabilityResultObserved = true;
        }
    }
}
