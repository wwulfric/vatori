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

private func optionalResourceLoadTracePrefixes() -> [String] {
    []
}

private func optionalDomTracePrefixes() -> [String] {
    []
}

private func optionalEnvelopeTracePrefixes() -> [String] {
    []
}

func isSensitiveTraceKey(_ key: String) -> Bool {
    let normalized = key.lowercased()
    return normalized == "securitykey"
        || normalized == "csrftoken"
        || normalized == "pushid"
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

func compactResourceLoadTrace(_ payload: [String: Any]) -> [String: Any] {
    let scalarKeys: Set<String> = [
        "frontendResourceSchemeVersion",
        "frontendResourceSchemeName",
        "frontendResourceLoaderMode",
        "stagingMode",
        "frontendResourceSchemeHandlerInstalled",
        "frontendResourceSchemeRequestObserved",
        "frontendResourceSchemeResponseObserved",
        "frontendResourceLoaderNoLocalhost",
        "frontendResourceFileBacked",
        "requestId",
        "resourceUrl",
        "resourceUrlPath",
        "sourcePath",
        "stagedUrl",
        "artifactFamily",
        "moduleSpecifier",
        "mimeType",
        "bytes",
        "sha256",
        "cacheStatus",
        "relativeImportBase",
        "resourceBaseUrl",
        "cleanupPolicy",
        "filePathRedacted",
        "fileDirectoryRedacted",
        "noLocalhostProof",
        "executionStatus",
        "failureLayer",
        "browserError",
        "error"
    ]
    let scalarPrefixes = [
        "productionBundleRuntimeOwner",
        "packagedFrontend",
        "frontendResolver",
        "frontendPipelineClosure",
        "generatedLazy",
        "currentAppFrontendBuild",
        "currentAppBuildFrontend",
        "currentAppGenerated",
        "currentAppProduction"
    ] + optionalResourceLoadTracePrefixes()
    var compact: [String: Any] = [:]
    for (key, value) in payload {
        let keep = scalarKeys.contains(key)
            || scalarPrefixes.contains { key.hasPrefix($0) }
        if !keep {
            continue
        }
        if let text = value as? String {
            compact[key] = text.count <= 512
                ? text
                : String(text.prefix(512)) + "...[truncated]"
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
    return compact
}

func compactTraceString(_ text: String, maxLength: Int = 1024) -> String {
    text.count <= maxLength
        ? text
        : String(text.prefix(maxLength)) + "...[truncated]"
}

func compactScalarTracePayload(_ payload: [String: Any],
                               maxStringLength: Int = 1024) -> [String: Any] {
    var compact: [String: Any] = [:]
    for (key, value) in payload {
        if isSensitiveTraceKey(key) {
            compact[key] = redactedTraceValue(value)
            continue
        }
        if let text = value as? String {
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
        } else if let strings = value as? [String] {
            compact[key] = compactTraceString(strings.joined(separator: ","), maxLength: maxStringLength)
        } else if let numbers = value as? [NSNumber] {
            compact[key] = numbers.map { "\($0)" }.joined(separator: ",")
        }
    }
    return compact
}

func compactWebViewDomTracePayload(_ payload: [String: Any]) -> [String: Any] {
    let alwaysKeep: Set<String> = [
        "readyState",
        "title",
        "location",
        "bodyText",
        "hasVaadinRoot",
        "elementCount"
    ]
    let diagnosticPrefixes = [
        "flowClientEngine",
        "standardBundle",
        "currentAppGeneratedImportsOwnerRuntimeContract",
        "currentAppGeneratedImportsOwnerSideEffect",
        "frontendResourcePipelineBasicClosure"
    ] + optionalDomTracePrefixes()
    var compact: [String: Any] = [:]
    for (key, value) in payload {
        let keepDiagnostic = diagnosticPrefixes.contains { key.hasPrefix($0) }
        if let text = value as? String {
            if alwaysKeep.contains(key) || keepDiagnostic || !text.isEmpty {
                compact[key] = compactTraceString(text, maxLength: 1024)
            }
        } else if let bool = traceBoolean(value) {
            let keepBoolean = bool || alwaysKeep.contains(key) || keepDiagnostic
            if keepBoolean {
                compact[key] = bool
            }
        } else if let number = value as? NSNumber {
            let doubleValue = number.doubleValue
            if alwaysKeep.contains(key) || keepDiagnostic || (doubleValue != 0 && doubleValue != -1) {
                compact[key] = number
            }
        } else if let int = value as? Int {
            if alwaysKeep.contains(key) || keepDiagnostic || (int != 0 && int != -1) {
                compact[key] = int
            }
        } else if let int64 = value as? Int64 {
            if alwaysKeep.contains(key) || keepDiagnostic || (int64 != 0 && int64 != -1) {
                compact[key] = int64
            }
        } else if let double = value as? Double {
            if alwaysKeep.contains(key) || keepDiagnostic || (double != 0 && double != -1) {
                compact[key] = double
            }
        } else if let strings = value as? [String], !strings.isEmpty {
            compact[key] = compactTraceString(strings.joined(separator: ","), maxLength: 1024)
        } else if let numbers = value as? [NSNumber], !numbers.isEmpty {
            compact[key] = numbers.map { "\($0)" }.joined(separator: ",")
        }
    }
    return compact
}

func compactFlowIpcTracePayload(_ payload: [String: Any],
                                maxStringLength: Int = 1024) -> [String: Any] {
    let scalarKeys: Set<String> = [
        "requestId",
        "correlationId",
        "route",
        "sessionId",
        "uiId",
        "windowId",
        "transport",
        "serverSequence",
        "serverSequenceScope",
        "chunkSequence",
        "chunkCount",
        "credit",
        "contentLength",
        "resourceUrl",
        "resourceUrlPath",
        "sourcePath",
        "path",
        "method",
        "status",
        "statusCode",
        "mimeType",
        "contentType",
        "bytes",
        "sha256",
        "range",
        "bodyPreview",
        "bodyBytes",
        "bodyRedacted",
        "fileName",
        "failureLayer",
        "nextResolutionStep",
        "noLocalhostProof"
    ]
    let prefixes = [
        "frontendResource",
        "flowClientResource",
        "flowClientEngine",
        "frontendResolver",
        "frontendPipelineClosure",
        "packagedFrontend",
        "packagingFrontendInventory",
        "packagingRuntimeHandoffReuse",
        "generatedLazy",
        "generatedLoadOnDemand",
        "loadOnDemand",
        "currentApp",
        "productionBundleRuntimeOwner",
        "componentProductionOwner",
        "streamResource",
        "uploadReceiver",
        "uploadMultipart",
        "downloadHandlerSaveOpen"
    ] + optionalEnvelopeTracePrefixes()
    var compact: [String: Any] = [:]
    for (key, value) in payload {
        let keep = scalarKeys.contains(key)
            || prefixes.contains { key.hasPrefix($0) }
            || isSensitiveTraceKey(key)
        if !keep {
            continue
        }
        if isSensitiveTraceKey(key) {
            compact[key] = redactedTraceValue(value)
            continue
        }
        if let text = value as? String {
            if !text.isEmpty || key.hasSuffix("UnresolvedSpecifiers") {
                compact[key] = compactTraceString(text, maxLength: maxStringLength)
            }
        } else if let bool = traceBoolean(value) {
            let keepBoolean = bool || key.hasSuffix("FullBodyOmitted")
            if keepBoolean {
                compact[key] = bool
            }
        } else if let number = value as? NSNumber {
            let doubleValue = number.doubleValue
            if doubleValue != 0 && doubleValue != -1 {
                compact[key] = number
            }
        } else if let int = value as? Int {
            if int != 0 && int != -1 {
                compact[key] = int
            }
        } else if let int64 = value as? Int64 {
            if int64 != 0 && int64 != -1 {
                compact[key] = int64
            }
        } else if let double = value as? Double {
            if double != 0 && double != -1 {
                compact[key] = double
            }
        } else if let strings = value as? [String], !strings.isEmpty {
            compact[key] = compactTraceString(strings.joined(separator: ","), maxLength: maxStringLength)
        }
    }
    return compact
}

func compactResourceManifestPayload(_ payload: [String: Any]) -> [String: Any] {
    var compact = compactFlowIpcTracePayload(payload, maxStringLength: 1024)
    if let artifactFamilies = payload["artifactFamilies"] as? [String], !artifactFamilies.isEmpty {
        compact["artifactFamilies"] = artifactFamilies.joined(separator: ",")
    }
    if let resources = payload["resources"] as? [[String: Any]] {
        var paths: [String] = []
        var families = Set<String>()
        var ipcAugmented = false
        for resource in resources {
            if let path = resource["path"] as? String, !path.isEmpty {
                paths.append(path)
            } else if let moduleSpecifier = resource["moduleSpecifier"] as? String, !moduleSpecifier.isEmpty {
                paths.append(moduleSpecifier)
            }
            if let family = resource["artifactFamily"] as? String, !family.isEmpty {
                families.insert(family)
            }
            if resource["ipcAugmented"] as? Bool == true {
                ipcAugmented = true
            }
        }
        compact["resourceCount"] = resources.count
        compact["resourcePaths"] = compactTraceString(paths.joined(separator: ","), maxLength: 4096)
        compact["resourceFamilies"] = families.sorted().joined(separator: ",")
        if ipcAugmented {
            compact["ipcAugmented"] = true
        }
        compact["resourcesRedacted"] = true
    }
    return compact
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

private func compactEnvelopeTracePayload(_ payload: [String: Any]) -> [String: Any] {
    var compact: [String: Any] = ["traceDetail": "compact"]
    copyTraceScalars(
        from: payload,
        keys: ["source", "target", "transport", "command"],
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
            "path", "url", "method", "requestId", "purpose", "requestPurpose",
            "lookupKind", "artifactFamily", "status", "statusText", "statusCode",
            "mimeType", "contentType", "bytes", "sha256",
            "bodyBytes", "bodyRedacted", "serverSequence",
            "chunkSequence", "chunkCount", "credit", "contentLength",
            "failureLayer", "noLocalhostProof", "connectionId", "flowUiId",
            "pushSequence", "lastSeenServerSyncId", "resync", "uidlBytes",
            "uidlRedacted"
        ],
        into: &compact,
        maxStringLength: 384)
    if let flowResources = envelopePayload["flowClientResources"] as? [String: Any] {
        compact["flowClientResourceCount"] = flowResources["resourceCount"]
            ?? flowResources["count"]
            ?? (flowResources["resources"] as? [Any])?.count
            ?? 0
    }
    if let componentResources = envelopePayload["componentFrontendResources"] as? [String: Any] {
        compact["componentFrontendResourceCount"] = componentResources["resourceCount"]
            ?? componentResources["count"]
            ?? (componentResources["resources"] as? [Any])?.count
            ?? 0
    }
    if let uidl = envelopePayload["uidl"] as? [String: Any] {
        compact["uidlBodyBytes"] = uidl["bodyBytes"]
            ?? (uidl["body"] as? String)?.utf8.count
            ?? 0
    }
    if let bodyBase64 = envelopePayload["bodyBase64"] as? String {
        compact["bodyBytes"] = Data(base64Encoded: bodyBase64)?.count ?? 0
        compact["bodyRedacted"] = true
    }
    if let headers = envelopePayload["headers"] as? [String: Any] {
        compact["headerCount"] = headers.count
        compact["headerNames"] = headers.keys.sorted().joined(separator: ",")
    }
    return compact
}

private func compactWebViewTracePayload(_ payload: [String: Any]) -> [String: Any] {
    var compact: [String: Any] = [
        "traceDetail": "compact",
        "fieldCount": payload.count
    ]
    copyTraceScalars(
        from: payload,
        keys: [
            "label", "readyState", "title", "location", "hasVaadinRoot", "elementCount",
            "resultBytes", "resultRedacted", "flowClientEngineErrorCode",
            "flowClientEngineInitialized", "flowClientEngineBootstrapDomBound",
            "flowClientEngineEventPatchConsumed", "flowClientEngineResyncPatchConsumed"
        ],
        into: &compact,
        maxStringLength: 384)
    var trueCount = 0
    var falseCount = 0
    var nonEmptyStringCount = 0
    var completeCount = 0
    for (key, value) in payload {
        if let bool = traceBoolean(value) {
            if bool {
                trueCount += 1
                if key.hasSuffix("Complete") {
                    completeCount += 1
                }
            } else {
                falseCount += 1
            }
        } else if let text = value as? String, !text.isEmpty {
            nonEmptyStringCount += 1
        }
    }
    compact["trueFieldCount"] = trueCount
    compact["falseFieldCount"] = falseCount
    compact["completeFieldCount"] = completeCount
    compact["nonEmptyStringFieldCount"] = nonEmptyStringCount
    return compact
}

private func compactCloseTracePayload(_ payload: [String: Any]) -> [String: Any] {
    var compact: [String: Any] = ["traceDetail": "compact"]
    copyTraceScalars(from: payload, keys: ["label", "reason"], into: &compact)
    if let readiness = payload["readiness"] as? String {
        compact["readinessBytes"] = readiness.utf8.count
        compact["readinessComplete"] = readiness.contains("\"complete\":true")
    }
    return compact
}

func compactHostProtocolTracePayload(type: String, payload: [String: Any]) -> [String: Any] {
    switch type {
    case "opaqueHttpRequest", "opaqueHttpResponse", "opaquePush",
         "opaqueHttpStreamOpen", "opaqueHttpStreamChunk", "opaqueHttpStreamEnd",
         "opaqueHttpStreamCancel", "opaqueHttpStreamRequestCredit",
         "opaqueHttpStreamResponse", "opaqueHttpStreamResponseChunk",
         "opaqueHttpStreamResponseEnd", "opaqueHttpStreamResponseCredit",
         "opaqueHttpStreamError",
         "opaquePushConnected", "opaquePushConnect", "opaquePushDisconnect",
         "opaquePushResync":
        return compactEnvelopeTracePayload(payload)
    case "webview.dom":
        return compactWebViewTracePayload(payload)
    case "window.closeRequested":
        return compactCloseTracePayload(payload)
    default:
        return payload
    }
}
