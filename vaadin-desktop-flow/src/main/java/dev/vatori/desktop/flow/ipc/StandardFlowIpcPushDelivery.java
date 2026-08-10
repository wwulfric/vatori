package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.flow.transport.DesktopFlowPushMessage;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Retains scoped opaque pushes until the Flow client confirms reception. */
final class StandardFlowIpcPushDelivery implements AutoCloseable {
    static final long RETRY_INTERVAL_MILLIS = 1_000;

    private final Sender sender;
    private final ConcurrentMap<String, ScopeState> statesByWindow =
            new ConcurrentHashMap<>();
    private final ScheduledExecutorService retryExecutor;
    private final AtomicBoolean closed = new AtomicBoolean();

    StandardFlowIpcPushDelivery(Sender sender) {
        this(sender, true);
    }

    StandardFlowIpcPushDelivery(Sender sender, boolean startRetryLoop) {
        this.sender = Objects.requireNonNull(sender, "sender");
        if (startRetryLoop) {
            retryExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable, "vaadin-desktop-push-delivery");
                thread.setDaemon(true);
                return thread;
            });
            retryExecutor.scheduleWithFixedDelay(
                    this::retryUnacknowledged,
                    RETRY_INTERVAL_MILLIS,
                    RETRY_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS);
        } else {
            retryExecutor = null;
        }
    }

    void registerScope(FlowIpcScope scope) {
        requireOpen();
        Objects.requireNonNull(scope, "scope");
        statesByWindow.compute(scope.windowId(), (ignored, existing) -> {
            if (existing == null) {
                return new ScopeState(scope);
            }
            existing.requireScope(scope);
            return existing;
        });
    }

    void connect(
            FlowIpcScope scope,
            String connectionId,
            int lastSeenServerSyncId) {
        state(scope).connect(connectionId, lastSeenServerSyncId);
    }

    void disconnect(FlowIpcScope scope, String connectionId) {
        ScopeState state = statesByWindow.get(scope.windowId());
        if (state != null) {
            state.requireScope(scope);
            state.disconnect(connectionId);
        }
    }

    void send(FlowIpcScope scope, DesktopFlowPushMessage message) {
        Delivery delivery = state(scope).enqueue(message);
        sender.send(delivery);
    }

    boolean acknowledge(
            FlowIpcScope scope,
            String connectionId,
            long pushSequence,
            int lastSeenServerSyncId) {
        return state(scope).acknowledge(
                connectionId, pushSequence, lastSeenServerSyncId);
    }

    void removeScope(FlowIpcScope scope) {
        ScopeState state = statesByWindow.get(scope.windowId());
        if (state == null) {
            return;
        }
        state.requireScope(scope);
        statesByWindow.remove(scope.windowId(), state);
    }

    void retryUnacknowledged() {
        if (closed.get()) {
            return;
        }
        for (ScopeState state : statesByWindow.values()) {
            Delivery delivery = state.nextRetry();
            if (delivery == null) {
                continue;
            }
            try {
                sender.send(delivery);
            } catch (RuntimeException error) {
                error.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (retryExecutor != null) {
            retryExecutor.shutdownNow();
        }
        statesByWindow.clear();
    }

    private ScopeState state(FlowIpcScope scope) {
        requireOpen();
        Objects.requireNonNull(scope, "scope");
        ScopeState state = statesByWindow.get(scope.windowId());
        if (state == null) {
            throw new IllegalStateException(
                    "Flow IPC push scope is not registered for window "
                            + scope.windowId());
        }
        state.requireScope(scope);
        return state;
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Flow IPC push delivery is closed");
        }
    }

    @FunctionalInterface
    interface Sender {
        void send(Delivery delivery);
    }

    record Delivery(
            FlowIpcScope scope,
            String connectionId,
            int flowUiId,
            long pushSequence,
            int flowServerSyncId,
            String uidl,
            boolean resync,
            int deliveryAttempt) {
        Delivery {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(connectionId, "connectionId");
            Objects.requireNonNull(uidl, "uidl");
            if (connectionId.isBlank() || flowUiId < 0 || pushSequence <= 0
                    || flowServerSyncId < -1 || deliveryAttempt <= 0) {
                throw new IllegalArgumentException(
                        "Invalid opaque push delivery metadata");
            }
        }

        Delivery nextAttempt() {
            return new Delivery(
                    scope,
                    connectionId,
                    flowUiId,
                    pushSequence,
                    flowServerSyncId,
                    uidl,
                    resync,
                    deliveryAttempt + 1);
        }
    }

    private static final class ScopeState {
        private final FlowIpcScope scope;
        private final NavigableMap<Long, Delivery> pending = new TreeMap<>();
        private String activeConnectionId = "";
        private boolean connected;
        private long nextPushSequence;

        private ScopeState(FlowIpcScope scope) {
            this.scope = scope;
        }

        private synchronized void connect(
                String connectionId,
                int lastSeenServerSyncId) {
            requireConnectionId(connectionId);
            if (!activeConnectionId.equals(connectionId)) {
                pending.clear();
            }
            activeConnectionId = connectionId;
            connected = true;
            if (lastSeenServerSyncId >= 0) {
                pending.values().removeIf(delivery ->
                        delivery.connectionId().equals(connectionId)
                                && delivery.flowServerSyncId()
                                <= lastSeenServerSyncId);
            }
        }

        private synchronized void disconnect(String connectionId) {
            requireConnectionId(connectionId);
            if (!activeConnectionId.equals(connectionId)) {
                throw new SecurityException(
                        "Invalid desktop push connection id");
            }
            connected = false;
        }

        private synchronized Delivery enqueue(DesktopFlowPushMessage message) {
            Objects.requireNonNull(message, "message");
            if (!connected
                    || !activeConnectionId.equals(message.connectionId())) {
                throw new IllegalStateException(
                        "Opaque push delivery has no active browser connection");
            }
            if (message.resync()) {
                pending.clear();
            }
            Delivery delivery = new Delivery(
                    scope,
                    message.connectionId(),
                    message.uiId(),
                    ++nextPushSequence,
                    message.flowServerSyncId(),
                    message.uidl(),
                    message.resync(),
                    1);
            pending.put(delivery.pushSequence(), delivery);
            return delivery;
        }

        private synchronized boolean acknowledge(
                String connectionId,
                long pushSequence,
                int lastSeenServerSyncId) {
            requireConnectionId(connectionId);
            if (!connected || !activeConnectionId.equals(connectionId)) {
                throw new SecurityException(
                        "Invalid desktop push connection id");
            }
            if (pushSequence <= 0 || pushSequence > nextPushSequence) {
                throw new IllegalArgumentException(
                        "Invalid opaque push acknowledgement sequence "
                                + pushSequence);
            }
            Delivery acknowledged = pending.get(pushSequence);
            if (acknowledged == null) {
                return false;
            }
            if (acknowledged.flowServerSyncId() >= 0
                    && lastSeenServerSyncId
                    < acknowledged.flowServerSyncId()) {
                return false;
            }
            Iterator<Map.Entry<Long, Delivery>> iterator =
                    pending.headMap(pushSequence, true).entrySet().iterator();
            while (iterator.hasNext()) {
                Delivery delivery = iterator.next().getValue();
                if (delivery.connectionId().equals(connectionId)
                        && (delivery.flowServerSyncId() < 0
                        || delivery.flowServerSyncId()
                        <= lastSeenServerSyncId)) {
                    iterator.remove();
                }
            }
            return true;
        }

        private synchronized Delivery nextRetry() {
            if (!connected) {
                return null;
            }
            for (Map.Entry<Long, Delivery> entry : pending.entrySet()) {
                Delivery delivery = entry.getValue();
                if (!delivery.connectionId().equals(activeConnectionId)) {
                    continue;
                }
                Delivery retry = delivery.nextAttempt();
                entry.setValue(retry);
                return retry;
            }
            return null;
        }

        private void requireScope(FlowIpcScope candidate) {
            if (!scope.equals(candidate)) {
                throw new SecurityException("Flow IPC window scope mismatch");
            }
        }

        private static void requireConnectionId(String connectionId) {
            if (connectionId == null || connectionId.isBlank()) {
                throw new IllegalArgumentException(
                        "Opaque push connection id is missing");
            }
        }
    }
}
