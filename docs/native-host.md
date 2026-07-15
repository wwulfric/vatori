# Native Host

`vaadin-desktop-host-swift` is the current macOS native host prototype. It uses
Swift, AppKit and WKWebView so the host can own window lifecycle, WebView setup,
resource loading and desktop capability execution.

## Compatibility Host Path

`dev-swift` starts the Java app as a sidecar, waits for `Application URL: ...` and
opens that localhost URL in WKWebView. This validates native host behavior while
keeping Flow UI transport in compatibility mode.

The older Java `system-webview` launcher in `vaadin-desktop-host` is fail-fast by
default. If it cannot start the macOS WebView host, it throws instead of silently
falling back to compatibility mode. Use `VAADIN_DESKTOP_HOST=compatibility`,
`headless` or `browser` for an explicit compatibility host. For local development
only, `VAADIN_DESKTOP_HOST_FALLBACK=compatibility` opts into the old fallback
behavior.

## Flow IPC Host Path

`dev-ipc` starts the Java app as a Flow IPC sidecar, packages the application's
standard Vaadin production webapp and serves it from the local `vatori-app://`
scheme. The host:

- connects to Java through `VFIPC1` stdin/stdout envelopes.
- installs a document-start bridge that adapts application-origin XHR, Fetch and
  Atmosphere calls to opaque IPC exchanges.
- forwards method, URL, headers and body bytes without inspecting Vaadin request
  parameters, bootstrap data or UIDL.
- records `HOST_PROTOCOL` trace lines for launch, resource, command/result and
  shutdown evidence.
- owns AppKit file dialog, clipboard, notification and file-transfer capability
  behavior for accepted smoke paths.

Repository acceptance builds additionally compile DOM inspection and snapshot
support from `test-support/vaadin-desktop-host-swift-acceptance`; default host
builds neither inspect nor capture application windows. Acceptance traces are
compact by default: resource bodies are summarized, repeated Java envelopes are
logged once, and DOM diagnostics are reduced to readiness counters. Set
`VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full` only when a run must retain the
complete metadata consumed by `flow-ipc-trace-inspect`.

## Standard Production Bundle Path

The standard bundle is the default `dev-ipc` path. The launcher runs Vaadin's
production frontend build, refreshes the runnable application JAR and fails fast
if the webapp is missing. It has no custom shell fallback.

An explicit acceptance build can add the repository-only standard-bundle
framework driver from `test-support`. Normal host builds do not compile that
driver, and standard-bundle application resources contain neither the legacy
shell nor its acceptance JavaScript.

Unexpected sidecar exits use a bounded restart policy on this standard bundle
path. The host fails outstanding opaque requests, disconnects old push
subscriptions, keeps each NSWindow/WKWebView, and reloads the production webapp
after a replacement sidecar reports ready. `VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RESTART_LIMIT`
sets the restart limit (`3` by default, `0` disables it), while
`VAADIN_DESKTOP_FLOW_IPC_SIDECAR_RECOVERY_READY_TIMEOUT_SECONDS` bounds each
replacement's ready wait. Compatibility paths remain separate.
Recovery creates new Flow sessions and does not preserve process-local server UI
state.

Each Flow IPC window has a `FlowIpcWindowContext` containing its host-assigned
scope, NSWindow, WKWebView and desktop command executor. Incoming Java envelopes
must match that scope before they are delivered. Closing a window sends
`windowClosed` and removes only that context; the sidecar exits after the final
window closes.

## Build

```bash
bash bin/vaadin-desktop-build-swift-host /Users/bytedance/IdeaProjects/vatori
```

The build script uses `/usr/bin/swiftc`. Standard-bundle launches copy
`META-INF/VAADIN/webapp` and the opaque bridge into the app resources. Explicit
acceptance builds additionally compile the repository-only automation overlay.

## Automation Hooks

Automation environment variables live in the Swift host because only the host can
drive the real WKWebView DOM, file dialog cancellation, snapshot timing and
window shutdown path. They must remain opt-in and suitable for unattended local or
CI smoke runs.

Use `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1` for automated runs so the validation
window does not steal focus.

## Current Limits

The host proves bounded no-localhost Flow IPC behavior. It does not yet provide
full production packaging, durable reconnect, complete native menu/context-menu
parity or cross-platform host parity.
