package dev.vatori.desktop.flow;

import com.vaadin.flow.internal.JacksonUtils;
import dev.vatori.desktop.flow.routefixture.RouteDiscoveryApplication;
import dev.vatori.desktop.flow.routefixture.RouteDiscoveryService;
import dev.vatori.desktop.flow.routefixture.RouteDiscoveryView;
import dev.vatori.desktop.flow.routefixture.SecondaryRouteView;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponse;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class FlowIpcRouteResolverSmokeMain {
    private FlowIpcRouteResolverSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        FlowIpcRouteCatalog routes =
                FlowIpcRouteResolver.resolve(RouteDiscoveryApplication.class);
        if (routes.rootRoute() != RouteDiscoveryView.class) {
            throw new AssertionError(
                    "Unexpected root route: " + routes.rootRoute().getName());
        }
        if (!routes.routes().contains(SecondaryRouteView.class)
                || routes.routes().size() != 2) {
            throw new AssertionError("Flow IPC did not discover the complete route set: "
                    + routes.routes());
        }

        try (ConfigurableApplicationContext context =
                     FlowIpcSpringApplication.start(RouteDiscoveryApplication.class, new String[0]);
             DesktopFlowOpaqueHttpSessionRegistry registry =
                     DesktopFlowOpaqueHttpSessionRegistry.create(
                             "spring-route-smoke", routes.routes(), context, ignored -> {
                             })) {
            RouteDiscoveryService routeService = context.getBean(RouteDiscoveryService.class);
            if (context instanceof WebApplicationContext) {
                throw new AssertionError("Flow IPC Spring context is not a no-web application context");
            }
            DesktopFlowOpaqueHttpSession session = registry.session("spring-route-session");
            DesktopFlowHttpResponse response = session.handle(
                    "GET",
                    "https://app.vatori.invalid/?v-r=init&location=secondary&query="
                            + "&v-browserDetails=%7B%22v-wn%22%3A%22spring-route-smoke%22%7D",
                    Map.of(),
                    new byte[0]);
            ObjectMapper mapper = JacksonUtils.getMapper();
            JsonNode bootstrap = mapper.readTree(response.body());
            if (response.statusCode() != 200 || bootstrap.path("appConfig").path("uidl").isMissingNode()) {
                throw new AssertionError("Secondary Spring route bootstrap failed: status="
                        + response.statusCode() + ", body="
                        + new String(response.body(), StandardCharsets.UTF_8));
            }

            JsonNode appConfig = bootstrap.path("appConfig");
            int uiId = appConfig.path("v-uiId").asInt(-1);
            String csrfToken = appConfig.path("uidl").path("Vaadin-Security-Key").asText();
            DesktopFlowHttpResponse navigation = session.handle(
                    "POST",
                    "https://app.vatori.invalid/?v-r=uidl&v-uiId=" + uiId,
                    Map.of("Content-Type", "application/json"),
                    navigationBody(mapper, csrfToken));
            String navigationBody = new String(navigation.body(), StandardCharsets.UTF_8);
            if (navigation.statusCode() != 200
                    || !"Spring injected secondary route with UI scope".equals(routeService.lastMessage())
                    || !navigationBody.contains("Spring injected secondary route with UI scope")) {
                throw new AssertionError("Secondary Spring route navigation failed: status="
                        + navigation.statusCode() + ", body=" + navigationBody);
            }
        }
        System.out.println("Flow IPC Spring route discovery and bootstrap passed: "
                + routes.routes());
    }

    private static byte[] navigationBody(ObjectMapper mapper, String csrfToken) throws Exception {
        ObjectNode data = mapper.createObjectNode();
        data.put("route", "secondary");
        data.put("query", "");
        data.put("appShellTitle", "");
        data.putNull("historyState");
        data.put("trigger", "");

        ObjectNode event = mapper.createObjectNode();
        event.put("type", "event");
        event.put("node", 1);
        event.put("event", "ui-navigate");
        event.set("data", data);

        ObjectNode request = mapper.createObjectNode();
        request.put("csrfToken", csrfToken);
        request.putArray("rpc").add(event);
        request.put("syncId", 0);
        request.put("clientId", 0);
        return mapper.writeValueAsBytes(request);
    }
}
