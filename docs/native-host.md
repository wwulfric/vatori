# Native Host

## Mainline Host Spike

`vaadin-desktop-host-swift` is the next-stage macOS mainline host prototype. It uses Swift, AppKit and WKWebView so the native runtime can directly own macOS window lifecycle and platform behavior.

Run:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

Responsibilities implemented now:

- starts the Java Vaadin app as a sidecar with `VAADIN_DESKTOP_HOST=headless`,
- opens a native AppKit `NSWindow` with `WKWebView` after the sidecar prints `Application URL: ...`,
- traces host lifecycle messages as `HOST_PROTOCOL ...`,
- emits `webview.dom` inspection and writes `.build/vaadin-desktop-host-swift/window-snapshot.png` after WebView render,
- reads Java desktop command envelopes prefixed with `VDP1 `,
- closes the Java process when the desktop window closes,
- in `dev-ipc`, loads a local WebView shell, receives WebView messages through `WKScriptMessageHandler`, forwards them to Java stdin and delivers Java `VFIPC1` responses back to WebView JavaScript,
- in `dev-ipc`, has the WebView request all advertised Flow client classpath resources through `VFIPC1 resourceRequest`, echoes v1.4 artifact family / module specifier / dependency / execution policy metadata in requests, imports `FlowBootstrap.js` and `FlowClient.js` as Blob-backed ES modules, calls `FlowBootstrap.init` with an IPC bootstrap config, then calls `FlowClient.init` and records DOM diagnostics,
- in `dev-ipc`, classifies Flow client resources as executed, verified-only, intentionally-not-executed or execution-failed, and performs a dynamic no-HTTP `flow-client-lazy-lookup` probe for `Flow.js`,
- in `dev-ipc`, requests the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` Vaadin component frontend module plus focused Vaadin dev-bundle TextField, Checkbox and Select entries through IPC, stages real artifact bodies as Blob-backed module URLs, executes BigDecimalField with manifest `importRewrites`, resolves Lit CSS through a focused extracted helper, executes the TextField/Checkbox/Select entries, records every stage as `resourceStage`, resolves the focused TextField production `indexhtml` side-effect blocker with stable resource-path module identity, requests and injects one theme/static classpath CSS resource, verifies one repository-local Vaadin generated-flow-imports fixture resource, executes one real Vaadin dev-bundle `generated-flow-imports-*.js` chunk through the staged generated/lazy path after rewriting its markdown helper relative import to a stable staged dependency, records v1.9 `Flow.loadOnDemand` API/registry/mapping diagnostics and the precise `flow-ipc-loadondemand-v1_9` blocker, executes one Flow `LoadMode.LAZY` dependency through the visible DependencyLoader and IPC resolver, verifies the Vaadin dev-bundle `config/stats.json` production graph resource, stages the stats-selected production `indexhtml` artifact as an execution attempt, verifies `package-lock.json` dev-mode package graph resource, captures real UIDL generated `Flow.loadOnDemand(chunkId)` requests and maps/stages them to the generated-flow-imports artifact with `flow-ipc-loadondemand-chunk-v1_11`, parses the real generated-flow-imports body for v1.12 and records the current no-op table as `chunk-file-not-generated`, records v1.13 production bundle runtime owner metadata and owner-scoped staging for the stats-selected `indexhtml` artifact, performs a v1.14 controlled production owner import attempt with dependency rewrite / Vaadin global / registry / browser diagnostics and current blocker `production-owner-relative-import-unresolved`, and records blockers for dev-mode/Vite execution, production owner import success and full generated chunk-file execution,
- in `dev-ipc`, records Flow IPC push ordering diagnostics, including delivered push order, duplicate/late ignored order and ordering-recovery resync reason,
- in `dev-ipc`, handles `VFIPC1 command` for `window.setTitle`, `clipboard.writeText`, trace-only `notification.show`, `fileDialog.openFile` through AppKit `NSOpenPanel` and bounded `fileTransfer.download` / `fileTransfer.selectUploadFile` smoke paths, then emits `capabilityResult`.

Current compatibility boundary:

- In `dev` and `dev-swift`, Flow bootstrap, client events, UIDL/patch responses and push still run through the embedded Spring Boot localhost server.
- Desktop runtime commands can cross Java -> native host as Host Protocol v0 trace envelopes, but synchronous native return values are still scaffolded.
- The WebView loads a local HTTP URL today. This is explicitly not the final Flow over IPC transport.
- `dev-ipc` does not load a local HTTP URL for the smoke app and uses `VFIPC1` for desktop commands. The WebView bridge sends click events, native input/checkbox changes, `BigDecimalField`, real `TextField` and `Checkbox` Web Component changes and ordering-recovery resync from the visible Flow client, Java includes real `UidlWriter`/`UidlRequestHandler` output in IPC payloads, two server-initiated push UIDL updates are queued until the Flow client engine is ready and then delivered into it, one controlled duplicate/late probe is ignored with diagnostics, and the advertised Flow client resources are loaded from the Java sidecar classpath through IPC. `FlowBootstrap.js` / `FlowClient.js` are imported as modules and initialized with an IPC app config; `Flow.js`, `index.js`, `compile.properties` and the GWT client bundle are currently verified-only / intentionally-not-executed. The real `vaadin-big-decimal-field.js` classpath frontend module is imported with manifest-driven rewrites; `lit` resolves through a focused extracted Lit CSS helper and `@vaadin/component-base/src/define.js` resolves through an extracted Vaadin dev-bundle define helper. v1.5 adds a host-visible staging resolver: staged Blob module URLs are traced with source artifact metadata, cache status, cleanup policy and execution status. v1.6 makes the TextField blocker precise, and v1.7 resolves that duplicate custom element layer by reusing stable module identity for the TextField entry and production `indexhtml` dependency. v1.8 applies the same stable identity model to generated/lazy: `generated-flow-imports-*.js` rewrites `./markdown-helpers-*.js` to a staged helper URL and both artifacts execute. v1.9 records `Flow.loadOnDemand` API owner/install source, requested chunk, registry/mapping source and resolved artifact path; that path remains a diagnostic bridge after direct staged import. v1.10 executes a separate Flow `LoadMode.LAZY` dependency requested by the visible DependencyLoader, and records a stats-selected production runtime staged attempt with `flow-ipc-production-runtime-v1_10`. v1.11 observes real UIDL `Flow.loadOnDemand(chunkId)` from the visible Flow client and records the remaining chunk-file mapping blocker as `flow-ipc-loadondemand-chunk-v1_11`. v1.12 parses the real generated-flow-imports body and proves the current build has no `./chunks/chunk-*.js` branch, recording `flow-ipc-loadondemand-chunk-file-v1_12` with `chunkFileStatus=not-generated`. v1.13 adds an explicit production bundle runtime owner for the stats-selected `indexhtml` artifact, records owner module identity, cache/cleanup policy, registry snapshots and `componentProductionOwnerReused=false`, and keeps owner stage evidence distinct from component imports. v1.14 makes that owner perform a real controlled import attempt, records `productionBundleRuntimeOwnerImportVersion=flow-ipc-production-owner-import-v1_14`, dependency rewrite diagnostics, Vaadin global snapshots, registry snapshots, browser error/name and a current `production-owner-relative-import-unresolved` blocker. Focused Vaadin dev-bundle TextField/Checkbox/Select entries import real production chunk classes through their staged component path and are not counted as production owner success. The registered Flow client now owns the visible bootstrap, click/change-event patch, push and recovery resync path through IPC-backed bridge methods.

Verification note: on some managed macOS desktops, global `screencapture` or coordinate clicking can hit a `loginwindow`/MDM overlay instead of normal app windows. In that case, use the WindowServer entry for `Vaadin Desktop Host`, `HOST_PROTOCOL webview.dom`, the `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1` / `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` WebView smoke hooks, `VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1` for unattended native file dialog cancellation, `VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1` for unattended shutdown evidence and `window-snapshot.png` as the reliable verification artifacts. Add `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1` before the command when a verification run should open without stealing keyboard focus; the Swift host will skip `NSRunningApplication.activate(...)` and trace `window.activation` with `mode=no-activate`.

## macOS Fallback Host

`SystemWebViewDesktopHost` is the default host on macOS. It:

- receives the local Vaadin URL,
- creates a temporary `.app` bundle,
- compiles the bundled Swift host with `/usr/bin/swiftc`,
- opens a native WKWebView window,
- logs bootstrap and host-exit trace events,
- terminates the Java app when the window process exits by default.

This is a real desktop window, but it still uses **compatibility HTTP transport**. It is not final Flow over IPC.

`CompatibilityDesktopHost` is still available as a fallback. It:

- receives the local Vaadin URL,
- logs bootstrap trace,
- optionally opens the default browser,
- marks the app as compatibility mode.

Select a host with:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

`CompatibilityNativeBridge` handles desktop API calls by logging trace and returning deterministic results. Clipboard is backed by in-memory state.

`SystemWebViewDesktopHost` remains useful as a small macOS fallback, but it is no longer the long-term mainline.

## Capability Permission Model

`dev-ipc` now reads the permission mode from `desktop-capabilities.json` when a manifest
is present and records a `capabilityManifest` trace event. Supported modes are:

- `allow-all-dev`: development default; commands are allowed when no manifest is present,
  or when a loaded manifest lists the command.
- `manifest-enforced`: a loaded manifest is required, and commands must be listed for
  the window.
- `trace-only-dry-run`: commands execute, but permission metadata records whether a
  loaded manifest would have allowed or denied the command.

The current host allows the known v1 commands for the single main window:

- `window.setTitle`
- `clipboard.writeText`
- `fileDialog.openFile`
- `fileTransfer.download`
- `fileTransfer.selectUploadFile`
- `notification.show`

Every `capabilityResult` includes `permissionMode`, `permissionDecision`, `permissionReason` and `permissionScope`. The Flow IPC smoke app uses `permissionMode=manifest-enforced`; known commands must be listed for the `main` window and successful calls are marked `permissionDecision=allowed` with `permissionReason=manifest-allow`. The smoke view also sends an intentionally undeclared `diagnostics.deniedCapability` command, which is rejected with `permissionDecision=denied` and `permissionReason=manifest-denied`. Results include `permissionManifestLoaded`, `permissionManifestPath` and `permissionManifestCommands`. `fileDialog.openFile` now returns AppKit panel result metadata (`canceled`, `paths`, `allowsMultipleSelection`, `securityScoped=false`, `autoCanceled`) but does not yet create security-scoped bookmarks. `fileTransfer.download` and `fileTransfer.selectUploadFile` are v1.1 smoke boundaries: they support controlled test paths, byte-count metadata and redacted trace bodies, but they are not production file streaming or Vaadin Upload/Download parity. This is intentionally still a development permission system. The production direction is a stricter manifest-gated model with per-window, per-origin, user-gesture and per-capability grants.

## Next Native Host Work

The real host still needs to:

- replace the controlled push ordering/resync recovery smoke with a durable queue, ack and reconnect model,
- replace the remaining generated chunk-file, production owner import/side-effect success and dev-mode/Vite blockers with broader real frontend execution,
- replace the bounded controlled-path file-transfer smoke with streaming upload/download, native progress/cancel semantics and sandbox-safe file grants,
- replace the development manifest modes with production capability grants, user-gesture checks and security-scoped file permissions.

## Why Not Call This Final

Embedding a localhost URL in WKWebView is compatibility mode. It is useful for proving framework ergonomics, real Flow app structure and desktop host lifecycle, but it is not Flow over IPC.

The `dev-ipc` path is the first real no-HTTP IPC slice, but it is not final either: it proves host/WebView/Java message flow, visible Flow client ownership for bootstrap, click-event patches, native input/checkbox change-event patches, real Vaadin Web Component event patches (`BigDecimalField`, `TextField`, `Checkbox`, plus framework-app `Select` coverage), ordered server pushes, duplicate/late ignored diagnostics, ordering-recovery resync, IPC-backed Flow client XHR/event/resync dispatch, listener-driven patches, server-side UIDL generation, Flow client module initialization, bootstrap UIDL binding, v1.5 frontend staging diagnostics, v1.7 TextField side-effect/module-identity execution, v1.8 generated/lazy relative dependency rewrite and staged execution, v1.9 loadOnDemand contract/blocker evidence, v1.10 Flow lazy dependency execution and production runtime staged-attempt evidence, v1.11 generated `Flow.loadOnDemand(chunkId)` mapping/staging evidence, v1.12 generated-flow-imports no-op table / `chunk-file-not-generated` evidence, v1.13 production bundle runtime owner contract/staging evidence, v1.14 production owner import attempt / `production-owner-relative-import-unresolved` evidence, resource execution classification/lazy lookup diagnostics, and a bounded controlled-path file-transfer capability boundary. The repository-local UIDL applier remains as a diagnostic fallback, while broader component coverage, generated chunk-file execution, production owner import/side-effect success, dev-mode/Vite execution, full upload/download streaming and durable push/reconnect semantics remain future work.
