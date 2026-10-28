package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class DesktopClipboard {
    private final NativeBridge nativeBridge;

    DesktopClipboard(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public DesktopCommandResult writeText(String text) {
        return writeTextResult(text).commandResult();
    }

    public DesktopClipboardWriteResult writeTextResult(String text) {
        Objects.requireNonNull(text, "text");
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand(
                "clipboard.writeText", Map.of("text", text)));
        return DesktopClipboardWriteResult.from(result);
    }

    public Optional<String> readText() {
        return readTextResult().text();
    }

    public DesktopClipboardReadResult readTextResult() {
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand(
                "clipboard.readText", Map.of()));
        return DesktopClipboardReadResult.from(result);
    }
}
