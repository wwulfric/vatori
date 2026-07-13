package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildCurrentAppOwnerEarlyImportTraceFields {
    boolean earlyImportV139ManifestObserved;
    boolean earlyImportV139RequestObserved;
    boolean earlyImportV139ResponseObserved;
    boolean earlyImportV139StageObserved;
    boolean earlyImportV139CompleteStageObserved;
    boolean earlyImportV139DomObserved;

    boolean hasEarlyImportV1_39Signals() {
        return earlyImportV139ManifestObserved
                && earlyImportV139RequestObserved
                && earlyImportV139ResponseObserved
                && earlyImportV139StageObserved
                && earlyImportV139CompleteStageObserved
                && earlyImportV139DomObserved;
    }

    static boolean containsCurrentAppOwnerEarlyImportV139Fields(String line) {
        return containsStringField(line, "currentAppProductionOwnerEarlyImportVersion",
                        "flow-ipc-current-app-production-owner-early-import-v1_39")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportSource",
                        "current-app-owner-component-reuse-matrix-v1_38")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportPolicy",
                        "request-current-app-production-owner-before-covered-component-imports-v1_39")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportPhase",
                        "before-component-imports")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportReusableTags",
                        "vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportCoveredTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && line.contains("currentAppProductionOwnerEarlyImportReusedTags")
                && line.contains("currentAppProductionOwnerEarlyImportSkippedFocusedImports")
                && line.contains("currentAppProductionOwnerEarlyImportFallbackTags")
                && line.contains("currentAppProductionOwnerEarlyImportFallbackFocusedImports")
                && line.contains("currentAppProductionOwnerEarlyImportOrderingBlockers")
                && line.contains("currentAppProductionOwnerEarlyImportDecision")
                && line.contains("currentAppProductionOwnerEarlyImportReason")
                && line.contains("currentAppProductionOwnerEarlyImportFailureLayer")
                && line.contains("currentAppProductionOwnerEarlyImportNextResolutionStep")
                && containsLiteralField(line, "currentAppProductionOwnerEarlyImportNoLocalhost", "true")
                && containsLiteralField(line, "currentAppProductionOwnerEarlyImportFixtureSubstituted", "false");
    }

    static boolean containsCurrentAppOwnerEarlyImportV139Complete(String line) {
        return containsCurrentAppOwnerEarlyImportV139Fields(line)
                && containsLiteralField(line,
                        "currentAppProductionOwnerEarlyImportRequestBeforeCoveredComponents", "true")
                && containsLiteralField(line, "currentAppProductionOwnerEarlyImportAttempted", "true")
                && containsLiteralField(line, "currentAppProductionOwnerEarlyImported", "true")
                && (containsCurrentAppOwnerEarlyImportV139PreferredComplete(line)
                        || containsCurrentAppOwnerEarlyImportV139SideEffectMissComplete(line));
    }

    private static boolean containsCurrentAppOwnerEarlyImportV139PreferredComplete(String line) {
        return containsLiteralField(line,
                        "currentAppProductionOwnerEarlyImportSideEffectSatisfied", "true")
                && containsLiteralField(line,
                        "currentAppProductionOwnerEarlyImportPartialReuseSatisfied", "true")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportDecision",
                        "early-import-reused-covered-owner-tags-bigdecimal-focused-fallback")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportFailureLayer",
                        "current-app-owner-early-import-bigdecimal-unowned-pending")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportReusedTags",
                        "vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportFallbackTags",
                        "vaadin-big-decimal-field")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportOrderingBlockers",
                        "vaadin-big-decimal-field:current-app-owner-side-effect-does-not-own-tag-before-focused-import")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportSkippedFocusedImports",
                        "META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js,META-INF/vatori/dev-bundle-entry/vaadin-select.js,META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportFallbackFocusedImports",
                        "META-INF/resources/frontend/vaadin-big-decimal-field.js");
    }

    private static boolean containsCurrentAppOwnerEarlyImportV139SideEffectMissComplete(String line) {
        return containsLiteralField(line,
                        "currentAppProductionOwnerEarlyImportSideEffectSatisfied", "false")
                && containsLiteralField(line,
                        "currentAppProductionOwnerEarlyImportPartialReuseSatisfied", "false")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportDecision",
                        "early-import-owner-imported-before-components-side-effect-not-reusable")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportFailureLayer",
                        "current-app-owner-early-import-side-effect-not-reusable")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportFallbackTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportOrderingBlockers",
                        "vaadin-big-decimal-field:current-app-owner-imported-before-focused-import-without-reusable-side-effect,vaadin-checkbox:current-app-owner-imported-before-focused-import-without-reusable-side-effect,vaadin-select:current-app-owner-imported-before-focused-import-without-reusable-side-effect,vaadin-text-field:current-app-owner-imported-before-focused-import-without-reusable-side-effect")
                && containsStringField(line, "currentAppProductionOwnerEarlyImportFallbackFocusedImports",
                        "META-INF/resources/frontend/vaadin-big-decimal-field.js,META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js,META-INF/vatori/dev-bundle-entry/vaadin-select.js,META-INF/vatori/dev-bundle-entry/vaadin-text-field.js");
    }
}
