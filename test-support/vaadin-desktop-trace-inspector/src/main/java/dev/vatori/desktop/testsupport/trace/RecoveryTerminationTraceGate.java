package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Focused three-generation recovery evidence; never a claim of durable application state. */
final class RecoveryTerminationTraceGate {
    private static final String PREFIX = "flowRecoveryTermination.";
    private static final Set<String> HTTP = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamChunk",
            "opaqueHttpStreamEnd", "opaqueHttpStreamRequestCredit", "opaqueHttpStreamResponse",
            "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseCredit", "opaqueHttpStreamResponseEnd");
    private static final Set<String> STREAM = new HashSet<>(HTTP);
    static { STREAM.addAll(Set.of("opaquePush", "opaquePushAck")); }
    private final List<Event> events = new ArrayList<>();
    private List<String> missing;
    private Event accepted;
    private long[] pids;
    void accept(JsonNode root) { events.add(new Event(root, events.size())); }

    void require(List<String> missing) {
        this.missing = missing;
        accepted = one(PREFIX + "frameworkAcceptance");
        List<Event> starts = of("sidecar.started"), launches = of("launch.sidecar");
        check(starts.size() == 3 && launches.size() == 3 && accepted != null,
                "exact three sidecar launches and one completed acceptance");
        if (starts.size() != 3 || launches.size() != 3 || accepted == null) return;
        pids = starts.stream().mapToLong(e -> integer(e.p(), "processIdentifier")).toArray();
        check(pids[0] > 0 && pids[1] > 0 && pids[2] > 0 && pids[0] != pids[1]
                && pids[1] != pids[2] && pids[0] != pids[2], "three distinct owned JVM identities");
        for (int i = 0; i < 3; i++) {
            Event start = starts.get(i), launch = launches.get(i);
            check(start.recv() && number(start.p(), "generation", i + 1)
                    && "flow-ipc".equals(text(start.p(), "transport"))
                    && launch.send() && number(launch.p(), "generation", i + 1)
                    && "flow-ipc".equals(text(launch.p(), "transport"))
                    && "$APP/Contents/Resources/Sidecar/application.jar".equals(text(launch.p(), "jar"))
                    && launch.order < start.order && start.order < accepted.order,
                    "ordered standard-bundle launch generation " + (i + 1));
        }
        requireLifecycle(starts, launches);
        requireStages();
        requireTransport();
        requireActivationDiagnostics();
        check(events.stream().noneMatch(e -> Set.of("error", "webview.error", "sidecar.startFailed",
                "sidecar.recoveryEscalationFailed", "sidecar.replacementRequested",
                "opaqueHttpStreamError", "opaqueHttpStreamCancel", "opaquePushError").contains(e.type())
                || e.type().startsWith(PREFIX) && !Set.of(PREFIX + "nativePointerDispatch",
                PREFIX + "stageObserved", PREFIX + "failureInjected", PREFIX + "blockedShutdownObserved",
                PREFIX + "frameworkAcceptance", PREFIX + "activationObservation").contains(e.type())),
                "no hidden error, replacement, or unknown acceptance event");
    }

    private void requireActivationDiagnostics() {
        List<Event> observations = of(PREFIX + "activationObservation");
        if (observations.isEmpty()) return; // Older retained traces have no timeline.
        check(observations.size() <= 128
                && number(accepted.p(), "activationObservationCount", observations.size())
                && integer(accepted.p(), "activationObservationDropped") >= 0,
                "bounded activation diagnostic counts");
        double previous = -1;
        for (int i = 0; i < observations.size(); i++) {
            Event event = observations.get(i);
            JsonNode p = event.p();
            double uptime = p.path("uptimeSeconds").isNumber()
                    ? p.path("uptimeSeconds").asDouble() : Double.NaN;
            check(event.recv() && event.order < accepted.order
                    && exact(p, "sequence", "reason", "uptimeSeconds", "phase", "pointerPhase",
                        "recoveryGeneration", "activeApplication", "keyWindow", "applicationKeyWindowMatches",
                        "windowVisible", "windowMiniaturized")
                    && number(p, "sequence", i + 1) && finitePositive(p, "uptimeSeconds") && uptime >= previous
                    && Set.of("observer-start", "app-active", "app-inactive", "window-key", "window-not-key",
                        "window-miniaturized", "window-restored", "acceptance-complete").contains(text(p, "reason"))
                    && Set.of("ready", "clicking", "result", "recovering", "drain").contains(text(p, "phase"))
                    && p.path("pointerPhase").isString()
                    && (number(p, "recoveryGeneration", 0) || number(p, "recoveryGeneration", 1))
                    && List.of("activeApplication", "keyWindow", "applicationKeyWindowMatches",
                        "windowVisible", "windowMiniaturized").stream().allMatch(k -> p.path(k).isBoolean()),
                    "ordered typed activation diagnostic " + i);
            previous = uptime;
        }
    }

    private void requireLifecycle(List<Event> starts, List<Event> launches) {
        List<Event> exits = of("sidecar.exit"), scheduled = of("sidecar.recoveryScheduled"),
                restarted = of("sidecar.recoveryStarted");
        Event timeout = one("sidecar.recoveryReadyTimeout"), term = one("sidecar.recoveryTerminated"),
                escalation = one("sidecar.recoveryEscalated"), ready = one("sidecar.recoveryReady"),
                injected = one(PREFIX + "failureInjected"), hook = one(PREFIX + "blockedShutdownObserved");
        check(exits.size() >= 2 && exits.size() <= 3 && scheduled.size() == 2 && restarted.size() == 2
                && timeout != null && term != null && escalation != null && ready != null
                && injected != null && hook != null, "complete unique timeout/TERM/escalation/actual-exit recovery chain");
        if (exits.size() < 2 || scheduled.size() != 2 || restarted.size() != 2
                || timeout == null || term == null || escalation == null || ready == null
                || injected == null || hook == null) return;
        for (int i = 0; i < 2; i++) {
            Event exit = exits.get(i), schedule = scheduled.get(i), restart = restarted.get(i);
            check(exit.recv() && number(exit.p(), "generation", i + 1)
                    && number(exit.p(), "processIdentifier", pids[i]) && number(exit.p(), "status", 9)
                    && number(exit.p(), "reason", 2) && flag(exit.p(), "expected", false),
                    "confirmed uncaught-signal exit of exact failed generation " + (i + 1));
            check(schedule.recv() && number(schedule.p(), "attempt", i + 1)
                    && number(schedule.p(), "delayMillis", i == 0 ? 500 : 1000)
                    && number(schedule.p(), "restartLimit", 2) && number(schedule.p(), "windowCount", 1)
                    && number(schedule.p(), "previousProcessIdentifier", pids[i])
                    && restart.send() && number(restart.p(), "attempt", i + 1)
                    && number(restart.p(), "previousProcessIdentifier", pids[i])
                    && exit.order < schedule.order && schedule.order < restart.order
                    && restart.order < launches.get(i + 1).order,
                    "actual exit precedes retry scheduling and next launch " + (i + 1));
        }
        check(timeout.send() && number(timeout.p(), "attempt", 1)
                && number(timeout.p(), "processIdentifier", pids[1])
                && timeout.p().path("timeoutSeconds").isNumber() && timeout.p().path("timeoutSeconds").asDouble() == 15
                && term.send() && number(term.p(), "generation", 2)
                && number(term.p(), "processIdentifier", pids[1]) && "ready-timeout".equals(text(term.p(), "reason"))
                && escalation.send() && number(escalation.p(), "generation", 2)
                && number(escalation.p(), "processIdentifier", pids[1])
                && "ready-timeout".equals(text(escalation.p(), "reason"))
                && escalation.p().path("graceSeconds").isNumber() && escalation.p().path("graceSeconds").asDouble() == 1,
                "exact timed-out generation receives one TERM and one fixed-grace escalation");
        long elapsed = integer(escalation.root, "at") - integer(term.root, "at");
        check(integer(term.root, "at") > 0 && integer(escalation.root, "at") > 0
                && elapsed >= 950 && elapsed <= 5000, "observed bounded grace interval (not a real-time scheduling guarantee)");
        check(starts.get(1).order < timeout.order && timeout.order < term.order && term.order < hook.order
                && hook.order < escalation.order && escalation.order < exits.get(1).order
                && injected.order < exits.get(0).order && exits.get(1).order < starts.get(2).order
                && starts.get(2).order < ready.order && ready.order < accepted.order,
                "blocked shutdown hook observed before escalation and real exit before third generation");
        check(ready.recv() && number(ready.p(), "attempt", 2)
                && number(ready.p(), "previousProcessIdentifier", pids[1])
                && number(ready.p(), "processIdentifier", pids[2]) && flag(ready.p(), "processReplaced", true)
                && number(ready.p(), "windowsReloaded", 1) && number(ready.p(), "recoveryGeneration", 1),
                "only ready third generation reloads the retained window once");
        Event window = one("window.create");
        check(window != null && window.send() && scope(window.p())
                && "main".equals(text(window.p(), "label")) && "flow-ipc".equals(text(window.p(), "mode"))
                && starts.getFirst().order < window.order && window.order < injected.order,
                "one scoped Flow IPC native window throughout recovery");
        List<Event> javaReady = of("ready").stream().filter(e -> e.recv() || e.p().has("envelope")).toList();
        List<Event> loaded = of("webview.loaded");
        check(javaReady.size() == 2 && loaded.size() == 2, "only first and third JVMs become ready and load documents");
        if (javaReady.size() == 2 && loaded.size() == 2) for (int i = 0; i < 2; i++) {
            int generation = i == 0 ? 1 : 3;
            Event r = javaReady.get(i), load = loaded.get(i);
            check(r.recv() && number(r.p(), "sidecarGeneration", generation)
                    && "java-sidecar".equals(text(r.p(), "source")) && "flow-ipc".equals(text(r.p(), "transport"))
                    && number(r.envelope(), "version", 1) && "ready".equals(text(r.envelope(), "type"))
                    && r.envelope().path("error").isNull() && scope(r.envelope())
                    && "standard-vaadin-bundle-opaque-ipc".equals(text(r.body(), "runtime"))
                    && "flow-ipc".equals(text(r.body(), "transport"))
                    && "standard-flow-client".equals(text(r.body(), "uidlConsumer"))
                    && "opaque-http-stream-ipc".equals(text(r.body(), "browserRequestTransport"))
                    && starts.get(generation - 1).order < r.order && r.order < load.order
                    && load.recv() && scope(load.p()) && number(load.p(), "recoveryGeneration", i)
                    && (i == 0 ? window != null && r.order < window.order : r.order < ready.order)
                    && epoch("opaqueHttpStreamOpen", generation, accepted.order).stream().allMatch(e -> r.order < e.order),
                    "exact standard Java ready and real document load epoch " + generation);
        }
        List<Event> shutdown = of("shutdown.sidecar");
        check(shutdown.size() == 1 && shutdown.getFirst().recv()
                && "graceful".equals(text(shutdown.getFirst().p(), "mode"))
                && accepted.order < shutdown.getFirst().order, "final third-generation graceful shutdown after acceptance");
        if (exits.size() == 3) {
            Event exit = exits.get(2);
            check(exit.recv() && number(exit.p(), "generation", 3)
                    && number(exit.p(), "processIdentifier", pids[2]) && number(exit.p(), "status", 0)
                    && flag(exit.p(), "expected", true) && accepted.order < exit.order,
                    "optional final actual exit is expected and successful");
        }
    }

    private static final Set<String> FRAME_KEYS = Set.of("windowId", "sessionId", "uiId", "runId", "fixtureGeneration", "processIdentifier", "recoveryGeneration", "documentMarker", "browserDocumentMarker", "browserDocumentRetained", "documentTimeOriginMillis", "firstMeaningfulPaint", "flowClientCount", "mainContextRetained", "mainWindowRetained", "mainWebViewRetained", "mainWindowNumber");
    private static final List<String> FRAME_TRUE = List.of("firstMeaningfulPaint", "mainContextRetained",
            "mainWindowRetained", "mainWebViewRetained", "browserDocumentRetained");
    private static final List<String> ACCEPTED_TRUE = List.of("complete", "initialJavaListenerObserved",
            "recoveredJavaListenerObserved", "initialUidlObserved", "recoveredUidlObserved", "initialPushObserved",
            "recoveredPushObserved", "blockedShutdownHookObserved", "blockedProcessAbsent", "newDocumentObserved",
            "balancedDrainObserved", "resultStableAfterDrain", "errorFree");

    private void requireStages() {
        List<Event> pointers = of(PREFIX + "nativePointerDispatch"), stages = of(PREFIX + "stageObserved");
        Event injected = one(PREFIX + "failureInjected"), hook = one(PREFIX + "blockedShutdownObserved");
        check(pointers.size() == 2 && stages.size() == 2
                && events.stream().filter(e -> e.type().startsWith(PREFIX)
                    && !e.type().equals(PREFIX + "activationObservation")).count() == 7,
                "exact two generated AppKit inputs and Java/push receipts");
        if (pointers.size() != 2 || stages.size() != 2 || injected == null || hook == null) return;
        for (int i = 0; i < 2; i++) {
            Event pointer = pointers.get(i), stage = stages.get(i);
            JsonNode p = pointer.p(), s = stage.p();
            int generation = i == 0 ? 1 : 3;
            check(pointer.recv() && frame(p, generation, i) && stage.recv() && frame(s, generation, i)
                    && FRAME_KEYS.stream().allMatch(k -> p.path(k).equals(s.path(k)))
                    && extendedKeys(p, "stage", "index", "inputMode", "button", "activeApplication", "keyWindow",
                        "webViewX", "webViewY", "windowX", "windowY", "dispatchedAt",
                        "requestBaseline", "responseBaseline", "pushBaseline", "counterBefore", "pushPending")
                    && extendedKeys(s, "stage", "index", "counter", "pushCount", "client",
                        "backgroundSessionAbsent", "nativePointerReceiptCount", "nativePointerTrustedCount",
                        "requestBaseline", "responseBaseline", "pushBaseline", "requestCount", "responseCount",
                        "pushMessageCount", "balancedDrainObserved"),
                    "strict pointer/Java receipt schema with exact document identity " + generation);
            String label = i == 0 ? "initial" : "recovered";
            check(label.equals(text(p, "stage")) && label.equals(text(s, "stage"))
                    && number(p, "index", i) && number(s, "index", i)
                    && "appkit-window-pointer-event".equals(text(p, "inputMode"))
                    && "primary".equals(text(p, "button")) && flag(p, "activeApplication", true)
                    && flag(p, "keyWindow", true) && flag(p, "pushPending", true) && number(p, "counterBefore", 0)
                    && List.of("webViewX", "webViewY", "windowX", "windowY", "dispatchedAt").stream()
                        .allMatch(k -> finitePositive(p, k))
                    && number(s, "counter", 1) && number(s, "pushCount", 1)
                    && flag(s, "client", true) && flag(s, "backgroundSessionAbsent", true)
                    && flag(s, "balancedDrainObserved", true) && number(s, "nativePointerReceiptCount", 1)
                    && number(s, "nativePointerTrustedCount", 1)
                    && of("sidecar.started").get(generation - 1).order < pointer.order
                    && pointer.order < stage.order && stage.order < accepted.order,
                    "one trusted native input, public listener and background push per document " + generation);
            for (String key : List.of("requestBaseline", "responseBaseline", "pushBaseline"))
                check(integer(p, key) >= 0 && number(s, key, integer(p, key)), "matching numeric " + key);
            check(integer(s, "requestCount") > integer(s, "requestBaseline")
                    && integer(s, "responseCount") > integer(s, "responseBaseline")
                    && integer(s, "pushMessageCount") > integer(s, "pushBaseline")
                    && integer(s, "requestCount") == integer(s, "responseCount"),
                    "fresh balanced round-trip and push after each native input");
        }
        JsonNode initial = stages.getFirst().p(), recovered = stages.getLast().p(), a = accepted.p();
        String runId = text(initial, "runId");
        check(runId.equals(text(recovered, "runId")) && runId.equals(text(hook.p(), "runId"))
                && !text(initial, "documentMarker").equals(text(recovered, "documentMarker"))
                && !text(initial, "browserDocumentMarker").equals(text(recovered, "browserDocumentMarker"))
                && finitePositive(initial, "documentTimeOriginMillis") && finitePositive(recovered, "documentTimeOriginMillis")
                && initial.path("documentTimeOriginMillis").asDouble() < recovered.path("documentTimeOriginMillis").asDouble()
                && number(initial, "mainWindowNumber", integer(recovered, "mainWindowNumber")),
                "same run and native window, fresh third-generation document");
        check(injected.send() && frame(injected.p(), 1, 0)
                && extendedKeys(injected.p(), "signal", "targetProcessIdentifier", "ownedProcessConfirmed",
                    "initialInteractionSettled")
                && FRAME_KEYS.stream().allMatch(k -> initial.path(k).equals(injected.p().path(k)))
                && number(injected.p(), "signal", 9) && number(injected.p(), "targetProcessIdentifier", pids[0])
                && flag(injected.p(), "ownedProcessConfirmed", true) && flag(injected.p(), "initialInteractionSettled", true)
                && stages.getFirst().order < injected.order,
                "precise initial-process fault injection after settled real Flow interaction");
        check(hook.recv() && exact(hook.p(), "windowId", "sessionId", "uiId", "runId", "fixtureGeneration",
                "processIdentifier", "initialProcessIdentifier", "recoveryGeneration", "hookMarkerMatched",
                "processMarkerMatched", "mainContextRetained", "mainWindowRetained", "mainWebViewRetained")
                && scope(hook.p()) && number(hook.p(), "fixtureGeneration", 2)
                && number(hook.p(), "processIdentifier", pids[1]) && number(hook.p(), "initialProcessIdentifier", pids[0])
                && number(hook.p(), "recoveryGeneration", 0)
                && List.of("hookMarkerMatched", "processMarkerMatched", "mainContextRetained", "mainWindowRetained",
                    "mainWebViewRetained").stream().allMatch(k -> flag(hook.p(), k, true)),
                "blocked exact second-process shutdown hook with untouched native owner");
        Set<String> acceptedKeys = new HashSet<>(FRAME_KEYS);
        acceptedKeys.addAll(ACCEPTED_TRUE);
        if (!of(PREFIX + "activationObservation").isEmpty())
            acceptedKeys.addAll(List.of("activationObservationCount", "activationObservationDropped"));
        acceptedKeys.addAll(List.of("initialProcessIdentifier", "blockedProcessIdentifier", "recoveredProcessIdentifier",
                "initialDocumentMarker", "initialBrowserDocumentMarker", "initialDocumentTimeOriginMillis", "initialRecoveryGeneration",
                "finalRecoveryGeneration", "initialRequestCount", "initialResponseCount", "initialPushMessageCount",
                "recoveredRequestCount", "recoveredResponseCount", "recoveredPushMessageCount",
                "nativePointerEventCount", "nativePointerTrustedEventCount", "nativePointerStages"));
        check(accepted.recv() && frame(a, 3, 1) && a.propertyNames().equals(acceptedKeys)
                && FRAME_KEYS.stream().allMatch(k -> recovered.path(k).equals(a.path(k)))
                && ACCEPTED_TRUE.stream().allMatch(k -> flag(a, k, true))
                && number(a, "initialProcessIdentifier", pids[0]) && number(a, "blockedProcessIdentifier", pids[1])
                && number(a, "recoveredProcessIdentifier", pids[2]) && number(a, "initialRecoveryGeneration", 0)
                && number(a, "finalRecoveryGeneration", 1) && number(a, "nativePointerEventCount", 2)
                && number(a, "nativePointerTrustedEventCount", 2)
                && "initial,recovered".equals(text(a, "nativePointerStages"))
                && text(initial, "documentMarker").equals(text(a, "initialDocumentMarker"))
                && text(initial, "browserDocumentMarker").equals(text(a, "initialBrowserDocumentMarker"))
                && initial.path("documentTimeOriginMillis").equals(a.path("initialDocumentTimeOriginMillis")),
                "strict final receipt including retained owner and fresh-document boundaries");
        for (int i = 0; i < 2; i++) {
            JsonNode stage = stages.get(i).p();
            String prefix = i == 0 ? "initial" : "recovered";
            for (String key : List.of("RequestCount", "ResponseCount", "PushMessageCount"))
                check(number(a, prefix + key, integer(stage, Character.toLowerCase(key.charAt(0)) + key.substring(1))),
                        "final drained counts match stage " + prefix + key);
        }
        Event ready = of("sidecar.recoveryReady").stream().findFirst().orElse(null);
        check(ready != null && ready.order < pointers.getLast().order, "recovered input only after actual third-generation ready");
    }

    private boolean frame(JsonNode p, int generation, int epoch) {
        return scope(p) && uuid(text(p, "runId")) && uuid(text(p, "documentMarker"))
                && uuid(text(p, "browserDocumentMarker"))
                && number(p, "fixtureGeneration", generation) && number(p, "processIdentifier", pids[generation - 1])
                && number(p, "recoveryGeneration", epoch) && number(p, "flowClientCount", 1)
                && integer(p, "mainWindowNumber") > 0 && finitePositive(p, "documentTimeOriginMillis")
                && FRAME_TRUE.stream().allMatch(k -> flag(p, k, true));
    }
    private static boolean extendedKeys(JsonNode p, String... extra) {
        Set<String> keys = new HashSet<>(FRAME_KEYS); keys.addAll(List.of(extra));
        return p.isObject() && p.propertyNames().equals(keys);
    }
    private static boolean uuid(String value) {
        try { return java.util.UUID.fromString(value).toString().equals(value); }
        catch (IllegalArgumentException ex) { return false; }
    }
    private static boolean finitePositive(JsonNode p, String key) {
        return p.path(key).isNumber() && Double.isFinite(p.path(key).asDouble()) && p.path(key).asDouble() > 0;
    }

    private void requireTransport() {
        List<Event> pointers = of(PREFIX + "nativePointerDispatch"), stages = of(PREFIX + "stageObserved");
        if (pointers.size() != 2 || stages.size() != 2 || of(PREFIX + "failureInjected").size() != 1) return;
        for (Event e : events) {
            if (!STREAM.contains(e.type())) continue;
            int generation = generationAt(e);
            boolean send = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamChunk", "opaqueHttpStreamEnd",
                    "opaqueHttpStreamResponseCredit", "opaquePushAck").contains(e.type());
            check((generation == 1 || generation == 3) && (send ? e.send() : e.recv())
                    && "flow-ipc".equals(text(e.p(), "transport"))
                    && "java-sidecar".equals(text(e.p(), send ? "target" : "source"))
                    && (send ? !e.p().has("sidecarGeneration") || number(e.p(), "sidecarGeneration", generation)
                        : number(e.p(), "sidecarGeneration", generation))
                    && number(e.envelope(), "version", 1) && e.type().equals(text(e.envelope(), "type"))
                    && scope(e.envelope()) && e.envelope().path("error").isNull() && e.body().isObject()
                    && !text(e.envelope(), "id").isBlank() && !text(e.envelope(), "correlationId").isBlank()
                    && e.order < accepted.order,
                    "all checked opaque traffic belongs to the live ready generation and retained scope");
        }
        for (int i = 0; i < 2; i++) {
            int generation = i == 0 ? 1 : 3;
            Event pointer = pointers.get(i), stage = stages.get(i);
            int terminalOrder = i == 0 ? of(PREFIX + "failureInjected").getFirst().order : accepted.order;
            List<Event> opens = epoch("opaqueHttpStreamOpen", generation, terminalOrder);
            List<Event> responses = epoch("opaqueHttpStreamResponse", generation, terminalOrder);
            List<Event> ends = epoch("opaqueHttpStreamResponseEnd", generation, terminalOrder);
            check(number(pointer.p(), "requestBaseline", opens.stream().filter(e -> e.order < pointer.order).count())
                    && number(pointer.p(), "responseBaseline", ends.stream().filter(e -> e.order < pointer.order).count())
                    && number(stage.p(), "requestCount", opens.size()) && number(stage.p(), "responseCount", ends.size()),
                    "document request baselines and settled counters equal actual scoped traffic");
            check(opens.size() >= 2 && opens.size() == responses.size() && opens.size() == ends.size()
                    && opens.stream().map(e -> text(e.body(), "requestId")).distinct().count() == opens.size(),
                    "generation " + generation + " balanced unique opaque requests");
            check(events.stream().filter(e -> HTTP.contains(e.type()) && generationAt(e) == generation)
                    .allMatch(e -> !text(e.body(), "requestId").isBlank()
                        && text(e.body(), "requestId").equals(text(e.envelope(), "correlationId"))
                        && opens.stream().anyMatch(o -> text(o.body(), "requestId").equals(text(e.body(), "requestId")))),
                    "every stream frame belongs to an actual request in its exact epoch");
            boolean roundTrip = false;
            for (Event open : opens) {
                String id = text(open.body(), "requestId");
                List<Event> rs = responses.stream().filter(e -> id.equals(text(e.body(), "requestId"))).toList();
                List<Event> es = ends.stream().filter(e -> id.equals(text(e.body(), "requestId"))).toList();
                boolean valid = !id.isBlank() && rs.size() == 1 && es.size() == 1
                        && id.equals(text(open.envelope(), "correlationId"));
                if (valid) valid = integer(rs.getFirst().body(), "status") >= 200
                        && integer(rs.getFirst().body(), "status") < 400
                        && id.equals(text(rs.getFirst().envelope(), "correlationId"))
                        && id.equals(text(es.getFirst().envelope(), "correlationId"))
                        && open.order < rs.getFirst().order && rs.getFirst().order < es.getFirst().order;
                check(valid, "ordered successful exact request/response " + generation + "/" + id);
                if (valid) {
                    requireBody(open, rs.getFirst(), es.getFirst(), generation, true);
                    List<Event> requestEnds = epoch("opaqueHttpStreamEnd", generation, terminalOrder).stream()
                            .filter(e -> id.equals(text(e.body(), "requestId"))).toList();
                    check(requestEnds.size() == 1, "exact one request submission end");
                    if (requestEnds.size() == 1) requireBody(open, open, requestEnds.getFirst(), generation, false);
                }
                roundTrip |= valid && pointer.order < open.order && "POST".equals(text(open.body(), "method"));
            }
            List<Event> pushes = epoch("opaquePush", generation, terminalOrder);
            List<Event> acks = epoch("opaquePushAck", generation, terminalOrder);
            check(number(pointer.p(), "pushBaseline", pushes.stream().filter(e -> e.order < pointer.order)
                        .map(this::pushKey).distinct().count())
                    && number(stage.p(), "pushMessageCount", pushes.stream().map(this::pushKey).distinct().count()),
                    "document push baselines and settled count equal actual scoped logical deliveries");
            check(roundTrip && !pushes.isEmpty() && pushes.size() == acks.size()
                    && pushes.stream().anyMatch(e -> pointer.order < e.order),
                    "fresh listener request and acknowledged post-click push per recovered document");
            Map<String, List<Event>> logical = new LinkedHashMap<>();
            for (Event push : pushes) logical.computeIfAbsent(pushKey(push), ignored -> new ArrayList<>()).add(push);
            check(acks.stream().allMatch(e -> logical.containsKey(pushKey(e))), "no orphan push acknowledgement");
            Map<String, Long> sequences = new LinkedHashMap<>();
            for (Map.Entry<String, List<Event>> group : logical.entrySet()) {
                List<Event> attempts = group.getValue();
                List<Event> receipts = acks.stream().filter(e -> group.getKey().equals(pushKey(e))).toList();
                Event first = attempts.getFirst();
                String connection = text(first.body(), "connectionId") + "/" + integer(first.body(), "flowUiId");
                long sequence = integer(first.body(), "pushSequence");
                long previous = sequences.getOrDefault(connection, 0L);
                check(previous < Long.MAX_VALUE && sequence == previous + 1,
                        "logical push sequence starts at one and is contiguous in the fresh epoch");
                sequences.put(connection, sequence);
                check(first.order < stage.order && attempts.size() == receipts.size(),
                        "settled logical push has an ordinal acknowledgement for every attempt");
                check(receipts.stream().anyMatch(e -> integer(e.body(), "lastSeenServerSyncId")
                        >= integer(first.body(), "flowServerSyncId")),
                        "logical push eventually acknowledges Flow consumption, not only transport receipt");
                for (int j = 0; j < Math.min(attempts.size(), receipts.size()); j++) {
                    Event push = attempts.get(j), ack = receipts.get(j);
                    check(push.order < ack.order
                            && !text(push.body(), "connectionId").isBlank() && integer(push.body(), "flowUiId") >= 0
                            && integer(push.body(), "flowServerSyncId") >= 0 && number(push.body(), "deliveryAttempt", j + 1)
                            && number(push.body(), "uidlBytes", integer(first.body(), "uidlBytes"))
                            && flag(push.body(), "resync", false) && integer(ack.body(), "lastSeenServerSyncId") >= -1
                            && integer(push.body(), "pushSequence") > 0 && integer(push.body(), "uidlBytes") > 0
                            && flag(push.body(), "uidlRedacted", true) && !push.body().has("uidl"),
                            "exact redacted same-sequence retry and ordinal ACK " + generation);
                }
            }
            check(events.stream().filter(e -> STREAM.contains(e.type()) && generationAt(e) == generation)
                    .allMatch(e -> e.order < terminalOrder
                        && (Set.of("opaquePush", "opaquePushAck").contains(e.type()) || e.order < stage.order)),
                    "no unaccounted new traffic after settled document receipt");
        }
    }

    private void requireBody(Event open, Event start, Event end, int generation, boolean response) {
        String id = text(open.body(), "requestId");
        List<Event> chunks = epoch(response ? "opaqueHttpStreamResponseChunk" : "opaqueHttpStreamChunk", generation, accepted.order)
                .stream().filter(e -> id.equals(text(e.body(), "requestId"))).toList();
        List<Event> credits = epoch(response ? "opaqueHttpStreamResponseCredit" : "opaqueHttpStreamRequestCredit", generation, accepted.order)
                .stream().filter(e -> id.equals(text(e.body(), "requestId"))).toList();
        check(credits.stream().allMatch(e -> integer(e.body(), "credit") > 0
                && integer(e.body(), "credit") <= Integer.MAX_VALUE), "positive bounded stream credits");
        long bytes = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Event chunk = chunks.get(i);
            long available = credits.stream().filter(e -> e.order < chunk.order)
                    .mapToLong(e -> integer(e.body(), "credit")).sum();
            check(number(chunk.body(), "chunkSequence", i) && integer(chunk.body(), "bodyBytes") > 0
                    && integer(chunk.body(), "bodyBytes") <= Integer.MAX_VALUE
                    && flag(chunk.body(), "bodyRedacted", true) && !chunk.body().has("bodyBase64")
                    && start.order < chunk.order && chunk.order < end.order && available > i,
                    "ordered credited redacted body chunks");
            bytes += Math.max(0, integer(chunk.body(), "bodyBytes"));
        }
        check(start.order < end.order && number(end.body(), "bodyBytes", bytes)
                && number(end.body(), "chunkCount", chunks.size()) && (!response || bytes > 0),
                "complete nonempty response or fully submitted request body");
        if (!response && open.body().has("contentLength"))
            check(number(open.body(), "contentLength", -1) || number(open.body(), "contentLength", bytes),
                    "request body matches declared length");
    }

    private int generationAt(Event event) {
        int generation = 0;
        for (Event start : of("sidecar.started")) if (start.order < event.order) generation = (int) integer(start.p(), "generation");
        return generation;
    }
    private List<Event> epoch(String type, int generation, int before) {
        return of(type).stream().filter(e -> e.order < before && generationAt(e) == generation).toList();
    }
    private String pushKey(Event e) {
        return generationAt(e) + "/" + text(e.body(), "connectionId") + "/" + integer(e.body(), "flowUiId")
                + "/" + integer(e.body(), "pushSequence") + "/" + integer(e.body(), "flowServerSyncId");
    }
    private List<Event> of(String type) { return events.stream().filter(e -> type.equals(e.type())).toList(); }
    private Event one(String type) {
        List<Event> all = of(type); check(all.size() == 1, "exactly one " + type); return all.size() == 1 ? all.getFirst() : null;
    }
    private void check(boolean condition, String description) { if (!condition) missing.add(description); }
    private static boolean scope(JsonNode p) {
        return "main".equals(text(p, "windowId")) && "flow-ipc-session".equals(text(p, "sessionId"))
                && "main".equals(text(p, "uiId"));
    }
    private static String text(JsonNode p, String k) { return p.path(k).isString() ? p.path(k).asString() : ""; }
    private static long integer(JsonNode p, String k) {
        JsonNode n = p.path(k); return n.isIntegralNumber() && n.canConvertToLong() ? n.asLong() : Long.MIN_VALUE;
    }
    private static boolean number(JsonNode p, String k, long n) { return integer(p, k) == n; }
    private static boolean flag(JsonNode p, String k, boolean v) { return p.path(k).isBoolean() && p.path(k).asBoolean() == v; }
    private static boolean exact(JsonNode p, String... keys) { return p.isObject() && p.propertyNames().equals(Set.of(keys)); }
    private record Event(JsonNode root, int order) {
        String type() { return text(root, "type"); }
        JsonNode p() { return root.path("payload"); }
        JsonNode envelope() { return p().path("envelope"); }
        JsonNode body() { return envelope().path("payload"); }
        boolean recv() { return "recv".equals(text(root, "direction")); }
        boolean send() { return "send".equals(text(root, "direction")); }
    }
}
