package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.experimental.Feature;
import com.vaadin.experimental.FeatureFlags;
import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/** Owns one Vaadin service and an isolated Vaadin session for each host scope. */
public final class DesktopFlowOpaqueHttpSessionRegistry implements AutoCloseable {
    private final DesktopFlowVaadinService service;
    private final ConcurrentMap<String, DesktopFlowOpaqueHttpSession> sessions =
            new ConcurrentHashMap<>();
    private boolean closed;

    private DesktopFlowOpaqueHttpSessionRegistry(
            String appId,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            Consumer<DesktopFlowPushMessage> pushSender,
            boolean productionMode,
            Path projectDirectory,
            DesktopFlowMultipartConfig multipartConfig) {
        this.service = new DesktopFlowVaadinService(
                appId, productionMode, Objects.requireNonNull(pushSender, "pushSender"),
                instantiatorFactory, projectDirectory, multipartConfig);
        service.registerRoutes(routeClasses);
        service.initialize();
    }

    private DesktopFlowOpaqueHttpSessionRegistry(
            String appId,
            String route,
            Class<? extends Component> routeClass,
            Consumer<DesktopFlowPushMessage> pushSender) {
        this.service = new DesktopFlowVaadinService(
                appId, true, Objects.requireNonNull(pushSender, "pushSender"));
        service.registerRoute(route, Objects.requireNonNull(routeClass, "routeClass"));
        service.initialize();
    }

    public static DesktopFlowOpaqueHttpSessionRegistry create(
            String appId,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            Consumer<DesktopFlowPushMessage> pushSender) {
        return new DesktopFlowOpaqueHttpSessionRegistry(
                appId, routeClasses, instantiatorFactory, pushSender,
                true, null, DesktopFlowMultipartConfig.defaults());
    }

    public static DesktopFlowOpaqueHttpSessionRegistry create(
            String appId,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            Consumer<DesktopFlowPushMessage> pushSender,
            boolean productionMode) {
        return new DesktopFlowOpaqueHttpSessionRegistry(
                appId, routeClasses, instantiatorFactory, pushSender,
                productionMode, null, DesktopFlowMultipartConfig.defaults());
    }

    public static DesktopFlowOpaqueHttpSessionRegistry create(
            String appId,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            Consumer<DesktopFlowPushMessage> pushSender,
            boolean productionMode,
            Path projectDirectory) {
        return new DesktopFlowOpaqueHttpSessionRegistry(
                appId, routeClasses, instantiatorFactory, pushSender,
                productionMode, projectDirectory,
                DesktopFlowMultipartConfig.defaults());
    }

    public static DesktopFlowOpaqueHttpSessionRegistry create(
            String appId,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            Consumer<DesktopFlowPushMessage> pushSender,
            boolean productionMode,
            Path projectDirectory,
            DesktopFlowMultipartConfig multipartConfig) {
        return new DesktopFlowOpaqueHttpSessionRegistry(
                appId, routeClasses, instantiatorFactory, pushSender,
                productionMode, projectDirectory,
                Objects.requireNonNull(multipartConfig, "multipartConfig"));
    }

    public static DesktopFlowOpaqueHttpSessionRegistry create(
            String appId,
            String route,
            Class<? extends Component> routeClass,
            Consumer<DesktopFlowPushMessage> pushSender) {
        return new DesktopFlowOpaqueHttpSessionRegistry(
                appId, route, routeClass, pushSender);
    }

    public synchronized DesktopFlowOpaqueHttpSession session(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Desktop Flow session id is required");
        }
        if (closed) {
            throw new IllegalStateException("Desktop Flow session registry is closed");
        }
        return sessions.computeIfAbsent(
                sessionId,
                id -> DesktopFlowOpaqueHttpSession.open(service, id));
    }

    public synchronized boolean closeSession(String sessionId) {
        DesktopFlowOpaqueHttpSession removed = sessions.remove(sessionId);
        if (removed == null) {
            return false;
        }
        removed.close();
        return true;
    }

    public synchronized int size() {
        return sessions.size();
    }

    public List<String> enabledFeatureFlagIds() {
        return FeatureFlags.get(service.getContext()).getFeatures()
                .stream()
                .filter(Feature::isEnabled)
                .map(Feature::getId)
                .toList();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException failure = null;
        for (DesktopFlowOpaqueHttpSession session : sessions.values()) {
            try {
                session.close();
            } catch (RuntimeException error) {
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
        }
        sessions.clear();
        try {
            service.destroy();
        } catch (RuntimeException error) {
            if (failure == null) {
                failure = error;
            } else {
                failure.addSuppressed(error);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
