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
        config.flowIpcAutoClick = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK"] == "1"
        config.flowIpcAutoResync = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC"] == "1"
        config.flowIpcAutoError = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR"] == "1"
        config.flowIpcAutoFileDialogCancel = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL"] == "1"
            || config.flowIpcAutoClick
        config.flowIpcAutoVaadinTextField = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD"] == "1"
            || config.flowIpcAutoClick
        config.flowIpcAutoFrameworkUpdate = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE"] == "1"
        config.flowIpcAutoShutdown = ProcessInfo.processInfo.environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN"] == "1"
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
        process.arguments = [
            "-U",
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
    private var sidecar: JavaSidecar?
    private var window: NSWindow?
    private var webView: WKWebView?
    private var didOpenWindow = false
    private var fileDialogInFlight = false
    private var fileTransferPanelInFlight = false

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
        window.makeKeyAndOrderFront(nil)

        self.window = window
        self.webView = webView

        NSRunningApplication.current.activate(options: [.activateAllWindows])
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
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        if config.flowIpcAutoFrameworkUpdate {
            let source = "window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE__ = true;"
            userContentController.addUserScript(WKUserScript(source: source, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        }
        userContentController.add(self, name: "flowIpc")

        let configuration = WKWebViewConfiguration()
        configuration.userContentController = userContentController
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
        window.makeKeyAndOrderFront(nil)

        self.window = window
        self.webView = webView

        NSRunningApplication.current.activate(options: [.activateAllWindows])
        webView.loadHTMLString(flowIpcShell, baseURL: nil)
        trace("recv", "window.ready", ["label": "main", "transport": "flow-ipc"])
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
        if messageType == "webview.firstMeaningfulPaint" {
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
        }
        if config.flowIpcAutoFrameworkUpdate {
            DispatchQueue.main.asyncAfter(deadline: .now() + 14.0) {
                self.inspectWebView(webView)
            }
        }
        let snapshotDelay = config.flowIpcAutoFrameworkUpdate ? 14.6 : (extendedFlowIpcVerification ? 9.5 : 5.0)
        DispatchQueue.main.asyncAfter(deadline: .now() + snapshotDelay) {
            self.writeSnapshot(webView)
        }
        if config.flowIpcAutoShutdown {
            let shutdownDelay = config.flowIpcAutoFrameworkUpdate ? 15.6 : (extendedFlowIpcVerification ? 10.5 : 5.6)
            DispatchQueue.main.asyncAfter(deadline: .now() + shutdownDelay) {
                trace("recv", "window.closeRequested", [
                    "label": "main",
                    "reason": "flow-ipc-auto-shutdown"
                ])
                self.window?.close()
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
          frontendArtifactThemeStaticVerified: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticVerified),
          frontendArtifactThemeStaticPath: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticPath : "",
          frontendArtifactThemeStaticBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactThemeStaticBlocker),
          frontendArtifactDevModeBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactDevModeBlocker),
          frontendArtifactGeneratedLazyBlocker: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.frontendArtifactGeneratedLazyBlocker),
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

    private func writeSnapshot(_ webView: WKWebView) {
        let snapshotConfig = WKSnapshotConfiguration()
        snapshotConfig.rect = webView.bounds
        webView.takeSnapshot(with: snapshotConfig) { image, error in
            if let error {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": error.localizedDescription
                ])
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
            } catch {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": error.localizedDescription
                ])
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
      let originalXMLHttpRequest = null;
      let flowClientResourceGraphMetadataVerified = true;
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
        frontendArtifactThemeStaticVerified: false,
        frontendArtifactThemeStaticPath: '',
        frontendArtifactThemeStaticBlocker: false,
        frontendArtifactDevModeBlocker: false,
        frontendArtifactGeneratedLazyBlocker: false,
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

      function flowClientEngineClient() {
        const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients ? window.Vaadin.Flow.clients : {};
        return clients[flowBootstrapClientId] || clients[flowBootstrapAppId] || null;
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
          if (element && (element.id === id || element.getAttribute('id') === id
              || element.dataset && element.dataset.flowId === id)) {
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
        const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients ? window.Vaadin.Flow.clients : {};
        const client = clients[flowBootstrapClientId] || clients[flowBootstrapAppId];
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
          }
          if (message.payload && message.payload.reason === 'resync') {
            window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed = true;
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
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventDispatchMode = 'dom-event';
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventComponentId = descriptor.id;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventType = descriptor.eventType;
        window.VaadinDesktopIpcDiagnostics.vaadinWebComponentEventValue = String(descriptor.value);
      }

      function markVaadinComponentDomEventObserved(descriptor) {
        if (descriptor.key === 'vaadin-checkbox') {
          window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentEventObserved = true;
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
            && entry.executionPolicy === 'blocked-shim-fallback');
        window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundlePartial =
          window.VaadinDesktopIpcDiagnostics.frontendArtifactProductionBundlePartial
          || resources.some((entry) => entry
            && entry.artifactFamily === 'production-bundle'
            && entry.artifactStatus === 'partial');
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
          artifactStatus: resource.artifactStatus || ''
        };
      }

      function requestFlowClientResourceProbe(payload) {
        flowClientResources = payload && payload.flowClientResources && Array.isArray(payload.flowClientResources.resources)
          ? payload.flowClientResources.resources
          : [];
        recordFrontendArtifactManifest(payload && payload.flowClientResources ? payload.flowClientResources : {});
        window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount = flowClientResources.length;
        requestFlowClientResource('META-INF/frontend/FlowBootstrap.js', 'flow-bootstrap-module', 'flow-client-resource-probe');
      }

      function requestComponentFrontendResourceProbe(payload) {
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
        for (const resource of componentFrontendResources) {
          if (!resource || typeof resource.path !== 'string') {
            continue;
          }
          requestComponentFrontendResource(resource.path, resource.kind || '', 'vaadin-web-component-resource-probe');
        }
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
        const requestPayload = Object.assign({
          purpose,
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        }, artifactRequestMetadata(resource));
        send('resourceRequest', requestPayload);
      }

      function requestComponentFrontendResource(path, kind, purpose) {
        const resource = componentFrontendResources.find((entry) => entry.path === path)
          || componentFrontendResources.find((entry) => entry.kind === kind);
        if (!resource || typeof resource.path !== 'string') {
          return;
        }
        if (componentFrontendResourceRequests.has(resource.path)) {
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
        const requestPayload = Object.assign({
          purpose,
          lookupKind: resource.lookupKind || 'component-frontend-resource',
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        }, artifactRequestMetadata(resource));
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
        const requestPayload = Object.assign({
          purpose: 'flow-client-lazy-lookup',
          lookupKind: 'Flow.loadOnDemand-minimum-probe',
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        }, artifactRequestMetadata(resource));
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
        window.VaadinDesktopIpcDiagnostics.flowClientBootstrapAppId = flowBootstrapAppId;
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
          if (tag === 'vaadin-big-decimal-field' || tag === 'vaadin-checkbox') {
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
          if (node.tag === 'vaadin-big-decimal-field' || node.tag === 'vaadin-checkbox') {
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
          module.init(flowClientBootstrapInitResponse);
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
        const client = clients[flowBootstrapClientId] || clients[flowBootstrapAppId];
        window.VaadinDesktopIpcDiagnostics.flowClientEngineClientKey = clients[flowBootstrapClientId] ? flowBootstrapClientId : (clients[flowBootstrapAppId] ? flowBootstrapAppId : '');
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
        await maybeExecuteVaadinWebComponentResource();
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

      function componentModuleBlobUrl(path, urlsToRevoke) {
        const resource = componentFrontendResourceByPath(path);
        let body = componentFrontendResourceBodies.get(path) || '';
        if (!body) {
          return '';
        }
        if (resource && resource.devBundleResource === true && resource.componentBundleDependency === true) {
          body = rewriteVaadinDevBundleModule(body, path, urlsToRevoke);
          if (!body) {
            return '';
          }
        }
        const url = URL.createObjectURL(new Blob([body], { type: 'text/javascript' }));
        urlsToRevoke.push(url);
        return url;
      }

      function rewriteVaadinBigDecimalFieldModule(source, urlsToRevoke) {
        const litUrl = componentModuleBlobUrl('META-INF/vatori/frontend-shims/lit.js', urlsToRevoke);
        const textFieldUrl = componentModuleBlobUrl('META-INF/vatori/frontend-shims/vaadin-text-field.js', urlsToRevoke);
        const defineUrl = componentModuleBlobUrl('META-INF/vatori/frontend-shims/component-base-define.js', urlsToRevoke);
        if (!litUrl || !textFieldUrl || !defineUrl) {
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
            'missing-v1_2-component-import-shim';
          return '';
        }
        return source
          .replace("from 'lit'", "from '" + litUrl + "'")
            .replace("from '@vaadin/text-field/src/vaadin-text-field.js'", "from '" + textFieldUrl + "'")
            .replace("from '@vaadin/component-base/src/define.js'", "from '" + defineUrl + "'");
      }

      function componentModuleFileName(path) {
        const index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
      }

      function rewriteVaadinDevBundleModule(source, currentPath, urlsToRevoke) {
        let rewritten = source;
        for (const resource of componentFrontendResources) {
          if (!resource
              || resource.devBundleResource !== true
              || resource.componentBundleDependency !== true
              || resource.path === currentPath) {
            continue;
          }
          const specifier = './' + componentModuleFileName(resource.path);
          if (!rewritten.includes('"' + specifier + '"') && !rewritten.includes("'" + specifier + "'")) {
            continue;
          }
          const url = componentModuleBlobUrl(resource.path, urlsToRevoke);
          if (!url) {
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
              'missing-v1_4-dev-bundle-dependency';
            return '';
          }
          rewritten = rewritten
            .split('"' + specifier + '"').join('"' + url + '"')
            .split("'" + specifier + "'").join("'" + url + "'");
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
        if (!window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceGraphVerified) {
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
        const urlsToRevoke = [];
        try {
          let importSource = primaryBody;
          if (primaryPath === 'META-INF/resources/frontend/vaadin-big-decimal-field.js') {
            importSource = rewriteVaadinBigDecimalFieldModule(primaryBody, urlsToRevoke);
            if (!importSource) {
              componentFrontendResourceExecutionFailedPaths.add(primaryPath);
              refreshComponentFrontendResourceDiagnostics();
              return;
            }
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceImportMode =
              'dynamic-import-blob-with-v1_2-shimmed-transitive-imports';
          } else if (resource.devBundleResource === true) {
            installOpaqueOriginStorageFallbacks();
            installVaadinDevBundleGlobals();
            importSource = rewriteVaadinDevBundleModule(primaryBody, primaryPath, urlsToRevoke);
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
          const primaryUrl = URL.createObjectURL(new Blob([importSource], { type: 'text/javascript' }));
          urlsToRevoke.push(primaryUrl);
          await import(primaryUrl);
          if (componentTag && !customElements.get(componentTag)) {
            componentFrontendResourceExecutionFailedPaths.add(primaryPath);
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
              'custom-element-not-defined-after-import';
            refreshComponentFrontendResourceDiagnostics();
            return;
          }
          componentFrontendResourceExecutedPaths.add(primaryPath);
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
          if (resource.devBundleResource === true && componentTag && customElements.get(componentTag)) {
            componentFrontendResourceExecutedPaths.add(primaryPath);
            componentFrontendResourceExecutedTags.add(componentTag);
            if (!window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath) {
              window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutedPath = primaryPath;
            }
            if (componentTag === 'vaadin-checkbox') {
              window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentTagDefined = true;
              window.VaadinDesktopIpcDiagnostics.vaadinSecondWebComponentFailureReason = '';
            }
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError = '';
            window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason = '';
            refreshComponentFrontendResourceDiagnostics();
            return;
          }
          componentFrontendResourceExecutionFailedPaths.add(primaryPath);
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentResourceExecutionError =
            error && error.message ? error.message : String(error);
          window.VaadinDesktopIpcDiagnostics.vaadinWebComponentFailureReason =
            'browser-execution-error';
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
                requestComponentFrontendResourceProbe(message.payload);
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
