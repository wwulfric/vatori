import Cocoa
import Foundation
import WebKit

final class FlowIpcSidecarController {
    private let config: HostConfig
    private let standardBundleEntryURL: URL
    private let flowIpcWindows: () -> [FlowIpcWindowContext]
    private let terminateApplication: () -> Void
    private var sidecar: JavaSidecar!
    private var recoveryInProgress = false
    private var restartAttempts = 0
    private var exitedProcessIdentifier: Int32?

    init(
        config: HostConfig,
        standardBundleEntryURL: URL,
        flowIpcWindows: @escaping () -> [FlowIpcWindowContext],
        onApplicationURL: @escaping (URL) -> Void,
        onFlowIpcEnvelope: @escaping (String, [String: Any]) -> Void,
        terminateApplication: @escaping () -> Void
    ) {
        self.config = config
        self.standardBundleEntryURL = standardBundleEntryURL
        self.flowIpcWindows = flowIpcWindows
        self.terminateApplication = terminateApplication
        self.sidecar = JavaSidecar(
            config: config,
            onApplicationURL: onApplicationURL,
            onFlowIpcEnvelope: onFlowIpcEnvelope,
            onTermination: { [weak self] termination in
                self?.handleTermination(termination)
            })
    }

    @discardableResult
    func start() -> Bool {
        sidecar.start()
    }

    func terminate() {
        sidecar.terminate()
    }

    @discardableResult
    func sendFlowIpc(_ envelope: String) -> Bool {
        sidecar.sendFlowIpc(envelope)
    }

    var processIdentifier: Int32? {
        sidecar.processIdentifier
    }

    @discardableResult
    func requestRecovery(reason: String) -> Bool {
        sidecar.terminateForRecovery(reason: reason)
    }

    func handleReady() -> Bool {
        guard recoveryInProgress else {
            return false
        }
        completeRecovery()
        return true
    }

    private func handleTermination(_ termination: JavaSidecarTermination) {
        guard !termination.expected else {
            return
        }
        let windows = flowIpcWindows()
        guard config.flowIpcSidecarRecoveryEnabled, !windows.isEmpty else {
            trace("send", "error", [
                "code": "sidecarUnexpectedExit",
                "generation": termination.generation,
                "processIdentifier": termination.processIdentifier,
                "status": termination.status,
                "recoverySupported": false
            ])
            terminateApplication()
            return
        }

        exitedProcessIdentifier = termination.processIdentifier
        if !recoveryInProgress {
            recoveryInProgress = true
            for context in windows {
                context.webView.evaluateJavaScript("""
                (() => {
                  window.VaadinDesktopOpaqueHttp?.sidecarDisconnected?.();
                  window.VaadinDesktopPush?.sidecarDisconnected?.();
                  return true;
                })()
                """)
            }
        }
        scheduleRecovery()
    }

    private func scheduleRecovery() {
        let windows = flowIpcWindows()
        guard recoveryInProgress, !windows.isEmpty else {
            return
        }
        guard restartAttempts < config.flowIpcSidecarRestartLimit else {
            trace("send", "error", [
                "code": "sidecarRecoveryExhausted",
                "attempts": restartAttempts,
                "restartLimit": config.flowIpcSidecarRestartLimit
            ])
            terminateApplication()
            return
        }

        restartAttempts += 1
        let attempt = restartAttempts
        let delay = min(2.0, 0.5 * pow(2.0, Double(attempt - 1)))
        trace("recv", "sidecar.recoveryScheduled", [
            "attempt": attempt,
            "delayMillis": Int(delay * 1000),
            "previousProcessIdentifier": exitedProcessIdentifier ?? 0,
            "windowCount": windows.count
        ])
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            guard let self,
                  self.recoveryInProgress,
                  !self.flowIpcWindows().isEmpty else {
                return
            }
            trace("send", "sidecar.recoveryStarted", [
                "attempt": attempt,
                "previousProcessIdentifier": self.exitedProcessIdentifier ?? 0
            ])
            guard self.sidecar.start() else {
                self.scheduleRecovery()
                return
            }
            self.scheduleReadyTimeout(attempt: attempt)
        }
    }

    private func scheduleReadyTimeout(attempt: Int) {
        let processIdentifier = sidecar.processIdentifier
        DispatchQueue.main.asyncAfter(
            deadline: .now() + config.flowIpcSidecarRecoveryReadyTimeoutSeconds
        ) { [weak self] in
            guard let self,
                  self.recoveryInProgress,
                  self.sidecar.processIdentifier == processIdentifier else {
                return
            }
            trace("send", "sidecar.recoveryReadyTimeout", [
                "attempt": attempt,
                "processIdentifier": processIdentifier ?? 0,
                "timeoutSeconds": self.config.flowIpcSidecarRecoveryReadyTimeoutSeconds
            ])
            self.sidecar.terminateForRecovery(reason: "ready-timeout")
        }
    }

    private func completeRecovery() {
        let previousProcessIdentifier = exitedProcessIdentifier ?? 0
        let currentProcessIdentifier = sidecar.processIdentifier ?? 0
        let windows = flowIpcWindows()
        var highestRecoveryGeneration = 0
        for context in windows {
            context.recoveryGeneration += 1
            context.navigationFinished = false
            highestRecoveryGeneration = max(highestRecoveryGeneration, context.recoveryGeneration)
            context.window.title = context.scope.windowId == "main"
                ? config.title
                : "\(config.title) - \(context.scope.windowId)"
            context.webView.configuration.userContentController.addUserScript(WKUserScript(
                source: "window.VaadinDesktopHostRecoveryGeneration = \(context.recoveryGeneration);",
                injectionTime: .atDocumentStart,
                forMainFrameOnly: true))
            var request = URLRequest(url: standardBundleEntryURL)
            request.cachePolicy = .reloadIgnoringLocalCacheData
            context.webView.load(request)
        }
        trace("recv", "sidecar.recoveryReady", [
            "attempt": restartAttempts,
            "previousProcessIdentifier": previousProcessIdentifier,
            "processIdentifier": currentProcessIdentifier,
            "processReplaced": previousProcessIdentifier > 0
                && currentProcessIdentifier > 0
                && previousProcessIdentifier != currentProcessIdentifier,
            "windowsReloaded": windows.count,
            "recoveryGeneration": highestRecoveryGeneration
        ])
        recoveryInProgress = false
        exitedProcessIdentifier = nil
    }
}
