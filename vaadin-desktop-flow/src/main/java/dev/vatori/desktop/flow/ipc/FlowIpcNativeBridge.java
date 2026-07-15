package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;
import dev.vatori.desktop.ipc.FlowIpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;
import com.vaadin.flow.server.VaadinSession;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

final class FlowIpcNativeBridge implements NativeBridge, AutoCloseable {
    private final PrintStream out;
    private final String route;
    private final FlowIpcScope defaultScope;
    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentMap<String, PendingCommand> pending = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FlowIpcScope> scopesBySession = new ConcurrentHashMap<>();
    private volatile boolean closed;

    FlowIpcNativeBridge(String route, FlowIpcScope defaultScope) {
        this(System.out, route, defaultScope);
    }

    FlowIpcNativeBridge(PrintStream out, String route, FlowIpcScope defaultScope) {
        this.out = Objects.requireNonNull(out, "out");
        this.route = route == null ? "" : route;
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
    }

    @Override
    public DesktopCommandResult invoke(DesktopCommand command) {
        if (closed) {
            return DesktopCommandResult.error("Flow IPC native bridge is closed");
        }
        String id = "desktop-command-" + sequence.incrementAndGet();
        FlowIpcScope scope = currentScope();
        CompletableFuture<DesktopCommandResult> result = new CompletableFuture<>();
        PendingCommand commandState = new PendingCommand(scope, result);
        pending.put(id, commandState);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", command.name());
        payload.put("args", command.payload());
        FlowIpcEnvelope envelope = FlowIpcEnvelope.message(
                "command",
                id,
                id,
                scope.sessionId(),
                scope.uiId(),
                scope.windowId(),
                route,
                payload);
        out.println(FlowIpcWire.PREFIX + IpcJson.stringify(envelope));
        out.flush();
        try {
            return result.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return DesktopCommandResult.error("Interrupted while waiting for native capability result");
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
        boolean success = envelope.booleanValue("success", false);
        if (success) {
            command.result.complete(DesktopCommandResult.ok(payload));
        } else {
            String message = envelope.errorString("message");
            if (message == null || message.isBlank()) {
                Object payloadError = payload.get("error");
                message = payloadError == null
                        ? "Native capability command failed"
                        : String.valueOf(payloadError);
            }
            command.result.complete(DesktopCommandResult.error(message));
        }
    }

    @Override
    public void close() {
        closed = true;
        pending.forEach((id, command) -> command.result.complete(
                DesktopCommandResult.error("Flow IPC runtime closed")));
        pending.clear();
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
