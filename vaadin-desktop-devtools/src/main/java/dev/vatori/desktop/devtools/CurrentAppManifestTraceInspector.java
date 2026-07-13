package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;

final class CurrentAppManifestTraceInspector {
    private CurrentAppManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppBuildFrontendOutputV132Fields(line)) {
            summary.currentAppBuildFrontend.outputV132ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)) {
            summary.currentAppBuildFrontend.generatedChunkPackageEdgeV133ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)) {
            summary.currentAppBuildFrontend.generatedImportsRootPackageGraphV134ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppProductionRuntimeOwnershipV135Fields(line)) {
            summary.currentAppProductionOwner.runtimeOwnershipV135ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppProductionOwnerImportV136Fields(line)) {
            summary.currentAppProductionOwner.ownerImportV136ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppProductionOwnerImportV136Fields(line)
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)) {
            summary.currentAppOwnerComponentReuse.productionOwnerComponentReuseV137ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)
                && containsCurrentAppOwnerComponentReuseMatrixV138Fields(line)) {
            summary.currentAppOwnerComponentReuse.matrixV138ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppOwnerEarlyImportV139Fields(line)) {
            summary.currentAppOwnerEarlyImport.earlyImportV139ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(line)) {
            summary.generatedImportsOwner.sideEffectV140ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(line)) {
            summary.generatedImportsOwner.selectOwnerCoverageV141ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(line)) {
            summary.generatedImportsOwner.runtimeContractV142ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGeneratedLazySpiDecisionV143Fields(line)) {
            summary.generatedLazySpiDecision.decisionV143ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsPackagingFrontendInventoryV144Fields(line)) {
            summary.packaging.frontendInventoryV144ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsPackagingRuntimeHandoffReuseV145Fields(line)) {
            summary.packaging.runtimeHandoffReuseV145ManifestObserved = true;
        }
    }
}
