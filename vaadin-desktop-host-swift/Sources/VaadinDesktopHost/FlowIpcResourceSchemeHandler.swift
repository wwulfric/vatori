import Foundation
import WebKit

struct FlowIpcHostResourceEntry {
    let sourcePath: String
    let fileURL: URL
    let mimeType: String
    let sha256: String
    let bytes: Int
    let cacheStatus: String
    let relativeImportBase: String
    let metadata: [String: Any]
}

final class FlowIpcResourceSchemeHandler: NSObject, WKURLSchemeHandler {
    static let schemeName = "vatori-resource"
    private let queue = DispatchQueue(label: "dev.vatori.desktop.flow-ipc-resource-scheme")
    private var resources: [String: FlowIpcHostResourceEntry] = [:]

    func register(urlPath: String, entry: FlowIpcHostResourceEntry) {
        queue.sync {
            resources[urlPath] = entry
        }
    }

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let url = urlSchemeTask.request.url else {
            urlSchemeTask.didFailWithError(NSError(domain: "dev.vatori.desktop.resource-scheme",
                                                   code: 400,
                                                   userInfo: [NSLocalizedDescriptionKey: "missing resource URL"]))
            return
        }
        let urlPath = url.path
        let entry = queue.sync { resources[urlPath] }
        var requestTrace: [String: Any] = [
            "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
            "frontendResourceSchemeName": Self.schemeName,
            "frontendResourceLoaderMode": "custom-scheme-file-backed",
            "stagingMode": "custom-scheme",
            "frontendResourceSchemeHandlerInstalled": true,
            "frontendResourceSchemeRequestObserved": true,
            "frontendResourceSchemeResponseObserved": false,
            "frontendResourceLoaderNoLocalhost": true,
            "resourceUrl": url.absoluteString,
            "resourceUrlPath": urlPath,
            "sourcePath": entry?.sourcePath ?? "",
            "mimeType": entry?.mimeType ?? "",
            "sha256": entry?.sha256 ?? "",
            "bytes": entry?.bytes ?? 0,
            "cacheStatus": entry?.cacheStatus ?? "missing",
            "relativeImportBase": entry?.relativeImportBase ?? "",
            "cleanupPolicy": FlowIpcResourceSchemeContract.cleanupPolicy
        ]
        if let metadata = entry?.metadata {
            for (key, value) in metadata {
                requestTrace[key] = value
            }
        }
        trace("recv", "resourceLoad", compactResourceLoadTrace(requestTrace))

        guard let entry else {
            let response = URLResponse(url: url,
                                       mimeType: "text/plain",
                                       expectedContentLength: 0,
                                       textEncodingName: "utf-8")
            urlSchemeTask.didReceive(response)
            urlSchemeTask.didFailWithError(NSError(domain: "dev.vatori.desktop.resource-scheme",
                                                   code: 404,
                                                   userInfo: [NSLocalizedDescriptionKey: "resource not registered"]))
            return
        }

        do {
            let data = try Data(contentsOf: entry.fileURL)
            let headers = [
                "Content-Type": entry.mimeType,
                "Content-Length": String(data.count),
                "Cache-Control": "no-store",
                "Access-Control-Allow-Origin": "*",
                "Cross-Origin-Resource-Policy": "cross-origin"
            ]
            if let response = HTTPURLResponse(url: url,
                                              statusCode: 200,
                                              httpVersion: "HTTP/1.1",
                                              headerFields: headers) {
                urlSchemeTask.didReceive(response)
            } else {
                let response = URLResponse(url: url,
                                           mimeType: entry.mimeType,
                                           expectedContentLength: data.count,
                                           textEncodingName: "utf-8")
                urlSchemeTask.didReceive(response)
            }
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
            var responseTrace: [String: Any] = [
                "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
                "frontendResourceSchemeName": Self.schemeName,
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceSchemeHandlerInstalled": true,
                "frontendResourceSchemeRequestObserved": true,
                "frontendResourceSchemeResponseObserved": true,
                "frontendResourceLoaderNoLocalhost": true,
                "resourceUrl": url.absoluteString,
                "resourceUrlPath": urlPath,
                "sourcePath": entry.sourcePath,
                "mimeType": entry.mimeType,
                "sha256": entry.sha256,
                "bytes": data.count,
                "cacheStatus": entry.cacheStatus,
                "relativeImportBase": entry.relativeImportBase,
                "cleanupPolicy": FlowIpcResourceSchemeContract.cleanupPolicy,
                "executionStatus": "served"
            ]
            for (key, value) in entry.metadata {
                responseTrace[key] = value
            }
            trace("send", "resourceLoad", compactResourceLoadTrace(responseTrace))
        } catch {
            urlSchemeTask.didFailWithError(error)
            var failureTrace: [String: Any] = [
                "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
                "frontendResourceSchemeName": Self.schemeName,
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceSchemeHandlerInstalled": true,
                "frontendResourceSchemeRequestObserved": true,
                "frontendResourceSchemeResponseObserved": false,
                "frontendResourceLoaderNoLocalhost": true,
                "resourceUrl": url.absoluteString,
                "resourceUrlPath": urlPath,
                "sourcePath": entry.sourcePath,
                "mimeType": entry.mimeType,
                "sha256": entry.sha256,
                "bytes": entry.bytes,
                "cacheStatus": entry.cacheStatus,
                "relativeImportBase": entry.relativeImportBase,
                "cleanupPolicy": FlowIpcResourceSchemeContract.cleanupPolicy,
                "browserError": error.localizedDescription,
                "executionStatus": "serve-failed"
            ]
            for (key, value) in entry.metadata {
                failureTrace[key] = value
            }
            trace("send", "resourceLoad", compactResourceLoadTrace(failureTrace))
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
    }
}
