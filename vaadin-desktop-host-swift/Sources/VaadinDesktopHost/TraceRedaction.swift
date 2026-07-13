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

func redactedUidlPayload(_ payload: [String: Any]) -> [String: Any] {
    var copy = compactFlowIpcTracePayload(payload)
    if let clientState = payload["clientState"] as? [String: Any] {
        let redactedClientState = redactedSensitiveRpcValue(clientState) as? [String: Any] ?? clientState
        copy["clientState"] = compactScalarTracePayload(redactedClientState, maxStringLength: 256)
    }
    if let flowClientResources = payload["flowClientResources"] as? [String: Any] {
        copy["flowClientResources"] = compactResourceManifestPayload(flowClientResources)
    }
    if let componentFrontendResources = payload["componentFrontendResources"] as? [String: Any] {
        copy["componentFrontendResources"] = compactResourceManifestPayload(componentFrontendResources)
    }
    guard var uidl = payload["uidl"] as? [String: Any],
          let body = uidl["body"] as? String else {
        return copy
    }
    uidl["bodyPreview"] = compactTraceString(body, maxLength: 768)
    uidl["bodyBytes"] = body.utf8.count
    uidl["bodyRedacted"] = true
    uidl["bodySearchIndex"] = uidlTraceSearchIndex(body)
    uidl.removeValue(forKey: "body")
    copy["uidl"] = uidl
    return copy
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

func redactedEventPayload(_ payload: [String: Any]) -> [String: Any] {
    redactedSensitiveRpcValue(payload) as? [String: Any] ?? payload
}

func redactedFlowIpcEnvelope(_ object: [String: Any]) -> [String: Any] {
    if let type = object["type"] as? String,
       ["bootstrapResponse", "push", "patch", "resync"].contains(type),
       let payload = object["payload"] as? [String: Any] {
        var copy = object
        copy["payload"] = redactedUidlPayload(payload)
        return copy
    }

    if object["type"] as? String == "event",
       let payload = object["payload"] as? [String: Any] {
        var copy = object
        copy["payload"] = redactedEventPayload(payload)
        return copy
    }

    if object["type"] as? String == "resourceResponse",
       var payload = object["payload"] as? [String: Any],
       let body = payload["body"] as? String {
        var copy = object
        payload = redactedSensitiveRpcValue(payload) as? [String: Any] ?? payload
        payload["bodyPreview"] = String(body.prefix(96))
        payload["bodyBytes"] = body.utf8.count
        payload["bodyRedacted"] = true
        payload.removeValue(forKey: "body")
        copy["payload"] = compactFlowIpcTracePayload(payload, maxStringLength: 768)
        return copy
    }

    if object["type"] as? String == "resourceResponse",
       let payload = object["payload"] as? [String: Any] {
        var copy = object
        let redacted = redactedSensitiveRpcValue(payload) as? [String: Any] ?? payload
        copy["payload"] = compactFlowIpcTracePayload(redacted, maxStringLength: 768)
        return copy
    }

    if object["type"] as? String == "resourceRequest",
       let payload = object["payload"] as? [String: Any] {
        var copy = object
        let redacted = redactedSensitiveRpcValue(payload) as? [String: Any] ?? payload
        copy["payload"] = compactFlowIpcTracePayload(redacted, maxStringLength: 768)
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

func redactedJavaStdoutLine(_ line: String) -> String {
    guard line.hasPrefix("VFIPC1 ") else {
        return line
    }
    let raw = String(line.dropFirst(7))
    guard let data = raw.data(using: .utf8),
          let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
          let redacted = try? JSONSerialization.data(withJSONObject: redactedFlowIpcEnvelope(object)),
          let text = String(data: redacted, encoding: .utf8) else {
        return line
    }
    return "VFIPC1 \(text)"
}
