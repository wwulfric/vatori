# Test Apps

These apps are validation targets. They may contain deterministic ids, manifests,
auto-run hooks, broad component matrices and artificial probes that would make a
developer-facing example noisy.

Use centralized commands and trace gates instead of duplicating ad hoc test
recipes inside examples:

```bash
bash bin/verify-smoke
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

Current validation targets:

- `vaadin-desktop-smoke-app`: independent compatibility-mode smoke app.
- `flow-over-ipc-smoke-app`: strict Flow IPC fixture for historical gates.
- `flow-over-ipc-framework-app`: developer-shaped Flow IPC validation app.
