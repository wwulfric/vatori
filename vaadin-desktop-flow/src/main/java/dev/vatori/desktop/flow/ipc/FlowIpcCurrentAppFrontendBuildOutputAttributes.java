package dev.vatori.desktop.flow.ipc;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcCurrentAppFrontendBuildOutputAttributes {
    private FlowIpcCurrentAppFrontendBuildOutputAttributes() {
    }

    static Map<String, Object> frontendOutputAttributes(
            Config config,
            Map<String, Object> buildFrontendOutputAttributes) {
        AppBuildGeneratedImportsEvidence appOutput =
                FlowIpcFrontendBuildEvidence.appGeneratedImports(
                        config.devBundleGeneratedFlowImportsPath());
        FrontendBuildStepEvidence buildStep = FlowIpcFrontendBuildEvidence.buildStep();
        Path module = FlowIpcBuildOutputScanner.appModuleDirectory();
        List<Map<String, Object>> inventory = FlowIpcBuildOutputScanner.frontendBuildOutputInventory(
                module,
                config.devBundleGeneratedFlowImportsPath());
        String outputRoot = FlowIpcFrontendBuildEvidence.firstObservedInventoryPath(inventory);
        if (outputRoot.isBlank()) {
            outputRoot = FlowIpcDiagnostics.redactLocation(module.resolve("target").normalize().toString());
        }

        String runtimeSource = FlowIpcFrontendBuildEvidence.runtimeSource(buildStep, appOutput);

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppFrontendBuildOutputVersion", config.frontendBuildOutputVersion());
        attributes.put("currentAppFrontendBuildOutputDiscoverySource",
                config.frontendBuildDiscoverySource());
        attributes.put("currentAppFrontendBuildStepCommand", buildStep.command());
        attributes.put("currentAppFrontendBuildStepExitCode", buildStep.exitCode());
        attributes.put("currentAppFrontendBuildStepStatus", buildStep.status());
        attributes.put("currentAppFrontendBuildStepDurationMillis", buildStep.durationMillis());
        attributes.put("currentAppFrontendBuildStepSummary", buildStep.summary());
        attributes.put("currentAppFrontendBuildStepStdoutLog", buildStep.stdoutLog());
        attributes.put("currentAppFrontendBuildStepStderrLog", buildStep.stderrLog());
        attributes.put("currentAppFrontendBuildModulePath", appOutput.modulePath());
        attributes.put("currentAppFrontendBuildOutputRoots", buildStep.outputRoots());
        attributes.put("currentAppFrontendBuildOutputRoot", outputRoot);
        attributes.put("currentAppFrontendBuildOutputInventory", inventory);
        attributes.put("currentAppFrontendBuildOutputRuntimeSource", runtimeSource);
        attributes.put("currentAppFrontendBuildGeneratedImportsPath", appOutput.path());
        attributes.put("currentAppFrontendBuildGeneratedImportsObserved", appOutput.observed());
        attributes.put("currentAppFrontendBuildGeneratedImportsMtimeMillis",
                appOutput.lastModifiedMillis());
        attributes.put("currentAppFrontendBuildGeneratedImportsBytes", appOutput.bytes());
        attributes.put("currentAppFrontendBuildGeneratedImportsSha256", appOutput.sha256());
        attributes.put("currentAppFrontendBuildGeneratedImportsTableShape", appOutput.tableShape());
        attributes.put("currentAppFrontendBuildGeneratedImportsNoOpLoadOnDemand",
                appOutput.noOpLoadOnDemand());
        attributes.put("currentAppFrontendBuildChunkReferences", appOutput.chunkReferences());
        attributes.put("currentAppFrontendBuildChunkStatus", appOutput.chunkStatus());
        attributes.put("currentAppFrontendBuildChunkArtifact", appOutput.chunkArtifact());
        attributes.put("currentAppFrontendBuildFailureLayer",
                frontendBuildOutputFailureLayer(buildStep, appOutput));
        attributes.put("currentAppFrontendBuildNextResolutionStep", config.frontendBuildNextStep());
        attributes.put("currentAppFrontendBuildFixtureSubstituted", false);
        attributes.put("currentAppFrontendBuildNoLocalhost", true);
        attributes.putAll(preflightAvailabilityAttributes(buildStep, appOutput, runtimeSource, config));
        attributes.putAll(buildFrontendOutputAttributes);
        return attributes;
    }

    static Map<String, Object> buildFrontendOutputAttributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            FrontendBuildStepEvidence buildStep,
            Config config,
            Map<String, Object> generatedImportsRootAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppBuildFrontendOutputVersion", config.buildFrontendOutputVersion());
        attributes.put("currentAppBuildFrontendOutputSource", config.buildFrontendOutputSource());
        attributes.put("currentAppBuildFrontendMavenGoal", evidence.mavenGoal());
        attributes.put("currentAppBuildFrontendProductionMode", buildStep.productionMode());
        attributes.put("currentAppBuildFrontendForceProductionBuild", buildStep.forceProductionBuild());
        attributes.put("currentAppBuildFrontendIgnoreVersionChecks", buildStep.ignoreVersionChecks());
        attributes.put("currentAppBuildFrontendOutputUsability", buildStep.outputUsability());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsObserved",
                evidence.sourceGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsPath",
                evidence.sourceGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsMtimeMillis",
                evidence.sourceGeneratedImportsMtimeMillis());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsBytes",
                evidence.sourceGeneratedImportsBytes());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsSha256",
                evidence.sourceGeneratedImportsSha256());
        attributes.put("currentAppBuildFrontendSourceGeneratedImportsTableShape",
                evidence.sourceGeneratedImportsTableShape());
        attributes.put("currentAppBuildFrontendSourceChunkId", evidence.sourceChunkId());
        attributes.put("currentAppBuildFrontendSourceChunkImportSpecifier",
                evidence.sourceChunkImportSpecifier());
        attributes.put("currentAppBuildFrontendSourceChunkArtifact", evidence.sourceChunkArtifact());
        attributes.put("currentAppBuildFrontendSourceChunkVirtualPath", evidence.sourceChunkVirtualPath());
        attributes.put("currentAppBuildFrontendSourceChunkStatus", evidence.sourceChunkStatus());
        attributes.put("currentAppBuildFrontendSourceChunkBytes", evidence.sourceChunkBytes());
        attributes.put("currentAppBuildFrontendSourceChunkSha256", evidence.sourceChunkSha256());
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
        attributes.putAll(generatedImportsRootAttributes);
        attributes.put("currentAppBuildFrontendProductionBuildObserved",
                evidence.productionBuildObserved());
        attributes.put("currentAppBuildFrontendProductionBuildRoot", evidence.productionBuildRoot());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsObserved",
                evidence.productionGeneratedImportsObserved());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsPath",
                evidence.productionGeneratedImportsPath());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsBytes",
                evidence.productionGeneratedImportsBytes());
        attributes.put("currentAppBuildFrontendProductionGeneratedImportsSha256",
                evidence.productionGeneratedImportsSha256());
        attributes.put("currentAppBuildFrontendStatsObserved", evidence.statsObserved());
        attributes.put("currentAppBuildFrontendStatsPath", evidence.statsPath());
        attributes.put("currentAppBuildFrontendProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("currentAppBuildFrontendProdBundlePath", evidence.prodBundlePath());
        attributes.put("currentAppBuildFrontendProdBundleBytes", evidence.prodBundleBytes());
        attributes.put("currentAppBuildFrontendProdBundleSha256", evidence.prodBundleSha256());
        attributes.put("currentAppBuildFrontendRuntimeCandidate", evidence.runtimeCandidate());
        attributes.put("currentAppBuildFrontendFailureLayer", evidence.failureLayer());
        attributes.put("currentAppBuildFrontendNextResolutionStep", config.buildFrontendNextStep());
        attributes.put("currentAppBuildFrontendNoLocalhost", true);
        attributes.put("currentAppBuildFrontendFixtureSubstituted", false);
        return attributes;
    }

    static Map<String, Object> preflightAvailabilityAttributes(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput,
            String runtimeSource,
            Config config) {
        String failureLayer = preflightFailureLayer(buildStep, appOutput);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("frontendBuildPreflightAvailabilityVersion",
                config.preflightAvailabilityVersion());
        attributes.put("frontendBuildPreflightAvailabilitySource",
                config.preflightAvailabilitySource());
        attributes.put("frontendBuildPreflightAvailabilityMode", buildStep.availabilityMode());
        attributes.put("frontendBuildPreflightEffectiveMavenMode", buildStep.effectiveMavenMode());
        attributes.put("frontendBuildPreflightPluginCoordinates", buildStep.pluginCoordinates());
        attributes.put("frontendBuildPreflightPluginResolutionStatus",
                buildStep.pluginResolutionStatus());
        attributes.put("frontendBuildPreflightMavenGoalResult", buildStep.mavenGoalResult());
        attributes.put("frontendBuildPreflightOutputUsability", buildStep.outputUsability());
        attributes.put("frontendBuildPreflightRuntimeSourceDecision", runtimeSource);
        attributes.put("frontendBuildPreflightBuildStepStatus", buildStep.status());
        attributes.put("frontendBuildPreflightFailureLayer", failureLayer);
        attributes.put("frontendBuildPreflightNextResolutionStep", config.preflightNextStep());
        attributes.put("frontendBuildPreflightBuildStepUnavailableCleared",
                !"unavailable".equals(buildStep.status())
                        && !"missing".equals(buildStep.pluginResolutionStatus())
                        && !"plugin-unavailable".equals(buildStep.mavenGoalResult()));
        attributes.put("frontendBuildPreflightNoLocalhost", true);
        return attributes;
    }

    private static String frontendBuildOutputFailureLayer(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput) {
        boolean buildStepAttempted = "success".equals(buildStep.status())
                || "failed".equals(buildStep.status())
                || "unavailable".equals(buildStep.status());
        if (!buildStepAttempted) {
            return "current-app-frontend-build-step-not-enabled";
        }
        if ("unavailable".equals(buildStep.status())) {
            return "vaadin-frontend-build-step-unavailable";
        }
        if (!appOutput.observed()) {
            return "vaadin-frontend-build-step-output-incomplete";
        }
        if ("available".equals(appOutput.chunkStatus())) {
            return "pending-real-generated-chunk-stage";
        }
        if ("branch-table-dynamic-import".equals(appOutput.tableShape())) {
            return "current-app-build-output-generated-chunk-file-missing";
        }
        return "current-app-build-output-has-no-generated-lazy-chunk";
    }

    private static String preflightFailureLayer(
            FrontendBuildStepEvidence buildStep,
            AppBuildGeneratedImportsEvidence appOutput) {
        if ("skipped".equals(buildStep.status())) {
            return "frontend-build-preflight-not-enabled";
        }
        if ("unavailable".equals(buildStep.status())
                || "missing".equals(buildStep.pluginResolutionStatus())
                || "plugin-unavailable".equals(buildStep.mavenGoalResult())) {
            return "frontend-build-plugin-unavailable";
        }
        if ("failed".equals(buildStep.status()) || "failed".equals(buildStep.mavenGoalResult())) {
            return "frontend-build-maven-goal-failed-before-output";
        }
        if (!"success".equals(buildStep.status())) {
            return "frontend-build-preflight-unknown";
        }
        if (!appOutput.observed()) {
            return "frontend-build-plugin-resolved-output-incomplete";
        }
        if ("available".equals(appOutput.chunkStatus())) {
            return "current-app-build-output-generated-chunk-available-pending-stage";
        }
        if ("branch-table-dynamic-import".equals(appOutput.tableShape())) {
            return "current-app-build-output-generated-chunk-file-missing";
        }
        return "current-app-build-output-has-no-generated-lazy-chunk";
    }

    record Config(
            String frontendBuildOutputVersion,
            String frontendBuildDiscoverySource,
            String frontendBuildNextStep,
            String preflightAvailabilityVersion,
            String preflightAvailabilitySource,
            String preflightNextStep,
            String buildFrontendOutputVersion,
            String buildFrontendOutputSource,
            String buildFrontendNextStep,
            String generatedChunkPackageResolverVersion,
            String generatedChunkPackageResolverSource,
            String generatedChunkPackageNextStep,
            String devBundleGeneratedFlowImportsPath) {
    }
}
