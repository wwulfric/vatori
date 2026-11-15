import Foundation

enum HostTransport: String, Decodable {
    case flowIpc = "flow-ipc"
    case compatibilityHttp = "compatibility-http"
}

private struct HostLaunchManifest: Decodable {
    struct SidecarStartup: Decodable {
        let readyTimeoutSeconds: Double
    }

    struct Recovery: Decodable {
        let restartLimit: Int
        let readyTimeoutSeconds: Double
        let stableRunSeconds: Double?
    }

    struct WebAppDevelopmentServer: Decodable {
        let origin: String
        let entryPath: String
    }

    struct RuntimeCandidate: Decodable {
        let sidecarJar: String
        let webApp: String
        let applicationResources: String?
    }

    let version: Int
    let transport: HostTransport
    let sidecarJar: String
    let webApp: String?
    let webAppDevelopmentServer: WebAppDevelopmentServer?
    let applicationResources: String?
    let opaqueBridge: String
    let pushBridge: String
    let capabilityManifest: String
    let sidecarStartup: SidecarStartup?
    let recovery: Recovery
    let runtimeCandidates: [String: RuntimeCandidate]?
}

// Host-owned, bundle-relative asset selection. This is not an authorization
// token, a browser-provided path, or proof that a candidate can reach ready.
struct HostRuntimeCandidate {
    let identifier: String
    let sidecarJarURL: URL
    let webAppURL: URL
    let applicationResourcesURL: URL?
    fileprivate let sidecarJarPath: String
    fileprivate let webAppPath: String
    fileprivate let applicationResourcesPath: String?
}

struct FlowWebAppDevelopmentServer {
    let originURL: URL
    let entryPath: String
}

enum FlowWebAppSource {
    case packaged(URL)
    case development(FlowWebAppDevelopmentServer)

    var isDevelopment: Bool {
        if case .development = self {
            return true
        }
        return false
    }

    var mode: String {
        switch self {
        case .packaged:
            return "production-bundle"
        case .development:
            return "development-server"
        }
    }
}

enum HostConfigError: LocalizedError {
    case manifestMissing
    case manifestUnreadable(String)
    case unsupportedManifestVersion(Int)
    case invalidResourcePath(String)
    case missingResource(String)
    case resourceNotDirectory(String)
    case resourceNotRegularFile(String)
    case invalidCapabilityManifest(CapabilityManifest)
    case invalidSidecarStartup
    case invalidRecovery
    case flowWebAppMissing
    case conflictingFlowWebAppSources
    case invalidDevelopmentWebApp(String)
    case invalidRuntimeCandidates(String)

    var errorDescription: String? {
        switch self {
        case .manifestMissing:
            return "VatoriHost.json is missing from the application bundle"
        case .manifestUnreadable(let message):
            return "VatoriHost.json is invalid: \(message)"
        case .unsupportedManifestVersion(let version):
            return "Unsupported Vatori host manifest version: \(version)"
        case .invalidResourcePath(let path):
            return "Host manifest resource paths must be bundle-relative and cannot contain '..': \(path)"
        case .missingResource(let path):
            return "Host manifest resource is missing: \(path)"
        case .resourceNotDirectory(let path):
            return "Host manifest resource must be a directory: \(path)"
        case .resourceNotRegularFile(let path):
            return "Host manifest resource must be a regular file: \(path)"
        case .invalidCapabilityManifest(let manifest):
            return "desktop-capabilities.json is invalid: \(manifest.parseError)"
        case .invalidSidecarStartup:
            return "Host manifest sidecar startup timeout must be at least one second"
        case .invalidRecovery:
            return "Host manifest recovery values must be non-negative and recovery timeouts must be at least one second"
        case .flowWebAppMissing:
            return "flow-ipc requires a packaged web app or a development web app server"
        case .conflictingFlowWebAppSources:
            return "flow-ipc accepts exactly one web app source"
        case .invalidDevelopmentWebApp(let message):
            return "Development web app server is invalid: \(message)"
        case .invalidRuntimeCandidates(let message):
            return "Runtime candidates are invalid: \(message)"
        }
    }
}

struct HostConfig {
    private let resourceRootURL: URL
    let runtimeObserver: any HostRuntimeObserver
    let transport: HostTransport
    let sidecarJarURL: URL
    let webAppURL: URL?
    let flowWebAppSource: FlowWebAppSource?
    let opaqueBridgeURL: URL
    let pushBridgeURL: URL
    let capabilityManifest: CapabilityManifest
    let applicationResourcesURL: URL?
    let runtimeCandidates: [String: HostRuntimeCandidate]
    let sidecarInitialReadyTimeoutSeconds: Double
    let flowIpcSidecarRestartLimit: Int
    let flowIpcSidecarRecoveryReadyTimeoutSeconds: Double
    let flowIpcSidecarRecoveryStableRunSeconds: Double

    var hostNoActivate: Bool {
        runtimeObserver.hostNoActivate
    }

    var sidecarJarDisplayPath: String {
        let prefix = resourceRootURL.path.hasSuffix("/") ? resourceRootURL.path : resourceRootURL.path + "/"
        return "$APP/Contents/Resources/" + String(sidecarJarURL.path.dropFirst(prefix.count))
    }

    var flowIpcSidecarRecoveryEnabled: Bool {
        transport == .flowIpc && flowIpcSidecarRestartLimit > 0
    }

    // Preflight only: the lifecycle owner must authorize and retire the old
    // process before using this configuration. Never reread a mutable manifest
    // to acquire new candidates or capability permissions.
    func configuration(forRuntimeCandidate identifier: String) throws -> HostConfig {
        guard transport == .flowIpc,
              let source = flowWebAppSource, case .packaged = source,
              isValidSidecarIdentifier(identifier),
              let candidate = runtimeCandidates[identifier] else {
            throw HostConfigError.invalidRuntimeCandidates("unknown or unavailable candidate")
        }
        let jar = try Self.resourceFile(candidate.sidecarJarPath, beneath: resourceRootURL)
        let webApp = try Self.resourceDirectory(candidate.webAppPath, beneath: resourceRootURL)
        _ = try Self.resourceFile("index.html", beneath: webApp)
        let resources = try candidate.applicationResourcesPath.map {
            try Self.resourceDirectory($0, beneath: resourceRootURL)
        }
        guard jar == candidate.sidecarJarURL, webApp == candidate.webAppURL,
              resources == candidate.applicationResourcesURL else {
            throw HostConfigError.invalidRuntimeCandidates("candidate asset identity changed")
        }
        return HostConfig(
            resourceRootURL: resourceRootURL,
            runtimeObserver: runtimeObserver,
            transport: transport,
            sidecarJarURL: jar,
            webAppURL: webApp,
            flowWebAppSource: .packaged(webApp),
            opaqueBridgeURL: opaqueBridgeURL,
            pushBridgeURL: pushBridgeURL,
            capabilityManifest: capabilityManifest,
            applicationResourcesURL: resources,
            runtimeCandidates: runtimeCandidates,
            sidecarInitialReadyTimeoutSeconds: sidecarInitialReadyTimeoutSeconds,
            flowIpcSidecarRestartLimit: flowIpcSidecarRestartLimit,
            flowIpcSidecarRecoveryReadyTimeoutSeconds: flowIpcSidecarRecoveryReadyTimeoutSeconds,
            flowIpcSidecarRecoveryStableRunSeconds: flowIpcSidecarRecoveryStableRunSeconds)
    }

    static func load(
        resourceURL: URL? = Bundle.main.resourceURL,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) throws -> HostConfig {
        guard let resourceURL else {
            throw HostConfigError.manifestMissing
        }
        var manifestURL = resourceURL.appendingPathComponent("VatoriHost.json")
        guard FileManager.default.fileExists(atPath: manifestURL.path) else {
            throw HostConfigError.manifestMissing
        }
        // Validate shape/containment before reading: special files can block a
        // synchronous manifest read before lifecycle deadlines are installed.
        manifestURL = try resourceFile("VatoriHost.json", beneath: resourceURL)

        let manifest: HostLaunchManifest
        do {
            manifest = try JSONDecoder().decode(
                HostLaunchManifest.self,
                from: Data(contentsOf: manifestURL))
        } catch {
            throw HostConfigError.manifestUnreadable(error.localizedDescription)
        }
        guard manifest.version == 1 else {
            throw HostConfigError.unsupportedManifestVersion(manifest.version)
        }
        let sidecarInitialReadyTimeoutSeconds =
            manifest.sidecarStartup?.readyTimeoutSeconds ?? 30
        let recoveryStableRunSeconds =
            manifest.recovery.stableRunSeconds ?? 30
        guard sidecarInitialReadyTimeoutSeconds >= 1 else {
            throw HostConfigError.invalidSidecarStartup
        }
        guard manifest.recovery.restartLimit >= 0,
              manifest.recovery.readyTimeoutSeconds >= 1,
              recoveryStableRunSeconds >= 1 else {
            throw HostConfigError.invalidRecovery
        }

        let sidecarJarURL = try resourceFile(manifest.sidecarJar, beneath: resourceURL)
        let opaqueBridgeURL = try resourceFile(manifest.opaqueBridge, beneath: resourceURL)
        let pushBridgeURL = try resourceFile(manifest.pushBridge, beneath: resourceURL)
        let capabilityManifestURL = try resource(
            manifest.capabilityManifest, beneath: resourceURL, mustExist: false)
        let capabilityManifest = CapabilityManifest.load(
            from: capabilityManifestURL)
        guard capabilityManifest.parseError.isEmpty else {
            throw HostConfigError.invalidCapabilityManifest(
                capabilityManifest)
        }
        let applicationResourcesURL = try manifest.applicationResources.map {
            try resourceDirectory($0, beneath: resourceURL)
        }
        let webAppURL = try manifest.webApp.map {
            try resourceDirectory($0, beneath: resourceURL)
        }
        let developmentServer = try manifest.webAppDevelopmentServer.map {
            try developmentWebApp($0)
        }
        let flowWebAppSource: FlowWebAppSource?
        if manifest.transport == .flowIpc {
            if webAppURL != nil && developmentServer != nil {
                throw HostConfigError.conflictingFlowWebAppSources
            }
            if let webAppURL {
                flowWebAppSource = .packaged(webAppURL)
            } else if let developmentServer {
                flowWebAppSource = .development(developmentServer)
            } else {
                throw HostConfigError.flowWebAppMissing
            }
        } else {
            if webAppURL != nil || developmentServer != nil {
                throw HostConfigError.conflictingFlowWebAppSources
            }
            flowWebAppSource = nil
        }

        var runtimeCandidates: [String: HostRuntimeCandidate] = [:]
        if let candidates = manifest.runtimeCandidates, !candidates.isEmpty {
            guard manifest.transport == .flowIpc,
                  let source = flowWebAppSource, case .packaged = source else {
                throw HostConfigError.invalidRuntimeCandidates("requires packaged flow-ipc")
            }
            for identifier in candidates.keys.sorted() {
                guard isValidSidecarIdentifier(identifier), let candidate = candidates[identifier] else {
                    throw HostConfigError.invalidRuntimeCandidates("invalid candidate identifier")
                }
                let jar = try resourceFile(candidate.sidecarJar, beneath: resourceURL)
                let webApp = try resourceDirectory(candidate.webApp, beneath: resourceURL)
                _ = try resourceFile("index.html", beneath: webApp)
                let resources = try candidate.applicationResources.map {
                    try resourceDirectory($0, beneath: resourceURL)
                }
                runtimeCandidates[identifier] = HostRuntimeCandidate(
                    identifier: identifier, sidecarJarURL: jar,
                    webAppURL: webApp, applicationResourcesURL: resources,
                    sidecarJarPath: candidate.sidecarJar, webAppPath: candidate.webApp,
                    applicationResourcesPath: candidate.applicationResources)
            }
        }

        let observer = makeHostRuntimeObserver()
        observer.configure(environment: environment)
        return HostConfig(
            resourceRootURL: resourceURL.standardizedFileURL.resolvingSymlinksInPath(),
            runtimeObserver: observer,
            transport: manifest.transport,
            sidecarJarURL: sidecarJarURL,
            webAppURL: webAppURL,
            flowWebAppSource: flowWebAppSource,
            opaqueBridgeURL: opaqueBridgeURL,
            pushBridgeURL: pushBridgeURL,
            capabilityManifest: capabilityManifest,
            applicationResourcesURL: applicationResourcesURL,
            runtimeCandidates: runtimeCandidates,
            sidecarInitialReadyTimeoutSeconds:
                sidecarInitialReadyTimeoutSeconds,
            flowIpcSidecarRestartLimit: manifest.recovery.restartLimit,
            flowIpcSidecarRecoveryReadyTimeoutSeconds:
                manifest.recovery.readyTimeoutSeconds,
            flowIpcSidecarRecoveryStableRunSeconds:
                recoveryStableRunSeconds)
    }

    private static func developmentWebApp(
        _ manifest: HostLaunchManifest.WebAppDevelopmentServer
    ) throws -> FlowWebAppDevelopmentServer {
        guard let origin = URL(string: manifest.origin),
              origin.scheme == "http",
              origin.host == "127.0.0.1",
              origin.port != nil,
              origin.user == nil,
              origin.password == nil,
              origin.query == nil,
              origin.fragment == nil,
              origin.path.isEmpty || origin.path == "/" else {
            throw HostConfigError.invalidDevelopmentWebApp(
                "origin must be an explicit 127.0.0.1 HTTP origin")
        }
        let path = manifest.entryPath
        let decodedPath = path.removingPercentEncoding ?? path
        let segments = decodedPath.split(
            separator: "/", omittingEmptySubsequences: false)
        guard path.hasPrefix("/"),
              !path.hasPrefix("//"),
              !path.contains("?"),
              !path.contains("#"),
              !path.contains("\\"),
              !segments.contains(where: { $0 == "." || $0 == ".." }) else {
            throw HostConfigError.invalidDevelopmentWebApp(
                "entryPath must be an absolute path without traversal")
        }
        return FlowWebAppDevelopmentServer(
            originURL: origin,
            entryPath: path)
    }

    private static func resource(
        _ path: String,
        beneath root: URL,
        mustExist: Bool
    ) throws -> URL {
        let parts = path.split(separator: "/", omittingEmptySubsequences: false)
        guard !path.isEmpty,
              !path.hasPrefix("/"),
              !path.hasPrefix("~"),
              !parts.contains(where: { $0.isEmpty || $0 == "." || $0 == ".." }) else {
            throw HostConfigError.invalidResourcePath(path)
        }
        let normalizedRoot = root.standardizedFileURL.resolvingSymlinksInPath()
        let candidate = parts.reduce(normalizedRoot) { partial, part in
            partial.appendingPathComponent(String(part))
        }.standardizedFileURL.resolvingSymlinksInPath()
        let rootPrefix = normalizedRoot.path.hasSuffix("/")
            ? normalizedRoot.path
            : normalizedRoot.path + "/"
        guard candidate.path.hasPrefix(rootPrefix) else {
            throw HostConfigError.invalidResourcePath(path)
        }
        if mustExist && !FileManager.default.fileExists(atPath: candidate.path) {
            throw HostConfigError.missingResource(path)
        }
        return candidate
    }

    private static func resourceFile(_ path: String, beneath root: URL) throws -> URL {
        let candidate = try resource(path, beneath: root, mustExist: true)
        let attributes = try FileManager.default.attributesOfItem(atPath: candidate.path)
        guard attributes[.type] as? FileAttributeType == .typeRegular else {
            throw HostConfigError.resourceNotRegularFile(path)
        }
        return candidate
    }

    private static func resourceDirectory(
        _ path: String,
        beneath root: URL
    ) throws -> URL {
        let candidate = try resource(path, beneath: root, mustExist: true)
        var isDirectory: ObjCBool = false
        guard FileManager.default.fileExists(
            atPath: candidate.path,
            isDirectory: &isDirectory),
              isDirectory.boolValue else {
            throw HostConfigError.resourceNotDirectory(path)
        }
        return candidate
    }
}
