package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import jakarta.servlet.http.Cookie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class DesktopVaadinResponse implements VaadinResponse {
    private final VaadinService service;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private int statusCode = 200;
    private String contentType;

    DesktopVaadinResponse(VaadinService service) {
        this.service = service;
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
        headers.put(name, value);
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public void setDateHeader(String name, long timestamp) {
        headers.put(name, String.valueOf(timestamp));
    }

    @Override
    public OutputStream getOutputStream() {
        return body;
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(body, true, StandardCharsets.UTF_8);
    }

    @Override
    public void setCacheTime(long milliseconds) {
        headers.put("Cache-Control", milliseconds < 0 ? "no-cache" : "max-age=" + milliseconds);
    }

    @Override
    public void sendError(int errorCode, String message) throws IOException {
        statusCode = errorCode;
        body.reset();
        body.write(message == null ? new byte[0] : message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public VaadinService getService() {
        return service;
    }

    @Override
    public void addCookie(Cookie cookie) {
        headers.put("Set-Cookie", cookie.getName() + "=" + cookie.getValue());
    }

    @Override
    public void setContentLength(int len) {
        headers.put("Content-Length", String.valueOf(len));
    }

    @Override
    public void setContentLengthLong(long len) {
        headers.put("Content-Length", String.valueOf(len));
    }

    String bodyAsString() {
        return body.toString(StandardCharsets.UTF_8);
    }

    byte[] bodyBytes() {
        return body.toByteArray();
    }

    String contentType() {
        return contentType;
    }

    int statusCode() {
        return statusCode;
    }

    Map<String, String> headers() {
        return Map.copyOf(headers);
    }
}
