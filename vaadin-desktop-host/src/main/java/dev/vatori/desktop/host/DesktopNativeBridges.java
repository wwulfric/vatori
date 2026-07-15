package dev.vatori.desktop.host;

import dev.vatori.desktop.NativeBridge;

import java.util.Locale;

public final class DesktopNativeBridges {
    private DesktopNativeBridges() {
    }

    public static NativeBridge defaultBridge() {
        String requested = firstNonBlank(
                System.getProperty("vaadin.desktop.nativeBridge"),
                System.getenv("VAADIN_DESKTOP_NATIVE_BRIDGE"),
                "compatibility");
        return switch (requested.toLowerCase(Locale.ROOT)) {
            case "host-protocol", "stdio", "stdout-v0" -> new HostProtocolNativeBridge();
            case "compatibility" -> new CompatibilityNativeBridge();
            default -> throw new IllegalArgumentException("Unsupported Vaadin desktop native bridge: " + requested);
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
