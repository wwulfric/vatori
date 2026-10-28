# Acceptance And Smoke Tools

These tools verify repository and runtime contracts. They are not public
framework APIs.

## Capability Matrix Evidence Rules

The canonical capability IDs and statuses live in the
[runtime matrix](flow-over-ipc-runtime.md#product-capability-matrix); do not keep
a second status table in this document. Its A/ command names refer to the
acceptance bin directory. Use the full commands and prerequisites below when
executing them; a named runner is not proof that it passed.

- Source/driver inspection establishes implementation and intended coverage,
  not execution. Synthetic inspector positives/negatives establish the verifier's
  discrimination, not an OS or WKWebView outcome. Non-window smoke never closes
  physical input, Finder, VoiceOver, system permission or banner visibility.
- A retained trace may be rechecked using `bin/flow-ipc-trace-inspect` without
  launching the app. Passing establishes only that trace's focused contract;
  record source/bundle provenance or mark it historical/unknown. An acceptance
  marker alone is insufficient, and intentional fatal tests may correctly
  contain their narrowly specified error event.
- Accepted baseline evidence must identify capability IDs, source revision,
  dirty state, host/bundle digest, OS/JDK/architecture, command and parameters,
  input method, gate version/result, explicit exclusions and operator actions
  where required. Keep full traces and receipts in CI/external artifacts, not
  Git. Require actual exit/cleanup evidence when lifecycle is part of the claim.
- Current failures override historical green results for the affected contract.
  A canceled run, empty overwritten log, missing output or stale app cannot
  establish success. Preserve completed evidence before retry; shared runner
  filenames are working files, not an immutable evidence archive.
- UI activation by an external operator/tool may be a declared setup action.
  It does not prove automatic activation. Generated AppKit input, physical
  keyboard/IME, accessibility interaction and cross-app Finder transfer must
  be labeled separately. Do not weaken active/key or provenance gates to pass.
- Stage A is a source/evidence audit, not a requirement to rerun all gates.
  Revalidation is targeted by changed ownership/transport/capability code;
  final Stage B certification uses the approved frozen profile and revision.

Baseline criteria proposed in the runtime matrix are not automatically supplied
by existing runners. In particular three-window churn, the proposed endurance
profile, VoiceOver/operator tasks and product planned replacement need gap work
before a complete baseline run can be claimed. Per-operation semantics must
distinguish cancellation from revocation, fresh-request retry from resume, and
transient grants from persistent/transactional authorization.

## Non-Window Suite

```bash
bash bin/verify-smoke
```

The script currently covers:

- repository hygiene and trace-inspector contracts;
- opaque request-handler integration and StreamResource behavior;
- Flow IPC scope, backpressure, cancellation and capability correlation;
- launcher transport, ready payload, pure Flow EOF and optional Spring context lifecycle;
- application bundle input discovery and safe packaged-resource lookup;
- browser request and push bridge contracts.

The script itself is the source of truth for its subcommands.

`bash test-support/native-host-macos-acceptance/bin/check-fatal-protocol-startup`
checks the test application's opt-in, one-shot invalid-version trigger in owned
JVMs. `VAADIN_DESKTOP_TEST_FATAL_PROTOCOL_DIR` must name a fresh canonical,
owner-only `vatori-fatal-protocol.XXXXXX` directory. Startup publishes
`startup.pid`; a later empty owner-only `trigger` file requests exactly one
unsupported-version frame. Invalid state fails closed and trigger polling is
bounded to ten minutes. It cannot be combined with the recovery or host-shutdown
startup fixtures. This helper does not itself establish Flow preparation,
select a file, prove native cleanup, or constitute a fatal file-grant acceptance
gate; those require a real-window driver and independently correlated evidence.

`node test-support/native-host-macos-acceptance/Tests/file-read-grant-fatal-evidence-smoke.mjs`
checks the companion external evidence utility. Its `watch` mode waits at most
180 seconds for the original selected grant's prepared receipt before creating
the one-shot trigger; `verify` requires the matching fatal sequence and actual
host/JVM absence before appending `flowFileReadGrant.fatalExitObserved`. Only
signal-zero `ESRCH` counts as absence; a live process or permission error fails.
Private state and trace files must be owned, bounded, non-symlink, single-link
files with mode 0600, under the canonical mode-0700 state directory where
applicable. The utility sends no termination signals and does no cleanup.
The smoke uses synthetic traces and owned exited Node processes, not a real
Flow app. Full scope, command/result, privacy and cleanup-field validation is
the separate `--require-flow-ipc-file-read-grant-fatal-shutdown` inspector gate.
These supporting checks do not themselves constitute real-window fatal-grant
evidence. The isolated runner is:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-fatal-shutdown
```

It builds the standard production bundle and acceptance host under the package
lock, launches a uniquely identified private app through LaunchServices and
reuses the normal host-shutdown grant preparation driver. Select only its
generated fixture under the printed `Fixture root` in the real Open panel;
do not invoke Quit or close either window. After preparation the external
watcher creates the invalid-version trigger. The runner then requires fatal
completion, independent host/JVM absence and the full inspector gate, in that
order before cleanup. Preparation is bounded to 180 seconds and post-trigger
exit to 30 seconds. LaunchServices waiter status alone is not a host exit-code
measurement. Any process cleanup makes the run fail; uninspectable or surviving
processes retain the clone. Logs remain in
`.build/native-hosts/macos/host-file-read-grant-fatal-shutdown.{out,err}.log`.
`file-read-grant-fatal-runner-smoke.mjs` checks ownership matching and cleanup
guards with inert substitutes. Focused real-window coverage includes external
native Open-panel selection, the public Java `SELECTED` result, scope-specific
UIDL completion and an idle live grant before the unsupported-version fault.
The gate requires successful descriptor close and matching security-scope stop,
zero-pending/zero-failure drain, actual cooperative JVM exit, fatal status `1`
in the host completion trace and independent pre-cleanup process absence.
Route setup is browser-programmatic; the two preparation clicks are generated
AppKit input and the panel is operated through Computer Use. This does not
establish physical input, App Sandbox, in-flight I/O or all fatal causes.

Within that suite, `bash bin/flow-ipc-scope-smoke` also verifies the public
captured-capability contract. It captures an exact secondary opaque UUID while a
Vaadin session is current, clears the current session and invokes from a virtual
thread; checks wrong-scope completion rejection; closes the owner and requires
pending and late bound calls to return `owner-window-closed` without late
output; reopens the same public id with a new UUID without retargeting the old
context; fails an unbound no-current-session call with two live scopes without a
command; and preserves the single-main legacy fallback. The default
`NativeBridge` capture SPI is covered separately so non-scoped implementations
keep their existing behavior. The same smoke also exercises the explicit
single-window compatibility bridge with a current Vaadin session and no opaque
scope registration, for both direct and captured calls, and
requires a capability result containing a JSON `null` value to settle without
leaving its caller blocked. It also requires an exact closed secondary tuple to
remain non-registrable after teardown while a fresh UUID for the same public id
registers normally.

The scope smoke also exercises the stdin admission fence across A→B→C use of one
public id. It admits and tears down A before B/new-UUID work, then tears down B
before C/new-UUID work. Late registered, unknown/future, duplicate-close and
capability-result frames for both A and B must be silently dropped, including
forged A and B results that reuse C's live correlation id. The correct C result
and a following C health request must still succeed; neither retired exact scope
may register again, the registry must retain only C, and a global shutdown
carrying A's retired tuple must still stop the loop.

`bash bin/native-host-retired-scope-smoke` separately exercises the production
macOS host registry. It requires full-tuple identity, simultaneous retention of
multiple retired generations under one public id, no time-based eviction, a
typed `owner-window-closed` disposition for retired commands, silent drops for
retired opaque request/push transport and normal admission of a fresh UUID.

A second deterministic close-race probe blocks an admitted A handler before its
first session access, lets the reader tombstone A, and releases it from an
independent main-window lane. Native registration must reject A and the context
transaction must remove its just-created push and Vaadin state before queued A
teardown and B/new-UUID creation run. B must succeed, the registry must contain
only B, no sidecar error may be emitted, and normal shutdown must complete.
Separate bridge assertions require a failed registration transaction to release
its exact session mapping without retiring the live default lifecycle, while a
close tombstone installed concurrently remains non-reactivatable.

## macOS Acceptance Build

Normal CLI and host builds contain only production sources. The repository-only
runner under `test-support` explicitly compiles the acceptance overlay and then
launches the resulting host.

Representative unattended framework run:

```bash
VAADIN_DESKTOP_MACOS_NO_ACTIVATE=1 \
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full \
VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 \
bash test-support/native-host-macos-acceptance/bin/run \
  dev test-apps/flow-over-ipc-framework-app
```

Acceptance flags, DOM inspection, snapshots and fault injection must remain in
test-support. Production host code may expose a neutral runtime observer, but
the production CLI, builder and launcher cannot know acceptance variables,
fixture ids or test-support paths.

For unattended runs, the test-support runner requires the completion trace for
the selected acceptance family and exits nonzero when the app closes without
reaching it.

Opaque push delivery acknowledgement has a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-push-delivery
```

The test-support overlay drops exactly the first delivery of a final
`UI.access` update, then generates no request, push or reconnect that could
otherwise reveal the loss. The gate requires Java to resend the same opaque UIDL
with the same transport sequence and Flow server sync id, the standard Flow
client to update the DOM, and the browser to acknowledge the matching sequence
and latest Flow sync id. The bridge must not dispatch duplicate retries to Flow.
This proves process-memory delivery recovery while the sidecar remains alive;
it does not prove persistence across sidecar replacement.

Opaque push recovery has a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-push-recovery
```

The gate requires an initial Java `UI.access` push, a scoped disconnect and
reconnect carrying an advanced Flow last-seen server sync id, a synthetic
browser sequence gap, the resulting opaque resync request and Java-generated
resync UIDL, then another normal `UI.access` push and visible DOM update. The
same window, session, UI and connection id must survive the cycle, push
sequences must increase, and shutdown must be graceful. This proves transient
reopen and gap recovery. Final in-flight loss without later activity is covered
by the separate delivery-acknowledgement gate; sidecar-replacement persistence
is outside both contracts.

In-flight opaque request interruption across unexpected sidecar replacement has
a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-in-flight-request-recovery
```

The test application uses a public raw Flow `UploadHandler` only as a large
request probe. Test-support waits until more than 256 KiB has reached the Java
process, kills that real sidecar, and requires the existing XHR to fail without
a request end, cancel, response or accepted partial body. The disconnected old
document must reject a later fetch and push reconnect locally without opening
new sidecar envelopes. One bounded recovery retains the native window and
WKWebView, reloads the document, establishes a fresh official Flow client and
session, and completes a distinct `4 MiB + 509 byte` request exactly before a
public Java listener and UIDL patch confirm it. The gate does not claim
automatic upload resume or durable transfer state across process replacement;
the interrupted request is terminally failed and the recovered request is new.

Large public `DownloadHandler` response cancellation and open-ended byte-range
resume have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-large-download-resume
```

The test application streams a deterministic `4 MiB + 113 byte` response using
only public Flow APIs. Test-support consumes at least 256 KiB, cancels the
browser response reader, then requests the remaining bytes with
`Range: bytes=<consumed>-`. The gate correlates the first successful response
with its opaque cancel frame and the second request with a `206` response and a
complete large response-end frame. It also requires the Java output stream to
observe cancellation, `Content-Range` to match the requested offset, exact
content reconstruction, a client-originated Java confirmation listener, the
returned UIDL patch, opaque push evidence and graceful shutdown. This is a
bounded transport contract, not automatic application retry policy; repeated
or multi-range requests, larger scale and recovery across sidecar replacement
remain outside it.

Large public raw `UploadHandler` request progress, cancellation and
re-selection retry have a separate focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-large-upload-retry
```

The test application accepts deterministic `4 MiB + 211 byte` content using
only the public `Upload` and `UploadHandler` APIs. Test-support starts the
component's normal XHR upload, observes browser progress past 256 KiB, dispatches
the component's abort action, waits for its normal file-removal lifecycle, then
re-selects an equivalent file and uploads it successfully. The gate correlates
the first large raw opaque request with request cancellation and a Java input
stream failure, and the distinct retry request with a complete request body and
successful response. It also requires exact bytes and metadata, the public Java
file-removed listener and returned UIDL patch, opaque push evidence and graceful
shutdown. This proves user/application re-selection after abort, not automatic
retry, resumable upload or durable recovery across sidecar replacement.
Multipart cancellation/re-selection is covered by the separate gate below.

Large public multipart `UploadHandler` parsing cancellation and re-selection
retry have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-large-multipart-upload-retry
```

The test application sends a deterministic `4 MiB + 307 byte` file through the
standard Upload `FormData` path. Test-support requires two multipart
`upload-request` events with the exact file entry, framing-aware progress past
256 KiB, normal abort/removal and a second selection. The gate correlates the
first unknown-length opaque request with cancellation and no request-end or
successful response. Servlet multipart form fields participate in request
parameter parsing, so Vaadin can reject the canceled request during session
lookup before `UploadHandler` is dispatched. The gate therefore requires that
the public handler and `responseHandled` see only the complete retry; it does
not require or synthesize a failed component result for the incomplete request.
A distinct retry request carries the complete multipart framing, parses the
exact file bytes and metadata, spills the file above the configured threshold,
invokes the handler once and reports one successful accepted file. Public
file-removed/all-finished listeners, returned UIDL state, temporary-part
cleanup, opaque push and clean shutdown are also required. The fixture,
component IDs and automation are test-only; host and adapter production code
contain no Upload-specific branch. This bounded gate does not prove every
Servlet multipart edge case, higher-fan-out concurrency, repeated cancellation,
automatic or resumable retry, or process-replacement recovery. Same-session and
cross-session isolation are covered separately below.

Same-window concurrent raw Upload request isolation has a focused real-WKWebView
entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-concurrent-upload
```

The test application starts two files through the standard `Upload` client API.
Both raw opaque requests must be open before either terminates. After both make
progress, test-support aborts one file through Vaadin's normal abort/removal
path while the sibling continues. The gate correlates the canceled and complete
request IDs independently, rejects a late completion or successful response for
the canceled request, and requires the sibling request to end with its exact
`3 MiB + 149 byte` body and a successful response. Browser overlap/progress,
abort/removal/success cardinality, concurrent Java `UploadHandler` execution,
exact deterministic content, public Java file-removed/all-finished listeners,
the returned UIDL patch, opaque push and graceful shutdown are all required.
The fixture and automation are test-only; the host and adapter remain unaware
of Upload semantics. This is a bounded two-file raw-upload contract, not proof
of automatic or resumable retry, larger scale or process-replacement recovery.
Standard queue saturation and repeated cancellation across two consecutive
batches are covered separately below.

Same-window opaque request fan-out has a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-request-fanout
```

The test application submits ten consecutive batches of six uniquely sized raw
files through the standard Upload client. Vaadin's production `UploadManager`
keeps its default `maxConcurrentUploads=3`, so each batch requires exactly three
browser and Java requests in flight while later requests replenish the queue.
Two requests per batch are canceled. The first two large batches trigger at
`512 KiB`; the remaining eight compact churn batches trigger at `192 KiB` and
require at least `160 KiB` to reach Java, beyond the opaque transport's initial
`128 KiB` credit window. The forty survivors complete an exact aggregate
`33,640,688` bytes. After the first batch, test-only automation
captures the actual opaque request IDs, injects four late response frame kinds
for one canceled request and requires its cancellation tombstone to absorb all
of them, then verifies a completed survivor ID is no longer active before the
second batch starts. The same canceled ID must still absorb response metadata
about `55` seconds after the actual XHR abort, become an exact `unknown request`
after the production `60`-second cleanup, and leave the bridge healthy for the
following nine batches. The inspector correlates all sixty IDs by content
length, requires every batch to terminate before the next opens, reconstructs
the three-active queue independently in all ten batches, and rejects
end/success for canceled requests or inexact survivor completion. Browser
events, concurrent Java handlers, ten public all-finished cycles, UIDL, push
and clean shutdown are required. Upload is only the test probe; no product
layer branches on it. This is bounded ten-batch same-window churn evidence,
not arbitrary-duration or higher multi-window fan-out, automatic/resumable
retry or process-replacement recovery.

Same-window concurrent multipart request isolation has a separate focused
real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-concurrent-multipart-upload
```

The test application starts two files through the standard multipart Upload
client path. Both browser requests and opaque streams must be open and making
progress before one file is aborted. Vaadin's normal session lock covers
multipart parameter lookup, so the gate requires serialized same-session
parsing with one request-scoped temporary directory at a time, rather than
inventing parallel parser semantics. The canceled request must terminate
without application-handler dispatch or a successful response; the sibling
must then parse exact bytes and metadata, spill above the configured threshold
and return one successful public result. Browser events, public
file-removed/all-finished listeners, UIDL confirmation, opaque push, final
temporary-root cleanup and graceful shutdown are required. The separate gate
below covers bounded two-window parser concurrency; higher fan-out, repeated
cancellation and process-replacement recovery remain outside this gate.

Cross-session multipart parser concurrency has a separate focused
real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-cross-session-multipart-upload
```

The test application opens the same public multipart Upload route in two native
windows with distinct window, session and UI identities. Both standard
`FormData` requests must advance beyond the initial opaque request credit while
the shared acceptance storage observes at least two simultaneous
request-scoped directories and spill files. Test-support then aborts the main
window file and requires no application-handler dispatch or successful
response for that scoped request. The secondary window must parse its exact
`10 MiB + 263 byte` file, return one successful public result and complete Java
listeners, UIDL, push and temporary-part cleanup in both sessions. Inspector
correlation includes window, session, UI and request id so request-id reuse
between WebViews cannot merge the streams. This remains a bounded two-window
contract; higher window/file fan-out, repeated cancellation and
process-replacement recovery remain open.

Window-scoped capability manifest enforcement has a separate focused
real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-permission
```

The public desktop API issues the same `window.setTitle` command from two
isolated Flow sessions. The manifest permits it for `main` and denies it for
`permission-secondary`. Two generated AppKit primary clicks must reach the
standard Flow Button listeners as trusted client events and produce a UIDL
update round trip in each real WKWebView. The allowed command changes only the
main native window title; the denied command returns its error and complete
permission audit payload to Java while leaving the secondary title unchanged.
Full-detail trace inspection dynamically correlates each command/result pair by
correlation id and exact window/session/UI scope, then checks the allow/deny
payloads. Navigation and opening the secondary window are the only
browser-programmatic setup stages; the gate does not make raw envelope counts a
contract.

The production default for an application with no source capability manifest
has a separate focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-default-deny
```

The CLI selects the fallback policy explicitly: `dev`, `dev-vite` and
`dev-compat` request `allow-all-dev`, while `package` requests
`manifest-enforced`. The product macOS builder itself has the secure
`manifest-enforced` default. When the application does not provide
`desktop-capabilities.json`, that builder stages a valid empty grant set:
`{"schemaVersion":1,"permissionMode":"manifest-enforced","windows":[]}`.
For an explicit product manifest, the builder strictly parses JSON once into a
builder-private canonical snapshot that remains immutable for staging, validates
`permissionMode` from that snapshot and stages only the snapshot. Invalid JSON,
XML property lists, a missing or unsupported mode, and explicit `allow-all-dev`
or `trace-only-dry-run` are rejected before replacing an existing staged
application. Duplicate keys are collapsed to the JSON parser's single
runtime-effective value: a document with an earlier permissive value and final
`manifest-enforced` value stages one enforced key and no permissive token.
Development staging continues to accept explicit `allow-all-dev` and
`trace-only-dry-run` manifests.

The runner uses a test-support-only omission hook to withhold the framework
fixture's source manifest, then exercises the real product builder and runtime.
One generated AppKit primary click must arrive at the standard Flow Button as a
trusted client event. The Java listener receives the complete denied permission
payload, returns the status through a UIDL response and leaves the native title
unchanged. Full-detail inspection requires the one empty enforced-manifest trace
to precede the unique `window.setTitle` command/result pair, whose dynamic
correlation id and exact window/session/UI scope must match. Navigation is the
only browser-programmatic stage; focused push is not claimed, and graceful
sidecar shutdown is required.

Explicit permissive-manifest package rejection has a focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-package-policy
```

The runner invokes the real product builder once with an explicit
`allow-all-dev` manifest and once with an explicit `trace-only-dry-run`
manifest. Each negative probe must exit with status `2`, emit
`Product capability manifest must use permissionMode=manifest-enforced; found: <mode>`,
and retain byte identity for a pre-existing staged enforced manifest and marker.
It then packages the real framework application and mutates only its staged
capability manifest through malformed JSON, unsupported schema, unsupported
mode and invalid window-entry cases. Each direct production-host probe must exit
`2`, emit its exact stable diagnostic, and pass
`--require-flow-ipc-capability-manifest-startup-failure`: exactly one manifest
diagnostic followed by its matching error, with no host `launch`/`ready`, sidecar,
Java or window event. The runner restores the staged manifest before executing
the complete source-missing default-deny gate above. These negative probes do
not launch a WKWebView; the delegated gate provides the real-WKWebView, trusted
AppKit click, Java, UIDL, trace and graceful shutdown evidence.

`bash bin/cli-contract-smoke` separately covers the broader builder contract. It
requires explicit enforced documents and duplicate-key documents to stage as
canonical JSON, with exactly one effective `permissionMode` and retained
`schemaVersion`/`windows` semantics. It also requires invalid JSON, XML property
lists, missing mode and unknown mode to fail before destructive staging, while
development mode continues to stage both explicit permissive modes. The focused
package-policy runner does not duplicate those CLI smoke probes.

This focused gate does not prove a complete native permission model or full
product parity. Product source JSON and permission-mode failures are rejected
at build time. Removing the staged resource remains startup-fatal, and
parser-invalid staged JSON, schema, permission modes or window entries now fail
before `NSApplication`, sidecar, Java or window creation. The gate does not
prove signature-backed protection against replacing the staged file with a
different but syntactically valid manifest. App Sandbox/Powerbox enforcement,
persistent bookmarks and cross-process grant recovery, broader Clipboard formats
and pasteboard contention,
macOS authorization states, broader asynchronous
progress/cancellation/retry/resume contracts, physical input hardware, touch
and assistive-technology interoperability remain open.

Public FileDialog terminal outcomes have a separate focused real-WKWebView
entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-dialog-outcomes
```

The first generated AppKit primary click reaches a client-originated Flow
listener, which calls `Desktop.current().captureCapabilityContext()` before
passing that immutable facade to a background virtual thread. The thread has no
current Vaadin session (`backgroundSessionAbsent=true`), invokes public
`DesktopFileDialog.openFileResult` through the captured main context and opens
one real attached, visible `NSOpenPanel`. After the runner has observed that
unique panel, a second generated trusted click invokes the same public API
concurrently. It must return `ERROR` with
`File dialog already in progress`, preserve the complete allowed permission
audit and leave the original panel as the only visible panel. Test-support then
calls `NSOpenPanel.cancel(nil)`; the resulting normal AppKit completion must
return public `CANCELED`, detach and hide the panel, and update the Flow UI
through a main-scoped automatic opaque push with a matching scope-exact ack.
This deterministic panel action is not a physical user click on the Cancel
button, and the older automatic panel-cancel hook is disabled.

Browser-programmatic setup opens a dedicated secondary whose manifest scope
does not grant `fileDialog.openFile`. A third generated trusted AppKit click in
that opaque-UUID Flow session captures the secondary context, passes it to a
background thread with `backgroundSessionAbsent=true`, and must issue the
denied command in the exact secondary scope. It returns public `DENIED`, retains
the complete denied permission audit in the raw result, creates no panel and
updates through a secondary-scoped automatic opaque push with a matching
scope-exact ack. Full-detail inspection with
`--require-flow-ipc-file-dialog-outcomes` requires exactly three uniquely
correlated commands/results, exact dynamic window/session/UI scopes and payloads,
one panel observation, the `ERROR` result before panel cancellation, the
`CANCELED` result before its main-scoped push/ack, the denied result before its
secondary-scoped push/ack, balanced quiet drain, no recovery and graceful
shutdown.

`openFileResult` uses the stable precedence `INVALIDATED`, `DENIED`, `CANCELED`,
`SELECTED`, then `ERROR`; it exposes the selected path, optional cancel reason
and original command result. The legacy `openFile` method delegates and still
returns `Optional.empty()` for every non-selected outcome. The core contract
smoke in `bash bin/flow-ipc-scope-smoke` covers selection normalization and
compatibility, while the owner-close gate below now requires the public API to
return `INVALIDATED`. This focused runner does not select a file or prove
owner-scoped file access; that has the separate gate below. It also does not
prove physical input, broader asynchronous progress/cancellation/retry/resume
behavior, broader OS authorization or full FileDialog/product parity.

Process-lifetime read-only selected-file grants have a focused real-WKWebView
entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant
```

Browser-programmatic setup navigates the retained main document to the public
fixture route. Seven generated AppKit primary clicks then open an isolated
secondary window, invoke two selections plus read/release/read-after-release in
that exact secondary scope, and close the secondary from main. Both selections
use real attached, visible `NSOpenPanel` sheets. Panel selection is interactive:
an operator or external native UI driver uses the system Go to Folder UI to
select the temporary fixture file. Test-support waits until the canonical
`panel.url` matches the fixture exactly and emits `flowFileReadGrant.panelObserved`;
the operator or UI driver then confirms with the real Open control. It does not
invoke `NSSavePanel.ok(_:)` or depend on the panel's internal view
hierarchy or inject panel key events. The trace labels this
`external-native-panel-selection`; it does not prove physical Finder, keyboard
or mouse input.

The runner allows 300 seconds by default, configurable through
`VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS`. While its panel is open,
locate this run's file under
`${TMPDIR:-/tmp}/vatori-file-read-grant.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`.
Use Command-Shift-G, enter the absolute file path, then Return to select it.
After the matching `panelObserved` event, press Open; repeat for the second
panel. Keep this operator-only path out of the host trace.

Run this gate in an unlocked macOS desktop session. macOS activation is
cooperative: the host must be frontmost and key before the generated native-input
stages. Bring its title bar to the foreground if another
application retains focus; the gate waits and fails instead of dispatching an
event to an inactive window.

The public Flow listeners capture their capability context before background
virtual-thread work observes no current Vaadin session. The first typed result
must be `SELECTED`; a 19-byte bounded read must be `DATA` and match the known
fixture bytes without putting those bytes in trace; explicit release must be
`RELEASED`; and a later host call through the same Java handle must be
`UNAVAILABLE`. A second `SELECTED` grant is intentionally left open. Closing
its owner must remove the exact secondary context and make the host trace an
owner-close invalidation followed by handle closure.

The runner invokes `--require-flow-ipc-file-read-grant`. The gate dynamically
correlates all five grant commands/results with the secondary's opaque UUID
scope, requires two distinct grant lifecycle diagnostic ids, explicit-release
and owner-close cleanup, two real-panel observations with the external selection
mode, seven trusted generated pointer receipts, scope-exact UIDL completion,
balanced drain, no recovery and
graceful shutdown. It also rejects any raw grant UUID, path, fixture basename
or fixture content in the full-detail trace; read payloads must retain only
redacted byte counts. Content scanning includes the fixture's Base64-encoded
read body in raw diagnostic lines and alias fields, not only the canonical
`bodyBase64` field. The Java contract smoke in
`bash bin/flow-ipc-scope-smoke` covers classification precedence, malformed
payloads, defensive byte copies, captured-bridge retention and local
double-release semantics.

This gate records `sandboxEnforced=false`. A successful run closes host-owned
bounded read, explicit release and owner-window-close cleanup in the exact
selecting scope, not the whole process-lifetime invalidation or App
Sandbox/Powerbox boundary.

Sandbox-selected read access has a separate entry:

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-sandbox
```

The runner builds the same standard Flow application and an acceptance-only
host, then copies the app and the local `JAVA_HOME` runtime into a unique
temporary directory. `VAADIN_DESKTOP_ACCEPTANCE_JAVA_HOME` can select another
complete local JDK. The source JDK is not modified or re-signed. Only the copied
host receives three entitlements: App Sandbox, user-selected read-only files,
and outbound network client access needed by WebKit. This is test staging,
not an embedded-JRE distribution or production-signing contract. The default
host remains unsandboxed.

The external runner creates two private regular files outside the app and its
container, verifies their ownership, distinct identity and single-link status,
and prints the exact selected-file path. The host does not create these files.
After official Flow startup, the host checks its exact signed entitlement set
and actually attempts `open(O_RDONLY | O_CLOEXEC | O_NOFOLLOW)` on both files;
both must fail with `EPERM` before any panel opens. Mere nonexistence or a false
`fileExists` result cannot pass.

Bring the app to the foreground if needed. In the genuine native open panel,
use Command-Shift-G, enter the printed file path and press Return. Wait for
`flowFileReadGrantSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-read-grant.out.log`, then click
Open. Select only the file, never its parent directory. The driver passively
checks the selected URL; it never selects or confirms the panel internally.

Four generated AppKit clicks exercise the existing public typed API and Flow
listeners: `SELECTED`, a content-checked 19-byte `DATA` read, `RELEASED`, and
post-release `UNAVAILABLE`. Between read and release, the same host PID must
still receive `EPERM` for the unselected sibling. The separate
`--require-flow-ipc-sandbox-file-read-grant` gate correlates those policy and
access observations with one window/JVM generation, command/results, the
grant's handle/scope cleanup, trusted event receipts, Java/UIDL/push updates,
balanced requests and graceful shutdown. The trace must not disclose selected
or sibling paths, grant tokens or fixture bytes. Push checks correlate sequence,
scope and delivery attempts, allowing the transport's legitimate redelivery
and asynchronous client-sync acknowledgements; visible typed status is checked
independently after each Java update.

The default runtime deadline is 300 seconds, adjustable from 1 to 3600 with
`VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS`; the runner adds an
independent 30-second launch/cleanup margin. It retains the package lock until
inspection and exact-owned temporary app/JVM/fixture cleanup finish. The
non-GUI runner smoke verifies fresh logs, external fixture provenance, failure
propagation, source-JVM preservation and cleanup without launching an app.

This focused gate proves the selected read path under an actual test Sandbox
policy. It does not prove write grants, persistent bookmarks, cross-process
grant recovery, Finder or physical/assistive input. Token release and owned
handle closure do not imply that every OS-issued Powerbox extension is
immediately revoked for the process; raw-path denial after release is not an
assertion of this gate.

Sandbox-selected create-new write access has its own entry:

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-sandbox
```

The runner uses a fresh acceptance-only app/JDK clone and exactly App Sandbox,
user-selected read-write and network-client entitlements. It leaves the source
JDK and default product policy unchanged. The external runner creates only an
unselected sibling; the selected target initially does not exist. Before any
panel, the same sandboxed host must actually fail exclusive creation with
`EPERM`, and fail reading the sibling. `ENOENT` is not accepted as enforcement.

In `Flow file write grant new file`, navigate to the printed target using
Command-Shift-G and set its exact filename. Wait for
`flowFileWriteGrantSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant.out.log` before Save.
After automatic write/release stages, the panel titled
`Flow file write grant canceled` must point to the same external directory and
`canceled-write-grant.bin`; wait for its `panelObserved`, then Cancel.
Neither panel is selected or confirmed programmatically by the driver.

Seven generated AppKit clicks require typed `SELECTED`, `WRITTEN`,
`INVALID_RANGE`, `WRITTEN`, `RELEASED`, `UNAVAILABLE`, `CANCELED` results through
captured public capability facades. The first write is a 65,536-byte nonuniform
binary block; a rejected negative offset leaves the grant usable for a six-byte
tail. The same host still cannot read the sibling after writing. The gate
`--require-flow-ipc-sandbox-file-write-grant` correlates policy, access probes,
commands/results, redacted byte counts, native lifecycle, trusted event receipts,
visible Java statuses, opaque request/push updates and graceful shutdown.
After exit, an external verifier compares all 65,542 bytes, unchanged sibling
content and the absent canceled target. Runner smoke covers fresh logs,
launch/gate/byte-verification failure, timeout and exact-clone cleanup.

Existing-target selection remains pending in this gate, avoiding a replacement
confirmation. `bash bin/native-host-file-write-smoke` covers exclusive creation,
existing-file/symlink refusal, strict ranges, bounded decoding, interrupted and
partial writes against the native helper. These are not real Sandbox Replace
panel or write-lifecycle race evidence. No atomic publication, fsync guarantee,
rollback, overwrite, persistent grant, retry/resume, physical/AT or Finder
coverage is claimed. Release closes the owned descriptor; it neither removes
the file nor promises immediate revocation of every OS-issued path extension.

Explicit write-grant cancellation has a separate focused Sandbox entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-cancel-sandbox
```

Select the runner's exact new-file path in `Flow file write grant new file`,
waiting for `flowFileWriteGrantCancelSandbox.panelObserved` before Save. This
entry has one real Save panel and no canceled or Replace panel. Seven generated
AppKit clicks require `SELECTED`, `WRITTEN`, `INVALID_RANGE`, `WRITTEN`,
`CANCELED`, `ALREADY_CANCELED`, `UNAVAILABLE`. Only six native commands are sent:
the second cancellation reuses the confirmed Java result and its subsequent
`close()` is also local. The manifest allows selection, write and cancel,
not release. Native cancellation must close exactly once with final offset
65,542; later writes must be denied. The gate
`--require-flow-ipc-sandbox-file-write-grant-cancel` correlates these outcomes,
trusted listener receipts, opaque updates, scoped lifecycle, three actual
EPERM probes and graceful shutdown. The external verifier compares all output
bytes and the unchanged sibling after exit.

This gate deliberately cancels after writes settle; it does not claim a real
blocked-syscall cancellation. `bin/native-host-file-write-smoke` separately
checks pre-first-write, partial-write, EINTR, full-completion and I/O-error
races, cross-thread cancellation and the final-prefix close barrier through
deterministic syscall seams. Java contract smoke checks unknown prefixes,
concurrent write/cancel, terminal-call serialization and interrupted waits.
These checks do not prove all owner-close/Quit/replacement races, physical/AT
input, rollback, fsync, retry or resume.

Active write-grant cleanup when its secondary owner closes the last window has
a separate focused real-window entry. Run it on an unlocked interactive macOS
desktop:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-owner-close-sandbox
```

Browser-programmatic setup uses public window APIs to open `file-write-owner`
and close `main`. The secondary then runs selection and the first three write
operations above, under the same three-entitlement test policy. Its manifest
does not allow `fileGrant.releaseWrite`. Operate only the first Save panel and
wait for `flowFileWriteGrantOwnerCloseSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-owner-close.out.log`
before Save; no Cancel or replacement panel follows.

After four trusted generated AppKit stages and balanced secondary transport,
a fifth generated click delivers `event.isTrusted` to a standard Java DOM
listener. The listener records a native title result and publicly closes the
owner while retaining its grant. The gate
`--require-flow-ipc-sandbox-file-write-grant-owner-close` requires owner removal,
`owner-window-closed` invalidation, descriptor closure at offset 65,542,
security-scope stop, successful shared cleanup drain, and graceful
shutdown with both window contexts absent. An independent five-second deadline
bounds final-close hangs; the external verifier checks that every written byte
and the sibling survive. The secondary has its own executor, unlike `main`,
which reuses the host's retained executor. The focused proof is a successful
cleanup completion receipt after owner `windowClosed`, not an ARC deallocation
point. A close completed before drain start must pair with pending zero; a close
completed during the drain must pair with pending one. Only the first ordering
has real-window coverage; the second has synthetic gate coverage. The AppKit-wide
visible-window count is diagnostic, not proof of registered Flow window presence.
Same-sequence push retries may already be in flight before the preceding ACK;
the gate checks identity, counts and each delivery-before-ACK relation without
requiring alternating packets. Writes settle before close: in-flight/blocked
I/O, timeout, Quit/replacement races and durable commit remain outside this gate.
No final DOM response after owner destruction is required.

Active write-grant cross-window isolation has a separate real-window Sandbox
entry, requiring an unlocked interactive desktop:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-isolation-sandbox
```

One browser-programmatic route navigation prepares eight generated AppKit
stages. Public Java listeners capture the clicking window's raw `NativeBridge`,
then invoke it off the Flow session thread and publish results through
`UI.access`. Main opens `file-write-grant-isolation-secondary`; that owner
selects a new target in the panel titled `Flow file write grant owner isolation`.
Choose the exact printed path and wait for
`flowFileWriteGrantIsolationSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-isolation.out.log`
before Save. There is no second Cancel or Replace panel. If the desktop does
not activate the app automatically, bring its window to the foreground; the
driver waits for the actual active/key state before dispatching native input.

The owner first writes 65,536 bytes. Main then attempts a valid six-byte write
at offset 65,536 and a release using the same token. Both scopes explicitly
allow write/release; both foreign results must instead be `UNAVAILABLE`, with
no progress fields. The original owner must still append the expected six-byte
tail at that offset and release, after which main closes the secondary. Token
and actual command argument equality are checked privately in the test app.
The gate `--require-flow-ipc-sandbox-file-write-grant-isolation` correlates all
eight scoped command/results and trusted listener receipts, exact lifecycle,
opaque request/push/ACK and retained native window/WebView identities through
release. Three actual EPERM probes and the same three-entitlement write policy
establish the Sandbox boundary; the external verifier independently checks all
65,542 bytes and the unchanged sibling. Runner smoke covers all six write
modes, mutually exclusive driver flags, fresh logs and exact-clone cleanup.
This is sequential live-grant isolation, not in-flight races, persistence,
mutually untrusted Java code, physical/AT input or a public-last-window-close
proof. Final main close is performed by the acceptance driver after balanced
transport.

Settled write-grant cleanup on unexpected JVM exit has a separate real Sandbox
entry, also requiring an unlocked interactive desktop:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-sidecar-exit-sandbox
```

This entry has focused real-window Sandbox coverage with the standard production
bundle and opaque Flow IPC. The host must actually become active/key before any
generated input is dispatched; external Computer Use operates the real Save
panels. This is not physical keyboard/mouse or assistive-technology evidence.

The runner prints two distinct new-file paths in `original` and `recovered`
subdirectories of its external fixture root. Both real Save panels are titled
`Flow file write grant new file`. Use the original path first and the recovered
path only after the JVM has restarted; neither target exists before selection.
Wait for `flowFileWriteGrantSidecarExitSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-sidecar-exit.out.log`
before confirming each Save. There is no Cancel or Replace panel.

Twelve generated AppKit clicks drive public Java listeners: main opens the
owner; old selection, first block, negative-offset rejection and tail settle;
the driver kills only that exact JVM PID without releasing the grant. Both
original windows, WebViews and scopes must remain through one bounded recovery.
Browser-programmatic setup restores the main control route; fresh Java state
is checked before the second selection, first block, negative-offset rejection,
tail, release and post-release rejection. Main then publicly closes the owner;
the driver closes main after balanced transport.

`--require-flow-ipc-sandbox-file-write-grant-sidecar-exit` correlates both JVM
generations, twelve native command/results and grant lifecycle receipts, trusted
pointer/Java outcomes, two external panels and per-generation opaque
request/push/ACK evidence. Old invalidation must precede recovery scheduling;
old descriptor closure may follow new JVM ready but must finish successfully
with matching security-scope stop before acceptance. The late-close ordering is
covered synthetically, not by the focused real-window proof. Each generation has
actual target-before-selection and sibling-before/after-write EPERM probes under the
same three-entitlement policy. After exit, the external verifier compares all
65,542 bytes of each file and both siblings. Runner smoke additionally rejects
missing/corrupt recovered output or sibling changes and checks cleanup of both
fixture directories. This focused proof targets settled cleanup and fresh
authorization, not old-token replay/restoration, in-flight races, persistent authorization, forced
I/O failure, OS-wide revocation, physical/AT input or durable commit.

Old write-token replay after unexpected JVM recovery has a separate Sandbox entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-replay-sandbox
```

This gate has focused real-window Sandbox coverage through the standard
production bundle and opaque Flow IPC. It uses public raw `NativeBridge` calls
in captured owner contexts, not an
owner-bound typed grant transferred to a different JVM. Fourteen generated
AppKit stages extend settled writes and one exact-PID failure with two recovered
old-token submissions before the fresh Save. Both external panels are titled
`Flow file write grant new file`; select the printed `original` then `recovered`
path, waiting for `flowFileWriteGrantReplaySandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-replay.out.log` before
each Save. The runner defaults to a 300-second interactive deadline.

The original selection privately writes its UUID and PID to one bounded 0600
record in a newly created 0700 directory under the actual app container's
`Data/tmp`. The native driver verifies container binding before selection;
neither selected-file directory authorizes this handoff. No entitlement is added.
After recovery, the Java listener verifies the producer PID differs and compares
each actual command argument with the retained token without publishing either.
It attempts a six-byte write at the valid next offset 65,542, then release.

`--require-flow-ipc-sandbox-file-write-grant-replay` requires both denials to be
permission-allowed, have no write progress/release effect, and correlate with
the original grant's diagnostic identity and `sidecar-exit` retirement reason
in generation two and the same owner scope. Unknown random tokens, wrong owner,
permission denial or invalid-range results cannot substitute. A distinct fresh
token then writes from zero, releases and rejects a post-release write. The
external verifier checks every byte of both files and both unchanged siblings,
so replay must not append or alter the old output.

The helper smoke covers private record validation; runner smoke covers six-mode
routing and bounded handoff cleanup, including symlink/hard-link/wide-permission
refusal and retention of unexpected children. Successful fresh release consumes
only the verified record; runner cleanup removes its empty unique parent and,
on failure, only the known owned record. It never recursively deletes the shared
container. This is not secure erasure, grant persistence/restoration, direct
active-map generation-mismatch injection, in-flight I/O, physical/AT input or
fsync durability. A temporary record's forced flush is not a write-grant commit
guarantee.

Settled write-grant cleanup on normal AppKit Quit has a separate Sandbox entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-write-grant-host-shutdown-sandbox
```

This has focused real-window Sandbox coverage through the standard production
bundle and opaque Flow IPC, with external Computer Use operating Save and Quit.
It reuses the public typed write route and its main-window controls. Five
generated AppKit clicks open the secondary, select a new file, write the first
64 KiB, reject a negative offset and append six bytes. Select the printed new
target in `Flow file write grant new file` and wait for
`flowFileWriteGrantHostShutdownSandbox.panelObserved` before Save. There is no
Cancel, Replace, explicit release or window-close step.

After `flowFileWriteGrantHostShutdownSandbox.hostShutdownPrepared` appears in
`.build/native-hosts/macos/host-sandbox-file-write-grant-host-shutdown.out.log`,
invoke `Acceptance > Quit Vaadin Desktop Host` externally. This test-only menu
uses the standard `NSApplication.terminate(_:)` action; the driver never invokes
Quit. Both original windows, WebViews and scopes must still be present at the
actual `applicationWillTerminate` observation. The original JVM must already
have exited normally, with its exact PID absent at that callback.

`--require-flow-ipc-sandbox-file-write-grant-host-shutdown` requires exact policy,
three EPERM probes, one real panel, five scoped native command/results and
listener/opaque request/push/ACK evidence. The unreleased grant must be
invalidated with `host-shutdown` after preparation, enter a one-pending cleanup
drain, close successfully with matching scope stop, and complete drain with no
pending work or failures before the original JVM's actual exit, graceful
shutdown receipt and final termination observation, in that order.
Preparation requires balanced scoped requests and verified push delivery. While
waiting for external Quit, ordinary traffic may advance; the gate validates its
causal request/response and retry/ACK prefix without requiring outstanding UI
traffic to finish after the host's termination fence. No opaque envelope may
follow grant invalidation. This does not claim in-flight file-write coverage.
Truncated request/retry edge cases are covered by synthetic inspector smoke;
they are not additional real-window transport or file-I/O claims.
Owner-close substitutions, release, recovery, cleanup failure/timeout and early
shutdown are rejected. Optional AppKit teardown is allowed only after graceful
shutdown as exact original-scope close/dropped pairs, never successful Java
window-lifecycle sends. The external verifier checks all 65,542 bytes and the
unchanged sibling after host exit. The shared runner owns build/run/inspection
under its package lock and cleans its exact app/JDK clone and fixture root.

This is settled-write cleanup, not blocked/in-flight I/O, durable commit,
physical/AT input, Dock-origin Quit or a production application-menu API. The
default interactive deadline is 300 seconds.

Active-grant cleanup on unexpected sidecar exit has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-sidecar-exit
```

This shares the public fixture and native driver with the base gate. After the
first real selection is visible and requests have drained, it sends SIGKILL to
the exact original sidecar PID without reading or releasing that grant. The
host must revoke and close the old-generation grant before final acceptance,
including stopping its acquired security scope. Recovery retains both original
windows, WKWebViews and exact scopes, replaces the JVM, and resets Java view
state. Browser-programmatic setup navigates the recovered main document back
to the fixture; the retained secondary receives a fresh native selection,
19-byte read, explicit release and post-release `UNAVAILABLE` result.

The default deadline is 600 seconds with the same timeout override. Select the
file under
`${TMPDIR:-/tmp}/vatori-file-read-grant-exit.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`
using the same external Command-Shift-G/Return/Open procedure. Both panels have
the title `Flow file read grant explicit release`; wait for `panelObserved`
stages `first` and `recovered` before confirming each. The driver never selects
or confirms either panel internally. The runner holds the package lock across
build, execution, inspection and temporary-fixture cleanup.

`--require-flow-ipc-file-read-grant-sidecar-exit` correlates exactly one injected
exit/recovery, two actual JVM generations/PIDs, two original window creations
and four initial/recovered loads, eight grant lifecycle events and five grant
command/results. Correlation includes generation because a fresh JVM may reuse
command ids; security-scope acquisition/stop is checked independently for each
grant. Seven trusted generated AppKit pointer receipts, two external panel
observations, balanced scoped UIDL advances and graceful shutdown are required.
The old handle may close asynchronously after the new JVM becomes ready, but
must be invalidated before recovery is scheduled and closed before acceptance.
The same full-trace privacy gate applies. Smoke positives include reused ids,
different acquisition results, a delayed old close and a single recovered-main
UIDL advance; negatives reject missing cleanup, stale scope/generation,
reconstructed windows, false continuity, missing panels and leaked secrets.
This is focused exit cleanup and fresh-access evidence, not old-grant recovery,
physical/Finder input, App Sandbox/Powerbox enforcement, replacement/shutdown
cleanup, stolen-token isolation or an in-flight read/release race.

Across the unsandboxed base and sidecar-exit gates, successful AppKit security-scope acquisition and matching
scope stop on release, owner close and sidecar exit have focused coverage,
without establishing sandbox enforcement. Persistent security-scoped bookmarks,
broader write-grant lifecycle, cross-process grant recovery,
Finder-origin drops, physical selection and broader
progress/cancel/retry/resume behavior remain open.

Active selected-file grant cleanup on normal AppKit application termination has
a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-host-shutdown
```

The shared driver navigates main programmatically, opens the secondary and
invokes its first selection using two trusted generated AppKit pointer events.
Externally select the current fixture under
`${TMPDIR:-/tmp}/vatori-file-read-grant-quit.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`
in the real panel using Command-Shift-G, Return and Open after the `first`
`panelObserved` event. The grant remains active: no read, release, second
selection or owner close is allowed. After two unchanged balanced polls,
`flowFileReadGrant.hostShutdownPrepared` records the visible Java `SELECTED`
result, live owner windows, exact PID/scopes/identities and input provenance.

Only this acceptance mode installs a test-only AppKit menu item,
`Quit Vaadin Desktop Host`, wired directly to `NSApplication.terminate(_:)`.
After Prepared, invoke that item externally (its key equivalent is Command-Q).
The driver only installs the menu and observes termination; it never invokes
Quit itself. This is a way to exercise normal AppKit termination, not evidence
that the production application-menu API is implemented, physical keyboard
input or Dock-origin Quit. Do not close either owner window to end this test.

`--require-flow-ipc-file-read-grant-host-shutdown` requires one original JVM,
two live owner windows, one panel/selection, two trusted pointer receipts and
balanced scope-specific UIDL completion. The grant must be invalidated with
`host-shutdown`, closed successfully, and its successful security-scope
acquisition matched by scope stop. Direct host drain traces must show one
pending cleanup followed by `drained=true`, zero pending work and zero failures.
Only then may the original JVM actually exit, followed by the single graceful
sidecar shutdown receipt and `applicationWillTerminate` observation. That
callback must see the original PID absent while retaining both window owners.
Optional AppKit final window teardown is allowed only after that observation,
with at most one exact original-scope
close/dropped pair per window and no successful Java lifecycle send. The gate
rejects earlier owner-close substitutions,
early shutdown, missing/late cleanup, unsuccessful or timed-out drain, recovery,
extra operations, false continuity and the same full-trace privacy violations
as the other read-grant gates. The package lock covers build/run/inspection;
the runner removes its temporary fixture root after exit, even on failure.
The deadline defaults to 600 seconds with the same timeout override.

`bash bin/native-host-resource-cleanup-smoke` separately tests the production
tracker's asynchronous drain, retired-owner bookkeeping, duplicate/reentrant
completion, failures and timeout/late-completion semantics. It also enters a
modal-panel run loop from inside a main dispatch callback and checks empty and
repeated drains, background-worker completion and timeout/late completion.
An independent background deadline prevents a broken scheduler hanging the
smoke. Its owner objects
are test doubles; the real-window gate supplies actual file-handle evidence.
Neither proves blocked filesystem I/O cleanup, a real in-flight read/release
race, fatal `exit(1)` cleanup (covered separately for unsupported-version with
an idle grant), forced child termination, App Sandbox/Powerbox,
old-token replay rejection or physical input.

Public final-window close has a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-last-window-close
```

The driver navigates to the lifecycle route programmatically. A generated
AppKit click prepares closure and must produce a trusted browser receipt,
`isFromClient=true` Java status and visible UIDL patch. After a balanced quiet
transport interval, another generated AppKit click invokes the public Java
title and `windows().close("main")` APIs. The success path never calls host
`window.close` or `terminate` from the driver.

`--require-flow-ipc-last-window-close` correlates the original PID and sole
window scope, native dispatches, pre-close opaque exchanges, prepared state,
native title/close command results, actual window removal, successful empty
resource drain, application termination observation and graceful shutdown.
The terminal browser response need not complete after its window is destroyed.
The termination observation checks that the original context left the host
registry; AppKit may still report the closing NSWindow visible while its
`windowWillClose` frame is active. An independent background five-second
test-only deadline fails with a fatal exit if close cannot terminate, so the
main-queue starvation regression cannot also starve its own failure fence.
Such a fatal exit is not accepted as graceful completion.
This is a lifecycle gate with no active file grant or focused push requirement,
not physical pointer, product application-menu, blocked-I/O or Sandbox proof.
The runner holds the package lock through build/run/inspection and clears its
logs before running to reject stale evidence.

Live selected-file grant isolation between two windows has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-isolation
```

The runner stages an explicit test-support capability manifest: main is allowed
to read and release grants, and the selecting secondary is allowed to select,
read and release them. The regular test application's main policy is unchanged.
This distinguishes owner isolation from an earlier manifest denial. The same
production builder and immutable runtime manifest parsing apply; the override
is an input to the explicit acceptance build, not a production host test flag.

Select the current 48-byte fixture under
`${TMPDIR:-/tmp}/vatori-file-read-grant-isolation.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`
in the one real panel titled `Flow file read grant owner isolation`. Use
Command-Shift-G, the absolute path and Return; confirm Open only after
`flowFileReadGrantIsolation.panelObserved`. The driver does not select or
confirm the panel. It uses browser-programmatic route setup and seven trusted
generated AppKit clicks: open the owner window, select, foreign read, foreign
release, owner read, owner release, and close the owner window.

The test application deliberately retains the selected token only in private
in-memory state and calls the public `NativeBridge`/`DesktopCommand` surface.
Each listener captures its own capability context before background work;
reusing the owner-bound typed grant facade would not test a foreign caller.
The displayed outcome names classify raw public command results in the fixture,
not a new typed-result API. Foreign read and release use the actual selected
token while it is still active. Both must pass manifest authorization yet
return `grant-unavailable`, with no bytes or release effect. The original owner
must subsequently read the expected 19 bytes and release successfully. Token
equality is checked inside the Java fixture; neither token nor file content is
published as DOM status or trace data.

`--require-flow-ipc-file-read-grant-isolation` correlates five grant commands
and results across the exact main and secondary scopes in one JVM generation.
It requires two foreign `unknown-token` diagnostics, no premature grant
invalidation/closure, the original owner's read and single successful release,
matching acquired security-scope stop, visible Java outcomes, balanced scoped
UIDL completion, window/WebView continuity and graceful shutdown. Full-trace
privacy checks and negative smoke cases reject permission-denial substitutes,
wrong scope/generation, false input receipts, missing positive controls and
leaked token/path/content. The package lock covers build, execution, inspection
and temporary-fixture cleanup. The driver deadline defaults to 600 seconds;
it does not bound building or waiting for the lock.

This proves a foreign window cannot use or release a live owner's token through
the native command boundary. It does not isolate mutually hostile Java code
inside the same JVM, where code can deliberately pass an owner-bound context;
nor does it prove cross-generation replay, App Sandbox/Powerbox enforcement,
in-flight read/release races, physical input or Finder provenance.

Typed selected-file reads at the host range boundary have a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-range
```

This independent single-main-window route uses only the public captured
capability context and `DesktopFileReadGrant` facade. Its explicit test-support
manifest permits selection, read, release and main close; the application's
regular main policy is unchanged. Select the runner's current
`vatori-file-read-grant-range-fixture.bin` in the real panel titled
`Flow file read grant bounded ranges`, using Command-Shift-G, its absolute path
and Return. The active file is under
`${TMPDIR:-/tmp}/vatori-file-read-grant-range.*/vatori-file-read-grant-range-*/vatori-file-read-grant-range-fixture.bin`.
Wait for `flowFileReadGrantRange.panelObserved` before confirming
Open. The acceptance overlay only observes the canonical URL, attached sheet
and visibility; it never selects or confirms the panel.

The 65,573-byte fixture contains deterministic nonuniform binary bytes. Ten
trusted generated AppKit clicks run this sequence, each with its own Java
listener and visible typed result:

| Stage | Offset / maximum bytes | Expected result |
| --- | --- | --- |
| select | — | `SELECTED`, size 65,573, limit 65,536 |
| negative | -1 / 16 | `INVALID_RANGE`, no bytes, grant remains active |
| zero | 0 / 0 | `INVALID_RANGE`, no bytes, grant remains active |
| oversized | 0 / 65,537 | `INVALID_RANGE`, no bytes, grant remains active |
| maximum | 0 / 65,536 | `DATA`, 65,536 matching bytes, EOF false |
| tail | 65,536 / 65,536 | `DATA`, 37 matching bytes, EOF true |
| eof | 65,573 / 1 | `END_OF_FILE`, zero bytes, unchanged offset |
| beyond | 65,590 / 16 | `END_OF_FILE`, zero bytes, unchanged offset |
| seek | 257 / 113 | `DATA`, 113 matching bytes, EOF false |
| release | — | `RELEASED` |

The same private typed handle is retained throughout. Range errors have typed
terminal offsets of -1; requested offsets remain separately visible and are
checked against actual native command arguments. Successful backward reading
after both EOF probes proves EOF and invalid-range results do not retire the
grant. Exact byte comparison stays inside Java; neither the bytes nor the
private token are rendered or traced.

`--require-flow-ipc-file-read-grant-range` requires ten grant command/result
pairs, one grant lifecycle with five successful read receipts and successful
explicit release/close, allowed manifest decisions, exact offsets/counts/EOF,
visible Java results, scoped opaque UI completion, retained native identities,
balanced drain and graceful shutdown. Security-scope stop must match acquisition.
Negative smoke cases reject wrong ranges, outcomes, lifecycle, correlation,
input provenance and leaked token/path/filename/content. Route navigation and
`scrollIntoView`/pointer geometry are browser-programmatic setup; activation
uses generated AppKit events, not physical input. The runner holds the package
lock through build/run/inspection and removes its temporary fixture directory
after exit, including failure. Its driver deadline defaults to 600 seconds.

This is focused real selected-file data-plane evidence, not long-running or
concurrent I/O, file mutation/truncation, App Sandbox/Powerbox, persistent
bookmarks, physical keyboard/mouse or Finder-origin input coverage.

Replaying an exit-revoked grant from the recovered owner has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-replay
```

This independent public Flow route uses raw `NativeBridge` commands in the
selecting secondary scope. The first real panel is titled
`Flow file read grant before recovery`; select the current 48-byte fixture under
`${TMPDIR:-/tmp}/vatori-file-read-grant-replay.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`
with Command-Shift-G, Return and Open, waiting for
`flowFileReadGrantReplay.panelObserved` stage `old` before confirming.

Before reporting the first selection as ready, the test application writes and
reads back the actual raw token in a bounded private handoff file. The runner
creates a unique owner-only directory; the handoff is created exclusively with
owner-read/write permissions. It is never published to DOM, trace or environment
variables. Only the handoff file location is passed through the explicit
test-support launch environment. The recovered JVM reads that test-only record,
checks the original PID differs, and compares each replay command's argument to
the restored token privately. This is not public grant persistence or recovery.

After the visible selection and balanced transport drain, the driver kills only
the original sidecar PID. A separate
`flowFileReadGrantReplay.beforeFailureObserved` receipt binds the private-handoff
checks, original PID, retained identities and balanced drain before injection.
Recovery must preserve both native windows, WKWebViews
and exact scopes while replacing the JVM and resetting Java view state. New
listeners in the recovered owner capture fresh capability contexts and invoke
read and release using that original token. Both must be permission-allowed yet
`UNAVAILABLE`, with no content or release effect. Only then does the second real
panel, titled `Flow file read grant after recovery`, select the same fixture
again. Wait for `panelObserved` stage `fresh` before Open. The new token must be
distinct privately, read 19 matching bytes and release successfully.

`--require-flow-ipc-file-read-grant-replay` correlates six grant commands/results
by generation and correlation id, including ids reused by the fresh JVM. It
requires one actual injected exit/recovery, two JVM generations/PIDs, two original
window creations and four initial/recovered loads. Nine grant lifecycle receipts
must show the old grant created, invalidated and closed, both replay denials
referencing its original diagnostic id with reason `sidecar-exit` in generation
2, and the new grant created, read and explicitly released/closed. Old closure
may finish asynchronously after recovery, but must follow invalidation and
complete before final acceptance. Security-scope acquisition and stop are
matched independently for the two grants.

Eight trusted generated AppKit clicks, two external native panels, private
handoff checks, visible Java outcomes, scoped UIDL/push completion, retained
native identities, balanced drain and graceful final shutdown are required.
Browser-programmatic setup includes initial and recovered main-route navigation
and `scrollIntoView`/geometry for pointer positioning; button activation itself
uses generated AppKit events. Full-trace privacy and negative smoke cases reject random-token
substitutes, permission denial, wrong scope/generation/diagnostic id, missing
positive controls, false continuity and token/path/content leaks. The runner
holds the package lock through build/run/inspection and removes its generated
fixture/handoff directory after exit, including failure. The driver deadline
defaults to 600 seconds and does not bound build or lock wait.

The replay runner first executes the standalone Java handoff smoke. It can also
be run without a native window or Maven build:

```bash
bash test-support/native-host-macos-acceptance/bin/check-file-read-grant-replay-handoff
```

It checks private-file creation/readback, recovered-PID separation and cleanup,
and rejects broad permissions, symbolic links, malformed/oversized records and
duplicate creation. These filesystem checks complement the real native grant
proof; they do not establish App Sandbox enforcement.

Temporary-file removal is an unlink, not a cryptographic-erasure guarantee.
This proves a known exit-revoked token cannot be replayed after recovery. It
does not prove persistence or survival of the original grant, directly inject an
active-map generation mismatch, or cover planned replacement, in-flight races,
App Sandbox/Powerbox, physical input or Finder provenance.

Public Clipboard terminal outcomes have a separate focused real-WKWebView
entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-clipboard-outcomes
```

Three generated trusted AppKit primary clicks reach public Flow listeners. The
main scope first reads an AppKit pasteboard with no string value and must return
public `NO_TEXT`. It then writes an explicit empty string and reads it back as
public `WRITTEN` plus `TEXT` with length zero. Browser-programmatic setup opens
a dedicated secondary whose manifest grants neither Clipboard command; its
trusted click invokes both operations in the exact opaque-UUID scope and must
return public `DENIED` twice without changing the pasteboard.

The test-support overlay snapshots the pre-run pasteboard in memory. Before
each bounded restore attempt it compares the general pasteboard `changeCount`
with its latest test-owned count; an observed mismatch stops restoration without
another mutation. A failed AppKit snapshot-restoration write can be retried
while that observation still holds; graceful app termination provides a final
conditional restore opportunity. Clipboard contents are absent from DOM status,
acceptance payloads
and full-detail command/result traces. macOS activation is cooperative: the
host must be frontmost and key before the native-input stages, so bring its
title bar to the foreground if another application retains focus; the gate
times out rather than dispatching into an inactive window. Inspection
with `--require-flow-ipc-clipboard-outcomes` requires exactly five unique
correlations in operation order, exact result shapes and permission audits,
three trusted pointer stages, scoped UIDL completion, redaction metadata,
balanced quiet drain, no recovery and graceful shutdown. The core contract
smoke in `bash bin/flow-ipc-scope-smoke` separately covers `INVALIDATED`,
malformed/incomplete `ERROR` shapes and single-delegation legacy compatibility.
AppKit has no compare-and-swap pasteboard replacement: an external writer can
still race in the narrow interval between the count check and clear/write, so
this is best-effort observed-contention protection rather than an absolute
no-overwrite guarantee. Because the snapshot is intentionally memory-only,
`SIGKILL`, a hard host crash or machine termination cannot restore it. This
focused gate also does not deterministically force an AppKit write failure or
prove OS authorization prompts, non-text formats, adversarial contention,
physical input or full Clipboard/product parity.

Public Notification outcomes have a dedicated test-support runner:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-notification-outcomes authorization-required
# These modes require an external operator to answer the new app's OS prompt:
bash test-support/native-host-macos-acceptance/bin/run-flow-notification-outcomes os-denied
bash test-support/native-host-macos-acceptance/bin/run-flow-notification-outcomes scheduled
```

Each invocation holds the shared package lock through build, launch, inspection
and cleanup. It uses a private disposable app clone under
`.build/native-hosts/macos/notification-outcomes.XXXXXX`, outside system
temporary directories, with a fresh bundle identifier. It never resets
notification settings for an existing app, and
requires initial OS authorization to be `not-determined`. Permission choices
can leave system records for those new test identities even after clone cleanup.
Cleanup restricts process matching to the exact clone executable or configured
Java command with that clone's JAR, and removes only the owned leaf after
successful process checks. Failed process inspection retains the clone. Exit
and HUP/INT/TERM share the same bounded cleanup entry; synthetic runner tests
separately cover pending-signal reentry and canceled watchdog escalation.
`SIGKILL`, crashes or the outer watchdog interrupting a stuck cleanup can still
leave a clone; this is not an atomic process-identity or crash-cleanup guarantee.
The `authorization-required` mode selects the host's no-activate prompt policy
through the acceptance overlay; the default production observer does not select
that policy. This mode alone is not production OS authorization evidence.
All modes use a normal foreground launch because generated AppKit input
requires a frontmost/key window; it is not a background-window test.
No system authorization prompt is permitted in `authorization-required`.
The other modes require explicitly authorized external UI interaction;
the acceptance driver only reads authorization settings.

Three trusted AppKit primary clicks reach public Flow listeners: blank-title
`ERROR`, the selected main-scope outcome, then manifest `DENIED` in a distinct
secondary opaque scope. Each listener captures the public capability context,
invokes `showResult` off the Vaadin session thread, and updates its own visible
status through `UI.access`. Browser-programmatic clicks perform only route
navigation and secondary-window setup. Notification content, identifiers and
native error text are absent from DOM status and acceptance receipts.

The `--require-flow-ipc-notification-outcomes` inspector gate correlates those
receipts with full-detail native command/results, permission metadata, scoped
opaque requests/responses and push acknowledgements. It checks HTTP envelope
correlations, contiguous logical push sequences and same-sequence retries, and
rejects disconnect/resync before acceptance. It requires read-only OS
authorization observations before and after the main request, stable scopes,
one sidecar generation, balanced drain and graceful shutdown. Synthetic gate
tests cover all three modes; core contract and native trace smoke separately
cover malformed/contradictory results, invalidation, legacy single delegation
and recursive content redaction. Synthetic acceptance is not real OS
authorization evidence. `SCHEDULED` means OS acceptance only: this surface
does not prove foreground banner presentation, delivery, physical input,
already-submitted request revocation or full notification product parity.
All three modes have focused real-window validation. The no-prompt path leaves
OS authorization `not-determined`; external operator choices exercise the real
OS transitions to `denied` and `authorized`, with public `OS_DENIED` and
`SCHEDULED` results respectively. Each mode completes all three Flow stages and
the scoped native result/push/visible-state gate. Flow button input remains
generated AppKit evidence, not physical input or assistive-technology validation.
Bring the test app to the foreground when cooperative activation
cannot make it active/key; an activation timeout is a failed run, not evidence
for a notification outcome.
An authorization API error alone is not evidence of a user choosing Don't Allow.
The host performs one additional read-only settings check after that error:
only an independently observed `denied` state produces `OS_DENIED`; every
other state retains `ERROR` and fails the OS-branch gate. A fresh identity and
`not-determined` observation
do not prove that the OS can present its permission prompt; do not reinterpret
`notification-authorization-failed` as `OS_DENIED` or reset existing app
settings to make the gate pass.
When a main request renders an authorization/scheduling API `ERROR` after its
trusted click, Java listener and push update, the driver emits a fixed-metadata
`unexpectedErrorObserved` receipt and ends the run as failure. This receipt is
rejected by the success gate; it does not replace the three required outcomes.

Foreground notification delivery has a separate, opt-in acceptance runner:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-notification-delivery
bash bin/flow-ipc-trace-inspect --require-flow-ipc-notification-delivery \
  .build/native-hosts/macos/host-notification-foreground-delivery.out.log
```

This uses the same locked, freshly signed disposable clone and exact-process
cleanup as the outcomes runner, with only `notification.show` allowed in `main`.
It requires the external operator's authorization for one new app's **Allow**
choice and **two** notifications: a preparation call obtains permission through
the public API, then a separate already-authorized probe runs after the window
regains foreground/key status. Existing app settings are never reset or changed.
The test overlay does not install a notification delegate or alter the product's
presentation policy. Flow buttons use generated trusted AppKit pointer events;
route navigation is browser-programmatic.

The new gate correlates both public captured-context background calls, native
command/results, Java listener/UI.access state and opaque request/push patches.
The public probe identifier is SHA-256 hashed by the test app and matched against
identifiers returned by the real `getDeliveredNotifications` API. Only digests,
counts and fixed booleans enter acceptance traces; raw identifiers, notification
title/body and native error text do not. A match must have a notification date no
earlier than probe dispatch, with no application-resign-active or window-resign-key
event during dispatch-to-observation. The test confirms a delivered notification
still retained by Notification Center, not a display timestamp or a banner receipt.

After observing delivery the driver leaves a bounded visual-observation interval,
then requests removal of only its matched test identifiers and closes gracefully.
This cleanup is test-only, best-effort SDK removal, not a public cancellation API
or proof of atomic OS revocation. Abnormal termination or a notification not yet
observed by the read-only query can still leave a test notification behind. The
operator may separately report whether the
system banner titled **Vatori foreground probe** appeared. The automated gate
always sets `bannerVisibilityClaimed=false`; an Allow prompt, app-internal status,
or app-window screenshot is not banner evidence. Focus loss fails the foreground
claim instead of being repaired after the fact. No physical keyboard/mouse,
assistive-technology or general notification reliability claim follows from this
focused path.

`bash bin/native-host-notification-failure-smoke` verifies the production error
payload helper's fixed domain tokens, signed integer codes and unchanged
application-facing message, plus the denied-only settings classification after
authorization errors. These controlled states do not exercise the actual OS
settings callback or its invalidation timing. Native trace smoke verifies
recursive diagnostic
redaction and safe full/compact metadata; core contract smoke rejects diagnostic
fields on non-error native shapes. These checks establish diagnostic integrity,
not an OS authorization choice or a root cause for a particular native failure.

`bash bin/native-host-notification-completion-smoke` separately exercises the
production completion adapter with controlled removal/publish/drop closures.
It covers active success/error callbacks, invalidated late success/error,
missing or replaced identifier registration and exact old-identifier cleanup
without publishing into or removing a newer operation. Pending and delivered
removal must precede the stale-result drop. This is deterministic helper
evidence, not actual `UNUserNotificationCenter` scheduling/removal or the whole
executor's invalidation lifecycle. The real `authorization-required` gate is a
regression check and never enters the OS add/compensating-removal branch.
Neither surface proves that a banner or sound never occurred, or that a late
callback can run after host termination.

Native capability isolation across sidecar generations has a separate focused
real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-generation-recovery
```

An independent public Flow route invokes `fileDialog.openFile` with a distinct
title in each of two sidecar generations. Each invocation begins with a
generated AppKit primary click that must arrive as a trusted client Button
event, execute the Java listener and display a real `NSOpenPanel`. Once the
first panel is visible, test-support sends `SIGKILL` to the real Java process
without canceling that panel. Product retirement must clear the active panel
token before `cancel` and `orderOut`, drop the old completion, and preserve the
same native window and WKWebView while the next sidecar reaches `ready`.

The recovered JVM intentionally reuses `desktop-command-1` with the exact same
window/session/UI scope. Test-support cancels only the second real panel. Its
normal canceled result must reach the new Java listener, visible UIDL status and
a balanced quiet drain before graceful shutdown. Full-detail inspection requires
the old-generation invalidation and dropped completion/result before the
new-generation result; an expected-generation comparison and admission to the
generation-bound stdin writer are atomic, so the stale result cannot enter the
new JVM. Admission is not confirmation of a complete pipe write. Each observed command must
follow the corresponding sidecar `ready`, consistent with the controller's
initial, recovery and replacement readiness guard. The two route navigations are
the only browser-programmatic stages, focused push is false, and generated
AppKit events are not evidence of physical input.

Owner-window close during an in-flight native capability has a separate focused
real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-owner-window-close
```

The application opens an isolated secondary whose `sessionId` is a dynamic
host-generated opaque UUID rather than a value derived from its public window
id. A generated AppKit primary click in that secondary reaches a
client-originated public Flow listener. The listener captures its exact
secondary capability context before passing it to a background virtual thread;
that thread requires `backgroundSessionAbsent=true` and opens a real attached
`NSOpenPanel` through the captured facade.
A second generated AppKit primary click in the retained main window calls the
public `window.close` API for the secondary; no test panel-cancel hook is used.
Product close invalidates the operation, clears the active panel token before
`cancel` and `orderOut`, drops the stale AppKit completion, sends the waiting
Java call the correlated typed `owner-window-closed` result, and only then emits
the exact secondary `windowClosed` and releases its native context. A third
generated AppKit click proves a new main-window Flow listener/UIDL round trip in
the same JVM, window and WebView.

Full-detail inspection with
`--require-flow-ipc-capability-owner-window-close` requires the exact ordering
from command and visible panel through operation invalidation, completion drop,
typed capability result and `windowClosed`; it also requires detached/hidden
panel state, released secondary context, dynamic full-scope correlation,
balanced request/response drain, no recovery and graceful shutdown. The Java
scope smoke (`bash bin/flow-ipc-scope-smoke`) separately requires same-window
close ordering, cross-window lane progress, immediate scope tombstoning and
synchronous Vaadin UI detach plus `SessionDestroy` before the native capability
session-to-scope mapping is released. It also proves that a late captured
secondary context cannot emit or retarget a command after teardown or after the
same public id reopens with a new opaque UUID, and that late inbound frames for
the exact retired tuple cannot recreate or poison the reopened scope.

Route navigation and secondary opening are browser-programmatic setup. The
three generated AppKit events prove the host event path, trusted DOM receipt and
client-originated Flow listeners, but not physical pointing-device hardware,
touch or assistive-technology interoperability. This closes the stated
file-dialog owner-close lifecycle slice, not every native capability or full
product parity.

Captured capability context across same-public-id close/reopen has its own
focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-context-reopen
```

Incarnations A, B and C share `capability-reopen-secondary` as their public
window/UI id but must have pairwise-distinct host-generated opaque UUID sessions
and native window/WebView identities. A generated trusted AppKit click in A
captures its exact context on a Flow listener; a background virtual thread with
no current Vaadin session sets the A title and publishes the result through an
acknowledged scope-exact push. A second trusted click in retained `main` closes
A and waits for its host context to disappear before browser-programmatic setup
opens B.

A third trusted click in B runs A-old/B-fresh/A-old from another
no-current-session virtual thread. A fourth trusted click closes B and waits for
its context to disappear before setup opens C. A fifth trusted click in C runs
A-old/B-old/C-fresh/A-old/B-old. Every retired call must return
`owner-window-closed` locally. Full-detail inspection with
`--require-flow-ipc-capability-context-reopen` rejects all six stale-title
variants, retargeting, any reused session/window/WebView identity, missing or
wrong-scope push acknowledgements and misordered create/close events. It also
requires balanced per-incarnation and main request/response drains, the same
sidecar/main window/main WebView, no recovery and graceful shutdown. Route
navigation and all three opens are browser-programmatic setup; this closes two
consecutive reincarnation cycles, not long-running or unbounded cycles, higher
fan-out, recovery/replacement or full product parity.

Production-host retired-scope wiring has a complementary focused real-window
entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-retired-scope-production-host
```

The runner packages the standard Vaadin production bundle, copies that bundle
and its capability manifest into an isolated app, and executes the production
Swift host composition with real AppKit windows and WKWebViews. The isolated
app stages a test-support fake sidecar as its Java JAR; that sidecar drives
protocol envelopes through the normal `main.swift` handler. Two
successive secondary incarnations A and B reuse one public window id, receive
distinct host-generated opaque UUID sessions, close normally and leave two
simultaneously retained host tombstones. After both closes, exact A and B
`command` probes must each receive a correlated typed result with
`canceled=true`, `cancelReason=owner-window-closed` and
`operationInvalidated=true`; late exact-scope HTTP response and push envelopes
must be dropped by the production handler.

A third incarnation C must receive another fresh UUID, complete a live
`window.setTitle` command and a browser request/response-credit round trip, then
close before the retained main window and fake sidecar shut down gracefully.
Here the round trip means that the real WKWebView emits scope-exact response
credit and the host accepts the matching chunk/end; it does not claim that the
browser consumes the full body or that the fake request is a Flow UI request.
Full-detail inspection with
`--require-flow-ipc-retired-scope-production-host` requires that ordering, both
retired command rejections, all late-transport drops, C health, one sidecar
generation and no recovery.

This gate proves the production host's `main.swift` wiring and process-lifetime
retention of two exact retired scopes. Its Java sidecar is deliberately fake;
it does not prove a Flow UI, Java listener, physical user close input,
unbounded reincarnation or recovery/replacement behavior. The preceding real
Flow A→B→C gate supplies the complementary public API, Flow listener, UIDL
and visible-state evidence.

Main-window close with a retained secondary and sidecar recovery has a separate
focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-main-window-close-recovery
```

Browser-programmatic setup navigates the main document and opens one secondary
on the same non-root route. A generated AppKit primary click in the secondary
invokes the public `window.close("main")` API. The gate requires the main
context to be released while the secondary native window, WKWebView and opaque
UUID scope remain. Test-support then sends `SIGKILL` to generation one. After
generation two reaches lifecycle-ready, the host must leave main absent, replay
the normal `windowClosed` envelope for the exact main default scope, and reload
the retained secondary from its original route-specific entry URL.

A second generated AppKit primary click in the recovered secondary reaches its
fresh Java listener. A no-current-session background call to
`Desktop.current()` must settle locally with `canceled=true`,
`operationInvalidated=true` and `cancelReason=owner-window-closed`, without a
generation-two main-scoped host command. The same listener must then issue one
successful secondary-scoped `window.setTitle`, render the exact result through
UIDL and preserve the secondary's dynamic scope.

Full-detail inspection with `--require-flow-ipc-main-window-close-recovery`
requires two distinct sidecar processes, one unexpected-exit/recovery sequence,
one `windowClosedReplay` trace with reason `sidecar-lifecycle-ready`, two normal
main `windowClosed` sends, no reconstructed main window, one recovered
route-preserving secondary load, the exact two trusted pointer stages, balanced
drain and graceful shutdown. Route navigation and secondary opening are the only
browser-programmatic stages. Generated AppKit events are not evidence of
physical hardware, and this bounded two-window topology does not establish full
multi-window or product parity.

Lazy `DataProvider` query semantics have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-lazy-grid-query
```

The isolated route first uses the official Grid `MULTI` selection column over
an 80-item exact-count `CallbackDataProvider` with stable `WorkItem::id`
identity and `PRESERVE_ALL`. Its first five generated AppKit primary clicks
target three official visible row checkboxes and two Flow refresh buttons with
exact trusted DOM receipt. The corresponding Java `MultiSelectionEvent`
sequence remains client-originated exactly three times: add `L-061`, add
`L-060`, then remove `L-061`; each checkbox click completes an opaque
request/UIDL-response round trip. Between the additions and removal, the button
replaces selected `L-060`
with a fresh, unequal object that keeps the same `WorkItem::id`, invokes
`refreshItem`, and requires the `refreshed` cell patch and selected row state
without an extra selection event. Owner sorting and a `platform` filter retain
both selected ids while hiding `L-061` and rehydrating the refreshed, selected
`L-060`. With both selected rows outside the rendered viewport, the second
button removes matching item `L-003` and explicitly calls `refreshAll`; the
provider total changes from 80 to 79 and the active-filter count from 26 to 25,
fetch and count queries advance, one generic `DataChangeEvent` and no
`DataRefreshEvent` are observed, `L-003` disappears from the rendered Grid, and
selection remains unchanged. Returning to the selected rows and then resetting
the filter rehydrates their selected and refreshed state before the final
deselection leaves `L-060`.

The same route then exercises a separate fetch-only Grid configured through
the public `setItems` overload, with page size 5, an initial unknown-size
estimate of 40 and an increase of 40. Browser-programmatic viewport movement
must grow the client estimate beyond 40 without an exact count query, cross the
initial estimate in backend fetches, and then produce a terminal short fetch
whose last item is `U-067`. The official `ItemCountChangeEvent` remains
estimated during growth and settles to exact count 67; the client Grid size and
rendered tail must agree. A sixth generated AppKit primary click selects the
official `U-067` row checkbox and requires a client-originated Java
`MultiSelectionEvent`, opaque request/UIDL response and selected DOM state.

After the selected old tail leaves the viewport, a seventh generated AppKit
primary click invokes a standard Flow Button listener. The application appends
`U-068` through `U-093` to its backing list and calls the public
`GridLazyDataView.refreshAll()` API. The gate requires one generic data-change
event and no item-refresh event, a reset from the locked exact 67 tail to the
40-item estimate, estimated growth through at least 120, a fetch beyond the
prior exact tail, and a second positive short terminal fetch ending at `U-093`
before Java and client size settle to exact 93. `PRESERVE_ALL` must retain
`U-067` without another Java selection event, including selected row, checkbox
and ARIA-state rehydration when that row re-enters the viewport.

After returning to the top with `U-067` and the prior tail outside rendered
rows, an eighth generated AppKit primary click invokes another Flow Button.
It removes unselected `U-073..U-093` (93 to 72 items), retains the original
`U-067` object and calls public `GridLazyDataView.refreshAll()`. A separate
shrink epoch must reset to estimate 40, grow through 80 and discover exact 72
from a positive short fetch ending at `U-072`. Global fetch/count-event deltas
must equal the new epoch counters; one generic data-change event, no item-refresh
event and no extra selection event are required. The selected row, checkbox and
ARIA state rehydrate, and all 21 removed ids are absent from rendered cells.
The inspector cross-checks frozen initial/append fields against the final
snapshot, unique status values, count histories, bounded short-fetch cardinality
and the eighth trusted DOM receipt plus opaque request/UIDL response. Negative
smoke rejects stale epochs, changed frozen evidence, contradictory status keys,
impossible estimates/cardinality and fabricated progress.

Route navigation, viewport movement, sorting, filtering, refresh-button positioning
and unknown-size scrolling remain browser-programmatic setup. The gate runs
the standard production bundle in a real WKWebView and requires graceful
sidecar shutdown; it does not claim focused-route push, physical input
hardware, touch, assistive-technology interoperability, selected-item removal,
arbitrary repeated reset sequences, reorder or sort/filter-key mutation, broader
pagination, larger scale, fresh-object identity across the unknown-size reset,
or other Grid capabilities.

Selected-item deletion in the exact-count lazy Grid has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-lazy-grid-selected-removal
```

It reuses the same public Grid route without changing the original eight-click
gate. Three generated AppKit clicks select `L-060`, invoke an ordinary Flow
delete Button while that row is outside the rendered viewport, and select the
surviving neighbor `L-061` after refresh. The application explicitly calls
`grid.deselect(item)`, removes the item from its backing collection and invokes
the configurable provider's public `refreshAll()`. Under `PRESERVE_ALL`, lazy
providers do not automatically prune removed selections. The button event is
client-originated, but the synchronous public `deselect` selection event is
server-originated; the exact Java selection history must therefore report
`true`, `false`, `true` for its three changes.

The deletion receipt binds the selected id, empty selection after deselection,
80-to-79 backend cardinality, one generic data-change event and zero item-refresh
events. Fetch/count baselines are captured after deselection and backend removal
but before `refreshAll`, so a count query caused by deselection cannot substitute
for refresh progress. Each native click requires its own post-dispatch opaque
UIDL request and completed response. Returning to the deleted neighborhood must
show the surviving rows without `L-060` or stale selected/checkbox/ARIA state;
the later `L-061` selection must persist after another viewport recycle.

The gate checks original main-window, WebView, scope and sidecar continuity,
trusted DOM receipts, exact Java histories, client size and selected-item count,
balanced quiet drain and graceful shutdown. The client `selectedItems` array is
a cached-page projection, not the complete Java selection: clearing a distant
page removes its client objects without a server deselection event. The two
off-viewport stages therefore require an empty client projection while Java
retains the selected id; visible row, checkbox and ARIA state prove rehydration
on return. Browser code only navigates,
scrolls, positions and observes; it does not assign Grid selection or remove
DOM rows. This is focused evidence for application-directed deletion in an
exact-count lazy provider, not automatic pruning, unknown-size selected-item
deletion, multi-item transactions, filter/sort-key mutation, recovery, physical
input, assistive technology or focused-route push.

Flow DragSource/DropTarget semantics have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-dnd
```

It opens an isolated Flow DnD route through the production bundle and posts a
generated macOS primary-pointer drag through the AppKit event path into the real
WKWebView. The gate requires trusted `mousedown`, `dragstart`, `dragenter`,
`dragover`, `drop` and `dragend` events, exactly one client-originated Flow Java
drag-start/drop/drag-end listener round-trip, server-side drag data, MOVE drop
semantics, opaque request/response and push evidence, and clean shutdown. The
trace requires `appkit-event-queue-pointer` and rejects the former scripted DOM
fallback. WKWebView can normalize the native drop event's `effectAllowed` from
MOVE to COPY_MOVE while preserving the configured MOVE source and final MOVE
`dropEffect`. This generated native-input gate does not claim physical mouse
hardware coverage or the separate external Finder file-drop boundary.

A separate external-file gate waits for an operator or Computer Use to drag a
real file from Finder into the standard Upload component:

This acceptance surface is prepared, not yet a claim of verified Finder parity.
An external automation tool must actually support cross-application dragging;
an API restricted to coordinates within one app window is insufficient.

```bash
# Use a new absolute canonical directory; its parent must already exist.
bash test-support/native-host-macos-acceptance/bin/run-flow-external-file-drop \
  /private/tmp/vatori-finder-drop-example
```

The runner prepares a private, random 65,573-byte regular file outside the app
and an independent manifest before launch. Open the printed `source` directory
in Finder, wait for `externalFileDrop.readyForExternalDrop`, then drag the one
file into the visible drop zone. The test driver only navigates and positions
the target; it does not construct a File/DataTransfer, dispatch a drop, fill a
file input or call an Upload method. Readiness starts a separate five-minute
external-input deadline. The runtime gate requires one trusted drop, the same
File in the standard multipart request and success events, one actual public
Java `UploadHandler` byte-count/SHA-256 result, the standard response and
AllFinished listener, returned DOM state, a balanced quiet drain and graceful
shutdown. Upstream AllFinished is server-originated (`isFromClient=false`).

`--require-flow-ipc-external-file-drop` is deliberately only the runtime half of
the proof: a trusted drop cannot identify its source application. The runner
preserves its trace under the run directory. After completion, independently
record the observed Finder action in an operator receipt and bind it with:

```bash
node test-support/native-host-macos-acceptance/Tests/external-file-drop-evidence.mjs \
  verify /private/tmp/vatori-finder-drop-example/manifest.json \
  /private/tmp/vatori-finder-drop-example/host.out.log \
  /private/tmp/vatori-finder-drop-example/operator-receipt.json
```

The JSON receipt uses `schemaVersion:1`,
`evidenceKind:"external-operator-computer-use"`,
`sourceApplication:"com.apple.finder"`, the manifest `runId`, `observedFileName`,
`manifestSha256`, completed `traceSha256`, actual `hostPid`/`sidecarPid`, and
nonempty `evidenceRefs:[{"path":"/absolute/evidence-file","sha256":"..."}]`.
It must retain `sourceApplicationVerified:false` and
`claim:"external operator evidence present and bound"`. References must contain
the actual external observation/action evidence, not generated assertions of
success. For a human-performed drag, use
`evidenceKind:"external-operator-manual"` and include the operator's actual
confirmation in a nonempty `operatorStatement`; never prefill that statement
before the action occurs. The verifier checks file integrity, independent expected metadata,
actual Java result, process identities and evidence hashes; it does not
cryptographically authenticate Finder or replace the runtime gate. Preserve
the manifest, file, trace, receipt and evidence together as generated artifacts.
This bounded one-file case does not establish physical hardware, VoiceOver,
App Sandbox/Powerbox, directory or multi-file drops, promised/cloud files,
other source applications, drop cancellation/retry, or focused-route push.

A heterogeneous three-level overlay stack has a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-overlay-stack
```

The gate uses browser-programmatic setup and Flow Java listeners to open a modal
Dialog, an anchored modeless Popover and an auto-added modal ConfirmDialog. It
then delivers generated AppKit window events to the real WKWebView: Escape
closes the ConfirmDialog, a primary-pointer outside click closes the Popover,
and a second Escape closes the Dialog. The gate requires trusted DOM receipt
with exact stage counts, standard ConfirmDialog initial focus and accessibility,
parent-layer retention, layered focus restoration, client-originated Java
lifecycle listeners and UIDL patches, opaque request/response and push evidence,
and graceful shutdown. The opening actions remain scripted setup; physical
keyboard or pointing-device hardware, touch and assistive technology remain
outside this contract.

Owner-bound input popups have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-input-popup
```

The gate uses ComboBox, Select and DatePicker as probes of the shared
Flow/browser path. It requires ComboBox owner focus, combobox accessibility state,
active-option navigation, selection, Java value-listener execution, close and
focus restoration. Select additionally requires value-button/listbox/option
accessibility, selected-item focus, ArrowDown movement without an early value
change, Enter selection through a Java value listener, close and owner-focus
restoration, then selected-item focus after reopening and value-preserving
Escape close. DatePicker requires combobox/dialog accessibility, Java
opened/value listeners, calendar focus, ArrowRight movement, Enter selection,
Escape close, retained value and owner-focus restoration. The ComboBox sequence
and DatePicker opening are scripted browser events in the real WKWebView.
Select interaction and DatePicker calendar focus, movement, selection and close
use AppKit keyboard events delivered to the WKWebView, with DOM receipt recorded
in the trace. After those keyboard paths, Select and DatePicker are reopened and
closed by AppKit primary-pointer events. The pointer contract requires trusted
DOM receipt on the document background selected by Vaadin's modal pointer
shield, retained values, owner-focus restoration, client-originated Java
opened-change listeners and UIDL status patches, with no click-through to the
underlying Flow button. This contract does not claim physical keyboard or mouse
hardware, remaining picker variants, locale/validation breadth, touch dismissal
or assistive-technology interoperability.

MultiSelectComboBox token and collection-value semantics have a separate
focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-multi-select-combo-box
```

The independent route uses only public Flow APIs and runs the standard
production bundle in a real WKWebView over opaque requests and UIDL responses.
Its token field starts with `ava`, a public clear-enabled field starts with
`kai,sol`, and a third field starts with `ava` and uses an explicit public
`ItemFilter` to force server-side filtering plus `setKeepFilter(true)`. A fourth
field also starts with `ava` and uses an explicit `ItemFilter`, while retaining
the public default `keepFilter=false` behavior. Five trusted generated AppKit
primary clicks, 15 trusted generated AppKit keydowns and two resulting trusted
input events reach the four fields. The first three clicks and first seven keys
add `sol`, exercise two-stage Escape and Backspace, remove the remaining `ava`
through its nested-shadow remove button and atomically clear `kai,sol` through
the clear button.

The fourth click opens and focuses the keep-filter field. Trusted `k` input
reaches a latched server-filter request/response checkpoint that renders only
`kai` while retaining `ava`; ArrowDown and Enter focus and select that result.
Two trusted Escape keydowns then clear its active option and close the popup,
resetting its input/filter while preserving `ava,kai`, before the fifth click
opens and focuses the default-clear field. Its independent `k` query again
renders only `kai` while retaining `ava`; ArrowDown and Enter select `kai`.
Because `keepFilter` remains at its default, selection clears input/filter,
performs the empty-filter server rehydrate and leaves the field focused and
open with all three options, selected chips `ava,kai`, and the normalized
owning-listbox ARIA selection map `ava:true,kai:true,sol:false`.

The token field requires exactly three client-originated Java
`MultiSelectionEvent`s with three matching UIDL responses; the clear field
requires one event with `removed=kai,sol` and one matching response. Each
server-filter field requires its own query checkpoint, one distinct selection
UIDL-response checkpoint and exactly one client-originated event with
`added=kai`. The first two fields finish empty. Navigation and pointer
positioning, plus scripted token-field focus and empty-input/caret-at-start
preparation before Backspace, are browser-programmatic setup; filtering-field
focus comes from the trusted fourth and fifth clicks. The runner drains balanced
opaque requests/responses through a quiet interval before closing and requires
graceful sidecar exit. Focused push evidence is explicitly false. Lazy-data
filtering has the separate bounded route below; unknown-size providers,
mutation-driven or manual refresh, broader pagination/filter and close/reopen
combinations, selected-items-on-top combinations, physical input hardware,
touch and assistive-technology interoperability remain open. This is focused
route evidence, not full MultiSelectComboBox parity.

Lazy MultiSelectComboBox paging, filtering and stable identity have a separate
focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-lazy-multi-select-combo-box
```

The independent route uses only public Flow APIs. Its page-size-10
`CallbackDataProvider(fetch, count, id)` supplies 80 logical reviewers as
fresh, unequal Java objects on every fetch. The initial server value is `R-060`.
One trusted generated AppKit primary click opens and focuses the owner; nine
trusted generated AppKit keydowns follow the
exact `z`, ArrowDown, Enter, Escape, Escape, `z`, Escape, `x`, Escape sequence,
with each typed filter keydown producing one trusted input event.
Browser-programmatic work is limited to route navigation, owner positioning and
ordinary scrolling of the owning listbox resolved through the input's public
ARIA relationship.

The distant-page checkpoint requires a nonzero-offset fetch to render a fresh
`R-060` option with `role=option` and `aria-selected=true`. The `z` query must
render only fresh `R-061` while retaining `R-060`; ArrowDown focuses it and
Enter produces one client-originated Java selection event and one semantic
selection UIDL checkpoint, with logical `R-060` retained and `R-061` added. The
first Escape clears option focus while retaining the open filtered popup; the
second closes it and resets input, filter and rendered options. Typing the same
`z` term after that close/reset reopens the owner and re-queries a fresh
`R-061` option with `aria-selected=true`; both logical selections and chips
remain intact before the next Escape closes and resets the field. Typing `x`
then reopens the owner, re-queries a fresh `R-060` option with
`aria-selected=true`, and retains both logical selections and chips before the
final Escape closes and resets the field again.

The gate records semantic far-page, first-`z`, selection, repeated-`z` and `x`
request/UIDL-response checkpoints instead of making raw envelope counts or
payloads part of the contract. It drains balanced opaque requests and responses
through a quiet interval, then requests graceful sidecar shutdown. Focused push
is false.
Unknown-size providers, mutation-driven data refresh, manual refresh and
broader pagination/filter and close/reopen combinations, physical input
hardware, touch and assistive-technology interoperability remain open. This is
focused evidence, not full MultiSelectComboBox parity.

Localized owner-bound time input has a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-localized-input-popup
```

The gate uses TimePicker as a probe with `Locale.US`, a 12:00-14:00 range,
30-minute steps and an initial 13:00 value. It requires the official 12-hour
display and listbox accessibility, initial owner-input focus without a focused
candidate, AppKit ArrowDown traversal through 12:00 PM, 12:30 PM, 1:00 PM and
1:30 PM, Enter selection, Java value-listener execution, the returned UIDL
patch, close and owner-focus restoration. It uses the standard production
bundle and opaque request/response/push transport in a real WKWebView. This
focused contract does not cover other locales, validation, physical hardware,
touch or assistive-technology interoperability.

Localized decimal input and an in-place locale change have a focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-localized-number
```

The isolated route uses public `BigDecimalField` APIs with explicit
`ValueChangeMode.ON_CHANGE`, `Locale.GERMANY` and initial `42.50`. Generated
AppKit primary clicks and keyboard events select the field, replace its content
with `1234,567890123456789` and commit with Tab. A client-originated Java listener
must report the exact `BigDecimal` plain value and scale 15. A trusted click on a
Flow Button calls only `setLocale(Locale.US)`; the returned UIDL must change the
display separator to a dot while preserving the Java value, scale and event
count. A second native edit to `9876.125` and Tab commit must produce the second
client-originated value event, including the correct old value and scale.

The gate cross-checks initial, German-commit, locale-switch and US-commit
snapshots, exact Java event history, trusted DOM key/input/click receipts and
ordered AppKit dispatches. The runner enables full-detail transport tracing for
scope, generation and response-end correlation. Cmd+A goes through
`NSApplication.sendEvent` and the
production host's standard Edit menu/responder chain. Each full selection must
be observed on the focused input before the first replacement character; a
menu-consumed Cmd+A need not also produce a DOM keydown. Only those two optional
keydown receipts may be absent; all typed characters, input events and Tab
commits remain required. Each Java transition requires new opaque
request/response progress, and the final exchanges must drain through a quiet
interval before graceful shutdown. Browser code only navigates, positions and
observes the probe; it does not assign the input value or synthesize input
events. Valid main-scoped entry-page push/ack traffic is allowed and audited,
but cannot replace the three request/response checkpoints. This focused
standard-bundle WKWebView contract does not claim push,
physical keyboard hardware, IME, assistive technology, grouping/currency
formatting, other locales, RTL, empty/malformed input or Binder validation.

Localized decimal validation and bound-model recovery have a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-localized-number-validation
```

The independent ordinary form binds a German-locale `BigDecimalField` through
the standard Binder default validator, required validator and custom maximum
of 100. It starts with bean/field value `42.50`. Five generated AppKit edits,
each committed with Tab, cover empty required input, malformed `1,,2`, valid
`12,50`, custom-rejected `150,00` and valid `7,125`. Browser code only navigates,
positions and observes; it never assigns values, dispatches synthetic input or
calls validation methods.

Empty input changes the Java field to null and preserves the last valid bean.
Malformed nonempty input also parses to null, so that transition must not add
a value-change event. Instead, the component validation-status event must
advance, Binder must revalidate, and the bad-input error must replace the
required error while retaining the bean. Valid recovery updates the bean;
the later default-valid custom rejection changes the field but preserves the
previous valid bean, before a final valid edit updates both with exact scale.
The four actual Java value changes must all be client-originated, with exact
old/new values and history. Recovery requires valid state and hidden error
presentation, not clearing an upstream-retained raw error-message property.
The last component validation-status event is historical evidence, not current
Binder validity; it may still report the earlier malformed input after recovery.

Five native focus clicks, 30 native key pairs, four actual Cmd+A full-selection
effects and 21 trusted input events are required. AppKit may consume each of
the four Cmd+A shortcuts without a DOM keydown; all other key/input receipts
remain mandatory. Each Tab commit, including the null-to-null malformed
transition, requires its own completed post-key opaque UIDL response. Six
stable snapshots bind actual input, component invalid/error presentation,
Java validation and Binder status, last-valid bean and main-window/sidecar
ownership; a final quiet drain precedes graceful shutdown. The original
locale-switch gate is independent and unchanged. This focused contract does
not prove locale breadth, grouping/currency, RTL, IME, physical input,
assistive technology, cross-window form ownership or focused-route push.

`bash bin/native-host-application-menu-smoke` separately checks the production
App/Edit menu structure, standard selectors and keyboard equivalents, nil
targets and automatic validation. It does not send editing actions, touch the
clipboard, create a window or prove all responder-chain owners at runtime.

Native editing history has a separate focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-native-edit
```

The public `TextField` probe starts empty with `ValueChangeMode.EAGER`. One
generated AppKit primary click focuses its actual input, then application-level
keyboard dispatch sends `x`, Cmd+Z and Cmd+Shift+Z. Each step must finish its
Java listener and opaque request/response round trip before the next key. The
required value history is empty → `x` → empty → `x`, with exactly three
client-originated Java changes and the corresponding old values. Trusted DOM
input events must report `insertText`, `historyUndo` and `historyRedo` in that
order; each server status/history patch must be visible in the returned DOM.

The shortcuts use `NSApplication.sendEvent` and the production host's standard
Edit menu/responder chain. Only the optional DOM keydown for each command
shortcut may be absent when AppKit consumes it; all three native dispatches,
all three trusted input events and the three request/response checkpoints
remain mandatory. Browser-programmatic work only navigates, positions and
observes the input: it does not assign values, synthesize input or invoke an
editing command. The inspector binds ordered input/stage receipts to the same
main window, opaque session and sidecar generation, audits full-detail opaque
envelopes and a balanced quiet drain, and requires graceful shutdown. The
runner holds the shared package lock through its final trace inspection.

This contract covers a single-character editing history in one focused
TextField, not arbitrary undo grouping, rich-text editing, cross-window or
native-sheet command ownership, clipboard commands, physical keyboard
hardware, IME, assistive technology or focused-route push. Entry-page push/ack
traffic is allowed but cannot substitute for the editing round trips.

Two-window native editing ownership has a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-native-edit-isolation
```

The same public TextField route opens a secondary through a trusted Flow Button
and `Desktop.current().windows().open`. Each window has its own empty EAGER
field, Java listener history and opaque session. The fixture uses the public
`setAutocorrect(false)` and `setAutocapitalize(Autocapitalize.NONE)` APIs; the gate
checks `off`/`none` on both the component and its actual input. This separates
editing-owner history from native text substitution on focus loss without
changing system preferences. Generated AppKit input then
interleaves main `x`, secondary `y`, main Undo, secondary Undo, main Redo and
secondary Redo. Every command is preceded by a trusted click on that window's
actual input and travels through `NSApplication.sendEvent`; a final trusted
click returns editing focus to main without another edit. The complete contract
requires eight native pointer dispatches and six keyboard dispatch pairs.

Paired observations after each step require the targeted window's exact
client-originated Java old/new value history and trusted input receipts, while
the other window's editing state stays unchanged. Both windows finish with
exactly three Java changes and their own `insertText`, `historyUndo` and
`historyRedo` sequence; main contains `x`, secondary contains `y`. An inactive
document may retain its DOM active element, and ordinary background transport
may advance: native key-window ownership and unchanged peer editing state are
the isolation evidence, not an assumed absence of all background activity.

The gate correlates each of the six post-key UIDL request/response completions
within its exact window/session/UI tuple. It permits request IDs to repeat
across different WebViews but rejects cross-scope evidence substitution. The
secondary session is the host-generated opaque UUID, not its public window id.
Both native windows, WebViews, scopes and the sidecar must remain stable through
the interleaved sequence and return to main. Paired quiet observations and a
final balanced drain precede closing both windows and graceful shutdown. The
runner keeps the shared package lock through trace inspection.

This verifies two existing WebView editing owners and their independent native
undo histories. It does not cover native-sheet text fields, clipboard commands,
automatic text substitution, arbitrary undo grouping, rich-text editors, owner close/reopen or sidecar
recovery during editing, physical input, IME, assistive technology or focused
push. Navigation and geometry setup remain browser-programmatic; browser code
never writes the input values or invokes Undo/Redo.

Native editing after closing and reopening the same public window id has an
independent focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-native-edit-reopen
```

The ordinary TextField route adds a public close button with its own actual
client-listener status and counter. A successful close reports `closing`, not
completed teardown; it enables the existing open button for a later operation.
The driver waits for the old native context to leave the registry and its window
to become hidden, plus the main close-status UIDL, before the next native open
click. The inspector independently requires the old exact-scope `windowClosed`
before the second open/create sequence. The same public secondary id must receive
a different opaque session, context, native window and WKWebView. The acceptance
retains old object references solely to compare identities; it does not prove
their deallocation or directly inspect an internal WebKit undo manager.

Main first receives native `m`, incarnation A receives native `x`, and A closes.
Fresh incarnation B starts empty and receives native Undo. At least one second
of monotonic elapsed time and three quiet observations must show no input event
or Java value/history change; an empty screenshot alone is insufficient. This
no-op does not require a UIDL mutation, and balanced background transport may
advance. B then receives native `y`, Undo and Redo with its own exact three-event
Java history. Finally main performs Undo and Redo to prove its retained history
still works while B remains unchanged. The main state is frozen during B edits,
and B's state is frozen during the final main edits.

The contract has twelve ordered checkpoints, eleven native pointer dispatches
and eight application-level key pairs. Seven actual value changes (main three,
A one, B three) require trusted input and distinct completed post-key UIDL
responses, each correlated by the full window/session/UI/request tuple. The
three native open/close/open button clicks require actual Java listener state,
public native command/result and their main-scoped UIDL returns. Command DOM
keydown may be consumed by AppKit, but native owner/responder checks, dispatched
keys and subsequent positive editing controls remain mandatory. Browser code
only navigates, positions and observes; it never assigns values or invokes Undo.

A balanced final quiet drain precedes closing B and main and graceful shutdown.
The runner retains the package lock through independent trace inspection. This
is one bounded same-id retirement/reopen cycle under generated AppKit input,
not arbitrary repeated churn, old-object destruction, native-sheet/rich-text
ownership, physical input, IME, AT, sidecar recovery/replacement or focused-route
push. Existing single-window, two-retained-window and Cut/Paste gates remain
independent.

Native editing across one unexpected sidecar exit has a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-native-edit-recovery
```

The main window navigates to the ordinary native-edit route and opens its
secondary through the public window API. A generated AppKit key inserts `x`
in that secondary. Only after the trusted input, client Java change, completed
post-key UIDL and balanced quiet observations does the driver freeze the old
document's evidence and terminate the exact current JVM with SIGKILL.

Recovery must retain both native contexts, windows, WKWebViews, window numbers
and opaque scopes while changing the JVM PID and advancing the recovery
generation once. The secondary's immutable route-specific entry URL reloads
the edit route directly. The main window reloads its original bundle root; its
earlier browser navigation is not a persisted route contract. No second
browser-programmatic navigation is allowed after recovery. Native recovery
ready alone is insufficient: the driver also waits for both new documents,
navigation completion, Flow clients, first paint and balanced transport.
The actual `performance.timeOrigin` of each document must change across recovery
and remain stable within its generation; marker arrays are never manually reset.

The recovered secondary starts empty with zero Java changes and fresh browser
event records. Native Cmd+Z must leave input and Java history unchanged for at
least 1000 monotonic milliseconds and three quiet polls. No artificial UIDL is
required for this no-op. Native `y`, Undo and Redo then establish a fresh
three-change Java history, each with a distinct completed post-key UIDL. The
main root remains available; balanced background transport and its ordinary
push probe may advance without becoming focused editing-push evidence.

Eight ordered checkpoints, six native pointer pairs, five key pairs and four
actual editing changes cover the old and new documents. Independent request
and push checks use sidecar generation plus full window/session/UI identity;
request IDs and push sequence numbers may restart after recovery. Asynchronous
driver callbacks are also fenced by the captured PID and document generation,
not merely the retained WebView identity. The runner owns the package lock
through inspection, and final quiet observations precede both window closes,
empty resource drain and graceful shutdown of the recovered JVM.

This is settled-edit recovery under generated AppKit input, not preservation of
application values or undo history, internal undo-manager destruction, in-flight
editing recovery, planned replacement, repeated failures, native-sheet or
rich-text editing, physical input, IME or assistive technology.

Native Cut/Paste has an independent focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-native-clipboard-edit
```

It reuses the ordinary empty EAGER TextField and public autocorrect/autocapitalize
configuration. One generated AppKit focus click and four application-level key
pairs type `x`, select all, Cut and Paste through the standard Edit menu/responder
chain. The full selection must actually change from a collapsed cursor to
`[0,1]` without another Java value event. Trusted target-owned `cut` and `paste`
events must precede `deleteByCut` and `insertFromPaste`; together with the initial
`insertText`, they produce the exact three client-originated Java changes
empty → `x` → empty → `x`. Each value change needs its own completed post-key
UIDL request/response. Only the three command-shortcut DOM keydowns are optional;
the native dispatches and actual clipboard/input effects are mandatory.

The driver snapshots the existing system pasteboard's materializable item/type
bytes privately before Cut, requiring a stable change count and refusing an
incomplete snapshot. It does not seed the test text into the pasteboard. Actual
native pasteboard observation after the trusted Cut/deletion must find `x` and
an advanced change count; Paste and the quiet drain must retain that owned
epoch. Successful restoration of the original byte representations is required
before final acceptance. A separate deterministic helper smoke covers capture,
ownership and bounded cleanup failures without touching the general pasteboard.

```bash
bash test-support/native-host-macos-acceptance/bin/native-clipboard-edit-pasteboard-smoke
```

This smoke is also part of `bin/verify-smoke`. Pasteboard retrieval errors are
not treated as an empty snapshot. Materialization is limited to 64 items, 64
representations per item and 16 MiB of retained bytes; incomplete or oversized
snapshots abort before Cut. Cleanup permits at most six attempts in total.

Observed contention stops restoration without another write. Failure after Cut
dispatch but before ownership can be established is explicitly uncertain: the
driver must not claim restoration or overwrite an unowned pasteboard. Bounded
owned restoration is attempted on normal completion, failure and graceful
termination, but AppKit has no compare-and-swap operation; an external writer
can still race between a check and mutation. The in-memory snapshot cannot
survive a hard crash or `SIGKILL`, and materialized bytes do not preserve the
original lazy provider or file-promise identity.

Original clipboard bytes and unexpected pasted text are not emitted in driver
receipts or failure snapshots; unexpected field/history strings are redacted,
and this gate suppresses automatic WebView screenshots. Browser code only
navigates, positions and observes, never assigns values, invokes editing
commands or writes clipboard data. The full-detail inspector correlates actual
input, native clipboard observation/restoration, Java histories and opaque
streams with one retained main window/WebView/sidecar and clean shutdown. This
is a generated-AppKit plain-text editing slice, not public Clipboard capability
permission/outcome coverage, physical input, IME, AT, non-text formats,
adversarial pasteboard contention, multi-window/native-sheet ownership or
focused-route push.

Bounded picker validation has a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-picker-validation
```

The gate uses DateTimePicker as a composite-picker probe with a 12:00-14:00
range. Programmatic browser input in the real WKWebView drives the component's
own commit state machine through minimum, maximum, incomplete, required and
malformed default-validation branches, with a valid recovery between branches
and after the final malformed value. The same field then uses the public Binder
default/required/custom validator chain. One value violates both the minimum and
30-minute custom rule and must retain the default error plus the previous bean;
another passes the defaults but reaches the custom error and again retains the
bean; a distinct valid value must clear the invalid presentation and update the
bean. The gate requires client-originated Java value listeners, the malformed
branch's Java validation-status listener, Binder status, configured messages,
parent/child invalid and error-presentation UIDL patches, recovered values and
closed child overlays. Per the upstream `HasValidation` contract, recovery does
not require clearing a hidden raw custom error-message property. The path uses
the standard production bundle and opaque request/response/push transport. This
focused contract does not cover remaining picker variants, additional locales,
physical hardware, touch or assistive-technology interoperability.

Owner-bound menu popups have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-menu-popup
```

The gate uses MenuBar, ContextMenu and GridContextMenu as probes of the shared
standard Flow/browser path, not as component-specific product adapters. It
requires MenuBar owner/expanded ownership, menu accessibility, initial focus,
ArrowDown movement, Enter selection, Java listener execution, Escape close and
focus restoration. Its hierarchical MenuBar sequence additionally verifies
child-submenu ownership and accessibility, ArrowRight opening, ArrowDown leaf
movement, ArrowLeft return while retaining the root menu, leaf Java listener
execution, and layered Escape close with focus restored first to the parent
item and then to the owner. Its responsive MenuBar sequence constrains the
available width and requires the default collapse-from-end policy to move
actual items into the overflow menu, with overflow ownership, accessibility,
AppKit-key selection, Java listener execution, Escape focus restoration and
expanded-width restoration of all original top-level items. A collapsed parent
item additionally opens one child submenu with ArrowRight and verifies
collapsed-parent ownership, root/child menu accessibility, child keyboard
movement, ArrowLeft child close while retaining the overflow root and restoring
collapsed-parent focus, child reopening, Java leaf selection, full-stack focus
restoration after selection, and layered Escape restoration first to the
collapsed parent and then to the overflow button. It opens ContextMenu with an
AppKit secondary-click delivered to the WKWebView, requires a trusted DOM
`contextmenu` event, then verifies the
same focus, keyboard, selection, Java opened/action/closed listener and
restoration behavior. Its nested ContextMenu sequence additionally verifies
child ownership and accessibility, ArrowRight opening, ArrowDown movement,
ArrowLeft return while retaining the root menu, Java leaf selection,
full-stack close after selection, and layered Escape restoration first to the
parent item and then to the target. The Grid sequence scrolls to a distant
virtualized body row, verifies that fewer rows than the backing data are
rendered, secondary-clicks the visible cell through AppKit, and requires the
trusted DOM target plus Java row/column context before keyboard selection and
focus restoration. A nested GridContextMenu path on the same virtualized row
additionally requires child ownership and ARIA, Right/Left traversal with root
retention, parent-focus restoration and retained Java row/column context, child
reopening, a Java leaf listener carrying the row item, full-stack close after
selection, and layered Escape restoration first to the parent item and then to
the Grid. A separate
one-row Grid policy path uses the public `setDynamicContentHandler` API. Trusted
AppKit secondary-clicks on its header and trailing blank body area produce
item-absent event contexts, execute the Java predicate and are rejected without
opening the menu. A row secondary-click executes the same predicate with
`P-001`, updates the menu item text, opens with the expected accessibility and
initial focus, then reaches the Java action/close listeners and restores focus
to the Grid. Route navigation and Grid scrolling are scripted test setup; menu
keyboard input and secondary-clicks are AppKit window events. The host window
must be frontmost and key before native input; the gate waits and fails instead
of simulating browser focus. This contract does not claim physical
keyboard/mouse hardware, touch or long-press input, deeper MenuBar chains,
reverse-collapse policy or deeper chains inside overflow,
deeper GridContextMenu child chains, or assistive-technology interoperability.

Structured Flow JavaScript RPC semantics have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-structured-rpc
```

The gate sends a nested bean/list argument through `@ClientCallable`, verifies
the Promise resolves with a nested list/map response, passes a Java bean to
`executeJs`, converts the browser result through
`TypeReference<List<...>>`, and exercises the remaining core JSON codec
branches. The ClientCallable matrix covers numeric-string and enum decoders,
primitive arrays, a method-level generic collection, primitive-null defaulting,
a nullable reference and varargs, then checks enum, array and null output. The
PendingJavaScriptResult matrix round-trips Java numeric, enum, array, null and
nested-map arguments through `Class` and `TypeReference` conversion. The gate
also cancels another pending execution before it is sent and requires that the
canceled browser side effect never runs,
that canceling an already-sent execution returns false while its Promise still
completes, and that a disabled element blocks the default `@ClientCallable`
method while allowing its `DisabledUpdateMode.ALWAYS` method. It also queues
JavaScript on an invisible owner and proves execution is deferred until the
owner becomes visible, then queues another invocation on an invisible owner,
detaches and reattaches that owner, and proves the detached invocation remains
purged. Finally, it verifies typed generic-list CompletableFuture completion,
JavaScriptException failure and cancellation, session-lock blocking protection,
multiple subscribers registered before send, and post-send subscription
rejection while the original subscriber still completes. It creates server-side
modality with `UI.addModal`, proves a normal ClientCallable is ignored with its
browser Promise left pending, proves an `@AllowInert` method completes, removes
the modal component and proves the normal method resumes. Opaque
request/response and push evidence must be present and shutdown must be
graceful.

Real-WKWebView Vite/HMR acceptance has a dedicated test-support entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-vite-development
```

It runs `dev-vite`, changes an application-owned frontend probe, waits for HMR
inside WKWebView, restores the source file, closes the app and applies the Vite
development trace gate. The probe and file mutation remain in `test-apps` and
`test-support`; production host and Flow sources contain no fixture knowledge.

Real-WKWebView Java rebuild acceptance has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-java-reload-development
```

It runs `dev-vite`, changes a test-application Java source, waits for Maven
rebuild and planned sidecar replacement, and verifies that the same native
window and WKWebView show the rebuilt Java value through a fresh Flow session.
The runner restores the source and applies
`--require-flow-ipc-java-reload-development`.

Effective Maven reactor reload has a separate real-window entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-reactor-reload-development
```

It activates a test-only Maven profile whose dependent module declares custom
Java and resource roots. The gate first changes a normal dependent module's POM
property, then independently changes Java and resource inputs under those
custom roots. It requires Maven's selected effective reactor to drive all three
rebuilds, exactly three planned sidecar replacements, Vite workspace refresh
for the POM change, retained native window/WKWebView identity, fresh values and
recovered Flow request/push paths after each replacement. The runner restores
all fixtures and applies `--require-flow-ipc-reactor-reload-development`.

Failed Java replacement rollback has its own real-window entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-java-rollback-development
```

It compiles a test-application candidate that exits before Java `ready`,
requires the CLI to restore the last ready staged JAR, and verifies that the
same native window and WKWebView establish a fresh Flow session through the
restored runtime. The runner restores its source fixture and applies
`--require-flow-ipc-java-reload-rollback-development`.

These three replacement gates require a development webapp and the CLI-owned
rebuild/rollback transaction. The product standard-bundle host does not start
the development control channel, and its controller rejects planned replacement
as `unsupported-mode`. Do not count these gates as product-runtime planned
replacement or noncooperative-process termination evidence. Automatic recovery
of the bundled sidecar is a separate lifecycle contract and gate.

Fatal startup propagation has a separate host-lifecycle gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-startup-failure
```

It packages a standard Flow IPC app, temporarily substitutes an invalid sidecar
JAR inside the generated acceptance app, and verifies nonzero host termination,
no window creation before Java `ready`, trace evidence and fixture restoration.
The production CLI contract separately verifies that a native host exit status
is preserved.

A live-but-unready sidecar has its own lifecycle gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-ready-timeout cooperative
bash test-support/native-host-macos-acceptance/bin/run-host-ready-timeout blocked
```

It packages the standard app with a short product-level initial-ready deadline
and substitutes a test-support Java sidecar only in a private app clone. Neither
mode sends `ready`; the cooperative shutdown hook returns, while the blocked
hook waits forever. The default mode is `cooperative`. The gate requires an
empty native-resource drain, matching actual child exit, then fatal host
completion with status `1`; blocked mode must pass through TERM and KILL.
An independent verifier matches private startup/hook markers and host/JVM PIDs,
requires both processes absent, and appends `hostReadyTimeout.externalExitObserved`
before cleanup. Any cleanup of a live owned process forces failure. A bounded
external watchdog cannot rescue PASS. This pre-ready failure intentionally
creates no WKWebView and is not UI/listener evidence; existing real-window
shutdown gates provide the complementary normal-runtime regression.
Logs remain as `host-ready-timeout-{cooperative,blocked}.out.log` and `.err.log`
under the host build directory; only the exact owned clone/state are removed
after process absence is confirmed. Planned replacement, other fatal causes
and active file-grant cleanup are separate validation boundaries. The shared
retirement smoke covers callback timeout/retry through a scheduling double;
fatal-host retry and normal/fatal termination contention are source-reviewed,
not real OS signal-error injection or GUI retry evidence.

An acceptance host that receives Java `ready` but never finishes its initial
WKWebView navigation has a separate failure gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-navigation-timeout
```

The explicit acceptance observer starts its deadline when the initial window
opens, before `didFinish`. With auto-shutdown enabled, a missing initial
navigation completion emits `flowIpcInitialNavigationTimeout` and requests
normal AppKit termination so the sidecar follows the host-shutdown path. The
runner reports failure even when LaunchServices itself returns zero. This is
an acceptance watchdog, not a new production timeout or WebKit recovery policy.
It requires a responsive AppKit main run loop and is not an external detector
for a hung host main thread. Its Quit action is deferred to the run loop so
the main-queue work item returns before asynchronous resource drain begins.

The focused runner injects `stopLoading()` only into that initial test-host
WKWebView after arming a two-second deadline. Its
`--require-flow-ipc-host-navigation-timeout` gate requires the original
scope/JVM, ordered arming/cancellation/timeout, no successful navigation or
Flow first paint, and graceful sidecar shutdown without replacement. Passing
this negative gate proves bounded failure handling, not successful Flow
startup, input, UIDL/push, App Sandbox or Powerbox enforcement. The pure
watchdog state-machine regression is `bash bin/native-host-initial-navigation-smoke`.

Repeated unexpected sidecar exits have two real-window paths. The recovery path
first leaves a real `UI.access` push unacknowledged, replaces the sidecar and
requires a fresh session sequence with no stale UIDL replay. It retains the
native window and WKWebView, verifies attempts one and two, waits for the manifest
stable-run deadline, then requires the next failure to restart at attempt one:

```bash
VAADIN_DESKTOP_MACOS_NO_ACTIVATE=1 \
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_RESTART=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 \
bash test-support/native-host-macos-acceptance/bin/run \
  dev test-apps/flow-over-ipc-framework-app
```

The exhaustion path sets a two-attempt manifest budget, injects three unexpected
process exits and requires the host to terminate nonzero:

```bash
bash test-support/native-host-macos-acceptance/bin/run-sidecar-recovery-exhaustion
```

An unresponsive recovery process has a separate standard-bundle entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-recovery-termination
```

The runner creates a private app clone and private three-generation test state.
Gen1 establishes a trusted generated AppKit click, Java listener, UIDL and push
before the driver kills only its held PID. Gen2 is test-app-only: it never sends
`ready` and blocks inside its SIGTERM shutdown hook. The driver reads its exact
PID/run marker but does not terminate it. Production must hit the 15-second
ready deadline, request TERM, escalate after one second, observe actual exit,
then start Gen3. The original window/WebView/scope survive; the document changes
only after Gen3 `ready`, followed by another native click and fresh Java/push
state. The test does not require notification or other OS permission choices.

`--require-flow-ipc-recovery-termination` checks ordered PID/generation-bound
lifecycle evidence, two independent document traffic epochs, trusted DOM
receipts, balanced requests and acknowledged pushes, and graceful final
shutdown. Logs remain in `host-recovery-termination.out.log` and `.err.log` under
the normal host build directory. The owned clone/state are removed only after
its exact host/JVM processes are confirmed stopped. Runner mock smoke checks
ownership matching and cleanup without launching a GUI; the production-process
regression is `bash bin/native-host-sidecar-recovery-termination-smoke`.
That process smoke exercises both recovery and replacement retirement using the
real `JavaSidecar` with dedicated non-Java children. It checks one-second
TERM-to-KILL escalation, duplicate and mixed requests retaining the first
deadline/reason, cooperative and pre-deadline exit, fresh-generation protection,
and host-shutdown takeover. Replacement uses distinct trace event names; it
does not become a recovery event or consume a restart budget at this layer.
Its separate callback-fault binary substitutes only callback delivery: the real
child exits after KILL, but its callback is held past the two-second retirement
confirmation deadline. Both retirement kinds must report exactly one fatal
failure without emitting synthetic exit or permitting a fresh generation; a
late callback must settle a subsequent host-shutdown attempt. This is a
scheduling double, not real OS signal-error injection.
The process smoke does not prove controller transitions, signal-error recovery,
Java shutdown hooks or a standard-bundle product replacement entry.
The real-window recovery gate proves generated AppKit input, not physical hardware or assistive
technology. The focused recovery contract excludes ordinary Quit, initial
startup, planned replacement, durable application state and guaranteed cleanup
of application side effects; OS scheduling is not a hard real-time guarantee.

Ordinary final-window shutdown has two isolated standard-bundle runs:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-shutdown-termination cooperative
bash test-support/native-host-macos-acceptance/bin/run-host-shutdown-termination blocked
```

Both use two generated AppKit clicks, a public Java listener, visible UIDL,
consumed background push, then a trusted final-event title receipt and public
`window.close`. The cooperative hook returns; the blocked test-app hook waits
forever, so production must send TERM, escalate to KILL and observe actual exit
before approving AppKit termination. The driver never kills the child. Private
startup/launcher-returned/hook-entered markers identify the exact run/PID; the
launcher marker does not distinguish shutdown-envelope handling from EOF.

`--require-flow-ipc-host-shutdown-termination` requires the ordered empty
resource drain, exact original sidecar exit and final callback. After
LaunchServices reports host exit, the runner independently checks host/JVM
absence and marker identity, appending an external receipt before inspection
or cleanup. Any cleanup of a still-owned process forces failure. Logs are
`host-shutdown-termination-{cooperative,blocked}.out.log` and `.err.log` in the
host build directory; only the owned app clone/state are removed. No OS
permission choices or notifications are used. Generic `bin/run` rejects this
fixture flag so its private state cannot be omitted.

`bash bin/native-host-sidecar-host-shutdown-smoke` covers actual owned child
processes, duplicate requests, modal-only nested loops and stale recovery
deadlines. A separate scheduling double withholds a real child's exit callback
to check timeout, retry and late completion; it is not an OS signal-failure
test. Node runner/evidence smoke exercises ownership and fail-closed checks
without GUI. This gate does not revalidate active file grants, physical input,
AT, a full stdin pipe, fatal startup or planned replacement.

Sidecar stdin backpressure has a separate real-window fault probe:

```bash
bash test-support/native-host-macos-acceptance/bin/run-sidecar-write-backpressure
```

The private clone first establishes a generated AppKit click, public Java
listener, visible UIDL and consumed push acknowledgement through the standard
production bundle. Test-support then sends SIGSTOP only to the retained JVM
and confirms its stopped state with `KERN_PROC_PID`. Sixteen ordinary browser
GET requests carry fixed 16 KiB headers through the opaque bridge; they are
browser-programmatic load, not physical input or forged sidecar envelopes.
The gate requires actual writer EAGAIN evidence after suspension, followed by
a live AppKit-main-loop heartbeat and the product menu's Cmd-Q path. It then
requires the exact stopped child to exit after TERM/KILL before AppKit ends.
The driver never kills the JVM on the successful path.

`--require-flow-ipc-sidecar-write-backpressure` correlates original
PID/generation, window/WebView/document, prepare traffic, the bounded GET
burst, real pipe backpressure and normal host retirement. A startup-only
private marker and independent post-host-exit ESRCH checks precede all cleanup;
any cleanup of live owned processes forces failure. Logs remain as
`host-sidecar-write-backpressure.out.log` and `.err.log`. No notification or OS
permission choice is used. The generic runner rejects this fault flag.

`bash bin/native-host-sidecar-input-writer-smoke` covers real pipes, FIFO bytes,
partial/EAGAIN recovery, byte/frame capacity, SIGPIPE-safe EPIPE, idle timeout,
cancel/retire and descriptor isolation. The host-shutdown process smoke also
covers a full opaque write, a delayed old-generation writer failure, and a write
failure queued before an already-observed process exit whose callback is pending. These
are complementary lower-layer tests, not additional JVM/GUI evidence. SIGSTOP
freezes the whole JVM; this focused gate does not isolate only the reader
thread, prove application cleanup, unattended recovery, physical input or
general host responsiveness under unrelated blocking work.

## Trace Inspector

The host writes compact operational logs to:

```text
.build/native-hosts/macos/host.out.log
.build/native-hosts/macos/host.err.log
```

Inspect supported gates with:

```bash
bash bin/flow-ipc-trace-inspect --help
```

Examples:

```bash
bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-standard-bundle-runtime \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-standard-bundle-framework-app \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-capability-permission \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-capability-default-deny \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-capability-generation-recovery \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-capability-owner-window-close \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-capability-context-reopen \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-file-dialog-outcomes \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-main-window-close-recovery \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-flow-dnd \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-flow-overlay-stack \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-flow-input-popup \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-lazy-multi-select-combo-box \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-lazy-grid-selected-removal \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-multi-select-combo-box \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-localized-input-popup \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-localized-number \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-localized-number-validation \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-native-edit \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-native-edit-isolation \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-native-edit-reopen \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-native-edit-recovery \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-native-clipboard-edit \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-external-file-drop \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-picker-validation \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-structured-rpc \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-push-delivery \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-large-download-resume \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-large-upload-retry \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-large-multipart-upload-retry \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-concurrent-upload \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-request-fanout \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-concurrent-multipart-upload \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-in-flight-request-recovery \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-standard-bundle-sidecar-recovery \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-sidecar-recovery-exhaustion \
  .build/native-hosts/macos/host-sidecar-recovery-exhaustion.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-vite-development-runtime \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-java-reload-development \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-reactor-reload-development \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-java-reload-rollback-development \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-host-startup-failure \
  .build/native-hosts/macos/host-startup-failure.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-host-ready-timeout \
  .build/native-hosts/macos/host-ready-timeout-cooperative.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-host-navigation-timeout \
  .build/native-hosts/macos/host-navigation-timeout.out.log
```

Gate names describe semantic behavior; they are not wire negotiation or goal
versions. `HOST_PROTOCOL` is an acceptance trace prefix, not sidecar framing.

## Coverage Families

- Runtime: production bundle load, opaque bootstrap/UIDL, push, first paint,
  enabled feature flag initialization, capability result and shutdown.
- Push delivery: a test-support-only dropped final delivery, same-sequence and
  same-Flow-sync-id retry without later browser activity, official Flow client
  DOM application, matching last-seen acknowledgement and graceful shutdown.
- Push recovery: same-scope disconnect/reconnect with advanced Flow last-seen
  metadata, gap-triggered Java resync UIDL, continued normal push delivery and
  visible DOM continuity in a real WKWebView.
- In-flight request replacement: a real sidecar exits after receiving a bounded
  partial opaque body; the old document's XHR fails and its later request/push
  activity stays local, no browser envelope crosses the recovery interval, and
  the retained window/WKWebView reloads into a fresh Flow client and session
  whose distinct request reaches exact completion and Java listener/UIDL
  confirmation. Cross-process replay, resume and durable transfer state remain
  outside this contract.
- Large response resume: public Flow `DownloadHandler`, bounded response-reader
  cancellation, Java stream cancellation, open-ended `Range` propagation,
  correlated HTTP `206` response, exact remaining-byte reconstruction, Java
  listener/UIDL confirmation and clean shutdown in a real WKWebView.
- Large raw request retry: public Flow `UploadHandler`, XHR progress, standard
  abort/removal lifecycle, Java input-stream cancellation, re-selection of an
  equivalent file, a distinct complete retry request, exact bytes and metadata,
  public Java file-removed listener/UIDL confirmation and clean shutdown in a
  real WKWebView. Automatic or resumable retry remains outside this focused
  contract.
- Large multipart request retry: standard Upload `FormData`, unknown-length
  opaque request streaming, framing-aware progress and cancellation without a
  late end/response or accepted partial file; request-level isolation before
  application handler dispatch, distinct re-selection request, exact parsed
  file bytes/metadata, configured disk spill and temporary-part cleanup,
  successful handler/result, file-removed/all-finished listeners, UIDL
  confirmation, opaque push and clean shutdown in a real WKWebView. Broader
  Servlet multipart edge cases, concurrency, repeated cancellation and
  larger-scale behavior remain outside this focused contract.
- Concurrent raw request isolation: two standard Upload XHRs open in the same
  WKWebView before either terminates; one correlated request is canceled and
  removed while the other reaches exact request-end bytes and a successful
  response. Browser and Java overlap, public file-removed/all-finished
  listeners, UIDL confirmation, opaque push and clean shutdown are required.
  Standard queue saturation is covered by the separate fan-out gate;
  process-replacement recovery remains outside this focused contract.
- Opaque request fan-out: ten consecutive batches of six uniquely sized
  standard raw requests traverse Vaadin's default three-request client queue in
  one WKWebView. The trace must show three opens before the first terminal in
  each batch, twenty independently canceled requests after their tiered
  browser/Java thresholds, forty exact survivors totaling `33,640,688` bytes,
  strict sequential batch boundaries, browser/trace request-id agreement,
  public listeners, UIDL, push and clean shutdown. A
  test-only boundary probe requires completed-id reclamation and canceled-id
  late-response isolation after batch one, retention near the end of the
  production timeout, exact release after `60` seconds and healthy subsequent
  traffic. Arbitrary-duration or higher multi-window fan-out and process
  replacement remain open.
- Concurrent multipart request isolation: two standard `FormData` Upload XHRs
  and opaque streams overlap in one WKWebView; upstream Vaadin session locking
  serializes multipart parameter parsing to one request-scoped temporary
  directory at a time. Cancellation is isolated before application-handler
  dispatch, while the sibling completes with exact parsed bytes, configured
  disk spill, one successful public result, Java listeners, UIDL confirmation,
  temporary-root cleanup, opaque push and graceful shutdown. Bounded
  cross-session parsing is covered separately; higher fan-out remains outside
  this focused contract.
- Cross-session multipart parser concurrency: two native windows with distinct
  Vaadin sessions advance standard `FormData` streams beyond initial credit
  while two request-scoped spill paths coexist. Full-scope trace correlation
  proves that canceling one window does not dispatch its application handler or
  contaminate the second window's exact successful result, Java listeners,
  UIDL, push or temporary-part cleanup. Higher fan-out remains outside this
  focused contract.
- Capability permission: the same public `window.setTitle` command is allowed
  in `main` and manifest-denied in an isolated secondary window. Two trusted
  AppKit clicks reach client-originated Java listeners and UIDL responses; the
  main native title changes while the secondary title does not. Full trace
  dynamically correlates command/result ids and complete window/session/UI
  scopes and validates both permission payloads. Browser-programmatic work is
  limited to navigation and opening the secondary window. Raw envelope counts
  and the broader native permission, file-grant and OS-authorization surface
  remain outside this focused contract.
- Capability production default: CLI development commands explicitly select
  `allow-all-dev`; `package` and the product builder's own default select
  `manifest-enforced`. With no source manifest, the real builder stages an
  empty enforced grant set. A test-support-only omission hook proves that path
  in a real WKWebView with one trusted AppKit click, exact Java denial payload,
  UIDL response, unchanged native title, manifest-before-command trace order
  and unique dynamically scoped command/result correlation. Focused push is
  false; the broader native permission/file-grant surface remains open.
- Capability package policy: in product mode, the real builder rejects explicit
  `allow-all-dev` and `trace-only-dry-run` manifests with status `2` and the
  stable mode-specific diagnostic before destructive staging, retaining the
  byte identity of an existing enforced manifest and marker. The focused runner
  then delegates to the complete default-deny real-WKWebView gate; its two
  negative probes do not themselves launch a window. The CLI contract smoke
  separately verifies strict JSON input, build-time rejection of XML, missing
  and unsupported modes, development acceptance of both permissive modes, and
  canonical snapshot staging. A duplicate-key probe requires the final
  `manifest-enforced` runtime value to become the one staged mode without any
  permissive token.
- Capability generation isolation: a public Flow file-dialog listener opens real
  `NSOpenPanel` instances in two sidecar generations after generated trusted
  AppKit clicks. Test-support kills the first JVM without canceling its panel;
  product retirement clears, cancels and hides that operation, then traces and
  drops its stale completion. The retained native window/WKWebView recovers a
  new JVM that reuses `desktop-command-1` and the exact same scope, while only
  its separately canceled panel can complete the new Java listener and UIDL
  state. Ready-before-command ordering, generation-bound atomic writer admission,
  balanced quiet drain and graceful shutdown are required.
  Browser-programmatic work is limited to two navigations, focused push is
  false, and physical input is not claimed.
- Capability owner close: an isolated host-generated UUID secondary captures
  its capability context, then a background thread with
  `backgroundSessionAbsent=true` opens a real `NSOpenPanel`; public close from
  the retained main invalidates and releases the panel, makes public
  `openFileResult` return `INVALIDATED` from a correlated typed
  `owner-window-closed` result, then sends the exact `windowClosed` and destroys
  the secondary Flow session.
  Three generated trusted AppKit clicks cover dialog invocation, close and
  retained-main health without a panel-cancel hook. Same-process continuity,
  exact operation/result/lifecycle ordering, dynamic scope correlation and a
  balanced drain are required. The input evidence is generated, not physical.
- Capability context reincarnation: three native secondary incarnations reuse
  one public window/UI id while receiving pairwise-distinct host-generated UUID
  sessions and native window/WebView identities. B proves A's retired facade
  fails locally before and after B's fresh title; C proves both A's and B's
  retired facades fail locally before and after C's fresh title. No stale or
  retargeted title command is allowed. Five generated trusted AppKit clicks,
  scope-exact acknowledged pushes, same-sidecar/main continuity, balanced drain
  and graceful shutdown are required. Longer-running or unbounded cycles,
  higher fan-out, recovery/replacement combinations and physical input remain
  open.
- Production-host retired-scope wiring: a test-support fake sidecar drives two
  closed same-public-id UUID scopes and a fresh third scope through the actual
  production Swift composition, AppKit windows, WKWebViews and standard
  production bundle. The `main.swift` handler must return typed
  `owner-window-closed` results for both retired commands, drop both scopes'
  late HTTP/push transport, admit C's live command and browser
  request/response-credit round trip, and shut down gracefully without
  recovery. This is host-wiring evidence; the complementary real Flow gate,
  not this fake sidecar, supplies Flow UI/listener and UIDL evidence.
- File-dialog outcomes: public `openFileResult` returns `CANCELED` after
  test-support invokes `cancel(nil)` on one observed real `NSOpenPanel`, returns
  `ERROR` for a concurrent call after that unique-panel observation, and returns
  `DENIED` with a full permission audit from an isolated secondary without
  creating a panel. Three generated trusted AppKit clicks reach the corresponding
  Java listeners. The main and secondary listeners capture their exact contexts
  before their background threads observe `backgroundSessionAbsent=true`; both
  results reach the DOM through scope-exact automatic pushes and matching acks.
  The legacy `Optional` contract and classification precedence have a core
  smoke; owner-scoped read access has the separate focused grant gate, while
  broader asynchronous operation contracts and physical input remain open.
- Main-closed secondary recovery: a public secondary-scoped command closes
  `main`, generation one exits unexpectedly, and generation two receives the
  exact main-scope `windowClosed` replay while the same secondary native
  window, WebView and opaque scope reload their original route. A background
  default-scope probe is rejected locally with the typed close result and emits
  no ghost main command; one recovered secondary title command succeeds and
  reaches UIDL. Two generated trusted AppKit clicks, no main reconstruction,
  balanced drain and graceful shutdown are required. This is bounded
  two-window evidence, not full topology parity.
- Vite development: standard Vite client and Vaadin development resources,
  frontend HMR in real WKWebView, opaque Flow request/push ownership, source
  restoration and clean shutdown.
- Java reload development: successful application rebuild and atomic JAR
  staging, planned sidecar replacement, retained native window/WebView, fresh
  Java-rendered value, re-established standard Flow request/push paths, source
  restoration and clean shutdown.
- Reactor reload development: Maven-selected effective reactor observation,
  profile-activated dependency inclusion, inherited/interpolated custom Java
  source and resource roots, a POM-controlled filtered resource rebuild, three
  planned sidecar replacements, Vite workspace refresh, retained native
  window/WebView, recovered Flow request/push paths and fixture restoration.
- Java reload rollback development: a successfully built candidate exiting
  before `ready`, request-correlated host failure, atomic restoration of the
  last ready JAR, rollback `ready`, retained native window/WebView,
  re-established standard Flow request/push paths, fixture restoration and clean
  shutdown.
- Framework app: representative input including explicitly enabled Slider,
  selection, navigation including same-origin History API mapping and multiple
  non-eager routes with generated application-owned route/layout JavaScript,
  CSS and a transitive dynamic import, bidirectional lazy Grid viewport loading
  with recycled-row selection, server-side query sorting, configurable filtering
  and count/fetch refresh, backend hierarchical TreeGrid root/child queries,
  two-level client expansion, leaf selection, server child refresh and collapse,
  ComponentRenderer create/update lifecycle, LitRenderer property binding,
  client-function callback and server refresh patch, buffered Grid Editor/Binder
  save, cancel, required-field validation and model restoration,
  ClientCallable Promise resolve/server-reject, structured nested bean/list
  input with a nested list/map result, PendingJavaScriptResult browser Promise
  result/error callbacks, bean argument serialization, generic-list result
  conversion, numeric-string/enum decoder branches, primitive arrays,
  method-level generic collections, primitive-null and nullable-reference
  behavior, varargs, enum/array/null Promise output, `Class`-based scalar,
  enum, array and null result conversion, nested generic-map `TypeReference`
  conversion, pre-send cancellation, already-sent cancellation refusal and
  disabled ClientCallable update-mode enforcement, invisible-owner deferral
  until visibility returns, detached-owner pending invocation cleanup,
  CompletableFuture completion/failure/cancellation, session-lock blocking
  protection, pre-send/post-send subscription lifecycle, inert default
  ClientCallable suppression with a pending browser Promise, `@AllowInert`
  completion and post-modal recovery,
  data, single and nested modal Dialog initial focus,
  focus trapping, top-of-stack Escape close, client lifecycle round-trip, parent
  retention, layered focus restoration and accessibility semantics,
  anchored modeless Popover ownership, target ARIA, logical Tab order, Escape
  focus restoration, outside-click close with external focus preservation,
  mixed modal Dialog/modeless anchored Popover top-layer Escape and
  outside-click ownership, parent retention and two-stage focus restoration,
  two-level anchored modeless Popover nested ownership and accessibility,
  logical Tab order, top-layer Escape close, progressive outside-click close,
  parent retention, layered trigger-focus restoration and external-focus
  preservation,
  layout including AppLayout, slotted Card, experimental MasterDetailLayout
  virtual-detail content, Badge, Markdown, Icon/Avatar resources, rich content,
  authentication, upload/download and native capability workflows.
- Lazy Grid query: an isolated standard-production-bundle `MULTI` route with an
  80-item stable-identity `CallbackDataProvider`, five trusted generated AppKit
  clicks across three official row checkboxes and two Flow refresh buttons,
  exactly three client-originated Java `MultiSelectionEvent`/UIDL-response
  round trips, virtual-row selected-state rehydration, and a same-id fresh,
  unequal `L-060` replacement whose `refreshItem` response patches the cell
  without adding a selection event. Owner sorting and `PRESERVE_ALL` retain the
  refreshed selection across a `platform` filter. While both selected rows are
  outside the rendered viewport, removing matching `L-003` plus explicit
  `refreshAll` changes filtered/total cardinality from 26/80 to 25/79, advances
  fetch/count queries, emits one generic data-change event without a selection
  event, removes `L-003` from the rendered Grid, and preserves selection through
  row rehydration and filter reset.
  The same route also carries an independent fetch-only unknown-size Grid. Its
  client estimate grows from 40 to at least 80 before a positive short terminal
  fetch fixes the exact size at 67; a sixth trusted generated AppKit checkbox
  click selects the rendered tail item `U-067` through a client-originated Java
  `MultiSelectionEvent`/UIDL-response round trip. A seventh trusted click then
  appends through `U-093` and calls public `refreshAll`: one generic data-change
  event resets exact 67 to estimated 40, the estimate grows through at least
  120, backend fetch crosses the old tail, `U-067` selection rehydrates without
  another selection event, and a second positive short page settles exact 93.
  An eighth trusted click removes the 21 unselected tail items and calls
  public `refreshAll`: an independent `40E` to `80E` to `72X` epoch advances
  fetch/count events, rehydrates the same selected item without another
  selection event, evicts all removed ids and confirms the new positive short
  terminal page. Initial and append snapshots remain frozen and cross-checked.
  Navigation, viewport, sort, filter and refresh-button positioning are
  browser-programmatic setup; physical hardware, touch, assistive technology,
  selected-item removal or arbitrary repeated reset sequences, reorder or sort/filter-key
  mutation, broader pagination, larger scale, fresh-object identity across the
  unknown-size reset and other Grid capabilities remain outside this focused
  contract.
- Exact-count lazy Grid selected-item removal: three generated AppKit clicks
  select visible `L-060`, delete it after viewport eviction through explicit
  public `deselect`/backing-list removal/`refreshAll`, and select a surviving
  neighbor.
  Exact Java `true/false/true` selection-event provenance, 80-to-79 cardinality,
  independent post-click UIDL responses, removed-row eviction and empty
  selection precede positive neighbor reselection and viewport rehydration.
  The client selection array is checked as a cached-page projection, separately
  from retained Java selection. This does not claim automatic pruning,
  unknown-size selected deletion, multi-item transactions, recovery,
  focused-route push, physical input or assistive-technology interoperability.
- Flow DnD: isolated-route standard DragSource/DropTarget connector
  configuration, generated macOS primary-pointer routing with trusted browser
  drag lifecycle events, exactly-once client-originated drag-start/drop/drag-end
  Flow listener round-trips, server-side drag data, MOVE drop semantics and
  opaque request/response/push ownership in a real WKWebView. Physical hardware
  input and external-file behavior remain outside this contract.
- Flow overlay stack: isolated-route modal Dialog, anchored modeless Popover
  and auto-added modal ConfirmDialog ownership, default ConfirmDialog focus,
  accessibility, parent retention and layered focus restoration through Flow
  Java listeners and opaque request/response/push ownership in a real WKWebView.
  Browser-programmatic setup opens the layers; two generated AppKit window
  Escape sequences and one primary-pointer outside click close them with exact
  trusted DOM receipt. Physical input hardware, touch and assistive technology
  remain outside this contract.
- Flow input popup: isolated-route ComboBox, Select and DatePicker owner/popup
  accessibility, focus ownership, active-option/listbox/calendar keyboard
  navigation, selection, close, value retention, Java listener round-trips and
  UIDL patch behavior through opaque request/response/push ownership in a real
  WKWebView. Select opening, movement, selection, reopening and Escape close
  use AppKit keyboard events and preserve selected-item focus semantics. Select
  and DatePicker outside dismissal uses trusted AppKit primary-pointer events
  and verifies value retention, owner-focus restoration, Java lifecycle
  listeners, UIDL patches and suppression of an underlying Flow button click.
  The gate distinguishes scripted browser setup from AppKit events delivered to
  the WKWebView; physical hardware input, touch dismissal and broader popup
  families remain outside this contract.
- Flow MultiSelectComboBox: an independent public-API route with an initial
  `ava` token field, a clear-enabled `kai,sol` field, an explicit-ItemFilter
  keep-filter field and an explicit-ItemFilter default-clear field, driven
  through five trusted generated AppKit primary clicks, 15 trusted generated
  AppKit keydowns and two trusted input events.
  Two-stage Escape clears option focus then closes the popup; two-stage
  Backspace focuses then removes the `sol` chip. The second click deep-hits the
  token field's remaining chip remove button with a trusted composed path
  containing the remove button, chip and owner in order. A third click deep-hits
  the second field's shadow clear button, focuses its input through the standard
  pointer path, keeps its popup closed and removes `kai,sol` atomically. The
  token field produces exactly three client-originated Java
  `MultiSelectionEvent`/UIDL-response round trips; the clear field produces one
  event with `removed=kai,sol` and one matching UIDL response. The fourth click
  opens and focuses the keep-filter field; trusted `k` input reaches a latched
  server-filter request/response checkpoint, renders only `kai` while retaining
  `ava`, and ArrowDown/Enter adds `kai` through exactly one Java event and one
  distinct selection UIDL-response checkpoint. Two trusted Escapes clear its
  active option and close/reset it before the fifth click opens the
  default-clear field. That field repeats the independent server-filter and
  selection checkpoints, then default post-selection clearing empties its
  input/filter and server-rehydrates all options with chips `ava,kai` and ARIA
  selection map `ava:true,kai:true,sol:false`. The first two fields finish
  empty. Navigation and pointer positioning, scripted token-field focus, and
  empty-input/caret-at-start
  preparation before Backspace are browser-programmatic. Balanced opaque
  exchanges drain through a quiet interval before graceful shutdown; focused
  push is false. Lazy behavior has the separate bounded gate below;
  unknown-size providers, mutation/manual refresh,
  broader pagination/filter and close/reopen combinations,
  selected-items-on-top combinations, physical hardware, touch and assistive
  technology remain outside this focused, non-parity contract.
- Flow lazy MultiSelectComboBox: an independent public-API route backed by a
  page-size-10 `CallbackDataProvider(fetch, count, id)` that returns fresh,
  unequal objects for 80 stable logical ids and starts with `R-060` selected.
  One trusted generated AppKit primary click, nine trusted generated AppKit
  keydowns and three trusted input events cover a distant nonzero-offset `R-060`
  page with public option ARIA selection, a unique `z` query and `R-061`
  addition while retaining `R-060`, followed by two-stage Escape close/reset
  and the same `z` term re-querying a fresh selected `R-061` option while
  retaining both logical selections and chips. A later `x` re-query renders a
  fresh selected `R-060` option while retaining both chips. Navigation,
  owner positioning and scoped owning-listbox scrolling are the only
  browser-programmatic stages. Far-page, first-`z`, selection, repeated-`z` and
  `x` query checkpoints are semantic request/UIDL-response evidence
  rather than exact raw-envelope contracts. Balanced exchanges drain through a quiet
  interval before a graceful sidecar shutdown request; focused push is false.
  Unknown-size providers, mutation/manual refresh, broader pagination/filter
  and close/reopen combinations, physical hardware, touch and assistive
  technology remain outside this focused, non-parity contract.
- Flow localized input popup: isolated-route TimePicker `Locale.US` formatting,
  bounded step options, initial owner-input focus, AppKit ArrowDown candidate
  traversal, Enter selection, Java value-listener execution, UIDL patch, close
  and owner-focus restoration through opaque request/response/push ownership in
  a real WKWebView. Other locales, validation, physical hardware, touch and
  assistive-technology behavior remain outside this focused contract.
- Flow localized decimal validation: an independent German `BigDecimalField`
  bound form, empty required input, malformed null-to-null validation without
  another value change, valid recovery, custom maximum rejection and final
  exact-scale recovery. The gate requires five generated AppKit Tab commits,
  four client-originated Java value changes, actual component/Binder histories,
  last-valid bean retention, invalid/error presentation and five independent
  complete UIDL response checkpoints. The separate locale-switch gate remains
  independent. Other locales, grouping/currency, RTL, IME, physical hardware,
  assistive technology, cross-window forms and focused-route push are outside
  this contract.
- Flow picker validation: isolated-route DateTimePicker default-validation
  state machine covering minimum, maximum, incomplete, required and malformed
  values plus recoveries, followed on the same field by Binder default/custom
  precedence, custom-only rejection, bean retention and a distinct valid write.
  The real WKWebView gate requires component commit events, client-originated
  Java value listeners, Java validation-status and Binder-status evidence,
  configured messages, parent/child invalid and error-presentation UIDL patches,
  final recovery and closed overlays through opaque request/response/push
  ownership. Remaining picker variants, additional locales, physical hardware,
  touch and assistive-technology behavior remain outside this focused contract.
- Flow menu popup: isolated-route MenuBar, ContextMenu and GridContextMenu
  ownership, accessibility, initial focus, keyboard movement, selection, close
  and focus restoration, including a nested MenuBar child submenu with
  ownership/ARIA, Right/Left traversal, leaf selection and layered Escape, plus
  default end-collapsing responsive overflow with collapsed-item ownership,
  ARIA, Java selection, Escape focus restoration and expanded-width item
  restoration, including one collapsed parent with child ownership/ARIA,
  Right/Left traversal with overflow-root retention and collapsed-parent focus
  restoration, child reopening, Java leaf selection and layered Escape, one
  nested ContextMenu child submenu with ownership/ARIA, Right/Left traversal,
  Java leaf selection and layered Escape, plus virtualized Grid body-row/column
  context and one GridContextMenu child submenu with ownership/ARIA, Right/Left
  traversal with root retention, parent-focus and row/column-context retention,
  child reopening, a row-bound Java leaf listener, full-stack selection close
  and layered Escape. A compact Grid policy probe additionally verifies trusted
  header and trailing blank-body itemless contexts, Java dynamic-content
  rejection without opening, and row-specific dynamic text before Java
  selection, close and focus restoration. Opaque request/response/push
  ownership is required in a real WKWebView. MenuBar keys and
  ContextMenu/GridContextMenu secondary-clicks are AppKit window events with
  trusted DOM receipt; the focused host must be frontmost/key, while physical
  hardware, touch/long-press, deeper MenuBar chains, reverse-collapse,
  deeper chains inside overflow, deeper GridContextMenu child chains, and
  assistive technology remain outside this contract.
- Structured JavaScript RPC: nested bean/list `@ClientCallable` input,
  nested list/map Promise output, Java bean `executeJs` argument,
  `TypeReference` generic-list conversion, numeric-string and enum
  ClientCallable decoders, primitive arrays, method-level generic collections,
  primitive-null defaults, nullable references, varargs, enum/array/null
  Promise output, `Class`-based scalar/enum/array/null result conversion and
  nested generic-map `TypeReference` conversion, pre-send cancellation with no
  browser side effect, already-sent cancellation refusal with normal Promise
  completion, disabled `ONLY_WHEN_ENABLED`/`ALWAYS` ClientCallable enforcement,
  invisible-owner deferral until visibility returns, detached-owner cleanup
  across reattachment, typed generic-list CompletableFuture completion,
  JavaScriptException failure/cancellation, session-lock blocking protection,
  multiple pre-send subscribers, post-send subscription rejection, inert
  default ClientCallable suppression with a pending Promise, `@AllowInert`
  completion, post-modal recovery, and opaque request/response/push ownership
  in a real WKWebView.
- Multi-window: scoped sessions, request/push isolation and close behavior.
- Recovery: retained native window/WebView, replaced Java process, fresh Flow
  session, recovered interaction, consecutive attempt accounting and stable-run
  budget reset.
- Recovery exhaustion: two successful rapid recoveries followed by a third
  unexpected exit, manifest limit diagnostics and nonzero host termination.
- Host startup failure: sidecar launch before Java `ready`, no window creation
  and a fatal operational trace; the runner also asserts nonzero process status.
- Host ready timeout: live sidecar process, bounded initial handshake, no Java
  `ready` or window, cooperative/blocked shutdown hooks, actual process exit
  before fatal status `1`, and independent pre-cleanup host/JVM absence.
- Acceptance navigation timeout: Java ready and a real initial WKWebView,
  deliberately stopped initial navigation, scoped deadline failure and normal
  resource drain/sidecar shutdown; not successful Flow startup.
- Compatibility: loopback UI load through the same host plus real native
  capability result correlation.

The complete assertion set lives in the acceptance source and trace inspector,
not in this document.

## Log Budget

Compact production traces retain bounded protocol, scope, status, byte-count
and window metadata without inspecting opaque UIDL/bootstrap content or
component resources. The acceptance overlay precompacts DOM and fixture
diagnostics before emitting `webview.dom`; production trace code does not own
that schema. Full detail is enabled only for evidence inspection, and opaque
request/push bodies remain redacted:

```bash
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full bash bin/vaadin-desktop dev <module>
```

`bash bin/native-host-trace-boundary-smoke` verifies body redaction and rejects
UIDL-derived search indexes, component-resource summaries and acceptance-state
interpretation in production trace code.

Set `VAADIN_DESKTOP_MACOS_LOG_FOLLOW=1` to follow full logs live. Compact
auto-shutdown runs use `bin/flow-ipc-log-budget-check`; its size overrides remain
repository test settings.

## Generated Artifacts

Logs, traces, screenshots, downloaded files and native apps stay under `.build`,
CI artifacts, release assets or external storage. Do not commit one evidence file
per milestone.
