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
- Button events travel WebView -> Swift host -> Java stdin as `VFIPC1` envelopes carrying Flow UIDL RPC payloads.
- Java emits one server-initiated push UIDL update after bootstrap, and the WebView applies it without localhost HTTP.
- Java listener execution updates Flow component state and returns a full replace-root patch.
- Desktop API calls travel Java -> Swift host as `VFIPC1 command`.
- The smoke view calls `window.setTitle`, `notification.show` and `clipboard.writeText`; Swift host returns `capabilityResult`, with clipboard backed by native `NSPasteboard`.
- This is not the full Vaadin Flow client engine or UIDL adapter yet.

For automated real-window verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```
