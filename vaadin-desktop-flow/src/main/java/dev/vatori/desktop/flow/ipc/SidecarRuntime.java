package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopContext;
import dev.vatori.desktop.DesktopContextHolder;
import dev.vatori.desktop.DesktopWindow;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Internal runtime facade used by {@code DesktopLauncher}. */
public final class SidecarRuntime {
    private static final String FLOW_IPC_SESSION_ID = "flow-ipc-session";
    private static final String COMPATIBILITY_SESSION_ID = "compatibility-session";
    private static final String MAIN_UI_ID = "main";

    private SidecarRuntime() {
    }

    public static void runFlow(
            Class<? extends Component> rootRouteClass,
            Collection<Class<? extends Component>> routeClasses,
            DesktopApplication application,
            String[] args,
            ApplicationContext applicationContext) {
        Objects.requireNonNull(rootRouteClass, "rootRouteClass");
        String route = route(rootRouteClass);
        FlowIpcScope scope = scope(application, FLOW_IPC_SESSION_ID);
        SidecarOutput output = new SidecarOutput(route, scope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
        DesktopContextHolder.set(new DesktopContext(
                application,
                URI.create("vatori-app://app/"),
                nativeBridge,
                parseLaunchArguments(args)));
        try {
            new StandardFlowIpcRuntime(
                    route,
                    routeClasses,
                    applicationContext,
                    application,
                    output,
                    nativeBridge,
                    scope).run();
        } finally {
            nativeBridge.close();
            DesktopContextHolder.clear();
        }
    }

    public static void runCompatibility(
            DesktopApplication application,
            URI applicationUri,
            String[] args) {
        Objects.requireNonNull(applicationUri, "applicationUri");
        FlowIpcScope scope = scope(application, COMPATIBILITY_SESSION_ID);
        SidecarOutput output = new SidecarOutput("", scope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(output, nativeBridge, scope);
        messageLoop.onWindowClosed(ignored -> messageLoop.requestShutdown());
        DesktopContextHolder.set(new DesktopContext(
                application,
                applicationUri,
                nativeBridge,
                parseLaunchArguments(args)));
        Map<String, Object> ready = readyPayload("compatibility-http", application);
        ready.put("applicationUrl", applicationUri.toString());
        output.send("ready", "sidecar-ready", "sidecar-ready", ready);
        try {
            messageLoop.run();
        } finally {
            nativeBridge.close();
            DesktopContextHolder.clear();
        }
    }

    static Map<String, Object> readyPayload(
            String transport,
            DesktopApplication application) {
        Objects.requireNonNull(application, "application");
        DesktopWindow window = application.mainWindow();
        Map<String, Object> windowPayload = new LinkedHashMap<>();
        windowPayload.put("id", window.id());
        windowPayload.put("title", window.title());
        windowPayload.put("width", window.width());
        windowPayload.put("height", window.height());

        Map<String, Object> applicationPayload = new LinkedHashMap<>();
        applicationPayload.put("name", application.name());
        applicationPayload.put("metadata", application.metadata());
        applicationPayload.put("mainWindow", windowPayload);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transport", transport);
        payload.put("application", applicationPayload);
        return payload;
    }

    private static FlowIpcScope scope(
            DesktopApplication application,
            String sessionId) {
        Objects.requireNonNull(application, "application");
        return new FlowIpcScope(
                application.mainWindow().id(), sessionId, MAIN_UI_ID);
    }

    private static String route(Class<? extends Component> routeClass) {
        for (RouteAlias alias : routeClass.getAnnotationsByType(RouteAlias.class)) {
            if (alias.value().isBlank()) {
                return "";
            }
        }
        Route route = routeClass.getAnnotation(Route.class);
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
