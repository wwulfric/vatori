package dev.vatori.desktop;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public final class DesktopFileDialog {
    private final NativeBridge nativeBridge;

    DesktopFileDialog(NativeBridge nativeBridge) {
        this.nativeBridge = nativeBridge;
    }

    public Optional<Path> openFile(String title) {
        DesktopCommandResult result = nativeBridge.invoke(new DesktopCommand("fileDialog.openFile",
                Map.of("title", title)));
        Object path = result.payload().get("path");
        return result.success() && path != null && !String.valueOf(path).isBlank()
                ? Optional.of(Path.of(String.valueOf(path)))
                : Optional.empty();
    }
}

