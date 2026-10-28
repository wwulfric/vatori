package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Real nonblocking-pipe pressure must leave the main loop and product Quit usable. */
final class SidecarWriteBackpressureTraceGate {
    private static final String PREFIX = "flowSidecarWriteBackpressure.";
    private static final Set<String> COMMON = Set.of("windowId", "sessionId", "uiId", "runId",
            "processIdentifier", "hostProcessIdentifier", "recoveryGeneration");
    private static final Set<String> FOCUSED = Set.of("nativePointerDispatch", "prepared", "sidecarSuspended",
            "burstRequested", "browserDispatchCompleted", "mainLoopHeartbeat", "quitDispatched",
            "applicationWillTerminate", "externalExitObserved");
    private static final String PADDING = "w".repeat(16_384);
    private final List<Event> events = new ArrayList<>();
    private List<String> missing;
    private long pid, hostPid;
    private String runId;
    private Event pointer, prepared, suspended, burst, dispatched, heartbeat, quit, terminal, external;

    void accept(JsonNode root) { events.add(new Event(root, events.size())); }

    void require(List<String> missing) {
        this.missing = missing;
        pointer = one(PREFIX + "nativePointerDispatch"); prepared = one(PREFIX + "prepared");
        suspended = one(PREFIX + "sidecarSuspended"); burst = one(PREFIX + "burstRequested");
        dispatched = one(PREFIX + "browserDispatchCompleted"); heartbeat = one(PREFIX + "mainLoopHeartbeat");
        quit = one(PREFIX + "quitDispatched"); terminal = one(PREFIX + "applicationWillTerminate");
        external = one(PREFIX + "externalExitObserved");
        if (List.of(pointer == null, prepared == null, suspended == null, burst == null, dispatched == null,
                heartbeat == null, quit == null, terminal == null, external == null).contains(true)) return;
        pid = integer(prepared.p(), "processIdentifier"); hostPid = integer(prepared.p(), "hostProcessIdentifier");
        runId = text(prepared.p(), "runId");
        check(pid > 0 && pid <= Integer.MAX_VALUE && hostPid > 0 && hostPid <= Integer.MAX_VALUE && pid != hostPid && uuid(runId),
                "exact distinct original host/sidecar identity and fixture run");
        check(events.stream().noneMatch(e -> e.type().startsWith(PREFIX) && !FOCUSED.contains(e.type().substring(PREFIX.length()))),
                "no unknown focused receipt");
        requireFocused();
        requireLifecycle();
        HostShutdownTerminationTraceGate.requirePreparedTraffic(events.stream().map(Event::root).toList(), PREFIX,
                PREFIX + "sidecarSuspended", missing);
        requireBurst();
        requireNoFailure();
    }

    private void requireFocused() {
        for (Event e : List.of(pointer, prepared, suspended, burst, dispatched, heartbeat, quit, terminal, external)) {
            check(e.recv() && identity(e.p()) && integer(e.root(), "at") > 0, "focused receipt retains exact identity/direction/time: " + e.type());
        }
        JsonNode p = pointer.p();
        check(extended(p, "stage", "index", "inputMode", "button", "activeApplication", "keyWindow",
                        "webViewX", "webViewY", "windowX", "windowY", "dispatchedAt")
                        && "prepare".equals(text(p, "stage")) && number(p, "index", 0)
                        && "appkit-window-pointer-event".equals(text(p, "inputMode")) && "primary".equals(text(p, "button"))
                        && flag(p, "activeApplication", true) && flag(p, "keyWindow", true)
                        && List.of("webViewX", "webViewY", "windowX", "windowY", "dispatchedAt").stream().allMatch(k -> positive(p, k)),
                "one active/key generated AppKit prepare click");
        JsonNode state = prepared.p();
        check(extended(state, "documentTimeOriginMillis", "mainWindowNumber", "routeReady", "firstMeaningfulPaint", "flowClientCount",
                        "mainContextRetained", "mainWindowRetained", "mainWebViewRetained", "counter", "pushCount", "client", "trusted",
                        "backgroundSessionAbsent", "requestBaseline", "responseBaseline", "pushBaseline", "requestCount", "responseCount",
                        "pushMessageCount", "nativePointerReceiptCount", "nativePointerTrustedCount", "balancedDrainObserved")
                        && positive(state, "documentTimeOriginMillis") && integer(state, "mainWindowNumber") > 0
                        && List.of("flowClientCount", "counter", "pushCount", "nativePointerReceiptCount", "nativePointerTrustedCount")
                            .stream().allMatch(k -> number(state, k, 1))
                        && List.of("routeReady", "firstMeaningfulPaint", "mainContextRetained", "mainWindowRetained", "mainWebViewRetained",
                            "client", "trusted", "backgroundSessionAbsent", "balancedDrainObserved").stream().allMatch(k -> flag(state, k, true)),
                "visible Java listener/UIDL/push preparation before suspension");
        check(extended(suspended.p(), "signal", "processState", "stopped") && "SIGSTOP".equals(text(suspended.p(), "signal"))
                        && number(suspended.p(), "processState", 4) && flag(suspended.p(), "stopped", true), "exact child observed in OS stopped state");
        check(extended(burst.p(), "requestCount", "headerBytes", "browserProgrammatic") && number(burst.p(), "requestCount", 16)
                        && number(burst.p(), "headerBytes", 16_384) && flag(burst.p(), "browserProgrammatic", true)
                        && extended(dispatched.p(), "requestCount") && number(dispatched.p(), "requestCount", 16),
                "finite browser-programmatic request burst, not native input evidence");
        check(extended(heartbeat.p(), "mainThread", "sidecarStopped", "windowRetained", "webViewRetained", "browserDispatchCompleted")
                        && List.of("mainThread", "sidecarStopped", "windowRetained", "webViewRetained", "browserDispatchCompleted")
                            .stream().allMatch(k -> flag(heartbeat.p(), k, true)) && elapsed(dispatched, heartbeat, 950, 5000),
                "delayed main-run-loop heartbeat while exact child remains stopped");
        check(extended(quit.p(), "source", "action", "keyEquivalent", "commandModifier", "menuMatched", "activeApplication", "keyWindow")
                        && "product-application-menu".equals(text(quit.p(), "source")) && "terminate:".equals(text(quit.p(), "action"))
                        && "q".equals(text(quit.p(), "keyEquivalent"))
                        && List.of("commandModifier", "menuMatched", "activeApplication", "keyWindow").stream().allMatch(k -> flag(quit.p(), k, true)),
                "active product Cmd-Q intent precedes actual AppKit completion");
        check(extended(terminal.p(), "complete", "preparedObserved", "sidecarSuspendedObserved", "burstDispatched", "heartbeatObserved",
                        "quitDispatched", "terminationSource", "currentProcessIdentifier", "originalSidecarAbsent", "originalWindowRetained",
                        "originalWebViewRetained", "startupMarkerMatched") && "applicationWillTerminate".equals(text(terminal.p(), "terminationSource"))
                        && number(terminal.p(), "currentProcessIdentifier", 0)
                        && List.of("complete", "preparedObserved", "sidecarSuspendedObserved", "burstDispatched", "heartbeatObserved", "quitDispatched",
                            "originalSidecarAbsent", "originalWindowRetained", "originalWebViewRetained", "startupMarkerMatched")
                            .stream().allMatch(k -> flag(terminal.p(), k, true)), "AppKit final callback sees absent child and retained original native objects");
        check(extended(external.p(), "schemaVersion", "hostAbsent", "sidecarAbsent", "markersMatched", "cleanupPerformed")
                        && number(external.p(), "schemaVersion", 1) && flag(external.p(), "hostAbsent", true)
                        && flag(external.p(), "sidecarAbsent", true) && flag(external.p(), "markersMatched", true)
                        && flag(external.p(), "cleanupPerformed", false) && external.order() == events.size() - 1,
                "last independent receipt confirms host/JVM absence before cleanup");
        check(ordered(pointer, prepared, suspended, burst, dispatched, heartbeat, quit, terminal, external), "causally ordered focused lifecycle");
    }

    private void requireLifecycle() {
        Event manifest = one("capabilityManifest"), launch = one("launch.sidecar"), start = one("sidecar.started");
        Event create = one("window.create"), loaded = one("webview.loaded");
        List<Event> readies = of("ready").stream().filter(e -> e.recv() || e.p().has("envelope")).toList();
        check(readies.size() == 1, "one actual original Java ready");
        Event drain = one("host.nativeResourceDrainStarted"), drained = one("host.nativeResourceDrainCompleted"), shutdown = one("shutdown");
        Event term = one("sidecar.shutdownTerminated"), kill = one("sidecar.shutdownEscalated"), exit = one("sidecar.exit");
        Event done = one("shutdown.sidecar"), complete = one("host.sidecarShutdownCompleted");
        if (manifest == null || launch == null || start == null || create == null || loaded == null || readies.size() != 1
                || drain == null || drained == null || shutdown == null || term == null || kill == null || exit == null || done == null || complete == null) return;
        Event ready = readies.getFirst(); JsonNode m = manifest.p();
        check(manifest.recv() && flag(m, "loaded", true) && "manifest-enforced".equals(text(m, "permissionMode"))
                        && "$APP/Contents/Resources/desktop-capabilities.json".equals(text(m, "path"))
                        && m.path("parseError").isString() && text(m, "parseError").isEmpty()
                        && launch.send() && number(launch.p(), "generation", 1) && "flow-ipc".equals(text(launch.p(), "transport"))
                        && "$APP/Contents/Resources/Sidecar/application.jar".equals(text(launch.p(), "jar"))
                        && start.recv() && number(start.p(), "generation", 1) && number(start.p(), "processIdentifier", pid)
                        && "flow-ipc".equals(text(start.p(), "transport")) && envelope(ready, false)
                        && "standard-vaadin-bundle-opaque-ipc".equals(text(ready.body(), "runtime"))
                        && "flow-ipc".equals(text(ready.body(), "transport")) && "standard-flow-client".equals(text(ready.body(), "uidlConsumer"))
                        && "opaque-http-stream-ipc".equals(text(ready.body(), "browserRequestTransport"))
                        && create.send() && scope(create.p()) && "main".equals(text(create.p(), "label")) && "flow-ipc".equals(text(create.p(), "mode"))
                        && loaded.recv() && scope(loaded.p()) && number(loaded.p(), "recoveryGeneration", 0)
                        && ordered(manifest, launch, start, ready, create, loaded, pointer), "one standard-bundle JVM and original loaded main window");
        check(drain.recv() && exact(drain.p(), "pendingCount", "timeoutSeconds") && number(drain.p(), "pendingCount", 0)
                        && scalar(drain.p(), "timeoutSeconds", 2) && drained.recv() && exact(drained.p(), "drained", "pendingCount", "failureCount")
                        && flag(drained.p(), "drained", true) && number(drained.p(), "pendingCount", 0) && number(drained.p(), "failureCount", 0)
                        && shutdown.send() && exact(shutdown.p(), "target", "transport", "reason")
                        && "java-sidecar".equals(text(shutdown.p(), "target")) && "flow-ipc".equals(text(shutdown.p(), "transport"))
                        && "host-terminate".equals(text(shutdown.p(), "reason")), "product Quit drains before nonblocking shutdown");
        for (Event signal : List.of(term, kill)) check(signal.send() && exact(signal.p(), "generation", "processIdentifier", "graceSeconds")
                        && number(signal.p(), "generation", 1) && number(signal.p(), "processIdentifier", pid) && scalar(signal.p(), "graceSeconds", 1),
                "TERM/KILL targets only original stopped JVM");
        check(exit.recv() && exact(exit.p(), "generation", "processIdentifier", "status", "reason", "expected")
                        && number(exit.p(), "generation", 1) && number(exit.p(), "processIdentifier", pid) && number(exit.p(), "status", 9)
                        && number(exit.p(), "reason", 2) && flag(exit.p(), "expected", true)
                        && done.recv() && exact(done.p(), "mode") && "killed".equals(text(done.p(), "mode"))
                        && complete.recv() && exact(complete.p(), "succeeded") && flag(complete.p(), "succeeded", true)
                        && ordered(quit, drain, drained, shutdown, term, kill, exit, done, complete, terminal, external)
                        && elapsed(shutdown, term, 950, 5000) && elapsed(term, kill, 950, 5000) && elapsed(kill, exit, 0, 5000),
                "bounded actual killed-child exit precedes AppKit and independent host exit");
    }

    private void requireBurst() {
        List<Event> pressure = of("sidecar.writeBackpressure");
        check(pressure.stream().anyMatch(e -> burst.order() < e.order() && e.order() < heartbeat.order()),
                "real EAGAIN after stopped-child burst and before responsive main-loop heartbeat");
        for (Event e : pressure) check(e.recv() && exact(e.p(), "generation", "processIdentifier", "pendingBytes", "pendingFrames", "partialFrameBytes")
                        && number(e.p(), "generation", 1) && number(e.p(), "processIdentifier", pid)
                        && integer(e.p(), "pendingBytes") > 0 && integer(e.p(), "pendingBytes") <= 16 * 1024 * 1024
                        && integer(e.p(), "pendingFrames") > 0 && integer(e.p(), "pendingFrames") <= 256
                        && integer(e.p(), "partialFrameBytes") >= 0 && integer(e.p(), "partialFrameBytes") <= integer(e.p(), "pendingBytes")
                        && e.order() < quit.order(), "actual backpressure diagnostic belongs to bounded original writer");
        List<Event> opens = of("opaqueHttpStreamOpen").stream().filter(e -> e.order() > suspended.order()).toList();
        Set<String> allIds = new HashSet<>();
        check(of("opaqueHttpStreamOpen").stream().allMatch(e -> allIds.add(text(e.body(), "requestId"))), "unique preparation/burst request IDs");
        check(opens.size() == 16, "exact sixteen ordinary opaque request opens after SIGSTOP");
        Set<String> urls = new HashSet<>();
        for (Event open : opens) {
            String id = text(open.body(), "requestId"), url = text(open.body(), "url");
            List<Event> ends = of("opaqueHttpStreamEnd").stream().filter(e -> id.equals(text(e.body(), "requestId"))).toList();
            check(envelope(open, true) && !id.isBlank() && id.equals(text(open.envelope(), "correlationId"))
                            && "GET".equals(text(open.body(), "method")) && number(open.body(), "contentLength", 0)
                            && probeUrl(url) && urls.add(url) && paddedHeader(open.body().path("headers"))
                            && burst.order() < open.order() && open.order() < heartbeat.order(),
                    "ordinary scoped GET carries unique finite probe and exact padded header");
            check(ends.size() == 1, "one empty end for every bounded probe");
            if (ends.size() == 1) { Event end = ends.getFirst(); check(envelope(end, true)
                            && id.equals(text(end.envelope(), "correlationId")) && number(end.body(), "chunkCount", 0)
                            && number(end.body(), "bodyBytes", 0) && open.order() < end.order() && end.order() < heartbeat.order(),
                    "empty submitted GET ends before heartbeat despite stopped JVM"); }
        }
        check(pressure.stream().anyMatch(e -> burst.order() < e.order() && e.order() < heartbeat.order()
                        && opens.stream().anyMatch(open -> open.order() < e.order())), "physical pipe pressure follows an actual probe open");
        for (Event e : events) if (e.order() > suspended.order() && e.type().startsWith("opaque")) {
            check(Set.of("opaqueHttpStreamOpen", "opaqueHttpStreamEnd").contains(e.type())
                            && opens.stream().anyMatch(open -> text(open.body(), "requestId").equals(text(e.body(), "requestId"))),
                    "stopped JVM produces no response/push and no orphan or extra transport traffic");
        }
    }

    private void requireNoFailure() {
        requireFinalTeardown();
        for (Event e : events) {
            check(!Set.of("error", "webview.error", "sidecar.startFailed", "sidecar.writeFailed", "sidecar.shutdownFailed", "host.sidecarShutdownFailed",
                            "command", "capabilityResult", "windowClosed", "window.closeDropped", "opaqueHttpStreamError", "opaqueHttpStreamCancel", "opaquePushError",
                            "opaquePushDisconnect", "opaquePushResync", "staleCapabilityResultDropped").contains(e.type())
                            && !e.type().startsWith("sidecar.recovery") && !e.type().startsWith("sidecar.replacement")
                            && !e.type().startsWith("host.failureShutdown"), "no write failure, fatal exit, new owner or unrelated native/transport workflow");
            if (e.type().equals("flow-ipc.sidecarWrite") && "written".equals(text(e.p(), "status")))
                check(e.send(), "actual written receipt has the outbound direction");
        }
    }

    private void requireFinalTeardown() {
        List<Event> closes = of("window.closeRequested"), drops = of("windowClosedDropped");
        List<Event> rejected = of("flow-ipc.sidecarWrite").stream().filter(e -> !"written".equals(text(e.p(), "status"))).toList();
        if (closes.isEmpty() && drops.isEmpty() && rejected.isEmpty()) {
            check(terminal.order() + 1 == external.order(), "external observation immediately follows AppKit completion without teardown");
            return;
        }
        check(closes.size() == 1 && rejected.size() == 1 && drops.size() == 1,
                "zero or one complete final AppKit window-close/rejected-write/drop triple");
        if (closes.size() != 1 || rejected.size() != 1 || drops.size() != 1) return;
        Event close = closes.getFirst(), write = rejected.getFirst(), drop = drops.getFirst();
        check(close.recv() && exact(close.p(), "label", "windowId", "sessionId", "uiId") && scope(close.p())
                        && "main".equals(text(close.p(), "label"))
                        && write.send() && exact(write.p(), "target", "status", "hostShutdownRequested", "initialReadyPending",
                            "recoveryInProgress", "replacementInProgress")
                        && "java-sidecar".equals(text(write.p(), "target")) && "lifecycle-unavailable".equals(text(write.p(), "status"))
                        && flag(write.p(), "hostShutdownRequested", true) && flag(write.p(), "initialReadyPending", false)
                        && flag(write.p(), "recoveryInProgress", false) && flag(write.p(), "replacementInProgress", false)
                        && drop.recv() && exact(drop.p(), "reason", "windowId", "sessionId", "uiId") && scope(drop.p())
                        && "sidecar-lifecycle-unavailable".equals(text(drop.p(), "reason")),
                "final close is rejected only by the already-fenced host shutdown and dropped for its exact scope");
        check(terminal.order() + 1 == close.order() && close.order() + 1 == write.order()
                        && write.order() + 1 == drop.order() && drop.order() + 1 == external.order(),
                "one contiguous teardown triple after AppKit completion and immediately before external absence");
    }

    private boolean identity(JsonNode p) { return scope(p) && runId.equals(text(p, "runId")) && number(p, "processIdentifier", pid)
            && number(p, "hostProcessIdentifier", hostPid) && number(p, "recoveryGeneration", 0); }
    private static boolean extended(JsonNode p, String... extra) { Set<String> keys = new HashSet<>(COMMON); keys.addAll(List.of(extra)); return p.isObject() && p.propertyNames().equals(keys); }
    private static boolean scope(JsonNode p) { return "main".equals(text(p, "windowId")) && "flow-ipc-session".equals(text(p, "sessionId")) && "main".equals(text(p, "uiId")); }
    private static boolean envelope(Event e, boolean send) { return (send ? e.send() : e.recv()) && "flow-ipc".equals(text(e.p(), "transport"))
            && "java-sidecar".equals(text(e.p(), send ? "target" : "source"))
            && (send && !e.p().has("sidecarGeneration") || number(e.p(), "sidecarGeneration", 1))
            && number(e.envelope(), "version", 1) && e.type().equals(text(e.envelope(), "type")) && scope(e.envelope())
            && e.envelope().path("error").isNull() && e.body().isObject()
            && !text(e.envelope(), "id").isBlank() && !text(e.envelope(), "correlationId").isBlank(); }
    private static boolean probeUrl(String url) { for (int i = 0; i < 16; i++) if (url.equals("https://app.vatori.invalid/__vatori_acceptance/write-backpressure?probe=" + i)) return true; return false; }
    private static boolean paddedHeader(JsonNode headers) { if (!headers.isObject()) return false; List<String> names = headers.propertyNames().stream().filter(k -> k.equalsIgnoreCase("X-Vatori-Backpressure")).toList(); return names.size() == 1 && PADDING.equals(text(headers, names.getFirst())); }
    private List<Event> of(String type) { return events.stream().filter(e -> type.equals(e.type())).toList(); }
    private Event one(String type) { List<Event> all = of(type); check(all.size() == 1, "exactly one " + type); return all.size() == 1 ? all.getFirst() : null; }
    private void check(boolean value, String label) { if (!value) missing.add(label); }
    private static boolean ordered(Event... values) { for (int i = 1; i < values.length; i++) if (values[i - 1].order() >= values[i].order()) return false; return true; }
    private static boolean elapsed(Event from, Event to, long min, long max) { long a = integer(from.root(), "at"), b = integer(to.root(), "at"); return a > 0 && b >= a && b - a >= min && b - a <= max; }
    private static String text(JsonNode p, String k) { return p.path(k).isString() ? p.path(k).asString() : ""; }
    private static long integer(JsonNode p, String k) { JsonNode n = p.path(k); return n.isIntegralNumber() && n.canConvertToLong() ? n.asLong() : Long.MIN_VALUE; }
    private static boolean number(JsonNode p, String k, long value) { return integer(p, k) == value; }
    private static boolean flag(JsonNode p, String k, boolean value) { return p.path(k).isBoolean() && p.path(k).asBoolean() == value; }
    private static boolean positive(JsonNode p, String k) { return p.path(k).isNumber() && Double.isFinite(p.path(k).asDouble()) && p.path(k).asDouble() > 0; }
    private static boolean scalar(JsonNode p, String k, double value) { return p.path(k).isNumber() && p.path(k).asDouble() == value; }
    private static boolean exact(JsonNode p, String... keys) { return p.isObject() && p.propertyNames().equals(Set.of(keys)); }
    private static boolean uuid(String value) { try { return UUID.fromString(value).toString().equals(value); } catch (IllegalArgumentException e) { return false; } }
    private record Event(JsonNode root, int order) {
        String type() { return text(root, "type"); } JsonNode p() { return root.path("payload"); }
        JsonNode envelope() { return p().path("envelope"); } JsonNode body() { return envelope().path("payload"); }
        boolean recv() { return "recv".equals(text(root, "direction")); } boolean send() { return "send".equals(text(root, "direction")); }
    }
}
