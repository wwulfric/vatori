package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RouteRegistry;
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
import java.util.function.Supplier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UidlAdapterSmokeMain {
    private UidlAdapterSmokeMain() {
    }

    public static void main(String[] args) {
        SmokeVaadinService service = new SmokeVaadinService();
        WrappedSession wrappedSession = new DesktopWrappedSession("uidl-smoke-session");
        VaadinSession session = new VaadinSession(service);
        session.refreshTransients(wrappedSession, service);

        session.lock();
        try {
            UI ui = new UI();
            ui.getInternals().setSession(session);
            FlowClientMessage bootstrapMessage = new FlowClientMessage(
                    wrappedSession.getId(),
                    1,
                    "",
                    "",
                    "",
                    -1,
                    false);
            VaadinRequest request = DesktopVaadinRequest.uidl(session, bootstrapMessage);
            ui.doInit(request, 1, "flow-ipc-uidl-smoke");
            ui.add(new Div(new Span("UIDL smoke over desktop adapter")));
            session.addUI(ui);

            String uidl = new UidlFlowTransportAdapter().createUidl(ui, false);
            System.out.println(uidl);
            if (!uidl.contains("\"changes\"") || !uidl.contains("\"syncId\"")) {
                throw new IllegalStateException("UIDL smoke did not produce expected Flow UIDL fields");
            }
        } finally {
            session.unlock();
        }
    }

    private static final class SmokeVaadinService extends VaadinService {
        SmokeVaadinService() {
            super(smokeDeploymentConfiguration());
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
            return "flow-ipc-uidl-smoke";
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
            return new SmokeVaadinContext();
        }

        @Override
        protected Lock getSessionLock(WrappedSession wrappedSession) {
            Object lock = wrappedSession.getAttribute("flow-ipc-smoke-lock");
            if (lock instanceof Lock existing) {
                return existing;
            }
            ReentrantLock created = new ReentrantLock();
            wrappedSession.setAttribute("flow-ipc-smoke-lock", created);
            return created;
        }

        @Override
        public Iterable<com.vaadin.flow.server.DependencyFilter> getDependencyFilters() {
            return List.of();
        }
    }

    private static final class SmokeVaadinContext implements VaadinContext {
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

    private static DeploymentConfiguration smokeDeploymentConfiguration() {
        return (DeploymentConfiguration) Proxy.newProxyInstance(
                UidlAdapterSmokeMain.class.getClassLoader(),
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
            return UidlAdapterSmokeMain.class.getClassLoader();
        }
        return null;
    }
}
