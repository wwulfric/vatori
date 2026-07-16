package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.ipc.FlowIpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** The single synchronized VFIPC1 writer shared by every sidecar transport. */
final class SidecarOutput {
    private final PrintStream out;
    private final String route;
    private final FlowIpcScope defaultScope;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong serverSequence = new AtomicLong();
    private final Object outputLock = new Object();

    SidecarOutput(String route, FlowIpcScope defaultScope) {
        this(System.out, route, defaultScope);
    }

    SidecarOutput(PrintStream out, String route, FlowIpcScope defaultScope) {
        this.out = Objects.requireNonNull(out, "out");
        this.route = Objects.requireNonNullElse(route, "");
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
            write(FlowIpcEnvelope.message(
                    type,
                    id,
                    correlationId,
                    scope.sessionId(),
                    scope.uiId(),
                    scope.windowId(),
                    route,
                    sequencedPayload));
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
            write(FlowIpcEnvelope.error(
                    id == null ? nextId("error") : id,
                    correlationId,
                    scope.sessionId(),
                    scope.uiId(),
                    scope.windowId(),
                    route,
                    code,
                    message));
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
        payload.forEach((key, value) -> copy.put(key, Objects.requireNonNullElse(value, "")));
        return copy;
    }

    private void write(FlowIpcEnvelope envelope) {
        out.println(FlowIpcWire.PREFIX + IpcJson.stringify(envelope));
        out.flush();
    }
}
