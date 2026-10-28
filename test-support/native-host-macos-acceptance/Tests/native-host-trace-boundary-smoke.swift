import Foundation

@main
struct NativeHostTraceBoundarySmoke {
    static func main() throws {
        try writeGrantRedaction()
        try notificationRedaction()
        try notificationNativeErrorDiagnostics()
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

        let grantIdMarker = "11111111-2222-3333-4444-555555555555"
        let selectedPathMarker = "/private/tmp/file-grant-secret/report.txt"
        let selectedFilenameMarker = "report.txt"
        let redactedGrantReadCommand = redactedFlowIpcEnvelope([
            "type": "command",
            "payload": [
                "command": "fileGrant.read",
                "args": [
                    "grantId": grantIdMarker,
                    "offset": 0,
                    "maxBytes": 19
                ]
            ]
        ])
        let redactedGrantReadCommandPayload = try requireDictionary(
            redactedGrantReadCommand["payload"],
            "redacted file grant read command payload")
        let redactedGrantReadCommandArgs = try requireDictionary(
            redactedGrantReadCommandPayload["args"],
            "redacted file grant read command arguments")
        try require(
            (redactedGrantReadCommandArgs["grantId"] as? String)?
                .hasPrefix("[redacted") == true
                && redactedGrantReadCommandArgs["offset"] as? Int == 0
                && redactedGrantReadCommandArgs["maxBytes"] as? Int == 19,
            "file grant read trace retained its bearer token or lost its range")

        let redactedGrantSelection = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": true,
                "command": "fileDialog.openReadGrant",
                "grantId": grantIdMarker,
                "path": selectedPathMarker,
                "paths": [selectedPathMarker],
                "filename": selectedFilenameMarker,
                "byteCount": 49,
                "pathExposed": false
            ]
        ])
        let redactedGrantSelectionPayload = try requireDictionary(
            redactedGrantSelection["payload"],
            "redacted file grant selection payload")
        for key in ["grantId", "path", "paths", "filename"] {
            try require(
                (redactedGrantSelectionPayload[key] as? String)?
                    .hasPrefix("[redacted") == true,
                "file grant selection trace retained sensitive \(key)")
        }

        let grantBody = Data("file-grant-body-secret".utf8)
        let redactedGrantReadResult = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": true,
                "command": "fileGrant.read",
                "bodyBase64": grantBody.base64EncodedString(),
                "bytesRead": grantBody.count,
                "offset": 0,
                "nextOffset": grantBody.count
            ]
        ])
        let redactedGrantReadResultPayload = try requireDictionary(
            redactedGrantReadResult["payload"],
            "redacted file grant read result payload")
        try require(
            redactedGrantReadResultPayload["bodyBase64"] == nil
                && redactedGrantReadResultPayload["bodyBytes"] as? Int
                    == grantBody.count
                && redactedGrantReadResultPayload["bodyRedacted"] as? Bool
                    == true,
            "file grant read trace retained body content or lost byte metadata")
        let grantErrorMarker =
            "read failed at /private/tmp/file-grant-error-secret/report.txt"
        let redactedGrantError = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": false,
                "command": "fileGrant.read",
                "error": grantErrorMarker,
                "errorCode": "grant-read-failed"
            ]
        ])
        let redactedGrantErrorPayload = try requireDictionary(
            redactedGrantError["payload"],
            "redacted file grant error payload")
        try require(
            (redactedGrantErrorPayload["error"] as? String)?
                .hasPrefix("[redacted") == true
                && redactedGrantErrorPayload["errorCode"] as? String
                    == "grant-read-failed",
            "file grant error trace retained filesystem diagnostics")
        let redactedGrantTrace = try jsonText([
            redactedGrantReadCommand,
            redactedGrantSelection,
            redactedGrantReadResult,
            redactedGrantError
        ])
        try require(
            !redactedGrantTrace.contains(grantIdMarker)
                && !redactedGrantTrace.contains(selectedPathMarker)
                && !redactedGrantTrace.contains(selectedFilenameMarker)
                && !redactedGrantTrace.contains("file-grant-body-secret")
                && !redactedGrantTrace.contains(grantBody.base64EncodedString())
                && !redactedGrantTrace.contains(grantErrorMarker),
            "file grant secret survived serialized trace redaction")

        let clipboardMarker = "clipboard-boundary-secret"
        let redactedClipboardWrite = redactedFlowIpcEnvelope([
            "type": "command",
            "payload": [
                "command": "clipboard.writeText",
                "args": ["text": clipboardMarker]
            ]
        ])
        let redactedClipboardWritePayload = try requireDictionary(
            redactedClipboardWrite["payload"],
            "redacted clipboard write payload")
        let redactedClipboardWriteArgs = try requireDictionary(
            redactedClipboardWritePayload["args"],
            "redacted clipboard write arguments")
        try require(
            redactedClipboardWriteArgs["text"] == nil
                && redactedClipboardWriteArgs["textLength"] as? Int
                    == clipboardMarker.count
                && redactedClipboardWriteArgs["textRedacted"] as? Bool == true,
            "clipboard write trace retained text or lost redaction metadata")

        let redactedEmptyClipboardWrite = redactedFlowIpcEnvelope([
            "type": "command",
            "payload": [
                "command": "clipboard.writeText",
                "args": ["text": ""]
            ]
        ])
        let redactedEmptyClipboardWritePayload = try requireDictionary(
            redactedEmptyClipboardWrite["payload"],
            "redacted empty clipboard write payload")
        let redactedEmptyClipboardWriteArgs = try requireDictionary(
            redactedEmptyClipboardWritePayload["args"],
            "redacted empty clipboard write arguments")
        try require(
            redactedEmptyClipboardWriteArgs["text"] == nil
                && redactedEmptyClipboardWriteArgs["textLength"] as? Int == 0
                && redactedEmptyClipboardWriteArgs["textRedacted"] as? Bool
                    == true,
            "empty clipboard write trace lost redaction metadata")

        let malformedClipboardMarker = "clipboard-malformed-boundary-secret"
        let redactedClipboardWriteWithSibling = redactedFlowIpcEnvelope([
            "type": "command",
            "payload": [
                "command": "clipboard.writeText",
                "args": [
                    "text": "safe",
                    "textLength": malformedClipboardMarker,
                    "sibling": malformedClipboardMarker
                ]
            ]
        ])
        let redactedClipboardWriteWithSiblingPayload = try requireDictionary(
            redactedClipboardWriteWithSibling["payload"],
            "redacted clipboard write with sibling payload")
        let redactedClipboardWriteWithSiblingArgs = try requireDictionary(
            redactedClipboardWriteWithSiblingPayload["args"],
            "redacted clipboard write with sibling arguments")
        try require(
            redactedClipboardWriteWithSiblingArgs.count == 2
                && redactedClipboardWriteWithSiblingArgs["textLength"] as? Int
                    == "safe".count
                && redactedClipboardWriteWithSiblingArgs["textRedacted"] as? Bool
                    == true,
            "clipboard write trace retained a sibling or untrusted length")

        let malformedClipboardWrites: [[String: Any]] = [
            [
                "type": "command",
                "payload": [
                    "command": "clipboard.writeText",
                    "args": [
                        "text": [malformedClipboardMarker],
                        "textLength": malformedClipboardMarker
                    ]
                ]
            ],
            [
                "type": "command",
                "payload": [
                    "command": "clipboard.writeText",
                    "args": [
                        "text": ["nested": malformedClipboardMarker],
                        "textLength": malformedClipboardMarker
                    ]
                ]
            ],
            [
                "type": "command",
                "payload": [
                    "command": "clipboard.writeText",
                    "args": [
                        "text": NSNull(),
                        "textLength": malformedClipboardMarker
                    ]
                ]
            ],
            [
                "type": "command",
                "payload": [
                    "command": "clipboard.writeText",
                    "args": [malformedClipboardMarker]
                ]
            ],
            [
                "type": "command",
                "payload": [
                    "command": "clipboard.writeText",
                    "args": NSNull()
                ]
            ],
            [
                "type": "command",
                "payload": [
                    "command": "clipboard.writeText",
                    "args": [
                        "textLength": malformedClipboardMarker,
                        "sibling": malformedClipboardMarker
                    ]
                ]
            ]
        ]
        var redactedMalformedClipboardWrites: [[String: Any]] = []
        for malformedWrite in malformedClipboardWrites {
            let redactedWrite = redactedFlowIpcEnvelope(malformedWrite)
            redactedMalformedClipboardWrites.append(redactedWrite)
            let writePayload = try requireDictionary(
                redactedWrite["payload"],
                "redacted malformed clipboard write payload")
            let writeArgs = try requireDictionary(
                writePayload["args"],
                "redacted malformed clipboard write arguments")
            try require(
                writeArgs.count == 2
                    && writeArgs["text"] == nil
                    && writeArgs["textLength"] == nil
                    && writeArgs["textRedacted"] as? Bool == true
                    && writeArgs["textMalformed"] as? Bool == true,
                "malformed clipboard write retained content or untrusted length")
        }

        let redactedAllowedClipboardWriteResult = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": true,
                "command": "clipboard.writeText",
                "written": true,
                "textLength": 0,
                "permissionDecision": "allowed"
            ]
        ])
        let redactedAllowedClipboardWriteResultPayload = try requireDictionary(
            redactedAllowedClipboardWriteResult["payload"],
            "redacted allowed clipboard write result payload")
        try require(
            redactedAllowedClipboardWriteResultPayload.count == 5
                && redactedAllowedClipboardWriteResultPayload["args"] == nil
                && redactedAllowedClipboardWriteResultPayload["success"] as? Bool
                    == true
                && redactedAllowedClipboardWriteResultPayload["written"] as? Bool
                    == true
                && redactedAllowedClipboardWriteResultPayload["textLength"] as? Int
                    == 0
                && redactedAllowedClipboardWriteResultPayload["permissionDecision"]
                    as? String == "allowed",
            "allowed clipboard write result trace changed its payload shape")

        let redactedDeniedClipboardWriteResult = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": false,
                "command": "clipboard.writeText",
                "error": "Command not allowed",
                "permissionDecision": "denied"
            ]
        ])
        let redactedDeniedClipboardWriteResultPayload = try requireDictionary(
            redactedDeniedClipboardWriteResult["payload"],
            "redacted denied clipboard write result payload")
        try require(
            redactedDeniedClipboardWriteResultPayload.count == 4
                && redactedDeniedClipboardWriteResultPayload["args"] == nil
                && redactedDeniedClipboardWriteResultPayload["success"] as? Bool
                    == false
                && redactedDeniedClipboardWriteResultPayload["error"] as? String
                    == "Command not allowed"
                && redactedDeniedClipboardWriteResultPayload["permissionDecision"]
                    as? String == "denied",
            "denied clipboard write result trace changed its payload shape")

        let redactedClipboardRead = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": true,
                "command": "clipboard.readText",
                "textPresent": true,
                "text": clipboardMarker,
                "textLength": -1
            ]
        ])
        let redactedClipboardReadPayload = try requireDictionary(
            redactedClipboardRead["payload"],
            "redacted clipboard read payload")
        try require(
            redactedClipboardReadPayload["text"] == nil
                && redactedClipboardReadPayload["textPresent"] as? Bool == true
                && redactedClipboardReadPayload["textLength"] as? Int
                    == clipboardMarker.count
                && redactedClipboardReadPayload["textRedacted"] as? Bool == true,
            "clipboard read trace retained text or lost redaction metadata")

        let redactedEmptyClipboardRead = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": true,
                "command": "clipboard.readText",
                "textPresent": true,
                "text": "",
                "textLength": -1
            ]
        ])
        let redactedEmptyClipboardReadPayload = try requireDictionary(
            redactedEmptyClipboardRead["payload"],
            "redacted empty clipboard read payload")
        try require(
            redactedEmptyClipboardReadPayload["text"] == nil
                && redactedEmptyClipboardReadPayload["textPresent"] as? Bool
                    == true
                && redactedEmptyClipboardReadPayload["textLength"] as? Int == 0
                && redactedEmptyClipboardReadPayload["textRedacted"] as? Bool
                    == true,
            "empty clipboard read trace lost redaction metadata")

        let redactedNoTextClipboardRead = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": [
                "success": true,
                "command": "clipboard.readText",
                "textPresent": false,
                "textLength": 0
            ]
        ])
        let redactedNoTextClipboardReadPayload = try requireDictionary(
            redactedNoTextClipboardRead["payload"],
            "redacted no-text clipboard read payload")
        try require(
            redactedNoTextClipboardReadPayload["text"] == nil
                && redactedNoTextClipboardReadPayload["textPresent"] as? Bool
                    == false
                && redactedNoTextClipboardReadPayload["textLength"] as? Int == 0,
            "no-text clipboard read trace changed its result shape")

        let malformedClipboardReads: [[String: Any]] = [
            [
                "type": "capabilityResult",
                "payload": [
                    "success": true,
                    "command": "clipboard.readText",
                    "textPresent": true,
                    "text": [malformedClipboardMarker],
                    "textLength": malformedClipboardMarker
                ]
            ],
            [
                "type": "capabilityResult",
                "payload": [
                    "success": true,
                    "command": "clipboard.readText",
                    "textPresent": true,
                    "text": ["nested": malformedClipboardMarker],
                    "textLength": malformedClipboardMarker
                ]
            ],
            [
                "type": "capabilityResult",
                "payload": [
                    "success": true,
                    "command": "clipboard.readText",
                    "textPresent": true,
                    "text": NSNull(),
                    "textLength": malformedClipboardMarker
                ]
            ]
        ]
        var redactedMalformedClipboardReads: [[String: Any]] = []
        for malformedRead in malformedClipboardReads {
            let redactedRead = redactedFlowIpcEnvelope(malformedRead)
            redactedMalformedClipboardReads.append(redactedRead)
            let readPayload = try requireDictionary(
                redactedRead["payload"],
                "redacted malformed clipboard read payload")
            try require(
                readPayload["text"] == nil
                    && readPayload["textLength"] == nil
                    && readPayload["textRedacted"] as? Bool == true
                    && readPayload["textMalformed"] as? Bool == true,
                "malformed clipboard read retained content or untrusted length")
        }
        let redactedClipboardTrace = try jsonText([
            redactedClipboardWrite,
            redactedEmptyClipboardWrite,
            redactedClipboardWriteWithSibling,
            redactedAllowedClipboardWriteResult,
            redactedDeniedClipboardWriteResult,
            redactedClipboardRead,
            redactedEmptyClipboardRead,
            redactedNoTextClipboardRead,
            redactedMalformedClipboardWrites,
            redactedMalformedClipboardReads
        ])
        try require(
            !redactedClipboardTrace.contains(clipboardMarker)
                && !redactedClipboardTrace.contains(malformedClipboardMarker),
            "clipboard marker survived serialized trace redaction")

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

    private static func notificationRedaction() throws {
        let title = "notification-private-title"
        let body = "notification-private-body"
        let preview = "notification-private-preview"
        let identifier = "vatori-notification-private-identifier"
        let error = "notification-private-error /private/tmp/private-notification"
        let forged = "[redacted notification-private-forged-marker]"
        let malformed = "notification-private-malformed-arguments"
        let sensitive: [String: Any] = [
            "title": title, "body": body, "bodyPreview": preview,
            "notificationId": identifier, "error": error
        ]
        let permission: [String: Any] = [
            "permissionMode": "manifest-enforced",
            "permissionDecision": "allowed",
            "permissionReason": "manifest-allowed",
            "permissionScope": "window:main",
            "permissionManifestLoaded": true,
            "permissionManifestCommands": "notification.show"
        ]
        var traces: [Any] = []
        for type in ["command", "capabilityResult"] {
            var payload: [String: Any] = [
                "command": "notification.show",
                "mode": "native-user-notifications"
            ]
            if type == "command" {
                payload["args"] = sensitive
            } else {
                payload.merge(sensitive) { _, value in value }
                payload.removeValue(forKey: "error")
                payload["success"] = true
                payload["scheduled"] = true
                payload.merge(permission) { _, value in value }
            }
            let original: [String: Any] = [
                "version": 1, "type": type,
                "id": "notification-command-1", "correlationId": "notification-command-1",
                "sessionId": "notification-session", "uiId": "main", "windowId": "main",
                "payload": payload
            ]
            let redacted = redactedFlowIpcEnvelope(original)
            traces.append(redacted)
            let result = try requireDictionary(redacted["payload"], "notification payload")
            try require(result["command"] as? String == "notification.show"
                        && result["mode"] as? String == "native-user-notifications"
                        && redacted["sessionId"] as? String == "notification-session"
                        && redacted["uiId"] as? String == "main"
                        && redacted["windowId"] as? String == "main"
                        && redacted["correlationId"] as? String == "notification-command-1",
                        "notification redaction changed command or scope correlation")
            if type == "capabilityResult" {
                try require(result["success"] as? Bool == true
                            && result["scheduled"] as? Bool == true,
                            "notification success lost its scheduling result")
                for (key, value) in permission {
                    let actual = try jsonText([result[key] ?? NSNull()])
                    let expected = try jsonText([value])
                    try require(actual == expected,
                                "notification redaction changed permission metadata")
                }
            }
            let originalText = try jsonText(original)
            try require(originalText.contains(title) && originalText.contains(body)
                        && originalText.contains(identifier),
                        "notification trace redaction mutated the original wire result")
        }
        for code in [
            "notification-title-invalid", "notification-authorization-required",
            "notification-authorization-denied", "notification-authorization-failed",
            "notification-authorization-unsupported", "notification-scheduling-failed"
        ] {
            var payload = sensitive
            payload.merge(permission) { _, value in value }
            payload["command"] = "notification.show"
            payload["success"] = false
            payload["scheduled"] = false
            payload["errorCode"] = code
            let redacted = redactedFlowIpcEnvelope([
                "type": "capabilityResult", "payload": payload
            ])
            traces.append(redacted)
            let result = try requireDictionary(redacted["payload"], "notification failure")
            try require(result["success"] as? Bool == false
                        && result["scheduled"] as? Bool == false
                        && result["errorCode"] as? String == code
                        && result["permissionDecision"] as? String == "allowed",
                        "notification failure lost stable outcome or permission metadata")
        }
        for invalidated in [false, true] {
            var payload: [String: Any] = [
                "command": "notification.show", "success": false,
                "title": title, "error": error,
                "permissionDecision": invalidated ? "allowed" : "denied"
            ]
            if invalidated {
                payload["operationInvalidated"] = true
                payload["canceled"] = true
                payload["cancelReason"] = "owner-window-closed"
            }
            let redacted = redactedFlowIpcEnvelope([
                "type": "capabilityResult", "payload": payload
            ])
            traces.append(redacted)
            let result = try requireDictionary(redacted["payload"], "notification denial/invalidation")
            try require(result["scheduled"] == nil
                        && result["permissionDecision"] as? String
                            == (invalidated ? "allowed" : "denied"),
                        "notification denial/invalidation acquired a false scheduling claim")
            if invalidated {
                try require(result["operationInvalidated"] as? Bool == true
                            && result["canceled"] as? Bool == true
                            && result["cancelReason"] as? String == "owner-window-closed",
                            "notification invalidation lost lifecycle metadata")
            }
        }
        let values: [Any] = [forged, [forged], ["nested": forged], NSNull(), 17, true]
        for type in ["command", "capabilityResult"] {
            for key in ["TiTlE", "BoDy", "BoDyPrEvIeW", "NoTiFiCaTiOnId", "ErRoR", "UsErInFo"] {
                for value in values {
                    let nested: [String: Any] = [
                        "items": [["deeper": [[key: value, "scheduled": true,
                                              "errorCode": "notification-scheduling-failed"]]]]
                    ]
                    var payload: [String: Any] = ["command": "notification.show"]
                    payload[type == "command" ? "args" : "details"] = nested
                    let redacted = redactedFlowIpcEnvelope([
                        "type": type, "payload": payload
                    ])
                    traces.append(redacted)
                    let text = try jsonText(redacted)
                    let expected = (value as? String).map {
                        "[redacted length=\($0.count)]"
                    } ?? "[redacted]"
                    try require(text.contains(expected) && !text.contains(forged)
                                && text.contains("notification-scheduling-failed"),
                                "nested notification value was not freshly redacted")
                }
            }
        }
        for args in [malformed, [malformed], NSNull(), 17, true] as [Any] {
            let redacted = redactedFlowIpcEnvelope([
                "type": "command",
                "payload": ["command": "notification.show", "args": args]
            ])
            traces.append(redacted)
            let payload = try requireDictionary(redacted["payload"], "malformed notification")
            let result = try requireDictionary(payload["args"], "malformed notification arguments")
            try require(result.count == 2
                        && result["argumentsRedacted"] as? Bool == true
                        && result["argumentsMalformed"] as? Bool == true,
                        "malformed notification arguments retained content")
        }
        let fullText = try jsonText(traces)
        for marker in [title, body, preview, identifier, "notification-private-error",
                       "private-notification", forged, malformed] {
            try require(!fullText.contains(marker),
                        "notification secret survived serialized full trace")
        }
        let unrelated = redactedFlowIpcEnvelope([
            "type": "capabilityResult",
            "payload": ["command": "window.setTitle", "details": sensitive]
        ])
        let unrelatedPayload = try requireDictionary(unrelated["payload"], "unrelated result")
        let unrelatedDetails = try requireDictionary(unrelatedPayload["details"], "unrelated details")
        try require(NSDictionary(dictionary: unrelatedDetails).isEqual(to: sensitive),
                    "notification redaction changed another command's trace policy")
    }

    private static func notificationNativeErrorDiagnostics() throws {
        let privateDomain = "private.notification.domain"
        let privateCode = "private-notification-code"
        let privateError = "private-notification-localized-error"
        let privateUserInfo = "private-notification-user-info"
        let forged = "[redacted private-notification-diagnostic-marker]"
        var traces: [Any] = []
        let safeCodes: [Int64] = [Int64.min, -1, 0, 1, 9_007_199_254_740_993, Int64.max]
        for domain in ["UNErrorDomain", "other"] {
            for code in safeCodes {
                let original: [String: Any] = [
                    "type": "capabilityResult",
                    "payload": [
                        "command": "notification.show", "success": false, "scheduled": false,
                        "errorCode": "notification-authorization-failed",
                        "nativeErrorDomain": domain, "nativeErrorCode": code,
                        "error": privateError, "userInfo": ["description": privateUserInfo]
                    ]
                ]
                let redacted = redactedFlowIpcEnvelope(original)
                let result = try requireDictionary(redacted["payload"], "notification diagnostics")
                try require(result["nativeErrorDomain"] as? String == domain
                            && result["nativeErrorCode"] as? Int64 == code
                            && result["scheduled"] as? Bool == false
                            && result["errorCode"] as? String == "notification-authorization-failed",
                            "safe notification diagnostics or outcome metadata were lost")
                let compact = compactHostProtocolTracePayload(
                    type: "sidecar.envelope", payload: ["envelope": redacted])
                try require(compact["nativeErrorDomain"] as? String == domain
                            && compact["nativeErrorCode"] as? Int64 == code
                            && compact["error"] == nil && compact["userInfo"] == nil,
                            "compact notification trace lost safe diagnostics or exposed NSError content")
                for trace in [redacted, compact] {
                    let text = try jsonText(trace)
                    try require(text.contains("\"nativeErrorCode\":\(code)"),
                                "notification diagnostic code lost integer precision in JSON")
                    traces.append(trace)
                }
                let originalText = try jsonText(original)
                try require(originalText.contains(privateError) && originalText.contains(privateUserInfo),
                            "notification diagnostic redaction changed the original result")
            }
        }

        let invalidDomains: [Any] = [
            privateDomain, forged, "unerrordomain", "OTHER", "", 1, true,
            [privateDomain], ["domain": privateDomain], NSNull()
        ]
        let invalidCodes: [Any] = [
            privateCode, forged, "1", "-1", true, false,
            Float(1), Double(1), Double(1.5), NSNumber(value: Double(-1)),
            NSNumber(value: UInt64.max), [privateCode], ["code": privateCode], NSNull()
        ]
        for (key, values) in [
            ("nativeErrorDomain", invalidDomains), ("NaTiVeErRoRDoMaIn", invalidDomains),
            ("nativeErrorCode", invalidCodes), ("NaTiVeErRoRCoDe", invalidCodes)
        ] {
            for value in values {
                let marker = (value as? String).map {
                    "[redacted length=\($0.count)]"
                } ?? "[redacted]"
                let original: [String: Any] = [
                    "type": "capabilityResult",
                    "payload": ["command": "notification.show", key: value]
                ]
                let redacted = redactedFlowIpcEnvelope(original)
                let result = try requireDictionary(redacted["payload"], "malformed notification diagnostics")
                try require(result[key] as? String == marker,
                            "malformed notification diagnostic was not freshly redacted: \(key)")
                traces.append(redacted)
                // Compaction must validate raw input independently, not just rely
                // on the production call order to have redacted the envelope first.
                for envelope in [original, redacted] {
                    let compact = compactHostProtocolTracePayload(
                        type: "sidecar.envelope", payload: ["envelope": envelope])
                    try require((compact[key] as? String)?.hasPrefix("[redacted") == true,
                                "compact notification trace retained a malformed diagnostic")
                    traces.append(compact)
                }
                for type in ["command", "capabilityResult"] {
                    let nested = redactedFlowIpcEnvelope([
                        "type": type,
                        "payload": [
                            "command": "notification.show",
                            type == "command" ? "args" : "details": [
                                "items": [["deeper": [[key: value]]]]
                            ]
                        ]
                    ])
                    let text = try jsonText(nested)
                    try require(text.contains(marker),
                                "nested notification diagnostic escaped recursive redaction")
                    traces.append(nested)
                }
            }
        }

        let caseVaried: [String: Any] = [
            "NaTiVeErRoRDoMaIn": "UNErrorDomain", "NaTiVeErRoRCoDe": Int64.min
        ]
        for type in ["command", "capabilityResult"] {
            let redacted = redactedFlowIpcEnvelope([
                "type": type,
                "payload": [
                    "command": "notification.show",
                    type == "command" ? "args" : "details": ["items": [["deeper": [caseVaried]]]]
                ]
            ])
            let text = try jsonText(redacted)
            try require(text.contains("UNErrorDomain") && text.contains("\"NaTiVeErRoRCoDe\":\(Int64.min)"),
                        "nested case-varied safe notification diagnostics were not preserved")
            traces.append(redacted)
        }
        var caseVariedPayload = caseVaried
        caseVariedPayload["command"] = "notification.show"
        let caseVariedCompact = compactHostProtocolTracePayload(
            type: "sidecar.envelope", payload: ["envelope": [
                "type": "capabilityResult", "payload": caseVariedPayload
            ]])
        try require(caseVariedCompact["NaTiVeErRoRDoMaIn"] as? String == "UNErrorDomain"
                    && caseVariedCompact["NaTiVeErRoRCoDe"] as? Int64 == Int64.min,
                    "compaction did not retain case-varied safe notification diagnostics")
        traces.append(caseVariedCompact)
        let fullText = try jsonText(traces)
        for secret in [privateDomain, privateCode, privateError, privateUserInfo, forged] {
            try require(!fullText.contains(secret),
                        "private NSError content survived full or compact serialized trace")
        }

        let unrelated: [String: Any] = [
            "type": "capabilityResult", "payload": [
                "command": "window.setTitle", "nativeErrorDomain": privateDomain,
                "nativeErrorCode": privateCode, "userInfo": privateUserInfo
            ]
        ]
        let unrelatedRedacted = redactedFlowIpcEnvelope(unrelated)
        try require(NSDictionary(dictionary: unrelatedRedacted).isEqual(to: unrelated),
                    "notification diagnostic policy changed another command's full trace")
        let unrelatedCompact = compactHostProtocolTracePayload(
            type: "sidecar.envelope", payload: ["envelope": unrelated])
        try require(unrelatedCompact["nativeErrorDomain"] == nil
                    && unrelatedCompact["nativeErrorCode"] == nil,
                    "notification diagnostic metadata widened another command's compact trace")
    }

    private static func writeGrantRedaction() throws {
        let token = "write-grant-token-secret"
        let filename = "write-grant-filename-secret.bin"
        let path = "/private/tmp/write-grant-path-secret/\(filename)"
        let body = Data("write-grant-body-secret".utf8).base64EncodedString()
        let request = redactedFlowIpcEnvelope([
            "type": "command",
            "payload": [
                "command": "fileGrant.write",
                "args": ["grantId": token, "offset": 65_536, "bodyBase64": body]
            ]
        ])
        let payload = try requireDictionary(request["payload"], "write request payload")
        let args = try requireDictionary(payload["args"], "write request args")
        try require(args["bodyBase64"] == nil && args["bodyRedacted"] as? Bool == true
                    && args["bodyEncodedBytes"] as? Int == body.utf8.count
                    && args["bodyBytes"] == nil && args["offset"] as? Int == 65_536,
                    "write command trace decoded or retained the opaque payload")
        try require((args["grantId"] as? String)?.hasPrefix("[redacted") == true,
                    "write command trace retained its grant token")
        let selector = redactedFlowIpcEnvelope([
            "type": "command", "payload": [
                "command": "fileDialog.saveNewWriteGrant",
                "args": ["title": filename, "suggestedName": filename]
            ]
        ])
        let selection = redactedFlowIpcEnvelope([
            "type": "capabilityResult", "payload": [
                "command": "fileDialog.saveNewWriteGrant", "success": true,
                "filename": filename, "path": path, "paths": [path],
                "grantId": token, "maxWriteBytes": 65_536, "createNewOnly": true
            ]
        ])
        var traces: [Any] = [request, selector, selection]
        for command in ["fileDialog.saveNewWriteGrant", "fileGrant.write", "fileGrant.releaseWrite", "fileGrant.cancelWrite"] {
            traces.append(redactedFlowIpcEnvelope([
                "type": "capabilityResult", "payload": [
                    "command": command, "error": path,
                    "nested": [["grantId": ["value": token], "filename": [filename],
                                "BoDyBaSe64": ["secret": body], "suggestedName": filename]]
                ]
            ]))
        }
        let malformedValues: [Any] = [
            body, [body], ["bodyBase64": [body], "grantId": [token]],
            ["bodyBase64": NSNull(), "grantId": token],
            ["BODYBASE64": body, "bodyBase64": body]
        ]
        for command in ["fileGrant.write", "fileGrant.cancelWrite"] {
        for malformed in malformedValues {
            let redacted = redactedFlowIpcEnvelope([
                "type": "command", "payload": ["command": command, "args": malformed]
            ])
            traces.append(redacted)
            let malformedPayload = try requireDictionary(redacted["payload"], "malformed write payload")
            let malformedArgs = try requireDictionary(malformedPayload["args"], "malformed write args")
            try require(malformedArgs["bodyMalformed"] as? Bool == true
                        || malformedArgs["argumentsMalformed"] as? Bool == true,
                        "malformed write args did not carry a redacted malformed marker")
        }
        }
        let huge = String(repeating: "A", count: 400_000)
        let oversized = redactedFlowIpcEnvelope([
            "type": "command", "payload": [
                "command": "fileGrant.write",
                "args": ["grantId": token, "bodyBase64": huge, "bodyBytes": 1,
                         "bodyEncodedBytes": 1, "bodyRedacted": false, "bodyMalformed": false]
            ]
        ])
        let hugePayload = try requireDictionary(oversized["payload"], "oversized write payload")
        let hugeArgs = try requireDictionary(hugePayload["args"], "oversized write args")
        try require(hugeArgs["bodyBytes"] == nil && hugeArgs["bodyMalformed"] == nil
                    && hugeArgs["bodyRedacted"] as? Bool == true
                    && hugeArgs["bodyEncodedBytes"] as? Int == huge.utf8.count,
                    "write trace trusted forged metadata or decoded an oversized body")
        traces.append(oversized)
        let forgedMarker = "[redacted private-write-grant-secret]"
        for command in ["fileDialog.saveNewWriteGrant", "fileGrant.write", "fileGrant.releaseWrite", "fileGrant.cancelWrite"] {
            traces.append(redactedFlowIpcEnvelope([
                "type": "command", "payload": [
                    "command": command,
                    "args": ["title": forgedMarker, "suggestedName": forgedMarker,
                             "filename": forgedMarker, "body": forgedMarker,
                             "grantId": forgedMarker, "error": forgedMarker]
                ]
            ]))
        }
        let canceled = redactedFlowIpcEnvelope([
            "type": "capabilityResult", "payload": [
                "command": "fileGrant.cancelWrite", "operation": "cancel-write-grant",
                "success": true, "canceled": true, "cancellationRequested": true,
                "grantUnavailable": true, "grantState": "unavailable", "nextOffset": 65_538,
                "securityScopeStopped": true, "pathExposed": false,
                "grantId": token, "error": path,
                "nested": ["grantId": token, "bodyBase64": body]
            ]
        ])
        let canceledPayload = try requireDictionary(canceled["payload"], "cancel result payload")
        try require(canceledPayload["nextOffset"] as? Int == 65_538
                    && canceledPayload["cancellationRequested"] as? Bool == true
                    && canceledPayload["canceled"] as? Bool == true
                    && canceledPayload["securityScopeStopped"] as? Bool == true,
                    "cancellation trace lost its non-sensitive prefix or cleanup result")
        traces.append(canceled)
        let text = try jsonText(traces)
        for marker in [token, filename, path, body, huge, "write-grant-body-secret", forgedMarker] {
            try require(!text.contains(marker), "write grant secret survived serialized trace")
        }
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
