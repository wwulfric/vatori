import Cocoa
import WebKit

final class WebViewHost: NSObject, NSApplicationDelegate, NSWindowDelegate, WKNavigationDelegate, WKScriptMessageHandler {
    let config: HostConfig
    private let capabilityManifest: CapabilityManifest
    let commandExecutor: DesktopCommandExecutor
    private let flowWebAppSchemeHandler: FlowIpcWebAppSchemeHandler
    private var sidecarController: FlowIpcSidecarController?
    private var hostControlChannel: HostControlChannel?
    private var windowsById: [String: FlowIpcWindowContext] = [:]
    private var closedWindowScopes: [String: FlowIpcWindowScope] = [:]
    private var windowIdByWebView: [ObjectIdentifier: String] = [:]
    private var didOpenCompatibilityWindow = false
    private var compatibilityWindow: NSWindow?
    private var compatibilityWebView: WKWebView?
    private var sidecarReady: SidecarReady?
    private var mainScope: FlowIpcWindowScope?

    var mainWindowContext: FlowIpcWindowContext? {
        guard let mainScope else {
            return nil
        }
        return windowsById[mainScope.windowId]
    }

    func flowIpcWindowContext(_ windowId: String) -> FlowIpcWindowContext? {
        windowsById[windowId]
    }

    var sidecarProcessIdentifier: Int32? {
        sidecarController?.processIdentifier
    }

    init(config: HostConfig) {
        let capabilityManifest = CapabilityManifest.load(from: config.capabilityManifestURL)
        self.config = config
        self.capabilityManifest = capabilityManifest
        self.commandExecutor = DesktopCommandExecutor(config: config,
                                                      capabilityManifest: capabilityManifest)
        self.flowWebAppSchemeHandler = FlowIpcWebAppSchemeHandler(
            source: config.flowWebAppSource,
            pushBridgeURL: config.pushBridgeURL)
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        trace("recv", "launch", [
            "host": "swift-appkit-wkwebview",
            "transport": config.transport.rawValue,
            "sidecarRestartPolicy": config.flowIpcSidecarRecoveryEnabled
                ? "consecutive-failure-budget"
                : "fail-stop",
            "sidecarRestartLimit": config.flowIpcSidecarRecoveryEnabled
                ? config.flowIpcSidecarRestartLimit
                : 0,
            "sidecarRecoveryStableRunSeconds":
                config.flowIpcSidecarRecoveryStableRunSeconds
        ])
        trace("send", "ready", [
            "host": "swift-appkit-wkwebview",
            "protocolVersion": 1,
            "transport": config.transport.rawValue
        ])
        if config.transport == .flowIpc {
            trace("recv", "capabilityManifest", capabilityManifest.tracePayload())
        }

        let sidecarController = FlowIpcSidecarController(
            config: config,
            standardBundleEntryURL: flowWebAppSchemeHandler.entryURL,
            flowIpcWindows: { [weak self] in
                guard let self else {
                    return []
                }
                return Array(self.windowsById.values)
            },
            onSidecarEnvelope: { [weak self] raw, object in
                self?.handleSidecarEnvelope(raw, object)
            },
            onReplacementResult: {
                [weak self] requestId, status, cause in
                self?.hostControlChannel?.sendReplacementResult(
                    requestId: requestId,
                    status: status,
                    cause: cause)
            },
            terminateApplication: { [weak self] in
                self?.terminateAfterFailure()
            })
        self.sidecarController = sidecarController
        if !sidecarController.start() {
            terminateAfterFailure()
            return
        }
        if config.flowWebAppSource?.isDevelopment == true {
            let channel = HostControlChannel { [weak self] command in
                self?.handleHostControl(command)
            }
            hostControlChannel = channel
            channel.start()
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        hostControlChannel?.stop()
        sidecarController?.terminate()
    }

    private func handleHostControl(_ command: HostControlCommand) {
        switch command {
        case .replaceSidecar(let requestId, let reason):
            trace("recv", "hostControl.received", [
                "type": "replaceSidecar",
                "requestId": requestId,
                "reason": reason
            ])
            _ = sidecarController?.requestReplacement(
                requestId: requestId,
                reason: reason)
        case .rollbackSidecar(let requestId, let reason):
            trace("recv", "hostControl.received", [
                "type": "rollbackSidecar",
                "requestId": requestId,
                "reason": reason
            ])
            _ = sidecarController?.requestRollback(
                requestId: requestId,
                reason: reason)
        }
    }

    private func openCompatibilityWindow(ready: SidecarReady) {
        guard !didOpenCompatibilityWindow else {
            return
        }
        guard let url = ready.applicationURL else {
            return
        }
        didOpenCompatibilityWindow = true
        let descriptor = ready.application.mainWindow

        trace("send", "window.create", [
            "label": descriptor.id,
            "url": url.absoluteString,
            "mode": "compatibility-http"
        ])

        let contentRect = NSRect(
            x: 0, y: 0, width: descriptor.width, height: descriptor.height)
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

        window.title = descriptor.title
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
        trace("recv", "window.ready", ["label": descriptor.id])
    }

    func openFlowIpcWindow(
        scope: FlowIpcWindowScope,
        descriptor requestedDescriptor: SidecarWindowDescriptor? = nil
    ) {
        guard windowsById[scope.windowId] == nil else {
            return
        }
        closedWindowScopes.removeValue(forKey: scope.windowId)
        guard let mainDescriptor = sidecarReady?.application.mainWindow else {
            trace("send", "error", ["code": "sidecarReadyMissing"])
            return
        }
        let descriptor = requestedDescriptor ?? SidecarWindowDescriptor(
            id: scope.windowId,
            title: scope.windowId == mainDescriptor.id
                ? mainDescriptor.title
                : "\(mainDescriptor.title) - \(scope.windowId)",
            width: mainDescriptor.width,
            height: mainDescriptor.height)

        trace("send", "window.create", [
            "label": scope.windowId,
            "windowId": scope.windowId,
            "sessionId": scope.sessionId,
            "uiId": scope.uiId,
            "mode": "flow-ipc"
        ])

        let contentRect = NSRect(
            x: 0, y: 0, width: descriptor.width, height: descriptor.height)
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
        config.runtimeObserver.configureUserContentController(
            userContentController,
            config: config)
        userContentController.add(self, name: "flowIpc")

        let configuration = WKWebViewConfiguration()
        configuration.userContentController = userContentController
        configuration.websiteDataStore = .nonPersistent()
        guard flowWebAppSchemeHandler.isAvailable else {
            trace("send", "error", [
                "code": "flowWebAppMissing",
                "root": flowWebAppSchemeHandler.redactedRoot
            ])
            sidecarController?.terminate()
            terminateAfterFailure()
            return
        }
        guard let bridgeSource = FlowIpcOpaqueHttpBridge.load(from: config.opaqueBridgeURL) else {
            trace("send", "error", ["code": "opaqueHttpBridgeMissing"])
            sidecarController?.terminate()
            terminateAfterFailure()
            return
        }
        userContentController.addUserScript(WKUserScript(
            source: "window.__vatoriFlowWebAppMode = '\(flowWebAppSchemeHandler.sourceMode)';",
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true))
        userContentController.addUserScript(WKUserScript(
            source: bridgeSource,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true))
        configuration.setURLSchemeHandler(
            flowWebAppSchemeHandler,
            forURLScheme: FlowIpcWebAppSchemeHandler.schemeName)
        trace("recv", "flowWebAppReady", [
            "mode": flowWebAppSchemeHandler.sourceMode,
            "root": flowWebAppSchemeHandler.redactedRoot,
            "entry": flowWebAppSchemeHandler.entryURL.absoluteString,
            "requestTransport": "opaque-http-stream-ipc"
        ])
        if flowWebAppSchemeHandler.sourceMode == "production-bundle" {
            trace("recv", "standardBundleReady", [
                "root": flowWebAppSchemeHandler.redactedRoot,
                "entry": flowWebAppSchemeHandler.entryURL.absoluteString,
                "requestTransport": "opaque-http-stream-ipc"
            ])
        }
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        let webView = WKWebView(frame: contentRect, configuration: configuration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.width, .height]

        window.title = descriptor.title
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

        webView.load(URLRequest(url: flowWebAppSchemeHandler.entryURL(for: descriptor.route)))
        trace("recv", "window.ready", [
            "label": scope.windowId,
            "windowId": scope.windowId,
            "sessionId": scope.sessionId,
            "uiId": scope.uiId,
            "transport": "flow-ipc"
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

    private func handleSidecarEnvelope(_ raw: String, _ object: [String: Any]) {
        let type = object["type"] as? String ?? "unknown"
        trace("recv", type, [
            "source": "java-sidecar",
            "transport": config.transport.rawValue,
            "envelope": redactedFlowIpcEnvelope(object)
        ])
        if type == "ready" {
            let ready: SidecarReady
            do {
                ready = try SidecarReady.parse(object, expectedTransport: config.transport)
            } catch {
                if sidecarController?.handleReadyFailure(
                    cause: "invalid-candidate-ready") == true {
                    return
                }
                trace("send", "error", [
                    "code": "invalidSidecarReady",
                    "message": error.localizedDescription
                ])
                sidecarController?.terminate()
                terminateAfterFailure()
                return
            }
            guard let scope = FlowIpcWindowScope.sidecarScope(
                object,
                fallbackWindowId: ready.application.mainWindow.id) else {
                if sidecarController?.handleReadyFailure(
                    cause: "invalid-candidate-ready-scope") == true {
                    return
                }
                trace("send", "error", ["code": "invalidSidecarReadyScope"])
                sidecarController?.terminate()
                terminateAfterFailure()
                return
            }
            sidecarReady = ready
            mainScope = scope
            flowWebAppSchemeHandler.setEnabledFeatureFlags(ready.enabledFeatureFlags)
            if sidecarController?.handleReady() == true {
                return
            }
            if ready.transport == .flowIpc {
                openFlowIpcWindow(
                    scope: scope,
                    descriptor: ready.application.mainWindow)
                config.runtimeObserver.didOpenInitialFlowIpcWindow(host: self)
            } else {
                openCompatibilityWindow(ready: ready)
            }
            return
        }
        if type == "command", config.transport == .compatibilityHttp {
            guard let mainScope else {
                trace("send", "error", ["code": "sidecarReadyMissing"])
                return
            }
            handleSidecarCommand(
                object,
                executor: commandExecutor,
                scope: mainScope)
            return
        }
        guard config.transport == .flowIpc else {
            trace("send", "error", [
                "code": "unsupportedCompatibilityMessage",
                "type": type
            ])
            return
        }
        guard let context = flowIpcContext(for: object) else {
            if isLateEnvelopeForClosedWindow(object, type: type) {
                trace("recv", "flowIpcClosedWindowEnvelopeDropped", [
                    "type": type,
                    "windowId": object["windowId"] as? String ?? ""
                ])
                return
            }
            trace("send", "error", [
                "code": "flowIpcWindowScopeUnknown",
                "type": type,
                "windowId": object["windowId"] as? String ?? ""
            ])
            return
        }
        if type == "command" {
            handleSidecarCommand(
                object,
                executor: context.commandExecutor,
                scope: context.scope)
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
            ?? mainScope?.windowId
            ?? "main"
        guard let context = windowsById[windowId], context.scope.matches(object) else {
            return nil
        }
        return context
    }

    private func isLateEnvelopeForClosedWindow(
        _ object: [String: Any],
        type: String
    ) -> Bool {
        let isTransportResponse = type == "opaqueHttpResponse"
            || type.hasPrefix("opaqueHttpStream")
            || type == "opaquePush"
            || type == "opaquePushConnected"
        guard isTransportResponse,
              let windowId = object["windowId"] as? String,
              let closedScope = closedWindowScopes[windowId] else {
            return false
        }
        return closedScope.matches(object)
    }

    private func rememberClosedWindow(_ scope: FlowIpcWindowScope) {
        closedWindowScopes[scope.windowId] = scope
        DispatchQueue.main.asyncAfter(deadline: .now() + 30) { [weak self] in
            guard self?.closedWindowScopes[scope.windowId] == scope else {
                return
            }
            self?.closedWindowScopes.removeValue(forKey: scope.windowId)
        }
    }

    private func handleSidecarCommand(
        _ object: [String: Any],
        executor: DesktopCommandExecutor,
        scope: FlowIpcWindowScope
    ) {
        let payload = object["payload"] as? [String: Any] ?? [:]
        let command = payload["command"] as? String ?? "unknown"
        let args = payload["args"] as? [String: Any] ?? [:]
        let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
        if command == "window.open" {
            let result = openWindowCommand(args)
            sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope)
            return
        }
        if command == "window.close" {
            let (result, target) = closeWindowCommand(args)
            sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope)
            if result["success"] as? Bool == true, let target {
                DispatchQueue.main.async {
                    target.window.close()
                }
            }
            return
        }
        executor.execute(command, args: args) { result in
            self.sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope)
        }
    }

    private func openWindowCommand(_ args: [String: Any]) -> [String: Any] {
        guard config.transport == .flowIpc else {
            return windowCommandFailure(
                "window.open", "Multiple windows require the flow-ipc transport")
        }
        let descriptor: SidecarWindowDescriptor
        do {
            descriptor = try SidecarWindowDescriptor.parse(args)
        } catch {
            return windowCommandFailure("window.open", error.localizedDescription)
        }
        guard descriptor.id != sidecarReady?.application.mainWindow.id,
              windowsById[descriptor.id] == nil else {
            return windowCommandFailure("window.open", "Window id is already open")
        }
        guard capabilityManifest.allows("window.open", windowId: descriptor.id) else {
            return windowCommandFailure(
                "window.open", "Command not allowed by desktop-capabilities.json",
                reason: capabilityManifest.denialReason(
                    "window.open", windowId: descriptor.id))
        }
        openFlowIpcWindow(scope: .window(descriptor.id), descriptor: descriptor)
        return [
            "success": true,
            "command": "window.open",
            "windowId": descriptor.id,
            "route": descriptor.route,
            "permissionDecision": "allowed",
            "permissionReason": capabilityManifest.allowedReason(
                "window.open", windowId: descriptor.id)
        ]
    }

    private func closeWindowCommand(
        _ args: [String: Any]
    ) -> ([String: Any], FlowIpcWindowContext?) {
        guard config.transport == .flowIpc else {
            return (windowCommandFailure(
                "window.close", "Multiple windows require the flow-ipc transport"), nil)
        }
        guard let windowId = args["id"] as? String,
              !windowId.isBlank,
              let target = windowsById[windowId] else {
            return (windowCommandFailure(
                "window.close", "Target window is not open"), nil)
        }
        guard capabilityManifest.allows("window.close", windowId: windowId) else {
            return (windowCommandFailure(
                "window.close", "Command not allowed by desktop-capabilities.json",
                reason: capabilityManifest.denialReason(
                    "window.close", windowId: windowId)), nil)
        }
        return ([
            "success": true,
            "command": "window.close",
            "windowId": windowId,
            "permissionDecision": "allowed",
            "permissionReason": capabilityManifest.allowedReason(
                "window.close", windowId: windowId)
        ], target)
    }

    private func windowCommandFailure(
        _ command: String,
        _ message: String,
        reason: String = "invalid-window-command"
    ) -> [String: Any] {
        [
            "success": false,
            "command": command,
            "error": message,
            "permissionDecision": "denied",
            "permissionReason": reason
        ]
    }

    private func sendCapabilityResult(
        _ result: [String: Any],
        command: String,
        correlationId: String,
        object: [String: Any],
        scope: FlowIpcWindowScope
    ) {
        let resultEnvelope: [String: Any] = [
            "version": 1,
            "type": "capabilityResult",
            "id": "capability-\(correlationId)",
            "correlationId": correlationId,
            "sessionId": scope.sessionId,
            "uiId": scope.uiId,
            "windowId": scope.windowId,
            "route": object["route"] as? String ?? "",
            "payload": result,
            "error": NSNull(),
            "timestamp": nowMillis()
        ]
        trace("send", "capabilityResult", [
            "target": "java-sidecar",
            "transport": config.transport.rawValue,
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
        sidecarController?.sendSidecarEnvelope(json)
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
        guard hasValidSidecarVersion(object) else {
            trace("send", "error", [
                "code": "unsupportedWebViewSidecarVersion",
                "version": object["version"] ?? NSNull()
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
        sidecarController?.sendSidecarEnvelope(json)
    }

    private func sendWindowClosed(_ scope: FlowIpcWindowScope) {
        let id = "window-closed-\(scope.windowId)-\(nowMillis())"
        let object = scope.applying(to: [
            "version": 1,
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
        sidecarController?.sendSidecarEnvelope(json)
    }

    private func terminateAfterFailure() {
        sidecarController?.terminate()
        fflush(stdout)
        fflush(stderr)
        exit(1)
    }

    func windowWillClose(_ notification: Notification) {
        guard let closingWindow = notification.object as? NSWindow,
              let context = windowsById.values.first(where: { $0.window === closingWindow }) else {
            if compatibilityWindow === notification.object as? NSWindow,
               let mainScope {
                trace("recv", "window.closeRequested", mainScope.tracePayload)
                sendWindowClosed(mainScope)
            }
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
        rememberClosedWindow(context.scope)
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
                config.runtimeObserver.didFinishCompatibilityNavigation(
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
        config.runtimeObserver.didFinishFlowIpcNavigation(host: self, context: context)
    }

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        if config.transport == .flowIpc,
           navigationAction.targetFrame?.isMainFrame == true,
           let requestURL = navigationAction.request.url,
           isStandardBundleVirtualRequestURL(requestURL) {
            trace("recv", "standardBundleVirtualNavigationBlocked", [
                "scheme": requestURL.scheme ?? "",
                "host": requestURL.host ?? "",
                "path": requestURL.path,
                "navigationType": "\(navigationAction.navigationType.rawValue)"
            ])
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    private func isStandardBundleVirtualRequestURL(_ url: URL) -> Bool {
        guard let virtualBaseURL = URL(string: FlowIpcWebAppSchemeHandler.virtualRequestBaseURL) else {
            return false
        }
        return url.scheme == virtualBaseURL.scheme && url.host == virtualBaseURL.host
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


let config: HostConfig
do {
    config = try HostConfig.load()
} catch {
    FileHandle.standardError.write(Data("\(error.localizedDescription)\n".utf8))
    exit(2)
}
let application = NSApplication.shared
let delegate = WebViewHost(config: config)
application.delegate = delegate
application.setActivationPolicy(.regular)
application.run()
