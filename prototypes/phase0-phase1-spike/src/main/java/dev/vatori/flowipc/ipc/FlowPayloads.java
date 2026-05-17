package dev.vatori.flowipc.ipc;

import dev.vatori.flowipc.spi.FlowTransport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FlowPayloads {
    private FlowPayloads() {
    }

    public static Map<String, Object> bootstrapPayload(FlowTransport.BootstrapResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("route", result.route());
        payload.put("patches", patches(result.patches()));
        payload.put("metadata", result.metadata());
        return payload;
    }

    public static Map<String, Object> patchPayload(FlowTransport.PatchResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullResync", result.fullResync());
        payload.put("patches", patches(result.patches()));
        payload.put("metadata", result.metadata());
        return payload;
    }

    public static FlowTransport.ClientEvent clientEvent(IpcEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return new FlowTransport.ClientEvent(
                envelope.sessionId(),
                envelope.uiId(),
                stringValue(payload.get("nodeId")),
                stringValue(payload.get("eventType")),
                nestedPayload(payload.get("payload")));
    }

    public static FlowTransport.BootstrapRequest bootstrapRequest(IpcEnvelope envelope) {
        return new FlowTransport.BootstrapRequest(
                stringValue(envelope.payload().get("route")),
                envelope.sessionId(),
                envelope.payload());
    }

    public static FlowTransport.ResyncRequest resyncRequest(IpcEnvelope envelope) {
        return new FlowTransport.ResyncRequest(
                envelope.sessionId(),
                envelope.uiId(),
                stringValue(envelope.payload().get("reason")));
    }

    private static List<Map<String, Object>> patches(List<FlowTransport.Patch> patches) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FlowTransport.Patch patch : patches) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("op", patch.op());
            map.put("target", patch.target());
            map.put("value", patch.value());
            list.add(map);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nestedPayload(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}

