package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyAppBuildOutputTraceFields {
    private GeneratedLazyAppBuildOutputTraceFields() {
    }

    static boolean containsGeneratedLazyAppBuildOutputV129Fields(String line) {
        return containsStringField(line, "generatedLazyAppBuildOutputVersion",
                "flow-ipc-generated-lazy-app-build-output-v1_29")
                && containsStringField(line, "generatedLazyAppBuildOutputDiscoverySource",
                        "dev-ipc-module-target-generated-imports-discovery-v1_29")
                && containsStringField(line, "generatedLazyAppBuildOutputInputRoute",
                        "flow-ipc-lazy-probe-v1_27")
                && containsStringField(line, "generatedLazyAppBuildOutputInputRouteClass",
                        "dev.vatori.ipcsmoke.FlowIpcGeneratedLazyProbeView")
                && containsStringField(line, "generatedLazyAppBuildOutputInputDependency",
                        "context://vatori-flow-ipc-real-build-probe-v1_27.js")
                && containsLiteralField(line, "generatedLazyAppBuildOutputScannerObserved", "true")
                && containsGeneratedLazyAppBuildOutputRuntimeSource(line)
                && line.contains("generatedLazyAppBuildOutputModulePath")
                && line.contains("generatedLazyAppBuildOutputGeneratedImportsPath")
                && line.contains("generatedLazyAppBuildOutputGeneratedImportsObserved")
                && line.contains("generatedLazyAppBuildOutputGeneratedImportsMtimeMillis")
                && line.contains("generatedLazyAppBuildOutputGeneratedImportsBytes")
                && line.contains("generatedLazyAppBuildOutputGeneratedImportsSha256")
                && containsGeneratedLazyAppBuildOutputTableShape(line)
                && line.contains("generatedLazyAppBuildOutputGeneratedImportsNoOpLoadOnDemand")
                && line.contains("generatedLazyAppBuildOutputChunkReferences")
                && containsGeneratedLazyAppBuildOutputChunkStatus(line)
                && line.contains("generatedLazyAppBuildOutputChunkArtifact")
                && line.contains("generatedLazyAppBuildOutputFallbackReason")
                && line.contains("generatedLazyAppBuildOutputPackagedFallbackPath")
                && containsGeneratedLazyAppBuildOutputSpiAssessment(line)
                && containsGeneratedLazyAppBuildOutputFailureLayer(line)
                && line.contains("prepare-frontend")
                && containsLiteralField(line, "generatedLazyAppBuildOutputFixtureSubstituted", "false")
                && containsLiteralField(line, "generatedLazyAppBuildOutputNoLocalhost", "true");
    }

    static boolean containsGeneratedLazyAppBuildOutputRuntimeSource(String line) {
        return containsStringField(line, "generatedLazyAppBuildOutputRuntimeSource",
                "current-app-build-output")
                || containsStringField(line, "generatedLazyAppBuildOutputRuntimeSource",
                        "packaged-dev-bundle-fallback")
                || containsStringField(line, "generatedLazyAppBuildOutputRuntimeSource",
                        "missing-build-output");
    }

    static boolean containsGeneratedLazyAppBuildOutputTableShape(String line) {
        return containsStringField(line, "generatedLazyAppBuildOutputGeneratedImportsTableShape",
                "missing")
                || containsStringField(line, "generatedLazyAppBuildOutputGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                || containsStringField(line, "generatedLazyAppBuildOutputGeneratedImportsTableShape",
                        "branch-table-dynamic-import")
                || containsStringField(line, "generatedLazyAppBuildOutputGeneratedImportsTableShape",
                        "unrecognized");
    }

    static boolean containsGeneratedLazyAppBuildOutputChunkStatus(String line) {
        return containsStringField(line, "generatedLazyAppBuildOutputChunkStatus",
                "missing-build-output")
                || containsStringField(line, "generatedLazyAppBuildOutputChunkStatus",
                        "not-generated")
                || containsStringField(line, "generatedLazyAppBuildOutputChunkStatus",
                        "available")
                || containsStringField(line, "generatedLazyAppBuildOutputChunkStatus",
                        "not-found")
                || containsStringField(line, "generatedLazyAppBuildOutputChunkStatus",
                        "blocked");
    }

    static boolean containsGeneratedLazyAppBuildOutputSpiAssessment(String line) {
        return containsStringField(line, "generatedLazyAppBuildOutputSpiAssessment",
                "requires-build-step")
                || containsStringField(line, "generatedLazyAppBuildOutputSpiAssessment",
                        "requires-chunk-output-or-supported-spi");
    }

    static boolean containsGeneratedLazyAppBuildOutputFailureLayer(String line) {
        return containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                "lazy-route-probe-metadata-not-on-classpath")
                || containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                        "lazy-route-input-not-observed-by-scanner")
                || containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                        "app-build-output-missing-generated-imports")
                || containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                        "app-build-output-has-no-generated-lazy-chunk")
                || containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                        "app-build-output-generated-chunk-file-missing")
                || containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                        "pending-real-generated-chunk-stage");
    }
}
