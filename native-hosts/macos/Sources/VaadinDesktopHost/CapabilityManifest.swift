import Foundation

private struct CapabilityManifestDocument: Decodable {
    struct Window: Decodable {
        let label: String
        let commands: [String]
    }

    let schemaVersion: Int
    let permissionMode: String
    let windows: [Window]
}

struct CapabilityManifest {
    let path: String
    let loaded: Bool
    let permissionMode: String
    let commandsByWindow: [String: Set<String>]
    let parseError: String

    static func load(from url: URL) -> CapabilityManifest {
        let path = "$APP/Contents/Resources/\(url.lastPathComponent)"
        guard FileManager.default.fileExists(atPath: url.path) else {
            return invalid(path: path, loaded: false, error: "manifest-missing")
        }

        let data: Data
        do {
            let attributes = try FileManager.default.attributesOfItem(
                atPath: url.resolvingSymlinksInPath().path)
            guard attributes[.type] as? FileAttributeType == .typeRegular else {
                return invalid(path: path, loaded: true, error: "manifest-not-regular-file")
            }
            data = try Data(contentsOf: url)
        } catch {
            return invalid(
                path: path, loaded: true,
                error: "manifest-unreadable")
        }

        let document: CapabilityManifestDocument
        do {
            document = try JSONDecoder().decode(
                CapabilityManifestDocument.self,
                from: data)
        } catch {
            return invalid(
                path: path, loaded: true,
                error: "invalid-json-document")
        }

        guard document.schemaVersion == 1 else {
            return invalid(
                path: path, loaded: true,
                error: "unsupported-schema-version-\(document.schemaVersion)")
        }
        let modes = Set([
            "allow-all-dev",
            "manifest-enforced",
            "trace-only-dry-run"
        ])
        guard modes.contains(document.permissionMode) else {
            return invalid(
                path: path, loaded: true,
                error: "unsupported-permission-mode")
        }
        var commandsByWindow: [String: Set<String>] = [:]
        for window in document.windows {
            guard isValidSidecarIdentifier(window.label),
                  commandsByWindow[window.label] == nil,
                  window.commands.allSatisfy(isValidSidecarIdentifier) else {
                return invalid(
                    path: path, loaded: true,
                    error: "invalid-window-capability-entry")
            }
            commandsByWindow[window.label] = Set(window.commands)
        }
        return CapabilityManifest(
            path: path,
            loaded: true,
            permissionMode: document.permissionMode,
            commandsByWindow: commandsByWindow,
            parseError: "")
    }

    private static func invalid(
        path: String,
        loaded: Bool,
        error: String
    ) -> CapabilityManifest {
        CapabilityManifest(
            path: path,
            loaded: loaded,
            permissionMode: "invalid",
            commandsByWindow: [:],
            parseError: error)
    }

    func allows(_ command: String, windowId: String) -> Bool {
        if !parseError.isEmpty {
            return false
        }
        if permissionMode == "trace-only-dry-run" || permissionMode == "allow-all-dev" {
            return true
        }
        return permissionMode == "manifest-enforced"
            && commandsByWindow[windowId, default: []].contains(command)
    }

    func denialReason(_ command: String, windowId: String) -> String {
        if !parseError.isEmpty {
            return loaded ? "manifest-invalid" : "manifest-missing"
        }
        if !commandsByWindow[windowId, default: []].contains(command) {
            return "manifest-denied"
        }
        return "unsupported-command"
    }

    func allowedReason(_ command: String, windowId: String) -> String {
        if permissionMode == "trace-only-dry-run" {
            if !commandsByWindow[windowId, default: []].contains(command) {
                return "dry-run-would-deny"
            }
            return "dry-run-manifest-allow"
        }
        return permissionMode == "allow-all-dev"
            ? "development-default"
            : "manifest-allow"
    }

    func tracePayload() -> [String: Any] {
        [
            "path": path,
            "loaded": loaded,
            "permissionMode": permissionMode,
            "commandsByWindow": commandsByWindow.mapValues { $0.sorted() },
            "parseError": parseError
        ]
    }
}
