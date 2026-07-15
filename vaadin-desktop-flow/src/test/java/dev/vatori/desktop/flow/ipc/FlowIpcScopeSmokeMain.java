package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.flow.transport.DesktopFlowHttpResponseHead;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSession;
import dev.vatori.desktop.flow.transport.DesktopFlowOpaqueHttpSessionRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
        verifyOpaqueResponseBackpressureAndCancel();
        verifyOpaqueSessionRegistry();
        verifyWindowCloseScopeBeforeCancellation();
        verifyStandardBundleRejectsUnknownTransport();
        verifyNativeCapabilityCorrelation();
        System.out.println("standard Flow IPC transport smoke passed");
    }

    private static void verifyStandardBundleRejectsUnknownTransport() {
        String input = FlowIpcWire.PREFIX
                + envelope("unsupportedMessage", "unsupported", "flow-ipc-session", "main") + "\n"
                + FlowIpcWire.PREFIX
                + envelope("shutdown", "standard-shutdown", "flow-ipc-session", "main") + "\n";
        String output = captureRuntime(input, new StandardFlowIpcRuntime(
                ScopeSmokeView.class, List.of(ScopeSmokeView.class), null));
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
        FlowIpcNativeBridge bridge = new FlowIpcNativeBridge(
                new PrintStream(output, true, StandardCharsets.UTF_8), "", scope);
        AtomicReference<DesktopCommandResult> result = new AtomicReference<>();
        Thread caller = Thread.ofVirtual().start(() -> result.set(bridge.invoke(
                new DesktopCommand("clipboard.readText", Map.of()))));
        for (int attempt = 0; attempt < 100 && output.size() == 0; attempt++) {
            Thread.sleep(5);
        }
        if (!output.toString(StandardCharsets.UTF_8).contains("\"type\":\"command\"")) {
            throw new IllegalStateException("Native bridge did not emit a command envelope");
        }
        bridge.complete(scope, FlowIpcInboundEnvelope.parse("""
                {"type":"capabilityResult","id":"capability-desktop-command-1",
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

    private static String captureRuntime(String input, StandardFlowIpcRuntime runtime) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            runtime.run();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
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

    private static void verifyWindowCloseScopeBeforeCancellation() {
        FlowIpcScope owner = new FlowIpcScope("main", "flow-ipc-session", "main");
        StandardFlowIpcOutput output = new StandardFlowIpcOutput("", owner);
        FlowIpcNativeBridge nativeBridge = new FlowIpcNativeBridge("", owner);
        StandardFlowIpcSessions sessions = new StandardFlowIpcSessions(
                List.of(ScopeSmokeView.class), null, output, nativeBridge);
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
                {"version":"flow-ipc-v1","type":"%s","id":"%s","correlationId":"%s","sessionId":"%s","uiId":"%s","route":"","payload":{},"error":null,"timestamp":0}
                """.formatted(type, id, id, sessionId, uiId).trim();
    }

    @Route("")
    public static final class ScopeSmokeView extends VerticalLayout {
        public ScopeSmokeView() {
            setId("flow-ipc-scope-smoke");
            add(new Button("Scope smoke"));
        }
    }
}
