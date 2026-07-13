package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceSummaryLinePrinter.printLine;

final class FrontendBuildGeneratedImportsTraceSummaryPrinter {
    private FrontendBuildGeneratedImportsTraceSummaryPrinter() {
    }

    static void print(TraceSummary summary) {
        FrontendBuildGeneratedImportsOwnerTraceFields generatedImportsOwner =
                summary.generatedImportsOwner;
        FrontendBuildGeneratedLazySpiDecisionTraceFields generatedLazySpiDecision =
                summary.generatedLazySpiDecision;
        FrontendBuildPackagingTraceFields packaging = summary.packaging;
        printLine("Flow IPC v1.40 current app generated imports owner side effect manifest observed",
                generatedImportsOwner.sideEffectV140ManifestObserved);
        printLine("Flow IPC v1.40 current app generated imports owner side effect request/response observed",
                generatedImportsOwner.sideEffectV140RequestObserved,
                generatedImportsOwner.sideEffectV140ResponseObserved);
        printLine("Flow IPC v1.40 current app generated imports owner side effect stage/complete/DOM observed",
                generatedImportsOwner.sideEffectV140StageObserved,
                generatedImportsOwner.sideEffectV140CompleteStageObserved,
                generatedImportsOwner.sideEffectV140DomObserved);
        printLine("Flow IPC v1.41 current app generated imports Select owner coverage manifest observed",
                generatedImportsOwner.selectOwnerCoverageV141ManifestObserved);
        printLine("Flow IPC v1.41 current app generated imports Select owner coverage request/response observed",
                generatedImportsOwner.selectOwnerCoverageV141RequestObserved,
                generatedImportsOwner.selectOwnerCoverageV141ResponseObserved);
        printLine("Flow IPC v1.41 current app generated imports Select owner coverage stage/complete/DOM observed",
                generatedImportsOwner.selectOwnerCoverageV141StageObserved,
                generatedImportsOwner.selectOwnerCoverageV141CompleteStageObserved,
                generatedImportsOwner.selectOwnerCoverageV141DomObserved);
        printLine("Flow IPC v1.42 generated imports owner runtime contract manifest observed",
                generatedImportsOwner.runtimeContractV142ManifestObserved);
        printLine("Flow IPC v1.42 generated imports owner runtime contract request/response observed",
                generatedImportsOwner.runtimeContractV142RequestObserved,
                generatedImportsOwner.runtimeContractV142ResponseObserved);
        printLine("Flow IPC v1.42 generated imports owner runtime contract stage/complete/DOM observed",
                generatedImportsOwner.runtimeContractV142StageObserved,
                generatedImportsOwner.runtimeContractV142CompleteStageObserved,
                generatedImportsOwner.runtimeContractV142DomObserved);
        printLine("Flow IPC v1.43 generated lazy SPI decision manifest observed",
                generatedLazySpiDecision.decisionV143ManifestObserved);
        printLine("Flow IPC v1.43 generated lazy SPI decision request/response observed",
                generatedLazySpiDecision.decisionV143RequestObserved,
                generatedLazySpiDecision.decisionV143ResponseObserved);
        printLine("Flow IPC v1.43 generated lazy SPI decision stage/chunk/complete/DOM observed",
                generatedLazySpiDecision.decisionV143StageObserved,
                generatedLazySpiDecision.decisionV143ChunkExecutedObserved,
                generatedLazySpiDecision.decisionV143CompleteStageObserved,
                generatedLazySpiDecision.decisionV143DomObserved);
        printLine("Flow IPC v1.44 packaging frontend inventory manifest observed",
                packaging.frontendInventoryV144ManifestObserved);
        printLine("Flow IPC v1.44 packaging frontend inventory request/response observed",
                packaging.frontendInventoryV144RequestObserved,
                packaging.frontendInventoryV144ResponseObserved);
        printLine("Flow IPC v1.44 packaging frontend inventory stage/complete/DOM observed",
                packaging.frontendInventoryV144StageObserved,
                packaging.frontendInventoryV144CompleteStageObserved,
                packaging.frontendInventoryV144DomObserved);
        printLine("Flow IPC v1.45 packaging runtime handoff reuse manifest observed",
                packaging.runtimeHandoffReuseV145ManifestObserved);
        printLine("Flow IPC v1.45 packaging runtime handoff reuse request/response observed",
                packaging.runtimeHandoffReuseV145RequestObserved,
                packaging.runtimeHandoffReuseV145ResponseObserved);
        printLine("Flow IPC v1.45 packaging runtime handoff reuse stage/complete/DOM/framework observed",
                packaging.runtimeHandoffReuseV145StageObserved,
                packaging.runtimeHandoffReuseV145CompleteStageObserved,
                packaging.runtimeHandoffReuseV145DomObserved,
                packaging.runtimeHandoffReuseV145FrameworkDomObserved);
    }
}
