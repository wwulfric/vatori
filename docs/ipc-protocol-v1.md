# Flow IPC Protocol v1

Flow IPC v1 carries the standard Vaadin browser/server exchange over a local
sidecar channel. It is deliberately Flow-agnostic at the transport boundary:
the WebView bridge and native host forward HTTP-shaped bytes and never interpret
bootstrap, UIDL, component events or patches.

## Framing

Java and the native host exchange one JSON envelope per line. Each line starts
with `VFIPC1 `.

```text
VFIPC1 {"version":"flow-ipc-v1","type":"ready",...}
```

Payload bodies use base64 only while crossing this JSON framing. Operational
traces must redact body content and retain bounded byte counts instead.

## Envelope

```json
{
  "version": "flow-ipc-v1",
  "type": "opaqueHttpStreamOpen",
  "id": "opaque-http-1-open",
  "correlationId": "opaque-http-1",
  "sessionId": "flow-ipc-session",
  "uiId": "main",
  "windowId": "main",
  "route": "",
  "payload": {},
  "error": null,
  "timestamp": 1784040788912
}
```

`windowId`, `sessionId` and `uiId` are assigned by the host. Java validates the
complete scope before accessing a session, request, push connection or native
capability result. `id` identifies one message; `correlationId` ties replies to
the originating exchange or command.

## Opaque Requests

Browser to Java:

- `opaqueHttpStreamOpen`: request id, method, URL, headers and declared content
  length.
- `opaqueHttpStreamChunk`: ordered request body chunk.
- `opaqueHttpStreamEnd`: terminal chunk count and byte count.
- `opaqueHttpStreamResponseCredit`: permission for Java to emit bounded response
  chunks.
- `opaqueHttpStreamCancel`: cancellation of an in-flight exchange.

Java to browser:

- `opaqueHttpStreamRequestCredit`: permission for the browser to emit bounded
  request chunks.
- `opaqueHttpStreamResponse`: status and repeated response headers.
- `opaqueHttpStreamResponseChunk`: ordered response body chunk.
- `opaqueHttpStreamResponseEnd`: terminal chunk count and byte count.
- `opaqueHttpStreamError`: failure after the response stream has started.

The sender may not exceed receiver-issued credit. Chunk sequence, terminal
counts and declared lengths are validated. Cancellation releases the Java
worker, request body queue and browser stream; a frame already in transit may be
ignored as a late response.

`opaqueHttpRequest` and `opaqueHttpResponse` are the bounded buffered form used
by focused adapter contracts. The production browser bridge uses the streaming
form.

## Push

- `opaquePushConnect`: browser connection id, Flow UI id, push id and last-seen
  server sync id.
- `opaquePushConnected`: Java accepted the scoped connection.
- `opaquePush`: opaque UIDL text, monotonic push sequence and resync marker.
- `opaquePushDisconnect`: closes the exact scoped connection.
- `opaquePushResync`: requests a full Flow resynchronization.

The push bridge implements the narrow Atmosphere surface expected by the
standard Flow client. It does not parse UIDL or maintain component state.

## Native Capabilities

Java sends `command` with a command name and argument object. The host executes
the manifest-authorized native operation and returns `capabilityResult` to the
Java sidecar using the command id as `correlationId`. Java completes the calling
`NativeBridge` invocation only after validating that result's host-assigned
scope. Capability results are not routed through the WebView.

## Lifecycle

- `ready`: Java runtime metadata and supported transport boundary.
- `windowClosed`: releases only the matching session and in-flight exchanges.
- `shutdown`: asks the Java sidecar to stop.
- `error`: structured transport or validation failure.

Host lifecycle events such as process launch, window creation, first meaningful
paint and sidecar recovery are `HOST_PROTOCOL` trace events, not Flow IPC
application messages.

## Compatibility

Flow IPC v1 has one browser runtime: the application's standard Vaadin production
bundle. Unknown message types fail closed with `unsupportedTransport`; there is
no typed bootstrap/event/resource protocol or custom Flow client fallback.
