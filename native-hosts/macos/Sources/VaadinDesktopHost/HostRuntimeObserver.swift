import Cocoa
import WebKit

protocol HostRuntimeObserver: AnyObject {
    func configure(environment: [String: String])
    func configureUserContentController(
        _ userContentController: WKUserContentController,
        config: HostConfig
    )
    func didOpenInitialFlowIpcWindow(host: WebViewHost)
    func didFinishCompatibilityNavigation(host: WebViewHost, webView: WKWebView)
    func didFinishFlowIpcNavigation(host: WebViewHost, context: FlowIpcWindowContext)
}

extension HostRuntimeObserver {
    func configure(environment: [String: String]) {}

    func configureUserContentController(
        _ userContentController: WKUserContentController,
        config: HostConfig
    ) {}

    func didOpenInitialFlowIpcWindow(host: WebViewHost) {}

    func didFinishCompatibilityNavigation(host: WebViewHost, webView: WKWebView) {}

    func didFinishFlowIpcNavigation(host: WebViewHost, context: FlowIpcWindowContext) {}
}

final class ProductionHostRuntimeObserver: HostRuntimeObserver {}
