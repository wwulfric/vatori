import Cocoa
import WebKit

struct HostConfig {
    var module: String = "examples/counter-app"
    var repoRoot: String = FileManager.default.currentDirectoryPath
    var mavenRepo: String = ""
    var mavenExecutable: String = "mvn"
    var transport: String = "compatibility-http"
    var flowIpcAutoClick: Bool = false
    var flowIpcAutoResync: Bool = false
    var flowIpcAutoError: Bool = false
    var flowIpcAutoFileDialogCancel: Bool = false
    var flowIpcAutoVaadinTextField: Bool = false
    var flowIpcAutoFrameworkUpdate: Bool = false
    var flowIpcAutoShutdown: Bool = false
    var hostNoActivate: Bool = false
    var title: String = "Vaadin Desktop"
    var width: Double = 1100
    var height: Double = 780

    static func parse() -> HostConfig {
        var config = HostConfig()
        var arguments = CommandLine.arguments.dropFirst().makeIterator()
        while let argument = arguments.next() {
            switch argument {
            case "--module":
                config.module = arguments.next() ?? config.module
            case "--repo-root":
                config.repoRoot = arguments.next() ?? config.repoRoot
            case "--maven-repo":
                config.mavenRepo = arguments.next() ?? config.mavenRepo
            case "--maven-executable":
                config.mavenExecutable = arguments.next() ?? config.mavenExecutable
            case "--transport":
                config.transport = arguments.next() ?? config.transport
            case "--title":
                config.title = arguments.next() ?? config.title
            case "--width":
                config.width = Double(arguments.next() ?? "") ?? config.width
            case "--height":
                config.height = Double(arguments.next() ?? "") ?? config.height
            default:
                continue
            }
        }

        if config.mavenRepo.isEmpty {
            config.mavenRepo = "\(config.repoRoot)/.m2/repository"
        }
        if !config.module.hasPrefix("/") {
            config.module = "\(config.repoRoot)/\(config.module)"
        }
        // Real-window acceptance hooks; keep them opt-in and documented in docs/acceptance-tools.md.
        config.flowIpcAutoClick = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK"] == "1"
        config.flowIpcAutoResync = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC"] == "1"
        config.flowIpcAutoError = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR"] == "1"
        config.flowIpcAutoFileDialogCancel = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL"] == "1"
            || config.flowIpcAutoClick
        config.flowIpcAutoVaadinTextField = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD"] == "1"
            || ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL"] == "1"
            || config.flowIpcAutoClick
        config.flowIpcAutoFrameworkUpdate = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE"] == "1"
        config.flowIpcAutoShutdown = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN"] == "1"
        config.hostNoActivate = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_HOST_NO_ACTIVATE"] == "1"
        return config
    }
}

struct CapabilityManifest {
    let path: String
    let loaded: Bool
    let permissionMode: String
    let allowedCommands: Set<String>
    let parseError: String

    static func load(module: String) -> CapabilityManifest {
        let path = "\(module)/desktop-capabilities.json"
        guard FileManager.default.fileExists(atPath: path) else {
            return CapabilityManifest(path: path,
                                      loaded: false,
                                      permissionMode: "allow-all-dev",
                                      allowedCommands: [],
                                      parseError: "")
        }
        guard let data = FileManager.default.contents(atPath: path) else {
            return CapabilityManifest(path: path,
                                      loaded: true,
                                      permissionMode: "allow-all-dev",
                                      allowedCommands: [],
                                      parseError: "manifest-unreadable")
        }
        do {
            let json = try JSONSerialization.jsonObject(with: data)
            guard let object = json as? [String: Any] else {
                return CapabilityManifest(path: path,
                                          loaded: true,
                                          permissionMode: "allow-all-dev",
                                          allowedCommands: [],
                                          parseError: "manifest-root-not-object")
            }
            let permissionMode = object["permissionMode"] as? String ?? "allow-all-dev"
            let windows = object["windows"] as? [[String: Any]] ?? []
            var commands = Set<String>()
            for window in windows where (window["label"] as? String ?? "main") == "main" {
                if let values = window["commands"] as? [String] {
                    commands.formUnion(values)
                }
                if let values = window["capabilities"] as? [String] {
                    commands.formUnion(values)
                }
            }
            return CapabilityManifest(path: path,
                                      loaded: true,
                                      permissionMode: permissionMode,
                                      allowedCommands: commands,
                                      parseError: "")
        } catch {
            return CapabilityManifest(path: path,
                                      loaded: true,
                                      permissionMode: "allow-all-dev",
                                      allowedCommands: [],
                                      parseError: error.localizedDescription)
        }
    }

    func allows(_ command: String) -> Bool {
        if permissionMode == "trace-only-dry-run" {
            return true
        }
        if !parseError.isEmpty {
            return false
        }
        if permissionMode == "manifest-enforced" {
            return loaded && allowedCommands.contains(command)
        }
        if !loaded {
            return true
        }
        return allowedCommands.contains(command)
    }

    func denialReason(_ command: String) -> String {
        if !parseError.isEmpty {
            return "manifest-invalid"
        }
        if permissionMode == "manifest-enforced" && !loaded {
            return "manifest-missing"
        }
        if loaded && !allowedCommands.contains(command) {
            return "manifest-denied"
        }
        return "unsupported-command"
    }

    func allowedReason(_ command: String) -> String {
        if permissionMode == "trace-only-dry-run" {
            if loaded && !allowedCommands.contains(command) {
                return "dry-run-would-deny"
            }
            return loaded ? "dry-run-manifest-allow" : "dry-run-development-default"
        }
        return loaded ? "manifest-allow" : "development-default"
    }

    func tracePayload() -> [String: Any] {
        [
            "path": path,
            "loaded": loaded,
            "permissionMode": permissionMode,
            "allowedCommands": allowedCommands.sorted(),
            "parseError": parseError
        ]
    }
}

func nowMillis() -> Int64 {
    Int64(Date().timeIntervalSince1970 * 1000)
}

extension String {
    var isBlank: Bool {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

func trace(_ direction: String, _ type: String, _ payload: [String: Any]) {
    let object: [String: Any] = [
        "direction": direction,
        "type": type,
        "at": nowMillis(),
        "payload": payload
    ]
    if let data = try? JSONSerialization.data(withJSONObject: object, options: []),
       let text = String(data: data, encoding: .utf8) {
        print("HOST_PROTOCOL \(text)")
    } else {
        print("HOST_PROTOCOL {\"direction\":\"\(direction)\",\"type\":\"\(type)\",\"at\":\(nowMillis()),\"payload\":{}}")
    }
    fflush(stdout)
}

func redactedFlowIpcEnvelope(_ object: [String: Any]) -> [String: Any] {
    if object["type"] as? String == "resourceResponse",
       var payload = object["payload"] as? [String: Any],
       let body = payload["body"] as? String {
        var copy = object
        payload["bodyPreview"] = String(body.prefix(96))
        payload["bodyBytes"] = body.utf8.count
        payload["bodyRedacted"] = true
        payload.removeValue(forKey: "body")
        copy["payload"] = payload
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

struct FlowIpcHostResourceEntry {
    let sourcePath: String
    let fileURL: URL
    let mimeType: String
    let sha256: String
    let bytes: Int
    let cacheStatus: String
    let relativeImportBase: String
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
        trace("recv", "resourceLoad", [
            "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
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
            "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17"
        ])

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
            trace("send", "resourceLoad", [
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
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
                "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17",
                "executionStatus": "served"
            ])
        } catch {
            urlSchemeTask.didFailWithError(error)
            trace("send", "resourceLoad", [
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
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
                "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17",
                "browserError": error.localizedDescription,
                "executionStatus": "serve-failed"
            ])
        }
    }

    func webView(_ webView: WKWebView, stop urlSchemeTask: WKURLSchemeTask) {
    }
}

final class LineReader {
    private var buffer = Data()
    private let onLine: (String) -> Void

    init(onLine: @escaping (String) -> Void) {
        self.onLine = onLine
    }

    func append(_ data: Data) {
        buffer.append(data)
        while let range = buffer.firstRange(of: Data([0x0A])) {
            let lineData = buffer.subdata(in: buffer.startIndex..<range.lowerBound)
            buffer.removeSubrange(buffer.startIndex..<range.upperBound)
            if let line = String(data: lineData, encoding: .utf8) {
                onLine(line.trimmingCharacters(in: .newlines))
            }
        }
    }
}

final class JavaSidecar {
    private let config: HostConfig
    private let onApplicationURL: (URL) -> Void
    private let onFlowIpcEnvelope: (String, [String: Any]) -> Void
    private var process: Process?
    private var stdinPipe: Pipe?
    private var stdoutPipe: Pipe?
    private var stderrPipe: Pipe?

    init(config: HostConfig,
         onApplicationURL: @escaping (URL) -> Void,
         onFlowIpcEnvelope: @escaping (String, [String: Any]) -> Void) {
        self.config = config
        self.onApplicationURL = onApplicationURL
        self.onFlowIpcEnvelope = onFlowIpcEnvelope
    }

    func start() {
        let modulePom = "\(config.module)/pom.xml"
        guard FileManager.default.fileExists(atPath: modulePom) else {
            trace("send", "error", [
                "code": "moduleNotFound",
                "message": "Module pom not found: \(modulePom)"
            ])
            NSApp.terminate(nil)
            return
        }

        let process = Process()
        process.currentDirectoryURL = URL(fileURLWithPath: config.repoRoot)
        process.executableURL = URL(fileURLWithPath: config.mavenExecutable)
        let mavenMode = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_MAVEN_OFFLINE"] == "1" ? "-o" : "-U"
        process.arguments = [
            mavenMode,
            "-Dmaven.repo.local=\(config.mavenRepo)",
            "-f",
            modulePom,
            "spring-boot:run"
        ]

        var environment = ProcessInfo.processInfo.environment
        environment["VAADIN_DESKTOP_HOST"] = config.transport == "flow-ipc" ? "flow-ipc" : "headless"
        environment["VAADIN_DESKTOP_TRANSPORT"] = config.transport
        environment["VAADIN_DESKTOP_NATIVE_BRIDGE"] = "host-protocol"
        environment["VAADIN_DESKTOP_HOST_PROTOCOL"] = "stdout-v0"
        process.environment = environment

        let stdinPipe = Pipe()
        let stdoutPipe = Pipe()
        let stderrPipe = Pipe()
        process.standardInput = stdinPipe
        process.standardOutput = stdoutPipe
        process.standardError = stderrPipe
        self.stdinPipe = stdinPipe
        self.stdoutPipe = stdoutPipe
        self.stderrPipe = stderrPipe
        self.process = process

        let stdoutReader = LineReader { [weak self] line in
            self?.handleStdout(line)
        }
        stdoutPipe.fileHandleForReading.readabilityHandler = { handle in
            stdoutReader.append(handle.availableData)
        }

        stderrPipe.fileHandleForReading.readabilityHandler = { handle in
            let data = handle.availableData
            if !data.isEmpty, let text = String(data: data, encoding: .utf8) {
                FileHandle.standardError.write(("[java:err] \(text)").data(using: .utf8)!)
            }
        }

        process.terminationHandler = { process in
            trace("recv", "sidecar.exit", [
                "status": process.terminationStatus,
                "reason": process.terminationReason.rawValue
            ])
            if process.terminationStatus != 0 {
                DispatchQueue.main.async {
                    NSApp.terminate(nil)
                }
            }
        }

        trace("send", "launch.sidecar", [
            "command": "\(config.mavenExecutable) spring-boot:run",
            "modulePom": modulePom,
            "nativeBridge": "host-protocol",
            "transport": config.transport
        ])

        do {
            try process.run()
        } catch {
            trace("send", "error", [
                "code": "sidecarStartFailed",
                "message": error.localizedDescription
            ])
            NSApp.terminate(nil)
        }
    }

    func terminate() {
        if config.transport == "flow-ipc" {
            trace("send", "shutdown", [
                "target": "java-sidecar",
                "transport": "flow-ipc",
                "reason": "host-terminate"
            ])
            sendFlowIpc("{\"version\":\"flow-ipc-v1\",\"type\":\"shutdown\",\"id\":\"shutdown\",\"correlationId\":\"shutdown\",\"sessionId\":\"flow-ipc-session\",\"uiId\":\"main\",\"route\":\"\",\"payload\":{},\"error\":null,\"timestamp\":0}")
        }
        stdoutPipe?.fileHandleForReading.readabilityHandler = nil
        stderrPipe?.fileHandleForReading.readabilityHandler = nil
        if let process, process.isRunning {
            trace("send", "shutdown.sidecar", [:])
            process.terminate()
        }
    }

    func sendFlowIpc(_ envelope: String) {
        guard let stdinPipe else {
            return
        }
        let line = "VFIPC1 \(envelope)\n"
        if let data = line.data(using: .utf8) {
            stdinPipe.fileHandleForWriting.write(data)
        }
    }

    private func handleStdout(_ line: String) {
        print("[java] \(redactedJavaStdoutLine(line))")
        fflush(stdout)

        if line.hasPrefix("VDP1 ") {
            handleHostProtocolEnvelope(String(line.dropFirst(5)))
        }

        if line.hasPrefix("VFIPC1 ") {
            handleFlowIpcEnvelope(String(line.dropFirst(7)))
        }

        if line.hasPrefix("Application URL: ") {
            let text = String(line.dropFirst("Application URL: ".count)).trimmingCharacters(in: .whitespacesAndNewlines)
            if let url = URL(string: text) {
                DispatchQueue.main.async {
                    self.onApplicationURL(url)
                }
            }
        }
    }

    private func handleFlowIpcEnvelope(_ envelope: String) {
        guard let data = envelope.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            trace("send", "error", [
                "code": "invalidFlowIpcEnvelope",
                "raw": envelope
            ])
            return
        }
        DispatchQueue.main.async {
            self.onFlowIpcEnvelope(envelope, object)
        }
    }

    private func handleHostProtocolEnvelope(_ envelope: String) {
        guard let data = envelope.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            trace("send", "error", [
                "code": "invalidJavaEnvelope",
                "raw": envelope
            ])
            return
        }

        let type = object["type"] as? String ?? "unknown"
        trace("recv", type, [
            "source": "java-sidecar",
            "envelope": object
        ])

        if type == "command",
           let payload = object["payload"] as? [String: Any],
           let command = payload["command"] as? String {
            trace("send", "capabilityResult", [
                "command": command,
                "mode": "swift-host-scaffold",
                "result": "traced"
            ])
        }
    }
}

final class WebViewHost: NSObject, NSApplicationDelegate, NSWindowDelegate, WKNavigationDelegate, WKScriptMessageHandler {
    private let config: HostConfig
    private let capabilityManifest: CapabilityManifest
    private let resourceSchemeHandler = FlowIpcResourceSchemeHandler()
    private var sidecar: JavaSidecar?
    private var window: NSWindow?
    private var webView: WKWebView?
    private var didOpenWindow = false
    private var fileDialogInFlight = false
    private var fileTransferPanelInFlight = false
    private var hostResourceStageCache: Set<String> = []

    init(config: HostConfig) {
        self.config = config
        self.capabilityManifest = CapabilityManifest.load(module: config.module)
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        trace("recv", "launch", [
            "host": "swift-appkit-wkwebview",
            "module": config.module,
            "compatibilityMode": config.transport != "flow-ipc",
            "transport": config.transport
        ])
        trace("send", "ready", [
            "host": "swift-appkit-wkwebview",
            "protocol": config.transport == "flow-ipc" ? "flow-ipc-v1" : "host-protocol-v0",
            "compatibilityMode": config.transport != "flow-ipc"
        ])
        if config.transport == "flow-ipc" {
            trace("recv", "capabilityManifest", capabilityManifest.tracePayload())
        }

        let sidecar = JavaSidecar(config: config) { [weak self] url in
            self?.openWindow(url: url)
        } onFlowIpcEnvelope: { [weak self] raw, object in
            self?.handleFlowIpcEnvelope(raw, object)
        }
        self.sidecar = sidecar
        sidecar.start()
    }

    func applicationWillTerminate(_ notification: Notification) {
        sidecar?.terminate()
    }

    private func openWindow(url: URL) {
        guard !didOpenWindow else {
            return
        }
        didOpenWindow = true

        trace("send", "window.create", [
            "label": "main",
            "url": url.absoluteString,
            "mode": "compatibility-http"
        ])

        let contentRect = NSRect(x: 0, y: 0, width: config.width, height: config.height)
        let window = NSWindow(
            contentRect: contentRect,
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false)

        let configuration = WKWebViewConfiguration()
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        let webView = WKWebView(frame: contentRect, configuration: configuration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.width, .height]

        window.title = config.title
        window.contentView = webView
        window.delegate = self
        window.isReleasedWhenClosed = false
        window.sharingType = .readOnly
        window.level = .normal
        window.collectionBehavior = [.fullScreenPrimary]
        window.center()
        showWindow(window)

        self.window = window
        self.webView = webView

        webView.load(URLRequest(url: url))
        trace("recv", "window.ready", ["label": "main"])
    }

    private func openFlowIpcWindow() {
        guard !didOpenWindow else {
            return
        }
        didOpenWindow = true

        trace("send", "window.create", [
            "label": "main",
            "mode": "flow-ipc",
            "compatibilityMode": false
        ])

        let contentRect = NSRect(x: 0, y: 0, width: config.width, height: config.height)
        let window = NSWindow(
            contentRect: contentRect,
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false)

        let userContentController = WKUserContentController()
        if config.flowIpcAutoClick {
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        if config.flowIpcAutoResync {
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        if config.flowIpcAutoError {
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        if config.flowIpcAutoVaadinTextField {
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__ = true; window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        if config.flowIpcAutoFrameworkUpdate {
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        userContentController.add(self, name: "flowIpc")

        prepareFlowIpcResourceStagingDirectory()
        let configuration = WKWebViewConfiguration()
        configuration.userContentController = userContentController
        configuration.setURLSchemeHandler(resourceSchemeHandler,
                                          forURLScheme: FlowIpcResourceSchemeHandler.schemeName)
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        trace("recv", "resourceLoad", [
            "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
            "frontendResourceSchemeName": FlowIpcResourceSchemeHandler.schemeName,
            "frontendResourceLoaderMode": "custom-scheme-file-backed",
            "stagingMode": "custom-scheme",
            "frontendResourceSchemeHandlerInstalled": true,
            "frontendResourceLoaderNoLocalhost": true,
            "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17",
            "fileDirectoryRedacted": "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources"
        ])
        let webView = WKWebView(frame: contentRect, configuration: configuration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.width, .height]

        window.title = config.title
        window.contentView = webView
        window.delegate = self
        window.isReleasedWhenClosed = false
        window.sharingType = .readOnly
        window.level = .normal
        window.collectionBehavior = [.fullScreenPrimary]
        window.center()
        showWindow(window)

        self.window = window
        self.webView = webView

        webView.loadHTMLString(flowIpcShell, baseURL: nil)
        trace("recv", "window.ready", ["label": "main", "transport": "flow-ipc"])
    }

    private func showWindow(_ window: NSWindow) {
        if config.hostNoActivate {
            window.orderFront(nil)
            trace("recv", "window.activation", [
                "label": "main",
                "mode": "no-activate",
                "keyWindow": window.isKeyWindow,
                "activeApplication": NSApplication.shared.isActive
            ])
            return
        }

        window.makeKeyAndOrderFront(nil)
        NSRunningApplication.current.activate(options: [.activateAllWindows])
        trace("recv", "window.activation", [
            "label": "main",
            "mode": "activate",
            "keyWindow": window.isKeyWindow,
            "activeApplication": NSApplication.shared.isActive
        ])
    }

    private func handleFlowIpcEnvelope(_ raw: String, _ object: [String: Any]) {
        let type = object["type"] as? String ?? "unknown"
        trace("recv", type, [
            "source": "java-sidecar",
            "transport": "flow-ipc",
            "envelope": redactedFlowIpcEnvelope(object)
        ])
        if type == "ready" {
            openFlowIpcWindow()
            return
        }
        if type == "command" {
            handleFlowIpcCommand(object)
            return
        }
        deliverFlowIpcEnvelope(raw, type: type)
    }

    private func handleFlowIpcCommand(_ object: [String: Any]) {
        let payload = object["payload"] as? [String: Any] ?? [:]
        let command = payload["command"] as? String ?? "unknown"
        let args = payload["args"] as? [String: Any] ?? [:]
        let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
        executeDesktopCommand(command, args: args) { result in
            let resultEnvelope: [String: Any] = [
                "version": "flow-ipc-v1",
                "type": "capabilityResult",
                "id": "capability-\(correlationId)",
                "correlationId": correlationId,
                "sessionId": object["sessionId"] as? String ?? "flow-ipc-session",
                "uiId": object["uiId"] as? String ?? "main",
                "route": object["route"] as? String ?? "",
                "payload": result,
                "error": NSNull(),
                "timestamp": nowMillis()
            ]
            trace("send", "capabilityResult", [
                "target": "webview",
                "transport": "flow-ipc",
                "command": command,
                "envelope": resultEnvelope
            ])
            self.deliverFlowIpcEnvelope(resultEnvelope, type: "capabilityResult")
        }
    }

    private func executeDesktopCommand(_ command: String,
                                       args: [String: Any],
                                       completion: @escaping ([String: Any]) -> Void) {
        if !capabilityManifest.allows(command) {
            completion(capabilityPermission([
                "success": false,
                "command": command,
                "error": capabilityErrorMessage(reason: capabilityManifest.denialReason(command))
            ], decision: "denied", reason: capabilityManifest.denialReason(command)))
            return
        }
        switch command {
        case "window.setTitle":
            guard let title = args["title"] as? String else {
                completion(capabilityPermission([
                    "success": false,
                    "command": command,
                    "error": "Missing title"
                ]))
                return
            }
            window?.title = title
            completion(capabilityPermission([
                "success": true,
                "command": command,
                "title": title,
                "mode": "native-appkit"
            ]))
        case "clipboard.writeText":
            guard let text = args["text"] as? String else {
                completion(capabilityPermission([
                    "success": false,
                    "command": command,
                    "error": "Missing text"
                ]))
                return
            }
            let pasteboard = NSPasteboard.general
            pasteboard.clearContents()
            let written = pasteboard.setString(text, forType: .string)
            completion(capabilityPermission([
                "success": written,
                "command": command,
                "mode": "native-nspasteboard"
            ]))
        case "notification.show":
            completion(capabilityPermission([
                "success": true,
                "command": command,
                "mode": "trace-only",
                "title": args["title"] as? String ?? "",
                "body": args["body"] as? String ?? ""
            ]))
        case "fileDialog.openFile":
            openFileDialog(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        case "fileTransfer.download":
            saveDownloadedFile(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        case "fileTransfer.selectUploadFile":
            selectUploadFile(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        default:
            completion(capabilityPermission([
                "success": false,
                "command": command,
                "error": "Unsupported desktop command"
            ], decision: "denied", reason: "unsupported-command"))
        }
    }

    private func capabilityErrorMessage(reason: String) -> String {
        switch reason {
        case "manifest-invalid":
            return "Invalid desktop-capabilities.json: \(capabilityManifest.parseError)"
        case "manifest-missing":
            return "desktop-capabilities.json is required by manifest-enforced mode"
        case "manifest-denied":
            return "Command not allowed by desktop-capabilities.json"
        default:
            return "Unsupported desktop command"
        }
    }

    private func openFileDialog(args: [String: Any],
                                command: String,
                                completion: @escaping ([String: Any]) -> Void) {
        let title = args["title"] as? String ?? "Open File"
        let allowsMultipleSelection = args["allowsMultipleSelection"] as? Bool ?? false
        if fileDialogInFlight {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "title": title,
                "canceled": true,
                "paths": [],
                "allowsMultipleSelection": allowsMultipleSelection,
                "securityScoped": false,
                "autoCanceled": false,
                "error": "File dialog already in progress"
            ])
            return
        }

        fileDialogInFlight = true
        let panel = NSOpenPanel()
        panel.title = title
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = allowsMultipleSelection
        panel.canCreateDirectories = false

        if config.flowIpcAutoFileDialogCancel {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                panel.cancel(nil)
            }
        }

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            self.fileDialogInFlight = false
            let paths = response == .OK ? panel.urls.map(\.path) : []
            var result: [String: Any] = [
                "success": true,
                "command": command,
                "mode": "native-appkit-open-panel",
                "title": title,
                "canceled": response != .OK,
                "paths": paths,
                "allowsMultipleSelection": allowsMultipleSelection,
                "securityScoped": false,
                "autoCanceled": response != .OK && self.config.flowIpcAutoFileDialogCancel
            ]
            if paths.count == 1 {
                result["path"] = paths[0]
            }
            completion(result)
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func saveDownloadedFile(args: [String: Any],
                                    command: String,
                                    completion: @escaping ([String: Any]) -> Void) {
        let suggestedName = args["suggestedName"] as? String ?? "download.txt"
        let mimeType = args["mimeType"] as? String ?? "application/octet-stream"
        let bodyEncoding = args["bodyEncoding"] as? String ?? "utf-8"
        let body = args["body"] as? String ?? ""
        let bodyData = body.data(using: .utf8) ?? Data()
        let declaredByteCount = integerArg(args, "byteCount", fallback: bodyData.count)

        if let targetPath = args["targetPath"] as? String, !targetPath.isBlank {
            let targetURL = resolvedTransferURL(targetPath)
            completion(downloadWriteResult(command: command,
                                           mode: "controlled-test-path",
                                           targetURL: targetURL,
                                           suggestedName: suggestedName,
                                           mimeType: mimeType,
                                           bodyEncoding: bodyEncoding,
                                           bodyData: bodyData,
                                           declaredByteCount: declaredByteCount,
                                           usedSavePanel: false))
            return
        }

        if fileTransferPanelInFlight {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-save-panel",
                "operation": "download",
                "suggestedName": suggestedName,
                "canceled": true,
                "bodyRedacted": true,
                "byteCount": bodyData.count,
                "declaredByteCount": declaredByteCount,
                "error": "File transfer panel already in progress"
            ])
            return
        }

        fileTransferPanelInFlight = true
        let panel = NSSavePanel()
        panel.title = args["title"] as? String ?? "Save File"
        panel.nameFieldStringValue = suggestedName
        panel.canCreateDirectories = true

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            self.fileTransferPanelInFlight = false
            guard response == .OK, let targetURL = panel.url else {
                completion([
                    "success": true,
                    "command": command,
                    "mode": "native-appkit-save-panel",
                    "operation": "download",
                    "suggestedName": suggestedName,
                    "canceled": true,
                    "bodyRedacted": true,
                    "byteCount": bodyData.count,
                    "declaredByteCount": declaredByteCount,
                    "usedSavePanel": true
                ])
                return
            }
            completion(self.downloadWriteResult(command: command,
                                                mode: "native-appkit-save-panel",
                                                targetURL: targetURL,
                                                suggestedName: suggestedName,
                                                mimeType: mimeType,
                                                bodyEncoding: bodyEncoding,
                                                bodyData: bodyData,
                                                declaredByteCount: declaredByteCount,
                                                usedSavePanel: true))
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func downloadWriteResult(command: String,
                                     mode: String,
                                     targetURL: URL,
                                     suggestedName: String,
                                     mimeType: String,
                                     bodyEncoding: String,
                                     bodyData: Data,
                                     declaredByteCount: Int,
                                     usedSavePanel: Bool) -> [String: Any] {
        do {
            try FileManager.default.createDirectory(at: targetURL.deletingLastPathComponent(),
                                                    withIntermediateDirectories: true)
            try bodyData.write(to: targetURL)
            return [
                "success": true,
                "command": command,
                "mode": mode,
                "operation": "download",
                "suggestedName": suggestedName,
                "mimeType": mimeType,
                "bodyEncoding": bodyEncoding,
                "bodyRedacted": true,
                "byteCount": bodyData.count,
                "declaredByteCount": declaredByteCount,
                "path": targetURL.path,
                "filename": targetURL.lastPathComponent,
                "usedSavePanel": usedSavePanel,
                "canceled": false
            ]
        } catch {
            return [
                "success": false,
                "command": command,
                "mode": mode,
                "operation": "download",
                "suggestedName": suggestedName,
                "bodyRedacted": true,
                "byteCount": bodyData.count,
                "declaredByteCount": declaredByteCount,
                "path": targetURL.path,
                "filename": targetURL.lastPathComponent,
                "usedSavePanel": usedSavePanel,
                "canceled": false,
                "error": error.localizedDescription
            ]
        }
    }

    private func selectUploadFile(args: [String: Any],
                                  command: String,
                                  completion: @escaping ([String: Any]) -> Void) {
        let title = args["title"] as? String ?? "Select File"
        let readBytesLimit = integerArg(args, "readBytesLimit", fallback: 64 * 1024)

        if let testPath = args["testPath"] as? String, !testPath.isBlank {
            completion(uploadSelectionResult(command: command,
                                             mode: "controlled-test-path",
                                             title: title,
                                             selectedURL: resolvedTransferURL(testPath),
                                             readBytesLimit: readBytesLimit,
                                             usedOpenPanel: false))
            return
        }

        if fileTransferPanelInFlight {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "upload",
                "title": title,
                "canceled": true,
                "paths": [],
                "securityScoped": false,
                "bodyRedacted": true,
                "readBytesLimit": readBytesLimit,
                "error": "File transfer panel already in progress"
            ])
            return
        }

        fileTransferPanelInFlight = true
        let panel = NSOpenPanel()
        panel.title = title
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.canCreateDirectories = false

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            self.fileTransferPanelInFlight = false
            guard response == .OK, let selectedURL = panel.url else {
                completion([
                    "success": true,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "operation": "upload",
                    "title": title,
                    "canceled": true,
                    "paths": [],
                    "securityScoped": false,
                    "bodyRedacted": true,
                    "readBytesLimit": readBytesLimit,
                    "usedOpenPanel": true
                ])
                return
            }
            completion(self.uploadSelectionResult(command: command,
                                                  mode: "native-appkit-open-panel",
                                                  title: title,
                                                  selectedURL: selectedURL,
                                                  readBytesLimit: readBytesLimit,
                                                  usedOpenPanel: true))
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func uploadSelectionResult(command: String,
                                       mode: String,
                                       title: String,
                                       selectedURL: URL,
                                       readBytesLimit: Int,
                                       usedOpenPanel: Bool) -> [String: Any] {
        guard FileManager.default.fileExists(atPath: selectedURL.path) else {
            return [
                "success": false,
                "command": command,
                "mode": mode,
                "operation": "upload",
                "title": title,
                "canceled": false,
                "paths": [selectedURL.path],
                "path": selectedURL.path,
                "filename": selectedURL.lastPathComponent,
                "securityScoped": false,
                "bodyRedacted": true,
                "readBytesLimit": readBytesLimit,
                "usedOpenPanel": usedOpenPanel,
                "error": "Selected file does not exist"
            ]
        }

        let byteCount = fileByteCount(selectedURL)
        var sampledBytes = 0
        do {
            let handle = try FileHandle(forReadingFrom: selectedURL)
            defer {
                try? handle.close()
            }
            let sampleLimit = max(0, min(readBytesLimit, 64 * 1024))
            let sample = try handle.read(upToCount: sampleLimit)
            sampledBytes = sample?.count ?? 0
        } catch {
            return [
                "success": false,
                "command": command,
                "mode": mode,
                "operation": "upload",
                "title": title,
                "canceled": false,
                "paths": [selectedURL.path],
                "path": selectedURL.path,
                "filename": selectedURL.lastPathComponent,
                "byteCount": byteCount,
                "securityScoped": false,
                "bodyRedacted": true,
                "readBytesLimit": readBytesLimit,
                "usedOpenPanel": usedOpenPanel,
                "error": error.localizedDescription
            ]
        }

        return [
            "success": true,
            "command": command,
            "mode": mode,
            "operation": "upload",
            "title": title,
            "canceled": false,
            "paths": [selectedURL.path],
            "path": selectedURL.path,
            "filename": selectedURL.lastPathComponent,
            "byteCount": byteCount,
            "sampledBytes": sampledBytes,
            "securityScoped": false,
            "bodyRedacted": true,
            "readBytesLimit": readBytesLimit,
            "usedOpenPanel": usedOpenPanel
        ]
    }

    private func resolvedTransferURL(_ rawPath: String) -> URL {
        let expanded = NSString(string: rawPath).expandingTildeInPath
        if expanded.hasPrefix("/") {
            return URL(fileURLWithPath: expanded)
        }
        return URL(fileURLWithPath: config.repoRoot).appendingPathComponent(expanded)
    }

    private func fileByteCount(_ url: URL) -> Int64 {
        guard let attributes = try? FileManager.default.attributesOfItem(atPath: url.path),
              let size = attributes[.size] as? NSNumber else {
            return 0
        }
        return size.int64Value
    }

    private func integerArg(_ args: [String: Any], _ key: String, fallback: Int) -> Int {
        if let value = args[key] as? Int {
            return value
        }
        if let value = args[key] as? NSNumber {
            return value.intValue
        }
        return fallback
    }

    private func capabilityPermission(_ result: [String: Any],
                                      decision: String = "allowed",
                                      reason: String? = nil) -> [String: Any] {
        var payload = result
        payload["permissionMode"] = capabilityManifest.permissionMode
        payload["permissionDecision"] = decision
        payload["permissionReason"] = reason ?? capabilityManifest.allowedReason(result["command"] as? String ?? "")
        payload["permissionScope"] = "window:main"
        payload["permissionManifestLoaded"] = capabilityManifest.loaded
        payload["permissionManifestPath"] = capabilityManifest.path
        payload["permissionManifestCommands"] = capabilityManifest.allowedCommands.sorted().joined(separator: ",")
        return payload
    }

    private var flowIpcResourceStagingDirectory: URL {
        URL(fileURLWithPath: config.repoRoot)
            .appendingPathComponent(".build")
            .appendingPathComponent("vaadin-desktop-host-swift")
            .appendingPathComponent("flow-ipc-resources")
    }

    private func prepareFlowIpcResourceStagingDirectory() {
        let directory = flowIpcResourceStagingDirectory
        try? FileManager.default.removeItem(at: directory)
        do {
            try FileManager.default.createDirectory(at: directory,
                                                    withIntermediateDirectories: true)
        } catch {
            trace("send", "resourceLoad", [
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceLoaderNoLocalhost": true,
                "error": error.localizedDescription,
                "fileDirectoryRedacted": "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources"
            ])
        }
    }

    private func handleHostResourceStageRequest(_ payload: [String: Any]) {
        let requestId = payload["requestId"] as? String ?? ""
        let sourcePath = payload["sourcePath"] as? String ?? ""
        let body = payload["body"] as? String ?? ""
        let sha256 = payload["sha256"] as? String ?? ""
        let artifactFamily = payload["artifactFamily"] as? String ?? ""
        let relativeImportBase = payload["relativeImportBase"] as? String ?? sourcePath
        let mimeType = payload["mimeType"] as? String ?? mimeTypeForFrontendResource(path: sourcePath)
        let data = Data(body.utf8)
        let cacheKey = "\(sourcePath)#\(sha256)#\(data.count)"
        let cacheStatus = hostResourceStageCache.contains(cacheKey) ? "hit" : "miss"
        hostResourceStageCache.insert(cacheKey)

        let resourceKey = sanitizedResourceKey(sourcePath: sourcePath, sha256: sha256, requestId: requestId)
        let fileName = sanitizedResourceFileName(sourcePath)
        let directory = flowIpcResourceStagingDirectory.appendingPathComponent(resourceKey)
        let fileURL = directory.appendingPathComponent(fileName)
        let urlPath = "/\(resourceKey)/\(fileName)"
        let stagedUrl = "\(FlowIpcResourceSchemeHandler.schemeName)://flow-ipc\(urlPath)"
        let filePathRedacted = "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources/\(resourceKey)/\(fileName)"
        let fileDirectoryRedacted = "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources"

        trace("recv", "resourceLoad", [
            "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
            "frontendResourceSchemeName": FlowIpcResourceSchemeHandler.schemeName,
            "frontendResourceLoaderMode": "custom-scheme-file-backed",
            "stagingMode": "custom-scheme",
            "frontendResourceSchemeHandlerInstalled": true,
            "frontendResourceSchemeRequestObserved": false,
            "frontendResourceSchemeResponseObserved": false,
            "frontendResourceLoaderNoLocalhost": true,
            "frontendResourceFileBacked": true,
            "requestId": requestId,
            "sourcePath": sourcePath,
            "artifactFamily": artifactFamily,
            "mimeType": mimeType,
            "bytes": data.count,
            "sha256": sha256,
            "cacheStatus": cacheStatus,
            "relativeImportBase": relativeImportBase,
            "resourceBaseUrl": stagedUrl,
            "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17",
            "filePathRedacted": filePathRedacted,
            "fileDirectoryRedacted": fileDirectoryRedacted
        ])

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
                                                                          relativeImportBase: relativeImportBase))
            sendHostResourceStageResponse([
                "requestId": requestId,
                "success": true,
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
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
                "relativeImportResolutionMode": "scheme-url-native-relative-base-v1_17",
                "resolvedRelativeImportPaths": "",
                "missingRelativeImportPaths": "",
                "sourcePath": sourcePath,
                "artifactFamily": artifactFamily,
                "mimeType": mimeType,
                "bytes": data.count,
                "sha256": sha256,
                "cacheStatus": cacheStatus,
                "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17",
                "filePathRedacted": filePathRedacted,
                "fileDirectoryRedacted": fileDirectoryRedacted,
                "noLocalhostProof": "custom-scheme-served-by-wkurlschemehandler-no-http-v1_17"
            ])
            trace("send", "resourceLoad", [
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
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
                "bytes": data.count,
                "sha256": sha256,
                "cacheStatus": cacheStatus,
                "relativeImportBase": relativeImportBase,
                "resourceBaseUrl": stagedUrl,
                "cleanupPolicy": "cleanup-owned-window-lifetime-file-backed-v1_17",
                "filePathRedacted": filePathRedacted,
                "fileDirectoryRedacted": fileDirectoryRedacted
            ])
        } catch {
            sendHostResourceStageResponse([
                "requestId": requestId,
                "success": false,
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
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
            ])
        }
    }

    private func sendHostResourceStageResponse(_ response: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: response),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "resourceLoad", [
                "frontendResourceSchemeVersion": "flow-ipc-resource-scheme-v1_17",
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceLoaderNoLocalhost": true,
                "error": "host-resource-stage-response-serialize-failed"
            ])
            return
        }
        webView?.evaluateJavaScript("window.VaadinDesktopIpcHostResourceStageResponse && window.VaadinDesktopIpcHostResourceStageResponse(\(json));")
    }

    private func sanitizedResourceKey(sourcePath: String, sha256: String, requestId: String) -> String {
        let hashPrefix = sha256.isEmpty ? sanitizedPathComponent(requestId) : String(sha256.prefix(16))
        let name = sanitizedPathComponent(sourcePath)
        let suffix = name.isEmpty ? "resource" : String(name.prefix(80))
        return "\(hashPrefix)-\(suffix)"
    }

    private func sanitizedResourceFileName(_ sourcePath: String) -> String {
        let last = URL(fileURLWithPath: sourcePath).lastPathComponent
        let candidate = last.isBlank ? "resource.js" : last
        let sanitized = sanitizedPathComponent(candidate)
        if sanitized.contains(".") {
            return sanitized
        }
        return "\(sanitized).js"
    }

    private func sanitizedPathComponent(_ value: String) -> String {
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

    private func mimeTypeForFrontendResource(path: String) -> String {
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

    private func deliverFlowIpcEnvelope(_ object: [String: Any], type: String) {
        guard let data = try? JSONSerialization.data(withJSONObject: object),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "error", [
                "code": "flowIpcSerializeFailed",
                "type": type
            ])
            return
        }
        deliverFlowIpcEnvelope(json, type: type)
    }

    private func deliverFlowIpcEnvelope(_ raw: String, type: String) {
        if let webView {
            let script = "window.VaadinDesktopIpc && window.VaadinDesktopIpc.receive(\(raw));"
            webView.evaluateJavaScript(script) { _, error in
                if let error {
                    trace("send", "error", [
                        "code": "flowIpcDeliverFailed",
                        "message": error.localizedDescription,
                        "type": type
                    ])
                }
            }
        }
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "flowIpc" else {
            return
        }
        guard let object = message.body as? [String: Any],
              let data = try? JSONSerialization.data(withJSONObject: object),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "error", [
                "code": "invalidWebViewFlowIpcMessage",
                "body": String(describing: message.body)
            ])
            return
        }
        let messageType = object["type"] as? String ?? "unknown"
        if messageType == "hostResourceStageRequest" {
            handleHostResourceStageRequest(object["payload"] as? [String: Any] ?? [:])
            return
        }
        if messageType == "webview.firstMeaningfulPaint" {
            trace("recv", messageType, object["payload"] as? [String: Any] ?? [:])
            return
        }
        if messageType == "resourceStage" {
            trace("recv", messageType, object["payload"] as? [String: Any] ?? [:])
            return
        }
        trace("send", messageType, [
            "target": "java-sidecar",
            "transport": "flow-ipc",
            "envelope": object
        ])
        sidecar?.sendFlowIpc(json)
    }

    func windowWillClose(_ notification: Notification) {
        trace("recv", "window.closeRequested", ["label": "main"])
        sidecar?.terminate()
        NSApp.terminate(nil)
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        window?.sharingType = .readOnly
        trace("recv", "webview.loaded", [
            "label": "main",
            "title": webView.title ?? "",
            "windowSharingType": window?.sharingType.rawValue ?? -1
        ])

        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            self.inspectWebView(webView)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.5) {
            self.inspectWebView(webView)
        }
        let extendedFlowIpcVerification = config.flowIpcAutoFrameworkUpdate
            || config.flowIpcAutoVaadinTextField
            || config.flowIpcAutoResync
            || config.flowIpcAutoError
        if extendedFlowIpcVerification {
            DispatchQueue.main.asyncAfter(deadline: .now() + 9.0) {
                self.inspectWebView(webView)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 16.0) {
                self.inspectWebView(webView)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 24.0) {
                self.inspectWebView(webView)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 34.0) {
                self.inspectWebView(webView)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 42.0) {
                self.inspectWebView(webView)
            }
        }
        if config.flowIpcAutoFrameworkUpdate {
            DispatchQueue.main.asyncAfter(deadline: .now() + 20.0) {
                self.inspectWebView(webView)
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 28.0) {
                self.inspectWebView(webView)
            }
        }
        let snapshotDelay = config.flowIpcAutoFrameworkUpdate ? 29.2 : (extendedFlowIpcVerification ? 42.6 : 5.0)
        DispatchQueue.main.asyncAfter(deadline: .now() + snapshotDelay) {
            self.writeSnapshot(webView)
        }
        if config.flowIpcAutoShutdown {
            scheduleAutoShutdown(webView, extendedFlowIpcVerification: extendedFlowIpcVerification)
        }
    }

    private func scheduleAutoShutdown(_ webView: WKWebView, extendedFlowIpcVerification: Bool) {
        if !extendedFlowIpcVerification {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5.6) {
                trace("recv", "window.closeRequested", [
                    "label": "main",
                    "reason": "flow-ipc-auto-shutdown"
                ])
                self.window?.close()
            }
            return
        }

        let timeoutSeconds = config.flowIpcAutoFrameworkUpdate ? 46.0 : 120.0
        let deadline = Date().addingTimeInterval(timeoutSeconds)
        pollAutoShutdownReadiness(webView, deadline: deadline)
    }

    private func pollAutoShutdownReadiness(_ webView: WKWebView, deadline: Date) {
        let script = """
        (() => {
          const d = window.VaadinDesktopIpcDiagnostics || {};
          const componentEventsNeeded =
            !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__
            || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__;
          const smokeComplete = !!d.flowClientEngineEventPatchConsumed
            && (!componentEventsNeeded || (
              !!d.flowClientEngineTextInputEventObserved
              && !!d.flowClientEngineCheckboxEventObserved
              && !!d.vaadinWebComponentEventRpcObserved
              && !!d.vaadinTextFieldEventRpcObserved
              && !!d.vaadinSecondWebComponentEventRpcObserved
            ))
            && (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ || !!d.flowClientEngineResyncPatchConsumed)
            && (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR__ || !!d.flowClientEngineErrorReported)
            && (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__ || (
              d.frontendArtifactTextFieldExecutionVersion === 'flow-ipc-textfield-v1_7'
              && !!d.frontendArtifactTextFieldExecuted
              && !d.frontendArtifactTextFieldShimUsed
            ))
            && !!d.flowClientResourceExecutionClassificationComplete
            && !!d.vaadinWebComponentResourceGraphVerified
            && !!d.frontendStagingObserved;
          const frameworkComplete = !!d.frameworkAppInteractionComplete;
          const complete = window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__
            ? frameworkComplete
            : smokeComplete;
          return JSON.stringify({
            complete,
            smokeComplete,
            frameworkComplete,
            click: !!d.flowClientEngineEventPatchConsumed,
            text: !!d.flowClientEngineTextInputEventObserved,
            checkbox: !!d.flowClientEngineCheckboxEventObserved,
            bigDecimal: !!d.vaadinWebComponentEventRpcObserved,
            textField: !!d.vaadinTextFieldEventRpcObserved,
            vaadinCheckbox: !!d.vaadinSecondWebComponentEventRpcObserved,
            resync: !!d.flowClientEngineResyncPatchConsumed,
            error: !!d.flowClientEngineErrorReported,
            flowGraph: !!d.flowClientResourceExecutionClassificationComplete,
            componentGraph: !!d.vaadinWebComponentResourceGraphVerified,
            staging: !!d.frontendStagingObserved,
            textFieldBlocker: !!d.frontendArtifactTextFieldStagedBlocker,
            textFieldBlockerVersion: d.frontendArtifactTextFieldBlockerVersion || '',
            textFieldExecutionVersion: d.frontendArtifactTextFieldExecutionVersion || '',
            textFieldExecuted: !!d.frontendArtifactTextFieldExecuted
          });
        })();
        """
        webView.evaluateJavaScript(script) { result, _ in
            let readiness = result as? String ?? "{}"
            if readiness.contains("\"complete\":true") || Date() >= deadline {
                self.inspectWebView(webView)
                self.writeSnapshot(webView) {
                    trace("recv", "window.closeRequested", [
                        "label": "main",
                        "reason": readiness.contains("\"complete\":true")
                            ? "flow-ipc-auto-shutdown-complete"
                            : "flow-ipc-auto-shutdown-timeout",
                        "readiness": readiness
                    ])
                    self.window?.close()
                }
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                self.pollAutoShutdownReadiness(webView, deadline: deadline)
            }
        }
    }

    private func inspectWebView(_ webView: WKWebView) {
        let script = """
        JSON.stringify({
          readyState: document.readyState,
          title: document.title,
          location: window.location.href,
          bodyText: (() => {
            if (!document.body) {
              return "";
            }
            const clone = document.body.cloneNode(true);
            return clone.innerText.slice(0, 500);
          })(),
          hasVaadinRoot: !!document.querySelector("vaadin-app-layout, vaadin-vertical-layout, flow-container-root, #flow-client-engine-root .vatori-flow-viewport-root"),
          elementCount: document.body ? document.body.querySelectorAll("*").length : 0,
          flowClientResourceLoaded: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceLoaded),
          flowClientResourcePath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourcePath : "",
          flowClientResourceBytes: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceBytes : 0,
          flowClientResourceVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified),
          flowClientResourceModuleLoaded: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceModuleLoaded),
          flowClientBootstrapExported: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientBootstrapExported),
          flowClientBootstrapInitialized: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized),
          flowClientBootstrapAppId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientBootstrapAppId : "",
          flowClientResourceManifestCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount : 0,
          flowClientResourceGraphRequestedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphRequestedCount : 0,
          flowClientResourceGraphResponseCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphResponseCount : 0,
          flowClientResourceGraphPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphPaths : "",
          flowClientResourceGraphResponsePaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphResponsePaths : "",
          flowClientResourceGraphComplete: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphComplete),
          flowClientResourceGraphVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphVerified),
          flowClientResourceExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceExecuted),
          flowClientResourceVerifiedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedCount : 0,
          flowClientResourceVerifiedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedPaths : "",
          flowClientResourceExecutedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutedCount : 0,
          flowClientResourceExecutedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutedPaths : "",
          flowClientResourceVerifiedOnlyCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedOnlyCount : 0,
          flowClientResourceVerifiedOnlyPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedOnlyPaths : "",
          flowClientResourceIntentionallyNotExecutedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceIntentionallyNotExecutedCount : 0,
          flowClientResourceIntentionallyNotExecutedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceIntentionallyNotExecutedPaths : "",
          flowClientResourceExecutionFailedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionFailedCount : 0,
          flowClientResourceExecutionFailedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionFailedPaths : "",
          flowClientResourceExecutionClassificationComplete: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionClassificationComplete),
          flowClientResourceLazyLookupRequested: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupRequested),
          flowClientResourceLazyLookupResponse: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupResponse),
          flowClientResourceLazyLookupVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupVerified),
          flowClientResourceLazyLookupPurpose: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupPurpose : "",
          flowClientResourceLazyLookupKind: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupKind : "",
          flowClientResourceLazyLookupPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupPath : "",
          flowClientResourceLazyLookupResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupResult : "",
          flowClientResourceLoadOnDemandProbeResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceLoadOnDemandProbeResult : "",
          flowClientResourceBootstrapLazyEntriesResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceBootstrapLazyEntriesResult : "",
          flowClientResourceStaticLookupVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientResourceStaticLookupVerified),
          flowClientResourceStaticLookupPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceStaticLookupPath : "",
          flowClientResourceThemeLookupResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceThemeLookupResult : "",
          frontendAssetPipelineVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendAssetPipelineVersion : "",
          frontendArtifactGraphSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphSource : "",
          frontendArtifactGraphManifestObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphManifestObserved),
          frontendArtifactFamilies: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilies : "",
          frontendArtifactFamilyCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilyCount : 0,
          frontendArtifactGraphDependencyObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphDependencyObserved),
          frontendArtifactGraphRequestedBy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphRequestedBy : "",
          frontendArtifactGraphDependencyOf: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphDependencyOf : "",
          frontendArtifactGraphModuleSpecifiers: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphModuleSpecifiers : "",
          frontendArtifactExecutionPolicies: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactExecutionPolicies : "",
          frontendArtifactStatuses: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactStatuses : "",
          frontendArtifactStrictBlockers: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactStrictBlockers : "",
          frontendArtifactStrictBlockerCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactStrictBlockerCount : 0,
          frontendArtifactShimFallback: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactShimFallback),
          frontendArtifactProductionBundlePartial: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundlePartial),
          frontendArtifactProductionBundleStatsVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsVerified),
          frontendArtifactProductionBundleStatsPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsPath : "",
          frontendArtifactProductionBundleStatsResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsResult : "",
          frontendArtifactThemeStaticVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticVerified),
          frontendArtifactThemeStaticPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticPath : "",
          frontendArtifactThemeStaticBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticBlocker),
          frontendArtifactGeneratedLazyVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyVerified),
          frontendArtifactGeneratedLazyPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyPath : "",
          frontendArtifactGeneratedLazyBundleChunkVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkVerified),
          frontendArtifactGeneratedLazyBundleChunkPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkPath : "",
          frontendArtifactGeneratedLazyBundleChunkResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkResult : "",
          frontendArtifactGeneratedLazyExecutionAttempted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionAttempted),
          frontendArtifactGeneratedLazyExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecuted),
          frontendArtifactGeneratedLazyExecutionResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionResult : "",
          frontendArtifactGeneratedLazyExecutionError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionError : "",
          frontendArtifactGeneratedLazyLoadOnDemandResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandResult : "",
          frontendArtifactGeneratedLazyLoadOnDemandContractVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandContractVersion : "",
          frontendArtifactGeneratedLazyTriggerSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyTriggerSource : "",
          frontendArtifactGeneratedLazyApiOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiOwner : "",
          frontendArtifactGeneratedLazyApiInstallSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiInstallSource : "",
          frontendArtifactGeneratedLazyRequestedChunk: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRequestedChunk : "",
          frontendArtifactGeneratedLazyRegistrySource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRegistrySource : "",
          frontendArtifactGeneratedLazyMappingSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyMappingSource : "",
          frontendArtifactGeneratedLazyResolvedArtifactPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedArtifactPath : "",
          frontendArtifactGeneratedLazyFlowClientTriggered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFlowClientTriggered),
          frontendArtifactGeneratedLazyDiagnosticBridge: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyDiagnosticBridge),
          frontendArtifactGeneratedLazyLoadOnDemandBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandBlocker),
          frontendArtifactGeneratedLazyResolverVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolverVersion : "",
          frontendArtifactGeneratedLazyExecutionVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionVersion : "",
          frontendArtifactGeneratedLazyImportResolutionStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyImportResolutionStrategy : "",
          frontendArtifactGeneratedLazyRelativeImportSpecifier: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeImportSpecifier : "",
          frontendArtifactGeneratedLazyResolvedDependencyPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedDependencyPath : "",
          frontendArtifactGeneratedLazyRelativeDependencyResolved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeDependencyResolved),
          frontendArtifactGeneratedLazyFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFailureLayer : "",
          frontendArtifactGeneratedLazyBlockerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlockerVersion : "",
          frontendArtifactGeneratedLazyNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyNextResolutionStep : "",
          frontendArtifactGeneratedLazyModuleIdentityStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyModuleIdentityStrategy : "",
          frontendArtifactGeneratedLazyModuleIdentityKey: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyModuleIdentityKey : "",
          generatedLazyChunkUidlExpressionObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.generatedLazyChunkUidlExpressionObserved),
          generatedLazyChunkExpressionSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource : "",
          generatedLazyChunkExpression: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpression : "",
          generatedLazyChunkId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkId : "",
          generatedLazyChunkIds: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkIds : "",
          generatedLazyChunkTriggerOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner : "",
          generatedLazyChunkTriggeredByFlowClient: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggeredByFlowClient),
          generatedLazyChunkDiagnosticBridgeUsed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.generatedLazyChunkDiagnosticBridgeUsed),
          generatedLazyChunkDirectImportProbeUsed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.generatedLazyChunkDirectImportProbeUsed),
          generatedLazyChunkResolvedArtifactPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkResolvedArtifactPath : "",
          generatedLazyChunkFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer : "",
          generatedLazyChunkBlockerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkBlockerVersion : "",
          generatedLazyChunkNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLazyChunkNextResolutionStep : "",
          loadOnDemandChunkContractVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkContractVersion : "",
          loadOnDemandChunkApiOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiOwner : "",
          loadOnDemandChunkApiInstallSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiInstallSource : "",
          loadOnDemandChunkRegistrySource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRegistrySource : "",
          loadOnDemandChunkMappingSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkMappingSource : "",
          loadOnDemandChunkRequestedChunk: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRequestedChunk : "",
          loadOnDemandChunkResolvedArtifactPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkResolvedArtifactPath : "",
          loadOnDemandChunkModuleIdentity: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkModuleIdentity : "",
          loadOnDemandChunkTriggeredByFlowClient: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkTriggeredByFlowClient),
          loadOnDemandChunkStageObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkStageObserved),
          loadOnDemandChunkExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecuted),
          loadOnDemandChunkExecutionResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecutionResult : "",
          loadOnDemandChunkFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer : "",
          loadOnDemandChunkBrowserError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBrowserError : "",
          loadOnDemandChunkBlockerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBlockerVersion : "",
          loadOnDemandChunkNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkNextResolutionStep : "",
          generatedLoadOnDemandChunkFileRegistryVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistryVersion : "",
          generatedLoadOnDemandChunkFileRegistrySource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistrySource : "",
          generatedLoadOnDemandChunkFileMappingSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileMappingSource : "",
          generatedLoadOnDemandGeneratedImportsArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandGeneratedImportsArtifact : "",
          generatedLoadOnDemandSourceBuildMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceBuildMode : "",
          generatedLoadOnDemandSourceArtifactFamily: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceArtifactFamily : "",
          generatedLoadOnDemandNoOpLoadOnDemand: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandNoOpLoadOnDemand),
          loadOnDemandTableShape: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandTableShape : "",
          chunkImportSpecifier: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.chunkImportSpecifier : "",
          chunkArtifactPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.chunkArtifactPath : "",
          chunkFileResolver: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.chunkFileResolver : "",
          chunkFileStatus: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.chunkFileStatus : "",
          chunkFileBytes: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.chunkFileBytes : 0,
          chunkFileSha256: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.chunkFileSha256 : "",
          noChunkGeneratedReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.noChunkGeneratedReason : "",
          loadOnDemandChunkFileTriggeredByFlowClient: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileTriggeredByFlowClient),
          loadOnDemandChunkFileStageObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileStageObserved),
          loadOnDemandChunkFileExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecuted),
          loadOnDemandChunkFileCallbackResolved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileCallbackResolved),
          loadOnDemandChunkFileExecutionResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecutionResult : "",
          loadOnDemandChunkFileSideEffectProof: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileSideEffectProof : "",
          loadOnDemandChunkFileFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer : "",
          loadOnDemandChunkFileBrowserError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBrowserError : "",
          loadOnDemandChunkFileBlockerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBlockerVersion : "",
          loadOnDemandChunkFileNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileNextResolutionStep : "",
          frontendRuntimeResolverContractVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverContractVersion : "",
          frontendRuntimeResolverLogicalRequestIds: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverLogicalRequestIds : "",
          frontendRuntimeResolverPathMappings: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverPathMappings : "",
          flowLazyDependencyObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowLazyDependencyObserved),
          flowLazyDependencyRequested: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowLazyDependencyRequested),
          flowLazyDependencyResponse: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowLazyDependencyResponse),
          flowLazyDependencyStageObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowLazyDependencyStageObserved),
          flowLazyDependencyExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecuted),
          flowLazyDependencyVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyVersion : "",
          flowLazyDependencySource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencySource : "",
          flowLazyDependencyMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyMode : "",
          flowLazyDependencyExpression: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExpression : "",
          flowLazyDependencyChunkId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyChunkId : "",
          flowLazyDependencyResolvedPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyResolvedPath : "",
          flowLazyDependencyTriggerOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggerOwner : "",
          flowLazyDependencyTriggeredByFlowClient: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggeredByFlowClient),
          flowLazyDependencyExecutionResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecutionResult : "",
          flowLazyDependencyFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyFailureLayer : "",
          flowLazyDependencyBrowserError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowLazyDependencyBrowserError : "",
          frontendArtifactProductionBundleRuntimeObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeObserved),
          frontendArtifactProductionBundleRuntimeStageAttempted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStageAttempted),
          frontendArtifactProductionBundleRuntimeExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecuted),
          frontendArtifactProductionBundleRuntimeBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker),
          frontendArtifactProductionBundleRuntimeVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeVersion : "",
          frontendArtifactProductionBundleRuntimeRegistrySource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeRegistrySource : "",
          frontendArtifactProductionBundleRuntimeStatsSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStatsSource : "",
          frontendArtifactProductionBundleRuntimeSelectedArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeSelectedArtifact : "",
          frontendArtifactProductionBundleRuntimeDependencyChain: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeDependencyChain : "",
          frontendArtifactProductionBundleRuntimeModuleIdentity: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeModuleIdentity : "",
          frontendArtifactProductionBundleRuntimeExecutionResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult : "",
          frontendArtifactProductionBundleRuntimeFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeFailureLayer : "",
          frontendArtifactProductionBundleRuntimeBrowserError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError : "",
          frontendArtifactProductionBundleRuntimeNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeNextResolutionStep : "",
          frontendArtifactProductionBundleRuntimeOwnerObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerObserved),
          frontendArtifactProductionBundleRuntimeOwnerStageObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageObserved),
          frontendArtifactProductionBundleRuntimeOwnerImportAttempted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAttempted),
          frontendArtifactProductionBundleRuntimeOwnerImported: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImported),
          frontendArtifactProductionBundleRuntimeOwnerBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlocker),
          frontendArtifactProductionBundleRuntimeOwnerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerVersion : "",
          frontendArtifactProductionBundleRuntimeOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwner : "",
          frontendArtifactProductionBundleRuntimeOwnerEntryArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerEntryArtifact : "",
          frontendArtifactProductionBundleRuntimeOwnerSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSource : "",
          frontendArtifactProductionBundleRuntimeOwnerSelectedBy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSelectedBy : "",
          frontendArtifactProductionBundleRuntimeOwnerModuleIdentity: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerModuleIdentity : "",
          frontendArtifactProductionBundleRuntimeOwnerLifecycle: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerLifecycle : "",
          frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy : "",
          frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy : "",
          frontendArtifactProductionBundleRuntimeOwnerCachePolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCachePolicy : "",
          frontendArtifactProductionBundleRuntimeOwnerCacheStatus: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCacheStatus : "",
          frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy : "",
          frontendArtifactProductionBundleRuntimeOwnerExecutionResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionResult : "",
          frontendArtifactProductionBundleRuntimeOwnerSideEffectProof: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSideEffectProof : "",
          frontendArtifactProductionBundleRuntimeOwnerFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer : "",
          frontendArtifactProductionBundleRuntimeOwnerBrowserError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBrowserError : "",
          frontendArtifactProductionBundleRuntimeOwnerBlockerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlockerVersion : "",
          frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep : "",
          frontendArtifactProductionBundleRuntimeOwnerRegistryBefore: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerRegistryBefore : "",
          frontendArtifactProductionBundleRuntimeOwnerRegistryAfter: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerRegistryAfter : "",
          frontendArtifactProductionBundleRuntimeOwnerDuplicateTag: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDuplicateTag : "",
          frontendArtifactProductionBundleRuntimeOwnerFirstDefinitionOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFirstDefinitionOwner : "",
          frontendArtifactProductionBundleRuntimeOwnerSecondDefinitionOwner: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSecondDefinitionOwner : "",
          frontendArtifactProductionBundleRuntimeOwnerImportVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVersion : "",
          frontendArtifactProductionBundleRuntimeOwnerImportPolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPolicy : "",
          frontendArtifactProductionBundleRuntimeOwnerImportArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportArtifact : "",
          frontendArtifactProductionBundleRuntimeOwnerImportPhase: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhase : "",
          frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates : "",
          frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes : "",
          frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects : "",
          frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures : "",
          frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy : "",
          frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths : "",
          frontendArtifactProductionBundleRuntimeOwnerImportResolvedDependencyPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportResolvedDependencyPaths : "",
          frontendArtifactProductionBundleRuntimeOwnerImportUnresolvedSpecifiers: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportUnresolvedSpecifiers : "",
          frontendArtifactProductionBundleRuntimeOwnerImportDependencyRewriteResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyRewriteResult : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact : "",
          frontendArtifactProductionBundleRuntimeOwnerDependencyGraphClosed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphClosed),
          frontendArtifactProductionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount : 0,
          frontendArtifactProductionBundleRuntimeOwnerDependencyGraphResolvedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphResolvedCount : 0,
          frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMissingCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMissingCount : 0,
          frontendArtifactProductionBundleRuntimeOwnerDependencyGraphStagedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphStagedCount : 0,
          frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMaxDepth: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMaxDepth : 0,
          frontendArtifactProductionBundleRuntimeOwnerDependencyClassifications: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyClassifications : "",
          frontendArtifactProductionBundleRuntimeOwnerImportMissingDependencyPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportMissingDependencyPaths : "",
          frontendArtifactProductionBundleRuntimeOwnerImportMissingSpecifiers: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportMissingSpecifiers : "",
          frontendArtifactProductionBundleRuntimeOwnerImportStageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportStageCount : 0,
          frontendArtifactProductionBundleRuntimeOwnerImportModuleIdentity: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportModuleIdentity : "",
          frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalBefore: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalBefore : "",
          frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalAfter: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalAfter : "",
          frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep : "",
          frontendArtifactProductionBundleRuntimeOwnerImportBrowserErrorName: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportBrowserErrorName : "",
          frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion : "",
          frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy : "",
          frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult : "",
          frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases : "",
          frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures : "",
          componentProductionOwnerImportOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerImportOrder : "",
          componentProductionOwnerReuseVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseVersion : "",
          componentProductionOwnerReusePolicy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReusePolicy : "",
          componentProductionOwnerReuseMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseMode : "",
          componentProductionOwnerReuseAcceptableFailures: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseAcceptableFailures : "",
          componentProductionOwnerReused: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused),
          componentProductionOwnerReuseReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason : "",
          componentProductionOwnerOwnedTags: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerOwnedTags : "",
          componentProductionOwnerSkippedDefinitions: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerSkippedDefinitions : "",
          componentProductionOwnerFallbackImports: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerFallbackImports : "",
          componentProductionOwnerDependencyPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths : "",
          frontendArtifactGeneratedLazyStagedDependencyGraph: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyStagedDependencyGraph : "",
          frontendArtifactDevModeVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModeVerified),
          frontendArtifactDevModePath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePath : "",
          frontendArtifactDevModePackageGraphResult: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePackageGraphResult : "",
          frontendArtifactDevModeBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModeBlocker),
          frontendArtifactGeneratedLazyBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker),
          frontendStagingResolverVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagingResolverVersion : "",
          frontendStagingObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendStagingObserved),
          frontendStagedResourceCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceCount : 0,
          frontendStagedResourcePaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourcePaths : "",
          frontendStagedResourceFamilies: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceFamilies : "",
          frontendStagedResourceSchemes: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceSchemes : "",
          frontendStagedResourceCacheHitCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceCacheHitCount : 0,
          frontendStagedResourceCacheMissCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceCacheMissCount : 0,
          frontendStagedResourceExecutedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceExecutedCount : 0,
          frontendStagedResourceExecutedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceExecutedPaths : "",
          frontendStagedResourceFailedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceFailedCount : 0,
          frontendStagedResourceFailedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendStagedResourceFailedPaths : "",
          frontendResourceSchemeVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeVersion : "",
          frontendResourceLoaderMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderMode : "",
          frontendResourceSchemeName: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeName : "",
          frontendResourceSchemeHandlerInstalled: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeHandlerInstalled),
          frontendResourceSchemeRequestObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeRequestObserved),
          frontendResourceSchemeResponseObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeResponseObserved),
          frontendResourceLoaderNoLocalhost: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderNoLocalhost),
          frontendResourceFileBacked: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceFileBacked),
          frontendResourceLoaderObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderObserved),
          frontendResourceLoaderObservedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderObservedPaths : "",
          frontendResourceLoaderExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderExecuted),
          frontendResourceLoaderExecutedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderExecutedPaths : "",
          frontendResourceBlobFallbackObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendResourceBlobFallbackObserved),
          frontendResourceBlobFallbackPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceBlobFallbackPaths : "",
          frontendResourceMimeType: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceMimeType : "",
          frontendResourceFilePathRedacted: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceFilePathRedacted : "",
          frontendResourceFileDirectoryRedacted: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceFileDirectoryRedacted : "",
          frontendResourceBaseUrl: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceBaseUrl : "",
          frontendResourceRelativeImportBase: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceRelativeImportBase : "",
          frontendResourceRelativeImportResolutionMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceRelativeImportResolutionMode : "",
          frontendResourceResolvedRelativeImportPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceResolvedRelativeImportPaths : "",
          frontendResourceMissingRelativeImportPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceMissingRelativeImportPaths : "",
          frontendResourceNoLocalhostProof: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendResourceNoLocalhostProof : "",
          frontendArtifactTextFieldStagedBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlocker),
          frontendArtifactTextFieldStagedBlockerPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerPath : "",
          frontendArtifactTextFieldStagedBlockerReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerReason : "",
          frontendArtifactTextFieldStagedBlockerError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerError : "",
          frontendArtifactTextFieldStagedBlockerDependency: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerDependency : "",
          frontendArtifactTextFieldGraphParityVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphParityVersion : "",
          frontendArtifactTextFieldBlockerVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldBlockerVersion : "",
          frontendArtifactTextFieldGraphProbe: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphProbe : "",
          frontendArtifactTextFieldEntryArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldEntryArtifact : "",
          frontendArtifactTextFieldTransitiveArtifacts: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldTransitiveArtifacts : "",
          frontendArtifactTextFieldImportResolutionStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldImportResolutionStrategy : "",
          frontendArtifactTextFieldSideEffectVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectVersion : "",
          frontendArtifactTextFieldSideEffectStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectStrategy : "",
          frontendArtifactTextFieldExecutionVersion: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecutionVersion : "",
          frontendArtifactTextFieldExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecuted),
          frontendArtifactTextFieldShimUsed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldShimUsed),
          frontendArtifactTextFieldClassExportArtifact: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldClassExportArtifact : "",
          frontendArtifactTextFieldFirstDefinitionSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldFirstDefinitionSource : "",
          frontendArtifactTextFieldSecondDefinitionSource: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSecondDefinitionSource : "",
          frontendArtifactTextFieldDuplicateTag: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldDuplicateTag : "",
          frontendArtifactTextFieldRemainingBlockerClassification: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldRemainingBlockerClassification : "",
          frontendArtifactTextFieldModuleIdentityStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldModuleIdentityStrategy : "",
          frontendArtifactTextFieldModuleIdentityPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldModuleIdentityPaths : "",
          frontendArtifactTextFieldNarrowedFailureClassification: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldNarrowedFailureClassification : "",
          frontendArtifactTextFieldFailureLayer: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldFailureLayer : "",
          frontendArtifactTextFieldMinimalReproduction: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldMinimalReproduction : "",
          frontendArtifactTextFieldNextResolutionStep: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldNextResolutionStep : "",
          frontendArtifactTextFieldCustomElementRegistryBefore: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldCustomElementRegistryBefore : "",
          frontendArtifactTextFieldCustomElementRegistryAfter: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldCustomElementRegistryAfter : "",
          flowClientBootstrapInitError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitError : "",
          flowClientResourceExecutionError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionError : "",
          vaadinWebComponentResourceManifestObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestObserved),
          vaadinWebComponentResourceManifestCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestCount : 0,
          vaadinWebComponentResourceManifestPrimaryPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestPrimaryPath : "",
          vaadinWebComponentResourceManifestBlockedReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestBlockedReason : "",
          vaadinWebComponentResourceManifestFailureClassification: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestFailureClassification : "",
          vaadinWebComponentResourceResolverStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResolverStrategy : "",
          vaadinWebComponentResourceRequested: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceRequested),
          vaadinWebComponentResourceRequestedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceRequestedCount : 0,
          vaadinWebComponentResourceRequestedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceRequestedPaths : "",
          vaadinWebComponentResourceResponseCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResponseCount : 0,
          vaadinWebComponentResourceResponsePaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResponsePaths : "",
          vaadinWebComponentResourceResolved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResolved),
          vaadinWebComponentResourceVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceVerified),
          vaadinWebComponentResourceVerifiedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceVerifiedCount : 0,
          vaadinWebComponentResourceVerifiedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceVerifiedPaths : "",
          vaadinWebComponentResourceExecuted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecuted),
          vaadinWebComponentResourceExecutedPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath : "",
          vaadinWebComponentResourceExecutedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedCount : 0,
          vaadinWebComponentResourceExecutedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPaths : "",
          vaadinWebComponentResourceExecutedTags: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedTags : "",
          vaadinWebComponentResourceExecutionFailed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionFailed),
          vaadinWebComponentResourceExecutionFailedCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionFailedCount : 0,
          vaadinWebComponentResourceExecutionFailedPaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionFailedPaths : "",
          vaadinWebComponentResourceGraphComplete: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphComplete),
          vaadinWebComponentResourceGraphVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified),
          vaadinWebComponentResourceBlockedReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceBlockedReason : "",
          vaadinWebComponentResourceFailureClassification: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceFailureClassification : "",
          vaadinWebComponentResourceShimmedImports: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceShimmedImports : "",
          vaadinWebComponentResourceImportMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode : "",
          vaadinWebComponentImportRewriteStrategy: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewriteStrategy : "",
          vaadinWebComponentImportRewriteCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewriteCount : 0,
          vaadinWebComponentImportRewriteSpecifiers: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewriteSpecifiers : "",
          vaadinWebComponentImportRewritePaths: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewritePaths : "",
          vaadinWebComponentResourceExecutionError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError : "",
          vaadinWebComponentTagDefined: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentTagDefined),
          vaadinWebComponentRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRendered),
          vaadinWebComponentRenderedTag: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRenderedTag : "",
          vaadinWebComponentRenderedValue: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRenderedValue : "",
          vaadinWebComponentEventDispatchMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventDispatchMode : "",
          vaadinWebComponentEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventObserved),
          vaadinWebComponentEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventRpcObserved),
          vaadinWebComponentEventComponentId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventComponentId : "",
          vaadinWebComponentEventType: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventType : "",
          vaadinWebComponentEventValue: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventValue : "",
          vaadinWebComponentStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentStatusText : "",
          vaadinWebComponentPostEventState: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentPostEventState : "",
          vaadinWebComponentFailureReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason : "",
          vaadinSecondWebComponentTagDefined: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined),
          vaadinSecondWebComponentRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRendered),
          vaadinSecondWebComponentRenderedTag: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRenderedTag : "",
          vaadinSecondWebComponentRenderedValue: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRenderedValue : "",
          vaadinSecondWebComponentEventDispatchMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventDispatchMode : "",
          vaadinSecondWebComponentEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved),
          vaadinSecondWebComponentEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventRpcObserved),
          vaadinSecondWebComponentEventComponentId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventComponentId : "",
          vaadinSecondWebComponentEventType: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventType : "",
          vaadinSecondWebComponentEventValue: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventValue : "",
          vaadinSecondWebComponentStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentStatusText : "",
          vaadinSecondWebComponentPostEventState: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentPostEventState : "",
          vaadinSecondWebComponentFailureReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentFailureReason : "",
          vaadinTextFieldRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRendered),
          vaadinTextFieldRenderedTag: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRenderedTag : "",
          vaadinTextFieldRenderedValue: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRenderedValue : "",
          vaadinTextFieldTagDefined: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinTextFieldTagDefined),
          vaadinTextFieldEventDispatchMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventDispatchMode : "",
          vaadinTextFieldEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventObserved),
          vaadinTextFieldEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventRpcObserved),
          vaadinTextFieldEventComponentId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventComponentId : "",
          vaadinTextFieldEventType: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventType : "",
          vaadinTextFieldEventValue: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventValue : "",
          vaadinTextFieldStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldStatusText : "",
          vaadinTextFieldPostEventState: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldPostEventState : "",
          vaadinTextFieldFailureReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.vaadinTextFieldFailureReason : "",
          flowClientEngineResourceLoaded: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceLoaded),
          flowClientEngineResourcePath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineResourcePath : "",
          flowClientEngineResourceBytes: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceBytes : 0,
          flowClientEngineResourceVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceVerified),
          flowClientEngineModuleLoaded: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineModuleLoaded),
          flowClientEngineInitExported: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineInitExported),
          flowClientEngineInitialized: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineInitialized),
          flowClientEngineClientRegistered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered),
          flowClientEngineClientKey: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineClientKey : "",
          flowClientEngineClientKeys: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineClientKeys : "",
          flowClientEngineViewportReady: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineViewportReady),
          flowClientEngineViewportRootTag: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineViewportRootTag : "",
          flowClientEngineViewportRootId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineViewportRootId : "",
          flowClientEnginePromotionGate: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEnginePromotionGate : "",
          flowClientEngineBootstrapDomBound: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapDomBound),
          flowClientEngineBoundNodeIds: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineBoundNodeIds : "",
          flowClientEngineBootstrapStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapStatusText : "",
          flowClientEngineBootstrapButtonText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapButtonText : "",
          flowClientEngineListBound: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineListBound),
          flowClientEngineListItemCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineListItemCount : 0,
          flowClientEngineListText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineListText : "",
          flowIpcServerSequenceObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowIpcServerSequenceObserved),
          flowIpcServerSequenceMonotonic: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowIpcServerSequenceMonotonic),
          flowIpcServerSequenceCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowIpcServerSequenceCount : 0,
          flowIpcServerSequences: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowIpcServerSequences : "",
          flowClientEngineXhrBridgeInstalled: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled),
          flowClientEngineXhrRequestObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrRequestObserved),
          flowClientEngineXhrResponseObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrResponseObserved),
          flowClientEngineXhrLastClientId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrLastClientId : -1,
          flowClientEngineIpcMessageBridgeInstalled: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineIpcMessageBridgeInstalled),
          flowClientEngineIpcResyncBridgeInstalled: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineIpcResyncBridgeInstalled),
          flowClientEngineEventPatchConsumed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed),
          flowClientEngineEventStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineEventStatusText : "",
          flowClientEngineTextInputEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputEventObserved),
          flowClientEngineTextInputStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputStatusText : "",
          flowClientEngineCheckboxEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved),
          flowClientEngineCheckboxStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxStatusText : "",
          flowClientEngineComponentEventPatchCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventPatchCount : 0,
          flowClientEngineComponentEventOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventOrder : "",
          fileTransferDownloadResultObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.fileTransferDownloadResultObserved),
          fileTransferDownloadMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferDownloadMode : "",
          fileTransferDownloadPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferDownloadPath : "",
          fileTransferDownloadByteCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferDownloadByteCount : 0,
          fileTransferDownloadBodyRedacted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.fileTransferDownloadBodyRedacted),
          fileTransferUploadResultObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.fileTransferUploadResultObserved),
          fileTransferUploadMode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferUploadMode : "",
          fileTransferUploadPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferUploadPath : "",
          fileTransferUploadByteCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferUploadByteCount : 0,
          fileTransferUploadSampledBytes: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferUploadSampledBytes : 0,
          fileTransferUploadBodyRedacted: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.fileTransferUploadBodyRedacted),
          fileTransferResultOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.fileTransferResultOrder : "",
          flowClientEnginePushPatchConsumed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEnginePushPatchConsumed),
          flowClientEnginePushStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEnginePushStatusText : "",
          flowClientEnginePushOrderStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEnginePushOrderStatusText : "",
          flowClientEngineQueuedMessageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageCount : -1,
          flowClientEngineQueuedMessageHighWater: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageHighWater : 0,
          flowClientEngineQueuedPushCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedPushCount : 0,
          flowClientEngineQueuedMessageOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageOrder : "",
          flowClientEngineDeliveredQueuedMessageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredQueuedMessageCount : 0,
          flowClientEngineDeliveredQueuedMessageOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredQueuedMessageOrder : "",
          flowClientEngineDeliveredMessageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredMessageCount : 0,
          flowClientEngineDeliveredPushCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredPushCount : 0,
          flowClientEngineDeliveredMessageOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredMessageOrder : "",
          flowClientEngineIgnoredMessageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineIgnoredMessageCount : 0,
          flowClientEngineIgnoredMessageOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineIgnoredMessageOrder : "",
          flowClientEngineDuplicateLateIgnored: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineDuplicateLateIgnored),
          flowClientEngineOrderingLastDeliveredServerSequence: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineOrderingLastDeliveredServerSequence : 0,
          flowClientEngineRecoveryResyncRequested: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineRecoveryResyncRequested),
          flowClientEngineRecoveryResyncReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineRecoveryResyncReason : "",
          flowClientEngineResyncRequested: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncRequested),
          flowClientEngineResyncPatchConsumed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed),
          flowClientEngineErrorReported: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineErrorReported),
          flowClientEngineErrorCode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineErrorCode : "",
          flowClientEngineFirstMeaningfulPaintReported: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineFirstMeaningfulPaintReported),
          flowClientEngineVisibleOwner: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner),
          flowClientEngineVisibleRootId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleRootId : "",
          flowClientEngineVisibleStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleStatusText : "",
          flowClientEngineVisibleButtonText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleButtonText : "",
          flowClientEngineLocalFallbackHidden: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineLocalFallbackHidden),
          flowClientEngineXhrError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError : "",
          flowClientEngineInitError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError : "",
          flowClientEngineResourceExecutionError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceExecutionError : "",
          frameworkAppRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppRendered),
          frameworkAppRootId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppRootId : "",
          frameworkAppBudgetRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetRendered),
          frameworkAppCheckboxRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxRendered),
          frameworkAppSelectRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppSelectRendered),
          frameworkAppApplyRendered: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppApplyRendered),
          frameworkAppStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppStatusText : "",
          frameworkAppComponentStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppComponentStatusText : "",
          frameworkAppUpdatesText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppUpdatesText : "",
          frameworkAppBudgetEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventRpcObserved),
          frameworkAppBudgetEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventObserved),
          frameworkAppCheckboxEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventRpcObserved),
          frameworkAppCheckboxEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventObserved),
          frameworkAppSelectEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventRpcObserved),
          frameworkAppSelectEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventObserved),
          frameworkAppApplyEventRpcObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventRpcObserved),
          frameworkAppApplyEventObserved: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventObserved),
          frameworkAppEventPatchCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppEventPatchCount : 0,
          frameworkAppEventOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppEventOrder : "",
          frameworkAppInteractionComplete: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frameworkAppInteractionComplete),
          frameworkAppFailureReason: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frameworkAppFailureReason : ""
        })
        """
        webView.evaluateJavaScript(script) { result, error in
            if let error {
                trace("send", "error", [
                    "code": "webviewInspectFailed",
                    "message": error.localizedDescription
                ])
                return
            }
            trace("recv", "webview.dom", [
                "label": "main",
                "result": result as? String ?? String(describing: result)
            ])
        }
    }

    private func writeSnapshot(_ webView: WKWebView, completion: (() -> Void)? = nil) {
        let snapshotConfig = WKSnapshotConfiguration()
        snapshotConfig.rect = webView.bounds
        webView.takeSnapshot(with: snapshotConfig) { image, error in
            if let error {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": error.localizedDescription
                ])
                completion?()
                return
            }

            let snapshotPath = "\(self.config.repoRoot)/.build/vaadin-desktop-host-swift/window-snapshot.png"
            guard let image,
                  let tiffData = image.tiffRepresentation,
                  let bitmap = NSBitmapImageRep(data: tiffData),
                  let pngData = bitmap.representation(using: .png, properties: [:]) else {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": "WKWebView snapshot produced no PNG data"
                ])
                completion?()
                return
            }

            do {
                try FileManager.default.createDirectory(
                    at: URL(fileURLWithPath: snapshotPath).deletingLastPathComponent(),
                    withIntermediateDirectories: true)
                try pngData.write(to: URL(fileURLWithPath: snapshotPath))
                trace("recv", "webview.snapshot", [
                    "label": "main",
                    "path": snapshotPath,
                    "width": Int(image.size.width),
                    "height": Int(image.size.height)
                ])
                completion?()
            } catch {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": error.localizedDescription
                ])
                completion?()
            }
        }
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        trace("send", "error", [
            "code": "navigationFailed",
            "message": error.localizedDescription
        ])
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        trace("send", "error", [
            "code": "navigationFailed",
            "message": error.localizedDescription
        ])
    }
}

let flowIpcShell = #"""
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Vaadin Flow over IPC</title>
  <style>
    :root {
      color-scheme: light;
      font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", system-ui, sans-serif;
      background: #ffffff;
      color: #111827;
    }
    html,
    body {
      height: 100%;
    }
    body {
      margin: 0;
      min-height: 100%;
      overflow: hidden;
      background: #ffffff;
      color: #111827;
    }
    #app,
    #flow-client-engine-root {
      box-sizing: border-box;
      position: fixed;
      inset: 0;
      width: auto;
      height: auto;
      min-height: 0;
      overflow: auto;
      overscroll-behavior: contain;
      background: #ffffff;
      color: #111827;
    }
    #app {
      padding: 28px;
    }
    #app[hidden],
    #flow-client-engine-root[hidden] {
      display: none !important;
    }
    #flow-client-engine-root {
      display: block;
      padding: 0;
      overflow: hidden;
    }
    .vatori-flow-client-container {
      display: block;
      position: fixed;
      inset: 0;
      box-sizing: border-box;
      padding: 0;
      min-height: 0;
      max-width: 100%;
      overflow: auto;
      background: #ffffff;
      color: #111827;
    }
    #app *,
    #flow-client-engine-root *,
    .vatori-flow-client-container * {
      color: inherit;
    }
    #app vaadin-vertical-layout,
    #flow-client-engine-root vaadin-vertical-layout {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
      max-width: 100%;
    }
    .vatori-flow-viewport-root {
      position: fixed;
      inset: 28px;
      box-sizing: border-box;
      z-index: 10;
      display: flex !important;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
      min-height: 0;
      max-width: 100%;
      overflow: auto;
      background: #ffffff;
      color: #111827;
    }
    .vatori-flow-viewport-root > vaadin-vertical-layout,
    .vatori-flow-viewport-root vaadin-vertical-layout {
      display: flex !important;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
      max-width: 100%;
    }
    #app vaadin-button,
    #flow-client-engine-root vaadin-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: fit-content;
      min-height: 32px;
      padding: 4px 14px;
      border: 1px solid #c7ccd3;
      border-radius: 6px;
      background: #f3f4f6;
      color: #111827;
    }
    #app input,
    #flow-client-engine-root input,
    #app vaadin-big-decimal-field,
    #flow-client-engine-root vaadin-big-decimal-field,
    #app vaadin-text-field,
    #flow-client-engine-root vaadin-text-field,
    #app vaadin-checkbox,
    #flow-client-engine-root vaadin-checkbox {
      display: inline-block;
      max-width: 100%;
    }
    .flow-layout {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }
    h1 {
      margin: 0 0 6px;
      font-size: 28px;
      font-weight: 700;
      letter-spacing: 0;
    }
    button {
      appearance: auto;
      font: inherit;
      min-height: 32px;
      padding: 4px 14px;
    }
    input {
      font: inherit;
      min-height: 28px;
      color: #111827;
      background: #ffffff;
    }
    input[type="text"] {
      min-width: 240px;
      padding: 3px 8px;
    }
    .flow-list {
      display: grid;
      grid-template-columns: 120px 160px 1fr;
      gap: 1px;
      margin-top: 10px;
      border: 1px solid #d1d5db;
      min-width: min(640px, 100%);
      background: #d1d5db;
    }
    .flow-list span {
      padding: 8px 10px;
      background: #ffffff;
    }
    #trace {
      position: fixed;
      left: -10000px;
      bottom: 0;
      width: 1px;
      height: 1px;
      margin: 0;
      overflow: hidden;
      pointer-events: none;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
      color: #4b5563;
    }
  </style>
</head>
<body>
  <main id="app"></main>
  <section id="flow-client-engine-root" hidden></section>
  <div id="trace"></div>
  <script>
    (function () {
      const app = document.getElementById('app');
      const trace = document.getElementById('trace');
      let sequence = 0;
      let autoClickDone = false;
      let autoClickAttempts = 0;
      let autoClickReadinessAttempts = 0;
      let autoClickInFlight = false;
      let autoComponentEventValue = 'Flow IPC typed';
      let autoVaadinWebComponentValue = '42.5';
      let autoVaadinTextFieldValue = 'Flow IPC typed';
      let autoVaadinCheckboxValue = true;
      let autoComponentEventRetryLimit = 30;
      let autoComponentEventDelayMs = 140;
      let autoComponentEventRetryMs = 80;
      let autoComponentEventTimeoutMs = 500;
      let autoComponentEventEnabled =
        !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__
        || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__;
      let autoComponentEvents = [
        {
          key: 'text',
          id: 'ipc-text-input',
          eventType: 'change',
          value: autoComponentEventValue,
          data: () => ({ 'element.value': autoComponentEventValue }),
          prepare: (element) => { element.value = autoComponentEventValue; }
        },
        {
          key: 'checkbox',
          id: 'ipc-checkbox',
          eventType: 'change',
          value: true,
          data: () => ({ 'element.checked': true }),
          prepare: (element) => { element.checked = true; }
        },
        {
          key: 'vaadin-big-decimal',
          id: 'ipc-vaadin-big-decimal',
          eventType: 'change',
          value: autoVaadinWebComponentValue,
          data: () => ({ 'element.value': autoVaadinWebComponentValue }),
          requiresComponentResource: true,
          requiredTag: 'vaadin-big-decimal-field',
          dispatchDomEvent: true,
          prepare: (element) => {
            element.value = autoVaadinWebComponentValue;
            if (element.inputElement) {
              element.inputElement.value = autoVaadinWebComponentValue;
            }
          }
        },
        {
          key: 'vaadin-text-field',
          id: 'ipc-vaadin-text-field',
          eventType: 'change',
          value: autoVaadinTextFieldValue,
          data: () => ({ 'element.value': autoVaadinTextFieldValue }),
          requiresComponentResource: true,
          requiredTag: 'vaadin-text-field',
          dispatchDomEvent: true,
          prepare: (element) => {
            element.value = autoVaadinTextFieldValue;
            if (element.inputElement) {
              element.inputElement.value = autoVaadinTextFieldValue;
            }
          }
        },
        {
          key: 'vaadin-checkbox',
          id: 'ipc-vaadin-checkbox',
          eventType: 'change',
          value: autoVaadinCheckboxValue,
          data: () => ({ 'element.checked': autoVaadinCheckboxValue }),
          requiresComponentResource: true,
          requiredTag: 'vaadin-checkbox',
          dispatchDomEvent: true,
          prepare: (element) => {
            element.checked = autoVaadinCheckboxValue;
            if (element.inputElement) {
              element.inputElement.checked = autoVaadinCheckboxValue;
            }
          }
        }
      ];
      let autoComponentEventIndex = 0;
      let autoComponentEventAttempts = 0;
      let autoComponentEventInFlight = false;
      let autoComponentEventsDone = false;
      const autoFrameworkBudgetValue = '73.25';
      const autoFrameworkPriorityValue = 'audit';
      let autoFrameworkUpdateStep = 0;
      let autoFrameworkUpdateAttempts = 0;
      let autoFrameworkUpdateInFlight = false;
      let autoFrameworkUpdateDone = false;
      let autoFrameworkUpdateInterval = null;
      let autoResyncDone = false;
      let autoErrorProbeDone = false;
      const flowBootstrapAppId = 'ROOT-2521314';
      const flowBootstrapClientId = flowBootstrapAppId.replace(/-\d+$/, '');
      let flowClientResources = [];
      let componentFrontendResources = [];
      let flowClientBootstrapInitResponse = null;
      let flowClientEngineResourceBaseVerified = false;
      let flowClientEngineBridgeClient = null;
      let flowClientEngineBridgeClientKey = '';
      let originalXMLHttpRequest = null;
      let flowClientResourceGraphMetadataVerified = true;
      let deferredComponentFrontendResourceGraphRequested = false;
      let componentFrontendResourceGraphMetadataVerified = true;
      const flowClientResourceRequests = new Set();
      const flowClientResourceResponsePaths = new Set();
      const flowClientResourceExpectedSha256ByPath = new Map();
      const flowClientResourceVerifiedPaths = new Set();
      const flowClientResourceExecutedPaths = new Set();
      const flowClientResourceVerifiedOnlyPaths = new Set();
      const flowClientResourceIntentionallyNotExecutedPaths = new Set();
      const flowClientResourceExecutionFailedPaths = new Set();
      const componentFrontendResourceRequests = new Set();
      const componentFrontendResourceResponsePaths = new Set();
      const componentFrontendResourceExpectedSha256ByPath = new Map();
      const componentFrontendResourceBodies = new Map();
      const componentFrontendResourceVerifiedPaths = new Set();
      const componentFrontendResourceExecutedPaths = new Set();
      const componentFrontendResourceExecutedTags = new Set();
      const componentFrontendResourceExecutionFailedPaths = new Set();
      const frontendStagedResourcePaths = new Set();
      const frontendStagedResourceFamilies = new Set();
      const frontendStagedResourceUrls = new Map();
      const frontendModuleIdentityUrlCache = new Map();
      const frontendProductionOwnerUrlCache = new Map();
      const frontendProductionOwnerImportUrlCache = new Map();
      const frontendModuleIdentityResourcePaths = new Set();
      const frontendStagedResourceCacheHitPaths = new Set();
      const frontendStagedResourceCacheMissPaths = new Set();
      const frontendStagedResourceExecutedPaths = new Set();
      const frontendStagedResourceFailedPaths = new Set();
      const frontendStageResourceByUrl = new Map();
      const frontendHostResourceLoaderPaths = new Set();
      const frontendHostResourceLoaderExecutedPaths = new Set();
      const frontendHostResourceLoaderFallbackPaths = new Set();
      const hostResourceStagePromises = new Map();
      let hostResourceStageSequence = 0;
      let generatedLazyBundleExecutionAttempted = false;
      const generatedLoadOnDemandChunkPromises = new Map();
      let flowLazyDependencyExecutionAttempted = false;
      let productionBundleRuntimeStageAttempted = false;
      let productionBundleRuntimeOwnerImportAttempted = false;
      var productionBundleRuntimeOwnerOrderingPreflightRecorded = false;
      let textFieldDevBundleStageAttempted = false;
      const flowLazyDependencyPendingElements = new Map();
      const pendingFlowClientXhrRequests = new Map();
      const queuedFlowClientEngineMessages = [];
      const queuedFlowClientEngineMessageOrder = [];
      const deliveredQueuedFlowClientEngineMessageOrder = [];
      const deliveredFlowClientEngineMessageOrder = [];
      const ignoredFlowClientEngineMessageOrder = [];
      const flowIpcServerSequences = [];
      let flowClientEngineResyncXhrSent = false;
      let flowClientLazyLookupRequested = false;
      let lastFlowIpcServerSequence = 0;
      let flowClientEngineLastDeliveredServerSequence = 0;
      let flowClientEnginePromotionRetryActive = false;
      let flowClientEnginePromotionRetryAttempts = 0;
      let pendingFlowClientEngineRecoveryResyncReason = '';
      const flowClientState = {
        csrfToken: '',
        securityKey: '',
        securityTokenId: 'Vaadin-Security-Key',
        syncId: -1,
        clientId: 0
      };
      const flowNodeElements = new Map();
      const flowTextNodes = new Map();
      const flowUidlNodes = new Map();
      const flowClientNodeIdsById = new Map();
      const flowClientIdsByNodeId = new Map();
      window.VaadinDesktopIpcDiagnostics = {
        flowClientResourceLoaded: false,
        flowClientResourcePath: '',
        flowClientResourceBytes: 0,
        flowClientResourceSha256: '',
        flowClientResourceExpectedSha256: '',
        flowClientResourceVerified: false,
        flowClientResourceModuleLoaded: false,
        flowClientBootstrapExported: false,
        flowClientBootstrapInitialized: false,
        flowClientBootstrapAppId: '',
        flowClientResourceManifestCount: 0,
        flowClientResourceGraphRequestedCount: 0,
        flowClientResourceGraphResponseCount: 0,
        flowClientResourceGraphPaths: '',
        flowClientResourceGraphResponsePaths: '',
        flowClientResourceGraphComplete: false,
        flowClientResourceGraphVerified: false,
        flowClientBootstrapInitError: '',
        flowClientResourceExecuted: false,
        flowClientResourceVerifiedCount: 0,
        flowClientResourceVerifiedPaths: '',
        flowClientResourceExecutedCount: 0,
        flowClientResourceExecutedPaths: '',
        flowClientResourceVerifiedOnlyCount: 0,
        flowClientResourceVerifiedOnlyPaths: '',
        flowClientResourceIntentionallyNotExecutedCount: 0,
        flowClientResourceIntentionallyNotExecutedPaths: '',
        flowClientResourceExecutionFailedCount: 0,
        flowClientResourceExecutionFailedPaths: '',
        flowClientResourceExecutionClassificationComplete: false,
        flowClientResourceLazyLookupRequested: false,
        flowClientResourceLazyLookupResponse: false,
        flowClientResourceLazyLookupVerified: false,
        flowClientResourceLazyLookupPurpose: '',
        flowClientResourceLazyLookupKind: '',
        flowClientResourceLazyLookupPath: '',
        flowClientResourceLazyLookupResult: '',
        flowClientResourceLoadOnDemandProbeResult: '',
        flowClientResourceBootstrapLazyEntriesResult: '',
        flowClientResourceStaticLookupVerified: false,
        flowClientResourceStaticLookupPath: '',
        flowClientResourceThemeLookupResult: '',
        frontendAssetPipelineVersion: '',
        frontendArtifactGraphSource: '',
        frontendArtifactGraphManifestObserved: false,
        frontendArtifactFamilies: '',
        frontendArtifactFamilyCount: 0,
        frontendArtifactGraphDependencyObserved: false,
        frontendArtifactGraphRequestedBy: '',
        frontendArtifactGraphDependencyOf: '',
        frontendArtifactGraphModuleSpecifiers: '',
        frontendArtifactExecutionPolicies: '',
        frontendArtifactStatuses: '',
        frontendArtifactStrictBlockers: '',
        frontendArtifactStrictBlockerCount: 0,
        frontendArtifactShimFallback: false,
        frontendArtifactProductionBundlePartial: false,
        frontendArtifactProductionBundleStatsVerified: false,
        frontendArtifactProductionBundleStatsPath: '',
        frontendArtifactProductionBundleStatsResult: '',
        frontendArtifactThemeStaticVerified: false,
        frontendArtifactThemeStaticPath: '',
        frontendArtifactThemeStaticBlocker: false,
        frontendArtifactGeneratedLazyVerified: false,
        frontendArtifactGeneratedLazyPath: '',
        frontendArtifactGeneratedLazyBundleChunkVerified: false,
        frontendArtifactGeneratedLazyBundleChunkPath: '',
        frontendArtifactGeneratedLazyBundleChunkResult: '',
        frontendArtifactGeneratedLazyExecutionAttempted: false,
        frontendArtifactGeneratedLazyExecuted: false,
        frontendArtifactGeneratedLazyExecutionResult: '',
        frontendArtifactGeneratedLazyExecutionError: '',
        frontendArtifactGeneratedLazyLoadOnDemandResult: '',
        frontendArtifactGeneratedLazyLoadOnDemandContractVersion: '',
        frontendArtifactGeneratedLazyTriggerSource: '',
        frontendArtifactGeneratedLazyApiOwner: '',
        frontendArtifactGeneratedLazyApiInstallSource: '',
        frontendArtifactGeneratedLazyRequestedChunk: '',
        frontendArtifactGeneratedLazyRegistrySource: '',
        frontendArtifactGeneratedLazyMappingSource: '',
        frontendArtifactGeneratedLazyResolvedArtifactPath: '',
        frontendArtifactGeneratedLazyFlowClientTriggered: false,
        frontendArtifactGeneratedLazyDiagnosticBridge: false,
        frontendArtifactGeneratedLazyLoadOnDemandBlocker: false,
        frontendArtifactGeneratedLazyResolverVersion: '',
        frontendArtifactGeneratedLazyExecutionVersion: '',
        frontendArtifactGeneratedLazyImportResolutionStrategy: '',
        frontendArtifactGeneratedLazyRelativeImportSpecifier: '',
        frontendArtifactGeneratedLazyResolvedDependencyPath: '',
        frontendArtifactGeneratedLazyRelativeDependencyResolved: false,
        frontendArtifactGeneratedLazyFailureLayer: '',
        frontendArtifactGeneratedLazyBlockerVersion: '',
        frontendArtifactGeneratedLazyNextResolutionStep: '',
        frontendArtifactGeneratedLazyModuleIdentityStrategy: '',
        frontendArtifactGeneratedLazyModuleIdentityKey: '',
        frontendArtifactGeneratedLazyStagedDependencyGraph: '',
        generatedLazyChunkUidlExpressionObserved: false,
        generatedLazyChunkExpressionSource: '',
        generatedLazyChunkExpression: '',
        generatedLazyChunkId: '',
        generatedLazyChunkIds: '',
        generatedLazyChunkTriggerOwner: '',
        generatedLazyChunkTriggeredByFlowClient: false,
        generatedLazyChunkDiagnosticBridgeUsed: false,
        generatedLazyChunkDirectImportProbeUsed: false,
        generatedLazyChunkResolvedArtifactPath: '',
        generatedLazyChunkFailureLayer: '',
        generatedLazyChunkBlockerVersion: '',
        generatedLazyChunkNextResolutionStep: '',
        frontendRuntimeResolverContractVersion: '',
        frontendRuntimeResolverLogicalRequestIds: '',
        frontendRuntimeResolverPathMappings: '',
        loadOnDemandChunkContractVersion: '',
        loadOnDemandChunkApiOwner: '',
        loadOnDemandChunkApiInstallSource: '',
        loadOnDemandChunkRegistrySource: '',
        loadOnDemandChunkMappingSource: '',
        loadOnDemandChunkRequestedChunk: '',
        loadOnDemandChunkResolvedArtifactPath: '',
        loadOnDemandChunkModuleIdentity: '',
        loadOnDemandChunkTriggeredByFlowClient: false,
        loadOnDemandChunkStageObserved: false,
        loadOnDemandChunkExecuted: false,
        loadOnDemandChunkExecutionResult: '',
        loadOnDemandChunkFailureLayer: '',
        loadOnDemandChunkBrowserError: '',
        loadOnDemandChunkBlockerVersion: '',
        loadOnDemandChunkNextResolutionStep: '',
        generatedLoadOnDemandChunkFileRegistryVersion: '',
        generatedLoadOnDemandChunkFileRegistrySource: '',
        generatedLoadOnDemandChunkFileMappingSource: '',
        generatedLoadOnDemandGeneratedImportsArtifact: '',
        generatedLoadOnDemandSourceBuildMode: '',
        generatedLoadOnDemandSourceArtifactFamily: '',
        generatedLoadOnDemandNoOpLoadOnDemand: false,
        loadOnDemandTableShape: '',
        chunkImportSpecifier: '',
        chunkArtifactPath: '',
        chunkFileResolver: '',
        chunkFileStatus: '',
        chunkFileBytes: 0,
        chunkFileSha256: '',
        noChunkGeneratedReason: '',
        loadOnDemandChunkFileTriggeredByFlowClient: false,
        loadOnDemandChunkFileStageObserved: false,
        loadOnDemandChunkFileExecuted: false,
        loadOnDemandChunkFileCallbackResolved: false,
        loadOnDemandChunkFileExecutionResult: '',
        loadOnDemandChunkFileSideEffectProof: '',
        loadOnDemandChunkFileFailureLayer: '',
        loadOnDemandChunkFileBrowserError: '',
        loadOnDemandChunkFileBlockerVersion: '',
        loadOnDemandChunkFileNextResolutionStep: '',
        flowLazyDependencyObserved: false,
        flowLazyDependencyRequested: false,
        flowLazyDependencyResponse: false,
        flowLazyDependencyStageObserved: false,
        flowLazyDependencyExecuted: false,
        flowLazyDependencyVersion: '',
        flowLazyDependencySource: '',
        flowLazyDependencyMode: '',
        flowLazyDependencyExpression: '',
        flowLazyDependencyChunkId: '',
        flowLazyDependencyResolvedPath: '',
        flowLazyDependencyTriggerOwner: '',
        flowLazyDependencyTriggeredByFlowClient: false,
        flowLazyDependencyExecutionResult: '',
        flowLazyDependencyFailureLayer: '',
        flowLazyDependencyBrowserError: '',
        frontendArtifactProductionBundleRuntimeObserved: false,
        frontendArtifactProductionBundleRuntimeStageAttempted: false,
        frontendArtifactProductionBundleRuntimeExecuted: false,
        frontendArtifactProductionBundleRuntimeBlocker: false,
        frontendArtifactProductionBundleRuntimeVersion: '',
        frontendArtifactProductionBundleRuntimeRegistrySource: '',
        frontendArtifactProductionBundleRuntimeStatsSource: '',
        frontendArtifactProductionBundleRuntimeSelectedArtifact: '',
        frontendArtifactProductionBundleRuntimeDependencyChain: '',
        frontendArtifactProductionBundleRuntimeModuleIdentity: '',
        frontendArtifactProductionBundleRuntimeExecutionResult: '',
        frontendArtifactProductionBundleRuntimeFailureLayer: '',
        frontendArtifactProductionBundleRuntimeBrowserError: '',
        frontendArtifactProductionBundleRuntimeNextResolutionStep: '',
        frontendArtifactProductionBundleRuntimeOwnerObserved: false,
        frontendArtifactProductionBundleRuntimeOwnerStageObserved: false,
        frontendArtifactProductionBundleRuntimeOwnerImportAttempted: false,
        frontendArtifactProductionBundleRuntimeOwnerImported: false,
        frontendArtifactProductionBundleRuntimeOwnerBlocker: false,
        frontendArtifactProductionBundleRuntimeOwnerVersion: '',
        frontendArtifactProductionBundleRuntimeOwner: '',
        frontendArtifactProductionBundleRuntimeOwnerEntryArtifact: '',
        frontendArtifactProductionBundleRuntimeOwnerSource: '',
        frontendArtifactProductionBundleRuntimeOwnerSelectedBy: '',
        frontendArtifactProductionBundleRuntimeOwnerModuleIdentity: '',
        frontendArtifactProductionBundleRuntimeOwnerLifecycle: '',
        frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy: '',
        frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy: '',
        frontendArtifactProductionBundleRuntimeOwnerCachePolicy: '',
        frontendArtifactProductionBundleRuntimeOwnerCacheStatus: '',
        frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy: '',
        frontendArtifactProductionBundleRuntimeOwnerExecutionResult: '',
        frontendArtifactProductionBundleRuntimeOwnerSideEffectProof: '',
        frontendArtifactProductionBundleRuntimeOwnerFailureLayer: '',
        frontendArtifactProductionBundleRuntimeOwnerBrowserError: '',
        frontendArtifactProductionBundleRuntimeOwnerBlockerVersion: '',
        frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep: '',
        frontendArtifactProductionBundleRuntimeOwnerRegistryBefore: '',
        frontendArtifactProductionBundleRuntimeOwnerRegistryAfter: '',
        frontendArtifactProductionBundleRuntimeOwnerDuplicateTag: '',
        frontendArtifactProductionBundleRuntimeOwnerFirstDefinitionOwner: '',
        frontendArtifactProductionBundleRuntimeOwnerSecondDefinitionOwner: '',
        frontendArtifactProductionBundleRuntimeOwnerImportVersion: '',
        frontendArtifactProductionBundleRuntimeOwnerImportPolicy: '',
        frontendArtifactProductionBundleRuntimeOwnerImportArtifact: '',
        frontendArtifactProductionBundleRuntimeOwnerImportPhase: '',
        frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates: '',
        frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes: '',
        frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects: '',
        frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures: '',
        frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy: '',
        frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths: '',
        frontendArtifactProductionBundleRuntimeOwnerImportResolvedDependencyPaths: '',
        frontendArtifactProductionBundleRuntimeOwnerImportUnresolvedSpecifiers: '',
        frontendArtifactProductionBundleRuntimeOwnerImportDependencyRewriteResult: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact: '',
        frontendArtifactProductionBundleRuntimeOwnerDependencyGraphClosed: false,
        frontendArtifactProductionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount: 0,
        frontendArtifactProductionBundleRuntimeOwnerDependencyGraphResolvedCount: 0,
        frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMissingCount: 0,
        frontendArtifactProductionBundleRuntimeOwnerDependencyGraphStagedCount: 0,
        frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMaxDepth: 0,
        frontendArtifactProductionBundleRuntimeOwnerDependencyClassifications: '',
        frontendArtifactProductionBundleRuntimeOwnerImportMissingDependencyPaths: '',
        frontendArtifactProductionBundleRuntimeOwnerImportMissingSpecifiers: '',
        frontendArtifactProductionBundleRuntimeOwnerImportStageCount: 0,
        frontendArtifactProductionBundleRuntimeOwnerImportModuleIdentity: '',
        frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalBefore: '',
        frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalAfter: '',
        frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep: '',
        frontendArtifactProductionBundleRuntimeOwnerImportBrowserErrorName: '',
        frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion: '',
        frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy: '',
        frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult: '',
        frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases: '',
        frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures: '',
        componentProductionOwnerImportOrder: '',
        componentProductionOwnerReuseVersion: '',
        componentProductionOwnerReusePolicy: '',
        componentProductionOwnerReuseMode: '',
        componentProductionOwnerReuseAcceptableFailures: '',
        componentProductionOwnerReused: false,
        componentProductionOwnerReuseReason: '',
        componentProductionOwnerOwnedTags: '',
        componentProductionOwnerSkippedDefinitions: '',
        componentProductionOwnerFallbackImports: '',
        componentProductionOwnerDependencyPaths: '',
        frontendArtifactDevModeVerified: false,
        frontendArtifactDevModePath: '',
        frontendArtifactDevModePackageGraphResult: '',
        frontendArtifactDevModeBlocker: false,
        frontendArtifactGeneratedLazyBlocker: false,
        frontendStagingResolverVersion: '',
        frontendStagingObserved: false,
        frontendStagedResourceCount: 0,
        frontendStagedResourcePaths: '',
        frontendStagedResourceFamilies: '',
        frontendStagedResourceSchemes: '',
        frontendStagedResourceCacheHitCount: 0,
        frontendStagedResourceCacheMissCount: 0,
        frontendStagedResourceExecutedCount: 0,
        frontendStagedResourceExecutedPaths: '',
        frontendStagedResourceFailedCount: 0,
        frontendStagedResourceFailedPaths: '',
        frontendResourceSchemeVersion: '',
        frontendResourceLoaderMode: '',
        frontendResourceSchemeName: '',
        frontendResourceSchemeHandlerInstalled: false,
        frontendResourceSchemeRequestObserved: false,
        frontendResourceSchemeResponseObserved: false,
        frontendResourceLoaderNoLocalhost: false,
        frontendResourceFileBacked: false,
        frontendResourceLoaderObserved: false,
        frontendResourceLoaderObservedPaths: '',
        frontendResourceLoaderExecuted: false,
        frontendResourceLoaderExecutedPaths: '',
        frontendResourceBlobFallbackObserved: false,
        frontendResourceBlobFallbackPaths: '',
        frontendResourceMimeType: '',
        frontendResourceFilePathRedacted: '',
        frontendResourceFileDirectoryRedacted: '',
        frontendResourceBaseUrl: '',
        frontendResourceRelativeImportBase: '',
        frontendResourceRelativeImportResolutionMode: '',
        frontendResourceResolvedRelativeImportPaths: '',
        frontendResourceMissingRelativeImportPaths: '',
        frontendResourceNoLocalhostProof: '',
        frontendArtifactTextFieldStagedBlocker: false,
        frontendArtifactTextFieldStagedBlockerPath: '',
        frontendArtifactTextFieldStagedBlockerReason: '',
        frontendArtifactTextFieldStagedBlockerError: '',
        frontendArtifactTextFieldStagedBlockerDependency: '',
        frontendArtifactTextFieldGraphParityVersion: '',
        frontendArtifactTextFieldBlockerVersion: '',
        frontendArtifactTextFieldGraphProbe: '',
        frontendArtifactTextFieldEntryArtifact: '',
        frontendArtifactTextFieldTransitiveArtifacts: '',
        frontendArtifactTextFieldImportResolutionStrategy: '',
        frontendArtifactTextFieldSideEffectVersion: '',
        frontendArtifactTextFieldSideEffectStrategy: '',
        frontendArtifactTextFieldExecutionVersion: '',
        frontendArtifactTextFieldExecuted: false,
        frontendArtifactTextFieldShimUsed: false,
        frontendArtifactTextFieldClassExportArtifact: '',
        frontendArtifactTextFieldFirstDefinitionSource: '',
        frontendArtifactTextFieldSecondDefinitionSource: '',
        frontendArtifactTextFieldDuplicateTag: '',
        frontendArtifactTextFieldRemainingBlockerClassification: '',
        frontendArtifactTextFieldModuleIdentityStrategy: '',
        frontendArtifactTextFieldModuleIdentityPaths: '',
        frontendArtifactTextFieldNarrowedFailureClassification: '',
        frontendArtifactTextFieldFailureLayer: '',
        frontendArtifactTextFieldMinimalReproduction: '',
        frontendArtifactTextFieldNextResolutionStep: '',
        frontendArtifactTextFieldCustomElementRegistryBefore: '',
        frontendArtifactTextFieldCustomElementRegistryAfter: '',
        flowClientResourceExecutionError: '',
        vaadinWebComponentResourceManifestObserved: false,
        vaadinWebComponentResourceManifestCount: 0,
        vaadinWebComponentResourceManifestPrimaryPath: '',
        vaadinWebComponentResourceManifestBlockedReason: '',
        vaadinWebComponentResourceManifestFailureClassification: '',
        vaadinWebComponentResourceResolverStrategy: '',
        vaadinWebComponentResourceRequested: false,
        vaadinWebComponentResourceRequestedCount: 0,
        vaadinWebComponentResourceRequestedPaths: '',
        vaadinWebComponentResourceResponseCount: 0,
        vaadinWebComponentResourceResponsePaths: '',
        vaadinWebComponentResourceResolved: false,
        vaadinWebComponentResourceVerified: false,
        vaadinWebComponentResourceVerifiedCount: 0,
        vaadinWebComponentResourceVerifiedPaths: '',
        vaadinWebComponentResourceExecuted: false,
        vaadinWebComponentResourceExecutedPath: '',
        vaadinWebComponentResourceExecutedCount: 0,
        vaadinWebComponentResourceExecutedPaths: '',
        vaadinWebComponentResourceExecutedTags: '',
        vaadinWebComponentResourceExecutionFailed: false,
        vaadinWebComponentResourceExecutionFailedCount: 0,
        vaadinWebComponentResourceExecutionFailedPaths: '',
        vaadinWebComponentResourceGraphComplete: false,
        vaadinWebComponentResourceGraphVerified: false,
        vaadinWebComponentResourceBlockedReason: '',
        vaadinWebComponentResourceFailureClassification: '',
        vaadinWebComponentResourceShimmedImports: '',
        vaadinWebComponentResourceImportMode: '',
        vaadinWebComponentResourceExecutionError: '',
        vaadinWebComponentTagDefined: false,
        vaadinWebComponentRendered: false,
        vaadinWebComponentRenderedTag: '',
        vaadinWebComponentRenderedValue: '',
        vaadinWebComponentEventDispatchMode: '',
        vaadinWebComponentEventObserved: false,
        vaadinWebComponentEventRpcObserved: false,
        vaadinWebComponentEventComponentId: '',
        vaadinWebComponentEventType: '',
        vaadinWebComponentEventValue: '',
        vaadinWebComponentStatusText: '',
        vaadinWebComponentPostEventState: '',
        vaadinWebComponentFailureReason: '',
        vaadinSecondWebComponentTagDefined: false,
        vaadinSecondWebComponentRendered: false,
        vaadinSecondWebComponentRenderedTag: '',
        vaadinSecondWebComponentRenderedValue: '',
        vaadinSecondWebComponentEventDispatchMode: '',
        vaadinSecondWebComponentEventObserved: false,
        vaadinSecondWebComponentEventRpcObserved: false,
        vaadinSecondWebComponentEventComponentId: '',
        vaadinSecondWebComponentEventType: '',
        vaadinSecondWebComponentEventValue: '',
        vaadinSecondWebComponentStatusText: '',
        vaadinSecondWebComponentPostEventState: '',
        vaadinSecondWebComponentFailureReason: '',
        vaadinTextFieldRendered: false,
        vaadinTextFieldRenderedTag: '',
        vaadinTextFieldRenderedValue: '',
        vaadinTextFieldTagDefined: false,
        vaadinTextFieldEventDispatchMode: '',
        vaadinTextFieldEventObserved: false,
        vaadinTextFieldEventRpcObserved: false,
        vaadinTextFieldEventComponentId: '',
        vaadinTextFieldEventType: '',
        vaadinTextFieldEventValue: '',
        vaadinTextFieldStatusText: '',
        vaadinTextFieldPostEventState: '',
        vaadinTextFieldFailureReason: '',
        flowClientEngineResourceLoaded: false,
        flowClientEngineResourcePath: '',
        flowClientEngineResourceBytes: 0,
        flowClientEngineResourceSha256: '',
        flowClientEngineResourceExpectedSha256: '',
        flowClientEngineResourceVerified: false,
        flowClientEngineModuleLoaded: false,
        flowClientEngineInitExported: false,
        flowClientEngineInitialized: false,
        flowClientEngineClientRegistered: false,
        flowClientEngineClientKey: '',
        flowClientEngineClientKeys: '',
        flowClientEngineViewportReady: false,
        flowClientEngineViewportRootTag: '',
        flowClientEngineViewportRootId: '',
        flowClientEnginePromotionGate: '',
        flowClientEngineBootstrapDomBound: false,
        flowClientEngineBoundNodeIds: '',
        flowClientEngineBootstrapStatusText: '',
        flowClientEngineBootstrapButtonText: '',
        flowClientEngineListBound: false,
        flowClientEngineListItemCount: 0,
        flowClientEngineListText: '',
        flowIpcServerSequenceObserved: false,
        flowIpcServerSequenceMonotonic: true,
        flowIpcServerSequenceCount: 0,
        flowIpcServerSequences: '',
        flowClientEngineXhrBridgeInstalled: false,
        flowClientEngineXhrRequestObserved: false,
        flowClientEngineXhrResponseObserved: false,
        flowClientEngineXhrLastClientId: -1,
        flowClientEngineIpcMessageBridgeInstalled: false,
        flowClientEngineIpcResyncBridgeInstalled: false,
        flowClientEngineEventPatchConsumed: false,
        flowClientEngineEventStatusText: '',
        flowClientEngineTextInputEventObserved: false,
        flowClientEngineTextInputStatusText: '',
        flowClientEngineCheckboxEventObserved: false,
        flowClientEngineCheckboxStatusText: '',
        flowClientEngineComponentEventPatchCount: 0,
        flowClientEngineComponentEventOrder: '',
        fileTransferDownloadResultObserved: false,
        fileTransferDownloadMode: '',
        fileTransferDownloadPath: '',
        fileTransferDownloadByteCount: 0,
        fileTransferDownloadBodyRedacted: false,
        fileTransferUploadResultObserved: false,
        fileTransferUploadMode: '',
        fileTransferUploadPath: '',
        fileTransferUploadByteCount: 0,
        fileTransferUploadSampledBytes: 0,
        fileTransferUploadBodyRedacted: false,
        fileTransferResultOrder: '',
        flowClientEnginePushPatchConsumed: false,
        flowClientEnginePushStatusText: '',
        flowClientEnginePushOrderStatusText: '',
        flowClientEngineQueuedMessageCount: 0,
        flowClientEngineQueuedMessageHighWater: 0,
        flowClientEngineQueuedPushCount: 0,
        flowClientEngineQueuedMessageOrder: '',
        flowClientEngineDeliveredQueuedMessageCount: 0,
        flowClientEngineDeliveredQueuedMessageOrder: '',
        flowClientEngineDeliveredMessageCount: 0,
        flowClientEngineDeliveredPushCount: 0,
        flowClientEngineDeliveredMessageOrder: '',
        flowClientEngineIgnoredMessageCount: 0,
        flowClientEngineIgnoredMessageOrder: '',
        flowClientEngineDuplicateLateIgnored: false,
        flowClientEngineOrderingLastDeliveredServerSequence: 0,
        flowClientEngineRecoveryResyncRequested: false,
        flowClientEngineRecoveryResyncReason: '',
        flowClientEngineResyncRequested: false,
        flowClientEngineResyncPatchConsumed: false,
        flowClientEngineErrorReported: false,
        flowClientEngineErrorCode: '',
        flowClientEngineFirstMeaningfulPaintReported: false,
        flowClientEngineVisibleOwner: false,
        flowClientEngineVisibleRootId: '',
        flowClientEngineVisibleStatusText: '',
        flowClientEngineVisibleButtonText: '',
        flowClientEngineLocalFallbackHidden: false,
        flowClientEngineXhrError: '',
        flowClientEngineInitError: '',
        flowClientEngineResourceExecutionError: '',
        frameworkAppRendered: false,
        frameworkAppRootId: '',
        frameworkAppBudgetRendered: false,
        frameworkAppCheckboxRendered: false,
        frameworkAppSelectRendered: false,
        frameworkAppApplyRendered: false,
        frameworkAppStatusText: '',
        frameworkAppComponentStatusText: '',
        frameworkAppUpdatesText: '',
        frameworkAppBudgetEventRpcObserved: false,
        frameworkAppBudgetEventObserved: false,
        frameworkAppCheckboxEventRpcObserved: false,
        frameworkAppCheckboxEventObserved: false,
        frameworkAppSelectEventRpcObserved: false,
        frameworkAppSelectEventObserved: false,
        frameworkAppApplyEventRpcObserved: false,
        frameworkAppApplyEventObserved: false,
        frameworkAppEventPatchCount: 0,
        frameworkAppEventOrder: '',
        frameworkAppInteractionComplete: false,
        frameworkAppFailureReason: ''
      };

      function envelope(type, payload) {
        sequence += 1;
        return {
          version: 'flow-ipc-v1',
          type,
          id: 'webview-' + sequence,
          correlationId: 'webview-' + sequence,
          sessionId: 'flow-ipc-session',
          uiId: 'main',
          route: '',
          payload: payload || {},
          error: null,
          timestamp: Date.now()
        };
      }

      function send(type, payload) {
        const message = envelope(type, payload);
        trace.textContent = 'send ' + type + ' #' + message.id;
        window.webkit.messageHandlers.flowIpc.postMessage(message);
        return message;
      }

      function sendHostTrace(type, payload) {
        window.webkit.messageHandlers.flowIpc.postMessage({
          type,
          payload: payload || {},
          timestamp: Date.now()
        });
      }

      function sendWebViewError(code, message, details) {
        const envelopeMessage = envelope('error', details || {});
        envelopeMessage.error = {
          code,
          message
        };
        window.VaadinDesktopIpcDiagnostics.flowClientEngineErrorReported = true;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineErrorCode = code;
        trace.textContent = 'send error #' + envelopeMessage.id;
        window.webkit.messageHandlers.flowIpc.postMessage(envelopeMessage);
        maybeRequestDeferredComponentFrontendResourceGraph();
        return envelopeMessage;
      }

      function observeFlowIpcServerSequence(message) {
        const rawSequence = message && message.payload ? message.payload.serverSequence : undefined;
        const serverSequence = Number(rawSequence);
        if (!Number.isFinite(serverSequence) || serverSequence <= 0) {
          return;
        }
        if (lastFlowIpcServerSequence > 0 && serverSequence <= lastFlowIpcServerSequence) {
          window.VaadinDesktopIpcDiagnostics.flowIpcServerSequenceMonotonic = false;
        }
        lastFlowIpcServerSequence = serverSequence;
        flowIpcServerSequences.push((message.type || 'unknown') + ':' + serverSequence);
        window.VaadinDesktopIpcDiagnostics.flowIpcServerSequenceObserved = true;
        window.VaadinDesktopIpcDiagnostics.flowIpcServerSequenceCount = flowIpcServerSequences.length;
        window.VaadinDesktopIpcDiagnostics.flowIpcServerSequences = flowIpcServerSequences.join(',');
      }

      function updateClientState(payload) {
        if (payload && payload.clientState) {
          if (typeof payload.clientState.csrfToken === 'string') {
            flowClientState.csrfToken = payload.clientState.csrfToken;
          }
          if (typeof payload.clientState.securityKey === 'string') {
            flowClientState.securityKey = payload.clientState.securityKey;
          }
          if (typeof payload.clientState.securityTokenId === 'string') {
            flowClientState.securityTokenId = payload.clientState.securityTokenId;
          }
          if (Number.isInteger(payload.clientState.syncId)) {
            flowClientState.syncId = payload.clientState.syncId;
          }
          if (Number.isInteger(payload.clientState.clientId)) {
            flowClientState.clientId = payload.clientState.clientId;
          }
        }
        const body = payload && payload.uidl && payload.uidl.body;
        if (typeof body === 'string' && body.length > 0) {
          try {
            const uidl = JSON.parse(body);
            if (Number.isInteger(uidl.syncId)) {
              flowClientState.syncId = uidl.syncId;
            }
            if (typeof uidl[flowClientState.securityTokenId] === 'string') {
              flowClientState.securityKey = uidl[flowClientState.securityTokenId];
              flowClientState.csrfToken = uidl[flowClientState.securityTokenId];
            }
          } catch (_) {
            // The bridge still treats UIDL as an opaque payload until the Flow client engine consumes it.
          }
        }
      }

      function sendFlowEvent(node, eventType, eventData) {
        const data = eventData || {};
        if (sendFlowClientEngineEvent(node, eventType, data)) {
          return;
        }
        const clientId = flowClientState.clientId++;
        const securityKey = flowClientState.securityKey || flowClientState.csrfToken;
        const uidlRpc = {
          csrfToken: securityKey,
          securityKey,
          rpc: [{
            type: 'event',
            node: node.nodeId,
            event: eventType,
            data
          }],
          syncId: flowClientState.syncId,
          clientId
        };
        send('event', {
          dispatch: 'flow-client-rpc',
          componentId: node.id,
          nodeId: node.nodeId,
          securityKey,
          event: eventType,
          uidlRpc
        });
      }

      function flowClientEngineClientEntry() {
        const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients ? window.Vaadin.Flow.clients : {};
        if (clients[flowBootstrapClientId]) {
          const client = clients[flowBootstrapClientId];
          if (client && typeof client.sendEventMessage === 'function') {
            flowClientEngineBridgeClient = client;
            flowClientEngineBridgeClientKey = flowBootstrapClientId;
          }
          return { key: flowBootstrapClientId, client };
        }
        if (clients[flowBootstrapAppId]) {
          const client = clients[flowBootstrapAppId];
          if (client && typeof client.sendEventMessage === 'function') {
            flowClientEngineBridgeClient = client;
            flowClientEngineBridgeClientKey = flowBootstrapAppId;
          }
          return { key: flowBootstrapAppId, client };
        }
        for (const key of Object.keys(clients)) {
          const client = clients[key];
          if (client && typeof client.sendEventMessage === 'function') {
            flowClientEngineBridgeClient = client;
            flowClientEngineBridgeClientKey = key;
            return { key, client };
          }
        }
        if (flowClientEngineBridgeClient && typeof flowClientEngineBridgeClient.sendEventMessage === 'function') {
          return { key: flowClientEngineBridgeClientKey, client: flowClientEngineBridgeClient };
        }
        for (const key of Object.keys(clients)) {
          if (clients[key]) {
            return { key, client: clients[key] };
          }
        }
        return { key: '', client: null };
      }

      function flowClientEngineClient() {
        return flowClientEngineClientEntry().client;
      }

      function flowClientEngineRoot() {
        return document.getElementById('flow-client-engine-root');
      }

      function flowClientEngineContainer() {
        return document.querySelector(flowClientContainerTag());
      }

      function flowClientContainerTag() {
        return 'flow-container-' + flowBootstrapAppId.toLowerCase();
      }

      function isFlowShellInternalElement(element) {
        if (!element || !element.tagName) {
          return true;
        }
        const tag = element.tagName.toLowerCase();
        if (app && app.contains(element)) {
          return true;
        }
        return tag === 'script' || tag === 'style' || tag === 'link'
          || tag === 'template' || tag === 'meta' || element.id === 'trace'
          || element.id === 'flow-client-engine-root';
      }

      function firstFlowViewportRoot(container) {
        if (!container) {
          return null;
        }
        const selector = 'vaadin-app-layout, vaadin-vertical-layout, main, [data-flow-root], [data-flow-id]';
        const scopes = [container, flowClientEngineRoot(), document]
          .filter((scope, index, values) => scope && values.indexOf(scope) === index);
        for (const scope of scopes) {
          for (const semanticRoot of Array.from(scope.querySelectorAll(selector))) {
            if (!isFlowShellInternalElement(semanticRoot)) {
              return semanticRoot;
            }
          }
        }
        const directChildren = Array.from(container.children || [])
          .filter((element) => !isFlowShellInternalElement(element));
        return directChildren.length > 0 ? directChildren[0] : null;
      }

      function normalizeFlowClientViewport(container) {
        if (!container) {
          return null;
        }
        container.classList.add('vatori-flow-client-container');
        for (const previous of container.querySelectorAll('.vatori-flow-viewport-root')) {
          previous.classList.remove('vatori-flow-viewport-root');
        }
        const viewportRoot = firstFlowViewportRoot(container);
        if (viewportRoot) {
          viewportRoot.hidden = false;
          viewportRoot.classList.add('vatori-flow-viewport-root');
        }
        return viewportRoot;
      }

      function refreshFlowClientEngineViewportDiagnostics() {
        const container = flowClientEngineContainer();
        const viewportRoot = container ? firstFlowViewportRoot(container) : null;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineViewportReady = !!viewportRoot;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineViewportRootTag =
          viewportRoot && viewportRoot.tagName ? viewportRoot.tagName.toLowerCase() : '';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineViewportRootId =
          viewportRoot && viewportRoot.id ? viewportRoot.id : '';
        return viewportRoot;
      }

      function flowClientEngineElement(nodeId) {
        const client = flowClientEngineClient();
        if (client && typeof client.getByNodeId === 'function') {
          try {
            return client.getByNodeId(nodeId);
          } catch (_) {
            return null;
          }
        }
        return null;
      }

      function flowClientEngineElementById(id) {
        const root = flowClientEngineRoot() || document;
        const selector = '#' + id + ', [data-flow-id="' + id + '"]';
        const domElement = root.querySelector ? root.querySelector(selector) : null;
        if (domElement) {
          const rawNodeId = Number(domElement.dataset ? domElement.dataset.flowNodeId : NaN);
          if (Number.isInteger(rawNodeId) && rawNodeId > 0) {
            return { element: domElement, nodeId: rawNodeId, id };
          }
        }
        const mappedNodeId = flowClientNodeIdsById.get(id);
        if (Number.isInteger(mappedNodeId) && mappedNodeId > 0) {
          const mappedElement = flowClientEngineElement(mappedNodeId);
          if (mappedElement) {
            return { element: mappedElement, nodeId: mappedNodeId, id };
          }
        }
        for (let nodeId = 1; nodeId < 240; nodeId += 1) {
          const element = flowClientEngineElement(nodeId);
          const elementId = element && element.id ? element.id : '';
          const attributeId = element && typeof element.getAttribute === 'function'
            ? element.getAttribute('id')
            : '';
          const flowId = element && element.dataset ? element.dataset.flowId : '';
          if (element && (elementId === id || attributeId === id || flowId === id)) {
            return { element, nodeId, id };
          }
        }
        return null;
      }

      function flowClientElementIdByNodeId(nodeId) {
        const mappedId = flowClientIdsByNodeId.get(nodeId);
        if (mappedId) {
          return mappedId;
        }
        const found = flowClientEngineElement(nodeId);
        if (!found) {
          return '';
        }
        return found.id || (found.dataset ? found.dataset.flowId || '' : '') || '';
      }

      function resetFlowIpcViewport() {
        requestAnimationFrame(() => {
          const root = flowClientEngineRoot();
          const scroller = root || document.scrollingElement || document.documentElement;
          if (scroller && typeof scroller.scrollTo === 'function') {
            scroller.scrollTo(0, 0);
          } else if (scroller) {
            scroller.scrollTop = 0;
          }
          if (document.scrollingElement && document.scrollingElement !== scroller) {
            document.scrollingElement.scrollTop = 0;
          }
          window.scrollTo(0, 0);
        });
      }

      function reportFirstMeaningfulPaint(reason) {
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineFirstMeaningfulPaintReported) {
          return;
        }
        const viewportRoot = refreshFlowClientEngineViewportDiagnostics();
        window.VaadinDesktopIpcDiagnostics.flowClientEngineFirstMeaningfulPaintReported = true;
        sendHostTrace('webview.firstMeaningfulPaint', {
          label: 'main',
          transport: 'flow-ipc',
          reason,
          performanceNow: Math.round(performance.now()),
          viewportRootTag: viewportRoot && viewportRoot.tagName ? viewportRoot.tagName.toLowerCase() : '',
          viewportRootId: viewportRoot && viewportRoot.id ? viewportRoot.id : '',
          flowClientEngineVisibleOwner: window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner,
          flowClientEngineLocalFallbackHidden: window.VaadinDesktopIpcDiagnostics.flowClientEngineLocalFallbackHidden,
          promotionGate: window.VaadinDesktopIpcDiagnostics.flowClientEnginePromotionGate
        });
      }

      function flowClientEngineButton() {
        const byId = flowClientEngineElementById('ipc-button');
        if (byId) {
          return byId.element;
        }
        return flowClientEngineElement(16)
          || document.querySelector('#flow-client-engine-root #ipc-button')
          || document.querySelector('#flow-client-engine-root [data-flow-id="ipc-button"]')
          || document.querySelector('[data-flow-id="ipc-button"]');
      }

      function promoteFlowClientEngineToVisible() {
        const root = flowClientEngineRoot();
        const container = flowClientEngineContainer();
        const viewportRoot = container ? firstFlowViewportRoot(container) : null;
        if (!root || !container || !viewportRoot) {
          window.VaadinDesktopIpcDiagnostics.flowClientEnginePromotionGate = viewportRoot
            ? 'waiting-for-host-root'
            : 'waiting-for-generic-flow-viewport';
          refreshFlowClientEngineViewportDiagnostics();
          return false;
        }
        root.hidden = false;
        root.style.display = '';
        container.hidden = false;
        container.style.display = '';
        if (container.parentElement !== root) {
          root.appendChild(container);
        }
        app.hidden = true;
        app.style.display = 'none';
        normalizeFlowClientViewport(container);
        refreshFlowClientEngineViewportDiagnostics();
        window.VaadinDesktopIpcDiagnostics.flowClientEnginePromotionGate = 'generic-flow-viewport';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner = true;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleRootId = root.id;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineLocalFallbackHidden = app.hidden;
        resetFlowIpcViewport();
        reportFirstMeaningfulPaint('flow-client-engine-visible');
        return true;
      }

      function promoteFlowClientEngineWhenReady() {
        const client = flowClientEngineClient();
        const viewportRoot = refreshFlowClientEngineViewportDiagnostics();
        if (!client || typeof client.sendEventMessage !== 'function' || !viewportRoot) {
          return false;
        }
        try {
          const promoted = promoteFlowClientEngineToVisible();
          if (promoted) {
            flushQueuedFlowClientEngineMessages();
          }
          return promoted;
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError =
            'generic viewport promotion failed: ' + (error && error.message ? error.message : String(error));
          return false;
        }
      }

      function scheduleFlowClientEnginePromotionRetry() {
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner
            || flowClientEnginePromotionRetryActive) {
          return;
        }
        flowClientEnginePromotionRetryActive = true;
        setTimeout(() => {
          flowClientEnginePromotionRetryActive = false;
          if (window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner) {
            return;
          }
          flowClientEnginePromotionRetryAttempts += 1;
          refreshFlowClientEngineDiagnostics();
          if (!window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner
              && flowClientEnginePromotionRetryAttempts < 24) {
            scheduleFlowClientEnginePromotionRetry();
          }
        }, 250);
      }

      function refreshFlowClientEngineVisibleDiagnostics() {
        const statusById = flowClientEngineElementById('ipc-status');
        const buttonById = flowClientEngineElementById('ipc-button');
        const status = statusById ? statusById.element : flowClientEngineElement(14);
        const button = buttonById ? buttonById.element : flowClientEngineElement(16);
        window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleStatusText = status ? (status.textContent || '') : '';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleButtonText = button ? (button.textContent || '') : '';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineLocalFallbackHidden = app.hidden;
      }

      function appendFrameworkAppEventOrder(componentId) {
        const current = window.VaadinDesktopIpcDiagnostics.frameworkAppEventOrder;
        window.VaadinDesktopIpcDiagnostics.frameworkAppEventOrder =
          current ? current + ',' + componentId : componentId;
      }

      function refreshFrameworkAppDiagnostics() {
        const root = flowClientEngineElementById('framework-root');
        const budget = flowClientEngineElementById('framework-budget');
        const checkbox = flowClientEngineElementById('framework-expedite');
        const priority = flowClientEngineElementById('framework-priority');
        const apply = flowClientEngineElementById('framework-apply');
        const status = flowClientEngineElementById('framework-status');
        const componentStatus = flowClientEngineElementById('framework-component-status');
        const updates = flowClientEngineElementById('framework-updates');
        const statusText = status ? (status.element.textContent || '') : '';
        const componentStatusText = componentStatus ? (componentStatus.element.textContent || '') : '';
        const updatesText = updates ? (updates.element.textContent || '') : '';

        window.VaadinDesktopIpcDiagnostics.frameworkAppRendered =
          !!root && document.body.textContent.includes('Flow IPC Framework Desk');
        window.VaadinDesktopIpcDiagnostics.frameworkAppRootId = root ? root.id : '';
        window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetRendered =
          !!budget && budget.element.tagName.toLowerCase() === 'vaadin-big-decimal-field';
        window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxRendered =
          !!checkbox && checkbox.element.tagName.toLowerCase() === 'vaadin-checkbox';
        window.VaadinDesktopIpcDiagnostics.frameworkAppSelectRendered =
          !!priority && priority.element.tagName.toLowerCase() === 'vaadin-select'
          && !!customElements.get('vaadin-select');
        window.VaadinDesktopIpcDiagnostics.frameworkAppApplyRendered = !!apply;
        window.VaadinDesktopIpcDiagnostics.frameworkAppStatusText = statusText;
        window.VaadinDesktopIpcDiagnostics.frameworkAppComponentStatusText = componentStatusText;
        window.VaadinDesktopIpcDiagnostics.frameworkAppUpdatesText = updatesText;

        if (componentStatusText.includes('Budget changed: ' + autoFrameworkBudgetValue)) {
          window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventObserved = true;
        }
        if (componentStatusText.includes('Expedite changed: yes')) {
          window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventObserved = true;
        }
        if (componentStatusText.includes('Priority changed: ' + autoFrameworkPriorityValue)) {
          window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventObserved = true;
        }
        if (statusText.includes('Revision 1:')
            && updatesText.includes('Revision 1:')
            && updatesText.includes('priority ' + autoFrameworkPriorityValue)
            && updatesText.includes('Renderer owned by Flow client engine')) {
          window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventObserved = true;
        }
        window.VaadinDesktopIpcDiagnostics.frameworkAppInteractionComplete =
          window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventRpcObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventRpcObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventRpcObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventRpcObserved
          && window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventObserved;
        if (window.VaadinDesktopIpcDiagnostics.frameworkAppInteractionComplete) {
          autoFrameworkUpdateDone = true;
          autoFrameworkUpdateInFlight = false;
        }
      }

      function sendFlowClientEngineEvent(node, eventType, eventData) {
        const client = flowClientEngineClient();
        if (!client || typeof client.sendEventMessage !== 'function' || !window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          return false;
        }
        try {
          client.sendEventMessage(node.nodeId, eventType, eventData || {});
          return true;
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
          return false;
        }
      }

      function sendFrameworkAppEvent(componentId, eventType, eventData, prepare) {
        const target = flowClientEngineElementById(componentId);
        const client = flowClientEngineClient();
        if (!target || !client || typeof client.sendEventMessage !== 'function'
            || !window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          return false;
        }
        try {
          if (typeof prepare === 'function') {
            prepare(target.element);
          }
          autoFrameworkUpdateInFlight = true;
          client.sendEventMessage(target.nodeId, eventType, eventData || {});
          return true;
        } catch (error) {
          autoFrameworkUpdateInFlight = false;
          window.VaadinDesktopIpcDiagnostics.frameworkAppFailureReason =
            error && error.message ? error.message : String(error);
          return false;
        }
      }

      function frameworkAppStepDone() {
        refreshFrameworkAppDiagnostics();
        if (autoFrameworkUpdateStep === 0) {
          return window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventObserved;
        }
        if (autoFrameworkUpdateStep === 1) {
          return window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventObserved;
        }
        if (autoFrameworkUpdateStep === 2) {
          return window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventObserved;
        }
        return window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventObserved;
      }

      function advanceFrameworkAppStepIfDone() {
        while (autoFrameworkUpdateStep < 4 && frameworkAppStepDone()) {
          autoFrameworkUpdateStep += 1;
          autoFrameworkUpdateAttempts = 0;
        }
        if (autoFrameworkUpdateStep >= 4) {
          refreshFrameworkAppDiagnostics();
          if (window.VaadinDesktopIpcDiagnostics.frameworkAppInteractionComplete) {
            autoFrameworkUpdateDone = true;
          }
        }
      }

      function scheduleAutoFrameworkUpdate() {
        if (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__ || autoFrameworkUpdateDone) {
          return;
        }
        refreshFrameworkAppDiagnostics();
        if (!window.VaadinDesktopIpcDiagnostics.frameworkAppRendered
            || !window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner
            || !window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          setTimeout(scheduleAutoFrameworkUpdate, 120);
          return;
        }
        advanceFrameworkAppStepIfDone();
        if (autoFrameworkUpdateDone || autoFrameworkUpdateInFlight) {
          return;
        }
        if (autoFrameworkUpdateAttempts >= 18) {
          window.VaadinDesktopIpcDiagnostics.frameworkAppFailureReason =
            'framework-app-auto-update-step-timeout-' + autoFrameworkUpdateStep;
          return;
        }
        autoFrameworkUpdateAttempts += 1;

        let sent = false;
        if (autoFrameworkUpdateStep === 0) {
          sent = sendFrameworkAppEvent('framework-budget', 'change',
            { 'element.value': autoFrameworkBudgetValue },
            (element) => {
              element.value = autoFrameworkBudgetValue;
              if (element.inputElement) {
                element.inputElement.value = autoFrameworkBudgetValue;
              }
            });
        } else if (autoFrameworkUpdateStep === 1) {
          sent = sendFrameworkAppEvent('framework-expedite', 'change',
            { 'element.checked': true },
            (element) => {
              element.checked = true;
              if (element.inputElement) {
                element.inputElement.checked = true;
              }
            });
        } else if (autoFrameworkUpdateStep === 2) {
          sent = sendFrameworkAppEvent('framework-priority', 'change',
            { 'element.value': autoFrameworkPriorityValue },
            (element) => {
              element.value = autoFrameworkPriorityValue;
            });
        } else if (autoFrameworkUpdateStep === 3) {
          sent = sendFrameworkAppEvent('framework-apply', 'click', {});
        }
        if (!sent) {
          setTimeout(scheduleAutoFrameworkUpdate, 160);
          return;
        }
        setTimeout(() => {
          autoFrameworkUpdateInFlight = false;
          refreshFlowClientEngineDiagnostics();
          advanceFrameworkAppStepIfDone();
          if (!autoFrameworkUpdateDone) {
            setTimeout(scheduleAutoFrameworkUpdate, 160);
          }
        }, 850);
      }

      function startAutoFrameworkUpdatePolling() {
        if (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__ || autoFrameworkUpdateInterval) {
          return;
        }
        autoFrameworkUpdateInterval = setInterval(() => {
          if (autoFrameworkUpdateDone || window.VaadinDesktopIpcDiagnostics.frameworkAppFailureReason) {
            clearInterval(autoFrameworkUpdateInterval);
            return;
          }
          scheduleAutoFrameworkUpdate();
        }, 360);
        scheduleAutoFrameworkUpdate();
      }

      function flowClientEngineMessageOrdering(message, kind) {
        const payload = message && message.payload ? message.payload : {};
        const probe = payload.orderingProbe || {};
        const raw = Number(payload.serverSequence);
        const probeSequence = Number(probe.effectiveServerSequence);
        const effective = Number.isFinite(probeSequence) && probeSequence > 0 ? probeSequence : raw;
        const probeKind = typeof probe.kind === 'string' ? probe.kind : '';
        const sequenceText = Number.isFinite(effective) && effective > 0 ? String(effective) : 'unknown';
        return {
          raw,
          effective,
          probeKind,
          tag: kind + ':' + sequenceText + (probeKind ? ':' + probeKind : '')
        };
      }

      function recordIgnoredFlowClientEngineMessage(message, kind, reason) {
        const ordering = flowClientEngineMessageOrdering(message, kind);
        ignoredFlowClientEngineMessageOrder.push(ordering.tag);
        window.VaadinDesktopIpcDiagnostics.flowClientEngineIgnoredMessageCount =
          ignoredFlowClientEngineMessageOrder.length;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineIgnoredMessageOrder =
          ignoredFlowClientEngineMessageOrder.join(',');
        if (reason === 'duplicate-late' || ordering.probeKind === 'duplicate-late') {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineDuplicateLateIgnored = true;
          requestOrderingRecoveryResync('duplicate-late');
        }
      }

      function shouldIgnoreFlowClientEngineMessage(message, kind) {
        const ordering = flowClientEngineMessageOrdering(message, kind);
        if (ordering.probeKind === 'duplicate-late') {
          recordIgnoredFlowClientEngineMessage(message, kind, 'duplicate-late');
          return true;
        }
        if (Number.isFinite(ordering.effective) && ordering.effective > 0
            && ordering.effective <= flowClientEngineLastDeliveredServerSequence) {
          recordIgnoredFlowClientEngineMessage(message, kind, 'late');
          return true;
        }
        return false;
      }

      function recordDeliveredFlowClientEngineMessage(message, kind) {
        const ordering = flowClientEngineMessageOrdering(message, kind);
        deliveredFlowClientEngineMessageOrder.push(ordering.tag);
        if (Number.isFinite(ordering.effective) && ordering.effective > 0) {
          flowClientEngineLastDeliveredServerSequence = Math.max(
            flowClientEngineLastDeliveredServerSequence,
            ordering.effective);
          window.VaadinDesktopIpcDiagnostics.flowClientEngineOrderingLastDeliveredServerSequence =
            flowClientEngineLastDeliveredServerSequence;
        }
        window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredMessageCount =
          deliveredFlowClientEngineMessageOrder.length;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredMessageOrder =
          deliveredFlowClientEngineMessageOrder.join(',');
        if (kind === 'push') {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredPushCount += 1;
        }
      }

      function requestOrderingRecoveryResync(reason) {
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineRecoveryResyncRequested
            || pendingFlowClientEngineRecoveryResyncReason) {
          return;
        }
        pendingFlowClientEngineRecoveryResyncReason = 'ordering-recovery-' + reason;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineRecoveryResyncReason =
          pendingFlowClientEngineRecoveryResyncReason;
      }

      function deliverFlowClientEngineMessage(message, kind) {
        const client = flowClientEngineClient();
        const body = message && message.payload && message.payload.uidl && typeof message.payload.uidl.body === 'string'
          ? message.payload.uidl.body
          : '';
        if (!client || typeof client.handleIpcMessage !== 'function' || !body
            || !refreshFlowClientEngineViewportDiagnostics()) {
          return false;
        }
        if (shouldIgnoreFlowClientEngineMessage(message, kind)) {
          return true;
        }
        try {
          client.handleIpcMessage(body);
          recordDeliveredFlowClientEngineMessage(message, kind);
          if (kind === 'push') {
            window.VaadinDesktopIpcDiagnostics.flowClientEnginePushPatchConsumed = true;
          } else if (kind === 'resync') {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed = true;
          }
          setTimeout(refreshFlowClientEngineDiagnostics, 0);
          setTimeout(refreshFlowClientEngineDiagnostics, 100);
          return true;
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
          return false;
        }
      }

      function queueFlowClientEngineMessage(message, kind) {
        if (!message || !(message.payload && message.payload.uidl && typeof message.payload.uidl.body === 'string')) {
          return false;
        }
        const tag = flowClientEngineMessageOrdering(message, kind).tag;
        queuedFlowClientEngineMessages.push({ message, kind, tag });
        queuedFlowClientEngineMessageOrder.push(tag);
        window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageCount = queuedFlowClientEngineMessages.length;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageHighWater =
          Math.max(window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageHighWater, queuedFlowClientEngineMessages.length);
        if (kind === 'push') {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedPushCount += 1;
        }
        window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageOrder =
          queuedFlowClientEngineMessageOrder.join(',');
        return true;
      }

      function flushQueuedFlowClientEngineMessages() {
        if (queuedFlowClientEngineMessages.length === 0) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageCount = 0;
          return;
        }
        for (let index = 0; index < queuedFlowClientEngineMessages.length;) {
          const item = queuedFlowClientEngineMessages[index];
          if (deliverFlowClientEngineMessage(item.message, item.kind)) {
            deliveredQueuedFlowClientEngineMessageOrder.push(item.tag || item.kind);
            window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredQueuedMessageCount =
              deliveredQueuedFlowClientEngineMessageOrder.length;
            window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredQueuedMessageOrder =
              deliveredQueuedFlowClientEngineMessageOrder.join(',');
            queuedFlowClientEngineMessages.splice(index, 1);
          } else {
            index += 1;
          }
        }
        window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageCount = queuedFlowClientEngineMessages.length;
      }

      function installFlowIpcXhrBridge() {
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          return;
        }
        originalXMLHttpRequest = originalXMLHttpRequest || window.XMLHttpRequest;
        class FlowIpcXMLHttpRequest {
          constructor() {
            this.readyState = 0;
            this.status = 0;
            this.statusText = '';
            this.responseText = '';
            this.response = '';
            this.onreadystatechange = null;
            this.onload = null;
            this.onerror = null;
            this.onloadend = null;
            this.withCredentials = false;
            this._headers = {};
            this._method = '';
            this._url = '';
            this._delegate = new originalXMLHttpRequest();
            this._flowIpc = false;
          }

          open(method, url, async) {
            this._method = method || '';
            this._url = String(url || '');
            this._flowIpc = this._url.startsWith('flow-ipc://local/flow/');
            if (!this._flowIpc) {
              this._delegate.onreadystatechange = () => {
                this.readyState = this._delegate.readyState;
                this.status = this._delegate.status;
                this.statusText = this._delegate.statusText;
                this.responseText = this._delegate.responseText;
                this.response = this._delegate.response;
                this._emitReadyStateChange();
              };
              this._delegate.open(method, url, async !== false);
              return;
            }
            this.readyState = 1;
            this._emitReadyStateChange();
          }

          setRequestHeader(name, value) {
            if (this._flowIpc) {
              this._headers[name] = value;
            } else {
              this._delegate.setRequestHeader(name, value);
            }
          }

          getResponseHeader(name) {
            if (this._flowIpc) {
              return name && name.toLowerCase() === 'content-type' ? 'application/json; charset=UTF-8' : null;
            }
            return this._delegate.getResponseHeader(name);
          }

          getAllResponseHeaders() {
            if (this._flowIpc) {
              return 'content-type: application/json; charset=UTF-8\r\n';
            }
            return this._delegate.getAllResponseHeaders();
          }

          send(body) {
            if (!this._flowIpc) {
              this._delegate.withCredentials = this.withCredentials;
              this._delegate.send(body);
              return;
            }
            try {
              const requestPayload = typeof body === 'string' && body.length > 0 ? JSON.parse(body) : {};
              const securityKey = requestPayload[flowClientState.securityTokenId]
                || requestPayload.csrfToken
                || flowClientState.securityKey
                || flowClientState.csrfToken;
              window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrRequestObserved = true;
              window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrLastClientId = Number.isInteger(requestPayload.clientId) ? requestPayload.clientId : -1;
              const type = requestPayload.resynchronize === true ? 'resync' : 'event';
              const rpc = Array.isArray(requestPayload.rpc)
                ? requestPayload.rpc.find((entry) => entry && entry.type === 'event')
                : null;
              const rpcNodeId = rpc && Number.isInteger(rpc.node) ? rpc.node : -1;
              const rpcEventType = rpc && typeof rpc.event === 'string' ? rpc.event : (type === 'event' ? 'uidl-rpc' : 'resync');
              const rpcComponentId = rpcNodeId >= 0 ? flowClientElementIdByNodeId(rpcNodeId) : '';
              const ipcPayload = {
                dispatch: 'flow-client-xhr',
                componentId: rpcComponentId,
                nodeId: rpcNodeId,
                securityKey,
                event: type === 'event' ? rpcEventType : 'resync',
                uidlRpc: requestPayload,
                syncId: Number.isInteger(requestPayload.syncId) ? requestPayload.syncId : flowClientState.syncId,
                clientId: Number.isInteger(requestPayload.clientId) ? requestPayload.clientId : flowClientState.clientId
              };
              if (type === 'resync') {
                ipcPayload.reason = typeof requestPayload.reason === 'string' && requestPayload.reason
                  ? requestPayload.reason
                  : 'flow-client-xhr-resync';
                flowClientEngineResyncXhrSent = true;
              }
              const message = send(type, ipcPayload);
              pendingFlowClientXhrRequests.set(message.correlationId, {
                xhr: this,
                eventType: rpcEventType,
                componentId: rpcComponentId,
                nodeId: rpcNodeId
              });
            } catch (error) {
              this._fail(error);
            }
          }

          abort() {
            if (!this._flowIpc) {
              this._delegate.abort();
              return;
            }
            this.readyState = 0;
          }

          _complete(status, responseText) {
            this.status = status;
            this.statusText = status === 200 ? 'OK' : 'Flow IPC error';
            this.responseText = responseText || '';
            this.response = this.responseText;
            this.readyState = 4;
            this._emitReadyStateChange();
            if (status === 200 && typeof this.onload === 'function') {
              this.onload();
            } else if (status !== 200 && typeof this.onerror === 'function') {
              this.onerror();
            }
            if (typeof this.onloadend === 'function') {
              this.onloadend();
            }
          }

          _fail(error) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
            this._complete(500, '');
          }

          _emitReadyStateChange() {
            if (typeof this.onreadystatechange === 'function') {
              this.onreadystatechange();
            }
          }
        }
        window.XMLHttpRequest = FlowIpcXMLHttpRequest;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled = true;
      }

      function flowTreeTextById(node, id) {
        if (!node) {
          return '';
        }
        if (node.id === id && typeof node.text === 'string') {
          return node.text;
        }
        if (Array.isArray(node.children)) {
          for (const child of node.children) {
            const text = flowTreeTextById(child, id);
            if (text) {
              return text;
            }
          }
        }
        return '';
      }

      function completeFlowClientXhrIfPending(message) {
        const pending = pendingFlowClientXhrRequests.get(message.correlationId || '');
        if (!pending) {
          return false;
        }
        pendingFlowClientXhrRequests.delete(message.correlationId || '');
        const xhr = pending.xhr || pending;
        const body = message.payload && message.payload.uidl && typeof message.payload.uidl.body === 'string'
          ? message.payload.uidl.body
          : '';
        if (body) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrResponseObserved = true;
          if (message.type === 'patch' && message.payload && message.payload.reason === 'event') {
            if (pending.componentId && pending.componentId.startsWith('framework-')) {
              window.VaadinDesktopIpcDiagnostics.frameworkAppEventPatchCount += 1;
              appendFrameworkAppEventOrder(pending.componentId);
              if (pending.componentId === 'framework-budget') {
                window.VaadinDesktopIpcDiagnostics.frameworkAppBudgetEventRpcObserved = true;
              } else if (pending.componentId === 'framework-expedite') {
                window.VaadinDesktopIpcDiagnostics.frameworkAppCheckboxEventRpcObserved = true;
              } else if (pending.componentId === 'framework-priority') {
                window.VaadinDesktopIpcDiagnostics.frameworkAppSelectEventRpcObserved = true;
              } else if (pending.componentId === 'framework-apply') {
                window.VaadinDesktopIpcDiagnostics.frameworkAppApplyEventRpcObserved = true;
                autoFrameworkUpdateInFlight = false;
              }
            }
            if (pending.eventType === 'click') {
              autoClickInFlight = false;
              if (pending.componentId === 'ipc-button') {
                const statusText = flowTreeTextById(message.payload && message.payload.tree, 'ipc-status');
                if (statusText) {
                  window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapStatusText = statusText;
                }
                window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed = true;
                window.VaadinDesktopIpcDiagnostics.flowClientEngineEventStatusText =
                  statusText || window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapStatusText || 'click patch consumed';
                autoClickDone = true;
              }
            } else if (pending.eventType === 'change') {
              window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventPatchCount += 1;
              const currentOrder = window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventOrder;
              window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventOrder =
                currentOrder ? currentOrder + ',' + pending.componentId : pending.componentId;
              if (pending.componentId === 'ipc-text-input') {
                window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputEventObserved = true;
                window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputStatusText =
                  'Text input: ' + autoComponentEventValue;
              } else if (pending.componentId === 'ipc-checkbox') {
                window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved = true;
                window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxStatusText = 'Checkbox: checked';
              } else if (pending.componentId === 'ipc-vaadin-big-decimal') {
                window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventRpcObserved = true;
                window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventObserved = true;
                window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventComponentId = pending.componentId;
                window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventType = pending.eventType;
                window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventValue = autoVaadinWebComponentValue;
                window.VaadinDesktopIpcDiagnostics.vaadinWebComponentPostEventState =
                  'Vaadin BigDecimalField: ' + autoVaadinWebComponentValue;
              } else if (pending.componentId === 'ipc-vaadin-text-field') {
                window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventRpcObserved = true;
                window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventObserved = true;
                window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventComponentId = pending.componentId;
                window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventType = pending.eventType;
                window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventValue = autoVaadinTextFieldValue;
                window.VaadinDesktopIpcDiagnostics.vaadinTextFieldPostEventState =
                  'Vaadin TextField: ' + autoVaadinTextFieldValue;
              } else if (pending.componentId === 'ipc-vaadin-checkbox') {
                window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventRpcObserved = true;
                window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved = true;
                window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventComponentId = pending.componentId;
                window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventType = pending.eventType;
                window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventValue = String(autoVaadinCheckboxValue);
                window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentPostEventState = 'Vaadin Checkbox: checked';
              }
              advanceAutoComponentEventAfterPatch(pending.componentId);
            }
            maybeRequestDeferredComponentFrontendResourceGraph();
          }
          if (message.payload && message.payload.reason === 'resync') {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed = true;
            maybeRequestDeferredComponentFrontendResourceGraph();
          }
          setTimeout(scheduleAutoClick, 160);
          try {
            xhr._complete(200, body);
          } catch (error) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
          }
          setTimeout(refreshFlowClientEngineDiagnostics, 0);
          setTimeout(refreshFlowClientEngineDiagnostics, 100);
          setTimeout(refreshFlowClientEngineDiagnostics, 400);
          setTimeout(refreshFlowClientEngineDiagnostics, 800);
          scheduleFlowClientEnginePromotionRetry();
          setTimeout(startAutoFrameworkUpdatePolling, 260);
          setTimeout(scheduleAutoComponentEvents, 900);
        } else {
          autoClickInFlight = false;
          autoComponentEventInFlight = false;
          autoFrameworkUpdateInFlight = false;
          xhr._complete(500, '');
        }
        return true;
      }

      function scheduleAutoClick() {
        if (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__ || autoClickDone) {
          return;
        }
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed) {
          autoClickDone = true;
          autoClickInFlight = false;
          return;
        }
        if (autoClickInFlight) {
          return;
        }
        const client = flowClientEngineClient();
        const buttonTarget = flowClientEngineElementById('ipc-button');
        const button = buttonTarget ? buttonTarget.element : flowClientEngineButton();
        if (buttonTarget && client && typeof client.sendEventMessage === 'function' && window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          if (autoClickAttempts >= 5) {
            autoClickDone = true;
            return;
          }
          autoClickAttempts += 1;
          autoClickInFlight = true;
          client.sendEventMessage(buttonTarget.nodeId, 'click', {});
          setTimeout(() => {
            if (window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed) {
              autoClickDone = true;
              autoClickInFlight = false;
              return;
            }
            autoClickInFlight = false;
            refreshFlowClientEngineDiagnostics();
            scheduleAutoClick();
          }, 6000);
          return;
        }
        autoClickReadinessAttempts += 1;
        if (autoClickReadinessAttempts >= 30 && button && !autoComponentEventEnabled) {
          autoClickDone = true;
          button.click();
          return;
        }
        setTimeout(scheduleAutoClick, 50);
      }

      function rememberFlowClientNodeIdsFromTree(node) {
        if (!node || !Number.isInteger(node.nodeId)) {
          return;
        }
        if (node.id) {
          flowClientNodeIdsById.set(node.id, node.nodeId);
          flowClientIdsByNodeId.set(node.nodeId, node.id);
        }
        for (const child of node.children || []) {
          rememberFlowClientNodeIdsFromTree(child);
        }
      }

      function rememberFlowClientNodeIds(payload) {
        if (!payload) {
          return;
        }
        rememberFlowClientNodeIdsFromTree(payload.tree);
      }

      function requiredComponentResourceReady(descriptor) {
        if (!descriptor.requiresComponentResource) {
          return true;
        }
        const requiredTag = descriptor.requiredTag || '';
        return requiredTag ? !!customElements.get(requiredTag) : window.VaadinDesktopIpcDiagnostics.vaadinWebComponentTagDefined;
      }

      function recordVaadinComponentDispatch(descriptor) {
        if (descriptor.key === 'vaadin-checkbox') {
          window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventDispatchMode = 'dom-event';
          window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventComponentId = descriptor.id;
          window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventType = descriptor.eventType;
          window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventValue = String(descriptor.value);
          return;
        }
        if (descriptor.key === 'vaadin-text-field') {
          window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventDispatchMode = 'dom-event';
          window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventComponentId = descriptor.id;
          window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventType = descriptor.eventType;
          window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventValue = String(descriptor.value);
          return;
        }
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventDispatchMode = 'dom-event';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventComponentId = descriptor.id;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventType = descriptor.eventType;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventValue = String(descriptor.value);
      }

      function markVaadinComponentDomEventObserved(descriptor) {
        if (descriptor.key === 'vaadin-checkbox') {
          window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved = true;
        } else if (descriptor.key === 'vaadin-text-field') {
          window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventObserved = true;
        } else {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventObserved = true;
        }
      }

      function componentEventDone(descriptor) {
        const patchOrder = window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventOrder || '';
        const patchObserved = patchOrder.split(',').includes(descriptor.id);
        if (descriptor.key === 'text') {
          return window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputEventObserved || patchObserved;
        }
        if (descriptor.key === 'checkbox') {
          return window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved || patchObserved;
        }
        if (descriptor.key === 'vaadin-checkbox') {
          return window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventRpcObserved
            && (patchObserved
              || window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved
              || window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentPostEventState.includes('Vaadin Checkbox: checked'));
        }
        if (descriptor.key === 'vaadin-text-field') {
          return window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventRpcObserved
            && (patchObserved
              || window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventObserved
              || window.VaadinDesktopIpcDiagnostics.vaadinTextFieldPostEventState.includes(autoVaadinTextFieldValue));
        }
        return window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventRpcObserved
          && (patchObserved
            || window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventObserved
            || window.VaadinDesktopIpcDiagnostics.vaadinWebComponentPostEventState.includes(autoVaadinWebComponentValue));
      }

      function advanceAutoComponentEventAfterPatch(componentId) {
        if (!autoComponentEventEnabled || autoComponentEventsDone) {
          return;
        }
        const descriptor = autoComponentEvents[autoComponentEventIndex];
        if (!descriptor || descriptor.id !== componentId) {
          return;
        }
        autoComponentEventIndex += 1;
        autoComponentEventInFlight = false;
        if (autoComponentEventIndex >= autoComponentEvents.length) {
          autoComponentEventsDone = true;
          requestAutoResyncIfReady();
        } else {
          setTimeout(scheduleAutoComponentEvents, autoComponentEventDelayMs);
        }
      }

      function scheduleAutoComponentEvents() {
        if (!autoComponentEventEnabled || autoComponentEventsDone) {
          return;
        }
        const baselineEventReady = !window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__
          || window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed;
        if (!baselineEventReady
            || !window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          setTimeout(scheduleAutoComponentEvents, autoComponentEventRetryMs);
          return;
        }
        if (autoComponentEventInFlight) {
          return;
        }
        if (autoComponentEventIndex >= autoComponentEvents.length) {
          autoComponentEventsDone = true;
          requestAutoResyncIfReady();
          return;
        }

        const descriptor = autoComponentEvents[autoComponentEventIndex];
        if (!requiredComponentResourceReady(descriptor)) {
          autoComponentEventAttempts += 1;
          if (autoComponentEventAttempts < autoComponentEventRetryLimit) {
            setTimeout(scheduleAutoComponentEvents, autoComponentEventRetryMs);
          } else {
            if (descriptor.key === 'vaadin-checkbox') {
              window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentFailureReason =
                'component-resource-not-executed-before-event';
            } else if (descriptor.key === 'vaadin-text-field') {
              window.VaadinDesktopIpcDiagnostics.vaadinTextFieldFailureReason =
                'component-resource-not-executed-before-event';
            } else {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
                'component-resource-not-executed-before-event';
            }
          }
          return;
        }
        const target = flowClientEngineElementById(descriptor.id);
        const client = flowClientEngineClient();
        if (!target || !client || typeof client.sendEventMessage !== 'function') {
          autoComponentEventAttempts += 1;
          if (autoComponentEventAttempts < autoComponentEventRetryLimit) {
            setTimeout(scheduleAutoComponentEvents, autoComponentEventRetryMs);
          }
          return;
        }

        autoComponentEventAttempts = 0;
        autoComponentEventInFlight = true;
        const dispatchedIndex = autoComponentEventIndex;
        try {
          descriptor.prepare(target.element);
          if (descriptor.dispatchDomEvent) {
            recordVaadinComponentDispatch(descriptor);
            target.element.dispatchEvent(new Event(descriptor.eventType, {
              bubbles: true,
              composed: true
            }));
            markVaadinComponentDomEventObserved(descriptor);
          } else {
            client.sendEventMessage(target.nodeId, descriptor.eventType, descriptor.data());
          }
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
          autoComponentEventInFlight = false;
          return;
        }

        setTimeout(() => {
          if (autoComponentEventIndex !== dispatchedIndex) {
            return;
          }
          autoComponentEventInFlight = false;
          refreshFlowClientEngineDiagnostics();
          if (componentEventDone(descriptor)) {
            autoComponentEventIndex += 1;
          }
          if (autoComponentEventIndex >= autoComponentEvents.length) {
            autoComponentEventsDone = true;
            requestAutoResyncIfReady();
          } else {
            setTimeout(scheduleAutoComponentEvents, autoComponentEventDelayMs);
          }
        }, autoComponentEventTimeoutMs);
      }

      function render(node) {
        if (!node) {
          app.textContent = 'No Flow IPC tree received.';
          return;
        }
        flowNodeElements.clear();
        flowTextNodes.clear();
        flowUidlNodes.clear();
        app.replaceChildren(renderNode(node));
      }

      function requestResync(reason) {
        const client = flowClientEngineClient();
        if (client && typeof client.requestIpcResync === 'function') {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncRequested = true;
          try {
            client.requestIpcResync();
            setTimeout(() => {
              if (!flowClientEngineResyncXhrSent
                  && !window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed) {
                sendResyncFallback(reason);
              }
            }, 180);
            return;
          } catch (error) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
          }
        }
        sendResyncFallback(reason);
      }

      function requestAutoResyncIfReady() {
        if (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ || autoResyncDone) {
          return;
        }
        if (autoComponentEventEnabled && !autoComponentEventsDone) {
          return;
        }
        autoResyncDone = true;
        if (pendingFlowClientEngineRecoveryResyncReason) {
          const recoveryReason = pendingFlowClientEngineRecoveryResyncReason;
          pendingFlowClientEngineRecoveryResyncReason = '';
          window.VaadinDesktopIpcDiagnostics.flowClientEngineRecoveryResyncRequested = true;
          window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncRequested = true;
          setTimeout(() => sendResyncFallback(recoveryReason), 80);
          return;
        }
        setTimeout(() => requestResync('verification-resync-after-event'), 80);
      }

      function sendResyncFallback(reason) {
        const securityKey = flowClientState.securityKey || flowClientState.csrfToken;
        flowClientEngineResyncXhrSent = true;
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrBridgeInstalled) {
          const clientId = Math.max(
            flowClientState.clientId,
            window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrLastClientId + 1,
            1);
          const xhr = new XMLHttpRequest();
          xhr.open('POST', 'flow-ipc://local/flow/?v-r=uidl&v-uiId=1', true);
          xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
          xhr.onerror = () => {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = 'verification resync XHR fallback failed';
          };
          xhr.send(JSON.stringify({
            [flowClientState.securityTokenId]: securityKey,
            csrfToken: securityKey,
            resynchronize: true,
            reason,
            clientId,
            syncId: flowClientState.syncId,
            rpc: []
          }));
          return;
        }
        send('resync', {
          reason,
          securityKey,
          syncId: flowClientState.syncId,
          clientId: flowClientState.clientId
        });
      }

      function scheduleAutoErrorProbe() {
        if (!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR__ || autoErrorProbeDone) {
          return;
        }
        if (!window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed) {
          return;
        }
        if (window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__
            && !window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed) {
          return;
        }
        autoErrorProbeDone = true;
        setTimeout(() => sendWebViewError('webviewVerificationProbe',
          'Verification error probe from WebView bridge',
          { reason: 'verification-error-probe' }), 40);
      }

      function csvUnique(values) {
        const unique = [];
        for (const value of values) {
          if (typeof value !== 'string' || !value || unique.includes(value)) {
            continue;
          }
          unique.push(value);
        }
        return unique.join(',');
      }

      function mergeCsv(existing, values) {
        const merged = [];
        if (typeof existing === 'string' && existing) {
          merged.push(...existing.split(','));
        }
        merged.push(...values);
        return csvUnique(merged);
      }

      function recordFrontendArtifactManifest(manifest) {
        if (!manifest || typeof manifest !== 'object') {
          return;
        }
        if (typeof manifest.frontendAssetPipelineVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendAssetPipelineVersion =
            manifest.frontendAssetPipelineVersion;
        }
        if (typeof manifest.artifactGraphSource === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphSource =
            manifest.artifactGraphSource;
        }
        if (typeof manifest.textfieldGraphParityVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphParityVersion =
            manifest.textfieldGraphParityVersion;
        }
        if (typeof manifest.textfieldBlockerVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldBlockerVersion =
            manifest.textfieldBlockerVersion;
        }
        if (typeof manifest.textfieldPreviousBlockerVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldBlockerVersion =
            manifest.textfieldPreviousBlockerVersion;
        }
        if (typeof manifest.textfieldExecutionVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecutionVersion =
            manifest.textfieldExecutionVersion;
        }
        if (typeof manifest.textfieldSideEffectVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectVersion =
            manifest.textfieldSideEffectVersion;
        }
        if (typeof manifest.textfieldGraphProbe === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphProbe =
            manifest.textfieldGraphProbe;
        }
        if (typeof manifest.textfieldEntryArtifact === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldEntryArtifact =
            manifest.textfieldEntryArtifact;
        }
        if (typeof manifest.textfieldClassExportArtifact === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldClassExportArtifact =
            manifest.textfieldClassExportArtifact;
        }
        if (Array.isArray(manifest.textfieldTransitiveArtifacts)) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldTransitiveArtifacts =
            manifest.textfieldTransitiveArtifacts.filter((value) => typeof value === 'string').join(',');
        }
        if (typeof manifest.textfieldImportResolutionStrategy === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldImportResolutionStrategy =
            manifest.textfieldImportResolutionStrategy;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldModuleIdentityStrategy =
            manifest.textfieldImportResolutionStrategy;
        }
        if (typeof manifest.textfieldSideEffectStrategy === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectStrategy =
            manifest.textfieldSideEffectStrategy;
        }
        if (typeof manifest.textfieldFirstDefinitionSource === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldFirstDefinitionSource =
            manifest.textfieldFirstDefinitionSource;
        }
        if (typeof manifest.textfieldSecondDefinitionSource === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSecondDefinitionSource =
            manifest.textfieldSecondDefinitionSource;
        }
        if (typeof manifest.textfieldDuplicateTag === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldDuplicateTag =
            manifest.textfieldDuplicateTag;
        }
        if (typeof manifest.textfieldRemainingBlockerClassification === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldRemainingBlockerClassification =
            manifest.textfieldRemainingBlockerClassification;
        }
        if (typeof manifest.textfieldNarrowedFailureClassification === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldNarrowedFailureClassification =
            manifest.textfieldNarrowedFailureClassification;
        }
        if (typeof manifest.textfieldFailureLayer === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldFailureLayer =
            manifest.textfieldFailureLayer;
        }
        if (typeof manifest.textfieldMinimalReproduction === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldMinimalReproduction =
            manifest.textfieldMinimalReproduction;
        }
        if (typeof manifest.textfieldNextResolutionStep === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldNextResolutionStep =
            manifest.textfieldNextResolutionStep;
        }
        if (window.VaadinDesktopIpcDiagnostics.frontendAssetPipelineVersion
            || window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphSource) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphManifestObserved = true;
        }
        const families = Array.isArray(manifest.artifactFamilies)
          ? manifest.artifactFamilies.filter((value) => typeof value === 'string')
          : [];
        if (families.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilies =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilies, families);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilyCount =
            window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilies
              ? window.VaadinDesktopIpcDiagnostics.frontendArtifactFamilies.split(',').length
              : 0;
        }
        const resources = Array.isArray(manifest.resources) ? manifest.resources : [];
        const requestedBy = resources.map((entry) => entry && entry.requestedBy);
        const dependencyOf = resources.map((entry) => entry && entry.dependencyOf);
        const moduleSpecifiers = resources.map((entry) => entry && entry.moduleSpecifier);
        const executionPolicies = resources.map((entry) => entry && entry.executionPolicy);
        const artifactStatuses = resources.map((entry) => entry && entry.artifactStatus);
        if (requestedBy.concat(dependencyOf, moduleSpecifiers, executionPolicies)
            .some((value) => typeof value === 'string' && value)) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphDependencyObserved = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphRequestedBy =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphRequestedBy, requestedBy);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphDependencyOf =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphDependencyOf, dependencyOf);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphModuleSpecifiers =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactGraphModuleSpecifiers, moduleSpecifiers);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactExecutionPolicies =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactExecutionPolicies, executionPolicies);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactStatuses =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactStatuses, artifactStatuses);
        }
        window.VaadinDesktopIpcDiagnostics.frontendArtifactShimFallback =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactShimFallback
          || resources.some((entry) => entry
            && entry.artifactFamily === 'npm-frontend'
            && (entry.executionPolicy === 'blocked-shim-fallback'
              || entry.executionPolicy === 'legacy-shim-fallback'));
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundlePartial =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundlePartial
          || resources.some((entry) => entry
            && entry.artifactFamily === 'production-bundle'
            && entry.artifactStatus === 'partial');
        const productionBundleStatsResources = resources.filter((entry) => entry
          && entry.artifactFamily === 'production-bundle'
          && entry.artifactStatus === 'available'
          && entry.productionBundleStatsGraph === true
          && typeof entry.path === 'string'
          && entry.path);
        if (productionBundleStatsResources.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsPath =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsPath,
              productionBundleStatsResources.map((entry) => entry.path));
        }
        const themeStaticResources = resources.filter((entry) => entry
          && entry.artifactFamily === 'theme-static'
          && entry.artifactStatus === 'available'
          && typeof entry.path === 'string'
          && entry.path);
        if (themeStaticResources.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticVerified = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticPath =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticPath,
              themeStaticResources.map((entry) => entry.path));
        }
        const generatedLazyResources = resources.filter((entry) => entry
          && entry.artifactFamily === 'generated-lazy'
          && entry.artifactStatus === 'available'
          && typeof entry.path === 'string'
          && entry.path);
        if (generatedLazyResources.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyVerified = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyPath =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyPath,
              generatedLazyResources.map((entry) => entry.path));
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolverVersion =
            manifest.generatedLazyResolverVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolverVersion;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionVersion =
            manifest.generatedLazyExecutionVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionVersion;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyImportResolutionStrategy =
            manifest.generatedLazyImportResolutionStrategy || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyImportResolutionStrategy;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandContractVersion =
            manifest.generatedLazyLoadOnDemandContractVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandContractVersion;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyTriggerSource =
            manifest.generatedLazyTriggerSource || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyTriggerSource;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiOwner =
            manifest.generatedLazyApiOwner || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiOwner;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiInstallSource =
            manifest.generatedLazyApiInstallSource || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiInstallSource;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRequestedChunk =
            manifest.generatedLazyRequestedChunk || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRequestedChunk;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRegistrySource =
            manifest.generatedLazyRegistrySource || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRegistrySource;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyMappingSource =
            manifest.generatedLazyMappingSource || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyMappingSource;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedArtifactPath =
            manifest.generatedLazyResolvedArtifactPath || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedArtifactPath;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFlowClientTriggered =
            manifest.generatedLazyFlowClientTriggered === true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyDiagnosticBridge =
            manifest.generatedLazyDiagnosticBridge === true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeImportSpecifier =
            manifest.generatedLazyRelativeImportSpecifier || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeImportSpecifier;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedDependencyPath =
            manifest.generatedLazyResolvedDependencyPath || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedDependencyPath;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFailureLayer =
            manifest.generatedLazyFailureLayer || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFailureLayer;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlockerVersion =
            manifest.generatedLazyBlockerVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlockerVersion;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyNextResolutionStep =
            manifest.generatedLazyNextResolutionStep || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyNextResolutionStep;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource =
            manifest.generatedLazyChunkExpressionSource || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner =
            manifest.generatedLazyChunkTriggerOwner || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkResolvedArtifactPath =
            manifest.generatedLazyChunkResolvedArtifactPath || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkResolvedArtifactPath;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkDiagnosticBridgeUsed =
            manifest.generatedLazyChunkDiagnosticBridgeUsed === true;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkDirectImportProbeUsed =
            manifest.generatedLazyChunkDirectImportProbeUsed === true;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer =
            manifest.generatedLazyChunkFailureLayer || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkBlockerVersion =
            manifest.generatedLazyChunkBlockerVersion || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkBlockerVersion;
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkNextResolutionStep =
            manifest.generatedLazyChunkNextResolutionStep || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkNextResolutionStep;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkContractVersion =
            manifest.loadOnDemandChunkContractVersion || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkContractVersion;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiOwner =
            manifest.loadOnDemandChunkApiOwner || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiOwner;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiInstallSource =
            manifest.loadOnDemandChunkApiInstallSource || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiInstallSource;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRegistrySource =
            manifest.loadOnDemandChunkRegistrySource || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRegistrySource;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkMappingSource =
            manifest.loadOnDemandChunkMappingSource || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkMappingSource;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkResolvedArtifactPath =
            manifest.loadOnDemandChunkResolvedArtifactPath || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkResolvedArtifactPath;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkModuleIdentity =
            manifest.loadOnDemandChunkModuleIdentity || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkModuleIdentity;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer =
            manifest.loadOnDemandChunkFailureLayer || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBlockerVersion =
            manifest.loadOnDemandChunkBlockerVersion || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBlockerVersion;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkNextResolutionStep =
            manifest.loadOnDemandChunkNextResolutionStep || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkNextResolutionStep;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistryVersion =
            manifest.generatedLoadOnDemandChunkFileRegistryVersion
            || window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistryVersion;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistrySource =
            manifest.generatedLoadOnDemandChunkFileRegistrySource
            || window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistrySource;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileMappingSource =
            manifest.generatedLoadOnDemandChunkFileMappingSource
            || window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileMappingSource;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandGeneratedImportsArtifact =
            manifest.generatedLoadOnDemandGeneratedImportsArtifact
            || window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandGeneratedImportsArtifact;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceBuildMode =
            manifest.generatedLoadOnDemandSourceBuildMode
            || window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceBuildMode;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceArtifactFamily =
            manifest.generatedLoadOnDemandSourceArtifactFamily
            || window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceArtifactFamily;
          window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandNoOpLoadOnDemand =
            manifest.generatedLoadOnDemandNoOpLoadOnDemand === true;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandTableShape =
            manifest.loadOnDemandTableShape || window.VaadinDesktopIpcDiagnostics.loadOnDemandTableShape;
          window.VaadinDesktopIpcDiagnostics.chunkImportSpecifier =
            manifest.chunkImportSpecifier || window.VaadinDesktopIpcDiagnostics.chunkImportSpecifier;
          window.VaadinDesktopIpcDiagnostics.chunkArtifactPath =
            manifest.chunkArtifactPath || window.VaadinDesktopIpcDiagnostics.chunkArtifactPath;
          window.VaadinDesktopIpcDiagnostics.chunkFileResolver =
            manifest.chunkFileResolver || window.VaadinDesktopIpcDiagnostics.chunkFileResolver;
          window.VaadinDesktopIpcDiagnostics.chunkFileStatus =
            manifest.chunkFileStatus || window.VaadinDesktopIpcDiagnostics.chunkFileStatus;
          window.VaadinDesktopIpcDiagnostics.chunkFileBytes =
            Number.isFinite(manifest.chunkFileBytes) ? manifest.chunkFileBytes
              : window.VaadinDesktopIpcDiagnostics.chunkFileBytes;
          window.VaadinDesktopIpcDiagnostics.chunkFileSha256 =
            manifest.chunkFileSha256 || window.VaadinDesktopIpcDiagnostics.chunkFileSha256;
          window.VaadinDesktopIpcDiagnostics.noChunkGeneratedReason =
            manifest.noChunkGeneratedReason || window.VaadinDesktopIpcDiagnostics.noChunkGeneratedReason;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer =
            manifest.loadOnDemandChunkFileFailureLayer
            || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBlockerVersion =
            manifest.loadOnDemandChunkFileBlockerVersion
            || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBlockerVersion;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileNextResolutionStep =
            manifest.loadOnDemandChunkFileNextResolutionStep
            || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileNextResolutionStep;
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileSideEffectProof =
            manifest.loadOnDemandChunkFileSideEffectProof
            || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileSideEffectProof;
        }
        if (typeof manifest.runtimeResolverContractVersion === 'string') {
          window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverContractVersion =
            manifest.runtimeResolverContractVersion;
        }
        const runtimeResolverResources = resources.filter((entry) => entry
          && typeof entry.runtimeResolverContractVersion === 'string'
          && entry.runtimeResolverContractVersion);
        if (runtimeResolverResources.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverLogicalRequestIds =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverLogicalRequestIds,
              runtimeResolverResources.map((entry) => entry.logicalRequestId));
          window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverPathMappings =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendRuntimeResolverPathMappings,
              runtimeResolverResources.map((entry) => entry.pathMapping));
        }
        const flowLazyResources = resources.filter((entry) => entry
          && entry.flowLazyDependencyResource === true
          && typeof entry.path === 'string'
          && entry.path);
        if (flowLazyResources.length > 0) {
          const lazy = flowLazyResources[0];
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyObserved = true;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyVersion =
            manifest.flowLazyDependencyVersion || lazy.lazyDependencyVersion || '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencySource =
            manifest.flowLazyDependencySource || lazy.lazyDependencySource || '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyMode =
            manifest.flowLazyDependencyMode || lazy.lazyDependencyMode || '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExpression =
            manifest.flowLazyDependencyExpression || lazy.lazyDependencyExpression || '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyChunkId =
            manifest.flowLazyDependencyChunkId || lazy.lazyDependencyChunkId || '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyResolvedPath =
            manifest.flowLazyDependencyResolvedPath || lazy.lazyDependencyResolvedPath || lazy.path;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggerOwner =
            manifest.flowLazyDependencyTriggerOwner || lazy.lazyDependencyTriggerOwner || '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggeredByFlowClient =
            manifest.flowLazyDependencyTriggeredByFlowClient === true
            || lazy.lazyDependencyTriggeredByFlowClient === true;
        }
        const productionRuntimeResources = resources.filter((entry) => entry
          && entry.productionBundleRuntimeArtifact === true
          && typeof entry.path === 'string'
          && entry.path);
        if (productionRuntimeResources.length > 0) {
          const runtime = productionRuntimeResources[0];
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeObserved = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeVersion =
            manifest.productionBundleRuntimeVersion || runtime.productionBundleRuntimeVersion || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeRegistrySource =
            manifest.productionBundleRuntimeRegistrySource || runtime.productionBundleRuntimeRegistrySource || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStatsSource =
            manifest.productionBundleRuntimeStatsSource || runtime.productionBundleRuntimeStatsSource || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeSelectedArtifact =
            manifest.productionBundleRuntimeSelectedArtifact || runtime.productionBundleRuntimeSelectedArtifact
            || runtime.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeDependencyChain =
            manifest.productionBundleRuntimeDependencyChain || runtime.productionBundleRuntimeDependencyChain || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeModuleIdentity =
            manifest.productionBundleRuntimeModuleIdentity || runtime.productionBundleRuntimeModuleIdentity || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeNextResolutionStep =
            manifest.productionBundleRuntimeNextResolutionStep
            || runtime.productionBundleRuntimeNextResolutionStep
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerObserved = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerVersion =
            manifest.productionBundleRuntimeOwnerVersion || runtime.productionBundleRuntimeOwnerVersion || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwner =
            manifest.productionBundleRuntimeOwner || runtime.productionBundleRuntimeOwner || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerEntryArtifact =
            manifest.productionBundleRuntimeEntryArtifact || runtime.productionBundleRuntimeEntryArtifact
            || runtime.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSource =
            manifest.productionBundleRuntimeSource || runtime.productionBundleRuntimeSource || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSelectedBy =
            manifest.productionBundleRuntimeSelectedBy || runtime.productionBundleRuntimeSelectedBy || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerModuleIdentity =
            manifest.productionBundleRuntimeOwnerModuleIdentity
            || runtime.productionBundleRuntimeOwnerModuleIdentity
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerLifecycle =
            manifest.productionBundleRuntimeOwnerLifecycle
            || runtime.productionBundleRuntimeOwnerLifecycle
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy =
            manifest.productionBundleRuntimeOwnerExecutionPolicy
            || runtime.productionBundleRuntimeOwnerExecutionPolicy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy =
            manifest.productionBundleRuntimeOwnerStageUrlPolicy
            || runtime.productionBundleRuntimeOwnerStageUrlPolicy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCachePolicy =
            manifest.productionBundleRuntimeOwnerCachePolicy
            || runtime.productionBundleRuntimeOwnerCachePolicy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy =
            manifest.productionBundleRuntimeOwnerCleanupStrategy
            || runtime.productionBundleRuntimeOwnerCleanupStrategy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer =
            manifest.productionBundleRuntimeOwnerFailureLayer
            || runtime.productionBundleRuntimeOwnerFailureLayer
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlockerVersion =
            manifest.productionBundleRuntimeOwnerBlockerVersion
            || runtime.productionBundleRuntimeOwnerBlockerVersion
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep =
            manifest.productionBundleRuntimeOwnerNextResolutionStep
            || runtime.productionBundleRuntimeOwnerNextResolutionStep
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVersion =
            manifest.productionBundleRuntimeOwnerImportVersion
            || runtime.productionBundleRuntimeOwnerImportVersion
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPolicy =
            manifest.productionBundleRuntimeOwnerImportPolicy
            || runtime.productionBundleRuntimeOwnerImportPolicy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportArtifact =
            manifest.productionBundleRuntimeOwnerImportArtifact
            || runtime.productionBundleRuntimeOwnerImportArtifact
            || runtime.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhase =
            manifest.productionBundleRuntimeOwnerImportPhase
            || runtime.productionBundleRuntimeOwnerImportPhase
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates =
            manifest.productionBundleRuntimeOwnerImportPhaseCandidates
            || runtime.productionBundleRuntimeOwnerImportPhaseCandidates
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes =
            manifest.productionBundleRuntimeOwnerImportDiagnosticModes
            || runtime.productionBundleRuntimeOwnerImportDiagnosticModes
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects =
            manifest.productionBundleRuntimeOwnerImportExpectedSideEffects
            || runtime.productionBundleRuntimeOwnerImportExpectedSideEffects
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures =
            manifest.productionBundleRuntimeOwnerImportAcceptableFailures
            || runtime.productionBundleRuntimeOwnerImportAcceptableFailures
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy =
            manifest.productionBundleRuntimeOwnerImportRewriteStrategy
            || runtime.productionBundleRuntimeOwnerImportRewriteStrategy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths =
            manifest.productionBundleRuntimeOwnerImportDependencyPaths
            || runtime.productionBundleRuntimeOwnerImportDependencyPaths
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion =
            manifest.productionBundleRuntimeOwnerDependencyResolverVersion
            || runtime.productionBundleRuntimeOwnerDependencyResolverVersion
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot =
            manifest.productionBundleRuntimeOwnerDependencyResolverRoot
            || runtime.productionBundleRuntimeOwnerDependencyResolverRoot
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy =
            manifest.productionBundleRuntimeOwnerDependencyResolverPolicy
            || runtime.productionBundleRuntimeOwnerDependencyResolverPolicy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts =
            manifest.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
            || runtime.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures =
            manifest.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
            || runtime.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace =
            manifest.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
            || runtime.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact =
            manifest.productionBundleRuntimeOwnerDependencyResolverRootArtifact
            || runtime.productionBundleRuntimeOwnerDependencyResolverRootArtifact
            || runtime.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep =
            manifest.productionBundleRuntimeOwnerImportNextResolutionStep
            || runtime.productionBundleRuntimeOwnerImportNextResolutionStep
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion =
            manifest.productionBundleRuntimeOwnerImportOrderingVersion
            || runtime.productionBundleRuntimeOwnerImportOrderingVersion
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy =
            manifest.productionBundleRuntimeOwnerImportPhasePolicy
            || runtime.productionBundleRuntimeOwnerImportPhasePolicy
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult =
            manifest.productionBundleRuntimeOwnerImportOrderingResult
            || runtime.productionBundleRuntimeOwnerImportOrderingResult
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases =
            manifest.productionBundleRuntimeOwnerImportOrderingObservedPhases
            || runtime.productionBundleRuntimeOwnerImportOrderingObservedPhases
            || '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures =
            manifest.productionBundleRuntimeOwnerImportOrderingAcceptableFailures
            || runtime.productionBundleRuntimeOwnerImportOrderingAcceptableFailures
            || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseVersion =
            manifest.componentProductionOwnerReuseVersion || runtime.componentProductionOwnerReuseVersion || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReusePolicy =
            manifest.componentProductionOwnerReusePolicy || runtime.componentProductionOwnerReusePolicy || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseMode =
            manifest.componentProductionOwnerReuseMode || runtime.componentProductionOwnerReuseMode || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseAcceptableFailures =
            manifest.componentProductionOwnerReuseAcceptableFailures
            || runtime.componentProductionOwnerReuseAcceptableFailures
            || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused =
            manifest.componentProductionOwnerReused === true || runtime.componentProductionOwnerReused === true;
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason =
            manifest.componentProductionOwnerReuseReason || runtime.componentProductionOwnerReuseReason || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerOwnedTags =
            manifest.componentProductionOwnerOwnedTags || runtime.componentProductionOwnerOwnedTags || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerSkippedDefinitions =
            manifest.componentProductionOwnerSkippedDefinitions || runtime.componentProductionOwnerSkippedDefinitions || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerFallbackImports =
            manifest.componentProductionOwnerFallbackImports || runtime.componentProductionOwnerFallbackImports || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths =
            manifest.componentProductionOwnerDependencyPaths || runtime.componentProductionOwnerDependencyPaths || '';
          window.VaadinDesktopIpcDiagnostics.componentProductionOwnerImportOrder =
            manifest.componentProductionOwnerImportOrder || runtime.componentProductionOwnerImportOrder || '';
        }
        const devModeResources = resources.filter((entry) => entry
          && entry.artifactFamily === 'dev-mode-bundle'
          && entry.artifactStatus === 'available'
          && typeof entry.path === 'string'
          && entry.path);
        if (devModeResources.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePath =
            mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePath,
              devModeResources.map((entry) => entry.path));
        }
        const blockers = Array.isArray(manifest.strictBlockers) ? manifest.strictBlockers : [];
        if (blockers.length > 0) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactStrictBlockerCount =
            Math.max(window.VaadinDesktopIpcDiagnostics.frontendArtifactStrictBlockerCount, blockers.length);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactStrictBlockers = csvUnique(blockers.map((entry) => {
            if (!entry || typeof entry.artifactFamily !== 'string') {
              return '';
            }
            return entry.artifactFamily + ':' + (entry.failureClassification || 'blocked');
          }));
          window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticBlocker =
            window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticBlocker
            || blockers.some((entry) => entry && entry.artifactFamily === 'theme-static' && entry.strictBlocker === true);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModeBlocker =
            window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModeBlocker
            || blockers.some((entry) => entry && entry.artifactFamily === 'dev-mode-bundle' && entry.strictBlocker === true);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker =
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker
            || blockers.some((entry) => entry && entry.artifactFamily === 'generated-lazy' && entry.strictBlocker === true);
        }
      }

      function artifactRequestMetadata(resource) {
        return {
          artifactFamily: resource.artifactFamily || '',
          moduleSpecifier: resource.moduleSpecifier || '',
          dependencyOf: resource.dependencyOf || '',
          executionPolicy: resource.executionPolicy || '',
          requestedBy: resource.requestedBy || '',
          resolver: resource.resolver || '',
          artifactStatus: resource.artifactStatus || '',
          lookupKind: resource.lookupKind || '',
          textfieldGraphParityVersion: resource.textfieldGraphParityVersion || '',
          blockerVersion: resource.blockerVersion || '',
          previousBlockerVersion: resource.previousBlockerVersion || '',
          executionVersion: resource.executionVersion || '',
          textfieldSideEffectVersion: resource.textfieldSideEffectVersion || '',
          graphProbe: resource.graphProbe || '',
          graphEntryArtifact: resource.graphEntryArtifact || '',
          classExportArtifact: resource.classExportArtifact || '',
          transitiveImports: Array.isArray(resource.transitiveImports) ? resource.transitiveImports : [],
          importResolutionStrategy: resource.importResolutionStrategy || '',
          sideEffectStrategy: resource.sideEffectStrategy || '',
          narrowedFailureClassification: resource.narrowedFailureClassification || '',
          failureLayer: resource.failureLayer || '',
          minimalReproduction: resource.minimalReproduction || '',
          nextResolutionStep: resource.nextResolutionStep || '',
          firstDefinitionSource: resource.firstDefinitionSource || '',
          secondDefinitionSource: resource.secondDefinitionSource || '',
          duplicateTag: resource.duplicateTag || '',
          remainingBlockerClassification: resource.remainingBlockerClassification || '',
          moduleIdentityStrategy: resource.moduleIdentityStrategy || '',
          generatedLazyResolverVersion: resource.generatedLazyResolverVersion || '',
          generatedLazyExecutionVersion: resource.generatedLazyExecutionVersion || '',
          generatedLazyImportResolutionStrategy: resource.generatedLazyImportResolutionStrategy || '',
          generatedLazyLoadOnDemandContractVersion: resource.generatedLazyLoadOnDemandContractVersion || '',
          generatedLazyTriggerSource: resource.generatedLazyTriggerSource || '',
          generatedLazyApiOwner: resource.generatedLazyApiOwner || '',
          generatedLazyApiInstallSource: resource.generatedLazyApiInstallSource || '',
          generatedLazyRequestedChunk: resource.generatedLazyRequestedChunk || '',
          generatedLazyRegistrySource: resource.generatedLazyRegistrySource || '',
          generatedLazyMappingSource: resource.generatedLazyMappingSource || '',
          generatedLazyResolvedArtifactPath: resource.generatedLazyResolvedArtifactPath || '',
          generatedLazyFlowClientTriggered: resource.generatedLazyFlowClientTriggered === true,
          generatedLazyDiagnosticBridge: resource.generatedLazyDiagnosticBridge === true,
          generatedLazyRelativeImportSpecifier: resource.generatedLazyRelativeImportSpecifier || '',
          generatedLazyResolvedDependencyPath: resource.generatedLazyResolvedDependencyPath || '',
          generatedLazyGraphProbe: resource.generatedLazyGraphProbe || '',
          generatedLazyBlockerVersion: resource.generatedLazyBlockerVersion || '',
          generatedLazyChunkExpressionSource: resource.generatedLazyChunkExpressionSource || '',
          generatedLazyChunkTriggerOwner: resource.generatedLazyChunkTriggerOwner || '',
          generatedLazyChunkResolvedArtifactPath: resource.generatedLazyChunkResolvedArtifactPath || '',
          generatedLazyChunkDiagnosticBridgeUsed: resource.generatedLazyChunkDiagnosticBridgeUsed === true,
          generatedLazyChunkDirectImportProbeUsed: resource.generatedLazyChunkDirectImportProbeUsed === true,
          generatedLazyChunkFailureLayer: resource.generatedLazyChunkFailureLayer || '',
          generatedLazyChunkBlockerVersion: resource.generatedLazyChunkBlockerVersion || '',
          generatedLazyChunkNextResolutionStep: resource.generatedLazyChunkNextResolutionStep || '',
          loadOnDemandChunkContractVersion: resource.loadOnDemandChunkContractVersion || '',
          loadOnDemandChunkApiOwner: resource.loadOnDemandChunkApiOwner || '',
          loadOnDemandChunkApiInstallSource: resource.loadOnDemandChunkApiInstallSource || '',
          loadOnDemandChunkRegistrySource: resource.loadOnDemandChunkRegistrySource || '',
          loadOnDemandChunkMappingSource: resource.loadOnDemandChunkMappingSource || '',
          loadOnDemandChunkResolvedArtifactPath: resource.loadOnDemandChunkResolvedArtifactPath || '',
          loadOnDemandChunkModuleIdentity: resource.loadOnDemandChunkModuleIdentity || '',
          loadOnDemandChunkFailureLayer: resource.loadOnDemandChunkFailureLayer || '',
          loadOnDemandChunkBlockerVersion: resource.loadOnDemandChunkBlockerVersion || '',
          loadOnDemandChunkNextResolutionStep: resource.loadOnDemandChunkNextResolutionStep || '',
          generatedLoadOnDemandChunkFileRegistryVersion: resource.generatedLoadOnDemandChunkFileRegistryVersion || '',
          generatedLoadOnDemandChunkFileRegistrySource: resource.generatedLoadOnDemandChunkFileRegistrySource || '',
          generatedLoadOnDemandChunkFileMappingSource: resource.generatedLoadOnDemandChunkFileMappingSource || '',
          generatedLoadOnDemandGeneratedImportsArtifact: resource.generatedLoadOnDemandGeneratedImportsArtifact || '',
          generatedLoadOnDemandSourceBuildMode: resource.generatedLoadOnDemandSourceBuildMode || '',
          generatedLoadOnDemandSourceArtifactFamily: resource.generatedLoadOnDemandSourceArtifactFamily || '',
          generatedLoadOnDemandNoOpLoadOnDemand: resource.generatedLoadOnDemandNoOpLoadOnDemand === true,
          loadOnDemandTableShape: resource.loadOnDemandTableShape || '',
          chunkId: resource.chunkId || '',
          chunkImportSpecifier: resource.chunkImportSpecifier || '',
          chunkArtifactPath: resource.chunkArtifactPath || '',
          chunkContentHash: resource.chunkContentHash || '',
          chunkFileResolver: resource.chunkFileResolver || '',
          chunkFileStatus: resource.chunkFileStatus || '',
          chunkFileBytes: Number.isFinite(resource.chunkFileBytes) ? resource.chunkFileBytes : 0,
          chunkFileSha256: resource.chunkFileSha256 || '',
          noChunkGeneratedReason: resource.noChunkGeneratedReason || '',
          loadOnDemandChunkFileRegistryVersion: resource.loadOnDemandChunkFileRegistryVersion || '',
          loadOnDemandChunkFileRegistrySource: resource.loadOnDemandChunkFileRegistrySource || '',
          loadOnDemandChunkFileMappingSource: resource.loadOnDemandChunkFileMappingSource || '',
          loadOnDemandChunkFileGeneratedImportsArtifact: resource.loadOnDemandChunkFileGeneratedImportsArtifact || '',
          loadOnDemandChunkFileNoOpLoadOnDemand: resource.loadOnDemandChunkFileNoOpLoadOnDemand === true,
          loadOnDemandChunkFileTableShape: resource.loadOnDemandChunkFileTableShape || '',
          loadOnDemandChunkFileResolver: resource.loadOnDemandChunkFileResolver || '',
          loadOnDemandChunkFileStatus: resource.loadOnDemandChunkFileStatus || '',
          loadOnDemandChunkFileBytes: Number.isFinite(resource.loadOnDemandChunkFileBytes)
            ? resource.loadOnDemandChunkFileBytes
            : 0,
          loadOnDemandChunkFileSha256: resource.loadOnDemandChunkFileSha256 || '',
          loadOnDemandChunkFileFailureLayer: resource.loadOnDemandChunkFileFailureLayer || '',
          loadOnDemandChunkFileBlockerVersion: resource.loadOnDemandChunkFileBlockerVersion || '',
          loadOnDemandChunkFileNextResolutionStep: resource.loadOnDemandChunkFileNextResolutionStep || '',
          loadOnDemandChunkFileSideEffectProof: resource.loadOnDemandChunkFileSideEffectProof || '',
          runtimeResolverContractVersion: resource.runtimeResolverContractVersion || '',
          logicalRequestId: resource.logicalRequestId || '',
          pathMapping: resource.pathMapping || '',
          stageUrlPolicy: resource.stageUrlPolicy || '',
          cachePolicy: resource.cachePolicy || '',
          cleanupOwnership: resource.cleanupOwnership || '',
          blockerClassification: resource.blockerClassification || '',
          flowLazyDependencyResource: resource.flowLazyDependencyResource === true,
          lazyDependencyVersion: resource.lazyDependencyVersion || '',
          lazyDependencySource: resource.lazyDependencySource || '',
          lazyDependencyMode: resource.lazyDependencyMode || '',
          lazyDependencyExpression: resource.lazyDependencyExpression || '',
          lazyDependencyChunkId: resource.lazyDependencyChunkId || '',
          lazyDependencyResolvedPath: resource.lazyDependencyResolvedPath || '',
          lazyDependencyTriggerOwner: resource.lazyDependencyTriggerOwner || '',
          lazyDependencyTriggeredByFlowClient: resource.lazyDependencyTriggeredByFlowClient === true,
          lazyDependencyResolver: resource.lazyDependencyResolver || '',
          productionBundleRuntimeArtifact: resource.productionBundleRuntimeArtifact === true,
          productionBundleRuntimeVersion: resource.productionBundleRuntimeVersion || '',
          productionBundleRuntimeRegistrySource: resource.productionBundleRuntimeRegistrySource || '',
          productionBundleRuntimeStatsSource: resource.productionBundleRuntimeStatsSource || '',
          productionBundleRuntimeSelectedArtifact: resource.productionBundleRuntimeSelectedArtifact || '',
          productionBundleRuntimeDependencyChain: resource.productionBundleRuntimeDependencyChain || '',
          productionBundleRuntimeModuleIdentity: resource.productionBundleRuntimeModuleIdentity || '',
          productionBundleRuntimeExecutionPolicy: resource.productionBundleRuntimeExecutionPolicy || '',
          productionBundleRuntimeBlockerVersion: resource.productionBundleRuntimeBlockerVersion || '',
          productionBundleRuntimeFailureLayer: resource.productionBundleRuntimeFailureLayer || '',
          productionBundleRuntimeNextResolutionStep: resource.productionBundleRuntimeNextResolutionStep || '',
          productionBundleRuntimeOwnerVersion: resource.productionBundleRuntimeOwnerVersion || '',
          productionBundleRuntimeOwner: resource.productionBundleRuntimeOwner || '',
          productionBundleRuntimeEntryArtifact: resource.productionBundleRuntimeEntryArtifact || '',
          productionBundleRuntimeSource: resource.productionBundleRuntimeSource || '',
          productionBundleRuntimeSelectedBy: resource.productionBundleRuntimeSelectedBy || '',
          productionBundleRuntimeOwnerModuleIdentity: resource.productionBundleRuntimeOwnerModuleIdentity || '',
          productionBundleRuntimeOwnerLifecycle: resource.productionBundleRuntimeOwnerLifecycle || '',
          productionBundleRuntimeOwnerExecutionPolicy: resource.productionBundleRuntimeOwnerExecutionPolicy || '',
          productionBundleRuntimeOwnerStageUrlPolicy: resource.productionBundleRuntimeOwnerStageUrlPolicy || '',
          productionBundleRuntimeOwnerCachePolicy: resource.productionBundleRuntimeOwnerCachePolicy || '',
          productionBundleRuntimeOwnerCleanupStrategy: resource.productionBundleRuntimeOwnerCleanupStrategy || '',
          productionBundleRuntimeOwnerBlockerVersion: resource.productionBundleRuntimeOwnerBlockerVersion || '',
          productionBundleRuntimeOwnerFailureLayer: resource.productionBundleRuntimeOwnerFailureLayer || '',
          productionBundleRuntimeOwnerSideEffectProof: resource.productionBundleRuntimeOwnerSideEffectProof || '',
          productionBundleRuntimeOwnerBrowserError: resource.productionBundleRuntimeOwnerBrowserError || '',
          productionBundleRuntimeOwnerNextResolutionStep: resource.productionBundleRuntimeOwnerNextResolutionStep || '',
          productionBundleRuntimeOwnerExpectedSideEffect: resource.productionBundleRuntimeOwnerExpectedSideEffect || '',
          productionBundleRuntimeOwnerCustomElementTags: resource.productionBundleRuntimeOwnerCustomElementTags || '',
          productionBundleRuntimeOwnerImportVersion: resource.productionBundleRuntimeOwnerImportVersion || '',
          productionBundleRuntimeOwnerImportPolicy: resource.productionBundleRuntimeOwnerImportPolicy || '',
          productionBundleRuntimeOwnerImportArtifact: resource.productionBundleRuntimeOwnerImportArtifact || '',
          productionBundleRuntimeOwnerImportPhase: resource.productionBundleRuntimeOwnerImportPhase || '',
          productionBundleRuntimeOwnerImportExpectedSideEffects:
            resource.productionBundleRuntimeOwnerImportExpectedSideEffects || '',
          productionBundleRuntimeOwnerImportAcceptableFailures:
            resource.productionBundleRuntimeOwnerImportAcceptableFailures || '',
          productionBundleRuntimeOwnerImportRewriteStrategy:
            resource.productionBundleRuntimeOwnerImportRewriteStrategy || '',
          productionBundleRuntimeOwnerImportDependencyPaths:
            resource.productionBundleRuntimeOwnerImportDependencyPaths || '',
          productionBundleRuntimeOwnerDependencyResolverVersion:
            resource.productionBundleRuntimeOwnerDependencyResolverVersion || '',
          productionBundleRuntimeOwnerDependencyResolverRoot:
            resource.productionBundleRuntimeOwnerDependencyResolverRoot || '',
          productionBundleRuntimeOwnerDependencyResolverPolicy:
            resource.productionBundleRuntimeOwnerDependencyResolverPolicy || '',
          productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts:
            resource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts || '',
          productionBundleRuntimeOwnerDependencyResolverAcceptableFailures:
            resource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures || '',
          productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace:
            resource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace || '',
          productionBundleRuntimeOwnerDependencyResolverRootArtifact:
            resource.productionBundleRuntimeOwnerDependencyResolverRootArtifact || '',
          productionOwnerDependencyResource: resource.productionOwnerDependencyResource === true,
          productionBundleRuntimeOwnerImportAttempted:
            resource.productionBundleRuntimeOwnerImportAttempted === true,
          productionBundleRuntimeOwnerImported:
            resource.productionBundleRuntimeOwnerImported === true,
          productionBundleRuntimeOwnerImportNextResolutionStep:
            resource.productionBundleRuntimeOwnerImportNextResolutionStep || '',
          productionBundleRuntimeOwnerImportOrderingVersion:
            resource.productionBundleRuntimeOwnerImportOrderingVersion || '',
          productionBundleRuntimeOwnerImportPhasePolicy:
            resource.productionBundleRuntimeOwnerImportPhasePolicy || '',
          productionBundleRuntimeOwnerImportOrderingResult:
            resource.productionBundleRuntimeOwnerImportOrderingResult || '',
          productionBundleRuntimeOwnerImportOrderingObservedPhases:
            resource.productionBundleRuntimeOwnerImportOrderingObservedPhases || '',
          productionBundleRuntimeOwnerImportOrderingAcceptableFailures:
            resource.productionBundleRuntimeOwnerImportOrderingAcceptableFailures || '',
          componentProductionOwnerReuseVersion: resource.componentProductionOwnerReuseVersion || '',
          componentProductionOwnerReusePolicy: resource.componentProductionOwnerReusePolicy || '',
          componentProductionOwnerReuseMode: resource.componentProductionOwnerReuseMode || '',
          componentProductionOwnerReuseAcceptableFailures:
            resource.componentProductionOwnerReuseAcceptableFailures || '',
          componentProductionOwnerReused: resource.componentProductionOwnerReused === true,
          componentProductionOwnerReuseReason: resource.componentProductionOwnerReuseReason || '',
          componentProductionOwnerOwnedTags: resource.componentProductionOwnerOwnedTags || '',
          componentProductionOwnerSkippedDefinitions: resource.componentProductionOwnerSkippedDefinitions || '',
          componentProductionOwnerFallbackImports: resource.componentProductionOwnerFallbackImports || '',
          componentProductionOwnerDependencyPaths: resource.componentProductionOwnerDependencyPaths || '',
          componentProductionOwnerImportOrder: resource.componentProductionOwnerImportOrder || '',
          legacyShimSuccessPath: resource.legacyShimSuccessPath === true
        };
      }

      function stagedUrlScheme(stagedUrl) {
        if (typeof stagedUrl !== 'string') {
          return '';
        }
        const index = stagedUrl.indexOf(':');
        return index > 0 ? stagedUrl.substring(0, index) : '';
      }

      function refreshFrontendStagingDiagnostics() {
        window.VaadinDesktopIpcDiagnostics.frontendStagingResolverVersion = 'flow-ipc-resource-staging-v1_5';
        window.VaadinDesktopIpcDiagnostics.frontendStagingObserved = frontendStagedResourcePaths.size > 0;
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceCount = frontendStagedResourcePaths.size;
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourcePaths =
          Array.from(frontendStagedResourcePaths).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceFamilies =
          Array.from(frontendStagedResourceFamilies).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceSchemes =
          Array.from(new Set(Array.from(frontendStagedResourceUrls.values()))).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceCacheHitCount =
          frontendStagedResourceCacheHitPaths.size;
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceCacheMissCount =
          frontendStagedResourceCacheMissPaths.size;
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceExecutedCount =
          frontendStagedResourceExecutedPaths.size;
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceExecutedPaths =
          Array.from(frontendStagedResourceExecutedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceFailedCount =
          frontendStagedResourceFailedPaths.size;
        window.VaadinDesktopIpcDiagnostics.frontendStagedResourceFailedPaths =
          Array.from(frontendStagedResourceFailedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderObserved =
          frontendHostResourceLoaderPaths.size > 0;
        window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderObservedPaths =
          Array.from(frontendHostResourceLoaderPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderExecuted =
          frontendHostResourceLoaderExecutedPaths.size > 0;
        window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderExecutedPaths =
          Array.from(frontendHostResourceLoaderExecutedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendResourceBlobFallbackObserved =
          frontendHostResourceLoaderFallbackPaths.size > 0;
        window.VaadinDesktopIpcDiagnostics.frontendResourceBlobFallbackPaths =
          Array.from(frontendHostResourceLoaderFallbackPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldModuleIdentityPaths =
          Array.from(frontendModuleIdentityResourcePaths).join(',');
      }

      function recordFrontendStage(resource, stagedUrl, executionStatus, cacheStatus, extra) {
        if (!resource || typeof resource.path !== 'string' || !resource.path) {
          return;
        }
        const sourcePath = resource.path;
        const body = extra && typeof extra.body === 'string' ? extra.body : '';
        const bytes = Number.isFinite(resource.bytes) ? resource.bytes : (body ? new Blob([body]).size : 0);
        const family = resource.artifactFamily || '';
        const scheme = stagedUrlScheme(stagedUrl);
        const stageOwner = extra && extra.owner
          ? extra.owner
          : (resource.productionBundleRuntimeArtifact === true
            ? 'production-bundle-runtime'
            : (resource.componentFrontendResource === true && resource.artifactFamily === 'production-bundle'
              ? 'component-focused-entry'
              : ''));
        frontendStagedResourcePaths.add(sourcePath);
        if (family) {
          frontendStagedResourceFamilies.add(family);
        }
        if (scheme) {
          frontendStagedResourceUrls.set(sourcePath, scheme);
        }
        if (cacheStatus === 'hit') {
          frontendStagedResourceCacheHitPaths.add(sourcePath);
        } else if (cacheStatus === 'miss') {
          frontendStagedResourceCacheMissPaths.add(sourcePath);
        }
        if (executionStatus === 'executed') {
          frontendStagedResourceExecutedPaths.add(sourcePath);
          frontendStagedResourceFailedPaths.delete(sourcePath);
        } else if (executionStatus === 'strict-blocker' || executionStatus === 'execution-failed') {
          frontendStagedResourceFailedPaths.add(sourcePath);
        }
        if (extra && extra.frontendResourceSchemeVersion === 'flow-ipc-resource-scheme-v1_17') {
          frontendHostResourceLoaderPaths.add(sourcePath);
          if (executionStatus === 'executed') {
            frontendHostResourceLoaderExecutedPaths.add(sourcePath);
          }
          if (extra.stagingMode === 'blob-fallback') {
            frontendHostResourceLoaderFallbackPaths.add(sourcePath);
          }
          window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeVersion =
            extra.frontendResourceSchemeVersion;
          window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderMode =
            extra.frontendResourceLoaderMode || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeName =
            extra.frontendResourceSchemeName || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeHandlerInstalled =
            extra.frontendResourceSchemeHandlerInstalled === true;
          window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeRequestObserved =
            extra.frontendResourceSchemeRequestObserved === true
            || window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeRequestObserved;
          window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeResponseObserved =
            extra.frontendResourceSchemeResponseObserved === true
            || window.VaadinDesktopIpcDiagnostics.frontendResourceSchemeResponseObserved;
          window.VaadinDesktopIpcDiagnostics.frontendResourceLoaderNoLocalhost =
            extra.frontendResourceLoaderNoLocalhost === true;
          window.VaadinDesktopIpcDiagnostics.frontendResourceFileBacked =
            extra.frontendResourceFileBacked === true;
          window.VaadinDesktopIpcDiagnostics.frontendResourceMimeType = extra.mimeType || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceFilePathRedacted =
            extra.filePathRedacted || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceFileDirectoryRedacted =
            extra.fileDirectoryRedacted || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceBaseUrl =
            extra.resourceBaseUrl || stagedUrl || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceRelativeImportBase =
            extra.relativeImportBase || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceRelativeImportResolutionMode =
            extra.relativeImportResolutionMode || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceResolvedRelativeImportPaths =
            extra.resolvedRelativeImportPaths || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceMissingRelativeImportPaths =
            extra.missingRelativeImportPaths || '';
          window.VaadinDesktopIpcDiagnostics.frontendResourceNoLocalhostProof =
            extra.noLocalhostProof || '';
        }
        refreshFrontendStagingDiagnostics();
        sendHostTrace('resourceStage', {
          stagingResolverVersion: 'flow-ipc-resource-staging-v1_5',
          frontendResourceSchemeVersion: extra && extra.frontendResourceSchemeVersion
            ? extra.frontendResourceSchemeVersion
            : '',
          frontendResourceLoaderMode: extra && extra.frontendResourceLoaderMode
            ? extra.frontendResourceLoaderMode
            : '',
          frontendResourceSchemeName: extra && extra.frontendResourceSchemeName
            ? extra.frontendResourceSchemeName
            : '',
          frontendResourceSchemeHandlerInstalled: extra
            && typeof extra.frontendResourceSchemeHandlerInstalled === 'boolean'
            ? extra.frontendResourceSchemeHandlerInstalled
            : false,
          frontendResourceSchemeRequestObserved: extra
            && typeof extra.frontendResourceSchemeRequestObserved === 'boolean'
            ? extra.frontendResourceSchemeRequestObserved
            : false,
          frontendResourceSchemeResponseObserved: extra
            && typeof extra.frontendResourceSchemeResponseObserved === 'boolean'
            ? extra.frontendResourceSchemeResponseObserved
            : false,
          frontendResourceLoaderNoLocalhost: extra
            && typeof extra.frontendResourceLoaderNoLocalhost === 'boolean'
            ? extra.frontendResourceLoaderNoLocalhost
            : false,
          frontendResourceFileBacked: extra
            && typeof extra.frontendResourceFileBacked === 'boolean'
            ? extra.frontendResourceFileBacked
            : false,
          stagingMode: extra && extra.stagingMode ? extra.stagingMode : 'blob-fallback',
          resourceBaseUrl: extra && extra.resourceBaseUrl ? extra.resourceBaseUrl : stagedUrl,
          relativeImportBase: extra && extra.relativeImportBase ? extra.relativeImportBase : '',
          relativeImportResolutionMode: extra && extra.relativeImportResolutionMode
            ? extra.relativeImportResolutionMode
            : '',
          resolvedRelativeImportPaths: extra && extra.resolvedRelativeImportPaths
            ? extra.resolvedRelativeImportPaths
            : '',
          missingRelativeImportPaths: extra && extra.missingRelativeImportPaths
            ? extra.missingRelativeImportPaths
            : '',
          mimeType: extra && extra.mimeType ? extra.mimeType : 'text/javascript; charset=utf-8',
          filePathRedacted: extra && extra.filePathRedacted ? extra.filePathRedacted : '',
          fileDirectoryRedacted: extra && extra.fileDirectoryRedacted ? extra.fileDirectoryRedacted : '',
          noLocalhostProof: extra && extra.noLocalhostProof ? extra.noLocalhostProof : '',
          blobFallbackReason: extra && extra.blobFallbackReason ? extra.blobFallbackReason : '',
          blobFallbackBlockedLayer: extra && extra.blobFallbackBlockedLayer ? extra.blobFallbackBlockedLayer : '',
          blobFallbackNextResolutionStep: extra && extra.blobFallbackNextResolutionStep
            ? extra.blobFallbackNextResolutionStep
            : '',
          sourcePath,
          stagedUrl,
          stagedUrlScheme: scheme,
          stagedPath: '',
          owner: stageOwner,
          artifactFamily: family,
          moduleSpecifier: resource.moduleSpecifier || '',
          sha256: resource.sha256 || '',
          bytes,
          dependencyOf: resource.dependencyOf || '',
          requestedBy: resource.requestedBy || '',
          resolver: resource.resolver || '',
          lookupKind: resource.lookupKind || '',
          cacheStatus: cacheStatus || '',
          cleanupPolicy: extra && extra.cleanupPolicy
            ? extra.cleanupPolicy
            : 'revoke-on-module-graph-complete',
          executionStatus,
          failureClassification: extra && extra.failureClassification
            ? extra.failureClassification
            : (resource.failureClassification || ''),
          narrowedFailureClassification: extra && extra.narrowedFailureClassification
            ? extra.narrowedFailureClassification
            : (resource.narrowedFailureClassification || ''),
          blockerVersion: extra && extra.blockerVersion
            ? extra.blockerVersion
            : (resource.blockerVersion || ''),
          previousBlockerVersion: extra && extra.previousBlockerVersion
            ? extra.previousBlockerVersion
            : (resource.previousBlockerVersion || ''),
          executionVersion: extra && extra.executionVersion
            ? extra.executionVersion
            : (resource.executionVersion || ''),
          textfieldSideEffectVersion: extra && extra.textfieldSideEffectVersion
            ? extra.textfieldSideEffectVersion
            : (resource.textfieldSideEffectVersion || ''),
          textfieldGraphParityVersion: extra && extra.textfieldGraphParityVersion
            ? extra.textfieldGraphParityVersion
            : (resource.textfieldGraphParityVersion || ''),
          graphProbe: extra && extra.graphProbe ? extra.graphProbe : (resource.graphProbe || ''),
          graphEntryArtifact: extra && extra.graphEntryArtifact
            ? extra.graphEntryArtifact
            : (resource.graphEntryArtifact || ''),
          classExportArtifact: extra && extra.classExportArtifact
            ? extra.classExportArtifact
            : (resource.classExportArtifact || ''),
          transitiveArtifacts: extra && extra.transitiveArtifacts
            ? extra.transitiveArtifacts
            : (Array.isArray(resource.transitiveImports) ? resource.transitiveImports.join(',') : ''),
          importResolutionStrategy: extra && extra.importResolutionStrategy
            ? extra.importResolutionStrategy
            : (resource.importResolutionStrategy || ''),
          sideEffectStrategy: extra && extra.sideEffectStrategy
            ? extra.sideEffectStrategy
            : (resource.sideEffectStrategy || ''),
          moduleIdentityStrategy: extra && extra.moduleIdentityStrategy
            ? extra.moduleIdentityStrategy
            : (resource.moduleIdentityStrategy || ''),
          failureLayer: extra && extra.failureLayer ? extra.failureLayer : (resource.failureLayer || ''),
          blockedReason: extra && extra.blockedReason
            ? extra.blockedReason
            : (resource.blockedReason || ''),
          browserError: extra && extra.browserError ? extra.browserError : '',
          minimalReproduction: extra && extra.minimalReproduction
            ? extra.minimalReproduction
            : (resource.minimalReproduction || ''),
          nextResolutionStep: extra && extra.nextResolutionStep
            ? extra.nextResolutionStep
            : (resource.nextResolutionStep || ''),
          customElementTag: extra && extra.customElementTag ? extra.customElementTag : '',
          customElementDefinedBefore: extra && typeof extra.customElementDefinedBefore === 'boolean'
            ? extra.customElementDefinedBefore
            : false,
          customElementDefinedAfter: extra && typeof extra.customElementDefinedAfter === 'boolean'
            ? extra.customElementDefinedAfter
            : false,
          customElementRegistryBefore: extra && extra.customElementRegistryBefore
            ? extra.customElementRegistryBefore
            : '',
          customElementRegistryAfter: extra && extra.customElementRegistryAfter
            ? extra.customElementRegistryAfter
            : '',
          firstDefinitionSource: extra && extra.firstDefinitionSource ? extra.firstDefinitionSource : '',
          secondDefinitionSource: extra && extra.secondDefinitionSource ? extra.secondDefinitionSource : '',
          duplicateTag: extra && extra.duplicateTag ? extra.duplicateTag : (resource.duplicateTag || ''),
          remainingBlockerClassification: extra && extra.remainingBlockerClassification
            ? extra.remainingBlockerClassification
            : (resource.remainingBlockerClassification || ''),
          generatedLazyResolverVersion: extra && extra.generatedLazyResolverVersion
            ? extra.generatedLazyResolverVersion
            : (resource.generatedLazyResolverVersion || ''),
          generatedLazyExecutionVersion: extra && extra.generatedLazyExecutionVersion
            ? extra.generatedLazyExecutionVersion
            : (resource.generatedLazyExecutionVersion || ''),
          generatedLazyImportResolutionStrategy: extra && extra.generatedLazyImportResolutionStrategy
            ? extra.generatedLazyImportResolutionStrategy
            : (resource.generatedLazyImportResolutionStrategy || ''),
          generatedLazyLoadOnDemandContractVersion: extra && extra.generatedLazyLoadOnDemandContractVersion
            ? extra.generatedLazyLoadOnDemandContractVersion
            : (resource.generatedLazyLoadOnDemandContractVersion || ''),
          generatedLazyTriggerSource: extra && extra.generatedLazyTriggerSource
            ? extra.generatedLazyTriggerSource
            : (resource.generatedLazyTriggerSource || ''),
          generatedLazyApiOwner: extra && extra.generatedLazyApiOwner
            ? extra.generatedLazyApiOwner
            : (resource.generatedLazyApiOwner || ''),
          generatedLazyApiInstallSource: extra && extra.generatedLazyApiInstallSource
            ? extra.generatedLazyApiInstallSource
            : (resource.generatedLazyApiInstallSource || ''),
          generatedLazyRequestedChunk: extra && extra.generatedLazyRequestedChunk
            ? extra.generatedLazyRequestedChunk
            : (resource.generatedLazyRequestedChunk || ''),
          generatedLazyRegistrySource: extra && extra.generatedLazyRegistrySource
            ? extra.generatedLazyRegistrySource
            : (resource.generatedLazyRegistrySource || ''),
          generatedLazyMappingSource: extra && extra.generatedLazyMappingSource
            ? extra.generatedLazyMappingSource
            : (resource.generatedLazyMappingSource || ''),
          generatedLazyResolvedArtifactPath: extra && extra.generatedLazyResolvedArtifactPath
            ? extra.generatedLazyResolvedArtifactPath
            : (resource.generatedLazyResolvedArtifactPath || ''),
          generatedLazyFlowClientTriggered: extra
            && typeof extra.generatedLazyFlowClientTriggered === 'boolean'
            ? extra.generatedLazyFlowClientTriggered
            : resource.generatedLazyFlowClientTriggered === true,
          generatedLazyDiagnosticBridge: extra
            && typeof extra.generatedLazyDiagnosticBridge === 'boolean'
            ? extra.generatedLazyDiagnosticBridge
            : resource.generatedLazyDiagnosticBridge === true,
          generatedLazyRelativeImportSpecifier: extra && extra.generatedLazyRelativeImportSpecifier
            ? extra.generatedLazyRelativeImportSpecifier
            : (resource.generatedLazyRelativeImportSpecifier || ''),
          generatedLazyResolvedDependencyPath: extra && extra.generatedLazyResolvedDependencyPath
            ? extra.generatedLazyResolvedDependencyPath
            : (resource.generatedLazyResolvedDependencyPath || ''),
          generatedLazyRelativeDependencyResolved: extra
            && typeof extra.generatedLazyRelativeDependencyResolved === 'boolean'
            ? extra.generatedLazyRelativeDependencyResolved
            : false,
          generatedLazyGraphProbe: extra && extra.generatedLazyGraphProbe
            ? extra.generatedLazyGraphProbe
            : (resource.generatedLazyGraphProbe || ''),
          generatedLazyBlockerVersion: extra && extra.generatedLazyBlockerVersion
            ? extra.generatedLazyBlockerVersion
            : (resource.generatedLazyBlockerVersion || ''),
          generatedLazyChunkExpressionSource: extra && extra.generatedLazyChunkExpressionSource
            ? extra.generatedLazyChunkExpressionSource
            : (resource.generatedLazyChunkExpressionSource || ''),
          generatedLazyChunkExpression: extra && extra.generatedLazyChunkExpression
            ? extra.generatedLazyChunkExpression
            : '',
          generatedLazyChunkId: extra && extra.generatedLazyChunkId ? extra.generatedLazyChunkId : '',
          generatedLazyChunkIds: extra && extra.generatedLazyChunkIds ? extra.generatedLazyChunkIds : '',
          generatedLazyChunkTriggerOwner: extra && extra.generatedLazyChunkTriggerOwner
            ? extra.generatedLazyChunkTriggerOwner
            : (resource.generatedLazyChunkTriggerOwner || ''),
          generatedLazyChunkTriggeredByFlowClient: extra
            && typeof extra.generatedLazyChunkTriggeredByFlowClient === 'boolean'
            ? extra.generatedLazyChunkTriggeredByFlowClient
            : false,
          generatedLazyChunkDiagnosticBridgeUsed: extra
            && typeof extra.generatedLazyChunkDiagnosticBridgeUsed === 'boolean'
            ? extra.generatedLazyChunkDiagnosticBridgeUsed
            : resource.generatedLazyChunkDiagnosticBridgeUsed === true,
          generatedLazyChunkDirectImportProbeUsed: extra
            && typeof extra.generatedLazyChunkDirectImportProbeUsed === 'boolean'
            ? extra.generatedLazyChunkDirectImportProbeUsed
            : resource.generatedLazyChunkDirectImportProbeUsed === true,
          generatedLazyChunkResolvedArtifactPath: extra && extra.generatedLazyChunkResolvedArtifactPath
            ? extra.generatedLazyChunkResolvedArtifactPath
            : (resource.generatedLazyChunkResolvedArtifactPath || ''),
          generatedLazyChunkFailureLayer: extra && extra.generatedLazyChunkFailureLayer
            ? extra.generatedLazyChunkFailureLayer
            : (resource.generatedLazyChunkFailureLayer || ''),
          generatedLazyChunkBlockerVersion: extra && extra.generatedLazyChunkBlockerVersion
            ? extra.generatedLazyChunkBlockerVersion
            : (resource.generatedLazyChunkBlockerVersion || ''),
          generatedLazyChunkNextResolutionStep: extra && extra.generatedLazyChunkNextResolutionStep
            ? extra.generatedLazyChunkNextResolutionStep
            : (resource.generatedLazyChunkNextResolutionStep || ''),
          loadOnDemandChunkContractVersion: extra && extra.loadOnDemandChunkContractVersion
            ? extra.loadOnDemandChunkContractVersion
            : (resource.loadOnDemandChunkContractVersion || ''),
          loadOnDemandChunkApiOwner: extra && extra.loadOnDemandChunkApiOwner
            ? extra.loadOnDemandChunkApiOwner
            : (resource.loadOnDemandChunkApiOwner || ''),
          loadOnDemandChunkApiInstallSource: extra && extra.loadOnDemandChunkApiInstallSource
            ? extra.loadOnDemandChunkApiInstallSource
            : (resource.loadOnDemandChunkApiInstallSource || ''),
          loadOnDemandChunkRegistrySource: extra && extra.loadOnDemandChunkRegistrySource
            ? extra.loadOnDemandChunkRegistrySource
            : (resource.loadOnDemandChunkRegistrySource || ''),
          loadOnDemandChunkMappingSource: extra && extra.loadOnDemandChunkMappingSource
            ? extra.loadOnDemandChunkMappingSource
            : (resource.loadOnDemandChunkMappingSource || ''),
          loadOnDemandChunkRequestedChunk: extra && extra.loadOnDemandChunkRequestedChunk
            ? extra.loadOnDemandChunkRequestedChunk
            : '',
          loadOnDemandChunkResolvedArtifactPath: extra && extra.loadOnDemandChunkResolvedArtifactPath
            ? extra.loadOnDemandChunkResolvedArtifactPath
            : (resource.loadOnDemandChunkResolvedArtifactPath || ''),
          loadOnDemandChunkModuleIdentity: extra && extra.loadOnDemandChunkModuleIdentity
            ? extra.loadOnDemandChunkModuleIdentity
            : (resource.loadOnDemandChunkModuleIdentity || ''),
          loadOnDemandChunkTriggeredByFlowClient: extra
            && typeof extra.loadOnDemandChunkTriggeredByFlowClient === 'boolean'
            ? extra.loadOnDemandChunkTriggeredByFlowClient
            : false,
          loadOnDemandChunkExecutionResult: extra && extra.loadOnDemandChunkExecutionResult
            ? extra.loadOnDemandChunkExecutionResult
            : '',
          loadOnDemandChunkFailureLayer: extra && extra.loadOnDemandChunkFailureLayer
            ? extra.loadOnDemandChunkFailureLayer
            : (resource.loadOnDemandChunkFailureLayer || ''),
          loadOnDemandChunkBrowserError: extra && extra.loadOnDemandChunkBrowserError
            ? extra.loadOnDemandChunkBrowserError
            : '',
          loadOnDemandChunkBlockerVersion: extra && extra.loadOnDemandChunkBlockerVersion
            ? extra.loadOnDemandChunkBlockerVersion
            : (resource.loadOnDemandChunkBlockerVersion || ''),
          loadOnDemandChunkNextResolutionStep: extra && extra.loadOnDemandChunkNextResolutionStep
            ? extra.loadOnDemandChunkNextResolutionStep
            : (resource.loadOnDemandChunkNextResolutionStep || ''),
          generatedLoadOnDemandChunkFileRegistryVersion: extra && extra.generatedLoadOnDemandChunkFileRegistryVersion
            ? extra.generatedLoadOnDemandChunkFileRegistryVersion
            : (resource.generatedLoadOnDemandChunkFileRegistryVersion || ''),
          generatedLoadOnDemandChunkFileRegistrySource: extra && extra.generatedLoadOnDemandChunkFileRegistrySource
            ? extra.generatedLoadOnDemandChunkFileRegistrySource
            : (resource.generatedLoadOnDemandChunkFileRegistrySource || ''),
          generatedLoadOnDemandChunkFileMappingSource: extra && extra.generatedLoadOnDemandChunkFileMappingSource
            ? extra.generatedLoadOnDemandChunkFileMappingSource
            : (resource.generatedLoadOnDemandChunkFileMappingSource || ''),
          generatedLoadOnDemandGeneratedImportsArtifact: extra && extra.generatedLoadOnDemandGeneratedImportsArtifact
            ? extra.generatedLoadOnDemandGeneratedImportsArtifact
            : (resource.generatedLoadOnDemandGeneratedImportsArtifact || ''),
          generatedLoadOnDemandSourceBuildMode: extra && extra.generatedLoadOnDemandSourceBuildMode
            ? extra.generatedLoadOnDemandSourceBuildMode
            : (resource.generatedLoadOnDemandSourceBuildMode || ''),
          generatedLoadOnDemandSourceArtifactFamily: extra && extra.generatedLoadOnDemandSourceArtifactFamily
            ? extra.generatedLoadOnDemandSourceArtifactFamily
            : (resource.generatedLoadOnDemandSourceArtifactFamily || ''),
          generatedLoadOnDemandNoOpLoadOnDemand: extra
            && typeof extra.generatedLoadOnDemandNoOpLoadOnDemand === 'boolean'
            ? extra.generatedLoadOnDemandNoOpLoadOnDemand
            : resource.generatedLoadOnDemandNoOpLoadOnDemand === true,
          loadOnDemandTableShape: extra && extra.loadOnDemandTableShape
            ? extra.loadOnDemandTableShape
            : (resource.loadOnDemandTableShape || ''),
          chunkId: extra && extra.chunkId ? extra.chunkId : (resource.chunkId || ''),
          chunkImportSpecifier: extra && extra.chunkImportSpecifier
            ? extra.chunkImportSpecifier
            : (resource.chunkImportSpecifier || ''),
          chunkArtifactPath: extra && extra.chunkArtifactPath ? extra.chunkArtifactPath : (resource.chunkArtifactPath || ''),
          chunkContentHash: extra && extra.chunkContentHash ? extra.chunkContentHash : (resource.chunkContentHash || ''),
          chunkFileResolver: extra && extra.chunkFileResolver ? extra.chunkFileResolver : (resource.chunkFileResolver || ''),
          chunkFileStatus: extra && extra.chunkFileStatus ? extra.chunkFileStatus : (resource.chunkFileStatus || ''),
          chunkFileBytes: extra && Number.isFinite(extra.chunkFileBytes)
            ? extra.chunkFileBytes
            : (Number.isFinite(resource.chunkFileBytes) ? resource.chunkFileBytes : 0),
          chunkFileSha256: extra && extra.chunkFileSha256 ? extra.chunkFileSha256 : (resource.chunkFileSha256 || ''),
          noChunkGeneratedReason: extra && extra.noChunkGeneratedReason
            ? extra.noChunkGeneratedReason
            : (resource.noChunkGeneratedReason || ''),
          loadOnDemandChunkFileRegistryVersion: extra && extra.loadOnDemandChunkFileRegistryVersion
            ? extra.loadOnDemandChunkFileRegistryVersion
            : (resource.loadOnDemandChunkFileRegistryVersion || ''),
          loadOnDemandChunkFileRegistrySource: extra && extra.loadOnDemandChunkFileRegistrySource
            ? extra.loadOnDemandChunkFileRegistrySource
            : (resource.loadOnDemandChunkFileRegistrySource || ''),
          loadOnDemandChunkFileMappingSource: extra && extra.loadOnDemandChunkFileMappingSource
            ? extra.loadOnDemandChunkFileMappingSource
            : (resource.loadOnDemandChunkFileMappingSource || ''),
          loadOnDemandChunkFileGeneratedImportsArtifact: extra && extra.loadOnDemandChunkFileGeneratedImportsArtifact
            ? extra.loadOnDemandChunkFileGeneratedImportsArtifact
            : (resource.loadOnDemandChunkFileGeneratedImportsArtifact || ''),
          loadOnDemandChunkFileNoOpLoadOnDemand: extra
            && typeof extra.loadOnDemandChunkFileNoOpLoadOnDemand === 'boolean'
            ? extra.loadOnDemandChunkFileNoOpLoadOnDemand
            : resource.loadOnDemandChunkFileNoOpLoadOnDemand === true,
          loadOnDemandChunkFileTableShape: extra && extra.loadOnDemandChunkFileTableShape
            ? extra.loadOnDemandChunkFileTableShape
            : (resource.loadOnDemandChunkFileTableShape || ''),
          loadOnDemandChunkFileResolver: extra && extra.loadOnDemandChunkFileResolver
            ? extra.loadOnDemandChunkFileResolver
            : (resource.loadOnDemandChunkFileResolver || ''),
          loadOnDemandChunkFileStatus: extra && extra.loadOnDemandChunkFileStatus
            ? extra.loadOnDemandChunkFileStatus
            : (resource.loadOnDemandChunkFileStatus || ''),
          loadOnDemandChunkFileBytes: extra && Number.isFinite(extra.loadOnDemandChunkFileBytes)
            ? extra.loadOnDemandChunkFileBytes
            : (Number.isFinite(resource.loadOnDemandChunkFileBytes) ? resource.loadOnDemandChunkFileBytes : 0),
          loadOnDemandChunkFileSha256: extra && extra.loadOnDemandChunkFileSha256
            ? extra.loadOnDemandChunkFileSha256
            : (resource.loadOnDemandChunkFileSha256 || ''),
          loadOnDemandChunkFileTriggeredByFlowClient: extra
            && typeof extra.loadOnDemandChunkFileTriggeredByFlowClient === 'boolean'
            ? extra.loadOnDemandChunkFileTriggeredByFlowClient
            : false,
          loadOnDemandChunkFileStageObserved: extra
            && typeof extra.loadOnDemandChunkFileStageObserved === 'boolean'
            ? extra.loadOnDemandChunkFileStageObserved
            : false,
          loadOnDemandChunkFileExecuted: extra && typeof extra.loadOnDemandChunkFileExecuted === 'boolean'
            ? extra.loadOnDemandChunkFileExecuted
            : false,
          loadOnDemandChunkFileCallbackResolved: extra
            && typeof extra.loadOnDemandChunkFileCallbackResolved === 'boolean'
            ? extra.loadOnDemandChunkFileCallbackResolved
            : false,
          loadOnDemandChunkFileExecutionResult: extra && extra.loadOnDemandChunkFileExecutionResult
            ? extra.loadOnDemandChunkFileExecutionResult
            : '',
          loadOnDemandChunkFileSideEffectProof: extra && extra.loadOnDemandChunkFileSideEffectProof
            ? extra.loadOnDemandChunkFileSideEffectProof
            : (resource.loadOnDemandChunkFileSideEffectProof || ''),
          loadOnDemandChunkFileFailureLayer: extra && extra.loadOnDemandChunkFileFailureLayer
            ? extra.loadOnDemandChunkFileFailureLayer
            : (resource.loadOnDemandChunkFileFailureLayer || ''),
          loadOnDemandChunkFileBrowserError: extra && extra.loadOnDemandChunkFileBrowserError
            ? extra.loadOnDemandChunkFileBrowserError
            : '',
          loadOnDemandChunkFileBlockerVersion: extra && extra.loadOnDemandChunkFileBlockerVersion
            ? extra.loadOnDemandChunkFileBlockerVersion
            : (resource.loadOnDemandChunkFileBlockerVersion || ''),
          loadOnDemandChunkFileNextResolutionStep: extra && extra.loadOnDemandChunkFileNextResolutionStep
            ? extra.loadOnDemandChunkFileNextResolutionStep
            : (resource.loadOnDemandChunkFileNextResolutionStep || ''),
          generatedLazySourceArtifact: extra && extra.generatedLazySourceArtifact
            ? extra.generatedLazySourceArtifact
            : '',
          runtimeResolverContractVersion: extra && extra.runtimeResolverContractVersion
            ? extra.runtimeResolverContractVersion
            : (resource.runtimeResolverContractVersion || ''),
          logicalRequestId: extra && extra.logicalRequestId ? extra.logicalRequestId : (resource.logicalRequestId || ''),
          pathMapping: extra && extra.pathMapping ? extra.pathMapping : (resource.pathMapping || ''),
          stageUrlPolicy: extra && extra.stageUrlPolicy ? extra.stageUrlPolicy : (resource.stageUrlPolicy || ''),
          cachePolicy: extra && extra.cachePolicy ? extra.cachePolicy : (resource.cachePolicy || ''),
          cleanupOwnership: extra && extra.cleanupOwnership ? extra.cleanupOwnership : (resource.cleanupOwnership || ''),
          blockerClassification: extra && extra.blockerClassification
            ? extra.blockerClassification
            : (resource.blockerClassification || ''),
          flowLazyDependencyResource: resource.flowLazyDependencyResource === true,
          lazyDependencyVersion: extra && extra.lazyDependencyVersion
            ? extra.lazyDependencyVersion
            : (resource.lazyDependencyVersion || ''),
          lazyDependencySource: extra && extra.lazyDependencySource
            ? extra.lazyDependencySource
            : (resource.lazyDependencySource || ''),
          lazyDependencyMode: extra && extra.lazyDependencyMode
            ? extra.lazyDependencyMode
            : (resource.lazyDependencyMode || ''),
          lazyDependencyExpression: extra && extra.lazyDependencyExpression
            ? extra.lazyDependencyExpression
            : (resource.lazyDependencyExpression || ''),
          lazyDependencyChunkId: extra && extra.lazyDependencyChunkId
            ? extra.lazyDependencyChunkId
            : (resource.lazyDependencyChunkId || ''),
          lazyDependencyResolvedPath: extra && extra.lazyDependencyResolvedPath
            ? extra.lazyDependencyResolvedPath
            : (resource.lazyDependencyResolvedPath || ''),
          lazyDependencyTriggerOwner: extra && extra.lazyDependencyTriggerOwner
            ? extra.lazyDependencyTriggerOwner
            : (resource.lazyDependencyTriggerOwner || ''),
          lazyDependencyTriggeredByFlowClient: extra
            && typeof extra.lazyDependencyTriggeredByFlowClient === 'boolean'
            ? extra.lazyDependencyTriggeredByFlowClient
            : resource.lazyDependencyTriggeredByFlowClient === true,
          productionBundleRuntimeArtifact: resource.productionBundleRuntimeArtifact === true,
          productionBundleRuntimeVersion: extra && extra.productionBundleRuntimeVersion
            ? extra.productionBundleRuntimeVersion
            : (resource.productionBundleRuntimeVersion || ''),
          productionBundleRuntimeRegistrySource: extra && extra.productionBundleRuntimeRegistrySource
            ? extra.productionBundleRuntimeRegistrySource
            : (resource.productionBundleRuntimeRegistrySource || ''),
          productionBundleRuntimeStatsSource: extra && extra.productionBundleRuntimeStatsSource
            ? extra.productionBundleRuntimeStatsSource
            : (resource.productionBundleRuntimeStatsSource || ''),
          productionBundleRuntimeSelectedArtifact: extra && extra.productionBundleRuntimeSelectedArtifact
            ? extra.productionBundleRuntimeSelectedArtifact
            : (resource.productionBundleRuntimeSelectedArtifact || ''),
          productionBundleRuntimeDependencyChain: extra && extra.productionBundleRuntimeDependencyChain
            ? extra.productionBundleRuntimeDependencyChain
            : (resource.productionBundleRuntimeDependencyChain || ''),
          productionBundleRuntimeModuleIdentity: extra && extra.productionBundleRuntimeModuleIdentity
            ? extra.productionBundleRuntimeModuleIdentity
            : (resource.productionBundleRuntimeModuleIdentity || ''),
          productionBundleRuntimeExecutionPolicy: extra && extra.productionBundleRuntimeExecutionPolicy
            ? extra.productionBundleRuntimeExecutionPolicy
            : (resource.productionBundleRuntimeExecutionPolicy || ''),
          productionBundleRuntimeBlockerVersion: extra && extra.productionBundleRuntimeBlockerVersion
            ? extra.productionBundleRuntimeBlockerVersion
            : (resource.productionBundleRuntimeBlockerVersion || ''),
          productionBundleRuntimeFailureLayer: extra && extra.productionBundleRuntimeFailureLayer
            ? extra.productionBundleRuntimeFailureLayer
            : (resource.productionBundleRuntimeFailureLayer || ''),
          productionBundleRuntimeNextResolutionStep: extra && extra.productionBundleRuntimeNextResolutionStep
            ? extra.productionBundleRuntimeNextResolutionStep
            : (resource.productionBundleRuntimeNextResolutionStep || ''),
          productionBundleRuntimeOwnerVersion: extra && extra.productionBundleRuntimeOwnerVersion
            ? extra.productionBundleRuntimeOwnerVersion
            : (resource.productionBundleRuntimeOwnerVersion || ''),
          productionBundleRuntimeOwner: extra && extra.productionBundleRuntimeOwner
            ? extra.productionBundleRuntimeOwner
            : (resource.productionBundleRuntimeOwner || ''),
          productionBundleRuntimeEntryArtifact: extra && extra.productionBundleRuntimeEntryArtifact
            ? extra.productionBundleRuntimeEntryArtifact
            : (resource.productionBundleRuntimeEntryArtifact || ''),
          productionBundleRuntimeSource: extra && extra.productionBundleRuntimeSource
            ? extra.productionBundleRuntimeSource
            : (resource.productionBundleRuntimeSource || ''),
          productionBundleRuntimeSelectedBy: extra && extra.productionBundleRuntimeSelectedBy
            ? extra.productionBundleRuntimeSelectedBy
            : (resource.productionBundleRuntimeSelectedBy || ''),
          productionBundleRuntimeOwnerModuleIdentity: extra && extra.productionBundleRuntimeOwnerModuleIdentity
            ? extra.productionBundleRuntimeOwnerModuleIdentity
            : (resource.productionBundleRuntimeOwnerModuleIdentity || ''),
          productionBundleRuntimeOwnerLifecycle: extra && extra.productionBundleRuntimeOwnerLifecycle
            ? extra.productionBundleRuntimeOwnerLifecycle
            : (resource.productionBundleRuntimeOwnerLifecycle || ''),
          productionBundleRuntimeOwnerExecutionPolicy: extra && extra.productionBundleRuntimeOwnerExecutionPolicy
            ? extra.productionBundleRuntimeOwnerExecutionPolicy
            : (resource.productionBundleRuntimeOwnerExecutionPolicy || ''),
          productionBundleRuntimeOwnerStageUrlPolicy: extra && extra.productionBundleRuntimeOwnerStageUrlPolicy
            ? extra.productionBundleRuntimeOwnerStageUrlPolicy
            : (resource.productionBundleRuntimeOwnerStageUrlPolicy || ''),
          productionBundleRuntimeOwnerCachePolicy: extra && extra.productionBundleRuntimeOwnerCachePolicy
            ? extra.productionBundleRuntimeOwnerCachePolicy
            : (resource.productionBundleRuntimeOwnerCachePolicy || ''),
          productionBundleRuntimeOwnerCacheStatus: extra && extra.productionBundleRuntimeOwnerCacheStatus
            ? extra.productionBundleRuntimeOwnerCacheStatus
            : (cacheStatus || ''),
          productionBundleRuntimeOwnerCleanupStrategy: extra && extra.productionBundleRuntimeOwnerCleanupStrategy
            ? extra.productionBundleRuntimeOwnerCleanupStrategy
            : (resource.productionBundleRuntimeOwnerCleanupStrategy || ''),
          productionBundleRuntimeOwnerExecutionResult: extra && extra.productionBundleRuntimeOwnerExecutionResult
            ? extra.productionBundleRuntimeOwnerExecutionResult
            : '',
          productionBundleRuntimeOwnerSideEffectProof: extra && extra.productionBundleRuntimeOwnerSideEffectProof
            ? extra.productionBundleRuntimeOwnerSideEffectProof
            : (resource.productionBundleRuntimeOwnerSideEffectProof || ''),
          productionBundleRuntimeOwnerFailureLayer: extra && extra.productionBundleRuntimeOwnerFailureLayer
            ? extra.productionBundleRuntimeOwnerFailureLayer
            : (resource.productionBundleRuntimeOwnerFailureLayer || ''),
          productionBundleRuntimeOwnerBrowserError: extra && extra.productionBundleRuntimeOwnerBrowserError
            ? extra.productionBundleRuntimeOwnerBrowserError
            : (resource.productionBundleRuntimeOwnerBrowserError || ''),
          productionBundleRuntimeOwnerBlockerVersion: extra && extra.productionBundleRuntimeOwnerBlockerVersion
            ? extra.productionBundleRuntimeOwnerBlockerVersion
            : (resource.productionBundleRuntimeOwnerBlockerVersion || ''),
          productionBundleRuntimeOwnerNextResolutionStep: extra && extra.productionBundleRuntimeOwnerNextResolutionStep
            ? extra.productionBundleRuntimeOwnerNextResolutionStep
            : (resource.productionBundleRuntimeOwnerNextResolutionStep || ''),
          productionBundleRuntimeOwnerRegistryBefore: extra && extra.productionBundleRuntimeOwnerRegistryBefore
            ? extra.productionBundleRuntimeOwnerRegistryBefore
            : '',
          productionBundleRuntimeOwnerRegistryAfter: extra && extra.productionBundleRuntimeOwnerRegistryAfter
            ? extra.productionBundleRuntimeOwnerRegistryAfter
            : '',
          productionBundleRuntimeOwnerDuplicateTag: extra && extra.productionBundleRuntimeOwnerDuplicateTag
            ? extra.productionBundleRuntimeOwnerDuplicateTag
            : '',
          productionBundleRuntimeOwnerFirstDefinitionOwner: extra && extra.productionBundleRuntimeOwnerFirstDefinitionOwner
            ? extra.productionBundleRuntimeOwnerFirstDefinitionOwner
            : '',
          productionBundleRuntimeOwnerSecondDefinitionOwner: extra && extra.productionBundleRuntimeOwnerSecondDefinitionOwner
            ? extra.productionBundleRuntimeOwnerSecondDefinitionOwner
            : '',
          productionBundleRuntimeOwnerImportVersion: extra && extra.productionBundleRuntimeOwnerImportVersion
            ? extra.productionBundleRuntimeOwnerImportVersion
            : (resource.productionBundleRuntimeOwnerImportVersion || ''),
          productionBundleRuntimeOwnerImportPolicy: extra && extra.productionBundleRuntimeOwnerImportPolicy
            ? extra.productionBundleRuntimeOwnerImportPolicy
            : (resource.productionBundleRuntimeOwnerImportPolicy || ''),
          productionBundleRuntimeOwnerImportArtifact: extra && extra.productionBundleRuntimeOwnerImportArtifact
            ? extra.productionBundleRuntimeOwnerImportArtifact
            : (resource.productionBundleRuntimeOwnerImportArtifact || ''),
          productionBundleRuntimeOwnerImportPhase: extra && extra.productionBundleRuntimeOwnerImportPhase
            ? extra.productionBundleRuntimeOwnerImportPhase
            : (resource.productionBundleRuntimeOwnerImportPhase || ''),
          productionBundleRuntimeOwnerImportPhaseCandidates:
            extra && extra.productionBundleRuntimeOwnerImportPhaseCandidates
              ? extra.productionBundleRuntimeOwnerImportPhaseCandidates
              : (resource.productionBundleRuntimeOwnerImportPhaseCandidates || ''),
          productionBundleRuntimeOwnerImportDiagnosticModes:
            extra && extra.productionBundleRuntimeOwnerImportDiagnosticModes
              ? extra.productionBundleRuntimeOwnerImportDiagnosticModes
              : (resource.productionBundleRuntimeOwnerImportDiagnosticModes || ''),
          productionBundleRuntimeOwnerImportExpectedSideEffects:
            extra && extra.productionBundleRuntimeOwnerImportExpectedSideEffects
              ? extra.productionBundleRuntimeOwnerImportExpectedSideEffects
              : (resource.productionBundleRuntimeOwnerImportExpectedSideEffects || ''),
          productionBundleRuntimeOwnerImportAcceptableFailures:
            extra && extra.productionBundleRuntimeOwnerImportAcceptableFailures
              ? extra.productionBundleRuntimeOwnerImportAcceptableFailures
              : (resource.productionBundleRuntimeOwnerImportAcceptableFailures || ''),
          productionBundleRuntimeOwnerImportRewriteStrategy:
            extra && extra.productionBundleRuntimeOwnerImportRewriteStrategy
              ? extra.productionBundleRuntimeOwnerImportRewriteStrategy
              : (resource.productionBundleRuntimeOwnerImportRewriteStrategy || ''),
          productionBundleRuntimeOwnerImportDependencyPaths:
            extra && extra.productionBundleRuntimeOwnerImportDependencyPaths
              ? extra.productionBundleRuntimeOwnerImportDependencyPaths
              : (resource.productionBundleRuntimeOwnerImportDependencyPaths || ''),
          productionBundleRuntimeOwnerImportResolvedDependencyPaths:
            extra && extra.productionBundleRuntimeOwnerImportResolvedDependencyPaths
              ? extra.productionBundleRuntimeOwnerImportResolvedDependencyPaths
              : '',
          productionBundleRuntimeOwnerImportUnresolvedSpecifiers:
            extra && extra.productionBundleRuntimeOwnerImportUnresolvedSpecifiers
              ? extra.productionBundleRuntimeOwnerImportUnresolvedSpecifiers
              : '',
          productionBundleRuntimeOwnerImportDependencyRewriteResult:
            extra && extra.productionBundleRuntimeOwnerImportDependencyRewriteResult
              ? extra.productionBundleRuntimeOwnerImportDependencyRewriteResult
              : '',
          productionBundleRuntimeOwnerDependencyResolverVersion:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverVersion
              ? extra.productionBundleRuntimeOwnerDependencyResolverVersion
              : (resource.productionBundleRuntimeOwnerDependencyResolverVersion || ''),
          productionBundleRuntimeOwnerDependencyResolverRoot:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverRoot
              ? extra.productionBundleRuntimeOwnerDependencyResolverRoot
              : (resource.productionBundleRuntimeOwnerDependencyResolverRoot || ''),
          productionBundleRuntimeOwnerDependencyResolverPolicy:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverPolicy
              ? extra.productionBundleRuntimeOwnerDependencyResolverPolicy
              : (resource.productionBundleRuntimeOwnerDependencyResolverPolicy || ''),
          productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
              ? extra.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
              : (resource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts || ''),
          productionBundleRuntimeOwnerDependencyResolverAcceptableFailures:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
              ? extra.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
              : (resource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures || ''),
          productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
              ? extra.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
              : (resource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace || ''),
          productionBundleRuntimeOwnerDependencyResolverRootArtifact:
            extra && extra.productionBundleRuntimeOwnerDependencyResolverRootArtifact
              ? extra.productionBundleRuntimeOwnerDependencyResolverRootArtifact
              : (resource.productionBundleRuntimeOwnerDependencyResolverRootArtifact || ''),
          productionBundleRuntimeOwnerDependencyGraphClosed: extra
            && typeof extra.productionBundleRuntimeOwnerDependencyGraphClosed === 'boolean'
              ? extra.productionBundleRuntimeOwnerDependencyGraphClosed
              : false,
          productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount: extra
            && Number.isFinite(extra.productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount)
              ? extra.productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount
              : 0,
          productionBundleRuntimeOwnerDependencyGraphResolvedCount: extra
            && Number.isFinite(extra.productionBundleRuntimeOwnerDependencyGraphResolvedCount)
              ? extra.productionBundleRuntimeOwnerDependencyGraphResolvedCount
              : 0,
          productionBundleRuntimeOwnerDependencyGraphMissingCount: extra
            && Number.isFinite(extra.productionBundleRuntimeOwnerDependencyGraphMissingCount)
              ? extra.productionBundleRuntimeOwnerDependencyGraphMissingCount
              : 0,
          productionBundleRuntimeOwnerDependencyGraphStagedCount: extra
            && Number.isFinite(extra.productionBundleRuntimeOwnerDependencyGraphStagedCount)
              ? extra.productionBundleRuntimeOwnerDependencyGraphStagedCount
              : 0,
          productionBundleRuntimeOwnerDependencyGraphMaxDepth: extra
            && Number.isFinite(extra.productionBundleRuntimeOwnerDependencyGraphMaxDepth)
              ? extra.productionBundleRuntimeOwnerDependencyGraphMaxDepth
              : 0,
          productionBundleRuntimeOwnerDependencyClassifications:
            extra && extra.productionBundleRuntimeOwnerDependencyClassifications
              ? extra.productionBundleRuntimeOwnerDependencyClassifications
              : '',
          productionBundleRuntimeOwnerImportMissingDependencyPaths:
            extra && extra.productionBundleRuntimeOwnerImportMissingDependencyPaths
              ? extra.productionBundleRuntimeOwnerImportMissingDependencyPaths
              : '',
          productionBundleRuntimeOwnerImportMissingSpecifiers:
            extra && extra.productionBundleRuntimeOwnerImportMissingSpecifiers
              ? extra.productionBundleRuntimeOwnerImportMissingSpecifiers
              : '',
          productionBundleRuntimeOwnerImportStageCount: extra
            && Number.isFinite(extra.productionBundleRuntimeOwnerImportStageCount)
              ? extra.productionBundleRuntimeOwnerImportStageCount
              : 0,
          productionBundleRuntimeOwnerImportModuleIdentity:
            extra && extra.productionBundleRuntimeOwnerImportModuleIdentity
              ? extra.productionBundleRuntimeOwnerImportModuleIdentity
              : '',
          productionBundleRuntimeOwnerImportVaadinGlobalBefore:
            extra && extra.productionBundleRuntimeOwnerImportVaadinGlobalBefore
              ? extra.productionBundleRuntimeOwnerImportVaadinGlobalBefore
              : '',
          productionBundleRuntimeOwnerImportVaadinGlobalAfter:
            extra && extra.productionBundleRuntimeOwnerImportVaadinGlobalAfter
              ? extra.productionBundleRuntimeOwnerImportVaadinGlobalAfter
              : '',
          productionBundleRuntimeOwnerImportAttempted: extra
            && typeof extra.productionBundleRuntimeOwnerImportAttempted === 'boolean'
              ? extra.productionBundleRuntimeOwnerImportAttempted
              : resource.productionBundleRuntimeOwnerImportAttempted === true,
          productionBundleRuntimeOwnerImported: extra
            && typeof extra.productionBundleRuntimeOwnerImported === 'boolean'
              ? extra.productionBundleRuntimeOwnerImported
              : resource.productionBundleRuntimeOwnerImported === true,
          productionBundleRuntimeOwnerImportNextResolutionStep:
            extra && extra.productionBundleRuntimeOwnerImportNextResolutionStep
              ? extra.productionBundleRuntimeOwnerImportNextResolutionStep
              : (resource.productionBundleRuntimeOwnerImportNextResolutionStep || ''),
          productionBundleRuntimeOwnerImportBrowserErrorName:
            extra && extra.productionBundleRuntimeOwnerImportBrowserErrorName
              ? extra.productionBundleRuntimeOwnerImportBrowserErrorName
              : '',
          productionBundleRuntimeOwnerImportOrderingVersion:
            extra && extra.productionBundleRuntimeOwnerImportOrderingVersion
              ? extra.productionBundleRuntimeOwnerImportOrderingVersion
              : (resource.productionBundleRuntimeOwnerImportOrderingVersion || ''),
          productionBundleRuntimeOwnerImportPhasePolicy:
            extra && extra.productionBundleRuntimeOwnerImportPhasePolicy
              ? extra.productionBundleRuntimeOwnerImportPhasePolicy
              : (resource.productionBundleRuntimeOwnerImportPhasePolicy || ''),
          productionBundleRuntimeOwnerImportOrderingResult:
            extra && extra.productionBundleRuntimeOwnerImportOrderingResult
              ? extra.productionBundleRuntimeOwnerImportOrderingResult
              : (resource.productionBundleRuntimeOwnerImportOrderingResult || ''),
          productionBundleRuntimeOwnerImportOrderingObservedPhases:
            extra && extra.productionBundleRuntimeOwnerImportOrderingObservedPhases
              ? extra.productionBundleRuntimeOwnerImportOrderingObservedPhases
              : (resource.productionBundleRuntimeOwnerImportOrderingObservedPhases || ''),
          productionBundleRuntimeOwnerImportOrderingAcceptableFailures:
            extra && extra.productionBundleRuntimeOwnerImportOrderingAcceptableFailures
              ? extra.productionBundleRuntimeOwnerImportOrderingAcceptableFailures
              : (resource.productionBundleRuntimeOwnerImportOrderingAcceptableFailures || ''),
          componentProductionOwnerReuseVersion: extra && extra.componentProductionOwnerReuseVersion
            ? extra.componentProductionOwnerReuseVersion
            : (resource.componentProductionOwnerReuseVersion || ''),
          componentProductionOwnerReusePolicy: extra && extra.componentProductionOwnerReusePolicy
            ? extra.componentProductionOwnerReusePolicy
            : (resource.componentProductionOwnerReusePolicy || ''),
          componentProductionOwnerReuseMode: extra && extra.componentProductionOwnerReuseMode
            ? extra.componentProductionOwnerReuseMode
            : (resource.componentProductionOwnerReuseMode || ''),
          componentProductionOwnerReuseAcceptableFailures: extra
            && extra.componentProductionOwnerReuseAcceptableFailures
            ? extra.componentProductionOwnerReuseAcceptableFailures
            : (resource.componentProductionOwnerReuseAcceptableFailures || ''),
          componentProductionOwnerReused: extra
            && typeof extra.componentProductionOwnerReused === 'boolean'
            ? extra.componentProductionOwnerReused
            : resource.componentProductionOwnerReused === true,
          componentProductionOwnerReuseReason: extra && extra.componentProductionOwnerReuseReason
            ? extra.componentProductionOwnerReuseReason
            : (resource.componentProductionOwnerReuseReason || ''),
          componentProductionOwnerOwnedTags: extra && extra.componentProductionOwnerOwnedTags
            ? extra.componentProductionOwnerOwnedTags
            : (resource.componentProductionOwnerOwnedTags || ''),
          componentProductionOwnerSkippedDefinitions: extra && extra.componentProductionOwnerSkippedDefinitions
            ? extra.componentProductionOwnerSkippedDefinitions
            : (resource.componentProductionOwnerSkippedDefinitions || ''),
          componentProductionOwnerFallbackImports: extra && extra.componentProductionOwnerFallbackImports
            ? extra.componentProductionOwnerFallbackImports
            : (resource.componentProductionOwnerFallbackImports || ''),
          componentProductionOwnerDependencyPaths: extra && extra.componentProductionOwnerDependencyPaths
            ? extra.componentProductionOwnerDependencyPaths
            : (resource.componentProductionOwnerDependencyPaths || ''),
          componentProductionOwnerImportOrder: extra && extra.componentProductionOwnerImportOrder
            ? extra.componentProductionOwnerImportOrder
            : (resource.componentProductionOwnerImportOrder || ''),
          moduleIdentityKey: extra && extra.moduleIdentityKey ? extra.moduleIdentityKey : sourcePath
        });
      }

      function stageFrontendArtifact(resource, body, executionStatus, urlsToRevoke, urlCache, extra) {
        if (!resource || typeof resource.path !== 'string' || !resource.path || typeof body !== 'string') {
          return '';
        }
        if (urlCache && urlCache.has(resource.path)) {
          const cachedUrl = urlCache.get(resource.path);
          recordFrontendStage(resource, cachedUrl, executionStatus || 'staged', 'hit',
            Object.assign({ body }, extra || {}));
          return cachedUrl;
        }
        const url = URL.createObjectURL(new Blob([body], { type: 'text/javascript' }));
        frontendStageResourceByUrl.set(url, resource);
        if (Array.isArray(urlsToRevoke)) {
          urlsToRevoke.push(url);
        }
        if (urlCache) {
          urlCache.set(resource.path, url);
        }
        recordFrontendStage(resource, url, executionStatus || 'staged', 'miss',
          Object.assign({ body }, extra || {}));
        return url;
      }

      window.VaadinDesktopIpcHostResourceStageResponse = function(response) {
        const requestId = response && typeof response.requestId === 'string' ? response.requestId : '';
        const pending = requestId ? hostResourceStagePromises.get(requestId) : null;
        if (!pending) {
          return;
        }
        hostResourceStagePromises.delete(requestId);
        clearTimeout(pending.timeout);
        if (response && response.success === true) {
          pending.resolve(response);
        } else {
          pending.reject(new Error((response && response.error) || 'host-resource-stage-failed'));
        }
      };

      function requestHostResourceStage(payload) {
        return new Promise((resolve, reject) => {
          if (!window.webkit || !window.webkit.messageHandlers || !window.webkit.messageHandlers.flowIpc) {
            reject(new Error('host-resource-stage-bridge-unavailable'));
            return;
          }
          const requestId = 'host-resource-stage-' + (++hostResourceStageSequence);
          const timeout = setTimeout(() => {
            hostResourceStagePromises.delete(requestId);
            reject(new Error('host-resource-stage-timeout'));
          }, 5000);
          hostResourceStagePromises.set(requestId, { resolve, reject, timeout });
          window.webkit.messageHandlers.flowIpc.postMessage({
            type: 'hostResourceStageRequest',
            payload: Object.assign({}, payload || {}, { requestId })
          });
        });
      }

      async function stageHostOwnedFrontendArtifact(resource, body, executionStatus, extra) {
        if (!resource || typeof resource.path !== 'string' || !resource.path || typeof body !== 'string') {
          return '';
        }
        const hostExtra = extra || {};
        try {
          const response = await requestHostResourceStage({
            sourcePath: resource.path,
            body,
            sha256: resource.sha256 || '',
            artifactFamily: resource.artifactFamily || '',
            mimeType: hostExtra.mimeType || 'text/javascript; charset=utf-8',
            relativeImportBase: hostExtra.relativeImportBase || resource.path
          });
          hostExtra.frontendResourceSchemeVersion =
            response.frontendResourceSchemeVersion || 'flow-ipc-resource-scheme-v1_17';
          hostExtra.frontendResourceLoaderMode =
            response.frontendResourceLoaderMode || 'custom-scheme-file-backed';
          hostExtra.frontendResourceSchemeName = response.frontendResourceSchemeName || 'vatori-resource';
          hostExtra.frontendResourceSchemeHandlerInstalled =
            response.frontendResourceSchemeHandlerInstalled === true;
          hostExtra.frontendResourceSchemeRequestObserved =
            response.frontendResourceSchemeRequestObserved === true;
          hostExtra.frontendResourceSchemeResponseObserved =
            response.frontendResourceSchemeResponseObserved === true;
          hostExtra.frontendResourceLoaderNoLocalhost =
            response.frontendResourceLoaderNoLocalhost === true;
          hostExtra.frontendResourceFileBacked = response.frontendResourceFileBacked === true;
          hostExtra.stagingMode = response.stagingMode || 'custom-scheme';
          hostExtra.resourceBaseUrl = response.resourceBaseUrl || response.stagedUrl || '';
          hostExtra.relativeImportBase = response.relativeImportBase || resource.path;
          hostExtra.relativeImportResolutionMode =
            response.relativeImportResolutionMode || 'scheme-url-native-relative-base-v1_17';
          hostExtra.resolvedRelativeImportPaths = response.resolvedRelativeImportPaths || '';
          hostExtra.missingRelativeImportPaths = response.missingRelativeImportPaths || '';
          hostExtra.mimeType = response.mimeType || 'text/javascript; charset=utf-8';
          hostExtra.filePathRedacted = response.filePathRedacted || '';
          hostExtra.fileDirectoryRedacted = response.fileDirectoryRedacted || '';
          hostExtra.noLocalhostProof =
            response.noLocalhostProof || 'custom-scheme-served-by-wkurlschemehandler-no-http-v1_17';
          const stagedUrl = response.stagedUrl || '';
          if (!stagedUrl) {
            return '';
          }
          recordFrontendStage(resource, stagedUrl, executionStatus || 'staged',
            response.cacheStatus || 'miss', Object.assign({ body }, hostExtra));
          return stagedUrl;
        } catch (error) {
          hostExtra.frontendResourceSchemeVersion = 'flow-ipc-resource-scheme-v1_17';
          hostExtra.frontendResourceLoaderMode = 'custom-scheme-file-backed';
          hostExtra.frontendResourceSchemeName = 'vatori-resource';
          hostExtra.frontendResourceSchemeHandlerInstalled = true;
          hostExtra.frontendResourceLoaderNoLocalhost = true;
          hostExtra.frontendResourceFileBacked = true;
          hostExtra.stagingMode = 'blob-fallback';
          hostExtra.blobFallbackReason = error && error.message ? error.message : String(error);
          hostExtra.blobFallbackBlockedLayer = 'host-owned-resource-stage';
          hostExtra.blobFallbackNextResolutionStep =
            'keep using Blob fallback only for this artifact until the host-owned resource stage request succeeds';
          return '';
        }
      }

      function recordFrontendStageOutcome(resource, stagedUrl, executionStatus, extra) {
        recordFrontendStage(resource, stagedUrl, executionStatus || 'executed', 'hit', extra || {});
      }

      function requestFlowClientResourceProbe(payload) {
        flowClientResources = payload && payload.flowClientResources && Array.isArray(payload.flowClientResources.resources)
          ? payload.flowClientResources.resources
          : [];
        recordFrontendArtifactManifest(payload && payload.flowClientResources ? payload.flowClientResources : {});
        window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount = flowClientResources.length;
        requestFlowClientResource('META-INF/frontend/FlowBootstrap.js', 'flow-bootstrap-module', 'flow-client-resource-probe');
      }

      function requestComponentFrontendResourceProbe(payload, requestNow = true) {
        const manifest = payload && payload.componentFrontendResources ? payload.componentFrontendResources : {};
        recordFrontendArtifactManifest(manifest);
        componentFrontendResources = Array.isArray(manifest.resources) ? manifest.resources : [];
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestObserved =
          componentFrontendResources.length > 0;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestCount = componentFrontendResources.length;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestPrimaryPath =
          typeof manifest.primaryResource === 'string' ? manifest.primaryResource : '';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestBlockedReason =
          typeof manifest.blockedReason === 'string' ? manifest.blockedReason : '';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestFailureClassification =
          typeof manifest.failureClassification === 'string' ? manifest.failureClassification : '';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResolverStrategy =
          typeof manifest.resolverStrategy === 'string' ? manifest.resolverStrategy : '';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceBlockedReason =
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestBlockedReason;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceFailureClassification =
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestFailureClassification;
        if (!requestNow) {
          return;
        }
        requestComponentFrontendResourceGraph();
      }

      function componentFrontendResourcePriority(resource) {
        if (!resource || typeof resource.path !== 'string') {
          return 100;
        }
        if (resource.productionBundleRuntimeArtifact === true) {
          return 5;
        }
        if (resource.productionOwnerDependencyResource === true) {
          return 6;
        }
        if (resource.generatedLazyBundleChunk === true || resource.generatedLazyBundleDependency === true) {
          return 90;
        }
        if (resource.flowLazyDependencyResource === true) {
          return 85;
        }
        if (resource.artifactFamily === 'generated-lazy' || resource.devModeBundleResource === true) {
          return 80;
        }
        if (resource.productionBundleStatsGraph === true) {
          return 70;
        }
        if (resource.artifactFamily === 'theme-static') {
          return 40;
        }
        if (resource.componentPrimary === true) {
          return 20;
        }
        if (resource.componentBundleDependency === true) {
          return 10;
        }
        return 50;
      }

      function sortedComponentFrontendResources() {
        return componentFrontendResources.slice().sort((left, right) => {
          const priorityDiff = componentFrontendResourcePriority(left) - componentFrontendResourcePriority(right);
          if (priorityDiff !== 0) {
            return priorityDiff;
          }
          return String(left && left.path ? left.path : '').localeCompare(String(right && right.path ? right.path : ''));
        });
      }

      function isDeferredComponentFrontendResource(resource) {
        if (!resource) {
          return false;
        }
        return resource.generatedLazyBundleChunk === true
          || resource.generatedLazyBundleDependency === true
          || resource.artifactFamily === 'generated-lazy'
          || resource.devModeBundleResource === true
          || resource.productionBundleStatsGraph === true;
      }

      function requestComponentFrontendResourceGraph() {
        for (const resource of sortedComponentFrontendResources()) {
          if (!resource || typeof resource.path !== 'string') {
            continue;
          }
          if (isDeferredComponentFrontendResource(resource)) {
            continue;
          }
          if (resource.flowLazyDependencyResource === true) {
            continue;
          }
          requestComponentFrontendResource(resource.path, resource.kind || '', 'vaadin-web-component-resource-probe');
        }
      }

      function requestDeferredComponentFrontendResourceGraph() {
        if (window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__) {
          return;
        }
        if (deferredComponentFrontendResourceGraphRequested) {
          return;
        }
        deferredComponentFrontendResourceGraphRequested = true;
        for (const resource of sortedComponentFrontendResources()) {
          if (!resource || typeof resource.path !== 'string') {
            continue;
          }
          if (!isDeferredComponentFrontendResource(resource)) {
            continue;
          }
          requestComponentFrontendResource(resource.path, resource.kind || '', 'vaadin-web-component-resource-probe');
        }
      }

      function smokeInteractionBaselineComplete() {
        if (window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__) {
          return false;
        }
        const automationEnabled =
          !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__
          || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__
          || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__
          || !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR__;
        if (!automationEnabled) {
          return true;
        }
        const d = window.VaadinDesktopIpcDiagnostics;
        if (window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__ && !d.flowClientEngineEventPatchConsumed) {
          return false;
        }
        if (autoComponentEventEnabled
            && (!d.flowClientEngineTextInputEventObserved
              || !d.flowClientEngineCheckboxEventObserved
              || !d.vaadinWebComponentEventRpcObserved
              || !d.vaadinTextFieldEventRpcObserved
              || !d.vaadinSecondWebComponentEventRpcObserved)) {
          return false;
        }
        if (window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ && !d.flowClientEngineResyncPatchConsumed) {
          return false;
        }
        if (window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR__ && !d.flowClientEngineErrorReported) {
          return false;
        }
        return true;
      }

      function maybeRequestDeferredComponentFrontendResourceGraph() {
        if (!smokeInteractionBaselineComplete()) {
          return;
        }
        requestDeferredComponentFrontendResourceGraph();
      }

      function requestFlowClientEngineResourceProbe() {
        requestFlowClientResource('META-INF/frontend/FlowClient.js', 'flow-client-module', 'flow-client-engine-probe');
      }

      function requestRemainingFlowClientResourceGraph() {
        for (const resource of flowClientResources) {
          if (!resource || typeof resource.path !== 'string') {
            continue;
          }
          if (resource.path === 'META-INF/frontend/FlowBootstrap.js' || resource.path === 'META-INF/frontend/FlowClient.js') {
            continue;
          }
          requestFlowClientResource(resource.path, resource.kind || '', 'flow-client-resource-graph-probe');
        }
      }

      function flowClientResourceByPath(path) {
        return flowClientResources.find((entry) => entry && entry.path === path) || null;
      }

      function componentFrontendResourceByPath(path) {
        return componentFrontendResources.find((entry) => entry && entry.path === path) || null;
      }

      function requestFlowClientResource(path, kind, purpose) {
        const resource = flowClientResources.find((entry) => entry.path === path)
          || flowClientResources.find((entry) => entry.kind === kind);
        if (!resource || typeof resource.path !== 'string') {
          return;
        }
        if (flowClientResourceRequests.has(resource.path)) {
          return;
        }
        flowClientResourceRequests.add(resource.path);
        window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphRequestedCount = flowClientResourceRequests.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphPaths =
          Array.from(flowClientResourceRequests).join(',');
        flowClientResourceExpectedSha256ByPath.set(resource.path, resource.sha256 || '');
        const securityKey = flowClientState.securityKey || flowClientState.csrfToken;
        if (kind === 'flow-bootstrap-module') {
          window.VaadinDesktopIpcDiagnostics.flowClientResourceExpectedSha256 = resource.sha256 || '';
        } else if (kind === 'flow-client-module') {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceExpectedSha256 = resource.sha256 || '';
        }
        const requestPayload = Object.assign(artifactRequestMetadata(resource), {
          purpose,
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        });
        send('resourceRequest', requestPayload);
      }

      function requestComponentFrontendResource(path, kind, purpose, extra) {
        const resource = componentFrontendResources.find((entry) => entry.path === path)
          || componentFrontendResources.find((entry) => entry.kind === kind);
        if (!resource || typeof resource.path !== 'string') {
          return;
        }
        const allowDuplicateLoadOnDemandTrace = purpose === 'generated-loadondemand-chunk-v1_11';
        if (componentFrontendResourceRequests.has(resource.path) && !allowDuplicateLoadOnDemandTrace) {
          return;
        }
        componentFrontendResourceRequests.add(resource.path);
        componentFrontendResourceExpectedSha256ByPath.set(resource.path, resource.sha256 || '');
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceRequested = true;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceRequestedCount =
          componentFrontendResourceRequests.size;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceRequestedPaths =
          Array.from(componentFrontendResourceRequests).join(',');
        if (resource.blockedReason) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceBlockedReason = resource.blockedReason;
        }
        if (resource.failureClassification) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceFailureClassification =
            resource.failureClassification;
        }
        if (Array.isArray(resource.shimmedImports)) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceShimmedImports =
            resource.shimmedImports.join(',');
        }
        const securityKey = flowClientState.securityKey || flowClientState.csrfToken;
        const requestPayload = Object.assign(artifactRequestMetadata(resource), extra || {}, {
          purpose,
          lookupKind: resource.lookupKind || 'component-frontend-resource',
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        });
        send('resourceRequest', requestPayload);
      }

      function requestFlowClientLazyResourceLookup() {
        if (flowClientLazyLookupRequested) {
          return;
        }
        if (!window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphComplete) {
          return;
        }
        const resource = flowClientResourceByPath('META-INF/frontend/Flow.js')
          || flowClientResources.find((entry) => entry && entry.kind === 'flow-module');
        if (!resource || typeof resource.path !== 'string') {
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupResult = 'no-flow-module-resource';
          return;
        }
        flowClientLazyLookupRequested = true;
        const loadOnDemand = window.Vaadin && window.Vaadin.Flow
          && typeof window.Vaadin.Flow.loadOnDemand === 'function';
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLoadOnDemandProbeResult =
          loadOnDemand ? 'api-present-not-invoked-in-smoke' : 'api-not-exposed-on-current-bootstrap';
        window.VaadinDesktopIpcDiagnostics.flowClientResourceBootstrapLazyEntriesResult =
          'no-bootstrap-LAZY-entry-in-smoke';
        if (!window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticVerified) {
          window.VaadinDesktopIpcDiagnostics.flowClientResourceThemeLookupResult =
            'no-theme-resource-declared-by-smoke-app';
        }
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupRequested = true;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupPurpose = 'flow-client-lazy-lookup';
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupKind = 'Flow.loadOnDemand-minimum-probe';
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupPath = resource.path;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupResult = 'requested';
        flowClientResourceExpectedSha256ByPath.set(resource.path, resource.sha256 || '');
        const securityKey = flowClientState.securityKey || flowClientState.csrfToken;
        const requestPayload = Object.assign(artifactRequestMetadata(resource), {
          purpose: 'flow-client-lazy-lookup',
          lookupKind: 'Flow.loadOnDemand-minimum-probe',
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        });
        send('resourceRequest', requestPayload);
      }

      function refreshFlowClientResourceExecutionDiagnostics() {
        window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedCount = flowClientResourceVerifiedPaths.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedPaths =
          Array.from(flowClientResourceVerifiedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutedCount = flowClientResourceExecutedPaths.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutedPaths =
          Array.from(flowClientResourceExecutedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedOnlyCount = flowClientResourceVerifiedOnlyPaths.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedOnlyPaths =
          Array.from(flowClientResourceVerifiedOnlyPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.flowClientResourceIntentionallyNotExecutedCount =
          flowClientResourceIntentionallyNotExecutedPaths.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceIntentionallyNotExecutedPaths =
          Array.from(flowClientResourceIntentionallyNotExecutedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionFailedCount =
          flowClientResourceExecutionFailedPaths.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionFailedPaths =
          Array.from(flowClientResourceExecutionFailedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.flowClientResourceExecuted =
          flowClientResourceExecutedPaths.has('META-INF/frontend/FlowBootstrap.js')
          && flowClientResourceExecutedPaths.has('META-INF/frontend/FlowClient.js');
        const manifestCount = window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount || flowClientResources.length;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionClassificationComplete =
          manifestCount > 0
          && window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedCount >= manifestCount
          && window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutedCount >= 2
          && window.VaadinDesktopIpcDiagnostics.flowClientResourceVerifiedOnlyCount >= Math.max(0, manifestCount - 2)
          && window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionFailedCount === 0
          && window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupVerified === true;
      }

      function classifyFlowClientResourceResponse(payload, body, metadataMatches) {
        const path = payload && typeof payload.path === 'string' ? payload.path : '';
        if (!path) {
          return;
        }
        if (metadataMatches && !!body) {
          flowClientResourceVerifiedPaths.add(path);
        }
        if (path === 'META-INF/resources/VAADIN/static/client/compile.properties') {
          window.VaadinDesktopIpcDiagnostics.flowClientResourceStaticLookupVerified = metadataMatches && !!body;
          window.VaadinDesktopIpcDiagnostics.flowClientResourceStaticLookupPath = path;
        }
        if (payload.requestPurpose === 'flow-client-lazy-lookup') {
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupResponse = true;
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupVerified = metadataMatches && !!body;
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupPurpose = payload.requestPurpose || '';
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupKind = payload.lookupKind || '';
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupPath = path;
          window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupResult =
            window.VaadinDesktopIpcDiagnostics.flowClientResourceLazyLookupVerified
              ? 'verified-only:intentionally-not-executed'
              : 'execution-failed:lookup-unverified';
          maybeRequestDeferredComponentFrontendResourceGraph();
        }
        const executable = path === 'META-INF/frontend/FlowBootstrap.js'
          || path === 'META-INF/frontend/FlowClient.js';
        if (!executable && metadataMatches && !!body) {
          flowClientResourceVerifiedOnlyPaths.add(path);
          flowClientResourceIntentionallyNotExecutedPaths.add(path);
        } else if (!metadataMatches || !body) {
          flowClientResourceExecutionFailedPaths.add(path);
        }
        refreshFlowClientResourceExecutionDiagnostics();
      }

      function markFlowClientResourceExecuted(path) {
        if (!path) {
          return;
        }
        flowClientResourceExecutedPaths.add(path);
        flowClientResourceVerifiedOnlyPaths.delete(path);
        flowClientResourceIntentionallyNotExecutedPaths.delete(path);
        refreshFlowClientResourceExecutionDiagnostics();
      }

      function createFlowClientBootstrapInitResponse(payload) {
        const uidl = parseUidl(payload) || {};
        const clientState = payload && payload.clientState ? payload.clientState : {};
        const uiId = Number.isInteger(clientState.uiId) ? clientState.uiId : 1;
        const securityKey = clientState.securityKey || clientState.csrfToken || uidl[flowClientState.securityTokenId] || '';
        return {
          appConfig: {
            appId: flowBootstrapAppId,
            uidl,
            productionMode: false,
            debug: true,
            serviceUrl: 'flow-ipc://local/flow/',
            contextRootUrl: '../',
            heartbeatInterval: 300,
            maxMessageSuspendTimeout: 5000,
            sendUrlsAsParameters: false,
            'v-uiId': uiId,
            'Vaadin-Security-Key': securityKey,
            webcomponents: []
          },
          errors: []
        };
      }

      function ensureFlowClientContainer() {
        const tag = flowClientContainerTag();
        let root = flowClientEngineRoot();
        if (!root) {
          root = document.createElement('section');
          root.id = 'flow-client-engine-root';
          root.hidden = true;
          document.body.insertBefore(root, trace);
        }
        let container = document.querySelector(tag);
        if (!container) {
          container = document.createElement(tag);
          container.id = flowBootstrapAppId;
          container.hidden = true;
          container.style.display = 'none';
          root.appendChild(container);
        } else if (container.parentElement !== root) {
          root.appendChild(container);
        }
        container.classList.add('vatori-flow-client-container');
        document.body.$ = document.body.$ || {};
        document.body.$[flowBootstrapAppId] = container;
        return container;
      }

      function rememberFlowClientBootstrapPayload(payload) {
        flowClientBootstrapInitResponse = createFlowClientBootstrapInitResponse(payload);
        rememberGeneratedLazyLoadOnDemandUidlExpressions(payload);
        window.VaadinDesktopIpcDiagnostics.flowClientBootstrapAppId = flowBootstrapAppId;
      }

      function rememberGeneratedLazyLoadOnDemandUidlExpressions(payload) {
        const body = payload && payload.uidl && payload.uidl.body;
        if (typeof body !== 'string' || body.length === 0) {
          return;
        }
        const regex = /return window\.Vaadin\.Flow\.loadOnDemand\('([^']+)'\);/g;
        const ids = [];
        const expressions = [];
        let match = regex.exec(body);
        while (match) {
          const expression = match[0];
          const chunkId = match[1] || '';
          if (chunkId && !ids.includes(chunkId)) {
            ids.push(chunkId);
          }
          if (expression && !expressions.includes(expression)) {
            expressions.push(expression);
          }
          match = regex.exec(body);
        }
        if (ids.length === 0) {
          return;
        }
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkUidlExpressionObserved = true;
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource =
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource
          || 'bootstrap-uidl-dependencies';
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpression = expressions[0] || '';
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkId = ids[0] || '';
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkIds = ids.join(',');
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner =
          window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner
          || 'visible-flow-client-dependencyloader';
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkDiagnosticBridgeUsed = false;
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkDirectImportProbeUsed = false;
      }

      function parseUidl(payload) {
        const body = payload && payload.uidl && payload.uidl.body;
        if (typeof body !== 'string' || body.length === 0) {
          return null;
        }
        try {
          return JSON.parse(body);
        } catch (_) {
          return null;
        }
      }

      function uidlState(nodeId) {
        if (!flowUidlNodes.has(nodeId)) {
          flowUidlNodes.set(nodeId, { nodeId, children: [] });
        }
        return flowUidlNodes.get(nodeId);
      }

      function ingestUidlChanges(changes) {
        for (const change of changes) {
          const state = uidlState(change.node);
          if (change.type === 'attach') {
            continue;
          }
          if (change.type === 'put') {
            state[change.key] = change.value;
            if (change.key === 'text') {
              flowTextNodes.set(change.node, String(change.value || ''));
            }
          } else if (change.type === 'clear') {
            state.children = [];
          } else if (change.type === 'splice' && Array.isArray(change.addNodes)) {
            const children = Array.isArray(state.children) ? state.children.slice() : [];
            children.splice(change.index || 0, 0, ...change.addNodes);
            state.children = children;
          }
        }
      }

      function renderUidlNode(nodeId) {
        const state = flowUidlNodes.get(nodeId);
        if (!state) {
          const text = document.createElement('span');
          text.textContent = flowTextNodes.get(nodeId) || '';
          text.dataset.flowNodeId = String(nodeId);
          flowNodeElements.set(nodeId, text);
          return text;
        }

        const tag = state.tag || 'span';
        let element;
        if (tag === 'vaadin-button') {
          element = document.createElement('button');
          element.addEventListener('click', () => {
            sendFlowEvent({ id: state.id || '', nodeId }, 'click');
          });
        } else if (tag === 'input') {
          element = document.createElement('input');
          if (state.type) {
            element.type = String(state.type);
          }
          if (state.placeholder) {
            element.placeholder = String(state.placeholder);
          }
          if (state.value) {
            element.value = String(state.value);
          }
          element.addEventListener('change', () => {
            const data = element.type === 'checkbox'
              ? { 'element.checked': !!element.checked }
              : { 'element.value': element.value || '' };
            sendFlowEvent({ id: state.id || '', nodeId }, 'change', data);
          });
        } else if (tag === 'h1') {
          element = document.createElement('h1');
        } else if (tag === 'span') {
          element = document.createElement('span');
        } else if (tag.startsWith('vaadin-')) {
          element = document.createElement(tag);
          if (tag === 'vaadin-big-decimal-field'
              || tag === 'vaadin-text-field'
              || tag === 'vaadin-checkbox') {
            element.addEventListener('change', () => {
              const data = tag === 'vaadin-checkbox'
                ? { 'element.checked': !!element.checked }
                : { 'element.value': element.value || '' };
              sendFlowEvent({ id: state.id || '', nodeId }, 'change', data);
            });
          }
        } else {
          element = document.createElement('div');
          if (tag.includes('vertical-layout') || tag.includes('horizontal-layout')) {
            element.className = 'flow-layout';
          }
        }

        if (state.id) {
          element.dataset.flowId = state.id;
          element.id = state.id;
        }
        element.dataset.flowNodeId = String(nodeId);
        flowNodeElements.set(nodeId, element);

        const children = Array.isArray(state.children) ? state.children : [];
        if (children.length > 0) {
          for (const childId of children) {
            element.appendChild(renderUidlNode(childId));
          }
        } else if (typeof state.text === 'string') {
          element.textContent = state.text;
        }
        return element;
      }

      function applyUidlBootstrap(payload) {
        const uidl = parseUidl(payload);
        if (!uidl || !Array.isArray(uidl.changes)) {
          return false;
        }
        flowNodeElements.clear();
        flowTextNodes.clear();
        flowUidlNodes.clear();
        ingestUidlChanges(uidl.changes);

        const body = flowUidlNodes.get(1);
        const roots = body && Array.isArray(body.children) ? body.children : [];
        if (roots.length === 0) {
          return false;
        }
        app.replaceChildren(...roots.map(renderUidlNode));
        resetFlowIpcViewport();
        return true;
      }

      function applyUidlPatch(payload) {
        const uidl = parseUidl(payload);
        if (!uidl || !Array.isArray(uidl.changes)) {
          return false;
        }
        ingestUidlChanges(uidl.changes);

        let applied = false;
        for (const change of uidl.changes) {
          if (change.type === 'put' && change.key === 'text') {
            const value = String(change.value || '');
            flowTextNodes.set(change.node, value);
            const element = flowNodeElements.get(change.node);
            if (element) {
              element.textContent = value;
              applied = true;
            }
          } else if (change.type === 'clear') {
            const element = flowNodeElements.get(change.node);
            if (element) {
              element.replaceChildren();
              applied = true;
            }
          } else if (change.type === 'splice' && Array.isArray(change.addNodes)) {
            const element = flowNodeElements.get(change.node);
            if (element) {
              let offset = 0;
              for (const nodeId of change.addNodes) {
                const text = renderUidlNode(nodeId);
                const reference = element.childNodes[(change.index || 0) + offset] || null;
                element.insertBefore(text, reference);
                offset += 1;
              }
              applied = true;
            }
          }
        }
        return applied;
      }

      function renderNode(node) {
        let element;
        if (node.kind === 'button') {
          element = document.createElement('button');
          element.textContent = node.text || node.id;
          element.addEventListener('click', () => {
            sendFlowEvent(node, 'click');
          });
        } else if (node.tag === 'input') {
          element = document.createElement('input');
          if (node.id === 'ipc-checkbox') {
            element.type = 'checkbox';
          }
          element.addEventListener('change', () => {
            const data = element.type === 'checkbox'
              ? { 'element.checked': !!element.checked }
              : { 'element.value': element.value || '' };
            sendFlowEvent(node, 'change', data);
          });
        } else if (node.tag === 'h1') {
          element = document.createElement('h1');
          element.textContent = node.text || '';
        } else if (typeof node.tag === 'string' && node.tag.startsWith('vaadin-')) {
          element = document.createElement(node.tag);
          if (node.tag === 'vaadin-big-decimal-field'
              || node.tag === 'vaadin-text-field'
              || node.tag === 'vaadin-checkbox') {
            element.addEventListener('change', () => {
              const data = node.tag === 'vaadin-checkbox'
                ? { 'element.checked': !!element.checked }
                : { 'element.value': element.value || '' };
              sendFlowEvent(node, 'change', data);
            });
          }
        } else {
          element = document.createElement('div');
          if (node.kind === 'layout') {
            element.className = 'flow-layout';
          }
          if (node.text) {
            const text = document.createElement('span');
            text.textContent = node.text;
            element.appendChild(text);
          }
        }
        element.dataset.flowId = node.id || '';
        if (node.id) {
          element.id = node.id;
        }
        if (Number.isInteger(node.nodeId)) {
          element.dataset.flowNodeId = String(node.nodeId);
          flowNodeElements.set(node.nodeId, element);
        }
        if (node.kind !== 'button') {
          for (const child of node.children || []) {
            element.appendChild(renderNode(child));
          }
        }
        return element;
      }

      function initializeFlowBootstrapModule(module) {
        if (!flowClientBootstrapInitResponse || typeof module.init !== 'function') {
          return;
        }
        try {
          const container = ensureFlowClientContainer();
          installGeneratedLoadOnDemandChunkBridge();
          module.init(flowClientBootstrapInitResponse);
          installGeneratedLoadOnDemandChunkBridge();
          normalizeFlowClientViewport(container);
          const flow = window.Vaadin && window.Vaadin.Flow ? window.Vaadin.Flow : null;
          const appIds = flow && typeof flow.getAppIds === 'function' ? flow.getAppIds() : [];
          window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized = appIds.includes(flowBootstrapAppId);
          refreshFlowClientEngineViewportDiagnostics();
          if (!window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized) {
            window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitError = 'FlowBootstrap.init did not register ' + flowBootstrapAppId;
          } else {
            requestFlowClientEngineResourceProbe();
          }
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized = false;
          window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitError = error && error.message ? error.message : String(error);
        }
      }

      function refreshFlowClientEngineDiagnostics() {
        const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients ? window.Vaadin.Flow.clients : {};
        const clientKeys = Object.keys(clients);
        const clientEntry = flowClientEngineClientEntry();
        const client = clientEntry.client;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineClientKey = clientEntry.key;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineClientKeys = clientKeys.join(',');
        window.VaadinDesktopIpcDiagnostics.flowClientEngineInitialized = !!client && client.initializing === false;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered = !!client && typeof client.sendEventMessage === 'function';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineIpcMessageBridgeInstalled = !!client && typeof client.handleIpcMessage === 'function';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineIpcResyncBridgeInstalled = !!client && typeof client.requestIpcResync === 'function';
        refreshFlowClientEngineViewportDiagnostics();
        promoteFlowClientEngineWhenReady();
        try {
          if (client && typeof client.getByNodeId === 'function') {
          const boundNodeIds = [];
          const expectedIds = [
            'ipc-root',
            'ipc-button',
            'ipc-status',
            'ipc-list',
            'ipc-event-matrix',
            'ipc-text-input',
            'ipc-text-status',
            'ipc-checkbox',
            'ipc-checkbox-status',
            'ipc-vaadin-big-decimal',
            'ipc-vaadin-big-decimal-status',
            'ipc-vaadin-text-field',
            'ipc-vaadin-text-field-status',
            'ipc-vaadin-checkbox',
            'ipc-vaadin-checkbox-status',
            'ipc-push-status',
            'ipc-push-order-status'
          ];
          for (const id of expectedIds) {
            const nodeId = flowClientNodeIdsById.get(id);
            if (!Number.isInteger(nodeId)) {
              continue;
            }
            try {
              if (client.getByNodeId(nodeId)) {
                boundNodeIds.push(nodeId);
              }
            } catch (_) {
              // Keep diagnostics best-effort; the client bridge itself has already initialized.
            }
          }
          window.VaadinDesktopIpcDiagnostics.flowClientEngineBoundNodeIds = boundNodeIds.join(',');
          const buttonById = flowClientEngineElementById('ipc-button');
          const statusById = flowClientEngineElementById('ipc-status');
          const listById = flowClientEngineElementById('ipc-list');
          const matrixById = flowClientEngineElementById('ipc-event-matrix');
          window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapDomBound =
            !!buttonById && !!statusById && !!listById && !!matrixById;
          try {
            const status = statusById ? statusById.element : client.getByNodeId(14);
            window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapStatusText = status ? (status.textContent || '') : '';
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapStatusText = '';
          }
          try {
            const button = buttonById ? buttonById.element : client.getByNodeId(16);
            window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapButtonText = button ? (button.textContent || '') : '';
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapButtonText = '';
          }
          try {
            const list = listById ? listById.element : (client.getByNodeId(12) || document.querySelector('#flow-client-engine-root #ipc-list'));
            const listText = list ? (list.textContent || '') : '';
            window.VaadinDesktopIpcDiagnostics.flowClientEngineListBound =
              !!list && listText.includes('1 Bootstrap over IPC READY')
              && listText.includes('4 Desktop command/capabilityResult over IPC READY');
            window.VaadinDesktopIpcDiagnostics.flowClientEngineListItemCount =
              list ? list.querySelectorAll('span').length : 0;
            window.VaadinDesktopIpcDiagnostics.flowClientEngineListText = listText;
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineListBound = false;
            window.VaadinDesktopIpcDiagnostics.flowClientEngineListItemCount = 0;
            window.VaadinDesktopIpcDiagnostics.flowClientEngineListText = '';
          }
          try {
            const pushStatusById = flowClientEngineElementById('ipc-push-status');
            const pushStatus = pushStatusById ? pushStatusById.element : (client.getByNodeId(21) || document.querySelector('#flow-client-engine-root #ipc-push-status'));
            window.VaadinDesktopIpcDiagnostics.flowClientEnginePushStatusText = pushStatus ? (pushStatus.textContent || '') : '';
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEnginePushStatusText = '';
          }
          try {
            const pushOrderStatusById = flowClientEngineElementById('ipc-push-order-status');
            const pushOrderStatus = pushOrderStatusById ? pushOrderStatusById.element : document.querySelector('#flow-client-engine-root #ipc-push-order-status');
            window.VaadinDesktopIpcDiagnostics.flowClientEnginePushOrderStatusText = pushOrderStatus ? (pushOrderStatus.textContent || '') : '';
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEnginePushOrderStatusText = '';
          }
          try {
            const textStatus = flowClientEngineElementById('ipc-text-status');
            const textStatusText = textStatus ? (textStatus.element.textContent || '') : '';
            window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputStatusText = textStatusText;
            window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputEventObserved =
              textStatusText.includes('Text input: ' + autoComponentEventValue);
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputStatusText = '';
          }
          try {
            const checkboxStatus = flowClientEngineElementById('ipc-checkbox-status');
            const checkboxStatusText = checkboxStatus ? (checkboxStatus.element.textContent || '') : '';
            window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxStatusText = checkboxStatusText;
            window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved =
              checkboxStatusText.includes('Checkbox: checked');
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxStatusText = '';
          }
          try {
            const componentTarget = flowClientEngineElementById('ipc-vaadin-big-decimal');
            const componentElement = componentTarget ? componentTarget.element : null;
            const componentStatus = flowClientEngineElementById('ipc-vaadin-big-decimal-status');
            const componentStatusText = componentStatus ? (componentStatus.element.textContent || '') : '';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRendered = !!componentElement;
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRenderedTag =
              componentElement ? componentElement.tagName.toLowerCase() : '';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRenderedValue =
              componentElement && typeof componentElement.value !== 'undefined' ? String(componentElement.value || '') : '';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentTagDefined =
              !!customElements.get('vaadin-big-decimal-field');
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentStatusText = componentStatusText;
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentPostEventState = componentStatusText;
            if (componentStatusText.includes('Vaadin BigDecimalField: ' + autoVaadinWebComponentValue)) {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventObserved = true;
            }
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentRendered = false;
          }
          try {
            const textFieldTarget = flowClientEngineElementById('ipc-vaadin-text-field');
            const textFieldElement = textFieldTarget ? textFieldTarget.element : null;
            const textFieldStatus = flowClientEngineElementById('ipc-vaadin-text-field-status');
            const textFieldStatusText = textFieldStatus ? (textFieldStatus.element.textContent || '') : '';
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRendered = !!textFieldElement;
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRenderedTag =
              textFieldElement ? textFieldElement.tagName.toLowerCase() : '';
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRenderedValue =
              textFieldElement && typeof textFieldElement.value !== 'undefined' ? String(textFieldElement.value || '') : '';
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldTagDefined =
              !!customElements.get('vaadin-text-field');
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldStatusText = textFieldStatusText;
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldPostEventState = textFieldStatusText;
            if (textFieldStatusText.includes('Vaadin TextField: ' + autoVaadinTextFieldValue)) {
              window.VaadinDesktopIpcDiagnostics.vaadinTextFieldEventObserved = true;
            }
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.vaadinTextFieldRendered = false;
          }
          try {
            const checkboxTarget = flowClientEngineElementById('ipc-vaadin-checkbox');
            const checkboxElement = checkboxTarget ? checkboxTarget.element : null;
            const checkboxStatus = flowClientEngineElementById('ipc-vaadin-checkbox-status');
            const checkboxStatusText = checkboxStatus ? (checkboxStatus.element.textContent || '') : '';
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRendered = !!checkboxElement;
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRenderedTag =
              checkboxElement ? checkboxElement.tagName.toLowerCase() : '';
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRenderedValue =
              checkboxElement && typeof checkboxElement.checked !== 'undefined' ? String(!!checkboxElement.checked) : '';
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined =
              !!customElements.get('vaadin-checkbox');
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentStatusText = checkboxStatusText;
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentPostEventState = checkboxStatusText;
            if (checkboxStatusText.includes('Vaadin Checkbox: checked')) {
              window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved = true;
            }
          } catch (_) {
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentRendered = false;
          }
          }
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError =
            'smoke diagnostics refresh failed: ' + (error && error.message ? error.message : String(error));
        }
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = '';
        } else if (clientKeys.length > 0) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = 'FlowClient.init has not registered sendEventMessage client yet';
        }
        promoteFlowClientEngineWhenReady();
        refreshFlowClientEngineVisibleDiagnostics();
        refreshFrameworkAppDiagnostics();
        window.VaadinDesktopIpcDiagnostics.flowClientEngineEventStatusText =
          window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapStatusText;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed =
          /^Clicks:\s*[1-9]\d*/.test(window.VaadinDesktopIpcDiagnostics.flowClientEngineEventStatusText);
        window.VaadinDesktopIpcDiagnostics.flowClientEnginePushPatchConsumed =
          window.VaadinDesktopIpcDiagnostics.flowClientEnginePushPatchConsumed
          || /Server push over IPC READY/.test(window.VaadinDesktopIpcDiagnostics.flowClientEnginePushStatusText);
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed) {
          autoClickDone = true;
          autoClickInFlight = false;
        }
        if (autoComponentEventEnabled
            && window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputEventObserved
            && window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved
            && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventObserved
            && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventRpcObserved
            && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentPostEventState.includes(autoVaadinWebComponentValue)
            && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved
            && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventRpcObserved
            && window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentPostEventState.includes('Vaadin Checkbox: checked')) {
          autoComponentEventsDone = true;
          autoComponentEventInFlight = false;
        }
        window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceVerified =
          flowClientEngineResourceBaseVerified
          && window.VaadinDesktopIpcDiagnostics.flowClientEngineInitExported
          && window.VaadinDesktopIpcDiagnostics.flowClientEngineInitialized
          && window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered
          && window.VaadinDesktopIpcDiagnostics.flowClientEngineIpcMessageBridgeInstalled
          && window.VaadinDesktopIpcDiagnostics.flowClientEngineIpcResyncBridgeInstalled;
        flushQueuedFlowClientEngineMessages();
        startAutoFrameworkUpdatePolling();
        scheduleAutoComponentEvents();
        requestAutoResyncIfReady();
        scheduleAutoErrorProbe();
      }

      function initializeFlowClientEngineModule(module) {
        try {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitExported = typeof module.init === 'function';
          if (!window.VaadinDesktopIpcDiagnostics.flowClientEngineInitExported) {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = 'FlowClient.js did not export init';
            return;
          }
          ensureFlowClientContainer();
          installFlowIpcXhrBridge();
          installFlowIpcDependencyLoaderBridge();
          installGeneratedLoadOnDemandChunkBridge();
          module.init();
          refreshFlowClientEngineDiagnostics();
          setTimeout(refreshFlowClientEngineDiagnostics, 250);
          setTimeout(refreshFlowClientEngineDiagnostics, 1000);
          scheduleFlowClientEnginePromotionRetry();
          scheduleAutoClick();
          startAutoFrameworkUpdatePolling();
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitialized = false;
          window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered = false;
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = error && error.message ? error.message : String(error);
        }
      }

      async function handleResourceResponse(payload) {
        if (payload && payload.componentFrontendResource === true) {
          await handleComponentFrontendResourceResponse(payload);
          return;
        }
        if (!payload || payload.flowClientResource !== true) {
          return;
        }
        const body = typeof payload.body === 'string' ? payload.body : '';
        const resourcePath = payload.path || '';
        const expectedSha256 = flowClientResourceExpectedSha256ByPath.get(resourcePath) || '';
        const metadataMatches = !expectedSha256 || expectedSha256 === (payload.sha256 || '');
        recordFlowClientResourceGraphResponse(resourcePath, body, metadataMatches);
        classifyFlowClientResourceResponse(payload, body, metadataMatches);
        if (resourcePath === 'META-INF/frontend/FlowClient.js' || payload.kind === 'flow-client-module') {
          await handleFlowClientEngineResourceResponse(payload, body, metadataMatches);
          return;
        }
        if (resourcePath !== 'META-INF/frontend/FlowBootstrap.js' && payload.kind !== 'flow-bootstrap-module') {
          handleAdditionalFlowClientResourceResponse(payload);
          return;
        }
        await handleFlowBootstrapResourceResponse(payload, body, metadataMatches);
      }

      function recordFlowClientResourceGraphResponse(resourcePath, body, metadataMatches) {
        if (!resourcePath) {
          return;
        }
        flowClientResourceResponsePaths.add(resourcePath);
        flowClientResourceGraphMetadataVerified = flowClientResourceGraphMetadataVerified && metadataMatches && !!body;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphResponseCount = flowClientResourceResponsePaths.size;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphResponsePaths =
          Array.from(flowClientResourceResponsePaths).join(',');
        const manifestCount = window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount || flowClientResources.length;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphComplete =
          manifestCount > 0 && flowClientResourceResponsePaths.size >= manifestCount;
        window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphVerified =
          flowClientResourceGraphMetadataVerified && window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphComplete;
        if (window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphComplete) {
          setTimeout(requestFlowClientLazyResourceLookup, 0);
        }
      }

      function handleAdditionalFlowClientResourceResponse(payload) {
        trace.textContent = 'flow resource graph '
          + window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphResponseCount
          + '/'
          + window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount;
      }

      async function handleComponentFrontendResourceResponse(payload) {
        const body = typeof payload.body === 'string' ? payload.body : '';
        const resourcePath = payload.path || '';
        const expectedSha256 = componentFrontendResourceExpectedSha256ByPath.get(resourcePath) || '';
        const metadataMatches = !expectedSha256 || expectedSha256 === (payload.sha256 || '');
        if (resourcePath) {
          componentFrontendResourceResponsePaths.add(resourcePath);
          componentFrontendResourceGraphMetadataVerified =
            componentFrontendResourceGraphMetadataVerified && metadataMatches && !!body;
          if (metadataMatches && !!body) {
            componentFrontendResourceVerifiedPaths.add(resourcePath);
            componentFrontendResourceBodies.set(resourcePath, body);
            if (payload.artifactFamily === 'theme-static') {
              window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticVerified = true;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticPath =
                mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticPath, [resourcePath]);
              window.VaadinDesktopIpcDiagnostics.flowClientResourceThemeLookupResult =
                'verified:' + resourcePath;
              if (!document.querySelector('style[data-vatori-theme-static-resource="' + resourcePath + '"]')) {
                const style = document.createElement('style');
                style.dataset.vatoriThemeStaticResource = resourcePath;
                style.textContent = body;
                document.head.appendChild(style);
              }
            } else if (payload.artifactFamily === 'generated-lazy') {
              window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyVerified = true;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyPath =
                mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyPath, [resourcePath]);
              if (payload.generatedLazyBundleChunk === true) {
                window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkVerified = true;
                window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkPath =
                  mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkPath, [resourcePath]);
                window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkResult =
                  'verified:' + resourcePath;
                window.VaadinDesktopIpcDiagnostics.generatedLazyChunkResolvedArtifactPath =
                  payload.generatedLazyChunkResolvedArtifactPath
                  || payload.loadOnDemandChunkResolvedArtifactPath
                  || resourcePath;
                window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource =
                  payload.generatedLazyChunkExpressionSource
                  || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpressionSource;
                window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner =
                  payload.generatedLazyChunkTriggerOwner
                  || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggerOwner;
                window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer =
                  payload.generatedLazyChunkFailureLayer
                  || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer;
                window.VaadinDesktopIpcDiagnostics.generatedLazyChunkBlockerVersion =
                  payload.generatedLazyChunkBlockerVersion
                  || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkBlockerVersion;
                window.VaadinDesktopIpcDiagnostics.generatedLazyChunkNextResolutionStep =
                  payload.generatedLazyChunkNextResolutionStep
                  || window.VaadinDesktopIpcDiagnostics.generatedLazyChunkNextResolutionStep;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkContractVersion =
                  payload.loadOnDemandChunkContractVersion
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkContractVersion;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiOwner =
                  payload.loadOnDemandChunkApiOwner
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiOwner;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiInstallSource =
                  payload.loadOnDemandChunkApiInstallSource
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiInstallSource;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRegistrySource =
                  payload.loadOnDemandChunkRegistrySource
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRegistrySource;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkMappingSource =
                  payload.loadOnDemandChunkMappingSource
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkMappingSource;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkResolvedArtifactPath =
                  payload.loadOnDemandChunkResolvedArtifactPath || resourcePath;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkModuleIdentity =
                  payload.loadOnDemandChunkModuleIdentity
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkModuleIdentity;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer =
                  payload.loadOnDemandChunkFailureLayer
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBlockerVersion =
                  payload.loadOnDemandChunkBlockerVersion
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBlockerVersion;
                window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkNextResolutionStep =
                  payload.loadOnDemandChunkNextResolutionStep
                  || window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkNextResolutionStep;
              }
            } else if (payload.artifactFamily === 'production-bundle'
                && payload.productionBundleStatsGraph === true) {
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsVerified = true;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsPath =
                mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsPath, [resourcePath]);
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleStatsResult =
                'verified:' + resourcePath;
            } else if (payload.flowLazyDependencyResource === true) {
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyResponse = true;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyVersion =
                payload.lazyDependencyVersion || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyVersion;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencySource =
                payload.lazyDependencySource || window.VaadinDesktopIpcDiagnostics.flowLazyDependencySource;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyMode =
                payload.lazyDependencyMode || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyMode;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExpression =
                payload.lazyDependencyExpression || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExpression;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyChunkId =
                payload.lazyDependencyChunkId || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyChunkId;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyResolvedPath =
                payload.lazyDependencyResolvedPath || resourcePath;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggerOwner =
                payload.lazyDependencyTriggerOwner || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggerOwner;
              window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggeredByFlowClient =
                payload.lazyDependencyTriggeredByFlowClient === true
                || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggeredByFlowClient;
            } else if (payload.productionBundleRuntimeArtifact === true) {
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeObserved = true;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeVersion =
                payload.productionBundleRuntimeVersion
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeVersion;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeRegistrySource =
                payload.productionBundleRuntimeRegistrySource
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeRegistrySource;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStatsSource =
                payload.productionBundleRuntimeStatsSource
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStatsSource;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeSelectedArtifact =
                payload.productionBundleRuntimeSelectedArtifact || resourcePath;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeDependencyChain =
                payload.productionBundleRuntimeDependencyChain
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeDependencyChain;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeModuleIdentity =
                payload.productionBundleRuntimeModuleIdentity
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeModuleIdentity;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerObserved = true;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerVersion =
                payload.productionBundleRuntimeOwnerVersion
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerVersion;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwner =
                payload.productionBundleRuntimeOwner
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwner;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerEntryArtifact =
                payload.productionBundleRuntimeEntryArtifact || resourcePath;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSource =
                payload.productionBundleRuntimeSource
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSource;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSelectedBy =
                payload.productionBundleRuntimeSelectedBy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSelectedBy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerModuleIdentity =
                payload.productionBundleRuntimeOwnerModuleIdentity
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerModuleIdentity;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerLifecycle =
                payload.productionBundleRuntimeOwnerLifecycle
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerLifecycle;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy =
                payload.productionBundleRuntimeOwnerExecutionPolicy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy =
                payload.productionBundleRuntimeOwnerStageUrlPolicy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCachePolicy =
                payload.productionBundleRuntimeOwnerCachePolicy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCachePolicy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy =
                payload.productionBundleRuntimeOwnerCleanupStrategy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer =
                payload.productionBundleRuntimeOwnerFailureLayer
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlockerVersion =
                payload.productionBundleRuntimeOwnerBlockerVersion
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlockerVersion;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep =
                payload.productionBundleRuntimeOwnerNextResolutionStep
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVersion =
                payload.productionBundleRuntimeOwnerImportVersion
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVersion;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPolicy =
                payload.productionBundleRuntimeOwnerImportPolicy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPolicy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportArtifact =
                payload.productionBundleRuntimeOwnerImportArtifact || resourcePath;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhase =
                payload.productionBundleRuntimeOwnerImportPhase
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhase;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects =
                payload.productionBundleRuntimeOwnerImportExpectedSideEffects
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures =
                payload.productionBundleRuntimeOwnerImportAcceptableFailures
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy =
                payload.productionBundleRuntimeOwnerImportRewriteStrategy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths =
                payload.productionBundleRuntimeOwnerImportDependencyPaths
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion =
                payload.productionBundleRuntimeOwnerDependencyResolverVersion
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot =
                payload.productionBundleRuntimeOwnerDependencyResolverRoot
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy =
                payload.productionBundleRuntimeOwnerDependencyResolverPolicy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts =
                payload.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures =
                payload.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace =
                payload.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact =
                payload.productionBundleRuntimeOwnerDependencyResolverRootArtifact
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact
                || resourcePath;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep =
                payload.productionBundleRuntimeOwnerImportNextResolutionStep
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion =
                payload.productionBundleRuntimeOwnerImportOrderingVersion
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy =
                payload.productionBundleRuntimeOwnerImportPhasePolicy
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult =
                payload.productionBundleRuntimeOwnerImportOrderingResult
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases =
                payload.productionBundleRuntimeOwnerImportOrderingObservedPhases
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures =
                payload.productionBundleRuntimeOwnerImportOrderingAcceptableFailures
                || window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseVersion =
                payload.componentProductionOwnerReuseVersion
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseVersion;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReusePolicy =
                payload.componentProductionOwnerReusePolicy
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReusePolicy;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseMode =
                payload.componentProductionOwnerReuseMode
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseMode;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseAcceptableFailures =
                payload.componentProductionOwnerReuseAcceptableFailures
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseAcceptableFailures;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused =
                payload.componentProductionOwnerReused === true
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason =
                payload.componentProductionOwnerReuseReason
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerOwnedTags =
                payload.componentProductionOwnerOwnedTags
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerOwnedTags;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerSkippedDefinitions =
                payload.componentProductionOwnerSkippedDefinitions
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerSkippedDefinitions;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerFallbackImports =
                payload.componentProductionOwnerFallbackImports
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerFallbackImports;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths =
                payload.componentProductionOwnerDependencyPaths
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths;
              window.VaadinDesktopIpcDiagnostics.componentProductionOwnerImportOrder =
                payload.componentProductionOwnerImportOrder
                || window.VaadinDesktopIpcDiagnostics.componentProductionOwnerImportOrder;
            } else if (payload.artifactFamily === 'dev-mode-bundle') {
              window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModeVerified = true;
              window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePath =
                mergeCsv(window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePath, [resourcePath]);
              window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModePackageGraphResult =
                'verified:' + resourcePath;
            }
          } else {
            componentFrontendResourceExecutionFailedPaths.add(resourcePath);
          }
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResponseCount =
            componentFrontendResourceResponsePaths.size;
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResponsePaths =
            Array.from(componentFrontendResourceResponsePaths).join(',');
        }
        if (payload.blockedReason) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceBlockedReason = payload.blockedReason;
        }
        if (payload.failureClassification) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceFailureClassification =
            payload.failureClassification;
        }
        if (Array.isArray(payload.shimmedImports)) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceShimmedImports =
            payload.shimmedImports.join(',');
        }
        refreshComponentFrontendResourceDiagnostics();
        try {
          await maybeStageProductionBundleRuntimeResource();
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStageAttempted = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult =
            'execution-failed:stage-dispatch';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError =
            error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker = true;
        }
        try {
          await maybeExecuteFlowLazyDependencyResource();
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecutionResult =
            'execution-failed:dispatch';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyBrowserError =
            error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyFailureLayer =
            'flow-lazy-dependency-execution-dispatch';
        }
        try {
          await maybeImportProductionBundleRuntimeOwner();
        } catch (error) {
          const message = error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAttempted = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImported = false;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionResult =
            'strict-blocker:production-owner-browser-import-failed:dispatch';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer =
            'production-owner-browser-import-failed';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBrowserError = message;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlocker = true;
        }
        try {
          await maybeExecuteVaadinWebComponentResource();
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
            error && error.message ? error.message : String(error);
        }
        try {
          await maybeExecuteGeneratedLazyBundleResource();
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionAttempted = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionResult =
            'strict-blocker:probe-dispatch';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionError =
            error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker = true;
        }
        try {
          await maybeStageTextFieldDevBundleBlocker();
        } catch (error) {
          const resource = textFieldDevBundleResource();
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlocker = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerPath =
            resource && resource.path ? resource.path : 'TextField-dev-bundle-resource-v1_4';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerReason =
            'textfield-staging-probe-dispatch-failed';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerError =
            error && error.message ? error.message : String(error);
        }
        try {
          await maybeImportProductionBundleRuntimeOwner();
        } catch (error) {
          const message = error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAttempted = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImported = false;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionResult =
            'strict-blocker:production-owner-browser-import-failed:dispatch';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer =
            'production-owner-browser-import-failed';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBrowserError = message;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlocker = true;
        }
      }

      function refreshComponentFrontendResourceDiagnostics() {
        const manifestCount = window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestCount
          || componentFrontendResources.length;
        const primaryResources = componentFrontendPrimaryResources();
        const primaryResourceCount = primaryResources.length || 1;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceVerifiedCount =
          componentFrontendResourceVerifiedPaths.size;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceVerifiedPaths =
          Array.from(componentFrontendResourceVerifiedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedCount =
          componentFrontendResourceExecutedPaths.size;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPaths =
          Array.from(componentFrontendResourceExecutedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedTags =
          Array.from(componentFrontendResourceExecutedTags).join(',');
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionFailedCount =
          componentFrontendResourceExecutionFailedPaths.size;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionFailedPaths =
          Array.from(componentFrontendResourceExecutionFailedPaths).join(',');
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionFailed =
          componentFrontendResourceExecutionFailedPaths.size > 0;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphComplete =
          manifestCount > 0 && componentFrontendResourceResponsePaths.size >= manifestCount;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified =
          componentFrontendResourceGraphMetadataVerified
          && window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphComplete;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceResolved =
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceVerified =
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecuted =
          componentFrontendResourceExecutedPaths.size >= primaryResourceCount
          && componentFrontendResourceExecutionFailedPaths.size === 0;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentTagDefined =
          !!customElements.get('vaadin-big-decimal-field');
        window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined =
          !!customElements.get('vaadin-checkbox');
      }

      function componentFrontendPrimaryResources() {
        const primary = componentFrontendResources.filter((entry) => {
          return entry
            && typeof entry.path === 'string'
            && entry.componentPrimary === true;
        });
        if (primary.length > 0) {
          return primary;
        }
        const primaryPath = window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceManifestPrimaryPath
          || 'META-INF/resources/frontend/vaadin-big-decimal-field.js';
        const resource = componentFrontendResourceByPath(primaryPath);
        return resource ? [resource] : [];
      }

      function componentFrontendExecutableResourcePaths() {
        const paths = new Set();
        for (const resource of componentFrontendResources) {
          if (!resource || typeof resource.path !== 'string') {
            continue;
          }
          if (resource.componentPrimary === true || resource.componentBundleDependency === true) {
            paths.add(resource.path);
          }
          const rewrites = Array.isArray(resource.importRewrites) ? resource.importRewrites : [];
          for (const rewrite of rewrites) {
            if (rewrite && typeof rewrite.resourcePath === 'string' && rewrite.resourcePath) {
              paths.add(rewrite.resourcePath);
            }
          }
        }
        return Array.from(paths);
      }

      function componentFrontendExecutableResourcesReady() {
        const requiredPaths = componentFrontendExecutableResourcePaths();
        if (requiredPaths.length === 0) {
          return false;
        }
        for (const path of requiredPaths) {
          if (!componentFrontendResourceBodies.has(path)) {
            return false;
          }
        }
        return componentFrontendPrimaryResources().length > 0;
      }

      function componentTagForResource(resource) {
        if (resource && typeof resource.componentTag === 'string' && resource.componentTag) {
          return resource.componentTag;
        }
        if (resource && resource.path === 'META-INF/resources/frontend/vaadin-big-decimal-field.js') {
          return 'vaadin-big-decimal-field';
        }
        if (resource && resource.path === 'META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js') {
          return 'vaadin-checkbox';
        }
        if (resource && resource.path === 'META-INF/vatori/dev-bundle-entry/vaadin-select.js') {
          return 'vaadin-select';
        }
        return '';
      }

      function usesStableModuleIdentity(resource) {
        return !!(resource
          && (resource.devBundleResource === true
            || resource.artifactFamily === 'production-bundle'
            || resource.path === 'META-INF/vatori/dev-bundle-entry/vaadin-text-field.js'
            || resource.path === 'vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js'
            || resource.path === 'vaadin-dev-bundle/webapp/VAADIN/build/commonjsHelpers-CUmg6egw.js'));
      }

      function componentModuleBlobUrl(path, urlsToRevoke, urlCache, executionStatus, extra) {
        const resource = componentFrontendResourceByPath(path);
        const stableIdentity = usesStableModuleIdentity(resource);
        const ownerScopedIdentity = extra
          && extra.owner === 'production-bundle-runtime'
          && extra.productionBundleRuntimeOwnerDependencyResolverVersion;
        const cache = ownerScopedIdentity
          ? (urlCache || frontendProductionOwnerImportUrlCache)
          : (stableIdentity ? frontendModuleIdentityUrlCache : urlCache);
        const cleanupPolicy = ownerScopedIdentity
          ? 'retain-for-window-lifetime-owner-dependency-v1_15'
          : (stableIdentity
          ? 'retain-for-window-lifetime-module-identity-v1_7'
          : 'revoke-on-module-graph-complete');
        const stageExtra = Object.assign({
          cleanupPolicy,
          moduleIdentityStrategy: ownerScopedIdentity
            ? 'owner-scoped-stable-blob-module-identity-v1_15'
            : (stableIdentity
            ? 'stable-resource-path-blob-module-identity-v1_7'
            : ''),
          moduleIdentityKey: path
        }, extra || {});
        if (cache && cache.has(path)) {
          const cachedUrl = stageFrontendArtifact(resource || { path }, '', executionStatus || 'staged',
            stableIdentity ? null : urlsToRevoke, cache, stageExtra);
          if (stableIdentity && urlCache) {
            urlCache.set(path, cachedUrl);
          }
          return cachedUrl;
        }
        let body = componentFrontendResourceBodies.get(path) || '';
        if (!body) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
            'missing-component-module-body path=' + path;
          return '';
        }
        if (resource && resource.devBundleResource === true) {
          installOpaqueOriginStorageFallbacks();
          installVaadinDevBundleGlobals();
        }
        if (resource && resource.devBundleResource === true) {
          body = rewriteVaadinDevBundleModule(body, path, urlsToRevoke, urlCache);
          if (!body) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
              || ('failed-dev-bundle-rewrite path=' + path);
            return '';
          }
        }
        if (stableIdentity) {
          frontendModuleIdentityResourcePaths.add(path);
        }
        const stagedUrl = stageFrontendArtifact(resource || { path }, body, executionStatus || 'staged',
          stableIdentity ? null : urlsToRevoke, cache, stageExtra);
        if (stableIdentity && urlCache) {
          urlCache.set(path, stagedUrl);
        }
        return stagedUrl;
      }

      function componentModuleFileName(path) {
        const index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
      }

      function rewriteStaticImportSpecifier(source, specifier, replacementUrl) {
        return source
          .split("from '" + specifier + "'").join("from '" + replacementUrl + "'")
          .split('from "' + specifier + '"').join('from "' + replacementUrl + '"')
          .split("import '" + specifier + "'").join("import '" + replacementUrl + "'")
          .split('import "' + specifier + '"').join('import "' + replacementUrl + '"');
      }

      function rewriteManifestImportSpecifiers(source, resource, urlsToRevoke, urlCache) {
        const rewrites = Array.isArray(resource && resource.importRewrites) ? resource.importRewrites : [];
        if (rewrites.length === 0) {
          return source;
        }
        let rewritten = source;
        const observedSpecifiers = [];
        const observedPaths = [];
        for (const rewrite of rewrites) {
          const specifier = rewrite && typeof rewrite.specifier === 'string' ? rewrite.specifier : '';
          const resourcePath = rewrite && typeof rewrite.resourcePath === 'string' ? rewrite.resourcePath : '';
          if (!specifier || !resourcePath) {
            continue;
          }
          const url = componentModuleBlobUrl(resourcePath, urlsToRevoke, urlCache);
          if (!url) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
              'missing-manifest-import-rewrite-resource';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
              || ('missing-manifest-import-rewrite-resource specifier=' + specifier
                + ' path=' + resourcePath);
            return '';
          }
          rewritten = rewriteStaticImportSpecifier(rewritten, specifier, url);
          observedSpecifiers.push(specifier);
          observedPaths.push(resourcePath);
        }
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewriteStrategy =
          resource.importRewriteStrategy || 'manifest-import-rewrite-v1_4';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewriteCount = observedSpecifiers.length;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewriteSpecifiers =
          observedSpecifiers.join(',');
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentImportRewritePaths =
          observedPaths.join(',');
        return rewritten;
      }

      function rewriteVaadinDevBundleModule(source, currentPath, urlsToRevoke, urlCache, ownerDependencyMode = false) {
        let rewritten = source;
        const currentResource = componentFrontendResourceByPath(currentPath);
        const productionOwnerDependency = ownerDependencyMode
          || !!(currentResource && currentResource.productionOwnerDependencyResource === true);
        for (const resource of componentFrontendResources) {
          const componentDependency = resource && resource.componentBundleDependency === true;
          const generatedLazyDependency = currentResource
            && currentResource.generatedLazyBundleChunk === true
            && resource
            && resource.generatedLazyBundleDependency === true;
          const ownerDependency = productionOwnerDependency
            && resource
            && isVaadinDevBundleBuildPath(resource.path);
          if (!resource
              || resource.devBundleResource !== true
              || (!componentDependency && !generatedLazyDependency && !ownerDependency)
              || resource.path === currentPath) {
            continue;
          }
          const specifier = './' + componentModuleFileName(resource.path);
          if (!rewritten.includes('"' + specifier + '"') && !rewritten.includes("'" + specifier + "'")) {
            continue;
          }
          const ownerDependencyExtra = ownerDependency ? {
            owner: 'production-bundle-runtime',
            cleanupPolicy: 'retain-for-window-lifetime-owner-dependency-v1_15',
            productionBundleRuntimeOwnerDependencyResolverVersion:
              currentResource.productionBundleRuntimeOwnerDependencyResolverVersion
              || resource.productionBundleRuntimeOwnerDependencyResolverVersion
              || 'flow-ipc-production-owner-deps-v1_15',
            productionBundleRuntimeOwnerDependencyResolverRoot:
              currentResource.productionBundleRuntimeOwnerDependencyResolverRoot
              || resource.productionBundleRuntimeOwnerDependencyResolverRoot
              || 'vaadin-dev-bundle/webapp/VAADIN/build/',
            productionBundleRuntimeOwnerDependencyResolverPolicy:
              currentResource.productionBundleRuntimeOwnerDependencyResolverPolicy
              || resource.productionBundleRuntimeOwnerDependencyResolverPolicy
              || 'owner-scoped-recursive-relative-import-graph-v1_15',
            productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts:
              currentResource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
              || resource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts
              || '',
            productionBundleRuntimeOwnerDependencyResolverAcceptableFailures:
              currentResource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
              || resource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
              || '',
            productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace:
              currentResource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
              || resource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
              || 'production-owner-deps-v1_15',
            productionBundleRuntimeOwnerDependencyResolverRootArtifact:
              currentResource.productionBundleRuntimeOwnerDependencyResolverRootArtifact
              || currentPath,
            moduleIdentityStrategy: 'owner-scoped-stable-blob-module-identity-v1_15',
            moduleIdentityKey: 'production-owner-deps-v1_15:' + resource.path,
            importResolutionStrategy:
              currentResource.productionBundleRuntimeOwnerDependencyResolverPolicy
              || resource.productionBundleRuntimeOwnerDependencyResolverPolicy
              || 'owner-scoped-recursive-relative-import-graph-v1_15'
          } : {};
          const generatedLazyExtra = generatedLazyDependency ? {
            cleanupPolicy: usesStableModuleIdentity(resource)
              ? 'retain-for-window-lifetime-module-identity-v1_7'
              : 'revoke-on-module-graph-complete',
            importResolutionStrategy: currentResource.generatedLazyImportResolutionStrategy
              || resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyResolverVersion: currentResource.generatedLazyResolverVersion
              || resource.generatedLazyResolverVersion
              || 'flow-ipc-generated-lazy-resolver-v1_8',
            generatedLazyExecutionVersion: currentResource.generatedLazyExecutionVersion
              || resource.generatedLazyExecutionVersion
              || 'flow-ipc-generated-lazy-v1_8',
            generatedLazyImportResolutionStrategy: currentResource.generatedLazyImportResolutionStrategy
              || resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyLoadOnDemandContractVersion: currentResource.generatedLazyLoadOnDemandContractVersion
              || resource.generatedLazyLoadOnDemandContractVersion
              || 'flow-ipc-loadondemand-v1_9',
            generatedLazyTriggerSource: currentResource.generatedLazyTriggerSource
              || resource.generatedLazyTriggerSource
              || 'diagnostic-preflight-and-direct-staged-import',
            generatedLazyApiOwner: currentResource.generatedLazyApiOwner
              || resource.generatedLazyApiOwner
              || 'window.Vaadin.Flow.loadOnDemand',
            generatedLazyApiInstallSource: 'dependency-staged-for-generated-flow-imports',
            generatedLazyRequestedChunk: currentResource.generatedLazyRequestedChunk
              || resource.generatedLazyRequestedChunk
              || 'vatori-flow-ipc-generated-lazy-v1_9-probe',
            generatedLazyRegistrySource: currentResource.generatedLazyRegistrySource
              || resource.generatedLazyRegistrySource
              || 'no-bootstrap-lazy-registry-observed',
            generatedLazyMappingSource: currentResource.generatedLazyMappingSource
              || resource.generatedLazyMappingSource
              || 'component-frontend-manifest-generated-lazy-v1_9',
            generatedLazyResolvedArtifactPath: resource.path,
            generatedLazyFlowClientTriggered: false,
            generatedLazyDiagnosticBridge: false,
            generatedLazyRelativeImportSpecifier: specifier,
            generatedLazyResolvedDependencyPath: resource.path,
            generatedLazyRelativeDependencyResolved: true,
            generatedLazyGraphProbe: currentResource.generatedLazyGraphProbe || resource.generatedLazyGraphProbe || '',
            generatedLazyBlockerVersion: currentResource.generatedLazyBlockerVersion
              || resource.generatedLazyBlockerVersion
              || 'flow-ipc-generated-lazy-v1_8',
            generatedLazySourceArtifact: currentPath,
            moduleIdentityStrategy: usesStableModuleIdentity(resource)
              ? 'stable-resource-path-blob-module-identity-v1_7'
              : '',
            moduleIdentityKey: resource.path
          } : {};
          const url = componentModuleBlobUrl(resource.path, urlsToRevoke, urlCache, 'staged',
            Object.assign({}, ownerDependencyExtra, generatedLazyExtra));
          if (!url) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
              generatedLazyDependency
                ? 'missing-v1_8-generated-lazy-relative-dependency'
                : 'missing-v1_4-dev-bundle-dependency';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
              || ((generatedLazyDependency
                ? 'missing-v1_8-generated-lazy-relative-dependency current='
                : 'missing-v1_4-dev-bundle-dependency current=') + currentPath
                + ' dependency=' + resource.path);
            return '';
          }
          rewritten = rewritten
            .split('"' + specifier + '"').join('"' + url + '"')
            .split("'" + specifier + "'").join("'" + url + "'");
          if (generatedLazyDependency) {
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeDependencyResolved = true;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolverVersion =
              generatedLazyExtra.generatedLazyResolverVersion;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionVersion =
              generatedLazyExtra.generatedLazyExecutionVersion;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyImportResolutionStrategy =
              generatedLazyExtra.generatedLazyImportResolutionStrategy;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeImportSpecifier = specifier;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedDependencyPath = resource.path;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlockerVersion =
              generatedLazyExtra.generatedLazyBlockerVersion;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFailureLayer =
              currentResource.failureLayer || resource.failureLayer || 'relative-dependency-resolved-flow-loadondemand-boundary';
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyNextResolutionStep =
              currentResource.nextResolutionStep || resource.nextResolutionStep || '';
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyModuleIdentityStrategy =
              generatedLazyExtra.moduleIdentityStrategy;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyModuleIdentityKey = resource.path;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyStagedDependencyGraph =
              currentPath + '=>' + specifier + '=>' + resource.path;
          }
        }
        return rewritten;
      }

      function memoryStorageForOpaqueOrigin() {
        const storage = new Map();
        const memoryStorage = {
          getItem(key) {
            const normalizedKey = String(key);
            return storage.has(normalizedKey) ? storage.get(normalizedKey) : null;
          },
          setItem(key, value) {
            storage.set(String(key), String(value));
          },
          removeItem(key) {
            storage.delete(String(key));
          },
          clear() {
            storage.clear();
          },
          key(index) {
            return Array.from(storage.keys())[Number(index)] || null;
          }
        };
        Object.defineProperty(memoryStorage, 'length', {
          get() {
            return storage.size;
          }
        });
        return memoryStorage;
      }

      function installOpaqueOriginStorageFallback(name) {
        try {
          window[name].getItem('__vatori_storage_probe__');
          return;
        } catch (error) {
          // WKWebView loadHTMLString uses an opaque about:blank origin. Vaadin's
          // dev-bundle chunks may touch Web Storage during module evaluation.
        }
        try {
          Object.defineProperty(window, name, {
            value: memoryStorageForOpaqueOrigin(),
            configurable: true
          });
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
            'opaque-origin-' + name + '-unavailable';
        }
      }

      function installOpaqueOriginStorageFallbacks() {
        installOpaqueOriginStorageFallback('localStorage');
        installOpaqueOriginStorageFallback('sessionStorage');
      }

      function installVaadinDevBundleGlobals() {
        window.Vaadin = window.Vaadin || {};
        window.Vaadin.devToolsPlugins = window.Vaadin.devToolsPlugins || [];
      }

      async function maybeExecuteVaadinWebComponentResource() {
        if (!componentFrontendExecutableResourcesReady()) {
          return;
        }
        const primaryResources = componentFrontendPrimaryResources();
        if (primaryResources.length === 0) {
          return;
        }
        for (const resource of primaryResources) {
          if (!resource || typeof resource.path !== 'string') {
            continue;
          }
          if (componentFrontendResourceExecutedPaths.has(resource.path)
              || componentFrontendResourceExecutionFailedPaths.has(resource.path)) {
            continue;
          }
          await executeComponentFrontendResource(resource);
        }
        refreshComponentFrontendResourceDiagnostics();
        refreshFlowClientEngineDiagnostics();
        scheduleFlowClientEnginePromotionRetry();
        setTimeout(scheduleAutoComponentEvents, 160);
      }

      function generatedLazyBundleResource() {
        return componentFrontendResources.find((entry) => entry
          && entry.generatedLazyBundleChunk === true
          && entry.artifactFamily === 'generated-lazy'
          && typeof entry.path === 'string')
          || null;
      }

      function textFieldDevBundleResource() {
        return componentFrontendResources.find((entry) => entry
          && entry.lookupKind === 'TextField-dev-bundle-resource-v1_4'
          && typeof entry.path === 'string')
          || null;
      }

      function flowLazyDependencyResource() {
        return componentFrontendResources.find((entry) => entry
          && entry.flowLazyDependencyResource === true
          && typeof entry.path === 'string')
          || null;
      }

      function productionBundleRuntimeResource() {
        return componentFrontendResources.find((entry) => entry
          && entry.productionBundleRuntimeArtifact === true
          && typeof entry.path === 'string')
          || null;
      }

      function observedGeneratedLoadOnDemandChunkIds() {
        const csv = window.VaadinDesktopIpcDiagnostics.generatedLazyChunkIds || '';
        return csv.split(',').map((entry) => entry.trim()).filter((entry) => entry);
      }

      function isObservedGeneratedLoadOnDemandChunk(chunkId) {
        return observedGeneratedLoadOnDemandChunkIds().includes(String(chunkId || ''));
      }

      function generatedLoadOnDemandChunkExpression(chunkId) {
        const expression = window.VaadinDesktopIpcDiagnostics.generatedLazyChunkExpression || '';
        if (expression.includes("'" + chunkId + "'")) {
          return expression;
        }
        return "return window.Vaadin.Flow.loadOnDemand('" + chunkId + "');";
      }

      function generatedLoadOnDemandChunkStageExtra(resource, chunkId, expression, extra) {
        return Object.assign({
          cleanupPolicy: usesStableModuleIdentity(resource)
            ? 'retain-for-window-lifetime-module-identity-v1_7'
            : 'revoke-after-generated-loadondemand-chunk',
          importResolutionStrategy: resource.generatedLazyImportResolutionStrategy
            || 'stable-relative-import-rewrite-generated-lazy-v1_8',
          generatedLazyResolverVersion: resource.generatedLazyResolverVersion
            || 'flow-ipc-generated-lazy-resolver-v1_8',
          generatedLazyExecutionVersion: resource.generatedLazyExecutionVersion
            || 'flow-ipc-generated-lazy-v1_8',
          generatedLazyImportResolutionStrategy: resource.generatedLazyImportResolutionStrategy
            || 'stable-relative-import-rewrite-generated-lazy-v1_8',
          generatedLazyLoadOnDemandContractVersion: resource.generatedLazyLoadOnDemandContractVersion
            || 'flow-ipc-loadondemand-v1_9',
          generatedLazyTriggerSource: resource.generatedLazyTriggerSource
            || 'diagnostic-preflight-and-direct-staged-import',
          generatedLazyApiOwner: resource.generatedLazyApiOwner || 'window.Vaadin.Flow.loadOnDemand',
          generatedLazyApiInstallSource: resource.generatedLazyApiInstallSource
            || 'generated-flow-imports-dev-bundle-after-direct-staged-import',
          generatedLazyRequestedChunk: resource.generatedLazyRequestedChunk
            || 'vatori-flow-ipc-generated-lazy-v1_9-probe',
          generatedLazyRegistrySource: resource.generatedLazyRegistrySource
            || 'no-bootstrap-lazy-registry-observed',
          generatedLazyMappingSource: resource.generatedLazyMappingSource
            || 'component-frontend-manifest-generated-lazy-v1_9',
          generatedLazyResolvedArtifactPath: resource.generatedLazyResolvedArtifactPath || resource.path,
          generatedLazyFlowClientTriggered: false,
          generatedLazyDiagnosticBridge: resource.generatedLazyDiagnosticBridge === true,
          generatedLazyChunkExpressionSource: resource.generatedLazyChunkExpressionSource
            || 'bootstrap-uidl-dependencies',
          generatedLazyChunkExpression: expression,
          generatedLazyChunkId: chunkId,
          generatedLazyChunkIds: window.VaadinDesktopIpcDiagnostics.generatedLazyChunkIds || chunkId,
          generatedLazyChunkTriggerOwner: resource.generatedLazyChunkTriggerOwner
            || 'visible-flow-client-dependencyloader',
          generatedLazyChunkTriggeredByFlowClient: true,
          generatedLazyChunkDiagnosticBridgeUsed: false,
          generatedLazyChunkDirectImportProbeUsed: false,
          generatedLazyChunkResolvedArtifactPath: resource.generatedLazyChunkResolvedArtifactPath || resource.path,
          generatedLazyChunkFailureLayer: resource.generatedLazyChunkFailureLayer
            || 'dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file',
          generatedLazyChunkBlockerVersion: resource.generatedLazyChunkBlockerVersion
            || 'flow-ipc-loadondemand-chunk-v1_11',
          generatedLazyChunkNextResolutionStep: resource.generatedLazyChunkNextResolutionStep || '',
          loadOnDemandChunkContractVersion: resource.loadOnDemandChunkContractVersion
            || 'flow-ipc-loadondemand-chunk-v1_11',
          loadOnDemandChunkApiOwner: resource.loadOnDemandChunkApiOwner || 'window.Vaadin.Flow.loadOnDemand',
          loadOnDemandChunkApiInstallSource: resource.loadOnDemandChunkApiInstallSource
            || 'flow-ipc-loadondemand-bridge-before-generated-imports',
          loadOnDemandChunkRegistrySource: resource.loadOnDemandChunkRegistrySource
            || 'uidl-expression-to-dev-bundle-generated-flow-imports-registry-v1_11',
          loadOnDemandChunkMappingSource: resource.loadOnDemandChunkMappingSource
            || 'flow-ipc-generated-loadondemand-chunk-map-v1_11',
          loadOnDemandChunkRequestedChunk: chunkId,
          loadOnDemandChunkResolvedArtifactPath: resource.loadOnDemandChunkResolvedArtifactPath || resource.path,
          loadOnDemandChunkModuleIdentity: resource.loadOnDemandChunkModuleIdentity || resource.path,
          loadOnDemandChunkTriggeredByFlowClient: true,
          loadOnDemandChunkFailureLayer: resource.loadOnDemandChunkFailureLayer
            || 'dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file',
          loadOnDemandChunkBlockerVersion: resource.loadOnDemandChunkBlockerVersion
            || 'flow-ipc-loadondemand-chunk-v1_11',
          loadOnDemandChunkNextResolutionStep: resource.loadOnDemandChunkNextResolutionStep || '',
          generatedLoadOnDemandChunkFileRegistryVersion: resource.generatedLoadOnDemandChunkFileRegistryVersion
            || 'flow-ipc-loadondemand-chunk-file-v1_12',
          generatedLoadOnDemandChunkFileRegistrySource: resource.generatedLoadOnDemandChunkFileRegistrySource
            || 'generated-flow-imports-body-parser-v1_12',
          generatedLoadOnDemandChunkFileMappingSource: resource.generatedLoadOnDemandChunkFileMappingSource
            || 'real-generated-flow-imports-loadondemand-table-v1_12',
          generatedLoadOnDemandGeneratedImportsArtifact:
            resource.generatedLoadOnDemandGeneratedImportsArtifact || resource.path,
          generatedLoadOnDemandSourceBuildMode: resource.generatedLoadOnDemandSourceBuildMode || '',
          generatedLoadOnDemandSourceArtifactFamily: resource.generatedLoadOnDemandSourceArtifactFamily
            || 'generated-lazy',
          generatedLoadOnDemandNoOpLoadOnDemand: resource.generatedLoadOnDemandNoOpLoadOnDemand === true,
          loadOnDemandTableShape: resource.loadOnDemandTableShape || '',
          chunkId: resource.chunkId || '',
          chunkImportSpecifier: resource.chunkImportSpecifier || '',
          chunkArtifactPath: resource.chunkArtifactPath || '',
          chunkContentHash: resource.chunkContentHash || '',
          chunkFileResolver: resource.chunkFileResolver
            || 'generated-flow-imports-relative-chunk-file-resolver-v1_12',
          chunkFileStatus: resource.chunkFileStatus || '',
          chunkFileBytes: Number.isFinite(resource.chunkFileBytes) ? resource.chunkFileBytes : 0,
          chunkFileSha256: resource.chunkFileSha256 || '',
          noChunkGeneratedReason: resource.noChunkGeneratedReason || '',
          loadOnDemandChunkFileRegistryVersion: resource.loadOnDemandChunkFileRegistryVersion
            || 'flow-ipc-loadondemand-chunk-file-v1_12',
          loadOnDemandChunkFileRegistrySource: resource.loadOnDemandChunkFileRegistrySource
            || resource.generatedLoadOnDemandChunkFileRegistrySource
            || 'generated-flow-imports-body-parser-v1_12',
          loadOnDemandChunkFileMappingSource: resource.loadOnDemandChunkFileMappingSource
            || resource.generatedLoadOnDemandChunkFileMappingSource
            || 'real-generated-flow-imports-loadondemand-table-v1_12',
          loadOnDemandChunkFileGeneratedImportsArtifact:
            resource.loadOnDemandChunkFileGeneratedImportsArtifact
            || resource.generatedLoadOnDemandGeneratedImportsArtifact
            || resource.path,
          loadOnDemandChunkFileNoOpLoadOnDemand:
            resource.loadOnDemandChunkFileNoOpLoadOnDemand === true
            || resource.generatedLoadOnDemandNoOpLoadOnDemand === true,
          loadOnDemandChunkFileTableShape: resource.loadOnDemandChunkFileTableShape
            || resource.loadOnDemandTableShape
            || '',
          loadOnDemandChunkFileResolver: resource.loadOnDemandChunkFileResolver
            || resource.chunkFileResolver
            || 'generated-flow-imports-relative-chunk-file-resolver-v1_12',
          loadOnDemandChunkFileStatus: resource.loadOnDemandChunkFileStatus || resource.chunkFileStatus || '',
          loadOnDemandChunkFileBytes: Number.isFinite(resource.loadOnDemandChunkFileBytes)
            ? resource.loadOnDemandChunkFileBytes
            : (Number.isFinite(resource.chunkFileBytes) ? resource.chunkFileBytes : 0),
          loadOnDemandChunkFileSha256: resource.loadOnDemandChunkFileSha256 || resource.chunkFileSha256 || '',
          loadOnDemandChunkFileTriggeredByFlowClient: true,
          loadOnDemandChunkFileStageObserved: resource.chunkFileStatus === 'available',
          loadOnDemandChunkFileExecuted: false,
          loadOnDemandChunkFileCallbackResolved: false,
          loadOnDemandChunkFileExecutionResult: '',
          loadOnDemandChunkFileSideEffectProof: resource.loadOnDemandChunkFileSideEffectProof || '',
          loadOnDemandChunkFileFailureLayer: resource.loadOnDemandChunkFileFailureLayer
            || (resource.chunkFileStatus === 'not-generated' ? 'chunk-file-not-generated' : ''),
          loadOnDemandChunkFileBrowserError: '',
          loadOnDemandChunkFileBlockerVersion: resource.loadOnDemandChunkFileBlockerVersion
            || 'flow-ipc-loadondemand-chunk-file-v1_12',
          loadOnDemandChunkFileNextResolutionStep: resource.loadOnDemandChunkFileNextResolutionStep || '',
          blockerClassification: resource.loadOnDemandChunkBlockerVersion
            || 'flow-ipc-loadondemand-chunk-v1_11',
          moduleIdentityStrategy: usesStableModuleIdentity(resource)
            ? 'stable-resource-path-blob-module-identity-v1_7'
            : '',
          moduleIdentityKey: resource.path
        }, extra || {});
      }

      function rememberGeneratedLoadOnDemandChunkRequest(resource, chunkId, expression) {
        const stageExtra = generatedLoadOnDemandChunkStageExtra(resource, chunkId, expression, {
          loadOnDemandChunkExecutionResult: 'resource-requested:' + resource.path
        });
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkTriggeredByFlowClient = true;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkTriggeredByFlowClient = true;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRequestedChunk = chunkId;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkResolvedArtifactPath =
          stageExtra.loadOnDemandChunkResolvedArtifactPath;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkContractVersion =
          stageExtra.loadOnDemandChunkContractVersion;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiOwner =
          stageExtra.loadOnDemandChunkApiOwner;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkApiInstallSource =
          stageExtra.loadOnDemandChunkApiInstallSource;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkRegistrySource =
          stageExtra.loadOnDemandChunkRegistrySource;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkMappingSource =
          stageExtra.loadOnDemandChunkMappingSource;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkModuleIdentity =
          stageExtra.loadOnDemandChunkModuleIdentity;
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkResolvedArtifactPath =
          stageExtra.generatedLazyChunkResolvedArtifactPath;
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer =
          stageExtra.generatedLazyChunkFailureLayer;
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkBlockerVersion =
          stageExtra.generatedLazyChunkBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.generatedLazyChunkNextResolutionStep =
          stageExtra.generatedLazyChunkNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer =
          stageExtra.loadOnDemandChunkFailureLayer;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBlockerVersion =
          stageExtra.loadOnDemandChunkBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkNextResolutionStep =
          stageExtra.loadOnDemandChunkNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistryVersion =
          stageExtra.generatedLoadOnDemandChunkFileRegistryVersion;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileRegistrySource =
          stageExtra.generatedLoadOnDemandChunkFileRegistrySource;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandChunkFileMappingSource =
          stageExtra.generatedLoadOnDemandChunkFileMappingSource;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandGeneratedImportsArtifact =
          stageExtra.generatedLoadOnDemandGeneratedImportsArtifact;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceBuildMode =
          stageExtra.generatedLoadOnDemandSourceBuildMode;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandSourceArtifactFamily =
          stageExtra.generatedLoadOnDemandSourceArtifactFamily;
        window.VaadinDesktopIpcDiagnostics.generatedLoadOnDemandNoOpLoadOnDemand =
          stageExtra.generatedLoadOnDemandNoOpLoadOnDemand === true;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandTableShape =
          stageExtra.loadOnDemandTableShape;
        window.VaadinDesktopIpcDiagnostics.chunkImportSpecifier =
          stageExtra.chunkImportSpecifier;
        window.VaadinDesktopIpcDiagnostics.chunkArtifactPath =
          stageExtra.chunkArtifactPath;
        window.VaadinDesktopIpcDiagnostics.chunkFileResolver =
          stageExtra.chunkFileResolver;
        window.VaadinDesktopIpcDiagnostics.chunkFileStatus =
          stageExtra.chunkFileStatus;
        window.VaadinDesktopIpcDiagnostics.chunkFileBytes =
          stageExtra.chunkFileBytes;
        window.VaadinDesktopIpcDiagnostics.chunkFileSha256 =
          stageExtra.chunkFileSha256;
        window.VaadinDesktopIpcDiagnostics.noChunkGeneratedReason =
          stageExtra.noChunkGeneratedReason;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileTriggeredByFlowClient = true;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileStageObserved =
          stageExtra.loadOnDemandChunkFileStageObserved === true;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer =
          stageExtra.loadOnDemandChunkFileFailureLayer;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBlockerVersion =
          stageExtra.loadOnDemandChunkFileBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileNextResolutionStep =
          stageExtra.loadOnDemandChunkFileNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileSideEffectProof =
          stageExtra.loadOnDemandChunkFileSideEffectProof;
        requestComponentFrontendResource(resource.path, resource.kind || '',
          'generated-loadondemand-chunk-v1_11', stageExtra);
      }

      function requestGeneratedLoadOnDemandChunkDependencies(resource, chunkId, expression) {
        const dependencyPaths = [];
        if (Array.isArray(resource.transitiveImports)) {
          for (const path of resource.transitiveImports) {
            if (typeof path === 'string' && path && path !== resource.path && !dependencyPaths.includes(path)) {
              dependencyPaths.push(path);
            }
          }
        }
        if (resource.generatedLazyResolvedDependencyPath
            && !dependencyPaths.includes(resource.generatedLazyResolvedDependencyPath)) {
          dependencyPaths.push(resource.generatedLazyResolvedDependencyPath);
        }
        for (const path of dependencyPaths) {
          const dependency = componentFrontendResourceByPath(path);
          if (!dependency || componentFrontendResourceBodies.has(path)) {
            continue;
          }
          requestComponentFrontendResource(path, dependency.kind || '',
            'generated-loadondemand-chunk-dependency-v1_11',
            generatedLoadOnDemandChunkStageExtra(resource, chunkId, expression, {
              generatedLazyChunkResolvedArtifactPath: path,
              loadOnDemandChunkResolvedArtifactPath: resource.path,
              loadOnDemandChunkExecutionResult: 'dependency-requested:' + path
            }));
        }
      }

      async function waitForGeneratedLoadOnDemandChunkDependencies(resource) {
        const waits = [];
        if (Array.isArray(resource.transitiveImports)) {
          for (const path of resource.transitiveImports) {
            if (typeof path !== 'string' || !path || path === resource.path) {
              continue;
            }
            if (componentFrontendResourceByPath(path)) {
              waits.push(waitForComponentFrontendResourceBody(path, 0));
            }
          }
        }
        if (resource.generatedLazyResolvedDependencyPath
            && componentFrontendResourceByPath(resource.generatedLazyResolvedDependencyPath)) {
          waits.push(waitForComponentFrontendResourceBody(resource.generatedLazyResolvedDependencyPath, 0));
        }
        if (waits.length > 0) {
          await Promise.all(waits);
        }
      }

      function waitForComponentFrontendResourceBody(path, attempts) {
        if (componentFrontendResourceBodies.has(path)) {
          return Promise.resolve(componentFrontendResourceBodies.get(path) || '');
        }
        if ((attempts || 0) >= 60) {
          return Promise.reject(new Error('generated-loadondemand-chunk-resource-timeout:' + path));
        }
        return new Promise((resolve, reject) => {
          setTimeout(() => {
            waitForComponentFrontendResourceBody(path, (attempts || 0) + 1).then(resolve, reject);
          }, 100);
        });
      }

      async function executeGeneratedLoadOnDemandChunk(chunkId) {
        const normalizedChunkId = String(chunkId || '');
        if (!normalizedChunkId) {
          return 0;
        }
        const resource = generatedLazyBundleResource();
        if (!resource) {
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecutionResult =
            'strict-blocker:generated-lazy-resource-missing';
          window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer =
            'generated-lazy-resource-not-advertised';
          return 0;
        }
        const expression = generatedLoadOnDemandChunkExpression(normalizedChunkId);
        const executionKey = resource.path || normalizedChunkId;
        if (generatedLoadOnDemandChunkPromises.has(executionKey)) {
          return generatedLoadOnDemandChunkPromises.get(executionKey);
        }
        const promise = (async () => {
          rememberGeneratedLoadOnDemandChunkRequest(resource, normalizedChunkId, expression);
          requestGeneratedLoadOnDemandChunkDependencies(resource, normalizedChunkId, expression);
          const urlsToRevoke = [];
          const urlCache = new Map();
          let stagedUrl = '';
          const source = await waitForComponentFrontendResourceBody(resource.path, 0);
          await waitForGeneratedLoadOnDemandChunkDependencies(resource);
          const stageExtra = generatedLoadOnDemandChunkStageExtra(resource, normalizedChunkId, expression, {
            loadOnDemandChunkExecutionResult: 'execution-attempt:' + resource.path
          });
          const noGeneratedChunkFile = stageExtra.chunkFileStatus === 'not-generated'
            && stageExtra.loadOnDemandChunkFileNoOpLoadOnDemand === true;
          try {
            installOpaqueOriginStorageFallbacks();
            installVaadinDevBundleGlobals();
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkStageObserved = true;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileTriggeredByFlowClient = true;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileStageObserved =
              stageExtra.loadOnDemandChunkFileStageObserved === true;
            stagedUrl = componentModuleBlobUrl(resource.path, urlsToRevoke, urlCache,
              'execution-attempt', stageExtra);
            if (!stagedUrl) {
              throw new Error('generated-loadondemand-chunk-stage-url-missing');
            }
            await import(stagedUrl);
            const generatedDelegate = window.Vaadin && window.Vaadin.Flow
              && typeof window.Vaadin.Flow.loadOnDemand === 'function'
              ? window.Vaadin.Flow.loadOnDemand
              : null;
            let delegateResult = 'delegate-missing';
            if (generatedDelegate && generatedDelegate.__vatoriGeneratedLoadOnDemandChunkBridge !== true) {
              try {
                const value = await generatedDelegate(normalizedChunkId);
                delegateResult = 'delegate-returned=' + String(value);
              } catch (delegateError) {
                delegateResult = 'delegate-failed='
                  + (delegateError && delegateError.message ? delegateError.message : String(delegateError));
              }
            }
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecuted = true;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecutionResult =
              'executed:' + resource.path + ';' + delegateResult + ';remaining-blocker='
              + stageExtra.loadOnDemandChunkFailureLayer;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBrowserError = '';
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer =
              stageExtra.loadOnDemandChunkFailureLayer;
            window.VaadinDesktopIpcDiagnostics.generatedLazyChunkFailureLayer =
              stageExtra.generatedLazyChunkFailureLayer;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecuted = false;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileCallbackResolved = true;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecutionResult =
              noGeneratedChunkFile
                ? 'strict-blocker:chunk-file-not-generated;' + delegateResult
                : 'strict-blocker:' + (stageExtra.chunkFileStatus || 'chunk-file-unresolved');
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBrowserError = '';
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer =
              stageExtra.loadOnDemandChunkFileFailureLayer
              || (noGeneratedChunkFile ? 'chunk-file-not-generated' : 'chunk-file-unresolved');
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileSideEffectProof =
              stageExtra.loadOnDemandChunkFileSideEffectProof
              || (noGeneratedChunkFile
                ? 'no-chunk-generated-by-real-generated-flow-imports-no-op-table'
                : 'generated-flow-imports-import-resolved-without-chunk-file-side-effect');
            recordFrontendStageOutcome(resource, stagedUrl, 'executed',
              generatedLoadOnDemandChunkStageExtra(resource, normalizedChunkId, expression, {
                loadOnDemandChunkExecutionResult:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecutionResult,
                loadOnDemandChunkFileStageObserved:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileStageObserved,
                loadOnDemandChunkFileExecuted:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecuted,
                loadOnDemandChunkFileCallbackResolved:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileCallbackResolved,
                loadOnDemandChunkFileExecutionResult:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecutionResult,
                loadOnDemandChunkFileSideEffectProof:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileSideEffectProof,
                loadOnDemandChunkFileFailureLayer:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer
              }));
            installGeneratedLoadOnDemandChunkBridge(generatedDelegate);
          } catch (error) {
            const message = error && error.message ? error.message : String(error);
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecuted = false;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecutionResult =
              'strict-blocker:' + resource.path;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkBrowserError = message;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFailureLayer =
              stageExtra.loadOnDemandChunkFailureLayer || 'generated-loadondemand-chunk-browser-execution';
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecuted = false;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileCallbackResolved = false;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecutionResult =
              'strict-blocker:' + (stageExtra.chunkFileStatus || 'chunk-file-import-failed');
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileBrowserError = message;
            window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer =
              stageExtra.loadOnDemandChunkFileFailureLayer || 'chunk-file-import-failed';
            recordFrontendStageOutcome(resource, stagedUrl, 'strict-blocker',
              generatedLoadOnDemandChunkStageExtra(resource, normalizedChunkId, expression, {
                browserError: message,
                loadOnDemandChunkBrowserError: message,
                loadOnDemandChunkExecutionResult:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkExecutionResult,
                loadOnDemandChunkFileStageObserved:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileStageObserved,
                loadOnDemandChunkFileExecuted: false,
                loadOnDemandChunkFileCallbackResolved: false,
                loadOnDemandChunkFileBrowserError: message,
                loadOnDemandChunkFileExecutionResult:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileExecutionResult,
                loadOnDemandChunkFileFailureLayer:
                  window.VaadinDesktopIpcDiagnostics.loadOnDemandChunkFileFailureLayer
              }));
          } finally {
            refreshFrontendStagingDiagnostics();
            for (const url of urlsToRevoke) {
              URL.revokeObjectURL(url);
            }
          }
          return 0;
        })();
        generatedLoadOnDemandChunkPromises.set(executionKey, promise);
        return promise;
      }

      function installGeneratedLoadOnDemandChunkBridge(delegate) {
        window.Vaadin = window.Vaadin || {};
        window.Vaadin.Flow = window.Vaadin.Flow || {};
        const existing = typeof window.Vaadin.Flow.loadOnDemand === 'function'
          ? window.Vaadin.Flow.loadOnDemand
          : null;
        if (existing && existing.__vatoriGeneratedLoadOnDemandChunkBridge === true) {
          return;
        }
        const delegatedLoadOnDemand = delegate || existing || null;
        if (delegatedLoadOnDemand && delegatedLoadOnDemand.__vatoriGeneratedLoadOnDemandChunkBridge !== true) {
          window.__vatoriGeneratedLoadOnDemandDelegate = delegatedLoadOnDemand;
        }
        const bridge = function(key) {
          const chunkId = String(key || '');
          const delegated = window.__vatoriGeneratedLoadOnDemandDelegate || null;
          if (!isObservedGeneratedLoadOnDemandChunk(chunkId)) {
            if (typeof delegated === 'function') {
              return Promise.resolve(delegated.call(this, key));
            }
            return Promise.resolve(0);
          }
          return executeGeneratedLoadOnDemandChunk(chunkId);
        };
        bridge.__vatoriGeneratedLoadOnDemandChunkBridge = true;
        window.Vaadin.Flow.loadOnDemand = bridge;
      }

      function flowLazyDependencyPathFromUrl(url) {
        const resource = flowLazyDependencyResource();
        if (!resource || typeof url !== 'string' || !url) {
          return '';
        }
        const expression = typeof resource.lazyDependencyExpression === 'string'
          ? resource.lazyDependencyExpression
          : '';
        const expressionPath = expression.startsWith('context://')
          ? expression.substring('context://'.length)
          : expression;
        if (url.includes(resource.path) || (expressionPath && url.includes(expressionPath))) {
          return resource.path;
        }
        return '';
      }

      function installFlowIpcDependencyLoaderBridge() {
        if (window.__vatoriFlowIpcDependencyLoaderBridgeInstalled) {
          return;
        }
        window.__vatoriFlowIpcDependencyLoaderBridgeInstalled = true;
        const originalAppendChild = Node.prototype.appendChild;
        Node.prototype.appendChild = function(child) {
          const tagName = child && child.tagName ? String(child.tagName).toLowerCase() : '';
          if (tagName === 'script' || tagName === 'link') {
            const url = tagName === 'script' ? (child.src || '') : (child.href || '');
            const path = flowLazyDependencyPathFromUrl(url);
            if (path) {
              requestFlowLazyDependencyResourceFromElement(path, child, url);
              return child;
            }
          }
          return originalAppendChild.call(this, child);
        };
      }

      function requestFlowLazyDependencyResourceFromElement(path, element, requestedUrl) {
        const resource = componentFrontendResourceByPath(path);
        if (!resource) {
          return;
        }
        let pending = flowLazyDependencyPendingElements.get(path);
        if (!pending) {
          pending = { elements: [], requestedUrls: [] };
          flowLazyDependencyPendingElements.set(path, pending);
        }
        pending.elements.push(element);
        pending.requestedUrls.push(requestedUrl || '');
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyObserved = true;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyRequested = true;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyVersion =
          resource.lazyDependencyVersion || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyVersion;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencySource =
          resource.lazyDependencySource || window.VaadinDesktopIpcDiagnostics.flowLazyDependencySource;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyMode =
          resource.lazyDependencyMode || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyMode;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExpression =
          resource.lazyDependencyExpression || requestedUrl || '';
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyChunkId =
          resource.lazyDependencyChunkId || window.VaadinDesktopIpcDiagnostics.flowLazyDependencyChunkId;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyResolvedPath =
          resource.lazyDependencyResolvedPath || path;
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggerOwner =
          resource.lazyDependencyTriggerOwner || 'visible-flow-client-dependencyloader';
        window.VaadinDesktopIpcDiagnostics.flowLazyDependencyTriggeredByFlowClient =
          resource.lazyDependencyTriggeredByFlowClient === true;
        if (componentFrontendResourceBodies.has(path)) {
          setTimeout(maybeExecuteFlowLazyDependencyResource, 0);
          return;
        }
        requestComponentFrontendResource(path, resource.kind || '', 'flow-lazy-dependency-loader');
      }

      function dispatchFlowLazyDependencyElement(path, type) {
        const pending = flowLazyDependencyPendingElements.get(path);
        if (!pending || !Array.isArray(pending.elements)) {
          return;
        }
        for (const element of pending.elements) {
          try {
            element.dispatchEvent(new Event(type));
          } catch (error) {
            // The Flow ResourceLoader listener is best-effort here; diagnostics
            // below still records whether the IPC-resolved module executed.
          }
        }
      }

      async function maybeExecuteFlowLazyDependencyResource() {
        if (flowLazyDependencyExecutionAttempted) {
          return;
        }
        const resource = flowLazyDependencyResource();
        if (!resource || !flowLazyDependencyPendingElements.has(resource.path)) {
          return;
        }
        const source = componentFrontendResourceBodies.get(resource.path) || '';
        if (!source) {
          return;
        }
        flowLazyDependencyExecutionAttempted = true;
        const urlsToRevoke = [];
        let stagedUrl = '';
        const stageExtra = {
          cleanupPolicy: resource.cleanupOwnership || 'revoke-after-lazy-dependency-load',
          runtimeResolverContractVersion: resource.runtimeResolverContractVersion || 'flow-ipc-frontend-runtime-resolver-v1_10',
          logicalRequestId: resource.logicalRequestId || 'flow-lazy-dependency:v1_10',
          pathMapping: resource.pathMapping || 'context-url-to-ipc-resource-path',
          stageUrlPolicy: 'host-owned-resource-scheme-file-backed-v1_17',
          cachePolicy: resource.cachePolicy || 'single-trigger-per-resource-path',
          cleanupOwnership: 'cleanup-owned-window-lifetime-file-backed-v1_17',
          blockerClassification: resource.blockerClassification || 'none',
          lazyDependencyVersion: resource.lazyDependencyVersion || 'flow-ipc-lazy-dependency-v1_10',
          lazyDependencySource: resource.lazyDependencySource || 'flow-uidl-dependencyloader-loadmode-lazy-v1_10',
          lazyDependencyMode: resource.lazyDependencyMode || 'LoadMode.LAZY',
          lazyDependencyExpression: resource.lazyDependencyExpression || '',
          lazyDependencyChunkId: resource.lazyDependencyChunkId || '',
          lazyDependencyResolvedPath: resource.lazyDependencyResolvedPath || resource.path,
          lazyDependencyTriggerOwner: resource.lazyDependencyTriggerOwner || 'visible-flow-client-dependencyloader',
          lazyDependencyTriggeredByFlowClient: resource.lazyDependencyTriggeredByFlowClient === true,
          frontendResourceSchemeVersion: 'flow-ipc-resource-scheme-v1_17',
          frontendResourceLoaderMode: 'custom-scheme-file-backed',
          frontendResourceSchemeName: 'vatori-resource',
          frontendResourceSchemeHandlerInstalled: true,
          frontendResourceLoaderNoLocalhost: true,
          frontendResourceFileBacked: true,
          stagingMode: 'custom-scheme',
          relativeImportBase: resource.path,
          relativeImportResolutionMode: 'scheme-url-native-relative-base-v1_17',
          resolvedRelativeImportPaths: '',
          missingRelativeImportPaths: '',
          mimeType: 'text/javascript; charset=utf-8',
          noLocalhostProof: 'custom-scheme-served-by-wkurlschemehandler-no-http-v1_17',
          failureLayer: ''
        };
        try {
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyStageObserved = true;
          stagedUrl = await stageHostOwnedFrontendArtifact(resource, source, 'execution-attempt', stageExtra);
          if (!stagedUrl) {
            stageExtra.stagingMode = 'blob-fallback';
            stageExtra.blobFallbackReason = stageExtra.blobFallbackReason || 'host-owned-resource-stage-empty-url';
            stageExtra.blobFallbackBlockedLayer = 'host-owned-resource-stage';
            stageExtra.blobFallbackNextResolutionStep =
              'keep the Flow lazy dependency Blob fallback until the host-owned resource scheme returns a module URL';
            stagedUrl = stageFrontendArtifact(resource, source, 'execution-attempt', urlsToRevoke, null, stageExtra);
            if (!stagedUrl) {
              throw new Error('flow-lazy-dependency-staging-failed');
            }
          }
          await import(stagedUrl);
          if (stagedUrl.startsWith('vatori-resource:')) {
            stageExtra.frontendResourceSchemeRequestObserved = true;
            stageExtra.frontendResourceSchemeResponseObserved = true;
          }
          const marker = window.VaadinDesktopIpcLazyDependencyProbe || {};
          if (marker.executed !== true) {
            throw new Error('flow-lazy-dependency-marker-missing');
          }
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecuted = true;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecutionResult =
            'executed:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyBrowserError = '';
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyFailureLayer = '';
          recordFrontendStageOutcome(resource, stagedUrl, 'executed', stageExtra);
          dispatchFlowLazyDependencyElement(resource.path, 'load');
        } catch (error) {
          const message = error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecuted = false;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyExecutionResult =
            'execution-failed:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyBrowserError = message;
          window.VaadinDesktopIpcDiagnostics.flowLazyDependencyFailureLayer =
            'flow-lazy-dependency-browser-execution';
          recordFrontendStageOutcome(resource, stagedUrl, 'execution-failed', Object.assign({}, stageExtra, {
            failureLayer: 'flow-lazy-dependency-browser-execution',
            browserError: message,
            blockerClassification: 'flow-ipc-lazy-dependency-v1_10'
          }));
          dispatchFlowLazyDependencyElement(resource.path, 'error');
        } finally {
          refreshFrontendStagingDiagnostics();
          for (const url of urlsToRevoke) {
            URL.revokeObjectURL(url);
          }
        }
      }

      async function maybeStageProductionBundleRuntimeResource() {
        if (productionBundleRuntimeStageAttempted) {
          return;
        }
        const resource = productionBundleRuntimeResource();
        if (!resource) {
          return;
        }
        const source = componentFrontendResourceBodies.get(resource.path) || '';
        if (!source) {
          return;
        }
        productionBundleRuntimeStageAttempted = true;
        const ownerIdentity = resource.productionBundleRuntimeOwnerModuleIdentity
          || ('production-owner-v1_13:' + resource.path);
        const registryBefore = vaadinCustomElementRegistrySnapshot();
        const ownerFailureLayer = resource.productionBundleRuntimeOwnerFailureLayer
          || 'production-owner-runtime-contract-missing';
        const ownerBlockerVersion = resource.productionBundleRuntimeOwnerBlockerVersion
          || 'flow-ipc-production-owner-v1_13';
        const ownerExecutionResult = 'strict-blocker:' + ownerFailureLayer + ':' + resource.path;
        const ownerReuseReason = resource.componentProductionOwnerReuseReason
          || 'production owner import disabled until production indexhtml side effects can be owned without duplicate custom element definitions';
        const stageExtra = {
          owner: 'production-bundle-runtime',
          cleanupPolicy: resource.productionBundleRuntimeCleanupPolicy
            || 'retain-for-window-lifetime-module-identity-v1_7',
          runtimeResolverContractVersion: resource.runtimeResolverContractVersion
            || 'flow-ipc-frontend-runtime-resolver-v1_10',
          logicalRequestId: resource.logicalRequestId || 'production-runtime:indexhtml:v1_10',
          pathMapping: resource.pathMapping || 'stats-bundle-import-to-dev-bundle-artifact',
          stageUrlPolicy: resource.stageUrlPolicy || 'stable-module-identity-blob-url',
          cachePolicy: resource.cachePolicy || 'retain-per-resource-path',
          cleanupOwnership: resource.cleanupOwnership || 'window-lifetime-module-identity-cache',
          blockerClassification: resource.blockerClassification || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeVersion: resource.productionBundleRuntimeVersion
            || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeRegistrySource: resource.productionBundleRuntimeRegistrySource
            || 'vaadin-dev-bundle-stats-runtime-registry-v1_10',
          productionBundleRuntimeStatsSource: resource.productionBundleRuntimeStatsSource || '',
          productionBundleRuntimeSelectedArtifact: resource.productionBundleRuntimeSelectedArtifact || resource.path,
          productionBundleRuntimeDependencyChain: resource.productionBundleRuntimeDependencyChain || '',
          productionBundleRuntimeModuleIdentity: resource.productionBundleRuntimeModuleIdentity || resource.path,
          productionBundleRuntimeExecutionPolicy: resource.productionBundleRuntimeExecutionPolicy
            || 'stage-execution-attempt-production-bundle-runtime-v1_10',
          productionBundleRuntimeBlockerVersion: resource.productionBundleRuntimeBlockerVersion
            || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeFailureLayer: resource.productionBundleRuntimeFailureLayer
            || 'production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports',
          productionBundleRuntimeNextResolutionStep: resource.productionBundleRuntimeNextResolutionStep || '',
          productionBundleRuntimeOwnerVersion: resource.productionBundleRuntimeOwnerVersion
            || 'flow-ipc-production-owner-v1_13',
          productionBundleRuntimeOwner: resource.productionBundleRuntimeOwner
            || 'flow-ipc-production-bundle-runtime',
          productionBundleRuntimeEntryArtifact: resource.productionBundleRuntimeEntryArtifact || resource.path,
          productionBundleRuntimeSource: resource.productionBundleRuntimeSource || 'vaadin-dev-bundle-stats',
          productionBundleRuntimeSelectedBy: resource.productionBundleRuntimeSelectedBy
            || 'stats-entrypoint-or-focused-production-entry',
          productionBundleRuntimeOwnerModuleIdentity: ownerIdentity,
          productionBundleRuntimeOwnerLifecycle: resource.productionBundleRuntimeOwnerLifecycle
            || 'created,staged,import-skipped,side-effect-unobservable,not-reused,cleanup-owned',
          productionBundleRuntimeOwnerExecutionPolicy: resource.productionBundleRuntimeOwnerExecutionPolicy
            || 'stage-owner-contract-production-bundle-runtime-v1_13',
          productionBundleRuntimeOwnerStageUrlPolicy: resource.productionBundleRuntimeOwnerStageUrlPolicy
            || 'temporary-blob-module-url-owner-namespace-v1_13',
          productionBundleRuntimeOwnerCachePolicy: resource.productionBundleRuntimeOwnerCachePolicy
            || 'retain-per-owner-module-identity',
          productionBundleRuntimeOwnerCacheStatus: frontendProductionOwnerUrlCache.has(resource.path) ? 'hit' : 'miss',
          productionBundleRuntimeOwnerCleanupStrategy: resource.productionBundleRuntimeOwnerCleanupStrategy
            || 'cleanup-owned-window-lifetime-module-identity',
          productionBundleRuntimeOwnerExecutionResult: ownerExecutionResult,
          productionBundleRuntimeOwnerSideEffectProof: resource.productionBundleRuntimeOwnerSideEffectProof
            || 'owner-stage-resolved-import-intentionally-disabled-until-production-runtime-contract-can-own-indexhtml-side-effects',
          productionBundleRuntimeOwnerFailureLayer: ownerFailureLayer,
          productionBundleRuntimeOwnerBrowserError: resource.productionBundleRuntimeOwnerBrowserError || '',
          productionBundleRuntimeOwnerBlockerVersion: ownerBlockerVersion,
          productionBundleRuntimeOwnerNextResolutionStep: resource.productionBundleRuntimeOwnerNextResolutionStep || '',
          productionBundleRuntimeOwnerRegistryBefore: registryBefore,
          productionBundleRuntimeOwnerRegistryAfter: registryBefore,
          productionBundleRuntimeOwnerDuplicateTag: '',
          productionBundleRuntimeOwnerFirstDefinitionOwner: '',
          productionBundleRuntimeOwnerSecondDefinitionOwner: '',
          componentProductionOwnerReused: resource.componentProductionOwnerReused === true,
          componentProductionOwnerReuseReason: ownerReuseReason,
          componentProductionOwnerDependencyPaths: resource.componentProductionOwnerDependencyPaths || resource.path,
          moduleIdentityStrategy: 'production-owner-module-identity-v1_13',
          moduleIdentityKey: ownerIdentity,
          blockerClassification: ownerBlockerVersion
        };
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeObserved = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStageAttempted = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeVersion =
          stageExtra.productionBundleRuntimeVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeRegistrySource =
          stageExtra.productionBundleRuntimeRegistrySource;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStatsSource =
          stageExtra.productionBundleRuntimeStatsSource;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeSelectedArtifact =
          stageExtra.productionBundleRuntimeSelectedArtifact;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeDependencyChain =
          stageExtra.productionBundleRuntimeDependencyChain;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeModuleIdentity =
          stageExtra.productionBundleRuntimeModuleIdentity;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeFailureLayer =
          stageExtra.productionBundleRuntimeFailureLayer;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeNextResolutionStep =
          stageExtra.productionBundleRuntimeNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerObserved = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageObserved = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImported = false;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlocker = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerVersion =
          stageExtra.productionBundleRuntimeOwnerVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwner =
          stageExtra.productionBundleRuntimeOwner;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerEntryArtifact =
          stageExtra.productionBundleRuntimeEntryArtifact;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSource =
          stageExtra.productionBundleRuntimeSource;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSelectedBy =
          stageExtra.productionBundleRuntimeSelectedBy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerModuleIdentity =
          stageExtra.productionBundleRuntimeOwnerModuleIdentity;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerLifecycle =
          stageExtra.productionBundleRuntimeOwnerLifecycle;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy =
          stageExtra.productionBundleRuntimeOwnerExecutionPolicy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageUrlPolicy =
          stageExtra.productionBundleRuntimeOwnerStageUrlPolicy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCachePolicy =
          stageExtra.productionBundleRuntimeOwnerCachePolicy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCacheStatus =
          stageExtra.productionBundleRuntimeOwnerCacheStatus;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerCleanupStrategy =
          stageExtra.productionBundleRuntimeOwnerCleanupStrategy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionResult =
          stageExtra.productionBundleRuntimeOwnerExecutionResult;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSideEffectProof =
          stageExtra.productionBundleRuntimeOwnerSideEffectProof;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer =
          stageExtra.productionBundleRuntimeOwnerFailureLayer;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBrowserError =
          stageExtra.productionBundleRuntimeOwnerBrowserError;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlockerVersion =
          stageExtra.productionBundleRuntimeOwnerBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep =
          stageExtra.productionBundleRuntimeOwnerNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerRegistryBefore =
          stageExtra.productionBundleRuntimeOwnerRegistryBefore;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerRegistryAfter =
          stageExtra.productionBundleRuntimeOwnerRegistryAfter;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDuplicateTag =
          stageExtra.productionBundleRuntimeOwnerDuplicateTag;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFirstDefinitionOwner =
          stageExtra.productionBundleRuntimeOwnerFirstDefinitionOwner;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSecondDefinitionOwner =
          stageExtra.productionBundleRuntimeOwnerSecondDefinitionOwner;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused =
          stageExtra.componentProductionOwnerReused === true;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason =
          stageExtra.componentProductionOwnerReuseReason;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths =
          stageExtra.componentProductionOwnerDependencyPaths;
        const stagedUrl = stageFrontendArtifact(resource, source, 'execution-attempt',
          null, frontendProductionOwnerUrlCache, stageExtra);
        if (!stagedUrl) {
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult =
            'execution-failed:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError =
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
            || 'production-runtime-stage-url-missing';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionResult =
            'execution-failed:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBrowserError =
            window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError;
          recordFrontendStageOutcome(resource, '', 'execution-failed', Object.assign({}, stageExtra, {
            productionBundleRuntimeOwnerExecutionResult: 'execution-failed:' + resource.path,
            productionBundleRuntimeOwnerBrowserError:
              window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError,
            browserError: window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError
          }));
          return;
        }
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult =
          'execution-attempt:' + resource.path;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError = '';
        recordFrontendStageOutcome(resource, stagedUrl, 'execution-attempt', stageExtra);
      }

      function relativeImportSpecifiers(source) {
        const specifiers = [];
        const patterns = [
          /(?:import|export)\s+[^'"]*?\s+from\s+['"](\.\/[^'"]+)['"]/g,
          /import\s+['"](\.\/[^'"]+)['"]/g,
          /import\s*\(\s*['"](\.\/[^'"]+)['"]\s*\)/g
        ];
        for (const pattern of patterns) {
          let match = pattern.exec(source);
          while (match) {
            if (match[1]) {
              specifiers.push(match[1]);
            }
            match = pattern.exec(source);
          }
        }
        return Array.from(new Set(specifiers));
      }

      function resolveRelativeDevBundlePath(currentPath, specifier) {
        if (typeof currentPath !== 'string' || typeof specifier !== 'string' || !specifier.startsWith('./')) {
          return '';
        }
        const lastSlash = currentPath.lastIndexOf('/');
        const base = lastSlash >= 0 ? currentPath.substring(0, lastSlash + 1) : '';
        const withoutQuery = specifier.substring(2).split('#')[0].split('?')[0];
        return base + withoutQuery;
      }

      function isVaadinDevBundleBuildPath(path) {
        return typeof path === 'string'
          && path.startsWith('vaadin-dev-bundle/webapp/VAADIN/build/');
      }

      function isProductionOwnerDependencyResource(resource) {
        return !!(resource
          && (resource.productionBundleRuntimeArtifact === true
            || resource.productionOwnerDependencyResource === true
            || resource.productionBundleRuntimeOwnerDependencyResolverVersion === 'flow-ipc-production-owner-deps-v1_15'));
      }

      function resolveRelativeDevBundleArtifactPath(currentPath, specifier) {
        const directPath = resolveRelativeDevBundlePath(currentPath, specifier);
        if (!directPath) {
          return '';
        }
        if (componentFrontendResourceByPath(directPath)) {
          return directPath;
        }
        if (!directPath.endsWith('.js') && componentFrontendResourceByPath(directPath + '.js')) {
          return directPath + '.js';
        }
        return directPath;
      }

      function productionOwnerImportPreflight(resource, source) {
        const queue = [{ path: resource.path, source, depth: 0 }];
        const visited = new Set();
        const specifierKeys = new Set();
        const resolvedPaths = new Set();
        const missingPaths = new Set();
        const missingSpecifiers = new Set();
        const pendingPaths = new Set();
        const classifications = new Map();
        let maxDepth = 0;
        while (queue.length > 0) {
          const entry = queue.shift();
          if (!entry || !entry.path || visited.has(entry.path)) {
            continue;
          }
          visited.add(entry.path);
          maxDepth = Math.max(maxDepth, Number(entry.depth || 0));
          const entrySource = typeof entry.source === 'string' ? entry.source : '';
          const relativeSpecifiers = relativeImportSpecifiers(entrySource);
          for (const specifier of relativeSpecifiers) {
            const key = entry.path + '=>' + specifier;
            specifierKeys.add(key);
            const candidatePath = resolveRelativeDevBundleArtifactPath(entry.path, specifier);
            if (!candidatePath || !isVaadinDevBundleBuildPath(candidatePath)) {
              const classification = 'unsupported-extension';
              classifications.set(key, specifier + '=>:' + classification);
              missingSpecifiers.add(specifier);
              continue;
            }
            const dependencyResource = componentFrontendResourceByPath(candidatePath);
            if (!dependencyResource) {
              missingPaths.add(candidatePath);
              missingSpecifiers.add(specifier);
              classifications.set(key, specifier + '=>' + candidatePath + ':missing-real-artifact');
              continue;
            }
            resolvedPaths.add(candidatePath);
            classifications.set(key, specifier + '=>' + candidatePath + ':resolved-existing-artifact');
            const dependencySource = componentFrontendResourceBodies.get(candidatePath) || '';
            if (!dependencySource) {
              pendingPaths.add(candidatePath);
              continue;
            }
            queue.push({ path: candidatePath, source: dependencySource, depth: Number(entry.depth || 0) + 1 });
          }
        }
        const resolved = Array.from(resolvedPaths).sort();
        const missing = Array.from(missingPaths).sort();
        const missingSpecifiersList = Array.from(missingSpecifiers).sort();
        const pending = Array.from(pendingPaths).sort();
        let dependencyRewriteResult = 'closed:no-relative-imports';
        if (pending.length > 0) {
          dependencyRewriteResult = 'partial-pending:' + pending.join(',');
        } else if (missing.length > 0) {
          dependencyRewriteResult = 'missing-real-artifact:' + missing.join(',');
        } else if (resolved.length > 0) {
          dependencyRewriteResult = 'closed:' + resolved.join(',');
        }
        return {
          resolvedPaths: resolved,
          unresolvedSpecifiers: missingSpecifiersList,
          missingDependencyPaths: missing,
          missingSpecifiers: missingSpecifiersList,
          pendingPaths: pending,
          dependencyRewriteResult,
          dependencyGraphClosed: pending.length === 0,
          totalSpecifierCount: specifierKeys.size,
          resolvedCount: resolved.length,
          missingCount: missing.length,
          stagedCount: resolved.length,
          maxDepth,
          dependencyClassifications: Array.from(classifications.values()).sort()
        };
      }

      function requestProductionOwnerPendingDependencies(preflight) {
        const pending = preflight && Array.isArray(preflight.pendingPaths) ? preflight.pendingPaths : [];
        for (const path of pending) {
          const dependency = componentFrontendResourceByPath(path);
          if (dependency) {
            requestComponentFrontendResource(path, dependency.kind || '', 'production-owner-dependency-v1_15');
          }
        }
      }

      function stageProductionOwnerResolvedDependencies(preflight, rootResource) {
        const resolved = preflight && Array.isArray(preflight.resolvedPaths) ? preflight.resolvedPaths : [];
        const resolverVersion = rootResource.productionBundleRuntimeOwnerDependencyResolverVersion
          || 'flow-ipc-production-owner-deps-v1_15';
        for (const path of resolved) {
          if (path === rootResource.path) {
            continue;
          }
          const dependency = componentFrontendResourceByPath(path);
          const body = componentFrontendResourceBodies.get(path) || '';
          if (!dependency || !body) {
            continue;
          }
          stageFrontendArtifact(dependency, body, 'staged', null, frontendProductionOwnerImportUrlCache, {
            owner: 'production-bundle-runtime',
            cleanupPolicy: 'retain-for-window-lifetime-owner-dependency-v1_15',
            productionBundleRuntimeOwnerDependencyResolverVersion: resolverVersion,
            productionBundleRuntimeOwnerDependencyResolverRoot:
              rootResource.productionBundleRuntimeOwnerDependencyResolverRoot
              || 'vaadin-dev-bundle/webapp/VAADIN/build/',
            productionBundleRuntimeOwnerDependencyResolverPolicy:
              rootResource.productionBundleRuntimeOwnerDependencyResolverPolicy
              || 'owner-scoped-recursive-relative-import-graph-v1_15',
            productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts:
              rootResource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts || '',
            productionBundleRuntimeOwnerDependencyResolverAcceptableFailures:
              rootResource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures || '',
            productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace:
              rootResource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
              || 'production-owner-deps-v1_15',
            productionBundleRuntimeOwnerDependencyResolverRootArtifact: rootResource.path,
            productionBundleRuntimeOwnerDependencyGraphClosed: preflight.dependencyGraphClosed === true,
            productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount: preflight.totalSpecifierCount,
            productionBundleRuntimeOwnerDependencyGraphResolvedCount: preflight.resolvedCount,
            productionBundleRuntimeOwnerDependencyGraphMissingCount: preflight.missingCount,
            productionBundleRuntimeOwnerDependencyGraphStagedCount: preflight.stagedCount,
            productionBundleRuntimeOwnerDependencyGraphMaxDepth: preflight.maxDepth,
            productionBundleRuntimeOwnerDependencyClassifications: preflight.dependencyClassifications.join('|'),
            productionBundleRuntimeOwnerImportResolvedDependencyPaths: preflight.resolvedPaths.join(','),
            productionBundleRuntimeOwnerImportMissingDependencyPaths: preflight.missingDependencyPaths.join(','),
            productionBundleRuntimeOwnerImportMissingSpecifiers: preflight.missingSpecifiers.join(','),
            productionBundleRuntimeOwnerImportDependencyRewriteResult: preflight.dependencyRewriteResult,
            productionBundleRuntimeOwnerImportStageCount: preflight.resolvedPaths.length,
            moduleIdentityStrategy: 'owner-scoped-stable-blob-module-identity-v1_15',
            moduleIdentityKey: 'production-owner-deps-v1_15:' + path,
            importResolutionStrategy: rootResource.productionBundleRuntimeOwnerDependencyResolverPolicy
              || 'owner-scoped-recursive-relative-import-graph-v1_15'
          });
        }
      }

      function vaadinGlobalSnapshot() {
        const hasVaadin = !!window.Vaadin;
        const hasFlow = !!(window.Vaadin && window.Vaadin.Flow);
        const loadOnDemand = hasFlow && typeof window.Vaadin.Flow.loadOnDemand === 'function'
          ? window.Vaadin.Flow.loadOnDemand
          : null;
        let loadOnDemandOwner = '';
        if (loadOnDemand) {
          if (loadOnDemand.__vatoriGeneratedLoadOnDemandChunkBridge === true) {
            loadOnDemandOwner = 'vatori-generated-loadondemand-chunk-bridge';
          } else {
            loadOnDemandOwner = loadOnDemand.name ? ('function:' + loadOnDemand.name) : 'function:anonymous';
          }
        }
        return 'Vaadin=' + hasVaadin
          + ';Flow=' + hasFlow
          + ';loadOnDemand=' + !!loadOnDemand
          + ';loadOnDemandOwner=' + loadOnDemandOwner;
      }

      function classifyProductionOwnerImportFailure(error, preflight) {
        const message = error && error.message ? error.message : String(error || '');
        const lower = message.toLowerCase();
        if (lower.includes('already been defined')
            || lower.includes('cannot define multiple custom elements')
            || lower.includes('customelementregistry')
            || (lower.includes('custom element') && lower.includes('defined'))) {
          return 'production-owner-duplicate-custom-element';
        }
        if ((lower.includes('vaadin') || lower.includes('flow')) && lower.includes('already')) {
          return 'production-owner-global-collision';
        }
        if (lower.includes('failed to resolve module specifier')
            || lower.includes('relative references must start')
            || lower.includes('module specifier')
            || lower.includes('failed to fetch dynamically imported module')) {
          return 'production-owner-relative-import-unresolved';
        }
        if (preflight && preflight.missingDependencyPaths && preflight.missingDependencyPaths.length > 0) {
          return 'production-owner-missing-real-artifact';
        }
        if (preflight && preflight.pendingPaths && preflight.pendingPaths.length > 0) {
          return 'production-owner-relative-import-unresolved';
        }
        return 'production-owner-browser-import-failed';
      }

      function duplicateTagFromProductionOwnerError(error) {
        const message = error && error.message ? error.message : String(error || '');
        const match = message.match(/vaadin-[a-z0-9-]+/i);
        return match ? match[0].toLowerCase() : '';
      }

      function productionOwnerImportSideEffectProof(registryBefore, registryAfter, globalBefore, globalAfter) {
        const proof = [];
        if (registryBefore !== registryAfter) {
          proof.push('custom-element-registry-changed');
        }
        if (globalBefore !== globalAfter) {
          proof.push('vaadin-global-changed');
        }
        if (globalBefore.includes('loadOnDemand=false')
            && window.Vaadin
            && window.Vaadin.Flow
            && typeof window.Vaadin.Flow.loadOnDemand === 'function') {
          proof.push('loadondemand-present-after-owner-import');
        }
        return Array.from(new Set(proof)).join(',');
      }

      function productionOwnerImportPhase() {
        const componentImportStarted = componentFrontendResourceExecutedPaths.size > 0
          || componentFrontendResourceExecutedTags.size > 0
          || window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecuted === true
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecuted === true
          || window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined === true;
        return componentImportStarted ? 'after-component-imports' : 'before-component-imports';
      }

      function productionOwnerOrderingResult(phase, preflight, failureLayer) {
        const base = phase === 'before-component-imports'
          ? 'owner-first-import-attempted-before-component-focused-imports'
          : 'after-component-import-fallback-observed';
        const graph = preflight && preflight.dependencyGraphClosed === true
          ? 'dependency-graph-closed-for-requested-artifacts'
          : 'dependency-graph-pending';
        const failure = failureLayer ? ('failureLayer=' + failureLayer) : 'failureLayer=none';
        return base + ';' + graph + ';' + failure;
      }

      function mergedProductionOwnerOrderingResult(next) {
        const existing =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult || '';
        if (!existing || existing === next) {
          return next;
        }
        if (existing.includes(next)) {
          return existing;
        }
        return existing + '|' + next;
      }

      function componentProductionOwnerReuseDiagnostics(resource, componentTag) {
        const ownerImported =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImported === true;
        const ownerAttempted =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAttempted === true;
        const ownerFailureLayer =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer || '';
        const ownerPhase =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhase || '';
        const ownerHasTag = ownerImported && componentTag && !!customElements.get(componentTag);
        let mode = ownerHasTag ? 'owner-owned-custom-elements' : 'focused-component-import-fallback';
        let reason = '';
        if (ownerHasTag) {
          reason = 'owner import registered ' + componentTag + ' before focused component import';
        } else if (ownerAttempted) {
          reason = 'owner import failed before component import; failureLayer='
            + ownerFailureLayer + ';phase=' + ownerPhase;
        } else {
          reason = 'owner import not attempted before this focused component import';
        }
        const details = {
          componentProductionOwnerReuseVersion:
            resource.componentProductionOwnerReuseVersion || 'flow-ipc-component-owner-reuse-v1_16',
          componentProductionOwnerReusePolicy:
            resource.componentProductionOwnerReusePolicy || 'reuse-owner-owned-custom-elements-before-focused-imports-v1_16',
          componentProductionOwnerReuseMode: mode,
          componentProductionOwnerReuseAcceptableFailures:
            resource.componentProductionOwnerReuseAcceptableFailures
            || 'owner-import-failed-before-component-import,owner-import-succeeded-but-component-class-export-unreachable,owner-side-effects-global-but-focused-entry-still-required,vaadin-production-artifact-does-not-expose-reusable-owner-contract,blob-module-identity-cannot-safely-merge-owner-and-component-lifetime',
          componentProductionOwnerReused: ownerHasTag,
          componentProductionOwnerReuseReason: reason,
          componentProductionOwnerOwnedTags: ownerHasTag ? componentTag : '',
          componentProductionOwnerSkippedDefinitions: ownerHasTag ? componentTag : '',
          componentProductionOwnerFallbackImports: ownerHasTag ? '' : (resource.path || ''),
          componentProductionOwnerDependencyPaths:
            resource.componentProductionOwnerDependencyPaths || 'vaadin-dev-bundle/webapp/VAADIN/build/indexhtml-BfYYZi8S.js',
          componentProductionOwnerImportOrder: ownerPhase
            ? ('production-owner-' + ownerPhase + '-then-focused-component-import-v1_16')
            : (resource.componentProductionOwnerImportOrder || 'focused-component-import-before-owner-decision-v1_16')
        };
        applyComponentProductionOwnerReuseDiagnostics(details);
        return details;
      }

      function applyComponentProductionOwnerReuseDiagnostics(details) {
        if (!details) {
          return;
        }
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseVersion =
          details.componentProductionOwnerReuseVersion || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReusePolicy =
          details.componentProductionOwnerReusePolicy || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseMode =
          details.componentProductionOwnerReuseMode || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseAcceptableFailures =
          details.componentProductionOwnerReuseAcceptableFailures || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused =
          details.componentProductionOwnerReused === true;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason =
          details.componentProductionOwnerReuseReason || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerOwnedTags =
          details.componentProductionOwnerOwnedTags || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerSkippedDefinitions =
          details.componentProductionOwnerSkippedDefinitions || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerFallbackImports =
          details.componentProductionOwnerFallbackImports || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths =
          details.componentProductionOwnerDependencyPaths || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerImportOrder =
          details.componentProductionOwnerImportOrder || '';
      }

      function recordProductionOwnerOrderingPreflight(resource, source, preflight, phase) {
        if (productionBundleRuntimeOwnerOrderingPreflightRecorded || phase !== 'before-component-imports') {
          return;
        }
        productionBundleRuntimeOwnerOrderingPreflightRecorded = true;
        const registryBefore = vaadinCustomElementRegistrySnapshot();
        const globalBefore = vaadinGlobalSnapshot();
        const failureLayer = 'production-owner-component-reuse-contract-missing';
        const importVersion = resource.productionBundleRuntimeOwnerImportVersion
          || 'flow-ipc-production-owner-import-v1_14';
        const ownerIdentity = resource.productionBundleRuntimeOwnerModuleIdentity
          || ('production-owner-v1_13:' + resource.path);
        const importModuleIdentity = 'production-owner-import-v1_14:' + resource.path;
        const observedPhases = mergeCsv(
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases,
          [phase]);
        const preflightExtra = {
          owner: 'production-bundle-runtime',
          cleanupPolicy: 'retain-for-window-lifetime-owner-ordering-preflight-v1_16',
          runtimeResolverContractVersion: resource.runtimeResolverContractVersion
            || 'flow-ipc-frontend-runtime-resolver-v1_10',
          logicalRequestId: resource.logicalRequestId || 'production-runtime:indexhtml:v1_10',
          pathMapping: resource.pathMapping || 'stats-bundle-import-to-dev-bundle-artifact',
          stageUrlPolicy: 'blob-module-url-owner-ordering-preflight-v1_16',
          cachePolicy: 'retain-per-owner-import-module-identity',
          cleanupOwnership: 'window-lifetime-owner-ordering-preflight-cache',
          blockerClassification: 'flow-ipc-production-owner-ordering-v1_16',
          frontendResourceSchemeVersion: 'flow-ipc-resource-scheme-v1_17',
          frontendResourceLoaderMode: 'blob-fallback',
          frontendResourceSchemeName: 'vatori-resource',
          frontendResourceSchemeHandlerInstalled: true,
          frontendResourceLoaderNoLocalhost: true,
          frontendResourceFileBacked: false,
          stagingMode: 'blob-fallback',
          blobFallbackReason: 'production-owner-root-keeps-blob-import-until-custom-scheme-owner-graph-can-own-indexhtml-side-effects',
          blobFallbackBlockedLayer: 'production-owner-import-runtime',
          blobFallbackNextResolutionStep: 'migrate the production owner root and dependency graph to the v1.17 host-owned scheme after the lazy dependency proves custom-scheme execution',
          productionBundleRuntimeVersion: resource.productionBundleRuntimeVersion
            || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeRegistrySource: resource.productionBundleRuntimeRegistrySource
            || 'vaadin-dev-bundle-stats-runtime-registry-v1_10',
          productionBundleRuntimeStatsSource: resource.productionBundleRuntimeStatsSource || '',
          productionBundleRuntimeSelectedArtifact: resource.productionBundleRuntimeSelectedArtifact || resource.path,
          productionBundleRuntimeDependencyChain: resource.productionBundleRuntimeDependencyChain || '',
          productionBundleRuntimeModuleIdentity: resource.productionBundleRuntimeModuleIdentity || resource.path,
          productionBundleRuntimeExecutionPolicy: resource.productionBundleRuntimeExecutionPolicy
            || 'stage-execution-attempt-production-bundle-runtime-v1_10',
          productionBundleRuntimeBlockerVersion: resource.productionBundleRuntimeBlockerVersion
            || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeFailureLayer: resource.productionBundleRuntimeFailureLayer
            || 'production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports',
          productionBundleRuntimeNextResolutionStep: resource.productionBundleRuntimeNextResolutionStep || '',
          productionBundleRuntimeOwnerVersion: resource.productionBundleRuntimeOwnerVersion
            || 'flow-ipc-production-owner-v1_13',
          productionBundleRuntimeOwner: resource.productionBundleRuntimeOwner
            || 'flow-ipc-production-bundle-runtime',
          productionBundleRuntimeEntryArtifact: resource.productionBundleRuntimeEntryArtifact || resource.path,
          productionBundleRuntimeSource: resource.productionBundleRuntimeSource || 'vaadin-dev-bundle-stats',
          productionBundleRuntimeSelectedBy: resource.productionBundleRuntimeSelectedBy
            || 'stats-entrypoint-or-focused-production-entry',
          productionBundleRuntimeOwnerModuleIdentity: ownerIdentity,
          productionBundleRuntimeOwnerLifecycle:
            'created,staged,owner-first-preflight-blocked,component-reuse-fallback,cleanup-owned',
          productionBundleRuntimeOwnerExecutionPolicy:
            resource.productionBundleRuntimeOwnerExecutionPolicy
            || 'stage-owner-contract-production-bundle-runtime-v1_13',
          productionBundleRuntimeOwnerStageUrlPolicy:
            resource.productionBundleRuntimeOwnerStageUrlPolicy || 'temporary-blob-module-url-owner-namespace-v1_13',
          productionBundleRuntimeOwnerCachePolicy: 'retain-per-owner-import-module-identity',
          productionBundleRuntimeOwnerCacheStatus:
            frontendProductionOwnerImportUrlCache.has(resource.path) ? 'hit' : 'miss',
          productionBundleRuntimeOwnerCleanupStrategy: 'cleanup-owned-window-lifetime-owner-import',
          productionBundleRuntimeOwnerExecutionResult:
            'strict-blocker:' + failureLayer + ':' + resource.path,
          productionBundleRuntimeOwnerSideEffectProof:
            'owner-first-preflight-reached-before-component-imports-but-owner-module-not-reusable',
          productionBundleRuntimeOwnerFailureLayer: failureLayer,
          productionBundleRuntimeOwnerBrowserError: '',
          productionBundleRuntimeOwnerBlockerVersion: 'flow-ipc-production-owner-ordering-v1_16',
          productionBundleRuntimeOwnerNextResolutionStep:
            'run the owner through a host-owned resource scheme or Vaadin-supported production owner contract before focused component imports can reuse it',
          productionBundleRuntimeOwnerRegistryBefore: registryBefore,
          productionBundleRuntimeOwnerRegistryAfter: registryBefore,
          productionBundleRuntimeOwnerDuplicateTag: '',
          productionBundleRuntimeOwnerFirstDefinitionOwner: '',
          productionBundleRuntimeOwnerSecondDefinitionOwner: '',
          productionBundleRuntimeOwnerImportVersion: importVersion,
          productionBundleRuntimeOwnerImportPolicy:
            resource.productionBundleRuntimeOwnerImportPolicy
            || 'controlled-owner-dynamic-import-after-component-graph-v1_14',
          productionBundleRuntimeOwnerImportArtifact:
            resource.productionBundleRuntimeOwnerImportArtifact || resource.path,
          productionBundleRuntimeOwnerImportPhase: phase,
          productionBundleRuntimeOwnerImportPhaseCandidates:
            resource.productionBundleRuntimeOwnerImportPhaseCandidates
            || 'isolated-preflight,before-component-imports,after-component-imports',
          productionBundleRuntimeOwnerImportDiagnosticModes:
            resource.productionBundleRuntimeOwnerImportDiagnosticModes
            || 'isolated-preflight:available-not-enabled,before-component-imports:available-not-enabled,after-component-imports:default',
          productionBundleRuntimeOwnerImportExpectedSideEffects:
            resource.productionBundleRuntimeOwnerImportExpectedSideEffects || '',
          productionBundleRuntimeOwnerImportAcceptableFailures:
            resource.productionBundleRuntimeOwnerImportAcceptableFailures || '',
          productionBundleRuntimeOwnerImportRewriteStrategy:
            resource.productionBundleRuntimeOwnerImportRewriteStrategy
            || 'rewrite-relative-dev-bundle-dependencies-before-owner-import-v1_14',
          productionBundleRuntimeOwnerImportDependencyPaths:
            resource.productionBundleRuntimeOwnerImportDependencyPaths || resource.path,
          productionBundleRuntimeOwnerImportResolvedDependencyPaths: preflight.resolvedPaths.join(','),
          productionBundleRuntimeOwnerImportUnresolvedSpecifiers: preflight.unresolvedSpecifiers.join(','),
          productionBundleRuntimeOwnerImportDependencyRewriteResult: preflight.dependencyRewriteResult,
          productionBundleRuntimeOwnerDependencyResolverVersion:
            resource.productionBundleRuntimeOwnerDependencyResolverVersion
            || 'flow-ipc-production-owner-deps-v1_15',
          productionBundleRuntimeOwnerDependencyResolverRoot:
            resource.productionBundleRuntimeOwnerDependencyResolverRoot
            || 'vaadin-dev-bundle/webapp/VAADIN/build/',
          productionBundleRuntimeOwnerDependencyResolverPolicy:
            resource.productionBundleRuntimeOwnerDependencyResolverPolicy
            || 'owner-scoped-recursive-relative-import-graph-v1_15',
          productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts:
            resource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts || '',
          productionBundleRuntimeOwnerDependencyResolverAcceptableFailures:
            resource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures || '',
          productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace:
            resource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
            || 'production-owner-deps-v1_15',
          productionBundleRuntimeOwnerDependencyResolverRootArtifact:
            resource.productionBundleRuntimeOwnerDependencyResolverRootArtifact || resource.path,
          productionBundleRuntimeOwnerDependencyGraphClosed: preflight.dependencyGraphClosed === true,
          productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount: preflight.totalSpecifierCount,
          productionBundleRuntimeOwnerDependencyGraphResolvedCount: preflight.resolvedCount,
          productionBundleRuntimeOwnerDependencyGraphMissingCount: preflight.missingCount,
          productionBundleRuntimeOwnerDependencyGraphStagedCount: preflight.stagedCount,
          productionBundleRuntimeOwnerDependencyGraphMaxDepth: preflight.maxDepth,
          productionBundleRuntimeOwnerDependencyClassifications:
            preflight.dependencyClassifications.join('|'),
          productionBundleRuntimeOwnerImportMissingDependencyPaths:
            preflight.missingDependencyPaths.join(','),
          productionBundleRuntimeOwnerImportMissingSpecifiers:
            preflight.missingSpecifiers.join(','),
          productionBundleRuntimeOwnerImportStageCount: preflight.resolvedPaths.length,
          productionBundleRuntimeOwnerImportModuleIdentity: importModuleIdentity,
          productionBundleRuntimeOwnerImportVaadinGlobalBefore: globalBefore,
          productionBundleRuntimeOwnerImportVaadinGlobalAfter: globalBefore,
          productionBundleRuntimeOwnerImportAttempted: true,
          productionBundleRuntimeOwnerImported: false,
          productionBundleRuntimeOwnerImportNextResolutionStep:
            'component resolver fallback remains required until the owner import can expose reusable side effects',
          productionBundleRuntimeOwnerImportBrowserErrorName: '',
          productionBundleRuntimeOwnerImportOrderingVersion:
            resource.productionBundleRuntimeOwnerImportOrderingVersion
            || 'flow-ipc-production-owner-ordering-v1_16',
          productionBundleRuntimeOwnerImportPhasePolicy:
            resource.productionBundleRuntimeOwnerImportPhasePolicy
            || 'owner-first-before-component-imports-with-after-component-fallback-v1_16',
          productionBundleRuntimeOwnerImportOrderingResult:
            productionOwnerOrderingResult(phase, preflight, failureLayer),
          productionBundleRuntimeOwnerImportOrderingObservedPhases: observedPhases,
          productionBundleRuntimeOwnerImportOrderingAcceptableFailures:
            resource.productionBundleRuntimeOwnerImportOrderingAcceptableFailures || '',
          componentProductionOwnerReuseVersion:
            resource.componentProductionOwnerReuseVersion || 'flow-ipc-component-owner-reuse-v1_16',
          componentProductionOwnerReusePolicy:
            resource.componentProductionOwnerReusePolicy || 'reuse-owner-owned-custom-elements-before-focused-imports-v1_16',
          componentProductionOwnerReuseMode: 'focused-component-import-fallback',
          componentProductionOwnerReuseAcceptableFailures:
            resource.componentProductionOwnerReuseAcceptableFailures || '',
          componentProductionOwnerReused: false,
          componentProductionOwnerReuseReason:
            'owner import failed before component import; failureLayer=' + failureLayer,
          componentProductionOwnerOwnedTags: '',
          componentProductionOwnerSkippedDefinitions: '',
          componentProductionOwnerFallbackImports:
            resource.componentProductionOwnerFallbackImports || '',
          componentProductionOwnerDependencyPaths:
            resource.componentProductionOwnerDependencyPaths || resource.path,
          componentProductionOwnerImportOrder:
            'production-owner-before-focused-component-imports-v1_16',
          moduleIdentityStrategy: 'production-owner-ordering-preflight-module-identity-v1_16',
          moduleIdentityKey: 'production-owner-ordering-v1_16:' + phase + ':' + resource.path,
          importResolutionStrategy:
            resource.productionBundleRuntimeOwnerImportRewriteStrategy
            || 'rewrite-relative-dev-bundle-dependencies-before-owner-import-v1_14'
        };
        applyProductionOwnerImportDiagnostics(preflightExtra);
        stageFrontendArtifact(resource, source, 'strict-blocker', null, null, preflightExtra);
      }

      function applyProductionOwnerImportDiagnostics(extra) {
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeObserved = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeStageAttempted = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerObserved = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerStageObserved = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAttempted =
          extra.productionBundleRuntimeOwnerImportAttempted === true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImported =
          extra.productionBundleRuntimeOwnerImported === true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlocker =
          !!extra.productionBundleRuntimeOwnerFailureLayer;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerVersion =
          extra.productionBundleRuntimeOwnerVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwner =
          extra.productionBundleRuntimeOwner;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerEntryArtifact =
          extra.productionBundleRuntimeEntryArtifact;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerModuleIdentity =
          extra.productionBundleRuntimeOwnerModuleIdentity;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionPolicy =
          extra.productionBundleRuntimeOwnerExecutionPolicy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerExecutionResult =
          extra.productionBundleRuntimeOwnerExecutionResult;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerSideEffectProof =
          extra.productionBundleRuntimeOwnerSideEffectProof;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerFailureLayer =
          extra.productionBundleRuntimeOwnerFailureLayer;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBrowserError =
          extra.productionBundleRuntimeOwnerBrowserError || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerBlockerVersion =
          extra.productionBundleRuntimeOwnerBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerNextResolutionStep =
          extra.productionBundleRuntimeOwnerNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerRegistryBefore =
          extra.productionBundleRuntimeOwnerRegistryBefore;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerRegistryAfter =
          extra.productionBundleRuntimeOwnerRegistryAfter;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDuplicateTag =
          extra.productionBundleRuntimeOwnerDuplicateTag || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVersion =
          extra.productionBundleRuntimeOwnerImportVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPolicy =
          extra.productionBundleRuntimeOwnerImportPolicy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportArtifact =
          extra.productionBundleRuntimeOwnerImportArtifact;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhase =
          extra.productionBundleRuntimeOwnerImportPhase;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhaseCandidates =
          extra.productionBundleRuntimeOwnerImportPhaseCandidates || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDiagnosticModes =
          extra.productionBundleRuntimeOwnerImportDiagnosticModes || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportExpectedSideEffects =
          extra.productionBundleRuntimeOwnerImportExpectedSideEffects;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportAcceptableFailures =
          extra.productionBundleRuntimeOwnerImportAcceptableFailures;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportRewriteStrategy =
          extra.productionBundleRuntimeOwnerImportRewriteStrategy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyPaths =
          extra.productionBundleRuntimeOwnerImportDependencyPaths;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportResolvedDependencyPaths =
          extra.productionBundleRuntimeOwnerImportResolvedDependencyPaths;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportUnresolvedSpecifiers =
          extra.productionBundleRuntimeOwnerImportUnresolvedSpecifiers;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportDependencyRewriteResult =
          extra.productionBundleRuntimeOwnerImportDependencyRewriteResult;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverVersion =
          extra.productionBundleRuntimeOwnerDependencyResolverVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRoot =
          extra.productionBundleRuntimeOwnerDependencyResolverRoot;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverPolicy =
          extra.productionBundleRuntimeOwnerDependencyResolverPolicy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverExpectedArtifacts =
          extra.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverAcceptableFailures =
          extra.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace =
          extra.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyResolverRootArtifact =
          extra.productionBundleRuntimeOwnerDependencyResolverRootArtifact;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphClosed =
          extra.productionBundleRuntimeOwnerDependencyGraphClosed === true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount =
          Number(extra.productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount || 0);
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphResolvedCount =
          Number(extra.productionBundleRuntimeOwnerDependencyGraphResolvedCount || 0);
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMissingCount =
          Number(extra.productionBundleRuntimeOwnerDependencyGraphMissingCount || 0);
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphStagedCount =
          Number(extra.productionBundleRuntimeOwnerDependencyGraphStagedCount || 0);
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyGraphMaxDepth =
          Number(extra.productionBundleRuntimeOwnerDependencyGraphMaxDepth || 0);
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerDependencyClassifications =
          extra.productionBundleRuntimeOwnerDependencyClassifications || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportMissingDependencyPaths =
          extra.productionBundleRuntimeOwnerImportMissingDependencyPaths || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportMissingSpecifiers =
          extra.productionBundleRuntimeOwnerImportMissingSpecifiers || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportStageCount =
          Number(extra.productionBundleRuntimeOwnerImportStageCount || 0);
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportModuleIdentity =
          extra.productionBundleRuntimeOwnerImportModuleIdentity;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalBefore =
          extra.productionBundleRuntimeOwnerImportVaadinGlobalBefore;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportVaadinGlobalAfter =
          extra.productionBundleRuntimeOwnerImportVaadinGlobalAfter;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportNextResolutionStep =
          extra.productionBundleRuntimeOwnerImportNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportBrowserErrorName =
          extra.productionBundleRuntimeOwnerImportBrowserErrorName || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingVersion =
          extra.productionBundleRuntimeOwnerImportOrderingVersion || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportPhasePolicy =
          extra.productionBundleRuntimeOwnerImportPhasePolicy || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingResult =
          extra.productionBundleRuntimeOwnerImportOrderingResult || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases =
          extra.productionBundleRuntimeOwnerImportOrderingObservedPhases || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingAcceptableFailures =
          extra.productionBundleRuntimeOwnerImportOrderingAcceptableFailures || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseVersion =
          extra.componentProductionOwnerReuseVersion || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReusePolicy =
          extra.componentProductionOwnerReusePolicy || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseMode =
          extra.componentProductionOwnerReuseMode || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseAcceptableFailures =
          extra.componentProductionOwnerReuseAcceptableFailures || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReused =
          extra.componentProductionOwnerReused === true;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerReuseReason =
          extra.componentProductionOwnerReuseReason;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerOwnedTags =
          extra.componentProductionOwnerOwnedTags || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerSkippedDefinitions =
          extra.componentProductionOwnerSkippedDefinitions || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerFallbackImports =
          extra.componentProductionOwnerFallbackImports || '';
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerDependencyPaths =
          extra.componentProductionOwnerDependencyPaths;
        window.VaadinDesktopIpcDiagnostics.componentProductionOwnerImportOrder =
          extra.componentProductionOwnerImportOrder;
      }

      async function maybeImportProductionBundleRuntimeOwner() {
        if (productionBundleRuntimeOwnerImportAttempted) {
          return;
        }
        const resource = productionBundleRuntimeResource();
        if (!resource) {
          return;
        }
        const source = componentFrontendResourceBodies.get(resource.path) || '';
        if (!source) {
          return;
        }
        installOpaqueOriginStorageFallbacks();
        installVaadinDevBundleGlobals();
        const preflight = productionOwnerImportPreflight(resource, source);
        if (preflight.pendingPaths && preflight.pendingPaths.length > 0) {
          recordProductionOwnerOrderingPreflight(resource, source, preflight, productionOwnerImportPhase());
          requestProductionOwnerPendingDependencies(preflight);
          return;
        }
        stageProductionOwnerResolvedDependencies(preflight, resource);
        productionBundleRuntimeOwnerImportAttempted = true;
        const registryBefore = vaadinCustomElementRegistrySnapshot();
        const globalBefore = vaadinGlobalSnapshot();
        const importPhase = productionOwnerImportPhase();
        const importVersion = resource.productionBundleRuntimeOwnerImportVersion
          || 'flow-ipc-production-owner-import-v1_14';
        const importModuleIdentity = 'production-owner-import-v1_14:' + resource.path;
        const ownerIdentity = resource.productionBundleRuntimeOwnerModuleIdentity
          || ('production-owner-v1_13:' + resource.path);
        let importSource = rewriteVaadinDevBundleModule(source, resource.path, null,
          frontendProductionOwnerImportUrlCache, true);
        if (!importSource) {
          importSource = source;
        }
        const baseExtra = {
          owner: 'production-bundle-runtime',
          cleanupPolicy: 'retain-for-window-lifetime-owner-import-v1_14',
          runtimeResolverContractVersion: resource.runtimeResolverContractVersion
            || 'flow-ipc-frontend-runtime-resolver-v1_10',
          logicalRequestId: resource.logicalRequestId || 'production-runtime:indexhtml:v1_10',
          pathMapping: resource.pathMapping || 'stats-bundle-import-to-dev-bundle-artifact',
          stageUrlPolicy: 'blob-module-url-owner-import-v1_14',
          cachePolicy: 'retain-per-owner-import-module-identity',
          cleanupOwnership: 'window-lifetime-owner-import-cache',
          blockerClassification: importVersion,
          productionBundleRuntimeVersion: resource.productionBundleRuntimeVersion
            || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeRegistrySource: resource.productionBundleRuntimeRegistrySource
            || 'vaadin-dev-bundle-stats-runtime-registry-v1_10',
          productionBundleRuntimeStatsSource: resource.productionBundleRuntimeStatsSource || '',
          productionBundleRuntimeSelectedArtifact: resource.productionBundleRuntimeSelectedArtifact || resource.path,
          productionBundleRuntimeDependencyChain: resource.productionBundleRuntimeDependencyChain || '',
          productionBundleRuntimeModuleIdentity: resource.productionBundleRuntimeModuleIdentity || resource.path,
          productionBundleRuntimeExecutionPolicy: resource.productionBundleRuntimeExecutionPolicy
            || 'stage-execution-attempt-production-bundle-runtime-v1_10',
          productionBundleRuntimeBlockerVersion: resource.productionBundleRuntimeBlockerVersion
            || 'flow-ipc-production-runtime-v1_10',
          productionBundleRuntimeFailureLayer: resource.productionBundleRuntimeFailureLayer
            || 'production-runtime-ownership-not-yet-separated-from-focused-component-entry-imports',
          productionBundleRuntimeNextResolutionStep: resource.productionBundleRuntimeNextResolutionStep || '',
          productionBundleRuntimeOwnerVersion: resource.productionBundleRuntimeOwnerVersion
            || 'flow-ipc-production-owner-v1_13',
          productionBundleRuntimeOwner: resource.productionBundleRuntimeOwner
            || 'flow-ipc-production-bundle-runtime',
          productionBundleRuntimeEntryArtifact: resource.productionBundleRuntimeEntryArtifact || resource.path,
          productionBundleRuntimeSource: resource.productionBundleRuntimeSource || 'vaadin-dev-bundle-stats',
          productionBundleRuntimeSelectedBy: resource.productionBundleRuntimeSelectedBy
            || 'stats-entrypoint-or-focused-production-entry',
          productionBundleRuntimeOwnerModuleIdentity: ownerIdentity,
          productionBundleRuntimeOwnerLifecycle:
            'created,staged,import-attempted,side-effect-probed,not-reused,cleanup-owned',
          productionBundleRuntimeOwnerExecutionPolicy:
            resource.productionBundleRuntimeOwnerExecutionPolicy || 'stage-owner-contract-production-bundle-runtime-v1_13',
          productionBundleRuntimeOwnerStageUrlPolicy:
            resource.productionBundleRuntimeOwnerStageUrlPolicy || 'temporary-blob-module-url-owner-namespace-v1_13',
          productionBundleRuntimeOwnerCachePolicy: 'retain-per-owner-import-module-identity',
          productionBundleRuntimeOwnerCacheStatus:
            frontendProductionOwnerImportUrlCache.has(resource.path) ? 'hit' : 'miss',
          productionBundleRuntimeOwnerCleanupStrategy: 'cleanup-owned-window-lifetime-owner-import',
          productionBundleRuntimeOwnerExecutionResult: 'import-attempt:' + resource.path,
          productionBundleRuntimeOwnerSideEffectProof: '',
          productionBundleRuntimeOwnerFailureLayer: '',
          productionBundleRuntimeOwnerBrowserError: '',
          productionBundleRuntimeOwnerBlockerVersion: importVersion,
          productionBundleRuntimeOwnerNextResolutionStep:
            resource.productionBundleRuntimeOwnerImportNextResolutionStep
            || resource.productionBundleRuntimeOwnerNextResolutionStep
            || '',
          productionBundleRuntimeOwnerRegistryBefore: registryBefore,
          productionBundleRuntimeOwnerRegistryAfter: registryBefore,
          productionBundleRuntimeOwnerDuplicateTag: '',
          productionBundleRuntimeOwnerFirstDefinitionOwner: '',
          productionBundleRuntimeOwnerSecondDefinitionOwner: '',
          productionBundleRuntimeOwnerImportVersion: importVersion,
          productionBundleRuntimeOwnerImportPolicy:
            resource.productionBundleRuntimeOwnerImportPolicy
            || 'controlled-owner-dynamic-import-after-component-graph-v1_14',
          productionBundleRuntimeOwnerImportArtifact:
            resource.productionBundleRuntimeOwnerImportArtifact || resource.path,
          productionBundleRuntimeOwnerImportPhase:
            importPhase,
          productionBundleRuntimeOwnerImportPhaseCandidates:
            resource.productionBundleRuntimeOwnerImportPhaseCandidates
            || 'isolated-preflight,before-component-imports,after-component-imports',
          productionBundleRuntimeOwnerImportDiagnosticModes:
            resource.productionBundleRuntimeOwnerImportDiagnosticModes
            || 'isolated-preflight:available-not-enabled,before-component-imports:available-not-enabled,after-component-imports:default',
          productionBundleRuntimeOwnerImportExpectedSideEffects:
            resource.productionBundleRuntimeOwnerImportExpectedSideEffects
            || 'window.Vaadin,window.Vaadin.Flow,window.Vaadin.Flow.loadOnDemand,customElements:vaadin-text-field|vaadin-checkbox|vaadin-select',
          productionBundleRuntimeOwnerImportAcceptableFailures:
            resource.productionBundleRuntimeOwnerImportAcceptableFailures
            || 'production-owner-relative-import-unresolved,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable',
          productionBundleRuntimeOwnerImportRewriteStrategy:
            resource.productionBundleRuntimeOwnerImportRewriteStrategy
            || 'rewrite-relative-dev-bundle-dependencies-before-owner-import-v1_14',
          productionBundleRuntimeOwnerImportDependencyPaths:
            resource.productionBundleRuntimeOwnerImportDependencyPaths || resource.path,
          productionBundleRuntimeOwnerImportResolvedDependencyPaths: preflight.resolvedPaths.join(','),
          productionBundleRuntimeOwnerImportUnresolvedSpecifiers: preflight.unresolvedSpecifiers.join(','),
          productionBundleRuntimeOwnerImportDependencyRewriteResult: preflight.dependencyRewriteResult,
          productionBundleRuntimeOwnerDependencyResolverVersion:
            resource.productionBundleRuntimeOwnerDependencyResolverVersion
            || 'flow-ipc-production-owner-deps-v1_15',
          productionBundleRuntimeOwnerDependencyResolverRoot:
            resource.productionBundleRuntimeOwnerDependencyResolverRoot
            || 'vaadin-dev-bundle/webapp/VAADIN/build/',
          productionBundleRuntimeOwnerDependencyResolverPolicy:
            resource.productionBundleRuntimeOwnerDependencyResolverPolicy
            || 'owner-scoped-recursive-relative-import-graph-v1_15',
          productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts:
            resource.productionBundleRuntimeOwnerDependencyResolverExpectedArtifacts || '',
          productionBundleRuntimeOwnerDependencyResolverAcceptableFailures:
            resource.productionBundleRuntimeOwnerDependencyResolverAcceptableFailures
            || 'production-owner-missing-real-artifact,production-owner-browser-import-failed,production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable',
          productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace:
            resource.productionBundleRuntimeOwnerDependencyResolverModuleIdentityNamespace
            || 'production-owner-deps-v1_15',
          productionBundleRuntimeOwnerDependencyResolverRootArtifact:
            resource.productionBundleRuntimeOwnerDependencyResolverRootArtifact || resource.path,
          productionBundleRuntimeOwnerDependencyGraphClosed: preflight.dependencyGraphClosed === true,
          productionBundleRuntimeOwnerDependencyGraphTotalSpecifierCount: preflight.totalSpecifierCount,
          productionBundleRuntimeOwnerDependencyGraphResolvedCount: preflight.resolvedCount,
          productionBundleRuntimeOwnerDependencyGraphMissingCount: preflight.missingCount,
          productionBundleRuntimeOwnerDependencyGraphStagedCount: preflight.stagedCount,
          productionBundleRuntimeOwnerDependencyGraphMaxDepth: preflight.maxDepth,
          productionBundleRuntimeOwnerDependencyClassifications:
            preflight.dependencyClassifications.join('|'),
          productionBundleRuntimeOwnerImportMissingDependencyPaths:
            preflight.missingDependencyPaths.join(','),
          productionBundleRuntimeOwnerImportMissingSpecifiers:
            preflight.missingSpecifiers.join(','),
          productionBundleRuntimeOwnerImportStageCount: preflight.resolvedPaths.length,
          productionBundleRuntimeOwnerImportModuleIdentity: importModuleIdentity,
          productionBundleRuntimeOwnerImportVaadinGlobalBefore: globalBefore,
          productionBundleRuntimeOwnerImportVaadinGlobalAfter: globalBefore,
          productionBundleRuntimeOwnerImportAttempted: true,
          productionBundleRuntimeOwnerImported: false,
          productionBundleRuntimeOwnerImportNextResolutionStep:
            resource.productionBundleRuntimeOwnerImportNextResolutionStep
            || 'if owner import fails, move the production owner to a custom-scheme/file-backed resolver or tighten dependency rewrite/global collision guards around the real indexhtml artifact',
          productionBundleRuntimeOwnerImportBrowserErrorName: '',
          productionBundleRuntimeOwnerImportOrderingVersion:
            resource.productionBundleRuntimeOwnerImportOrderingVersion
            || 'flow-ipc-production-owner-ordering-v1_16',
          productionBundleRuntimeOwnerImportPhasePolicy:
            resource.productionBundleRuntimeOwnerImportPhasePolicy
            || 'owner-first-before-component-imports-with-after-component-fallback-v1_16',
          productionBundleRuntimeOwnerImportOrderingResult:
            mergedProductionOwnerOrderingResult(productionOwnerOrderingResult(importPhase, preflight, '')),
          productionBundleRuntimeOwnerImportOrderingObservedPhases: mergeCsv(
            window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeOwnerImportOrderingObservedPhases,
            [importPhase]),
          productionBundleRuntimeOwnerImportOrderingAcceptableFailures:
            resource.productionBundleRuntimeOwnerImportOrderingAcceptableFailures
            || 'production-owner-duplicate-custom-element,production-owner-global-collision,production-owner-side-effect-unobservable,production-owner-browser-import-failed,production-owner-component-reuse-contract-missing,production-owner-unsupported-vaadin-production-owner-contract',
          componentProductionOwnerReuseVersion:
            resource.componentProductionOwnerReuseVersion || 'flow-ipc-component-owner-reuse-v1_16',
          componentProductionOwnerReusePolicy:
            resource.componentProductionOwnerReusePolicy || 'reuse-owner-owned-custom-elements-before-focused-imports-v1_16',
          componentProductionOwnerReuseMode:
            resource.componentProductionOwnerReuseMode || 'focused-component-import-fallback',
          componentProductionOwnerReuseAcceptableFailures:
            resource.componentProductionOwnerReuseAcceptableFailures
            || 'owner-import-failed-before-component-import,owner-import-succeeded-but-component-class-export-unreachable,owner-side-effects-global-but-focused-entry-still-required,vaadin-production-artifact-does-not-expose-reusable-owner-contract,blob-module-identity-cannot-safely-merge-owner-and-component-lifetime',
          componentProductionOwnerReused: false,
          componentProductionOwnerReuseReason:
            importPhase === 'before-component-imports'
              ? 'owner-first import attempted before focused component imports; component resolver will decide reuse after owner result'
              : 'owner import ran after focused component imports; component resolver reuse remains a fallback decision',
          componentProductionOwnerOwnedTags: '',
          componentProductionOwnerSkippedDefinitions: '',
          componentProductionOwnerFallbackImports:
            resource.componentProductionOwnerFallbackImports || '',
          componentProductionOwnerDependencyPaths:
            resource.componentProductionOwnerDependencyPaths || resource.path,
          componentProductionOwnerImportOrder:
            importPhase === 'before-component-imports'
              ? 'production-owner-before-focused-component-imports-v1_16'
              : 'component-focused-imports-before-owner-import-fallback-v1_16',
          moduleIdentityStrategy: 'production-owner-import-module-identity-v1_14',
          moduleIdentityKey: importModuleIdentity,
          importResolutionStrategy:
            resource.productionBundleRuntimeOwnerImportRewriteStrategy
            || 'rewrite-relative-dev-bundle-dependencies-before-owner-import-v1_14'
        };
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult =
          'import-attempt:' + resource.path;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker = true;
        applyProductionOwnerImportDiagnostics(baseExtra);
        const stagedUrl = stageFrontendArtifact(resource, importSource, 'execution-attempt',
          null, frontendProductionOwnerImportUrlCache, baseExtra);
        if (!stagedUrl) {
          const failureExtra = Object.assign({}, baseExtra, {
            productionBundleRuntimeOwnerExecutionResult: 'execution-failed:' + resource.path,
            productionBundleRuntimeOwnerFailureLayer: 'production-owner-browser-import-failed',
            productionBundleRuntimeOwnerBrowserError: 'production-owner-import-stage-url-missing',
            productionBundleRuntimeOwnerImportOrderingResult:
              mergedProductionOwnerOrderingResult(
                productionOwnerOrderingResult(importPhase, preflight, 'production-owner-browser-import-failed')),
            componentProductionOwnerReuseReason:
              'owner import failed before component import; failureLayer=production-owner-browser-import-failed',
            browserError: 'production-owner-import-stage-url-missing'
          });
          applyProductionOwnerImportDiagnostics(failureExtra);
          recordFrontendStageOutcome(resource, '', 'execution-failed', failureExtra);
          return;
        }
        try {
          await import(stagedUrl);
          const registryAfter = vaadinCustomElementRegistrySnapshot();
          const globalAfter = vaadinGlobalSnapshot();
          const proof = productionOwnerImportSideEffectProof(registryBefore, registryAfter, globalBefore, globalAfter);
          const failureLayer = proof ? '' : 'production-owner-side-effect-unobservable';
          const successExtra = Object.assign({}, baseExtra, {
            productionBundleRuntimeOwnerExecutionResult: proof
              ? ('imported:' + resource.path)
              : ('strict-blocker:production-owner-side-effect-unobservable:' + resource.path),
            productionBundleRuntimeOwnerSideEffectProof: proof || 'module-import-resolved-side-effect-unobservable',
            productionBundleRuntimeOwnerFailureLayer: failureLayer,
            productionBundleRuntimeOwnerBrowserError: '',
            productionBundleRuntimeOwnerRegistryAfter: registryAfter,
            productionBundleRuntimeOwnerImportVaadinGlobalAfter: globalAfter,
            productionBundleRuntimeOwnerImported: true,
            productionBundleRuntimeOwnerImportOrderingResult:
              mergedProductionOwnerOrderingResult(productionOwnerOrderingResult(importPhase, preflight, failureLayer)),
            componentProductionOwnerReuseMode: proof
              ? 'owner-side-effect-observed-awaiting-focused-component-reuse'
              : 'owner-imported-side-effect-unobservable-focused-component-fallback',
            componentProductionOwnerReuseReason: proof
              ? 'owner import completed; focused component resolver will reuse only owner-owned tags'
              : 'owner import resolved without observable component side effects; focused component imports remain required',
            browserError: ''
          });
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult =
            successExtra.productionBundleRuntimeOwnerExecutionResult;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker =
            !!failureLayer;
          applyProductionOwnerImportDiagnostics(successExtra);
          recordFrontendStageOutcome(resource, stagedUrl, proof ? 'executed' : 'strict-blocker', successExtra);
        } catch (error) {
          const message = error && error.message ? error.message : String(error);
          const classifiedFailureLayer = classifyProductionOwnerImportFailure(error, preflight);
          const failureLayer = importPhase === 'before-component-imports'
            && classifiedFailureLayer === 'production-owner-missing-real-artifact'
            ? 'production-owner-component-reuse-contract-missing'
            : classifiedFailureLayer;
          const registryAfter = vaadinCustomElementRegistrySnapshot();
          const globalAfter = vaadinGlobalSnapshot();
          const failureExtra = Object.assign({}, baseExtra, {
            productionBundleRuntimeOwnerExecutionResult:
              'strict-blocker:' + failureLayer + ':' + resource.path,
            productionBundleRuntimeOwnerSideEffectProof:
              'owner-import-rejected-before-observable-side-effect',
            productionBundleRuntimeOwnerFailureLayer: failureLayer,
            productionBundleRuntimeOwnerBrowserError: message,
            productionBundleRuntimeOwnerRegistryAfter: registryAfter,
            productionBundleRuntimeOwnerDuplicateTag: duplicateTagFromProductionOwnerError(error),
            productionBundleRuntimeOwnerImportVaadinGlobalAfter: globalAfter,
            productionBundleRuntimeOwnerImportBrowserErrorName:
              error && error.name ? error.name : '',
            productionBundleRuntimeOwnerImported: false,
            productionBundleRuntimeOwnerImportOrderingResult:
              mergedProductionOwnerOrderingResult(productionOwnerOrderingResult(importPhase, preflight, failureLayer)
              + ';rawFailureLayer=' + classifiedFailureLayer),
            componentProductionOwnerReuseMode: 'focused-component-import-fallback',
            componentProductionOwnerReuseReason:
              'owner import failed before component import; failureLayer='
              + failureLayer + ';rawFailureLayer=' + classifiedFailureLayer,
            componentProductionOwnerFallbackImports:
              resource.componentProductionOwnerFallbackImports || '',
            browserError: message,
            failureLayer
          });
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeExecutionResult =
            failureExtra.productionBundleRuntimeOwnerExecutionResult;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBrowserError = message;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundleRuntimeBlocker = true;
          applyProductionOwnerImportDiagnostics(failureExtra);
          recordFrontendStageOutcome(resource, stagedUrl, 'strict-blocker', failureExtra);
        }
      }

      function customElementRegistrySnapshot(tag) {
        const defined = !!customElements.get(tag);
        return tag + ':defined=' + defined;
      }

      function vaadinCustomElementRegistrySnapshot() {
        return ['vaadin-text-field', 'vaadin-checkbox', 'vaadin-select']
          .map((tag) => customElementRegistrySnapshot(tag))
          .join(';');
      }

      function markStagedDependenciesStrictBlocker(urlCache, browserError, extra) {
        const skipPath = extra && typeof extra.skipPath === 'string' ? extra.skipPath : '';
        for (const [dependencyPath, dependencyUrl] of urlCache.entries()) {
          if (skipPath && dependencyPath === skipPath) {
            continue;
          }
          if (frontendStagedResourceExecutedPaths.has(dependencyPath)
              && frontendModuleIdentityResourcePaths.has(dependencyPath)) {
            continue;
          }
          const dependency = componentFrontendResourceByPath(dependencyPath);
          recordFrontendStageOutcome(dependency, dependencyUrl, 'strict-blocker', Object.assign({
            cleanupPolicy: 'revoke-on-module-graph-complete',
            blockedReason: 'blocked-by-parent-module-execution',
            browserError
          }, extra || {}));
        }
      }

      async function maybeExecuteGeneratedLazyBundleResource() {
        if (generatedLazyBundleExecutionAttempted
            || !window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified) {
          return;
        }
        const resource = generatedLazyBundleResource();
        if (!resource) {
          return;
        }
        const source = componentFrontendResourceBodies.get(resource.path) || '';
        if (!source) {
          return;
        }
        generatedLazyBundleExecutionAttempted = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionAttempted = true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolverVersion =
          resource.generatedLazyResolverVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolverVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionVersion =
          resource.generatedLazyExecutionVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyImportResolutionStrategy =
          resource.generatedLazyImportResolutionStrategy || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyImportResolutionStrategy;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeImportSpecifier =
          resource.generatedLazyRelativeImportSpecifier || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeImportSpecifier;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedDependencyPath =
          resource.generatedLazyResolvedDependencyPath || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedDependencyPath;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlockerVersion =
          resource.generatedLazyBlockerVersion || resource.blockerVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFailureLayer =
          resource.failureLayer || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFailureLayer;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyNextResolutionStep =
          resource.nextResolutionStep || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyNextResolutionStep;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandContractVersion =
          resource.generatedLazyLoadOnDemandContractVersion
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandContractVersion
          || 'flow-ipc-loadondemand-v1_9';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyTriggerSource =
          resource.generatedLazyTriggerSource
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyTriggerSource
          || 'diagnostic-preflight-and-direct-staged-import';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiOwner =
          resource.generatedLazyApiOwner
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiOwner
          || 'window.Vaadin.Flow.loadOnDemand';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiInstallSource =
          resource.generatedLazyApiInstallSource
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiInstallSource
          || 'generated-flow-imports-dev-bundle-after-direct-staged-import';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRequestedChunk =
          resource.generatedLazyRequestedChunk
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRequestedChunk
          || 'vatori-flow-ipc-generated-lazy-v1_9-probe';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRegistrySource =
          resource.generatedLazyRegistrySource
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRegistrySource
          || 'no-bootstrap-lazy-registry-observed';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyMappingSource =
          resource.generatedLazyMappingSource
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyMappingSource
          || 'component-frontend-manifest-generated-lazy-v1_9';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedArtifactPath =
          resource.generatedLazyResolvedArtifactPath
          || window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyResolvedArtifactPath
          || resource.path;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFlowClientTriggered = false;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyDiagnosticBridge =
          resource.generatedLazyDiagnosticBridge === true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandBlocker = true;
        const loadOnDemand = window.Vaadin && window.Vaadin.Flow
          && typeof window.Vaadin.Flow.loadOnDemand === 'function';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandResult =
          loadOnDemand
            ? 'api-present-before-generated-flow-imports;registry-unmapped;diagnostic-direct-import-required'
            : 'api-not-installed-before-generated-flow-imports;diagnostic-direct-import-required';
        const urlsToRevoke = [];
        const urlCache = new Map();
        let stagedUrl = '';
        try {
          installOpaqueOriginStorageFallbacks();
          installVaadinDevBundleGlobals();
          stagedUrl = componentModuleBlobUrl(resource.path, urlsToRevoke, urlCache, 'execution-attempt', {
            cleanupPolicy: usesStableModuleIdentity(resource)
              ? 'retain-for-window-lifetime-module-identity-v1_7'
              : 'revoke-after-generated-lazy-import',
            importResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyResolverVersion: resource.generatedLazyResolverVersion
              || 'flow-ipc-generated-lazy-resolver-v1_8',
            generatedLazyExecutionVersion: resource.generatedLazyExecutionVersion
              || 'flow-ipc-generated-lazy-v1_8',
            generatedLazyImportResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyLoadOnDemandContractVersion: resource.generatedLazyLoadOnDemandContractVersion
              || 'flow-ipc-loadondemand-v1_9',
            generatedLazyTriggerSource: resource.generatedLazyTriggerSource
              || 'diagnostic-preflight-and-direct-staged-import',
            generatedLazyApiOwner: resource.generatedLazyApiOwner || 'window.Vaadin.Flow.loadOnDemand',
            generatedLazyApiInstallSource: resource.generatedLazyApiInstallSource
              || 'generated-flow-imports-dev-bundle-after-direct-staged-import',
            generatedLazyRequestedChunk: resource.generatedLazyRequestedChunk
              || 'vatori-flow-ipc-generated-lazy-v1_9-probe',
            generatedLazyRegistrySource: resource.generatedLazyRegistrySource
              || 'no-bootstrap-lazy-registry-observed',
            generatedLazyMappingSource: resource.generatedLazyMappingSource
              || 'component-frontend-manifest-generated-lazy-v1_9',
            generatedLazyResolvedArtifactPath: resource.generatedLazyResolvedArtifactPath || resource.path,
            generatedLazyFlowClientTriggered: false,
            generatedLazyDiagnosticBridge: resource.generatedLazyDiagnosticBridge === true,
            generatedLazyRelativeImportSpecifier: resource.generatedLazyRelativeImportSpecifier || '',
            generatedLazyResolvedDependencyPath: resource.generatedLazyResolvedDependencyPath || '',
            generatedLazyRelativeDependencyResolved:
              window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeDependencyResolved,
            generatedLazyGraphProbe: resource.generatedLazyGraphProbe || '',
            generatedLazyBlockerVersion: resource.generatedLazyBlockerVersion
              || resource.blockerVersion
              || 'flow-ipc-loadondemand-v1_9',
            moduleIdentityStrategy: usesStableModuleIdentity(resource)
              ? 'stable-resource-path-blob-module-identity-v1_7'
              : '',
            moduleIdentityKey: resource.path
          });
          if (!stagedUrl) {
            throw new Error(window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
              || 'generated-lazy-staging-rewrite-failed');
          }
          await import(stagedUrl);
          const generatedLazyLoadOnDemandDelegate = window.Vaadin && window.Vaadin.Flow
            && typeof window.Vaadin.Flow.loadOnDemand === 'function'
            ? window.Vaadin.Flow.loadOnDemand
            : null;
          installGeneratedLoadOnDemandChunkBridge(generatedLazyLoadOnDemandDelegate);
          const loadOnDemandAfter = window.Vaadin && window.Vaadin.Flow
            && typeof window.Vaadin.Flow.loadOnDemand === 'function';
          if (loadOnDemandAfter) {
            let loadOnDemandProbe = 'not-invoked';
            try {
              const probeResult = await window.Vaadin.Flow.loadOnDemand(
                window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRequestedChunk
                  || 'vatori-flow-ipc-generated-lazy-v1_9-probe');
              loadOnDemandProbe = 'probe-returned=' + String(probeResult);
            } catch (probeError) {
              loadOnDemandProbe = 'probe-failed='
                + (probeError && probeError.message ? probeError.message : String(probeError));
            }
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyApiInstallSource =
              resource.generatedLazyApiInstallSource
              || 'generated-flow-imports-dev-bundle-after-direct-staged-import';
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyDiagnosticBridge = true;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyFlowClientTriggered = false;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandBlocker = true;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandResult =
              'api-installed-by-staged-generated-flow-imports;diagnostic-loadondemand-call;'
              + loadOnDemandProbe + ';triggerSource=diagnostic-preflight-and-direct-staged-import;'
              + 'flow-client-triggered=false;blocker=flow-ipc-loadondemand-v1_9';
          }
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecuted = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionResult =
            'executed:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkResult =
            'executed:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionError = '';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker = false;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyLoadOnDemandBlocker = true;
          recordFrontendStageOutcome(resource, stagedUrl, 'executed', {
            cleanupPolicy: usesStableModuleIdentity(resource)
              ? 'retain-for-window-lifetime-module-identity-v1_7'
              : 'revoke-after-generated-lazy-import',
            importResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyResolverVersion: resource.generatedLazyResolverVersion
              || 'flow-ipc-generated-lazy-resolver-v1_8',
            generatedLazyExecutionVersion: resource.generatedLazyExecutionVersion
              || 'flow-ipc-generated-lazy-v1_8',
            generatedLazyImportResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyLoadOnDemandContractVersion: resource.generatedLazyLoadOnDemandContractVersion
              || 'flow-ipc-loadondemand-v1_9',
            generatedLazyTriggerSource: resource.generatedLazyTriggerSource
              || 'diagnostic-preflight-and-direct-staged-import',
            generatedLazyApiOwner: resource.generatedLazyApiOwner || 'window.Vaadin.Flow.loadOnDemand',
            generatedLazyApiInstallSource: resource.generatedLazyApiInstallSource
              || 'generated-flow-imports-dev-bundle-after-direct-staged-import',
            generatedLazyRequestedChunk: resource.generatedLazyRequestedChunk
              || 'vatori-flow-ipc-generated-lazy-v1_9-probe',
            generatedLazyRegistrySource: resource.generatedLazyRegistrySource
              || 'no-bootstrap-lazy-registry-observed',
            generatedLazyMappingSource: resource.generatedLazyMappingSource
              || 'component-frontend-manifest-generated-lazy-v1_9',
            generatedLazyResolvedArtifactPath: resource.generatedLazyResolvedArtifactPath || resource.path,
            generatedLazyFlowClientTriggered: false,
            generatedLazyDiagnosticBridge: true,
            generatedLazyRelativeImportSpecifier: resource.generatedLazyRelativeImportSpecifier || '',
            generatedLazyResolvedDependencyPath: resource.generatedLazyResolvedDependencyPath || '',
            generatedLazyRelativeDependencyResolved:
              window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeDependencyResolved,
            generatedLazyGraphProbe: resource.generatedLazyGraphProbe || '',
            generatedLazyBlockerVersion: resource.generatedLazyBlockerVersion
              || resource.blockerVersion
              || 'flow-ipc-loadondemand-v1_9',
            moduleIdentityStrategy: usesStableModuleIdentity(resource)
              ? 'stable-resource-path-blob-module-identity-v1_7'
              : '',
            moduleIdentityKey: resource.path
          });
          for (const [dependencyPath, dependencyUrl] of urlCache.entries()) {
            if (dependencyPath === resource.path) {
              continue;
            }
            const dependency = componentFrontendResourceByPath(dependencyPath);
            recordFrontendStageOutcome(dependency, dependencyUrl, 'executed', {
              cleanupPolicy: usesStableModuleIdentity(dependency)
                ? 'retain-for-window-lifetime-module-identity-v1_7'
                : 'revoke-on-module-graph-complete',
              importResolutionStrategy: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyImportResolutionStrategy
                  || 'stable-relative-import-rewrite-generated-lazy-v1_8')
                : '',
              generatedLazyResolverVersion: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyResolverVersion || 'flow-ipc-generated-lazy-resolver-v1_8')
                : '',
              generatedLazyExecutionVersion: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyExecutionVersion || 'flow-ipc-generated-lazy-v1_8')
                : '',
              generatedLazyImportResolutionStrategy: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyImportResolutionStrategy
                  || 'stable-relative-import-rewrite-generated-lazy-v1_8')
                : '',
              generatedLazyLoadOnDemandContractVersion: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyLoadOnDemandContractVersion || 'flow-ipc-loadondemand-v1_9')
                : '',
              generatedLazyTriggerSource: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyTriggerSource || 'diagnostic-preflight-and-direct-staged-import')
                : '',
              generatedLazyApiOwner: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyApiOwner || 'window.Vaadin.Flow.loadOnDemand')
                : '',
              generatedLazyApiInstallSource: dependency && dependency.generatedLazyBundleDependency === true
                ? 'dependency-staged-for-generated-flow-imports'
                : '',
              generatedLazyRequestedChunk: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyRequestedChunk || 'vatori-flow-ipc-generated-lazy-v1_9-probe')
                : '',
              generatedLazyRegistrySource: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyRegistrySource || 'no-bootstrap-lazy-registry-observed')
                : '',
              generatedLazyMappingSource: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyMappingSource || 'component-frontend-manifest-generated-lazy-v1_9')
                : '',
              generatedLazyResolvedArtifactPath: dependency && dependency.generatedLazyBundleDependency === true
                ? dependencyPath
                : '',
              generatedLazyFlowClientTriggered: false,
              generatedLazyDiagnosticBridge: false,
              generatedLazyRelativeImportSpecifier: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyRelativeImportSpecifier || '')
                : '',
              generatedLazyResolvedDependencyPath: dependency && dependency.generatedLazyBundleDependency === true
                ? dependencyPath
                : '',
              generatedLazyRelativeDependencyResolved: dependency && dependency.generatedLazyBundleDependency === true,
              generatedLazyGraphProbe: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyGraphProbe || '')
                : '',
              generatedLazyBlockerVersion: dependency && dependency.generatedLazyBundleDependency === true
                ? (resource.generatedLazyBlockerVersion || resource.blockerVersion || 'flow-ipc-loadondemand-v1_9')
                : '',
              generatedLazySourceArtifact: dependency && dependency.generatedLazyBundleDependency === true
                ? resource.path
                : '',
              moduleIdentityStrategy: usesStableModuleIdentity(dependency)
                ? 'stable-resource-path-blob-module-identity-v1_7'
                : '',
              moduleIdentityKey: dependencyPath
            });
          }
        } catch (error) {
          const message = error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecuted = false;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionResult =
            'strict-blocker:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBundleChunkResult =
            'strict-blocker:' + resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyExecutionError = message;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker = true;
          recordFrontendStageOutcome(resource, stagedUrl, 'strict-blocker', {
            cleanupPolicy: usesStableModuleIdentity(resource)
              ? 'retain-for-window-lifetime-module-identity-v1_7'
              : 'revoke-after-generated-lazy-import',
            blockedReason: resource.blockedReason || 'generated-lazy-staged-execution-blocked',
            browserError: message,
            importResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyResolverVersion: resource.generatedLazyResolverVersion
              || 'flow-ipc-generated-lazy-resolver-v1_8',
            generatedLazyExecutionVersion: resource.generatedLazyExecutionVersion
              || 'flow-ipc-generated-lazy-v1_8',
            generatedLazyImportResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyLoadOnDemandContractVersion: resource.generatedLazyLoadOnDemandContractVersion
              || 'flow-ipc-loadondemand-v1_9',
            generatedLazyTriggerSource: resource.generatedLazyTriggerSource
              || 'diagnostic-preflight-and-direct-staged-import',
            generatedLazyApiOwner: resource.generatedLazyApiOwner || 'window.Vaadin.Flow.loadOnDemand',
            generatedLazyApiInstallSource: resource.generatedLazyApiInstallSource
              || 'generated-flow-imports-dev-bundle-after-direct-staged-import',
            generatedLazyRequestedChunk: resource.generatedLazyRequestedChunk
              || 'vatori-flow-ipc-generated-lazy-v1_9-probe',
            generatedLazyRegistrySource: resource.generatedLazyRegistrySource
              || 'no-bootstrap-lazy-registry-observed',
            generatedLazyMappingSource: resource.generatedLazyMappingSource
              || 'component-frontend-manifest-generated-lazy-v1_9',
            generatedLazyResolvedArtifactPath: resource.generatedLazyResolvedArtifactPath || resource.path,
            generatedLazyFlowClientTriggered: false,
            generatedLazyDiagnosticBridge: resource.generatedLazyDiagnosticBridge === true,
            generatedLazyRelativeImportSpecifier: resource.generatedLazyRelativeImportSpecifier || '',
            generatedLazyResolvedDependencyPath: resource.generatedLazyResolvedDependencyPath || '',
            generatedLazyRelativeDependencyResolved:
              window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeDependencyResolved,
            generatedLazyGraphProbe: resource.generatedLazyGraphProbe || '',
            generatedLazyBlockerVersion: resource.generatedLazyBlockerVersion
              || resource.blockerVersion
              || 'flow-ipc-loadondemand-v1_9',
            moduleIdentityStrategy: usesStableModuleIdentity(resource)
              ? 'stable-resource-path-blob-module-identity-v1_7'
              : '',
            moduleIdentityKey: resource.path
          });
          markStagedDependenciesStrictBlocker(urlCache, message, {
            skipPath: resource.path,
            generatedLazyResolverVersion: resource.generatedLazyResolverVersion
              || 'flow-ipc-generated-lazy-resolver-v1_8',
            generatedLazyExecutionVersion: resource.generatedLazyExecutionVersion
              || 'flow-ipc-generated-lazy-v1_8',
            generatedLazyImportResolutionStrategy: resource.generatedLazyImportResolutionStrategy
              || 'stable-relative-import-rewrite-generated-lazy-v1_8',
            generatedLazyLoadOnDemandContractVersion: resource.generatedLazyLoadOnDemandContractVersion
              || 'flow-ipc-loadondemand-v1_9',
            generatedLazyTriggerSource: resource.generatedLazyTriggerSource
              || 'diagnostic-preflight-and-direct-staged-import',
            generatedLazyApiOwner: resource.generatedLazyApiOwner || 'window.Vaadin.Flow.loadOnDemand',
            generatedLazyApiInstallSource: 'dependency-staged-for-generated-flow-imports',
            generatedLazyRequestedChunk: resource.generatedLazyRequestedChunk
              || 'vatori-flow-ipc-generated-lazy-v1_9-probe',
            generatedLazyRegistrySource: resource.generatedLazyRegistrySource
              || 'no-bootstrap-lazy-registry-observed',
            generatedLazyMappingSource: resource.generatedLazyMappingSource
              || 'component-frontend-manifest-generated-lazy-v1_9',
            generatedLazyResolvedArtifactPath: resource.generatedLazyResolvedArtifactPath || '',
            generatedLazyFlowClientTriggered: false,
            generatedLazyDiagnosticBridge: false,
            generatedLazyRelativeImportSpecifier: resource.generatedLazyRelativeImportSpecifier || '',
            generatedLazyResolvedDependencyPath: resource.generatedLazyResolvedDependencyPath || '',
            generatedLazyRelativeDependencyResolved:
              window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyRelativeDependencyResolved,
            generatedLazyBlockerVersion: resource.generatedLazyBlockerVersion
              || resource.blockerVersion
              || 'flow-ipc-loadondemand-v1_9',
            generatedLazyGraphProbe: resource.generatedLazyGraphProbe || ''
          });
        } finally {
          refreshFrontendStagingDiagnostics();
          for (const url of urlsToRevoke) {
            URL.revokeObjectURL(url);
          }
        }
      }

      async function maybeStageTextFieldDevBundleBlocker() {
        if (textFieldDevBundleStageAttempted
            || !window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified) {
          return;
        }
        const resource = textFieldDevBundleResource();
        if (!resource) {
          return;
        }
        const source = componentFrontendResourceBodies.get(resource.path) || '';
        if (!source) {
          return;
        }
        textFieldDevBundleStageAttempted = true;
        const urlsToRevoke = [];
        const urlCache = new Map();
        let stagedUrl = '';
        const registryBefore = customElementRegistrySnapshot('vaadin-text-field');
        const textFieldDefinedBefore = !!customElements.get('vaadin-text-field');
        const ownerReuseDetails = componentProductionOwnerReuseDiagnostics(resource, 'vaadin-text-field');
        const transitiveArtifacts = Array.isArray(resource.transitiveImports)
          ? resource.transitiveImports.join(',')
          : '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphParityVersion =
          resource.textfieldGraphParityVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphParityVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldBlockerVersion =
          resource.previousBlockerVersion || resource.blockerVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldBlockerVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecutionVersion =
          resource.executionVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecutionVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectVersion =
          resource.textfieldSideEffectVersion || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectVersion;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphProbe =
          resource.graphProbe || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldGraphProbe;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldEntryArtifact =
          resource.graphEntryArtifact || resource.path;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldClassExportArtifact =
          resource.classExportArtifact || window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldClassExportArtifact;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldTransitiveArtifacts =
          transitiveArtifacts;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldImportResolutionStrategy =
          resource.importResolutionStrategy || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSideEffectStrategy =
          resource.sideEffectStrategy || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldModuleIdentityStrategy =
          resource.moduleIdentityStrategy || resource.importResolutionStrategy || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldNarrowedFailureClassification =
          resource.narrowedFailureClassification || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldFailureLayer =
          resource.failureLayer || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldFirstDefinitionSource =
          resource.firstDefinitionSource || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldSecondDefinitionSource =
          resource.secondDefinitionSource || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldDuplicateTag =
          resource.duplicateTag || 'vaadin-text-field';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldRemainingBlockerClassification =
          resource.remainingBlockerClassification || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldShimUsed =
          resource.legacyShimSuccessPath === true;
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldMinimalReproduction =
          resource.minimalReproduction || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldNextResolutionStep =
          resource.nextResolutionStep || '';
        window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldCustomElementRegistryBefore =
          registryBefore;
        try {
          installOpaqueOriginStorageFallbacks();
          installVaadinDevBundleGlobals();
          stagedUrl = componentModuleBlobUrl(resource.path, urlsToRevoke, urlCache, 'execution-attempt', Object.assign({
            cleanupPolicy: 'retain-for-window-lifetime-module-identity-v1_7',
            previousBlockerVersion: resource.previousBlockerVersion || resource.blockerVersion || '',
            executionVersion: resource.executionVersion || '',
            textfieldSideEffectVersion: resource.textfieldSideEffectVersion || '',
            graphProbe: resource.graphProbe || '',
            graphEntryArtifact: resource.graphEntryArtifact || resource.path,
            classExportArtifact: resource.classExportArtifact || '',
            transitiveArtifacts,
            importResolutionStrategy: resource.importResolutionStrategy || '',
            sideEffectStrategy: resource.sideEffectStrategy || '',
            moduleIdentityStrategy: resource.moduleIdentityStrategy || resource.importResolutionStrategy || '',
            narrowedFailureClassification: resource.narrowedFailureClassification || '',
            failureLayer: resource.failureLayer || '',
            minimalReproduction: resource.minimalReproduction || '',
            nextResolutionStep: resource.nextResolutionStep || '',
            firstDefinitionSource: resource.firstDefinitionSource || '',
            secondDefinitionSource: resource.secondDefinitionSource || '',
            duplicateTag: resource.duplicateTag || 'vaadin-text-field',
            remainingBlockerClassification: resource.remainingBlockerClassification || ''
          }, ownerReuseDetails));
          if (!stagedUrl) {
            throw new Error(window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
              || 'textfield-staging-rewrite-failed');
          }
          await import(stagedUrl);
          if (customElements.get('vaadin-text-field')) {
            window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldExecuted = true;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlocker = false;
            window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerError = '';
            window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerReason = '';
            window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerDependency = '';
            window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldRemainingBlockerClassification =
              resource.remainingBlockerClassification || 'none';
            recordFrontendStageOutcome(resource, stagedUrl, 'executed', Object.assign({
              cleanupPolicy: 'retain-for-window-lifetime-module-identity-v1_7',
              previousBlockerVersion: resource.previousBlockerVersion || resource.blockerVersion || '',
              executionVersion: resource.executionVersion || '',
              textfieldSideEffectVersion: resource.textfieldSideEffectVersion || '',
              textfieldGraphParityVersion: resource.textfieldGraphParityVersion || '',
              graphProbe: resource.graphProbe || '',
              graphEntryArtifact: resource.graphEntryArtifact || resource.path,
              classExportArtifact: resource.classExportArtifact || '',
              transitiveArtifacts,
              importResolutionStrategy: resource.importResolutionStrategy || '',
              sideEffectStrategy: resource.sideEffectStrategy || '',
              moduleIdentityStrategy: resource.moduleIdentityStrategy || resource.importResolutionStrategy || '',
              narrowedFailureClassification: resource.narrowedFailureClassification || '',
              failureLayer: resource.failureLayer || '',
              minimalReproduction: resource.minimalReproduction || '',
              nextResolutionStep: resource.nextResolutionStep || '',
              customElementTag: 'vaadin-text-field',
              customElementDefinedBefore: textFieldDefinedBefore,
              customElementDefinedAfter: true,
              customElementRegistryBefore: registryBefore,
              customElementRegistryAfter: customElementRegistrySnapshot('vaadin-text-field'),
              firstDefinitionSource: resource.firstDefinitionSource || '',
              secondDefinitionSource: resource.secondDefinitionSource || '',
              duplicateTag: resource.duplicateTag || 'vaadin-text-field',
              remainingBlockerClassification: resource.remainingBlockerClassification || 'none'
            }, ownerReuseDetails));
            for (const [dependencyPath, dependencyUrl] of urlCache.entries()) {
              const dependency = componentFrontendResourceByPath(dependencyPath);
              recordFrontendStageOutcome(dependency, dependencyUrl, 'executed', {
                cleanupPolicy: usesStableModuleIdentity(dependency)
                  ? 'retain-for-window-lifetime-module-identity-v1_7'
                  : 'revoke-on-module-graph-complete',
                moduleIdentityStrategy: usesStableModuleIdentity(dependency)
                  ? 'stable-resource-path-blob-module-identity-v1_7'
                  : ''
              });
            }
            return;
          }
          throw new Error('vaadin-text-field not defined after staged dev-bundle import');
        } catch (error) {
          const message = error && error.message ? error.message : String(error);
          const registryAfter = customElementRegistrySnapshot('vaadin-text-field');
          const textFieldDefinedAfter = !!customElements.get('vaadin-text-field');
          const dependency = Array.from(urlCache.keys()).join(',');
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlocker = true;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerPath = resource.path;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerReason =
            resource.blockedReason || 'textfield-staged-dev-bundle-execution-blocked';
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerError = message;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldStagedBlockerDependency = dependency;
          window.VaadinDesktopIpcDiagnostics.frontendArtifactTextFieldCustomElementRegistryAfter =
            registryAfter;
          const blockerDetails = {
            cleanupPolicy: 'retain-for-window-lifetime-module-identity-v1_7',
            blockerVersion: resource.executionVersion || 'flow-ipc-textfield-v1_7',
            previousBlockerVersion: resource.previousBlockerVersion || resource.blockerVersion || '',
            executionVersion: resource.executionVersion || '',
            textfieldSideEffectVersion: resource.textfieldSideEffectVersion || '',
            textfieldGraphParityVersion: resource.textfieldGraphParityVersion || '',
            graphProbe: resource.graphProbe || '',
            graphEntryArtifact: resource.graphEntryArtifact || resource.path,
            classExportArtifact: resource.classExportArtifact || '',
            transitiveArtifacts: dependency || transitiveArtifacts,
            importResolutionStrategy: resource.importResolutionStrategy || '',
            sideEffectStrategy: resource.sideEffectStrategy || '',
            moduleIdentityStrategy: resource.moduleIdentityStrategy || resource.importResolutionStrategy || '',
            failureClassification: resource.failureClassification || 'textfield-production-class-execution-blocked',
            narrowedFailureClassification: resource.narrowedFailureClassification || '',
            failureLayer: message.includes('multiple custom elements')
              ? 'module-identity-regression-duplicate-custom-element-v1_7'
              : 'textfield-real-graph-execution-failed-v1_7',
            blockedReason: resource.blockedReason || 'textfield-staged-dev-bundle-execution-blocked',
            browserError: message,
            minimalReproduction: resource.minimalReproduction || '',
            nextResolutionStep: resource.nextResolutionStep || '',
            customElementTag: 'vaadin-text-field',
            customElementDefinedBefore: textFieldDefinedBefore,
            customElementDefinedAfter: textFieldDefinedAfter,
            customElementRegistryBefore: registryBefore,
            customElementRegistryAfter: registryAfter,
            firstDefinitionSource: textFieldDefinedBefore
              ? (resource.firstDefinitionSource || 'customElements registry before TextField v1.7 staged probe')
              : 'not-defined-before-v1_7-probe',
            secondDefinitionSource: textFieldDefinedAfter
              ? (resource.secondDefinitionSource || 'TextField staged dev-bundle graph')
              : 'not-defined-after-v1_7-probe',
            duplicateTag: resource.duplicateTag || 'vaadin-text-field',
            remainingBlockerClassification: 'textfield-real-graph-execution-failed-v1_7'
          };
          recordFrontendStageOutcome(resource, stagedUrl, 'strict-blocker',
            Object.assign({}, blockerDetails, ownerReuseDetails));
          markStagedDependenciesStrictBlocker(urlCache, message, blockerDetails);
        } finally {
          refreshFrontendStagingDiagnostics();
          for (const url of urlsToRevoke) {
            URL.revokeObjectURL(url);
          }
        }
      }

      async function executeComponentFrontendResource(resource) {
        const primaryPath = resource.path;
        const primaryBody = componentFrontendResourceBodies.get(primaryPath) || '';
        if (!primaryBody) {
          componentFrontendResourceExecutionFailedPaths.add(primaryPath);
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
            'missing-primary-component-resource-body';
          refreshComponentFrontendResourceDiagnostics();
          return;
        }
        const componentTag = componentTagForResource(resource);
        const ownerReuseDetails = componentProductionOwnerReuseDiagnostics(resource, componentTag);
        const urlsToRevoke = [];
        const urlCache = new Map();
        let primaryUrl = '';
        try {
          let importSource = primaryBody;
          if (Array.isArray(resource.importRewrites) && resource.importRewrites.length > 0) {
            importSource = rewriteManifestImportSpecifiers(primaryBody, resource, urlsToRevoke, urlCache);
            if (!importSource) {
              componentFrontendResourceExecutionFailedPaths.add(primaryPath);
              refreshComponentFrontendResourceDiagnostics();
              return;
            }
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode =
              resource.browserExecutionPlan || 'dynamic-import-blob-with-manifest-import-rewrites-v1_4';
          } else if (resource.devBundleResource === true) {
            installOpaqueOriginStorageFallbacks();
            installVaadinDevBundleGlobals();
            importSource = rewriteVaadinDevBundleModule(primaryBody, primaryPath, urlsToRevoke, urlCache);
            if (!importSource) {
              componentFrontendResourceExecutionFailedPaths.add(primaryPath);
              refreshComponentFrontendResourceDiagnostics();
              return;
            }
            if (!window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode) {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode =
                'dynamic-import-blob-vaadin-dev-bundle-production-build-v1_4';
            }
          } else if (!window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode =
              'dynamic-import-blob-generic-component-frontend-v1_3';
          }
          primaryUrl = stageFrontendArtifact(resource, importSource, 'execution-attempt', urlsToRevoke, null,
            Object.assign({
              cleanupPolicy: 'revoke-after-dynamic-import'
            }, ownerReuseDetails));
          await import(primaryUrl);
          if (componentTag && !customElements.get(componentTag)) {
            componentFrontendResourceExecutionFailedPaths.add(primaryPath);
            const textFieldDefined = !!customElements.get('vaadin-text-field');
            const featureFlagKey = componentTag
              ? componentTag.split('-').slice(1).map((part, index) => {
                  return index === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1);
                }).join('') + 'Component'
              : '';
            const featureFlagEnabled = featureFlagKey && window.Vaadin && window.Vaadin.featureFlags
              ? String(!!window.Vaadin.featureFlags[featureFlagKey])
              : '';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
              'custom-element-not-defined-after-import tag=' + componentTag
              + ' textFieldDefined=' + textFieldDefined
              + ' featureFlag=' + featureFlagKey + ':' + featureFlagEnabled;
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
              'custom-element-not-defined-after-import';
            recordFrontendStageOutcome(resource, primaryUrl, 'execution-failed',
              Object.assign({
                cleanupPolicy: 'revoke-after-dynamic-import',
                browserError: window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
              }, ownerReuseDetails));
            refreshComponentFrontendResourceDiagnostics();
            return;
          }
          componentFrontendResourceExecutedPaths.add(primaryPath);
          recordFrontendStageOutcome(resource, primaryUrl, 'executed',
            Object.assign({
              cleanupPolicy: 'revoke-after-dynamic-import'
            }, ownerReuseDetails));
          for (const [dependencyPath, dependencyUrl] of urlCache.entries()) {
            const dependency = componentFrontendResourceByPath(dependencyPath);
            recordFrontendStageOutcome(dependency, dependencyUrl, 'executed', {
              cleanupPolicy: usesStableModuleIdentity(dependency)
                ? 'retain-for-window-lifetime-module-identity-v1_7'
                : 'revoke-on-module-graph-complete',
              moduleIdentityStrategy: usesStableModuleIdentity(dependency)
                ? 'stable-resource-path-blob-module-identity-v1_7'
                : ''
            });
          }
          if (componentTag) {
            componentFrontendResourceExecutedTags.add(componentTag);
          }
          if (!window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath = primaryPath;
          }
          if (componentTag === 'vaadin-big-decimal-field') {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentTagDefined = true;
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason = '';
          }
          if (componentTag === 'vaadin-checkbox') {
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined = true;
            window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentFailureReason = '';
          }
          refreshComponentFrontendResourceDiagnostics();
        } catch (error) {
          if (componentTag && customElements.get(componentTag)) {
            componentFrontendResourceExecutedPaths.add(primaryPath);
            componentFrontendResourceExecutionFailedPaths.delete(primaryPath);
            recordFrontendStageOutcome(resource, primaryUrl, 'executed',
              Object.assign({
                cleanupPolicy: 'revoke-after-dynamic-import',
                browserError: error && error.message ? error.message : String(error)
              }, ownerReuseDetails));
            componentFrontendResourceExecutedTags.add(componentTag);
            if (!window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath) {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath = primaryPath;
            }
            if (componentTag === 'vaadin-checkbox') {
              window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined = true;
              window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentFailureReason = '';
            }
            if (componentTag === 'vaadin-big-decimal-field') {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentTagDefined = true;
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason = '';
            }
            if (componentFrontendResourceExecutionFailedPaths.size === 0) {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError = '';
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason = '';
            }
            refreshComponentFrontendResourceDiagnostics();
            return;
          }
          componentFrontendResourceExecutionFailedPaths.add(primaryPath);
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
            error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
            'browser-execution-error';
          recordFrontendStageOutcome(resource, primaryUrl, 'execution-failed', {
            cleanupPolicy: 'revoke-after-dynamic-import',
            browserError: window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError
          });
          refreshComponentFrontendResourceDiagnostics();
        } finally {
          for (const url of urlsToRevoke) {
            URL.revokeObjectURL(url);
          }
        }
      }

      async function handleFlowBootstrapResourceResponse(payload, body, metadataMatches) {
        const bodyLooksLikeFlowBootstrap = body.includes('window.Vaadin.Flow.initApplication') && body.includes('export { init }');
        window.VaadinDesktopIpcDiagnostics.flowClientResourceLoaded = true;
        window.VaadinDesktopIpcDiagnostics.flowClientResourcePath = payload.path || '';
        window.VaadinDesktopIpcDiagnostics.flowClientResourceBytes = Number(payload.bytes || body.length);
        window.VaadinDesktopIpcDiagnostics.flowClientResourceSha256 = payload.sha256 || '';
        window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified = metadataMatches && bodyLooksLikeFlowBootstrap;
        if (!body) {
          trace.textContent = 'flow resource ' + (payload.path || 'unknown') + ' empty';
          return;
        }
        try {
          const blobUrl = URL.createObjectURL(new Blob([body], { type: 'text/javascript' }));
          try {
            const module = await import(blobUrl);
            window.VaadinDesktopIpcDiagnostics.flowClientResourceModuleLoaded = true;
            window.VaadinDesktopIpcDiagnostics.flowClientBootstrapExported = typeof module.init === 'function';
            initializeFlowBootstrapModule(module);
            markFlowClientResourceExecuted(payload.path || 'META-INF/frontend/FlowBootstrap.js');
            window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified =
              window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified
              && window.VaadinDesktopIpcDiagnostics.flowClientBootstrapExported
              && window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized;
            trace.textContent = 'flow resource module ' + (window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified ? 'verified' : 'loaded');
          } finally {
            URL.revokeObjectURL(blobUrl);
          }
        } catch (error) {
          flowClientResourceExecutionFailedPaths.add(payload.path || 'META-INF/frontend/FlowBootstrap.js');
          refreshFlowClientResourceExecutionDiagnostics();
          window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionError = error && error.message ? error.message : String(error);
          trace.textContent = 'flow resource module failed';
        }
      }

      async function handleFlowClientEngineResourceResponse(payload, body, metadataMatches) {
        const bodyLooksLikeFlowClient = body.includes('export function init')
          && body.includes('Vaadin.Flow.registerWidgetset')
          && body.includes('handleIpcMessage')
          && body.includes('requestIpcResync');
        flowClientEngineResourceBaseVerified = metadataMatches && bodyLooksLikeFlowClient;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceLoaded = true;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineResourcePath = payload.path || '';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceBytes = Number(payload.bytes || body.length);
        window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceSha256 = payload.sha256 || '';
        window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceVerified = flowClientEngineResourceBaseVerified;
        if (!body) {
          trace.textContent = 'flow client engine resource empty';
          return;
        }
        try {
          const blobUrl = URL.createObjectURL(new Blob([body], { type: 'text/javascript' }));
          try {
            const module = await import(blobUrl);
            window.VaadinDesktopIpcDiagnostics.flowClientEngineModuleLoaded = true;
            markFlowClientResourceExecuted(payload.path || 'META-INF/frontend/FlowClient.js');
            initializeFlowClientEngineModule(module);
            requestRemainingFlowClientResourceGraph();
            requestComponentFrontendResourceGraph();
            trace.textContent = 'flow client engine ' + (window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceVerified ? 'verified' : 'loaded');
          } finally {
            URL.revokeObjectURL(blobUrl);
          }
        } catch (error) {
          flowClientResourceExecutionFailedPaths.add(payload.path || 'META-INF/frontend/FlowClient.js');
          refreshFlowClientResourceExecutionDiagnostics();
          window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceExecutionError = error && error.message ? error.message : String(error);
          trace.textContent = 'flow client engine failed';
        }
      }

      function appendFileTransferResultOrder(value) {
        const current = window.VaadinDesktopIpcDiagnostics.fileTransferResultOrder || '';
        window.VaadinDesktopIpcDiagnostics.fileTransferResultOrder = current ? current + ',' + value : value;
      }

      function handleCapabilityResult(message) {
        const payload = message && message.payload ? message.payload : {};
        const command = payload.command || '';
        if (command === 'fileTransfer.download') {
          window.VaadinDesktopIpcDiagnostics.fileTransferDownloadMode = payload.mode || '';
          window.VaadinDesktopIpcDiagnostics.fileTransferDownloadPath = payload.path || '';
          window.VaadinDesktopIpcDiagnostics.fileTransferDownloadByteCount = Number(payload.byteCount || 0);
          window.VaadinDesktopIpcDiagnostics.fileTransferDownloadBodyRedacted = payload.bodyRedacted === true;
          window.VaadinDesktopIpcDiagnostics.fileTransferDownloadResultObserved =
            payload.success === true
            && payload.operation === 'download'
            && payload.canceled !== true
            && window.VaadinDesktopIpcDiagnostics.fileTransferDownloadByteCount > 0
            && payload.bodyRedacted === true
            && !!payload.path;
          appendFileTransferResultOrder('download');
        } else if (command === 'fileTransfer.selectUploadFile') {
          window.VaadinDesktopIpcDiagnostics.fileTransferUploadMode = payload.mode || '';
          window.VaadinDesktopIpcDiagnostics.fileTransferUploadPath = payload.path || '';
          window.VaadinDesktopIpcDiagnostics.fileTransferUploadByteCount = Number(payload.byteCount || 0);
          window.VaadinDesktopIpcDiagnostics.fileTransferUploadSampledBytes = Number(payload.sampledBytes || 0);
          window.VaadinDesktopIpcDiagnostics.fileTransferUploadBodyRedacted = payload.bodyRedacted === true;
          window.VaadinDesktopIpcDiagnostics.fileTransferUploadResultObserved =
            payload.success === true
            && payload.operation === 'upload'
            && payload.canceled !== true
            && window.VaadinDesktopIpcDiagnostics.fileTransferUploadByteCount > 0
            && window.VaadinDesktopIpcDiagnostics.fileTransferUploadSampledBytes > 0
            && payload.bodyRedacted === true
            && !!payload.path;
          appendFileTransferResultOrder('upload');
        }
      }

      window.VaadinDesktopIpc = {
        receive(message) {
          try {
            trace.textContent = 'recv ' + message.type + ' #' + message.id;
            observeFlowIpcServerSequence(message);
            if (message.type === 'bootstrapResponse' || message.type === 'patch' || message.type === 'push' || message.type === 'resync') {
              rememberFlowClientNodeIds(message.payload);
              const flowClientCompleted = completeFlowClientXhrIfPending(message);
              updateClientState(message.payload);
              let flowClientOwnsMessage = flowClientCompleted;
              if (message.type === 'push') {
                flowClientOwnsMessage = deliverFlowClientEngineMessage(message, 'push') || queueFlowClientEngineMessage(message, 'push');
              } else if (!flowClientCompleted && message.payload && message.payload.uidl && message.payload.uidl.resync === true) {
                flowClientOwnsMessage = deliverFlowClientEngineMessage(message, 'resync') || queueFlowClientEngineMessage(message, 'resync');
              }
              if (message.type === 'bootstrapResponse') {
                if (!applyUidlBootstrap(message.payload)) {
                  render(message.payload && message.payload.tree);
                }
                rememberFlowClientBootstrapPayload(message.payload);
                requestFlowClientResourceProbe(message.payload);
                requestComponentFrontendResourceProbe(message.payload, false);
                scheduleFlowClientEnginePromotionRetry();
                startAutoFrameworkUpdatePolling();
              } else if (!flowClientOwnsMessage && message.payload && message.payload.uidl && message.payload.uidl.resync === true) {
                if (!applyUidlBootstrap(message.payload)) {
                  render(message.payload && message.payload.tree);
                }
              } else if (!flowClientOwnsMessage && !applyUidlPatch(message.payload)) {
                render(message.payload && message.payload.tree);
              }
              if (message.type === 'patch' && message.payload && message.payload.reason === 'event') {
                scheduleAutoComponentEvents();
                startAutoFrameworkUpdatePolling();
              }
              if (message.type === 'patch' && message.payload && message.payload.reason === 'event'
                  && window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ && !autoResyncDone) {
                requestAutoResyncIfReady();
              }
              if (message.type === 'bootstrapResponse') {
                scheduleAutoClick();
                startAutoFrameworkUpdatePolling();
              }
              scheduleAutoErrorProbe();
            } else if (message.type === 'resourceResponse') {
              handleResourceResponse(message.payload);
            } else if (message.type === 'capabilityResult') {
              handleCapabilityResult(message);
            } else if (message.type === 'error') {
              app.textContent = 'Flow IPC error: ' + JSON.stringify(message.error || message.payload);
            }
          } catch (error) {
            const errorMessage = error && error.message ? error.message : String(error);
            window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = errorMessage;
            sendWebViewError('flowIpcReceiveFailed', errorMessage, {
              receivedType: message && message.type ? message.type : '',
              receivedId: message && message.id ? message.id : ''
            });
          }
        }
      };

      document.addEventListener('DOMContentLoaded', () => {
        send('bootstrapRequest', { route: '' });
        startAutoFrameworkUpdatePolling();
      });
    })();
  </script>
</body>
</html>
"""#

let config = HostConfig.parse()
let application = NSApplication.shared
let delegate = WebViewHost(config: config)
application.delegate = delegate
application.setActivationPolicy(.regular)
application.run()
