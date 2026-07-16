# Framework Architecture

vatori is a Vaadin desktop framework. Application code keeps the Vaadin Flow
Java programming model; a platform native host owns the system WebView, native
capabilities, process lifecycle and packaging boundary.

## Ownership

```text
CLI/build selects platform host
  -> macOS AppKit + WKWebView host
  -> starts Java application as a sidecar
  -> DesktopLauncher starts the host-declared UI transport adapter
  -> both processes communicate over VFIPC1
```

Java does not select a WebView implementation and does not fall back to a browser,
headless runner or embedded host. Running an application JAR without a valid
sidecar transport fails with a CLI-oriented message.

## Flow IPC

`dev` is the primary architecture:

```text
application standard Vaadin production webapp
  -> WKWebView local resource scheme
  -> document-start opaque request/push bridge
  -> VFIPC1 method, URL, headers and body bytes
  -> DesktopVaadinRequest / DesktopVaadinResponse
  -> VaadinService.handleRequest()
  -> Vaadin request-handler chain
```

Vaadin owns the production bundle, Flow client, bootstrap, UIDL, component event
and rendering semantics. The native host and browser bridge do not parse UIDL or
implement a parallel component runtime.

Every browser request reaches `VaadinService.handleRequest()`. Desktop-specific
request handlers compensate only for missing servlet infrastructure:

- classpath resources are served from the packaged application classpath;
- `StreamResource` downloads bypass an upstream servlet request cast;
- Upload and StreamReceiver paths remain with Vaadin's standard stream handler.

Opaque request and response streaming uses bounded chunks, credit and
cancellation. Flow push uses the standard client-facing Atmosphere surface and
Flow's server-side push writer while carrying UIDL opaquely.

## Compatibility HTTP

`dev-compat` starts the same native host and Java application model, but the
WebView loads a random `127.0.0.1` Spring Boot/Vaadin URL. Servlet, HTTP and
WebSocket behavior remains available for integrations that require a normal web
application context.

Compatibility is only a UI transport difference. Window, clipboard, notification,
file dialog and other native commands still use the shared VFIPC1
`command`/`capabilityResult` path and return real host results.

## Sidecar Contract

`DesktopLauncher.launch(...)` is a blocking `void` entry. It requires exactly
one transport declared by the native host:

- `flow-ipc`: starts a non-web Spring context and the desktop Vaadin adapter;
- `compatibility-http`: starts the embedded web server and publishes its loopback
  URL in `ready`.

Both modes use one stdin reader, one synchronized stdout writer and one native
bridge. The Java `ready` message owns application name, metadata and main-window
id/title/size. The host validates that descriptor before creating a window.
Window close, host shutdown and stdin EOF terminate the control loop and close
the Spring context.

## Multi-Window And Recovery

For Flow IPC, the host assigns an immutable window/session/UI scope to each
WKWebView. Java reuses one initialized Vaadin service and creates isolated
sessions per scope. Requests, push and native capability results must match the
scope before delivery; closing one window releases only its state.

The macOS host can perform bounded sidecar restart while retaining host-owned
windows. A recovered sidecar creates new Spring, VaadinSession and UI state; only
native window identity survives. Durable application state must live outside
process-local UI state.

## Build Boundary

The CLI invokes Vaadin's production frontend build and packages
`META-INF/VAADIN/webapp` into the selected native host. `vaadin-desktop-flow`
consumes requests from that bundle but does not build or stage frontend output.
Default host builds exclude repository acceptance sources and resources.

Current implementation is macOS-only. Windows and Linux hosts are future
implementations of the same ready, lifecycle, opaque transport and capability
contract; they are not represented by placeholder modules.

## Explicit Limits

- Flow IPC product parity is broader than a smoke shell but not complete across
  every Vaadin component, focus, accessibility and browser edge case.
- Dev-mode/Vite over IPC is not implemented; `dev` currently uses a production
  bundle.
- Servlet filters and servlet-backed security belong to compatibility mode until
  a desktop-specific contract exists.
- Native installers, signing, updates and full cross-platform hosts remain open.
