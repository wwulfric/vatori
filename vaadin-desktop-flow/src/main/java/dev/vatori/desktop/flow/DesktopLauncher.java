package dev.vatori.desktop.flow;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.flow.ipc.SidecarRuntime;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Starts a Vaadin application as a native-host-owned Java sidecar. */
public final class DesktopLauncher {
    private static final String TRANSPORT_PROPERTY = "vaadin.desktop.transport";
    private static final String TRANSPORT_ENVIRONMENT = "VAADIN_DESKTOP_TRANSPORT";

    private DesktopLauncher() {
    }

    public static void launch(
            Class<?> springBootApplicationClass,
            DesktopApplication application,
            String[] args) {
        Objects.requireNonNull(springBootApplicationClass, "springBootApplicationClass");
        Objects.requireNonNull(application, "application");
        switch (requiredTransport()) {
            case FLOW_IPC -> launchFlow(springBootApplicationClass, application, args);
            case COMPATIBILITY_HTTP ->
                    launchCompatibility(springBootApplicationClass, application, args);
        }
    }

    private static void launchFlow(
            Class<?> springBootApplicationClass,
            DesktopApplication application,
            String[] args) {
        ConfigurableApplicationContext context =
                FlowIpcSpringApplication.start(springBootApplicationClass, args);
        try {
            FlowIpcRouteCatalog routes =
                    FlowIpcRouteResolver.resolve(springBootApplicationClass);
            SidecarRuntime.runFlow(
                    routes.rootRoute(), routes.routes(), application, args, context);
        } finally {
            context.close();
        }
    }

    private static void launchCompatibility(
            Class<?> springBootApplicationClass,
            DesktopApplication application,
            String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                springBootApplicationClass)
                .properties(compatibilityProperties())
                .run(args);
        URI applicationUri = URI.create(
                "http://127.0.0.1:" + resolvePort(context) + "/");
        runCompatibilitySidecar(context, application, args, applicationUri);
    }

    static void runCompatibilitySidecar(
            ConfigurableApplicationContext context,
            DesktopApplication application,
            String[] args,
            URI applicationUri) {
        try {
            SidecarRuntime.runCompatibility(application, applicationUri, args);
        } finally {
            context.close();
        }
    }

    private static Map<String, Object> compatibilityProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.address", "127.0.0.1");
        properties.put("server.port", "0");
        properties.put("vaadin.desktop.compatibility-mode", "true");
        properties.put("vaadin.launch-browser", "false");
        return properties;
    }

    private static SidecarTransport requiredTransport() {
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
            case "flow-ipc" -> SidecarTransport.FLOW_IPC;
            case "compatibility-http" -> SidecarTransport.COMPATIBILITY_HTTP;
            default -> throw new IllegalStateException(
                    "Unknown desktop sidecar transport '" + transport
                            + "'. Start the application through bin/vaadin-desktop.");
        };
    }

    private static int resolvePort(ConfigurableApplicationContext context) {
        try {
            Method getWebServer = context.getClass().getMethod("getWebServer");
            Object webServer = getWebServer.invoke(context);
            Method getPort = webServer.getClass().getMethod("getPort");
            return ((Number) getPort.invoke(webServer)).intValue();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(
                    "Unable to resolve embedded web server port.", error);
        }
    }

    private enum SidecarTransport {
        FLOW_IPC,
        COMPATIBILITY_HTTP
    }
}
