package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyResourceRequestTraceInspector {
    private GeneratedLazyResourceRequestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazyResolverVersion",
                        "flow-ipc-generated-lazy-resolver-v1_8")
                && containsStringField(line, "generatedLazyExecutionVersion", "flow-ipc-generated-lazy-v1_8")
                && containsStringField(line, "generatedLazyImportResolutionStrategy",
                        "stable-relative-import-rewrite-generated-lazy-v1_8")) {
            summary.frontendBuildPipeline.generatedLazyV18RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazyLoadOnDemandContractVersion",
                        "flow-ipc-loadondemand-v1_9")
                && containsStringField(line, "generatedLazyTriggerSource",
                        "diagnostic-preflight-and-direct-staged-import")
                && containsStringField(line, "generatedLazyApiOwner", "window.Vaadin.Flow.loadOnDemand")
                && containsStringField(line, "generatedLazyRequestedChunk",
                        "vatori-flow-ipc-generated-lazy-v1_9-probe")
                && containsStringField(line, "generatedLazyMappingSource",
                        "component-frontend-manifest-generated-lazy-v1_9")
                && containsLiteralField(line, "generatedLazyFlowClientTriggered", "false")
                && containsLiteralField(line, "generatedLazyDiagnosticBridge", "true")) {
            summary.frontendBuildPipeline.generatedLazyV19RequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"flow-lazy-dependency-loader\"")
                && line.contains("\"lookupKind\":\"Flow-lazy-dependency-resource-v1_10\"")
                && containsStringField(line, "runtimeResolverContractVersion",
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                && containsStringField(line, "lazyDependencyVersion",
                        "flow-ipc-lazy-dependency-v1_10")
                && containsStringField(line, "lazyDependencySource",
                        "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                && containsStringField(line, "lazyDependencyMode", "LoadMode.LAZY")
                && containsStringField(line, "lazyDependencyExpression",
                        "context://vatori-flow-ipc-lazy-v1_10.js")
                && containsStringField(line, "lazyDependencyResolvedPath",
                        "vatori-flow-ipc-lazy-v1_10.js")
                && containsLiteralField(line, "lazyDependencyTriggeredByFlowClient", "true")
                && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")) {
            summary.frontendBuildPipeline.flowLazyDependencyV110RequestObserved = true;
        }
    }
}
