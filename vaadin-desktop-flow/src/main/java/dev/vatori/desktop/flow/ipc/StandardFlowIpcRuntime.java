package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.InstantiatorFactory;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponse;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class StandardFlowIpcRuntime {
    private final String route;
    private final DesktopApplication application;
    private final SidecarOutput output;
    private final FlowFrontendMode frontendMode;
    private final StandardFlowIpcSessions sessions;
    private final StandardFlowIpcExchanges exchanges;
    private final SidecarMessageLoop messageLoop;

    StandardFlowIpcRuntime(
            String route,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            DesktopApplication application,
            SidecarOutput output,
            SidecarNativeBridge nativeBridge,
            FlowIpcScope defaultScope) {
        this(route, routeClasses, instantiatorFactory, application, output,
                nativeBridge, defaultScope,
                DesktopFlowMultipartConfig.defaults());
    }

    StandardFlowIpcRuntime(
            String route,
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            DesktopApplication application,
            SidecarOutput output,
            SidecarNativeBridge nativeBridge,
            FlowIpcScope defaultScope,
            DesktopFlowMultipartConfig multipartConfig) {
        this.route = Objects.requireNonNullElse(route, "");
        this.application = Objects.requireNonNull(application, "application");
        this.output = Objects.requireNonNull(output, "output");
        this.frontendMode = FlowFrontendMode.current();
        this.sessions = new StandardFlowIpcSessions(
                List.copyOf(routeClasses), instantiatorFactory, output, nativeBridge,
                frontendMode.productionMode(), frontendMode.projectDirectory(),
                multipartConfig);
        this.exchanges = new StandardFlowIpcExchanges(output);
        this.messageLoop = new SidecarMessageLoop(output, nativeBridge, defaultScope);
        registerHandlers();
    }

    void run() {
        Map<String, Object> ready = SidecarRuntime.readyPayload("flow-ipc", application);
        ready.put("route", route);
        ready.put("runtime", "standard-vaadin-bundle-opaque-ipc");
        ready.put("uidlWriter", true);
        ready.put("uidlRpcDispatch", true);
        ready.put("flowClientResourceLoading", frontendMode.resourceLoading());
        ready.put("componentFrontendResourceLoading", frontendMode.resourceLoading());
        ready.put("enabledFeatureFlags", sessions.enabledFeatureFlagIds());
        ready.put("uidlRpcProducer", "standard-flow-client");
        ready.put("uidlConsumer", "standard-flow-client");
        ready.put("uidlFallback", "none");
        ready.put("browserRequestTransport", "opaque-http-stream-ipc");
        output.send("ready", "sidecar-ready", "sidecar-ready", ready);
        try {
            messageLoop.run();
        } finally {
            exchanges.close();
            sessions.close();
        }
    }

    private void registerHandlers() {
        messageLoop.register("opaqueHttpRequest", this::handleBufferedRequest);
        messageLoop.register("opaqueHttpStreamOpen", (scope, envelope, id, correlationId) ->
                exchanges.open(scope, sessions.session(scope), envelope, id, correlationId));
        messageLoop.register("opaqueHttpStreamChunk", (scope, envelope, id, correlationId) ->
                exchanges.acceptChunk(scope, envelope));
        messageLoop.register("opaqueHttpStreamEnd", (scope, envelope, id, correlationId) ->
                exchanges.finishRequest(scope, envelope));
        messageLoop.register("opaqueHttpStreamResponseCredit",
                (scope, envelope, id, correlationId) ->
                        exchanges.grantResponseCredits(scope, envelope));
        messageLoop.register("opaqueHttpStreamCancel", (scope, envelope, id, correlationId) ->
                exchanges.cancel(scope, envelope));
        messageLoop.register("opaquePushConnect", sessions::connectPush);
        messageLoop.register("opaquePushDisconnect", (scope, envelope, id, correlationId) ->
                sessions.disconnectPush(scope, envelope));
        messageLoop.register("opaquePushResync", (scope, envelope, id, correlationId) ->
                sessions.resyncPush(scope, envelope));
        messageLoop.register("opaquePushAck", (scope, envelope, id, correlationId) ->
                sessions.acknowledgePush(scope, envelope));
        messageLoop.onWindowClosed(scope ->
                sessions.closeWindow(scope, () -> exchanges.cancelWindow(scope)));
    }

    private void handleBufferedRequest(
            FlowIpcScope scope,
            SidecarInboundEnvelope envelope,
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
            SidecarInboundEnvelope envelope,
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
                SidecarOutput.correlationIdOr(id, correlationId), payload);
    }
}
