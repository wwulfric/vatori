import Cocoa
import WebKit

struct FlowIpcWindowScope: Equatable {
    let windowId: String
    let sessionId: String
    let uiId: String

    static let main = FlowIpcWindowScope(
        windowId: "main",
        sessionId: "flow-ipc-session",
        uiId: "main")

    static func sidecarScope(
        _ envelope: [String: Any],
        fallbackWindowId: String
    ) -> FlowIpcWindowScope? {
        let windowId = envelope["windowId"] as? String ?? fallbackWindowId
        let sessionId = envelope["sessionId"] as? String
        let uiId = envelope["uiId"] as? String
        guard !windowId.isBlank,
              let sessionId, !sessionId.isBlank,
              let uiId, !uiId.isBlank else {
            return nil
        }
        return FlowIpcWindowScope(
            windowId: windowId,
            sessionId: sessionId,
            uiId: uiId)
    }

    static func window(_ windowId: String) -> FlowIpcWindowScope {
        precondition(isValidSidecarIdentifier(windowId),
                     "Flow IPC window id must be a valid sidecar identifier")
        if windowId == "main" {
            return .main
        }
        let sessionId = "flow-ipc-window:\(UUID().uuidString)"
        precondition(isValidSidecarIdentifier(sessionId),
                     "Flow IPC session id must be a valid sidecar identifier")
        return FlowIpcWindowScope(
            windowId: windowId,
            sessionId: sessionId,
            uiId: windowId)
    }

    func applying(to object: [String: Any]) -> [String: Any] {
        var scoped = object
        scoped["windowId"] = windowId
        scoped["sessionId"] = sessionId
        scoped["uiId"] = uiId
        return scoped
    }

    func matches(_ object: [String: Any]) -> Bool {
        (object["windowId"] as? String ?? object["uiId"] as? String) == windowId
            && object["sessionId"] as? String == sessionId
            && object["uiId"] as? String == uiId
    }

    var identity: FlowIpcScopeIdentity {
        FlowIpcScopeIdentity(
            windowId: windowId,
            sessionId: sessionId,
            uiId: uiId)
    }

    var tracePayload: [String: Any] {
        [
            "windowId": windowId,
            "sessionId": sessionId,
            "uiId": uiId
        ]
    }

    var documentStartScript: String {
        let object: [String: Any] = tracePayload
        guard let data = try? JSONSerialization.data(withJSONObject: object),
              let json = String(data: data, encoding: .utf8) else {
            preconditionFailure("Unable to serialize Flow IPC window scope")
        }
        return "window.VaadinDesktopHostContext = Object.freeze(\(json));"
    }
}

final class FlowIpcWindowContext {
    let lifetimeId = UUID()
    let scope: FlowIpcWindowScope
    let window: NSWindow
    let webView: WKWebView
    let commandExecutor: DesktopCommandExecutor
    let reloadURL: URL
    var navigationFinished = false
    var recoveryGeneration = 0

    init(scope: FlowIpcWindowScope,
         window: NSWindow,
         webView: WKWebView,
         commandExecutor: DesktopCommandExecutor,
         reloadURL: URL) {
        self.scope = scope
        self.window = window
        self.webView = webView
        self.commandExecutor = commandExecutor
        self.reloadURL = reloadURL
    }
}
