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
8. Publishes a v1.4 frontend artifact manifest for `BigDecimalField`, `Checkbox` and `Select`, covering flow-client classpath, component classpath frontend, npm/frontend, theme/static, production bundle, dev-mode bundle and generated/lazy artifact families. It serves the real `vaadin-big-decimal-field.js` classpath module, manifest-declared BigDecimalField import rewrites, an extracted Vaadin dev-bundle define helper, a focused Vaadin dev-bundle TextField partial-blocker entry, focused Vaadin dev-bundle Checkbox/Select entries backed by the production `indexhtml` chunk, one theme/static classpath CSS resource and one repository-local generated Flow imports fixture resource, and records blocked/failure classification for the remaining `lit`/TextField shims, dev-mode and full generated/lazy graph execution.
9. Emits ordered server push UIDL updates with `UidlWriter`, records duplicate/late probe handling and drives a controlled recovery resync through the same Flow RPC path.

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

The current `FlowIpcRuntime` adapter intentionally does not claim to be the final Vaadin UIDL adapter. It now creates a desktop-shaped Flow session/UI, includes real `UidlWriter` output in `bootstrapResponse` and ordered server-initiated `push` envelopes, lets the visible Flow client engine send UIDL RPC bodies for click/change events and ordering-recovery resync over an IPC-backed bridge, and uses `UidlRequestHandler`/`ServerRpcHandler` for event/resync patches. The WebView promotes the initialized Flow client container to the visible surface, feeds bootstrap/event/push/recovery resync UIDL into that client, ignores a controlled duplicate/late push probe and records delivered/ignored order diagnostics. The repository-local UIDL applier remains as a diagnostic fallback for non-engine or failed-delivery paths. `FlowClientResources` reads the real `flow-client` jar resources from the Java sidecar classpath, augments `FlowClient.js` with the narrow IPC bridge methods used by this slice, serves the full advertised resource graph over IPC, imports `FlowBootstrap.js` / `FlowClient.js` so the host can initialize the Flow bootstrap and GWT client path without localhost HTTP, classifies the remaining resources as verified-only / intentionally-not-executed and records one dynamic `Flow.js` lazy lookup. It also exposes v1.4 frontend artifact metadata for `com.vaadin.flow.component.textfield.BigDecimalField`, `com.vaadin.flow.component.checkbox.Checkbox` and `com.vaadin.flow.component.select.Select`; the host requests and imports the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` module using manifest-driven import rewrites, keeps BigDecimalField's `lit` and `@vaadin/text-field` imports as controlled shims, resolves `@vaadin/component-base/src/define.js` through an extracted Vaadin dev-bundle define helper, verifies the focused TextField production class entry as a partial blocker resource, requests focused Vaadin dev-bundle Checkbox/Select entries that import real classes from the production `indexhtml` chunk, requests/injects one real `theme-static` classpath CSS resource and verifies a repository-local Vaadin generated-flow-imports fixture as a generated/lazy artifact. The remaining `lit`/TextField shim paths, dev-mode bundle resources and full generated/lazy execution remain explicit shim or strict-blocker entries until production bundle/dev-mode/theme handling is complete. The repository also contains the compiled transport adapter around the real Flow server path:

- `dev.vatori.desktop.flow.transport.FlowTransportAdapter`
- `dev.vatori.desktop.flow.transport.UidlFlowTransportAdapter`
- `dev.vatori.desktop.flow.transport.DesktopVaadinRequest`
- `dev.vatori.desktop.flow.transport.DesktopVaadinResponse`
- `dev.vatori.desktop.flow.transport.DesktopWrappedSession`
- `dev.vatori.desktop.flow.transport.DesktopFlowRuntimeSession`
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

`uidl-rpc-smoke` goes one step deeper: it sends a real Flow client event RPC body shaped like Vaadin's own `ServerRpcHandlerTest` examples, lets `UidlRequestHandler` locate the desktop `UI`, runs `ServerRpcHandler`/`EventRpcHandler`, triggers a Button click listener and verifies that the UIDL response contains `Clicks: 1` plus `syncId`. It also verifies that invalid Flow IPC `csrfToken`, invalid security key, stale `syncId` and out-of-order `clientId` values are rejected before dispatch. `flow-ipc-scope-smoke` verifies that event envelopes with mismatched `sessionId` or `uiId` are rejected before event handling. `flow-client-resource-smoke` verifies that `FlowBootstrap.js` and `FlowClient.js` are present on the runtime classpath and that `FlowClient.js` carries the IPC message/resync bridge methods. It also checks the v1.4 manifest metadata for artifact families, resolver/status fields, the available theme/static resource and remaining strict blockers. `dev-ipc` now imports the real Flow client engine, promotes it to the visible renderer, bridges its `XMLHttpRequest` transport over IPC, classifies resource execution diagnostics and uses the same server-side RPC path for Flow client generated click, change and recovery resync dispatch.

The `jakarta.servlet-api` dependency is present on the runtime classpath because Vaadin server classes such as `VaadinSession` and the `VaadinRequest`/`VaadinResponse` signatures still reference servlet API types. This does not start a servlet container and is not used as the `dev-ipc` transport.

The existing spike in `flow-transport-adapter-spike/README.md` remains the code-level map for that work.

## Why This Is Still Useful

The minimum adapter removes localhost HTTP from the demo event/patch loop. That lets the project validate the native host and IPC responsibilities before solving every Flow internal dependency.

It also keeps the developer-facing shape intact: the smoke app still uses ordinary Vaadin Flow Java components and `@Route`.

## Next Adapter Step

The next concrete step is to replace another v1.4 strict blocker with a real frontend artifact path: full generated/lazy execution, dev-mode bundle staging or broader production bundle graph execution. In parallel, the controlled duplicate/late push-ordering recovery smoke still needs a real Flow push queue, ack model and reconnect/resync behavior.
