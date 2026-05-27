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

`--require-flow-ipc-v1_11-loadondemand-chunk` is a delta gate. It verifies the v1.11 generated `Flow.loadOnDemand(chunkId)` path: a real bootstrap UIDL lazy expression is observed, the visible Flow client triggers `window.Vaadin.Flow.loadOnDemand`, the host maps the requested chunk to the real generated-flow-imports artifact, stages/imports it over IPC and records either execution or the precise `flow-ipc-loadondemand-chunk-v1_11` blocker. `--require-flow-ipc-v1_11` is cumulative: it requires the v1.10 baseline plus this v1.11 delta.

`--require-flow-ipc-v1_12-loadondemand-chunk-file` is a delta gate. It verifies that the v1.11 generated `Flow.loadOnDemand(chunkId)` path is now chunk-file aware: the runtime parses the real generated-flow-imports body, distinguishes a `./chunks/chunk-*.js` registry from the current no-op `Promise.resolve(0)` table, records the source artifact and resolver metadata, and either stages a real chunk file or emits the precise `flow-ipc-loadondemand-chunk-file-v1_12` / `chunk-file-not-generated` blocker. `--require-flow-ipc-v1_12` is cumulative: it requires the v1.11 baseline plus this v1.12 delta.

`--require-flow-ipc-v1_13-production-owner` is a delta gate. It verifies that the v1.10 production runtime staged attempt now has an explicit runtime owner contract: manifest/request/stage/DOM evidence must name `flow-ipc-production-owner-v1_13`, `owner=production-bundle-runtime`, the stats-selected production `indexhtml` artifact, owner module identity, cache/cleanup policy and a precise owner blocker such as `production-owner-runtime-contract-missing`. Focused component import success is not accepted as production owner success. `--require-flow-ipc-v1_13` is cumulative: it requires the v1.12 baseline plus this v1.13 delta.

`--require-flow-ipc-v1_14-production-owner-import` is a delta gate. It verifies that the v1.13 production owner now performs a real controlled import attempt for the stats-selected production `indexhtml` artifact. Manifest/request/stage/DOM evidence must name `flow-ipc-production-owner-import-v1_14`, `owner=production-bundle-runtime`, the import policy, module identity, dependency rewrite result, Vaadin global snapshots, registry snapshots, component owner reuse status and either an import success or a concrete import failure layer. `production-owner-runtime-contract-missing` is accepted only as v1.13 history; it is not accepted as the v1.14 delta. The current fixture completes the gate with `production-owner-relative-import-unresolved` plus captured browser duplicate-custom-element evidence. `--require-flow-ipc-v1_14` is cumulative: it requires the v1.13 baseline plus this v1.14 delta.

`--require-flow-ipc-v1_15-production-owner-dependencies` is a delta gate. It verifies that the v1.14 owner import attempt now has an owner-scoped relative dependency resolver for the stats-selected production `indexhtml` artifact. Manifest/request/stage/DOM evidence must name `flow-ipc-production-owner-deps-v1_15`, the real dev-bundle build root, the owner dependency namespace, real dependency requests marked `productionOwnerDependencyResource=true`, graph counts, staged dependency count, dependency classifications, import phase candidates for isolated/pre-component diagnostics and either an owner import success or a downstream owner blocker such as `production-owner-missing-real-artifact`. Unclassified `production-owner-relative-import-unresolved` is not accepted as the v1.15 delta. `--require-flow-ipc-v1_15` is cumulative: it requires the v1.14 baseline plus this v1.15 delta.

`--require-flow-ipc-v1_16-production-owner-ordering` is a delta gate. It verifies that the v1.15 owner dependency resolver evidence is now followed by explicit production owner import-ordering and component-reuse decisions. Manifest/request/stage/DOM evidence must name `flow-ipc-production-owner-ordering-v1_16` and `flow-ipc-component-owner-reuse-v1_16`, record an owner-first or isolated phase such as `before-component-imports`, record the after-component fallback when used, and classify the final result at the owner ordering / component reuse layer. Focused component import success, plain after-component owner import evidence, `production-owner-runtime-contract-missing` and unclassified `production-owner-relative-import-unresolved` are not accepted as the v1.16 delta. `--require-flow-ipc-v1_16` is cumulative: it requires the v1.15 baseline plus this v1.16 delta.

`--require-flow-ipc-v1_17-resource-scheme` is a delta gate. It verifies that at least one real frontend artifact execution path is served by a host-owned no-localhost custom scheme / file-backed loader rather than only by WebView Blob staging. Manifest/stage/DOM evidence must name `flow-ipc-resource-scheme-v1_17`, the `vatori-resource` scheme, handler installation, request/response observation, file-backed staging, MIME metadata, redacted path metadata, no-localhost proof and an executed artifact. Production owner Blob fallback is allowed only when the trace names the fallback reason, blocked layer and next resolution step. `--require-flow-ipc-v1_17` is cumulative: it requires the v1.16 baseline plus this v1.17 delta.

`--require-flow-ipc-v1_18-production-owner-resource-scheme` is a delta gate. It verifies that the production owner root/dependency graph itself uses the host-owned no-localhost custom scheme rather than reusing the v1.17 Flow lazy dependency proof. Manifest/request/stage/load/DOM evidence must name `flow-ipc-production-owner-resource-loader-v1_18`, `vatori-resource`, the `production-owner` namespace, stable source-path URL policy, root Blob fallback disabled, root custom-scheme request/response/import evidence and at least one owner dependency custom-scheme request/response. Production owner root Blob fallback is not accepted as the v1.18 delta. `--require-flow-ipc-v1_18` is cumulative: it requires the v1.17 baseline plus this v1.18 delta.

`--require-flow-ipc-v1_19-production-owner-side-effect` is a delta gate. It verifies that the v1.18 custom-scheme production owner graph now reports a distinct side-effect contract: manifest/request/stage/DOM evidence must name `flow-ipc-production-owner-side-effect-v1_19`, record expected custom element tags and Vaadin globals, distinguish `productionBundleRuntimeOwnerModuleImported` from `productionBundleRuntimeOwnerSideEffectSatisfied`, record an acceptable side-effect failure layer when side effects are not satisfied, and record a component reuse decision such as `focused-fallback-after-owner-side-effect-check`. Plain owner import resolve, focused component import success and root Blob fallback are not accepted as v1.19 success. `--require-flow-ipc-v1_19` is cumulative: it requires the v1.18 baseline plus this v1.19 delta.

`--require-flow-ipc-v1_20-production-owner-owned-runtime` is a delta gate. It verifies that the v1.19 side-effect decision is now followed by an owner-first custom-scheme runtime attempt before focused component imports. Manifest/request/stage/DOM evidence must name `flow-ipc-production-owner-owned-runtime-v1_20`, policy `owner-first-custom-scheme-runtime-reuse-v1_20`, phase `before-component-imports`, expected tags/globals, no-Blob-fallback contract and `flow-ipc-component-owner-reuse-policy-v1_20`. If owner-owned runtime is not satisfied, the failure layer must be one of the v1.20 acceptable failures and the component resolver must record `focused-fallback-after-owner-owned-runtime-attempt`; focused component import success alone is not accepted as v1.20 success. `--require-flow-ipc-v1_20` is cumulative: it requires the v1.19 baseline plus this v1.20 delta.

`--require-flow-ipc-v1_21-frontend-resolver-contract` is a delta gate. It verifies that the current frontend artifact families are now covered by one unified resolver contract instead of only family-specific fields. Manifest/request/response/stage/load/DOM evidence must name `flow-ipc-frontend-resolver-contract-v1_21`, cover flow-client classpath, component frontend, theme/static, generated/lazy, Flow lazy dependency, production bundle / owner and dev-mode package graph families, record sha256/body integrity, stable identity/cache/cleanup policy, no-localhost proof, both `blob` and `vatori-resource` schemes, verified-only resources, strict blockers and accepted gaps. Production owner fallback is accepted only when it remains classified as the v1.20 owner-owned runtime blocker; focused component import success or unclassified fallback is not accepted as v1.21 success. `--require-flow-ipc-v1_21` is cumulative: it requires the v1.20 baseline plus this v1.21 delta.

`--require-flow-ipc-v1_22-generated-chunk-fixture` is a delta gate. It verifies that the generated `Flow.loadOnDemand` chunk-file executor has a positive Vaadin-shaped fixture path: manifest/request/response/stage/DOM evidence must name `flow-ipc-loadondemand-chunk-file-execution-v1_22`, include repository fixture imports and chunk artifacts, parse the fixture table as `branch-table-dynamic-import`, return a real chunk body with sha256/byte metadata, rewrite `./chunks/chunk-v1_22.js` to a staged URL, call fixture `Flow.loadOnDemand(chunkId)` and observe `window.VaadinDesktopGeneratedLazyChunkV122.executed`. The gate also requires the v1.12 real-build no-op table / `chunk-file-not-generated` proof, so the fixture is not accepted as current app/build generated chunk parity. `--require-flow-ipc-v1_22` is cumulative: it requires the v1.21 baseline plus this v1.22 delta.

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
- The v1.11 generated loadOnDemand chunk bridge: it only counts chunk ids captured from real UIDL `Flow.loadOnDemand('<chunkId>')` expressions and triggered by the visible Flow client. It records chunk mapping/staging evidence and keeps the remaining chunk-file gap explicit as `flow-ipc-loadondemand-chunk-v1_11`.
- The v1.12 generated loadOnDemand chunk-file registry: it parses the real generated-flow-imports artifact body and, for the current Vaadin build, records `loadOnDemandTableShape=no-op-promise-resolve-0`, `chunkFileStatus=not-generated`, no generated chunk file side-effect proof and `loadOnDemandChunkFileFailureLayer=chunk-file-not-generated`. If a later import attempt hits a downstream browser strict blocker before callback resolution, the gate still requires the no-op table / missing chunk-file proof instead of treating that browser failure as generated chunk success.
- The v1.13 production bundle owner contract: it stages the real stats-selected production `indexhtml` artifact under `owner=production-bundle-runtime`, records owner module identity, registry before/after diagnostics, `componentProductionOwnerReused=false` and `production-owner-runtime-contract-missing`. This is explicit owner evidence, not production bundle import/side-effect success.
- The v1.14 production bundle owner import probe: it performs a controlled dynamic import attempt for the same real stats-selected production `indexhtml` artifact under `owner=production-bundle-runtime`, records import preflight, dependency rewrite, resolved/unresolved specifier, Vaadin global, custom element registry, browser error and side-effect diagnostics, and currently classifies the result as `production-owner-relative-import-unresolved`. This is import attempt evidence, not production bundle parity or import success.
- The v1.15 production owner dependency resolver: it scans the real stats-selected production `indexhtml` artifact's relative imports, requests and stages real existing dev-bundle build artifacts under an owner dependency namespace, records resolved and missing dependency classifications plus import phase candidates for later ordering diagnostics, and currently classifies the remaining owner result as `production-owner-missing-real-artifact`. This is dependency graph closure evidence, not production owner import success or production bundle parity.
- The v1.16 production owner ordering / component reuse probe: it records a `before-component-imports` owner-first preflight, keeps the after-component fallback import visible, records observed phases and component reuse decisions, and currently classifies the remaining owner result as a duplicate custom element / component reuse contract boundary. This is owner ordering evidence, not production owner import success or production bundle parity.
- The v1.17 host-owned resource loader probe: it installs a Swift `WKURLSchemeHandler` for `vatori-resource`, writes the Flow lazy dependency body to a redacted file-backed staging directory, serves/imports that artifact through a custom-scheme URL and records request/response/execution diagnostics. In the v1.17 fixture, production owner custom-scheme graph coverage remains incomplete and is expressed as explicit Blob fallback metadata; v1.18 supersedes that fallback for the owner graph delta.
- The v1.18 production owner resource-scheme probe: it uses the same Swift `WKURLSchemeHandler` for production owner root and resolved dependency artifacts under `vatori-resource://flow-ipc/production-owner/...`, records root/dependency request/response/import diagnostics and disables root Blob fallback as a passing condition. It is still production owner graph evidence, not production owner import/side-effect success or production bundle parity.
- The v1.19 production owner side-effect probe: it records expected tags/globals, module import result, side-effect satisfaction result, side-effect failure layer and component reuse decision after the owner custom-scheme import. The current accepted path is `focused-fallback-after-owner-side-effect-check`, which is explicit fallback evidence and not production owner reuse success.
- The v1.20 production owner-owned runtime probe: it attempts the owner custom-scheme runtime before focused component imports, records expected tags/globals, actual phase, no-Blob-fallback policy, owner-owned runtime satisfied/failure result and component resolver reuse decision. The current accepted path is `production-owner-owned-missing-real-artifact` plus `focused-fallback-after-owner-owned-runtime-attempt`, which is explicit contract-layer fallback evidence and not production owner side-effect success.
- The v1.21 frontend resolver contract probe: it aggregates resource request/response/stage/load evidence into one resolver summary for flow-client, component frontend, theme/static, generated/lazy, Flow lazy dependency, production owner and dev-mode families. It records body integrity, stable identity, cache/cleanup, Blob and `vatori-resource` scheme usage, verified-only classifications, strict blockers and accepted gaps. This is unified resolver evidence, not real app/build generated chunk-file parity, production owner side-effect success or dev-mode/Vite parity.
- The v1.22 generated chunk-file positive fixture: it uses repository classpath fixtures shaped like Vaadin generated-flow-imports output with `pending.push(import('./chunks/chunk-v1_22.js'))`, runs them through the same no-localhost IPC resolver and WebView staging/import path, and records callback plus side-effect success. This is positive executor evidence for a real chunk table shape, not proof that the current Vaadin app/build generated a lazy chunk file.
- The repository-local UIDL applier fallback for non-engine or failed-delivery diagnostics.

These are acceptable only when the trace names the blocker or fallback explicitly. They should not be described as final runtime behavior, and successful future work should move stable checks toward trace gates, smoke commands or validation apps instead of growing hidden app-specific runtime branches.

## Maintenance rules

- Add new verification requirements as trace gates or smoke commands when possible.
- Keep real-window automation behind explicit `VAADIN_DESKTOP_*` environment switches.
- Keep compatibility mode and incomplete Flow over IPC boundaries visible in docs and trace names.
- Do not treat historical blocker gates as required predecessors for replacement-path gates unless the replacement still depends on that blocker evidence.
- When a gate is superseded, document the relationship here and in `DesktopTraceInspector.AcceptanceGate`.
