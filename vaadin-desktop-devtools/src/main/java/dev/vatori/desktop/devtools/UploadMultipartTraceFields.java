package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class UploadMultipartTraceFields {
    private UploadMultipartTraceFields() {
    }

    static boolean containsUploadMultipartV154Fields(String line) {
        return containsStringField(line, "uploadMultipartVersion",
                        "flow-ipc-upload-multipart-v1_54")
                && containsStringField(line, "uploadMultipartSource",
                        "flow-ipc-upload-receiver-v1_53")
                && containsStringField(line, "uploadMultipartScope",
                        "framework-validation-app-upload-element-request-handler-bounded-multipart-progress-cancel")
                && containsStringField(line, "uploadMultipartAppShape",
                        "framework-validation-app")
                && line.contains("jakarta.servlet.http.Part")
                && containsStringField(line, "uploadMultipartComponentIds",
                        "framework-upload,framework-upload-status")
                && containsStringField(line, "uploadMultipartDynamicPathPrefix",
                        "VAADIN/dynamic/resource/")
                && containsStringField(line, "uploadMultipartExpectedFile",
                        "flow-ipc-upload-multipart-v1_54.txt")
                && containsStringField(line, "uploadMultipartExpectedContentType",
                        "text/plain; charset=UTF-8")
                && containsStringField(line, "uploadMultipartExpectedBodyMarker",
                        "Flow IPC Upload Multipart v1.54")
                && containsStringField(line, "uploadMultipartTransport",
                        "ipc-upload-request-multipart-form-data")
                && containsStringFieldPrefix(line, "uploadMultipartSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "uploadMultipartSelectedArtifactCount")
                && containsStringField(line, "uploadMultipartGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "uploadMultipartMavenGoal", "build-frontend")
                && containsStringField(line, "uploadMultipartBuildStatus", "success")
                && containsLiteralField(line, "uploadMultipartFrameworkAppRequired", "true")
                && containsLiteralField(line, "uploadMultipartFrameworkAppObserved", "true")
                && containsLiteralField(line, "uploadMultipartCliContractEnabled", "true")
                && containsLiteralField(line, "uploadMultipartManifestReady", "true")
                && containsLiteralField(line, "uploadMultipartRuntimeHandoffRequired", "true")
                && containsLiteralField(line, "uploadMultipartRuntimeHandoffReady", "true")
                && containsLiteralField(line, "uploadMultipartNoLocalhost", "true")
                && containsLiteralField(line, "uploadMultipartFixtureSubstituted", "false")
                && containsLiteralField(line, "uploadMultipartParity", "true")
                && line.contains("uploadMultipartAcceptableFailures")
                && line.contains("uploadMultipartRemainingBlockers")
                && line.contains("uploadMultipartNextResolutionStep");
    }

    static boolean containsUploadMultipartV154Response(String line) {
        return containsStringField(line, "uploadMultipartVersion",
                        "flow-ipc-upload-multipart-v1_54")
                && containsStringField(line, "artifactFamily", "flow-upload-receiver")
                && containsStringField(line, "requestPurpose",
                        "framework-upload-multipart-v1_54")
                && containsStringField(line, "lookupKind",
                        "Flow-Upload-ElementRequestHandler-multipart-v1_54")
                && containsStringField(line, "uploadMultipartRequestBodyMode",
                        "multipart-form-data")
                && containsStringField(line, "uploadMultipartBoundary",
                        "----VatoriFlowIpcUploadMultipartV154")
                && containsStringField(line, "uploadMultipartFileName",
                        "flow-ipc-upload-multipart-v1_54.txt")
                && containsStringField(line, "uploadMultipartFileContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "uploadMultipartFileBytes")
                && line.contains("uploadMultipartFileSha256")
                && containsPositiveIntegerField(line, "uploadMultipartBytes")
                && line.contains("uploadMultipartSha256")
                && containsPositiveIntegerField(line, "uploadMultipartPartCount")
                && line.contains("ElementStreamResource")
                && line.contains("UploadHandler")
                && containsStringField(line, "uploadMultipartStatus", "resolved")
                && containsPositiveIntegerField(line, "uploadMultipartResponseStatusCode")
                && containsLiteralField(line, "uploadMultipartPatchContainsChanges", "true")
                && containsLiteralField(line, "uploadMultipartPatchContainsStatus", "true")
                && containsStringField(line, "uploadMultipartProgressTerminalStatus", "completed")
                && containsPositiveIntegerField(line, "uploadMultipartProgressTotalBytes")
                && containsPositiveIntegerField(line, "uploadMultipartProgressDeliveredBytes")
                && containsLiteralField(line, "uploadMultipartProgressConsumedByVaadinClient", "false")
                && containsLiteralField(line, "uploadMultipartCancelProbeAttempted", "true")
                && containsStringField(line, "uploadMultipartCancelProbeResult",
                        "classified-not-issued-after-success")
                && containsStringField(line, "uploadMultipartCancelFailureLayer",
                        "upload-client-cancel-event-not-wired-to-no-http-bridge-yet")
                && containsLiteralField(line, "uploadMultipartNoLocalhost", "true")
                && containsLiteralField(line, "uploadMultipartFixtureSubstituted", "false")
                && containsLiteralField(line, "uploadMultipartParity", "true")
                && containsLiteralField(line, "uploadReceiverRequestBodyBase64Observed", "true")
                && line.contains("VAADIN/dynamic/resource/");
    }

    static boolean containsUploadMultipartV154Dom(String line) {
        return containsStringField(line, "uploadMultipartVersion",
                        "flow-ipc-upload-multipart-v1_54")
                && line.contains("VAADIN/dynamic/resource/")
                && containsStringField(line, "uploadMultipartFileName",
                        "flow-ipc-upload-multipart-v1_54.txt")
                && containsStringField(line, "uploadMultipartFileContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "uploadMultipartFileBytes")
                && line.contains("uploadMultipartFileSha256")
                && containsPositiveIntegerField(line, "uploadMultipartBytes")
                && line.contains("uploadMultipartSha256")
                && containsStringField(line, "uploadMultipartBoundary",
                        "----VatoriFlowIpcUploadMultipartV154")
                && containsPositiveIntegerField(line, "uploadMultipartPartCount")
                && line.contains("ElementStreamResource")
                && line.contains("UploadHandler")
                && containsPositiveIntegerField(line, "uploadMultipartResponseStatusCode")
                && containsLiteralField(line, "uploadMultipartTargetHrefObserved", "true")
                && containsLiteralField(line, "uploadMultipartRequestObserved", "true")
                && containsLiteralField(line, "uploadMultipartResponseObserved", "true")
                && containsLiteralField(line, "uploadMultipartPatchObserved", "true")
                && containsLiteralField(line, "uploadMultipartBodyVerified", "true")
                && containsStringField(line, "uploadMultipartProgressTerminalStatus", "completed")
                && containsPositiveIntegerField(line, "uploadMultipartProgressTotalBytes")
                && containsPositiveIntegerField(line, "uploadMultipartProgressDeliveredBytes")
                && containsLiteralField(line, "uploadMultipartProgressConsumedByVaadinClient", "false")
                && containsLiteralField(line, "uploadMultipartCancelProbeAttempted", "true")
                && containsStringField(line, "uploadMultipartCancelProbeResult",
                        "classified-not-issued-after-success")
                && containsStringField(line, "uploadMultipartCancelFailureLayer",
                        "upload-client-cancel-event-not-wired-to-no-http-bridge-yet")
                && containsLiteralField(line, "uploadMultipartNoLocalhost", "true")
                && containsLiteralField(line, "uploadMultipartFixtureSubstituted", "false")
                && containsLiteralField(line, "uploadMultipartParity", "true")
                && line.contains("Multipart upload received: flow-ipc-upload-multipart-v1_54.txt")
                && line.contains("marker=true")
                && containsStringField(line, "frameworkUploadMultipartVersion",
                        "flow-ipc-upload-multipart-v1_54")
                && containsLiteralField(line, "frameworkUploadMultipartRendered", "true")
                && containsLiteralField(line, "frameworkUploadMultipartTargetHrefObserved",
                        "true")
                && containsLiteralField(line, "frameworkUploadMultipartRequestObserved", "true")
                && containsLiteralField(line, "frameworkUploadMultipartResponseObserved",
                        "true")
                && containsLiteralField(line, "frameworkUploadMultipartPatchObserved", "true")
                && containsLiteralField(line, "frameworkUploadMultipartBodyVerified", "true")
                && containsLiteralField(line, "frameworkUploadMultipartComplete", "true");
    }
}
