# Vaadin Desktop Swift Host

This is the macOS native host mainline for Vaadin Desktop Framework.

The host is written in Swift with AppKit and WKWebView. It owns the desktop process boundary:

- starts the Java Vaadin Flow app as a sidecar,
- waits for the Java sidecar to print `Application URL: ...`,
- opens a native `NSWindow` with `WKWebView`,
- reads Java desktop command envelopes prefixed with `VDP1 `,
- traces Host Protocol v0 lifecycle events,
- terminates the Java sidecar when the window closes.

Run from the repository root:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

The Flow UI transport is still localhost HTTP compatibility mode. The long-term direction remains Flow over IPC, and native runtime work now uses Swift on macOS.
