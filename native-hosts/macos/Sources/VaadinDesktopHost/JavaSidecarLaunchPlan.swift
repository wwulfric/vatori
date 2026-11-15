import Foundation

struct JavaSidecarLaunchPlan {
    let executable: String
    let arguments: [String]
    let commandSummary: String
    let jarDisplayPath: String
    let currentDirectoryURL: URL
    let environment: [String: String]

    static func make(
        config: HostConfig,
        inheritedEnvironment: [String: String] = ProcessInfo.processInfo.environment
    ) -> JavaSidecarLaunchPlan {
        let executable = javaExecutable(environment: inheritedEnvironment)
        var environment = inheritedEnvironment
        environment["VAADIN_DESKTOP_TRANSPORT"] = config.transport.rawValue
        // Absence is authoritative too: a new runtime must not borrow a prior
        // or caller-provided application's resource directory/frontend mode.
        environment["VAADIN_DESKTOP_FLOW_FRONTEND_MODE"] = config.flowWebAppSource?.mode
        environment["VATORI_APPLICATION_RESOURCES"] = config.applicationResourcesURL?.path
        return JavaSidecarLaunchPlan(
            executable: executable,
            arguments: ["-jar", config.sidecarJarURL.path],
            commandSummary: "\(executable) -jar \(config.sidecarJarDisplayPath)",
            jarDisplayPath: config.sidecarJarDisplayPath,
            currentDirectoryURL: config.sidecarJarURL.deletingLastPathComponent(),
            environment: environment)
    }

    private static func javaExecutable(environment: [String: String]) -> String {
        if let javaHome = environment["JAVA_HOME"], !javaHome.isEmpty {
            let candidate = "\(javaHome)/bin/java"
            if FileManager.default.isExecutableFile(atPath: candidate) {
                return candidate
            }
        }
        return "/usr/bin/java"
    }
}
