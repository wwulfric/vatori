package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLoadOnDemandChunkTraceInspector {
    private static final String GENERATED_IMPORTS_ARTIFACT =
            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js";
    private static final String LOAD_ON_DEMAND_CHUNK_V111 =
            "flow-ipc-loadondemand-chunk-v1_11";
    private static final String LOAD_ON_DEMAND_CHUNK_FILE_V112 =
            "flow-ipc-loadondemand-chunk-file-v1_12";
    private static final String NO_OP_TABLE_SHAPE = "no-op-promise-resolve-0";
    private static final String CHUNK_FILE_NOT_GENERATED = "chunk-file-not-generated";
    private static final String NO_CHUNK_SIDE_EFFECT_PROOF =
            "no-chunk-generated-by-real-generated-flow-imports-no-op-table";
    private static final String NO_CHUNK_FAILURE_LAYER =
            "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file";

    private GeneratedLoadOnDemandChunkTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isBootstrapResponse(line)
                && line.contains("\"uidl\"")
                && line.contains("return window.Vaadin.Flow.loadOnDemand('")) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkV111UidlExpressionObserved = true;
        }
        if (isBootstrapComponentResourceResponse(line)
                && containsGeneratedLoadOnDemandChunkContractFields(line)
                && containsStringField(line, "generatedLazyChunkTriggerOwner",
                        "visible-flow-client-dependencyloader")
                && containsStringField(line, "loadOnDemandChunkRegistrySource",
                        "uidl-expression-to-dev-bundle-generated-flow-imports-registry-v1_11")
                && containsStringField(line, "loadOnDemandChunkMappingSource",
                        "flow-ipc-generated-loadondemand-chunk-map-v1_11")
                && containsStringField(line, "loadOnDemandChunkBlockerVersion",
                        LOAD_ON_DEMAND_CHUNK_V111)
                && containsGeneratedImportsArtifact(line)) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkV111ManifestObserved = true;
        }
        if (isBootstrapComponentResourceResponse(line)
                && containsGeneratedChunkFileManifestFields(line)
                && line.contains("noChunkGeneratedReason")) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkFileV112ManifestObserved = true;
        }
        if (isGeneratedLoadOnDemandChunkRequest(line)
                && line.contains("\"lookupKind\":\"Generated-flow-imports-dev-bundle-chunk-v1_4\"")
                && containsGeneratedImportsArtifact(line)
                && containsGeneratedLoadOnDemandChunkContractFields(line)
                && containsStringField(line, "generatedLazyChunkTriggerOwner",
                        "visible-flow-client-dependencyloader")
                && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")
                && containsTrueFields(line,
                        "generatedLazyChunkTriggeredByFlowClient",
                        "loadOnDemandChunkTriggeredByFlowClient")
                && line.contains("return window.Vaadin.Flow.loadOnDemand('")) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkV111RequestObserved = true;
        }
        if (isGeneratedLoadOnDemandChunkRequest(line)
                && containsGeneratedChunkFileRegistryFields(line)
                && containsTrueFields(line,
                        "loadOnDemandChunkFileTriggeredByFlowClient",
                        "loadOnDemandChunkTriggeredByFlowClient")
                && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkFileV112RequestObserved = true;
        }
        if (isGeneratedLazyResourceStage(line)
                && containsGeneratedImportsArtifact(line)
                && containsGeneratedLoadOnDemandChunkContractFields(line)
                && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")
                && containsNoChunkFailureFields(line)
                && (line.contains("\"executionStatus\":\"executed\"")
                        || line.contains("\"executionStatus\":\"strict-blocker\""))) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkV111StageObserved = true;
        }
        if (isGeneratedLazyResourceStage(line)
                && containsGeneratedImportsArtifact(line)
                && containsGeneratedChunkFileRegistryFields(line)
                && containsGeneratedChunkFileStrictBlockerFields(line)) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkFileV112StageObserved = true;
        }
        if (isWebviewDom(line)
                && containsLiteralField(line, "generatedLazyChunkUidlExpressionObserved", "true")
                && containsGeneratedLoadOnDemandChunkContractFields(line)
                && line.contains("return window.Vaadin.Flow.loadOnDemand('")
                && containsStringFieldPrefix(line, "generatedLazyChunkId", "")
                && containsStringField(line, "generatedLazyChunkTriggerOwner",
                        "visible-flow-client-dependencyloader")
                && containsLiteralField(line, "generatedLazyChunkTriggeredByFlowClient", "true")
                && containsStringFieldPrefix(line, "loadOnDemandChunkRequestedChunk", "")
                && containsLiteralField(line, "loadOnDemandChunkTriggeredByFlowClient", "true")
                && (containsLiteralField(line, "loadOnDemandChunkStageObserved", "true")
                        || summary.frontendBuildPipeline.generatedLoadOnDemandChunkV111StageObserved)
                && containsStringField(line, "loadOnDemandChunkResolvedArtifactPath",
                        GENERATED_IMPORTS_ARTIFACT)
                && containsNoChunkFailureFields(line)
                && (containsLiteralField(line, "loadOnDemandChunkExecuted", "true")
                        || containsStringFieldPrefix(line, "loadOnDemandChunkExecutionResult",
                                "strict-blocker:")
                        || containsStringField(line, "loadOnDemandChunkFailureLayer",
                                NO_CHUNK_FAILURE_LAYER))) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkV111DomObserved = true;
        }
        if (isWebviewDom(line)
                && containsGeneratedChunkFileManifestFields(line)
                && containsLiteralField(line, "loadOnDemandChunkFileTriggeredByFlowClient", "true")
                && containsGeneratedChunkFileStrictBlockerFields(line)
                && line.contains("noChunkGeneratedReason")) {
            summary.frontendBuildPipeline.generatedLoadOnDemandChunkFileV112DomObserved = true;
        }
    }

    private static boolean isGeneratedLoadOnDemandChunkRequest(String line) {
        return isResourceRequest(line)
                && line.contains("\"purpose\":\"generated-loadondemand-chunk-v1_11\"");
    }

    private static boolean isGeneratedLazyResourceStage(String line) {
        return isResourceStageLine(line)
                && line.contains("\"artifactFamily\":\"generated-lazy\"");
    }

    private static boolean containsGeneratedImportsArtifact(String line) {
        return containsPath(line, GENERATED_IMPORTS_ARTIFACT);
    }

    private static boolean containsGeneratedLoadOnDemandChunkContractFields(String line) {
        return containsStringField(line, "loadOnDemandChunkContractVersion",
                LOAD_ON_DEMAND_CHUNK_V111)
                && containsStringField(line, "generatedLazyChunkExpressionSource",
                        "bootstrap-uidl-dependencies")
                && containsGeneratedLazyChunkProbeDisabledFields(line);
    }

    private static boolean containsNoChunkFailureFields(String line) {
        return containsStringField(line, "loadOnDemandChunkBlockerVersion",
                LOAD_ON_DEMAND_CHUNK_V111)
                && containsStringField(line, "loadOnDemandChunkFailureLayer",
                        NO_CHUNK_FAILURE_LAYER);
    }

    private static boolean containsGeneratedLazyChunkProbeDisabledFields(String line) {
        return containsFalseFields(line,
                "generatedLazyChunkDiagnosticBridgeUsed",
                "generatedLazyChunkDirectImportProbeUsed");
    }

    private static boolean containsGeneratedChunkFileManifestFields(String line) {
        return containsGeneratedChunkFileRegistryFields(line)
                && containsStringField(line, "generatedLoadOnDemandChunkFileMappingSource",
                        "real-generated-flow-imports-loadondemand-table-v1_12")
                && containsStringField(line, "generatedLoadOnDemandGeneratedImportsArtifact",
                        GENERATED_IMPORTS_ARTIFACT)
                && containsLiteralField(line, "generatedLoadOnDemandNoOpLoadOnDemand", "true")
                && containsStringField(line, "chunkFileResolver",
                        "generated-flow-imports-relative-chunk-file-resolver-v1_12");
    }

    private static boolean containsGeneratedChunkFileRegistryFields(String line) {
        return containsStringField(line, "generatedLoadOnDemandChunkFileRegistryVersion",
                LOAD_ON_DEMAND_CHUNK_FILE_V112)
                && containsStringField(line, "generatedLoadOnDemandChunkFileRegistrySource",
                        "generated-flow-imports-body-parser-v1_12")
                && containsStringField(line, "chunkFileStatus", "not-generated")
                && containsStringField(line, "loadOnDemandTableShape", NO_OP_TABLE_SHAPE)
                && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                        CHUNK_FILE_NOT_GENERATED)
                && containsStringField(line, "loadOnDemandChunkFileBlockerVersion",
                        LOAD_ON_DEMAND_CHUNK_FILE_V112);
    }

    private static boolean containsGeneratedChunkFileStrictBlockerFields(String line) {
        return containsFalseFields(line,
                "loadOnDemandChunkFileStageObserved",
                "loadOnDemandChunkFileExecuted")
                && containsGeneratedChunkFileNoOpResolution(line)
                && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                        NO_CHUNK_SIDE_EFFECT_PROOF)
                && containsGeneratedChunkFileStrictBlockerResult(line);
    }
}
