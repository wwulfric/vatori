import Cocoa
import Foundation

enum FlowIpcNativeInputActivation {
    static func prepare(
        context: FlowIpcWindowContext,
        deadline: Date,
        onFailure: @escaping ([String: Any]) -> Void,
        completion: @escaping () -> Void
    ) {
        request(context: context)
        wait(
            context: context,
            deadline: deadline,
            onFailure: onFailure,
            completion: completion)
    }

    private static func request(context: FlowIpcWindowContext) {
        let application = NSApplication.shared
        application.unhide(nil)
        context.window.makeKeyAndOrderFront(nil)
        NSRunningApplication.current.activate(options: [.activateAllWindows])
        if #available(macOS 14.0, *) {
            application.activate()
        } else {
            application.activate(ignoringOtherApps: true)
        }
        context.window.makeKeyAndOrderFront(nil)
    }

    private static func wait(
        context: FlowIpcWindowContext,
        deadline: Date,
        onFailure: @escaping ([String: Any]) -> Void,
        completion: @escaping () -> Void
    ) {
        guard Date() < deadline else {
            onFailure([
                "message": "Timed out waiting for native input activation",
                "activeApplication": NSApplication.shared.isActive,
                "keyWindow": context.window.isKeyWindow
            ])
            return
        }
        guard NSApplication.shared.isActive,
              context.window.isKeyWindow else {
            request(context: context)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                wait(
                    context: context,
                    deadline: deadline,
                    onFailure: onFailure,
                    completion: completion)
            }
            return
        }
        let responder = context.window.firstResponder
        let responderView = responder as? NSView
        let webViewOwnsResponder = responderView === context.webView
            || responderView?.isDescendant(of: context.webView) == true
        if !webViewOwnsResponder,
           !context.window.makeFirstResponder(context.webView) {
            onFailure([
                "message": "Unable to route native input to WKWebView",
                "previousResponder": String(describing: responder)
            ])
            return
        }
        completion()
    }
}
