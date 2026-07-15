# Framework Architecture

vatori is a Vaadin-based desktop framework. The long-term shape is Flow over IPC:
standard Vaadin Flow Java views run in a Java sidecar, while a native host owns the
system WebView, lifecycle and platform capabilities.

## Compatibility Mode

```text
Vaadin Flow view
  -> vaadin-desktop-flow
  -> embedded Spring Boot / Vaadin compatibility adapter
  -> local loopback URL
  -> native host
  -> system WebView
```

`dev` and `dev-swift` are real desktop host paths, but the Flow UI transport still
uses local HTTP/Servlet/WebSocket assumptions. Keep this mode explicitly named as
compatibility mode.

## Flow IPC Runtime

```text
Vaadin Flow view
  -> DesktopLauncher / StandardFlowIpcRuntime
  -> Vaadin request-handler chain
  -> opaque HTTP-shaped VFIPC1 exchanges
  -> Swift/AppKit/WKWebView host bridge
  -> standard Vaadin production webapp and Flow client
```

`dev-ipc` removes localhost HTTP from the tested bootstrap, event, push, patch and
resync path. Its default transport architecture is:

```text
standard Vaadin production webapp
  -> WKWebView local static-resource scheme
  -> opaque XMLHttpRequest / Fetch bridge
  -> VFIPC1 method / URL / headers / body bytes
  -> Vaadin request-handler chain + classpath static resources
```

Vaadin remains responsible for the production frontend,
Flow client and UIDL semantics. The framework-specific browser code only needs to
adapt the application origin and carry opaque request/response bytes; it does not
recreate a Flow client or component runtime. The production webapp is built before
launch and copied into the native app resources;
common browser BodyInit forms, bounded request/response streaming, backpressure,
Fetch/XHR cancellation, repeated response headers, cookie continuity and
classpath static resources are covered by focused contracts. A narrow Atmosphere
API shim carries standard Flow push UIDL over IPC;
it does not parse UIDL or maintain component state. Server push uses Flow's
`PushConnectionFactory` and `UidlWriter`, and in-process disconnect/reconnect is
verified. A repository-only WKWebView driver verifies a representative
framework-shaped workflow through the standard Flow client, including field and
button events, server patches, push recovery and native command results. The
Java runtime accepts only opaque bridge and lifecycle envelopes. There is no
second Flow client, typed renderer or resource-staging fallback in the product
path.

On an unexpected sidecar exit, this path keeps the host-owned NSWindow and
WKWebView, terminates outstanding browser exchanges, performs a bounded Java
restart and reloads the same production webapp after the new sidecar is ready.
The Java process, `VaadinSession` and UI are replaced; only host-owned native
window identity is retained. Durable application state must therefore live
outside process-local UI state.

For multiple windows, the native host assigns an immutable
`windowId`/`sessionId`/`uiId` scope to each WKWebView and routes every response,
push and capability result to the matching context. Java reuses one initialized
`VaadinService` but creates an independent `VaadinSession` and UI per scope.
Request ids are scoped by window, push sequences are scoped by session, and
closing one window releases only that session.

Current verified boundaries include:

- Swift owns the Java sidecar lifecycle and opens the packaged production webapp.
- Java starts the application's Spring context without a web server, registers
  its annotated Flow routes and delegates component creation to Vaadin's
  `SpringInstantiator`, including Vaadin UI/session scopes.
- Bootstrap, click/change events, ordered pushes and recovery resync carry real
  server-side UIDL.
- The visible Flow client engine owns the rendered surface for the verified path.
- The framework validation app exercises ordinary Flow APIs across common input,
  navigation, data, overlay, rich text, theme and authentication surfaces.

Current explicit non-goals and blockers:

- Servlet filters, servlet-backed Spring Security and other beans that require a
  `WebApplicationContext` are compatibility-mode integrations; the no-HTTP
  runtime deliberately excludes Vaadin's servlet auto-configuration.
- Full current app generated chunk parity is not complete.
- Full production owner import/reusable side-effect parity is not complete.
- Dev-mode/Vite execution is not complete.
- Full Grid virtualization, Upload/Download retry and resume, overlay
  keyboard/focus, theme system, cross-process application-state restoration and
  installer packaging remain open.

Streaming extends the generic transport rather than the Flow client: bounded
opaque chunks map onto Servlet input/output streams with receiver credit and
cancellation. Multi-window routing likewise stays outside Flow semantics through
host-assigned identities and a Java session/UI registry. Neither concern may
introduce request-type branches for bootstrap, UIDL, Upload or Download in the
native host.

## Modules

- `vaadin-desktop-core`: public Java desktop API.
- `vaadin-desktop-flow`: compatibility launcher plus the standard-bundle opaque
  Flow IPC runtime and adapter code.
- `vaadin-desktop-ipc`: protocol envelopes and trace support.
- `vaadin-desktop-host`: Java host abstractions, explicit compatibility host and
  native bridge support.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host.
- `test-support/vaadin-desktop-host-swift-acceptance`: repository-only Swift and
  WKWebView automation support for explicit native-host acceptance profiles.
- `vaadin-desktop-devtools`: trace inspector and acceptance diagnostics.
- `vaadin-desktop-cli`: developer command entry.
- `examples/*`: developer-shaped examples.
- `test-apps/*`: validation fixtures and framework-shape smoke apps.

The macOS host follows the same dependency direction. `vaadin-desktop-host-swift`
contains only the ordinary host and standard-bundle bridge. Repository acceptance
builds explicitly add the small `VaadinDesktopHostAcceptanceSupport` automation
overlay from `test-support/vaadin-desktop-host-swift-acceptance`; normal builds
neither compile nor package those sources.

## Relationship to Tauri and Electron

Like Tauri, the intended shape is a small native host plus system WebView plus a
backend runtime. Unlike Electron, vatori does not bundle Chromium. The app model
remains Vaadin Flow Java views instead of a JavaScript-first UI.

## Relationship to Vaadin Flow

The compatibility path uses real Vaadin Flow through `vaadin-spring-boot-starter`.
The IPC path progressively isolates HTTP/Servlet/WebSocket assumptions behind
adapter boundaries while preserving Flow's server-side programming model.

Architecture decisions should prefer adapter boundaries that keep Flow semantics
visible and testable instead of one-off fixture behavior.
