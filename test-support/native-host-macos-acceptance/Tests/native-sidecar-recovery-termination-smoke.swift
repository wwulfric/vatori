import Cocoa
import Darwin

#if RETIREMENT_CALLBACK_FAULTS
// Test-only callback delivery substitution; Process and signals remain real.
enum NativeMainRunLoop {
    private static let lock = NSLock()
    private static var holding = false
    private static var held: [() -> Void] = []
    static func hold() { lock.lock(); holding = true; lock.unlock() }
    static var heldCount: Int { lock.lock(); defer { lock.unlock() }; return held.count }
    static func release() {
        lock.lock()
        holding = false
        let work = held
        held.removeAll()
        lock.unlock()
        work.forEach { DispatchQueue.main.async(execute: $0) }
    }
    static func perform(_ work: @escaping () -> Void) {
        lock.lock()
        if holding && !Thread.isMainThread {
            held.append(work)
            lock.unlock()
            return
        }
        lock.unlock()
        DispatchQueue.main.async(execute: work)
    }
    static func perform(after delay: TimeInterval, _ work: @escaping () -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: work)
    }
}
#endif

// Compile the real JavaSidecar and its wire/line readers. Only launch configuration
// and trace collection are replaced; the child is this test executable, not Java
// or a GUI. It ignores SIGTERM before announcing readiness, so escalation is not
// dependent on a signal-handler installation race.
enum SmokeTransport: String { case flowIpc = "flow-ipc" }
struct SmokeWebAppSource { let mode: String }
struct HostConfig {
    let mode: String
    let transport = SmokeTransport.flowIpc
    let flowWebAppSource: SmokeWebAppSource? = nil
    let applicationResourcesURL: URL? = nil
    let sidecarJarURL = URL(fileURLWithPath: CommandLine.arguments[0])
}
struct JavaSidecarLaunchPlan {
    let executable: String
    let arguments: [String]
    let commandSummary = "test-owned-recovery-child"
    static func make(config: HostConfig) -> Self {
        Self(executable: URL(fileURLWithPath: CommandLine.arguments[0]).standardizedFileURL.path,
             arguments: ["--child", config.mode])
    }
}
private struct TraceRecord {
    let direction: String
    let type: String
    let payload: [String: Any]
}
private var recordedTraces: [TraceRecord] = []
func trace(_ direction: String, _ type: String, _ payload: [String: Any]) {
    recordedTraces.append(TraceRecord(direction: direction, type: type, payload: payload))
}
func summarizedJavaStdoutLine(_ line: String) -> String { "test-child-envelope" }

@main
struct NativeSidecarRecoveryTerminationSmoke {
    static func main() {
        if CommandLine.arguments.dropFirst().first == "--child" {
            child(mode: CommandLine.arguments.last ?? "ignore-term")
        }
        do {
#if RETIREMENT_CALLBACK_FAULTS
            for replacement in [false, true] {
                try missingExitCallbackFailsWithoutInventingExit(replacement: replacement)
            }
            print("Native retirement callback-fault smoke passed (real child exits; test-only callback scheduling double)")
#else
            for replacement in [false, true] {
                try ignoresTermAndRepeatedRequestsDoNotExtendGrace(replacement: replacement)
                try exitBeforeDeadlineCannotKillNextGeneration(replacement: replacement)
                try cooperativeRecoveryDoesNotEscalate(replacement: replacement)
                try shutdownFencesAnAlreadyScheduledRecoveryEscalation(replacement: replacement)
            }
            try mixedRequestsRetainFirstDeadline()
            print("Native sidecar recovery/replacement termination smoke passed (real owned child processes; no JVM, controller or GUI evidence)")
#endif
        } catch {
            FileHandle.standardError.write(Data("FAIL: \(error)\n".utf8))
            exit(1)
        }
    }

#if RETIREMENT_CALLBACK_FAULTS
    private static func missingExitCallbackFailsWithoutInventingExit(replacement: Bool) throws {
        recordedTraces.removeAll()
        let kind = replacement ? "replacement" : "recovery"
        let h = Harness(mode: "ignore-term")
        defer { NativeMainRunLoop.release(); h.cleanup() }
        try h.start(generation: 1)
        let pid = try h.pid()
        NativeMainRunLoop.hold()
        try require(retire(h, replacement: replacement, reason: "callback-timeout"), "retirement rejected")
        try require(until(seconds: 3.5) { !h.fatal.isEmpty }, "missing exit callback left retirement waiting forever")
        try require(NativeMainRunLoop.heldCount > 0, "no actual callback was withheld")
        try require(h.fatal == ["sidecar-\(kind)-retirement-exit-confirmation-timeout"], "wrong or duplicate fatal cause")
        try require(h.terminations.isEmpty && !recordedTraces.contains { $0.type == "sidecar.exit" }, "timeout invented exit")
        try require(!h.sidecar.start(), "missing callback allowed a new generation")
        let failures = recordedTraces.filter { $0.type == "sidecar.\(kind)RetirementFailed" }
        try require(failures.count == 1 && failures[0].payload["processIdentifier"] as? Int32 == pid,
                    "failure lost exact process identity")
        var shutdownCompleted = false
        h.sidecar.stopForHostShutdown { result in
            if case .success = result { shutdownCompleted = true }
        }
        try require(!shutdownCompleted, "shutdown invented exit before callback")
        NativeMainRunLoop.release()
        try require(until(seconds: 2) { shutdownCompleted && h.terminations.count == 1 }, "late actual exit did not settle shutdown")
        try require(h.terminations[0].processIdentifier == pid && h.terminations[0].expected,
                    "late exit lost ownership or host-shutdown fencing")
        spin(for: 0.2)
        try require(h.fatal.count == 1, "timeout reported failure twice")
    }
#endif

    private static func child(mode: String) -> Never {
        signal(SIGTERM, mode == "ignore-term" ? SIG_IGN : SIG_DFL)
        print("VFIPC {\"version\":1,\"type\":\"ready\",\"payload\":{},\"error\":null}")
        fflush(stdout)
        while true {
            if let line = readLine() {
                if line.contains("smokeExit") { exit(0) }
            } else {
                // EOF must not accidentally resolve an ignored-SIGTERM test.
                Thread.sleep(forTimeInterval: 0.02)
            }
        }
    }

    private static func ignoresTermAndRepeatedRequestsDoNotExtendGrace(replacement: Bool) throws {
        let prefix = replacement ? "sidecar.replacement" : "sidecar.recovery"
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        defer { h.cleanup() }
        try h.start(generation: 1)
        let pid = try h.pid()
        let started = ProcessInfo.processInfo.systemUptime
        try require(retire(h, replacement: replacement, reason: "ready-timeout"), "recovery request was rejected")
        try require(h.terminations.isEmpty && h.sidecar.processIdentifier == pid,
                    "SIGTERM request was treated as process exit")
        try require(!h.sidecar.start(), "a live retiring child was replaced before actual exit")
        // Continuous duplicates would keep a reset-on-every-call deadline alive.
        while h.terminations.isEmpty && ProcessInfo.processInfo.systemUptime - started < 2.5 {
            spin(for: 0.05)
            if h.sidecar.processIdentifier != nil {
                _ = retire(h, replacement: replacement, reason: "duplicate-must-not-replace-reason")
            }
        }
        try require(h.terminations.count == 1,
                    "SIGTERM-ignoring recovery child did not actually exit within bounded grace; old implementation hangs here")
        let ended = h.terminations[0]
        try require(ended.generation == 1 && ended.processIdentifier == pid
                    && ended.reason == .uncaughtSignal && ended.status == SIGKILL && !ended.expected,
                    "recovery escalation was not an actual exact-generation SIGKILL exit")
        let requests = recordedTraces.filter { $0.type == prefix + "Terminated" }
        let escalated = recordedTraces.filter { $0.type == prefix + "Escalated" }
        try require(requests.count == 1 && escalated.count == 1,
                    "duplicate requests rearmed or repeated the recovery deadline")
        let event = escalated[0]
        try require(event.direction == "send"
                    && Set(event.payload.keys) == Set(["generation", "processIdentifier", "reason", "graceSeconds"])
                    && event.payload["generation"] as? Int == 1
                    && event.payload["processIdentifier"] as? Int32 == pid
                    && event.payload["reason"] as? String == "ready-timeout"
                    && event.payload["graceSeconds"] as? Double == 1,
                    "recovery escalation trace lost captured identity/reason/grace")
        let escalationIndex = recordedTraces.firstIndex { $0.type == prefix + "Escalated" }!
        let exitIndex = recordedTraces.firstIndex { $0.type == "sidecar.exit" }!
        try require(escalationIndex < exitIndex && h.sidecar.processIdentifier == nil,
                    "termination callback was synthesized or delivered before escalation")
        try require(!retire(h, replacement: replacement, reason: "already-exited"), "exited child accepted recovery termination")
        try h.start(generation: 2)
        spin(for: 1.15)
        try require(h.sidecar.isActive(generation: 2) && h.terminations.count == 1,
                    "old recovery deadline killed the fresh generation")
    }

    private static func exitBeforeDeadlineCannotKillNextGeneration(replacement: Bool) throws {
        let prefix = replacement ? "sidecar.replacement" : "sidecar.recovery"
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        defer { h.cleanup() }
        try h.start(generation: 1)
        try require(retire(h, replacement: replacement, reason: "ready-timeout"), "first recovery request rejected")
        try require(h.sidecar.sendSidecarEnvelope("{\"version\":1,\"type\":\"smokeExit\"}"), "controlled child exit not submitted")
        try require(until(seconds: 0.8) { h.terminations.count == 1 }, "controlled child did not exit before grace")
        try require(h.terminations[0].status == 0, "pre-deadline child was forcibly killed")
        try h.start(generation: 2)
        spin(for: 1.2)
        try require(h.sidecar.isActive(generation: 2) && h.terminations.count == 1,
                    "retired process deadline touched replacement generation")
        try require(!recordedTraces.contains { $0.type.hasPrefix(prefix + "Escalat") },
                    "already-exited generation emitted escalation")
    }

    private static func cooperativeRecoveryDoesNotEscalate(replacement: Bool) throws {
        let prefix = replacement ? "sidecar.replacement" : "sidecar.recovery"
        recordedTraces.removeAll()
        let h = Harness(mode: "cooperative")
        defer { h.cleanup() }
        try h.start(generation: 1)
        try require(retire(h, replacement: replacement, reason: "ready-timeout"), "cooperative recovery rejected")
        try require(until(seconds: 0.8) { h.terminations.count == 1 }, "cooperative SIGTERM did not exit")
        spin(for: 1.15)
        try require(h.terminations[0].status == SIGTERM && h.terminations[0].reason == .uncaughtSignal,
                    "cooperative child was not terminated by SIGTERM")
        try require(!recordedTraces.contains { $0.type.hasPrefix(prefix + "Escalat") },
                    "cooperative exit was escalated")
    }

    private static func retire(_ h: Harness, replacement: Bool, reason: String) -> Bool {
        replacement ? h.sidecar.terminateForReplacement(reason: reason)
            : h.sidecar.terminateForRecovery(reason: reason)
    }

    private static func mixedRequestsRetainFirstDeadline() throws {
        for replacement in [false, true] {
            recordedTraces.removeAll()
            let h = Harness(mode: "ignore-term")
            defer { h.cleanup() }
            try h.start(generation: 1)
            let first = replacement ? "sidecar.replacement" : "sidecar.recovery"
            let other = replacement ? "sidecar.recovery" : "sidecar.replacement"
            try require(retire(h, replacement: replacement, reason: "first-owner"), "first retirement rejected")
            let started = ProcessInfo.processInfo.systemUptime
            while h.terminations.isEmpty && ProcessInfo.processInfo.systemUptime - started < 2.5 {
                spin(for: 0.05)
                _ = retire(h, replacement: !replacement, reason: "must-not-rearm")
            }
            try require(h.terminations.count == 1 && h.terminations[0].status == SIGKILL,
                        "mixed retirement requests postponed actual exit")
            let escalations = recordedTraces.filter { $0.type == first + "Escalated" }
            try require(escalations.count == 1 && escalations[0].payload["reason"] as? String == "first-owner"
                        && !recordedTraces.contains { $0.type.hasPrefix(other) },
                        "a second retirement kind replaced the first owner")
        }
    }

    private static func shutdownFencesAnAlreadyScheduledRecoveryEscalation(replacement: Bool) throws {
        let prefix = replacement ? "sidecar.replacement" : "sidecar.recovery"
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        defer { h.cleanup() }
        try h.start(generation: 1)
        try require(retire(h, replacement: replacement, reason: "ready-timeout"), "recovery before shutdown rejected")
        var shutdownCompleted = false
        h.sidecar.stopForHostShutdown { result in
            if case .success = result { shutdownCompleted = true }
        }
        try require(until(seconds: 5) { shutdownCompleted }, "host retirement did not finish")
        try require(!h.sidecar.isActive(generation: 1) && h.terminations.count == 1
                    && h.terminations[0].expected
                    && !recordedTraces.contains { $0.type.hasPrefix(prefix + "Escalat") },
                    "recovery deadline bypassed host shutdown ownership")
    }

    private final class Harness {
        var ready: [Int] = []
        var terminations: [JavaSidecarTermination] = []
        var fatal: [String] = []
        let mode: String
        lazy var sidecar = JavaSidecar(config: HostConfig(mode: mode),
            onSidecarEnvelope: { [weak self] _, _, generation in self?.ready.append(generation) },
            onTermination: { [weak self] termination in self?.terminations.append(termination) },
            onFatalError: { [weak self] cause in self?.fatal.append(cause) })

        init(mode: String) { self.mode = mode }

        func start(generation: Int) throws {
            try require(sidecar.start(), "test-owned child could not start")
            try require(until(seconds: 3) { self.ready.contains(generation) }, "test-owned child handshake missing")
            try require(fatal.isEmpty, "unexpected protocol failure")
        }

        func pid() throws -> Int32 {
            guard let pid = sidecar.processIdentifier, pid > 0 else { throw Failure("no owned live child PID") }
            return pid
        }

        func cleanup() {
            // Only the currently retained, test-created child's exact positive
            // PID is killed; no name, argv, process group or global scan.
            if let pid = sidecar.processIdentifier, pid > 0 {
                _ = Darwin.kill(pid, SIGKILL)
                _ = until(seconds: 2) { self.sidecar.processIdentifier == nil }
            }
            spin(for: 0.05)
        }
    }

    private static func until(seconds: Double, _ condition: () -> Bool) -> Bool {
        let deadline = ProcessInfo.processInfo.systemUptime + seconds
        while !condition() && ProcessInfo.processInfo.systemUptime < deadline {
            _ = RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.01))
        }
        return condition()
    }

    private static func spin(for seconds: Double) { _ = until(seconds: seconds) { false } }

    private struct Failure: Error, CustomStringConvertible {
        let description: String
        init(_ description: String) { self.description = description }
    }

    private static func require(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() { throw Failure(message) }
    }
}
