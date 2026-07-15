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
- **Flow IPC slice**: an experimental no-localhost path that runs the
  application's standard Vaadin production bundle. Browser requests cross an
  opaque IPC bridge and are handled by Vaadin's normal request-handler chain.

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
bash bin/vaadin-desktop dev-ipc examples/counter-app
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

The `dev`, `dev-swift` and `dev-ipc` commands open desktop windows and stay
alive until the app/window is closed.

## Runtime Modes

| Mode | Command | Transport | Purpose |
| --- | --- | --- | --- |
| `dev` | `bash bin/vaadin-desktop dev examples/counter-app` | Local HTTP compatibility | Default developer loop with a native macOS WebView host. |
| `dev-swift` | `bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app` | Local HTTP compatibility | Swift/AppKit host path with Java sidecar lifecycle and host protocol tracing. |
| `dev-ipc` | `bash bin/vaadin-desktop dev-ipc examples/counter-app` | Opaque requests over `VFIPC1` IPC | No-localhost standard Vaadin production bundle path for the covered Flow surface. |

Host selection for compatibility mode:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

`system-webview` is fail-fast by default. Use `VAADIN_DESKTOP_HOST=compatibility`,
`headless` or `browser` for an explicit compatibility host. To preserve a local
development fallback from `system-webview`, set
`VAADIN_DESKTOP_HOST_FALLBACK=compatibility`.

## Verification

Run the central non-window checks:

```bash
bash bin/verify-smoke
```

`bin/verify-smoke` runs repository hygiene, inspector contract checks, opaque
HTTP adapter tests, Flow IPC scope tests and browser bridge tests. The script is
the source of truth for its current subcommands.

Window automation and trace-gate validation are documented in
`docs/acceptance-tools.md`. Gate names live in the inspector and acceptance docs,
not in this README.

Full `.ndjson` traces are generated artifacts and are not committed per version.
Acceptance lives in the inspector and smoke scripts. Preserve traces as CI
artifacts, release assets or external archives when needed.

## Repository Map

See [Repository Structure and Mental Model](docs/repository-structure.md) for
dependency direction, publishing boundaries and placement rules.

- `vaadin-desktop-core`: public Java desktop runtime API.
- `vaadin-desktop-ipc`: IPC envelope and trace primitives.
- `vaadin-desktop-host`: Java host abstractions, explicit compatibility host and
  native bridge support.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host for the native macOS
  runtime path.
- `vaadin-desktop-flow`: Vaadin Flow/Spring Boot launcher, standard production
  bundle runtime and opaque IPC transport adapter.
- `vaadin-desktop-cli`: CLI scaffold; shell entry is `bin/vaadin-desktop`.
- `bin/`: shell entry points for local development, smoke checks and hygiene.
- `docs/`: current architecture, runtime, protocol and acceptance docs.
- `assets/`: brand source assets and generated distribution-ready exports.
- `.agents/`: repository-local skills for recurring Codex workflows.
- `examples/`: small developer-facing sample apps.
- `test-apps/`: validation apps with deterministic hooks and broader assertion
  surfaces.
- `test-support/`: repository-only acceptance automation and trace gates; it is
  outside the published framework dependency graph.

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

## Status And Roadmap

The stable roadmap lives in `docs/roadmap.md`. Current Flow IPC runtime boundaries
live in `docs/flow-over-ipc-runtime.md`; acceptance tooling lives in
`docs/acceptance-tools.md`.

Do not treat the current Flow IPC validation surface as full product parity.
Compatibility mode, focused validation gates, fallback classifications and
blocker diagnostics should stay explicit when describing current behavior.

## Documentation

- [Getting Started](docs/getting-started.md)
- [Repository Structure and Mental Model](docs/repository-structure.md)
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
