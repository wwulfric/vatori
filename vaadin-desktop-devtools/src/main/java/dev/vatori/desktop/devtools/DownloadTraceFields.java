package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class DownloadTraceFields {
    boolean saveOpenV155ManifestObserved;
    boolean saveOpenV155RequestObserved;
    boolean saveOpenV155ResponseObserved;
    boolean saveOpenV155CapabilityResultObserved;
    boolean saveOpenV155DomObserved;
    boolean saveOpenBootstrapObserved;
    boolean saveOpenDomObserved;

    boolean hasSaveOpenSignals() {
        return saveOpenBootstrapObserved && saveOpenDomObserved;
    }

    boolean hasSaveOpenGateSignals() {
        return saveOpenV155ManifestObserved
                && saveOpenV155RequestObserved
                && saveOpenV155ResponseObserved
                && saveOpenV155CapabilityResultObserved
                && saveOpenV155DomObserved;
    }

    static boolean containsDownloadHandlerSaveOpenV155Fields(String line) {
        return containsStringField(line, "downloadHandlerSaveOpenVersion",
                        "flow-ipc-download-handler-save-open-v1_55")
                && containsStringField(line, "downloadHandlerSaveOpenSource",
                        "flow-ipc-upload-multipart-v1_54")
                && containsStringField(line, "downloadHandlerSaveOpenScope",
                        "framework-validation-app-downloadhandler-native-save-open-grant")
                && containsStringField(line, "downloadHandlerSaveOpenAppShape",
                        "framework-validation-app")
                && line.contains("com.vaadin.flow.server.streams.DownloadHandler")
                && line.contains("com.vaadin.flow.server.streams.DownloadResponse")
                && line.contains("StreamResourceRegistry.ElementStreamResource")
                && containsStringField(line, "downloadHandlerSaveOpenComponentIds",
                        "framework-download-handler-save-open,framework-download-handler-save-open-status")
                && containsStringField(line, "downloadHandlerSaveOpenDynamicPathPrefix",
                        "VAADIN/dynamic/resource/")
                && containsStringField(line, "downloadHandlerSaveOpenExpectedFile",
                        "flow-ipc-download-handler-v1_55.txt")
                && containsStringField(line, "downloadHandlerSaveOpenExpectedContentType",
                        "text/plain; charset=UTF-8")
                && containsStringField(line, "downloadHandlerSaveOpenExpectedBodyMarker",
                        "Flow IPC DownloadHandler v1.55")
                && containsStringField(line, "downloadHandlerSaveOpenTransport",
                        "ipc-resource-request-downloadhandler-to-fileTransfer.download")
                && containsStringFieldPrefix(line, "downloadHandlerSaveOpenSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "downloadHandlerSaveOpenSelectedArtifactCount")
                && containsStringField(line, "downloadHandlerSaveOpenGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "downloadHandlerSaveOpenMavenGoal", "build-frontend")
                && containsStringField(line, "downloadHandlerSaveOpenBuildStatus", "success")
                && containsLiteralField(line, "downloadHandlerSaveOpenFrameworkAppRequired", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenFrameworkAppObserved", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenCliContractEnabled", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenManifestReady", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenRuntimeHandoffRequired", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenRuntimeHandoffReady", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenPathRedacted", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenSecurityScoped", "false")
                && containsLiteralField(line, "downloadHandlerSaveOpenNoLocalhost", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenFixtureSubstituted", "false")
                && line.contains("downloadHandlerSaveOpenAcceptableFailures")
                && line.contains("downloadHandlerSaveOpenRemainingBlockers")
                && line.contains("downloadHandlerSaveOpenNextResolutionStep");
    }

    static boolean containsDownloadHandlerSaveOpenV155Response(String line) {
        return containsStringField(line, "downloadHandlerVersion",
                        "flow-ipc-download-handler-save-open-v1_55")
                && containsStringField(line, "artifactFamily",
                        "flow-download-handler-save-open")
                && containsStringField(line, "requestPurpose",
                        "framework-download-handler-save-open-v1_55")
                && containsStringField(line, "lookupKind",
                        "Flow-DownloadHandler-save-open-v1_55")
                && containsStringField(line, "downloadHandlerRegistrySource",
                        "VaadinSession.StreamResourceRegistry")
                && line.contains("ElementStreamResource")
                && line.contains("DownloadHandler")
                && containsStringField(line, "downloadHandlerTransport",
                        "ipc-resource-request-downloadhandler")
                && containsStringField(line, "downloadHandlerStatus", "resolved")
                && containsPositiveIntegerField(line, "downloadHandlerResponseStatusCode")
                && containsStringField(line, "downloadHandlerFileName",
                        "flow-ipc-download-handler-v1_55.txt")
                && containsStringField(line, "downloadHandlerContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "downloadHandlerBytes")
                && line.contains("downloadHandlerSha256")
                && line.contains("downloadHandlerBodyBase64")
                && line.contains("Flow IPC DownloadHandler v1.55")
                && containsLiteralField(line, "dynamicDownloadHandlerResource", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenRequested", "true")
                && containsStringField(line, "downloadHandlerSaveTargetPath",
                        ".build/vaadin-desktop-host-swift/downloads/flow-ipc-download-handler-v1_55.txt")
                && containsLiteralField(line, "downloadHandlerOpenAfterSaveRequested", "true")
                && containsLiteralField(line, "downloadHandlerNoLocalhost", "true")
                && containsLiteralField(line, "downloadHandlerFixtureSubstituted", "false")
                && line.contains("VAADIN/dynamic/resource/");
    }

    static boolean containsDownloadHandlerSaveOpenV155CapabilityResult(String line) {
        return containsStringField(line, "downloadHandlerSaveOpenVersion",
                        "flow-ipc-download-handler-save-open-v1_55")
                && containsStringField(line, "command", "fileTransfer.download")
                && containsStringField(line, "operation", "download")
                && containsStringField(line, "downloadHandlerGrantType", "controlled-test-path")
                && containsStringField(line, "downloadHandlerOpenAfterSaveResult",
                        "classified-not-opened-in-auto-smoke")
                && containsStringField(line, "downloadHandlerSavePathRedacted",
                        "$REPO/.build/vaadin-desktop-host-swift/downloads/flow-ipc-download-handler-v1_55.txt")
                && containsLiteralField(line, "success", "true")
                && containsLiteralField(line, "bodyRedacted", "true")
                && containsLiteralField(line, "downloadHandlerBodyVerifiedByHost", "true")
                && containsLiteralField(line, "downloadHandlerSecurityScoped", "false")
                && containsLiteralField(line, "downloadHandlerNoLocalhost", "true")
                && containsLiteralField(line, "downloadHandlerFixtureSubstituted", "false")
                && containsPositiveIntegerField(line, "byteCount")
                && containsPositiveIntegerField(line, "downloadHandlerResourceBytes")
                && line.contains("permissionMode")
                && line.contains("permissionDecision")
                && line.contains("flow-ipc-download-handler-v1_55.txt");
    }

    static boolean containsDownloadHandlerSaveOpenV155Dom(String line) {
        return containsStringField(line, "downloadHandlerSaveOpenVersion",
                        "flow-ipc-download-handler-save-open-v1_55")
                && line.contains("VAADIN/dynamic/resource/")
                && containsStringField(line, "downloadHandlerSaveOpenFileName",
                        "flow-ipc-download-handler-v1_55.txt")
                && containsStringField(line, "downloadHandlerSaveOpenContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "downloadHandlerSaveOpenBytes")
                && line.contains("downloadHandlerSaveOpenSha256")
                && line.contains("ElementStreamResource")
                && line.contains("DownloadHandler")
                && containsPositiveIntegerField(line, "downloadHandlerSaveOpenResponseStatusCode")
                && containsLiteralField(line, "downloadHandlerSaveOpenAnchorHrefObserved", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenRequestObserved", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenResponseObserved", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenCapabilityResultObserved", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenBodyVerified", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenFileWritten", "true")
                && containsPositiveIntegerField(line, "downloadHandlerSaveOpenByteCount")
                && containsLiteralField(line, "downloadHandlerSaveOpenBodyRedacted", "true")
                && containsStringField(line, "downloadHandlerSaveOpenGrantType", "controlled-test-path")
                && containsStringField(line, "downloadHandlerSaveOpenPathRedacted",
                        "$REPO/.build/vaadin-desktop-host-swift/downloads/flow-ipc-download-handler-v1_55.txt")
                && containsStringField(line, "downloadHandlerSaveOpenAfterSaveResult",
                        "classified-not-opened-in-auto-smoke")
                && containsLiteralField(line, "downloadHandlerSaveOpenNoLocalhost", "true")
                && containsLiteralField(line, "downloadHandlerSaveOpenFixtureSubstituted", "false")
                && line.contains("Flow IPC DownloadHandler v1.55")
                && containsStringField(line, "frameworkDownloadHandlerSaveOpenVersion",
                        "flow-ipc-download-handler-save-open-v1_55")
                && containsLiteralField(line, "frameworkDownloadHandlerSaveOpenRendered", "true")
                && containsLiteralField(line,
                        "frameworkDownloadHandlerSaveOpenAnchorHrefObserved", "true")
                && containsLiteralField(line,
                        "frameworkDownloadHandlerSaveOpenRequestObserved", "true")
                && containsLiteralField(line,
                        "frameworkDownloadHandlerSaveOpenResponseObserved", "true")
                && containsLiteralField(line,
                        "frameworkDownloadHandlerSaveOpenCapabilityResultObserved", "true")
                && containsLiteralField(line,
                        "frameworkDownloadHandlerSaveOpenBodyVerified", "true")
                && containsLiteralField(line,
                        "frameworkDownloadHandlerSaveOpenFileWritten", "true")
                && containsLiteralField(line, "frameworkDownloadHandlerSaveOpenComplete", "true");
    }
}
