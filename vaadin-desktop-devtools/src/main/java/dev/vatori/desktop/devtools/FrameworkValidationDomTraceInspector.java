package dev.vatori.desktop.devtools;

final class FrameworkValidationDomTraceInspector {
    private FrameworkValidationDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        FrameworkCoreDomTraceInspector.inspectLine(summary, line);
        FrameworkComponentMatrixDomTraceInspector.inspectLine(summary, line);
        FrameworkBehaviorResourceDomTraceInspector.inspectLine(summary, line);
    }
}
