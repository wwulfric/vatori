package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class StreamResourceChunksTraceFields {
    private StreamResourceChunksTraceFields() {
    }

    static boolean containsStreamResourceChunksV152Fields(String line) {
        return containsStringField(line, "streamResourceChunksVersion",
                        "flow-ipc-stream-resource-chunks-v1_52")
                && containsStringField(line, "streamResourceChunksSource",
                        "stream-resource-range-v1_51")
                && containsStringField(line, "streamResourceChunksScope",
                        "framework-validation-app-binary-stream-resource-sequential-chunk-download")
                && containsStringField(line, "streamResourceChunksAppShape",
                        "framework-validation-app")
                && containsStringField(line, "streamResourceChunksJavaClasses",
                        "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.StreamResource,com.vaadin.flow.server.StreamResourceRegistry")
                && containsStringField(line, "streamResourceChunksComponentIds",
                        "framework-stream-resource-range-download,framework-stream-resource-range-status,framework-stream-resource-chunks-status")
                && containsStringField(line, "streamResourceChunksDynamicPathPrefix",
                        "VAADIN/dynamic/resource/")
                && containsStringField(line, "streamResourceChunksExpectedFile",
                        "flow-ipc-framework-v1_51.bin")
                && containsStringField(line, "streamResourceChunksExpectedContentType",
                        "application/octet-stream")
                && containsStringField(line, "streamResourceChunksExpectedBodyMarker",
                        "Flow IPC Binary StreamResource v1.51")
                && containsStringField(line, "streamResourceChunksRangePlan",
                        "bytes=0-255,bytes=256-511,bytes=512-767")
                && containsLiteralField(line, "streamResourceChunksChunkCount", "3")
                && containsLiteralField(line, "streamResourceChunksChunkSize", "256")
                && containsLiteralField(line, "streamResourceChunksExpectedAggregateBytes", "768")
                && containsLiteralField(line, "streamResourceChunksBackpressureWindow", "1")
                && containsStringFieldPrefix(line, "streamResourceChunksSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "streamResourceChunksSelectedArtifactCount")
                && containsStringField(line, "streamResourceChunksGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "streamResourceChunksGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line, "streamResourceChunksProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("streamResourceChunksProductionSupportSha256")
                && containsPositiveIntegerField(line, "streamResourceChunksProductionSupportBytes")
                && line.contains("streamResourceChunksProductionEntryArtifact")
                && containsStringField(line, "streamResourceChunksMavenGoal", "build-frontend")
                && containsStringField(line, "streamResourceChunksBuildStatus", "success")
                && line.contains("streamResourceChunksOutputUsability")
                && containsLiteralField(line, "streamResourceChunksFrameworkAppRequired", "true")
                && containsLiteralField(line, "streamResourceChunksFrameworkAppObserved", "true")
                && containsLiteralField(line, "streamResourceChunksCliContractEnabled", "true")
                && containsLiteralField(line, "streamResourceChunksManifestReady", "true")
                && containsLiteralField(line, "streamResourceChunksRuntimeHandoffRequired", "true")
                && containsLiteralField(line, "streamResourceChunksRuntimeHandoffReady", "true")
                && containsLiteralField(line, "streamResourceChunksFullBodyOmitted", "true")
                && containsStringField(line, "streamResourceChunksResponseMode",
                        "range-chunk-sequential")
                && containsLiteralField(line, "streamResourceChunksNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceChunksFixtureSubstituted", "false")
                && line.contains("streamResourceChunksAcceptableFailures")
                && line.contains("streamResourceChunksRemainingBlockers")
                && line.contains("streamResourceChunksNextResolutionStep");
    }

    static boolean containsStreamResourceChunksV152Response(String line) {
        return containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-chunks-v1_52")
                && containsStringField(line, "artifactFamily", "flow-stream-resource-chunks")
                && containsStringField(line, "streamResourceStatus", "partial-content")
                && containsStringField(line, "streamResourceFileName",
                        "flow-ipc-framework-v1_51.bin")
                && containsStringField(line, "streamResourceContentType",
                        "application/octet-stream")
                && containsStringField(line, "streamResourceRangeHeader", "bytes=0-255")
                && containsStringField(line, "streamResourceChunkRangeHeader", "bytes=0-255")
                && containsLiteralField(line, "streamResourceChunkedDownload", "true")
                && containsLiteralField(line, "streamResourceChunkIndex", "0")
                && containsLiteralField(line, "streamResourceChunkCount", "3")
                && containsLiteralField(line, "streamResourceChunkSize", "256")
                && containsPositiveIntegerField(line, "streamResourceRangeBytes")
                && containsPositiveIntegerField(line, "streamResourceFullBytes")
                && line.contains("streamResourceRangeSha256")
                && line.contains("streamResourceFullSha256")
                && line.contains("streamResourceRangeBodyBase64")
                && containsLiteralField(line, "dynamicStreamResource", "true")
                && containsLiteralField(line, "streamResourceRangeRequested", "true")
                && containsLiteralField(line, "streamResourceRangeSatisfied", "true")
                && containsLiteralField(line, "streamResourceFullBodyOmitted", "true")
                && containsStringField(line, "streamResourceResponseMode",
                        "range-chunk-sequential")
                && containsStringField(line, "streamResourceBackpressureEvidence",
                        "sequential-single-chunk-window-full-body-omitted")
                && containsLiteralField(line, "streamResourceNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceFixtureSubstituted", "false");
    }

    static boolean containsStreamResourceChunksV152Dom(String line) {
        return containsStringField(line, "streamResourceChunksVersion",
                        "flow-ipc-stream-resource-chunks-v1_52")
                && line.contains("VAADIN/dynamic/resource/")
                && line.contains("flow-ipc-framework-v1_51.bin")
                && line.contains("Flow IPC Binary StreamResource v1.51")
                && containsStringField(line, "streamResourceChunksRequestOrder",
                        "0:0-255,1:256-511,2:512-767")
                && containsStringField(line, "streamResourceChunksResponseOrder",
                        "0:0-255,1:256-511,2:512-767")
                && containsLiteralField(line, "streamResourceChunksVerifiedCount", "3")
                && containsLiteralField(line, "streamResourceChunksExpectedCount", "3")
                && containsLiteralField(line, "streamResourceChunksChunkSize", "256")
                && containsLiteralField(line, "streamResourceChunksAggregateBytes", "768")
                && containsPositiveIntegerField(line, "streamResourceChunksFullBytes")
                && line.contains("streamResourceChunksFullSha256")
                && containsLiteralField(line, "streamResourceChunksAnchorHrefObserved", "true")
                && containsLiteralField(line, "streamResourceChunksRequestObserved", "true")
                && containsLiteralField(line, "streamResourceChunksResponseObserved", "true")
                && containsLiteralField(line, "streamResourceChunksBodyVerified", "true")
                && containsLiteralField(line, "streamResourceChunksFullBodyOmitted", "true")
                && containsStringField(line, "streamResourceChunksResponseMode",
                        "range-chunk-sequential")
                && containsLiteralField(line, "streamResourceChunksNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceChunksFixtureSubstituted", "false")
                && containsStringField(line, "frameworkStreamResourceChunksVersion",
                        "flow-ipc-stream-resource-chunks-v1_52")
                && containsLiteralField(line, "frameworkStreamResourceChunksRendered", "true")
                && containsLiteralField(line, "frameworkStreamResourceChunksAnchorHrefObserved",
                        "true")
                && containsLiteralField(line, "frameworkStreamResourceChunksRequestObserved",
                        "true")
                && containsLiteralField(line, "frameworkStreamResourceChunksResponseObserved",
                        "true")
                && containsLiteralField(line, "frameworkStreamResourceChunksBodyVerified",
                        "true")
                && containsLiteralField(line, "frameworkStreamResourceChunksComplete", "true");
    }
}
