package dev.vatori.desktop.host;

import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;
import dev.vatori.desktop.ipc.IpcTraceLogger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class CompatibilityNativeBridge implements NativeBridge {
    private final IpcTraceLogger trace = new IpcTraceLogger(System.out, "compatibility-native-bridge");
    private final AtomicReference<String> clipboard = new AtomicReference<>("");

    @Override
    public DesktopCommandResult invoke(DesktopCommand command) {
        trace.trace("recv", "command", Map.of("name", command.name(), "payload", command.payload()));
        return switch (command.name()) {
            case "notification.show" -> ok("notification.logged", command.payload());
            case "clipboard.writeText" -> {
                clipboard.set(String.valueOf(command.payload().getOrDefault("text", "")));
                yield ok("clipboard.writeText", Map.of("stored", true));
            }
            case "clipboard.readText" -> ok("clipboard.readText", Map.of("text", clipboard.get()));
            case "fileDialog.openFile" -> ok("fileDialog.openFile", Map.of("path", ""));
            case "menu.setApplicationMenu" -> ok("menu.setApplicationMenu", command.payload());
            case "window.setTitle" -> ok("window.setTitle", command.payload());
            default -> DesktopCommandResult.error("Unsupported desktop command in compatibility host: " + command.name());
        };
    }

    private DesktopCommandResult ok(String type, Map<String, Object> payload) {
        trace.trace("send", type, payload);
        return DesktopCommandResult.ok(payload);
    }
}

