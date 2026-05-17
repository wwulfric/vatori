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
  -> WKWebView local HTML shell
```

The Java sidecar is still launched by Maven `spring-boot:run`, but the application main does not start a web server. It calls `FlowIpcLauncher.launch(...)`, instantiates a real Vaadin Flow route class and waits for IPC messages on stdin.

## Verified Boundaries In This Slice

- Java sidecar lifecycle is owned by Swift/AppKit host.
- WKWebView loads local HTML through `loadHTMLString`, not `http://127.0.0.1`.
- WebView sends `bootstrapRequest` and `event` messages through `WKScriptMessageHandler`.
- Swift host forwards those messages to Java stdin as `VFIPC1` envelopes.
- Java instantiates real Vaadin Flow components and dispatches `Button.click()`.
- Java listener mutates server-side Flow component state.
- Java returns `bootstrapResponse` and `patch` envelopes.
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
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1` is a verification-only hook. It injects a WebView user script that clicks the real DOM button after `bootstrapResponse`, so the event still travels through WKWebView -> Swift host -> Java sidecar -> Vaadin listener -> Swift capability handling -> WebView patch. It avoids unreliable global mouse coordinates on managed macOS desktops.

Verified trace evidence:

- `HOST_PROTOCOL bootstrapRequest` from WebView to Java.
- `HOST_PROTOCOL event` with `action=click` and `componentId=ipc-button`.
- Java `VFIPC1 command` for `window.setTitle`, `notification.show` and `clipboard.writeText`.
- `HOST_PROTOCOL capabilityResult` for `window.setTitle`, `notification.show` and `clipboard.writeText`.
- `HOST_PROTOCOL patch` with `Clicks: 1`.
- `HOST_PROTOCOL webview.snapshot` writes `.build/vaadin-desktop-host-swift/window-snapshot.png`, whose rendered state shows `Clicks: 1`.

Inspect the sample trace:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
```

The inspector fails with exit code `2` if the required Flow IPC message types or key markers are missing.

## Current Limitations

This is not the final complete Vaadin Flow transport yet:

- Patch payload is a repository-local component tree, not Vaadin UIDL.
- The WebView shell is a minimal renderer, not the full Flow client engine.
- Only click events are implemented.
- The adapter does not yet use `UidlRequestHandler`, `ServerRpcHandler` or `UidlWriter`.
- `VaadinSession`, `UI`, CSRF and sync-id semantics are not complete.
- Push is represented by a server-initiated trace message, not a full Flow push implementation.
- Notification handling is trace-only; clipboard write is backed by native `NSPasteboard`.
- Upload/download, frontend resources, production bundle and dev mode are not implemented for `dev-ipc`.

The value of this slice is that the core demo path can run without a localhost HTTP server and exposes the real host/WebView/Java IPC contract that the later UIDL adapter must satisfy.
