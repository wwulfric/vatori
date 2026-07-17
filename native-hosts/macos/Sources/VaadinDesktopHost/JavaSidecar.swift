import Cocoa

struct JavaSidecarTermination {
    let generation: Int
    let processIdentifier: Int32
    let status: Int32
    let reason: Process.TerminationReason
    let expected: Bool
}

final class JavaSidecar {
    private let config: HostConfig
    private let onSidecarEnvelope: (String, [String: Any]) -> Void
    private let onTermination: (JavaSidecarTermination) -> Void
    private var process: Process?
    private var stdinPipe: Pipe?
    private var stdoutPipe: Pipe?
    private var stderrPipe: Pipe?
    private var generation = 0
    private var runningGeneration: Int?
    private var shutdownRequested = false
    private let stdinLock = NSLock()

    init(config: HostConfig,
         onSidecarEnvelope: @escaping (String, [String: Any]) -> Void,
         onTermination: @escaping (JavaSidecarTermination) -> Void) {
        self.config = config
        self.onSidecarEnvelope = onSidecarEnvelope
        self.onTermination = onTermination
    }

    @discardableResult
    func start() -> Bool {
        guard !shutdownRequested,
              process?.isRunning != true else {
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
        process.environment = environment

        let stdinPipe = Pipe()
        let stdoutPipe = Pipe()
        let stderrPipe = Pipe()
        process.standardInput = stdinPipe
        process.standardOutput = stdoutPipe
        process.standardError = stderrPipe
        self.stdinPipe = stdinPipe
        self.stdoutPipe = stdoutPipe
        self.stderrPipe = stderrPipe
        self.process = process
        runningGeneration = launchGeneration

        let stdoutReader = LineReader { [weak self] line in
            self?.handleStdout(line, generation: launchGeneration)
        }
        stdoutPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
            guard self?.runningGeneration == launchGeneration else {
                return
            }
            stdoutReader.append(handle.availableData)
        }

        stderrPipe.fileHandleForReading.readabilityHandler = { [weak self] handle in
            guard self?.runningGeneration == launchGeneration else {
                return
            }
            let data = handle.availableData
            if !data.isEmpty, let text = String(data: data, encoding: .utf8) {
                FileHandle.standardError.write(("[java:err] \(text)").data(using: .utf8)!)
            }
        }

        process.terminationHandler = { [weak self] process in
            DispatchQueue.main.async {
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
            trace("send", "error", [
                "code": "sidecarStartFailed",
                "message": error.localizedDescription,
                "generation": launchGeneration
            ])
            return false
        }
    }

    func terminate() {
        guard !shutdownRequested else {
            return
        }
        shutdownRequested = true
        trace("send", "shutdown", [
            "target": "java-sidecar",
            "transport": config.transport.rawValue,
            "reason": "host-terminate"
        ])
        sendSidecarEnvelope("{\"version\":1,\"type\":\"shutdown\",\"id\":\"shutdown\",\"correlationId\":\"shutdown\",\"payload\":{},\"error\":null,\"timestamp\":0}")
        try? stdinPipe?.fileHandleForWriting.close()
        if let process, process.isRunning {
            let deadline = Date().addingTimeInterval(1)
            while process.isRunning && Date() < deadline {
                Thread.sleep(forTimeInterval: 0.02)
            }
            if process.isRunning {
                trace("send", "shutdown.sidecar", ["mode": "forced-after-timeout"])
                process.terminate()
            } else {
                trace("recv", "shutdown.sidecar", ["mode": "graceful"])
            }
        }
    }

    @discardableResult
    func sendSidecarEnvelope(_ envelope: String) -> Bool {
        stdinLock.lock()
        defer { stdinLock.unlock() }
        guard let stdinPipe,
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
        do {
            try stdinPipe.fileHandleForWriting.write(contentsOf: data)
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "written",
                "bytes": data.count
            ])
            return true
        } catch {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "write-failed",
                "message": error.localizedDescription
            ])
            return false
        }
    }

    var processIdentifier: Int32? {
        guard process?.isRunning == true else {
            return nil
        }
        return process?.processIdentifier
    }

    @discardableResult
    func terminateForRecovery(reason: String) -> Bool {
        guard !shutdownRequested,
              let process,
              process.isRunning else {
            return false
        }
        trace("send", "sidecar.recoveryTerminated", [
            "generation": runningGeneration ?? 0,
            "processIdentifier": process.processIdentifier,
            "reason": reason
        ])
        process.terminate()
        return true
    }

    private func handleStdout(_ line: String, generation: Int) {
        guard runningGeneration == generation else {
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
            trace("send", "error", [
                "code": "unsupportedSidecarVersion",
                "version": object["version"] ?? NSNull()
            ])
            DispatchQueue.main.async {
                self.terminate()
                NSApp.terminate(nil)
            }
            return
        }
        DispatchQueue.main.async {
            guard self.runningGeneration == generation else {
                return
            }
            self.onSidecarEnvelope(envelope, object)
        }
    }

    private func handleTermination(_ process: Process, generation: Int) {
        guard runningGeneration == generation else {
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
        onTermination(termination)
    }

    private func clearProcess(generation: Int) {
        guard runningGeneration == generation else {
            return
        }
        stdoutPipe?.fileHandleForReading.readabilityHandler = nil
        stderrPipe?.fileHandleForReading.readabilityHandler = nil
        try? stdinPipe?.fileHandleForWriting.close()
        try? stdoutPipe?.fileHandleForReading.close()
        try? stderrPipe?.fileHandleForReading.close()
        stdinPipe = nil
        stdoutPipe = nil
        stderrPipe = nil
        process = nil
        runningGeneration = nil
    }

}
