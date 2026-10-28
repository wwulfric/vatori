package dev.vatori.desktop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DesktopCommandResult(boolean success, Map<String, Object> payload, String error) {
    public DesktopCommandResult {
        payload = payload == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static DesktopCommandResult ok(Map<String, Object> payload) {
        return new DesktopCommandResult(true, payload, null);
    }

    public static DesktopCommandResult error(String message) {
        return new DesktopCommandResult(false, Map.of(), message);
    }

    public static DesktopCommandResult error(Map<String, Object> payload, String message) {
        return new DesktopCommandResult(false, payload, message);
    }
}
