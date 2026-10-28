# macOS Native Host Acceptance Support

This directory contains repository-only WKWebView automation, DOM inspection,
snapshots, multi-window checks and sidecar recovery fault injection.

`bin/build-host` explicitly composes this overlay with the production macOS host
sources. `bin/run` prepares a validation application, builds that host and
forwards repository-only automation variables. The production CLI, builder and
launcher never reference this directory.

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 \
bash test-support/native-host-macos-acceptance/bin/run \
  dev test-apps/flow-over-ipc-framework-app
```

Opaque push delivery acknowledgement has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-push-delivery
```

The test-only overlay drops the first delivery of a final `UI.access` update and
does not create later browser activity. The gate requires same-sequence,
same-Flow-sync-id redelivery, a visible DOM update through the official Flow
client and a matching last-seen acknowledgement. The delivery queue remains
process memory and is not preserved across sidecar replacement.

Opaque push reconnect and resync have a separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-push-recovery
```

It requires same-scope disconnect/reconnect with advanced Flow last-seen
metadata, gap-triggered Java resync UIDL and a later normal `UI.access` push
updating the existing DOM.

In-flight opaque request interruption across unexpected sidecar replacement has
a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-in-flight-request-recovery
```

It kills the real sidecar after a public raw `UploadHandler` probe has received
more than 256 KiB, requires the old XHR to fail and late old-document fetch/push
activity to remain local, then verifies one recovery reloads the retained
window/WKWebView into a fresh Flow client and session. A distinct request must
complete with exact bytes, public Java listener/UIDL confirmation, push and
graceful shutdown. The Upload fixture, process kill, DOM IDs and diagnostics
belong only to the test app and acceptance overlay. The product contract is
generic request/push generation isolation, not automatic upload resume.

Large public `DownloadHandler` cancellation and Range resume have a focused
real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-large-download-resume
```

It consumes part of a deterministic response larger than 4 MiB, cancels the
browser reader, resumes with `Range: bytes=<consumed>-`, and requires Java
stream cancellation, HTTP `206`/`Content-Range`, exact remaining-byte
reconstruction, Java listener/UIDL confirmation, opaque push evidence and clean
shutdown. The automation and deterministic content remain in test-support and
the test app; production host and framework sources contain no download-specific
branch. Repeated or multi-range requests, larger scale and process-replacement
recovery remain separate gaps.

Large public raw `UploadHandler` progress, cancellation and re-selection retry
have a separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-large-upload-retry
```

It starts the standard Upload XHR for deterministic content larger than 4 MiB,
observes progress past 256 KiB, invokes Vaadin's abort/removal path, re-selects
an equivalent file and completes the second request. The gate requires request
cancel correlation, Java input-stream cancellation, exact retry bytes and
metadata, browser start/progress/abort/remove/success events, a public Java
file-removed listener and UIDL patch, opaque push evidence and clean shutdown.
Automation and deterministic content remain in test-support and the test app;
production host and framework sources contain no Upload-specific branch. This
does not prove automatic or resumable retry, larger scale or process-replacement
recovery.

Large multipart Upload parsing cancellation and re-selection retry have a
separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-large-multipart-upload-retry
```

It sends deterministic content larger than 4 MiB through standard Upload
`FormData`, verifies two multipart request events and framing-aware progress,
then cancels the first unknown-length opaque request without a late end or
successful response. The canceled request is isolated during Servlet multipart
parameter parsing before the application handler is dispatched, with no
accepted partial file or fabricated failed component result. Re-selection
creates a distinct request that parses the exact file and metadata, spills it
above the configured threshold, invokes the handler once and returns one
successful result. The gate also requires browser abort/remove/success events,
public Java file-removed/all-finished listeners, temporary-part cleanup, UIDL
confirmation, opaque push and clean shutdown. Automation remains test-only and
production code has no Upload branch. This bounded gate does not cover every
Servlet multipart edge case, higher-fan-out concurrency, repeated cancellation,
larger scale or process-replacement recovery. Same-session and cross-session
isolation are covered separately below.

Same-window concurrent raw Upload request isolation has a separate focused
real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-concurrent-upload
```

It starts two files through the standard Upload client, waits until the browser
and Java handler both observe overlap, then aborts one file while the sibling
continues to exact completion. The gate independently correlates both opaque
request IDs, requires the canceled request to have no end or successful
response, and requires the sibling's exact body end and successful response.
It also checks normal browser abort/removal/success events, public Java
file-removed/all-finished listeners, UIDL confirmation, opaque push and clean
shutdown. This covers a bounded two-file raw case; standard queue saturation
and two cancellations in a larger batch are covered by the separate gate below,
while process-replacement recovery remains open.

Same-window opaque request fan-out has a separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-request-fanout
```

It submits ten consecutive batches of six uniquely sized files through the
standard Upload client without changing its default `maxConcurrentUploads=3`.
Each batch requires three active browser XHRs and Java handlers plus queue
replenishment after terminal frames. Two requests per batch cancel: the first
two large batches trigger at `512 KiB`, while the eight compact churn batches
trigger at `192 KiB` and require at least `160 KiB` to reach Java, beyond the
initial `128 KiB` credit. The forty survivors complete an exact aggregate
`33,640,688` bytes with independent `2xx` responses. After the first batch,
test-only fault injection
uses the actual opaque IDs to require completed-entry reclamation and absorption
of four late response frame kinds for a canceled entry. It then verifies the
same ID is still isolated at about `55` seconds, is released after the
production `60`-second timeout and does not affect the following nine batches.
Public
file-removed/all-finished listeners, UIDL confirmation, push and clean shutdown
are also required. The route and automation are test-only; arbitrary-duration
or higher multi-window fan-out, automatic/resumable retry and process
replacement remain separate gaps.

Same-window concurrent multipart Upload isolation has a separate focused
real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-concurrent-multipart-upload
```

It starts two standard `FormData` requests and requires browser progress plus
opaque request overlap before canceling one file. Upstream Vaadin session
locking serializes multipart parameter parsing, so the gate observes one
request-scoped temporary directory at a time. The canceled request must not
reach the application handler; the sibling must parse exact bytes, spill to
disk and return one successful result. Public file-removed/all-finished
listeners, UIDL confirmation, temporary-root cleanup, opaque push and graceful
shutdown are also required. Bounded cross-session parser concurrency is covered
by the separate gate below; higher fan-out, repeated cancellation and
process-replacement recovery remain separate gaps.

Cross-session multipart parser concurrency has a separate focused real-window
gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-cross-session-multipart-upload
```

It opens the same public Upload route in two native windows with distinct
Vaadin sessions, advances both standard `FormData` streams beyond initial
transport credit and requires two simultaneous request-scoped directories and
spill files. It then cancels only the main-window request and correlates that
request using window, session, UI and request id, while the second window
completes exact parsing, public handler/listener state, UIDL, push and temporary
cleanup. This is bounded two-window evidence; higher fan-out, repeated
cancellation and process replacement remain separate gaps.

Window-scoped capability manifest enforcement has a separate focused
real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-permission
```

It invokes the same public `window.setTitle` command from `main` and an isolated
`permission-secondary` Flow session. The manifest allows the main command and
denies the secondary command. Two generated AppKit primary clicks must arrive as
trusted client Button events, execute one Java listener per window and produce
the corresponding UIDL responses. The main native title changes; the denied
secondary title stays unchanged, and Java receives the full permission payload
with its error. Full-detail trace inspection dynamically matches both
command/result correlation ids and exact window/session/UI scopes and validates
the allow/deny payloads. Only route navigation and opening the secondary window
are browser-programmatic setup; raw envelope counts are not gated.

Product-package capability policy has a focused build-time and real-window
gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-package-policy
```

The runner calls the real product macOS builder in `manifest-enforced` mode
against isolated staging inputs. Explicit `allow-all-dev` and
`trace-only-dry-run` manifests must each fail with exit code 2 and an exact
single-line diagnostic before staging starts; a pre-existing enforced manifest
and sentinel inside the staged app must retain byte identity. The runner then
packages the real framework application and replaces only its staged capability
manifest with malformed JSON, an unsupported schema, an unsupported mode and an
invalid window entry in turn. Each direct production-host probe must exit `2`
with its stable diagnostic and a strict two-event manifest/error trace, before
host `launch`/`ready`, sidecar, Java or window creation. It restores the
manifest and then executes the default-deny runner below, so only the positive control
launches a native host and real WKWebView while proving the packaged empty
enforced manifest at runtime. These focused negative probes do not cover the
rest of source-manifest validation or syntactically valid post-build replacement.

The broader builder contract is covered by `bash bin/cli-contract-smoke`. Its
real-builder probes require an explicit enforced manifest and a duplicate-key
manifest to stage as canonical JSON with one runtime-effective
`permissionMode`, while retaining `schemaVersion` and `windows` semantics. The
duplicate probe places `allow-all-dev` first and `manifest-enforced` last, then
requires one staged enforced key and no permissive token. Separate probes require
invalid JSON, an XML property list, a missing mode and an unknown mode to fail
before destructive staging. Development-mode probes require both explicit
permissive modes to remain accepted.

Missing source capability manifests retain the separate production-default
gate used by that combined runner:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-default-deny
```

The product CLI explicitly selects `allow-all-dev` for `dev`, `dev-vite` and
`dev-compat`, and `manifest-enforced` for `package`. The product macOS builder
also defaults to `manifest-enforced`; without a source manifest it stages
`{"schemaVersion":1,"permissionMode":"manifest-enforced","windows":[]}`.
For an explicit product manifest, the builder strictly parses JSON once into a
builder-private canonical snapshot that remains immutable for staging, validates
`permissionMode` from that snapshot and stages only the snapshot. It does not
validate one representation and later reread the mutable source for staging.
Invalid JSON, XML property lists, a missing or unsupported mode, and the two
development-only permissive modes are rejected before replacing an existing
staged app. Duplicate keys collapse to the parser's single runtime-effective
value. Development staging continues to accept explicit `allow-all-dev` and
`trace-only-dry-run` manifests.

This runner's omission switch belongs only to test-support. It withholds the
framework fixture manifest before calling the real product builder, then runs
the resulting standard production bundle and opaque Flow IPC runtime in a real
WKWebView. One generated AppKit primary click must arrive as a trusted Button
event, execute the Java listener and produce the denial UIDL response. Java must
receive the complete permission payload and the native title must remain
unchanged. The inspector requires the exact empty enforced-manifest trace before
the unique `window.setTitle` command/result pair and dynamically matches its
correlation id and full window/session/UI scope. Route navigation is the only
browser-programmatic setup stage; focused push is false, balanced quiet drain
and graceful sidecar shutdown are required.

This remains focused evidence, not a complete permission model or full product
parity. Product source JSON and permission-mode failures are rejected at build
time. Removing the manifest resource after staging remains startup-fatal;
parser-invalid staged JSON, schema, permission modes or window entries now emit
stable diagnostics and fail before `NSApplication`, sidecar, Java or window
creation. This does not prove signature-backed protection against a post-build
replacement that is still syntactically valid. Sandbox-selected read access has
the independent gate below; persistent bookmarks and cross-process grant recovery, broader Clipboard formats
and pasteboard contention, macOS authorization states, broader asynchronous
progress/cancellation/retry/resume
contracts, physical input hardware, touch and assistive technology remain
separate gaps.

Public FileDialog terminal outcomes have a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-dialog-outcomes
```

The first generated AppKit primary click reaches a public Flow listener, which
calls `Desktop.current().captureCapabilityContext()` before passing that
immutable facade to a background virtual thread. The thread has no current
Vaadin session (`backgroundSessionAbsent=true`), invokes
`DesktopFileDialog.openFileResult` through the captured main context and opens
one real attached, visible `NSOpenPanel`. Only after observing that unique panel
does the gate deliver a second generated trusted click. The concurrent public
call must return `ERROR` with `File dialog already in progress`, preserve its
allowed permission audit and create no second panel. Test-support then invokes
`NSOpenPanel.cancel(nil)` on the original panel. Its normal AppKit completion
must return public `CANCELED`, detach and hide the panel, and reach the DOM via a
main-scoped automatic opaque push and matching ack. This deterministic panel
action is not a physical user click on the Cancel button; the older automatic
panel-cancel hook is disabled.

Browser-programmatic setup opens a dedicated secondary whose manifest does not
grant `fileDialog.openFile`. A third generated trusted click in that isolated
opaque-UUID Flow session captures its exact secondary context and passes it to a
background thread with `backgroundSessionAbsent=true`. Its command must remain
secondary-scoped, return public `DENIED` with the full permission audit and
create no panel; the `UI.access` result must arrive through a secondary-scoped
automatic opaque push with a matching scope-exact ack. The runner invokes
`--require-flow-ipc-file-dialog-outcomes`, which requires exactly three unique
scoped command/result correlations, exact result payloads and ordering, one
panel observation, balanced quiet drain, no recovery and graceful shutdown.
The core contract smoke in `bash bin/flow-ipc-scope-smoke` separately covers
`openFileResult` precedence and legacy `openFile` `Optional<Path>`
compatibility, while the owner-close gate below proves public `INVALIDATED`.
This focused gate does not select a file or prove owner-scoped access; that has
the separate gate below. It also does not prove physical input, broader
asynchronous progress/cancellation/retry/resume behavior, broader authorization
states or full FileDialog/product parity.

Process-lifetime read-only selected-file grants have a focused real-window
gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant
```

Browser-programmatic setup navigates main to the public fixture route. Seven
generated AppKit primary clicks open an isolated secondary, invoke two
selections plus read/release/read-after-release in its exact scope, and close
that owner from main. Each selection presents a real attached, visible
`NSOpenPanel`. Panel selection is interactive: an operator or external native
UI driver uses Go to Folder to select the temporary fixture file. The overlay
waits for the exact canonical selected URL and emits `flowFileReadGrant.panelObserved`;
the operator or UI driver then confirms with the real Open control. The overlay
does not invoke `NSSavePanel.ok(_:)`. The trace labels this
`external-native-panel-selection`, without
relying on the panel's internal view hierarchy or injecting panel key events.
It is not evidence of physical Finder, keyboard or mouse input.

The runner's default deadline is 300 seconds; override
`VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS` if needed. Locate the
active fixture at
`${TMPDIR:-/tmp}/vatori-file-read-grant.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`,
then use Command-Shift-G, its absolute path and Return in each panel. After the
matching `panelObserved` event, press Open. Do not put the operator-only path
into host trace output.

Run this gate in an unlocked macOS desktop session. macOS activation is
cooperative: the host must be frontmost and key before the generated native-input
stages. Bring its title bar to the foreground if another
application retains focus; the gate waits and fails instead of dispatching an
event to an inactive window.

The public listener captures the secondary capability context before its
virtual thread observes `backgroundSessionAbsent=true`. The first selection
must return `SELECTED`, a bounded 19-byte read must return `DATA` with
`contentMatches=true`, explicit release must return `RELEASED`, and the next
host call through that Java handle must return `UNAVAILABLE`. The second
selected grant remains open until public main-scope close removes its exact
secondary context; the host must invalidate and close that grant automatically.

The runner invokes `--require-flow-ipc-file-read-grant`, which correlates the
five grant commands/results to the secondary's dynamic UUID scope and requires
two distinct lifecycle diagnostic ids, explicit-release and owner-close cleanup,
two panel observations with the external selection mode, seven trusted generated
pointer receipts, scope-exact
UIDL completion, balanced drain, no recovery and graceful shutdown. Full-detail
trace must contain no raw grant UUID, path, fixture basename or fixture bytes;
the read body retains only redacted byte-count metadata. The scanner also
rejects the fixture's Base64-encoded read body in diagnostic lines and alias
fields. The core scope smoke
separately covers typed-result precedence, malformed payloads, defensive byte
copies, captured-bridge retention and local double release.

The acceptance payload explicitly records `sandboxEnforced=false`. A successful
run closes bounded read, explicit release and owner-window-close cleanup in the
exact selecting scope, not the whole process-lifetime invalidation or App
Sandbox/Powerbox boundary. The gate correlates AppKit security-scope acquisition
with matching scope-stop metadata on both cleanup paths; the successful
acquisition branch has focused coverage, not sandbox-enforcement evidence.
An independent `bin/run-flow-file-read-grant-sandbox` entry uses the same public
typed listeners with an ad-hoc sandbox-signed temporary clone and an unchanged
app-local copy of the selected JDK. Its external runner creates the target and
an unselected sibling outside the app/container. Actual pre-panel `EPERM` for
both files, a genuine externally operated panel, content-checked typed read,
continued sibling denial, release and post-release `UNAVAILABLE` establish the
focused read-access boundary. The host checks exactly App Sandbox,
user-selected read-only and outbound network client entitlements; the latter
is a real network right needed for WebKit startup, not an IPC-only permission.
The default product host's signing policy is unchanged.

Run with `JAVA_HOME` (or `VAADIN_DESKTOP_ACCEPTANCE_JAVA_HOME`) pointing to a
complete local JDK. Choose the exact file printed by the runner via the panel's
Command-Shift-G control; wait for `flowFileReadGrantSandbox.panelObserved`
before Open. The independent gate is
`--require-flow-ipc-sandbox-file-read-grant`; its trace is
`.build/native-hosts/macos/host-sandbox-file-read-grant.out.log`. Four generated
AppKit clicks are native-host input evidence, not physical-input evidence.
The 300-second default deadline accepts a 1–3600 second override with
`VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN_TIMEOUT_SECONDS`, plus a 30-second external
watchdog margin. The package lock covers build, run, inspection and temporary
clone/fixture cleanup. Non-GUI runner smoke tests failure propagation, fresh
logs, external fixture provenance and preservation of the original JVM and
unrelated files. Persistent bookmarks, write access and cross-process grant
recovery remain outside this gate; release proves token/handle lifetime, not
immediate revocation of every OS-issued dynamic path extension.

The separate `bin/run-flow-file-write-grant-sandbox` runner uses the public
create-new-only write API and exactly App Sandbox, user-selected read-write and
network-client entitlements. The target initially does not exist; actual
exclusive creation must fail with `EPERM` before the externally operated Save
panel. Save the printed path only after `flowFileWriteGrantSandbox.panelObserved`;
after the write/release stages, select the same directory and
`canceled-write-grant.bin` in the second panel, wait for its observation, then
Cancel. Seven generated AppKit events drive typed selection, a 64 KiB block,
invalid offset, six-byte tail, release, unavailable write and cancellation.
An unselected sibling remains denied. The write-specific strict gate checks
trace/lifecycle/Flow correlation, and an external verifier checks all 65,542
binary bytes, the unchanged sibling and absent canceled target. Its trace is
`.build/native-hosts/macos/host-sandbox-file-write-grant.out.log`.
Existing-target refusal and partial I/O are covered by helper smoke, not a real
Sandbox replacement panel. Write owner teardown races, long/concurrent writes,
overwrite/transactions, persistence and retry/resume remain open. No default
Sandbox, distribution, physical input or AT claim is added.

`bin/run-flow-file-write-grant-owner-close-sandbox` reuses that isolated runner
with a separate manifest, driver and
`--require-flow-ipc-sandbox-file-write-grant-owner-close` gate with focused
real-window coverage. An unlocked interactive desktop is required. Public setup opens
the secondary `file-write-owner` and closes `main`; four generated AppKit stages
select and write 65,542 bytes, without release permission. Use only the first
Save panel and wait for `flowFileWriteGrantOwnerCloseSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-owner-close.out.log`.
The final generated click reaches a trusted Java DOM listener and publicly
closes the remaining owner. The gate requires owner retirement, invalidation,
successful close/security-scope stop, drained resources
and graceful exit; the external runner independently verifies preserved bytes.
The gate requires the same JVM to stay alive through native resource drain.
The focused proof is settled-write secondary owner teardown: a successful close
receipt after owner `windowClosed`, with both contexts absent at termination.
Real-window coverage observes close completed before an empty drain starts;
the alternative pending-one drain ordering has synthetic coverage only. The
gate matches each ordering to its pending count and treats the AppKit-wide
visible-window count as diagnostic. This is not in-flight/blocked I/O, ARC
deallocation timing, Quit/replacement, forced exit or durable commit evidence.
No final DOM patch is expected.

`bin/run-flow-file-write-grant-isolation-sandbox` uses the same isolated runner
with a two-window manifest and
`--require-flow-ipc-sandbox-file-write-grant-isolation`. Eight generated AppKit
clicks open the owner, select one new file, write a 64 KiB owner block, attempt
foreign write/release, append the owner's six-byte tail, release and close the
owner. Bring the app to the foreground if native activation waits. Use the one
Save panel titled `Flow file write grant owner isolation`; select the printed
target and wait for `flowFileWriteGrantIsolationSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-isolation.out.log`.
No Cancel or replacement panel follows.

Both windows allow write/release. The main window submits the same private
token at the valid next offset with different bytes, yet must receive uniform
`grant-unavailable` responses with no progress or owner diagnostic ID. The
owner's successful append/release and external 65,542-byte comparison prove
those attempts neither advance nor revoke the owner grant. Each listener
captures its own public raw bridge; sharing an owner-bound typed grant would
not exercise foreign scope. Three EPERM probes, exact entitlements, scoped
request/push/ACK and all eight native/Java receipts are required. This focused
real Sandbox proof is sequential API ownership isolation, not in-flight I/O,
OS-level window isolation, hostile Java-code isolation, persistence, physical
input or AT evidence. The driver performs final main close after balanced
transport; it is not another public-last-window-close gate.

`bin/run-flow-file-write-grant-sidecar-exit-sandbox` extends that isolated runner
with two external fixture directories and
`--require-flow-ipc-sandbox-file-write-grant-sidecar-exit`. This has focused
real-window Sandbox coverage through the standard production bundle and opaque
Flow IPC. Twelve generated AppKit stages reuse the public typed write route;
external Computer Use operates both real Save panels. The old owner writes its
65,542 bytes, leaves the grant active and reaches balanced transport before the
driver kills only the original JVM. Main and secondary windows, WebViews and
scopes survive one recovery; new Java view state is observed before another
selection and complete write, explicit release and post-release rejection.

Both real Save panels are titled `Flow file write grant new file`. Select the
printed `original` path first, then the distinct `recovered` path. Wait for
`flowFileWriteGrantSidecarExitSandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-sidecar-exit.out.log`
before each Save; no Cancel or Replace follows. Bring the app forward if active
window input is waiting. The shared runner's default deadline is 300 seconds.

The gate requires six actual EPERM probes, exact Sandbox policy, twelve native
command/results, old invalidation before recovery scheduling and successful
tracked close/scope stop before acceptance. Old asynchronous close may finish
after the new JVM is ready; this late-close ordering has synthetic coverage only.
All opaque counters, push sequences and command correlations are partitioned by
generation; reset IDs are not evidence of replay.
Both complete outputs and unchanged siblings are verified independently after
host exit. This targets settled cleanup and fresh authorization only, not old
token replay/restoration, in-flight writes, durable commit, physical/AT input or
OS-wide dynamic-extension revocation. Final main close is driver-owned.

`bin/run-flow-file-write-grant-replay-sandbox` adds recovered old-token write and
release probes to the isolated Sandbox runner. It has focused real-window
coverage through the standard production bundle and opaque Flow IPC.
Fourteen generated AppKit stages use
public raw bridges, settle the original 65,542-byte write, kill only that JVM,
retain both windows/WebViews/scopes, then submit the actual old token from the
new JVM before the second Save. Both panels use `Flow file write grant new file`;
choose the printed original/recovered paths and wait for
`flowFileWriteGrantReplaySandbox.panelObserved` in
`.build/native-hosts/macos/host-sandbox-file-write-grant-replay.out.log` before
confirming each. The default interactive deadline is 300 seconds.

The handoff is a bounded 0600 token/PID record under one new 0700 app-container
temporary directory. The driver checks actual container binding; Java privately
checks record protection, distinct producer/current PID and actual command-token
equality. No token, path or handoff contents enter DOM or traces, and no extra
entitlement or selected-directory authorization is used. Replayed write uses the
valid next offset 65,542 and six different bytes; write/release must both be
permission-allowed `grant-unavailable`, with no progress or release effect and
host `accessDenied` linked to the original diagnostic identity's `sidecar-exit`
retirement in generation two. The new token must differ and still complete its
own write/release/post-release rejection.

`--require-flow-ipc-sandbox-file-write-grant-replay` correlates the two epochs,
six EPERM probes, fourteen operations, private-handoff assertions, actual denied
commands and fresh positive control. The external verifier compares both whole
files and unchanged siblings, including the original file after replay. Java
consumes the matched record after fresh release; runner cleanup removes only that
fixed owned record and its empty unique parent, retaining unsafe or unexpected
material rather than recursively deleting the shared container. This is not
secure erasure, product persistence/restoration, a direct active-map generation
mismatch probe, in-flight writes, physical/AT input or durable commit.

`bin/run-flow-file-write-grant-host-shutdown-sandbox` adds normal AppKit Quit to
the isolated write runner. It has focused real-window Sandbox coverage through
the standard production bundle and opaque Flow IPC; external Computer Use
operates Save and Quit. Five generated AppKit stages reuse the public typed route:
main opens the secondary, which selects a new file, writes 64 KiB, rejects a
negative offset and writes its six-byte tail. Both windows stay open and the
grant is deliberately not released.

In the real Save panel `Flow file write grant new file`, choose the printed
target and wait for `flowFileWriteGrantHostShutdownSandbox.panelObserved`
before Save. Then wait for `hostShutdownPrepared` under that prefix in
`.build/native-hosts/macos/host-sandbox-file-write-grant-host-shutdown.out.log`
and invoke `Acceptance > Quit Vaadin Desktop Host` externally. The test-only
menu targets standard `NSApplication.terminate(_:)`; the driver does not invoke
it. Do not close either owner window. The interactive deadline is 300 seconds.

`--require-flow-ipc-sandbox-file-write-grant-host-shutdown` correlates exact
policy, three EPERM probes, five command/results, pointer/Java outcomes and
both scopes' opaque streams. It requires `host-shutdown` invalidation, one
pending tracked close, successful close/scope stop and zero-pending/zero-failure
drain before the original JVM's actual exit, graceful shutdown receipt and
`applicationWillTerminate` observation, in that order. Both original native
windows and WebViews/scopes survive until the observation; the exact original
JVM must be absent. Prepared traffic is balanced; later heartbeat/retry
traffic must form a valid causal prefix and cannot cross the termination fence
at invalidation. An outstanding UI response/ACK need not finish after that fence.
Truncated request/retry edge cases have synthetic inspector coverage only.
Only subsequent exact-scope AppKit close/dropped pairs
are permitted. The external verifier checks all output bytes and the unchanged
sibling after exit; the shared runner cleans its temporary clone and fixtures.
This does not prove physical/AT input, Dock Quit, production application menus,
in-flight/blocked writes, failure/timeout recovery or durable commit.

The host also binds grants to a sidecar generation. Unexpected-exit cleanup and
fresh recovered access have a separate real-window entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-sidecar-exit
```

This mode leaves the first selected grant active, waits for its visible Java
result and balanced transport, and SIGKILLs only the original sidecar PID. The
trace must show old-generation invalidation, successful handle closure and
matching security-scope stop. Both original windows, WKWebViews and scopes
survive one recovery; fresh Java state is observed before a new external
selection completes bounded read, release and post-release denial. This is a
new grant, not persistence or replay of the old one.

The default deadline is 600 seconds, with the same timeout override. Locate
the current fixture under
`${TMPDIR:-/tmp}/vatori-file-read-grant-exit.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`.
Both panel titles are `Flow file read grant explicit release`. Use the external
Command-Shift-G/Return/Open sequence above, confirming only after `panelObserved`
stages `first` and `recovered`. The driver remains passive for panel selection
and confirmation. Its seven generated AppKit pointer stages include the fresh
selection, and browser-programmatic navigation runs once per JVM. The package
lock covers build, run, inspector and temporary-fixture cleanup.

The runner invokes `--require-flow-ipc-file-read-grant-sidecar-exit`. This checks
the actual two-generation command/result and PID correlation, old/fresh grant
diagnostic ids, two window creations and four document loads, exact cleanup,
balanced scoped UIDL advances and graceful shutdown. Old and fresh acquisition
values are independent; a late asynchronous old close is allowed only before
acceptance. Reused JVM command ids cannot match another generation's result.
Full-trace privacy and the explicit physical-input/sandbox limitations remain.

Normal AppKit Quit with an active grant has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-host-shutdown
```

Select the one current fixture under
`${TMPDIR:-/tmp}/vatori-file-read-grant-quit.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`
in the real panel, using the external sequence above and waiting for
`panelObserved` stage `first` before confirming Open. After the visible Java
selection result and two stable balanced-transport polls,
`flowFileReadGrant.hostShutdownPrepared` must report complete. Then use
`Acceptance > Quit Vaadin Desktop Host` in the native menu. This test-only item
targets `NSApplication.terminate`; the driver installs it but never invokes it.
It is not a production application-menu feature or physical/Dock-origin proof.

The driver deadline is 600 seconds; it does not bound build, lock wait or
inspection. The package lock covers build/run/inspection/cleanup.
`--require-flow-ipc-file-read-grant-host-shutdown` requires
the grant and both original owner windows to remain active until Quit, then
invalidation, successful handle close/scope stop, a successful bounded native
resource drain, actual original JVM exit, graceful sidecar shutdown receipt and
`applicationWillTerminate`, in order. AppKit window teardown after that final
observation is permitted; an early owner close
is not. The external-wait timeout is failure, not alternative Quit evidence.
See [the detailed operator contract](../../docs/acceptance-tools.md) for drain
diagnostics, the cleanup-tracker smoke and negative gate coverage.

Live grant isolation between two windows has a separate entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-isolation
```

This runner uses an explicit test-support manifest that allows main to read and
release grants, so the foreign calls must be rejected by grant ownership, not
manifest policy. It does not mutate the regular test application's main policy.
The owner secondary selects one file through the real panel titled
`Flow file read grant owner isolation`. Locate the current file under
`${TMPDIR:-/tmp}/vatori-file-read-grant-isolation.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`;
use the external Command-Shift-G/Return/Open procedure, waiting for
`flowFileReadGrantIsolation.panelObserved` before Open.

Seven trusted generated AppKit clicks invoke Java listeners in both scopes.
The Java probe keeps the actual selected token private and captures a fresh
caller context for each raw public `NativeBridge` command. Main's read and
release must both be permission-allowed but unavailable, then the original
owner must read 19 matching bytes and release successfully. This does not use
the typed grant's owner-bound bridge as a substitute for a foreign caller.
The inspector checks both negative results and the owner positive control,
single-grant lifecycle, exact scopes/generation, privacy, continuity, balanced
UIDL state and graceful shutdown. The package lock and 600-second driver
deadline have the same respective scopes as above. Detailed gate and evidence
limits are in [acceptance tools](../../docs/acceptance-tools.md).

Typed file-grant range and EOF behavior has an independent entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-range
```

The runner creates a 65,573-byte nonuniform binary fixture named
`vatori-file-read-grant-range-fixture.bin`. Select that current generated file in
the single real panel titled `Flow file read grant bounded ranges`, using the
external Command-Shift-G/Return procedure. Its location is
`${TMPDIR:-/tmp}/vatori-file-read-grant-range.*/vatori-file-read-grant-range-*/vatori-file-read-grant-range-fixture.bin`.
Wait for
`flowFileReadGrantRange.panelObserved` before Open; the overlay never selects or
confirms the panel. Ten trusted generated AppKit clicks exercise selection,
three invalid ranges, a 64 KiB read, a 37-byte tail, EOF at and beyond file size,
a backward seek and release. The public typed facade remains owner-bound and
the same handle is used throughout; exact bytes are compared only inside Java.

The gate correlates native range/count/EOF receipts, allowed command/results,
visible Java state, opaque UI updates, stable main window/WebView/scope and
graceful shutdown. Browser-programmatic navigation and pointer positioning are
explicit setup, not physical input evidence. Package locking spans the full
run, the temporary directory is removed on exit, and the driver deadline is
600 seconds. See [acceptance tools](../../docs/acceptance-tools.md) for the exact
range table and coverage limits. This does not prove concurrent or changing-file
I/O, App Sandbox/Powerbox enforcement, physical input or Finder provenance.

Old-token replay after unexpected sidecar recovery has an independent entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-file-read-grant-replay
```

Select the current fixture under
`${TMPDIR:-/tmp}/vatori-file-read-grant-replay.*/vatori-file-read-grant-*/vatori-file-read-grant-fixture.txt`
in the panel titled `Flow file read grant before recovery`. Use the same external
native selection procedure and wait for `flowFileReadGrantReplay.panelObserved`
stage `old` before Open. The test app privately persists and verifies the token
in a bounded owner-only handoff file before the driver kills the original JVM.
The recovered JVM reuses the original owner scope with newly captured raw bridge
contexts: old-token read and release must both be allowed by policy but rejected
by the host's `sidecar-exit` retirement record. Only after both rejections, select
again in `Flow file read grant after recovery`, waiting for stage `fresh` before
Open. A privately verified distinct token must read 19 matching bytes and release.

The gate requires eight trusted generated clicks, two external panels, six
generation-scoped grant command/results, nine lifecycle receipts, original
native-window/WebView/scope continuity, balanced transport and graceful shutdown.
The private handoff is test-only, never a product grant persistence API or trace
payload. The runner's package lock covers the full build/run/inspection/cleanup
lifecycle; its generated owner-only directory is removed after exit even on
failure. The driver deadline defaults to 600 seconds. Detailed ordering, privacy
and negative coverage are in [acceptance tools](../../docs/acceptance-tools.md).

Sidecar retirement/replacement cleanup, broader cross-generation combinations
beyond replay of a known exit-revoked token, and an in-flight read/release race
are not directly injected by these gates. Fatal direct exit, forced process termination and
blocked I/O past the cleanup deadline are outside normal-Quit evidence.
Persistent bookmarks, write grants, grant recovery across
process replacement, Finder-origin drops, physical selection and broader
progress/cancel/retry/resume behavior remain open.

Public Clipboard terminal outcomes have a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-clipboard-outcomes
```

Three generated AppKit primary clicks reach public Flow listeners. The main
scope first reads a pasteboard with no string value and must return `NO_TEXT`.
It then writes an explicit empty string and reads it back as `WRITTEN` plus
`TEXT` with length zero. Browser-programmatic setup opens a dedicated secondary
whose manifest denies both Clipboard commands; its trusted click returns
`DENIED` for write and read without changing the pasteboard.

The test-support overlay keeps its pre-run pasteboard snapshot in memory. Each
bounded restore attempt first checks that the general pasteboard `changeCount`
still equals its latest test-owned count; an observed mismatch stops without
another mutation. Failed snapshot-restoration writes remain retryable while
that check holds; the app termination observer provides a final conditional
restore opportunity.
Clipboard contents never enter DOM status, acceptance payloads or full-detail
command/result traces. macOS activation is cooperative: the host must be
frontmost and key before these native-input stages, so bring its title bar to
the foreground if another application retains focus; the gate times out rather
than dispatching into an inactive window. The runner invokes
`--require-flow-ipc-clipboard-outcomes`, which
requires exactly five unique scoped correlations in operation order, exact
typed results and permission audits, three trusted pointer stages, scoped UIDL
completion, trace redaction, balanced quiet drain, no recovery and graceful
shutdown. The core contract smoke covers `INVALIDATED`, malformed/incomplete
`ERROR` payloads and legacy single delegation. AppKit has no compare-and-swap
pasteboard replacement, so an external writer can still race between the count
check and clear/write; this is best-effort protection for observed contention,
not an absolute no-overwrite guarantee. A `SIGKILL`, hard host crash or machine
termination cannot restore the intentionally memory-only snapshot. This gate
does not force a real AppKit write failure or prove OS authorization prompts,
non-text formats, adversarial contention, physical input or full
Clipboard/product parity.

Notification typed outcomes use a fresh, disposable app identity:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-notification-outcomes authorization-required
```

The default mode must leave OS authorization undetermined without displaying a
permission prompt. Generated AppKit input still activates the test window.
`os-denied` and `scheduled` modes require an authorized external operator to
answer the new identity's system prompt; no existing application's settings are
reset. The clone is removed after exact owned-process cleanup, but macOS can
retain that identity's permission record. The gate also requires blank-title
`ERROR`, secondary-manifest `DENIED`, scoped command/result and visible
`UI.access` updates. `SCHEDULED` is only OS acceptance, never a banner/delivery
claim. See [acceptance tools](../../docs/acceptance-tools.md) for the complete
contract, evidence limits and inspector entry.

`bash test-support/native-host-macos-acceptance/bin/run-flow-notification-delivery`
is a separate foreground-delivery probe. It needs authorization for one fresh
app's Allow choice and two public notifications (permission preparation, then an
already-authorized foreground probe). It never installs a delegate or changes
existing app permissions. The driver matches the public result ID's SHA-256 to
a real Notification Center record while observing an uninterrupted active/key
interval. The inspector flag is `--require-flow-ipc-notification-delivery`.
This proves retained delivery, not banner visibility; external operator banner
observations remain separate. Exact matched test notifications are removed
best-effort after a bounded visual-observation interval. See the acceptance-tools
contract for focus, privacy, cleanup and evidence limits.

Native capability isolation across sidecar generations has a focused real-window
gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-generation-recovery
```

An independent public Flow route invokes `fileDialog.openFile` with distinct
titles in two Java processes. Generated AppKit primary clicks must arrive as
trusted client Button events and open a real `NSOpenPanel` in each process. Once
the first panel is visible, the acceptance intentionally sends `SIGKILL` to the
sidecar without canceling the panel. The product must invalidate the executor,
clear its single active panel token before `cancel` and `orderOut`, trace and
drop the stale completion, and retain the same native window and WKWebView while
recovering a fresh Flow document.

The new JVM intentionally reuses `desktop-command-1` and the exact same
window/session/UI scope. Test-support cancels only the second panel; its canceled
result must reach the new Java listener and UIDL status before balanced quiet
drain and graceful shutdown. The inspector requires each command to follow the
corresponding `ready`, consistent with the initial, recovery and replacement
readiness guard, and proves that the expected-generation comparison and stdin
write prevent the old result from entering the replacement JVM. Only the two
route navigations are browser-programmatic, focused push is false, and generated
AppKit events do not claim physical-input coverage. Sandbox permission boundaries
beyond the independent selected-read gate, persistent bookmarks and cross-process grant recovery, broader OS
authorization and broader asynchronous capability progress/cancellation/retry/
resume behavior remain open.

Owner-window close during an in-flight native operation has a focused
real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-owner-window-close
```

The fixture opens an isolated secondary with a host-generated opaque UUID
session scope. A generated AppKit primary click reaches its public Flow listener,
which captures that exact secondary capability context before passing it to a
background virtual thread. The thread requires
`backgroundSessionAbsent=true` and opens a real attached `NSOpenPanel` through
the captured facade. A second generated AppKit click in the main window invokes
public secondary close, with the panel cancellation hook disabled. Product code
must invalidate the operation, clear its panel token,
`cancel` and `orderOut` the panel, drop the stale completion, return the exact
correlated `owner-window-closed` typed failure so public `openFileResult`
returns `INVALIDATED`, then send the secondary `windowClosed` and release its
native context. A third generated click proves the retained main Flow listener
and UIDL path remain healthy in the same JVM, window and WebView.

The runner invokes
`--require-flow-ipc-capability-owner-window-close`. The inspector requires the
exact command/panel/invalidation/drop/result/window-close order, dynamic
window/session/UI correlation, detached and hidden panel state, released
secondary context, balanced quiet drain, no recovery and graceful shutdown.
The Java scope smoke (`bash bin/flow-ipc-scope-smoke`) separately covers
per-window handler ordering, immediate close tombstoning and synchronous UI
detach plus Vaadin `SessionDestroy` before the native capability
session-to-scope mapping is released. It also proves that an old captured
secondary context cannot emit or retarget a command after teardown or after the
same public id reopens with a new opaque UUID. Route navigation and secondary
opening are browser-programmatic. Generated trusted AppKit events prove the host
input path, not physical hardware, touch or assistive technology. This is a
focused file-dialog lifecycle slice, not full capability or product parity.

Same-public-id captured-context reincarnation has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-capability-context-reopen
```

Incarnations A, B and C use the same public `capability-reopen-secondary`
window/UI id, but the gate requires pairwise-distinct host-generated opaque UUID
sessions and native window/WebView identities. A trusted generated AppKit click
in A captures its exact context and a background virtual thread with no current
Vaadin session sets the A title. A trusted click in retained `main` closes A;
only after its host context is gone does browser-programmatic setup open B.

A third trusted click in B invokes A-old/B-fresh/A-old. A fourth trusted click
in retained `main` closes B; only after release does setup open C. A fifth
trusted click in C invokes A-old/B-old/C-fresh/A-old/B-old. Every retired call
must return a local typed `owner-window-closed` failure without stale or
retargeted title commands, while the fresh B and C calls set their exact titles
and each UI update receives a scope-matching push acknowledgement. The runner
invokes `--require-flow-ipc-capability-context-reopen`; the inspector also
requires exact A→B→C create/close ordering, balanced per-scope drain,
same-sidecar/main continuity, no recovery and graceful shutdown. Route
navigation and all three opens are browser-programmatic setup. This is a
focused two-cycle contract, not long-running or unbounded reincarnation, higher
fan-out, recovery/replacement or full product parity.

Production-host retired-scope wiring has a complementary focused real-window
gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-retired-scope-production-host
```

The runner packages the standard Vaadin production bundle, copies that bundle
and its capability manifest into an isolated app, and launches the production
Swift host composition with real AppKit windows and WKWebViews. The isolated
app stages a test-support fake sidecar as its Java JAR. That sidecar opens and
closes same-public-id incarnations A and B, then
injects their exact old commands plus late HTTP response and push envelopes
through the normal `main.swift` handler. Both commands must receive correlated
typed `owner-window-closed` results, all four late HTTP/push transport probes
must be dropped, and the host must retain both tombstones at once.

Incarnation C must receive a third host-generated UUID, complete a live title
command and browser request/response-credit round trip, then close before a
graceful main-window and sidecar shutdown. The runner invokes
`--require-flow-ipc-retired-scope-production-host`, which also rejects recovery,
scope reuse, misordered retirement or C traffic classified as retired.
The credit round trip proves scope-exact WKWebView credit plus host chunk/end
ingress; it does not prove complete browser body consumption or a Flow request.

This gate proves production host composition and `main.swift` wiring for two
process-lifetime tombstones. The fake sidecar does not run a Flow UI or Java
listener and the command-driven closes are not physical user close input. It
also does not prove unbounded churn or recovery/replacement; the preceding real
Flow A→B→C gate supplies the complementary public API, listener, UIDL and
visible-state evidence.

Main-window close with a retained secondary across sidecar recovery has a
focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-main-window-close-recovery
```

Browser-programmatic setup navigates and opens a secondary on the same non-root
route. A generated AppKit primary click in that secondary invokes public
`window.close("main")`. The gate requires main context release while preserving
the secondary native window, WKWebView and host-generated opaque UUID scope.
Test-support then kills generation one. After generation two reaches
lifecycle-ready, product code must keep main absent, replay its exact normal
`windowClosed` envelope and reload the retained secondary from its original
route-specific entry URL.

A second generated AppKit primary click reaches the recovered secondary's fresh
Java listener. Its no-current-session background main probe must return the
typed `owner-window-closed` failure locally without emitting a generation-two
main command; the same listener then completes one secondary-scoped
`window.setTitle` and visible UIDL result. The runner invokes
`--require-flow-ipc-main-window-close-recovery`, which requires two distinct
sidecar processes, one lifecycle-ready main-scope replay, no main
reconstruction, exact route/scope retention, the two trusted pointer stages,
balanced drain and graceful shutdown. Navigation and window opening remain
browser-programmatic, generated AppKit input is not physical-input evidence,
and this bounded two-window topology is not full parity.

The lazy Grid query contract has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-lazy-grid-query
```

The isolated route first uses the official Grid `MULTI` selection column over
an 80-item exact-count `CallbackDataProvider` with stable ids and
`PRESERVE_ALL`. Its first five generated AppKit primary clicks reach three
official visible row checkboxes and two Flow refresh buttons as trusted DOM
events. The checkbox clicks produce
exactly three client-originated Java `MultiSelectionEvent` updates with opaque
UIDL responses: add `L-061`, add `L-060`, then remove `L-061`. The intervening
button click replaces selected `L-060` with a fresh, unequal object carrying the
same id, invokes `refreshItem`, patches its state cell to `refreshed`, and keeps
it selected without another selection event. Owner sorting and `platform`
filtering preserve both ids and the refreshed `L-060`. With both selected rows
outside the rendered viewport, the second button removes matching `L-003` and
explicitly calls `refreshAll`; filtered/total cardinality changes from 26/80 to
25/79, fetch and count queries advance, one generic `DataChangeEvent`, no
`DataRefreshEvent` and no selection event are observed, and both ids remain
selected while `L-003` disappears from the rendered Grid. Selected-row return,
filter reset and distant-row recycling rehydrate the selected state before the
final checkbox click leaves only refreshed `L-060`. Navigation, viewport
movement, sorting, filtering and refresh-button positioning remain
browser-programmatic setup.

The route then uses a separate Grid configured through the public fetch-only
`setItems` overload with page size 5, unknown-size estimate 40 and
increase 40. Browser-programmatic scrolling must grow the estimate without an
exact count query, fetch across the initial estimate and discover `U-067` from
a terminal short fetch. Java `ItemCountChangeEvent` evidence and the client
Grid size must settle from estimated growth to exact 67. A sixth generated
AppKit primary click selects the official `U-067` row checkbox and completes a
client-originated Java selection listener plus opaque UIDL/selected-DOM round
trip. After returning the old tail off viewport, a seventh generated AppKit
primary click invokes a Flow Button listener that appends through `U-093` and
calls public `GridLazyDataView.refreshAll()`. The exact 67 tail must reset to
estimate 40, grow through at least 120, cross the previous tail and finish with
a positive short fetch at exact 93. Selected `U-067` must rehydrate without an
additional Java selection event; the refresh emits one generic data-change
event and no item-refresh event. An eighth generated AppKit primary click,
after returning to the top, removes unselected `U-073..U-093` and calls the same
public refresh API. An independent shrink epoch resets exact 93 to estimate
40, grows to 80 and converges to exact 72 from a positive short fetch ending at
`U-072`. The original `U-067` object remains selected and rehydrates row,
checkbox and ARIA state without another selection event; all removed tail ids
are absent from rendered cells. Fetch/count-event deltas must match the new
epoch counters, and the inspector cross-checks frozen initial/append evidence,
unique status values, history and short-fetch bounds. Unknown-size scrolling is also
browser-programmatic setup. The gate uses the standard production bundle and
opaque request/response transport in a real WKWebView; it does not itself prove
push, physical input hardware, touch, assistive technology, selected-item
removal or arbitrary repeated reset sequences, reorder or sort/filter-key mutation, broader
pagination, larger scale, fresh-object identity across unknown-size mutation or
other Grid capabilities.

Flow DnD has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-dnd
```

It opens an isolated route using the standard Flow DragSource/DropTarget APIs
and production bundle, posts a generated macOS primary-pointer sequence through
the AppKit event path, and requires trusted browser drag lifecycle events in the
real WKWebView. It verifies exactly-once client-originated Flow listener
round-trips, server-side drag data, MOVE drop semantics and opaque request/push
ownership. The focused runner directly executes the staged bundle executable so
the test-only HID events inherit the runner's input authorization; this does not
change the production launch path. Physical mouse hardware and external Finder
file drops remain separate acceptance gaps.

The heterogeneous overlay stack has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-overlay-stack
```

It opens a modal Dialog, anchored modeless Popover and auto-added modal
ConfirmDialog through browser-programmatic setup in the standard production
bundle. Generated AppKit window events then close the ConfirmDialog and Dialog
with Escape and close the Popover with a primary-pointer outside click. The gate
requires trusted DOM receipt with exact stage counts, Flow Java lifecycle
listener round-trips and UIDL patches, default ConfirmDialog focus and
accessibility, parent retention and layered focus restoration through opaque
request/push transport. Physical keyboard or pointing-device hardware, touch
and assistive technology remain separate gaps.

Owner-bound input popups have a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-input-popup
```

It uses ComboBox, Select and DatePicker as probes through the standard
production bundle and opaque Flow request/push transport. The gate verifies
owner, selected-item and calendar focus, accessibility state, keyboard
navigation, selection, close, value retention, Java listener round-trips and
UIDL patches. ComboBox actions and DatePicker opening use scripted browser
events. Select opening, movement, Enter selection, reopening and Escape close,
plus DatePicker calendar focus, ArrowRight movement, Enter selection and Escape
close, use AppKit keyboard events delivered to the WKWebView. Both owner-bound
overlays are then reopened and dismissed with AppKit primary-pointer events.
The gate requires trusted document-background receipt under Vaadin's modal
pointer shield, value retention, restored owner focus, client-originated Java
opened-change listeners and UIDL patches, while the underlying Flow button must
remain unclicked. Physical keyboard or mouse hardware, remaining picker
variants, locale/validation breadth, touch dismissal and assistive-technology
interoperability remain separate gaps.

MultiSelectComboBox token and collection-value behavior has a separate focused
real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-multi-select-combo-box
```

The independent route uses public Flow APIs through the standard production
bundle, a real WKWebView and opaque requests with UIDL responses. Its token field
begins with `ava`, a clear-enabled field begins with `kai,sol`, and a third field
begins with `ava`, uses an explicit public `ItemFilter` and keeps its filter
after selection. A fourth field begins with `ava`, uses an explicit public
`ItemFilter`, and retains the default `keepFilter=false` behavior. Five trusted
generated AppKit primary clicks, 15 trusted generated AppKit keydowns and two
resulting trusted input events drive the route. The first three clicks and first
seven keys retain the verified token addition, two-stage Escape/Backspace,
nested-shadow remove-button and atomic clear-button paths.

The fourth click opens and focuses the keep-filter field. Trusted `k` input
reaches a latched server-filter request/response checkpoint that renders only
`kai` while retaining `ava`; ArrowDown and Enter focus and select `kai`. Two
trusted Escapes then clear its active option and close/reset the popup while
preserving `ava,kai`. The fifth click opens and focuses the default-clear field.
Its independent `k` query and ArrowDown/Enter selection add `kai`, clear the
input/filter and rehydrate all options from the empty server filter. It remains
focused/open with selected chips `ava,kai` and the owning-listbox ARIA map
`ava:true,kai:true,sol:false`.

The token field requires exactly three client-originated Java
`MultiSelectionEvent`s with three matching UIDL responses, the clear field
requires one event with `removed=kai,sol` and one matching response, and each
filtering field requires one event with `added=kai` plus independent query and
selection UIDL-response checkpoints. The first two fields finish empty.
Navigation and pointer positioning, scripted token-field focus, and
empty-input/caret-at-start preparation before Backspace are
browser-programmatic; filtering-field focus comes from the trusted fourth and
fifth clicks. The runner waits for balanced opaque exchanges and a quiet
interval before graceful sidecar exit. Focused push is false. Lazy-data
filtering has the separate bounded gate below; unknown-size providers,
mutation-driven or manual refresh, broader pagination/filter and close/reopen
combinations, selected-items-on-top combinations, physical input hardware,
touch and assistive technology remain separate gaps, so this gate is not full
component parity.

Lazy MultiSelectComboBox paging, filtering and stable identity have a separate
focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-lazy-multi-select-combo-box
```

The independent route uses public Flow APIs through the standard production
bundle, a real WKWebView and opaque requests with UIDL responses. Its
page-size-10 `CallbackDataProvider(fetch, count, id)` exposes 80 stable logical
reviewer ids while returning fresh, unequal Java objects on every fetch, and the
initial server value is `R-060`. One trusted generated AppKit primary click
opens and focuses the owner. Nine trusted generated AppKit keydowns follow
`z`, ArrowDown, Enter, Escape, Escape, `z`, Escape, `x`, Escape, with one
trusted input event for each of the three text keys. Route navigation, owner
positioning and ordinary scrolling of the scoped owning listbox are the only
browser-programmatic stages.

The distant scroll requires a nonzero-offset fetch and a fresh `R-060` option
with `role=option` and `aria-selected=true`. The `z` query renders only fresh
`R-061` while retaining `R-060`; ArrowDown focuses it and Enter produces one
client-originated Java selection event plus one semantic selection UIDL
checkpoint, with logical `R-060` retained and `R-061` added. Two Escapes clear
option focus, close and reset the filtered popup. Typing the same `z` again must
advance the server fetch count beyond the captured first-query baseline, render
a fresh selected `R-061`, preserve both selected items and chips, and produce no
additional Java selection event. Escape closes and resets that repeated query.
Typing `x` then reopens the field and re-queries a fresh selected `R-060` option
while retaining both selected items and chips; the final Escape closes and
resets the field.

Far-page, first-`z`, selection, repeated-`z` and `x` progress are semantic
request/UIDL-response checkpoints, not raw-envelope contracts. The runner waits
for balanced opaque exchanges through a quiet interval before requesting
graceful sidecar shutdown, and focused push is false. Unknown-size providers,
mutation-driven or manual refresh, broader pagination/filter and close/reopen
combinations, physical input hardware, touch and assistive technology remain
separate gaps, so this gate is not full component parity.

Localized time input has a separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-localized-input-popup
```

It uses TimePicker as a probe with `Locale.US`, a bounded 30-minute list and an
initial 13:00 value. AppKit ArrowDown events open the official listbox and move
candidate focus through 12:00 PM, 12:30 PM, 1:00 PM and 1:30 PM before Enter
selection. The gate requires Java value-listener and UIDL patch evidence,
localized 1:30 PM display, close and owner-focus restoration through the
standard production bundle and opaque request/push transport. Other locales,
validation, physical hardware, touch and assistive technology remain separate
gaps.

Localized decimal input and an in-place locale change have a focused gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-localized-number
```

An isolated public-API `BigDecimalField` starts at `42.50` with `Locale.GERMANY`
and explicit `ON_CHANGE`. Generated AppKit pointer and keyboard events replace
the input with `1234,567890123456789`, then Tab commits the exact Java decimal
with scale 15. A trusted Flow Button click calls `setLocale(Locale.US)` without
setting the value: the DOM uses a dot while the Java value, scale and listener
count stay unchanged. Another native edit and Tab commit to `9876.125` must
produce the second client-originated Java event with scale 3 and the correct old
value. The gate checks ordered native dispatches, trusted DOM key/input/click
receipts, both actual Cmd+A selections, full Java event history, frozen stage
snapshots and three semantic
opaque request/response checkpoints, then drains through a quiet interval and
shuts down gracefully.

Command-modified editing keys use application-level AppKit dispatch and the
production Edit menu's responder chain. A menu-consumed Cmd+A may omit its DOM
keydown, but must select the complete unchanged input before replacement;
typed-character, input and Tab receipts cannot be omitted.

Route navigation and positioning remain browser-programmatic; no browser script
assigns the field value or dispatches its input events. Valid main-scoped
entry-page push/ack traffic is allowed without counting as this slice's proof.
This does not prove
focused-route push, physical input hardware, IME, assistive technology, RTL,
grouping/currency formatting, other locales, empty/malformed input or Binder
validation. All probe logic stays in test-apps/test-support.

Bounded picker validation has a separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-picker-validation
```

It uses DateTimePicker as a composite probe. Programmatic browser input drives
the component's own commit state machine through minimum, maximum, incomplete,
required and malformed default-validation branches, with valid recoveries. The
same field's public Binder chain then proves default-validator precedence,
custom-only rejection, bean retention and a distinct valid write. The gate
requires client-originated Java value listeners, malformed-input Java
validation-status evidence, Binder status, configured messages, parent/child
invalid and error-presentation UIDL patches, final recovery and closed overlays
in a real WKWebView through the standard production bundle and opaque
request/push transport. Hidden raw custom error-message retention follows the
upstream `HasValidation` contract. Remaining picker variants, other locales,
physical hardware, touch and assistive technology remain separate gaps.

Owner-bound menu popups have a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-menu-popup
```

It uses MenuBar, ContextMenu and GridContextMenu as probes of the standard
Flow/browser path through the production bundle and opaque request/push
transport. The gate verifies owner/expanded ownership, accessibility, initial
focus, keyboard movement, selection, Escape close, focus restoration and Java
listener round-trips. The MenuBar sequence additionally verifies one nested
child submenu with ownership/ARIA, Right/Left traversal, leaf selection,
layered Escape and parent/owner focus restoration. A responsive sequence
additionally verifies default collapse-from-end into the overflow menu,
collapsed-item ownership/ARIA, Java selection, Escape focus restoration and
expanded-width restoration of all original top-level items. It also verifies
one collapsed parent with child ownership/ARIA, Right/Left traversal with
overflow-root retention and collapsed-parent focus restoration, child
reopening, Java leaf selection and layered Escape focus restoration.
The ContextMenu sequence additionally verifies one nested child submenu with
root/child ownership and ARIA, Right/Left traversal, Java leaf selection,
layered Escape and parent/target focus restoration. The Grid sequence targets a
distant virtualized body row and verifies trusted pointer receipt plus Java
row/column context. It also verifies one child submenu on that row with
ownership/ARIA, Right/Left traversal with root retention, parent-focus and
row/column-context retention, child reopening, a row-bound Java leaf listener,
full-stack selection close and layered Escape focus restoration. A compact
Grid policy path additionally verifies trusted header and trailing blank-body
itemless contexts, Java `setDynamicContentHandler` rejection without opening,
and row-specific dynamic text before Java selection, close and focus restoration.
MenuBar keys and ContextMenu/GridContextMenu secondary-clicks are AppKit window
events. The host must be frontmost and key before native input. Physical
hardware, touch/long-press, deeper MenuBar chains, reverse-collapse, deeper
chains inside overflow, deeper GridContextMenu child chains, and assistive
technology remain separate gaps.

Structured JavaScript RPC has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-structured-rpc
```

It verifies nested bean/list `@ClientCallable` input, nested list/map Promise
output, a Java bean passed to `executeJs`, generic-list result conversion and
the core JSON codec branch matrix: numeric-string and enum decoders, primitive
arrays, method-level generic collections, primitive-null defaults, nullable
references, varargs, enum/array/null Promise output, `Class`-based
scalar/enum/array/null result conversion and nested generic-map
`TypeReference` conversion. It also verifies pre-send cancellation with no
browser side effect, already-sent cancellation refusal with normal Promise
completion, and disabled
`ONLY_WHEN_ENABLED`/`ALWAYS` ClientCallable enforcement. It also verifies
invisible-owner deferral until visibility returns and detached-owner pending
invocation cleanup across reattachment. CompletableFuture coverage includes
typed generic-list completion, JavaScriptException failure and cancellation,
session-lock blocking protection, multiple pre-send subscribers and post-send
subscription rejection while the original subscriber completes through the
standard Flow client and opaque request/push transport. The gate also creates
server-side modality with `UI.addModal`, verifies that a normal ClientCallable
is ignored with its browser Promise left pending, verifies `@AllowInert`
completion, removes the modal component and verifies normal invocation
recovery.

The Vite development path has a focused real-window HMR gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-vite-development
```

The rebuild-driven Java process replacement path has its own real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-java-reload-development
```

It mutates and restores `JavaReloadProbe.java`, then requires a rebuilt value
after planned sidecar replacement while preserving native window and WebView
identity.

Effective Maven reactor inputs have a separate real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-reactor-reload-development
```

It activates a test-only profile module, mutates and restores a normal dependent
POM plus the profile module's custom Java and resource roots, then requires
three reactor rebuilds and planned sidecar replacements while preserving native
window/WebView identity and recovering Flow request/push ownership each time.

Failed replacement rollback has a separate real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-java-rollback-development
```

It mutates and restores the test application's startup mode, builds a candidate
that exits before `ready`, and requires stable-JAR rollback plus recovered Flow
request/push behavior in the retained window and WebView.

Fatal sidecar startup and native process status propagation have a focused
failure gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-startup-failure
```

The runner mutates only the generated app bundle, restores the staged JAR on
exit and requires failure before Java `ready` without creating a window.

The live-process initial-ready deadline has a separate fixture and gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-ready-timeout cooperative
bash test-support/native-host-macos-acceptance/bin/run-host-ready-timeout blocked
```

`Fixtures/NeverReadySidecar.java` never sends `ready`. Its cooperative hook
returns; its blocked hook forces production TERM/KILL escalation. Each run uses
a private clone and marker state. The runner requires product timeout, actual
child exit, fatal host status `1`, no window, then independently verifies both
PIDs absent before cleanup. Cleanup cannot rescue PASS. This is a pre-ready
lifecycle fault probe, not a WKWebView/Flow UI test; see the acceptance-tools
contract for the evidence schema and limitations.

Initial WKWebView navigation failure after Java `ready` has an acceptance-only
watchdog and a deterministic real-window negative gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-host-navigation-timeout
```

The observer arms before navigation completes whenever auto-shutdown is enabled.
The focused fixture stops the original WKWebView's initial load and requires
the scoped `flowIpcInitialNavigationTimeout`, failed acceptance exit status and
normal AppKit/sidecar shutdown. A successful negative gate is not a successful
Flow bootstrap or Sandbox/Powerbox proof. It does not modify the production
host's timeout or crash-recovery policy. The state-machine smoke is included
in `bin/verify-smoke`.

The public API closing the sole remaining window has a separate lifecycle gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-last-window-close
```

Two generated AppKit clicks establish a Java/UIDL preparation state and invoke
the public Java title/close APIs. The inspector then requires actual final
window removal, successful empty resource drain and graceful shutdown. The
success driver never closes the host window itself; a final browser response
is not required after that window is destroyed. This does not prove physical
input, active-grant cleanup, focused push or blocked filesystem I/O. The
production cleanup scheduler has separate default/modal-loop smoke coverage.

Unexpected-exit supervision is covered by the normal sidecar-restart acceptance
and a fatal exhaustion entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-sidecar-recovery-exhaustion
```

The recovery acceptance kills three real Java child processes and verifies rapid
attempt accounting plus stable-run reset while retaining the native window and
WKWebView. The exhaustion runner stages a two-attempt manifest budget and
requires the third rapid failure to terminate the host with status `1`.

`bin/run-recovery-termination` isolates a production-bundle three-JVM recovery:
the test application's second generation blocks in a SIGTERM shutdown hook,
while the production host escalates and waits for actual exit before starting
the third. Two generated AppKit inputs prove independent Java/UIDL/push states
in the retained native window. Its test-only marker state and fault injection
never enter framework artifacts. The runner removes only its owned clone/state
after confirming process cleanup; no OS permission action is needed. See the
acceptance-tools contract for limits and the exact trace gate.

`bin/run-host-shutdown-termination cooperative|blocked` uses one private app
and original JVM, a least-privilege close/title manifest, two generated AppKit
inputs and a consumed preparation push. The public final close must reach
actual JVM exit before AppKit termination; blocked hooks require production
TERM/KILL. Independent marker/PID checks run before cleanup and a cleanup signal
cannot rescue PASS. It does not repeat OS permission or notification tests.
This is empty-drain process evidence, not a replacement for live file-grant
cleanup, physical input, fatal-startup or planned-replacement validation.

`bin/run-sidecar-write-backpressure` prepares the same public Flow fixture,
then test-support suspends only its exact JVM and confirms the stopped process
state. Sixteen ordinary browser GETs with bounded padding headers exercise the
production stdin writer. The gate requires real EAGAIN followed by a live
main-loop heartbeat, product-menu Quit, actual TERM/KILL exit and independent
host/JVM absence before cleanup. It uses only the startup marker: a suspended
JVM cannot produce launcher-returned or hook-entry evidence. This is whole-JVM
fault injection with generated AppKit preparation/menu input and programmatic
browser load, not physical input or application cleanup evidence. No OS
permission or notification action is used; see the acceptance-tools contract.

`Tests/flow-ipc-opaque-http-bridge-smoke.mjs` and
`Tests/flow-ipc-push-bridge-smoke.mjs` verify the production browser bridge
scripts without adding code to an application production bundle.
