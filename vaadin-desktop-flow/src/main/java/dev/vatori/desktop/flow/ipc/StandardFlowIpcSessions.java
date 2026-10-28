package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.InstantiatorFactory;
import dev.vatori.desktop.flow.DesktopFlowMultipartConfig;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import dev.vatori.desktop.flow.transport.DesktopFlowPushMessage;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class StandardFlowIpcSessions implements AutoCloseable {
    private final List<Class<? extends Component>> routeClasses;
    private final InstantiatorFactory instantiatorFactory;
    private final SidecarOutput output;
    private final SidecarNativeBridge nativeBridge;
    private final boolean productionMode;
    private final Path projectDirectory;
    private final DesktopFlowMultipartConfig multipartConfig;
    private final StandardFlowIpcPushDelivery pushDelivery;
    private final ConcurrentMap<String, SessionContext> sessionsByWindow =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionContext> sessionsBySession =
            new ConcurrentHashMap<>();
    private DesktopFlowOpaqueHttpSessionRegistry sessionRegistry;

    StandardFlowIpcSessions(
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            SidecarOutput output,
            SidecarNativeBridge nativeBridge,
            boolean productionMode,
            Path projectDirectory) {
        this(routeClasses, instantiatorFactory, output, nativeBridge,
                productionMode, projectDirectory,
                DesktopFlowMultipartConfig.defaults());
    }

    StandardFlowIpcSessions(
            Collection<Class<? extends Component>> routeClasses,
            InstantiatorFactory instantiatorFactory,
            SidecarOutput output,
            SidecarNativeBridge nativeBridge,
            boolean productionMode,
            Path projectDirectory,
            DesktopFlowMultipartConfig multipartConfig) {
        this.routeClasses = List.copyOf(routeClasses);
        this.instantiatorFactory = instantiatorFactory;
        this.output = Objects.requireNonNull(output, "output");
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.productionMode = productionMode;
        this.projectDirectory = projectDirectory;
        this.multipartConfig = Objects.requireNonNull(
                multipartConfig, "multipartConfig");
        this.pushDelivery = new StandardFlowIpcPushDelivery(
                this::sendPushEnvelope);
    }

    DesktopFlowOpaqueHttpSession session(FlowIpcScope scope) {
        return context(scope).session;
    }

    List<String> enabledFeatureFlagIds() {
        return registry().enabledFeatureFlagIds();
    }

    void connectPush(
            FlowIpcScope scope,
            SidecarInboundEnvelope envelope,
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
        int lastSeenServerSyncId =
                envelope.integer("lastSeenServerSyncId", -1);
        pushDelivery.connect(
                scope, connectionId, lastSeenServerSyncId);
        output.send(scope, "opaquePushConnected", output.nextId("push-connect"),
                SidecarOutput.correlationIdOr(id, correlationId),
                Map.of(
                        "connectionId", connectionId,
                        "flowUiId", flowUiId,
                        "transport", "opaque-ipc-push"));
        try {
            context.session.connectPush(
                    flowUiId, connectionId, lastSeenServerSyncId);
        } catch (RuntimeException error) {
            pushDelivery.disconnect(scope, connectionId);
            throw error;
        }
    }

    void disconnectPush(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        SessionContext context = sessionsByWindow.get(scope.windowId());
        if (context != null) {
            context.requireScope(scope);
            String connectionId =
                    requiredPushMetadata(envelope, "connectionId");
            context.session.disconnectPush(
                    envelope.integer("flowUiId", -1),
                    connectionId,
                    requiredPushMetadata(envelope, "pushId"));
            pushDelivery.disconnect(scope, connectionId);
        }
    }

    void resyncPush(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        context(scope).session.resyncPush(
                envelope.integer("flowUiId", -1),
                requiredPushMetadata(envelope, "connectionId"),
                requiredPushMetadata(envelope, "pushId"));
    }

    void acknowledgePush(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        SessionContext context = context(scope);
        int flowUiId = envelope.integer("flowUiId", -1);
        String pushId = requiredPushMetadata(envelope, "pushId");
        String connectionId = requiredPushMetadata(envelope, "connectionId");
        long pushSequence = envelope.longValue("pushSequence", -1);
        int lastSeenServerSyncId =
                envelope.integer("lastSeenServerSyncId", -1);
        if (flowUiId < 0) {
            throw new IllegalArgumentException(
                    "Opaque push acknowledgement flowUiId is missing");
        }
        if (pushSequence <= 0) {
            throw new IllegalArgumentException(
                    "Opaque push acknowledgement sequence is missing");
        }
        context.session.validatePushAcknowledgement(
                flowUiId, connectionId, pushId);
        pushDelivery.acknowledge(
                scope,
                connectionId,
                pushSequence,
                lastSeenServerSyncId);
    }

    void closeWindow(FlowIpcScope scope, Runnable cancelExchanges) {
        Objects.requireNonNull(cancelExchanges, "cancelExchanges");
        SessionContext context = sessionsByWindow.get(scope.windowId());
        if (context == null) {
            nativeBridge.unregisterScope(scope);
            return;
        }
        context.requireScope(scope);
        if (!sessionsByWindow.remove(scope.windowId(), context)) {
            return;
        }
        sessionsBySession.remove(scope.sessionId(), context);
        try {
            pushDelivery.removeScope(scope);
            cancelExchanges.run();
            if (sessionRegistry != null) {
                sessionRegistry.closeSession(scope.sessionId());
            }
        } finally {
            nativeBridge.unregisterScope(scope);
        }
    }

    @Override
    public void close() {
        pushDelivery.close();
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
            DesktopFlowOpaqueHttpSession createdSession = null;
            SessionContext created = null;
            boolean pushRegistered = false;
            boolean nativeRegistered = false;
            boolean sessionPublished = false;
            boolean windowPublished = false;
            try {
                createdSession = registry().session(scope.sessionId());
                pushDelivery.registerScope(scope);
                pushRegistered = true;
                nativeBridge.registerScope(scope);
                nativeRegistered = true;
                created = new SessionContext(scope, createdSession);
                if (sessionsBySession.putIfAbsent(
                        scope.sessionId(), created) != null) {
                    throw new IllegalStateException(
                            "Flow IPC session owner changed during creation");
                }
                sessionPublished = true;
                if (sessionsByWindow.putIfAbsent(
                        scope.windowId(), created) != null) {
                    throw new IllegalStateException(
                            "Flow IPC window owner changed during creation");
                }
                windowPublished = true;
                return created;
            } catch (RuntimeException | Error failure) {
                SessionContext rollbackContext = created;
                DesktopFlowOpaqueHttpSession rollbackSession = createdSession;
                if (windowPublished) {
                    suppressCleanupFailure(failure, () -> sessionsByWindow.remove(
                            scope.windowId(), rollbackContext));
                }
                if (sessionPublished) {
                    suppressCleanupFailure(failure, () -> sessionsBySession.remove(
                            scope.sessionId(), rollbackContext));
                }
                if (pushRegistered) {
                    suppressCleanupFailure(
                            failure, () -> pushDelivery.removeScope(scope));
                }
                if (rollbackSession != null && sessionRegistry != null) {
                    suppressCleanupFailure(failure, () -> sessionRegistry.closeSession(
                            scope.sessionId(), rollbackSession));
                }
                if (nativeRegistered) {
                    suppressCleanupFailure(
                            failure,
                            () -> nativeBridge.rollbackScopeRegistration(scope));
                }
                throw failure;
            }
        }
    }

    private static void suppressCleanupFailure(
            Throwable failure,
            Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException | Error cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    private synchronized DesktopFlowOpaqueHttpSessionRegistry registry() {
        if (sessionRegistry == null) {
            sessionRegistry = DesktopFlowOpaqueHttpSessionRegistry.create(
                    "flow-ipc", routeClasses, instantiatorFactory, this::sendPush,
                    productionMode, projectDirectory, multipartConfig);
        }
        return sessionRegistry;
    }

    private void sendPush(DesktopFlowPushMessage message) {
        SessionContext context = sessionsBySession.get(message.sessionId());
        if (context == null) {
            return;
        }
        pushDelivery.send(context.scope, message);
    }

    private void sendPushEnvelope(
            StandardFlowIpcPushDelivery.Delivery delivery) {
        output.send(delivery.scope(), "opaquePush", output.nextId("push"),
                delivery.connectionId(),
                Map.of(
                        "connectionId", delivery.connectionId(),
                        "flowUiId", delivery.flowUiId(),
                        "pushSequence", delivery.pushSequence(),
                        "flowServerSyncId", delivery.flowServerSyncId(),
                        "uidl", delivery.uidl(),
                        "resync", delivery.resync(),
                        "deliveryAttempt", delivery.deliveryAttempt(),
                        "transport", "opaque-ipc-push"));
    }

    private static String requiredPushMetadata(
            SidecarInboundEnvelope envelope,
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
