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
    case invalidSidecarStartup
    case invalidRecovery
    case flowWebAppMissing
    case conflictingFlowWebAppSources
    case invalidDevelopmentWebApp(String)

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
        }
    }
}

struct HostConfig {
    let runtimeObserver: any HostRuntimeObserver
    let transport: HostTransport
    let sidecarJarURL: URL
    let webAppURL: URL?
    let flowWebAppSource: FlowWebAppSource?
    let opaqueBridgeURL: URL
    let pushBridgeURL: URL
    let capabilityManifestURL: URL
    let applicationResourcesURL: URL?
    let sidecarInitialReadyTimeoutSeconds: Double
    let flowIpcSidecarRestartLimit: Int
    let flowIpcSidecarRecoveryReadyTimeoutSeconds: Double
    let flowIpcSidecarRecoveryStableRunSeconds: Double

    var hostNoActivate: Bool {
        runtimeObserver.hostNoActivate
    }

    var flowIpcSidecarRecoveryEnabled: Bool {
        transport == .flowIpc && flowIpcSidecarRestartLimit > 0
    }

    static func load(
        resourceURL: URL? = Bundle.main.resourceURL,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) throws -> HostConfig {
        guard let resourceURL else {
            throw HostConfigError.manifestMissing
        }
        let manifestURL = resourceURL.appendingPathComponent("VatoriHost.json")
        guard FileManager.default.fileExists(atPath: manifestURL.path) else {
            throw HostConfigError.manifestMissing
        }

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

        let sidecarJarURL = try resource(
            manifest.sidecarJar, beneath: resourceURL, mustExist: true)
        let opaqueBridgeURL = try resource(
            manifest.opaqueBridge, beneath: resourceURL, mustExist: true)
        let pushBridgeURL = try resource(
            manifest.pushBridge, beneath: resourceURL, mustExist: true)
        let capabilityManifestURL = try resource(
            manifest.capabilityManifest, beneath: resourceURL, mustExist: true)
        let applicationResourcesURL = try manifest.applicationResources.map {
            try resourceDirectory($0, beneath: resourceURL)
        }
        let webAppURL = try manifest.webApp.map {
            try resource($0, beneath: resourceURL, mustExist: true)
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

        let observer = makeHostRuntimeObserver()
        observer.configure(environment: environment)
        return HostConfig(
            runtimeObserver: observer,
            transport: manifest.transport,
            sidecarJarURL: sidecarJarURL,
            webAppURL: webAppURL,
            flowWebAppSource: flowWebAppSource,
            opaqueBridgeURL: opaqueBridgeURL,
            pushBridgeURL: pushBridgeURL,
            capabilityManifestURL: capabilityManifestURL,
            applicationResourcesURL: applicationResourcesURL,
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
