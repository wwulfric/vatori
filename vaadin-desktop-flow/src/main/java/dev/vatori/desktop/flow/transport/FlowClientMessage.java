package dev.vatori.desktop.flow.transport;

public record FlowClientMessage(
        String sessionId,
        int uiId,
        String route,
        String requestBody,
        String csrfToken,
        int clientToServerMessageId,
        boolean resync) {
}
