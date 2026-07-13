package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcFrontendPipelineAttributes {
    private static final String RESOURCE_LOADER_VERSION = "flow-ipc-resource-scheme-v1_17";
    private static final String RESOURCE_LOADER_MODES = "custom-scheme,file-backed,blob-fallback";
    private static final String RESOURCE_LOADER_PREFERRED_MODE = "custom-scheme-file-backed";
    private static final String RESOURCE_LOADER_ACCEPTABLE_FALLBACKS =
            "production-owner-blob-fallback,generated-lazy-blob-fallback,component-complex-graph-blob-fallback";
    private static final String RESOLVER_CONTRACT_VERSION =
            "flow-ipc-frontend-resolver-contract-v1_21";
    private static final String RESOLVER_CONTRACT_SCOPE =
            "flow-client,component-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-owner,dev-mode";
    private static final String RESOLVER_IDENTITY_POLICY =
            "stable-source-path-plus-owner-namespace-v1_21";
    private static final String RESOLVER_INTEGRITY_POLICY = "sha256-required-for-bodied-resources-v1_21";
    private static final String RESOLVER_CACHE_POLICY = "window-lifetime-content-addressed-v1_21";
    private static final String RESOLVER_CLEANUP_POLICY = "host-owned-window-lifetime-v1_21";
    private static final String RESOLVER_ALLOWED_SCHEMES = "blob,vatori-resource";
    private static final String RESOLVER_FAMILIES_COVERED =
            "flow-client-classpath,vaadin-component-classpath-frontend,npm-frontend,theme-static,generated-lazy,flow-lazy-dependency,production-bundle,dev-mode-bundle";
    private static final String RESOLVER_ACCEPTABLE_GAPS =
            "generated-lazy-chunk-file-not-generated,production-owner-owned-missing-real-artifact,dev-mode-vite-execution-unmapped";
    private static final String PIPELINE_CLOSURE_VERSION =
            "flow-ipc-frontend-pipeline-closure-v1_24";
    private static final String PIPELINE_CLOSURE_SCOPE = RESOLVER_CONTRACT_SCOPE;
    private static final String PIPELINE_CLOSURE_TRANSPORT = "no-localhost-ipc-resource-request";
    private static final String PIPELINE_CLOSURE_INFRASTRUCTURE_STATUS =
            "closed-for-representative-fixture-paths";
    private static final String PIPELINE_CLOSURE_REAL_BUILD_STATUS =
            "product-parity-pending";
    private static final String PIPELINE_CLOSURE_SATISFIED_PROOFS =
            "flow-client-executed,component-frontend-executed,theme-static-verified,flow-lazy-dependency-executed,generated-chunk-fixture-executed,production-owner-fixture-executed";
    private static final String PIPELINE_CLOSURE_REMAINING_BLOCKERS =
            "real-generated-lazy-chunk-file-not-generated,real-production-owner-owned-runtime-missing-real-artifact,dev-mode-vite-execution-unmapped";
    private static final String PIPELINE_CLOSURE_DECISION =
            "basic-pipeline-infrastructure-closed-real-build-parity-pending";
    private static final String PIPELINE_CLOSURE_NEXT_PRODUCT_PATH =
            "packaging-time-staged-frontend-or-supported-vaadin-build-spi";
    private static final String PACKAGED_FRONTEND_BASELINE_VERSION =
            "flow-ipc-packaged-frontend-baseline-v1_25";
    private static final String PACKAGED_FRONTEND_PRODUCT_PATH =
            "packaging-time-staged-frontend-baseline";
    private static final String PACKAGED_FRONTEND_INVENTORY_SCOPE = RESOLVER_CONTRACT_SCOPE;
    private static final String PACKAGED_FRONTEND_PREFERRED_LOADER = "custom-scheme-file-backed";
    private static final String PACKAGED_FRONTEND_ALLOWED_FALLBACK = "diagnostic-blob-only";
    private static final String PACKAGED_FRONTEND_INTEGRITY = "sha256-and-byte-count";
    private static final String PACKAGED_FRONTEND_CLEANUP_POLICY = "window-lifetime-cache-cleanup";
    private static final String PACKAGED_FRONTEND_REAL_BUILD_STATUS = "product-parity-pending";
    private static final String PACKAGED_FRONTEND_REMAINING_BLOCKERS =
            PIPELINE_CLOSURE_REMAINING_BLOCKERS;

    private FlowIpcFrontendPipelineAttributes() {
    }

    static Map<String, Object> resourceLoader() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendResourceLoaderVersion", RESOURCE_LOADER_VERSION);
        attributes.put("frontendResourceLoaderModes", RESOURCE_LOADER_MODES);
        attributes.put("frontendResourceLoaderPreferredMode", RESOURCE_LOADER_PREFERRED_MODE);
        attributes.put("frontendResourceLoaderNoLocalhost", true);
        attributes.put("frontendResourceLoaderAcceptableFallbacks",
                RESOURCE_LOADER_ACCEPTABLE_FALLBACKS);
        return attributes;
    }

    static Map<String, Object> resolverContract() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendResolverContractVersion", RESOLVER_CONTRACT_VERSION);
        attributes.put("frontendResolverContractScope", RESOLVER_CONTRACT_SCOPE);
        attributes.put("frontendResolverIdentityPolicy", RESOLVER_IDENTITY_POLICY);
        attributes.put("frontendResolverIntegrityPolicy", RESOLVER_INTEGRITY_POLICY);
        attributes.put("frontendResolverCachePolicy", RESOLVER_CACHE_POLICY);
        attributes.put("frontendResolverCleanupPolicy", RESOLVER_CLEANUP_POLICY);
        attributes.put("frontendResolverAllowedSchemes", RESOLVER_ALLOWED_SCHEMES);
        attributes.put("frontendResolverNoLocalhost", true);
        attributes.put("frontendResolverFamiliesCovered", RESOLVER_FAMILIES_COVERED);
        attributes.put("frontendResolverAcceptableGaps", RESOLVER_ACCEPTABLE_GAPS);
        attributes.putAll(pipelineClosure());
        attributes.putAll(packagedFrontendBaseline());
        return attributes;
    }

    private static Map<String, Object> pipelineClosure() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendPipelineClosureVersion", PIPELINE_CLOSURE_VERSION);
        attributes.put("frontendPipelineClosureScope", PIPELINE_CLOSURE_SCOPE);
        attributes.put("frontendPipelineClosureTransport", PIPELINE_CLOSURE_TRANSPORT);
        attributes.put("frontendPipelineClosureInfrastructureStatus",
                PIPELINE_CLOSURE_INFRASTRUCTURE_STATUS);
        attributes.put("frontendPipelineClosureRealBuildStatus", PIPELINE_CLOSURE_REAL_BUILD_STATUS);
        attributes.put("frontendPipelineClosureSatisfiedProofs", PIPELINE_CLOSURE_SATISFIED_PROOFS);
        attributes.put("frontendPipelineClosureRemainingBlockers",
                PIPELINE_CLOSURE_REMAINING_BLOCKERS);
        attributes.put("frontendPipelineClosureDecision", PIPELINE_CLOSURE_DECISION);
        attributes.put("frontendPipelineClosureNextProductPath", PIPELINE_CLOSURE_NEXT_PRODUCT_PATH);
        attributes.put("frontendPipelineClosureNoLocalhost", true);
        return attributes;
    }

    private static Map<String, Object> packagedFrontendBaseline() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("packagedFrontendBaselineVersion", PACKAGED_FRONTEND_BASELINE_VERSION);
        attributes.put("packagedFrontendProductPath", PACKAGED_FRONTEND_PRODUCT_PATH);
        attributes.put("packagedFrontendInventoryScope", PACKAGED_FRONTEND_INVENTORY_SCOPE);
        attributes.put("packagedFrontendPreferredLoader", PACKAGED_FRONTEND_PREFERRED_LOADER);
        attributes.put("packagedFrontendAllowedFallback", PACKAGED_FRONTEND_ALLOWED_FALLBACK);
        attributes.put("packagedFrontendIntegrity", PACKAGED_FRONTEND_INTEGRITY);
        attributes.put("packagedFrontendCleanupPolicy", PACKAGED_FRONTEND_CLEANUP_POLICY);
        attributes.put("packagedFrontendRealBuildStatus", PACKAGED_FRONTEND_REAL_BUILD_STATUS);
        attributes.put("packagedFrontendRemainingBlockers", PACKAGED_FRONTEND_REMAINING_BLOCKERS);
        return attributes;
    }
}
