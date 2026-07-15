package dev.vatori.desktop.host;

import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.DesktopWindow;
import dev.vatori.desktop.ipc.IpcTraceLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class SystemWebViewDesktopHost implements DesktopHost {
    private static final String MACOS_HOST_RESOURCE = "/dev/vatori/desktop/host/macos-webview-host.swift";

    private final DesktopHost failureFallback;
    private final String fallbackName;
    private final IpcTraceLogger trace = new IpcTraceLogger(System.out, "system-webview-host");

    public SystemWebViewDesktopHost() {
        this(null, "disabled");
    }

    public SystemWebViewDesktopHost(DesktopHost failureFallback) {
        this(failureFallback, failureFallback == null ? "disabled" : "compatibility");
    }

    public SystemWebViewDesktopHost(DesktopHost failureFallback, String fallbackName) {
        this.failureFallback = failureFallback;
        this.fallbackName = fallbackName == null || fallbackName.isBlank() ? "enabled" : fallbackName;
    }

    @Override
    public void launch(DesktopContext context) {
        if (!isMacOs()) {
            handleLaunchFailure(context, "host.unsupportedPlatform",
                    Map.of("os", System.getProperty("os.name", "unknown")), null);
            return;
        }

        try {
            Process process = startMacOsHost(context);
            pumpHostOutput(process);
            watchHostExit(process);
        } catch (IOException error) {
            handleLaunchFailure(context, "host.start", Map.of("message", error.getMessage()), error);
        }
    }

    private void handleLaunchFailure(DesktopContext context, String event, Map<String, ?> details, Throwable cause) {
        trace.trace("error", event, withFallback(details));
        if (failureFallback != null) {
            failureFallback.launch(context);
            return;
        }
        throw new IllegalStateException(failureMessage(event, details), cause);
    }

    private Map<String, Object> withFallback(Map<String, ?> details) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.putAll(details);
        fields.put("fallback", fallbackName);
        return fields;
    }

    private String failureMessage(String event, Map<String, ?> details) {
        Object message = details.get("message");
        Object os = details.get("os");
        String detail = message != null ? message.toString() : "os=" + os;
        return "System WebView desktop host failed (" + event + "): " + detail
                + ". Set VAADIN_DESKTOP_HOST=compatibility/headless/browser for an explicit compatibility host, "
                + "or set VAADIN_DESKTOP_HOST_FALLBACK=compatibility to allow fallback.";
    }

    private Process startMacOsHost(DesktopContext context) throws IOException {
        Path executable = buildMacOsHostExecutable();
        DesktopWindow window = context.application().mainWindow();
        trace.trace("launch", "bootstrap", Map.of(
                "mode", "system-webview-compatibility-http",
                "application", context.application().name(),
                "url", context.appUri().toString(),
                "window", window.title()));
        System.out.println("Vaadin Desktop system WebView host");
        System.out.println("Application URL: " + context.appUri());
        System.out.println("Transport: compatibility HTTP. Final Flow over IPC is not enabled yet.");

        return new ProcessBuilder(
                executable.toString(),
                "--url", context.appUri().toString(),
                "--title", window.title(),
                "--width", Integer.toString(window.width()),
                "--height", Integer.toString(window.height()))
                .directory(executable.getParent().getParent().getParent().toFile())
                .redirectErrorStream(true)
                .start();
    }

    private Path buildMacOsHostExecutable() throws IOException {
        Path swiftc = Path.of("/usr/bin/swiftc");
        if (!Files.isExecutable(swiftc)) {
            throw new IOException("Swift compiler not found at " + swiftc);
        }

        Path hostRoot = Files.createTempDirectory("vaadin-desktop-macos-host");
        Path appBundle = hostRoot.resolve("VaadinDesktopHost.app");
        Path contents = appBundle.resolve("Contents");
        Path macOs = contents.resolve("MacOS");
        Path resources = contents.resolve("Resources");
        Files.createDirectories(macOs);
        Files.createDirectories(resources);
        Files.writeString(contents.resolve("Info.plist"), infoPlist(), StandardCharsets.UTF_8);

        Path source = resources.resolve("MacOSWebViewHost.swift");
        copyResource(MACOS_HOST_RESOURCE, source);

        Path executable = macOs.resolve("VaadinDesktopHost");
        Process compile = new ProcessBuilder(swiftc.toString(), source.toString(), "-o", executable.toString())
                .redirectErrorStream(true)
                .start();
        String output = readOutput(compile);
        int exitCode = waitFor(compile);
        if (exitCode != 0) {
            throw new IOException("Swift host compile failed with exit code " + exitCode + ":\n" + output);
        }
        return executable;
    }

    private String infoPlist() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0">
                <dict>
                    <key>CFBundleExecutable</key>
                    <string>VaadinDesktopHost</string>
                    <key>CFBundleIdentifier</key>
                    <string>dev.vatori.desktop.host</string>
                    <key>CFBundleName</key>
                    <string>Vaadin Desktop Host</string>
                    <key>CFBundlePackageType</key>
                    <string>APPL</string>
                    <key>LSMinimumSystemVersion</key>
                    <string>12.0</string>
                    <key>NSHighResolutionCapable</key>
                    <true/>
                    <key>NSAppTransportSecurity</key>
                    <dict>
                        <key>NSAllowsLocalNetworking</key>
                        <true/>
                    </dict>
                </dict>
                </plist>
                """;
    }

    private void copyResource(String resource, Path target) throws IOException {
        try (InputStream input = SystemWebViewDesktopHost.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Host resource not found: " + resource);
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void pumpHostOutput(Process process) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[vaadin-desktop-host] " + line);
                }
            } catch (IOException error) {
                trace.trace("error", "host.output", Map.of("message", error.getMessage()));
            }
        }, "vaadin-desktop-host-output");
        thread.setDaemon(true);
        thread.start();
    }

    private void watchHostExit(Process process) {
        Thread thread = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                trace.trace("recv", "host.exit", Map.of("exitCode", exitCode));
                if (Boolean.parseBoolean(System.getProperty("vaadin.desktop.shutdownOnHostExit", "true"))) {
                    System.exit(exitCode);
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                trace.trace("error", "host.exitInterrupted", Map.of("message", error.getMessage()));
            }
        }, "vaadin-desktop-host-exit");
        thread.setDaemon(true);
        thread.start();
    }

    private String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    private int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Swift host compilation.", error);
        }
    }

    private boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }
}
