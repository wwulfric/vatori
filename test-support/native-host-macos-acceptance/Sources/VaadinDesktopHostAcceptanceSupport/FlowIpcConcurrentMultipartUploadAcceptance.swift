import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleConcurrentMultipartUploadAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcConcurrentMultipartUploadAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcConcurrentMultipartUploadAcceptance {
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
                self.retryOrFail(
                    code: "concurrentMultipartUploadInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "concurrentMultipartUploadBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.rootRendered,
                      snapshot.firstMeaningfulPaint,
                      snapshot.pushMessageCount >= 1 else {
                    self.retryOrFail(
                        code: "concurrentMultipartUploadRootTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    next: .route,
                    failureCode:
                        "concurrentMultipartUploadNavigationFailed")
            case .route:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "concurrentMultipartUploadRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startTransferScript,
                    next: .transfer,
                    failureCode: "concurrentMultipartUploadStartFailed")
            case .transfer:
                guard snapshot.transferComplete,
                      snapshot.javaListenersObserved else {
                    self.retryOrFail(
                        code: "concurrentMultipartUploadTransferTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.confirmScript,
                    next: .confirm,
                    failureCode:
                        "concurrentMultipartUploadConfirmDispatchFailed")
            case .confirm:
                guard snapshot.serverConfirmed else {
                    self.retryOrFail(
                        code:
                            "concurrentMultipartUploadServerConfirmationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
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
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 4,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 3,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "concurrentMultipartUploadRequestObserved":
                snapshot.multipartRequestsObserved,
            "concurrentMultipartUploadBrowserOverlapObserved":
                snapshot.browserOverlapObserved,
            "concurrentMultipartUploadProgressObserved":
                snapshot.progressObserved,
            "concurrentMultipartUploadAbortObserved":
                snapshot.abortObserved,
            "concurrentMultipartUploadRemovalObserved":
                snapshot.removalObserved,
            "concurrentMultipartUploadSurvivorObserved":
                snapshot.survivorObserved,
            "concurrentMultipartUploadSessionSerializationObserved":
                snapshot.sessionSerializationObserved,
            "concurrentMultipartUploadCancellationIsolationObserved":
                snapshot.cancellationIsolationObserved,
            "concurrentMultipartUploadExactBytesObserved":
                snapshot.exactBytesObserved,
            "concurrentMultipartUploadDiskSpillObserved":
                snapshot.diskSpillObserved,
            "concurrentMultipartUploadTemporaryPartCleanupObserved":
                snapshot.temporaryPartCleanupObserved,
            "concurrentMultipartUploadJavaListenersObserved":
                snapshot.javaListenersObserved,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace(
            "recv",
            "flowConcurrentMultipartUpload.frameworkAcceptance",
            payload)
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
        let cancelledTotalBytes: Int
        let survivorTotalBytes: Int
        let cancelThreshold: Int
        let overlapThreshold: Int
        let cancelledProgressBytes: Int
        let survivorProgressBytes: Int
        let progressEvents: Int
        let startEvents: Int
        let requestEvents: Int
        let multipartRequestEvents: Int
        let multipartEntryEvents: Int
        let abortEvents: Int
        let removeEvents: Int
        let successEvents: Int
        let errorEvents: Int
        let browserMaximumActive: Int
        let overlapProgressObserved: Bool
        let cancelIssued: Bool
        let abortFileName: String
        let removedFileName: String
        let successFileName: String
        let remainingFileCount: Int
        let remainingFileName: String
        let remainingFileComplete: Bool
        let serverHandlerCalls: Int
        let serverHandlerFailures: Int
        let serverHandlerFile: String
        let serverFailedHandlerFile: String
        let serverResponseCalls: Int
        let serverFailedResponses: Int
        let serverSuccessfulResponses: Int
        let serverSuccessfulAcceptedFiles: Int
        let serverSuccessfulAcceptedFile: String
        let serverSuccessBytes: Int
        let serverSuccessDeclaredBytes: Int
        let serverSpillBytes: Int
        let serverMaximumTemporaryDirectories: Int
        let serverMaximumTemporaryFiles: Int
        let serverDiskSpillObserved: Bool
        let serverTemporaryPartCleanupObserved: Bool
        let serverMonitorError: String
        let listenerStatus: String
        let status: String
        let transferError: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        private static let cancelledFileName =
            "flow-ipc-concurrent-multipart-cancel.bin"
        private static let survivorFileName =
            "flow-ipc-concurrent-multipart-survivor.bin"

        var errorFree: Bool {
            transferError.isEmpty
                && browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
                && serverMonitorError.isEmpty
                && errorEvents == 0
        }

        var multipartRequestsObserved: Bool {
            requestEvents == 2
                && multipartRequestEvents == 2
                && multipartEntryEvents == 2
        }

        var browserOverlapObserved: Bool {
            startEvents == 2
                && browserMaximumActive >= 2
                && overlapProgressObserved
        }

        var progressObserved: Bool {
            progressEvents >= 3
                && cancelledProgressBytes >= cancelThreshold
                && cancelledProgressBytes
                    < cancelledTotalBytes + 64 * 1024
                && survivorProgressBytes >= survivorTotalBytes
                && survivorProgressBytes
                    < survivorTotalBytes + 64 * 1024
        }

        var abortObserved: Bool {
            cancelIssued
                && abortEvents == 1
                && abortFileName == Self.cancelledFileName
        }

        var removalObserved: Bool {
            removeEvents == 1
                && removedFileName == Self.cancelledFileName
        }

        var survivorObserved: Bool {
            successEvents == 1
                && successFileName == Self.survivorFileName
                && remainingFileCount == 1
                && remainingFileName == Self.survivorFileName
                && remainingFileComplete
        }

        var sessionSerializationObserved: Bool {
            serverMaximumTemporaryDirectories == 1
                && serverMaximumTemporaryFiles == 1
        }

        var cancellationIsolationObserved: Bool {
            serverHandlerCalls == 1
                && serverHandlerFailures == 0
                && serverHandlerFile == Self.survivorFileName
                && serverFailedHandlerFile.isEmpty
                && serverResponseCalls == 1
                && serverFailedResponses == 0
                && serverSuccessfulResponses == 1
                && serverSuccessfulAcceptedFiles == 1
                && serverSuccessfulAcceptedFile == Self.survivorFileName
        }

        var exactBytesObserved: Bool {
            serverSuccessBytes == survivorTotalBytes
                && serverSuccessDeclaredBytes == survivorTotalBytes
                && status.contains("successPattern=true")
                && status.contains("invalid=0")
                && status.contains(
                    "cancelledTotal=\(cancelledTotalBytes)")
                && status.contains("survivorTotal=\(survivorTotalBytes)")
        }

        var diskSpillObserved: Bool {
            serverDiskSpillObserved
                && serverSpillBytes == survivorTotalBytes
        }

        var temporaryPartCleanupObserved: Bool {
            serverTemporaryPartCleanupObserved
        }

        var javaListenersObserved: Bool {
            listenerStatus.contains("removed=1")
                && listenerStatus.contains(
                    "removedFile=\(Self.cancelledFileName)")
                && listenerStatus.contains("allFinished=1")
        }

        var serverConfirmed: Bool {
            status.contains("Concurrent multipart upload: confirmed")
                && status.contains("clicks=1")
                && multipartRequestsObserved
                && sessionSerializationObserved
                && cancellationIsolationObserved
                && exactBytesObserved
                && diskSpillObserved
                && temporaryPartCleanupObserved
        }

        var complete: Bool {
            transferComplete
                && pushMessageCount >= 1
                && multipartRequestsObserved
                && browserOverlapObserved
                && progressObserved
                && abortObserved
                && removalObserved
                && survivorObserved
                && sessionSerializationObserved
                && cancellationIsolationObserved
                && exactBytesObserved
                && diskSpillObserved
                && temporaryPartCleanupObserved
                && javaListenersObserved
                && serverConfirmed
                && errorFree
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
                "cancelledTotalBytes": cancelledTotalBytes,
                "survivorTotalBytes": survivorTotalBytes,
                "cancelThreshold": cancelThreshold,
                "overlapThreshold": overlapThreshold,
                "cancelledProgressBytes": cancelledProgressBytes,
                "survivorProgressBytes": survivorProgressBytes,
                "progressEvents": progressEvents,
                "startEvents": startEvents,
                "requestEvents": requestEvents,
                "multipartRequestEvents": multipartRequestEvents,
                "multipartEntryEvents": multipartEntryEvents,
                "abortEvents": abortEvents,
                "removeEvents": removeEvents,
                "successEvents": successEvents,
                "errorEvents": errorEvents,
                "browserMaximumActive": browserMaximumActive,
                "overlapProgressObserved": overlapProgressObserved,
                "cancelIssued": cancelIssued,
                "abortFileName": abortFileName,
                "removedFileName": removedFileName,
                "successFileName": successFileName,
                "remainingFileCount": remainingFileCount,
                "remainingFileName": remainingFileName,
                "remainingFileComplete": remainingFileComplete,
                "serverHandlerCalls": serverHandlerCalls,
                "serverHandlerFailures": serverHandlerFailures,
                "serverHandlerFile": serverHandlerFile,
                "serverFailedHandlerFile": serverFailedHandlerFile,
                "serverResponseCalls": serverResponseCalls,
                "serverFailedResponses": serverFailedResponses,
                "serverSuccessfulResponses": serverSuccessfulResponses,
                "serverSuccessfulAcceptedFiles":
                    serverSuccessfulAcceptedFiles,
                "serverSuccessfulAcceptedFile":
                    serverSuccessfulAcceptedFile,
                "serverSuccessBytes": serverSuccessBytes,
                "serverSuccessDeclaredBytes": serverSuccessDeclaredBytes,
                "serverSpillBytes": serverSpillBytes,
                "serverMaximumTemporaryDirectories":
                    serverMaximumTemporaryDirectories,
                "serverMaximumTemporaryFiles": serverMaximumTemporaryFiles,
                "serverDiskSpillObserved": serverDiskSpillObserved,
                "serverTemporaryPartCleanupObserved":
                    serverTemporaryPartCleanupObserved,
                "serverMonitorError": serverMonitorError,
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
      const trigger = document.querySelector(
        '#framework-flow-concurrent-multipart-upload-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-concurrent-multipart-upload-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let startTransferScript = """
    (() => {
      const upload = document.querySelector(
        '#framework-flow-concurrent-multipart-upload');
      if (!upload
          || window.VaadinDesktopConcurrentMultipartUpload?.started === true) {
        return false;
      }
      const cancelledFileName =
        'flow-ipc-concurrent-multipart-cancel.bin';
      const survivorFileName =
        'flow-ipc-concurrent-multipart-survivor.bin';
      const cancelledTotalBytes = 4 * 1024 * 1024 + 173;
      const survivorTotalBytes = 3 * 1024 * 1024 + 257;
      const cancelThreshold = 256 * 1024;
      const overlapThreshold = 96 * 1024;
      const state = {
        started: true,
        complete: false,
        cancelledTotalBytes,
        survivorTotalBytes,
        cancelThreshold,
        overlapThreshold,
        cancelledProgressBytes: 0,
        survivorProgressBytes: 0,
        progressEvents: 0,
        startEvents: 0,
        requestEvents: 0,
        multipartRequestEvents: 0,
        multipartEntryEvents: 0,
        abortEvents: 0,
        removeEvents: 0,
        successEvents: 0,
        errorEvents: 0,
        browserMaximumActive: 0,
        overlapProgressObserved: false,
        cancelIssued: false,
        abortFileName: '',
        removedFileName: '',
        successFileName: '',
        error: ''
      };
      window.VaadinDesktopConcurrentMultipartUpload = state;
      const activeFiles = new Set();

      const makeFile = (length, multiplier, increment, name) => {
        const bytes = new Uint8Array(length);
        for (let index = 0; index < bytes.length; index += 1) {
          bytes[index] = ((index * multiplier + increment) & 0xff);
        }
        return new File(
          [bytes],
          name,
          {
            type: 'application/octet-stream',
            lastModified: 1700000000000
          });
      };
      const cancelledFile = makeFile(
        cancelledTotalBytes, 41, 19, cancelledFileName);
      const survivorFile = makeFile(
        survivorTotalBytes, 37, 11, survivorFileName);

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
      const filesAdded = () => {
        if (!Array.isArray(upload.files)) return false;
        const names = new Set(upload.files.map((file) => file?.name));
        return names.has(cancelledFileName) && names.has(survivorFileName);
      };
      const addFiles = () => {
        if (input) {
          try {
            const dataTransfer = new DataTransfer();
            dataTransfer.items.add(cancelledFile);
            dataTransfer.items.add(survivorFile);
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
        if (!filesAdded() && typeof upload._addFiles === 'function') {
          upload._addFiles([cancelledFile, survivorFile]);
        }
        if (!filesAdded()) return null;
        return [
          upload.files.find((file) => file?.name === cancelledFileName),
          upload.files.find((file) => file?.name === survivorFileName)
        ];
      };

      upload.addEventListener('upload-request', (event) => {
        state.requestEvents += 1;
        const file = event.detail?.file;
        const requestBody = event.detail?.requestBody;
        if (event.detail?.uploadFormat === 'multipart'
            && requestBody instanceof FormData) {
          state.multipartRequestEvents += 1;
          const fieldName = String(
            file?.formDataName || upload.formDataName || 'file');
          const entry = requestBody.get(fieldName);
          const expectedSize = file?.name === cancelledFileName
            ? cancelledTotalBytes
            : survivorTotalBytes;
          if (entry
              && entry.name === file?.name
              && Number(entry.size) === expectedSize
              && entry.type === 'application/octet-stream') {
            state.multipartEntryEvents += 1;
          }
        }
      });
      upload.addEventListener('upload-start', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.startEvents += 1;
        activeFiles.add(name);
        state.browserMaximumActive = Math.max(
          state.browserMaximumActive, activeFiles.size);
      });
      upload.addEventListener('upload-progress', (event) => {
        const file = event.detail?.file;
        const name = String(file?.name || '');
        const loaded = Number(file?.loaded || 0);
        state.progressEvents += 1;
        if (name === cancelledFileName) {
          state.cancelledProgressBytes = Math.max(
            state.cancelledProgressBytes, loaded);
        } else if (name === survivorFileName) {
          state.survivorProgressBytes = Math.max(
            state.survivorProgressBytes, loaded);
        }
        if (state.cancelledProgressBytes >= overlapThreshold
            && state.survivorProgressBytes >= overlapThreshold) {
          state.overlapProgressObserved = true;
        }
        if (!state.cancelIssued
            && state.overlapProgressObserved
            && name === cancelledFileName
            && loaded >= cancelThreshold
            && loaded < cancelledTotalBytes) {
          state.cancelIssued = true;
          upload.dispatchEvent(new CustomEvent('file-abort', {
            detail: { file }
          }));
        }
      });
      upload.addEventListener('upload-abort', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.abortEvents += 1;
        state.abortFileName = name;
        activeFiles.delete(name);
      });
      upload.addEventListener('file-remove', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.removeEvents += 1;
        state.removedFileName = name;
        activeFiles.delete(name);
      });
      upload.addEventListener('upload-success', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.successEvents += 1;
        state.successFileName = name;
        activeFiles.delete(name);
        if (name === survivorFileName) state.complete = true;
      });
      upload.addEventListener('upload-error', (event) => {
        const name = String(event.detail?.file?.name || '');
        state.errorEvents += 1;
        activeFiles.delete(name);
        state.error = String(
          event.detail?.file?.error
            || `concurrent multipart upload failed: ${name}`);
        state.complete = true;
      });

      try {
        const added = addFiles();
        if (!added?.[0] || !added?.[1]
            || typeof upload.uploadFiles !== 'function') {
          state.error = 'concurrent multipart file injection failed';
          state.complete = true;
          return true;
        }
        upload.uploadFiles(added);
      } catch (error) {
        state.error = String(
          error?.message || error
            || 'concurrent multipart upload failed');
        state.complete = true;
      }
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const state = window.VaadinDesktopConcurrentMultipartUpload || {};
      const route = document.querySelector(
        '#framework-flow-concurrent-multipart-upload-route');
      const upload = document.querySelector(
        '#framework-flow-concurrent-multipart-upload');
      const confirm = document.querySelector(
        '#framework-flow-concurrent-multipart-upload-confirm');
      const listenerStatus = document.querySelector(
        '#framework-flow-concurrent-multipart-upload-listener-status')
        ?.textContent || '';
      const status = document.querySelector(
        '#framework-flow-concurrent-multipart-upload-status')
        ?.textContent || '';
      const statusNumber = (name) => {
        const match = status.match(new RegExp(`${name}=(-?\\\\d+)`));
        return Number(match?.[1] ?? -1);
      };
      const statusText = (name) => String(
        status.match(new RegExp(`${name}=([^ ]*)`))?.[1] || '');
      const files = Array.isArray(upload?.files) ? upload.files : [];
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith(
            '/flow-concurrent-multipart-upload')
            && !!route && !!upload && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        started: state.started === true,
        transferComplete: state.complete === true,
        cancelledTotalBytes: Number(state.cancelledTotalBytes || 0),
        survivorTotalBytes: Number(state.survivorTotalBytes || 0),
        cancelThreshold: Number(state.cancelThreshold || 0),
        overlapThreshold: Number(state.overlapThreshold || 0),
        cancelledProgressBytes: Number(state.cancelledProgressBytes || 0),
        survivorProgressBytes: Number(state.survivorProgressBytes || 0),
        progressEvents: Number(state.progressEvents || 0),
        startEvents: Number(state.startEvents || 0),
        requestEvents: Number(state.requestEvents || 0),
        multipartRequestEvents: Number(state.multipartRequestEvents || 0),
        multipartEntryEvents: Number(state.multipartEntryEvents || 0),
        abortEvents: Number(state.abortEvents || 0),
        removeEvents: Number(state.removeEvents || 0),
        successEvents: Number(state.successEvents || 0),
        errorEvents: Number(state.errorEvents || 0),
        browserMaximumActive: Number(state.browserMaximumActive || 0),
        overlapProgressObserved: state.overlapProgressObserved === true,
        cancelIssued: state.cancelIssued === true,
        abortFileName: String(state.abortFileName || ''),
        removedFileName: String(state.removedFileName || ''),
        successFileName: String(state.successFileName || ''),
        remainingFileCount: files.length,
        remainingFileName: String(files[0]?.name || ''),
        remainingFileComplete: files[0]?.complete === true,
        serverHandlerCalls: statusNumber('handlerCalls'),
        serverHandlerFailures: statusNumber('handlerFailures'),
        serverHandlerFile: statusText('handlerFile'),
        serverFailedHandlerFile: statusText('failedHandlerFile'),
        serverResponseCalls: statusNumber('responseCalls'),
        serverFailedResponses: statusNumber('failedResponses'),
        serverSuccessfulResponses: statusNumber('successfulResponses'),
        serverSuccessfulAcceptedFiles: statusNumber('successfulAccepted'),
        serverSuccessfulAcceptedFile: statusText('successfulAcceptedFile'),
        serverSuccessBytes: statusNumber('successBytes'),
        serverSuccessDeclaredBytes: statusNumber('successDeclared'),
        serverSpillBytes: statusNumber('spillBytes'),
        serverMaximumTemporaryDirectories: statusNumber('maxTempDirs'),
        serverMaximumTemporaryFiles: statusNumber('maxTempFiles'),
        serverDiskSpillObserved: status.includes('diskSpill=true'),
        serverTemporaryPartCleanupObserved:
          status.includes('tempCleanup=true'),
        serverMonitorError: statusText('monitorError'),
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
