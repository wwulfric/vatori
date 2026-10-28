import Foundation
import CoreFoundation

private func traceBoolean(_ value: Any) -> Bool? {
    guard let number = value as? NSNumber,
          CFGetTypeID(number) == CFBooleanGetTypeID() else {
        return nil
    }
    return number.boolValue
}

func detailedFlowIpcTraceEnabled() -> Bool {
    ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL"] == "full"
}

func isSensitiveTraceKey(_ key: String) -> Bool {
    let normalized = key.lowercased()
    return normalized == "securitykey"
        || normalized == "csrftoken"
        || normalized == "pushid"
        || normalized == "grantid"
        || normalized == "vaadin-security-key"
}

func redactedTraceValue(_ value: Any) -> String {
    if let text = value as? String {
        if text.hasPrefix("[redacted") {
            return text
        }
        return "[redacted length=\(text.count)]"
    }
    return "[redacted]"
}

func compactTraceString(_ text: String, maxLength: Int = 1024) -> String {
    text.count <= maxLength
        ? text
        : String(text.prefix(maxLength)) + "...[truncated]"
}

private func copyTraceScalars(
    from payload: [String: Any],
    keys: Set<String>,
    into compact: inout [String: Any],
    maxStringLength: Int = 256
) {
    for key in keys {
        guard let value = payload[key] else {
            continue
        }
        if isSensitiveTraceKey(key) {
            compact[key] = redactedTraceValue(value)
        } else if let text = value as? String {
            compact[key] = compactTraceString(text, maxLength: maxStringLength)
        } else if let bool = traceBoolean(value) {
            compact[key] = bool
        } else if let number = value as? NSNumber {
            compact[key] = number
        } else if let int = value as? Int {
            compact[key] = int
        } else if let int64 = value as? Int64 {
            compact[key] = int64
        } else if let double = value as? Double {
            compact[key] = double
        }
    }
}

private func compactProtocolEnvelopeTracePayload(
    _ payload: [String: Any]
) -> [String: Any] {
    var compact: [String: Any] = ["traceDetail": "compact"]
    copyTraceScalars(
        from: payload,
        keys: ["source", "target", "transport", "command", "windowId"],
        into: &compact)
    guard let envelope = payload["envelope"] as? [String: Any] else {
        compact["fieldCount"] = payload.count
        return compact
    }
    copyTraceScalars(
        from: envelope,
        keys: ["type", "id", "correlationId", "sessionId", "uiId", "windowId", "route"],
        into: &compact)
    guard let envelopePayload = envelope["payload"] as? [String: Any] else {
        return compact
    }
    compact["payloadFieldCount"] = envelopePayload.count
    copyTraceScalars(
        from: envelopePayload,
        keys: [
            "path", "url", "method", "requestId", "command",
            "runtime", "browserRequestTransport", "flowClientResourceLoading",
            "status", "statusText", "statusCode",
            "mimeType", "contentType", "bytes", "sha256",
            "bodyBytes", "bodyRedacted", "serverSequence",
            "chunkSequence", "chunkCount", "credit", "contentLength",
            "failureLayer", "noLocalhostProof", "connectionId", "flowUiId",
            "pushSequence", "flowServerSyncId", "lastSeenServerSyncId",
            "deliveryAttempt", "resync", "uidlBytes", "uidlRedacted"
        ],
        into: &compact,
        maxStringLength: 384)
    if envelope["type"] as? String == "capabilityResult",
       envelopePayload["command"] as? String == "notification.show" {
        for (key, value) in envelopePayload {
            let normalizedKey = key.lowercased()
            if normalizedKey == "nativeerrordomain" || normalizedKey == "nativeerrorcode" {
                compact[key] = redactedNotificationDiagnosticValue(value, key: key)
            }
        }
    }
    if let headers = envelopePayload["headers"] as? [String: Any] {
        compact["headerCount"] = headers.count
        compact["headerNames"] = headers.keys.sorted().joined(separator: ",")
    }
    return compact
}

private func compactWindowCloseTracePayload(
    _ payload: [String: Any]
) -> [String: Any] {
    var compact: [String: Any] = ["traceDetail": "compact"]
    copyTraceScalars(
        from: payload,
        keys: ["label", "reason", "windowId", "sessionId", "uiId", "transport"],
        into: &compact)
    return compact
}

func compactHostProtocolTracePayload(
    type: String,
    payload: [String: Any]
) -> [String: Any] {
    if payload["envelope"] is [String: Any] {
        return compactProtocolEnvelopeTracePayload(payload)
    }
    switch type {
    case "window.closeRequested":
        return compactWindowCloseTracePayload(payload)
    default:
        return payload
    }
}
