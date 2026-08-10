package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;
import com.vaadin.flow.server.communication.PushConnectionFactory;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;
import com.vaadin.flow.shared.communication.PushMode;
import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DesktopFlowVaadinService extends VaadinService {
    private final String appId;
    private final ApplicationRouteRegistry routeRegistry;
    private final InstantiatorFactory instantiatorFactory;
    private final PushConnectionFactory pushConnectionFactory;
    private final DesktopFlowMultipartConfig multipartConfig;

    DesktopFlowVaadinService(String appId) {
        this(appId, false);
    }

    DesktopFlowVaadinService(String appId, boolean productionMode) {
        this(appId, productionMode, null);
    }

    DesktopFlowVaadinService(
            String appId,
            boolean productionMode,
            Consumer<DesktopFlowPushMessage> pushSender) {
        this(appId, productionMode, pushSender, null);
    }

    DesktopFlowVaadinService(
            String appId,
            boolean productionMode,
            Consumer<DesktopFlowPushMessage> pushSender,
            InstantiatorFactory instantiatorFactory) {
        this(appId, productionMode, pushSender, instantiatorFactory, null);
    }

    DesktopFlowVaadinService(
            String appId,
            boolean productionMode,
            Consumer<DesktopFlowPushMessage> pushSender,
            InstantiatorFactory instantiatorFactory,
            Path projectDirectory) {
        this(appId, productionMode, pushSender, instantiatorFactory,
                projectDirectory, DesktopFlowMultipartConfig.defaults());
    }

    DesktopFlowVaadinService(
            String appId,
            boolean productionMode,
            Consumer<DesktopFlowPushMessage> pushSender,
            InstantiatorFactory instantiatorFactory,
            Path projectDirectory,
            DesktopFlowMultipartConfig multipartConfig) {
        super(deploymentConfiguration(
                productionMode, pushSender != null, projectDirectory));
        this.appId = appId == null || appId.isBlank() ? "flow-ipc" : appId;
        this.routeRegistry = ApplicationRouteRegistry.getInstance(getContext());
        this.instantiatorFactory = instantiatorFactory;
        this.multipartConfig = Objects.requireNonNull(
                multipartConfig, "multipartConfig");
        this.pushConnectionFactory = pushSender == null
                ? null
                : ui -> new DesktopIpcPushConnection(ui, pushSender);
    }

    DesktopFlowMultipartConfig multipartConfig() {
        return multipartConfig;
    }

    @Override
    public String getMimeType(String resourceName) {
        return null;
    }

    @Override
    protected RouteRegistry getRouteRegistry() {
        return routeRegistry;
    }

    void registerRoute(String route, Class<? extends Component> routeClass) {
        routeRegistry.setRoute(route == null ? "" : route, routeClass, List.of());
    }

    void registerRoutes(Collection<Class<? extends Component>> routeClasses) {
        Objects.requireNonNull(routeClasses, "routeClasses");
        RouteConfiguration configuration = RouteConfiguration.forRegistry(routeRegistry);
        configuration.update(() -> routeClasses.forEach(configuration::setAnnotatedRoute));
        List<Class<? extends Component>> missingRoutes = routeClasses.stream()
                .filter(routeClass -> !configuration.isRouteRegistered(routeClass))
                .toList();
        if (!missingRoutes.isEmpty()) {
            throw new IllegalStateException(
                    "Flow IPC route registration failed for " + missingRoutes);
        }
    }

    void initialize() {
        try {
            init();
        } catch (ServiceException error) {
            throw new IllegalStateException(
                    "Unable to initialize desktop Vaadin service", error);
        }
    }

    @Override
    protected Optional<Instantiator> loadInstantiators() throws ServiceException {
        if (instantiatorFactory == null) {
            return super.loadInstantiators();
        }
        Optional<Instantiator> spiInstantiator = super.loadInstantiators();
        Instantiator configuredInstantiator = instantiatorFactory.createInstantitor(this);
        if (spiInstantiator.isPresent() && configuredInstantiator != null) {
            throw new ServiceException(
                    "Multiple Flow IPC instantiators: Java SPI and configured factory");
        }
        return spiInstantiator.isPresent()
                ? spiInstantiator
                : Optional.ofNullable(configuredInstantiator);
    }

    @Override
    protected List<RequestHandler> createRequestHandlers() throws ServiceException {
        List<RequestHandler> handlers = super.createRequestHandlers();
        for (int index = 0; index < handlers.size(); index++) {
            if (handlers.get(index) instanceof JavaScriptBootstrapHandler) {
                handlers.set(index,
                        new DesktopJavaScriptBootstrapHandler(
                                pushConnectionFactory));
                break;
            }
        }
        handlers.add(new DesktopClasspathResourceRequestHandler());
        handlers.add(new DesktopStreamResourceRequestHandler());
        return handlers;
    }

    @Override
    public boolean ensurePushAvailable() {
        return pushConnectionFactory != null || super.ensurePushAvailable();
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

    void storeSession(WrappedSession wrappedSession, VaadinSession session) {
        writeToHttpSession(wrappedSession, session);
    }

    private static DeploymentConfiguration deploymentConfiguration(
            boolean productionMode,
            boolean pushEnabled,
            Path projectDirectory) {
        File projectFolder = projectDirectory == null
                ? new File(".").getAbsoluteFile()
                : projectDirectory.toAbsolutePath().normalize().toFile();
        return (DeploymentConfiguration) Proxy.newProxyInstance(
                DesktopFlowVaadinService.class.getClassLoader(),
                new Class<?>[]{DeploymentConfiguration.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isProductionMode" -> productionMode;
                    case "isSyncIdCheckEnabled", "isXsrfProtectionEnabled" ->
                            productionMode;
                    case "isRequestTiming", "isCloseIdleSessions" -> false;
                    case "getHeartbeatInterval" -> 300;
                    case "getMaxMessageSuspendTimeout" -> 5000;
                    case "getWebComponentDisconnect" -> 300;
                    case "getPushMode" ->
                            pushEnabled ? PushMode.AUTOMATIC : PushMode.DISABLED;
                    case "getSessionLockCheckStrategy" ->
                            SessionLockCheckStrategy.ASSERT;
                    case "getProjectFolder" ->
                            productionMode ? null : projectFolder;
                    case "getFrontendFolder" ->
                            new File(projectFolder, "src/main/frontend");
                    case "getJavaResourceFolder" ->
                            new File(projectFolder, "src/main/resources");
                    case "getJavaSourceFolder" ->
                            new File(projectFolder, "src/main/java");
                    case "getOutputResourceFolder" ->
                            new File(projectFolder, "target/classes");
                    case "getClassLoaderName" -> null;
                    case "getInitParameters" -> new Properties();
                    case "getStringProperty", "getApplicationOrSystemProperty",
                         "getBooleanProperty" -> args[1];
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
            return DesktopFlowVaadinService.class.getClassLoader();
        }
        return null;
    }

    private static final class DesktopJavaScriptBootstrapHandler
            extends JavaScriptBootstrapHandler {
        private final PushConnectionFactory pushConnectionFactory;

        private DesktopJavaScriptBootstrapHandler(
                PushConnectionFactory pushConnectionFactory) {
            this.pushConnectionFactory = pushConnectionFactory;
        }

        @Override
        protected void setupPushConnectionFactory(
                PushConfiguration pushConfiguration,
                BootstrapContext context) {
            if (pushConnectionFactory == null) {
                super.setupPushConnectionFactory(pushConfiguration, context);
            } else {
                pushConfiguration.setPushConnectionFactory(pushConnectionFactory);
            }
        }

        @Override
        protected String getRequestUrl(VaadinRequest request) {
            if (request instanceof HttpServletRequest httpRequest) {
                return httpRequest.getRequestURL().toString();
            }
            return "vatori-app://app/";
        }
    }

    private static final class DesktopFlowVaadinContext implements VaadinContext {
        private static final ResourceProvider RESOURCE_PROVIDER =
                new DesktopFlowResourceProvider();
        private static final ApplicationConfigurationFactory
                APPLICATION_CONFIGURATION_FACTORY =
                new DefaultApplicationConfigurationFactory();
        private static final Lookup DESKTOP_LOOKUP = new Lookup() {
            @Override
            public <T> T lookup(Class<T> serviceClass) {
                if (serviceClass.isInstance(RESOURCE_PROVIDER)) {
                    return serviceClass.cast(RESOURCE_PROVIDER);
                }
                if (serviceClass.isInstance(APPLICATION_CONFIGURATION_FACTORY)) {
                    return serviceClass.cast(APPLICATION_CONFIGURATION_FACTORY);
                }
                return null;
            }

            @Override
            public <T> Collection<T> lookupAll(Class<T> serviceClass) {
                if (serviceClass.isInstance(RESOURCE_PROVIDER)) {
                    return List.of(serviceClass.cast(RESOURCE_PROVIDER));
                }
                if (serviceClass.isInstance(APPLICATION_CONFIGURATION_FACTORY)) {
                    return List.of(serviceClass.cast(
                            APPLICATION_CONFIGURATION_FACTORY));
                }
                return Collections.emptyList();
            }
        };

        private final Map<Class<?>, Object> attributes = new LinkedHashMap<>();

        private DesktopFlowVaadinContext() {
            attributes.put(Lookup.class, DESKTOP_LOOKUP);
        }

        @Override
        public synchronized <T> T getAttribute(
                Class<T> type,
                Supplier<T> defaultValueSupplier) {
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

    private static final class DesktopFlowResourceProvider
            implements ResourceProvider {

        @Override
        public URL getApplicationResource(String path) {
            return classLoader().getResource(normalizeResourcePath(path));
        }

        @Override
        public List<URL> getApplicationResources(String path)
                throws IOException {
            return Collections.list(
                    classLoader().getResources(normalizeResourcePath(path)));
        }

        @Override
        public URL getClientResource(String path) {
            return getApplicationResource(path);
        }

        @Override
        public InputStream getClientResourceAsStream(String path)
                throws IOException {
            URL resource = getClientResource(path);
            return resource == null ? null : resource.openStream();
        }

        private static ClassLoader classLoader() {
            ClassLoader contextClassLoader =
                    Thread.currentThread().getContextClassLoader();
            return contextClassLoader == null
                    ? DesktopFlowVaadinService.class.getClassLoader()
                    : contextClassLoader;
        }

        private static String normalizeResourcePath(String path) {
            if (path == null || path.isBlank()) {
                return "";
            }
            return path.startsWith("/") ? path.substring(1) : path;
        }
    }
}
