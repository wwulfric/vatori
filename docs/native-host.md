# Native Host

`native-hosts/macos` is the current platform host. It uses AppKit and WKWebView
and is selected by the macOS CLI/build path, never by Java application code.

## Responsibilities

The host owns:

- Java sidecar launch, stdin/stdout channel and termination;
- application `ready` validation and native window creation;
- WKWebView configuration and standard production bundle resources;
- opaque browser request/push bridge installation for Flow IPC;
- native capability manifest checks and command execution;
- window-scoped routing, trace emission and bounded sidecar recovery.

It does not own Vaadin routes, Flow component semantics, UIDL parsing or a custom
renderer.

## Transports

`HostConfig.transport` is a strict enum:

- `flow-ipc` requires a packaged standard Vaadin production bundle and routes
  browser requests through the opaque bridge;
- `compatibility-http` requires a validated loopback `applicationUrl` from the
  sidecar and loads it directly.

The CLI must pass one value. There is no default host selection, compatibility
fallback or independent standard-bundle boolean.

Both modes receive window title and dimensions from the Java `ready` application
descriptor. Both execute desktop commands and return capability results over the
same VFIPC1 channel.

## Production And Acceptance Builds

Production sources live under `native-hosts/macos/Sources` and browser resources
under `native-hosts/macos/Resources`. The build script creates:

```text
.build/native-hosts/macos/VaadinDesktopHost.app
```

The production builder never reads `test-support` or acceptance environment
variables. A separate repository-only builder under
`test-support/native-host-macos-acceptance/bin` composes the production sources
with its automation overlay. Default builds contain no DOM inspection,
screenshots, fault injection or automation flags.

```bash
bash bin/vaadin-desktop-build-macos-host \
  /Users/bytedance/IdeaProjects/vatori examples/counter-app flow-ipc
```

Acceptance builds use the explicit test-support entrypoint documented in
`docs/acceptance-tools.md`.

## Lifecycle

The host waits for a structured `ready` envelope before opening the main window.
Closing a window sends `windowClosed`; closing the final window also shuts down
the sidecar. Host termination sends `shutdown`. Java treats stdin EOF as a
terminal lifecycle event as well.

Flow IPC supports bounded sidecar restart. Outstanding opaque exchanges and push
connections are disconnected, the existing WKWebView reloads after replacement
`ready`, and Java process-local application state is recreated.

## Trace

`HOST_PROTOCOL` lines are compact acceptance traces, not process framing. The
wire channel is exclusively VFIPC1. Default logs live under
`.build/native-hosts/macos`; full traces and screenshots remain generated
artifacts.

Automation can use `VAADIN_DESKTOP_MACOS_NO_ACTIVATE=1` to avoid stealing focus.
Other acceptance-only environment variables are documented in
`docs/acceptance-tools.md` and must not enter production host behavior.

## Limits

The current host is macOS-only and uses ad-hoc local signing for development.
Installer packaging, notarization, update delivery and Windows/Linux hosts are
future work.
