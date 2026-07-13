package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildPipelineTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPreflightTraceFields.*;
import static dev.vatori.desktop.devtools.GeneratedLazyBuildTraceFields.*;

final class FrontendBuildPipelineManifestTraceInspector {
    private FrontendBuildPipelineManifestTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsFrontendPipelineClosureV124Fields(line)) {
            summary.frontendBuildPipeline.pipelineClosureV124ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsPackagedFrontendBaselineV125Fields(line)) {
            summary.frontendBuildPipeline.packagedFrontendBaselineV125ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGeneratedLazySourceBoundaryV126Fields(line)) {
            summary.generatedLazyBuild.sourceBoundaryV126ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
            summary.generatedLazyBuild.realBuildProbeV127ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
            summary.generatedLazyBuild.buildLifecycleV128ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsGeneratedLazyAppBuildOutputV129Fields(line)) {
            summary.generatedLazyBuild.appBuildOutputV129ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsCurrentAppFrontendBuildOutputV130Fields(line)) {
            summary.generatedLazyBuild.currentAppFrontendBuildOutputV130ManifestObserved = true;
        }
        if (line.contains("\"type\":\"bootstrapResponse\"")
                && line.contains("\"componentFrontendResources\"")
                && containsFrontendBuildPreflightAvailabilityV131Fields(line)) {
            summary.frontendBuildPreflight.availabilityV131ManifestObserved = true;
        }
    }
}
