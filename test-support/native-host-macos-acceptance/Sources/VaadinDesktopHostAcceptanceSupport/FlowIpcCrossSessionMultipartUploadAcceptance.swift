import Foundation

private var flowIpcCrossSessionMultipartUploadAcceptance:
    FlowIpcCrossSessionMultipartUploadAcceptance?

extension WebViewHost {
    func scheduleCrossSessionMultipartUploadAcceptanceIfReady() {
        guard config.flowIpcAutoCrossSessionMultipartUpload,
              config.transport == .flowIpc else {
            return
        }
        if flowIpcCrossSessionMultipartUploadAcceptance == nil {
            let timeout = config.flowIpcAutoShutdownTimeoutSeconds > 0
                ? config.flowIpcAutoShutdownTimeoutSeconds
                : 90
            flowIpcCrossSessionMultipartUploadAcceptance =
                FlowIpcCrossSessionMultipartUploadAcceptance(
                    host: self,
                    deadline: Date().addingTimeInterval(timeout))
        }
        flowIpcCrossSessionMultipartUploadAcceptance?.resume()
    }
}

private final class FlowIpcCrossSessionMultipartUploadAcceptance {
    private enum Phase {
        case root
        case route
        case secondary
        case overlap
        case completion
        case confirmation
    }

    private static let secondaryWindowId = "multipart-secondary"
    private static let cancelledFileName =
        "flow-ipc-cross-session-multipart-cancel.bin"
    private static let survivorFileName =
        "flow-ipc-cross-session-multipart-survivor.bin"
    private static let cancelledTotalBytes = 12 * 1024 * 1024 + 181
    private static let survivorTotalBytes = 10 * 1024 * 1024 + 263
    private static let overlapThreshold = 512 * 1024

    private let host: WebViewHost
    private let deadline: Date
    private var phase = Phase.root
    private var pollScheduled = false
    private var crossWindowProgressOverlapObserved = false

    init(host: WebViewHost, deadline: Date) {
        self.host = host
        self.deadline = deadline
    }

    func resume() {
        schedulePoll(after: 0.1)
    }

    private func schedulePoll(after delay: Double) {
        guard !pollScheduled else {
            return
        }
        pollScheduled = true
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.pollScheduled = false
            self.poll()
        }
    }

    private func poll() {
        guard let main = host.flowIpcWindowContext("main"),
              main.navigationFinished else {
            retryOrFail(code: "crossSessionMultipartMainWindowTimeout")
            return
        }
        inspect(main) { mainSnapshot in
            guard let mainSnapshot else {
                self.retryOrFail(
                    code: "crossSessionMultipartMainInspectionFailed")
                return
            }
            guard mainSnapshot.errorFree else {
                self.fail(
                    code: "crossSessionMultipartMainRuntimeError",
                    details: mainSnapshot.tracePayload(prefix: "main"))
                return
            }

            switch self.phase {
            case .root:
                if mainSnapshot.routeReady {
                    self.phase = .route
                    self.schedulePoll(after: 0.1)
                    return
                }
                guard mainSnapshot.rootRendered else {
                    self.retryOrFail(
                        code: "crossSessionMultipartRootTimeout",
                        details: mainSnapshot.tracePayload(prefix: "main"))
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    in: main,
                    next: .route,
                    failureCode: "crossSessionMultipartNavigationFailed")
            case .route:
                guard mainSnapshot.routeReady else {
                    self.retryOrFail(
                        code: "crossSessionMultipartRouteTimeout",
                        details: mainSnapshot.tracePayload(prefix: "main"))
                    return
                }
                self.dispatch(
                    Self.openSecondaryScript,
                    in: main,
                    next: .secondary,
                    failureCode: "crossSessionMultipartWindowOpenFailed")
            case .secondary:
                guard let secondary = self.host.flowIpcWindowContext(
                    Self.secondaryWindowId),
                      secondary.navigationFinished else {
                    self.retryOrFail(
                        code: "crossSessionMultipartSecondaryWindowTimeout")
                    return
                }
                self.inspect(secondary) { secondarySnapshot in
                    guard let secondarySnapshot,
                          secondarySnapshot.routeReady,
                          secondarySnapshot.errorFree else {
                        self.retryOrFail(
                            code: "crossSessionMultipartSecondaryRouteTimeout",
                            details: secondarySnapshot?.tracePayload(
                                prefix: "secondary") ?? [:])
                        return
                    }
                    guard self.identityIsolated(
                        mainSnapshot,
                        secondarySnapshot) else {
                        self.fail(
                            code: "crossSessionMultipartIdentityNotIsolated",
                            details: self.mergedPayload(
                                mainSnapshot,
                                secondarySnapshot))
                        return
                    }
                    self.startTransfers(main: main, secondary: secondary)
                }
            case .overlap:
                self.withSecondarySnapshot(mainSnapshot) {
                    secondary, secondarySnapshot in
                    guard mainSnapshot.requestObserved,
                          secondarySnapshot.requestObserved,
                          mainSnapshot.progressBytes >= Self.overlapThreshold,
                          secondarySnapshot.progressBytes
                            >= Self.overlapThreshold,
                          !mainSnapshot.terminal,
                          !secondarySnapshot.terminal else {
                        self.retryOrFail(
                            code: "crossSessionMultipartOverlapTimeout",
                            details: self.mergedPayload(
                                mainSnapshot,
                                secondarySnapshot))
                        return
                    }
                    self.crossWindowProgressOverlapObserved = true
                    self.dispatch(
                        Self.cancelScript,
                        in: main,
                        next: .completion,
                        failureCode:
                            "crossSessionMultipartCancellationFailed")
                }
            case .completion:
                self.withSecondarySnapshot(mainSnapshot) {
                    _, secondarySnapshot in
                    guard mainSnapshot.cancelled,
                          secondarySnapshot.survived,
                          mainSnapshot.pushMessageCount >= 1,
                          secondarySnapshot.pushMessageCount >= 1,
                          mainSnapshot.listenerStatus.contains(
                            "removed=1"),
                          secondarySnapshot.listenerStatus.contains(
                            "allFinished=1") else {
                        self.retryOrFail(
                            code: "crossSessionMultipartCompletionTimeout",
                            details: self.mergedPayload(
                                mainSnapshot,
                                secondarySnapshot))
                        return
                    }
                    guard let secondary = self.host.flowIpcWindowContext(
                        Self.secondaryWindowId) else {
                        self.fail(
                            code: "crossSessionMultipartSecondaryWindowLost")
                        return
                    }
                    self.dispatchPair(
                        script: Self.confirmScript,
                        main: main,
                        secondary: secondary,
                        next: .confirmation,
                        failureCode:
                            "crossSessionMultipartConfirmationFailed")
                }
            case .confirmation:
                self.withSecondarySnapshot(mainSnapshot) {
                    _, secondarySnapshot in
                    guard mainSnapshot.serverConfirmed,
                          secondarySnapshot.serverConfirmed else {
                        self.retryOrFail(
                            code: "crossSessionMultipartServerConfirmationTimeout",
                            details: self.mergedPayload(
                                mainSnapshot,
                                secondarySnapshot))
                        return
                    }
                    self.finish(mainSnapshot, secondarySnapshot)
                }
            }
        }
    }

    private func withSecondarySnapshot(
        _ mainSnapshot: Snapshot,
        completion: @escaping (FlowIpcWindowContext, Snapshot) -> Void
    ) {
        guard let secondary = host.flowIpcWindowContext(
            Self.secondaryWindowId),
              secondary.navigationFinished else {
            retryOrFail(code: "crossSessionMultipartSecondaryWindowLost")
            return
        }
        inspect(secondary) { secondarySnapshot in
            guard let secondarySnapshot else {
                self.retryOrFail(
                    code: "crossSessionMultipartSecondaryInspectionFailed")
                return
            }
            guard secondarySnapshot.errorFree else {
                self.fail(
                    code: "crossSessionMultipartSecondaryRuntimeError",
                    details: secondarySnapshot.tracePayload(
                        prefix: "secondary"))
                return
            }
            completion(secondary, secondarySnapshot)
        }
    }

    private func inspect(
        _ context: FlowIpcWindowContext,
        completion: @escaping (Snapshot?) -> Void
    ) {
        context.webView.evaluateJavaScript(Self.inspectionScript) {
            result, error in
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
        in context: FlowIpcWindowContext,
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
            self.schedulePoll(after: 0.2)
        }
    }

    private func startTransfers(
        main: FlowIpcWindowContext,
        secondary: FlowIpcWindowContext
    ) {
        dispatchPair(
            mainScript: Self.startTransferScript(
                fileName: Self.cancelledFileName,
                byteCount: Self.cancelledTotalBytes,
                multiplier: 47,
                increment: 23,
                role: "cancelled"),
            secondaryScript: Self.startTransferScript(
                fileName: Self.survivorFileName,
                byteCount: Self.survivorTotalBytes,
                multiplier: 43,
                increment: 17,
                role: "survivor"),
            main: main,
            secondary: secondary,
            next: .overlap,
            failureCode: "crossSessionMultipartStartFailed")
    }

    private func dispatchPair(
        script: String,
        main: FlowIpcWindowContext,
        secondary: FlowIpcWindowContext,
        next: Phase,
        failureCode: String
    ) {
        dispatchPair(
            mainScript: script,
            secondaryScript: script,
            main: main,
            secondary: secondary,
            next: next,
            failureCode: failureCode)
    }

    private func dispatchPair(
        mainScript: String,
        secondaryScript: String,
        main: FlowIpcWindowContext,
        secondary: FlowIpcWindowContext,
        next: Phase,
        failureCode: String
    ) {
        let group = DispatchGroup()
        var mainSucceeded = false
        var secondarySucceeded = false
        var messages: [String] = []

        group.enter()
        main.webView.evaluateJavaScript(mainScript) { result, error in
            mainSucceeded = error == nil && result as? Bool == true
            if let error {
                messages.append("main: \(error.localizedDescription)")
            }
            group.leave()
        }
        group.enter()
        secondary.webView.evaluateJavaScript(secondaryScript) {
            result, error in
            secondarySucceeded = error == nil && result as? Bool == true
            if let error {
                messages.append("secondary: \(error.localizedDescription)")
            }
            group.leave()
        }
        group.notify(queue: .main) {
            guard mainSucceeded, secondarySucceeded else {
                self.retryOrFail(
                    code: failureCode,
                    details: ["messages": messages.joined(separator: "; ")])
                return
            }
            self.phase = next
            self.schedulePoll(after: 0.1)
        }
    }

    private func identityIsolated(
        _ main: Snapshot,
        _ secondary: Snapshot
    ) -> Bool {
        main.windowId == "main"
            && secondary.windowId == Self.secondaryWindowId
            && !main.sessionId.isEmpty
            && !secondary.sessionId.isEmpty
            && main.sessionId != secondary.sessionId
            && !main.uiId.isEmpty
            && !secondary.uiId.isEmpty
            && main.uiId != secondary.uiId
    }

    private func finish(_ main: Snapshot, _ secondary: Snapshot) {
        let identityIsolated = identityIsolated(main, secondary)
        let requestObserved = main.requestObserved
            && secondary.requestObserved
        let transportOverlapObserved =
            crossWindowProgressOverlapObserved
                && main.progressBytes >= Self.overlapThreshold
                && secondary.progressBytes >= Self.overlapThreshold
        let parserConcurrencyObserved = max(
            main.serverMaximumTemporaryDirectories,
            secondary.serverMaximumTemporaryDirectories) >= 2
            && max(
                main.serverMaximumTemporaryFiles,
                secondary.serverMaximumTemporaryFiles) >= 2
        let cancellationIsolationObserved = main.cancelled
            && main.serverHandlerCalls == 0
            && main.serverResponseCalls == 0
            && secondary.serverHandlerCalls == 1
            && secondary.serverResponseCalls == 1
        let survivorObserved = secondary.survived
            && secondary.serverSuccessfulResponses == 1
            && secondary.serverAcceptedFiles == 1
            && secondary.serverHandlerFile == Self.survivorFileName
            && secondary.serverAcceptedFile == Self.survivorFileName
        let exactBytesObserved =
            secondary.serverSuccessBytes == Self.survivorTotalBytes
                && secondary.serverDeclaredBytes == Self.survivorTotalBytes
                && secondary.serverSpillBytes == Self.survivorTotalBytes
                && secondary.serverPatternValid
        let diskSpillObserved = secondary.serverDiskSpillObserved
        let cleanupObserved = main.serverTemporaryPartCleanupObserved
            && secondary.serverTemporaryPartCleanupObserved
        let javaListenersObserved = main.listenerStatus.contains("removed=1")
            && main.listenerStatus.contains(Self.cancelledFileName)
            && secondary.listenerStatus.contains("allFinished=1")
        let pushObserved = main.pushMessageCount >= 1
            && secondary.pushMessageCount >= 1
        let errorFree = main.errorFree && secondary.errorFree
            && main.serverMonitorError.isEmpty
            && secondary.serverMonitorError.isEmpty
        let complete = identityIsolated
            && requestObserved
            && transportOverlapObserved
            && parserConcurrencyObserved
            && cancellationIsolationObserved
            && survivorObserved
            && exactBytesObserved
            && diskSpillObserved
            && cleanupObserved
            && javaListenersObserved
            && pushObserved
            && errorFree

        var payload = mergedPayload(main, secondary)
        payload.merge([
            "complete": complete,
            "standardBundleMode": main.firstMeaningfulPaint
                && secondary.firstMeaningfulPaint,
            "rootRendered": main.routeReady && secondary.routeReady,
            "flowClientOwned": main.flowClientCount > 0
                && secondary.flowClientCount > 0,
            "opaqueRequestBridgeObserved": main.requestCount >= 3
                && secondary.requestCount >= 2,
            "opaqueResponseBridgeObserved": main.responseCount >= 2
                && secondary.responseCount >= 2,
            "pushObserved": pushObserved,
            "crossSessionMultipartUploadIdentityIsolationObserved":
                identityIsolated,
            "crossSessionMultipartUploadRequestObserved": requestObserved,
            "crossSessionMultipartUploadTransportOverlapObserved":
                transportOverlapObserved,
            "crossSessionMultipartUploadParserConcurrencyObserved":
                parserConcurrencyObserved,
            "crossSessionMultipartUploadCancellationIsolationObserved":
                cancellationIsolationObserved,
            "crossSessionMultipartUploadSurvivorObserved": survivorObserved,
            "crossSessionMultipartUploadExactBytesObserved":
                exactBytesObserved,
            "crossSessionMultipartUploadDiskSpillObserved":
                diskSpillObserved,
            "crossSessionMultipartUploadTemporaryPartCleanupObserved":
                cleanupObserved,
            "crossSessionMultipartUploadJavaListenersObserved":
                javaListenersObserved,
            "crossWindowProgressOverlapObserved":
                crossWindowProgressOverlapObserved,
            "cancelledTotalBytes": Self.cancelledTotalBytes,
            "survivorTotalBytes": Self.survivorTotalBytes,
            "overlapThreshold": Self.overlapThreshold,
            "maximumTemporaryDirectories": max(
                main.serverMaximumTemporaryDirectories,
                secondary.serverMaximumTemporaryDirectories),
            "maximumTemporaryFiles": max(
                main.serverMaximumTemporaryFiles,
                secondary.serverMaximumTemporaryFiles),
            "errorFree": errorFree
        ]) { _, replacement in replacement }
        trace(
            complete ? "recv" : "send",
            "flowCrossSessionMultipartUpload.frameworkAcceptance",
            payload)
        closeWindows()
    }

    private func mergedPayload(
        _ main: Snapshot,
        _ secondary: Snapshot
    ) -> [String: Any] {
        var payload = main.tracePayload(prefix: "main")
        payload.merge(secondary.tracePayload(prefix: "secondary")) {
            _, replacement in replacement
        }
        return payload
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:]
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        schedulePoll(after: 0.1)
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        trace("send", "error", payload)
        closeWindows()
    }

    private func closeWindows() {
        host.flowIpcWindowContext(Self.secondaryWindowId)?.window.close()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
            self.host.flowIpcWindowContext("main")?.window.close()
        }
    }

    private struct Snapshot: Decodable {
        let windowId: String
        let sessionId: String
        let uiId: String
        let rootRendered: Bool
        let firstMeaningfulPaint: Bool
        let routeReady: Bool
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let started: Bool
        let fileName: String
        let totalBytes: Int
        let progressBytes: Int
        let progressEvents: Int
        let startEvents: Int
        let requestEvents: Int
        let multipartRequestEvents: Int
        let multipartEntryEvents: Int
        let abortEvents: Int
        let removeEvents: Int
        let successEvents: Int
        let errorEvents: Int
        let abortFileName: String
        let removedFileName: String
        let successFileName: String
        let transferError: String
        let listenerStatus: String
        let status: String
        let serverHandlerCalls: Int
        let serverHandlerFailures: Int
        let serverHandlerFile: String
        let serverFailedHandlerFile: String
        let serverResponseCalls: Int
        let serverFailedResponses: Int
        let serverSuccessfulResponses: Int
        let serverAcceptedFiles: Int
        let serverAcceptedFile: String
        let serverSuccessBytes: Int
        let serverDeclaredBytes: Int
        let serverSpillBytes: Int
        let serverMaximumTemporaryDirectories: Int
        let serverMaximumTemporaryFiles: Int
        let serverPatternValid: Bool
        let serverDiskSpillObserved: Bool
        let serverTemporaryPartCleanupObserved: Bool
        let serverMonitorError: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var errorFree: Bool {
            transferError.isEmpty
                && browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
                && errorEvents == 0
        }

        var requestObserved: Bool {
            started
                && startEvents == 1
                && requestEvents == 1
                && multipartRequestEvents == 1
                && multipartEntryEvents == 1
        }

        var terminal: Bool {
            abortEvents > 0 || removeEvents > 0
                || successEvents > 0 || errorEvents > 0
        }

        var cancelled: Bool {
            abortEvents == 1
                && removeEvents == 1
                && successEvents == 0
                && errorEvents == 0
                && abortFileName
                    == FlowIpcCrossSessionMultipartUploadAcceptance
                    .cancelledFileName
                && removedFileName
                    == FlowIpcCrossSessionMultipartUploadAcceptance
                    .cancelledFileName
        }

        var survived: Bool {
            abortEvents == 0
                && removeEvents == 0
                && successEvents == 1
                && errorEvents == 0
                && successFileName
                    == FlowIpcCrossSessionMultipartUploadAcceptance
                    .survivorFileName
        }

        var serverConfirmed: Bool {
            status.contains("Cross-session multipart upload: confirmed")
        }

        func tracePayload(prefix: String) -> [String: Any] {
            [
                "\(prefix)WindowId": windowId,
                "\(prefix)SessionId": sessionId,
                "\(prefix)UiId": uiId,
                "\(prefix)RouteReady": routeReady,
                "\(prefix)RequestCount": requestCount,
                "\(prefix)ResponseCount": responseCount,
                "\(prefix)PushMessageCount": pushMessageCount,
                "\(prefix)FlowClientCount": flowClientCount,
                "\(prefix)FileName": fileName,
                "\(prefix)TotalBytes": totalBytes,
                "\(prefix)ProgressBytes": progressBytes,
                "\(prefix)ProgressEvents": progressEvents,
                "\(prefix)StartEvents": startEvents,
                "\(prefix)RequestEvents": requestEvents,
                "\(prefix)MultipartRequestEvents": multipartRequestEvents,
                "\(prefix)MultipartEntryEvents": multipartEntryEvents,
                "\(prefix)AbortEvents": abortEvents,
                "\(prefix)RemoveEvents": removeEvents,
                "\(prefix)SuccessEvents": successEvents,
                "\(prefix)ErrorEvents": errorEvents,
                "\(prefix)AbortFileName": abortFileName,
                "\(prefix)RemovedFileName": removedFileName,
                "\(prefix)SuccessFileName": successFileName,
                "\(prefix)ServerHandlerCalls": serverHandlerCalls,
                "\(prefix)ServerHandlerFailures": serverHandlerFailures,
                "\(prefix)ServerHandlerFile": serverHandlerFile,
                "\(prefix)ServerFailedHandlerFile":
                    serverFailedHandlerFile,
                "\(prefix)ServerResponseCalls": serverResponseCalls,
                "\(prefix)ServerFailedResponses": serverFailedResponses,
                "\(prefix)ServerSuccessfulResponses":
                    serverSuccessfulResponses,
                "\(prefix)ServerAcceptedFiles": serverAcceptedFiles,
                "\(prefix)ServerAcceptedFile": serverAcceptedFile,
                "\(prefix)ServerSuccessBytes": serverSuccessBytes,
                "\(prefix)ServerDeclaredBytes": serverDeclaredBytes,
                "\(prefix)ServerSpillBytes": serverSpillBytes,
                "\(prefix)ServerMaximumTemporaryDirectories":
                    serverMaximumTemporaryDirectories,
                "\(prefix)ServerMaximumTemporaryFiles":
                    serverMaximumTemporaryFiles,
                "\(prefix)ServerPatternValid": serverPatternValid,
                "\(prefix)ServerDiskSpillObserved":
                    serverDiskSpillObserved,
                "\(prefix)ServerTemporaryPartCleanupObserved":
                    serverTemporaryPartCleanupObserved,
                "\(prefix)ServerMonitorError": serverMonitorError,
                "\(prefix)ListenerStatus": listenerStatus,
                "\(prefix)Status": status,
                "\(prefix)TransferError": transferError,
                "\(prefix)BrowserError": browserError,
                "\(prefix)RequestError": requestError,
                "\(prefix)ResponseError": responseError,
                "\(prefix)PushError": pushError
            ]
        }
    }

    private static let navigationScript = """
    (() => {
      const trigger = document.querySelector(
        '#framework-flow-cross-session-multipart-upload-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let openSecondaryScript = """
    (() => {
      const trigger = document.querySelector(
        '#framework-flow-cross-session-multipart-open-window');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let cancelScript = """
    (() => {
      const upload = document.querySelector(
        '#framework-flow-cross-session-multipart-upload');
      const state = window.VaadinDesktopCrossSessionMultipartUpload;
      const file = Array.isArray(upload?.files)
        ? upload.files.find((candidate) => candidate?.name === state?.fileName)
        : null;
      if (!upload || !state || !file || state.cancelIssued === true) {
        return false;
      }
      state.cancelIssued = true;
      upload.dispatchEvent(new CustomEvent('file-abort', {
        detail: { file }
      }));
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-cross-session-multipart-upload-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static func startTransferScript(
        fileName: String,
        byteCount: Int,
        multiplier: Int,
        increment: Int,
        role: String
    ) -> String {
        """
        (() => {
          const upload = document.querySelector(
            '#framework-flow-cross-session-multipart-upload');
          if (!upload
              || window.VaadinDesktopCrossSessionMultipartUpload?.started
                === true) {
            return false;
          }
          const fileName = '\(fileName)';
          const totalBytes = \(byteCount);
          const state = {
            started: true,
            role: '\(role)',
            fileName,
            totalBytes,
            progressBytes: 0,
            progressEvents: 0,
            startEvents: 0,
            requestEvents: 0,
            multipartRequestEvents: 0,
            multipartEntryEvents: 0,
            abortEvents: 0,
            removeEvents: 0,
            successEvents: 0,
            errorEvents: 0,
            cancelIssued: false,
            abortFileName: '',
            removedFileName: '',
            successFileName: '',
            error: ''
          };
          window.VaadinDesktopCrossSessionMultipartUpload = state;

          const bytes = new Uint8Array(totalBytes);
          for (let index = 0; index < bytes.length; index += 1) {
            bytes[index] = ((index * \(multiplier) + \(increment)) & 0xff);
          }
          const file = new File([bytes], fileName, {
            type: 'application/octet-stream',
            lastModified: 1700000000000
          });

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
          const fileAdded = () => Array.isArray(upload.files)
            && upload.files.some((candidate) => candidate?.name === fileName);
          if (input) {
            try {
              const dataTransfer = new DataTransfer();
              dataTransfer.items.add(file);
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
          if (!fileAdded() && typeof upload._addFiles === 'function') {
            upload._addFiles([file]);
          }

          upload.addEventListener('upload-request', (event) => {
            state.requestEvents += 1;
            const requestFile = event.detail?.file;
            const requestBody = event.detail?.requestBody;
            if (event.detail?.uploadFormat === 'multipart'
                && requestBody instanceof FormData) {
              state.multipartRequestEvents += 1;
              const fieldName = String(
                requestFile?.formDataName || upload.formDataName || 'file');
              const entry = requestBody.get(fieldName);
              if (entry
                  && entry.name === fileName
                  && Number(entry.size) === totalBytes
                  && entry.type === 'application/octet-stream') {
                state.multipartEntryEvents += 1;
              }
            }
          });
          upload.addEventListener('upload-start', () => {
            state.startEvents += 1;
          });
          upload.addEventListener('upload-progress', (event) => {
            state.progressEvents += 1;
            state.progressBytes = Math.max(
              state.progressBytes,
              Number(event.detail?.file?.loaded || 0));
          });
          upload.addEventListener('upload-abort', (event) => {
            state.abortEvents += 1;
            state.abortFileName = String(event.detail?.file?.name || '');
          });
          upload.addEventListener('file-remove', (event) => {
            state.removeEvents += 1;
            state.removedFileName = String(event.detail?.file?.name || '');
          });
          upload.addEventListener('upload-success', (event) => {
            state.successEvents += 1;
            state.successFileName = String(event.detail?.file?.name || '');
          });
          upload.addEventListener('upload-error', (event) => {
            state.errorEvents += 1;
            state.error = String(
              event.detail?.file?.error
                || `cross-session multipart upload failed: ${fileName}`);
          });

          try {
            const tracked = Array.isArray(upload.files)
              ? upload.files.find((candidate) => candidate?.name === fileName)
              : null;
            if (!tracked || typeof upload.uploadFiles !== 'function') {
              state.error = 'cross-session multipart file injection failed';
              return true;
            }
            upload.uploadFiles([tracked]);
          } catch (error) {
            state.error = String(
              error?.message || error
                || 'cross-session multipart upload failed');
          }
          return true;
        })()
        """
    }

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const context = window.VaadinDesktopHostContext || {};
      const state = window.VaadinDesktopCrossSessionMultipartUpload || {};
      const route = document.querySelector(
        '#framework-flow-cross-session-multipart-upload-route');
      const upload = document.querySelector(
        '#framework-flow-cross-session-multipart-upload');
      const confirm = document.querySelector(
        '#framework-flow-cross-session-multipart-upload-confirm');
      const listenerStatus = document.querySelector(
        '#framework-flow-cross-session-multipart-upload-listener-status')
        ?.textContent || '';
      const status = document.querySelector(
        '#framework-flow-cross-session-multipart-upload-status')
        ?.textContent || '';
      const statusNumber = (name) => {
        const match = status.match(new RegExp(`${name}=(-?\\\\d+)`));
        return Number(match?.[1] ?? -1);
      };
      const statusText = (name) => String(
        status.match(new RegExp(`${name}=([^ ]*)`))?.[1] || '');
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        windowId: String(context.windowId || ''),
        sessionId: String(context.sessionId || ''),
        uiId: String(context.uiId || ''),
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith(
            '/flow-cross-session-multipart-upload')
            && !!route && !!upload && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        started: state.started === true,
        fileName: String(state.fileName || ''),
        totalBytes: Number(state.totalBytes || 0),
        progressBytes: Number(state.progressBytes || 0),
        progressEvents: Number(state.progressEvents || 0),
        startEvents: Number(state.startEvents || 0),
        requestEvents: Number(state.requestEvents || 0),
        multipartRequestEvents: Number(state.multipartRequestEvents || 0),
        multipartEntryEvents: Number(state.multipartEntryEvents || 0),
        abortEvents: Number(state.abortEvents || 0),
        removeEvents: Number(state.removeEvents || 0),
        successEvents: Number(state.successEvents || 0),
        errorEvents: Number(state.errorEvents || 0),
        abortFileName: String(state.abortFileName || ''),
        removedFileName: String(state.removedFileName || ''),
        successFileName: String(state.successFileName || ''),
        transferError: String(state.error || ''),
        listenerStatus,
        status,
        serverHandlerCalls: statusNumber('handlerCalls'),
        serverHandlerFailures: statusNumber('handlerFailures'),
        serverHandlerFile: statusText('handlerFile'),
        serverFailedHandlerFile: statusText('failedHandlerFile'),
        serverResponseCalls: statusNumber('responseCalls'),
        serverFailedResponses: statusNumber('failedResponses'),
        serverSuccessfulResponses: statusNumber('successfulResponses'),
        serverAcceptedFiles: statusNumber('acceptedFiles'),
        serverAcceptedFile: statusText('acceptedFile'),
        serverSuccessBytes: statusNumber('successBytes'),
        serverDeclaredBytes: statusNumber('declaredBytes'),
        serverSpillBytes: statusNumber('spillBytes'),
        serverMaximumTemporaryDirectories: statusNumber('maxTempDirs'),
        serverMaximumTemporaryFiles: statusNumber('maxTempFiles'),
        serverPatternValid: status.includes('patternValid=true'),
        serverDiskSpillObserved: status.includes('diskSpill=true'),
        serverTemporaryPartCleanupObserved:
          status.includes('tempCleanup=true'),
        serverMonitorError: statusText('monitorError'),
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
