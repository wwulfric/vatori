package dev.vatori.desktop;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable native capability facade captured from a desktop execution
 * context. A captured instance can be passed explicitly to work on any
 * executor without carrying framework UI or session objects or relying on
 * implicit thread-local propagation.
 */
public final class DesktopCapabilityContext {
    private final NativeBridge nativeBridge;
    private final DesktopNotification notifications;
    private final DesktopClipboard clipboard;
    private final DesktopFileDialog fileDialog;
    private final DesktopFileTransfer fileTransfer;
    private final DesktopWindows windows;

    DesktopCapabilityContext(NativeBridge nativeBridge) {
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.notifications = new DesktopNotification(nativeBridge);
        this.clipboard = new DesktopClipboard(nativeBridge);
        this.fileDialog = new DesktopFileDialog(nativeBridge);
        this.fileTransfer = new DesktopFileTransfer(nativeBridge);
        this.windows = new DesktopWindows(nativeBridge);
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

    public NativeBridge nativeBridge() {
        return nativeBridge;
    }

    public DesktopCommandResult setWindowTitle(String title) {
        return nativeBridge.invoke(new DesktopCommand(
                "window.setTitle", Map.of("title", title)));
    }
}
