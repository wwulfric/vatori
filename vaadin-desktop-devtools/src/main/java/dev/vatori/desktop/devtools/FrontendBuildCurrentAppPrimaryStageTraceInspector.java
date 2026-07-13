package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;
import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildCurrentAppPrimaryStageTraceInspector {
    private FrontendBuildCurrentAppPrimaryStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isResourceStage(line)
                && containsPath(line, "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsCurrentAppBuildFrontendOutputV132Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppBuildFrontend.outputV132StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppBuildFrontend.generatedChunkPackageEdgeV133StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppBuildFrontend.generatedImportsRootPackageGraphV134StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsCurrentAppProductionRuntimeOwnershipV135Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppProductionOwner.runtimeOwnershipV135StageObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppProductionOwnerImportV136Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppProductionOwner.ownerImportV136StageObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppOwnerComponentReuse.productionOwnerComponentReuseV137StageObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppOwnerComponentReuseMatrixV138Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppOwnerComponentReuse.matrixV138StageObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppOwnerEarlyImportV139Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.currentAppOwnerEarlyImport.earlyImportV139StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedImportsOwner.sideEffectV140StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedImportsOwner.selectOwnerCoverageV141StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedImportsOwner.runtimeContractV142StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsGeneratedLazySpiDecisionV143Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedLazySpiDecision.decisionV143StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsPackagingFrontendInventoryV144Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.packaging.frontendInventoryV144StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsPackagingRuntimeHandoffReuseV145Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.packaging.runtimeHandoffReuseV145StageObserved = true;
        }
    }
}
