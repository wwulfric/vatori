package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class UploadReceiverTraceFields {
    private UploadReceiverTraceFields() {
    }

    static boolean containsUploadReceiverV153Fields(String line) {
        return containsStringField(line, "uploadReceiverVersion",
                        "flow-ipc-upload-receiver-v1_53")
                && containsStringField(line, "uploadReceiverSource",
                        "stream-resource-chunks-v1_52")
                && containsStringField(line, "uploadReceiverScope",
                        "framework-validation-app-upload-element-request-handler-bounded-raw-body")
                && containsStringField(line, "uploadReceiverAppShape",
                        "framework-validation-app")
                && containsStringField(line, "uploadReceiverJavaClasses",
                        "com.vaadin.flow.component.upload.Upload,com.vaadin.flow.server.streams.UploadHandler,com.vaadin.flow.server.StreamResourceRegistry.ElementStreamResource")
                && containsStringField(line, "uploadReceiverComponentIds",
                        "framework-upload,framework-upload-status")
                && containsStringField(line, "uploadReceiverDynamicPathPrefix",
                        "VAADIN/dynamic/resource/")
                && containsStringField(line, "uploadReceiverExpectedFile",
                        "flow-ipc-upload-v1_53.txt")
                && containsStringField(line, "uploadReceiverExpectedContentType",
                        "text/plain; charset=UTF-8")
                && containsStringField(line, "uploadReceiverExpectedBodyMarker",
                        "Flow IPC Upload v1.53")
                && containsStringField(line, "uploadReceiverTransport",
                        "ipc-upload-request-raw-xhr-compatible")
                && containsStringFieldPrefix(line, "uploadReceiverSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "uploadReceiverSelectedArtifactCount")
                && containsStringField(line, "uploadReceiverGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "uploadReceiverGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line, "uploadReceiverProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("uploadReceiverProductionSupportSha256")
                && containsPositiveIntegerField(line, "uploadReceiverProductionSupportBytes")
                && line.contains("uploadReceiverProductionEntryArtifact")
                && containsStringField(line, "uploadReceiverMavenGoal", "build-frontend")
                && containsStringField(line, "uploadReceiverBuildStatus", "success")
                && line.contains("uploadReceiverOutputUsability")
                && containsLiteralField(line, "uploadReceiverFrameworkAppRequired", "true")
                && containsLiteralField(line, "uploadReceiverFrameworkAppObserved", "true")
                && containsLiteralField(line, "uploadReceiverCliContractEnabled", "true")
                && containsLiteralField(line, "uploadReceiverManifestReady", "true")
                && containsLiteralField(line, "uploadReceiverRuntimeHandoffRequired", "true")
                && containsLiteralField(line, "uploadReceiverRuntimeHandoffReady", "true")
                && containsLiteralField(line, "uploadReceiverMultipartParity", "false")
                && containsLiteralField(line, "uploadReceiverNoLocalhost", "true")
                && containsLiteralField(line, "uploadReceiverFixtureSubstituted", "false")
                && line.contains("uploadReceiverAcceptableFailures")
                && line.contains("uploadReceiverRemainingBlockers")
                && line.contains("uploadReceiverNextResolutionStep");
    }

    static boolean containsUploadReceiverV153Response(String line) {
        return containsStringField(line, "uploadReceiverVersion",
                        "flow-ipc-upload-receiver-v1_53")
                && containsStringField(line, "artifactFamily", "flow-upload-receiver")
                && containsStringField(line, "requestPurpose",
                        "framework-upload-receiver-v1_53")
                && containsStringField(line, "lookupKind",
                        "Flow-Upload-ElementRequestHandler-receiver-v1_53")
                && containsStringField(line, "uploadReceiverRegistrySource",
                        "VaadinSession.StreamResourceRegistry")
                && line.contains("ElementStreamResource")
                && line.contains("UploadHandler")
                && containsStringField(line, "uploadReceiverTransport",
                        "ipc-upload-request-raw-xhr-compatible")
                && containsStringField(line, "uploadReceiverStatus", "resolved")
                && containsStringField(line, "uploadReceiverFileName",
                        "flow-ipc-upload-v1_53.txt")
                && containsStringField(line, "uploadReceiverContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "uploadReceiverBytes")
                && line.contains("uploadReceiverSha256")
                && containsPositiveIntegerField(line, "uploadReceiverResponseStatusCode")
                && containsLiteralField(line, "uploadReceiverPatchContainsChanges", "true")
                && containsLiteralField(line, "uploadReceiverPatchContainsStatus", "true")
                && containsLiteralField(line, "uploadReceiverNoLocalhost", "true")
                && containsLiteralField(line, "uploadReceiverFixtureSubstituted", "false")
                && containsLiteralField(line, "uploadReceiverMultipartParity", "false")
                && containsLiteralField(line, "uploadReceiverRequestBodyBase64Observed", "true")
                && containsLiteralField(line, "uploadReceiverRequestBodyRedacted", "false")
                && line.contains("VAADIN/dynamic/resource/");
    }

    static boolean containsUploadReceiverV153Dom(String line) {
        return containsStringField(line, "uploadReceiverVersion",
                        "flow-ipc-upload-receiver-v1_53")
                && line.contains("VAADIN/dynamic/resource/")
                && containsStringField(line, "uploadReceiverFileName",
                        "flow-ipc-upload-v1_53.txt")
                && containsStringField(line, "uploadReceiverContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "uploadReceiverBytes")
                && line.contains("uploadReceiverSha256")
                && line.contains("ElementStreamResource")
                && line.contains("UploadHandler")
                && containsPositiveIntegerField(line, "uploadReceiverResponseStatusCode")
                && containsLiteralField(line, "uploadReceiverTargetHrefObserved", "true")
                && containsLiteralField(line, "uploadReceiverRequestObserved", "true")
                && containsLiteralField(line, "uploadReceiverResponseObserved", "true")
                && containsLiteralField(line, "uploadReceiverPatchObserved", "true")
                && containsLiteralField(line, "uploadReceiverBodyVerified", "true")
                && containsLiteralField(line, "uploadReceiverNoLocalhost", "true")
                && containsLiteralField(line, "uploadReceiverFixtureSubstituted", "false")
                && containsLiteralField(line, "uploadReceiverMultipartParity", "false")
                && line.contains("Upload received: flow-ipc-upload-v1_53.txt")
                && line.contains("marker=true")
                && containsStringField(line, "frameworkUploadReceiverVersion",
                        "flow-ipc-upload-receiver-v1_53")
                && containsLiteralField(line, "frameworkUploadReceiverRendered", "true")
                && containsLiteralField(line, "frameworkUploadReceiverTargetHrefObserved",
                        "true")
                && containsLiteralField(line, "frameworkUploadReceiverRequestObserved", "true")
                && containsLiteralField(line, "frameworkUploadReceiverResponseObserved",
                        "true")
                && containsLiteralField(line, "frameworkUploadReceiverPatchObserved", "true")
                && containsLiteralField(line, "frameworkUploadReceiverBodyVerified", "true")
                && containsLiteralField(line, "frameworkUploadReceiverComplete", "true");
    }
}
