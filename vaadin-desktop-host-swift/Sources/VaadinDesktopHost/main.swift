import Cocoa
import WebKit

final class WebViewHost: NSObject, NSApplicationDelegate, NSWindowDelegate, WKNavigationDelegate, WKScriptMessageHandler {
    private let config: HostConfig
    private let capabilityManifest: CapabilityManifest
    private let commandExecutor: DesktopCommandExecutor
    private let diagnostics: WebViewDiagnostics
    private let resourceSchemeHandler: FlowIpcResourceSchemeHandler
    private let resourceStager: FlowIpcResourceStager
    private var sidecar: JavaSidecar?
    private var window: NSWindow?
    private var webView: WKWebView?
    private var didOpenWindow = false

    init(config: HostConfig) {
        let resourceSchemeHandler = FlowIpcResourceSchemeHandler()
        let capabilityManifest = CapabilityManifest.load(module: config.module)
        self.config = config
        self.capabilityManifest = capabilityManifest
        self.commandExecutor = DesktopCommandExecutor(config: config,
                                                      capabilityManifest: capabilityManifest)
        self.diagnostics = WebViewDiagnostics(repoRoot: config.repoRoot)
        self.resourceSchemeHandler = resourceSchemeHandler
        self.resourceStager = FlowIpcResourceStager(repoRoot: config.repoRoot,
                                                    resourceSchemeHandler: resourceSchemeHandler)
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
        commandExecutor.window = window

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
        userContentController.addFlowIpcAutomationScripts(config: config)
        userContentController.add(self, name: "flowIpc")

        resourceStager.prepareStagingDirectory()
        let configuration = WKWebViewConfiguration()
        configuration.userContentController = userContentController
        configuration.setURLSchemeHandler(resourceSchemeHandler,
                                          forURLScheme: FlowIpcResourceSchemeHandler.schemeName)
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        trace("recv", "resourceLoad", compactResourceLoadTrace([
            "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
            "frontendResourceSchemeName": FlowIpcResourceSchemeHandler.schemeName,
            "frontendResourceLoaderMode": "custom-scheme-file-backed",
            "stagingMode": "custom-scheme",
            "frontendResourceSchemeHandlerInstalled": true,
            "frontendResourceLoaderNoLocalhost": true,
            "cleanupPolicy": FlowIpcResourceSchemeContract.cleanupPolicy,
            "fileDirectoryRedacted": "$REPO/.build/vaadin-desktop-host-swift/flow-ipc-resources"
        ]))
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
        commandExecutor.window = window

        webView.loadHTMLString(FlowIpcShell.load(repoRoot: config.repoRoot), baseURL: nil)
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
        if type == "resourceResponse" {
            deliverFlowIpcEnvelope(raw, type: type)
            handleFlowIpcDownloadSaveOpenResourceResponse(object)
            return
        }
        deliverFlowIpcEnvelope(raw, type: type)
    }

    private func handleFlowIpcDownloadSaveOpenResourceResponse(_ object: [String: Any]) {
        guard config.flowIpcAutoDownloadSaveOpen,
              let payload = object["payload"] as? [String: Any],
              payload["downloadHandlerVersion"] as? String == "flow-ipc-download-handler-save-open-v1_55",
              payload["downloadHandlerSaveOpenRequested"] as? Bool == true else {
            return
        }
        let base64 = payload["downloadHandlerBodyBase64"] as? String ?? ""
        let bodyData = Data(base64Encoded: base64) ?? Data()
        let body = String(data: bodyData, encoding: .utf8) ?? ""
        let suggestedName = payload["downloadHandlerFileName"] as? String ?? "flow-ipc-download-handler-v1_55.txt"
        let contentType = payload["downloadHandlerContentType"] as? String ?? "text/plain; charset=UTF-8"
        let targetPath = (payload["downloadHandlerSaveTargetPath"] as? String).flatMap { $0.isBlank ? nil : $0 }
            ?? ".build/vaadin-desktop-host-swift/downloads/flow-ipc-download-handler-v1_55.txt"
        let command = "fileTransfer.download"
        commandExecutor.execute(command, args: [
            "title": "Save Flow IPC DownloadHandler",
            "suggestedName": suggestedName,
            "mimeType": contentType,
            "bodyEncoding": "utf-8",
            "body": body,
            "byteCount": bodyData.count,
            "targetPath": targetPath
        ]) { result in
            var augmented = result
            let path = augmented["path"] as? String ?? ""
            augmented["downloadHandlerSaveOpenVersion"] = "flow-ipc-download-handler-save-open-v1_55"
            augmented["downloadHandlerSaveOpenSource"] = "flow-ipc-download-handler-resource-response"
            augmented["downloadHandlerResourcePath"] = payload["path"] as? String ?? ""
            augmented["downloadHandlerResourceSha256"] = payload["downloadHandlerSha256"] as? String ?? ""
            augmented["downloadHandlerResourceBytes"] = payload["downloadHandlerBytes"] as? Int ?? bodyData.count
            augmented["downloadHandlerResourceContentType"] = contentType
            augmented["downloadHandlerSavePathRedacted"] = self.commandExecutor.redactedTransferPath(path)
            augmented["downloadHandlerGrantType"] = "controlled-test-path"
            augmented["downloadHandlerSecurityScoped"] = false
            augmented["downloadHandlerNoLocalhost"] = true
            augmented["downloadHandlerFixtureSubstituted"] = false
            augmented["downloadHandlerBodyVerifiedByHost"] = body.contains("Flow IPC DownloadHandler v1.55")
            augmented["downloadHandlerOpenAfterSaveRequested"] = payload["downloadHandlerOpenAfterSaveRequested"] as? Bool ?? false
            augmented["downloadHandlerOpenAfterSaveResult"] = "classified-not-opened-in-auto-smoke"
            augmented["downloadHandlerFailureLayer"] = (augmented["success"] as? Bool) == true
                ? ""
                : "download-save-write-failed"
            let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
            let resultEnvelope: [String: Any] = [
                "version": "flow-ipc-v1",
                "type": "capabilityResult",
                "id": "download-save-open-\(correlationId)",
                "correlationId": correlationId,
                "sessionId": object["sessionId"] as? String ?? "flow-ipc-session",
                "uiId": object["uiId"] as? String ?? "main",
                "route": object["route"] as? String ?? "",
                "payload": augmented,
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

    private func handleFlowIpcCommand(_ object: [String: Any]) {
        let payload = object["payload"] as? [String: Any] ?? [:]
        let command = payload["command"] as? String ?? "unknown"
        let args = payload["args"] as? [String: Any] ?? [:]
        let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
        commandExecutor.execute(command, args: args) { result in
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

    private func sendHostResourceStageResponse(_ response: [String: Any]) {
        guard let data = try? JSONSerialization.data(withJSONObject: response),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "resourceLoad", compactResourceLoadTrace([
                "frontendResourceSchemeVersion": FlowIpcResourceSchemeContract.version,
                "frontendResourceLoaderMode": "custom-scheme-file-backed",
                "stagingMode": "custom-scheme",
                "frontendResourceLoaderNoLocalhost": true,
                "error": "host-resource-stage-response-serialize-failed"
            ]))
            return
        }
        webView?.evaluateJavaScript("window.VaadinDesktopIpcHostResourceStageResponse && window.VaadinDesktopIpcHostResourceStageResponse(\(json));")
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
            let response = resourceStager.handleStageRequest(object["payload"] as? [String: Any] ?? [:])
            sendHostResourceStageResponse(response)
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
            "envelope": redactedFlowIpcEnvelope(object)
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

        let verificationSchedule = FlowIpcVerificationSchedule(config: config)
        for delay in verificationSchedule.inspectionDelaysSeconds {
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                self.diagnostics.inspect(webView)
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + verificationSchedule.snapshotDelaySeconds) {
            self.diagnostics.writeSnapshot(webView)
        }
        if config.flowIpcAutoShutdown {
            scheduleAutoShutdown(webView, verificationSchedule: verificationSchedule)
        }
    }

    private func scheduleAutoShutdown(_ webView: WKWebView, verificationSchedule: FlowIpcVerificationSchedule) {
        if !verificationSchedule.extendedVerificationEnabled {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5.6) {
                trace("recv", "window.closeRequested", [
                    "label": "main",
                    "reason": "flow-ipc-auto-shutdown"
                ])
                self.window?.close()
            }
            return
        }

        let deadline = Date().addingTimeInterval(verificationSchedule.autoShutdownTimeoutSeconds)
        pollAutoShutdownReadiness(webView, deadline: deadline)
    }

    private func pollAutoShutdownReadiness(_ webView: WKWebView, deadline: Date) {
        let script = flowIpcAutoShutdownReadinessScript
        webView.evaluateJavaScript(script) { result, _ in
            let readiness = result as? String ?? "{}"
            if readiness.contains("\"complete\":true") || Date() >= deadline {
                self.diagnostics.inspect(webView)
                self.diagnostics.writeSnapshot(webView) {
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
