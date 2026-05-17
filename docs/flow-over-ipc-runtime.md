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
  -> WKWebView local HTML shell + Flow client resource probe
```

The Java sidecar is still launched by Maven `spring-boot:run`, but the application main does not start a web server. It calls `FlowIpcLauncher.launch(...)`, instantiates a real Vaadin Flow route class and waits for IPC messages on stdin.

## Verified Boundaries In This Slice

- Java sidecar lifecycle is owned by Swift/AppKit host.
- WKWebView loads local HTML through `loadHTMLString`, not `http://127.0.0.1`.
- WebView sends `bootstrapRequest` and `event` messages through `WKScriptMessageHandler`.
- Swift host forwards those messages to Java stdin as `VFIPC1` envelopes.
- Java instantiates real Vaadin Flow components and routes click events through `UidlRequestHandler`/`ServerRpcHandler`.
- Java listener mutates server-side Flow component state.
- Java returns `bootstrapResponse` and `patch` envelopes.
- Java includes real Vaadin UIDL JSON under `payload.uidl`; bootstrap uses `UidlWriter`, and click events use `UidlRequestHandler`/`ServerRpcHandler`.
- Java advertises real Vaadin `flow-client` classpath resources in `bootstrapResponse`.
- WebView requests `META-INF/frontend/FlowBootstrap.js` with `resourceRequest`; Java returns the resource body and metadata with `resourceResponse`.
- WebView imports the returned `FlowBootstrap.js` body as an ES module through a Blob URL, verifies that it exports `init`, then calls `FlowBootstrap.init` with an IPC bootstrap config.
- Java emits a minimum server-initiated `push` envelope after bootstrap by mutating the server-side Flow component tree and serializing the resulting UIDL with `UidlWriter`.
- WebView applies replace-root patches.
- Desktop API calls in `dev-ipc` emit `VFIPC1` `command` envelopes.
- Swift host handles `window.setTitle` through AppKit and `clipboard.writeText` through `NSPasteboard`.
- Swift host emits `capabilityResult` envelopes for command handling.

## Verification

Compile both runtimes:

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
CLANG_MODULE_CACHE_PATH=/Users/bytedance/IdeaProjects/vatori/.build/clang-module-cache /usr/bin/swiftc vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift -o /private/tmp/VaadinDesktopHost-flow-ipc-check
```

Run a real-window smoke verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` is a verification-only hook. It injects a WebView user script that clicks the real DOM button after `bootstrapResponse`, so the event still travels through WKWebView -> Swift host -> Java sidecar -> Vaadin listener -> Swift capability handling -> WebView patch. `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1` asks the WebView bridge to send one `resync` envelope after the event patch, so Java returns a `UidlWriter` patch with `resync=true`; Vaadin currently emits `resynchronize=true`, which the WebView treats as a rebuild signal and satisfies through the local tree fallback. These hooks avoid unreliable global mouse coordinates on managed macOS desktops.

Verified trace evidence:

- `HOST_PROTOCOL bootstrapRequest` from WebView to Java.
- `HOST_PROTOCOL bootstrapResponse` with `uidl.source=UidlWriter` and `uidl.containsSyncId=true`.
- `HOST_PROTOCOL bootstrapResponse` with `flowClientResources.source=flow-client-classpath`.
- `HOST_PROTOCOL resourceRequest` / `resourceResponse` for `META-INF/frontend/FlowBootstrap.js`; host trace redacts the full body and keeps `bodyPreview` / `bodyBytes`.
- `HOST_PROTOCOL push` with `uidl.source=UidlWriter`, `uidl.reason=push` and `5 Server push over IPC READY`.
- `HOST_PROTOCOL event` with `dispatch=flow-client-rpc`, `uidlRpc.csrfToken`, `uidlRpc.syncId`, `uidlRpc.clientId` and Flow `nodeId`.
- `HOST_PROTOCOL resync` from WebView to Java and a follow-up `HOST_PROTOCOL patch` with `uidl.source=UidlWriter`, `uidl.reason=resync`, `uidl.resync=true` and `resynchronize=true`.
- Java `VFIPC1 command` for `window.setTitle`, `notification.show` and `clipboard.writeText`.
- `HOST_PROTOCOL capabilityResult` for `window.setTitle`, `notification.show` and `clipboard.writeText`.
- `HOST_PROTOCOL patch` with `Clicks: 1` and event UIDL generated by `UidlRequestHandler`.
- `HOST_PROTOCOL webview.dom` with `flowClientResourceLoaded=true`, `flowClientResourceModuleLoaded=true`, `flowClientBootstrapExported=true` and `flowClientBootstrapInitialized=true`.
- `HOST_PROTOCOL webview.snapshot` writes `.build/vaadin-desktop-host-swift/window-snapshot.png`, whose rendered state shows `Clicks: 1`.

Inspect the sample trace:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
```

The inspector fails with exit code `2` if the required Flow IPC message types or key markers are missing.

## Current Limitations

This is not the final complete Vaadin Flow transport yet:

- Bootstrap, push and event patch payloads include real Vaadin UIDL `changes`, and the WebView applies those changes for the smoke app subset. Verification resync includes Vaadin's real `resynchronize=true` signal and rebuilds through the local tree fallback.
- The WebView shell is a minimum UIDL applier, not the full Flow client engine.
- Flow client resource loading currently imports `FlowBootstrap.js` and calls `FlowBootstrap.init`, but does not call `FlowClient.init` or wire the GWT client engine into UIDL consumption.
- Only click events are implemented.
- The WebView bridge now sends Flow-client-shaped UIDL RPC for click events, but full Flow client event collection is not implemented.
- `VaadinSession` and `UI` are desktop-shaped; Flow IPC bootstrap now carries `Vaadin-Security-Key`; event and resync envelopes carry the key back to Java; and envelope `sessionId`/`uiId`, `csrfToken`/security key, `syncId` and `clientId` are checked before UIDL RPC dispatch. Full resync semantics are not complete.
- Push covers one server-initiated UIDL update in the smoke app, but it is not a full Flow push implementation with background scheduling, ordering guarantees or reconnect semantics.
- Notification handling is trace-only; clipboard write is backed by native `NSPasteboard`.
- Upload/download, full frontend resource graph execution, production bundle and dev mode are not implemented for `dev-ipc`.

The value of this slice is that the core demo path can run without a localhost HTTP server and exposes the real host/WebView/Java IPC contract that the later UIDL adapter must satisfy.
