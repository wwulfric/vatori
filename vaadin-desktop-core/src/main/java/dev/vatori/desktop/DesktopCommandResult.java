package dev.vatori.desktop;

import java.util.Map;

public record DesktopCommandResult(boolean success, Map<String, Object> payload, String error) {
    public static DesktopCommandResult ok(Map<String, Object> payload) {
        return new DesktopCommandResult(true, payload == null ? Map.of() : Map.copyOf(payload), null);
    }

    public static DesktopCommandResult error(String message) {
        return new DesktopCommandResult(false, Map.of(), message);
    }
}

