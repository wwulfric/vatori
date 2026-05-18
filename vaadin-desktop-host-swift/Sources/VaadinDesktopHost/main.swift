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
        window.level = .floating
        window.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        window.center()
        window.makeKeyAndOrderFront(nil)
        window.orderFrontRegardless()

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
        window.level = .floating
        window.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        window.center()
        window.makeKeyAndOrderFront(nil)
        window.orderFrontRegardless()

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
        trace("send", object["type"] as? String ?? "unknown", [
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
        DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
            self.writeSnapshot(webView)
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
          hasVaadinRoot: !!document.querySelector("vaadin-app-layout, vaadin-vertical-layout, flow-container-root, flow-container-root-2521314, #flow-client-engine-root"),
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
          flowClientBootstrapInitError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitError : "",
          flowClientResourceExecutionError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientResourceExecutionError : "",
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
          flowClientEngineQueuedMessageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageCount : -1,
          flowClientEngineQueuedMessageHighWater: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageHighWater : 0,
          flowClientEngineQueuedPushCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedPushCount : 0,
          flowClientEngineQueuedMessageOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineQueuedMessageOrder : "",
          flowClientEngineDeliveredQueuedMessageCount: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredQueuedMessageCount : 0,
          flowClientEngineDeliveredQueuedMessageOrder: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineDeliveredQueuedMessageOrder : "",
          flowClientEngineResyncRequested: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncRequested),
          flowClientEngineResyncPatchConsumed: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineResyncPatchConsumed),
          flowClientEngineErrorReported: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineErrorReported),
          flowClientEngineErrorCode: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineErrorCode : "",
          flowClientEngineVisibleOwner: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner),
          flowClientEngineVisibleRootId: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleRootId : "",
          flowClientEngineVisibleStatusText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleStatusText : "",
          flowClientEngineVisibleButtonText: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleButtonText : "",
          flowClientEngineLocalFallbackHidden: !!(window.VaadinDesktopIpcDiagnostics && window.VaadinDesktopIpcDiagnostics.flowClientEngineLocalFallbackHidden),
          flowClientEngineXhrError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError : "",
          flowClientEngineInitError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError : "",
          flowClientEngineResourceExecutionError: window.VaadinDesktopIpcDiagnostics ? window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceExecutionError : ""
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
      color-scheme: light dark;
      font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", system-ui, sans-serif;
      background: Canvas;
      color: CanvasText;
    }
    body {
      margin: 0;
      min-height: 100vh;
      background: Canvas;
    }
    #app,
    #flow-client-engine-root {
      box-sizing: border-box;
      min-height: 100vh;
      padding: 28px;
    }
    #app[hidden],
    #flow-client-engine-root[hidden] {
      display: none !important;
    }
    #flow-client-engine-root {
      display: block;
    }
    flow-container-root-2521314 {
      display: block;
      min-height: calc(100vh - 56px);
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
      border: 1px solid color-mix(in srgb, CanvasText 20%, transparent);
      min-width: min(640px, 100%);
      background: color-mix(in srgb, CanvasText 16%, transparent);
    }
    .flow-list span {
      padding: 8px 10px;
      background: Canvas;
    }
    #trace {
      margin-top: 18px;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 12px;
      color: color-mix(in srgb, CanvasText 65%, transparent);
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
      let autoComponentEventRetryLimit = 30;
      let autoComponentEventDelayMs = 140;
      let autoComponentEventRetryMs = 80;
      let autoComponentEventTimeoutMs = 500;
      let autoComponentEventEnabled = !!window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__;
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
        }
      ];
      let autoComponentEventIndex = 0;
      let autoComponentEventAttempts = 0;
      let autoComponentEventInFlight = false;
      let autoComponentEventsDone = false;
      let autoResyncDone = false;
      let autoErrorProbeDone = false;
      const flowBootstrapAppId = 'ROOT-2521314';
      const flowBootstrapClientId = flowBootstrapAppId.replace(/-\d+$/, '');
      let flowClientResources = [];
      let flowClientBootstrapInitResponse = null;
      let flowClientEngineResourceBaseVerified = false;
      let originalXMLHttpRequest = null;
      let flowClientResourceGraphMetadataVerified = true;
      const flowClientResourceRequests = new Set();
      const flowClientResourceResponsePaths = new Set();
      const flowClientResourceExpectedSha256ByPath = new Map();
      const pendingFlowClientXhrRequests = new Map();
      const queuedFlowClientEngineMessages = [];
      const queuedFlowClientEngineMessageOrder = [];
      const deliveredQueuedFlowClientEngineMessageOrder = [];
      const flowIpcServerSequences = [];
      let flowClientEngineResyncXhrSent = false;
      let lastFlowIpcServerSequence = 0;
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
        flowClientResourceExecutionError: '',
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
        flowClientEngineQueuedMessageCount: 0,
        flowClientEngineQueuedMessageHighWater: 0,
        flowClientEngineQueuedPushCount: 0,
        flowClientEngineQueuedMessageOrder: '',
        flowClientEngineDeliveredQueuedMessageCount: 0,
        flowClientEngineDeliveredQueuedMessageOrder: '',
        flowClientEngineResyncRequested: false,
        flowClientEngineResyncPatchConsumed: false,
        flowClientEngineErrorReported: false,
        flowClientEngineErrorCode: '',
        flowClientEngineVisibleOwner: false,
        flowClientEngineVisibleRootId: '',
        flowClientEngineVisibleStatusText: '',
        flowClientEngineVisibleButtonText: '',
        flowClientEngineLocalFallbackHidden: false,
        flowClientEngineXhrError: '',
        flowClientEngineInitError: '',
        flowClientEngineResourceExecutionError: ''
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

      function flowClientContainerTag() {
        return 'flow-container-' + flowBootstrapAppId.toLowerCase();
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
        const container = document.querySelector(flowClientContainerTag());
        if (!root || !container || !window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapDomBound) {
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
        window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleOwner = true;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineVisibleRootId = root.id;
        window.VaadinDesktopIpcDiagnostics.flowClientEngineLocalFallbackHidden = app.hidden;
        return true;
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

      function deliverFlowClientEngineMessage(message, kind) {
        const client = flowClientEngineClient();
        const body = message && message.payload && message.payload.uidl && typeof message.payload.uidl.body === 'string'
          ? message.payload.uidl.body
          : '';
        if (!client || typeof client.handleIpcMessage !== 'function' || !body
            || !window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapDomBound) {
          return false;
        }
        try {
          client.handleIpcMessage(body);
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
        const rawSequence = message.payload ? Number(message.payload.serverSequence) : NaN;
        const tag = kind + ':' + (Number.isFinite(rawSequence) && rawSequence > 0 ? rawSequence : 'unknown');
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
              const rpcNodeId = rpc && Number.isInteger(rpc.node) ? rpc.node : 16;
              const rpcEventType = rpc && typeof rpc.event === 'string' ? rpc.event : (type === 'event' ? 'uidl-rpc' : 'resync');
              const rpcComponentId = flowClientElementIdByNodeId(rpcNodeId) || (rpcNodeId === 16 ? 'ipc-button' : '');
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
                ipcPayload.reason = 'flow-client-xhr-resync';
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
            if (pending.eventType === 'click') {
              autoClickInFlight = false;
            } else if (pending.eventType === 'change') {
              window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventPatchCount += 1;
              const currentOrder = window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventOrder;
              window.VaadinDesktopIpcDiagnostics.flowClientEngineComponentEventOrder =
                currentOrder ? currentOrder + ',' + pending.componentId : pending.componentId;
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
          setTimeout(scheduleAutoComponentEvents, 900);
        } else {
          autoClickInFlight = false;
          autoComponentEventInFlight = false;
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
            autoClickInFlight = false;
            refreshFlowClientEngineDiagnostics();
            scheduleAutoClick();
          }, 1200);
          return;
        }
        autoClickReadinessAttempts += 1;
        if (autoClickReadinessAttempts >= 30 && button) {
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

      function scheduleAutoComponentEvents() {
        if (!autoComponentEventEnabled || autoComponentEventsDone) {
          return;
        }
        if (!window.VaadinDesktopIpcDiagnostics.flowClientEngineEventPatchConsumed
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
        try {
          descriptor.prepare(target.element);
          client.sendEventMessage(target.nodeId, descriptor.eventType, descriptor.data());
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineXhrError = error && error.message ? error.message : String(error);
          autoComponentEventInFlight = false;
          return;
        }

        setTimeout(() => {
          autoComponentEventInFlight = false;
          refreshFlowClientEngineDiagnostics();
          const textDone = window.VaadinDesktopIpcDiagnostics.flowClientEngineTextInputEventObserved;
          const checkboxDone = window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved;
          if ((descriptor.key === 'text' && textDone) || (descriptor.key === 'checkbox' && checkboxDone)) {
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

      function requestFlowClientResourceProbe(payload) {
        flowClientResources = payload && payload.flowClientResources && Array.isArray(payload.flowClientResources.resources)
          ? payload.flowClientResources.resources
          : [];
        window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount = flowClientResources.length;
        requestFlowClientResource('META-INF/frontend/FlowBootstrap.js', 'flow-bootstrap-module', 'flow-client-resource-probe');
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
        send('resourceRequest', {
          purpose,
          path: resource.path,
          expectedSha256: resource.sha256 || '',
          securityKey
        });
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
          ensureFlowClientContainer();
          module.init(flowClientBootstrapInitResponse);
          const flow = window.Vaadin && window.Vaadin.Flow ? window.Vaadin.Flow : null;
          const appIds = flow && typeof flow.getAppIds === 'function' ? flow.getAppIds() : [];
          window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized = appIds.includes(flowBootstrapAppId);
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
            'ipc-push-status'
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
        }
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = '';
        } else if (clientKeys.length > 0) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = 'FlowClient.init has not registered sendEventMessage client yet';
        }
        if (window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered
            && window.VaadinDesktopIpcDiagnostics.flowClientEngineBootstrapDomBound) {
          promoteFlowClientEngineToVisible();
        }
        refreshFlowClientEngineVisibleDiagnostics();
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
            && window.VaadinDesktopIpcDiagnostics.flowClientEngineCheckboxEventObserved) {
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
          scheduleAutoClick();
        } catch (error) {
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitialized = false;
          window.VaadinDesktopIpcDiagnostics.flowClientEngineClientRegistered = false;
          window.VaadinDesktopIpcDiagnostics.flowClientEngineInitError = error && error.message ? error.message : String(error);
        }
      }

      async function handleResourceResponse(payload) {
        if (!payload || payload.flowClientResource !== true) {
          return;
        }
        const body = typeof payload.body === 'string' ? payload.body : '';
        const resourcePath = payload.path || '';
        const expectedSha256 = flowClientResourceExpectedSha256ByPath.get(resourcePath) || '';
        const metadataMatches = !expectedSha256 || expectedSha256 === (payload.sha256 || '');
        recordFlowClientResourceGraphResponse(resourcePath, body, metadataMatches);
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
      }

      function handleAdditionalFlowClientResourceResponse(payload) {
        trace.textContent = 'flow resource graph '
          + window.VaadinDesktopIpcDiagnostics.flowClientResourceGraphResponseCount
          + '/'
          + window.VaadinDesktopIpcDiagnostics.flowClientResourceManifestCount;
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
            window.VaadinDesktopIpcDiagnostics.flowClientResourceExecuted = true;
            window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified =
              window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified
              && window.VaadinDesktopIpcDiagnostics.flowClientBootstrapExported
              && window.VaadinDesktopIpcDiagnostics.flowClientBootstrapInitialized;
            trace.textContent = 'flow resource module ' + (window.VaadinDesktopIpcDiagnostics.flowClientResourceVerified ? 'verified' : 'loaded');
          } finally {
            URL.revokeObjectURL(blobUrl);
          }
        } catch (error) {
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
            initializeFlowClientEngineModule(module);
            requestRemainingFlowClientResourceGraph();
            trace.textContent = 'flow client engine ' + (window.VaadinDesktopIpcDiagnostics.flowClientEngineResourceVerified ? 'verified' : 'loaded');
          } finally {
            URL.revokeObjectURL(blobUrl);
          }
        } catch (error) {
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
              } else if (!flowClientOwnsMessage && message.payload && message.payload.uidl && message.payload.uidl.resync === true) {
                if (!applyUidlBootstrap(message.payload)) {
                  render(message.payload && message.payload.tree);
                }
              } else if (!flowClientOwnsMessage && !applyUidlPatch(message.payload)) {
                render(message.payload && message.payload.tree);
              }
              if (message.type === 'patch' && message.payload && message.payload.reason === 'event') {
                scheduleAutoComponentEvents();
              }
              if (message.type === 'patch' && message.payload && message.payload.reason === 'event'
                  && window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC__ && !autoResyncDone) {
                requestAutoResyncIfReady();
              }
              if (message.type === 'bootstrapResponse') {
                scheduleAutoClick();
              }
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
