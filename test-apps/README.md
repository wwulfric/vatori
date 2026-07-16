# Test Apps

These are repository validation applications. They use the same public
`@SpringBootApplication + DesktopLauncher` entry as examples, but may expose
deterministic ids and broad assertion surfaces.

- `vaadin-desktop-smoke-app`: compatibility HTTP and basic host behavior.
- `flow-over-ipc-smoke-app`: focused standard-bundle runtime behavior.
- `flow-over-ipc-framework-app`: broad Flow component and native capability
  validation.

Run non-window checks with `bash bin/verify-smoke`. Run any app through Flow IPC
with `bash bin/vaadin-desktop dev <module>` or through loopback UI transport with
`bash bin/vaadin-desktop dev-compat <module>`.

Automation and trace contracts live in `docs/acceptance-tools.md`.
