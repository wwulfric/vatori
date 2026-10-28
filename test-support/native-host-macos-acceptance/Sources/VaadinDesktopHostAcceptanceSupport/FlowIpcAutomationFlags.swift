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
    var initialNavigationWatchdog = FlowIpcInitialNavigationWatchdog()
    var initialNavigationTimeoutWorkItem: DispatchWorkItem?
    private var openPanelObservers: [NSObjectProtocol] = []

    func flag(_ name: String) -> Bool {
        flags.contains(name)
    }

    func applicationWillTerminate(host: WebViewHost) {
        if flag("sidecarWriteBackpressure") {
            observeSidecarWriteBackpressure()
        }
        if flag("hostShutdownTermination") {
            observeHostShutdownTermination()
        }
        if flag("lastWindowClose") {
            host.observeLastWindowCloseTermination()
        }
        if flag("fileWriteGrantOwnerCloseSandbox") {
            host.observeFileWriteGrantOwnerCloseSandboxTermination()
        }
        if flag("clipboardOutcomes") {
            restoreClipboardOutcomesPasteboardOnTermination()
        }
        if flag("notificationDelivery") {
            cleanupNotificationDeliveryOnTermination()
        }
        if flag("nativeClipboardEdit") {
            restoreNativeClipboardEditPasteboardOnTermination()
        }
        if flag("fileReadGrantHostShutdown") {
            host.observeFileReadGrantHostShutdown()
        }
        if flag("fileWriteGrantHostShutdownSandbox") {
            host.observeFileWriteGrantHostShutdownSandboxTermination()
        }
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
            "capabilityGenerationRecovery",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_CAPABILITY_GENERATION_RECOVERY",
            environment: environment)
        setFlag(
            "capabilityOwnerWindowClose",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_CAPABILITY_OWNER_WINDOW_CLOSE",
            environment: environment)
        setFlag(
            "capabilityContextReopen",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_CAPABILITY_CONTEXT_REOPEN",
            environment: environment)
        setFlag(
            "mainWindowCloseRecovery",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_MAIN_WINDOW_CLOSE_RECOVERY",
            environment: environment)
        setFlag(
            "lastWindowClose",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LAST_WINDOW_CLOSE",
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
            "lazyGridSelectedRemoval",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_GRID_SELECTED_REMOVAL",
            environment: environment)
        setFlag(
            "lazyMultiSelectComboBox",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_LAZY_MULTI_SELECT_COMBO_BOX",
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
            "multiSelectComboBox",
            from:
                "VAADIN_DESKTOP_FLOW_IPC_AUTO_MULTI_SELECT_COMBO_BOX",
            environment: environment)
        setFlag(
            "localizedInputPopup",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOCALIZED_INPUT_POPUP",
            environment: environment)
        setFlag(
            "localizedNumberValidation",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOCALIZED_NUMBER_VALIDATION",
            environment: environment)
        setFlag(
            "localizedNumber",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_LOCALIZED_NUMBER",
            environment: environment)
        setFlag(
            "nativeClipboardEdit",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_CLIPBOARD_EDIT",
            environment: environment)
        setFlag(
            "nativeEdit",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_EDIT",
            environment: environment)
        setFlag(
            "nativeEditIsolation",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_EDIT_ISOLATION",
            environment: environment)
        setFlag(
            "nativeEditReopen",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_EDIT_REOPEN",
            environment: environment)
        setFlag(
            "nativeEditRecovery",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NATIVE_EDIT_RECOVERY",
            environment: environment)
        setFlag(
            "recoveryTermination",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_RECOVERY_TERMINATION",
            environment: environment)
        values["recoveryTerminationDirectory"] =
            environment["VAADIN_DESKTOP_TEST_RECOVERY_TERMINATION_DIR"]
        setFlag(
            "hostShutdownTermination",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_HOST_SHUTDOWN_TERMINATION",
            environment: environment)
        setFlag(
            "sidecarWriteBackpressure",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_WRITE_BACKPRESSURE",
            environment: environment)
        values["hostShutdownTerminationDirectory"] =
            environment["VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_DIR"]
        values["hostShutdownTerminationMode"] =
            environment["VAADIN_DESKTOP_TEST_HOST_SHUTDOWN_MODE"]
        setFlag(
            "externalFileDrop",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_EXTERNAL_FILE_DROP",
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
            "capabilityPermission",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_CAPABILITY_PERMISSION",
            environment: environment)
        setFlag(
            "capabilityDefaultDeny",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_CAPABILITY_DEFAULT_DENY",
            environment: environment)
        setFlag(
            "clipboardOutcomes",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_CLIPBOARD_OUTCOMES",
            environment: environment)
        setFlag(
            "fileDialogOutcomes",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_OUTCOMES",
            environment: environment)
        setFlag(
            "notificationOutcomes",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_OUTCOMES",
            environment: environment)
        values["notificationExpectedOutcome"] =
            environment["VAADIN_DESKTOP_FLOW_IPC_NOTIFICATION_EXPECTED_OUTCOME"]
        setFlag(
            "notificationDelivery",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_NOTIFICATION_DELIVERY",
            environment: environment)
        setFlag(
            "fileReadGrant",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT",
            environment: environment)
        setFlag(
            "fileReadGrantSidecarExit",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT_SIDECAR_EXIT",
            environment: environment)
        setFlag(
            "fileReadGrantHostShutdown",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT_HOST_SHUTDOWN",
            environment: environment)
        setFlag(
            "fileReadGrantIsolation",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT_ISOLATION",
            environment: environment)
        setFlag(
            "fileReadGrantReplay",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT_REPLAY",
            environment: environment)
        setFlag(
            "fileReadGrantRange",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT_RANGE",
            environment: environment)
        setFlag(
            "fileReadGrantSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_READ_GRANT_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantCancelSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_CANCEL_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantOwnerCloseSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_OWNER_CLOSE_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantIsolationSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_ISOLATION_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantSidecarExitSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_SIDECAR_EXIT_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantReplaySandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_REPLAY_SANDBOX",
            environment: environment)
        setFlag(
            "fileWriteGrantHostShutdownSandbox",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_WRITE_GRANT_HOST_SHUTDOWN_SANDBOX",
            environment: environment)
        setFlag(
            "fileDialogCancel",
            from: "VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL",
            environment: environment)
        setFlag(
            "stopInitialNavigation",
            from: "VAADIN_DESKTOP_FLOW_IPC_STOP_INITIAL_NAVIGATION",
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

    var flowIpcAutoCapabilityGenerationRecovery: Bool {
        flowIpcAcceptanceObserver.flag("capabilityGenerationRecovery")
    }

    var flowIpcAutoCapabilityOwnerWindowClose: Bool {
        flowIpcAcceptanceObserver.flag("capabilityOwnerWindowClose")
    }

    var flowIpcAutoCapabilityContextReopen: Bool {
        flowIpcAcceptanceObserver.flag("capabilityContextReopen")
    }

    var flowIpcAutoMainWindowCloseRecovery: Bool {
        flowIpcAcceptanceObserver.flag("mainWindowCloseRecovery")
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

    var flowIpcAutoLazyGridSelectedRemoval: Bool {
        flowIpcAcceptanceObserver.flag("lazyGridSelectedRemoval")
    }

    var flowIpcAutoLazyMultiSelectComboBox: Bool {
        flowIpcAcceptanceObserver.flag("lazyMultiSelectComboBox")
    }

    var flowIpcAutoDnd: Bool {
        flowIpcAcceptanceObserver.flag("flowDnd")
    }

    var flowIpcAutoInputPopup: Bool {
        flowIpcAcceptanceObserver.flag("flowInputPopup")
    }

    var flowIpcAutoMultiSelectComboBox: Bool {
        flowIpcAcceptanceObserver.flag("multiSelectComboBox")
    }

    var flowIpcAutoLocalizedInputPopup: Bool {
        flowIpcAcceptanceObserver.flag("localizedInputPopup")
    }

    var flowIpcAutoLocalizedNumberValidation: Bool {
        flowIpcAcceptanceObserver.flag("localizedNumberValidation")
    }

    var flowIpcAutoLocalizedNumber: Bool {
        flowIpcAcceptanceObserver.flag("localizedNumber")
    }

    var flowIpcAutoNativeClipboardEdit: Bool {
        flowIpcAcceptanceObserver.flag("nativeClipboardEdit")
    }

    var flowIpcAutoNativeEdit: Bool {
        flowIpcAcceptanceObserver.flag("nativeEdit")
    }

    var flowIpcAutoNativeEditIsolation: Bool {
        flowIpcAcceptanceObserver.flag("nativeEditIsolation")
    }

    var flowIpcAutoNativeEditReopen: Bool {
        flowIpcAcceptanceObserver.flag("nativeEditReopen")
    }

    var flowIpcAutoNativeEditRecovery: Bool {
        flowIpcAcceptanceObserver.flag("nativeEditRecovery")
    }

    var flowIpcAutoRecoveryTermination: Bool {
        flowIpcAcceptanceObserver.flag("recoveryTermination")
    }

    var flowIpcRecoveryTerminationDirectory: String {
        flowIpcAcceptanceObserver.values["recoveryTerminationDirectory"] ?? ""
    }

    var flowIpcAutoHostShutdownTermination: Bool {
        flowIpcAcceptanceObserver.flag("hostShutdownTermination")
    }

    var flowIpcAutoSidecarWriteBackpressure: Bool {
        flowIpcAcceptanceObserver.flag("sidecarWriteBackpressure")
    }

    var flowIpcHostShutdownTerminationDirectory: String {
        flowIpcAcceptanceObserver.values["hostShutdownTerminationDirectory"] ?? ""
    }

    var flowIpcHostShutdownTerminationMode: String {
        flowIpcAcceptanceObserver.values["hostShutdownTerminationMode"] ?? ""
    }

    var flowIpcAutoExternalFileDrop: Bool {
        flowIpcAcceptanceObserver.flag("externalFileDrop")
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

    var flowIpcAutoCapabilityPermission: Bool {
        flowIpcAcceptanceObserver.flag("capabilityPermission")
    }

    var flowIpcAutoCapabilityDefaultDeny: Bool {
        flowIpcAcceptanceObserver.flag("capabilityDefaultDeny")
    }

    var flowIpcAutoClipboardOutcomes: Bool {
        flowIpcAcceptanceObserver.flag("clipboardOutcomes")
    }

    var flowIpcAutoFileDialogOutcomes: Bool {
        flowIpcAcceptanceObserver.flag("fileDialogOutcomes")
    }

    var flowIpcAutoNotificationOutcomes: Bool {
        flowIpcAcceptanceObserver.flag("notificationOutcomes")
    }

    var flowIpcAutoNotificationDelivery: Bool {
        flowIpcAcceptanceObserver.flag("notificationDelivery")
    }

    var flowIpcNotificationExpectedOutcome: String {
        flowIpcAcceptanceObserver.values["notificationExpectedOutcome"] ?? ""
    }

    var flowIpcAutoFileReadGrant: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrant")
    }

    var flowIpcAutoFileReadGrantSidecarExit: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrantSidecarExit")
    }

    var flowIpcAutoFileReadGrantHostShutdown: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrantHostShutdown")
    }

    var flowIpcAutoFileReadGrantIsolation: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrantIsolation")
    }

    var flowIpcAutoFileReadGrantReplay: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrantReplay")
    }

    var flowIpcAutoFileReadGrantRange: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrantRange")
    }

    var flowIpcAutoFileReadGrantSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileReadGrantSandbox")
    }

    var flowIpcAutoFileWriteGrantSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantSandbox")
    }

    var flowIpcAutoFileWriteGrantCancelSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantCancelSandbox")
    }

    var flowIpcAutoFileWriteGrantOwnerCloseSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantOwnerCloseSandbox")
    }

    var flowIpcAutoFileWriteGrantIsolationSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantIsolationSandbox")
    }

    var flowIpcAutoFileWriteGrantSidecarExitSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantSidecarExitSandbox")
    }

    var flowIpcAutoFileWriteGrantReplaySandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantReplaySandbox")
    }

    var flowIpcAutoFileWriteGrantHostShutdownSandbox: Bool {
        flowIpcAcceptanceObserver.flag("fileWriteGrantHostShutdownSandbox")
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
