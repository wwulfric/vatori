package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendResourceSchemeTraceInspector {
    private FrontendResourceSchemeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceLoad\"")
                && containsStringField(line, "frontendResourceSchemeVersion",
                        "flow-ipc-resource-scheme-v1_17")
                && containsStringField(line, "frontendResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "frontendResourceSchemeName", "vatori-resource")
                && containsLiteralField(line, "frontendResourceSchemeHandlerInstalled", "true")
                && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")) {
            summary.frontendBuildPipeline.resourceSchemeV117HostInstallObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsStringField(line, "frontendResourceSchemeVersion",
                        "flow-ipc-resource-scheme-v1_17")
                && containsStringField(line, "frontendResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "frontendResourceSchemeName", "vatori-resource")
                && containsLiteralField(line, "frontendResourceSchemeHandlerInstalled", "true")
                && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                && containsLiteralField(line, "frontendResourceFileBacked", "true")
                && containsStringField(line, "stagingMode", "custom-scheme")
                && containsStringField(line, "mimeType", "text/javascript; charset=utf-8")
                && line.contains("filePathRedacted")
                && line.contains("$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources")
                && line.contains("resourceBaseUrl")
                && line.contains("vatori-resource://")
                && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")) {
            summary.frontendBuildPipeline.resourceSchemeV117StageObserved = true;
            if (line.contains("\"executionStatus\":\"executed\"")) {
                summary.frontendBuildPipeline.resourceSchemeV117ExecutedObserved = true;
            }
        }
        if (line.contains("\"type\":\"resourceLoad\"")
                && containsStringField(line, "frontendResourceSchemeVersion",
                        "flow-ipc-resource-scheme-v1_17")
                && containsStringField(line, "frontendResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsLiteralField(line, "frontendResourceSchemeRequestObserved", "true")
                && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")) {
            summary.frontendBuildPipeline.resourceSchemeV117HandlerRequestObserved = true;
            if (containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                    && containsStringField(line, "executionStatus", "served")) {
                summary.frontendBuildPipeline.resourceSchemeV117HandlerResponseObserved = true;
            }
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && containsStringField(line, "frontendResourceSchemeVersion",
                        "flow-ipc-resource-scheme-v1_17")
                && containsStringField(line, "stagingMode", "blob-fallback")
                && line.contains("blobFallbackReason")
                && line.contains("blobFallbackBlockedLayer")
                && line.contains("blobFallbackNextResolutionStep")) {
            summary.frontendBuildPipeline.resourceSchemeV117FallbackClassifiedObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendResourceSchemeVersion",
                        "flow-ipc-resource-scheme-v1_17")
                && containsStringField(line, "frontendResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "frontendResourceSchemeName", "vatori-resource")
                && containsLiteralField(line, "frontendResourceSchemeHandlerInstalled", "true")
                && containsLiteralField(line, "frontendResourceSchemeRequestObserved", "true")
                && containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                && containsLiteralField(line, "frontendResourceLoaderNoLocalhost", "true")
                && containsLiteralField(line, "frontendResourceFileBacked", "true")
                && containsLiteralField(line, "frontendResourceLoaderObserved", "true")
                && containsLiteralField(line, "frontendResourceLoaderExecuted", "true")
                && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")
                && line.contains("frontendResourceFilePathRedacted")
                && line.contains("$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources")
                && line.contains("frontendResourceRelativeImportResolutionMode")
                && line.contains("scheme-url-native-relative-base-v1_17")
                && line.contains("custom-scheme-served-by-wkurlschemehandler-no-http-v1_17")) {
            summary.frontendBuildPipeline.resourceSchemeV117DomObserved = true;
        }
    }
}
