package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Test-only foreground delivery evidence. A retained OS record is not a banner receipt. */
final class NotificationDeliveryTraceGate {
    private static final String PREFIX = "flowNotificationDelivery.";
    private static final String MANIFEST = "$APP/Contents/Resources/desktop-capabilities.json";
    private static final List<String> STAGES = List.of("prepare", "probe");
    private static final List<String> TITLES = List.of("Vatori notification preparation", "Vatori foreground probe");
    private static final String BODY = "Native foreground delivery validation";
    private static final java.util.regex.Pattern RAW_IDENTIFIER = java.util.regex.Pattern.compile(
            "(?i)vatori-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Set<String> STREAM_TYPES = Set.of(
            "opaqueHttpStreamOpen", "opaqueHttpStreamChunk", "opaqueHttpStreamEnd",
            "opaqueHttpStreamRequestCredit", "opaqueHttpStreamResponse", "opaqueHttpStreamResponseCredit",
            "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseEnd",
            "opaquePushConnect", "opaquePushConnected", "opaquePush", "opaquePushAck");
    private static final Set<String> SEND_TYPES = Set.of(
            "capabilityResult", "opaqueHttpStreamOpen", "opaqueHttpStreamChunk",
            "opaqueHttpStreamEnd", "opaqueHttpStreamResponseCredit", "opaquePushConnect", "opaquePushAck");
    private static final List<String> ACCEPTANCE_TRUE = List.of(
            "complete", "standardBundleMode", "mainFlowClientOwned", "nativePointerObserved",
            "nativePointerTrustedObserved", "capabilityContextCapturedObserved", "backgroundSessionAbsentObserved",
            "permissionPayloadPreservedObserved", "mainJavaListenerObserved", "mainUidlPatchObserved",
            "mainPushObserved", "balancedDrainObserved", "resultStableAfterDrain", "errorFree",
            "authorizationReadOnlyObserved", "notificationDelegateNil", "foregroundIntervalUninterrupted",
            "deliveredNotificationMatched", "sameMainWindow", "sameMainWebView", "testCleanupRequested");
    private static final Set<String> PERMISSION_KEYS = Set.of(
            "permissionMode", "permissionDecision", "permissionReason", "permissionScope",
            "permissionManifestLoaded", "permissionManifestPath", "permissionManifestCommands");
    private static final Set<String> IDENTITY_KEYS = Set.of(
            "windowId", "sessionId", "uiId", "sidecarProcessIdentifier", "recoveryGeneration");
    private final List<Event> events = new ArrayList<>();
    private boolean leaked;
    private List<String> missing;
    private long pid;
    private Event accepted;

    void acceptRawLine(String line) {
        leaked |= privateText(line);
    }

    void accept(JsonNode root) {
        leaked |= privateValue(root);
        events.add(new Event(root, events.size()));
    }

    private static boolean privateText(String text) {
        return TITLES.stream().anyMatch(text::contains) || text.contains(BODY) || RAW_IDENTIFIER.matcher(text).find();
    }

    private static boolean privateValue(JsonNode value) {
        if (value.isString()) return privateText(value.asString());
        if (value.isArray()) {
            for (JsonNode item : value) if (privateValue(item)) return true;
        } else if (value.isObject()) {
            for (String key : value.propertyNames()) if (privateText(key) || privateValue(value.path(key))) return true;
        }
        return false;
    }

    void require(List<String> missing) {
        this.missing = missing;
        accepted = one(PREFIX + "frameworkAcceptance");
        List<Event> pointers = of(PREFIX + "nativePointerDispatch");
        List<Event> stages = of(PREFIX + "stageObserved");
        List<Event> settings = of(PREFIX + "authorizationObserved");
        Event delivery = one(PREFIX + "deliveryObserved");
        check(accepted != null && delivery != null && pointers.size() == 2 && stages.size() == 2
                && settings.size() == 2, "exact acceptance, delivery, two pointers/stages and OS observations");
        check(events.stream().filter(e -> e.type().startsWith(PREFIX)).count() == 8,
                "no unknown, failed or duplicated delivery acceptance event");
        check(!leaked, "no raw notification identifier, title or body in trace");
        if (accepted == null || delivery == null || pointers.size() != 2 || stages.size() != 2 || settings.size() != 2) return;
        JsonNode a = accepted.payload();
        pid = integer(a, "sidecarProcessIdentifier");
        Set<String> acceptedKeys = new HashSet<>(ACCEPTANCE_TRUE);
        acceptedKeys.addAll(List.of("bannerVisibilityClaimed", "nativePointerEventCount", "nativePointerTrustedEventCount",
                "applicationResignActiveCount", "windowResignKeyCount", "nativePointerStages",
                "browserProgrammaticStages", "authorizationBefore", "authorizationAfter", "mainWindowId",
                "mainSessionId", "mainUiId", "sidecarProcessIdentifier", "mainRecoveryGeneration",
                "mainRequestCount", "mainResponseCount", "mainPushMessageCount", "prepareNotificationDigest",
                "probeNotificationDigest", "testCleanupIdentifierCount"));
        check(accepted.received() && exactKeys(a, acceptedKeys) && pid > 0
                && ACCEPTANCE_TRUE.stream().allMatch(k -> flag(a, k, true))
                && flag(a, "bannerVisibilityClaimed", false)
                && "main".equals(text(a, "mainWindowId")) && "main".equals(text(a, "mainUiId"))
                && "flow-ipc-session".equals(text(a, "mainSessionId"))
                && number(a, "mainRecoveryGeneration", 0)
                && number(a, "nativePointerEventCount", 2) && number(a, "nativePointerTrustedEventCount", 2)
                && number(a, "applicationResignActiveCount", 0) && number(a, "windowResignKeyCount", 0)
                && "prepare,probe".equals(text(a, "nativePointerStages"))
                && "route-navigate".equals(text(a, "browserProgrammaticStages"))
                && "not-determined".equals(text(a, "authorizationBefore"))
                && "authorized".equals(text(a, "authorizationAfter"))
                && digest(a, "prepareNotificationDigest") && digest(a, "probeNotificationDigest")
                && !text(a, "prepareNotificationDigest").equals(text(a, "probeNotificationDigest"))
                && integer(a, "testCleanupIdentifierCount") >= 1 && integer(a, "testCleanupIdentifierCount") <= 2,
                "strict retained-owner, foreground, no-banner acceptance and exact owned cleanup request");
        requireLifecycle(settings.getFirst(), pointers.getFirst());
        for (int i = 0; i < 2; i++) {
            Event setting = settings.get(i);
            JsonNode p = setting.payload();
            check(setting.received() && scopedIdentity(p)
                    && identityKeys(p, "phase", "authorizationStatus", "readOnly", "notificationDelegateNil")
                    && flag(p, "readOnly", true) && flag(p, "notificationDelegateNil", true)
                    && (i == 0 ? "before" : "after").equals(text(p, "phase"))
                    && (i == 0 ? "not-determined" : "authorized").equals(text(p, "authorizationStatus"))
                    && (i == 0 ? setting.order < pointers.getFirst().order
                        : stages.getFirst().order < setting.order && setting.order < pointers.getLast().order),
                    "ordered read-only authorization and nil delegate " + i);
        }
        List<Event> commands = of("command"), results = of("capabilityResult");
        check(commands.size() == 2 && results.size() == 2, "exact two public native commands and results");
        if (commands.size() != 2 || results.size() != 2) return;
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 2; i++) {
            Event pointer = pointers.get(i), stage = stages.get(i), command = commands.get(i), result = results.get(i);
            JsonNode p = pointer.payload();
            check(pointer.received() && scopedIdentity(p)
                    && identityKeys(p, "index", "stage", "inputMode", "button", "activeApplication",
                        "keyWindow", "webViewX", "webViewY", "windowX", "windowY", "dispatchedAt")
                    && number(p, "index", i) && STAGES.get(i).equals(text(p, "stage"))
                    && "appkit-window-pointer-event".equals(text(p, "inputMode"))
                    && "primary".equals(text(p, "button")) && flag(p, "activeApplication", true)
                    && flag(p, "keyWindow", true) && timestamp(p, "dispatchedAt")
                    && List.of("webViewX", "webViewY", "windowX", "windowY").stream().allMatch(k -> finite(p, k)),
                    "exact trusted native pointer dispatch schema " + i);
            check(envelope(command, true) && scope(command.envelope(), true) && notificationPrivacy(command.root())
                    && envelope(result, true) && scope(result.envelope(), true)
                    && notificationPrivacy(result.root())
                    && !command.correlation().isBlank() && ids.add(command.correlation())
                    && command.correlation().equals(text(command.envelope(), "id")) && correlated(command, result)
                    && exactKeys(command.body(), Set.of("command", "args", "serverSequence", "serverSequenceScope"))
                    && integer(command.body(), "serverSequence") > 0
                    && "java-sidecar-to-webview".equals(text(command.body(), "serverSequenceScope"))
                    && (i == 0 || integer(commands.get(i - 1).body(), "serverSequence")
                        < integer(command.body(), "serverSequence"))
                    && "notification.show".equals(command.command()) && exactArgs(command.body().path("args"), i)
                    && exactResult(result.body(), i), "strict scoped, generation-bound public notification pair " + i);
            check(pointer.order < command.order && command.order < result.order && result.order < stage.order
                    && stage.order < accepted.order && (i == 0 || stages.getFirst().order < pointer.order),
                    "ordered native click, command, terminal result and Java receipt " + i);
            requireStage(stage, pointer, command, result, i);
        }
        requireDelivery(delivery, pointers.getLast(), stages);
        requireTransport(stages, pointers);
    }

    private void requireDelivery(Event delivery, Event pointer, List<Event> stages) {
        JsonNode p = delivery.payload();
        check(delivery.received() && scopedIdentity(p)
                && identityKeys(p, "stage", "notificationDigest", "digestAlgorithm", "matchingCount",
                    "exactPublicIdentifierDigestMatched", "notificationDateAtOrAfterProbeDispatch",
                    "probeDispatchAt", "queryStartedAt", "notificationDateAt", "observedAt", "activeApplication", "keyWindow",
                    "notificationDelegateNil", "applicationResignActiveCount", "windowResignKeyCount",
                    "foregroundIntervalUninterrupted", "readOnly")
                && "probe".equals(text(p, "stage")) && "sha256".equals(text(p, "digestAlgorithm"))
                && digest(p, "notificationDigest")
                && text(p, "notificationDigest").equals(text(stages.getLast().payload(), "notificationDigest"))
                && number(p, "matchingCount", 1)
                && List.of("exactPublicIdentifierDigestMatched", "notificationDateAtOrAfterProbeDispatch",
                    "activeApplication", "keyWindow", "notificationDelegateNil",
                    "foregroundIntervalUninterrupted", "readOnly").stream().allMatch(k -> flag(p, k, true))
                && number(p, "applicationResignActiveCount", 0) && number(p, "windowResignKeyCount", 0)
                && timestamp(p, "probeDispatchAt") && timestamp(p, "queryStartedAt")
                && timestamp(p, "notificationDateAt") && timestamp(p, "observedAt")
                && timestamp(pointer.payload(), "dispatchedAt")
                && p.path("probeDispatchAt").asDouble() == pointer.payload().path("dispatchedAt").asDouble()
                && p.path("probeDispatchAt").asDouble() <= p.path("queryStartedAt").asDouble()
                && p.path("queryStartedAt").asDouble() <= p.path("observedAt").asDouble()
                && p.path("probeDispatchAt").asDouble() <= p.path("notificationDateAt").asDouble()
                && p.path("notificationDateAt").asDouble() <= p.path("observedAt").asDouble()
                && stages.getLast().order < delivery.order && delivery.order < accepted.order,
                "exact public-ID digest match within uninterrupted foreground interval; no banner inference");
        check(pointersChronological() && text(stages.getFirst().payload(), "notificationDigest")
                    .equals(text(accepted.payload(), "prepareNotificationDigest"))
                && text(stages.getLast().payload(), "notificationDigest")
                    .equals(text(accepted.payload(), "probeNotificationDigest")),
                "distinct stage digests and chronologically correlated dispatch timestamps");
    }

    private boolean pointersChronological() {
        List<Event> p = of(PREFIX + "nativePointerDispatch");
        return timestamp(p.getFirst().payload(), "dispatchedAt") && timestamp(p.getLast().payload(), "dispatchedAt")
                && p.getFirst().payload().path("dispatchedAt").asDouble()
                < p.getLast().payload().path("dispatchedAt").asDouble();
    }

    private void requireStage(Event stage, Event pointer, Event command, Event result, int index) {
        JsonNode p = stage.payload();
        check(stage.received() && scopedIdentity(p)
                && identityKeys(p, "index", "stage", "outcome", "command", "success", "scheduled",
                    "notificationIdPresent", "errorCode", "change", "nativePointerReceiptCount",
                    "nativePointerTrustedCount", "client", "capabilityContextCaptured",
                    "backgroundSessionAbsent", "permissionMetadataValid", "nativePointerReceipt",
                    "nativePointerTrusted", "requestBaseline", "responseBaseline", "pushBaseline",
                    "requestCount", "responseCount", "pushMessageCount", "notificationDigest", "digestAlgorithm")
                && number(p, "index", index) && STAGES.get(index).equals(text(p, "stage"))
                && "SCHEDULED".equals(text(p, "outcome")) && "notification.show".equals(text(p, "command"))
                && p.path("errorCode").isString() && text(p, "errorCode").isEmpty()
                && number(p, "change", 1) && number(p, "nativePointerReceiptCount", 1)
                && number(p, "nativePointerTrustedCount", 1) && digest(p, "notificationDigest")
                && "sha256".equals(text(p, "digestAlgorithm"))
                && List.of("success", "scheduled", "notificationIdPresent", "client", "capabilityContextCaptured",
                    "backgroundSessionAbsent", "permissionMetadataValid", "nativePointerReceipt",
                    "nativePointerTrusted").stream().allMatch(k -> flag(p, k, true)),
                "strict public Java scheduled result, digest and trusted receipt " + index);
        check(integer(p, "requestBaseline") >= 0 && integer(p, "responseBaseline") >= 0
                && integer(p, "pushBaseline") >= 0 && integer(p, "requestCount") > integer(p, "requestBaseline")
                && integer(p, "responseCount") > integer(p, "responseBaseline")
                && integer(p, "pushMessageCount") > integer(p, "pushBaseline"), "fresh scoped stage traffic " + index);
        check(of("opaqueHttpStreamOpen").stream().filter(e -> "POST".equals(text(e.body(), "method"))
                && pointer.order < e.order && e.order < command.order).anyMatch(open ->
                    of("opaqueHttpStreamEnd").stream().anyMatch(end -> requestKey(open).equals(requestKey(end))
                        && end.order < command.order)
                    && of("opaqueHttpStreamResponseEnd").stream().anyMatch(end -> requestKey(open).equals(requestKey(end))
                        && end.order < stage.order)), "trusted click reaches completed scoped Java request " + index);
        check(of("opaquePush").stream().anyMatch(e -> result.order < e.order && e.order < stage.order),
                "native result produces scoped opaque UI push before Java receipt " + index);
    }

    private boolean exactArgs(JsonNode p, int index) {
        return exactKeys(p, Set.of("title", "body"))
                && ("[redacted length=" + TITLES.get(index).length() + "]").equals(text(p, "title"))
                && ("[redacted length=" + BODY.length() + "]").equals(text(p, "body"));
    }

    private boolean exactResult(JsonNode p, int index) {
        Set<String> keys = new HashSet<>(PERMISSION_KEYS);
        keys.addAll(List.of("command", "success", "mode", "scheduled", "notificationId", "title", "body"));
        return exactKeys(p, keys) && "notification.show".equals(text(p, "command"))
                && flag(p, "success", true) && flag(p, "scheduled", true)
                && "native-user-notifications".equals(text(p, "mode"))
                && "[redacted length=43]".equals(text(p, "notificationId"))
                && ("[redacted length=" + TITLES.get(index).length() + "]").equals(text(p, "title"))
                && ("[redacted length=" + BODY.length() + "]").equals(text(p, "body"))
                && "manifest-enforced".equals(text(p, "permissionMode"))
                && "allowed".equals(text(p, "permissionDecision")) && "manifest-allow".equals(text(p, "permissionReason"))
                && "window:main".equals(text(p, "permissionScope")) && flag(p, "permissionManifestLoaded", true)
                && MANIFEST.equals(text(p, "permissionManifestPath"))
                && "notification.show".equals(text(p, "permissionManifestCommands"));
    }

    private void requireLifecycle(Event before, Event firstPointer) {
        Event manifest = one("capabilityManifest"), launch = one("launch.sidecar"), start = one("sidecar.started");
        List<Event> readies = of("ready").stream().filter(e -> "java-sidecar".equals(text(e.payload(), "source"))).toList();
        Event ready = readies.size() == 1 ? readies.getFirst() : null;
        Event create = one("window.create"), close = one("windowClosed"), shutdown = one("shutdown.sidecar");
        Event drain = one("host.nativeResourceDrainStarted"), drained = one("host.nativeResourceDrainCompleted");
        check(manifest != null && launch != null && start != null && ready != null && create != null
                && close != null && shutdown != null && drain != null && drained != null,
                "unique least-privilege manifest, launch/ready/window and graceful cleanup lifecycle");
        if (manifest != null) {
            JsonNode p = manifest.payload(), windows = p.path("commandsByWindow");
            check(manifest.received() && flag(p, "loaded", true) && MANIFEST.equals(text(p, "path"))
                    && "manifest-enforced".equals(text(p, "permissionMode")) && p.path("parseError").isString()
                    && text(p, "parseError").isEmpty() && windows.isObject() && windows.size() == 1
                    && textArray(windows.path("main"), List.of("notification.show")), "exact main-only notification manifest");
        }
        if (manifest != null && launch != null && start != null && ready != null && create != null) {
            check("send".equals(launch.direction()) && number(launch.payload(), "generation", 1)
                    && "flow-ipc".equals(text(launch.payload(), "transport")) && start.received()
                    && number(start.payload(), "generation", 1) && number(start.payload(), "processIdentifier", pid)
                    && "flow-ipc".equals(text(start.payload(), "transport")) && envelope(ready, true)
                    && "send".equals(create.direction()) && scope(create.payload(), true)
                    && "main".equals(text(create.payload(), "label")) && "flow-ipc".equals(text(create.payload(), "mode"))
                    && manifest.order < launch.order && launch.order < start.order && start.order < ready.order
                    && ready.order < create.order && create.order < before.order && before.order < firstPointer.order,
                    "ordered one-generation, one-PID main-window startup");
        }
        if (close != null && drain != null && drained != null && shutdown != null) {
            check("send".equals(close.direction()) && scope(close.payload(), true)
                    && "flow-ipc".equals(text(close.payload(), "transport")) && "java-sidecar".equals(text(close.payload(), "target"))
                    && drain.received() && drained.received() && flag(drained.payload(), "drained", true)
                    && number(drain.payload(), "pendingCount", 0) && number(drained.payload(), "pendingCount", 0)
                    && number(drained.payload(), "failureCount", 0) && shutdown.received()
                    && "graceful".equals(text(shutdown.payload(), "mode")) && accepted.order < close.order
                    && close.order < drain.order && drain.order < drained.order && drained.order < shutdown.order,
                    "owner close and empty successful drain precede graceful shutdown");
        }
        check(events.stream().noneMatch(e -> e.type().startsWith("sidecar.recovery")
                || e.type().startsWith("sidecar.replacement") || e.type().startsWith("nativeCapabilityOperation.")
                || Set.of("staleCapabilityResultDropped", "windowClosedReplay", "windowClosedDropped",
                    "closedWindowCapabilityCommandRejected", "flowIpcClosedWindowEnvelopeDropped",
                    "opaqueHttpStreamCancel", "opaqueHttpStreamError", "error", "webview.error").contains(e.type())
                || e.type().equals("sidecar.exit") && (!flag(e.payload(), "expected", true) || !number(e.payload(), "status", 0))),
                "no recovery, replacement, invalidation, dropped frame or failure");
    }

    private void requireTransport(List<Event> stages, List<Event> pointers) {
        List<Event> frames = events.stream().filter(e -> STREAM_TYPES.contains(e.type())).toList();
        check(frames.stream().allMatch(e -> envelope(e, false)
                && scope(e.envelope(), true)),
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
                && opens.stream().map(NotificationDeliveryTraceGate::requestKey).distinct().count() == opens.size(),
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
        Map<String, List<Event>> groups = group(pushes, NotificationDeliveryTraceGate::pushKey);
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
        for (int i = 0; i < 2; i++) {
            Event stage = stages.get(i), pointer = pointers.get(i);
            checkCounts(stage.payload(), "", stage.order, true, opens, ends, groups);
            check(number(stage.payload(), "requestBaseline", countBefore(opens, pointer.order, true))
                    && number(stage.payload(), "responseBaseline", countBefore(ends, pointer.order, true))
                    && number(stage.payload(), "pushBaseline", logicalPushesBefore(groups, pointer.order, true)),
                    "stage baselines correspond to trace traffic " + i);
        }
        checkCounts(accepted.payload(), "main", accepted.order, true, opens, ends, groups);
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
        check(connects.size() >= 1 && connects.size() == connected.size()
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


    private boolean scope(JsonNode p, boolean ignored) {
        return "main".equals(text(p, "windowId")) && "main".equals(text(p, "uiId"))
                && "flow-ipc-session".equals(text(p, "sessionId"));
    }

    private boolean scopedIdentity(JsonNode p) {
        return scope(p, true) && number(p, "sidecarProcessIdentifier", pid) && number(p, "recoveryGeneration", 0);
    }

    private static boolean identityKeys(JsonNode p, String... own) {
        Set<String> keys = new HashSet<>(IDENTITY_KEYS);
        keys.addAll(List.of(own));
        return exactKeys(p, keys);
    }

    private static boolean exactKeys(JsonNode p, Set<String> keys) {
        return p.isObject() && p.size() == keys.size() && keys.stream().allMatch(p::has);
    }

    private static boolean digest(JsonNode p, String field) {
        return p.path(field).isString() && p.path(field).asString().matches("[0-9a-f]{64}");
    }

    private static boolean notificationPrivacy(JsonNode value) {
        if (value.isArray()) {
            for (JsonNode item : value) if (!notificationPrivacy(item)) return false;
        } else if (value.isObject()) {
            for (String key : value.propertyNames()) {
                JsonNode child = value.path(key);
                String normalized = key.toLowerCase(java.util.Locale.ROOT);
                if (Set.of("title", "body", "bodypreview", "notificationid").contains(normalized)
                        && !marker(child, 0)) return false;
                if (normalized.equals("userinfo") || normalized.equals("error") && !child.isNull()) return false;
                if (!notificationPrivacy(child)) return false;
            }
        }
        return true;
    }

    private static boolean timestamp(JsonNode p, String field) {
        return finite(p, field) && p.path(field).asDouble() > 0;
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
        if (!valid) missing.add("notification delivery: " + message);
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
