package dev.vatori.desktop.devtools;

final class LoginOverlayFocusTraceInspector {
    private LoginOverlayFocusTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginOverlayInitialFocusTraceInspector.inspectLine(summary, line);
        LoginOverlayCloseBehaviorTraceInspector.inspectLine(summary, line);
        LoginOverlayTabTraversalTraceInspector.inspectLine(summary, line);
    }
}
