import Cocoa
import WebKit

final class WebViewHost: NSObject, NSApplicationDelegate, NSWindowDelegate, WKNavigationDelegate, WKScriptMessageHandler {
    let config: HostConfig
    private let capabilityManifest: CapabilityManifest
    let commandExecutor: DesktopCommandExecutor
    private let standardBundleSchemeHandler: FlowIpcStandardBundleSchemeHandler
    private var sidecarController: FlowIpcSidecarController?
    private var windowsById: [String: FlowIpcWindowContext] = [:]
    private var windowIdByWebView: [ObjectIdentifier: String] = [:]
    private var didOpenCompatibilityWindow = false
    private var compatibilityWindow: NSWindow?
    private var compatibilityWebView: WKWebView?

    var mainWindowContext: FlowIpcWindowContext? {
        windowsById[FlowIpcWindowScope.main.windowId]
    }

    func flowIpcWindowContext(_ windowId: String) -> FlowIpcWindowContext? {
        windowsById[windowId]
    }

    var sidecarProcessIdentifier: Int32? {
        sidecarController?.processIdentifier
    }

    @discardableResult
    func requestSidecarRecovery(reason: String) -> Bool {
        sidecarController?.requestRecovery(reason: reason) == true
    }

    init(config: HostConfig) {
        let capabilityManifest = CapabilityManifest.load(module: config.module)
        self.config = config
        self.capabilityManifest = capabilityManifest
        self.commandExecutor = DesktopCommandExecutor(config: config,
                                                      capabilityManifest: capabilityManifest)
        self.standardBundleSchemeHandler = FlowIpcStandardBundleSchemeHandler(module: config.module)
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        trace("recv", "launch", [
            "host": "swift-appkit-wkwebview",
            "module": config.module,
            "compatibilityMode": config.transport != "flow-ipc",
            "transport": config.transport,
            "standardBundleMode": config.flowIpcStandardBundleEnabled,
            "sidecarRestartPolicy": config.flowIpcSidecarRecoveryEnabled
                ? "limited-restart"
                : "fail-stop",
            "sidecarRestartLimit": config.flowIpcSidecarRecoveryEnabled
                ? config.flowIpcSidecarRestartLimit
                : 0
        ])
        trace("send", "ready", [
            "host": "swift-appkit-wkwebview",
            "protocol": config.transport == "flow-ipc" ? "flow-ipc-v1" : "host-protocol-v0",
            "compatibilityMode": config.transport != "flow-ipc"
        ])
        if config.transport == "flow-ipc" {
            trace("recv", "capabilityManifest", capabilityManifest.tracePayload())
        }

        let sidecarController = FlowIpcSidecarController(
            config: config,
            standardBundleEntryURL: standardBundleSchemeHandler.entryURL,
            flowIpcWindows: { [weak self] in
                guard let self else {
                    return []
                }
                return Array(self.windowsById.values)
            },
            onApplicationURL: { [weak self] url in
                self?.openWindow(url: url)
            },
            onFlowIpcEnvelope: { [weak self] raw, object in
                self?.handleFlowIpcEnvelope(raw, object)
            },
            terminateApplication: {
                NSApp.terminate(nil)
            })
        self.sidecarController = sidecarController
        if !sidecarController.start() {
            NSApp.terminate(nil)
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        sidecarController?.terminate()
    }

    private func openWindow(url: URL) {
        guard !didOpenCompatibilityWindow else {
            return
        }
        didOpenCompatibilityWindow = true

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

        commandExecutor.window = window
        compatibilityWindow = window
        compatibilityWebView = webView

        webView.load(URLRequest(url: url))
        trace("recv", "window.ready", ["label": "main"])
    }

    func openFlowIpcWindow(scope: FlowIpcWindowScope) {
        guard windowsById[scope.windowId] == nil else {
            return
        }

        trace("send", "window.create", [
            "label": scope.windowId,
            "windowId": scope.windowId,
            "sessionId": scope.sessionId,
            "uiId": scope.uiId,
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
        userContentController.addUserScript(WKUserScript(
            source: scope.documentStartScript,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true))
        config.runtimeHooks.configureUserContentController(
            userContentController,
            config: config)
        userContentController.add(self, name: "flowIpc")

        let configuration = WKWebViewConfiguration()
        configuration.userContentController = userContentController
        configuration.websiteDataStore = .nonPersistent()
        guard config.flowIpcStandardBundleEnabled,
              standardBundleSchemeHandler.isAvailable else {
            trace("send", "error", [
                "code": "standardBundleMissing",
                "root": standardBundleSchemeHandler.redactedRoot
            ])
            sidecarController?.terminate()
            NSApp.terminate(nil)
            return
        }
        guard let bridgeSource = FlowIpcOpaqueHttpBridge.load(repoRoot: config.repoRoot) else {
            trace("send", "error", ["code": "opaqueHttpBridgeMissing"])
            sidecarController?.terminate()
            NSApp.terminate(nil)
            return
        }
        userContentController.addUserScript(WKUserScript(
            source: bridgeSource,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true))
        configuration.setURLSchemeHandler(
            standardBundleSchemeHandler,
            forURLScheme: FlowIpcStandardBundleSchemeHandler.schemeName)
        trace("recv", "standardBundleReady", [
            "root": standardBundleSchemeHandler.redactedRoot,
            "entry": standardBundleSchemeHandler.entryURL.absoluteString,
            "requestTransport": "opaque-http-stream-ipc"
        ])
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        let webView = WKWebView(frame: contentRect, configuration: configuration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.width, .height]

        window.title = scope.windowId == "main"
            ? config.title
            : "\(config.title) - \(scope.windowId)"
        window.contentView = webView
        window.delegate = self
        window.isReleasedWhenClosed = false
        window.sharingType = .readOnly
        window.level = .normal
        window.collectionBehavior = [.fullScreenPrimary]
        window.center()
        if scope.windowId != "main" {
            window.setFrameOrigin(NSPoint(
                x: window.frame.origin.x + 36,
                y: window.frame.origin.y - 36))
        }
        showWindow(window, label: scope.windowId)

        let executor: DesktopCommandExecutor
        if scope.windowId == "main" {
            executor = commandExecutor
        } else {
            executor = DesktopCommandExecutor(
                config: config,
                capabilityManifest: capabilityManifest,
                windowId: scope.windowId)
        }
        executor.window = window
        let context = FlowIpcWindowContext(
            scope: scope,
            window: window,
            webView: webView,
            commandExecutor: executor)
        windowsById[scope.windowId] = context
        windowIdByWebView[ObjectIdentifier(webView)] = scope.windowId

        webView.load(URLRequest(url: standardBundleSchemeHandler.entryURL))
        trace("recv", "window.ready", [
            "label": scope.windowId,
            "windowId": scope.windowId,
            "sessionId": scope.sessionId,
            "uiId": scope.uiId,
            "transport": "flow-ipc",
            "standardBundleMode": config.flowIpcStandardBundleEnabled
        ])
    }

    private func showWindow(_ window: NSWindow, label: String = "main") {
        if config.hostNoActivate {
            window.orderFront(nil)
            trace("recv", "window.activation", [
                "label": label,
                "mode": "no-activate",
                "keyWindow": window.isKeyWindow,
                "activeApplication": NSApplication.shared.isActive
            ])
            return
        }

        window.makeKeyAndOrderFront(nil)
        NSRunningApplication.current.activate(options: [.activateAllWindows])
        trace("recv", "window.activation", [
            "label": label,
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
            if sidecarController?.handleReady() == true {
                return
            }
            openFlowIpcWindow(scope: .main)
            config.runtimeHooks.didOpenInitialFlowIpcWindow(host: self)
            return
        }
        guard let context = flowIpcContext(for: object) else {
            trace("send", "error", [
                "code": "flowIpcWindowScopeUnknown",
                "type": type,
                "windowId": object["windowId"] as? String ?? ""
            ])
            return
        }
        if type == "command" {
            handleFlowIpcCommand(object, context: context)
            return
        }
        if type == "opaqueHttpResponse" || type.hasPrefix("opaqueHttpStream") {
            deliverOpaqueHttpResponse(raw, to: context)
            return
        }
        if type == "opaquePush" || type == "opaquePushConnected" {
            deliverOpaquePush(raw, type: type, to: context)
            return
        }
        deliverFlowIpcEnvelope(raw, type: type, to: context)
    }

    private func flowIpcContext(for object: [String: Any]) -> FlowIpcWindowContext? {
        let windowId = object["windowId"] as? String
            ?? object["uiId"] as? String
            ?? FlowIpcWindowScope.main.windowId
        guard let context = windowsById[windowId], context.scope.matches(object) else {
            return nil
        }
        return context
    }

    private func handleFlowIpcCommand(
        _ object: [String: Any],
        context: FlowIpcWindowContext
    ) {
        let payload = object["payload"] as? [String: Any] ?? [:]
        let command = payload["command"] as? String ?? "unknown"
        let args = payload["args"] as? [String: Any] ?? [:]
        let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
        context.commandExecutor.execute(command, args: args) { result in
            let resultEnvelope: [String: Any] = [
                "version": "flow-ipc-v1",
                "type": "capabilityResult",
                "id": "capability-\(correlationId)",
                "correlationId": correlationId,
                "sessionId": context.scope.sessionId,
                "uiId": context.scope.uiId,
                "windowId": context.scope.windowId,
                "route": object["route"] as? String ?? "",
                "payload": result,
                "error": NSNull(),
                "timestamp": nowMillis()
            ]
            trace("send", "capabilityResult", [
                "target": "java-sidecar",
                "transport": "flow-ipc",
                "command": command,
                "envelope": resultEnvelope
            ])
            guard let data = try? JSONSerialization.data(withJSONObject: resultEnvelope),
                  let json = String(data: data, encoding: .utf8) else {
                trace("send", "error", [
                    "code": "flowIpcSerializeFailed",
                    "type": "capabilityResult"
                ])
                return
            }
            self.sidecarController?.sendFlowIpc(json)
        }
    }

    func deliverFlowIpcEnvelope(
        _ object: [String: Any],
        type: String,
        to context: FlowIpcWindowContext? = nil
    ) {
        guard let data = try? JSONSerialization.data(withJSONObject: object),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "error", [
                "code": "flowIpcSerializeFailed",
                "type": type
            ])
            return
        }
        let target = context ?? flowIpcContext(for: object)
        guard let target else {
            trace("send", "error", [
                "code": "flowIpcWindowScopeUnknown",
                "type": type
            ])
            return
        }
        deliverFlowIpcEnvelope(json, type: type, to: target)
    }

    private func deliverFlowIpcEnvelope(
        _ raw: String,
        type: String,
        to context: FlowIpcWindowContext
    ) {
        let script = "window.VaadinDesktopIpc && window.VaadinDesktopIpc.receive(\(raw));"
        context.webView.evaluateJavaScript(script) { _, error in
            if let error {
                trace("send", "error", [
                    "code": "flowIpcDeliverFailed",
                    "message": error.localizedDescription,
                    "type": type,
                    "windowId": context.scope.windowId
                ])
            }
        }
    }

    private func deliverOpaqueHttpResponse(
        _ raw: String,
        to context: FlowIpcWindowContext
    ) {
        let script = "window.VaadinDesktopOpaqueHttp && window.VaadinDesktopOpaqueHttp.receive(\(raw));"
        context.webView.evaluateJavaScript(script) { _, error in
            if let error {
                trace("send", "error", [
                    "code": "opaqueHttpResponseDeliveryFailed",
                    "message": error.localizedDescription,
                    "windowId": context.scope.windowId
                ])
            }
        }
    }

    private func deliverOpaquePush(
        _ raw: String,
        type: String,
        to context: FlowIpcWindowContext
    ) {
        let script = "window.VaadinDesktopPush && window.VaadinDesktopPush.receive(\(raw));"
        context.webView.evaluateJavaScript(script) { _, error in
            if let error {
                trace("send", "error", [
                    "code": "opaquePushDeliveryFailed",
                    "message": error.localizedDescription,
                    "type": type,
                    "windowId": context.scope.windowId
                ])
            }
        }
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "flowIpc" else {
            return
        }
        guard let object = message.body as? [String: Any] else {
            trace("send", "error", [
                "code": "invalidWebViewFlowIpcMessage",
                "body": String(describing: message.body)
            ])
            return
        }
        guard let sourceWebView = message.webView,
              let windowId = windowIdByWebView[ObjectIdentifier(sourceWebView)],
              let context = windowsById[windowId] else {
            trace("send", "error", [
                "code": "unknownWebViewFlowIpcSource"
            ])
            return
        }
        let scopedObject = context.scope.applying(to: object)
        let messageType = scopedObject["type"] as? String ?? "unknown"
        if messageType == "webview.firstMeaningfulPaint" {
            var payload = scopedObject["payload"] as? [String: Any] ?? [:]
            payload.merge(context.scope.tracePayload) { current, _ in current }
            payload["recoveryGeneration"] = context.recoveryGeneration
            trace("recv", messageType, payload)
            return
        }
        trace("send", messageType, [
            "target": "java-sidecar",
            "transport": "flow-ipc",
            "windowId": context.scope.windowId,
            "envelope": redactedFlowIpcEnvelope(scopedObject)
        ])
        guard let data = try? JSONSerialization.data(withJSONObject: scopedObject),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "error", [
                "code": "flowIpcSerializeFailed",
                "type": messageType
            ])
            return
        }
        sidecarController?.sendFlowIpc(json)
    }

    private func sendWindowClosed(_ scope: FlowIpcWindowScope) {
        let id = "window-closed-\(scope.windowId)-\(nowMillis())"
        let object = scope.applying(to: [
            "version": "flow-ipc-v1",
            "type": "windowClosed",
            "id": id,
            "correlationId": id,
            "route": "",
            "payload": ["windowId": scope.windowId],
            "error": NSNull(),
            "timestamp": nowMillis()
        ])
        guard let data = try? JSONSerialization.data(withJSONObject: object),
              let json = String(data: data, encoding: .utf8) else {
            return
        }
        sidecarController?.sendFlowIpc(json)
    }

    func windowWillClose(_ notification: Notification) {
        guard let closingWindow = notification.object as? NSWindow,
              let context = windowsById.values.first(where: { $0.window === closingWindow }) else {
            sidecarController?.terminate()
            NSApp.terminate(nil)
            return
        }
        trace("recv", "window.closeRequested", [
            "label": context.scope.windowId,
            "windowId": context.scope.windowId,
            "sessionId": context.scope.sessionId,
            "uiId": context.scope.uiId
        ])
        sendWindowClosed(context.scope)
        windowsById.removeValue(forKey: context.scope.windowId)
        windowIdByWebView.removeValue(forKey: ObjectIdentifier(context.webView))
        if windowsById.isEmpty {
            sidecarController?.terminate()
            NSApp.terminate(nil)
        }
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        guard let windowId = windowIdByWebView[ObjectIdentifier(webView)] else {
            if compatibilityWebView === webView {
                compatibilityWindow?.sharingType = .readOnly
                trace("recv", "webview.loaded", [
                    "label": "main",
                    "title": webView.title ?? "",
                    "windowSharingType": compatibilityWindow?.sharingType.rawValue ?? -1
                ])
                config.runtimeHooks.didFinishCompatibilityNavigation(
                    host: self,
                    webView: webView)
            }
            return
        }
        guard let context = windowsById[windowId] else {
            return
        }
        context.navigationFinished = true
        context.window.sharingType = .readOnly
        trace("recv", "webview.loaded", [
            "label": context.scope.windowId,
            "windowId": context.scope.windowId,
            "sessionId": context.scope.sessionId,
            "uiId": context.scope.uiId,
            "recoveryGeneration": context.recoveryGeneration,
            "title": webView.title ?? "",
            "windowSharingType": context.window.sharingType.rawValue
        ])
        config.runtimeHooks.didFinishFlowIpcNavigation(host: self, context: context)
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
if config.module.isEmpty {
    FileHandle.standardError.write(Data(
        "Vaadin Desktop Host requires --module <application-module>\n".utf8))
    exit(2)
}
let application = NSApplication.shared
let delegate = WebViewHost(config: config)
application.delegate = delegate
application.setActivationPolicy(.regular)
application.run()
