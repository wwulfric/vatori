# Roadmap

## v0: Framework Skeleton

Status: implemented in this repository.

- Maven multi-module framework.
- Real Vaadin Flow examples.
- Desktop Java API.
- Compatibility host.
- IPC envelope and trace scaffold.
- Independent smoke app.

## v0.1: Real Host Spike

Status: in progress in this repository.

- Replace browser-style compatibility host with Swift/AppKit WKWebView host on macOS.
- Keep local HTTP temporarily.
- Route desktop commands through WebView/native bridge.
- Manage Java sidecar lifecycle from the host.
- Document Host Protocol v0 traces and compatibility boundaries.

## v0.2: Flow Transport Adapter Spike

Status: first minimum runtime slice implemented, server-side UIDL writer integrated into `dev-ipc` bootstrap/push payloads, Flow client XHR click/change and ordering-recovery resync dispatch wired over IPC, FlowBootstrap and FlowClient resource loading/import/init over IPC, BigDecimalField and Checkbox component frontend resources executed with explicit shim classification, visible Flow client ownership verified for bootstrap, click/change-event patches, ordered server pushes and recovery resync, and resource execution/lazy lookup diagnostics added for the advertised Flow client resource graph.

- Audit Vaadin Flow event and patch internals.
- Extract a minimal adapter around bootstrap, event dispatch and UIDL/patch response.
- Run one route without servlet request/response in the event path.
- `dev-ipc` now runs `test-apps/flow-over-ipc-smoke-app` without localhost HTTP in the core event/patch path.
- `bash bin/uidl-adapter-smoke` now verifies that a desktop-shaped session/UI can drive Vaadin's real `UidlWriter` and produce UIDL `changes`/`syncId`.
- `bash bin/uidl-rpc-smoke` now verifies that a real Flow event RPC can pass through `UidlRequestHandler`/`ServerRpcHandler` and produce a listener UIDL patch without a real HTTP socket.
- `dev-ipc` bootstrap/push/patch/resync envelopes now include `payload.uidl`; bootstrap and ordered server-initiated pushes come from `UidlWriter`, and WebView-produced click plus ordering-recovery resync RPC patches come from `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, click-event UIDL, push UIDL and recovery resync UIDL are consumed by the visible Flow client engine, with Java-to-WebView `serverSequence` diagnostics proving monotonic receive order for the smoke path.
- `dev-ipc` bootstrap now advertises Vaadin `flow-client` classpath resources, and the WebView requests the full advertised classpath resource graph over IPC while importing/initializing `FlowBootstrap.js` and `FlowClient.js` as the first no-HTTP frontend resource loading slice. The WebView also classifies executed vs verified-only resources and performs a dynamic `flow-client-lazy-lookup` probe for `Flow.js`.
- `dev-ipc` v1.3 bootstrap now advertises `componentFrontendResources` for `com.vaadin.flow.component.textfield.BigDecimalField` and `com.vaadin.flow.component.checkbox.Checkbox`; the WebView requests and imports the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` classpath module plus the `META-INF/vatori/frontend-shims/vaadin-checkbox.js` shim, records `blockedReason` / `failureClassification` for the missing npm import graph and verifies `Vaadin BigDecimalField: 42.5` and `Vaadin Checkbox: checked` through the real Flow event/patch path.
- `test-apps/flow-over-ipc-framework-app` now provides a less smoke-specific validation target for the generic shell/runtime path. It uses ordinary Flow Java APIs for Button, BigDecimalField, Checkbox, repeated list updates and manifest-gated desktop capability calls, while the strict trace gates remain centered on the smoke fixture.

First issue candidates:

- Extend `DesktopVaadinRequest` to enforce full resync state from Host Protocol; minimum Flow IPC `sessionId`/`uiId`, `csrfToken`/security key, `syncId` and `clientId` preflight is already in place.
- Replace the controlled server-initiated push ordering and duplicate/late recovery smoke with a real Flow push queue, ack, reconnect/resync path and durable ordering model; the current smoke path proves monotonic Java-to-WebView delivery with `serverSequence`, records WebView queue/flush/deliver/ignore order, and triggers explicit recovery resync for a duplicate/late probe.
- Continue broadening Flow client event/resource coverage beyond the current smoke-app button, native input/checkbox and BigDecimalField/Checkbox component set, replacing resource diagnostics and v1.3 shims with real production bundle/dev-mode execution.
- Add a strict validation gate for the framework-shaped app so generic renderer promotion and bridge installation are tested without relying on the smoke fixture's DOM structure.

## v0.3: Capability Gateway

- Add capability manifest. The first `dev-ipc` scaffold now reads module-root `desktop-capabilities.json`, supports `allow-all-dev`, `manifest-enforced` and `trace-only-dry-run`, and the Flow IPC smoke app runs in `manifest-enforced` mode with an explicit unlisted-command deny-path. `fileDialog.openFile` now uses AppKit `NSOpenPanel` and exposes an automated cancellation path for repeatable smoke verification.
- Add a bounded file-transfer gateway. The v1.1 smoke app now sends `fileTransfer.download` and `fileTransfer.selectUploadFile` through the same manifest-gated capability path; the Swift host writes a controlled download file, reads selected-file metadata plus sampled bytes and redacts file contents from trace output. Full streaming, progress/cancel and sandbox-safe production grants remain future work.
- Enforce window/source/user-gesture checks.
- Implement notifications and production-grade file permission grants through real native APIs.

## v0.4: Packaging

- Add jlink/jpackage benchmark.
- Package at least one smoke app.
- Document app size, startup and memory.
