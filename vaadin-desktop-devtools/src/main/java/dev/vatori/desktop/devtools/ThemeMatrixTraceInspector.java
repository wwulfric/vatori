package dev.vatori.desktop.devtools;

final class ThemeMatrixTraceInspector {
    private ThemeMatrixTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        ThemeMatrixCoreTraceInspector.inspectLine(summary, line);
        ThemeMatrixInputTraceInspector.inspectLine(summary, line);
        ThemeMatrixNavigationContentTraceInspector.inspectLine(summary, line);
        AuthenticationTraceInspector.inspectLine(summary, line);
    }
}
