package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildPreflightTraceFields {
    boolean availabilityV131ManifestObserved;
    boolean availabilityV131RequestObserved;
    boolean availabilityV131ResponseObserved;
    boolean availabilityV131StageObserved;
    boolean availabilityV131DomObserved;
    boolean availabilityV131ResolvedObserved;

    boolean hasAvailabilityV1_31Signals() {
        return availabilityV131ManifestObserved
                && availabilityV131RequestObserved
                && availabilityV131ResponseObserved
                && availabilityV131StageObserved
                && availabilityV131DomObserved
                && availabilityV131ResolvedObserved;
    }

    static boolean containsCurrentAppFrontendBuildOutputV130Fields(String line) {
        return containsStringField(line, "currentAppFrontendBuildOutputVersion",
                "flow-ipc-current-app-frontend-build-output-v1_30")
                && containsStringField(line, "currentAppFrontendBuildOutputDiscoverySource",
                        "dev-ipc-current-app-frontend-build-output-materialization-v1_30")
                && line.contains("currentAppFrontendBuildStepCommand")
                && containsCurrentAppFrontendBuildStepStatus(line)
                && line.contains("currentAppFrontendBuildStepExitCode")
                && line.contains("currentAppFrontendBuildStepDurationMillis")
                && line.contains("currentAppFrontendBuildOutputRoots")
                && line.contains("currentAppFrontendBuildOutputRoot")
                && line.contains("currentAppFrontendBuildModulePath")
                && containsCurrentAppFrontendBuildRuntimeSource(line)
                && line.contains("currentAppFrontendBuildGeneratedImportsPath")
                && line.contains("currentAppFrontendBuildGeneratedImportsObserved")
                && line.contains("currentAppFrontendBuildGeneratedImportsMtimeMillis")
                && line.contains("currentAppFrontendBuildGeneratedImportsBytes")
                && line.contains("currentAppFrontendBuildGeneratedImportsSha256")
                && containsCurrentAppFrontendBuildTableShape(line)
                && line.contains("currentAppFrontendBuildGeneratedImportsNoOpLoadOnDemand")
                && line.contains("currentAppFrontendBuildChunkReferences")
                && containsCurrentAppFrontendBuildChunkStatus(line)
                && line.contains("currentAppFrontendBuildChunkArtifact")
                && containsCurrentAppFrontendBuildFailureLayer(line)
                && line.contains("Vaadin frontend build")
                && containsLiteralField(line, "currentAppFrontendBuildFixtureSubstituted", "false")
                && containsLiteralField(line, "currentAppFrontendBuildNoLocalhost", "true");
    }

    static boolean containsCurrentAppFrontendBuildStepStatus(String line) {
        return containsStringField(line, "currentAppFrontendBuildStepStatus", "skipped")
                || containsCurrentAppFrontendBuildStepAttempted(line);
    }

    static boolean containsCurrentAppFrontendBuildStepAttempted(String line) {
        return containsStringField(line, "currentAppFrontendBuildStepStatus", "success")
                || containsStringField(line, "currentAppFrontendBuildStepStatus", "failed")
                || containsStringField(line, "currentAppFrontendBuildStepStatus", "unavailable");
    }

    static boolean containsCurrentAppFrontendBuildRuntimeSource(String line) {
        return containsStringField(line, "currentAppFrontendBuildOutputRuntimeSource",
                "current-app-build-output")
                || containsStringField(line, "currentAppFrontendBuildOutputRuntimeSource",
                        "current-app-build-output-incomplete")
                || containsStringField(line, "currentAppFrontendBuildOutputRuntimeSource",
                        "current-app-build-output-no-generated-lazy-chunk")
                || containsStringField(line, "currentAppFrontendBuildOutputRuntimeSource",
                        "packaged-dev-bundle-fallback")
                || containsStringField(line, "currentAppFrontendBuildOutputRuntimeSource",
                        "build-step-unavailable");
    }

    static boolean containsCurrentAppFrontendBuildTableShape(String line) {
        return containsStringField(line, "currentAppFrontendBuildGeneratedImportsTableShape",
                "missing")
                || containsStringField(line, "currentAppFrontendBuildGeneratedImportsTableShape",
                        "no-op-promise-resolve-0")
                || containsStringField(line, "currentAppFrontendBuildGeneratedImportsTableShape",
                        "branch-table-dynamic-import")
                || containsStringField(line, "currentAppFrontendBuildGeneratedImportsTableShape",
                        "unrecognized")
                || containsStringField(line, "currentAppFrontendBuildGeneratedImportsTableShape",
                        "unreadable");
    }

    static boolean containsCurrentAppFrontendBuildChunkStatus(String line) {
        return containsStringField(line, "currentAppFrontendBuildChunkStatus",
                "missing-build-output")
                || containsStringField(line, "currentAppFrontendBuildChunkStatus",
                        "not-generated")
                || containsStringField(line, "currentAppFrontendBuildChunkStatus",
                        "available")
                || containsStringField(line, "currentAppFrontendBuildChunkStatus",
                        "not-found")
                || containsStringField(line, "currentAppFrontendBuildChunkStatus",
                        "blocked");
    }

    static boolean containsCurrentAppFrontendBuildFailureLayer(String line) {
        return containsStringField(line, "currentAppFrontendBuildFailureLayer",
                "current-app-frontend-build-step-not-enabled")
                || containsStringField(line, "currentAppFrontendBuildFailureLayer",
                        "vaadin-frontend-build-step-unavailable")
                || containsStringField(line, "currentAppFrontendBuildFailureLayer",
                        "vaadin-frontend-build-step-output-incomplete")
                || containsStringField(line, "currentAppFrontendBuildFailureLayer",
                        "current-app-build-output-has-no-generated-lazy-chunk")
                || containsStringField(line, "currentAppFrontendBuildFailureLayer",
                        "current-app-build-output-generated-chunk-file-missing")
                || containsStringField(line, "currentAppFrontendBuildFailureLayer",
                        "pending-real-generated-chunk-stage");
    }

    static boolean containsFrontendBuildPreflightAvailabilityV131Fields(String line) {
        return containsStringField(line, "frontendBuildPreflightAvailabilityVersion",
                "flow-ipc-frontend-build-preflight-availability-v1_31")
                && containsStringField(line, "frontendBuildPreflightAvailabilitySource",
                        "dev-ipc-vaadin-maven-plugin-availability-v1_31")
                && containsFrontendBuildPreflightAvailabilityMode(line)
                && containsFrontendBuildPreflightMavenMode(line)
                && line.contains("frontendBuildPreflightPluginCoordinates")
                && containsFrontendBuildPreflightPluginStatus(line)
                && containsFrontendBuildPreflightGoalResult(line)
                && containsFrontendBuildPreflightOutputUsability(line)
                && containsCurrentAppFrontendBuildRuntimeSource(line.replace(
                        "frontendBuildPreflightRuntimeSourceDecision",
                        "currentAppFrontendBuildOutputRuntimeSource"))
                && containsFrontendBuildPreflightFailureLayer(line)
                && line.contains("Vaadin frontend build")
                && line.contains("frontendBuildPreflightBuildStepUnavailableCleared")
                && containsLiteralField(line, "frontendBuildPreflightNoLocalhost", "true");
    }

    static boolean containsFrontendBuildPreflightAvailabilityMode(String line) {
        return containsStringField(line, "frontendBuildPreflightAvailabilityMode", "offline-default")
                || containsStringField(line, "frontendBuildPreflightAvailabilityMode", "forced-offline")
                || containsStringField(line, "frontendBuildPreflightAvailabilityMode",
                        "explicit-online-local-repo");
    }

    static boolean containsFrontendBuildPreflightMavenMode(String line) {
        return containsStringField(line, "frontendBuildPreflightEffectiveMavenMode", "-o")
                || containsStringField(line, "frontendBuildPreflightEffectiveMavenMode", "-U");
    }

    static boolean containsFrontendBuildPreflightPluginStatus(String line) {
        return containsStringField(line, "frontendBuildPreflightPluginResolutionStatus", "not-attempted")
                || containsStringField(line, "frontendBuildPreflightPluginResolutionStatus",
                        "version-unavailable")
                || containsStringField(line, "frontendBuildPreflightPluginResolutionStatus", "missing")
                || containsStringField(line, "frontendBuildPreflightPluginResolutionStatus", "resolved")
                || containsStringField(line, "frontendBuildPreflightPluginResolutionStatus", "unknown");
    }

    static boolean containsFrontendBuildPreflightGoalResult(String line) {
        return containsStringField(line, "frontendBuildPreflightMavenGoalResult", "skipped")
                || containsStringField(line, "frontendBuildPreflightMavenGoalResult", "version-unavailable")
                || containsStringField(line, "frontendBuildPreflightMavenGoalResult", "plugin-unavailable")
                || containsStringField(line, "frontendBuildPreflightMavenGoalResult", "success")
                || containsStringField(line, "frontendBuildPreflightMavenGoalResult", "failed");
    }

    static boolean containsFrontendBuildPreflightOutputUsability(String line) {
        return containsStringField(line, "frontendBuildPreflightOutputUsability", "not-inspected")
                || containsStringField(line, "frontendBuildPreflightOutputUsability", "output-missing")
                || containsStringField(line, "frontendBuildPreflightOutputUsability", "output-incomplete")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "prepare-frontend-generated-scaffold-no-generated-imports")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "generated-imports-no-chunk")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "usable-generated-chunk")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "source-generated-imports-and-chunk")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "source-generated-imports-no-chunk")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "production-generated-imports-bundled")
                || containsStringField(line, "frontendBuildPreflightOutputUsability",
                        "production-bundle-only");
    }

    static boolean containsFrontendBuildPreflightFailureLayer(String line) {
        return containsStringField(line, "frontendBuildPreflightFailureLayer",
                "frontend-build-preflight-not-enabled")
                || containsStringField(line, "frontendBuildPreflightFailureLayer",
                        "frontend-build-plugin-unavailable")
                || containsStringField(line, "frontendBuildPreflightFailureLayer",
                        "frontend-build-maven-goal-failed-before-output")
                || containsResolvedFrontendBuildPreflightFailureLayer(line)
                || containsStringField(line, "frontendBuildPreflightFailureLayer",
                        "frontend-build-preflight-unknown");
    }

    static boolean containsResolvedFrontendBuildPreflightFailureLayer(String line) {
        return containsStringField(line, "frontendBuildPreflightFailureLayer",
                "frontend-build-plugin-resolved-output-incomplete")
                || containsStringField(line, "frontendBuildPreflightFailureLayer",
                        "current-app-build-output-has-no-generated-lazy-chunk")
                || containsStringField(line, "frontendBuildPreflightFailureLayer",
                        "current-app-build-output-generated-chunk-file-missing")
                || containsStringField(line, "frontendBuildPreflightFailureLayer",
                        "current-app-build-output-generated-chunk-available-pending-stage");
    }
}
