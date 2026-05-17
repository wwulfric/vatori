# Framework Architecture

Vaadin Desktop Framework v0 is a staged implementation of the architecture described in `docs/flow-over-ipc-design.md`.

## Current v0 Stack

```text
Vaadin Flow View (@Route, Button, Grid)
  -> vaadin-desktop-flow
  -> embedded Spring Boot / Vaadin compatibility adapter
  -> local loopback URL
  -> vaadin-desktop-host-swift
  -> Swift/AppKit/WKWebView native host
  -> Host Protocol v0 trace / Desktop runtime command bridge
```

This is intentionally marked as **compatibility mode** for the Flow UI transport. It provides a real developer-facing framework, real Vaadin Flow applications and a real Swift/AppKit desktop host while keeping HTTP/Servlet isolated behind `DesktopLauncher`.

The older `SystemWebViewDesktopHost` remains as a macOS fallback path.

## Current IPC Slice

```text
Vaadin Flow View (@Route, Button, Span, Layout)
  -> FlowIpcLauncher / FlowIpcRuntime
  -> VFIPC1 stdin/stdout envelopes
  -> vaadin-desktop-host-swift
  -> WKWebView local HTML shell
```

This path is started with:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

It does not use localhost HTTP for bootstrap/push/event/patch/resync in the smoke app. The current WebView consumes real Vaadin UIDL through a minimum repository-local applier for the smoke subset, while the repository-local rendered tree remains as fallback. Bootstrap also advertises real Vaadin `flow-client` classpath resources, and the WebView loads `FlowBootstrap.js` through IPC as the first local-resource bridge.

The Java side emits real server-side Vaadin UIDL under `payload.uidl`: bootstrap, one server-initiated push and verification resync come from `UidlWriter`, and click event patches come from `UidlRequestHandler`/`ServerRpcHandler`. The current WebView shell applies a minimum subset of UIDL `changes`; replacing that applier with the real Flow client engine is the next transport milestone.

## Target Stack

```text
Vaadin Flow View
  -> Flow Transport Abstraction
  -> IPC Bridge
  -> Native Host
  -> System WebView
  -> Native Capability Gateway
```

The target stack removes local HTTP from the UI event/patch path. Framework v0 keeps module boundaries aligned with that target.

## Module Responsibilities

- `vaadin-desktop-core`: public Java API for desktop apps.
- `vaadin-desktop-flow`: compatibility adapter plus the first `FlowIpcLauncher`/`FlowIpcRuntime` and UIDL writer slice.
- `vaadin-desktop-ipc`: Host Protocol v0 envelope plus Flow IPC Protocol v1 envelope.
- `vaadin-desktop-host`: Java host abstractions, macOS WKWebView fallback and native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host spike that owns sidecar process lifecycle and system WebView.
- `vaadin-desktop-devtools`: Flow IPC trace inspector and diagnostics.
- `vaadin-desktop-cli`: developer command entry.
- `flow-transport-adapter-spike`: code-level analysis and draft adapter shape for real Flow UIDL/event boundaries.

## Relationship to Tauri and Electron

Like Tauri, the intended shape is a small native host plus system WebView plus backend runtime. Unlike Electron, the framework does not bundle Chromium. The macOS native implementation is now Swift/AppKit/WKWebView so the host can directly own platform behavior while the main UI programming model remains Vaadin Flow Java views.

## Relationship to Vaadin Flow

Framework v0 uses real Vaadin Flow through `vaadin-spring-boot-starter`. Developers write standard Flow views with `@Route`, `Button`, `Grid`, `Span`, layouts and listeners.

The current gap is transport completeness: `dev-ipc` removes HTTP for one constrained smoke app, has the WebView bridge emit Flow-client-shaped click RPC payloads, routes those events through the server-side Flow UIDL RPC path, and can fetch a real Flow bootstrap resource over IPC. Full Flow still needs a real Flow client engine bridge that executes those resources, consumes UIDL and covers broader events/resources.
