package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrontendBuildResolverTraceSummaryPrinter {
    private FrontendBuildResolverTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrontendBuildPipelineTraceFields pipeline = summary.frontendBuildPipeline;
        ProductionOwnerRuntimeTraceFields productionOwner =
                summary.productionOwnerRuntime;
        printLine("Flow IPC v1.21 frontend resolver contract manifest/request/response observed",
                pipeline.resolverContractV121ManifestObserved,
                pipeline.resolverContractV121RequestObserved,
                pipeline.resolverContractV121ResponseObserved);
        printLine("Flow IPC v1.21 frontend resolver contract stage blob/custom/gap observed",
                pipeline.resolverContractV121BlobStageObserved,
                pipeline.resolverContractV121CustomSchemeStageObserved,
                pipeline.resolverContractV121StrictOrGapStageObserved);
        printLine("Flow IPC v1.21 frontend resolver contract DOM observed",
                pipeline.resolverContractV121DomObserved);
        printLine("Flow IPC v1.22 generated chunk fixture request observed",
                pipeline.generatedChunkFileFixtureV122RequestObserved);
        printLine("Flow IPC v1.22 generated chunk fixture imports/chunk response observed",
                pipeline.generatedChunkFileFixtureV122ImportsResponseObserved,
                pipeline.generatedChunkFileFixtureV122ChunkResponseObserved);
        printLine("Flow IPC v1.22 generated chunk fixture stage/DOM observed",
                pipeline.generatedChunkFileFixtureV122StageObserved,
                pipeline.generatedChunkFileFixtureV122DomObserved);
        printLine("Flow IPC v1.23 production owner positive fixture manifest observed",
                productionOwner.positiveFixtureV123ManifestObserved);
        printLine("Flow IPC v1.23 production owner positive fixture request root/dependency observed",
                productionOwner.positiveFixtureV123RootRequestObserved,
                productionOwner.positiveFixtureV123DependencyRequestObserved);
        printLine("Flow IPC v1.23 production owner positive fixture response root/dependency observed",
                productionOwner.positiveFixtureV123RootResponseObserved,
                productionOwner.positiveFixtureV123DependencyResponseObserved);
        printLine("Flow IPC v1.23 production owner positive fixture stage root/dependency observed",
                productionOwner.positiveFixtureV123RootStageObserved,
                productionOwner.positiveFixtureV123DependencyStageObserved);
        printLine("Flow IPC v1.23 production owner positive fixture DOM/real blocker observed",
                productionOwner.positiveFixtureV123DomObserved,
                productionOwner.positiveFixtureV123RealBlockerObserved);
        printLine("Flow IPC v1.24 frontend pipeline closure manifest observed",
                pipeline.pipelineClosureV124ManifestObserved);
        printLine("Flow IPC v1.24 frontend pipeline closure request/response observed",
                pipeline.pipelineClosureV124RequestObserved,
                pipeline.pipelineClosureV124ResponseObserved);
        printLine("Flow IPC v1.24 frontend pipeline closure stage/DOM observed",
                pipeline.pipelineClosureV124StageObserved,
                pipeline.pipelineClosureV124DomObserved);
        printLine("Flow IPC v1.24 frontend pipeline closure fixture proofs/real blockers observed",
                pipeline.pipelineClosureV124FixtureProofsObserved,
                pipeline.pipelineClosureV124RealBlockersObserved);
        printLine("Flow IPC v1.25 packaged frontend baseline manifest observed",
                pipeline.packagedFrontendBaselineV125ManifestObserved);
        printLine("Flow IPC v1.25 packaged frontend baseline request/response observed",
                pipeline.packagedFrontendBaselineV125RequestObserved,
                pipeline.packagedFrontendBaselineV125ResponseObserved);
        printLine("Flow IPC v1.25 packaged frontend baseline stage/custom-scheme/fallback observed",
                pipeline.packagedFrontendBaselineV125StageObserved,
                pipeline.packagedFrontendBaselineV125CustomSchemeStageObserved,
                pipeline.packagedFrontendBaselineV125DiagnosticBlobFallbackObserved);
        printLine("Flow IPC v1.25 packaged frontend baseline DOM/custom-scheme/fallback/real blockers observed",
                pipeline.packagedFrontendBaselineV125DomObserved,
                pipeline.packagedFrontendBaselineV125CustomSchemeDomObserved,
                pipeline.packagedFrontendBaselineV125DiagnosticBlobFallbackDomObserved,
                pipeline.packagedFrontendBaselineV125RealBlockersObserved);
    }
}
