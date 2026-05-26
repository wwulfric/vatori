# Flow over IPC Framework Validation App

This app is a developer-shaped validation project for the `dev-ipc` runtime. It is separate from the strict smoke fixture so the framework shell can be checked against a less test-specific Flow view.

Run:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

Current scope:

- The view is written with ordinary Flow Java APIs: `@Route`, `Button`, `VerticalLayout`, `Span`, `BigDecimalField`, `Checkbox`, `Select` and a `LoadMode.LAZY` JavaScript dependency.
- It uses semantic component ids only for optional diagnostics; the runtime must not depend on them for renderer promotion, layout, push/resync delivery or bridge installation.
- It covers a Button command path, three real Vaadin Web Components, a v1.10 lazy dependency side effect, a repeated-list text update and manifest-gated desktop capability calls.
- It has a dedicated framework-shape gate, while `test-apps/flow-over-ipc-smoke-app` remains the strict trace fixture for v1/v1.1/v1.2/v1.3 and later cumulative resource gates.

For unattended first-paint verification:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

For unattended interaction verification:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

The framework gate requires generic viewport promotion, first meaningful paint, component events, a list text update and manifest-allowed desktop capability command/result traces.
