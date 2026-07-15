package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import dev.vatori.desktop.flow.transport.DesktopFlowPushMessage;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

final class StandardFlowIpcSessions implements AutoCloseable {
    private final List<Class<? extends Component>> routeClasses;
    private final ApplicationContext applicationContext;
    private final StandardFlowIpcOutput output;
    private final FlowIpcNativeBridge nativeBridge;
    private final ConcurrentMap<String, SessionContext> sessionsByWindow =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionContext> sessionsBySession =
            new ConcurrentHashMap<>();
    private DesktopFlowOpaqueHttpSessionRegistry sessionRegistry;

    StandardFlowIpcSessions(
            Collection<Class<? extends Component>> routeClasses,
            ApplicationContext applicationContext,
            StandardFlowIpcOutput output,
            FlowIpcNativeBridge nativeBridge) {
        this.routeClasses = List.copyOf(routeClasses);
        this.applicationContext = applicationContext;
        this.output = Objects.requireNonNull(output, "output");
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
    }

    DesktopFlowOpaqueHttpSession session(FlowIpcScope scope) {
        return context(scope).session;
    }

    void connectPush(
            FlowIpcScope scope,
            FlowIpcInboundEnvelope envelope,
            String id,
            String correlationId) {
        SessionContext context = context(scope);
        int flowUiId = envelope.integer("flowUiId", -1);
        String pushId = requiredPushMetadata(envelope, "pushId");
        String connectionId = requiredPushMetadata(envelope, "connectionId");
        if (flowUiId < 0) {
            throw new IllegalArgumentException("Opaque push flowUiId is missing");
        }
        context.session.validatePush(flowUiId, pushId);
        output.send(scope, "opaquePushConnected", output.nextId("push-connect"),
                StandardFlowIpcOutput.correlationIdOr(id, correlationId),
                Map.of(
                        "connectionId", connectionId,
                        "flowUiId", flowUiId,
                        "transport", "opaque-ipc-push"));
        context.session.connectPush(flowUiId, connectionId,
                envelope.integer("lastSeenServerSyncId", -1));
    }

    void disconnectPush(FlowIpcScope scope, FlowIpcInboundEnvelope envelope) {
        SessionContext context = sessionsByWindow.get(scope.windowId());
        if (context != null) {
            context.requireScope(scope);
            context.session.disconnectPush(
                    envelope.integer("flowUiId", -1),
                    requiredPushMetadata(envelope, "connectionId"),
                    requiredPushMetadata(envelope, "pushId"));
        }
    }

    void resyncPush(FlowIpcScope scope, FlowIpcInboundEnvelope envelope) {
        context(scope).session.resyncPush(
                envelope.integer("flowUiId", -1),
                requiredPushMetadata(envelope, "connectionId"),
                requiredPushMetadata(envelope, "pushId"));
    }

    void closeWindow(FlowIpcScope scope, Runnable cancelExchanges) {
        Objects.requireNonNull(cancelExchanges, "cancelExchanges");
        SessionContext context = sessionsByWindow.get(scope.windowId());
        if (context == null) {
            return;
        }
        context.requireScope(scope);
        if (!sessionsByWindow.remove(scope.windowId(), context)) {
            return;
        }
        sessionsBySession.remove(scope.sessionId(), context);
        nativeBridge.unregisterScope(scope);
        cancelExchanges.run();
        if (sessionRegistry != null) {
            sessionRegistry.closeSession(scope.sessionId());
        }
    }

    @Override
    public void close() {
        sessionsByWindow.values().stream()
                .map(context -> context.scope)
                .distinct()
                .forEach(nativeBridge::unregisterScope);
        sessionsByWindow.clear();
        sessionsBySession.clear();
        if (sessionRegistry != null) {
            sessionRegistry.close();
        }
    }

    private SessionContext context(FlowIpcScope scope) {
        SessionContext existing = sessionsByWindow.get(scope.windowId());
        if (existing != null) {
            existing.requireScope(scope);
            return existing;
        }
        synchronized (sessionsByWindow) {
            existing = sessionsByWindow.get(scope.windowId());
            if (existing != null) {
                existing.requireScope(scope);
                return existing;
            }
            if (sessionRegistry == null) {
                sessionRegistry = DesktopFlowOpaqueHttpSessionRegistry.create(
                        "flow-ipc", routeClasses, applicationContext, this::sendPush);
            }
            SessionContext sessionOwner = sessionsBySession.get(scope.sessionId());
            if (sessionOwner != null) {
                if (!sessionOwner.scope.equals(scope)) {
                    throw new IllegalArgumentException(
                            "Flow IPC session id is already assigned to window "
                                    + sessionOwner.scope.windowId());
                }
                sessionsByWindow.put(scope.windowId(), sessionOwner);
                return sessionOwner;
            }
            SessionContext created = new SessionContext(
                    scope, sessionRegistry.session(scope.sessionId()));
            nativeBridge.registerScope(scope);
            sessionsBySession.put(scope.sessionId(), created);
            sessionsByWindow.put(scope.windowId(), created);
            return created;
        }
    }

    private void sendPush(DesktopFlowPushMessage message) {
        SessionContext context = sessionsBySession.get(message.sessionId());
        if (context == null) {
            return;
        }
        output.send(context.scope, "opaquePush", output.nextId("push"),
                message.connectionId(),
                Map.of(
                        "connectionId", message.connectionId(),
                        "flowUiId", message.uiId(),
                        "pushSequence", context.pushSequence.incrementAndGet(),
                        "uidl", message.uidl(),
                        "resync", message.resync(),
                        "transport", "opaque-ipc-push"));
    }

    private static String requiredPushMetadata(
            FlowIpcInboundEnvelope envelope,
            String name) {
        String value = envelope.string(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Opaque push " + name + " is missing");
        }
        return value;
    }

    private static final class SessionContext {
        private final FlowIpcScope scope;
        private final DesktopFlowOpaqueHttpSession session;
        private final AtomicLong pushSequence = new AtomicLong();

        private SessionContext(
                FlowIpcScope scope,
                DesktopFlowOpaqueHttpSession session) {
            this.scope = scope;
            this.session = session;
        }

        private void requireScope(FlowIpcScope candidate) {
            if (!scope.equals(candidate)) {
                throw new SecurityException("Flow IPC window scope mismatch");
            }
        }
    }
}
