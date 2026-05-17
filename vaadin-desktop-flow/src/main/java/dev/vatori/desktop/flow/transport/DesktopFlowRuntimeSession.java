package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.communication.PushMode;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class DesktopFlowRuntimeSession {
    private final VaadinSession session;
    private final UI ui;
    private final UidlFlowTransportAdapter uidlAdapter;

    private DesktopFlowRuntimeSession(VaadinSession session, UI ui, UidlFlowTransportAdapter uidlAdapter) {
        this.session = Objects.requireNonNull(session, "session");
        this.ui = Objects.requireNonNull(ui, "ui");
        this.uidlAdapter = Objects.requireNonNull(uidlAdapter, "uidlAdapter");
    }

    public static DesktopFlowRuntimeSession create(String sessionId, int uiId, String appId, Component root) {
        Objects.requireNonNull(root, "root");

        DesktopFlowVaadinService service = new DesktopFlowVaadinService(appId);
        WrappedSession wrappedSession = new DesktopWrappedSession(sessionId);
        VaadinSession session = new VaadinSession(service);
        session.refreshTransients(wrappedSession, service);

        session.lock();
        try {
            UI ui = new UI();
            ui.getInternals().setSession(session);
            FlowClientMessage bootstrapMessage = new FlowClientMessage(
                    wrappedSession.getId(),
                    uiId,
                    "",
                    "",
                    "",
                    -1,
                    false);
            VaadinRequest request = DesktopVaadinRequest.uidl(session, bootstrapMessage);
            ui.doInit(request, uiId, appId);
            ui.add(root);
            session.addUI(ui);
            return new DesktopFlowRuntimeSession(session, ui, new UidlFlowTransportAdapter());
        } finally {
            session.unlock();
        }
    }

    public UI ui() {
        return ui;
    }

    public void runLocked(Runnable action) {
        session.lock();
        try {
            action.run();
        } finally {
            session.unlock();
        }
    }

    public String createUidl(boolean resync) {
        session.lock();
        try {
            return uidlAdapter.createUidl(ui, resync);
        } finally {
            session.unlock();
        }
    }

    private static final class DesktopFlowVaadinService extends VaadinService {
        private final String appId;

        private DesktopFlowVaadinService(String appId) {
            super(deploymentConfiguration());
            this.appId = appId == null || appId.isBlank() ? "flow-ipc" : appId;
        }

        @Override
        public String getMimeType(String resourceName) {
            return null;
        }

        @Override
        protected RouteRegistry getRouteRegistry() {
            return null;
        }

        @Override
        protected PwaRegistry getPwaRegistry() {
            return null;
        }

        @Override
        public String getContextRootRelativePath(VaadinRequest request) {
            return "";
        }

        @Override
        protected boolean requestCanCreateSession(VaadinRequest request) {
            return true;
        }

        @Override
        public String getServiceName() {
            return appId;
        }

        @Override
        public String getMainDivId(VaadinSession session, VaadinRequest request) {
            return "outlet";
        }

        @Override
        public URL getStaticResource(String url) {
            return null;
        }

        @Override
        public URL getResource(String url) {
            return null;
        }

        @Override
        public InputStream getResourceAsStream(String url) {
            return null;
        }

        @Override
        public String resolveResource(String url) {
            return url;
        }

        @Override
        protected VaadinContext constructVaadinContext() {
            return new DesktopFlowVaadinContext();
        }

        @Override
        protected Lock getSessionLock(WrappedSession wrappedSession) {
            Object lock = wrappedSession.getAttribute("flow-ipc-session-lock");
            if (lock instanceof Lock existing) {
                return existing;
            }
            ReentrantLock created = new ReentrantLock();
            wrappedSession.setAttribute("flow-ipc-session-lock", created);
            return created;
        }

        @Override
        public Iterable<com.vaadin.flow.server.DependencyFilter> getDependencyFilters() {
            return List.of();
        }
    }

    private static final class DesktopFlowVaadinContext implements VaadinContext {
        private final Map<Class<?>, Object> attributes = new LinkedHashMap<>();

        @Override
        public synchronized <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
            Object value = attributes.get(type);
            if (value == null && defaultValueSupplier != null) {
                value = defaultValueSupplier.get();
                attributes.put(type, value);
            }
            return type.cast(value);
        }

        @Override
        public synchronized <T> void setAttribute(Class<T> clazz, T value) {
            if (value == null) {
                attributes.remove(clazz);
            } else {
                attributes.put(clazz, value);
            }
        }

        @Override
        public synchronized void removeAttribute(Class<?> clazz) {
            attributes.remove(clazz);
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.emptyEnumeration();
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    }

    private static DeploymentConfiguration deploymentConfiguration() {
        return (DeploymentConfiguration) Proxy.newProxyInstance(
                DesktopFlowRuntimeSession.class.getClassLoader(),
                new Class<?>[]{DeploymentConfiguration.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isProductionMode", "isRequestTiming", "isSyncIdCheckEnabled",
                            "isXsrfProtectionEnabled", "isCloseIdleSessions" -> false;
                    case "getHeartbeatInterval" -> 300;
                    case "getMaxMessageSuspendTimeout" -> 5000;
                    case "getWebComponentDisconnect" -> 300;
                    case "getPushMode" -> PushMode.DISABLED;
                    case "getSessionLockCheckStrategy" -> SessionLockCheckStrategy.ASSERT;
                    case "getProjectFolder" -> new File(".");
                    case "getFrontendFolder" -> new File("frontend");
                    case "getClassLoaderName" -> null;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == String.class) {
            return "";
        }
        if (type == ClassLoader.class) {
            return DesktopFlowRuntimeSession.class.getClassLoader();
        }
        return null;
    }
}
