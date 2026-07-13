package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyResourceStageTraceInspector {
    private GeneratedLazyResourceStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")
                && containsStringField(line, "generatedLazyResolverVersion",
                        "flow-ipc-generated-lazy-resolver-v1_8")
                && containsStringField(line, "generatedLazyImportResolutionStrategy",
                        "stable-relative-import-rewrite-generated-lazy-v1_8")
                && containsLiteralField(line, "generatedLazyRelativeDependencyResolved", "true")
                && containsStringField(line, "generatedLazyRelativeImportSpecifier",
                        "./markdown-helpers-31I0A-hg.js")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js")) {
            summary.frontendBuildPipeline.generatedLazyV18DependencyRewriteObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsStringField(line, "generatedLazyExecutionVersion", "flow-ipc-generated-lazy-v1_8")
                && containsStringField(line, "generatedLazyImportResolutionStrategy",
                        "stable-relative-import-rewrite-generated-lazy-v1_8")
                && (line.contains("\"executionStatus\":\"executed\"")
                        || (line.contains("\"executionStatus\":\"strict-blocker\"")
                                && ((containsStringField(line, "generatedLazyBlockerVersion",
                                        "flow-ipc-generated-lazy-v1_8")
                                        && containsStringField(line, "failureLayer",
                                                "relative-dependency-resolved-flow-loadondemand-boundary"))
                                        || (containsStringField(line, "generatedLazyBlockerVersion",
                                                "flow-ipc-loadondemand-v1_9")
                                                && containsStringField(line, "failureLayer",
                                                        "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")))))) {
            summary.frontendBuildPipeline.generatedLazyV18ExecutionOrNarrowedBlockerObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
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
                && containsStringField(line, "generatedLazyBlockerVersion",
                        "flow-ipc-loadondemand-v1_9")
                && containsStringField(line, "failureLayer",
                        "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered")
                && containsLiteralField(line, "generatedLazyFlowClientTriggered", "false")
                && containsLiteralField(line, "generatedLazyDiagnosticBridge", "true")
                && (line.contains("\"executionStatus\":\"execution-attempt\"")
                        || line.contains("\"executionStatus\":\"executed\"")
                        || line.contains("\"executionStatus\":\"strict-blocker\""))) {
            summary.frontendBuildPipeline.generatedLazyV19StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"artifactFamily\":\"flow-lazy-dependency\"")
                && containsPath(line, "vatori-flow-ipc-lazy-v1_10.js")
                && containsStringField(line, "runtimeResolverContractVersion",
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                && containsStringField(line, "lazyDependencyVersion",
                        "flow-ipc-lazy-dependency-v1_10")
                && containsStringField(line, "lazyDependencySource",
                        "flow-uidl-dependencyloader-loadmode-lazy-v1_10")
                && containsStringField(line, "lazyDependencyMode", "LoadMode.LAZY")
                && containsLiteralField(line, "lazyDependencyTriggeredByFlowClient", "true")
                && line.contains("\"executionStatus\":\"executed\"")) {
            summary.frontendBuildPipeline.flowLazyDependencyV110StageObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && (line.contains("\"executionStatus\":\"execution-attempt\"")
                        || line.contains("\"executionStatus\":\"executed\"")
                        || line.contains("\"executionStatus\":\"strict-blocker\""))) {
            summary.frontendBuildPipeline.generatedLazyExecutionAttemptObserved = true;
        }
        if (line.contains("\"type\":\"resourceStage\"")
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && (line.contains("\"executionStatus\":\"executed\"")
                        || (line.contains("\"executionStatus\":\"strict-blocker\"")
                                && line.contains("\"browserError\"")))) {
            summary.frontendBuildPipeline.generatedLazyExecutionResolvedObserved = true;
        }
    }
}
