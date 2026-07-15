package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.DesktopContextHolder;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FlowIpcLauncher {
    private FlowIpcLauncher() {
    }

    public static void launch(Class<? extends Component> routeClass,
                              DesktopApplication application,
                              String[] args) {
        launch(routeClass, List.of(routeClass), application, args, null);
    }

    public static void launchSpringApplication(
            Class<? extends Component> rootRouteClass,
            Collection<Class<? extends Component>> routeClasses,
            DesktopApplication application,
            String[] args,
            ApplicationContext applicationContext) {
        launch(rootRouteClass, routeClasses, application, args,
                Objects.requireNonNull(applicationContext, "applicationContext"));
    }

    private static void launch(
            Class<? extends Component> rootRouteClass,
            Collection<Class<? extends Component>> routeClasses,
            DesktopApplication application,
            String[] args,
            ApplicationContext applicationContext) {
        FlowIpcScope defaultScope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        FlowIpcNativeBridge nativeBridge = new FlowIpcNativeBridge(
                route(rootRouteClass), defaultScope);
        DesktopContext context = new DesktopContext(
                application,
                URI.create("flow-ipc://local/"),
                nativeBridge,
                parseLaunchArguments(args));
        DesktopContextHolder.set(context);
        try {
            List<Class<? extends Component>> routes = List.copyOf(routeClasses);
            new StandardFlowIpcRuntime(
                    rootRouteClass, routes, applicationContext, nativeBridge).run();
        } finally {
            DesktopContextHolder.clear();
        }
    }

    private static String route(Class<? extends Component> routeClass) {
        for (com.vaadin.flow.router.RouteAlias alias
                : routeClass.getAnnotationsByType(com.vaadin.flow.router.RouteAlias.class)) {
            if (alias.value().isBlank()) {
                return "";
            }
        }
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
