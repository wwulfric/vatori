package dev.vatori.desktop.devtools;

final class RichTextEditorBehaviorEventTraceInspector {
    private RichTextEditorBehaviorEventTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("dev.vatori.ipcframework.FlowOverIpcFrameworkView")
                && line.contains("framework-rich-text-editor")
                && line.contains("framework-rich-text-editor-behavior-status")
                && line.contains("com.vaadin.flow.component.richtexteditor.RichTextEditor")
                && line.contains("com.vaadin.flow.component.richtexteditor.RichTextEditorVariant")) {
            summary.frameworkRichTextEditor.behaviorBootstrapObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"vatori-rich-text-editor-selection\"")
                && line.contains("\"event.detail.index\":0")
                && line.contains("\"event.detail.length\":8")) {
            summary.frameworkRichTextEditor.behaviorSelectionEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"vatori-rich-text-editor-toolbar\"")
                && line.contains("\"event.detail.format\":\"bold\"")) {
            summary.frameworkRichTextEditor.behaviorToolbarEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"keydown\"")
                && line.contains("\"event.key\":\"b\"")
                && line.contains("\"event.metaKey\":true")) {
            summary.frameworkRichTextEditor.behaviorKeyboardEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"framework-rich-text-editor\"")
                && line.contains("\"event\":\"value-changed\"")
                && line.contains("Flow IPC rich text v1.63")
                && line.contains("bold")) {
            summary.frameworkRichTextEditor.behaviorDeltaEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text behavior selection: 0-8")) {
            summary.frameworkRichTextEditor.behaviorSelectionPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text behavior toolbar: bold")) {
            summary.frameworkRichTextEditor.behaviorToolbarPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text behavior keyboard: b")) {
            summary.frameworkRichTextEditor.behaviorKeyboardPatchObserved = true;
        }
        if (line.contains("\"type\":\"patch\"")
                && line.contains("\"source\":\"UidlRequestHandler\"")
                && line.contains("\"reason\":\"event\"")
                && line.contains("Rich text behavior delta/html: Flow IPC rich text v1.63")
                && line.contains("bold=true")) {
            summary.frameworkRichTextEditor.behaviorDeltaPatchObserved = true;
        }
    }
}
