package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcProductionRuntimeAttributes {
    static final String BLOCKER = "flow-ipc-production-runtime-v1_10";

    private static final String VERSION = "flow-ipc-production-runtime-v1_10";
    private static final String REGISTRY_SOURCE =
            "vaadin-dev-bundle-stats-runtime-registry-v1_10";
    private static final String MODULE_IDENTITY_PREFIX =
            "stable-resource-path-blob-module-identity-v1_7:";
    private static final String NEXT_RESOLUTION_STEP =
            "promote the stats-selected production bundle entry from staged execution-attempt to an owned module runtime that can execute side effects without relying on focused component entry imports";
    private static final String DEV_BUNDLE_WEBAPP_PREFIX = "vaadin-dev-bundle/webapp/";

    private FlowIpcProductionRuntimeAttributes() {
    }

    static Map<String, Object> runtimeAttributes(String statsSource, String selectedArtifact) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("productionBundleRuntimeVersion", VERSION);
        attributes.put("productionBundleRuntimeRegistrySource", REGISTRY_SOURCE);
        attributes.put("productionBundleRuntimeStatsSource", statsSource);
        attributes.put("productionBundleRuntimeSelectedArtifact", selectedArtifact);
        attributes.put("productionBundleRuntimeDependencyChain",
                dependencyChain(statsSource, selectedArtifact));
        attributes.put("productionBundleRuntimeModuleIdentity",
                moduleIdentity(selectedArtifact));
        attributes.put("productionBundleRuntimeBlockerVersion", BLOCKER);
        attributes.put("productionBundleRuntimeNextResolutionStep", NEXT_RESOLUTION_STEP);
        return attributes;
    }

    static String dependencyChain(String statsSource, String selectedArtifact) {
        return statsSource + "=>bundleImports=>" + bundleImportPath(selectedArtifact)
                + "=>" + selectedArtifact;
    }

    private static String moduleIdentity(String selectedArtifact) {
        return MODULE_IDENTITY_PREFIX + selectedArtifact;
    }

    private static String bundleImportPath(String selectedArtifact) {
        if (selectedArtifact.startsWith(DEV_BUNDLE_WEBAPP_PREFIX)) {
            return selectedArtifact.substring(DEV_BUNDLE_WEBAPP_PREFIX.length());
        }
        return selectedArtifact;
    }
}
