package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyResourceDomTraceInspector {
    private GeneratedLazyResourceDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyExecutionAttempted", "true")
                && line.contains("frontendArtifactGeneratedLazyExecutionResult")
                && line.contains("frontendArtifactGeneratedLazyLoadOnDemandResult")) {
            summary.frontendBuildPipeline.generatedLazyExecutionAttemptObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyExecutionAttempted", "true")
                && (containsLiteralField(line, "frontendArtifactGeneratedLazyExecuted", "true")
                        || line.contains("strict-blocker:vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js"))) {
            summary.frontendBuildPipeline.generatedLazyExecutionResolvedObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("frontendArtifactGeneratedLazyLoadOnDemandResult")
                && (line.contains("staged-import-attempted")
                        || line.contains("api-installed-by-staged-generated-flow-imports")
                        || line.contains("api-present-before-generated-flow-imports"))) {
            summary.frontendBuildPipeline.loadOnDemandBoundaryObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendArtifactGeneratedLazyResolverVersion",
                        "flow-ipc-generated-lazy-resolver-v1_8")
                && containsStringField(line, "frontendArtifactGeneratedLazyExecutionVersion",
                        "flow-ipc-generated-lazy-v1_8")
                && containsStringField(line, "frontendArtifactGeneratedLazyImportResolutionStrategy",
                        "stable-relative-import-rewrite-generated-lazy-v1_8")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyRelativeDependencyResolved", "true")
                && containsStringField(line, "frontendArtifactGeneratedLazyRelativeImportSpecifier",
                        "./markdown-helpers-31I0A-hg.js")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")
                && (containsLiteralField(line, "frontendArtifactGeneratedLazyExecuted", "true")
                        || containsStringField(line, "frontendArtifactGeneratedLazyBlockerVersion",
                                "flow-ipc-generated-lazy-v1_8")
                        || containsStringField(line, "frontendArtifactGeneratedLazyBlockerVersion",
                                "flow-ipc-loadondemand-v1_9"))) {
            summary.frontendBuildPipeline.generatedLazyV18DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendArtifactGeneratedLazyLoadOnDemandContractVersion",
                        "flow-ipc-loadondemand-v1_9")
                && containsStringField(line, "frontendArtifactGeneratedLazyTriggerSource",
                        "diagnostic-preflight-and-direct-staged-import")
                && containsStringField(line, "frontendArtifactGeneratedLazyApiOwner",
                        "window.Vaadin.Flow.loadOnDemand")
                && containsStringField(line, "frontendArtifactGeneratedLazyApiInstallSource",
                        "generated-flow-imports-dev-bundle-after-direct-staged-import")
                && containsStringField(line, "frontendArtifactGeneratedLazyRequestedChunk",
                        "vatori-flow-ipc-generated-lazy-v1_9-probe")
                && containsStringField(line, "frontendArtifactGeneratedLazyRegistrySource",
                        "no-bootstrap-lazy-registry-observed")
                && containsStringField(line, "frontendArtifactGeneratedLazyMappingSource",
                        "component-frontend-manifest-generated-lazy-v1_9")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "frontendArtifactGeneratedLazyBlockerVersion",
                        "flow-ipc-loadondemand-v1_9")
                && containsStringField(line, "frontendArtifactGeneratedLazyFailureLayer",
                        "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyFlowClientTriggered", "false")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyDiagnosticBridge", "true")
                && containsLiteralField(line, "frontendArtifactGeneratedLazyLoadOnDemandBlocker", "true")
                && line.contains("frontendArtifactGeneratedLazyLoadOnDemandResult")
                && (line.contains("diagnostic-loadondemand-call")
                        || line.contains("diagnostic-direct-import-required"))) {
            summary.frontendBuildPipeline.generatedLazyV19DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendRuntimeResolverContractVersion",
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                && line.contains("frontendRuntimeResolverLogicalRequestIds")
                && line.contains("flow-lazy-dependency:v1_10")
                && line.contains("production-runtime:indexhtml:v1_10")) {
            summary.frontendBuildPipeline.runtimeResolverV110ManifestObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowLazyDependencyObserved", "true")
                && containsLiteralField(line, "flowLazyDependencyRequested", "true")
                && containsLiteralField(line, "flowLazyDependencyResponse", "true")
                && containsLiteralField(line, "flowLazyDependencyStageObserved", "true")
                && containsLiteralField(line, "flowLazyDependencyExecuted", "true")
                && containsStringField(line, "flowLazyDependencyVersion",
                        "flow-ipc-lazy-dependency-v1_10")
                && containsStringField(line, "flowLazyDependencySource",
                        "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                && containsStringField(line, "flowLazyDependencyMode", "LoadMode.LAZY")
                && containsStringField(line, "flowLazyDependencyExpression",
                        "context://vatori-flow-ipc-lazy-v1_10.js")
                && containsStringField(line, "flowLazyDependencyResolvedPath",
                        "vatori-flow-ipc-lazy-v1_10.js")
                && containsLiteralField(line, "flowLazyDependencyTriggeredByFlowClient", "true")
                && line.contains("executed:vatori-flow-ipc-lazy-v1_10.js")) {
            summary.frontendBuildPipeline.flowLazyDependencyV110DomObserved = true;
        }
    }
}
