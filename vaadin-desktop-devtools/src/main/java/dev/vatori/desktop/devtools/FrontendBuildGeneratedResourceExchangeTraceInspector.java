package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPreflightTraceFields.*;
import static dev.vatori.desktop.devtools.GeneratedLazyBuildTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildGeneratedResourceExchangeTraceInspector {
    private static final String GENERATED_IMPORTS_ARTIFACT =
            "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js";

    private FrontendBuildGeneratedResourceExchangeTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isGeneratedImportsRequest(line)
                && containsGeneratedLazySourceBoundaryV126Fields(line)) {
            summary.generatedLazyBuild.sourceBoundaryV126RequestObserved = true;
        }
        if (isGeneratedImportsRedactedResponse(line)
                && containsGeneratedLazySourceBoundaryV126Fields(line)) {
            summary.generatedLazyBuild.sourceBoundaryV126ResponseObserved = true;
        }
        if (isGeneratedImportsRequest(line)
                && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
            summary.generatedLazyBuild.realBuildProbeV127RequestObserved = true;
        }
        if (isGeneratedImportsRedactedResponse(line)
                && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
            summary.generatedLazyBuild.realBuildProbeV127ResponseObserved = true;
        }
        if (isGeneratedImportsRequest(line)
                && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
            summary.generatedLazyBuild.buildLifecycleV128RequestObserved = true;
        }
        if (isGeneratedImportsRedactedResponse(line)
                && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
            summary.generatedLazyBuild.buildLifecycleV128ResponseObserved = true;
        }
        if (isGeneratedImportsRequest(line)
                && containsGeneratedLazyAppBuildOutputV129Fields(line)) {
            summary.generatedLazyBuild.appBuildOutputV129RequestObserved = true;
        }
        if (isGeneratedImportsRedactedResponse(line)
                && containsGeneratedLazyAppBuildOutputV129Fields(line)) {
            summary.generatedLazyBuild.appBuildOutputV129ResponseObserved = true;
        }
        if (isGeneratedImportsRequest(line)
                && containsCurrentAppFrontendBuildOutputV130Fields(line)) {
            summary.generatedLazyBuild.currentAppFrontendBuildOutputV130RequestObserved = true;
        }
        if (isGeneratedImportsRedactedResponse(line)
                && containsCurrentAppFrontendBuildOutputV130Fields(line)) {
            summary.generatedLazyBuild.currentAppFrontendBuildOutputV130ResponseObserved = true;
        }
        if (isGeneratedImportsRequest(line)
                && containsFrontendBuildPreflightAvailabilityV131Fields(line)) {
            summary.frontendBuildPreflight.availabilityV131RequestObserved = true;
        }
        if (isGeneratedImportsRedactedResponse(line)
                && containsFrontendBuildPreflightAvailabilityV131Fields(line)) {
            summary.frontendBuildPreflight.availabilityV131ResponseObserved = true;
        }
    }

    private static boolean isGeneratedImportsRequest(String line) {
        return isResourceRequest(line)
                && containsPath(line, GENERATED_IMPORTS_ARTIFACT);
    }

    private static boolean isGeneratedImportsRedactedResponse(String line) {
        return isRedactedResourceResponse(line)
                && containsPath(line, GENERATED_IMPORTS_ARTIFACT);
    }
}
