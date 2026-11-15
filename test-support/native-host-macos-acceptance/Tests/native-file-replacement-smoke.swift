// Appended to an unchanged copy of DesktopCommandExecutor.swift in a generated
// test build, so private grant setup needs no production test hooks or OS panel.
extension DesktopCommandExecutor {
    static func replacementRaceSmoke(config: HostConfig) throws {
        let scope = FlowIpcWindowScope(windowId: "main", sessionId: "test-session", uiId: "main")
        let unsupportedCleanup = NativeResourceCleanupTracker()
        let unsupported = DesktopCommandExecutor(config: config,
            capabilityManifest: config.capabilityManifest, resourceCleanup: unsupportedCleanup)
        var unsupportedResults: [[String: Any]] = []
        unsupported.execute("fileGrant.resume", args: [:], ownerScope: scope, sidecarGeneration: 1) {
            unsupportedResults.append($0)
        }
        precondition(unsupportedResults.count == 1 && unsupportedResults[0]["success"] as? Bool == false)
        precondition(unsupportedResults[0]["permissionReason"] as? String == "unsupported-command")
        precondition(unsupported.activeNativeOperations.isEmpty && unsupportedCleanup.pendingCount == 0)
        print("PASS unsupported resume remains rejected despite manifest permission")
        for write in [false, true] {
            for workerReturned in [false, true] {
                let directory = FileManager.default.temporaryDirectory.appendingPathComponent("vatori-native-race-" + UUID().uuidString)
                try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: false)
                defer { try? FileManager.default.removeItem(at: directory) }
                let file = directory.appendingPathComponent("owned-test.bin")
                let bytes = Data(repeating: 73, count: 65536)
                try (write ? Data() : bytes).write(to: file)
                let cleanup = NativeResourceCleanupTracker()
                let executor = DesktopCommandExecutor(config: config,
                    capabilityManifest: config.capabilityManifest, resourceCleanup: cleanup)
                let queue: DispatchQueue
                let grantId: String
                let descriptor: Int32
                if write {
                    descriptor = Darwin.open(file.path, O_WRONLY | O_CLOEXEC)
                    precondition(descriptor >= 0)
                    let grant = ActiveFileWriteGrant(diagnosticId: 1, ownerScope: scope,
                        sidecarGeneration: 1, url: file, descriptor: descriptor, securityScoped: false)
                    executor.activeFileWriteGrants[grant.id] = grant
                    queue = grant.queue; grantId = grant.id
                } else {
                    let handle = try FileHandle(forReadingFrom: file)
                    descriptor = handle.fileDescriptor
                    let grant = ActiveFileReadGrant(id: UUID().uuidString.lowercased(), diagnosticId: 1,
                        ownerScope: scope, sidecarGeneration: 1, url: file, filename: file.lastPathComponent,
                        byteCount: UInt64(bytes.count), fileHandle: handle, securityScoped: false)
                    executor.activeFileReadGrants[grant.id] = grant
                    queue = grant.queue; grantId = grant.id
                }
                let releaseWorker = DispatchSemaphore(value: 0)
                if !workerReturned { queue.async { releaseWorker.wait() } }
                var results: [[String: Any]] = []
                let command = write ? "fileGrant.write" : "fileGrant.read"
                let args: [String: Any] = write
                    ? ["grantId": grantId, "offset": 0, "bodyBase64": bytes.base64EncodedString()]
                    : ["grantId": grantId, "offset": 0, "maxBytes": bytes.count]
                // Use the real executor entry and async worker; only authorization
                // setup is synthetic and never presented as Sandbox evidence.
                executor.execute(command, args: args, ownerScope: scope, sidecarGeneration: 1) { results.append($0) }
                precondition(results.isEmpty && executor.activeNativeOperations.count == 1)
                if workerReturned { queue.sync {} }
                executor.invalidateNativeOperations(reason: "sidecar-replacement")
                precondition(results.count == 1 && results[0]["success"] as? Bool == false)
                precondition(results[0]["operationInvalidated"] as? Bool == true)
                precondition(results[0]["cancelReason"] as? String == "sidecar-replacement")
                precondition(results[0]["bodyBase64"] == nil && results[0]["bytesWritten"] == nil)
                precondition(executor.activeNativeOperations.isEmpty && cleanup.pendingCount == 1)
                executor.invalidateNativeOperations(reason: "sidecar-replacement")
                precondition(results.count == 1 && cleanup.pendingCount == 1)
                if !workerReturned { releaseWorker.signal() }
                var drain: NativeResourceCleanupTracker.DrainResult?
                cleanup.drain(timeout: 5) { drain = $0 }
                let deadline = Date().addingTimeInterval(6)
                while drain == nil && Date() < deadline { RunLoop.current.run(until: Date().addingTimeInterval(0.01)) }
                precondition(drain?.drained == true && drain?.pendingCount == 0 && drain?.failureCount == 0)
                precondition(results.count == 1, "late completion must not report success")
                precondition(fcntl(descriptor, F_GETFD) == -1 && errno == EBADF)
                precondition(executor.activeFileReadGrants.isEmpty && executor.activeFileWriteGrants.isEmpty)
                print("PASS replacement \(write ? "write" : "read") workerReturned=\(workerReturned): one terminal, drained, descriptor closed")
            }
        }
    }
}

@main
struct NativeFileReplacementSmoke {
    static func main() throws {
        let resource = URL(fileURLWithPath: CommandLine.arguments[1])
        try DesktopCommandExecutor.replacementRaceSmoke(config: HostConfig.load(resourceURL: resource, environment: [:]))
    }
}
