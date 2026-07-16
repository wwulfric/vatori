# Flow over IPC Framework Validation App

This repository-only app uses ordinary Flow APIs across input, selection, data,
overlay, resource, upload/download, authentication and desktop capability
surfaces. Deterministic state belongs here rather than in framework runtime or
developer examples.

```bash
bash bin/vaadin-desktop dev test-apps/flow-over-ipc-framework-app
```

The app includes Vaadin Rich Text Editor, so its Maven configuration enables the
commercial banner for this test app only. Framework modules and examples do not
inherit that policy.

Unattended workflows and trace inspection are documented in
`docs/acceptance-tools.md`.
