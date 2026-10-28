package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Synthetic gate checks, not evidence of AppKit, JVM hooks or external process observation. */
public final class HostShutdownTerminationTraceGateSmokeMain {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PREFIX = "flowHostShutdownTermination.";
    private static final String RUN = "b30a9069-eaf7-46a2-8993-11fe8e95be71";
    private static final String MANIFEST = "$APP/Contents/Resources/desktop-capabilities.json";
    private static final long PID = 111, HOST_PID = 222;
    private static final List<String> failures = new ArrayList<>();
    private static final Set<String> FRACTIONAL = Set.of("documentTimeOriginMillis", "webViewX", "webViewY", "windowX", "windowY", "dispatchedAt");
    private static int positives, negatives;

    public static void main(String[] args) {
        for (String mode : List.of("cooperative", "blocked")) {
            List<ObjectNode> valid = valid(mode);
            expect(valid, true, mode + " actual-exit chain and final independent receipt");
            List<ObjectNode> implicit = copy(valid);
            for (ObjectNode event : implicit) if (type(event).startsWith("opaque") && text(event, "direction").equals("send")) {
                payload(event).remove("sidecarGeneration");
            }
            expect(implicit, true, mode + " outbound scope in the single exact live generation");
            retryCases(valid);
            dropAndDuplicate(valid);
            focusedSchema(valid);
            lifecycle(valid, mode);
            transport(valid);
            capability(valid);
        }
        if (!failures.isEmpty()) throw new AssertionError("Host shutdown termination gate failures (" + failures.size() + "):\n" + String.join("\n", failures));
        System.out.printf("Host shutdown termination trace gate smoke passed: %d positives, %d rejections%n", positives, negatives);
    }

    private static void dropAndDuplicate(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            List<ObjectNode> missing = copy(valid); missing.remove(i);
            expect(missing, false, "drop " + type(valid.get(i)) + " / " + i);
            if (!type(valid.get(i)).endsWith("Credit")) {
                List<ObjectNode> duplicate = copy(valid); duplicate.add(i, duplicate.get(i).deepCopy());
                expect(duplicate, false, "duplicate " + type(valid.get(i)) + " / " + i);
            }
        }
    }

    private static void focusedSchema(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            ObjectNode event = valid.get(i); if (!type(event).startsWith(PREFIX)) continue;
            int row = i;
            for (String field : List.copyOf(payload(event).propertyNames())) {
                String label = type(event) + "/" + field;
                mutate(valid, row, e -> payload(e).remove(field), "missing " + label);
                mutate(valid, row, e -> payload(e).putNull(field), "null " + label);
                mutate(valid, row, e -> payload(e).set(field, object()), "object " + label);
                mutate(valid, row, e -> payload(e).set(field, JSON.createArrayNode()), "array " + label);
                mutate(valid, row, e -> payload(e).put(field, "wrong"), "string " + label);
                JsonNode original = payload(event).path(field);
                if (original.isBoolean()) mutate(valid, row, e -> payload(e).put(field, !original.asBoolean()), "inverted " + label);
                else if (original.isNumber()) {
                    mutate(valid, row, e -> payload(e).put(field, true), "boolean number " + label);
                    mutate(valid, row, e -> payload(e).put(field, -1), "negative number " + label);
                    if (!FRACTIONAL.contains(field)) mutate(valid, row, e -> payload(e).put(field, original.asDouble() + 0.5), "fractional integer " + label);
                } else mutate(valid, row, e -> payload(e).put(field, true), "boolean string " + label);
            }
            mutate(valid, row, e -> e.put("direction", "send"), "focused direction");
            mutate(valid, row, e -> payload(e).put("unexpected", true), "unknown focused field");
            mutate(valid, row, e -> e.put("type", PREFIX + "forgedAcceptance"), "unknown focused event");
        }
        List<ObjectNode> afterExternal = copy(valid);
        afterExternal.add(event(100_000, "recv", "late-unowned-event", object()));
        expect(afterExternal, false, "external receipt must be final");
        expect(List.of(valid.get(valid.size() - 1).deepCopy()), false, "external assertion without actual process lifecycle");
    }

    private static void lifecycle(List<ObjectNode> valid, String mode) {
        for (String name : List.of("launch.sidecar", "sidecar.started", "window.create", "webview.loaded", "window.closeRequested",
                "windowClosed", "host.nativeResourceDrainStarted", "host.nativeResourceDrainCompleted", "shutdown", "sidecar.exit",
                "shutdown.sidecar", "host.sidecarShutdownCompleted", "sidecar.shutdownTerminated", "sidecar.shutdownEscalated")) {
            for (int row : indices(valid, name)) {
                for (String field : List.copyOf(payload(valid.get(row)).propertyNames())) {
                    mutate(valid, row, e -> payload(e).remove(field), "missing lifecycle " + name + "/" + field);
                    mutate(valid, row, e -> payload(e).put(field, "wrong"), "malformed lifecycle " + name + "/" + field);
                }
                mutate(valid, row, e -> e.put("direction", "wrong"), "lifecycle direction " + name);
            }
        }
        List<String> ordered = new ArrayList<>(List.of("capabilityManifest", "launch.sidecar", "sidecar.started", "ready", "window.create",
                "webview.loaded", PREFIX + "prepared", "window.closeRequested", "windowClosed", "host.nativeResourceDrainStarted",
                "host.nativeResourceDrainCompleted", "shutdown"));
        if (mode.equals("blocked")) ordered.addAll(List.of("sidecar.shutdownTerminated", "sidecar.shutdownEscalated"));
        ordered.addAll(List.of("sidecar.exit", "shutdown.sidecar", "host.sidecarShutdownCompleted", PREFIX + "applicationWillTerminate", PREFIX + "externalExitObserved"));
        for (int i = 1; i < ordered.size(); i++) reorder(valid, find(valid, ordered.get(i), 0), find(valid, ordered.get(i - 1), 0), "reordered lifecycle " + ordered.get(i));
        int exit = find(valid, "sidecar.exit", 0);
        mutate(valid, exit, e -> payload(e).put("processIdentifier", HOST_PID), "actual exit targets host not child");
        mutate(valid, exit, e -> payload(e).put("generation", 2), "actual exit wrong generation");
        mutate(valid, exit, e -> payload(e).put("status", mode.equals("blocked") ? 0 : 9), "wrong mode-specific actual status");
        mutate(valid, exit, e -> payload(e).put("expected", false), "unexpected crash is not successful close");
        if (mode.equals("blocked")) {
            for (String signal : List.of("sidecar.shutdownTerminated", "sidecar.shutdownEscalated")) {
                int row = find(valid, signal, 0);
                mutate(valid, row, e -> payload(e).put("processIdentifier", 999), "signal stale exact PID");
                mutate(valid, row, e -> e.remove("at"), "signal missing timestamp");
                mutate(valid, row, e -> e.put("at", "1000"), "signal timestamp type");
                mutate(valid, row, e -> e.put("at", 1), "signal clock before grace");
            }
            int term = find(valid, "sidecar.shutdownTerminated", 0), kill = find(valid, "sidecar.shutdownEscalated", 0);
            for (int elapsed : List.of(0, 100, 6000)) mutate(valid, kill,
                    e -> e.put("at", valid.get(term).path("at").asLong() + elapsed), "invalid TERM grace " + elapsed);
        } else {
            List<ObjectNode> signal = copy(valid);
            signal.add(exit, event(1000, "send", "sidecar.shutdownTerminated", object("generation", 1, "processIdentifier", PID, "graceSeconds", 1.0)));
            expect(signal, false, "cooperative path must not use TERM");
        }
        for (String type : List.of("error", "webview.error", "sidecar.shutdownFailed", "host.sidecarShutdownFailed", "sidecar.recoveryScheduled",
                "sidecar.replacementRequested", "opaquePushResync", "opaquePushDisconnect", "opaqueHttpStreamCancel", PREFIX + "unexpectedErrorObserved")) {
            List<ObjectNode> broken = copy(valid); broken.add(1, event(1100, "recv", type, object()));
            expect(broken, false, "forbidden " + type);
        }
    }

    private static void retryCases(List<ObjectNode> valid) {
        List<ObjectNode> retry = copy(valid);
        int prepared = find(retry, PREFIX + "prepared", 0);
        ObjectNode push = retry.get(find(retry, "opaquePush", 0)).deepCopy(), ack = retry.get(find(retry, "opaquePushAck", 0)).deepCopy();
        body(retry.get(find(retry, "opaquePushAck", 0))).put("lastSeenServerSyncId", 1);
        body(push).put("deliveryAttempt", 2);
        retry.add(prepared + 1, push); retry.add(prepared + 2, ack);
        expect(retry, true, "low transport ACK followed by consumed same-update retry before final click");
        mutate(retry, prepared + 1, e -> body(e).put("deliveryAttempt", 3), "retry attempt gap");
        mutate(retry, prepared + 1, e -> body(e).put("deliveryAttempt", 1), "retry duplicate attempt");
        mutate(retry, prepared + 1, e -> body(e).put("uidlBytes", 26), "retry changes payload length");
        mutate(retry, prepared + 1, e -> body(e).put("flowServerSyncId", 3), "retry changes sync identity");
        for (int lastSeen : List.of(-1, 1)) {
            List<ObjectNode> low = copy(retry);
            for (ObjectNode e : low) if (type(e).equals("opaquePushAck")) body(e).put("lastSeenServerSyncId", lastSeen);
            expect(low, false, "no consumed ACK in whole logical group");
        }
        List<ObjectNode> missing = copy(retry); missing.remove(prepared + 2);
        expect(missing, false, "missing retry ACK");
        reorder(retry, find(retry, PREFIX + "nativePointerDispatch", 1), prepared + 2, "close before consumed retry ACK");
        reorder(retry, prepared + 1, find(retry, "opaquePushAck", 0), "retry before prior ordinal ACK");
    }

    private static void transport(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            ObjectNode event = valid.get(i); if (!payload(event).has("envelope")) continue;
            int row = i;
            for (String key : List.of("version", "type", "id", "correlationId", "windowId", "sessionId", "uiId", "error")) {
                mutate(valid, row, e -> envelope(e).remove(key), "wire missing " + type(event) + "/" + key);
                mutate(valid, row, e -> envelope(e).put(key, true), "wire malformed " + type(event) + "/" + key);
            }
            mutate(valid, row, e -> payload(e).put("sidecarGeneration", 2), "wire stale generation");
            mutate(valid, row, e -> payload(e).put("transport", "compatibility-http"), "wrong transport");
            mutate(valid, row, e -> e.put("direction", "wrong"), "wire wrong direction");
            if (type(event).startsWith("opaqueHttp")) {
                mutate(valid, row, e -> body(e).put("requestId", "orphan"), "wire orphan request");
                if (type(event).endsWith("Chunk")) {
                    for (String key : List.of("chunkSequence", "bodyBytes", "bodyRedacted")) mutate(valid, row, e -> body(e).remove(key), "chunk missing " + key);
                    mutate(valid, row, e -> body(e).put("bodyBase64", "cHJpdmF0ZQ=="), "unredacted content");
                    mutate(valid, row, e -> body(e).put("bodyBytes", 99), "chunk bytes differ from terminal");
                }
                if (type(event).endsWith("Credit")) mutate(valid, row, e -> body(e).put("credit", 0), "no stream credit");
            }
        }
        int push = find(valid, "opaquePush", 0), ack = find(valid, "opaquePushAck", 0);
        for (String field : List.of("connectionId", "flowUiId", "pushSequence", "flowServerSyncId", "deliveryAttempt", "uidlBytes", "uidlRedacted", "resync")) {
            mutate(valid, push, e -> body(e).remove(field), "push missing " + field);
            mutate(valid, push, e -> body(e).put(field, "wrong"), "push malformed " + field);
        }
        for (String field : List.of("connectionId", "flowUiId", "pushSequence", "flowServerSyncId", "lastSeenServerSyncId")) {
            mutate(valid, ack, e -> body(e).remove(field), "ACK missing " + field);
            mutate(valid, ack, e -> body(e).put(field, "wrong"), "ACK malformed " + field);
        }
        mutate(valid, ack, e -> body(e).put("lastSeenServerSyncId", -1), "unconsumed update ACK");
        mutate(valid, push, e -> body(e).put("uidl", "private"), "raw UIDL");
        List<ObjectNode> gap = copy(valid); body(gap.get(push)).put("pushSequence", 99); body(gap.get(ack)).put("pushSequence", 99);
        expect(gap, false, "matching ACK cannot hide logical sequence gap");
        reorder(valid, ack, push, "ACK before actual push");
        List<ObjectNode> missingChunks = copy(valid); missingChunks.removeIf(e -> type(e).equals("opaqueHttpStreamResponseChunk"));
        expect(missingChunks, false, "all response chunks omitted");
        for (String id : List.of("prepare", "final")) {
            List<ObjectNode> emptyPost = copy(valid);
            emptyPost.removeIf(e -> type(e).equals("opaqueHttpStreamChunk") && text(body(e), "requestId").equals(id));
            for (ObjectNode event : emptyPost) {
                if (type(event).equals("opaqueHttpStreamOpen") && text(body(event), "requestId").equals(id)) body(event).put("contentLength", 0);
                if (type(event).equals("opaqueHttpStreamEnd") && text(body(event), "requestId").equals(id)) body(event).put("chunkCount", 0).put("bodyBytes", 0);
            }
            expect(emptyPost, false, "self-consistent empty " + id + " POST cannot prove a listener input");
        }
        List<ObjectNode> emptyResponse = copy(valid);
        emptyResponse.removeIf(e -> type(e).equals("opaqueHttpStreamResponseChunk") && text(body(e), "requestId").equals("prepare"));
        for (ObjectNode event : emptyResponse) if (type(event).equals("opaqueHttpStreamResponseEnd") && text(body(event), "requestId").equals("prepare")) {
            body(event).put("chunkCount", 0).put("bodyBytes", 0);
        }
        expect(emptyResponse, false, "self-consistent empty prepare response cannot prove the Java update");
        int prepared = find(valid, PREFIX + "prepared", 0);
        mutate(valid, prepared, e -> payload(e).put("requestBaseline", 0), "forged baseline");
        int ready = find(valid, "ready", 0);
        for (String key : List.of("runtime", "transport", "uidlConsumer", "browserRequestTransport")) mutate(valid, ready, e -> body(e).put(key, "other"), "ready wrong " + key);
        reorder(valid, find(valid, "opaqueHttpStreamOpen", 2), prepared, "final request before final native input");
        reorder(valid, find(valid, "opaqueHttpStreamEnd", 2), find(valid, "command", 0) + 1, "title before final request submission");
    }

    private static void capability(List<ObjectNode> valid) {
        int manifest = find(valid, "capabilityManifest", 0);
        mutate(valid, manifest, e -> payload(e).put("permissionMode", "allow-all-dev"), "permissive manifest");
        mutate(valid, manifest, e -> ((ObjectNode) payload(e).path("commandsByWindow")).set("secondary", JSON.createArrayNode()), "additional manifest owner");
        int title = find(valid, "command", 0), result = find(valid, "capabilityResult", 0);
        mutate(valid, title, e -> ((ObjectNode) body(e).path("args")).put("title", "forged trusted=true"), "forged trusted title");
        mutate(valid, result, e -> body(e).put("permissionManifestCommands", "window.close"), "different permission snapshot");
        mutate(valid, result, e -> body(e).put("success", false), "failed native title");
        mutate(valid, result, e -> body(e).put("error", "masked"), "error contradicts successful title");
        int closed = find(valid, "capabilityResult", 1);
        mutate(valid, closed, e -> body(e).put("windowId", "secondary"), "wrong close target");
        mutate(valid, closed, e -> body(e).put("permissionDecision", "denied"), "denied close cannot count");
        reorder(valid, result, title, "result before title command");
        reorder(valid, find(valid, "command", 1), result, "close before successful title receipt");
    }

    static List<ObjectNode> valid(String mode) {
        Builder b = new Builder(mode);
        b.add("recv", "capabilityManifest", object("loaded", true, "path", MANIFEST, "permissionMode", "manifest-enforced", "parseError", "",
                "commandsByWindow", object("main", JSON.createArrayNode().add("window.close").add("window.setTitle"))));
        b.add("send", "launch.sidecar", object("generation", 1, "transport", "flow-ipc", "jar", "$APP/Contents/Resources/Sidecar/application.jar"));
        b.add("recv", "sidecar.started", object("generation", 1, "transport", "flow-ipc", "processIdentifier", PID));
        b.wire("ready", "ready", object("runtime", "standard-vaadin-bundle-opaque-ipc", "transport", "flow-ipc", "uidlConsumer", "standard-flow-client", "browserRequestTransport", "opaque-http-stream-ipc"));
        b.add("send", "window.create", scope().put("label", "main").put("mode", "flow-ipc"));
        b.add("recv", "webview.loaded", scope().put("recoveryGeneration", 0));
        b.request("bootstrap", true);
        b.pointer(0);
        b.request("prepare", true);
        b.wire("opaquePush", "push", object("connectionId", "connection", "flowUiId", 0, "pushSequence", 1,
                "flowServerSyncId", 2, "deliveryAttempt", 1, "uidlBytes", 25, "uidlRedacted", true, "resync", false));
        b.wire("opaquePushAck", "push", object("connectionId", "connection", "flowUiId", 0, "pushSequence", 1, "flowServerSyncId", 2, "lastSeenServerSyncId", 2));
        b.add("recv", PREFIX + "prepared", b.identity().put("documentTimeOriginMillis", 1000.0).put("mainWindowNumber", 101)
                .put("routeReady", true).put("firstMeaningfulPaint", true).put("flowClientCount", 1).put("mainContextRetained", true)
                .put("mainWindowRetained", true).put("mainWebViewRetained", true).put("counter", 1).put("pushCount", 1)
                .put("client", true).put("trusted", true).put("backgroundSessionAbsent", true).put("requestBaseline", 1)
                .put("responseBaseline", 1).put("pushBaseline", 0).put("requestCount", 2).put("responseCount", 2)
                .put("pushMessageCount", 1).put("nativePointerReceiptCount", 1).put("nativePointerTrustedCount", 1).put("balancedDrainObserved", true));
        b.pointer(1);
        b.request("final", false);
        String title = "Host shutdown close: runId=" + RUN + " pid=" + PID + " mode=" + mode + " trusted=true closeCount=1";
        b.wire("command", "title", object("command", "window.setTitle", "args", object("title", title)));
        b.wire("capabilityResult", "title", object("command", "window.setTitle", "success", true, "title", title, "mode", "native-appkit",
                "permissionMode", "manifest-enforced", "permissionDecision", "allowed", "permissionReason", "manifest-allow",
                "permissionScope", "window:main", "permissionManifestLoaded", true, "permissionManifestPath", MANIFEST,
                "permissionManifestCommands", "window.close,window.setTitle"));
        b.wire("command", "close", object("command", "window.close", "args", object("id", "main")));
        b.wire("capabilityResult", "close", object("command", "window.close", "success", true, "windowId", "main", "permissionDecision", "allowed", "permissionReason", "manifest-allow"));
        b.add("recv", "window.closeRequested", scope().put("label", "main"));
        b.add("send", "windowClosed", scope().put("target", "java-sidecar").put("transport", "flow-ipc"));
        b.add("recv", "host.nativeResourceDrainStarted", object("pendingCount", 0, "timeoutSeconds", 2.0));
        b.add("recv", "host.nativeResourceDrainCompleted", object("drained", true, "pendingCount", 0, "failureCount", 0));
        b.add("send", "shutdown", object("target", "java-sidecar", "transport", "flow-ipc", "reason", "host-terminate"));
        boolean blocked = mode.equals("blocked");
        if (blocked) {
            b.clock += 900;
            b.add("send", "sidecar.shutdownTerminated", object("generation", 1, "processIdentifier", PID, "graceSeconds", 1.0));
            b.clock += 900;
            b.add("send", "sidecar.shutdownEscalated", object("generation", 1, "processIdentifier", PID, "graceSeconds", 1.0));
        }
        b.add("recv", "sidecar.exit", object("generation", 1, "processIdentifier", PID, "status", blocked ? 9 : 0, "reason", blocked ? 2 : 1, "expected", true));
        b.add("recv", "shutdown.sidecar", object("mode", blocked ? "killed" : "graceful"));
        b.add("recv", "host.sidecarShutdownCompleted", object("succeeded", true));
        b.add("recv", PREFIX + "applicationWillTerminate", b.identity().put("complete", true).put("preparedObserved", true)
                .put("finalPointerDispatched", true).put("terminationSource", "applicationWillTerminate").put("currentProcessIdentifier", 0)
                .put("originalSidecarAbsent", true).put("originalWindowRetained", false).put("launcherReturnedMarkerMatched", true).put("hookEnteredMarkerMatched", true));
        b.add("recv", PREFIX + "externalExitObserved", b.identity().put("schemaVersion", 1).put("hostAbsent", true)
                .put("sidecarAbsent", true).put("markersMatched", true).put("cleanupPerformed", false));
        return b.events;
    }

    private static final class Builder {
        final List<ObjectNode> events = new ArrayList<>(); final String mode; long clock = 1000;
        Builder(String mode) { this.mode = mode; }
        void add(String direction, String type, ObjectNode payload) { events.add(event(clock, direction, type, payload)); clock += 100; }
        ObjectNode identity() { return scope().put("runId", RUN).put("mode", mode).put("processIdentifier", PID).put("hostProcessIdentifier", HOST_PID).put("recoveryGeneration", 0); }
        void pointer(int index) { add("recv", PREFIX + "nativePointerDispatch", identity().put("stage", index == 0 ? "prepare" : "close-last")
                .put("index", index).put("inputMode", "appkit-window-pointer-event").put("button", "primary").put("activeApplication", true)
                .put("keyWindow", true).put("webViewX", 40.0).put("webViewY", 80.0).put("windowX", 40.0).put("windowY", 680.0).put("dispatchedAt", (double) clock)); }
        void request(String id, boolean response) {
            int bytes = id.equals("bootstrap") ? 0 : 16;
            wire("opaqueHttpStreamOpen", id, object("requestId", id, "method", bytes == 0 ? "GET" : "POST", "contentLength", bytes));
            if (bytes > 0) {
                wire("opaqueHttpStreamRequestCredit", id, object("requestId", id, "credit", 1));
                wire("opaqueHttpStreamChunk", id, object("requestId", id, "chunkSequence", 0, "bodyBytes", bytes, "bodyRedacted", true));
            }
            wire("opaqueHttpStreamEnd", id, object("requestId", id, "chunkCount", bytes == 0 ? 0 : 1, "bodyBytes", bytes));
            if (response) {
                wire("opaqueHttpStreamResponse", id, object("requestId", id, "status", 200));
                wire("opaqueHttpStreamResponseCredit", id, object("requestId", id, "credit", 1));
                wire("opaqueHttpStreamResponseChunk", id, object("requestId", id, "chunkSequence", 0, "bodyBytes", 8, "bodyRedacted", true));
                wire("opaqueHttpStreamResponseEnd", id, object("requestId", id, "chunkCount", 1, "bodyBytes", 8));
            }
        }
        void wire(String type, String correlation, ObjectNode body) {
            boolean send = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamEnd", "opaqueHttpStreamChunk", "opaqueHttpStreamResponseCredit", "opaquePushAck", "capabilityResult").contains(type);
            ObjectNode envelope = scope().put("version", 1).put("type", type).put("id", type.equals("capabilityResult") ? "capability-" + correlation : correlation)
                    .put("correlationId", correlation).putNull("error"); envelope.set("payload", body);
            ObjectNode wrapper = object(send ? "target" : "source", "java-sidecar", "transport", "flow-ipc", "sidecarGeneration", 1, "envelope", envelope);
            if (type.equals("capabilityResult")) wrapper.put("command", text(body, "command"));
            add(send ? "send" : "recv", type, wrapper);
        }
    }

    private static ObjectNode event(long at, String direction, String type, ObjectNode payload) { return object("at", at, "direction", direction, "type", type, "payload", payload); }
    private static ObjectNode scope() { return object("windowId", "main", "sessionId", "flow-ipc-session", "uiId", "main"); }
    private static ObjectNode object(Object... fields) {
        ObjectNode value = JSON.createObjectNode();
        for (int i = 0; i < fields.length; i += 2) {
            String key = (String) fields[i]; Object v = fields[i + 1];
            if (v instanceof String s) value.put(key, s); else if (v instanceof Integer n) value.put(key, n);
            else if (v instanceof Long n) value.put(key, n); else if (v instanceof Double n) value.put(key, n);
            else if (v instanceof Boolean b) value.put(key, b); else if (v instanceof JsonNode node) value.set(key, node);
            else throw new IllegalArgumentException("Unsupported synthetic value");
        }
        return value;
    }
    private static String text(JsonNode p, String key) { return p.path(key).isString() ? p.path(key).asString() : ""; }
    private static String type(ObjectNode e) { return text(e, "type"); }
    private static ObjectNode payload(ObjectNode e) { return (ObjectNode) e.path("payload"); }
    private static ObjectNode envelope(ObjectNode e) { return (ObjectNode) payload(e).path("envelope"); }
    private static ObjectNode body(ObjectNode e) { return (ObjectNode) envelope(e).path("payload"); }
    private static List<ObjectNode> copy(List<ObjectNode> input) { return new ArrayList<>(input.stream().map(ObjectNode::deepCopy).toList()); }
    private static int find(List<ObjectNode> input, String type, int ordinal) { for (int i = 0; i < input.size(); i++) if (type.equals(type(input.get(i))) && ordinal-- == 0) return i; throw new IllegalArgumentException("Missing " + type); }
    private static List<Integer> indices(List<ObjectNode> input, String type) { List<Integer> rows = new ArrayList<>(); for (int i = 0; i < input.size(); i++) if (type.equals(type(input.get(i)))) rows.add(i); return rows; }
    private static void mutate(List<ObjectNode> valid, int row, Consumer<ObjectNode> action, String label) { List<ObjectNode> changed = copy(valid); action.accept(changed.get(row)); if (changed.equals(valid)) throw new AssertionError("No-op mutation " + label); expect(changed, false, label); }
    private static void reorder(List<ObjectNode> valid, int from, int to, String label) { List<ObjectNode> changed = copy(valid); changed.add(to, changed.remove(from)); expect(changed, false, label); }
    private static void expect(List<ObjectNode> input, boolean pass, String label) {
        HostShutdownTerminationTraceGate gate = new HostShutdownTerminationTraceGate(); List<String> missing = new ArrayList<>();
        try { input.forEach(gate::accept); gate.require(missing); }
        catch (RuntimeException e) { failures.add(label + ": gate threw " + e.getClass().getSimpleName()); return; }
        if (missing.isEmpty() != pass) failures.add(label + (pass ? ": rejected " + missing : ": unexpectedly accepted"));
        else if (pass) positives++; else negatives++;
    }
}
