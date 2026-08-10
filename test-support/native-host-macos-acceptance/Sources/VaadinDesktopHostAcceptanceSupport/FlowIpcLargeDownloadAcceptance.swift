import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleLargeDownloadAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcLargeDownloadAcceptance(
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcLargeDownloadAcceptance {
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
                self.retryOrFail(code: "largeDownloadInspectionFailed")
                return
            }
            guard snapshot.errorFree else {
                self.fail(
                    code: "largeDownloadBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .ready:
                guard snapshot.rootRendered,
                      snapshot.firstMeaningfulPaint,
                      snapshot.pushMessageCount >= 1 else {
                    self.retryOrFail(
                        code: "largeDownloadRootTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    next: .route,
                    failureCode: "largeDownloadNavigationFailed")
            case .route:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "largeDownloadRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startTransferScript,
                    next: .transfer,
                    failureCode: "largeDownloadStartFailed")
            case .transfer:
                guard snapshot.transferComplete else {
                    self.retryOrFail(
                        code: "largeDownloadTransferTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.confirmScript,
                    next: .confirm,
                    failureCode: "largeDownloadConfirmDispatchFailed")
            case .confirm:
                guard snapshot.serverConfirmed else {
                    self.retryOrFail(
                        code: "largeDownloadServerConfirmationTimeout",
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
            "largeDownloadInitialResponseObserved":
                snapshot.initialResponseObserved,
            "largeDownloadCancellationObserved": snapshot.cancellationObserved,
            "largeDownloadPartialResponseObserved":
                snapshot.partialResponseObserved,
            "largeDownloadContentRangeObserved":
                snapshot.contentRangeObserved,
            "largeDownloadExactReassemblyObserved": snapshot.patternValid,
            "largeDownloadServerCancellationObserved":
                snapshot.serverCancellationObserved,
            "largeDownloadJavaListenerObserved": snapshot.serverConfirmed,
            "errorFree": snapshot.errorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowLargeDownload.frameworkAcceptance", payload)
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
        let initialStatus: Int
        let resumeStatus: Int
        let totalBytes: Int
        let cancelledBytes: Int
        let resumedBytes: Int
        let acceptRanges: String
        let contentRange: String
        let browserCancelled: Bool
        let patternValid: Bool
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
        }

        var initialResponseObserved: Bool {
            started
                && initialStatus == 200
                && acceptRanges.lowercased() == "bytes"
                && totalBytes > 4 * 1024 * 1024
        }

        var cancellationObserved: Bool {
            browserCancelled
                && cancelledBytes >= 256 * 1024
                && cancelledBytes < totalBytes
        }

        var partialResponseObserved: Bool {
            resumeStatus == 206
                && resumedBytes == totalBytes - cancelledBytes
        }

        var contentRangeObserved: Bool {
            contentRange == "bytes \(cancelledBytes)-\(totalBytes - 1)/\(totalBytes)"
        }

        var serverCancellationObserved: Bool {
            status.contains("requests=2")
                && status.contains("ranges=1")
                && status.contains("cancelled=1")
                && status.contains("rangeStart=\(cancelledBytes)")
                && status.contains("total=\(totalBytes)")
        }

        var serverConfirmed: Bool {
            status.contains("Large download: confirmed")
                && serverCancellationObserved
        }

        var complete: Bool {
            transferComplete
                && pushMessageCount >= 1
                && initialResponseObserved
                && cancellationObserved
                && partialResponseObserved
                && contentRangeObserved
                && patternValid
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
                "initialStatus": initialStatus,
                "resumeStatus": resumeStatus,
                "totalBytes": totalBytes,
                "cancelledBytes": cancelledBytes,
                "resumedBytes": resumedBytes,
                "acceptRanges": acceptRanges,
                "contentRange": contentRange,
                "browserCancelled": browserCancelled,
                "patternValid": patternValid,
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
        '#framework-flow-large-download-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-large-download-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let startTransferScript = """
    (() => {
      const link = document.querySelector('#framework-flow-large-download-link');
      if (!link || window.VaadinDesktopLargeDownload?.started === true) {
        return false;
      }
      const state = {
        started: true,
        complete: false,
        initialStatus: 0,
        resumeStatus: 0,
        totalBytes: 0,
        cancelledBytes: 0,
        resumedBytes: 0,
        acceptRanges: '',
        contentRange: '',
        browserCancelled: false,
        patternValid: false,
        error: ''
      };
      window.VaadinDesktopLargeDownload = state;
      const url = new URL(link.getAttribute('href') || link.href, document.baseURI).href;
      const validPattern = (bytes, globalOffset) => {
        for (let index = 0; index < bytes.byteLength; index += 1) {
          if (bytes[index] !== (((globalOffset + index) * 31 + 17) & 0xff)) {
            return false;
          }
        }
        return true;
      };
      (async () => {
        const initial = await fetch(url, { credentials: 'same-origin' });
        state.initialStatus = initial.status;
        state.acceptRanges = initial.headers.get('accept-ranges') || '';
        state.totalBytes = Number(initial.headers.get('content-length') || 0);
        if (!initial.body) throw new Error('initial response body missing');
        const reader = initial.body.getReader();
        let prefixValid = true;
        while (state.cancelledBytes < 256 * 1024) {
          const result = await reader.read();
          if (result.done || !result.value) break;
          prefixValid = prefixValid
            && validPattern(result.value, state.cancelledBytes);
          state.cancelledBytes += result.value.byteLength;
        }
        if (state.cancelledBytes < 256 * 1024
            || state.cancelledBytes >= state.totalBytes) {
          throw new Error(`invalid cancellation offset ${state.cancelledBytes}`);
        }
        await reader.cancel('large-download-resume');
        state.browserCancelled = true;
        await new Promise((resolve) => setTimeout(resolve, 250));

        const resumed = await fetch(url, {
          credentials: 'same-origin',
          headers: { Range: `bytes=${state.cancelledBytes}-` }
        });
        state.resumeStatus = resumed.status;
        state.contentRange = resumed.headers.get('content-range') || '';
        const resumedBytes = new Uint8Array(await resumed.arrayBuffer());
        state.resumedBytes = resumedBytes.byteLength;
        state.patternValid = prefixValid
          && validPattern(resumedBytes, state.cancelledBytes)
          && state.cancelledBytes + state.resumedBytes === state.totalBytes;
        state.complete = true;
      })().catch((error) => {
        state.error = String(error?.message || error || 'large download failed');
        state.complete = true;
      });
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const state = window.VaadinDesktopLargeDownload || {};
      const route = document.querySelector('#framework-flow-large-download-route');
      const link = document.querySelector('#framework-flow-large-download-link');
      const confirm = document.querySelector('#framework-flow-large-download-confirm');
      const status = document.querySelector(
        '#framework-flow-large-download-status')?.textContent || '';
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-large-download')
            && !!route && !!link && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        started: state.started === true,
        transferComplete: state.complete === true,
        initialStatus: Number(state.initialStatus || 0),
        resumeStatus: Number(state.resumeStatus || 0),
        totalBytes: Number(state.totalBytes || 0),
        cancelledBytes: Number(state.cancelledBytes || 0),
        resumedBytes: Number(state.resumedBytes || 0),
        acceptRanges: String(state.acceptRanges || ''),
        contentRange: String(state.contentRange || ''),
        browserCancelled: state.browserCancelled === true,
        patternValid: state.patternValid === true,
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
