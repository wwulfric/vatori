package dev.vatori.desktop.flow.ipc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FlowIpcCurrentAppProductionRuntimeOwnershipAttributes {
    private FlowIpcCurrentAppProductionRuntimeOwnershipAttributes() {
    }

    static Map<String, Object> attributes(
            CurrentAppBuildFrontendOutputEvidence evidence,
            Config config) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        boolean rootResolved = evidence.sourceGeneratedImportsRootUnresolvedSpecifiers().isBlank()
                && evidence.sourceGeneratedImportsRootProductionSupportObserved()
                && !evidence.sourceGeneratedImportsRootPlaceholderUsed();
        boolean chunkResolved = "available".equals(evidence.sourceChunkStatus())
                && evidence.sourceChunkPackageUnresolvedSpecifiers().isBlank();
        boolean handoffReady = evidence.sourceGeneratedImportsObserved()
                && chunkResolved
                && rootResolved
                && evidence.productionGeneratedImportsObserved()
                && evidence.statsObserved()
                && evidence.prodBundleObserved();
        String selectedArtifacts = artifactInventory(evidence, config.generatedImportsArtifact());
        attributes.put("currentAppProductionRuntimeOwnershipVersion", config.version());
        attributes.put("currentAppProductionRuntimeOwnershipSource", config.source());
        attributes.put("currentAppProductionRuntimeOwnershipPrimaryCandidate",
                config.primaryCandidate());
        attributes.put("currentAppProductionRuntimeOwnershipHandoffPolicy", config.handoffPolicy());
        attributes.put("currentAppProductionRuntimeOwnershipSelectedImportsArtifact",
                config.generatedImportsArtifact());
        attributes.put("currentAppProductionRuntimeOwnershipSelectedChunkArtifact",
                evidence.sourceChunkVirtualPath());
        attributes.put("currentAppProductionRuntimeOwnershipProductionSupportArtifact",
                evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        attributes.put("currentAppProductionRuntimeOwnershipProductionEntryArtifact",
                evidence.productionEntryScriptVirtualPath());
        attributes.put("currentAppProductionRuntimeOwnershipStatsObserved", evidence.statsObserved());
        attributes.put("currentAppProductionRuntimeOwnershipProdBundleObserved", evidence.prodBundleObserved());
        attributes.put("currentAppProductionRuntimeOwnershipArtifactInventory", selectedArtifacts);
        attributes.put("currentAppProductionRuntimeOwnershipRootPackageGraphResolved", rootResolved);
        attributes.put("currentAppProductionRuntimeOwnershipChunkPackageGraphResolved", chunkResolved);
        attributes.put("currentAppProductionRuntimeOwnershipProductionSupportObserved",
                evidence.sourceGeneratedImportsRootProductionSupportObserved());
        attributes.put("currentAppProductionRuntimeOwnershipHandoffReady", handoffReady);
        attributes.put("currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied", false);
        attributes.put("currentAppProductionRuntimeOwnershipOwnerReuseSatisfied", false);
        attributes.put("currentAppProductionRuntimeOwnershipNoLocalhost", true);
        attributes.put("currentAppProductionRuntimeOwnershipFixtureSubstituted", false);
        attributes.put("currentAppProductionRuntimeOwnershipFailureLayer",
                handoffReady ? config.failureLayer() : evidence.failureLayer());
        attributes.put("currentAppProductionRuntimeOwnershipNextResolutionStep",
                config.nextResolutionStep());
        attributes.putAll(FlowIpcPackagingHandoffAttributes.attributes(
                evidence,
                handoffReady,
                selectedArtifacts,
                config.packagingConfig()));
        attributes.putAll(FlowIpcCurrentAppOwnerAttributes.attributes(
                ownerContext(evidence, handoffReady, config)));
        return attributes;
    }

    private static FlowIpcCurrentAppOwnerAttributes.Context ownerContext(
            CurrentAppBuildFrontendOutputEvidence evidence,
            boolean handoffReady,
            Config config) {
        return new FlowIpcCurrentAppOwnerAttributes.Context(
                handoffReady,
                evidence.failureLayer(),
                config.primaryCandidate(),
                evidence.productionEntryScriptVirtualPath(),
                evidence.sourceGeneratedImportsRootProductionSupportArtifact(),
                evidence.productionBuildJsVirtualPaths(),
                evidence.productionGeneratedImportsSha256(),
                evidence.productionGeneratedImportsBytes(),
                evidence.sourceGeneratedImportsTableShape(),
                evidence.sourceChunkId(),
                evidence.sourceChunkVirtualPath(),
                evidence.sourceChunkStatus(),
                evidence.sourceChunkImportSpecifier(),
                evidence.sourceChunkSha256(),
                evidence.sourceChunkBytes(),
                evidence.mavenGoal(),
                config.generatedImportsArtifact(),
                evidence.sourceGeneratedImportsSha256(),
                evidence.sourceGeneratedImportsBytes(),
                evidence.statsPath(),
                evidence.prodBundlePath(),
                evidence.sourceGeneratedImportsObserved(),
                evidence.sourceGeneratedImportsRootProductionSupportObserved(),
                evidence.statsObserved(),
                evidence.prodBundleObserved());
    }

    private static String artifactInventory(
            CurrentAppBuildFrontendOutputEvidence evidence,
            String generatedImportsArtifact) {
        List<String> inventory = new ArrayList<>();
        addNonBlank(inventory, generatedImportsArtifact);
        addNonBlank(inventory, evidence.sourceChunkVirtualPath());
        addNonBlank(inventory, evidence.sourceGeneratedImportsRootProductionSupportArtifact());
        addNonBlank(inventory, evidence.productionEntryScriptVirtualPath());
        inventory.addAll(FlowIpcJavaScriptModuleGraph.csvValues(evidence.productionBuildJsVirtualPaths()));
        addNonBlank(inventory, evidence.prodBundlePath());
        return String.join(",", inventory.stream().distinct().toList());
    }

    private static void addNonBlank(List<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value);
        }
    }

    record Config(
            String version,
            String source,
            String primaryCandidate,
            String handoffPolicy,
            String failureLayer,
            String nextResolutionStep,
            String generatedImportsArtifact,
            FlowIpcPackagingHandoffAttributes.Config packagingConfig) {
    }
}
