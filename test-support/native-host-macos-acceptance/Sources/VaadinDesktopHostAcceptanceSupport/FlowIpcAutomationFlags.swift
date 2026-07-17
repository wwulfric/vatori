import Cocoa
import Foundation

final class FlowIpcAcceptanceRuntimeObserver: HostRuntimeObserver {
    var flags: Set<String> = []
    var values: [String: String] = [:]
    var repositoryRoot: String = FileManager.default.temporaryDirectory.path
    var hostNoActivate = false
    var webViewDiagnostics: WebViewDiagnostics?
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
            "sidecarRestart",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_RESTART",
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

    var flowIpcAutoSidecarRestart: Bool {
        flowIpcAcceptanceObserver.flag("sidecarRestart")
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
}
