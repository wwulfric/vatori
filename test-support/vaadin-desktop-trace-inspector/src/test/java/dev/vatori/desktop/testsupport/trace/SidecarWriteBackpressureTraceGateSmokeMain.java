package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Synthetic gate mutations only; real EAGAIN/AppKit/process evidence comes from the runner. */
public final class SidecarWriteBackpressureTraceGateSmokeMain {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PREFIX = "flowSidecarWriteBackpressure.";
    private static final String OLD_PREFIX = "flowHostShutdownTermination.";
    private static int positives, negatives;

    public static void main(String[] args) throws Exception {
        List<ObjectNode> valid = valid();
        expect(valid, true, "actual pressure, retained main loop and confirmed product Quit");
        List<ObjectNode> repeated = copy(valid);
        repeated.add(find(repeated, "sidecar.writeBackpressure"), repeated.get(find(repeated, "sidecar.writeBackpressure")).deepCopy());
        expect(repeated, true, "multiple real EAGAIN observations in the same bounded writer");
        List<ObjectNode> early = copy(valid);
        early.add(find(early, PREFIX + "nativePointerDispatch"), early.get(find(early, "sidecar.writeBackpressure")).deepCopy());
        expect(early, true, "incidental preparation pressure does not replace the stopped-child pressure");
        List<ObjectNode> teardown = withTeardown(valid);
        expect(teardown, true, "AppKit window teardown only after final retained-object receipt");
        teardown(teardown);
        dropAndSchema(valid);
        preparation(valid);
        burst(valid);
        lifecycle(valid);
        publicEntry(valid);
        System.out.printf("Sidecar write backpressure trace gate smoke passed: %d positives, %d rejections%n", positives, negatives);
    }

    private static void dropAndSchema(List<ObjectNode> valid) {
        for (int row = 0; row < valid.size(); row++) {
            String type = type(valid.get(row));
            if (!type.startsWith(PREFIX) && !Set.of("sidecar.writeBackpressure", "sidecar.shutdownTerminated", "sidecar.shutdownEscalated",
                    "sidecar.exit", "shutdown.sidecar", "host.sidecarShutdownCompleted").contains(type)) continue;
            List<ObjectNode> removed = copy(valid); removed.remove(row); expect(removed, false, "missing " + type);
            if (!type.equals("sidecar.writeBackpressure")) {
                List<ObjectNode> duplicate = copy(valid); duplicate.add(row, duplicate.get(row).deepCopy()); expect(duplicate, false, "duplicate " + type);
            }
            mutate(valid, row, e -> e.put("direction", "wrong"), "wrong direction " + type);
            mutate(valid, row, e -> payload(e).put("unknown", true), "extra field " + type);
            for (String field : List.copyOf(payload(valid.get(row)).propertyNames())) {
                mutate(valid, row, e -> payload(e).remove(field), "missing " + type + "/" + field);
                mutate(valid, row, e -> payload(e).set(field, JSON.createArrayNode()), "wrong JSON type " + type + "/" + field);
                JsonNode value = payload(valid.get(row)).path(field);
                if (value.isBoolean()) mutate(valid, row, e -> payload(e).put(field, !value.asBoolean()), "false assertion " + type + "/" + field);
                else if (value.isIntegralNumber()) mutate(valid, row, e -> payload(e).put(field, value.asDouble() + 0.5), "fractional count " + type + "/" + field);
            }
        }
        List<String> order = new ArrayList<>(List.of("capabilityManifest", "launch.sidecar", "sidecar.started", "ready", "window.create", "webview.loaded",
                PREFIX + "nativePointerDispatch", PREFIX + "prepared", PREFIX + "sidecarSuspended", PREFIX + "burstRequested",
                PREFIX + "browserDispatchCompleted", PREFIX + "mainLoopHeartbeat", PREFIX + "quitDispatched",
                "host.nativeResourceDrainStarted", "host.nativeResourceDrainCompleted", "shutdown", "sidecar.shutdownTerminated",
                "sidecar.shutdownEscalated", "sidecar.exit", "shutdown.sidecar", "host.sidecarShutdownCompleted",
                PREFIX + "applicationWillTerminate", PREFIX + "externalExitObserved"));
        for (int i = 1; i < order.size(); i++) {
            List<ObjectNode> broken = copy(valid); Collections.swap(broken, find(broken, order.get(i)), find(broken, order.get(i - 1)));
            expect(broken, false, "reordered " + order.get(i));
        }
        List<ObjectNode> afterExternal = copy(valid); afterExternal.add(event(50000, "recv", "later", object()));
        expect(afterExternal, false, "external absence is final");
    }

    private static void preparation(List<ObjectNode> valid) {
        for (String type : List.of("opaquePush", "opaquePushAck", "opaqueHttpStreamChunk", "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseEnd")) {
            List<ObjectNode> broken = copy(valid); broken.remove(find(broken, type)); expect(broken, false, "missing actual preparation " + type);
        }
        mutate(valid, find(valid, "opaquePushAck"), e -> body(e).put("lastSeenServerSyncId", 1), "transport ACK without Flow consumption");
        mutate(valid, find(valid, "opaqueHttpStreamChunk"), e -> body(e).put("chunkSequence", 1), "request body sequence gap");
        mutate(valid, find(valid, "opaqueHttpStreamResponseEnd"), e -> body(e).put("bodyBytes", 999), "response byte accounting mismatch");
        mutate(valid, find(valid, PREFIX + "prepared"), e -> payload(e).put("requestCount", 999), "DOM counter unsupported by actual trace");
        mutate(valid, find(valid, PREFIX + "prepared"), e -> payload(e).put("processIdentifier", 999), "prepared original JVM mismatch");
        mutate(valid, find(valid, PREFIX + "nativePointerDispatch"), e -> payload(e).put("inputMode", "browser-programmatic"), "programmatic prepare not native input");
        mutate(valid, find(valid, "ready"), e -> body(e).put("transport", "compatibility-http"), "compatibility ready is not pure IPC");
        mutate(valid, find(valid, "sidecar.started"), e -> payload(e).put("generation", 2), "new process generation");
        List<ObjectNode> noAfter = copy(valid); noAfter.remove(find(noAfter, "sidecar.writeBackpressure"));
        noAfter.add(find(noAfter, PREFIX + "nativePointerDispatch"), valid.get(find(valid, "sidecar.writeBackpressure")).deepCopy());
        expect(noAfter, false, "preparation-only EAGAIN is not stopped-child pressure");
    }

    private static void burst(List<ObjectNode> valid) {
        int pressure = find(valid, "sidecar.writeBackpressure"), first = probe(valid, 0);
        for (String field : List.of("pendingBytes", "pendingFrames")) mutate(valid, pressure, e -> payload(e).put(field, 0), "zero backlog " + field);
        mutate(valid, pressure, e -> payload(e).put("pendingBytes", 16 * 1024 * 1024 + 1), "unbounded queued bytes");
        mutate(valid, pressure, e -> payload(e).put("pendingFrames", 257), "unbounded queued frames");
        mutate(valid, pressure, e -> payload(e).put("partialFrameBytes", -1), "invalid partial progress");
        mutate(valid, pressure, e -> payload(e).put("partialFrameBytes", 180001), "partial progress exceeds retained byte count");
        mutate(valid, pressure, e -> payload(e).put("processIdentifier", 999), "foreign JVM EAGAIN");
        mutate(valid, pressure, e -> payload(e).put("generation", 2), "new generation EAGAIN");
        mutate(valid, first, e -> body(e).put("url", "https://app.vatori.invalid/__vatori_acceptance/write-backpressure?probe=16"), "out-of-range request probe");
        mutate(valid, first, e -> body(e).put("url", "https://example.com/__vatori_acceptance/write-backpressure?probe=0"), "foreign URL origin");
        mutate(valid, first, e -> body(e).put("url", text(body(valid.get(probe(valid, 1))), "url")), "duplicate probe URL");
        mutate(valid, first, e -> body(e).put("method", "POST"), "unexpected body-bearing burst");
        mutate(valid, first, e -> body(e).put("contentLength", -1), "burst body length unknown");
        mutate(valid, first, e -> body(e).set("headers", object("x-vatori-backpressure", "w".repeat(16383))), "short pressure header");
        mutate(valid, first, e -> body(e).set("headers", object("x-vatori-backpressure", "x".repeat(16384))), "wrong pressure data");
        mutate(valid, first, e -> envelope(e).put("sessionId", "foreign"), "foreign burst scope");
        mutate(valid, first, e -> envelope(e).put("correlationId", "foreign"), "uncorrelated burst request");
        for (String type : List.of("opaqueHttpStreamOpen", "opaqueHttpStreamEnd")) {
            int row = index(valid, e -> type(e).equals(type) && text(body(e), "requestId").equals("probe-0"));
            List<ObjectNode> removed = copy(valid); removed.remove(row); expect(removed, false, "missing burst " + type);
            List<ObjectNode> duplicate = copy(valid); duplicate.add(row, duplicate.get(row).deepCopy()); expect(duplicate, false, "duplicate burst " + type);
        }
        List<ObjectNode> response = copy(valid);
        response.add(find(response, PREFIX + "mainLoopHeartbeat"), wire(10000, "opaqueHttpStreamResponse", "probe-0", object("requestId", "probe-0", "status", 200)));
        expect(response, false, "stopped JVM cannot emit a response");
        List<ObjectNode> latePressure = copy(valid);
        latePressure.add(find(latePressure, PREFIX + "quitDispatched"), latePressure.remove(pressure));
        expect(latePressure, false, "pressure only after heartbeat cannot prove responsive pressure interval");
        for (int delay : List.of(0, 100, 6000)) mutate(valid, find(valid, PREFIX + "mainLoopHeartbeat"),
                e -> e.put("at", valid.get(find(valid, PREFIX + "browserDispatchCompleted")).path("at").asLong() + delay), "invalid delayed heartbeat " + delay);
    }

    private static void lifecycle(List<ObjectNode> valid) {
        for (String type : List.of("error", "webview.error", "sidecar.writeFailed", "sidecar.shutdownFailed", "host.sidecarShutdownFailed",
                "host.failureShutdownStarted", "host.failureShutdownCompleted", "sidecar.recoveryScheduled", "sidecar.replacementRequested",
                "command", "capabilityResult", "windowClosed", "opaqueHttpStreamCancel", PREFIX + "unknown")) {
            List<ObjectNode> broken = copy(valid); broken.add(1, event(100, "recv", type, object())); expect(broken, false, "forbidden " + type);
        }
        List<ObjectNode> failed = copy(valid); failed.add(1, event(100, "send", "flow-ipc.sidecarWrite", object("status", "write-failed")));
        expect(failed, false, "hidden failed ordinary write");
        mutate(valid, find(valid, "sidecar.exit"), e -> payload(e).put("status", 0), "stopped JVM cannot exit cooperatively");
        mutate(valid, find(valid, "sidecar.exit"), e -> payload(e).put("processIdentifier", 999), "exit of wrong child");
        mutate(valid, find(valid, PREFIX + "sidecarSuspended"), e -> payload(e).put("signal", "SIGTERM"), "request termination is not SIGSTOP");
        mutate(valid, find(valid, PREFIX + "sidecarSuspended"), e -> payload(e).put("processState", 2), "child not OS stopped");
        mutate(valid, find(valid, PREFIX + "quitDispatched"), e -> payload(e).put("source", "direct-test-terminate"), "not product menu action");
        for (String type : List.of("sidecar.shutdownTerminated", "sidecar.shutdownEscalated")) {
            mutate(valid, find(valid, type), e -> payload(e).put("processIdentifier", 999), "foreign signal " + type);
            mutate(valid, find(valid, type), e -> e.put("at", 1), "signal before shutdown " + type);
        }
    }

    private static List<ObjectNode> withTeardown(List<ObjectNode> valid) {
        List<ObjectNode> result = copy(valid);
        int row = result.size() - 1;
        long at = result.get(row - 1).path("at").asLong();
        result.add(row++, event(at, "recv", "window.closeRequested", scope().put("label", "main")));
        result.add(row++, event(at, "send", "flow-ipc.sidecarWrite", object("target", "java-sidecar", "status", "lifecycle-unavailable",
                "hostShutdownRequested", true, "initialReadyPending", false, "recoveryInProgress", false, "replacementInProgress", false)));
        result.add(row, event(at, "recv", "windowClosedDropped", scope().put("reason", "sidecar-lifecycle-unavailable")));
        return result;
    }

    private static void teardown(List<ObjectNode> valid) {
        for (String type : List.of("window.closeRequested", "flow-ipc.sidecarWrite", "windowClosedDropped")) {
            int row = find(valid, type);
            List<ObjectNode> missing = copy(valid); missing.remove(row); expect(missing, false, "incomplete teardown " + type);
            List<ObjectNode> duplicate = copy(valid); duplicate.add(row, duplicate.get(row).deepCopy()); expect(duplicate, false, "duplicate teardown " + type);
            mutate(valid, row, e -> e.put("direction", "wrong"), "wrong teardown direction " + type);
            mutate(valid, row, e -> payload(e).put("extra", true), "unexpected teardown field " + type);
            for (String field : List.copyOf(payload(valid.get(row)).propertyNames())) {
                mutate(valid, row, e -> payload(e).remove(field), "missing teardown field " + type + "/" + field);
                mutate(valid, row, e -> payload(e).set(field, JSON.createArrayNode()), "wrong teardown type " + type + "/" + field);
            }
            List<ObjectNode> early = copy(valid); early.add(find(early, PREFIX + "applicationWillTerminate"), early.remove(row));
            expect(early, false, "teardown before AppKit completion " + type);
        }
        for (String type : List.of("window.closeRequested", "windowClosedDropped")) {
            for (String field : List.of("windowId", "sessionId", "uiId")) mutate(valid, find(valid, type),
                    e -> payload(e).put(field, "foreign"), "foreign teardown scope " + type + "/" + field);
        }
        int write = find(valid, "flow-ipc.sidecarWrite"), close = find(valid, "window.closeRequested"), drop = find(valid, "windowClosedDropped");
        mutate(valid, write, e -> payload(e).put("hostShutdownRequested", false), "teardown without shutdown fence");
        for (String field : List.of("initialReadyPending", "recoveryInProgress", "replacementInProgress")) mutate(valid, write,
                e -> payload(e).put(field, true), "teardown rejected for wrong lifecycle phase " + field);
        for (String status : List.of("written", "write-failed", "admission-rejected", "sidecar-unavailable")) mutate(valid, write,
                e -> payload(e).put("status", status), "non-terminal write outcome in teardown " + status);
        mutate(valid, drop, e -> payload(e).put("reason", "owner-window-retired"), "wrong final window drop reason");
        List<ObjectNode> swapped = copy(valid); Collections.swap(swapped, close, write); expect(swapped, false, "write before teardown close");
        swapped = copy(valid); Collections.swap(swapped, write, drop); expect(swapped, false, "drop before teardown write rejection");
        List<ObjectNode> interrupted = copy(valid); interrupted.add(write, event(50000, "recv", "unexpected-middle-event", object()));
        expect(interrupted, false, "noncontiguous teardown cannot excuse arbitrary rejected write");
    }

    private static void publicEntry(List<ObjectNode> events) throws Exception {
        Path trace = Files.createTempFile("vatori-sidecar-backpressure-inspector-", ".log");
        try {
            StringBuilder text = new StringBuilder(); for (ObjectNode e : events) text.append("HOST_PROTOCOL ").append(e).append('\n');
            Files.writeString(trace, text);
            ByteArrayOutputStream output = new ByteArrayOutputStream(), errors = new ByteArrayOutputStream();
            int status = StandardBundleTraceInspector.run(new String[]{"--require-flow-ipc-sidecar-write-backpressure", trace.toString()},
                    new PrintStream(output), new PrintStream(errors));
            if (status != 0) throw new AssertionError("public gate rejected valid trace: " + output + errors);
            positives++;
        } finally { Files.deleteIfExists(trace); }
    }

    private static List<ObjectNode> valid() {
        List<ObjectNode> events = new ArrayList<>();
        events.add(event(900, "recv", "launch", object("transport", "flow-ipc")));
        for (ObjectNode source : HostShutdownTerminationTraceGateSmokeMain.valid("cooperative")) {
            ObjectNode e = source.deepCopy();
            if (type(e).startsWith(OLD_PREFIX)) { e.put("type", type(e).replace(OLD_PREFIX, PREFIX)); payload(e).remove("mode"); }
            if (type(e).equals("ready")) body(e).put("flowClientResourceLoading", "standard-production-bundle");
            events.add(e);
            if (type(e).equals("webview.loaded")) {
                events.add(event(1550, "recv", "standardBundleReady", object("requestTransport", "opaque-http-stream-ipc")));
                events.add(event(1550, "recv", "standardBundleResource", object("path", "/VAADIN/build/FlowClient-synthetic.js")));
                events.add(event(1550, "recv", "webview.firstMeaningfulPaint", object()));
                events.add(event(1550, "recv", "opaquePushConnected", object()));
            }
            if (type(e).equals(PREFIX + "prepared")) break;
        }
        ObjectNode identity = payload(events.getLast()).deepCopy();
        identity.retain(Set.of("windowId", "sessionId", "uiId", "runId", "processIdentifier", "hostProcessIdentifier", "recoveryGeneration"));
        long clock = events.getLast().path("at").asLong() + 100;
        events.add(event(clock++, "recv", PREFIX + "sidecarSuspended", extra(identity, "signal", "SIGSTOP", "processState", 4, "stopped", true)));
        events.add(event(clock++, "recv", PREFIX + "burstRequested", extra(identity, "requestCount", 16, "headerBytes", 16384, "browserProgrammatic", true)));
        for (int i = 0; i < 16; i++) {
            String id = "probe-" + i;
            events.add(wire(clock++, "opaqueHttpStreamOpen", id, object("requestId", id, "method", "GET", "contentLength", 0,
                    "url", "https://app.vatori.invalid/__vatori_acceptance/write-backpressure?probe=" + i,
                    "headers", object("x-vatori-backpressure", "w".repeat(16384)))));
            events.add(wire(clock++, "opaqueHttpStreamEnd", id, object("requestId", id, "chunkCount", 0, "bodyBytes", 0)));
        }
        events.add(event(clock++, "recv", "sidecar.writeBackpressure", object("generation", 1, "processIdentifier", 111,
                "pendingBytes", 180000, "pendingFrames", 16, "partialFrameBytes", 8192)));
        events.add(event(clock, "recv", PREFIX + "browserDispatchCompleted", extra(identity, "requestCount", 16)));
        clock += 1000;
        events.add(event(clock++, "recv", PREFIX + "mainLoopHeartbeat", extra(identity, "mainThread", true, "sidecarStopped", true,
                "windowRetained", true, "webViewRetained", true, "browserDispatchCompleted", true)));
        events.add(event(clock++, "recv", PREFIX + "quitDispatched", extra(identity, "source", "product-application-menu", "action", "terminate:",
                "keyEquivalent", "q", "commandModifier", true, "menuMatched", true, "activeApplication", true, "keyWindow", true)));
        events.add(event(clock++, "recv", "host.nativeResourceDrainStarted", object("pendingCount", 0, "timeoutSeconds", 2)));
        events.add(event(clock++, "recv", "host.nativeResourceDrainCompleted", object("drained", true, "pendingCount", 0, "failureCount", 0)));
        events.add(event(clock, "send", "shutdown", object("target", "java-sidecar", "transport", "flow-ipc", "reason", "host-terminate")));
        clock += 1000; events.add(event(clock, "send", "sidecar.shutdownTerminated", object("generation", 1, "processIdentifier", 111, "graceSeconds", 1)));
        clock += 1000; events.add(event(clock++, "send", "sidecar.shutdownEscalated", object("generation", 1, "processIdentifier", 111, "graceSeconds", 1)));
        events.add(event(clock++, "recv", "sidecar.exit", object("generation", 1, "processIdentifier", 111, "status", 9, "reason", 2, "expected", true)));
        events.add(event(clock++, "recv", "shutdown.sidecar", object("mode", "killed")));
        events.add(event(clock++, "recv", "host.sidecarShutdownCompleted", object("succeeded", true)));
        events.add(event(clock++, "recv", PREFIX + "applicationWillTerminate", extra(identity, "complete", true, "preparedObserved", true,
                "sidecarSuspendedObserved", true, "burstDispatched", true, "heartbeatObserved", true, "quitDispatched", true,
                "terminationSource", "applicationWillTerminate", "currentProcessIdentifier", 0, "originalSidecarAbsent", true,
                "originalWindowRetained", true, "originalWebViewRetained", true, "startupMarkerMatched", true)));
        events.add(event(clock, "recv", PREFIX + "externalExitObserved", extra(identity, "schemaVersion", 1, "hostAbsent", true,
                "sidecarAbsent", true, "markersMatched", true, "cleanupPerformed", false)));
        return events;
    }

    private static ObjectNode wire(long at, String type, String id, ObjectNode body) {
        boolean send = !type.equals("opaqueHttpStreamResponse");
        ObjectNode envelope = scope().put("version", 1).put("type", type).put("id", id + "-" + type).put("correlationId", id).putNull("error");
        envelope.set("payload", body);
        return event(at, send ? "send" : "recv", type, object(send ? "target" : "source", "java-sidecar", "transport", "flow-ipc", "envelope", envelope, "sidecarGeneration", 1));
    }
    private static int probe(List<ObjectNode> events, int i) { return index(events, e -> type(e).equals("opaqueHttpStreamOpen") && text(body(e), "requestId").equals("probe-" + i)); }
    private static int index(List<ObjectNode> events, java.util.function.Predicate<ObjectNode> match) { for (int i = 0; i < events.size(); i++) if (match.test(events.get(i))) return i; throw new AssertionError("missing event"); }
    private static int find(List<ObjectNode> events, String type) { return index(events, e -> type(e).equals(type)); }
    private static List<ObjectNode> copy(List<ObjectNode> events) { return new ArrayList<>(events.stream().map(ObjectNode::deepCopy).toList()); }
    private static void mutate(List<ObjectNode> valid, int row, Consumer<ObjectNode> action, String label) { List<ObjectNode> copy = copy(valid); action.accept(copy.get(row)); expect(copy, false, label); }
    private static void expect(List<ObjectNode> events, boolean pass, String label) {
        SidecarWriteBackpressureTraceGate gate = new SidecarWriteBackpressureTraceGate(); events.forEach(gate::accept); List<String> missing = new ArrayList<>(); gate.require(missing);
        if (missing.isEmpty() != pass) throw new AssertionError(label + " expected " + pass + ": " + missing);
        if (pass) positives++; else negatives++;
    }
    private static ObjectNode event(long at, String direction, String type, ObjectNode p) { return object("at", at, "direction", direction, "type", type, "payload", p); }
    private static ObjectNode extra(ObjectNode identity, Object... fields) { ObjectNode result = identity.deepCopy(); result.setAll(object(fields)); return result; }
    private static ObjectNode scope() { return object("windowId", "main", "sessionId", "flow-ipc-session", "uiId", "main"); }
    private static ObjectNode object(Object... fields) { ObjectNode result = JSON.createObjectNode(); for (int i = 0; i < fields.length; i += 2) result.set((String) fields[i], JSON.valueToTree(fields[i + 1])); return result; }
    private static String text(JsonNode p, String k) { return p.path(k).isString() ? p.path(k).asString() : ""; }
    private static String type(ObjectNode e) { return text(e, "type"); }
    private static ObjectNode payload(ObjectNode e) { return (ObjectNode) e.path("payload"); }
    private static ObjectNode envelope(ObjectNode e) { return (ObjectNode) payload(e).path("envelope"); }
    private static ObjectNode body(ObjectNode e) { return (ObjectNode) envelope(e).path("payload"); }
}
