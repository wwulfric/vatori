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
- native windows, standard application/editing menus and WKWebView configuration;
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

## Standard Editing Commands

The host installs an application menu with Quit and an Edit menu with Undo,
Redo, Cut, Copy, Paste and Select All. Their standard keyboard equivalents use
nil-target AppKit actions and automatic menu validation, so the current responder
chain selects the editing owner, including a WebView or native sheet text field.
The host does not select DOM nodes, assign input values or translate commands
into component-specific IPC. Quit uses the normal AppKit termination path below.

These are native user-editing commands, not a new Java menu API or a replacement
for the separately permission-checked `clipboard.readText`/`clipboard.writeText`
capabilities. Custom application menus and broader command customization remain
separate product boundaries.

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

AppKit clipboard results preserve the platform distinction between no string
value and an explicit empty string: `clipboard.readText` returns
`textPresent=false` without `text` for the former, and `textPresent=true` with
`text=""` for the latter. `clipboard.writeText` reports the actual
`NSPasteboard.setString` Boolean as both `success` and `written`. Full-detail
tracing removes clipboard write arguments and read-result text while retaining
only length and redaction metadata; the correlated wire envelope sent to Java
is unchanged.

`notification.show` reports OS authorization separately from capability-manifest
denial, and reports `scheduled=true` only after `UNUserNotificationCenter.add`
completes without an error. This acknowledges OS acceptance, not banner
presentation or user delivery. The host does not install a foreground
notification presentation delegate. The independent foreground-delivery
acceptance leaves this policy unchanged and checks a real retained Notification
Center record during an uninterrupted active/key interval; it does not infer a
visible banner from that record or change `SCHEDULED` semantics.
Full-detail command/result tracing
recursively masks notification title, body, body preview, identifier and native
error values, including nested/case-varied keys and caller-forged redaction
markers; malformed non-object arguments are hidden as a whole. Stable error
codes, permission audits and correlation/scope metadata remain visible. These
trace transformations do not alter the wire result delivered to Java.
After an authorization API error, the host reads notification settings once.
Only a newly observed `denied` state produces the existing OS-denied result;
other states retain the API error. Both asynchronous callbacks check the active
operation token on the main queue. This does not retry authorization, schedule
a request, infer a user action from an error code, or guarantee future OS state.
Remaining authorization and scheduling API failures include a fixed
`nativeErrorDomain`
token (`UNErrorDomain` or `other`) and integer `nativeErrorCode`. Full and compact
traces retain these safe diagnostics; non-allowlisted domain values and
non-integer/Boolean codes are redacted, including nested/case-varied keys.
Notification `userInfo` is also masked and is not emitted by the failure helper.
Neither diagnostic establishes a user permission choice, a delivery result or
retry safety.

When an outstanding notification's add callback arrives after invalidation or
identifier replacement, the host requests pending/delivered removal again for
the captured old identifier and drops the callback without settling a newer
operation. These asynchronous OS removals have no completion acknowledgement;
they do not establish atomic revocation or undo presentation/sound. Previously
settled notifications are outside this outstanding-operation cleanup policy.

For `fileDialog.openReadGrant`, the host retains the selected URL and an open
read-only `FileHandle`; it does not return a filesystem path to Java. The
opaque grant is bound to the selecting Flow window's complete scope and current
sidecar generation. Reads are serialized per grant, capped at 64 KiB and
limited to the file length observed when the handle was opened. Explicit
release closes the handle. Owner-window close, sidecar retirement or exit and
host shutdown revoke every remaining grant. If AppKit reports that the URL is
security-scoped, the host balances its one successful start with exactly one
stop, including invalidation racing an in-flight read or release.

Production traces use a host-local numeric diagnostic id for lifecycle events;
they never record the opaque grant id, path, basename or file bytes. The
current development host remains ad-hoc signed and does not ship App Sandbox
entitlements, so this owner/generation lifecycle is not evidence of Powerbox or
App Sandbox enforcement. A separate acceptance-only sandbox clone now verifies
the read path with runner-created external files: actual `EPERM` before panel
selection, typed read of the selected file, continued denial of an unselected
sibling, release and post-release token rejection. It uses the unchanged public
grant implementation; it does not enable Sandbox in the default product host.
Persistent bookmarks and recovery of a grant across process replacement are
not implemented. Balancing the host's own scope and closing
its handle do not imply immediate revocation of every Powerbox extension the
OS may have issued to the process.

`fileDialog.saveNewWriteGrant` provides a separate create-new-only write grant.
The real `NSSavePanel` chooses the destination; the host uses exclusive,
no-follow creation with owner-only permissions and never truncates an existing
target. Java receives a private-token facade, not a path. `fileGrant.write`
accepts canonical Base64 blocks of 1–65,536 bytes at the exact next offset;
the host checks the bound independently before decoding and serializes I/O on
the grant's queue. `fileGrant.releaseWrite` closes the descriptor and balances
scope access. The same exact owner/generation checks and lifecycle cleanup
tracker apply, but read and write tokens are not interchangeable.

`fileGrant.cancelWrite` separately authorizes cooperative termination of a write
grant. The main-thread admission fence rejects later blocks, while a locked
signal is polled between OS write attempts. Work already past a checkpoint may
complete, including the whole block. Cancellation never holds that lock across
I/O or concurrently closes a descriptor being written. Cleanup is registered
at admission; its close waits until the in-flight write's exact prefix has
settled on the main thread, including after owner invalidation. The terminal
reply reports the final prefix only after close. This differs from release,
which does not ask an accepted block to stop between attempts.

Writes become visible immediately. An I/O failure reports a known written
prefix and retires the grant; an invalidated or lost reply does not establish
that no bytes were written. Release, owner teardown and cancellation of future
work do not roll back or delete the new file. There is no overwrite, atomic
commit, fsync durability, automatic retry/resume or persistent bookmark promise.
The default host's Sandbox policy remains unchanged; the separate write-grant
acceptance clone uses user-selected read-write instead of read-only entitlement.
See Apple's [Sandbox file-access contract](https://developer.apple.com/documentation/security/accessing-files-from-the-macos-app-sandbox)
for panel-provided access; closing one owned grant does not establish process-wide
revocation of implicit Powerbox access.

Sandbox validation must first establish that the selected JVM and WebKit child
processes can start under the signed host's actual policy and reach a Flow
first paint; Java `ready` or window creation alone is insufficient. Only then
can an external, initially unreadable file selected through a real open panel
demonstrate Powerbox access. A temporary app-local JVM used to isolate these
runtime prerequisites is test material, not an embedded-JRE distribution
contract. Signing the host alone does not establish either boundary.
WebKit's [sandboxed XPC service startup check](https://github.com/WebKit/WebKit/blob/main/Source/WebKit/Shared/EntryPointUtilities/Cocoa/XPCService/XPCServiceEntryPoint.mm)
requires network communication permission even before loading local content.
Validate this prerequisite against the deployed macOS version; a
`com.apple.security.network.client` entitlement grants outbound network access,
not merely internal opaque IPC. It must not be described as an IPC-only right.

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

Normal AppKit termination first fences new host/sidecar work and invalidates
native operations. A host-owned cleanup tracker accounts for queued file-handle
closes, including work held by window executors already removed from the live
window registry. `applicationShouldTerminate` returns `terminateLater`, allowing
the main run loop to settle those closes before replying to AppKit. The drain
has a two-second bound; its completion diagnostic records actual pending and
failure counts, and a timeout does not fabricate successful cleanup. Before
replying YES to AppKit, the host asynchronously retires the owned sidecar:
best-effort shutdown/EOF, one second of channel grace, SIGTERM, one second of
signal grace, then SIGKILL with a two-second actual-exit callback deadline.
Signals are bound to the retained `Process`, PID and generation; repeated
pending requests do not extend deadlines. A successful signal call is not exit
evidence. Only the actual process callback permits `applicationWillTerminate`.
Signal failure or missing exit confirmation cancels that Quit and keeps the
host and process ownership fenced. A later Quit retries retirement only; it
does not restore the already-drained Flow runtime or resend shutdown. AppKit can
subsequently close windows as final teardown without sending new Java work.
Drain startup, deadlines, process-exit delivery and file-close receipts use the
main run loop's default and modal-panel modes, not the main dispatch queue.
This matters when a public `window.close` command closes the final window from
a main-queue callback: AppKit may enter its nested termination loop before that
callback returns. Cleanup remains asynchronous, main-thread-owned and exactly
once, including empty drains and timeout results.
Each sidecar generation has one nonblocking stdin FIFO, bounded to 16 MiB of
retained frame bytes and 256 frames. Admission and generation checks are atomic
but do not wait for pipe capacity; acceptance is not a Java acknowledgement.
Only complete newline-terminated writes emit `written`. A serial write source
preserves byte order, limits each drain pass and reports actual EAGAIN as
backpressure. Its descriptor disables SIGPIPE locally. Overflow, write failure
or ten seconds without write progress fail the entire channel and request fatal
retirement; no partial frame is replayed into a new JVM. Stale writer callbacks
cannot affect a replacement generation. If the process is already observed as
exited when a queued failure callback runs, its actual-exit handler retains
ownership of recovery rather than having the late write error start fatal
retirement. This is not a guarantee for every simultaneous OS exit/write race.
Quit fences admission and requests best-effort shutdown/EOF without waiting for
the writer. If an ordinary frame is partial, remaining bytes are abandoned and
EOF is used instead of inserting shutdown into that frame. Otherwise unsent
frames can be abandoned for the terminal shutdown frame. Descriptor close occurs
only in the dispatch-source cancellation handler. These bounds cover queued
stdin data, not all host allocations or guaranteed application cleanup.
This prevents a child that stops reading stdin from blocking the AppKit thread
or its retirement deadlines. It does not repair an otherwise hung host main
thread. Deadlines are event-loop
scheduled, not hard real-time guarantees. Fatal runtime shutdown uses the same
fencing, drain and actual-exit retirement, then exits with status `1` instead of
reporting an AppKit termination callback. If retirement fails, the host remains
fenced; a later Quit retries retirement and preserves the failure exit status.
The initial-ready-timeout gate covers cooperative and blocked JVM shutdown
hooks before any window is allowed. This is not evidence for every fatal cause,
host crashes, completion of indefinitely blocked file I/O or guaranteed JVM
hook/transaction completion.

Before creating `NSApplication`, starting the sidecar or opening a window, the
host parses `desktop-capabilities.json` exactly once and retains that immutable
snapshot in `HostConfig`. Missing resources and parser-invalid JSON, schema,
permission modes or window entries are startup-fatal. For parser-invalid staged
content, the host writes a compact `capabilityManifest`/`capabilityManifestInvalid`
diagnostic pair and exits with status `2`; it does not continue to command-time
permission checks. The command executor's `manifest-invalid` denial remains a
defensive fail-closed branch rather than the standard startup path.

Each closed Flow IPC window leaves a tombstone for its complete
`windowId`/`sessionId`/`uiId` identity for the lifetime of the host process.
Tombstones accumulate across repeated use of one public window id: an exact
retired `command` receives the typed `owner-window-closed` result and retired
opaque request/push transport is dropped. A fresh host-generated opaque UUID is
a new scope and remains admissible; the host never retargets an old identity to
that window.

Flow IPC supervises unexpected exits with a consecutive-failure budget.
Recovery uses exponential backoff capped at two seconds. Existing native windows
reload after replacement `ready`; Java process-local state is recreated. A
successful recovery starts the manifest-defined stable-run deadline. The
failure count resets only if the same sidecar remains ready for that entire
duration; another exit before then consumes the next attempt. Exhausting the
budget is fatal.
If a recovery child misses its `ready` deadline, the host requests SIGTERM and
arms a one-second grace deadline. If that exact retained `Process`, PID and
generation are still current and running, it sends SIGKILL. Duplicate requests
do not extend the deadline; exit clears it, and a stale deadline cannot target a
new generation. Only the actual process termination callback schedules the
next attempt, not a successful signal call. Signal failure is traced without
claiming exit. This is event-loop-scheduled escalation, not a hard real-time OS
guarantee or a guarantee that shutdown hooks, file transactions or external
effects finish. Ordinary host Quit uses the separate asynchronous retirement
contract above, also used by fatal host shutdown. Planned replacement still has
its own termination path and lacks noncooperative-child exit confirmation.
Unexpected process recovery and planned development replacement use distinct
host states. Planned replacement is allowed only for the generic development
webapp mode and does not consume the unexpected-exit restart budget. If a
candidate cannot reach `ready`, the host waits for the CLI-owned stable-JAR
restore, starts that asset and reloads retained WebViews only after rollback
`ready`.

CLI development commands execute the bundled host process directly so its exit
status is authoritative. Normal close exits zero. Fatal launch or capability
manifest, sidecar startup, `ready` validation and recovery-exhaustion paths exit
nonzero after writing available stderr and operational trace diagnostics. The
host also enforces the manifest's initial `ready` deadline and exits nonzero
without creating a window after retiring a live sidecar that never completes
the handshake. If child retirement cannot be confirmed, it stays fenced for
an explicit Quit retry instead of abandoning ownership.

`HOST_PROTOCOL` lines are acceptance traces, not wire framing. Logs and
screenshots under `.build/native-hosts/macos` are generated artifacts.

## Limits

The current host is macOS-only and uses ad-hoc signing for development. Embedded
JREs, installers, notarization, updates and Windows/Linux hosts remain future
work.
