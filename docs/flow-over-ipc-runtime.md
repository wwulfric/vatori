# Flow over IPC Runtime

Flow IPC is the default development architecture:

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

It runs the application's standard Vaadin production bundle in WKWebView without
a localhost UI server.

## Startup

```text
CLI builds application production webapp and runnable JAR
  -> macOS host packages production webapp
  -> host starts Java sidecar with flow-ipc transport
  -> DesktopLauncher starts non-web Spring context
  -> Java discovers Flow routes and sends structured ready
  -> host validates application/window descriptor
  -> WKWebView loads packaged production webapp
```

The CLI/native-host build owns frontend generation and packaging.
`vaadin-desktop-flow` owns only the Java runtime and transport adapter.

## Browser Transport

At document start, the host installs request and push bridges. They carry URL,
method, headers and body bytes over VFIPC1 but do not inspect bootstrap, UIDL,
component event or patch content.

The standard Flow client remains the only browser runtime. There is no custom
shell, typed component protocol, hidden renderer or application resource
fallback.

Buffered exchanges remain available for focused contracts. The production bridge
uses streaming messages with chunk order, byte counts, credit, cancellation and
repeated response headers.

## Java Runtime

`DesktopLauncher` starts a non-web Spring context, discovers one root route and
the application's annotated route set, and creates a desktop Vaadin service with
Spring instantiation and Vaadin scopes.

Every opaque browser request becomes `DesktopVaadinRequest` and
`DesktopVaadinResponse`, then enters `VaadinService.handleRequest()`. Vaadin's
normal handler chain owns bootstrap, UIDL, heartbeat, stream receivers and other
request semantics. Desktop handlers only supply classpath resources and adapt
the upstream servlet-only StreamResource download path.

## Push

The bridge exposes the Atmosphere calls required by the standard client. Java
validates push id and window/session/UI scope, uses a desktop push connection and
emits opaque UIDL with sequence and resync metadata. Window close and sidecar
replacement release the old push connection.

## Native Capabilities

Flow views use the public desktop API. The shared sidecar native bridge emits a
`command`, blocks the caller and completes only after the host returns a scoped
`capabilityResult`. The WebView is not part of native capability correlation.

The same bridge is used by compatibility HTTP, so validation of clipboard,
window, notification and file-dialog behavior is transport-independent.

## Window Scope

Each WKWebView has immutable `windowId`, `sessionId` and `uiId` values. Java
creates an isolated VaadinSession per scope while reusing the initialized Vaadin
service. Request ids are window-scoped and push sequences are session-scoped.
`windowClosed` validates ownership before cancelling exchanges and closing the
session.

## Sidecar Lifecycle

The Java sidecar has one stdin reader and one synchronized stdout writer.
`shutdown`, final window close and stdin EOF stop the message loop and close the
Spring context. Pending native commands complete with an error when the bridge
closes.

The macOS host supports bounded process restart for Flow IPC. It disconnects
outstanding browser exchanges, retains native windows, starts a replacement
sidecar and reloads the production webapp after `ready`. Spring and Vaadin state
are recreated; durable state is an application responsibility.

## Current Verified Surface

The validation applications and acceptance driver cover representative:

- bootstrap, UIDL event dispatch, server patches and push resync;
- ordinary input, selection, navigation, data and overlay components;
- production classpath/component resources, Avatar resources and
  StreamResource downloads;
- Upload/StreamReceiver paths through Vaadin's standard handler;
- multi-window scope isolation and final shutdown;
- native command/result correlation and sidecar replacement.

This is a bounded product surface, not a claim of complete Vaadin parity.

## Compatibility Boundary

Applications requiring servlet filters, a servlet-backed security chain or
normal WebSocket infrastructure should use:

```bash
bash bin/vaadin-desktop dev-compat examples/counter-app
```

That mode uses loopback HTTP for UI traffic while retaining the same native host,
application descriptor and capability channel.

## Open Work

- dev-mode/Vite over IPC;
- full generated/lazy frontend owner parity;
- complete Grid virtualization and large-file retry/resume;
- complete overlay, keyboard, focus and accessibility behavior;
- cross-process durable application state;
- production installers, signing, updates and non-macOS hosts.
