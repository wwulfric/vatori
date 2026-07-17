import Foundation

struct JavaSidecarLaunchPlan {
    let executable: String
    let arguments: [String]
    let commandSummary: String

    static func make(config: HostConfig) -> JavaSidecarLaunchPlan {
        let executable = javaExecutable()
        return JavaSidecarLaunchPlan(
            executable: executable,
            arguments: ["-jar", config.sidecarJarURL.path],
            commandSummary: "\(executable) -jar $APP/Contents/Resources/Sidecar/application.jar")
    }

    private static func javaExecutable() -> String {
        if let javaHome = ProcessInfo.processInfo.environment["JAVA_HOME"], !javaHome.isEmpty {
            let candidate = "\(javaHome)/bin/java"
            if FileManager.default.isExecutableFile(atPath: candidate) {
                return candidate
            }
        }
        return "/usr/bin/java"
    }
}
