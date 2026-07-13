package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcCurrentAppGeneratedImportsRootAttributes {
    private FlowIpcCurrentAppGeneratedImportsRootAttributes() {
    }

    static Map<String, Object> attributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            Config config) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("currentAppGeneratedImportsRootPackageGraphVersion",
                config.packageGraphVersion());
        attributes.put("currentAppGeneratedImportsRootPackageGraphSource",
                config.packageGraphSource());
        attributes.put("currentAppGeneratedImportsRootPackageResolverMode",
                evidence.sourceGeneratedImportsRootPackageResolverMode());
        attributes.put("currentAppGeneratedImportsRootBareSpecifiers",
                evidence.sourceGeneratedImportsRootBareImportSpecifiers());
        attributes.put("currentAppGeneratedImportsRootStatsBundleImports",
                evidence.statsBundleImports());
        attributes.put("currentAppGeneratedImportsRootResolvedSpecifiers",
                evidence.sourceGeneratedImportsRootResolvedSpecifiers());
        attributes.put("currentAppGeneratedImportsRootUnresolvedSpecifiers",
                evidence.sourceGeneratedImportsRootUnresolvedSpecifiers());
        attributes.put("currentAppGeneratedImportsRootProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppGeneratedImportsRootProductionSupportObserved",
                evidence.sourceGeneratedImportsRootProductionSupportObserved());
        attributes.put("currentAppGeneratedImportsRootProductionEntryArtifact",
                evidence.productionEntryScriptVirtualPath());
        attributes.put("currentAppGeneratedImportsRootProductionBuildArtifacts",
                evidence.productionBuildJsVirtualPaths());
        attributes.put("currentAppGeneratedImportsRootPlaceholderUsed",
                evidence.sourceGeneratedImportsRootPlaceholderUsed());
        attributes.put("currentAppGeneratedImportsRootFailureLayer",
                evidence.sourceGeneratedImportsRootFailureLayer());
        attributes.put("currentAppGeneratedImportsRootNextResolutionStep",
                config.nextResolutionStep());
        attributes.putAll(FlowIpcCurrentAppProductionRuntimeOwnershipAttributes.attributes(
                evidence,
                config.productionRuntimeOwnershipConfig()));
        return attributes;
    }

    record Config(
            String packageGraphVersion,
            String packageGraphSource,
            String nextResolutionStep,
            FlowIpcCurrentAppProductionRuntimeOwnershipAttributes.Config
                    productionRuntimeOwnershipConfig) {
    }
}
