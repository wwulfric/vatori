# Flow over IPC Runtime

Run an ordinary developer-shaped Flow app:

```bash
bash bin/vaadin-desktop dev-ipc examples/counter-app
```

Run the framework-shaped validation app:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

`dev-ipc` is separate from compatibility mode:

- `dev` starts embedded Spring Boot and localhost HTTP.
- `dev-swift` starts the Swift/AppKit host but still loads the localhost Flow URL.
- `dev-ipc` builds the application's standard Vaadin production webapp and loads
  it through a host-owned local scheme with opaque IPC request transport.

## Runtime Shape

```text
Flow route
  -> DesktopLauncher / FlowIpcLauncher
  -> StandardFlowIpcRuntime
  -> Vaadin request-handler chain
  -> opaque HTTP-shaped VFIPC1 exchanges
  -> Swift/AppKit/WKWebView host
  -> standard production webapp + standard Flow client
```

For Flow IPC transport, the Swift host prefers the built application executable
jar so stdin/stdout is connected to the real Java app process. If no runnable jar
is available, the host can still fall back to Maven. Ordinary application mains
continue to call `DesktopLauncher.launch(...)`; Flow IPC transport starts the
application's Spring context as `WebApplicationType.NONE`, discovers all
annotated routes under the application package and requires exactly one root
route. Route components are created through Vaadin's `SpringInstantiator`, so
constructor injection and Vaadin UI/session scopes remain available without an
HTTP server. Test fixtures may call `FlowIpcLauncher.launch(...)` directly.

## Standard Production Bundle

The standard production bundle is the default `dev-ipc` runtime:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

The launcher runs Vaadin's production frontend build, refreshes the executable
application JAR and fails fast if the output is incomplete. It copies the
application's standard Vaadin production output from
`target/classes/META-INF/VAADIN/webapp` into the native app bundle as
`Contents/Resources/FlowWebApp`. Direct executable development can still use the
target directory as a fallback. The Swift host serves those static files through
`vatori-app://`; no custom Flow client shell is packaged.

A small document-start bridge replaces `XMLHttpRequest` and `fetch` only for the
virtual application origin. It forwards method, URL, repeated headers and body
bytes without interpreting Flow request types or UIDL. String, array-buffer,
typed-array, URL-encoded, Blob, FormData and readable-stream request bodies are
serialized through the browser's standard BodyInit implementation. Request and
response bodies cross IPC as bounded 32 KiB chunks. Receiver-issued credit limits
queued data, and abort or XHR timeout cancels both sides of the exchange. A frame
already in transit when cancellation wins may still arrive and must be ignored.

On the Java side, `DesktopFlowOpaqueHttpSession` adapts that HTTP-shaped exchange
to Vaadin's normal request-handler chain. It also restores the generic classpath
static-resource responsibility normally provided by the Servlet container for
production webapp files and `META-INF/resources`. `JavaScriptBootstrapHandler`
produces the bootstrap response and the standard Flow client subsequently uses
the same bridge for UIDL requests. The browser and native host do not parse
bootstrap or UIDL payloads.

Standard-bundle mode accepts only opaque request-stream, response-credit, push,
window-lifecycle and shutdown envelopes. Bootstrap, event, UIDL and resource
meaning stays inside Vaadin's standard client and request-handler chain.

The bridge maintains a private, per-WebView cookie jar for the virtual origin.
It consumes repeated `Set-Cookie` values, applies domain/path/secure/expiry rules,
honors Fetch `credentials: omit`, and sends matching `Cookie` headers on later
requests. Cookie values are not exposed through Fetch/XHR response headers or
diagnostics.

Flow's frontend route calculation currently recognizes HTTP(S) application
bases. The static scheme handler therefore adds a virtual HTTPS `<base>` to the
served index response while leaving the generated production artifact unchanged.

Standard Flow still expects the Atmosphere browser API for push. The production
bundle runtime maps only Vaadin's standard push script URL to a small IPC transport
shim. Flow's own client continues to own UIDL parsing, state synchronization and
component rendering. On the server, a `PushConnectionFactory` writes normal
`UidlWriter` responses to the shim. Connection identity, monotonic transport
sequence, duplicate rejection, gap-triggered resync and an in-process
disconnect/reconnect cycle are covered without WebSocket or localhost. A
reconnecting client also sends Flow's last-seen server sync id; if it is behind
the latest UIDL already emitted by this connection, Java sends a full resync
instead of waiting for another push to expose the loss.

The current real-window surface proves packaged production bundle loading,
bootstrap GET, UIDL POST, real Vaadin component rendering and server-initiated
push across an in-process reconnect/resync. Focused Java and browser bridge tests
cover classpath static GET/HEAD, repeated headers, common BodyInit forms,
request/response streaming, backpressure and cancellation. A host window
registry assigns an immutable `windowId`/`sessionId`/`uiId` scope to each WebView;
Java shares one initialized `VaadinService` while creating a separate
`VaadinSession`, UI, in-flight request namespace and push sequence for each
scope. The framework-shaped app provides the representative component and
desktop-capability workflow. This is a bounded validation surface, not installer
or full component parity.

The standard-bundle host also has bounded sidecar recovery. An unexpected Java
exit fails outstanding opaque requests, disconnects the old push subscriptions
and preserves the existing NSWindow and WKWebView. The host restarts Java with a
limited exponential-backoff policy and a ready watchdog, then reloads the same
production webapp in the existing WebView. The new process creates a new
`VaadinSession` and UI: in-memory server UI state resets, while durable business
state across processes remains the application's responsibility. A real
WKWebView gate verifies different sidecar process ids, one native window, two
loads and first paints, a fresh push sequence, recovered request transport and a
post-recovery component event.

The remaining transport work stays HTTP-shaped and Flow-agnostic. Large bodies
use request/response stream ids, bounded chunks, receiver credit, cancellation
and terminal status frames. Java exposes those frames through Servlet
input/output stream interfaces, while the browser and native host keep every
chunk opaque. The multipart `getParts()` adapter still buffers only when that
API is explicitly requested; modern raw `UploadHandler` input remains
streaming. Multiple windows use host-assigned scope rather than browser-provided
session authority. The native host routes responses to the exact WebView, and a
`windowClosed` envelope releases only the corresponding Java session and
in-flight exchanges.

## Verified Boundaries

- Java sidecar lifecycle is owned by the Swift host.
- WKWebView loads the packaged production webapp from `vatori-app://`, not
  `http://127.0.0.1`.
- XHR, Fetch and push cross `WKScriptMessageHandler` and `VFIPC1` without Flow
  parsing in the browser bridge or native host.
- Java instantiates real Flow routes and components.
- Click/change events, ordered push and recovery resync are dispatched through
  the server-side Flow RPC path.
- Bootstrap and patches include real UIDL payloads.
- Desktop capabilities use manifest-enforced command/result traces.
- Standard-bundle windows have host-assigned identities, separate Vaadin
  sessions and directed response/push delivery.
- Standard-bundle sidecar recovery preserves the native window/WebView while
  replacing the Java process and rebuilding Flow session state.

## Framework Validation App

`test-apps/flow-over-ipc-framework-app` keeps the strict fixture separate from a
developer-shaped Flow view. It uses ordinary Flow Java APIs for buttons, fields,
selectors, data components, grids, uploads/downloads, overlays, rich text, theme
variants, authentication components and desktop capability calls.

Unattended interaction run:

```bash
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-framework-app .build/vaadin-desktop-host-swift/host.out.log
```

Standard production bundle sidecar-recovery run:

```bash
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SIDECAR_RESTART=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-sidecar-recovery .build/vaadin-desktop-host-swift/host.out.log
```

The standard-bundle driver is compiled from `test-support`; it is not shipped in
the normal framework host or copied into the Vaadin production webapp.

## Current Limits

Passing the current gates does not mean full Flow over IPC product parity. These
remain open:

- Servlet filters, servlet-backed Spring Security and application beans that
  require a `WebApplicationContext`; use compatibility mode for those web
  integrations.
- Dynamic route registration outside the application package scan requires a
  dedicated desktop registration contract.
- Generalized current app generated chunk execution.
- Production owner import and reusable side-effect parity.
- Dev-mode/Vite execution.
- Full Grid scroll virtualization and reconnect behavior.
- Upload/Download retry and resume, plus removal or bounding of the
  multipart `getParts()` buffering path.
- Cross-process restoration of application-owned business state and broader
  multi-window/repeated-failure recovery coverage.
- Full overlay focus trap, keyboard, context menu and accessibility parity.
- Complete theme system and production packaging.

The authoritative acceptance contract lives in the trace inspector code and the
current gate list shown by `bin/flow-ipc-trace-inspect --help`.
