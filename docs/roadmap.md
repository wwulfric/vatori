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

Status: first minimum runtime slice implemented, server-side UIDL writer integrated into `dev-ipc` bootstrap/push payloads, Flow client XHR click/change and ordering-recovery resync dispatch wired over IPC, FlowBootstrap and FlowClient resource loading/import/init over IPC, BigDecimalField classpath frontend plus TextField, Checkbox and Select dev-bundle frontend resources executed with explicit classification for legacy fallback shims, BigDecimalField fallback imports driven by manifest `importRewrites`, v1.5 frontend staging evidence added for the current resource graph, v1.6 TextField/npm graph blocker narrowed to production `indexhtml` reimport and duplicate custom element registration, v1.7 TextField side-effect/module-identity execution added, one theme/static classpath CSS lookup verified, one repository-local Vaadin generated-flow-imports fixture resource verified, one real Vaadin dev-bundle `generated-flow-imports-*.js` chunk moved to staged generated/lazy execution attempt with a strict blocker, one Vaadin dev-bundle `config/stats.json` production graph resource verified, one Vaadin dev-bundle `package-lock.json` package graph resource verified, visible Flow client ownership verified for bootstrap, click/change-event patches, ordered server pushes and recovery resync, and resource execution/lazy lookup diagnostics added for the advertised Flow client resource graph.

- Audit Vaadin Flow event and patch internals.
- Extract a minimal adapter around bootstrap, event dispatch and UIDL/patch response.
- Run one route without servlet request/response in the event path.
- `dev-ipc` now runs `test-apps/flow-over-ipc-smoke-app` without localhost HTTP in the core event/patch path.
- `bash bin/uidl-adapter-smoke` now verifies that a desktop-shaped session/UI can drive Vaadin's real `UidlWriter` and produce UIDL `changes`/`syncId`.
- `bash bin/uidl-rpc-smoke` now verifies that a real Flow event RPC can pass through `UidlRequestHandler`/`ServerRpcHandler` and produce a listener UIDL patch without a real HTTP socket.
- `dev-ipc` bootstrap/push/patch/resync envelopes now include `payload.uidl`; bootstrap and ordered server-initiated pushes come from `UidlWriter`, and WebView-produced click plus ordering-recovery resync RPC patches come from `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, click-event UIDL, push UIDL and recovery resync UIDL are consumed by the visible Flow client engine, with Java-to-WebView `serverSequence` diagnostics proving monotonic receive order for the smoke path.
- `dev-ipc` bootstrap now advertises Vaadin `flow-client` classpath resources, and the WebView requests the full advertised classpath resource graph over IPC while importing/initializing `FlowBootstrap.js` and `FlowClient.js` as the first no-HTTP frontend resource loading slice. The WebView also classifies executed vs verified-only resources and performs a dynamic `flow-client-lazy-lookup` probe for `Flow.js`.
- `dev-ipc` bootstrap now advertises `componentFrontendResources` for `com.vaadin.flow.component.textfield.BigDecimalField`, `com.vaadin.flow.component.textfield.TextField`, `com.vaadin.flow.component.checkbox.Checkbox` and `com.vaadin.flow.component.select.Select`; the WebView requests the real `META-INF/resources/frontend/vaadin-big-decimal-field.js` classpath module plus focused Vaadin dev-bundle TextField, Checkbox and Select entries, stages real artifact bodies as Blob module URLs, executes BigDecimalField with manifest import rewrites, resolves Lit CSS through a focused extracted helper, executes the TextField/Checkbox/Select entries, records v1.7 module identity metadata for the resolved TextField production `indexhtml` side-effect path, records legacy fallback metadata for the old TextField shim path and verifies `Vaadin BigDecimalField: 42.5`, `Vaadin TextField: Flow IPC typed` and `Vaadin Checkbox: checked` through the real Flow event/patch path.
- `dev-ipc` resource manifests expose a frontend artifact graph with artifact families, module specifiers, origins, resolver names, dependency links, execution policies, a verified theme/static classpath CSS resource, a verified repository-local generated Flow imports fixture resource, a real Vaadin dev-bundle `generated-flow-imports-*.js` chunk plus markdown helper dependency, a verified Vaadin dev-bundle `config/stats.json` production graph resource, a verified Vaadin dev-bundle `package-lock.json` package graph resource, a partial production bundle execution boundary, and strict blockers for unresolved npm/frontend, dev-mode/Vite execution and full generated lazy execution paths.
- `dev-ipc` now records v1.5 no-localhost frontend staging through `resourceStage`: real artifact bodies are staged as Blob module URLs, BigDecimalField plus focused TextField/Checkbox/Select entries execute through staged artifacts, and the real Vaadin dev-bundle `generated-flow-imports-*.js` chunk is attempted through the generated/lazy staging path with a recorded `Flow.loadOnDemand` boundary and browser error.
- `dev-ipc` v1.6 narrows the TextField blocker to production `indexhtml` side effects; v1.7 resolves that duplicate custom element layer by reusing stable module identity for the TextField entry and production `indexhtml` dependency, with side-effect metadata and real TextField event diagnostics in `resourceStage` / `webview.dom`.
- `test-apps/flow-over-ipc-framework-app` now provides a less smoke-specific validation target for the generic shell/runtime path. It uses ordinary Flow Java APIs for Button, BigDecimalField, Checkbox, Select, repeated-list text updates and manifest-gated desktop capability calls, with a dedicated `--require-flow-ipc-v1_3-framework` host-log gate that includes first meaningful paint evidence. The v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7 fixture gates remain centered on the smoke fixture.

First issue candidates:

- Extend `DesktopVaadinRequest` to enforce full resync state from Host Protocol; minimum Flow IPC `sessionId`/`uiId`, `csrfToken`/security key, `syncId` and `clientId` preflight is already in place.
- Replace the controlled server-initiated push ordering and duplicate/late recovery smoke with a real Flow push queue, ack, reconnect/resync path and durable ordering model; the current smoke path proves monotonic Java-to-WebView delivery with `serverSequence`, records WebView queue/flush/deliver/ignore order, and triggers explicit recovery resync for a duplicate/late probe.
- Continue broadening Flow client event/resource coverage beyond the current smoke-app button, native input/checkbox and BigDecimalField/TextField/Checkbox/Select validation slice, replacing the generated/lazy blocker and remaining staged blockers with real production bundle, dev-mode/Vite execution and full generated lazy execution.
- Keep broadening the framework-shaped app gate so generic renderer promotion, bridge installation and ordinary Flow app interactions stay covered without relying on the smoke fixture's DOM structure.

## v0.3: Capability Gateway

- Add capability manifest. The first `dev-ipc` scaffold now reads module-root `desktop-capabilities.json`, supports `allow-all-dev`, `manifest-enforced` and `trace-only-dry-run`, and the Flow IPC smoke app runs in `manifest-enforced` mode with an explicit unlisted-command deny-path. `fileDialog.openFile` now uses AppKit `NSOpenPanel` and exposes an automated cancellation path for repeatable smoke verification.
- Add a bounded file-transfer gateway. The v1.1 smoke app now sends `fileTransfer.download` and `fileTransfer.selectUploadFile` through the same manifest-gated capability path; the Swift host writes a controlled download file, reads selected-file metadata plus sampled bytes and redacts file contents from trace output. Full streaming, progress/cancel and sandbox-safe production grants remain future work.
- Enforce window/source/user-gesture checks.
- Implement notifications and production-grade file permission grants through real native APIs.

## v0.4: Packaging

- Add jlink/jpackage benchmark.
- Package at least one smoke app.
- Document app size, startup and memory.
