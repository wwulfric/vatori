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
- in `dev-ipc`, has the WebView request all advertised Flow client classpath resources through `VFIPC1 resourceRequest`, imports `FlowBootstrap.js` and `FlowClient.js` as Blob-backed ES modules, calls `FlowBootstrap.init` with an IPC bootstrap config, then calls `FlowClient.init` and records DOM diagnostics,
- in `dev-ipc`, classifies Flow client resources as executed, verified-only, intentionally-not-executed or execution-failed, and performs a dynamic no-HTTP `flow-client-lazy-lookup` probe for `Flow.js`,
- in `dev-ipc`, requests and executes the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` Vaadin component frontend module through IPC, records `blockedReason` / `failureClassification` for the missing npm transitive imports, and uses v1.2-only shims so the first Web Component smoke can run without localhost HTTP,
- in `dev-ipc`, records Flow IPC push ordering diagnostics, including delivered push order, duplicate/late ignored order and ordering-recovery resync reason,
- in `dev-ipc`, handles `VFIPC1 command` for `window.setTitle`, `clipboard.writeText`, trace-only `notification.show`, `fileDialog.openFile` through AppKit `NSOpenPanel` and bounded `fileTransfer.download` / `fileTransfer.selectUploadFile` smoke paths, then emits `capabilityResult`.

Current compatibility boundary:

- In `dev` and `dev-swift`, Flow bootstrap, client events, UIDL/patch responses and push still run through the embedded Spring Boot localhost server.
- Desktop runtime commands can cross Java -> native host as Host Protocol v0 trace envelopes, but synchronous native return values are still scaffolded.
- The WebView loads a local HTTP URL today. This is explicitly not the final Flow over IPC transport.
- `dev-ipc` does not load a local HTTP URL for the smoke app and uses `VFIPC1` for desktop commands. The WebView bridge sends click events, native input/checkbox changes, one `BigDecimalField` Web Component change and ordering-recovery resync from the visible Flow client, Java includes real `UidlWriter`/`UidlRequestHandler` output in IPC payloads, two server-initiated push UIDL updates are queued until the Flow client engine is ready and then delivered into it, one controlled duplicate/late probe is ignored with diagnostics, and the advertised Flow client resources are loaded from the Java sidecar classpath through IPC. `FlowBootstrap.js` / `FlowClient.js` are imported as modules and initialized with an IPC app config; `Flow.js`, `index.js`, `compile.properties` and the GWT client bundle are currently verified-only / intentionally-not-executed. The real `vaadin-big-decimal-field.js` classpath frontend module is imported with explicit v1.2 shim classification for missing npm imports. The registered Flow client now owns the visible bootstrap, click/change-event patch, push and recovery resync path through IPC-backed bridge methods.

Verification note: on some managed macOS desktops, global `screencapture` or coordinate clicking can hit a `loginwindow`/MDM overlay instead of normal app windows. In that case, use the WindowServer entry for `Vaadin Desktop Host`, `HOST_PROTOCOL webview.dom`, the `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` WebView smoke hooks, `VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1` for unattended native file dialog cancellation, `VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1` for unattended shutdown evidence and `window-snapshot.png` as the reliable verification artifacts.

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

## Capability Permission Model

`dev-ipc` now reads the permission mode from `desktop-capabilities.json` when a manifest
is present and records a `capabilityManifest` trace event. Supported modes are:

- `allow-all-dev`: development default; commands are allowed when no manifest is present,
  or when a loaded manifest lists the command.
- `manifest-enforced`: a loaded manifest is required, and commands must be listed for
  the window.
- `trace-only-dry-run`: commands execute, but permission metadata records whether a
  loaded manifest would have allowed or denied the command.

The current host allows the known v1 commands for the single main window:

- `window.setTitle`
- `clipboard.writeText`
- `fileDialog.openFile`
- `fileTransfer.download`
- `fileTransfer.selectUploadFile`
- `notification.show`

Every `capabilityResult` includes `permissionMode`, `permissionDecision`, `permissionReason` and `permissionScope`. The Flow IPC smoke app uses `permissionMode=manifest-enforced`; known commands must be listed for the `main` window and successful calls are marked `permissionDecision=allowed` with `permissionReason=manifest-allow`. The smoke view also sends an intentionally undeclared `diagnostics.deniedCapability` command, which is rejected with `permissionDecision=denied` and `permissionReason=manifest-denied`. Results include `permissionManifestLoaded`, `permissionManifestPath` and `permissionManifestCommands`. `fileDialog.openFile` now returns AppKit panel result metadata (`canceled`, `paths`, `allowsMultipleSelection`, `securityScoped=false`, `autoCanceled`) but does not yet create security-scoped bookmarks. `fileTransfer.download` and `fileTransfer.selectUploadFile` are v1.1 smoke boundaries: they support controlled test paths, byte-count metadata and redacted trace bodies, but they are not production file streaming or Vaadin Upload/Download parity. This is intentionally still a development permission system. The production direction is a stricter manifest-gated model with per-window, per-origin, user-gesture and per-capability grants.

## Next Native Host Work

The real host still needs to:

- replace the controlled push ordering/resync recovery smoke with a durable queue, ack and reconnect model,
- replace resource classification/lazy-lookup/v1.2 component-shim diagnostics with full production bundle/dev-mode frontend execution,
- replace the bounded controlled-path file-transfer smoke with streaming upload/download, native progress/cancel semantics and sandbox-safe file grants,
- replace the development manifest modes with production capability grants, user-gesture checks and security-scoped file permissions.

## Why Not Call This Final

Embedding a localhost URL in WKWebView is compatibility mode. It is useful for proving framework ergonomics, real Flow app structure and desktop host lifecycle, but it is not Flow over IPC.

The `dev-ipc` path is the first real no-HTTP IPC slice, but it is not final either: it proves host/WebView/Java message flow, visible Flow client ownership for bootstrap, click-event patches, native input/checkbox change-event patches, one real `BigDecimalField` Web Component frontend module and event patch, two ordered server pushes, duplicate/late ignored diagnostics, ordering-recovery resync, IPC-backed Flow client XHR/event/resync dispatch, listener-driven patches, server-side UIDL generation, Flow client module initialization, bootstrap UIDL binding, resource execution classification/lazy lookup diagnostics and a bounded controlled-path file-transfer capability boundary. The repository-local UIDL applier remains as a diagnostic fallback, while broader component coverage, full resource loading, production bundles, full upload/download streaming and durable push/reconnect semantics remain future work.
