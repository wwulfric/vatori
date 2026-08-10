import Cocoa
import Darwin
import Foundation

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleInFlightRequestRecoveryAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        FlowIpcInFlightRequestRecoveryAcceptance(
            host: host,
            context: context,
            deadline: Date().addingTimeInterval(timeoutSeconds),
            initialWindowIdentifier: ObjectIdentifier(context.window),
            initialWebViewIdentifier: ObjectIdentifier(context.webView),
            initialProcessIdentifier: host.sidecarProcessIdentifier ?? 0
        ).start()
    }
}

private final class FlowIpcInFlightRequestRecoveryAcceptance {
    private enum Phase {
        case initialRoot
        case initialRoute
        case interruptedTransfer
        case waitingForDisconnect
        case waitingForLateProbe
        case waitingForRecovery
        case recoveredRoute
        case recoveredTransfer
        case recoveredConfirmation
    }

    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let deadline: Date
    private let initialWindowIdentifier: ObjectIdentifier
    private let initialWebViewIdentifier: ObjectIdentifier
    private let initialProcessIdentifier: Int32
    private var phase = Phase.initialRoot
    private var failedProcessIdentifier: Int32 = 0
    private var recoveredProcessIdentifier: Int32 = 0
    private var interruptedRequestId = ""
    private var interruptedProgressBytes = 0
    private var interruptedRequestFailed = false
    private var lateRequestRejected = false
    private var lateRequestCountStable = false
    private var disconnectedRequestCount = 0
    private var disconnectedPushOutboundCount = 0

    init(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        deadline: Date,
        initialWindowIdentifier: ObjectIdentifier,
        initialWebViewIdentifier: ObjectIdentifier,
        initialProcessIdentifier: Int32
    ) {
        self.host = host
        self.context = context
        self.deadline = deadline
        self.initialWindowIdentifier = initialWindowIdentifier
        self.initialWebViewIdentifier = initialWebViewIdentifier
        self.initialProcessIdentifier = initialProcessIdentifier
    }

    func start() {
        poll()
    }

    private func poll() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(code: "inFlightRequestInspectionFailed")
                return
            }
            guard snapshot.diagnosticsErrorFree else {
                self.fail(
                    code: "inFlightRequestBrowserRuntimeError",
                    details: snapshot.tracePayload)
                return
            }

            switch self.phase {
            case .initialRoot:
                guard snapshot.rootReady,
                      self.context.recoveryGeneration == 0,
                      self.initialProcessIdentifier > 0 else {
                    self.retryOrFail(
                        code: "inFlightRequestInitialRootTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.navigationScript,
                    next: .initialRoute,
                    failureCode: "inFlightRequestNavigationFailed")
            case .initialRoute:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "inFlightRequestRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startInterruptedTransferScript,
                    next: .interruptedTransfer,
                    failureCode: "inFlightRequestStartFailed")
            case .interruptedTransfer:
                if snapshot.successEvents > 0 || snapshot.progressBytes >= snapshot.totalBytes {
                    self.fail(
                        code: "inFlightRequestCompletedBeforeFailure",
                        details: snapshot.tracePayload)
                    return
                }
                guard snapshot.started,
                      snapshot.startEvents == 1,
                      snapshot.progressBytes >= snapshot.interruptThreshold,
                      !snapshot.requestId.isEmpty else {
                    self.retryOrFail(
                        code: "inFlightRequestProgressTimeout",
                        details: snapshot.tracePayload,
                        delay: 0.05)
                    return
                }
                self.interruptedRequestId = snapshot.requestId
                self.interruptedProgressBytes = snapshot.progressBytes
                guard self.failSidecar() else {
                    self.fail(
                        code: "inFlightRequestFailureInjectionFailed",
                        details: snapshot.tracePayload)
                    return
                }
                self.phase = .waitingForDisconnect
                self.retry(delay: 0.05)
            case .waitingForDisconnect:
                guard self.context.recoveryGeneration == 0 else {
                    self.fail(
                        code: "inFlightRequestDisconnectEvidenceLost",
                        details: snapshot.tracePayload)
                    return
                }
                guard snapshot.sidecarDisconnected,
                      snapshot.requestFailed,
                      snapshot.errorEvents == 1 else {
                    self.retryOrFail(
                        code: "inFlightRequestDisconnectTimeout",
                        details: snapshot.tracePayload,
                        delay: 0.05)
                    return
                }
                self.interruptedRequestFailed = true
                self.dispatch(
                    Self.lateRequestProbeScript,
                    next: .waitingForLateProbe,
                    failureCode: "inFlightRequestLateProbeDispatchFailed",
                    delay: 0.05)
            case .waitingForLateProbe:
                guard snapshot.lateProbeComplete else {
                    self.retryOrFail(
                        code: "inFlightRequestLateProbeTimeout",
                        details: snapshot.tracePayload,
                        delay: 0.05)
                    return
                }
                guard snapshot.lateProbeRejected,
                      snapshot.lateProbeRequestCountBefore
                        == snapshot.lateProbeRequestCountAfter,
                      snapshot.disconnectedRequestCount >= 1,
                      snapshot.disconnectedPushOutboundCount >= 2 else {
                    self.fail(
                        code: "inFlightRequestOldDocumentNotIsolated",
                        details: snapshot.tracePayload)
                    return
                }
                self.lateRequestRejected = true
                self.lateRequestCountStable = true
                self.disconnectedRequestCount = snapshot.disconnectedRequestCount
                self.disconnectedPushOutboundCount =
                    snapshot.disconnectedPushOutboundCount
                self.phase = .waitingForRecovery
                self.retry(delay: 0.1)
            case .waitingForRecovery:
                let processIdentifier = self.host.sidecarProcessIdentifier ?? 0
                guard self.context.recoveryGeneration >= 1,
                      processIdentifier > 0,
                      processIdentifier != self.failedProcessIdentifier,
                      snapshot.rootReady,
                      !snapshot.sidecarDisconnected else {
                    self.retryOrFail(
                        code: "inFlightRequestRecoveryTimeout",
                        details: snapshot.tracePayload.merging([
                            "processIdentifier": processIdentifier,
                            "recoveryGeneration": self.context.recoveryGeneration
                        ]) { _, replacement in replacement })
                    return
                }
                self.recoveredProcessIdentifier = processIdentifier
                self.dispatch(
                    Self.navigationScript,
                    next: .recoveredRoute,
                    failureCode: "inFlightRequestRecoveredNavigationFailed")
            case .recoveredRoute:
                guard snapshot.routeReady else {
                    self.retryOrFail(
                        code: "inFlightRequestRecoveredRouteTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.dispatch(
                    Self.startRecoveredTransferScript,
                    next: .recoveredTransfer,
                    failureCode: "inFlightRequestRecoveredStartFailed")
            case .recoveredTransfer:
                if snapshot.errorEvents > 0 || !snapshot.transferError.isEmpty {
                    self.fail(
                        code: "inFlightRequestRecoveredTransferFailed",
                        details: snapshot.tracePayload)
                    return
                }
                guard snapshot.transferComplete,
                      snapshot.successEvents == 1,
                      snapshot.progressBytes == snapshot.totalBytes,
                      !snapshot.requestId.isEmpty else {
                    self.retryOrFail(
                        code: "inFlightRequestRecoveredTransferTimeout",
                        details: snapshot.tracePayload,
                        delay: 0.1)
                    return
                }
                self.dispatch(
                    Self.confirmScript,
                    next: .recoveredConfirmation,
                    failureCode: "inFlightRequestConfirmationDispatchFailed")
            case .recoveredConfirmation:
                guard snapshot.serverConfirmed else {
                    self.retryOrFail(
                        code: "inFlightRequestServerConfirmationTimeout",
                        details: snapshot.tracePayload)
                    return
                }
                self.finish(snapshot)
            }
        }
    }

    private func failSidecar() -> Bool {
        guard let processIdentifier = host.sidecarProcessIdentifier,
              processIdentifier > 0,
              Darwin.kill(processIdentifier, SIGKILL) == 0 else {
            return false
        }
        failedProcessIdentifier = processIdentifier
        trace("send", "acceptance.sidecarFailureInjected", [
            "reason": "in-flight-opaque-request",
            "processIdentifier": processIdentifier,
            "requestId": interruptedRequestId,
            "progressBytes": interruptedProgressBytes
        ])
        return true
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
        failureCode: String,
        delay: Double = 0.25
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
            self.retry(delay: delay)
        }
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:],
        delay: Double = 0.25
    ) {
        guard Date() < deadline else {
            fail(code: code, details: details)
            return
        }
        retry(delay: delay)
    }

    private func retry(delay: Double) {
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
            self.poll()
        }
    }

    private func finish(_ snapshot: Snapshot) {
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": true,
            "standardBundleMode": snapshot.firstMeaningfulPaint,
            "interruptedRequestId": interruptedRequestId,
            "interruptedProgressBytes": interruptedProgressBytes,
            "interruptedRequestInFlight":
                interruptedProgressBytes >= snapshot.interruptThreshold
                    && interruptedProgressBytes < snapshot.totalBytes,
            "interruptedRequestFailed": interruptedRequestFailed,
            "oldDocumentSidecarDisconnected": true,
            "oldDocumentLateRequestRejected": lateRequestRejected,
            "oldDocumentRequestCountStable": lateRequestCountStable,
            "disconnectedRequestCount": disconnectedRequestCount,
            "oldDocumentPushRejected": disconnectedPushOutboundCount >= 2,
            "disconnectedPushOutboundCount": disconnectedPushOutboundCount,
            "previousProcessIdentifier": failedProcessIdentifier,
            "processIdentifier": recoveredProcessIdentifier,
            "processRestarted": failedProcessIdentifier > 0
                && recoveredProcessIdentifier > 0
                && failedProcessIdentifier != recoveredProcessIdentifier,
            "windowPreserved": initialWindowIdentifier
                == ObjectIdentifier(context.window),
            "webViewPreserved": initialWebViewIdentifier
                == ObjectIdentifier(context.webView),
            "webViewReloaded": context.recoveryGeneration >= 1,
            "recoveryGeneration": context.recoveryGeneration,
            "freshFlowClientOwned": snapshot.flowClientCount > 0,
            "requestBridgeRecovered": snapshot.requestCount >= 3
                && snapshot.responseCount >= 3,
            "pushRecovered": snapshot.pushMessageCount >= 1,
            "recoveredRequestId": snapshot.requestId,
            "recoveredRequestComplete": snapshot.transferComplete,
            "recoveredRequestExactBytes": snapshot.progressBytes
                == snapshot.totalBytes,
            "javaConfirmationObserved": snapshot.serverConfirmed,
            "errorFree": snapshot.diagnosticsErrorFree
        ]) { _, replacement in replacement }
        trace("recv", "flowInFlightRequestRecovery.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
        var payload = details
        payload["code"] = code
        payload["phase"] = String(describing: phase)
        payload["recoveryGeneration"] = context.recoveryGeneration
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
        let sidecarDisconnected: Bool
        let disconnectedRequestCount: Int
        let disconnectedPushOutboundCount: Int
        let started: Bool
        let transferComplete: Bool
        let totalBytes: Int
        let interruptThreshold: Int
        let progressBytes: Int
        let progressEvents: Int
        let startEvents: Int
        let errorEvents: Int
        let successEvents: Int
        let requestFailed: Bool
        let requestId: String
        let lateProbeComplete: Bool
        let lateProbeRejected: Bool
        let lateProbeRequestCountBefore: Int
        let lateProbeRequestCountAfter: Int
        let transferError: String
        let lateProbeError: String
        let status: String
        let browserError: String
        let requestError: String
        let responseError: String
        let pushError: String

        var rootReady: Bool {
            rootRendered
                && firstMeaningfulPaint
                && requestCount >= 2
                && responseCount >= 2
                && pushMessageCount >= 1
                && flowClientCount > 0
        }

        var diagnosticsErrorFree: Bool {
            browserError.isEmpty
                && requestError.isEmpty
                && responseError.isEmpty
                && pushError.isEmpty
        }

        var serverConfirmed: Bool {
            status.contains("In-flight request: confirmed")
                && status.contains("clicks=1")
                && status.contains("requests=1")
                && status.contains("successes=1")
                && status.contains("successBytes=\(totalBytes)")
                && status.contains("invalid=0")
                && status.contains("file=flow-ipc-inflight-recovered.bin")
                && status.contains("type=application/octet-stream")
                && status.contains("declared=\(totalBytes)")
                && status.contains("total=\(totalBytes)")
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
                "sidecarDisconnected": sidecarDisconnected,
                "disconnectedRequestCount": disconnectedRequestCount,
                "disconnectedPushOutboundCount":
                    disconnectedPushOutboundCount,
                "started": started,
                "transferComplete": transferComplete,
                "totalBytes": totalBytes,
                "interruptThreshold": interruptThreshold,
                "progressBytes": progressBytes,
                "progressEvents": progressEvents,
                "startEvents": startEvents,
                "errorEvents": errorEvents,
                "successEvents": successEvents,
                "requestFailed": requestFailed,
                "requestId": requestId,
                "lateProbeComplete": lateProbeComplete,
                "lateProbeRejected": lateProbeRejected,
                "lateProbeRequestCountBefore": lateProbeRequestCountBefore,
                "lateProbeRequestCountAfter": lateProbeRequestCountAfter,
                "transferError": transferError,
                "lateProbeError": lateProbeError,
                "status": status,
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
        '#framework-flow-in-flight-request-open-route');
      if (!trigger || typeof trigger.click !== 'function') return false;
      trigger.click();
      return true;
    })()
    """

    private static let startInterruptedTransferScript = transferScript(
        fileName: "flow-ipc-inflight-interrupted.bin")

    private static let startRecoveredTransferScript = transferScript(
        fileName: "flow-ipc-inflight-recovered.bin")

    private static func transferScript(fileName: String) -> String {
        """
        (() => {
          const upload = document.querySelector(
            '#framework-flow-in-flight-request-upload');
          if (!upload || window.VaadinDesktopInFlightRequest?.started === true) {
            return false;
          }
          const totalBytes = 4 * 1024 * 1024 + 509;
          const interruptThreshold = 256 * 1024;
          const state = {
            started: true,
            complete: false,
            totalBytes,
            interruptThreshold,
            progressBytes: 0,
            progressEvents: 0,
            startEvents: 0,
            errorEvents: 0,
            successEvents: 0,
            requestFailed: false,
            requestId: '',
            lateProbeStarted: false,
            lateProbeComplete: false,
            lateProbeRejected: false,
            lateProbeRequestCountBefore: -1,
            lateProbeRequestCountAfter: -1,
            lateProbeError: '',
            error: ''
          };
          window.VaadinDesktopInFlightRequest = state;

          const bytes = new Uint8Array(totalBytes);
          for (let index = 0; index < bytes.length; index += 1) {
            bytes[index] = ((index * 29 + 7) & 0xff);
          }
          const file = new File(
            [bytes],
            '\(fileName)',
            {
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
          const xhrPrototype = window.XMLHttpRequest?.prototype;
          const originalSend = xhrPrototype?.send;
          if (!xhrPrototype || typeof originalSend !== 'function') {
            state.error = 'opaque XHR request tracking is unavailable';
            state.complete = true;
            return true;
          }
          const captureRequestId = (xhr, attempt = 0) => {
            const requestId = String(xhr?._requestId || '');
            if (requestId) {
              state.requestId = requestId;
              return;
            }
            if (attempt >= 200) {
              state.error = 'request id capture failed';
              state.complete = true;
              return;
            }
            setTimeout(() => captureRequestId(xhr, attempt + 1), 5);
          };
          xhrPrototype.send = function trackedSend(body) {
            const result = originalSend.call(this, body);
            if (String(body?.name || '') === '\(fileName)') {
              captureRequestId(this);
            }
            return result;
          };

          upload.addEventListener('upload-start', () => {
            state.startEvents += 1;
          });
          upload.addEventListener('upload-progress', (event) => {
            state.progressEvents += 1;
            state.progressBytes = Math.max(
              state.progressBytes,
              Number(event.detail?.file?.loaded || 0));
          });
          upload.addEventListener('upload-error', (event) => {
            state.errorEvents += 1;
            state.requestFailed = true;
            state.error = String(
              event.detail?.file?.error || 'request failed');
          });
          upload.addEventListener('upload-success', () => {
            state.successEvents += 1;
            state.complete = true;
          });

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
          const added = Array.isArray(upload.files)
            ? upload.files.find((candidate) => candidate === file)
            : null;
          if (!added || typeof upload.uploadFiles !== 'function') {
            state.error = 'file injection failed';
            state.complete = true;
            return true;
          }
          upload.uploadFiles(added);
          return true;
        })()
        """
    }

    private static let lateRequestProbeScript = """
    (() => {
      const state = window.VaadinDesktopInFlightRequest;
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      if (!state || state.lateProbeStarted) return false;
      state.lateProbeStarted = true;
      state.lateProbeRequestCountBefore = Number(
        diagnostics.standardBundleOpaqueRequestCount || 0);
      window.VaadinDesktopPush?.reconnect?.();
      window.fetch('?vatori-inflight-old-document-probe=1')
        .then(() => {
          state.lateProbeError = 'old document request unexpectedly resolved';
          state.lateProbeComplete = true;
          state.lateProbeRequestCountAfter = Number(
            diagnostics.standardBundleOpaqueRequestCount || 0);
        })
        .catch((error) => {
          const message = String(error?.message || error || '');
          state.lateProbeRejected = message.includes('sidecar disconnected');
          state.lateProbeError = state.lateProbeRejected ? '' : message;
          state.lateProbeComplete = true;
          state.lateProbeRequestCountAfter = Number(
            diagnostics.standardBundleOpaqueRequestCount || 0);
        });
      return true;
    })()
    """

    private static let confirmScript = """
    (() => {
      const button = document.querySelector(
        '#framework-flow-in-flight-request-confirm');
      if (!button || typeof button.click !== 'function') return false;
      button.click();
      return true;
    })()
    """

    private static let inspectionScript = """
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
      const state = window.VaadinDesktopInFlightRequest || {};
      const route = document.querySelector(
        '#framework-flow-in-flight-request-route');
      const upload = document.querySelector(
        '#framework-flow-in-flight-request-upload');
      const confirm = document.querySelector(
        '#framework-flow-in-flight-request-confirm');
      const status = document.querySelector(
        '#framework-flow-in-flight-request-status')?.textContent || '';
      const clients = window.Vaadin?.Flow?.clients
        ? Object.keys(window.Vaadin.Flow.clients)
          .filter((name) => name !== 'TypeScript')
        : [];
      return JSON.stringify({
        rootRendered: !!document.querySelector('#framework-root') || !!route,
        firstMeaningfulPaint:
          diagnostics.standardBundleFirstMeaningfulPaintReported === true,
        routeReady:
          window.location.pathname.endsWith('/flow-in-flight-request')
            && !!route && !!upload && !!confirm,
        requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
        responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
        pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
        flowClientCount: clients.length,
        sidecarDisconnected:
          diagnostics.standardBundleSidecarDisconnected === true,
        disconnectedRequestCount: Number(
          diagnostics.standardBundleOpaqueDisconnectedRequestCount || 0),
        disconnectedPushOutboundCount: Number(
          diagnostics.standardBundlePushDisconnectedOutboundCount || 0),
        started: state.started === true,
        transferComplete: state.complete === true,
        totalBytes: Number(state.totalBytes || 0),
        interruptThreshold: Number(state.interruptThreshold || 0),
        progressBytes: Number(state.progressBytes || 0),
        progressEvents: Number(state.progressEvents || 0),
        startEvents: Number(state.startEvents || 0),
        errorEvents: Number(state.errorEvents || 0),
        successEvents: Number(state.successEvents || 0),
        requestFailed: state.requestFailed === true,
        requestId: String(state.requestId || ''),
        lateProbeComplete: state.lateProbeComplete === true,
        lateProbeRejected: state.lateProbeRejected === true,
        lateProbeRequestCountBefore: Number(
          state.lateProbeRequestCountBefore ?? -1),
        lateProbeRequestCountAfter: Number(
          state.lateProbeRequestCountAfter ?? -1),
        transferError: String(state.error || ''),
        lateProbeError: String(state.lateProbeError || ''),
        status,
        browserError: String(diagnostics.standardBundleBrowserError || ''),
        requestError: String(diagnostics.standardBundleOpaqueRequestError || ''),
        responseError: String(diagnostics.standardBundleOpaqueResponseError || ''),
        pushError: String(diagnostics.standardBundlePushError || '')
      });
    })()
    """
}
