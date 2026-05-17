package dev.vatori.flowipc.trace;

import dev.vatori.flowipc.json.Json;

import java.io.PrintStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TraceLogger {
    private final PrintStream out;
    private final String component;

    public TraceLogger(PrintStream out, String component) {
        this.out = out;
        this.component = component;
    }

    public void log(String direction, Object message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("at", Instant.now().toString());
        event.put("component", component);
        event.put("direction", direction);
        event.put("message", message);
        out.println(Json.stringify(event));
    }
}

