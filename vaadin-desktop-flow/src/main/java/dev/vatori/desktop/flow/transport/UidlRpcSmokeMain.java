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
        String initialUidl = session.createBootstrapUidl();
        if (!initialUidl.contains("uidl-rpc-button") || !initialUidl.contains("\"syncId\"")
                || !initialUidl.contains("\"Vaadin-Security-Key\"")) {
            throw new IllegalStateException("Initial UIDL did not expose the RPC smoke button");
        }

        int buttonNodeId = button.getElement().getNode().getId();
        FlowPatchResponse response = session.handleDomEvent(buttonNodeId, "click", 0);
        System.out.println(response.uidlJson());
        if (!response.desktopVaadinRequestUsed()
                || !response.uidlJson().contains("Clicks: 1")
                || !response.uidlJson().contains("\"syncId\"")) {
            throw new IllegalStateException("UIDL RPC smoke did not produce click listener patch");
        }

        String invalidCsrfBody = eventBody("invalid-csrf-token", session.securityKey(), buttonNodeId, -1, 1);
        try {
            session.handleClientMessage(invalidCsrfBody, 1, false);
            throw new IllegalStateException("UIDL RPC smoke accepted an invalid csrfToken");
        } catch (SecurityException expected) {
            System.out.println("invalid csrfToken rejected");
        }

        String invalidSecurityKeyBody = eventBody(session.csrfToken(), "invalid-security-key", buttonNodeId, -1, 1);
        try {
            session.handleClientMessage(invalidSecurityKeyBody, 1, false);
            throw new IllegalStateException("UIDL RPC smoke accepted an invalid securityKey");
        } catch (SecurityException expected) {
            System.out.println("invalid securityKey rejected");
        }

        String staleSyncBody = eventBody(session.csrfToken(), session.securityKey(), buttonNodeId, -2, 1);
        try {
            session.handleClientMessage(staleSyncBody, 1, false);
            throw new IllegalStateException("UIDL RPC smoke accepted a stale syncId");
        } catch (IllegalStateException expected) {
            System.out.println("stale syncId rejected");
        }

        String outOfOrderClientBody = eventBody(session.csrfToken(), session.securityKey(), buttonNodeId, -1, 3);
        try {
            session.handleClientMessage(outOfOrderClientBody, 3, false);
            throw new IllegalStateException("UIDL RPC smoke accepted an out-of-order clientId");
        } catch (IllegalStateException expected) {
            System.out.println("out-of-order clientId rejected");
        }
    }

    private static String eventBody(String csrfToken, String securityKey, int buttonNodeId, int syncId, int clientId) {
        return """
                {
                  "csrfToken": "%s",
                  "securityKey": "%s",
                  "rpc": [
                    {
                      "type": "event",
                      "node": %d,
                      "event": "click",
                      "data": {}
                    }
                  ],
                  "syncId": %d,
                  "clientId": %d
                }
                """.formatted(csrfToken, securityKey, buttonNodeId, syncId, clientId);
    }
}
