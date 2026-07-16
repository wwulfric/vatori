package dev.vatori.desktop.ipc;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IpcJson {
    private IpcJson() {
    }

    public static String stringify(Object value) {
        StringBuilder json = new StringBuilder();
        append(json, value);
        return json.toString();
    }

    private static void append(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String string) {
            appendString(json, string);
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof FlowIpcEnvelope envelope) {
            appendFlowEnvelope(json, envelope);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(json, map);
        } else if (value instanceof Iterable<?> iterable) {
            appendIterable(json, iterable.iterator());
        } else if (value.getClass().isArray()) {
            appendArray(json, value);
        } else {
            appendString(json, String.valueOf(value));
        }
    }

    private static void appendFlowEnvelope(StringBuilder json, FlowIpcEnvelope envelope) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("version", envelope.version());
        values.put("type", envelope.type());
        values.put("id", envelope.id());
        values.put("correlationId", envelope.correlationId());
        values.put("sessionId", envelope.sessionId());
        values.put("uiId", envelope.uiId());
        values.put("windowId", envelope.windowId());
        values.put("route", envelope.route());
        values.put("payload", envelope.payload());
        values.put("error", envelope.error());
        values.put("timestamp", envelope.timestamp());
        appendMap(json, values);
    }

    private static void appendMap(StringBuilder json, Map<?, ?> map) {
        json.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            appendString(json, String.valueOf(entry.getKey()));
            json.append(':');
            append(json, entry.getValue());
        }
        json.append('}');
    }

    private static void appendIterable(StringBuilder json, Iterator<?> iterator) {
        json.append('[');
        boolean first = true;
        while (iterator.hasNext()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            append(json, iterator.next());
        }
        json.append(']');
    }

    private static void appendArray(StringBuilder json, Object array) {
        json.append('[');
        int length = Array.getLength(array);
        for (int index = 0; index < length; index++) {
            if (index > 0) {
                json.append(',');
            }
            append(json, Array.get(array, index));
        }
        json.append(']');
    }

    private static void appendString(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }
}
