package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.FrontendBuildGeneratedImportsOwnerTraceFields.*;
import static dev.vatori.desktop.devtools.FrontendBuildPackagingTraceFields.*;
import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrontendBuildOwnerPackagingDomTraceInspector {
    private FrontendBuildOwnerPackagingDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppGeneratedImportsOwnerSideEffectV140Complete(line)
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectReusedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports",
                        "META-INF/resources/frontend/vaadin-big-decimal-field.js,META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js,META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports",
                        "META-INF/vatori/dev-bundle-entry/vaadin-select.js")
                && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                && containsStringField(line, "vaadinWebComponentStatusText",
                        "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinSecondWebComponentStatusText",
                        "Vaadin Checkbox: checked")
                && containsLiteralField(line, "vaadinTextFieldRendered", "true")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")) {
            summary.generatedImportsOwner.sideEffectV140DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppGeneratedImportsSelectOwnerCoverageV141Complete(line)
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectReusedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectSkippedFocusedImports",
                        "META-INF/resources/frontend/vaadin-big-decimal-field.js,META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js,META-INF/vatori/dev-bundle-entry/vaadin-select.js,META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectFallbackTags", "")
                && containsStringField(line, "currentAppGeneratedImportsOwnerSideEffectFallbackFocusedImports", "")
                && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                && containsLiteralField(line, "vaadinSelectTagDefined", "true")
                && containsStringField(line, "vaadinWebComponentStatusText",
                        "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinSecondWebComponentStatusText",
                        "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")
                && containsLiteralField(line, "vaadinSelectRendered", "true")
                && containsStringField(line, "vaadinSelectRenderedTag", "vaadin-select")
                && containsLiteralField(line, "vaadinSelectEventRpcObserved", "true")
                && containsStringField(line, "vaadinSelectEventComponentId", "ipc-vaadin-select")
                && containsStringField(line, "vaadinSelectStatusText", "Vaadin Select: audit")
                && containsStringField(line, "vaadinSelectPostEventState", "Vaadin Select: audit")) {
            summary.generatedImportsOwner.selectOwnerCoverageV141DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(line)
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractReusedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractSkippedFocusedImports",
                        "META-INF/resources/frontend/vaadin-big-decimal-field.js,META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js,META-INF/vatori/dev-bundle-entry/vaadin-select.js,META-INF/vatori/dev-bundle-entry/vaadin-text-field.js")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "")
                && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                && containsLiteralField(line, "vaadinSelectTagDefined", "true")
                && containsStringField(line, "vaadinWebComponentStatusText",
                        "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinSecondWebComponentStatusText",
                        "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")
                && containsLiteralField(line, "vaadinSelectRendered", "true")
                && containsLiteralField(line, "vaadinSelectEventRpcObserved", "true")
                && containsStringField(line, "vaadinSelectStatusText", "Vaadin Select: audit")) {
            summary.generatedImportsOwner.runtimeContractV142DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(line)
                && containsGeneratedLazySpiDecisionV143ChunkExecuted(line)
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractReusedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "")
                && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "true")
                && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                && containsLiteralField(line, "vaadinSelectTagDefined", "true")
                && containsStringField(line, "vaadinWebComponentStatusText",
                        "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinSecondWebComponentStatusText",
                        "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")
                && containsStringField(line, "vaadinSelectStatusText", "Vaadin Select: audit")) {
            summary.generatedLazySpiDecision.decisionV143DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(line)
                && containsPackagingFrontendInventoryV144Complete(line)
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractReusedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "")
                && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "true")
                && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                && containsLiteralField(line, "vaadinSelectTagDefined", "true")
                && containsStringField(line, "vaadinWebComponentStatusText",
                        "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinSecondWebComponentStatusText",
                        "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")
                && containsStringField(line, "vaadinSelectStatusText", "Vaadin Select: audit")) {
            summary.packaging.frontendInventoryV144DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsCurrentAppGeneratedImportsOwnerRuntimeContractV142DomComplete(line)
                && containsPackagingFrontendInventoryV144Complete(line)
                && containsPackagingRuntimeHandoffReuseV145Complete(line)
                && containsStringField(line, "packagingRuntimeHandoffReuseAppShape", "strict-smoke-app")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractReusedTags",
                        "vaadin-big-decimal-field,vaadin-checkbox,vaadin-select,vaadin-text-field")
                && containsStringField(line, "currentAppGeneratedImportsOwnerRuntimeContractFallbackTags", "")
                && containsLiteralField(line, "loadOnDemandChunkFileExecuted", "true")
                && containsStringField(line, "vaadinWebComponentStatusText",
                        "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinSecondWebComponentStatusText",
                        "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinTextFieldStatusText",
                        "Vaadin TextField: Flow IPC typed")
                && containsStringField(line, "vaadinSelectStatusText", "Vaadin Select: audit")) {
            summary.packaging.runtimeHandoffReuseV145DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsPackagingFrontendInventoryV144Complete(line)
                && containsPackagingRuntimeHandoffReuseV145Complete(line)
                && containsStringField(line, "packagingRuntimeHandoffReuseAppShape",
                        "framework-validation-app")
                && containsLiteralField(line, "frameworkAppInteractionComplete", "true")
                && containsLiteralField(line, "frameworkAppBudgetEventObserved", "true")
                && containsLiteralField(line, "frameworkAppCheckboxEventObserved", "true")
                && containsLiteralField(line, "frameworkAppSelectEventObserved", "true")
                && containsLiteralField(line, "frameworkAppApplyEventObserved", "true")
                && line.contains("Revision ")
                && line.contains("priority audit")) {
            summary.packaging.runtimeHandoffReuseV145FrameworkDomObserved = true;
        }
    }
}
