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

The lazy Grid query contract has a focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-lazy-grid-query
```

It drives a distant viewport, recycled-row selection, return to the initial
viewport, a real sortable-column click, configurable owner filtering and filter
reset through the standard Flow client and opaque request/push transport.

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
ConfirmDialog through the standard production bundle. The gate verifies Flow
Java listener round-trips, default ConfirmDialog focus and accessibility,
top-layer Escape, parent retention, Popover outside-click close and layered
focus restoration through opaque request/push transport. Browser keyboard and
pointer events are programmatic; physical AppKit input remains a separate gap.

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

Bounded picker validation has a separate focused real-window gate:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-picker-validation
```

It uses DateTimePicker as a composite probe. Programmatic browser input drives
the component's own commit state machine through minimum, maximum, incomplete,
required and malformed default-validation branches, with valid recoveries. The
gate requires client-originated Java value listeners, malformed-input Java
validation-status evidence, configured messages, parent/child invalid and
visible-error UIDL patches, final value/error recovery and closed overlays in a
real WKWebView through the standard production bundle and opaque request/push
transport. Custom-validator composition, remaining picker variants, other
locales, physical hardware, touch and assistive technology remain separate
gaps.

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
bash test-support/native-host-macos-acceptance/bin/run-host-ready-timeout
```

`Fixtures/NeverReadySidecar.java` stays alive on the sidecar channel without
sending `ready`; the runner requires the product timeout, nonzero exit and no
window creation, then restores the generated app bundle.

Unexpected-exit supervision is covered by the normal sidecar-restart acceptance
and a fatal exhaustion entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-sidecar-recovery-exhaustion
```

The recovery acceptance kills three real Java child processes and verifies rapid
attempt accounting plus stable-run reset while retaining the native window and
WKWebView. The exhaustion runner stages a two-attempt manifest budget and
requires the third rapid failure to terminate the host with status `1`.

`Tests/flow-ipc-opaque-http-bridge-smoke.mjs` and
`Tests/flow-ipc-push-bridge-smoke.mjs` verify the production browser bridge
scripts without adding code to an application production bundle.
