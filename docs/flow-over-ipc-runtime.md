# Flow over IPC Runtime

Flow IPC is the default architecture:

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

It runs the application's standard Vaadin production bundle in WKWebView without
a localhost UI server.

## Startup

```text
Java CLI builds a production webapp and unique runnable JAR
  -> platform builder stages both into a macOS app
  -> Swift reads VatoriHost.json and executes java -jar
  -> pure DesktopLauncher initializes explicit Flow routes
  -> Java sends structured ready
  -> WKWebView loads the staged production webapp
```

The CLI owns Maven, frontend generation and staging. `vaadin-desktop-flow` owns
only the pure Java runtime and transport adapter.

## Vite Development Frontend

```bash
bash bin/vaadin-desktop dev-vite examples/counter-app
```

This mode prepares Vaadin's standard Vite workspace, starts a CLI-owned
loopback Vite process and lets the host proxy the development resource graph
through the same `vatori-app` document origin. Vite's HTTP/WebSocket connection
is used only for frontend modules and HMR. Flow bootstrap, UIDL, events,
resources and push continue over opaque IPC.

The Java sidecar runs the Vaadin service in development mode but does not start
the upstream Servlet `DevModeInitializer`. A temporary build-only
`vaadin-dev-server` dependency supplies Vaadin's standard frontend development
resources and is excluded from the runnable sidecar JAR. The CLI passes the
source project directory to the sidecar so standard development configuration,
including feature flag resources, resolves without treating the app bundle as a
build project.

While the host is running, the same Maven invocation that builds the sidecar
exports its selected effective reactor model to the CLI. The model therefore
reflects active profiles, inherited and interpolated build configuration, the
actual `-pl ... -am` project closure, each selected project's effective compile
source and resource roots, and its local POM chain. The CLI observes those paths
plus root `.mvn` configuration. A POM or Maven configuration change also
regenerates the standard Vaadin Vite workspace after the reactor build; the
observer refreshes from a newly exported effective model after every successful
build. Source roots added only by lifecycle plugins after Maven's projects-read
phase are not yet observed. A successful Maven rebuild is
copied to a temporary file beside the staged sidecar JAR. The CLI preserves the
last ready JAR, atomically stages the candidate and sends a versioned,
request-correlated lifecycle command to the host. The host deliberately
replaces the Java process, retains its native windows and reloads each existing
WKWebView after the candidate sends valid `ready`. The CLI commits that candidate
only after the host result. If the candidate cannot start, exits before `ready`
or misses the ready deadline, the CLI atomically restores the last ready JAR and
the host starts it before reloading the retained WebViews. A failed build leaves
both the running process and staged JAR unchanged.

This is process-level Java reload with fresh Vaadin sessions and UI state, not
JVM in-place class hot replacement. Complete upstream DevTools server
integration remains open.

## Browser Transport

Document-start bridges carry URL, method, headers and body bytes through `VFIPC `
framing without inspecting bootstrap, UIDL, events or patches. The standard Flow
client remains the only browser runtime. The static standard-bundle index keeps
Vaadin's enabled feature flag initializer in the browser by injecting the ids
reported by the sidecar `ready` payload before the production modules execute.
The document keeps a virtual HTTPS base so the standard Flow client derives
request and route paths normally. The opaque bridge maps History API targets
back to the current `vatori-app` origin before calling native
`pushState`/`replaceState`, while request URLs continue through the virtual HTTPS
origin. This preserves standard Flow navigation without a custom router.
Streaming messages enforce chunk order, byte counts, credit and cancellation.
A focused real-WKWebView gate uses a public Flow `DownloadHandler` to stream a
deterministic response larger than 4 MiB, cancels after consuming at least
256 KiB, and resumes from that offset with an open-ended HTTP `Range` request.
The same generic transport propagates cancellation to the Java output stream,
preserves the request header and `206`/`Content-Range` response, and reconstructs
the remaining bytes exactly. Range interpretation and retry policy stay in the
application; neither the host nor the adapter branches on download semantics.
A separate focused gate uses a public raw Flow `UploadHandler` with deterministic
content larger than 4 MiB. It observes normal XHR progress, aborts after at least
256 KiB, verifies request cancellation reaches the Java input stream, then
re-selects an equivalent file and completes a distinct request with exact bytes
and metadata. Vaadin's normal abort path removes the first file, so this is an
application-driven re-selection retry rather than automatic or resumable
upload. The host and adapter remain upload-semantic agnostic.
A multipart-focused gate sends a deterministic file larger than 4 MiB through
the standard Upload `FormData` path. The bridge streams the unknown-length
multipart framing opaquely, and cancellation interrupts Servlet multipart
parameter parsing before the public handler accepts a file. Because Servlet
multipart form fields also participate in `getParameter*()`, Vaadin session
lookup can observe the parse failure before dispatch reaches `UploadHandler`;
the adapter does not manufacture a component-level failed `UploadResult`.
Re-selecting the file creates a distinct request whose parsed bytes and metadata
reach the handler exactly, followed by the public successful result,
file-removed/all-finished listeners and UIDL patch.

`DesktopVaadinRequest` delegates multipart parsing to Apache Commons FileUpload's
Jakarta Servlet adapter. It creates a request-scoped temporary directory,
retains small parts in memory, spills parts above the configured threshold to
disk, supports Servlet `Part.write()`/`delete()`, aggregates query and multipart
form parameters, and cleans temporary storage after success, malformed input or
cancellation. The application can configure storage location, memory threshold,
file/request/part-count limits and per-part header size through
`DesktopFlowMultipartConfig`. Adapter smoke covers repeated fields, quoted
filenames, boundary-like binary content, spill, explicit part lifecycle and
failure cleanup. The real-window gate additionally observes disk spill and
cleanup for the successful 4 MiB retry. This remains bounded validation rather
than proof of every Servlet-container edge case, high-scale multipart
concurrency, repeated cancellation or durable process-external transfer state.
A second focused gate starts two standard raw Upload XHRs in one WKWebView. It
requires both opaque request IDs to open before either terminal frame, cancels
one file after both requests make progress, and verifies that only the canceled
Java input stream fails while its sibling receives the exact declared body and
returns successfully. Public file-removed and all-finished listeners plus the
resulting UIDL patch confirm the component lifecycle. This validates generic
same-window request isolation without adding an Upload branch to the host or
adapter.
A third focused gate submits ten consecutive batches of six standard raw
requests through Vaadin's default three-active production Upload queue. Two
requests per batch are canceled: the first two large batches trigger at
`512 KiB`, while eight compact churn batches trigger at `192 KiB` and require
at least `160 KiB` to reach Java beyond the initial `128 KiB` credit. Forty
survivors complete an exact `33,640,688` bytes. Browser, Java and protocol
maximum-active counts remain three; trace ordering proves queue replenishment
within each batch and full termination before every next batch opens. A
test-only boundary probe correlates the actual opaque IDs and verifies that
completed entries are reclaimed while
canceled entries absorb late response metadata, chunk, end and error frames.
It also exercises the production `60`-second cancellation tombstone in a real
WKWebView: the canceled ID remains isolated at about `55` seconds, expires after
the boundary, and the following nine standard Upload batches succeed after the
expected unknown-request diagnostic is cleared by test-support.
Java smoke also exercises completion/cancellation reclamation and late request
frame rejection. This validates the generic request registry, credit and
cancellation paths without making Upload a runtime adaptation unit.
A fourth focused gate starts two standard multipart Upload XHRs in the same
WKWebView and cancels one after both browser requests and opaque streams overlap.
Vaadin acquires the session lock before checking multipart request parameters,
so same-session Servlet parsing is intentionally serialized: acceptance
observes exactly one request-scoped temporary directory at a time. Cancellation
releases that path without entering the application handler; the sibling then
parses, spills to disk, reaches the public handler with exact bytes and returns
successfully. Public removal/all-finished listeners, UIDL confirmation and an
empty temporary root close the lifecycle. This matches upstream Vaadin locking
instead of adding parallel parsing or Upload semantics to the adapter.
A fifth focused gate opens the same public multipart Upload route in two
native windows with distinct Vaadin sessions. Both streams advance beyond the
initial transport credit while two request-scoped directories and two spill
files coexist, proving that the upstream lock remains session-scoped rather
than globally serializing the parser. Canceling the main-window request is
correlated by window, session, UI and request id and does not affect the second
window, whose exact file reaches the public handler before both sessions emit
listener, UIDL and push updates and clean their temporary parts. The fixture
and scoped trace correlation remain test-only.

## Java Runtime

`DesktopFlowApplication` supplies all route classes and optional instantiation.
There is no default classpath scanner or Spring context. Every opaque request
becomes `DesktopVaadinRequest` and `DesktopVaadinResponse`, then enters
`VaadinService.handleRequest()`. Desktop handlers only supply classpath resources
and adapt the servlet-only `StreamResource` download path.

The optional Spring adapter can provide `SpringInstantiator`, scopes and route
scanning without changing this request path.

## Push And Capabilities

The bridge exposes the Atmosphere calls required by the standard client. Java
validates push id and window/session/UI scope and emits opaque UIDL with sequence
and resync metadata. Java retains unacknowledged messages in process memory and
retries the oldest delivery with the same transport sequence and Flow server
sync id. The browser acknowledges only after the official Flow client callback
and reports Flow's last-seen server sync id; duplicate retries are acknowledged
without invoking that callback twice. A reconnect preserves scope, carries the
current last-seen id and confirms already-consumed deliveries. A browser-observed
sequence gap requests a full Flow resync before normal delivery resumes.

Focused real-WKWebView gates prove both paths: one drops the first delivery of a
final `UI.access` message and requires same-sequence redelivery without later
browser activity; another requires reconnect, gap-triggered resync and a later
normal update to reach the existing DOM. The sidecar-replacement gate leaves a
third `UI.access` delivery unacknowledged, terminates that process, then requires
the replacement document to receive and acknowledge only its new sequence-one
and sequence-two baseline pushes. The old marker must not appear after a bounded
observation interval. This proves that delivery retry is session-scoped and that
old UIDL is not replayed into a fresh Flow state tree.

Desktop API calls emit a scoped `command` and complete only after a real host
`capabilityResult`. Clipboard, dialogs, notifications, windows and file transfer
are native host operations; notification denial or scheduling failure is
reported as failure. A failed result retains the host payload as well as the
Java error string, so callers can inspect generic permission and operation
metadata without parsing the message.

`DesktopNotification.showResult(title, body)` distinguishes manifest `DENIED`,
OS `OS_DENIED`, `AUTHORIZATION_REQUIRED`, `SCHEDULED`, `INVALIDATED` and `ERROR`
while retaining the original command result. Invalidation wins over manifest
denial; other outcomes require a consistent native notification payload.
`SCHEDULED` exposes the identifier accepted by the OS, not a delivery receipt,
presentation guarantee or persistent cancellation capability. The old `show`
method remains a single-invocation raw-result delegate. With the no-activate
policy selected by the acceptance overlay, the host returns
`AUTHORIZATION_REQUIRED` when authorization is undetermined. The default
production observer does not select that policy and can request authorization.
An authorization API error triggers one read-only settings check. Only an
independently observed `denied` state produces `OS_DENIED`; all other states
retain `ERROR`. Error codes and messages alone never establish denial. This
additional callback is fenced by the same active operation token and never
retries authorization or schedules a notification. Notification content and
identifiers are redacted from full trace.
Authorization/scheduling failures retain a safe native domain token and integer
code in the raw command result and both trace detail modes, without parsing
localized messages. Diagnostic-bearing native results cannot become a typed
authorization or scheduling success; invalidation and manifest-denial
precedence remain unchanged. Arbitrary domains, error text and `userInfo` are
not diagnostic trace metadata.
The typed parser and trace gate have contract/synthetic coverage. A focused
real-WKWebView gate also verifies blank-title `ERROR`, no-prompt
`AUTHORIZATION_REQUIRED` and manifest `DENIED` in a separate opaque secondary
scope, with three trusted generated AppKit clicks, public captured-context
background calls and scoped native result/push/visible-state correlation.
Read-only OS observations remain `not-determined` before and after. This uses
the acceptance overlay's no-prompt policy, not a production authorization
choice. Separate focused gates also cover real external-operator OS allow/deny
choices: initial `not-determined` becomes `authorized`/`denied`, the main result
is `SCHEDULED`/`OS_DENIED`, and both complete the same three-stage scoped
result/push/visible-state checks. Scheduling acceptance is not notification
presentation or delivery evidence. Flow input remains generated AppKit, without
physical hardware or assistive-technology validation.
A separate focused foreground-delivery gate leaves the production delegate
unset. A fresh app completes one public preparation call and then an
already-authorized probe, with two trusted generated AppKit clicks, captured
background calls, native command/result and Java UI.access/push state. The
probe's public identifier digest matches a real notification still retained by
Notification Center; application/window resign observers and active/key checks
cover the uninterrupted dispatch-to-query interval. This is observed retained
delivery for that path, not proof of a visible banner, sound, physical input or
general delivery reliability. Visible-banner coverage remains unverified.
Test-only best-effort removal targets matched
identifiers and does not introduce a public cancellation capability.

`DesktopFileDialog.openFileResult(String)` exposes that retained result through
the public `DesktopFileDialogResult` contract. Classification is deterministic
and ordered: `operationInvalidated=true` is `INVALIDATED`; otherwise
`permissionDecision=denied` is `DENIED`; otherwise a successful result with
`canceled=true` is `CANCELED`; otherwise a successful result with a non-blank,
valid local `path`, or the first such entry in `paths`, is `SELECTED`; malformed
path values are ignored and every other failed or incomplete result is `ERROR`.
The normalized value also exposes the selected path, optional `cancelReason`
and original `DesktopCommandResult`.
The existing `openFile(String)` API remains source-compatible: it delegates to
`openFileResult` and returns its `selectedPath()`, so all non-`SELECTED`
outcomes remain `Optional.empty()`.

`DesktopClipboard.readTextResult()` and `writeTextResult(String)` likewise
normalize the retained command result without changing the wire protocol. Read
classification is ordered `INVALIDATED`, `DENIED`, exact successful `TEXT`,
exact successful `NO_TEXT`, then `ERROR`; an explicit empty string is `TEXT`,
not `NO_TEXT`, and incomplete or contradictory payloads fail closed as
`ERROR`. Write classification is `INVALIDATED`, `DENIED`, exact successful
`WRITTEN`, then `ERROR`. Both results expose their original
`DesktopCommandResult`. The source-compatible `readText()` and
`writeText(String)` APIs delegate once to these typed operations.

`DesktopFileDialog.openReadGrant(String)` provides the path-free selected-file
contract. It captures the dialog bridge's current capability context, returns a
typed `INVALIDATED`, `DENIED`, `CANCELED`, `SELECTED` or `ERROR` result, and on
selection yields `DesktopFileReadGrant`. The grant exposes only basename,
initial byte count, the 64 KiB per-read limit and whether AppKit acquired
security-scoped access; its UUID remains private and is removed from the public
retained command result. `read(offset, maxBytes)` returns typed bytes plus exact
range/EOF metadata, while `release()` closes the host grant and a second
successful local release reports `ALREADY_RELEASED`. A read after release still
goes to the host and must return `UNAVAILABLE`, proving that local handle state
does not forge availability.

The macOS host owns the selected URL and open read-only file handle. It accepts
reads and release only from the exact selecting window/session/UI scope and
sidecar generation, serializes each grant's reads, and presents the same
`grant-unavailable` result for unknown, mismatched, released and revoked ids.
Owner close, sidecar retirement/replacement or exit, and host shutdown revoke
unreleased grants. Traces replace grant identity and content with a host-local
numeric lifecycle id plus bounded lifecycle, scope, generation, permission and
range/count metadata; grant ids, path, basename and bytes are redacted.

The focused real-window gate verifies bounded read, explicit release,
read-after-release rejection and owner-window-close cleanup in the exact
selecting scope. It uses external native UI selection and confirmation in two
real panels, seven generated AppKit pointer events into WKWebView, and exact
command/result, lifecycle and redacted-trace correlation. The successful
security-scope acquisition branch and matching stop on release and owner-close
are covered. This remains focused evidence, not physical keyboard/mouse or
Finder-origin input coverage.

A separate bounded-range gate exercises the public typed grant against one
externally selected 65,573-byte nonuniform binary file. Negative offset, zero
length and a request above 64 KiB return `INVALID_RANGE` without retiring the
grant. Reads then verify an exact 64 KiB block, a 37-byte short tail, empty EOF
at and beyond the initial file size, and a backward seek with matching content.
The same retained grant releases normally. Ten trusted generated AppKit clicks,
Java outcomes, opaque UI updates and exact host range/count/lifecycle receipts
correlate the sequence; bytes are compared privately and never traced. This
adds real file-handle and typed-result evidence beyond simulated result parsing,
but does not cover concurrent reads, changing file contents or long transfers.

`DesktopFileDialog.saveNewWriteGrant(title, suggestedName)` separately returns
a path-free `DesktopFileWriteGrant` for a newly created destination. Selection
is typed as `SELECTED`, `TARGET_EXISTS`, `CANCELED`, `DENIED`, `INVALIDATED` or
`ERROR`. The captured grant offers `write(offset, byte[])`, `cancel()`, `release()` and
`close()`, with a 64 KiB nonempty-block limit checked by both Java and the host.
Only the exact next offset is accepted; there is no seek, overwrite or hole
creation. Bytes are written immediately; release never deletes the file.
Existing targets are protected by atomic exclusive creation, not a racy
existence check. A write error preserves any verified prefix count and retires
the grant. A missing or invalidated reply is not proof of zero side effects.

`cancel()` is a separately permitted, owner-bound terminal operation. It closes
admission to later blocks and asks the worker to stop at the next observed
checkpoint between OS write attempts. A write already past its checkpoint can
still complete; there is no syscall interruption or bounded-time promise.
An interrupted block reports `DesktopFileWriteResult.Outcome.CANCELED` with its exact
written prefix, while a complete block remains `WRITTEN`. The terminal
`DesktopFileWriteCancelResult` exposes the final total `nextOffset` after native
close; unknown outcomes use `-1`, and close failure can preserve a known prefix
without claiming success. Repeated confirmed cancellation is Java-local
`ALREADY_CANCELED`, and subsequent `close()` does not send a release. Explicit
release or write after cancellation still reaches the host and is unavailable.
Cancellation does not delete bytes, restore the grant or implement resume.

The focused Sandbox write gate uses a real externally operated Save panel,
seven generated AppKit clicks, public Java listeners and opaque UIDL/push.
It checks denied creation before selection, a 64 KiB binary block plus six-byte
tail, negative-offset rejection, release/post-release rejection, a second
panel's cancellation and continued denial of an unselected sibling. The
external runner independently compares all 65,542 output bytes and the sibling,
and checks that the canceled target is absent. Existing-target and symlink
refusal, nonsequential/overflow ranges, malformed/oversized data, EINTR and
partial I/O errors have native filesystem/helper smoke coverage; the existing
target panel is deliberately not exercised by this real-window gate.
A separate real-window Sandbox owner-close gate closes `main` first, then
targets the secondary's independent executor with a retained write grant:
settled writes, trusted Java close, owner retirement, successful tracked cleanup
and preserved output bytes. The close completion receipt follows the owner's
`windowClosed` and precedes termination; both window contexts are absent at
termination. Focused real evidence covers cleanup completed before an empty
shutdown drain. The alternative pending-one drain ordering has synthetic gate
coverage only. Neither proves executor deallocation timing, in-flight writes
or a final DOM patch after destruction.
A separate real-window Sandbox write-isolation gate keeps the grant active
across two independently captured public raw `NativeBridge` contexts. Both
windows allow write/release commands. The selecting secondary first writes
64 KiB; main then submits the same privately held token with the valid next
offset and six different bytes, followed by release. Both operations return
`grant-unavailable` with no write progress and no owner diagnostic identity.
The owner then appends its expected six-byte tail at the unchanged offset and
releases successfully. An external verifier checks every output byte and the
unchanged, still-denied sibling. This proves logical window/API grant isolation
under a real process-level Sandbox policy, not OS isolation between windows or
mutually untrusted Java code inside one JVM. Reusing an owner-bound typed grant
from another view would still use the owner context and is not this probe.

A separate Sandbox settled-write sidecar-exit gate has focused real-window
coverage through the standard production bundle and opaque Flow IPC. It retains
the main and owner windows while the owning JVM is killed after its complete
65,542-byte write and visible Java
result. It requires the unreleased old grant to be invalidated before recovery
is scheduled; its descriptor closes successfully and acquired security scope
stops before acceptance. Asynchronous close may finish after the new JVM is
ready; that late-close ordering has synthetic regression coverage only. Both
native windows, WebViews and scopes survive, but Java view state resets. A
second externally operated Save panel selects a different new target;
a fresh generation-bound grant writes from offset zero, releases and rejects a
post-release write. The external runner verifies both complete files and their
unchanged siblings. Per-generation opaque request/push and command correlation
prevents reset counters or reused IDs from mixing old and new evidence. This
targets cleanup plus fresh authorization, not restoration/replay of the old token,
in-flight write recovery, OS-wide dynamic-extension revocation or fsync durability.
A separate Sandbox write-token replay gate has focused real-window coverage
through the standard production bundle and opaque Flow IPC. A private, bounded
record in a unique owner-only app-container directory carries the old token and
producer PID to the recovered JVM. Public raw `NativeBridge` calls then submit that same token
for a valid next-offset write and release before fresh authorization. The
gate verifies permission-allowed `grant-unavailable` outcomes without write
progress or release effect, plus host denial receipts matching the old grant's
`sidecar-exit` diagnostic identity in the retained owner scope. A different fresh
token writes and releases normally; the runner independently compares
both complete files and unchanged siblings. The handoff is acceptance-only,
not a product persistence API or a direct active-map generation-mismatch probe.
Fourteen generated AppKit stages and two externally operated native Save panels
cover this settled-write boundary, not physical/AT input or in-flight writes.
A separate normal AppKit Quit Sandbox write-grant gate has focused real-window
coverage through the standard production bundle and opaque Flow IPC. Both
windows and the settled 65,542-byte owner grant remain active until external
native Quit. The gate verifies
`host-shutdown` invalidation, one pending tracked close, successful descriptor
closure and security-scope stop, then a zero-pending/zero-failure drain before
`applicationWillTerminate` and graceful sidecar shutdown. The external runner
checks the complete output and unchanged sibling after exit. Preparation has
balanced requests and verified push delivery; later UI traffic may end at a
causal prefix, but no opaque envelope may cross the invalidation fence. Truncated
request/retry edge cases have synthetic coverage, not real in-flight file-write
evidence. This is distinct from owner-window close and does not establish
blocked/in-flight I/O or fsync durability. External Computer Use operates the
Save panel and test-only standard AppKit Quit menu; this is not Dock-origin Quit
or a production menu API.
Broader write-grant owner-close/Quit/replacement and in-flight races, forced I/O errors under Sandbox,
long/concurrent transfers, Save/Replace, rollback/commit, durable progress,
retry/resume and bookmark persistence remain open. Generated events are not
physical keyboard/mouse, Finder or assistive-technology evidence.

A separate real-window sidecar-exit gate selects an unreleased grant, kills the
owning JVM, and correlates old-generation invalidation, handle closure and
security-scope stop with the actual unexpected exit. Both windows, WKWebViews
and scopes are retained while a fresh JVM resets the Flow view state. A second
external native selection creates a distinct new-generation grant; its bounded
read, explicit release and subsequent rejection are verified through Java
outcomes and direct host traces. This proves cleanup and fresh access after
recovery, not survival or replay of the old grant.

A separate recovered-token replay gate carries the original selected token
across that unexpected JVM exit using a test-only, owner-readable temporary
handoff file. The file is not a framework persistence or grant-recovery API.
After recovery, listeners capture a new public raw `NativeBridge` context in
the retained original owner scope and submit the old token for both read and
release. Both commands pass manifest authorization but return
`grant-unavailable`; host diagnostics correlate them with the original grant's
`sidecar-exit` retirement record. A second external native selection creates a
privately verified distinct token, which can read the expected bytes and release
normally. The test checks handoff and actual argument equality privately; the
trace contains no token or handoff contents. This proves rejection of a known
revoked old-generation token, not continued access to the old grant or a direct
probe of the active-map `generation-mismatch` branch.

Normal AppKit application Quit has a separate real-window gate with one active
grant and both owner windows still open. External native UI invokes a test-only
menu item targeting the standard `NSApplication.terminate` action. The host
revokes the grant, closes its handle and stops acquired security-scoped access
before `applicationWillTerminate` and graceful sidecar shutdown. A host-owned
cleanup tracker also retains pending close work from already-retired window
executors. `applicationShouldTerminate` defers termination while its main run
loop drains that work, with a two-second bound and explicit pending/failure
diagnostics rather than claiming success after timeout. Post-shutdown AppKit
window teardown is distinct from an owner close before Quit. The test menu is
not a production application-menu capability or physical/Dock-origin evidence.

A separate final-window gate calls the public Java window-close API from a
client listener in the sole WKWebView. A generated AppKit preparation click
first produces a visible Java/UIDL state change; the final generated click
performs correlated title and close commands, then the real host window-close,
empty resource drain and graceful sidecar shutdown. The close request itself
may lose its browser response when its owner disappears; no final DOM patch or
focused push is claimed. Cleanup delivery and its deadline use default/modal
main-run-loop modes so a nested AppKit termination loop entered from the main
dispatch queue cannot starve them. Modal-loop smoke additionally checks pending
worker completion, repeated callbacks and timeout/late-completion semantics.
These worker resources are test doubles, not blocked filesystem I/O evidence.

Normal Quit now waits for actual JVM exit before the final AppKit termination
callback, after resource drain. Shutdown/EOF gets one second of grace, followed
by TERM and one further second before KILL; the host then waits up to two
seconds for the actual process callback. Missing confirmation or signal failure
cancels Quit and retains fenced process ownership for a later retry. Repeated
pending Quit requests do not extend the current attempt. A failed Quit does not
restore the drained runtime. The final callback observes the original JVM
absent, not merely a signal request.

A separate standard-bundle gate exercises cooperative and blocked shutdown
hooks through the same public final-window close. Before close, generated
AppKit input reaches a Java listener, UIDL and a consumed `UI.access` push; the
final trusted DOM event produces a correlated title receipt and native close.
Private test-app markers bind startup, launcher return and hook entry to one
run/PID. Launcher return proves runtime termination, not whether shutdown or
EOF was consumed first. The blocked hook requires production TERM/KILL and
actual signal exit; an external observer confirms both host and JVM absent
before runner cleanup. This gate has an empty resource drain and does not
replace live-grant cleanup evidence. Retry/late-callback failures have
deterministic scheduler-double coverage, not real OS signal-error injection.
The generation-bound nonblocking writer removes the full-stdin-pipe exception;
it does not repair a host loop blocked by unrelated work. A separate focused
backpressure gate prepares real Flow state, suspends only its exact JVM,
confirms the stopped OS state and sends sixteen finite ordinary browser GETs.
Actual EAGAIN must precede a responsive main-loop heartbeat and the product
Cmd-Q path, then actual TERM/KILL exit and independent host/JVM absence before
cleanup. The request burst is browser-programmatic; preparation/menu input is
generated AppKit input. Whole-JVM suspension is not reader-only fault isolation,
application cleanup or unattended recovery evidence. Fatal startup, planned
replacement and durable application state remain separate validation surfaces.

The live-token cross-window gate exercises public raw `NativeBridge` commands
from two independently captured contexts. With read/release permission allowed
in both scopes, main's use of the selecting secondary's token returns the same
`grant-unavailable` response as an unknown token. The original owner can then
read the expected bytes and release once, proving the foreign release did not
revoke its grant. The test application checks actual token equality privately;
the trace correlates scoped commands/results, id-free foreign denial receipts
and the one owner's lifecycle without disclosing the token. One external native
panel selection, seven generated AppKit clicks, visible Java outcomes and
balanced opaque transport provide focused evidence. This raw-command probe is
distinct from the typed grant facade, which intentionally retains its captured
owner context. It is not a security boundary between mutually hostile Java code
sharing the same JVM and owner-context objects.

Planned sidecar retirement/replacement and an in-flight read/release race are
implemented or fail closed in the host but are not directly injected by these
gates. A separate unsupported-version fatal gate covers an idle read grant;
other fatal causes, forced process termination and blocked file I/O beyond the
drain deadline remain outside that and the normal-Quit evidence. The current
ad-hoc host has no App Sandbox entitlements, so
`securityScoped=true` is only the return value of the AppKit access call and not
proof of Powerbox enforcement in those unsandboxed read gates. Persistent bookmarks, grant
recovery, broader transfer progress/cancel/retry/resume and Finder-origin file
drops remain open.

An independent acceptance-only App Sandbox clone has focused selected-file
read coverage under an exact three-entitlement policy: App Sandbox,
user-selected read-only, and outbound network client access required by WebKit
startup. The unchanged local JVM is copied into that temporary app, not
packaged as a product distribution feature. The external runner creates a
target and unselected sibling outside the app/container. After standard Flow
startup, both actual host `open()` calls must fail with `EPERM`; an externally
operated native panel then authorizes only the target. The same public typed
listeners receive `SELECTED`, content-checked `DATA`, `RELEASED` and
post-release `UNAVAILABLE`; sibling access remains `EPERM` in the same host
process. Four generated AppKit clicks, one retained Flow scope/JVM, correlated
commands/results and UIDL/push, handle/scope cleanup and graceful shutdown are
required. This verifies a real policy boundary, not just signature metadata or
`securityScoped=true`, but does not change the default host policy or establish
physical/Finder/AT input, persistent authorization, write grants or recovery.
Release invalidates the opaque token and owned handle; the gate does not assert
immediate raw-path revocation of all OS-issued dynamic extensions.

Capability work that leaves a Flow callback uses an explicit captured context:

```java
Button open = new Button("Open", event -> {
    DesktopCapabilityContext capabilities = Desktop.current()
            .captureCapabilityContext();
    UI ui = event.getSource().getUI().orElseThrow();
    executor.execute(() -> {
        DesktopFileDialogResult result = capabilities.fileDialog()
                .openFileResult("Choose a file");
        ui.access(() -> render(result));
    });
});
```

`captureCapabilityContext()` must run while a Flow listener or `UI.access`
callback owns the current Vaadin session. Its immutable facade exposes only the
capability APIs (`notifications`, `clipboard`, `fileDialog`, `fileTransfer`,
`windows`, `nativeBridge` and `setWindowTitle`); it holds no Flow `UI` or
`VaadinSession` reference and exposes no application-visible window/session/UI
scope identity. Applications may pass that facade to work on any executor.
Scope is not propagated implicitly by `ThreadLocal`, executor choice or thread
inheritance.

The Flow bridge binds the facade to the exact opaque owner scope captured in the
callback. Pending commands continue to use the existing correlated settlement
path. After that owner closes, or after the same public window id reopens with a
new opaque UUID, an old facade returns the existing typed
`owner-window-closed` failure without emitting or retargeting a command;
`openFileResult` exposes this as `INVALIDATED`. A captured facade is
process-local and is never carried into a replacement JVM. Host generation
fencing separately invalidates old-generation operations and drops their stale
completions before they can reach the next sidecar. Within one bridge process,
unregistering a secondary session mapping does not erase its exact closed-scope
tombstone; the same tuple cannot reactivate, while the same public id with a new
opaque UUID remains a distinct, admissible scope.

An unbound `Desktop.current()` capability call with no current Vaadin session
keeps the legacy main fallback only while the main/default scope is the sole
live scope. With multiple live scopes it fails closed with an actionable Java
`ERROR` and emits no command. A tombstoned main/default scope remains the
special case that returns `owner-window-closed`. Bridges without scoped Flow
semantics retain their existing behavior through the default capture SPI.
Compatibility HTTP uses an explicit single-window bridge: its Servlet-backed
`VaadinSession` is not looked up in the pure Flow opaque-scope registry, and
both direct and captured calls continue to target the compatibility main scope.

Native command completion is also bound to the host-local monotonic sidecar
generation that received the command. The controller rejects inbound native
commands until the current initial, recovered or replacement process has sent
valid `ready`. For an accepted command, comparing its expected generation with
the active generation and admitting the result to that generation's bounded
stdin writer are one atomic operation. Admission is not delivery: the per-JVM
nonblocking FIFO alone owns its descriptor, and only a full frame write emits
`written`. A late completion from a retired generation therefore cannot enter a
replacement JVM, even when the new Java bridge reuses the same command id and
window/session/UI scope.

Once a sidecar generation is confirmed retired, each retained window invalidates
its native-operation executor epoch. Notification callbacks check that epoch
before scheduling an operating-system side effect. File dialog and transfer
panels share one active AppKit panel token; invalidation clears that token before
calling `cancel` and `orderOut`, so the cancellation callback cannot reclaim or
complete a newer panel. Planned replacement revokes these operations immediately
after its process-termination request succeeds, and termination handling
remains the fallback for every retirement path. Token- or generation-stale completions are
traced and dropped instead of becoming a normal `capabilityResult`.

For notifications, these fences prevent stale callbacks from scheduling a new
request or settling into a replacement owner. They do not establish atomic
revocation of a request already submitted to the OS: immediate delivery can
race with invalidation and removal. An add callback now checks both the active
operation and its registered identifier before publishing a result. A stale
callback makes a second best-effort pending/delivered removal using only its
captured identifier, then drops the result without touching a newer operation.
The deterministic completion-helper smoke covers this routing, not actual OS
removal or a host that has already exited. Neither `INVALIDATED` nor an unknown
Java wait result proves absence of a notification side effect. Notifications
already settled successfully are not revoked by later owner invalidation.
Reliable already-submitted notification revocation and broader presentation/
delivery semantics remain open product boundaries. The independent focused
foreground gate above covers retained OS delivery only, not these races.

Capability-manifest fallback is selected by the product command rather than by
the application runtime. `dev`, `dev-vite` and `dev-compat` explicitly select
`allow-all-dev`; `package` selects `manifest-enforced`. The macOS product
builder also defaults to `manifest-enforced` when called without a fallback
argument. If the application has no source `desktop-capabilities.json`, the
builder still stages a valid manifest with an empty `windows` grant set. For an
explicit product manifest, the builder strictly parses JSON once into a
builder-private canonical snapshot that remains immutable for staging, validates
`permissionMode` from that snapshot and stages only the snapshot. Invalid JSON,
XML property lists, a missing or unsupported mode, and explicit `allow-all-dev`
or `trace-only-dry-run` are rejected before the existing staged application is
replaced. Duplicate keys collapse to the JSON parser's single runtime-effective
value; an earlier permissive value and final `manifest-enforced` value therefore
stage one enforced key with no permissive token. Development staging continues
to accept explicit `allow-all-dev` and `trace-only-dry-run` manifests. The staged
resource remains mandatory in `VatoriHost.json`, so removing it from the built
bundle fails host startup. The host parses the staged capability manifest once
into `HostConfig` before creating `NSApplication`, launching the sidecar or
opening a window. Parser-invalid JSON, schema, permission modes or window entries
introduced after staging emit stable operational diagnostics and terminate with
status `2`; the parsed snapshot is then reused by the live command executor.
Its command-level `manifest-invalid` denial remains a defensive fail-closed
branch, not a reachable standard startup state.

A focused real-WKWebView gate exercises window-scoped manifest enforcement with
the same public `window.setTitle` call in two isolated sessions. The `main`
scope is allowed and changes its native title; `permission-secondary` is denied,
returns the complete permission audit payload to Java and leaves its native
title unchanged. Two generated AppKit primary clicks reach client-originated
Flow Button listeners and UIDL responses. Full-detail trace inspection
dynamically matches both command/result correlation ids and exact
window/session/UI scopes and validates the allow/deny payloads. Only navigation
and opening the secondary window are browser-programmatic setup, and raw
envelope counts are not part of the contract.

The source-missing production default has a separate focused gate. A
test-support-only omission hook removes the fixture manifest input before the
real product builder stages the empty `manifest-enforced` grant set. In the
resulting standard production bundle, one generated AppKit primary click is a
trusted client event, reaches the Java listener and produces a UIDL response.
The complete denied permission payload reaches Java, and the native title stays
unchanged. Full trace inspection requires the exact manifest event before the
unique dynamically correlated and exactly scoped `window.setTitle`
command/result pair. Navigation is browser-programmatic setup, focused push is
false and shutdown is graceful.

Explicit permissive-manifest package rejection is covered by
`test-support/native-host-macos-acceptance/bin/run-flow-capability-package-policy`.
It invokes the real product builder with separate explicit `allow-all-dev` and
`trace-only-dry-run` manifests. Each build-time negative probe must exit with
status `2`, emit
`Product capability manifest must use permissionMode=manifest-enforced; found: <mode>`,
and retain byte identity for a pre-existing staged enforced manifest and marker.
It then builds a real production app, replaces only the staged manifest with
malformed JSON, an unsupported schema, an unsupported mode and an invalid
window entry in turn, and runs the production host directly for each case. Each
runtime probe must exit `2` with its stable reason and pass the strict
`capabilityManifest` followed by `capabilityManifestInvalid` trace gate, with no
host `launch`/`ready`, sidecar, Java or window event. The original staged
manifest is restored before the runner executes the complete source-missing
default-deny gate. The negative probes therefore do not launch a WKWebView; real-WKWebView,
trusted AppKit click, Java, UIDL, trace and graceful-shutdown evidence comes from
the delegated positive gate.

The broader builder contract belongs to `bash bin/cli-contract-smoke`, not to
those two focused probes. That smoke requires explicit enforced and duplicate-key
documents to stage as canonical JSON with one runtime-effective
`permissionMode`, while preserving `schemaVersion` and `windows` semantics. It
also requires invalid JSON, XML property lists, missing mode and unknown mode to
fail before destructive staging, and development staging to continue accepting
both explicit permissive modes.

File-dialog terminal outcomes have a separate focused real-WKWebView gate. A
generated trusted AppKit primary click reaches a Flow listener, captures the
main capability context and passes it to a background virtual thread whose
`VaadinSession.getCurrent()` is absent (`backgroundSessionAbsent=true`). The
captured facade invokes public `openFileResult` and opens one real attached,
visible `NSOpenPanel`. Only after that unique panel has been observed does a
second generated trusted click issue another invocation; it returns public
`ERROR` with `File dialog already in progress`, while the first panel remains
the only panel. Test-support then calls `NSOpenPanel.cancel(nil)` to exercise the
panel's normal AppKit cancellation completion. This is a deterministic
test-support panel action, not a physical user click on the Cancel button. The
first call applies its public `CANCELED` result through `UI.access`; the exact
main scope must receive and acknowledge the resulting automatic opaque push.

A dedicated opaque-UUID secondary session omits `fileDialog.openFile` from its
manifest grant. Its generated trusted click captures that exact secondary
context and passes it to another background thread with
`backgroundSessionAbsent=true`. The command remains secondary-scoped, returns
public `DENIED` with the complete permission audit payload and creates no panel;
its `UI.access` update is delivered through the exact secondary push scope and
receives a scope-matching acknowledgement. The existing owner-window-close gate
separately requires the waiting public `openFileResult` call to observe
`INVALIDATED` with
`cancelReason=owner-window-closed`. Together with the Java API contract smoke in
`bash bin/flow-ipc-scope-smoke`, these gates cover classification precedence and
legacy `Optional` compatibility without adding outcome fields to the wire
protocol.

Cross-generation native-capability isolation has its own focused real-WKWebView
gate. An independent public Flow route opens a real `NSOpenPanel` with distinct
titles in two sidecar generations. Both invocations start with generated trusted
AppKit primary clicks. After the first panel is visible, test-support kills the
first JVM without canceling the panel; product retirement must hide and cancel
it, trace and drop its stale completion, retain the native window and WKWebView,
and recover a fresh Flow document. The second JVM intentionally reuses
`desktop-command-1` and the same window/session/UI scope. Test-support cancels
only that second real panel, whose canceled result must reach the Java listener
and visible UIDL state before a balanced quiet drain and graceful shutdown. Only
the two route navigations are browser-programmatic, focused push is false, and
this generated AppKit input evidence does not claim physical-input coverage.

Owner-window close now has a separate focused real-WKWebView contract. A public
Flow listener in an isolated secondary session captures its exact capability
context and passes it to a background virtual thread. That thread has no current
Vaadin session (`backgroundSessionAbsent=true`) yet opens a real `NSOpenPanel`
through the captured facade.

Closing that owner from the retained main window invalidates the secondary
executor, clears its active panel token before `cancel` and `orderOut`, drops
the resulting stale AppKit completion and makes the waiting public
`openFileResult` call return `INVALIDATED` from a correlated typed failure:
`canceled=true`,
`cancelReason=owner-window-closed` and `operationInvalidated=true`. Only after
that settlement does the host send the exact secondary `windowClosed` scope and
release its window, WebView and executor. Java tombstones the scope as soon as
the close frame is parsed, then removes push and request state and synchronously
drives UI detach plus Vaadin `SessionDestroy` to the session's `CLOSED` state
before unregistering the native capability session-to-scope mapping. The same
sidecar process, main window and main WebView remain usable after the close.

The focused gate uses three generated AppKit primary-pointer events for the
secondary dialog invocation, main-window close action and retained-main health
probe. They prove AppKit-to-WKWebView delivery, trusted browser receipt and
client-originated Flow listeners, but do not claim physical pointing-device
hardware, touch or assistive-technology coverage. Route navigation and opening
the secondary are browser-programmatic setup, and no test cancellation hook is
used for the panel.

Same-public-id capability-context reincarnation has a separate focused
real-WKWebView gate. Incarnation A captures its exact host-generated opaque UUID
scope and uses it from a background virtual thread to set the A native title.
After a trusted main-window close action releases A, the host opens incarnation
B with the same public window id but a different UUID, native `NSWindow` and
`WKWebView`. B invokes A's retired facade before and after its own fresh title
command. After a second trusted close, incarnation C receives a third opaque
UUID, `NSWindow` and `WKWebView`; C invokes both A's and B's retired facades
before and after its own fresh title command. All six retired calls return the
typed `owner-window-closed` result locally, emit no command and cannot produce a
stale title. Scope-exact automatic pushes for A, B and C are acknowledged and
the main window, main WebView and sidecar process remain unchanged through a
balanced drain.

Five generated AppKit primary-pointer events drive A capture, A close, the B
old/fresh/old probe, B close and the C two-old/fresh/two-old probe; all must be
observed as trusted client input. Route navigation plus the three opens are
browser-programmatic setup. This is a focused two-cycle A→B→C contract, not
long-running or unbounded reincarnation, higher fan-out, recovery/replacement
or physical-input parity.

A complementary production-host gate copies the packaged standard Vaadin
production bundle and capability manifest into an isolated app, then stages a
test-support fake sidecar as that app's Java JAR while retaining the production
Swift composition and real AppKit windows and WKWebViews.
The fake sidecar closes same-public-id incarnations A and B, then sends exact
retired commands and late HTTP response/push envelopes back through the normal
`main.swift` handler. The host retains both full scopes simultaneously, returns
the correlated typed `owner-window-closed` result for both commands and drops
both scopes' late transport. Incarnation C receives a fresh opaque UUID and
completes a live title command plus browser request/response-credit round trip
before graceful shutdown. That round trip proves scope-exact WKWebView credit
and host chunk/end ingress, not complete browser body consumption or a Flow UI
request. This closes the production composition wiring gap
for two process-lifetime tombstones; because the sidecar is fake, the gate does
not establish Flow UI/listener behavior, physical user close input, unbounded
churn or recovery/replacement. The real Flow A→B→C gate above supplies the
complementary Java listener, UIDL and visible-state evidence.

Clipboard terminal outcomes have a complementary focused real-WKWebView gate.
Generated trusted AppKit clicks invoke public Flow listeners for a main-scope
no-text read, an explicit empty write/read round trip and a secondary-scope
denied write/read pair. The fixture snapshots pasteboard state only in memory
and uses the general pasteboard `changeCount` as best-effort observed-contention
protection: each restore attempt proceeds only while the latest count still
matches the count owned by test-support, and a detected mismatch aborts without
another pasteboard mutation. Failed snapshot-restoration writes may be retried
a bounded number of times while that ownership observation remains valid,
including from the app termination hook. Clipboard content never enters DOM
status or acceptance
trace. The inspector requires
five exact scoped command/result correlations, distinct `NO_TEXT`, `WRITTEN`,
empty `TEXT` and `DENIED` classifications, no denied pasteboard side effect,
redacted command/result traces, normal UIDL completion, no recovery and
graceful shutdown. A core contract smoke separately covers `INVALIDATED`,
malformed/incomplete `ERROR` shapes and legacy delegation.

AppKit exposes no compare-and-swap operation for pasteboard replacement, so the
`changeCount` check and subsequent clear/write still have a narrow race with an
external writer. This is best-effort protection for observed contention, not an
absolute no-overwrite guarantee. The in-memory snapshot also cannot be restored
after `SIGKILL`, a hard host crash or machine termination; graceful app
termination and the launch helper's bounded quit window cover only paths where
the observer hook can run.

This is focused evidence for file-dialog and Clipboard result normalization and
one window capability lifecycle, not a complete native permission model or
product parity.
Product source JSON and permission-mode failures are rejected at build time.
Parser-invalid staged capability content is startup-fatal before application,
sidecar and window creation. This does not prove signature-backed protection
against a post-build replacement that is still a syntactically valid manifest.
Beyond the focused sandbox-selected read and create-new write gates, persistent
selected-file grants, overwrite/transaction and write-lifecycle recovery boundaries, broader
Clipboard formats and pasteboard contention, macOS authorization states,
broader asynchronous progress/cancellation/retry/resume contracts, physical
input hardware, touch and assistive-technology interoperability remain open.
Sandbox enforcement evidence requires successful JVM and WebKit startup
under the actual policy before testing external-file denial and panel-selected
access; the selected-read gate checks that ordering. The acceptance-only initial-navigation watchdog bounds failures before
`didFinish`; its negative gate does not establish Flow startup or file access.

## Window Scope

`Desktop.current().windows().open(DesktopWindow)` creates a new native window for
a registered relative route. Each WKWebView has immutable `windowId`,
`sessionId` and `uiId`, with an isolated Vaadin session and UI. The main scope is
stable. For every secondary window the host generates an opaque UUID-backed
session id, currently encoded as `flow-ipc-window:<UUID>`; it is independent of
the public window id and cannot be selected or derived by application code.
Each context also retains the route-specific bundle entry URL used to create
that document.

The single stdin reader parses envelopes and dispatches request/push work to one
ordered handler lane per window. Messages for the same window retain arrival
order, while unrelated windows can continue concurrently. `windowClosed`
synchronously tombstones that exact native-command scope before its Java
teardown is queued behind earlier same-window work. The teardown removes push
and exchange state, destroys the Vaadin session synchronously and retires the
lane. A reopened public window is a new host context with a new opaque session
identity. The reader retains a process-lifetime tombstone for the complete old
scope and silently drops all of its later non-shutdown frames before capability
correlation or handler lookup, including duplicate close and unknown/future
types. Frames admitted before close still run; the new UUID remains admissible
and follows the old teardown in the shared public-window lane. Late traffic can
therefore neither recreate the retired Vaadin session, poison a fresh
capability correlation nor be retargeted to the new incarnation. Compatibility
HTTP stays single-window.

The macOS host independently retains every complete retired native scope for
its own process lifetime rather than remembering only the latest incarnation.
It returns the typed `owner-window-closed` result for a command carrying any
exact retired identity and drops its late opaque request/push transport. A new
opaque UUID under the same public window id is not retired and can open
normally. In addition to the registry smoke, the production-host fake-sidecar
gate drives two retired identities and a fresh third identity through the
actual `main.swift` handler, real AppKit/WKWebView composition and standard
production bundle. That gate proves host wiring, not Flow UI/listener parity.

Creation of a scoped session context is an acquire/publish transaction under
the session-owner monitor. The Vaadin session and push/native registrations
must all succeed before the two owner maps are published. A failure
compare-removes only that attempt's published entries and exact resources,
closes the matching registry session, and rolls back native registration last
so session destruction retains its capability mapping. Registration rollback
does not use the owner-teardown operation: it preserves a concurrent close
tombstone while leaving an otherwise live default lifecycle retryable. This
covers the close race where an A request was admitted before close but reaches
its first session access only after the reader has tombstoned A: A rolls back
completely, then B/new UUID can acquire the same public window lane without
stale push or Vaadin state.

## Sidecar Lifecycle

The sidecar has one stdin reader and one synchronized stdout writer. `shutdown`,
final window close and stdin EOF stop the loop. Pending native commands complete
with an error when the bridge closes.

The host's stdin writer has a 16 MiB retained-frame and 256-frame bound and a
ten-second no-write-progress deadline. It preserves opaque frame order without
blocking the AppKit thread on pipe capacity. Overflow, EPIPE/other write errors
or stalled writes fail the channel and enter fatal retirement; these are not
silently dropped frames or automatic application-level retries. If the process
has already been observed as exited when the queued write-failure callback runs,
the host defers to the actual-exit handler instead of converting that exit into
a fatal shutdown. This does not promise recovery for every simultaneous OS
exit/write-error race. An already
partial frame is never followed by an inserted shutdown frame: Quit abandons
the remainder and closes the channel, then relies on the same actual-exit
TERM/KILL contract. New generations get new writers; no pending byte is carried
across JVMs. The bound concerns queued stdin data, not the entire host heap.

When the sidecar channel closes, each existing document's opaque request bridge
fails every pending request and becomes permanently disconnected. Later fetch,
XHR or request-stream activity from that document is rejected locally instead
of being forwarded to a replacement process. Its push bridge likewise ignores
late inbound delivery and refuses reconnect, acknowledgement and subscription
messages. The host does not forward browser envelopes while initial `ready`,
unexpected recovery or planned replacement is pending. After a replacement
sends valid `ready`, retained WKWebViews reload and the official Flow client
establishes fresh sessions. An in-flight request is therefore failed, never
reattached or replayed across sidecar generations; any retry is a new
browser/application request. Unacknowledged push delivery follows the same
generation boundary: the old process-local queue is discarded, and the fresh
session starts a new transport sequence rather than consuming old UIDL.

The same readiness and generation boundary applies to native commands: no
command is accepted while the controller is awaiting initial, recovery or
replacement `ready`, and a result captured for an older generation cannot be
written to the new sidecar channel.

Window topology also survives sidecar lifecycle boundaries. If `main` has
already closed while one or more secondary windows remain, a newly ready
recovered, replaced or rollback sidecar does not recreate the main window. At
the lifecycle-ready boundary the host replays the normal `windowClosed`
envelope for the exact retired main scope, so the fresh Java bridge tombstones
its default scope before application work resumes. Each retained secondary
keeps its native window, WKWebView and opaque scope and reloads its own immutable
route-specific entry URL, rather than falling back to the bundle root. The
official Flow client then establishes fresh Vaadin session/UI state for that
route. A no-current-session background desktop call therefore receives the
typed `owner-window-closed` result locally and cannot emit a ghost main-scoped
host command, while a command from the recovered secondary remains valid.

Bounded process recovery retains native windows but recreates Vaadin session and
UI state. Spring state is also recreated when the optional adapter is present.
Unexpected exits consume a consecutive-failure budget with exponential
backoff. Recovery `ready` starts a manifest-defined stable-run deadline; the
budget resets only when the same process remains ready for the complete
duration. A failure before that deadline consumes the next attempt, while a
failure after reset starts again at attempt one.
An unready recovery JVM that ignores SIGTERM is escalated to SIGKILL after a
one-second grace period, fenced by the exact retained process/PID/generation.
The next attempt waits for confirmed process exit. A focused standard-bundle
gate covers a ready first JVM, a never-ready second JVM blocked in its shutdown
hook, and a ready third JVM. The same native window, WKWebView and opaque scope
survive; only the third JVM's valid `ready` reloads the document, and new
generated AppKit clicks reach fresh Java listeners and acknowledged pushes.
This does not preserve process-local Flow state, guarantee application cleanup,
or cover planned replacement. Ordinary host Quit and fatal host shutdown have
the separate actual-exit contract described above.
`dev-vite` additionally uses a planned replacement path after a successful Java
rebuild. Planned replacement does not consume the unexpected-exit restart
budget. Replacement is a CLI/host transaction: the host reports candidate
readiness or failure, the CLI owns stable-JAR restoration, and the host reports
rollback readiness before development continues. A failed rollback is fatal
rather than consuming the unexpected-exit restart budget.
The current `requestReplacement` entry explicitly rejects a standard production
bundle with `unsupported-mode`; its only allowed webapp source is development.
Thus development replacement evidence does not establish a product-runtime
planned-replacement API. A product trigger, authorization, candidate validation
and rollback contract remain open. The low-level recovery and replacement
retirement paths now share a one-second TERM-to-KILL deadline bound to the
retained Process/PID/generation. The first request owns the deadline and reason,
including mixed requests; actual exit cancels it, and host shutdown takes over
ownership. Real owned non-Java child tests cover cooperative and ignored-TERM
exit, early exit/new-generation protection and shutdown contention. These are
not controller, JVM or real-WKWebView replacement evidence. A failed KILL reports
a fatal retirement failure immediately; absence of the actual exit callback two
seconds after the first retirement request reports a fatal confirmation timeout.
Both retain ownership for the existing fenced host-shutdown path, rather than
inventing exit or allowing a new generation. Actual exit or shutdown takeover
invalidates the old timeout. A test-only callback scheduling double covers real
child exit with delayed callback, one failure notification and late-callback
shutdown settlement for both retirement kinds. It is not OS signal-error or
real-WKWebView fault evidence; timers depend on main-loop scheduling and cannot
guarantee process exit. Product replacement failure settlement remains open.
This is distinct from automatic
unexpected-exit recovery of the existing bundled sidecar.
If the initial sidecar exits before valid `ready`, the host exits nonzero before
presenting a window. A manifest-defined initial `ready` deadline also retires a
sidecar that remains alive without completing the handshake. Fatal runtime
shutdown fences work, drains native resources, sends best-effort shutdown/EOF,
then escalates TERM/KILL on the same bounded schedule as normal Quit. Only
actual child exit permits host status `1`; signal success is not confirmation.
Failure retains the fenced host and child ownership for a later Quit retry,
which still preserves status `1`. An initial-ready-timeout gate uses isolated
cooperative and blocked-hook JVMs, and independently checks both processes are
absent before any cleanup. It deliberately creates no window and is not Flow
UI evidence or coverage of every fatal cause. A separate focused real-window
gate retains an idle selected read grant across an unsupported-version fatal
fault. External native Open-panel selection reaches the public Java `SELECTED`
result before the fault; the grant is invalidated, its descriptor closed and
acquired security scope stopped before the successful drain and actual
cooperative JVM exit. Host completion records status `1`, and an external
verifier confirms both original processes absent before cleanup. This uses
browser-programmatic route setup, generated AppKit preparation clicks and
Computer Use panel selection; it does not establish physical input, App Sandbox,
in-flight file I/O, forced child termination or other fatal causes.
Exhausted recovery after startup
is fatal. CLI development commands preserve that status and the host logs.

## Product Capability Matrix

This is the canonical capability inventory, not a declaration of full parity.
The audit reference is `ef1a26e` (Vaadin version selected by the root POM:
25.1.5). Later implementation changes require affected evidence to be refreshed.
Detailed historical coverage below remains useful, but does not override the
status and boundaries here. The matrix groups shared contracts, not individual
components or every permutation of their behavior.

Implementation: **I** = implemented for the stated bounded contract; **P** =
partial; **M** = missing product entry/contract. Verification: **H** = historical
focused coverage documented, with driver/gate located but no fresh end-to-end
certification; **L** = retained local trace inspected for a focused path, not a
fresh run of the audit revision; **S** = non-window contract/synthetic evidence;
**U** = missing required evidence; **R** = unresolved real-run regression.
Multiple codes describe different parts of a row, not interchangeable levels of
confidence. No row is certified for the proposed full baseline by this audit.
The latest completed recovery run was reported as failing before the first
click at active/key activation. Its shared log was subsequently truncated by an
interrupted rerun, so the failure remains unresolved and the present empty log
cannot independently reproduce or supersede it. Other unretained historical
runs stay H rather than being promoted from documentation alone.

Priority: **B** = proposed baseline requirement, **B/E** = bounded baseline with
explicit extensions proposed for later, **E?** = proposed deferral requiring user
approval. Gap classes **1/2/3/4** mean framework/upstream-or-application/evidence/
native-product-boundary respectively. A `3→4?` classification requires diagnosis
before changing production. An upstream-owned behavior is not automatically a
gap; an application-usage issue must be proven against upstream semantics.

Source keys: **Flow** = `vaadin-desktop-flow/.../flow/{ipc,transport}`;
**Host** = `native-hosts/macos/Sources/VaadinDesktopHost`;
**Core** = `vaadin-desktop-core/.../desktop`. **A/** denotes
`test-support/native-host-macos-acceptance/bin/`. Commands below are evidence
entry points; required arguments, manifests and OS actions are specified in
[acceptance-tools.md](acceptance-tools.md), not implied by the short names.

### Flow Shared Contracts

| ID / contract and baseline acceptance | Owner / source | Implementation | Verification / representative evidence entry | Gap and priority |
| --- | --- | --- | --- | --- |
| F01 Bundle, routes and resources: initial and lazy navigation load JS/CSS/transitive imports; one official client; no localhost UI fallback | Host `FlowIpcWebAppSchemeHandler`, Flow `StandardFlowIpcRuntime` | I | H/L: standard framework acceptance; A/run with framework flags; retained broad trace is not proof of final host exit | 3; B/E: arbitrary dependency graphs later |
| F02 Event → Java listener → UIDL and push-visible state, with correct scope/correlation | Flow `SidecarMessageLoop`, opaque HTTP adapter | I | H/S: framework route (button/value/layout/resource probes), `bin/flow-ipc-scope-smoke`, HTTP bridge smoke | 3; B: healthy round trip plus failed request, no component decoding |
| F03 Java↔JS RPC, structured values, Promise result/error conversion | Official Flow RPC through generic IPC | I | H/S: A/run-structured-rpc; inspector structured-RPC assertions | 3; B: positive and rejection/serialization cases, no custom decoder |
| F04 Lazy data identity, query, selection and refresh across recycled rows; exact-count and unknown-size providers | Official Flow data provider, generic transport | P | H/S: A/run-lazy-grid-query, A/run-lazy-grid-selected-removal, A/run-flow-lazy-multi-select-combo-box | 3, then 1/2 if reproduced; B/E: selected-item mutation and bounded repeated resets required, arbitrary combinations later |
| F05 Hierarchy, renderer lifecycle and buffered editor/Binder validation | Official Flow; framework route samples | I | H/S: framework TreeGrid, ComponentRenderer/LitRenderer and Editor/Binder assertions | 3; B: expand/collapse, callback/update, save/cancel/reject/recover; deeper trees later |
| F06 Collection/value fields and owner-bound picker/menu keyboard behavior | Official components + Host input routing | I | H/S: A/run-flow-input-popup, run-flow-menu-popup, run-flow-multi-select-combo-box, run-flow-picker-validation | 3; B/E: representative families, not every picker/menu permutation |
| F07 Overlay stack ownership, Escape/outside click, modal suppression and focus return | Official overlays + Host WKWebView | I | H/S: A/run-flow-overlay-stack (modal/modeless/mixed three-layer samples) | 3; B/E: bounded stack, not arbitrary depth |
| F08 Localized values and validation, formatting without changing Java meaning | Official Flow/Binder; app locale configuration | P | H/S: A/run-flow-localized-number, run-flow-localized-number-validation, run-flow-localized-input-popup | 3/2; B/E: en-US/de-DE value and invalid-input recovery; broader locales, RTL/currency proposed extensions |

### Real Desktop Interaction Contracts

| ID / contract and baseline acceptance | Owner / source | Implementation | Verification / representative evidence entry | Gap and priority |
| --- | --- | --- | --- | --- |
| D01 Active/key window, WKWebView responder, trusted pointer/key path and focus restoration after reload/owner change | Host `main.swift`; test-only `FlowIpcNativeInputActivation` | P | R/H: A/run-recovery-termination stalls at initial activation in latest completed attempt; generated-event paths exist elsewhere | 3→4?; B blocker. External activation may establish test preconditions but cannot prove automatic activation |
| D02 Native Edit menu, plain-text cut/copy/paste and bounded undo/redo, isolated across owners | Host `NativeApplicationMenu`, responder chain | I | H/S: A/run-flow-native-edit, run-flow-native-clipboard-edit, run-flow-native-edit-isolation/reopen/recovery | 3; B/E: plain-text baseline, rich-text/non-text/history permutations later |
| D03 IME composition and physical keyboard/pointing input do not duplicate/drop Java changes | WKWebView/system input; official Flow | P | U: generated key events are not IME or physical hardware evidence; no dedicated baseline operator receipt located | 3→4?; B: Chinese Pinyin composition/cancel/commit, pointer selection and keyboard traversal |
| D04 Accessibility across native window and web content: names, roles, values, traversal and modal focus | WKWebView accessibility + official component semantics | P | H for DOM/ARIA assertions; U for VoiceOver baseline | 3→4?; B: actual VoiceOver control/field/grid/dialog task; ARIA alone cannot close it |
| D05 Drag/drop within Flow and external Finder file ingestion with provenance, exact bytes and Java result | Official DnD/Upload + system drag session | P | H: A/run-flow-dnd; U: A/run-flow-external-file-drop has verifier but no accepted real Finder transfer established | 3→4?; B: separate internal DnD and external Finder evidence, no DOM-trust substitution |

### Continuity and Durability Contracts

| ID / contract and baseline acceptance | Owner / source | Implementation | Verification / representative evidence entry | Gap and priority |
| --- | --- | --- | --- | --- |
| R01 Push retry/ack, reconnect and gap-resync produce consistent DOM without cross-scope replay | Flow `StandardFlowIpcPushDelivery`, push bridge | I | H/S: A/run-flow-push-delivery, run-flow-push-recovery; push bridge and inspector negatives | 3; B: dropped final delivery, reconnect, resync and fresh normal push |
| R02 Multiple window/session owners, close/reopen and stale callback rejection | Flow `StandardFlowIpcSessions`, `SidecarNativeBridge`; Host retired scopes | I | H/S: A/run-flow-capability-context-reopen, run-flow-retired-scope-production-host, run-flow-main-window-close-recovery | 3; B/E: three owners and ten bounded churn cycles proposed; higher fan-out later |
| R03 Startup/ready timeout, ordinary Quit, fatal shutdown and exact process exit; failed shutdown retains fenced ownership | Host `JavaSidecar`, `FlowIpcSidecarController`, resource cleanup | I | L/S: A/run-host-ready-timeout, run-host-shutdown-termination, run-flow-file-read-grant-fatal-shutdown; process callback doubles | 3; B: cooperative/blocked child and resource drain; OS signal refusal remains separate from callback simulation |
| R04 Unexpected sidecar exit, restart budget, retained windows and fresh document/session; interrupted request fails explicitly | Host controller, Flow scope/session teardown | P | R/H/S: A/run-recovery-termination blocked before recovery; A/run-sidecar-recovery-exhaustion and run-flow-in-flight-request-recovery provide narrower historical contracts | 3→4?; B: latest changes need real three-generation regression, no promise of old Java state preservation |
| R05 Upload/Download progress, cancel, retry/Range-resume, concurrent isolation and finite endurance | Flow streaming adapters; official UploadHandler/DownloadHandler | P | H/L/S: A/run-flow-large-upload-retry, run-flow-large-multipart-upload-retry, run-flow-large-download-resume, run-flow-request-fanout, run-flow-cross-session-multipart-upload, run-sidecar-write-backpressure | 3/1; B/E: finite load profile below; transparent cross-process continuation not implemented |
| R06 Product planned replacement: authorized trigger, validated candidate, ready/failure/rollback result and bounded retirement | Host controller; product trigger still absent | M (low-level pieces exist) | S/H only for termination/development paths; current `requestReplacement` rejects standard bundle with `unsupported-mode` | 4; B: remain required, do not count dev reload or unexpected recovery as completion |

### Native Product Contracts

| ID / contract and baseline acceptance | Owner / source | Implementation | Verification / representative evidence entry | Gap and priority |
| --- | --- | --- | --- | --- |
| N01 Manifest permissions, startup fail-closed, window scope and explicit captured async capability context | Core `DesktopCapabilityContext`; Flow native bridge; Host `CapabilityManifest` | I | H/S: A/run-flow-capability-package-policy/default-deny/permission/context-reopen/generation-recovery; CLI and scope contracts | 3; B: allowed/denied/malformed/stale owner, including no ambient background-context borrowing |
| N02 Public window create/title/close and last-window lifetime, owner removal without affecting siblings | Core `DesktopWindows`; Host window owner | I | H/L/S: A/run-flow-last-window-close, run-flow-main-window-close-recovery, run-flow-capability-owner-window-close | 3; B/E: existing window contract; extra window affordances later |
| N03 Notification authorization/result correlation and foreground system presentation | Core `DesktopNotificationResult`; Host notification adapters | P | L/S: A/run-flow-notification-outcomes and run-flow-notification-delivery; retained trace explicitly has `bannerVisibilityClaimed=false` | 3/4; B: consent branches plus separately observed banner under eligible settings; never promise OS-guaranteed delivery |
| N04 Sandbox-safe selected read grant, bounded range read/release, owner isolation and lifecycle revocation | Core `DesktopFileReadGrant`; Host executor/security scope | I | L/H/S: A/run-flow-file-read-grant-sandbox/range/isolation/replay/fatal-shutdown; actual selected-read/sibling-denial evidence | 3; B: in-flight invalidation and replacement coverage not implied by idle-grant evidence |
| N05 Sandbox-safe create-new write, chunk progress/cancel/release, stale token and owner isolation | Core write grant API; Host `NativeFileWriteIO` | I | L/H/S: A/run-flow-file-write-grant-sandbox and cancel/isolation/owner-close/sidecar-exit/host-shutdown/replay variants | 3; B: in-flight races and current shutdown semantics need aligned evidence; no overwrite/atomic-transaction promise |
| N06 Persistent file authorization, bookmarks, overwrite/transaction and durable cross-process resume | Native file product contract | M/P (transient grants only) | U: no complete public contract or end-to-end gate located | 4; E?: explicitly defer only with approval; these remain in long-term original scope |
| N07 Async native operation lifecycle: context correlation, progress where meaningful, cancel/retry and terminal invalidation | Core APIs, Flow bridge, Host resource tracker | P | H/S across notification/file/owner-close tests, not a uniform all-capabilities continuation contract | 4/3; B/E: define per operation; reject unsupported resume rather than claim generic support |
| N08 Plain-text clipboard read/write typed outcomes and bounded retry without owner leakage or overwriting unrelated changes during test cleanup | Core `DesktopClipboard`; Host pasteboard operations | I | H/S: A/run-flow-clipboard-outcomes; Java contracts and pasteboard fake-provider smoke; native Edit uses this surface but is D02 | 3; B/E: plain text with real pasteboard contention case; rich formats deferred only by approval |
| N09 Native file-panel selection/cancel/deny/error and in-flight owner invalidation, without treating a path as persistent authorization | Core `DesktopFileDialog`; Host panel owner | I | H/S: A/run-flow-file-dialog-outcomes, run-flow-capability-owner-window-close, run-flow-capability-generation-recovery | 3; B: real panel outcomes; actual authorized I/O belongs to N04/N05 |

### Proposed First Acceptance Baseline (Awaiting Approval)

The four original categories map to F01–F08, D01–D05, R01–R06 and N01–N09.
Permissions and file grants are not duplicated under transfer; transfer is R05,
authorization is N04/N05. Browser ARIA and generated input do not duplicate the
actual OS/AT evidence in D03–D05. Progress/cancel/retry/resume are operation-specific
contracts, not four mandatory verbs for every API (for example a synchronous
clipboard read).

Proposed profile, not an already-supported or already-passed guarantee:

- macOS on the available Apple Silicon host, system WKWebView, root-POM Vaadin
  version and standard production bundle/opaque IPC. Record exact OS build,
  JDK, architecture, bundle digest and source revision on every accepted run.
  Other macOS/JDK combinations require a separate compatibility claim.
- Up to three windows, ten close/reopen cycles, three sequential unexpected
  recoveries and one successful/one failed-candidate planned replacement with
  rollback. Confirm no cross-owner state, stale capability success or orphan
  process; state loss across JVM replacement must be explicit.
- Exact-count and unknown-size lazy datasets of 1,000 items, page size 50, ten
  filter/sort/refresh cycles and selected-item mutation; two-level hierarchy and
  at most three overlay layers. These are proposed test bounds, not new product
  limits or a claim that current fixtures already reach them.
- Raw/multipart upload and download through 256 MiB, three active requests per
  window, two concurrent transfer windows, a 30-minute mixed run. Require exact
  bytes, isolated cancellation, progress, fresh-request retry, supported Range
  resume, bounded configured queues/timeouts and no retained operations/temp
  files after drain. Collect memory/descriptor trends; a memory ceiling/SLO is
  a separate decision, not fabricated from one run.
- D03 physical input/Chinese Pinyin, D04 VoiceOver and D05 real Finder transfer
  each need one bounded operator scenario in addition to automated regression.
  New OS permission choices, clipboard mutation and external interactions use
  explicit per-run authorization; old one-time approvals cannot be reused.
- N04/N05 cover transient selected read and create-new write, their applicable
  lifecycle branches and replacement invalidation. N03 requires an observed
  eligible-settings banner, while API success means scheduling, not delivery.

All B rows, and the bounded part of B/E rows, must pass before this baseline is
declared complete. Proposed extensions are: N06 persistent/transactional file
operations; transparent cross-process transfer continuation; richer clipboard
formats/edit histories; further locales/RTL/currency; deeper combinations and
higher/longer load. These are **proposed deferrals**, not silently removed
requirements. Product planned replacement, accessibility and Finder interaction
are deliberately not deferred by default. User approval freezes the profile;
new independent capabilities require a scope change, while defects inside the
approved profile always block completion.

Stage A delivers this inventory and decision list, not a green matrix. Stage B
starts only after approval. See [roadmap.md](roadmap.md) for dependencies and
[acceptance-tools.md](acceptance-tools.md) for evidence admissibility.

## Current Verified Surface

The following is the accumulated focused surface, not current baseline-wide
certification. Apply the matrix statuses above, particularly unresolved
activation/recovery regressions and missing OS/AT evidence.

Validation covers representative bootstrap, UIDL, events, server patches, push,
input components including explicitly enabled Slider, shell layout through
AppLayout, experimental complex layout through MasterDetailLayout, slotted
content through Card, Badge and Markdown rendering, component resources
including Icon and Avatar, owner-bound input popup behavior using ComboBox,
Select, DatePicker and a focused localized TimePicker slice as probes, including
combobox/listbox/dialog
accessibility, owner, selected-item and calendar focus, active-option, listbox
and calendar keyboard navigation, selection, close, value retention, Java
listener round-trips and UIDL patches, plus trusted AppKit primary-pointer
outside dismissal with modal click-through suppression,
an independent public-API MultiSelectComboBox route with an initial `ava` token
field, a clear-enabled `kai,sol` field and two explicit-ItemFilter server-filter
fields initialized to `ava`, five trusted generated AppKit primary clicks, 15
trusted keys and two trusted input events, `sol` addition, two-stage
Escape/Backspace semantics, nested-shadow remove-button removal, atomic
clear-button removal, keep-filter selection and Escape isolation, plus default
post-selection filter clearing with full server/ARIA option rehydration,
a separate page-size-10 lazy MultiSelectComboBox route whose public
`CallbackDataProvider(fetch, count, id)` returns 80 fresh, unequal objects with
stable logical ids, retains initial `R-060` through distant paging and `z`
filtering, adds `R-061`, closes/resets and repeats `z` to render a fresh selected
`R-061` while retaining both logical selections and chips, then re-queries a
fresh selected `R-060` through `x`,
owner-bound menu popup behavior using MenuBar, ContextMenu and GridContextMenu
as probes, including owner/expanded ownership, menu accessibility, initial
focus, keyboard movement and selection, trusted secondary-click opening, Java
opened/action/closed listener round-trips, Escape close and focus restoration,
one nested MenuBar child submenu with ownership/ARIA, Right/Left traversal,
leaf selection and layered Escape, one nested ContextMenu child submenu with
ownership/ARIA, Right/Left traversal, Java leaf selection and layered Escape,
default end-collapsing responsive MenuBar overflow with collapsed-item
ownership/ARIA, Java selection, Escape focus restoration and expanded-width
top-level item restoration, including one collapsed parent with child-menu
ownership/ARIA, Right/Left traversal with overflow-root retention and
collapsed-parent focus restoration, child reopening, Java leaf selection and
layered Escape, plus distant virtualized Grid body-row targeting, row/column
context association and recycled DOM, including one child submenu with
ownership/ARIA, Right/Left traversal with root retention, parent-focus and
row/column-context retention, child reopening, Java leaf selection and layered
Escape,
single and nested modal Dialog initial focus, focus
trapping, top-of-stack Escape close, client-originated lifecycle events, parent
retention, layered focus restoration and dialog accessibility semantics, plus
anchored modeless Popover ownership, target ARIA, logical Tab order, Escape
focus restoration, outside-click close with external focus preservation and
client-originated lifecycle events, and a modal Dialog/modeless anchored
Popover mixed stack with top-layer Escape and outside-click ownership, parent
retention and two-stage focus restoration, plus a two-level anchored modeless
Popover stack with nested ownership and accessibility, logical Tab order,
top-layer Escape close, progressive outside-click close, parent retention,
layered trigger-focus restoration and external-focus preservation, plus a
three-level modal Dialog/anchored modeless Popover/auto-added modal
ConfirmDialog stack with default confirm focus, alert-dialog accessibility,
browser-programmatic opening setup, generated AppKit Escape close for the top
and bottom modal layers, trusted AppKit primary-pointer outside close for the
middle layer, parent retention, Java lifecycle/UIDL evidence and layered focus
restoration, multiple
non-eager routes whose application-owned route and router-layout
JavaScript/CSS dependencies are generated and loaded as production chunks
including a transitive dynamic import, bidirectional lazy Grid virtualization
with a distant viewport, stable-identity `MULTI` selection across recycled rows,
eight trusted generated AppKit clicks across four row checkboxes and four Flow
refresh buttons, exactly three exact-count Grid client-originated Java
selection/UIDL-response round trips, a same-id fresh unequal selected-item replacement with
`refreshItem` cell patching and no extra selection event, and refreshed
selected-state rehydration after owner sorting and `PRESERVE_ALL`
configurable filtering, plus matching-item removal while both selected rows are
off viewport, with explicit `refreshAll`, filtered/total cardinality change,
query advancement, removed-row DOM eviction, selection retention and
filter-reset rehydration over 80 items, plus an independent public fetch-only
unknown-size Grid whose estimate grows from 40 without a count query, crosses
the initial estimate in backend fetching, discovers exact tail 67 from a
terminal short fetch, reports estimated then exact `ItemCountChangeEvent`
state, and completes a trusted tail-row selection through Java, UIDL and DOM.
A subsequent trusted Flow button appends through item 93 and invokes public
`refreshAll`; the locked exact tail resets to estimate 40, grows through at
least 120, crosses the prior tail, rehydrates the retained selected item without
another selection event, and discovers exact tail 93 from another positive
short fetch. Another trusted Flow button removes the unselected tail down to
72 items and invokes public `refreshAll`; a separate estimated-to-exact epoch
retains the selected item, evicts the removed tail and discovers exact 72.
An independent exact-count lazy-Grid path deletes an off-viewport selected
record through explicit public `deselect`, backing-list removal and
`refreshAll`, verifies 80-to-79 cardinality and empty selection, then selects
and recycles a surviving neighbor through another trusted native click.
The verified surface also includes backend hierarchical TreeGrid root/child
count and fetch queries, two-level
client expansion, leaf selection, server-driven child refresh and collapse,
ComponentRenderer create/update lifecycle, LitRenderer property binding,
client-function callback and server refresh patch, and buffered Grid Editor
with Binder save, cancel, required-field validation and model restoration,
ClientCallable Promise resolve/reject, structured nested bean/list input with a
nested list/map result, PendingJavaScriptResult browser Promise result/error
callbacks, Java bean argument serialization with TypeReference generic-list
result conversion, and a branch-complete core JSON-shape matrix covering
numeric-string and enum ClientCallable decoders, primitive arrays,
method-level generic collections, primitive-null defaults, nullable references,
varargs, enum/array/null Promise output, plus `Class`-based scalar, enum, array
and null PendingJavaScriptResult conversion and nested generic-map
`TypeReference` conversion, pre-send cancellation without browser execution,
already-sent cancellation refusal with normal Promise completion, and disabled
ClientCallable `ONLY_WHEN_ENABLED`/`ALWAYS` enforcement, invisible-owner
JavaScript deferral until visibility returns, and detached-owner pending
JavaScript cleanup that survives reattachment, typed generic-list
CompletableFuture completion, JavaScriptException failure and cancellation,
session-lock blocking protection, multiple pre-send subscribers and post-send
subscription rejection with the original subscriber completing normally,
ordinary ClientCallable suppression while its owner is inert with the browser
Promise left pending, `@AllowInert` completion, and ordinary ClientCallable
recovery after modality is removed, plus focused Flow
DragSource/DropTarget
configuration, drag-start/drop/drag-end listener round-trips, server-side drag
data and MOVE effect semantics,
small multipart Upload/StreamReceiver completion, large raw UploadHandler
progress/cancellation/re-selection retry, large multipart
parsing/cancellation/re-selection retry, same-window two-file raw Upload
overlap/cancellation isolation, same-window multipart transport overlap with
session-locked parsing and cancellation isolation, cross-session multipart
parser concurrency and scoped cancellation isolation, StreamResource, scoped
multiple windows, native command correlation, sidecar recovery, and
deterministic failure of an in-flight opaque request followed by a fresh-session
request after unexpected sidecar replacement. Sidecar recovery also verifies a
deliberately unacknowledged old-session push is not replayed while fresh-session
push starts at sequence one. The focused DnD gate
runs an isolated route
through the standard production bundle in a real WKWebView. It routes a
generated macOS primary-pointer gesture through AppKit, requires the trusted
browser drag lifecycle and exactly-once client-originated Flow listeners, and
proves opaque request, response and push ownership. This does not claim physical
mouse hardware or external Finder file-drop coverage. The focused lazy-Grid
selection gate runs an isolated route through the standard production
bundle and opaque request/response transport. Its 80-item
`CallbackDataProvider` uses stable ids and the official Grid `MULTI` selection
column. The first five generated AppKit primary clicks reach three visible
official row checkboxes and two Flow refresh buttons as trusted DOM events. The checkboxes
produce exactly three client-originated Java `MultiSelectionEvent` updates with
UIDL responses: two additions followed by one removal. Between those changes,
the button replaces selected `L-060` with a fresh, unequal object carrying the
same stable id, invokes `refreshItem`, and requires its state cell to patch to
`refreshed` while the row remains selected and the Java selection-event count
does not change. Owner sorting and a `platform` filter preserve `L-060,L-061`
under `PRESERVE_ALL`. After both selected rows leave the rendered viewport, the
second button removes matching `L-003` and explicitly invokes `refreshAll`:
filtered/total cardinality changes from 26/80 to 25/79, fetch and count queries
advance, one generic `DataChangeEvent` but no `DataRefreshEvent` or selection
event is observed, `L-003` is absent from the rendered Grid, and both ids remain
selected. Returning to a selected row, resetting the filter and recycling
distant rows rehydrates refreshed selection state before deselection leaves
`L-060`.
The route then drives a separate Grid through the public fetch-only `setItems`
overload with page size 5, estimate 40 and increase 40. Its browser
viewport crosses the initial estimate without invoking an exact count query,
observes estimated growth, and reaches a terminal short fetch ending at
`U-067`; the Java item-count event and client Grid size then settle to exact 67.
A sixth generated AppKit click selects the official tail-row checkbox and
requires the client-originated Java selection listener, UIDL response and
selected DOM state. Once the old tail is off viewport, a seventh generated
AppKit click enters a Flow Button listener that appends through `U-093` and
calls public `GridLazyDataView.refreshAll()`. One generic data-change event and
no item-refresh event are required. Exact 67 must reset to estimate 40, grow
through at least 120, fetch beyond the old tail, rehydrate selected `U-067`
without another selection event, and settle to exact 93 from a positive short
terminal page ending at `U-093`.
After returning to the top, an eighth generated AppKit click removes unselected
`U-073` through `U-093` and calls the same public `refreshAll()` API. It emits
one generic data-change event without an item-refresh or selection event.
The independent shrink epoch must reset exact 93 to estimate 40, grow to 80,
advance fetch and item-count events, and converge to exact 72 from a positive
short terminal page ending at `U-072`. The retained same-instance `U-067`
rehydrates selected row, checkbox and ARIA state; all 21 removed ids are absent
from rendered cells. Initial and append evidence remain frozen and are checked
against the final snapshot, rather than being reused as shrink evidence.
Navigation, viewport, sort, filter and refresh-button positioning are
browser-programmatic setup, as are the unknown-size viewport advances.
This eight-click gate does not itself prove push, physical input hardware, touch,
assistive-technology interoperability, selected-item removal, arbitrary repeated
reset sequences, reorder or sort/filter-key mutation, broader pagination, larger scale,
fresh-object identity across the unknown-size reset or other Grid capabilities.
The separate selected-removal gate reuses the exact-count Grid and makes three
generated AppKit clicks: select `L-060`, delete it through a Flow Button while
its row is outside the rendered viewport, and select surviving `L-061`. The
application calls public `grid.deselect(item)` before removing the backing item
and invoking the configurable provider's `refreshAll()`. `PRESERVE_ALL` does not
automatically prune lazy-provider selection; the exact Java event history is
client-originated selection, server-originated explicit deselection, then
client-originated neighbor selection. Refresh advances fetch/count beyond
baselines captured after deselection/removal, emits one generic data-change
event without an item-refresh event, and changes both backend/client size from
80 to 79. Returning to the deletion neighborhood shows adjacent `L-059,L-061`,
no `L-060` and no stale row, checkbox or ARIA selection. The new `L-061`
selection rehydrates after another viewport recycle. Client `selectedItems`
represents cached pages and empties when the selected page is evicted; Java
history and visible rehydration establish retained server selection. Each
native click requires an independent completed opaque UIDL response, with
stable main-window/WebView/sidecar ownership and final quiet drain and shutdown.
This focused application-directed deletion does not prove automatic pruning,
unknown-size selected-item deletion, multi-item transactions, filter/sort-key
mutation, recovery, physical input, assistive technology or focused-route push.
The focused heterogeneous
overlay-stack gate uses browser-programmatic setup to open a modal Dialog, an
anchored modeless Popover and an auto-added modal ConfirmDialog, then sends two
generated AppKit window Escape sequences and one primary-pointer outside click
to the real WKWebView. It requires trusted DOM receipt for each closing input,
top-layer ownership, parent retention, layered focus restoration,
client-originated Java lifecycle listeners and their UIDL patches through the
opaque request/push path. This focused evidence does not claim physical input
hardware, touch or assistive-technology interoperability, or deeper overlay
stacks. The focused input-popup gate distinguishes scripted ComboBox and
DatePicker-open events from AppKit keyboard events delivered to the WKWebView.
Select uses AppKit keys for open, listbox movement without early value change,
Enter selection, reopening and value-preserving Escape close, with selected-item
focus, Java value-listener and UIDL patch evidence. DatePicker uses AppKit keys
for calendar focus, movement, selection and close. After those keyboard paths,
Select and DatePicker are reopened and dismissed with AppKit primary-pointer
events delivered to the WKWebView. The gate requires trusted DOM receipt on the
document background selected by Vaadin's modal pointer shield, retained values,
restored owner focus, client-originated Java opened-change listeners and UIDL
patches, while an underlying Flow button remains unclicked. A separate focused
MultiSelectComboBox gate runs the standard production bundle in a real
WKWebView over opaque requests and UIDL responses. Its token field starts with
`ava`, a public clear-enabled field starts with `kai,sol`, and a third field
starts with `ava` and uses an explicit public `ItemFilter` plus
`setKeepFilter(true)`. A fourth field starts with `ava`, uses an explicit
`ItemFilter`, and retains the public default `keepFilter=false` behavior. Five
trusted generated AppKit primary clicks, 15 trusted generated AppKit keydowns
and two resulting trusted input events drive the route. The first three clicks
and first seven keys retain the verified token, Escape/Backspace,
nested-shadow remove-button and atomic clear-button paths.

The fourth click opens and focuses the keep-filter field; trusted `k` input
reaches a latched server-filter request/response checkpoint that renders only
`kai` while retaining `ava`, then ArrowDown/Enter selects `kai`. Two trusted
Escapes clear its active option and close/reset the popup while preserving
`ava,kai`. The fifth click opens and focuses the default-clear field. Its own
`k` query and ArrowDown/Enter selection checkpoints add `kai`; selection then
clears input/filter, performs an empty-filter server rehydrate and leaves the
field focused/open with all options, chips `ava,kai`, and normalized owning
listbox ARIA selection map `ava:true,kai:true,sol:false`.

The token field produces exactly three client-originated Java
`MultiSelectionEvent`s and three matching UIDL-response checkpoints, the clear
field produces one event with `removed=kai,sol` and one matching response, and
each server-filter field produces one event with `added=kai` plus independent
query and selection checkpoints. The first two fields finish empty. Navigation
and pointer positioning, scripted token-field focus, and
empty-input/caret-at-start preparation before Backspace are
browser-programmatic; field focus comes
from the trusted fourth and fifth clicks. Balanced opaque exchanges drain
through a quiet interval before graceful sidecar exit, and focused push is
false. That eager route is not full component parity; remaining behavior
includes manual reset, broader close/reopen combinations,
selected-items-on-top combinations, physical input hardware, touch and
assistive-technology interoperability.

A separate focused lazy MultiSelectComboBox gate uses a page-size-10 public
`CallbackDataProvider(fetch, count, id)` to expose 80 stable logical ids through
fresh, unequal Java objects, with initial `R-060` selected. One trusted generated
AppKit primary click opens and focuses the owner. Nine trusted generated AppKit
keydowns (`z`, ArrowDown, Enter, Escape, Escape, `z`, Escape, `x`, Escape) and
three trusted input events drive public listbox/option ARIA behavior. A scoped
ordinary listbox scroll reaches a nonzero-offset fresh `R-060` option whose
`aria-selected=true` state matches the initial value. The `z` query renders only
fresh `R-061`; ArrowDown focuses it and Enter produces one client-originated
Java selection event and one semantic UIDL checkpoint, adding it while
logically retaining `R-060`. Two Escapes clear option focus, close and reset the
popup. Typing the same `z` term after that close/reset renders a new fresh
`R-061` option with `aria-selected=true`; both logical selections and chips
remain intact before another Escape closes and resets the field. The `x` query
then renders a fresh `R-060` option as selected while both logical items and
chips remain selected, before the last Escape closes and resets the field again.
Navigation, owner positioning and scoped listbox scrolling are the only
browser-programmatic stages. Far-page, first-`z`, selection, repeated-`z` and
`x` query checkpoints assert semantic
request/UIDL-response progress rather than exact raw envelopes. Balanced opaque
exchanges drain through a quiet interval before a graceful sidecar shutdown
request, and focused push is false. Unknown-size providers, mutation-driven or
manual refresh, broader pagination/filter and close/reopen combinations,
physical input hardware, touch and assistive-technology interoperability remain
open; this focused route is not full parity.

A focused localized-decimal route uses public `BigDecimalField` APIs and
`ON_CHANGE` to verify exact decimal editing, not JavaScript floating-point
conversion. Generated AppKit clicks and application-level keyboard events
replace an initial `42.50` under `Locale.GERMANY`, commit a scale-15 value through
Tab, change only the locale to `Locale.US` and then commit a different scale-3
value. Java listener history and returned UIDL preserve the full old/new values
and scales; changing the separator preserves the Java value and produces no
extra value-change event. The standard host Edit menu supplies responder-chain
Cmd+A. Both actual full selections precede replacement, whether or not AppKit
also delivers a DOM keydown for the shortcut. The gate requires trusted typed
input, three request/response checkpoints and a balanced quiet drain. Navigation
and positioning remain browser-programmatic. This focused single-window slice
does not prove physical hardware, IME, assistive technology, other locales,
RTL, grouping/currency formatting, empty/malformed input, Binder validation or
focused-route push. It also does not validate every Edit command or command
ownership across native sheets and multiple windows.

A separate German-locale bound-form route verifies required, malformed and
custom-validation recovery through the ordinary Binder default/required/custom
validator chain. Five generated AppKit edits and Tab commits empty the initial
`42.50`, enter malformed `1,,2`, recover to `12,50`, reject `150,00` against a
custom maximum of 100 and recover to `7,125`. Empty and malformed input both
have a null Java field value: the second transition produces no extra value
change, but an actual component validation-status event makes Binder replace
the required error with the default bad-input error. Required, malformed and
custom rejection preserve the last valid bean. The final four-event Java
history includes the rejected field value as the final event's old value and
preserves exact decimal scales; the bean advances only on valid edits.

The real standard-bundle WKWebView gate cross-checks actual field/error
presentation, component/Binder histories and five distinct completed post-Tab
UIDL responses. Recovery means `invalid=false` and hidden error presentation;
an upstream-retained raw error-message property need not be cleared. Component
validation-status history records emitted events rather than current bound-form
validity, so its last malformed-input event can remain invalid after recovery.
Five native focus clicks, 30 native key pairs, four observed full-selection effects
and 21 trusted input events precede a quiet drain and graceful shutdown. Browser
code only navigates, positions and observes. This single-window focused result
does not prove other locales, grouping/currency, RTL, IME, physical hardware,
assistive technology, cross-window forms or focused-route push; the independent
German-to-US locale-switch contract remains unchanged.

A separate focused native-edit route verifies one editing-history cycle in a
public `TextField` with `ValueChangeMode.EAGER`. A generated AppKit click focuses
the input, then `NSApplication.sendEvent` delivers `x`, Cmd+Z and Cmd+Shift+Z
through the normal application/responder-chain path. The real WKWebView emits
trusted `insertText`, `historyUndo` and `historyRedo` input events; the official
Flow client sends exactly three client-originated Java value changes with the
empty → `x` → empty → `x` old/new history. Each step waits for the returned
Java status/history patch and an independently correlated, post-key opaque
UIDL request/response completion before the next key. Native dispatches and
actual input events remain mandatory even if AppKit consumes a shortcut without
a DOM keydown. The gate binds browser and native scope to the same main window
and sidecar, checks the balanced quiet drain and graceful shutdown, and keeps
all automation outside production sources. Navigation and input positioning
remain browser-programmatic. This closes one single-character undo/redo cycle,
not arbitrary undo grouping, rich-text editing, clipboard commands,
cross-window/native-sheet command ownership, physical input, IME, assistive
technology or focused-route push.

A separate two-window gate reuses that public route and opens a secondary through
a trusted Flow Button and `Desktop.current().windows().open`. Generated AppKit
input interleaves main `x`, secondary `y`, main Undo, secondary Undo, main Redo
and secondary Redo, with a trusted input-focus click before each key. Both
windows retain their own three-event Java history and trusted
`insertText`/`historyUndo`/`historyRedo` sequence; the peer's editing state stays
unchanged at each paired checkpoint. A final click returns to main without
another edit. The shared fixture explicitly uses public `setAutocorrect(false)`
and `setAutocapitalize(Autocapitalize.NONE)` APIs, and the new gate checks their
actual component/input attributes. Automatic text substitution on focus loss
is a separate behavior, not an editing-owner isolation failure.

Each of the six post-key UIDL completions is correlated by the full
window/session/UI/request tuple: independent WebViews may reuse request IDs.
The two native windows, WebViews, distinct scopes and original sidecar stay
stable; native key-window ownership, not the background document's retained
active element, identifies the command owner. Ordinary background transport may
advance without changing peer editing history. Eight paired observations,
eight pointer dispatches, six key pairs and a final balanced quiet drain precede
closing both windows and graceful shutdown. This is focused evidence for two
retained WebView undo histories, not native-sheet or rich-text editing,
clipboard commands, automatic substitution, arbitrary undo grouping,
owner close/reopen, recovery during editing, physical input, IME, assistive
technology or focused-route push.

A separate same-ID close/reopen gate exercises one retired and one fresh native
editing owner while preserving the original main window. Generated AppKit input
first establishes main `m` and secondary A `x` histories. A trusted main-window
Button calls the public close API; its `closing=true` result is only acceptance
of the close request. The gate also requires the returned main UIDL status,
actual scoped window-close events, registry removal and a hidden A window before
opening secondary B through the public API with the same window ID. B has a new
context, native window, WebView and session UUID, while main retains its original
objects, scope, sidecar PID and generation.

B starts empty with no Java history. A native Cmd+Z then produces no input or
Java change during at least 1000 monotonic milliseconds and three quiet polls;
this bounded no-op does not require an artificial UIDL mutation. Native
`y`/Undo/Redo proves B's new history works, followed by main Undo/Redo proving
its earlier history survived. The inactive owner's editing state remains exact
even when balanced background transport advances. Twelve checkpoints, eleven
pointer pairs, eight key pairs and seven actual Java value changes are checked;
each change has a distinct completed post-key UIDL correlated by the full
window/session/UI/request tuple. The three public open/close/open commands must
also occur within their respective main UIDL request/result intervals. A final
quiet drain precedes closing B and main and graceful shutdown.

The driver retains A references only for identity and retirement observations;
this is not evidence of object deallocation or internal undo-manager destruction.
It covers one same-ID close/reopen cycle with settled single-character histories,
not repeated churn, recovery during editing, native sheets, rich-text grouping,
clipboard ownership, physical hardware, IME, AT or focused-route push. All new
automation remains in test support; production host and Flow transport are
unchanged.

A separate settled-edit recovery gate keeps the same two native window owners
through one unexpected JVM exit. Main opens the fixed-route secondary using the
public window API. The secondary's native `x` input must reach Java change one
and a completed post-key UIDL; both documents then reach a balanced quiet
checkpoint before the exact old JVM is killed. Its real pre-exit snapshots are
archived without carrying counters into the new document.

Recovery retains both contexts, windows, WKWebViews, native window numbers and
opaque scopes, but changes the JVM PID and document generation. Actual browser
`performance.timeOrigin` changes for each WebView and stays stable within its
epoch. The secondary directly reloads its immutable edit-route entry URL;
main returns to its original bundle root, not the route reached by earlier
browser navigation. The driver does not navigate again or reset marker arrays.
It waits for both actual documents and Flow clients, rather than treating native
recovery-ready as DOM readiness. Every asynchronous input/inspection completion
is fenced by captured PID and generation in addition to native object identity.

The fresh secondary is empty with no Java history. Native Cmd+Z is observed for
at least 1000 monotonic milliseconds and three quiet polls with no input or Java
change; no synthetic UIDL is demanded. Native `y`/Undo/Redo then produces three
new client-originated Java changes and distinct post-key UIDL completions. Eight
checkpoints, six pointer pairs and five key pairs cover four actual edits across
both JVMs. The inspector independently checks complete request streams and
reliable ambient pushes in four generation/window buckets, allowing request IDs
and push sequences to restart. Main's root push probe is not focused editing-push
evidence. Final quiet observations, both closes, empty drain and graceful new-JVM
shutdown complete this bounded recovery path.

This proves a fresh editor remains usable after a settled edit and one unexpected
sidecar exit. It does not preserve application values or undo history, prove
internal undo-manager destruction, or cover in-flight editing, planned
replacement, repeated failures, native sheets, rich-text grouping, physical
hardware, IME or AT. The fixture and production runtime are unchanged.

A separate single-window Cut/Paste gate reuses the same public EAGER TextField.
One generated AppKit focus click and four application-level key pairs type `x`,
select all, Cut and Paste through the normal Edit menu/responder chain. The
actual collapsed-to-full selection produces no Java change; trusted target-owned
`cut`/`paste` events precede `deleteByCut`/`insertFromPaste`. Together with
`insertText`, they yield exactly three client Java changes and three distinct
completed post-key UIDL responses, empty → `x` → empty → `x`. Command-shortcut
DOM keydowns may be consumed by AppKit, but native dispatches and actual effects
remain mandatory.

The acceptance-only driver privately snapshots materializable pasteboard bytes
before Cut, treating read failure as failure, not an empty clipboard. It does
not seed clipboard text: causal trusted Cut/deletion evidence must precede the
native observation of `x` at an advanced change count. Paste and the final quiet
drain retain that epoch; bounded restoration must verify the original byte/type
representations before acceptance. Fake-provider tests cover failure, contention
and partial restoration without accessing the system clipboard. Original contents
stay in memory, unexpected driver snapshot strings are redacted, and this gate
disables automatic WebView screenshots. After Cut dispatch, uncertain ownership
or observed contention prevents cleanup from overwriting an unowned clipboard.
This is best-effort preservation on a cooperative desktop, not atomic exclusion,
crash recovery or lazy-provider/file-promise identity preservation. It does not
cover public Clipboard capability permissions, non-text editing, multi-window or
native-sheet clipboard ownership, physical hardware, IME, AT or focused-route
push. Production host and Flow transport remain unchanged.

A separate focused TimePicker gate verifies the official `Locale.US`
12-hour
display, a bounded
30-minute step list, initial owner-input focus with no focused list candidate,
successive AppKit ArrowDown candidate focus, Enter selection, Java
value-listener execution, the returned UIDL patch, close and owner-focus
restoration. A separate focused DateTimePicker validation gate drives the
component's own commit state machine through minimum, maximum, incomplete,
required and malformed default-validation branches, with valid recovery between
branches and after malformed input. The same picker is bound through the
standard Binder default/required/custom validator chain. A value that violates
both the minimum and custom rule preserves the default error precedence, a
default-valid custom rejection retains the last valid bean value, and a
different valid recovery updates that bean. The gate requires client-originated
Java value listeners, malformed-input validation-status evidence, Binder status,
configured messages, parent/child invalid and error-presentation UIDL patches,
final recovery and closed overlays. Recovery follows the upstream
`HasValidation` contract: an invalid field becomes valid and hides its error,
while the raw custom error-message property is not required to be cleared.
These gates do not claim physical keyboard or mouse hardware, remaining picker
variants, locale breadth beyond that `en-US` case, touch dismissal or
assistive-technology interoperability. The
focused menu-popup gate uses AppKit key and secondary-click events delivered to
the WKWebView and requires trusted DOM receipt. Its MenuBar, ContextMenu and
GridContextMenu are acceptance probes for the generic standard Flow/browser
path, not product adapters. The ContextMenu sequence additionally verifies one
nested child submenu, including root and child ownership/ARIA, Right/Left
traversal, a client-originated Java leaf listener, full-stack close after
selection, layered Escape close and parent/target focus restoration. The Grid
sequence scrolls to a distant virtualized body row, dispatches the secondary
click through AppKit, verifies trusted DOM receipt and Flow row/column
association, then selects an action with AppKit key events. It also opens one
child submenu on that virtualized row, verifies child ownership and ARIA,
Right/Left traversal with root retention, parent-focus and Flow row/column
context retention, child reopening, a client-originated Java leaf listener
carrying the row item, full-stack close after selection, and layered Escape
restoration first to the parent item and then to the Grid. A second compact
Grid probe uses
`setDynamicContentHandler` to reject trusted header and trailing blank-body
itemless contexts without opening, then dynamically labels and allows the
`P-001` row context before Java selection, close and Grid-focus restoration.
The MenuBar sequence also verifies one
nested child submenu, including child ownership and
ARIA, Right/Left traversal, a client-originated leaf Java listener, layered
Escape close and parent/owner focus restoration. It also constrains a
responsive MenuBar, verifies Vaadin's default collapse-from-end overflow
ownership and accessibility, selects an actual collapsed item through AppKit
and a client-originated Java listener, restores focus on Escape, then opens one
collapsed parent item with ArrowRight, verifies child ownership, ARIA, keyboard
movement, ArrowLeft child close with root retention and collapsed-parent focus
restoration, child reopening and a client-originated leaf Java listener, and
closes the child/root layers with progressive Escape focus restoration. It
finally expands the bar and requires all original top-level items to return.
Physical hardware, touch/long-press, deeper MenuBar chains, reverse-collapse,
deeper chains inside overflow, deeper GridContextMenu child chains, and
assistive-technology behavior remain open. A separate real-WKWebView gate
covers the standard Vite client, Vaadin development resource graph, frontend
HMR and opaque Flow request ownership. Another real-WKWebView gate changes an
application Java source, verifies successful rebuild and planned process
replacement, retained native window/WebView identity, fresh Java output and
re-established opaque request/push ownership. A multi-module gate activates a
test-only Maven profile, proves its dependent module belongs to the selected
effective reactor, then independently changes a normal dependent POM, the
profile module's custom Java source root and its custom resource root. It
requires three planned replacements, Vite workspace refresh for the build
configuration change, the new Java/resource values in the retained WKWebView,
and recovered request/push ownership after every replacement. A failure-path
gate builds a candidate
that exits before `ready`, verifies request-correlated stable-JAR rollback, and
proves the retained window/WebView establish a fresh Flow session through the
restored runtime. These are development-webapp workflow gates, not evidence of
planned replacement in the standard production-bundle runtime. The current
controller rejects that mode; product-runtime triggering, authorization,
candidate validation and rollback remain open. Development HMR and source
watching are separate from the current macOS product-function completion gate.

## Compatibility Boundary

Servlet filters, servlet-backed security and normal WebSocket infrastructure
require the Spring Boot adapter:

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

## Open Work

### Current macOS Product-Function Scope

- product-runtime planned sidecar replacement, including a host-owned trigger,
  authorization, candidate validation, rollback and bounded termination of
  uncooperative old/candidate processes; the development-only control channel
  and automatic unexpected-exit recovery do not supply this product contract;
- broader generated and lazy frontend graphs beyond the verified non-eager
  route/layout JavaScript, CSS and transitive dynamic-import graph;
- broader Grid features and scale beyond the verified 80-item stable-identity
  `MULTI` selection path across distant recycled rows, trusted generated AppKit
  checkbox/button input, same-id fresh unequal `refreshItem` replacement, owner
  sorting, `PRESERVE_ALL` configurable-filter/reset rehydration and
  cardinality-changing removal while both selected rows are off viewport, with
  explicit `refreshAll`, plus the independent fetch-only unknown-size path from
  a 40-item estimate through growth and a positive short terminal fetch to
  exact 67 and trusted tail-item selection, followed by append-to-93 plus public
  `refreshAll`, reset to estimate 40, regrowth through at least 120, retained
  selection rehydration and another positive short terminal fetch to exact 93,
  then one unselected-tail shrink to 72 with a separate estimate-reset epoch,
  retained selection and removed-tail eviction, plus independent exact-count
  selected-item deletion through explicit `deselect`/removal/`refreshAll`, empty
  selection and surviving-neighbor reselection/rehydration,
  the backend
  hierarchical TreeGrid expansion/selection/refresh path,
  ComponentRenderer lifecycle, LitRenderer property/function/refresh semantics
  and buffered Editor/Binder save, cancel and validation; remaining gaps include
  unknown-size selected-item deletion, multi-item transactions and arbitrary
  repeated reset sequences, reorder or sort/filter-key mutation, fresh-object
  identity across unknown-size mutation and broader pagination;
- physical mouse-hardware Flow DragSource/DropTarget sessions and the separate
  external Finder file-drop boundary beyond the verified generated native
  pointer, trusted browser lifecycle, server-side data and MOVE-effect path.
  A test-only external-file runner, standard multipart Upload route, strict
  runtime inspector and independent manifest/operator-evidence verifier are
  prepared, but this boundary remains unverified until a real Finder transfer
  passes both proof layers. Same-application Computer Use drag coordinates do
  not establish a cross-application transfer, and a trusted DOM drop alone
  cannot authenticate Finder provenance;
- localized decimal behavior beyond the focused German-to-US separator switch,
  exact scale-preserving values and German required/malformed/custom Binder
  rejection and recovery, including other locales, RTL, grouping/currency
  formatting, broader bound-form combinations and cross-window ownership;
- native Edit behavior beyond focused Cmd+A replacement and single-character
  TextField undo/redo cycles in one window, two retained WebViews and one same-ID
  owner close/reopen cycle, plus single-window plain-text Cut/Paste, including
  cross-window/non-text clipboard commands, automatic text substitution,
  arbitrary undo grouping, rich-text editing, native-sheet ownership, repeated
  owner churn, and in-flight editing, planned replacement or repeated failures
  beyond one settled-edit unexpected sidecar recovery;
- broader owner-bound input popup behavior beyond the verified ComboBox, Select,
  DatePicker, focused `en-US` TimePicker and bounded DateTimePicker default
  and custom validation-state probe, including remaining picker variants,
  broader locale, touch dismissal, physical keyboard or pointing-device input
  and assistive-technology interoperability;
- broader MultiSelectComboBox behavior beyond the focused initial-value,
  keyboard-add, two-stage Escape/Backspace, nested-shadow chip remove-button,
  atomic two-value clear-button, explicit-ItemFilter server filtering and
  default post-selection empty-filter/ARIA rehydration, plus the focused
  80-item fresh-object lazy-provider paging, filtering, stable selection,
  close/reset, same-term `z` re-query with fresh selected-item rehydration and
  a later fresh selected `R-060` query through `x`,
  including unknown-size providers, mutation-driven and manual refresh, broader
  pagination/filter and close/reopen combinations, selected-items-on-top
  combinations, physical input hardware, touch and assistive-technology
  interoperability;
- broader owner-bound menu popup behavior beyond the verified MenuBar,
  ContextMenu and GridContextMenu body-row/nested-child/dynamic-policy probes,
  including deeper MenuBar submenu chains, reverse-collapse and deeper child
  chains inside overflow, deeper GridContextMenu child chains, touch/long-press
  input, physical hardware and assistive-technology interoperability;
- deeper anchored and broader heterogeneous multi-overlay-stack keyboard,
  outside-click, focus and accessibility behavior beyond the verified single
  and nested modal Dialog, single and two-level nested anchored modeless
  Popover, one modal Dialog/modeless Popover stack and one three-level modal
  Dialog/modeless Popover/modal ConfirmDialog stack;
- broader large-transfer behavior beyond the bounded DownloadHandler
  cancellation/open-ended Range-resume and raw UploadHandler
  progress/cancellation/re-selection-retry, large multipart
  parsing/cancellation/re-selection-retry, two-file same-window raw and
  multipart isolation, ten consecutive six-request/three-active same-window raw
  queue batches with registry turnover and bounded two-window cross-session
  parser concurrency, including broader Servlet multipart edge cases,
  arbitrary-duration or higher multi-window fan-out, automatic or resumable
  continuation across sidecar replacement, repeated or multi-range requests,
  larger scale and durable process-external transfer state. Deterministic
  failure of an interrupted request and a distinct fresh-session retry after
  one replacement are verified, but no transfer state crosses the process
  boundary;
- broader window-topology durability beyond the focused secondary owner close
  and main-closed/one-secondary recovery paths, including higher fan-out,
  repeated close/reopen cycles, concurrent closes and mixed
  recovery/replacement sequences;
- native capability coverage beyond the focused window-scoped
  `window.setTitle` allow/deny, source-missing production-default, explicit
  permissive-manifest package rejection, post-build parser-invalid manifest
  startup failure and file-dialog/Clipboard typed-outcome and in-flight
  owner-close paths, plus focused path-free bounded read, explicit release,
  owner-window-close, normal AppKit Quit, unsupported-version fatal shutdown of
  an idle grant and unexpected-sidecar-exit grant cleanup
  with fresh recovered access, and live cross-window read/release token rejection
  with a subsequent owner positive control, plus independent actual-Sandbox
  external selected-read/denied-sibling evidence; remaining work includes focused
  evidence for retirement/replacement, other fatal/forced termination and in-flight
  cleanup races, broader cross-generation combinations beyond recovered replay
  of an exit-revoked token, Sandbox boundaries beyond selected read and
  create-new bounded write, persistent bookmarks, write-lifecycle races,
  overwrite/transaction semantics and cross-process recovery, broader
  Clipboard formats and pasteboard contention, asynchronous capability
  progress/cancellation/retry/resume behavior, and broader macOS authorization
  states.

### Deferred Work Outside the Current Completion Gate

- JVM in-place class hot replacement and the complete upstream Servlet-based
  Vaadin DevTools server lifecycle;
- source roots introduced only by lifecycle plugins after Maven's
  projects-read phase, beyond the verified effective-model source/resource
  roots and profile-activated reactor graph;
- embedded JREs, installers, updates and non-macOS hosts.
