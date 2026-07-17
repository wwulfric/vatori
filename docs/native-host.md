# Native Host

`native-hosts/macos` is the current platform host. It uses AppKit and WKWebView
and is selected by the Java CLI/build path, never by application runtime code.

## Bundle Contract

The platform builder stages:

```text
VaadinDesktopHost.app/Contents/Resources/
  VatoriHost.json
  Sidecar/application.jar
  FlowWebApp/
  FlowIpcOpaqueHttpBridge.js
  desktop-capabilities.json
```

`VatoriHost.json` contains manifest version, transport, bundle-relative resource
paths and sidecar recovery settings. Absolute paths, empty segments and `..` are
rejected. The app can be moved out of the source repository after staging.

Swift does not receive command-line module or build arguments. It never reads
Maven, a repository root or `pom.xml`, and always launches the bundled sidecar
with `java -jar Sidecar/application.jar`. A system Java 21 runtime is currently
required.

## Responsibilities

The host owns:

- launch manifest and Java `ready` validation;
- sidecar process, stdin/stdout channel and termination;
- native windows and WKWebView configuration;
- packaged production resources and opaque Flow IPC bridges;
- window-scoped capability manifest checks;
- AppKit clipboard, windows and dialogs, UserNotifications, file transfer;
- compact tracing and bounded sidecar recovery.

It does not own Vaadin routes, component semantics, UIDL parsing, Maven builds or
a custom renderer.

## Transports

- `flow-ipc` requires `FlowWebApp` and carries browser requests through the
  opaque bridge. It supports public route-aware multiple windows.
- `compatibility-http` requires a loopback `applicationUrl` from the optional
  Spring adapter and remains single-window.

Both modes receive application and main-window metadata from Java `ready`. Both
use `VFIPC ` framing with numeric envelope version `1` for lifecycle and native
command results.

## Production And Acceptance

Production sources and resources live only under `native-hosts/macos`. The
explicit builder under `test-support/native-host-macos-acceptance` composes an
automation overlay for DOM inspection, snapshots and fault injection. Production
Swift sources do not recognize acceptance paths or flags.

Applications should build through:

```bash
bash bin/vaadin-desktop package examples/counter-app
```

The lower-level platform builder receives a prebuilt JAR and webapp from the Java
CLI; it is not an application-facing command.

## Lifecycle

The host waits for valid `ready` before opening a window. Closing a window sends
`windowClosed`; closing the final window shuts down the sidecar. Host termination
sends `shutdown`, and Java treats stdin EOF as terminal.

Flow IPC supports bounded process restart. Existing native windows reload after
replacement `ready`; Java process-local state is recreated.

`HOST_PROTOCOL` lines are acceptance traces, not wire framing. Logs and
screenshots under `.build/native-hosts/macos` are generated artifacts.

## Limits

The current host is macOS-only and uses ad-hoc signing for development. Embedded
JREs, installers, notarization, updates and Windows/Linux hosts remain future
work.
