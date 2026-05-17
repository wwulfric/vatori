# Flow Transport Adapter Spike

This spike records the first real Vaadin Flow transport boundary found in the local Vaadin source tree at `/Users/bytedance/IdeaProjects/study/vaadin`.

It is intentionally code-level and conservative: no upstream Vaadin sources are modified here.

## Candidate Boundary

The most promising first cut is the UIDL request path:

```text
client event JSON
  -> UidlRequestHandler.synchronizedHandleRequest(session, request, response, requestBody)
  -> ServerRpcHandler.handleRpc(ui, requestBody, request)
  -> Java component listener / UI state mutation
  -> UidlRequestHandler.writeUidl(ui, writer, resync)
  -> UidlWriter.createUidl(ui, false, resync)
  -> UIDL JSON patch
```

Local source references:

- `flow/flow-server/src/main/java/com/vaadin/flow/server/SynchronizedRequestHandler.java:58-68`
  already reads the request body before locking the session and writes the response after unlocking for handlers that opt in.
- `flow/flow-server/src/main/java/com/vaadin/flow/server/communication/UidlRequestHandler.java:122-173`
  is the concrete event-to-patch path. It finds the `UI`, dispatches RPC, writes UIDL and returns a response writer.
- `flow/flow-server/src/main/java/com/vaadin/flow/server/communication/UidlRequestHandler.java:191-202`
  isolates `writeUidl` and delegates to `UidlWriter`.
- `flow/flow-server/src/main/java/com/vaadin/flow/server/communication/UidlWriter.java:120-195`
  creates the JSON UIDL/patch object from a `UI`.

## First Real Adapter Shape

The first adapter should not try to replace all of `VaadinRequest`.

Instead, it should carve out this narrower shape:

```text
(VaadinSession, UI, client message JSON, transport metadata)
  -> dispatch RPC
  -> collect UIDL JSON
  -> return Flow patch envelope
```

The current blocker is that the public internal methods still accept `VaadinRequest`:

- `UidlRequestHandler.java:126` uses `session.getService().findUI(request)`.
- `ServerRpcHandler.java:343-353` takes a `VaadinRequest` and reads deployment configuration through `request.getService()`.
- `VaadinService.java:1410-1424` resolves UI id from request parameters and wrapped session.

This means the v0 adapter can bypass the HTTP socket, but cannot yet bypass the request/session abstraction without one of:

1. a small Vaadin-side SPI that accepts `UI`, message JSON and sync/security metadata directly;
2. a desktop `VaadinRequest` adapter backed by Host Protocol metadata instead of servlet request data;
3. a fork/spike subclass around `UidlRequestHandler` and `ServerRpcHandler` to prove the new method signature.

## Bootstrap Boundary

Bootstrap is harder than UIDL patching:

- `flow/flow-server/src/main/java/com/vaadin/flow/server/BootstrapHandler.java:599-609`
  validates request location, resolves UI class and creates/initializes UI from `VaadinRequest` and `VaadinResponse`.
- `flow/flow-server/src/main/java/com/vaadin/flow/server/BootstrapHandler.java:1497-1508`
  uses `UidlWriter.createUidl(ui, false)` for initial UIDL, then writes security key and push id.

The useful split is: keep the existing bootstrap page in compatibility mode for now, then move initial UIDL generation after the UIDL adapter is proven.

## Push Boundary

Push remains the hardest area:

- `flow/flow-server/src/main/java/com/vaadin/flow/server/communication/PushRequestHandler.java:66-96`
  initializes Atmosphere from `VaadinServletService` and servlet config.
- `flow/flow-server/src/main/java/com/vaadin/flow/server/communication/PushHandler.java:164-172`
  expects `VaadinService.getCurrentRequest()` before dispatching RPC.
- `PushHandler.java:299-303` and `PushHandler.java:458-464` again resolve `UI` from `VaadinRequest`.

For the next phase, push should stay out of scope. Treat resync as request/response first; only then map async server push to Host Protocol.

## Spike Conclusion

At least one real boundary is feasible: `UidlWriter.createUidl(UI, false, resync)` is already transport-neutral enough to produce patch JSON from server-side UI state.

The hard part is not patch generation. The hard part is request-derived context:

- session lookup and lock ownership;
- UI id lookup;
- deployment configuration;
- CSRF/security key and message sync ids;
- current request/current UI thread locals;
- servlet/push integration.

Next engineering issue: introduce a `FlowTransportAdapter` in this repository that mirrors the UIDL path and uses a compatibility `VaadinRequest` adapter only at the edge. The goal is to make the future upstream SPI shape obvious before asking Flow to support it directly.
