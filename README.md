# Vaadin Desktop Framework v0

This repository explores a Vaadin Flow desktop runtime inspired by Tauri/Raycast-style process boundaries. The current deliverable is a real framework skeleton, not only the earlier mock IPC spike.

Framework v0 runs in **compatibility mode**: real Vaadin Flow views are served by embedded Spring Boot on `127.0.0.1`, while the desktop runtime API, host boundary, IPC envelope and trace logs are already separated so they can evolve toward Flow over IPC.

## Quick Start

Requirements:

- JDK 21+.
- Maven 3.9+.
- macOS with Swift toolchain for the current system WebView host.
- macOS with Swift toolchain for the Swift/AppKit native host.
- If Maven is installed via SDKMAN, `bin/vaadin-desktop` sources SDKMAN automatically.

Run the counter example:

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

Run the independent smoke app:

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

Run the next-stage Swift native host path:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

`dev-swift` builds `VaadinDesktopHost.app` and starts it through macOS LaunchServices, launches the Java Vaadin app as a sidecar, waits for the compatibility URL, opens an AppKit `NSWindow` with `WKWebView` and watches the Java process lifecycle. Host logs are tailed from `.build/vaadin-desktop-host-swift/host.out.log` and `.build/vaadin-desktop-host-swift/host.err.log`; the host also writes `.build/vaadin-desktop-host-swift/window-snapshot.png` after the WebView renders. Desktop API calls can now emit Host Protocol v0 command envelopes from Java to the host. The Flow UI transport still uses localhost HTTP, so this is a long-term host/runtime prototype, not final Flow over IPC yet.

On macOS the original `dev` command still opens a native WKWebView desktop window by default. That path is now the macOS fallback/compatibility host.

Host selection:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

## Modules

- `vaadin-desktop-core`: Java desktop runtime API.
- `vaadin-desktop-ipc`: IPC envelope and trace logging primitives.
- `vaadin-desktop-host`: macOS system WebView fallback, compatibility host and Java native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host for the macOS native runtime path.
- `vaadin-desktop-flow`: real Vaadin Flow/Spring Boot integration layer.
- `vaadin-desktop-devtools`: trace inspector scaffold.
- `vaadin-desktop-cli`: CLI scaffold; shell entry is `bin/vaadin-desktop`.
- `examples/counter-app`: real Vaadin Flow counter app.
- `examples/grid-app`: real Vaadin Flow Grid app.
- `examples/desktop-capabilities-app`: desktop runtime API example.
- `test-apps/vaadin-desktop-smoke-app`: independent test project using the framework.
- `flow-transport-adapter-spike`: code-level Vaadin Flow transport boundary analysis and adapter shape.
- `prototypes/phase0-phase1-spike`: earlier non-HTTP mock transport and IPC validation.

## Documentation

- [Framework Architecture](docs/framework-architecture.md)
- [Getting Started](docs/getting-started.md)
- [Flow Integration](docs/flow-integration.md)
- [IPC Protocol](docs/ipc-protocol.md)
- [Native Host](docs/native-host.md)
- [Roadmap](docs/roadmap.md)
- [Original Design Package](docs/flow-over-ipc-design.md)

## Current Status

Implemented:

- Real Vaadin Flow application modules.
- Framework-level Java desktop API: window title, notification, clipboard, file dialog, menu facade.
- macOS system WebView launcher: Spring Boot starts Vaadin on random localhost port; a generated WKWebView `.app` opens the desktop window.
- Swift/AppKit host: starts Java sidecar, opens WKWebView, traces Host Protocol v0 and cleans up the sidecar on window shutdown.
- Host Protocol v0 stdout envelope for Java desktop commands.
- Flow transport adapter spike with concrete Vaadin source references for UIDL/event/patch boundaries.
- Compatibility host fallback for headless/browser modes.
- IPC trace shape and command envelope.
- Independent smoke app that depends on the framework modules.

Not implemented yet:

- Real Flow over IPC without HTTP/Servlet.
- Native capability permission manifest.
- Production packaging, signing, auto-update.
