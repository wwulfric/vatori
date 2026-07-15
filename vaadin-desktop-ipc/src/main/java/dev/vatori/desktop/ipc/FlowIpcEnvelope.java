package dev.vatori.desktop.ipc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record FlowIpcEnvelope(
        String version,
        String type,
        String id,
        String correlationId,
        String sessionId,
        String uiId,
        String windowId,
        String route,
        Map<String, Object> payload,
        Map<String, Object> error,
        long timestamp) {
    public static final String VERSION = "flow-ipc-v1";

    public FlowIpcEnvelope {
        version = version == null ? VERSION : version;
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static FlowIpcEnvelope message(String type, String id, String correlationId,
                                          String sessionId, String uiId, String windowId, String route,
                                          Map<String, Object> payload) {
        return new FlowIpcEnvelope(VERSION, type, id, correlationId, sessionId, uiId, windowId, route,
                payload, null, Instant.now().toEpochMilli());
    }

    public static FlowIpcEnvelope message(String type, String id, String correlationId,
                                          String sessionId, String uiId, String route,
                                          Map<String, Object> payload) {
        return message(type, id, correlationId, sessionId, uiId, uiId, route, payload);
    }

    public static FlowIpcEnvelope error(String id, String correlationId, String sessionId,
                                        String uiId, String windowId, String route,
                                        String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code == null ? "error" : code);
        error.put("message", message == null ? "" : message);
        return new FlowIpcEnvelope(VERSION, "error", id, correlationId, sessionId, uiId, windowId, route,
                Map.of(), error, Instant.now().toEpochMilli());
    }

    public static FlowIpcEnvelope error(String id, String correlationId, String sessionId,
                                        String uiId, String route, String code, String message) {
        return error(id, correlationId, sessionId, uiId, uiId, route, code, message);
    }
}
