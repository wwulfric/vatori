import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleViteDevelopmentAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        let mode = host.config.flowWebAppSource?.mode ?? ""
        guard mode == "development-server" else {
            trace("send", "error", [
                "code": "viteDevelopmentAcceptanceModeMismatch",
                "mode": mode
            ])
            context.window.close()
            return
        }
        guard let probeFile = host.config.flowIpcViteHmrProbeFile else {
            trace("send", "error", [
                "code": "viteDevelopmentAcceptanceProbeMissing"
            ])
            context.window.close()
            return
        }
        FlowIpcViteDevelopmentAcceptance(
            context: context,
            probeFile: URL(fileURLWithPath: probeFile),
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcViteDevelopmentAcceptance {
    private static let baselineSource = "const moduleRevision = 'baseline';"
    private static let updatedSource = "const moduleRevision = 'updated';"

    private let context: FlowIpcWindowContext
    private let probeFile: URL
    private let deadline: Date
    private var originalSource: String?
    private var baselineObserved = false
    private var updatedObserved = false

    init(context: FlowIpcWindowContext, probeFile: URL, deadline: Date) {
        self.context = context
        self.probeFile = probeFile
        self.deadline = deadline
    }

    func start() {
        pollForBaseline()
    }

    private func pollForBaseline() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "viteDevelopmentAcceptanceBaselineTimeout",
                    poll: self.pollForBaseline)
                return
            }
            if snapshot.readyForBaseline {
                self.baselineObserved = true
                self.applyUpdate()
                return
            }
            self.retryOrFail(
                code: "viteDevelopmentAcceptanceBaselineTimeout",
                details: snapshot.tracePayload,
                poll: self.pollForBaseline)
        }
    }

    private func applyUpdate() {
        do {
            let source = try String(contentsOf: probeFile, encoding: .utf8)
            guard source.components(separatedBy: Self.baselineSource).count == 2 else {
                fail(code: "viteDevelopmentAcceptanceProbeInvalid")
                return
            }
            originalSource = source
            let updated = source.replacingOccurrences(
                of: Self.baselineSource,
                with: Self.updatedSource)
            try updated.write(to: probeFile, atomically: true, encoding: .utf8)
            pollForUpdate()
        } catch {
            fail(
                code: "viteDevelopmentAcceptanceProbeWriteFailed",
                details: ["message": error.localizedDescription])
        }
    }

    private func pollForUpdate() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "viteDevelopmentAcceptanceUpdateTimeout",
                    poll: self.pollForUpdate)
                return
            }
            if snapshot.readyForUpdate {
                self.updatedObserved = true
                self.finish(snapshot: snapshot)
                return
            }
            self.retryOrFail(
                code: "viteDevelopmentAcceptanceUpdateTimeout",
                details: snapshot.tracePayload,
                poll: self.pollForUpdate)
        }
    }

    private func inspect(_ completion: @escaping (Snapshot?) -> Void) {
        let script = """
        (() => {
          const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
          const probe = document.querySelector('#framework-vite-hmr-probe');
          const clients = window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients
            ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
            : [];
          return JSON.stringify({
            hmrConnected: probe?.dataset?.hmrConnected === 'true',
            revision: probe?.dataset?.moduleRevision || '',
            firstMeaningfulPaint:
              diagnostics.standardBundleFirstMeaningfulPaintReported === true,
            requestCount: Number(diagnostics.standardBundleOpaqueRequestCount || 0),
            responseCount: Number(diagnostics.standardBundleOpaqueResponseCount || 0),
            flowClientCount: clients.length,
            browserError: diagnostics.standardBundleBrowserError || '',
            requestError: diagnostics.standardBundleOpaqueRequestError || '',
            responseError: diagnostics.standardBundleOpaqueResponseError || ''
          });
        })()
        """
        context.webView.evaluateJavaScript(script) { result, error in
            guard error == nil,
                  let json = result as? String,
                  let data = json.data(using: .utf8),
                  let decoded = try? JSONDecoder().decode(Snapshot.self, from: data) else {
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
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5, execute: poll)
    }

    private func finish(snapshot: Snapshot) {
        let restored = restoreSource()
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": restored && snapshot.errorFree,
            "viteConnected": snapshot.hmrConnected,
            "baselineObserved": baselineObserved,
            "updatedObserved": updatedObserved,
            "sourceRestored": restored,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "opaqueRequestBridgeObserved": snapshot.requestCount >= 2,
            "opaqueResponseBridgeObserved": snapshot.responseCount >= 2,
            "errorFree": snapshot.errorFree,
            "mode": "development-server"
        ]) { _, new in new }
        trace("recv", "viteDevelopment.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(code: String, details: [String: Any] = [:]) {
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
            try originalSource.write(to: probeFile, atomically: true, encoding: .utf8)
            return true
        } catch {
            return false
        }
    }

    private struct Snapshot: Decodable {
        let hmrConnected: Bool
        let revision: String
        let firstMeaningfulPaint: Bool
        let requestCount: Int
        let responseCount: Int
        let flowClientCount: Int
        let browserError: String
        let requestError: String
        let responseError: String

        var errorFree: Bool {
            browserError.isEmpty && requestError.isEmpty && responseError.isEmpty
        }

        var readyForBaseline: Bool {
            hmrConnected
                && revision == "baseline"
                && firstMeaningfulPaint
                && requestCount >= 2
                && responseCount >= 2
                && flowClientCount > 0
                && errorFree
        }

        var readyForUpdate: Bool {
            hmrConnected && revision == "updated" && errorFree
        }

        var tracePayload: [String: Any] {
            [
                "hmrConnected": hmrConnected,
                "revision": revision,
                "firstMeaningfulPaint": firstMeaningfulPaint,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "flowClientCount": flowClientCount,
                "browserError": browserError,
                "requestError": requestError,
                "responseError": responseError
            ]
        }
    }
}
