import Cocoa
import WebKit

protocol HostRuntimeHooks: AnyObject {
    func configure(environment: [String: String])
    func configureUserContentController(
        _ userContentController: WKUserContentController,
        config: HostConfig
    )
    func didOpenInitialFlowIpcWindow(host: WebViewHost)
    func didFinishCompatibilityNavigation(host: WebViewHost, webView: WKWebView)
    func didFinishFlowIpcNavigation(host: WebViewHost, context: FlowIpcWindowContext)
}

extension HostRuntimeHooks {
    func configure(environment: [String: String]) {}

    func configureUserContentController(
        _ userContentController: WKUserContentController,
        config: HostConfig
    ) {}

    func didOpenInitialFlowIpcWindow(host: WebViewHost) {}

    func didFinishCompatibilityNavigation(host: WebViewHost, webView: WKWebView) {}

    func didFinishFlowIpcNavigation(host: WebViewHost, context: FlowIpcWindowContext) {}
}

final class ProductionHostRuntimeHooks: HostRuntimeHooks {}
