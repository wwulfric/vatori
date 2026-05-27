# Vaadin Desktop Framework v0

This repository explores a Vaadin Flow desktop runtime inspired by Tauri/Raycast-style process boundaries. The current deliverable is a real framework skeleton, not only the earlier mock IPC spike.

Framework v0 runs in **compatibility mode** by default: real Vaadin Flow views are served by embedded Spring Boot on `127.0.0.1`, while the desktop runtime API, host boundary, IPC envelope and trace logs are already separated so they can evolve toward Flow over IPC.

The repository now also contains the first `dev-ipc` runtime slice. It runs a constrained Flow-over-IPC smoke app without a localhost HTTP server in the core UI path, sends WebView click, native input `change`, native checkbox `change`, real BigDecimalField/TextField/Checkbox Web Component `change` events and recovery resync as Flow-client-shaped UIDL RPC payloads, emits real server-side Vaadin UIDL and dispatches those messages through `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, two ordered server-initiated pushes, event patches and recovery resync are consumed by the visible Flow client engine; a controlled duplicate/late push probe is ignored and triggers explicit resync diagnostics. Bootstrap also advertises real Vaadin `flow-client` classpath resources and the v1.4 frontend artifact manifest; the WebView requests and verifies the full advertised resource graph over IPC, imports and initializes `FlowBootstrap.js` and an augmented `FlowClient.js`, records executed vs verified-only resource classification, and performs a minimum no-HTTP lazy lookup probe for `Flow.js`. The component graph covers `com.vaadin.flow.component.textfield.BigDecimalField`, `TextField`, `Checkbox` and the framework-app `Select` path with real classpath or focused dev-bundle frontend artifacts, manifest import rewrites and strict trace evidence for resource resolution, event RPC, Java listener patch and visible DOM state. The v1.5-v1.18 resource slices add Blob-backed no-localhost staging, stable resource-path module identity, TextField side-effect split, generated/lazy relative dependency resolution, a real Flow `LoadMode.LAZY` dependency executed through the visible Flow DependencyLoader path, a production-bundle runtime registry staged execution attempt, a real UIDL generated `Flow.loadOnDemand(chunkId)` request mapped to the generated-flow-imports artifact, a v1.12 parser that proves the current Vaadin generated-flow-imports body is a no-op table with `chunkFileStatus=not-generated`, a v1.13 production bundle runtime owner boundary, a v1.14 controlled owner import / side-effect probe for the stats-selected production `indexhtml` artifact, a v1.15 owner-scoped dependency resolver closure, a v1.16 owner import-ordering / component-reuse boundary, a v1.17 host-owned `vatori-resource` file-backed loader boundary and a v1.18 production owner custom-scheme resource graph boundary. v1.18 moves the stats-selected production owner root and resolved owner dependencies to `vatori-resource://flow-ipc/production-owner/...` file-backed URLs with root/dependency `resourceLoad` request/response evidence and root Blob fallback disabled for the v1.18 gate. These are framework-resource boundary proofs, not broad component/resource coverage or full production bundle/dev-mode parity. The desktop capability gateway also has a bounded v1.1 file-transfer smoke boundary for controlled-path download writes and upload/select-file metadata reads.

The v1.7 slice supersedes the v1.6 TextField blocker: TextField and its production `indexhtml` dependency use stable resource-path Blob module identity, so the production chunk is reused instead of re-imported under a second Blob URL, and a real Flow `TextField` event updates visible state to `Vaadin TextField: Flow IPC typed`. The v1.8 slice then extends the same staging resolver to the generated/lazy graph: the real Vaadin dev-bundle `generated-flow-imports-VW7hRaNw.js` chunk now rewrites its `./markdown-helpers-31I0A-hg.js` relative import to a stable staged module identity, imports the real helper body, and records both artifacts as executed `resourceStage` paths. v1.9 narrows the `Flow.loadOnDemand` gap to a diagnostic bridge / API registry blocker. v1.10 adds a separate real Flow `LoadMode.LAZY` JavaScript dependency: the visible Flow DependencyLoader requests `context://vatori-flow-ipc-lazy-v1_10.js`, the host resolves it through IPC, stages it as a Blob module URL and executes it. v1.10 also selects the production `indexhtml` chunk from the Vaadin dev-bundle stats registry and records a staged `execution-attempt` blocker for the remaining production runtime ownership layer. v1.11 captures a real bootstrap UIDL `Flow.loadOnDemand('<chunkId>')` expression, observes the visible Flow client trigger, maps the chunk request to `generated-flow-imports-VW7hRaNw.js`, stages/imports it once by artifact identity, and records the remaining blocker as `dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file`. v1.12 parses the real generated-flow-imports body, records `loadOnDemandTableShape=no-op-promise-resolve-0`, resolves the current outcome to `chunk-file-not-generated`, and keeps callback resolution distinct from actual chunk-file execution. v1.13 adds an explicit production bundle runtime owner: manifest/request/response/stage/DOM evidence records `productionBundleRuntimeOwnerVersion=flow-ipc-production-owner-v1_13`, `owner=production-bundle-runtime`, owner module identity, cache/cleanup policy, registry snapshots and `componentProductionOwnerReused=false`. v1.14 then makes that owner attempt a controlled dynamic import of the same stats-selected `indexhtml` artifact after component imports, with `productionBundleRuntimeOwnerImportVersion=flow-ipc-production-owner-import-v1_14`, import preflight, relative dependency rewrite diagnostics, `window.Vaadin` snapshots, registry before/after snapshots and a final blocker of `production-owner-relative-import-unresolved`. v1.15 closes the owner-scoped relative dependency resolver for existing real dev-bundle artifacts, stages 24 resolved owner dependencies, records 53 missing real artifacts by path/specifier, exposes isolated/pre-component phase candidates for later ordering diagnostics and classifies the final owner result as `production-owner-missing-real-artifact`. v1.16 records owner import phase evidence and component reuse decisions, including `before-component-imports` owner-first preflight and the current duplicate custom element / component reuse contract blocker. v1.17 installs a Swift-owned `vatori-resource` scheme handler and executes the Flow lazy dependency from a file-backed custom-scheme URL. v1.18 applies that host-owned loader to the production owner graph itself: the owner root and at least one resolved dependency are staged and served through stable source-path custom-scheme URLs, the WebView imports the owner root over that URL, and the trace records the remaining owner result at the custom-scheme graph layer instead of accepting Blob fallback. Full generated chunk-file execution, production owner import/side-effect success and dev-mode/Vite execution remain explicit blockers.

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

`dev-ipc` starts the same Swift/AppKit host with `transport=flow-ipc`, loads a local WKWebView shell, sends Flow client messages to the Java sidecar over `VFIPC1` stdin/stdout envelopes, and returns Java patches without loading `http://127.0.0.1`. Bootstrap payloads include real Vaadin server-side UIDL from `UidlWriter`; the runtime also emits two ordered server-initiated push UIDL updates and one controlled duplicate/late probe. The WebView bridge sends click, native input `change`, native checkbox `change`, `BigDecimalField` Web Component `change`, `Checkbox` Web Component `change` and ordering-recovery resync as UIDL RPC bodies with CSRF, `syncId`, `clientId`, event data and Flow node id, then Java handles them through `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap advertises the Flow client classpath resources and the v1.4 frontend artifact manifest; the WebView requests and verifies the Flow resource graph, echoes artifact metadata in `resourceRequest`, imports `FlowBootstrap.js` and `FlowClient.js`, dynamically imports the real `vaadin-big-decimal-field.js` module with manifest-driven fallback import rewrites for missing npm imports and a focused extracted dev-bundle `defineCustomElement` helper, dynamically imports focused dev-bundle entries backed by Vaadin's production `indexhtml` chunk for the real `vaadin-checkbox` and `vaadin-select` definitions, performs a dynamic `flow-client-lazy-lookup` request for `Flow.js`, records executed / verified-only / intentionally-not-executed / execution-failed classifications, and consumes bootstrap, push, event and resync DOM updates through the visible Flow client engine. Desktop capability results now support `allow-all-dev`, `manifest-enforced` and `trace-only-dry-run` permission modes; the smoke app uses `manifest-enforced`, exercises AppKit `NSOpenPanel` cancellation, emits a manifest deny-path, writes a bounded download body to a controlled path and reads upload/select-file metadata from a controlled test file. The verification hook can emit a WebView-originated `error` envelope; the repository-local UIDL applier remains only as a diagnostic fallback.

For unattended real-window verification, add `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1` before the `dev-ipc` command. The host no-activate flag starts the app in the background and skips AppKit activation so verification runs do not steal keyboard focus. The click hook waits for the Flow client XHR path when the component-event hook is active; the file dialog hook exercises the AppKit `NSOpenPanel` path and cancels it automatically; the Web Component hook dispatches `BigDecimalField`, `Checkbox` and real `TextField` DOM `change` events after their frontend resources are imported; the shutdown hook closes the window after DOM/snapshot capture so the host log includes shutdown evidence.

Run the framework-shaped Flow IPC validation app:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

This app keeps the strict fixture separate from a more ordinary developer project. It uses normal Flow Java APIs for a Button, `BigDecimalField`, `Checkbox`, `Select`, repeated list updates and manifest-gated desktop capability calls. Its semantic ids are only for optional diagnostics; the runtime's renderer promotion, layout, bridge installation and push/resync delivery must continue to use the generic Flow viewport and Flow metadata rather than app-specific ids.

For unattended framework-shape verification, use:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

That gate verifies generic viewport promotion, first meaningful paint, two Web Component events, the Button apply event, UIDL patches, a list item text update and `window.setTitle` / `clipboard.writeText` / `notification.show` command-result traces in a less smoke-specific app.

Inspect the Flow IPC trace sample:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_8 docs/traces/flow-ipc-v1_8.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9-loadondemand docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9 docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-lazy-dependency docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-production-runtime docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10 docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11-loadondemand-chunk docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11 docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12-loadondemand-chunk-file docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12 docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13-production-owner docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13 docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14-production-owner-import docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14 docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15-production-owner-dependencies docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15 docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16-production-owner-ordering docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16 docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17-resource-scheme docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17 docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18-production-owner-resource-scheme docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18 docs/traces/flow-ipc-v1_18.ndjson
```

Verify the first real Flow UIDL writer adapter slice:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

These commands construct a minimal desktop-shaped `VaadinSession`/`UI` in process. The first verifies that Vaadin's real `UidlWriter` emits UIDL `changes`, `syncId` and bootstrap `Vaadin-Security-Key`; the second sends a real Flow event RPC body through `UidlRequestHandler`/`ServerRpcHandler`, verifies a click listener UIDL patch, and confirms invalid Flow IPC `csrfToken`/security key, stale `syncId` and out-of-order `clientId` values are rejected before dispatch. The third verifies that invalid Flow IPC envelope `sessionId` / `uiId` values are rejected before event handling. The fourth verifies that the runtime can read and respond with every advertised Vaadin `flow-client` classpath resource, including `FlowBootstrap.js`, `FlowClient.js`, `Flow.js`, `index.js`, `compile.properties` and the generated GWT client bundle.

Expected diagnostic output: `SLF4J(W): No SLF4J providers` is harmless in these smoke commands. The UIDL RPC and scope smoke commands intentionally send invalid tokens, stale IDs and invalid scope values; output such as `invalid csrfToken rejected`, `invalid securityKey rejected`, `stale syncId rejected`, `out-of-order clientId rejected` and `invalid Flow IPC sessionId/uiId rejected` means the negative-path assertions passed. In unattended `dev-ipc` verification, `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` intentionally emits a `FLOW_IPC_CLIENT_ERROR ... webviewVerificationProbe` trace; success is determined by process exit code plus the matching `flow-ipc-trace-inspect` gate. The plain `dev`, `dev-swift` and `dev-ipc` commands are interactive desktop runs and stay alive until the window or app is closed; use the auto-shutdown forms above for repeatable smoke verification.

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
- `test-apps/flow-over-ipc-framework-app`: developer-shaped Flow IPC validation app for checking generic shell/runtime behavior beyond the strict smoke fixture.
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
- [Acceptance and Smoke Tools](docs/acceptance-tools.md)
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
- Real Vaadin Web Component and frontend staging slice: the smoke app uses `com.vaadin.flow.component.textfield.BigDecimalField`, `TextField` and `com.vaadin.flow.component.checkbox.Checkbox`; the framework app also covers `Select`. The WebView requests the real `vaadin-big-decimal-field.js` classpath frontend module plus focused Vaadin dev-bundle entries for TextField, Checkbox and Select over IPC, stages the real artifact bodies as Blob module URLs, executes BigDecimalField, TextField and the Checkbox/Select production class entries, records every staged artifact with `resourceStage`, and verifies Java listener -> `UidlRequestHandler` patch -> visible Flow client DOM states `Vaadin BigDecimalField: 42.5`, `Vaadin TextField: Flow IPC typed` and `Vaadin Checkbox: checked`. The v1.7 gate resolves the v1.6 duplicate `vaadin-text-field` custom element blocker with stable resource-path module identity. The v1.8 gate also executes the real Vaadin dev-bundle `generated-flow-imports-*.js` generated/lazy chunk after rewriting `./markdown-helpers-31I0A-hg.js` to a staged dependency. The v1.9 delta gate records the real `Flow.loadOnDemand` API owner/install source, requested chunk, registry/mapping source and resolved artifact path, then classifies the remaining gap as `flow-ipc-loadondemand-v1_9` with `frontendArtifactGeneratedLazyFlowClientTriggered=false`. The v1.10 delta gates verify a Flow `LoadMode.LAZY` dependency triggered by the visible Flow DependencyLoader and executed through the IPC resolver, plus a production-bundle stats registry artifact staged as `execution-attempt` with blocker `flow-ipc-production-runtime-v1_10`. The v1.11 delta gate verifies real UIDL generated `Flow.loadOnDemand(chunkId)` evidence, Flow-client-triggered chunk request, chunk-to-generated-flow-imports mapping, staging evidence and the `flow-ipc-loadondemand-chunk-v1_11` blocker. The v1.12 delta gate parses the real generated-flow-imports body, proves the current Vaadin build produced a no-op loadOnDemand table instead of a `./chunks/chunk-*.js` branch, and records `flow-ipc-loadondemand-chunk-file-v1_12` with `chunkFileStatus=not-generated`. The v1.13 delta gate verifies production bundle owner metadata and owner-scoped staging for the real stats-selected `indexhtml` artifact; it records a precise `production-owner-runtime-contract-missing` blocker instead of counting focused component import success as production owner success. The v1.14 delta gate verifies the same owner makes a real import attempt, records import preflight and side-effect/collision diagnostics, and narrows the failure to `production-owner-relative-import-unresolved` with browser duplicate custom element evidence. The v1.15 delta gate verifies the owner dependency resolver contract, requests/stages real existing owner dependencies, records graph counts/classifications and accepts only a downstream owner result such as `production-owner-missing-real-artifact`, not the old unresolved-relative-import blocker. The v1.16 delta gate verifies owner import ordering and component reuse decisions, including `before-component-imports` preflight, after-component fallback evidence and the current duplicate custom element / component reuse contract blocker. The v1.17 delta gate verifies Swift `WKURLSchemeHandler` installation, file-backed `vatori-resource://` request/response and executed Flow lazy dependency evidence. The v1.18 delta gate verifies production owner root/dependency custom-scheme request/response, stable `vatori-resource://flow-ipc/production-owner/...` URLs, root import over custom scheme and disabled root Blob fallback. Full generated chunk-file execution, production owner import/side-effect success and dev-mode/Vite execution remain blockers.
- Flow IPC v1.7 TextField execution: the TextField dev-bundle entry and its production `indexhtml` dependency are staged with stable module identity, the v1.6 duplicate custom element blocker is resolved, and the smoke app verifies a real Flow `TextField` Java listener producing `Vaadin TextField: Flow IPC typed` through the visible Flow client event/patch path. The legacy TextField shim is still present only as a fallback diagnostic.
- Flow IPC file-transfer smoke: Java sends `fileTransfer.download` and `fileTransfer.selectUploadFile` commands through the native capability gateway; Swift writes a bounded body to a controlled test path, reads selected-file metadata and sampled byte count, redacts body content in traces and reports typed `capabilityResult` metadata.
- Flow IPC framework validation app: `test-apps/flow-over-ipc-framework-app` runs a less smoke-specific Flow view with ordinary Java APIs, three real Vaadin Web Components (`BigDecimalField`, `Checkbox`, `Select`), a v1.10 lazy dependency declaration, repeated list updates and manifest-gated desktop capability calls. It is a framework-shape validation target; the strict v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9/v1.10/v1.11/v1.12/v1.13/v1.14/v1.15/v1.16/v1.17/v1.18 gates still live on the smoke fixture.
- UIDL adapter smoke: drives Vaadin's real server-side `UidlWriter` from an in-process desktop-shaped session/UI boundary.
- UIDL RPC smoke: drives a real Flow event RPC through `UidlRequestHandler`/`ServerRpcHandler` and verifies a listener-produced UIDL patch.
- Host Protocol v0 stdout envelope for Java desktop commands.
- Flow transport adapter spike with concrete Vaadin source references for UIDL/event/patch boundaries.
- Compatibility host fallback for headless/browser modes.
- IPC trace shape and command envelope.
- Independent smoke app that depends on the framework modules.

Not implemented yet:

- Broad Flow component and event coverage beyond Button, native input/checkbox and the current `BigDecimalField` / `TextField` / `Checkbox` / `Select` validation slice.
- Full Flow client UIDL parity, generated `Flow.loadOnDemand` chunk-file execution, dev mode replacement and production owner import/side-effect success beyond the current classification/lazy-lookup/frontend-staging/custom-scheme diagnostics.
- Durable Flow push/reconnect semantics beyond the controlled duplicate/late ordering recovery smoke.
- Full Vaadin Upload/Download parity, binary streaming, progress and sandbox-safe file grants.
- Production native capability permission model.
- Production packaging, signing, auto-update.
