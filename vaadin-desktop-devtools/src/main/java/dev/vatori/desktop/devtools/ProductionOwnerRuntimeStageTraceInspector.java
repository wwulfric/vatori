package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerRuntimeStageTraceInspector {
    private static final String PRODUCTION_BUNDLE_RUNTIME_OWNER = "production-bundle-runtime";
    private static final String PRODUCTION_BUNDLE_ARTIFACT_FAMILY =
            "\"artifactFamily\":\"production-bundle\"";
    private static final String PRODUCTION_RUNTIME_ENTRY_ARTIFACT =
            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";
    private static final String PRODUCTION_OWNER_V113_ID =
            "production-owner-v1_13:" + PRODUCTION_RUNTIME_ENTRY_ARTIFACT;
    private static final String PRODUCTION_OWNER_IMPORT_V114_ID =
            "production-owner-import-v1_14:" + PRODUCTION_RUNTIME_ENTRY_ARTIFACT;

    private ProductionOwnerRuntimeStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isProductionRuntimeStage(line)
                && containsStringField(line, "runtimeResolverContractVersion",
                        "flow-ipc-frontend-runtime-resolver-v1_10")
                && containsStringField(line, "productionBundleRuntimeVersion",
                        "flow-ipc-production-runtime-v1_10")
                && containsStringField(line, "productionBundleRuntimeRegistrySource",
                        "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                && containsStringField(line, "productionBundleRuntimeSelectedArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "productionBundleRuntimeBlockerVersion",
                        "flow-ipc-production-runtime-v1_10")
                && hasAttemptExecutedOrFailed(line)) {
            summary.frontendBuildPipeline.productionRuntimeV110StageObserved = true;
        }
        if (isOwnedProductionRuntimeStage(line)
                && containsStringField(line, "productionBundleRuntimeOwnerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsStringField(line, "productionBundleRuntimeOwner",
                        "flow-ipc-production-bundle-runtime")
                && containsStringField(line, "productionBundleRuntimeEntryArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "productionBundleRuntimeOwnerModuleIdentity",
                        PRODUCTION_OWNER_V113_ID)
                && containsStringField(line, "productionBundleRuntimeOwnerExecutionPolicy",
                        "stage-owner-contract-production-bundle-runtime-v1_13")
                && containsStringField(line, "productionBundleRuntimeOwnerBlockerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsProductionOwnerFailureLayer(line, "productionBundleRuntimeOwnerFailureLayer")
                && containsStringFieldPrefix(line, "productionBundleRuntimeOwnerExecutionResult",
                        "strict-blocker:")
                && containsStringField(line, "moduleIdentityKey",
                        PRODUCTION_OWNER_V113_ID)
                && containsLiteralField(line, "componentProductionOwnerReused", "false")
                && hasAttemptStrictBlockerOrFailed(line)) {
            summary.productionOwnerRuntime.ownerV113StageObserved = true;
        }
        if (isOwnedProductionRuntimeStage(line)
                && containsStringField(line, "productionBundleRuntimeOwnerImportVersion",
                        "flow-ipc-production-owner-import-v1_14")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPolicy",
                        "controlled-owner-dynamic-import-after-component-graph-v1_14")
                && containsStringField(line, "productionBundleRuntimeOwnerImportArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsLiteralField(line, "productionBundleRuntimeOwnerImportAttempted", "true")
                && line.contains("\"productionBundleRuntimeOwnerImportDependencyRewriteResult\"")
                && line.contains("\"productionBundleRuntimeOwnerImportVaadinGlobalBefore\"")
                && line.contains("\"productionBundleRuntimeOwnerImportVaadinGlobalAfter\"")
                && containsStringField(line, "productionBundleRuntimeOwnerImportModuleIdentity",
                        PRODUCTION_OWNER_IMPORT_V114_ID)
                && containsProductionOwnerImportFinalResult(line,
                        "productionBundleRuntimeOwnerExecutionResult",
                        "productionBundleRuntimeOwnerFailureLayer")
                && hasExecutedStrictBlockerOrFailed(line)) {
            summary.productionOwnerRuntime.ownerImportV114StageObserved = true;
        }
        if (isOwnedProductionRuntimeStage(line)
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverVersion",
                        "flow-ipc-production-owner-deps-v1_15")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverRoot",
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverPolicy",
                        "owner-scoped-recursive-relative-import-graph-v1_15")
                && containsStringField(line,
                        "productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                        "production-owner-deps-v1_15")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPhase",
                        "after-component-imports")
                && line.contains("productionBundleRuntimeOwnerImportPhaseCandidates")
                && line.contains("isolated-preflight")
                && line.contains("before-component-imports")
                && line.contains("productionBundleRuntimeOwnerImportDiagnosticModes")
                && containsLiteralField(line, "productionBundleRuntimeOwnerDependencyGraphClosed", "true")
                && containsPositiveIntegerField(line,
                        "productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount")
                && containsPositiveIntegerField(line,
                        "productionBundleRuntimeOwnerDependencyGraphResolvedCount")
                && containsPositiveIntegerField(line,
                        "productionBundleRuntimeOwnerDependencyGraphStagedCount")
                && containsPositiveIntegerField(line, "productionBundleRuntimeOwnerImportStageCount")
                && line.contains("productionBundleRuntimeOwnerDependencyClassifications")
                && line.contains("resolved-existing-artifact")
                && line.contains("productionBundleRuntimeOwnerImportResolvedDependencyPaths")
                && (containsPath(line,
                        "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js")
                        || containsPath(line,
                                "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js"))
                && line.contains("productionBundleRuntimeOwnerImportMissingDependencyPaths")
                && line.contains("productionBundleRuntimeOwnerImportMissingSpecifiers")
                && containsProductionOwnerDependencyFinalResult(line,
                        "productionBundleRuntimeOwnerExecutionResult",
                        "productionBundleRuntimeOwnerFailureLayer")
                && !containsStringField(line, "productionBundleRuntimeOwnerFailureLayer",
                        "production-owner-relative-import-unresolved")
                && hasExecutedStrictBlockerOrFailed(line)) {
            summary.productionOwnerRuntime.ownerDependenciesV115StageObserved = true;
        }
        if (isOwnedProductionRuntimeStage(line)
                && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingVersion",
                        "flow-ipc-production-owner-ordering-v1_16")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPhasePolicy",
                        "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                && containsOwnerFirstOrIsolatedPhase(line, "productionBundleRuntimeOwnerImportPhase")
                && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingObservedPhases",
                        "before-component-imports")
                && line.contains("productionBundleRuntimeOwnerImportOrderingResult")
                && line.contains("owner-first-import-attempted-before-component-focused-imports")
                && containsStringField(line, "componentProductionOwnerReuseVersion",
                        "flow-ipc-component-owner-reuse-v1_16")
                && containsStringField(line, "componentProductionOwnerReusePolicy",
                        "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                && line.contains("componentProductionOwnerReuseMode")
                && line.contains("componentProductionOwnerReuseReason")
                && containsProductionOwnerOrderingFinalResult(line,
                        "productionBundleRuntimeOwnerExecutionResult",
                        "productionBundleRuntimeOwnerFailureLayer")
                && !containsStringField(line, "productionBundleRuntimeOwnerFailureLayer",
                        "production-owner-relative-import-unresolved")
                && hasExecutedStrictBlockerOrFailed(line)) {
            summary.productionOwnerRuntime.ownerOrderingV116StageObserved = true;
        }
    }

    private static boolean isProductionRuntimeStage(String line) {
        return isResourceStage(line)
                && line.contains(PRODUCTION_BUNDLE_ARTIFACT_FAMILY)
                && containsPath(line, PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsLiteralField(line, "productionBundleRuntimeArtifact", "true");
    }

    private static boolean isOwnedProductionRuntimeStage(String line) {
        return isProductionRuntimeStage(line)
                && containsStringField(line, "owner", PRODUCTION_BUNDLE_RUNTIME_OWNER);
    }
}
