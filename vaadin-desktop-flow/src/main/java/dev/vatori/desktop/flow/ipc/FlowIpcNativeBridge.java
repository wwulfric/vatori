package dev.vatori.desktop.flow.ipc;

import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;
import dev.vatori.desktop.ipc.FlowIpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

final class FlowIpcNativeBridge implements NativeBridge {
    private final PrintStream out;
    private final String route;
    private final AtomicLong sequence = new AtomicLong();

    FlowIpcNativeBridge(String route) {
        this(System.out, route);
    }

    FlowIpcNativeBridge(PrintStream out, String route) {
        this.out = out;
        this.route = route == null ? "" : route;
    }

    @Override
    public DesktopCommandResult invoke(DesktopCommand command) {
        String id = "desktop-command-" + sequence.incrementAndGet();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("command", command.name());
        payload.put("args", command.payload());
        FlowIpcEnvelope envelope = FlowIpcEnvelope.message(
                "command",
                id,
                id,
                "flow-ipc-session",
                "main",
                route,
                payload);
        out.println(FlowIpcRuntime.PREFIX + IpcJson.stringify(envelope));
        out.flush();
        return DesktopCommandResult.ok(Map.of(
                "transport", "flow-ipc",
                "sent", true,
                "command", command.name(),
                "correlationId", id));
    }
}
