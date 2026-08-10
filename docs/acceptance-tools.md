# Acceptance And Smoke Tools

These tools verify repository and runtime contracts. They are not public
framework APIs.

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

Lazy `DataProvider` query semantics have a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-lazy-grid-query
```

The gate requires distant viewport loading, recycled-row selection, return to
the initial viewport, a client-originated sortable-column request, server-side
query ordering, configurable owner filtering with matching fetch/count results,
filter reset, opaque request/push evidence and graceful sidecar shutdown.

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

A heterogeneous three-level overlay stack has a focused real-WKWebView entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-overlay-stack
```

The gate opens a modal Dialog, an anchored modeless Popover and an auto-added
modal ConfirmDialog through browser events and Flow Java listeners. It requires
the standard ConfirmDialog initial focus and accessibility semantics,
top-of-stack Escape close, parent-layer retention, focus restoration to the
Popover trigger, Popover outside-click close with the Dialog retained, final
Dialog Escape close, layered focus restoration, opaque request/response and
push evidence, and graceful shutdown. Keyboard and pointer events are
dispatched programmatically inside the real WKWebView; physical AppKit input
remains outside this contract.

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

Bounded picker validation has a separate focused entry:

```bash
bash test-support/native-host-macos-acceptance/bin/run-flow-picker-validation
```

The gate uses DateTimePicker as a composite-picker probe with a 12:00-14:00
range. Programmatic browser input in the real WKWebView drives the component's
own commit state machine through minimum, maximum, incomplete, required and
malformed default-validation branches, with a valid recovery between branches
and after the final malformed value. The gate requires client-originated Java
value listeners where the value changes, a Java validation-status listener for
the malformed branch, configured default-validator messages, and UIDL patches
for parent/child invalid state, visible errors, recovered values and error
clearing. Child overlays must be closed at completion. The path uses the
standard production bundle and opaque request/response/push transport. This
focused contract does not cover custom-validator composition, remaining picker
variants, additional locales, physical hardware, touch or assistive-technology
interoperability.

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
bash test-support/native-host-macos-acceptance/bin/run-host-ready-timeout
```

It packages the app with a short product-level initial-ready deadline, stages a
test-support Java sidecar that remains alive without sending `ready`, and
requires timeout diagnostics, nonzero termination, no window creation and
fixture restoration.

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
  --require-flow-ipc-flow-dnd \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-flow-overlay-stack \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-flow-input-popup \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-localized-input-popup \
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
  .build/native-hosts/macos/host-ready-timeout.out.log
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
- Lazy Grid query: a focused standard-production-bundle path for distant
  viewport loading, selection, return, sortable-column RPC, server query
  ordering, configurable filter refresh/reset and opaque request/push ownership.
- Flow DnD: isolated-route standard DragSource/DropTarget connector
  configuration, generated macOS primary-pointer routing with trusted browser
  drag lifecycle events, exactly-once client-originated drag-start/drop/drag-end
  Flow listener round-trips, server-side drag data, MOVE drop semantics and
  opaque request/response/push ownership in a real WKWebView. Physical hardware
  input and external-file behavior remain outside this contract.
- Flow overlay stack: isolated-route modal Dialog, anchored modeless Popover
  and auto-added modal ConfirmDialog ownership, default ConfirmDialog focus,
  accessibility, top-layer Escape, parent retention, outside-click close and
  layered focus restoration through Flow Java listeners and opaque
  request/response/push ownership in a real WKWebView. The focused gate records
  programmatic browser input; physical AppKit input remains outside this
  contract.
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
- Flow localized input popup: isolated-route TimePicker `Locale.US` formatting,
  bounded step options, initial owner-input focus, AppKit ArrowDown candidate
  traversal, Enter selection, Java value-listener execution, UIDL patch, close
  and owner-focus restoration through opaque request/response/push ownership in
  a real WKWebView. Other locales, validation, physical hardware, touch and
  assistive-technology behavior remain outside this focused contract.
- Flow picker validation: isolated-route DateTimePicker default-validation
  state machine covering minimum, maximum, incomplete, required and malformed
  values plus recoveries. The real WKWebView gate requires the component's own
  commit events, client-originated Java value listeners, Java validation-status
  evidence for malformed input, configured validator messages, parent/child
  invalid and visible-error UIDL patches, final value/error recovery and closed
  overlays through opaque request/response/push ownership. Custom-validator
  composition, remaining picker variants, additional locales, physical
  hardware, touch and assistive-technology behavior remain outside this
  focused contract.
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
  `ready`, no window creation and nonzero host termination.
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
