package dev.vatori.desktop.flow.ipc;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcPackagingHandoffAttributes {
    private FlowIpcPackagingHandoffAttributes() {
    }

    static Map<String, Object> attributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            boolean handoffReady,
            String selectedArtifacts,
            Config config) {
        FrontendBuildStepEvidence buildStep = FlowIpcFrontendBuildEvidence.buildStep();
        Map<String, Object> attributes = new LinkedHashMap<>();
        putFrontendInventory(attributes, evidence, buildStep, handoffReady, selectedArtifacts, config);
        putRuntimeHandoffReuse(attributes, evidence, buildStep, handoffReady, selectedArtifacts, config);
        return attributes;
    }

    private static void putFrontendInventory(
            Map<String, Object> attributes,
            CurrentAppBuildFrontendOutputEvidence evidence,
            FrontendBuildStepEvidence buildStep,
            boolean handoffReady,
            String selectedArtifacts,
            Config config) {
        attributes.put("packagingFrontendInventoryVersion", config.frontendInventoryVersion());
        attributes.put("packagingFrontendInventorySource", config.frontendInventorySource());
        attributes.put("packagingFrontendInventoryProducer", config.frontendInventoryProducer());
        attributes.put("packagingFrontendInventoryMode", config.frontendInventoryMode());
        attributes.put("packagingFrontendInventoryMavenGoal", buildStep.mavenGoal());
        attributes.put("packagingFrontendInventoryBuildStatus", buildStep.status());
        attributes.put("packagingFrontendInventoryMavenGoalResult", buildStep.mavenGoalResult());
        attributes.put("packagingFrontendInventoryOutputUsability", buildStep.outputUsability());
        attributes.put("packagingFrontendInventoryOutputRoots", buildStep.outputRoots());
        attributes.put("packagingFrontendInventoryRequiredArtifacts",
                config.frontendInventoryRequiredArtifacts());
        attributes.put("packagingFrontendInventorySelectedArtifacts", selectedArtifacts);
        attributes.put("packagingFrontendInventorySelectedArtifactCount",
                FlowIpcJavaScriptModuleGraph.csvValues(selectedArtifacts).size());
        attributes.put("packagingFrontendInventoryGeneratedImportsArtifact",
                config.generatedImportsArtifact());
        attributes.put("packagingFrontendInventoryGeneratedImportsSha256",
                evidence.sourceGeneratedImportsSha256());
        attributes.put("packagingFrontendInventoryGeneratedImportsBytes",
                evidence.sourceGeneratedImportsBytes());
        attributes.put("packagingFrontendInventoryChunkArtifact", evidence.sourceChunkVirtualPath());
        attributes.put("packagingFrontendInventoryChunkSha256", evidence.sourceChunkSha256());
        attributes.put("packagingFrontendInventoryChunkBytes", evidence.sourceChunkBytes());
        attributes.put("packagingFrontendInventoryProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("packagingFrontendInventoryProductionSupportSha256",
                evidence.productionGeneratedImportsSha256());
        attributes.put("packagingFrontendInventoryProductionSupportBytes",
                evidence.productionGeneratedImportsBytes());
        attributes.put("packagingFrontendInventoryProductionEntryArtifact",
                evidence.productionEntryScriptVirtualPath());
        attributes.put("packagingFrontendInventoryProductionBuildArtifacts",
                evidence.productionBuildJsVirtualPaths());
        attributes.put("packagingFrontendInventoryStatsPath", evidence.statsPath());
        attributes.put("packagingFrontendInventoryStatsSha256", evidence.statsSha256());
        attributes.put("packagingFrontendInventoryStatsBytes", evidence.statsBytes());
        attributes.put("packagingFrontendInventoryProdBundlePath", evidence.prodBundlePath());
        attributes.put("packagingFrontendInventoryProdBundleSha256", evidence.prodBundleSha256());
        attributes.put("packagingFrontendInventoryProdBundleBytes", evidence.prodBundleBytes());
        attributes.put("packagingFrontendInventoryLoaderPolicy",
                config.frontendInventoryLoaderPolicy());
        attributes.put("packagingFrontendInventoryAllowedSchemes",
                config.frontendInventoryAllowedSchemes());
        attributes.put("packagingFrontendInventoryIntegrityPolicy",
                config.frontendInventoryIntegrityPolicy());
        attributes.put("packagingFrontendInventoryIdentityPolicy",
                config.frontendInventoryIdentityPolicy());
        attributes.put("packagingFrontendInventoryCachePolicy",
                config.frontendInventoryCachePolicy());
        attributes.put("packagingFrontendInventoryCleanupPolicy",
                config.frontendInventoryCleanupPolicy());
        attributes.put("packagingFrontendInventoryRuntimeHandoffPolicy",
                config.frontendInventoryHandoffPolicy());
        attributes.put("packagingFrontendInventoryProductPathDecision",
                FlowIpcCurrentAppOwnerAttributes.GENERATED_LAZY_SPI_DECISION);
        attributes.put("packagingFrontendInventoryRemainingBlockers",
                config.frontendInventoryRemainingBlockers());
        attributes.put("packagingFrontendInventoryNextResolutionStep",
                config.frontendInventoryNextStep());
        attributes.put("packagingFrontendInventoryCliContractEnabled",
                "1".equals(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_44")));
        attributes.put("packagingFrontendInventoryBuildFrontendGoalSelected",
                "build-frontend".equals(buildStep.mavenGoal()));
        attributes.put("packagingFrontendInventorySourceGeneratedImportsObserved",
                evidence.sourceGeneratedImportsObserved());
        attributes.put("packagingFrontendInventorySourceChunkAvailable",
                "available".equals(evidence.sourceChunkStatus()));
        attributes.put("packagingFrontendInventoryProductionSupportObserved",
                evidence.sourceGeneratedImportsRootProductionSupportObserved());
        attributes.put("packagingFrontendInventoryStatsObserved", evidence.statsObserved());
        attributes.put("packagingFrontendInventoryProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("packagingFrontendInventoryManifestReady", handoffReady);
        attributes.put("packagingFrontendInventoryRuntimeHandoffReady", handoffReady);
        attributes.put("packagingFrontendInventoryRuntimeHandoffSatisfied", false);
        attributes.put("packagingFrontendInventoryStaged", false);
        attributes.put("packagingFrontendInventoryCustomSchemeObserved", false);
        attributes.put("packagingFrontendInventoryBlobFallbackObserved", false);
        attributes.put("packagingFrontendInventoryCacheObserved", false);
        attributes.put("packagingFrontendInventoryCleanupObserved", false);
        attributes.put("packagingFrontendInventoryNoLocalhost", true);
        attributes.put("packagingFrontendInventoryFixtureSubstituted", false);
    }

    private static void putRuntimeHandoffReuse(
            Map<String, Object> attributes,
            CurrentAppBuildFrontendOutputEvidence evidence,
            FrontendBuildStepEvidence buildStep,
            boolean handoffReady,
            String selectedArtifacts,
            Config config) {
        Path module = FlowIpcBuildOutputScanner.appModuleDirectory();
        attributes.put("packagingRuntimeHandoffReuseVersion", config.runtimeHandoffReuseVersion());
        attributes.put("packagingRuntimeHandoffReuseSource", config.runtimeHandoffReuseSource());
        attributes.put("packagingRuntimeHandoffReuseScope", config.runtimeHandoffReuseScope());
        attributes.put("packagingRuntimeHandoffReuseModulePath",
                FlowIpcDiagnostics.redactLocation(module.toString()));
        attributes.put("packagingRuntimeHandoffReuseAppShape", appShape(module));
        attributes.put("packagingRuntimeHandoffReuseArtifactBasePolicy",
                config.runtimeHandoffReuseArtifactBasePolicy());
        attributes.put("packagingRuntimeHandoffReuseRuntimeContracts",
                config.runtimeHandoffReuseRuntimeContracts());
        attributes.put("packagingRuntimeHandoffReuseValidationTargets",
                config.runtimeHandoffReuseValidationTargets());
        attributes.put("packagingRuntimeHandoffReuseSelectedArtifacts", selectedArtifacts);
        attributes.put("packagingRuntimeHandoffReuseSelectedArtifactCount",
                FlowIpcJavaScriptModuleGraph.csvValues(selectedArtifacts).size());
        attributes.put("packagingRuntimeHandoffReuseGeneratedImportsArtifact",
                config.generatedImportsArtifact());
        attributes.put("packagingRuntimeHandoffReuseProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("packagingRuntimeHandoffReuseMavenGoal", buildStep.mavenGoal());
        attributes.put("packagingRuntimeHandoffReuseBuildStatus", buildStep.status());
        attributes.put("packagingRuntimeHandoffReuseOutputUsability", buildStep.outputUsability());
        attributes.put("packagingRuntimeHandoffReuseRemainingBlockers",
                config.runtimeHandoffReuseRemainingBlockers());
        attributes.put("packagingRuntimeHandoffReuseNextResolutionStep",
                config.runtimeHandoffReuseNextStep());
        attributes.put("packagingRuntimeHandoffReuseCliContractEnabled",
                "1".equals(System.getenv("VAADIN_DESKTOP_FLOW_IPC_FRONTEND_BUILD_V1_45")));
        attributes.put("packagingRuntimeHandoffReuseSameContractAsInventory", true);
        attributes.put("packagingRuntimeHandoffReuseReusableContract", true);
        attributes.put("packagingRuntimeHandoffReuseModuleRelativeArtifacts", true);
        attributes.put("packagingRuntimeHandoffReuseSmokeAppSpecific", false);
        attributes.put("packagingRuntimeHandoffReuseHostOwnedLoaderRequired", true);
        attributes.put("packagingRuntimeHandoffReuseCustomSchemeRequired", true);
        attributes.put("packagingRuntimeHandoffReuseManifestReady", handoffReady);
        attributes.put("packagingRuntimeHandoffReuseRuntimeHandoffReady", handoffReady);
        attributes.put("packagingRuntimeHandoffReuseRuntimeHandoffSatisfied", false);
        attributes.put("packagingRuntimeHandoffReuseStaged", false);
        attributes.put("packagingRuntimeHandoffReuseCustomSchemeObserved", false);
        attributes.put("packagingRuntimeHandoffReuseCacheObserved", false);
        attributes.put("packagingRuntimeHandoffReuseCleanupObserved", false);
        attributes.put("packagingRuntimeHandoffReuseNoLocalhost", true);
        attributes.put("packagingRuntimeHandoffReuseFixtureSubstituted", false);

        boolean productionChunkReady = handoffReady
                || productionChunkMatrixHandoffReady(evidence, buildStep);
        attributes.putAll(FlowIpcComponentMatrixAttributes.frameworkValidationMatrixAttributes(
                componentMatrixContext(evidence, buildStep, selectedArtifacts, handoffReady, config),
                componentMatrixContext(evidence, buildStep, selectedArtifacts, productionChunkReady, config)));
        attributes.putAll(FlowIpcDynamicResourceAttributes.matrixAttributes(
                componentMatrixContext(evidence, buildStep, selectedArtifacts, handoffReady, config),
                dynamicResourceContext(evidence, buildStep, selectedArtifacts, handoffReady, config)));
    }

    private static FlowIpcComponentMatrixAttributes.Context componentMatrixContext(
            CurrentAppBuildFrontendOutputEvidence evidence,
            FrontendBuildStepEvidence buildStep,
            String selectedArtifacts,
            boolean runtimeHandoffReady,
            Config config) {
        String appShape = appShape(FlowIpcBuildOutputScanner.appModuleDirectory());
        return new FlowIpcComponentMatrixAttributes.Context(
                appShape,
                "framework-validation-app".equals(appShape),
                selectedArtifacts,
                FlowIpcCurrentAppOwnerAttributes.GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SOURCE,
                config.generatedImportsArtifact(),
                evidence.sourceGeneratedImportsRootProductionSupportArtifact(),
                evidence.productionGeneratedImportsSha256(),
                evidence.productionGeneratedImportsBytes(),
                evidence.productionEntryScriptVirtualPath(),
                evidence.sourceChunkVirtualPath(),
                evidence.sourceChunkStatus(),
                evidence.sourceGeneratedImportsRootResolvedSpecifiers(),
                evidence.sourceGeneratedImportsRootUnresolvedSpecifiers(),
                config.frontendInventoryAllowedSchemes(),
                config.frontendInventoryIntegrityPolicy(),
                buildStep.mavenGoal(),
                buildStep.status(),
                buildStep.outputUsability(),
                runtimeHandoffReady);
    }

    private static boolean productionChunkMatrixHandoffReady(
            CurrentAppBuildFrontendOutputEvidence evidence,
            FrontendBuildStepEvidence buildStep) {
        return evidence.sourceGeneratedImportsObserved()
                && "available".equals(evidence.sourceChunkStatus())
                && !evidence.sourceChunkVirtualPath().isBlank()
                && !evidence.productionEntryScriptVirtualPath().isBlank()
                && !evidence.productionBuildJsVirtualPaths().isBlank()
                && evidence.statsObserved()
                && evidence.prodBundleObserved()
                && "success".equals(buildStep.status());
    }

    private static FlowIpcDynamicResourceAttributes.Context dynamicResourceContext(
            CurrentAppBuildFrontendOutputEvidence evidence,
            FrontendBuildStepEvidence buildStep,
            String selectedArtifacts,
            boolean runtimeHandoffReady,
            Config config) {
        String appShape = appShape(FlowIpcBuildOutputScanner.appModuleDirectory());
        return new FlowIpcDynamicResourceAttributes.Context(
                appShape,
                "framework-validation-app".equals(appShape),
                selectedArtifacts,
                FlowIpcCurrentAppOwnerAttributes.GENERATED_IMPORTS_OWNER_RUNTIME_CONTRACT_SOURCE,
                config.generatedImportsArtifact(),
                evidence.sourceGeneratedImportsRootProductionSupportArtifact(),
                evidence.productionGeneratedImportsSha256(),
                evidence.productionGeneratedImportsBytes(),
                evidence.productionEntryScriptVirtualPath(),
                buildStep.mavenGoal(),
                buildStep.status(),
                buildStep.outputUsability(),
                runtimeHandoffReady);
    }

    private static String appShape(Path module) {
        String path = module.toString().replace('\\', '/');
        if (path.endsWith("/test-apps/flow-over-ipc-smoke-app")) {
            return "strict-smoke-app";
        }
        if (path.endsWith("/test-apps/flow-over-ipc-framework-app")) {
            return "framework-validation-app";
        }
        return "developer-app";
    }

    record Config(
            String frontendInventoryVersion,
            String frontendInventorySource,
            String frontendInventoryProducer,
            String frontendInventoryMode,
            String frontendInventoryRequiredArtifacts,
            String frontendInventoryLoaderPolicy,
            String frontendInventoryAllowedSchemes,
            String frontendInventoryIntegrityPolicy,
            String frontendInventoryIdentityPolicy,
            String frontendInventoryCachePolicy,
            String frontendInventoryCleanupPolicy,
            String frontendInventoryHandoffPolicy,
            String frontendInventoryRemainingBlockers,
            String frontendInventoryNextStep,
            String runtimeHandoffReuseVersion,
            String runtimeHandoffReuseSource,
            String runtimeHandoffReuseScope,
            String runtimeHandoffReuseArtifactBasePolicy,
            String runtimeHandoffReuseRuntimeContracts,
            String runtimeHandoffReuseValidationTargets,
            String runtimeHandoffReuseRemainingBlockers,
            String runtimeHandoffReuseNextStep,
            String generatedImportsArtifact) {
    }
}
