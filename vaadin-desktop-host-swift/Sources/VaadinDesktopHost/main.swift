import Cocoa
import WebKit

struct HostConfig {
    var module: String = "examples/counter-app"
    var repoRoot: String = FileManager.default.currentDirectoryPath
    var mavenRepo: String = ""
    var mavenExecutable: String = "mvn"
    var transport: String = "compatibility-http"
    var flowIpcAutoClick: Bool = false
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
        print("[java] \(line)")
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
            "compatibilityMode": config.transport != "flow-ipc",
            "transport": config.transport
        ])
        trace("send", "ready", [
            "host": "swift-appkit-wkwebview",
            "protocol": config.transport == "flow-ipc" ? "flow-ipc-v1" : "host-protocol-v0",
            "compatibilityMode": config.transport != "flow-ipc"
        ])

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
            "envelope": object
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
        let result = executeDesktopCommand(command, args: args)
        let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
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
        deliverFlowIpcEnvelope(resultEnvelope, type: "capabilityResult")
    }

    private func executeDesktopCommand(_ command: String, args: [String: Any]) -> [String: Any] {
        switch command {
        case "window.setTitle":
            guard let title = args["title"] as? String else {
                return ["success": false, "command": command, "error": "Missing title"]
            }
            window?.title = title
            return ["success": true, "command": command, "title": title, "mode": "native-appkit"]
        case "clipboard.writeText":
            guard let text = args["text"] as? String else {
                return ["success": false, "command": command, "error": "Missing text"]
            }
            let pasteboard = NSPasteboard.general
            pasteboard.clearContents()
            let written = pasteboard.setString(text, forType: .string)
            return ["success": written, "command": command, "mode": "native-nspasteboard"]
        case "notification.show":
            return [
                "success": true,
                "command": command,
                "mode": "trace-only",
                "title": args["title"] as? String ?? "",
                "body": args["body"] as? String ?? ""
            ]
        default:
            return ["success": false, "command": command, "error": "Unsupported desktop command"]
        }
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
    #app {
      box-sizing: border-box;
      min-height: 100vh;
      padding: 28px;
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
  <div id="trace"></div>
  <script>
    (function () {
      const app = document.getElementById('app');
      const trace = document.getElementById('trace');
      let sequence = 0;
      let autoClickDone = false;
      const flowClientState = {
        csrfToken: '',
        syncId: -1,
        clientId: 0
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
      }

      function updateClientState(payload) {
        if (payload && payload.clientState) {
          if (typeof payload.clientState.csrfToken === 'string') {
            flowClientState.csrfToken = payload.clientState.csrfToken;
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
          } catch (_) {
            // The bridge still treats UIDL as an opaque payload until the Flow client engine consumes it.
          }
        }
      }

      function sendFlowEvent(node, eventType) {
        const clientId = flowClientState.clientId++;
        const uidlRpc = {
          csrfToken: flowClientState.csrfToken,
          rpc: [{
            type: 'event',
            node: node.nodeId,
            event: eventType,
            data: {}
          }],
          syncId: flowClientState.syncId,
          clientId
        };
        send('event', {
          dispatch: 'flow-client-rpc',
          componentId: node.id,
          nodeId: node.nodeId,
          event: eventType,
          uidlRpc
        });
      }

      function render(node) {
        if (!node) {
          app.textContent = 'No Flow IPC tree received.';
          return;
        }
        app.replaceChildren(renderNode(node));
      }

      function renderNode(node) {
        let element;
        if (node.kind === 'button') {
          element = document.createElement('button');
          element.textContent = node.text || node.id;
          element.addEventListener('click', () => {
            sendFlowEvent(node, 'click');
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
        if (node.kind !== 'button') {
          for (const child of node.children || []) {
            element.appendChild(renderNode(child));
          }
        }
        return element;
      }

      window.VaadinDesktopIpc = {
        receive(message) {
          trace.textContent = 'recv ' + message.type + ' #' + message.id;
          if (message.type === 'bootstrapResponse' || message.type === 'patch') {
            updateClientState(message.payload);
            render(message.payload && message.payload.tree);
            if (message.type === 'bootstrapResponse' && window.__VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK__ && !autoClickDone) {
              autoClickDone = true;
              setTimeout(() => {
                const button = document.querySelector('[data-flow-id="ipc-button"]');
                if (button) {
                  button.click();
                }
              }, 80);
            }
          } else if (message.type === 'error') {
            app.textContent = 'Flow IPC error: ' + JSON.stringify(message.error || message.payload);
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
