# Framework Architecture

vatori is a Vaadin desktop framework. Application code keeps the Vaadin Flow
Java programming model; a platform native host owns the system WebView, native
capabilities, process lifecycle and packaging boundary.

## Ownership

```text
Java CLI builds the application and selects the platform builder
  -> stages Sidecar/application.jar, webapp source and VatoriHost.json
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

## Vite Development Frontend

`dev-vite` keeps the same Flow Java runtime and opaque request/push bridge while
the CLI prepares Vaadin's standard frontend workspace and manages a loopback
Vite process. The host proxies that generic development webapp through its
existing `vatori-app` origin; it does not recognize Vite modules, routes,
components or HMR messages.

Vite HTTP and WebSocket traffic is frontend tooling traffic, not a compatibility
UI transport. Bootstrap, UIDL, component events, resources and push still enter
the sidecar through opaque Flow IPC and `VaadinService.handleRequest()`. The
temporary frontend-build POM may use `vaadin-dev-server` to copy standard Vaadin
development resources, but that dependency is not packaged into the application
sidecar.

The CLI also owns Java source observation and Maven rebuilds in `dev-vite`.
After a successful build it atomically replaces the staged bundle JAR and sends
a versioned lifecycle command over the host process stdin. This CLI-to-host
control path is separate from `VFIPC`: it carries host lifecycle intent, never
Flow requests, UIDL or component semantics. The host knows only that the
already-staged sidecar should be replaced; it does not know the source tree,
module selector, Maven command or JAR producer.

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
UI state. Unexpected-exit recovery and planned development replacement are
separate states: planned replacement does not consume the crash-restart budget,
and a candidate startup, early-exit or readiness failure waits for the CLI to
restore the last ready sidecar asset. The host then starts that stable asset and
reloads retained WebViews only after valid `ready`. Rollback failure is fatal.

## Build Boundary

The Java CLI invokes Maven and either Vaadin's production frontend build or the
managed Vite preparation path, requires one JAR with `Main-Class`, and stages
that JAR plus the selected webapp source into the native host. `dev` and
`package` use a packaged production webapp; `dev-vite` uses a CLI-owned
development origin; `dev-compat` uses the Spring compatibility URL. Default host
builds exclude repository acceptance sources and resources. Application-owned
desktop inputs come from `src/main/desktop`: the CLI validates them, the platform
builder selects the native icon and stages common/platform resource layers, and
the host exposes only the resulting bundle-relative application resource root
to the sidecar.

For development commands, the CLI executes the staged app bundle's native host
process directly and owns its stdout/stderr logs. Normal final-window shutdown
returns zero. Manifest errors, sidecar launch failures, invalid `ready` and
exhausted recovery terminate the host nonzero, which the CLI propagates instead
of relying on LaunchServices completion status. The launch manifest also gives
the first sidecar `ready` a bounded deadline, so a live but unresponsive Java
process cannot leave a development command waiting indefinitely.

In `dev-vite`, the building Maven session exports its selected effective reactor
model to the CLI. Observation therefore follows active profiles, the actual
`-pl ... -am` closure, effective compile source/resource roots, local POMs and
root Maven configuration instead of reparsing component or application POM
semantics. Build-configuration changes also refresh the standard Vaadin Vite
workspace. Compilation or preparation failure is
non-destructive to the running sidecar and staged JAR. A successful rebuild is
staged before the replacement command, so the host never observes a partially
copied JAR. The CLI preserves the last ready JAR until the host returns a
request-correlated success. Candidate failure restores that JAR atomically and
uses the same generic host-control transaction to start it.

Current implementation is macOS-only. Windows and Linux hosts are future
implementations of the same manifest, ready, lifecycle, opaque transport and
capability contracts; placeholder modules are not used.

## Explicit Limits

- Flow IPC validation is broad but not complete product parity.
- Vite frontend HMR and rebuild-driven Java process replacement are implemented;
  failed-replacement JAR rollback is implemented, while JVM in-place class hot
  replacement and the complete upstream Servlet-based Vaadin DevTools server
  lifecycle are not.
- Servlet filters and servlet-backed security require the Spring compatibility adapter.
- Embedded JREs, installers, signing, updates and non-macOS hosts remain open.
