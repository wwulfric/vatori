import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleJavaReloadDevelopmentAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        guard !javaReloadAcceptanceScheduled else {
            return
        }
        javaReloadAcceptanceScheduled = true
        let mode = host.config.flowWebAppSource?.mode ?? ""
        guard mode == "development-server" else {
            trace("send", "error", [
                "code": "javaReloadAcceptanceModeMismatch",
                "mode": mode
            ])
            context.window.close()
            return
        }
        guard let probeFile = host.config.flowIpcJavaReloadProbeFile else {
            trace("send", "error", [
                "code": "javaReloadAcceptanceProbeMissing"
            ])
            context.window.close()
            return
        }
        FlowIpcJavaReloadDevelopmentAcceptance(
            host: host,
            context: context,
            probeFile: URL(fileURLWithPath: probeFile),
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcJavaReloadDevelopmentAcceptance {
    private static let baselineSource =
        "private static final String REVISION = \"baseline\";"
    private static let updatedSource =
        "private static final String REVISION = \"updated\";"

    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let probeFile: URL
    private let deadline: Date
    private let initialWindowIdentifier: ObjectIdentifier
    private let initialWebViewIdentifier: ObjectIdentifier
    private let initialProcessIdentifier: Int32
    private var originalSource: String?
    private var baselineObserved = false
    private var updatedObserved = false

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
        initialProcessIdentifier = host.sidecarProcessIdentifier ?? 0
    }

    func start() {
        pollForBaseline()
    }

    private func pollForBaseline() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "javaReloadAcceptanceBaselineTimeout",
                    poll: self.pollForBaseline)
                return
            }
            if snapshot.ready && snapshot.revision == "baseline" {
                self.baselineObserved = true
                self.applyUpdate()
                return
            }
            self.retryOrFail(
                code: "javaReloadAcceptanceBaselineTimeout",
                details: snapshot.tracePayload,
                poll: self.pollForBaseline)
        }
    }

    private func applyUpdate() {
        do {
            let source = try String(
                contentsOf: probeFile, encoding: .utf8)
            guard source.components(
                separatedBy: Self.baselineSource).count == 2 else {
                fail(code: "javaReloadAcceptanceProbeInvalid")
                return
            }
            originalSource = source
            let updated = source.replacingOccurrences(
                of: Self.baselineSource,
                with: Self.updatedSource)
            try updated.write(
                to: probeFile, atomically: true, encoding: .utf8)
            pollForUpdate()
        } catch {
            fail(
                code: "javaReloadAcceptanceProbeWriteFailed",
                details: ["message": error.localizedDescription])
        }
    }

    private func pollForUpdate() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "javaReloadAcceptanceUpdateTimeout",
                    poll: self.pollForUpdate)
                return
            }
            let processIdentifier =
                self.host.sidecarProcessIdentifier ?? 0
            if snapshot.ready,
               snapshot.revision == "updated",
               self.context.recoveryGeneration >= 1,
               processIdentifier > 0,
               processIdentifier != self.initialProcessIdentifier {
                self.updatedObserved = true
                self.finish(
                    snapshot: snapshot,
                    processIdentifier: processIdentifier)
                return
            }
            self.retryOrFail(
                code: "javaReloadAcceptanceUpdateTimeout",
                details: snapshot.tracePayload.merging([
                    "recoveryGeneration":
                        self.context.recoveryGeneration,
                    "processIdentifier": processIdentifier
                ]) { _, replacement in replacement },
                poll: self.pollForUpdate)
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const clients = window.Vaadin?.Flow?.clients
            ? Object.keys(window.Vaadin.Flow.clients)
                .filter((name) => name !== 'TypeScript')
            : [];
          const pushStatus =
            document.querySelector('#ipc-standard-push-status')?.textContent || '';
          const probe = document.querySelector('#framework-java-reload-probe');
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
        context.webView.evaluateJavaScript(script) { result, error in
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
            deadline: .now() + 0.5, execute: poll)
    }

    private func finish(
        snapshot: Snapshot,
        processIdentifier: Int32
    ) {
        let restored = restoreSource()
        let processReplaced = initialProcessIdentifier > 0
            && processIdentifier > 0
            && initialProcessIdentifier != processIdentifier
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": restored
                && baselineObserved
                && updatedObserved
                && processReplaced
                && snapshot.error.isEmpty,
            "baselineObserved": baselineObserved,
            "updatedObserved": updatedObserved,
            "sourceRestored": restored,
            "processReplaced": processReplaced,
            "previousProcessIdentifier": initialProcessIdentifier,
            "processIdentifier": processIdentifier,
            "windowPreserved":
                initialWindowIdentifier == ObjectIdentifier(context.window),
            "webViewPreserved":
                initialWebViewIdentifier == ObjectIdentifier(context.webView),
            "webViewReloaded": context.recoveryGeneration >= 1,
            "recoveryGeneration": context.recoveryGeneration,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "requestBridgeRecovered":
                snapshot.requestCount >= 2
                    && snapshot.responseCount >= 2,
            "pushRecovered": snapshot.pushMessageCount >= 2,
            "errorFree": snapshot.error.isEmpty,
            "mode": "development-server"
        ]) { _, replacement in replacement }
        trace("recv", "javaReload.frameworkAcceptance", payload)
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
                to: probeFile, atomically: true, encoding: .utf8)
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
