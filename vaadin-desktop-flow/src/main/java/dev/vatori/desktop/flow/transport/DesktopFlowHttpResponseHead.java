package dev.vatori.desktop.flow.transport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DesktopFlowHttpResponseHead(
        int statusCode,
        String statusText,
        Map<String, List<String>> headers) {

    public DesktopFlowHttpResponseHead {
        Map<String, List<String>> copiedHeaders = new LinkedHashMap<>();
        headers.forEach((name, values) -> copiedHeaders.put(name, List.copyOf(values)));
        headers = Map.copyOf(copiedHeaders);
    }
}
