package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPreflightTraceFields.*;
import static dev.vatori.desktop.devtools.GeneratedLazyBuildTraceFields.*;
import static dev.vatori.desktop.devtools.ResourceStageTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildGeneratedStageTraceInspector {
    private FrontendBuildGeneratedStageTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (isResourceStage(line)
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsGeneratedLazySourceBoundaryV126Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedLazyBuild.sourceBoundaryV126StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsGeneratedLazyRealBuildProbeV127Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedLazyBuild.realBuildProbeV127StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsGeneratedLazyBuildLifecycleV128Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedLazyBuild.buildLifecycleV128StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsGeneratedLazyAppBuildOutputV129Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedLazyBuild.appBuildOutputV129StageObserved = true;
        }
        if (isResourceStage(line)
                && containsPath(line, "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                && containsCurrentAppFrontendBuildOutputV130Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.generatedLazyBuild.currentAppFrontendBuildOutputV130StageObserved = true;
        }
        if (isResourceStage(line)
                && (containsPath(line,
                        "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                        || containsPath(line,
                                "current-app-frontend/generated/flow/generated-flow-imports.js"))
                && containsFrontendBuildPreflightAvailabilityV131Fields(line)
                && hasExecutedStrictBlockerOrStaged(line)) {
            summary.frontendBuildPreflight.availabilityV131StageObserved = true;
        }
    }
}
