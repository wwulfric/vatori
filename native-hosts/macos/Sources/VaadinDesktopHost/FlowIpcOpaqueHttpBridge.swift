import Foundation

enum FlowIpcOpaqueHttpBridge {
    static func load(repoRoot: String) -> String? {
        let candidates = [
            Bundle.main.resourceURL?.appendingPathComponent("FlowIpcOpaqueHttpBridge.js"),
            URL(fileURLWithPath: repoRoot)
                .appendingPathComponent("native-hosts/macos")
                .appendingPathComponent("Resources")
                .appendingPathComponent("FlowIpcOpaqueHttpBridge.js")
        ].compactMap { $0 }

        for candidate in candidates {
            if let source = try? String(contentsOf: candidate, encoding: .utf8) {
                return source
            }
        }
        return nil
    }
}
