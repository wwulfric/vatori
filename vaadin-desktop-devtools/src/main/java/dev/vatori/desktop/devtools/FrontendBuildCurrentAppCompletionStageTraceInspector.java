package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;
import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildCurrentAppCompletionStageTraceInspector {
    private FrontendBuildCurrentAppCompletionStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isResourceStage(line)
                && containsPath(line, "current-app-frontend/generated/flow/chunks/")
                && containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)
                && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "true")) {
            summary.currentAppBuildFrontend.generatedChunkPackageEdgeV133ExecutedObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)
                && containsLiteralField(line,
                        "currentAppGeneratedImportsRootProductionSupportExecuted", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsRootPackageGraphExecuted", "true")) {
            summary.currentAppBuildFrontend.generatedImportsRootPackageGraphV134SupportExecutedObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppProductionRuntimeOwnershipV135Fields(line)
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipArtifactsStaged", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipPrimaryCandidateSelected", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipHandoffReady", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied", "false")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipOwnerReuseSatisfied", "false")) {
            summary.currentAppProductionOwner.runtimeOwnershipV135HandoffObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppProductionOwnerImportV136Fields(line)
                && containsLiteralField(line, "currentAppProductionOwnerImportAttempted", "true")
                && containsLiteralField(line, "currentAppProductionOwnerImportOverCustomScheme", "true")
                && containsPositiveIntegerField(line, "currentAppProductionOwnerImportDependencyStageCount")
                && line.contains("currentAppProductionOwnerImportExecutionResult")
                && line.contains("currentAppProductionOwnerImportSideEffectProof")
                && containsCurrentAppProductionOwnerImportV136FailureOrSuccess(line)) {
            summary.currentAppProductionOwner.ownerImportV136AttemptObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)
                && containsStringField(line, "currentAppProductionOwnerComponentReuseDecision",
                        "current-app-owner-reused")
                && containsLiteralField(line, "currentAppProductionOwnerComponentReused", "true")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseReusedTags",
                        "vaadin-text-field")
                && containsStringField(line,
                        "currentAppProductionOwnerComponentReuseSkippedFocusedImports",
                        "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsStringField(line, "executionResult",
                        "current-app-owner-reused:META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && hasExecuted(line)) {
            summary.currentAppOwnerComponentReuse.productionOwnerComponentReuseV137TextFieldReuseObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsCurrentAppOwnerComponentReuseMatrixV138Complete(line)
                && containsStringField(line, "currentAppProductionOwnerComponentReuseDecision",
                        "current-app-owner-reused")
                && containsStringField(line, "executionResult",
                        "current-app-owner-reused:META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && hasExecuted(line)) {
            summary.currentAppOwnerComponentReuse.matrixV138CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsCurrentAppOwnerEarlyImportV139Complete(line)
                && hasExecuted(line)) {
            summary.currentAppOwnerEarlyImport.earlyImportV139CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsOwnerSideEffectV140Complete(line)
                && hasExecuted(line)) {
            summary.generatedImportsOwner.sideEffectV140CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Complete(line)
                && hasExecuted(line)) {
            summary.generatedImportsOwner.selectOwnerCoverageV141CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142ImportComplete(line)
                && hasExecuted(line)) {
            summary.generatedImportsOwner.runtimeContractV142CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142ImportComplete(line)
                && containsGeneratedLazySpiDecisionV143ChunkExecuted(line)
                && hasExecuted(line)) {
            summary.generatedLazySpiDecision.decisionV143CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-frontend/generated/flow/chunks/")
                && containsGeneratedLazySpiDecisionV143ChunkExecuted(line)
                && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "true")) {
            summary.generatedLazySpiDecision.decisionV143ChunkExecutedObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsPackagingFrontendInventoryV144Complete(line)
                && hasExecuted(line)) {
            summary.packaging.frontendInventoryV144CompleteStageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsPackagingRuntimeHandoffReuseV145Complete(line)
                && hasExecuted(line)) {
            summary.packaging.runtimeHandoffReuseV145CompleteStageObserved = true;
        }
    }
}
