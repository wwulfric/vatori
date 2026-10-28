package dev.vatori.desktop.testsupport.trace;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Initial ready failure must retire its owned child before the fatal host exit. */
final class HostReadyTimeoutTraceGate {
    private static final String EXTERNAL = "hostReadyTimeout.externalExitObserved";
    private static final Set<String> ALLOWED = Set.of("launch", "ready", "capabilityManifest",
            "launch.sidecar", "sidecar.started", "sidecar.initialReadyDeadline", "error",
            "host.failureShutdownStarted", "host.nativeResourceDrainStarted", "host.nativeResourceDrainCompleted",
            "shutdown", "sidecar.shutdownTerminated", "sidecar.shutdownEscalated", "sidecar.exit",
            "shutdown.sidecar", "host.sidecarShutdownCompleted", "host.failureShutdownCompleted", EXTERNAL);
    private final List<Event> events = new ArrayList<>();
    private List<String> missing;

    void accept(JsonNode root) { events.add(new Event(root, events.size())); }

    void require(List<String> missing) {
        this.missing = missing;
        check(events.stream().allMatch(e -> ALLOWED.contains(e.type())),
                "no Java ready, window, transport, capability, recovery, replacement or hidden failure events");
        check(events.stream().allMatch(e -> integer(e.root(), "at") > 0), "integer positive lifecycle timestamps");
        Event launch = one("launch"), hostReady = one("ready"), manifest = one("capabilityManifest");
        Event childLaunch = one("launch.sidecar"), start = one("sidecar.started"), deadline = one("sidecar.initialReadyDeadline");
        Event error = one("error"), failure = one("host.failureShutdownStarted");
        Event drain = one("host.nativeResourceDrainStarted"), drained = one("host.nativeResourceDrainCompleted");
        Event shutdown = one("shutdown"), exit = one("sidecar.exit"), done = one("shutdown.sidecar");
        Event completed = one("host.sidecarShutdownCompleted"), fatal = one("host.failureShutdownCompleted"), external = one(EXTERNAL);
        if (launch == null || hostReady == null || manifest == null || childLaunch == null || start == null
                || deadline == null || error == null || failure == null || drain == null || drained == null
                || shutdown == null || exit == null || done == null || completed == null || fatal == null || external == null) return;
        long pid = integer(start.p(), "processIdentifier"), hostPid = integer(failure.p(), "hostProcessIdentifier");
        String mode = text(external.p(), "mode");
        boolean blocked = "blocked".equals(mode);
        check(pid > 0 && pid <= Integer.MAX_VALUE && hostPid > 0 && hostPid <= Integer.MAX_VALUE && pid != hostPid
                        && Set.of("cooperative", "blocked").contains(mode), "distinct owned host/child PIDs and known fixture mode");
        check(launch.recv() && "swift-appkit-wkwebview".equals(text(launch.p(), "host"))
                        && "flow-ipc".equals(text(launch.p(), "transport"))
                        && hostReady.send() && exact(hostReady.p(), "host", "protocolVersion", "transport")
                        && "swift-appkit-wkwebview".equals(text(hostReady.p(), "host")) && number(hostReady.p(), "protocolVersion", 1)
                        && "flow-ipc".equals(text(hostReady.p(), "transport")), "one host ready only, never a Java ready envelope");
        check(manifest.recv() && exact(manifest.p(), "path", "loaded", "permissionMode", "commandsByWindow", "parseError")
                        && "$APP/Contents/Resources/desktop-capabilities.json".equals(text(manifest.p(), "path"))
                        && flag(manifest.p(), "loaded", true) && "manifest-enforced".equals(text(manifest.p(), "permissionMode"))
                        && manifest.p().path("commandsByWindow").isObject() && manifest.p().path("parseError").isString()
                        && text(manifest.p(), "parseError").isEmpty(), "valid packaged capability manifest before child launch");
        check(childLaunch.send() && exact(childLaunch.p(), "generation", "transport", "jar", "command")
                        && number(childLaunch.p(), "generation", 1) && "flow-ipc".equals(text(childLaunch.p(), "transport"))
                        && "$APP/Contents/Resources/Sidecar/application.jar".equals(text(childLaunch.p(), "jar"))
                        && !text(childLaunch.p(), "command").isBlank()
                        && start.recv() && exact(start.p(), "generation", "processIdentifier", "transport")
                        && number(start.p(), "generation", 1) && "flow-ipc".equals(text(start.p(), "transport")),
                "one original generation launched from the staged sidecar path");
        check(deadline.recv() && exact(deadline.p(), "processIdentifier", "timeoutSeconds")
                        && number(deadline.p(), "processIdentifier", pid) && scalar(deadline.p(), "timeoutSeconds", 1)
                        && error.send() && exact(error.p(), "code", "processIdentifier", "timeoutSeconds")
                        && "sidecarInitialReadyTimeout".equals(text(error.p(), "code")) && number(error.p(), "processIdentifier", pid)
                        && scalar(error.p(), "timeoutSeconds", 1) && elapsed(deadline, error, 950, 5000),
                "unique bounded initial-ready timeout belongs to the original live JVM");
        check(failure.recv() && exact(failure.p(), "processIdentifier", "hostProcessIdentifier")
                        && number(failure.p(), "processIdentifier", pid)
                        && drain.recv() && exact(drain.p(), "pendingCount", "timeoutSeconds") && number(drain.p(), "pendingCount", 0)
                        && scalar(drain.p(), "timeoutSeconds", 2) && drained.recv()
                        && exact(drained.p(), "drained", "pendingCount", "failureCount") && flag(drained.p(), "drained", true)
                        && number(drained.p(), "pendingCount", 0) && number(drained.p(), "failureCount", 0),
                "fatal host owns original child and completes empty native cleanup drain");
        check(shutdown.send() && exact(shutdown.p(), "target", "transport", "reason")
                        && "java-sidecar".equals(text(shutdown.p(), "target")) && "flow-ipc".equals(text(shutdown.p(), "transport"))
                        && "host-terminate".equals(text(shutdown.p(), "reason")), "one host shutdown/EOF request");
        check(exit.recv() && exact(exit.p(), "generation", "processIdentifier", "status", "reason", "expected")
                        && number(exit.p(), "generation", 1) && number(exit.p(), "processIdentifier", pid)
                        && flag(exit.p(), "expected", true) && number(exit.p(), "status", blocked ? 9 : 0)
                        && number(exit.p(), "reason", blocked ? 2 : 1)
                        && done.recv() && exact(done.p(), "mode") && (blocked ? "killed" : "graceful").equals(text(done.p(), "mode"))
                        && completed.recv() && exact(completed.p(), "succeeded") && flag(completed.p(), "succeeded", true)
                        && fatal.recv() && exact(fatal.p(), "exitStatus") && number(fatal.p(), "exitStatus", 1),
                "mode-specific actual JVM exit precedes successful retirement and fatal status 1");
        check(external.recv() && exact(external.p(), "schemaVersion", "runId", "mode", "processIdentifier", "hostProcessIdentifier",
                        "hostAbsent", "sidecarAbsent", "markersMatched", "cleanupPerformed")
                        && number(external.p(), "schemaVersion", 1) && uuid(text(external.p(), "runId"))
                        && number(external.p(), "processIdentifier", pid) && number(external.p(), "hostProcessIdentifier", hostPid)
                        && flag(external.p(), "hostAbsent", true) && flag(external.p(), "sidecarAbsent", true)
                        && flag(external.p(), "markersMatched", true) && flag(external.p(), "cleanupPerformed", false)
                        && external.order() == events.size() - 1,
                "final external observation links both absent PIDs and fixture markers before any runner cleanup");
        check(ordered(launch, hostReady, manifest, childLaunch, start, deadline, error, failure,
                        drain, drained, shutdown, exit, done, completed, fatal, external),
                "ordered startup failure, drain, actual child exit, fatal host exit and independent observation");
        if (blocked) {
            Event term = one("sidecar.shutdownTerminated"), kill = one("sidecar.shutdownEscalated");
            if (term == null || kill == null) return;
            for (Event signal : List.of(term, kill)) check(signal.send()
                            && exact(signal.p(), "generation", "processIdentifier", "graceSeconds")
                            && number(signal.p(), "generation", 1) && number(signal.p(), "processIdentifier", pid)
                            && scalar(signal.p(), "graceSeconds", 1), "escalation targets exact original JVM generation");
            check(ordered(shutdown, term, kill, exit) && elapsed(shutdown, term, 950, 5000)
                            && elapsed(term, kill, 950, 5000) && elapsed(kill, exit, 0, 5000),
                    "bounded channel grace then TERM grace then confirmed KILL exit");
        } else check(of("sidecar.shutdownTerminated").isEmpty() && of("sidecar.shutdownEscalated").isEmpty(),
                "cooperative EOF exit uses no TERM or KILL");
    }

    private List<Event> of(String type) { return events.stream().filter(e -> type.equals(e.type())).toList(); }
    private Event one(String type) { List<Event> found = of(type); check(found.size() == 1, "exactly one " + type); return found.size() == 1 ? found.getFirst() : null; }
    private void check(boolean value, String label) { if (!value) missing.add(label); }
    private static boolean ordered(Event... values) { for (int i = 1; i < values.length; i++) if (values[i - 1].order() >= values[i].order()) return false; return true; }
    private static boolean elapsed(Event from, Event to, long min, long max) { long a = integer(from.root(), "at"), b = integer(to.root(), "at"); return a > 0 && b >= a && b - a >= min && b - a <= max; }
    private static String text(JsonNode p, String k) { return p.path(k).isString() ? p.path(k).asString() : ""; }
    private static long integer(JsonNode p, String k) { JsonNode n = p.path(k); return n.isIntegralNumber() && n.canConvertToLong() ? n.asLong() : Long.MIN_VALUE; }
    private static boolean number(JsonNode p, String k, long value) { return integer(p, k) == value; }
    private static boolean flag(JsonNode p, String k, boolean value) { return p.path(k).isBoolean() && p.path(k).asBoolean() == value; }
    private static boolean scalar(JsonNode p, String k, double value) { return p.path(k).isNumber() && p.path(k).asDouble() == value; }
    private static boolean exact(JsonNode p, String... keys) { return p.isObject() && p.propertyNames().equals(Set.of(keys)); }
    private static boolean uuid(String value) { try { return UUID.fromString(value).toString().equals(value); } catch (IllegalArgumentException error) { return false; } }
    private record Event(JsonNode root, int order) {
        String type() { return text(root, "type"); }
        JsonNode p() { return root.path("payload"); }
        boolean recv() { return "recv".equals(text(root, "direction")); }
        boolean send() { return "send".equals(text(root, "direction")); }
    }
}
