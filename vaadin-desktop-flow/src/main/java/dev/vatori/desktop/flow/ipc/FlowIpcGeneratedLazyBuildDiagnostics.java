package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

final class FlowIpcGeneratedLazyBuildDiagnostics {
    private static final String REAL_BUILD_PROBE_VERSION =
            "flow-ipc-generated-lazy-real-build-probe-v1_27";
    private static final String REAL_BUILD_PROBE_PROPERTIES =
            "META-INF/vatori/generated-lazy-real-build-probe-v1_27.properties";
    private static final String REAL_BUILD_DEFAULT_ROUTE_CLASS =
            "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView";
    private static final String REAL_BUILD_DEFAULT_ROUTE =
            "flow-ipc-lazy-probe-v1_27";
    private static final String REAL_BUILD_DEFAULT_DEPENDENCY =
            "context://vatori-flow-ipc-real-build-probe-v1_27.js";
    private static final String REAL_BUILD_SCANNER_SOURCE =
            "route-annotation-classpath-probe-v1_27";
    private static final String REAL_BUILD_FAILURE_LAYER =
            "scanner-output-not-connected-to-dev-bundle-generated-imports";
    private static final String REAL_BUILD_NEXT_STEP =
            "run a Vaadin frontend build/update-imports path that includes the non-eager v1.27 route input and compare generated-flow-imports output";
    static final String BUILD_LIFECYCLE_VERSION =
            "flow-ipc-generated-lazy-build-lifecycle-v1_28";
    private static final String BUILD_LIFECYCLE_SCANNER_SOURCE =
            "route-annotation-classpath-probe-plus-vaadin-FrontendDependencies-contract-v1_28";
    private static final String BUILD_LIFECYCLE_BUILD_TASK_SOURCE =
            "dev-ipc-runtime-classpath-resource-resolver-v1_28";
    private static final String BUILD_LIFECYCLE_FAILURE_LAYER =
            "runtime-using-stale-packaged-dev-bundle";
    private static final String BUILD_LIFECYCLE_NEXT_STEP =
            "connect dev-ipc resource resolution to a Vaadin prepare-frontend/update-imports output for the v1.27 route, or replace the static dev-bundle artifact with a supported generated lazy chunk build SPI";

    private FlowIpcGeneratedLazyBuildDiagnostics() {
    }

    static Map<String, Object> realBuildProbeAttributes(String generatedImportsPath) {
        ProbeEvidence probe = probeEvidence();
        GeneratedLoadOnDemandChunkFileRegistry registry =
                GeneratedLoadOnDemandChunkFileRegistry.parse(generatedImportsPath,
                        FlowIpcClasspathResources.text(generatedImportsPath));
        boolean branchTable = "branch-table-dynamic-import".equals(registry.loadOnDemandTableShape());
        String failureLayer;
        if (branchTable) {
            failureLayer = "pending-real-generated-chunk-stage";
        } else if (!probe.manifestObserved()) {
            failureLayer = "lazy-route-probe-metadata-not-on-classpath";
        } else if (!probe.routeObserved()) {
            failureLayer = "lazy-route-input-class-not-on-classpath";
        } else if (!probe.lazyJavascriptObserved()) {
            failureLayer = "lazy-route-input-no-lazy-javascript";
        } else {
            failureLayer = REAL_BUILD_FAILURE_LAYER;
        }

        ProbeInput input = probe.input();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazyRealBuildProbeVersion", REAL_BUILD_PROBE_VERSION);
        attributes.put("generatedLazyRealBuildProbeManifest", REAL_BUILD_PROBE_PROPERTIES);
        attributes.put("generatedLazyRealBuildProbeManifestObserved", probe.manifestObserved());
        attributes.put("generatedLazyRealBuildInputRoute", input.route());
        attributes.put("generatedLazyRealBuildInputRouteClass", input.routeClass());
        attributes.put("generatedLazyRealBuildInputTrigger", input.trigger());
        attributes.put("generatedLazyRealBuildInputDependency", input.dependency());
        attributes.put("generatedLazyRealBuildInputEager", false);
        attributes.put("generatedLazyRealBuildInputRouteObserved", probe.routeObserved());
        attributes.put("generatedLazyRealBuildInputRouteMatches", probe.routeMatches());
        attributes.put("generatedLazyRealBuildScannerSource", REAL_BUILD_SCANNER_SOURCE);
        attributes.put("generatedLazyRealBuildScannerObserved", probe.scannerObserved());
        attributes.put("generatedLazyRealBuildLazyJavascriptObserved", probe.lazyJavascriptObserved());
        attributes.put("generatedLazyRealBuildGeneratedImportsArtifact", generatedImportsPath);
        attributes.put("generatedLazyRealBuildGeneratedImportsTableShape", registry.loadOnDemandTableShape());
        attributes.put("generatedLazyRealBuildNoOpLoadOnDemand", registry.noOpLoadOnDemand());
        attributes.put("generatedLazyRealBuildChunkStatus", registry.chunkFileStatus());
        attributes.put("generatedLazyRealBuildChunkArtifact", registry.chunkArtifactPath());
        attributes.put("generatedLazyRealBuildFailureLayer", failureLayer);
        attributes.put("generatedLazyRealBuildNextResolutionStep", REAL_BUILD_NEXT_STEP);
        attributes.put("generatedLazyRealBuildFixtureSubstituted", false);
        attributes.put("generatedLazyRealBuildNoLocalhost", true);
        return attributes;
    }

    static Map<String, Object> buildLifecycleAttributes(String generatedImportsPath) {
        ProbeEvidence probe = probeEvidence();
        byte[] generatedImportsBytes = FlowIpcClasspathResources.bytes(generatedImportsPath);
        String generatedImportsBody = new String(generatedImportsBytes, StandardCharsets.UTF_8);
        GeneratedLoadOnDemandChunkFileRegistry registry =
                GeneratedLoadOnDemandChunkFileRegistry.parse(generatedImportsPath,
                        generatedImportsBody);
        FlowIpcClasspathResources.ArtifactEvidence generatedImportsEvidence =
                FlowIpcClasspathResources.artifactEvidence(generatedImportsPath);
        FlowIpcBuildOutputScanner.WriteTargetEvidence writeTarget =
                FlowIpcBuildOutputScanner.writeTargetEvidence(generatedImportsPath);
        boolean branchTable = "branch-table-dynamic-import".equals(registry.loadOnDemandTableShape());
        boolean updateImportsObserved = writeTarget.observed()
                && "current-build".equals(generatedImportsEvidence.source());
        boolean scannerObserved = probe.manifestObserved() && probe.scannerObserved();

        String failureLayer;
        if (!probe.manifestObserved()) {
            failureLayer = "lazy-route-probe-metadata-not-on-classpath";
        } else if (!probe.routeObserved()) {
            failureLayer = "lazy-route-not-in-route-registry";
        } else if (!probe.lazyJavascriptObserved()) {
            failureLayer = "lazy-route-input-no-lazy-javascript";
        } else if ("static-dev-bundle".equals(generatedImportsEvidence.source())) {
            failureLayer = BUILD_LIFECYCLE_FAILURE_LAYER;
        } else if (!updateImportsObserved) {
            failureLayer = "update-imports-not-run-for-ipc-runtime";
        } else if (!branchTable) {
            failureLayer = "vaadin-build-does-not-emit-chunk-for-loadmode-lazy-route-probe";
        } else {
            failureLayer = "pending-real-generated-chunk-stage";
        }

        ProbeInput input = probe.input();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("generatedLazyBuildLifecycleVersion", BUILD_LIFECYCLE_VERSION);
        attributes.put("generatedLazyBuildLifecycleInputRoute", input.route());
        attributes.put("generatedLazyBuildLifecycleInputRouteClass", input.routeClass());
        attributes.put("generatedLazyBuildLifecycleInputDependency", input.dependency());
        attributes.put("generatedLazyBuildLifecycleInputEager", false);
        attributes.put("generatedLazyBuildLifecycleScannerSource",
                BUILD_LIFECYCLE_SCANNER_SOURCE);
        attributes.put("generatedLazyBuildLifecycleScannerStatus",
                scannerObserved ? "observed" : "missing");
        attributes.put("generatedLazyBuildLifecycleScannerObserved", scannerObserved);
        attributes.put("generatedLazyBuildLifecycleLazyJavascriptObserved",
                probe.lazyJavascriptObserved());
        attributes.put("generatedLazyBuildLifecycleLazyRouteChunkInput",
                "ChunkInfo(type=ROUTE,eager=false,trigger=" + input.routeClass()
                        + ",scripts=1)");
        attributes.put("generatedLazyBuildLifecycleBuildTaskSource",
                BUILD_LIFECYCLE_BUILD_TASK_SOURCE);
        attributes.put("generatedLazyBuildLifecycleUpdateImportsObserved", updateImportsObserved);
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsSource",
                generatedImportsEvidence.source());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsRuntimePath",
                generatedImportsEvidence.runtimePath());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsProtocol",
                generatedImportsEvidence.protocol());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsMtimeMillis",
                generatedImportsEvidence.lastModifiedMillis());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsSha256",
                FlowIpcDiagnostics.sha256(generatedImportsBytes));
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsBytes",
                generatedImportsBytes.length);
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsTableShape",
                registry.loadOnDemandTableShape());
        attributes.put("generatedLazyBuildLifecycleGeneratedImportsNoOpLoadOnDemand",
                registry.noOpLoadOnDemand());
        attributes.put("generatedLazyBuildLifecycleWriteTarget", writeTarget.path());
        attributes.put("generatedLazyBuildLifecycleWriteTargetObserved", writeTarget.observed());
        attributes.put("generatedLazyBuildLifecycleWriteTargetMtimeMillis",
                writeTarget.lastModifiedMillis());
        attributes.put("generatedLazyBuildLifecycleRuntimeUsesStaticDevBundle",
                "static-dev-bundle".equals(generatedImportsEvidence.source()));
        attributes.put("generatedLazyBuildLifecycleRuntimeProbeMismatch",
                scannerObserved && "static-dev-bundle".equals(generatedImportsEvidence.source()));
        attributes.put("generatedLazyBuildLifecycleChunkStatus", registry.chunkFileStatus());
        attributes.put("generatedLazyBuildLifecycleChunkArtifact", registry.chunkArtifactPath());
        attributes.put("generatedLazyBuildLifecycleFailureLayer", failureLayer);
        attributes.put("generatedLazyBuildLifecycleNextResolutionStep",
                BUILD_LIFECYCLE_NEXT_STEP);
        attributes.put("generatedLazyBuildLifecycleFixtureSubstituted", false);
        attributes.put("generatedLazyBuildLifecycleNoLocalhost", true);
        return attributes;
    }

    static ProbeEvidence probeEvidence() {
        Properties properties = probeProperties();
        ProbeInput input = new ProbeInput(
                propertyOrDefault(properties, "routeClass", REAL_BUILD_DEFAULT_ROUTE_CLASS),
                propertyOrDefault(properties, "route", REAL_BUILD_DEFAULT_ROUTE),
                propertyOrDefault(properties, "trigger", REAL_BUILD_DEFAULT_ROUTE_CLASS),
                propertyOrDefault(properties, "dependency", REAL_BUILD_DEFAULT_DEPENDENCY));
        Class<?> probeClass = loadOptionalClass(input.routeClass());
        Route route = probeClass == null ? null : probeClass.getAnnotation(Route.class);
        JavaScript[] scripts = probeClass == null ? new JavaScript[0]
                : probeClass.getAnnotationsByType(JavaScript.class);
        boolean routeObserved = route != null;
        boolean routeMatches = routeObserved && input.route().equals(route.value());
        boolean lazyJavascriptObserved = false;
        for (JavaScript script : scripts) {
            if ("LAZY".equals(script.loadMode().name())
                    && input.dependency().equals(script.value())) {
                lazyJavascriptObserved = true;
                break;
            }
        }
        return new ProbeEvidence(input,
                !properties.isEmpty(),
                routeObserved,
                routeMatches,
                lazyJavascriptObserved,
                routeMatches && lazyJavascriptObserved);
    }

    private static Properties probeProperties() {
        Properties properties = new Properties();
        String text = FlowIpcClasspathResources.text(REAL_BUILD_PROBE_PROPERTIES);
        if (text.isBlank()) {
            return properties;
        }
        try {
            properties.load(new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read generated lazy real build probe metadata", error);
        }
    }

    private static String propertyOrDefault(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static Class<?> loadOptionalClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FlowIpcGeneratedLazyBuildDiagnostics.class.getClassLoader();
        }
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    record ProbeInput(String routeClass, String route, String trigger, String dependency) {
    }

    record ProbeEvidence(
            ProbeInput input,
            boolean manifestObserved,
            boolean routeObserved,
            boolean routeMatches,
            boolean lazyJavascriptObserved,
            boolean scannerObserved) {
    }
}
