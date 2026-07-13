package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcGeneratedLazyAttributes {
    static final String CHUNK_BLOCKER_VERSION = "flow-ipc-loadondemand-chunk-v1_11";
    static final String CHUNK_FILE_BLOCKER_VERSION =
            "flow-ipc-loadondemand-chunk-file-v1_12";

    private static final String RESOLVER_VERSION = "flow-ipc-generated-lazy-resolver-v1_8";
    private static final String EXECUTION_VERSION = "flow-ipc-generated-lazy-v1_8";
    private static final String IMPORT_RESOLUTION_STRATEGY =
            "stable-relative-import-rewrite-generated-lazy-v1_8";
    private static final String GRAPH_PROBE =
            "direct-generated-flow-imports-dev-bundle-chunk-v1_8";
    static final String LOAD_ON_DEMAND_CONTRACT_VERSION =
            "flow-ipc-loadondemand-v1_9";
    private static final String LOAD_ON_DEMAND_TRIGGER_SOURCE =
            "diagnostic-preflight-and-direct-staged-import";
    static final String LOAD_ON_DEMAND_API_OWNER =
            "window.Vaadin.Flow.loadOnDemand";
    private static final String LOAD_ON_DEMAND_API_INSTALL_SOURCE =
            "generated-flow-imports-dev-bundle-after-direct-staged-import";
    private static final String LOAD_ON_DEMAND_REQUESTED_CHUNK =
            "vatori-flow-ipc-generated-lazy-v1_9-probe";
    private static final String LOAD_ON_DEMAND_REGISTRY_SOURCE =
            "no-bootstrap-lazy-registry-observed";
    private static final String LOAD_ON_DEMAND_MAPPING_SOURCE =
            "component-frontend-manifest-generated-lazy-v1_9";
    private static final String BLOCKER_VERSION = "flow-ipc-loadondemand-v1_9";
    private static final String RELATIVE_IMPORT_SPECIFIER = "./markdown-helpers-31I0A-hg.js";
    private static final String FAILURE_LAYER =
            "loadondemand-api-installed-by-generated-imports-not-flow-client-triggered";
    private static final String NEXT_RESOLUTION_STEP =
            "discover the Flow client bootstrap lazy registry or supported Vaadin SPI that maps real lazy keys to generated-lazy artifacts without direct staged import";
    static final String CHUNK_CONTRACT_VERSION = "flow-ipc-loadondemand-chunk-v1_11";
    private static final String CHUNK_EXPRESSION_SOURCE = "bootstrap-uidl-dependencies";
    private static final String CHUNK_TRIGGER_OWNER = "visible-flow-client-dependencyloader";
    private static final String CHUNK_REGISTRY_SOURCE =
            "uidl-expression-to-dev-bundle-generated-flow-imports-registry-v1_11";
    private static final String CHUNK_MAPPING_SOURCE =
            "flow-ipc-generated-loadondemand-chunk-map-v1_11";
    private static final String CHUNK_API_INSTALL_SOURCE =
            "flow-ipc-loadondemand-bridge-before-generated-imports";
    private static final String CHUNK_FAILURE_LAYER =
            "dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file";
    private static final String CHUNK_NEXT_RESOLUTION_STEP =
            "derive chunk-id-to-file mapping from generated-flow-imports or stats.json instead of staging only the generated imports module";
    private static final String CHUNK_FILE_REGISTRY_VERSION =
            "flow-ipc-loadondemand-chunk-file-v1_12";
    private static final String CHUNK_FILE_EXECUTION_VERSION =
            "flow-ipc-loadondemand-chunk-file-execution-v1_22";
    private static final String CHUNK_FILE_FIXTURE_SOURCE =
            "repository-vaadin-shaped-generated-flow-imports-v1_22";
    private static final String CHUNK_FILE_FIXTURE_CHUNK_ID =
            "vatori-flow-ipc-generated-lazy-chunk-v1_22";
    private static final String CHUNK_FILE_FIXTURE_TRIGGER_OWNER =
            "repository-fixture-generated-loadondemand-executor";
    private static final String CHUNK_FILE_FIXTURE_SIDE_EFFECT =
            "window.VaadinDesktopGeneratedLazyChunkV122.executed";
    private static final String SOURCE_BOUNDARY_VERSION =
            "flow-ipc-generated-lazy-source-boundary-v1_26";
    private static final String SOURCE_MECHANISM =
            "vaadin-AbstractUpdateImports-generateLazyLoadedChunks";
    private static final String SOURCE_ARTIFACT =
            "com.vaadin.flow.server.frontend.AbstractUpdateImports";
    private static final String SOURCE_SCANNER =
            "com.vaadin.flow.server.frontend.scanner.FrontendDependencies";
    private static final String SOURCE_LAZY_ROUTE_RULE =
            "ChunkInfo(type=ROUTE,eager=false)";
    private static final String SOURCE_CHUNK_TRIGGER_INPUT =
            "DependencyTrigger classes plus lazyJavascript/lazyCss";
    private static final String SOURCE_POSITIVE_TABLE_SHAPE =
            "branch-table-dynamic-import";
    private static final String SOURCE_POSITIVE_IMPORT_PATTERN =
            "pending.push(import('./chunks/chunk-<content-hash>.js'))";
    private static final String SOURCE_NO_CHUNK_FALLBACK =
            "Promise.resolve(0)";
    private static final String SOURCE_CURRENT_BUILD_STATUS =
            "no-real-generated-lazy-chunk-produced";
    private static final String SOURCE_BUILD_INPUT_STATUS =
            "current-smoke-dev-bundle-has-no-lazy-javascript-or-css-chunk-input";
    private static final String SOURCE_SUPPORTED_BUILD_PATH =
            "run-vaadin-update-imports-with-non-eager-route-dependency-or-supported-lazy-chunk-spi";
    private static final String SOURCE_DECISION =
            "build-spi-required-before-real-app-chunk-parity";
    private static final String SOURCE_REMAINING_BLOCKER =
            "real-app-generated-chunk-file-not-produced-by-current-build";
    private static final String SOURCE_NEXT_STEP =
            "make the app build produce a non-empty lazyJavascript/lazyCss ChunkInfo or add a supported Vaadin lazy chunk SPI before replacing the v1.22 fixture with real app/build chunk parity";

    private FlowIpcGeneratedLazyAttributes() {
    }

    static Map<String, Object> manifestAttributes(
            Context context, Map<String, Object> primaryDiagnostics) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        putDevBundleContractAttributes(manifest, context, context.devBundleGeneratedFlowImports(), true);
        manifest.put("generatedLazyFailureLayer", FAILURE_LAYER);
        manifest.put("generatedLazyNextResolutionStep", NEXT_RESOLUTION_STEP);
        manifest.putAll(chunkFileRegistryAttributes(context.devBundleGeneratedFlowImports()));
        manifest.putAll(primaryDiagnostics);
        putChunkFileFixtureManifestAttributes(manifest, context);
        return manifest;
    }

    static Map<String, Object> sourceBoundaryAttributes(String currentBuildArtifact) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazySourceBoundaryVersion", SOURCE_BOUNDARY_VERSION);
        attributes.put("generatedLazySourceMechanism", SOURCE_MECHANISM);
        attributes.put("generatedLazySourceArtifact", SOURCE_ARTIFACT);
        attributes.put("generatedLazySourceScanner", SOURCE_SCANNER);
        attributes.put("generatedLazySourceLazyRouteRule", SOURCE_LAZY_ROUTE_RULE);
        attributes.put("generatedLazySourceChunkTriggerInput", SOURCE_CHUNK_TRIGGER_INPUT);
        attributes.put("generatedLazySourcePositiveTableShape", SOURCE_POSITIVE_TABLE_SHAPE);
        attributes.put("generatedLazySourcePositiveImportPattern", SOURCE_POSITIVE_IMPORT_PATTERN);
        attributes.put("generatedLazySourceNoChunkFallback", SOURCE_NO_CHUNK_FALLBACK);
        attributes.put("generatedLazySourceCurrentBuildArtifact", currentBuildArtifact);
        attributes.put("generatedLazySourceCurrentBuildStatus", SOURCE_CURRENT_BUILD_STATUS);
        attributes.put("generatedLazySourceCurrentBuildTableShape", "no-op-promise-resolve-0");
        attributes.put("generatedLazySourceCurrentBuildChunkStatus", "not-generated");
        attributes.put("generatedLazySourceCurrentBuildNoOpConfirmed", true);
        attributes.put("generatedLazySourceCurrentBuildChunkArtifact", "");
        attributes.put("generatedLazySourceBuildInputStatus", SOURCE_BUILD_INPUT_STATUS);
        attributes.put("generatedLazySourceSupportedBuildPath", SOURCE_SUPPORTED_BUILD_PATH);
        attributes.put("generatedLazySourceDecision", SOURCE_DECISION);
        attributes.put("generatedLazySourceRemainingBlocker", SOURCE_REMAINING_BLOCKER);
        attributes.put("generatedLazySourceNextResolutionStep", SOURCE_NEXT_STEP);
        attributes.put("generatedLazySourceNoLocalhost", true);
        return attributes;
    }

    static Map<String, Object> repositoryFixtureAttributes(Context context) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("moduleSpecifier", context.repositoryGeneratedFlowImports());
        attributes.put("resolver", "repository-generated-flow-imports-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "repository-generated-flow-imports");
        attributes.put("resourceOrigin", "repository-generated-flow-imports-fixture");
        attributes.put("dependencyOf", "Flow.loadOnDemand");
        attributes.put("executionPolicy", "verified-only-intentionally-not-executed");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification", "generated-flow-imports-verified");
        attributes.put("blockedReason",
                "verified generated Flow imports resource; full loadOnDemand execution remains unmapped");
        attributes.put("lookupKind", "Generated-flow-imports-resource-v1_4");
        attributes.put("generatedLazyLoadOnDemandContractVersion", LOAD_ON_DEMAND_CONTRACT_VERSION);
        attributes.put("generatedLazyTriggerSource", "repository-fixture-verified-only");
        attributes.put("generatedLazyApiOwner", "repository-local generated Flow fixture");
        attributes.put("generatedLazyApiInstallSource", "repository-generated-flow-imports-fixture");
        attributes.put("generatedLazyRequestedChunk", LOAD_ON_DEMAND_REQUESTED_CHUNK);
        attributes.put("generatedLazyRegistrySource", "repository-fixture-static-map");
        attributes.put("generatedLazyMappingSource", "repository-generated-flow-imports-fixture");
        attributes.put("generatedLazyResolvedArtifactPath", context.repositoryGeneratedFlowImports());
        attributes.put("generatedLazyFlowClientTriggered", false);
        attributes.put("generatedLazyDiagnosticBridge", false);
        attributes.put("generatedLazyChunkExpressionSource", "repository-fixture-verified-only");
        attributes.put("generatedLazyChunkTriggerOwner", "repository-local-generated-flow-fixture");
        attributes.put("generatedLazyChunkResolvedArtifactPath", context.repositoryGeneratedFlowImports());
        attributes.put("generatedLazyChunkDiagnosticBridgeUsed", false);
        attributes.put("generatedLazyChunkDirectImportProbeUsed", false);
        return attributes;
    }

    static Map<String, Object> chunkFileFixtureAttributes(Context context, boolean importsArtifact) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("generatedLazyChunkFileFixture", true);
        attributes.put("generatedLazyChunkFileFixtureRole", importsArtifact ? "imports" : "chunk");
        putChunkFileFixtureManifestAttributes(attributes, context);
        attributes.put("loadOnDemandChunkFileBlockerVersion", "");
        attributes.put("blockerClassification", "none");
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "repository-generated-flow-imports-fixture");
        attributes.put("resourceOrigin", importsArtifact
                ? "repository-generated-flow-imports-v1_22"
                : "repository-generated-flow-imports-v1_22-chunk");
        attributes.put("dependencyOf", importsArtifact
                ? "Flow.loadOnDemand"
                : context.repositoryGeneratedFlowImportsV122());
        attributes.put("executionPolicy", importsArtifact
                ? "stage-and-import-generated-lazy-chunk-file-fixture-v1_22"
                : "generated-lazy-chunk-file-body-v1_22");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_22");
        attributes.put("failureClassification", importsArtifact
                ? "generated-lazy-chunk-file-positive-fixture-v1_22"
                : "generated-lazy-chunk-file-body-positive-fixture-v1_22");
        attributes.put("blockedReason", importsArtifact
                ? "Vaadin-shaped repository fixture contains a real generated loadOnDemand chunk table and is used only to prove the no-localhost chunk-file executor positive path"
                : "Repository chunk body for the v1.22 generated loadOnDemand positive fixture");
        attributes.put("lookupKind", importsArtifact
                ? "Generated-flow-imports-positive-fixture-v1_22"
                : "Generated-flow-imports-positive-fixture-chunk-v1_22");
        attributes.put("moduleSpecifier", importsArtifact
                ? context.repositoryGeneratedFlowImportsV122()
                : context.repositoryGeneratedFlowImportsChunkV122());
        attributes.put("resolver", "repository-generated-flow-imports-chunk-file-fixture-v1_22");
        attributes.put("shim", false);
        if (importsArtifact) {
            attributes.putAll(chunkFileRegistryAttributes(context.repositoryGeneratedFlowImportsV122()));
            attributes.put("loadOnDemandChunkContractVersion", CHUNK_CONTRACT_VERSION);
            attributes.put("loadOnDemandChunkApiOwner", LOAD_ON_DEMAND_API_OWNER);
            attributes.put("loadOnDemandChunkApiInstallSource",
                    "repository-generated-flow-imports-v1_22-after-staged-import");
            attributes.put("loadOnDemandChunkRegistrySource", "repository-fixture-chunk-table-v1_22");
            attributes.put("loadOnDemandChunkMappingSource",
                    GeneratedLoadOnDemandChunkFileRegistry.MAPPING_SOURCE);
            attributes.put("loadOnDemandChunkResolvedArtifactPath",
                    context.repositoryGeneratedFlowImportsV122());
            attributes.put("loadOnDemandChunkRequestedChunk", CHUNK_FILE_FIXTURE_CHUNK_ID);
            attributes.put("loadOnDemandChunkFailureLayer", "");
            attributes.put("loadOnDemandChunkNextResolutionStep", "");
        } else {
            byte[] chunkBody = FlowIpcClasspathResources.bytes(
                    context.repositoryGeneratedFlowImportsChunkV122());
            attributes.put("chunkId", CHUNK_FILE_FIXTURE_CHUNK_ID);
            attributes.put("chunkArtifactPath", context.repositoryGeneratedFlowImportsChunkV122());
            attributes.put("chunkFileStatus", "available");
            attributes.put("chunkFileBytes", chunkBody.length);
            attributes.put("chunkFileSha256", FlowIpcDiagnostics.sha256(chunkBody));
            attributes.put("chunkFileResolver", GeneratedLoadOnDemandChunkFileRegistry.CHUNK_FILE_RESOLVER);
            attributes.put("loadOnDemandChunkFileStatus", "available");
            attributes.put("loadOnDemandChunkFileBytes", chunkBody.length);
            attributes.put("loadOnDemandChunkFileSha256", FlowIpcDiagnostics.sha256(chunkBody));
            attributes.put("loadOnDemandChunkFileSideEffectProof",
                    "chunk-file-body-available-for-staged-import");
        }
        return attributes;
    }

    static Map<String, Object> devBundleChunkAttributes(
            Context context,
            String path,
            String lookupKind,
            String dependencyOf,
            boolean primary,
            Map<String, Object> primaryDiagnostics,
            FlowIpcProductionOwnerAttributes.Context productionOwnerContext) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("generatedLazyBundleChunk", primary);
        attributes.put("generatedLazyBundleDependency", !primary);
        putDevBundleContractAttributes(attributes, context, path, primary);
        if (primary) {
            attributes.putAll(chunkFileRegistryAttributes(path));
            attributes.putAll(primaryDiagnostics);
        }
        attributes.put("devBundleResource", true);
        attributes.put("productionBundleResource", true);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "vaadin-dev-bundle-generated-flow-imports-v1_4");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "vaadin-dev-bundle");
        attributes.put("resourceOrigin",
                primary ? "vaadin-dev-bundle-generated-flow-imports"
                        : "vaadin-dev-bundle-generated-flow-imports-dependency");
        attributes.put("dependencyOf", dependencyOf);
        attributes.put("executionPolicy",
                primary ? "stage-and-import-generated-lazy-chunk-v1_8"
                        : "stable-dependency-module-generated-lazy-v1_8");
        attributes.put("artifactStatus", "partial");
        attributes.put("requestedBy", "component-frontend-manifest-v1_4");
        attributes.put("failureClassification",
                primary ? "generated-lazy-loadondemand-contract-narrowed-v1_9"
                        : "generated-lazy-dependency-module-resolved-v1_8");
        attributes.put("blockedReason",
                primary
                        ? "Vaadin dev-bundle generated-flow-imports chunk executes with relative imports rewritten to stable module identities; Flow.loadOnDemand remains a diagnostic call installed by that direct import instead of a Flow-client-triggered lazy request"
                        : "generated-flow-imports dependency chunk is available as a stable staged module identity for the v1.8 generated/lazy resolver");
        attributes.put("lookupKind", lookupKind);
        attributes.put("blockerVersion", BLOCKER_VERSION);
        attributes.put("failureLayer", FAILURE_LAYER);
        attributes.put("nextResolutionStep", NEXT_RESOLUTION_STEP);
        attributes.put("importResolutionStrategy", IMPORT_RESOLUTION_STRATEGY);
        attributes.put("moduleIdentityStrategy", "stable-resource-path-blob-module-identity-v1_7");
        FlowIpcProductionOwnerAttributes.putDependencyResolverAttributes(
                attributes, productionOwnerContext, path);
        attributes.put("productionOwnerDependencyResource", true);
        if (primary) {
            attributes.put("transitiveImports", List.of(
                    "vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js",
                    "vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js",
                    "vaadin-dev-bundle/webapp/VAADIN/build/markdown-helpers-31I0A-hg.js"));
        }
        return attributes;
    }

    static Map<String, Object> chunkFileRegistryAttributes(String generatedImportsPath) {
        GeneratedLoadOnDemandChunkFileRegistry registry =
                GeneratedLoadOnDemandChunkFileRegistry.parse(generatedImportsPath,
                        FlowIpcClasspathResources.text(generatedImportsPath));
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLoadOnDemandChunkFileRegistryVersion", CHUNK_FILE_REGISTRY_VERSION);
        attributes.put("generatedLoadOnDemandChunkFileRegistrySource", registry.registrySource());
        attributes.put("generatedLoadOnDemandChunkFileMappingSource", registry.mappingSource());
        attributes.put("generatedLoadOnDemandGeneratedImportsArtifact", registry.generatedImportsArtifact());
        attributes.put("generatedLoadOnDemandSourceBuildMode", registry.sourceBuildMode());
        attributes.put("generatedLoadOnDemandSourceArtifactFamily", registry.sourceArtifactFamily());
        attributes.put("generatedLoadOnDemandNoOpLoadOnDemand", registry.noOpLoadOnDemand());
        attributes.put("loadOnDemandTableShape", registry.loadOnDemandTableShape());
        attributes.put("chunkId", registry.chunkId());
        attributes.put("chunkImportSpecifier", registry.chunkImportSpecifier());
        attributes.put("chunkArtifactPath", registry.chunkArtifactPath());
        attributes.put("chunkContentHash", registry.chunkContentHash());
        attributes.put("chunkFileResolver", registry.chunkFileResolver());
        attributes.put("chunkFileStatus", registry.chunkFileStatus());
        attributes.put("chunkFileBytes", registry.chunkFileBytes());
        attributes.put("chunkFileSha256", registry.chunkFileSha256());
        attributes.put("noChunkGeneratedReason", registry.noChunkGeneratedReason());
        attributes.put("loadOnDemandChunkFileRegistryVersion", CHUNK_FILE_REGISTRY_VERSION);
        attributes.put("loadOnDemandChunkFileRegistrySource", registry.registrySource());
        attributes.put("loadOnDemandChunkFileMappingSource", registry.mappingSource());
        attributes.put("loadOnDemandChunkFileGeneratedImportsArtifact", registry.generatedImportsArtifact());
        attributes.put("loadOnDemandChunkFileNoOpLoadOnDemand", registry.noOpLoadOnDemand());
        attributes.put("loadOnDemandChunkFileTableShape", registry.loadOnDemandTableShape());
        attributes.put("loadOnDemandChunkFileResolver", registry.chunkFileResolver());
        attributes.put("loadOnDemandChunkFileStatus", registry.chunkFileStatus());
        attributes.put("loadOnDemandChunkFileBytes", registry.chunkFileBytes());
        attributes.put("loadOnDemandChunkFileSha256", registry.chunkFileSha256());
        attributes.put("loadOnDemandChunkFileFailureLayer", registry.failureLayer());
        attributes.put("loadOnDemandChunkFileBlockerVersion", CHUNK_FILE_BLOCKER_VERSION);
        attributes.put("loadOnDemandChunkFileNextResolutionStep", registry.nextResolutionStep());
        attributes.put("loadOnDemandChunkFileSideEffectProof", registry.sideEffectProof());
        return attributes;
    }

    private static void putDevBundleContractAttributes(
            Map<String, Object> attributes, Context context, String path, boolean primary) {
        attributes.put("generatedLazyResolverVersion", RESOLVER_VERSION);
        attributes.put("generatedLazyExecutionVersion", EXECUTION_VERSION);
        attributes.put("generatedLazyImportResolutionStrategy", IMPORT_RESOLUTION_STRATEGY);
        attributes.put("generatedLazyGraphProbe", GRAPH_PROBE);
        attributes.put("generatedLazyLoadOnDemandContractVersion", LOAD_ON_DEMAND_CONTRACT_VERSION);
        attributes.put("generatedLazyTriggerSource",
                primary ? LOAD_ON_DEMAND_TRIGGER_SOURCE : "generated-lazy-stable-relative-dependency");
        attributes.put("generatedLazyApiOwner", LOAD_ON_DEMAND_API_OWNER);
        attributes.put("generatedLazyApiInstallSource",
                primary ? LOAD_ON_DEMAND_API_INSTALL_SOURCE : "dependency-staged-for-generated-flow-imports");
        attributes.put("generatedLazyRequestedChunk", LOAD_ON_DEMAND_REQUESTED_CHUNK);
        attributes.put("generatedLazyRegistrySource", LOAD_ON_DEMAND_REGISTRY_SOURCE);
        attributes.put("generatedLazyMappingSource", LOAD_ON_DEMAND_MAPPING_SOURCE);
        attributes.put("generatedLazyResolvedArtifactPath", path);
        attributes.put("generatedLazyFlowClientTriggered", false);
        attributes.put("generatedLazyDiagnosticBridge", primary);
        attributes.put("generatedLazyBlockerVersion", BLOCKER_VERSION);
        attributes.put("generatedLazyRelativeImportSpecifier", RELATIVE_IMPORT_SPECIFIER);
        attributes.put("generatedLazyResolvedDependencyPath", context.devBundleMarkdownHelpers());
        attributes.put("generatedLazyChunkExpressionSource",
                primary ? CHUNK_EXPRESSION_SOURCE : "generated-lazy-stable-relative-dependency");
        attributes.put("generatedLazyChunkTriggerOwner",
                primary ? CHUNK_TRIGGER_OWNER : "generated-flow-imports-relative-import");
        attributes.put("generatedLazyChunkResolvedArtifactPath", path);
        attributes.put("generatedLazyChunkDiagnosticBridgeUsed", false);
        attributes.put("generatedLazyChunkDirectImportProbeUsed", false);
        attributes.put("generatedLazyChunkFailureLayer", CHUNK_FAILURE_LAYER);
        attributes.put("generatedLazyChunkBlockerVersion", CHUNK_BLOCKER_VERSION);
        attributes.put("generatedLazyChunkNextResolutionStep", CHUNK_NEXT_RESOLUTION_STEP);
        attributes.put("loadOnDemandChunkContractVersion", CHUNK_CONTRACT_VERSION);
        attributes.put("loadOnDemandChunkApiOwner", LOAD_ON_DEMAND_API_OWNER);
        attributes.put("loadOnDemandChunkApiInstallSource", CHUNK_API_INSTALL_SOURCE);
        attributes.put("loadOnDemandChunkRegistrySource", CHUNK_REGISTRY_SOURCE);
        attributes.put("loadOnDemandChunkMappingSource", CHUNK_MAPPING_SOURCE);
        attributes.put("loadOnDemandChunkResolvedArtifactPath", path);
        attributes.put("loadOnDemandChunkModuleIdentity",
                "stable-resource-path-blob-module-identity-v1_7:" + path);
        attributes.put("loadOnDemandChunkFailureLayer", CHUNK_FAILURE_LAYER);
        attributes.put("loadOnDemandChunkBlockerVersion", CHUNK_BLOCKER_VERSION);
        attributes.put("loadOnDemandChunkNextResolutionStep", CHUNK_NEXT_RESOLUTION_STEP);
    }

    private static void putChunkFileFixtureManifestAttributes(
            Map<String, Object> attributes, Context context) {
        attributes.put("generatedLazyChunkFileExecutionVersion", CHUNK_FILE_EXECUTION_VERSION);
        attributes.put("generatedLazyChunkFileFixtureSource", CHUNK_FILE_FIXTURE_SOURCE);
        attributes.put("generatedLazyChunkFileFixtureImportsArtifact",
                context.repositoryGeneratedFlowImportsV122());
        attributes.put("generatedLazyChunkFileFixtureChunkArtifact",
                context.repositoryGeneratedFlowImportsChunkV122());
        attributes.put("generatedLazyChunkFileFixtureChunkId", CHUNK_FILE_FIXTURE_CHUNK_ID);
        attributes.put("generatedLazyChunkFileFixtureTriggerOwner", CHUNK_FILE_FIXTURE_TRIGGER_OWNER);
        attributes.put("generatedLazyChunkFileFixtureExpectedSideEffect",
                CHUNK_FILE_FIXTURE_SIDE_EFFECT);
    }

    record Context(
            String repositoryGeneratedFlowImports,
            String repositoryGeneratedFlowImportsV122,
            String repositoryGeneratedFlowImportsChunkV122,
            String devBundleGeneratedFlowImports,
            String devBundleMarkdownHelpers) {
    }
}
