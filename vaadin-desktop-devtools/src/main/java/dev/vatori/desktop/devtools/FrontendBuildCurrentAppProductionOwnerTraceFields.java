package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildCurrentAppProductionOwnerTraceFields {
    boolean runtimeOwnershipV135ManifestObserved;
    boolean runtimeOwnershipV135RequestObserved;
    boolean runtimeOwnershipV135ResponseObserved;
    boolean runtimeOwnershipV135StageObserved;
    boolean runtimeOwnershipV135HandoffObserved;
    boolean runtimeOwnershipV135DomObserved;
    boolean ownerImportV136ManifestObserved;
    boolean ownerImportV136RequestObserved;
    boolean ownerImportV136ResponseObserved;
    boolean ownerImportV136StageObserved;
    boolean ownerImportV136AttemptObserved;
    boolean ownerImportV136DomObserved;

    boolean hasRuntimeOwnershipV1_35Signals() {
        return runtimeOwnershipV135ManifestObserved
                && runtimeOwnershipV135RequestObserved
                && runtimeOwnershipV135ResponseObserved
                && runtimeOwnershipV135StageObserved
                && runtimeOwnershipV135HandoffObserved
                && runtimeOwnershipV135DomObserved;
    }

    boolean hasOwnerImportV1_36Signals() {
        return ownerImportV136ManifestObserved
                && ownerImportV136RequestObserved
                && ownerImportV136ResponseObserved
                && ownerImportV136StageObserved
                && ownerImportV136AttemptObserved
                && ownerImportV136DomObserved;
    }

    static boolean containsCurrentAppProductionRuntimeOwnershipV135Fields(String line) {
        return containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)
                && containsStringField(line, "currentAppProductionRuntimeOwnershipVersion",
                        "flow-ipc-current-app-production-runtime-ownership-v1_35")
                && containsStringField(line, "currentAppProductionRuntimeOwnershipSource",
                        "current-app-build-frontend-output-v1_32-to-v1_34")
                && containsStringField(line, "currentAppProductionRuntimeOwnershipPrimaryCandidate",
                        "current-app-build-frontend-production-runtime")
                && containsStringField(line, "currentAppProductionRuntimeOwnershipHandoffPolicy",
                        "current-app-source-root-chunk-plus-production-support-to-owner-runtime-v1_35")
                && containsStringField(line, "currentAppProductionRuntimeOwnershipSelectedImportsArtifact",
                        "current-app-frontend/generated/flow/generated-flow-imports.js")
                && containsStringFieldPrefix(line,
                        "currentAppProductionRuntimeOwnershipSelectedChunkArtifact",
                        "current-app-frontend/generated/flow/chunks/")
                && containsStringFieldPrefix(line,
                        "currentAppProductionRuntimeOwnershipProductionSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && containsStringFieldPrefix(line,
                        "currentAppProductionRuntimeOwnershipProductionEntryArtifact",
                        "current-app-production/VAADIN/build/indexhtml-")
                && line.contains("currentAppProductionRuntimeOwnershipArtifactInventory")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipRootPackageGraphResolved", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipChunkPackageGraphResolved", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipProductionSupportObserved", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipStatsObserved", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipProdBundleObserved", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipHandoffReady", "true")
                && containsBooleanField(line,
                        "currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied")
                && containsBooleanField(line,
                        "currentAppProductionRuntimeOwnershipOwnerReuseSatisfied")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipNoLocalhost", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipFixtureSubstituted", "false")
                && containsStringField(line, "currentAppProductionRuntimeOwnershipFailureLayer",
                        "current-app-production-runtime-ownership-handoff-pending")
                && line.contains("currentAppProductionRuntimeOwnershipNextResolutionStep");
    }

    static boolean containsCurrentAppProductionOwnerImportV136Fields(String line) {
        return containsCurrentAppProductionRuntimeOwnershipV135Fields(line)
                && containsStringField(line, "currentAppProductionOwnerImportVersion",
                        "flow-ipc-current-app-production-owner-import-v1_36")
                && containsStringField(line, "currentAppProductionOwnerImportSource",
                        "current-app-production-runtime-ownership-handoff-v1_35")
                && containsStringField(line, "currentAppProductionOwnerImportPolicy",
                        "owner-first-current-app-production-entry-import-v1_36")
                && containsStringField(line, "currentAppProductionOwnerImportCandidate",
                        "current-app-build-frontend-production-runtime")
                && containsStringFieldPrefix(line,
                        "currentAppProductionOwnerImportArtifact",
                        "current-app-production/VAADIN/build/indexhtml-")
                && containsStringFieldPrefix(line,
                        "currentAppProductionOwnerImportSupportArtifact",
                        "current-app-production/VAADIN/build/generated-flow-imports-")
                && line.contains("currentAppProductionOwnerImportDependencyPaths")
                && containsStringField(line, "currentAppProductionOwnerImportExpectedSideEffects",
                        "window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-vertical-layout|vaadin-text-field|vaadin-checkbox|vaadin-select")
                && containsStringField(line, "currentAppProductionOwnerImportPhase",
                        "after-current-app-generated-root-execution")
                && containsLiteralField(line, "currentAppProductionOwnerImportHandoffReady", "true")
                && containsLiteralField(line, "currentAppProductionOwnerImportNoLocalhost", "true")
                && containsLiteralField(line, "currentAppProductionOwnerImportFixtureSubstituted", "false")
                && line.contains("currentAppProductionOwnerImportFailureLayer")
                && line.contains("currentAppProductionOwnerImportNextResolutionStep");
    }

    static boolean containsCurrentAppProductionOwnerImportV136FailureOrSuccess(String line) {
        return containsLiteralField(line, "currentAppProductionOwnerImported", "true")
                || containsLiteralField(line, "currentAppProductionOwnerImported", "false")
                || containsStringField(line, "currentAppProductionOwnerImportFailureLayer",
                        "current-app-production-owner-import-side-effect-pending")
                || containsStringField(line, "currentAppProductionOwnerImportFailureLayer",
                        "current-app-production-owner-duplicate-custom-element")
                || containsStringField(line, "currentAppProductionOwnerImportFailureLayer",
                        "current-app-production-owner-missing-real-artifact")
                || containsStringField(line, "currentAppProductionOwnerImportFailureLayer",
                        "current-app-production-owner-global-collision")
                || containsStringField(line, "currentAppProductionOwnerImportFailureLayer",
                        "current-app-production-owner-browser-import-failed");
    }
}
