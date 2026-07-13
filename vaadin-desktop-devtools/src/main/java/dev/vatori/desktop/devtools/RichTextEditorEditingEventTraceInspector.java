package dev.vatori.desktop.devtools;

final class RichTextEditorEditingEventTraceInspector {
    private RichTextEditorEditingEventTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-rich-text-editor")
                && line.contains("framework-rich-text-editor-editing-status")
                && line.contains("com.vaadin.flow.component.richtexteditor.RichTextEditor")
                && line.contains("com.vaadin.flow.component.richtexteditor.RichTextEditorVariant")) {
            summary.frameworkRichTextEditor.editingBootstrapObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"vatori-rich-text-editor-format\"")
                && line.contains("\"event.detail.formats\":\"italic,ordered-list,link,clear\"")
                && line.contains("\"event.detail.range\":\"5-17\"")) {
            summary.frameworkRichTextEditor.editingFormatEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"vatori-rich-text-editor-clipboard\"")
                && line.contains("\"event.detail.operation\":\"paste\"")
                && line.contains("Flow IPC rich text v1.64")) {
            summary.frameworkRichTextEditor.editingClipboardEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"vatori-rich-text-editor-link\"")
                && line.contains("https://vatori.dev/flow-ipc/v1_64")
                && line.contains("\"event.detail.list\":\"ordered\"")) {
            summary.frameworkRichTextEditor.editingLinkEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text editing format: italic,ordered-list,link,clear")
                && line.contains("range=5-17")) {
            summary.frameworkRichTextEditor.editingFormatPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text editing clipboard: paste")
                && line.contains("Flow IPC rich text v1.64")
                && line.contains("html=true")) {
            summary.frameworkRichTextEditor.editingClipboardPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text editing link/list: Flow IPC rich text v1.64")
                && line.contains("https://vatori.dev/flow-ipc/v1_64")
                && line.contains("list=ordered")) {
            summary.frameworkRichTextEditor.editingLinkPatchObserved = true;
        }
    }
}
