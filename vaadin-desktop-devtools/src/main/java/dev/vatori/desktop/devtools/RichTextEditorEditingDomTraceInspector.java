package dev.vatori.desktop.devtools;

import static dev.vatori.desktop.devtools.TraceLineFields.*;

final class RichTextEditorEditingDomTraceInspector {
    private RichTextEditorEditingDomTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"webview.dom\"")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixComplete", "true")
                && containsStringField(line,
                        "frameworkRichTextEditorEditingMatrixVersion",
                        "flow-ipc-rich-text-editor-editing-matrix-v1_64")
                && line.contains("Rich text editing link\\/list: Flow IPC rich text v1.64")
                && line.contains("https:\\/\\/vatori.dev\\/flow-ipc\\/v1_64")) {
            summary.frameworkRichTextEditor.editingDomObserved = true;
            summary.frameworkRichTextEditor.editingMatrixV164DomObserved = true;
        }
        if (line.contains("\"type\":\"webview.dom\"")
                && line.contains("Flow IPC Framework Desk")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixRendered", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixEditorApiObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixFormatEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixFormatEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixClipboardEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixClipboardEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixLinkEventRpcObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixLinkEventObserved", "true")
                && containsLiteralField(line,
                        "frameworkRichTextEditorEditingMatrixComplete", "true")
                && containsStringField(line,
                        "frameworkRichTextEditorEditingMatrixVersion",
                        "flow-ipc-rich-text-editor-editing-matrix-v1_64")) {
            summary.frameworkRichTextEditor.editingDomObserved = true;
            summary.frameworkRichTextEditor.editingMatrixV164DomObserved = true;
        }
    }
}
