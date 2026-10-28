import Cocoa
import CoreFoundation
import Darwin
import UserNotifications

final class DesktopCommandExecutor {
    private static let maxActiveFileReadGrants = 32
    private static let maxRetiredFileReadGrants = 256
    private static let maxFileGrantReadBytes = 64 * 1024
    private static let maxActiveFileWriteGrants = 32
    private static let maxRetiredFileWriteGrants = 256

    private struct ActiveNativeOperation {
        let command: String
        let completion: ([String: Any]) -> Void
        let invalidationResult: (String) -> [String: Any]
    }

    private struct RetiredFileReadGrant {
        let reason: String
        let diagnosticId: Int
    }

    private final class ActiveFileReadGrant {
        let id: String
        let diagnosticId: Int
        let ownerScope: FlowIpcWindowScope
        let sidecarGeneration: Int
        let url: URL
        let filename: String
        let byteCount: UInt64
        let fileHandle: FileHandle
        let securityScoped: Bool
        let queue: DispatchQueue
        var readInFlight = false
        var closing = false
        var closeScheduled = false
        var securityScopeStopped = false

        init(
            id: String,
            diagnosticId: Int,
            ownerScope: FlowIpcWindowScope,
            sidecarGeneration: Int,
            url: URL,
            filename: String,
            byteCount: UInt64,
            fileHandle: FileHandle,
            securityScoped: Bool
        ) {
            self.id = id
            self.diagnosticId = diagnosticId
            self.ownerScope = ownerScope
            self.sidecarGeneration = sidecarGeneration
            self.url = url
            self.filename = filename
            self.byteCount = byteCount
            self.fileHandle = fileHandle
            self.securityScoped = securityScoped
            self.queue = DispatchQueue(
                label: "dev.vatori.file-read-grant.\(diagnosticId)",
                qos: .userInitiated)
        }
    }

    private final class ActiveFileWriteGrant {
        let id = UUID().uuidString.lowercased()
        let diagnosticId: Int
        let ownerScope: FlowIpcWindowScope
        let sidecarGeneration: Int
        let url: URL
        let descriptor: Int32
        let securityScoped: Bool
        let queue: DispatchQueue
        // All mutable state is confined to the AppKit main thread. Only the
        // descriptor's I/O and the separately locked cancellation signal cross
        // to this grant's serial queue.
        let cancellation = NativeFileWriteIO.CancellationSignal()
        var cancellationClose = NativeFileWriteIO.CancellationCloseBarrier()
        var nextOffset: Int64 = 0
        var writeInFlight = false
        var closing = false
        var closeScheduled = false
        var securityScopeStopped = false

        init(diagnosticId: Int, ownerScope: FlowIpcWindowScope,
             sidecarGeneration: Int, url: URL, descriptor: Int32,
             securityScoped: Bool) {
            self.diagnosticId = diagnosticId
            self.ownerScope = ownerScope
            self.sidecarGeneration = sidecarGeneration
            self.url = url
            self.descriptor = descriptor
            self.securityScoped = securityScoped
            self.queue = DispatchQueue(
                label: "dev.vatori.file-write-grant.\(diagnosticId)",
                qos: .userInitiated)
        }
    }

    private struct RetiredFileWriteGrant {
        let reason: String
        let diagnosticId: Int
    }

    private let config: HostConfig
    private let capabilityManifest: CapabilityManifest
    private let resourceCleanup: NativeResourceCleanupTracker
    private let windowId: String
    weak var window: NSWindow?
    private var operationSequence = 0
    private var activeNativeOperations: [Int: ActiveNativeOperation] = [:]
    private var notificationIdentifiersByOperation: [Int: String] = [:]
    private var suppressedPanelCompletionTokens: Set<Int> = []
    private var fileGrantSequence = 0
    private var activeFileReadGrants: [String: ActiveFileReadGrant] = [:]
    private var retiredFileReadGrants: [String: RetiredFileReadGrant] = [:]
    private var retiredFileReadGrantOrder: [String] = []
    private var activeFileWriteGrants: [String: ActiveFileWriteGrant] = [:]
    private var retiredFileWriteGrants: [String: RetiredFileWriteGrant] = [:]
    private var retiredFileWriteGrantOrder: [String] = []
    private var activePanelOperation: (
        token: Int,
        command: String,
        panel: NSSavePanel
    )?

    init(config: HostConfig,
         capabilityManifest: CapabilityManifest,
         resourceCleanup: NativeResourceCleanupTracker,
         windowId: String = "main") {
        self.config = config
        self.capabilityManifest = capabilityManifest
        self.resourceCleanup = resourceCleanup
        self.windowId = windowId
    }

    func execute(_ command: String,
                 args: [String: Any],
                 ownerScope: FlowIpcWindowScope,
                 sidecarGeneration: Int,
                 completion: @escaping ([String: Any]) -> Void) {
        precondition(Thread.isMainThread,
                     "Desktop commands must execute on the AppKit main thread")
        if !capabilityManifest.allows(command, windowId: windowId) {
            completion(capabilityPermission([
                "success": false,
                "command": command,
                "error": capabilityErrorMessage(reason: capabilityManifest.denialReason(command, windowId: windowId))
            ], decision: "denied", reason: capabilityManifest.denialReason(command, windowId: windowId)))
            return
        }
        switch command {
        case "window.setTitle":
            guard let title = args["title"] as? String else {
                completion(capabilityPermission([
                    "success": false,
                    "command": command,
                    "error": "Missing title"
                ]))
                return
            }
            window?.title = title
            completion(capabilityPermission([
                "success": true,
                "command": command,
                "title": title,
                "mode": "native-appkit"
            ]))
        case "clipboard.writeText":
            guard let text = args["text"] as? String else {
                completion(capabilityPermission([
                    "success": false,
                    "command": command,
                    "mode": "native-nspasteboard",
                    "written": false,
                    "textLength": 0,
                    "error": "Missing text"
                ]))
                return
            }
            let pasteboard = NSPasteboard.general
            pasteboard.clearContents()
            let written = pasteboard.setString(text, forType: .string)
            var result: [String: Any] = [
                "success": written,
                "command": command,
                "mode": "native-nspasteboard",
                "written": written,
                "textLength": text.count
            ]
            if !written {
                result["error"] = "Unable to write text to the system clipboard"
            }
            completion(capabilityPermission(result))
        case "clipboard.readText":
            let text = NSPasteboard.general.string(forType: .string)
            var result: [String: Any] = [
                "success": true,
                "command": command,
                "mode": "native-nspasteboard",
                "textPresent": text != nil,
                "textLength": text?.count ?? 0
            ]
            if let text {
                result["text"] = text
            }
            completion(capabilityPermission(result))
        case "notification.show":
            showNotification(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        case "fileDialog.openFile":
            openFileDialog(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        case "fileDialog.openReadGrant":
            openFileReadGrant(
                args: args,
                command: command,
                ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileGrant.read":
            readFileGrant(
                args: args,
                command: command,
                ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileGrant.release":
            releaseFileGrant(
                args: args,
                command: command,
                ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileTransfer.download":
            saveDownloadedFile(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        case "fileDialog.saveNewWriteGrant":
            saveNewFileWriteGrant(
                args: args, command: command, ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileGrant.write":
            writeFileGrant(
                args: args, command: command, ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileGrant.releaseWrite":
            releaseFileWriteGrant(
                args: args, command: command, ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileGrant.cancelWrite":
            cancelFileWriteGrant(
                args: args, command: command, ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration) { result in
                    completion(self.capabilityPermission(result))
                }
        case "fileTransfer.selectUploadFile":
            selectUploadFile(args: args, command: command) { result in
                completion(self.capabilityPermission(result))
            }
        default:
            completion(capabilityPermission([
                "success": false,
                "command": command,
                "error": "Unsupported desktop command"
            ], decision: "denied", reason: "unsupported-command"))
        }
    }

    func invalidateNativeOperations(reason: String) {
        precondition(Thread.isMainThread,
                     "Native operations must be invalidated on the AppKit main thread")
        let operations = activeNativeOperations.sorted { $0.key < $1.key }
        let notificationIdentifiers = Array(
            notificationIdentifiersByOperation.values)
        activeNativeOperations.removeAll()
        notificationIdentifiersByOperation.removeAll()
        let panelOperation = activePanelOperation
        activePanelOperation = nil
        let fileReadGrants = activeFileReadGrants.values.sorted {
            $0.diagnosticId < $1.diagnosticId
        }
        activeFileReadGrants.removeAll()
        for grant in fileReadGrants {
            grant.closing = true
            rememberRetiredFileReadGrant(grant, reason: reason)
        }
        let fileWriteGrants = activeFileWriteGrants.values.sorted {
            $0.diagnosticId < $1.diagnosticId
        }
        activeFileWriteGrants.removeAll()
        for grant in fileWriteGrants {
            grant.closing = true
            rememberRetiredFileWriteGrant(grant, reason: reason)
        }

        for (operationToken, operation) in operations {
            trace("send", "nativeCapabilityOperation.invalidated", [
                "command": operation.command,
                "operationToken": operationToken,
                "reason": reason,
                "settlement": "canceled-result",
                "windowId": windowId
            ])
        }

        if !notificationIdentifiers.isEmpty {
            let center = UNUserNotificationCenter.current()
            center.removePendingNotificationRequests(
                withIdentifiers: notificationIdentifiers)
            center.removeDeliveredNotifications(
                withIdentifiers: notificationIdentifiers)
        }
        if let panelOperation {
            suppressedPanelCompletionTokens.insert(panelOperation.token)
            panelOperation.panel.cancel(nil)
            panelOperation.panel.orderOut(nil)
            traceDroppedPanelCompletion(
                command: panelOperation.command,
                operationToken: panelOperation.token)
        }
        for grant in fileReadGrants {
            scheduleFileReadGrantClose(
                grant,
                reason: reason,
                releaseOperationToken: nil)
        }
        for grant in fileWriteGrants {
            scheduleFileWriteGrantClose(
                grant, reason: reason, releaseOperationToken: nil)
        }

        for (_, operation) in operations {
            operation.completion(operation.invalidationResult(reason))
        }
    }

    private func capabilityErrorMessage(reason: String) -> String {
        switch reason {
        case "manifest-invalid":
            return "Invalid desktop-capabilities.json: \(capabilityManifest.parseError)"
        case "manifest-missing":
            return "desktop-capabilities.json is required by manifest-enforced mode"
        case "manifest-denied":
            return "Command not allowed by desktop-capabilities.json"
        default:
            return "Unsupported desktop command"
        }
    }

    private func showNotification(
        args: [String: Any],
        command: String,
        completion: @escaping ([String: Any]) -> Void
    ) {
        let title = args["title"] as? String ?? ""
        let body = args["body"] as? String ?? ""
        guard !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            completion([
                "success": false,
                "command": command,
                "mode": "native-user-notifications",
                "scheduled": false,
                "errorCode": "notification-title-invalid",
                "error": "Notification title must not be blank"
            ])
            return
        }
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "mode": "native-user-notifications",
                    "title": title,
                    "canceled": true,
                    "cancelReason": reason,
                    "operationInvalidated": true,
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            DispatchQueue.main.async {
                guard self.isNativeOperationActive(operationToken) else {
                    self.traceDroppedAsyncCompletion(
                        command: command,
                        operationToken: operationToken)
                    return
                }
                switch settings.authorizationStatus {
                case .authorized, .provisional, .ephemeral:
                    self.scheduleNotification(
                        center: center, command: command, title: title,
                        body: body, operationToken: operationToken)
                case .notDetermined:
                    if self.config.hostNoActivate {
                        self.completeNativeOperation(operationToken, result: [
                            "success": false,
                            "command": command,
                            "mode": "native-user-notifications",
                            "scheduled": false,
                            "errorCode": "notification-authorization-required",
                            "error": "Notification permission requires an active authorization prompt"
                        ])
                        return
                    }
                    center.requestAuthorization(options: [.alert, .sound]) {
                        granted, error in
                        DispatchQueue.main.async {
                            guard self.isNativeOperationActive(
                                operationToken) else {
                                self.traceDroppedAsyncCompletion(
                                    command: command,
                                    operationToken: operationToken)
                                return
                            }
                            if let error {
                                // The authorization API can also report an error
                                // after a refusal. Only an independent OS settings
                                // observation establishes denial; an error code
                                // or localized message alone never does.
                                center.getNotificationSettings { finalSettings in
                                    DispatchQueue.main.async {
                                        guard self.isNativeOperationActive(operationToken) else {
                                            self.traceDroppedAsyncCompletion(
                                                command: command,
                                                operationToken: operationToken)
                                            return
                                        }
                                        self.completeNativeOperation(
                                            operationToken,
                                            result: NativeNotificationFailure.authorizationFailure(
                                                command: command, error: error,
                                                observedStatus: finalSettings.authorizationStatus))
                                    }
                                }
                                return
                            }
                            guard granted else {
                                self.completeNativeOperation(
                                    operationToken,
                                    result: [
                                    "success": false,
                                    "command": command,
                                    "mode": "native-user-notifications",
                                    "scheduled": false,
                                    "errorCode": "notification-authorization-denied",
                                    "error": "Notification permission was denied"
                                ])
                                return
                            }
                            self.scheduleNotification(
                                center: center, command: command,
                                title: title, body: body,
                                operationToken: operationToken)
                        }
                    }
                case .denied:
                    self.completeNativeOperation(operationToken, result: [
                        "success": false,
                        "command": command,
                        "mode": "native-user-notifications",
                        "scheduled": false,
                        "errorCode": "notification-authorization-denied",
                        "error": "Notification permission was denied"
                    ])
                @unknown default:
                    self.completeNativeOperation(operationToken, result: [
                        "success": false,
                        "command": command,
                        "mode": "native-user-notifications",
                        "scheduled": false,
                        "errorCode": "notification-authorization-unsupported",
                        "error": "Unsupported notification authorization state"
                    ])
                }
            }
        }
    }

    private func scheduleNotification(
        center: UNUserNotificationCenter,
        command: String,
        title: String,
        body: String,
        operationToken: Int
    ) {
        guard isNativeOperationActive(operationToken) else {
            traceDroppedAsyncCompletion(
                command: command,
                operationToken: operationToken)
            return
        }
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let identifier = "vatori-\(UUID().uuidString)"
        notificationIdentifiersByOperation[operationToken] = identifier
        center.add(UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: nil)) { error in
            DispatchQueue.main.async {
                NativeNotificationCompletion.complete(
                    submittedIdentifier: identifier,
                    operationActive: self.isNativeOperationActive(operationToken),
                    registeredIdentifier: self.notificationIdentifiersByOperation[operationToken],
                    removePending: center.removePendingNotificationRequests(withIdentifiers:),
                    removeDelivered: center.removeDeliveredNotifications(withIdentifiers:),
                    publish: {
                        self.notificationIdentifiersByOperation.removeValue(
                            forKey: operationToken)
                        if let error {
                            self.completeNativeOperation(
                                operationToken,
                                result: NativeNotificationFailure.payload(
                                    command: command, stage: .scheduling, error: error))
                        } else {
                            self.completeNativeOperation(operationToken, result: [
                                "success": true,
                                "command": command,
                                "mode": "native-user-notifications",
                                "scheduled": true,
                                "notificationId": identifier,
                                "title": title,
                                "body": body
                            ])
                        }
                    },
                    drop: {
                        self.traceDroppedAsyncCompletion(
                            command: command, operationToken: operationToken)
                    })
            }
        }
    }

    private func openFileDialog(args: [String: Any],
                                command: String,
                                completion: @escaping ([String: Any]) -> Void) {
        let title = args["title"] as? String ?? "Open File"
        let allowsMultipleSelection = args["allowsMultipleSelection"] as? Bool ?? false
        if activePanelOperation != nil {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "title": title,
                "canceled": true,
                "paths": [],
                "allowsMultipleSelection": allowsMultipleSelection,
                "securityScoped": false,
                "error": "File dialog already in progress"
            ])
            return
        }

        let panel = NSOpenPanel()
        panel.title = title
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = allowsMultipleSelection
        panel.canCreateDirectories = false
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "title": title,
                    "canceled": true,
                    "paths": [],
                    "allowsMultipleSelection": allowsMultipleSelection,
                    "securityScoped": false,
                    "cancelReason": reason,
                    "operationInvalidated": true,
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        activePanelOperation = (
            token: operationToken,
            command: command,
            panel: panel)

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            guard let operation = self.activePanelOperation,
                  operation.token == operationToken,
                  operation.panel === panel else {
                if !self.consumeSuppressedPanelCompletion(operationToken) {
                    self.traceDroppedPanelCompletion(
                        command: command,
                        operationToken: operationToken)
                }
                return
            }
            self.activePanelOperation = nil
            let paths = response == .OK ? panel.urls.map(\.path) : []
            var result: [String: Any] = [
                "success": true,
                "command": command,
                "mode": "native-appkit-open-panel",
                "title": title,
                "canceled": response != .OK,
                "paths": paths,
                "allowsMultipleSelection": allowsMultipleSelection,
                "securityScoped": false
            ]
            if paths.count == 1 {
                result["path"] = paths[0]
            }
            self.completeNativeOperation(operationToken, result: result)
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func openFileReadGrant(
        args: [String: Any],
        command: String,
        ownerScope: FlowIpcWindowScope,
        sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        let title = args["title"] as? String ?? "Open File"
        if activePanelOperation != nil {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "open-read-grant",
                "title": title,
                "canceled": true,
                "grantPresent": false,
                "pathExposed": false,
                "maxReadBytes": Self.maxFileGrantReadBytes,
                "errorCode": "panel-in-progress",
                "error": "File dialog already in progress"
            ])
            return
        }
        guard activeFileReadGrants.count < Self.maxActiveFileReadGrants else {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "open-read-grant",
                "title": title,
                "canceled": false,
                "grantPresent": false,
                "pathExposed": false,
                "maxReadBytes": Self.maxFileGrantReadBytes,
                "errorCode": "grant-limit-reached",
                "error": "Active file read grant limit reached"
            ])
            return
        }

        let panel = NSOpenPanel()
        panel.title = title
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.canCreateDirectories = false
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "operation": "open-read-grant",
                    "title": title,
                    "canceled": true,
                    "grantPresent": false,
                    "pathExposed": false,
                    "maxReadBytes": Self.maxFileGrantReadBytes,
                    "cancelReason": reason,
                    "operationInvalidated": true,
                    "errorCode": "grant-selection-invalidated",
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        activePanelOperation = (
            token: operationToken,
            command: command,
            panel: panel)

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            guard let operation = self.activePanelOperation,
                  operation.token == operationToken,
                  operation.panel === panel else {
                if !self.consumeSuppressedPanelCompletion(operationToken) {
                    self.traceDroppedPanelCompletion(
                        command: command,
                        operationToken: operationToken)
                }
                return
            }
            self.activePanelOperation = nil
            guard response == .OK, let selectedURL = panel.url else {
                self.completeNativeOperation(operationToken, result: [
                    "success": true,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "operation": "open-read-grant",
                    "title": title,
                    "canceled": true,
                    "grantPresent": false,
                    "pathExposed": false,
                    "maxReadBytes": Self.maxFileGrantReadBytes,
                    "securityScoped": false,
                    "usedOpenPanel": true
                ])
                return
            }
            self.completeNativeOperation(
                operationToken,
                result: self.createFileReadGrantResult(
                    command: command,
                    title: title,
                    selectedURL: selectedURL,
                    ownerScope: ownerScope,
                    sidecarGeneration: sidecarGeneration))
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func createFileReadGrantResult(
        command: String,
        title: String,
        selectedURL: URL,
        ownerScope: FlowIpcWindowScope,
        sidecarGeneration: Int
    ) -> [String: Any] {
        guard activeFileReadGrants.count < Self.maxActiveFileReadGrants else {
            return [
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "open-read-grant",
                "title": title,
                "canceled": false,
                "grantPresent": false,
                "pathExposed": false,
                "maxReadBytes": Self.maxFileGrantReadBytes,
                "errorCode": "grant-limit-reached",
                "error": "Active file read grant limit reached"
            ]
        }

        let securityScoped = selectedURL.startAccessingSecurityScopedResource()
        var openedFileHandle: FileHandle?
        do {
            let fileHandle = try FileHandle(forReadingFrom: selectedURL)
            openedFileHandle = fileHandle
            let byteCount = try fileHandle.seekToEnd()
            try fileHandle.seek(toOffset: 0)
            guard byteCount <= UInt64(Int64.max) else {
                try? fileHandle.close()
                if securityScoped {
                    selectedURL.stopAccessingSecurityScopedResource()
                }
                return [
                    "success": false,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "operation": "open-read-grant",
                    "title": title,
                    "canceled": false,
                    "grantPresent": false,
                    "filename": selectedURL.lastPathComponent,
                    "maxReadBytes": Self.maxFileGrantReadBytes,
                    "securityScoped": securityScoped,
                    "pathExposed": false,
                    "usedOpenPanel": true,
                    "errorCode": "file-too-large",
                    "error": "Selected file size exceeds the supported range"
                ]
            }
            let jsonByteCount = Int64(byteCount)
            fileGrantSequence &+= 1
            let grant = ActiveFileReadGrant(
                id: UUID().uuidString.lowercased(),
                diagnosticId: fileGrantSequence,
                ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration,
                url: selectedURL,
                filename: selectedURL.lastPathComponent,
                byteCount: byteCount,
                fileHandle: fileHandle,
                securityScoped: securityScoped)
            activeFileReadGrants[grant.id] = grant
            trace("recv", "fileReadGrant.created", [
                "diagnosticId": grant.diagnosticId,
                "byteCount": jsonByteCount,
                "maxReadBytes": Self.maxFileGrantReadBytes,
                "securityScoped": securityScoped,
                "sidecarGeneration": sidecarGeneration,
                "windowId": ownerScope.windowId,
                "sessionId": ownerScope.sessionId,
                "uiId": ownerScope.uiId,
                "pathExposed": false
            ])
            return [
                "success": true,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "open-read-grant",
                "title": title,
                "canceled": false,
                "grantPresent": true,
                "grantId": grant.id,
                "filename": grant.filename,
                "byteCount": jsonByteCount,
                "maxReadBytes": Self.maxFileGrantReadBytes,
                "securityScoped": securityScoped,
                "pathExposed": false,
                "usedOpenPanel": true
            ]
        } catch {
            try? openedFileHandle?.close()
            if securityScoped {
                selectedURL.stopAccessingSecurityScopedResource()
            }
            return [
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "open-read-grant",
                "title": title,
                "canceled": false,
                "grantPresent": false,
                "filename": selectedURL.lastPathComponent,
                "maxReadBytes": Self.maxFileGrantReadBytes,
                "securityScoped": securityScoped,
                "pathExposed": false,
                "usedOpenPanel": true,
                "errorCode": "grant-open-failed",
                "error": "Selected file could not be opened for reading"
            ]
        }
    }

    private func readFileGrant(
        args: [String: Any],
        command: String,
        ownerScope: FlowIpcWindowScope,
        sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        guard let grantId = args["grantId"] as? String,
              let grant = activeFileReadGrants[grantId],
              !grant.closing,
              grant.ownerScope == ownerScope,
              grant.sidecarGeneration == sidecarGeneration else {
            traceUnavailableFileGrant(
                command: command,
                grantId: args["grantId"] as? String,
                ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration)
            completion(fileGrantUnavailableResult(command: command))
            return
        }
        guard let offset = exactIntegerArg(args, "offset"),
              let maxBytesValue = exactIntegerArg(args, "maxBytes"),
              offset >= 0,
              maxBytesValue > 0,
              maxBytesValue <= Int64(Self.maxFileGrantReadBytes) else {
            completion([
                "success": false,
                "command": command,
                "operation": "read-grant",
                "grantUnavailable": false,
                "grantState": "active",
                "errorCode": "invalid-read-range",
                "error": "File grant read range is invalid",
                "maxReadBytes": Self.maxFileGrantReadBytes
            ])
            return
        }
        let maxBytes = Int(maxBytesValue)
        guard !grant.readInFlight else {
            completion([
                "success": false,
                "command": command,
                "operation": "read-grant",
                "grantUnavailable": false,
                "grantState": "active",
                "errorCode": "grant-busy",
                "error": "File grant already has a read in progress"
            ])
            return
        }

        grant.readInFlight = true
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "operation": "read-grant",
                    "grantUnavailable": true,
                    "grantState": "unavailable",
                    "operationInvalidated": true,
                    "cancelReason": reason,
                    "errorCode": "grant-invalidated",
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        let requestedOffset = UInt64(offset)
        let remaining = requestedOffset < grant.byteCount
            ? grant.byteCount - requestedOffset
            : 0
        let boundedReadCount = Int(min(UInt64(maxBytes), remaining))
        grant.queue.async {
            let result: [String: Any]
            do {
                try grant.fileHandle.seek(toOffset: requestedOffset)
                let data = boundedReadCount == 0
                    ? Data()
                    : try grant.fileHandle.read(upToCount: boundedReadCount) ?? Data()
                let nextOffset = requestedOffset + UInt64(data.count)
                let jsonOffset = Int64(requestedOffset)
                let jsonNextOffset = Int64(nextOffset)
                result = [
                    "success": true,
                    "command": command,
                    "operation": "read-grant",
                    "grantUnavailable": false,
                    "grantState": "active",
                    "bodyEncoding": "base64",
                    "bodyBase64": data.base64EncodedString(),
                    "bytesRead": data.count,
                    "offset": jsonOffset,
                    "nextOffset": jsonNextOffset,
                    "endOfFile": nextOffset >= grant.byteCount
                        || data.count < boundedReadCount,
                    "byteCount": Int64(grant.byteCount),
                    "maxReadBytes": Self.maxFileGrantReadBytes,
                    "pathExposed": false
                ]
            } catch {
                result = [
                    "success": false,
                    "command": command,
                    "operation": "read-grant",
                    "grantUnavailable": false,
                    "grantState": "active",
                    "offset": Int64(requestedOffset),
                    "bytesRead": 0,
                    "errorCode": "grant-read-failed",
                    "error": "Selected file could not be read"
                ]
            }
            DispatchQueue.main.async {
                grant.readInFlight = false
                guard self.isNativeOperationActive(operationToken) else {
                    self.traceDroppedAsyncCompletion(
                        command: command,
                        operationToken: operationToken)
                    return
                }
                trace("recv", "fileReadGrant.read", [
                    "diagnosticId": grant.diagnosticId,
                    "offset": Int64(requestedOffset),
                    "bytesRead": result["bytesRead"] as? Int ?? 0,
                    "endOfFile": result["endOfFile"] as? Bool ?? false,
                    "sidecarGeneration": sidecarGeneration,
                    "windowId": ownerScope.windowId
                ])
                self.completeNativeOperation(operationToken, result: result)
            }
        }
    }

    private func releaseFileGrant(
        args: [String: Any],
        command: String,
        ownerScope: FlowIpcWindowScope,
        sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        guard let grantId = args["grantId"] as? String,
              let grant = activeFileReadGrants[grantId],
              !grant.closing,
              grant.ownerScope == ownerScope,
              grant.sidecarGeneration == sidecarGeneration else {
            traceUnavailableFileGrant(
                command: command,
                grantId: args["grantId"] as? String,
                ownerScope: ownerScope,
                sidecarGeneration: sidecarGeneration)
            completion(fileGrantUnavailableResult(command: command))
            return
        }

        grant.closing = true
        rememberRetiredFileReadGrant(grant, reason: "released")
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "operation": "release-grant",
                    "released": false,
                    "grantUnavailable": true,
                    "grantState": "unavailable",
                    "operationInvalidated": true,
                    "cancelReason": reason,
                    "errorCode": "grant-invalidated",
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        scheduleFileReadGrantClose(
            grant,
            reason: "released",
            releaseOperationToken: operationToken)
    }

    private func saveNewFileWriteGrant(
        args: [String: Any], command: String,
        ownerScope: FlowIpcWindowScope, sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        let title = args["title"] as? String ?? "Create File"
        let base: [String: Any] = [
            "command": command, "mode": "native-appkit-save-panel",
            "operation": "save-new-write-grant", "title": title,
            "grantPresent": false, "pathExposed": false,
            "createNewOnly": true, "maxWriteBytes": NativeFileWriteIO.maxWriteBytes
        ]
        if activePanelOperation != nil
                || activeFileWriteGrants.count >= Self.maxActiveFileWriteGrants {
            var result = base
            result["success"] = false
            result["canceled"] = false
            result["errorCode"] = activePanelOperation != nil
                ? "panel-in-progress" : "grant-limit-reached"
            result["error"] = activePanelOperation != nil
                ? "File dialog already in progress" : "Active file write grant limit reached"
            completion(result)
            return
        }
        let panel = NSSavePanel()
        panel.title = title
        panel.nameFieldStringValue = args["suggestedName"] as? String ?? "untitled"
        panel.canCreateDirectories = false
        let operationToken = beginNativeOperation(
            command: command, completion: completion,
            invalidationResult: { reason in
                var result = base
                result["success"] = false
                result["canceled"] = true
                result["cancelReason"] = reason
                result["operationInvalidated"] = true
                result["errorCode"] = "grant-selection-invalidated"
                result["error"] = Self.invalidationError(reason: reason)
                return result
            })
        activePanelOperation = (token: operationToken, command: command, panel: panel)
        let finish: (NSApplication.ModalResponse) -> Void = { response in
            guard let operation = self.activePanelOperation,
                  operation.token == operationToken, operation.panel === panel else {
                if !self.consumeSuppressedPanelCompletion(operationToken) {
                    self.traceDroppedPanelCompletion(
                        command: command, operationToken: operationToken)
                }
                return
            }
            self.activePanelOperation = nil
            var result = base
            result["usedSavePanel"] = true
            result["securityScoped"] = false
            guard response == .OK, let selectedURL = panel.url else {
                result["success"] = true
                result["canceled"] = true
                self.completeNativeOperation(operationToken, result: result)
                return
            }
            result["canceled"] = false
            guard self.activeFileWriteGrants.count < Self.maxActiveFileWriteGrants else {
                result["success"] = false
                result["errorCode"] = "grant-limit-reached"
                result["error"] = "Active file write grant limit reached"
                self.completeNativeOperation(operationToken, result: result)
                return
            }
            let securityScoped = selectedURL.startAccessingSecurityScopedResource()
            switch NativeFileWriteIO.createNew(at: selectedURL) {
            case .failed(let errorNumber):
                if securityScoped { selectedURL.stopAccessingSecurityScopedResource() }
                result["success"] = false
                result["securityScoped"] = securityScoped
                result["errorCode"] = errorNumber == EEXIST
                    ? "target-exists" : "grant-create-failed"
                result["error"] = errorNumber == EEXIST
                    ? "Selected destination already exists"
                    : "Selected destination could not be created"
            case .opened(let descriptor):
                self.fileGrantSequence &+= 1
                let grant = ActiveFileWriteGrant(
                    diagnosticId: self.fileGrantSequence,
                    ownerScope: ownerScope, sidecarGeneration: sidecarGeneration,
                    url: selectedURL, descriptor: descriptor,
                    securityScoped: securityScoped)
                self.activeFileWriteGrants[grant.id] = grant
                var created = self.fileWriteGrantTrace(grant)
                created["maxWriteBytes"] = NativeFileWriteIO.maxWriteBytes
                created["securityScoped"] = securityScoped
                created["createNewOnly"] = true
                created["pathExposed"] = false
                trace("recv", "fileWriteGrant.created", created)
                result["success"] = true
                result["grantPresent"] = true
                result["grantId"] = grant.id
                result["filename"] = selectedURL.lastPathComponent
                result["securityScoped"] = securityScoped
            }
            self.completeNativeOperation(operationToken, result: result)
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func writeFileGrant(
        args: [String: Any], command: String,
        ownerScope: FlowIpcWindowScope, sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        guard let grant = availableFileWriteGrant(
            args: args, command: command, ownerScope: ownerScope,
            sidecarGeneration: sidecarGeneration) else {
            completion(fileWriteGrantUnavailableResult(command: command))
            return
        }
        guard let offset = exactIntegerArg(args, "offset"), offset >= 0 else {
            completion(fileWriteGrantError(command: command,
                code: "invalid-write-range", message: "File grant write range is invalid"))
            return
        }
        guard let data = NativeFileWriteIO.boundedBody(args["bodyBase64"]) else {
            completion(fileWriteGrantError(command: command,
                code: "invalid-write-data", message: "File grant write data is invalid"))
            return
        }
        guard !grant.writeInFlight else {
            completion(fileWriteGrantError(command: command,
                code: "grant-busy", message: "File grant already has a write in progress"))
            return
        }
        guard NativeFileWriteIO.validRange(
            offset: offset, nextOffset: grant.nextOffset, byteCount: data.count) else {
            completion(fileWriteGrantError(command: command,
                code: "invalid-write-range", message: "File grant write range is invalid"))
            return
        }
        grant.writeInFlight = true
        let operationToken = beginNativeOperation(
            command: command, completion: completion,
            invalidationResult: { reason in
                self.invalidatedFileWriteGrantResult(command: command, reason: reason)
            })
        grant.queue.async {
            let written = NativeFileWriteIO.writeAll(
                descriptor: grant.descriptor, data: data, cancellation: grant.cancellation)
            NativeMainRunLoop.perform {
                grant.writeInFlight = false
                grant.nextOffset = offset + Int64(written.bytesWritten)
                self.closeCanceledFileWriteGrantIfSettled(grant)
                if written.errorNumber != nil, !grant.closing {
                    grant.closing = true
                    self.rememberRetiredFileWriteGrant(grant, reason: "write-failed")
                    self.scheduleFileWriteGrantClose(
                        grant, reason: "write-failed", releaseOperationToken: nil)
                }
                guard self.isNativeOperationActive(operationToken) else {
                    self.traceDroppedAsyncCompletion(
                        command: command, operationToken: operationToken)
                    return
                }
                let success = written.errorNumber == nil && !written.canceled
                var result: [String: Any] = [
                    "success": success, "command": command, "operation": "write-grant",
                    "grantUnavailable": grant.closing,
                    "grantState": grant.closing ? "unavailable" : "active",
                    "offset": offset, "bytesWritten": written.bytesWritten,
                    "nextOffset": grant.nextOffset,
                    "maxWriteBytes": NativeFileWriteIO.maxWriteBytes,
                    "pathExposed": false
                ]
                if written.canceled {
                    result["errorCode"] = "write-canceled"
                    result["error"] = "Selected file write was canceled"
                    result["canceled"] = true
                } else if !success {
                    result["errorCode"] = "write-failed"
                    result["error"] = "Selected file could not be fully written"
                }
                var event = self.fileWriteGrantTrace(grant)
                event["offset"] = offset
                event["bytesWritten"] = written.bytesWritten
                event["nextOffset"] = grant.nextOffset
                event["success"] = success
                if written.canceled { event["canceled"] = true }
                trace("recv", "fileWriteGrant.write", event)
                self.completeNativeOperation(operationToken, result: result)
            }
        }
    }

    private func releaseFileWriteGrant(
        args: [String: Any], command: String,
        ownerScope: FlowIpcWindowScope, sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        guard let grant = availableFileWriteGrant(
            args: args, command: command, ownerScope: ownerScope,
            sidecarGeneration: sidecarGeneration) else {
            completion(fileWriteGrantUnavailableResult(command: command))
            return
        }
        grant.closing = true
        rememberRetiredFileWriteGrant(grant, reason: "released")
        let operationToken = beginNativeOperation(
            command: command, completion: completion,
            invalidationResult: { reason in
                self.invalidatedFileWriteGrantResult(command: command, reason: reason)
            })
        scheduleFileWriteGrantClose(grant, reason: "released",
                                   releaseOperationToken: operationToken)
    }

    private func cancelFileWriteGrant(
        args: [String: Any], command: String,
        ownerScope: FlowIpcWindowScope, sidecarGeneration: Int,
        completion: @escaping ([String: Any]) -> Void
    ) {
        guard let grant = availableFileWriteGrant(
            args: args, command: command, ownerScope: ownerScope,
            sidecarGeneration: sidecarGeneration) else {
            completion(fileWriteGrantUnavailableResult(command: command))
            return
        }
        // This admission boundary is synchronous on the main thread. The
        // worker observes the signal cooperatively, never by concurrent close.
        grant.closing = true
        rememberRetiredFileWriteGrant(grant, reason: "canceled")
        grant.cancellation.requestCancel()
        let operationToken = beginNativeOperation(
            command: command, completion: completion,
            invalidationResult: { reason in
                self.invalidatedFileWriteGrantResult(command: command, reason: reason)
            })
        grant.closeScheduled = true
        // Register before waiting: owner close or Quit must drain this pending
        // cleanup even if the write worker has not returned yet.
        let cleanupToken = resourceCleanup.begin()
        grant.cancellationClose.request(operationToken: operationToken, cleanupToken: cleanupToken)
        var event = fileWriteGrantTrace(grant)
        event["reason"] = "canceled"
        event["writeInFlight"] = grant.writeInFlight
        event["securityScoped"] = grant.securityScoped
        trace("recv", "fileWriteGrant.cancelScheduled", event)
        closeCanceledFileWriteGrantIfSettled(grant)
    }

    private func closeCanceledFileWriteGrantIfSettled(_ grant: ActiveFileWriteGrant) {
        guard let pending = grant.cancellationClose.takeIfWriteSettled(writeInFlight: grant.writeInFlight) else { return }
        // nextOffset is published by the write's main-thread completion before
        // this enqueue. No ordering between separate run-loop blocks is needed.
        enqueueFileWriteGrantClose(grant, reason: "canceled", command: "fileGrant.cancelWrite",
                                  operationToken: pending.operationToken, cleanupToken: pending.cleanupToken)
    }

    private func availableFileWriteGrant(
        args: [String: Any], command: String,
        ownerScope: FlowIpcWindowScope, sidecarGeneration: Int
    ) -> ActiveFileWriteGrant? {
        let id = args["grantId"] as? String
        let grant = id.flatMap { activeFileWriteGrants[$0] }
        if let grant, !grant.closing, grant.ownerScope == ownerScope,
           grant.sidecarGeneration == sidecarGeneration { return grant }
        let retired = id.flatMap { retiredFileWriteGrants[$0] }
        let reason: String
        if id == nil || id?.isBlank == true { reason = "invalid-token" }
        else if let retired { reason = retired.reason }
        else if let grant, grant.ownerScope != ownerScope { reason = "owner-mismatch" }
        else if let grant, grant.sidecarGeneration != sidecarGeneration {
            reason = "generation-mismatch"
        } else if grant?.closing == true { reason = "closing" }
        else { reason = "unknown-token" }
        var event: [String: Any] = [
            "command": command, "reason": reason,
            "sidecarGeneration": sidecarGeneration, "windowId": ownerScope.windowId,
            "sessionId": ownerScope.sessionId, "uiId": ownerScope.uiId
        ]
        if let diagnosticId = grant?.diagnosticId ?? retired?.diagnosticId {
            event["diagnosticId"] = diagnosticId
        }
        trace("recv", "fileWriteGrant.accessDenied", event)
        return nil
    }

    private func fileWriteGrantError(
        command: String, code: String, message: String,
        unavailable: Bool = false
    ) -> [String: Any] {
        [
            "success": false, "command": command,
            "operation": command == "fileGrant.cancelWrite" ? "cancel-write-grant"
                : command == "fileGrant.releaseWrite" ? "release-write-grant" : "write-grant",
            "grantUnavailable": unavailable,
            "grantState": unavailable ? "unavailable" : "active",
            "errorCode": code, "error": message,
            "maxWriteBytes": NativeFileWriteIO.maxWriteBytes, "pathExposed": false
        ]
    }

    private func fileWriteGrantUnavailableResult(command: String) -> [String: Any] {
        fileWriteGrantError(command: command, code: "grant-unavailable",
                            message: "File grant is unavailable", unavailable: true)
    }

    private func invalidatedFileWriteGrantResult(
        command: String, reason: String
    ) -> [String: Any] {
        var result = fileWriteGrantError(command: command, code: "grant-invalidated",
            message: Self.invalidationError(reason: reason), unavailable: true)
        result["operationInvalidated"] = true
        result["cancelReason"] = reason
        if command == "fileGrant.releaseWrite" { result["released"] = false }
        if command == "fileGrant.cancelWrite" { result["canceled"] = false }
        return result
    }

    private func fileWriteGrantTrace(_ grant: ActiveFileWriteGrant) -> [String: Any] {
        [
            "diagnosticId": grant.diagnosticId,
            "sidecarGeneration": grant.sidecarGeneration,
            "windowId": grant.ownerScope.windowId,
            "sessionId": grant.ownerScope.sessionId, "uiId": grant.ownerScope.uiId
        ]
    }

    private func rememberRetiredFileWriteGrant(_ grant: ActiveFileWriteGrant, reason: String) {
        guard retiredFileWriteGrants[grant.id] == nil else { return }
        retiredFileWriteGrants[grant.id] = RetiredFileWriteGrant(
            reason: reason, diagnosticId: grant.diagnosticId)
        retiredFileWriteGrantOrder.append(grant.id)
        while retiredFileWriteGrantOrder.count > Self.maxRetiredFileWriteGrants {
            retiredFileWriteGrants.removeValue(forKey: retiredFileWriteGrantOrder.removeFirst())
        }
    }

    private func scheduleFileWriteGrantClose(
        _ grant: ActiveFileWriteGrant, reason: String, releaseOperationToken: Int?
    ) {
        if releaseOperationToken == nil, grant.securityScoped, !grant.securityScopeStopped {
            grant.url.stopAccessingSecurityScopedResource()
            grant.securityScopeStopped = true
        }
        guard !grant.closeScheduled else { return }
        grant.closeScheduled = true
        let cleanupToken = resourceCleanup.begin()
        var event = fileWriteGrantTrace(grant)
        event["reason"] = reason
        event["writeInFlight"] = grant.writeInFlight
        event["securityScoped"] = grant.securityScoped
        trace("recv", reason == "released" ? "fileWriteGrant.releaseScheduled"
                                           : "fileWriteGrant.invalidated", event)
        enqueueFileWriteGrantClose(grant, reason: reason, command: "fileGrant.releaseWrite",
                                  operationToken: releaseOperationToken, cleanupToken: cleanupToken)
    }

    private func enqueueFileWriteGrantClose(
        _ grant: ActiveFileWriteGrant, reason: String, command: String,
        operationToken: Int?, cleanupToken: Int
    ) {
        grant.queue.async {
            let closed = NativeFileWriteIO.close(descriptor: grant.descriptor)
            NativeMainRunLoop.perform {
                defer { self.resourceCleanup.complete(cleanupToken, succeeded: closed) }
                if grant.securityScoped, !grant.securityScopeStopped {
                    grant.url.stopAccessingSecurityScopedResource()
                    grant.securityScopeStopped = true
                }
                if self.activeFileWriteGrants[grant.id] === grant {
                    self.activeFileWriteGrants.removeValue(forKey: grant.id)
                }
                var closedEvent = self.fileWriteGrantTrace(grant)
                closedEvent["reason"] = reason
                closedEvent["closeSucceeded"] = closed
                closedEvent["securityScopeStopped"] = grant.securityScopeStopped
                closedEvent["nextOffset"] = grant.nextOffset
                trace("recv", "fileWriteGrant.closed", closedEvent)
                guard let operationToken else { return }
                guard self.isNativeOperationActive(operationToken) else {
                    self.traceDroppedAsyncCompletion(
                        command: command,
                        operationToken: operationToken)
                    return
                }
                let cancel = command == "fileGrant.cancelWrite"
                var result: [String: Any] = [
                    "success": closed, "command": command,
                    "operation": cancel ? "cancel-write-grant" : "release-write-grant",
                    "grantUnavailable": true, "grantState": "unavailable",
                    "securityScopeStopped": grant.securityScopeStopped,
                    "nextOffset": grant.nextOffset, "pathExposed": false
                ]
                result[cancel ? "canceled" : "released"] = closed
                if cancel { result["cancellationRequested"] = true }
                if !closed {
                    result["errorCode"] = "grant-close-failed"
                    result["error"] = "Selected file grant could not be closed"
                } else {
                    var released = self.fileWriteGrantTrace(grant)
                    released["nextOffset"] = grant.nextOffset
                    trace("recv", cancel ? "fileWriteGrant.canceled" : "fileWriteGrant.released", released)
                }
                self.completeNativeOperation(operationToken, result: result)
            }
        }
    }

    private func saveDownloadedFile(args: [String: Any],
                                    command: String,
                                    completion: @escaping ([String: Any]) -> Void) {
        let suggestedName = args["suggestedName"] as? String ?? "download.txt"
        let mimeType = args["mimeType"] as? String ?? "application/octet-stream"
        let bodyEncoding = args["bodyEncoding"] as? String ?? "utf-8"
        let body = args["body"] as? String ?? ""
        let bodyData = body.data(using: .utf8) ?? Data()
        let declaredByteCount = integerArg(args, "byteCount", fallback: bodyData.count)

        if activePanelOperation != nil {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-save-panel",
                "operation": "download",
                "suggestedName": suggestedName,
                "canceled": true,
                "bodyRedacted": true,
                "byteCount": bodyData.count,
                "declaredByteCount": declaredByteCount,
                "error": "File transfer panel already in progress"
            ])
            return
        }

        let panel = NSSavePanel()
        panel.title = args["title"] as? String ?? "Save File"
        panel.nameFieldStringValue = suggestedName
        panel.canCreateDirectories = true
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "mode": "native-appkit-save-panel",
                    "operation": "download",
                    "suggestedName": suggestedName,
                    "canceled": true,
                    "bodyRedacted": true,
                    "byteCount": bodyData.count,
                    "declaredByteCount": declaredByteCount,
                    "usedSavePanel": true,
                    "cancelReason": reason,
                    "operationInvalidated": true,
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        activePanelOperation = (
            token: operationToken,
            command: command,
            panel: panel)

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            guard let operation = self.activePanelOperation,
                  operation.token == operationToken,
                  operation.panel === panel else {
                if !self.consumeSuppressedPanelCompletion(operationToken) {
                    self.traceDroppedPanelCompletion(
                        command: command,
                        operationToken: operationToken)
                }
                return
            }
            self.activePanelOperation = nil
            guard response == .OK, let targetURL = panel.url else {
                self.completeNativeOperation(operationToken, result: [
                    "success": true,
                    "command": command,
                    "mode": "native-appkit-save-panel",
                    "operation": "download",
                    "suggestedName": suggestedName,
                    "canceled": true,
                    "bodyRedacted": true,
                    "byteCount": bodyData.count,
                    "declaredByteCount": declaredByteCount,
                    "usedSavePanel": true
                ])
                return
            }
            self.completeNativeOperation(
                operationToken,
                result: self.downloadWriteResult(
                    command: command,
                    mode: "native-appkit-save-panel",
                    targetURL: targetURL,
                    suggestedName: suggestedName,
                    mimeType: mimeType,
                    bodyEncoding: bodyEncoding,
                    bodyData: bodyData,
                    declaredByteCount: declaredByteCount,
                    usedSavePanel: true))
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func downloadWriteResult(command: String,
                                     mode: String,
                                     targetURL: URL,
                                     suggestedName: String,
                                     mimeType: String,
                                     bodyEncoding: String,
                                     bodyData: Data,
                                     declaredByteCount: Int,
                                     usedSavePanel: Bool) -> [String: Any] {
        do {
            try FileManager.default.createDirectory(at: targetURL.deletingLastPathComponent(),
                                                    withIntermediateDirectories: true)
            try bodyData.write(to: targetURL)
            return [
                "success": true,
                "command": command,
                "mode": mode,
                "operation": "download",
                "suggestedName": suggestedName,
                "mimeType": mimeType,
                "bodyEncoding": bodyEncoding,
                "bodyRedacted": true,
                "byteCount": bodyData.count,
                "declaredByteCount": declaredByteCount,
                "path": targetURL.path,
                "filename": targetURL.lastPathComponent,
                "usedSavePanel": usedSavePanel,
                "canceled": false
            ]
        } catch {
            return [
                "success": false,
                "command": command,
                "mode": mode,
                "operation": "download",
                "suggestedName": suggestedName,
                "bodyRedacted": true,
                "byteCount": bodyData.count,
                "declaredByteCount": declaredByteCount,
                "path": targetURL.path,
                "filename": targetURL.lastPathComponent,
                "usedSavePanel": usedSavePanel,
                "canceled": false,
                "error": error.localizedDescription
            ]
        }
    }

    private func selectUploadFile(args: [String: Any],
                                  command: String,
                                  completion: @escaping ([String: Any]) -> Void) {
        let title = args["title"] as? String ?? "Select File"
        let readBytesLimit = integerArg(args, "readBytesLimit", fallback: 64 * 1024)

        if activePanelOperation != nil {
            completion([
                "success": false,
                "command": command,
                "mode": "native-appkit-open-panel",
                "operation": "upload",
                "title": title,
                "canceled": true,
                "paths": [],
                "securityScoped": false,
                "bodyRedacted": true,
                "readBytesLimit": readBytesLimit,
                "error": "File transfer panel already in progress"
            ])
            return
        }

        let panel = NSOpenPanel()
        panel.title = title
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = false
        panel.canCreateDirectories = false
        let operationToken = beginNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: { reason in
                [
                    "success": false,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "operation": "upload",
                    "title": title,
                    "canceled": true,
                    "paths": [],
                    "securityScoped": false,
                    "bodyRedacted": true,
                    "readBytesLimit": readBytesLimit,
                    "usedOpenPanel": true,
                    "cancelReason": reason,
                    "operationInvalidated": true,
                    "error": Self.invalidationError(reason: reason)
                ]
            })
        activePanelOperation = (
            token: operationToken,
            command: command,
            panel: panel)

        let finish: (NSApplication.ModalResponse) -> Void = { response in
            guard let operation = self.activePanelOperation,
                  operation.token == operationToken,
                  operation.panel === panel else {
                if !self.consumeSuppressedPanelCompletion(operationToken) {
                    self.traceDroppedPanelCompletion(
                        command: command,
                        operationToken: operationToken)
                }
                return
            }
            self.activePanelOperation = nil
            guard response == .OK, let selectedURL = panel.url else {
                self.completeNativeOperation(operationToken, result: [
                    "success": true,
                    "command": command,
                    "mode": "native-appkit-open-panel",
                    "operation": "upload",
                    "title": title,
                    "canceled": true,
                    "paths": [],
                    "securityScoped": false,
                    "bodyRedacted": true,
                    "readBytesLimit": readBytesLimit,
                    "usedOpenPanel": true
                ])
                return
            }
            self.completeNativeOperation(
                operationToken,
                result: self.uploadSelectionResult(
                    command: command,
                    mode: "native-appkit-open-panel",
                    title: title,
                    selectedURL: selectedURL,
                    readBytesLimit: readBytesLimit,
                    usedOpenPanel: true))
        }
        if let window {
            panel.beginSheetModal(for: window, completionHandler: finish)
        } else {
            panel.begin(completionHandler: finish)
        }
    }

    private func uploadSelectionResult(command: String,
                                       mode: String,
                                       title: String,
                                       selectedURL: URL,
                                       readBytesLimit: Int,
                                       usedOpenPanel: Bool) -> [String: Any] {
        guard FileManager.default.fileExists(atPath: selectedURL.path) else {
            return [
                "success": false,
                "command": command,
                "mode": mode,
                "operation": "upload",
                "title": title,
                "canceled": false,
                "paths": [selectedURL.path],
                "path": selectedURL.path,
                "filename": selectedURL.lastPathComponent,
                "securityScoped": false,
                "bodyRedacted": true,
                "readBytesLimit": readBytesLimit,
                "usedOpenPanel": usedOpenPanel,
                "error": "Selected file does not exist"
            ]
        }

        let byteCount = fileByteCount(selectedURL)
        var sampledBytes = 0
        do {
            let handle = try FileHandle(forReadingFrom: selectedURL)
            defer {
                try? handle.close()
            }
            let sampleLimit = max(0, min(readBytesLimit, 64 * 1024))
            let sample = try handle.read(upToCount: sampleLimit)
            sampledBytes = sample?.count ?? 0
        } catch {
            return [
                "success": false,
                "command": command,
                "mode": mode,
                "operation": "upload",
                "title": title,
                "canceled": false,
                "paths": [selectedURL.path],
                "path": selectedURL.path,
                "filename": selectedURL.lastPathComponent,
                "byteCount": byteCount,
                "securityScoped": false,
                "bodyRedacted": true,
                "readBytesLimit": readBytesLimit,
                "usedOpenPanel": usedOpenPanel,
                "error": error.localizedDescription
            ]
        }

        return [
            "success": true,
            "command": command,
            "mode": mode,
            "operation": "upload",
            "title": title,
            "canceled": false,
            "paths": [selectedURL.path],
            "path": selectedURL.path,
            "filename": selectedURL.lastPathComponent,
            "byteCount": byteCount,
            "sampledBytes": sampledBytes,
            "securityScoped": false,
            "bodyRedacted": true,
            "readBytesLimit": readBytesLimit,
            "usedOpenPanel": usedOpenPanel
        ]
    }

    private func fileGrantUnavailableResult(command: String) -> [String: Any] {
        [
            "success": false,
            "command": command,
            "operation": command == "fileGrant.release"
                ? "release-grant"
                : "read-grant",
            "grantUnavailable": true,
            "grantState": "unavailable",
            "errorCode": "grant-unavailable",
            "error": "File grant is unavailable"
        ]
    }

    private func traceUnavailableFileGrant(
        command: String,
        grantId: String?,
        ownerScope: FlowIpcWindowScope,
        sidecarGeneration: Int
    ) {
        let activeGrant = grantId.flatMap { activeFileReadGrants[$0] }
        let retiredGrant = grantId.flatMap { retiredFileReadGrants[$0] }
        let reason: String
        if grantId == nil || grantId?.isBlank == true {
            reason = "invalid-token"
        } else if let retiredGrant {
            reason = retiredGrant.reason
        } else if let activeGrant,
                  activeGrant.ownerScope != ownerScope {
            reason = "owner-mismatch"
        } else if let activeGrant,
                  activeGrant.sidecarGeneration != sidecarGeneration {
            reason = "generation-mismatch"
        } else if activeGrant?.closing == true {
            reason = "closing"
        } else {
            reason = "unknown-token"
        }
        var payload: [String: Any] = [
            "command": command,
            "reason": reason,
            "sidecarGeneration": sidecarGeneration,
            "windowId": ownerScope.windowId,
            "sessionId": ownerScope.sessionId,
            "uiId": ownerScope.uiId
        ]
        if let activeGrant {
            payload["diagnosticId"] = activeGrant.diagnosticId
        } else if let retiredGrant {
            payload["diagnosticId"] = retiredGrant.diagnosticId
        }
        trace("recv", "fileReadGrant.accessDenied", payload)
    }

    private func rememberRetiredFileReadGrant(
        _ grant: ActiveFileReadGrant,
        reason: String
    ) {
        guard retiredFileReadGrants[grant.id] == nil else {
            return
        }
        retiredFileReadGrants[grant.id] = RetiredFileReadGrant(
            reason: reason,
            diagnosticId: grant.diagnosticId)
        retiredFileReadGrantOrder.append(grant.id)
        while retiredFileReadGrantOrder.count
                > Self.maxRetiredFileReadGrants {
            let evicted = retiredFileReadGrantOrder.removeFirst()
            retiredFileReadGrants.removeValue(forKey: evicted)
        }
    }

    private func scheduleFileReadGrantClose(
        _ grant: ActiveFileReadGrant,
        reason: String,
        releaseOperationToken: Int?
    ) {
        // Invalidation can race a previously scheduled explicit release. Stop
        // the App Sandbox access on the main thread even when the close work is
        // already queued, while retaining the one-close invariant below.
        if releaseOperationToken == nil, grant.securityScoped,
           !grant.securityScopeStopped {
            grant.url.stopAccessingSecurityScopedResource()
            grant.securityScopeStopped = true
        }
        guard !grant.closeScheduled else {
            return
        }
        grant.closeScheduled = true
        let cleanupToken = resourceCleanup.begin()
        let lifecycleEvent = reason == "released"
            ? "fileReadGrant.releaseScheduled"
            : "fileReadGrant.invalidated"
        trace("recv", lifecycleEvent, [
            "diagnosticId": grant.diagnosticId,
            "reason": reason,
            "readInFlight": grant.readInFlight,
            "securityScoped": grant.securityScoped,
            "sidecarGeneration": grant.sidecarGeneration,
            "windowId": grant.ownerScope.windowId,
            "sessionId": grant.ownerScope.sessionId,
            "uiId": grant.ownerScope.uiId
        ])

        grant.queue.async {
            let closeError: String?
            do {
                try grant.fileHandle.close()
                closeError = nil
            } catch {
                closeError = error.localizedDescription
            }
            NativeMainRunLoop.perform {
                defer {
                    self.resourceCleanup.complete(
                        cleanupToken,
                        succeeded: closeError == nil)
                }
                if grant.securityScoped, !grant.securityScopeStopped {
                    grant.url.stopAccessingSecurityScopedResource()
                    grant.securityScopeStopped = true
                }
                if self.activeFileReadGrants[grant.id] === grant {
                    self.activeFileReadGrants.removeValue(forKey: grant.id)
                }
                trace("recv", "fileReadGrant.closed", [
                    "diagnosticId": grant.diagnosticId,
                    "reason": reason,
                    "closeSucceeded": closeError == nil,
                    "securityScopeStopped": grant.securityScopeStopped,
                    "sidecarGeneration": grant.sidecarGeneration,
                    "windowId": grant.ownerScope.windowId
                ])
                guard let releaseOperationToken else {
                    return
                }
                let result: [String: Any]
                if closeError != nil {
                    result = [
                        "success": false,
                        "command": "fileGrant.release",
                        "operation": "release-grant",
                        "released": false,
                        "grantUnavailable": true,
                        "grantState": "unavailable",
                        "errorCode": "grant-close-failed",
                        "error": "Selected file grant could not be closed"
                    ]
                } else {
                    result = [
                        "success": true,
                        "command": "fileGrant.release",
                        "operation": "release-grant",
                        "released": true,
                        "grantUnavailable": true,
                        "grantState": "unavailable",
                        "securityScopeStopped": grant.securityScopeStopped
                    ]
                }
                self.completeNativeOperation(
                    releaseOperationToken,
                    result: result)
            }
        }
    }

    func redactedTransferPath(_ path: String) -> String {
        if path.isBlank {
            return ""
        }
        return URL(fileURLWithPath: path).lastPathComponent
    }

    private func fileByteCount(_ url: URL) -> Int64 {
        guard let attributes = try? FileManager.default.attributesOfItem(atPath: url.path),
              let size = attributes[.size] as? NSNumber else {
            return 0
        }
        return size.int64Value
    }

    private func integerArg(_ args: [String: Any], _ key: String, fallback: Int) -> Int {
        if let value = args[key] as? Int {
            return value
        }
        if let value = args[key] as? NSNumber {
            return value.intValue
        }
        return fallback
    }

    private func exactIntegerArg(
        _ args: [String: Any],
        _ key: String
    ) -> Int64? {
        guard let number = args[key] as? NSNumber,
              CFGetTypeID(number) != CFBooleanGetTypeID() else {
            return nil
        }
        let type = String(cString: number.objCType)
        guard type != "f", type != "d" else {
            return nil
        }
        let value = number.int64Value
        guard NSNumber(value: value).compare(number) == .orderedSame else {
            return nil
        }
        return value
    }

    private func nextOperationToken() -> Int {
        operationSequence &+= 1
        return operationSequence
    }

    private func beginNativeOperation(
        command: String,
        completion: @escaping ([String: Any]) -> Void,
        invalidationResult: @escaping (String) -> [String: Any]
    ) -> Int {
        let operationToken = nextOperationToken()
        activeNativeOperations[operationToken] = ActiveNativeOperation(
            command: command,
            completion: completion,
            invalidationResult: invalidationResult)
        return operationToken
    }

    private func completeNativeOperation(
        _ operationToken: Int,
        result: [String: Any]
    ) {
        guard let operation = activeNativeOperations.removeValue(
            forKey: operationToken) else {
            traceDroppedAsyncCompletion(
                command: result["command"] as? String ?? "unknown",
                operationToken: operationToken)
            return
        }
        notificationIdentifiersByOperation.removeValue(
            forKey: operationToken)
        operation.completion(result)
    }

    private func isNativeOperationActive(_ operationToken: Int) -> Bool {
        activeNativeOperations[operationToken] != nil
    }

    private func consumeSuppressedPanelCompletion(
        _ operationToken: Int
    ) -> Bool {
        suppressedPanelCompletionTokens.remove(operationToken) != nil
    }

    private static func invalidationError(reason: String) -> String {
        switch reason {
        case "owner-window-closed":
            return "Owner window closed before native capability completed"
        case "host-shutdown":
            return "Native host shut down before native capability completed"
        case "sidecar-replacement":
            return "Sidecar replacement invalidated the native capability operation"
        case "sidecar-exit":
            return "Sidecar exit invalidated the native capability operation"
        default:
            return "Native capability operation was invalidated: \(reason)"
        }
    }

    private func traceDroppedPanelCompletion(
        command: String,
        operationToken: Int
    ) {
        trace("recv", "nativeCapabilityOperation.completionDropped", [
            "command": command,
            "operationToken": operationToken,
            "reason": "operation-invalidated",
            "windowId": windowId
        ])
    }

    private func traceDroppedAsyncCompletion(
        command: String,
        operationToken: Int
    ) {
        trace("recv", "nativeCapabilityOperation.completionDropped", [
            "command": command,
            "operationToken": operationToken,
            "reason": "operation-invalidated",
            "windowId": windowId
        ])
    }

    private func capabilityPermission(_ result: [String: Any],
                                      decision: String = "allowed",
                                      reason: String? = nil) -> [String: Any] {
        var payload = result
        payload["permissionMode"] = capabilityManifest.permissionMode
        payload["permissionDecision"] = decision
        payload["permissionReason"] = reason ?? capabilityManifest.allowedReason(
            result["command"] as? String ?? "", windowId: windowId)
        payload["permissionScope"] = "window:\(windowId)"
        payload["permissionManifestLoaded"] = capabilityManifest.loaded
        payload["permissionManifestPath"] = capabilityManifest.path
        payload["permissionManifestCommands"] = capabilityManifest
            .commandsByWindow[windowId, default: []]
            .sorted()
            .joined(separator: ",")
        return payload
    }


}
