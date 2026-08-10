import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLargeMultipartUploadAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLargeMultipartUploadAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcLargeMultipartUploadAcceptance {
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
                    code: "largeMultipartUploadInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "largeMultipartUploadBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.rootRendered,
                      snapshot.firstMeaningfulPaint,
                      snapshot.pushMessageCount >= 1 else {
                    self.retryOrFail(
                        code: "largeMultipartUploadRootTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    next: .route,
                    failureCode: "largeMultipartUploadNavigationFailed")
            case .route:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "largeMultipartUploadRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startTransferScript,
                    next: .transfer,
                    failureCode: "largeMultipartUploadStartFailed")
            case .transfer:
                guard snapshot.transferComplete,
                      snapshot.javaListenersObserved else {
                    self.retryOrFail(
                        code: "largeMultipartUploadTransferTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.confirmScript,
                    next: .confirm,
                    failureCode:
                        "largeMultipartUploadConfirmDispatchFailed")
            case .confirm:
                guard snapshot.serverConfirmed else {
                    self.retryOrFail(
                        code: "largeMultipartUploadServerConfirmationTimeout",
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
            "largeMultipartUploadRequestObserved":
                snapshot.multipartRequestObserved,
            "largeMultipartUploadProgressObserved":
                snapshot.progressObserved,
            "largeMultipartUploadAbortObserved": snapshot.abortObserved,
            "largeMultipartUploadRemovalObserved": snapshot.removalObserved,
            "largeMultipartUploadRetryObserved": snapshot.retryObserved,
            "largeMultipartUploadSuccessObserved": snapshot.successObserved,
            "largeMultipartUploadCancellationIsolationObserved":
                snapshot.cancellationIsolationObserved,
            "largeMultipartUploadExactBytesObserved":
                snapshot.exactBytesObserved,
            "largeMultipartUploadDiskSpillObserved":
                snapshot.diskSpillObserved,
            "largeMultipartUploadTemporaryPartCleanupObserved":
                snapshot.temporaryPartCleanupObserved,
            "largeMultipartUploadJavaListenersObserved":
                snapshot.javaListenersObserved,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace(
            "recv",
            "flowLargeMultipartUpload.frameworkAcceptance",
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
        let totalFileBytes: Int
        let cancelThreshold: Int
        let firstProgressBytes: Int
        let retryProgressBytes: Int
        let progressEvents: Int
        let startEvents: Int
        let requestEvents: Int
        let multipartRequestEvents: Int
        let multipartEntryEvents: Int
        let abortEvents: Int
        let removeEvents: Int
        let successEvents: Int
        let errorEvents: Int
        let retryAdded: Bool
        let browserCancelled: Bool
        let serverHandlerCalls: Int
        let serverHandlerFailures: Int
        let serverResponseCalls: Int
        let serverFailedResponses: Int
        let serverFailedAcceptedFiles: Int
        let serverSuccessfulResponses: Int
        let serverSuccessfulAcceptedFiles: Int
        let serverSuccessfulBytes: Int
        let serverDeclaredBytes: Int
        let serverSpillBytes: Int
        let failedResponseHasException: Bool
        let failureType: String
        let listenerStatus: String
        let status: String
        let transferError: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        private static let fileName =
            "flow-ipc-large-multipart-upload.bin"

        var errorFree: Bool {
            transferError.isEmpty
                && browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
                && errorEvents == 0
        }

        var multipartRequestObserved: Bool {
            requestEvents == 2
                && multipartRequestEvents == 2
                && multipartEntryEvents == 2
        }

        var progressObserved: Bool {
            progressEvents >= 3
                && firstProgressBytes >= cancelThreshold
                && firstProgressBytes < totalFileBytes
                && retryProgressBytes >= totalFileBytes
                && retryProgressBytes < totalFileBytes + 64 * 1024
        }

        var abortObserved: Bool {
            browserCancelled && abortEvents == 1
        }

        var removalObserved: Bool {
            removeEvents == 1
        }

        var retryObserved: Bool {
            retryAdded && startEvents == 2
        }

        var successObserved: Bool {
            successEvents == 1 && transferComplete
        }

        var cancellationIsolationObserved: Bool {
            serverHandlerCalls == 1
                && serverHandlerFailures == 0
                && serverResponseCalls == 1
                && serverFailedResponses == 0
                && serverFailedAcceptedFiles == -1
                && !failedResponseHasException
                && failureType.isEmpty
                && serverSuccessfulResponses == 1
                && serverSuccessfulAcceptedFiles == 1
        }

        var exactBytesObserved: Bool {
            serverSuccessfulBytes == totalFileBytes
                && serverDeclaredBytes == totalFileBytes
                && status.contains("successPattern=true")
                && status.contains("invalid=0")
                && status.contains("successfulAcceptedFile=\(Self.fileName)")
                && status.contains("successFile=\(Self.fileName)")
                && status.contains("type=application/octet-stream")
                && status.contains("total=\(totalFileBytes)")
        }

        var diskSpillObserved: Bool {
            serverSpillBytes == totalFileBytes
                && status.contains("diskSpill=true")
        }

        var temporaryPartCleanupObserved: Bool {
            status.contains("tempCleanup=true")
        }

        var javaListenersObserved: Bool {
            listenerStatus.contains("removed=1")
                && listenerStatus.contains("removedFile=\(Self.fileName)")
                && listenerStatus.contains("allFinished=1")
        }

        var serverConfirmed: Bool {
            status.contains("Large multipart upload: confirmed")
                && status.contains("clicks=1")
                && status.contains("removed=1")
                && status.contains("removedFile=\(Self.fileName)")
                && status.contains("allFinished=1")
                && cancellationIsolationObserved
                && exactBytesObserved
        }

        var complete: Bool {
            transferComplete
                && pushMessageCount >= 1
                && multipartRequestObserved
                && progressObserved
                && abortObserved
                && removalObserved
                && retryObserved
                && successObserved
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
                "totalFileBytes": totalFileBytes,
                "cancelThreshold": cancelThreshold,
                "firstProgressBytes": firstProgressBytes,
                "retryProgressBytes": retryProgressBytes,
                "progressEvents": progressEvents,
                "startEvents": startEvents,
                "requestEvents": requestEvents,
                "multipartRequestEvents": multipartRequestEvents,
                "multipartEntryEvents": multipartEntryEvents,
                "abortEvents": abortEvents,
                "removeEvents": removeEvents,
                "successEvents": successEvents,
                "errorEvents": errorEvents,
                "retryAdded": retryAdded,
                "browserCancelled": browserCancelled,
                "serverHandlerCalls": serverHandlerCalls,
                "serverHandlerFailures": serverHandlerFailures,
                "serverResponseCalls": serverResponseCalls,
                "serverFailedResponses": serverFailedResponses,
                "serverFailedAcceptedFiles": serverFailedAcceptedFiles,
                "serverSuccessfulResponses": serverSuccessfulResponses,
                "serverSuccessfulAcceptedFiles":
                    serverSuccessfulAcceptedFiles,
                "serverSuccessfulBytes": serverSuccessfulBytes,
                "serverDeclaredBytes": serverDeclaredBytes,
                "serverSpillBytes": serverSpillBytes,
                "failedResponseHasException": failedResponseHasException,
                "failureType": failureType,
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
        '#framework-flow-large-multipart-upload-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-large-multipart-upload-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let startTransferScript = """
    (() => {
      const upload = document.querySelector(
        '#framework-flow-large-multipart-upload');
      if (!upload
          || window.VaadinDesktopLargeMultipartUpload?.started === true) {
        return false;
      }
      const fileName = 'flow-ipc-large-multipart-upload.bin';
      const totalFileBytes = 4 * 1024 * 1024 + 307;
      const cancelThreshold = 256 * 1024;
      const state = {
        started: true,
        complete: false,
        totalFileBytes,
        cancelThreshold,
        firstProgressBytes: 0,
        retryProgressBytes: 0,
        progressEvents: 0,
        startEvents: 0,
        requestEvents: 0,
        multipartRequestEvents: 0,
        multipartEntryEvents: 0,
        abortEvents: 0,
        removeEvents: 0,
        successEvents: 0,
        errorEvents: 0,
        retryAdded: false,
        browserCancelled: false,
        error: ''
      };
      window.VaadinDesktopLargeMultipartUpload = state;

      const bytes = new Uint8Array(totalFileBytes);
      for (let index = 0; index < bytes.length; index += 1) {
        bytes[index] = ((index * 43 + 23) & 0xff);
      }
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
      let activeAttempt = 0;
      let retryScheduled = false;

      const makeFile = () => new File(
        [bytes],
        fileName,
        {
          type: 'application/octet-stream',
          lastModified: 1700000000000
        });
      const addFile = (file) => {
        const fileAdded = () => Array.isArray(upload.files)
          && upload.files.some((candidate) => candidate === file);
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
        return Array.isArray(upload.files)
          ? upload.files.find((candidate) => candidate === file)
          : null;
      };
      const startAttempt = (attempt) => {
        activeAttempt = attempt;
        const file = makeFile();
        const added = addFile(file);
        if (!added || typeof upload.uploadFiles !== 'function') {
          state.error = `attempt ${attempt} file injection failed`;
          state.complete = true;
          return;
        }
        if (attempt === 2) state.retryAdded = true;
        upload.uploadFiles(added);
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
          if (entry
              && entry.name === fileName
              && Number(entry.size) === totalFileBytes
              && entry.type === 'application/octet-stream') {
            state.multipartEntryEvents += 1;
          }
        }
      });
      upload.addEventListener('upload-start', () => {
        state.startEvents += 1;
      });
      upload.addEventListener('upload-progress', (event) => {
        const loaded = Number(event.detail?.file?.loaded || 0);
        state.progressEvents += 1;
        if (activeAttempt === 1) {
          state.firstProgressBytes = Math.max(
            state.firstProgressBytes, loaded);
          if (!state.browserCancelled
              && loaded >= cancelThreshold
              && loaded < totalFileBytes) {
            upload.dispatchEvent(new CustomEvent('file-abort', {
              detail: { file: event.detail.file }
            }));
          }
        } else if (activeAttempt === 2) {
          state.retryProgressBytes = Math.max(
            state.retryProgressBytes, loaded);
        }
      });
      upload.addEventListener('upload-abort', () => {
        state.abortEvents += 1;
        state.browserCancelled = true;
      });
      upload.addEventListener('file-remove', () => {
        state.removeEvents += 1;
        if (activeAttempt === 1 && !retryScheduled) {
          retryScheduled = true;
          setTimeout(() => startAttempt(2), 500);
        }
      });
      upload.addEventListener('upload-success', () => {
        state.successEvents += 1;
        if (activeAttempt === 2) state.complete = true;
      });
      upload.addEventListener('upload-error', (event) => {
        state.errorEvents += 1;
        state.error = String(
          event.detail?.file?.error || 'large multipart upload failed');
        state.complete = true;
      });

      try {
        startAttempt(1);
      } catch (error) {
        state.error = String(
          error?.message || error || 'large multipart upload failed');
        state.complete = true;
      }
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const state = window.VaadinDesktopLargeMultipartUpload || {};
      const route = document.querySelector(
        '#framework-flow-large-multipart-upload-route');
      const upload = document.querySelector(
        '#framework-flow-large-multipart-upload');
      const confirm = document.querySelector(
        '#framework-flow-large-multipart-upload-confirm');
      const listenerStatus = document.querySelector(
        '#framework-flow-large-multipart-upload-listener-status')
        ?.textContent || '';
      const status = document.querySelector(
        '#framework-flow-large-multipart-upload-status')?.textContent || '';
      const statusNumber = (name) => {
        const match = status.match(new RegExp(`${name}=(-?\\\\d+)`));
        return Number(match?.[1] ?? -1);
      };
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-large-multipart-upload')
            && !!route && !!upload && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        started: state.started === true,
        transferComplete: state.complete === true,
        totalFileBytes: Number(state.totalFileBytes || 0),
        cancelThreshold: Number(state.cancelThreshold || 0),
        firstProgressBytes: Number(state.firstProgressBytes || 0),
        retryProgressBytes: Number(state.retryProgressBytes || 0),
        progressEvents: Number(state.progressEvents || 0),
        startEvents: Number(state.startEvents || 0),
        requestEvents: Number(state.requestEvents || 0),
        multipartRequestEvents: Number(state.multipartRequestEvents || 0),
        multipartEntryEvents: Number(state.multipartEntryEvents || 0),
        abortEvents: Number(state.abortEvents || 0),
        removeEvents: Number(state.removeEvents || 0),
        successEvents: Number(state.successEvents || 0),
        errorEvents: Number(state.errorEvents || 0),
        retryAdded: state.retryAdded === true,
        browserCancelled: state.browserCancelled === true,
        serverHandlerCalls: statusNumber('handlerCalls'),
        serverHandlerFailures: statusNumber('handlerFailures'),
        serverResponseCalls: statusNumber('responseCalls'),
        serverFailedResponses: statusNumber('failedResponses'),
        serverFailedAcceptedFiles: statusNumber('failedAccepted'),
        serverSuccessfulResponses: statusNumber('successfulResponses'),
        serverSuccessfulAcceptedFiles: statusNumber('successfulAccepted'),
        serverSuccessfulBytes: statusNumber('successBytes'),
        serverDeclaredBytes: statusNumber('declared'),
        serverSpillBytes: statusNumber('spillBytes'),
        failedResponseHasException:
          status.includes('failedHasException=true'),
        failureType: String(
          status.match(/failureType=([^ ]*)/)?.[1] || ''),
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
