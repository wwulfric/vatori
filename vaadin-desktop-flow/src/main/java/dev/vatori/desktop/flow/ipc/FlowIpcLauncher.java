package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.DesktopContextHolder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FlowIpcLauncher {
    private FlowIpcLauncher() {
    }

    public static void launch(Class<? extends Component> routeClass,
                              DesktopApplication application,
                              String[] args) {
        DesktopContext context = new DesktopContext(
                application,
                URI.create("flow-ipc://local/"),
                new FlowIpcNativeBridge(route(routeClass)),
                parseLaunchArguments(args));
        DesktopContextHolder.set(context);
        new FlowIpcRuntime(routeClass).run();
    }

    private static String route(Class<? extends Component> routeClass) {
        com.vaadin.flow.router.Route route = routeClass.getAnnotation(com.vaadin.flow.router.Route.class);
        return route == null ? "" : route.value();
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
