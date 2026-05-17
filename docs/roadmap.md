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

Status: first minimum runtime slice implemented, server-side UIDL writer integrated into `dev-ipc` bootstrap/push payloads, Flow client XHR click and verification resync dispatch wired over IPC, FlowBootstrap and FlowClient resource loading/import/init over IPC, and visible Flow client ownership verified for bootstrap, click-event patches, one server push and one verification resync.

- Audit Vaadin Flow event and patch internals.
- Extract a minimal adapter around bootstrap, event dispatch and UIDL/patch response.
- Run one route without servlet request/response in the event path.
- `dev-ipc` now runs `test-apps/flow-over-ipc-smoke-app` without localhost HTTP in the core event/patch path.
- `bash bin/uidl-adapter-smoke` now verifies that a desktop-shaped session/UI can drive Vaadin's real `UidlWriter` and produce UIDL `changes`/`syncId`.
- `bash bin/uidl-rpc-smoke` now verifies that a real Flow event RPC can pass through `UidlRequestHandler`/`ServerRpcHandler` and produce a listener UIDL patch without a real HTTP socket.
- `dev-ipc` bootstrap/push/patch/resync envelopes now include `payload.uidl`; bootstrap and one server-initiated push come from `UidlWriter`, and WebView-produced click plus verification resync RPC patches come from `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, click-event UIDL, push UIDL and verification resync UIDL are consumed by the visible Flow client engine.
- `dev-ipc` bootstrap now advertises Vaadin `flow-client` classpath resources, and the WebView requests/imports/initializes `FlowBootstrap.js` and `FlowClient.js` over IPC as the first no-HTTP frontend resource loading slice.

First issue candidates:

- Extend `DesktopVaadinRequest` to enforce full resync state from Host Protocol; minimum Flow IPC `sessionId`/`uiId`, `csrfToken`/security key, `syncId` and `clientId` preflight is already in place.
- Replace the one-shot server-initiated push and verification-only resync with a real Flow push queue, ordering model and reconnect/resync path.
- Broaden Flow client event/resource coverage beyond the constrained smoke-app component set.

## v0.3: Capability Gateway

- Add capability manifest.
- Enforce window/source/user-gesture checks.
- Implement notifications, clipboard and file dialog through real native APIs.

## v0.4: Packaging

- Add jlink/jpackage benchmark.
- Package at least one smoke app.
- Document app size, startup and memory.
