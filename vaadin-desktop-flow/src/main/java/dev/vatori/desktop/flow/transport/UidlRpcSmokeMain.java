package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.io.IOException;

public final class UidlRpcSmokeMain {
    private UidlRpcSmokeMain() {
    }

    public static void main(String[] args) throws IOException {
        Span status = new Span("Clicks: 0");
        status.setId("uidl-rpc-status");
        Button button = new Button("Increment through UIDL RPC", event -> status.setText("Clicks: 1"));
        button.setId("uidl-rpc-button");
        VerticalLayout root = new VerticalLayout(button, status);
        root.setId("uidl-rpc-root");

        DesktopFlowRuntimeSession session = DesktopFlowRuntimeSession.create(
                "uidl-rpc-session",
                1,
                "flow-ipc-uidl-rpc-smoke",
                root);
        String initialUidl = session.createUidl(false);
        if (!initialUidl.contains("uidl-rpc-button") || !initialUidl.contains("\"syncId\"")) {
            throw new IllegalStateException("Initial UIDL did not expose the RPC smoke button");
        }

        int buttonNodeId = button.getElement().getNode().getId();
        String requestBody = """
                {
                  "csrfToken": "%s",
                  "rpc": [
                    {
                      "type": "event",
                      "node": %d,
                      "event": "click",
                      "data": {}
                    }
                  ],
                  "syncId": -1,
                  "clientId": 0
                }
                """.formatted(session.ui().getCsrfToken(), buttonNodeId);

        FlowPatchResponse response = session.handleClientMessage(requestBody, 0, false);
        System.out.println(response.uidlJson());
        if (!response.desktopVaadinRequestUsed()
                || !response.uidlJson().contains("Clicks: 1")
                || !response.uidlJson().contains("\"syncId\"")) {
            throw new IllegalStateException("UIDL RPC smoke did not produce click listener patch");
        }
    }
}
