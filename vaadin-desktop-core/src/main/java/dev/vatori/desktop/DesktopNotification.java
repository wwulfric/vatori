package dev.vatori.desktop;

import java.util.Map;

public final class DesktopNotification {
    private final NativeBridge nativeBridge;

    DesktopNotification(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public DesktopCommandResult show(String title, String body) {
        return nativeBridge.invoke(new DesktopCommand("notification.show", Map.of(
                "title", title,
                "body", body)));
    }
}

