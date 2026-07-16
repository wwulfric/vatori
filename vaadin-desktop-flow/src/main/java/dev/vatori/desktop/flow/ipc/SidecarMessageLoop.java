package dev.vatori.desktop.flow.ipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Owns the only stdin reader for a Java sidecar process. */
final class SidecarMessageLoop {
    private final BufferedReader reader;
    private final SidecarOutput output;
    private final SidecarNativeBridge nativeBridge;
    private final FlowIpcScope defaultScope;
    private final ExecutorService handlerExecutor;
    private final Map<String, Handler> handlers = new LinkedHashMap<>();
    private Consumer<FlowIpcScope> windowClosedHandler = ignored -> {
    };
    private volatile boolean shutdownRequested;

    SidecarMessageLoop(
            SidecarOutput output,
            SidecarNativeBridge nativeBridge,
            FlowIpcScope defaultScope) {
        this(new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                output, nativeBridge, defaultScope);
    }

    SidecarMessageLoop(
            BufferedReader reader,
            SidecarOutput output,
            SidecarNativeBridge nativeBridge,
            FlowIpcScope defaultScope) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.output = Objects.requireNonNull(output, "output");
        this.nativeBridge = Objects.requireNonNull(nativeBridge, "nativeBridge");
        this.defaultScope = Objects.requireNonNull(defaultScope, "defaultScope");
        this.handlerExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "vaadin-desktop-sidecar-handler");
            thread.setDaemon(true);
            return thread;
        });
    }

    void register(String type, Handler handler) {
        if (handlers.putIfAbsent(type, handler) != null) {
            throw new IllegalArgumentException("Duplicate sidecar handler for " + type);
        }
    }

    void onWindowClosed(Consumer<FlowIpcScope> handler) {
        windowClosedHandler = Objects.requireNonNull(handler, "handler");
    }

    void requestShutdown() {
        shutdownRequested = true;
        try {
            reader.close();
        } catch (IOException ignored) {
            // Closing the reader is only used to unblock the terminating loop.
        }
    }

    void run() {
        try {
            String line;
            while (!shutdownRequested && (line = reader.readLine()) != null) {
                if (line.startsWith(FlowIpcWire.PREFIX)) {
                    handle(line.substring(FlowIpcWire.PREFIX.length()));
                }
            }
        } catch (IOException error) {
            if (!shutdownRequested) {
                output.sendError(defaultScope, "io",
                        "Unable to read sidecar stdin: " + error.getMessage(), null, null);
            }
        } finally {
            nativeBridge.close();
            handlerExecutor.shutdown();
            try {
                if (!handlerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    handlerExecutor.shutdownNow();
                }
            } catch (InterruptedException error) {
                handlerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handle(String json) {
        String id = null;
        String correlationId = null;
        FlowIpcScope scope = defaultScope;
        try {
            FlowIpcInboundEnvelope envelope = FlowIpcInboundEnvelope.parse(json);
            String type = Objects.requireNonNullElse(envelope.type(), "");
            id = envelope.id();
            correlationId = envelope.correlationId();
            scope = FlowIpcScope.from(
                    envelope, defaultScope.sessionId(), defaultScope.uiId());
            switch (type) {
                case "capabilityResult" -> nativeBridge.complete(scope, envelope);
                case "windowClosed" -> {
                    FlowIpcScope handlerScope = scope;
                    String handlerId = id;
                    String handlerCorrelationId = correlationId;
                    handlerExecutor.execute(() -> invokeHandler(
                            (ignoredScope, ignoredEnvelope, ignoredId,
                                    ignoredCorrelationId) ->
                                    windowClosedHandler.accept(handlerScope),
                            handlerScope,
                            envelope,
                            handlerId,
                            handlerCorrelationId));
                }
                case "shutdown" -> requestShutdown();
                default -> {
                    Handler handler = handlers.get(type);
                    if (handler == null) {
                        output.sendError(scope, "unsupportedTransport",
                                "Unsupported sidecar message: " + type,
                                id, correlationId);
                    } else {
                        FlowIpcScope handlerScope = scope;
                        String handlerId = id;
                        String handlerCorrelationId = correlationId;
                        handlerExecutor.execute(() -> invokeHandler(
                                handler,
                                handlerScope,
                                envelope,
                                handlerId,
                                handlerCorrelationId));
                    }
                }
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

    private void invokeHandler(
            Handler handler,
            FlowIpcScope scope,
            FlowIpcInboundEnvelope envelope,
            String id,
            String correlationId) {
        try {
            handler.handle(scope, envelope, id, correlationId);
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

    @FunctionalInterface
    interface Handler {
        void handle(
                FlowIpcScope scope,
                FlowIpcInboundEnvelope envelope,
                String id,
                String correlationId);
    }
}
