import Cocoa
import WebKit

final class WebViewHost: NSObject, NSApplicationDelegate, NSWindowDelegate, WKNavigationDelegate, WKScriptMessageHandler {
    private enum TerminationState {
        case running
        case draining
        case stoppingSidecar
        case shutdownFailed
        case ready
    }

    let config: HostConfig
    private let capabilityManifest: CapabilityManifest
    private let replacementRecords = RuntimeReplacementRecords()
    private let resourceCleanup: NativeResourceCleanupTracker
    private var terminationState = TerminationState.running
    private var failureExitRequested = false
    private var applicationTerminationReplyPending = false
    let commandExecutor: DesktopCommandExecutor
    private let flowWebAppSchemeHandler: FlowIpcWebAppSchemeHandler
    private var sidecarController: FlowIpcSidecarController?
    private var hostControlChannel: HostControlChannel?
    private var windowsById: [String: FlowIpcWindowContext] = [:]
    private var retiredWindowScopes = FlowIpcRetiredScopeRegistry()
    private var closingWindowIds: Set<String> = []
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

    var isTerminationRequested: Bool {
        if case .running = terminationState { return false }
        return true
    }

    init(config: HostConfig) {
        self.config = config
        self.capabilityManifest = config.capabilityManifest
        let resourceCleanup = NativeResourceCleanupTracker()
        self.resourceCleanup = resourceCleanup
        self.commandExecutor = DesktopCommandExecutor(
            config: config,
            capabilityManifest: config.capabilityManifest,
            resourceCleanup: resourceCleanup)
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
            replacementRecords: replacementRecords,
            onProductAssetsReady: { [weak self] selected in
                if let root = selected.webAppURL { self?.flowWebAppSchemeHandler.installPackagedSource(root) }
            },
            flowIpcWindows: { [weak self] in
                guard let self else {
                    return []
                }
                return Array(self.windowsById.values)
            },
            onSidecarEnvelope: { [weak self] raw, object, generation in
                self?.handleSidecarEnvelope(
                    raw,
                    object,
                    sidecarGeneration: generation)
            },
            onLifecycleReady: { [weak self] in
                self?.replayClosedMainScopeIfNeeded()
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

    func applicationShouldTerminate(
        _ sender: NSApplication
    ) -> NSApplication.TerminateReply {
        switch terminationState {
        case .ready:
            return .terminateNow
        case .draining, .stoppingSidecar:
            applicationTerminationReplyPending = true
            return .terminateLater
        case .shutdownFailed:
            applicationTerminationReplyPending = true
            stopSidecarForApplicationTermination(sender)
            return .terminateLater
        case .running:
            applicationTerminationReplyPending = true
        }
        beginApplicationTermination(sender)
        return .terminateLater
    }

    private func beginApplicationTermination(_ sender: NSApplication) {
        terminationState = .draining
        replacementRecords.shutdown()
        hostControlChannel?.stop()
        // Fence incoming work before invalidation can settle its callbacks.
        sidecarController?.prepareForHostShutdown()
        commandExecutor.invalidateNativeOperations(reason: "host-shutdown")
        let timeout: TimeInterval = 2
        trace("recv", "host.nativeResourceDrainStarted", [
            "pendingCount": resourceCleanup.pendingCount,
            "timeoutSeconds": timeout
        ])
        resourceCleanup.drain(timeout: timeout) { [weak self] result in
            guard let self, case .draining = self.terminationState else {
                return
            }
            trace("recv", "host.nativeResourceDrainCompleted", [
                "drained": result.drained,
                "pendingCount": result.pendingCount,
                "failureCount": result.failureCount
            ])
            self.stopSidecarForApplicationTermination(sender)
        }
    }

    private func stopSidecarForApplicationTermination(_ sender: NSApplication) {
        terminationState = .stoppingSidecar
        let completion: (Result<Void, JavaSidecarShutdownFailure>) -> Void = { [weak self] result in
            guard let self, case .stoppingSidecar = self.terminationState else { return }
            switch result {
            case .success:
                trace("recv", "host.sidecarShutdownCompleted", ["succeeded": true])
                self.terminationState = .ready
                if self.failureExitRequested {
                    // Preserve the operational failure status, but only after
                    // the owned child has actually exited. This is not an
                    // AppKit applicationWillTerminate success notification.
                    trace("recv", "host.failureShutdownCompleted", ["exitStatus": 1])
                    fflush(stdout)
                    fflush(stderr)
                    exit(1)
                }
                self.applicationTerminationReplyPending = false
                sender.reply(toApplicationShouldTerminate: true)
            case .failure(let failure):
                trace("recv", "host.sidecarShutdownFailed", [
                    "cause": failure.cause,
                    "phase": failure.phase
                ])
                // The runtime is already fenced and drained. Do not restore
                // Flow work; a later Quit retries only the retained child.
                self.terminationState = .shutdownFailed
                if self.applicationTerminationReplyPending {
                    self.applicationTerminationReplyPending = false
                    sender.reply(toApplicationShouldTerminate: false)
                }
            }
        }
        if let sidecarController {
            sidecarController.stopForHostShutdown(completion: completion)
        } else {
            NativeMainRunLoop.perform { completion(.success(())) }
        }
    }

    func applicationWillTerminate(_ notification: Notification) {
        config.runtimeObserver.applicationWillTerminate(host: self)
        hostControlChannel?.stop()
        commandExecutor.invalidateNativeOperations(reason: "host-shutdown")
        // Normal termination has already observed the child's actual exit.
        // Starting asynchronous shutdown here would let AppKit exit too early.
    }

    private func handleHostControl(_ command: HostControlCommand) {
        guard case .running = terminationState else {
            return
        }
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
        guard !retiredWindowScopes.contains(scope.identity) else {
            trace("send", "error", [
                "code": "flowIpcWindowScopeRetired",
                "windowId": scope.windowId,
                "sessionId": scope.sessionId,
                "uiId": scope.uiId
            ])
            return
        }
        guard windowsById[scope.windowId] == nil else {
            return
        }
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
            terminateAfterFailure()
            return
        }
        guard let bridgeSource = FlowIpcOpaqueHttpBridge.load(from: config.opaqueBridgeURL) else {
            trace("send", "error", ["code": "opaqueHttpBridgeMissing"])
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
                resourceCleanup: resourceCleanup,
                windowId: scope.windowId)
        }
        executor.window = window
        let reloadURL = flowWebAppSchemeHandler.entryURL(
            for: descriptor.route)
        let context = FlowIpcWindowContext(
            scope: scope,
            window: window,
            webView: webView,
            commandExecutor: executor,
            reloadURL: reloadURL)
        windowsById[scope.windowId] = context
        windowIdByWebView[ObjectIdentifier(webView)] = scope.windowId

        webView.load(URLRequest(url: reloadURL))
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

        if #available(macOS 14.0, *) {
            NSApplication.shared.activate()
        } else {
            NSRunningApplication.current.activate(options: [
                .activateAllWindows,
                .activateIgnoringOtherApps
            ])
        }
        window.makeKeyAndOrderFront(nil)
        trace("recv", "window.activation", [
            "label": label,
            "mode": "activate",
            "keyWindow": window.isKeyWindow,
            "activeApplication": NSApplication.shared.isActive
        ])
    }

    private func handleSidecarEnvelope(
        _ raw: String,
        _ object: [String: Any],
        sidecarGeneration: Int
    ) {
        guard case .running = terminationState else {
            return
        }
        let type = object["type"] as? String ?? "unknown"
        trace("recv", type, [
            "source": "java-sidecar",
            "transport": config.transport.rawValue,
            "sidecarGeneration": sidecarGeneration,
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
                terminateAfterFailure()
                return
            }
            if sidecarController?.isProductReplacementInProgress == true,
               let previous = sidecarReady,
               previous.application.mainWindow.id != ready.application.mainWindow.id {
                _ = sidecarController?.handleReadyFailure(cause: "candidate-main-window-mismatch")
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
                guard case .running = terminationState else { return }
                config.runtimeObserver.didOpenInitialFlowIpcWindow(host: self)
            } else {
                openCompatibilityWindow(ready: ready)
            }
            return
        }
        if type == "command", config.transport == .compatibilityHttp {
            guard canExecuteSidecarCommand(
                object,
                sidecarGeneration: sidecarGeneration) else {
                return
            }
            guard let mainScope else {
                trace("send", "error", ["code": "sidecarReadyMissing"])
                return
            }
            handleSidecarCommand(
                object,
                executor: commandExecutor,
                scope: mainScope,
                sidecarGeneration: sidecarGeneration,
                ownerContext: nil)
            return
        }
        guard config.transport == .flowIpc else {
            trace("send", "error", [
                "code": "unsupportedCompatibilityMessage",
                "type": type
            ])
            return
        }
        switch retiredWindowScopes.disposition(for: object, type: type) {
        case .rejectCommand(let retiredIdentity):
            guard canExecuteSidecarCommand(
                object,
                sidecarGeneration: sidecarGeneration
            ) else {
                return
            }
            let payload = object["payload"] as? [String: Any] ?? [:]
            let command = payload["command"] as? String ?? "unknown"
            let correlationId = object["correlationId"] as? String
                ?? object["id"] as? String
                ?? ""
            let retiredScope = FlowIpcWindowScope(
                windowId: retiredIdentity.windowId,
                sessionId: retiredIdentity.sessionId,
                uiId: retiredIdentity.uiId)
            trace("recv", "closedWindowCapabilityCommandRejected", [
                "command": command,
                "correlationId": correlationId,
                "windowId": retiredIdentity.windowId,
                "sessionId": retiredIdentity.sessionId,
                "uiId": retiredIdentity.uiId
            ])
            sendCapabilityResult(
                flowIpcOwnerWindowClosedCapabilityResult(command),
                command: command,
                correlationId: correlationId,
                object: object,
                scope: retiredScope,
                sidecarGeneration: sidecarGeneration,
                ownerContext: nil)
            return
        case .dropTransportFrame(let retiredIdentity):
            trace("recv", "flowIpcClosedWindowEnvelopeDropped", [
                "type": type,
                "windowId": retiredIdentity.windowId,
                "sessionId": retiredIdentity.sessionId,
                "uiId": retiredIdentity.uiId
            ])
            return
        case .notRetired:
            break
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
            guard canExecuteSidecarCommand(
                object,
                sidecarGeneration: sidecarGeneration) else {
                return
            }
            handleSidecarCommand(
                object,
                executor: context.commandExecutor,
                scope: context.scope,
                sidecarGeneration: sidecarGeneration,
                ownerContext: context)
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

    private func canExecuteSidecarCommand(
        _ object: [String: Any],
        sidecarGeneration: Int
    ) -> Bool {
        guard sidecarController?.canExecuteSidecarCommand(
            expectedGeneration: sidecarGeneration) == true else {
            trace("recv", "staleCapabilityCommandDropped", [
                "command": (object["payload"] as? [String: Any])?["command"]
                    as? String ?? "unknown",
                "correlationId": object["correlationId"] as? String ?? "",
                "expectedGeneration": sidecarGeneration,
                "reason": "sidecar-generation-unavailable",
                "windowId": object["windowId"] as? String ?? "",
                "sessionId": object["sessionId"] as? String ?? "",
                "uiId": object["uiId"] as? String ?? ""
            ])
            return false
        }
        return true
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

    private func rememberClosedWindow(_ scope: FlowIpcWindowScope) {
        retiredWindowScopes.retire(scope.identity)
    }

    private func handleSidecarCommand(
        _ object: [String: Any],
        executor: DesktopCommandExecutor,
        scope: FlowIpcWindowScope,
        sidecarGeneration: Int,
        ownerContext: FlowIpcWindowContext?
    ) {
        let payload = object["payload"] as? [String: Any] ?? [:]
        let command = payload["command"] as? String ?? "unknown"
        let args = payload["args"] as? [String: Any] ?? [:]
        let correlationId = (object["correlationId"] as? String) ?? (object["id"] as? String) ?? ""
        if command == RuntimeReplacementRequestCommand.name {
            let result = RuntimeReplacementRequestCommand.execute(
                args: args, hasOwner: ownerContext != nil, windowId: scope.windowId,
                manifest: capabilityManifest) { requestId, candidateId in
                    guard let ownerContext, let sidecarController = self.sidecarController else {
                        return ["success": false, "error": "Runtime unavailable"]
                    }
                    return sidecarController.requestProductReplacement(
                        owner: ownerContext.lifetimeId, requestId: requestId,
                        candidateId: candidateId, expectedGeneration: sidecarGeneration)
                }
            sendCapabilityResult(result, command: command, correlationId: correlationId,
                object: object, scope: scope, sidecarGeneration: sidecarGeneration, ownerContext: ownerContext)
            return
        }
        if command == RuntimeReplacementStatusCommand.name {
            let result = RuntimeReplacementStatusCommand.execute(
                args: args, owner: ownerContext?.lifetimeId, windowId: scope.windowId,
                manifest: capabilityManifest, records: replacementRecords)
            sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope,
                sidecarGeneration: sidecarGeneration, ownerContext: ownerContext)
            return
        }
        if command == "window.open" {
            let result = openWindowCommand(args)
            sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope,
                sidecarGeneration: sidecarGeneration,
                ownerContext: ownerContext)
            return
        }
        if command == "window.close" {
            let (result, target) = closeWindowCommand(args)
            sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope,
                sidecarGeneration: sidecarGeneration,
                ownerContext: ownerContext)
            if result["success"] as? Bool == true, let target {
                DispatchQueue.main.async {
                    target.window.close()
                }
            }
            return
        }
        executor.execute(
            command,
            args: args,
            ownerScope: scope,
            sidecarGeneration: sidecarGeneration
        ) { result in
            self.sendCapabilityResult(
                result, command: command, correlationId: correlationId,
                object: object, scope: scope,
                sidecarGeneration: sidecarGeneration,
                ownerContext: ownerContext)
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
              windowsById[descriptor.id] == nil,
              !closingWindowIds.contains(descriptor.id) else {
            return windowCommandFailure("window.open", "Window id is already open")
        }
        guard capabilityManifest.allows("window.open", windowId: descriptor.id) else {
            return windowCommandFailure(
                "window.open", "Command not allowed by desktop-capabilities.json",
                reason: capabilityManifest.denialReason(
                    "window.open", windowId: descriptor.id))
        }
        openFlowIpcWindow(
            scope: .window(descriptor.id),
            descriptor: descriptor)
        guard case .running = terminationState,
              windowsById[descriptor.id] != nil else {
            return windowCommandFailure("window.open", "Host runtime is shutting down")
        }
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
              let target = windowsById[windowId],
              !closingWindowIds.contains(windowId) else {
            return (windowCommandFailure(
                "window.close", "Target window is not open"), nil)
        }
        guard capabilityManifest.allows("window.close", windowId: windowId) else {
            return (windowCommandFailure(
                "window.close", "Command not allowed by desktop-capabilities.json",
                reason: capabilityManifest.denialReason(
                    "window.close", windowId: windowId)), nil)
        }
        closingWindowIds.insert(windowId)
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
        scope: FlowIpcWindowScope,
        sidecarGeneration: Int,
        ownerContext: FlowIpcWindowContext?
    ) {
        if let ownerContext,
           windowsById[scope.windowId] !== ownerContext {
            trace("recv", "staleCapabilityResultDropped", [
                "command": command,
                "correlationId": correlationId,
                "expectedGeneration": sidecarGeneration,
                "reason": "owner-window-retired",
                "windowId": scope.windowId,
                "sessionId": scope.sessionId,
                "uiId": scope.uiId
            ])
            return
        }
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
        guard let data = try? JSONSerialization.data(withJSONObject: resultEnvelope),
              let json = String(data: data, encoding: .utf8) else {
            trace("send", "error", [
                "code": "flowIpcSerializeFailed",
                "type": "capabilityResult"
            ])
            return
        }
        guard sidecarController?.sendSidecarEnvelope(
            json,
            expectedGeneration: sidecarGeneration) == true else {
            trace("recv", "staleCapabilityResultDropped", [
                "command": command,
                "correlationId": correlationId,
                "expectedGeneration": sidecarGeneration,
                "reason": "sidecar-generation-unavailable",
                "windowId": scope.windowId,
                "sessionId": scope.sessionId,
                "uiId": scope.uiId
            ])
            return
        }
        trace("send", "capabilityResult", [
            "target": "java-sidecar",
            "transport": config.transport.rawValue,
            "command": command,
            "sidecarGeneration": sidecarGeneration,
            "envelope": redactedFlowIpcEnvelope(resultEnvelope)
        ])
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
        guard case .running = terminationState else {
            return
        }
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
        guard sidecarController?.sendSidecarEnvelope(json) == true else {
            trace("recv", "windowClosedDropped", [
                "reason": "sidecar-lifecycle-unavailable",
                "windowId": scope.windowId,
                "sessionId": scope.sessionId,
                "uiId": scope.uiId
            ])
            return
        }
        trace("send", "windowClosed", [
            "target": "java-sidecar",
            "transport": config.transport.rawValue,
            "windowId": scope.windowId,
            "sessionId": scope.sessionId,
            "uiId": scope.uiId
        ])
    }

    private func replayClosedMainScopeIfNeeded() {
        guard config.transport == .flowIpc,
              let mainScope,
              windowsById[mainScope.windowId] == nil,
              !windowsById.isEmpty else {
            return
        }
        trace("recv", "windowClosedReplay", [
            "reason": "sidecar-lifecycle-ready",
            "windowId": mainScope.windowId,
            "sessionId": mainScope.sessionId,
            "uiId": mainScope.uiId
        ])
        sendWindowClosed(mainScope)
    }

    private func terminateAfterFailure() {
        guard case .running = terminationState else { return }
        failureExitRequested = true
        trace("recv", "host.failureShutdownStarted", [
            "processIdentifier": sidecarController?.processIdentifier ?? 0,
            "hostProcessIdentifier": ProcessInfo.processInfo.processIdentifier
        ])
        // Fatal paths have the same process-ownership obligation as Quit.
        // If retirement fails, retain the fenced host/child for an explicit
        // later Quit retry; never turn a signal request into exit confirmation.
        beginApplicationTermination(NSApp)
    }

    func windowWillClose(_ notification: Notification) {
        guard let closingWindow = notification.object as? NSWindow,
              let context = windowsById.values.first(where: { $0.window === closingWindow }) else {
            if compatibilityWindow === notification.object as? NSWindow,
               let mainScope {
                trace("recv", "window.closeRequested", mainScope.tracePayload)
                commandExecutor.invalidateNativeOperations(
                    reason: "owner-window-closed")
                sendWindowClosed(mainScope)
                NSApp.terminate(nil)
            } else if let closingWindow = notification.object as? NSWindow {
                trace("recv", "window.closeDropped", [
                    "reason": "unknown-or-retired-window",
                    "windowNumber": closingWindow.windowNumber
                ])
            }
            return
        }
        trace("recv", "window.closeRequested", [
            "label": context.scope.windowId,
            "windowId": context.scope.windowId,
            "sessionId": context.scope.sessionId,
            "uiId": context.scope.uiId
        ])
        rememberClosedWindow(context.scope)
        replacementRecords.forgetOwner(context.lifetimeId)
        context.commandExecutor.invalidateNativeOperations(
            reason: "owner-window-closed")
        sendWindowClosed(context.scope)
        windowsById.removeValue(forKey: context.scope.windowId)
        closingWindowIds.remove(context.scope.windowId)
        windowIdByWebView.removeValue(forKey: ObjectIdentifier(context.webView))
        closingWindow.delegate = nil
        if windowsById.isEmpty {
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
} catch HostConfigError.invalidCapabilityManifest(let manifest) {
    trace("recv", "capabilityManifest", manifest.tracePayload())
    trace("send", "error", [
        "code": "capabilityManifestInvalid",
        "path": manifest.path,
        "loaded": manifest.loaded,
        "parseError": manifest.parseError
    ])
    let error = HostConfigError.invalidCapabilityManifest(manifest)
    FileHandle.standardError.write(Data("\(error.localizedDescription)\n".utf8))
    exit(2)
} catch {
    FileHandle.standardError.write(Data("\(error.localizedDescription)\n".utf8))
    exit(2)
}
let application = NSApplication.shared
let delegate = WebViewHost(config: config)
application.delegate = delegate
application.setActivationPolicy(.regular)
NativeApplicationMenu.install(on: application)
application.run()
