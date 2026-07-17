package dev.vatori.desktop.flow;

import com.vaadin.flow.internal.JacksonUtils;
import dev.vatori.desktop.flow.routefixture.RouteDiscoveryView;
import dev.vatori.desktop.flow.routefixture.SecondaryRouteView;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponse;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DesktopFlowApplicationSmokeMain {
    private DesktopFlowApplicationSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        DesktopFlowApplication flowApplication = DesktopFlowApplication.builder(
                RouteDiscoveryView.class, SecondaryRouteView.class).build();
        if (flowApplication.rootRoute() != RouteDiscoveryView.class
                || !flowApplication.routes().contains(SecondaryRouteView.class)
                || flowApplication.routes().size() != 2) {
            throw new AssertionError("Explicit desktop routes are incomplete: "
                    + flowApplication.routes());
        }
        expectInvalid(() -> DesktopFlowApplication.builder(SecondaryRouteView.class).build());
        expectInvalid(() -> DesktopFlowApplication.builder(
                RouteDiscoveryView.class, AnotherRootView.class).build());

        try (DesktopFlowOpaqueHttpSessionRegistry registry =
                     DesktopFlowOpaqueHttpSessionRegistry.create(
                             "pure-route-smoke",
                             flowApplication.routes(),
                             null,
                             ignored -> {
                             })) {
            DesktopFlowOpaqueHttpSession session = registry.session("pure-route-session");
            DesktopFlowHttpResponse response = session.handle(
                    "GET",
                    "https://app.vatori.invalid/?v-r=init&location=secondary&query="
                            + "&v-browserDetails=%7B%22v-wn%22%3A%22pure-route-smoke%22%7D",
                    Map.of(),
                    new byte[0]);
            ObjectMapper mapper = JacksonUtils.getMapper();
            JsonNode bootstrap = mapper.readTree(response.body());
            if (response.statusCode() != 200
                    || bootstrap.path("appConfig").path("uidl").isMissingNode()) {
                throw new AssertionError("Secondary pure Flow route bootstrap failed: status="
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
                    || !navigationText.contains("Pure secondary route")) {
                throw new AssertionError("Secondary pure Flow route navigation failed: status="
                        + navigation.statusCode() + ", body=" + navigationText);
            }
        }
        System.out.println("Explicit desktop Flow routes and default Instantiator passed");
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
            throw new AssertionError("DesktopFlowApplication accepted an invalid root route set");
        } catch (IllegalArgumentException expected) {
            // Expected route contract guard.
        }
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
