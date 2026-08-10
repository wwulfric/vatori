package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadResult;
import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.Part;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class OpaqueHttpAdapterSmokeMain {
    private OpaqueHttpAdapterSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        verifyRepeatedCookieHeaders();
        verifyOpaqueStaticResource();
        verifyStreamResourceThroughRequestChain();
        verifyDevelopmentProjectConfiguration();
        verifyMultipartParsingAndLifecycle();
        verifyMultipartFailureCleanup();
        verifyCancelledMultipartStopsBeforeUploadHandler();
        System.out.println("Opaque HTTP adapter smoke passed");
    }

    private static void verifyCancelledMultipartStopsBeforeUploadHandler()
            throws Exception {
        Path storage = Files.createTempDirectory(
                "vatori-flow-multipart-handler-smoke");
        DesktopFlowMultipartConfig config = DesktopFlowMultipartConfig.builder()
                .location(storage)
                .memoryThresholdBytes(64)
                .maxPartCount(8)
                .build();
        AtomicInteger handlerCalls = new AtomicInteger();
        AtomicReference<UploadResult> result = new AtomicReference<>();
        UploadHandler handler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) {
                handlerCalls.incrementAndGet();
            }

            @Override
            public void responseHandled(UploadResult uploadResult) {
                result.set(uploadResult);
                UploadHandler.super.responseHandled(uploadResult);
            }
        };

        String boundary = "VatoriHandlerCancellationBoundary";
        byte[] body = multipartBody(
                boundary, deterministicBytes(64 * 1024 + 13), true);
        try (DesktopFlowOpaqueHttpSessionRegistry registry =
                     multipartRegistry(config)) {
            DesktopFlowOpaqueHttpSession opaqueSession =
                    registry.session("multipart-handler-smoke");
            VaadinSession session = vaadinSession(opaqueSession);
            UI ui = new UI();
            ui.getInternals().setSession(session);
            Div owner = new Div();
            ui.getElement().appendChild(owner.getElement());
            StreamRegistration registration;
            Map<Class<?>, CurrentInstance> previousInstances =
                    CurrentInstance.setCurrent(ui);
            session.lock();
            try {
                registration = session.getResourceRegistry()
                        .registerResource(handler, owner.getElement());
            } finally {
                session.unlock();
                CurrentInstance.restoreInstances(previousInstances);
            }
            try {
                boolean parseFailed = false;
                try {
                    opaqueSession.handleStreaming(
                            "POST",
                            "https://app.vatori.invalid/"
                                    + registration.getResourceUri(),
                            Map.of("Content-Type",
                                    "multipart/form-data; boundary=" + boundary),
                            new FailingInputStream(body, body.length / 2),
                            -1,
                            ignored -> {
                            },
                            new ByteArrayOutputStream());
                } catch (IllegalStateException expected) {
                    parseFailed = true;
                }
                if (!parseFailed) {
                    throw new IllegalStateException(
                            "Cancelled multipart request completed successfully");
                }
            } finally {
                session.lock();
                try {
                    registration.unregister();
                } finally {
                    session.unlock();
                }
            }
        } finally {
            try {
                requireStorageEmpty(storage,
                        "Cancelled upload handler request left temporary parts behind");
            } finally {
                deleteTree(storage);
            }
        }

        if (handlerCalls.get() != 0 || result.get() != null) {
            throw new IllegalStateException(
                    "Cancelled multipart request reached the application UploadHandler");
        }
    }

    private static void verifyMultipartParsingAndLifecycle()
            throws Exception {
        Path storage = Files.createTempDirectory(
                "vatori-flow-multipart-smoke");
        DesktopFlowMultipartConfig config = DesktopFlowMultipartConfig.builder()
                .location(storage)
                .memoryThresholdBytes(1024)
                .maxPartCount(8)
                .build();
        String boundary = "VatoriBoundary7MA4YWxkTrZu0gW";
        byte[] fileBytes = deterministicBytes(128 * 1024 + 37);
        byte[] marker = ("prefix--" + boundary + "-inside")
                .getBytes(StandardCharsets.ISO_8859_1);
        System.arraycopy(marker, 0, fileBytes, 4093, marker.length);
        byte[] body = multipartBody(boundary, fileBytes, true);
        Path persisted = storage.resolve("persisted-upload.bin");
        Part retainedPart;

        try (DesktopFlowOpaqueHttpSessionRegistry registry =
                     multipartRegistry(config)) {
            DesktopFlowOpaqueHttpSession opaqueSession =
                    registry.session("multipart-lifecycle-smoke");
            DesktopVaadinRequest request = DesktopVaadinRequest.http(
                    vaadinSession(opaqueSession),
                    body,
                    Map.of("tag", new String[]{"query"}),
                    "/upload",
                    null,
                    "https://app.vatori.invalid/upload",
                    "POST",
                    "multipart/form-data; boundary=\"" + boundary + "\"",
                    Map.of("Content-Type",
                            "multipart/form-data; boundary=\"" + boundary + "\""));
            try (request) {
                Collection<Part> parts = request.getParts();
                if (parts.size() != 4) {
                    throw new IllegalStateException(
                            "Multipart parser did not preserve all form and file parts");
                }
                String[] tags = request.getParameterValues("tag");
                if (!Arrays.equals(tags,
                        new String[]{"query", "alpha", "beta"})) {
                    throw new IllegalStateException(
                            "Multipart form fields were not merged with query parameters");
                }
                Map<String, String[]> parameterMap = request.getParameterMap();
                parameterMap.get("tag")[0] = "mutated";
                if (!"query".equals(request.getParameter("tag"))) {
                    throw new IllegalStateException(
                            "Multipart parameter map exposed mutable request state");
                }

                retainedPart = request.getPart("file");
                if (retainedPart == null
                        || !"report;final.bin".equals(
                        retainedPart.getSubmittedFileName())
                        || retainedPart.getSize() != fileBytes.length
                        || !Arrays.equals(
                        retainedPart.getInputStream().readAllBytes(), fileBytes)
                        || !retainedPart.getHeaders("X-Vatori-Probe")
                        .equals(List.of("one", "two"))) {
                    throw new IllegalStateException(
                            "Multipart binary part metadata or bytes were not preserved");
                }
                if (largestRegularFile(storage) != fileBytes.length) {
                    throw new IllegalStateException(
                            "Multipart part did not spill above the configured threshold");
                }

                retainedPart.write("persisted-upload.bin");
                if (!Arrays.equals(Files.readAllBytes(persisted), fileBytes)) {
                    throw new IllegalStateException(
                            "Part.write did not persist the exact file bytes");
                }
                expectIOException(() ->
                        retainedPart.write("second-write.bin"));

                Part disposable = request.getPart("disposable");
                if (disposable == null) {
                    throw new IllegalStateException(
                            "Multipart disposable part is missing");
                }
                disposable.delete();
                expectIOException(disposable::getInputStream);
            }

            expectIOException(retainedPart::getInputStream);
            Files.deleteIfExists(persisted);
            requireStorageEmpty(storage,
                    "Successful multipart request left temporary parts behind");
        } finally {
            deleteTree(storage);
        }
    }

    private static void verifyMultipartFailureCleanup() throws Exception {
        Path storage = Files.createTempDirectory(
                "vatori-flow-multipart-failure-smoke");
        DesktopFlowMultipartConfig config = DesktopFlowMultipartConfig.builder()
                .location(storage)
                .memoryThresholdBytes(64)
                .maxPartCount(8)
                .build();
        String boundary = "VatoriFailureBoundary";
        byte[] body = multipartBody(
                boundary, deterministicBytes(64 * 1024 + 13), true);

        try (DesktopFlowOpaqueHttpSessionRegistry registry =
                     multipartRegistry(config)) {
            VaadinSession session = vaadinSession(
                    registry.session("multipart-failure-smoke"));
            DesktopVaadinRequest cancelled = DesktopVaadinRequest.http(
                    session,
                    new FailingInputStream(body, body.length / 2),
                    -1,
                    Map.of(),
                    "/upload",
                    null,
                    "https://app.vatori.invalid/upload",
                    "POST",
                    "multipart/form-data; boundary=" + boundary,
                    Map.of("Content-Type",
                            "multipart/form-data; boundary=" + boundary));
            expectMultipartFailure(cancelled);
            requireStorageEmpty(storage,
                    "Cancelled multipart request left temporary parts behind");

            byte[] closing = ("--" + boundary + "--\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1);
            byte[] malformedBody = Arrays.copyOf(
                    body, body.length - closing.length);
            DesktopVaadinRequest malformed = DesktopVaadinRequest.http(
                    session,
                    malformedBody,
                    Map.of(),
                    "/upload",
                    null,
                    "https://app.vatori.invalid/upload",
                    "POST",
                    "multipart/form-data; boundary=" + boundary,
                    Map.of("Content-Type",
                            "multipart/form-data; boundary=" + boundary));
            expectMultipartFailure(malformed);
            requireStorageEmpty(storage,
                    "Malformed multipart request left temporary parts behind");
        } finally {
            deleteTree(storage);
        }
    }

    private static DesktopFlowOpaqueHttpSessionRegistry multipartRegistry(
            DesktopFlowMultipartConfig config) {
        return DesktopFlowOpaqueHttpSessionRegistry.create(
                "multipart-adapter-smoke",
                List.of(DevelopmentView.class),
                null,
                ignored -> {
                },
                true,
                null,
                config);
    }

    private static byte[] multipartBody(
            String boundary, byte[] fileBytes, boolean complete) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        appendAscii(output, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n\r\n"
                + "alpha\r\n");
        appendAscii(output, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"tag\"\r\n\r\n"
                + "beta\r\n");
        appendAscii(output, "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; "
                + "filename=\"report;final.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n"
                + "X-Vatori-Probe: one\r\n"
                + "X-Vatori-Probe: two\r\n\r\n");
        output.writeBytes(fileBytes);
        appendAscii(output, "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"disposable\"; "
                + "filename=\"discard.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n"
                + "discard-me\r\n");
        if (complete) {
            appendAscii(output, "--" + boundary + "--\r\n");
        }
        return output.toByteArray();
    }

    private static void appendAscii(
            ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] deterministicBytes(int size) {
        byte[] bytes = new byte[size];
        for (int index = 0; index < size; index++) {
            bytes[index] = (byte) ((index * 37 + 19) & 0xff);
        }
        return bytes;
    }

    private static long largestRegularFile(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException error) {
                            throw new IllegalStateException(error);
                        }
                    })
                    .max()
                    .orElse(-1);
        }
    }

    private static void expectMultipartFailure(DesktopVaadinRequest request)
            throws IOException {
        boolean failed = false;
        try (request) {
            request.getParts();
        } catch (IOException | ServletException | IllegalStateException expected) {
            failed = true;
        }
        if (!failed) {
            throw new IllegalStateException(
                    "Invalid multipart request was accepted");
        }
    }

    private static void expectIOException(ThrowingAction action)
            throws Exception {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new IllegalStateException("Expected multipart IOException");
    }

    private static void requireStorageEmpty(Path root, String message)
            throws IOException {
        try (var paths = Files.list(root)) {
            if (paths.findAny().isPresent()) {
                throw new IllegalStateException(message);
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(
                    (left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static final class FailingInputStream extends InputStream {
        private final byte[] bytes;
        private final int failAfter;
        private int position;

        private FailingInputStream(byte[] bytes, int failAfter) {
            this.bytes = bytes.clone();
            this.failAfter = failAfter;
        }

        @Override
        public int read() throws IOException {
            if (position >= failAfter) {
                throw new IOException("Simulated multipart request cancellation");
            }
            return position >= bytes.length ? -1 : bytes[position++] & 0xff;
        }

        @Override
        public int read(byte[] target, int offset, int length)
                throws IOException {
            if (position >= failAfter) {
                throw new IOException("Simulated multipart request cancellation");
            }
            if (position >= bytes.length) {
                return -1;
            }
            int read = Math.min(length,
                    Math.min(bytes.length, failAfter) - position);
            System.arraycopy(bytes, position, target, offset, read);
            position += read;
            return read;
        }
    }

    private static void verifyDevelopmentProjectConfiguration()
            throws Exception {
        Path project = Files.createTempDirectory(
                "vatori-flow-development-project");
        Files.createDirectories(project.resolve("src/main/resources"));
        try (DesktopFlowOpaqueHttpSessionRegistry registry =
                     DesktopFlowOpaqueHttpSessionRegistry.create(
                             "development-project-smoke",
                             List.of(DevelopmentView.class),
                             null,
                             ignored -> {
                             },
                             false,
                             project)) {
            registry.enabledFeatureFlagIds();
        } finally {
            try (var paths = Files.walk(project)) {
                for (Path path : paths.sorted(
                        (left, right) -> right.compareTo(left)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void verifyRepeatedCookieHeaders() {
        DesktopVaadinResponse response = new DesktopVaadinResponse(null);
        Cookie sessionCookie = new Cookie("desktop_session", "abc123");
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(true);
        sessionCookie.setAttribute("SameSite", "Strict");
        response.addCookie(sessionCookie);
        response.addCookie(new Cookie("preference", "compact"));

        List<String> cookies = response.multiValueHeaders().get("Set-Cookie");
        if (cookies == null || cookies.size() != 2
                || !cookies.get(0).contains("desktop_session=abc123")
                || !cookies.get(0).contains("Path=/")
                || !cookies.get(0).contains("HttpOnly")
                || !cookies.get(0).contains("Secure")
                || !cookies.get(0).contains("SameSite=Strict")
                || !cookies.get(1).contains("preference=compact")) {
            throw new IllegalStateException(
                    "Desktop response did not preserve repeated Set-Cookie headers");
        }
    }

    private static void verifyOpaqueStaticResource() {
        DesktopFlowOpaqueHttpSession session = DesktopFlowOpaqueHttpSession.create(
                "opaque-static-smoke-session", "opaque-static-smoke", "", Div.class);
        DesktopFlowHttpResponse response = session.handle(
                "GET",
                "https://app.vatori.invalid/opaque-static-probe.txt?cache=1",
                Map.of(),
                new byte[0]);
        if (response.statusCode() != 200
                || !new String(response.body(), StandardCharsets.UTF_8)
                .equals("opaque static resource\n")) {
            throw new IllegalStateException(
                    "Opaque adapter did not serve a classpath static resource");
        }

        DesktopFlowHttpResponse head = session.handle(
                "HEAD",
                "https://app.vatori.invalid/opaque-static-probe.txt",
                Map.of(),
                new byte[0]);
        if (head.statusCode() != 200 || head.body().length != 0
                || !head.headers().get("Content-Length").equals(List.of("23"))) {
            throw new IllegalStateException(
                    "Opaque adapter did not preserve static HEAD metadata");
        }
        assertRequestHandlerOrder(session);
        session.close();
    }

    @SuppressWarnings("removal")
    private static void verifyStreamResourceThroughRequestChain() {
        DesktopFlowOpaqueHttpSession opaqueSession = DesktopFlowOpaqueHttpSession.create(
                "opaque-stream-smoke-session", "opaque-stream-smoke", "", Div.class);
        VaadinSession session = vaadinSession(opaqueSession);
        StreamRegistration registration;
        UI ui = new UI();
        ui.getInternals().setSession(session);
        Map<Class<?>, CurrentInstance> previousInstances = CurrentInstance.setCurrent(ui);
        session.lock();
        try {
            StreamResource resource = new StreamResource(
                    "opaque-stream-probe.txt",
                    (output, writerSession) -> {
                        if (writerSession != session || VaadinSession.getCurrent() != session) {
                            throw new IllegalStateException(
                                    "StreamResource writer lost its Vaadin session context");
                        }
                        output.write("opaque stream resource\n"
                                .getBytes(StandardCharsets.UTF_8));
                    });
            resource.setContentType("text/plain; charset=utf-8");
            resource.setHeader("X-Vatori-Stream", "request-chain");
            registration = session.getResourceRegistry().registerResource(resource);
        } finally {
            session.unlock();
            CurrentInstance.restoreInstances(previousInstances);
        }

        try {
            DesktopFlowHttpResponse response = opaqueSession.handle(
                    "GET",
                    "https://app.vatori.invalid/" + registration.getResourceUri(),
                    Map.of(),
                    new byte[0]);
            if (response.statusCode() != 200
                    || !new String(response.body(), StandardCharsets.UTF_8)
                    .equals("opaque stream resource\n")
                    || !response.headers().get("X-Vatori-Stream")
                    .equals(List.of("request-chain"))) {
                throw new IllegalStateException(
                        "Opaque adapter did not serve StreamResource through the request chain");
            }
        } finally {
            session.lock();
            try {
                registration.unregister();
            } finally {
                session.unlock();
            }
            opaqueSession.close();
        }
    }

    private static void assertRequestHandlerOrder(DesktopFlowOpaqueHttpSession opaqueSession) {
        List<Class<?>> handlerTypes = new ArrayList<>();
        vaadinSession(opaqueSession).getService().getRequestHandlers()
                .forEach(handler -> handlerTypes.add(handler.getClass()));
        int classpath = indexOf(handlerTypes, DesktopClasspathResourceRequestHandler.class);
        int desktopStream = indexOf(handlerTypes, DesktopStreamResourceRequestHandler.class);
        int upstreamStream = indexOf(handlerTypes, StreamRequestHandler.class);
        int bootstrap = indexOf(handlerTypes, JavaScriptBootstrapHandler.class);
        long bootstrapCount = handlerTypes.stream()
                .filter(JavaScriptBootstrapHandler.class::isAssignableFrom)
                .count();
        if (classpath < 0 || desktopStream < 0 || upstreamStream < 0
                || bootstrap < 0 || bootstrapCount != 1
                || desktopStream >= upstreamStream
                || upstreamStream >= bootstrap) {
            throw new IllegalStateException(
                    "Desktop resource handlers are not registered in the Vaadin request chain: "
                            + handlerTypes);
        }
    }

    private static int indexOf(List<Class<?>> types, Class<? extends RequestHandler> target) {
        for (int index = 0; index < types.size(); index++) {
            if (target.isAssignableFrom(types.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static VaadinSession vaadinSession(DesktopFlowOpaqueHttpSession opaqueSession) {
        try {
            Field field = DesktopFlowOpaqueHttpSession.class.getDeclaredField("session");
            field.setAccessible(true);
            return (VaadinSession) field.get(opaqueSession);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to inspect opaque Vaadin session", error);
        }
    }

    @Route("")
    public static final class DevelopmentView extends Div {
    }
}
