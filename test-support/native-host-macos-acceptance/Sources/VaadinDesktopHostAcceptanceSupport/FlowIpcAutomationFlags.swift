import Cocoa
import Foundation

final class FlowIpcAcceptanceRuntimeObserver: HostRuntimeObserver {
    var flags: Set<String> = []
    var values: [String: String] = [:]
    var repositoryRoot: String = FileManager.default.temporaryDirectory.path
    var hostNoActivate = false
    var webViewDiagnostics: WebViewDiagnostics?
    var reactorReloadAcceptanceScheduled = false
    var javaReloadAcceptanceScheduled = false
    var javaRollbackAcceptanceScheduled = false
    private var openPanelObservers: [NSObjectProtocol] = []

    func flag(_ name: String) -> Bool {
        flags.contains(name)
    }

    func configure(environment: [String: String]) {
        hostNoActivate = environment["VAADIN_DESKTOP_MACOS_NO_ACTIVATE"] == "1"
        if let configuredRoot = environment["VAADIN_DESKTOP_ACCEPTANCE_REPO_ROOT"],
           !configuredRoot.isEmpty {
            repositoryRoot = configuredRoot
        }
        setFlag(
            "frameworkUpdate",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE",
            environment: environment)
        setFlag(
            "pushRecovery",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_PUSH_RECOVERY",
            environment: environment)
        setFlag(
            "pushDelivery",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_PUSH_DELIVERY",
            environment: environment)
        setFlag(
            "largeDownloadResume",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LARGE_DOWNLOAD_RESUME",
            environment: environment)
        setFlag(
            "largeUploadRetry",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LARGE_UPLOAD_RETRY",
            environment: environment)
        setFlag(
            "inFlightRequestRecovery",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_IN_FLIGHT_REQUEST_RECOVERY",
            environment: environment)
        setFlag(
            "largeMultipartUploadRetry",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_LARGE_MULTIPART_UPLOAD_RETRY",
            environment: environment)
        setFlag(
            "concurrentUpload",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_CONCURRENT_UPLOAD",
            environment: environment)
        setFlag(
            "requestFanout",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_REQUEST_FANOUT",
            environment: environment)
        setFlag(
            "concurrentMultipartUpload",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_CONCURRENT_MULTIPART_UPLOAD",
            environment: environment)
        setFlag(
            "crossSessionMultipartUpload",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_CROSS_SESSION_MULTIPART_UPLOAD",
            environment: environment)
        setFlag(
            "lazyGridQuery",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_GRID_QUERY",
            environment: environment)
        setFlag(
            "flowDnd",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_DND",
            environment: environment)
        setFlag(
            "flowInputPopup",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_INPUT_POPUP",
            environment: environment)
        setFlag(
            "localizedInputPopup",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOCALIZED_INPUT_POPUP",
            environment: environment)
        setFlag(
            "pickerValidation",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_PICKER_VALIDATION",
            environment: environment)
        setFlag(
            "flowMenuPopup",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_MENU_POPUP",
            environment: environment)
        setFlag(
            "flowOverlayStack",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_OVERLAY_STACK",
            environment: environment)
        setFlag(
            "structuredRpc",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_STRUCTURED_RPC",
            environment: environment)
        setFlag(
            "viteHmr",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_VITE_HMR",
            environment: environment)
        setFlag(
            "reactorReload",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_REACTOR_RELOAD",
            environment: environment)
        setFlag(
            "javaReload",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_JAVA_RELOAD",
            environment: environment)
        setFlag(
            "javaRollback",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_JAVA_ROLLBACK",
            environment: environment)
        setFlag(
            "sidecarRestart",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_RESTART",
            environment: environment)
        setFlag(
            "sidecarRecoveryExhaustion",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_RECOVERY_EXHAUSTION",
            environment: environment)
        setFlag(
            "shutdown",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN",
            environment: environment)
        setFlag(
            "uploadReceiver",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER",
            environment: environment)
        setFlag(
            "downloadSaveOpen",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN",
            environment: environment)
        setFlag(
            "multiWindow",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_WINDOW",
            environment: environment)
        setFlag(
            "fileDialogCancel",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL",
            environment: environment)
        if let timeout = Double(
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS"] ?? ""),
           timeout > 0 {
            values["shutdownTimeoutSeconds"] = String(timeout)
        }
        if let probeFile = environment["VAADIN_DESKTOP_VITE_HMR_PROBE_FILE"],
           !probeFile.isEmpty {
            values["viteHmrProbeFile"] = probeFile
        }
        if let probeFile = environment["VAADIN_DESKTOP_JAVA_RELOAD_PROBE_FILE"],
           !probeFile.isEmpty {
            values["javaReloadProbeFile"] = probeFile
        }
        if let probeFile = environment[
            "VAADIN_DESKTOP_REACTOR_RELOAD_PROBE_FILE"],
           !probeFile.isEmpty {
            values["reactorReloadProbeFile"] = probeFile
        }
        if let directory = environment[
            "VAADIN_DESKTOP_REACTOR_RELOAD_APPLICATION_DIRECTORY"],
           !directory.isEmpty {
            values["reactorReloadApplicationDirectory"] = directory
        }
        if let sourceFile = environment[
            "VAADIN_DESKTOP_EFFECTIVE_MAVEN_SOURCE_FILE"],
           !sourceFile.isEmpty {
            values["effectiveMavenSourceFile"] = sourceFile
        }
        if let resourceFile = environment[
            "VAADIN_DESKTOP_EFFECTIVE_MAVEN_RESOURCE_FILE"],
           !resourceFile.isEmpty {
            values["effectiveMavenResourceFile"] = resourceFile
        }
        if let probeFile = environment["VAADIN_DESKTOP_JAVA_ROLLBACK_PROBE_FILE"],
           !probeFile.isEmpty {
            values["javaRollbackProbeFile"] = probeFile
        }
        configureOpenPanelAutomation()
    }

    private func setFlag(
        _ name: String,
        from environmentName: String,
        environment: [String: String]
    ) {
        if environment[environmentName] == "1" {
            flags.insert(name)
        } else {
            flags.remove(name)
        }
    }

    private func configureOpenPanelAutomation() {
        for observer in openPanelObservers {
            NotificationCenter.default.removeObserver(observer)
        }
        openPanelObservers.removeAll()
        guard flag("fileDialogCancel") else {
            return
        }

        let cancelOpenPanel: (Notification) -> Void = { notification in
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                let panel = notification.object as? NSOpenPanel
                    ?? (notification.object as? NSWindow)?.attachedSheet as? NSOpenPanel
                if let panel {
                    panel.cancel(nil)
                }
            }
        }
        openPanelObservers.append(NotificationCenter.default.addObserver(
            forName: NSWindow.didBecomeKeyNotification,
            object: nil,
            queue: .main,
            using: cancelOpenPanel))
        openPanelObservers.append(NotificationCenter.default.addObserver(
            forName: NSWindow.willBeginSheetNotification,
            object: nil,
            queue: .main,
            using: cancelOpenPanel))
    }
}

func makeHostRuntimeObserver() -> any HostRuntimeObserver {
    FlowIpcAcceptanceRuntimeObserver()
}

extension HostConfig {
    private var flowIpcAcceptanceObserver: FlowIpcAcceptanceRuntimeObserver {
        guard let observer = runtimeObserver as? FlowIpcAcceptanceRuntimeObserver else {
            preconditionFailure("Flow IPC acceptance observer is not installed")
        }
        return observer
    }

    var flowIpcAutoFrameworkUpdate: Bool {
        flowIpcAcceptanceObserver.flag("frameworkUpdate")
    }

    var flowIpcAutoPushRecovery: Bool {
        flowIpcAcceptanceObserver.flag("pushRecovery")
    }

    var flowIpcAutoPushDelivery: Bool {
        flowIpcAcceptanceObserver.flag("pushDelivery")
    }

    var flowIpcAutoLargeDownloadResume: Bool {
        flowIpcAcceptanceObserver.flag("largeDownloadResume")
    }

    var flowIpcAutoLargeUploadRetry: Bool {
        flowIpcAcceptanceObserver.flag("largeUploadRetry")
    }

    var flowIpcAutoInFlightRequestRecovery: Bool {
        flowIpcAcceptanceObserver.flag("inFlightRequestRecovery")
    }

    var flowIpcAutoLargeMultipartUploadRetry: Bool {
        flowIpcAcceptanceObserver.flag("largeMultipartUploadRetry")
    }

    var flowIpcAutoConcurrentUpload: Bool {
        flowIpcAcceptanceObserver.flag("concurrentUpload")
    }

    var flowIpcAutoRequestFanout: Bool {
        flowIpcAcceptanceObserver.flag("requestFanout")
    }

    var flowIpcAutoConcurrentMultipartUpload: Bool {
        flowIpcAcceptanceObserver.flag("concurrentMultipartUpload")
    }

    var flowIpcAutoCrossSessionMultipartUpload: Bool {
        flowIpcAcceptanceObserver.flag("crossSessionMultipartUpload")
    }

    var flowIpcAutoLazyGridQuery: Bool {
        flowIpcAcceptanceObserver.flag("lazyGridQuery")
    }

    var flowIpcAutoDnd: Bool {
        flowIpcAcceptanceObserver.flag("flowDnd")
    }

    var flowIpcAutoInputPopup: Bool {
        flowIpcAcceptanceObserver.flag("flowInputPopup")
    }

    var flowIpcAutoLocalizedInputPopup: Bool {
        flowIpcAcceptanceObserver.flag("localizedInputPopup")
    }

    var flowIpcAutoPickerValidation: Bool {
        flowIpcAcceptanceObserver.flag("pickerValidation")
    }

    var flowIpcAutoMenuPopup: Bool {
        flowIpcAcceptanceObserver.flag("flowMenuPopup")
    }

    var flowIpcAutoOverlayStack: Bool {
        flowIpcAcceptanceObserver.flag("flowOverlayStack")
    }

    var flowIpcAutoStructuredRpc: Bool {
        flowIpcAcceptanceObserver.flag("structuredRpc")
    }

    var flowIpcAutoViteHmr: Bool {
        flowIpcAcceptanceObserver.flag("viteHmr")
    }

    var flowIpcAutoJavaReload: Bool {
        flowIpcAcceptanceObserver.flag("javaReload")
    }

    var flowIpcAutoReactorReload: Bool {
        flowIpcAcceptanceObserver.flag("reactorReload")
    }

    var flowIpcAutoJavaRollback: Bool {
        flowIpcAcceptanceObserver.flag("javaRollback")
    }

    var flowIpcAutoSidecarRestart: Bool {
        flowIpcAcceptanceObserver.flag("sidecarRestart")
    }

    var flowIpcAutoSidecarRecoveryExhaustion: Bool {
        flowIpcAcceptanceObserver.flag("sidecarRecoveryExhaustion")
    }

    var flowIpcAutoShutdown: Bool {
        flowIpcAcceptanceObserver.flag("shutdown")
    }

    var flowIpcAutoUploadReceiver: Bool {
        flowIpcAcceptanceObserver.flag("uploadReceiver")
    }

    var flowIpcAutoDownloadSaveOpen: Bool {
        flowIpcAcceptanceObserver.flag("downloadSaveOpen")
    }

    var flowIpcAutoMultiWindow: Bool {
        flowIpcAcceptanceObserver.flag("multiWindow")
    }

    var flowIpcAutoFileDialogCancel: Bool {
        flowIpcAcceptanceObserver.flag("fileDialogCancel")
    }

    var flowIpcAutoShutdownTimeoutSeconds: Double {
        Double(flowIpcAcceptanceObserver.values["shutdownTimeoutSeconds"] ?? "") ?? 0
    }

    var flowIpcViteHmrProbeFile: String? {
        flowIpcAcceptanceObserver.values["viteHmrProbeFile"]
    }

    var flowIpcJavaReloadProbeFile: String? {
        flowIpcAcceptanceObserver.values["javaReloadProbeFile"]
    }

    var flowIpcReactorReloadProbeFile: String? {
        flowIpcAcceptanceObserver.values["reactorReloadProbeFile"]
    }

    var flowIpcReactorReloadApplicationDirectory: String? {
        flowIpcAcceptanceObserver.values[
            "reactorReloadApplicationDirectory"]
    }

    var flowIpcEffectiveMavenSourceFile: String? {
        flowIpcAcceptanceObserver.values["effectiveMavenSourceFile"]
    }

    var flowIpcEffectiveMavenResourceFile: String? {
        flowIpcAcceptanceObserver.values["effectiveMavenResourceFile"]
    }

    var flowIpcJavaRollbackProbeFile: String? {
        flowIpcAcceptanceObserver.values["javaRollbackProbeFile"]
    }
}
