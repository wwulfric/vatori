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

Current v0:

```text
SystemWebViewDesktopHost / vaadin-desktop-host-swift
  -> local HTTP compatibility URL
  -> Vaadin Flow/Spring Boot
```

Target:

```text
Swift/AppKit Host on macOS
  -> host bridge / IPC
  -> Flow transport adapter
  -> Vaadin Flow runtime
```

The current host can be native-feeling at the window level while still being transport-incomplete. Keep that distinction visible in code names, trace logs and docs.

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
vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift
vaadin-desktop-host/src/main/resources/dev/vatori/desktop/host/macos-webview-host.swift
```

The host protocol should be designed before adding features. Minimum messages:

- `launch`
- `ready`
- `window.created`
- `window.closed`
- `command`
- `capabilityResult`
- `error`
- `shutdown`
