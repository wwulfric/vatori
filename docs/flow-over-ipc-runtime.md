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
- Desktop API calls emit `VDP1` command envelopes and are traced by the host.

## Current Limitations

This is not the final complete Vaadin Flow transport yet:

- Patch payload is a repository-local component tree, not Vaadin UIDL.
- The WebView shell is a minimal renderer, not the full Flow client engine.
- Only click events are implemented.
- The adapter does not yet use `UidlRequestHandler`, `ServerRpcHandler` or `UidlWriter`.
- `VaadinSession`, `UI`, CSRF and sync-id semantics are not complete.
- Push is represented by a server-initiated trace message, not a full Flow push implementation.
- Upload/download, frontend resources, production bundle and dev mode are not implemented for `dev-ipc`.

The value of this slice is that the core demo path can run without a localhost HTTP server and exposes the real host/WebView/Java IPC contract that the later UIDL adapter must satisfy.
