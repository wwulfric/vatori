package dev.vatori.flowipc.spi;

import java.util.List;
import java.util.Map;

public interface FlowTransport {
    BootstrapResult bootstrap(BootstrapRequest request);

    PatchResult dispatch(ClientEvent event);

    PatchResult resync(ResyncRequest request);

    default void push(ServerPush push) {
        throw new UnsupportedOperationException("Push is reserved for a later spike.");
    }

    record BootstrapRequest(String route, String sessionId, Map<String, Object> capabilities) {
    }

    record BootstrapResult(
            String sessionId,
            String uiId,
            String route,
            List<Patch> patches,
            Map<String, Object> metadata) {
    }

    record ClientEvent(
            String sessionId,
            String uiId,
            String nodeId,
            String eventType,
            Map<String, Object> payload) {
    }

    record ResyncRequest(String sessionId, String uiId, String reason) {
    }

    record ServerPush(String sessionId, String uiId, List<Patch> patches) {
    }

    record Patch(String op, String target, Map<String, Object> value) {
    }

    record PatchResult(
            String sessionId,
            String uiId,
            List<Patch> patches,
            boolean fullResync,
            Map<String, Object> metadata) {
    }
}

