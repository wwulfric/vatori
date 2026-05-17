package dev.vatori.desktop;

import java.net.URI;
import java.util.Map;

public final class DesktopContext {
    private final DesktopApplication application;
    private final URI appUri;
    private final NativeBridge nativeBridge;
    private final Map<String, String> launchArguments;
    private final DesktopNotification notifications;
    private final DesktopClipboard clipboard;
    private final DesktopFileDialog fileDialog;
    private final DesktopMenu menu;

    public DesktopContext(DesktopApplication application, URI appUri, NativeBridge nativeBridge,
                          Map<String, String> launchArguments) {
        this.application = application;
        this.appUri = appUri;
        this.nativeBridge = nativeBridge;
        this.launchArguments = Map.copyOf(launchArguments);
        this.notifications = new DesktopNotification(nativeBridge);
        this.clipboard = new DesktopClipboard(nativeBridge);
        this.fileDialog = new DesktopFileDialog(nativeBridge);
        this.menu = new DesktopMenu(nativeBridge);
    }

    public DesktopApplication application() {
        return application;
    }

    public URI appUri() {
        return appUri;
    }

    public NativeBridge nativeBridge() {
        return nativeBridge;
    }

    public Map<String, String> launchArguments() {
        return launchArguments;
    }

    public DesktopNotification notifications() {
        return notifications;
    }

    public DesktopClipboard clipboard() {
        return clipboard;
    }

    public DesktopFileDialog fileDialog() {
        return fileDialog;
    }

    public DesktopMenu menu() {
        return menu;
    }

    public DesktopCommandResult setWindowTitle(String title) {
        return nativeBridge.invoke(new DesktopCommand("window.setTitle", Map.of("title", title)));
    }
}

