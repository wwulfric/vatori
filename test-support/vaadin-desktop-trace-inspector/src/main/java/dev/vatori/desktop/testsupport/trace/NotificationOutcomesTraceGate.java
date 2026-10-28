package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Focused real-WKWebView notification evidence, independent of component semantics. */
final class NotificationOutcomesTraceGate {
    private static final String PREFIX = "flowNotificationOutcomes.";
    private static final String SECONDARY = "notification-outcomes-secondary";
    private static final String MANIFEST = "$APP/Contents/Resources/desktop-capabilities.json";
    private static final List<String> STAGES =
            List.of("main-invalid", "main-show", "secondary-denied");
    private static final Set<String> STREAM_TYPES = Set.of(
            "opaqueHttpStreamOpen", "opaqueHttpStreamChunk", "opaqueHttpStreamEnd",
            "opaqueHttpStreamRequestCredit", "opaqueHttpStreamResponse", "opaqueHttpStreamResponseCredit",
            "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseEnd",
            "opaquePushConnect", "opaquePushConnected", "opaquePush", "opaquePushAck");
    private static final Set<String> SEND_TYPES = Set.of(
            "capabilityResult", "opaqueHttpStreamOpen", "opaqueHttpStreamChunk",
            "opaqueHttpStreamEnd", "opaqueHttpStreamResponseCredit", "opaquePushConnect", "opaquePushAck");
    private static final List<String> ACCEPTANCE_TRUE = List.of(
            "complete", "standardBundleMode", "mainFlowClientOwned", "secondaryFlowClientOwned",
            "scopeIsolationObserved", "nativePointerObserved", "nativePointerTrustedObserved",
            "capabilityContextCapturedObserved", "backgroundSessionAbsentObserved",
            "permissionPayloadPreservedObserved", "mainJavaListenerObserved",
            "secondaryJavaListenerObserved", "mainUidlPatchObserved", "secondaryUidlPatchObserved",
            "mainPushObserved", "secondaryPushObserved", "balancedDrainObserved",
            "resultStableAfterDrain", "errorFree", "authorizationReadOnlyObserved",
            "sameMainWindow", "sameMainWebView", "sameSecondaryWindow", "sameSecondaryWebView");
    private static final Set<String> PERMISSION_KEYS = Set.of(
            "permissionMode", "permissionDecision", "permissionReason", "permissionScope",
            "permissionManifestLoaded", "permissionManifestPath", "permissionManifestCommands");
    private final List<Event> events = new ArrayList<>();
    private boolean leaked;
    private List<String> missing;
    private String secondarySession;
    private long pid;
    private String outcome;
    private Event accepted;

    void acceptRawLine(String line) {
        leaked |= List.of("Vatori notification acceptance", "Vatori denied notification",
                "Native capability outcome validation",
                "Notification title must not be blank",
                "Notification permission was denied",
                "Notification permission requires an active authorization prompt")
                .stream().anyMatch(line::contains);
    }

    void accept(JsonNode root) {
        events.add(new Event(root, events.size()));
    }

    void require(List<String> missing) {
        this.missing = missing;
        accepted = one(PREFIX + "frameworkAcceptance");
        List<Event> pointers = of(PREFIX + "nativePointerDispatch");
        List<Event> stages = of(PREFIX + "stageObserved");
        List<Event> authorization = of(PREFIX + "authorizationObserved");
        check(accepted != null && pointers.size() == 3 && stages.size() == 3
                && authorization.size() == 2, "exact acceptance, three pointers/stages and two OS observations");
        check(of(PREFIX + "unexpectedErrorObserved").isEmpty(),
                "no unexpected native API error observed");
        check(events.stream().filter(e -> e.type().startsWith(PREFIX)).count() == 9,
                "no unknown or duplicated notification acceptance events");
        check(!leaked, "notification content and error text are not present in raw trace");
        if (accepted == null || pointers.size() != 3 || stages.size() != 3 || authorization.size() != 2) return;
        JsonNode a = accepted.payload();
        outcome = text(a, "expectedOutcome");
        secondarySession = text(a, "secondarySessionId");
        pid = integer(a, "sidecarProcessIdentifier");
        check(Set.of("AUTHORIZATION_REQUIRED", "OS_DENIED", "SCHEDULED").contains(outcome),
                "one supported expected outcome");
        check(opaqueSession(secondarySession) && pid > 0, "opaque secondary scope and positive sidecar PID");
        check(accepted.received() && ACCEPTANCE_TRUE.stream().allMatch(k -> flag(a, k, true))
                && flag(a, "bannerVisibilityClaimed", false)
                && exactScope(a, "main", true) && exactScope(a, "secondary", false)
                && number(a, "mainRecoveryGeneration", 0) && number(a, "secondaryRecoveryGeneration", 0)
                && number(a, "nativePointerEventCount", 3) && number(a, "nativePointerTrustedEventCount", 3)
                && "ERROR".equals(text(a, "invalidOutcome")) && "DENIED".equals(text(a, "deniedOutcome"))
                && String.join(",", STAGES).equals(text(a, "nativePointerStages"))
                && "route-navigate,secondary-open".equals(text(a, "browserProgrammaticStages")),
                "exact typed, trusted, retained-owner acceptance without banner claim");
        requireLifecycle(authorization.getFirst());
        requireAuthorization(authorization, pointers, stages);
        List<Event> commands = of("command"), results = of("capabilityResult");
        List<Event> notifications = commands.stream().filter(e -> "notification.show".equals(e.command())).toList();
        List<Event> notificationResults = results.stream().filter(e -> "notification.show".equals(e.command())).toList();
        check(commands.size() == 4 && results.size() == 4
                && notifications.size() == 3 && notificationResults.size() == 3,
                "exact three notifications plus one setup window command/result");
        Set<String> correlations = new HashSet<>();
        check(commands.stream().allMatch(e -> !e.correlation().isBlank()
                        && correlations.add(e.correlation()) && e.correlation().equals(text(e.envelope(), "id"))),
                "unique command correlation identities");
        if (notifications.size() != 3 || notificationResults.size() != 3) return;
        for (int i = 0; i < 3; i++) {
            Event pointer = pointers.get(i), stage = stages.get(i);
            Event command = notifications.get(i), result = notificationResults.get(i);
            boolean main = i < 2;
            check(pointer.received() && scope(pointer.payload(), main)
                    && eventIdentity(pointer.payload()) && number(pointer.payload(), "index", i)
                    && STAGES.get(i).equals(text(pointer.payload(), "stage"))
                    && "appkit-window-pointer-event".equals(text(pointer.payload(), "inputMode"))
                    && "primary".equals(text(pointer.payload(), "button"))
                    && flag(pointer.payload(), "activeApplication", true)
                    && flag(pointer.payload(), "keyWindow", true)
                    && List.of("webViewX", "webViewY", "windowX", "windowY").stream()
                        .allMatch(k -> finite(pointer.payload(), k)),
                    "native pointer identity and geometry " + i);
            check(envelope(command, true) && scope(command.envelope(), main)
                    && exactArgs(command.body().path("args"), i)
                    && envelope(result, true) && scope(result.envelope(), main)
                    && correlated(command, result) && exactResult(result.body(), i),
                    "strict scoped and generation-bound notification command/result " + i);
            check(pointer.order < command.order && command.order < result.order
                    && result.order < stage.order && stage.order < accepted.order
                    && (i == 0 || stages.get(i - 1).order < pointer.order),
                    "notification click, command, result and Java state order " + i);
            requireStage(stage, pointer, command, result, i);
        }
        List<Event> opens = commands.stream().filter(e -> "window.open".equals(e.command())).toList();
        List<Event> opened = results.stream().filter(e -> "window.open".equals(e.command())).toList();
        check(opens.size() == 1 && opened.size() == 1, "one public setup window open");
        if (opens.size() == 1 && opened.size() == 1) {
            Event command = opens.getFirst(), result = opened.getFirst();
            List<Event> created = of("window.create").stream().filter(e -> scope(e.payload(), false)).toList();
            check(envelope(command, true) && envelope(result, true)
                    && scope(command.envelope(), true) && scope(result.envelope(), true)
                    && correlated(command, result) && flag(result.body(), "success", true)
                    && SECONDARY.equals(text(command.body().path("args"), "id"))
                    && "flow-notification-outcomes".equals(text(command.body().path("args"), "route"))
                    && "Flow notification permission denied".equals(text(command.body().path("args"), "title"))
                    && number(command.body().path("args"), "width", 760)
                    && number(command.body().path("args"), "height", 620)
                    && command.body().path("args").size() == 5
                    && SECONDARY.equals(text(result.body(), "windowId"))
                    && "flow-notification-outcomes".equals(text(result.body(), "route"))
                    && "allowed".equals(text(result.body(), "permissionDecision"))
                    && "manifest-allow".equals(text(result.body(), "permissionReason"))
                    && !result.body().has("error") && !result.body().has("errorCode")
                    && created.size() == 1 && command.order < created.getFirst().order
                    && created.getFirst().order < result.order
                    && authorization.getLast().order < command.order
                    && command.order < result.order && result.order < pointers.get(2).order,
                    "setup window opens only after OS observation in allowed main scope");
        }
        requireTransport(stages, pointers);
    }

    private void requireLifecycle(Event before) {
        Event launch = one("launch.sidecar"), start = one("sidecar.started");
        List<Event> readies = of("ready").stream()
                .filter(e -> "java-sidecar".equals(text(e.payload(), "source"))).toList();
        Event ready = readies.size() == 1 ? readies.getFirst() : null;
        Event manifest = one("capabilityManifest"), shutdown = one("shutdown.sidecar");
        check(launch != null && start != null && ready != null && manifest != null && shutdown != null,
                "unique manifest, JVM launch/start/ready and shutdown");
        if (launch != null && start != null && ready != null && manifest != null && shutdown != null) {
            JsonNode m = manifest.payload(), windows = m.path("commandsByWindow");
            check(manifest.received() && flag(m, "loaded", true)
                    && MANIFEST.equals(text(m, "path")) && "manifest-enforced".equals(text(m, "permissionMode"))
                    && "".equals(text(m, "parseError")) && m.path("parseError").isString()
                    && windows.isObject() && windows.size() == 2
                    && textArray(windows.path("main"), List.of("notification.show", "window.open"))
                    && textArray(windows.path(SECONDARY), List.of("window.close", "window.open")),
                    "exact least-privilege manifest");
            check("send".equals(launch.direction()) && number(launch.payload(), "generation", 1)
                    && "flow-ipc".equals(text(launch.payload(), "transport"))
                    && start.received() && number(start.payload(), "generation", 1)
                    && number(start.payload(), "processIdentifier", pid)
                    && "flow-ipc".equals(text(start.payload(), "transport"))
                    && envelope(ready, true) && ready.received()
                    && manifest.order < launch.order && launch.order < start.order
                    && start.order < ready.order && ready.order < before.order
                    && shutdown.received() && "graceful".equals(text(shutdown.payload(), "mode"))
                    && accepted.order < shutdown.order,
                    "one generation and PID through ordered graceful termination");
        }
        List<Event> creates = of("window.create"), closes = of("windowClosed");
        check(creates.size() == 2 && closes.size() == 2, "exactly two created and retired native windows");
        for (boolean main : List.of(true, false)) {
            List<Event> created = creates.stream().filter(e -> scope(e.payload(), main)).toList();
            List<Event> closed = closes.stream().filter(e -> scope(e.payload(), main)).toList();
            check(created.size() == 1 && closed.size() == 1
                    && "send".equals(created.getFirst().direction())
                    && "flow-ipc".equals(text(created.getFirst().payload(), "mode"))
                    && (main ? "main" : SECONDARY).equals(text(created.getFirst().payload(), "label"))
                    && ready != null && ready.order < created.getFirst().order
                    && created.getFirst().order < of(PREFIX + "nativePointerDispatch").get(main ? 0 : 2).order
                    && "send".equals(closed.getFirst().direction())
                    && "flow-ipc".equals(text(closed.getFirst().payload(), "transport"))
                    && "java-sidecar".equals(text(closed.getFirst().payload(), "target"))
                    && accepted.order < closed.getFirst().order,
                    "retained exact window scope " + (main ? "main" : "secondary"));
        }
        Event drain = one("host.nativeResourceDrainStarted");
        Event drained = one("host.nativeResourceDrainCompleted");
        check(drain != null && drained != null && drain.received() && drained.received()
                && accepted.order < drain.order && drain.order < drained.order
                && flag(drained.payload(), "drained", true)
                && number(drain.payload(), "pendingCount", 0)
                && number(drained.payload(), "pendingCount", 0)
                && number(drained.payload(), "failureCount", 0)
                && closes.size() == 2 && closes.stream().allMatch(e -> e.order < drain.order)
                && shutdown != null && drained.order < shutdown.order,
                "empty successful native cleanup drain before graceful shutdown");
        check(events.stream().noneMatch(e -> e.type().startsWith("sidecar.recovery")
                || e.type().startsWith("sidecar.replacement") || e.type().startsWith("nativeCapabilityOperation.")
                || Set.of("staleCapabilityResultDropped", "windowClosedReplay", "windowClosedDropped",
                "closedWindowCapabilityCommandRejected", "flowIpcClosedWindowEnvelopeDropped",
                "opaqueHttpStreamCancel", "opaqueHttpStreamError", "error", "webview.error").contains(e.type())
                || (e.type().equals("sidecar.exit") && (!flag(e.payload(), "expected", true)
                    || !number(e.payload(), "status", 0)))),
                "no recovery, replacement, invalidation, dropped frame or failure");
    }

    private void requireAuthorization(List<Event> observations, List<Event> pointers, List<Event> stages) {
        String after = text(accepted.payload(), "authorizationAfter");
        check("not-determined".equals(text(accepted.payload(), "authorizationBefore"))
                && switch (outcome) {
                    case "AUTHORIZATION_REQUIRED" -> after.equals("not-determined");
                    case "OS_DENIED" -> after.equals("denied");
                    case "SCHEDULED" -> Set.of("authorized", "provisional", "ephemeral").contains(after);
                    default -> false;
                }, "OS authorization state agrees with expected outcome");
        for (int i = 0; i < 2; i++) {
            Event e = observations.get(i);
            check(e.received() && scope(e.payload(), true) && eventIdentity(e.payload())
                    && flag(e.payload(), "readOnly", true)
                    && (i == 0 ? "before" : "after").equals(text(e.payload(), "phase"))
                    && outcome.equals(text(e.payload(), "expectedOutcome"))
                    && (i == 0 ? "not-determined" : after).equals(text(e.payload(), "authorizationStatus"))
                    && (i == 0 ? e.order < pointers.getFirst().order
                        : stages.get(1).order < e.order && e.order < pointers.get(2).order),
                    "ordered read-only OS authorization observation " + i);
        }
    }

    private void requireStage(Event stage, Event pointer, Event command, Event result, int i) {
        JsonNode p = stage.payload();
        boolean scheduled = i == 1 && outcome.equals("SCHEDULED");
        check(stage.received() && scope(p, i < 2) && eventIdentity(p)
                && number(p, "index", i) && STAGES.get(i).equals(text(p, "stage"))
                && (i == 0 ? "ERROR" : i == 1 ? outcome : "DENIED").equals(text(p, "outcome"))
                && "notification.show".equals(text(p, "command"))
                && flag(p, "success", scheduled) && flag(p, "scheduled", scheduled)
                && flag(p, "notificationIdPresent", scheduled) && p.path("errorCode").isString()
                && errorCode(i).equals(text(p, "errorCode"))
                && number(p, "change", 1) && number(p, "nativePointerReceiptCount", 1)
                && number(p, "nativePointerTrustedCount", 1)
                && List.of("client", "capabilityContextCaptured", "backgroundSessionAbsent",
                    "permissionMetadataValid", "nativePointerReceipt", "nativePointerTrusted").stream()
                    .allMatch(k -> flag(p, k, true)),
                "strict public Java outcome and trusted receipt " + i);
        check(integer(p, "requestBaseline") >= 0 && integer(p, "responseBaseline") >= 0
                && integer(p, "pushBaseline") >= 0
                && integer(p, "requestCount") > integer(p, "requestBaseline")
                && integer(p, "responseCount") > integer(p, "responseBaseline")
                && integer(p, "pushMessageCount") > integer(p, "pushBaseline"),
                "fresh request, response and push progress for stage " + i);
        List<Event> posts = of("opaqueHttpStreamOpen").stream().filter(e -> scope(e.envelope(), i < 2)
                && "POST".equals(text(e.body(), "method")) && pointer.order < e.order && e.order < command.order).toList();
        check(posts.stream().anyMatch(open -> of("opaqueHttpStreamResponseEnd").stream().anyMatch(end ->
                requestKey(open).equals(requestKey(end)) && end.order < stage.order)
                && of("opaqueHttpStreamEnd").stream().anyMatch(end ->
                    requestKey(open).equals(requestKey(end)) && end.order < command.order)),
                "native click reaches a completed scoped Java request " + i);
        check(of("opaquePush").stream().anyMatch(e -> scope(e.envelope(), i < 2)
                && result.order < e.order && e.order < stage.order),
                "native result produces a scoped opaque UI push before Java receipt " + i);
    }

    private boolean exactResult(JsonNode p, int index) {
        // Every acceptance stage uses exact fields. Even the expected blank-title
        // error and manifest denial must not carry native API error diagnostics.
        boolean scheduled = index == 1 && outcome.equals("SCHEDULED");
        if (!"notification.show".equals(text(p, "command")) || !permission(p, index < 2)
                || !flag(p, "success", scheduled)) return false;
        if (index == 2) return keys(p, "command", "success", "error")
                && marker(p.path("error"), 1);
        if (!"native-user-notifications".equals(text(p, "mode")) || !flag(p, "scheduled", scheduled)) return false;
        return scheduled
                ? keys(p, "command", "success", "mode", "scheduled", "notificationId", "title", "body")
                    && "[redacted length=43]".equals(text(p, "notificationId"))
                    && "[redacted length=30]".equals(text(p, "title"))
                    && "[redacted length=36]".equals(text(p, "body"))
                : keys(p, "command", "success", "mode", "scheduled", "errorCode", "error")
                    && errorCode(index).equals(text(p, "errorCode")) && marker(p.path("error"), 1);
    }

    private boolean exactArgs(JsonNode p, int index) {
        return p.isObject() && p.size() == 2
                && ("[redacted length=" + List.of(1, 30, 26).get(index) + "]").equals(text(p, "title"))
                && "[redacted length=36]".equals(text(p, "body"));
    }

    private String errorCode(int index) {
        if (index == 0) return "notification-title-invalid";
        if (index == 2 || outcome.equals("SCHEDULED")) return "";
        return outcome.equals("OS_DENIED")
                ? "notification-authorization-denied" : "notification-authorization-required";
    }

    private boolean permission(JsonNode p, boolean main) {
        return "manifest-enforced".equals(text(p, "permissionMode"))
                && (main ? "allowed" : "denied").equals(text(p, "permissionDecision"))
                && (main ? "manifest-allow" : "manifest-denied").equals(text(p, "permissionReason"))
                && ("window:" + (main ? "main" : SECONDARY)).equals(text(p, "permissionScope"))
                && flag(p, "permissionManifestLoaded", true)
                && MANIFEST.equals(text(p, "permissionManifestPath"))
                && (main ? "notification.show,window.open" : "window.close,window.open")
                    .equals(text(p, "permissionManifestCommands"));
    }

    private void requireTransport(List<Event> stages, List<Event> pointers) {
        List<Event> frames = events.stream().filter(e -> STREAM_TYPES.contains(e.type())).toList();
        check(frames.stream().allMatch(e -> envelope(e, false)
                && (scope(e.envelope(), true) || scope(e.envelope(), false))),
                "all opaque traffic retains exact live scope, direction, envelope and generation");
        check(frames.stream().filter(e -> e.type().startsWith("opaqueHttpStream")).allMatch(e ->
                !text(e.envelope(), "id").isBlank() && !text(e.body(), "requestId").isBlank()
                        && text(e.body(), "requestId").equals(e.correlation())),
                "HTTP envelope identity and correlation agree with the scoped request");
        check(events.stream().noneMatch(e -> e.order < accepted.order
                && Set.of("opaquePushDisconnect", "opaquePushResync").contains(e.type())),
                "no push disconnect or resync before notification acceptance");
        List<Event> opens = of("opaqueHttpStreamOpen"), responses = of("opaqueHttpStreamResponse");
        List<Event> ends = of("opaqueHttpStreamResponseEnd"), requestEnds = of("opaqueHttpStreamEnd");
        check(!opens.isEmpty() && opens.size() == responses.size() && opens.size() == ends.size()
                && opens.size() == requestEnds.size()
                && opens.stream().map(NotificationOutcomesTraceGate::requestKey).distinct().count() == opens.size(),
                "globally unique balanced opaque requests");
        for (Event open : opens) {
            List<Event> replies = responses.stream().filter(e -> requestKey(open).equals(requestKey(e))).toList();
            List<Event> completed = ends.stream().filter(e -> requestKey(open).equals(requestKey(e))).toList();
            List<Event> submitted = requestEnds.stream().filter(e -> requestKey(open).equals(requestKey(e))).toList();
            check(!text(open.body(), "requestId").isBlank() && replies.size() == 1 && completed.size() == 1
                    && submitted.size() == 1 && open.order < submitted.getFirst().order
                    && submitted.getFirst().order < accepted.order
                    && open.order < replies.getFirst().order && replies.getFirst().order < completed.getFirst().order
                    && integer(replies.getFirst().body(), "status") >= 200
                    && integer(replies.getFirst().body(), "status") < 400
                    && completed.getFirst().order < accepted.order,
                    "one successful complete response per scoped request " + requestKey(open));
            if (replies.size() == 1 && completed.size() == 1) {
                requireStreamBody(open, replies.getFirst(), completed.getFirst(), true);
            }
            if (submitted.size() == 1) requireStreamBody(open, open, submitted.getFirst(), false);
        }
        check(frames.stream().filter(e -> e.type().startsWith("opaqueHttpStream")
                && !e.type().equals("opaqueHttpStreamOpen")).allMatch(frame ->
                        opens.stream().anyMatch(open -> requestKey(open).equals(requestKey(frame))
                            && open.order < frame.order && frame.order < accepted.order)),
                "all request/response chunks, terminals and credit belong to an admitted scoped request");
        requirePushConnections();
        List<Event> pushes = of("opaquePush"), acks = of("opaquePushAck");
        Map<String, List<Event>> groups = group(pushes, NotificationOutcomesTraceGate::pushKey);
        check(!pushes.isEmpty() && pushes.size() == acks.size()
                && acks.stream().allMatch(e -> groups.containsKey(pushKey(e))),
                "all opaque pushes and ACKs have a matching logical delivery");
        Map<String, Long> lastSequences = new LinkedHashMap<>();
        for (Map.Entry<String, List<Event>> entry : groups.entrySet()) {
            List<Event> attempts = entry.getValue();
            List<Event> receipts = acks.stream().filter(e -> pushKey(e).equals(entry.getKey())).toList();
            Event first = attempts.getFirst();
            long sequence = integer(first.body(), "pushSequence");
            Long previous = lastSequences.put(connectionKey(first), sequence);
            check(previous == null || previous < Long.MAX_VALUE && sequence == previous + 1,
                    "new logical push sequences are contiguous within their exact connection");
            check(!text(first.body(), "connectionId").isBlank() && integer(first.body(), "flowUiId") >= 0
                    && integer(first.body(), "pushSequence") > 0 && integer(first.body(), "flowServerSyncId") >= 0
                    && integer(first.body(), "uidlBytes") > 0 && attempts.size() == receipts.size(),
                    "valid scoped logical push identity");
            for (int i = 0; i < Math.min(attempts.size(), receipts.size()); i++) {
                Event push = attempts.get(i), ack = receipts.get(i);
                check(number(push.body(), "deliveryAttempt", i + 1)
                        && number(push.body(), "flowServerSyncId", integer(first.body(), "flowServerSyncId"))
                        && number(push.body(), "uidlBytes", integer(first.body(), "uidlBytes"))
                        && flag(push.body(), "resync", false)
                        && flag(push.body(), "uidlRedacted", true) && !push.body().has("uidl")
                        && number(ack.body(), "flowServerSyncId", integer(first.body(), "flowServerSyncId"))
                        && integer(ack.body(), "lastSeenServerSyncId") >= -1
                        && push.order < ack.order && ack.order < accepted.order,
                        "sequential same-payload retries and ordinal ACK causality");
            }
        }
        for (int i = 0; i < 3; i++) {
            Event stage = stages.get(i), pointer = pointers.get(i);
            checkCounts(stage.payload(), "", stage.order, i < 2, opens, ends, groups);
            check(number(stage.payload(), "requestBaseline", countBefore(opens, pointer.order, i < 2))
                    && number(stage.payload(), "responseBaseline", countBefore(ends, pointer.order, i < 2))
                    && number(stage.payload(), "pushBaseline", logicalPushesBefore(groups, pointer.order, i < 2)),
                    "stage baselines correspond to trace traffic " + i);
        }
        checkCounts(accepted.payload(), "main", accepted.order, true, opens, ends, groups);
        checkCounts(accepted.payload(), "secondary", accepted.order, false, opens, ends, groups);
    }

    private void requireStreamBody(Event open, Event start, Event end, boolean response) {
        String chunkType = response ? "opaqueHttpStreamResponseChunk" : "opaqueHttpStreamChunk";
        String creditType = response ? "opaqueHttpStreamResponseCredit" : "opaqueHttpStreamRequestCredit";
        List<Event> chunks = of(chunkType).stream().filter(e -> requestKey(e).equals(requestKey(open))).toList();
        List<Event> credits = of(creditType).stream().filter(e -> requestKey(e).equals(requestKey(open))).toList();
        check(credits.stream().allMatch(e -> integer(e.body(), "credit") > 0
                && integer(e.body(), "credit") <= Integer.MAX_VALUE), "positive bounded integer stream credit");
        long bytes = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Event chunk = chunks.get(i);
            long supplied = credits.stream().filter(e -> e.order < chunk.order)
                    .mapToLong(e -> integer(e.body(), "credit")).sum();
            check(number(chunk.body(), "chunkSequence", i)
                    && integer(chunk.body(), "bodyBytes") > 0 && integer(chunk.body(), "bodyBytes") <= Integer.MAX_VALUE
                    && flag(chunk.body(), "bodyRedacted", true) && !chunk.body().has("bodyBase64")
                    && start.order < chunk.order && chunk.order < end.order && supplied > i,
                    "ordered, credited and redacted " + (response ? "response" : "request") + " chunks");
            bytes += Math.max(0, integer(chunk.body(), "bodyBytes"));
        }
        check(number(end.body(), "chunkCount", chunks.size()) && number(end.body(), "bodyBytes", bytes),
                "complete " + (response ? "response" : "request") + " byte/chunk counts");
        if (!response && open.body().has("contentLength")) {
            long length = integer(open.body(), "contentLength");
            check(length == -1 || length >= 0 && length == bytes, "declared request length agrees with submitted bytes");
        }
    }

    private void requirePushConnections() {
        List<Event> connects = of("opaquePushConnect"), connected = of("opaquePushConnected");
        check(connects.size() >= 2 && connects.size() == connected.size()
                && connects.stream().map(Event::correlation).distinct().count() == connects.size(),
                "balanced scoped push connection handshakes");
        for (Event connect : connects) {
            List<Event> replies = connected.stream().filter(e -> e.correlation().equals(connect.correlation())).toList();
            check(!connect.correlation().isBlank() && replies.size() == 1
                    && connectionKey(connect).equals(connectionKey(replies.getFirst()))
                    && "opaque-ipc-push".equals(text(connect.body(), "transport"))
                    && "opaque-ipc-push".equals(text(replies.getFirst().body(), "transport"))
                    && !text(connect.body(), "connectionId").isBlank() && integer(connect.body(), "flowUiId") >= 0
                    && connect.order < replies.getFirst().order && replies.getFirst().order < accepted.order,
                    "one ordered same-scope and same-identity push connection response");
        }
        for (Event push : of("opaquePush")) {
            check(connected.stream().anyMatch(e -> connectionKey(e).equals(connectionKey(push)) && e.order < push.order),
                    "push belongs to an already-established exact scoped connection");
        }
    }

    private void checkCounts(JsonNode p, String prefix, int order, boolean main,
                             List<Event> opens, List<Event> ends, Map<String, List<Event>> pushes) {
        String request = prefix.isEmpty() ? "requestCount" : prefix + "RequestCount";
        String response = prefix.isEmpty() ? "responseCount" : prefix + "ResponseCount";
        String push = prefix.isEmpty() ? "pushMessageCount" : prefix + "PushMessageCount";
        check(number(p, request, countBefore(opens, order, main))
                && number(p, response, countBefore(ends, order, main))
                && number(p, push, logicalPushesBefore(pushes, order, main)),
                "exact scoped observed transport counts " + prefix);
    }

    private long logicalPushesBefore(Map<String, List<Event>> groups, int order, boolean main) {
        return groups.values().stream().map(List::getFirst)
                .filter(e -> e.order < order && scope(e.envelope(), main)).count();
    }

    private long countBefore(List<Event> values, int order, boolean main) {
        return values.stream().filter(e -> e.order < order && scope(e.envelope(), main)).count();
    }

    private boolean envelope(Event e, boolean requireGeneration) {
        JsonNode p = e.payload(), env = e.envelope();
        boolean send = SEND_TYPES.contains(e.type());
        return (send ? "send" : "recv").equals(e.direction())
                && "java-sidecar".equals(text(p, send ? "target" : "source"))
                && "flow-ipc".equals(text(p, "transport"))
                && ((!requireGeneration && send && !p.has("sidecarGeneration")) || number(p, "sidecarGeneration", 1))
                && number(env, "version", 1) && e.type().equals(text(env, "type"))
                && env.path("error").isNull() && e.body().isObject();
    }

    private static boolean correlated(Event command, Event result) {
        return command.correlation().equals(result.correlation())
                && ("capability-" + command.correlation()).equals(text(result.envelope(), "id"))
                && command.command().equals(result.command())
                && result.command().equals(text(result.payload(), "command"));
    }

    private boolean scope(JsonNode p, boolean main) {
        String window = main ? "main" : SECONDARY;
        return window.equals(text(p, "windowId")) && window.equals(text(p, "uiId"))
                && (main ? "flow-ipc-session" : secondarySession).equals(text(p, "sessionId"));
    }

    private boolean exactScope(JsonNode p, String prefix, boolean main) {
        String window = main ? "main" : SECONDARY;
        return window.equals(text(p, prefix + "WindowId")) && window.equals(text(p, prefix + "UiId"))
                && (main ? "flow-ipc-session" : secondarySession).equals(text(p, prefix + "SessionId"));
    }

    private boolean eventIdentity(JsonNode p) {
        return number(p, "sidecarProcessIdentifier", pid) && number(p, "recoveryGeneration", 0);
    }

    private static boolean opaqueSession(String session) {
        if (!session.startsWith("flow-ipc-window:")) return false;
        try {
            String id = session.substring("flow-ipc-window:".length());
            return UUID.fromString(id).toString().equalsIgnoreCase(id);
        } catch (IllegalArgumentException e) { return false; }
    }

    private static boolean keys(JsonNode p, String... own) {
        Set<String> keys = new HashSet<>(PERMISSION_KEYS);
        keys.addAll(List.of(own));
        return p.isObject() && p.size() == keys.size() && keys.stream().allMatch(p::has);
    }

    private static boolean textArray(JsonNode p, List<String> expected) {
        if (!p.isArray() || p.size() != expected.size()) return false;
        for (int i = 0; i < expected.size(); i++)
            if (!p.get(i).isString() || !expected.get(i).equals(p.get(i).asString())) return false;
        return true;
    }

    private static boolean marker(JsonNode p, int minimum) {
        if (!p.isString() || !p.asString().matches("\\[redacted length=(0|[1-9][0-9]*)]")) return false;
        try {
            return Long.parseLong(p.asString().substring(17, p.asString().length() - 1)) >= minimum;
        } catch (NumberFormatException e) { return false; }
    }

    private static boolean finite(JsonNode p, String name) {
        return p.path(name).isNumber() && Double.isFinite(p.path(name).asDouble()) && p.path(name).asDouble() >= 0;
    }

    private static String requestKey(Event e) {
        return scopeKey(e) + ":" + text(e.body(), "requestId");
    }

    private static String pushKey(Event e) {
        return connectionKey(e) + ":" + integer(e.body(), "pushSequence");
    }

    private static String connectionKey(Event e) {
        return scopeKey(e) + ":" + text(e.body(), "connectionId") + ":" + integer(e.body(), "flowUiId");
    }

    private static String scopeKey(Event e) {
        return text(e.envelope(), "windowId") + ":" + text(e.envelope(), "sessionId") + ":" + text(e.envelope(), "uiId");
    }

    private static Map<String, List<Event>> group(List<Event> events,
                                                  java.util.function.Function<Event, String> key) {
        Map<String, List<Event>> grouped = new LinkedHashMap<>();
        for (Event event : events) grouped.computeIfAbsent(key.apply(event), ignored -> new ArrayList<>()).add(event);
        return grouped;
    }

    private List<Event> of(String type) {
        return events.stream().filter(e -> e.type().equals(type)).toList();
    }

    private Event one(String type) {
        List<Event> matching = of(type);
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private void check(boolean valid, String message) {
        if (!valid) missing.add("notification outcomes: " + message);
    }

    private static String text(JsonNode p, String field) {
        return p.path(field).isString() ? p.path(field).asString() : "";
    }

    private static long integer(JsonNode p, String field) {
        return p.path(field).isIntegralNumber() && p.path(field).canConvertToLong()
                ? p.path(field).asLong() : Long.MIN_VALUE;
    }

    private static boolean number(JsonNode p, String field, long expected) {
        return integer(p, field) == expected;
    }

    private static boolean flag(JsonNode p, String field, boolean expected) {
        return p.path(field).isBoolean() && p.path(field).asBoolean() == expected;
    }

    private record Event(JsonNode root, int order) {
        String type() { return text(root, "type"); }
        String direction() { return text(root, "direction"); }
        boolean received() { return direction().equals("recv"); }
        JsonNode payload() { return root.path("payload"); }
        JsonNode envelope() { return payload().path("envelope"); }
        JsonNode body() { return envelope().path("payload"); }
        String command() { return text(body(), "command"); }
        String correlation() { return text(envelope(), "correlationId"); }
    }
}
