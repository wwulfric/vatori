# Agent Instructions

Scope: this file applies to the whole repository.

## Project Goal

This repository is building `Vaadin Desktop Framework v0`: a real framework skeleton for creating desktop-grade Vaadin Flow applications, shaped like Tauri + Vaadin Flow.

The target is not a simple browser wrapper. The long-term direction is Flow over IPC:

- Vaadin Flow Java UI programming model stays intact.
- Servlet/HTTP/WebSocket assumptions are isolated behind adapters.
- A desktop host owns system WebView, native capabilities, lifecycle, packaging and update paths.
- IPC becomes the eventual transport for bootstrap, events, patches, commands and capability results.

## Current State

The current implementation is a real framework skeleton plus a compatibility transport:

- `vaadin-desktop-core`: public Java desktop API.
- `vaadin-desktop-ipc`: IPC envelope and trace primitives.
- `vaadin-desktop-host`: macOS WKWebView fallback, compatibility host and Java native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host spike for the macOS native runtime mainline.
- `vaadin-desktop-flow`: real Vaadin Flow/Spring Boot launcher plus Flow IPC runtime and UIDL adapter slices.
- `vaadin-desktop-devtools`: trace inspector scaffold with Flow IPC v1 checks.
- `vaadin-desktop-cli`: CLI scaffold.
- `examples/*`: real Vaadin Flow example apps.
- `test-apps/vaadin-desktop-smoke-app`: independent developer-style smoke app.
- `test-apps/flow-over-ipc-smoke-app`: independent no-HTTP Flow IPC smoke app for the constrained `dev-ipc` runtime slice.
- `prototypes/phase0-phase1-spike`: earlier non-HTTP mock Flow transport/IPC validation.
- `flow-transport-adapter-spike`: current code-level Vaadin Flow UIDL/event/patch boundary analysis.
- `vaadin-desktop-flow/src/main/java/dev/vatori/desktop/flow/transport`: compiled UIDL adapter slice around `UidlRequestHandler`, `UidlWriter`, `DesktopVaadinRequest`, `DesktopVaadinResponse` and `DesktopFlowRuntimeSession`.

Default `dev` behavior on macOS:

1. Start a real Vaadin Flow/Spring Boot app on `127.0.0.1` with a random port.
2. Start `SystemWebViewDesktopHost`.
3. Compile a temporary Swift/WKWebView `.app` bundle with `/usr/bin/swiftc`.
4. Open a native desktop window that loads the local Vaadin URL.

Important boundary: this is a real desktop WebView host, but the UI transport is still local HTTP/Servlet/WebSocket compatibility mode. Do not describe it as final Flow over IPC.

Long-term host path:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

This runs `vaadin-desktop-host-swift`, which starts the Java app as a sidecar, opens a native AppKit/WKWebView window after the sidecar prints `Application URL: ...`, traces Host Protocol v0 as `HOST_PROTOCOL ...`, reads Java command envelopes prefixed with `VDP1 `, and kills the Java process on window shutdown. It still loads the Flow UI through localhost HTTP compatibility mode.

`dev-swift` launches the generated `.build/vaadin-desktop-host-swift/VaadinDesktopHost.app` via macOS LaunchServices. Logs are tailed from `.build/vaadin-desktop-host-swift/host.out.log` and `.build/vaadin-desktop-host-swift/host.err.log`. For real-window verification, check `HOST_PROTOCOL webview.dom` and `.build/vaadin-desktop-host-swift/window-snapshot.png`; on managed macOS desktops, global screenshots may be masked by a `loginwindow`/MDM overlay.

No-HTTP IPC slice:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

This runs the Swift host with `--transport flow-ipc`, loads a local WKWebView HTML shell and communicates with Java through `VFIPC1` stdin/stdout envelopes. It proves WebView event -> Java Button listener -> desktop command/capabilityResult -> patch -> WebView without localhost HTTP for the smoke app. The Java side emits real UIDL under `payload.uidl`: bootstrap uses `UidlWriter`, and click events are dispatched through `UidlRequestHandler`/`ServerRpcHandler`. The WebView still consumes the repository-local tree patch, so this is not full Flow client engine parity yet.

Repeatable real-window smoke verification can use:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

## Build And Run

Maven is available through SDKMAN on this machine. Source SDKMAN before calling Maven directly:

```bash
source /Users/bytedance/.sdkman/bin/sdkman-init.sh
```

Use the project-local Maven repository to avoid writing to the user's global Maven cache:

```bash
mvn -o -Dmaven.repo.local=/Users/bytedance/IdeaProjects/vatori/.m2/repository -DskipTests compile
```

Run the smoke desktop app:

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

Run the Swift native host spike:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

Run the Flow IPC smoke app:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

Inspect the Flow IPC trace sample:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
```

Verify the server-side UIDL writer adapter:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
```

Host selection:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

Starting the system WebView host opens a macOS UI process and may require sandbox escalation. Stop long-running dev sessions before finishing a turn.

## Local Reference Source Trees

The user has local upstream source checkouts available for study. Treat them as read-only references unless the user explicitly asks to edit them.

Tauri, historical reference only:

```text
/Users/bytedance/RustroverProjects/study/tauri
```

Useful Tauri areas if comparing against its process model:

- `crates/tauri`
- `crates/tauri-runtime`
- `crates/tauri-runtime-wry`
- `crates/tauri-cli`
- `crates/tauri-bundler`
- `crates/tauri-utils`

Vaadin:

```text
/Users/bytedance/IdeaProjects/study/vaadin
```

Useful Vaadin areas:

- `flow/flow-server`
- `flow/flow-client`
- `flow/flow-push`
- `flow/vaadin-spring`
- `platform/vaadin-spring-boot-starter`
- `platform/vaadin-maven-plugin`
- `platform/vaadin-dev-bundle`

When making architecture decisions, prefer checking these local source trees before using web search.

## Documentation Map

- `README.md`: current quick start and status.
- `docs/framework-architecture.md`: current and target architecture.
- `docs/getting-started.md`: developer workflow.
- `docs/flow-integration.md`: how real Vaadin Flow is integrated today.
- `docs/ipc-protocol.md`: protocol envelope and message taxonomy.
- `docs/ipc-protocol-v1.md`: Flow IPC v1 envelope and message taxonomy.
- `docs/flow-over-ipc-runtime.md`: current `dev-ipc` runtime slice and limitations.
- `docs/flow-transport-adapter.md`: current adapter implementation and next UIDL boundary.
- `docs/native-host.md`: current WKWebView host and host roadmap.
- `docs/roadmap.md`: next engineering phases.
- `docs/flow-over-ipc-design.md`: original Chinese design package.
- `docs/flow-over-ipc-implementation.md`: Phase 0/1 prototype notes.

## Project Skills

- `.agents/skills/vaadin-desktop-native-feel`: project-level skill for making Vaadin Desktop apps feel native. Use it when work touches Swift/AppKit host behavior, WKWebView/WebView2 quirks, Vaadin Flow desktop UI conventions, desktop capability APIs, compatibility-mode boundaries or native-readiness reviews.

## Engineering Rules

- Keep compatibility mode explicitly named and documented.
- Do not remove or hide the fact that current Flow still depends on Servlet/HTTP/WebSocket.
- Do not collapse the framework into a one-off example app.
- Keep example apps developer-shaped: they should depend on framework modules like external users would.
- Do not edit the local Tauri or Vaadin upstream source checkouts unless explicitly requested.
- Prefer small, reviewable modules and clear adapter boundaries.
- Use `rg` / `rg --files` for source searches.
- Use `apply_patch` for file edits.
- Do not delete generated or user files unless explicitly requested.

## Near-Term Work

Most valuable next steps:

1. Make `vaadin-desktop-host-swift` the default host once it is stable across local machines.
2. Turn Host Protocol v0 traces into a bidirectional command/result transport.
3. Make the WebView bridge send Flow-client-shaped UIDL RPC payloads directly instead of Java translating `{action, componentId}`.
4. Replace the repository-local WebView tree renderer with Flow client engine UIDL consumption.
5. Add smoke tests for Swift host window launch, sidecar cleanup and desktop capability calls.
