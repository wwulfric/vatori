package dev.vatori.desktop.devtools;

final class LoginOverlayA11yTraceInspector {
    private LoginOverlayA11yTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (!line.contains("\"type\":\"webview.dom\"")) {
            return;
        }
        LoginOverlayAutofillAttributesTraceInspector.inspectLine(summary, line);
        LoginOverlayA11ySemanticsTraceInspector.inspectLine(summary, line);
        LoginOverlayErrorFocusRetryTraceInspector.inspectLine(summary, line);
    }
}
