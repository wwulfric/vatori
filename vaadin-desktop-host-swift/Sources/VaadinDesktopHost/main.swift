import Cocoa
import WebKit

struct HostConfig {
    var module: String = "examples/counter-app"
    var repoRoot: String = FileManager.default.currentDirectoryPath
    var mavenRepo: String = ""
    var mavenExecutable: String = "mvn"
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
        return config
    }
}

func nowMillis() -> Int64 {
    Int64(Date().timeIntervalSince1970 * 1000)
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
    private var process: Process?
    private var stdoutPipe: Pipe?
    private var stderrPipe: Pipe?

    init(config: HostConfig, onApplicationURL: @escaping (URL) -> Void) {
        self.config = config
        self.onApplicationURL = onApplicationURL
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
        environment["VAADIN_DESKTOP_HOST"] = "headless"
        environment["VAADIN_DESKTOP_NATIVE_BRIDGE"] = "host-protocol"
        environment["VAADIN_DESKTOP_HOST_PROTOCOL"] = "stdout-v0"
        process.environment = environment

        let stdoutPipe = Pipe()
        let stderrPipe = Pipe()
        process.standardOutput = stdoutPipe
        process.standardError = stderrPipe
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
            "nativeBridge": "host-protocol"
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
        stdoutPipe?.fileHandleForReading.readabilityHandler = nil
        stderrPipe?.fileHandleForReading.readabilityHandler = nil
        if let process, process.isRunning {
            trace("send", "shutdown.sidecar", [:])
            process.terminate()
        }
    }

    private func handleStdout(_ line: String) {
        print("[java] \(line)")
        fflush(stdout)

        if line.hasPrefix("VDP1 ") {
            handleHostProtocolEnvelope(String(line.dropFirst(5)))
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

final class WebViewHost: NSObject, NSApplicationDelegate, NSWindowDelegate, WKNavigationDelegate {
    private let config: HostConfig
    private var sidecar: JavaSidecar?
    private var window: NSWindow?
    private var webView: WKWebView?
    private var didOpenWindow = false

    init(config: HostConfig) {
        self.config = config
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        trace("recv", "launch", [
            "host": "swift-appkit-wkwebview",
            "module": config.module,
            "compatibilityMode": true,
            "transport": "localhost-http"
        ])
        trace("send", "ready", [
            "host": "swift-appkit-wkwebview",
            "protocol": "host-protocol-v0",
            "compatibilityMode": true
        ])

        let sidecar = JavaSidecar(config: config) { [weak self] url in
            self?.openWindow(url: url)
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
          bodyText: document.body ? document.body.innerText.slice(0, 500) : "",
          hasVaadinRoot: !!document.querySelector("vaadin-app-layout, vaadin-vertical-layout, flow-container-root"),
          elementCount: document.body ? document.body.querySelectorAll("*").length : 0
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

let config = HostConfig.parse()
let application = NSApplication.shared
let delegate = WebViewHost(config: config)
application.delegate = delegate
application.setActivationPolicy(.regular)
application.run()
