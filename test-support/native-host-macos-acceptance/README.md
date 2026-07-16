# macOS Native Host Acceptance Support

This directory contains repository-only WKWebView automation, DOM inspection,
snapshots, multi-window checks and sidecar recovery fault injection.

`bin/build-host` explicitly composes this overlay with the production macOS host
sources. `bin/run` prepares a validation application, builds that host and
forwards repository-only automation variables. The production CLI, builder and
launcher never reference this directory.

```bash
VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 \
bash test-support/native-host-macos-acceptance/bin/run \
  dev test-apps/flow-over-ipc-framework-app
```

`Tests/flow-ipc-opaque-http-bridge-smoke.mjs` and
`Tests/flow-ipc-push-bridge-smoke.mjs` verify the production browser bridge
scripts without adding code to an application production bundle.
