package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;
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
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.fileupload2.core.FileUploadContentTypeException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletDiskFileUpload;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class DesktopVaadinRequest
        implements VaadinRequest, HttpServletRequest, AutoCloseable {
    private final VaadinService service;
    private final WrappedSession wrappedSession;
    private final RequestBody body;
    private final Map<String, String[]> parameters;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Locale locale;
    private final String pathInfo;
    private final String queryString;
    private final String requestUrl;
    private final String method;
    private final String contentType;
    private final DesktopFlowMultipartConfig multipartConfig;
    private List<Part> parts;
    private MultipartStorage multipartStorage;
    private IOException multipartIoFailure;
    private ServletException multipartServletFailure;
    private RuntimeException multipartRuntimeFailure;
    private boolean closed;

    private DesktopVaadinRequest(VaadinService service, WrappedSession wrappedSession,
                                 byte[] bodyBytes, Map<String, String[]> parameters, Locale locale,
                                 String pathInfo, String queryString, String requestUrl,
                                 String method, String contentType,
                                 Map<String, String> headers,
                                 DesktopFlowMultipartConfig multipartConfig) {
        this(service, wrappedSession, RequestBody.buffered(bodyBytes), parameters, locale,
                pathInfo, queryString, requestUrl, method, contentType, headers,
                multipartConfig);
    }

    private DesktopVaadinRequest(VaadinService service, WrappedSession wrappedSession,
                                 RequestBody body, Map<String, String[]> parameters, Locale locale,
                                 String pathInfo, String queryString, String requestUrl,
                                 String method, String contentType,
                                 Map<String, String> headers,
                                 DesktopFlowMultipartConfig multipartConfig) {
        this.service = service;
        this.wrappedSession = wrappedSession;
        this.body = Objects.requireNonNull(body, "body");
        this.parameters = copyParameters(parameters);
        this.locale = locale == null ? Locale.getDefault() : locale;
        this.pathInfo = pathInfo == null || pathInfo.isBlank() ? "/" : pathInfo;
        this.queryString = queryString;
        this.requestUrl = requestUrl == null || requestUrl.isBlank()
                ? "https://desktop" + this.pathInfo
                : requestUrl;
        this.method = method == null || method.isBlank() ? "POST" : method;
        this.contentType = contentType == null || contentType.isBlank() ? "application/json" : contentType;
        this.multipartConfig = Objects.requireNonNull(
                multipartConfig, "multipartConfig");
        this.headers.putAll(headers == null ? Map.of() : headers);
    }

    static DesktopVaadinRequest http(VaadinSession session, byte[] bodyBytes,
                                     Map<String, String[]> parameters, String path,
                                     String queryString, String requestUrl, String method,
                                     String contentType, Map<String, String> headers) {
        return new DesktopVaadinRequest(
                session.getService(),
                session.getSession(),
                bodyBytes,
                parameters,
                Locale.getDefault(),
                path,
                queryString,
                requestUrl,
                method,
                contentType,
                headers,
                multipartConfig(session));
    }

    static DesktopVaadinRequest http(VaadinSession session, InputStream body, long contentLength,
                                     Map<String, String[]> parameters, String path,
                                     String queryString, String requestUrl, String method,
                                     String contentType, Map<String, String> headers) {
        return new DesktopVaadinRequest(
                session.getService(),
                session.getSession(),
                RequestBody.streaming(body, contentLength),
                parameters,
                Locale.getDefault(),
                path,
                queryString,
                requestUrl,
                method,
                contentType,
                headers,
                multipartConfig(session));
    }

    @Override
    public String getParameter(String parameter) {
        ensureMultipartParameters();
        String[] values = parameters.get(parameter);
        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        ensureMultipartParameters();
        return Collections.unmodifiableMap(copyParameters(parameters));
    }

    @Override
    public int getContentLength() {
        long length = body.contentLength;
        return length < 0 || length > Integer.MAX_VALUE ? -1 : (int) length;
    }

    @Override
    public long getContentLengthLong() {
        return body.contentLength;
    }

    @Override
    public ServletInputStream getInputStream() {
        return body.stream;
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
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
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
        ensureMultipartParameters();
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        ensureMultipartParameters();
        String[] values = parameters.get(name);
        return values == null ? null : values.clone();
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
        return queryString;
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
        return new StringBuffer(requestUrl);
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
    public synchronized Collection<Part> getParts()
            throws IOException, ServletException {
        requireOpen();
        if (parts != null) {
            return parts;
        }
        if (multipartIoFailure != null) {
            throw multipartIoFailure;
        }
        if (multipartServletFailure != null) {
            throw multipartServletFailure;
        }
        if (multipartRuntimeFailure != null) {
            throw multipartRuntimeFailure;
        }
        try {
            parts = parseMultipartParts();
            return parts;
        } catch (IOException error) {
            multipartIoFailure = error;
            throw error;
        } catch (ServletException error) {
            multipartServletFailure = error;
            throw error;
        } catch (RuntimeException error) {
            multipartRuntimeFailure = error;
            throw error;
        }
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return getParts().stream()
                .filter(part -> part.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws ServletException {
        throw new ServletException("Desktop Flow IPC request does not support servlet upgrade");
    }

    Optional<Part> firstPart() {
        try {
            return getParts().stream().findFirst();
        } catch (IOException | ServletException error) {
            throw new IllegalStateException("Unable to parse desktop multipart request", error);
        }
    }

    int partCount() {
        try {
            return getParts().size();
        } catch (IOException | ServletException error) {
            throw new IllegalStateException("Unable to parse desktop multipart request", error);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            body.stream.close();
        } catch (IOException error) {
            failure = error;
        }
        if (parts != null) {
            for (Part part : parts) {
                if (part instanceof DesktopPart desktopPart) {
                    try {
                        desktopPart.delete();
                    } catch (IOException error) {
                        failure = append(failure, error);
                    }
                }
            }
        }
        if (multipartStorage != null) {
            try {
                multipartStorage.close();
            } catch (IOException error) {
                failure = append(failure, error);
            }
            multipartStorage = null;
        }
        if (failure != null) {
            throw failure;
        }
    }

    private List<Part> parseMultipartParts()
            throws IOException, ServletException {
        if (!isMultipartFormData()) {
            throw new ServletException(
                    "Desktop Flow multipart parsing requires multipart/form-data");
        }
        Files.createDirectories(multipartConfig.location());
        MultipartStorage storage = new MultipartStorage(
                Files.createTempDirectory(
                        multipartConfig.location(), "vatori-flow-multipart-"));
        multipartStorage = storage;

        DiskFileItemFactory factory = DiskFileItemFactory.builder()
                .setPath(storage.requestDirectory())
                .setThreshold(multipartConfig.memoryThresholdBytes())
                .setCharset(StandardCharsets.UTF_8)
                .get();
        JakartaServletDiskFileUpload upload =
                new JakartaServletDiskFileUpload(factory);
        upload.setHeaderCharset(StandardCharsets.UTF_8);
        upload.setMaxFileSize(multipartConfig.maxFileSizeBytes());
        upload.setMaxSize(multipartConfig.maxRequestSizeBytes());
        upload.setMaxFileCount(multipartConfig.maxPartCount());
        upload.setMaxPartHeaderSize(
                multipartConfig.maxPartHeaderSizeBytes());

        try {
            List<DiskFileItem> items = upload.parseRequest(this);
            mergeFormParameters(items);
            return items.stream()
                    .map(item -> (Part) new DesktopPart(
                            item, multipartConfig.location()))
                    .toList();
        } catch (FileUploadSizeException error) {
            discardMultipartStorage(error);
            throw new IllegalStateException(
                    "Desktop Flow multipart request exceeds configured limits",
                    error);
        } catch (FileUploadContentTypeException error) {
            discardMultipartStorage(error);
            throw new ServletException(
                    "Unable to process desktop Flow multipart content type",
                    error);
        } catch (IOException | RuntimeException error) {
            discardMultipartStorage(error);
            throw error;
        }
    }

    private void mergeFormParameters(List<DiskFileItem> items)
            throws IOException {
        for (DiskFileItem item : items) {
            if (!item.isFormField()) {
                continue;
            }
            String name = item.getFieldName();
            if (name == null) {
                continue;
            }
            String[] existing = parameters.get(name);
            int existingLength = existing == null ? 0 : existing.length;
            String[] merged = new String[existingLength + 1];
            if (existingLength > 0) {
                System.arraycopy(existing, 0, merged, 0, existingLength);
            }
            merged[existingLength] = item.getString();
            parameters.put(name, merged);
        }
    }

    private void ensureMultipartParameters() {
        if (!isMultipartFormData()) {
            return;
        }
        try {
            getParts();
        } catch (IOException | ServletException error) {
            throw new IllegalStateException(
                    "Unable to parse desktop multipart parameters", error);
        }
    }

    private boolean isMultipartFormData() {
        return contentType.toLowerCase(Locale.ROOT)
                .startsWith("multipart/form-data");
    }

    private void discardMultipartStorage(Throwable failure) {
        if (multipartStorage == null) {
            return;
        }
        try {
            multipartStorage.close();
        } catch (IOException cleanupError) {
            failure.addSuppressed(cleanupError);
        } finally {
            multipartStorage = null;
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Desktop Flow request is closed");
        }
    }

    private static DesktopFlowMultipartConfig multipartConfig(
            VaadinSession session) {
        if (session.getService() instanceof DesktopFlowVaadinService service) {
            return service.multipartConfig();
        }
        return DesktopFlowMultipartConfig.defaults();
    }

    private static Map<String, String[]> copyParameters(
            Map<String, String[]> source) {
        Map<String, String[]> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, values) -> copy.put(
                    name, values == null ? new String[0] : values.clone()));
        }
        return copy;
    }

    private static IOException append(
            IOException existing, IOException additional) {
        if (existing == null) {
            return additional;
        }
        existing.addSuppressed(additional);
        return existing;
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

    private static final class DesktopServletInputStream extends ServletInputStream {
        private final InputStream input;
        private boolean finished;

        private DesktopServletInputStream(InputStream input) {
            this.input = Objects.requireNonNull(input, "input");
        }

        @Override
        public boolean isFinished() {
            return finished;
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
        public int read() throws IOException {
            int value = input.read();
            finished = value < 0;
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int read = input.read(bytes, offset, length);
            finished = read < 0;
            return read;
        }

        @Override
        public void close() throws IOException {
            finished = true;
            input.close();
        }
    }

    private static final class RequestBody {
        private final long contentLength;
        private final ServletInputStream stream;

        private RequestBody(long contentLength, InputStream stream) {
            this.contentLength = contentLength;
            this.stream = new DesktopServletInputStream(stream);
        }

        private static RequestBody buffered(byte[] bytes) {
            byte[] copy = bytes == null ? new byte[0] : bytes.clone();
            return new RequestBody(copy.length, new ByteArrayInputStream(copy));
        }

        private static RequestBody streaming(InputStream stream, long contentLength) {
            return new RequestBody(contentLength, stream);
        }
    }

    private static final class DesktopPart implements Part {
        private final DiskFileItem item;
        private final Path storageLocation;
        private final long size;
        private boolean written;
        private boolean deleted;

        private DesktopPart(DiskFileItem item, Path storageLocation) {
            this.item = Objects.requireNonNull(item, "item");
            this.storageLocation = Objects.requireNonNull(
                    storageLocation, "storageLocation");
            this.size = item.getSize();
        }

        @Override
        public synchronized InputStream getInputStream() throws IOException {
            requireAvailable();
            return item.getInputStream();
        }

        @Override
        public String getContentType() {
            return item.getContentType();
        }

        @Override
        public String getName() {
            return item.getFieldName();
        }

        @Override
        public String getSubmittedFileName() {
            return item.getName();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public synchronized void write(String fileName) throws IOException {
            requireAvailable();
            if (written) {
                throw new IOException("Desktop multipart Part.write may only be called once");
            }
            Path target = Path.of(Objects.requireNonNull(fileName, "fileName"));
            if (!target.isAbsolute()) {
                target = storageLocation.resolve(target);
            }
            target = target.toAbsolutePath().normalize();
            try (InputStream input = item.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            written = true;
        }

        @Override
        public synchronized void delete() throws IOException {
            if (deleted) {
                return;
            }
            item.delete();
            deleted = true;
        }

        @Override
        public String getHeader(String name) {
            FileItemHeaders headers = item.getHeaders();
            return headers == null || name == null
                    ? null
                    : headers.getHeader(name);
        }

        @Override
        public Collection<String> getHeaders(String name) {
            FileItemHeaders headers = item.getHeaders();
            return headers == null || name == null
                    ? List.of()
                    : values(headers.getHeaders(name));
        }

        @Override
        public Collection<String> getHeaderNames() {
            FileItemHeaders headers = item.getHeaders();
            return headers == null
                    ? List.of()
                    : values(headers.getHeaderNames());
        }

        private void requireAvailable() throws IOException {
            if (deleted) {
                throw new IOException("Desktop multipart part is no longer available");
            }
        }

        private static List<String> values(
                java.util.Iterator<String> iterator) {
            if (iterator == null) {
                return List.of();
            }
            List<String> values = new java.util.ArrayList<>();
            iterator.forEachRemaining(values::add);
            return List.copyOf(values);
        }
    }

    private static final class MultipartStorage implements AutoCloseable {
        private final Path requestDirectory;
        private boolean closed;

        private MultipartStorage(Path requestDirectory) {
            this.requestDirectory = Objects.requireNonNull(
                    requestDirectory, "requestDirectory");
        }

        private Path requestDirectory() {
            return requestDirectory;
        }

        @Override
        public synchronized void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (!Files.exists(requestDirectory)) {
                return;
            }
            List<Path> paths;
            try (var tree = Files.walk(requestDirectory)) {
                paths = tree.sorted(Comparator.reverseOrder()).toList();
            }
            IOException failure = null;
            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    failure = append(failure, error);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
