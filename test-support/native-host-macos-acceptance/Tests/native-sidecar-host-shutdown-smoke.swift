import Cocoa
import Darwin

#if HOST_SHUTDOWN_CALLBACK_FAULTS
// This separate test binary compiles the real JavaSidecar but substitutes only
// callback delivery. The child really exits; withholding its callback proves
// that isRunning == false and a successful SIGKILL are not synthetic success.
enum NativeMainRunLoop {
    private static let lock = NSLock()
    private static var holding = false
    private static var holdOne = false
    private static var held: [() -> Void] = []
    static func holdActualExitCallback() { lock.lock(); holding = true; lock.unlock() }
    static func holdNextBackgroundWork() { lock.lock(); holdOne = true; lock.unlock() }
    static var heldCount: Int { lock.lock(); defer { lock.unlock() }; return held.count }
    static func releaseHeld() {
        lock.lock()
        holding = false
        holdOne = false
        let work = held
        held.removeAll()
        lock.unlock()
        work.forEach(submit)
    }
    static func releaseHeldInlineOnMain() {
        precondition(Thread.isMainThread)
        lock.lock()
        holding = false
        holdOne = false
        let work = held
        held.removeAll()
        lock.unlock()
        work.forEach { $0() }
    }
    static func perform(_ work: @escaping () -> Void) {
        lock.lock()
        if (holding || holdOne) && !Thread.isMainThread {
            holdOne = false
            held.append(work)
            lock.unlock()
            return
        }
        lock.unlock()
        submit(work)
    }
    static func perform(after delay: TimeInterval, _ work: @escaping () -> Void) {
        DispatchQueue.global().asyncAfter(deadline: .now() + delay) { submit(work) }
    }
    private static func submit(_ work: @escaping () -> Void) {
        let runLoop = CFRunLoopGetMain()
        let modes = [RunLoop.Mode.default.rawValue, RunLoop.Mode.modalPanel.rawValue] as CFArray
        CFRunLoopPerformBlock(runLoop, modes, work)
        CFRunLoopWakeUp(runLoop)
    }
}
#endif

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
    let currentDirectoryURL = URL(fileURLWithPath: CommandLine.arguments[0]).deletingLastPathComponent()
    let environment = ProcessInfo.processInfo.environment
    let executable: String
    let arguments: [String]
    let commandSummary = "test-owned-host-shutdown-child"
    let jarDisplayPath = "$TEST/owned-child"
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
struct NativeSidecarHostShutdownSmoke {
    static func main() {
        if CommandLine.arguments.dropFirst().first == "--child" {
            child(mode: CommandLine.arguments.last ?? "ignore-term")
        }
        do {
#if HOST_SHUTDOWN_CALLBACK_FAULTS
            try callbackTimeoutThenRetry()
            try actualExitAfterFailureDoesNotResettleFailedAttempt()
            try staleWriterFailureCannotAffectNewGeneration()
            try observedExitWinsOverPendingWriterFailure()
            print("Native sidecar host shutdown callback-fault smoke passed (real child exits; test-only callback scheduling double)")
#else
            try noProcessCompletesAsynchronously()
            try gracefulChannelExit()
            try cooperativeTermExit()
            try closedInputDoesNotCrashHost()
            try stalledOpaqueWriteDoesNotBlockRetirement()
            try closedInputFailsOpaqueChannelWithoutSIGPIPE()
            try ignoredTermEscalatesWithoutExtendingDuplicateDeadline()
            try modalLoopInsideMainQueueCanComplete()
            try recoveryDeadlineCannotBypassHostShutdown()
            print("Native sidecar host shutdown smoke passed (real exact-owned child exits and default/modal run loops; no JVM, controller or GUI evidence)")
#endif
        } catch {
            FileHandle.standardError.write(Data("FAIL: \(error)\n".utf8))
            exit(1)
        }
    }

    private static func child(mode: String) -> Never {
        signal(SIGTERM, ["cooperative-term", "closed-input"].contains(mode) ? SIG_DFL : SIG_IGN)
        if mode == "closed-input" { Darwin.close(STDIN_FILENO) }
        let childDeadline = ProcessInfo.processInfo.systemUptime + (mode == "no-read" ? 8 : 3)
        print("VFIPC {\"version\":1,\"type\":\"ready\",\"payload\":{},\"error\":null}")
        fflush(stdout)
        while true {
            // Even a regression that kills the test host with SIGPIPE cannot
            // leave this deliberately closed-input fixture alive indefinitely.
            if mode == "closed-input", ProcessInfo.processInfo.systemUptime >= childDeadline { exit(0) }
            if mode == "no-read", ProcessInfo.processInfo.systemUptime >= childDeadline { exit(0) }
            if mode == "no-read" { Thread.sleep(forTimeInterval: 0.02); continue }
            if let line = readLine() {
                if mode == "graceful", line.contains("\"shutdown\"") { exit(0) }
            } else if mode == "graceful" {
                exit(0)
            } else {
                Thread.sleep(forTimeInterval: 0.02)
            }
        }
    }

    private static func noProcessCompletesAsynchronously() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "graceful")
        h.stop()
        try require(h.results.isEmpty, "no-process completion was inline before terminateLater returned")
        try require(until(seconds: 1) { h.results.count == 1 }, "no-process completion missing")
        try require(h.successes == 1 && !h.sidecar.start(), "no-process shutdown did not fence future starts")
        try require(!recordedTraces.contains { $0.type == "sidecar.exit" }, "no-process shutdown fabricated an exit")
    }

    private static func gracefulChannelExit() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "graceful")
        defer { h.cleanup() }
        try h.start()
        let pid = try h.pid()
        h.stop()
        try require(h.results.isEmpty, "shutdown completed without actual exit callback")
        try require(until(seconds: 0.8) { h.results.count == 1 }, "cooperative channel/EOF did not exit before TERM grace")
        try assertExit(h, pid: pid, status: 0, mode: "graceful")
        spin(seconds: 2.2)
        try require(!recordedTraces.contains { $0.type == "sidecar.shutdownTerminated" || $0.type == "sidecar.shutdownEscalated" },
                    "graceful completion left a live escalation deadline")
        h.stop()
        try require(h.results.count == 1, "completed-shutdown retry was delivered inline")
        try require(until(seconds: 0.5) { h.results.count == 2 } && h.successes == 2,
                    "already-exited child did not permit a deferred successful retry")
        try require(recordedTraces.filter { $0.type == "shutdown" }.count == 1,
                    "completed retry resent shutdown")
    }

    private static func cooperativeTermExit() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "cooperative-term")
        defer { h.cleanup() }
        try h.start()
        let pid = try h.pid()
        h.stop()
        try require(until(seconds: 2) { h.results.count == 1 }, "cooperative TERM child did not exit")
        try assertExit(h, pid: pid, status: SIGTERM, mode: "terminated")
        spin(seconds: 1.2)
        try require(recordedTraces.filter { $0.type == "sidecar.shutdownTerminated" }.count == 1
                    && !recordedTraces.contains { $0.type == "sidecar.shutdownEscalated" },
                    "cooperative TERM was skipped or unnecessarily escalated")
    }

    private static func stalledOpaqueWriteDoesNotBlockRetirement() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "no-read")
        defer { h.cleanup() }
        try h.start()
        let pid = try h.pid()
        let started = ProcessInfo.processInfo.systemUptime
        try require(h.sidecar.sendSidecarEnvelope(String(repeating: "x", count: 512 * 1024)),
                    "bounded opaque frame was not admitted")
        try require(ProcessInfo.processInfo.systemUptime - started < 0.2,
                    "opaque admission blocked the caller on a full pipe")
        try require(until(seconds: 1) {
            recordedTraces.contains { $0.type == "sidecar.writeBackpressure"
                && $0.payload["processIdentifier"] as? Int32 == pid }
        }, "never-reading owned child did not produce actual EAGAIN evidence")
        let quitStarted = ProcessInfo.processInfo.systemUptime
        h.stop()
        try require(ProcessInfo.processInfo.systemUptime - quitStarted < 0.2,
                    "full pipe blocked host shutdown admission")
        try require(until(seconds: 4.5) { h.results.count == 1 },
                    "backpressured child was not retired before its fixture safety deadline")
        try assertExit(h, pid: pid, status: SIGKILL, mode: "killed")
        try require(!recordedTraces.contains { $0.type == "sidecar.writeFailed" }
                    && !recordedTraces.contains { $0.type == "flow-ipc.sidecarWrite"
                        && $0.payload["status"] as? String == "written" },
                    "partial frame was reported delivered or normal Quit became a transport failure")
    }

    private static func closedInputFailsOpaqueChannelWithoutSIGPIPE() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "closed-input")
        defer { h.cleanup() }
        try h.start()
        try require(h.sidecar.sendSidecarEnvelope("{}"), "first closed-pipe frame was not admitted")
        try require(until(seconds: 1) { h.fatals.count == 1 }, "closed pipe did not fail the current transport")
        try require(h.fatals[0].hasPrefix("sidecar-write-")
                    && !h.sidecar.sendSidecarEnvelope("{}")
                    && recordedTraces.filter { $0.type == "sidecar.writeFailed" }.count == 1,
                    "failed writer accepted more frames or did not fail exactly once")
        h.stop()
        try require(until(seconds: 2.5) { h.results.count == 1 }, "failed writer blocked retirement")
    }

    private static func ignoredTermEscalatesWithoutExtendingDuplicateDeadline() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        let unrelated = Harness(mode: "ignore-term")
        defer { h.cleanup(); unrelated.cleanup() }
        try h.start()
        try unrelated.start()
        let pid = try h.pid()
        let unrelatedPID = try unrelated.pid()
        let started = ProcessInfo.processInfo.systemUptime
        h.stop()
        try require(ProcessInfo.processInfo.systemUptime - started < 0.2,
                    "normal shutdown blocked the caller instead of deferring work")
        try require(!h.sidecar.start(), "shutdown allowed a new generation")
        try require(!h.sidecar.sendSidecarEnvelope("{}"), "detached shutdown stdin accepted new work")
        var submitted = 1
        while h.results.isEmpty && ProcessInfo.processInfo.systemUptime - started < 3.3 {
            spin(seconds: 0.05)
            if h.results.isEmpty { h.stop(); submitted += 1 }
        }
        try require(h.results.count == submitted && h.successes == submitted,
                    "duplicates delayed the deadline or were not each settled exactly once")
        try assertExit(h, pid: pid, status: SIGKILL, mode: "killed")
        try require(unrelated.sidecar.processIdentifier == unrelatedPID && unrelated.terminations.isEmpty,
                    "shutdown touched an unrelated exact-owned child")
        for type in ["shutdown", "sidecar.shutdownTerminated", "sidecar.shutdownEscalated"] {
            try require(recordedTraces.filter { $0.type == type }.count == 1,
                        "duplicate shutdown repeated \(type)")
        }
        for event in recordedTraces.filter({ $0.type == "sidecar.shutdownTerminated" || $0.type == "sidecar.shutdownEscalated" }) {
            try require(event.direction == "send" && Set(event.payload.keys) == Set(["generation", "processIdentifier", "graceSeconds"])
                        && event.payload["generation"] as? Int == 1
                        && event.payload["processIdentifier"] as? Int32 == pid
                        && event.payload["graceSeconds"] as? Double == 1,
                        "shutdown signal trace lost precise identity/grace")
        }
    }

    private static func closedInputDoesNotCrashHost() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "closed-input")
        defer { h.cleanup() }
        try h.start()
        let pid = try h.pid()
        h.stop()
        try require(until(seconds: 2.5) { h.results.count == 1 },
                    "closed-input EPIPE prevented bounded child shutdown")
        try assertExit(h, pid: pid, status: SIGTERM, mode: "terminated")
    }

    private static func modalLoopInsideMainQueueCanComplete() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "no-read")
        defer { h.cleanup() }
        try h.start()
        let pid = try h.pid()
        var returned = false
        var completedInModalLoop = false
        DispatchQueue.main.async {
            h.stop()
            // Reproduce a last-window close inside a main-dispatch callback.
            // Neither main-queue timers nor termination delivery can reenter it.
            completedInModalLoop = until(seconds: 4.8, mode: .modalPanel) { h.results.count == 1 }
            returned = true
        }
        try require(until(seconds: 5.5) { returned } && completedInModalLoop,
                    "modal termination loop starved shutdown deadlines or actual exit delivery")
        try assertExit(h, pid: pid, status: SIGKILL, mode: "killed")
    }

    private static func recoveryDeadlineCannotBypassHostShutdown() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        defer { h.cleanup() }
        try h.start()
        let pid = try h.pid()
        try require(h.sidecar.terminateForRecovery(reason: "ready-timeout"), "recovery setup failed")
        h.stop()
        try require(until(seconds: 3.5) { h.results.count == 1 }, "host shutdown failed after recovery retirement")
        try assertExit(h, pid: pid, status: SIGKILL, mode: "killed")
        try require(!recordedTraces.contains { $0.type.hasPrefix("sidecar.recoveryEscalat") },
                    "recovery deadline bypassed host shutdown ownership")
    }

#if HOST_SHUTDOWN_CALLBACK_FAULTS
    private static func observedExitWinsOverPendingWriterFailure() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "closed-input")
        defer { NativeMainRunLoop.releaseHeld(); h.cleanup() }
        try h.start()
        let pid = try h.pid()
        NativeMainRunLoop.holdNextBackgroundWork()
        try require(h.sidecar.sendSidecarEnvelope("{}"), "frame was not admitted before exit")
        try require(until(seconds: 1) { NativeMainRunLoop.heldCount == 1 }, "writer failure was not held")
        try require(Darwin.kill(pid, SIGKILL) == 0, "could not stop the exact test child")
        // Let Foundation observe exit without processing the queued host callback.
        let deadline = ProcessInfo.processInfo.systemUptime + 1
        while h.sidecar.processIdentifier != nil && ProcessInfo.processInfo.systemUptime < deadline {
            Thread.sleep(forTimeInterval: 0.01)
        }
        try require(h.sidecar.processIdentifier == nil && h.terminations.isEmpty,
                    "test did not isolate observed exit from actual callback delivery")
        NativeMainRunLoop.releaseHeldInlineOnMain()
        try require(h.fatals.isEmpty, "late EPIPE stole an already-observed unexpected exit")
        try require(until(seconds: 1) { h.terminations.count == 1 }, "actual exit callback missing")
        try require(!h.terminations[0].expected, "transport failure converted unexpected exit into host shutdown")
    }

    private static func staleWriterFailureCannotAffectNewGeneration() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "closed-input")
        defer { NativeMainRunLoop.releaseHeld(); h.cleanup() }
        try h.start()
        let oldPID = try h.pid()
        NativeMainRunLoop.holdNextBackgroundWork()
        try require(h.sidecar.sendSidecarEnvelope("{}"), "old-generation frame was not admitted")
        try require(until(seconds: 1) { NativeMainRunLoop.heldCount == 1 }, "old writer failure callback was not captured")
        try require(Darwin.kill(oldPID, SIGKILL) == 0, "could not retire the exact test-owned old child")
        try require(until(seconds: 2) { h.terminations.count == 1 }, "old actual exit was not observed")
        h.ready = false
        try h.start()
        let newPID = try h.pid()
        try require(oldPID != newPID && h.sidecar.isActive(generation: 2), "new generation was not distinct")
        NativeMainRunLoop.releaseHeld()
        spin(seconds: 0.15)
        try require(h.fatals.isEmpty && h.sidecar.processIdentifier == newPID
                    && !recordedTraces.contains { $0.type == "sidecar.writeFailed" },
                    "late old-writer failure poisoned the replacement generation")
    }

    private static func callbackTimeoutThenRetry() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        defer { NativeMainRunLoop.releaseHeld(); h.cleanup() }
        try h.start()
        let pid = try h.pid()
        NativeMainRunLoop.holdActualExitCallback()
        h.stop()
        try require(until(seconds: 5) { h.results.count == 1 }, "missing bounded actual-exit callback timeout")
        try assertExitTimeout(h, pid: pid)
        h.stop()
        spin(seconds: 0.15)
        try require(h.results.count == 1 && h.terminations.isEmpty,
                    "retry synthesized success from exited Process before actual callback")
        NativeMainRunLoop.releaseHeld()
        try require(until(seconds: 1) { h.results.count == 2 }, "actual callback did not settle the retry")
        try assertExit(h, pid: pid, status: SIGKILL, mode: "killed")
        spin(seconds: 2.2)
        try require(h.results.count == 2 && h.successes == 1,
                    "old failed-attempt deadline resettled the retry")
        try require(recordedTraces.filter { $0.type == "shutdown" }.count == 1
                    && recordedTraces.filter { $0.type == "sidecar.shutdownFailed" }.count == 1,
                    "retry resent shutdown or repeated failure")
    }

    private static func actualExitAfterFailureDoesNotResettleFailedAttempt() throws {
        recordedTraces.removeAll()
        let h = Harness(mode: "ignore-term")
        defer { NativeMainRunLoop.releaseHeld(); h.cleanup() }
        try h.start()
        let pid = try h.pid()
        NativeMainRunLoop.holdActualExitCallback()
        h.stop()
        try require(until(seconds: 5) { h.results.count == 1 }, "missing late-callback timeout")
        try assertExitTimeout(h, pid: pid)
        NativeMainRunLoop.releaseHeld()
        try require(until(seconds: 1) { h.terminations.count == 1 }, "late actual exit not observed")
        try require(h.results.count == 1 && h.successes == 0,
                    "late actual exit resettled a canceled Quit completion")
        h.stop()
        try require(h.results.count == 1, "late-exit retry did not defer completion")
        try require(until(seconds: 1) { h.results.count == 2 }, "late-exit retry failed")
        try assertExit(h, pid: pid, status: SIGKILL, mode: "killed")
    }

    private static func assertExitTimeout(_ h: Harness, pid: Int32) throws {
        guard case .failure(let failure) = h.results.first else { throw Failure("timeout reported success") }
        try require(failure.phase == "exit" && failure.cause == "exit-timeout" && failure.signalErrno == nil,
                    "timeout produced incorrect typed failure")
        try require(h.terminations.isEmpty && h.sidecar.processIdentifier == nil
                    && !recordedTraces.contains { $0.type == "sidecar.exit" || $0.type == "shutdown.sidecar" },
                    "signal success or isRunning observation fabricated actual exit")
        let failures = recordedTraces.filter { $0.type == "sidecar.shutdownFailed" }
        try require(failures.count == 1 && failures[0].direction == "send"
                    && Set(failures[0].payload.keys) == Set(["generation", "processIdentifier", "phase", "cause"])
                    && failures[0].payload["generation"] as? Int == 1
                    && failures[0].payload["processIdentifier"] as? Int32 == pid
                    && failures[0].payload["phase"] as? String == "exit"
                    && failures[0].payload["cause"] as? String == "exit-timeout",
                    "timeout diagnostic lost exact process ownership or leaked extra fields")
        try require(!h.sidecar.start() && !h.sidecar.sendSidecarEnvelope("{}"),
                    "failed Quit reopened a drained sidecar runtime")
    }
#endif

    private static func assertExit(_ h: Harness, pid: Int32, status: Int32, mode: String) throws {
        try require(h.successes > 0 && h.terminations.count == 1, "success did not come from the one actual process callback")
        let ended = h.terminations[0]
        try require(ended.generation == 1 && ended.processIdentifier == pid && ended.status == status && ended.expected,
                    "shutdown callback has wrong process/generation/status/expected")
        try require(h.sidecar.processIdentifier == nil && !h.completionBeforeActualExit,
                    "shutdown completion preceded actual exit ownership cleanup")
        let exits = recordedTraces.enumerated().filter { $0.element.type == "sidecar.exit" }
        let shutdown = recordedTraces.enumerated().filter { $0.element.type == "shutdown.sidecar" }
        try require(exits.count == 1 && shutdown.count == 1 && exits[0].offset < shutdown[0].offset
                    && shutdown[0].element.direction == "recv"
                    && Set(shutdown[0].element.payload.keys) == Set(["mode"])
                    && shutdown[0].element.payload["mode"] as? String == mode,
                    "shutdown completion trace did not follow the exact actual sidecar exit")
    }

    private final class Harness {
        var ready = false
        var terminations: [JavaSidecarTermination] = []
        var results: [Result<Void, JavaSidecarShutdownFailure>] = []
        var fatals: [String] = []
        var completionBeforeActualExit = false
        let mode: String
        lazy var sidecar = JavaSidecar(config: HostConfig(mode: mode),
            onSidecarEnvelope: { [weak self] _, _, _ in self?.ready = true },
            onTermination: { [weak self] value in self?.terminations.append(value) },
            onFatalError: { [weak self] cause in self?.fatals.append(cause) })
        var successes: Int { results.filter { if case .success = $0 { return true }; return false }.count }
        init(mode: String) { self.mode = mode }
        func start() throws {
            try require(sidecar.start() && until(seconds: 3) { self.ready }, "test-owned child failed readiness")
        }
        func pid() throws -> Int32 {
            guard let pid = sidecar.processIdentifier, pid > 0 else { throw Failure("no exact owned child PID") }
            return pid
        }
        func stop() {
            sidecar.stopForHostShutdown { [weak self] result in
                guard let self else { return }
                if case .success = result, self.sidecar.processIdentifier != nil { self.completionBeforeActualExit = true }
                self.results.append(result)
            }
        }
        func cleanup() {
            if let pid = sidecar.processIdentifier, pid > 0 {
                _ = Darwin.kill(pid, SIGKILL)
                _ = until(seconds: 2) { self.sidecar.processIdentifier == nil }
            }
            spin(seconds: 0.05)
        }
    }

    private static func until(seconds: Double, mode: RunLoop.Mode = .default, _ condition: () -> Bool) -> Bool {
        let deadline = ProcessInfo.processInfo.systemUptime + seconds
        while !condition() && ProcessInfo.processInfo.systemUptime < deadline {
            _ = RunLoop.current.run(mode: mode, before: Date().addingTimeInterval(0.01))
        }
        return condition()
    }
    private static func spin(seconds: Double) { _ = until(seconds: seconds) { false } }
    private struct Failure: Error, CustomStringConvertible {
        let description: String
        init(_ description: String) { self.description = description }
    }
    private static func require(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() { throw Failure(message) }
    }
}
