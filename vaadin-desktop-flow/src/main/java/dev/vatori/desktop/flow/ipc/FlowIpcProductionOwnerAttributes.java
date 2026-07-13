package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcProductionOwnerAttributes {
    static final String FIXTURE_ROOT = "META-INF/vatori/production-owner/owner-runtime-v1_23.js";
    static final String FIXTURE_DEPENDENCY = "META-INF/vatori/production-owner/owner-dependency-v1_23.js";
    static final String RUNTIME_OWNER_BLOCKER = "flow-ipc-production-owner-v1_13";
    static final String RUNTIME_OWNER_IMPORT_VERSION = "flow-ipc-production-owner-import-v1_14";
    static final String RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION =
            "flow-ipc-production-owner-deps-v1_15";

    private static final String RUNTIME_OWNER_VERSION = "flow-ipc-production-owner-v1_13";
    private static final String RUNTIME_OWNER = "flow-ipc-production-bundle-runtime";
    private static final String RUNTIME_OWNER_SOURCE = "vaadin-dev-bundle-stats";
    private static final String RUNTIME_OWNER_SELECTED_BY =
            "stats-entrypoint-or-focused-production-entry";
    private static final String RUNTIME_OWNER_LIFECYCLE =
            "created,staged,import-skipped,side-effect-unobservable,not-reused,cleanup-owned";
    private static final String RUNTIME_OWNER_EXECUTION_POLICY =
            "stage-owner-contract-production-bundle-runtime-v1_13";
    private static final String RUNTIME_OWNER_FAILURE_LAYER =
            "production-owner-runtime-contract-missing";
    private static final String RUNTIME_OWNER_SIDE_EFFECT_PROOF =
            "owner-stage-resolved-import-intentionally-disabled-until-production-runtime-contract-can-own-indexhtml-side-effects";
    private static final String RUNTIME_OWNER_NEXT_STEP =
            "unify the production owner module identity with component focused dependency imports or move the owner to a custom-scheme/file-backed runtime before enabling indexhtml side effects";
    private static final String RUNTIME_OWNER_IMPORT_POLICY =
            "controlled-owner-dynamic-import-after-component-graph-v1_14";
    private static final String RUNTIME_OWNER_IMPORT_PHASE = "after-component-imports";
    private static final String RUNTIME_OWNER_IMPORT_PHASE_CANDIDATES =
            "isolated-preflight,before-component-imports,after-component-imports";
    private static final String RUNTIME_OWNER_IMPORT_DIAGNOSTIC_MODES =
            "isolated-preflight:available-not-enabled,before-component-imports:available-not-enabled,after-component-imports:default";
    private static final String RUNTIME_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-text-field|vaadin-checkbox|vaadin-select";
    private static final String RUNTIME_OWNER_IMPORT_ACCEPTABLE_FAILURES =
            "production-owner-relative-import-unresolved,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable";
    private static final String RUNTIME_OWNER_IMPORT_REWRITE_STRATEGY =
            "rewrite-relative-dev-bundle-dependencies-before-owner-import-v1_14";
    private static final String RUNTIME_OWNER_IMPORT_NEXT_STEP =
            "if owner import fails, move the production owner to a custom-scheme/file-backed resolver or tighten dependency rewrite/global collision guards around the real indexhtml artifact";
    private static final String RUNTIME_OWNER_DEPENDENCY_RESOLVER_POLICY =
            "owner-scoped-recursive-relative-import-graph-v1_15";
    private static final String RUNTIME_OWNER_DEPENDENCY_RESOLVER_MODULE_IDENTITY_NAMESPACE =
            "production-owner-deps-v1_15";
    private static final String RUNTIME_OWNER_DEPENDENCY_RESOLVER_ACCEPTABLE_FAILURES =
            "production-owner-missing-real-artifact,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable";
    private static final String RUNTIME_OWNER_DEPENDENCY_RESOLVER_NEXT_STEP =
            "run production owner import after the real dev-bundle relative dependency graph has been resolved, staged and classified";
    private static final String RUNTIME_OWNER_IMPORT_ORDERING_VERSION =
            "flow-ipc-production-owner-ordering-v1_16";
    private static final String RUNTIME_OWNER_IMPORT_PHASE_POLICY =
            "owner-first-before-component-imports-with-after-component-fallback-v1_16";
    private static final String RUNTIME_OWNER_IMPORT_ORDERING_ACCEPTABLE_FAILURES =
            "production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable,production-owner-browser-import-failed,production-owner-component-reuse-contract-missing,production-owner-unsupported-vaadin-production-owner-contract";
    private static final String COMPONENT_OWNER_REUSE_VERSION =
            "flow-ipc-component-owner-reuse-v1_16";
    private static final String COMPONENT_OWNER_REUSE_POLICY =
            "reuse-owner-owned-custom-elements-before-focused-imports-v1_16";
    private static final String COMPONENT_OWNER_REUSE_ACCEPTABLE_FAILURES =
            "owner-import-failed-before-component-import,owner-import-succeeded-but-component-class-export-unreachable,owner-side-effects-global-but-focused-entry-still-required,vaadin-production-artifact-does-not-expose-reusable-owner-contract,blob-module-identity-cannot-safely-merge-owner-and-component-lifetime";
    private static final String RUNTIME_OWNER_RESOURCE_LOADER_VERSION =
            "flow-ipc-production-owner-resource-loader-v1_18";
    private static final String RUNTIME_OWNER_RESOURCE_LOADER_MODE =
            "custom-scheme-file-backed";
    private static final String RUNTIME_OWNER_RESOURCE_SCHEME = "vatori-resource";
    private static final String RUNTIME_OWNER_RESOURCE_GRAPH_MODE =
            "owner-root-and-resolved-dependencies";
    private static final String RUNTIME_OWNER_RESOURCE_BASE_POLICY =
            "stable-owner-resource-path-base-v1_18";
    private static final String RUNTIME_OWNER_DEPENDENCY_BLOB_FALLBACK_POLICY =
            "explicit-classified-only";
    private static final String RUNTIME_OWNER_RESOURCE_LOADER_ACCEPTABLE_FAILURES =
            "production-owner-custom-scheme-missing-real-artifact,production-owner-custom-scheme-duplicate-custom-element,production-owner-custom-scheme-runtime-side-effect-blocked,production-owner-custom-scheme-browser-import-failed";
    private static final String RUNTIME_OWNER_RESOURCE_LOADER_NAMESPACE =
            "production-owner";
    private static final String RUNTIME_OWNER_SIDE_EFFECT_VERSION =
            "flow-ipc-production-owner-side-effect-v1_19";
    private static final String RUNTIME_OWNER_SIDE_EFFECT_POLICY =
            "custom-scheme-owner-import-side-effect-contract-v1_19";
    private static final String RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS =
            "vaadin-text-field,vaadin-checkbox,vaadin-select";
    private static final String RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand";
    private static final String RUNTIME_OWNER_SIDE_EFFECT_ACCEPTABLE_FAILURES =
            "production-owner-custom-scheme-runtime-side-effect-blocked,production-owner-custom-scheme-duplicate-custom-element,production-owner-custom-scheme-browser-import-failed,production-owner-custom-scheme-missing-real-artifact,production-owner-custom-scheme-unsupported-vaadin-contract";
    private static final String COMPONENT_OWNER_REUSE_DECISION_VERSION =
            "flow-ipc-component-owner-reuse-decision-v1_19";
    private static final String COMPONENT_OWNER_REUSE_DECISION_SOURCE =
            "production-owner-custom-scheme-side-effect-v1_19";
    private static final String RUNTIME_OWNER_OWNED_RUNTIME_VERSION =
            "flow-ipc-production-owner-owned-runtime-v1_20";
    private static final String RUNTIME_OWNER_OWNED_RUNTIME_POLICY =
            "owner-first-custom-scheme-runtime-reuse-v1_20";
    private static final String RUNTIME_OWNER_OWNED_RUNTIME_PHASE =
            "before-component-imports";
    private static final String RUNTIME_OWNER_OWNED_RUNTIME_ACCEPTABLE_FAILURES =
            "production-owner-owned-side-effect-unobservable,production-owner-owned-duplicate-custom-element,production-owner-owned-missing-real-artifact,production-owner-owned-browser-import-failed,production-owner-owned-unsupported-vaadin-contract";
    private static final String COMPONENT_OWNER_REUSE_POLICY_VERSION_V1_20 =
            "flow-ipc-component-owner-reuse-policy-v1_20";
    private static final String COMPONENT_OWNER_REUSE_POLICY_V1_20 =
            "owner-first-then-focused-fallback-v1_20";
    private static final String POSITIVE_FIXTURE_VERSION =
            "flow-ipc-production-owner-positive-runtime-v1_23";
    private static final String POSITIVE_FIXTURE_SOURCE =
            "repository-production-owner-positive-fixture-v1_23";
    private static final String POSITIVE_FIXTURE_EXPECTED_TAGS =
            "vatori-production-owner-fixture";
    private static final String POSITIVE_FIXTURE_EXPECTED_GLOBALS =
            "window.VaadinDesktopProductionOwnerV123";
    private static final String POSITIVE_FIXTURE_EXECUTION_POLICY =
            "owner-first-custom-scheme-positive-fixture-v1_23";

    private FlowIpcProductionOwnerAttributes() {
    }

    static Map<String, Object> ownerAttributes(Context context) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("productionBundleRuntimeOwnerVersion", RUNTIME_OWNER_VERSION);
        attributes.put("productionBundleRuntimeOwner", RUNTIME_OWNER);
        attributes.put("productionBundleRuntimeEntryArtifact", context.entryArtifact());
        attributes.put("productionBundleRuntimeDependencyChain", context.dependencyChain());
        attributes.put("productionBundleRuntimeSource", RUNTIME_OWNER_SOURCE);
        attributes.put("productionBundleRuntimeSelectedBy", RUNTIME_OWNER_SELECTED_BY);
        attributes.put("productionBundleRuntimeOwnerModuleIdentity",
                "production-owner-v1_13:" + context.entryArtifact());
        attributes.put("productionBundleRuntimeOwnerLifecycle", RUNTIME_OWNER_LIFECYCLE);
        attributes.put("productionBundleRuntimeOwnerExecutionPolicy", RUNTIME_OWNER_EXECUTION_POLICY);
        attributes.put("productionBundleRuntimeOwnerStageUrlPolicy",
                "temporary-blob-module-url-owner-namespace-v1_13");
        attributes.put("productionBundleRuntimeOwnerCachePolicy",
                "retain-per-owner-module-identity");
        attributes.put("productionBundleRuntimeOwnerCleanupStrategy",
                "cleanup-owned-window-lifetime-module-identity");
        attributes.put("productionBundleRuntimeOwnerBlockerVersion", RUNTIME_OWNER_BLOCKER);
        attributes.put("productionBundleRuntimeOwnerFailureLayer", RUNTIME_OWNER_FAILURE_LAYER);
        attributes.put("productionBundleRuntimeOwnerSideEffectProof", RUNTIME_OWNER_SIDE_EFFECT_PROOF);
        attributes.put("productionBundleRuntimeOwnerBrowserError", "");
        attributes.put("productionBundleRuntimeOwnerNextResolutionStep", RUNTIME_OWNER_NEXT_STEP);
        attributes.put("productionBundleRuntimeOwnerExpectedSideEffect",
                "own production indexhtml custom element and Flow global side effects before component imports reuse them");
        attributes.put("productionBundleRuntimeOwnerCustomElementTags",
                "vaadin-text-field,vaadin-checkbox,vaadin-select");
        attributes.put("productionBundleRuntimeOwnerImportVersion", RUNTIME_OWNER_IMPORT_VERSION);
        attributes.put("productionBundleRuntimeOwnerImportPolicy", RUNTIME_OWNER_IMPORT_POLICY);
        attributes.put("productionBundleRuntimeOwnerImportArtifact", context.entryArtifact());
        attributes.put("productionBundleRuntimeOwnerImportPhase", RUNTIME_OWNER_IMPORT_PHASE);
        attributes.put("productionBundleRuntimeOwnerImportPhaseCandidates",
                RUNTIME_OWNER_IMPORT_PHASE_CANDIDATES);
        attributes.put("productionBundleRuntimeOwnerImportDiagnosticModes",
                RUNTIME_OWNER_IMPORT_DIAGNOSTIC_MODES);
        attributes.put("productionBundleRuntimeOwnerImportExpectedSideEffects",
                RUNTIME_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS);
        attributes.put("productionBundleRuntimeOwnerImportAcceptableFailures",
                RUNTIME_OWNER_IMPORT_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerImportRewriteStrategy",
                RUNTIME_OWNER_IMPORT_REWRITE_STRATEGY);
        attributes.put("productionBundleRuntimeOwnerImportSourceArtifactFamily", "production-bundle");
        attributes.put("productionBundleRuntimeOwnerImportDependencyPaths",
                context.entryArtifact() + "," + context.commonJsArtifact());
        putDependencyResolverContractAttributes(attributes, context);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverNextResolutionStep",
                RUNTIME_OWNER_DEPENDENCY_RESOLVER_NEXT_STEP);
        attributes.put("productionBundleRuntimeOwnerImportOrderingVersion",
                RUNTIME_OWNER_IMPORT_ORDERING_VERSION);
        attributes.put("productionBundleRuntimeOwnerImportPhasePolicy",
                RUNTIME_OWNER_IMPORT_PHASE_POLICY);
        attributes.put("productionBundleRuntimeOwnerImportOrderingAcceptableFailures",
                RUNTIME_OWNER_IMPORT_ORDERING_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerImportOrderingResult",
                "not-run-until-host-selects-owner-first-or-after-component-phase");
        attributes.put("productionBundleRuntimeOwnerImportOrderingObservedPhases", "");
        attributes.put("productionBundleRuntimeOwnerDependencyGraphClosed", false);
        attributes.put("productionBundleRuntimeOwnerImportStageCount", 0);
        attributes.put("productionBundleRuntimeOwnerImportMissingDependencyPaths", "");
        attributes.put("productionBundleRuntimeOwnerImportAttempted", false);
        attributes.put("productionBundleRuntimeOwnerImported", false);
        attributes.put("productionBundleRuntimeOwnerImportNextResolutionStep",
                RUNTIME_OWNER_IMPORT_NEXT_STEP);
        attributes.put("componentProductionOwnerReuseVersion", COMPONENT_OWNER_REUSE_VERSION);
        attributes.put("componentProductionOwnerReusePolicy", COMPONENT_OWNER_REUSE_POLICY);
        attributes.put("componentProductionOwnerReuseAcceptableFailures",
                COMPONENT_OWNER_REUSE_ACCEPTABLE_FAILURES);
        attributes.put("componentProductionOwnerReuseMode",
                "decision-required-after-owner-import-v1_16");
        attributes.put("componentProductionOwnerReused", false);
        attributes.put("componentProductionOwnerReuseReason",
                "owner import ordering and component reuse decision are evaluated by the v1.16 host runtime");
        attributes.put("componentProductionOwnerOwnedTags", "");
        attributes.put("componentProductionOwnerSkippedDefinitions", "");
        attributes.put("componentProductionOwnerFallbackImports",
                context.textFieldEntryArtifact() + "," + context.checkboxEntryArtifact() + ","
                        + context.selectEntryArtifact());
        attributes.put("componentProductionOwnerDependencyPaths", context.entryArtifact());
        attributes.put("componentProductionOwnerImportOrder",
                "owner-first-or-component-focused-fallback-v1_16");
        putResourceLoaderAttributes(attributes, context, context.entryArtifact(), true);
        putSideEffectAttributes(attributes);
        return attributes;
    }

    static Map<String, Object> dependencyAttributes(String path, Context context) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "vaadin-production-owner-dependency-resolver-v1_15");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "production-bundle");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin", "vaadin-dev-bundle-production-owner-dependency");
        attributes.put("dependencyOf", context.entryArtifact());
        attributes.put("executionPolicy", "owner-dependency-module-v1_15");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "production-owner-dependency-resolver-v1_15");
        attributes.put("failureClassification", "production-owner-dependency-resolved-existing-artifact");
        attributes.put("blockedReason",
                "real Vaadin dev-bundle production artifact exposed for owner-scoped dependency graph closure; missing paths are classified at runtime instead of being faked");
        attributes.put("lookupKind", "Production-owner-dependency-resource-v1_15");
        attributes.put("devBundleResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("productionOwnerDependencyResource", true);
        putDependencyResolverAttributes(attributes, context, path);
        putResourceLoaderAttributes(attributes, context, path, false);
        putSideEffectAttributes(attributes);
        return attributes;
    }

    static Map<String, Object> positiveFixtureManifestAttributes() {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureVersion", POSITIVE_FIXTURE_VERSION);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureSource", POSITIVE_FIXTURE_SOURCE);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureRootArtifact", FIXTURE_ROOT);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureDependencyArtifact", FIXTURE_DEPENDENCY);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureExpectedTags",
                POSITIVE_FIXTURE_EXPECTED_TAGS);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureExpectedGlobals",
                POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureExecutionPolicy",
                POSITIVE_FIXTURE_EXECUTION_POLICY);
        manifest.put("productionBundleRuntimeOwnerPositiveFixtureNoBlobFallback", true);
        return manifest;
    }

    static Map<String, Object> positiveFixtureAttributes(boolean rootArtifact) {
        String path = rootArtifact ? FIXTURE_ROOT : FIXTURE_DEPENDENCY;
        String dependencyPath = rootArtifact ? "production-owner-positive-fixture" : FIXTURE_ROOT;
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("productionOwnerPositiveFixture", true);
        attributes.put("productionBundleRuntimeOwnerPositiveFixture", true);
        attributes.put("productionBundleRuntimeOwnerPositiveFixtureRole",
                rootArtifact ? "root" : "dependency");
        attributes.putAll(positiveFixtureManifestAttributes());
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "repository-production-owner-positive-fixture-v1_23");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "production-bundle");
        attributes.put("origin", "repository-production-owner-fixture");
        attributes.put("resourceOrigin", rootArtifact
                ? "repository-production-owner-positive-fixture-root"
                : "repository-production-owner-positive-fixture-dependency");
        attributes.put("dependencyOf", dependencyPath);
        attributes.put("executionPolicy", rootArtifact
                ? POSITIVE_FIXTURE_EXECUTION_POLICY
                : "owner-positive-fixture-dependency-v1_23");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_23");
        attributes.put("failureClassification", rootArtifact
                ? "production-owner-positive-fixture-root-v1_23"
                : "production-owner-positive-fixture-dependency-v1_23");
        attributes.put("blockedReason",
                "Repository production owner fixture proves the custom-scheme owner runtime positive path without claiming real Vaadin production bundle parity");
        attributes.put("lookupKind", rootArtifact
                ? "Production-owner-positive-fixture-root-v1_23"
                : "Production-owner-positive-fixture-dependency-v1_23");
        putPositiveFixtureOwnerAttributes(attributes, rootArtifact, path);
        if (!rootArtifact) {
            byte[] dependencyBody = FlowIpcClasspathResources.bytes(FIXTURE_DEPENDENCY);
            attributes.put("productionBundleRuntimeOwnerPositiveFixtureDependencyBytes",
                    dependencyBody.length);
            attributes.put("productionBundleRuntimeOwnerPositiveFixtureDependencySha256",
                    FlowIpcDiagnostics.sha256(dependencyBody));
        }
        return attributes;
    }

    static void putDependencyResolverAttributes(
            Map<String, Object> attributes, Context context, String path) {
        putDependencyResolverContractAttributes(attributes, context);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverRootArtifact",
                context.entryArtifact());
        attributes.put("productionBundleRuntimeOwnerDependencyResolverArtifact", path);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverClassification",
                "resolved-existing-artifact");
    }

    private static void putDependencyResolverContractAttributes(
            Map<String, Object> attributes, Context context) {
        attributes.put("productionBundleRuntimeOwnerDependencyResolverVersion",
                RUNTIME_OWNER_DEPENDENCY_RESOLVER_VERSION);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverRoot", context.buildRoot());
        attributes.put("productionBundleRuntimeOwnerDependencyResolverPolicy",
                RUNTIME_OWNER_DEPENDENCY_RESOLVER_POLICY);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts",
                String.join(",", context.dependencyArtifacts()));
        attributes.put("productionBundleRuntimeOwnerDependencyResolverAcceptableFailures",
                RUNTIME_OWNER_DEPENDENCY_RESOLVER_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace",
                RUNTIME_OWNER_DEPENDENCY_RESOLVER_MODULE_IDENTITY_NAMESPACE);
    }

    private static void putResourceLoaderAttributes(
            Map<String, Object> attributes, Context context, String path, boolean rootArtifact) {
        attributes.put("productionBundleRuntimeOwnerResourceLoaderVersion",
                RUNTIME_OWNER_RESOURCE_LOADER_VERSION);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderMode",
                RUNTIME_OWNER_RESOURCE_LOADER_MODE);
        attributes.put("productionBundleRuntimeOwnerResourceScheme", RUNTIME_OWNER_RESOURCE_SCHEME);
        attributes.put("productionBundleRuntimeOwnerResourceGraphMode",
                RUNTIME_OWNER_RESOURCE_GRAPH_MODE);
        attributes.put("productionBundleRuntimeOwnerResourceBasePolicy",
                RUNTIME_OWNER_RESOURCE_BASE_POLICY);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNamespace",
                RUNTIME_OWNER_RESOURCE_LOADER_NAMESPACE);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRootArtifact",
                context.entryArtifact());
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifact", path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                rootArtifact ? "root" : "dependency");
        attributes.put("productionBundleRuntimeOwnerBlobFallbackAllowed", false);
        attributes.put("productionBundleRuntimeOwnerDependencyBlobFallbackPolicy",
                RUNTIME_OWNER_DEPENDENCY_BLOB_FALLBACK_POLICY);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderAcceptableFailures",
                RUNTIME_OWNER_RESOURCE_LOADER_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderExpectedArtifacts",
                context.entryArtifact() + "," + String.join(",", context.dependencyArtifacts()));
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNoLocalhost", true);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRelativeImportBase",
                rootArtifact ? context.entryArtifact() : path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderStageUrlPolicy",
                "host-owned-custom-scheme-stable-source-path-v1_18");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderCleanupPolicy",
                "cleanup-owned-window-lifetime-file-backed-production-owner-v1_18");
    }

    private static void putSideEffectAttributes(Map<String, Object> attributes) {
        attributes.put("productionBundleRuntimeOwnerSideEffectVersion",
                RUNTIME_OWNER_SIDE_EFFECT_VERSION);
        attributes.put("productionBundleRuntimeOwnerSideEffectPolicy",
                RUNTIME_OWNER_SIDE_EFFECT_POLICY);
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedTags",
                RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerSideEffectAcceptableFailures",
                RUNTIME_OWNER_SIDE_EFFECT_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerSideEffectNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerModuleImported", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectFailureLayer",
                "pending-custom-scheme-owner-import-side-effect-check-v1_19");
        attributes.put("componentProductionOwnerReuseDecisionVersion",
                COMPONENT_OWNER_REUSE_DECISION_VERSION);
        attributes.put("componentProductionOwnerReuseDecisionSource",
                COMPONENT_OWNER_REUSE_DECISION_SOURCE);
        attributes.put("componentProductionOwnerReuseDecision",
                "pending-runtime-side-effect-check");
        attributes.put("componentProductionOwnerReuseDecisionReason",
                "production owner custom-scheme import side effects must be checked before focused component resolver can reuse owner-owned tags");
        attributes.put("componentProductionOwnerReuseDecisionTags",
                RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        putOwnedRuntimeAttributes(attributes);
    }

    private static void putOwnedRuntimeAttributes(Map<String, Object> attributes) {
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeVersion",
                RUNTIME_OWNER_OWNED_RUNTIME_VERSION);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePolicy",
                RUNTIME_OWNER_OWNED_RUNTIME_POLICY);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePhase",
                RUNTIME_OWNER_OWNED_RUNTIME_PHASE);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals",
                RUNTIME_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeAcceptableFailures",
                RUNTIME_OWNER_OWNED_RUNTIME_ACCEPTABLE_FAILURES);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeAttempted", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                "pending-owner-first-custom-scheme-runtime-reuse-v1_20");
        attributes.put("componentProductionOwnerReusePolicyVersionV1_20",
                COMPONENT_OWNER_REUSE_POLICY_VERSION_V1_20);
        attributes.put("componentProductionOwnerReusePolicyV1_20",
                COMPONENT_OWNER_REUSE_POLICY_V1_20);
        attributes.put("componentProductionOwnerReuseDecisionPhase",
                RUNTIME_OWNER_OWNED_RUNTIME_PHASE);
    }

    private static void putPositiveFixtureOwnerAttributes(
            Map<String, Object> attributes, boolean rootArtifact, String path) {
        attributes.put("productionBundleRuntimeOwnerVersion", RUNTIME_OWNER_VERSION);
        attributes.put("productionBundleRuntimeOwner", RUNTIME_OWNER);
        attributes.put("productionBundleRuntimeEntryArtifact", FIXTURE_ROOT);
        attributes.put("productionBundleRuntimeOwnerImportArtifact", FIXTURE_ROOT);
        attributes.put("productionBundleRuntimeOwnerImportVersion", RUNTIME_OWNER_IMPORT_VERSION);
        attributes.put("productionBundleRuntimeOwnerImportPolicy",
                "owner-first-positive-fixture-import-v1_23");
        attributes.put("productionBundleRuntimeOwnerImportPhase", "before-component-imports");
        attributes.put("productionBundleRuntimeOwnerImportPhaseCandidates",
                RUNTIME_OWNER_IMPORT_PHASE_CANDIDATES);
        attributes.put("productionBundleRuntimeOwnerImportExpectedSideEffects",
                POSITIVE_FIXTURE_EXPECTED_GLOBALS + ",customElements:" + POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerImportAcceptableFailures",
                "production-owner-positive-fixture-browser-import-failed,production-owner-positive-fixture-side-effect-missing");
        attributes.put("productionBundleRuntimeOwnerImportRewriteStrategy",
                "custom-scheme-owner-positive-fixture-relative-import-rewrite-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderVersion",
                RUNTIME_OWNER_RESOURCE_LOADER_VERSION);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderMode",
                RUNTIME_OWNER_RESOURCE_LOADER_MODE);
        attributes.put("productionBundleRuntimeOwnerResourceScheme", RUNTIME_OWNER_RESOURCE_SCHEME);
        attributes.put("productionBundleRuntimeOwnerResourceGraphMode",
                "positive-fixture-root-and-dependency-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceBasePolicy",
                RUNTIME_OWNER_RESOURCE_BASE_POLICY);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNamespace",
                RUNTIME_OWNER_RESOURCE_LOADER_NAMESPACE);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRootArtifact", FIXTURE_ROOT);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifact", path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderArtifactRole",
                rootArtifact ? "positive-fixture-root" : "positive-fixture-dependency");
        attributes.put("productionBundleRuntimeOwnerBlobFallbackAllowed", false);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderNoLocalhost", true);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderRelativeImportBase", path);
        attributes.put("productionBundleRuntimeOwnerResourceLoaderStageUrlPolicy",
                "host-owned-custom-scheme-stable-source-path-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderCleanupPolicy",
                "cleanup-owned-window-lifetime-file-backed-production-owner-v1_23");
        attributes.put("productionBundleRuntimeOwnerResourceLoaderExpectedArtifacts",
                FIXTURE_ROOT + "," + FIXTURE_DEPENDENCY);
        attributes.put("productionBundleRuntimeOwnerSideEffectVersion",
                RUNTIME_OWNER_SIDE_EFFECT_VERSION);
        attributes.put("productionBundleRuntimeOwnerSideEffectPolicy",
                "positive-fixture-owner-side-effect-contract-v1_23");
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedTags",
                POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerSideEffectExpectedGlobals",
                POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerSideEffectAcceptableFailures",
                "production-owner-positive-fixture-side-effect-missing,production-owner-positive-fixture-browser-import-failed");
        attributes.put("productionBundleRuntimeOwnerSideEffectNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerModuleImported", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerSideEffectFailureLayer",
                "pending-production-owner-positive-fixture-import-v1_23");
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeVersion",
                RUNTIME_OWNER_OWNED_RUNTIME_VERSION);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePolicy",
                "owner-first-positive-fixture-runtime-reuse-v1_23");
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimePhase", "before-component-imports");
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedTags",
                POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeExpectedGlobals",
                POSITIVE_FIXTURE_EXPECTED_GLOBALS);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeNoBlobFallback", true);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeAttempted", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeSatisfied", false);
        attributes.put("productionBundleRuntimeOwnerOwnedRuntimeFailureLayer",
                "pending-production-owner-positive-fixture-runtime-v1_23");
        attributes.put("componentProductionOwnerReuseDecisionVersion",
                COMPONENT_OWNER_REUSE_DECISION_VERSION);
        attributes.put("componentProductionOwnerReuseDecisionSource",
                "production-owner-positive-fixture-v1_23");
        attributes.put("componentProductionOwnerReuseDecision",
                "pending-production-owner-positive-fixture");
        attributes.put("componentProductionOwnerReuseDecisionReason",
                "positive fixture must import through owner custom scheme before reuse can be counted");
        attributes.put("componentProductionOwnerReuseDecisionTags",
                POSITIVE_FIXTURE_EXPECTED_TAGS);
        attributes.put("componentProductionOwnerReusePolicyVersionV1_20",
                COMPONENT_OWNER_REUSE_POLICY_VERSION_V1_20);
        attributes.put("componentProductionOwnerReusePolicyV1_20",
                COMPONENT_OWNER_REUSE_POLICY_V1_20);
        attributes.put("componentProductionOwnerReuseDecisionPhase", "before-component-imports");
    }

    record Context(
            String buildRoot,
            String entryArtifact,
            String commonJsArtifact,
            String textFieldEntryArtifact,
            String checkboxEntryArtifact,
            String selectEntryArtifact,
            String dependencyChain,
            List<String> dependencyArtifacts) {
    }
}
