package dev.vatori.desktop.host;

import dev.vatori.desktop.DesktopCommand;
import dev.vatori.desktop.DesktopCommandResult;
import dev.vatori.desktop.NativeBridge;
import dev.vatori.desktop.ipc.IpcEnvelope;
import dev.vatori.desktop.ipc.IpcJson;
import dev.vatori.desktop.ipc.IpcTraceLogger;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class HostProtocolNativeBridge implements NativeBridge {
    public static final String STDOUT_PREFIX = "VDP1 ";

    private final PrintStream out;
    private final IpcTraceLogger trace;
    private final AtomicLong sequence = new AtomicLong();

    public HostProtocolNativeBridge() {
        this(System.out);
    }

    HostProtocolNativeBridge(PrintStream out) {
        this.out = out;
        this.trace = new IpcTraceLogger(out, "host-protocol-native-bridge");
    }

    @Override
    public DesktopCommandResult invoke(DesktopCommand command) {
        String id = "desktop-command-" + sequence.incrementAndGet();
        IpcEnvelope envelope = IpcEnvelope.command(
                id,
                id,
                "compatibility-session",
                "main",
                command.name(),
                command.payload());
        trace.trace("send", "command", Map.of("id", id, "name", command.name()));
        out.println(STDOUT_PREFIX + IpcJson.stringify(envelope));
        out.flush();
        return DesktopCommandResult.ok(Map.of(
                "hostProtocol", "stdout-v0",
                "sent", true,
                "command", command.name(),
                "correlationId", id));
    }
}
