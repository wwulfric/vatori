package dev.vatori.desktop.flow.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

final class FlowIpcGeneratedLazyAppBuildOutputAttributes {
    private FlowIpcGeneratedLazyAppBuildOutputAttributes() {
    }

    static Map<String, Object> attributes(Config config) {
        FlowIpcGeneratedLazyBuildDiagnostics.ProbeEvidence probe =
                FlowIpcGeneratedLazyBuildDiagnostics.probeEvidence();
        FlowIpcGeneratedLazyBuildDiagnostics.ProbeInput probeInput = probe.input();

        AppBuildGeneratedImportsEvidence appOutput =
                FlowIpcFrontendBuildEvidence.appGeneratedImports(
                        config.devBundleGeneratedFlowImportsPath());
        FlowIpcClasspathResources.ArtifactEvidence fallbackEvidence =
                FlowIpcClasspathResources.artifactEvidence(
                        config.devBundleGeneratedFlowImportsPath());
        boolean usesFallback = !appOutput.observed()
                && "static-dev-bundle".equals(fallbackEvidence.source());
        String runtimeSource = appOutput.observed()
                ? "current-app-build-output"
                : usesFallback ? "packaged-dev-bundle-fallback" : "missing-build-output";

        String failureLayer;
        if (!probe.manifestObserved()) {
            failureLayer = "lazy-route-probe-metadata-not-on-classpath";
        } else if (!probe.scannerObserved()) {
            failureLayer = "lazy-route-input-not-observed-by-scanner";
        } else if (!appOutput.observed()) {
            failureLayer = config.failureLayer();
        } else if ("available".equals(appOutput.chunkStatus())) {
            failureLayer = "pending-real-generated-chunk-stage";
        } else if ("branch-table-dynamic-import".equals(appOutput.tableShape())) {
            failureLayer = "app-build-output-generated-chunk-file-missing";
        } else {
            failureLayer = "app-build-output-has-no-generated-lazy-chunk";
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazyAppBuildOutputVersion", config.version());
        attributes.put("generatedLazyAppBuildOutputDiscoverySource", config.discoverySource());
        attributes.put("generatedLazyAppBuildOutputInputRoute", probeInput.route());
        attributes.put("generatedLazyAppBuildOutputInputRouteClass", probeInput.routeClass());
        attributes.put("generatedLazyAppBuildOutputInputDependency", probeInput.dependency());
        attributes.put("generatedLazyAppBuildOutputScannerObserved", probe.scannerObserved());
        attributes.put("generatedLazyAppBuildOutputRuntimeSource", runtimeSource);
        attributes.put("generatedLazyAppBuildOutputModulePath", appOutput.modulePath());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsPath", appOutput.path());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsObserved", appOutput.observed());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsMtimeMillis",
                appOutput.lastModifiedMillis());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsBytes", appOutput.bytes());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsSha256", appOutput.sha256());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsTableShape", appOutput.tableShape());
        attributes.put("generatedLazyAppBuildOutputGeneratedImportsNoOpLoadOnDemand",
                appOutput.noOpLoadOnDemand());
        attributes.put("generatedLazyAppBuildOutputChunkReferences", appOutput.chunkReferences());
        attributes.put("generatedLazyAppBuildOutputChunkStatus", appOutput.chunkStatus());
        attributes.put("generatedLazyAppBuildOutputChunkArtifact", appOutput.chunkArtifact());
        attributes.put("generatedLazyAppBuildOutputFallbackReason",
                usesFallback ? "current-app-build-output-generated-imports-missing" : "");
        attributes.put("generatedLazyAppBuildOutputPackagedFallbackPath",
                fallbackEvidence.runtimePath());
        attributes.put("generatedLazyAppBuildOutputSpiAssessment",
                appOutput.observed() ? "requires-chunk-output-or-supported-spi" : "requires-build-step");
        attributes.put("generatedLazyAppBuildOutputFailureLayer", failureLayer);
        attributes.put("generatedLazyAppBuildOutputNextResolutionStep", config.nextResolutionStep());
        attributes.put("generatedLazyAppBuildOutputFixtureSubstituted", false);
        attributes.put("generatedLazyAppBuildOutputNoLocalhost", true);
        return attributes;
    }

    record Config(
            String version,
            String discoverySource,
            String failureLayer,
            String nextResolutionStep,
            String devBundleGeneratedFlowImportsPath) {
    }
}
