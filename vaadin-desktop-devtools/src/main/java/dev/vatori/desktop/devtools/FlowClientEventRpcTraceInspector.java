package dev.vatori.desktop.devtools;

final class FlowClientEventRpcTraceInspector {
    private FlowClientEventRpcTraceInspector() {
    }

    static void inspectLine(TraceSummary summary, String line) {
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"ipc-text-input\"")
                && line.contains("\"event\":\"change\"")
                && line.contains("element.value")
                && line.contains("Flow IPC typed")) {
            summary.flowClient.xhrTextInputObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"ipc-checkbox\"")
                && line.contains("\"event\":\"change\"")
                && line.contains("element.checked")) {
            summary.flowClient.xhrCheckboxObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"ipc-vaadin-big-decimal\"")
                && line.contains("\"event\":\"change\"")
                && line.contains("element.value")
                && line.contains("42.5")
                && line.contains("\"uidlRpc\"")) {
            summary.vaadinComponents.webComponentEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"ipc-vaadin-text-field\"")
                && line.contains("\"event\":\"change\"")
                && line.contains("element.value")
                && line.contains("Flow IPC typed")
                && line.contains("\"uidlRpc\"")) {
            summary.vaadinComponents.textFieldEventRpcObserved = true;
        }
        if (line.contains("\"type\":\"event\"")
                && line.contains("\"dispatch\":\"flow-client-xhr\"")
                && line.contains("\"componentId\":\"ipc-vaadin-checkbox\"")
                && line.contains("\"event\":\"change\"")
                && line.contains("element.checked")
                && line.contains("\"uidlRpc\"")) {
            summary.vaadinComponents.secondWebComponentEventRpcObserved = true;
        }
    }
}
