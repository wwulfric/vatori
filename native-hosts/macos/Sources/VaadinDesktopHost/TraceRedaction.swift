import Foundation

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

    copy["payload"] = redactedSensitiveRpcValue(payload)
    return copy
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
