package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildCurrentAppOwnerComponentReuseTraceFields {
    boolean productionOwnerComponentReuseV137ManifestObserved;
    boolean productionOwnerComponentReuseV137RequestObserved;
    boolean productionOwnerComponentReuseV137ResponseObserved;
    boolean productionOwnerComponentReuseV137StageObserved;
    boolean productionOwnerComponentReuseV137TextFieldReuseObserved;
    boolean productionOwnerComponentReuseV137DomObserved;
    boolean matrixV138ManifestObserved;
    boolean matrixV138RequestObserved;
    boolean matrixV138ResponseObserved;
    boolean matrixV138StageObserved;
    boolean matrixV138CompleteStageObserved;
    boolean matrixV138DomObserved;

    boolean hasProductionOwnerComponentReuseV1_37Signals() {
        return productionOwnerComponentReuseV137ManifestObserved
                && productionOwnerComponentReuseV137RequestObserved
                && productionOwnerComponentReuseV137ResponseObserved
                && productionOwnerComponentReuseV137StageObserved
                && productionOwnerComponentReuseV137TextFieldReuseObserved
                && productionOwnerComponentReuseV137DomObserved;
    }

    boolean hasMatrixV1_38Signals() {
        return matrixV138ManifestObserved
                && matrixV138RequestObserved
                && matrixV138ResponseObserved
                && matrixV138StageObserved
                && matrixV138CompleteStageObserved
                && matrixV138DomObserved;
    }

    static boolean containsCurrentAppProductionOwnerComponentReuseV137Fields(String line) {
        return containsStringField(line, "currentAppProductionOwnerComponentReuseVersion",
                        "flow-ipc-current-app-production-owner-component-reuse-v1_37")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseSource",
                        "current-app-production-owner-import-side-effect-v1_36")
                && containsStringField(line, "currentAppProductionOwnerComponentReusePolicy",
                        "reuse-current-app-owner-defined-custom-elements-before-focused-import-v1_37")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseExpectedTags",
                        "vaadin-text-field")
                && line.contains("currentAppProductionOwnerComponentReuseDecision")
                && line.contains("currentAppProductionOwnerComponentReuseReason")
                && line.contains("currentAppProductionOwnerComponentReuseReusedTags")
                && line.contains("currentAppProductionOwnerComponentReuseSkippedFocusedImports")
                && line.contains("currentAppProductionOwnerComponentReuseFallbackFocusedImports")
                && line.contains("currentAppProductionOwnerComponentReuseFailureLayer")
                && line.contains("currentAppProductionOwnerComponentReuseNextResolutionStep")
                && containsLiteralField(line, "currentAppProductionOwnerComponentReuseNoLocalhost", "true");
    }

    static boolean containsCurrentAppOwnerComponentReuseMatrixV138Fields(String line) {
        return containsStringField(line, "currentAppProductionOwnerComponentReuseMatrixVersion",
                        "flow-ipc-current-app-owner-component-reuse-matrix-v1_38")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseMatrixSource",
                        "current-app-production-owner-component-reuse-v1_37")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseMatrixPolicy",
                        "classify-current-app-owner-reuse-or-focused-fallback-for-covered-components-v1_38")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseMatrixExpectedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixObservedTags")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixReusedTags")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixFallbackTags")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixSkippedFocusedImports")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixFallbackFocusedImports")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixOrderingBlockers")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixDecision")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixReason")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixFailureLayer")
                && line.contains("currentAppProductionOwnerComponentReuseMatrixNextResolutionStep")
                && containsLiteralField(line, "currentAppProductionOwnerComponentReuseMatrixNoLocalhost",
                        "true");
    }

    static boolean containsCurrentAppOwnerComponentReuseMatrixCoveredComponentPath(String line) {
        return containsPath(line, "META-INF/resources/frontend/vaadin-big-decimal-field.js")
                || containsPath(line, "META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js")
                || containsPath(line, "META-INF/vatori/dev-bundle-entry/vaadin-select.js")
                || containsPath(line, "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js");
    }

    static boolean containsCurrentAppOwnerComponentReuseMatrixV138Complete(String line) {
        return containsCurrentAppOwnerComponentReuseMatrixV138Fields(line)
                && containsLiteralField(line, "currentAppProductionOwnerComponentReuseMatrixComplete",
                        "true")
                && containsLiteralField(line,
                        "currentAppProductionOwnerComponentReuseMatrixTextFieldReuseRetained", "true")
                && containsLiteralField(line,
                        "currentAppProductionOwnerComponentReuseMatrixEarlyFallbackClassified", "true")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseMatrixDecision",
                        "matrix-complete-textfield-reused-early-components-focused-fallback")
                && line.contains("vaadin-big-decimal-field")
                && line.contains("vaadin-checkbox")
                && line.contains("vaadin-select")
                && line.contains("vaadin-text-field")
                && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-select.js")
                && line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && line.contains("current-app-owner-not-ready-before-focused-import")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseMatrixFailureLayer",
                        "current-app-owner-component-reuse-matrix-early-component-ordering-pending");
    }
}
