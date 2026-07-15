import Cocoa
import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeHooks {
    func configureUserContentController(
        _ userContentController: WKUserContentController,
        config: HostConfig
    ) {
        userContentController.addFlowIpcAutomationScripts(config: config)
    }

    func didOpenInitialFlowIpcWindow(host: WebViewHost) {
        if host.config.flowIpcAutoMultiWindow {
            host.openFlowIpcWindow(scope: .window("secondary"))
        }
    }

    func didFinishCompatibilityNavigation(host: WebViewHost, webView: WKWebView) {
        let diagnostics = diagnostics(for: host)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            diagnostics.inspect(webView)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            diagnostics.writeSnapshot(webView)
        }
    }

    func didFinishFlowIpcNavigation(host: WebViewHost, context: FlowIpcWindowContext) {
        let diagnostics = diagnostics(for: host)
        let verificationSchedule = FlowIpcVerificationSchedule(config: host.config)
        if !host.config.flowIpcAutoShutdown {
            for delay in verificationSchedule.inspectionDelaysSeconds {
                DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                    diagnostics.inspect(context.webView)
                }
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + verificationSchedule.snapshotDelaySeconds) {
            diagnostics.writeSnapshot(context.webView)
        }
        if host.config.flowIpcAutoShutdown {
            scheduleAutoShutdown(
                host: host,
                context: context,
                verificationSchedule: verificationSchedule)
        }
    }

    private func diagnostics(for host: WebViewHost) -> WebViewDiagnostics {
        if let webViewDiagnostics {
            return webViewDiagnostics
        }
        let diagnostics = WebViewDiagnostics(repoRoot: host.config.repoRoot)
        webViewDiagnostics = diagnostics
        return diagnostics
    }

    private func scheduleAutoShutdown(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        verificationSchedule: FlowIpcVerificationSchedule
    ) {
        if host.config.flowIpcAutoSidecarRestart {
            host.scheduleStandardBundleSidecarRestartAcceptance(
                context,
                timeoutSeconds: verificationSchedule.autoShutdownTimeoutSeconds)
            return
        }
        if host.config.flowIpcAutoMultiWindow {
            host.scheduleMultiWindowAcceptanceIfReady()
            return
        }
        if host.config.flowIpcAutoFrameworkUpdate {
            host.scheduleStandardBundleFrameworkAcceptance(
                context,
                timeoutSeconds: verificationSchedule.autoShutdownTimeoutSeconds)
            return
        }

        let deadline = Date().addingTimeInterval(verificationSchedule.autoShutdownTimeoutSeconds)
        pollAutoShutdownReadiness(host: host, context: context, deadline: deadline)
    }

    private func pollAutoShutdownReadiness(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        deadline: Date
    ) {
        let script = """
              (() => {
                const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
                const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients
                  ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
                  : [];
                if (Number(diagnostics.standardBundlePushMessageCount || 0) >= 1
                    && Number(diagnostics.standardBundlePushReconnectCount || 0) === 0
                    && !diagnostics.standardBundlePushReconnectTriggered) {
                  diagnostics.standardBundlePushReconnectTriggered = true;
                  const push = window.VaadinDesktopPush;
                  push?.reconnect?.();
                  const connectionId = diagnostics.standardBundlePushActiveConnectionId;
                  if (connectionId) {
                    push?.receive?.({
                      type: 'opaquePush',
                      payload: {
                        connectionId,
                        pushSequence:
                          Number(diagnostics.standardBundlePushLastSequence || 0) + 2,
                        uidl: '',
                        resync: false
                      }
                    });
                  }
                }
                const pushStatus = document.querySelector('#ipc-standard-push-status')
                  ?.textContent || '';
                const pushProbeRequired = pushStatus.length > 0;
                const pushProbeComplete = !pushProbeRequired || (
                  diagnostics.standardBundlePushBridgeInstalled === true
                    && diagnostics.standardBundlePushConnected === true
                    && Number(diagnostics.standardBundlePushMessageCount || 0) >= 3
                    && Number(diagnostics.standardBundlePushReconnectCount || 0) >= 1
                    && Number(diagnostics.standardBundlePushResyncCount || 0) >= 1
                    && pushStatus.includes('Standard push 2 READY')
                );
                return JSON.stringify({
                  complete: diagnostics.standardBundleFirstMeaningfulPaintReported === true
                    && diagnostics.standardBundleCookieBridgeInstalled === true
                    && pushProbeComplete
                    && Number(diagnostics.standardBundleOpaqueRequestCount || 0) >= 2
                    && Number(diagnostics.standardBundleOpaqueResponseCount || 0) >= 2
                    && (!diagnostics.standardBundleStreamingProbeRequired
                      || diagnostics.standardBundleStreamingProbeComplete === true)
                    && clients.length > 0
                    && !diagnostics.standardBundleBrowserError
                    && !diagnostics.standardBundleOpaqueRequestError
                    && !diagnostics.standardBundleOpaqueResponseError,
                  requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
                  responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
                  streamingProbeRequired:
                    diagnostics.standardBundleStreamingProbeRequired === true,
                  streamingProbeComplete:
                    diagnostics.standardBundleStreamingProbeComplete === true,
                  streamingUploadBytes:
                    Number(diagnostics.standardBundleStreamingUploadBytes || 0),
                  streamingDownloadBytes:
                    Number(diagnostics.standardBundleStreamingDownloadBytes || 0),
                  streamingProbeError: diagnostics.standardBundleStreamingProbeError || '',
                  cookieBridgeInstalled: diagnostics.standardBundleCookieBridgeInstalled === true,
                  cookieCount: Number(diagnostics.standardBundleCookieCount || 0),
                  pushConnected: diagnostics.standardBundlePushConnected === true,
                  pushMessageCount: Number(diagnostics.standardBundlePushMessageCount || 0),
                  pushReconnectCount: Number(diagnostics.standardBundlePushReconnectCount || 0),
                  pushResyncCount: Number(diagnostics.standardBundlePushResyncCount || 0),
                  pushProbeRequired,
                  pushProbeComplete,
                  pushStatus,
                  flowClientCount: clients.length,
                  browserError: diagnostics.standardBundleBrowserError || '',
                  requestError: diagnostics.standardBundleOpaqueRequestError || '',
                  responseError: diagnostics.standardBundleOpaqueResponseError || '',
                  pushError: diagnostics.standardBundlePushError || ''
                });
              })()
              """
        context.webView.evaluateJavaScript(script) { result, _ in
            let readiness = result as? String ?? "{}"
            if readiness.contains("\"complete\":true") || Date() >= deadline {
                let diagnostics = self.diagnostics(for: host)
                diagnostics.inspect(context.webView) {
                    diagnostics.writeSnapshot(context.webView) {
                        trace("recv", "window.closeRequested", [
                            "label": context.scope.windowId,
                            "reason": readiness.contains("\"complete\":true")
                                ? "flow-ipc-auto-shutdown-complete"
                                : "flow-ipc-auto-shutdown-timeout",
                            "readiness": readiness
                        ])
                        context.window.close()
                    }
                }
                return
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                self.pollAutoShutdownReadiness(
                    host: host,
                    context: context,
                    deadline: deadline)
            }
        }
    }
}
