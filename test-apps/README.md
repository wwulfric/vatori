# Test Apps

These are repository validation applications. They use the same public
public launcher contracts as examples, but may expose deterministic ids and
broad assertion surfaces.

- `vaadin-desktop-smoke-app`: optional Spring adapter, compatibility HTTP and basic host behavior.
- `flow-over-ipc-smoke-app`: focused standard-bundle runtime behavior.
- `flow-over-ipc-framework-app`: broad Flow component and native capability
  validation.

Run non-window checks with `bash bin/verify-smoke`. Any app can use Flow IPC with
`bash bin/vaadin-desktop dev <module>`; only Spring adapter apps can use
`bash bin/vaadin-desktop dev-compat <module>`.

Automation and trace contracts live in `docs/acceptance-tools.md`.
