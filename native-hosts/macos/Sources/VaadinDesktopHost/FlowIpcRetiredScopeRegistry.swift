import Foundation

struct FlowIpcScopeIdentity: Hashable {
    let windowId: String
    let sessionId: String
    let uiId: String

    init(windowId: String, sessionId: String, uiId: String) {
        self.windowId = windowId
        self.sessionId = sessionId
        self.uiId = uiId
    }

    init?(envelope: [String: Any]) {
        guard let windowId = envelope["windowId"] as? String,
              !windowId.isEmpty,
              let sessionId = envelope["sessionId"] as? String,
              !sessionId.isEmpty,
              let uiId = envelope["uiId"] as? String,
              !uiId.isEmpty else {
            return nil
        }
        self.init(
            windowId: windowId,
            sessionId: sessionId,
            uiId: uiId)
    }
}

enum FlowIpcRetiredScopeDisposition: Equatable {
    case rejectCommand(FlowIpcScopeIdentity)
    case dropTransportFrame(FlowIpcScopeIdentity)
    case notRetired
}

struct FlowIpcRetiredScopeRegistry {
    private var scopes: Set<FlowIpcScopeIdentity> = []

    mutating func retire(_ scope: FlowIpcScopeIdentity) {
        scopes.insert(scope)
    }

    func contains(_ scope: FlowIpcScopeIdentity) -> Bool {
        scopes.contains(scope)
    }

    func disposition(
        for envelope: [String: Any],
        type: String
    ) -> FlowIpcRetiredScopeDisposition {
        guard let scope = FlowIpcScopeIdentity(envelope: envelope),
              scopes.contains(scope) else {
            return .notRetired
        }
        if type == "command" {
            return .rejectCommand(scope)
        }
        if type.hasPrefix("opaqueHttp") || type.hasPrefix("opaquePush") {
            return .dropTransportFrame(scope)
        }
        return .notRetired
    }
}

func flowIpcOwnerWindowClosedCapabilityResult(
    _ command: String
) -> [String: Any] {
    [
        "success": false,
        "command": command,
        "canceled": true,
        "cancelReason": "owner-window-closed",
        "operationInvalidated": true,
        "error": "Owner window closed before native capability completed"
    ]
}
