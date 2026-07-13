import Foundation

final class FlowIpcResourceStager {
    private let repoRoot: String
    private let resourceSchemeHandler: FlowIpcResourceSchemeHandler
    private var stageCache: Set<String> = []

    init(repoRoot: String, resourceSchemeHandler: FlowIpcResourceSchemeHandler) {
        self.repoRoot = repoRoot
        self.resourceSchemeHandler = resourceSchemeHandler
    }

    private var stagingDirectory: URL {
        URL(fileURLWithPath: repoRoot)
            .appendingPathComponent(".build")
            .appendingPathComponent("vaadin-desktop-host-swift")
            .appendingPathComponent("flow-ipc-resources")
    }

    func prepareStagingDirectory() {
        let directory = stagingDirectory
        try? FileManager.default.removeItem(at: directory)
        do {
            try FileManager.default.createDirectory(at: directory,
                                                    withIntermediateDirectories: true)
        } catch {
            trace("send", "resourceLoad", compactResourceLoadTrace([
                "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceLoaderNoLocalhost": true,
                "error": error.localizedDescription,
                "fileDirectoryRedacted": "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources"
            ]))
        }
    }

    func handleStageRequest(_ payload: [String: Any]) -> [String: Any] {
        let requestId = payload["requestId"] as? String ?? ""
        let sourcePath = payload["sourcePath"] as? String ?? ""
        let body = payload["body"] as? String ?? ""
        let sha256 = payload["sha256"] as? String ?? ""
        let artifactFamily = payload["artifactFamily"] as? String ?? ""
        let relativeImportBase = payload["relativeImportBase"] as? String ?? sourcePath
        let mimeType = payload["mimeType"] as? String ?? Self.mimeTypeForFrontendResource(path: sourcePath)
        let resourceNamespace = payload["resourceNamespace"] as? String ?? ""
        let resourcePathMode = payload["resourcePathMode"] as? String ?? ""

        let data = Data(body.utf8)
        let cacheKey = "\(sourcePath)#\(sha256)#\(data.count)"
        let cacheStatus = stageCache.contains(cacheKey) ? "hit" : "miss"
        stageCache.insert(cacheKey)

        let useStableSourcePath = resourcePathMode == "stable-source-path"
            || resourceNamespace == "production-owner"
        let stableComponents = useStableSourcePath
            ? Self.stableResourcePathComponents(namespace: resourceNamespace, sourcePath: sourcePath)
            : []
        let resourceKey = Self.sanitizedResourceKey(sourcePath: sourcePath, sha256: sha256, requestId: requestId)
        let fileName = useStableSourcePath
            ? (stableComponents.last ?? Self.sanitizedResourceFileName(sourcePath))
            : Self.sanitizedResourceFileName(sourcePath)
        let directory = useStableSourcePath
            ? Self.appendingPathComponents(Array(stableComponents.dropLast()), to: stagingDirectory)
            : stagingDirectory.appendingPathComponent(resourceKey)
        let fileURL = directory.appendingPathComponent(fileName)
        let urlPath = useStableSourcePath
            ? "/" + stableComponents.joined(separator: "/")
            : "/\(resourceKey)/\(fileName)"
        let stagedUrl = "\(FlowIpcResourceSchemeHandler.schemeName)://flow-ipc\(urlPath)"
        let redactedRelativePath = useStableSourcePath
            ? stableComponents.joined(separator: "/")
            : "\(resourceKey)/\(fileName)"
        let filePathRedacted = "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources/\(redactedRelativePath)"
        let fileDirectoryRedacted = "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources"
        let resourceMetadata = self.resourceMetadata(from: payload,
                                                     resourceNamespace: resourceNamespace,
                                                     sourcePath: sourcePath,
                                                     useStableSourcePath: useStableSourcePath,
                                                     urlPath: urlPath)

        trace("recv", "resourceLoad", compactResourceLoadTrace(baseTracePayload(
            requestId: requestId,
            stagedUrl: stagedUrl,
            sourcePath: sourcePath,
            artifactFamily: artifactFamily,
            mimeType: mimeType,
            bytes: data.count,
            sha256: sha256,
            cacheStatus: cacheStatus,
            relativeImportBase: relativeImportBase,
            filePathRedacted: filePathRedacted,
            fileDirectoryRedacted: fileDirectoryRedacted,
            resourceMetadata: resourceMetadata
        )))

        do {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            try data.write(to: fileURL, options: [.atomic])
            resourceSchemeHandler.register(urlPath: urlPath,
                                           entry: FlowIpcHostResourceEntry(sourcePath: sourcePath,
                                                                          fileURL: fileURL,
                                                                          mimeType: mimeType,
                                                                          sha256: sha256,
                                                                          bytes: data.count,
                                                                          cacheStatus: cacheStatus,
                                                                          relativeImportBase: relativeImportBase,
                                                                          metadata: resourceMetadata))
            let response = stageResponsePayload(requestId: requestId,
                                                stagedUrl: stagedUrl,
                                                sourcePath: sourcePath,
                                                artifactFamily: artifactFamily,
                                                mimeType: mimeType,
                                                bytes: data.count,
                                                sha256: sha256,
                                                cacheStatus: cacheStatus,
                                                relativeImportBase: relativeImportBase,
                                                filePathRedacted: filePathRedacted,
                                                fileDirectoryRedacted: fileDirectoryRedacted,
                                                resourceMetadata: resourceMetadata)
            trace("send", "resourceLoad", compactResourceLoadTrace(baseTracePayload(
                requestId: requestId,
                stagedUrl: stagedUrl,
                sourcePath: sourcePath,
                artifactFamily: artifactFamily,
                mimeType: mimeType,
                bytes: data.count,
                sha256: sha256,
                cacheStatus: cacheStatus,
                relativeImportBase: relativeImportBase,
                filePathRedacted: filePathRedacted,
                fileDirectoryRedacted: fileDirectoryRedacted,
                resourceMetadata: resourceMetadata
            )))
            return response
        } catch {
            return [
                "requestId": requestId,
                "success": false,
                "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceLoaderNoLocalhost": true,
                "sourcePath": sourcePath,
                "artifactFamily": artifactFamily,
                "mimeType": mimeType,
                "bytes": data.count,
                "sha256": sha256,
                "cacheStatus": cacheStatus,
                "error": error.localizedDescription,
                "fileDirectoryRedacted": fileDirectoryRedacted
            ]
        }
    }

    private func baseTracePayload(requestId: String,
                                  stagedUrl: String,
                                  sourcePath: String,
                                  artifactFamily: String,
                                  mimeType: String,
                                  bytes: Int,
                                  sha256: String,
                                  cacheStatus: String,
                                  relativeImportBase: String,
                                  filePathRedacted: String,
                                  fileDirectoryRedacted: String,
                                  resourceMetadata: [String: Any]) -> [String: Any] {
        var payload: [String: Any] = [
            "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
            "frontendResourceSchemeName": FlowIpcResourceSchemeHandler.schemeName,
            "frontendResourceLoaderMode": "custom-scheme-file-backed",
            "stagingMode": "custom-scheme",
            "frontendResourceSchemeHandlerInstalled": true,
            "frontendResourceSchemeRequestObserved": false,
            "frontendResourceSchemeResponseObserved": false,
            "frontendResourceLoaderNoLocalhost": true,
            "frontendResourceFileBacked": true,
            "requestId": requestId,
            "stagedUrl": stagedUrl,
            "sourcePath": sourcePath,
            "artifactFamily": artifactFamily,
            "mimeType": mimeType,
            "bytes": bytes,
            "sha256": sha256,
            "cacheStatus": cacheStatus,
            "relativeImportBase": relativeImportBase,
            "resourceBaseUrl": stagedUrl,
            "cleanupPolicy": FlowIpcResourceSchemeContract.cleanupPolicy,
            "filePathRedacted": filePathRedacted,
            "fileDirectoryRedacted": fileDirectoryRedacted
        ]
        for (key, value) in resourceMetadata {
            payload[key] = value
        }
        return payload
    }

    private func stageResponsePayload(requestId: String,
                                      stagedUrl: String,
                                      sourcePath: String,
                                      artifactFamily: String,
                                      mimeType: String,
                                      bytes: Int,
                                      sha256: String,
                                      cacheStatus: String,
                                      relativeImportBase: String,
                                      filePathRedacted: String,
                                      fileDirectoryRedacted: String,
                                      resourceMetadata: [String: Any]) -> [String: Any] {
        var payload: [String: Any] = [
            "requestId": requestId,
            "success": true,
            "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
            "frontendResourceSchemeName": FlowIpcResourceSchemeHandler.schemeName,
            "frontendResourceLoaderMode": "custom-scheme-file-backed",
            "stagingMode": "custom-scheme",
            "frontendResourceSchemeHandlerInstalled": true,
            "frontendResourceSchemeRequestObserved": false,
            "frontendResourceSchemeResponseObserved": false,
            "frontendResourceLoaderNoLocalhost": true,
            "frontendResourceFileBacked": true,
            "stagedUrl": stagedUrl,
            "resourceBaseUrl": stagedUrl,
            "relativeImportBase": relativeImportBase,
            "relativeImportResolutionMode": FlowIpcResourceSchemeContract.relativeImportResolutionMode,
            "resolvedRelativeImportPaths": "",
            "missingRelativeImportPaths": "",
            "sourcePath": sourcePath,
            "artifactFamily": artifactFamily,
            "mimeType": mimeType,
            "bytes": bytes,
            "sha256": sha256,
            "cacheStatus": cacheStatus,
            "cleanupPolicy": FlowIpcResourceSchemeContract.cleanupPolicy,
            "filePathRedacted": filePathRedacted,
            "fileDirectoryRedacted": fileDirectoryRedacted,
            "noLocalhostProof": FlowIpcResourceSchemeContract.noLocalhostProof
        ]
        for (key, value) in resourceMetadata {
            if payload[key] == nil {
                payload[key] = value
            }
        }
        return payload
    }

    private func resourceMetadata(from payload: [String: Any],
                                  resourceNamespace: String,
                                  sourcePath: String,
                                  useStableSourcePath: Bool,
                                  urlPath: String) -> [String: Any] {
        let excludedKeys: Set<String> = [
            "requestId",
            "body",
            "sourcePath",
            "sha256",
            "artifactFamily",
            "relativeImportBase",
            "mimeType",
            "resourceNamespace",
            "resourcePathMode",
            "securityKey",
            "csrfToken",
            "Vaadin-Security-Key"
        ]
        var metadata = payload.filter { key, _ in !excludedKeys.contains(key) }
        metadata["productionBundleRuntimeOwnerResourceLoaderNamespace"] = resourceNamespace
        metadata["productionBundleRuntimeOwnerResourceLoaderArtifact"] = sourcePath
        metadata["productionBundleRuntimeOwnerResourcePathMode"] = useStableSourcePath
            ? "stable-source-path"
            : "hash-resource-key"
        metadata["productionBundleRuntimeOwnerResourceUrlPath"] = urlPath
        return metadata
    }

    private static func sanitizedResourceKey(sourcePath: String, sha256: String, requestId: String) -> String {
        let hashPrefix = sha256.isEmpty ? sanitizedPathComponent(requestId) : String(sha256.prefix(16))
        let name = sanitizedPathComponent(sourcePath)
        let suffix = name.isEmpty ? "resource" : String(name.prefix(80))
        return "\(hashPrefix)-\(suffix)"
    }

    private static func sanitizedResourceFileName(_ sourcePath: String) -> String {
        let last = URL(fileURLWithPath: sourcePath).lastPathComponent
        let candidate = last.isBlank ? "resource.js" : last
        let sanitized = sanitizedPathComponent(candidate)
        if sanitized.contains(".") {
            return sanitized
        }
        return "\(sanitized).js"
    }

    private static func stableResourcePathComponents(namespace: String, sourcePath: String) -> [String] {
        let sanitizedNamespace = sanitizedPathComponent(namespace).isBlank
            ? "resource"
            : sanitizedPathComponent(namespace)
        var components = sourcePath
            .split(separator: "/")
            .map { sanitizedPathComponent(String($0)) }
            .filter { !$0.isBlank }
        if components.isEmpty {
            components = [sanitizedResourceFileName(sourcePath)]
        }
        return [sanitizedNamespace] + components
    }

    private static func appendingPathComponents(_ components: [String], to baseURL: URL) -> URL {
        var url = baseURL
        for component in components {
            url.appendPathComponent(component)
        }
        return url
    }

    private static func sanitizedPathComponent(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "._-"))
        var result = ""
        for scalar in value.unicodeScalars {
            if allowed.contains(scalar) {
                result.append(String(scalar))
            } else {
                result.append("-")
            }
        }
        return result.trimmingCharacters(in: CharacterSet(charactersIn: "-"))
    }

    private static func mimeTypeForFrontendResource(path: String) -> String {
        if path.hasSuffix(".css") {
            return "text/css; charset=utf-8"
        }
        if path.hasSuffix(".json") {
            return "application/json; charset=utf-8"
        }
        if path.hasSuffix(".js") || path.hasSuffix(".mjs") || path.isBlank {
            return "text/javascript; charset=utf-8"
        }
        return "application/octet-stream"
    }
}
