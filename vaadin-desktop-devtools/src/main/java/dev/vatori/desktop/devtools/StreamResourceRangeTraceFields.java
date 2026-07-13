package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class StreamResourceRangeTraceFields {
    private StreamResourceRangeTraceFields() {
    }

    static boolean containsStreamResourceRangeV151Fields(String line) {
        return containsStringField(line, "streamResourceRangeVersion",
                        "flow-ipc-stream-resource-range-v1_51")
                && containsStringField(line, "streamResourceRangeSource",
                        "stream-resource-v1_50")
                && containsStringField(line, "streamResourceRangeScope",
                        "framework-validation-app-binary-stream-resource-range-download")
                && containsStringField(line, "streamResourceRangeAppShape",
                        "framework-validation-app")
                && containsStringField(line, "streamResourceRangeJavaClasses",
                        "com.vaadin.flow.component.html.Anchor,com.vaadin.flow.server.StreamResource,com.vaadin.flow.server.StreamResourceRegistry")
                && containsStringField(line, "streamResourceRangeComponentIds",
                        "framework-stream-resource-range-download,framework-stream-resource-range-status")
                && containsStringField(line, "streamResourceRangeDynamicPathPrefix",
                        "VAADIN/dynamic/resource/")
                && containsStringField(line, "streamResourceRangeExpectedFile",
                        "flow-ipc-framework-v1_51.bin")
                && containsStringField(line, "streamResourceRangeExpectedContentType",
                        "application/octet-stream")
                && containsStringField(line, "streamResourceRangeExpectedBodyMarker",
                        "Flow IPC Binary StreamResource v1.51")
                && containsStringField(line, "streamResourceRangeHeader", "bytes=0-255")
                && containsStringFieldPrefix(line, "streamResourceRangeSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "streamResourceRangeSelectedArtifactCount")
                && containsStringField(line, "streamResourceRangeGeneratedImportsOwnerSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "streamResourceRangeGeneratedImportsOwnerArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line, "streamResourceRangeProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("streamResourceRangeProductionSupportSha256")
                && containsPositiveIntegerField(line, "streamResourceRangeProductionSupportBytes")
                && line.contains("streamResourceRangeProductionEntryArtifact")
                && containsStringField(line, "streamResourceRangeMavenGoal", "build-frontend")
                && containsStringField(line, "streamResourceRangeBuildStatus", "success")
                && line.contains("streamResourceRangeOutputUsability")
                && containsLiteralField(line, "streamResourceRangeFrameworkAppRequired", "true")
                && containsLiteralField(line, "streamResourceRangeFrameworkAppObserved", "true")
                && containsLiteralField(line, "streamResourceRangeCliContractEnabled", "true")
                && containsLiteralField(line, "streamResourceRangeManifestReady", "true")
                && containsLiteralField(line, "streamResourceRangeRuntimeHandoffRequired", "true")
                && containsLiteralField(line, "streamResourceRangeRuntimeHandoffReady", "true")
                && containsLiteralField(line, "streamResourceRangeFullBodyOmitted", "true")
                && containsStringField(line, "streamResourceRangeResponseMode", "range-slice")
                && containsLiteralField(line, "streamResourceRangeNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceRangeFixtureSubstituted", "false")
                && line.contains("streamResourceRangeAcceptableFailures")
                && line.contains("streamResourceRangeRemainingBlockers")
                && line.contains("streamResourceRangeNextResolutionStep");
    }

    static boolean containsStreamResourceRangeV151Response(String line) {
        return containsStringField(line, "streamResourceVersion",
                        "flow-ipc-stream-resource-range-v1_51")
                && containsStringField(line, "artifactFamily", "flow-stream-resource-range")
                && containsStringField(line, "streamResourceStatus", "partial-content")
                && containsStringField(line, "streamResourceFileName",
                        "flow-ipc-framework-v1_51.bin")
                && containsStringField(line, "streamResourceContentType",
                        "application/octet-stream")
                && containsStringField(line, "streamResourceRangeHeader", "bytes=0-255")
                && containsPositiveIntegerField(line, "streamResourceRangeBytes")
                && containsPositiveIntegerField(line, "streamResourceFullBytes")
                && line.contains("streamResourceRangeSha256")
                && line.contains("streamResourceFullSha256")
                && line.contains("streamResourceRangeBodyBase64")
                && containsLiteralField(line, "dynamicStreamResource", "true")
                && containsLiteralField(line, "streamResourceRangeRequested", "true")
                && containsLiteralField(line, "streamResourceRangeSatisfied", "true")
                && containsLiteralField(line, "streamResourceFullBodyOmitted", "true")
                && containsStringField(line, "streamResourceResponseMode", "range-slice")
                && containsLiteralField(line, "streamResourceNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceFixtureSubstituted", "false");
    }

    static boolean containsStreamResourceRangeV151Dom(String line) {
        return containsStringField(line, "streamResourceRangeVersion",
                        "flow-ipc-stream-resource-range-v1_51")
                && containsStringField(line, "streamResourceRangeFileName",
                        "flow-ipc-framework-v1_51.bin")
                && containsStringField(line, "streamResourceRangeContentType",
                        "application/octet-stream")
                && line.contains("streamResourceRangeBodySample")
                && line.contains("Flow IPC Binary StreamResource v1.51")
                && line.contains("VAADIN/dynamic/resource/")
                && containsPositiveIntegerField(line, "streamResourceRangeBytes")
                && containsPositiveIntegerField(line, "streamResourceRangeFullBytes")
                && line.contains("streamResourceRangeSha256")
                && line.contains("streamResourceRangeFullSha256")
                && containsLiteralField(line, "streamResourceRangeAnchorHrefObserved", "true")
                && containsLiteralField(line, "streamResourceRangeRequestObserved", "true")
                && containsLiteralField(line, "streamResourceRangeResponseObserved", "true")
                && containsLiteralField(line, "streamResourceRangeBodyVerified", "true")
                && containsLiteralField(line, "streamResourceRangeFullBodyOmitted", "true")
                && containsStringField(line, "streamResourceRangeResponseMode", "range-slice")
                && containsLiteralField(line, "streamResourceRangeNoLocalhost", "true")
                && containsLiteralField(line, "streamResourceRangeFixtureSubstituted", "false")
                && containsLiteralField(line, "frameworkStreamResourceRangeComplete", "true");
    }
}
