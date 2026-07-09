# Roadmap

This document is the stable engineering roadmap for `vatori`. It should describe
direction and sequencing, not preserve per-run trace evidence. Detailed runtime
status belongs in `docs/flow-over-ipc-runtime.md`; acceptance gates belong in
`docs/acceptance-tools.md`. Per-run goal snapshots and archive directories are
not part of the current documentation surface.

## 1. Framework Skeleton

Status: implemented.

- Maven multi-module framework layout.
- Public Java desktop API.
- Real Vaadin Flow examples and validation apps.
- Compatibility host path for local HTTP Vaadin apps.
- IPC envelope and trace infrastructure.
- Central smoke and hygiene scripts.

## 2. Native Host Runtime

Status: in progress.

- Keep the Swift/AppKit/WKWebView host as the main native macOS runtime path.
- Continue hardening Java sidecar lifecycle, window lifecycle and cleanup.
- Keep compatibility-mode boundaries explicit while local HTTP is still used.
- Move desktop commands toward bidirectional command/result transport.
- Add focused smoke coverage for host launch, shutdown, sidecar cleanup and
  desktop capability calls.

## 3. Flow Over IPC Runtime

Status: active product direction with broad validation coverage, not full parity.

- Keep carving Vaadin Flow bootstrap, event, patch, push, resource and capability
  behavior out of Servlet/HTTP/WebSocket assumptions.
- Preserve the Java Flow programming model for application code.
- Keep the no-localhost validation apps developer-shaped where possible, with
  deterministic hooks isolated to `test-apps/`.
- Continue broadening Flow component, resource and event coverage without
  claiming full Flow parity until the relevant surface is complete.
- Keep frontend ownership, generated/lazy resources, production bundle behavior
  and dev-mode/Vite parity as explicit product boundaries.

## 4. Native Capability Model

Status: partially validated, not production-complete.

- Evolve the capability manifest from validation scaffold toward a production
  permission model.
- Keep file dialogs, clipboard, notifications and file transfer behind explicit
  desktop capability APIs.
- Finish sandbox-safe file grants, large-file streaming, progress,
  cancellation, retry and resume behavior.
- Keep command/result traces auditable without committing generated trace logs.

## 5. Packaging And Distribution

Status: future work.

- Define the packaging-time frontend inventory contract for real applications.
- Add jlink/jpackage benchmarks and native app packaging.
- Document size, startup and memory characteristics.
- Add signing, installer packaging and update strategy.

## Current Open Boundaries

- Full Flow client event coverage and UIDL parity.
- Dev-mode/Vite parity and generalized frontend owner/runtime reuse.
- Durable push, reconnect and resync semantics.
- Full Grid virtualization and large-file Upload/Download streaming.
- Full overlay, focus, keyboard and accessibility behavior.
- Production native capability permissions and file grants.
- Packaging, signing and auto-update.
