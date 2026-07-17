import Foundation

enum HostTransport: String, Decodable {
    case flowIpc = "flow-ipc"
    case compatibilityHttp = "compatibility-http"
}

private struct HostLaunchManifest: Decodable {
    struct Recovery: Decodable {
        let restartLimit: Int
        let readyTimeoutSeconds: Double
    }

    let version: Int
    let transport: HostTransport
    let sidecarJar: String
    let webApp: String?
    let opaqueBridge: String
    let capabilityManifest: String
    let recovery: Recovery
}

enum HostConfigError: LocalizedError {
    case manifestMissing
    case manifestUnreadable(String)
    case unsupportedManifestVersion(Int)
    case invalidResourcePath(String)
    case missingResource(String)
    case invalidRecovery
    case flowBundleMissing

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
        case .invalidRecovery:
            return "Host manifest recovery values must be non-negative and the timeout must be at least one second"
        case .flowBundleMissing:
            return "flow-ipc requires a packaged FlowWebApp resource"
        }
    }
}

struct HostConfig {
    let runtimeObserver: any HostRuntimeObserver
    let transport: HostTransport
    let sidecarJarURL: URL
    let webAppURL: URL?
    let opaqueBridgeURL: URL
    let capabilityManifestURL: URL
    let flowIpcSidecarRestartLimit: Int
    let flowIpcSidecarRecoveryReadyTimeoutSeconds: Double

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
        guard manifest.recovery.restartLimit >= 0,
              manifest.recovery.readyTimeoutSeconds >= 1 else {
            throw HostConfigError.invalidRecovery
        }

        let sidecarJarURL = try resource(
            manifest.sidecarJar, beneath: resourceURL, mustExist: true)
        let opaqueBridgeURL = try resource(
            manifest.opaqueBridge, beneath: resourceURL, mustExist: true)
        let capabilityManifestURL = try resource(
            manifest.capabilityManifest, beneath: resourceURL, mustExist: true)
        let webAppURL = try manifest.webApp.map {
            try resource($0, beneath: resourceURL, mustExist: true)
        }
        if manifest.transport == .flowIpc && webAppURL == nil {
            throw HostConfigError.flowBundleMissing
        }

        let observer = makeHostRuntimeObserver()
        observer.configure(environment: environment)
        return HostConfig(
            runtimeObserver: observer,
            transport: manifest.transport,
            sidecarJarURL: sidecarJarURL,
            webAppURL: webAppURL,
            opaqueBridgeURL: opaqueBridgeURL,
            capabilityManifestURL: capabilityManifestURL,
            flowIpcSidecarRestartLimit: manifest.recovery.restartLimit,
            flowIpcSidecarRecoveryReadyTimeoutSeconds: manifest.recovery.readyTimeoutSeconds)
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
}
