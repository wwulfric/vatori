package dev.vatori.flowipc.ipc;

import dev.vatori.flowipc.json.Json;

import java.util.Map;

public final class IpcCodec {
    private IpcCodec() {
    }

    public static String encode(IpcEnvelope envelope) {
        return Json.stringify(envelope.toMap());
    }

    public static IpcEnvelope decode(String line) {
        Object parsed = Json.parse(line);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected JSON object envelope.");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) map;
        return IpcEnvelope.fromMap(typed);
    }
}

