import WebKit

extension WKUserContentController {
    func addFlowIpcAutomationScripts(config: HostConfig) {
        guard FlowIpcStandardBundleStreamingProbe.isEnabled(config: config) else {
            return
        }
        addUserScript(WKUserScript(
            source: FlowIpcStandardBundleStreamingProbe.source,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true))
    }
}
