package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.ProductionOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class ProductionOwnerResourceExchangeTraceInspector {
    private static final String PRODUCTION_RUNTIME_ENTRY_ARTIFACT =
            "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js";
    private static final String POSITIVE_FIXTURE_ROOT_ARTIFACT =
            "META-INF/vatori/production-owner/owner-runtime-v1_23.js";
    private static final String POSITIVE_FIXTURE_DEPENDENCY_ARTIFACT =
            "META-INF/vatori/production-owner/owner-dependency-v1_23.js";

    private ProductionOwnerResourceExchangeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isWebComponentResourceProbeRequest(line)
                && containsProductionRuntimeArtifact(line)
                && containsStringField(line, "productionBundleRuntimeVersion",
                        "flow-ipc-production-runtime-v1_10")
                && containsStringField(line, "productionBundleRuntimeRegistrySource",
                        "vaadin-dev-bundle-stats-runtime-registry-v1_10")
                && containsStringField(line, "productionBundleRuntimeSelectedArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)) {
            summary.frontendBuildPipeline.productionRuntimeV110RequestObserved = true;
        }
        if (isWebComponentResourceProbeRequest(line)
                && containsProductionRuntimeArtifact(line)
                && containsStringField(line, "productionBundleRuntimeOwnerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsStringField(line, "productionBundleRuntimeOwner",
                        "flow-ipc-production-bundle-runtime")
                && containsStringField(line, "productionBundleRuntimeEntryArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsStringField(line, "productionBundleRuntimeOwnerModuleIdentity",
                        "production-owner-v1_13:vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js")
                && containsStringField(line, "productionBundleRuntimeOwnerExecutionPolicy",
                        "stage-owner-contract-production-bundle-runtime-v1_13")
                && containsStringField(line, "productionBundleRuntimeOwnerBlockerVersion",
                        "flow-ipc-production-owner-v1_13")
                && containsProductionOwnerFailureLayer(line, "productionBundleRuntimeOwnerFailureLayer")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerV113RequestObserved = true;
        }
        if (isWebComponentResourceProbeRequest(line)
                && containsProductionRuntimeArtifact(line)
                && containsStringField(line, "productionBundleRuntimeOwnerImportVersion",
                        "flow-ipc-production-owner-import-v1_14")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPolicy",
                        "controlled-owner-dynamic-import-after-component-graph-v1_14")
                && containsStringField(line, "productionBundleRuntimeOwnerImportArtifact",
                        PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsAll(line,
                        "productionBundleRuntimeOwnerImportExpectedSideEffects",
                        "productionBundleRuntimeOwnerImportAcceptableFailures")
                && containsFalseFields(line,
                        "productionBundleRuntimeOwnerImportAttempted",
                        "productionBundleRuntimeOwnerImported")) {
            summary.productionOwnerRuntime.ownerImportV114RequestObserved = true;
        }
        if (isProductionOwnerDependencyResourceRequest(line)
                && containsLiteralField(line, "productionOwnerDependencyResource", "true")
                && containsStringField(line, "lookupKind", "Production-owner-dependency-resource-v1_15")
                && containsStringField(line, "requestedBy", "production-owner-dependency-resolver-v1_15")
                && containsStringField(line, "executionPolicy", "owner-dependency-module-v1_15")
                && (containsPath(line,
                        "vaadin-dev-bundle/webapp/VAADIN/build/copilot-log-plugin-h3VQq9-2-BlujgixT.js")
                        || containsPath(line,
                                "vaadin-dev-bundle/webapp/VAADIN/build/FlowClient-RTzSA_ve.js"))
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverVersion",
                        "flow-ipc-production-owner-deps-v1_15")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverRoot",
                        "vaadin-dev-bundle/webapp/VAADIN/build/")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyResolverPolicy",
                        "owner-scoped-recursive-relative-import-graph-v1_15")) {
            summary.productionOwnerRuntime.ownerDependenciesV115RequestObserved = true;
        }
        if (isWebComponentResourceProbeRequest(line)
                && containsProductionRuntimeArtifact(line)
                && containsStringField(line, "productionBundleRuntimeOwnerImportOrderingVersion",
                        "flow-ipc-production-owner-ordering-v1_16")
                && containsStringField(line, "productionBundleRuntimeOwnerImportPhasePolicy",
                        "owner-first-before-component-imports-with-after-component-fallback-v1_16")
                && containsStringField(line, "componentProductionOwnerReuseVersion",
                        "flow-ipc-component-owner-reuse-v1_16")
                && containsStringField(line, "componentProductionOwnerReusePolicy",
                        "reuse-owner-owned-custom-elements-before-focused-imports-v1_16")
                && containsAll(line, "componentProductionOwnerReuseAcceptableFailures")
                && containsLiteralField(line, "componentProductionOwnerReused", "false")) {
            summary.productionOwnerRuntime.ownerOrderingV116RequestObserved = true;
        }
        if (isProductionOwnerDependencyResourceRequest(line)
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderVersion",
                        "flow-ipc-production-owner-resource-loader-v1_18")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceLoaderMode",
                        "custom-scheme-file-backed")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceGraphMode",
                        "owner-root-and-resolved-dependencies")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceBasePolicy",
                        "stable-owner-resource-path-base-v1_18")
                && containsLiteralField(line, "productionBundleRuntimeOwnerBlobFallbackAllowed", "false")
                && containsStringField(line, "productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                        "explicit-classified-only")) {
            summary.productionOwnerRuntime.resourceSchemeV118RequestObserved = true;
        }
        if (isProductionOwnerDependencyResourceRequest(line)
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectVersion",
                        "flow-ipc-production-owner-side-effect-v1_19")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectPolicy",
                        "custom-scheme-owner-import-side-effect-contract-v1_19")
                && containsLiteralField(line, "productionBundleRuntimeOwnerSideEffectNoBlobFallback", "true")
                && containsStringField(line, "componentProductionOwnerReuseDecisionVersion",
                        "flow-ipc-component-owner-reuse-decision-v1_19")
                && containsStringField(line, "componentProductionOwnerReuseDecisionSource",
                        "production-owner-custom-scheme-side-effect-v1_19")) {
            summary.productionOwnerRuntime.sideEffectV119RequestObserved = true;
        }
        if (isProductionOwnerDependencyResourceRequest(line)
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeVersion",
                        "flow-ipc-production-owner-owned-runtime-v1_20")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePolicy",
                        "owner-first-custom-scheme-runtime-reuse-v1_20")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimePhase",
                        "before-component-imports")
                && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", "true")
                && containsStringField(line, "componentProductionOwnerReusePolicyVersionV1_20",
                        "flow-ipc-component-owner-reuse-policy-v1_20")
                && containsStringField(line, "componentProductionOwnerReusePolicyV1_20",
                        "owner-first-then-focused-fallback-v1_20")) {
            summary.productionOwnerRuntime.ownedRuntimeV120RequestObserved = true;
        }
        if (isResourceRequest(line)
                && containsPath(line, POSITIVE_FIXTURE_ROOT_ARTIFACT)
                && containsStringField(line, "lookupKind", "Production-owner-positive-fixture-root-v1_23")
                && containsStringField(line, "resolver", "repository-production-owner-positive-fixture-v1_23")
                && containsStringField(line, "executionPolicy",
                        "owner-first-custom-scheme-positive-fixture-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                        "vatori-production-owner-fixture")
                && containsStringField(line, "productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals",
                        "window.VaadinDesktopProductionOwnerV123")
                && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback",
                        "true")) {
            summary.productionOwnerRuntime.positiveFixtureV123RootRequestObserved = true;
        }
        if (isResourceRequest(line)
                && containsPath(line, POSITIVE_FIXTURE_DEPENDENCY_ARTIFACT)
                && containsStringField(line, "lookupKind",
                        "Production-owner-positive-fixture-dependency-v1_23")
                && containsStringField(line, "resolver", "repository-production-owner-positive-fixture-v1_23")
                && containsStringField(line, "executionPolicy",
                        "owner-positive-fixture-dependency-v1_23")
                && containsStringField(line, "dependencyOf",
                        POSITIVE_FIXTURE_ROOT_ARTIFACT)
                && containsStringField(line, "productionBundleRuntimeOwnerResourceScheme",
                        "vatori-resource")
                && containsStringField(line, "productionBundleRuntimeOwnerSideEffectExpectedTags",
                        "vatori-production-owner-fixture")
                && containsLiteralField(line, "productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback",
                        "true")) {
            summary.productionOwnerRuntime.positiveFixtureV123DependencyRequestObserved = true;
        }
        if (isRedactedResourceResponse(line)
                && containsPath(line, POSITIVE_FIXTURE_ROOT_ARTIFACT)
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "root")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                        "flow-ipc-production-owner-positive-runtime-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                        POSITIVE_FIXTURE_DEPENDENCY_ARTIFACT)
                && containsAll(line,
                        "\"sha256\":\"")) {
            summary.productionOwnerRuntime.positiveFixtureV123RootResponseObserved = true;
        }
        if (isRedactedResourceResponse(line)
                && containsPath(line, POSITIVE_FIXTURE_DEPENDENCY_ARTIFACT)
                && containsLiteralField(line, "productionBundleRuntimeOwnerPositiveFixture", "true")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureRole", "dependency")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureVersion",
                        "flow-ipc-production-owner-positive-runtime-v1_23")
                && containsStringField(line, "productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact",
                        POSITIVE_FIXTURE_DEPENDENCY_ARTIFACT)
                && containsAll(line,
                        "productionBundleRuntimeOwnerPositiveFixtureDependencySha256",
                        "\"sha256\":\"")) {
            summary.productionOwnerRuntime.positiveFixtureV123DependencyResponseObserved = true;
        }
    }

    private static boolean isWebComponentResourceProbeRequest(String line) {
        return isResourceRequest(line)
                && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"");
    }

    private static boolean isProductionOwnerDependencyResourceRequest(String line) {
        return isResourceRequest(line)
                && (line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                        || line.contains("\"purpose\":\"production-owner-dependency-v1_15\""));
    }

    private static boolean containsProductionRuntimeArtifact(String line) {
        return containsPath(line, PRODUCTION_RUNTIME_ENTRY_ARTIFACT)
                && containsLiteralField(line, "productionBundleRuntimeArtifact", "true");
    }
}
