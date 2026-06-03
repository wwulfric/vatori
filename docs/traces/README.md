# Trace Artifacts

Full Flow IPC `.ndjson` traces are generated artifacts and are intentionally not
tracked in Git.

Generate a fresh trace with an unattended validation run, then inspect the
resulting host log:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

Milestone traces that need long-term retention should live in CI artifacts,
release assets or external archival storage, not as one committed trace file per
Flow IPC version.
