package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Adapts an HTTP-shaped IPC exchange to Vaadin's normal request handler chain.
 * Request and response bodies remain opaque to the browser and native host.
 */
public final class DesktopFlowOpaqueHttpSession {
    private final DesktopFlowVaadinService service;
    private final VaadinSession session;

    private DesktopFlowOpaqueHttpSession(
            DesktopFlowVaadinService service,
            VaadinSession session) {
        this.service = service;
        this.session = session;
    }

    public static DesktopFlowOpaqueHttpSession create(
            String sessionId,
            String appId,
            String route,
            Class<? extends Component> routeClass,
            Consumer<DesktopFlowPushMessage> pushSender) {
        Objects.requireNonNull(routeClass, "routeClass");
        DesktopFlowVaadinService service =
                new DesktopFlowVaadinService(
                        appId, true, Objects.requireNonNull(pushSender, "pushSender"));
        service.registerRoute(route, routeClass);
        service.initialize();

        return open(service, sessionId);
    }

    static DesktopFlowOpaqueHttpSession open(
            DesktopFlowVaadinService service,
            String sessionId) {
        Objects.requireNonNull(service, "service");

        WrappedSession wrappedSession = new DesktopWrappedSession(sessionId);
        VaadinSession session = new VaadinSession(service);
        session.refreshTransients(wrappedSession, service);
        service.storeSession(wrappedSession, session);
        return new DesktopFlowOpaqueHttpSession(service, session);
    }

    public String sessionId() {
        return session.getSession().getId();
    }

    public void close() {
        session.lock();
        try {
            session.close();
        } finally {
            session.unlock();
        }
    }

    public static DesktopFlowOpaqueHttpSession create(
            String sessionId,
            String appId,
            String route,
            Class<? extends Component> routeClass) {
        return create(sessionId, appId, route, routeClass, ignored -> {
        });
    }

    public void validatePush(int uiId, String pushId) {
        runWithPushConnection(uiId, connection -> {
            if (!Objects.equals(session.getPushId(), pushId)) {
                throw new SecurityException("Invalid Flow push id");
            }
        });
    }

    public void connectPush(int uiId, String connectionId, int lastSeenServerSyncId) {
        runWithPushConnection(uiId,
                connection -> connection.connect(connectionId, lastSeenServerSyncId));
    }

    public void disconnectPush(int uiId, String connectionId, String pushId) {
        runWithValidatedPushConnection(uiId, connectionId, pushId,
                DesktopIpcPushConnection::disconnect);
    }

    public void resyncPush(int uiId, String connectionId, String pushId) {
        runWithValidatedPushConnection(uiId, connectionId, pushId,
                DesktopIpcPushConnection::sendResync);
    }

    private void runWithValidatedPushConnection(
            int uiId,
            String connectionId,
            String pushId,
            Consumer<DesktopIpcPushConnection> action) {
        runWithPushConnection(uiId, connection -> {
            if (!Objects.equals(session.getPushId(), pushId)) {
                throw new SecurityException("Invalid Flow push id");
            }
            if (!connection.matches(connectionId)) {
                throw new SecurityException("Invalid desktop push connection id");
            }
            action.accept(connection);
        });
    }

    private void runWithPushConnection(
            int uiId,
            Consumer<DesktopIpcPushConnection> action) {
        Map<Class<?>, CurrentInstance> previousInstances = null;
        session.lock();
        try {
            UI ui = session.getUIById(uiId);
            if (ui == null) {
                throw new IllegalArgumentException("Unknown Flow UI id " + uiId);
            }
            if (!(ui.getInternals().getPushConnection()
                    instanceof DesktopIpcPushConnection connection)) {
                throw new IllegalStateException("Desktop IPC push is not enabled for UI " + uiId);
            }
            previousInstances = CurrentInstance.setCurrent(ui);
            action.accept(connection);
        } finally {
            session.unlock();
            if (previousInstances != null) {
                CurrentInstance.restoreInstances(previousInstances);
            }
        }
    }

    public DesktopFlowHttpResponse handle(
            String method,
            String url,
            Map<String, String> headers,
            byte[] body) {
        URI uri = URI.create(Objects.requireNonNull(url, "url"));
        DesktopFlowHttpResponse staticResponse = staticResourceResponse(method, uri);
        if (staticResponse != null) {
            return staticResponse;
        }
        Map<String, String[]> parameters = queryParameters(uri.getRawQuery());
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        String requestUrl = uri.getScheme() + "://" + uri.getRawAuthority() + path;
        String contentType = header(headers, "Content-Type");
        DesktopVaadinRequest request = DesktopVaadinRequest.http(
                session,
                body,
                parameters,
                path,
                uri.getRawQuery(),
                requestUrl,
                method,
                contentType,
                headers);
        DesktopVaadinResponse response = new DesktopVaadinResponse(service);
        try {
            service.handleRequest(request, response);
        } catch (ServiceException error) {
            throw new IllegalStateException("Vaadin request handling failed", error);
        }

        Map<String, List<String>> responseHeaders = new LinkedHashMap<>(response.multiValueHeaders());
        if (response.contentType() != null && !response.contentType().isBlank()) {
            responseHeaders.putIfAbsent("Content-Type", List.of(response.contentType()));
        }
        byte[] responseBody = response.bodyBytes();
        responseHeaders.putIfAbsent("Content-Length", List.of(String.valueOf(responseBody.length)));
        return new DesktopFlowHttpResponse(
                response.statusCode(),
                statusText(response.statusCode()),
                responseHeaders,
                responseBody);
    }

    public void handleStreaming(
            String method,
            String url,
            Map<String, String> headers,
            InputStream body,
            long contentLength,
            Consumer<DesktopFlowHttpResponseHead> responseStart,
            OutputStream responseBody) throws IOException {
        Objects.requireNonNull(responseStart, "responseStart");
        Objects.requireNonNull(responseBody, "responseBody");
        URI uri = URI.create(Objects.requireNonNull(url, "url"));
        DesktopFlowHttpResponse staticResponse = staticResourceResponse(method, uri);
        if (staticResponse != null) {
            responseStart.accept(new DesktopFlowHttpResponseHead(
                    staticResponse.statusCode(),
                    staticResponse.statusText(),
                    staticResponse.headers()));
            responseBody.write(staticResponse.body());
            responseBody.flush();
            return;
        }

        Map<String, String[]> parameters = queryParameters(uri.getRawQuery());
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        String requestUrl = uri.getScheme() + "://" + uri.getRawAuthority() + path;
        String contentType = header(headers, "Content-Type");
        DesktopVaadinRequest request = DesktopVaadinRequest.http(
                session,
                Objects.requireNonNull(body, "body"),
                contentLength,
                parameters,
                path,
                uri.getRawQuery(),
                requestUrl,
                method,
                contentType,
                headers);
        DesktopVaadinResponse response = new DesktopVaadinResponse(
                service,
                responseBody,
                (statusCode, committedContentType, committedHeaders) -> {
                    Map<String, List<String>> responseHeaders =
                            new LinkedHashMap<>(committedHeaders);
                    if (committedContentType != null && !committedContentType.isBlank()) {
                        responseHeaders.putIfAbsent(
                                "Content-Type", List.of(committedContentType));
                    }
                    responseStart.accept(new DesktopFlowHttpResponseHead(
                            statusCode, statusText(statusCode), responseHeaders));
                });
        try {
            service.handleRequest(request, response);
            response.finish();
        } catch (ServiceException error) {
            throw new IllegalStateException("Vaadin request handling failed", error);
        }
    }

    private DesktopFlowHttpResponse staticResourceResponse(String method, URI uri) {
        String normalizedMethod = Objects.requireNonNullElse(method, "GET").toUpperCase(Locale.ROOT);
        if (!normalizedMethod.equals("GET") && !normalizedMethod.equals("HEAD")) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.equals("/") || path.indexOf('\\') >= 0) {
            return null;
        }
        List<String> segments = path.startsWith("/")
                ? List.of(path.substring(1).split("/"))
                : List.of(path.split("/"));
        if (segments.stream().anyMatch(segment -> segment.isBlank()
                || segment.equals(".") || segment.equals(".."))) {
            return null;
        }
        String relativePath = String.join("/", segments);
        for (String root : List.of("META-INF/VAADIN/webapp/", "META-INF/resources/")) {
            try (InputStream stream = classpathResource(root + relativePath)) {
                if (stream == null) {
                    continue;
                }
                byte[] bytes = stream.readAllBytes();
                String contentType = URLConnection.guessContentTypeFromName(relativePath);
                if (contentType == null || contentType.isBlank()) {
                    contentType = "application/octet-stream";
                }
                return new DesktopFlowHttpResponse(
                        200,
                        "OK",
                        Map.of(
                                "Content-Type", List.of(contentType),
                                "Content-Length", List.of(String.valueOf(bytes.length)),
                                "Cache-Control", List.of("no-cache")),
                        normalizedMethod.equals("HEAD") ? new byte[0] : bytes);
            } catch (IOException error) {
                throw new IllegalStateException("Unable to read static resource " + relativePath, error);
            }
        }
        return null;
    }

    private static InputStream classpathResource(String name) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextLoader == null ? null : contextLoader.getResourceAsStream(name);
        if (stream != null) {
            return stream;
        }
        ClassLoader frameworkLoader = DesktopFlowOpaqueHttpSession.class.getClassLoader();
        return frameworkLoader == contextLoader ? null : frameworkLoader.getResourceAsStream(name);
    }

    private static Map<String, String[]> queryParameters(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, List<String>> values = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            String rawName = separator < 0 ? pair : pair.substring(0, separator);
            String rawValue = separator < 0 ? "" : pair.substring(separator + 1);
            String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
            String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            values.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        Map<String, String[]> parameters = new LinkedHashMap<>();
        values.forEach((name, entries) -> parameters.put(name, entries.toArray(String[]::new)));
        return parameters;
    }

    private static String header(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return "application/octet-stream";
    }

    private static String statusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "HTTP " + statusCode;
        };
    }
}
