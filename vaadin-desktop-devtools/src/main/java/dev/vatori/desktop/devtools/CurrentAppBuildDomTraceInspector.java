package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.CurrentAppBuildFrontendTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildCurrentAppOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class CurrentAppBuildDomTraceInspector {
    private CurrentAppBuildDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppBuildFrontendOutputObserved", "true")
                && containsCurrentAppBuildFrontendOutputV132Fields(line)) {
            summary.currentAppBuildFrontend.outputV132DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppBuildFrontendOutputObserved", "true")
                && containsCurrentAppGeneratedChunkPackageEdgeV133Fields(line)) {
            summary.currentAppBuildFrontend.generatedChunkPackageEdgeV133DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppBuildFrontendOutputObserved", "true")
                && containsCurrentAppGeneratedImportsRootPackageGraphV134Fields(line)
                && containsLiteralField(line,
                        "currentAppGeneratedImportsRootProductionSupportExecuted", "true")
                && containsLiteralField(line,
                        "currentAppGeneratedImportsRootPackageGraphExecuted", "true")) {
            summary.currentAppBuildFrontend.generatedImportsRootPackageGraphV134DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipCandidateObserved", "true")
                && containsCurrentAppProductionRuntimeOwnershipV135Fields(line)
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipArtifactsStaged", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipPrimaryCandidateSelected", "true")
                && containsLiteralField(line,
                        "currentAppProductionRuntimeOwnershipHandoffReady", "true")
                && containsBooleanField(line,
                        "currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied")
                && containsBooleanField(line,
                        "currentAppProductionRuntimeOwnershipOwnerReuseSatisfied")) {
            summary.currentAppProductionOwner.runtimeOwnershipV135DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppProductionOwnerImportObserved", "true")
                && containsCurrentAppProductionOwnerImportV136Fields(line)
                && containsLiteralField(line, "currentAppProductionOwnerImportAttempted", "true")
                && containsLiteralField(line, "currentAppProductionOwnerImportOverCustomScheme", "true")
                && line.contains("currentAppProductionOwnerImportExecutionResult")
                && containsCurrentAppProductionOwnerImportV136FailureOrSuccess(line)) {
            summary.currentAppProductionOwner.ownerImportV136DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppProductionOwnerImportObserved", "true")
                && containsLiteralField(line, "currentAppProductionOwnerComponentReused", "true")
                && containsCurrentAppProductionOwnerComponentReuseV137Fields(line)
                && containsStringField(line, "currentAppProductionOwnerComponentReuseDecision",
                        "current-app-owner-reused")
                && containsStringField(line, "currentAppProductionOwnerComponentReuseReusedTags",
                        "vaadin-text-field")
                && containsStringField(line,
                        "currentAppProductionOwnerComponentReuseSkippedFocusedImports",
                        "META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")) {
            summary.currentAppOwnerComponentReuse.productionOwnerComponentReuseV137DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "currentAppProductionOwnerImportObserved", "true")
                && containsCurrentAppOwnerComponentReuseMatrixV138Complete(line)
                && containsLiteralField(line, "vaadinWebComponentResourceExecuted", "true")
                && containsLiteralField(line, "vaadinTextFieldRendered", "true")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")) {
            summary.currentAppOwnerComponentReuse.matrixV138DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppOwnerEarlyImportV139Complete(line)
                && containsLiteralField(line, "vaadinWebComponentResourceExecuted", "true")
                && containsLiteralField(line, "vaadinTextFieldRendered", "true")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")) {
            summary.currentAppOwnerEarlyImport.earlyImportV139DomObserved = true;
        }
    }
}
