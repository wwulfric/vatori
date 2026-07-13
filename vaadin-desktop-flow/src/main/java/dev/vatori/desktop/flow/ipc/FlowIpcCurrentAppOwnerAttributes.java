package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcCurrentAppOwnerAttributes {
    static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SOURCE =
            "current-app-generated-imports-select-owner-coverage-v1_41";
    static final String GENERATED_LAZY_SPI_DECISION =
            "packaging-time-build-frontend-staging";

    private static final String PRODUCTION_OWNER_IMPORT_VERSION =
            "flow-ipc-current-app-production-owner-import-v1_36";
    private static final String PRODUCTION_OWNER_IMPORT_SOURCE =
            "current-app-production-runtime-ownership-handoff-v1_35";
    private static final String PRODUCTION_OWNER_IMPORT_POLICY =
            "owner-first-current-app-production-entry-import-v1_36";
    private static final String PRODUCTION_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-vertical-layout|vaadin-text-field|vaadin-checkbox|vaadin-select";
    private static final String PRODUCTION_OWNER_IMPORT_FAILURE_LAYER =
            "current-app-production-owner-import-side-effect-pending";
    private static final String PRODUCTION_OWNER_IMPORT_NEXT_STEP =
            "make the current app production owner import expose reusable Flow/component side effects, or switch this handoff to a supported Vaadin build/runtime SPI";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_VERSION =
            "flow-ipc-current-app-production-owner-component-reuse-v1_37";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_SOURCE =
            "current-app-production-owner-import-side-effect-v1_36";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_POLICY =
            "reuse-current-app-owner-defined-custom-elements-before-focused-import-v1_37";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_EXPECTED_TAGS = "vaadin-text-field";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_FAILURE_LAYER =
            "current-app-production-owner-component-reuse-pending";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_NEXT_STEP =
            "extend current app owner reuse to earlier component paths and broader Vaadin component/resource coverage, then cover dev-mode/Vite parity";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_VERSION =
            "flow-ipc-current-app-owner-component-reuse-matrix-v1_38";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_SOURCE =
            "current-app-production-owner-component-reuse-v1_37";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_POLICY =
            "classify-current-app-owner-reuse-or-focused-fallback-for-covered-components-v1_38";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_EXPECTED_TAGS =
            "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_FAILURE_LAYER =
            "current-app-owner-component-reuse-matrix-pending";
    private static final String PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_NEXT_STEP =
            "move current app owner import earlier than covered component focused imports, or switch to a supported Vaadin build/runtime SPI before broadening component parity";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_VERSION =
            "flow-ipc-current-app-production-owner-early-import-v1_39";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_SOURCE =
            "current-app-owner-component-reuse-matrix-v1_38";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_POLICY =
            "request-current-app-production-owner-before-covered-component-imports-v1_39";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_PHASE = "before-component-imports";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_REUSABLE_TAGS =
            "vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_FAILURE_LAYER =
            "current-app-production-owner-early-import-pending";
    private static final String PRODUCTION_OWNER_EARLY_IMPORT_NEXT_STEP =
            "move remaining owner-ready focused fallback tags to a supported Vaadin build/runtime SPI or broaden current app owner tag coverage";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_VERSION =
            "flow-ipc-current-app-generated-imports-select-owner-coverage-v1_41";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_SOURCE =
            "current-app-generated-imports-owner-side-effect-v1_40";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_POLICY =
            "cover-current-app-production-generated-imports-select-before-components-v1_41";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_CANDIDATE =
            "current-app-production-generated-imports-support-artifact";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_TAGS =
            "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_COVERED_TAGS =
            "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_KNOWN_MISSING_TAGS = "";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS =
            "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_FAILURE_LAYER =
            "current-app-generated-imports-select-owner-coverage-pending";
    private static final String GENERATED_IMPORTS_OWNER_SIDE_EFFECT_NEXT_STEP =
            "promote four-tag current app generated imports side effects into the owner runtime contract, then broaden component/resource coverage and dev-mode/Vite parity";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_VERSION =
            "flow-ipc-current-app-generated-imports-owner-runtime-contract-v1_42";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_ARTIFACT_SOURCE =
            "current-app-production-build-frontend-generated-imports-support-artifact";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_OWNER_PRIORITY =
            "before-covered-focused-component-imports";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SKIP_POLICY =
            "skip-covered-focused-imports-when-owner-side-effect-satisfied";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FALLBACK_POLICY =
            "strict-blocker-with-focused-fallback-diagnostics";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_RESOLVER_HANDOFF =
            "component-resolver-owned-input";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FAILURE_LAYER =
            "current-app-generated-imports-owner-runtime-contract-pending";
    private static final String GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_NEXT_STEP =
            "connect this owner contract to real generated lazy chunk SPI, dev-mode/Vite parity, and broader component/resource coverage";
    private static final String GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_VERSION =
            "flow-ipc-current-app-generated-imports-owner-lazy-chunk-bridge-v1_42";
    private static final String GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_SOURCE =
            "current-app-build-frontend-output-v1_32";
    private static final String GENERATED_IMPORTS_OWNER_LAZY_CHUNK_SPI_DECISION =
            "packaging-time-build-frontend-staging";
    private static final String GENERATED_LAZY_SPI_DECISION_VERSION =
            "flow-ipc-generated-lazy-spi-decision-v1_43";
    private static final String GENERATED_LAZY_SPI_DECISION_SOURCE =
            "current-app-generated-imports-owner-runtime-contract-v1_42";
    private static final String GENERATED_LAZY_SPI_AUDITED_SOURCE_SURFACES =
            "AbstractUpdateImports,FrontendDependencies,ChunkInfo,build-frontend-generated-output,production-stats-json,prod-bundle";
    private static final String GENERATED_LAZY_SPI_CHUNK_INFO_API_STATUS =
            "internal-may-be-renamed-or-removed";
    private static final String GENERATED_LAZY_SPI_REGISTRY_SOURCE =
            "current-app-build-frontend-output-v1_32";
    private static final String GENERATED_LAZY_SPI_UNAVAILABLE_REASON =
            "vaadin-chunkinfo-internal-no-supported-runtime-spi";
    private static final String GENERATED_LAZY_SPI_FAILURE_LAYER =
            "supported-vaadin-lazy-chunk-spi-unavailable";
    private static final String GENERATED_LAZY_SPI_PRODUCT_PATH =
            "packaging-time-build-frontend-output-staging";
    private static final String GENERATED_LAZY_SPI_EXECUTION_EVIDENCE =
            "current-app-generated-chunk-loadondemand-executed-v1_33";
    private static final String GENERATED_LAZY_SPI_EXECUTION_TRIGGER =
            "host-decision-audit-probe-not-flow-client-spi";
    private static final String GENERATED_LAZY_SPI_NEXT_STEP =
            "productize packaging-time build-frontend artifacts as framework-managed staged frontend inventory, or upstream/support a stable Vaadin lazy chunk SPI";

    private FlowIpcCurrentAppOwnerAttributes() {
    }

    static Map<String, Object> attributes(Context context) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putProductionOwnerImport(attributes, context);
        putProductionOwnerReuse(attributes, context);
        putGeneratedImportsOwnerContract(attributes, context);
        putGeneratedLazySpiDecision(attributes, context);
        return attributes;
    }

    private static void putProductionOwnerImport(Map<String, Object> attributes, Context context) {
        attributes.put("currentAppProductionOwnerImportVersion", PRODUCTION_OWNER_IMPORT_VERSION);
        attributes.put("currentAppProductionOwnerImportSource", PRODUCTION_OWNER_IMPORT_SOURCE);
        attributes.put("currentAppProductionOwnerImportPolicy", PRODUCTION_OWNER_IMPORT_POLICY);
        attributes.put("currentAppProductionOwnerImportCandidate", context.productionRuntimeOwnershipCandidate());
        attributes.put("currentAppProductionOwnerImportArtifact", context.productionEntryScriptVirtualPath());
        attributes.put("currentAppProductionOwnerImportSupportArtifact", context.productionSupportArtifact());
        attributes.put("currentAppProductionOwnerImportDependencyPaths", context.productionBuildJsVirtualPaths());
        attributes.put("currentAppProductionOwnerImportExpectedSideEffects",
                PRODUCTION_OWNER_IMPORT_EXPECTED_SIDE_EFFECTS);
        attributes.put("currentAppProductionOwnerImportPhase", "after-current-app-generated-root-execution");
        attributes.put("currentAppProductionOwnerImportHandoffReady", context.handoffReady());
        attributes.put("currentAppProductionOwnerImportAttempted", false);
        attributes.put("currentAppProductionOwnerImported", false);
        attributes.put("currentAppProductionOwnerImportOverCustomScheme", false);
        attributes.put("currentAppProductionOwnerImportDependencyStageCount", 0);
        attributes.put("currentAppProductionOwnerImportSideEffectSatisfied", false);
        attributes.put("currentAppProductionOwnerImportReuseSatisfied", false);
        attributes.put("currentAppProductionOwnerImportNoLocalhost", true);
        attributes.put("currentAppProductionOwnerImportFixtureSubstituted", false);
        attributes.put("currentAppProductionOwnerImportFailureLayer",
                context.handoffReady() ? PRODUCTION_OWNER_IMPORT_FAILURE_LAYER : context.failureLayer());
        attributes.put("currentAppProductionOwnerImportNextResolutionStep", PRODUCTION_OWNER_IMPORT_NEXT_STEP);
    }

    private static void putProductionOwnerReuse(Map<String, Object> attributes, Context context) {
        attributes.put("currentAppProductionOwnerComponentReuseVersion",
                PRODUCTION_OWNER_COMPONENT_REUSE_VERSION);
        attributes.put("currentAppProductionOwnerComponentReuseSource",
                PRODUCTION_OWNER_COMPONENT_REUSE_SOURCE);
        attributes.put("currentAppProductionOwnerComponentReusePolicy",
                PRODUCTION_OWNER_COMPONENT_REUSE_POLICY);
        attributes.put("currentAppProductionOwnerComponentReuseExpectedTags",
                PRODUCTION_OWNER_COMPONENT_REUSE_EXPECTED_TAGS);
        attributes.put("currentAppProductionOwnerComponentReuseDecision",
                "pending-current-app-owner-reuse-check");
        attributes.put("currentAppProductionOwnerComponentReuseReason",
                "current app production owner import side effects must be checked before focused component import");
        attributes.put("currentAppProductionOwnerComponentReuseReusedTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseSkippedFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseFallbackFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseFailureLayer",
                context.handoffReady() ? PRODUCTION_OWNER_COMPONENT_REUSE_FAILURE_LAYER : context.failureLayer());
        attributes.put("currentAppProductionOwnerComponentReuseNextResolutionStep",
                PRODUCTION_OWNER_COMPONENT_REUSE_NEXT_STEP);
        attributes.put("currentAppProductionOwnerComponentReused", false);
        attributes.put("currentAppProductionOwnerComponentReuseNoLocalhost", true);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixVersion",
                PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_VERSION);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixSource",
                PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_SOURCE);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixPolicy",
                PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_POLICY);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixExpectedTags",
                PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_EXPECTED_TAGS);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixObservedTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixReusedTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixFallbackTags", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixSkippedFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixFallbackFocusedImports", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixOrderingBlockers", "");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixDecision",
                "pending-current-app-owner-component-reuse-matrix");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixReason",
                "covered components must be classified as current app owner reuse or focused fallback");
        attributes.put("currentAppProductionOwnerComponentReuseMatrixFailureLayer",
                context.handoffReady() ? PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_FAILURE_LAYER
                        : context.failureLayer());
        attributes.put("currentAppProductionOwnerComponentReuseMatrixNextResolutionStep",
                PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_NEXT_STEP);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixComplete", false);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixTextFieldReuseRetained", false);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixEarlyFallbackClassified", false);
        attributes.put("currentAppProductionOwnerComponentReuseMatrixNoLocalhost", true);
        attributes.put("currentAppProductionOwnerEarlyImportVersion", PRODUCTION_OWNER_EARLY_IMPORT_VERSION);
        attributes.put("currentAppProductionOwnerEarlyImportSource", PRODUCTION_OWNER_EARLY_IMPORT_SOURCE);
        attributes.put("currentAppProductionOwnerEarlyImportPolicy", PRODUCTION_OWNER_EARLY_IMPORT_POLICY);
        attributes.put("currentAppProductionOwnerEarlyImportPhase", PRODUCTION_OWNER_EARLY_IMPORT_PHASE);
        attributes.put("currentAppProductionOwnerEarlyImportReusableTags",
                PRODUCTION_OWNER_EARLY_IMPORT_REUSABLE_TAGS);
        attributes.put("currentAppProductionOwnerEarlyImportCoveredTags",
                PRODUCTION_OWNER_COMPONENT_REUSE_MATRIX_EXPECTED_TAGS);
        attributes.put("currentAppProductionOwnerEarlyImportReusedTags", "");
        attributes.put("currentAppProductionOwnerEarlyImportSkippedFocusedImports", "");
        attributes.put("currentAppProductionOwnerEarlyImportFallbackTags", "");
        attributes.put("currentAppProductionOwnerEarlyImportFallbackFocusedImports", "");
        attributes.put("currentAppProductionOwnerEarlyImportOrderingBlockers", "");
        attributes.put("currentAppProductionOwnerEarlyImportDecision",
                "pending-current-app-owner-early-import");
        attributes.put("currentAppProductionOwnerEarlyImportReason",
                "current app production owner must be imported before covered component focused imports");
        attributes.put("currentAppProductionOwnerEarlyImportFailureLayer",
                context.handoffReady() ? PRODUCTION_OWNER_EARLY_IMPORT_FAILURE_LAYER : context.failureLayer());
        attributes.put("currentAppProductionOwnerEarlyImportNextResolutionStep",
                PRODUCTION_OWNER_EARLY_IMPORT_NEXT_STEP);
        attributes.put("currentAppProductionOwnerEarlyImportRequestBeforeCoveredComponents", false);
        attributes.put("currentAppProductionOwnerEarlyImportAttempted", false);
        attributes.put("currentAppProductionOwnerEarlyImported", false);
        attributes.put("currentAppProductionOwnerEarlyImportSideEffectSatisfied", false);
        attributes.put("currentAppProductionOwnerEarlyImportPartialReuseSatisfied", false);
        attributes.put("currentAppProductionOwnerEarlyImportNoLocalhost", true);
        attributes.put("currentAppProductionOwnerEarlyImportFixtureSubstituted", false);
    }

    private static void putGeneratedImportsOwnerContract(Map<String, Object> attributes, Context context) {
        attributes.put("currentAppGeneratedImportsOwnerSideEffectVersion",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_VERSION);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectSource",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectPolicy",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_POLICY);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectCandidate",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_CANDIDATE);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectArtifact", context.productionSupportArtifact());
        attributes.put("currentAppGeneratedImportsOwnerSideEffectExpectedTags",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectCoveredTags",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_COVERED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectKnownMissingTags",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_KNOWN_MISSING_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectExpectedGlobals",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_GLOBALS);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectPhase",
                PRODUCTION_OWNER_EARLY_IMPORT_PHASE);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectObservedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectMissingTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectReusedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFallbackTags", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports", "");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectDecision",
                "pending-current-app-generated-imports-owner-side-effect");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectReason",
                "current app production generated imports support artifact must be imported before focused component imports");
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFailureLayer",
                context.handoffReady() ? GENERATED_IMPORTS_OWNER_SIDE_EFFECT_FAILURE_LAYER
                        : context.failureLayer());
        attributes.put("currentAppGeneratedImportsOwnerSideEffectNextResolutionStep",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_NEXT_STEP);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectAttempted", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectImported", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectPartialReuseSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectLoadOnDemandPresent", false);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectNoLocalhost", true);
        attributes.put("currentAppGeneratedImportsOwnerSideEffectFixtureSubstituted", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractVersion",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_VERSION);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSource",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractArtifactSource",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_ARTIFACT_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifact",
                context.productionSupportArtifact());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifactSha256",
                context.productionSupportSha256());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifactBytes",
                context.productionSupportBytes());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractExpectedTags",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_EXPECTED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractCoveredTags",
                GENERATED_IMPORTS_OWNER_SIDE_EFFECT_COVERED_TAGS);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractOwnerPriority",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_OWNER_PRIORITY);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSkipPolicy",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SKIP_POLICY);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFallbackPolicy",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FALLBACK_POLICY);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractResolverHandoff",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_RESOLVER_HANDOFF);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractDecision",
                "pending-current-app-generated-imports-owner-runtime-contract");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFailureLayer",
                context.handoffReady() ? GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_FAILURE_LAYER
                        : context.failureLayer());
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractNextResolutionStep",
                GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_NEXT_STEP);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractAttempted", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractImported", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractResolverHandoffSatisfied", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFocusedImportsSkipped", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractNoLocalhost", true);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFixtureSubstituted", false);
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractObservedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractReusedTags", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractSkippedFocusedImports", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractBrowserError", "");
        attributes.put("currentAppGeneratedImportsOwnerRuntimeContractModuleIdentity", "");
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeVersion",
                GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_VERSION);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeSource",
                GENERATED_IMPORTS_OWNER_LAZY_CHUNK_BRIDGE_SOURCE);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeGeneratedImportsTableShape",
                context.sourceGeneratedImportsTableShape());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkId", context.sourceChunkId());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkArtifact",
                context.sourceChunkVirtualPath());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkStatus",
                context.sourceChunkStatus());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeBuildOutputSource", context.mavenGoal());
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeSupportedSpiDecision",
                GENERATED_IMPORTS_OWNER_LAZY_CHUNK_SPI_DECISION);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeNoLocalhost", true);
        attributes.put("currentAppGeneratedImportsOwnerLazyChunkBridgeFixtureSubstituted", false);
    }

    private static void putGeneratedLazySpiDecision(Map<String, Object> attributes, Context context) {
        attributes.put("generatedLazySpiDecisionVersion", GENERATED_LAZY_SPI_DECISION_VERSION);
        attributes.put("generatedLazySpiDecisionSource", GENERATED_LAZY_SPI_DECISION_SOURCE);
        attributes.put("generatedLazySpiAuditedSourceSurfaces", GENERATED_LAZY_SPI_AUDITED_SOURCE_SURFACES);
        attributes.put("generatedLazySpiChunkInfoApiStatus", GENERATED_LAZY_SPI_CHUNK_INFO_API_STATUS);
        attributes.put("generatedLazySpiRegistrySource", GENERATED_LAZY_SPI_REGISTRY_SOURCE);
        attributes.put("generatedLazySpiGeneratedImportsArtifact", context.generatedImportsArtifact());
        attributes.put("generatedLazySpiGeneratedImportsSha256", context.sourceGeneratedImportsSha256());
        attributes.put("generatedLazySpiGeneratedImportsBytes", context.sourceGeneratedImportsBytes());
        attributes.put("generatedLazySpiGeneratedImportsTableShape", context.sourceGeneratedImportsTableShape());
        attributes.put("generatedLazySpiChunkId", context.sourceChunkId());
        attributes.put("generatedLazySpiChunkImportSpecifier", context.sourceChunkImportSpecifier());
        attributes.put("generatedLazySpiChunkArtifact", context.sourceChunkVirtualPath());
        attributes.put("generatedLazySpiChunkStatus", context.sourceChunkStatus());
        attributes.put("generatedLazySpiChunkSha256", context.sourceChunkSha256());
        attributes.put("generatedLazySpiChunkBytes", context.sourceChunkBytes());
        attributes.put("generatedLazySpiBuildOutputSource", context.mavenGoal());
        attributes.put("generatedLazySpiProductionSupportArtifact", context.productionSupportArtifact());
        attributes.put("generatedLazySpiStatsPath", context.statsPath());
        attributes.put("generatedLazySpiProdBundlePath", context.prodBundlePath());
        attributes.put("generatedLazySpiDecision", GENERATED_LAZY_SPI_DECISION);
        attributes.put("generatedLazySpiUnavailableReason", GENERATED_LAZY_SPI_UNAVAILABLE_REASON);
        attributes.put("generatedLazySpiFailureLayer", GENERATED_LAZY_SPI_FAILURE_LAYER);
        attributes.put("generatedLazySpiProductPath", GENERATED_LAZY_SPI_PRODUCT_PATH);
        attributes.put("generatedLazySpiExecutionEvidence", GENERATED_LAZY_SPI_EXECUTION_EVIDENCE);
        attributes.put("generatedLazySpiExecutionTrigger", GENERATED_LAZY_SPI_EXECUTION_TRIGGER);
        attributes.put("generatedLazySpiNextResolutionStep", GENERATED_LAZY_SPI_NEXT_STEP);
        attributes.put("generatedLazySpiAvailable", false);
        attributes.put("generatedLazySpiRuntimePathSelected", false);
        attributes.put("generatedLazySpiPackagingTimeStagingSelected", true);
        attributes.put("generatedLazySpiCurrentAppGeneratedImportsObserved",
                context.sourceGeneratedImportsObserved());
        attributes.put("generatedLazySpiCurrentAppChunkAvailable",
                "available".equals(context.sourceChunkStatus()));
        attributes.put("generatedLazySpiCurrentAppChunkExecuted", false);
        attributes.put("generatedLazySpiProductionSupportObserved", context.productionSupportObserved());
        attributes.put("generatedLazySpiStatsObserved", context.statsObserved());
        attributes.put("generatedLazySpiProdBundleObserved", context.prodBundleObserved());
        attributes.put("generatedLazySpiNoLocalhost", true);
        attributes.put("generatedLazySpiFixtureSubstituted", false);
    }

    record Context(
            boolean handoffReady,
            String failureLayer,
            String productionRuntimeOwnershipCandidate,
            String productionEntryScriptVirtualPath,
            String productionSupportArtifact,
            String productionBuildJsVirtualPaths,
            String productionSupportSha256,
            int productionSupportBytes,
            String sourceGeneratedImportsTableShape,
            String sourceChunkId,
            String sourceChunkVirtualPath,
            String sourceChunkStatus,
            String sourceChunkImportSpecifier,
            String sourceChunkSha256,
            int sourceChunkBytes,
            String mavenGoal,
            String generatedImportsArtifact,
            String sourceGeneratedImportsSha256,
            int sourceGeneratedImportsBytes,
            String statsPath,
            String prodBundlePath,
            boolean sourceGeneratedImportsObserved,
            boolean productionSupportObserved,
            boolean statsObserved,
            boolean prodBundleObserved) {
    }
}
