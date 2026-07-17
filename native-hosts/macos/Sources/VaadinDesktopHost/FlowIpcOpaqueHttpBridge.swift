import Foundation

enum FlowIpcOpaqueHttpBridge {
    static func load(from url: URL) -> String? {
        try? String(contentsOf: url, encoding: .utf8)
    }
}
