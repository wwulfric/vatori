# IPC Protocol

Framework v0 keeps the envelope designed in `flow-ipc-protocol/protocol-v0.md` and starts moving it into `vaadin-desktop-ipc`. The next-stage host path adds Host Protocol v0 for Java sidecar, native host and WebView bridge coordination.

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

Planned minimum:

- `launch`
- `ready`
- `window.created`
- `window.closed`
- `bootstrap`
- `event`
- `patch`
- `command`
- `capabilityResult`
- `error`
- `shutdown`

Current implementation status:

- `launch`, `ready`, `window.create`, `window.ready`, `command`, `capabilityResult`, `error` and `shutdown` are traced by `vaadin-desktop-host-swift`.
- Java desktop commands are emitted from `HostProtocolNativeBridge` as stdout lines prefixed with `VDP1 `.
- Real Flow `bootstrap`, `event`, `patch` and push still use compatibility-mode HTTP.

## Trace

`IpcTraceLogger` prints structured trace lines for host and native bridge activity. This is intentionally simple so the next step can replace it with NDJSON and an inspector.

Host Protocol trace uses two visible prefixes:

```text
HOST_PROTOCOL {"direction":"send","type":"ready","payload":{...}}
VDP1 {"type":"command","id":"desktop-command-1","payload":{...}}
```

See `docs/traces/host-protocol-v0.ndjson` for an example review trace.
