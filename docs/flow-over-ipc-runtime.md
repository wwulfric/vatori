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
reported as failure.

## Window Scope

`Desktop.current().windows().open(DesktopWindow)` creates a new native window for
a registered relative route. Each WKWebView has immutable `windowId`, `sessionId`
and `uiId`, with an isolated Vaadin session and UI. `close(windowId)` closes the
target and `windowClosed` releases Java state. Compatibility HTTP stays
single-window.

## Sidecar Lifecycle

The sidecar has one stdin reader and one synchronized stdout writer. `shutdown`,
final window close and stdin EOF stop the loop. Pending native commands complete
with an error when the bridge closes.

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

Bounded process recovery retains native windows but recreates Vaadin session and
UI state. Spring state is also recreated when the optional adapter is present.
Unexpected exits consume a consecutive-failure budget with exponential
backoff. Recovery `ready` starts a manifest-defined stable-run deadline; the
budget resets only when the same process remains ready for the complete
duration. A failure before that deadline consumes the next attempt, while a
failure after reset starts again at attempt one.
`dev-vite` additionally uses a planned replacement path after a successful Java
rebuild. Planned replacement does not consume the unexpected-exit restart
budget. Replacement is a CLI/host transaction: the host reports candidate
readiness or failure, the CLI owns stable-JAR restoration, and the host reports
rollback readiness before development continues. A failed rollback is fatal
rather than consuming the unexpected-exit restart budget.
If the initial sidecar exits before valid `ready`, the host exits nonzero before
presenting a window. A manifest-defined initial `ready` deadline also terminates
a sidecar that remains alive without completing the handshake. Exhausted
recovery after startup is fatal. CLI development commands preserve that status
and the host logs.

## Current Verified Surface

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
top-layer Escape, parent retention, outside-click ownership and layered focus
restoration, multiple
non-eager routes whose application-owned route and router-layout
JavaScript/CSS dependencies are generated and loaded as production chunks
including a transitive dynamic import, bidirectional lazy Grid virtualization
with a distant viewport, recycled-row selection, return to the initial viewport,
client-originated sortable-column changes, server-side query ordering,
configurable owner filtering, matching count/fetch refresh and filter reset,
backend hierarchical TreeGrid root/child count and fetch queries, two-level
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
mouse hardware or external Finder file-drop coverage. The focused input-popup
gate distinguishes scripted ComboBox and
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
TimePicker gate verifies the official `Locale.US` 12-hour display, a bounded
30-minute step list, initial owner-input focus with no focused list candidate,
successive AppKit ArrowDown candidate focus, Enter selection, Java
value-listener execution, the returned UIDL patch, close and owner-focus
restoration. A separate focused DateTimePicker validation gate drives the
component's own commit state machine through minimum, maximum, incomplete,
required and malformed default-validation branches, with valid recovery between
branches and after malformed input. It requires client-originated Java value
listeners, malformed-input validation-status evidence, configured validator
messages, parent/child invalid and visible-error UIDL patches, final value/error
recovery and closed overlays. These gates do not claim physical keyboard or
mouse hardware, remaining picker variants, locale breadth beyond that `en-US`
case, custom-validator composition, touch dismissal or assistive-technology
interoperability. The
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
restored runtime. This is a bounded product surface, not complete Vaadin parity.

## Compatibility Boundary

Servlet filters, servlet-backed security and normal WebSocket infrastructure
require the Spring Boot adapter:

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

## Open Work

- JVM in-place class hot replacement and the complete upstream Servlet-based
  Vaadin DevTools server lifecycle;
- source roots introduced only by lifecycle plugins after Maven's
  projects-read phase, beyond the verified effective-model source/resource
  roots and profile-activated reactor graph;
- broader generated and lazy frontend graphs beyond the verified non-eager
  route/layout JavaScript, CSS and transitive dynamic-import graph;
- broader Grid features and scale beyond the verified bidirectional lazy
  viewport/selection, server-side sorting/configurable-filter query path and
  backend hierarchical TreeGrid expansion/selection/refresh path, plus
  ComponentRenderer lifecycle, LitRenderer property/function/refresh semantics
  and buffered Editor/Binder save, cancel and validation;
- physical mouse-hardware Flow DragSource/DropTarget sessions and the separate
  external Finder file-drop boundary beyond the verified generated native
  pointer, trusted browser lifecycle, server-side data and MOVE-effect path;
- broader owner-bound input popup behavior beyond the verified ComboBox, Select,
  DatePicker, focused `en-US` TimePicker and bounded DateTimePicker default
  validation-state probes, including remaining picker variants, broader locale,
  custom-validator composition, touch dismissal, physical keyboard or
  pointing-device input and assistive-technology interoperability;
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
- embedded JREs, installers, updates and non-macOS hosts.
