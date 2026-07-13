import WebKit

extension WKUserContentController {
    func addFlowIpcAutomationScripts(config: HostConfig) {
        let flags = config.flowIpcAutomationWindowFlags
        guard !flags.isEmpty else {
            return
        }
        let source = flags
            .map { "window.__\($0)__ = true;" }
            .joined(separator: " ")
        addUserScript(WKUserScript(source: source,
                                   injectionTime: .atDocumentStart,
                                   forMainFrameOnly: true))
    }
}
