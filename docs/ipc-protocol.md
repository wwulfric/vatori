# IPC Protocol

The shared host envelope lives in `vaadin-desktop-ipc`. It is intentionally
small: one message per envelope, stable correlation fields, a payload object and
an optional error object. The Swift host path adds Host Protocol tracing for Java
sidecar, native host and WebView bridge coordination.

Flow IPC Protocol v1 is documented in `docs/ipc-protocol-v1.md`. It adds scoped
window identity, opaque request streaming, push transport, native capability
correlation and lifecycle messages for the `dev-ipc` runtime.

## Envelope

```json
{
  "type": "command",
  "id": "msg-1",
  "correlationId": "trace-1",
  "sessionId": "session-main",
  "uiId": "ui-main",
  "payload": {},
  "error": null
}
```

Fields:

- `type`
- `id`
- `correlationId`
- `sessionId`
- `uiId`
- `payload`
- `error`

## Message Types

Stable message families are opaque HTTP stream, opaque push, native
`command`/`capabilityResult`, window lifecycle, `ready`, `error` and `shutdown`.
Bootstrap, events and UIDL patches remain opaque bodies owned by Vaadin.

Current implementation status:

- `launch`, `ready`, `window.create`, `window.ready`, `command`, `capabilityResult`, `error` and `shutdown` are traced by `vaadin-desktop-host-swift`.
- Compatibility-mode Java desktop commands are emitted from `HostProtocolNativeBridge` as stdout lines prefixed with `VDP1 `.
- `dev-ipc` emits Flow IPC envelopes, including desktop `command` and `capabilityResult`, as stdout/stdin lines prefixed with `VFIPC1 `.
- Compatibility modes still use HTTP for real Vaadin bootstrap, event, patch and push.

## Trace

`IpcTraceLogger` prints structured trace lines for host and native bridge activity. This is intentionally simple so the next step can replace it with NDJSON and an inspector.

Host Protocol trace uses two visible prefixes:

```text
HOST_PROTOCOL {"direction":"send","type":"ready","payload":{...}}
VDP1 {"type":"command","id":"desktop-command-1","payload":{...}}
```

Generate a current review trace by running the Swift host path and inspecting
`.build/vaadin-desktop-host-swift/host.out.log`. Full trace logs are generated
artifacts and are not committed per version.
