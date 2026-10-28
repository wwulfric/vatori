package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Helper-only synthetic evidence, not real JVM termination, AppKit or durable-state validation. */
public final class RecoveryTerminationTraceGateSmokeMain {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String PREFIX = "flowRecoveryTermination.";
    private static final String RUN = "f128acfe-bf33-4ccd-a416-292886f09bc7";
    private static final String FIRST_DOCUMENT = "0613995a-c259-49b0-a902-4b774a23cf5a";
    private static final String LAST_DOCUMENT = "35357813-cf56-4be3-92cf-d7e85cbf7455";
    private static final long FIRST_PID = 111, BLOCKED_PID = 222, LAST_PID = 333;
    private static final Set<String> WIRE = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamEnd",
            "opaqueHttpStreamRequestCredit", "opaqueHttpStreamChunk", "opaqueHttpStreamResponse",
            "opaqueHttpStreamResponseCredit", "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseEnd",
            "opaquePush", "opaquePushAck");
    private static final Set<String> FRACTIONAL = Set.of("documentTimeOriginMillis",
            "initialDocumentTimeOriginMillis", "webViewX", "webViewY", "windowX", "windowY", "dispatchedAt");
    private static final List<String> ACCEPTANCE_TRUE = List.of("complete", "initialJavaListenerObserved",
            "recoveredJavaListenerObserved", "initialUidlObserved", "recoveredUidlObserved", "initialPushObserved",
            "recoveredPushObserved", "blockedShutdownHookObserved", "blockedProcessAbsent", "newDocumentObserved",
            "balancedDrainObserved", "resultStableAfterDrain", "errorFree");
    private static int positive, rejected;
    private static final List<String> failures = new ArrayList<>();

    private RecoveryTerminationTraceGateSmokeMain() { }

    public static void main(String[] args) {
        List<ObjectNode> valid = valid();
        expect(valid, true, "three generations, blocked TERM, bounded escalation and recovered Flow push");
        List<ObjectNode> finalExit = copy(valid);
        finalExit.add(object("at", 100_000L, "direction", "recv", "type", "sidecar.exit", "payload",
                object("generation", 3, "processIdentifier", LAST_PID, "status", 0, "reason", 1, "expected", true)));
        expect(finalExit, true, "optional final actual graceful exit");
        List<ObjectNode> implicitGeneration = copy(valid);
        for (ObjectNode e : implicitGeneration) {
            if (WIRE.contains(type(e)) && "send".equals(e.path("direction").asString())) {
                payload(e).remove("sidecarGeneration");
            }
        }
        expect(implicitGeneration, true, "outbound generation inferred from exact live launch epoch");
        retryCases(valid);
        missingAndDuplicate(valid);
        focusedSchema(valid);
        readyAndWindowContract(valid);
        lifecycleIdentityAndOrder(valid);
        transportCorrelation(valid);
        counterfeitAcceptance(valid);
        if (!failures.isEmpty()) {
            throw new AssertionError("Recovery termination gate smoke failures (" + failures.size() + "):\n"
                    + String.join("\n", failures));
        }
        System.out.printf("Recovery termination trace gate smoke passed: %d positives, %d rejections%n",
                positive, rejected);
    }

    private static void missingAndDuplicate(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            List<ObjectNode> dropped = copy(valid);
            dropped.remove(i);
            expect(dropped, false, "drop " + type(valid.get(i)) + " #" + i);
            if (!type(valid.get(i)).endsWith("Credit")) {
                List<ObjectNode> duplicate = copy(valid);
                duplicate.add(i, duplicate.get(i).deepCopy());
                expect(duplicate, false, "duplicate " + type(valid.get(i)) + " #" + i);
            }
        }
    }

    private static void retryCases(List<ObjectNode> valid) {
        for (int epoch = 0; epoch < 2; epoch++) retryCases(valid, epoch);
    }

    private static void retryCases(List<ObjectNode> valid, int epoch) {
        List<ObjectNode> retry = copy(valid);
        int stage = find(retry, PREFIX + "stageObserved", epoch);
        ObjectNode push = retry.get(find(retry, "opaquePush", epoch)).deepCopy();
        ObjectNode ack = retry.get(find(retry, "opaquePushAck", epoch)).deepCopy();
        body(push).put("deliveryAttempt", 2);
        push.put("at", retry.get(stage).path("at").asLong() + 1);
        ack.put("at", retry.get(stage).path("at").asLong() + 2);
        retry.add(stage + 1, push);
        retry.add(stage + 2, ack);
        expect(retry, true, "same logical update retry after stage, before epoch terminal " + epoch);
        List<ObjectNode> earlyAck = copy(retry);
        body(earlyAck.get(find(earlyAck, "opaquePushAck", epoch))).put("lastSeenServerSyncId", 1);
        expect(earlyAck, true, "early lagging ACK followed by consumed retry ACK " + epoch);
        for (int lastSeen : List.of(-1, 1)) {
            List<ObjectNode> unconsumed = copy(earlyAck);
            String connection = "connection-" + (epoch == 0 ? 1 : 3);
            for (ObjectNode event : unconsumed) {
                if (type(event).equals("opaquePushAck") && body(event).path("connectionId").asString().equals(connection)) {
                    body(event).put("lastSeenServerSyncId", lastSeen);
                }
            }
            expect(unconsumed, false, "all logical ACKs lag update " + epoch + "/" + lastSeen);
        }
        mutate(retry, stage + 1, e -> body(e).put("deliveryAttempt", 3), "retry delivery attempt gap");
        mutate(retry, stage + 1, e -> body(e).put("deliveryAttempt", 1), "repeated attempt is not a retry");
        mutate(retry, stage + 1, e -> body(e).put("uidlBytes", 26), "retry changes logical payload length");
        mutate(retry, stage + 1, e -> body(e).put("flowServerSyncId", 3), "retry changes logical payload sync");
        List<ObjectNode> noAck = copy(retry);
        noAck.remove(stage + 2);
        expect(noAck, false, "post-stage retry lacks ordinal ACK");
        List<ObjectNode> repeated = copy(retry);
        repeated.add(stage + 1, repeated.get(stage + 1).deepCopy());
        expect(repeated, false, "duplicate retry payload");
    }

    private static void readyAndWindowContract(List<ObjectNode> valid) {
        int window = find(valid, "window.create", 0);
        for (String key : List.of("windowId", "sessionId", "uiId", "label", "mode")) {
            mutate(valid, window, e -> payload(e).remove(key), "window.create missing " + key);
            mutate(valid, window, e -> payload(e).put(key, "foreign"), "window.create wrong " + key);
            mutate(valid, window, e -> payload(e).put(key, true), "window.create malformed " + key);
        }
        mutate(valid, window, e -> e.put("direction", "recv"), "window.create wrong direction");
        for (int epoch = 0; epoch < 2; epoch++) {
            int row = find(valid, "ready", epoch), loaded = find(valid, "webview.loaded", epoch);
            for (String key : List.of("source", "transport", "sidecarGeneration")) {
                mutate(valid, row, e -> payload(e).remove(key), "ready missing wrapper " + key);
                mutate(valid, row, e -> payload(e).put(key, true), "ready malformed wrapper " + key);
            }
            for (String key : List.of("version", "type", "windowId", "sessionId", "uiId", "error")) {
                mutate(valid, row, e -> envelope(e).remove(key), "ready missing envelope " + key);
                mutate(valid, row, e -> envelope(e).put(key, true), "ready malformed envelope " + key);
            }
            for (String key : List.of("runtime", "transport", "uidlConsumer", "browserRequestTransport")) {
                mutate(valid, row, e -> body(e).remove(key), "ready missing contract " + key);
                mutate(valid, row, e -> body(e).put(key, "compatibility-http"), "ready wrong contract " + key);
            }
            mutate(valid, row, e -> e.put("direction", "send"), "ready wrong direction");
            mutate(valid, row, e -> payload(e).put("sidecarGeneration", 2), "non-ready second generation claims ready");
            for (String key : List.of("windowId", "sessionId", "uiId", "recoveryGeneration")) {
                mutate(valid, loaded, e -> payload(e).remove(key), "loaded missing " + key);
                mutate(valid, loaded, e -> payload(e).put(key, true), "loaded malformed " + key);
            }
            mutate(valid, loaded, e -> payload(e).put("recoveryGeneration", 2), "wrong loaded recovery epoch");
            mutate(valid, loaded, e -> e.put("direction", "send"), "loaded wrong direction");
            reorder(valid, row, find(valid, "sidecar.started", epoch == 0 ? 0 : 2), "ready before actual process start");
            reorder(valid, loaded, row, "loaded before Java ready");
        }
        List<ObjectNode> extraReady = copy(valid);
        ObjectNode blockedReady = extraReady.get(find(extraReady, "ready", 0)).deepCopy();
        payload(blockedReady).put("sidecarGeneration", 2);
        extraReady.add(find(extraReady, "sidecar.recoveryReadyTimeout", 0), blockedReady);
        expect(extraReady, false, "additional ready from blocked generation");
    }

    private static void focusedSchema(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            ObjectNode event = valid.get(i);
            if (!type(event).startsWith(PREFIX)) continue;
            int row = i;
            for (String key : List.copyOf(payload(event).propertyNames())) {
                String label = type(event) + "/" + key;
                mutate(valid, row, e -> payload(e).remove(key), "missing " + label);
                mutate(valid, row, e -> payload(e).putNull(key), "null " + label);
                mutate(valid, row, e -> payload(e).set(key, object()), "object " + label);
                mutate(valid, row, e -> payload(e).set(key, JSON.createArrayNode()), "array " + label);
                mutate(valid, row, e -> payload(e).put(key, "malformed"), "malformed string " + label);
                JsonNode original = payload(event).path(key);
                if (original.isBoolean()) {
                    mutate(valid, row, e -> payload(e).put(key, !original.asBoolean()), "inverted " + label);
                } else if (original.isNumber()) {
                    mutate(valid, row, e -> payload(e).put(key, true), "boolean number " + label);
                    mutate(valid, row, e -> payload(e).put(key, -1), "negative " + label);
                    if (!FRACTIONAL.contains(key)) {
                        mutate(valid, row, e -> payload(e).put(key, original.asDouble() + 0.5),
                                "fractional integer " + label);
                    }
                } else {
                    mutate(valid, row, e -> payload(e).put(key, true), "boolean string " + label);
                }
            }
            mutate(valid, row, e -> e.put("direction",
                    "send".equals(e.path("direction").asString()) ? "recv" : "send"), "focused wrong direction");
            mutate(valid, row, e -> payload(e).put("unrecognized", true), "unknown focused field");
            mutate(valid, row, e -> e.put("type", PREFIX + "unknownReceipt"), "unknown focused event");
        }
    }

    private static void lifecycleIdentityAndOrder(List<ObjectNode> valid) {
        for (String type : List.of("launch.sidecar", "sidecar.started", "sidecar.exit",
                "sidecar.recoveryScheduled", "sidecar.recoveryStarted", "sidecar.recoveryReadyTimeout",
                "sidecar.recoveryTerminated", "sidecar.recoveryEscalated", "sidecar.recoveryReady", "shutdown.sidecar")) {
            for (int row : indices(valid, type)) {
                for (String key : List.copyOf(payload(valid.get(row)).propertyNames())) {
                    mutate(valid, row, e -> payload(e).remove(key), "missing lifecycle " + type + "/" + key);
                    mutate(valid, row, e -> payload(e).put(key, "wrong"), "malformed lifecycle " + type + "/" + key);
                    if (payload(valid.get(row)).path(key).isNumber()) {
                        mutate(valid, row, e -> payload(e).put(key, true), "boolean lifecycle number " + type + "/" + key);
                    }
                }
                mutate(valid, row, e -> e.put("direction", "wrong"), "lifecycle direction " + type);
            }
        }
        int term = find(valid, "sidecar.recoveryTerminated", 0);
        int escalation = find(valid, "sidecar.recoveryEscalated", 0);
        for (long pid : List.of(FIRST_PID, LAST_PID, 999L)) {
            mutate(valid, term, e -> payload(e).put("processIdentifier", pid), "TERM stale or foreign target " + pid);
            mutate(valid, escalation, e -> payload(e).put("processIdentifier", pid), "escalation stale or foreign target " + pid);
        }
        for (long elapsed : List.of(-1L, 0L, 100L, 6000L)) {
            mutate(valid, escalation, e -> e.put("at", valid.get(term).path("at").asLong() + elapsed),
                    "invalid escalation grace interval " + elapsed);
        }
        for (int row : List.of(term, escalation)) {
            mutate(valid, row, e -> e.remove("at"), "missing escalation clock");
            mutate(valid, row, e -> e.put("at", "1000"), "string escalation clock");
        }
        reorder(valid, find(valid, "sidecar.recoveryScheduled", 1), find(valid, "sidecar.exit", 1),
                "retry scheduled before second actual exit");
        reorder(valid, find(valid, "sidecar.recoveryStarted", 1), find(valid, "sidecar.exit", 1),
                "restart before second actual exit");
        reorder(valid, find(valid, "launch.sidecar", 2), find(valid, "sidecar.exit", 1),
                "third launch while TERM target still alive");
        reorder(valid, find(valid, "sidecar.started", 2), find(valid, "sidecar.exit", 1),
                "third start while TERM target still alive");
        reorder(valid, escalation, find(valid, PREFIX + "blockedShutdownObserved", 0), "escalate before hook observation");
        reorder(valid, term, find(valid, "sidecar.recoveryReadyTimeout", 0), "TERM before ready timeout");
        reorder(valid, find(valid, PREFIX + "failureInjected", 0), find(valid, PREFIX + "stageObserved", 0),
                "kill initial process before settled interaction");
        reorder(valid, find(valid, PREFIX + "nativePointerDispatch", 1), find(valid, "sidecar.recoveryReady", 0),
                "recovered click before actual ready");
        reorder(valid, find(valid, "shutdown.sidecar", 0), find(valid, PREFIX + "frameworkAcceptance", 0),
                "shutdown before acceptance");
        mutate(valid, find(valid, "sidecar.started", 2), e -> payload(e).put("processIdentifier", FIRST_PID),
                "PID reused as recovered identity");
        mutate(valid, find(valid, "sidecar.recoveryReady", 0), e -> payload(e).put("processIdentifier", BLOCKED_PID),
                "non-ready blocked generation forged as recovered");
        mutate(valid, find(valid, "sidecar.exit", 1), e -> payload(e).put("status", 0), "TERM exit mistaken for SIGKILL");
        mutate(valid, find(valid, "sidecar.exit", 1), e -> payload(e).put("reason", 1), "wrong process termination reason");
    }

    private static void transportCorrelation(List<ObjectNode> valid) {
        for (int i = 0; i < valid.size(); i++) {
            ObjectNode event = valid.get(i);
            if (!WIRE.contains(type(event))) continue;
            int row = i;
            for (String key : List.of("version", "type", "windowId", "sessionId", "uiId", "id", "correlationId", "error")) {
                mutate(valid, row, e -> envelope(e).remove(key), "missing wire envelope " + type(event) + "/" + key);
                mutate(valid, row, e -> envelope(e).put(key, true), "boolean wire envelope " + type(event) + "/" + key);
            }
            mutate(valid, row, e -> payload(e).put("sidecarGeneration", 2), "non-ready wire generation");
            mutate(valid, row, e -> payload(e).put("transport", "compatibility-http"), "non-opaque transport");
            mutate(valid, row, e -> e.put("direction", "wrong"), "wire direction");
            String peer = "send".equals(event.path("direction").asString()) ? "target" : "source";
            mutate(valid, row, e -> payload(e).put(peer, "foreign-peer"), "wire peer");
            if (type(event).startsWith("opaqueHttp")) {
                mutate(valid, row, e -> body(e).put("requestId", "orphan"), "orphan request body");
                mutate(valid, row, e -> envelope(e).put("correlationId", "orphan"), "orphan response correlation");
            } else {
                for (String key : List.of("connectionId", "flowUiId", "pushSequence", "flowServerSyncId")) {
                    mutate(valid, row, e -> body(e).remove(key), "missing push key " + key);
                    mutate(valid, row, e -> body(e).put(key, "wrong"), "wrong push key " + key);
                }
            }
        }
        for (int i = 0; i < 2; i++) {
            int push = find(valid, "opaquePush", i), ack = find(valid, "opaquePushAck", i);
            int pointer = find(valid, PREFIX + "nativePointerDispatch", i), stage = find(valid, PREFIX + "stageObserved", i);
            reorder(valid, ack, push, "ACK before push");
            List<ObjectNode> lateAck = copy(valid);
            lateAck.add(ack, lateAck.remove(stage));
            expect(lateAck, true, "stage before matching ACK within epoch terminal bound " + i);
            mutate(valid, push, e -> body(e).put("uidlRedacted", false), "unredacted push marker");
            mutate(valid, push, e -> body(e).put("uidl", "private-body"), "raw UIDL body");
            mutate(valid, push, e -> body(e).put("uidlBytes", 0), "empty pushed update");
            mutate(valid, ack, e -> body(e).remove("lastSeenServerSyncId"), "ACK lacks actual client sync receipt");
            mutate(valid, ack, e -> body(e).put("lastSeenServerSyncId", -1), "ACK cannot settle unapplied update");
            mutate(valid, ack, e -> body(e).put("lastSeenServerSyncId", "2"), "ACK client sync must be integral");
            mutate(valid, find(valid, "opaqueHttpStreamResponse", i * 2 + 1), e -> body(e).put("status", 500),
                    "failed listener response");
            List<ObjectNode> falseBaseline = copy(valid);
            payload(falseBaseline.get(pointer)).put("requestBaseline", 0).put("responseBaseline", 0);
            payload(falseBaseline.get(stage)).put("requestBaseline", 0).put("responseBaseline", 0);
            expect(falseBaseline, false, "matching but fictitious request baselines");
            List<ObjectNode> gap = copy(valid);
            body(gap.get(push)).put("pushSequence", 99);
            body(gap.get(ack)).put("pushSequence", 99);
            expect(gap, false, "matching ACK cannot conceal logical push gap");
        }
        List<ObjectNode> lateTraffic = copy(valid);
        ObjectNode extra = valid.get(find(valid, "opaqueHttpStreamOpen", 2)).deepCopy();
        body(extra).put("requestId", "late-request");
        envelope(extra).put("id", "late-request").put("correlationId", "late-request");
        lateTraffic.add(find(lateTraffic, PREFIX + "frameworkAcceptance", 0), extra);
        expect(lateTraffic, false, "unaccounted traffic after recovered stage");
        List<ObjectNode> blockedTraffic = copy(valid);
        blockedTraffic.add(find(blockedTraffic, "sidecar.recoveryReadyTimeout", 0),
                valid.get(find(valid, "opaquePush", 0)).deepCopy());
        expect(blockedTraffic, false, "old generation traffic during unready replacement");
        List<ObjectNode> staleTraffic = copy(valid);
        staleTraffic.add(find(staleTraffic, PREFIX + "stageObserved", 1), valid.get(find(valid, "opaquePush", 0)).deepCopy());
        expect(staleTraffic, false, "old push carried into recovered document");
        List<ObjectNode> noChunks = copy(valid);
        noChunks.removeIf(e -> type(e).equals("opaqueHttpStreamResponseChunk"));
        expect(noChunks, false, "missing every response chunk despite successful end receipts");
        int chunk = find(valid, "opaqueHttpStreamResponseChunk", 0);
        mutate(valid, chunk, e -> envelope(e).put("sessionId", "foreign-session"), "wrong-scope response chunk");
        mutate(valid, chunk, e -> payload(e).put("sidecarGeneration", 2), "unready generation response chunk");
        mutate(valid, chunk, e -> body(e).put("chunkSequence", 1), "response chunk sequence gap");
        mutate(valid, chunk, e -> body(e).put("bodyBytes", 9), "response chunk length differs from end");
        mutate(valid, chunk, e -> body(e).put("bodyRedacted", false), "response chunk not redacted");
        mutate(valid, chunk, e -> body(e).put("bodyBase64", "cHJpdmF0ZQ=="), "raw response chunk content");
        List<ObjectNode> blockedChunk = copy(valid);
        blockedChunk.add(find(blockedChunk, "sidecar.recoveryReadyTimeout", 0), valid.get(chunk).deepCopy());
        expect(blockedChunk, false, "response chunk during non-ready generation");
    }

    private static void counterfeitAcceptance(List<ObjectNode> valid) {
        expect(List.of(valid.get(find(valid, PREFIX + "frameworkAcceptance", 0)).deepCopy()), false,
                "acceptance assertion without underlying evidence");
        int accepted = find(valid, PREFIX + "frameworkAcceptance", 0);
        for (String field : ACCEPTANCE_TRUE) {
            mutate(valid, accepted, e -> payload(e).put(field, false), "false final assertion " + field);
        }
        List<ObjectNode> sameDocument = copy(valid);
        for (ObjectNode event : sameDocument) {
            if (payload(event).path("fixtureGeneration").asInt() == 3) {
                payload(event).put("documentMarker", FIRST_DOCUMENT).put("documentTimeOriginMillis", 1000.0);
            }
        }
        expect(sameDocument, false, "self-consistent but reused old document");
        List<ObjectNode> wrongRun = copy(valid);
        payload(wrongRun.get(find(wrongRun, PREFIX + "blockedShutdownObserved", 0)))
                .put("runId", "ad7ffb59-cd9f-45ec-be31-8f142ad9bf70");
        expect(wrongRun, false, "foreign private-run hook receipt");
        for (String type : List.of("error", "webview.error", "sidecar.startFailed", "sidecar.recoveryEscalationFailed",
                "sidecar.replacementRequested", "opaqueHttpStreamError", "opaquePushError", PREFIX + "unexpectedErrorObserved")) {
            List<ObjectNode> bad = copy(valid);
            bad.add(object("at", 100_000L, "direction", "recv", "type", type, "payload", object()));
            expect(bad, false, "forbidden failure event " + type);
        }
    }

    private static List<ObjectNode> valid() {
        Builder trace = new Builder();
        trace.launch(1, FIRST_PID);
        trace.ready(1);
        trace.add("send", "window.create", scope().put("label", "main").put("mode", "flow-ipc"));
        trace.add("recv", "webview.loaded", scope().put("recoveryGeneration", 0));
        trace.stage(0);
        trace.add("send", PREFIX + "failureInjected", frame(0).put("signal", 9)
                .put("targetProcessIdentifier", FIRST_PID).put("ownedProcessConfirmed", true)
                .put("initialInteractionSettled", true));
        trace.exit(1, FIRST_PID);
        trace.recovery(1, FIRST_PID);
        trace.launch(2, BLOCKED_PID);
        trace.clock += 15_000;
        trace.add("send", "sidecar.recoveryReadyTimeout", object("attempt", 1, "processIdentifier", BLOCKED_PID,
                "timeoutSeconds", 15.0));
        trace.add("send", "sidecar.recoveryTerminated", object("generation", 2, "processIdentifier", BLOCKED_PID,
                "reason", "ready-timeout"));
        ObjectNode hook = scope().put("runId", RUN).put("fixtureGeneration", 2)
                .put("processIdentifier", BLOCKED_PID).put("initialProcessIdentifier", FIRST_PID)
                .put("recoveryGeneration", 0).put("hookMarkerMatched", true).put("processMarkerMatched", true)
                .put("mainContextRetained", true).put("mainWindowRetained", true).put("mainWebViewRetained", true);
        trace.add("recv", PREFIX + "blockedShutdownObserved", hook);
        trace.clock += 800;
        trace.add("send", "sidecar.recoveryEscalated", object("generation", 2, "processIdentifier", BLOCKED_PID,
                "reason", "ready-timeout", "graceSeconds", 1.0));
        trace.exit(2, BLOCKED_PID);
        trace.recovery(2, BLOCKED_PID);
        trace.launch(3, LAST_PID);
        trace.ready(3);
        trace.add("recv", "sidecar.recoveryReady", object("attempt", 2, "previousProcessIdentifier", BLOCKED_PID,
                "processIdentifier", LAST_PID, "processReplaced", true, "windowsReloaded", 1, "recoveryGeneration", 1));
        trace.add("recv", "webview.loaded", scope().put("recoveryGeneration", 1));
        trace.stage(1);
        ObjectNode accepted = frame(1).put("initialProcessIdentifier", FIRST_PID)
                .put("blockedProcessIdentifier", BLOCKED_PID).put("recoveredProcessIdentifier", LAST_PID)
                .put("initialDocumentMarker", FIRST_DOCUMENT).put("initialDocumentTimeOriginMillis", 1000.0)
                .put("initialRecoveryGeneration", 0).put("finalRecoveryGeneration", 1)
                .put("initialRequestCount", 2).put("initialResponseCount", 2).put("initialPushMessageCount", 1)
                .put("recoveredRequestCount", 2).put("recoveredResponseCount", 2).put("recoveredPushMessageCount", 1)
                .put("nativePointerEventCount", 2).put("nativePointerTrustedEventCount", 2)
                .put("nativePointerStages", "initial,recovered");
        ACCEPTANCE_TRUE.forEach(field -> accepted.put(field, true));
        trace.add("recv", PREFIX + "frameworkAcceptance", accepted);
        trace.add("recv", "shutdown.sidecar", object("mode", "graceful"));
        return trace.events;
    }

    private static ObjectNode frame(int epoch) {
        return scope().put("runId", RUN).put("fixtureGeneration", epoch == 0 ? 1 : 3)
                .put("processIdentifier", epoch == 0 ? FIRST_PID : LAST_PID).put("recoveryGeneration", epoch)
                .put("documentMarker", epoch == 0 ? FIRST_DOCUMENT : LAST_DOCUMENT)
                .put("documentTimeOriginMillis", epoch == 0 ? 1000.0 : 20_000.0)
                .put("firstMeaningfulPaint", true).put("flowClientCount", 1).put("mainContextRetained", true)
                .put("mainWindowRetained", true).put("mainWebViewRetained", true).put("mainWindowNumber", 101);
    }

    private static final class Builder {
        final List<ObjectNode> events = new ArrayList<>();
        long clock = 1100;
        void add(String direction, String type, ObjectNode payload) {
            events.add(object("at", clock, "direction", direction, "type", type, "payload", payload));
            clock += 100;
        }
        void launch(int generation, long pid) {
            add("send", "launch.sidecar", object("generation", generation, "transport", "flow-ipc",
                    "jar", "$APP/Contents/Resources/Sidecar/application.jar"));
            add("recv", "sidecar.started", object("generation", generation, "processIdentifier", pid, "transport", "flow-ipc"));
        }
        void exit(int generation, long pid) {
            add("recv", "sidecar.exit", object("generation", generation, "processIdentifier", pid,
                    "status", 9, "reason", 2, "expected", false));
        }
        void ready(int generation) {
            wire("ready", generation, "ready-" + generation, object("runtime", "standard-vaadin-bundle-opaque-ipc",
                    "transport", "flow-ipc", "uidlConsumer", "standard-flow-client",
                    "browserRequestTransport", "opaque-http-stream-ipc"));
        }
        void recovery(int attempt, long pid) {
            add("recv", "sidecar.recoveryScheduled", object("attempt", attempt, "delayMillis", attempt * 500,
                    "restartLimit", 2, "windowCount", 1, "previousProcessIdentifier", pid));
            clock += attempt * 500;
            add("send", "sidecar.recoveryStarted", object("attempt", attempt, "previousProcessIdentifier", pid));
        }
        void stage(int epoch) {
            int generation = epoch == 0 ? 1 : 3;
            String label = epoch == 0 ? "initial" : "recovered";
            request(generation, "bootstrap");
            add("recv", PREFIX + "nativePointerDispatch", frame(epoch).put("stage", label).put("index", epoch)
                    .put("inputMode", "appkit-window-pointer-event").put("button", "primary")
                    .put("activeApplication", true).put("keyWindow", true).put("webViewX", 40.0).put("webViewY", 80.0)
                    .put("windowX", 40.0).put("windowY", 680.0).put("dispatchedAt", (double) clock)
                    .put("requestBaseline", 1).put("responseBaseline", 1).put("pushBaseline", 0)
                    .put("counterBefore", 0).put("pushPending", true));
            request(generation, "listener");
            ObjectNode push = object("connectionId", "connection-" + generation, "flowUiId", 0,
                    "pushSequence", 1, "flowServerSyncId", 2, "deliveryAttempt", 1, "uidlBytes", 25,
                    "uidlRedacted", true, "resync", false);
            wire("opaquePush", generation, "push-" + generation, push);
            wire("opaquePushAck", generation, "push-" + generation, object("connectionId", "connection-" + generation,
                    "flowUiId", 0, "pushSequence", 1, "flowServerSyncId", 2, "lastSeenServerSyncId", 2));
            add("recv", PREFIX + "stageObserved", frame(epoch).put("stage", label).put("index", epoch)
                    .put("counter", 1).put("pushCount", 1).put("client", true).put("backgroundSessionAbsent", true)
                    .put("nativePointerReceiptCount", 1).put("nativePointerTrustedCount", 1)
                    .put("requestBaseline", 1).put("responseBaseline", 1).put("pushBaseline", 0)
                    .put("requestCount", 2).put("responseCount", 2).put("pushMessageCount", 1)
                    .put("balancedDrainObserved", true));
        }
        void request(int generation, String suffix) {
            String id = "request-" + generation + "-" + suffix;
            wire("opaqueHttpStreamOpen", generation, id, object("requestId", id,
                    "method", suffix.equals("listener") ? "POST" : "GET", "contentLength", 0));
            wire("opaqueHttpStreamEnd", generation, id, object("requestId", id, "chunkCount", 0, "bodyBytes", 0));
            wire("opaqueHttpStreamResponse", generation, id, object("requestId", id, "status", 200));
            wire("opaqueHttpStreamResponseCredit", generation, id, object("requestId", id, "credit", 1));
            wire("opaqueHttpStreamResponseChunk", generation, id,
                    object("requestId", id, "chunkSequence", 0, "bodyBytes", 8, "bodyRedacted", true));
            wire("opaqueHttpStreamResponseEnd", generation, id, object("requestId", id, "chunkCount", 1, "bodyBytes", 8));
        }
        void wire(String type, int generation, String id, ObjectNode body) {
            boolean send = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamEnd",
                    "opaqueHttpStreamChunk", "opaqueHttpStreamResponseCredit", "opaquePushAck").contains(type);
            ObjectNode envelope = scope().put("version", 1).put("type", type).put("id", id)
                    .put("correlationId", id).putNull("error");
            envelope.set("payload", body);
            add(send ? "send" : "recv", type, object(send ? "target" : "source", "java-sidecar",
                    "transport", "flow-ipc", "sidecarGeneration", generation, "envelope", envelope));
        }
    }

    private static ObjectNode scope() { return object("windowId", "main", "sessionId", "flow-ipc-session", "uiId", "main"); }
    private static ObjectNode object(Object... fields) {
        ObjectNode value = JSON.createObjectNode();
        for (int i = 0; i < fields.length; i += 2) {
            String key = (String) fields[i]; Object field = fields[i + 1];
            if (field instanceof String s) value.put(key, s);
            else if (field instanceof Integer n) value.put(key, n);
            else if (field instanceof Long n) value.put(key, n);
            else if (field instanceof Double n) value.put(key, n);
            else if (field instanceof Boolean b) value.put(key, b);
            else if (field instanceof JsonNode node) value.set(key, node);
            else throw new IllegalArgumentException("Unsupported synthetic field");
        }
        return value;
    }
    private static String type(ObjectNode event) { return event.path("type").asString(); }
    private static ObjectNode payload(ObjectNode event) { return (ObjectNode) event.path("payload"); }
    private static ObjectNode envelope(ObjectNode event) { return (ObjectNode) payload(event).path("envelope"); }
    private static ObjectNode body(ObjectNode event) { return (ObjectNode) envelope(event).path("payload"); }
    private static List<ObjectNode> copy(List<ObjectNode> events) { return new ArrayList<>(events.stream().map(ObjectNode::deepCopy).toList()); }
    private static int find(List<ObjectNode> events, String type, int ordinal) {
        for (int i = 0; i < events.size(); i++) if (type.equals(type(events.get(i))) && ordinal-- == 0) return i;
        throw new IllegalArgumentException("Missing synthetic event " + type);
    }
    private static List<Integer> indices(List<ObjectNode> events, String type) {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) if (type.equals(type(events.get(i)))) rows.add(i);
        return rows;
    }
    private static void mutate(List<ObjectNode> valid, int row, Consumer<ObjectNode> action, String label) {
        List<ObjectNode> events = copy(valid);
        action.accept(events.get(row));
        if (events.equals(valid)) throw new AssertionError("No-op mutation: " + label);
        expect(events, false, label);
    }
    private static void reorder(List<ObjectNode> valid, int from, int to, String label) {
        List<ObjectNode> events = copy(valid);
        ObjectNode event = events.remove(from);
        events.add(to, event);
        expect(events, false, label);
    }
    private static void expect(List<ObjectNode> events, boolean shouldPass, String label) {
        RecoveryTerminationTraceGate gate = new RecoveryTerminationTraceGate();
        List<String> missing = new ArrayList<>();
        try {
            events.forEach(gate::accept);
            gate.require(missing);
        } catch (RuntimeException ex) {
            failures.add(label + ": gate threw " + ex.getClass().getSimpleName());
            return;
        }
        if (missing.isEmpty() != shouldPass) {
            failures.add(label + (shouldPass ? ": rejected " + missing : ": unexpectedly accepted"));
        } else if (shouldPass) positive++;
        else rejected++;
    }
}
