package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.flow.transport.DesktopFlowRuntimeSession;
import dev.vatori.desktop.flow.transport.FlowPatchResponse;
import dev.vatori.desktop.ipc.FlowIpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FlowIpcRuntime {
    static final String PREFIX = "VFIPC1 ";

    private final Class<? extends Component> routeClass;
    private final String route;
    private final FlowIpcRenderer renderer = new FlowIpcRenderer();
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong clientToServerSequence = new AtomicLong();
    private Component root;
    private DesktopFlowRuntimeSession flowSession;

    FlowIpcRuntime(Class<? extends Component> routeClass) {
        this.routeClass = Objects.requireNonNull(routeClass, "routeClass");
        Route routeAnnotation = routeClass.getAnnotation(Route.class);
        this.route = routeAnnotation == null ? "" : routeAnnotation.value();
    }

    void run() {
        send("ready", "flow-ipc-ready", "flow-ipc-ready", Map.of(
                "transport", "flow-ipc",
                "compatibilityMode", false,
                "route", route,
                "runtime", "repository-local-flow-ipc-adapter",
                "uidlWriter", true,
                "uidlRpcDispatch", true,
                "uidlConsumer", "trace-only"));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(PREFIX)) {
                    continue;
                }
                handle(line.substring(PREFIX.length()));
            }
        } catch (IOException error) {
            sendError("io", "Unable to read Flow IPC stdin: " + error.getMessage(), null, null);
        }
    }

    private void handle(String json) {
        String type = JsonFields.string(json, "type");
        String id = JsonFields.string(json, "id");
        String correlationId = JsonFields.string(json, "correlationId");
        try {
            switch (type == null ? "" : type) {
                case "bootstrapRequest" -> bootstrap(id, correlationId);
                case "event" -> handleEvent(json, id, correlationId);
                case "resync" -> sendPatch("resync", id, correlationId);
                case "shutdown" -> System.exit(0);
                default -> sendError("unknownMessage", "Unsupported Flow IPC message type: " + type, id, correlationId);
            }
        } catch (RuntimeException error) {
            error.printStackTrace(System.err);
            sendError("runtime", error.getMessage() == null ? error.toString() : error.getMessage(), id, correlationId);
        }
    }

    private void bootstrap(String id, String correlationId) {
        ensureBootstrapped();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transport", "flow-ipc");
        payload.put("compatibilityMode", false);
        payload.put("route", route);
        payload.put("uidl", uidlPayload("bootstrap", false));
        payload.put("tree", renderer.render(root));
        send("bootstrapResponse", nextId("bootstrap"), correlationIdOr(id, correlationId), payload);
        send("push", nextId("push"), correlationIdOr(id, correlationId), Map.of(
                "reason", "server-initiated-ready",
                "transport", "flow-ipc",
                "uidlWriter", true));
    }

    private Component instantiateRoute() {
        try {
            return routeClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException error) {
            throw new IllegalStateException("Unable to instantiate Flow IPC route "
                    + routeClass.getName() + ": " + error.getMessage(), error);
        }
    }

    private void handleEvent(String json, String id, String correlationId) {
        ensureBootstrapped();
        String action = JsonFields.string(json, "action");
        String componentId = JsonFields.string(json, "componentId");
        if (!"click".equals(action)) {
            sendError("unsupportedEvent", "Only click events are supported by the v1 minimum adapter.", id, correlationId);
            return;
        }
        Component target = renderer.component(componentId);
        if (target instanceof Button button) {
            int nodeId = button.getElement().getNode().getId();
            FlowPatchResponse response = handleDomEvent(nodeId, "click");
            sendPatch("event", id, correlationId, response);
        } else {
            sendError("unsupportedTarget", "No clickable Vaadin Button registered for componentId=" + componentId,
                    id, correlationId);
        }
    }

    private FlowPatchResponse handleDomEvent(int nodeId, String eventType) {
        try {
            return flowSession.handleDomEvent(nodeId, eventType, (int) clientToServerSequence.getAndIncrement());
        } catch (IOException error) {
            throw new IllegalStateException("Unable to dispatch Flow UIDL RPC event: " + error.getMessage(), error);
        }
    }

    private void sendPatch(String reason, String id, String correlationId) {
        sendPatch(reason, id, correlationId, null);
    }

    private void sendPatch(String reason, String id, String correlationId, FlowPatchResponse uidlResponse) {
        ensureBootstrapped();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("patchType", "replaceRoot");
        payload.put("reason", reason);
        payload.put("transport", "flow-ipc");
        payload.put("compatibilityMode", false);
        payload.put("uidl", uidlResponse == null
                ? uidlPayload(reason, "resync".equals(reason))
                : uidlPayload(reason, uidlResponse));
        payload.put("tree", renderer.render(root));
        send("patch", nextId("patch"), correlationIdOr(id, correlationId), payload);
    }

    private void ensureBootstrapped() {
        if (root == null) {
            root = instantiateRoute();
            flowSession = DesktopFlowRuntimeSession.create(
                    "flow-ipc-session",
                    1,
                    "flow-ipc",
                    root);
        }
        if (flowSession == null) {
            flowSession = DesktopFlowRuntimeSession.create(
                    "flow-ipc-session",
                    1,
                    "flow-ipc",
                    root);
        }
    }

    private Map<String, Object> uidlPayload(String reason, boolean resync) {
        String uidlJson = flowSession.createUidl(resync);
        Map<String, Object> uidl = new LinkedHashMap<>();
        uidl.put("source", "UidlWriter");
        uidl.put("format", "vaadin-uidl-json");
        uidl.put("reason", reason);
        uidl.put("resync", resync);
        uidl.put("desktopSession", true);
        uidl.put("consumedByWebView", false);
        uidl.put("containsChanges", uidlJson.contains("\"changes\""));
        uidl.put("containsSyncId", uidlJson.contains("\"syncId\""));
        uidl.put("body", uidlJson);
        return uidl;
    }

    private Map<String, Object> uidlPayload(String reason, FlowPatchResponse response) {
        String uidlJson = response.uidlJson();
        Map<String, Object> uidl = new LinkedHashMap<>();
        uidl.put("source", "UidlRequestHandler");
        uidl.put("format", "vaadin-uidl-json");
        uidl.put("reason", reason);
        uidl.put("resync", response.resync());
        uidl.put("desktopSession", response.desktopVaadinRequestUsed());
        uidl.put("consumedByWebView", false);
        uidl.put("statusCode", response.statusCode());
        uidl.put("contentType", response.contentType());
        uidl.put("containsChanges", uidlJson.contains("\"changes\""));
        uidl.put("containsSyncId", uidlJson.contains("\"syncId\""));
        uidl.put("body", uidlJson);
        return uidl;
    }

    private void send(String type, String id, String correlationId, Map<String, Object> payload) {
        FlowIpcEnvelope envelope = FlowIpcEnvelope.message(type, id, correlationId,
                "flow-ipc-session", "main", route, payload);
        System.out.println(PREFIX + IpcJson.stringify(envelope));
        System.out.flush();
    }

    private void sendError(String code, String message, String id, String correlationId) {
        FlowIpcEnvelope envelope = FlowIpcEnvelope.error(
                id == null ? nextId("error") : id,
                correlationId,
                "flow-ipc-session",
                "main",
                route,
                code,
                message);
        System.out.println(PREFIX + IpcJson.stringify(envelope));
        System.out.flush();
    }

    private String nextId(String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }

    private static String correlationIdOr(String id, String correlationId) {
        return correlationId == null || correlationId.isBlank() ? id : correlationId;
    }

    private static final class JsonFields {
        private static final Pattern STRING_FIELD = Pattern.compile(
                "\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

        private JsonFields() {
        }

        static String string(String json, String field) {
            Matcher matcher = Pattern.compile(String.format(STRING_FIELD.pattern(), Pattern.quote(field))).matcher(json);
            if (!matcher.find()) {
                return null;
            }
            return unescape(matcher.group(1));
        }

        private static String unescape(String value) {
            return value.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
    }
}
