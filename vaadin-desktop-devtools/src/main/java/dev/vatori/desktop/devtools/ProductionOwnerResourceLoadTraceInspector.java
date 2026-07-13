package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerResourceLoadTraceInspector {
    private ProductionOwnerResourceLoadTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceLoad\"")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                        "flow-ipc-production-owner-resource-loader-v1_18")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line, "productionBundleRuntimeOwnerResourcePathMode",
                        "stable-source-path")
                && containsLiteralField(line, "frontendResourceSchemeRequestObserved", "true")) {
            if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                    "root")
                    && containsPath(line,
                            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")) {
                summary.productionOwnerRuntime.resourceSchemeV118RootHandlerRequestObserved = true;
                if (containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                        && containsStringField(line, "executionStatus", "served")) {
                    summary.productionOwnerRuntime.resourceSchemeV118RootHandlerResponseObserved = true;
                }
            }
            if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                    "dependency")) {
                summary.productionOwnerRuntime.resourceSchemeV118DependencyHandlerRequestObserved = true;
                if (containsLiteralField(line, "frontendResourceSchemeResponseObserved", "true")
                        && containsStringField(line, "executionStatus", "served")) {
                    summary.productionOwnerRuntime.resourceSchemeV118DependencyHandlerResponseObserved = true;
                }
            }
        }
    }
}
