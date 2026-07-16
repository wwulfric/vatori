# Sidecar IPC Protocol

The Java sidecar and native host use one bidirectional protocol for Flow UI
transport, native capabilities and lifecycle control. The protocol is independent
of whether the WebView loads a packaged production bundle or a loopback URL.

## Framing

Each message is one JSON envelope prefixed by `VFIPC1 ` and terminated by a
newline:

```text
VFIPC1 {"version":"flow-ipc-v1","type":"ready",...}
```

The prefix is process framing, while `flow-ipc-v1` is the envelope version.
Payload bodies use base64 only when opaque bytes must cross JSON framing.

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

The host assigns window scope. Java validates `windowId`, `sessionId` and `uiId`
before accessing a session, exchange, push connection or capability result.
`id` identifies one message; `correlationId` joins replies to requests or native
commands.

## Ready Handshake

Java sends `ready` before the host creates the main window. Its payload contains
the selected transport and the application descriptor:

```json
{
  "transport": "flow-ipc",
  "application": {
    "name": "Counter App",
    "metadata": {},
    "mainWindow": {
      "id": "main",
      "title": "Vaadin Desktop Counter",
      "width": 900,
      "height": 620
    }
  }
}
```

`compatibility-http` additionally requires an `applicationUrl` on
`127.0.0.1`. A `flow-ipc` launch requires the standard Vaadin production bundle
inside the native host artifact. Missing or mismatched fields fail the launch.

## Opaque Requests

Browser to Java:

- `opaqueHttpStreamOpen`: request id, method, URL, headers and content length.
- `opaqueHttpStreamChunk`: ordered request body chunk.
- `opaqueHttpStreamEnd`: terminal chunk count and byte count.
- `opaqueHttpStreamResponseCredit`: response credit granted to Java.
- `opaqueHttpStreamCancel`: cancellation of an in-flight exchange.

Java to browser:

- `opaqueHttpStreamRequestCredit`: request credit granted to the browser.
- `opaqueHttpStreamResponse`: status and repeated response headers.
- `opaqueHttpStreamResponseChunk`: ordered response body chunk.
- `opaqueHttpStreamResponseEnd`: terminal chunk count and byte count.
- `opaqueHttpStreamError`: failure after response streaming started.

Senders may not exceed receiver-issued credit. Sequence numbers, terminal counts
and declared lengths are validated. `opaqueHttpRequest` and
`opaqueHttpResponse` remain a bounded buffered form for focused contracts.

## Push

- `opaquePushConnect` and `opaquePushConnected` establish a scoped Flow push
  connection.
- `opaquePush` carries opaque UIDL text, sequence and resync state.
- `opaquePushDisconnect` closes the exact scoped connection.
- `opaquePushResync` requests Flow resynchronization.

The browser bridge implements only the narrow Atmosphere surface expected by the
standard Flow client. It does not parse UIDL or maintain component state.

## Native Capabilities

Java sends `command` with a command name and arguments. The native host executes
the manifest-authorized operation and returns `capabilityResult` using the
command id as `correlationId`. Both UI transports use this same path; compatibility
mode does not synthesize successful capability results.

## Lifecycle

- `windowClosed` releases the matching Java window/session state.
- `shutdown` asks the sidecar to stop.
- stdin EOF also stops the sidecar.
- `error` reports protocol, scope or runtime failures.

The Java process has one stdin reader and one synchronized stdout writer.
Flow-specific opaque request and push handlers are registered only for
`flow-ipc`; lifecycle and capability correlation are shared.

## Operational Trace

The macOS host writes compact lines prefixed with `HOST_PROTOCOL` for acceptance
inspection. Those lines describe launch, window, bridge and capability activity;
they are not the sidecar wire protocol. Full logs are generated artifacts under
`.build/native-hosts/macos` and should be retained as CI or release artifacts,
not committed.
