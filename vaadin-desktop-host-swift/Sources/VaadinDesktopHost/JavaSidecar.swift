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
    private let onApplicationURL: (URL) -> Void
    private let onFlowIpcEnvelope: (String, [String: Any]) -> Void
    private let onTermination: (JavaSidecarTermination) -> Void
    private var process: Process?
    private var stdinPipe: Pipe?
    private var stdoutPipe: Pipe?
    private var stderrPipe: Pipe?
    private var generation = 0
    private var runningGeneration: Int?
    private var shutdownRequested = false

    init(config: HostConfig,
         onApplicationURL: @escaping (URL) -> Void,
         onFlowIpcEnvelope: @escaping (String, [String: Any]) -> Void,
         onTermination: @escaping (JavaSidecarTermination) -> Void) {
        self.config = config
        self.onApplicationURL = onApplicationURL
        self.onFlowIpcEnvelope = onFlowIpcEnvelope
        self.onTermination = onTermination
    }

    @discardableResult
    func start() -> Bool {
        guard !shutdownRequested,
              process?.isRunning != true else {
            return false
        }
        let modulePom = "\(config.module)/pom.xml"
        guard FileManager.default.fileExists(atPath: modulePom) else {
            trace("send", "error", [
                "code": "moduleNotFound",
                "message": "Module pom not found: \(modulePom)"
            ])
            return false
        }

        generation += 1
        let launchGeneration = generation
        let process = Process()
        process.currentDirectoryURL = URL(fileURLWithPath: config.repoRoot)
        let launchPlan = JavaSidecarLaunchPlan.make(config: config, modulePom: modulePom)
        process.executableURL = URL(fileURLWithPath: launchPlan.executable)
        process.arguments = launchPlan.arguments

        var environment = ProcessInfo.processInfo.environment
        environment["VAADIN_DESKTOP_HOST"] = config.transport == "flow-ipc" ? "flow-ipc" : "headless"
        environment["VAADIN_DESKTOP_TRANSPORT"] = config.transport
        environment["VAADIN_DESKTOP_NATIVE_BRIDGE"] = "host-protocol"
        environment["VAADIN_DESKTOP_HOST_PROTOCOL"] = "stdout-v0"
        environment["VAADIN_DESKTOP_REPO_ROOT"] = config.repoRoot
        environment["VAADIN_DESKTOP_MODULE"] = config.module
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
            "modulePom": modulePom,
            "jar": launchPlan.directJar ?? "",
            "nativeBridge": "host-protocol",
            "transport": config.transport,
            "generation": launchGeneration
        ])

        do {
            try process.run()
            trace("recv", "sidecar.started", [
                "generation": launchGeneration,
                "processIdentifier": process.processIdentifier,
                "transport": config.transport
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
        if config.transport == "flow-ipc" {
            trace("send", "shutdown", [
                "target": "java-sidecar",
                "transport": "flow-ipc",
                "reason": "host-terminate"
            ])
            sendFlowIpc("{\"version\":\"flow-ipc-v1\",\"type\":\"shutdown\",\"id\":\"shutdown\",\"correlationId\":\"shutdown\",\"sessionId\":\"flow-ipc-session\",\"uiId\":\"main\",\"windowId\":\"main\",\"route\":\"\",\"payload\":{},\"error\":null,\"timestamp\":0}")
        }
        stdoutPipe?.fileHandleForReading.readabilityHandler = nil
        stderrPipe?.fileHandleForReading.readabilityHandler = nil
        if let process, process.isRunning {
            trace("send", "shutdown.sidecar", [:])
            process.terminate()
        }
    }

    @discardableResult
    func sendFlowIpc(_ envelope: String) -> Bool {
        guard let stdinPipe,
              process?.isRunning == true,
              runningGeneration != nil else {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "sidecar-unavailable"
            ])
            return false
        }
        let line = "VFIPC1 \(envelope)\n"
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

        if line.hasPrefix("VDP1 ") {
            handleHostProtocolEnvelope(String(line.dropFirst(5)))
        }

        if line.hasPrefix("VFIPC1 ") {
            handleFlowIpcEnvelope(String(line.dropFirst(7)), generation: generation)
        }

        if line.hasPrefix("Application URL: ") {
            let text = String(line.dropFirst("Application URL: ".count)).trimmingCharacters(in: .whitespacesAndNewlines)
            if let url = URL(string: text) {
                DispatchQueue.main.async {
                    self.onApplicationURL(url)
                }
            }
        }
    }

    private func handleFlowIpcEnvelope(_ envelope: String, generation: Int) {
        guard let data = envelope.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            trace("send", "error", [
                "code": "invalidFlowIpcEnvelope",
                "raw": envelope
            ])
            return
        }
        DispatchQueue.main.async {
            guard self.runningGeneration == generation else {
                return
            }
            self.onFlowIpcEnvelope(envelope, object)
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

    private func handleHostProtocolEnvelope(_ envelope: String) {
        guard let data = envelope.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            trace("send", "error", [
                "code": "invalidJavaEnvelope",
                "raw": envelope
            ])
            return
        }

        let type = object["type"] as? String ?? "unknown"
        trace("recv", type, [
            "source": "java-sidecar",
            "envelope": object
        ])

        if type == "command",
           let payload = object["payload"] as? [String: Any],
           let command = payload["command"] as? String {
            trace("send", "capabilityResult", [
                "command": command,
                "mode": "swift-host-scaffold",
                "result": "traced"
            ])
        }
    }
}
