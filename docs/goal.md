# vatori Long-Term Goal

This document describes the stable long-term goal for `vatori`. It should stay
small and durable. Do not use it as a running implementation log.

## Product Direction

`vatori` is a Vaadin-based desktop framework for building desktop-class Vaadin
Flow applications. The long-term direction is similar to Tauri plus Vaadin Flow:

- Preserve the Java Vaadin Flow programming model for application developers.
- Move browser/server assumptions behind explicit adapters.
- Let the desktop host own native WebView integration, native capabilities,
  lifecycle, packaging and update paths.
- Make IPC the transport for bootstrap, events, patches, commands and
  capability results.

## Stable Boundaries

- Compatibility mode remains explicit: a real Vaadin Flow/Spring Boot app can
  run on loopback HTTP while the native host opens it in a desktop WebView.
- Flow over IPC is the product direction, but covered validation slices must not
  be described as full parity until the relevant product surface is actually
  complete.
- Examples should remain developer-shaped applications that depend on framework
  modules as an external user would.
- Test apps may contain deterministic hooks, broad matrices and trace-oriented
  assertion surfaces.

## Open Product Areas

The following areas are intentionally tracked in living design and acceptance
documents rather than in this file:

- generalized Flow client event and UIDL parity;
- dev-mode/Vite and production frontend ownership parity;
- full component, overlay, focus, keyboard and accessibility behavior;
- Grid virtualization and large-file Upload/Download streaming;
- native capability permissions and file grants;
- durable push, reconnect and resync semantics;
- packaging, signing and auto-update.

Use these current documents for details:

- `docs/flow-over-ipc-runtime.md`
- `docs/acceptance-tools.md`
- `docs/roadmap.md`
- `docs/framework-architecture.md`

## Operating Rule

Do not create per-run goal snapshots such as `docs/goal-*.md` or archive
directories for implementation history. Git history, CI artifacts, release
assets and final agent summaries are enough for historical evidence.

When a new implementation slice changes durable product state, update the
relevant living document instead: `docs/flow-over-ipc-runtime.md`,
`docs/acceptance-tools.md`, `docs/roadmap.md` or a focused design document.
Keep this file focused on the stable long-term goal.
