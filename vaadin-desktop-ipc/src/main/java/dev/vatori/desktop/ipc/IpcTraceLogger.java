package dev.vatori.desktop.ipc;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class IpcTraceLogger {
    private final PrintStream out;
    private final String component;
    private final AtomicLong sequence = new AtomicLong();

    public IpcTraceLogger(PrintStream out, String component) {
        this.out = out;
        this.component = component;
    }

    public void trace(String direction, String type, Map<String, ?> payload) {
        long id = sequence.incrementAndGet();
        out.println("TRACE component=" + component
                + " seq=" + id
                + " at=" + Instant.now()
                + " direction=" + direction
                + " type=" + type
                + " payload=" + payload);
    }
}

