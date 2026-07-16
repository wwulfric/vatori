# Flow Transport Adapter

The desktop adapter preserves Vaadin's browser/server protocol and replaces only
the transport boundary.

## Request Path

```text
opaque method + URL + headers + body
  -> DesktopVaadinRequest / DesktopVaadinResponse
  -> VaadinService.handleRequest()
  -> Vaadin request-handler chain
  -> opaque status + repeated headers + body
```

`DesktopFlowOpaqueHttpSession` owns session-scoped request adaptation. It does
not branch on static, bootstrap, UIDL, upload or download request type before
calling Vaadin.

## Desktop Request Handlers

The adapter registers two focused handlers:

- `DesktopClasspathResourceRequestHandler` serves safe GET/HEAD classpath
  resources when no servlet static server exists.
- `DesktopStreamResourceRequestHandler` handles only registered
  `StreamResource` downloads because Vaadin's upstream download handler requires
  `VaadinServletRequest` for `ServletContext` access.

The desktop stream handler returns `false` for Upload, StreamReceiver and other
dynamic resource types. Vaadin's standard `StreamRequestHandler` remains
responsible for those paths.

`DesktopFlowVaadinService` also supplies a classloader-backed `ResourceProvider`
for Flow component and theme resources.

## Streaming And Push

Opaque request/response bodies use bounded chunks, credits and cancellation.
The Java worker sees streaming servlet-compatible input/output adapters without
the native host interpreting request semantics.

Push uses a desktop `PushConnectionFactory` and Flow's normal UIDL writer. The
browser bridge presents the narrow Atmosphere API expected by the standard Flow
client and transports push messages opaquely.

## Verification

```bash
bash bin/opaque-http-adapter-smoke
bash bin/flow-ipc-scope-smoke
node test-support/native-host-macos-acceptance/Tests/flow-ipc-opaque-http-bridge-smoke.mjs
node test-support/native-host-macos-acceptance/Tests/flow-ipc-push-bridge-smoke.mjs
```

The adapter smoke verifies handler ordering, classpath GET/HEAD, repeated
`Set-Cookie`, StreamResource content/headers and VaadinSession context. Scope and
browser smokes cover window isolation, backpressure, cancellation and push
reconnect/resync.

## Open Boundaries

- dev-mode/Vite resource execution;
- durable push acknowledgement and reconnect semantics;
- large-file retry/resume behavior;
- complete Flow component and browser behavior parity.
