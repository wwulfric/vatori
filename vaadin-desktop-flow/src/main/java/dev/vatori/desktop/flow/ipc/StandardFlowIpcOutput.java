package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.ipc.FlowIpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

final class StandardFlowIpcOutput {
    private final String route;
    private final FlowIpcScope defaultScope;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong serverSequence = new AtomicLong();
    private final Object outputLock = new Object();

    StandardFlowIpcOutput(String route, FlowIpcScope defaultScope) {
        this.route = Objects.requireNonNull(route, "route");
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
    }

    long send(
            String type,
            String id,
            String correlationId,
            Map<String, Object> payload) {
        return send(defaultScope, type, id, correlationId, payload);
    }

    long send(
            FlowIpcScope scope,
            String type,
            String id,
            String correlationId,
            Map<String, Object> payload) {
        synchronized (outputLock) {
            Map<String, Object> sequencedPayload = nullSafePayload(payload);
            long nextServerSequence = serverSequence.incrementAndGet();
            sequencedPayload.put("serverSequence", nextServerSequence);
            sequencedPayload.put("serverSequenceScope", "java-sidecar-to-webview");
            FlowIpcEnvelope envelope = FlowIpcEnvelope.message(
                    type,
                    id,
                    correlationId,
                    scope.sessionId(),
                    scope.uiId(),
                    scope.windowId(),
                    route,
                    sequencedPayload);
            write(envelope);
            return nextServerSequence;
        }
    }

    void sendError(
            FlowIpcScope scope,
            String code,
            String message,
            String id,
            String correlationId) {
        synchronized (outputLock) {
            FlowIpcEnvelope envelope = FlowIpcEnvelope.error(
                    id == null ? nextId("error") : id,
                    correlationId,
                    scope.sessionId(),
                    scope.uiId(),
                    scope.windowId(),
                    route,
                    code,
                    message);
            write(envelope);
        }
    }

    String nextId(String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }

    static String correlationIdOr(String id, String correlationId) {
        return correlationId == null || correlationId.isBlank() ? id : correlationId;
    }

    private static Map<String, Object> nullSafePayload(Map<String, Object> payload) {
        Map<String, Object> copy = new LinkedHashMap<>();
        payload.forEach((key, value) ->
                copy.put(key, Objects.requireNonNullElse(value, "")));
        return copy;
    }

    private static void write(FlowIpcEnvelope envelope) {
        System.out.println(FlowIpcWire.PREFIX + IpcJson.stringify(envelope));
        System.out.flush();
    }
}
