# Acceptance and Smoke Tools

This document describes the repository tools used to verify Flow IPC behavior.
These tools are not public runtime APIs.

## Main Commands

```bash
bash bin/repository-hygiene-check
bash bin/trace-inspector-acceptance-gate-smoke
bash bin/verify-smoke
bash bin/flow-ipc-trace-inspect --help
```

`bin/verify-smoke` runs the hygiene guard, inspector contract smoke, opaque HTTP
adapter smoke, Flow IPC scope smoke and the browser-side opaque request/push
bridge smokes.

## Trace Inspector

`bin/flow-ipc-trace-inspect` reads host logs and verifies the supported
standard-bundle acceptance gates. The CLI is the contract for the current gate
set; gate names are not protocol negotiation flags or worklog milestones.

Normal `dev-ipc` runs write compact operational traces. Generate inspector
evidence explicitly with:

```bash
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full bash bin/vaadin-desktop dev-ipc <application-module>
```

Full trace mode keeps resource and DOM acceptance metadata and disables live log
following by default. Set `VAADIN_DESKTOP_HOST_LOG_FOLLOW=1` only when live full
output is needed. Compact auto-shutdown runs invoke
`bin/flow-ipc-log-budget-check`; the byte budgets can be overridden with
`VAADIN_DESKTOP_FLOW_IPC_LOG_MAX_BYTES` and
`VAADIN_DESKTOP_FLOW_IPC_LOG_MAX_LINE_BYTES`.

Examples:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-runtime .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-framework-app .build/vaadin-desktop-host-swift/host.out.log
bash bin/flow-ipc-trace-inspect --require-flow-ipc-standard-bundle-sidecar-recovery .build/vaadin-desktop-host-swift/host.out.log
```

Use `--help` for the current complete list. Documentation should reference
semantic gate names only; do not add numbered gate aliases or historical slice
labels.

## Gate Families

The product-path gates are grouped by stable runtime behavior:

- Standard-bundle runtime: packaged production resources, Flow client loading,
  opaque bootstrap/UIDL responses, push, first meaningful paint and shutdown.
- Standard-bundle framework app: packaged Vaadin production output, opaque
  request/push transport, standard Flow client ownership, representative input,
  selection, date, dialog, form-validation and native-command workflows.
- Standard-bundle sidecar recovery: one retained NSWindow/WKWebView, a replaced
  Java process, two production-bundle loads/first paints, fresh Flow session and
  push state, and a recovered component-event round trip.

## Smoke Apps

- `test-apps/flow-over-ipc-smoke-app` is the strict runtime fixture.
- `test-apps/flow-over-ipc-framework-app` is the developer-shaped validation app.
- `test-apps/vaadin-desktop-smoke-app` validates compatibility-mode desktop
  launch behavior.

The smoke apps may contain deterministic ids and automation hooks. Examples
should stay developer-shaped and should not depend on fixture ids.

The standard production bundle uses a small WKWebView driver from
`test-support`. It interacts with the rendered developer-shaped app
through ordinary DOM events and records one semantic completion event; it does
not add application runtime code to the production webapp.
A second driver injects an unexpected sidecar exit and checks the bounded host
recovery contract; neither driver ships in normal host builds.

## Generated Artifacts

Host logs, `.ndjson` traces, screenshots and downloaded files are generated
artifacts. Keep them under `.build/`, CI artifacts, release assets or external
archives. Do not commit one trace file per milestone.

## Hygiene Guard

`bin/repository-hygiene-check` protects top-level docs and agent instructions
from process logs, historical goal files and numbered gate aliases. It also
rejects custom shell/runtime remnants in production modules and validation-app
fixtures in framework runtime, and prevents retired shell/test-support or the
historical inspector source tree from returning. Keep that guard active when changing
`AGENTS.md`, `README.md`, docs, host resources or acceptance gate naming.
