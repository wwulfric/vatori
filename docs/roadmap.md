# Roadmap

This document records stable engineering direction, not per-run evidence or goal
history. Current runtime boundaries live in `docs/flow-over-ipc-runtime.md` and
acceptance contracts in `docs/acceptance-tools.md`.

## Framework Foundation

Status: implemented.

- Public Java desktop application and capability API.
- Core IPC envelope and one blocking sidecar launcher model.
- Developer examples and separate validation applications.
- Central smoke and repository hygiene guards.

## Native Host Contract

Status: implemented on macOS, open cross-platform.

- One platform-selected AppKit/WKWebView host.
- Structured application/window `ready` handshake.
- Stable `VFIPC ` framing and numeric envelope version for both UI transports.
- Bundle launch manifest and repository-independent sidecar staging.
- Explicit production and acceptance build boundaries.
- Bounded sidecar replacement for Flow IPC.

Next work:

- harden process supervision and repeated-failure policy;
- define the same contract for future Windows/Linux hosts;
- add production packaging, signing and update delivery.

## Flow Over IPC

Status: active product direction with broad validation, not full parity.

- Keep the standard Vaadin production webapp and opaque request bridge as the
  only browser runtime.
- Keep all browser requests inside Vaadin's request-handler chain.
- Preserve pure `DesktopFlowApplication + DesktopLauncher` as the default and
  Spring Boot as an optional adapter.
- Continue component, resource, event, push and multi-window parity one
  verifiable semantic slice at a time.

Next work:

- dev-mode/Vite workflow over IPC;
- generated and lazy frontend owner/resource parity;
- full Grid virtualization and large-file retry/resume;
- overlay, keyboard, focus and accessibility parity;
- durable application state across sidecar replacement.

## Native Capabilities

Status: real command/result path, production permission model incomplete.

- Keep clipboard, dialogs, notification, window and file transfer behind public
  desktop APIs and manifest authorization.
- Add a complete application-menu model only when native click events and Java
  callback correlation are defined.
- Complete sandbox-safe file grants, progress, cancellation, retry and resume.
- Keep capability correlation transport-independent and auditable without
  committing generated logs.

## Distribution

Status: future work.

- Add an embedded JRE to the existing application/native-host staging layout.
- Measure startup, memory and package size.
- Add installers, signing/notarization, update strategy and release automation.

Do not create versioned goal docs or placeholder platform modules to represent
these items. Product state should be proven by code, focused living docs and
executable acceptance.
