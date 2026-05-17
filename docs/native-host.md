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
- in `dev-ipc`, loads a local WebView shell, receives WebView messages through `WKScriptMessageHandler`, forwards them to Java stdin and delivers Java `VFIPC1` responses back to WebView JavaScript,
- in `dev-ipc`, has the WebView request `FlowBootstrap.js` and `FlowClient.js` through `VFIPC1 resourceRequest`, imports them as Blob-backed ES modules, calls `FlowBootstrap.init` with an IPC bootstrap config, then calls `FlowClient.init` and records DOM diagnostics,
- in `dev-ipc`, handles `VFIPC1 command` for `window.setTitle`, `clipboard.writeText` and trace-only `notification.show`, then emits `capabilityResult`.

Current compatibility boundary:

- In `dev` and `dev-swift`, Flow bootstrap, client events, UIDL/patch responses and push still run through the embedded Spring Boot localhost server.
- Desktop runtime commands can cross Java -> native host as Host Protocol v0 trace envelopes, but synchronous native return values are still scaffolded.
- The WebView loads a local HTTP URL today. This is explicitly not the final Flow over IPC transport.
- `dev-ipc` does not load a local HTTP URL for the smoke app and uses `VFIPC1` for desktop commands. The WebView bridge sends click events and one verification resync, Java includes real `UidlWriter`/`UidlRequestHandler` output in IPC payloads, one server-initiated push UIDL update is applied by a hidden fallback, and `FlowBootstrap.js` / `FlowClient.js` are loaded from the Java sidecar classpath through IPC, imported as modules and initialized with an IPC app config. The registered Flow client now owns the visible bootstrap and click-event patch path through an IPC-backed XHR bridge; full Flow push/resync ownership is still pending.

Verification note: on some managed macOS desktops, global `screencapture` or coordinate clicking can hit a `loginwindow`/MDM overlay instead of normal app windows. In that case, use the WindowServer entry for `Vaadin Desktop Host`, `HOST_PROTOCOL webview.dom`, the `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1` WebView smoke hooks and `window-snapshot.png` as the reliable verification artifacts.

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

- feed push and resync UIDL patches into the initialized Flow client engine,
- enforce capability permissions.

## Why Not Call This Final

Embedding a localhost URL in WKWebView is compatibility mode. It is useful for proving framework ergonomics, real Flow app structure and desktop host lifecycle, but it is not Flow over IPC.

The `dev-ipc` path is the first real no-HTTP IPC slice, but it is not final either: it proves host/WebView/Java message flow, visible Flow client ownership for bootstrap and click-event patches, IPC-backed Flow client XHR event dispatch, one verification resync, one server-initiated push, listener-driven patches, server-side UIDL generation, Flow client module initialization and bootstrap UIDL binding. Push/resync still use a repository-local fallback boundary.
