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

## Current IPC Slice

```text
Vaadin Flow View (@Route, Button, Span, Layout, BigDecimalField, TextField, Checkbox, Select)
  -> FlowIpcLauncher / FlowIpcRuntime
  -> VFIPC1 stdin/stdout envelopes
  -> vaadin-desktop-host-swift
  -> WKWebView local HTML shell
```

This path is started with:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

The same `dev-ipc` command can also launch `test-apps/flow-over-ipc-framework-app`, a less smoke-specific validation app written with ordinary Flow Java APIs for Button, `BigDecimalField`, `Checkbox`, `Select`, repeated-list text updates and desktop capability calls. The strict v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8 fixtures remain on the smoke app, while the framework app has a dedicated `--require-flow-ipc-v1_3-framework` host-log gate to check first meaningful paint and prove that the shell and Flow client bridge are not shaped only around smoke DOM ids.

It does not use localhost HTTP for bootstrap/push/event/patch/resync in the smoke app. Bootstrap advertises real Vaadin `flow-client` classpath resources and a v1.4 frontend artifact manifest. The WebView requests, verifies and classifies the advertised resource graph through IPC while echoing artifact family, module specifier, dependency, execution policy, resolver and status metadata in `resourceRequest`; it imports and initializes `FlowBootstrap.js` / `FlowClient.js` as the first local-resource bridge. The same resource channel now records a dynamic `flow-client-lazy-lookup` probe for `Flow.js`, verifies the static `compile.properties` lookup, and classifies resources that are intentionally not executed by this smoke slice. The component bootstrap also advertises `componentFrontendResources` for `com.vaadin.flow.component.textfield.BigDecimalField`, `com.vaadin.flow.component.textfield.TextField`, `com.vaadin.flow.component.checkbox.Checkbox` and `com.vaadin.flow.component.select.Select`; the WebView requests real artifact bodies, stages them as Blob module URLs, executes the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` classpath module using manifest `importRewrites`, resolves BigDecimalField `lit` through a focused extracted Lit CSS helper, resolves `@vaadin/component-base/src/define.js` through an extracted Vaadin dev-bundle define helper, imports focused Vaadin dev-bundle TextField/Checkbox/Select entries backed by the production `indexhtml` chunk, injects one real theme/static classpath CSS resource, verifies a repository-local Vaadin generated-flow-imports fixture as a generated/lazy artifact, executes a real Vaadin dev-bundle `generated-flow-imports-*.js` generated/lazy chunk after rewriting its markdown helper relative dependency to a stable staged module URL, verifies Vaadin dev-bundle `config/stats.json` as a production-bundle graph artifact, and verifies Vaadin dev-bundle `package-lock.json` as a dev-mode package graph artifact. v1.5 records every staged artifact as `resourceStage`, stages the focused TextField production class entry as blocker evidence, and attempts a real Vaadin dev-bundle `generated-flow-imports-*.js` generated/lazy chunk with explicit `Flow.loadOnDemand` and browser-error diagnostics. v1.6 narrows the TextField blocker to a concrete production `indexhtml` side-effect path; v1.7 resolves that duplicate custom element layer by assigning stable resource-path module identity to the TextField entry and production `indexhtml` dependency, then verifies a real Flow `TextField` event path and visible DOM state; v1.8 resolves the generated/lazy markdown helper relative import and records `frontendArtifactGeneratedLazyExecuted=true`, while full Flow-client-triggered `loadOnDemand` remains a boundary. The legacy TextField shim, partial production bundle execution, dev-mode/Vite execution and Flow-client-triggered generated/lazy execution are explicit fallback/partial/blocker classifications rather than hidden production bundle/dev-mode parity. Once the Flow client engine binds the bootstrap UIDL nodes, its container becomes the visible WebView surface; the repository-local rendered tree remains only as a diagnostic fallback for non-engine or failed-delivery paths.

The Java side emits real server-side Vaadin UIDL under `payload.uidl`: bootstrap and ordered server-initiated pushes come from `UidlWriter`, while click/change event and ordering-recovery resync patches come from `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, Button/native input/checkbox/BigDecimalField/TextField/Checkbox event patches, ordered pushes and the recovery resync are now consumed by the initialized visible Flow client engine through IPC-backed bridge methods. The v1.1 smoke also emits a controlled duplicate/late push probe; the WebView ignores it, records delivered/ignored order diagnostics and requests resync.

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
- `vaadin-desktop-flow`: compatibility adapter plus the first `FlowIpcLauncher`/`FlowIpcRuntime` and UIDL writer slice.
- `vaadin-desktop-ipc`: Host Protocol v0 envelope plus Flow IPC Protocol v1 envelope.
- `vaadin-desktop-host`: Java host abstractions, macOS WKWebView fallback and native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host spike that owns sidecar process lifecycle and system WebView.
- `vaadin-desktop-devtools`: Flow IPC trace inspector and diagnostics.
- `vaadin-desktop-cli`: developer command entry.
- `flow-transport-adapter-spike`: code-level analysis and draft adapter shape for real Flow UIDL/event boundaries.

## Relationship to Tauri and Electron

Like Tauri, the intended shape is a small native host plus system WebView plus backend runtime. Unlike Electron, the framework does not bundle Chromium. The macOS native implementation is now Swift/AppKit/WKWebView so the host can directly own platform behavior while the main UI programming model remains Vaadin Flow Java views.

## Relationship to Vaadin Flow

Framework v0 uses real Vaadin Flow through `vaadin-spring-boot-starter`. Developers write standard Flow views with `@Route`, `Button`, `Grid`, `Span`, layouts and listeners.

The current gap is transport completeness: `dev-ipc` removes HTTP for one constrained smoke app, has the visible Flow client engine emit click/change and recovery resync RPC payloads through an IPC-backed bridge, routes those messages through the server-side Flow UIDL RPC path, verifies and classifies the advertised Flow client resource graph over IPC, imports/initializes real Flow bootstrap/GWT client resources, publishes frontend artifact metadata for all current resource families, stages real artifact bodies as Blob module URLs, executes the BigDecimalField classpath frontend module with manifest-driven import rewrites, executes focused Vaadin dev-bundle entries for real TextField, Checkbox and Select definitions with stable module identity for the TextField production side-effect path, resolves Lit CSS through a focused extracted helper, keeps the legacy TextField shim only as fallback diagnostics, verifies one theme/static classpath resource, verifies one generated-flow-imports fixture resource, executes one real Vaadin dev-bundle `generated-flow-imports-*.js` chunk as a generated/lazy staged import after resolving its markdown helper dependency, verifies one production-bundle `stats.json` graph resource, verifies one dev-mode `package-lock.json` package graph resource, records remaining lazy and bundle execution blockers, feeds ordered server pushes back into the visible Flow client engine and records duplicate/late recovery diagnostics. A second framework validation app now checks a less smoke-specific project shape, but full Flow still needs broader event coverage, full theme parity, Flow-client-triggered generated/lazy execution, production/dev resource execution, durable push queue, reconnect and production resource semantics.
