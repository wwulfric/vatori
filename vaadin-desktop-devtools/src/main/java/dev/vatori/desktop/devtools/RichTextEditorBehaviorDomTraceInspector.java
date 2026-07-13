package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class RichTextEditorBehaviorDomTraceInspector {
    private RichTextEditorBehaviorDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixComplete", "true")
                && containsStringField(line,
                        "frameworkRichTextEditorBehaviorMatrixVersion",
                        "flow-ipc-rich-text-editor-behavior-matrix-v1_63")
                && (line.contains("Rich text behavior delta\\/html: Flow IPC rich text v1.63 bold=true")
                        || line.contains("Rich text behavior delta/html: Flow IPC rich text v1.63 bold=true"))) {
            summary.frameworkRichTextEditor.behaviorDomObserved = true;
            summary.frameworkRichTextEditor.behaviorMatrixV163DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixRendered", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixToolbarRendered", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixEditorApiObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixSelectionEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixSelectionEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixToolbarEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixToolbarEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixKeyboardEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixKeyboardEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixDeltaEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixDeltaEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixHtmlEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorBehaviorMatrixComplete", "true")
                && containsStringField(line,
                        "frameworkRichTextEditorBehaviorMatrixVersion",
                        "flow-ipc-rich-text-editor-behavior-matrix-v1_63")
                && line.contains("Rich text behavior delta\\/html: Flow IPC rich text v1.63 bold=true")) {
            summary.frameworkRichTextEditor.behaviorDomObserved = true;
            summary.frameworkRichTextEditor.behaviorMatrixV163DomObserved = true;
        }
    }
}
