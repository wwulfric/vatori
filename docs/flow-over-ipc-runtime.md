# Flow over IPC Runtime

Flow IPC is the default architecture:

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

It runs the application's standard Vaadin production bundle in WKWebView without
a localhost UI server.

## Startup

```text
Java CLI builds a production webapp and unique runnable JAR
  -> platform builder stages both into a macOS app
  -> Swift reads VatoriHost.json and executes java -jar
  -> pure DesktopLauncher initializes explicit Flow routes
  -> Java sends structured ready
  -> WKWebView loads the staged production webapp
```

The CLI owns Maven, frontend generation and staging. `vaadin-desktop-flow` owns
only the pure Java runtime and transport adapter.

## Browser Transport

Document-start bridges carry URL, method, headers and body bytes through `VFIPC `
framing without inspecting bootstrap, UIDL, events or patches. The standard Flow
client remains the only browser runtime. Streaming messages enforce chunk order,
byte counts, credit and cancellation.

## Java Runtime

`DesktopFlowApplication` supplies all route classes and optional instantiation.
There is no default classpath scanner or Spring context. Every opaque request
becomes `DesktopVaadinRequest` and `DesktopVaadinResponse`, then enters
`VaadinService.handleRequest()`. Desktop handlers only supply classpath resources
and adapt the servlet-only `StreamResource` download path.

The optional Spring adapter can provide `SpringInstantiator`, scopes and route
scanning without changing this request path.

## Push And Capabilities

The bridge exposes the Atmosphere calls required by the standard client. Java
validates push id and window/session/UI scope and emits opaque UIDL with sequence
and resync metadata.

Desktop API calls emit a scoped `command` and complete only after a real host
`capabilityResult`. Clipboard, dialogs, notifications, windows and file transfer
are native host operations; notification denial or scheduling failure is
reported as failure.

## Window Scope

`Desktop.current().windows().open(DesktopWindow)` creates a new native window for
a registered relative route. Each WKWebView has immutable `windowId`, `sessionId`
and `uiId`, with an isolated Vaadin session and UI. `close(windowId)` closes the
target and `windowClosed` releases Java state. Compatibility HTTP stays
single-window.

## Sidecar Lifecycle

The sidecar has one stdin reader and one synchronized stdout writer. `shutdown`,
final window close and stdin EOF stop the loop. Pending native commands complete
with an error when the bridge closes.

Bounded process recovery retains native windows but recreates Vaadin session and
UI state. Spring state is also recreated when the optional adapter is present.

## Current Verified Surface

Validation covers representative bootstrap, UIDL, events, server patches, push,
resources, Upload/StreamReceiver, StreamResource, scoped multiple windows,
native command correlation and sidecar recovery. This is a bounded product
surface, not complete Vaadin parity.

## Compatibility Boundary

Servlet filters, servlet-backed security and normal WebSocket infrastructure
require the Spring Boot adapter:

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

## Open Work

- dev-mode/Vite over IPC;
- broader generated and lazy frontend owner parity;
- complete virtualization, overlay, keyboard, focus and accessibility behavior;
- large-file retry/resume and durable process-external state;
- embedded JREs, installers, updates and non-macOS hosts.
