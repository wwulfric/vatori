# Getting Started

## Requirements

- JDK 21+
- Maven 3.9+
- macOS + Swift toolchain for the current system WebView host
- macOS Swift toolchain for the Swift/AppKit host path

If Maven is installed through SDKMAN, the repository script automatically loads SDKMAN.

## Run an Example

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

On macOS, the launcher opens a native WKWebView desktop window and prints a trace similar to:

```text
Vaadin Desktop system WebView host
Application URL: http://127.0.0.1:53123/
Transport: compatibility HTTP. Final Flow over IPC is not enabled yet.
```

The URL is still visible for debugging, but the default host is now a desktop window. Set `VAADIN_DESKTOP_HOST=headless` to run without a window, or `VAADIN_DESKTOP_HOST=browser` to open the default browser.

## Run the Swift Native Host Path

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

This starts the next-stage native host:

- The script compiles `VaadinDesktopHost.app` and starts it through macOS LaunchServices.
- The Swift/AppKit host starts the Java app as a sidecar with `VAADIN_DESKTOP_HOST=headless`.
- The Java app still serves Flow over `127.0.0.1`, but desktop API calls use `VAADIN_DESKTOP_NATIVE_BRIDGE=host-protocol`.
- The host opens a WKWebView window when the Java sidecar prints `Application URL: ...`.
- Host logs are tailed from `.build/vaadin-desktop-host-swift/host.out.log` and `.build/vaadin-desktop-host-swift/host.err.log`.
- The host emits `webview.dom` and writes `.build/vaadin-desktop-host-swift/window-snapshot.png` to verify the real WKWebView rendered the Vaadin view.
- Closing the window terminates the Java sidecar.

Trace lines prefixed with `HOST_PROTOCOL` are host/runtime trace. In compatibility mode, Java-to-host command envelopes are emitted by the sidecar with the `VDP1 ` prefix.

## Run the Flow IPC Runtime Slice

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

This starts the same Swift/AppKit host with `--transport flow-ipc`.

Expected trace characteristics:

- `transport=flow-ipc`
- `compatibilityMode=false`
- Java sidecar emits `VFIPC1` envelopes.
- The WebView loads a local HTML shell through `loadHTMLString`, not `http://127.0.0.1`.
- Button click travels WebView -> Swift host -> Java sidecar -> Vaadin Button listener -> patch -> WebView.
- Bootstrap and event patches include real server-side UIDL under `payload.uidl`; click events are dispatched through `UidlRequestHandler`/`ServerRpcHandler`.
- Desktop API calls travel Java sidecar -> Swift host as `VFIPC1 command`, and host responses appear as `capabilityResult`.

For repeatable real-window smoke verification without global mouse coordinates:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

Inspect the bundled review trace:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
```

Run server-side UIDL adapter checks:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
```

Current limitation: this path uses a minimum repository-local renderer and replace-root patches in the WebView. It now generates Vaadin UIDL and uses the server-side Flow RPC path for click dispatch, but the full Flow client engine does not consume that UIDL yet.

## Run the Independent Smoke App

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

For the long-term host path, use:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

The smoke app is intentionally outside `examples/`. It validates that a developer-style project can depend on the framework modules and run a real Vaadin Flow desktop app.

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
        add(new Button("Notify", event ->
                Desktop.current().notifications().show("My App", "Hello")));
    }
}
```

## Compatibility Mode

Framework v0 still starts an embedded Spring Boot web server on `127.0.0.1`. The macOS desktop window embeds that app in WKWebView, but the transport is still a transition layer, not the final Flow over IPC architecture.

The Swift native path narrows the long-term host/runtime boundary, but Flow bootstrap, UIDL event handling and push are still compatibility HTTP unless a trace or document explicitly says otherwise.

`dev-ipc` is the exception: it is the first no-HTTP runtime slice, now with server-side UIDL generation, but only for `test-apps/flow-over-ipc-smoke-app` and the constrained component/event set documented in `docs/flow-over-ipc-runtime.md`.
