package dev.vatori.desktop.devtools;

import dev.vatori.desktop.ipc.IpcTraceLogger;

import java.util.Map;

public final class DesktopTraceInspector {
    private final IpcTraceLogger trace = new IpcTraceLogger(System.out, "desktop-trace-inspector");

    public void record(String type, Map<String, ?> payload) {
        trace.trace("inspect", type, payload);
    }
}

