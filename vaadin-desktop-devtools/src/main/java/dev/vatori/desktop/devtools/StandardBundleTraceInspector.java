package dev.vatori.desktop.devtools;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Inspects only the standard production-bundle Flow IPC runtime. */
public final class StandardBundleTraceInspector {
    private static final String HOST_PROTOCOL_PREFIX = "HOST_PROTOCOL ";
    private static final ObjectMapper JSON = JsonMapper.builder().build();

    private StandardBundleTraceInspector() {
    }

    public static void main(String[] args) {
        String[] effectiveArgs = args.length == 0 ? environmentArguments() : args;
        int exitCode = run(effectiveArgs, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static String[] environmentArguments() {
        if ("1".equals(System.getenv("VATORI_TRACE_INSPECT_HELP"))) {
            return new String[]{"--help"};
        }
        List<String> arguments = new ArrayList<>();
        String gates = System.getenv("VATORI_TRACE_INSPECT_GATES");
        if (gates != null && !gates.isBlank()) {
            for (String gate : gates.split(",")) {
                if (!gate.isBlank()) {
                    arguments.add(gate);
                }
            }
        }
        String trace = System.getenv("VATORI_TRACE_INSPECT_FILE");
        if (trace != null && !trace.isBlank()) {
            arguments.add(trace);
        }
        return arguments.toArray(String[]::new);
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && "--help".equals(args[0])) {
            printHelp(out);
            return 0;
        }

        Set<Gate> gates = EnumSet.noneOf(Gate.class);
        Path trace = null;
        for (String argument : args) {
            Gate gate = Gate.forOption(argument);
            if (gate != null) {
                gates.add(gate);
            } else if (argument.startsWith("--")) {
                err.println("Unknown option: " + argument);
                printHelp(err);
                return 2;
            } else if (trace == null) {
                trace = Path.of(argument);
            } else {
                err.println("Only one trace file may be inspected at a time.");
                return 2;
            }
        }
        if (trace == null) {
            printHelp(err);
            return 2;
        }

        TraceFacts facts;
        try {
            facts = read(trace);
        } catch (IOException | IllegalArgumentException error) {
            err.println("Unable to inspect " + trace + ": " + error.getMessage());
            return 1;
        }

        out.printf("Trace: %s%n", trace);
        out.printf("HOST_PROTOCOL messages: %d%n", facts.messageCount);
        out.printf("Types: %s%n", facts.typeCounts);
        if (gates.isEmpty()) {
            out.println("No acceptance gate requested.");
            return 0;
        }

        List<String> failures = new ArrayList<>();
        for (Gate gate : gates) {
            List<String> missing = gate.missing(facts);
            if (missing.isEmpty()) {
                out.println("PASS " + gate.option);
            } else {
                failures.add(gate.option + ": " + String.join(", ", missing));
            }
        }
        failures.forEach(failure -> err.println("FAIL " + failure));
        return failures.isEmpty() ? 0 : 2;
    }

    private static TraceFacts read(Path trace) throws IOException {
        TraceFacts facts = new TraceFacts();
        int lineNumber = 0;
        for (String line : Files.readAllLines(trace)) {
            lineNumber++;
            if (!line.startsWith(HOST_PROTOCOL_PREFIX)) {
                continue;
            }
            JsonNode root;
            try {
                root = JSON.readTree(line.substring(HOST_PROTOCOL_PREFIX.length()));
            } catch (JacksonException error) {
                throw new IllegalArgumentException(
                        "invalid HOST_PROTOCOL JSON at line " + lineNumber, error);
            }
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException(
                        "HOST_PROTOCOL line " + lineNumber + " is not an object");
            }
            facts.accept(root);
        }
        if (facts.messageCount == 0) {
            throw new IllegalArgumentException("no HOST_PROTOCOL messages found");
        }
        return facts;
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage: flow-ipc-trace-inspect [gate ...] <host.out.log>");
        out.println("Gates:");
        for (Gate gate : Gate.values()) {
            out.printf("  %-62s %s%n", gate.option, gate.description);
        }
    }

    private enum Gate {
        RUNTIME(
                "--require-flow-ipc-standard-bundle-runtime",
                "packaged bundle, opaque requests, push and clean shutdown"),
        FRAMEWORK(
                "--require-flow-ipc-standard-bundle-framework-app",
                "representative Flow component and native capability workflow"),
        SIDECAR_RECOVERY(
                "--require-flow-ipc-standard-bundle-sidecar-recovery",
                "bounded Java restart with retained native window and WebView");

        private final String option;
        private final String description;

        Gate(String option, String description) {
            this.option = option;
            this.description = description;
        }

        private static Gate forOption(String option) {
            for (Gate gate : values()) {
                if (gate.option.equals(option)) {
                    return gate;
                }
            }
            return null;
        }

        private List<String> missing(TraceFacts facts) {
            List<String> missing = facts.runtimeMissing();
            if (this == FRAMEWORK) {
                facts.requireFramework(missing);
            } else if (this == SIDECAR_RECOVERY) {
                facts.requireRecovery(missing);
            }
            return missing;
        }
    }

    private static final class TraceFacts {
        private final Map<String, Integer> typeCounts = new LinkedHashMap<>();
        private int messageCount;
        private boolean standardLaunch;
        private boolean standardJavaReady;
        private boolean standardBundleReady;
        private boolean standardFlowClientResource;
        private boolean firstMeaningfulPaint;
        private boolean shutdown;
        private boolean errorObserved;
        private int opaqueRequestCount;
        private int opaqueResponseCount;
        private int opaqueResponseEndCount;
        private int pushConnectedCount;
        private int pushCount;
        private JsonNode frameworkAcceptance;
        private JsonNode recoveryAcceptance;

        private void accept(JsonNode root) {
            messageCount++;
            String type = text(root, "type");
            typeCounts.merge(type.isBlank() ? "unknown" : type, 1, Integer::sum);
            JsonNode payload = root.path("payload");
            switch (type) {
                case "launch" -> standardLaunch = "flow-ipc".equals(text(payload, "transport"))
                        && bool(payload, "standardBundleMode")
                        && !bool(payload, "compatibilityMode");
                case "ready" -> {
                    JsonNode runtime = payload.path("envelope").path("payload");
                    standardJavaReady |= "standard-vaadin-bundle-opaque-ipc"
                            .equals(text(runtime, "runtime"))
                            && "opaque-http-stream-ipc"
                            .equals(text(runtime, "browserRequestTransport"))
                            && bool(runtime, "standardBundleMode")
                            && !bool(runtime, "compatibilityMode");
                }
                case "standardBundleReady" -> standardBundleReady |=
                        "opaque-http-stream-ipc".equals(text(payload, "requestTransport"));
                case "standardBundleResource" -> {
                    String path = text(payload, "path");
                    standardFlowClientResource |= path.contains("FlowClient-")
                            || path.contains("generated-flow-imports-");
                }
                case "webview.firstMeaningfulPaint" -> firstMeaningfulPaint = true;
                case "opaqueHttpStreamOpen" -> opaqueRequestCount++;
                case "opaqueHttpStreamResponse" -> {
                    JsonNode response = envelopePayload(payload);
                    if (response.path("status").asInt() >= 200
                            && response.path("status").asInt() < 400) {
                        opaqueResponseCount++;
                    }
                }
                case "opaqueHttpStreamResponseEnd" -> opaqueResponseEndCount++;
                case "opaquePushConnected" -> pushConnectedCount++;
                case "opaquePush" -> pushCount++;
                case "standardBundle.frameworkAcceptance" -> frameworkAcceptance = payload;
                case "standardBundle.sidecarRestartAcceptance" -> recoveryAcceptance = payload;
                case "shutdown.sidecar" -> shutdown = true;
                case "error", "webview.error" -> errorObserved = true;
                default -> {
                }
            }
        }

        private List<String> runtimeMissing() {
            List<String> missing = new ArrayList<>();
            require(missing, standardLaunch, "standard flow-ipc launch");
            require(missing, standardJavaReady, "standard Java runtime ready");
            require(missing, standardBundleReady, "packaged bundle ready");
            require(missing, standardFlowClientResource, "standard Flow client resource");
            require(missing, firstMeaningfulPaint, "first meaningful paint");
            require(missing, opaqueRequestCount >= 2, "bootstrap and UIDL requests");
            require(missing, opaqueResponseCount >= 2, "successful opaque responses");
            require(missing, opaqueResponseEndCount >= 2, "complete opaque responses");
            require(missing, pushConnectedCount >= 1, "opaque push connection");
            require(missing, pushCount >= 1, "opaque push message");
            require(missing, shutdown, "sidecar shutdown");
            require(missing, !errorObserved, "no host/runtime error");
            return missing;
        }

        private void requireFramework(List<String> missing) {
            JsonNode payload = frameworkAcceptance;
            require(missing, payload != null, "framework acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "standardBundleMode", "rootRendered", "flowClientOwned",
                    "opaqueRequestBridgeObserved", "opaqueResponseBridgeObserved",
                    "pushRecoveryObserved", "errorFree", "budgetEventObserved",
                    "checkboxEventObserved", "selectEventObserved", "datePickerEventObserved",
                    "dialogEventObserved", "formValidationObserved", "applyEventObserved",
                    "serverStateUpdated", "nativeWindowTitleApplied", "nativeClipboardApplied")) {
                require(missing, bool(payload, field), "framework " + field);
            }
        }

        private void requireRecovery(List<String> missing) {
            JsonNode payload = recoveryAcceptance;
            require(missing, payload != null, "sidecar recovery acceptance result");
            if (payload == null) {
                return;
            }
            for (String field : List.of(
                    "complete", "standardBundleMode", "errorFree", "processRestarted",
                    "windowPreserved", "webViewPreserved", "webViewReloaded",
                    "serverUiStateReset", "requestBridgeRecovered", "pushRecovered",
                    "interactionRecovered")) {
                require(missing, bool(payload, field), "recovery " + field);
            }
            require(missing, payload.path("recoveryGeneration").asInt() >= 1,
                    "recovery generation");
            require(missing,
                    payload.path("previousProcessIdentifier").asLong() > 0
                            && payload.path("processIdentifier").asLong() > 0
                            && payload.path("previousProcessIdentifier").asLong()
                            != payload.path("processIdentifier").asLong(),
                    "replaced sidecar process");
        }

        private static void require(List<String> missing, boolean condition, String label) {
            if (!condition && !missing.contains(label)) {
                missing.add(label);
            }
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.stringValue() : "";
    }

    private static boolean bool(JsonNode node, String field) {
        return node.path(field).isBoolean() && node.path(field).asBoolean();
    }

    private static JsonNode envelopePayload(JsonNode payload) {
        JsonNode nested = payload.path("envelope").path("payload");
        return nested.isObject() ? nested : payload;
    }
}
