package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendResourceClosureTraceFields {
    boolean domObserved;

    static boolean containsFrontendResourcePipelineBasicClosureV156Dom(String line) {
        return containsStringField(line, "frontendResourcePipelineBasicClosureVersion",
                        "flow-ipc-frontend-resource-pipeline-basic-closure-v1_56")
                && containsStringField(line, "frontendResourcePipelineBasicClosureScope",
                        "framework-validation-app-basic-frontend-resource-pipeline")
                && containsStringField(line, "frontendResourcePipelineBasicClosureTransport",
                        "flow-ipc-no-localhost")
                && containsStringField(line, "frontendResourcePipelineBasicClosureAllowedSchemes",
                        "vatori-resource,blob")
                && line.contains("generated-imports-owner-runtime-contract-v1_42")
                && line.contains("packaging-runtime-handoff-reuse-v1_45")
                && line.contains("download-handler-save-open-v1_55")
                && line.contains("component-frontend-resources")
                && line.contains("upload-element-request-handler")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureStaticFrontendClosed", "true")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureComponentRuntimeClosed", "true")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureDynamicResourceClosed", "true")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureUploadDownloadClosed", "true")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureNativeCapabilityClosed", "true")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureNoLocalhost", "true")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureFixtureSubstituted", "false")
                && containsLiteralField(line,
                        "frontendResourcePipelineBasicClosureComplete", "true")
                && containsStringField(line,
                        "frontendResourcePipelineBasicClosureFailureLayer", "");
    }
}
