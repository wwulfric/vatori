# Flow over IPC Framework Validation App

This app is a developer-shaped validation project for the `dev-ipc` runtime. It is separate from the strict smoke fixture so the framework shell can be checked against a less test-specific Flow view.

Run:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

Current scope:

- The view is written with ordinary Flow Java APIs: `@Route`, `Button`, `VerticalLayout`, `Span`, `BigDecimalField` and `Checkbox`.
- It uses semantic component ids only for optional diagnostics; the runtime must not depend on them for renderer promotion, layout, push/resync delivery or bridge installation.
- It covers a Button command path, two real Vaadin Web Components, repeated list updates and manifest-gated desktop capability calls.
- It is not a new strict fixture yet. `test-apps/flow-over-ipc-smoke-app` remains the strict trace-gated app for v1/v1.1/v1.2/v1.3.

For unattended first-paint verification:

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```
