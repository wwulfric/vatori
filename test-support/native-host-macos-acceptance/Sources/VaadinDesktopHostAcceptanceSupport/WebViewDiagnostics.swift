import Cocoa
import CoreFoundation
import WebKit

private func acceptanceTraceBoolean(_ value: Any) -> Bool? {
    guard let number = value as? NSNumber,
          CFGetTypeID(number) == CFBooleanGetTypeID() else {
        return nil
    }
    return number.boolValue
}

private func compactAcceptanceWebViewDomTracePayload(
    _ payload: [String: Any]
) -> [String: Any] {
    let alwaysKeep: Set<String> = [
        "readyState",
        "title",
        "location",
        "bodyText",
        "hasVaadinRoot",
        "elementCount"
    ]
    let diagnosticPrefixes = [
        "flowClientEngine",
        "standardBundle",
        "currentAppGeneratedImportsOwnerRuntimeContract",
        "currentAppGeneratedImportsOwnerSideEffect",
        "frontendResourcePipelineBasicClosure"
    ]
    var compact: [String: Any] = [:]
    for (key, value) in payload {
        let keepDiagnostic = diagnosticPrefixes.contains { key.hasPrefix($0) }
        if let text = value as? String {
            if alwaysKeep.contains(key) || keepDiagnostic || !text.isEmpty {
                compact[key] = compactTraceString(text, maxLength: 1024)
            }
        } else if let bool = acceptanceTraceBoolean(value) {
            if bool || alwaysKeep.contains(key) || keepDiagnostic {
                compact[key] = bool
            }
        } else if let number = value as? NSNumber {
            let scalar = number.doubleValue
            if alwaysKeep.contains(key) || keepDiagnostic || (scalar != 0 && scalar != -1) {
                compact[key] = number
            }
        } else if let int = value as? Int {
            if alwaysKeep.contains(key) || keepDiagnostic || (int != 0 && int != -1) {
                compact[key] = int
            }
        } else if let int64 = value as? Int64 {
            if alwaysKeep.contains(key) || keepDiagnostic || (int64 != 0 && int64 != -1) {
                compact[key] = int64
            }
        } else if let double = value as? Double {
            if alwaysKeep.contains(key) || keepDiagnostic || (double != 0 && double != -1) {
                compact[key] = double
            }
        } else if let strings = value as? [String], !strings.isEmpty {
            compact[key] = compactTraceString(
                strings.joined(separator: ","),
                maxLength: 1024)
        } else if let numbers = value as? [NSNumber], !numbers.isEmpty {
            compact[key] = numbers.map { "\($0)" }.joined(separator: ",")
        }
    }
    return compact
}

final class WebViewDiagnostics {
    private let repoRoot: String

    init(repoRoot: String) {
        self.repoRoot = repoRoot
    }

    func inspect(_ webView: WKWebView, completion: (() -> Void)? = nil) {
        let script = flowIpcRuntimeInspectionScript
        webView.evaluateJavaScript(script) { result, error in
            if let error {
                trace("send", "error", [
                    "code": "webviewInspectFailed",
                    "message": error.localizedDescription
                ])
                completion?()
                return
            }
            var payload: [String: Any] = ["label": "main"]
            if let text = result as? String {
                payload["resultBytes"] = text.utf8.count
                payload["resultRedacted"] = true
                payload["resultPreview"] = compactTraceString(text, maxLength: 768)
                if let data = text.data(using: .utf8),
                   let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    payload.merge(compactAcceptanceWebViewDomTracePayload(object)) { _, new in new }
                }
            } else {
                payload["result"] = compactTraceString(String(describing: result), maxLength: 768)
            }
            trace("recv", "webview.dom", payload)
            completion?()
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

            let snapshotPath = "\(self.repoRoot)/.build/native-hosts/macos/window-snapshot.png"
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
