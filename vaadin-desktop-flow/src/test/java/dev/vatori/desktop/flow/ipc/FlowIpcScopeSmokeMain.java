package dev.vatori.desktop.flow.ipc;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class FlowIpcScopeSmokeMain {
    private FlowIpcScopeSmokeMain() {
    }

    public static void main(String[] args) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

        String input = FlowIpcRuntime.PREFIX + envelope("bootstrapRequest", "bootstrap", "flow-ipc-session", "main")
                + "\n"
                + FlowIpcRuntime.PREFIX + clientErrorEnvelope() + "\n"
                + FlowIpcRuntime.PREFIX + payloadScopeSpoofEnvelope() + "\n"
                + FlowIpcRuntime.PREFIX + envelope("event", "bad-session", "wrong-session", "main") + "\n"
                + FlowIpcRuntime.PREFIX + envelope("event", "bad-ui", "flow-ipc-session", "wrong-ui") + "\n";

        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
            new FlowIpcRuntime(ScopeSmokeView.class).run();
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        String output = captured.toString(StandardCharsets.UTF_8);
        String errorOutput = capturedErr.toString(StandardCharsets.UTF_8);
        if (!output.contains("\"code\":\"invalidClientState\"")
                || !output.contains("Flow IPC sessionId mismatch")
                || !output.contains("Flow IPC uiId mismatch")
                || !output.contains("\"id\":\"payload-scope-spoof\"")) {
            throw new IllegalStateException("Flow IPC scope smoke did not reject invalid envelope scope:\n" + output);
        }
        if (!errorOutput.contains("FLOW_IPC_CLIENT_ERROR id=client-error code=webviewBridgeFailure")
                || !errorOutput.contains("message=client bridge exploded")) {
            throw new IllegalStateException("Flow IPC scope smoke did not preserve client error details:\n"
                    + errorOutput);
        }
        System.out.println("invalid Flow IPC sessionId/uiId rejected");
    }

    private static String envelope(String type, String id, String sessionId, String uiId) {
        return """
                {"version":"flow-ipc-v1","type":"%s","id":"%s","correlationId":"%s","sessionId":"%s","uiId":"%s","route":"","payload":{},"error":null,"timestamp":0}
                """.formatted(type, id, id, sessionId, uiId).trim();
    }

    private static String payloadScopeSpoofEnvelope() {
        return """
                {"version":"flow-ipc-v1","type":"event","id":"payload-scope-spoof","correlationId":"payload-scope-spoof","payload":{"sessionId":"flow-ipc-session","uiId":"main","securityKey":"not-authoritative","action":"click","componentId":"flow-ipc-scope-smoke"},"sessionId":"wrong-session","uiId":"wrong-ui","route":"","error":null,"timestamp":0}
                """.trim();
    }

    private static String clientErrorEnvelope() {
        return """
                {"version":"flow-ipc-v1","type":"error","id":"client-error","correlationId":"client-error","sessionId":"flow-ipc-session","uiId":"main","route":"","payload":{},"error":{"code":"webviewBridgeFailure","message":"client bridge exploded"},"timestamp":0}
                """.trim();
    }

    @Route("")
    public static final class ScopeSmokeView extends VerticalLayout {
        public ScopeSmokeView() {
            setId("flow-ipc-scope-smoke");
            add(new Button("Scope smoke"));
        }
    }
}
