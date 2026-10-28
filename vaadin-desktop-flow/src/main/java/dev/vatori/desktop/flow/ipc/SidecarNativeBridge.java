package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/** Correlates native capability commands over the shared sidecar channel. */
final class SidecarNativeBridge implements NativeBridge, AutoCloseable {
    private final SidecarOutput output;
    private final FlowIpcScope defaultScope;
    private final boolean sessionScoped;
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentMap<String, PendingCommand> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FlowIpcScope> scopesBySession = new ConcurrentHashMap<>();
    private final Map<FlowIpcScope, ScopeLifecycle> scopeLifecycles = new HashMap<>();
    private final Set<FlowIpcScope> closedScopes = new HashSet<>();
    private final ScopeLifecycle defaultScopeLifecycle;
    private final Object lifecycleLock = new Object();
    private volatile boolean closed;

    SidecarNativeBridge(SidecarOutput output, FlowIpcScope defaultScope) {
        this(output, defaultScope, true);
    }

    static SidecarNativeBridge singleWindow(
            SidecarOutput output, FlowIpcScope defaultScope) {
        return new SidecarNativeBridge(output, defaultScope, false);
    }

    private SidecarNativeBridge(
            SidecarOutput output,
            FlowIpcScope defaultScope,
            boolean sessionScoped) {
        this.output = Objects.requireNonNull(output, "output");
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
        this.sessionScoped = sessionScoped;
        this.defaultScopeLifecycle = new ScopeLifecycle(defaultScope);
        scopeLifecycles.put(defaultScope, defaultScopeLifecycle);
    }

    @Override
    public DesktopCommandResult invoke(DesktopCommand command) {
        return invoke(command, null);
    }

    @Override
    public NativeBridge captureCapabilityContext() {
        synchronized (lifecycleLock) {
            if (closed) {
                return new BoundNativeBridge(defaultScopeLifecycle);
            }
            return new BoundNativeBridge(currentScopeLifecycleLocked());
        }
    }

    private DesktopCommandResult invoke(
            DesktopCommand command, ScopeLifecycle capturedScope) {
        String id = "desktop-command-" + sequence.incrementAndGet();
        CompletableFuture<DesktopCommandResult> result = new CompletableFuture<>();
        PendingCommand commandState = null;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", command.name());
        payload.put("args", command.payload());
        try {
            synchronized (lifecycleLock) {
                if (closed) {
                    return DesktopCommandResult.error(
                            "Sidecar native bridge is closed");
                }
                ScopeLifecycle scopeLifecycle = capturedScope == null
                        ? currentScopeLifecycleLocked()
                        : capturedScope;
                FlowIpcScope scope = scopeLifecycle.scope;
                if (scopeLifecycle.closed
                        || (capturedScope != null
                                && !scope.equals(defaultScope)
                                && !scope.equals(scopesBySession.get(
                                        scope.sessionId())))) {
                    scopeLifecycle.closed = true;
                    return windowClosedResult(command.name());
                }
                commandState = new PendingCommand(
                        scope, command.name(), result);
                pending.put(id, commandState);
                output.send(scope, "command", id, id, payload);
            }
            return result.get();
        } catch (RuntimeException error) {
            return DesktopCommandResult.error(Objects.requireNonNullElse(
                    error.getMessage(), "Unable to send native capability command"));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return DesktopCommandResult.error(
                    "Interrupted while waiting for native capability result");
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            return DesktopCommandResult.error(Objects.requireNonNullElse(
                    cause == null ? null : cause.getMessage(),
                    "Native capability command failed"));
        } finally {
            if (commandState != null) {
                pending.remove(id, commandState);
            }
        }
    }

    void registerScope(FlowIpcScope scope) {
        synchronized (lifecycleLock) {
            if (closed) {
                throw new IllegalStateException(
                        "Sidecar native bridge is closed");
            }
            if (closedScopes.contains(scope)) {
                throw new IllegalStateException(
                        "Closed Flow IPC scope cannot be reactivated: "
                                + scope.windowId());
            }
            FlowIpcScope existing = scopesBySession.get(scope.sessionId());
            if (existing != null && !existing.equals(scope)) {
                throw new SecurityException(
                        "Flow IPC session already belongs to window "
                                + existing.windowId());
            }
            scopesBySession.put(scope.sessionId(), scope);
            scopeLifecycles.computeIfAbsent(scope, ScopeLifecycle::new);
        }
    }

    void unregisterScope(FlowIpcScope scope) {
        synchronized (lifecycleLock) {
            scopesBySession.remove(scope.sessionId(), scope);
            if (scope.equals(defaultScope)) {
                closedScopes.add(scope);
                defaultScopeLifecycle.closed = true;
            } else {
                ScopeLifecycle lifecycle = scopeLifecycles.remove(scope);
                if (lifecycle != null) {
                    lifecycle.closed = true;
                }
            }
        }
    }

    void rollbackScopeRegistration(FlowIpcScope scope) {
        synchronized (lifecycleLock) {
            scopesBySession.remove(scope.sessionId(), scope);
            if (closedScopes.contains(scope) || scope.equals(defaultScope)) {
                return;
            }
            ScopeLifecycle lifecycle = scopeLifecycles.get(scope);
            if (lifecycle != null && !lifecycle.closed) {
                scopeLifecycles.remove(scope, lifecycle);
            }
        }
    }

    void closeScope(FlowIpcScope scope) {
        List<PendingCommand> closedCommands = new ArrayList<>();
        synchronized (lifecycleLock) {
            closedScopes.add(scope);
            scopeLifecycles.computeIfAbsent(scope, ScopeLifecycle::new).closed = true;
            pending.forEach((id, command) -> {
                if (command.scope.equals(scope)
                        && pending.remove(id, command)) {
                    closedCommands.add(command);
                }
            });
        }
        closedCommands.forEach(command -> command.result.complete(
                windowClosedResult(command.command)));
    }

    void complete(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        String correlationId = envelope.correlationId();
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Capability result correlation id is required");
        }
        PendingCommand command = pending.remove(correlationId);
        if (command == null) {
            return;
        }
        if (!command.scope.equals(scope)) {
            command.result.complete(DesktopCommandResult.error(
                    "Native capability result scope mismatch"));
            throw new SecurityException("Native capability result scope mismatch");
        }
        try {
            Map<String, Object> payload = envelope.payloadMap();
            if (envelope.booleanValue("success", false)) {
                command.result.complete(DesktopCommandResult.ok(payload));
                return;
            }
            String message = envelope.errorString("message");
            if (message == null || message.isBlank()) {
                Object payloadError = payload.get("error");
                message = payloadError == null
                        ? "Native capability command failed"
                        : String.valueOf(payloadError);
            }
            command.result.complete(DesktopCommandResult.error(payload, message));
        } catch (RuntimeException error) {
            command.result.complete(DesktopCommandResult.error(
                    "Invalid native capability result payload"));
            throw error;
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            closed = true;
            pending.forEach((id, command) -> command.result.complete(
                    DesktopCommandResult.error("Sidecar runtime closed")));
            pending.clear();
            closedScopes.clear();
            scopeLifecycles.values().forEach(lifecycle -> lifecycle.closed = true);
            scopeLifecycles.clear();
        }
        scopesBySession.clear();
    }

    private ScopeLifecycle currentScopeLifecycleLocked() {
        if (!sessionScoped) {
            return defaultScopeLifecycle;
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            if (defaultScopeLifecycle.closed) {
                return defaultScopeLifecycle;
            }
            Set<FlowIpcScope> liveScopes = new HashSet<>();
            liveScopes.add(defaultScope);
            scopesBySession.values().forEach(scope -> {
                ScopeLifecycle lifecycle = scopeLifecycles.get(scope);
                if (lifecycle != null && !lifecycle.closed) {
                    liveScopes.add(scope);
                }
            });
            if (liveScopes.size() > 1) {
                throw new IllegalStateException(
                        "Native capability scope is ambiguous without a current Vaadin session; "
                                + "capture a DesktopCapabilityContext before leaving the Flow callback");
            }
            return defaultScopeLifecycle;
        }
        if (session.getSession() == null) {
            throw new IllegalStateException(
                    "Closed Vaadin session has no active Flow IPC scope");
        }
        FlowIpcScope scope = scopesBySession.get(session.getSession().getId());
        if (scope == null) {
            throw new IllegalStateException(
                    "Vaadin session has no active Flow IPC scope");
        }
        ScopeLifecycle lifecycle = scopeLifecycles.get(scope);
        if (lifecycle == null) {
            throw new IllegalStateException(
                    "Vaadin session has no active Flow IPC scope");
        }
        return lifecycle;
    }

    private static DesktopCommandResult windowClosedResult(String command) {
        String message =
                "Owner window closed before native capability completed";
        return DesktopCommandResult.error(Map.of(
                "success", false,
                "command", command,
                "canceled", true,
                "cancelReason", "owner-window-closed",
                "operationInvalidated", true,
                "error", message), message);
    }

    private record PendingCommand(
            FlowIpcScope scope,
            String command,
            CompletableFuture<DesktopCommandResult> result) {
    }

    private final class BoundNativeBridge implements NativeBridge {
        private final ScopeLifecycle scopeLifecycle;

        private BoundNativeBridge(ScopeLifecycle scopeLifecycle) {
            this.scopeLifecycle = scopeLifecycle;
        }

        @Override
        public DesktopCommandResult invoke(DesktopCommand command) {
            return SidecarNativeBridge.this.invoke(command, scopeLifecycle);
        }

        @Override
        public NativeBridge captureCapabilityContext() {
            return this;
        }
    }

    private static final class ScopeLifecycle {
        private final FlowIpcScope scope;
        private boolean closed;

        private ScopeLifecycle(FlowIpcScope scope) {
            this.scope = scope;
        }
    }
}
