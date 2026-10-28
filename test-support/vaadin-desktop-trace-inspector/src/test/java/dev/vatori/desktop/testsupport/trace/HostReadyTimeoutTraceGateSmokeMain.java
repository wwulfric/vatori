package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Synthetic contract mutations, not proof of a real process or shutdown hook. */
public final class HostReadyTimeoutTraceGateSmokeMain {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String EXTERNAL = "hostReadyTimeout.externalExitObserved";
    private static final long PID = 101, HOST_PID = 202;
    private static int positives, negatives;

    public static void main(String[] args) {
        for (String mode : List.of("cooperative", "blocked")) {
            List<ObjectNode> valid = valid(mode);
            expect(valid, true, mode + " actual exit chain");
            schemaAndOrder(valid);
            identityAndOutcome(valid, mode);
        }
        System.out.printf("Host ready timeout trace gate smoke passed: %d positives, %d rejections%n", positives, negatives);
    }

    static String validTrace(String mode) {
        StringBuilder output = new StringBuilder();
        for (ObjectNode event : valid(mode)) output.append("HOST_PROTOCOL ").append(event).append('\n');
        return output.toString();
    }

    private static void schemaAndOrder(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            int row = i;
            String type = type(valid.get(i));
            List<ObjectNode> missing = copy(valid); missing.remove(i);
            expect(missing, false, "missing " + type);
            List<ObjectNode> duplicate = copy(valid); duplicate.add(i, duplicate.get(i).deepCopy());
            expect(duplicate, false, "duplicate " + type);
            mutate(valid, row, e -> e.put("direction", text(e, "direction").equals("recv") ? "send" : "recv"), "wrong direction " + type);
            mutate(valid, row, e -> e.remove("at"), "missing timestamp " + type);
            mutate(valid, row, e -> e.put("at", "1000"), "string timestamp " + type);
            for (String field : List.copyOf(payload(valid.get(i)).propertyNames())) {
                mutate(valid, row, e -> payload(e).remove(field), "missing " + type + "/" + field);
                // Deliberately change the JSON type, not merely a value accepted by a nonempty string field.
                mutate(valid, row, e -> payload(e).set(field, JSON.createArrayNode()), "wrong type " + type + "/" + field);
            }
            if (!type.equals("launch")) mutate(valid, row, e -> payload(e).put("unrecognized", true), "extra field " + type);
            if (i > 0) {
                List<ObjectNode> reordered = copy(valid); Collections.swap(reordered, i, i - 1);
                expect(reordered, false, "reorder " + type);
            }
        }
        List<ObjectNode> late = copy(valid); late.add(event(6000, "recv", "unknown", object()));
        expect(late, false, "external observation is final");
        for (String type : List.of("window.create", "webview.loaded", "command", "capabilityResult", "opaqueHttpStreamOpen",
                "opaquePush", "sidecar.recoveryScheduled", "sidecar.replacementTerminated", "sidecar.startFailed",
                "sidecar.shutdownFailed", "host.sidecarShutdownFailed", "host.failureShutdownFailed", "applicationWillTerminate",
                "hostReadyTimeout.unexpectedReceipt")) {
            List<ObjectNode> forbidden = copy(valid); forbidden.add(1, event(1001, "recv", type, object()));
            expect(forbidden, false, "forbidden " + type);
        }
        List<ObjectNode> javaReady = copy(valid);
        javaReady.add(5, event(1005, "recv", "ready", object("source", "java-sidecar", "envelope", object())));
        expect(javaReady, false, "Java ready before timeout");
        List<ObjectNode> replacedReady = copy(valid);
        replacedReady.set(1, event(1001, "recv", "ready", object("source", "java-sidecar", "envelope", object())));
        expect(replacedReady, false, "Java ready cannot replace host ready");
        expect(List.of(valid.getLast().deepCopy()), false, "external assertion without actual lifecycle");
    }

    private static void identityAndOutcome(List<ObjectNode> valid, String mode) {
        for (String type : List.of("sidecar.started", "sidecar.initialReadyDeadline", "error", "host.failureShutdownStarted",
                "sidecar.exit", EXTERNAL)) {
            int row = find(valid, type);
            mutate(valid, row, e -> payload(e).put("processIdentifier", 999), "wrong PID " + type);
            mutate(valid, row, e -> payload(e).put("processIdentifier", PID + 0.5), "fractional PID " + type);
        }
        for (String type : List.of("launch.sidecar", "sidecar.started", "sidecar.exit")) {
            mutate(valid, find(valid, type), e -> payload(e).put("generation", 2), "wrong generation " + type);
        }
        int failure = find(valid, "host.failureShutdownStarted"), external = find(valid, EXTERNAL);
        mutate(valid, failure, e -> payload(e).put("hostProcessIdentifier", PID), "host and child cannot be same PID");
        mutate(valid, external, e -> payload(e).put("hostProcessIdentifier", 999), "external wrong host identity");
        mutate(valid, external, e -> payload(e).put("runId", "not-a-uuid"), "invalid fixture run identity");
        mutate(valid, external, e -> payload(e).put("mode", mode.equals("blocked") ? "cooperative" : "blocked"), "mode swapped after exit");
        for (String field : List.of("hostAbsent", "sidecarAbsent", "markersMatched")) {
            mutate(valid, external, e -> payload(e).put(field, false), "external false " + field);
        }
        mutate(valid, external, e -> payload(e).put("cleanupPerformed", true), "cleanup cannot rescue success");
        mutate(valid, external, e -> payload(e).put("schemaVersion", 2), "unsupported external schema");
        mutate(valid, find(valid, "sidecar.exit"), e -> payload(e).put("expected", false), "unexpected crash is not retirement");
        mutate(valid, find(valid, "sidecar.exit"), e -> payload(e).put("status", mode.equals("blocked") ? 0 : 9), "wrong exit status");
        mutate(valid, find(valid, "sidecar.exit"), e -> payload(e).put("reason", mode.equals("blocked") ? 1 : 2), "wrong exit reason");
        mutate(valid, find(valid, "shutdown.sidecar"), e -> payload(e).put("mode", "forced-after-timeout"), "old signal-only completion rejected");
        mutate(valid, find(valid, "host.failureShutdownCompleted"), e -> payload(e).put("exitStatus", 0), "fatal failure cannot exit zero");
        mutate(valid, find(valid, "host.nativeResourceDrainCompleted"), e -> payload(e).put("drained", false), "undrained native resources");
        mutate(valid, find(valid, "host.nativeResourceDrainCompleted"), e -> payload(e).put("failureCount", 1), "cleanup error");
        mutate(valid, find(valid, "error"), e -> payload(e).put("code", "sidecarUnexpectedExit"), "wrong fatal failure");
        for (int delay : List.of(0, 100, 6000)) mutate(valid, find(valid, "error"),
                e -> e.put("at", valid.get(find(valid, "sidecar.initialReadyDeadline")).path("at").asLong() + delay),
                "invalid initial deadline " + delay);
        if (mode.equals("blocked")) {
            for (String type : List.of("sidecar.shutdownTerminated", "sidecar.shutdownEscalated")) {
                mutate(valid, find(valid, type), e -> payload(e).put("processIdentifier", 999), "signal targets foreign child " + type);
                mutate(valid, find(valid, type), e -> payload(e).put("generation", 2), "signal targets new generation " + type);
                mutate(valid, find(valid, type), e -> payload(e).put("graceSeconds", 0), "missing grace " + type);
            }
            int term = find(valid, "sidecar.shutdownTerminated"), kill = find(valid, "sidecar.shutdownEscalated");
            for (int delay : List.of(0, 100, 6000)) {
                mutate(valid, term, e -> e.put("at", valid.get(find(valid, "shutdown")).path("at").asLong() + delay), "invalid channel grace " + delay);
                mutate(valid, kill, e -> e.put("at", valid.get(term).path("at").asLong() + delay), "invalid TERM grace " + delay);
            }
            mutate(valid, find(valid, "sidecar.exit"), e -> e.put("at", valid.get(kill).path("at").asLong() - 1), "exit before KILL timestamp");
        } else {
            List<ObjectNode> forced = copy(valid);
            forced.add(find(valid, "sidecar.exit"), event(3000, "send", "sidecar.shutdownTerminated",
                    object("generation", 1, "processIdentifier", PID, "graceSeconds", 1)));
            expect(forced, false, "cooperative run unexpectedly forced");
        }
    }

    private static List<ObjectNode> valid(String mode) {
        List<ObjectNode> events = new ArrayList<>();
        events.add(event(1000, "recv", "launch", object("host", "swift-appkit-wkwebview", "transport", "flow-ipc")));
        events.add(event(1001, "send", "ready", object("host", "swift-appkit-wkwebview", "protocolVersion", 1, "transport", "flow-ipc")));
        events.add(event(1002, "recv", "capabilityManifest", object("path", "$APP/Contents/Resources/desktop-capabilities.json",
                "loaded", true, "permissionMode", "manifest-enforced", "commandsByWindow", object(), "parseError", "")));
        events.add(event(1003, "send", "launch.sidecar", object("generation", 1, "transport", "flow-ipc",
                "jar", "$APP/Contents/Resources/Sidecar/application.jar", "command", "java -jar $APP/Contents/Resources/Sidecar/application.jar")));
        events.add(event(1004, "recv", "sidecar.started", object("generation", 1, "processIdentifier", PID, "transport", "flow-ipc")));
        events.add(event(1005, "recv", "sidecar.initialReadyDeadline", object("processIdentifier", PID, "timeoutSeconds", 1)));
        events.add(event(2005, "send", "error", object("code", "sidecarInitialReadyTimeout", "processIdentifier", PID, "timeoutSeconds", 1)));
        events.add(event(2006, "recv", "host.failureShutdownStarted", object("processIdentifier", PID, "hostProcessIdentifier", HOST_PID)));
        events.add(event(2007, "recv", "host.nativeResourceDrainStarted", object("pendingCount", 0, "timeoutSeconds", 2)));
        events.add(event(2008, "recv", "host.nativeResourceDrainCompleted", object("drained", true, "pendingCount", 0, "failureCount", 0)));
        events.add(event(2009, "send", "shutdown", object("target", "java-sidecar", "transport", "flow-ipc", "reason", "host-terminate")));
        boolean blocked = mode.equals("blocked");
        if (blocked) {
            events.add(event(3009, "send", "sidecar.shutdownTerminated", object("generation", 1, "processIdentifier", PID, "graceSeconds", 1.0)));
            events.add(event(4009, "send", "sidecar.shutdownEscalated", object("generation", 1, "processIdentifier", PID, "graceSeconds", 1.0)));
        }
        long finished = blocked ? 4010 : 2010;
        events.add(event(finished, "recv", "sidecar.exit", object("generation", 1, "processIdentifier", PID,
                "status", blocked ? 9 : 0, "reason", blocked ? 2 : 1, "expected", true)));
        events.add(event(finished + 1, "recv", "shutdown.sidecar", object("mode", blocked ? "killed" : "graceful")));
        events.add(event(finished + 2, "recv", "host.sidecarShutdownCompleted", object("succeeded", true)));
        events.add(event(finished + 3, "recv", "host.failureShutdownCompleted", object("exitStatus", 1)));
        events.add(event(finished + 4, "recv", EXTERNAL, object("schemaVersion", 1, "runId", "af23d7c9-49f6-4dc7-a2a5-e8f111cb24a2",
                "mode", mode, "processIdentifier", PID, "hostProcessIdentifier", HOST_PID, "hostAbsent", true,
                "sidecarAbsent", true, "markersMatched", true, "cleanupPerformed", false)));
        return events;
    }

    private static void mutate(List<ObjectNode> valid, int row, Consumer<ObjectNode> change, String label) {
        List<ObjectNode> changed = copy(valid); change.accept(changed.get(row)); expect(changed, false, label);
    }
    private static void expect(List<ObjectNode> events, boolean success, String label) {
        HostReadyTimeoutTraceGate gate = new HostReadyTimeoutTraceGate(); events.forEach(gate::accept);
        List<String> missing = new ArrayList<>(); gate.require(missing);
        if (missing.isEmpty() != success) throw new AssertionError(label + " expected " + success + ": " + missing);
        if (success) positives++; else negatives++;
    }
    private static List<ObjectNode> copy(List<ObjectNode> original) { return new ArrayList<>(original.stream().map(ObjectNode::deepCopy).toList()); }
    private static int find(List<ObjectNode> events, String type) { for (int i = 0; i < events.size(); i++) if (type(events.get(i)).equals(type)) return i; throw new AssertionError(type); }
    private static String type(ObjectNode event) { return text(event, "type"); }
    private static String text(ObjectNode event, String field) { return event.path(field).asString(); }
    private static ObjectNode payload(ObjectNode event) { return (ObjectNode) event.path("payload"); }
    private static ObjectNode event(long at, String direction, String type, ObjectNode payload) {
        return object("at", at, "direction", direction, "type", type, "payload", payload);
    }
    private static ObjectNode object(Object... pairs) {
        ObjectNode object = JSON.createObjectNode();
        for (int i = 0; i < pairs.length; i += 2) object.set((String) pairs[i], JSON.valueToTree(pairs[i + 1]));
        return object;
    }
}
