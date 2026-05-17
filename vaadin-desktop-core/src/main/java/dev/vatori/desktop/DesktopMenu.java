package dev.vatori.desktop;

import java.util.List;
import java.util.Map;

public final class DesktopMenu {
    private final NativeBridge nativeBridge;

    DesktopMenu(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public DesktopCommandResult setApplicationMenu(List<String> labels) {
        return nativeBridge.invoke(new DesktopCommand("menu.setApplicationMenu", Map.of("labels", labels)));
    }
}

