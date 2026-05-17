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

Status: first minimum runtime slice implemented, server-side UIDL writer integrated into `dev-ipc` payloads, WebView-produced UIDL RPC click dispatch wired, full Flow client UIDL consumption still pending.

- Audit Vaadin Flow event and patch internals.
- Extract a minimal adapter around bootstrap, event dispatch and UIDL/patch response.
- Run one route without servlet request/response in the event path.
- `dev-ipc` now runs `test-apps/flow-over-ipc-smoke-app` without localhost HTTP in the core event/patch path.
- `bash bin/uidl-adapter-smoke` now verifies that a desktop-shaped session/UI can drive Vaadin's real `UidlWriter` and produce UIDL `changes`/`syncId`.
- `bash bin/uidl-rpc-smoke` now verifies that a real Flow event RPC can pass through `UidlRequestHandler`/`ServerRpcHandler` and produce a listener UIDL patch without a real HTTP socket.
- `dev-ipc` bootstrap/patch envelopes now include `payload.uidl`; bootstrap comes from `UidlWriter`, and WebView-produced click RPC patches come from `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap/event UIDL `changes` are consumed by a minimum WebView applier.

First issue candidates:

- Replace the minimum repository-local UIDL applier with Flow client engine UIDL consumption.
- Extend `DesktopVaadinRequest` to enforce session id, UI id, CSRF token and sync ids from Host Protocol.
- Keep push out of scope until request/response UIDL works without HTTP sockets.

## v0.3: Capability Gateway

- Add capability manifest.
- Enforce window/source/user-gesture checks.
- Implement notifications, clipboard and file dialog through real native APIs.

## v0.4: Packaging

- Add jlink/jpackage benchmark.
- Package at least one smoke app.
- Document app size, startup and memory.
