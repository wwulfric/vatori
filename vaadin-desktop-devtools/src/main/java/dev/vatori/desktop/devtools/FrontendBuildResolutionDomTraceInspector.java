package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPreflightTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildResolutionDomTraceInspector {
    private FrontendBuildResolutionDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazySourceCurrentBuildNoOpConfirmed", "true")
                && containsStringField(line, "generatedLazySourceCurrentBuildTableShape",
                        "no-op-promise-resolve-0")
                && containsStringField(line, "generatedLazySourceCurrentBuildChunkStatus",
                        "not-generated")
                && containsStringField(line, "generatedLazySourceRemainingBlocker",
                        "real-app-generated-chunk-file-not-produced-by-current-build")
                && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                        "chunk-file-not-generated")) {
            summary.generatedLazyBuild.sourceBoundaryV126CurrentBuildBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyRealBuildProbeManifestObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildInputRouteObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildInputRouteMatches", "true")
                && containsLiteralField(line, "generatedLazyRealBuildScannerObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildLazyJavascriptObserved", "true")
                && containsLiteralField(line, "generatedLazyRealBuildNoOpLoadOnDemand", "true")
                && containsStringField(line, "generatedLazyRealBuildFailureLayer",
                        "scanner-output-not-connected-to-dev-bundle-generated-imports")
                && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                        "chunk-file-not-generated")) {
            summary.generatedLazyBuild.realBuildProbeV127CurrentBuildBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyBuildLifecycleScannerObserved", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleLazyJavascriptObserved", "true")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsSource",
                        "static-dev-bundle")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsProtocol",
                        "jar")
                && containsStringField(line, "generatedLazyBuildLifecycleGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                && containsLiteralField(line,
                        "generatedLazyBuildLifecycleGeneratedImportsNoOpLoadOnDemand", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleUpdateImportsObserved", "false")
                && containsLiteralField(line, "generatedLazyBuildLifecycleWriteTargetObserved", "false")
                && containsLiteralField(line,
                        "generatedLazyBuildLifecycleRuntimeUsesStaticDevBundle", "true")
                && containsLiteralField(line, "generatedLazyBuildLifecycleRuntimeProbeMismatch", "true")
                && containsStringField(line, "generatedLazyBuildLifecycleFailureLayer",
                        "runtime-using-stale-packaged-dev-bundle")
                && containsStringField(line, "loadOnDemandChunkFileFailureLayer",
                        "chunk-file-not-generated")) {
            summary.generatedLazyBuild.buildLifecycleV128CurrentBuildBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "generatedLazyAppBuildOutputScannerObserved", "true")
                && containsStringField(line, "generatedLazyAppBuildOutputRuntimeSource",
                        "packaged-dev-bundle-fallback")
                && containsLiteralField(line, "generatedLazyAppBuildOutputGeneratedImportsObserved", "false")
                && containsStringField(line, "generatedLazyAppBuildOutputGeneratedImportsTableShape",
                        "missing")
                && containsStringField(line, "generatedLazyAppBuildOutputChunkStatus",
                        "missing-build-output")
                && containsStringField(line, "generatedLazyAppBuildOutputFailureLayer",
                        "app-build-output-missing-generated-imports")
                && containsStringField(line, "generatedLazyAppBuildOutputFallbackReason",
                        "current-app-build-output-generated-imports-missing")) {
            summary.generatedLazyBuild.appBuildOutputV129CurrentBuildBlockerObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppFrontendBuildOutputV130Fields(line)
                && containsCurrentAppFrontendBuildStepAttempted(line)
                && containsCurrentAppFrontendBuildRuntimeSource(line)
                && containsCurrentAppFrontendBuildFailureLayer(line)) {
            summary.generatedLazyBuild.currentAppFrontendBuildOutputV130BuildStepObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsFrontendBuildPreflightAvailabilityV131Fields(line)
                && containsLiteralField(line, "frontendBuildPreflightBuildStepUnavailableCleared", "true")
                && containsResolvedFrontendBuildPreflightFailureLayer(line)) {
            summary.frontendBuildPreflight.availabilityV131ResolvedObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppBuildFrontendOutputV132Fields(line)
                && containsLiteralField(line, "currentAppBuildFrontendSourceGeneratedImportsObserved", "true")
                && containsLiteralField(line, "currentAppBuildFrontendProductionBuildObserved", "true")
                && containsLiteralField(line, "currentAppBuildFrontendProdBundleObserved", "true")
                && containsStringField(line, "currentAppBuildFrontendSourceChunkStatus", "available")) {
            summary.currentAppBuildFrontend.outputV132ResolvedObserved = true;
        }
    }
}
