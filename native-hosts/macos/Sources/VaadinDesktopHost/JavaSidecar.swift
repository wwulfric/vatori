import Cocoa
import Darwin

struct JavaSidecarTermination {
    let generation: Int
    let processIdentifier: Int32
    let status: Int32
    let reason: Process.TerminationReason
    let expected: Bool
}

struct JavaSidecarShutdownFailure: Error {
    let phase: String
    let cause: String
    let signalErrno: Int32?
}

final class JavaSidecar {
    private final class HostShutdownAttempt {
        let process: Process
        let generation: Int
        let processIdentifier: Int32
        let token = UUID()
        var phase: String
        var completions: [(Result<Void, JavaSidecarShutdownFailure>) -> Void]

        init(process: Process, generation: Int, phase: String,
             completion: @escaping (Result<Void, JavaSidecarShutdownFailure>) -> Void) {
            self.process = process
            self.generation = generation
            self.processIdentifier = process.processIdentifier
            self.phase = phase
            self.completions = [completion]
        }
    }

    private enum RetirementKind: String {
        case recovery
        case replacement
    }

    private struct RetirementDeadline {
        let process: Process
        let generation: Int
        let processIdentifier: Int32
        let token: UUID
        let workItem: DispatchWorkItem
        var failureReported = false
    }

    private static let retirementGraceSeconds = 1.0
    private static let retirementExitConfirmationSeconds = 1.0
    private let config: HostConfig
    private let onSidecarEnvelope: (String, [String: Any], Int) -> Void
    private let onTermination: (JavaSidecarTermination) -> Void
    private let onFatalError: (String) -> Void
    private var process: Process?
    private var stdinPipe: Pipe?
    private var inputWriter: NativeSidecarInputWriter?
    private var stdoutPipe: Pipe?
    private var stderrPipe: Pipe?
    private var generation = 0
    private var runningGeneration: Int?
    private var shutdownRequested = false
    private var retirementDeadline: RetirementDeadline?
    private var hostShutdownAttempt: HostShutdownAttempt?
    private var hostShutdownCompleted = false
    private var hostShutdownMode = "graceful"
    private let stdinLock = NSLock()

    init(config: HostConfig,
         onSidecarEnvelope: @escaping (String, [String: Any], Int) -> Void,
         onTermination: @escaping (JavaSidecarTermination) -> Void,
         onFatalError: @escaping (String) -> Void) {
        self.config = config
        self.onSidecarEnvelope = onSidecarEnvelope
        self.onTermination = onTermination
        self.onFatalError = onFatalError
    }

    @discardableResult
    func start() -> Bool {
        guard !shutdownRequested,
              process == nil else {
            return false
        }
        generation += 1
        let launchGeneration = generation
        let process = Process()
        process.currentDirectoryURL = config.sidecarJarURL.deletingLastPathComponent()
        let launchPlan = JavaSidecarLaunchPlan.make(config: config)
        process.executableURL = URL(fileURLWithPath: launchPlan.executable)
        process.arguments = launchPlan.arguments

        var environment = ProcessInfo.processInfo.environment
        environment["VAADIN_DESKTOP_TRANSPORT"] = config.transport.rawValue
        if let source = config.flowWebAppSource {
            environment["VAADIN_DESKTOP_FLOW_FRONTEND_MODE"] = source.mode
        }
        if let applicationResourcesURL = config.applicationResourcesURL {
            environment["VATORI_APPLICATION_RESOURCES"] =
                applicationResourcesURL.path
        }
        process.environment = environment

        let stdinPipe = Pipe()
        let stdoutPipe = Pipe()
        let stderrPipe = Pipe()
        process.standardInput = stdinPipe
        process.standardOutput = stdoutPipe
        process.standardError = stderrPipe
        stdinLock.lock()
        self.stdinPipe = stdinPipe
        self.stdoutPipe = stdoutPipe
        self.stderrPipe = stderrPipe
        self.process = process
        runningGeneration = launchGeneration
        stdinLock.unlock()

        do {
            let writer = try NativeSidecarInputWriter(
                handle: stdinPipe.fileHandleForWriting,
                onWritten: { [weak self, weak process] bytes in
                    NativeMainRunLoop.perform {
                        guard let self, let process,
                              self.owns(process, generation: launchGeneration) else { return }
                        trace("send", "flow-ipc.sidecarWrite", [
                            "target": "java-sidecar", "status": "written", "bytes": bytes
                        ])
                    }
                },
                onBackpressure: { [weak self, weak process] bytes, frames, partialBytes in
                    NativeMainRunLoop.perform {
                        guard let self, let process,
                              !self.shutdownRequested,
                              self.owns(process, generation: launchGeneration) else { return }
                        trace("recv", "sidecar.writeBackpressure", [
                            "generation": launchGeneration,
                            "processIdentifier": process.processIdentifier,
                            "pendingBytes": bytes, "pendingFrames": frames,
                            "partialFrameBytes": partialBytes
                        ])
                    }
                },
                onFailure: { [weak self, weak process] failure in
                    NativeMainRunLoop.perform {
                        guard let self, let process,
                              !self.shutdownRequested,
                              process.isRunning,
                              self.owns(process, generation: launchGeneration) else { return }
                        var payload: [String: Any] = [
                            "generation": launchGeneration,
                            "processIdentifier": process.processIdentifier,
                            "cause": failure.cause
                        ]
                        if let value = failure.signalErrno { payload["errno"] = value }
                        trace("send", "sidecar.writeFailed", payload)
                        self.onFatalError("sidecar-write-\(failure.cause)")
                    }
                })
            stdinLock.lock()
            inputWriter = writer
            stdinLock.unlock()
        } catch {
            // No dispatch source owns this handle when construction fails.
            try? stdinPipe.fileHandleForWriting.close()
            clearProcess(generation: launchGeneration)
            trace("send", "sidecar.startFailed", [
                "cause": "stdin-writer-setup-failed", "generation": launchGeneration
            ])
            return false
        }

        let stdoutReader = LineReader { [weak self] line in
            self?.handleStdout(line, generation: launchGeneration)
        }
        stdoutPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
            guard self?.isActive(generation: launchGeneration) == true else {
                return
            }
            stdoutReader.append(handle.availableData)
        }

        stderrPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
            guard self?.isActive(generation: launchGeneration) == true else {
                return
            }
            let data = handle.availableData
            if !data.isEmpty, let text = String(data: data, encoding: .utf8) {
                FileHandle.standardError.write(("[java:err] \(text)").data(using: .utf8)!)
            }
        }

        process.terminationHandler = { [weak self] process in
            NativeMainRunLoop.perform {
                self?.handleTermination(process, generation: launchGeneration)
            }
        }

        trace("send", "launch.sidecar", [
            "command": launchPlan.commandSummary,
            "jar": "$APP/Contents/Resources/Sidecar/application.jar",
            "transport": config.transport.rawValue,
            "generation": launchGeneration
        ])

        do {
            try process.run()
            trace("recv", "sidecar.started", [
                "generation": launchGeneration,
                "processIdentifier": process.processIdentifier,
                "transport": config.transport.rawValue
            ])
            return true
        } catch {
            clearProcess(generation: launchGeneration)
            trace("send", "sidecar.startFailed", [
                "cause": "process-run-failed",
                "message": error.localizedDescription,
                "generation": launchGeneration
            ])
            return false
        }
    }

    /// Shared retirement for AppKit Quit and fatal host shutdown.
    /// The completion succeeds only after the retained child has actually exited,
    /// or when there is no owned process. Failed attempts remain fenced and retryable.
    func stopForHostShutdown(
        completion: @escaping (Result<Void, JavaSidecarShutdownFailure>) -> Void
    ) {
        precondition(Thread.isMainThread)
        if let attempt = hostShutdownAttempt, attempt.phase != "failed" {
            attempt.completions.append(completion)
            return
        }
        let firstRequest = !shutdownRequested
        shutdownRequested = true
        stdinLock.lock()
        retirementDeadline?.workItem.cancel()
        retirementDeadline = nil
        guard !hostShutdownCompleted, let process, let generation = runningGeneration,
              process.processIdentifier > 0 else {
            stdinLock.unlock()
            NativeMainRunLoop.perform { completion(.success(())) }
            return
        }
        // Fencing and writer admission never wait for pipe capacity. This
        // generation's writer alone owns its descriptor through cancellation.
        let shutdownInput = firstRequest ? inputWriter : nil
        stdinLock.unlock()
        let attempt = HostShutdownAttempt(
            process: process, generation: generation,
            phase: firstRequest ? "channel" : "term", completion: completion)
        hostShutdownAttempt = attempt
        if firstRequest {
            trace("send", "shutdown", [
                "target": "java-sidecar",
                "transport": config.transport.rawValue,
                "reason": "host-terminate"
            ])
            if let shutdownInput {
                let envelope = "{\"version\":1,\"type\":\"shutdown\",\"id\":\"shutdown\",\"correlationId\":\"shutdown\",\"payload\":{},\"error\":null,\"timestamp\":0}"
                let data = Data("\(sidecarWirePrefix)\(envelope)\n".utf8)
                shutdownInput.retire(shutdownFrame: data)
            }
            NativeMainRunLoop.perform(after: 1) { [weak self] in
                self?.advanceHostShutdown(attempt, signal: SIGTERM)
            }
        } else {
            // A failed Quit cannot restore a drained Flow runtime or resend its
            // shutdown frame. A new attempt owns a new token and deadline only.
            NativeMainRunLoop.perform { [weak self] in
                self?.advanceHostShutdown(attempt, signal: SIGTERM)
            }
        }
    }

    private func advanceHostShutdown(_ attempt: HostShutdownAttempt, signal: Int32) {
        precondition(Thread.isMainThread)
        guard hostShutdownAttempt === attempt,
              hostShutdownAttempt?.token == attempt.token,
              attempt.phase != "failed" else { return }
        stdinLock.lock()
        guard self.process === attempt.process,
              runningGeneration == attempt.generation,
              attempt.processIdentifier > 0,
              attempt.process.processIdentifier == attempt.processIdentifier else {
            stdinLock.unlock()
            return
        }
        if !attempt.process.isRunning {
            stdinLock.unlock()
            // Process may have exited before its callback reached this run loop.
            // Do not convert an isRunning observation into a synthetic exit.
            awaitHostShutdownExit(attempt)
            return
        }
        let result = Darwin.kill(attempt.processIdentifier, signal)
        let failure = result == 0 ? 0 : errno
        stdinLock.unlock()
        let phase = signal == SIGTERM ? "term" : "kill"
        if result != 0 {
            failHostShutdown(attempt, phase: phase, cause: "signal-failed", signalErrno: failure)
            return
        }
        hostShutdownMode = signal == SIGTERM ? "terminated" : "killed"
        attempt.phase = phase
        trace("send", signal == SIGTERM ? "sidecar.shutdownTerminated" : "sidecar.shutdownEscalated", [
            "generation": attempt.generation,
            "processIdentifier": attempt.processIdentifier,
            "graceSeconds": 1.0
        ])
        if signal == SIGTERM {
            NativeMainRunLoop.perform(after: 1) { [weak self] in
                self?.advanceHostShutdown(attempt, signal: SIGKILL)
            }
        } else {
            awaitHostShutdownExit(attempt)
        }
    }

    private func awaitHostShutdownExit(_ attempt: HostShutdownAttempt) {
        guard hostShutdownAttempt === attempt, hostShutdownAttempt?.token == attempt.token,
              attempt.phase != "failed" else { return }
        attempt.phase = "exit"
        NativeMainRunLoop.perform(after: 2) { [weak self] in
            guard let self, self.hostShutdownAttempt === attempt,
                  self.hostShutdownAttempt?.token == attempt.token,
                  attempt.phase == "exit" else { return }
            self.failHostShutdown(attempt, phase: "exit", cause: "exit-timeout", signalErrno: nil)
        }
    }

    private func failHostShutdown(
        _ attempt: HostShutdownAttempt, phase: String, cause: String, signalErrno: Int32?
    ) {
        guard hostShutdownAttempt === attempt, hostShutdownAttempt?.token == attempt.token,
              attempt.phase != "failed" else { return }
        attempt.phase = "failed"
        var payload: [String: Any] = [
            "generation": attempt.generation,
            "processIdentifier": attempt.processIdentifier,
            "phase": phase,
            "cause": cause
        ]
        if let signalErrno { payload["errno"] = signalErrno }
        trace("send", "sidecar.shutdownFailed", payload)
        let completions = attempt.completions
        attempt.completions.removeAll()
        let failure = JavaSidecarShutdownFailure(phase: phase, cause: cause, signalErrno: signalErrno)
        for completion in completions { completion(.failure(failure)) }
    }

    /// True means bounded admission to this generation's FIFO, not delivery or
    /// a Java acknowledgement. Only the writer's completion emits "written".
    @discardableResult
    func sendSidecarEnvelope(_ envelope: String) -> Bool {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        return writeSidecarEnvelopeLocked(envelope)
    }

    @discardableResult
    func sendSidecarEnvelope(
        _ envelope: String,
        expectedGeneration: Int
    ) -> Bool {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        guard runningGeneration == expectedGeneration else {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "generation-mismatch",
                "expectedGeneration": expectedGeneration,
                "activeGeneration": runningGeneration ?? 0
            ])
            return false
        }
        return writeSidecarEnvelopeLocked(envelope)
    }

    private func writeSidecarEnvelopeLocked(_ envelope: String) -> Bool {
        guard !shutdownRequested,
              let inputWriter,
              process?.isRunning == true,
              runningGeneration != nil else {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "sidecar-unavailable"
            ])
            return false
        }
        let line = "\(sidecarWirePrefix)\(envelope)\n"
        guard let data = line.data(using: .utf8) else {
            return false
        }
        guard inputWriter.enqueue(data) else {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "admission-rejected",
                "bytes": data.count
            ])
            return false
        }
        return true
    }

    var processIdentifier: Int32? {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        guard process?.isRunning == true else {
            return nil
        }
        return process?.processIdentifier
    }

    func isActive(generation: Int) -> Bool {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        return runningGeneration == generation
            && process?.isRunning == true
    }

    @discardableResult
    func terminateForRecovery(reason: String) -> Bool {
        terminateForRetirement(reason: reason, kind: .recovery)
    }

    @discardableResult
    func terminateForReplacement(reason: String) -> Bool {
        terminateForRetirement(reason: reason, kind: .replacement)
    }

    private func terminateForRetirement(reason: String, kind: RetirementKind) -> Bool {
        stdinLock.lock()
        guard !shutdownRequested,
              let process,
              process.isRunning,
              let generation = runningGeneration,
              process.processIdentifier > 0 else {
            stdinLock.unlock()
            return false
        }
        let processIdentifier = process.processIdentifier
        if let pending = retirementDeadline,
           pending.process === process,
           pending.generation == generation,
           pending.processIdentifier == processIdentifier {
            // The first request owns the deadline and reason, even if a later
            // request uses the other retirement kind. Duplicates cannot rearm it.
            stdinLock.unlock()
            return true
        }
        retirementDeadline?.workItem.cancel()
        let token = UUID()
        let workItem = DispatchWorkItem { [weak self, weak process] in
            guard let self, let process else { return }
            self.escalateRetirement(
                process: process, generation: generation,
                processIdentifier: processIdentifier, token: token, reason: reason, kind: kind)
        }
        retirementDeadline = RetirementDeadline(
            process: process, generation: generation,
            processIdentifier: processIdentifier, token: token, workItem: workItem)
        let deadline = DispatchTime.now() + Self.retirementGraceSeconds
        stdinLock.unlock()
        trace("send", "sidecar.\(kind.rawValue)Terminated", [
            "generation": generation,
            "processIdentifier": processIdentifier,
            "reason": reason
        ])
        DispatchQueue.main.asyncAfter(deadline: deadline, execute: workItem)
        DispatchQueue.main.asyncAfter(
            deadline: deadline + Self.retirementExitConfirmationSeconds
        ) { [weak self, weak process] in
            guard let self, let process else { return }
            self.failRetirement(
                process: process, generation: generation,
                processIdentifier: processIdentifier, token: token,
                kind: kind, cause: "exit-confirmation-timeout")
        }
        process.terminate()
        return true
    }

    private func escalateRetirement(
        process: Process, generation: Int, processIdentifier: Int32,
        token: UUID, reason: String, kind: RetirementKind
    ) {
        stdinLock.lock()
        guard !shutdownRequested,
              let pending = retirementDeadline,
              pending.token == token,
              pending.process === process,
              pending.generation == generation,
              pending.processIdentifier == processIdentifier,
              self.process === process,
              runningGeneration == generation,
              processIdentifier > 0,
              process.processIdentifier == processIdentifier,
              process.isRunning else {
            stdinLock.unlock()
            return
        }
        // Signal only this retained process. Never signal a group or locate a
        // process by executable name. Keep the deadline until actual exit so
        // duplicates cannot rearm it while termination delivery is pending.
        let result = Darwin.kill(processIdentifier, SIGKILL)
        let failure = result == 0 ? 0 : errno
        stdinLock.unlock()
        var payload: [String: Any] = [
            "generation": generation,
            "processIdentifier": processIdentifier,
            "reason": reason,
            "graceSeconds": Self.retirementGraceSeconds
        ]
        if result == 0 {
            trace("send", "sidecar.\(kind.rawValue)Escalated", payload)
        } else {
            payload["errno"] = failure
            trace("send", "sidecar.\(kind.rawValue)EscalationFailed", payload)
            failRetirement(
                process: process, generation: generation,
                processIdentifier: processIdentifier, token: token,
                kind: kind, cause: "signal-failed")
        }
        // Only Process.terminationHandler may clear ownership and notify the
        // controller. A successful signal is not proof that the child exited.
    }

    private func failRetirement(
        process: Process, generation: Int, processIdentifier: Int32,
        token: UUID, kind: RetirementKind, cause: String
    ) {
        stdinLock.lock()
        guard !shutdownRequested,
              var pending = retirementDeadline,
              pending.token == token, !pending.failureReported,
              pending.process === process,
              pending.generation == generation,
              pending.processIdentifier == processIdentifier,
              self.process === process, runningGeneration == generation else {
            stdinLock.unlock()
            return
        }
        // Even isRunning == false is not an actual-exit callback. Retain all
        // ownership until that callback; the controller must fence and shut down.
        pending.failureReported = true
        pending.workItem.cancel()
        retirementDeadline = pending
        stdinLock.unlock()
        trace("send", "sidecar.\(kind.rawValue)RetirementFailed", [
            "generation": generation,
            "processIdentifier": processIdentifier,
            "cause": cause
        ])
        onFatalError("sidecar-\(kind.rawValue)-retirement-\(cause)")
    }

    private func handleStdout(_ line: String, generation: Int) {
        guard isActive(generation: generation) else {
            return
        }
        print("[java] \(summarizedJavaStdoutLine(line))")
        fflush(stdout)

        if line.hasPrefix(sidecarWirePrefix) {
            handleSidecarEnvelope(
                String(line.dropFirst(sidecarWirePrefix.count)),
                generation: generation)
        }
    }

    private func handleSidecarEnvelope(_ envelope: String, generation: Int) {
        guard let data = envelope.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            trace("send", "error", [
                "code": "invalidSidecarEnvelope",
                "raw": envelope
            ])
            return
        }
        guard hasValidSidecarVersion(object) else {
            trace("send", "sidecar.protocolRejected", [
                "cause": "unsupported-sidecar-version",
                "version": object["version"] ?? NSNull()
            ])
            DispatchQueue.main.async {
                guard self.isActive(generation: generation) else {
                    return
                }
                self.onFatalError(
                    "unsupported-sidecar-version")
            }
            return
        }
        DispatchQueue.main.async {
            guard self.isActive(generation: generation) else {
                return
            }
            self.onSidecarEnvelope(envelope, object, generation)
        }
    }

    private func handleTermination(_ process: Process, generation: Int) {
        guard isActive(generation: generation)
                || isCurrentGeneration(generation) else {
            return
        }
        let termination = JavaSidecarTermination(
            generation: generation,
            processIdentifier: process.processIdentifier,
            status: process.terminationStatus,
            reason: process.terminationReason,
            expected: shutdownRequested)
        clearProcess(generation: generation)
        trace("recv", "sidecar.exit", [
            "generation": termination.generation,
            "processIdentifier": termination.processIdentifier,
            "status": termination.status,
            "reason": termination.reason.rawValue,
            "expected": termination.expected
        ])
        if let attempt = hostShutdownAttempt,
           attempt.process === process, attempt.generation == generation,
           attempt.processIdentifier == termination.processIdentifier {
            hostShutdownCompleted = true
            hostShutdownAttempt = nil
            trace("recv", "shutdown.sidecar", ["mode": hostShutdownMode])
            let completions = attempt.completions
            attempt.completions.removeAll()
            for completion in completions { completion(.success(())) }
        }
        onTermination(termination)
    }

    private func clearProcess(generation: Int) {
        stdinLock.lock()
        guard runningGeneration == generation else {
            stdinLock.unlock()
            return
        }
        let writer = inputWriter
        let stdoutHandle = stdoutPipe?.fileHandleForReading
        let stderrHandle = stderrPipe?.fileHandleForReading
        retirementDeadline?.workItem.cancel()
        retirementDeadline = nil
        stdinPipe = nil
        inputWriter = nil
        stdoutPipe = nil
        stderrPipe = nil
        process = nil
        runningGeneration = nil
        stdinLock.unlock()

        stdoutHandle?.readabilityHandler = nil
        stderrHandle?.readabilityHandler = nil
        writer?.cancel()
        try? stdoutHandle?.close()
        try? stderrHandle?.close()
    }

    private func isCurrentGeneration(_ generation: Int) -> Bool {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        return runningGeneration == generation
    }

    private func owns(_ process: Process, generation: Int) -> Bool {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        return self.process === process && runningGeneration == generation
    }

}
