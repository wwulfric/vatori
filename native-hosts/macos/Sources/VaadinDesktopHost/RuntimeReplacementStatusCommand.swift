import Foundation

enum RuntimeReplacementStatusCommand {
    static let name = "runtime.replacement.status"

    static func execute(args: [String: Any], owner: UUID?, windowId: String,
                        manifest: CapabilityManifest,
                        records: RuntimeReplacementRecords) -> [String: Any] {
        var result: [String: Any] = ["success": false, "command": name,
                                    "mode": "host-runtime-replacement"]
        guard manifest.allows(name, windowId: windowId) else {
            result["permissionDecision"] = "denied"
            result["permissionReason"] = manifest.denialReason(name, windowId: windowId)
            result["error"] = "Command not allowed by desktop-capabilities.json"
            return result
        }
        result["permissionDecision"] = "allowed"
        result["permissionReason"] = manifest.allowedReason(name, windowId: windowId)
        guard let owner else {
            result["error"] = "A live Flow IPC native window owner is required"
            result["operationInvalidated"] = true
            return result
        }
        // No caller-supplied owner, window, generation or operation selector.
        guard args.isEmpty else {
            result["error"] = "Replacement status takes no arguments"
            return result
        }
        result["success"] = true
        guard let record = records.latest(owner: owner) else {
            result["status"] = "none"
            return result
        }
        result["status"] = record.outcome?.rawValue ?? "pending"
        result["operationId"] = record.operationId.uuidString
        result["requestId"] = record.requestId
        result["candidateId"] = record.candidateId
        result["requestingGeneration"] = record.requestingGeneration
        return result
    }
}
