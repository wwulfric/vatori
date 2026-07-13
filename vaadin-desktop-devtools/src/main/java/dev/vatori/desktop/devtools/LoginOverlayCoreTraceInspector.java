package dev.vatori.desktop.devtools;

final class LoginOverlayCoreTraceInspector {
    private LoginOverlayCoreTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginOverlayPropertyI18nTraceInspector.inspectLine(summary, line);
        LoginOverlayEventTraceInspector.inspectLine(summary, line);
        LoginOverlayLifecycleTraceInspector.inspectLine(summary, line);
    }
}
