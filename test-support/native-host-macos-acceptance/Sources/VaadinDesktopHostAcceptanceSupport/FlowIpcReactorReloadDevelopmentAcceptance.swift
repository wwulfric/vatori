import Foundation
import WebKit

extension FlowIpcAcceptanceRuntimeObserver {
    func scheduleReactorReloadDevelopmentAcceptance(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        timeoutSeconds: Double
    ) {
        guard !reactorReloadAcceptanceScheduled else {
            return
        }
        reactorReloadAcceptanceScheduled = true
        let mode = host.config.flowWebAppSource?.mode ?? ""
        guard mode == "development-server" else {
            trace("send", "error", [
                "code": "reactorReloadAcceptanceModeMismatch",
                "mode": mode
            ])
            context.window.close()
            return
        }
        guard let probeFile = host.config.flowIpcReactorReloadProbeFile,
              let applicationDirectory =
                host.config.flowIpcReactorReloadApplicationDirectory,
              let sourceFile =
                host.config.flowIpcEffectiveMavenSourceFile,
              let resourceFile =
                host.config.flowIpcEffectiveMavenResourceFile else {
            trace("send", "error", [
                "code": "reactorReloadAcceptanceProbeMissing"
            ])
            context.window.close()
            return
        }
        FlowIpcReactorReloadDevelopmentAcceptance(
            host: host,
            context: context,
            probeFile: URL(fileURLWithPath: probeFile),
            profileSourceFile: URL(fileURLWithPath: sourceFile),
            profileResourceFile: URL(fileURLWithPath: resourceFile),
            applicationDirectory: URL(
                fileURLWithPath: applicationDirectory,
                isDirectory: true),
            deadline: Date().addingTimeInterval(timeoutSeconds)
        ).start()
    }
}

private final class FlowIpcReactorReloadDevelopmentAcceptance {
    private static let baselineConfiguration =
        "<reactor.reload.revision>baseline</reactor.reload.revision>"
    private static let updatedConfiguration =
        "<reactor.reload.revision>updated</reactor.reload.revision>"
    private static let baselineSource =
        "private static final String SOURCE_REVISION = \"baseline\";"
    private static let updatedSource =
        "private static final String SOURCE_REVISION = \"updated\";"
    private static let baselineResource = "baseline"
    private static let updatedResource = "updated"

    private let host: WebViewHost
    private let context: FlowIpcWindowContext
    private let probeFile: URL
    private let profileSourceFile: URL
    private let profileResourceFile: URL
    private let applicationDirectory: URL
    private let deadline: Date
    private let initialWindowIdentifier: ObjectIdentifier
    private let initialWebViewIdentifier: ObjectIdentifier
    private let initialProcessIdentifier: Int32
    private var configurationProcessIdentifier: Int32 = 0
    private var sourceProcessIdentifier: Int32 = 0
    private var originalConfiguration: String?
    private var originalSource: String?
    private var originalResource: String?
    private var baselineObserved = false
    private var configurationUpdatedObserved = false
    private var sourceUpdatedObserved = false
    private var resourceUpdatedObserved = false
    private var buildConfigurationChanged = false
    private var profileSourceChanged = false
    private var profileResourceChanged = false

    init(
        host: WebViewHost,
        context: FlowIpcWindowContext,
        probeFile: URL,
        profileSourceFile: URL,
        profileResourceFile: URL,
        applicationDirectory: URL,
        deadline: Date
    ) {
        self.host = host
        self.context = context
        self.probeFile = probeFile
        self.profileSourceFile = profileSourceFile
        self.profileResourceFile = profileResourceFile
        self.applicationDirectory = applicationDirectory
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
                    code: "reactorReloadAcceptanceBaselineTimeout",
                    poll: self.pollForBaseline)
                return
            }
            if snapshot.ready,
               snapshot.revision == "baseline",
               snapshot.profileModuleAvailable,
               snapshot.sourceRevision == "baseline",
               snapshot.resourceRevision == "baseline" {
                self.baselineObserved = true
                self.applyConfigurationUpdate()
                return
            }
            self.retryOrFail(
                code: "reactorReloadAcceptanceBaselineTimeout",
                details: snapshot.tracePayload,
                poll: self.pollForBaseline)
        }
    }

    private func applyConfigurationUpdate() {
        do {
            let configuration = try String(
                contentsOf: probeFile, encoding: .utf8)
            guard configuration.components(
                separatedBy: Self.baselineConfiguration).count == 2 else {
                fail(code: "reactorReloadAcceptanceProbeInvalid")
                return
            }
            originalConfiguration = configuration
            let updated = configuration.replacingOccurrences(
                of: Self.baselineConfiguration,
                with: Self.updatedConfiguration)
            try updated.write(
                to: probeFile, atomically: true, encoding: .utf8)
            buildConfigurationChanged = true
            pollForConfigurationUpdate()
        } catch {
            fail(
                code: "reactorReloadAcceptanceProbeWriteFailed",
                details: ["message": error.localizedDescription])
        }
    }

    private func pollForConfigurationUpdate() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "reactorReloadAcceptanceUpdateTimeout",
                    poll: self.pollForConfigurationUpdate)
                return
            }
            let processIdentifier =
                self.host.sidecarProcessIdentifier ?? 0
            if snapshot.ready,
               snapshot.revision == "updated",
               snapshot.profileModuleAvailable,
               snapshot.sourceRevision == "baseline",
               snapshot.resourceRevision == "baseline",
               self.context.recoveryGeneration >= 1,
               processIdentifier > 0,
               processIdentifier != self.initialProcessIdentifier {
                self.configurationUpdatedObserved = true
                self.configurationProcessIdentifier = processIdentifier
                self.applySourceUpdate()
                return
            }
            self.retryOrFail(
                code: "reactorReloadAcceptanceUpdateTimeout",
                details: snapshot.tracePayload.merging([
                    "recoveryGeneration": self.context.recoveryGeneration,
                    "processIdentifier": processIdentifier
                ]) { _, replacement in replacement },
                poll: self.pollForConfigurationUpdate)
        }
    }

    private func applySourceUpdate() {
        do {
            let source = try String(
                contentsOf: profileSourceFile, encoding: .utf8)
            guard source.components(
                separatedBy: Self.baselineSource).count == 2 else {
                fail(code: "effectiveMavenSourceProbeInvalid")
                return
            }
            originalSource = source
            let updated = source.replacingOccurrences(
                of: Self.baselineSource,
                with: Self.updatedSource)
            try updated.write(
                to: profileSourceFile,
                atomically: true,
                encoding: .utf8)
            profileSourceChanged = true
            pollForSourceUpdate()
        } catch {
            fail(
                code: "effectiveMavenSourceWriteFailed",
                details: ["message": error.localizedDescription])
        }
    }

    private func pollForSourceUpdate() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "effectiveMavenSourceUpdateTimeout",
                    poll: self.pollForSourceUpdate)
                return
            }
            let processIdentifier =
                self.host.sidecarProcessIdentifier ?? 0
            if snapshot.ready,
               snapshot.revision == "updated",
               snapshot.profileModuleAvailable,
               snapshot.sourceRevision == "updated",
               snapshot.resourceRevision == "baseline",
               self.context.recoveryGeneration >= 2,
               processIdentifier > 0,
               processIdentifier != self.configurationProcessIdentifier {
                self.sourceUpdatedObserved = true
                self.sourceProcessIdentifier = processIdentifier
                self.applyResourceUpdate()
                return
            }
            self.retryOrFail(
                code: "effectiveMavenSourceUpdateTimeout",
                details: snapshot.tracePayload.merging([
                    "recoveryGeneration": self.context.recoveryGeneration,
                    "processIdentifier": processIdentifier
                ]) { _, replacement in replacement },
                poll: self.pollForSourceUpdate)
        }
    }

    private func applyResourceUpdate() {
        do {
            let resource = try String(
                contentsOf: profileResourceFile, encoding: .utf8)
            guard resource.trimmingCharacters(
                in: .whitespacesAndNewlines) == Self.baselineResource else {
                fail(code: "effectiveMavenResourceProbeInvalid")
                return
            }
            originalResource = resource
            let updated = resource.replacingOccurrences(
                of: Self.baselineResource,
                with: Self.updatedResource)
            try updated.write(
                to: profileResourceFile,
                atomically: true,
                encoding: .utf8)
            profileResourceChanged = true
            pollForResourceUpdate()
        } catch {
            fail(
                code: "effectiveMavenResourceWriteFailed",
                details: ["message": error.localizedDescription])
        }
    }

    private func pollForResourceUpdate() {
        inspect { snapshot in
            guard let snapshot else {
                self.retryOrFail(
                    code: "effectiveMavenResourceUpdateTimeout",
                    poll: self.pollForResourceUpdate)
                return
            }
            let processIdentifier =
                self.host.sidecarProcessIdentifier ?? 0
            if snapshot.ready,
               snapshot.revision == "updated",
               snapshot.profileModuleAvailable,
               snapshot.sourceRevision == "updated",
               snapshot.resourceRevision == "updated",
               self.context.recoveryGeneration >= 3,
               processIdentifier > 0,
               processIdentifier != self.sourceProcessIdentifier {
                self.resourceUpdatedObserved = true
                self.finish(
                    snapshot: snapshot,
                    processIdentifier: processIdentifier)
                return
            }
            self.retryOrFail(
                code: "effectiveMavenResourceUpdateTimeout",
                details: snapshot.tracePayload.merging([
                    "recoveryGeneration": self.context.recoveryGeneration,
                    "processIdentifier": processIdentifier
                ]) { _, replacement in replacement },
                poll: self.pollForResourceUpdate)
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
          const probe =
            document.querySelector('#framework-reactor-reload-probe');
          const profileProbe =
            document.querySelector('#framework-effective-maven-profile-probe');
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
              && !!profileProbe
              && !error,
            revision: probe?.dataset?.revision || '',
            profileModuleAvailable:
              profileProbe?.dataset?.profileModuleAvailable === 'true',
            sourceRevision: profileProbe?.dataset?.sourceRevision || '',
            resourceRevision: profileProbe?.dataset?.resourceRevision || '',
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
        let restored = restoreFixtures()
        let processReplaced = initialProcessIdentifier > 0
            && processIdentifier > 0
            && initialProcessIdentifier != processIdentifier
        let applicationPath = applicationDirectory
            .standardizedFileURL.path + "/"
        let dependentModuleObserved = !probeFile
            .standardizedFileURL.path.hasPrefix(applicationPath)
        let profileModuleObserved = !profileSourceFile
            .standardizedFileURL.path.hasPrefix(applicationPath)
            && !profileResourceFile.standardizedFileURL.path
                .hasPrefix(applicationPath)
        let allUpdatesObserved = configurationUpdatedObserved
            && sourceUpdatedObserved
            && resourceUpdatedObserved
        var payload = snapshot.tracePayload
        payload.merge([
            "complete": restored.complete
                && baselineObserved
                && allUpdatesObserved
                && buildConfigurationChanged
                && profileSourceChanged
                && profileResourceChanged
                && dependentModuleObserved
                && profileModuleObserved
                && processReplaced
                && context.recoveryGeneration >= 3
                && snapshot.error.isEmpty,
            "baselineObserved": baselineObserved,
            "updatedObserved": allUpdatesObserved,
            "configurationUpdatedObserved":
                configurationUpdatedObserved,
            "sourceUpdatedObserved": sourceUpdatedObserved,
            "resourceUpdatedObserved": resourceUpdatedObserved,
            "configurationRestored": restored.configuration,
            "sourceRestored": restored.source,
            "resourceRestored": restored.resource,
            "buildConfigurationObserved": buildConfigurationChanged,
            "dependentModuleObserved": dependentModuleObserved,
            "dependentResourceObserved":
                baselineObserved && configurationUpdatedObserved,
            "effectiveMavenModelObserved": true,
            "profileActivatedModuleObserved":
                profileModuleObserved && snapshot.profileModuleAvailable,
            "customSourceRootObserved":
                profileSourceChanged && sourceUpdatedObserved,
            "customResourceRootObserved":
                profileResourceChanged && resourceUpdatedObserved,
            "processReplaced": processReplaced,
            "previousProcessIdentifier": initialProcessIdentifier,
            "processIdentifier": processIdentifier,
            "windowPreserved":
                initialWindowIdentifier == ObjectIdentifier(context.window),
            "webViewPreserved":
                initialWebViewIdentifier == ObjectIdentifier(context.webView),
            "webViewReloaded": context.recoveryGeneration >= 3,
            "recoveryGeneration": context.recoveryGeneration,
            "replacementCount": context.recoveryGeneration,
            "flowClientOwned": snapshot.flowClientCount > 0,
            "requestBridgeRecovered":
                snapshot.requestCount >= 2
                    && snapshot.responseCount >= 2,
            "pushRecovered": snapshot.pushMessageCount >= 2,
            "errorFree": snapshot.error.isEmpty,
            "mode": "development-server"
        ]) { _, replacement in replacement }
        trace("recv", "reactorReload.frameworkAcceptance", payload)
        context.window.close()
    }

    private func fail(
        code: String,
        details: [String: Any] = [:]
    ) {
        let restored = restoreFixtures()
        var payload = details
        payload["code"] = code
        payload["configurationRestored"] = restored.configuration
        payload["sourceRestored"] = restored.source
        payload["resourceRestored"] = restored.resource
        trace("send", "error", payload)
        context.window.close()
    }

    private func restoreFixtures() -> RestoreResult {
        RestoreResult(
            configuration: restore(
                originalConfiguration, to: probeFile),
            source: restore(
                originalSource, to: profileSourceFile),
            resource: restore(
                originalResource, to: profileResourceFile))
    }

    private func restore(_ value: String?, to file: URL) -> Bool {
        guard let value else {
            return true
        }
        do {
            try value.write(to: file, atomically: true, encoding: .utf8)
            return true
        } catch {
            return false
        }
    }

    private struct RestoreResult {
        let configuration: Bool
        let source: Bool
        let resource: Bool

        var complete: Bool {
            configuration && source && resource
        }
    }

    private struct Snapshot: Decodable {
        let ready: Bool
        let revision: String
        let profileModuleAvailable: Bool
        let sourceRevision: String
        let resourceRevision: String
        let requestCount: Int
        let responseCount: Int
        let pushMessageCount: Int
        let flowClientCount: Int
        let error: String

        var tracePayload: [String: Any] {
            [
                "ready": ready,
                "revision": revision,
                "profileModuleAvailable": profileModuleAvailable,
                "sourceRevision": sourceRevision,
                "resourceRevision": resourceRevision,
                "requestCount": requestCount,
                "responseCount": responseCount,
                "pushMessageCount": pushMessageCount,
                "flowClientCount": flowClientCount,
                "error": error
            ]
        }
    }
}
