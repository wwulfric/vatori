package dev.vatori.desktop.devtools;

final class LoginFormErrorTargetDiagnosticsTraceInspector {
    private LoginFormErrorTargetDiagnosticsTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        LoginFormErrorTargetStateDiagnosticsTraceInspector.inspectLine(summary, line);
        LoginFormErrorTargetLayoutDiagnosticsTraceInspector.inspectLine(summary, line);
        LoginFormErrorTargetHiddenSourceDiagnosticsTraceInspector.inspectLine(summary, line);
    }
}
