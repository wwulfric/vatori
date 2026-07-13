import Foundation

func isSensitiveTraceKey(_ key: String) -> Bool {
    let normalized = key.lowercased()
    return normalized == "securitykey"
        || normalized == "csrftoken"
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
        "currentAppProduction",
        "broaderComponentResourceMatrix",
        "dataComponentResourceMatrix",
        "gridLazyDataProvider",
        "gridScrollVirtualization",
        "inputComponentParityMatrix",
        "multiValueNavigationDisclosureMatrix",
        "overlayContextMenuMatrix",
        "contentAppShellLoginMatrix",
        "richTextEditorMatrix",
        "richTextEditorBehaviorMatrix",
        "richTextEditorEditingMatrix"
    ]
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
        } else if let number = value as? NSNumber {
            compact[key] = number
        } else if let bool = value as? Bool {
            compact[key] = bool
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
        } else if let bool = value as? Bool {
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
        "frameworkApp",
        "frameworkRichContentComponentMatrix",
        "frameworkRichTextEditorMatrix",
        "frameworkRichTextEditorBehaviorMatrix",
        "frameworkRichTextEditorEditingMatrix",
        "contentAppShellLoginMatrix",
        "messageListMarkdownMatrix",
        "loginOverlayPropertyMatrix",
        "loginOverlayForgotPasswordEventMatrix",
        "loginOverlayLoginEventMatrix",
        "loginOverlayInvalidLoginEventMatrix",
        "loginOverlayCloseLifecycleMatrix",
        "loginOverlayRecoveryLoginMatrix",
        "loginOverlayServerReopenMatrix",
        "loginOverlayShowErrorMatrix",
        "loginOverlayErrorA11yMatrix",
        "loginOverlayErrorFocusMatrix",
        "loginOverlayErrorFocusRetryMatrix",
        "loginOverlayErrorFocusRetryTargetMatrix",
        "loginOverlayFullI18nMatrix",
        "loginOverlayNoAutofocusMatrix",
        "loginOverlayAutofocusControlMatrix",
        "loginOverlayEscCloseMatrix",
        "loginOverlayOutsideCloseMatrix",
        "loginOverlayTabTraversalMatrix",
        "loginOverlayShiftTabTraversalMatrix",
        "loginOverlayAutofillAttributesMatrix",
        "loginOverlayA11ySemanticsMatrix",
        "loginOverlayDisabledGuardMatrix",
        "loginOverlayDisabledKeyboardGuardMatrix",
        "loginOverlayActionPostMatrix",
        "loginOverlaySubmitButtonMatrix",
        "loginOverlayEnterSubmitMatrix",
        "loginFormPropertyMatrix",
        "loginFormLoginEventMatrix",
        "loginFormInvalidLoginEventMatrix",
        "loginFormForgotPasswordEventMatrix",
        "loginFormActionPostMatrix",
        "loginFormTabTraversalMatrix",
        "loginFormShiftTabTraversalMatrix",
        "loginFormAutofillAttributesMatrix",
        "loginFormA11ySemanticsMatrix",
        "loginFormFullI18nMatrix",
        "loginFormShowErrorMatrix",
        "loginFormErrorA11yMatrix",
        "loginFormErrorFocusMatrix",
        "loginFormErrorFocusRetryMatrix",
        "loginFormErrorFocusRetryTargetMatrix",
        "loginFormErrorFocusRetryTargetStateMatrix",
        "loginFormErrorFocusRetryTargetLayoutMatrix",
        "loginFormErrorFocusRetryTargetHiddenSourceMatrix",
        "loginFormErrorFocusRetryTargetVisibleOwnerMatrix",
        "loginFormErrorFocusRetryTargetVisibleOwnerFocusMatrix",
        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentMatrix",
        "loginFormErrorFocusRetryTargetVisibleOwnerFocusContainmentStabilityMatrix",
        "richTextEditorMatrix",
        "richTextEditorBehaviorMatrix",
        "richTextEditorEditingMatrix",
        "currentAppGeneratedImportsOwnerRuntimeContract",
        "currentAppGeneratedImportsOwnerSideEffect",
        "frontendResourcePipelineBasicClosure"
    ]
    var compact: [String: Any] = [:]
    for (key, value) in payload {
        let keepDiagnostic = diagnosticPrefixes.contains { key.hasPrefix($0) }
        if let text = value as? String {
            if alwaysKeep.contains(key) || keepDiagnostic || !text.isEmpty {
                compact[key] = compactTraceString(text, maxLength: 1024)
            }
        } else if let bool = value as? Bool {
            if bool || alwaysKeep.contains(key) || keepDiagnostic || key.hasSuffix("FixtureSubstituted") {
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
        "transport",
        "compatibilityMode",
        "serverSequence",
        "serverSequenceScope",
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
        "broaderComponentResourceMatrix",
        "dataComponentResourceMatrix",
        "gridLazyDataProvider",
        "gridScrollVirtualization",
        "inputComponentParityMatrix",
        "multiValueNavigationDisclosureMatrix",
        "overlayContextMenuMatrix",
        "contentAppShellLoginMatrix",
        "richTextEditorMatrix",
        "richTextEditorBehaviorMatrix",
        "richTextEditorEditingMatrix",
        "streamResource",
        "uploadReceiver",
        "uploadMultipart",
        "downloadHandlerSaveOpen",
        "frameworkStreamResource",
        "frameworkUpload",
        "frameworkDownloadHandler"
    ]
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
        } else if let bool = value as? Bool {
            if bool || key.hasSuffix("FixtureSubstituted") || key.hasSuffix("FullBodyOmitted") {
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
