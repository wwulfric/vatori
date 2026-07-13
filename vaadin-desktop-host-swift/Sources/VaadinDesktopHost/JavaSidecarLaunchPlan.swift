import Foundation

struct JavaSidecarLaunchPlan {
    let executable: String
    let arguments: [String]
    let directJar: String?
    let commandSummary: String

    static func make(config: HostConfig, modulePom: String) -> JavaSidecarLaunchPlan {
        if config.transport == "flow-ipc", let directJar = runnableJar(in: config.module) {
            let executable = javaExecutable()
            return JavaSidecarLaunchPlan(executable: executable,
                                         arguments: ["-jar", directJar],
                                         directJar: directJar,
                                         commandSummary: "\(executable) -jar")
        }

        let mavenMode = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_MAVEN_OFFLINE"] == "1" ? "-o" : "-U"
        return JavaSidecarLaunchPlan(executable: config.mavenExecutable,
                                     arguments: [
                                         mavenMode,
                                         "-Dmaven.repo.local=\(config.mavenRepo)",
                                         "-f",
                                         modulePom,
                                         "spring-boot:run"
                                     ],
                                     directJar: nil,
                                     commandSummary: "\(config.mavenExecutable) spring-boot:run")
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

    private static func runnableJar(in modulePath: String) -> String? {
        let targetPath = "\(modulePath)/target"
        guard let entries = try? FileManager.default.contentsOfDirectory(atPath: targetPath) else {
            return nil
        }
        let jars = entries
            .filter { entry in
                entry.hasSuffix(".jar")
                    && !entry.hasSuffix(".jar.original")
                    && !entry.hasSuffix("-sources.jar")
                    && !entry.hasSuffix("-javadoc.jar")
            }
            .sorted()
        guard let jar = jars.first else {
            return nil
        }
        return "\(targetPath)/\(jar)"
    }
}
