import WebKit

extension WKUserContentController {
    func addFlowIpcAutomationScripts(config: HostConfig) {
        if config.flowIpcAutoFrameworkUpdate {
            addUserScript(WKUserScript(
                source: FlowIpcRpcRequestObservation.source,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: true))
        }
        if config.flowIpcAutoRecoveryTermination {
            addUserScript(WKUserScript(
                source: FlowIpcRecoveryDocumentIdentity.source,
                injectionTime: .atDocumentStart,
                forMainFrameOnly: true))
        }
        guard FlowIpcStandardBundleStreamingProbe.isEnabled(config: config) else {
            return
        }
        addUserScript(WKUserScript(
            source: FlowIpcStandardBundleStreamingProbe.source,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true))
    }
}

// Test-only wire observation. Production transport remains opaque.
enum FlowIpcRpcRequestObservation {
    static let source = #"""
    (() => {
      const diagnostics = window.VaadinDesktopIpcDiagnostics ||= {};
      const records = diagnostics.frameworkRpcRequests ||= [];
      const descriptor = Object.getOwnPropertyDescriptor(window, 'XMLHttpRequest');
      if (!descriptor?.configurable || !('value' in descriptor)) return;
      let current = descriptor.value;
      const install = (XHR) => {
        const original = XHR.prototype.send;
        XHR.prototype.send = function(bodyText) {
        diagnostics.frameworkRpcObservedSends = (diagnostics.frameworkRpcObservedSends || 0) + 1;
        try {
          // No body retention; only small string JSON requests are inspected.
          if (typeof bodyText === 'string' && bodyText.length <= 65536) {
            const body = JSON.parse(bodyText);
            for (const rpc of Array.isArray(body.rpc) ? body.rpc : []) {
              if (typeof rpc.templateEventMethodName !== 'string'
                  || !Number.isSafeInteger(rpc.node) || !Number.isSafeInteger(rpc.promise)) continue;
              records.push({send: diagnostics.frameworkRpcObservedSends, node: rpc.node,
                method: rpc.templateEventMethodName, promise: rpc.promise});
              if (records.length > 64) records.shift();
            }
          }
        } catch (_) { /* Diagnostics must not change request delivery. */ }
        return Reflect.apply(original, this, arguments);
        };
        diagnostics.frameworkRpcRequestObserverInstalled = XHR.prototype.send !== original;
      };
      // The production bridge replaces XHR in a later document-start script.
      // Restore the ordinary global descriptor immediately after observing it.
      Object.defineProperty(window, 'XMLHttpRequest', {
        configurable: true, enumerable: descriptor.enumerable,
        get: () => current,
        set: value => {
          current = value;
          install(value);
          Object.defineProperty(window, 'XMLHttpRequest', {...descriptor, value});
        }
      });
    })();
    """#
}
