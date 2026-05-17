# Native Host

## Mainline Host Spike

`vaadin-desktop-host-swift` is the next-stage macOS mainline host prototype. It uses Swift, AppKit and WKWebView so the native runtime can directly own macOS window lifecycle and platform behavior.

Run:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

Responsibilities implemented now:

- starts the Java Vaadin app as a sidecar with `VAADIN_DESKTOP_HOST=headless`,
- opens a native AppKit `NSWindow` with `WKWebView` after the sidecar prints `Application URL: ...`,
- traces host lifecycle messages as `HOST_PROTOCOL ...`,
- emits `webview.dom` inspection and writes `.build/vaadin-desktop-host-swift/window-snapshot.png` after WebView render,
- reads Java desktop command envelopes prefixed with `VDP1 `,
- closes the Java process when the desktop window closes,
- in `dev-ipc`, loads a local WebView shell, receives WebView messages through `WKScriptMessageHandler`, forwards them to Java stdin and delivers Java `VFIPC1` responses back to WebView JavaScript.

Current compatibility boundary:

- Flow bootstrap, client events, UIDL/patch responses and push still run through the embedded Spring Boot localhost server.
- Desktop runtime commands can cross Java -> native host as Host Protocol v0 trace envelopes, but synchronous native return values are still scaffolded.
- The WebView loads a local HTTP URL today. This is explicitly not the final Flow over IPC transport.
- `dev-ipc` does not load a local HTTP URL for the smoke app, but it is still a minimum renderer rather than the complete Vaadin Flow client engine.

Verification note: on some managed macOS desktops, global `screencapture` can show a `loginwindow`/MDM overlay instead of normal app windows. In that case, use the WindowServer entry for `Vaadin Desktop Host`, `HOST_PROTOCOL webview.dom` and `window-snapshot.png` as the reliable verification artifacts.

## macOS Fallback Host

`SystemWebViewDesktopHost` is the default host on macOS. It:

- receives the local Vaadin URL,
- creates a temporary `.app` bundle,
- compiles the bundled Swift host with `/usr/bin/swiftc`,
- opens a native WKWebView window,
- logs bootstrap and host-exit trace events,
- terminates the Java app when the window process exits by default.

This is a real desktop window, but it still uses **compatibility HTTP transport**. It is not final Flow over IPC.

`CompatibilityDesktopHost` is still available as a fallback. It:

- receives the local Vaadin URL,
- logs bootstrap trace,
- optionally opens the default browser,
- marks the app as compatibility mode.

Select a host with:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

`CompatibilityNativeBridge` handles desktop API calls by logging trace and returning deterministic results. Clipboard is backed by in-memory state.

`SystemWebViewDesktopHost` remains useful as a small macOS fallback, but it is no longer the long-term mainline.

## Next Native Host Work

The real host still needs to:

- load local Vaadin frontend resources,
- replace the current local HTML shell with the real Flow client engine bridge,
- apply Vaadin UIDL patches,
- enforce capability permissions.

## Why Not Call This Final

Embedding a localhost URL in WKWebView is compatibility mode. It is useful for proving framework ergonomics, real Flow app structure and desktop host lifecycle, but it is not Flow over IPC.

The `dev-ipc` path is the first real no-HTTP IPC slice, but it is not final either: it proves host/WebView/Java message flow and listener-driven patches before the project swaps in Vaadin UIDL and the real Flow client engine.
