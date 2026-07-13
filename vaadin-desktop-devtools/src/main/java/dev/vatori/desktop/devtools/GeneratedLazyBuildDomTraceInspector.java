package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildPreflightTraceFields.*;
import static dev.vatori.desktop.devtools.GeneratedLazyBuildTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class GeneratedLazyBuildDomTraceInspector {
    private GeneratedLazyBuildDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazySourceBoundaryObserved", "true")
                && containsGeneratedLazySourceBoundaryV126Fields(line)) {
            summary.generatedLazyBuild.sourceBoundaryV126DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyRealBuildProbeObserved", "true")
                && containsGeneratedLazyRealBuildProbeV127Fields(line)) {
            summary.generatedLazyBuild.realBuildProbeV127DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyBuildLifecycleObserved", "true")
                && containsGeneratedLazyBuildLifecycleV128Fields(line)) {
            summary.generatedLazyBuild.buildLifecycleV128DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyAppBuildOutputObserved", "true")
                && containsGeneratedLazyAppBuildOutputV129Fields(line)) {
            summary.generatedLazyBuild.appBuildOutputV129DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppFrontendBuildOutputObserved", "true")
                && containsCurrentAppFrontendBuildOutputV130Fields(line)) {
            summary.generatedLazyBuild.currentAppFrontendBuildOutputV130DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frontendBuildPreflightAvailabilityObserved", "true")
                && containsFrontendBuildPreflightAvailabilityV131Fields(line)) {
            summary.frontendBuildPreflight.availabilityV131DomObserved = true;
        }
    }
}
