package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildPipelineTraceFields {
    boolean assetPipelineV14Observed;
    boolean artifactFamilyModelObserved;
    boolean dependencyGraphObserved;
    boolean shimBlockerObserved;
    boolean manifestImportRewriteObserved;
    boolean textFieldDevBundleBlockerObserved;
    boolean productionBundlePartialObserved;
    boolean productionBundleStatsResourceObserved;
    boolean themeStaticResourceObserved;
    boolean themeStaticStrictBlockerObserved;
    boolean generatedLazyResourceObserved;
    boolean generatedLazyBundleChunkResourceObserved;
    boolean devModeResourceObserved;
    boolean devModeStrictBlockerObserved;
    boolean generatedLazyStrictBlockerObserved;
    boolean resourceStageObserved;
    boolean resourceStageExecutedObserved;
    boolean resourceStageStrictBlockerObserved;
    boolean resourceStageFailureClassificationObserved;
    boolean stagingDomObserved;
    boolean textFieldStagedExecutionObserved;
    boolean textFieldStagedBlockerObserved;
    boolean generatedLazyExecutionAttemptObserved;
    boolean generatedLazyExecutionResolvedObserved;
    boolean loadOnDemandBoundaryObserved;
    boolean textFieldV16ManifestObserved;
    boolean textFieldV16RequestObserved;
    boolean textFieldV16StageBlockerObserved;
    boolean textFieldV16DependencyClassificationObserved;
    boolean textFieldV16DomObserved;
    boolean textFieldV17ManifestObserved;
    boolean textFieldV17RequestObserved;
    boolean textFieldV17StageExecutedObserved;
    boolean textFieldV17ModuleIdentityDependencyObserved;
    boolean textFieldV17DomObserved;
    boolean generatedLazyV18ManifestObserved;
    boolean generatedLazyV18RequestObserved;
    boolean generatedLazyV18DependencyRewriteObserved;
    boolean generatedLazyV18ExecutionOrNarrowedBlockerObserved;
    boolean generatedLazyV18DomObserved;
    boolean generatedLazyV19ManifestObserved;
    boolean generatedLazyV19RequestObserved;
    boolean generatedLazyV19StageObserved;
    boolean generatedLazyV19DomObserved;
    boolean runtimeResolverV110ManifestObserved;
    boolean flowLazyDependencyV110ManifestObserved;
    boolean flowLazyDependencyV110RequestObserved;
    boolean flowLazyDependencyV110StageObserved;
    boolean flowLazyDependencyV110DomObserved;
    boolean productionRuntimeV110ManifestObserved;
    boolean productionRuntimeV110RequestObserved;
    boolean productionRuntimeV110StageObserved;
    boolean productionRuntimeV110DomObserved;
    boolean generatedLoadOnDemandChunkV111ManifestObserved;
    boolean generatedLoadOnDemandChunkV111UidlExpressionObserved;
    boolean generatedLoadOnDemandChunkV111RequestObserved;
    boolean generatedLoadOnDemandChunkV111StageObserved;
    boolean generatedLoadOnDemandChunkV111DomObserved;
    boolean generatedLoadOnDemandChunkFileV112ManifestObserved;
    boolean generatedLoadOnDemandChunkFileV112RequestObserved;
    boolean generatedLoadOnDemandChunkFileV112StageObserved;
    boolean generatedLoadOnDemandChunkFileV112DomObserved;
    boolean resourceSchemeV117ManifestObserved;
    boolean resourceSchemeV117HostInstallObserved;
    boolean resourceSchemeV117StageObserved;
    boolean resourceSchemeV117HandlerRequestObserved;
    boolean resourceSchemeV117HandlerResponseObserved;
    boolean resourceSchemeV117ExecutedObserved;
    boolean resourceSchemeV117DomObserved;
    boolean resourceSchemeV117FallbackClassifiedObserved;
    boolean resolverContractV121ManifestObserved;
    boolean resolverContractV121RequestObserved;
    boolean resolverContractV121ResponseObserved;
    boolean resolverContractV121StageObserved;
    boolean resolverContractV121BlobStageObserved;
    boolean resolverContractV121CustomSchemeStageObserved;
    boolean resolverContractV121StrictOrGapStageObserved;
    boolean resolverContractV121DomObserved;
    boolean generatedChunkFileFixtureV122RequestObserved;
    boolean generatedChunkFileFixtureV122ImportsResponseObserved;
    boolean generatedChunkFileFixtureV122ChunkResponseObserved;
    boolean generatedChunkFileFixtureV122StageObserved;
    boolean generatedChunkFileFixtureV122DomObserved;
    boolean pipelineClosureV124ManifestObserved;
    boolean pipelineClosureV124RequestObserved;
    boolean pipelineClosureV124ResponseObserved;
    boolean pipelineClosureV124StageObserved;
    boolean pipelineClosureV124DomObserved;
    boolean pipelineClosureV124FixtureProofsObserved;
    boolean pipelineClosureV124RealBlockersObserved;
    boolean packagedFrontendBaselineV125ManifestObserved;
    boolean packagedFrontendBaselineV125RequestObserved;
    boolean packagedFrontendBaselineV125ResponseObserved;
    boolean packagedFrontendBaselineV125StageObserved;
    boolean packagedFrontendBaselineV125CustomSchemeStageObserved;
    boolean packagedFrontendBaselineV125DiagnosticBlobFallbackObserved;
    boolean packagedFrontendBaselineV125DomObserved;
    boolean packagedFrontendBaselineV125CustomSchemeDomObserved;
    boolean packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved;
    boolean packagedFrontendBaselineV125RealBlockersObserved;

    boolean hasV1_4Signals() {
        return assetPipelineV14Observed
                && artifactFamilyModelObserved
                && dependencyGraphObserved
                && shimBlockerObserved
                && manifestImportRewriteObserved
                && textFieldDevBundleBlockerObserved
                && productionBundlePartialObserved
                && productionBundleStatsResourceObserved
                && themeStaticResourceObserved
                && devModeResourceObserved
                && devModeStrictBlockerObserved
                && generatedLazyStrictBlockerObserved
                && generatedLazyResourceObserved
                && generatedLazyBundleChunkResourceObserved;
    }

    boolean hasV1_5Signals(boolean currentAppBuildFrontendOutputSuccessorEvidence) {
        return resourceStageObserved
                && stagingDomObserved
                && (resourceStageExecutedObserved || resourceStageStrictBlockerObserved)
                && resourceStageFailureClassificationObserved
                && (textFieldStagedExecutionObserved || textFieldStagedBlockerObserved)
                && generatedLazyExecutionAttemptObserved
                && (generatedLazyExecutionResolvedObserved
                        || currentAppBuildFrontendOutputSuccessorEvidence)
                && loadOnDemandBoundaryObserved;
    }

    boolean hasTextFieldV1_6Signals() {
        return textFieldV16ManifestObserved
                && textFieldV16RequestObserved
                && textFieldV16StageBlockerObserved
                && textFieldV16DependencyClassificationObserved
                && textFieldV16DomObserved;
    }

    boolean hasTextFieldV1_7Signals() {
        return textFieldV17ManifestObserved
                && textFieldV17RequestObserved
                && textFieldV17StageExecutedObserved
                && textFieldV17ModuleIdentityDependencyObserved
                && textFieldV17DomObserved;
    }

    boolean hasGeneratedLazyV1_8Signals(boolean currentAppBuildFrontendOutputSuccessorEvidence) {
        return (generatedLazyV18ManifestObserved
                && (generatedLazyV18RequestObserved
                        || generatedLoadOnDemandChunkV111RequestObserved)
                && generatedLazyV18DependencyRewriteObserved
                && generatedLazyV18ExecutionOrNarrowedBlockerObserved
                && generatedLazyV18DomObserved)
                || currentAppBuildFrontendOutputSuccessorEvidence;
    }

    boolean hasLoadOnDemandV1_9Signals(boolean currentAppBuildFrontendOutputSuccessorEvidence) {
        return (generatedLazyV19ManifestObserved
                && (generatedLazyV19RequestObserved
                        || generatedLoadOnDemandChunkV111RequestObserved)
                && generatedLazyV19StageObserved
                && generatedLazyV19DomObserved)
                || currentAppBuildFrontendOutputSuccessorEvidence;
    }

    boolean hasLazyDependencyV1_10Signals() {
        return runtimeResolverV110ManifestObserved
                && flowLazyDependencyV110ManifestObserved
                && flowLazyDependencyV110RequestObserved
                && flowLazyDependencyV110StageObserved
                && flowLazyDependencyV110DomObserved;
    }

    boolean hasProductionRuntimeV1_10Signals() {
        return runtimeResolverV110ManifestObserved
                && productionRuntimeV110ManifestObserved
                && productionRuntimeV110RequestObserved
                && productionRuntimeV110StageObserved
                && productionRuntimeV110DomObserved;
    }

    boolean hasLoadOnDemandChunkV1_11Signals(
            boolean currentAppBuildFrontendOutputSuccessorEvidence) {
        return (generatedLoadOnDemandChunkV111ManifestObserved
                && generatedLoadOnDemandChunkV111UidlExpressionObserved
                && generatedLoadOnDemandChunkV111RequestObserved
                && generatedLoadOnDemandChunkV111StageObserved
                && generatedLoadOnDemandChunkV111DomObserved)
                || currentAppBuildFrontendOutputSuccessorEvidence;
    }

    boolean hasLoadOnDemandChunkFileV1_12Signals(
            boolean currentAppBuildFrontendOutputSuccessorEvidence) {
        return (generatedLoadOnDemandChunkFileV112ManifestObserved
                && generatedLoadOnDemandChunkFileV112RequestObserved
                && generatedLoadOnDemandChunkFileV112StageObserved
                && generatedLoadOnDemandChunkFileV112DomObserved)
                || currentAppBuildFrontendOutputSuccessorEvidence;
    }

    boolean hasResourceSchemeV1_17Signals() {
        return resourceSchemeV117ManifestObserved
                && resourceSchemeV117HostInstallObserved
                && resourceSchemeV117StageObserved
                && resourceSchemeV117HandlerRequestObserved
                && resourceSchemeV117HandlerResponseObserved
                && resourceSchemeV117ExecutedObserved
                && resourceSchemeV117DomObserved;
    }

    boolean hasResolverContractV1_21Signals() {
        return resolverContractV121ManifestObserved
                && resolverContractV121RequestObserved
                && resolverContractV121ResponseObserved
                && resolverContractV121StageObserved
                && resolverContractV121BlobStageObserved
                && resolverContractV121CustomSchemeStageObserved
                && resolverContractV121StrictOrGapStageObserved
                && resolverContractV121DomObserved;
    }

    boolean hasGeneratedChunkFileFixtureV1_22Signals() {
        return generatedChunkFileFixtureV122RequestObserved
                && generatedChunkFileFixtureV122ImportsResponseObserved
                && generatedChunkFileFixtureV122ChunkResponseObserved
                && generatedChunkFileFixtureV122StageObserved
                && generatedChunkFileFixtureV122DomObserved;
    }

    boolean hasPipelineClosureV1_24Signals() {
        return pipelineClosureV124ManifestObserved
                && pipelineClosureV124RequestObserved
                && pipelineClosureV124ResponseObserved
                && pipelineClosureV124StageObserved
                && pipelineClosureV124DomObserved
                && pipelineClosureV124FixtureProofsObserved
                && pipelineClosureV124RealBlockersObserved;
    }

    boolean hasPackagedFrontendBaselineV1_25Signals() {
        return packagedFrontendBaselineV125ManifestObserved
                && packagedFrontendBaselineV125RequestObserved
                && packagedFrontendBaselineV125ResponseObserved
                && packagedFrontendBaselineV125StageObserved
                && packagedFrontendBaselineV125CustomSchemeStageObserved
                && packagedFrontendBaselineV125DiagnosticBlobFallbackObserved
                && packagedFrontendBaselineV125DomObserved
                && packagedFrontendBaselineV125CustomSchemeDomObserved
                && packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved
                && packagedFrontendBaselineV125RealBlockersObserved;
    }

    static boolean containsFrontendPipelineClosureV124Fields(String line) {
        return containsStringField(line, "frontendPipelineClosureVersion",
                "flow-ipc-frontend-pipeline-closure-v1_24")
                && containsStringField(line, "frontendPipelineClosureScope",
                        "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode")
                && containsStringField(line, "frontendPipelineClosureTransport",
                        "no-localhost-ipc-resource-request")
                && containsStringField(line, "frontendPipelineClosureInfrastructureStatus",
                        "closed-for-representative-fixture-paths")
                && containsStringField(line, "frontendPipelineClosureRealBuildStatus",
                        "product-parity-pending")
                && containsStringField(line, "frontendPipelineClosureSatisfiedProofs",
                        "flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed")
                && containsStringField(line, "frontendPipelineClosureRemainingBlockers",
                        "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped")
                && containsStringField(line, "frontendPipelineClosureDecision",
                        "basic-pipeline-infrastructure-closed-real-build-parity-pending")
                && containsStringField(line, "frontendPipelineClosureNextProductPath",
                        "packaging-time-staged-frontend-or-supported-vaadin-build-spi")
                && containsLiteralField(line, "frontendPipelineClosureNoLocalhost", "true");
    }

    static boolean containsPackagedFrontendBaselineV125Fields(String line) {
        return containsStringField(line, "packagedFrontendBaselineVersion",
                "flow-ipc-packaged-frontend-baseline-v1_25")
                && containsStringField(line, "packagedFrontendProductPath",
                        "packaging-time-staged-frontend-baseline")
                && containsStringField(line, "packagedFrontendInventoryScope",
                        "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode")
                && containsStringField(line, "packagedFrontendPreferredLoader",
                        "custom-scheme-file-backed")
                && containsStringField(line, "packagedFrontendAllowedFallback",
                        "diagnostic-blob-only")
                && containsStringField(line, "packagedFrontendIntegrity",
                        "sha256-and-byte-count")
                && containsStringField(line, "packagedFrontendCleanupPolicy",
                        "window-lifetime-cache-cleanup")
                && containsStringField(line, "packagedFrontendRealBuildStatus",
                        "product-parity-pending")
                && containsStringField(line, "packagedFrontendRemainingBlockers",
                        "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped");
    }
}
