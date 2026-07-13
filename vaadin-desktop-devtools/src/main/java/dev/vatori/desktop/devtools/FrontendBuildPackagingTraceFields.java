package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildPackagingTraceFields {
    boolean frontendInventoryV144ManifestObserved;
    boolean frontendInventoryV144RequestObserved;
    boolean frontendInventoryV144ResponseObserved;
    boolean frontendInventoryV144StageObserved;
    boolean frontendInventoryV144CompleteStageObserved;
    boolean frontendInventoryV144DomObserved;
    boolean runtimeHandoffReuseV145ManifestObserved;
    boolean runtimeHandoffReuseV145RequestObserved;
    boolean runtimeHandoffReuseV145ResponseObserved;
    boolean runtimeHandoffReuseV145StageObserved;
    boolean runtimeHandoffReuseV145CompleteStageObserved;
    boolean runtimeHandoffReuseV145DomObserved;
    boolean runtimeHandoffReuseV145FrameworkDomObserved;

    boolean hasFrontendInventoryV1_44Signals() {
        return frontendInventoryV144ManifestObserved
                && frontendInventoryV144RequestObserved
                && frontendInventoryV144ResponseObserved
                && frontendInventoryV144StageObserved
                && frontendInventoryV144CompleteStageObserved
                && frontendInventoryV144DomObserved;
    }

    boolean hasRuntimeHandoffReuseV1_45Signals() {
        return runtimeHandoffReuseV145ManifestObserved
                && runtimeHandoffReuseV145RequestObserved
                && runtimeHandoffReuseV145ResponseObserved
                && runtimeHandoffReuseV145StageObserved
                && runtimeHandoffReuseV145CompleteStageObserved
                && runtimeHandoffReuseV145DomObserved;
    }

    boolean hasFrameworkRuntimeHandoffReuseV1_45Signals() {
        return runtimeHandoffReuseV145ManifestObserved
                && runtimeHandoffReuseV145RequestObserved
                && runtimeHandoffReuseV145ResponseObserved
                && runtimeHandoffReuseV145StageObserved
                && runtimeHandoffReuseV145CompleteStageObserved
                && runtimeHandoffReuseV145FrameworkDomObserved;
    }

    static boolean containsPackagingFrontendInventoryV144Fields(String line) {
        return containsStringField(line, "packagingFrontendInventoryVersion",
                        "flow-ipc-packaging-time-frontend-inventory-v1_44")
                && containsStringField(line, "packagingFrontendInventorySource",
                        "generated-lazy-spi-decision-v1_43")
                && containsStringField(line, "packagingFrontendInventoryProducer",
                        "vaadin-maven-plugin-build-frontend")
                && containsStringField(line, "packagingFrontendInventoryMode",
                        "framework-managed-current-app-build-output")
                && containsStringField(line, "packagingFrontendInventoryMavenGoal", "build-frontend")
                && containsStringField(line, "packagingFrontendInventoryBuildStatus", "success")
                && containsStringField(line, "packagingFrontendInventoryMavenGoalResult", "success")
                && line.contains("packagingFrontendInventoryOutputUsability")
                && containsStringField(line, "packagingFrontendInventoryRequiredArtifacts",
                        "source-generated-imports,source-generated-chunk,production-generated-imports-support,production-entry,production-build-graph,stats-json,prod-bundle")
                && containsStringFieldPrefix(line, "packagingFrontendInventorySelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "packagingFrontendInventorySelectedArtifactCount")
                && containsStringField(line, "packagingFrontendInventoryGeneratedImportsArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && line.contains("packagingFrontendInventoryGeneratedImportsSha256")
                && containsPositiveIntegerField(line, "packagingFrontendInventoryGeneratedImportsBytes")
                && containsStringFieldPrefix(line, "packagingFrontendInventoryChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/chunk-")
                && line.contains("packagingFrontendInventoryChunkSha256")
                && containsPositiveIntegerField(line, "packagingFrontendInventoryChunkBytes")
                && containsStringFieldPrefix(line, "packagingFrontendInventoryProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("packagingFrontendInventoryProductionSupportSha256")
                && containsPositiveIntegerField(line, "packagingFrontendInventoryProductionSupportBytes")
                && line.contains("packagingFrontendInventoryProductionEntryArtifact")
                && line.contains("packagingFrontendInventoryProductionBuildArtifacts")
                && line.contains("packagingFrontendInventoryStatsPath")
                && line.contains("packagingFrontendInventoryStatsSha256")
                && containsPositiveIntegerField(line, "packagingFrontendInventoryStatsBytes")
                && line.contains("packagingFrontendInventoryProdBundlePath")
                && line.contains("packagingFrontendInventoryProdBundleSha256")
                && containsPositiveIntegerField(line, "packagingFrontendInventoryProdBundleBytes")
                && containsStringField(line, "packagingFrontendInventoryLoaderPolicy",
                        "custom-scheme-file-backed-primary-with-blob-diagnostic-fallback")
                && containsStringField(line, "packagingFrontendInventoryAllowedSchemes",
                        "vatori-resource,blob")
                && containsStringField(line, "packagingFrontendInventoryIntegrityPolicy",
                        "sha256-and-byte-count-for-selected-artifacts")
                && containsStringField(line, "packagingFrontendInventoryIdentityPolicy",
                        "stable-source-path-plus-owner-namespace")
                && containsStringField(line, "packagingFrontendInventoryCachePolicy",
                        "window-lifetime-cache-by-stable-resource-path")
                && containsStringField(line, "packagingFrontendInventoryCleanupPolicy",
                        "delete-staged-files-on-window-close")
                && containsStringField(line, "packagingFrontendInventoryRuntimeHandoffPolicy",
                        "generated-imports-owner-runtime-contract-v1_42")
                && containsStringField(line, "packagingFrontendInventoryProductPathDecision",
                        "packaging-time-build-frontend-staging")
                && containsStringField(line, "packagingFrontendInventoryRemainingBlockers",
                        "dev-mode-vite-parity,broad-component-resource-parity,installer-packaging,durable-reconnect")
                && line.contains("packagingFrontendInventoryNextResolutionStep")
                && containsLiteralField(line, "packagingFrontendInventoryCliContractEnabled", "true")
                && containsLiteralField(line,
                        "packagingFrontendInventoryBuildFrontendGoalSelected", "true")
                && containsLiteralField(line,
                        "packagingFrontendInventorySourceGeneratedImportsObserved", "true")
                && containsLiteralField(line, "packagingFrontendInventorySourceChunkAvailable", "true")
                && containsLiteralField(line,
                        "packagingFrontendInventoryProductionSupportObserved", "true")
                && containsLiteralField(line, "packagingFrontendInventoryStatsObserved", "true")
                && containsLiteralField(line, "packagingFrontendInventoryProdBundleObserved", "true")
                && containsLiteralField(line, "packagingFrontendInventoryManifestReady", "true")
                && containsLiteralField(line, "packagingFrontendInventoryRuntimeHandoffReady", "true")
                && containsLiteralField(line, "packagingFrontendInventoryNoLocalhost", "true")
                && containsLiteralField(line, "packagingFrontendInventoryFixtureSubstituted", "false");
    }

    static boolean containsPackagingFrontendInventoryV144Complete(String line) {
        return containsPackagingFrontendInventoryV144Fields(line)
                && containsLiteralField(line, "packagingFrontendInventoryRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "packagingFrontendInventoryStaged", "true")
                && containsLiteralField(line, "packagingFrontendInventoryCustomSchemeObserved", "true")
                && containsLiteralField(line, "packagingFrontendInventoryCacheObserved", "true")
                && containsLiteralField(line, "packagingFrontendInventoryCleanupObserved", "true");
    }

    static boolean containsPackagingRuntimeHandoffReuseV145Fields(String line) {
        return containsStringField(line, "packagingRuntimeHandoffReuseVersion",
                        "flow-ipc-packaging-runtime-handoff-reuse-v1_45")
                && containsStringField(line, "packagingRuntimeHandoffReuseSource",
                        "packaging-time-frontend-inventory-v1_44")
                && containsStringField(line, "packagingRuntimeHandoffReuseScope",
                        "module-relative-build-frontend-inventory-to-runtime-owner")
                && line.contains("packagingRuntimeHandoffReuseModulePath")
                && line.contains("packagingRuntimeHandoffReuseAppShape")
                && containsStringField(line, "packagingRuntimeHandoffReuseArtifactBasePolicy",
                        "module-relative-source-and-target-output")
                && containsStringField(line, "packagingRuntimeHandoffReuseRuntimeContracts",
                        "packaging-inventory-v1_44,generated-imports-owner-runtime-contract-v1_42,host-owned-custom-scheme-loader-v1_17")
                && containsStringField(line, "packagingRuntimeHandoffReuseValidationTargets",
                        "strict-smoke-app,framework-validation-app")
                && containsStringFieldPrefix(line, "packagingRuntimeHandoffReuseSelectedArtifacts",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsPositiveIntegerField(line, "packagingRuntimeHandoffReuseSelectedArtifactCount")
                && containsStringField(line, "packagingRuntimeHandoffReuseGeneratedImportsArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line, "packagingRuntimeHandoffReuseProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsStringField(line, "packagingRuntimeHandoffReuseMavenGoal", "build-frontend")
                && containsStringField(line, "packagingRuntimeHandoffReuseBuildStatus", "success")
                && line.contains("packagingRuntimeHandoffReuseOutputUsability")
                && containsStringField(line, "packagingRuntimeHandoffReuseRemainingBlockers",
                        "dev-mode-vite-parity,broad-component-resource-parity,installer-packaging,durable-reconnect")
                && line.contains("packagingRuntimeHandoffReuseNextResolutionStep")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseCliContractEnabled", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseSameContractAsInventory", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseReusableContract", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseModuleRelativeArtifacts", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseSmokeAppSpecific", "false")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseHostOwnedLoaderRequired", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseCustomSchemeRequired", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseManifestReady", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseRuntimeHandoffReady", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseNoLocalhost", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseFixtureSubstituted", "false");
    }

    static boolean containsPackagingRuntimeHandoffReuseV145Complete(String line) {
        return containsPackagingRuntimeHandoffReuseV145Fields(line)
                && containsLiteralField(line, "packagingRuntimeHandoffReuseRuntimeHandoffSatisfied", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseStaged", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseCustomSchemeObserved", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseCacheObserved", "true")
                && containsLiteralField(line, "packagingRuntimeHandoffReuseCleanupObserved", "true");
    }
}
