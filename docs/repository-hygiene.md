# Repository Hygiene and Test Layout

This repository keeps framework source, acceptance contracts and small examples in
Git. It does not keep generated runtime evidence or local build output in normal
version control.

## Versioned Files

Track:

- Framework source, scripts and focused documentation.
- Small hand-written fixtures that are required to exercise a code path.
- Acceptance gates implemented in code, such as `bin/flow-ipc-trace-inspect` and
  `DesktopTraceInspector`.
- Minimal examples that show how a developer uses the framework.

Do not track:

- Full `.ndjson` traces from desktop runs.
- Maven `target/`, local `.build/`, `.m2`, frontend generated output, app bundles
  or packaged artifacts.
- Per-run screenshots, host logs or local resource staging directories.

Run the guard before publishing a branch:

```bash
bash bin/repository-hygiene-check
```

For the centralized non-window smoke path, use:

```bash
bash bin/verify-smoke
```

Generated traces are review evidence, not source fixtures. A trace may be copied
to a release artifact, CI artifact or external archive when a milestone needs
preserved proof, but ordinary development should regenerate it from the
validation app and inspect it in place.

## Trace Workflow

Use the latest validation command to create a fresh trace:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

For historical or narrower slices, keep the gate in `DesktopTraceInspector` and
the verification recipe in documentation. Do not add one permanent `.ndjson`
file per version.

Do not add per-run goal snapshots such as `docs/goal-*.md`, or archive
directories for those snapshots. Use git history, CI artifacts, release assets
or external storage when milestone evidence must be retained.

## Examples vs Test Apps

`examples/` is for developer-facing sample apps. These apps should be small,
readable and useful as starting points:

- `examples/counter-app`
- `examples/grid-app`
- `examples/desktop-capabilities-app`

`test-apps/` is for validation targets. These apps may contain deterministic ids,
auto-run hooks, broad component matrices, manifests and intentionally artificial
probes:

- `test-apps/vaadin-desktop-smoke-app`
- `test-apps/flow-over-ipc-smoke-app`
- `test-apps/flow-over-ipc-framework-app`

When a scenario is both educational and useful for validation, keep the polished
developer story in `examples/` and put the exhaustive assertion surface in
`test-apps/`. Tests should be centralized around reusable smoke commands and
trace gates rather than duplicated inside example apps.
