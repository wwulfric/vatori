# Framework Architecture

vatori is a Vaadin desktop framework. Application code keeps the Vaadin Flow
Java programming model; a platform native host owns the system WebView, native
capabilities, process lifecycle and packaging boundary.

## Ownership

```text
Java CLI builds the application and selects the platform builder
  -> stages Sidecar/application.jar, FlowWebApp and VatoriHost.json
  -> macOS AppKit + WKWebView host reads only bundle resources
  -> host executes java -jar Sidecar/application.jar
  -> both processes use VFIPC framing and envelope version 1
```

Java runtime code does not select a WebView. Swift does not know Maven, module
paths, repositories or `pom.xml`. Running an application JAR without a valid
sidecar transport fails with a CLI-oriented message.

## Flow IPC

`dev` is the primary architecture:

```text
application standard Vaadin production webapp
  -> WKWebView local resource scheme
  -> document-start opaque request/push bridge
  -> VFIPC method, URL, headers and body bytes
  -> DesktopVaadinRequest / DesktopVaadinResponse
  -> VaadinService.handleRequest()
  -> Vaadin request-handler chain
```

Vaadin owns the production bundle, Flow client, bootstrap, UIDL, component event
and rendering semantics. The host and browser bridge do not parse UIDL or
implement a parallel component runtime.

Every browser request reaches `VaadinService.handleRequest()`. Desktop-specific
handlers supply classpath resources and adapt the upstream servlet-only
`StreamResource` download path. Upload and StreamReceiver stay with Vaadin's
standard stream handler. Opaque streaming uses bounded chunks, credit and
cancellation; push carries Flow UIDL without interpreting it.

## Flow Runtime

`DesktopLauncher.launch(...)` is the blocking pure Flow entry. It receives a
`DesktopApplication`, an explicit `DesktopFlowApplication` route set and args.
The route set has exactly one root route. Vaadin `DefaultInstantiator` is used
unless an `InstantiatorFactory` is supplied.

The default Flow artifact has no Spring dependency or classpath route scanner.
It owns the Vaadin service, request adapter, sessions, push and shared native
bridge.

## Spring Adapter

`vaadin-desktop-spring-boot` is optional. `SpringDesktopLauncher` supplies Spring
route scanning, `SpringInstantiator`, Vaadin scopes and context lifecycle.

`dev-compat` is available only through this adapter. The same native host loads
a random `127.0.0.1` Spring Boot/Vaadin URL, preserving Servlet, HTTP and
WebSocket integrations. Native commands still use the shared sidecar
`command`/`capabilityResult` path and return real host results.

## Sidecar Contract

Both transports use one stdin reader, one synchronized stdout writer and one
native bridge. Java `ready` owns application name, metadata and the main-window
id, title, dimensions and route. The host validates it before creating a window.
Window close, host shutdown and stdin EOF terminate the control loop. Spring
contexts are created and closed only by the optional adapter.

## Windows And Recovery

Flow IPC supports `Desktop.current().windows().open(...)` and `close(...)`.
Each WKWebView has an immutable window/session/UI scope and may load a distinct
registered route. Java reuses one Vaadin service while isolating sessions,
requests, push and capability results. Compatibility HTTP remains single-window.

The macOS host can perform bounded sidecar restart while retaining native
windows. A replacement process creates new Vaadin sessions and UI state; only
host-owned window identity survives. Durable state belongs outside process-local
UI state.

## Build Boundary

The Java CLI invokes Maven and Vaadin's production frontend build, requires one
JAR with `Main-Class`, and packages that JAR plus the transport-specific staged
resources into the selected native host. `dev`, `dev-compat` and `package` use
the same staged app boundary. Default host builds exclude repository acceptance
sources and resources.

Current implementation is macOS-only. Windows and Linux hosts are future
implementations of the same manifest, ready, lifecycle, opaque transport and
capability contracts; placeholder modules are not used.

## Explicit Limits

- Flow IPC validation is broad but not complete product parity.
- Dev-mode/Vite over IPC is not implemented; `dev` uses a production bundle.
- Servlet filters and servlet-backed security require the Spring compatibility adapter.
- Embedded JREs, installers, signing, updates and non-macOS hosts remain open.
