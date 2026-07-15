# Flow Integration

vatori integrates real Vaadin Flow applications with a desktop host in two modes:
compatibility mode and Flow IPC.

## Compatibility Mode

The default `dev` path starts a Spring Boot / Vaadin app on localhost and opens a
desktop WebView:

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

This is the developer-facing baseline today. It keeps Servlet, HTTP and WebSocket
assumptions behind `DesktopLauncher` and the host abstraction.

## Flow IPC Mode

`dev-ipc` starts a Java sidecar that communicates with the Swift host over
`VFIPC1` envelopes:

```bash
bash bin/vaadin-desktop dev-ipc examples/counter-app
```

The WebView loads the application's standard Vaadin production bundle from a
local scheme. Its normal XHR, Fetch and push calls cross an opaque IPC bridge;
Java adapts them to Vaadin's request-handler chain. The host does not implement a
Flow client or parse UIDL.

## Current Verified Integration

- Real Flow routes and components are instantiated by the Java sidecar.
- Bootstrap, UIDL requests and component events are ordinary requests from the
  standard Flow client, carried opaquely to Flow server request handlers.
- UIDL patches are consumed by the standard Flow client from the application's
  production bundle.
- Standard frontend resources come from the application's production webapp;
  the product path does not request individual Flow client modules through a
  custom protocol.
- Desktop capability calls travel through the host protocol and respect manifest
  checks.
- Ordinary applications keep calling `DesktopLauncher`; Flow IPC transport
  starts a no-web Spring application context, discovers the application's
  annotated routes and uses Vaadin's Spring instantiator and scopes without
  starting an HTTP server.

## Framework Validation App

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

This app validates ordinary Flow APIs across broader product surfaces without
making examples depend on fixture ids.

## Remaining Integration Work

- Define an explicit desktop contract for dynamic routes that are not discoverable
  from application-package annotations.
- Bound or replace Servlet/Web Spring Security integrations for applications that
  need desktop-native authentication semantics.
- Decide whether Flow IPC should remain production-bundle-only or gain a separate
  development frontend workflow without reviving the custom Flow client shell.
- Broaden event, focus, overlay, resource streaming, reconnect, distribution and
  packaging parity.

The Flow IPC product path has one browser runtime: the application's standard
Vaadin production bundle. Compatibility testing belongs to the localhost modes,
not to a second custom Flow client implementation.

Do not document integration progress as numbered batches. Use semantic capability
names and acceptance gates.
