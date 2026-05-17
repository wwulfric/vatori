package dev.vatori.desktop.flow.transport;

import java.util.Map;

public record FlowPatchResponse(
        String sessionId,
        int uiId,
        String uidlJson,
        String contentType,
        int statusCode,
        Map<String, String> headers,
        boolean desktopVaadinRequestUsed,
        boolean resync) {
}
