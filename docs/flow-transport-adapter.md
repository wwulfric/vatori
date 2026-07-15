# Flow Transport Adapter

The adapter work isolates Vaadin Flow server assumptions from the desktop
transport. The goal is to keep the Flow Java programming model while moving the
runtime transport from localhost HTTP toward IPC.

## Current Adapter Surface

`vaadin-desktop-flow/src/main/java/dev/vatori/desktop/flow/transport` contains
the production adapter around:

- `DesktopFlowOpaqueHttpSession`
- `DesktopFlowOpaqueHttpSessionRegistry`
- `DesktopFlowVaadinService`
- `DesktopVaadinRequest`
- `DesktopVaadinResponse`

The adapter creates desktop-owned Vaadin sessions and translates opaque
HTTP-shaped requests into Vaadin request-handler calls. Bootstrap, UIDL, dynamic
resources and other Flow traffic remain ordinary request bytes at the browser,
native-host and IPC layers.

## Boundary Finding

The production Flow server boundary is the normal request-handler chain:

```text
opaque method + URL + headers + body
  -> DesktopVaadinRequest / DesktopVaadinResponse
  -> VaadinService request handlers
  -> bootstrap, UIDL, resource or push behavior selected by Vaadin
  -> opaque status + headers + body
```

This preserves Vaadin's own browser/server protocol and avoids a parallel
component renderer or a request-type switch in the native host. The remaining
hard boundary is Servlet-derived context: session lookup, deployment
configuration, cookies, thread locals, async push and streaming lifecycle.

## Current Resource Surface

The standard path uses Vaadin's production output unchanged. The native host
serves the packaged webapp, while browser requests cross IPC as opaque method,
URL, headers and body bytes. Java dispatches those requests through Vaadin's
request-handler and static-resource paths; neither the host nor the bridge owns a
Flow client resource catalog.

## Smoke Coverage

```bash
bash bin/opaque-http-adapter-smoke
bash bin/flow-ipc-scope-smoke
node test-support/vaadin-desktop-host-swift-acceptance/Tests/FlowIpcOpaqueHttpBridgeSmoke.mjs
node test-support/vaadin-desktop-host-swift-acceptance/Tests/FlowIpcPushBridgeSmoke.mjs
```

`opaque-http-adapter-smoke` verifies production request/response behavior,
including classpath static resources, `HEAD` metadata and repeated `Set-Cookie`
headers.

The browser bridge smokes verify bounded request/response streaming,
backpressure, cancellation and push reconnect/resync behavior without loading a
custom Flow client.

## Current Adapter Boundaries

Completed for the standard runtime:

- Opaque bootstrap and UIDL request dispatch through Vaadin handlers.
- Click/change RPC dispatch through the standard Flow client and server.
- Ordered push and recovery resync delivery to the visible Flow client.
- Opaque request/response and streaming body transport over IPC.
- Standard production webapp and classpath static-resource serving.

Still open:

- Dev-mode/Vite resource execution.
- Durable push queue, ack and reconnect semantics.
- Complete component parity across Flow's full surface.

Do not document future adapter work as numbered milestones. Use semantic product
surfaces and acceptance gates instead.
