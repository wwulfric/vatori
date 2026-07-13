package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrontendBuildEarlyTraceSummaryPrinter {
    private FrontendBuildEarlyTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrontendBuildPipelineTraceFields pipeline = summary.frontendBuildPipeline;
        ProductionOwnerRuntimeTraceFields productionOwner =
                summary.productionOwnerRuntime;
        printLine("Flow IPC v1.4 frontend asset pipeline observed", pipeline.assetPipelineV14Observed);
        printLine("Flow IPC v1.4 frontend artifact families observed",
                pipeline.artifactFamilyModelObserved);
        printLine("Flow IPC v1.4 frontend dependency graph observed", pipeline.dependencyGraphObserved);
        printLine("Flow IPC v1.4 frontend shim blocker observed", pipeline.shimBlockerObserved);
        printLine("Flow IPC v1.4 manifest import rewrite observed", pipeline.manifestImportRewriteObserved);
        printLine("Flow IPC v1.4 TextField dev-bundle blocker observed",
                pipeline.textFieldDevBundleBlockerObserved);
        printLine("Flow IPC v1.4 production bundle partial graph observed",
                pipeline.productionBundlePartialObserved);
        printLine("Flow IPC v1.4 production bundle stats resource observed",
                pipeline.productionBundleStatsResourceObserved);
        printLine("Flow IPC v1.4 theme/static strict blocker observed",
                pipeline.themeStaticStrictBlockerObserved);
        printLine("Flow IPC v1.4 theme/static resource observed", pipeline.themeStaticResourceObserved);
        printLine("Flow IPC v1.4 dev-mode strict blocker observed", pipeline.devModeStrictBlockerObserved);
        printLine("Flow IPC v1.4 dev-mode resource observed", pipeline.devModeResourceObserved);
        printLine("Flow IPC v1.4 generated/lazy strict blocker observed",
                pipeline.generatedLazyStrictBlockerObserved);
        printLine("Flow IPC v1.4 generated/lazy resource observed", pipeline.generatedLazyResourceObserved);
        printLine("Flow IPC v1.4 generated/lazy dev-bundle chunk observed",
                pipeline.generatedLazyBundleChunkResourceObserved);
        printLine("Flow IPC v1.5 resource staging observed", pipeline.resourceStageObserved);
        printLine("Flow IPC v1.5 staging DOM observed", pipeline.stagingDomObserved);
        printLine("Flow IPC v1.5 staged execution/strict blocker observed",
                pipeline.resourceStageExecutedObserved,
                pipeline.resourceStageStrictBlockerObserved);
        printLine("Flow IPC v1.5 staged failure classification observed",
                pipeline.resourceStageFailureClassificationObserved);
        printLine("Flow IPC v1.5 TextField staged execution/blocker observed",
                pipeline.textFieldStagedExecutionObserved,
                pipeline.textFieldStagedBlockerObserved);
        printLine("Flow IPC v1.5 generated/lazy execution attempt/resolution observed",
                pipeline.generatedLazyExecutionAttemptObserved,
                pipeline.generatedLazyExecutionResolvedObserved);
        printLine("Flow IPC v1.5 loadOnDemand boundary observed", pipeline.loadOnDemandBoundaryObserved);
        printLine("Flow IPC v1.8 generated/lazy manifest observed", pipeline.generatedLazyV18ManifestObserved);
        printLine("Flow IPC v1.8 generated/lazy request observed", pipeline.generatedLazyV18RequestObserved);
        printLine("Flow IPC v1.8 generated/lazy dependency rewrite observed",
                pipeline.generatedLazyV18DependencyRewriteObserved);
        printLine("Flow IPC v1.8 generated/lazy execution/narrowed blocker observed",
                pipeline.generatedLazyV18ExecutionOrNarrowedBlockerObserved);
        printLine("Flow IPC v1.8 generated/lazy DOM observed", pipeline.generatedLazyV18DomObserved);
        printLine("Flow IPC v1.9 loadOnDemand manifest observed", pipeline.generatedLazyV19ManifestObserved);
        printLine("Flow IPC v1.9 loadOnDemand request observed", pipeline.generatedLazyV19RequestObserved);
        printLine("Flow IPC v1.9 loadOnDemand stage observed", pipeline.generatedLazyV19StageObserved);
        printLine("Flow IPC v1.9 loadOnDemand DOM observed", pipeline.generatedLazyV19DomObserved);
        printLine("Flow IPC v1.10 runtime resolver manifest observed", pipeline.runtimeResolverV110ManifestObserved);
        printLine("Flow IPC v1.10 lazy dependency manifest observed",
                pipeline.flowLazyDependencyV110ManifestObserved);
        printLine("Flow IPC v1.10 lazy dependency request observed",
                pipeline.flowLazyDependencyV110RequestObserved);
        printLine("Flow IPC v1.10 lazy dependency stage observed",
                pipeline.flowLazyDependencyV110StageObserved);
        printLine("Flow IPC v1.10 lazy dependency DOM observed",
                pipeline.flowLazyDependencyV110DomObserved);
        printLine("Flow IPC v1.10 production runtime manifest observed",
                pipeline.productionRuntimeV110ManifestObserved);
        printLine("Flow IPC v1.10 production runtime request observed",
                pipeline.productionRuntimeV110RequestObserved);
        printLine("Flow IPC v1.10 production runtime stage observed",
                pipeline.productionRuntimeV110StageObserved);
        printLine("Flow IPC v1.10 production runtime DOM observed",
                pipeline.productionRuntimeV110DomObserved);
        printLine("Flow IPC v1.11 loadOnDemand chunk manifest observed",
                pipeline.generatedLoadOnDemandChunkV111ManifestObserved);
        printLine("Flow IPC v1.11 loadOnDemand UIDL expression observed",
                pipeline.generatedLoadOnDemandChunkV111UidlExpressionObserved);
        printLine("Flow IPC v1.11 loadOnDemand chunk request observed",
                pipeline.generatedLoadOnDemandChunkV111RequestObserved);
        printLine("Flow IPC v1.11 loadOnDemand chunk stage observed",
                pipeline.generatedLoadOnDemandChunkV111StageObserved);
        printLine("Flow IPC v1.11 loadOnDemand chunk DOM observed",
                pipeline.generatedLoadOnDemandChunkV111DomObserved);
        printLine("Flow IPC v1.12 loadOnDemand chunk-file manifest observed",
                pipeline.generatedLoadOnDemandChunkFileV112ManifestObserved);
        printLine("Flow IPC v1.12 loadOnDemand chunk-file request observed",
                pipeline.generatedLoadOnDemandChunkFileV112RequestObserved);
        printLine("Flow IPC v1.12 loadOnDemand chunk-file stage observed",
                pipeline.generatedLoadOnDemandChunkFileV112StageObserved);
        printLine("Flow IPC v1.12 loadOnDemand chunk-file DOM observed",
                pipeline.generatedLoadOnDemandChunkFileV112DomObserved);
        printLine("Flow IPC v1.13 production owner manifest observed",
                productionOwner.ownerV113ManifestObserved);
        printLine("Flow IPC v1.13 production owner request observed",
                productionOwner.ownerV113RequestObserved);
        printLine("Flow IPC v1.13 production owner stage observed",
                productionOwner.ownerV113StageObserved);
        printLine("Flow IPC v1.13 production owner DOM observed",
                productionOwner.ownerV113DomObserved);
        printLine("Flow IPC v1.14 production owner import manifest observed",
                productionOwner.ownerImportV114ManifestObserved);
        printLine("Flow IPC v1.14 production owner import request observed",
                productionOwner.ownerImportV114RequestObserved);
        printLine("Flow IPC v1.14 production owner import stage observed",
                productionOwner.ownerImportV114StageObserved);
        printLine("Flow IPC v1.14 production owner import DOM observed",
                productionOwner.ownerImportV114DomObserved);
        printLine("Flow IPC v1.15 production owner dependency manifest observed",
                productionOwner.ownerDependenciesV115ManifestObserved);
        printLine("Flow IPC v1.15 production owner dependency request observed",
                productionOwner.ownerDependenciesV115RequestObserved);
        printLine("Flow IPC v1.15 production owner dependency stage observed",
                productionOwner.ownerDependenciesV115StageObserved);
        printLine("Flow IPC v1.15 production owner dependency DOM observed",
                productionOwner.ownerDependenciesV115DomObserved);
        printLine("Flow IPC v1.16 production owner ordering manifest observed",
                productionOwner.ownerOrderingV116ManifestObserved);
        printLine("Flow IPC v1.16 production owner ordering request observed",
                productionOwner.ownerOrderingV116RequestObserved);
        printLine("Flow IPC v1.16 production owner ordering stage observed",
                productionOwner.ownerOrderingV116StageObserved);
        printLine("Flow IPC v1.16 production owner ordering DOM observed",
                productionOwner.ownerOrderingV116DomObserved);
        printLine("Flow IPC v1.17 resource scheme manifest observed",
                pipeline.resourceSchemeV117ManifestObserved);
        printLine("Flow IPC v1.17 resource scheme host install observed",
                pipeline.resourceSchemeV117HostInstallObserved);
        printLine("Flow IPC v1.17 resource scheme stage observed",
                pipeline.resourceSchemeV117StageObserved);
        printLine("Flow IPC v1.17 resource scheme handler request/response observed",
                pipeline.resourceSchemeV117HandlerRequestObserved,
                pipeline.resourceSchemeV117HandlerResponseObserved);
        printLine("Flow IPC v1.17 resource scheme executed DOM observed",
                pipeline.resourceSchemeV117ExecutedObserved,
                pipeline.resourceSchemeV117DomObserved);
        printLine("Flow IPC v1.17 resource scheme fallback classification observed",
                pipeline.resourceSchemeV117FallbackClassifiedObserved);
        printLine("Flow IPC v1.18 production owner resource scheme manifest/request observed",
                productionOwner.resourceSchemeV118ManifestObserved,
                productionOwner.resourceSchemeV118RequestObserved);
        printLine("Flow IPC v1.18 production owner resource scheme root stage observed",
                productionOwner.resourceSchemeV118RootStageObserved);
        printLine("Flow IPC v1.18 production owner resource scheme dependency stage observed",
                productionOwner.resourceSchemeV118DependencyStageObserved);
        printLine("Flow IPC v1.18 production owner resource scheme root handler request/response observed",
                productionOwner.resourceSchemeV118RootHandlerRequestObserved,
                productionOwner.resourceSchemeV118RootHandlerResponseObserved);
        printLine("Flow IPC v1.18 production owner resource scheme dependency handler request/response observed",
                productionOwner.resourceSchemeV118DependencyHandlerRequestObserved,
                productionOwner.resourceSchemeV118DependencyHandlerResponseObserved);
        printLine("Flow IPC v1.18 production owner resource scheme import/DOM observed",
                productionOwner.resourceSchemeV118ImportObserved,
                productionOwner.resourceSchemeV118DomObserved);
        printLine("Flow IPC v1.19 production owner side-effect manifest/request observed",
                productionOwner.sideEffectV119ManifestObserved,
                productionOwner.sideEffectV119RequestObserved);
        printLine("Flow IPC v1.19 production owner side-effect stage/DOM observed",
                productionOwner.sideEffectV119StageObserved,
                productionOwner.sideEffectV119DomObserved);
        printLine("Flow IPC v1.19 component owner reuse decision observed",
                productionOwner.sideEffectV119ReuseDecisionObserved);
        printLine("Flow IPC v1.20 production owner-owned runtime manifest/request observed",
                productionOwner.ownedRuntimeV120ManifestObserved,
                productionOwner.ownedRuntimeV120RequestObserved);
        printLine("Flow IPC v1.20 production owner-owned runtime stage/DOM observed",
                productionOwner.ownedRuntimeV120StageObserved,
                productionOwner.ownedRuntimeV120DomObserved);
        printLine("Flow IPC v1.20 component owner reuse decision observed",
                productionOwner.ownedRuntimeV120ReuseDecisionObserved);
    }
}
