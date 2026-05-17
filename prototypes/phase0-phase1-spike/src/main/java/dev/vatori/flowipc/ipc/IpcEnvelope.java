package dev.vatori.flowipc.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

public record IpcEnvelope(
        String type,
        String id,
        String correlationId,
        String sessionId,
        String uiId,
        Map<String, Object> payload,
        Map<String, Object> error) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("id", id);
        map.put("correlationId", correlationId);
        map.put("sessionId", sessionId);
        map.put("uiId", uiId);
        map.put("payload", payload == null ? Map.of() : payload);
        map.put("error", error);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static IpcEnvelope fromMap(Map<String, Object> map) {
        Object payload = map.get("payload");
        Object error = map.get("error");
        return new IpcEnvelope(
                stringValue(map.get("type")),
                stringValue(map.get("id")),
                stringValue(map.get("correlationId")),
                stringValue(map.get("sessionId")),
                stringValue(map.get("uiId")),
                payload instanceof Map<?, ?> ? (Map<String, Object>) payload : Map.of(),
                error instanceof Map<?, ?> ? (Map<String, Object>) error : null);
    }

    public static IpcEnvelope response(String type, IpcEnvelope request, String sessionId, String uiId,
                                       Map<String, Object> payload) {
        return new IpcEnvelope(type, request.id(), request.correlationId(), sessionId, uiId, payload, null);
    }

    public static IpcEnvelope error(IpcEnvelope request, String code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        return new IpcEnvelope(
                "error",
                request == null ? null : request.id(),
                request == null ? null : request.correlationId(),
                request == null ? null : request.sessionId(),
                request == null ? null : request.uiId(),
                Map.of(),
                error);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

