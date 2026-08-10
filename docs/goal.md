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
- The canonical Flow IPC path is the standard Vaadin production bundle,
  official Flow client and opaque request/push transport. Do not introduce a
  custom Flow client, UIDL interpreter or component renderer to reach parity.
- Flow parity is capability-driven, not a checklist of per-component product
  integrations. Component families and representative components are
  acceptance probes for shared resource, event, UIDL, overlay, input,
  accessibility and lifecycle behavior.
- Classify every failed probe before editing: reusable framework/runtime gap,
  native-host product boundary, upstream/application semantics or
  acceptance-only coverage gap. Change production code only for a reusable
  generic capability.
- Examples should remain developer-shaped applications that depend on framework
  modules as an external user would.
- Test apps and test-support may contain deterministic hooks, component ids,
  automation, broad matrices, trace gates and fault injection. These must not
  leak into framework, native-host or IPC production sources.

## Open Product Areas

The following areas are intentionally tracked in living design and acceptance
documents rather than in this file:

- generalized Flow client event and UIDL parity;
- dev-mode/Vite and production frontend ownership parity;
- full component, overlay, focus, keyboard and accessibility behavior;
- Grid virtualization and large-file Upload/Download streaming;
- native capability permissions and file grants;
- broader session-scoped push delivery, reconnect and resync semantics, plus
  durable application outcomes across sidecar replacement;
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

Advance one smallest generic, verifiable capability slice at a time. A focused
component fixture closes only that slice; the long-term goal is complete only
when the generic capability matrix and real desktop product boundaries are
closed without component-specific production branches.
