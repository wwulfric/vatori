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
- `FlowBootstrap.js` / `FlowClient.js` are loaded over IPC, and the Flow client container becomes the visible WebView renderer for bootstrap, click-event patches, push and verification resync.
- Button events travel visible Flow client -> IPC-backed XHR shim -> Swift host -> Java stdin as `VFIPC1` envelopes carrying Flow UIDL RPC payloads.
- Java emits one server-initiated push UIDL update after bootstrap, and the visible Flow client engine consumes it without localhost HTTP.
- Java listener execution updates Flow component state and returns a full replace-root patch.
- With `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1`, the visible Flow client requests one resync after the event patch and consumes the returned `UidlRequestHandler` response.
- Desktop API calls travel Java -> Swift host as `VFIPC1 command`.
- The smoke view calls `window.setTitle`, `notification.show` and `clipboard.writeText`; Swift host returns `capabilityResult` with `permissionMode=allow-all-dev`, with clipboard backed by native `NSPasteboard`.
- This is not the full Vaadin Flow transport yet; the hidden local UIDL applier remains as a diagnostic fallback while real push queue/reconnect/resync semantics are still being built.

For automated real-window verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```
