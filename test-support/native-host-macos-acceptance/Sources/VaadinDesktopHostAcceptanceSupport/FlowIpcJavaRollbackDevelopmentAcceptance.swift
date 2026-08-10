import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleJavaRollbackDevelopmentAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        guard !javaRollbackAcceptanceScheduled else {
            return
        }
        javaRollbackAcceptanceScheduled = true
        let mode = host.config.flowWebAppSource?.mode ?? ""
        guard mode == "development-server" else {
            trace("send", "error", [
                "code": "javaRollbackAcceptanceModeMismatch",
                "mode": mode
            ])
            context.window.close()
            return
        }
        guard let probeFile =
                host.config.flowIpcJavaRollbackProbeFile else {
            trace("send", "error", [
                "code": "javaRollbackAcceptanceProbeMissing"
            ])
            context.window.close()
            return
        }
        FlowIpcJavaRollbackDevelopmentAcceptance(
            host: host,
            context: context,
            probeFile: URL(fileURLWithPath: probeFile),
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcJavaRollbackDevelopmentAcceptance {
    private static let baselineSource =
        "private static final String STARTUP_MODE = \"normal\";"
    private static let failingSource =
        "private static final String STARTUP_MODE = \"exit-before-ready\";"

    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let probeFile: URL
    private let deadline: Date
    private let initialWindowIdentifier: ObjectIdentifier
    private let initialWebViewIdentifier: ObjectIdentifier
    private let initialProcessIdentifier: Int32
    private var originalSource: String?
    private var baselineObserved = false
    private var failureApplied = false

    init(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        probeFile: URL,
        deadline: Date
    ) {
        self.host = host
        self.context = context
        self.probeFile = probeFile
        self.deadline = deadline
        initialWindowIdentifier = ObjectIdentifier(context.window)
        initialWebViewIdentifier = ObjectIdentifier(context.webView)
        initialProcessIdentifier =
            host.sidecarProcessIdentifier ?? 0
    }

    func start() {
        pollForBaseline()
    }

    private func pollForBaseline() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "javaRollbackAcceptanceBaselineTimeout",
                    poll: self.pollForBaseline)
                return
            }
            if snapshot.ready
                    && snapshot.revision == "baseline" {
                self.baselineObserved = true
                self.applyFailingCandidate()
                return
            }
            self.retryOrFail(
                code: "javaRollbackAcceptanceBaselineTimeout",
                details: snapshot.tracePayload,
                poll: self.pollForBaseline)
        }
    }

    private func applyFailingCandidate() {
        do {
            let source = try String(
                contentsOf: probeFile, encoding: .utf8)
            guard source.components(
                separatedBy: Self.baselineSource).count == 2 else {
                fail(code: "javaRollbackAcceptanceProbeInvalid")
                return
            }
            originalSource = source
            let failing = source.replacingOccurrences(
                of: Self.baselineSource,
                with: Self.failingSource)
            try failing.write(
                to: probeFile,
                atomically: true,
                encoding: .utf8)
            failureApplied = true
            pollForRollback()
        } catch {
            fail(
                code: "javaRollbackAcceptanceProbeWriteFailed",
                details: [
                    "message": error.localizedDescription
                ])
        }
    }

    private func pollForRollback() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "javaRollbackAcceptanceTimeout",
                    poll: self.pollForRollback)
                return
            }
            let processIdentifier =
                self.host.sidecarProcessIdentifier ?? 0
            if snapshot.ready,
               snapshot.revision == "baseline",
               self.context.recoveryGeneration >= 1,
               processIdentifier > 0,
               processIdentifier
                    != self.initialProcessIdentifier {
                self.finish(
                    snapshot: snapshot,
                    processIdentifier: processIdentifier)
                return
            }
            self.retryOrFail(
                code: "javaRollbackAcceptanceTimeout",
                details: snapshot.tracePayload.merging([
                    "recoveryGeneration":
                        self.context.recoveryGeneration,
                    "processIdentifier": processIdentifier
                ]) { _, replacement in replacement },
                poll: self.pollForRollback)
        }
    }

    private func inspect(
        _ completion: @escaping (Snapshot?) -> Void
    ) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const clients = window.Vaadin?.Flow?.clients
            ? Object.keys(window.Vaadin.Flow.clients)
                .filter((name) => name !== 'TypeScript')
            : [];
          const pushStatus =
            document.querySelector('#ipc-standard-push-status')?.textContent || '';
          const probe =
            document.querySelector('#framework-java-reload-probe');
          const error = diagnostics.standardBundleBrowserError
            || diagnostics.standardBundleOpaqueRequestError
            || diagnostics.standardBundleOpaqueResponseError
            || diagnostics.standardBundlePushError
            || '';
          return JSON.stringify({
            ready: diagnostics.standardBundleFirstMeaningfulPaintReported === true
              && diagnostics.standardBundlePushBridgeInstalled === true
              && diagnostics.standardBundlePushConnected === true
              && Number(diagnostics.standardBundlePushMessageCount || 0) >= 2
              && pushStatus.includes('Standard push 2 READY')
              && Number(diagnostics.standardBundleOpaqueRequestCount || 0) >= 2
              && Number(diagnostics.standardBundleOpaqueResponseCount || 0) >= 2
              && clients.length > 0
              && !!probe
              && !error,
            revision: probe?.dataset?.revision || '',
            requestCount:
              Number(diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount:
              Number(diagnostics.standardBundleOpaqueResponseCount || 0),
            pushMessageCount:
              Number(diagnostics.standardBundlePushMessageCount || 0),
            flowClientCount: clients.length,
            error
          });
        })()
        """
        context.webView.evaluateJavaScript(script) {
            result,
            error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let decoded = try? JSONDecoder().decode(
                    Snapshot.self, from: data) else {
                completion(nil)
                return
            }
            completion(decoded)
        }
    }

    private func retryOrFail(
        code: String,
        details: [String: Any] = [:],
        poll: @escaping () -> Void
    ) {
        if Date() >= deadline {
            fail(code: code, details: details)
            return
        }
        DispatchQueue.main.asyncAfter(
            deadline: .now() + 0.5,
            execute: poll)
    }

    private func finish(
        snapshot: Snapshot,
        processIdentifier: Int32
    ) {
        let restored = restoreSource()
        let processReplaced =
            initialProcessIdentifier > 0
                && processIdentifier > 0
                && initialProcessIdentifier
                    != processIdentifier
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": restored
                && baselineObserved
                && failureApplied
                && processReplaced
                && snapshot.error.isEmpty,
            "baselineObserved": baselineObserved,
            "failingCandidateApplied": failureApplied,
            "stableRuntimeRestored":
                snapshot.revision == "baseline",
            "sourceRestored": restored,
            "processReplaced": processReplaced,
            "previousProcessIdentifier":
                initialProcessIdentifier,
            "processIdentifier": processIdentifier,
            "windowPreserved":
                initialWindowIdentifier
                    == ObjectIdentifier(context.window),
            "webViewPreserved":
                initialWebViewIdentifier
                    == ObjectIdentifier(context.webView),
            "webViewReloaded":
                context.recoveryGeneration >= 1,
            "recoveryGeneration":
                context.recoveryGeneration,
            "flowClientOwned":
                snapshot.flowClientCount > 0,
            "requestBridgeRecovered":
                snapshot.requestCount >= 2
                    && snapshot.responseCount >= 2,
            "pushRecovered":
                snapshot.pushMessageCount >= 2,
            "errorFree": snapshot.error.isEmpty,
            "mode": "development-server"
        ]) { _, replacement in replacement }
        trace(
            "recv",
            "javaRollback.frameworkAcceptance",
            payload)
        context.window.close()
    }

    private func fail(
        code: String,
        details: [String: Any] = [:]
    ) {
        let restored = restoreSource()
        var payload = details
        payload["code"] = code
        payload["sourceRestored"] = restored
        trace("send", "error", payload)
        context.window.close()
    }

    private func restoreSource() -> Bool {
        guard let originalSource else {
            return true
        }
        do {
            try originalSource.write(
                to: probeFile,
                atomically: true,
                encoding: .utf8)
            return true
        } catch {
            return false
        }
    }

    private struct Snapshot: Decodable {
        let ready: Bool
        let revision: String
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let error: String

        var tracePayload: [String: Any] {
            [
                "ready": ready,
                "revision": revision,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "error": error
            ]
        }
    }
}
