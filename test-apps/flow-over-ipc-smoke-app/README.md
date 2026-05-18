# Flow over IPC Smoke App

This is the first independent developer-shaped app for the `dev-ipc` runtime path.

Run:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

Expected transport:

- `transport=flow-ipc`
- `compatibilityMode=false`
- no `Application URL: http://127.0.0.1:...` is required for the core UI path

Current scope:

- Real Vaadin Flow Java components are instantiated in the Java sidecar.
- The Swift/AppKit host loads a local WebView shell instead of a localhost URL.
- The full advertised Flow client classpath resource graph is requested over IPC. `FlowBootstrap.js` / `FlowClient.js` are imported, and the Flow client container becomes the visible WebView renderer for bootstrap, click-event patches, push and verification resync.
- The smoke view's `ipc-list` contains four Flow `Span` rows; `webview.dom` diagnostics verify that the visible Flow client bound and rendered all four list items.
- Button events travel visible Flow client -> IPC-backed XHR shim -> Swift host -> Java stdin as `VFIPC1` envelopes carrying Flow UIDL RPC payloads.
- The v1.1 component matrix adds a native Flow `Input` text field and checkbox. Their `change` events travel through the same visible Flow client -> IPC-backed XHR -> Java listener -> UIDL patch path, carrying `element.value` and `element.checked` event data.
- Java emits one server-initiated push UIDL update after bootstrap; the WebView bridge queues it until the visible Flow client engine is ready, then flushes and consumes it without localhost HTTP.
- Java-to-WebView payloads include monotonic `serverSequence` diagnostics for the smoke path.
- Java listener execution updates Flow component state and returns a full replace-root patch.
- With `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1`, the verification hook requests one resync after the event patch through the Flow client path, falling back to an IPC-backed Flow XHR body if the current engine method does not emit XHR, and consumes the returned `UidlRequestHandler` response.
- Desktop API calls travel Java -> Swift host as `VFIPC1 command`.
- `desktop-capabilities.json` declares `permissionMode=manifest-enforced` and allows `window.setTitle`, `notification.show`, `clipboard.writeText`, `fileDialog.openFile`, `fileTransfer.download` and `fileTransfer.selectUploadFile` for the `main` window. The smoke view calls those APIs; Swift host returns `capabilityResult` with `permissionReason=manifest-allow` and manifest metadata, with clipboard backed by native `NSPasteboard`, file dialog backed by AppKit `NSOpenPanel`, bounded download writing to a controlled test path and upload/select-file reading metadata plus sampled bytes from a controlled test file. The same click also sends `diagnostics.deniedCapability`, which is intentionally absent from the manifest and must return `permissionDecision=denied` with `permissionReason=manifest-denied`.
- The file-transfer smoke is deliberately narrow: traces redact the body and record byte count, filename, path, operation and permission metadata. It does not claim full Vaadin Upload/Download parity.
- This is not the full Vaadin Flow transport yet; the hidden local UIDL applier remains as a diagnostic fallback while real push queue/reconnect/resync semantics are still being built.

For automated real-window verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```
