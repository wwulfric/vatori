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
- Button click, the smoke app's text-input / checkbox `change` events and the `BigDecimalField` / real `TextField` / `Checkbox` Web Component `change` events travel WebView -> Swift host -> Java sidecar -> Vaadin listener -> patch -> WebView.
- Bootstrap, ordered server push, event patches and recovery resync include real server-side UIDL under `payload.uidl`; click, input/checkbox `change`, `BigDecimalField` / `TextField` / `Checkbox` `change` events and recovery resync are dispatched through `UidlRequestHandler`/`ServerRpcHandler`.
- The v1.1 push-ordering smoke emits two ordered server push updates plus a controlled duplicate/late probe; the WebView records delivered/ignored order and triggers explicit `ordering-recovery-duplicate-late` resync.
- Bootstrap advertises Vaadin `flow-client` classpath resources and the v1.4 frontend artifact manifest; the WebView requests and verifies the Flow resources through IPC, echoes artifact family / module specifier / dependency / execution policy metadata in `resourceRequest`, imports `FlowBootstrap.js` and `FlowClient.js` as ES modules, initializes the Flow bootstrap plus GWT client modules, classifies executed vs verified-only resources, stages real frontend artifact bodies as Blob module URLs, imports real component frontend entries, verifies generated/lazy, production and dev-mode package graph evidence, records production owner boundaries through v1.20, records a v1.21 unified frontend resolver contract across Blob staging, `vatori-resource` file-backed loading, verified-only resources, strict blockers and accepted gaps, records a v1.22 Vaadin-shaped generated chunk-file positive execution fixture, records a v1.23 Vaadin-shaped production owner positive runtime fixture, and records a v1.24 frontend pipeline closure decision contract.
- Desktop API calls travel Java sidecar -> Swift host as `VFIPC1 command`, and host responses appear as `capabilityResult`; the Flow IPC smoke app uses `manifest-enforced` permission metadata, includes an intentional manifest deny-path, exercises AppKit file-dialog cancellation and verifies bounded file-transfer download/upload metadata through controlled test paths.

For repeatable real-window smoke verification without global mouse coordinates:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

To check the same runtime against a less smoke-specific Flow project:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

`flow-over-ipc-framework-app` uses ordinary Flow Java APIs for a Button, `BigDecimalField`, `Checkbox`, `Select`, a v1.10 lazy dependency declaration, repeated list updates and manifest-gated desktop capability calls. It is a framework validation target; the strict trace gates still use `flow-over-ipc-smoke-app`.

For an unattended framework interaction check:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

Inspect the bundled review trace:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_8 docs/traces/flow-ipc-v1_8.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9-loadondemand docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9 docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-lazy-dependency docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-production-runtime docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10 docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11-loadondemand-chunk docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11 docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12-loadondemand-chunk-file docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12 docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13-production-owner docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13 docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14-production-owner-import docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14 docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15-production-owner-dependencies docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15 docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16-production-owner-ordering docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16 docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17-resource-scheme docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17 docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18-production-owner-resource-scheme docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18 docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19-production-owner-side-effect docs/traces/flow-ipc-v1_19.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19 docs/traces/flow-ipc-v1_19.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20-production-owner-owned-runtime docs/traces/flow-ipc-v1_20.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20 docs/traces/flow-ipc-v1_20.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21-frontend-resolver-contract docs/traces/flow-ipc-v1_21.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21 docs/traces/flow-ipc-v1_21.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22-generated-chunk-fixture docs/traces/flow-ipc-v1_22.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22 docs/traces/flow-ipc-v1_22.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23-production-owner-positive-fixture docs/traces/flow-ipc-v1_23.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23 docs/traces/flow-ipc-v1_23.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 docs/traces/flow-ipc-v1_24.ndjson
```

Run server-side UIDL adapter checks:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

Current limitation: this path is still a constrained smoke-app runtime. It now generates Vaadin UIDL, requests and verifies the advertised Flow client resource graph, publishes a frontend artifact manifest with explicit families and blockers, initializes `FlowBootstrap.js` / `FlowClient.js` over IPC, stages and executes representative real frontend artifacts, records production owner boundaries through v1.20, records a v1.21 unified frontend resolver contract, verifies a v1.22 generated chunk-file positive fixture, verifies a v1.23 production owner positive runtime fixture, and records a v1.24 closure decision contract. The v1.12 generated `Flow.loadOnDemand` real-build result is still a blocker at a narrower layer: the request is Flow-client-triggered and mapped to generated-flow-imports, and the adapter proves the current Vaadin build produced a no-op table rather than a `./chunks/chunk-*.js` branch. v1.22 proves the executor path can handle that table shape when a repository fixture supplies the chunk file, but it is not current app/build generated chunk parity. The v1.18-v1.24 production/resource result is also still a boundary: root/dependency loading is custom-scheme/file-backed, root Blob fallback is not accepted, owner module import is separated from reusable side effects, owner-first runtime reuse still falls back for the real production owner because a real owner artifact is missing, v1.23 only proves that the infrastructure succeeds when a fixture exposes the required owner contract, and v1.24 keeps product parity pending while choosing the next product path. The Flow client container is promoted to the visible WebView surface, and click/change events plus one ordering-recovery resync go through the server-side Flow RPC path with UIDL patches consumed by the visible Flow client engine. Two ordered server pushes are also delivered into that engine, and one duplicate/late probe is ignored before resync. The hidden repository-local UIDL applier remains only as a diagnostic fallback until broader Flow event coverage, real app/build generated chunk-file parity, production owner import/side-effect success, dev frontend execution and durable push/reconnect/resync semantics are implemented.

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

The Swift native compatibility path narrows the long-term host/runtime boundary, but Flow bootstrap, UIDL event handling and push are still compatibility HTTP there. Use `dev-ipc` for the no-localhost bootstrap/push/event/patch/resync runtime slice.

`dev-ipc` is the exception: it is the first no-HTTP runtime slice, now with server-side UIDL generation, visible Flow client engine ownership for bootstrap, click-event patches, native input/checkbox change-event patches, real BigDecimalField/TextField/Checkbox event patches, ordered pushes and recovery resync, v1.5-v1.20 frontend staging/generated-lazy/production-owner evidence, v1.21 unified frontend resolver contract evidence, v1.22 generated chunk-file positive fixture evidence, v1.23 production owner positive fixture evidence and v1.24 closure decision evidence. Strict v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9/v1.10/v1.11/v1.12/v1.13/v1.14/v1.15/v1.16/v1.17/v1.18/v1.19/v1.20/v1.21/v1.22/v1.23/v1.24 gates are centered on `test-apps/flow-over-ipc-smoke-app`; `test-apps/flow-over-ipc-framework-app` has a separate framework-shape gate for generic viewport promotion, first meaningful paint and ordinary Flow project interaction.
