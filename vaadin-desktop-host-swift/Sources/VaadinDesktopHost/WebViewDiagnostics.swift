import Cocoa
import WebKit

final class WebViewDiagnostics {
    private let repoRoot: String

    init(repoRoot: String) {
        self.repoRoot = repoRoot
    }

    func inspect(_ webView: WKWebView) {
        let script = flowIpcInspectionScript
        webView.evaluateJavaScript(script) { result, error in
            if let error {
                trace("send", "error", [
                    "code": "webviewInspectFailed",
                    "message": error.localizedDescription
                ])
                return
            }
            var payload: [String: Any] = ["label": "main"]
            if let text = result as? String {
                payload["resultBytes"] = text.utf8.count
                payload["resultRedacted"] = true
                payload["resultPreview"] = compactTraceString(text, maxLength: 768)
                if let data = text.data(using: .utf8),
                   let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    payload.merge(compactWebViewDomTracePayload(object)) { _, new in new }
                }
            } else {
                payload["result"] = compactTraceString(String(describing: result), maxLength: 768)
            }
            trace("recv", "webview.dom", payload)
        }
    }

    func writeSnapshot(_ webView: WKWebView, completion: (() -> Void)? = nil) {
        let snapshotConfig = WKSnapshotConfiguration()
        snapshotConfig.rect = webView.bounds
        webView.takeSnapshot(with: snapshotConfig) { image, error in
            if let error {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": error.localizedDescription
                ])
                completion?()
                return
            }

            let snapshotPath = "\(self.repoRoot)/.build/vaadin-desktop-host-swift/window-snapshot.png"
            guard let image,
                  let tiffData = image.tiffRepresentation,
                  let bitmap = NSBitmapImageRep(data: tiffData),
                  let pngData = bitmap.representation(using: .png, properties: [:]) else {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": "WKWebView snapshot produced no PNG data"
                ])
                completion?()
                return
            }

            do {
                try FileManager.default.createDirectory(
                    at: URL(fileURLWithPath: snapshotPath).deletingLastPathComponent(),
                    withIntermediateDirectories: true)
                try pngData.write(to: URL(fileURLWithPath: snapshotPath))
                trace("recv", "webview.snapshot", [
                    "label": "main",
                    "path": snapshotPath,
                    "width": Int(image.size.width),
                    "height": Int(image.size.height)
                ])
                completion?()
            } catch {
                trace("send", "error", [
                    "code": "snapshotFailed",
                    "message": error.localizedDescription
                ])
                completion?()
            }
        }
    }
}
