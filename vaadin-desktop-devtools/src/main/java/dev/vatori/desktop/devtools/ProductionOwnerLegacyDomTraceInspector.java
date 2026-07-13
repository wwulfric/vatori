package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerLegacyDomTraceInspector {
    private static final String PRODUCTION_RUNTIME_ENTRY_ARTIFACT =
            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";
    private static final String PRODUCTION_OWNER_MODULE_IDENTITY =
            "production-owner-v1_13:" + PRODUCTION_RUNTIME_ENTRY_ARTIFACT;
    private static final String PRODUCTION_OWNER_IMPORT_MODULE_IDENTITY =
            "production-owner-import-v1_14:" + PRODUCTION_RUNTIME_ENTRY_ARTIFACT;

    private ProductionOwnerLegacyDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!isWebviewDom(line)) {
            return;
        }
        if (containsTrueFields(line,
                        "frontendArtifactProductionBundleRuntimeObserved",
                        "frontendArtifactProductionBundleRuntimeStageAttempted",
                        "frontendArtifactProductionBundleRuntimeBlocker")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeVersion",
                        "flow-ipc-production-runtime-v1_10")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeRegistrySource",
                        "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeSelectedArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeFailureLayer",
                        "production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports")
                && (line.contains("execution-attempt:" + PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                        || containsStringFieldPrefix(line,
                                "frontendArtifactProductionBundleRuntimeExecutionResult",
                                "strict-blocker:production-owner-")
                        || containsStringFieldPrefix(line,
                                "frontendArtifactProductionBundleRuntimeExecutionResult",
                                "imported:" + PRODUCTION_RUNTIME_ENTRY_ARTIFACT))) {
            summary.frontendBuildPipeline.productionRuntimeV110DomObserved = true;
        }
        if (containsProductionOwnerV113IdentityFields(line)
                && containsFalseFields(line, "frontendArtifactProductionBundleRuntimeOwnerImported")
                && containsLiteralField(line, "frontendArtifactProductionBundleRuntimeOwnerBlocker", "true")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy",
                        "stage-owner-contract-production-bundle-runtime-v1_13")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerBlockerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsProductionOwnerFailureLayer(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                && containsStringFieldPrefix(line,
                        "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                        "strict-blocker:")
                && containsStringFieldPrefix(line,
                        "frontendArtifactProductionBundleRuntimeOwnerSideEffectProof",
                        "owner-stage-resolved")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerV113DomObserved = true;
        }
        if (containsProductionOwnerV113IdentityFields(line)
                && containsOwnerImportAttempted(line)
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportVersion",
                        "flow-ipc-production-owner-import-v1_14")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerV113DomObserved = true;
        }
        if (containsOwnerObservedStage(line)
                && containsOwnerImportAttempted(line)
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportVersion",
                        "flow-ipc-production-owner-import-v1_14")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportPolicy",
                        "controlled-owner-dynamic-import-after-component-graph-v1_14")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportDependencyRewriteResult")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalBefore")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalAfter")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportModuleIdentity",
                        PRODUCTION_OWNER_IMPORT_MODULE_IDENTITY)
                && containsProductionOwnerImportFinalResult(line,
                        "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerImportV114DomObserved = true;
        }
        if (containsOwnerObservedStage(line)
                && containsOwnerImportAttempted(line)
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion",
                        "flow-ipc-production-owner-deps-v1_15")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot",
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy",
                        "owner-scoped-recursive-relative-import-graph-v1_15")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                        "production-owner-deps-v1_15")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportPhase")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates")
                && line.contains("isolated-preflight")
                && line.contains("before-component-imports")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes")
                && containsLiteralField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphClosed", "true")
                && containsPositiveIntegerField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount")
                && containsPositiveIntegerField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphResolvedCount")
                && containsPositiveIntegerField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerDependencyGraphStagedCount")
                && containsPositiveIntegerField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportStageCount")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerDependencyClassifications")
                && line.contains("resolved-existing-artifact")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportMissingDependencyPaths")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportMissingSpecifiers")
                && containsProductionOwnerDependencyFinalResult(line,
                        "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                && !containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                        "production-owner-relative-import-unresolved")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerDependenciesV115DomObserved = true;
        }
        if (containsOwnerObservedStage(line)
                && containsOwnerImportAttempted(line)
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion",
                        "flow-ipc-production-owner-ordering-v1_16")
                && containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy",
                        "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases")
                && line.contains("before-component-imports")
                && line.contains("frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult")
                && line.contains("owner-first-import-attempted-before-component-focused-imports")
                && containsStringField(line, "componentProductionOwnerReuseVersion",
                        "flow-ipc-component-owner-reuse-v1_16")
                && containsStringField(line, "componentProductionOwnerReusePolicy",
                        "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                && line.contains("componentProductionOwnerReuseMode")
                && line.contains("componentProductionOwnerReuseReason")
                && containsProductionOwnerOrderingFinalResult(line,
                        "frontendArtifactProductionBundleRuntimeOwnerExecutionResult",
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer")
                && !containsStringField(line,
                        "frontendArtifactProductionBundleRuntimeOwnerFailureLayer",
                        "production-owner-relative-import-unresolved")) {
            summary.productionOwnerRuntime.ownerOrderingV116DomObserved = true;
        }
    }

    private static boolean containsProductionOwnerV113IdentityFields(String line) {
        return containsOwnerObservedStage(line)
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwner",
                        "flow-ipc-production-bundle-runtime")
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerEntryArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "frontendArtifactProductionBundleRuntimeOwnerModuleIdentity",
                        PRODUCTION_OWNER_MODULE_IDENTITY);
    }

    private static boolean containsOwnerObservedStage(String line) {
        return containsTrueFields(line,
                "frontendArtifactProductionBundleRuntimeOwnerObserved",
                "frontendArtifactProductionBundleRuntimeOwnerStageObserved");
    }

    private static boolean containsOwnerImportAttempted(String line) {
        return containsLiteralField(line,
                "frontendArtifactProductionBundleRuntimeOwnerImportAttempted", "true");
    }
}
