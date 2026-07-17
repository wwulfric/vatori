package dev.vatori.desktop.spring;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.spring.SpringInstantiator;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.DesktopLauncher;
import dev.vatori.desktop.flow.ipc.SidecarRuntime;
import dev.vatori.desktop.flow.ipc.SidecarTransport;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Starts a desktop application with optional Spring Boot dependency injection or HTTP transport. */
public final class SpringDesktopLauncher {
    private SpringDesktopLauncher() {
    }

    public static void launch(
            Class<?> springBootApplicationClass,
            DesktopApplication application,
            String[] args) {
        Objects.requireNonNull(springBootApplicationClass, "springBootApplicationClass");
        Objects.requireNonNull(application, "application");
        switch (SidecarTransport.required()) {
            case FLOW_IPC -> launchFlow(springBootApplicationClass, application, args);
            case COMPATIBILITY_HTTP ->
                    launchCompatibility(springBootApplicationClass, application, args);
        }
    }

    private static void launchFlow(
            Class<?> springBootApplicationClass,
            DesktopApplication application,
            String[] args) {
        try (ConfigurableApplicationContext context =
                     SpringFlowApplication.start(springBootApplicationClass, args)) {
            List<Class<? extends com.vaadin.flow.component.Component>> routes =
                    SpringRouteResolver.resolve(springBootApplicationClass);
            DesktopFlowApplication flowApplication = DesktopFlowApplication
                    .builder(routes.getFirst())
                    .routes(routes)
                    .instantiatorFactory(service -> springInstantiator(context, service))
                    .build();
            DesktopLauncher.launch(application, flowApplication, args);
        }
    }

    private static Instantiator springInstantiator(
            ConfigurableApplicationContext context,
            com.vaadin.flow.server.VaadinService service) {
        List<Instantiator> configured = context.getBeansOfType(Instantiator.class)
                .values()
                .stream()
                .toList();
        if (configured.size() > 1) {
            throw new IllegalStateException("Multiple Spring Instantiator beans: " + configured);
        }
        return configured.isEmpty()
                ? new SpringInstantiator(service, context)
                : configured.getFirst();
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
}
