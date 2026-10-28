import Foundation
import CoreFoundation

func redactedSensitiveRpcValue(_ value: Any, key: String = "") -> Any {
    let normalizedKey = key.lowercased()
    if isSensitiveTraceKey(key) {
        return redactedTraceValue(value)
    }
    if normalizedKey.contains("password") || normalizedKey.contains("secret") {
        if let stringValue = value as? String {
            return "[redacted length=\(stringValue.count)]"
        }
        return "[redacted]"
    }
    if let dictionary = value as? [String: Any] {
        var redacted: [String: Any] = [:]
        for (childKey, childValue) in dictionary {
            redacted[childKey] = redactedSensitiveRpcValue(childValue, key: childKey)
        }
        return redacted
    }
    if let array = value as? [Any] {
        return array.map { redactedSensitiveRpcValue($0, key: key) }
    }
    return value
}

func redactedFlowIpcEnvelope(_ object: [String: Any]) -> [String: Any] {
    guard var payload = object["payload"] as? [String: Any] else {
        return object
    }
    var copy = object

    if let type = object["type"] as? String,
       type == "command" || type == "capabilityResult",
       payload["command"] as? String == "notification.show" {
        payload = redactedNotificationValue(payload) as? [String: Any] ?? [:]
        if type == "command",
           let args = payload["args"], !(args is [String: Any]) {
            payload["args"] = ["argumentsRedacted": true, "argumentsMalformed": true]
        }
    }

    if let command = payload["command"] as? String,
       command == "fileDialog.saveNewWriteGrant"
            || command == "fileGrant.write"
            || command == "fileGrant.releaseWrite"
            || command == "fileGrant.cancelWrite" {
        payload = redactedFileWriteGrantValue(payload) as? [String: Any] ?? [:]
        if object["type"] as? String == "command",
           let args = payload["args"], !(args is [String: Any]) {
            payload["args"] = ["argumentsRedacted": true, "argumentsMalformed": true]
        }
    }

    if let bodyBase64 = payload["bodyBase64"] as? String {
        payload["bodyBytes"] = Data(base64Encoded: bodyBase64)?.count ?? 0
        payload["bodyRedacted"] = true
        payload.removeValue(forKey: "bodyBase64")
    }

    if let uidl = payload["uidl"] as? String {
        payload["uidlBytes"] = uidl.utf8.count
        payload["uidlRedacted"] = true
        payload.removeValue(forKey: "uidl")
    }

    if payload["command"] as? String == "fileTransfer.download",
       var args = payload["args"] as? [String: Any],
       let body = args["body"] as? String {
        args["bodyBytes"] = body.utf8.count
        args["bodyRedacted"] = true
        args.removeValue(forKey: "body")
        args.removeValue(forKey: "bodyPreview")
        payload["args"] = args
    }

    if object["type"] as? String == "command",
       payload["command"] as? String == "clipboard.writeText" {
        if let args = payload["args"] as? [String: Any],
           let text = args["text"] as? String {
            payload["args"] = [
                "textLength": text.count,
                "textRedacted": true
            ]
        } else {
            payload["args"] = [
                "textRedacted": true,
                "textMalformed": true
            ]
        }
    }

    if object["type"] as? String == "capabilityResult",
       payload["command"] as? String == "clipboard.readText",
       payload.keys.contains("text") {
        let value = payload.removeValue(forKey: "text")
        payload.removeValue(forKey: "textLength")
        payload.removeValue(forKey: "textMalformed")
        payload["textRedacted"] = true
        if let text = value as? String {
            payload["textLength"] = text.count
        } else {
            payload["textMalformed"] = true
        }
    }

    if object["type"] as? String == "capabilityResult",
       let command = payload["command"] as? String,
       command == "fileDialog.openFile"
            || command == "fileDialog.openReadGrant"
            || command == "fileTransfer.download"
            || command == "fileTransfer.selectUploadFile" {
        for key in ["path", "paths", "filename"] where payload[key] != nil {
            payload[key] = redactedTraceValue(payload[key] as Any)
        }
    }

    if object["type"] as? String == "capabilityResult",
       let command = payload["command"] as? String,
       command == "fileDialog.openReadGrant"
            || command == "fileGrant.read"
            || command == "fileGrant.release",
       let error = payload["error"] {
        payload["error"] = redactedTraceValue(error)
    }

    copy["payload"] = redactedSensitiveRpcValue(payload)
    return copy
}

func redactedNotificationDiagnosticValue(_ value: Any, key: String) -> Any {
    switch key.lowercased() {
    case "nativeerrordomain":
        if let domain = value as? String, domain == "UNErrorDomain" || domain == "other" {
            return domain
        }
    case "nativeerrorcode":
        if let number = value as? NSNumber,
           CFGetTypeID(number) != CFBooleanGetTypeID(),
           ["c", "s", "i", "l", "q", "C", "S", "I", "L", "Q"]
                .contains(String(cString: number.objCType)) {
            // NSError.code is signed. Avoid Double conversion so even the Int64
            // limits remain exact, and reject unsigned values outside that range.
            let code = number.int64Value
            if NSNumber(value: code).compare(number) == .orderedSame { return code }
        }
    default:
        break
    }
    // Diagnostic fields are untrusted too: never accept a supplied trace marker.
    if let text = value as? String { return "[redacted length=\(text.count)]" }
    return "[redacted]"
}

private func redactedNotificationValue(_ value: Any, key: String = "") -> Any {
    switch key.lowercased() {
    case "nativeerrordomain", "nativeerrorcode":
        return redactedNotificationDiagnosticValue(value, key: key)
    case "title", "body", "bodypreview", "notificationid", "error", "userinfo":
        // Notification content and system identifiers are not trace metadata.
        // Always generate a fresh marker, even for caller-supplied markers.
        if let text = value as? String { return "[redacted length=\(text.count)]" }
        return "[redacted]"
    default:
        break
    }
    if let dictionary = value as? [String: Any] {
        var result: [String: Any] = [:]
        for (childKey, childValue) in dictionary {
            result[childKey] = redactedNotificationValue(childValue, key: childKey)
        }
        return result
    }
    if let array = value as? [Any] {
        return array.map { redactedNotificationValue($0) }
    }
    return value
}

private func redactedFileWriteGrantValue(_ value: Any, key: String = "") -> Any {
    switch key.lowercased() {
    case "grantid", "suggestedname", "filename", "path", "paths", "title", "error",
         "body", "bodypreview":
        // These values are untrusted input, including strings that happen to
        // start with the trace marker. Do not accept caller-supplied markers.
        if let text = value as? String { return "[redacted length=\(text.count)]" }
        return "[redacted]"
    default:
        break
    }
    if let dictionary = value as? [String: Any] {
        var result: [String: Any] = [:]
        let bodies = dictionary.filter { $0.key.lowercased() == "bodybase64" }
        let bodyMetadata = Set(["bodybytes", "bodyencodedbytes", "bodyredacted", "bodymalformed"])
        for (childKey, childValue) in dictionary {
            let normalized = childKey.lowercased()
            if normalized == "bodybase64"
                    || (!bodies.isEmpty && bodyMetadata.contains(normalized)) { continue }
            result[childKey] = redactedFileWriteGrantValue(childValue, key: childKey)
        }
        if !bodies.isEmpty {
            result["bodyRedacted"] = true
            if bodies.count == 1, let body = bodies.first?.value as? String {
                // Trace untrusted encoded length only. Validation/decoding is
                // bounded by the capability executor, never by trace logging.
                result["bodyEncodedBytes"] = body.utf8.count
            } else {
                result["bodyMalformed"] = true
            }
        }
        return result
    }
    if let array = value as? [Any] {
        return array.map { redactedFileWriteGrantValue($0) }
    }
    return value
}

func summarizedJavaStdoutLine(_ line: String) -> String {
    guard line.hasPrefix(sidecarWirePrefix) else {
        return line
    }
    let raw = String(line.dropFirst(sidecarWirePrefix.count))
    guard let data = raw.data(using: .utf8),
          let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
        return "VFIPC invalid bytes=\(raw.utf8.count)"
    }
    let type = object["type"] as? String ?? "unknown"
    let id = object["id"] as? String ?? ""
    let correlationId = object["correlationId"] as? String ?? ""
    let payload = object["payload"] as? [String: Any] ?? [:]
    let path = compactTraceString(payload["path"] as? String ?? "", maxLength: 256)
    return "VFIPC type=\(type) id=\(id) correlationId=\(correlationId) bytes=\(raw.utf8.count) payloadFields=\(payload.count) path=\(path)"
}
