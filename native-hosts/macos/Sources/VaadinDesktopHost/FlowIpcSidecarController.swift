import Cocoa
import Foundation
import WebKit

private struct SidecarReplacementRequest {
    let requestId: String
    let reason: String
    let previousProcessIdentifier: Int32
}

private enum SidecarReplacementPhase {
    case terminatingCurrent(SidecarReplacementRequest)
    case candidateRunning(SidecarReplacementRequest)
    case terminatingFailedCandidate(
        SidecarReplacementRequest,
        cause: String,
        rollbackRequested: Bool)
    case awaitingRollback(
        SidecarReplacementRequest,
        cause: String)
    case rollbackRunning(
        SidecarReplacementRequest,
        cause: String)
}

final class FlowIpcSidecarController {
    private let config: HostConfig
    private var activeConfiguration: HostConfig
    private let replacementRecords: RuntimeReplacementRecords
    private let onProductAssetsReady: (HostConfig) -> Void
    private var productReplacement: (id: UUID, configuration: HostConfig)?
    private let flowIpcWindows: () -> [FlowIpcWindowContext]
    private let terminateApplication: () -> Void
    private var sidecar: JavaSidecar!
    private var initialReadyPending = false
    private var hostShutdownRequested = false
    private var recoveryInProgress = false
    private var replacementPhase: SidecarReplacementPhase?
    private var pendingReplacement: SidecarReplacementRequest?
    private var restartAttempts = 0
    private var exitedProcessIdentifier: Int32?
    private let onLifecycleReady: () -> Void
    private let developmentReplacementResult: (
        String,
        HostControlReplacementStatus,
        String
    ) -> Void

    init(
        config: HostConfig,
        replacementRecords: RuntimeReplacementRecords = RuntimeReplacementRecords(),
        onProductAssetsReady: @escaping (HostConfig) -> Void = { _ in },
        flowIpcWindows: @escaping () -> [FlowIpcWindowContext],
        onSidecarEnvelope: @escaping (String, [String: Any], Int) -> Void,
        onLifecycleReady: @escaping () -> Void,
        onReplacementResult: @escaping (
            String,
            HostControlReplacementStatus,
            String
        ) -> Void,
        terminateApplication: @escaping () -> Void
    ) {
        self.config = config
        self.activeConfiguration = config
        self.replacementRecords = replacementRecords
        self.onProductAssetsReady = onProductAssetsReady
        self.flowIpcWindows = flowIpcWindows
        self.terminateApplication = terminateApplication
        self.onLifecycleReady = onLifecycleReady
        self.developmentReplacementResult = onReplacementResult
        self.sidecar = JavaSidecar(
            config: config,
            onSidecarEnvelope: onSidecarEnvelope,
            onTermination: { [weak self] termination in
                self?.handleTermination(termination)
            },
            onFatalError: { [weak self] cause in
                self?.handleSidecarFatalError(cause: cause)
            })
    }

    @discardableResult
    func start() -> Bool {
        guard !hostShutdownRequested else {
            return false
        }
        guard sidecar.start() else {
            trace("send", "error", [
                "code": "sidecarStartFailed",
                "cause": "initial-process-run-failed"
            ])
            return false
        }
        initialReadyPending = true
        scheduleInitialReadyTimeout()
        return true
    }

    func prepareForHostShutdown() {
        guard !hostShutdownRequested else {
            return
        }
        hostShutdownRequested = true
        replacementRecords.shutdown()
        productReplacement = nil
        initialReadyPending = false
        recoveryInProgress = false
        replacementPhase = nil
        pendingReplacement = nil
        invalidateNativeOperations(
            flowIpcWindows(),
            reason: "host-shutdown")
    }

    func stopForHostShutdown(
        completion: @escaping (Result<Void, JavaSidecarShutdownFailure>) -> Void
    ) {
        prepareForHostShutdown()
        sidecar.stopForHostShutdown(completion: completion)
    }

    @discardableResult
    func sendSidecarEnvelope(_ envelope: String) -> Bool {
        guard !hostShutdownRequested,
              !initialReadyPending,
              !recoveryInProgress,
              replacementPhase == nil else {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "lifecycle-unavailable",
                "hostShutdownRequested": hostShutdownRequested,
                "initialReadyPending": initialReadyPending,
                "recoveryInProgress": recoveryInProgress,
                "replacementInProgress": replacementPhase != nil
            ])
            return false
        }
        return sidecar.sendSidecarEnvelope(envelope)
    }

    @discardableResult
    func sendSidecarEnvelope(
        _ envelope: String,
        expectedGeneration: Int
    ) -> Bool {
        guard !hostShutdownRequested,
              !initialReadyPending,
              !recoveryInProgress,
              replacementPhase == nil else {
            trace("send", "flow-ipc.sidecarWrite", [
                "target": "java-sidecar",
                "status": "lifecycle-unavailable",
                "hostShutdownRequested": hostShutdownRequested,
                "expectedGeneration": expectedGeneration,
                "initialReadyPending": initialReadyPending,
                "recoveryInProgress": recoveryInProgress,
                "replacementInProgress": replacementPhase != nil
            ])
            return false
        }
        return sidecar.sendSidecarEnvelope(
            envelope,
            expectedGeneration: expectedGeneration)
    }

    var processIdentifier: Int32? {
        sidecar.processIdentifier
    }

    var isProductReplacementInProgress: Bool { productReplacement != nil }

    func requestProductReplacement(owner: UUID, requestId: String,
                                   candidateId: String, expectedGeneration: Int) -> [String: Any] {
        guard config.transport == .flowIpc, config.flowWebAppSource?.isDevelopment == false,
              canExecuteSidecarCommand(expectedGeneration: expectedGeneration),
              flowIpcWindows().contains(where: { $0.lifetimeId == owner }) else {
            return ["success": false, "error": "Runtime is unavailable for product replacement"]
        }
        let candidate: HostConfig
        do { candidate = try activeConfiguration.configuration(forRuntimeCandidate: candidateId) }
        catch { return ["success": false, "error": "Candidate preflight failed", "errorCode": "invalid-candidate"] }
        switch replacementRecords.begin(owner: owner, requestId: requestId, candidateId: candidateId,
                                         requestingGeneration: expectedGeneration) {
        case .rejected(let reason):
            return ["success": false, "error": "Replacement request rejected", "errorCode": reason.rawValue]
        case .duplicate(let record):
            return ["success": true, "operationId": record.operationId.uuidString,
                    "status": record.outcome?.rawValue ?? "pending", "duplicate": true]
        case .accepted(let record):
            productReplacement = (record.operationId, candidate)
            // Leave the capability result path available for this main-loop
            // iteration. The returned receipt is not a terminal result.
            DispatchQueue.main.async { [weak self] in
                guard let self, self.productReplacement?.id == record.operationId else { return }
                guard self.canExecuteSidecarCommand(expectedGeneration: expectedGeneration),
                      self.flowIpcWindows().contains(where: { $0.lifetimeId == owner }),
                      let pid = self.sidecar.processIdentifier else {
                    self.replacementRecords.finish(operationId: record.operationId, outcome: .interrupted)
                    self.productReplacement = nil
                    return
                }
                let request = SidecarReplacementRequest(requestId: record.operationId.uuidString,
                    reason: "product-replacement", previousProcessIdentifier: pid)
                self.replacementPhase = .terminatingCurrent(request)
                self.exitedProcessIdentifier = pid
                let windows = self.flowIpcWindows()
                self.disconnectBrowserBridges(windows)
                self.invalidateNativeOperations(windows, reason: "sidecar-replacement")
                trace("recv", "sidecar.productReplacementRequested", [
                    "operationId": record.operationId.uuidString, "requestId": requestId,
                    "candidateId": candidateId, "processIdentifier": pid,
                    "requestingGeneration": expectedGeneration])
                guard self.sidecar.terminateForReplacement(reason: request.reason) else {
                    self.replacementRecords.finish(operationId: record.operationId, outcome: .retirementFailed)
                    self.productReplacement = nil
                    self.terminateApplication()
                    return
                }
            }
            return ["success": true, "operationId": record.operationId.uuidString,
                    "status": "pending", "duplicate": false]
        }
    }

    private func onReplacementResult(_ requestId: String, _ status: HostControlReplacementStatus,
                                     _ cause: String) {
        guard let product = productReplacement, product.id.uuidString == requestId else {
            developmentReplacementResult(requestId, status, cause)
            return
        }
        if status == .failed {
            // Candidate failure is not terminal until rollback settles. Wait
            // for exact child exit through the existing retirement state.
            DispatchQueue.main.async { [weak self] in
                guard self?.productReplacement?.id == product.id else { return }
                _ = self?.requestRollback(requestId: requestId, reason: "product-rollback")
            }
            return
        }
        let outcome: RuntimeReplacementRecords.Outcome
        switch status {
        case .ready: outcome = .replaced
        case .rolledBack: outcome = .rolledBack
        case .rollbackFailed: outcome = .rollbackFailed
        case .failed: return
        }
        replacementRecords.finish(operationId: product.id, outcome: outcome)
        trace("recv", "sidecar.productReplacementResult", [
            "operationId": requestId, "status": outcome.rawValue, "cause": cause])
        productReplacement = nil
    }

    @discardableResult
    func requestReplacement(
        requestId: String,
        reason: String
    ) -> Bool {
        guard !hostShutdownRequested else {
            trace("recv", "sidecar.replacementRejected", [
                "requestId": requestId,
                "reason": reason,
                "cause": "host-shutdown"
            ])
            onReplacementResult(requestId, .failed, "host-shutdown")
            return false
        }
        let windows = flowIpcWindows()
        guard config.transport == .flowIpc,
              config.flowWebAppSource?.isDevelopment == true else {
            trace("recv", "sidecar.replacementRejected", [
                "requestId": requestId,
                "reason": reason,
                "cause": "unsupported-mode"
            ])
            onReplacementResult(
                requestId, .failed, "unsupported-mode")
            return false
        }
        guard replacementPhase == nil else {
            trace("recv", "sidecar.replacementRejected", [
                "requestId": requestId,
                "reason": reason,
                "cause": "replacement-busy"
            ])
            onReplacementResult(
                requestId, .failed, "replacement-busy")
            return false
        }
        guard !recoveryInProgress else {
            let request = SidecarReplacementRequest(
                requestId: requestId,
                reason: reason,
                previousProcessIdentifier:
                    sidecar.processIdentifier ?? 0)
            guard pendingReplacement == nil else {
                trace("recv", "sidecar.replacementRejected", [
                    "requestId": requestId,
                    "reason": reason,
                    "cause": "replacement-queued"
                ])
                onReplacementResult(
                    requestId, .failed, "replacement-queued")
                return false
            }
            pendingReplacement = request
            trace("recv", "sidecar.replacementQueued", [
                "requestId": requestId,
                "reason": reason,
                "recoveryInProgress": true,
                "windowCount": windows.count
            ])
            return true
        }
        guard !windows.isEmpty,
              let processIdentifier = sidecar.processIdentifier else {
            trace("recv", "sidecar.replacementRejected", [
                "requestId": requestId,
                "reason": reason,
                "cause": "runtime-not-ready",
                "windowCount": windows.count
            ])
            onReplacementResult(
                requestId, .failed, "runtime-not-ready")
            return false
        }

        let request = SidecarReplacementRequest(
            requestId: requestId,
            reason: reason,
            previousProcessIdentifier: processIdentifier)
        initialReadyPending = false
        replacementPhase = .terminatingCurrent(request)
        exitedProcessIdentifier = processIdentifier
        disconnectBrowserBridges(windows)
        trace("recv", "sidecar.replacementRequested", [
            "requestId": requestId,
            "reason": reason,
            "processIdentifier": processIdentifier,
            "windowCount": windows.count
        ])
        guard sidecar.terminateForReplacement(reason: reason) else {
            replacementPhase = nil
            exitedProcessIdentifier = nil
            onReplacementResult(
                requestId, .failed, "termination-failed")
            return false
        }
        invalidateNativeOperations(
            windows,
            reason: "sidecar-replacement")
        return true
    }

    func canExecuteSidecarCommand(
        expectedGeneration: Int
    ) -> Bool {
        !hostShutdownRequested
            && !initialReadyPending
            && !recoveryInProgress
            && replacementPhase == nil
            && sidecar.isActive(generation: expectedGeneration)
    }

    @discardableResult
    func requestRollback(
        requestId: String,
        reason: String
    ) -> Bool {
        guard !hostShutdownRequested else {
            trace("recv", "sidecar.rollbackRejected", [
                "requestId": requestId,
                "reason": reason,
                "cause": "host-shutdown"
            ])
            onReplacementResult(requestId, .rollbackFailed, "host-shutdown")
            return false
        }
        switch replacementPhase {
        case .awaitingRollback(let request, let cause)
                where request.requestId == requestId:
            trace("recv", "sidecar.rollbackRequested", [
                "requestId": requestId,
                "reason": reason,
                "cause": cause
            ])
            startRollback(request: request, cause: cause)
            return true
        case .terminatingFailedCandidate(
                let request,
                let cause,
                rollbackRequested: false)
                where request.requestId == requestId:
            replacementPhase = .terminatingFailedCandidate(
                request,
                cause: cause,
                rollbackRequested: true)
            trace("recv", "sidecar.rollbackRequested", [
                "requestId": requestId,
                "reason": reason,
                "cause": cause,
                "waitingForCandidateExit": true
            ])
            return true
        default:
            trace("recv", "sidecar.rollbackRejected", [
                "requestId": requestId,
                "reason": reason,
                "cause": "rollback-not-expected"
            ])
            onReplacementResult(
                requestId, .rollbackFailed,
                "rollback-not-expected")
            return false
        }
    }

    func handleReady() -> Bool {
        guard !hostShutdownRequested else {
            return true
        }
        initialReadyPending = false
        if case .candidateRunning(let request) = replacementPhase {
            completeReplacement(request: request)
            return true
        }
        if case .rollbackRunning(let request, let cause) = replacementPhase {
            completeRollback(request: request, cause: cause)
            return true
        }
        guard recoveryInProgress else {
            return false
        }
        completeRecovery()
        return true
    }

    @discardableResult
    func handleReadyFailure(cause: String) -> Bool {
        guard !hostShutdownRequested else {
            return true
        }
        if case .rollbackRunning(let request, _) = replacementPhase {
            onReplacementResult(request.requestId, .rollbackFailed, cause)
            terminateApplication()
            return true
        }
        guard case .candidateRunning(let request) = replacementPhase else {
            return false
        }
        failRunningCandidate(
            request: request,
            cause: cause)
        return true
    }

    private func handleSidecarFatalError(
        cause: String
    ) {
        guard !hostShutdownRequested else {
            return
        }
        if case .candidateRunning(let request) =
                replacementPhase {
            failRunningCandidate(
                request: request,
                cause: cause)
            return
        }
        if case .rollbackRunning(let request, let replacementFailure) =
                replacementPhase {
            replacementPhase = nil
            trace("send", "error", [
                "code": "sidecarRollbackProtocolFailure",
                "requestId": request.requestId,
                "cause": cause,
                "replacementFailure": replacementFailure
            ])
            onReplacementResult(
                request.requestId,
                .rollbackFailed,
                cause)
            terminateApplication()
            return
        }
        if let product = productReplacement {
            let outcome: RuntimeReplacementRecords.Outcome
            if case .terminatingCurrent = replacementPhase { outcome = .retirementFailed }
            else { outcome = .rollbackFailed }
            replacementRecords.finish(operationId: product.id, outcome: outcome)
            productReplacement = nil
        }
        trace("send", "error", [
            "code": cause == "unsupported-sidecar-version"
                ? "unsupportedSidecarVersion" : "sidecarTransportFailure",
            "cause": cause
        ])
        terminateApplication()
    }

    private func handleTermination(_ termination: JavaSidecarTermination) {
        guard !hostShutdownRequested else {
            return
        }
        initialReadyPending = false
        guard !termination.expected else {
            return
        }
        let windows = flowIpcWindows()
        invalidateNativeOperations(
            windows,
            reason: replacementPhase == nil
                ? "sidecar-exit"
                : "sidecar-replacement")
        switch replacementPhase {
        case .terminatingCurrent(let request):
            exitedProcessIdentifier = termination.processIdentifier
            startReplacement(request: request)
            return
        case .candidateRunning(let request):
            failStoppedCandidate(
                request: request,
                cause: "candidate-exited-before-ready",
                termination: termination)
            return
        case .terminatingFailedCandidate(
                let request,
                let cause,
                let rollbackRequested):
            if rollbackRequested {
                startRollback(request: request, cause: cause)
            } else {
                replacementPhase = .awaitingRollback(
                    request, cause: cause)
            }
            return
        case .awaitingRollback:
            return
        case .rollbackRunning(let request, let cause):
            trace("send", "sidecar.rollbackFailed", [
                "requestId": request.requestId,
                "reason": request.reason,
                "cause": "rollback-exited-before-ready",
                "replacementFailure": cause,
                "processIdentifier": termination.processIdentifier,
                "status": termination.status
            ])
            replacementPhase = nil
            onReplacementResult(
                request.requestId,
                .rollbackFailed,
                "rollback-exited-before-ready")
            terminateApplication()
            return
        case nil:
            break
        }
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
            disconnectBrowserBridges(windows)
        }
        scheduleRecovery()
    }

    private func scheduleInitialReadyTimeout() {
        let processIdentifier = sidecar.processIdentifier
        let timeout = config.sidecarInitialReadyTimeoutSeconds
        trace("recv", "sidecar.initialReadyDeadline", [
            "processIdentifier": processIdentifier ?? 0,
            "timeoutSeconds": timeout
        ])
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { [weak self] in
            guard let self,
                  !self.hostShutdownRequested,
                  self.initialReadyPending,
                  !self.recoveryInProgress,
                  self.replacementPhase == nil,
                  self.sidecar.processIdentifier == processIdentifier else {
                return
            }
            self.initialReadyPending = false
            trace("send", "error", [
                "code": "sidecarInitialReadyTimeout",
                "processIdentifier": processIdentifier ?? 0,
                "timeoutSeconds": timeout
            ])
            self.terminateApplication()
        }
    }

    private func startReplacement(
        request: SidecarReplacementRequest
    ) {
        let windows = flowIpcWindows()
        guard !hostShutdownRequested,
              case .terminatingCurrent(let activeRequest) = replacementPhase,
              activeRequest.requestId == request.requestId,
              !windows.isEmpty else {
            return
        }
        trace("send", "sidecar.replacementStarted", [
            "requestId": request.requestId,
            "reason": request.reason,
            "previousProcessIdentifier":
                request.previousProcessIdentifier
        ])
        guard sidecar.start(configuration: productReplacement?.configuration ?? activeConfiguration) else {
            replacementPhase = .awaitingRollback(
                request, cause: "candidate-start-failed")
            trace("send", "sidecar.replacementFailed", [
                "requestId": request.requestId,
                "reason": request.reason,
                "cause": "candidate-start-failed"
            ])
            onReplacementResult(
                request.requestId,
                .failed,
                "candidate-start-failed")
            return
        }
        replacementPhase = .candidateRunning(request)
        scheduleReplacementReadyTimeout(request: request)
    }

    private func scheduleReplacementReadyTimeout(
        request: SidecarReplacementRequest
    ) {
        let processIdentifier = sidecar.processIdentifier
        let timeout = config.flowIpcSidecarRecoveryReadyTimeoutSeconds
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { [weak self] in
            guard let self,
                  !self.hostShutdownRequested,
                  case .candidateRunning(let activeRequest) =
                    self.replacementPhase,
                  activeRequest.requestId == request.requestId,
                  self.sidecar.processIdentifier == processIdentifier else {
                return
            }
            trace("send", "sidecar.replacementReadyTimeout", [
                "requestId": request.requestId,
                "processIdentifier": processIdentifier ?? 0,
                "timeoutSeconds": timeout
            ])
            self.failRunningCandidate(
                request: request,
                cause: "candidate-ready-timeout")
        }
    }

    private func scheduleRecovery() {
        let windows = flowIpcWindows()
        guard !hostShutdownRequested,
              recoveryInProgress, !windows.isEmpty else {
            return
        }
        guard restartAttempts < config.flowIpcSidecarRestartLimit else {
            trace("send", "error", [
                "code": "sidecarRecoveryExhausted",
                "attempts": restartAttempts,
                "restartLimit": config.flowIpcSidecarRestartLimit,
                "stableRunSeconds":
                    config.flowIpcSidecarRecoveryStableRunSeconds
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
            "restartLimit": config.flowIpcSidecarRestartLimit,
            "previousProcessIdentifier": exitedProcessIdentifier ?? 0,
            "windowCount": windows.count
        ])
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            guard let self,
                  !self.hostShutdownRequested,
                  self.recoveryInProgress,
                  !self.flowIpcWindows().isEmpty else {
                return
            }
            trace("send", "sidecar.recoveryStarted", [
                "attempt": attempt,
                "previousProcessIdentifier": self.exitedProcessIdentifier ?? 0
            ])
            guard self.sidecar.start(configuration: self.activeConfiguration) else {
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
                  !self.hostShutdownRequested,
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
        let reload = reloadWindows()
        trace("recv", "sidecar.recoveryReady", [
            "attempt": restartAttempts,
            "previousProcessIdentifier": previousProcessIdentifier,
            "processIdentifier": currentProcessIdentifier,
            "processReplaced": previousProcessIdentifier > 0
                && currentProcessIdentifier > 0
                && previousProcessIdentifier != currentProcessIdentifier,
            "windowsReloaded": reload.windowCount,
            "recoveryGeneration": reload.highestGeneration
        ])
        recoveryInProgress = false
        exitedProcessIdentifier = nil
        onLifecycleReady()
        scheduleRecoveryBudgetResetIfNeeded()
        startPendingReplacementIfNeeded()
    }

    private func completeReplacement(
        request: SidecarReplacementRequest
    ) {
        if let product = productReplacement, product.id.uuidString == request.requestId {
            activeConfiguration = product.configuration
            onProductAssetsReady(activeConfiguration)
        }
        let currentProcessIdentifier = sidecar.processIdentifier ?? 0
        let reload = reloadWindows()
        trace("recv", "sidecar.replacementReady", [
            "requestId": request.requestId,
            "reason": request.reason,
            "previousProcessIdentifier":
                request.previousProcessIdentifier,
            "processIdentifier": currentProcessIdentifier,
            "processReplaced":
                request.previousProcessIdentifier > 0
                && currentProcessIdentifier > 0
                && request.previousProcessIdentifier
                    != currentProcessIdentifier,
            "windowsReloaded": reload.windowCount,
            "recoveryGeneration": reload.highestGeneration
        ])
        replacementPhase = nil
        exitedProcessIdentifier = nil
        onLifecycleReady()
        scheduleRecoveryBudgetResetIfNeeded()
        onReplacementResult(
            request.requestId, .ready, "candidate-ready")
        startPendingReplacementIfNeeded()
    }

    private func startPendingReplacementIfNeeded() {
        guard !hostShutdownRequested,
              let request = pendingReplacement else {
            return
        }
        pendingReplacement = nil
        DispatchQueue.main.async { [weak self] in
            _ = self?.requestReplacement(
                requestId: request.requestId,
                reason: request.reason)
        }
    }

    private func failRunningCandidate(
        request: SidecarReplacementRequest,
        cause: String
    ) {
        replacementPhase = .terminatingFailedCandidate(
            request,
            cause: cause,
            rollbackRequested: false)
        trace("send", "sidecar.replacementFailed", [
            "requestId": request.requestId,
            "reason": request.reason,
            "cause": cause,
            "processIdentifier": sidecar.processIdentifier ?? 0
        ])
        onReplacementResult(
            request.requestId, .failed, cause)
        guard sidecar.terminateForReplacement(reason: cause) else {
            replacementPhase = .awaitingRollback(
                request, cause: cause)
            return
        }
    }

    private func failStoppedCandidate(
        request: SidecarReplacementRequest,
        cause: String,
        termination: JavaSidecarTermination
    ) {
        replacementPhase = .awaitingRollback(
            request, cause: cause)
        trace("send", "sidecar.replacementFailed", [
            "requestId": request.requestId,
            "reason": request.reason,
            "cause": cause,
            "processIdentifier": termination.processIdentifier,
            "status": termination.status
        ])
        onReplacementResult(
            request.requestId, .failed, cause)
    }

    private func startRollback(
        request: SidecarReplacementRequest,
        cause: String
    ) {
        guard !hostShutdownRequested else {
            return
        }
        replacementPhase = .rollbackRunning(
            request, cause: cause)
        trace("send", "sidecar.rollbackStarted", [
            "requestId": request.requestId,
            "reason": request.reason,
            "replacementFailure": cause
        ])
        guard sidecar.start(configuration: activeConfiguration) else {
            replacementPhase = nil
            trace("send", "sidecar.rollbackFailed", [
                "requestId": request.requestId,
                "reason": request.reason,
                "cause": "rollback-start-failed",
                "replacementFailure": cause
            ])
            onReplacementResult(
                request.requestId,
                .rollbackFailed,
                "rollback-start-failed")
            terminateApplication()
            return
        }
        scheduleRollbackReadyTimeout(
            request: request,
            replacementFailure: cause)
    }

    private func scheduleRollbackReadyTimeout(
        request: SidecarReplacementRequest,
        replacementFailure: String
    ) {
        let processIdentifier = sidecar.processIdentifier
        let timeout = config.flowIpcSidecarRecoveryReadyTimeoutSeconds
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout) { [weak self] in
            guard let self,
                  !self.hostShutdownRequested,
                  case .rollbackRunning(let activeRequest, _) =
                    self.replacementPhase,
                  activeRequest.requestId == request.requestId,
                  self.sidecar.processIdentifier == processIdentifier else {
                return
            }
            self.replacementPhase = nil
            trace("send", "sidecar.rollbackFailed", [
                "requestId": request.requestId,
                "reason": request.reason,
                "cause": "rollback-ready-timeout",
                "replacementFailure": replacementFailure,
                "processIdentifier": processIdentifier ?? 0,
                "timeoutSeconds": timeout
            ])
            self.onReplacementResult(
                request.requestId,
                .rollbackFailed,
                "rollback-ready-timeout")
            self.terminateApplication()
        }
    }

    private func completeRollback(
        request: SidecarReplacementRequest,
        cause: String
    ) {
        let currentProcessIdentifier =
            sidecar.processIdentifier ?? 0
        let reload = reloadWindows()
        trace("recv", "sidecar.rollbackReady", [
            "requestId": request.requestId,
            "reason": request.reason,
            "replacementFailure": cause,
            "previousProcessIdentifier":
                request.previousProcessIdentifier,
            "processIdentifier": currentProcessIdentifier,
            "processReplaced":
                request.previousProcessIdentifier > 0
                    && currentProcessIdentifier > 0
                    && request.previousProcessIdentifier
                        != currentProcessIdentifier,
            "windowsReloaded": reload.windowCount,
            "recoveryGeneration": reload.highestGeneration
        ])
        replacementPhase = nil
        exitedProcessIdentifier = nil
        onLifecycleReady()
        scheduleRecoveryBudgetResetIfNeeded()
        onReplacementResult(
            request.requestId,
            .rolledBack,
            "stable-sidecar-ready")
        startPendingReplacementIfNeeded()
    }

    private func scheduleRecoveryBudgetResetIfNeeded() {
        guard !hostShutdownRequested,
              restartAttempts > 0,
              let processIdentifier = sidecar.processIdentifier else {
            return
        }
        let attempts = restartAttempts
        let stableRunSeconds =
            config.flowIpcSidecarRecoveryStableRunSeconds
        trace("recv", "sidecar.recoveryStabilityDeadline", [
            "attempts": attempts,
            "processIdentifier": processIdentifier,
            "stableRunSeconds": stableRunSeconds
        ])
        DispatchQueue.main.asyncAfter(
            deadline: .now() + stableRunSeconds
        ) { [weak self] in
            guard let self,
                  !self.hostShutdownRequested,
                  !self.recoveryInProgress,
                  self.replacementPhase == nil,
                  self.restartAttempts == attempts,
                  self.sidecar.processIdentifier == processIdentifier else {
                return
            }
            self.restartAttempts = 0
            trace("recv", "sidecar.recoveryBudgetReset", [
                "previousAttempts": attempts,
                "processIdentifier": processIdentifier,
                "stableRunSeconds": stableRunSeconds
            ])
        }
    }

    private func disconnectBrowserBridges(
        _ windows: [FlowIpcWindowContext]
    ) {
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

    private func invalidateNativeOperations(
        _ windows: [FlowIpcWindowContext],
        reason: String
    ) {
        for context in windows {
            context.commandExecutor.invalidateNativeOperations(reason: reason)
        }
    }

    private func reloadWindows() -> (
        windowCount: Int,
        highestGeneration: Int
    ) {
        let windows = flowIpcWindows()
        var highestGeneration = 0
        for context in windows {
            context.recoveryGeneration += 1
            context.navigationFinished = false
            highestGeneration = max(
                highestGeneration, context.recoveryGeneration)
            context.webView.configuration.userContentController.addUserScript(
                WKUserScript(
                    source: "window.VaadinDesktopHostRecoveryGeneration = \(context.recoveryGeneration);",
                    injectionTime: .atDocumentStart,
                    forMainFrameOnly: true))
            var request = URLRequest(url: context.reloadURL)
            request.cachePolicy = .reloadIgnoringLocalCacheData
            context.webView.load(request)
        }
        return (windows.count, highestGeneration)
    }
}
