package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildGeneratedImportsOwnerSideEffectTraceFields {
    private FrontendBuildGeneratedImportsOwnerSideEffectTraceFields() {
    }

    static boolean containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(String line) {
        return containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectVersion",
                        "flow-ipc-current-app-generated-imports-owner-side-effect-v1_40")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectSource",
                        "current-app-owner-early-import-v1_39")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectPolicy",
                        "import-current-app-production-generated-imports-before-covered-components-v1_40")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectCandidate",
                        "current-app-production-generated-imports-support-artifact")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerSideEffectArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectExpectedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectCoveredTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectKnownMissingTags",
                        "vaadin-select")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectExpectedGlobals",
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectPhase",
                        "before-component-imports")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectObservedTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectMissingTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectReusedTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectFallbackTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectDecision")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectReason")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectFailureLayer")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectNextResolutionStep")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectNoLocalhost", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectFixtureSubstituted", "false");
    }

    static boolean containsCurrentAppGeneratedImportsOwnerSideEffectV140Complete(String line) {
        return containsCurrentAppGeneratedImportsOwnerSideEffectV140Fields(line)
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectAttempted", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectImported", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectPartialReuseSatisfied", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectLoadOnDemandPresent", "true")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectObservedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectFallbackTags",
                        "vaadin-select")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerSideEffectDecision",
                        "generated-imports-owner")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerSideEffectModuleIdentity",
                        "current-app-generated-imports-owner-v1_40:")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectRegistryBefore")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectRegistryAfter")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectVaadinGlobalBefore")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectVaadinGlobalAfter");
    }

    static boolean containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(String line) {
        return containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectVersion",
                        "flow-ipc-current-app-generated-imports-select-owner-coverage-v1_41")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectSource",
                        "current-app-generated-imports-owner-side-effect-v1_40")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectPolicy",
                        "cover-current-app-production-generated-imports-select-before-components-v1_41")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectCandidate",
                        "current-app-production-generated-imports-support-artifact")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerSideEffectArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectExpectedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectCoveredTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectKnownMissingTags", "")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectExpectedGlobals",
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectPhase",
                        "before-component-imports")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectObservedTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectMissingTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectReusedTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectFallbackTags")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectDecision")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectReason")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectFailureLayer")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectNextResolutionStep")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectNoLocalhost", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectFixtureSubstituted", "false");
    }

    static boolean containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Complete(String line) {
        return containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Fields(line)
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectAttempted", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectImported", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectPartialReuseSatisfied", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsOwnerSideEffectLoadOnDemandPresent", "true")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectObservedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectMissingTags", "")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectFallbackTags", "")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports", "")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectDecision",
                        "generated-imports-owner-reused-covered-tags")
                && containsStringFieldPrefix(line, "currentAppGeneratedImportsOwnerSideEffectModuleIdentity",
                        "current-app-generated-imports-select-owner-v1_41:")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectRegistryBefore")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectRegistryAfter")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectVaadinGlobalBefore")
                && line.contains("currentAppGeneratedImportsOwnerSideEffectVaadinGlobalAfter");
    }
}
