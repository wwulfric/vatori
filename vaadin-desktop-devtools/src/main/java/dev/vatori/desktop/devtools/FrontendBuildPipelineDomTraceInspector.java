package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildPipelineTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildPipelineDomTraceInspector {
    private FrontendBuildPipelineDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendResolverContractObserved", "true")
                && containsStringField(line, "frontendResolverContractVersion",
                        "flow-ipc-frontend-resolver-contract-v1_21")
                && containsStringField(line, "frontendResolverIdentityPolicy",
                        "stable-source-path-plus-owner-namespace-v1_21")
                && containsStringField(line, "frontendResolverIntegrityPolicy",
                        "sha256-required-for-bodied-resources-v1_21")
                && containsStringField(line, "frontendResolverCachePolicy",
                        "window-lifetime-content-addressed-v1_21")
                && containsStringField(line, "frontendResolverCleanupPolicy",
                        "host-owned-window-lifetime-v1_21")
                && containsStringField(line, "frontendResolverAllowedSchemes",
                        "blob,vatori-resource")
                && containsLiteralField(line, "frontendResolverNoLocalhost", "true")
                && line.contains("frontendResolverSchemesObserved")
                && line.contains("blob")
                && line.contains("vatori-resource")
                && line.contains("frontendResolverCustomSchemeFamilies")
                && line.contains("flow-lazy-dependency")
                && line.contains("production-bundle")
                && line.contains("frontendResolverBlobFamilies")
                && line.contains("frontendResolverVerifiedOnlyFamilies")
                && line.contains("flow-client-classpath")
                && line.contains("theme-static")
                && line.contains("frontendResolverStrictBlockerFamilies")
                && line.contains("generated-lazy")
                && line.contains("production-bundle")
                && line.contains("dev-mode-bundle")
                && containsLiteralField(line, "frontendResolverContractComplete", "true")) {
            summary.frontendBuildPipeline.resolverContractV121DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "generatedLazyChunkFileExecutionVersion",
                        "flow-ipc-loadondemand-chunk-file-execution-v1_22")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureObserved", "true")
                && containsStringField(line, "generatedLazyChunkFileFixtureSource",
                        "repository-vaadin-shaped-generated-flow-imports-v1_22")
                && containsStringField(line, "generatedLazyChunkFileFixtureImportsArtifact",
                        "META-INF/vatori/generated/flow/generated-flow-imports-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileFixtureChunkArtifact",
                        "META-INF/vatori/generated/flow/chunks/chunk-v1_22.js")
                && containsStringField(line, "generatedLazyChunkFileFixtureChunkId",
                        "vatori-flow-ipc-generated-lazy-chunk-v1_22")
                && containsStringField(line, "generatedLazyChunkFileFixtureTriggerOwner",
                        "repository-fixture-generated-loadondemand-executor")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureStageObserved", "true")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureExecuted", "true")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureCallbackResolved", "true")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureSideEffectSatisfied", "true")
                && containsStringFieldPrefix(line, "generatedLazyChunkFileFixtureExecutionResult",
                        "executed:")
                && containsStringField(line, "generatedLazyChunkFileFixtureSideEffectProof",
                        "window.VaadinDesktopGeneratedLazyChunkV122.executed")
                && containsStringField(line, "generatedLazyChunkFileFixtureFailureLayer", "")) {
            summary.frontendBuildPipeline.generatedChunkFileFixtureV122DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendPipelineClosureObserved", "true")
                && containsFrontendPipelineClosureV124Fields(line)) {
            summary.frontendBuildPipeline.pipelineClosureV124DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "packagedFrontendBaselineObserved", "true")
                && containsPackagedFrontendBaselineV125Fields(line)) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "packagedFrontendCustomSchemeObserved", "true")
                && line.contains("packagedFrontendHostOwnedStagedPaths")
                && line.contains("packagedFrontendHostOwnedExecutedPaths")) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125CustomSchemeDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "packagedFrontendDiagnosticBlobFallbackObserved", "true")
                && line.contains("packagedFrontendDiagnosticBlobFallbackPaths")) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyChunkFileFixtureSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "productionBundleRuntimeOwnerPositiveFixtureSideEffectSatisfied", "true")
                && containsStringField(line, "frontendPipelineClosureSatisfiedProofs",
                        "flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed")) {
            summary.frontendBuildPipeline.pipelineClosureV124FixtureProofsObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "frontendPipelineClosureRemainingBlockers",
                        "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped")
                && containsStringField(line, "frontendPipelineClosureRealBuildStatus",
                        "product-parity-pending")
                && (containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                        "production-owner-owned-missing-real-artifact")
                        || containsStringField(line, "componentProductionOwnerReuseDecision",
                                "focused-fallback-after-owner-owned-runtime-attempt"))) {
            summary.frontendBuildPipeline.pipelineClosureV124RealBlockersObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "packagedFrontendRealBlockersObserved", "true")
                && containsStringField(line, "packagedFrontendRealBuildStatus",
                        "product-parity-pending")
                && containsStringField(line, "packagedFrontendRemainingBlockers",
                        "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped")) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125RealBlockersObserved = true;
        }
    }
}
