package dev.vatori.desktop.devtools;

final class LoginFormErrorTraceInspector {
    private LoginFormErrorTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginFormErrorMessageTraceInspector.inspectLine(summary, line);
        LoginFormErrorFocusTraceInspector.inspectLine(summary, line);
        LoginFormErrorTargetDiagnosticsTraceInspector.inspectLine(summary, line);
        LoginFormVisibleOwnerFocusTraceInspector.inspectLine(summary, line);
    }
}
