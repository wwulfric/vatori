# Flow over IPC Framework Validation App

This repository-only app uses ordinary Flow APIs across input, selection, lazy
data virtualization with server-side query sorting and configurable filtering,
backend hierarchical data with nested expansion, selection and child refresh,
component-renderer lifecycle, LitRenderer property/function/refresh semantics
and buffered Grid editor save, cancel and validation, bidirectional Flow
JavaScript RPC with ClientCallable and PendingJavaScriptResult,
generated lazy frontend dependency graphs, overlay, keyboard, focus,
accessibility, resource, upload/download, authentication and desktop capability
surfaces, including modal Dialog stacks and an anchored modeless Popover
independently, in a two-level anchored stack and in a mixed modal/modeless
overlay stack. Its isolated owner-bound input route also carries localized
popup and bounded composite-picker default-validation state-machine probes.
An isolated public `DownloadHandler` route provides deterministic large-response
cancellation and open-ended Range-resume acceptance without adding
download-specific behavior to framework production code.
An isolated public raw `UploadHandler` route likewise provides deterministic
large-request progress, cancellation and re-selection-retry acceptance without
adding Upload-specific behavior to framework production code.
Another isolated raw route provides an in-flight opaque request probe for
unexpected sidecar replacement. It exposes deterministic bytes and a public
Java confirmation listener so test-support can prove that the old request fails
and a distinct fresh-session request completes exactly. Process termination,
old-document inspection and recovery orchestration remain outside the app and
no Upload-specific behavior enters framework production code.
An isolated public multipart `UploadHandler` route verifies unknown-length
`FormData` cancellation isolation, exact re-selection parsing, configured disk
spill and temporary-part cleanup. Its deterministic content, IDs and state are
acceptance-only; it is not a component adapter or a claim of exhaustive
Servlet-container multipart parity.
Another isolated public raw `UploadHandler` route starts two standard Upload
requests concurrently, cancels one and verifies exact sibling completion plus
public file-removed/all-finished listener state. It remains an acceptance probe,
not a component adapter.
A separate raw request fan-out route submits ten consecutive batches of six
uniquely sized files through Vaadin's unchanged default three-active Upload
queue. It cancels two requests per batch after transport credit has advanced
and verifies exact completion of the other forty plus ten all-finished cycles.
The first two batches retain large-body coverage; the remaining eight use
compact churn bodies while still requiring Java consumption beyond the initial
credit window. The route is a generic request registry, backpressure and
cancellation probe; its file names, patterns and counters are test-only and do
not define framework behavior.
An isolated multipart counterpart starts two standard `FormData` requests,
verifies browser/transport overlap with upstream session-locked parsing,
cancels one before handler dispatch and checks exact sibling completion, disk
spill and temporary-root cleanup. Its storage monitor, IDs and state remain
acceptance-only.
A separate route opens itself in a second native window and starts one standard
multipart request per Vaadin session. It proves bounded cross-session parser
concurrency, full-scope cancellation isolation, exact survivor parsing and
cleanup without adding window, Upload or multipart branches to framework
production code.

Deterministic state belongs here rather than in framework runtime or developer
examples.

```bash
bash bin/vaadin-desktop dev test-apps/flow-over-ipc-framework-app
bash bin/vaadin-desktop dev-vite test-apps/flow-over-ipc-framework-app
```

The application-owned Vite probe is used only by the explicit macOS acceptance
overlay to verify HMR and source restoration. It is not a framework runtime
adapter.

The app also depends on the sibling
`test-apps/flow-over-ipc-framework-shared` fixture as an ordinary Maven reactor
dependency. Its filtered classpath resource lets explicit acceptance verify
dependent-module resources and POM-driven rebuilds without adding test hooks to
framework production code.

The `flow-ipc-effective-maven-model-acceptance` profile additionally supplies
`test-apps/flow-over-ipc-profile-shared`. That fixture uses nonstandard Java and
resource roots so the explicit reactor-reload gate can verify Maven effective
model observation without making profile-only code a normal application or
framework dependency.

The app includes Vaadin Rich Text Editor, so its Maven configuration enables the
commercial banner for this test app only. Framework modules and examples do not
inherit that policy.

Unattended workflows and trace inspection are documented in
`docs/acceptance-tools.md`.
