# Architecture Guidance

Use this when deciding where a feature belongs in Vaadin Desktop.

## Layer Map

```text
Layer 1: Native Host
  Swift/AppKit first on macOS, platform code where needed.
  Owns windows, menus, tray, global shortcuts, drag/drop, file dialogs,
  notifications, clipboard, app activation, process supervision, packaging.

Layer 2: System WebView
  WKWebView on macOS, WebView2 on Windows, WebKitGTK on Linux.
  Renders Vaadin Flow client assets. It is not a browser tab.

Layer 3: Java Flow Runtime
  Java sidecar/application process.
  Owns Flow component tree, routes, server-side listeners, data providers,
  UI state, business logic and Spring integration.

Layer 4: Native Capability Core
  Optional for filesystem indexing, global hooks, low-latency search,
  packaging helpers, signing, updater and crash isolation.
```

## This Repository's Current Shape

```text
bin/vaadin-desktop
  -> native-hosts/macos
  -> AppKit + WKWebView
  -> VFIPC1 sidecar channel
  -> vaadin-desktop-flow
```

Platform and packaged artifact select the native host. `dev` uses the standard
Vaadin production bundle over Flow IPC. `dev-compat` uses the same host and
native capability channel while loading the UI from localhost HTTP. Keep that
transport distinction visible in code names, traces and docs.

## Placement Rules

- **Host:** anything that users expect the OS to own: app menu, window close/minimize/zoom, Dock/Taskbar, tray, file picker, save panel, clipboard formats, notifications, drag/drop, URL scheme, global shortcuts.
- **WebView/Vaadin UI:** component rendering, layout, forms, grids, content panels, validation UI, inline commands, developer-visible Vaadin APIs.
- **Java runtime:** Flow lifecycle, route/session/UI scope, business services, data providers, command dispatch, stateful application logic.
- **IPC:** host commands, capability calls, lifecycle events, desktop bridge events, future Flow bootstrap/event/patch transport.
- **Docs:** any temporary compatibility behavior that a user could confuse with final Flow over IPC.

## Native-Feel Failure Modes

- DOM modal used where a native sheet/dialog is expected.
- Web cursor, browser context menu, selectable labels or webpage scroll behavior in app chrome.
- Host starts app but does not own shutdown; Java process survives window close.
- Host command shape differs per platform.
- Desktop API works only in examples, not in an independent app.
- Flow route/page model leaks into desktop window lifecycle with no host abstraction.
- Startup shows white/black flash before first Vaadin paint.

## Swift Native Direction

Use Swift/AppKit/WKWebView as the macOS native runtime mainline. Tauri remains useful as a reference architecture for process boundaries and capability concepts, not as the current implementation stack.

```text
native-hosts/macos/Sources/VaadinDesktopHost/main.swift
```

The `VFIPC1` sidecar contract should be designed before adding features. Its
common lifecycle and capability messages include:

- `ready`
- `command`
- `capabilityResult`
- `windowClosed`
- `error`
- `shutdown`
