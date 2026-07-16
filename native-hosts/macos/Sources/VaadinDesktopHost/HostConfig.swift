import Foundation

enum HostTransport: String {
    case flowIpc = "flow-ipc"
    case compatibilityHttp = "compatibility-http"
}

enum HostConfigError: LocalizedError {
    case missingTransport
    case invalidTransport(String)

    var errorDescription: String? {
        switch self {
        case .missingTransport:
            return "Vaadin Desktop Host requires --transport flow-ipc|compatibility-http"
        case .invalidTransport(let value):
            return "Unsupported Vaadin Desktop transport: \(value)"
        }
    }
}

struct HostConfig {
    var runtimeHooks: any HostRuntimeHooks = makeHostRuntimeHooks()
    var module: String = ""
    var repoRoot: String = FileManager.default.currentDirectoryPath
    var mavenRepo: String = ""
    var mavenExecutable: String = "mvn"
    var transport: HostTransport = .flowIpc
    var flowIpcSidecarRestartLimit: Int = 3
    var flowIpcSidecarRecoveryReadyTimeoutSeconds: Double = 15
    var hostNoActivate: Bool = false

    var flowIpcSidecarRecoveryEnabled: Bool {
        transport == .flowIpc && flowIpcSidecarRestartLimit > 0
    }

    static func parse(
        arguments commandLineArguments: [String] = Array(CommandLine.arguments.dropFirst()),
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) throws -> HostConfig {
        var config = HostConfig()
        var transportValue: String?
        var arguments = commandLineArguments.makeIterator()
        while let argument = arguments.next() {
            switch argument {
            case "--module":
                config.module = arguments.next() ?? config.module
            case "--repo-root":
                config.repoRoot = arguments.next() ?? config.repoRoot
            case "--maven-repo":
                config.mavenRepo = arguments.next() ?? config.mavenRepo
            case "--maven-executable":
                config.mavenExecutable = arguments.next() ?? config.mavenExecutable
            case "--transport":
                transportValue = arguments.next()
            default:
                continue
            }
        }

        guard let transportValue, !transportValue.isEmpty else {
            throw HostConfigError.missingTransport
        }
        guard let transport = HostTransport(rawValue: transportValue) else {
            throw HostConfigError.invalidTransport(transportValue)
        }
        config.transport = transport
        if config.mavenRepo.isEmpty {
            config.mavenRepo = "\(config.repoRoot)/.m2/repository"
        }
        if !config.module.isEmpty, !config.module.hasPrefix("/") {
            config.module = "\(config.repoRoot)/\(config.module)"
        }
        config.runtimeHooks.configure(environment: environment)
        config.hostNoActivate = environment["VAADIN_DESKTOP_MACOS_NO_ACTIVATE"] == "1"
        if let restartLimitText = environment["VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RESTART_LIMIT"],
           let restartLimit = Int(restartLimitText),
           restartLimit >= 0 {
            config.flowIpcSidecarRestartLimit = restartLimit
        }
        if let timeoutText = environment["VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RECOVERY_READY_TIMEOUT_SECONDS"],
           let timeout = Double(timeoutText),
           timeout >= 1 {
            config.flowIpcSidecarRecoveryReadyTimeoutSeconds = timeout
        }
        return config
    }
}
