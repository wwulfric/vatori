# Flow Transport Adapter

`vaadin-desktop-flow` now contains three integration paths:

- `DesktopLauncher`: compatibility HTTP/Spring Boot launcher.
- `FlowIpcLauncher`: minimum Flow-over-IPC launcher.
- `transport.UidlFlowTransportAdapter`: first compiled wrapper around Vaadin's real UIDL request path.

## Current Minimum Adapter

Classes:

- `dev.vatori.desktop.flow.ipc.FlowIpcLauncher`
- `dev.vatori.desktop.flow.ipc.FlowIpcRuntime`
- `dev.vatori.desktop.flow.ipc.FlowIpcRenderer`
- `dev.vatori.desktop.flow.ipc.FlowClientResources`

The current adapter:

1. Sets up `DesktopContext` with a host-protocol native bridge.
2. Instantiates a `@Route` Vaadin Flow component class directly.
3. Renders a small component-tree payload from real Flow components.
4. Maps component ids to server-side component instances.
5. Accepts WebView-provided Flow UIDL RPC bodies for click/change events, including the `BigDecimalField` and `Checkbox` Web Component `change` events, and recovery resync, then dispatches them through `UidlRequestHandler`.
6. Returns a replace-root patch after Java listener execution, with the server-side UIDL response attached.
7. Publishes a v1.4 manifest of real Vaadin `flow-client` classpath resources, serves the full advertised resource graph through `resourceRequest` / `resourceResponse`, initializes `FlowBootstrap.js` / `FlowClient.js`, classifies executed vs verified-only resources and records one `Flow.js` lazy lookup diagnostic.
8. Publishes a v1.4+ frontend artifact manifest for `BigDecimalField`, `TextField`, `Checkbox` and `Select`, covering flow-client classpath, component classpath frontend, npm/frontend, theme/static, production bundle, dev-mode bundle, generated/lazy and flow-lazy-dependency artifact families. It serves the real `vaadin-big-decimal-field.js` classpath module, manifest-declared BigDecimalField import rewrites, a focused extracted Lit CSS helper, an extracted Vaadin dev-bundle define helper, a focused Vaadin dev-bundle TextField entry, focused Vaadin dev-bundle Checkbox/Select entries backed by the production `indexhtml` chunk, one theme/static classpath CSS resource, one repository-local generated Flow imports fixture resource, one real Vaadin dev-bundle `generated-flow-imports-*.js` chunk plus markdown helper dependency, one v1.12 parser for the real generated-flow-imports no-op table, one real Flow `LoadMode.LAZY` JavaScript dependency, one Vaadin dev-bundle `config/stats.json` production graph resource, one stats-selected production runtime artifact, one v1.13 production bundle runtime owner contract, one v1.14 production owner import contract, one v1.15 production owner dependency resolver contract and one Vaadin dev-bundle `package-lock.json` package graph resource, and records blocked/failure classification for the legacy TextField shim, production owner import/side-effect success, dev-mode/Vite execution and full generated `Flow.loadOnDemand` chunk-file execution.
9. Adds a v1.5 staging/execution boundary in the host/WebView layer: real artifact bodies are staged as Blob module URLs, `resourceStage` connects resource metadata to execution status, BigDecimalField plus focused TextField/Checkbox/Select entries execute through staged artifacts, and the real Vaadin dev-bundle `generated-flow-imports-*.js` chunk moves from verified-only to a generated/lazy staged execution attempt with `Flow.loadOnDemand` boundary diagnostics.
10. Adds v1.6 TextField blocker reduction metadata and v1.7 side-effect/module-identity execution gates: v1.6 narrows the blocker to duplicate `vaadin-text-field` custom element registration after BigDecimalField has already defined the tag, and v1.7 resolves that layer by reusing stable module identity for the TextField entry and production `indexhtml` dependency.
11. Adds v1.8 generated/lazy relative dependency resolution: the host rewrites `generated-flow-imports-*.js` relative markdown helper imports to stable staged helper module URLs, executes the real generated-flow-imports chunk plus helper, and records `flow-ipc-generated-lazy-resolver-v1_8` / `frontendArtifactGeneratedLazyExecuted=true`.
12. Adds v1.9 `Flow.loadOnDemand` contract evidence: the trace records API owner/install source, requested chunk, registry/mapping source, resolved artifact path and the precise `flow-ipc-loadondemand-v1_9` blocker, while keeping real generated `Flow.loadOnDemand` chunk parity as the next adapter boundary.
13. Adds v1.10 Flow lazy dependency and production runtime registry evidence: an ordinary Flow `LoadMode.LAZY` dependency is requested by the visible DependencyLoader, resolved over IPC, staged and executed; the production bundle stats graph selects the production `indexhtml` artifact and records a staged `execution-attempt` with `flow-ipc-production-runtime-v1_10`.
14. Adds v1.11 generated `Flow.loadOnDemand(chunkId)` evidence: the host captures real bootstrap UIDL lazy expressions, observes the visible Flow client trigger, maps the requested chunk to the real generated-flow-imports artifact, stages/imports it over IPC and records the remaining `flow-ipc-loadondemand-chunk-v1_11` chunk-file blocker.
15. Adds v1.12 generated chunk-file registry evidence: the runtime parses the real generated-flow-imports body, observes that the current Vaadin output is a `Promise.resolve(0)` no-op table with no `./chunks/chunk-*.js` branch, and records `flow-ipc-loadondemand-chunk-file-v1_12` / `chunk-file-not-generated` instead of treating the missing chunk file as success.
16. Adds v1.13 production bundle runtime owner evidence: Java manifest/response metadata declares `flow-ipc-production-owner-v1_13`, owner identity, stats source, cache/cleanup policy and component reuse status; the Swift host stages the real stats-selected production `indexhtml` artifact under `owner=production-bundle-runtime` and records `production-owner-runtime-contract-missing` rather than counting focused component imports as owner success.
17. Adds v1.14 production owner import evidence: Java manifest/response metadata declares `flow-ipc-production-owner-import-v1_14`, import policy, expected side effects, acceptable failures and rewrite strategy; the Swift host performs a controlled owner import attempt for the stats-selected `indexhtml` artifact, records dependency rewrite / Vaadin global / registry / browser diagnostics and narrows the current blocker to `production-owner-relative-import-unresolved`.
18. Adds v1.15 production owner dependency resolver evidence: Java manifest/response metadata declares `flow-ipc-production-owner-deps-v1_15`, resolver root/policy, expected artifacts, acceptable failures and import phase diagnostics; the Swift host resolves and stages existing real owner relative dependencies for the stats-selected `indexhtml` artifact, records graph classifications and narrows the current owner blocker to `production-owner-missing-real-artifact`.
19. Emits ordered server push UIDL updates with `UidlWriter`, records duplicate/late probe handling and drives a controlled recovery resync through the same Flow RPC path.

This proves the immediate boundary:

```text
WebView event
  -> Swift host bridge
  -> Java sidecar stdin
  -> real Vaadin Button listener
  -> server-side component state mutation
  -> patch envelope
  -> WebView apply
```

## Relationship To Vaadin Internals

The current `FlowIpcRuntime` adapter intentionally does not claim to be the final Vaadin UIDL adapter. It now creates a desktop-shaped Flow session/UI, includes real `UidlWriter` output in `bootstrapResponse` and ordered server-initiated `push` envelopes, lets the visible Flow client engine send UIDL RPC bodies for click/change events and ordering-recovery resync over an IPC-backed bridge, and uses `UidlRequestHandler`/`ServerRpcHandler` for event/resync patches. The WebView promotes the initialized Flow client container to the visible surface, feeds bootstrap/event/push/recovery resync UIDL into that client, ignores a controlled duplicate/late push probe and records delivered/ignored order diagnostics. The repository-local UIDL applier remains as a diagnostic fallback for non-engine or failed-delivery paths. `FlowClientResources` reads the real `flow-client` jar resources from the Java sidecar classpath, augments `FlowClient.js` with the narrow IPC bridge methods used by this slice, serves the full advertised resource graph over IPC, imports `FlowBootstrap.js` / `FlowClient.js` so the host can initialize the Flow bootstrap and GWT client path without localhost HTTP, classifies the remaining resources as verified-only / intentionally-not-executed and records one dynamic `Flow.js` lazy lookup. It also exposes frontend artifact metadata for `com.vaadin.flow.component.textfield.BigDecimalField`, `com.vaadin.flow.component.textfield.TextField`, `com.vaadin.flow.component.checkbox.Checkbox` and `com.vaadin.flow.component.select.Select`; the host requests the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` module, stages it as a Blob-backed module URL, executes it using manifest-driven import rewrites, resolves BigDecimalField's `lit` import through a focused extracted Lit CSS helper, resolves `@vaadin/component-base/src/define.js` through an extracted Vaadin dev-bundle define helper, executes the focused TextField production class entry with stable module identity for its production `indexhtml` dependency, requests focused Vaadin dev-bundle Checkbox/Select entries that import real classes from the production `indexhtml` chunk, requests/injects one real `theme-static` classpath CSS resource, verifies a repository-local Vaadin generated-flow-imports fixture as a generated/lazy artifact, executes a real Vaadin dev-bundle `generated-flow-imports-*.js` chunk as a staged generated/lazy import after rewriting its markdown helper relative dependency, records v1.9 `Flow.loadOnDemand` API/registry/mapping diagnostics and the `flow-ipc-loadondemand-v1_9` blocker, resolves and executes one ordinary Flow `LoadMode.LAZY` dependency through the visible DependencyLoader and IPC resolver, verifies Vaadin dev-bundle `config/stats.json` as a production-bundle graph artifact, stages the stats-selected production `indexhtml` artifact as a production runtime execution attempt, records a v1.13 production bundle runtime owner contract around that artifact with owner-scoped staging and `production-owner-runtime-contract-missing`, performs a v1.14 controlled owner import attempt for the same artifact and records `production-owner-relative-import-unresolved`, adds v1.15 owner dependency resolver metadata and trace evidence for existing real relative dependencies with `production-owner-missing-real-artifact`, verifies Vaadin dev-bundle `package-lock.json` as a dev-mode package graph artifact, records v1.11 real UIDL `Flow.loadOnDemand(chunkId)` chunk request/mapping/staging evidence with `flow-ipc-loadondemand-chunk-v1_11`, and records v1.12 generated-flow-imports parser evidence with `chunkFileStatus=not-generated` for the current no-op table. The legacy TextField shim path, production owner import/side-effect success, dev-mode/Vite execution and full generated chunk-file execution remain explicit fallback, partial or blocker entries until production bundle/dev-mode/theme handling is complete. The repository also contains the compiled transport adapter around the real Flow server path:

- `dev.vatori.desktop.flow.transport.FlowTransportAdapter`
- `dev.vatori.desktop.flow.transport.UidlFlowTransportAdapter`
- `dev.vatori.desktop.flow.transport.DesktopVaadinRequest`
- `dev.vatori.desktop.flow.transport.DesktopVaadinResponse`
- `dev.vatori.desktop.flow.transport.DesktopWrappedSession`
- `dev.vatori.desktop.flow.transport.DesktopFlowRuntimeSession`

The executable smoke harnesses live under `vaadin-desktop-flow/src/test/java` and keep the same package names so they can verify package-private transport/session boundaries without being packaged into the production runtime artifact:

- `dev.vatori.desktop.flow.transport.UidlAdapterSmokeMain`
- `dev.vatori.desktop.flow.transport.UidlRpcSmokeMain`

`UidlFlowTransportAdapter` wraps:

- `UidlRequestHandler`
- `ServerRpcHandler`
- `UidlWriter`
- `VaadinSession`
- `UI`

The adapter accepts `(UI, FlowClientMessage)` and returns `FlowPatchResponse`. Internally it creates a desktop `VaadinRequest` with UIDL request parameters, captures a desktop `VaadinResponse`, sets Flow current instances, runs `UidlRequestHandler.synchronizedHandleRequest(...)`, then exposes the UIDL JSON body.

This is still an adapter slice. It depends on Vaadin's current internal API shape and still needs broader Flow-client event coverage, full theme parity beyond the current static CSS lookup, production bundle/dev-mode resource execution, durable push queue, ack, reconnect and resync semantics. The current adapter now performs a minimum Flow IPC `csrfToken`/security key, `syncId` and `clientId` preflight before `UidlRequestHandler` dispatch.

`UidlAdapterSmokeMain` now constructs a minimal in-process `VaadinService`, `VaadinSession`, `WrappedSession` and `UI`, attaches a real Flow component tree, calls `UidlWriter` through `UidlFlowTransportAdapter`, mirrors Flow bootstrap's `Vaadin-Security-Key`, and asserts that the output contains Flow UIDL `changes`, `syncId` and the security key.

Run it with:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

These commands do not open a WebView and do not run a servlet container. They are server-side adapter verification steps that prove the repository can drive Vaadin's real UIDL writer and event RPC path from a desktop-shaped session/request boundary. The same session helper is now used by `dev-ipc` to emit UIDL markers in real runtime traces.

`uidl-rpc-smoke` goes one step deeper: it sends a real Flow client event RPC body shaped like Vaadin's own `ServerRpcHandlerTest` examples, lets `UidlRequestHandler` locate the desktop `UI`, runs `ServerRpcHandler`/`EventRpcHandler`, triggers a Button click listener and verifies that the UIDL response contains `Clicks: 1` plus `syncId`. It also verifies that invalid Flow IPC `csrfToken`, invalid security key, stale `syncId` and out-of-order `clientId` values are rejected before dispatch. `flow-ipc-scope-smoke` verifies that event envelopes with mismatched `sessionId` or `uiId` are rejected before event handling. `flow-client-resource-smoke` verifies that `FlowBootstrap.js` and `FlowClient.js` are present on the runtime classpath and that `FlowClient.js` carries the IPC message/resync bridge methods. It also checks the v1.4 manifest metadata for artifact families, resolver/status fields, the available theme/static resource, the generated-flow-imports fixture resource, the real Vaadin dev-bundle generated-flow-imports chunk and markdown helper dependency, the production-bundle `stats.json` graph resource, the dev-mode `package-lock.json` package graph resource, the v1.7 TextField graph side-effect metadata, the v1.8 generated/lazy resolver metadata, the v1.9 loadOnDemand contract/blocker metadata, the v1.10 lazy dependency plus production runtime registry metadata, the v1.11 generated loadOnDemand chunk contract metadata, the v1.12 generated-flow-imports no-op chunk-file registry metadata, the v1.13 production bundle runtime owner metadata, the v1.14 production owner import contract metadata and the v1.15 production owner dependency resolver metadata. `dev-ipc` now imports the real Flow client engine, promotes it to the visible renderer, bridges its `XMLHttpRequest` transport over IPC, classifies resource execution diagnostics and uses the same server-side RPC path for Flow client generated click, change and recovery resync dispatch.

The `jakarta.servlet-api` dependency is present on the runtime classpath because Vaadin server classes such as `VaadinSession` and the `VaadinRequest`/`VaadinResponse` signatures still reference servlet API types. This does not start a servlet container and is not used as the `dev-ipc` transport.

The existing spike in `flow-transport-adapter-spike/README.md` remains the code-level map for that work.

## Why This Is Still Useful

The minimum adapter removes localhost HTTP from the demo event/patch loop. That lets the project validate the native host and IPC responsibilities before solving every Flow internal dependency.

It also keeps the developer-facing shape intact: the smoke app still uses ordinary Vaadin Flow Java components and `@Route`.

## Next Adapter Step

The next concrete step is to turn one remaining blocker into an executable frontend path: continue from v1.15's `production-owner-missing-real-artifact` evidence into production owner import ordering/component reuse, make the smoke/framework build produce a real generated lazy chunk file or connect a supported Vaadin lazy chunk SPI beyond v1.12's no-op table proof, execute dev-mode/Vite resources beyond the verified package graph, or broaden component coverage. In parallel, the controlled duplicate/late push-ordering recovery smoke still needs a real Flow push queue, ack model and reconnect/resync behavior.
