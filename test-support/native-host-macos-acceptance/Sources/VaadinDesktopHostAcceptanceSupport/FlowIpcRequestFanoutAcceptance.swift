import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleRequestFanoutAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcRequestFanoutAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcRequestFanoutAcceptance {
    private enum Phase {
        case ready
        case route
        case transfer
        case confirm
    }

    private let context: FlowIpcWindowContext
    private let deadline: Date
    private var phase = Phase.ready

    init(context: FlowIpcWindowContext, deadline: Date) {
        self.context = context
        self.deadline = deadline
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "requestFanoutInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "requestFanoutBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.rootRendered,
                      snapshot.firstMeaningfulPaint,
                      snapshot.pushMessageCount >= 1 else {
                    self.retryOrFail(
                        code: "requestFanoutRootTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    next: .route,
                    failureCode: "requestFanoutNavigationFailed")
            case .route:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "requestFanoutRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startTransferScript,
                    next: .transfer,
                    failureCode: "requestFanoutStartFailed")
            case .transfer:
                guard snapshot.browserTransferComplete,
                      snapshot.javaListenersObserved else {
                    self.retryOrFail(
                        code: "requestFanoutTransferTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.confirmScript,
                    next: .confirm,
                    failureCode: "requestFanoutConfirmDispatchFailed")
            case .confirm:
                guard snapshot.serverConfirmed else {
                    self.retryOrFail(
                        code: "requestFanoutServerConfirmationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        context.webView.evaluateJavaScript(Self.inspectionScript) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let snapshot = try? JSONDecoder().decode(
                    Snapshot.self,
                    from: data) else {
                completion(nil)
                return
            }
            completion(snapshot)
        }
    }

    private func dispatch(
        _ script: String,
        next: Phase,
        failureCode: String
    ) {
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil, result as? Bool == true else {
                self.retryOrFail(
                    code: failureCode,
                    details: [
                        "message": error?.localizedDescription ?? "",
                        "result": String(describing: result)
                    ])
                return
            }
            self.phase = next
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                self.poll()
            }
        }
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.poll()
        }
    }

    private func finish(_ snapshot: Snapshot) {
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": snapshot.complete,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "rootRendered": snapshot.rootRendered,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 62,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 42,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "requestFanoutBrowserOverlapObserved":
                snapshot.browserOverlapObserved,
            "requestFanoutQueueReplenishmentObserved":
                snapshot.queueReplenishmentObserved,
            "requestFanoutBeyondInitialCreditObserved":
                snapshot.beyondInitialCreditObserved,
            "requestFanoutCancellationObserved":
                snapshot.cancellationObserved,
            "requestFanoutSurvivorsObserved": snapshot.survivorsObserved,
            "requestFanoutServerIsolationObserved":
                snapshot.serverIsolationObserved,
            "requestFanoutExactBytesObserved": snapshot.exactBytesObserved,
            "requestFanoutJavaListenersObserved":
                snapshot.javaListenersObserved,
            "requestFanoutRegistryTurnoverObserved":
                snapshot.registryTurnoverObserved,
            "requestFanoutLateResponseIsolationObserved":
                snapshot.lateResponseIsolationObserved,
            "requestFanoutCancellationTombstoneExpiryObserved":
                snapshot.cancellationTombstoneExpiryObserved,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowRequestFanout.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        context.window.close()
    }

    private struct Snapshot: Decodable {
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let started: Bool
        let transferComplete: Bool
        let batchCount: Int
        let completedBatches: Int
        let batchStartEvents: [Int]
        let batchTerminalEvents: [Int]
        let fileNames: [String]
        let fileLengths: [Int]
        let cancelledFileNames: [String]
        let cancelledFileLengths: [Int]
        let survivorFileNames: [String]
        let survivorFileLengths: [Int]
        let cancelThresholds: [Int]
        let progressBytes: [Int]
        let progressEvents: Int
        let startEvents: Int
        let abortEvents: Int
        let removeEvents: Int
        let successEvents: Int
        let errorEvents: Int
        let browserMaximumActive: Int
        let maxConcurrentUploads: Int
        let allBeyondThreshold: Bool
        let cancelIssuedCount: Int
        let abortFileNames: [String]
        let removedFileNames: [String]
        let successFileNames: [String]
        let requestIds: [String]
        let firstBatchRequestIds: [String]
        let secondBatchRequestIds: [String]
        let requestIdsUnique: Bool
        let lateFrameProbeCount: Int
        let ignoredLateResponseRequestId: String
        let reclaimedRequestId: String
        let registryProbeExpectedError: String
        let cancelledLateFramesIgnored: Bool
        let completedRequestReclaimed: Bool
        let tombstoneBoundaryProbeCount: Int
        let tombstoneRetentionElapsedMillis: Int
        let tombstoneExpiryElapsedMillis: Int
        let cancellationTombstoneRetained: Bool
        let cancellationTombstoneExpired: Bool
        let tombstoneExpiryExpectedError: String
        let remainingFileNames: [String]
        let remainingFilesComplete: Bool
        let serverMaximumActive: Int
        let serverCancelledMinimum: Int
        let serverCancelledMaximum: Int
        let serverSuccessBytes: Int
        let serverExpectedSuccessBytes: Int
        let listenerStatus: String
        let status: String
        let transferError: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        private static let batchSize = 6
        private static let expectedBatchCount = 10
        private static let expectedFileCount = batchSize * expectedBatchCount
        private static let initialFileLengths = [
            4 * 1024 * 1024 + 37,
            4 * 1024 * 1024 + 83,
            4 * 1024 * 1024 + 139,
            4 * 1024 * 1024 + 197,
            4 * 1024 * 1024 + 251,
            4 * 1024 * 1024 + 307,
            2 * 1024 * 1024 + 359,
            2 * 1024 * 1024 + 419,
            2 * 1024 * 1024 + 487,
            2 * 1024 * 1024 + 557,
            2 * 1024 * 1024 + 631,
            2 * 1024 * 1024 + 701
        ]
        private static let expectedFileNames: [String] = {
            var names: [String] = []
            names.reserveCapacity(expectedFileCount)
            for index in 0..<expectedFileCount {
                let outcome = isCancelledIndex(index)
                    ? "cancel" : "survivor"
                names.append(
                    "flow-ipc-request-fanout-\(index)-\(outcome).bin")
            }
            return names
        }()
        private static let expectedFileLengths: [Int] = {
            var lengths: [Int] = []
            lengths.reserveCapacity(expectedFileCount)
            for index in 0..<expectedFileCount {
                if index < initialFileLengths.count {
                    lengths.append(initialFileLengths[index])
                } else {
                    lengths.append(256 * 1024 + index * 73 + 17)
                }
            }
            return lengths
        }()
        private static let expectedCancelThresholds: [Int] = {
            var thresholds: [Int] = []
            thresholds.reserveCapacity(expectedFileCount)
            for index in 0..<expectedFileCount {
                thresholds.append(
                    index < initialFileLengths.count
                        ? 512 * 1024 : 192 * 1024)
            }
            return thresholds
        }()
        private static let expectedCancelledFileNames =
            expectedFileNames.enumerated().compactMap { index, name in
                isCancelledIndex(index) ? name : nil
            }
        private static let expectedCancelledFileLengths =
            expectedFileLengths.enumerated().compactMap { index, length in
                isCancelledIndex(index) ? length : nil
            }
        private static let expectedSurvivorFileNames =
            expectedFileNames.enumerated().compactMap { index, name in
                isCancelledIndex(index) ? nil : name
            }
        private static let expectedSurvivorFileLengths =
            expectedFileLengths.enumerated().compactMap { index, length in
                isCancelledIndex(index) ? nil : length
            }
        private static let expectedBatchEvents =
            Array(repeating: batchSize, count: expectedBatchCount)
        private static let expectedResultMask =
            String(repeating: "scsscs", count: expectedBatchCount)

        private static func isCancelledIndex(_ index: Int) -> Bool {
            index % batchSize == 1 || index % batchSize == 4
        }

        var errorFree: Bool {
            transferError.isEmpty
                && browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
                && errorEvents == 0
        }

        var browserOverlapObserved: Bool {
            startEvents == Self.expectedFileNames.count
                && browserMaximumActive == 3
                && batchStartEvents == Self.expectedBatchEvents
                && allBeyondThreshold
        }

        var queueReplenishmentObserved: Bool {
            maxConcurrentUploads == 3
                && startEvents == Self.expectedFileNames.count
                && startEvents > browserMaximumActive
                && browserMaximumActive == maxConcurrentUploads
                && batchTerminalEvents == Self.expectedBatchEvents
        }

        var beyondInitialCreditObserved: Bool {
            cancelThresholds == Self.expectedCancelThresholds
                && progressBytes.count == Self.expectedFileNames.count
                && progressBytes.enumerated().allSatisfy { index, progress in
                    if Self.isCancelledIndex(index) {
                        return progress >= cancelThresholds[index]
                            && progress < Self.expectedFileLengths[index]
                    }
                    return progress == Self.expectedFileLengths[index]
                }
        }

        var cancellationObserved: Bool {
            cancelIssuedCount == Self.expectedCancelledFileNames.count
                && abortEvents == Self.expectedCancelledFileNames.count
                && removeEvents == Self.expectedCancelledFileNames.count
                && Set(abortFileNames)
                    == Set(Self.expectedCancelledFileNames)
                && Set(removedFileNames)
                    == Set(Self.expectedCancelledFileNames)
        }

        var survivorsObserved: Bool {
            successEvents == Self.expectedSurvivorFileNames.count
                && Set(successFileNames) == Set(Self.expectedSurvivorFileNames)
                && Set(remainingFileNames)
                    == Set(Self.expectedSurvivorFileNames)
                && remainingFilesComplete
        }

        var javaListenersObserved: Bool {
            let expectedRemoved = Self.expectedCancelledFileNames
                .sorted().joined(separator: ",")
            return listenerStatus.contains(
                "removed=\(Self.expectedCancelledFileNames.count)")
                && listenerStatus.contains(
                    "removedFiles=\(expectedRemoved)")
                && listenerStatus.contains(
                    "allFinished=\(Self.expectedBatchCount)")
        }

        var registryTurnoverObserved: Bool {
            batchCount == Self.expectedBatchCount
                && completedBatches == Self.expectedBatchCount
                && batchStartEvents == Self.expectedBatchEvents
                && batchTerminalEvents == Self.expectedBatchEvents
                && requestIds.count == Self.expectedFileNames.count
                && requestIdsUnique
                && Set(requestIds).count == Self.expectedFileNames.count
                && firstBatchRequestIds.count == Self.batchSize
                && secondBatchRequestIds.count == Self.batchSize
                && Set(firstBatchRequestIds).isDisjoint(
                    with: Set(secondBatchRequestIds))
                && completedRequestReclaimed
        }

        var lateResponseIsolationObserved: Bool {
            guard let ignoredIndex = requestIds.firstIndex(
                of: ignoredLateResponseRequestId),
                  let reclaimedIndex = requestIds.firstIndex(
                    of: reclaimedRequestId) else {
                return false
            }
            return lateFrameProbeCount == 4
                && cancelledLateFramesIgnored
                && Self.isCancelledIndex(ignoredIndex)
                && reclaimedIndex < Self.batchSize
                && !Self.isCancelledIndex(reclaimedIndex)
                && registryProbeExpectedError
                    == "unknown request \(reclaimedRequestId)"
        }

        var cancellationTombstoneExpiryObserved: Bool {
            guard let ignoredIndex = requestIds.firstIndex(
                of: ignoredLateResponseRequestId) else {
                return false
            }
            return tombstoneBoundaryProbeCount == 2
                && cancellationTombstoneRetained
                && cancellationTombstoneExpired
                && tombstoneRetentionElapsedMillis >= 54_000
                && tombstoneRetentionElapsedMillis < 60_000
                && tombstoneExpiryElapsedMillis >= 60_000
                && Self.isCancelledIndex(ignoredIndex)
                && tombstoneExpiryExpectedError
                    == "unknown request \(ignoredLateResponseRequestId)"
        }

        var serverIsolationObserved: Bool {
            status.contains("requests=\(Self.expectedFileCount)")
                && status.contains("active=0")
                && serverMaximumActive == maxConcurrentUploads
                && status.contains(
                    "cancelled=\(Self.expectedCancelledFileNames.count)")
                && status.contains("cancellationsValid=true")
                && serverCancelledMinimum >= 160 * 1024
                && serverCancelledMaximum
                    < Self.expectedCancelledFileLengths.max()!
                && status.contains(
                    "successes=\(Self.expectedSurvivorFileNames.count)")
                && status.contains("survivorsValid=true")
                && status.contains("results=\(Self.expectedFileCount)")
                && status.contains(
                    "resultMask=\(Self.expectedResultMask)")
                && status.contains("invalid=0")
        }

        var exactBytesObserved: Bool {
            let expected = Self.expectedSurvivorFileLengths.reduce(0, +)
            return serverSuccessBytes == expected
                && serverExpectedSuccessBytes == expected
                && status.contains("declarationsValid=true")
                && status.contains("patternsValid=true")
        }

        var serverConfirmed: Bool {
            status.contains("Request fan-out: confirmed")
                && status.contains("clicks=1")
                && serverIsolationObserved
                && exactBytesObserved
        }

        var browserTransferComplete: Bool {
            transferComplete
                && browserOverlapObserved
                && queueReplenishmentObserved
                && beyondInitialCreditObserved
                && cancellationObserved
                && survivorsObserved
                && registryTurnoverObserved
                && lateResponseIsolationObserved
                && cancellationTombstoneExpiryObserved
                && errorFree
        }

        var complete: Bool {
            browserTransferComplete
                && fileNames == Self.expectedFileNames
                && fileLengths == Self.expectedFileLengths
                && cancelledFileNames == Self.expectedCancelledFileNames
                && cancelledFileLengths == Self.expectedCancelledFileLengths
                && survivorFileNames == Self.expectedSurvivorFileNames
                && survivorFileLengths == Self.expectedSurvivorFileLengths
                && pushMessageCount >= 1
                && javaListenersObserved
                && serverIsolationObserved
                && exactBytesObserved
                && serverConfirmed
        }

        var tracePayload: [String: Any] {
            [
                "rootRendered": rootRendered,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "routeReady": routeReady,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "started": started,
                "transferComplete": transferComplete,
                "batchCount": batchCount,
                "completedBatches": completedBatches,
                "batchStartEvents": batchStartEvents,
                "batchTerminalEvents": batchTerminalEvents,
                "fileNames": fileNames,
                "fileLengths": fileLengths,
                "cancelledFileNames": cancelledFileNames,
                "cancelledFileLengths": cancelledFileLengths,
                "survivorFileNames": survivorFileNames,
                "survivorFileLengths": survivorFileLengths,
                "cancelThresholds": cancelThresholds,
                "progressBytes": progressBytes,
                "progressEvents": progressEvents,
                "startEvents": startEvents,
                "abortEvents": abortEvents,
                "removeEvents": removeEvents,
                "successEvents": successEvents,
                "errorEvents": errorEvents,
                "browserMaximumActive": browserMaximumActive,
                "maxConcurrentUploads": maxConcurrentUploads,
                "allBeyondThreshold": allBeyondThreshold,
                "cancelIssuedCount": cancelIssuedCount,
                "abortFileNames": abortFileNames,
                "removedFileNames": removedFileNames,
                "successFileNames": successFileNames,
                "requestIds": requestIds,
                "firstBatchRequestIds": firstBatchRequestIds,
                "secondBatchRequestIds": secondBatchRequestIds,
                "requestIdsUnique": requestIdsUnique,
                "lateFrameProbeCount": lateFrameProbeCount,
                "ignoredLateResponseRequestId": ignoredLateResponseRequestId,
                "reclaimedRequestId": reclaimedRequestId,
                "registryProbeExpectedError": registryProbeExpectedError,
                "cancelledLateFramesIgnored": cancelledLateFramesIgnored,
                "completedRequestReclaimed": completedRequestReclaimed,
                "tombstoneBoundaryProbeCount": tombstoneBoundaryProbeCount,
                "tombstoneRetentionElapsedMillis":
                    tombstoneRetentionElapsedMillis,
                "tombstoneExpiryElapsedMillis": tombstoneExpiryElapsedMillis,
                "cancellationTombstoneRetained":
                    cancellationTombstoneRetained,
                "cancellationTombstoneExpired":
                    cancellationTombstoneExpired,
                "tombstoneExpiryExpectedError": tombstoneExpiryExpectedError,
                "remainingFileNames": remainingFileNames,
                "remainingFilesComplete": remainingFilesComplete,
                "serverMaximumActive": serverMaximumActive,
                "serverCancelledMinimum": serverCancelledMinimum,
                "serverCancelledMaximum": serverCancelledMaximum,
                "serverSuccessBytes": serverSuccessBytes,
                "serverExpectedSuccessBytes": serverExpectedSuccessBytes,
                "listenerStatus": listenerStatus,
                "status": status,
                "transferError": transferError,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError,
                "pushError": pushError
            ]
        }
    }

    private static let navigationScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-request-fanout-open-route');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-request-fanout-confirm');
      if (!button) return false;
      button.click();
      return true;
    })()
    """

    private static let startTransferScript = """
    (() => {
      const upload = document.querySelector(
        '#framework-flow-request-fanout');
      if (!upload || window.VaadinDesktopRequestFanout?.started === true) {
        return false;
      }
      const specs = [
        { name: 'flow-ipc-request-fanout-0-survivor.bin',
          length: 4 * 1024 * 1024 + 37, multiplier: 17,
          increment: 3, cancelled: false, batch: 0 },
        { name: 'flow-ipc-request-fanout-1-cancel.bin',
          length: 4 * 1024 * 1024 + 83, multiplier: 29,
          increment: 5, cancelled: true, batch: 0 },
        { name: 'flow-ipc-request-fanout-2-survivor.bin',
          length: 4 * 1024 * 1024 + 139, multiplier: 37,
          increment: 7, cancelled: false, batch: 0 },
        { name: 'flow-ipc-request-fanout-3-survivor.bin',
          length: 4 * 1024 * 1024 + 197, multiplier: 43,
          increment: 11, cancelled: false, batch: 0 },
        { name: 'flow-ipc-request-fanout-4-cancel.bin',
          length: 4 * 1024 * 1024 + 251, multiplier: 53,
          increment: 13, cancelled: true, batch: 0 },
        { name: 'flow-ipc-request-fanout-5-survivor.bin',
          length: 4 * 1024 * 1024 + 307, multiplier: 61,
          increment: 17, cancelled: false, batch: 0 },
        { name: 'flow-ipc-request-fanout-6-survivor.bin',
          length: 2 * 1024 * 1024 + 359, multiplier: 67,
          increment: 19, cancelled: false, batch: 1 },
        { name: 'flow-ipc-request-fanout-7-cancel.bin',
          length: 2 * 1024 * 1024 + 419, multiplier: 71,
          increment: 23, cancelled: true, batch: 1 },
        { name: 'flow-ipc-request-fanout-8-survivor.bin',
          length: 2 * 1024 * 1024 + 487, multiplier: 73,
          increment: 29, cancelled: false, batch: 1 },
        { name: 'flow-ipc-request-fanout-9-survivor.bin',
          length: 2 * 1024 * 1024 + 557, multiplier: 79,
          increment: 31, cancelled: false, batch: 1 },
        { name: 'flow-ipc-request-fanout-10-cancel.bin',
          length: 2 * 1024 * 1024 + 631, multiplier: 83,
          increment: 37, cancelled: true, batch: 1 },
        { name: 'flow-ipc-request-fanout-11-survivor.bin',
          length: 2 * 1024 * 1024 + 701, multiplier: 89,
          increment: 41, cancelled: false, batch: 1 }
      ];
      specs.forEach((spec) => {
        spec.cancelThreshold = 512 * 1024;
      });
      for (let index = 12; index < 60; index += 1) {
        const cancelled = index % 6 === 1 || index % 6 === 4;
        specs.push({
          name: `flow-ipc-request-fanout-${index}-${
            cancelled ? 'cancel' : 'survivor'}.bin`,
          length: 256 * 1024 + index * 73 + 17,
          multiplier: index * 14 + 17,
          increment: index * 11 + 3,
          cancelled,
          batch: Math.floor(index / 6),
          cancelThreshold: 192 * 1024
        });
      }
      const batchSize = 6;
      const batchCount = 10;
      const cancelledCount = specs.filter((spec) => spec.cancelled).length;
      const survivorCount = specs.length - cancelledCount;
      const state = {
        started: true,
        complete: false,
        batchCount,
        completedBatches: 0,
        batchStartEvents: Array(batchCount).fill(0),
        batchTerminalEvents: Array(batchCount).fill(0),
        fileNames: specs.map((spec) => spec.name),
        fileLengths: specs.map((spec) => spec.length),
        cancelledFileNames: specs.filter((spec) => spec.cancelled)
          .map((spec) => spec.name),
        cancelledFileLengths: specs.filter((spec) => spec.cancelled)
          .map((spec) => spec.length),
        survivorFileNames: specs.filter((spec) => !spec.cancelled)
          .map((spec) => spec.name),
        survivorFileLengths: specs.filter((spec) => !spec.cancelled)
          .map((spec) => spec.length),
        cancelThresholds: specs.map((spec) => spec.cancelThreshold),
        progressByName: Object.fromEntries(
          specs.map((spec) => [spec.name, 0])),
        progressEvents: 0,
        startEvents: 0,
        abortEvents: 0,
        removeEvents: 0,
        successEvents: 0,
        errorEvents: 0,
        browserMaximumActive: 0,
        maxConcurrentUploads: Number(upload.maxConcurrentUploads || 0),
        allBeyondThreshold: false,
        cancelIssuedCount: 0,
        cancelIssuedFileNames: [],
        cancelIssuedAtByName: {},
        abortFileNames: [],
        removedFileNames: [],
        successFileNames: [],
        requestIdsByName: {},
        lateFrameProbeCount: 0,
        ignoredLateResponseRequestId: '',
        reclaimedRequestId: '',
        registryProbeExpectedError: '',
        cancelledLateFramesIgnored: false,
        completedRequestReclaimed: false,
        tombstoneBoundaryProbeCount: 0,
        tombstoneRetentionElapsedMillis: 0,
        tombstoneExpiryElapsedMillis: 0,
        cancellationTombstoneRetained: false,
        cancellationTombstoneExpired: false,
        tombstoneExpiryExpectedError: '',
        error: ''
      };
      window.VaadinDesktopRequestFanout = state;
      const activeFiles = new Set();
      const terminalFiles = new Set();
      const batchTerminalFiles = Array.from(
        { length: batchCount }, () => new Set());
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const xhrPrototype = window.XMLHttpRequest?.prototype;
      const originalSend = xhrPrototype?.send;
      const originalAbort = xhrPrototype?.abort;
      const requestSpecs = new WeakMap();

      const appendUnique = (values, value) => {
        if (!values.includes(value)) values.push(value);
      };
      const updateComplete = () => {
        state.complete = state.completedBatches === batchCount
          && state.abortEvents === cancelledCount
          && state.removeEvents === cancelledCount
          && state.successEvents === survivorCount
          && state.errorEvents === 0
          && state.cancelledLateFramesIgnored
          && state.completedRequestReclaimed
          && state.cancellationTombstoneRetained
          && state.cancellationTombstoneExpired
          && specs.every((spec) => !!state.requestIdsByName[spec.name]);
        if (state.complete && xhrPrototype?.send === trackedSend) {
          xhrPrototype.send = originalSend;
          xhrPrototype.abort = originalAbort;
        }
      };
      const makeFile = (spec) => {
        const bytes = new Uint8Array(spec.length);
        for (let index = 0; index < bytes.length; index += 1) {
          bytes[index] = ((index * spec.multiplier + spec.increment) & 0xff);
        }
        return new File(
          [bytes],
          spec.name,
          {
            type: 'application/octet-stream',
            lastModified: 1700000000000
          });
      };
      const findDeep = (root, predicate) => {
        const visit = (node) => {
          if (!node) return null;
          if (node.nodeType === 1 && predicate(node)) return node;
          if (node.shadowRoot) {
            const shadowResult = visit(node.shadowRoot);
            if (shadowResult) return shadowResult;
          }
          const children = node.children || node.childNodes || [];
          for (const child of children) {
            const result = visit(child);
            if (result) return result;
          }
          return null;
        };
        return visit(root);
      };
      const input = findDeep(upload, (element) =>
        element.localName === 'input'
          && (element.type || '').toLowerCase() === 'file');
      const filesAdded = (batchSpecs) => {
        if (!Array.isArray(upload.files)) return false;
        const names = new Set(upload.files.map((file) => file?.name));
        return batchSpecs.every((spec) => names.has(spec.name));
      };
      const addFiles = (batchSpecs, files) => {
        if (input) {
          try {
            const dataTransfer = new DataTransfer();
            files.forEach((file) => dataTransfer.items.add(file));
            Object.defineProperty(input, 'files', {
              configurable: true,
              value: dataTransfer.files
            });
            input.dispatchEvent(new Event(
              'change', { bubbles: true, composed: true }));
          } catch (_) {
            // Constructed FileList assignment is not available on every WebKit.
          }
        }
        if (!filesAdded(batchSpecs) && typeof upload._addFiles === 'function') {
          upload._addFiles(files);
        }
        if (!filesAdded(batchSpecs)) return null;
        return batchSpecs.map((spec) =>
          upload.files.find((file) => file?.name === spec.name));
      };

      const captureRequestId = (xhr, spec, attempt = 0) => {
        const requestId = String(xhr?._requestId || '');
        if (requestId) {
          state.requestIdsByName[spec.name] = requestId;
          updateComplete();
          return;
        }
        if (attempt >= 200) {
          state.error = `request id capture failed: ${spec.name}`;
          state.complete = true;
          return;
        }
        setTimeout(() => captureRequestId(xhr, spec, attempt + 1), 5);
      };
      function trackedSend(body) {
        const result = originalSend.call(this, body);
        const bodyName = String(body?.name || '');
        const bodyLength = Number(body?.size ?? body?.byteLength ?? -1);
        const spec = specs.find((candidate) =>
          candidate.name === bodyName || candidate.length === bodyLength);
        if (spec) {
          requestSpecs.set(this, spec);
          captureRequestId(this, spec);
        }
        return result;
      }
      function trackedAbort() {
        const spec = requestSpecs.get(this);
        if (spec && !state.cancelIssuedAtByName[spec.name]) {
          state.cancelIssuedAtByName[spec.name] = Date.now();
        }
        return originalAbort.call(this);
      }
      if (!xhrPrototype
          || typeof originalSend !== 'function'
          || typeof originalAbort !== 'function') {
        state.error = 'opaque XHR request tracking is unavailable';
        state.complete = true;
        return true;
      }
      xhrPrototype.send = trackedSend;
      xhrPrototype.abort = trackedAbort;

      const receiveProbe = (
        type, requestId, payload = {}, tombstoneBoundary = false
      ) => {
        window.VaadinDesktopOpaqueHttp.receive({
          version: 1,
          type,
          correlationId: requestId,
          payload: { requestId, ...payload }
        });
        if (tombstoneBoundary) {
          state.tombstoneBoundaryProbeCount += 1;
        } else {
          state.lateFrameProbeCount += 1;
        }
      };

      const startBatch = (batch) => {
        const batchSpecs = specs.filter((spec) => spec.batch === batch);
        const files = batchSpecs.map(makeFile);
        try {
          const added = addFiles(batchSpecs, files);
          if (!Array.isArray(added)
              || added.length !== batchSpecs.length
              || added.some((file) => !file)
              || typeof upload.uploadFiles !== 'function') {
            state.error = `request fan-out batch ${batch} injection failed`;
            state.complete = true;
            return;
          }
          upload.uploadFiles(added);
        } catch (error) {
          state.error = String(
            error?.message || error || `request fan-out batch ${batch} failed`);
          state.complete = true;
        }
      };

      const probeRegistryAndStartSecondBatch = (attempt = 0) => {
        const firstBatch = specs.filter((spec) => spec.batch === 0);
        if (!firstBatch.every((spec) => !!state.requestIdsByName[spec.name])) {
          if (attempt >= 100) {
            state.error = 'first batch request ids were not captured';
            state.complete = true;
            return;
          }
          setTimeout(() => probeRegistryAndStartSecondBatch(attempt + 1), 10);
          return;
        }
        const ignoredSpec = firstBatch.find((spec) => spec.cancelled);
        const reclaimedSpec = firstBatch.find((spec) => !spec.cancelled);
        const ignoredRequestId = state.requestIdsByName[ignoredSpec.name];
        const reclaimedRequestId = state.requestIdsByName[reclaimedSpec.name];
        state.ignoredLateResponseRequestId = ignoredRequestId;
        state.reclaimedRequestId = reclaimedRequestId;

        delete diagnostics.standardBundleOpaqueResponseError;
        receiveProbe('opaqueHttpStreamResponse', ignoredRequestId, {
          status: 599, statusText: 'Late', headers: {}
        });
        receiveProbe('opaqueHttpStreamResponseChunk', ignoredRequestId, {
          chunkSequence: 0, bodyBase64: '', bodyBytes: 0
        });
        receiveProbe('opaqueHttpStreamResponseEnd', ignoredRequestId, {
          chunkCount: 0, bodyBytes: 0
        });
        receiveProbe('opaqueHttpStreamError', ignoredRequestId, {
          message: 'late cancelled response'
        });
        state.cancelledLateFramesIgnored =
          !diagnostics.standardBundleOpaqueResponseError;

        window.VaadinDesktopOpaqueHttp.receive({
          version: 1,
          type: 'opaqueHttpStreamResponse',
          correlationId: reclaimedRequestId,
          payload: {
            requestId: reclaimedRequestId,
            status: 599,
            statusText: 'Late',
            headers: {}
          }
        });
        state.registryProbeExpectedError = String(
          diagnostics.standardBundleOpaqueResponseError || '');
        state.completedRequestReclaimed =
          state.registryProbeExpectedError
            === `unknown request ${reclaimedRequestId}`;
        delete diagnostics.standardBundleOpaqueResponseError;

        if (!state.cancelledLateFramesIgnored
            || !state.completedRequestReclaimed) {
          state.error = 'request registry boundary probe failed';
          state.complete = true;
          return;
        }
        const cancelledAt = Number(
          state.cancelIssuedAtByName[ignoredSpec.name] || 0);
        if (cancelledAt <= 0) {
          state.error = 'cancellation tombstone start was not captured';
          state.complete = true;
          return;
        }
        const retentionDelay = Math.max(
          0, cancelledAt + 55000 - Date.now());
        setTimeout(() => {
          delete diagnostics.standardBundleOpaqueResponseError;
          receiveProbe('opaqueHttpStreamResponse', ignoredRequestId, {
            status: 598, statusText: 'Retained', headers: {}
          }, true);
          state.tombstoneRetentionElapsedMillis =
            Date.now() - cancelledAt;
          state.cancellationTombstoneRetained =
            !diagnostics.standardBundleOpaqueResponseError
              && state.tombstoneRetentionElapsedMillis >= 54000
              && state.tombstoneRetentionElapsedMillis < 60000;
          if (!state.cancellationTombstoneRetained) {
            state.error = 'cancellation tombstone expired too early';
            state.complete = true;
            return;
          }
          const expiryDelay = Math.max(
            0, cancelledAt + 61000 - Date.now());
          setTimeout(() => {
            delete diagnostics.standardBundleOpaqueResponseError;
            receiveProbe('opaqueHttpStreamResponse', ignoredRequestId, {
              status: 597, statusText: 'Expired', headers: {}
            }, true);
            state.tombstoneExpiryElapsedMillis =
              Date.now() - cancelledAt;
            state.tombstoneExpiryExpectedError = String(
              diagnostics.standardBundleOpaqueResponseError || '');
            state.cancellationTombstoneExpired =
              state.tombstoneExpiryElapsedMillis >= 60000
                && state.tombstoneExpiryExpectedError
                  === `unknown request ${ignoredRequestId}`;
            delete diagnostics.standardBundleOpaqueResponseError;
            if (!state.cancellationTombstoneExpired) {
              state.error = 'cancellation tombstone did not expire';
              state.complete = true;
              return;
            }
            startBatch(1);
          }, expiryDelay);
        }, retentionDelay);
      };

      const markTerminal = (name) => {
        if (terminalFiles.has(name)) return;
        const spec = specs.find((candidate) => candidate.name === name);
        if (!spec) {
          state.error = `unknown terminal request fan-out file: ${name}`;
          state.complete = true;
          return;
        }
        terminalFiles.add(name);
        batchTerminalFiles[spec.batch].add(name);
        state.batchTerminalEvents[spec.batch] =
          batchTerminalFiles[spec.batch].size;
        if (batchTerminalFiles[spec.batch].size === batchSize) {
          state.completedBatches += 1;
          if (spec.batch === 0) {
            setTimeout(() => probeRegistryAndStartSecondBatch(), 350);
          } else if (spec.batch + 1 < batchCount) {
            setTimeout(() => startBatch(spec.batch + 1), 100);
          }
        }
        updateComplete();
      };
      const maybeCancel = (name) => {
        state.allBeyondThreshold = specs.every((spec) =>
          Number(state.progressByName[spec.name] || 0)
            >= spec.cancelThreshold);
        const spec = specs.find((candidate) => candidate.name === name);
        if (!spec?.cancelled
            || state.cancelIssuedFileNames.includes(name)
            || Number(state.progressByName[name] || 0)
              < spec.cancelThreshold) {
          return;
        }
        const file = upload.files.find((candidate) =>
          candidate?.name === name);
        if (!file) {
          state.error = 'cancelled fan-out file lookup failed';
          state.complete = true;
          return;
        }
        state.cancelIssuedFileNames.push(name);
        state.cancelIssuedCount += 1;
        upload.dispatchEvent(new CustomEvent('file-abort', {
          detail: { file }
        }));
      };

      upload.addEventListener('upload-start', (event) => {
        const name = String(event.detail?.file?.name || '');
        const spec = specs.find((candidate) => candidate.name === name);
        state.startEvents += 1;
        if (spec) state.batchStartEvents[spec.batch] += 1;
        activeFiles.add(name);
        state.browserMaximumActive = Math.max(
          state.browserMaximumActive, activeFiles.size);
      });
      upload.addEventListener('upload-progress', (event) => {
        const file = event.detail?.file;
        const name = String(file?.name || '');
        const loaded = Number(file?.loaded || 0);
        state.progressEvents += 1;
        state.progressByName[name] = Math.max(
          Number(state.progressByName[name] || 0), loaded);
        maybeCancel(name);
      });
      upload.addEventListener('upload-abort', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.abortEvents += 1;
        appendUnique(state.abortFileNames, name);
        activeFiles.delete(name);
        markTerminal(name);
        updateComplete();
      });
      upload.addEventListener('file-remove', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.removeEvents += 1;
        appendUnique(state.removedFileNames, name);
        activeFiles.delete(name);
        updateComplete();
      });
      upload.addEventListener('upload-success', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.successEvents += 1;
        appendUnique(state.successFileNames, name);
        activeFiles.delete(name);
        markTerminal(name);
        updateComplete();
      });
      upload.addEventListener('upload-error', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.errorEvents += 1;
        activeFiles.delete(name);
        state.error = String(
          event.detail?.file?.error || `request fan-out failed: ${name}`);
        state.complete = true;
      });

      startBatch(0);
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const state = window.VaadinDesktopRequestFanout || {};
      const route = document.querySelector(
        '#framework-flow-request-fanout-route');
      const upload = document.querySelector(
        '#framework-flow-request-fanout');
      const confirm = document.querySelector(
        '#framework-flow-request-fanout-confirm');
      const listenerStatus = document.querySelector(
        '#framework-flow-request-fanout-listener-status')?.textContent || '';
      const status = document.querySelector(
        '#framework-flow-request-fanout-status')?.textContent || '';
      const statusNumber = (name) => {
        const match = status.match(new RegExp(`${name}=(-?\\\\d+)`));
        return Number(match?.[1] ?? -1);
      };
      const files = Array.isArray(upload?.files) ? upload.files : [];
      const fileNames = Array.isArray(state.fileNames) ? state.fileNames : [];
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-request-fanout')
            && !!route && !!upload && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        started: state.started === true,
        transferComplete: state.complete === true,
        batchCount: Number(state.batchCount || 0),
        completedBatches: Number(state.completedBatches || 0),
        batchStartEvents: Array.isArray(state.batchStartEvents)
          ? state.batchStartEvents : [],
        batchTerminalEvents: Array.isArray(state.batchTerminalEvents)
          ? state.batchTerminalEvents : [],
        fileNames,
        fileLengths: Array.isArray(state.fileLengths) ? state.fileLengths : [],
        cancelledFileNames: Array.isArray(state.cancelledFileNames)
          ? state.cancelledFileNames : [],
        cancelledFileLengths: Array.isArray(state.cancelledFileLengths)
          ? state.cancelledFileLengths : [],
        survivorFileNames: Array.isArray(state.survivorFileNames)
          ? state.survivorFileNames : [],
        survivorFileLengths: Array.isArray(state.survivorFileLengths)
          ? state.survivorFileLengths : [],
        cancelThresholds: Array.isArray(state.cancelThresholds)
          ? state.cancelThresholds : [],
        progressBytes: fileNames.map((name) =>
          Number(state.progressByName?.[name] || 0)),
        progressEvents: Number(state.progressEvents || 0),
        startEvents: Number(state.startEvents || 0),
        abortEvents: Number(state.abortEvents || 0),
        removeEvents: Number(state.removeEvents || 0),
        successEvents: Number(state.successEvents || 0),
        errorEvents: Number(state.errorEvents || 0),
        browserMaximumActive: Number(state.browserMaximumActive || 0),
        maxConcurrentUploads: Number(state.maxConcurrentUploads || 0),
        allBeyondThreshold: state.allBeyondThreshold === true,
        cancelIssuedCount: Number(state.cancelIssuedCount || 0),
        abortFileNames: Array.isArray(state.abortFileNames)
          ? state.abortFileNames : [],
        removedFileNames: Array.isArray(state.removedFileNames)
          ? state.removedFileNames : [],
        successFileNames: Array.isArray(state.successFileNames)
          ? state.successFileNames : [],
        requestIds: fileNames.map((name) =>
          String(state.requestIdsByName?.[name] || '')),
        firstBatchRequestIds: fileNames.slice(0, 6).map((name) =>
          String(state.requestIdsByName?.[name] || '')),
        secondBatchRequestIds: fileNames.slice(6, 12).map((name) =>
          String(state.requestIdsByName?.[name] || '')),
        requestIdsUnique: (() => {
          const ids = fileNames.map((name) =>
            String(state.requestIdsByName?.[name] || ''));
          return ids.length === 60
            && ids.every((requestId) => !!requestId)
            && new Set(ids).size === 60;
        })(),
        lateFrameProbeCount: Number(state.lateFrameProbeCount || 0),
        ignoredLateResponseRequestId: String(
          state.ignoredLateResponseRequestId || ''),
        reclaimedRequestId: String(state.reclaimedRequestId || ''),
        registryProbeExpectedError: String(
          state.registryProbeExpectedError || ''),
        cancelledLateFramesIgnored:
          state.cancelledLateFramesIgnored === true,
        completedRequestReclaimed:
          state.completedRequestReclaimed === true,
        tombstoneBoundaryProbeCount: Number(
          state.tombstoneBoundaryProbeCount || 0),
        tombstoneRetentionElapsedMillis: Number(
          state.tombstoneRetentionElapsedMillis || 0),
        tombstoneExpiryElapsedMillis: Number(
          state.tombstoneExpiryElapsedMillis || 0),
        cancellationTombstoneRetained:
          state.cancellationTombstoneRetained === true,
        cancellationTombstoneExpired:
          state.cancellationTombstoneExpired === true,
        tombstoneExpiryExpectedError: String(
          state.tombstoneExpiryExpectedError || ''),
        remainingFileNames: files.map((file) => String(file?.name || '')),
        remainingFilesComplete:
          files.length === 40 && files.every((file) => file?.complete === true),
        serverMaximumActive: statusNumber('maxActive'),
        serverCancelledMinimum: statusNumber('cancelledMin'),
        serverCancelledMaximum: statusNumber('cancelledMax'),
        serverSuccessBytes: statusNumber('successBytes'),
        serverExpectedSuccessBytes: statusNumber('expectedSuccessBytes'),
        listenerStatus,
        status,
        transferError: String(state.error || ''),
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
