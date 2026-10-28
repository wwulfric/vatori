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

The host assigns window scope. The main window uses the stable default scope.
Every secondary `sessionId` is a host-generated opaque UUID identity, currently
encoded on the wire as `flow-ipc-window:<UUID>`; it is independent of the public
`windowId` and must not be chosen, derived or reused by application code. A
reopened public window receives a new secondary session identity. Java validates
the complete `windowId`, `sessionId` and `uiId` tuple before accessing a session,
exchange, push connection or capability result. `id` identifies one message;
`correlationId` joins replies to requests or native commands.

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
- `opaquePush` carries opaque UIDL text, transport sequence, Flow server sync id,
  delivery attempt and resync state.
- `opaquePushAck` confirms the exact scoped transport sequence after the official
  Flow client callback, together with Flow's latest last-seen server sync id.
- `opaquePushDisconnect` closes the exact scoped connection.
- `opaquePushResync` requests Flow resynchronization.

The browser bridge implements only the Atmosphere surface expected by the
standard Flow client. It does not parse UIDL or maintain component state. Java
retains unacknowledged deliveries in process memory and retries the oldest one
with the same transport sequence and Flow sync id. A duplicate delivery is
acknowledged without invoking the Flow client twice. Resync supersedes older
pending deliveries. The queue belongs to the current sidecar generation and
Flow session/UI. Sidecar replacement reloads the document and creates a fresh
session/UI, so any old unacknowledged UIDL is discarded rather than replayed
against the new Flow state tree. Durable application effects must be stored
outside process-local UI and delivery state.

## Capabilities And Windows

Java sends `command`; the host returns `capabilityResult` with the command id as
`correlationId`. Capability manifests are evaluated for the target window scope.
`window.open` and `window.close` are supported only by Flow IPC. Both UI
transports use the same path for other native commands.

`DesktopContext.captureCapabilityContext()` is a Java-local scope-capture
contract; it adds no envelope field or new message type. A Flow application
captures it while a listener or `UI.access` callback has a current Vaadin
session, then passes the immutable facade explicitly to work on any executor.
The facade holds no Flow `UI` or `VaadinSession` reference and exposes no opaque
window/session/UI identity; the bridge keeps its exact binding internal.
Executor or thread inheritance does not implicitly propagate scope.

The scoped bridge binds every invocation through that facade to the exact
captured owner. Closing the owner settles pending commands through the existing
correlation path. Later invocations, including use after the same public window
id has reopened with a new opaque UUID, return the existing typed
`owner-window-closed` failure without sending or retargeting a `command`.
Captured facades are process-local and are not transferred to a new sidecar
generation; host generation fencing independently invalidates and drops stale
old-generation completions. The Java bridge retains an exact closed secondary
scope tombstone even after its live session mapping is unregistered; only a new
opaque UUID forms a registrable incarnation, and all tombstones disappear when
that bridge process closes.

The focused same-public-id reincarnation contract exercises this without a wire
extension: incarnations A, B and C use ordinary `window.create`, `command`,
`capabilityResult` and `windowClosed` messages. Their `windowId` is equal and
their host-generated UUID `sessionId` values are pairwise distinct. B proves
that A's retired facade stays invalid before and after B's fresh title command;
after B closes, C proves the same for both A and B before and after C's fresh
title command. Every retired invocation remains Java-local and returns
`owner-window-closed`; no stale command is sent for the host to retarget.

Without a captured facade or current Vaadin session, the Java bridge retains the
legacy main/default fallback only when that default is the sole live scope. If
multiple scopes are live, invocation fails closed with a Java error and no
`command` envelope. If the main/default scope is already tombstoned, the bridge
returns the typed `owner-window-closed` result instead. Non-scoped bridge
implementations keep their existing behavior through the default capture SPI.
The compatibility HTTP runtime explicitly uses that single-window behavior;
its Servlet-backed Vaadin session does not participate in pure Flow opaque
scope registration.

File-dialog outcomes are normalized by the public Java API rather than encoded
as a new envelope field. `DesktopFileDialog.openFileResult(String)` classifies
the retained `DesktopCommandResult` in this precedence order:
`operationInvalidated=true` -> `INVALIDATED`, then
`permissionDecision=denied` -> `DENIED`, then successful `canceled=true` ->
`CANCELED`, then a successful non-blank, valid local `path` or first such
`paths` entry -> `SELECTED`, otherwise `ERROR`. Malformed path values are
ignored rather than escaping the typed result contract. It preserves the
optional `cancelReason` and
raw command result. Legacy `openFile(String)` delegates to this method and
returns only `selectedPath()`, preserving `Optional.empty()` for every
non-`SELECTED` outcome.

Read-only selected-file access uses three ordinary scoped capability commands,
not a path-bearing transport or a new envelope type. `fileDialog.openReadGrant`
opens one `NSOpenPanel`; a successful result carries an opaque UUID grant id,
the selected basename, its initial byte count and the host read limit. Java
keeps the id private inside `DesktopFileReadGrant` and removes it from the
public retained `DesktopCommandResult`. `fileGrant.read` sends that private id,
an offset and a bounded byte count; the host returns at most 64 KiB as Base64,
with exact offset, next-offset and EOF metadata. `fileGrant.release` closes the
host handle. Applications that grant selection should normally grant all three
commands in the same window manifest scope.

The host binds each grant to the exact `windowId`/`sessionId`/`uiId` owner and
the sidecar generation that selected it. A mismatched, unknown, released or
revoked id has one external `grant-unavailable` shape, so the command channel
does not expose a token-existence oracle. Explicit release prevents later reads;
owner close, sidecar retirement/replacement, sidecar exit and host shutdown
revoke remaining grants and balance any security-scoped URL access. Grant ids,
file bytes, selected basenames and paths are redacted from detailed traces.
Paths are never returned by this API.

This is a process-lifetime read grant. It does not persist security-scoped
bookmarks, survive a host/JVM replacement, authorize writes or claim that the
current ad-hoc development app is running under App Sandbox. The
`securityScoped` result only reports whether AppKit's
`startAccessingSecurityScopedResource()` call acquired access for that URL; it
is not evidence that sandbox enforcement was active.

`fileDialog.saveNewWriteGrant` uses the same command/result envelope for a
create-new-only destination selected by `NSSavePanel`. Its arguments are `title`
and `suggestedName`. A selected result contains `grantId`, `filename`,
`maxWriteBytes`, `securityScoped`, `createNewOnly=true`, `pathExposed=false`,
`canceled=false` and `usedSavePanel=true`. Java removes the private id from the
public command result. An existing destination returns `target-exists` without
truncation; a canceled panel creates no file.

`fileGrant.write` carries `grantId`, an exact integer `offset` and canonical
`bodyBase64` for 1–65,536 bytes. The host independently bounds encoded and
decoded sizes, requires its exact next offset, rejects overflow and validates
the selecting scope/generation. Success returns exact `offset`, `bytesWritten`
and `nextOffset`; `write-failed` may return a partial count and retires the
grant. `invalid-write-range`, `invalid-write-data`, `grant-busy` and the uniform
`grant-unavailable` are distinct failures. `fileGrant.releaseWrite` closes the
write descriptor; read-grant commands cannot use its token. Selection,
write, release and `fileGrant.cancelWrite` are independently manifest-controlled commands.
Full-detail trace redaction covers nested write bytes, token, suggested name,
selected basename and error text without altering the wire payload.

An accepted write can finish while release is pending: its byte-count result
remains meaningful even if the grant has become unavailable. Lifecycle
invalidation may instead settle the operation before that count is known.
Neither invalidation nor release rolls back bytes or removes the new file;
no automatic retry, resume, atomic commit or fsync guarantee is implied.

`fileGrant.cancelWrite` carries only `grantId` and uses the same exact owner and
sidecar-generation guard. Admission on the host main thread makes the grant
unavailable to subsequent writes and requests cooperative cancellation. The
worker checks the signal before every write attempt, including short-write and
EINTR retries. An attempt that has passed its checkpoint can still finish;
there is no interruption or deadline guarantee for a blocking OS write. A fully
written block remains `WRITTEN`. A block stopped at a checkpoint returns
`success=false`, `errorCode=write-canceled`, `canceled=true` and its exact
`offset`, `bytesWritten` and `nextOffset`; real I/O errors remain `write-failed`.

Cancellation registers cleanup immediately, then waits for the write's actual
prefix to be published before closing the descriptor. The successful terminal
result has `operation=cancel-write-grant`, `canceled=true`,
`cancellationRequested=true`, `grantUnavailable=true`, `grantState=unavailable`
and the final total `nextOffset`. A `grant-close-failed` result retains a known
prefix but is not successful cancellation. Lifecycle invalidation, denial,
unavailability, malformed replies or interrupted Java waits do not establish a
final prefix. `DesktopFileWriteCancelResult.nextOffset()` uses `-1` for unknown;
it must never be interpreted as a zero-byte side effect. Only confirmed
cancellation is cached locally as `ALREADY_CANCELED`; it issues no new command.
The new file and its written prefix are preserved, not rolled back or deleted.

Clipboard outcomes use the same Java-local normalization boundary and add no
message type or envelope field. `DesktopClipboard.readTextResult()` classifies
the retained result in this precedence order: `operationInvalidated=true` ->
`INVALIDATED`, `permissionDecision=denied` -> `DENIED`, a successful result
with `textPresent=true` and a string `text` -> `TEXT`, and a successful result
with `textPresent=false` and no `text` field -> `NO_TEXT`; every failed,
incomplete or contradictory shape is `ERROR`. `TEXT` includes the explicit
empty string, so it remains distinct from `NO_TEXT`.
`DesktopClipboard.writeTextResult(String)` uses `INVALIDATED`, `DENIED`, then a
successful `written=true` -> `WRITTEN`, with every other shape classified as
`ERROR`. Both typed results retain the original `DesktopCommandResult`. The
legacy `readText()` and `writeText(String)` methods each delegate once to the
typed operation and preserve their existing return types.

`DesktopNotification.showResult(title, body)` also normalizes the existing
`notification.show` result without adding a wire message type. Precedence is
`operationInvalidated=true` -> `INVALIDATED`, then `permissionDecision=denied`
-> `DENIED` (capability-manifest denial, not OS authorization). Remaining native
results must identify `command=notification.show`,
`mode=native-user-notifications`, and a Boolean `success` consistent with the
command result. A successful `scheduled=true` result with a non-blank
`notificationId` and no error fields is `SCHEDULED`. Failed `scheduled=false`
results without an identifier use `notification-authorization-required` for
`AUTHORIZATION_REQUIRED` or `notification-authorization-denied` for `OS_DENIED`.
Malformed or contradictory shapes and all other failures are `ERROR`; human
error text is never parsed. Only `SCHEDULED` exposes the optional identifier.
The legacy `show(title, body)` delegates once and returns the original result.

The host uses stable error codes `notification-title-invalid`,
`notification-authorization-failed`, `notification-authorization-unsupported`
and `notification-scheduling-failed` for the corresponding `ERROR` cases.
Following an authorization API error, the host reads current OS settings once.
Only an independently observed denied state yields the existing `OS_DENIED`
payload, without diagnostic fields; all other states retain the original error.
For remaining authorization/scheduling API errors, the original payload also
contains
`nativeErrorDomain` (`UNErrorDomain` or the fixed token `other`) and integer
`nativeErrorCode`. These are diagnostics, not permission decisions or retry
instructions. The host does not expose arbitrary NSError domains or `userInfo`
through those fields. After invalidation/manifest-denial precedence, either
diagnostic field forces the typed outcome to `ERROR`, including contradictory
success/authorization shapes. The original application-facing error message is
retained; notification trace redaction still hides it.
`AUTHORIZATION_REQUIRED` is the host's no-activate refusal to begin an
undetermined authorization prompt; it is not a public permission-query API.
`SCHEDULED` means the OS accepted the request, not that a banner was displayed,
delivered or seen. Invalidation or an interrupted/unknown result does not prove
that no OS side effect occurred and does not establish a safe retry.

Closing a Flow IPC owner invalidates its outstanding native operations before
the host releases the window context. An affected command completes with a
correlated failure payload containing `canceled=true`,
`cancelReason=owner-window-closed` and `operationInvalidated=true`. Native
resources such as an attached AppKit panel are detached and hidden, and any
resulting stale completion is dropped. A command arriving with an exact retired
host scope receives the same typed failure instead of being retargeted to
another window.

The macOS host retains every complete retired
`windowId`/`sessionId`/`uiId` identity for the lifetime of that host process;
retiring a newer incarnation does not evict an older one. A later `command` for
any exact retired identity receives the same typed `owner-window-closed`
failure, while late opaque request/push transport for it is dropped. Reusing
the public `windowId` with a fresh opaque UUID remains a distinct admissible
scope.

## Lifecycle

- `windowClosed` tombstones the exact native-command scope immediately, settles
  its pending commands with the typed owner-window-closed result, then releases
  the matching Java push, exchange and session state.
- `shutdown` asks the sidecar to stop, including when it carries an already
  retired scope tuple.
- stdin EOF also stops the sidecar.
- `error` reports protocol, scope or runtime failures.

The Java process has one stdin reader and one synchronized stdout writer. The
macOS host feeds stdin through one bounded nonblocking writer per generation
(16 MiB retained frame bytes, 256 frames, ten seconds without write progress).
Writer admission is not delivery or a Java acknowledgement. Frames retain FIFO
byte order; overflow, write error or stalled progress fail the channel rather
than dropping a frame and continuing on a corrupted stream. Host retirement
may abandon pending frames and use EOF; it never inserts shutdown inside a
partially written frame or carries bytes into a replacement JVM.
The Java reader dispatches normal handler work to one ordered lane per `windowId`:
same-window envelopes remain serialized, while different windows may progress
concurrently. `capabilityResult` completion and the initial `windowClosed`
tombstone happen on the reader before queued lifecycle work. The close handler
runs after earlier work in that window lane, cancels remaining exchanges,
removes push state and synchronously drives UI detach and Vaadin
`SessionDestroy` to `CLOSED` before unregistering the native capability
session-to-scope mapping and retiring the lane.

The first `windowClosed` also installs a process-lifetime transport tombstone
for its complete `windowId`/`sessionId`/`uiId` scope at reader admission. Every
later non-shutdown envelope for that exact scope is silently dropped before
capability-result correlation, handler lookup or dispatch; this includes a
duplicate close and unknown or future scoped message types. Work admitted
before the close remains ordered ahead of teardown. A new opaque session UUID
for the same public window id is not tombstoned and enters the same window lane
after the prior teardown, so a late retired frame can neither recreate its
Vaadin session nor consume a fresh incarnation's correlation id.

First-use scope acquisition is transactional inside the handler path. Java
creates the Vaadin session and push/native registrations before publishing the
window and session owner maps. If any acquisition or publication fails, it
compare-removes only the resources created for that exact attempt, closes the
matching Vaadin session, and rolls back its native registration only after that
close has finished. Registration rollback is distinct from teardown: it keeps
any concurrent close tombstone but does not retire an otherwise live default
scope. Consequently, work admitted before `windowClosed` but reaching its first
session access after the reader tombstone cannot leave a push/session owner that
blocks the next UUID incarnation.

If the main window is already closed while a secondary window remains, each new
sidecar lifecycle-ready boundary carries a normal `windowClosed` replay for the
exact main default scope. The replay is a topology transfer, not a new message
type: `windowClosedReplay` is only its host trace event. It prevents the fresh
Java bridge from treating a no-current-session capability call as a live main
scope. The host does not recreate main; retained secondary windows keep their
opaque scopes and reload their original route-specific bundle entry URLs before
the official Flow client creates fresh session/UI state.

Flow-specific request and push handlers are registered only for `flow-ipc`.

`HOST_PROTOCOL` is an acceptance trace prefix, not sidecar framing. Generated
logs belong in CI or release artifacts, not version control.
