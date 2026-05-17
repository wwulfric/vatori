package dev.vatori.flowipc.sidecar;

import dev.vatori.flowipc.ipc.FlowPayloads;
import dev.vatori.flowipc.ipc.IpcCodec;
import dev.vatori.flowipc.ipc.IpcEnvelope;
import dev.vatori.flowipc.runtime.MockFlowRuntime;
import dev.vatori.flowipc.spi.FlowTransport;
import dev.vatori.flowipc.trace.TraceLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class SidecarMain {
    private SidecarMain() {
    }

    public static void main(String[] args) throws IOException {
        FlowTransport runtime = new MockFlowRuntime();
        TraceLogger trace = new TraceLogger(System.err, "java-sidecar");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true)) {
            String line;
            boolean running = true;
            while (running && (line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                IpcEnvelope request = null;
                IpcEnvelope response;
                try {
                    request = IpcCodec.decode(line);
                    trace.log("recv", request.toMap());
                    response = handle(runtime, request);
                    if ("shutdown".equals(request.type())) {
                        running = false;
                    }
                } catch (Exception error) {
                    response = IpcEnvelope.error(request, "SIDECAR_ERROR", error.getMessage());
                }

                writer.println(IpcCodec.encode(response));
                trace.log("send", response.toMap());
            }
        }
    }

    private static IpcEnvelope handle(FlowTransport runtime, IpcEnvelope request) {
        return switch (request.type()) {
            case "bootstrap" -> {
                FlowTransport.BootstrapResult result = runtime.bootstrap(FlowPayloads.bootstrapRequest(request));
                yield IpcEnvelope.response(
                        "bootstrapResult",
                        request,
                        result.sessionId(),
                        result.uiId(),
                        FlowPayloads.bootstrapPayload(result));
            }
            case "event" -> {
                FlowTransport.PatchResult result = runtime.dispatch(FlowPayloads.clientEvent(request));
                yield IpcEnvelope.response(
                        "patch",
                        request,
                        result.sessionId(),
                        result.uiId(),
                        FlowPayloads.patchPayload(result));
            }
            case "resync" -> {
                FlowTransport.PatchResult result = runtime.resync(FlowPayloads.resyncRequest(request));
                yield IpcEnvelope.response(
                        "patch",
                        request,
                        result.sessionId(),
                        result.uiId(),
                        FlowPayloads.patchPayload(result));
            }
            case "shutdown" -> IpcEnvelope.response(
                    "ack",
                    request,
                    request.sessionId(),
                    request.uiId(),
                    Map.of("status", "sidecar-stopping"));
            default -> IpcEnvelope.error(request, "UNKNOWN_MESSAGE_TYPE", "Unsupported message type: " + request.type());
        };
    }
}
