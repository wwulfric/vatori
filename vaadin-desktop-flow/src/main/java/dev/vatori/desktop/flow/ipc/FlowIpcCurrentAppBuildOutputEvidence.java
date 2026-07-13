package dev.vatori.desktop.flow.ipc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class FlowIpcCurrentAppBuildOutputEvidence {
    private FlowIpcCurrentAppBuildOutputEvidence() {
    }

    static CurrentAppBuildFrontendOutputEvidence current(Config config) {
        FrontendBuildStepEvidence buildStep = FlowIpcFrontendBuildEvidence.buildStep();
        Path module = FlowIpcBuildOutputScanner.appModuleDirectory();
        Path sourceGeneratedImports = FlowIpcBuildOutputScanner.sourceGeneratedImportsPath(module);
        FlowIpcBuildOutputScanner.FileEvidence sourceImports =
                FlowIpcBuildOutputScanner.fileEvidence(sourceGeneratedImports);
        String tableShape = "missing";
        String chunkId = "";
        String chunkImportSpecifier = "";
        String chunkArtifact = "";
        String chunkVirtualPath = "";
        String chunkStatus = sourceImports.observed() ? "not-generated" : "missing";
        FlowIpcBuildOutputScanner.FileEvidence sourceChunk =
                FlowIpcBuildOutputScanner.fileEvidence(module.resolve("src/main/frontend/generated/flow/chunks"));
        boolean sourceChunkContainsBareImports = false;
        String sourceChunkBareImportSpecifiers = "";
        String sourceChunkPackageResolvedSpecifiers = "";
        String sourceChunkPackageUnresolvedSpecifiers = "";
        String sourceChunkPackageResolverMode = "not-applicable";
        String sourceGeneratedImportsRootBareImportSpecifiers = "";

        if (sourceImports.observed()) {
            try {
                String body = Files.readString(sourceGeneratedImports, StandardCharsets.UTF_8);
                sourceGeneratedImportsRootBareImportSpecifiers =
                        String.join(",", FlowIpcJavaScriptModuleGraph.bareModuleImportSpecifiers(body));
                GeneratedLoadOnDemandChunkFileRegistry registry =
                        GeneratedLoadOnDemandChunkFileRegistry.parse(
                                config.generatedImportsRuntimePath(), body);
                tableShape = registry.loadOnDemandTableShape();
                chunkId = registry.chunkId();
                chunkImportSpecifier = registry.chunkImportSpecifier();
                if (!chunkImportSpecifier.isBlank()) {
                    Path chunkPath = sourceGeneratedImports.getParent()
                            .resolve(chunkImportSpecifier.replaceFirst("^\\./", ""))
                            .normalize();
                    sourceChunk = FlowIpcBuildOutputScanner.fileEvidence(chunkPath);
                    chunkArtifact = sourceChunk.path();
                    chunkVirtualPath = config.generatedChunkVirtualRoot()
                            + chunkImportSpecifier.replaceFirst("^\\./", "");
                    chunkStatus = sourceChunk.observed() ? "available" : "not-found";
                    if (sourceChunk.observed()) {
                        List<String> bareSpecifiers =
                                FlowIpcJavaScriptModuleGraph.bareModuleImportSpecifiers(
                                        Files.readString(chunkPath, StandardCharsets.UTF_8));
                        List<String> resolvedSpecifiers = bareSpecifiers.stream()
                                .filter(specifier -> isResolvableSpecifier(config, specifier))
                                .toList();
                        List<String> unresolvedSpecifiers = bareSpecifiers.stream()
                                .filter(specifier -> !isResolvableSpecifier(config, specifier))
                                .toList();
                        sourceChunkContainsBareImports = !bareSpecifiers.isEmpty();
                        sourceChunkBareImportSpecifiers = String.join(",", bareSpecifiers);
                        sourceChunkPackageResolvedSpecifiers = String.join(",", resolvedSpecifiers);
                        sourceChunkPackageUnresolvedSpecifiers = String.join(",", unresolvedSpecifiers);
                        if (bareSpecifiers.isEmpty()) {
                            sourceChunkPackageResolverMode = "no-bare-specifiers";
                        } else if (unresolvedSpecifiers.isEmpty()) {
                            sourceChunkPackageResolverMode = config.generatedChunkPackageResolverMode();
                        } else {
                            sourceChunkPackageResolverMode = "current-app-source-chunk-package-edge-unresolved";
                        }
                    }
                }
            } catch (IOException error) {
                tableShape = "unreadable";
                chunkStatus = "blocked";
            }
        }

        Path statsPath = module.resolve("target/classes/META-INF/VAADIN/config/stats.json").normalize();
        FlowIpcBuildOutputScanner.FileEvidence stats = FlowIpcBuildOutputScanner.fileEvidence(statsPath);
        FlowIpcBuildOutputScanner.FileEvidence prodBundle =
                FlowIpcBuildOutputScanner.fileEvidence(module.resolve("src/main/bundles/prod.bundle").normalize());
        Path productionBuildRoot = FlowIpcBuildOutputScanner.productionBuildRoot(module);
        boolean productionBuildObserved = Files.isDirectory(productionBuildRoot);
        String productionEntryScriptVirtualPath = FlowIpcBuildOutputScanner.productionEntryScriptVirtualPath(
                statsPath,
                config.productionBuildVirtualRoot());
        Path productionGeneratedImports = FlowIpcBuildOutputScanner.findProductionGeneratedImports(
                productionBuildRoot, productionEntryScriptVirtualPath);
        FlowIpcBuildOutputScanner.FileEvidence productionGeneratedImportsEvidence =
                FlowIpcBuildOutputScanner.fileEvidence(productionGeneratedImports);
        String productionBuildJsVirtualPaths = FlowIpcBuildOutputScanner.productionBuildVirtualPaths(
                productionBuildRoot,
                config.productionBuildVirtualRoot());
        String productionGeneratedImportsVirtualPath = productionGeneratedImports == null
                ? "" : FlowIpcBuildOutputScanner.productionBuildVirtualPath(
                        productionGeneratedImports,
                        config.productionBuildVirtualRoot());
        String statsBundleImports = String.join(",", FlowIpcBuildOutputScanner.statsBundleImports(statsPath));
        List<String> sourceRootBareSpecifiers =
                FlowIpcJavaScriptModuleGraph.csvValues(sourceGeneratedImportsRootBareImportSpecifiers);
        boolean rootProductionSupportObserved = productionGeneratedImportsEvidence.observed()
                && !productionGeneratedImportsVirtualPath.isBlank()
                && !statsBundleImports.isBlank();
        List<String> statsBundleImportList = FlowIpcJavaScriptModuleGraph.csvValues(statsBundleImports);
        List<String> sourceRootResolvedSpecifiers = sourceRootBareSpecifiers.stream()
                .filter(specifier -> rootProductionSupportObserved && statsBundleImportList.contains(specifier))
                .toList();
        List<String> sourceRootUnresolvedSpecifiers = sourceRootBareSpecifiers.stream()
                .filter(specifier -> !sourceRootResolvedSpecifiers.contains(specifier))
                .toList();
        String sourceGeneratedImportsRootResolvedSpecifiers = String.join(",", sourceRootResolvedSpecifiers);
        String sourceGeneratedImportsRootUnresolvedSpecifiers = String.join(",", sourceRootUnresolvedSpecifiers);
        String sourceGeneratedImportsRootPackageResolverMode;
        if (sourceRootBareSpecifiers.isEmpty()) {
            sourceGeneratedImportsRootPackageResolverMode = "no-root-bare-specifiers";
        } else if (sourceRootUnresolvedSpecifiers.isEmpty()) {
            sourceGeneratedImportsRootPackageResolverMode =
                    config.generatedImportsRootPackageResolverMode();
        } else {
            sourceGeneratedImportsRootPackageResolverMode =
                    "current-app-source-generated-imports-root-package-graph-unresolved";
        }
        boolean sourceGeneratedImportsRootPlaceholderUsed = false;
        String sourceGeneratedImportsRootFailureLayer = sourceRootUnresolvedSpecifiers.isEmpty()
                ? config.generatedImportsRootFailureLayer()
                : "current-app-source-generated-imports-root-package-specifier-unresolved";

        String runtimeCandidate;
        if (sourceImports.observed()) {
            runtimeCandidate = "source-generated-imports";
        } else if (productionGeneratedImportsEvidence.observed()) {
            runtimeCandidate = "production-webapp-build";
        } else {
            runtimeCandidate = "none";
        }

        String failureLayer;
        if (!"build-frontend".equals(buildStep.mavenGoal())) {
            failureLayer = "build-frontend-maven-goal-not-selected";
        } else if ("failed".equals(buildStep.status()) || "failed".equals(buildStep.mavenGoalResult())) {
            failureLayer = "build-frontend-maven-goal-failed-before-output";
        } else if (!sourceImports.observed() && !productionGeneratedImportsEvidence.observed()) {
            failureLayer = "build-frontend-output-missing-generated-imports";
        } else if ("available".equals(chunkStatus)
                && sourceChunkContainsBareImports
                && !sourceChunkPackageUnresolvedSpecifiers.isBlank()) {
            failureLayer = "current-app-source-generated-chunk-npm-specifier-unresolved";
        } else if (!sourceGeneratedImportsRootUnresolvedSpecifiers.isBlank()) {
            failureLayer = "current-app-source-generated-imports-root-package-specifier-unresolved";
        } else if ("available".equals(chunkStatus) && sourceChunkContainsBareImports) {
            failureLayer = config.generatedImportsRootFailureLayer();
        } else if ("available".equals(chunkStatus)) {
            failureLayer = "pending-current-app-source-generated-chunk-stage";
        } else if (productionGeneratedImportsEvidence.observed()) {
            failureLayer = "current-app-production-generated-imports-owner-boundary";
        } else {
            failureLayer = "current-app-build-frontend-output-incomplete";
        }

        return new CurrentAppBuildFrontendOutputEvidence(
                buildStep.mavenGoal(),
                sourceImports.observed(),
                sourceImports.path(),
                sourceImports.lastModifiedMillis(),
                sourceImports.bytes(),
                sourceImports.sha256(),
                tableShape,
                chunkId,
                chunkImportSpecifier,
                chunkArtifact,
                chunkVirtualPath,
                chunkStatus,
                sourceChunk.bytes(),
                sourceChunk.sha256(),
                sourceChunkContainsBareImports,
                sourceChunkBareImportSpecifiers,
                sourceChunkPackageResolvedSpecifiers,
                sourceChunkPackageUnresolvedSpecifiers,
                sourceChunkPackageResolverMode,
                sourceGeneratedImportsRootBareImportSpecifiers,
                statsBundleImports,
                sourceGeneratedImportsRootResolvedSpecifiers,
                sourceGeneratedImportsRootUnresolvedSpecifiers,
                sourceGeneratedImportsRootPackageResolverMode,
                productionGeneratedImportsVirtualPath,
                rootProductionSupportObserved,
                sourceGeneratedImportsRootPlaceholderUsed,
                sourceGeneratedImportsRootFailureLayer,
                productionEntryScriptVirtualPath,
                productionBuildJsVirtualPaths,
                productionBuildObserved,
                FlowIpcDiagnostics.redactLocation(productionBuildRoot.toString()),
                productionGeneratedImportsEvidence.observed(),
                productionGeneratedImportsEvidence.path(),
                productionGeneratedImportsEvidence.bytes(),
                productionGeneratedImportsEvidence.sha256(),
                stats.observed(),
                stats.path(),
                stats.bytes(),
                stats.sha256(),
                prodBundle.observed(),
                prodBundle.path(),
                prodBundle.bytes(),
                prodBundle.sha256(),
                runtimeCandidate,
                failureLayer);
    }

    private static boolean isResolvableSpecifier(Config config, String specifier) {
        return config.generatedChunkResolvedSpecifiers().equals(specifier);
    }

    record Config(
            String generatedImportsRuntimePath,
            String generatedChunkVirtualRoot,
            String productionBuildVirtualRoot,
            String generatedChunkResolvedSpecifiers,
            String generatedChunkPackageResolverMode,
            String generatedImportsRootPackageResolverMode,
            String generatedImportsRootFailureLayer) {
    }
}

record CurrentAppBuildFrontendOutputEvidence(
        String mavenGoal,
        boolean sourceGeneratedImportsObserved,
        String sourceGeneratedImportsPath,
        long sourceGeneratedImportsMtimeMillis,
        int sourceGeneratedImportsBytes,
        String sourceGeneratedImportsSha256,
        String sourceGeneratedImportsTableShape,
        String sourceChunkId,
        String sourceChunkImportSpecifier,
        String sourceChunkArtifact,
        String sourceChunkVirtualPath,
        String sourceChunkStatus,
        int sourceChunkBytes,
        String sourceChunkSha256,
        boolean sourceChunkContainsBareImports,
        String sourceChunkBareImportSpecifiers,
        String sourceChunkPackageResolvedSpecifiers,
        String sourceChunkPackageUnresolvedSpecifiers,
        String sourceChunkPackageResolverMode,
        String sourceGeneratedImportsRootBareImportSpecifiers,
        String statsBundleImports,
        String sourceGeneratedImportsRootResolvedSpecifiers,
        String sourceGeneratedImportsRootUnresolvedSpecifiers,
        String sourceGeneratedImportsRootPackageResolverMode,
        String sourceGeneratedImportsRootProductionSupportArtifact,
        boolean sourceGeneratedImportsRootProductionSupportObserved,
        boolean sourceGeneratedImportsRootPlaceholderUsed,
        String sourceGeneratedImportsRootFailureLayer,
        String productionEntryScriptVirtualPath,
        String productionBuildJsVirtualPaths,
        boolean productionBuildObserved,
        String productionBuildRoot,
        boolean productionGeneratedImportsObserved,
        String productionGeneratedImportsPath,
        int productionGeneratedImportsBytes,
        String productionGeneratedImportsSha256,
        boolean statsObserved,
        String statsPath,
        int statsBytes,
        String statsSha256,
        boolean prodBundleObserved,
        String prodBundlePath,
        int prodBundleBytes,
        String prodBundleSha256,
        String runtimeCandidate,
        String failureLayer) {
}
