package dev.vatori.desktop.host;

import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.ipc.IpcTraceLogger;

import java.awt.Desktop;
import java.io.IOException;
import java.util.Map;

public final class CompatibilityDesktopHost implements DesktopHost {
    private final IpcTraceLogger trace = new IpcTraceLogger(System.out, "compatibility-host");
    private final boolean openBrowser;

    public CompatibilityDesktopHost() {
        this(Boolean.parseBoolean(System.getProperty("vaadin.desktop.openBrowser", "false")));
    }

    public CompatibilityDesktopHost(boolean openBrowser) {
        this.openBrowser = openBrowser;
    }

    @Override
    public void launch(DesktopContext context) {
        trace.trace("launch", "bootstrap", Map.of(
                "mode", "compatibility-http",
                "application", context.application().name(),
                "url", context.appUri().toString(),
                "window", context.application().mainWindow().title()));
        System.out.println("Vaadin Desktop compatibility mode");
        System.out.println("Application URL: " + context.appUri());
        System.out.println("This host uses local HTTP as a transition layer, not final Flow over IPC.");

        if (openBrowser && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(context.appUri());
                trace.trace("send", "host.openBrowser", Map.of("url", context.appUri().toString()));
            } catch (IOException error) {
                trace.trace("error", "host.openBrowser", Map.of("message", error.getMessage()));
            }
        }
    }
}

