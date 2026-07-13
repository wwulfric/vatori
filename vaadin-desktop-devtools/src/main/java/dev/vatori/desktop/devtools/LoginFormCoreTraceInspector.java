package dev.vatori.desktop.devtools;

final class LoginFormCoreTraceInspector {
    private LoginFormCoreTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginFormPropertyEventTraceInspector.inspectLine(summary, line);
        LoginFormSubmitRecoveryTraceInspector.inspectLine(summary, line);
        LoginFormLifecycleGuardTraceInspector.inspectLine(summary, line);
        LoginFormFocusA11yTraceInspector.inspectLine(summary, line);
    }
}
