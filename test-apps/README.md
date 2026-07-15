# Test Apps

These modules are repository acceptance targets. They may contain deterministic
ids, manifests, automation hooks and broad component matrices that do not belong
in framework runtime or developer-facing examples.

- `vaadin-desktop-smoke-app`: compatibility-mode host smoke app.
- `flow-over-ipc-smoke-app`: focused standard-bundle Flow IPC fixture.
- `flow-over-ipc-framework-app`: developer-shaped Flow view used by standard
  production-bundle acceptance.

Run the central non-window suite with:

```bash
bash bin/verify-smoke
```

The `dev-ipc` path uses the standard Vaadin production bundle. Window automation
and the current trace-gate contract live in
`docs/acceptance-tools.md` and `bin/flow-ipc-trace-inspect --help`.
