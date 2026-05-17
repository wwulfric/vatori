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
5. Accepts a WebView-provided Flow UIDL RPC body for click events and dispatches it through `UidlRequestHandler`.
6. Returns a replace-root patch after Java listener execution, with the server-side UIDL response attached.
7. Publishes a manifest of real Vaadin `flow-client` classpath resources and serves/initializes `FlowBootstrap.js` / `FlowClient.js` through `resourceRequest` / `resourceResponse`.

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

The current `FlowIpcRuntime` adapter intentionally does not claim to be the final Vaadin UIDL adapter. It now creates a desktop-shaped Flow session/UI, includes real `UidlWriter` output in `bootstrapResponse`, one server-initiated `push` and one verification `resync`, lets the visible Flow client engine send UIDL RPC bodies for click events over an IPC-backed XHR bridge and uses `UidlRequestHandler`/`ServerRpcHandler` for event patches. The WebView promotes the initialized Flow client container to the visible surface for bootstrap and event patch consumption; push and resync currently remain on the repository-local fallback. `FlowClientResources` reads the real `flow-client` jar resources from the Java sidecar classpath and serves `FlowBootstrap.js` / `FlowClient.js` over IPC so the host can initialize the Flow bootstrap and GWT client path without localhost HTTP. The repository also contains the compiled transport adapter around the real Flow server path:

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

This is still an adapter slice. It depends on Vaadin's current internal API shape and still needs broader Flow-client event coverage, full push/resync semantics and complete client-engine consumption for server-initiated updates. The current adapter now performs a minimum Flow IPC `csrfToken`/security key, `syncId` and `clientId` preflight before `UidlRequestHandler` dispatch.

`UidlAdapterSmokeMain` now constructs a minimal in-process `VaadinService`, `VaadinSession`, `WrappedSession` and `UI`, attaches a real Flow component tree, calls `UidlWriter` through `UidlFlowTransportAdapter`, mirrors Flow bootstrap's `Vaadin-Security-Key`, and asserts that the output contains Flow UIDL `changes`, `syncId` and the security key.

Run it with:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

These commands do not open a WebView and do not run a servlet container. They are server-side adapter verification steps that prove the repository can drive Vaadin's real UIDL writer and event RPC path from a desktop-shaped session/request boundary. The same session helper is now used by `dev-ipc` to emit UIDL markers in real runtime traces.

`uidl-rpc-smoke` goes one step deeper: it sends a real Flow client event RPC body shaped like Vaadin's own `ServerRpcHandlerTest` examples, lets `UidlRequestHandler` locate the desktop `UI`, runs `ServerRpcHandler`/`EventRpcHandler`, triggers a Button click listener and verifies that the UIDL response contains `Clicks: 1` plus `syncId`. It also verifies that invalid Flow IPC `csrfToken`, invalid security key, stale `syncId` and out-of-order `clientId` values are rejected before dispatch. `flow-ipc-scope-smoke` verifies that event envelopes with mismatched `sessionId` or `uiId` are rejected before event handling. `flow-client-resource-smoke` verifies that `FlowBootstrap.js` and `FlowClient.js` are present on the runtime classpath and contain the expected Flow client entries. `dev-ipc` now imports the real Flow client engine, promotes it to the visible renderer, bridges its `XMLHttpRequest` transport over IPC and uses the same server-side RPC path for Flow client generated click dispatch.

The `jakarta.servlet-api` dependency is present on the runtime classpath because Vaadin server classes such as `VaadinSession` and the `VaadinRequest`/`VaadinResponse` signatures still reference servlet API types. This does not start a servlet container and is not used as the `dev-ipc` transport.

The existing spike in `flow-transport-adapter-spike/README.md` remains the code-level map for that work.

## Why This Is Still Useful

The minimum adapter removes localhost HTTP from the demo event/patch loop. That lets the project validate the native host and IPC responsibilities before solving every Flow internal dependency.

It also keeps the developer-facing shape intact: the smoke app still uses ordinary Vaadin Flow Java components and `@Route`.

## Next Adapter Step

The next concrete step is to route push/resync through the initialized Flow client engine and tighten full resync semantics.
