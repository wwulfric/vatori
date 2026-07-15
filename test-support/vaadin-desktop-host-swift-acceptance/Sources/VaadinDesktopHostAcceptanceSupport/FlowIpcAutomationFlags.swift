import Foundation

extension HostConfig {
    private mutating func setAcceptanceFlag(_ name: String, enabled: Bool) {
        if enabled {
            flowIpcAcceptanceFlags.insert(name)
        } else {
            flowIpcAcceptanceFlags.remove(name)
        }
    }

    private func acceptanceFlag(_ name: String) -> Bool {
        flowIpcAcceptanceFlags.contains(name)
    }

    var flowIpcAutoFrameworkUpdate: Bool {
        get { acceptanceFlag("frameworkUpdate") }
        set { setAcceptanceFlag("frameworkUpdate", enabled: newValue) }
    }

    var flowIpcAutoSidecarRestart: Bool {
        get { acceptanceFlag("sidecarRestart") }
        set { setAcceptanceFlag("sidecarRestart", enabled: newValue) }
    }

    var flowIpcAutoShutdown: Bool {
        get { acceptanceFlag("shutdown") }
        set { setAcceptanceFlag("shutdown", enabled: newValue) }
    }

    var flowIpcAutoUploadReceiver: Bool {
        get { acceptanceFlag("uploadReceiver") }
        set { setAcceptanceFlag("uploadReceiver", enabled: newValue) }
    }

    var flowIpcAutoDownloadSaveOpen: Bool {
        get { acceptanceFlag("downloadSaveOpen") }
        set { setAcceptanceFlag("downloadSaveOpen", enabled: newValue) }
    }

    var flowIpcAutoMultiWindow: Bool {
        get { acceptanceFlag("multiWindow") }
        set { setAcceptanceFlag("multiWindow", enabled: newValue) }
    }

    var flowIpcAutoFileDialogCancel: Bool {
        get { acceptanceFlag("fileDialogCancel") }
        set { setAcceptanceFlag("fileDialogCancel", enabled: newValue) }
    }

    var flowIpcAutoShutdownTimeoutSeconds: Double {
        get { Double(flowIpcAcceptanceValues["shutdownTimeoutSeconds"] ?? "") ?? 0 }
        set { flowIpcAcceptanceValues["shutdownTimeoutSeconds"] = String(newValue) }
    }

    mutating func applyFlowIpcAcceptanceArgument(_ argument: String) {
        switch argument {
        case "--flow-ipc-auto-framework-update":
            flowIpcAutoFrameworkUpdate = true
        case "--flow-ipc-auto-sidecar-restart":
            flowIpcAutoSidecarRestart = true
        case "--flow-ipc-auto-shutdown":
            flowIpcAutoShutdown = true
        case "--flow-ipc-auto-streaming":
            flowIpcAutoUploadReceiver = true
            flowIpcAutoDownloadSaveOpen = true
        case "--flow-ipc-auto-multi-window":
            flowIpcAutoMultiWindow = true
        case "--flow-ipc-auto-file-dialog-cancel":
            flowIpcAutoFileDialogCancel = true
        default:
            return
        }
    }

    mutating func configureFlowIpcAcceptance(_ environment: [String: String]) {
        flowIpcAutoFrameworkUpdate =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE"] == "1"
        flowIpcAutoSidecarRestart =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_RESTART"] == "1"
        flowIpcAutoShutdown =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN"] == "1"
        flowIpcAutoUploadReceiver =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_UPLOAD_RECEIVER"] == "1"
        flowIpcAutoDownloadSaveOpen =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_DOWNLOAD_SAVE_OPEN"] == "1"
        flowIpcAutoMultiWindow =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_WINDOW"] == "1"
        flowIpcAutoFileDialogCancel =
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL"] == "1"
        if let timeout = Double(
            environment["VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS"] ?? ""),
           timeout > 0 {
            flowIpcAutoShutdownTimeoutSeconds = timeout
        }
        flowIpcAcceptanceHarnessEnabled = !flowIpcAcceptanceFlags.isEmpty
    }
}
