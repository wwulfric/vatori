package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Synthetic fail-closed contract checks, not real OS delivery or banner evidence. */
public final class NotificationDeliveryTraceGateSmokeMain {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String GATE = "--require-flow-ipc-notification-delivery";
    private static final String PREFIX = "flowNotificationDelivery.";
    private static final String MANIFEST = "$APP/Contents/Resources/desktop-capabilities.json";
    private static final String FIRST_DIGEST = "a".repeat(64), PROBE_DIGEST = "b".repeat(64);
    private static final List<String> TITLES = List.of("Vatori notification preparation", "Vatori foreground probe");
    private static final String BODY = "Native foreground delivery validation";
    private static int positive, rejected;

    private NotificationDeliveryTraceGateSmokeMain() { }

    public static void main(String[] args) throws Exception {
        Path trace = Files.createTempFile("notification-delivery-inspector-", ".log");
        try {
            List<ObjectNode> valid = valid();
            assertTrace(trace, valid, true, "exact nil-delegate foreground OS delivery record");
            List<ObjectNode> oneCleanup = copy(valid);
            payload(oneCleanup.get(find(oneCleanup, PREFIX + "frameworkAcceptance", 0))).put("testCleanupIdentifierCount", 1);
            assertTrace(trace, oneCleanup, true, "only exact probe record remains to clean");
            List<ObjectNode> retry = copy(valid);
            int push = find(retry, "opaquePush", 0), ack = find(retry, "opaquePushAck", 0);
            ObjectNode retried = retry.get(push).deepCopy();
            body(retried).put("deliveryAttempt", 2);
            body(retry.get(ack)).put("lastSeenServerSyncId", -1);
            retry.add(ack, retried);
            retry.add(ack + 2, retry.get(ack + 1).deepCopy());
            assertTrace(trace, retry, true, "overlapping retry and ordinal ACK causality");
            rejectMissingDuplicateAndMalformed(trace, valid);
            rejectPrivacyAndContradictions(trace, valid);
            rejectOrderingAndCorrelation(trace, valid);
            System.out.printf("Notification delivery trace gate smoke passed: %d positives, %d rejections%n",
                    positive, rejected);
        } finally {
            Files.deleteIfExists(trace);
        }
    }

    private static void rejectMissingDuplicateAndMalformed(Path trace, List<ObjectNode> valid) throws Exception {
        Set<String> repeatable = Set.of("launch", "standardBundleReady", "standardBundleResource", "webview.firstMeaningfulPaint");
        for (int i = 0; i < valid.size(); i++) {
            String type = type(valid.get(i));
            if (type.equals("shutdown")) continue;
            List<ObjectNode> dropped = copy(valid);
            dropped.remove(i);
            assertTrace(trace, dropped, false, "drop " + type + " at " + i);
            if (!repeatable.contains(type) && !type.endsWith("Credit")) {
                List<ObjectNode> duplicated = copy(valid);
                duplicated.add(i, duplicated.get(i).deepCopy());
                assertTrace(trace, duplicated, false, "duplicate " + type + " at " + i);
            }
            final int row = i;
            if (type.startsWith(PREFIX)) {
                for (String field : List.copyOf(payload(valid.get(i)).propertyNames())) {
                    mutate(trace, valid, row, e -> payload(e).remove(field), "missing focused " + type + "/" + field);
                    mutate(trace, valid, row, e -> payload(e).put(field, "malformed"), "string focused " + type + "/" + field);
                    JsonNode original = payload(valid.get(i)).path(field);
                    if (original.isBoolean()) mutate(trace, valid, row, e -> payload(e).put(field, !original.asBoolean()),
                            "inverted boolean " + type + "/" + field);
                    if (original.isNumber()) {
                        mutate(trace, valid, row, e -> payload(e).put(field, true), "boolean number " + type + "/" + field);
                        if (!Set.of("dispatchedAt", "probeDispatchAt", "queryStartedAt", "notificationDateAt", "observedAt",
                                "webViewX", "webViewY", "windowX", "windowY").contains(field)) {
                            mutate(trace, valid, row, e -> payload(e).put(field, original.asDouble() + 0.5),
                                    "fractional count " + type + "/" + field);
                            mutate(trace, valid, row, e -> payload(e).put(field, -1), "negative count " + type + "/" + field);
                        }
                    }
                }
                mutate(trace, valid, row, e -> e.put("direction", "send"), "focused event direction");
                mutate(trace, valid, row, e -> payload(e).put("unrecognized", true), "unknown focused field");
            }
            if (Set.of("command", "capabilityResult").contains(type)) {
                for (String field : List.copyOf(envelope(valid.get(i)).propertyNames())) {
                    if (field.equals("payload")) continue;
                    mutate(trace, valid, row, e -> envelope(e).remove(field), "missing native envelope " + field);
                    mutate(trace, valid, row, e -> envelope(e).put(field, "wrong"), "malformed native envelope " + field);
                }
                for (String field : List.copyOf(body(valid.get(i)).propertyNames())) {
                    mutate(trace, valid, row, e -> body(e).remove(field), "missing native payload " + field);
                    mutate(trace, valid, row, e -> body(e).put(field, "wrong"), "malformed native payload " + field);
                }
                for (String field : List.of("sidecarGeneration", "transport", type.equals("command") ? "source" : "target")) {
                    mutate(trace, valid, row, e -> payload(e).remove(field), "missing native wrapper " + field);
                    mutate(trace, valid, row, e -> payload(e).put(field, "wrong"), "malformed native wrapper " + field);
                }
                mutate(trace, valid, row, e -> e.put("direction", "wrong"), "native wrong direction");
            }
        }
    }

    private static void rejectPrivacyAndContradictions(Path trace, List<ObjectNode> valid) throws Exception {
        for (String type : List.of(PREFIX + "deliveryObserved", PREFIX + "stageObserved",
                PREFIX + "frameworkAcceptance", "command", "capabilityResult")) {
            int i = find(valid, type, 0);
            for (String field : List.of("title", "body", "notificationId", "error", "userInfo")) {
                mutate(trace, valid, i, e -> payload(e).put(field, "private"), "raw focused/native field " + field);
            }
        }
        for (int i = 0; i < 2; i++) {
            int result = find(valid, "capabilityResult", i), command = find(valid, "command", i);
            for (String field : List.of("notificationId", "title", "body")) {
                mutate(trace, valid, result, e -> body(e).put(field, "private"), "unmasked result " + field);
            }
            for (String field : List.of("title", "body")) {
                mutate(trace, valid, command, e -> ((ObjectNode) body(e).path("args")).put(field, "[redacted]private"),
                        "forged redaction marker " + field);
            }
            for (String field : List.of("nativeErrorDomain", "nativeErrorCode", "errorCode", "error",
                    "operationInvalidated", "canceled", "cancelReason")) {
                mutate(trace, valid, result, e -> body(e).put(field, "contradiction"), "extra terminal diagnostic " + field);
            }
        }
        for (String raw : List.of(TITLES.getFirst(), TITLES.getLast(), BODY,
                "vatori-12345678-1234-1234-1234-123456789abc")) {
            List<ObjectNode> bad = copy(valid);
            bad.add(event("recv", "unrelated", object("leaked", raw)));
            assertTrace(trace, bad, false, "raw secret anywhere in serialized trace");
            assertTrace(trace, bad, false, "Unicode-escaped raw secret", true);
        }
        int delivered = find(valid, PREFIX + "deliveryObserved", 0), accepted = find(valid, PREFIX + "frameworkAcceptance", 0);
        for (String bad : List.of("", "b".repeat(63), "B".repeat(64), "[redacted length=64]", FIRST_DIGEST)) {
            mutate(trace, valid, delivered, e -> payload(e).put("notificationDigest", bad), "wrong exact digest");
        }
        for (String field : List.of("applicationResignActiveCount", "windowResignKeyCount")) {
            mutate(trace, valid, delivered, e -> payload(e).put(field, 1), "focus loss during probe");
            mutate(trace, valid, accepted, e -> payload(e).put(field, 1), "final focus loss");
        }
        for (String field : List.of("matchingCount", "testCleanupIdentifierCount")) {
            mutate(trace, valid, field.equals("matchingCount") ? delivered : accepted,
                    e -> payload(e).put(field, 3), "unexpected record/cleanup count");
        }
        for (String badType : List.of(PREFIX + "unexpectedErrorObserved", PREFIX + "failed",
                "sidecar.recoveryReady", "sidecar.replacementRequested", "nativeCapabilityOperation.invalidated",
                "opaqueHttpStreamError", "error", "webview.error", "staleCapabilityResultDropped")) {
            List<ObjectNode> bad = copy(valid);
            bad.add(event("recv", badType, object()));
            assertTrace(trace, bad, false, "forbidden failure " + badType);
        }
        int manifest = find(valid, "capabilityManifest", 0);
        mutate(trace, valid, manifest, e -> ((ObjectNode) payload(e).path("commandsByWindow"))
                .set("main", JSON.createArrayNode().add("notification.show").add("window.open")), "manifest expanded");
        mutate(trace, valid, manifest, e -> ((ObjectNode) payload(e).path("commandsByWindow"))
                .set("secondary", JSON.createArrayNode()), "extra manifest window");
    }

    private static void rejectOrderingAndCorrelation(Path trace, List<ObjectNode> valid) throws Exception {
        int delivered = find(valid, PREFIX + "deliveryObserved", 0);
        for (String field : List.of("probeDispatchAt", "queryStartedAt", "notificationDateAt", "observedAt")) {
            for (double bad : List.of(-1.0, 0.0)) mutate(trace, valid, delivered,
                    e -> payload(e).put(field, bad), "invalid timestamp " + field);
        }
        mutate(trace, valid, delivered, e -> payload(e).put("probeDispatchAt", 1999.0), "dispatch timestamp mismatch");
        mutate(trace, valid, delivered, e -> payload(e).put("notificationDateAt", 1999.0), "old delivered date");
        mutate(trace, valid, delivered, e -> payload(e).put("notificationDateAt", 4000.0), "future delivered date");
        mutate(trace, valid, delivered, e -> payload(e).put("observedAt", 2000.0), "observation before notification date");
        mutate(trace, valid, delivered, e -> payload(e).put("queryStartedAt", 1999.0), "query before probe dispatch");
        mutate(trace, valid, delivered, e -> payload(e).put("queryStartedAt", 3000.0), "query after observation");
        mutate(trace, valid, find(valid, PREFIX + "nativePointerDispatch", 0),
                e -> payload(e).put("dispatchedAt", 2001.0), "prepare dispatch after probe dispatch");
        reorder(trace, valid, find(valid, PREFIX + "authorizationObserved", 0),
                find(valid, PREFIX + "nativePointerDispatch", 0), "authorization before pointer required");
        reorder(trace, valid, find(valid, PREFIX + "authorizationObserved", 1),
                find(valid, PREFIX + "stageObserved", 0), "authorization after prepare required");
        reorder(trace, valid, delivered, find(valid, PREFIX + "stageObserved", 1), "delivery after public Java probe receipt");
        reorder(trace, valid, find(valid, PREFIX + "frameworkAcceptance", 0), delivered, "delivery before acceptance");
        for (int i = 0; i < 2; i++) {
            reorder(trace, valid, find(valid, "capabilityResult", i), find(valid, "command", i), "result before command");
            reorder(trace, valid, find(valid, "command", i), find(valid, PREFIX + "nativePointerDispatch", i), "command before click");
            int command = find(valid, "command", i);
            for (JsonNode invalid : List.of(JSON.nullNode(), JSON.createArrayNode(), object(),
                    JSON.getNodeFactory().booleanNode(true), JSON.getNodeFactory().numberNode(38.5),
                    JSON.getNodeFactory().numberNode(0), JSON.getNodeFactory().numberNode(-1),
                    JSON.getNodeFactory().numberNode(new java.math.BigInteger("9223372036854775808")))) {
                mutate(trace, valid, command, e -> body(e).set("serverSequence", invalid.deepCopy()),
                        "invalid standard server sequence type or range");
            }
            mutate(trace, valid, command, e -> body(e).put("serverSequenceScope", "webview-to-java-sidecar"),
                    "opposite server sequence scope");
            mutate(trace, valid, command, e -> body(e).put("serverSequenceScope", true),
                    "boolean server sequence scope");
            mutate(trace, valid, command, e -> body(e).put("unknownMetadata", 1),
                    "unknown command field remains rejected");
            int push = find(valid, "opaquePush", i), request = find(valid, "opaqueHttpStreamOpen", i + 2);
            mutate(trace, valid, push, e -> envelope(e).put("sessionId", "wrong"), "wrong push scope");
            mutate(trace, valid, request, e -> envelope(e).put("sessionId", "wrong"), "wrong request scope");
            mutate(trace, valid, push, e -> body(e).put("pushSequence", 99), "push sequence gap");
            mutate(trace, valid, find(valid, "opaquePushAck", i), e -> body(e).put("pushSequence", 99), "orphan ACK");
        }
        for (long sequence : List.of(38L, 37L)) {
            mutate(trace, valid, find(valid, "command", 1), e -> body(e).put("serverSequence", sequence),
                    "duplicate or reversed command server sequence");
        }
        for (String type : List.of("sidecar.started", "launch.sidecar", "window.create", "windowClosed",
                "host.nativeResourceDrainStarted", "host.nativeResourceDrainCompleted", "shutdown.sidecar")) {
            int i = find(valid, type, 0);
            for (String field : List.copyOf(payload(valid.get(i)).propertyNames())) {
                if (!field.equals("timeoutSeconds")) mutate(trace, valid, i, e -> payload(e).put(field, "wrong"),
                        "malformed lifecycle " + type + "/" + field);
            }
        }
    }

    private static List<ObjectNode> valid() {
        List<ObjectNode> events = new ArrayList<>();
        events.add(event("recv", "capabilityManifest", object("path", MANIFEST, "loaded", true,
                "permissionMode", "manifest-enforced", "parseError", "",
                "commandsByWindow", object("main", JSON.createArrayNode().add("notification.show")))));
        events.add(event("recv", "launch", object("transport", "flow-ipc")));
        events.add(event("send", "launch.sidecar", object("generation", 1, "transport", "flow-ipc")));
        events.add(event("recv", "sidecar.started", object("generation", 1, "processIdentifier", 111, "transport", "flow-ipc")));
        events.add(wire("ready", true, "ready", object("runtime", "standard-vaadin-bundle-opaque-ipc",
                "browserRequestTransport", "opaque-http-stream-ipc", "transport", "flow-ipc",
                "flowClientResourceLoading", "standard-production-bundle")));
        ObjectNode created = scope(true);
        created.put("label", "main").put("mode", "flow-ipc");
        events.add(event("send", "window.create", created));
        events.add(event("recv", "standardBundleReady", object("requestTransport", "opaque-http-stream-ipc")));
        events.add(event("recv", "standardBundleResource", object("path", "/VAADIN/build/FlowClient-notification.js")));
        events.add(event("recv", "webview.firstMeaningfulPaint", object()));
        request(events, true, "bootstrap", "GET");
        request(events, true, "route", "GET");
        connect(events, true);
        events.add(authorization(true));
        stage(events, 0);
        events.add(authorization(false));
        stage(events, 1);
        ObjectNode delivered = identity(true);
        delivered.put("stage", "probe").put("notificationDigest", PROBE_DIGEST).put("digestAlgorithm", "sha256")
                .put("matchingCount", 1).put("exactPublicIdentifierDigestMatched", true)
                .put("notificationDateAtOrAfterProbeDispatch", true).put("probeDispatchAt", 2000.0)
                .put("queryStartedAt", 2000.5).put("notificationDateAt", 2001.0).put("observedAt", 2002.0).put("activeApplication", true)
                .put("keyWindow", true).put("notificationDelegateNil", true).put("applicationResignActiveCount", 0)
                .put("windowResignKeyCount", 0).put("foregroundIntervalUninterrupted", true).put("readOnly", true);
        events.add(event("recv", PREFIX + "deliveryObserved", delivered));
        events.add(acceptance());
        ObjectNode closed = scope(true);
        closed.put("target", "java-sidecar").put("transport", "flow-ipc");
        events.add(event("send", "windowClosed", closed));
        events.add(event("recv", "host.nativeResourceDrainStarted", object("pendingCount", 0, "timeoutSeconds", 2)));
        events.add(event("recv", "host.nativeResourceDrainCompleted", object("drained", true, "pendingCount", 0, "failureCount", 0)));
        events.add(event("send", "shutdown", object("target", "java-sidecar", "transport", "flow-ipc")));
        events.add(event("recv", "shutdown.sidecar", object("mode", "graceful")));
        return events;
    }

    private static void stage(List<ObjectNode> events, int index) {
        ObjectNode pointer = identity(true);
        pointer.put("stage", index == 0 ? "prepare" : "probe").put("index", index)
                .put("inputMode", "appkit-window-pointer-event").put("button", "primary")
                .put("activeApplication", true).put("keyWindow", true)
                .put("webViewX", 10).put("webViewY", 20).put("windowX", 10).put("windowY", 20)
                .put("dispatchedAt", index == 0 ? 1000.0 : 2000.0);
        events.add(event("recv", PREFIX + "nativePointerDispatch", pointer));
        requestOpen(events, true, "stage-" + index, "POST");
        events.add(wire("command", true, "notification-" + index, object("command", "notification.show",
                "serverSequence", index == 0 ? 38 : 44, "serverSequenceScope", "java-sidecar-to-webview",
                "args", object("title", marker(TITLES.get(index).length()), "body", marker(BODY.length())))));
        events.add(wire("capabilityResult", true, "notification-" + index, object("command", "notification.show",
                "success", true, "scheduled", true, "mode", "native-user-notifications",
                "notificationId", marker(43), "title", marker(TITLES.get(index).length()), "body", marker(BODY.length()),
                "permissionMode", "manifest-enforced", "permissionDecision", "allowed",
                "permissionReason", "manifest-allow", "permissionScope", "window:main", "permissionManifestLoaded", true,
                "permissionManifestPath", MANIFEST, "permissionManifestCommands", "notification.show")));
        response(events, true, "stage-" + index);
        ObjectNode push = object("connectionId", "connection-main", "flowUiId", 0, "pushSequence", index + 1,
                "flowServerSyncId", index + 4, "deliveryAttempt", 1, "resync", false, "uidlBytes", 128, "uidlRedacted", true);
        events.add(wire("opaquePush", true, "push-" + index, push));
        ObjectNode ack = push.deepCopy();
        ack.remove("deliveryAttempt"); ack.remove("resync"); ack.remove("uidlBytes"); ack.remove("uidlRedacted");
        ack.put("lastSeenServerSyncId", index + 4);
        events.add(wire("opaquePushAck", true, "ack-" + index, ack));
        ObjectNode observed = identity(true);
        observed.put("index", index).put("stage", index == 0 ? "prepare" : "probe").put("outcome", "SCHEDULED")
                .put("command", "notification.show").put("success", true).put("scheduled", true)
                .put("notificationIdPresent", true).put("errorCode", "").put("change", 1)
                .put("nativePointerReceiptCount", 1).put("nativePointerTrustedCount", 1)
                .put("requestBaseline", index + 2).put("responseBaseline", index + 2).put("pushBaseline", index)
                .put("requestCount", index + 3).put("responseCount", index + 3).put("pushMessageCount", index + 1)
                .put("notificationDigest", index == 0 ? FIRST_DIGEST : PROBE_DIGEST).put("digestAlgorithm", "sha256");
        for (String field : List.of("client", "capabilityContextCaptured", "backgroundSessionAbsent",
                "permissionMetadataValid", "nativePointerReceipt", "nativePointerTrusted")) observed.put(field, true);
        events.add(event("recv", PREFIX + "stageObserved", observed));
    }

    private static ObjectNode acceptance() {
        ObjectNode p = object("nativePointerEventCount", 2, "nativePointerTrustedEventCount", 2,
                "applicationResignActiveCount", 0, "windowResignKeyCount", 0, "nativePointerStages", "prepare,probe",
                "browserProgrammaticStages", "route-navigate", "authorizationBefore", "not-determined",
                "authorizationAfter", "authorized", "bannerVisibilityClaimed", false,
                "mainWindowId", "main", "mainSessionId", "flow-ipc-session", "mainUiId", "main",
                "mainRequestCount", 4, "mainResponseCount", 4, "mainPushMessageCount", 2,
                "sidecarProcessIdentifier", 111, "mainRecoveryGeneration", 0,
                "prepareNotificationDigest", FIRST_DIGEST, "probeNotificationDigest", PROBE_DIGEST,
                "testCleanupIdentifierCount", 2);
        for (String field : List.of("complete", "standardBundleMode", "mainFlowClientOwned", "nativePointerObserved",
                "nativePointerTrustedObserved", "capabilityContextCapturedObserved", "backgroundSessionAbsentObserved",
                "permissionPayloadPreservedObserved", "mainJavaListenerObserved", "mainUidlPatchObserved",
                "mainPushObserved", "balancedDrainObserved", "resultStableAfterDrain", "errorFree",
                "authorizationReadOnlyObserved", "notificationDelegateNil", "foregroundIntervalUninterrupted",
                "deliveredNotificationMatched", "sameMainWindow", "sameMainWebView", "testCleanupRequested")) p.put(field, true);
        return event("recv", PREFIX + "frameworkAcceptance", p);
    }

    private static ObjectNode authorization(boolean before) {
        ObjectNode p = identity(true);
        p.put("phase", before ? "before" : "after").put("authorizationStatus", before ? "not-determined" : "authorized")
                .put("readOnly", true).put("notificationDelegateNil", true);
        return event("recv", PREFIX + "authorizationObserved", p);
    }

    private static void request(List<ObjectNode> events, boolean main, String id, String method) {
        requestOpen(events, main, id, method);
        response(events, main, id);
    }

    private static void requestOpen(List<ObjectNode> events, boolean main, String id, String method) {
        boolean post = method.equals("POST");
        events.add(wire("opaqueHttpStreamOpen", main, id,
                object("requestId", id, "method", method, "contentLength", post ? 10 : 0)));
        if (post) {
            events.add(wire("opaqueHttpStreamRequestCredit", main, id, object("requestId", id, "credit", 4)));
            events.add(wire("opaqueHttpStreamChunk", main, id,
                    object("requestId", id, "chunkSequence", 0, "bodyBytes", 10, "bodyRedacted", true)));
        }
        events.add(wire("opaqueHttpStreamEnd", main, id,
                object("requestId", id, "chunkCount", post ? 1 : 0, "bodyBytes", post ? 10 : 0)));
    }

    private static void response(List<ObjectNode> events, boolean main, String id) {
        events.add(wire("opaqueHttpStreamResponse", main, id, object("requestId", id, "status", 200)));
        events.add(wire("opaqueHttpStreamResponseCredit", main, id, object("requestId", id, "credit", 4)));
        events.add(wire("opaqueHttpStreamResponseChunk", main, id,
                object("requestId", id, "chunkSequence", 0, "bodyBytes", 10, "bodyRedacted", true)));
        events.add(wire("opaqueHttpStreamResponseEnd", main, id, object("requestId", id, "chunkCount", 1, "bodyBytes", 10)));
    }

    private static void connect(List<ObjectNode> events, boolean main) {
        String id = "connection-" + (main ? "main" : "secondary");
        events.add(wire("opaquePushConnect", main, id,
                object("connectionId", id, "flowUiId", 0, "transport", "opaque-ipc-push")));
        events.add(wire("opaquePushConnected", main, id,
                object("connectionId", id, "flowUiId", 0, "transport", "opaque-ipc-push")));
    }

    private static ObjectNode wire(String type, boolean main, String id, ObjectNode body) {
        boolean send = Set.of("capabilityResult", "opaqueHttpStreamOpen", "opaqueHttpStreamChunk",
                "opaqueHttpStreamEnd", "opaqueHttpStreamResponseCredit", "opaquePushConnect", "opaquePushAck",
                "opaquePushDisconnect", "opaquePushResync").contains(type);
        ObjectNode envelope = scope(main);
        envelope.put("version", 1).put("type", type).put("id", type.equals("capabilityResult") ? "capability-" + id : id)
                .put("correlationId", id).putNull("error").set("payload", body);
        ObjectNode wrapper = object(send ? "target" : "source", "java-sidecar", "transport", "flow-ipc",
                "sidecarGeneration", 1, "envelope", envelope);
        if (type.equals("capabilityResult")) wrapper.put("command", body.path("command").asString());
        return event(send ? "send" : "recv", type, wrapper);
    }


    private static ObjectNode identity(boolean ignored) {
        return scope(true).put("sidecarProcessIdentifier", 111).put("recoveryGeneration", 0);
    }

    private static ObjectNode scope(boolean ignored) {
        return object("windowId", "main", "sessionId", "flow-ipc-session", "uiId", "main");
    }

    private static String marker(int count) { return "[redacted length=" + count + "]"; }

    private static ObjectNode event(String direction, String type, ObjectNode payload) {
        return object("direction", direction, "type", type, "payload", payload);
    }

    private static ObjectNode object(Object... pairs) {
        ObjectNode value = JSON.createObjectNode();
        for (int i = 0; i < pairs.length; i += 2) {
            String name = (String) pairs[i]; Object field = pairs[i + 1];
            if (field instanceof String s) value.put(name, s);
            else if (field instanceof Integer n) value.put(name, n);
            else if (field instanceof Double n) value.put(name, n);
            else if (field instanceof Boolean b) value.put(name, b);
            else if (field instanceof JsonNode node) value.set(name, node);
            else throw new IllegalArgumentException("Unsupported test field");
        }
        return value;
    }

    private static ObjectNode payload(ObjectNode e) { return (ObjectNode) e.path("payload"); }
    private static ObjectNode envelope(ObjectNode e) { return (ObjectNode) payload(e).path("envelope"); }
    private static ObjectNode body(ObjectNode e) { return (ObjectNode) envelope(e).path("payload"); }
    private static String type(ObjectNode e) { return e.path("type").asString(); }

    private static int find(List<ObjectNode> events, String type, int ordinal) {
        for (int i = 0; i < events.size(); i++) if (type.equals(type(events.get(i))) && ordinal-- == 0) return i;
        throw new IllegalArgumentException("Missing event " + type);
    }

    private static int findNotification(List<ObjectNode> events, String type, int ordinal) {
        for (int i = 0; i < events.size(); i++) if (type.equals(type(events.get(i)))
                && "notification.show".equals(body(events.get(i)).path("command").asString("")) && ordinal-- == 0) return i;
        throw new IllegalArgumentException("Missing notification " + type);
    }

    private static List<ObjectNode> copy(List<ObjectNode> original) {
        return new ArrayList<>(original.stream().map(ObjectNode::deepCopy).toList());
    }

    private static void mutate(Path trace, List<ObjectNode> valid, int index,
                               Consumer<ObjectNode> mutation, String label) throws Exception {
        List<ObjectNode> mutated = copy(valid);
        mutation.accept(mutated.get(index));
        if (mutated.equals(valid)) throw new IllegalStateException("Mutation was a no-op: " + label);
        assertTrace(trace, mutated, false, label);
    }

    private static void reorder(Path trace, List<ObjectNode> valid, int from, int to, String label) throws Exception {
        List<ObjectNode> mutated = copy(valid);
        ObjectNode removed = mutated.remove(from);
        mutated.add(to, removed);
        assertTrace(trace, mutated, false, label);
    }

    private static void assertTrace(Path trace, List<ObjectNode> events, boolean expected, String label) throws Exception {
        assertTrace(trace, events, expected, label, false);
    }

    private static void assertTrace(Path trace, List<ObjectNode> events, boolean expected,
                                    String label, boolean escapeSecrets) throws Exception {
        StringBuilder content = new StringBuilder();
        for (ObjectNode event : events) content.append("HOST_PROTOCOL ").append(JSON.writeValueAsString(event)).append('\n');
        String serialized = content.toString();
        if (escapeSecrets) serialized = serialized.replace("Vatori", "\\u0056atori")
                .replace("vatori-", "\\u0076atori-").replace("Native foreground", "\\u004eative foreground");
        Files.writeString(trace, serialized, StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream(), errors = new ByteArrayOutputStream();
        int status = StandardBundleTraceInspector.run(new String[]{GATE, trace.toString()},
                new PrintStream(output), new PrintStream(errors));
        if (status != (expected ? 0 : 2) || !(expected ? output : errors).toString(StandardCharsets.UTF_8)
                .contains((expected ? "PASS " : "FAIL ") + GATE)) {
            throw new AssertionError(label + ": exit=" + status + "\n" + output + errors);
        }
        if (expected) positive++; else rejected++;
    }
}
