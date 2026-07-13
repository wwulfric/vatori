package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerManifestTraceInspector {
    private static final String PRODUCTION_RUNTIME_ENTRY_ARTIFACT =
            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";

    private ProductionOwnerManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!isBootstrapComponentResourceResponse(line)) {
            return;
        }
        if (containsStringField(line, "productionBundleRuntimeVersion",
                        "flow-ipc-production-runtime-v1_10")
                && containsStringField(line, "productionBundleRuntimeRegistrySource",
                        "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                && containsStringField(line, "productionBundleRuntimeSelectedArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "productionBundleRuntimeBlockerVersion",
                        "flow-ipc-production-runtime-v1_10")) {
            summary.frontendBuildPipeline.productionRuntimeV110ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsStringField(line, "productionBundleRuntimeOwner",
                        "flow-ipc-production-bundle-runtime")
                && containsStringField(line, "productionBundleRuntimeEntryArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "productionBundleRuntimeSource",
                        "vaadin-dev-bundle-stats")
                && containsStringField(line, "productionBundleRuntimeSelectedBy",
                        "stats-entrypoint-or-focused-production-entry")
                && containsStringField(line, "productionBundleRuntimeOwnerModuleIdentity",
                        "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && containsStringField(line, "productionBundleRuntimeOwnerExecutionPolicy",
                        "stage-owner-contract-production-bundle-runtime-v1_13")
                && containsStringField(line, "productionBundleRuntimeOwnerBlockerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsProductionOwnerFailureLayer(line, "productionBundleRuntimeOwnerFailureLayer")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerV113ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerImportVersion",
                        "flow-ipc-production-owner-import-v1_14")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPolicy",
                        "controlled-owner-dynamic-import-after-component-graph-v1_14")
                && containsStringField(line, "productionBundleRuntimeOwnerImportArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && line.contains("productionBundleRuntimeOwnerImportExpectedSideEffects")
                && line.contains("productionBundleRuntimeOwnerImportAcceptableFailures")
                && line.contains("production-owner-browser-import-failed")
                && line.contains("production-owner-duplicate-custom-element")
                && line.contains("production-owner-side-effect-unobservable")
                && containsLiteralField(line, "productionBundleRuntimeOwnerImportAttempted", "false")
                && containsLiteralField(line, "productionBundleRuntimeOwnerImported", "false")) {
            summary.productionOwnerRuntime.ownerImportV114ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverVersion",
                        "flow-ipc-production-owner-deps-v1_15")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverRoot",
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverPolicy",
                        "owner-scoped-recursive-relative-import-graph-v1_15")
                && containsStringField(line,
                        "productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                        "production-owner-deps-v1_15")
                && line.contains("productionBundleRuntimeOwnerImportPhase")
                && line.contains("productionBundleRuntimeOwnerImportPhaseCandidates")
                && line.contains("isolated-preflight")
                && line.contains("before-component-imports")
                && line.contains("productionBundleRuntimeOwnerImportDiagnosticModes")
                && line.contains("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts")
                && containsPath(line,
                        "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js")
                && containsPath(line,
                        "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js")
                && line.contains("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures")
                && line.contains("production-owner-missing-real-artifact")
                && line.contains("flow-ipc-production-owner-deps-v1_15")
                && containsLiteralField(line, "productionBundleRuntimeOwnerDependencyGraphClosed", "false")) {
            summary.productionOwnerRuntime.ownerDependenciesV115ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerImportOrderingVersion",
                        "flow-ipc-production-owner-ordering-v1_16")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPhasePolicy",
                        "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                && line.contains("productionBundleRuntimeOwnerImportOrderingAcceptableFailures")
                && line.contains("production-owner-component-reuse-contract-missing")
                && containsStringField(line, "componentProductionOwnerReuseVersion",
                        "flow-ipc-component-owner-reuse-v1_16")
                && containsStringField(line, "componentProductionOwnerReusePolicy",
                        "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                && line.contains("componentProductionOwnerReuseAcceptableFailures")
                && line.contains("owner-import-failed-before-component-import")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerOrderingV116ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                        "flow-ipc-production-owner-resource-loader-v1_18")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceGraphMode",
                        "owner-root-and-resolved-dependencies")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceBasePolicy",
                        "stable-owner-resource-path-base-v1_18")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderNamespace",
                        "production-owner")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderRootArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                        "explicit-classified-only")) {
            summary.productionOwnerRuntime.resourceSchemeV118ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerSideEffectVersion",
                        "flow-ipc-production-owner-side-effect-v1_19")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectPolicy",
                        "custom-scheme-owner-import-side-effect-contract-v1_19")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                        "vaadin-text-field,vaadin-checkbox,vaadin-select")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                && containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                        "flow-ipc-component-owner-reuse-decision-v1_19")
                && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                        "production-owner-custom-scheme-side-effect-v1_19")) {
            summary.productionOwnerRuntime.sideEffectV119ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeVersion",
                        "flow-ipc-production-owner-owned-runtime-v1_20")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePolicy",
                        "owner-first-custom-scheme-runtime-reuse-v1_20")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePhase",
                        "before-component-imports")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                        "vaadin-text-field,vaadin-checkbox,vaadin-select")
                && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                        "flow-ipc-component-owner-reuse-policy-v1_20")
                && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                        "owner-first-then-focused-fallback-v1_20")) {
            summary.productionOwnerRuntime.ownedRuntimeV120ManifestObserved = true;
        }
        if (containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                        "flow-ipc-production-owner-positive-runtime-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureSource",
                        "repository-production-owner-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRootArtifact",
                        "META-INF/vatori/production-owner/owner-runtime-v1_23.js")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                        "META-INF/vatori/production-owner/owner-dependency-v1_23.js")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                        "vatori-production-owner-fixture")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                        "window.VaadinDesktopProductionOwnerV123")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                        "owner-first-custom-scheme-positive-fixture-v1_23")
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback",
                        "true")) {
            summary.productionOwnerRuntime.positiveFixtureV123ManifestObserved = true;
        }
    }
}
