package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.server.VaadinSession;
import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/** Correlates native capability commands over the shared sidecar channel. */
final class SidecarNativeBridge implements NativeBridge, AutoCloseable {
    private final SidecarOutput output;
    private final FlowIpcScope defaultScope;
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentMap<String, PendingCommand> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FlowIpcScope> scopesBySession = new ConcurrentHashMap<>();
    private final Object lifecycleLock = new Object();
    private volatile boolean closed;

    SidecarNativeBridge(SidecarOutput output, FlowIpcScope defaultScope) {
        this.output = Objects.requireNonNull(output, "output");
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
    }

    @Override
    public DesktopCommandResult invoke(DesktopCommand command) {
        String id = "desktop-command-" + sequence.incrementAndGet();
        FlowIpcScope scope = currentScope();
        CompletableFuture<DesktopCommandResult> result = new CompletableFuture<>();
        PendingCommand commandState = new PendingCommand(scope, result);
        synchronized (lifecycleLock) {
            if (closed) {
                return DesktopCommandResult.error("Sidecar native bridge is closed");
            }
            pending.put(id, commandState);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", command.name());
        payload.put("args", command.payload());
        try {
            output.send(scope, "command", id, id, payload);
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
            pending.remove(id, commandState);
        }
    }

    void registerScope(FlowIpcScope scope) {
        scopesBySession.put(scope.sessionId(), scope);
    }

    void unregisterScope(FlowIpcScope scope) {
        scopesBySession.remove(scope.sessionId(), scope);
    }

    void complete(FlowIpcScope scope, FlowIpcInboundEnvelope envelope) {
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
        command.result.complete(DesktopCommandResult.error(message));
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            closed = true;
            pending.forEach((id, command) -> command.result.complete(
                    DesktopCommandResult.error("Sidecar runtime closed")));
            pending.clear();
        }
        scopesBySession.clear();
    }

    private FlowIpcScope currentScope() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null || session.getSession() == null) {
            return defaultScope;
        }
        return Objects.requireNonNullElse(
                scopesBySession.get(session.getSession().getId()), defaultScope);
    }

    private record PendingCommand(
            FlowIpcScope scope,
            CompletableFuture<DesktopCommandResult> result) {
    }
}
