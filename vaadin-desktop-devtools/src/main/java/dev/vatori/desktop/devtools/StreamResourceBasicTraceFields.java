package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class StreamResourceBasicTraceFields {
    private StreamResourceBasicTraceFields() {
    }

    static boolean containsStreamResourceV150Fields(String line) {
        return containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-v1_50")
                && containsStringField(line, "streamResourceSource",
                        "grid-scroll-virtualization-v1_49")
                && containsStringField(line, "streamResourceScope",
                        "framework-validation-app-dynamic-stream-resource-download")
                && containsStringField(line, "streamResourceAppShape",
                        "framework-validation-app")
                && containsStringField(line, "streamResourceJavaClasses",
                        "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.StreamResource,com.vaadin.flow.server.StreamResourceRegistry")
                && containsStringField(line, "streamResourceComponentIds",
                        "framework-stream-resource-download,framework-stream-resource-status")
                && containsStringField(line, "streamResourceDynamicPathPrefix",
                        "VAADIN/dynamic/resource/")
                && containsStringField(line, "streamResourceExpectedFile",
                        "flow-ipc-framework-v1_50.txt")
                && containsStringField(line, "streamResourceExpectedContentType",
                        "text/plain; charset=UTF-8")
                && containsStringField(line, "streamResourceExpectedBodyMarker",
                        "Flow IPC StreamResource v1.50")
                && containsStringFieldPrefix(line, "streamResourceSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "streamResourceSelectedArtifactCount")
                && containsStringField(line, "streamResourceGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "streamResourceGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line, "streamResourceProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("streamResourceProductionSupportSha256")
                && containsPositiveIntegerField(line, "streamResourceProductionSupportBytes")
                && line.contains("streamResourceProductionEntryArtifact")
                && containsStringField(line, "streamResourceMavenGoal", "build-frontend")
                && containsStringField(line, "streamResourceBuildStatus", "success")
                && line.contains("streamResourceOutputUsability")
                && containsLiteralField(line, "streamResourceFrameworkAppRequired", "true")
                && containsLiteralField(line, "streamResourceFrameworkAppObserved", "true")
                && containsLiteralField(line, "streamResourceCliContractEnabled", "true")
                && containsLiteralField(line, "streamResourceManifestReady", "true")
                && containsLiteralField(line, "streamResourceRuntimeHandoffRequired", "true")
                && containsLiteralField(line, "streamResourceRuntimeHandoffReady", "true")
                && containsLiteralField(line, "streamResourceNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceFixtureSubstituted", "false")
                && line.contains("streamResourceAcceptableFailures")
                && line.contains("streamResourceRemainingBlockers")
                && line.contains("streamResourceNextResolutionStep");
    }

    static boolean containsStreamResourceV150Response(String line) {
        return containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-v1_50")
                && containsStringField(line, "artifactFamily", "flow-stream-resource")
                && containsStringField(line, "streamResourceStatus", "resolved")
                && containsStringField(line, "streamResourceFileName",
                        "flow-ipc-framework-v1_50.txt")
                && containsStringField(line, "streamResourceContentType",
                        "text/plain; charset=UTF-8")
                && containsPositiveIntegerField(line, "streamResourceBytes")
                && line.contains("streamResourceSha256")
                && line.contains("streamResourceBodyBase64")
                && line.contains("Flow IPC StreamResource v1.50")
                && containsLiteralField(line, "dynamicStreamResource", "true")
                && containsLiteralField(line, "streamResourceNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceFixtureSubstituted", "false");
    }

    static boolean containsStreamResourceV150Dom(String line) {
        return containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-v1_50")
                && containsStringField(line, "streamResourceFileName",
                        "flow-ipc-framework-v1_50.txt")
                && containsStringField(line, "streamResourceContentType",
                        "text/plain; charset=UTF-8")
                && line.contains("streamResourceBodySample")
                && line.contains("Flow IPC StreamResource v1.50")
                && line.contains("VAADIN/dynamic/resource/")
                && containsPositiveIntegerField(line, "streamResourceBytes")
                && line.contains("streamResourceSha256")
                && containsLiteralField(line, "streamResourceAnchorHrefObserved", "true")
                && containsLiteralField(line, "streamResourceRequestObserved", "true")
                && containsLiteralField(line, "streamResourceResponseObserved", "true")
                && containsLiteralField(line, "streamResourceBodyVerified", "true")
                && containsLiteralField(line, "streamResourceNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceFixtureSubstituted", "false")
                && containsLiteralField(line, "frameworkStreamResourceComplete", "true");
    }
}
