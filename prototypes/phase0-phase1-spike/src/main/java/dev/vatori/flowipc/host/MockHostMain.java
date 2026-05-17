package dev.vatori.flowipc.host;

import dev.vatori.flowipc.ipc.IpcCodec;
import dev.vatori.flowipc.ipc.IpcEnvelope;
import dev.vatori.flowipc.runtime.MockFlowRuntime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class MockHostMain {
    private MockHostMain() {
    }

    public static void main(String[] args) throws Exception {
        Process sidecar = startSidecar();
        Thread stderr = copySidecarStderr(sidecar);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sidecar.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(sidecar.getInputStream(), StandardCharsets.UTF_8))) {

            MockWebView webView = new MockWebView();

            IpcEnvelope bootstrap = exchange(writer, reader, new IpcEnvelope(
                    "bootstrap", "msg-1", "trace-1", "session-demo", null,
                    Map.of("route", "/counter", "engine", "mock-host"), null));
            requireType(bootstrap, "bootstrapResult");
            webView.apply(bootstrap.payload());
            String sessionId = bootstrap.sessionId();
            String uiId = bootstrap.uiId();

            IpcEnvelope clickOne = exchange(writer, reader, event("msg-2", "trace-2", sessionId, uiId,
                    "counter.increment", "click", Map.of()));
            requireType(clickOne, "patch");
            webView.apply(clickOne.payload());

            IpcEnvelope clickTwo = exchange(writer, reader, event("msg-3", "trace-3", sessionId, uiId,
                    "counter.increment", "click", Map.of()));
            requireType(clickTwo, "patch");
            webView.apply(clickTwo.payload());

            IpcEnvelope rows = exchange(writer, reader, event("msg-4", "trace-4", sessionId, uiId,
                    "orders.grid", "requestRows", Map.of("offset", 0, "limit", 3)));
            requireType(rows, "patch");
            webView.apply(rows.payload());

            IpcEnvelope resync = exchange(writer, reader, new IpcEnvelope(
                    "resync", "msg-5", "trace-5", sessionId, uiId,
                    Map.of("reason", "mock-webview-reload"), null));
            requireType(resync, "patch");
            webView.apply(resync.payload());

            IpcEnvelope shutdown = exchange(writer, reader, new IpcEnvelope(
                    "shutdown", "msg-6", "trace-6", sessionId, uiId, Map.of(), null));
            requireType(shutdown, "ack");

            int exit = sidecar.waitFor();
            stderr.join();
            if (exit != 0) {
                throw new IllegalStateException("Sidecar exited with code " + exit);
            }
            webView.assertState();
            System.out.println("ASSERT OK: stdio IPC completed host -> sidecar -> mock WebView without HTTP.");
        }
    }

    private static Process startSidecar() throws IOException {
        String java = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");
        return new ProcessBuilder(java, "-cp", classpath, "dev.vatori.flowipc.sidecar.SidecarMain")
                .redirectErrorStream(false)
                .start();
    }

    private static Thread copySidecarStderr(Process sidecar) {
        Thread thread = new Thread(() -> {
            try (BufferedReader stderr = new BufferedReader(new InputStreamReader(sidecar.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = stderr.readLine()) != null) {
                    System.out.println("[sidecar-trace] " + line);
                }
            } catch (IOException error) {
                System.out.println("[sidecar-trace-error] " + error.getMessage());
            }
        }, "sidecar-stderr-copy");
        thread.start();
        return thread;
    }

    private static IpcEnvelope event(String id, String correlationId, String sessionId, String uiId,
                                    String nodeId, String eventType, Map<String, Object> payload) {
        return new IpcEnvelope("event", id, correlationId, sessionId, uiId,
                MockFlowRuntime.mapOf("nodeId", nodeId, "eventType", eventType, "payload", payload), null);
    }

    private static IpcEnvelope exchange(BufferedWriter writer, BufferedReader reader, IpcEnvelope request)
            throws IOException {
        String encoded = IpcCodec.encode(request);
        System.out.println("[host->sidecar] " + encoded);
        writer.write(encoded);
        writer.newLine();
        writer.flush();

        String response = reader.readLine();
        if (response == null) {
            throw new IOException("Sidecar closed stdout before response. Check sidecar trace above.");
        }
        System.out.println("[sidecar->host] " + response);
        IpcEnvelope envelope = IpcCodec.decode(response);
        if (envelope.error() != null) {
            throw new IllegalStateException("Sidecar returned error: " + envelope.error());
        }
        return envelope;
    }

    private static void requireType(IpcEnvelope envelope, String expectedType) {
        if (!expectedType.equals(envelope.type())) {
            throw new IllegalStateException("Expected " + expectedType + " but got " + envelope.type());
        }
    }

    private static final class MockWebView {
        private int counter = -1;
        private int rows = 0;
        private boolean sawBootstrap;
        private boolean sawResync;

        @SuppressWarnings("unchecked")
        private void apply(Map<String, Object> payload) {
            Object patches = payload.get("patches");
            if (!(patches instanceof List<?> list)) {
                throw new IllegalArgumentException("Payload has no patches: " + payload);
            }

            boolean fullResync = Boolean.TRUE.equals(payload.get("fullResync"));
            sawResync = sawResync || fullResync;
            for (Object item : list) {
                Map<String, Object> patch = (Map<String, Object>) item;
                String op = String.valueOf(patch.get("op"));
                String target = String.valueOf(patch.get("target"));
                Map<String, Object> value = (Map<String, Object>) patch.get("value");
                System.out.println("[mock-webview] apply op=" + op + " target=" + target + " value=" + value);
                if ("replace".equals(op) && "#app".equals(target)) {
                    sawBootstrap = true;
                    Map<String, Object> counterNode = (Map<String, Object>) value.get("counter");
                    counter = ((Number) counterNode.get("value")).intValue();
                    Object grid = value.get("grid");
                    if (grid instanceof Map<?, ?> gridMap) {
                        Object gridRows = gridMap.get("rows");
                        rows = gridRows instanceof List<?> rowList ? rowList.size() : rows;
                    }
                } else if ("setText".equals(op) && "#counter.value".equals(target)) {
                    counter = Integer.parseInt(String.valueOf(value.get("text")));
                } else if ("replaceRows".equals(op) && "#orders.grid".equals(target)) {
                    Object rowValue = value.get("rows");
                    rows = rowValue instanceof List<?> rowList ? rowList.size() : 0;
                }
            }
        }

        private void assertState() {
            if (!sawBootstrap || !sawResync || counter != 2 || rows != 3) {
                throw new IllegalStateException("Unexpected mock WebView state: sawBootstrap=" + sawBootstrap
                        + ", sawResync=" + sawResync + ", counter=" + counter + ", rows=" + rows);
            }
        }
    }
}

