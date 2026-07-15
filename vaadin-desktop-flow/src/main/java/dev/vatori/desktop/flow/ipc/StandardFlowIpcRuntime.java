package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponse;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StandardFlowIpcRuntime {
    private static final String IPC_SESSION_ID = "flow-ipc-session";
    private static final String IPC_UI_ID = "main";
    private static final FlowIpcScope DEFAULT_SCOPE =
            new FlowIpcScope("main", IPC_SESSION_ID, IPC_UI_ID);

    private final String route;
    private final StandardFlowIpcOutput output;
    private final StandardFlowIpcSessions sessions;
    private final StandardFlowIpcExchanges exchanges;
    private final FlowIpcNativeBridge nativeBridge;
    private boolean shutdownRequested;

    StandardFlowIpcRuntime(
            Class<? extends Component> rootRouteClass,
            Collection<Class<? extends Component>> routeClasses,
            ApplicationContext applicationContext) {
        this(rootRouteClass, routeClasses, applicationContext,
                new FlowIpcNativeBridge(route(rootRouteClass), DEFAULT_SCOPE));
    }

    StandardFlowIpcRuntime(
            Class<? extends Component> rootRouteClass,
            Collection<Class<? extends Component>> routeClasses,
            ApplicationContext applicationContext,
            FlowIpcNativeBridge nativeBridge) {
        Objects.requireNonNull(rootRouteClass, "rootRouteClass");
        List<Class<? extends Component>> configuredRoutes = List.copyOf(routeClasses);
        if (!configuredRoutes.contains(rootRouteClass)) {
            throw new IllegalArgumentException("Flow IPC routes must contain the root route");
        }
        Route routeAnnotation = rootRouteClass.getAnnotation(Route.class);
        this.route = routeAnnotation == null ? "" : routeAnnotation.value();
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.output = new StandardFlowIpcOutput(route, DEFAULT_SCOPE);
        this.sessions = new StandardFlowIpcSessions(
                configuredRoutes, applicationContext, output, nativeBridge);
        this.exchanges = new StandardFlowIpcExchanges(output);
    }

    void run() {
        output.send("ready", "flow-ipc-ready", "flow-ipc-ready", Map.ofEntries(
                Map.entry("transport", "flow-ipc"),
                Map.entry("compatibilityMode", false),
                Map.entry("route", route),
                Map.entry("runtime", "standard-vaadin-bundle-opaque-ipc"),
                Map.entry("uidlWriter", true),
                Map.entry("uidlRpcDispatch", true),
                Map.entry("flowClientResourceLoading", "standard-production-bundle"),
                Map.entry("componentFrontendResourceLoading", "standard-production-bundle"),
                Map.entry("uidlRpcProducer", "standard-flow-client"),
                Map.entry("uidlConsumer", "standard-flow-client"),
                Map.entry("uidlFallback", "none"),
                Map.entry("standardBundleMode", true),
                Map.entry("browserRequestTransport", "opaque-http-stream-ipc")));

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while (!shutdownRequested && (line = reader.readLine()) != null) {
                if (line.startsWith(FlowIpcWire.PREFIX)) {
                    handle(line.substring(FlowIpcWire.PREFIX.length()));
                }
            }
        } catch (IOException error) {
            output.sendError(DEFAULT_SCOPE, "io",
                    "Unable to read Flow IPC stdin: " + error.getMessage(), null, null);
        } finally {
            close();
        }
    }

    private void handle(String json) {
        String id = null;
        String correlationId = null;
        FlowIpcScope scope = DEFAULT_SCOPE;
        try {
            FlowIpcInboundEnvelope envelope = FlowIpcInboundEnvelope.parse(json);
            String type = Objects.requireNonNullElse(envelope.type(), "");
            id = envelope.id();
            correlationId = envelope.correlationId();
            scope = FlowIpcScope.from(envelope, IPC_SESSION_ID, IPC_UI_ID);
            switch (type) {
                case "opaqueHttpRequest" ->
                        handleBufferedRequest(scope, envelope, id, correlationId);
                case "opaqueHttpStreamOpen" -> exchanges.open(
                        scope, sessions.session(scope), envelope, id, correlationId);
                case "opaqueHttpStreamChunk" -> exchanges.acceptChunk(scope, envelope);
                case "opaqueHttpStreamEnd" -> exchanges.finishRequest(scope, envelope);
                case "opaqueHttpStreamResponseCredit" ->
                        exchanges.grantResponseCredits(scope, envelope);
                case "opaqueHttpStreamCancel" -> exchanges.cancel(scope, envelope);
                case "opaquePushConnect" ->
                        sessions.connectPush(scope, envelope, id, correlationId);
                case "opaquePushDisconnect" -> sessions.disconnectPush(scope, envelope);
                case "opaquePushResync" -> sessions.resyncPush(scope, envelope);
                case "capabilityResult" -> nativeBridge.complete(scope, envelope);
                case "windowClosed" -> closeWindow(scope);
                case "shutdown" -> shutdownRequested = true;
                default -> output.sendError(scope, "unsupportedTransport",
                        "Standard bundle mode accepts only opaque IPC bridge messages: " + type,
                        id, correlationId);
            }
        } catch (SecurityException error) {
            output.sendError(scope, "invalidScope", error.getMessage(), id, correlationId);
        } catch (IllegalArgumentException | IllegalStateException error) {
            output.sendError(scope, "invalidClientState", error.getMessage(), id, correlationId);
        } catch (RuntimeException error) {
            error.printStackTrace(System.err);
            output.sendError(scope, "runtime",
                    Objects.requireNonNullElse(error.getMessage(), error.toString()),
                    id, correlationId);
        }
    }

    private void handleBufferedRequest(
            FlowIpcScope scope,
            FlowIpcInboundEnvelope envelope,
            String id,
            String correlationId) {
        try {
            DesktopFlowOpaqueHttpSession session = sessions.session(scope);
            String method = Objects.requireNonNullElse(envelope.string("method"), "GET");
            String url = StandardFlowIpcExchanges.requiredUrl(envelope);
            String bodyBase64 = Objects.requireNonNullElse(envelope.string("bodyBase64"), "");
            byte[] body;
            try {
                body = bodyBase64.isEmpty()
                        ? new byte[0]
                        : Base64.getDecoder().decode(bodyBase64);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException(
                        "Opaque HTTP request body is not valid base64", error);
            }
            DesktopFlowHttpResponse response = session.handle(
                    method, url, envelope.stringMap("headers"), body);
            sendBufferedResponse(scope, envelope, id, correlationId, response);
        } catch (RuntimeException error) {
            byte[] message = Objects.requireNonNullElse(error.getMessage(), error.toString())
                    .getBytes(StandardCharsets.UTF_8);
            sendBufferedResponse(scope, envelope, id, correlationId,
                    new DesktopFlowHttpResponse(
                            500,
                            "Internal Server Error",
                            Map.of("Content-Type", List.of("text/plain; charset=utf-8")),
                            message));
        }
    }

    private void sendBufferedResponse(
            FlowIpcScope scope,
            FlowIpcInboundEnvelope envelope,
            String id,
            String correlationId,
            DesktopFlowHttpResponse response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", Objects.requireNonNullElse(envelope.string("requestId"), id));
        payload.put("status", response.statusCode());
        payload.put("statusText", response.statusText());
        payload.put("headers", response.headers());
        payload.put("bodyBase64", Base64.getEncoder().encodeToString(response.body()));
        payload.put("transport", "opaque-http-buffered-ipc");
        output.send(scope, "opaqueHttpResponse", output.nextId("http"),
                StandardFlowIpcOutput.correlationIdOr(id, correlationId), payload);
    }

    private void closeWindow(FlowIpcScope scope) {
        sessions.closeWindow(scope, () -> exchanges.cancelWindow(scope));
    }

    private void close() {
        exchanges.close();
        sessions.close();
        nativeBridge.close();
    }

    private static String route(Class<? extends Component> routeClass) {
        Route annotation = routeClass.getAnnotation(Route.class);
        return annotation == null ? "" : annotation.value();
    }
}
