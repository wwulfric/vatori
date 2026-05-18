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
- handles `VFIPC1 command` for AppKit window title, NSPasteboard clipboard writes, AppKit file dialogs and bounded file-transfer smoke paths,
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

`dev-swift` still uses localhost HTTP compatibility mode. `dev-ipc` does not load a localhost URL for the smoke app. It imports FlowBootstrap/FlowClient over `VFIPC1`, promotes the registered Flow client container to the visible WebView surface, bridges the Flow client engine's XHR transport over `VFIPC1`, sends real Flow client click/input/checkbox/resync RPCs through Java's `UidlRequestHandler`, delivers the one-shot server push into the visible Flow client and records bounded file-transfer capability results. The repository-local UIDL applier remains as a hidden diagnostic fallback while full Flow push/reconnect/resync semantics and production file streaming are still being built.

For repeatable real-window verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```
