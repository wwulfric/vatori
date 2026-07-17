package dev.vatori.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DesktopWindows {
    private final NativeBridge nativeBridge;

    DesktopWindows(NativeBridge nativeBridge) {
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
    }

    public DesktopCommandResult open(DesktopWindow window) {
        Objects.requireNonNull(window, "window");
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("id", window.id());
        descriptor.put("title", window.title());
        descriptor.put("width", window.width());
        descriptor.put("height", window.height());
        descriptor.put("route", window.route());
        return nativeBridge.invoke(new DesktopCommand("window.open", descriptor));
    }

    public DesktopCommandResult close(String windowId) {
        if (windowId == null || windowId.isBlank()) {
            throw new IllegalArgumentException("Window id must not be blank");
        }
        return nativeBridge.invoke(new DesktopCommand(
                "window.close", Map.of("id", windowId)));
    }
}
