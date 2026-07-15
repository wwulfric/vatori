import Foundation

struct HostConfig {
    var module: String = "examples/counter-app"
    var repoRoot: String = FileManager.default.currentDirectoryPath
    var mavenRepo: String = ""
    var mavenExecutable: String = "mvn"
    var transport: String = "compatibility-http"
    var flowIpcAcceptanceFlags: Set<String> = []
    var flowIpcAcceptanceValues: [String: String] = [:]
    var flowIpcAcceptanceHarnessEnabled: Bool = false
    var flowIpcStandardBundleEnabled: Bool = false
    var flowIpcSidecarRestartLimit: Int = 3
    var flowIpcSidecarRecoveryReadyTimeoutSeconds: Double = 15
    var hostNoActivate: Bool = false
    var title: String = "Vaadin Desktop"
    var width: Double = 1100
    var height: Double = 780

    var flowIpcSidecarRecoveryEnabled: Bool {
        transport == "flow-ipc"
            && flowIpcStandardBundleEnabled
            && flowIpcSidecarRestartLimit > 0
    }

    static func parse(
        arguments commandLineArguments: [String] = Array(CommandLine.arguments.dropFirst()),
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> HostConfig {
        var config = HostConfig()
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
                config.transport = arguments.next() ?? config.transport
            case "--title":
                config.title = arguments.next() ?? config.title
            case "--width":
                config.width = Double(arguments.next() ?? "") ?? config.width
            case "--height":
                config.height = Double(arguments.next() ?? "") ?? config.height
            default:
#if VATORI_FLOW_IPC_ACCEPTANCE
                config.applyFlowIpcAcceptanceArgument(argument)
#endif
                continue
            }
        }

        if config.mavenRepo.isEmpty {
            config.mavenRepo = "\(config.repoRoot)/.m2/repository"
        }
        if !config.module.hasPrefix("/") {
            config.module = "\(config.repoRoot)/\(config.module)"
        }
#if VATORI_FLOW_IPC_ACCEPTANCE
        config.configureFlowIpcAcceptance(environment)
#endif
        config.hostNoActivate = environment["VAADIN_DESKTOP_HOST_NO_ACTIVATE"] == "1"
        config.flowIpcStandardBundleEnabled =
            environment["VAADIN_DESKTOP_FLOW_IPC_STANDARD_BUNDLE"] == "1"
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
