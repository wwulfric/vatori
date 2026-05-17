import Cocoa
import WebKit

func argumentValue(_ name: String, default fallback: String? = nil) -> String? {
    let arguments = CommandLine.arguments
    for index in arguments.indices {
        let argument = arguments[index]
        if argument == name && index + 1 < arguments.count {
            return arguments[index + 1]
        }
        if argument.hasPrefix(name + "=") {
            return String(argument.dropFirst(name.count + 1))
        }
    }
    return fallback
}

func fail(_ message: String, code: Int32 = 2) -> Never {
    FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
    exit(code)
}

guard let urlText = argumentValue("--url"),
      let url = URL(string: urlText) else {
    fail("Missing or invalid --url")
}

let title = argumentValue("--title", default: "Vaadin Desktop")!
let width = Double(argumentValue("--width", default: "1100")!) ?? 1100
let height = Double(argumentValue("--height", default: "760")!) ?? 760

final class WebViewHost: NSObject, NSWindowDelegate, WKNavigationDelegate {
    private let url: URL
    private let title: String
    private let width: Double
    private let height: Double
    private var window: NSWindow?
    private var webView: WKWebView?

    init(url: URL, title: String, width: Double, height: Double) {
        self.url = url
        self.title = title
        self.width = width
        self.height = height
    }

    func show() {
        let contentRect = NSRect(x: 0, y: 0, width: width, height: height)
        let window = NSWindow(
            contentRect: contentRect,
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false)

        let configuration = WKWebViewConfiguration()
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = true
        let webView = WKWebView(frame: contentRect, configuration: configuration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.width, .height]

        window.title = title
        window.contentView = webView
        window.delegate = self
        window.center()
        window.makeKeyAndOrderFront(nil)

        self.window = window
        self.webView = webView

        NSApp.activate(ignoringOtherApps: true)
        webView.load(URLRequest(url: url))
        print("opened WKWebView window title=\"\(title)\" url=\(url.absoluteString)")
    }

    func windowWillClose(_ notification: Notification) {
        NSApp.terminate(nil)
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        FileHandle.standardError.write(("navigation failed: \(error.localizedDescription)\n").data(using: .utf8)!)
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        FileHandle.standardError.write(("navigation failed: \(error.localizedDescription)\n").data(using: .utf8)!)
    }
}

let application = NSApplication.shared
application.setActivationPolicy(.regular)
let host = WebViewHost(url: url, title: title, width: width, height: height)
host.show()
application.run()
