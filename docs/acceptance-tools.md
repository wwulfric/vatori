# Acceptance And Smoke Tools

These tools verify repository and runtime contracts. They are not public
framework APIs.

## Non-Window Suite

```bash
bash bin/verify-smoke
```

The script currently covers:

- repository hygiene and trace-inspector contracts;
- opaque request-handler integration and StreamResource behavior;
- Flow IPC scope, backpressure, cancellation and capability correlation;
- launcher transport, ready payload, pure Flow EOF and optional Spring context lifecycle;
- browser request and push bridge contracts.

The script itself is the source of truth for its subcommands.

## macOS Acceptance Build

Normal CLI and host builds contain only production sources. The repository-only
runner under `test-support` explicitly compiles the acceptance overlay and then
launches the resulting host.

Representative unattended framework run:

```bash
VAADIN_DESKTOP_MACOS_NO_ACTIVATE=1 \
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full \
VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 \
VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 \
bash test-support/native-host-macos-acceptance/bin/run \
  dev test-apps/flow-over-ipc-framework-app
```

Acceptance flags, DOM inspection, snapshots and fault injection must remain in
test-support. Production host code may expose a neutral runtime observer, but
the production CLI, builder and launcher cannot know acceptance variables,
fixture ids or test-support paths.

For unattended runs, the test-support runner requires the completion trace for
the selected acceptance family and exits nonzero when the app closes without
reaching it.

## Trace Inspector

The host writes compact operational logs to:

```text
.build/native-hosts/macos/host.out.log
.build/native-hosts/macos/host.err.log
```

Inspect supported gates with:

```bash
bash bin/flow-ipc-trace-inspect --help
```

Examples:

```bash
bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-standard-bundle-runtime \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-standard-bundle-framework-app \
  .build/native-hosts/macos/host.out.log

bash bin/flow-ipc-trace-inspect \
  --require-flow-ipc-standard-bundle-sidecar-recovery \
  .build/native-hosts/macos/host.out.log
```

Gate names describe semantic behavior; they are not wire negotiation or goal
versions. `HOST_PROTOCOL` is an acceptance trace prefix, not sidecar framing.

## Coverage Families

- Runtime: production bundle load, opaque bootstrap/UIDL, push, first paint,
  capability result and shutdown.
- Framework app: representative input, selection, navigation, data, overlay,
  layout, rich content, authentication, Avatar/resource, upload/download and
  native capability workflows.
- Multi-window: scoped sessions, request/push isolation and close behavior.
- Recovery: retained native window/WebView, replaced Java process, fresh Flow
  session and recovered interaction.
- Compatibility: loopback UI load through the same host plus real native
  capability result correlation.

The complete assertion set lives in the acceptance source and trace inspector,
not in this document.

## Log Budget

Compact runs summarize resource bodies and repeated envelopes. Full detail is
enabled only for evidence inspection:

```bash
VAADIN_DESKTOP_FLOW_IPC_TRACE_DETAIL=full bash bin/vaadin-desktop dev <module>
```

Set `VAADIN_DESKTOP_MACOS_LOG_FOLLOW=1` to follow full logs live. Compact
auto-shutdown runs use `bin/flow-ipc-log-budget-check`; its size overrides remain
repository test settings.

## Generated Artifacts

Logs, traces, screenshots, downloaded files and native apps stay under `.build`,
CI artifacts, release assets or external storage. Do not commit one evidence file
per milestone.
