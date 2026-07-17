# Flow Integration

## Pure Flow

The default application model is:

```text
@Route components + DesktopFlowApplication + DesktopLauncher
```

`DesktopFlowApplication` explicitly lists every route, requires exactly one root
route and optionally accepts a Vaadin `InstantiatorFactory`. The default is
Vaadin `DefaultInstantiator`; the framework does not add a classpath scanner.

The standard production client sends bootstrap, UIDL, event and resource
requests through an opaque bridge. Java converts only the HTTP-shaped boundary
and delegates every request to `VaadinService.handleRequest()`. Desktop handlers
supply classpath resources and adapt the servlet-only `StreamResource` download
path. Upload and StreamReceiver use Vaadin's standard stream handler.

## Spring Boot Adapter

Applications that add `vaadin-desktop-spring-boot` use:

```text
@SpringBootApplication + @Route components + SpringDesktopLauncher
```

For Flow IPC, the adapter creates a non-web Spring context, scans routes,
provides `SpringInstantiator` and registers Vaadin scopes. For
`compatibility-http`, it starts the embedded server on loopback and publishes
the URL through the normal `ready` handshake.

Compatibility changes only UI transport. Desktop API calls use the same native
bridge, command correlation and scoped capability results.

## Multiple Windows

Flow IPC applications may call:

```java
Desktop.current().windows().open(
        DesktopWindow.builder("details").route("details").build());
Desktop.current().windows().close("details");
```

The route must be relative and registered in `DesktopFlowApplication`. Each
window receives an isolated Vaadin session and UI. Compatibility HTTP remains
single-window.

## Validation Application

```bash
bash bin/vaadin-desktop dev test-apps/flow-over-ipc-framework-app
```

This app uses the same public pure Flow entry as external applications while
exposing deterministic state for broader validation.

## Open Boundaries

- desktop-native alternatives for servlet-backed authentication and security;
- dev-mode/Vite frontend workflow over IPC;
- complete component, focus, overlay, accessibility and resource parity;
- embedded JREs, production installers and non-macOS hosts.
