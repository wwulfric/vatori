package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import jakarta.servlet.http.Cookie;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OpaqueHttpAdapterSmokeMain {
    private OpaqueHttpAdapterSmokeMain() {
    }

    public static void main(String[] args) {
        verifyRepeatedCookieHeaders();
        verifyOpaqueStaticResource();
        verifyStreamResourceThroughRequestChain();
        System.out.println("Opaque HTTP adapter smoke passed");
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
        if (classpath < 0 || desktopStream < 0 || upstreamStream < 0
                || desktopStream >= upstreamStream) {
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
}
