import Cocoa
import Foundation

final class FlowIpcAcceptanceRuntimeHooks: HostRuntimeHooks {
    var flags: Set<String> = []
    var values: [String: String] = [:]
    var webViewDiagnostics: WebViewDiagnostics?
    private var openPanelObserver: NSObjectProtocol?

    func flag(_ name: String) -> Bool {
        flags.contains(name)
    }

    func configure(environment: [String: String]) {
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
        if let openPanelObserver {
            NotificationCenter.default.removeObserver(openPanelObserver)
            self.openPanelObserver = nil
        }
        guard flag("fileDialogCancel") else {
            return
        }
        openPanelObserver = NotificationCenter.default.addObserver(
            forName: NSWindow.didBecomeKeyNotification,
            object: nil,
            queue: .main
        ) { notification in
            guard let panel = notification.object as? NSOpenPanel else {
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                panel.cancel(nil)
            }
        }
    }
}

func makeHostRuntimeHooks() -> any HostRuntimeHooks {
    FlowIpcAcceptanceRuntimeHooks()
}

extension HostConfig {
    private var flowIpcAcceptanceHooks: FlowIpcAcceptanceRuntimeHooks {
        guard let hooks = runtimeHooks as? FlowIpcAcceptanceRuntimeHooks else {
            preconditionFailure("Flow IPC acceptance hooks are not installed")
        }
        return hooks
    }

    var flowIpcAutoFrameworkUpdate: Bool {
        flowIpcAcceptanceHooks.flag("frameworkUpdate")
    }

    var flowIpcAutoSidecarRestart: Bool {
        flowIpcAcceptanceHooks.flag("sidecarRestart")
    }

    var flowIpcAutoShutdown: Bool {
        flowIpcAcceptanceHooks.flag("shutdown")
    }

    var flowIpcAutoUploadReceiver: Bool {
        flowIpcAcceptanceHooks.flag("uploadReceiver")
    }

    var flowIpcAutoDownloadSaveOpen: Bool {
        flowIpcAcceptanceHooks.flag("downloadSaveOpen")
    }

    var flowIpcAutoMultiWindow: Bool {
        flowIpcAcceptanceHooks.flag("multiWindow")
    }

    var flowIpcAutoFileDialogCancel: Bool {
        flowIpcAcceptanceHooks.flag("fileDialogCancel")
    }

    var flowIpcAutoShutdownTimeoutSeconds: Double {
        Double(flowIpcAcceptanceHooks.values["shutdownTimeoutSeconds"] ?? "") ?? 0
    }
}
