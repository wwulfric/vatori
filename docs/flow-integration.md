# Flow Integration

## Pure Flow

The default application model is:

```text
@Route components + DesktopFlowApplication + DesktopLauncher
```

`DesktopFlowApplication` explicitly lists every route, requires exactly one root
route and optionally accepts a Vaadin `InstantiatorFactory`. The default is
Vaadin `DefaultInstantiator`; the framework does not add a classpath scanner.

Pure Flow applications can tune the generic Servlet multipart adapter without
depending on an Upload component or compatibility HTTP transport:

```java
DesktopFlowApplication application = DesktopFlowApplication
        .builder(MainView.class)
        .multipartConfig(DesktopFlowMultipartConfig.builder()
                .location(Path.of("/application-owned/temp"))
                .memoryThresholdBytes(64 * 1024)
                .maxFileSizeBytes(256L * 1024 * 1024)
                .maxRequestSizeBytes(512L * 1024 * 1024)
                .maxPartCount(1_000)
                .maxPartHeaderSizeBytes(8 * 1024)
                .build())
        .build();
```

The default location is `java.io.tmpdir`, the memory threshold is 64 KiB and
adapter-level file/request limits are disabled. The part-count and part-header
limits retain bounded defaults. Each request gets an isolated temporary
directory, and the adapter removes it after request completion or failure.

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
bash bin/vaadin-desktop dev-vite test-apps/flow-over-ipc-framework-app
```

This app uses the same public pure Flow entry as external applications while
exposing deterministic state for broader validation. `dev-vite` changes only
frontend resource delivery and HMR; Flow requests continue through the same
opaque adapter.

## Open Boundaries

- desktop-native alternatives for servlet-backed authentication and security;
- Java class hot replacement and complete Vaadin DevTools server integration;
- complete component, focus, overlay, accessibility and resource parity;
- embedded JREs, production installers and non-macOS hosts.
