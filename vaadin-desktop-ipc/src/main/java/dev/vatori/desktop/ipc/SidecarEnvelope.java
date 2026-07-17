package dev.vatori.desktop.ipc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record SidecarEnvelope(
        int version,
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
    public static final int VERSION = 1;

    public SidecarEnvelope {
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported sidecar envelope version: " + version);
        }
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static SidecarEnvelope message(String type, String id, String correlationId,
                                           String sessionId, String uiId, String windowId, String route,
                                           Map<String, Object> payload) {
        return new SidecarEnvelope(VERSION, type, id, correlationId, sessionId, uiId, windowId, route,
                payload, null, Instant.now().toEpochMilli());
    }

    public static SidecarEnvelope message(String type, String id, String correlationId,
                                           String sessionId, String uiId, String route,
                                           Map<String, Object> payload) {
        return message(type, id, correlationId, sessionId, uiId, uiId, route, payload);
    }

    public static SidecarEnvelope error(String id, String correlationId, String sessionId,
                                         String uiId, String windowId, String route,
                                         String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code == null ? "error" : code);
        error.put("message", message == null ? "" : message);
        return new SidecarEnvelope(VERSION, "error", id, correlationId, sessionId, uiId, windowId, route,
                Map.of(), error, Instant.now().toEpochMilli());
    }

    public static SidecarEnvelope error(String id, String correlationId, String sessionId,
                                         String uiId, String route, String code, String message) {
        return error(id, correlationId, sessionId, uiId, uiId, route, code, message);
    }
}
