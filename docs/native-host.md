# Native Host

`native-hosts/macos` is the current platform host. It uses AppKit and WKWebView
and is selected by the Java CLI/build path, never by application runtime code.

## Bundle Contract

The platform builder stages:

```text
VaadinDesktopHost.app/Contents/Resources/
  VatoriHost.json
  AppIcon.icns
  Sidecar/application.jar
  FlowWebApp/
  FlowIpcOpaqueHttpBridge.js
  FlowIpcPushBridge.js
  desktop-capabilities.json
  Application/                 optional application-owned resources
```

`VatoriHost.json` contains manifest version, transport, bundle-relative resource
paths, the optional application-resource root, the initial sidecar `ready`
deadline and recovery settings. Recovery defines the restart limit, recovery
`ready` deadline and stable-run duration. Absolute paths, empty segments and
`..` are rejected. The app can be moved out of the source repository after
staging.

The CLI selects an application-provided
`src/main/desktop/macos/AppIcon.icns` when present and otherwise uses the vatori
brand icon as a fallback. Common and macOS-specific application resource trees
are staged beneath `Application/`; the host exposes only that validated bundled
directory to the Java sidecar.

Swift does not receive command-line module or build arguments. It never reads
Maven, a repository root or `pom.xml`, and always launches the bundled sidecar
with `java -jar Sidecar/application.jar`. A system Java 21 runtime is currently
required.

## Responsibilities

The host owns:

- launch manifest and Java `ready` validation;
- sidecar process, stdin/stdout channel and termination;
- native windows and WKWebView configuration;
- packaged or generic loopback-development webapp resources and opaque Flow IPC
  bridges;
- window-scoped capability manifest checks;
- AppKit clipboard, windows and dialogs, UserNotifications, file transfer;
- compact tracing and bounded sidecar recovery.

It does not own Vaadin routes, component semantics, UIDL parsing, Maven builds or
a custom renderer.

For a CLI-managed development webapp only, the host also reads versioned
`VATORI_HOST_CONTROL ` lines from its process stdin. This channel carries generic
host lifecycle commands such as planned sidecar replacement and rollback.
Request-correlated `VATORI_HOST_CONTROL_RESULT ` lines on stdout report
candidate or rollback readiness to the CLI. This channel is separate from
`VFIPC ` and cannot carry browser requests, native capability results or
component-specific data.

## Transports

- `flow-ipc` requires exactly one packaged `FlowWebApp` or CLI-owned loopback
  development origin and carries Flow browser requests through the opaque
  bridge. It supports public route-aware multiple windows.
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
The development source is a generic origin and entry path in the launch
manifest; the host does not recognize Vite, HMR or component semantics.

Compact production tracing is transport-shaped. It retains bounded envelope,
scope, correlation, status, header-name, byte-count and window metadata after
opaque bodies have been redacted. It does not inspect or index bootstrap, UIDL,
component resources, DOM state or acceptance readiness. The acceptance overlay
compacts DOM and fixture diagnostics before passing them to the shared trace
writer; that diagnostic schema is not part of the production host.

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

Flow IPC supervises unexpected exits with a consecutive-failure budget.
Recovery uses exponential backoff capped at two seconds. Existing native windows
reload after replacement `ready`; Java process-local state is recreated. A
successful recovery starts the manifest-defined stable-run deadline. The
failure count resets only if the same sidecar remains ready for that entire
duration; another exit before then consumes the next attempt. Exhausting the
budget is fatal.
Unexpected process recovery and planned development replacement use distinct
host states. Planned replacement is allowed only for the generic development
webapp mode and does not consume the unexpected-exit restart budget. If a
candidate cannot reach `ready`, the host waits for the CLI-owned stable-JAR
restore, starts that asset and reloads retained WebViews only after rollback
`ready`.

CLI development commands execute the bundled host process directly so its exit
status is authoritative. Normal close exits zero. Fatal manifest, sidecar
startup, `ready` validation and recovery-exhaustion paths exit nonzero after
writing available stderr and operational trace diagnostics. The host also
enforces the manifest's initial `ready` deadline and exits nonzero without
creating a window when a live sidecar never completes the handshake.

`HOST_PROTOCOL` lines are acceptance traces, not wire framing. Logs and
screenshots under `.build/native-hosts/macos` are generated artifacts.

## Limits

The current host is macOS-only and uses ad-hoc signing for development. Embedded
JREs, installers, notarization, updates and Windows/Linux hosts remain future
work.
