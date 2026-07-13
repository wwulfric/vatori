package dev.vatori.desktop.flow.ipc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcCurrentAppGeneratedLazyResourceAttributes {
    private FlowIpcCurrentAppGeneratedLazyResourceAttributes() {
    }

    static Map<String, Object> attributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            boolean importsArtifact,
            Config config,
            Map<String, Object> generatedImportsRootAttributes,
            Map<String, Object> productionRuntimeOwnershipAttributes,
            Map<String, Object> buildFrontendOutputAttributes) {
        String path = importsArtifact
                ? config.generatedImportsPath()
                : evidence.sourceChunkVirtualPath();
        FrontendBuildStepEvidence buildStep = FlowIpcFrontendBuildEvidence.buildStep();
        AppBuildGeneratedImportsEvidence appOutput =
                FlowIpcFrontendBuildEvidence.appGeneratedImports(config.devBundleGeneratedFlowImportsPath());
        Map<String, Object> attributes = baseAttributes(evidence, importsArtifact, config, path);
        attributes.putAll(generatedImportsRootAttributes);
        attributes.put("currentAppBuildFrontendProductionBuildObserved",
                evidence.productionBuildObserved());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsObserved",
                evidence.productionGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsPath",
                evidence.productionGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("currentAppBuildFrontendProdBundlePath", evidence.prodBundlePath());
        attributes.put("currentAppBuildFrontendNoLocalhost", true);
        attributes.put("currentAppBuildFrontendFixtureSubstituted", false);
        attributes.putAll(productionRuntimeOwnershipAttributes);
        attributes.putAll(FlowIpcCurrentAppFrontendBuildOutputAttributes.preflightAvailabilityAttributes(
                buildStep,
                appOutput,
                FlowIpcFrontendBuildEvidence.runtimeSource(buildStep, appOutput),
                config.frontendBuildConfig()));
        if (importsArtifact) {
            putGeneratedImportsAttributes(attributes, evidence, config, path);
            attributes.putAll(buildFrontendOutputAttributes);
        } else {
            putGeneratedChunkAttributes(attributes, evidence, config, path);
        }
        return attributes;
    }

    private static Map<String, Object> baseAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            boolean importsArtifact,
            Config config,
            String path) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("componentFrontendResource", true);
        attributes.put("generatedLazyResource", true);
        attributes.put("currentAppBuildFrontendResource", true);
        attributes.put("currentAppBuildFrontendResourceRole", importsArtifact ? "generated-imports" : "chunk");
        attributes.put("generatedLazyBundleChunk", importsArtifact);
        attributes.put("generatedLazyBundleDependency", !importsArtifact);
        attributes.put("moduleSpecifier", path);
        attributes.put("resolver", "current-app-build-frontend-output-v1_32");
        attributes.put("shim", false);
        attributes.put("artifactFamily", "generated-lazy");
        attributes.put("origin", "current-app-build-output");
        attributes.put("resourceOrigin", importsArtifact
                ? "current-app-source-generated-flow-imports"
                : "current-app-source-generated-flow-imports-chunk");
        attributes.put("dependencyOf", importsArtifact ? "Flow.loadOnDemand"
                : config.generatedImportsPath());
        attributes.put("executionPolicy", importsArtifact
                ? "stage-and-import-current-app-generated-lazy-v1_32"
                : "current-app-generated-lazy-chunk-body-v1_32");
        attributes.put("artifactStatus", "available");
        attributes.put("requestedBy", "component-frontend-manifest-v1_32");
        attributes.put("failureClassification", importsArtifact
                ? "current-app-build-frontend-generated-imports-v1_32"
                : "current-app-build-frontend-generated-chunk-v1_32");
        attributes.put("blockedReason", evidence.failureLayer());
        attributes.put("lookupKind", importsArtifact
                ? "Current-app-generated-flow-imports-build-output-v1_32"
                : "Current-app-generated-flow-imports-chunk-build-output-v1_32");
        attributes.put("blockerVersion", config.buildFrontendOutputVersion());
        attributes.put("failureLayer", evidence.failureLayer());
        attributes.put("nextResolutionStep", config.buildFrontendNextStep());
        attributes.put("currentAppBuildFrontendOutputVersion", config.buildFrontendOutputVersion());
        attributes.put("currentAppBuildFrontendOutputSource", config.buildFrontendOutputSource());
        attributes.put("currentAppBuildFrontendMavenGoal", evidence.mavenGoal());
        attributes.put("currentAppBuildFrontendRuntimeCandidate", evidence.runtimeCandidate());
        attributes.put("currentAppBuildFrontendFailureLayer", evidence.failureLayer());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsObserved",
                evidence.sourceGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsPath",
                evidence.sourceGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsSha256",
                evidence.sourceGeneratedImportsSha256());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsTableShape",
                evidence.sourceGeneratedImportsTableShape());
        attributes.put("currentAppBuildFrontendSourceChunkStatus", evidence.sourceChunkStatus());
        attributes.put("currentAppBuildFrontendSourceChunkArtifact", evidence.sourceChunkArtifact());
        attributes.put("currentAppBuildFrontendSourceChunkVirtualPath", evidence.sourceChunkVirtualPath());
        attributes.put("currentAppBuildFrontendSourceChunkContainsBareImports",
                evidence.sourceChunkContainsBareImports());
        attributes.put("currentAppGeneratedChunkPackageResolverVersion",
                config.generatedChunkPackageResolverVersion());
        attributes.put("currentAppGeneratedChunkPackageResolverSource",
                config.generatedChunkPackageResolverSource());
        attributes.put("currentAppGeneratedChunkPackageResolverMode", evidence.sourceChunkPackageResolverMode());
        attributes.put("currentAppGeneratedChunkPackageBareSpecifiers",
                evidence.sourceChunkBareImportSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageResolvedSpecifiers",
                evidence.sourceChunkPackageResolvedSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageUnresolvedSpecifiers",
                evidence.sourceChunkPackageUnresolvedSpecifiers());
        attributes.put("currentAppGeneratedChunkPackageFailureLayer", evidence.failureLayer());
        attributes.put("currentAppGeneratedChunkPackageNextResolutionStep",
                config.generatedChunkPackageNextStep());
        return attributes;
    }

    private static void putGeneratedImportsAttributes(
            Map<String, Object> attributes,
            CurrentAppBuildFrontendOutputEvidence evidence,
            Config config,
            String path) {
        attributes.put("generatedLazyLoadOnDemandContractVersion",
                FlowIpcGeneratedLazyAttributes.LOAD_ON_DEMAND_CONTRACT_VERSION);
        attributes.put("generatedLazyTriggerSource", "current-app-build-frontend-output-v1_32");
        attributes.put("generatedLazyApiOwner", FlowIpcGeneratedLazyAttributes.LOAD_ON_DEMAND_API_OWNER);
        attributes.put("generatedLazyApiInstallSource",
                "current-app-generated-flow-imports-after-staged-import-v1_32");
        attributes.put("generatedLazyRequestedChunk", evidence.sourceChunkId());
        attributes.put("generatedLazyRegistrySource", "current-app-generated-flow-imports-source-v1_32");
        attributes.put("generatedLazyMappingSource", "current-app-generated-chunk-map-v1_32");
        attributes.put("generatedLazyResolvedArtifactPath", path);
        attributes.put("generatedLazyFlowClientTriggered", false);
        attributes.put("generatedLazyDiagnosticBridge", false);
        attributes.put("generatedLazyChunkExpressionSource", "bootstrap-uidl-dependencies");
        attributes.put("generatedLazyChunkTriggerOwner", "visible-flow-client-dependencyloader");
        attributes.put("generatedLazyChunkResolvedArtifactPath", path);
        attributes.put("generatedLazyChunkFailureLayer", evidence.failureLayer());
        attributes.put("generatedLazyChunkBlockerVersion", config.buildFrontendOutputVersion());
        attributes.put("generatedLazyChunkNextResolutionStep", config.buildFrontendNextStep());
        attributes.put("loadOnDemandChunkContractVersion",
                FlowIpcGeneratedLazyAttributes.CHUNK_CONTRACT_VERSION);
        attributes.put("loadOnDemandChunkApiOwner", FlowIpcGeneratedLazyAttributes.LOAD_ON_DEMAND_API_OWNER);
        attributes.put("loadOnDemandChunkApiInstallSource",
                "current-app-build-frontend-generated-imports-v1_32");
        attributes.put("loadOnDemandChunkRegistrySource", "current-app-generated-flow-imports-source-v1_32");
        attributes.put("loadOnDemandChunkMappingSource", "current-app-generated-chunk-map-v1_32");
        attributes.put("loadOnDemandChunkResolvedArtifactPath", path);
        attributes.put("loadOnDemandChunkModuleIdentity",
                "stable-current-app-build-frontend-module-identity-v1_32:" + path);
        attributes.put("loadOnDemandChunkFailureLayer", evidence.failureLayer());
        attributes.put("loadOnDemandChunkBlockerVersion", config.buildFrontendOutputVersion());
        attributes.put("loadOnDemandChunkNextResolutionStep", config.buildFrontendNextStep());
        attributes.put("chunkId", evidence.sourceChunkId());
        attributes.put("chunkImportSpecifier", evidence.sourceChunkImportSpecifier());
        attributes.put("chunkArtifactPath", evidence.sourceChunkVirtualPath());
        attributes.put("chunkFileStatus", evidence.sourceChunkStatus());
        attributes.put("chunkFileBytes", evidence.sourceChunkBytes());
        attributes.put("chunkFileSha256", evidence.sourceChunkSha256());
        attributes.put("chunkFileResolver", "current-app-generated-flow-imports-relative-chunk-v1_32");
        attributes.put("loadOnDemandChunkFileRegistryVersion", config.buildFrontendOutputVersion());
        attributes.put("loadOnDemandChunkFileRegistrySource",
                "current-app-generated-flow-imports-body-parser-v1_32");
        attributes.put("loadOnDemandChunkFileMappingSource", "current-app-generated-chunk-map-v1_32");
        attributes.put("loadOnDemandChunkFileGeneratedImportsArtifact", path);
        attributes.put("loadOnDemandChunkFileNoOpLoadOnDemand", false);
        attributes.put("loadOnDemandChunkFileTableShape", evidence.sourceGeneratedImportsTableShape());
        attributes.put("loadOnDemandChunkFileResolver",
                "current-app-generated-flow-imports-relative-chunk-v1_32");
        attributes.put("loadOnDemandChunkFileStatus", evidence.sourceChunkStatus());
        attributes.put("loadOnDemandChunkFileBytes", evidence.sourceChunkBytes());
        attributes.put("loadOnDemandChunkFileSha256", evidence.sourceChunkSha256());
        attributes.put("loadOnDemandChunkFileFailureLayer", evidence.failureLayer());
        attributes.put("loadOnDemandChunkFileBlockerVersion",
                config.generatedChunkPackageResolverVersion());
        attributes.put("loadOnDemandChunkFileNextResolutionStep",
                config.generatedChunkPackageNextStep());
        attributes.put("loadOnDemandChunkFileSideEffectProof",
                evidence.sourceChunkPackageUnresolvedSpecifiers().isBlank()
                        && evidence.sourceGeneratedImportsRootUnresolvedSpecifiers().isBlank()
                        ? "current-app-source-generated-imports-root-package-graph-resolved-v1_34"
                        : "chunk-file-body-available-but-bare-npm-imports-remain");
        attributes.put("importRewriteStrategy", "current-app-generated-imports-root-package-graph-rewrite-v1_34");
        attributes.put("browserExecutionPlan",
                "dynamic-import-current-app-generated-imports-with-root-package-graph-rewrites-v1_34");
        attributes.put("importRewrites", generatedImportsImportRewrites(evidence));
        attributes.put("transitiveImports", generatedImportsTransitiveImports(evidence));
        attributes.put("currentAppGeneratedChunkPackageRootPlaceholderUsed",
                evidence.sourceGeneratedImportsRootPlaceholderUsed());
    }

    private static void putGeneratedChunkAttributes(
            Map<String, Object> attributes,
            CurrentAppBuildFrontendOutputEvidence evidence,
            Config config,
            String path) {
        attributes.put("chunkId", evidence.sourceChunkId());
        attributes.put("chunkArtifactPath", path);
        attributes.put("chunkFileStatus", evidence.sourceChunkStatus());
        attributes.put("chunkFileBytes", evidence.sourceChunkBytes());
        attributes.put("chunkFileSha256", evidence.sourceChunkSha256());
        attributes.put("chunkFileResolver", "current-app-generated-flow-imports-relative-chunk-v1_32");
        attributes.put("loadOnDemandChunkFileStatus", evidence.sourceChunkStatus());
        attributes.put("loadOnDemandChunkFileBytes", evidence.sourceChunkBytes());
        attributes.put("loadOnDemandChunkFileSha256", evidence.sourceChunkSha256());
        attributes.put("loadOnDemandChunkFileSideEffectProof",
                evidence.sourceChunkPackageUnresolvedSpecifiers().isBlank()
                        ? "current-app-source-generated-chunk-package-edge-resolved-v1_33"
                        : "chunk-file-body-available-but-bare-npm-imports-remain");
        attributes.put("importRewriteStrategy", "current-app-generated-chunk-package-rewrite-v1_33");
        attributes.put("browserExecutionPlan",
                "dynamic-import-current-app-generated-chunk-with-package-rewrites-v1_33");
        attributes.put("importRewrites", generatedChunkImportRewrites(config));
        attributes.put("transitiveImports", List.of(config.devBundleIndexHtmlPath(), config.devBundleCommonJsPath()));
    }

    private static List<Map<String, Object>> generatedImportsImportRewrites(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        List<Map<String, Object>> rewrites = new ArrayList<>();
        String productionSupport = evidence.sourceGeneratedImportsRootProductionSupportArtifact();
        if (!productionSupport.isBlank()) {
            for (String specifier : FlowIpcJavaScriptModuleGraph.csvValues(evidence.sourceGeneratedImportsRootBareImportSpecifiers())) {
                rewrites.add(importRewrite(specifier,
                        productionSupport,
                        "current-app-production-build",
                        "current-app-generated-imports-root-production-support-rewrite-v1_34",
                        "current-app-generated-imports-root-package-graph-v1_34"));
            }
        }
        if (!evidence.sourceChunkImportSpecifier().isBlank()
                && !evidence.sourceChunkVirtualPath().isBlank()) {
            rewrites.add(importRewrite(evidence.sourceChunkImportSpecifier(),
                    evidence.sourceChunkVirtualPath(),
                    "generated-lazy",
                    "current-app-generated-imports-dynamic-chunk-rewrite-v1_33",
                    "current-app-generated-chunk-package-edge-v1_33"));
        }
        return rewrites;
    }

    private static List<Map<String, Object>> generatedChunkImportRewrites(Config config) {
        return List.of(importRewrite("@vaadin/vertical-layout/src/vaadin-vertical-layout.js",
                config.devBundleIndexHtmlPath(),
                "production-bundle",
                "current-app-generated-chunk-production-indexhtml-rewrite-v1_33",
                "current-app-generated-chunk-package-edge-v1_33"));
    }

    private static List<String> generatedImportsTransitiveImports(
            CurrentAppBuildFrontendOutputEvidence evidence) {
        List<String> imports = new ArrayList<>();
        for (String path : FlowIpcJavaScriptModuleGraph.csvValues(evidence.productionBuildJsVirtualPaths())) {
            if (!imports.contains(path)) {
                imports.add(path);
            }
        }
        if (!evidence.sourceChunkVirtualPath().isBlank()) {
            imports.add(evidence.sourceChunkVirtualPath());
        }
        return imports;
    }

    private static Map<String, Object> importRewrite(
            String specifier,
            String resourcePath,
            String artifactFamily,
            String resolver,
            String failureClassification) {
        return Map.of("specifier", specifier,
                "resourcePath", resourcePath,
                "artifactFamily", artifactFamily,
                "resolver", resolver,
                "failureClassification", failureClassification);
    }

    record Config(
            String generatedImportsPath,
            String buildFrontendOutputVersion,
            String buildFrontendOutputSource,
            String buildFrontendNextStep,
            String generatedChunkPackageResolverVersion,
            String generatedChunkPackageResolverSource,
            String generatedChunkPackageNextStep,
            String devBundleGeneratedFlowImportsPath,
            String devBundleIndexHtmlPath,
            String devBundleCommonJsPath,
            FlowIpcCurrentAppFrontendBuildOutputAttributes.Config frontendBuildConfig) {
    }
}
