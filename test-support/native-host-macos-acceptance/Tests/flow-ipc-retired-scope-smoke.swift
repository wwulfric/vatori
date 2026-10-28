import Foundation

@main
struct FlowIpcRetiredScopeSmoke {
    static func main() throws {
        let windowId = "reopened-secondary"
        let first = scope(windowId, "generation-a")
        let second = scope(windowId, "generation-b")
        let third = scope(windowId, "generation-c")
        let live = scope(windowId, "generation-d")
        var registry = FlowIpcRetiredScopeRegistry()

        registry.retire(first)
        registry.retire(second)
        registry.retire(third)
        registry.retire(first)

        try require(registry.contains(first)
                        && registry.contains(second)
                        && registry.contains(third),
                    "an exact retired generation was not retained")
        try require(!registry.contains(live),
                    "a fresh same-public-id generation was retired")

        for retired in [first, second, third] {
            try require(
                registry.disposition(
                    for: envelope(retired),
                    type: "command") == .rejectCommand(retired),
                "retired command did not receive the typed rejection path")
            let result = flowIpcOwnerWindowClosedCapabilityResult(
                "window.setTitle")
            try require(result["success"] as? Bool == false
                            && result["command"] as? String == "window.setTitle"
                            && result["canceled"] as? Bool == true
                            && result["cancelReason"] as? String
                                == "owner-window-closed"
                            && result["operationInvalidated"] as? Bool == true,
                        "retired command result lost its typed invalidation")
            for transportType in [
                "opaqueHttpResponse",
                "opaqueHttpStreamResponseChunk",
                "opaquePush",
                "opaquePushConnected"
            ] {
                try require(
                    registry.disposition(
                        for: envelope(retired),
                        type: transportType) == .dropTransportFrame(retired),
                    "retired \(transportType) was not dropped")
            }
        }

        try require(
            registry.disposition(
                for: envelope(live),
                type: "command") == .notRetired,
            "fresh generation command was rejected")
        try require(
            registry.disposition(
                for: envelope(first, windowId: "other-window"),
                type: "command") == .notRetired,
            "window id was not part of the retired identity")
        try require(
            registry.disposition(
                for: envelope(first, sessionId: "other-session"),
                type: "command") == .notRetired,
            "session id was not part of the retired identity")
        try require(
            registry.disposition(
                for: envelope(first, uiId: "other-ui"),
                type: "command") == .notRetired,
            "UI id was not part of the retired identity")
        try require(
            registry.disposition(
                for: [
                    "windowId": first.windowId,
                    "sessionId": first.sessionId
                ],
                type: "command") == .notRetired,
            "an incomplete tuple matched a retired scope")
        try require(
            registry.disposition(
                for: envelope(first),
                type: "futureControlMessage") == .notRetired,
            "a non-transport message used the transport drop path")

        for generation in 0..<64 {
            registry.retire(scope(windowId, "older-\(generation)"))
        }
        try require(registry.contains(first)
                        && registry.contains(second)
                        && registry.contains(third),
                    "later generations evicted an earlier exact tombstone")

        print("Flow IPC retired scope smoke passed")
    }

    private static func scope(
        _ windowId: String,
        _ generation: String
    ) -> FlowIpcScopeIdentity {
        FlowIpcScopeIdentity(
            windowId: windowId,
            sessionId: "flow-ipc-window:\(generation)",
            uiId: windowId)
    }

    private static func envelope(
        _ scope: FlowIpcScopeIdentity,
        windowId: String? = nil,
        sessionId: String? = nil,
        uiId: String? = nil
    ) -> [String: Any] {
        [
            "windowId": windowId ?? scope.windowId,
            "sessionId": sessionId ?? scope.sessionId,
            "uiId": uiId ?? scope.uiId
        ]
    }

    private static func require(
        _ condition: @autoclosure () -> Bool,
        _ message: String
    ) throws {
        if !condition() {
            throw SmokeFailure(message: message)
        }
    }

    private struct SmokeFailure: Error, CustomStringConvertible {
        let message: String
        var description: String { message }
    }
}
