# Flow IPC Protocol v1

Flow IPC Protocol v1 is the first runtime protocol for moving Vaadin Desktop beyond localhost HTTP compatibility mode.

Current implementation status: **minimum runtime slice implemented**. It supports a local WebView shell, Java sidecar stdin/stdout transport, bootstrap, event, replace-root patch, desktop command/capabilityResult, push trace and shutdown. It does not yet carry Vaadin's native UIDL payload or the real Flow client engine.

## Prefixes

Swift host trace:

```text
HOST_PROTOCOL {"direction":"recv","type":"launch",...}
```

Flow IPC envelopes between WebView/host and Java sidecar:

```text
VFIPC1 {"version":"flow-ipc-v1","type":"bootstrapRequest",...}
```

Compatibility-mode desktop capability command envelopes from Java to host:

```text
VDP1 {"type":"command","id":"desktop-command-1",...}
```

In `dev-ipc`, desktop capability commands also use `VFIPC1` Flow IPC envelopes:

```text
VFIPC1 {"version":"flow-ipc-v1","type":"command","id":"desktop-command-1",...}
```

## Envelope

```json
{
  "version": "flow-ipc-v1",
  "type": "event",
  "id": "webview-2",
  "correlationId": "webview-2",
  "sessionId": "flow-ipc-session",
  "uiId": "main",
  "route": "",
  "payload": {
    "action": "click",
    "componentId": "ipc-button"
  },
  "error": null,
  "timestamp": 1778997000000
}
```

Fields:

- `version`: protocol version, currently `flow-ipc-v1`.
- `type`: message type.
- `id`: message id.
- `correlationId`: request/response correlation id.
- `sessionId`: desktop Flow session id.
- `uiId`: desktop UI/window id.
- `route`: Flow route.
- `payload`: message payload.
- `error`: structured error or `null`.
- `timestamp`: sender timestamp in epoch milliseconds.

## Message Types

Implemented in the minimum runtime:

- `launch`
- `ready`
- `bootstrapRequest`
- `bootstrapResponse`
- `event`
- `patch`
- `command`
- `capabilityResult`
- `push`
- `resync`
- `error`
- `shutdown`

## Current Patch Payload

The current patch payload is deliberately simple:

```json
{
  "patchType": "replaceRoot",
  "reason": "event",
  "transport": "flow-ipc",
  "compatibilityMode": false,
  "tree": {
    "id": "ipc-root",
    "tag": "vaadin-vertical-layout",
    "kind": "layout",
    "text": "",
    "clickable": false,
    "children": []
  }
}
```

This is not Vaadin UIDL yet. It is a repository-local adapter payload that proves the host/WebView/Java event and patch boundary without using a localhost HTTP server.

## Current Command Payload

Desktop API calls in `dev-ipc` use the same envelope with `type=command`:

```json
{
  "type": "command",
  "id": "desktop-command-3",
  "correlationId": "desktop-command-3",
  "payload": {
    "command": "clipboard.writeText",
    "args": {
      "text": "Vaadin Flow over IPC clicks: 1"
    }
  }
}
```

The Swift host responds with `type=capabilityResult`:

```json
{
  "type": "capabilityResult",
  "id": "capability-desktop-command-3",
  "correlationId": "desktop-command-3",
  "payload": {
    "success": true,
    "command": "clipboard.writeText",
    "mode": "native-nspasteboard"
  }
}
```

Implemented host commands in `dev-ipc`:

- `window.setTitle`: updates the AppKit `NSWindow` title.
- `clipboard.writeText`: writes to macOS `NSPasteboard`.
- `notification.show`: accepted as trace-only scaffold.

## Error Model

Errors use the same envelope with `type=error` and an `error` object:

```json
{
  "type": "error",
  "error": {
    "code": "unsupportedEvent",
    "message": "Only click events are supported by the v1 minimum adapter."
  }
}
```

## Compatibility Strategy

- `dev` and `dev-swift` remain compatibility HTTP modes.
- `dev-ipc` uses Flow IPC Protocol v1 and must log `transport=flow-ipc` and `compatibilityMode=false`.
- Full Vaadin UIDL, Flow client engine resource loading, upload/download and push/WebSocket replacement remain future work.
