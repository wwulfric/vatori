# Flow over IPC Runtime

The first Flow over IPC runtime slice is available through:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

This path is intentionally separate from compatibility mode:

- `dev` starts embedded Spring Boot + localhost HTTP.
- `dev-swift` starts Swift/AppKit host but still loads the localhost Flow URL.
- `dev-ipc` starts Swift/AppKit host with `--transport flow-ipc` and loads a local WebView shell, not a localhost URL.

## Runtime Shape

```text
FlowOverIpcSmokeView (@Route, Button, Span, VerticalLayout)
  -> FlowIpcLauncher
  -> FlowIpcRuntime
  -> VFIPC1 stdin/stdout envelopes
  -> vaadin-desktop-host-swift
  -> WKWebView local HTML shell + visible Flow client engine
```

The Java sidecar is still launched by Maven `spring-boot:run`, but the application main does not start a web server. It calls `FlowIpcLauncher.launch(...)`, instantiates a real Vaadin Flow route class and waits for IPC messages on stdin.

`test-apps/flow-over-ipc-framework-app` is a second `dev-ipc` target for framework-shape validation:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

That app keeps the strict trace fixture separate from a less test-specific Flow view. It uses ordinary Flow Java APIs for a Button, `BigDecimalField`, `Checkbox`, repeated list updates and manifest-gated `window.setTitle`, `clipboard.writeText` and `notification.show` calls. Its semantic ids are optional diagnostics; the host/WebView runtime must continue to promote and lay out the visible renderer through generic Flow viewport readiness.

Its unattended interaction check drives the framework view without using smoke app ids:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

That gate requires the framework app launch, manifest, generic viewport promotion, first meaningful paint, two Web Component events, a Button apply event, UIDL patches, visible DOM completion, list item text update and manifest-allowed capability command/result traces.

## Verified Boundaries In This Slice

- Java sidecar lifecycle is owned by Swift/AppKit host.
- WKWebView loads local HTML through `loadHTMLString`, not `http://127.0.0.1`.
- WebView sends `bootstrapRequest` and `event` messages through `WKScriptMessageHandler`.
- Swift host forwards those messages to Java stdin as `VFIPC1` envelopes.
- Java instantiates real Vaadin Flow components and routes click plus native input/checkbox `change` events through `UidlRequestHandler`/`ServerRpcHandler`.
- Java listener mutates server-side Flow component state.
- Java returns `bootstrapResponse` and `patch` envelopes.
- Java includes real Vaadin UIDL JSON under `payload.uidl`; bootstrap uses `UidlWriter`, and click events use `UidlRequestHandler`/`ServerRpcHandler`.
- Java advertises real Vaadin `flow-client` classpath resources in `bootstrapResponse`.
- WebView requests every Flow client classpath resource advertised in bootstrap with `resourceRequest`; Java returns the resource bodies and metadata with redacted `resourceResponse` traces. `FlowBootstrap.js` and `FlowClient.js` are imported, while the remaining resources are SHA-verified and classified as verified-only / intentionally-not-executed for this smoke slice.
- The bootstrap resource manifests now carry the v1.4 frontend artifact graph fields: `frontendAssetPipelineVersion=flow-ipc-frontend-assets-v1_4`, `artifactGraphSource=vaadin-frontend-artifact-graph-v1_4`, artifact families, `moduleSpecifier`, `origin`, `resolver`, `dependencyOf`, `executionPolicy`, `artifactStatus`, one verified theme/static classpath CSS resource, one verified repository-local Vaadin generated-flow-imports fixture resource, one verified Vaadin production-bundle `stats.json` graph resource, one verified Vaadin dev-bundle `package-lock.json` package graph resource, a partial production bundle execution boundary, and explicit strict blockers for unresolved npm/frontend, dev-mode/Vite execution and full generated lazy execution.
- After graph coverage is complete, WebView sends one dynamic `flow-client-lazy-lookup` request for `META-INF/frontend/Flow.js` with `lookupKind=Flow.loadOnDemand-minimum-probe`. The result is verified over IPC and classified as `verified-only:intentionally-not-executed`.
- WebView imports the returned bodies as ES modules through Blob URLs, calls `FlowBootstrap.init` with an IPC bootstrap config, then calls `FlowClient.init` and verifies that a Flow client object is registered.
- Java emits two server-initiated `push` envelopes after bootstrap by mutating the server-side Flow component tree and serializing the resulting UIDL with `UidlWriter`.
- Java emits a controlled duplicate/late push probe with monotonic outer `serverSequence` metadata and an `orderingProbe.effectiveServerSequence` that replays the second push for recovery verification.
- WebView queues early pushes until the visible Flow client engine is registered, flushes the ordered pushes into the engine through the IPC message bridge, records delivered order, ignores the duplicate/late probe, and requests an explicit ordering-recovery resync.
- With the verification hook enabled, resync is requested through the Flow client IPC bridge or its equivalent IPC-backed XHR verification fallback, Java dispatches the Flow-client-shaped resync body through `UidlRequestHandler`, and the resulting UIDL patch is returned to the same Flow client XHR path.
- The smoke app includes a v1.1 component event matrix with a native Flow `Input` text field and checkbox. Their `change` events carry `element.value` / `element.checked` data through the IPC-backed Flow client XHR path and update visible Flow state with `Text input: Flow IPC typed` and `Checkbox: checked`.
- WebView applies replace-root patches.
- Desktop API calls in `dev-ipc` emit `VFIPC1` `command` envelopes.
- Swift host handles `window.setTitle` through AppKit, `clipboard.writeText` through `NSPasteboard`, trace-only `notification.show` and `fileDialog.openFile` through AppKit `NSOpenPanel`.
- Swift host handles bounded `fileTransfer.download` and `fileTransfer.selectUploadFile` smoke commands through the same native capability gateway. The download path writes a small UTF-8 body to a controlled path; the upload path reads metadata and a bounded sample from a controlled selected file without putting file contents into trace output.
- Swift host emits `capabilityResult` envelopes for command handling, including `allow-all-dev`, `manifest-enforced` or `trace-only-dry-run` permission metadata.
- WebView emits `webview.firstMeaningfulPaint` after the Flow client engine owns the visible renderer through the generic viewport promotion gate and the local fallback has been hidden. The framework app gate treats that as host-level first-paint regression evidence, separate from later DOM assertions and screenshots.

## Verification

Compile both runtimes:

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
CLANG_MODULE_CACHE_PATH=/Users/bytedance/IdeaProjects/vatori/.build/clang-module-cache /usr/bin/swiftc vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift -o /private/tmp/VaadinDesktopHost-flow-ipc-check
```

Run a real-window smoke verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` is a verification-only hook. It injects a WebView user script that waits for the Flow client XHR path before dispatching the click when the full component-event hook is enabled. The first engine request may attach the existing bootstrap container; the verification hook then clicks again so the visible Flow client sends the real `click` UIDL RPC through the IPC-backed XHR shim. The event travels through WKWebView -> Swift host -> Java sidecar -> Vaadin listener -> Swift capability handling -> WebView patch, and the visible Flow client consumes the event UIDL response. `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1` asks the bridge to complete the ordering-recovery resync path after the duplicate/late probe and component-event matrix; if the current Flow client method does not emit XHR, the verification bridge sends the same Flow-client-shaped resync body through the IPC-backed XHR shim and completes it with Java's `UidlRequestHandler` resync UIDL. `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` sends one WebView-originated `error` envelope after the verified event/resync path, proving that client-side bridge errors are representable in the same trace format. `VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1` opens the real AppKit file dialog path and then cancels it automatically so unattended smoke runs do not block; `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` also enables this cancellation path. `VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1` closes the host window after DOM/snapshot capture so the host log includes shutdown evidence for strict unattended gates. The bounded upload/download smoke uses controlled paths under the smoke app `.build` directory and does not require a file picker or save panel. These hooks avoid unreliable global mouse coordinates on managed macOS desktops.

Verified trace evidence:

- `HOST_PROTOCOL bootstrapRequest` from WebView to Java.
- `HOST_PROTOCOL bootstrapResponse` with `uidl.source=UidlWriter` and `uidl.containsSyncId=true`.
- `HOST_PROTOCOL bootstrapResponse` with `flowClientResources.source=flow-client-classpath`.
- `HOST_PROTOCOL bootstrapResponse` with `componentFrontendResources.source=vaadin-component-classpath-plus-dev-bundle-v1_4`, `frontendAssetPipelineVersion=flow-ipc-frontend-assets-v1_4`, `artifactGraphSource=vaadin-frontend-artifact-graph-v1_4`, `resolverStrategy=generic-component-frontend-manifest-v1_3`, artifact families for Flow client classpath, component classpath frontend, npm frontend, theme/static, production bundle, dev-mode bundle and generated lazy resources, `componentCount=3`, `componentTags=[vaadin-big-decimal-field, vaadin-checkbox, vaadin-select]`, `primaryResources` for `META-INF/resources/frontend/vaadin-big-decimal-field.js`, `META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js` and `META-INF/vatori/dev-bundle-entry/vaadin-select.js`, BigDecimalField `importRewriteStrategy=manifest-import-rewrite-v1_4`, one available theme/static resource at `META-INF/resources/frontend/themes/vatori-flow-ipc-smoke/theme-static.css`, one available generated-lazy resource at `META-INF/vatori/generated/flow/generated-flow-imports.js`, one available production bundle stats graph resource at `vaadin-dev-bundle/config/stats.json`, one available dev-mode package graph resource at `vaadin-dev-bundle/package-lock.json`, plus strict blocker metadata explaining that this is still not complete production bundle/dev-mode/lazy parity.
- `HOST_PROTOCOL resourceRequest` / `resourceResponse` for all six advertised Flow client classpath resources, including `FlowBootstrap.js`, `FlowClient.js`, `Flow.js`, `index.js`, `compile.properties` and the generated GWT client bundle; host trace redacts the full bodies and keeps `bodyPreview` / `bodyBytes`. v1.4 requests include `artifactFamily`, `moduleSpecifier`, `dependencyOf`, `executionPolicy`, `requestedBy`, `resolver` and `artifactStatus` when present.
- `HOST_PROTOCOL resourceRequest` / `resourceResponse` for the real Vaadin component frontend module `META-INF/resources/frontend/vaadin-big-decimal-field.js`, its manifest-declared import rewrite resources, Vaadin dev-bundle support chunks and the focused TextField/Checkbox/Select entries. The BigDecimal module plus Checkbox/Select dev-bundle entries are imported/executed in the no-localhost WebView; `lit` resolves through a focused extracted Lit CSS helper, the TextField entry is verified as a partial blocker resource with `blockedExecutionError=outlet._root`, and the remaining TextField shim entry is an explicit diagnostic for missing npm/frontend artifact coverage, not hidden production bundle parity or WebView path-specific code.
- A dynamic `HOST_PROTOCOL resourceRequest` / `resourceResponse` with `purpose=flow-client-lazy-lookup`, `lookupKind=Flow.loadOnDemand-minimum-probe` and `path=META-INF/frontend/Flow.js`.
- `HOST_PROTOCOL push` with `uidl.source=UidlWriter`, `uidl.reason=push`, `uidl.async=true`, `uidl.consumer=flow-client-engine-visible` and `5 Server push over IPC READY`.
- A second ordered `HOST_PROTOCOL push` with `pushIndex=2`, `ordering.mode=monotonic-server-sequence`, `ordering.expectedAction=deliver-in-order` and `6 Ordered push over IPC READY`.
- A controlled duplicate/late `HOST_PROTOCOL push` probe with `orderingProbe.kind=duplicate-late`, `orderingProbe.effectiveServerSequence` replaying the second push and `orderingProbe.expectedAction=ignore-and-request-resync`.
- `HOST_PROTOCOL event` with `dispatch=flow-client-xhr`, first for Flow client's `attachExistingElementById` bootstrap binding and then for a real `uidlRpc` click with `clientId=1`, `uidlRpc.csrfToken`, `uidlRpc.syncId` and Flow `nodeId`.
- `HOST_PROTOCOL event` for `ipc-text-input` and `ipc-checkbox` with `event=change`, carrying `uidlRpc.rpc[].data.element.value` or `uidlRpc.rpc[].data.element.checked`; the follow-up `HOST_PROTOCOL patch` entries are generated by `UidlRequestHandler` and consumed by the visible Flow client engine.
- `HOST_PROTOCOL event` for `ipc-vaadin-big-decimal` with `event=change`, carrying `uidlRpc.rpc[].data.element.value=42.5`; the Java listener is registered on a real `BigDecimalField` Flow component and returns a `UidlRequestHandler` patch containing `Vaadin BigDecimalField: 42.5`.
- `HOST_PROTOCOL event` for `ipc-vaadin-checkbox` with `event=change`, carrying `uidlRpc.rpc[].data.element.checked=true`; the Java listener is registered on a real `Checkbox` Flow component and returns a `UidlRequestHandler` patch containing `Vaadin Checkbox: checked`.
- `HOST_PROTOCOL resync` from the Flow client XHR bridge to Java with `reason=ordering-recovery-duplicate-late` and a follow-up `HOST_PROTOCOL patch` with `eventDispatch=flow-client-xhr`, `uidl.source=UidlRequestHandler`, `uidl.reason=resync`, `uidl.resync=true`, `uidl.consumer=flow-client-engine-visible` and `resynchronize=true`.
- Java `VFIPC1 command` for `window.setTitle`, `notification.show`, `clipboard.writeText`, `fileDialog.openFile`, `fileTransfer.download` and `fileTransfer.selectUploadFile`.
- `HOST_PROTOCOL capabilityResult` for `window.setTitle`, `notification.show`, `clipboard.writeText` and `fileDialog.openFile`; file dialog results use `mode=native-appkit-open-panel` with `canceled`, `paths`, `allowsMultipleSelection`, `securityScoped=false` and `autoCanceled` metadata.
- `HOST_PROTOCOL capabilityResult` for `fileTransfer.download` with `operation=download`, `mode=controlled-test-path`, `bodyRedacted=true`, `byteCount`, `filename`, `path` and permission metadata.
- `HOST_PROTOCOL capabilityResult` for `fileTransfer.selectUploadFile` with `operation=upload`, `mode=controlled-test-path`, `bodyRedacted=true`, `byteCount`, `sampledBytes`, `filename`, `path`, `securityScoped=false` and permission metadata.
- `HOST_PROTOCOL capabilityManifest` loaded from the app module's `desktop-capabilities.json`.
- `HOST_PROTOCOL capabilityResult` payloads with permission metadata. The current smoke app uses `permissionMode=manifest-enforced`; declared commands return `permissionDecision=allowed` and `permissionReason=manifest-allow`, while the intentionally undeclared `diagnostics.deniedCapability` command returns `permissionDecision=denied` and `permissionReason=manifest-denied`.
- `HOST_PROTOCOL error` with `error.code=webviewVerificationProbe` for the verification-only WebView error-reporting path.
- `HOST_PROTOCOL patch` with `eventDispatch=flow-client-xhr`, `Clicks: 1`, `Text input: Flow IPC typed`, `Checkbox: checked`, `Vaadin BigDecimalField: 42.5`, `Vaadin Checkbox: checked` and event UIDL generated by `UidlRequestHandler`.
- Java-to-WebView payloads carry a monotonic `serverSequence` with `serverSequenceScope=java-sidecar-to-webview`.
- `HOST_PROTOCOL webview.firstMeaningfulPaint` with `transport=flow-ipc`, `promotionGate=generic-flow-viewport`, `flowClientEngineVisibleOwner=true` and `flowClientEngineLocalFallbackHidden=true`.
- `HOST_PROTOCOL webview.dom` with `frontendAssetPipelineVersion=flow-ipc-frontend-assets-v1_4`, `frontendArtifactGraphManifestObserved=true`, `frontendArtifactFamilies=flow-client-classpath,vaadin-component-classpath-frontend,npm-frontend,theme-static,production-bundle,dev-mode-bundle,generated-lazy`, `frontendArtifactGraphDependencyObserved=true`, `frontendArtifactShimFallback=true`, `frontendArtifactProductionBundlePartial=true`, `frontendArtifactProductionBundleStatsVerified=true`, `frontendArtifactThemeStaticVerified=true`, `frontendArtifactGeneratedLazyVerified=true`, `frontendArtifactDevModeVerified=true`, `flowClientResourceThemeLookupResult=verified:META-INF/resources/frontend/themes/vatori-flow-ipc-smoke/theme-static.css`, `frontendArtifactProductionBundleStatsResult=verified:vaadin-dev-bundle/config/stats.json`, `frontendArtifactDevModePackageGraphResult=verified:vaadin-dev-bundle/package-lock.json`, strict blocker booleans for dev-mode execution and full generated/lazy execution, plus the existing Flow and component diagnostics.
- `HOST_PROTOCOL webview.dom` with `flowClientResourceLoaded=true`, `flowClientBootstrapInitialized=true`, `flowClientResourceManifestCount=6`, `flowClientResourceGraphRequestedCount=6`, `flowClientResourceGraphResponseCount=6`, `flowClientResourceGraphVerified=true`, `flowClientResourceExecutionClassificationComplete=true`, `flowClientResourceExecutedCount=2`, `flowClientResourceVerifiedOnlyCount=4`, `flowClientResourceIntentionallyNotExecutedCount=4`, `flowClientResourceExecutionFailedCount=0`, `flowClientResourceLazyLookupRequested=true`, `flowClientResourceLazyLookupVerified=true`, `flowClientResourceStaticLookupVerified=true`, `vaadinWebComponentResourceResolverStrategy=generic-component-frontend-manifest-v1_3`, `vaadinWebComponentResourceManifestCount=11`, `vaadinWebComponentResourceGraphVerified=true`, `vaadinWebComponentResourceExecuted=true`, `vaadinWebComponentResourceExecutedCount=3`, `vaadinWebComponentResourceExecutedPaths=META-INF/resources/frontend/vaadin-big-decimal-field.js,META-INF/vatori/dev-bundle-entry/vaadin-checkbox.js,META-INF/vatori/dev-bundle-entry/vaadin-select.js`, `vaadinWebComponentResourceExecutedTags=vaadin-big-decimal-field,vaadin-checkbox,vaadin-select`, `vaadinWebComponentResourceImportMode=dynamic-import-blob-with-manifest-import-rewrites-v1_4`, `vaadinWebComponentImportRewriteStrategy=manifest-import-rewrite-v1_4`, `vaadinWebComponentImportRewriteSpecifiers=lit,@vaadin/text-field/src/vaadin-text-field.js,@vaadin/component-base/src/define.js`, `vaadinWebComponentImportRewritePaths=META-INF/vatori/dev-bundle-entry/lit-css.js,META-INF/vatori/frontend-shims/vaadin-text-field.js,META-INF/vatori/dev-bundle-entry/component-base-define.js`, `vaadinWebComponentResourceShimmedImports=@vaadin/text-field/src/vaadin-text-field.js`, `vaadinWebComponentTagDefined=true`, `vaadinWebComponentRendered=true`, `vaadinWebComponentEventDispatchMode=dom-event`, `vaadinWebComponentEventRpcObserved=true`, `vaadinWebComponentStatusText=Vaadin BigDecimalField: 42.5`, `vaadinSecondWebComponentTagDefined=true`, `vaadinSecondWebComponentRendered=true`, `vaadinSecondWebComponentEventDispatchMode=dom-event`, `vaadinSecondWebComponentEventRpcObserved=true`, `vaadinSecondWebComponentStatusText=Vaadin Checkbox: checked`, `flowClientEngineResourceLoaded=true`, `flowClientEngineInitialized=true`, `flowClientEngineClientRegistered=true`, `flowClientEngineViewportReady=true`, `flowClientEnginePromotionGate=generic-flow-viewport`, `flowClientEngineBootstrapDomBound=true`, `flowClientEngineListBound=true`, `flowClientEngineListItemCount=4`, `flowIpcServerSequenceObserved=true`, `flowIpcServerSequenceMonotonic=true`, `flowClientEngineXhrBridgeInstalled=true`, `flowClientEngineXhrResponseObserved=true`, `flowClientEngineIpcMessageBridgeInstalled=true`, `flowClientEngineIpcResyncBridgeInstalled=true`, `flowClientEngineEventPatchConsumed=true`, `flowClientEngineTextInputEventObserved=true`, `flowClientEngineCheckboxEventObserved=true`, file-transfer diagnostics, ordered push diagnostics, recovery resync diagnostics, `flowClientEngineVisibleOwner=true`, `flowClientEngineVisibleRootId=flow-client-engine-root` and `flowClientEngineLocalFallbackHidden=true`.
- `HOST_PROTOCOL webview.snapshot` writes `.build/vaadin-desktop-host-swift/window-snapshot.png`, whose rendered state shows `Clicks: 1`.

Inspect the sample trace:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

The inspector fails with exit code `2` if the required Flow IPC message types or key markers are missing.

## Current Limitations

This is not the final complete Vaadin Flow transport yet:

- Bootstrap, event patch, two ordered server pushes and one recovery resync payload include real Vaadin UIDL and are consumed by the visible Flow client engine for the smoke app subset. The repository-local UIDL applier remains as a diagnostic fallback for non-engine or failed-delivery paths.
- Push ordering covers two server-initiated UIDL updates plus one controlled duplicate/late probe in the smoke app. Java-to-WebView delivery exposes monotonic outer `serverSequence`; the probe carries `orderingProbe.effectiveServerSequence` to model a replay of the second push, the WebView ignores it deterministically, records delivered/ignored order and requests recovery resync. This is still not a full Flow push implementation with background scheduling, durable queues, reconnect after process crash or WebSocket parity.
- Flow client resource loading currently requests and verifies all six advertised classpath resources, imports `FlowBootstrap.js` and `FlowClient.js`, augments the current Flow client bundle with narrow IPC message/resync bridge methods, then calls `FlowBootstrap.init` / `FlowClient.init`, records generic viewport readiness and promotes that client to the visible renderer through `flowClientEnginePromotionGate=generic-flow-viewport`. Smoke-specific node bindings remain as diagnostics, not the renderer promotion gate. It also classifies `Flow.js`, `index.js`, `compile.properties` and the generated GWT bundle as verified-only / intentionally-not-executed, records zero execution failures and performs one dynamic lazy lookup for `Flow.js`. The component resolver imports the real `vaadin-big-decimal-field.js` classpath module, rewrites BigDecimalField imports from manifest `importRewrites`, resolves `lit` through a focused extracted Lit CSS helper, resolves `@vaadin/component-base/src/define.js` through an extracted Vaadin dev-bundle define helper, verifies focused Vaadin dev-bundle TextField/Checkbox/Select entries, verifies the repository-local generated-flow-imports fixture as a generated-lazy artifact, verifies Vaadin dev-bundle `config/stats.json` as a production-bundle graph artifact, and verifies the Vaadin dev-bundle `package-lock.json` as a dev-mode package graph artifact; this proves a reusable manifest-driven resource boundary, not broad production bundle/dev-mode parity. The remaining BigDecimalField npm shim is `@vaadin/text-field`; replacing it with the focused TextField dev-bundle entry currently triggers the recorded `outlet._root` WebView execution blocker.
- Click, native text input `change`, native checkbox `change`, the `BigDecimalField` Web Component `change` path and the `Checkbox` Web Component `change` path are implemented for the smoke app. `Select` is covered by the framework validation app, while Grid/VirtualList, attach/detach and broader client event collection remain future work.
- The WebView bridge now sends Flow-client-shaped UIDL RPC for the covered click/change events, but full Flow client event collection is not implemented.
- `VaadinSession` and `UI` are desktop-shaped; Flow IPC bootstrap now carries `Vaadin-Security-Key`; event and resync envelopes carry the key back to Java; and envelope `sessionId`/`uiId`, `csrfToken`/security key, `syncId` and `clientId` are checked before UIDL RPC dispatch. Full resync semantics are not complete.
- Notification handling is trace-only; clipboard write is backed by native `NSPasteboard`; file dialog uses real AppKit `NSOpenPanel`, with automated smoke verification using the cancellation path. Security-scoped bookmarks and production grants are not implemented yet.
- Upload/download is only a bounded v1.1 smoke boundary: it uses inline UTF-8 download bodies capped by the Java API, controlled test paths, redacted trace metadata and sampled upload reads. It is not Vaadin `Upload`, `Anchor` or `StreamResource` parity; binary streaming, progress, cancellation, sandbox bookmarks and save/open-panel production grants remain future work.
- Full frontend resource graph execution, production bundle and dev mode are not implemented for `dev-ipc`; only the v1.4 artifact manifest, one theme/static classpath CSS lookup, one repository-local generated Flow imports fixture lookup, one Vaadin dev-bundle `config/stats.json` production graph lookup, one Vaadin dev-bundle `package-lock.json` package graph lookup, manifest-driven BigDecimalField import rewrites, focused TextField/Checkbox/Select dev-bundle entries, narrow classification, lazy-lookup and BigDecimalField/Checkbox/Select component diagnostics described above are verified. Production bundle execution, dev-mode/Vite execution and full generated lazy execution remain strict blockers until they have real resolver coverage.
- `test-apps/flow-over-ipc-framework-app` is a framework-shape validation target, not a replacement for the strict smoke trace. Its dedicated `--require-flow-ipc-v1_3-framework` gate proves that the generic shell can render and interact with a less smoke-specific Flow project, including framework app component events, a repeated-list text update and manifest-gated desktop capability calls.

The value of this slice is that the core demo path can run without a localhost HTTP server and exposes the real host/WebView/Java IPC contract that the later UIDL adapter must satisfy.
