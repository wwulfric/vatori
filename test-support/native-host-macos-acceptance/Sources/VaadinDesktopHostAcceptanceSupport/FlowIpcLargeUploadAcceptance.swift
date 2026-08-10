import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLargeUploadAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLargeUploadAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcLargeUploadAcceptance {
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
                self.retryOrFail(code: "largeUploadInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "largeUploadBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.rootRendered,
                      snapshot.firstMeaningfulPaint,
                      snapshot.pushMessageCount >= 1 else {
                    self.retryOrFail(
                        code: "largeUploadRootTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    next: .route,
                    failureCode: "largeUploadNavigationFailed")
            case .route:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "largeUploadRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startTransferScript,
                    next: .transfer,
                    failureCode: "largeUploadStartFailed")
            case .transfer:
                guard snapshot.transferComplete,
                      snapshot.javaListenerObserved else {
                    self.retryOrFail(
                        code: "largeUploadTransferTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.confirmScript,
                    next: .confirm,
                    failureCode: "largeUploadConfirmDispatchFailed")
            case .confirm:
                guard snapshot.serverConfirmed else {
                    self.retryOrFail(
                        code: "largeUploadServerConfirmationTimeout",
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
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 4,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 3,
            "pushObserved": snapshot.pushMessageCount >= 1,
            "largeUploadProgressObserved": snapshot.progressObserved,
            "largeUploadAbortObserved": snapshot.abortObserved,
            "largeUploadRemovalObserved": snapshot.removalObserved,
            "largeUploadRetryObserved": snapshot.retryObserved,
            "largeUploadSuccessObserved": snapshot.successObserved,
            "largeUploadExactBytesObserved": snapshot.exactBytesObserved,
            "largeUploadServerCancellationObserved":
                snapshot.serverCancellationObserved,
            "largeUploadJavaListenerObserved": snapshot.javaListenerObserved,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowLargeUpload.frameworkAcceptance", payload)
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
        let totalBytes: Int
        let cancelThreshold: Int
        let firstProgressBytes: Int
        let retryProgressBytes: Int
        let progressEvents: Int
        let startEvents: Int
        let abortEvents: Int
        let removeEvents: Int
        let successEvents: Int
        let errorEvents: Int
        let retryAdded: Bool
        let browserCancelled: Bool
        let serverCancelledBytes: Int
        let serverSuccessBytes: Int
        let serverDeclaredBytes: Int
        let listenerStatus: String
        let status: String
        let transferError: String
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

        var progressObserved: Bool {
            progressEvents >= 2
                && firstProgressBytes >= cancelThreshold
                && firstProgressBytes < totalBytes
                && retryProgressBytes == totalBytes
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

        var javaListenerObserved: Bool {
            listenerStatus.contains("Large upload listener: removed=1")
                && listenerStatus.contains("file=flow-ipc-large-upload.bin")
        }

        var serverCancellationObserved: Bool {
            status.contains("requests=2")
                && status.contains("cancelled=1")
                && serverCancelledBytes >= 128 * 1024
                && serverCancelledBytes < totalBytes
                && status.contains("cancelPattern=true")
        }

        var exactBytesObserved: Bool {
            status.contains("successes=1")
                && serverSuccessBytes == totalBytes
                && status.contains("successPattern=true")
                && status.contains("invalid=0")
                && status.contains("removed=1")
                && status.contains("file=flow-ipc-large-upload.bin")
                && status.contains("type=application/octet-stream")
                && serverDeclaredBytes == totalBytes
                && status.contains("total=\(totalBytes)")
        }

        var serverConfirmed: Bool {
            status.contains("Large upload: confirmed")
                && status.contains("clicks=1")
                && serverCancellationObserved
                && exactBytesObserved
        }

        var complete: Bool {
            transferComplete
                && pushMessageCount >= 1
                && progressObserved
                && abortObserved
                && removalObserved
                && retryObserved
                && successObserved
                && javaListenerObserved
                && serverCancellationObserved
                && exactBytesObserved
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
                "totalBytes": totalBytes,
                "cancelThreshold": cancelThreshold,
                "firstProgressBytes": firstProgressBytes,
                "retryProgressBytes": retryProgressBytes,
                "progressEvents": progressEvents,
                "startEvents": startEvents,
                "abortEvents": abortEvents,
                "removeEvents": removeEvents,
                "successEvents": successEvents,
                "errorEvents": errorEvents,
                "retryAdded": retryAdded,
                "browserCancelled": browserCancelled,
                "serverCancelledBytes": serverCancelledBytes,
                "serverSuccessBytes": serverSuccessBytes,
                "serverDeclaredBytes": serverDeclaredBytes,
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
        '#framework-flow-large-upload-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-large-upload-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let startTransferScript = """
    (() => {
      const upload = document.querySelector('#framework-flow-large-upload');
      if (!upload || window.VaadinDesktopLargeUpload?.started === true) {
        return false;
      }
      const totalBytes = 4 * 1024 * 1024 + 211;
      const cancelThreshold = 256 * 1024;
      const state = {
        started: true,
        complete: false,
        totalBytes,
        cancelThreshold,
        firstProgressBytes: 0,
        retryProgressBytes: 0,
        progressEvents: 0,
        startEvents: 0,
        abortEvents: 0,
        removeEvents: 0,
        successEvents: 0,
        errorEvents: 0,
        retryAdded: false,
        browserCancelled: false,
        error: ''
      };
      window.VaadinDesktopLargeUpload = state;

      const bytes = new Uint8Array(totalBytes);
      for (let index = 0; index < bytes.length; index += 1) {
        bytes[index] = ((index * 31 + 17) & 0xff);
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
        'flow-ipc-large-upload.bin',
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
              && loaded < totalBytes) {
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
        if (activeAttempt === 2) {
          state.complete = true;
        }
      });
      upload.addEventListener('upload-error', (event) => {
        state.errorEvents += 1;
        state.error = String(
          event.detail?.file?.error || 'large upload failed');
        state.complete = true;
      });

      try {
        startAttempt(1);
      } catch (error) {
        state.error = String(error?.message || error || 'large upload failed');
        state.complete = true;
      }
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const state = window.VaadinDesktopLargeUpload || {};
      const route = document.querySelector('#framework-flow-large-upload-route');
      const upload = document.querySelector('#framework-flow-large-upload');
      const confirm = document.querySelector('#framework-flow-large-upload-confirm');
      const listenerStatus = document.querySelector(
        '#framework-flow-large-upload-listener-status')?.textContent || '';
      const status = document.querySelector(
        '#framework-flow-large-upload-status')?.textContent || '';
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
          window.location.pathname.endsWith('/flow-large-upload')
            && !!route && !!upload && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        started: state.started === true,
        transferComplete: state.complete === true,
        totalBytes: Number(state.totalBytes || 0),
        cancelThreshold: Number(state.cancelThreshold || 0),
        firstProgressBytes: Number(state.firstProgressBytes || 0),
        retryProgressBytes: Number(state.retryProgressBytes || 0),
        progressEvents: Number(state.progressEvents || 0),
        startEvents: Number(state.startEvents || 0),
        abortEvents: Number(state.abortEvents || 0),
        removeEvents: Number(state.removeEvents || 0),
        successEvents: Number(state.successEvents || 0),
        errorEvents: Number(state.errorEvents || 0),
        retryAdded: state.retryAdded === true,
        browserCancelled: state.browserCancelled === true,
        serverCancelledBytes: statusNumber('cancelledBytes'),
        serverSuccessBytes: statusNumber('successBytes'),
        serverDeclaredBytes: statusNumber('declared'),
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
