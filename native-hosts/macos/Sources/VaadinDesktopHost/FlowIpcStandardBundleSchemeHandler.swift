import Foundation
import WebKit

final class FlowIpcStandardBundleSchemeHandler: NSObject, WKURLSchemeHandler {
    static let schemeName = "vatori-app"
    static let virtualRequestBaseURL = "https://app.vatori.invalid/"

    private let rootURL: URL
    private let rootTracePath: String

    init(rootURL: URL?) {
        let packagedRoot = rootURL?
            .standardizedFileURL
            .resolvingSymlinksInPath()
        if let packagedRoot,
           FileManager.default.fileExists(
               atPath: packagedRoot.appendingPathComponent("index.html").path) {
            self.rootURL = packagedRoot
            self.rootTracePath = "$APP/Contents/Resources/FlowWebApp"
        } else {
            self.rootURL = URL(fileURLWithPath: "/__vatori_missing_flow_webapp__")
            self.rootTracePath = "$APP/Contents/Resources/FlowWebApp"
        }
    }

    var isAvailable: Bool {
        FileManager.default.fileExists(atPath: rootURL.appendingPathComponent("index.html").path)
    }

    var entryURL: URL {
        URL(string: "\(Self.schemeName)://app/")!
    }

    func entryURL(for route: String) -> URL {
        if route.isEmpty {
            return entryURL
        }
        return URL(string: "\(Self.schemeName)://app/\(route)") ?? entryURL
    }

    var redactedRoot: String {
        rootTracePath
    }

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let requestURL = urlSchemeTask.request.url,
              requestURL.host == "app",
              let fileURL = fileURL(for: requestURL) else {
            fail(urlSchemeTask, code: 404, message: "standard bundle resource not found")
            return
        }

        do {
            let data = try responseData(for: fileURL)
            let headers = [
                "Content-Type": mimeType(for: fileURL),
                "Content-Length": String(data.count),
                "Cache-Control": "no-store",
                "Access-Control-Allow-Origin": "*",
                "Cross-Origin-Resource-Policy": "cross-origin"
            ]
            guard let response = HTTPURLResponse(
                url: requestURL,
                statusCode: 200,
                httpVersion: "HTTP/1.1",
                headerFields: headers) else {
                fail(urlSchemeTask, code: 500, message: "unable to create resource response")
                return
            }
            urlSchemeTask.didReceive(response)
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
            trace("recv", "standardBundleResource", [
                "path": requestURL.path,
                "bytes": data.count,
                "transport": "custom-scheme-static-file",
                "root": redactedRoot
            ])
        } catch {
            fail(urlSchemeTask, code: 500, message: error.localizedDescription)
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
    }

    private func fileURL(for requestURL: URL) -> URL? {
        let decodedPath = requestURL.path.removingPercentEncoding ?? requestURL.path
        let relativePath = decodedPath == "/" || decodedPath.isEmpty
            ? "index.html"
            : String(decodedPath.drop(while: { $0 == "/" }))
        let components = relativePath.split(separator: "/", omittingEmptySubsequences: true)
        guard !components.isEmpty,
              components.allSatisfy({ $0 != "." && $0 != ".." }) else {
            return nil
        }
        let candidate = components.reduce(rootURL) { partial, component in
            partial.appendingPathComponent(String(component))
        }.standardizedFileURL.resolvingSymlinksInPath()
        let rootPath = rootURL.path.hasSuffix("/") ? rootURL.path : rootURL.path + "/"
        guard candidate.path.hasPrefix(rootPath) else {
            return nil
        }
        var isDirectory: ObjCBool = false
        if !FileManager.default.fileExists(atPath: candidate.path, isDirectory: &isDirectory)
                || isDirectory.boolValue {
            guard candidate.pathExtension.isEmpty else {
                return nil
            }
            return rootURL.appendingPathComponent("index.html")
        }
        return candidate
    }

    private func mimeType(for url: URL) -> String {
        switch url.pathExtension.lowercased() {
        case "html": return "text/html; charset=utf-8"
        case "js", "mjs": return "text/javascript; charset=utf-8"
        case "css": return "text/css; charset=utf-8"
        case "json", "map": return "application/json; charset=utf-8"
        case "svg": return "image/svg+xml"
        case "png": return "image/png"
        case "jpg", "jpeg": return "image/jpeg"
        case "gif": return "image/gif"
        case "webp": return "image/webp"
        case "woff": return "font/woff"
        case "woff2": return "font/woff2"
        default: return "application/octet-stream"
        }
    }

    private func responseData(for fileURL: URL) throws -> Data {
        let data = try Data(contentsOf: fileURL)
        guard fileURL.lastPathComponent == "index.html",
              var html = String(data: data, encoding: .utf8) else {
            return data
        }
        let base = "  <base href=\"\(Self.virtualRequestBaseURL)\" data-vatori-ipc-request-base>\n"
        guard let headEnd = html.range(of: "</head>", options: .caseInsensitive) else {
            return data
        }
        html.insert(contentsOf: base, at: headEnd.lowerBound)
        return Data(html.utf8)
    }

    private func fail(_ task: WKURLSchemeTask, code: Int, message: String) {
        trace("send", "error", [
            "code": "standardBundleResourceFailed",
            "status": code,
            "message": message
        ])
        task.didFailWithError(NSError(
            domain: "dev.vatori.desktop.standard-bundle",
            code: code,
            userInfo: [NSLocalizedDescriptionKey: message]))
    }
}
