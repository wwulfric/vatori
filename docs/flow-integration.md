# Flow Integration

Applications use one programming and launch model for both UI transports:

```text
@SpringBootApplication + @Route components + DesktopLauncher
```

The native host declares the transport. Application code does not select host or
transport and does not call an alternate Flow-specific launcher.

## Flow IPC

`dev` starts a non-web Spring context, discovers annotated routes under the
application package and uses Vaadin's Spring instantiator and scopes. The standard
Vaadin production client sends ordinary bootstrap, UIDL, event and resource
requests through an opaque bridge. Java converts only the HTTP-shaped boundary
and delegates every request to `VaadinService.handleRequest()`.

Standard classpath resources and `StreamResource` downloads are represented by
desktop request handlers inside Vaadin's handler chain. Upload and StreamReceiver
requests continue through Vaadin's standard stream handler.

## Compatibility HTTP

`dev-compat` starts the application's normal embedded web server on loopback.
Vaadin servlet auto-configuration, filters and WebSocket behavior remain active.
The native host receives the URL in the same structured `ready` handshake used
by Flow IPC.

Compatibility changes only the WebView UI transport. Desktop API calls use the
same `NativeBridge`, command correlation and capability result validation as
Flow IPC.

## Validation Application

```bash
bash bin/vaadin-desktop dev test-apps/flow-over-ipc-framework-app
```

The validation app uses the same public launch shape as examples while exposing
deterministic state for broader Flow component and native capability checks.

## Open Integration Boundaries

- dynamic routes outside application-package discovery;
- desktop-native alternatives for servlet-backed authentication/security;
- dev-mode/Vite frontend workflow over IPC;
- complete component, focus, overlay, accessibility and resource parity;
- production packaging and cross-platform host implementations.

Use semantic capability names in docs and tests. Do not encode implementation
batch or goal versions in application or framework source.
