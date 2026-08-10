# Roadmap

This document records stable engineering direction, not per-run evidence or goal
history. Current runtime boundaries live in `docs/flow-over-ipc-runtime.md` and
acceptance contracts in `docs/acceptance-tools.md`.

## Framework Foundation

Status: implemented.

- Public Java desktop application and capability API.
- Core IPC envelope and one blocking sidecar launcher model.
- Developer examples and separate validation applications.
- Central smoke and repository hygiene guards.

## Native Host Contract

Status: implemented on macOS, open cross-platform.

- One platform-selected AppKit/WKWebView host.
- Structured application/window `ready` handshake.
- Stable `VFIPC ` framing and numeric envelope version for both UI transports.
- Bundle launch manifest and repository-independent sidecar staging.
- Explicit production and acceptance build boundaries.
- Transport-shaped trace redaction and compaction that keeps opaque bodies and
  acceptance-only DOM or fixture semantics out of the production host.
- Bounded sidecar replacement for Flow IPC.
- CLI-observed native host exit status and fatal pre-`ready` failure
  propagation.
- Manifest-defined initial sidecar `ready` deadline with nonzero timeout
  termination.
- Consecutive-failure sidecar supervision with bounded exponential backoff,
  manifest-defined stable-run reset and fatal exhaustion.

Next work:

- define the same contract for future Windows/Linux hosts;
- add production packaging, signing and update delivery.

## Flow Over IPC

Status: active product direction with broad validation, not full parity.

- Keep the standard Vaadin production webapp and opaque request bridge as the
  only browser runtime.
- Keep all browser requests inside Vaadin's request-handler chain.
- Preserve pure `DesktopFlowApplication + DesktopLauncher` as the default and
  Spring Boot as an optional adapter.
- Provide a CLI-managed standard Vaadin Vite frontend with HMR while preserving
  opaque Flow UI transport.
- Rebuild changed application Java sources in `dev-vite`, atomically stage the
  resulting JAR and deliberately replace the sidecar while retaining native
  windows.
- Consume the selected Maven invocation's effective reactor model, including
  active profiles and inherited/interpolated compile source and resource roots,
  and refresh the Vaadin Vite workspace when build configuration changes.
- Preserve the last ready JAR until replacement `ready` and automatically
  restore it when a built candidate cannot start, exits early or misses the
  ready deadline.
- Verify ClientCallable Promise resolve/server-reject and
  PendingJavaScriptResult browser Promise result/error callbacks through the
  standard Flow client and opaque request transport.
- Verify nested bean/list ClientCallable input and nested list/map output,
  PendingJavaScriptResult bean argument and generic-list conversion, and
  numeric-string/enum decoder branches, primitive arrays, method-level generic
  collections, primitive-null and nullable-reference behavior, varargs,
  enum/array/null Promise output, `Class`-based scalar/enum/array/null result
  conversion and nested generic-map `TypeReference` conversion, plus
  cancellation before and after browser send, plus disabled ClientCallable
  update-mode enforcement, invisible-owner deferral until visibility returns
  and detached-owner cleanup across reattachment, typed generic-list
  CompletableFuture completion, JavaScriptException failure/cancellation,
  session-lock blocking protection, multiple pre-send subscribers and
  post-send subscription rejection, inert default ClientCallable suppression
  with a pending browser Promise, `@AllowInert` completion and post-modal
  recovery, in a real WKWebView.
- Verify focused Flow DragSource/DropTarget connector configuration,
  generated macOS primary-pointer routing with trusted
  mousedown/dragstart/dragenter/dragover/drop/dragend events, exactly-once
  client-originated listener round-trips, server-side drag data and MOVE drop
  semantics on an isolated route in a real WKWebView through the standard
  production bundle and opaque request/push transport.
- Verify owner-bound input popup semantics with ComboBox, Select and DatePicker
  as probes on an isolated route: combobox/listbox/dialog accessibility
  ownership, owner/selected-item/calendar focus, keyboard navigation, selection,
  close, value retention, Java listener round-trips and UIDL patches through the
  standard production bundle and opaque request/push transport, with scripted
  browser setup, AppKit-to-WKWebView key events and trusted AppKit primary
  outside clicks distinguished in trace evidence. Outside dismissal must retain
  values, restore owner focus, reach Java lifecycle listeners and suppress
  click-through to an underlying Flow button.
- Verify a focused localized owner-bound input slice with TimePicker as the
  probe: `Locale.US` 12-hour formatting, bounded 30-minute options, initial
  owner-input focus, AppKit ArrowDown candidate traversal, Enter selection, Java
  value-listener execution, UIDL patch, close and focus restoration through the
  standard production bundle and opaque request/push transport.
- Verify a focused picker-validation slice with DateTimePicker as the composite
  probe: the component's commit state machine covers minimum, maximum,
  incomplete, required and malformed default-validation branches plus
  recoveries; Java value and validation-status listeners, configured messages,
  parent/child invalid and visible-error UIDL patches, final error clearing and
  closed overlays travel through the standard production bundle and opaque
  request/push transport.
- Verify owner-bound menu popup semantics with MenuBar, ContextMenu and
  GridContextMenu as probes on an isolated route: owner/expanded ownership,
  accessibility, initial focus, keyboard movement and selection, trusted
  secondary-click opening, Java listener round-trips, Escape close and focus
  restoration, plus a nested MenuBar child submenu with ownership/ARIA,
  Right/Left traversal, leaf selection and layered Escape, one nested
  ContextMenu child submenu with ownership/ARIA, Right/Left traversal, Java
  leaf selection and layered Escape, and default end-collapsing responsive
  overflow with collapsed-item ownership/ARIA, Java selection, Escape focus
  restoration and expanded-width top-level item restoration, including one
  collapsed parent with child ownership/ARIA, Right/Left traversal with
  overflow-root retention and collapsed-parent focus restoration, child
  reopening, Java leaf selection and layered Escape. The Grid probe
  additionally covers a distant virtualized body row, row/column context
  association and recycled DOM, plus one child submenu with ownership/ARIA,
  Right/Left traversal with root retention, parent-focus and row/column-context
  retention, child reopening, a row-bound Java leaf listener, full-stack
  selection close and layered Escape through the standard production bundle
  and opaque request/push transport. A compact Grid
  policy probe also verifies trusted header and trailing blank-body itemless
  contexts, Java `setDynamicContentHandler` rejection without opening, and
  row-specific dynamic content before Java selection, close and focus
  restoration. These remain acceptance probes of generic runtime behavior,
  not component-specific product integration.
- Verify generic opaque push recovery in a real WKWebView: an initial
  `UI.access` update, same-scope disconnect/reconnect with advanced Flow
  last-seen metadata, browser sequence-gap detection, Java-generated resync
  UIDL, strictly increasing transport sequence and a later normal push updating
  the existing DOM.
- Verify generic opaque push delivery acknowledgement in a real WKWebView:
  test-support drops the first final `UI.access` delivery, Java retries the same
  opaque payload with the same transport sequence and Flow sync id without later
  browser activity, and the official Flow client applies it before acknowledging
  the matching sequence and last-seen sync id.
- Verify the generic push boundary across unexpected sidecar replacement in a
  real WKWebView: test-support leaves an old-session `UI.access` delivery
  unacknowledged before process exit, the retained WKWebView reloads a fresh Flow
  client/session, new push starts at sequence one, and the old UIDL marker is not
  replayed into the new state tree.
- Verify generic in-flight request behavior across unexpected sidecar
  replacement in a real WKWebView: pending browser requests fail
  deterministically, the disconnected old document cannot send later request or
  push envelopes to the replacement process, retained native window/WebView
  identity survives, and a document reload establishes a fresh Flow client and
  session whose distinct request completes exactly with Java listener and UIDL
  confirmation. This does not replay or resume the interrupted request.
- Verify generic large-response cancellation and resume in a real WKWebView:
  a public Flow `DownloadHandler` streams more than 4 MiB, browser reader
  cancellation reaches the Java output stream, an open-ended `Range` request
  returns correlated HTTP `206`/`Content-Range`, and the remaining bytes are
  reconstructed exactly before a Java listener and UIDL patch confirm state.
- Verify generic large-request progress, cancellation and re-selection retry in
  a real WKWebView: a public raw Flow `UploadHandler` receives more than 4 MiB,
  normal XHR progress reaches a bounded abort point, cancellation reaches the
  Java input stream, Vaadin's abort/removal lifecycle completes, and re-selecting
  an equivalent file creates a distinct request whose bytes and metadata arrive
  exactly before a public Java listener and UIDL patch confirm state.
- Verify generic multipart parsing cancellation and re-selection retry in a real
  WKWebView: standard Upload `FormData` framing streams through an unknown-length
  opaque request, cancellation is isolated before application handler dispatch
  with no terminal request success or accepted partial file, while a distinct
  retry parses the exact file bytes and metadata, spills above the configured
  threshold and cleans temporary parts before public handler/result,
  file-removed/all-finished listener and UIDL confirmation.
- Verify generic same-window concurrent request isolation in a real WKWebView:
  two standard raw Upload requests open before either terminates, both reach
  browser and Java overlap, cancellation of one request reaches only its Java
  input stream, and the sibling completes with exact bytes, a successful
  response, public Java listeners and a UIDL patch.
- Verify generic same-window request fan-out and bounded queue replenishment in
  a real WKWebView: ten consecutive batches of six uniquely sized raw requests
  traverse Vaadin's default three-active Upload queue, twenty requests cancel
  after tiered browser/Java thresholds beyond initial transport credit, forty
  survivors complete an exact `33,640,688` bytes, and trace ordering
  independently correlates every open, chunk and cancel or end/response. A
  test-only boundary probe proves completed registry entries are reclaimed and
  canceled entries isolate late response frames before batch two. The same
  canceled ID remains isolated at about `55` seconds, is released after the
  production `60`-second tombstone timeout, and subsequent traffic remains
  healthy, without any component-specific production branch.
- Verify generic same-window concurrent multipart request isolation in a real
  WKWebView: two standard `FormData` Upload requests and opaque streams overlap,
  one is canceled before application-handler dispatch, and upstream Vaadin
  session locking serializes multipart parameter parsing to one request-scoped
  temporary directory at a time. The sibling then reaches the public handler
  with exact bytes, configured disk spill and cleanup before Java listeners and
  a UIDL patch confirm state.
- Verify generic cross-session multipart parser concurrency in two real
  WKWebViews: distinct window/session/UI scopes advance both standard
  `FormData` streams beyond initial request credit while two request-scoped
  directories and spill files coexist. Cancellation of the main-window request
  is correlated by full scope and does not affect the secondary request, which
  completes with exact bytes, a successful public result, Java listeners, UIDL,
  push and temporary-part cleanup.
- Continue component, resource, event, push and multi-window parity one
  verifiable semantic slice at a time.

Next work:

- JVM in-place class hot replacement and complete upstream Vaadin DevTools
  server integration without introducing Servlet transport into pure Flow;
- source roots added by lifecycle plugins after Maven's projects-read phase,
  beyond the verified effective-model roots and profile-activated reactor
  graph;
- broader generated and lazy frontend graphs beyond the verified non-eager
  route/layout JavaScript, CSS and transitive dynamic-import graph;
- broader Grid features and scale beyond verified bidirectional lazy
  viewport/selection, server-side sorting/configurable filtering and backend
  hierarchical TreeGrid expansion/selection/refresh, ComponentRenderer
  lifecycle, LitRenderer property/function/refresh semantics and buffered
  Editor/Binder save, cancel and validation;
- broader large-transfer behavior beyond the bounded DownloadHandler
  cancellation/open-ended Range-resume and raw UploadHandler
  progress/cancellation/re-selection-retry, large multipart
  parsing/cancellation/re-selection-retry, two-file same-window raw and
  multipart isolation, ten consecutive six-request/three-active same-window raw
  queue batches with registry turnover and bounded two-window cross-session
  parser concurrency, including broader Servlet multipart edge cases,
  arbitrary-duration or higher multi-window fan-out, automatic or resumable
  continuation across sidecar replacement, repeated or multi-range requests,
  larger scale and process-external transfer state. A bounded interrupted
  request followed by a distinct fresh-session retry is verified, without
  carrying request state across processes;
- physical mouse-hardware Flow DragSource/DropTarget sessions and an explicit
  external Finder file-drop boundary beyond the focused generated native-input
  gate;
- broader owner-bound input popup acceptance beyond the verified ComboBox,
  Select, DatePicker, focused `en-US` TimePicker and bounded DateTimePicker
  default validation-state probes, including remaining picker variants, broader
  locale, custom-validator composition, touch dismissal, physical keyboard or
  pointing-device input and assistive-technology interoperability;
- broader owner-bound menu popup acceptance beyond the verified MenuBar,
  ContextMenu and GridContextMenu body-row/nested-child/dynamic-policy probes,
  including deeper MenuBar submenu chains, reverse-collapse and deeper child
  chains inside overflow, deeper GridContextMenu child chains, touch/long-press
  input, physical hardware and assistive-technology interoperability;
- deeper anchored and broader heterogeneous multi-overlay-stack keyboard,
  outside-click, focus and accessibility parity beyond the verified single and
  nested modal Dialog, single and two-level nested anchored modeless Popover,
  one modal Dialog/modeless Popover stack and one three-level modal
  Dialog/modeless Popover/modal ConfirmDialog stack.

## Native Capabilities

Status: real command/result path, production permission model incomplete.

- Keep clipboard, dialogs, notification, window and file transfer behind public
  desktop APIs and manifest authorization.
- Add a complete application-menu model only when native click events and Java
  callback correlation are defined.
- Complete sandbox-safe file grants, progress, cancellation, retry and resume.
- Keep capability correlation transport-independent and auditable without
  committing generated logs.

## Distribution

Status: future work.

- Add an embedded JRE to the existing application/native-host staging layout.
- Measure startup, memory and package size.
- Add installers, signing/notarization, update strategy and release automation.

Do not create versioned goal docs or placeholder platform modules to represent
these items. Product state should be proven by code, focused living docs and
executable acceptance.
