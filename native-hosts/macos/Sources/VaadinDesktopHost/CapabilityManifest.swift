import Foundation

struct CapabilityManifest {
    let path: String
    let loaded: Bool
    let permissionMode: String
    let allowedCommands: Set<String>
    let parseError: String

    static func load(module: String) -> CapabilityManifest {
        let path = "\(module)/desktop-capabilities.json"
        guard FileManager.default.fileExists(atPath: path) else {
            return CapabilityManifest(path: path,
                                      loaded: false,
                                      permissionMode: "allow-all-dev",
                                      allowedCommands: [],
                                      parseError: "")
        }
        guard let data = FileManager.default.contents(atPath: path) else {
            return CapabilityManifest(path: path,
                                      loaded: true,
                                      permissionMode: "allow-all-dev",
                                      allowedCommands: [],
                                      parseError: "manifest-unreadable")
        }
        do {
            let json = try JSONSerialization.jsonObject(with: data)
            guard let object = json as? [String: Any] else {
                return CapabilityManifest(path: path,
                                          loaded: true,
                                          permissionMode: "allow-all-dev",
                                          allowedCommands: [],
                                          parseError: "manifest-root-not-object")
            }
            let permissionMode = object["permissionMode"] as? String ?? "allow-all-dev"
            let windows = object["windows"] as? [[String: Any]] ?? []
            var commands = Set<String>()
            for window in windows where (window["label"] as? String ?? "main") == "main" {
                if let values = window["commands"] as? [String] {
                    commands.formUnion(values)
                }
                if let values = window["capabilities"] as? [String] {
                    commands.formUnion(values)
                }
            }
            return CapabilityManifest(path: path,
                                      loaded: true,
                                      permissionMode: permissionMode,
                                      allowedCommands: commands,
                                      parseError: "")
        } catch {
            return CapabilityManifest(path: path,
                                      loaded: true,
                                      permissionMode: "allow-all-dev",
                                      allowedCommands: [],
                                      parseError: error.localizedDescription)
        }
    }

    func allows(_ command: String) -> Bool {
        if permissionMode == "trace-only-dry-run" {
            return true
        }
        if !parseError.isEmpty {
            return false
        }
        if permissionMode == "manifest-enforced" {
            return loaded && allowedCommands.contains(command)
        }
        if !loaded {
            return true
        }
        return allowedCommands.contains(command)
    }

    func denialReason(_ command: String) -> String {
        if !parseError.isEmpty {
            return "manifest-invalid"
        }
        if permissionMode == "manifest-enforced" && !loaded {
            return "manifest-missing"
        }
        if loaded && !allowedCommands.contains(command) {
            return "manifest-denied"
        }
        return "unsupported-command"
    }

    func allowedReason(_ command: String) -> String {
        if permissionMode == "trace-only-dry-run" {
            if loaded && !allowedCommands.contains(command) {
                return "dry-run-would-deny"
            }
            return loaded ? "dry-run-manifest-allow" : "dry-run-development-default"
        }
        return loaded ? "manifest-allow" : "development-default"
    }

    func tracePayload() -> [String: Any] {
        [
            "path": path,
            "loaded": loaded,
            "permissionMode": permissionMode,
            "allowedCommands": allowedCommands.sorted(),
            "parseError": parseError
        ]
    }
}
