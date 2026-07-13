package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class VaadinComponentResourceTraceInspector {
    private VaadinComponentResourceTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                && line.contains("\"lookupKind\":\"BigDecimalField-minimum-component-resource\"")
                && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                && line.contains("\"securityKey\"")) {
            summary.vaadinComponents.webComponentResourceRequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                && line.contains("\"lookupKind\":\"Checkbox-component-resource-v1_3\"")
                && line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                && line.contains("\"securityKey\"")) {
            summary.vaadinComponents.secondWebComponentResourceRequestObserved = true;
        }
        if (line.contains("\"type\":\"resourceRequest\"")
                && line.contains("\"purpose\":\"vaadin-web-component-resource-probe\"")
                && line.contains("\"lookupKind\":\"Checkbox-dev-bundle-resource-v1_4\"")
                && (line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                        || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))
                && line.contains("\"securityKey\"")) {
            summary.vaadinComponents.secondWebComponentResourceRequestObserved = true;
            summary.vaadinComponents.secondWebComponentDevBundleResourceObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && line.contains("\"componentFrontendResource\":true")
                && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                && line.contains("\"bodyRedacted\":true")
                && line.contains("\"blockedReason\"")
                && (line.contains("\"failureClassification\":\"npm-transitive-imports-shimmed\"")
                        || line.contains("\"failureClassification\":\"textfield-production-class-execution-blocked\"")
                        || line.contains("\"failureClassification\":\"real-textfield-production-graph-executed-v1_7\""))) {
            summary.vaadinComponents.webComponentResourceResponseObserved = true;
            summary.vaadinComponents.webComponentFailureClassificationObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && line.contains("\"componentFrontendResource\":true")
                && line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                && line.contains("\"lookupKind\":\"Checkbox-component-resource-v1_3\"")
                && line.contains("\"bodyRedacted\":true")
                && line.contains("\"failureClassification\":\"npm-transitive-imports-shimmed\"")) {
            summary.vaadinComponents.secondWebComponentResourceResponseObserved = true;
            summary.vaadinComponents.webComponentFailureClassificationObserved = true;
        }
        if (line.contains("\"type\":\"resourceResponse\"")
                && line.contains("\"componentFrontendResource\":true")
                && (line.contains("vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                        || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))
                && line.contains("\"lookupKind\":\"Checkbox-dev-bundle-resource-v1_4\"")
                && line.contains("\"bodyRedacted\":true")
                && line.contains("\"devBundleResource\":true")) {
            summary.vaadinComponents.secondWebComponentResourceResponseObserved = true;
            summary.vaadinComponents.secondWebComponentDevBundleResourceObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "vaadinWebComponentResourceManifestObserved", "true")
                && containsPositiveIntegerField(line, "vaadinWebComponentResourceManifestCount")
                && containsLiteralField(line, "vaadinWebComponentResourceRequested", "true")
                && containsPositiveIntegerField(line, "vaadinWebComponentResourceRequestedCount")
                && containsPositiveIntegerField(line, "vaadinWebComponentResourceResponseCount")
                && containsLiteralField(line, "vaadinWebComponentResourceResolved", "true")
                && containsLiteralField(line, "vaadinWebComponentResourceVerified", "true")
                && containsLiteralField(line, "vaadinWebComponentResourceGraphComplete", "true")
                && containsLiteralField(line, "vaadinWebComponentResourceGraphVerified", "true")
                && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                && (line.contains("META-INF/vatori/frontend-shims/vaadin-text-field.js")
                        || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-text-field.js"))
                && (line.contains("npm-transitive-imports-shimmed")
                        || line.contains("textfield-production-class-execution-blocked")
                        || line.contains("real-textfield-production-graph-executed-v1_7"))
                && (line.contains("full @vaadin/text-field npm graph")
                        || line.contains("lit npm helper")
                        || line.contains("@vaadin/text-field import is still a controlled shim")
                        || line.contains("@vaadin/text-field import resolves to the real focused TextField dev-bundle entry"))) {
            summary.vaadinComponents.webComponentResourceDomObserved = true;
            summary.vaadinComponents.webComponentFailureClassificationObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "vaadinWebComponentResourceExecuted", "true")
                && containsPath(line, "META-INF/resources/frontend/vaadin-big-decimal-field.js")
                && (containsStringField(line, "vaadinWebComponentResourceImportMode",
                        "dynamic-import-blob-with-v1_2-shimmed-transitive-imports")
                        || containsStringField(line, "vaadinWebComponentResourceImportMode",
                                "dynamic-import-blob-with-manifest-import-rewrites-v1_4"))
                && containsLiteralField(line, "vaadinWebComponentTagDefined", "true")
                && containsLiteralField(line, "vaadinWebComponentResourceExecutionFailed", "false")) {
            summary.vaadinComponents.webComponentResourceExecutedDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "vaadinWebComponentResourceResolverStrategy",
                        "generic-component-frontend-manifest-v1_3")
                && containsLiteralField(line, "vaadinWebComponentResourceExecuted", "true")
                && (containsLiteralField(line, "vaadinWebComponentResourceExecutedCount", "2")
                        || containsLiteralField(line, "vaadinWebComponentResourceExecutedCount", "3")
                        || containsLiteralField(line, "vaadinWebComponentResourceExecutedCount", "4"))
                && line.contains("META-INF/resources/frontend/vaadin-big-decimal-field.js")
                && (line.contains("META-INF/vatori/frontend-shims/vaadin-checkbox.js")
                        || line.contains(
                                "vaadin-dev-bundle/webapp/VAADIN/build/generated-flow-imports-VW7hRaNw.js")
                        || line.contains("META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js"))
                && line.contains("vaadin-big-decimal-field")
                && line.contains("vaadin-checkbox")
                && containsLiteralField(line, "vaadinWebComponentResourceExecutionFailed", "false")) {
            summary.vaadinComponents.resolverV13DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "vaadinWebComponentRendered", "true")
                && containsStringField(line, "vaadinWebComponentRenderedTag", "vaadin-big-decimal-field")
                && containsStringField(line, "vaadinWebComponentEventDispatchMode", "dom-event")
                && containsLiteralField(line, "vaadinWebComponentEventObserved", "true")
                && containsLiteralField(line, "vaadinWebComponentEventRpcObserved", "true")
                && containsStringField(line, "vaadinWebComponentEventComponentId", "ipc-vaadin-big-decimal")
                && containsStringField(line, "vaadinWebComponentEventType", "change")
                && containsStringField(line, "vaadinWebComponentEventValue", "42.5")
                && containsStringField(line, "vaadinWebComponentStatusText", "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinWebComponentPostEventState", "Vaadin BigDecimalField: 42.5")
                && containsStringField(line, "vaadinWebComponentFailureReason", "")) {
            summary.vaadinComponents.webComponentDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "vaadinTextFieldRendered", "true")
                && containsStringField(line, "vaadinTextFieldRenderedTag", "vaadin-text-field")
                && containsLiteralField(line, "vaadinTextFieldTagDefined", "true")
                && containsStringField(line, "vaadinTextFieldEventDispatchMode", "dom-event")
                && containsLiteralField(line, "vaadinTextFieldEventObserved", "true")
                && containsLiteralField(line, "vaadinTextFieldEventRpcObserved", "true")
                && containsStringField(line, "vaadinTextFieldEventComponentId", "ipc-vaadin-text-field")
                && containsStringField(line, "vaadinTextFieldEventType", "change")
                && containsStringField(line, "vaadinTextFieldEventValue", "Flow IPC typed")
                && containsStringField(line, "vaadinTextFieldStatusText", "Vaadin TextField: Flow IPC typed")
                && containsStringField(line, "vaadinTextFieldPostEventState", "Vaadin TextField: Flow IPC typed")
                && containsStringField(line, "vaadinTextFieldFailureReason", "")) {
            summary.vaadinComponents.textFieldDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "vaadinSecondWebComponentRendered", "true")
                && containsStringField(line, "vaadinSecondWebComponentRenderedTag", "vaadin-checkbox")
                && containsLiteralField(line, "vaadinSecondWebComponentTagDefined", "true")
                && containsStringField(line, "vaadinSecondWebComponentEventDispatchMode", "dom-event")
                && containsLiteralField(line, "vaadinSecondWebComponentEventObserved", "true")
                && containsLiteralField(line, "vaadinSecondWebComponentEventRpcObserved", "true")
                && containsStringField(line, "vaadinSecondWebComponentEventComponentId", "ipc-vaadin-checkbox")
                && containsStringField(line, "vaadinSecondWebComponentEventType", "change")
                && containsStringField(line, "vaadinSecondWebComponentEventValue", "true")
                && containsStringField(line, "vaadinSecondWebComponentStatusText", "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinSecondWebComponentPostEventState", "Vaadin Checkbox: checked")
                && containsStringField(line, "vaadinSecondWebComponentFailureReason", "")) {
            summary.vaadinComponents.secondWebComponentDomObserved = true;
        }
    }
}
