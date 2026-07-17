import Foundation

func uidlTraceSearchIndex(_ body: String) -> String {
    let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "._@/-:$"))
    let tokens = body.components(separatedBy: allowed.inverted)
    var seen = Set<String>()
    var result: [String] = []
    var totalLength = 0
    for token in tokens {
        if token.count < 2 || token.count > 220 || seen.contains(token) {
            continue
        }
        let keep = token.hasPrefix("framework-")
            || token.hasPrefix("com.vaadin.")
            || token.hasPrefix("dev.vatori.")
            || token.hasPrefix("@vaadin/")
            || token.hasPrefix("VAADIN/")
            || token.contains("flow-ipc")
            || token.contains("Flow.loadOnDemand")
            || token.contains("LoadMode")
            || token.contains("uidl")
            || token.contains("UIDL")
            || token == "LAZY"
            || token == "DYNAMIC_IMPORT"
            || token == "setViewportRange"
            || token == "scrollToIndex"
            || token == "flow-client-engine-visible"
        if !keep {
            continue
        }
        seen.insert(token)
        result.append(token)
        totalLength += token.count + 1
        if totalLength > 80_000 {
            break
        }
    }
    return result.joined(separator: ",")
}

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
    if let type = object["type"] as? String,
       type.hasPrefix("opaqueHttpStream"),
       var payload = object["payload"] as? [String: Any],
       let bodyBase64 = payload["bodyBase64"] as? String {
        var copy = object
        payload["bodyBytes"] = Data(base64Encoded: bodyBase64)?.count ?? 0
        payload["bodyRedacted"] = true
        payload.removeValue(forKey: "bodyBase64")
        copy["payload"] = redactedSensitiveRpcValue(payload)
        return copy
    }

    if object["type"] as? String == "opaquePush",
       var payload = object["payload"] as? [String: Any],
       let uidl = payload["uidl"] as? String {
        var copy = object
        payload["uidlBytes"] = uidl.utf8.count
        payload["uidlRedacted"] = true
        payload["uidlSearchIndex"] = uidlTraceSearchIndex(uidl)
        payload.removeValue(forKey: "uidl")
        copy["payload"] = payload
        return copy
    }

    if let type = object["type"] as? String,
       ["opaquePushConnect", "opaquePushDisconnect", "opaquePushResync"].contains(type),
       let payload = object["payload"] as? [String: Any] {
        var copy = object
        copy["payload"] = redactedSensitiveRpcValue(payload)
        return copy
    }

    if object["type"] as? String == "command",
       var payload = object["payload"] as? [String: Any],
       payload["command"] as? String == "fileTransfer.download",
       var args = payload["args"] as? [String: Any],
       let body = args["body"] as? String {
        var copy = object
        args["bodyPreview"] = String(body.prefix(32))
        args["bodyBytes"] = body.utf8.count
        args["bodyRedacted"] = true
        args.removeValue(forKey: "body")
        payload["args"] = args
        copy["payload"] = payload
        return copy
    }

    return object
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
