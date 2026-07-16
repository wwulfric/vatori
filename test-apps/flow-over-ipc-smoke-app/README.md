# Flow over IPC Smoke App

This focused runtime fixture is a normal Spring Boot desktop application using
`DesktopLauncher`.

```bash
bash bin/vaadin-desktop dev test-apps/flow-over-ipc-smoke-app
```

It covers standard-bundle opaque requests, push/recovery, scope and native
capability contracts. Detailed automation flags and pass criteria belong to
`docs/acceptance-tools.md` and the trace inspector.
