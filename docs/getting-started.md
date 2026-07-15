# Getting Started

## Requirements

- JDK 21+
- Maven 3.9+
- macOS and Swift toolchain for the current native host paths

If Maven is installed through SDKMAN, repository scripts load SDKMAN where needed.

## Run an Example

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

This opens a native desktop WebView window. The Flow UI still uses compatibility
HTTP transport in this mode.

Useful host choices:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

`system-webview` fails fast when the macOS host cannot be started. Use
`VAADIN_DESKTOP_HOST=compatibility`, `headless` or `browser` when you want an
explicit compatibility host. For local development only, opt into fallback with
`VAADIN_DESKTOP_HOST_FALLBACK=compatibility`.

## Swift Native Host Compatibility Path

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

The Swift/AppKit host starts the Java app as a sidecar, waits for the application
URL and opens a WKWebView. This is still localhost Flow UI transport.

## Flow IPC Runtime

```bash
bash bin/vaadin-desktop dev-ipc examples/counter-app
```

Expected trace characteristics:

- `transport=flow-ipc`
- `compatibilityMode=false`
- `standardBundleMode=true`
- Java sidecar emits `VFIPC1` envelopes.
- The WebView loads the app's production bundle from `vatori-app://`, not a
  localhost URL.
- Browser requests and push cross the opaque IPC bridge.
- Vaadin's standard Flow client owns bootstrap, UIDL and component rendering.

The command builds the production frontend and runnable JAR before opening the
native host. A failed production build stops the launch; there is no custom
Flow client shell fallback.

## Framework Validation App

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

Unattended interaction check:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-framework-app .build/vaadin-desktop-host-swift/host.out.log
```

Inspect the current gate list:

```bash
bash bin/flow-ipc-trace-inspect --help
```

Full traces and screenshots are generated artifacts and should stay in `.build/`,
CI artifacts, release assets or external archives.

## Verify

```bash
bash bin/verify-smoke
```

It covers repository hygiene, inspector contracts, the opaque HTTP adapter,
scope isolation and the browser-side request/push bridges. See the script for
the exact current subcommands.

## Create a New App

1. Create a Maven module.
2. Depend on `dev.vatori:vaadin-desktop-flow`.
3. Add `com.vaadin:vaadin-spring-boot-starter`.
4. Create a Spring Boot application class that calls `DesktopLauncher.launch(...)`.
5. Create Vaadin Flow views with `@Route`.

Minimal main class:

```java
@SpringBootApplication
public class MyDesktopApp {
    public static void main(String[] args) {
        DesktopLauncher.launch(MyDesktopApp.class,
                DesktopApplication.builder("My App").build(),
                args);
    }
}
```

Minimal view:

```java
@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        add(new Button("Click me", event -> add(new Span("Clicked"))));
    }
}
```

## Current Limits

Compatibility mode is production-shaped enough for examples, but it still uses
localhost HTTP for Flow UI transport. Flow IPC is the no-localhost path, but it is
currently bounded to verified smoke/framework surfaces and does not yet provide
complete Flow product parity.
