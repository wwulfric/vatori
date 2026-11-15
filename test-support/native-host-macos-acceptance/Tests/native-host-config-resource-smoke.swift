import Foundation
import Darwin

// Isolate the production manifest loader from AppKit and acceptance observers.
protocol HostRuntimeObserver {
    var hostNoActivate: Bool { get }
    func configure(environment: [String: String])
}
struct ConfigSmokeObserver: HostRuntimeObserver {
    var hostNoActivate: Bool { false }
    func configure(environment: [String: String]) {}
}
func makeHostRuntimeObserver() -> any HostRuntimeObserver { ConfigSmokeObserver() }

@main
struct HostConfigResourceSmoke {
    static func main() throws {
        let fm = FileManager.default
        let root = fm.temporaryDirectory.appendingPathComponent("vatori-config-" + UUID().uuidString)
        try fm.createDirectory(at: root, withIntermediateDirectories: false)
        defer { try? fm.removeItem(at: root) }
        for name in ["app.jar", "opaque.js", "push.js"] {
            try Data("inert".utf8).write(to: root.appendingPathComponent(name))
        }
        try fm.createDirectory(at: root.appendingPathComponent("webapp"), withIntermediateDirectories: false)
        try Data(#"{"schemaVersion":1,"permissionMode":"manifest-enforced","windows":[]}"#.utf8)
            .write(to: root.appendingPathComponent("capabilities.json"))
        let base: [String: Any] = ["version": 1, "transport": "flow-ipc", "sidecarJar": "app.jar",
            "opaqueBridge": "opaque.js", "pushBridge": "push.js", "webApp": "webapp",
            "capabilityManifest": "capabilities.json",
            "recovery": ["restartLimit": 2, "readyTimeoutSeconds": 15]]
        func load(_ manifest: [String: Any]) throws -> HostConfig {
            try JSONSerialization.data(withJSONObject: manifest).write(to: root.appendingPathComponent("VatoriHost.json"))
            return try HostConfig.load(resourceURL: root, environment: [:])
        }
        if let scenario = CommandLine.arguments.dropFirst().first {
            let manifest = root.appendingPathComponent("VatoriHost.json")
            if scenario == "launch-fifo" {
                precondition(mkfifo(manifest.path, 0o600) == 0)
                do { _ = try HostConfig.load(resourceURL: root, environment: [:]); fatalError("accepted manifest FIFO") }
                catch HostConfigError.resourceNotRegularFile { return }
            } else if scenario == "capability-fifo" {
                let capability = root.appendingPathComponent("capabilities.json")
                try fm.removeItem(at: capability)
                precondition(mkfifo(capability.path, 0o600) == 0)
                do { _ = try load(base); fatalError("accepted capability FIFO") }
                catch HostConfigError.invalidCapabilityManifest(let invalid) {
                    precondition(invalid.parseError == "manifest-not-regular-file"); return
                }
            }
            fatalError("unknown scenario")
        }
        let initial = try load(base)
        precondition(initial.runtimeCandidates.isEmpty)
        var cases = 1
        let inherited = ["VATORI_APPLICATION_RESOURCES": "/unrelated/previous-app",
            "VAADIN_DESKTOP_FLOW_FRONTEND_MODE": "wrong-mode",
            "VAADIN_DESKTOP_TRANSPORT": "wrong-transport", "APP_SETTING": "preserve-me"]
        let initialPlan = JavaSidecarLaunchPlan.make(config: initial, inheritedEnvironment: inherited)
        precondition(initialPlan.arguments == ["-jar", initial.sidecarJarURL.path])
        precondition(initialPlan.currentDirectoryURL == initial.sidecarJarURL.deletingLastPathComponent())
        precondition(initialPlan.environment["VATORI_APPLICATION_RESOURCES"] == nil)
        precondition(initialPlan.environment["VAADIN_DESKTOP_FLOW_FRONTEND_MODE"] == "production-bundle")
        precondition(initialPlan.environment["VAADIN_DESKTOP_TRANSPORT"] == "flow-ipc")
        precondition(initialPlan.environment["APP_SETTING"] == "preserve-me")
        precondition(inherited["VATORI_APPLICATION_RESOURCES"] == "/unrelated/previous-app")
        cases += 1
        var withResources = base; withResources["applicationResources"] = "webapp"
        let declared = try load(withResources)
        let declaredPlan = JavaSidecarLaunchPlan.make(config: declared, inheritedEnvironment: inherited)
        precondition(declaredPlan.environment["VATORI_APPLICATION_RESOURCES"] == declared.applicationResourcesURL?.path)
        let absentAgain = JavaSidecarLaunchPlan.make(config: initial, inheritedEnvironment: declaredPlan.environment)
        precondition(absentAgain.environment["VATORI_APPLICATION_RESOURCES"] == nil)
        cases += 1
        var compatibility = base; compatibility["transport"] = "compatibility-http"; compatibility.removeValue(forKey: "webApp")
        let compatibilityPlan = JavaSidecarLaunchPlan.make(config: try load(compatibility), inheritedEnvironment: declaredPlan.environment)
        precondition(compatibilityPlan.environment["VAADIN_DESKTOP_FLOW_FRONTEND_MODE"] == nil)
        precondition(compatibilityPlan.environment["VATORI_APPLICATION_RESOURCES"] == nil)
        precondition(compatibilityPlan.environment["VAADIN_DESKTOP_TRANSPORT"] == "compatibility-http")
        cases += 1
        // Exercise OS Process delivery using an inert executable in place of
        // Java. This proves environment/working-directory delivery, not a JVM.
        let fakeJdk = root.appendingPathComponent("jdk")
        try fm.createDirectory(at: fakeJdk.appendingPathComponent("bin"), withIntermediateDirectories: true)
        let fakeJava = fakeJdk.appendingPathComponent("bin/java")
        try Data("#!/bin/sh\nprintf 'WORKDIR=%s\\n' \"$PWD\"\n/usr/bin/env\n".utf8).write(to: fakeJava)
        try fm.setAttributes([.posixPermissions: 0o700], ofItemAtPath: fakeJava.path)
        var childEnvironment = inherited; childEnvironment["JAVA_HOME"] = fakeJdk.path
        for configuration in [initial, declared] {
            let plan = JavaSidecarLaunchPlan.make(config: configuration, inheritedEnvironment: childEnvironment)
            let child = Process(), output = Pipe()
            child.executableURL = URL(fileURLWithPath: plan.executable)
            child.arguments = plan.arguments; child.environment = plan.environment
            child.currentDirectoryURL = plan.currentDirectoryURL; child.standardOutput = output
            try child.run()
            let deadline = ProcessInfo.processInfo.systemUptime + 3
            while child.isRunning && ProcessInfo.processInfo.systemUptime < deadline { Thread.sleep(forTimeInterval: 0.01) }
            if child.isRunning { kill(child.processIdentifier, SIGKILL); child.waitUntilExit(); fatalError("launch-plan child deadline") }
            child.waitUntilExit(); precondition(child.terminationStatus == 0)
            let lines = String(decoding: output.fileHandleForReading.readDataToEndOfFile(), as: UTF8.self).split(separator: "\n").map(String.init)
            let workingDirectories = lines.filter { $0.hasPrefix("WORKDIR=") }
            precondition(workingDirectories.count == 1)
            let actualDirectory = URL(fileURLWithPath: String(workingDirectories[0].dropFirst("WORKDIR=".count)))
            precondition(actualDirectory.resolvingSymlinksInPath().path == plan.currentDirectoryURL.resolvingSymlinksInPath().path)
            precondition(lines.contains("APP_SETTING=preserve-me"))
            let resourceLines = lines.filter { $0.hasPrefix("VATORI_APPLICATION_RESOURCES=") }
            if let declaredPath = configuration.applicationResourcesURL?.path {
                precondition(resourceLines == ["VATORI_APPLICATION_RESOURCES=" + declaredPath])
            } else { precondition(resourceLines.isEmpty) }
            cases += 1
        }
        let candidateWeb = root.appendingPathComponent("candidate-web")
        try fm.createDirectory(at: candidateWeb, withIntermediateDirectories: false)
        try Data("inert HTML".utf8).write(to: candidateWeb.appendingPathComponent("index.html"))
        let candidate: [String: Any] = ["sidecarJar": "app.jar", "webApp": "candidate-web",
                                       "applicationResources": "webapp"]
        var catalog = base; catalog["runtimeCandidates"] = ["next": candidate]
        let configured = try load(catalog)
        precondition(configured.runtimeCandidates["next"]?.identifier == "next")
        precondition(configured.runtimeCandidates["next"]?.webAppURL == candidateWeb.resolvingSymlinksInPath())
        precondition(configured.runtimeCandidates["next"]?.sidecarJarURL == root.appendingPathComponent("app.jar").resolvingSymlinksInPath())
        precondition(configured.webAppURL == root.appendingPathComponent("webapp").resolvingSymlinksInPath())
        cases += 1
        try Data("second inert JAR".utf8).write(to: root.appendingPathComponent("second.jar"))
        let secondWeb = root.appendingPathComponent("second-web")
        try fm.createDirectory(at: secondWeb, withIntermediateDirectories: false)
        try Data("second inert HTML".utf8).write(to: secondWeb.appendingPathComponent("index.html"))
        var two = base
        two["runtimeCandidates"] = ["first": candidate,
            "second": ["sidecarJar": "second.jar", "webApp": "second-web"]]
        let paired = try load(two)
        precondition(paired.runtimeCandidates.count == 2)
        precondition(paired.runtimeCandidates["second"]?.sidecarJarURL == root.appendingPathComponent("second.jar").resolvingSymlinksInPath())
        precondition(paired.runtimeCandidates["second"]?.webAppURL == secondWeb.resolvingSymlinksInPath())
        precondition(paired.runtimeCandidates["second"]?.applicationResourcesURL == nil)
        precondition(paired.runtimeCandidates["first"]?.applicationResourcesURL != nil)
        cases += 1
        let firstSelection = try paired.configuration(forRuntimeCandidate: "first")
        let secondSelection = try firstSelection.configuration(forRuntimeCandidate: "second")
        precondition(firstSelection.sidecarJarURL == paired.runtimeCandidates["first"]?.sidecarJarURL)
        precondition(firstSelection.applicationResourcesURL == paired.runtimeCandidates["first"]?.applicationResourcesURL)
        precondition(secondSelection.sidecarJarURL == paired.runtimeCandidates["second"]?.sidecarJarURL)
        precondition(secondSelection.webAppURL == paired.runtimeCandidates["second"]?.webAppURL)
        if case .packaged(let selectedWeb) = secondSelection.flowWebAppSource {
            precondition(selectedWeb == secondSelection.webAppURL)
        } else { fatalError("selected frontend not packaged") }
        precondition(secondSelection.applicationResourcesURL == nil)
        precondition(secondSelection.opaqueBridgeURL == paired.opaqueBridgeURL)
        precondition(secondSelection.pushBridgeURL == paired.pushBridgeURL)
        precondition(secondSelection.capabilityManifest.commandsByWindow == paired.capabilityManifest.commandsByWindow)
        precondition(secondSelection.capabilityManifest.permissionMode == paired.capabilityManifest.permissionMode)
        precondition(secondSelection.sidecarInitialReadyTimeoutSeconds == paired.sidecarInitialReadyTimeoutSeconds)
        precondition(secondSelection.flowIpcSidecarRestartLimit == paired.flowIpcSidecarRestartLimit)
        precondition(secondSelection.flowIpcSidecarRecoveryReadyTimeoutSeconds == paired.flowIpcSidecarRecoveryReadyTimeoutSeconds)
        precondition(secondSelection.flowIpcSidecarRecoveryStableRunSeconds == paired.flowIpcSidecarRecoveryStableRunSeconds)
        precondition(paired.sidecarJarURL == initial.sidecarJarURL)
        let selectedPlan = JavaSidecarLaunchPlan.make(config: secondSelection, inheritedEnvironment: declaredPlan.environment)
        precondition(selectedPlan.arguments == ["-jar", secondSelection.sidecarJarURL.path])
        precondition(selectedPlan.commandSummary.hasSuffix(" -jar $APP/Contents/Resources/second.jar"))
        precondition(selectedPlan.jarDisplayPath == "$APP/Contents/Resources/second.jar")
        precondition(!selectedPlan.commandSummary.contains(root.path))
        precondition(selectedPlan.environment["VATORI_APPLICATION_RESOURCES"] == nil)
        cases += 1
        func rejectsSelection(_ configuration: HostConfig, _ identifier: String) throws {
            do { _ = try configuration.configuration(forRuntimeCandidate: identifier); fatalError("accepted invalid candidate selection") }
            catch is HostConfigError { cases += 1 }
            precondition(paired.sidecarJarURL == initial.sidecarJarURL)
        }
        for identifier in ["", "unknown", "../first", "/first"] {
            try rejectsSelection(paired, identifier)
        }
        try rejectsSelection(try load(compatibility), "first")
        var development = base; development.removeValue(forKey: "webApp")
        development["webAppDevelopmentServer"] = ["origin": "http://127.0.0.1:1234", "entryPath": "/"]
        try rejectsSelection(try load(development), "first")
        // A changed manifest cannot introduce a candidate into the captured catalog.
        var changed = two; changed["runtimeCandidates"] = ["injected": candidate]
        _ = try load(changed)
        try rejectsSelection(paired, "injected")
        _ = try paired.configuration(forRuntimeCandidate: "first"); cases += 1
        let capabilityFile = root.appendingPathComponent("capabilities.json")
        let originalCapabilities = try Data(contentsOf: capabilityFile)
        try Data(#"{"schemaVersion":1,"permissionMode":"allow-all-dev","windows":[]}"#.utf8).write(to: capabilityFile)
        let unchangedPolicy = try paired.configuration(forRuntimeCandidate: "second")
        precondition(unchangedPolicy.capabilityManifest.permissionMode == "manifest-enforced")
        try originalCapabilities.write(to: capabilityFile)
        cases += 1
        // Mutations after startup must fail preflight, before any lifecycle action.
        let secondJar = root.appendingPathComponent("second.jar")
        try fm.removeItem(at: secondJar)
        try rejectsSelection(paired, "second")
        try fm.createDirectory(at: secondJar, withIntermediateDirectories: false)
        try rejectsSelection(paired, "second")
        try fm.removeItem(at: secondJar)
        try fm.createSymbolicLink(atPath: secondJar.path, withDestinationPath: "/dev/null")
        try rejectsSelection(paired, "second")
        try fm.removeItem(at: secondJar)
        try fm.createSymbolicLink(atPath: secondJar.path, withDestinationPath: "app.jar")
        try rejectsSelection(paired, "second")
        try fm.removeItem(at: secondJar)
        try Data("second inert JAR".utf8).write(to: secondJar)
        let secondEntry = secondWeb.appendingPathComponent("index.html")
        try fm.removeItem(at: secondEntry)
        try rejectsSelection(paired, "second")
        precondition(mkfifo(secondEntry.path, 0o600) == 0)
        try rejectsSelection(paired, "second")
        try fm.removeItem(at: secondEntry)
        try Data("second inert HTML".utf8).write(to: secondEntry)
        // Optional resources must be rechecked too, without affecting siblings.
        try fm.removeItem(at: root.appendingPathComponent("webapp"))
        try rejectsSelection(paired, "first")
        _ = try paired.configuration(forRuntimeCandidate: "second"); cases += 1
        try fm.createDirectory(at: root.appendingPathComponent("webapp"), withIntermediateDirectories: false)
        func rejectsCatalog(_ value: [String: Any], _ matches: (Error) -> Bool) throws {
            do { _ = try load(value); fatalError("accepted invalid candidate catalog") }
            catch { precondition(matches(error), "unexpected rejection: \(error)"); cases += 1 }
        }
        for identifier in ["", "bad/id", "bad id", String(repeating: "x", count: 129)] {
            var invalid = base; invalid["runtimeCandidates"] = [identifier: candidate]
            try rejectsCatalog(invalid) { if case HostConfigError.invalidRuntimeCandidates = $0 { return true }; return false }
        }
        for (field, path) in [("sidecarJar", "webapp"), ("webApp", "app.jar"),
                              ("applicationResources", "app.jar")] {
            var badCandidate = candidate; badCandidate[field] = path
            var invalid = base; invalid["runtimeCandidates"] = ["next": badCandidate]
            try rejectsCatalog(invalid) {
                if case HostConfigError.resourceNotRegularFile = $0 { return field == "sidecarJar" }
                if case HostConfigError.resourceNotDirectory = $0 { return field != "sidecarJar" }
                return false
            }
        }
        var escapingCandidate = candidate; escapingCandidate["sidecarJar"] = "../outside.jar"
        var invalid = base; invalid["runtimeCandidates"] = ["next": escapingCandidate]
        try rejectsCatalog(invalid) { if case HostConfigError.invalidResourcePath = $0 { return true }; return false }
        var missingEntry = candidate; missingEntry["webApp"] = "webapp"
        invalid = base; invalid["runtimeCandidates"] = ["next": missingEntry]
        try rejectsCatalog(invalid) { if case HostConfigError.missingResource = $0 { return true }; return false }
        invalid = catalog; invalid["transport"] = "compatibility-http"; invalid.removeValue(forKey: "webApp")
        try rejectsCatalog(invalid) { if case HostConfigError.invalidRuntimeCandidates = $0 { return true }; return false }
        invalid = catalog; invalid.removeValue(forKey: "webApp")
        invalid["webAppDevelopmentServer"] = ["origin": "http://127.0.0.1:1234", "entryPath": "/"]
        try rejectsCatalog(invalid) { if case HostConfigError.invalidRuntimeCandidates = $0 { return true }; return false }
        invalid = base; invalid["runtimeCandidates"] = ["next": ["sidecarJar": "app.jar"]]
        try rejectsCatalog(invalid) { if case HostConfigError.manifestUnreadable = $0 { return true }; return false }
        try fm.removeItem(at: candidateWeb.appendingPathComponent("index.html"))
        try fm.createSymbolicLink(atPath: candidateWeb.appendingPathComponent("index.html").path,
            withDestinationPath: "../opaque.js")
        try rejectsCatalog(catalog) { if case HostConfigError.invalidResourcePath = $0 { return true }; return false }
        try fm.removeItem(at: candidateWeb.appendingPathComponent("index.html"))
        precondition(mkfifo(candidateWeb.appendingPathComponent("index.html").path, 0o600) == 0)
        try rejectsCatalog(catalog) { if case HostConfigError.resourceNotRegularFile = $0 { return true }; return false }
        try fm.removeItem(at: candidateWeb.appendingPathComponent("index.html"))
        try Data("inert HTML".utf8).write(to: candidateWeb.appendingPathComponent("index.html"))
        for field in ["sidecarJar", "opaqueBridge", "pushBridge"] {
            var bad = base
            bad[field] = "webapp"
            do { _ = try load(bad); fatalError("accepted directory for \(field)") }
            catch HostConfigError.resourceNotRegularFile { cases += 1 }
        }
        var bad = base
        bad["webApp"] = "app.jar"
        do { _ = try load(bad); fatalError("accepted file as webapp") }
        catch HostConfigError.resourceNotDirectory { cases += 1 }
        let fifo = root.appendingPathComponent("pipe")
        precondition(mkfifo(fifo.path, 0o600) == 0)
        bad = base; bad["sidecarJar"] = "pipe"
        do { _ = try load(bad); fatalError("accepted FIFO as sidecar") }
        catch HostConfigError.resourceNotRegularFile { cases += 1 }
        // Internal links retain the existing relocation-safe path policy.
        try fm.createSymbolicLink(atPath: root.appendingPathComponent("linked.jar").path, withDestinationPath: "app.jar")
        var linked = base; linked["sidecarJar"] = "linked.jar"
        _ = try load(linked); cases += 1
        bad = base; bad["sidecarJar"] = "../outside.jar"
        do { _ = try load(bad); fatalError("accepted escaping path") }
        catch HostConfigError.invalidResourcePath { cases += 1 }
        bad = base; bad["capabilityManifest"] = "webapp"
        do { _ = try load(bad); fatalError("accepted capability directory") }
        catch HostConfigError.invalidCapabilityManifest(let invalid) {
            precondition(invalid.parseError == "manifest-not-regular-file"); cases += 1
        }
        bad = base; bad["capabilityManifest"] = "missing.json"
        do { _ = try load(bad); fatalError("accepted missing capabilities") }
        catch HostConfigError.invalidCapabilityManifest(let invalid) {
            precondition(invalid.parseError == "manifest-missing"); cases += 1
        }
        try fm.createSymbolicLink(atPath: root.appendingPathComponent("linked-capabilities.json").path,
            withDestinationPath: "capabilities.json")
        linked = base; linked["capabilityManifest"] = "linked-capabilities.json"
        _ = try load(linked); cases += 1
        // Each potentially blocking read is bounded externally, including when
        // run against a regressed loader. Never hang the entire smoke suite.
        for scenario in ["launch-fifo", "capability-fifo"] {
            let child = Process()
            child.executableURL = URL(fileURLWithPath: CommandLine.arguments[0])
            child.arguments = [scenario]
            try child.run()
            let deadline = ProcessInfo.processInfo.systemUptime + 3
            while child.isRunning && ProcessInfo.processInfo.systemUptime < deadline {
                Thread.sleep(forTimeInterval: 0.01)
            }
            if child.isRunning {
                kill(child.processIdentifier, SIGKILL)
                child.waitUntilExit()
                fatalError("manifest read blocked: \(scenario)")
            }
            child.waitUntilExit()
            precondition(child.terminationStatus == 0, "manifest rejection child failed")
            cases += 1
        }
        let launch = root.appendingPathComponent("VatoriHost.json")
        try fm.removeItem(at: launch)
        try fm.createDirectory(at: launch, withIntermediateDirectories: false)
        do { _ = try HostConfig.load(resourceURL: root, environment: [:]); fatalError("accepted manifest directory") }
        catch HostConfigError.resourceNotRegularFile { cases += 1 }
        try fm.removeItem(at: launch)
        try JSONSerialization.data(withJSONObject: base).write(to: root.appendingPathComponent("internal.json"))
        try fm.createSymbolicLink(atPath: launch.path, withDestinationPath: "internal.json")
        _ = try HostConfig.load(resourceURL: root, environment: [:]); cases += 1
        try fm.removeItem(at: launch)
        try fm.createSymbolicLink(atPath: launch.path, withDestinationPath: "/dev/null")
        do { _ = try HostConfig.load(resourceURL: root, environment: [:]); fatalError("accepted escaping manifest") }
        catch HostConfigError.invalidResourcePath { cases += 1 }
        print("Host config resource smoke passed: \(cases) cases (no JVM or window)")
    }
}
