# Flow Transport Adapter

The desktop adapter preserves Vaadin's browser/server protocol and replaces only
the transport boundary.

## Request Path

```text
opaque method + URL + headers + body
  -> DesktopVaadinRequest / DesktopVaadinResponse
  -> VaadinService.handleRequest()
  -> Vaadin request-handler chain
  -> opaque status + repeated headers + body
```

`DesktopFlowOpaqueHttpSession` owns session-scoped request adaptation. It does
not branch on static, bootstrap, UIDL, upload or download request type before
calling Vaadin.

## Desktop Request Handlers

The adapter registers two focused handlers:

- `DesktopClasspathResourceRequestHandler` serves safe GET/HEAD classpath
  resources when no servlet static server exists.
- `DesktopStreamResourceRequestHandler` handles only registered
  `StreamResource` downloads because Vaadin's upstream download handler requires
  `VaadinServletRequest` for `ServletContext` access.

The desktop stream handler returns `false` for Upload, StreamReceiver and other
dynamic resource types. Vaadin's standard `StreamRequestHandler` remains
responsible for those paths.

`DesktopFlowVaadinService` also supplies a classloader-backed `ResourceProvider`
for Flow component and theme resources.

## Streaming And Push

Opaque request/response bodies use bounded chunks, credits and cancellation.
The Java worker sees streaming servlet-compatible input/output adapters without
the native host interpreting request semantics.

Push uses a desktop `PushConnectionFactory` and Flow's normal UIDL writer. The
browser bridge presents the narrow Atmosphere API expected by the standard Flow
client and transports push messages opaquely. Java retains each unacknowledged
delivery in process memory. The browser acknowledges it only after the official
Flow client callback and reports Flow's last-seen server sync id; Java retries an
unacknowledged message with the same transport sequence. This retry contract is
limited to the current sidecar generation and Flow session/UI. A replacement
process creates a new session and state tree, so old unacknowledged UIDL is
discarded instead of being replayed into the fresh document.

## Verification

```bash
bash bin/opaque-http-adapter-smoke
bash bin/flow-ipc-scope-smoke
node test-support/native-host-macos-acceptance/Tests/flow-ipc-opaque-http-bridge-smoke.mjs
node test-support/native-host-macos-acceptance/Tests/flow-ipc-push-bridge-smoke.mjs
bash test-support/native-host-macos-acceptance/bin/run-flow-push-delivery
bash test-support/native-host-macos-acceptance/bin/run-flow-push-recovery
bash test-support/native-host-macos-acceptance/bin/run-flow-large-download-resume
bash test-support/native-host-macos-acceptance/bin/run-flow-large-upload-retry
bash test-support/native-host-macos-acceptance/bin/run-flow-large-multipart-upload-retry
bash test-support/native-host-macos-acceptance/bin/run-flow-concurrent-upload
bash test-support/native-host-macos-acceptance/bin/run-flow-request-fanout
bash test-support/native-host-macos-acceptance/bin/run-flow-concurrent-multipart-upload
bash test-support/native-host-macos-acceptance/bin/run-flow-cross-session-multipart-upload
```

The adapter smoke verifies handler ordering, classpath GET/HEAD, repeated
`Set-Cookie`, StreamResource content/headers and VaadinSession context. Scope and
browser smokes cover window isolation, backpressure, cancellation and push
acknowledgement/retry/reconnect/resync. One focused macOS gate drops the first
delivery of a final `UI.access` message in test-support and proves same-sequence
redelivery and Flow last-seen acknowledgement without later browser activity.
The recovery gate separately proves same-scope disconnect/reconnect, advanced
last-seen metadata, gap-triggered Java resync and post-resync `UI.access`
delivery in a real WKWebView.

The large-download gate uses a public Flow `DownloadHandler` and correlates
response-reader cancellation with Java stream cancellation, then correlates an
open-ended `Range` request with HTTP `206`, a complete large response and exact
remaining-byte reconstruction. The host and adapter remain request-semantic
agnostic; Range handling belongs to the application.

The large-upload gate uses a public raw Flow `UploadHandler`, correlates normal
XHR progress and abort with request cancellation and Java input-stream failure,
then correlates re-selection with a distinct complete request, exact body bytes
and successful response. Vaadin removes the aborted file, so retry is explicit
re-selection by the application/test driver. The host and adapter remain
request-semantic agnostic and contain no Upload branch.

The large-multipart-upload gate uses the standard Upload `FormData` request and
keeps its unknown content length and framing opaque to the bridge and host. It
correlates cancellation before request end with request-level isolation, no
terminal success and zero accepted partial files, then correlates a distinct
re-selection request with complete framed bytes, exact parsed file content and
metadata, one handler invocation and one successful result. Multipart form
fields are Servlet request parameters, so Vaadin session lookup can trigger
parsing before request-handler dispatch; an aborted parse therefore need not
reach `UploadHandler.responseHandled`, and the adapter does not synthesize that
component callback. Public file-removed/all-finished listeners and UIDL
confirmation close the component lifecycle without adding Upload semantics to
production transport code.

The generic Servlet adapter uses Apache Commons FileUpload with a configurable
memory threshold, request-scoped disk spill, file/request/part-count and header
limits, query/form parameter aggregation, Servlet `Part.write()`/`delete()` and
cleanup after success, malformed input or cancellation. Adapter smoke covers
binary boundary-like content, quoted filenames, repeated fields and temporary
part lifecycle; the real gate observes spill and cleanup for the 4 MiB retry.

The concurrent-upload gate starts two standard raw Upload XHRs in one
WKWebView, requires both opaque request IDs to open before either terminal
frame, cancels one request and independently correlates the sibling's exact
request end and successful response. Browser and Java maximum-active counts,
public file-removed/all-finished listeners and the UIDL confirmation prove that
the cancellation did not contaminate the sibling. All file generation,
component lookup and automation remain in test-support and the test app; the
bridge, host and Java adapter still route both requests generically.

The request-fan-out gate extends that generic proof across ten consecutive
six-request batches. The standard production `UploadManager` supplies the
request source and retains its default three-request concurrency limit. Two
requests per batch are canceled. The first two large batches trigger at
`512 KiB`; eight compact churn batches trigger at `192 KiB` and require at
least `160 KiB` to reach Java beyond the initial `128 KiB` credit. Forty total
survivors complete an exact `33,640,688` bytes. Trace correlation requires a
three-active queue in each batch, every batch terminal before the next batch
opens, no end or successful response for any cancellation, and exact end plus
successful response for each survivor. After the first batch, test-only fault
injection also proves that a completed request ID has left the browser registry
while a canceled ID still absorbs late response metadata, chunk, end and error
frames.
The focused real-window gate then waits on the production timer: the canceled
ID still absorbs metadata at about `55` seconds, is reported as unknown after
`60` seconds, and the following nine six-request batches complete normally.
This proves time-bounded browser tombstone retention without exposing a test
clock or Upload-specific branch in production code.
Java smoke separately proves completion/cancellation reclamation permits safe
request-ID reuse and rejects a late request chunk. Upload names, file data and
automation remain entirely in test code.

The concurrent-multipart gate starts two standard `FormData` Upload XHRs in one
WKWebView and requires both opaque requests to open before either terminates.
The browser and transport overlap, while upstream `VaadinService` holds the
session lock during multipart parameter lookup and therefore serializes
same-session parsing. The gate observes one request-scoped temporary directory
at a time, cancels one request before application-handler dispatch, then proves
the sibling's exact parsed part, disk spill, successful public result,
file-removed/all-finished listeners, UIDL confirmation and complete temporary
storage cleanup. No production layer branches on Upload or multipart semantics.

The cross-session multipart gate opens the same route in two native windows,
each with an independent window, session and UI scope. Both standard
`FormData` streams progress beyond the initial credit while two parser
directories and spill files coexist. It then cancels only the main-window
request and correlates that terminal frame using the full scope, while the
secondary request parses exact bytes, returns a successful public result and
completes Java listener, UIDL, push and temporary-storage cleanup. This proves
that same-session serialization does not become a global parser lock and that
request-id reuse across WebViews remains scope-isolated.

## Open Boundaries

- complete upstream DevTools server integration and broader development
  resource graphs beyond the focused Vite/HMR path;
- broader large-transfer behavior beyond the bounded DownloadHandler
  cancellation/open-ended Range-resume and raw UploadHandler
  progress/cancellation/re-selection-retry, large multipart
  parsing/cancellation/re-selection-retry, two-file same-window raw and
  multipart isolation, ten consecutive six-request/three-active same-window raw
  queue batches with registry turnover and bounded two-window cross-session
  parser concurrency, especially broader Servlet multipart edge cases,
  arbitrary-duration or higher multi-window fan-out, automatic/resumable
  retry, repeated or multi-range requests, larger scale and
  process-external state;
- complete Flow component and browser behavior parity.
