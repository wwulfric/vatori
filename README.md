# vatori

vatori is a Vaadin desktop framework. It preserves the Java Vaadin Flow
programming model while a platform native host owns the system WebView, native
capabilities, lifecycle and packaging boundary.

The primary runtime loads the application's standard Vaadin production bundle
and carries browser requests opaquely to Vaadin's request-handler chain over IPC.
The host and bridge do not implement a custom Flow client or parse UIDL.

## Quick Start

Requirements:

- JDK 21+
- Maven 3.9+
- macOS with the Swift toolchain

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

Use the optional Spring Boot adapter when a Servlet web application context is
required:

```bash
bash bin/vaadin-desktop dev-compat test-apps/vaadin-desktop-smoke-app
```

Build the application and current platform host:

```bash
bash bin/vaadin-desktop package examples/counter-app
```

Both modes build the standard production bundle. `dev` stages it for local
WKWebView loading; `dev-compat` lets Spring serve the same application from a
random `127.0.0.1` URL. Both use the same bidirectional sidecar channel for window
lifecycle and real native capability results. Running an application JAR directly
without a native-host transport fails fast.

## Application Entry

The default runtime is pure Vaadin Flow. Applications register routes explicitly:

```java
public class MyDesktopApp {
    public static void main(String[] args) {
        DesktopLauncher.launch(
                DesktopApplication.builder("My App").build(),
                DesktopFlowApplication.builder(MainView.class).build(),
                args);
    }
}
```

`DesktopApplication` supplies metadata and the main window descriptor consumed
by the native host. Routes remain ordinary Vaadin `@Route` components. Spring
applications add `vaadin-desktop-spring-boot` and use `SpringDesktopLauncher`;
only those applications can use `dev-compat`.

## Verification

Run the central non-window checks:

```bash
bash bin/verify-smoke
```

The script is the source of truth for its subchecks. Real-window automation,
trace inspection and generated artifact handling are documented in
[Acceptance Tools](docs/acceptance-tools.md).

Do not interpret the current validation surface as complete Vaadin product
parity. Current runtime boundaries are maintained in
[Flow over IPC Runtime](docs/flow-over-ipc-runtime.md), and longer-term work is
tracked in [Roadmap](docs/roadmap.md).

## Repository

The stable mental model and code placement rules live in
[Repository Structure](docs/repository-structure.md). The main ownership split is:

- `vaadin-desktop-core`: public desktop application and capability API.
- `vaadin-desktop-ipc`: sidecar envelope and serialization primitives.
- `vaadin-desktop-flow`: pure Vaadin sidecar runtime and transport adapter.
- `vaadin-desktop-spring-boot`: optional Spring DI and compatibility HTTP adapter.
- `native-hosts/macos`: production AppKit/WKWebView host.
- `examples`: readable developer applications.
- `test-apps`: deterministic validation applications.
- `test-support`: repository-only acceptance automation and inspectors.

## Documentation

- [Getting Started](docs/getting-started.md)
- [Repository Structure](docs/repository-structure.md)
- [Framework Architecture](docs/framework-architecture.md)
- [Flow Integration](docs/flow-integration.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [Sidecar IPC Protocol](docs/ipc-protocol.md)
- [Native Host](docs/native-host.md)
- [Acceptance Tools](docs/acceptance-tools.md)
- [Repository Hygiene](docs/repository-hygiene.md)
- [Roadmap](docs/roadmap.md)
