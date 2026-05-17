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

Status: first minimum runtime slice implemented, full UIDL adapter still pending.

- Audit Vaadin Flow event and patch internals.
- Extract a minimal adapter around bootstrap, event dispatch and UIDL/patch response.
- Run one route without servlet request/response in the event path.
- `dev-ipc` now runs `test-apps/flow-over-ipc-smoke-app` without localhost HTTP in the core event/patch path.

First issue candidates:

- Implement a repository-local adapter around `UidlRequestHandler` that returns UIDL JSON instead of writing `VaadinResponse`.
- Wire the compiled `UidlFlowTransportAdapter` into `FlowIpcRuntime`.
- Extend `DesktopVaadinRequest` to carry session id, UI id, CSRF token and sync ids from Host Protocol.
- Keep push out of scope until request/response UIDL works without HTTP sockets.

## v0.3: Capability Gateway

- Add capability manifest.
- Enforce window/source/user-gesture checks.
- Implement notifications, clipboard and file dialog through real native APIs.

## v0.4: Packaging

- Add jlink/jpackage benchmark.
- Package at least one smoke app.
- Document app size, startup and memory.
