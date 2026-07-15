package dev.vatori.desktop.flow;

import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.DesktopContextHolder;
import dev.vatori.desktop.host.DesktopHost;
import dev.vatori.desktop.host.DesktopHosts;
import dev.vatori.desktop.host.DesktopNativeBridges;
import dev.vatori.desktop.flow.ipc.FlowIpcLauncher;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static ConfigurableApplicationContext launch(Class<?> springBootApplicationClass,
                                                        DesktopApplication application,
                                                        String[] args) {
        if (flowIpcTransportRequested()) {
            ConfigurableApplicationContext context =
                    FlowIpcSpringApplication.start(springBootApplicationClass, args);
            try {
                FlowIpcRouteCatalog routes =
                        FlowIpcRouteResolver.resolve(springBootApplicationClass);
                FlowIpcLauncher.launchSpringApplication(
                        routes.rootRoute(), routes.routes(), application, args, context);
            } finally {
                context.close();
            }
            return null;
        }
        return launch(springBootApplicationClass, application, args, DesktopHosts.defaultHost());
    }

    public static ConfigurableApplicationContext launch(Class<?> springBootApplicationClass,
                                                        DesktopApplication application,
                                                        String[] args,
                                                        DesktopHost host) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(springBootApplicationClass)
                .properties(defaultProperties())
                .run(args);

        URI appUri = URI.create("http://127.0.0.1:" + resolvePort(context) + "/");
        DesktopContext desktopContext = new DesktopContext(
                application,
                appUri,
                DesktopNativeBridges.defaultBridge(),
                parseLaunchArguments(args));
        DesktopContextHolder.set(desktopContext);
        host.launch(desktopContext);
        return context;
    }

    private static Map<String, Object> defaultProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("server.address", "127.0.0.1");
        properties.put("server.port", "0");
        properties.put("vaadin.desktop.compatibility-mode", "true");
        properties.put("vaadin.launch-browser", "false");
        return properties;
    }

    private static boolean flowIpcTransportRequested() {
        String transport = System.getProperty("vaadin.desktop.transport");
        if (transport == null || transport.isBlank()) {
            transport = System.getenv("VAADIN_DESKTOP_TRANSPORT");
        }
        return "flow-ipc".equalsIgnoreCase(transport);
    }

    private static int resolvePort(ConfigurableApplicationContext context) {
        try {
            Method getWebServer = context.getClass().getMethod("getWebServer");
            Object webServer = getWebServer.invoke(context);
            Method getPort = webServer.getClass().getMethod("getPort");
            return ((Number) getPort.invoke(webServer)).intValue();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to resolve embedded web server port.", error);
        }
    }

    private static Map<String, String> parseLaunchArguments(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String arg : args == null ? new String[0] : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                int split = arg.indexOf('=');
                values.put(arg.substring(2, split), arg.substring(split + 1));
            }
        }
        return values;
    }
}
