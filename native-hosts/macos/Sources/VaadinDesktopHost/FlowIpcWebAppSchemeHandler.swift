import Foundation
import WebKit

final class FlowIpcWebAppSchemeHandler: NSObject, WKURLSchemeHandler {
    static let schemeName = "vatori-app"
    static let virtualRequestBaseURL = "https://app.vatori.invalid/"
    private static let pushBridgePath = "/__vatori/FlowIpcPushBridge.js"

    private let source: FlowWebAppSource?
    private let pushBridgeURL: URL
    private let rootTracePath: String
    private let proxyTaskLock = NSLock()
    private var proxyTasks: [ObjectIdentifier: URLSessionDataTask] = [:]
    private var enabledFeatureFlags: [String] = []

    init(source: FlowWebAppSource?, pushBridgeURL: URL) {
        self.pushBridgeURL = pushBridgeURL
        switch source {
        case .packaged(let rootURL):
            let packagedRoot = rootURL
                .standardizedFileURL
                .resolvingSymlinksInPath()
            self.source = .packaged(packagedRoot)
            self.rootTracePath = "$APP/Contents/Resources/FlowWebApp"
        case .development(let server):
            self.source = .development(server)
            self.rootTracePath = "$LOOPBACK_WEBAPP"
        case nil:
            self.source = nil
            self.rootTracePath = "$MISSING_WEBAPP"
        }
    }

    var isAvailable: Bool {
        switch source {
        case .packaged(let rootURL):
            return FileManager.default.fileExists(
                atPath: rootURL.appendingPathComponent("index.html").path)
        case .development:
            return true
        case nil:
            return false
        }
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

    var sourceMode: String {
        source?.mode ?? "missing"
    }

    func setEnabledFeatureFlags(_ featureFlags: [String]) {
        enabledFeatureFlags = featureFlags
    }

    func webView(_ webView: WKWebView, start urlSchemeTask: WKURLSchemeTask) {
        guard let requestURL = urlSchemeTask.request.url,
              requestURL.host == "app" else {
            fail(urlSchemeTask, code: 404, message: "web app resource not found")
            return
        }

        if requestURL.path == Self.pushBridgePath {
            servePushBridge(urlSchemeTask, requestURL: requestURL)
            return
        }
        switch source {
        case .packaged(let rootURL):
            servePackagedResource(
                urlSchemeTask, requestURL: requestURL, rootURL: rootURL)
        case .development(let server):
            serveDevelopmentResource(
                urlSchemeTask, requestURL: requestURL, server: server)
        case nil:
            fail(urlSchemeTask, code: 404, message: "web app source is missing")
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
        takeProxyTask(for: urlSchemeTask)?.cancel()
    }

    private func servePackagedResource(
        _ urlSchemeTask: WKURLSchemeTask,
        requestURL: URL,
        rootURL: URL
    ) {
        guard let fileURL = fileURL(for: requestURL, rootURL: rootURL) else {
            fail(urlSchemeTask, code: 404, message: "packaged web app resource not found")
            return
        }
        do {
            let data = try injectedResponseData(
                Data(contentsOf: fileURL),
                isHTML: fileURL.lastPathComponent == "index.html")
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

    private func serveDevelopmentResource(
        _ urlSchemeTask: WKURLSchemeTask,
        requestURL: URL,
        server: FlowWebAppDevelopmentServer
    ) {
        let isEntryRequest = isMainDocumentRequest(urlSchemeTask.request)
        guard let targetURL = developmentURL(
            for: requestURL,
            server: server,
            useEntryPath: isEntryRequest) else {
            fail(urlSchemeTask, code: 400, message: "development web app resource URL is invalid")
            return
        }
        var request = URLRequest(url: targetURL)
        request.httpMethod = urlSchemeTask.request.httpMethod
        request.allHTTPHeaderFields = urlSchemeTask.request.allHTTPHeaderFields
        request.httpBody = urlSchemeTask.request.httpBody

        let identifier = ObjectIdentifier(urlSchemeTask as AnyObject)
        let dataTask = URLSession.shared.dataTask(with: request) {
            [weak self] data, response, error in
            DispatchQueue.main.async {
                guard let self,
                      self.takeProxyTask(identifier: identifier) != nil else {
                    return
                }
                if let error {
                    self.fail(
                        urlSchemeTask,
                        code: 502,
                        message: "development web app request failed: \(error.localizedDescription)")
                    return
                }
                guard let httpResponse = response as? HTTPURLResponse,
                      let data else {
                    self.fail(
                        urlSchemeTask,
                        code: 502,
                        message: "development web app returned no HTTP response")
                    return
                }
                let contentType = httpResponse.value(
                    forHTTPHeaderField: "Content-Type") ?? ""
                let responseData = self.injectedResponseData(
                    data,
                    isHTML: isEntryRequest
                        && contentType.lowercased().contains("text/html"))
                var headers = httpResponse.allHeaderFields.reduce(
                    into: [String: String]()) { result, item in
                    result[String(describing: item.key)] =
                        String(describing: item.value)
                }
                headers["Content-Length"] = String(responseData.count)
                headers["Cache-Control"] = "no-store"
                guard let proxiedResponse = HTTPURLResponse(
                    url: requestURL,
                    statusCode: httpResponse.statusCode,
                    httpVersion: "HTTP/1.1",
                    headerFields: headers) else {
                    self.fail(
                        urlSchemeTask,
                        code: 500,
                        message: "unable to create development resource response")
                    return
                }
                urlSchemeTask.didReceive(proxiedResponse)
                urlSchemeTask.didReceive(responseData)
                urlSchemeTask.didFinish()
                trace("recv", "developmentWebAppResource", [
                    "path": requestURL.path,
                    "bytes": responseData.count,
                    "status": httpResponse.statusCode,
                    "transport": "custom-scheme-loopback-proxy",
                    "root": self.redactedRoot
                ])
            }
        }
        storeProxyTask(dataTask, identifier: identifier)
        dataTask.resume()
    }

    private func servePushBridge(
        _ urlSchemeTask: WKURLSchemeTask,
        requestURL: URL
    ) {
        do {
            let data = try Data(contentsOf: pushBridgeURL)
            let headers = [
                "Content-Type": "text/javascript; charset=utf-8",
                "Content-Length": String(data.count),
                "Cache-Control": "no-store"
            ]
            guard let response = HTTPURLResponse(
                url: requestURL,
                statusCode: 200,
                httpVersion: "HTTP/1.1",
                headerFields: headers) else {
                fail(urlSchemeTask, code: 500, message: "unable to create push bridge response")
                return
            }
            urlSchemeTask.didReceive(response)
            urlSchemeTask.didReceive(data)
            urlSchemeTask.didFinish()
            trace("recv", "flowIpcPushBridgeResource", [
                "path": requestURL.path,
                "bytes": data.count,
                "transport": "custom-scheme-host-resource"
            ])
        } catch {
            fail(urlSchemeTask, code: 500, message: error.localizedDescription)
        }
    }

    private func fileURL(for requestURL: URL, rootURL: URL) -> URL? {
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

    private func developmentURL(
        for requestURL: URL,
        server: FlowWebAppDevelopmentServer,
        useEntryPath: Bool
    ) -> URL? {
        var components = URLComponents(
            url: server.originURL, resolvingAgainstBaseURL: false)
        components?.path = useEntryPath ? server.entryPath : requestURL.path
        components?.percentEncodedQuery = requestURL.query
        return components?.url
    }

    private func isMainDocumentRequest(_ request: URLRequest) -> Bool {
        guard let requestURL = request.url else {
            return false
        }
        return request.mainDocumentURL == nil
            || request.mainDocumentURL == requestURL
    }

    private func storeProxyTask(
        _ task: URLSessionDataTask,
        identifier: ObjectIdentifier
    ) {
        proxyTaskLock.lock()
        proxyTasks[identifier] = task
        proxyTaskLock.unlock()
    }

    private func takeProxyTask(
        for schemeTask: WKURLSchemeTask
    ) -> URLSessionDataTask? {
        takeProxyTask(identifier: ObjectIdentifier(schemeTask as AnyObject))
    }

    private func takeProxyTask(
        identifier: ObjectIdentifier
    ) -> URLSessionDataTask? {
        proxyTaskLock.lock()
        defer { proxyTaskLock.unlock() }
        return proxyTasks.removeValue(forKey: identifier)
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

    private func injectedResponseData(_ data: Data, isHTML: Bool) -> Data {
        guard isHTML,
              var html = String(data: data, encoding: .utf8) else {
            return data
        }
        let base = "  <base href=\"\(Self.virtualRequestBaseURL)\" data-vatori-ipc-request-base>\n"
        let featureFlags = featureFlagsInitializer()
        guard let headEnd = html.range(of: "</head>", options: .caseInsensitive) else {
            return data
        }
        html.insert(contentsOf: base + featureFlags, at: headEnd.lowerBound)
        return Data(html.utf8)
    }

    private func featureFlagsInitializer() -> String {
        guard !enabledFeatureFlags.isEmpty,
              JSONSerialization.isValidJSONObject(enabledFeatureFlags),
              let data = try? JSONSerialization.data(withJSONObject: enabledFeatureFlags),
              let json = String(data: data, encoding: .utf8) else {
            return ""
        }
        return """
          <script data-vatori-ipc-feature-flags>
          window.Vaadin = window.Vaadin || {};
          window.Vaadin.featureFlagsUpdaters = window.Vaadin.featureFlagsUpdaters || [];
          window.Vaadin.featureFlagsUpdaters.push((activator) => {
            \(json).forEach((id) => activator(id));
          });
          </script>

        """
    }

    private func fail(_ task: WKURLSchemeTask, code: Int, message: String) {
        trace("send", "error", [
            "code": "flowWebAppResourceFailed",
            "status": code,
            "message": message,
            "path": task.request.url?.path ?? ""
        ])
        task.didFailWithError(NSError(
            domain: "dev.vatori.desktop.web-app",
            code: code,
            userInfo: [NSLocalizedDescriptionKey: message]))
    }
}
