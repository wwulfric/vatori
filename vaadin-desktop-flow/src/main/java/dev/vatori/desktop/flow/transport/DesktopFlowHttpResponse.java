package dev.vatori.desktop.flow.transport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DesktopFlowHttpResponse(
        int statusCode,
        String statusText,
        Map<String, List<String>> headers,
        byte[] body) {

    public DesktopFlowHttpResponse {
        Map<String, List<String>> copiedHeaders = new LinkedHashMap<>();
        headers.forEach((name, values) -> copiedHeaders.put(name, List.copyOf(values)));
        headers = Map.copyOf(copiedHeaders);
        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
