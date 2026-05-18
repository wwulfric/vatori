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
- WebView requests every Flow client classpath resource advertised in bootstrap with `resourceRequest`; Java returns the resource bodies and metadata with redacted `resourceResponse` traces. `FlowBootstrap.js` and `FlowClient.js` are imported, while the remaining resources are verified as graph coverage.
- WebView imports the returned bodies as ES modules through Blob URLs, calls `FlowBootstrap.init` with an IPC bootstrap config, then calls `FlowClient.init` and verifies that a Flow client object is registered.
- Java emits a minimum server-initiated `push` envelope after bootstrap by mutating the server-side Flow component tree and serializing the resulting UIDL with `UidlWriter`.
- WebView queues that push until the visible Flow client engine is registered, then flushes it into the engine through the IPC message bridge.
- With the verification hook enabled, resync is requested through the Flow client IPC bridge or its equivalent IPC-backed XHR verification fallback, Java dispatches the Flow-client-shaped resync body through `UidlRequestHandler`, and the resulting UIDL patch is returned to the same Flow client XHR path.
- The smoke app includes a v1.1 component event matrix with a native Flow `Input` text field and checkbox. Their `change` events carry `element.value` / `element.checked` data through the IPC-backed Flow client XHR path and update visible Flow state with `Text input: Flow IPC typed` and `Checkbox: checked`.
- WebView applies replace-root patches.
- Desktop API calls in `dev-ipc` emit `VFIPC1` `command` envelopes.
- Swift host handles `window.setTitle` through AppKit, `clipboard.writeText` through `NSPasteboard`, trace-only `notification.show` and `fileDialog.openFile` through AppKit `NSOpenPanel`.
- Swift host emits `capabilityResult` envelopes for command handling, including `allow-all-dev`, `manifest-enforced` or `trace-only-dry-run` permission metadata.

## Verification

Compile both runtimes:

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
CLANG_MODULE_CACHE_PATH=/Users/bytedance/IdeaProjects/vatori/.build/clang-module-cache /usr/bin/swiftc vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift -o /private/tmp/VaadinDesktopHost-flow-ipc-check
```

Run a real-window smoke verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` is a verification-only hook. It injects a WebView user script that clicks the real DOM button after the Flow client engine is promoted to the visible surface and the XHR bridge is ready. The first engine request may attach the existing bootstrap container; the verification hook then clicks again so the visible Flow client sends the real `click` UIDL RPC through the IPC-backed XHR shim. The event travels through WKWebView -> Swift host -> Java sidecar -> Vaadin listener -> Swift capability handling -> WebView patch, and the visible Flow client consumes the event UIDL response. `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1` asks the Flow client engine to create one resync request after the event patch; if that engine method does not emit XHR on the current bundle, the verification bridge sends the same Flow-client-shaped resync body through the IPC-backed XHR shim and completes it with Java's `UidlRequestHandler` resync UIDL. `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` sends one WebView-originated `error` envelope after the verified event/resync path, proving that client-side bridge errors are representable in the same trace format. `VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1` opens the real AppKit file dialog path and then cancels it automatically so unattended smoke runs do not block; `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` also enables this cancellation path. These hooks avoid unreliable global mouse coordinates on managed macOS desktops.

Verified trace evidence:

- `HOST_PROTOCOL bootstrapRequest` from WebView to Java.
- `HOST_PROTOCOL bootstrapResponse` with `uidl.source=UidlWriter` and `uidl.containsSyncId=true`.
- `HOST_PROTOCOL bootstrapResponse` with `flowClientResources.source=flow-client-classpath`.
- `HOST_PROTOCOL resourceRequest` / `resourceResponse` for all six advertised Flow client classpath resources, including `FlowBootstrap.js`, `FlowClient.js`, `Flow.js`, `index.js`, `compile.properties` and the generated GWT client bundle; host trace redacts the full bodies and keeps `bodyPreview` / `bodyBytes`.
- `HOST_PROTOCOL push` with `uidl.source=UidlWriter`, `uidl.reason=push`, `uidl.async=true`, `uidl.consumer=flow-client-engine-visible` and `5 Server push over IPC READY`.
- `HOST_PROTOCOL event` with `dispatch=flow-client-xhr`, first for Flow client's `attachExistingElementById` bootstrap binding and then for a real `uidlRpc` click with `clientId=1`, `uidlRpc.csrfToken`, `uidlRpc.syncId` and Flow `nodeId`.
- `HOST_PROTOCOL event` for `ipc-text-input` and `ipc-checkbox` with `event=change`, carrying `uidlRpc.rpc[].data.element.value` or `uidlRpc.rpc[].data.element.checked`; the follow-up `HOST_PROTOCOL patch` entries are generated by `UidlRequestHandler` and consumed by the visible Flow client engine.
- `HOST_PROTOCOL resync` from the Flow client XHR bridge to Java and a follow-up `HOST_PROTOCOL patch` with `eventDispatch=flow-client-xhr`, `uidl.source=UidlRequestHandler`, `uidl.reason=resync`, `uidl.resync=true`, `uidl.consumer=flow-client-engine-visible` and `resynchronize=true`.
- Java `VFIPC1 command` for `window.setTitle`, `notification.show`, `clipboard.writeText` and `fileDialog.openFile`.
- `HOST_PROTOCOL capabilityResult` for `window.setTitle`, `notification.show`, `clipboard.writeText` and `fileDialog.openFile`; file dialog results use `mode=native-appkit-open-panel` with `canceled`, `paths`, `allowsMultipleSelection`, `securityScoped=false` and `autoCanceled` metadata.
- `HOST_PROTOCOL capabilityManifest` loaded from the app module's `desktop-capabilities.json`.
- `HOST_PROTOCOL capabilityResult` payloads with permission metadata. The current smoke app uses `permissionMode=manifest-enforced`; declared commands return `permissionDecision=allowed` and `permissionReason=manifest-allow`, while the intentionally undeclared `diagnostics.deniedCapability` command returns `permissionDecision=denied` and `permissionReason=manifest-denied`.
- `HOST_PROTOCOL error` with `error.code=webviewVerificationProbe` for the verification-only WebView error-reporting path.
- `HOST_PROTOCOL patch` with `eventDispatch=flow-client-xhr`, `Clicks: 1`, `Text input: Flow IPC typed`, `Checkbox: checked` and event UIDL generated by `UidlRequestHandler`.
- Java-to-WebView payloads carry a monotonic `serverSequence` with `serverSequenceScope=java-sidecar-to-webview`.
- `HOST_PROTOCOL webview.dom` with `flowClientResourceLoaded=true`, `flowClientBootstrapInitialized=true`, `flowClientResourceManifestCount=6`, `flowClientResourceGraphRequestedCount=6`, `flowClientResourceGraphResponseCount=6`, `flowClientResourceGraphVerified=true`, `flowClientEngineResourceLoaded=true`, `flowClientEngineInitialized=true`, `flowClientEngineClientRegistered=true`, `flowClientEngineBootstrapDomBound=true`, `flowClientEngineListBound=true`, `flowClientEngineListItemCount=4`, `flowIpcServerSequenceObserved=true`, `flowIpcServerSequenceMonotonic=true`, `flowClientEngineXhrBridgeInstalled=true`, `flowClientEngineXhrResponseObserved=true`, `flowClientEngineIpcMessageBridgeInstalled=true`, `flowClientEngineIpcResyncBridgeInstalled=true`, `flowClientEngineEventPatchConsumed=true`, `flowClientEngineTextInputEventObserved=true`, `flowClientEngineCheckboxEventObserved=true`, `flowClientEnginePushPatchConsumed=true`, `flowClientEngineQueuedMessageHighWater=1`, `flowClientEngineQueuedMessageOrder=push:3`, `flowClientEngineDeliveredQueuedMessageOrder=push:3`, `flowClientEngineResyncPatchConsumed=true`, `flowClientEngineQueuedMessageCount=0`, `flowClientEngineVisibleOwner=true`, `flowClientEngineVisibleRootId=flow-client-engine-root` and `flowClientEngineLocalFallbackHidden=true`.
- `HOST_PROTOCOL webview.snapshot` writes `.build/vaadin-desktop-host-swift/window-snapshot.png`, whose rendered state shows `Clicks: 1`.

Inspect the sample trace:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
```

The inspector fails with exit code `2` if the required Flow IPC message types or key markers are missing.

## Current Limitations

This is not the final complete Vaadin Flow transport yet:

- Bootstrap, event patch, one server push and one verification resync payload include real Vaadin UIDL and are consumed by the visible Flow client engine for the smoke app subset. The repository-local UIDL applier remains as a diagnostic fallback for non-engine or failed-delivery paths.
- Push covers one server-initiated UIDL update in the smoke app, and Java-to-WebView delivery now exposes monotonic `serverSequence` plus WebView queue/flush diagnostics for that push. This is still not a full Flow push implementation with background scheduling, durable queues or reconnect semantics.
- Flow client resource loading currently requests and verifies all six advertised classpath resources, imports `FlowBootstrap.js` and `FlowClient.js`, augments the current Flow client bundle with narrow IPC message/resync bridge methods, then calls `FlowBootstrap.init` / `FlowClient.init`, verifies bootstrap node bindings through `client.getByNodeId(...)`, and promotes that client to the visible renderer. This is not production bundle/dev-mode execution yet.
- Click, native text input `change` and native checkbox `change` are implemented for the smoke app. Vaadin Web Component events, selection components, Grid/VirtualList, attach/detach and broader client event collection remain future work.
- The WebView bridge now sends Flow-client-shaped UIDL RPC for the covered click/change events, but full Flow client event collection is not implemented.
- `VaadinSession` and `UI` are desktop-shaped; Flow IPC bootstrap now carries `Vaadin-Security-Key`; event and resync envelopes carry the key back to Java; and envelope `sessionId`/`uiId`, `csrfToken`/security key, `syncId` and `clientId` are checked before UIDL RPC dispatch. Full resync semantics are not complete.
- Notification handling is trace-only; clipboard write is backed by native `NSPasteboard`; file dialog uses real AppKit `NSOpenPanel`, with automated smoke verification using the cancellation path. Security-scoped bookmarks and production grants are not implemented yet.
- Upload/download, full frontend resource graph execution, production bundle and dev mode are not implemented for `dev-ipc`.

The value of this slice is that the core demo path can run without a localhost HTTP server and exposes the real host/WebView/Java IPC contract that the later UIDL adapter must satisfy.
