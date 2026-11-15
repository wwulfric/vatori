import Foundation

enum RuntimeReplacementRequestCommand {
    static let name = "runtime.replacement.request"

    static func execute(args: [String: Any], hasOwner: Bool, windowId: String,
                        manifest: CapabilityManifest,
                        admit: (String, String) -> [String: Any]) -> [String: Any] {
        let base: [String: Any] = ["command": name, "mode": "host-runtime-replacement"]
        func result(_ payload: [String: Any]) -> [String: Any] {
            base.merging(payload) { _, new in new }
        }
        // Process replacement must never acquire authority from allow-all-dev
        // or trace-only defaults, even though ordinary capabilities may do so.
        guard manifest.parseError.isEmpty, manifest.permissionMode == "manifest-enforced",
              manifest.allows(name, windowId: windowId) else {
            return result(["success": false, "permissionDecision": "denied",
                "permissionReason": "explicit-manifest-permission-required",
                "error": "Product replacement requires an explicit manifest permission"])
        }
        var payload: [String: Any]
        if !hasOwner {
            payload = ["success": false, "operationInvalidated": true,
                       "error": "A live Flow IPC native window owner is required"]
        } else if Set(args.keys) == Set(["requestId", "candidateId"]),
                  let requestId = args["requestId"] as? String, isValidSidecarIdentifier(requestId),
                  let candidateId = args["candidateId"] as? String, isValidSidecarIdentifier(candidateId) {
            payload = admit(requestId, candidateId)
        } else {
            payload = ["success": false, "error": "Expected bounded requestId and candidateId",
                       "errorCode": "invalid-request"]
        }
        payload["permissionDecision"] = "allowed"
        payload["permissionReason"] = "manifest-allow"
        return result(payload)
    }
}
