package dev.vatori.desktop.devtools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DesktopTraceInspector {
    private static final Pattern TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DIRECTION = Pattern.compile("\"direction\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> FLOW_IPC_REQUIRED_TYPES = List.of(
            "launch",
            "ready",
            "window.create",
            "bootstrapRequest",
            "bootstrapResponse",
            "push",
            "event",
            "command",
            "capabilityResult",
            "patch",
            "webview.dom",
            "webview.snapshot",
            "shutdown");

    private DesktopTraceInspector() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "--help".equals(args[0])) {
            usage();
            return;
        }

        boolean requireFlowIpc = false;
        List<Path> paths = new ArrayList<>();
        for (String arg : args) {
            if ("--require-flow-ipc-v1".equals(arg)) {
                requireFlowIpc = true;
            } else {
                paths.add(Path.of(arg));
            }
        }

        if (paths.isEmpty()) {
            usage();
            System.exit(2);
        }

        int failures = 0;
        for (Path path : paths) {
            TraceSummary summary = inspect(path);
            summary.print(path);
            if (requireFlowIpc && !summary.hasRequiredFlowIpcTypes()) {
                failures++;
            }
        }

        if (failures > 0) {
            System.exit(2);
        }
    }

    public static TraceSummary inspect(Path path) throws IOException {
        TraceSummary summary = new TraceSummary();
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = normalize(rawLine);
            if (line.isBlank()) {
                continue;
            }
            summary.total++;
            String type = first(TYPE, line, "unknown");
            String direction = first(DIRECTION, line, "unknown");
            summary.types.merge(type, 1, Integer::sum);
            summary.directions.merge(direction, 1, Integer::sum);
            if (line.contains("\"transport\":\"flow-ipc\"")) {
                summary.flowIpcTransport = true;
            }
            if (line.contains("\"compatibilityMode\":false")) {
                summary.compatibilityFalse = true;
            }
            if (line.contains("Clicks: 1")) {
                summary.clickPatchObserved = true;
            }
            if (line.contains("native-nspasteboard")) {
                summary.nativeClipboardObserved = true;
            }
        }
        return summary;
    }

    private static String normalize(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("HOST_PROTOCOL ") ? trimmed.substring("HOST_PROTOCOL ".length()) : trimmed;
    }

    private static String first(Pattern pattern, String line, String fallback) {
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    private static void usage() {
        System.out.println("Usage: bin/flow-ipc-trace-inspect [--require-flow-ipc-v1] <trace.ndjson>...");
    }

    public static final class TraceSummary {
        private int total;
        private final Map<String, Integer> types = new LinkedHashMap<>();
        private final Map<String, Integer> directions = new LinkedHashMap<>();
        private boolean flowIpcTransport;
        private boolean compatibilityFalse;
        private boolean clickPatchObserved;
        private boolean nativeClipboardObserved;

        public boolean hasRequiredFlowIpcTypes() {
            return types.keySet().containsAll(FLOW_IPC_REQUIRED_TYPES)
                    && flowIpcTransport
                    && compatibilityFalse
                    && clickPatchObserved
                    && nativeClipboardObserved;
        }

        private void print(Path path) {
            System.out.println("Trace: " + path);
            System.out.println("Messages: " + total);
            System.out.println("Directions: " + directions);
            System.out.println("Types: " + types);
            System.out.println("transport=flow-ipc: " + flowIpcTransport);
            System.out.println("compatibilityMode=false: " + compatibilityFalse);
            System.out.println("click patch observed: " + clickPatchObserved);
            System.out.println("native clipboard observed: " + nativeClipboardObserved);
            List<String> missing = FLOW_IPC_REQUIRED_TYPES.stream()
                    .filter(type -> !types.containsKey(type))
                    .toList();
            if (!missing.isEmpty()) {
                System.out.println("Missing required types: " + missing);
            }
        }
    }
}
