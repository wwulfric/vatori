# Vaadin Desktop Framework v0

This repository explores a Vaadin Flow desktop runtime inspired by Tauri/Raycast-style process boundaries. The current deliverable is a real framework skeleton, not only the earlier mock IPC spike.

Framework v0 runs in **compatibility mode** by default: real Vaadin Flow views are served by embedded Spring Boot on `127.0.0.1`, while the desktop runtime API, host boundary, IPC envelope and trace logs are already separated so they can evolve toward Flow over IPC.

The repository now also contains the first `dev-ipc` runtime slice. It runs a constrained Flow-over-IPC smoke app without a localhost HTTP server in the core event/patch path and now emits real server-side Vaadin UIDL in the IPC payload. This is still not full Flow client-engine parity because the WebView consumes a repository-local tree patch.

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

Run the first Flow IPC runtime slice:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`dev-ipc` starts the same Swift/AppKit host with `transport=flow-ipc`, loads a local WKWebView shell, sends WebView events to the Java sidecar over `VFIPC1` stdin/stdout envelopes, and applies Java patches without loading `http://127.0.0.1`. The payload now includes real Vaadin server-side UIDL from `UidlWriter`, but the WebView still applies a repository-local component tree until the Flow client engine bridge is implemented.

Inspect the Flow IPC trace sample:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
```

Verify the first real Flow UIDL writer adapter slice:

```bash
bash bin/uidl-adapter-smoke
```

This command constructs a minimal desktop-shaped `VaadinSession`/`UI` in process and verifies that Vaadin's real `UidlWriter` emits UIDL `changes` and `syncId`.

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
- `vaadin-desktop-flow`: real Vaadin Flow/Spring Boot integration layer plus the first compiled UIDL transport adapter slice.
- `vaadin-desktop-devtools`: trace inspector for Flow IPC review traces.
- `vaadin-desktop-cli`: CLI scaffold; shell entry is `bin/vaadin-desktop`.
- `examples/counter-app`: real Vaadin Flow counter app.
- `examples/grid-app`: real Vaadin Flow Grid app.
- `examples/desktop-capabilities-app`: desktop runtime API example.
- `test-apps/vaadin-desktop-smoke-app`: independent test project using the framework.
- `test-apps/flow-over-ipc-smoke-app`: independent test project for the `dev-ipc` runtime slice.
- `flow-transport-adapter-spike`: code-level Vaadin Flow transport boundary analysis and adapter shape.
- `prototypes/phase0-phase1-spike`: earlier non-HTTP mock transport and IPC validation.

## Documentation

- [Framework Architecture](docs/framework-architecture.md)
- [Getting Started](docs/getting-started.md)
- [Flow Integration](docs/flow-integration.md)
- [IPC Protocol](docs/ipc-protocol.md)
- [IPC Protocol v1](docs/ipc-protocol-v1.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [Native Host](docs/native-host.md)
- [Roadmap](docs/roadmap.md)
- [Original Design Package](docs/flow-over-ipc-design.md)

## Current Status

Implemented:

- Real Vaadin Flow application modules.
- Framework-level Java desktop API: window title, notification, clipboard, file dialog, menu facade.
- macOS system WebView launcher: Spring Boot starts Vaadin on random localhost port; a generated WKWebView `.app` opens the desktop window.
- Swift/AppKit host: starts Java sidecar, opens WKWebView, traces Host Protocol v0 and cleans up the sidecar on window shutdown.
- Flow IPC runtime slice: starts a no-HTTP smoke app, dispatches Button click over IPC, generates real server-side UIDL, sends desktop API calls as `VFIPC1 command`, receives `capabilityResult`, and applies a replace-root patch in the WebView shell.
- UIDL adapter smoke: drives Vaadin's real server-side `UidlWriter` from an in-process desktop-shaped session/UI boundary.
- Host Protocol v0 stdout envelope for Java desktop commands.
- Flow transport adapter spike with concrete Vaadin source references for UIDL/event/patch boundaries.
- Compatibility host fallback for headless/browser modes.
- IPC trace shape and command envelope.
- Independent smoke app that depends on the framework modules.

Not implemented yet:

- Flow-client-engine consumption of the generated UIDL over IPC.
- Native capability permission manifest.
- Production packaging, signing, auto-update.
