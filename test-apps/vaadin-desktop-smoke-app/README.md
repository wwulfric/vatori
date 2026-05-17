# Vaadin Desktop Smoke App

This is an independent developer-style test project. It depends on `vaadin-desktop-flow` and uses real Vaadin Flow views.

Run from the repository root:

```bash
bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

Run through the Swift native host spike:

```bash
bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

Framework v0 runs this app in **compatibility mode**: Vaadin Flow is served by embedded Spring Boot on `127.0.0.1`, and the default macOS host embeds that URL in a WKWebView desktop window. This is intentionally documented as a transition layer toward Flow over IPC, not the final transport architecture.

The `dev-swift` path uses a real Swift/AppKit host and manages the Java sidecar lifecycle, but the Flow UI transport is still localhost HTTP. Desktop runtime API calls are emitted as Host Protocol v0 command traces.

Smoke coverage:

- `@Route("")`
- Button click
- Span state update
- Grid/List data display
- Desktop runtime API call through `Desktop.current().notifications()`
