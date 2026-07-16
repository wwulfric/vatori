package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Serves production bundle resources when no servlet static server exists. */
final class DesktopClasspathResourceRequestHandler implements RequestHandler {
    private static final List<String> RESOURCE_ROOTS = List.of(
            "META-INF/VAADIN/webapp/",
            "META-INF/resources/");

    @Override
    public boolean handleRequest(
            VaadinSession session,
            VaadinRequest request,
            VaadinResponse response) throws IOException {
        String method = Objects.requireNonNullElse(request.getMethod(), "GET")
                .toUpperCase(Locale.ROOT);
        if (!method.equals("GET") && !method.equals("HEAD")) {
            return false;
        }

        String relativePath = safeRelativePath(request.getPathInfo());
        if (relativePath == null) {
            return false;
        }
        for (String root : RESOURCE_ROOTS) {
            try (InputStream resource = classpathResource(root + relativePath)) {
                if (resource == null) {
                    continue;
                }
                byte[] bytes = resource.readAllBytes();
                String contentType = URLConnection.guessContentTypeFromName(relativePath);
                response.setContentType(contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType);
                response.setContentLength(bytes.length);
                response.setCacheTime(-1);
                if (method.equals("GET")) {
                    response.getOutputStream().write(bytes);
                }
                return true;
            }
        }
        return false;
    }

    private static String safeRelativePath(String path) {
        if (path == null || path.isBlank() || path.equals("/") || path.indexOf('\\') >= 0) {
            return null;
        }
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        String[] segments = relativePath.split("/", -1);
        for (String segment : segments) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                return null;
            }
        }
        return String.join("/", segments);
    }

    private static InputStream classpathResource(String name) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextLoader == null ? null : contextLoader.getResourceAsStream(name);
        if (stream != null) {
            return stream;
        }
        ClassLoader frameworkLoader = DesktopClasspathResourceRequestHandler.class.getClassLoader();
        return frameworkLoader == contextLoader ? null : frameworkLoader.getResourceAsStream(name);
    }
}
