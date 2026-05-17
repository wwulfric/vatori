package dev.vatori.desktop.host;

import java.util.Locale;

public final class DesktopHosts {
    private DesktopHosts() {
    }

    public static DesktopHost defaultHost() {
        String requested = firstNonBlank(
                System.getProperty("vaadin.desktop.host"),
                System.getenv("VAADIN_DESKTOP_HOST"),
                "system-webview");
        return switch (requested.toLowerCase(Locale.ROOT)) {
            case "system-webview", "webview", "native" -> new SystemWebViewDesktopHost(new CompatibilityDesktopHost());
            case "compatibility", "headless", "console" -> new CompatibilityDesktopHost(false);
            case "browser" -> new CompatibilityDesktopHost(true);
            default -> throw new IllegalArgumentException("Unsupported Vaadin desktop host: " + requested);
        };
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }
}
