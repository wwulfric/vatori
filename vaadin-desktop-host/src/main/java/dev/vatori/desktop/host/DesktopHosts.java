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
            case "system-webview", "webview", "native" -> systemWebViewHost();
            case "compatibility", "headless", "console" -> new CompatibilityDesktopHost(false);
            case "browser" -> new CompatibilityDesktopHost(true);
            default -> throw new IllegalArgumentException("Unsupported Vaadin desktop host: " + requested);
        };
    }

    private static DesktopHost systemWebViewHost() {
        HostFallback fallback = systemWebViewFallback();
        return fallback == null
                ? new SystemWebViewDesktopHost()
                : new SystemWebViewDesktopHost(fallback.host(), fallback.name());
    }

    private static HostFallback systemWebViewFallback() {
        String requested = firstNonBlank(
                System.getProperty("vaadin.desktop.hostFallback"),
                System.getenv("VAADIN_DESKTOP_HOST_FALLBACK"),
                "");
        if (requested.isBlank()) {
            return null;
        }
        return switch (requested.toLowerCase(Locale.ROOT)) {
            case "none", "off", "false", "disabled", "fail-fast" -> null;
            case "compatibility", "headless", "console" ->
                    new HostFallback("compatibility", new CompatibilityDesktopHost(false));
            case "browser" -> new HostFallback("browser", new CompatibilityDesktopHost(true));
            default -> throw new IllegalArgumentException("Unsupported Vaadin desktop host fallback: " + requested);
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

    private record HostFallback(String name, DesktopHost host) {
    }
}
