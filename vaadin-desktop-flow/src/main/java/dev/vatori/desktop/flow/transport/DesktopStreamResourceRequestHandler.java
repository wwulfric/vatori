package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Adapts StreamResource downloads that upstream binds to a servlet request. */
final class DesktopStreamResourceRequestHandler implements RequestHandler {
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

        Optional<AbstractStreamResource> resource = registeredResource(session, request.getPathInfo());
        if (resource.isEmpty() || !(resource.get() instanceof StreamResource streamResource)) {
            return false;
        }

        StreamResourceWriter writer;
        session.lock();
        try {
            String contentType;
            try {
                contentType = streamResource.getContentTypeResolver().apply(streamResource, null);
            } catch (RuntimeException ignored) {
                contentType = URLConnection.guessContentTypeFromName(streamResource.getName());
            }
            response.setContentType(contentType == null || contentType.isBlank()
                    ? "application/octet-stream"
                    : contentType);
            response.setCacheTime(streamResource.getCacheTime());
            streamResource.getHeaders().forEach(response::setHeader);
            writer = streamResource.getWriter();
            if (writer == null) {
                response.setStatus(500);
                throw new IOException("Stream resource produces null writer");
            }
        } finally {
            session.unlock();
        }

        if (method.equals("HEAD")) {
            return true;
        }
        try (OutputStream output = response.getOutputStream()) {
            try {
                writer.accept(output, session);
            } catch (IOException | RuntimeException error) {
                response.setStatus(500);
                throw error;
            }
        }
        return true;
    }

    private static Optional<AbstractStreamResource> registeredResource(
            VaadinSession session,
            String path) {
        URI lookupUri = lookupUri(path);
        if (lookupUri == null) {
            return Optional.empty();
        }
        session.lock();
        try {
            return session.getResourceRegistry().getResource(lookupUri);
        } finally {
            session.unlock();
        }
    }

    private static URI lookupUri(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String pathInfo = path.startsWith("/") ? path.substring(1) : path;
        if (!pathInfo.startsWith(StreamRequestHandler.DYN_RES_PREFIX)) {
            return null;
        }
        int nameStart = pathInfo.lastIndexOf('/');
        if (nameStart < 0) {
            return null;
        }
        String prefix = pathInfo.substring(0, nameStart + 1);
        String name = pathInfo.substring(nameStart + 1);
        try {
            return new URI(prefix + UrlUtil.encodeURIComponent(name));
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}
