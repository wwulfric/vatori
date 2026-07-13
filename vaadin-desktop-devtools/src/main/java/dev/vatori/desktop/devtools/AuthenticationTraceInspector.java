package dev.vatori.desktop.devtools;

final class AuthenticationTraceInspector {
    private AuthenticationTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginFormCoreTraceInspector.inspectLine(summary, line);
        LoginOverlayCoreTraceInspector.inspectLine(summary, line);
        LoginOverlayFocusTraceInspector.inspectLine(summary, line);
        LoginOverlayGuardSubmitTraceInspector.inspectLine(summary, line);
        LoginOverlayA11yTraceInspector.inspectLine(summary, line);
        LoginFormErrorTraceInspector.inspectLine(summary, line);
    }
}
