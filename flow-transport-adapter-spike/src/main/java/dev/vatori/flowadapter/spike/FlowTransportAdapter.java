package dev.vatori.flowadapter.spike;

/**
 * Draft shape for the first real Flow transport boundary.
 *
 * <p>This source set is a spike, not a compiled framework module yet. It is
 * based on the local Vaadin Flow source path documented in this directory's
 * README. The intended implementation target is the UIDL path around
 * UidlRequestHandler, ServerRpcHandler and UidlWriter.
 */
public interface FlowTransportAdapter {
    FlowPatch handleClientMessage(FlowClientMessage message);

    record FlowClientMessage(
            String sessionId,
            int uiId,
            String requestBody,
            String csrfToken,
            int clientToServerMessageId,
            boolean resync) {
    }

    record FlowPatch(
            String sessionId,
            int uiId,
            String uidlJson,
            boolean compatibilityRequestAdapterUsed) {
    }
}
