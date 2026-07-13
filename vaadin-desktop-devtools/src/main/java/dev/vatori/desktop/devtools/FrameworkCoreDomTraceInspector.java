package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkCoreDomTraceInspector {
    private FrameworkCoreDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line, "frameworkAppRendered", "true")
                && containsStringField(line, "frameworkAppRootId", "framework-root")
                && containsLiteralField(line, "frameworkAppBudgetRendered", "true")
                && containsLiteralField(line, "frameworkAppCheckboxRendered", "true")
                && containsLiteralField(line, "frameworkAppSelectRendered", "true")
                && containsLiteralField(line, "frameworkAppApplyRendered", "true")
                && containsStringField(line, "flowClientEngineViewportRootId", "framework-root")
                && containsStringField(line, "flowClientEnginePromotionGate", "generic-flow-viewport")
                && containsLiteralField(line, "flowClientEngineBootstrapDomBound", "false")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
            summary.frameworkApp.domObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "frameworkAppBudgetEventRpcObserved", "true")
                && containsLiteralField(line, "frameworkAppBudgetEventObserved", "true")
                && containsLiteralField(line, "frameworkAppCheckboxEventRpcObserved", "true")
                && containsLiteralField(line, "frameworkAppCheckboxEventObserved", "true")
                && containsLiteralField(line, "frameworkAppSelectEventRpcObserved", "true")
                && containsLiteralField(line, "frameworkAppSelectEventObserved", "true")
                && containsLiteralField(line, "frameworkAppApplyEventRpcObserved", "true")
                && containsLiteralField(line, "frameworkAppApplyEventObserved", "true")
                && containsLiteralField(line, "frameworkAppInteractionComplete", "true")
                && containsPositiveIntegerField(line, "frameworkAppEventPatchCount")
                && line.contains("framework-budget")
                && line.contains("framework-expedite")
                && line.contains("framework-priority")
                && line.contains("framework-apply")
                && line.contains("Revision ")
                && line.contains("priority audit")
                && line.contains("Renderer owned by Flow client engine")) {
            summary.frameworkApp.interactionDomObserved = true;
        }
    }
}
