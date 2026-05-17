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
- `vaadin-desktop-flow`: compatibility adapter today; future home for Flow transport adapter.
- `vaadin-desktop-ipc`: message envelope, trace and later stdio/socket transport.
- `vaadin-desktop-host`: Java host abstractions, macOS WKWebView fallback and native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host spike that owns sidecar process lifecycle and system WebView.
- `vaadin-desktop-devtools`: inspector and diagnostics.
- `vaadin-desktop-cli`: developer command entry.
- `flow-transport-adapter-spike`: code-level analysis and draft adapter shape for real Flow UIDL/event boundaries.

## Relationship to Tauri and Electron

Like Tauri, the intended shape is a small native host plus system WebView plus backend runtime. Unlike Electron, the framework does not bundle Chromium. The macOS native implementation is now Swift/AppKit/WKWebView so the host can directly own platform behavior while the main UI programming model remains Vaadin Flow Java views.

## Relationship to Vaadin Flow

Framework v0 uses real Vaadin Flow through `vaadin-spring-boot-starter`. Developers write standard Flow views with `@Route`, `Button`, `Grid`, `Span`, layouts and listeners.

The current gap is transport: Flow still communicates over embedded HTTP/WebSocket. The repository now documents the first realistic extraction point around `UidlRequestHandler`/`ServerRpcHandler`/`UidlWriter`, but it has not removed `VaadinRequest` from the event path yet.
