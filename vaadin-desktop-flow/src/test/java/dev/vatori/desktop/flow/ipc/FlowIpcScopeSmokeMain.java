package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.DesktopApplication;
import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponseHead;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;
import dev.vatori.desktop.flow.transport.DesktopFlowPushMessage;
import dev.vatori.desktop.ipc.SidecarWire;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class FlowIpcScopeSmokeMain {
    private FlowIpcScopeSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        verifyOpaqueRequestBodyCredits();
        verifyOpaqueRequestBodyCancelUnblocksReader();
        verifyOpaqueResponseBackpressureAndCancel();
        verifyOpaqueExchangeRegistryLifecycle();
        verifyOpaqueSessionRegistry();
        verifyPushDeliveryAcknowledgement();
        verifyWindowCloseScopeBeforeCancellation();
        verifyWindowCloseFollowsQueuedRequests();
        verifyStandardBundleRejectsUnknownTransport();
        verifyNativeCapabilityCorrelation();
        verifyMessageLoopReadsCapabilityResultWhileHandlerWaits();
        verifyProtocolVersionAndFraming();
        System.out.println("standard Flow IPC transport smoke passed");
    }

    private static void verifyProtocolVersionAndFraming() {
        SidecarInboundEnvelope.parse("""
                {"version":1,"type":"shutdown","payload":{}}
                """);
        for (String invalid : List.of(
                "{\"type\":\"shutdown\",\"payload\":{}}",
                "{\"version\":2,\"type\":\"shutdown\",\"payload\":{}}",
                "{\"version\":\"1\",\"type\":\"shutdown\",\"payload\":{}}")) {
            try {
                SidecarInboundEnvelope.parse(invalid);
                throw new IllegalStateException("Sidecar accepted an invalid protocol version");
            } catch (SidecarProtocolException expected) {
                // Expected strict numeric version guard.
            }
        }
        String invalidVersionOutput = captureRuntime(
                SidecarWire.PREFIX
                        + "{\"version\":2,\"type\":\"shutdown\",\"payload\":{}}\n");
        if (!invalidVersionOutput.contains("\"code\":\"protocolVersion\"")) {
            throw new IllegalStateException(
                    "Unknown protocol version did not produce a protocol error: "
                            + invalidVersionOutput);
        }
        String invalidFramingOutput = captureRuntime(
                "VFIPC2 {\"version\":1,\"type\":\"shutdown\",\"payload\":{}}\n");
        if (!invalidFramingOutput.contains("\"code\":\"protocolFraming\"")) {
            throw new IllegalStateException(
                    "Invalid protocol framing did not produce a protocol error: "
                            + invalidFramingOutput);
        }
    }

    private static void verifyStandardBundleRejectsUnknownTransport() {
        String input = SidecarWire.PREFIX
                + envelope("unsupportedMessage", "unsupported", "flow-ipc-session", "main") + "\n"
                + SidecarWire.PREFIX
                + envelope("shutdown", "standard-shutdown", "flow-ipc-session", "main") + "\n";
        String output = captureRuntime(input);
        if (!output.contains("\"runtime\":\"standard-vaadin-bundle-opaque-ipc\"")
                || !output.contains("\"uidlFallback\":\"none\"")
                || !output.contains("\"id\":\"unsupported\"")
                || !output.contains("\"code\":\"unsupportedTransport\"")) {
            throw new IllegalStateException(
                    "Standard bundle runtime accepted an unknown transport message:\n" + output);
        }
    }

    private static void verifyNativeCapabilityCorrelation() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("clipboard.readText", Map.of()))));
        for (int attempt = 0; attempt < 100 && output.size() == 0; attempt++) {
            Thread.sleep(5);
        }
        if (!output.toString(StandardCharsets.UTF_8).contains("\"type\":\"command\"")) {
            throw new IllegalStateException("Native bridge did not emit a command envelope");
        }
        bridge.complete(scope, SidecarInboundEnvelope.parse("""
                {"version":1,"type":"capabilityResult","id":"capability-desktop-command-1",
                 "correlationId":"desktop-command-1","sessionId":"flow-ipc-session",
                 "uiId":"main","windowId":"main",
                 "payload":{"success":true,"text":"from-native"}}
                """));
        caller.join(1000);
        DesktopCommandResult commandResult = result.get();
        bridge.close();
        if (caller.isAlive() || commandResult == null || !commandResult.success()
                || !"from-native".equals(commandResult.payload().get("text"))) {
            throw new IllegalStateException(
                    "Native bridge did not correlate the real capability result");
        }
    }

    private static void verifyMessageLoopReadsCapabilityResultWhileHandlerWaits()
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput sidecarOutput = new SidecarOutput(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        SidecarNativeBridge bridge = new SidecarNativeBridge(sidecarOutput, scope);
        PipedWriter inputWriter = new PipedWriter();
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new PipedReader(inputWriter)),
                sidecarOutput,
                bridge,
                scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        messageLoop.register("invokeCapability", (ignoredScope, envelope, id, correlationId) ->
                result.set(bridge.invoke(new DesktopCommand(
                        "clipboard.readText", Map.of()))));
        Thread loop = Thread.ofVirtual().start(messageLoop::run);
        inputWriter.write(SidecarWire.PREFIX
                + envelope("invokeCapability", "invoke", "flow-ipc-session", "main")
                + "\n");
        inputWriter.flush();
        for (int attempt = 0; attempt < 100
                && !output.toString(StandardCharsets.UTF_8)
                        .contains("\"type\":\"command\""); attempt++) {
            Thread.sleep(5);
        }
        if (!output.toString(StandardCharsets.UTF_8).contains("\"type\":\"command\"")) {
            throw new IllegalStateException(
                    "Sidecar handler did not emit a native capability command");
        }
        inputWriter.write(SidecarWire.PREFIX + """
                {"version":1,"type":"capabilityResult","id":"capability-desktop-command-1",
                 "correlationId":"desktop-command-1","sessionId":"flow-ipc-session",
                 "uiId":"main","windowId":"main",
                 "payload":{"success":true,"text":"from-message-loop"}}
                """.replace("\n", "") + "\n");
        inputWriter.write(SidecarWire.PREFIX
                + envelope("shutdown", "shutdown", "flow-ipc-session", "main")
                + "\n");
        inputWriter.flush();
        inputWriter.close();
        loop.join(1000);
        DesktopCommandResult commandResult = result.get();
        if (loop.isAlive() || commandResult == null || !commandResult.success()
                || !"from-message-loop".equals(commandResult.payload().get("text"))) {
            throw new IllegalStateException(
                    "Sidecar reader could not correlate a capability result while its handler waited");
        }
    }

    private static String captureRuntime(String input) {
        InputStream originalIn = System.in;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
            SidecarOutput output = new SidecarOutput(
                    new PrintStream(captured, true, StandardCharsets.UTF_8), "", scope);
            SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
            StandardFlowIpcRuntime runtime = new StandardFlowIpcRuntime(
                    "",
                    List.of(ScopeSmokeView.class),
                    null,
                    DesktopApplication.builder("Scope Smoke").build(),
                    output,
                    nativeBridge,
                    scope);
            runtime.run();
            nativeBridge.close();
        } finally {
            System.setIn(originalIn);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static void verifyOpaqueRequestBodyCredits() throws Exception {
        AtomicInteger credits = new AtomicInteger();
        OpaqueHttpRequestBodyInputStream body =
                new OpaqueHttpRequestBodyInputStream(6, credits::addAndGet);
        body.accept(0, "abc".getBytes(StandardCharsets.UTF_8));
        body.accept(1, "def".getBytes(StandardCharsets.UTF_8));
        body.finish(2, 6);
        if (!Arrays.equals(body.readAllBytes(), "abcdef".getBytes(StandardCharsets.UTF_8))
                || credits.get() != 2) {
            throw new IllegalStateException("Opaque request stream did not replenish consumed chunk credits");
        }

        OpaqueHttpRequestBodyInputStream invalid =
                new OpaqueHttpRequestBodyInputStream(-1, ignored -> {
                });
        try {
            invalid.accept(1, new byte[]{1});
            throw new IllegalStateException("Opaque request stream accepted an out-of-order chunk");
        } catch (IllegalArgumentException expected) {
            // Expected sequence guard.
        }
    }

    private static void verifyOpaqueRequestBodyCancelUnblocksReader()
            throws Exception {
        OpaqueHttpRequestBodyInputStream body =
                new OpaqueHttpRequestBodyInputStream(-1, ignored -> {
                });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reader = Thread.ofVirtual().start(() -> {
            try {
                body.readAllBytes();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        Thread.sleep(20);
        body.cancel();
        reader.join(1000);
        if (reader.isAlive()
                || !(failure.get() instanceof java.io.IOException)) {
            throw new IllegalStateException(
                    "Opaque request cancellation did not unblock its reader",
                    failure.get());
        }
    }

    private static void verifyOpaqueResponseBackpressureAndCancel() throws Exception {
        List<byte[]> chunks = new ArrayList<>();
        AtomicInteger endedChunks = new AtomicInteger(-1);
        OpaqueHttpResponseOutputStream response = new OpaqueHttpResponseOutputStream(
                new OpaqueHttpResponseOutputStream.ResponseSender() {
                    @Override
                    public void start(DesktopFlowHttpResponseHead head) {
                    }

                    @Override
                    public void chunk(int sequence, byte[] bytes) {
                        if (sequence != chunks.size()) {
                            throw new IllegalStateException("Opaque response chunk sequence is not ordered");
                        }
                        chunks.add(bytes);
                    }

                    @Override
                    public void end(int chunkCount, long byteCount) {
                        endedChunks.set(chunkCount);
                    }
                });
        response.start(new DesktopFlowHttpResponseHead(200, "OK", Map.of()));
        byte[] bytes = new byte[OpaqueHttpResponseOutputStream.CHUNK_SIZE * 2 + 7];
        Arrays.fill(bytes, (byte) 0x41);
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = Thread.ofVirtual().start(() -> {
            try {
                response.write(bytes);
                response.finish();
                completed.set(true);
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        Thread.sleep(20);
        if (completed.get()) {
            throw new IllegalStateException("Opaque response ignored its credit window");
        }
        response.grantCredits(1);
        Thread.sleep(20);
        if (completed.get() || chunks.size() != 1) {
            throw new IllegalStateException("Opaque response did not stop after consuming one credit");
        }
        response.grantCredits(2);
        writer.join(1000);
        if (!completed.get() || failure.get() != null || chunks.size() != 3
                || endedChunks.get() != 3) {
            throw new IllegalStateException("Opaque response did not resume after receiving credit", failure.get());
        }

        OpaqueHttpResponseOutputStream cancelled = new OpaqueHttpResponseOutputStream(
                new NoopResponseSender());
        cancelled.start(new DesktopFlowHttpResponseHead(200, "OK", Map.of()));
        AtomicReference<Throwable> cancelFailure = new AtomicReference<>();
        Thread cancelledWriter = Thread.ofVirtual().start(() -> {
            try {
                cancelled.write(new byte[OpaqueHttpResponseOutputStream.CHUNK_SIZE]);
            } catch (Throwable error) {
                cancelFailure.set(error);
            }
        });
        Thread.sleep(20);
        cancelled.cancel();
        cancelledWriter.join(1000);
        if (cancelledWriter.isAlive() || cancelFailure.get() == null) {
            throw new IllegalStateException("Opaque response cancellation did not unblock its writer");
        }
    }

    private static void verifyOpaqueExchangeRegistryLifecycle()
            throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-registry-session", "main");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(captured, true, StandardCharsets.UTF_8),
                "", scope);
        DesktopFlowOpaqueHttpSession session =
                DesktopFlowOpaqueHttpSession.create(
                        scope.sessionId(), "registry-smoke", "",
                        ScopeSmokeView.class);
        try (StandardFlowIpcExchanges exchanges =
                     new StandardFlowIpcExchanges(output)) {
            String requestId = "reusable-request";
            exchanges.open(scope, session,
                    streamEnvelope("opaqueHttpStreamOpen", "first-open", """
                            {"requestId":"reusable-request","method":"GET",
                             "url":"https://app.vatori.invalid/","headers":{},
                             "contentLength":0}
                            """), "first-open", "first-open");
            exchanges.finishRequest(scope,
                    streamEnvelope("opaqueHttpStreamEnd", "first-end", """
                            {"requestId":"reusable-request","chunkCount":0,
                             "bodyBytes":0}
                            """));
            exchanges.grantResponseCredits(scope,
                    streamEnvelope("opaqueHttpStreamResponseCredit",
                            "first-response-credit", """
                                    {"requestId":"reusable-request",
                                     "credit":16}
                                    """));

            boolean reopenedAfterCompletion = false;
            for (int attempt = 0; attempt < 200; attempt++) {
                if (!captured.toString(StandardCharsets.UTF_8).contains(
                        "\"type\":\"opaqueHttpStreamResponseEnd\"")) {
                    Thread.sleep(5);
                    continue;
                }
                try {
                    exchanges.open(scope, session,
                            streamEnvelope("opaqueHttpStreamOpen",
                                    "second-open", """
                                            {"requestId":"reusable-request",
                                             "method":"GET",
                                             "url":"https://app.vatori.invalid/",
                                             "headers":{},"contentLength":0}
                                            """),
                            "second-open", "second-open");
                    reopenedAfterCompletion = true;
                    break;
                } catch (IllegalArgumentException error) {
                    if (!error.getMessage().contains(
                            "Duplicate opaque HTTP request id")) {
                        throw error;
                    }
                    Thread.sleep(5);
                }
            }
            if (!reopenedAfterCompletion) {
                throw new IllegalStateException(
                        "Completed opaque exchange was not reclaimed");
            }

            exchanges.cancel(scope,
                    streamEnvelope("opaqueHttpStreamCancel", "second-cancel",
                            "{\"requestId\":\"reusable-request\"}"));
            try {
                exchanges.acceptChunk(scope,
                        streamEnvelope("opaqueHttpStreamChunk", "late-chunk",
                                """
                                        {"requestId":"reusable-request",
                                         "chunkSequence":0,"bodyBase64":"YQ==",
                                         "bodyBytes":1}
                                        """));
                throw new IllegalStateException(
                        "Cancelled opaque exchange accepted a late request frame");
            } catch (IllegalArgumentException expected) {
                if (!expected.getMessage().contains(
                        "Unknown opaque HTTP request id " + requestId)) {
                    throw expected;
                }
            }

            exchanges.open(scope, session,
                    streamEnvelope("opaqueHttpStreamOpen", "third-open", """
                            {"requestId":"reusable-request","method":"GET",
                             "url":"https://app.vatori.invalid/","headers":{},
                             "contentLength":0}
                            """), "third-open", "third-open");
            exchanges.cancel(scope,
                    streamEnvelope("opaqueHttpStreamCancel", "third-cancel",
                            "{\"requestId\":\"reusable-request\"}"));
        } finally {
            session.close();
        }
    }

    private static void verifyOpaqueSessionRegistry() {
        DesktopFlowOpaqueHttpSessionRegistry registry =
                DesktopFlowOpaqueHttpSessionRegistry.create(
                        "scope-smoke", "", ScopeSmokeView.class, ignored -> {
                        });
        try {
            DesktopFlowOpaqueHttpSession main = registry.session("flow-ipc-session");
            DesktopFlowOpaqueHttpSession secondary =
                    registry.session("flow-ipc-session-secondary");
            if (main == secondary
                    || !main.sessionId().equals("flow-ipc-session")
                    || !secondary.sessionId().equals("flow-ipc-session-secondary")
                    || registry.size() != 2) {
                throw new IllegalStateException("Opaque session registry did not isolate host scopes");
            }
            if (!registry.closeSession("flow-ipc-session-secondary")
                    || registry.size() != 1
                    || registry.session("flow-ipc-session") != main) {
                throw new IllegalStateException(
                        "Closing one opaque session affected another host scope");
            }
        } finally {
            registry.close();
        }
        registry.close();
        boolean rejected = false;
        try {
            registry.session("after-close");
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        if (!rejected) {
            throw new IllegalStateException("Closed opaque session registry accepted a session");
        }
    }

    private static void verifyPushDeliveryAcknowledgement() {
        FlowIpcScope scope = new FlowIpcScope(
                "main", "flow-ipc-session", "main");
        List<StandardFlowIpcPushDelivery.Delivery> deliveries =
                new ArrayList<>();
        StandardFlowIpcPushDelivery tracker =
                new StandardFlowIpcPushDelivery(deliveries::add, false);
        try {
            tracker.registerScope(scope);
            tracker.connect(scope, "opaque-push-1", 4);
            tracker.send(scope, new DesktopFlowPushMessage(
                    scope.sessionId(),
                    "opaque-push-1",
                    7,
                    5,
                    "{\"syncId\":5}",
                    false));
            tracker.retryUnacknowledged();
            if (deliveries.size() != 2
                    || deliveries.get(0).pushSequence() != 1
                    || deliveries.get(1).pushSequence() != 1
                    || deliveries.get(0).deliveryAttempt() != 1
                    || deliveries.get(1).deliveryAttempt() != 2
                    || deliveries.get(0).flowServerSyncId() != 5
                    || !deliveries.get(0).uidl()
                    .equals(deliveries.get(1).uidl())) {
                throw new IllegalStateException(
                        "Unacknowledged opaque push was not retried identically");
            }
            if (tracker.acknowledge(scope, "opaque-push-1", 1, 4)) {
                throw new IllegalStateException(
                        "Opaque push accepted an acknowledgement before Flow saw it");
            }
            tracker.retryUnacknowledged();
            if (deliveries.size() != 3
                    || deliveries.get(2).deliveryAttempt() != 3) {
                throw new IllegalStateException(
                        "Unconfirmed opaque push stopped retrying");
            }
            if (!tracker.acknowledge(scope, "opaque-push-1", 1, 5)) {
                throw new IllegalStateException(
                        "Opaque push acknowledgement did not clear delivery");
            }
            tracker.retryUnacknowledged();
            if (deliveries.size() != 3) {
                throw new IllegalStateException(
                        "Acknowledged opaque push was delivered again");
            }

            tracker.send(scope, new DesktopFlowPushMessage(
                    scope.sessionId(),
                    "opaque-push-1",
                    7,
                    6,
                    "{\"syncId\":6}",
                    false));
            tracker.send(scope, new DesktopFlowPushMessage(
                    scope.sessionId(),
                    "opaque-push-1",
                    7,
                    7,
                    "{\"syncId\":7,\"resynchronize\":true}",
                    true));
            tracker.retryUnacknowledged();
            StandardFlowIpcPushDelivery.Delivery retry =
                    deliveries.get(deliveries.size() - 1);
            if (!retry.resync()
                    || retry.flowServerSyncId() != 7
                    || retry.deliveryAttempt() != 2) {
                throw new IllegalStateException(
                        "Resync did not supersede older pending opaque pushes");
            }
            tracker.disconnect(scope, "opaque-push-1");
            int disconnectedDeliveryCount = deliveries.size();
            tracker.retryUnacknowledged();
            if (deliveries.size() != disconnectedDeliveryCount) {
                throw new IllegalStateException(
                        "Disconnected opaque push continued retrying");
            }
            tracker.connect(scope, "opaque-push-1", 7);
            tracker.retryUnacknowledged();
            if (deliveries.size() != disconnectedDeliveryCount) {
                throw new IllegalStateException(
                        "Reconnect last-seen metadata did not confirm pending push");
            }
        } finally {
            tracker.close();
        }
    }

    private static void verifyWindowCloseScopeBeforeCancellation() {
        FlowIpcScope owner = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput output = new SidecarOutput("", owner);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, owner);
        StandardFlowIpcSessions sessions = new StandardFlowIpcSessions(
                List.of(ScopeSmokeView.class), null, output, nativeBridge,
                true, null);
        AtomicBoolean cancellationAttempted = new AtomicBoolean();
        try {
            sessions.session(owner);
            try {
                sessions.closeWindow(
                        new FlowIpcScope("main", "forged-session", "main"),
                        () -> cancellationAttempted.set(true));
                throw new IllegalStateException(
                        "Window close accepted a mismatched Flow IPC scope");
            } catch (SecurityException expected) {
                // The session owner must be checked before exchange cancellation.
            }
            if (cancellationAttempted.get()) {
                throw new IllegalStateException(
                        "Window close cancelled exchanges before validating its scope");
            }
        } finally {
            sessions.close();
            nativeBridge.close();
        }
    }

    private static void verifyWindowCloseFollowsQueuedRequests() {
        String input = SidecarWire.PREFIX
                + envelope("queuedRequest", "queued", "flow-ipc-session", "main") + "\n"
                + SidecarWire.PREFIX
                + envelope("windowClosed", "closed", "flow-ipc-session", "main") + "\n";
        FlowIpcScope scope = new FlowIpcScope("main", "flow-ipc-session", "main");
        SidecarOutput output = new SidecarOutput(
                new PrintStream(new ByteArrayOutputStream()), "", scope);
        SidecarNativeBridge nativeBridge = new SidecarNativeBridge(output, scope);
        SidecarMessageLoop messageLoop = new SidecarMessageLoop(
                new BufferedReader(new java.io.StringReader(input)),
                output,
                nativeBridge,
                scope);
        AtomicInteger stage = new AtomicInteger();
        messageLoop.register("queuedRequest", (ignoredScope, envelope, id, correlationId) -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Queued request was interrupted", error);
            }
            stage.compareAndSet(0, 1);
        });
        messageLoop.onWindowClosed(ignored -> {
            if (!stage.compareAndSet(1, 2)) {
                throw new IllegalStateException(
                        "Window close overtook an earlier sidecar request");
            }
            messageLoop.requestShutdown();
        });
        messageLoop.run();
        if (stage.get() != 2) {
            throw new IllegalStateException(
                    "Window close did not run after the queued sidecar request");
        }
    }

    private static final class NoopResponseSender
            implements OpaqueHttpResponseOutputStream.ResponseSender {
        @Override
        public void start(DesktopFlowHttpResponseHead head) {
        }

        @Override
        public void chunk(int sequence, byte[] bytes) {
        }

        @Override
        public void end(int chunkCount, long byteCount) {
        }
    }

    private static String envelope(String type, String id, String sessionId, String uiId) {
        return """
                {"version":1,"type":"%s","id":"%s","correlationId":"%s","sessionId":"%s","uiId":"%s","route":"","payload":{},"error":null,"timestamp":0}
                """.formatted(type, id, id, sessionId, uiId).trim();
    }

    private static SidecarInboundEnvelope streamEnvelope(
            String type, String id, String payload) {
        return SidecarInboundEnvelope.parse("""
                {"version":1,"type":"%s","id":"%s",
                 "correlationId":"%s","sessionId":"flow-ipc-registry-session",
                 "uiId":"main","windowId":"main","route":"",
                 "payload":%s,"error":null,"timestamp":0}
                """.formatted(type, id, id, payload).trim());
    }

    @Route("")
    public static final class ScopeSmokeView extends VerticalLayout {
        public ScopeSmokeView() {
            setId("flow-ipc-scope-smoke");
            add(new Button("Scope smoke"));
        }
    }
}
