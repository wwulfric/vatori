# Flow Transport Adapter

`vaadin-desktop-flow` now contains two integration paths:

- `DesktopLauncher`: compatibility HTTP/Spring Boot launcher.
- `FlowIpcLauncher`: minimum Flow-over-IPC launcher.

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

The current adapter intentionally does not claim to be the final Vaadin UIDL adapter. The next implementation step must move from the repository-local renderer toward the real Flow server path:

- `UidlRequestHandler`
- `ServerRpcHandler`
- `UidlWriter`
- `VaadinSession`
- `UI`
- CSRF/security key
- sync id / resync

The existing spike in `flow-transport-adapter-spike/README.md` remains the code-level map for that work.

## Why This Is Still Useful

The minimum adapter removes localhost HTTP from the demo event/patch loop. That lets the project validate the native host and IPC responsibilities before solving every Flow internal dependency.

It also keeps the developer-facing shape intact: the smoke app still uses ordinary Vaadin Flow Java components and `@Route`.
