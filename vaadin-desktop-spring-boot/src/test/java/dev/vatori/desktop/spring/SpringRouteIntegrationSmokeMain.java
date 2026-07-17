package dev.vatori.desktop.spring;

import com.vaadin.flow.internal.JacksonUtils;
import dev.vatori.desktop.flow.DesktopFlowApplication;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponse;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import dev.vatori.desktop.spring.routefixture.RouteDiscoveryApplication;
import dev.vatori.desktop.spring.routefixture.RouteDiscoveryService;
import dev.vatori.desktop.spring.routefixture.RouteDiscoveryView;
import dev.vatori.desktop.spring.routefixture.SecondaryRouteView;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class SpringRouteIntegrationSmokeMain {
    private SpringRouteIntegrationSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        List<Class<? extends com.vaadin.flow.component.Component>> routes =
                SpringRouteResolver.resolve(RouteDiscoveryApplication.class);
        DesktopFlowApplication routeApplication = DesktopFlowApplication
                .builder(routes.getFirst())
                .routes(routes)
                .build();
        if (routeApplication.rootRoute() != RouteDiscoveryView.class
                || !routes.contains(SecondaryRouteView.class)
                || routes.size() != 2) {
            throw new AssertionError("Spring route discovery is incomplete: " + routes);
        }

        try (ConfigurableApplicationContext context =
                     SpringFlowApplication.start(RouteDiscoveryApplication.class, new String[0]);
             DesktopFlowOpaqueHttpSessionRegistry registry =
                     DesktopFlowOpaqueHttpSessionRegistry.create(
                             "spring-route-smoke",
                             routes,
                             service -> new com.vaadin.flow.spring.SpringInstantiator(service, context),
                             ignored -> {
                             })) {
            RouteDiscoveryService routeService = context.getBean(RouteDiscoveryService.class);
            if (context instanceof WebApplicationContext) {
                throw new AssertionError("Spring Flow IPC context must be non-web");
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
            if (response.statusCode() != 200
                    || bootstrap.path("appConfig").path("uidl").isMissingNode()) {
                throw new AssertionError("Spring route bootstrap failed: status="
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
            String navigationText = new String(navigation.body(), StandardCharsets.UTF_8);
            if (navigation.statusCode() != 200
                    || !"Spring injected secondary route with UI scope".equals(
                    routeService.lastMessage())
                    || !navigationText.contains("Spring injected secondary route with UI scope")) {
                throw new AssertionError("Spring route navigation failed: status="
                        + navigation.statusCode() + ", body=" + navigationText);
            }
        }
        System.out.println("Spring route discovery, DI and UI scope passed: " + routes);
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
