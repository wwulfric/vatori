package dev.vatori.desktop;

import java.util.Map;
import java.util.Optional;

public final class DesktopClipboard {
    private final NativeBridge nativeBridge;

    DesktopClipboard(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public DesktopCommandResult writeText(String text) {
        return nativeBridge.invoke(new DesktopCommand("clipboard.writeText", Map.of("text", text)));
    }

    public Optional<String> readText() {
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand("clipboard.readText", Map.of()));
        Object text = result.payload().get("text");
        return result.success() && text != null ? Optional.of(String.valueOf(text)) : Optional.empty();
    }
}

