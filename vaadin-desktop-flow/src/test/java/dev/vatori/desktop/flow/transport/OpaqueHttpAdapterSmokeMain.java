package dev.vatori.desktop.flow.transport;

import com.vaadin.flow.component.html.Div;
import jakarta.servlet.http.Cookie;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class OpaqueHttpAdapterSmokeMain {
    private OpaqueHttpAdapterSmokeMain() {
    }

    public static void main(String[] args) {
        verifyRepeatedCookieHeaders();
        verifyOpaqueStaticResource();
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
    }
}
