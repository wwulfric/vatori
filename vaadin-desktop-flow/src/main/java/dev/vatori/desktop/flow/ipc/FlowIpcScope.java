package dev.vatori.desktop.flow.ipc;

import java.util.Objects;

record FlowIpcScope(String windowId, String sessionId, String uiId) {
    static FlowIpcScope from(
            FlowIpcInboundEnvelope envelope,
            String defaultSessionId,
            String defaultUiId) {
        String uiId = Objects.requireNonNullElse(envelope.uiId(), defaultUiId);
        String windowId = Objects.requireNonNullElse(envelope.windowId(), uiId);
        String sessionId = Objects.requireNonNullElse(
                envelope.sessionId(), defaultSessionId);
        validate("windowId", windowId);
        validate("sessionId", sessionId);
        validate("uiId", uiId);
        return new FlowIpcScope(windowId, sessionId, uiId);
    }

    private static void validate(String name, String value) {
        if (value.isBlank() || value.length() > 128
                || !value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("Invalid Flow IPC " + name);
        }
    }
}
