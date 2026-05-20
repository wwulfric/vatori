# Vaadin Desktop Framework v0

This repository explores a Vaadin Flow desktop runtime inspired by Tauri/Raycast-style process boundaries. The current deliverable is a real framework skeleton, not only the earlier mock IPC spike.

Framework v0 runs in **compatibility mode** by default: real Vaadin Flow views are served by embedded Spring Boot on `127.0.0.1`, while the desktop runtime API, host boundary, IPC envelope and trace logs are already separated so they can evolve toward Flow over IPC.

The repository now also contains the first `dev-ipc` runtime slice. It runs a constrained Flow-over-IPC smoke app without a localhost HTTP server in the core UI path, sends WebView click, native input `change`, checkbox `change`, one real Vaadin Web Component `change` and recovery resync as Flow-client-shaped UIDL RPC payloads, emits real server-side Vaadin UIDL and dispatches those messages through `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, two ordered server-initiated pushes, event patches and recovery resync are consumed by the visible Flow client engine; a controlled duplicate/late push probe is ignored and triggers explicit resync diagnostics. Bootstrap also advertises real Vaadin `flow-client` classpath resources; the WebView requests and verifies the full advertised resource graph over IPC, imports and initializes `FlowBootstrap.js` and an augmented `FlowClient.js`, records executed vs verified-only resource classification, and performs a minimum no-HTTP lazy lookup probe for `Flow.js`. The v1.2 slice also requests and executes the real classpath frontend module `META-INF/resources/frontend/vaadin-big-decimal-field.js` for `com.vaadin.flow.component.textfield.BigDecimalField`; its missing npm transitive imports are explicitly classified and bridged by v1.2-only shims, so this is evidence for the first Web Component boundary rather than full TextField/npm/dev-mode parity. The desktop capability gateway also has a bounded v1.1 file-transfer smoke boundary for controlled-path download writes and upload/select-file metadata reads. This is still not full Flow transport parity because broad component/resource coverage, durable push queue/reconnect, complete upload/download streaming and production bundle/dev-mode semantics remain future work.

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

`dev-ipc` starts the same Swift/AppKit host with `transport=flow-ipc`, loads a local WKWebView shell, sends Flow client messages to the Java sidecar over `VFIPC1` stdin/stdout envelopes, and returns Java patches without loading `http://127.0.0.1`. Bootstrap payloads include real Vaadin server-side UIDL from `UidlWriter`; the runtime also emits two ordered server-initiated push UIDL updates and one controlled duplicate/late probe. The WebView bridge sends click, native input `change`, checkbox `change`, `BigDecimalField` Web Component `change` and ordering-recovery resync as UIDL RPC bodies with CSRF, `syncId`, `clientId`, event data and Flow node id, then Java handles them through `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap advertises the Flow client classpath resources and the v1.2 component frontend resources; the WebView requests and verifies the Flow resource graph, imports `FlowBootstrap.js` and `FlowClient.js`, dynamically imports the real `vaadin-big-decimal-field.js` module with explicitly traced v1.2 shims for missing npm imports, performs a dynamic `flow-client-lazy-lookup` request for `Flow.js`, records executed / verified-only / intentionally-not-executed / execution-failed classifications, and consumes bootstrap, push, event and resync DOM updates through the visible Flow client engine. Desktop capability results now support `allow-all-dev`, `manifest-enforced` and `trace-only-dry-run` permission modes; the smoke app uses `manifest-enforced`, exercises AppKit `NSOpenPanel` cancellation, emits a manifest deny-path, writes a bounded download body to a controlled path and reads upload/select-file metadata from a controlled test file. The verification hook can emit a WebView-originated `error` envelope; the repository-local UIDL applier remains only as a diagnostic fallback.

For unattended real-window verification, add `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1` before the `dev-ipc` command. The file dialog hook exercises the AppKit `NSOpenPanel` path and cancels it automatically; the Web Component hook dispatches the `BigDecimalField` DOM `change` event after its frontend module is imported; the shutdown hook closes the window after DOM/snapshot capture so the host log includes shutdown evidence.

Inspect the Flow IPC trace sample:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
```

Verify the first real Flow UIDL writer adapter slice:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

These commands construct a minimal desktop-shaped `VaadinSession`/`UI` in process. The first verifies that Vaadin's real `UidlWriter` emits UIDL `changes`, `syncId` and bootstrap `Vaadin-Security-Key`; the second sends a real Flow event RPC body through `UidlRequestHandler`/`ServerRpcHandler`, verifies a click listener UIDL patch, and confirms invalid Flow IPC `csrfToken`/security key, stale `syncId` and out-of-order `clientId` values are rejected before dispatch. The third verifies that invalid Flow IPC envelope `sessionId` / `uiId` values are rejected before event handling. The fourth verifies that the runtime can read and respond with every advertised Vaadin `flow-client` classpath resource, including `FlowBootstrap.js`, `FlowClient.js`, `Flow.js`, `index.js`, `compile.properties` and the generated GWT client bundle.

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
- Framework-level Java desktop API: window title, notification, clipboard, file dialog, bounded file transfer and menu facade.
- macOS system WebView launcher: Spring Boot starts Vaadin on random localhost port; a generated WKWebView `.app` opens the desktop window.
- Swift/AppKit host: starts Java sidecar, opens WKWebView, traces Host Protocol v0 and cleans up the sidecar on window shutdown.
- Flow IPC runtime slice: starts a no-HTTP smoke app, dispatches Button click over IPC through `UidlRequestHandler`/`ServerRpcHandler`, generates real server-side UIDL, sends desktop API calls as `VFIPC1 command`, receives `capabilityResult`, and has the visible Flow client engine consume bootstrap, ordered push, event and recovery resync UIDL.
- Flow IPC push ordering smoke: Java emits two ordered server push UIDL updates plus a controlled duplicate/late probe; the WebView records delivered/ignored order and triggers explicit ordering-recovery resync.
- Flow client resource loading probe: `dev-ipc` advertises Vaadin `flow-client` classpath resources, the WebView requests the full advertised resource graph through `resourceRequest`, Java responds with redacted `resourceResponse` traces, diagnostics verify all responses plus real Flow bootstrap and GWT client module import/init/binding, and v1.1 DOM evidence classifies resources as executed, verified-only, intentionally not executed or execution-failed. The same channel now includes a dynamic `flow-client-lazy-lookup` probe for `Flow.js`.
- First real Vaadin Web Component slice: the smoke app uses `com.vaadin.flow.component.textfield.BigDecimalField`, the WebView requests its real `vaadin-big-decimal-field.js` classpath frontend module over IPC, imports it in the no-localhost shell with explicitly classified v1.2 shims for missing npm transitive imports, dispatches a DOM `change` event from the upgraded `vaadin-big-decimal-field`, and verifies Java listener -> `UidlRequestHandler` patch -> visible Flow client DOM state `Vaadin BigDecimalField: 42.5`.
- Flow IPC file-transfer smoke: Java sends `fileTransfer.download` and `fileTransfer.selectUploadFile` commands through the native capability gateway; Swift writes a bounded body to a controlled test path, reads selected-file metadata and sampled byte count, redacts body content in traces and reports typed `capabilityResult` metadata.
- UIDL adapter smoke: drives Vaadin's real server-side `UidlWriter` from an in-process desktop-shaped session/UI boundary.
- UIDL RPC smoke: drives a real Flow event RPC through `UidlRequestHandler`/`ServerRpcHandler` and verifies a listener-produced UIDL patch.
- Host Protocol v0 stdout envelope for Java desktop commands.
- Flow transport adapter spike with concrete Vaadin source references for UIDL/event/patch boundaries.
- Compatibility host fallback for headless/browser modes.
- IPC trace shape and command envelope.
- Independent smoke app that depends on the framework modules.

Not implemented yet:

- Broad Flow component and event coverage beyond Button, native input/checkbox and the v1.2 `BigDecimalField` slice.
- Full Flow client UIDL parity, dev mode replacement and production frontend bundle execution beyond the current classification/lazy-lookup/component-shim diagnostics.
- Durable Flow push/reconnect semantics beyond the controlled duplicate/late ordering recovery smoke.
- Full Vaadin Upload/Download parity, binary streaming, progress and sandbox-safe file grants.
- Production native capability permission model.
- Production packaging, signing, auto-update.
