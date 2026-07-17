# Sidecar IPC Protocol

The Java sidecar and native host use one bidirectional protocol for Flow UI
transport, native capabilities and lifecycle. It is independent of whether
WKWebView loads a packaged bundle or a loopback URL.

## Framing And Version

Each message is a single JSON object prefixed by the stable text `VFIPC ` and
terminated by a newline:

```text
VFIPC {"version":1,"type":"ready",...}
```

The prefix performs stream framing and does not contain a version. The envelope
has one required numeric `version`; the only accepted value is `1`. Missing,
string or unknown values produce a protocol error and stop that exchange.
Invalid `ready` terminates startup. There is no version alias encoded in the
framing prefix.

Opaque bytes use base64 only where JSON framing requires it.

## Envelope

```json
{
  "version": 1,
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

Java sends `ready` before the host creates the main window:

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
      "height": 620,
      "route": ""
    }
  }
}
```

`compatibility-http` additionally requires an `applicationUrl` on
`127.0.0.1`. `flow-ipc` requires the staged production webapp. Missing or
mismatched fields fail launch.

## Opaque Requests

Browser to Java:

- `opaqueHttpStreamOpen`: method, URL, headers and content length.
- `opaqueHttpStreamChunk`: ordered request body chunk.
- `opaqueHttpStreamEnd`: terminal chunk count and byte count.
- `opaqueHttpStreamResponseCredit`: response credit granted to Java.
- `opaqueHttpStreamCancel`: cancellation of an exchange.

Java to browser:

- `opaqueHttpStreamRequestCredit`: request credit granted to the browser.
- `opaqueHttpStreamResponse`: status and repeated response headers.
- `opaqueHttpStreamResponseChunk`: ordered response body chunk.
- `opaqueHttpStreamResponseEnd`: terminal chunk count and byte count.
- `opaqueHttpStreamError`: failure after streaming starts.

Senders may not exceed receiver-issued credit. Sequence numbers, terminal counts
and declared lengths are validated.

## Push

- `opaquePushConnect` and `opaquePushConnected` establish a scoped connection.
- `opaquePush` carries opaque UIDL text, sequence and resync state.
- `opaquePushDisconnect` closes the exact scoped connection.
- `opaquePushResync` requests Flow resynchronization.

The browser bridge implements only the Atmosphere surface expected by the
standard Flow client. It does not parse UIDL or maintain component state.

## Capabilities And Windows

Java sends `command`; the host returns `capabilityResult` with the command id as
`correlationId`. Capability manifests are evaluated for the target window scope.
`window.open` and `window.close` are supported only by Flow IPC. Both UI
transports use the same path for other native commands.

## Lifecycle

- `windowClosed` releases the matching Java session state.
- `shutdown` asks the sidecar to stop.
- stdin EOF also stops the sidecar.
- `error` reports protocol, scope or runtime failures.

The Java process has one stdin reader and one synchronized stdout writer.
Flow-specific request and push handlers are registered only for `flow-ipc`.

`HOST_PROTOCOL` is an acceptance trace prefix, not sidecar framing. Generated
logs belong in CI or release artifacts, not version control.
