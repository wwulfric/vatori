package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Public final-window close through actual JVM exit and independent external observation. */
final class HostShutdownTerminationTraceGate {
    private static final String PREFIX = "flowHostShutdownTermination.";
    private static final String MANIFEST = "$APP/Contents/Resources/desktop-capabilities.json";
    private static final Set<String> COMMON = Set.of("windowId", "sessionId", "uiId", "runId", "mode",
            "processIdentifier", "hostProcessIdentifier", "recoveryGeneration");
    private static final Set<String> HTTP = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamEnd",
            "opaqueHttpStreamChunk", "opaqueHttpStreamRequestCredit", "opaqueHttpStreamResponse",
            "opaqueHttpStreamResponseCredit", "opaqueHttpStreamResponseChunk", "opaqueHttpStreamResponseEnd");
    private static final Set<String> WIRE = new HashSet<>(HTTP);
    static { WIRE.addAll(Set.of("opaquePush", "opaquePushAck")); }
    private final List<Event> events = new ArrayList<>();
    private List<String> missing;
    private Event prepared, finalPointer, termination, external, shutdown, ready;
    private long pid, hostPid;
    private String runId, mode;

    void accept(JsonNode root) { events.add(new Event(root, events.size())); }

    /** Reuse the real preparation prefix without fabricating a final close request or ACK. */
    static void requirePreparedTraffic(List<JsonNode> roots, String prefix, String boundaryType, List<String> missing) {
        HostShutdownTerminationTraceGate gate = new HostShutdownTerminationTraceGate();
        gate.missing = missing;
        for (JsonNode root : roots) {
            gate.accept(root);
            if (boundaryType.equals(text(root, "type"))) break;
        }
        gate.prepared = gate.one(prefix + "prepared");
        Event pointer = gate.one(prefix + "nativePointerDispatch");
        gate.finalPointer = gate.one(boundaryType);
        gate.shutdown = gate.finalPointer;
        List<Event> readies = gate.of("ready").stream().filter(e -> e.recv() && "java-sidecar".equals(text(e.p(), "source"))).toList();
        gate.check(readies.size() == 1, "one Java ready before prepared traffic");
        gate.ready = readies.size() == 1 ? readies.getFirst() : null;
        if (gate.prepared != null && pointer != null && gate.finalPointer != null && gate.ready != null) gate.requireTransport(pointer, null);
    }

    void require(List<String> missing) {
        this.missing = missing;
        prepared = one(PREFIX + "prepared");
        termination = one(PREFIX + "applicationWillTerminate");
        external = one(PREFIX + "externalExitObserved");
        List<Event> pointers = of(PREFIX + "nativePointerDispatch");
        check(pointers.size() == 2 && events.stream().filter(e -> e.type().startsWith(PREFIX)).count() == 5,
                "exact two native inputs, prepared, termination and final external receipt");
        if (prepared == null || termination == null || external == null || pointers.size() != 2) return;
        pid = integer(prepared.p(), "processIdentifier"); hostPid = integer(prepared.p(), "hostProcessIdentifier");
        runId = text(prepared.p(), "runId"); mode = text(prepared.p(), "mode");
        check(pid > 0 && pid <= Integer.MAX_VALUE && hostPid > 0 && hostPid <= Integer.MAX_VALUE && pid != hostPid
                && uuid(runId) && Set.of("cooperative", "blocked").contains(mode), "distinct exact owned process identities and mode");
        finalPointer = pointers.getLast();
        requireFocused(pointers);
        requireLifecycle(pointers.getFirst());
        Event title = requireCommands();
        if (shutdown != null && ready != null && title != null) requireTransport(pointers.getFirst(), title);
        check(events.stream().noneMatch(e -> Set.of("error", "webview.error", "sidecar.startFailed",
                        "sidecar.shutdownFailed", "host.sidecarShutdownFailed", "opaqueHttpStreamError",
                        "opaqueHttpStreamCancel", "opaquePushError", "opaquePushDisconnect", "opaquePushResync",
                        "staleCapabilityResultDropped").contains(e.type())
                        || e.type().startsWith("sidecar.recovery") || e.type().startsWith("sidecar.replacement")
                        || e.type().startsWith(PREFIX) && !Set.of(PREFIX + "nativePointerDispatch", PREFIX + "prepared",
                            PREFIX + "applicationWillTerminate", PREFIX + "externalExitObserved").contains(e.type())),
                "no hidden runtime failure, recovery, replacement, resync or unknown focused event");
    }

    private void requireFocused(List<Event> pointers) {
        for (int i = 0; i < 2; i++) {
            Event event = pointers.get(i); JsonNode p = event.p();
            check(event.recv() && identity(p) && extended(p, "stage", "index", "inputMode", "button",
                    "activeApplication", "keyWindow", "webViewX", "webViewY", "windowX", "windowY", "dispatchedAt")
                    && (i == 0 ? "prepare" : "close-last").equals(text(p, "stage")) && number(p, "index", i)
                    && "appkit-window-pointer-event".equals(text(p, "inputMode")) && "primary".equals(text(p, "button"))
                    && flag(p, "activeApplication", true) && flag(p, "keyWindow", true)
                    && List.of("webViewX", "webViewY", "windowX", "windowY", "dispatchedAt").stream().allMatch(k -> positive(p, k)),
                    "strict active/key generated AppKit pointer " + i);
        }
        JsonNode p = prepared.p();
        check(prepared.recv() && identity(p) && extended(p, "documentTimeOriginMillis", "mainWindowNumber",
                "routeReady", "firstMeaningfulPaint", "flowClientCount", "mainContextRetained", "mainWindowRetained",
                "mainWebViewRetained", "counter", "pushCount", "client", "trusted", "backgroundSessionAbsent",
                "requestBaseline", "responseBaseline", "pushBaseline", "requestCount", "responseCount", "pushMessageCount",
                "nativePointerReceiptCount", "nativePointerTrustedCount", "balancedDrainObserved")
                && positive(p, "documentTimeOriginMillis") && integer(p, "mainWindowNumber") > 0
                && List.of("flowClientCount", "counter", "pushCount", "nativePointerReceiptCount", "nativePointerTrustedCount")
                    .stream().allMatch(k -> number(p, k, 1))
                && List.of("routeReady", "firstMeaningfulPaint", "mainContextRetained", "mainWindowRetained",
                    "mainWebViewRetained", "client", "trusted", "backgroundSessionAbsent", "balancedDrainObserved")
                    .stream().allMatch(k -> flag(p, k, true)), "strict ordinary Java listener and background push receipt");
        check(pointers.getFirst().order < prepared.order && prepared.order < finalPointer.order,
                "visible prepared state precedes final native close input");
        JsonNode t = termination.p();
        check(termination.recv() && identity(t) && extended(t, "complete", "preparedObserved", "finalPointerDispatched",
                "terminationSource", "currentProcessIdentifier", "originalSidecarAbsent", "originalWindowRetained",
                "launcherReturnedMarkerMatched", "hookEnteredMarkerMatched")
                && "applicationWillTerminate".equals(text(t, "terminationSource")) && number(t, "currentProcessIdentifier", 0)
                && flag(t, "originalWindowRetained", false)
                && List.of("complete", "preparedObserved", "finalPointerDispatched", "originalSidecarAbsent",
                    "launcherReturnedMarkerMatched", "hookEnteredMarkerMatched").stream().allMatch(k -> flag(t, k, true)),
                "AppKit termination observes actual absent child and matching launcher/hook records");
        JsonNode e = external.p();
        check(external.recv() && identity(e) && extended(e, "schemaVersion", "hostAbsent", "sidecarAbsent",
                "markersMatched", "cleanupPerformed") && number(e, "schemaVersion", 1)
                && flag(e, "hostAbsent", true) && flag(e, "sidecarAbsent", true)
                && flag(e, "markersMatched", true) && flag(e, "cleanupPerformed", false)
                && termination.order < external.order && external.order == events.size() - 1,
                "last independent receipt observes both absent before any runner cleanup");
    }

    private void requireLifecycle(Event pointer) {
        Event manifest = one("capabilityManifest"), launch = one("launch.sidecar"), start = one("sidecar.started");
        List<Event> readies = of("ready").stream().filter(e -> e.recv() || e.p().has("envelope")).toList();
        check(readies.size() == 1, "one actual Java ready"); ready = readies.size() == 1 ? readies.getFirst() : null;
        Event create = one("window.create"), loaded = one("webview.loaded"), close = one("window.closeRequested");
        Event closed = one("windowClosed"), drain = one("host.nativeResourceDrainStarted"), drained = one("host.nativeResourceDrainCompleted");
        shutdown = one("shutdown");
        Event exit = one("sidecar.exit"), done = one("shutdown.sidecar"), completed = one("host.sidecarShutdownCompleted");
        if (manifest == null || launch == null || start == null || ready == null || create == null || loaded == null
                || close == null || closed == null || drain == null || drained == null || shutdown == null
                || exit == null || done == null || completed == null) return;
        JsonNode m = manifest.p(), windows = m.path("commandsByWindow");
        check(manifest.recv() && flag(m, "loaded", true) && "manifest-enforced".equals(text(m, "permissionMode"))
                && MANIFEST.equals(text(m, "path")) && m.path("parseError").isString() && text(m, "parseError").isEmpty()
                && windows.isObject() && windows.size() == 1 && textArray(windows.path("main"), List.of("window.close", "window.setTitle")),
                "exact main-only least-privilege close and title manifest");
        check(launch.send() && number(launch.p(), "generation", 1) && "flow-ipc".equals(text(launch.p(), "transport"))
                && "$APP/Contents/Resources/Sidecar/application.jar".equals(text(launch.p(), "jar"))
                && start.recv() && number(start.p(), "generation", 1) && number(start.p(), "processIdentifier", pid)
                && "flow-ipc".equals(text(start.p(), "transport")) && envelope(ready, false, true)
                && "standard-vaadin-bundle-opaque-ipc".equals(text(ready.body(), "runtime"))
                && "flow-ipc".equals(text(ready.body(), "transport"))
                && "standard-flow-client".equals(text(ready.body(), "uidlConsumer"))
                && "opaque-http-stream-ipc".equals(text(ready.body(), "browserRequestTransport"))
                && create.send() && scope(create.p()) && "main".equals(text(create.p(), "label"))
                && "flow-ipc".equals(text(create.p(), "mode")) && loaded.recv() && scope(loaded.p())
                && number(loaded.p(), "recoveryGeneration", 0)
                && ordered(manifest, launch, start, ready, create, loaded, pointer), "one standard ready JVM and original loaded main window");
        check(close.recv() && exact(close.p(), "label", "windowId", "sessionId", "uiId") && scope(close.p())
                && "main".equals(text(close.p(), "label")) && closed.send() && scope(closed.p())
                && exact(closed.p(), "windowId", "sessionId", "uiId", "target", "transport")
                && "java-sidecar".equals(text(closed.p(), "target")) && "flow-ipc".equals(text(closed.p(), "transport")),
                "public main-window close reaches native retirement and windowClosed");
        check(drain.recv() && exact(drain.p(), "pendingCount", "timeoutSeconds") && number(drain.p(), "pendingCount", 0)
                && scalar(drain.p(), "timeoutSeconds", 2) && drained.recv() && exact(drained.p(), "drained", "pendingCount", "failureCount")
                && flag(drained.p(), "drained", true) && number(drained.p(), "pendingCount", 0) && number(drained.p(), "failureCount", 0)
                && shutdown.send() && exact(shutdown.p(), "target", "transport", "reason")
                && "java-sidecar".equals(text(shutdown.p(), "target")) && "flow-ipc".equals(text(shutdown.p(), "transport"))
                && "host-terminate".equals(text(shutdown.p(), "reason")), "empty native resource drain before shutdown channel request");
        boolean blocked = mode.equals("blocked");
        check(exit.recv() && exact(exit.p(), "generation", "processIdentifier", "status", "reason", "expected")
                && number(exit.p(), "generation", 1) && number(exit.p(), "processIdentifier", pid) && flag(exit.p(), "expected", true)
                && number(exit.p(), "status", blocked ? 9 : 0) && number(exit.p(), "reason", blocked ? 2 : 1)
                && done.recv() && exact(done.p(), "mode") && (blocked ? "killed" : "graceful").equals(text(done.p(), "mode"))
                && completed.recv() && exact(completed.p(), "succeeded") && flag(completed.p(), "succeeded", true)
                && ordered(finalPointer, close, closed, drain, drained, shutdown, exit, done, completed, termination, external),
                "actual mode-specific child exit precedes shutdown completion and AppKit/external receipts");
        if (blocked) {
            Event term = one("sidecar.shutdownTerminated"), kill = one("sidecar.shutdownEscalated");
            if (term != null && kill != null) {
                for (Event signal : List.of(term, kill)) check(signal.send()
                        && exact(signal.p(), "generation", "processIdentifier", "graceSeconds")
                        && number(signal.p(), "generation", 1) && number(signal.p(), "processIdentifier", pid)
                        && scalar(signal.p(), "graceSeconds", 1), "signal targets exact original JVM generation");
                check(ordered(shutdown, term, kill, exit) && elapsed(shutdown, term, 950, 5000)
                        && elapsed(term, kill, 950, 5000) && elapsed(kill, exit, 0, 5000),
                        "bounded channel grace then TERM grace then confirmed KILL exit");
            }
        } else check(of("sidecar.shutdownTerminated").isEmpty() && of("sidecar.shutdownEscalated").isEmpty(),
                "cooperative hook exits without any TERM or KILL");
    }

    private Event requireCommands() {
        List<Event> commands = of("command"), results = of("capabilityResult");
        check(commands.size() == 2 && results.size() == 2, "exact title and final-close commands/results");
        if (commands.size() != 2 || results.size() != 2) return null;
        String title = "Host shutdown close: runId=" + runId + " pid=" + pid + " mode=" + mode + " trusted=true closeCount=1";
        for (int i = 0; i < 2; i++) {
            Event command = commands.get(i), result = results.get(i);
            String name = i == 0 ? "window.setTitle" : "window.close", field = i == 0 ? "title" : "id", value = i == 0 ? title : "main";
            String id = text(command.envelope(), "id");
            check(envelope(command, false, true) && envelope(result, true, true) && name.equals(text(command.body(), "command"))
                    && exact(command.body().path("args"), field) && value.equals(text(command.body().path("args"), field))
                    && id.equals(text(command.envelope(), "correlationId")) && id.equals(text(result.envelope(), "correlationId"))
                    && ("capability-" + id).equals(text(result.envelope(), "id")) && name.equals(text(result.p(), "command"))
                    && name.equals(text(result.body(), "command")) && flag(result.body(), "success", true)
                    && "allowed".equals(text(result.body(), "permissionDecision")) && "manifest-allow".equals(text(result.body(), "permissionReason")),
                    "scope/generation/correlation-exact public " + name);
            if (i == 0) check(exact(result.body(), "command", "success", "title", "mode", "permissionMode", "permissionDecision",
                    "permissionReason", "permissionScope", "permissionManifestLoaded", "permissionManifestPath", "permissionManifestCommands")
                    && title.equals(text(result.body(), "title")) && "native-appkit".equals(text(result.body(), "mode"))
                    && "manifest-enforced".equals(text(result.body(), "permissionMode")) && "window:main".equals(text(result.body(), "permissionScope"))
                    && flag(result.body(), "permissionManifestLoaded", true) && MANIFEST.equals(text(result.body(), "permissionManifestPath"))
                    && "window.close,window.setTitle".equals(text(result.body(), "permissionManifestCommands")), "trusted final listener exact native title receipt");
            else check(exact(result.body(), "command", "success", "windowId", "permissionDecision", "permissionReason")
                    && "main".equals(text(result.body(), "windowId")), "successful public final-window close result");
        }
        Event closed = of("window.closeRequested").size() == 1 ? of("window.closeRequested").getFirst() : null;
        check(!text(commands.getFirst().envelope(), "id").equals(text(commands.getLast().envelope(), "id"))
                && ordered(finalPointer, commands.getFirst(), results.getFirst(), commands.getLast(), results.getLast())
                && closed != null && results.getLast().order < closed.order, "native final input, Java commands and native close are causally ordered");
        return commands.getFirst();
    }

    private void requireTransport(Event pointer, Event title) {
        for (Event event : events) if (WIRE.contains(event.type())) {
            boolean send = Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamChunk", "opaqueHttpStreamEnd", "opaqueHttpStreamResponseCredit", "opaquePushAck").contains(event.type());
            check(envelope(event, send, !send) && ready.order < event.order && event.order < shutdown.order,
                    "all opaque frames retain original ready scope/generation before shutdown");
        }
        List<Event> opens = of("opaqueHttpStreamOpen"), before = opens.stream().filter(e -> e.order < prepared.order).toList();
        List<Event> after = opens.stream().filter(e -> e.order > prepared.order).toList();
        check(before.size() >= 2 && after.size() == (title == null ? 0 : 1)
                        && opens.stream().map(e -> text(e.body(), "requestId")).distinct().count() == opens.size(),
                title == null ? "balanced preparation with no new request before the stopped boundary"
                        : "balanced preparation and one distinct final listener request");
        check(number(prepared.p(), "requestBaseline", before.stream().filter(e -> e.order < pointer.order).count())
                && number(prepared.p(), "responseBaseline", of("opaqueHttpStreamResponseEnd").stream().filter(e -> e.order < pointer.order).count())
                && number(prepared.p(), "requestCount", before.size())
                && number(prepared.p(), "responseCount", of("opaqueHttpStreamResponseEnd").stream().filter(e -> e.order < prepared.order).count())
                && integer(prepared.p(), "requestCount") > integer(prepared.p(), "requestBaseline")
                && number(prepared.p(), "requestCount", integer(prepared.p(), "responseCount")), "prepared counters match actual complete request traffic");
        check(before.stream().anyMatch(e -> pointer.order < e.order && "POST".equals(text(e.body(), "method"))
                && request("opaqueHttpStreamEnd", text(e.body(), "requestId")).stream().anyMatch(end -> integer(end.body(), "bodyBytes") > 0)
                && request("opaqueHttpStreamResponseEnd", text(e.body(), "requestId")).stream().anyMatch(end -> integer(end.body(), "bodyBytes") > 0)),
                "prepare input reaches Java through a nonempty submitted POST and nonempty response");
        for (Event event : events) if (HTTP.contains(event.type())) check(!text(event.body(), "requestId").isBlank()
                && text(event.body(), "requestId").equals(text(event.envelope(), "correlationId"))
                && opens.stream().anyMatch(e -> text(e.body(), "requestId").equals(text(event.body(), "requestId"))), "no orphan or cross-request stream frame");
        for (Event open : opens) {
            String id = text(open.body(), "requestId");
            List<Event> requestEnd = request("opaqueHttpStreamEnd", id), responses = request("opaqueHttpStreamResponse", id), ends = request("opaqueHttpStreamResponseEnd", id);
            boolean complete = open.order < prepared.order;
            check(requestEnd.size() == 1 && responses.size() <= 1 && ends.size() <= 1
                    && (!complete || responses.size() == 1 && ends.size() == 1), "one submitted request and complete prepared response");
            if (requestEnd.size() == 1) body(open, requestEnd.getFirst(), id, false, true);
            if (!responses.isEmpty()) {
                Event response = responses.getFirst();
                check(open.order < response.order && integer(response.body(), "status") >= 200 && integer(response.body(), "status") < 400,
                        "ordered successful response header");
                body(response, ends.isEmpty() ? null : ends.getFirst(), id, true, complete);
            } else check(request("opaqueHttpStreamResponseChunk", id).isEmpty() && ends.isEmpty(), "response body cannot precede response header");
            if (complete) check(events.stream().filter(e -> HTTP.contains(e.type()) && id.equals(text(e.body(), "requestId")))
                    .allMatch(e -> e.order < prepared.order), "prepared response fully drained before visible receipt");
            else check(title != null && finalPointer.order < open.order && open.order < title.order && "POST".equals(text(open.body(), "method"))
                    && requestEnd.size() == 1 && requestEnd.getFirst().order < title.order
                    && integer(requestEnd.getFirst().body(), "bodyBytes") > 0, "nonempty final input submitted before trusted Java title receipt");
        }
        requirePush(pointer);
    }

    private void body(Event start, Event end, String id, boolean response, boolean requireEnd) {
        List<Event> chunks = request(response ? "opaqueHttpStreamResponseChunk" : "opaqueHttpStreamChunk", id);
        List<Event> credits = request(response ? "opaqueHttpStreamResponseCredit" : "opaqueHttpStreamRequestCredit", id);
        check(!requireEnd || end != null, "required stream terminal exists");
        check(credits.stream().allMatch(e -> integer(e.body(), "credit") > 0 && integer(e.body(), "credit") <= Integer.MAX_VALUE
                && start.order < e.order), "positive bounded credits follow stream start");
        long bytes = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Event chunk = chunks.get(i);
            long available = credits.stream().filter(e -> e.order < chunk.order).mapToLong(e -> integer(e.body(), "credit")).sum();
            check(number(chunk.body(), "chunkSequence", i) && integer(chunk.body(), "bodyBytes") > 0
                    && integer(chunk.body(), "bodyBytes") <= Integer.MAX_VALUE && flag(chunk.body(), "bodyRedacted", true)
                    && !chunk.body().has("bodyBase64") && start.order < chunk.order && (end == null || chunk.order < end.order)
                    && available > i, "ordered credited redacted body bytes");
            bytes += Math.max(0, integer(chunk.body(), "bodyBytes"));
        }
        if (end != null) check(start.order < end.order && number(end.body(), "chunkCount", chunks.size())
                && number(end.body(), "bodyBytes", bytes), "end accounts for every exact body chunk and byte");
        if (!response && start.body().has("contentLength")) check(number(start.body(), "contentLength", -1)
                || number(start.body(), "contentLength", bytes), "submitted request matches declared body length");
    }

    private void requirePush(Event pointer) {
        List<Event> pushes = of("opaquePush"), acks = of("opaquePushAck");
        Map<String, List<Event>> groups = new LinkedHashMap<>();
        for (Event push : pushes) groups.computeIfAbsent(pushKey(push), ignored -> new ArrayList<>()).add(push);
        check(!groups.isEmpty() && number(prepared.p(), "pushBaseline", pushes.stream().filter(e -> e.order < pointer.order).map(this::pushKey).distinct().count())
                && number(prepared.p(), "pushMessageCount", groups.size()) && integer(prepared.p(), "pushMessageCount") > integer(prepared.p(), "pushBaseline")
                && pushes.size() == acks.size() && acks.stream().allMatch(e -> groups.containsKey(pushKey(e))), "actual fresh logical push and no orphan ACK");
        Map<String, Long> sequences = new LinkedHashMap<>();
        for (var group : groups.entrySet()) {
            List<Event> attempts = group.getValue(), receipts = acks.stream().filter(e -> group.getKey().equals(pushKey(e))).toList();
            Event first = attempts.getFirst();
            String connection = text(first.body(), "connectionId") + "/" + integer(first.body(), "flowUiId");
            long sequence = integer(first.body(), "pushSequence"), previous = sequences.getOrDefault(connection, 0L);
            check(previous < Long.MAX_VALUE && sequence == previous + 1, "logical sequence starts at one and is contiguous");
            sequences.put(connection, sequence);
            check(first.order < prepared.order && attempts.size() == receipts.size()
                    && receipts.stream().anyMatch(e -> e.order < finalPointer.order
                        && integer(e.body(), "lastSeenServerSyncId") >= integer(first.body(), "flowServerSyncId")),
                    "every logical push is consumed before final close input");
            for (int i = 0; i < Math.min(attempts.size(), receipts.size()); i++) {
                Event push = attempts.get(i), ack = receipts.get(i);
                check(!text(push.body(), "connectionId").isBlank() && integer(push.body(), "flowUiId") >= 0
                        && integer(push.body(), "flowServerSyncId") >= 0 && number(push.body(), "deliveryAttempt", i + 1)
                        && integer(push.body(), "uidlBytes") > 0 && number(push.body(), "uidlBytes", integer(first.body(), "uidlBytes"))
                        && flag(push.body(), "uidlRedacted", true) && !push.body().has("uidl") && flag(push.body(), "resync", false)
                        && integer(ack.body(), "lastSeenServerSyncId") >= -1 && push.order < ack.order
                        && ack.order < finalPointer.order && (i == 0 || receipts.get(i - 1).order < push.order),
                        "sequential same-payload push attempts and ordinal acknowledged consumption");
            }
        }
    }

    private boolean envelope(Event e, boolean send, boolean explicitGeneration) {
        return (send ? e.send() : e.recv()) && "flow-ipc".equals(text(e.p(), "transport"))
                && "java-sidecar".equals(text(e.p(), send ? "target" : "source"))
                && (explicitGeneration || e.p().has("sidecarGeneration") ? number(e.p(), "sidecarGeneration", 1) : true)
                && number(e.envelope(), "version", 1) && e.type().equals(text(e.envelope(), "type")) && scope(e.envelope())
                && e.envelope().path("error").isNull() && e.body().isObject()
                && !text(e.envelope(), "id").isBlank() && !text(e.envelope(), "correlationId").isBlank();
    }
    private boolean identity(JsonNode p) { return scope(p) && runId.equals(text(p, "runId")) && mode.equals(text(p, "mode"))
            && number(p, "processIdentifier", pid) && number(p, "hostProcessIdentifier", hostPid) && number(p, "recoveryGeneration", 0); }
    private static boolean extended(JsonNode p, String... extra) { Set<String> keys = new HashSet<>(COMMON); keys.addAll(List.of(extra)); return p.isObject() && p.propertyNames().equals(keys); }
    private List<Event> request(String type, String id) { return of(type).stream().filter(e -> id.equals(text(e.body(), "requestId"))).toList(); }
    private String pushKey(Event e) { return text(e.body(), "connectionId") + "/" + integer(e.body(), "flowUiId") + "/" + integer(e.body(), "pushSequence") + "/" + integer(e.body(), "flowServerSyncId"); }
    private List<Event> of(String type) { return events.stream().filter(e -> type.equals(e.type())).toList(); }
    private Event one(String type) { List<Event> all = of(type); check(all.size() == 1, "exactly one " + type); return all.size() == 1 ? all.getFirst() : null; }
    private void check(boolean value, String label) { if (!value) missing.add(label); }
    private static boolean ordered(Event... values) { for (int i = 1; i < values.length; i++) if (values[i - 1].order >= values[i].order) return false; return true; }
    private static boolean elapsed(Event from, Event to, long min, long max) { long a = integer(from.root, "at"), b = integer(to.root, "at"); return a > 0 && b >= a && b - a >= min && b - a <= max; }
    private static boolean scope(JsonNode p) { return "main".equals(text(p, "windowId")) && "flow-ipc-session".equals(text(p, "sessionId")) && "main".equals(text(p, "uiId")); }
    private static String text(JsonNode p, String k) { return p.path(k).isString() ? p.path(k).asString() : ""; }
    private static long integer(JsonNode p, String k) { JsonNode n = p.path(k); return n.isIntegralNumber() && n.canConvertToLong() ? n.asLong() : Long.MIN_VALUE; }
    private static boolean number(JsonNode p, String k, long value) { return integer(p, k) == value; }
    private static boolean flag(JsonNode p, String k, boolean value) { return p.path(k).isBoolean() && p.path(k).asBoolean() == value; }
    private static boolean positive(JsonNode p, String k) { return p.path(k).isNumber() && Double.isFinite(p.path(k).asDouble()) && p.path(k).asDouble() > 0; }
    private static boolean scalar(JsonNode p, String k, double value) { return p.path(k).isNumber() && p.path(k).asDouble() == value; }
    private static boolean exact(JsonNode p, String... keys) { return p.isObject() && p.propertyNames().equals(Set.of(keys)); }
    private static boolean textArray(JsonNode p, List<String> values) { if (!p.isArray() || p.size() != values.size()) return false; for (int i = 0; i < values.size(); i++) if (!p.get(i).isString() || !p.get(i).asString().equals(values.get(i))) return false; return true; }
    private static boolean uuid(String value) { try { return java.util.UUID.fromString(value).toString().equals(value); } catch (IllegalArgumentException e) { return false; } }
    private record Event(JsonNode root, int order) {
        String type() { return text(root, "type"); }
        JsonNode p() { return root.path("payload"); }
        JsonNode envelope() { return p().path("envelope"); }
        JsonNode body() { return envelope().path("payload"); }
        boolean recv() { return "recv".equals(text(root, "direction")); }
        boolean send() { return "send".equals(text(root, "direction")); }
    }
}
