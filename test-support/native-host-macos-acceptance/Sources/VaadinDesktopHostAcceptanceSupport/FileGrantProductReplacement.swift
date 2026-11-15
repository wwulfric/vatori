import Foundation
import Security
import Darwin

/// Fixture-only browser action, never a host-side replacement bypass.
enum FileGrantProductReplacement {
    static func readSandboxProbe(stage: String, selected: Bool) -> Bool {
        let env = ProcessInfo.processInfo.environment
        guard let target = env[selected ? "VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SELECTED_PATH"
                                        : "VAADIN_DESKTOP_FLOW_IPC_SANDBOX_SIBLING_PATH"] else { return false }
        var code: SecCode?
        var staticCode: SecStaticCode?
        var information: CFDictionary?
        let names: Set<String> = ["com.apple.security.app-sandbox",
            "com.apple.security.files.user-selected.read-only", "com.apple.security.network.client"]
        guard SecCodeCopySelf([], &code) == errSecSuccess, let code,
              SecCodeCopyStaticCode(code, [], &staticCode) == errSecSuccess, let staticCode,
              SecCodeCopySigningInformation(staticCode, SecCSFlags(rawValue: kSecCSSigningInformation), &information) == errSecSuccess,
              let info = information as? [String: Any],
              let entitlements = info[kSecCodeInfoEntitlementsDict as String] as? [String: Any],
              Set(entitlements.keys) == names,
              entitlements.values.allSatisfy({ CFGetTypeID($0 as CFTypeRef) == CFBooleanGetTypeID() && ($0 as? Bool) == true }) else { return false }
        errno = 0
        let descriptor = Darwin.open(target, O_RDONLY | O_NOFOLLOW | O_CLOEXEC)
        let codeNumber = descriptor < 0 ? errno : 0
        if descriptor >= 0 { Darwin.close(descriptor) }
        let denied = descriptor < 0 && codeNumber == EPERM
        trace("recv", "fileGrant.readSandboxProbe", ["stage": stage, "denied": denied,
            "errno": codeNumber, "exactEntitlements": true, "selected": selected])
        return denied
    }

    static var enabled: Bool {
        ProcessInfo.processInfo.environment["VATORI_TEST_FILE_GRANT_PRODUCT_REPLACEMENT"] == "1"
    }

    static func request(_ host: WebViewHost, completion: @escaping (Bool) -> Void) {
        guard let main = host.flowIpcWindowContext("main") else { completion(false); return }
        main.webView.evaluateJavaScript("""
          (() => {
            const button = document.getElementById('file-grant-product-replacement');
            if (!button || button.disabled) return false;
            button.click(); return true;
          })()
          """) { value, error in
            let dispatched = error == nil && value as? Bool == true
            trace("send", "fileGrant.productReplacementAction", [
                "dispatched": dispatched, "inputMode": "browser-programmatic",
                "physicalInput": false, "windowId": "main"
            ])
            completion(dispatched)
        }
    }
}
