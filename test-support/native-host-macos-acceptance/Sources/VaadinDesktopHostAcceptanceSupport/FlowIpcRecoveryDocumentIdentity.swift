import Foundation

// Test-only document identity. WebKit timeOrigin can drift within the same
// Document (WebKit bug 258572), so live timestamp equality is not identity.
enum FlowIpcRecoveryDocumentIdentity {
    static let source = """
    (() => {
      const owner = document;
      const marker = crypto.randomUUID();
      const timeOriginMillis = performance.timeOrigin;
      Object.defineProperty(window, 'VatoriRecoveryDocumentIdentity', {
        value: () => ({marker, timeOriginMillis, retained: document === owner,
          observedTimeOriginMillis: performance.timeOrigin}),
        writable: false, configurable: false
      });
    })();
    """
}
