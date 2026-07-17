import Cocoa
import WebKit

protocol HostRuntimeObserver: AnyObject {
    var hostNoActivate: Bool { get }
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
    var hostNoActivate: Bool { false }

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
