package dev.vatori.desktop.devtools;

final class LoginOverlayGuardSubmitTraceInspector {
    private LoginOverlayGuardSubmitTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginOverlayDisabledGuardTraceInspector.inspectLine(summary, line);
        LoginOverlaySubmitTraceInspector.inspectLine(summary, line);
    }
}
