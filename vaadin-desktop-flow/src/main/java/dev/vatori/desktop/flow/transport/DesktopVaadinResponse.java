package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import jakarta.servlet.http.Cookie;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DesktopVaadinResponse implements VaadinResponse {
    private final VaadinService service;
    private final ByteArrayOutputStream bufferedBody;
    private final OutputStream body;
    private final CommitHandler commitHandler;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private int statusCode = 200;
    private String contentType;
    private boolean committed;
    private PrintWriter writer;

    DesktopVaadinResponse(VaadinService service) {
        this(service, new ByteArrayOutputStream(), (statusCode, contentType, headers) -> {
        });
    }

    DesktopVaadinResponse(VaadinService service, OutputStream body, CommitHandler commitHandler) {
        this.service = service;
        this.bufferedBody = body instanceof ByteArrayOutputStream bytes ? bytes : null;
        this.commitHandler = commitHandler;
        this.body = new FilterOutputStream(body) {
            @Override
            public void write(int value) throws IOException {
                ensureCommitted();
                out.write(value);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                if (length == 0) {
                    return;
                }
                ensureCommitted();
                out.write(bytes, offset, length);
            }

            @Override
            public void flush() throws IOException {
                ensureCommitted();
                out.flush();
            }
        };
    }

    @Override
    public void setStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setHeader(String name, String value) {
        replaceHeader(name, value);
    }

    @Override
    public boolean containsHeader(String name) {
        return headerKey(name) != null;
    }

    @Override
    public void setDateHeader(String name, long timestamp) {
        replaceHeader(name, String.valueOf(timestamp));
    }

    @Override
    public OutputStream getOutputStream() {
        return body;
    }

    @Override
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(body, true, StandardCharsets.UTF_8);
        }
        return writer;
    }

    @Override
    public void setCacheTime(long milliseconds) {
        replaceHeader("Cache-Control", milliseconds < 0 ? "no-cache" : "max-age=" + milliseconds);
    }

    @Override
    public void sendError(int errorCode, String message) throws IOException {
        if (committed && bufferedBody == null) {
            throw new IOException("Desktop Flow IPC response is already committed");
        }
        statusCode = errorCode;
        if (bufferedBody != null) {
            bufferedBody.reset();
        }
        body.write(message == null ? new byte[0] : message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public VaadinService getService() {
        return service;
    }

    @Override
    public void addCookie(Cookie cookie) {
        StringBuilder value = new StringBuilder(cookie.getName())
                .append('=')
                .append(cookie.getValue() == null ? "" : cookie.getValue());
        cookie.getAttributes().forEach((name, attributeValue) -> {
            value.append("; ").append(name);
            if (attributeValue != null && !attributeValue.isEmpty()) {
                value.append('=').append(attributeValue);
            }
        });
        addHeaderValue("Set-Cookie", value.toString());
    }

    @Override
    public void setContentLength(int len) {
        replaceHeader("Content-Length", String.valueOf(len));
    }

    @Override
    public void setContentLengthLong(long len) {
        replaceHeader("Content-Length", String.valueOf(len));
    }

    String bodyAsString() {
        return requireBufferedBody().toString(StandardCharsets.UTF_8);
    }

    byte[] bodyBytes() {
        return requireBufferedBody().toByteArray();
    }

    String contentType() {
        return contentType;
    }

    int statusCode() {
        return statusCode;
    }

    Map<String, String> headers() {
        Map<String, String> flattened = new LinkedHashMap<>();
        headers.forEach((name, values) -> flattened.put(name, String.join(", ", values)));
        return Map.copyOf(flattened);
    }

    Map<String, List<String>> multiValueHeaders() {
        Map<String, List<String>> copied = new LinkedHashMap<>();
        headers.forEach((name, values) -> copied.put(name, List.copyOf(values)));
        return Map.copyOf(copied);
    }

    void finish() throws IOException {
        if (writer != null) {
            writer.flush();
            if (writer.checkError()) {
                throw new IOException("Unable to flush desktop Flow response writer");
            }
        } else {
            body.flush();
        }
        ensureCommitted();
    }

    private void ensureCommitted() {
        if (committed) {
            return;
        }
        committed = true;
        commitHandler.commit(statusCode, contentType, multiValueHeaders());
    }

    private ByteArrayOutputStream requireBufferedBody() {
        if (bufferedBody == null) {
            throw new IllegalStateException("Streaming desktop response does not expose buffered bytes");
        }
        return bufferedBody;
    }

    private void replaceHeader(String name, String value) {
        String existing = headerKey(name);
        if (existing != null && !existing.equals(name)) {
            headers.remove(existing);
        }
        headers.put(name, new ArrayList<>(List.of(value)));
    }

    private void addHeaderValue(String name, String value) {
        String existing = headerKey(name);
        String key = existing == null ? name : existing;
        headers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
    }

    private String headerKey(String name) {
        return headers.keySet().stream()
                .filter(candidate -> candidate.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @FunctionalInterface
    interface CommitHandler {
        void commit(int statusCode, String contentType, Map<String, List<String>> headers);
    }
}
