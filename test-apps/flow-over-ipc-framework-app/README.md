# Flow over IPC Framework Validation App

This repository-only app exercises a broad, developer-shaped Vaadin Flow view
without putting acceptance hooks into framework runtime or `examples/*`. It uses
ordinary Flow APIs across input, selection, data, overlay, upload/download,
authentication and desktop-capability surfaces.

Run the default standard production-bundle path:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

Run its unattended representative workflow and inspect the result:

```bash
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-framework-app .build/vaadin-desktop-host-swift/host.out.log
```

The app includes Vaadin Rich Text Editor to preserve broad acceptance coverage.
Its Maven configuration therefore enables Vaadin's unlicensed commercial banner
for this test app only; framework modules and examples do not inherit that
policy.

The current automation and trace-gate contract lives in
`docs/acceptance-tools.md` and `bin/flow-ipc-trace-inspect --help`.
