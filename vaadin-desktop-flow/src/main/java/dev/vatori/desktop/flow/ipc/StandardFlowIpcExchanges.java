package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponseHead;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class StandardFlowIpcExchanges implements AutoCloseable {
    private final SidecarOutput output;
    private final ConcurrentMap<ExchangeKey, OpaqueHttpExchange> exchanges =
            new ConcurrentHashMap<>();

    StandardFlowIpcExchanges(SidecarOutput output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    void open(
            FlowIpcScope scope,
            DesktopFlowOpaqueHttpSession session,
            SidecarInboundEnvelope envelope,
            String id,
            String correlationId) {
        String requestId = requiredRequestId(envelope);
        long contentLength = envelope.longValue("contentLength", -1);
        if (contentLength < -1) {
            throw new IllegalArgumentException(
                    "Opaque HTTP content length must be -1 or greater");
        }
        OpaqueHttpExchange exchange = new OpaqueHttpExchange(
                scope,
                session,
                requestId,
                SidecarOutput.correlationIdOr(id, correlationId),
                Objects.requireNonNullElse(envelope.string("method"), "GET"),
                requiredUrl(envelope),
                envelope.stringMap("headers"),
                contentLength);
        ExchangeKey key = new ExchangeKey(scope.windowId(), requestId);
        if (exchanges.putIfAbsent(key, exchange) != null) {
            throw new IllegalArgumentException("Duplicate opaque HTTP request id " + requestId);
        }
        exchange.start();
        sendRequestCredit(scope, requestId, exchange.correlationId,
                OpaqueHttpRequestBodyInputStream.INITIAL_CREDITS);
    }

    void acceptChunk(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        OpaqueHttpExchange exchange = requiredExchange(scope, envelope);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(
                    Objects.requireNonNullElse(envelope.string("bodyBase64"), ""));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Opaque HTTP request chunk is not valid base64", error);
        }
        if (bytes.length > OpaqueHttpResponseOutputStream.CHUNK_SIZE) {
            throw new IllegalArgumentException(
                    "Opaque HTTP request chunk exceeds the transport limit");
        }
        exchange.requestBody.accept(envelope.integer("chunkSequence", -1), bytes);
    }

    void finishRequest(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        OpaqueHttpExchange exchange = requiredExchange(scope, envelope);
        exchange.requestBody.finish(
                envelope.integer("chunkCount", -1),
                envelope.longValue("bodyBytes", -1));
    }

    void grantResponseCredits(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        String requestId = requiredRequestId(envelope);
        OpaqueHttpExchange exchange = exchanges.get(
                new ExchangeKey(scope.windowId(), requestId));
        if (exchange != null) {
            exchange.requireScope(scope);
            exchange.responseBody.grantCredits(envelope.integer("credit", 0));
        }
    }

    void cancel(FlowIpcScope scope, SidecarInboundEnvelope envelope) {
        String requestId = requiredRequestId(envelope);
        ExchangeKey key = new ExchangeKey(scope.windowId(), requestId);
        OpaqueHttpExchange exchange = exchanges.get(key);
        if (exchange != null) {
            exchange.requireScope(scope);
            if (exchanges.remove(key, exchange)) {
                exchange.cancel();
            }
        }
    }

    void cancelWindow(FlowIpcScope scope) {
        exchanges.forEach((key, exchange) -> {
            if (key.windowId.equals(scope.windowId())
                    && exchanges.remove(key, exchange)) {
                exchange.cancel();
            }
        });
    }

    @Override
    public void close() {
        exchanges.forEach((key, exchange) -> {
            if (exchanges.remove(key, exchange)) {
                exchange.cancel();
            }
        });
    }

    static String requiredUrl(SidecarInboundEnvelope envelope) {
        String url = envelope.string("url");
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Opaque HTTP request URL is required");
        }
        return url;
    }

    private OpaqueHttpExchange requiredExchange(
            FlowIpcScope scope,
            SidecarInboundEnvelope envelope) {
        String requestId = requiredRequestId(envelope);
        OpaqueHttpExchange exchange = exchanges.get(
                new ExchangeKey(scope.windowId(), requestId));
        if (exchange == null) {
            throw new IllegalArgumentException(
                    "Unknown opaque HTTP request id " + requestId);
        }
        exchange.requireScope(scope);
        return exchange;
    }

    private static String requiredRequestId(SidecarInboundEnvelope envelope) {
        String requestId = Objects.requireNonNullElse(
                envelope.string("requestId"), envelope.id());
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Opaque HTTP request id is required");
        }
        return requestId;
    }

    private void sendRequestCredit(
            FlowIpcScope scope,
            String requestId,
            String correlationId,
            int credit) {
        output.send(scope, "opaqueHttpStreamRequestCredit",
                output.nextId("http-request-credit"), correlationId,
                Map.of(
                        "requestId", requestId,
                        "credit", credit,
                        "transport", "opaque-http-stream-ipc"));
    }

    private void sendStreamError(
            FlowIpcScope scope,
            String requestId,
            String correlationId,
            Throwable error) {
        output.send(scope, "opaqueHttpStreamError",
                output.nextId("http-stream-error"), correlationId,
                Map.of(
                        "requestId", requestId,
                        "message", Objects.requireNonNullElse(
                                error.getMessage(), error.toString()),
                        "transport", "opaque-http-stream-ipc"));
    }

    private final class OpaqueHttpExchange {
        private final FlowIpcScope scope;
        private final DesktopFlowOpaqueHttpSession session;
        private final String requestId;
        private final String correlationId;
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final long contentLength;
        private final OpaqueHttpRequestBodyInputStream requestBody;
        private final OpaqueHttpResponseOutputStream responseBody;
        private volatile Thread worker;
        private volatile boolean cancelled;

        private OpaqueHttpExchange(
                FlowIpcScope scope,
                DesktopFlowOpaqueHttpSession session,
                String requestId,
                String correlationId,
                String method,
                String url,
                Map<String, String> headers,
                long contentLength) {
            this.scope = scope;
            this.session = session;
            this.requestId = requestId;
            this.correlationId = correlationId;
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.contentLength = contentLength;
            this.requestBody = new OpaqueHttpRequestBodyInputStream(
                    contentLength,
                    credit -> sendRequestCredit(scope, requestId, correlationId, credit));
            this.responseBody = new OpaqueHttpResponseOutputStream(
                    new OpaqueHttpResponseOutputStream.ResponseSender() {
                        @Override
                        public void start(DesktopFlowHttpResponseHead head) {
                            Map<String, Object> payload = new LinkedHashMap<>();
                            payload.put("requestId", requestId);
                            payload.put("status", head.statusCode());
                            payload.put("statusText", head.statusText());
                            payload.put("headers", head.headers());
                            payload.put("transport", "opaque-http-stream-ipc");
                            output.send(scope, "opaqueHttpStreamResponse",
                                    output.nextId("http-stream-response"), correlationId, payload);
                        }

                        @Override
                        public void chunk(int chunkSequence, byte[] bytes) {
                            output.send(scope, "opaqueHttpStreamResponseChunk",
                                    output.nextId("http-response-chunk"), correlationId,
                                    Map.of(
                                            "requestId", requestId,
                                            "chunkSequence", chunkSequence,
                                            "bodyBase64", Base64.getEncoder().encodeToString(bytes),
                                            "bodyBytes", bytes.length,
                                            "transport", "opaque-http-stream-ipc"));
                        }

                        @Override
                        public void end(int chunkCount, long byteCount) {
                            output.send(scope, "opaqueHttpStreamResponseEnd",
                                    output.nextId("http-response-end"), correlationId,
                                    Map.of(
                                            "requestId", requestId,
                                            "chunkCount", chunkCount,
                                            "bodyBytes", byteCount,
                                            "transport", "opaque-http-stream-ipc"));
                        }
                    });
        }

        private void start() {
            worker = Thread.ofVirtual()
                    .name("vatori-opaque-http-" + requestId)
                    .start(this::run);
        }

        private void run() {
            try {
                session.handleStreaming(
                        method,
                        url,
                        headers,
                        requestBody,
                        contentLength,
                        responseBody::start,
                        responseBody);
                responseBody.finish();
            } catch (IOException | RuntimeException error) {
                handleFailure(error);
            } finally {
                exchanges.remove(new ExchangeKey(scope.windowId(), requestId), this);
                try {
                    requestBody.close();
                } catch (RuntimeException ignored) {
                    // The exchange is already terminal.
                }
            }
        }

        private void handleFailure(Throwable error) {
            if (cancelled || responseBody.isCancelled()) {
                return;
            }
            if (responseBody.hasStarted()) {
                sendStreamError(scope, requestId, correlationId, error);
                return;
            }
            try {
                byte[] message = Objects.requireNonNullElse(
                                error.getMessage(), error.toString())
                        .getBytes(StandardCharsets.UTF_8);
                responseBody.start(new DesktopFlowHttpResponseHead(
                        500,
                        "Internal Server Error",
                        Map.of(
                                "Content-Type", List.of("text/plain; charset=utf-8"),
                                "Content-Length", List.of(String.valueOf(message.length)))));
                responseBody.write(message);
                responseBody.finish();
            } catch (IOException | RuntimeException responseError) {
                sendStreamError(scope, requestId, correlationId, responseError);
            }
        }

        private void cancel() {
            cancelled = true;
            requestBody.cancel();
            responseBody.cancel();
            Thread activeWorker = worker;
            if (activeWorker != null) {
                activeWorker.interrupt();
            }
        }

        private void requireScope(FlowIpcScope candidate) {
            if (!scope.equals(candidate)) {
                throw new SecurityException("Opaque HTTP exchange scope mismatch");
            }
        }
    }

    private record ExchangeKey(String windowId, String requestId) {
    }
}
