# Flow over IPC Smoke App

This is a focused repository fixture for low-level standard-bundle Flow IPC
behavior. It calls `FlowIpcLauncher` directly, so it is not a template for
application developers.

The default command builds and runs its standard Vaadin production bundle over
the opaque request bridge:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

The fixture covers opaque request transport, push/recovery and native-capability
contracts. Detailed automation flags and pass criteria belong to
`docs/acceptance-tools.md` and the trace inspector, not this README.
