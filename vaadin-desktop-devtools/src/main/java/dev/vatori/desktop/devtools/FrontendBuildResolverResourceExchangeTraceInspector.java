package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildPipelineTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildResolverResourceExchangeTraceInspector {
    private FrontendBuildResolverResourceExchangeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        inspectResolverContract(summary, line);
        inspectGeneratedChunkFixture(summary, line);
        inspectPipelineBaseline(summary, line);
    }

    private static void inspectResolverContract(TraceSummary summary, String line) {
        if (isResourceRequest(line)
                && containsStringField(line, "frontendResolverContractVersion",
                        "flow-ipc-frontend-resolver-contract-v1_21")
                && containsStringField(line, "frontendResolverIdentityPolicy",
                        "stable-source-path-plus-owner-namespace-v1_21")
                && containsStringField(line, "frontendResolverIntegrityPolicy",
                        "sha256-required-for-bodied-resources-v1_21")
                && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                && containsStringField(line, "frontendResolverAllowedSchemes",
                        "blob,vatori-resource")
                && line.contains("\"expectedSha256\":\"")) {
            summary.frontendBuildPipeline.resolverContractV121RequestObserved = true;
        }
        if (isRedactedResourceResponse(line)
                && containsStringField(line, "frontendResolverContractVersion",
                        "flow-ipc-frontend-resolver-contract-v1_21")
                && containsStringField(line, "frontendResolverIntegrityPolicy",
                        "sha256-required-for-bodied-resources-v1_21")
                && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                && line.contains("\"sha256\":\"")) {
            summary.frontendBuildPipeline.resolverContractV121ResponseObserved = true;
        }
    }

    private static void inspectGeneratedChunkFixture(TraceSummary summary, String line) {
        if (isResourceRequest(line)
                && containsPath(line, "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                        "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                && containsLiteralField(line, "generatedLazyChunkFileFixture", "true")
                && containsStringField(line, "generatedLazyChunkFileFixtureRole", "imports")
                && containsStringField(line, "generatedLazyChunkFileFixtureSource",
                        "repository-vaadin-shaped-generated-flow-imports-v1_22")
                && containsStringField(line, "generatedLazyChunkFileFixtureChunkArtifact",
                        "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                && containsStringField(line, "loadOnDemandTableShape", "branch-table-dynamic-import")
                && containsStringField(line, "chunkFileStatus", "available")) {
            summary.frontendBuildPipeline.generatedChunkFileFixtureV122RequestObserved = true;
        }
        if (isRedactedResourceResponse(line)
                && containsPath(line, "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                        "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                && containsLiteralField(line, "generatedLazyChunkFileFixture", "true")
                && containsStringField(line, "loadOnDemandTableShape", "branch-table-dynamic-import")
                && containsStringField(line, "chunkImportSpecifier", "./chunks/chunk-v1_22.js")
                && containsStringField(line, "chunkFileStatus", "available")
                && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                        "chunk-file-body-available-for-staged-import")
                && line.contains("\"sha256\":\"")) {
            summary.frontendBuildPipeline.generatedChunkFileFixtureV122ImportsResponseObserved = true;
        }
        if (isRedactedResourceResponse(line)
                && containsPath(line, "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                        "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                && containsLiteralField(line, "generatedLazyChunkFileFixture", "true")
                && containsStringField(line, "generatedLazyChunkFileFixtureRole", "chunk")
                && containsStringField(line, "chunkFileStatus", "available")
                && containsStringField(line, "loadOnDemandChunkFileStatus", "available")
                && containsStringField(line, "loadOnDemandChunkFileSideEffectProof",
                        "chunk-file-body-available-for-staged-import")
                && line.contains("\"sha256\":\"")) {
            summary.frontendBuildPipeline.generatedChunkFileFixtureV122ChunkResponseObserved = true;
        }
    }

    private static void inspectPipelineBaseline(TraceSummary summary, String line) {
        if (isResourceRequest(line)
                && containsFrontendPipelineClosureV124Fields(line)) {
            summary.frontendBuildPipeline.pipelineClosureV124RequestObserved = true;
        }
        if (isResourceResponse(line)
                && containsFrontendPipelineClosureV124Fields(line)) {
            summary.frontendBuildPipeline.pipelineClosureV124ResponseObserved = true;
        }
        if (isResourceRequest(line)
                && containsPackagedFrontendBaselineV125Fields(line)) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125RequestObserved = true;
        }
        if (isResourceResponse(line)
                && containsPackagedFrontendBaselineV125Fields(line)) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125ResponseObserved = true;
        }
    }
}
