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

The same `dev-ipc` command can also launch `test-apps/flow-over-ipc-framework-app`, a less smoke-specific validation app written with ordinary Flow Java APIs for Button, `BigDecimalField`, `Checkbox`, `Select`, a v1.10 lazy dependency declaration, repeated-list text updates and desktop capability calls. The strict v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9/v1.10/v1.11/v1.12/v1.13/v1.14/v1.15/v1.16/v1.17/v1.18/v1.19/v1.20/v1.21/v1.22/v1.23/v1.24 fixtures remain on the smoke app, while the framework app has a dedicated `--require-flow-ipc-v1_3-framework` host-log gate to check first meaningful paint and prove that the shell and Flow client bridge are not shaped only around smoke DOM ids.

It does not use localhost HTTP for bootstrap/push/event/patch/resync in the smoke app. Bootstrap advertises real Vaadin `flow-client` classpath resources and a v1.4 frontend artifact manifest. The WebView requests, verifies and classifies the advertised resource graph through IPC while echoing artifact family, module specifier, dependency, execution policy, resolver and status metadata in `resourceRequest`; it imports and initializes `FlowBootstrap.js` / `FlowClient.js` as the first local-resource bridge. The same resource channel now records a dynamic `flow-client-lazy-lookup` probe for `Flow.js`, verifies the static `compile.properties` lookup, and classifies resources that are intentionally not executed by this smoke slice. The component bootstrap also advertises `componentFrontendResources` for `com.vaadin.flow.component.textfield.BigDecimalField`, `com.vaadin.flow.component.textfield.TextField`, `com.vaadin.flow.component.checkbox.Checkbox` and `com.vaadin.flow.component.select.Select`; the WebView requests real artifact bodies, stages them as Blob module URLs, executes the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` classpath module using manifest `importRewrites`, resolves BigDecimalField `lit` through a focused extracted Lit CSS helper, resolves `@vaadin/component-base/src/define.js` through an extracted Vaadin dev-bundle define helper, imports focused Vaadin dev-bundle TextField/Checkbox/Select entries backed by the production `indexhtml` chunk, injects one real theme/static classpath CSS resource, verifies a repository-local Vaadin generated-flow-imports fixture as a generated/lazy artifact, executes a real Vaadin dev-bundle `generated-flow-imports-*.js` generated/lazy chunk after rewriting its markdown helper relative dependency to a stable staged module URL, records v1.9 `Flow.loadOnDemand` API/registry/mapping diagnostics and the `flow-ipc-loadondemand-v1_9` blocker, executes one ordinary Flow `LoadMode.LAZY` dependency through the visible DependencyLoader and IPC resolver, verifies Vaadin dev-bundle `config/stats.json` as a production-bundle graph artifact, stages the stats-selected production `indexhtml` artifact as a production runtime execution attempt, verifies Vaadin dev-bundle `package-lock.json` as a dev-mode package graph artifact, records v1.11 real UIDL `Flow.loadOnDemand(chunkId)` expression / Flow-client-triggered request / generated-flow-imports mapping / staging evidence with `flow-ipc-loadondemand-chunk-v1_11`, records v1.12 real generated-flow-imports parser evidence that the current build has a no-op `Promise.resolve(0)` table rather than a `./chunks/chunk-*.js` branch, records v1.13 production bundle runtime owner metadata, owner-scoped stage identity, cache/cleanup policy, registry snapshots and the precise `production-owner-runtime-contract-missing` blocker for the stats-selected `indexhtml` artifact, records v1.14 production owner import attempt metadata with dependency rewrite diagnostics, Vaadin global snapshots, registry snapshots, browser error diagnostics and current `production-owner-relative-import-unresolved` blocker, records v1.15 production owner dependency resolver metadata with owner dependency graph counts, phase candidates, resolved/missing classifications and current `production-owner-missing-real-artifact` blocker, records v1.16 production owner ordering / component reuse metadata with `before-component-imports` preflight, after-component fallback, observed phases and current duplicate custom element blocker, records v1.17 host-owned resource loader metadata plus `vatori-resource://` execution evidence for the Flow lazy dependency, records v1.18 production owner resource-loader metadata plus root/dependency `vatori-resource://flow-ipc/production-owner/...` request/response/import evidence, records v1.19 production owner side-effect metadata, expected tag/global diagnostics, module-import vs side-effect result separation and component reuse decision evidence, and records v1.20 owner-owned runtime reuse attempt metadata with `production-owner-owned-missing-real-artifact` plus `focused-fallback-after-owner-owned-runtime-attempt`. v1.5 records every staged artifact as `resourceStage`, stages the focused TextField production class entry as blocker evidence, and attempts a real Vaadin dev-bundle `generated-flow-imports-*.js` generated/lazy chunk with explicit `Flow.loadOnDemand` and browser-error diagnostics. v1.6 narrows the TextField blocker to a concrete production `indexhtml` side-effect path; v1.7 resolves that duplicate custom element layer by assigning stable resource-path module identity to the TextField entry and production `indexhtml` dependency, then verifies a real Flow `TextField` event path and visible DOM state; v1.8 resolves the generated/lazy markdown helper relative import and records `frontendArtifactGeneratedLazyExecuted=true`; v1.9 shows the real generated `Flow.loadOnDemand` API is still installed by diagnostic direct staged import rather than Flow client engine trigger; v1.10 proves a separate `LoadMode.LAZY` dependency can be Flow-client-triggered and resolved over IPC, while production runtime ownership remains a named blocker; v1.11 moves generated `Flow.loadOnDemand(chunkId)` past the diagnostic bridge; v1.12 moves the remaining generated blocker to `chunk-file-not-generated` based on real Vaadin output; v1.13 separates production owner evidence from focused component import success; v1.14 proves the owner import is attempted and narrows the blocker beyond v1.13's runtime contract layer; v1.15 resolves and stages existing real owner dependencies and moves the accepted blocker beyond generic relative import unresolved; v1.16 proves owner-first ordering/reuse decisions are visible but still ends at a duplicate custom element / component reuse contract blocker; v1.17 proves host-owned scheme/file-backed resource execution exists; v1.18 proves production owner root/dependency graph can use that host-owned custom-scheme layer, with root Blob fallback no longer accepted as v1.18 success; v1.19 proves that owner module import and reusable owner side effects are separately observable and that the component path is explicit fallback; v1.20 proves the owner-first runtime attempt happens before focused imports, but the current owner-owned runtime still falls back because a real owner artifact is missing. The legacy TextField shim, production owner import/side-effect success, dev-mode/Vite execution and real app/build generated chunk-file parity are explicit fallback/partial/blocker classifications rather than hidden production bundle/dev-mode parity. Once the Flow client engine binds the bootstrap UIDL nodes, its container becomes the visible WebView surface; the repository-local rendered tree remains only as a diagnostic fallback for non-engine or failed-delivery paths.

v1.21 adds the cross-family contract that the earlier resource slices were missing. Java manifest/resource response metadata now declares `frontendResolverContractVersion=flow-ipc-frontend-resolver-contract-v1_21`, resolver scope, stable source-path plus owner namespace identity, sha256 integrity, window-lifetime cache/cleanup, allowed schemes `blob,vatori-resource`, no-localhost proof and accepted gaps. Swift records a unified `webview.dom` resolver summary for flow-client, component frontend, theme/static, generated/lazy, Flow lazy dependency, production owner and dev-mode families, including verified-only resources, strict blockers and accepted fallbacks. This is architecture evidence for one resolver contract, not production owner success, real app/build generated chunk-file parity or dev-mode/Vite parity.

v1.22 adds a positive execution boundary for the generated chunk-file path without pretending the current app/build produced a real lazy chunk. Java advertises a Vaadin-shaped repository `generated-flow-imports-v1_22.js` fixture plus `chunks/chunk-v1_22.js`, with `flow-ipc-loadondemand-chunk-file-execution-v1_22`, `branch-table-dynamic-import`, chunk byte/sha metadata and side-effect proof. Swift requests both artifacts over IPC, stages them, rewrites the relative chunk import to the staged chunk URL, calls fixture `Flow.loadOnDemand(chunkId)` and records callback plus `window.VaadinDesktopGeneratedLazyChunkV122.executed`. This proves the resolver/executor can handle a real chunk table shape; current real Vaadin output still has the v1.12 no-op table blocker.

v1.23 adds the corresponding positive execution boundary for production owner runtime without pretending the current app/build production bundle is reusable yet. Java advertises a repository-local owner root/dependency fixture with `flow-ipc-production-owner-positive-runtime-v1_23`, expected tags/globals and side-effect proof. Swift serves the root and dependency through the production-owner `vatori-resource` namespace, rewrites the relative dependency import, imports the owner root and records `owner-reused-positive-fixture` when the expected side effect is visible. This proves the custom-scheme owner runtime path can close when an owner contract exists; the real production owner path still keeps its missing-real-artifact / focused fallback blocker.

v1.24 adds the closure decision boundary for the frontend/resource pipeline. Java manifest/resource response metadata and Swift request/response/stage/DOM diagnostics declare `frontendPipelineClosureVersion=flow-ipc-frontend-pipeline-closure-v1_24`, satisfied fixture proofs, real app/build blockers, `product-parity-pending` and the next product path `packaging-time-staged-frontend-or-supported-vaadin-build-spi`. This is architecture evidence that representative infrastructure paths have closed; it is not production bundle or dev-mode parity.

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

The current gap is transport completeness: `dev-ipc` removes HTTP for one constrained smoke app, has the visible Flow client engine emit click/change and recovery resync RPC payloads through an IPC-backed bridge, routes those messages through the server-side Flow UIDL RPC path, verifies and classifies the advertised Flow client resource graph over IPC, imports/initializes real Flow bootstrap/GWT client resources, publishes frontend artifact metadata for all current resource families, stages real artifact bodies as Blob module URLs, executes representative real component/generator/lazy resources, records production owner boundaries through v1.20, records a v1.21 unified frontend resolver contract, verifies v1.22 generated chunk-file and v1.23 production-owner positive fixtures, records a v1.24 frontend pipeline closure decision contract, and feeds ordered server pushes back into the visible Flow client engine with duplicate/late recovery diagnostics. A second framework validation app now checks a less smoke-specific project shape, but full Flow still needs broader event coverage, full theme parity, real app/build generated chunk-file parity, production owner import/side-effect success, dev-mode/Vite execution, durable push queue, reconnect and production resource semantics.
