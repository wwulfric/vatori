package dev.vatori.desktop.flow.transport;

public record DesktopFlowPushMessage(
        String sessionId,
        String connectionId,
        int uiId,
        int flowServerSyncId,
        String uidl,
        boolean resync) {
}
