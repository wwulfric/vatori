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

The current adapter:

1. Sets up `DesktopContext` with a host-protocol native bridge.
2. Instantiates a `@Route` Vaadin Flow component class directly.
3. Renders a small component-tree payload from real Flow components.
4. Maps component ids to server-side component instances.
5. Dispatches `event/action=click` to `Button.click()`.
6. Returns a replace-root patch after Java listener execution.

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

The current `FlowIpcRuntime` adapter intentionally does not claim to be the final Vaadin UIDL adapter. The repository now also contains the first compiled transport adapter around the real Flow server path:

- `dev.vatori.desktop.flow.transport.FlowTransportAdapter`
- `dev.vatori.desktop.flow.transport.UidlFlowTransportAdapter`
- `dev.vatori.desktop.flow.transport.DesktopVaadinRequest`
- `dev.vatori.desktop.flow.transport.DesktopVaadinResponse`
- `dev.vatori.desktop.flow.transport.DesktopWrappedSession`
- `dev.vatori.desktop.flow.transport.UidlAdapterSmokeMain`

`UidlFlowTransportAdapter` wraps:

- `UidlRequestHandler`
- `ServerRpcHandler`
- `UidlWriter`
- `VaadinSession`
- `UI`

The adapter accepts `(UI, FlowClientMessage)` and returns `FlowPatchResponse`. Internally it creates a desktop `VaadinRequest` with UIDL request parameters, captures a desktop `VaadinResponse`, sets Flow current instances, runs `UidlRequestHandler.synchronizedHandleRequest(...)`, then exposes the UIDL JSON body.

This is still an adapter slice, not the runtime default. It depends on Vaadin's current internal API shape and still needs a real initialized `UI`, `VaadinSession`, CSRF/security key, sync id and client UIDL JSON from the real Flow client engine.

`UidlAdapterSmokeMain` now constructs a minimal in-process `VaadinService`, `VaadinSession`, `WrappedSession` and `UI`, attaches a real Flow component tree, calls `UidlWriter` through `UidlFlowTransportAdapter`, and asserts that the output contains Flow UIDL `changes` and `syncId`.

Run it with:

```bash
bash bin/uidl-adapter-smoke
```

This command does not open a WebView and does not run a servlet container. It is a server-side adapter verification that proves the repository can drive Vaadin's real UIDL writer from a desktop-shaped session/request boundary.

The newly added `jakarta.servlet-api` dependency is `provided` only because Vaadin's `VaadinRequest` and `VaadinResponse` method signatures expose `jakarta.servlet.http.Cookie`. It is not used as a transport in `dev-ipc`.

The existing spike in `flow-transport-adapter-spike/README.md` remains the code-level map for that work.

## Why This Is Still Useful

The minimum adapter removes localhost HTTP from the demo event/patch loop. That lets the project validate the native host and IPC responsibilities before solving every Flow internal dependency.

It also keeps the developer-facing shape intact: the smoke app still uses ordinary Vaadin Flow Java components and `@Route`.

## Next Adapter Step

The next concrete step is to replace the manually initialized smoke `UI` with the runtime `dev-ipc` session/UI lifecycle, then feed the WebView's Flow-client UIDL payload into `UidlFlowTransportAdapter` instead of the repository-local `FlowIpcRenderer`.
