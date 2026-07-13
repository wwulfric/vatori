package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class FrameworkBehaviorResourceDomTraceInspector {
    private FrameworkBehaviorResourceDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixRendered", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixFocusFieldRendered", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixToggleRendered", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixFocusEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixFocusEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixBlurEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixBlurEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixAttachEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixAttachEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixDetachEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixDetachEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkLifecycleFocusBehaviorMatrixComplete", "true")
                && containsStringField(line,
                        "frameworkLifecycleFocusBehaviorMatrixVersion",
                        "flow-ipc-lifecycle-focus-behavior-matrix-v1_61")
                && line.contains("framework-lifecycle-focus")) {
            summary.frameworkLifecycleFocusBehaviorMatrix.domObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line, "frameworkLazyDataProviderRendered", "true")
                && containsLiteralField(line, "frameworkLazyDataProviderGridRendered", "true")
                && containsLiteralField(line, "frameworkLazyDataProviderPublishedRpcObserved", "true")
                && containsLiteralField(line, "frameworkLazyDataProviderFetchObserved", "true")
                && containsLiteralField(line, "frameworkLazyDataProviderReportEventRpcObserved", "true")
                && containsLiteralField(line, "frameworkLazyDataProviderReportEventObserved", "true")
                && containsLiteralField(line, "frameworkLazyDataProviderComplete", "true")
                && line.contains("Lazy grid fetches:")
                && containsLazyGridReportRange(line)) {
            summary.gridResources.lazyDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line,
                        "frameworkGridScrollVirtualizationRendered", "true")
                && containsLiteralField(line,
                        "frameworkGridScrollVirtualizationClientApiAvailable", "true")
                && containsLiteralField(line,
                        "frameworkGridScrollVirtualizationClientApiInvoked", "true")
                && (containsLiteralField(line,
                        "frameworkGridScrollVirtualizationComplete", "true")
                        || containsLiteralField(line,
                                "frameworkGridScrollVirtualizationAcceptedBlockerObserved",
                                "true"))) {
            summary.gridResources.scrollDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line, "frameworkStreamResourceRendered", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceAnchorHrefObserved", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceRequestObserved", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceResponseObserved", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceBodyVerified", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceComplete", "true")) {
            summary.streamResources.basicDomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line, "frameworkStreamResourceRangeRendered", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceRangeAnchorHrefObserved", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceRangeRequestObserved", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceRangeResponseObserved", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceRangeBodyVerified", "true")
                && containsLiteralField(line,
                        "frameworkStreamResourceRangeComplete", "true")) {
            summary.streamResources.rangeDomObserved = true;
        }
    }
}
