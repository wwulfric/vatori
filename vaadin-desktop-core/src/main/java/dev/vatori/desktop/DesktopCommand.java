package dev.vatori.desktop;

import java.util.Map;

public record DesktopCommand(String name, Map<String, Object> payload) {
    public DesktopCommand {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

