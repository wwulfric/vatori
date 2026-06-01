package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.ApplicationConstants;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

final class DesktopVaadinRequest implements VaadinRequest, HttpServletRequest {
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
    private final List<Part> parts;

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
        this.parts = parseMultipartParts(this.bodyBytes, this.contentType);
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
    public ServletInputStream getInputStream() {
        return new ByteArrayServletInputStream(bodyBytes);
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
        return headerValue(headers, headerName);
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
        String value = headerValue(headers, name);
        return Collections.enumeration(value == null ? java.util.List.of() : java.util.List.of(value));
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        // UTF-8 is fixed for the desktop adapter slice.
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public String getScheme() {
        return "https";
    }

    @Override
    public String getServerName() {
        return "desktop";
    }

    @Override
    public int getServerPort() {
        return 443;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 443;
    }

    @Override
    public String getLocalName() {
        return "desktop";
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new IllegalStateException("Desktop Flow IPC request does not support async servlet mode");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        throw new IllegalStateException("Desktop Flow IPC request does not support async servlet mode");
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new IllegalStateException("Desktop Flow IPC request does not support async servlet mode");
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getRequestId() {
        return "flow-ipc-request";
    }

    @Override
    public String getProtocolRequestId() {
        return "flow-ipc-request";
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        if (value == null || value.isBlank()) {
            return -1;
        }
        return Integer.parseInt(value);
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public String getRequestedSessionId() {
        return null;
    }

    @Override
    public String getRequestURI() {
        return pathInfo.startsWith("/") ? pathInfo : "/" + pathInfo;
    }

    @Override
    public StringBuffer getRequestURL() {
        return new StringBuffer("https://desktop").append(getRequestURI());
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public HttpSession getSession() {
        return null;
    }

    @Override
    public String changeSessionId() {
        throw new IllegalStateException("Desktop Flow IPC request does not expose an HttpSession");
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new ServletException("Desktop Flow IPC request does not support servlet login");
    }

    @Override
    public void logout() {
        // No servlet authentication state is attached to the desktop adapter.
    }

    @Override
    public Collection<Part> getParts() {
        return parts;
    }

    @Override
    public Part getPart(String name) {
        return parts.stream()
                .filter(part -> part.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws ServletException {
        throw new ServletException("Desktop Flow IPC request does not support servlet upgrade");
    }

    Optional<Part> firstPart() {
        return parts.stream().findFirst();
    }

    int partCount() {
        return parts.size();
    }

    private static List<Part> parseMultipartParts(byte[] bodyBytes, String contentType) {
        String boundary = multipartBoundary(contentType);
        if (boundary.isBlank() || bodyBytes.length == 0) {
            return List.of();
        }
        String body = new String(bodyBytes, StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;
        String[] sections = body.split(Pattern.quote(delimiter));
        List<Part> parsed = new ArrayList<>();
        for (String rawSection : sections) {
            String section = trimLeadingLineBreak(rawSection);
            if (section.isBlank() || section.startsWith("--")) {
                continue;
            }
            int headerEnd = section.indexOf("\r\n\r\n");
            int separatorLength = 4;
            if (headerEnd < 0) {
                headerEnd = section.indexOf("\n\n");
                separatorLength = 2;
            }
            if (headerEnd < 0) {
                continue;
            }
            Map<String, List<String>> partHeaders = parsePartHeaders(section.substring(0, headerEnd));
            String partBody = trimTrailingLineBreak(section.substring(headerEnd + separatorLength));
            byte[] partBytes = partBody.getBytes(StandardCharsets.ISO_8859_1);
            String disposition = partHeaderValue(partHeaders, "Content-Disposition");
            String name = headerParameter(disposition, "name").orElse("file");
            String fileName = headerParameter(disposition, "filename").orElse("unknown");
            String partContentType = partHeaderValue(partHeaders, "Content-Type");
            parsed.add(new DesktopPart(name, fileName, partContentType, partBytes, partHeaders));
        }
        return List.copyOf(parsed);
    }

    private static String multipartBoundary(String contentType) {
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            return "";
        }
        for (String token : contentType.split(";")) {
            String trimmed = token.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
                return unquote(trimmed.substring("boundary=".length()).trim());
            }
        }
        return "";
    }

    private static Map<String, List<String>> parsePartHeaders(String headerBlock) {
        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (String line : headerBlock.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            parsed.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
        }
        return parsed;
    }

    private static Optional<String> headerParameter(String header, String parameter) {
        if (header == null || parameter == null) {
            return Optional.empty();
        }
        for (String token : header.split(";")) {
            String trimmed = token.trim();
            int equals = trimmed.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            if (trimmed.substring(0, equals).trim().equalsIgnoreCase(parameter)) {
                return Optional.of(unquote(trimmed.substring(equals + 1).trim()));
            }
        }
        return Optional.empty();
    }

    private static String trimLeadingLineBreak(String value) {
        String result = value;
        while (result.startsWith("\r\n")) {
            result = result.substring(2);
        }
        while (result.startsWith("\n")) {
            result = result.substring(1);
        }
        return result;
    }

    private static String trimTrailingLineBreak(String value) {
        String result = value;
        if (result.endsWith("\r\n")) {
            result = result.substring(0, result.length() - 2);
        } else if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String headerValue(Map<String, String> headers, String name) {
        if (name == null) {
            return null;
        }
        String exact = headers.get(name);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String partHeaderValue(Map<String, List<String>> headers, String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue().isEmpty() ? null : entry.getValue().get(0);
            }
        }
        return null;
    }

    private static final class ByteArrayServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream input;

        private ByteArrayServletInputStream(byte[] bytes) {
            this.input = new ByteArrayInputStream(bytes == null ? new byte[0] : bytes);
        }

        @Override
        public boolean isFinished() {
            return input.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new IllegalStateException("Desktop Flow IPC request does not support async reads");
        }

        @Override
        public int read() {
            return input.read();
        }
    }

    private static final class DesktopPart implements Part {
        private final String name;
        private final String fileName;
        private final String contentType;
        private final byte[] bytes;
        private final Map<String, List<String>> headers;

        private DesktopPart(String name, String fileName, String contentType, byte[] bytes,
                            Map<String, List<String>> headers) {
            this.name = name;
            this.fileName = fileName;
            this.contentType = contentType == null || contentType.isBlank()
                    ? "application/octet-stream"
                    : contentType;
            this.bytes = bytes == null ? new byte[0] : bytes.clone();
            this.headers = new LinkedHashMap<>(headers);
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getSubmittedFileName() {
            return fileName;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public void write(String fileName) throws IOException {
            throw new IOException("Desktop Flow IPC multipart parts are memory-only");
        }

        @Override
        public void delete() {
            // Memory-only part; nothing to delete.
        }

        @Override
        public String getHeader(String name) {
            return partHeaderValue(headers, name);
        }

        @Override
        public Collection<String> getHeaders(String name) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return List.copyOf(entry.getValue());
                }
            }
            return List.of();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return List.copyOf(headers.keySet());
        }
    }
}
