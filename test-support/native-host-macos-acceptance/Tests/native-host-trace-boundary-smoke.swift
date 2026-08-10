import Foundation

@main
struct NativeHostTraceBoundarySmoke {
    static func main() throws {
        let opaqueBody = """
        {"changes":[{"key":"framework-fixture-secret"}],"method":"scrollToIndex"}
        """
        let envelope: [String: Any] = [
            "version": 1,
            "type": "opaquePush",
            "id": "push-1",
            "correlationId": "push-1",
            "sessionId": "session-1",
            "uiId": "main",
            "windowId": "main",
            "route": "",
            "payload": [
                "uidl": opaqueBody,
                "pushId": "sensitive-push-id",
                "pushSequence": 3,
                "flowServerSyncId": 7,
                "deliveryAttempt": 1
            ]
        ]

        let redacted = redactedFlowIpcEnvelope(envelope)
        let redactedPayload = try requireDictionary(
            redacted["payload"],
            "redacted envelope payload")
        try require(redactedPayload["uidl"] == nil, "opaque UIDL body was retained")
        try require(
            redactedPayload["uidlSearchIndex"] == nil,
            "production trace derived a UIDL search index")
        try require(
            redactedPayload["uidlBytes"] as? Int == opaqueBody.utf8.count,
            "opaque UIDL byte count was not preserved")
        try require(
            redactedPayload["uidlRedacted"] as? Bool == true,
            "opaque UIDL redaction marker is missing")
        try require(
            (redactedPayload["pushId"] as? String)?.hasPrefix("[redacted") == true,
            "sensitive push id was not redacted")

        let downloadBody = "download-secret-body"
        let redactedDownload = redactedFlowIpcEnvelope([
            "type": "command",
            "payload": [
                "command": "fileTransfer.download",
                "args": [
                    "body": downloadBody,
                    "bodyPreview": "must-not-survive"
                ]
            ]
        ])
        let redactedDownloadPayload = try requireDictionary(
            redactedDownload["payload"],
            "redacted download payload")
        let redactedDownloadArgs = try requireDictionary(
            redactedDownloadPayload["args"],
            "redacted download arguments")
        try require(
            redactedDownloadArgs["body"] == nil
                && redactedDownloadArgs["bodyPreview"] == nil,
            "download trace retained command body content")
        try require(
            redactedDownloadArgs["bodyBytes"] as? Int == downloadBody.utf8.count
                && redactedDownloadArgs["bodyRedacted"] as? Bool == true,
            "download trace lost opaque body redaction metadata")

        let compact = compactHostProtocolTracePayload(
            type: "opaquePush",
            payload: [
                "source": "java-sidecar",
                "transport": "flow-ipc",
                "envelope": redacted
            ])
        try require(
            compact["uidlBytes"] as? Int == opaqueBody.utf8.count,
            "compact trace lost opaque body size")
        try require(
            compact["uidlRedacted"] as? Bool == true,
            "compact trace lost opaque body redaction")
        let compactText = try jsonText(compact)
        try require(
            !compactText.contains("framework-fixture-secret")
                && !compactText.contains("scrollToIndex"),
            "compact production trace leaked opaque payload semantics")

        let semanticPayload: [String: Any] = [
            "source": "java-sidecar",
            "transport": "flow-ipc",
            "envelope": [
                "type": "opaquePush",
                "id": "push-2",
                "correlationId": "push-2",
                "payload": [
                    "flowClientResources": ["resourceCount": 2],
                    "componentFrontendResources": ["resourceCount": 4],
                    "uidl": ["body": "component-specific-body"]
                ]
            ]
        ]
        let semanticCompact = compactHostProtocolTracePayload(
            type: "opaquePush",
            payload: semanticPayload)
        try require(
            semanticCompact["flowClientResourceCount"] == nil
                && semanticCompact["componentFrontendResourceCount"] == nil
                && semanticCompact["uidlBodyBytes"] == nil,
            "production compaction derived Flow-semantic fields")

        let readyCompact = compactHostProtocolTracePayload(
            type: "ready",
            payload: [
                "source": "java-sidecar",
                "transport": "flow-ipc",
                "envelope": [
                    "type": "ready",
                    "id": "sidecar-ready",
                    "payload": [
                        "runtime": "standard-vaadin-bundle-opaque-ipc",
                        "browserRequestTransport": "opaque-http-stream-ipc",
                        "flowClientResourceLoading": "standard-production-bundle",
                        "transport": "inner-transport-must-not-overwrite-channel",
                        "enabledFeatureFlags": ["component-secret"],
                        "componentFrontendResourceLoading": "component-secret",
                        "application": ["name": "fixture-secret"]
                    ]
                ]
            ])
        try require(
            readyCompact["runtime"] as? String
                == "standard-vaadin-bundle-opaque-ipc"
                && readyCompact["browserRequestTransport"] as? String
                == "opaque-http-stream-ipc"
                && readyCompact["flowClientResourceLoading"] as? String
                == "standard-production-bundle"
                && readyCompact["transport"] as? String == "flow-ipc",
            "generic envelope compaction lost runtime transport metadata")
        let readyCompactText = try jsonText(readyCompact)
        try require(
            !readyCompactText.contains("component-secret")
                && !readyCompactText.contains("fixture-secret")
                && readyCompact["envelope"] == nil,
            "generic envelope compaction retained semantic ready content")

        let closeCompact = compactHostProtocolTracePayload(
            type: "window.closeRequested",
            payload: [
                "label": "main",
                "reason": "acceptance-complete",
                "readiness": "{\"complete\":true,\"fixture\":\"secret\"}"
            ])
        try require(
            closeCompact["reason"] as? String == "acceptance-complete",
            "generic close reason was not retained")
        try require(
            closeCompact["readiness"] == nil
                && closeCompact["readinessComplete"] == nil,
            "production compaction interpreted acceptance readiness")

        print("Native host trace boundary smoke passed")
    }

    private static func require(
        _ condition: @autoclosure () -> Bool,
        _ message: String
    ) throws {
        if !condition() {
            throw SmokeFailure(message: message)
        }
    }

    private static func requireDictionary(
        _ value: Any?,
        _ label: String
    ) throws -> [String: Any] {
        guard let dictionary = value as? [String: Any] else {
            throw SmokeFailure(message: "missing \(label)")
        }
        return dictionary
    }

    private static func jsonText(_ value: Any) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: value)
        guard let text = String(data: data, encoding: .utf8) else {
            throw SmokeFailure(message: "trace JSON was not UTF-8")
        }
        return text
    }

    private struct SmokeFailure: Error, CustomStringConvertible {
        let message: String
        var description: String { message }
    }
}
