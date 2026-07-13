package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildGeneratedImportsOwnerRuntimeContractTraceFields {
    private FrontendBuildGeneratedImportsOwnerRuntimeContractTraceFields() {
    }

    static boolean containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(String line) {
        return containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractVersion",
                        "flow-ipc-current-app-generated-imports-owner-runtime-contract-v1_42")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractSource",
                        "current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractArtifactSource",
                        "current-app-production-build-frontend-generated-imports-support-artifact")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifactSha256")
                && line.contains("currentAppGeneratedImportsOwnerRuntimeContractSelectedArtifactBytes")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractExpectedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractCoveredTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractOwnerPriority",
                        "before-covered-focused-component-imports")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractSkipPolicy",
                        "skip-covered-focused-imports-when-owner-side-effect-satisfied")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFallbackPolicy",
                        "strict-blocker-with-focused-fallback-diagnostics")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractResolverHandoff",
                        "component-resolver-owned-input")
                && line.contains("currentAppGeneratedImportsOwnerRuntimeContractDecision")
                && line.contains("currentAppGeneratedImportsOwnerRuntimeContractFailureLayer")
                && line.contains("currentAppGeneratedImportsOwnerRuntimeContractNextResolutionStep")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractNoLocalhost", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractFixtureSubstituted", "false")
                && containsStringField(line, "currentAppGeneratedImportsOwnerLazyChunkBridgeVersion",
                        "flow-ipc-current-app-generated-imports-owner-lazy-chunk-bridge-v1_42")
                && containsStringField(line, "currentAppGeneratedImportsOwnerLazyChunkBridgeSource",
                        "current-app-build-frontend-output-v1_32")
                && line.contains("currentAppGeneratedImportsOwnerLazyChunkBridgeGeneratedImportsTableShape")
                && line.contains("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkId")
                && line.contains("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkArtifact")
                && line.contains("currentAppGeneratedImportsOwnerLazyChunkBridgeChunkStatus")
                && containsStringField(line, "currentAppGeneratedImportsOwnerLazyChunkBridgeBuildOutputSource",
                        "build-frontend")
                && (containsStringField(line,
                        "currentAppGeneratedImportsOwnerLazyChunkBridgeSupportedSpiDecision",
                        "pending-supported-vaadin-lazy-chunk-spi")
                        || containsStringField(line,
                                "currentAppGeneratedImportsOwnerLazyChunkBridgeSupportedSpiDecision",
                                "packaging-time-build-frontend-staging"))
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerLazyChunkBridgeNoLocalhost", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerLazyChunkBridgeFixtureSubstituted", "false");
    }

    static boolean containsCurrentAppGeneratedImportsOwnerRuntimeContractV142ImportComplete(String line) {
        return containsCurrentAppGeneratedImportsOwnerRuntimeContractV142Fields(line)
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractAttempted", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractImported", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractSatisfied", "true")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractObservedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerRuntimeContractModuleIdentity",
                        "current-app-generated-imports-owner-runtime-contract-v1_42:");
    }

    static boolean containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(String line) {
        return containsCurrentAppGeneratedImportsOwnerRuntimeContractV142ImportComplete(line)
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractResolverHandoffSatisfied", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerRuntimeContractFocusedImportsSkipped", "true")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractDecision",
                        "generated-imports-owner-runtime-contract-satisfied")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFailureLayer", "");
    }
}
