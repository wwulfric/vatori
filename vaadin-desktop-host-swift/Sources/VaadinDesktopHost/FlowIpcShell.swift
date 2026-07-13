import Foundation

enum FlowIpcShell {
    private static let scriptPlaceholder = "__VATORI_FLOW_IPC_SHELL_SCRIPT__"

    static func load(repoRoot: String) -> String {
        for url in candidateHTMLURLs(repoRoot: repoRoot) {
            if let html = try? String(contentsOf: url, encoding: .utf8) {
                return inlineScript(html, repoRoot: repoRoot)
            }
        }
        trace("send", "error", [
            "code": "flowIpcShellMissing",
            "message": "FlowIpcShell.html could not be loaded from bundle or repository resources"
        ])
        return """
        <!doctype html>
        <html><body>Flow IPC shell resource missing.</body></html>
        """
    }

    private static func inlineScript(_ html: String, repoRoot: String) -> String {
        guard html.contains(scriptPlaceholder) else {
            return html
        }
        if let script = loadScript(repoRoot: repoRoot) {
            return html.replacingOccurrences(of: scriptPlaceholder, with: script)
        }
        trace("send", "error", [
            "code": "flowIpcShellScriptMissing",
            "message": "FlowIpcShellScript.js could not be loaded from bundle or repository resources"
        ])
        return html.replacingOccurrences(
            of: scriptPlaceholder,
            with: "document.body.textContent = 'Flow IPC shell script resource missing.';")
    }

    private static func loadScript(repoRoot: String) -> String? {
        for url in candidateScriptURLs(repoRoot: repoRoot) {
            if let script = try? String(contentsOf: url, encoding: .utf8) {
                return script
            }
        }
        return nil
    }

    private static func candidateHTMLURLs(repoRoot: String) -> [URL] {
        var urls: [URL] = []
        if let bundled = Bundle.main.url(forResource: "FlowIpcShell", withExtension: "html") {
            urls.append(bundled)
        }
        urls.append(URL(fileURLWithPath: repoRoot)
            .appendingPathComponent("vaadin-desktop-host-swift/Resources/FlowIpcShell.html"))
        return urls
    }

    private static func candidateScriptURLs(repoRoot: String) -> [URL] {
        var urls: [URL] = []
        if let bundled = Bundle.main.url(forResource: "FlowIpcShellScript", withExtension: "js") {
            urls.append(bundled)
        }
        urls.append(URL(fileURLWithPath: repoRoot)
            .appendingPathComponent("vaadin-desktop-host-swift/Resources/FlowIpcShellScript.js"))
        return urls
    }
}
