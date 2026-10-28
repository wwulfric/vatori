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

/** Synthetic contract checks; these do not replace actual OS permission/UI evidence. */
public final class NotificationOutcomesTraceGateSmokeMain {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String GATE = "--require-flow-ipc-notification-outcomes";
    private static final String PREFIX = "flowNotificationOutcomes.";
    private static final String SECONDARY = "notification-outcomes-secondary";
    private static final String SESSION = "flow-ipc-window:71149f39-43ca-4d8c-b7ac-91ad1a8a2345";
    private static final String MANIFEST = "$APP/Contents/Resources/desktop-capabilities.json";
    private static int positive;
    private static int rejected;

    private NotificationOutcomesTraceGateSmokeMain() { }

    public static void main(String[] args) throws Exception {
        Path trace = Files.createTempFile("notification-outcomes-inspector-", ".log");
        try {
            for (String outcome : List.of("AUTHORIZATION_REQUIRED", "OS_DENIED", "SCHEDULED")) {
                List<ObjectNode> valid = valid(outcome);
                assertTrace(trace, valid, true, outcome);
                dropAndDuplicateRequiredEvents(trace, valid);
                mutateAcceptance(trace, valid);
                mutateNative(trace, valid);
                rejectNativeErrorDiagnostics(trace, valid);
                rejectUnexpectedNativeErrorEvents(trace, valid);
                mutateStages(trace, valid);
                mutateTransport(trace, valid);
                mutateLifecycle(trace, valid);
                List<ObjectNode> retry = copy(valid);
                int p = find(retry, "opaquePush", 0), ack = find(retry, "opaquePushAck", 0);
                ObjectNode retried = retry.get(p).deepCopy();
                body(retried).put("deliveryAttempt", 2);
                // Permit an already in-flight retry and an ACK whose lastSeen
                // lags its logical sync id. Require ordinal—not alternating—causality.
                body(retry.get(ack)).put("lastSeenServerSyncId", -1);
                retry.add(ack, retried);
                retry.add(ack + 2, retry.get(ack + 1).deepCopy());
                assertTrace(trace, retry, true, outcome + " overlapping retry/early ACK");
                List<ObjectNode> closed = copy(valid);
                closed.add(find(closed, "windowClosed", 0), wire("opaquePushDisconnect", false,
                        "disconnect-secondary", object("connectionId", "connection-secondary",
                                "flowUiId", 0, "transport", "opaque-ipc-push")));
                assertTrace(trace, closed, true, outcome + " disconnect after acceptance");
            }
            System.out.printf("Notification outcomes trace gate smoke passed: %d positives, %d rejections%n",
                    positive, rejected);
        } finally {
            Files.deleteIfExists(trace);
        }
    }

    private static void dropAndDuplicateRequiredEvents(Path trace, List<ObjectNode> valid) throws Exception {
        Set<String> required = Set.of("launch", "launch.sidecar", "sidecar.started", "ready",
                "capabilityManifest", "window.create", "windowClosed", "standardBundleReady",
                "standardBundleResource", "webview.firstMeaningfulPaint", "command", "capabilityResult",
                "opaqueHttpStreamOpen", "opaqueHttpStreamResponse", "opaqueHttpStreamResponseChunk",
                "opaqueHttpStreamResponseEnd", "opaqueHttpStreamEnd", "opaqueHttpStreamChunk",
                "opaquePushConnect", "opaquePushConnected", "opaquePush", "opaquePushAck",
                "host.nativeResourceDrainStarted", "host.nativeResourceDrainCompleted", "shutdown.sidecar");
        for (int i = 0; i < valid.size(); i++) {
            String type = type(valid.get(i));
            if (!required.contains(type) && !type.startsWith(PREFIX)) continue;
            List<ObjectNode> dropped = copy(valid);
            dropped.remove(i);
            assertTrace(trace, dropped, false, "drop " + type + " at " + i);
            // Broad runtime evidence can legitimately repeat; focused cardinality
            // and wire events cannot silently duplicate.
            if (Set.of("launch", "standardBundleReady", "standardBundleResource",
                    "webview.firstMeaningfulPaint").contains(type)) continue;
            List<ObjectNode> duplicated = copy(valid);
            duplicated.add(i, duplicated.get(i).deepCopy());
            assertTrace(trace, duplicated, false, "duplicate " + type + " at " + i);
        }
    }

    private static void mutateAcceptance(Path trace, List<ObjectNode> valid) throws Exception {
        int index = find(valid, PREFIX + "frameworkAcceptance", 0);
        ObjectNode accepted = payload(valid.get(index));
        for (String name : accepted.propertyNames()) {
            mutate(trace, valid, index, event -> payload(event).remove(name), "missing acceptance " + name);
            mutate(trace, valid, index, event -> payload(event).put(name, "malformed"),
                    "malformed acceptance " + name);
        }
        mutate(trace, valid, index, e -> payload(e).put("bannerVisibilityClaimed", true), "no banner claim");
        mutate(trace, valid, index, e -> payload(e).put("expectedOutcome", "DISPLAYED"), "unknown outcome");
        for (int n = 0; n < 2; n++) {
            int authorization = find(valid, PREFIX + "authorizationObserved", n);
            for (String field : List.of("phase", "authorizationStatus", "expectedOutcome",
                    "windowId", "sessionId", "uiId", "readOnly", "sidecarProcessIdentifier", "recoveryGeneration")) {
                mutate(trace, valid, authorization, e -> payload(e).remove(field), "missing OS observation " + field);
                mutate(trace, valid, authorization, e -> payload(e).put(field, "wrong"), "wrong OS observation " + field);
            }
            reorder(trace, valid, authorization, index, "OS observation after final acceptance");
        }
    }

    private static void mutateNative(Path trace, List<ObjectNode> valid) throws Exception {
        for (String type : List.of("command", "capabilityResult")) {
            int ordinal = 0;
            for (int index = 0; index < valid.size(); index++) {
                ObjectNode event = valid.get(index);
                if (!type.equals(type(event))) continue;
                int row = index;
                for (String field : List.of("version", "type", "id", "correlationId", "windowId", "sessionId", "uiId", "error")) {
                    mutate(trace, valid, row, e -> envelope(e).remove(field), "missing native envelope " + field);
                    mutate(trace, valid, row, e -> envelope(e).put(field, "wrong"), "wrong native envelope " + field);
                }
                for (String field : List.of("transport", "sidecarGeneration",
                        type.equals("command") ? "source" : "target")) {
                    mutate(trace, valid, row, e -> payload(e).remove(field), "missing native wrapper " + field);
                    mutate(trace, valid, row, e -> payload(e).put(field, "wrong"), "wrong native wrapper " + field);
                }
                mutate(trace, valid, row, e -> e.put("direction", "opposite"), "wrong native direction");
                mutate(trace, valid, row, e -> payload(e).put("sidecarGeneration", 2), "native wrong generation");
                if (!"notification.show".equals(body(event).path("command").asString(""))) continue;
                for (String field : List.copyOf(body(event).propertyNames())) {
                    mutate(trace, valid, row, e -> body(e).remove(field), "missing native payload " + field);
                    mutate(trace, valid, row, e -> body(e).put(field, "wrong"), "malformed native payload " + field);
                }
                if (type.equals("command")) {
                    for (String field : List.of("title", "body")) {
                        mutate(trace, valid, row, e -> ((ObjectNode) body(e).path("args")).put(field, "private unmasked"),
                                "unmasked notification argument " + field);
                    }
                } else {
                    for (String field : List.of("notificationId", "title", "body", "error", "canceled", "cancelReason",
                            "operationInvalidated")) {
                        mutate(trace, valid, row, e -> body(e).put(field, "private unmasked"),
                                "contradictory or unmasked result " + field);
                    }
                }
                ordinal++;
            }
            if (ordinal != 3) throw new IllegalStateException("Three notification operations expected");
        }
        int firstCommand = find(valid, "command", 0);
        int firstResult = find(valid, "capabilityResult", 0);
        reorder(trace, valid, firstResult, firstCommand, "result before command");
        int windowCommand = find(valid, "command", 2);
        for (String field : List.of("id", "route", "title", "width", "height")) {
            mutate(trace, valid, windowCommand, e -> ((ObjectNode) body(e).path("args")).remove(field),
                    "missing public window descriptor " + field);
            mutate(trace, valid, windowCommand, e -> ((ObjectNode) body(e).path("args")).put(field, "wrong"),
                    "malformed public window descriptor " + field);
        }
        int created = find(valid, "window.create", 1);
        reorder(trace, valid, created, windowCommand, "secondary created before its command");
        reorder(trace, valid, created, find(valid, "capabilityResult", 2),
                "secondary create occurs before success result");
    }

    private static void mutateStages(Path trace, List<ObjectNode> valid) throws Exception {
        for (String type : List.of(PREFIX + "nativePointerDispatch", PREFIX + "stageObserved")) {
            for (int ordinal = 0; ordinal < 3; ordinal++) {
                int index = find(valid, type, ordinal);
                for (String field : payload(valid.get(index)).propertyNames()) {
                    mutate(trace, valid, index, e -> payload(e).remove(field), "missing stage/pointer " + field);
                    mutate(trace, valid, index, e -> payload(e).put(field, "wrong"), "wrong stage/pointer " + field);
                }
                mutate(trace, valid, index, e -> e.put("direction", "send"), "wrong stage direction");
                int command = findNotification(valid, "command", ordinal);
                reorder(trace, valid, command, index, "command before pointer or Java state");
            }
        }
    }

    private static void rejectNativeErrorDiagnostics(Path trace, List<ObjectNode> valid) throws Exception {
        List<JsonNode> values = List.of(
                object().putNull("value").path("value"),
                object("value", "UNErrorDomain").path("value"),
                object("value", "other").path("value"),
                object("value", "").path("value"),
                object("value", 1).path("value"),
                object("value", -1).path("value"),
                object("value", "1").path("value"),
                object("value", true).path("value"),
                object().put("value", 1.5).path("value"),
                JSON.createArrayNode(), object());
        for (int stage = 0; stage < 3; stage++) {
            int result = findNotification(valid, "capabilityResult", stage);
            for (String field : List.of("nativeErrorDomain", "nativeErrorCode")) {
                for (JsonNode value : values) {
                    mutate(trace, valid, result, e -> body(e).set(field, value.deepCopy()),
                            "native error diagnostic in successful acceptance stage " + stage + ": " + field);
                }
            }
            for (String domain : List.of("UNErrorDomain", "other")) {
                for (JsonNode value : values) {
                    mutate(trace, valid, result, e -> {
                        body(e).put("nativeErrorDomain", domain);
                        body(e).set("nativeErrorCode", value.deepCopy());
                    }, "paired native diagnostics in successful acceptance stage " + stage);
                }
            }
            mutate(trace, valid, result, e -> body(e).putNull("nativeErrorDomain")
                            .putNull("nativeErrorCode"),
                    "paired null native diagnostics in successful acceptance stage " + stage);
            mutate(trace, valid, result, e -> body(e).put("nativeErrorDomain", 1)
                            .put("nativeErrorCode", "UNErrorDomain"),
                    "paired wrong-type native diagnostics in successful acceptance stage " + stage);
        }
    }

    private static void rejectUnexpectedNativeErrorEvents(Path trace, List<ObjectNode> valid) throws Exception {
        for (String code : List.of("notification-authorization-failed", "notification-scheduling-failed")) {
            ObjectNode diagnostic = scope(true);
            diagnostic.put("outcome", "ERROR").put("errorCode", code)
                    .put("client", true).put("nativePointerTrusted", true).put("visibleStatusObserved", true)
                    .put("requestCount", 4).put("responseCount", 4).put("pushMessageCount", 2);
            for (String direction : List.of("recv", "send")) {
                for (int position : List.of(0, find(valid, PREFIX + "frameworkAcceptance", 0), valid.size())) {
                    List<ObjectNode> rejected = copy(valid);
                    rejected.add(position, event(direction, PREFIX + "unexpectedErrorObserved", diagnostic.deepCopy()));
                    assertTrace(trace, rejected, false,
                            "unexpected native error cannot coexist with complete acceptance at " + position);
                }
            }
        }
        for (ObjectNode diagnostic : List.of(object(), object("outcome", "OS_DENIED"))) {
            List<ObjectNode> rejected = copy(valid);
            rejected.add(event("recv", PREFIX + "unexpectedErrorObserved", diagnostic));
            assertTrace(trace, rejected, false, "malformed unexpected error evidence is not ignored");
        }
    }

    private static void mutateTransport(Path trace, List<ObjectNode> valid) throws Exception {
        for (String type : List.of("opaqueHttpStreamOpen", "opaqueHttpStreamResponse",
                "opaqueHttpStreamEnd", "opaqueHttpStreamChunk", "opaqueHttpStreamRequestCredit",
                "opaqueHttpStreamResponseEnd", "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseCredit",
                "opaquePushConnect", "opaquePushConnected", "opaquePush", "opaquePushAck")) {
            int index = find(valid, type, 0);
            for (String field : List.of("version", "type", "windowId", "sessionId", "uiId", "error")) {
                mutate(trace, valid, index, e -> envelope(e).remove(field), "missing stream envelope " + field);
                mutate(trace, valid, index, e -> envelope(e).put(field, "wrong"), "wrong stream envelope " + field);
            }
            if (type.startsWith("opaqueHttpStream")) {
                mutate(trace, valid, index, e -> envelope(e).remove("id"), "missing HTTP envelope id");
                mutate(trace, valid, index, e -> envelope(e).put("id", 1), "non-string HTTP envelope id");
                mutate(trace, valid, index, e -> envelope(e).put("id", " "), "blank HTTP envelope id");
                mutate(trace, valid, index, e -> envelope(e).remove("correlationId"), "missing HTTP correlation");
                mutate(trace, valid, index, e -> envelope(e).put("correlationId", 1), "non-string HTTP correlation");
                mutate(trace, valid, index, e -> envelope(e).put("correlationId", " "), "blank HTTP correlation");
                mutate(trace, valid, index, e -> envelope(e).put("correlationId", "another-request"),
                        "HTTP correlation differs from payload request id");
            }
            mutate(trace, valid, index, e -> payload(e).put("sidecarGeneration", 2), "wrong stream generation");
            mutate(trace, valid, index, e -> e.put("direction", "wrong"), "wrong stream direction");
            for (String field : body(valid.get(index)).propertyNames()) {
                if (Set.of("method", "path").contains(field)) continue;
                if (!field.equals("contentLength"))
                    mutate(trace, valid, index, e -> body(e).remove(field), "missing stream body " + field);
                mutate(trace, valid, index, e -> body(e).put(field, "wrong"), "wrong stream body " + field);
            }
        }
        int ack = find(valid, "opaquePushAck", 0), push = find(valid, "opaquePush", 0);
        reorder(trace, valid, ack, push, "ACK before push");
        mutate(trace, valid, push, e -> body(e).put("deliveryAttempt", 2), "initial push attempts start at one");
        mutate(trace, valid, push, e -> body(e).put("resync", true), "unsolicited resync push");
        mutate(trace, valid, push, e -> body(e).put("uidl", "raw confidential UIDL"), "unredacted UIDL");
        int chunk = find(valid, "opaqueHttpStreamResponseChunk", 0);
        mutate(trace, valid, chunk, e -> body(e).put("bodyBase64", "YQ=="), "unredacted response bytes");
        int requestChunk = find(valid, "opaqueHttpStreamChunk", 0);
        mutate(trace, valid, requestChunk, e -> body(e).put("bodyBase64", "YQ=="), "unredacted request bytes");
        List<ObjectNode> alien = copy(valid);
        for (ObjectNode event : alien)
            if (Set.of("opaquePush", "opaquePushAck").contains(type(event)))
                body(event).put("connectionId", "never-connected");
        assertTrace(trace, alien, false, "consistent push/ACK for a never-connected id");
        for (int[] sequences : List.of(new int[]{1, 4}, new int[]{4, 2})) {
            List<ObjectNode> gap = copy(valid);
            for (String type : List.of("opaquePush", "opaquePushAck")) {
                body(gap.get(find(gap, type, 0))).put("pushSequence", sequences[0]);
                body(gap.get(find(gap, type, 1))).put("pushSequence", sequences[1]);
            }
            assertTrace(trace, gap, false, "matching push/ACK with gapped or decreasing logical sequence");
        }
        for (String type : List.of("opaquePushDisconnect", "opaquePushResync")) {
            List<ObjectNode> interrupted = copy(valid);
            interrupted.add(push, wire(type, true, "interrupted-main",
                    object("connectionId", "connection-main", "flowUiId", 0, "transport", "opaque-ipc-push")));
            assertTrace(trace, interrupted, false, "unexpected pre-acceptance " + type);
        }
        reorder(trace, valid, find(valid, "opaquePushConnected", 0),
                find(valid, "shutdown.sidecar", 0), "late connected does not establish historical connection");
    }

    private static void mutateLifecycle(Path trace, List<ObjectNode> valid) throws Exception {
        int manifest = find(valid, "capabilityManifest", 0);
        mutate(trace, valid, manifest, e -> payload(e).put("permissionMode", "allow-all-dev"), "permissive manifest");
        mutate(trace, valid, manifest, e -> payload(e).put("loaded", false), "unloaded manifest");
        mutate(trace, valid, manifest, e -> payload(e).put("path", "/private/manifest.json"), "wrong manifest path");
        mutate(trace, valid, manifest, e -> payload(e).put("parseError", "invalid"), "manifest parse failure");
        mutate(trace, valid, manifest, e -> ((ObjectNode) payload(e).path("commandsByWindow"))
                .set(SECONDARY, JSON.createArrayNode().add("notification.show")), "secondary improperly allowed");
        for (String type : List.of("sidecar.started", "launch.sidecar", "host.nativeResourceDrainStarted",
                "host.nativeResourceDrainCompleted", "shutdown.sidecar")) {
            int index = find(valid, type, 0);
            for (String field : List.copyOf(payload(valid.get(index)).propertyNames())) {
                if (field.equals("timeoutSeconds")) continue;
                mutate(trace, valid, index, e -> payload(e).put(field, "wrong"), "wrong lifecycle " + field);
            }
        }
        for (String type : List.of("sidecar.recoveryReady", "sidecar.replacementRequested",
                "nativeCapabilityOperation.invalidated", "nativeCapabilityOperation.completionDropped",
                "staleCapabilityResultDropped", "windowClosedDropped", "sidecar.exit", "error", "webview.error")) {
            List<ObjectNode> bad = copy(valid);
            bad.add(event("recv", type, object()));
            assertTrace(trace, bad, false, "unexpected " + type);
        }
        for (ObjectNode exit : List.of(object("expected", true, "status", 1),
                object("expected", false, "status", 0))) {
            List<ObjectNode> bad = copy(valid);
            bad.add(event("recv", "sidecar.exit", exit));
            assertTrace(trace, bad, false, "nonzero or unexpected JVM exit");
        }
    }

    private static List<ObjectNode> valid(String outcome) {
        List<ObjectNode> events = new ArrayList<>();
        events.add(event("recv", "capabilityManifest", object("path", MANIFEST,
                "loaded", true, "permissionMode", "manifest-enforced", "parseError", "",
                "commandsByWindow", object("main", JSON.createArrayNode().add("notification.show").add("window.open"),
                        SECONDARY, JSON.createArrayNode().add("window.close").add("window.open")))));
        events.add(event("recv", "launch", object("transport", "flow-ipc")));
        events.add(event("send", "launch.sidecar", object("generation", 1, "transport", "flow-ipc")));
        events.add(event("recv", "sidecar.started", object("generation", 1, "processIdentifier", 111, "transport", "flow-ipc")));
        events.add(wire("ready", true, "ready", object(
                "runtime", "standard-vaadin-bundle-opaque-ipc", "browserRequestTransport", "opaque-http-stream-ipc",
                "transport", "flow-ipc", "flowClientResourceLoading", "standard-production-bundle")));
        events.add(created(true));
        events.add(event("recv", "standardBundleReady", object("requestTransport", "opaque-http-stream-ipc")));
        events.add(event("recv", "standardBundleResource", object("path", "/VAADIN/build/FlowClient-notification.js")));
        events.add(event("recv", "webview.firstMeaningfulPaint", object()));
        request(events, true, "bootstrap", "GET");
        request(events, true, "route", "GET");
        connect(events, true);
        events.add(authorization(true, outcome));
        stage(events, outcome, 0, 2, 0);
        stage(events, outcome, 1, 3, 1);
        events.add(authorization(false, outcome));
        requestOpen(events, true, "open-secondary", "POST");
        events.add(wire("command", true, "open-window", object("command", "window.open",
                "args", object("id", SECONDARY, "route", "flow-notification-outcomes",
                        "title", "Flow notification permission denied", "width", 760, "height", 620))));
        events.add(created(false));
        ObjectNode opened = object("command", "window.open", "success", true, "windowId", SECONDARY,
                "route", "flow-notification-outcomes", "permissionDecision", "allowed", "permissionReason", "manifest-allow");
        events.add(wire("capabilityResult", true, "open-window", opened));
        response(events, true, "open-secondary");
        request(events, false, "secondary-bootstrap", "GET");
        request(events, false, "secondary-route", "GET");
        connect(events, false);
        stage(events, outcome, 2, 2, 0);
        events.add(acceptance(outcome));
        for (boolean main : List.of(false, true)) {
            ObjectNode closed = scope(main);
            closed.put("target", "java-sidecar").put("transport", "flow-ipc");
            events.add(event("send", "windowClosed", closed));
        }
        events.add(event("recv", "host.nativeResourceDrainStarted", object("pendingCount", 0, "timeoutSeconds", 2)));
        events.add(event("recv", "host.nativeResourceDrainCompleted", object("drained", true, "pendingCount", 0, "failureCount", 0)));
        events.add(event("send", "shutdown", object("target", "java-sidecar", "transport", "flow-ipc")));
        events.add(event("recv", "shutdown.sidecar", object("mode", "graceful")));
        return events;
    }

    private static void stage(List<ObjectNode> events, String outcome, int index, int count, int pushes) {
        boolean main = index < 2, scheduled = index == 1 && outcome.equals("SCHEDULED");
        String stage = List.of("main-invalid", "main-show", "secondary-denied").get(index);
        ObjectNode pointer = identity(main);
        pointer.put("stage", stage).put("index", index).put("inputMode", "appkit-window-pointer-event")
                .put("button", "primary").put("activeApplication", true).put("keyWindow", true)
                .put("webViewX", 10).put("webViewY", 20).put("windowX", 10).put("windowY", 20);
        events.add(event("recv", PREFIX + "nativePointerDispatch", pointer));
        String request = "stage-" + index;
        requestOpen(events, main, request, "POST");
        events.add(wire("command", main, "notification-" + index, object("command", "notification.show",
                "args", object("title", marker(List.of(1, 30, 26).get(index)), "body", marker(36)))));
        ObjectNode result = permission(main);
        result.put("command", "notification.show").put("success", scheduled);
        if (main) {
            result.put("mode", "native-user-notifications").put("scheduled", scheduled);
            if (scheduled) result.put("notificationId", marker(43)).put("title", marker(30)).put("body", marker(36));
            else result.put("errorCode", errorCode(outcome, index)).put("error", marker(36));
        } else result.put("error", marker(53));
        events.add(wire("capabilityResult", main, "notification-" + index, result));
        response(events, main, request);
        int sequence = pushes + 1;
        ObjectNode push = object("connectionId", "connection-" + (main ? "main" : "secondary"),
                "flowUiId", 0, "pushSequence", sequence, "flowServerSyncId", sequence + 3,
                "deliveryAttempt", 1, "resync", false, "uidlBytes", 128, "uidlRedacted", true);
        events.add(wire("opaquePush", main, "push-" + index, push));
        ObjectNode ack = push.deepCopy();
        ack.remove("deliveryAttempt"); ack.remove("resync"); ack.remove("uidlBytes"); ack.remove("uidlRedacted");
        ack.put("lastSeenServerSyncId", sequence + 3);
        events.add(wire("opaquePushAck", main, "ack-" + index, ack));
        ObjectNode observed = identity(main);
        observed.put("index", index).put("stage", stage)
                .put("outcome", index == 0 ? "ERROR" : index == 1 ? outcome : "DENIED")
                .put("command", "notification.show").put("success", scheduled).put("scheduled", scheduled)
                .put("notificationIdPresent", scheduled).put("errorCode", errorCode(outcome, index))
                .put("change", 1).put("nativePointerReceiptCount", 1).put("nativePointerTrustedCount", 1)
                .put("requestBaseline", count).put("responseBaseline", count).put("pushBaseline", pushes)
                .put("requestCount", count + 1).put("responseCount", count + 1).put("pushMessageCount", pushes + 1);
        for (String field : List.of("client", "capabilityContextCaptured", "backgroundSessionAbsent",
                "permissionMetadataValid", "nativePointerReceipt", "nativePointerTrusted")) observed.put(field, true);
        events.add(event("recv", PREFIX + "stageObserved", observed));
    }

    private static ObjectNode acceptance(String outcome) {
        ObjectNode p = object("expectedOutcome", outcome, "invalidOutcome", "ERROR", "deniedOutcome", "DENIED",
                "nativePointerEventCount", 3, "nativePointerTrustedEventCount", 3,
                "nativePointerStages", "main-invalid,main-show,secondary-denied",
                "browserProgrammaticStages", "route-navigate,secondary-open",
                "authorizationBefore", "not-determined", "authorizationAfter", after(outcome),
                "bannerVisibilityClaimed", false, "mainWindowId", "main", "mainSessionId", "flow-ipc-session",
                "mainUiId", "main", "secondaryWindowId", SECONDARY, "secondarySessionId", SESSION, "secondaryUiId", SECONDARY,
                "mainRequestCount", 5, "mainResponseCount", 5, "mainPushMessageCount", 2,
                "secondaryRequestCount", 3, "secondaryResponseCount", 3, "secondaryPushMessageCount", 1,
                "sidecarProcessIdentifier", 111, "mainRecoveryGeneration", 0, "secondaryRecoveryGeneration", 0);
        for (String field : List.of("complete", "standardBundleMode", "mainFlowClientOwned", "secondaryFlowClientOwned",
                "scopeIsolationObserved", "nativePointerObserved", "nativePointerTrustedObserved", "capabilityContextCapturedObserved",
                "backgroundSessionAbsentObserved", "permissionPayloadPreservedObserved", "mainJavaListenerObserved", "secondaryJavaListenerObserved",
                "mainUidlPatchObserved", "secondaryUidlPatchObserved", "mainPushObserved", "secondaryPushObserved",
                "balancedDrainObserved", "resultStableAfterDrain", "errorFree", "authorizationReadOnlyObserved",
                "sameMainWindow", "sameMainWebView", "sameSecondaryWindow", "sameSecondaryWebView")) p.put(field, true);
        return event("recv", PREFIX + "frameworkAcceptance", p);
    }

    private static ObjectNode authorization(boolean before, String outcome) {
        ObjectNode p = identity(true);
        p.put("phase", before ? "before" : "after").put("authorizationStatus", before ? "not-determined" : after(outcome))
                .put("expectedOutcome", outcome).put("readOnly", true);
        return event("recv", PREFIX + "authorizationObserved", p);
    }

    private static String after(String outcome) {
        return outcome.equals("SCHEDULED") ? "authorized" : outcome.equals("OS_DENIED") ? "denied" : "not-determined";
    }

    private static String errorCode(String outcome, int index) {
        if (index == 0) return "notification-title-invalid";
        return index == 2 || outcome.equals("SCHEDULED") ? "" : outcome.equals("OS_DENIED")
                ? "notification-authorization-denied" : "notification-authorization-required";
    }

    private static ObjectNode permission(boolean main) {
        return object("permissionMode", "manifest-enforced", "permissionDecision", main ? "allowed" : "denied",
                "permissionReason", main ? "manifest-allow" : "manifest-denied",
                "permissionScope", "window:" + (main ? "main" : SECONDARY), "permissionManifestLoaded", true,
                "permissionManifestPath", MANIFEST, "permissionManifestCommands",
                main ? "notification.show,window.open" : "window.close,window.open");
    }

    private static ObjectNode created(boolean main) {
        ObjectNode p = scope(main);
        p.put("label", main ? "main" : SECONDARY).put("mode", "flow-ipc");
        return event("send", "window.create", p);
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

    private static ObjectNode identity(boolean main) {
        ObjectNode p = scope(main);
        return p.put("sidecarProcessIdentifier", 111).put("recoveryGeneration", 0);
    }

    private static ObjectNode scope(boolean main) {
        return object("windowId", main ? "main" : SECONDARY, "sessionId", main ? "flow-ipc-session" : SESSION,
                "uiId", main ? "main" : SECONDARY);
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
        StringBuilder content = new StringBuilder();
        for (ObjectNode event : events) content.append("HOST_PROTOCOL ").append(JSON.writeValueAsString(event)).append('\n');
        Files.writeString(trace, content, StandardCharsets.UTF_8);
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
