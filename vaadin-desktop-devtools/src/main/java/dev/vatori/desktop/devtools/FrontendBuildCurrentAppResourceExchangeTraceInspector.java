package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildCurrentAppResourceExchangeTraceInspector {
    private static final String CURRENT_APP_GENERATED_IMPORTS =
            "current-app-frontend/generated/flow/generated-flow-imports.js";
    private static final String CURRENT_APP_PRODUCTION_BUILD_ROOT =
            "current-app-production/VAADIN/build/";
    private static final String CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX =
            "current-app-production/VAADIN/build/generated-flow-imports-";

    private FrontendBuildCurrentAppResourceExchangeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        inspectRequests(summary, line);
        inspectResponses(summary, line);
    }

    private static void inspectRequests(TraceSummary summary, String line) {
        if (!isResourceRequest(line)) {
            return;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppBuildFrontendOutputV132Fields(line)) {
            summary.currentAppBuildFrontend.outputV132RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)) {
            summary.currentAppBuildFrontend.generatedChunkPackageEdgeV133RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)) {
            summary.currentAppBuildFrontend.generatedImportsRootPackageGraphV134RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionRuntimeOwnershipV135Fields(line)) {
            summary.currentAppProductionOwner.runtimeOwnershipV135RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionOwnerImportV136Fields(line)) {
            summary.currentAppProductionOwner.ownerImportV136RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionOwnerImportV136Fields(line)
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)) {
            summary.currentAppOwnerComponentReuse.productionOwnerComponentReuseV137RequestObserved = true;
        }
        if (containsCurrentAppOwnerComponentReuseMatrixCoveredComponentPath(line)) {
            summary.currentAppOwnerComponentReuse.matrixV138RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_BUILD_ROOT)
                && containsCurrentAppOwnerEarlyImportV139Fields(line)) {
            summary.currentAppOwnerEarlyImport.earlyImportV139RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(line)) {
            summary.generatedImportsOwner.sideEffectV140RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(line)) {
            summary.generatedImportsOwner.selectOwnerCoverageV141RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(line)) {
            summary.generatedImportsOwner.runtimeContractV142RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsGeneratedLazySpiDecisionV143Fields(line)) {
            summary.generatedLazySpiDecision.decisionV143RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsPackagingFrontendInventoryV144Fields(line)) {
            summary.packaging.frontendInventoryV144RequestObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsPackagingRuntimeHandoffReuseV145Fields(line)) {
            summary.packaging.runtimeHandoffReuseV145RequestObserved = true;
        }
    }

    private static void inspectResponses(TraceSummary summary, String line) {
        if (!isRedactedResourceResponse(line)) {
            return;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppBuildFrontendOutputV132Fields(line)) {
            summary.currentAppBuildFrontend.outputV132ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)) {
            summary.currentAppBuildFrontend.generatedChunkPackageEdgeV133ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)) {
            summary.currentAppBuildFrontend.generatedImportsRootPackageGraphV134ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionRuntimeOwnershipV135Fields(line)) {
            summary.currentAppProductionOwner.runtimeOwnershipV135ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionOwnerImportV136Fields(line)) {
            summary.currentAppProductionOwner.ownerImportV136ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionOwnerImportV136Fields(line)
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)) {
            summary.currentAppOwnerComponentReuse.productionOwnerComponentReuseV137ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_GENERATED_IMPORTS)
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)
                && containsCurrentAppOwnerComponentReuseMatrixV138Fields(line)) {
            summary.currentAppOwnerComponentReuse.matrixV138ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_BUILD_ROOT)
                && containsCurrentAppOwnerEarlyImportV139Fields(line)) {
            summary.currentAppOwnerEarlyImport.earlyImportV139ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(line)) {
            summary.generatedImportsOwner.sideEffectV140ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(line)) {
            summary.generatedImportsOwner.selectOwnerCoverageV141ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(line)) {
            summary.generatedImportsOwner.runtimeContractV142ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsGeneratedLazySpiDecisionV143Fields(line)) {
            summary.generatedLazySpiDecision.decisionV143ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsPackagingFrontendInventoryV144Fields(line)) {
            summary.packaging.frontendInventoryV144ResponseObserved = true;
        }
        if (containsPath(line, CURRENT_APP_PRODUCTION_GENERATED_IMPORTS_PREFIX)
                && containsPackagingRuntimeHandoffReuseV145Fields(line)) {
            summary.packaging.runtimeHandoffReuseV145ResponseObserved = true;
        }
    }

}
