package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FlowClientEngineDomTraceInspector {
    private FlowClientEngineDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("flowClientEngineResourceLoaded")
                && line.contains("flowClientEngineResourceVerified")
                && line.contains("flowClientEngineModuleLoaded")
                && line.contains("flowClientEngineInitExported")
                && line.contains("flowClientEngineInitialized")
                && line.contains("flowClientEngineClientRegistered")
                && line.contains("flowClientEngineIpcMessageBridgeInstalled")
                && line.contains("flowClientEngineIpcResyncBridgeInstalled")
                && line.contains("ROOT")
                && line.contains("META-INF/frontend/FlowClient.js")) {
            summary.flowClient.engineDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineBootstrapDomBound", "true")
                && line.contains("flowClientEngineBoundNodeIds")
                && line.contains("flowClientEngineBootstrapStatusText")
                && line.contains("flowClientEngineBootstrapButtonText")
                && line.contains("Increment over IPC")) {
            summary.flowClient.engineBootstrapDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineListBound", "true")
                && containsLiteralField(line, "flowClientEngineListItemCount", "4")
                && line.contains("1 Bootstrap over IPC READY")
                && line.contains("4 Desktop command/capabilityResult over IPC READY")) {
            summary.flowClient.engineListDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineXhrBridgeInstalled", "true")
                && containsLiteralField(line, "flowClientEngineXhrRequestObserved", "true")
                && containsLiteralField(line, "flowClientEngineXhrResponseObserved", "true")
                && containsLiteralField(line, "flowClientEngineEventPatchConsumed", "true")
                && containsClickedStatusField(line, "flowClientEngineEventStatusText")
                && containsStringField(line, "flowClientEngineXhrError", "")) {
            summary.flowClient.engineXhrDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineTextInputEventObserved", "true")
                && containsStringField(line, "flowClientEngineTextInputStatusText", "Text input: Flow IPC typed")
                && containsLiteralField(line, "flowClientEngineCheckboxEventObserved", "true")
                && containsStringField(line, "flowClientEngineCheckboxStatusText", "Checkbox: checked")
                && containsPositiveIntegerField(line, "flowClientEngineComponentEventPatchCount")
                && line.contains("ipc-text-input")
                && line.contains("ipc-checkbox")) {
            summary.flowClient.engineComponentEventsDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineIpcMessageBridgeInstalled", "true")
                && containsLiteralField(line, "flowClientEnginePushPatchConsumed", "true")
                && containsStringField(line, "flowClientEnginePushStatusText", "5 Server push over IPC READY")) {
            summary.flowClient.enginePushDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsPositiveIntegerField(line, "flowClientEngineQueuedMessageHighWater")
                && containsPositiveIntegerField(line, "flowClientEngineQueuedPushCount")
                && containsPositiveIntegerField(line, "flowClientEngineDeliveredQueuedMessageCount")
                && line.contains("flowClientEngineQueuedMessageOrder")
                && line.contains("flowClientEngineDeliveredQueuedMessageOrder")
                && line.contains("push:3")) {
            summary.flowClient.enginePushQueueDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsStringField(line, "flowClientEnginePushOrderStatusText", "6 Ordered push over IPC READY")
                && containsLiteralField(line, "flowClientEngineDeliveredPushCount", "2")
                && containsStringField(line, "flowClientEngineDeliveredMessageOrder", "push:3,push:4")
                && containsLiteralField(line, "flowClientEngineIgnoredMessageCount", "1")
                && containsStringField(line, "flowClientEngineIgnoredMessageOrder", "push:4:duplicate-late")
                && containsLiteralField(line, "flowClientEngineDuplicateLateIgnored", "true")
                && containsLiteralField(line, "flowClientEngineOrderingLastDeliveredServerSequence", "4")
                && containsLiteralField(line, "flowClientEngineRecoveryResyncRequested", "true")
                && containsStringField(line, "flowClientEngineRecoveryResyncReason",
                        "ordering-recovery-duplicate-late")) {
            summary.flowClient.enginePushOrderingDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineIpcResyncBridgeInstalled", "true")
                && containsLiteralField(line, "flowClientEngineResyncRequested", "true")
                && containsLiteralField(line, "flowClientEngineResyncPatchConsumed", "true")) {
            summary.flowClient.engineResyncDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsStringField(line, "flowClientEngineVisibleRootId", "flow-client-engine-root")
                && containsClickedStatusField(line, "flowClientEngineVisibleStatusText")
                && containsStringField(line, "flowClientEngineVisibleButtonText", "Increment over IPC")
                && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
            summary.flowClient.engineVisibleDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line, "flowClientEngineViewportReady", "true")
                && line.contains("flowClientEngineViewportRootTag")
                && containsStringField(line, "flowClientEnginePromotionGate", "generic-flow-viewport")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
            summary.flowClient.engineGenericViewportDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.firstMeaningfulPaint\"")
                && containsStringField(line, "transport", "flow-ipc")
                && containsStringField(line, "promotionGate", "generic-flow-viewport")
                && containsLiteralField(line, "flowClientEngineVisibleOwner", "true")
                && containsLiteralField(line, "flowClientEngineLocalFallbackHidden", "true")) {
            summary.core.firstMeaningfulPaintObserved = true;
        }
    }
}
