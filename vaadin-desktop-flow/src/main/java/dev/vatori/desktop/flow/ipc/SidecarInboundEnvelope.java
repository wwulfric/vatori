package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.ipc.SidecarEnvelope;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class SidecarInboundEnvelope {
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private final JsonNode root;
    private final JsonNode payload;
    private final JsonNode error;

    private SidecarInboundEnvelope(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new SidecarProtocolException("Sidecar envelope must be a JSON object");
        }
        JsonNode version = root.get("version");
        if (version == null || !version.isIntegralNumber()) {
            throw new SidecarProtocolException(
                    "Sidecar envelope version must be the number " + SidecarEnvelope.VERSION);
        }
        if (version.asInt() != SidecarEnvelope.VERSION) {
            throw new SidecarProtocolException(
                    "Unsupported sidecar envelope version: " + version.asText());
        }
        this.root = root;
        JsonNode payloadNode = root.get("payload");
        this.payload = payloadNode != null && payloadNode.isObject() ? payloadNode : null;
        JsonNode errorNode = root.get("error");
        this.error = errorNode != null && errorNode.isObject() ? errorNode : null;
    }

    static SidecarInboundEnvelope parse(String json) {
        try {
            return new SidecarInboundEnvelope(JSON.readTree(json));
        } catch (JacksonException error) {
            throw new SidecarProtocolException("Invalid sidecar JSON envelope", error);
        }
    }

    String type() {
        return topLevelString("type");
    }

    String id() {
        return topLevelString("id");
    }

    String correlationId() {
        return topLevelString("correlationId");
    }

    String sessionId() {
        return topLevelString("sessionId");
    }

    String uiId() {
        return topLevelString("uiId");
    }

    String windowId() {
        return topLevelString("windowId");
    }

    String string(String field) {
        return stringValue(field(field));
    }

    String errorString(String field) {
        String value = error == null ? null : stringValue(error.get(field));
        return value == null ? string(field) : value;
    }

    int integer(String field, int fallback) {
        return integerValue(field(field), fallback);
    }

    long longValue(String field, long fallback) {
        return longValue(field(field), fallback);
    }

    boolean booleanValue(String field, boolean fallback) {
        JsonNode node = field(field);
        return node != null && node.isBoolean() ? node.asBoolean() : fallback;
    }

    String objectJson(String field) {
        JsonNode node = field(field);
        if (node == null || !node.isObject()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(node);
        } catch (JacksonException error) {
            throw new IllegalArgumentException("Unable to serialize sidecar field " + field, error);
        }
    }

    Map<String, String> stringMap(String field) {
        JsonNode node = field(field);
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.properties().forEach(entry -> {
            JsonNode value = entry.getValue();
            if (value != null && value.isString()) {
                values.put(entry.getKey(), value.stringValue());
            }
        });
        return Map.copyOf(values);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> payloadMap() {
        if (payload == null) {
            return Map.of();
        }
        Map<String, Object> values = JSON.convertValue(payload, Map.class);
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    int objectInteger(String objectField, String nestedField, int fallback) {
        JsonNode object = field(objectField);
        if (object == null || !object.isObject()) {
            return fallback;
        }
        return integerValue(object.get(nestedField), fallback);
    }

    private String topLevelString(String field) {
        return stringValue(root.get(field));
    }

    private JsonNode field(String field) {
        JsonNode node = payload == null ? null : payload.get(field);
        if (hasValue(node)) {
            return node;
        }
        node = root.get(field);
        return hasValue(node) ? node : null;
    }

    private static boolean hasValue(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull();
    }

    private static String stringValue(JsonNode node) {
        return node != null && node.isString() ? node.stringValue() : null;
    }

    private static int integerValue(JsonNode node, int fallback) {
        return node != null && node.isIntegralNumber() ? node.asInt() : fallback;
    }

    private static long longValue(JsonNode node, long fallback) {
        return node != null && node.isIntegralNumber() ? node.asLong() : fallback;
    }
}

final class SidecarProtocolException extends IllegalArgumentException {
    SidecarProtocolException(String message) {
        super(message);
    }

    SidecarProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
