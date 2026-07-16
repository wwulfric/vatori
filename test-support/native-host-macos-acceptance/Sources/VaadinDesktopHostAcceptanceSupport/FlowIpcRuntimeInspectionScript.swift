let flowIpcRuntimeInspectionScript = """
(() => {
  const diagnostics = window.VaadinDesktopIpcDiagnostics || {};
  const clients = window.Vaadin?.Flow?.clients
    ? Object.keys(window.Vaadin.Flow.clients).filter((name) => name !== 'TypeScript')
    : [];
  return JSON.stringify({
    readyState: document.readyState,
    title: document.title,
    location: window.location.href,
    hasVaadinRoot: !!document.querySelector('#outlet, #framework-root, #ipc-root'),
    elementCount: document.body ? document.body.querySelectorAll('*').length : 0,
    standardBundleBridgeInstalled: diagnostics.standardBundleBridgeInstalled === true,
    standardBundleCookieBridgeInstalled:
      diagnostics.standardBundleCookieBridgeInstalled === true,
    standardBundlePushBridgeInstalled:
      diagnostics.standardBundlePushBridgeInstalled === true,
    standardBundlePushConnected: diagnostics.standardBundlePushConnected === true,
    standardBundlePushMessageCount:
      Number(diagnostics.standardBundlePushMessageCount || 0),
    standardBundlePushReconnectCount:
      Number(diagnostics.standardBundlePushReconnectCount || 0),
    standardBundlePushResyncCount:
      Number(diagnostics.standardBundlePushResyncCount || 0),
    standardBundleOpaqueRequestCount:
      Number(diagnostics.standardBundleOpaqueRequestCount || 0),
    standardBundleOpaqueResponseCount:
      Number(diagnostics.standardBundleOpaqueResponseCount || 0),
    standardBundleFirstMeaningfulPaintReported:
      diagnostics.standardBundleFirstMeaningfulPaintReported === true,
    standardBundleStreamingProbeRequired:
      diagnostics.standardBundleStreamingProbeRequired === true,
    standardBundleStreamingProbeComplete:
      diagnostics.standardBundleStreamingProbeComplete === true,
    standardBundleStreamingUploadBytes:
      Number(diagnostics.standardBundleStreamingUploadBytes || 0),
    standardBundleStreamingDownloadBytes:
      Number(diagnostics.standardBundleStreamingDownloadBytes || 0),
    standardBundleBrowserError: diagnostics.standardBundleBrowserError || '',
    standardBundleOpaqueRequestError:
      diagnostics.standardBundleOpaqueRequestError || '',
    standardBundleOpaqueResponseError:
      diagnostics.standardBundleOpaqueResponseError || '',
    standardBundlePushError: diagnostics.standardBundlePushError || '',
    standardBundleStreamingProbeError:
      diagnostics.standardBundleStreamingProbeError || '',
    standardBundleFlowClientCount: clients.length
  });
})()
"""
