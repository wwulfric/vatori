package dev.vatori.desktop.flow.ipc;

import java.util.Locale;

/** Host-selected sidecar transport shared by Flow and optional integration adapters. */
public enum SidecarTransport {
    FLOW_IPC,
    COMPATIBILITY_HTTP;

    private static final String TRANSPORT_PROPERTY = "vaadin.desktop.transport";
    private static final String TRANSPORT_ENVIRONMENT = "VAADIN_DESKTOP_TRANSPORT";

    public static SidecarTransport required() {
        String transport = System.getProperty(TRANSPORT_PROPERTY);
        if (transport == null || transport.isBlank()) {
            transport = System.getenv(TRANSPORT_ENVIRONMENT);
        }
        if (transport == null || transport.isBlank()) {
            throw new IllegalStateException(
                    "Desktop sidecar transport is missing. Start the application through "
                            + "bin/vaadin-desktop dev or dev-compat.");
        }
        return switch (transport.toLowerCase(Locale.ROOT)) {
            case "flow-ipc" -> FLOW_IPC;
            case "compatibility-http" -> COMPATIBILITY_HTTP;
            default -> throw new IllegalStateException(
                    "Unknown desktop sidecar transport '" + transport
                            + "'. Start the application through bin/vaadin-desktop.");
        };
    }
}
