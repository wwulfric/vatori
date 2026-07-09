# vatori

vatori is an experimental Vaadin-based desktop framework for building
desktop-class Vaadin Flow applications. The direction is similar to Tauri plus
Vaadin Flow: keep the Java Flow programming model, run inside a native desktop
host, and gradually move browser/server transport assumptions behind explicit
host and IPC boundaries.

This is not a polished application shell, and it is not yet the final Flow over
IPC runtime. The repository currently has two useful runtime paths:

- **Compatibility mode**: a real Vaadin Flow/Spring Boot app runs on
  `127.0.0.1`, and a native macOS WebView host opens it in a desktop window.
- **Flow IPC slice**: an experimental no-localhost path for a constrained Flow
  surface. Bootstrap, events, UIDL patches, frontend resources and selected
  desktop capabilities travel over IPC for the covered validation surface.

The README is intentionally a map. Detailed Flow IPC evidence and design notes
live under `docs/`.

## Quick Start

Requirements:

- JDK 21+
- Maven 3.9+
- macOS with Swift toolchain for the current native WebView hosts
- If Maven is installed through SDKMAN, the repository scripts load SDKMAN
  automatically

Common runs:

```bash
bash bin/vaadin-desktop dev examples/counter-app
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

The `dev`, `dev-swift` and `dev-ipc` commands open desktop windows and stay
alive until the app/window is closed.

## Runtime Modes

| Mode | Command | Transport | Purpose |
| --- | --- | --- | --- |
| `dev` | `bash bin/vaadin-desktop dev examples/counter-app` | Local HTTP compatibility | Default developer loop with a native macOS WebView host. |
| `dev-swift` | `bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app` | Local HTTP compatibility | Swift/AppKit host path with Java sidecar lifecycle and host protocol tracing. |
| `dev-ipc` | `bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app` | `VFIPC1` stdin/stdout IPC | No-localhost Flow IPC slice for covered bootstrap/event/patch/resource/capability behavior. |

Host selection for compatibility mode:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

## Verification

Run the central non-window checks:

```bash
bash bin/verify-smoke
```

`bin/verify-smoke` runs repository hygiene plus focused UIDL/resource smoke
checks:

```bash
bash bin/repository-hygiene-check
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

Run an unattended framework-window check:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

Then run the relevant `bin/flow-ipc-trace-inspect` gate for the surface being
validated. Gate names live in the inspector and acceptance docs, not in this
README.

Full `.ndjson` traces are generated artifacts and are not committed per version.
Acceptance lives in the inspector and smoke scripts. Preserve traces as CI
artifacts, release assets or external archives when needed.

## Repository Map

- `vaadin-desktop-core`: public Java desktop runtime API.
- `vaadin-desktop-ipc`: IPC envelope and trace primitives.
- `vaadin-desktop-host`: macOS system WebView fallback, compatibility host and
  Java native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host for the native macOS
  runtime path.
- `vaadin-desktop-flow`: Vaadin Flow/Spring Boot launcher, Flow IPC runtime and
  UIDL adapter slices.
- `vaadin-desktop-devtools`: trace inspector scaffold and Flow IPC gates.
- `vaadin-desktop-cli`: CLI scaffold; shell entry is `bin/vaadin-desktop`.
- `examples/`: small developer-facing sample apps.
- `test-apps/`: validation apps with deterministic hooks and broader assertion
  surfaces.
- `flow-transport-adapter-spike`: Vaadin Flow transport boundary analysis.
- `prototypes/phase0-phase1-spike`: earlier non-HTTP mock transport and IPC
  validation.

## Examples vs Test Apps

Use `examples/` for readable developer stories:

- `examples/counter-app`
- `examples/grid-app`
- `examples/desktop-capabilities-app`
- `examples/component-gallery-app`

Use `test-apps/` for repeatable validation:

- `test-apps/vaadin-desktop-smoke-app`
- `test-apps/flow-over-ipc-smoke-app`
- `test-apps/flow-over-ipc-framework-app`

If a scenario is useful both as documentation and as a test, keep the simple
developer story in `examples/` and the exhaustive assertion surface in
`test-apps/`.

## Current Status

Implemented:

- Real Vaadin Flow app modules and sample projects.
- Java desktop API for window title, notifications, clipboard, file dialog,
  bounded file transfer and menu facade.
- macOS WebView compatibility host.
- Swift/AppKit host that owns Java sidecar lifecycle and host protocol tracing.
- Flow IPC runtime slice with real server-side UIDL bootstrap/event/patch
  handling for the covered smoke apps.
- Flow client resource loading and frontend artifact resolver evidence for the
  current validation surface.
- Framework-shaped validation app covering representative Flow components,
  Binder validation, common input components, multi-value/navigation/disclosure
  and menu components, bounded overlay/context-menu components, StreamResource,
  Upload, DownloadHandler and selected desktop capability calls.
- Basic no-localhost frontend/resource pipeline closure for the current
  validation surface.
- Central hygiene and smoke verification through `bin/verify-smoke`.

In progress:

- Rich content, app shell and login component coverage for Avatar, MessageList,
  MessageInput, SideNav and LoginOverlay. This work is still behind explicit
  validation gates and should not be described as complete.

Still open:

- Full Flow client event coverage and UIDL parity.
- Dev-mode/Vite parity and generalized frontend owner/runtime reuse.
- Durable push, reconnect and resync semantics.
- Full Grid virtualization and large-file Upload/Download streaming.
- Full overlay focus/keyboard/native context-menu behavior, rich overlay
  behavior, theme variants and deeper focus/accessibility behavior.
- Production native capability permission model.
- Packaging, signing and auto-update.

## Documentation

- [Getting Started](docs/getting-started.md)
- [Framework Architecture](docs/framework-architecture.md)
- [Flow Integration](docs/flow-integration.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [IPC Protocol](docs/ipc-protocol.md)
- [IPC Protocol v1](docs/ipc-protocol-v1.md)
- [Acceptance and Smoke Tools](docs/acceptance-tools.md)
- [Repository Hygiene and Test Layout](docs/repository-hygiene.md)
- [Native Host](docs/native-host.md)
- [Roadmap](docs/roadmap.md)
- [Original Design Package](docs/flow-over-ipc-design.md)
