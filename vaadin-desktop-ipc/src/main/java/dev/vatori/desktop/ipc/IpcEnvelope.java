package dev.vatori.desktop.ipc;

import java.util.Map;

public record IpcEnvelope(
        String type,
        String id,
        String correlationId,
        String sessionId,
        String uiId,
        Map<String, Object> payload,
        Map<String, Object> error) {
    public IpcEnvelope {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static IpcEnvelope command(String id, String correlationId, String sessionId, String uiId,
                                      String command, Map<String, Object> payload) {
        return new IpcEnvelope("command", id, correlationId, sessionId, uiId,
                Map.of("command", command, "args", payload == null ? Map.of() : payload), null);
    }

    public static IpcEnvelope capabilityResult(IpcEnvelope request, Map<String, Object> payload) {
        return new IpcEnvelope("capabilityResult", request.id(), request.correlationId(), request.sessionId(),
                request.uiId(), payload, null);
    }

    public static IpcEnvelope error(IpcEnvelope request, String code, String message) {
        return new IpcEnvelope("error", request == null ? null : request.id(),
                request == null ? null : request.correlationId(),
                request == null ? null : request.sessionId(),
                request == null ? null : request.uiId(),
                Map.of(), Map.of("code", code, "message", message));
    }
}

