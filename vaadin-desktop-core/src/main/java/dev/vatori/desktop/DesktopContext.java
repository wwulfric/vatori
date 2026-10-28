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
    private final DesktopFileTransfer fileTransfer;
    private final DesktopWindows windows;
    private final DesktopResources resources;

    public DesktopContext(DesktopApplication application, URI appUri, NativeBridge nativeBridge,
                          Map<String, String> launchArguments) {
        this.application = application;
        this.appUri = appUri;
        this.nativeBridge = nativeBridge;
        this.launchArguments = Map.copyOf(launchArguments);
        this.notifications = new DesktopNotification(nativeBridge);
        this.clipboard = new DesktopClipboard(nativeBridge);
        this.fileDialog = new DesktopFileDialog(nativeBridge);
        this.fileTransfer = new DesktopFileTransfer(nativeBridge);
        this.windows = new DesktopWindows(nativeBridge);
        this.resources = DesktopResources.fromLaunchEnvironment();
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

    public DesktopFileTransfer fileTransfer() {
        return fileTransfer;
    }

    public DesktopWindows windows() {
        return windows;
    }

    public DesktopResources resources() {
        return resources;
    }

    /**
     * Captures the current native capability context for use after execution
     * leaves the current framework callback. Flow applications should call this
     * from a Flow listener or {@code UI.access} callback, then pass the returned
     * object explicitly to work running on any executor.
     *
     * @return an immutable capability facade bound to the current context
     */
    public DesktopCapabilityContext captureCapabilityContext() {
        return new DesktopCapabilityContext(
                nativeBridge.captureCapabilityContext());
    }

    public DesktopCommandResult setWindowTitle(String title) {
        return nativeBridge.invoke(new DesktopCommand("window.setTitle", Map.of("title", title)));
    }
}
