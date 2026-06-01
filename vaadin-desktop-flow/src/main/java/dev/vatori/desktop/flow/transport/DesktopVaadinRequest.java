package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.ApplicationConstants;
import jakarta.servlet.http.Cookie;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class DesktopVaadinRequest implements VaadinRequest {
    private final VaadinService service;
    private final WrappedSession wrappedSession;
    private final byte[] bodyBytes;
    private final Map<String, String[]> parameters;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Locale locale;
    private final String pathInfo;
    private final String method;
    private final String contentType;

    private DesktopVaadinRequest(VaadinService service, WrappedSession wrappedSession,
                                 byte[] bodyBytes, Map<String, String[]> parameters, Locale locale,
                                 String pathInfo, String method, String contentType,
                                 Map<String, String> headers) {
        this.service = service;
        this.wrappedSession = wrappedSession;
        this.bodyBytes = bodyBytes == null ? new byte[0] : bodyBytes.clone();
        this.parameters = Map.copyOf(parameters);
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.pathInfo = pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
        this.method = method == null || method.isBlank() ? "POST" : method;
        this.contentType = contentType == null || contentType.isBlank() ? "application/json" : contentType;
        this.headers.putAll(headers == null ? Map.of() : headers);
    }

    static DesktopVaadinRequest uidl(VaadinSession session, FlowClientMessage message) {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put(ApplicationConstants.REQUEST_TYPE_PARAMETER,
                new String[]{ApplicationConstants.REQUEST_TYPE_UIDL});
        parameters.put(ApplicationConstants.UI_ID_PARAMETER,
                new String[]{String.valueOf(message.uiId())});
        return new DesktopVaadinRequest(
                session.getService(),
                session.getSession() == null ? new DesktopWrappedSession(message.sessionId()) : session.getSession(),
                message.requestBody().getBytes(StandardCharsets.UTF_8),
                parameters,
                Locale.getDefault(),
                "/",
                "POST",
                "application/json",
                Map.of());
    }

    static DesktopVaadinRequest upload(VaadinSession session, String sessionId, int uiId, String path,
                                       byte[] bodyBytes, String fileName, String contentType) {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put(ApplicationConstants.UI_ID_PARAMETER, new String[]{String.valueOf(uiId)});
        String effectiveContentType = contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", effectiveContentType);
        headers.put("X-Filename", URLEncoder.encode(
                fileName == null || fileName.isBlank() ? "unknown" : fileName,
                StandardCharsets.UTF_8));
        return new DesktopVaadinRequest(
                session.getService(),
                session.getSession() == null ? new DesktopWrappedSession(sessionId) : session.getSession(),
                bodyBytes,
                parameters,
                Locale.getDefault(),
                path,
                "POST",
                effectiveContentType,
                headers);
    }

    @Override
    public String getParameter(String parameter) {
        String[] values = parameters.get(parameter);
        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public int getContentLength() {
        return bodyBytes.length;
    }

    @Override
    public long getContentLengthLong() {
        return bodyBytes.length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bodyBytes);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public WrappedSession getWrappedSession() {
        return wrappedSession;
    }

    @Override
    public WrappedSession getWrappedSession(boolean allowSessionCreation) {
        return wrappedSession;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public String getRemoteAddr() {
        return "127.0.0.1";
    }

    @Override
    public boolean isSecure() {
        return true;
    }

    @Override
    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

    @Override
    public VaadinService getService() {
        return service;
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return Collections.enumeration(java.util.List.of(locale));
    }

    @Override
    public String getRemoteHost() {
        return "desktop";
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(new String(bodyBytes, StandardCharsets.UTF_8)));
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public long getDateHeader(String name) {
        return -1;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String value = headers.get(name);
        return Collections.enumeration(value == null ? java.util.List.of() : java.util.List.of(value));
    }
}
