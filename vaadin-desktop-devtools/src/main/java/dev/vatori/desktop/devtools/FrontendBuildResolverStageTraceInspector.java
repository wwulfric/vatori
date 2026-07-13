package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildPipelineTraceFields.*;
import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildResolverStageTraceInspector {
    private FrontendBuildResolverStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isResourceStage(line)
                && containsStringField(line, "frontendResolverContractVersion",
                        "flow-ipc-frontend-resolver-contract-v1_21")
                && containsStringField(line, "frontendResolverIdentityPolicy",
                        "stable-source-path-plus-owner-namespace-v1_21")
                && containsStringField(line, "frontendResolverIntegrityPolicy",
                        "sha256-required-for-bodied-resources-v1_21")
                && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                && line.contains("\"sha256\":\"")
                && line.contains("\"sourcePath\":\"")) {
            summary.frontendBuildPipeline.resolverContractV121StageObserved = true;
            if (containsStringField(line, "stagedUrlScheme", "blob")
                    || line.contains("\"stagedUrl\":\"blob:")) {
                summary.frontendBuildPipeline.resolverContractV121BlobStageObserved = true;
            }
            if (containsStringField(line, "stagedUrlScheme", "vatori-resource")
                    || line.contains("vatori-resource://")) {
                summary.frontendBuildPipeline.resolverContractV121CustomSchemeStageObserved = true;
            }
            if (line.contains("\"artifactFamily\":\"generated-lazy\"")
                    || line.contains("\"artifactFamily\":\"production-bundle\"")
                    || line.contains("\"artifactFamily\":\"dev-mode-bundle\"")) {
                summary.frontendBuildPipeline.resolverContractV121StrictOrGapStageObserved = true;
            }
        }
        if (isResourceStage(line)
                && line.contains("\"artifactFamily\":\"generated-lazy\"")
                && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                        "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureObserved", "true")
                && containsStringField(line, "generatedLazyChunkFileFixtureSource",
                        "repository-vaadin-shaped-generated-flow-imports-v1_22")
                && containsPath(line, "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileFixtureChunkArtifact",
                        "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileFixtureChunkId",
                        "vatori-flow-ipc-generated-lazy-chunk-v1_22")
                && containsStringField(line, "loadOnDemandTableShape", "branch-table-dynamic-import")
                && containsStringField(line, "chunkFileStatus", "available")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureStageObserved", "true")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureExecuted", "true")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureCallbackResolved", "true")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureSideEffectSatisfied", "true")
                && containsStringField(line, "generatedLazyChunkFileFixtureSideEffectProof",
                        "window.VaadinDesktopGeneratedLazyChunkV122.executed")
                && hasExecuted(line)) {
            summary.frontendBuildPipeline.generatedChunkFileFixtureV122StageObserved = true;
        }
        if (isResourceStage(line)
                && containsFrontendPipelineClosureV124Fields(line)
                && hasExecutedOrStrictBlocker(line)) {
            summary.frontendBuildPipeline.pipelineClosureV124StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPackagedFrontendBaselineV125Fields(line)
                && hasExecutedStrictBlockerOrAttempt(line)) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPackagedFrontendBaselineV125Fields(line)
                && containsStringField(line, "stagedUrlScheme", "vatori-resource")
                && containsStringField(line, "stagingMode", "custom-scheme")) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125CustomSchemeStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPackagedFrontendBaselineV125Fields(line)
                && containsStringField(line, "stagedUrlScheme", "blob")
                && containsStringField(line, "packagedFrontendAllowedFallback",
                        "diagnostic-blob-only")) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125DiagnosticBlobFallbackObserved = true;
        }
    }
}
