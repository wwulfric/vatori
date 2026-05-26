# Acceptance and Smoke Tools

This document classifies the repository code that exists to verify staged framework behavior. These pieces are not product runtime APIs; they are acceptance gates, smoke harnesses or diagnostic probes used while Flow over IPC is being carved out of the compatibility runtime.

## Trace acceptance gates

- `bin/flow-ipc-trace-inspect`
- `vaadin-desktop-devtools/src/main/java/dev/vatori/desktop/devtools/DesktopTraceInspector.java`
- `docs/traces/flow-ipc-v1*.ndjson`

`DesktopTraceInspector` is the strict trace gate for the smoke fixture and selected framework validation runs. Its `--require-flow-ipc-*` options are acceptance gates over recorded trace evidence, not protocol negotiation flags. The gate list is intentionally cumulative for the main smoke path: a later gate requires earlier successful evidence unless a gate represents historical blocker evidence.

The current exception is `--require-flow-ipc-v1_6`. It preserves the narrowed TextField duplicate-definition blocker proof. `--require-flow-ipc-v1_7` and `--require-flow-ipc-v1_8` supersede that path and inherit from v1.5, not v1.6, because they prove the replacement stable module identity and generated/lazy resolver paths.

`--require-flow-ipc-v1_9-loadondemand` is a delta gate. It verifies only the v1.9 generated/lazy `Flow.loadOnDemand` contract evidence: API owner/install source, trigger source, requested chunk, registry/mapping source, resolved artifact path and the current `flow-ipc-loadondemand-v1_9` blocker. `--require-flow-ipc-v1_9` is the cumulative gate: it requires the v1.8 baseline plus this v1.9 delta.

`--require-flow-ipc-v1_10-lazy-dependency` and `--require-flow-ipc-v1_10-production-runtime` are delta gates. The first verifies the v1.10 Flow `LoadMode.LAZY` dependency path: a real Flow DependencyLoader-created lazy script request is intercepted by the no-localhost IPC resolver, staged as a Blob module and executed. The second verifies the production-bundle runtime registry path: the Vaadin dev-bundle stats graph selects the production `indexhtml` artifact and records a staged `execution-attempt` with blocker `flow-ipc-production-runtime-v1_10`. `--require-flow-ipc-v1_10` is cumulative: it requires the v1.9 baseline plus both v1.10 deltas.

`--require-flow-ipc-v1_3-framework` is separate from the strict smoke-app gate. It verifies a developer-shaped app with ordinary Flow APIs, generic viewport promotion and desktop capability command/result evidence.

## In-process smoke harnesses

- `bin/uidl-adapter-smoke`
- `bin/uidl-rpc-smoke`
- `bin/flow-ipc-scope-smoke`
- `bin/flow-client-resource-smoke`
- `vaadin-desktop-flow/src/test/java/dev/vatori/desktop/flow/transport/UidlAdapterSmokeMain.java`
- `vaadin-desktop-flow/src/test/java/dev/vatori/desktop/flow/transport/UidlRpcSmokeMain.java`
- `vaadin-desktop-flow/src/test/java/dev/vatori/desktop/flow/ipc/FlowIpcScopeSmokeMain.java`
- `vaadin-desktop-flow/src/test/java/dev/vatori/desktop/flow/ipc/FlowClientResourceSmokeMain.java`

These commands build a minimal desktop-shaped Flow boundary in process. They are used to verify the UIDL writer, RPC dispatch, session/UI scope rejection and advertised Flow client resource graph without opening a desktop window.
They live under `src/test/java` and run on Maven's test classpath so the production `vaadin-desktop-flow` artifact does not export smoke entrypoints.

## Real-window acceptance hooks

- `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1`
- `VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1`

These switches live in `vaadin-desktop-host-swift/Sources/VaadinDesktopHost/main.swift` because only the AppKit/WKWebView host can drive the real WebView DOM, file dialog cancellation, snapshot timing and window shutdown path. They must remain opt-in, environment-gated and documented as acceptance hooks. `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1` should be used for automated local runs so the validation window does not steal keyboard focus.

## Validation apps

- `test-apps/vaadin-desktop-smoke-app`
- `test-apps/flow-over-ipc-smoke-app`
- `test-apps/flow-over-ipc-framework-app`

The smoke app is the strict fixture for cumulative Flow IPC gates. The framework app is intentionally closer to an external developer project and should catch runtime assumptions that accidentally depend on smoke-app ids or layout.

## Runtime diagnostics

Some runtime code still emits diagnostic probes because the corresponding framework boundary is not final yet:

- Controlled duplicate/late push probes and resync diagnostics in `FlowIpcRuntime`.
- Resource staging, frontend artifact classification and generated/lazy blocker metadata in `FlowClientResources`.
- The v1.9 generated/lazy diagnostic bridge: it calls the real `window.Vaadin.Flow.loadOnDemand` only after the real generated-flow-imports artifact installs it through direct staged import. It is traced as `frontendArtifactGeneratedLazyDiagnosticBridge=true` and does not count as a Flow-client-engine-triggered success.
- The v1.10 Flow lazy dependency bridge: it intercepts the real Flow DependencyLoader's `LoadMode.LAZY` script element for `context://vatori-flow-ipc-lazy-v1_10.js`, resolves it over IPC and dispatches the load/error event back to the element. This counts only for the ordinary lazy dependency path, not for generated `Flow.loadOnDemand` chunk parity.
- The v1.10 production runtime registry path: it stages the stats-selected production `indexhtml` artifact as an execution attempt and keeps the remaining ownership gap explicit as `flow-ipc-production-runtime-v1_10`.
- The repository-local UIDL applier fallback for non-engine or failed-delivery diagnostics.

These are acceptable only when the trace names the blocker or fallback explicitly. They should not be described as final runtime behavior, and successful future work should move stable checks toward trace gates, smoke commands or validation apps instead of growing hidden app-specific runtime branches.

## Maintenance rules

- Add new verification requirements as trace gates or smoke commands when possible.
- Keep real-window automation behind explicit `VAADIN_DESKTOP_*` environment switches.
- Keep compatibility mode and incomplete Flow over IPC boundaries visible in docs and trace names.
- Do not treat historical blocker gates as required predecessors for replacement-path gates unless the replacement still depends on that blocker evidence.
- When a gate is superseded, document the relationship here and in `DesktopTraceInspector.AcceptanceGate`.
