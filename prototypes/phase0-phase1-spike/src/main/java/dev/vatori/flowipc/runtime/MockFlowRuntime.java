package dev.vatori.flowipc.runtime;

import dev.vatori.flowipc.spi.FlowTransport;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class MockFlowRuntime implements FlowTransport {
    private final AtomicInteger sessionSequence = new AtomicInteger(1);
    private final AtomicInteger uiSequence = new AtomicInteger(1);
    private final Map<String, UiState> sessions = new ConcurrentHashMap<>();

    @Override
    public BootstrapResult bootstrap(BootstrapRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? "session-" + sessionSequence.getAndIncrement()
                : request.sessionId();
        String uiId = "ui-" + uiSequence.getAndIncrement();
        String route = request.route() == null || request.route().isBlank() ? "/counter" : request.route();
        UiState state = new UiState(sessionId, uiId, route);
        sessions.put(key(sessionId, uiId), state);

        return new BootstrapResult(
                sessionId,
                uiId,
                route,
                List.of(new Patch("replace", "#app", renderView(state))),
                metadata("bootstrap"));
    }

    @Override
    public PatchResult dispatch(ClientEvent event) {
        UiState state = requireState(event.sessionId(), event.uiId());
        if ("counter.increment".equals(event.nodeId()) && "click".equals(event.eventType())) {
            int nextValue = state.counterView.onIncrementClicked();
            return new PatchResult(
                    state.sessionId,
                    state.uiId,
                    List.of(new Patch("setText", "#counter.value", mapOf("text", String.valueOf(nextValue)))),
                    false,
                    metadata("counter-click"));
        }

        if ("orders.grid".equals(event.nodeId()) && "requestRows".equals(event.eventType())) {
            int offset = intPayload(event.payload(), "offset", 0);
            int limit = intPayload(event.payload(), "limit", 3);
            List<Map<String, Object>> rows = state.ordersGridView.requestRows(offset, limit);
            return new PatchResult(
                    state.sessionId,
                    state.uiId,
                    List.of(new Patch("replaceRows", "#orders.grid", mapOf("rows", rows))),
                    false,
                    metadata("grid-request"));
        }

        throw new IllegalArgumentException("Unsupported event: nodeId=" + event.nodeId()
                + ", eventType=" + event.eventType());
    }

    @Override
    public PatchResult resync(ResyncRequest request) {
        UiState state = requireState(request.sessionId(), request.uiId());
        return new PatchResult(
                state.sessionId,
                state.uiId,
                List.of(new Patch("replace", "#app", renderView(state))),
                true,
                mapOf("phase", "resync", "reason", request.reason(), "at", Instant.now().toString()));
    }

    private UiState requireState(String sessionId, String uiId) {
        UiState state = sessions.get(key(sessionId, uiId));
        if (state == null) {
            throw new IllegalArgumentException("Unknown UI scope: sessionId=" + sessionId + ", uiId=" + uiId);
        }
        return state;
    }

    private static String key(String sessionId, String uiId) {
        return sessionId + "/" + uiId;
    }

    private static Map<String, Object> renderView(UiState state) {
        return mapOf(
                "kind", "MockFlowView",
                "route", state.route,
                "counter", state.counterView.render(),
                "grid", state.ordersGridView.render());
    }

    private static int intPayload(Map<String, Object> payload, String key, int fallback) {
        Object value = payload == null ? null : payload.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            return Integer.parseInt(text);
        }
        return fallback;
    }

    private static Map<String, Object> metadata(String phase) {
        return mapOf("phase", phase, "at", Instant.now().toString());
    }

    public static Map<String, Object> mapOf(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs.");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return map;
    }

    private static final class UiState {
        private final String sessionId;
        private final String uiId;
        private final String route;
        private final CounterView counterView = new CounterView();
        private final OrdersGridView ordersGridView = new OrdersGridView();

        private UiState(String sessionId, String uiId, String route) {
            this.sessionId = sessionId;
            this.uiId = uiId;
            this.route = route;
        }
    }
}
