# Vaadin Desktop Swift Host

This is the macOS native host mainline for Vaadin Desktop Framework.

The host is written in Swift with AppKit and WKWebView. It owns the desktop process boundary:

- starts the Java Vaadin Flow app as a sidecar,
- in compatibility mode, waits for the Java sidecar to print `Application URL: ...`,
- in `flow-ipc` mode, waits for a `VFIPC1 ready` envelope,
- opens a native `NSWindow` with `WKWebView`,
- loads either the compatibility URL or a local Flow IPC WebView shell,
- reads Java desktop command envelopes prefixed with `VDP1 `,
- reads and forwards Flow IPC envelopes prefixed with `VFIPC1 `,
- handles `VFIPC1 command` for AppKit window title and NSPasteboard clipboard writes,
- traces Host Protocol v0 lifecycle events,
- terminates the Java sidecar when the window closes.

Run from the repository root:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

Run the first no-HTTP IPC slice:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`dev-swift` still uses localhost HTTP compatibility mode. `dev-ipc` does not load a localhost URL for the smoke app, but it currently uses a minimum local renderer instead of Vaadin UIDL and the full Flow client engine.

For repeatable real-window verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```
