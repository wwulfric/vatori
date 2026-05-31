# Vaadin Desktop Framework v0

This repository explores a Vaadin Flow desktop runtime inspired by Tauri/Raycast-style process boundaries. The current deliverable is a real framework skeleton, not only the earlier mock IPC spike.

Framework v0 runs in **compatibility mode** by default: real Vaadin Flow views are served by embedded Spring Boot on `127.0.0.1`, while the desktop runtime API, host boundary, IPC envelope and trace logs are already separated so they can evolve toward Flow over IPC.

The repository now also contains the first `dev-ipc` runtime slice. It runs a constrained Flow-over-IPC smoke app without a localhost HTTP server in the core UI path, sends WebView click, native input `change`, native checkbox `change`, real BigDecimalField/TextField/Checkbox/Select Web Component `change` events and recovery resync as Flow-client-shaped UIDL RPC payloads, emits real server-side Vaadin UIDL and dispatches those messages through `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap, two ordered server-initiated pushes, event patches and recovery resync are consumed by the visible Flow client engine; a controlled duplicate/late push probe is ignored and triggers explicit resync diagnostics. Bootstrap also advertises real Vaadin `flow-client` classpath resources and the v1.4 frontend artifact manifest; the WebView requests and verifies the full advertised resource graph over IPC, imports and initializes `FlowBootstrap.js` and an augmented `FlowClient.js`, records executed vs verified-only resource classification, and performs a minimum no-HTTP lazy lookup probe for `Flow.js`. The component graph covers `com.vaadin.flow.component.textfield.BigDecimalField`, `TextField`, `Checkbox` and `Select` with real classpath or focused dev-bundle frontend artifacts, manifest import rewrites and strict trace evidence for resource resolution, event RPC, Java listener patch and visible DOM state. The v1.5-v1.46 resource slices add Blob-backed no-localhost staging, stable resource-path module identity, TextField side-effect split, generated/lazy relative dependency resolution, a real Flow `LoadMode.LAZY` dependency executed through the visible Flow DependencyLoader path, a production-bundle runtime registry staged execution attempt, a real UIDL generated `Flow.loadOnDemand(chunkId)` request mapped to the generated-flow-imports artifact, a v1.12 parser that proves the current Vaadin generated-flow-imports body is a no-op table with `chunkFileStatus=not-generated`, production owner boundaries from v1.13-v1.20, a v1.21 unified frontend resolver contract, a v1.22 Vaadin-shaped generated chunk-file positive execution fixture, a v1.23 production owner positive runtime fixture, a v1.24 frontend pipeline closure decision contract, a v1.25 packaged frontend baseline contract, generated lazy / current-app build output contracts from v1.26-v1.35, a v1.36 current app production owner import attempt, a v1.37 current app production owner component reuse path, a v1.38 current app owner component reuse matrix, a v1.39 current app owner early-import ordering contract, a v1.40 current app generated-imports owner side-effect contract, a v1.41 Select owner coverage contract, a v1.42 generated imports owner runtime contract, a v1.43 generated lazy SPI decision contract, a v1.44 packaging-time frontend inventory contract, a v1.45 packaging runtime handoff reuse contract and a v1.46 broader component resource matrix contract. v1.42 imports the current app production `generated-flow-imports-*.js` support artifact over the host-owned `vatori-resource` custom scheme before covered focused imports, records `currentAppGeneratedImportsOwnerRuntimeContractVersion=flow-ipc-current-app-generated-imports-owner-runtime-contract-v1_42`, reuses/skips focused imports for `vaadin-big-decimal-field`, `vaadin-checkbox`, `vaadin-select` and `vaadin-text-field`, records resolver handoff and lazy chunk SPI bridge metadata, and keeps the visible BigDecimalField/Checkbox/Select/TextField event paths working. v1.43 records `flow-ipc-generated-lazy-spi-decision-v1_43`, confirms the audited Vaadin lazy chunk surfaces do not expose a supported stable SPI, selects `packaging-time-build-frontend-staging`, and marks its chunk execution evidence as `host-decision-audit-probe-not-flow-client-spi`. v1.44 records `flow-ipc-packaging-time-frontend-inventory-v1_44`, selected `build-frontend` source/prod artifacts, sha256/byte-count integrity, allowed schemes, cache/cleanup policy and v1.42 runtime handoff evidence. v1.45 records `flow-ipc-packaging-runtime-handoff-reuse-v1_45`, module-relative source/target artifact policy, non-smoke-specific reusable contract flags, runtime handoff ready/satisfied evidence and both strict smoke plus framework validation app acceptance. v1.46 records `flow-ipc-broader-component-resource-matrix-v1_46` for framework-app `ComboBox`, `DatePicker` and `Dialog`, including target tags/classes/ids, current-app artifacts, resolver specifiers, custom-scheme/no-localhost proof and visible event/patch/DOM success. Real generated chunk parity, dev-mode/Vite parity, installer packaging, durable reconnect and deeper component/resource parity remain blockers. These are framework-resource boundary proofs, not full production bundle/dev-mode parity. The desktop capability gateway also has a bounded v1.1 file-transfer smoke boundary for controlled-path download writes and upload/select-file metadata reads.

v1.35 adds a current app production runtime ownership handoff contract. It gathers the current app source generated imports/chunk, production generated imports support artifact, production entry, stats and `prod.bundle` into a primary runtime candidate, records staged/handoff-ready evidence over the no-localhost path, and keeps owner side-effect/reuse explicitly false. This moves the remaining blocker to `current-app-production-runtime-ownership-handoff-pending`; v1.36 consumes that handoff for owner import and side-effect proof, and v1.37 consumes the owner side effect for the TextField component reuse path while full frontend/resource parity still remains open.

v1.36 adds `flow-ipc-current-app-production-owner-import-v1_36`. The Swift host waits until the current app production root and dependencies are staged, imports the production entry over `vatori-resource://`, and records side-effect proof for `window.Vaadin`, `window.Vaadin.Flow`, `Flow.loadOnDemand` and the expected Vaadin tags. The current accepted result is import and side-effect success with `currentAppProductionOwnerImportReuseSatisfied=false`, so the remaining frontend blocker has moved from owner import to owner-owned component runtime reuse.

v1.37 adds `flow-ipc-current-app-production-owner-component-reuse-v1_37`. The Swift host checks whether the current app production owner already registered `vaadin-text-field` before running the focused TextField dev-bundle import; when the owner owns the tag, trace records `currentAppProductionOwnerComponentReuseDecision=current-app-owner-reused`, skips `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js`, and still verifies the visible Flow `TextField` listener/patch/DOM path. v1.38 adds `flow-ipc-current-app-owner-component-reuse-matrix-v1_38`: the trace now classifies BigDecimalField, Checkbox, Select and TextField together, retaining TextField owner reuse while explicitly recording the earlier components as current-app-owner-not-ready focused fallbacks. v1.39 adds `flow-ipc-current-app-production-owner-early-import-v1_39`: the trace proves owner import now happens before covered focused imports, then narrows the current blocker to `current-app-owner-early-import-side-effect-not-reusable`. v1.40 adds `flow-ipc-current-app-generated-imports-owner-side-effect-v1_40`: the generated imports support artifact becomes the reusable owner source for BigDecimalField, Checkbox and TextField. v1.41 adds `flow-ipc-current-app-generated-imports-select-owner-coverage-v1_41`: the strict smoke app now includes ordinary Flow `Select`, and the generated imports support artifact reuses all four covered tags including `vaadin-select`. v1.42 adds `flow-ipc-current-app-generated-imports-owner-runtime-contract-v1_42`: manifest/request/response/stage/DOM diagnostics now treat that generated imports owner as an explicit resolver handoff contract, not just a side-effect candidate. v1.43 adds `flow-ipc-generated-lazy-spi-decision-v1_43`: it audits `AbstractUpdateImports`, `FrontendDependencies`, `ChunkInfo`, build-frontend output, production stats and `prod.bundle`, then selects packaging-time staging because no supported stable lazy chunk SPI is available. v1.44 adds `flow-ipc-packaging-time-frontend-inventory-v1_44`: it records framework-managed `build-frontend` inventory selection, integrity, loader/cache/cleanup policy and runtime handoff evidence. v1.45 adds `flow-ipc-packaging-runtime-handoff-reuse-v1_45`: the same module-relative handoff contract is accepted for strict smoke and framework validation app traces, including framework component interactions and desktop capability command-result evidence. v1.46 adds `flow-ipc-broader-component-resource-matrix-v1_46`: the framework validation app covers ordinary Flow `ComboBox`, `DatePicker` and `Dialog` over the no-localhost packaging runtime handoff and verifies visible event/patch/DOM success. The remaining blockers are real generated chunk parity, dev-mode/Vite, deeper component/resource parity, installer packaging and durable reconnect.

The v1.7 slice supersedes the v1.6 TextField blocker: TextField and its production `indexhtml` dependency use stable resource-path Blob module identity, so the production chunk is reused instead of re-imported under a second Blob URL, and a real Flow `TextField` event updates visible state to `Vaadin TextField: Flow IPC typed`. The v1.8 slice then extends the same staging resolver to the generated/lazy graph: the real Vaadin dev-bundle `generated-flow-imports-VW7hRaNw.js` chunk now rewrites its `./markdown-helpers-31I0A-hg.js` relative import to a stable staged module identity, imports the real helper body, and records both artifacts as executed `resourceStage` paths. v1.9 narrows the `Flow.loadOnDemand` gap to a diagnostic bridge / API registry blocker. v1.10 adds a separate real Flow `LoadMode.LAZY` JavaScript dependency: the visible Flow DependencyLoader requests `context://vatori-flow-ipc-lazy-v1_10.js`, the host resolves it through IPC, stages it as a Blob module URL and executes it. v1.10 also selects the production `indexhtml` chunk from the Vaadin dev-bundle stats registry and records a staged `execution-attempt` blocker for the remaining production runtime ownership layer. v1.11 captures a real bootstrap UIDL `Flow.loadOnDemand('<chunkId>')` expression, observes the visible Flow client trigger, maps the chunk request to `generated-flow-imports-VW7hRaNw.js`, stages/imports it once by artifact identity, and records the remaining blocker as `dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file`. v1.12 parses the real generated-flow-imports body, records `loadOnDemandTableShape=no-op-promise-resolve-0`, resolves the current outcome to `chunk-file-not-generated`, and keeps callback resolution distinct from actual chunk-file execution. v1.22 adds a repository Vaadin-shaped generated-flow-imports fixture plus chunk file, rewrites `./chunks/chunk-v1_22.js` to a staged chunk URL, calls fixture `Flow.loadOnDemand(chunkId)`, and records callback/side-effect success as `flow-ipc-loadondemand-chunk-file-execution-v1_22`. v1.13 through v1.20 progressively isolate production owner runtime, import, dependency, ordering, custom-scheme, side-effect and owner-owned reuse blockers. v1.23 adds a repository-local owner runtime fixture served through the production-owner `vatori-resource` namespace and records `owner-reused-positive-fixture` when the fixture exposes expected tags/globals. v1.24 then summarizes those proofs and remaining blockers as a closure decision contract. v1.36 proves current app production owner import and side effects; v1.37 proves TextField can consume that owner side effect and skip its focused import. Real app/build generated chunk-file parity, generalized owner-owned component runtime reuse and dev-mode/Vite execution remain explicit blockers.

v1.25 turns the v1.24 product-path decision into a packaged frontend baseline contract. Trace evidence records `flow-ipc-packaged-frontend-baseline-v1_25`, host-owned frontend inventory scope, preferred `custom-scheme-file-backed` loading, diagnostic Blob-only fallback, sha256/byte-count integrity and window-lifetime cleanup metadata across manifest/request/response/stage/DOM diagnostics. v1.26 adds `flow-ipc-generated-lazy-source-boundary-v1_26`: it names the Vaadin source path that generates lazy chunk tables, records the current no-op generated-flow-imports body and keeps the real generated chunk blocker visible. v1.27 adds `flow-ipc-generated-lazy-real-build-probe-v1_27`: it observes a real non-eager route / lazy JavaScript app input and records that the current generated-flow-imports output is still no-op. v1.28 adds `flow-ipc-generated-lazy-build-lifecycle-v1_28`: it records scanner status, update-imports/build status, redacted runtime artifact path, mtime, byte count, sha256, write target and the `runtime-using-stale-packaged-dev-bundle` failure layer. v1.29 adds `flow-ipc-generated-lazy-app-build-output-v1_29`: it checks current app build output discovery, records missing generated imports/chunk metadata, classifies fallback to the packaged dev-bundle and narrows the failure layer to `app-build-output-missing-generated-imports`. v1.30 adds `flow-ipc-current-app-frontend-build-output-v1_30`: with `VAADIN_DESKTOP_FLOW_IPC_PREPARE_FRONTEND=1`, `dev-ipc` runs an auditable Vaadin frontend build preflight and records command/status/log/output inventory. v1.31 adds `flow-ipc-frontend-build-preflight-availability-v1_31`: explicit online/local-repo preflight resolves `com.vaadin:vaadin-maven-plugin:25.1.5`, succeeds, and records output usability as scaffold-only/no generated imports. v1.32 adds `flow-ipc-current-app-build-frontend-output-v1_32`: explicit full `build-frontend` produces current app source generated imports/chunk, production generated output and `prod.bundle`. v1.33 adds `flow-ipc-current-app-generated-chunk-package-resolver-v1_33`: the first current-app generated chunk package edge is resolved and executed. v1.34 adds `flow-ipc-current-app-generated-imports-root-package-graph-v1_34`: the source generated imports root package imports are backed by the current app production generated imports support graph instead of a diagnostic placeholder. These are baselines for packaging-time staged frontend assets and generated-lazy build/SPI work; they are not full installer packaging, generated chunk parity, production bundle parity or dev-mode/Vite parity.

v1.35 adds `flow-ipc-current-app-production-runtime-ownership-v1_35`: the current app source/prod artifacts are selected as a primary runtime candidate, staged, and marked handoff-ready while `currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied=false` and `currentAppProductionRuntimeOwnershipOwnerReuseSatisfied=false`. v1.36 then imports that candidate as the current app production owner over the custom-scheme path and observes owner side effects. v1.37 reuses those side effects for `vaadin-text-field` and skips the focused TextField import; v1.38 records the four-component reuse matrix and proves the earlier components are still ordering fallbacks. v1.39 fixes that ordering by importing owner before the covered focused imports. v1.40 then switches the reusable side-effect source to the current app production generated-imports support artifact, reusing BigDecimalField/Checkbox/TextField; v1.41 adds Select to the strict smoke app build input and reuses BigDecimalField/Checkbox/Select/TextField from that same support artifact; v1.42 promotes that path into an explicit generated imports owner runtime contract with resolver handoff and lazy chunk SPI bridge evidence. v1.43 turns the lazy chunk SPI bridge metadata into an explicit decision contract and keeps the execution trigger honest as a host audit probe, not Flow-client SPI success.

## Quick Start

Requirements:

- JDK 21+.
- Maven 3.9+.
- macOS with Swift toolchain for the current system WebView host.
- macOS with Swift toolchain for the Swift/AppKit native host.
- If Maven is installed via SDKMAN, `bin/vaadin-desktop` sources SDKMAN automatically.

Run the counter example:

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

Run the independent smoke app:

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

Run the next-stage Swift native host path:

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

`dev-swift` builds `VaadinDesktopHost.app` and starts it through macOS LaunchServices, launches the Java Vaadin app as a sidecar, waits for the compatibility URL, opens an AppKit `NSWindow` with `WKWebView` and watches the Java process lifecycle. Host logs are tailed from `.build/vaadin-desktop-host-swift/host.out.log` and `.build/vaadin-desktop-host-swift/host.err.log`; the host also writes `.build/vaadin-desktop-host-swift/window-snapshot.png` after the WebView renders. Desktop API calls can now emit Host Protocol v0 command envelopes from Java to the host. The Flow UI transport still uses localhost HTTP, so this is a long-term host/runtime prototype, not final Flow over IPC yet.

Run the first Flow IPC runtime slice:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`dev-ipc` starts the same Swift/AppKit host with `transport=flow-ipc`, loads a local WKWebView shell, sends Flow client messages to the Java sidecar over `VFIPC1` stdin/stdout envelopes, and returns Java patches without loading `http://127.0.0.1`. Bootstrap payloads include real Vaadin server-side UIDL from `UidlWriter`; the runtime also emits two ordered server-initiated push UIDL updates and one controlled duplicate/late probe. The WebView bridge sends click, native input `change`, native checkbox `change`, `BigDecimalField` Web Component `change`, `Checkbox` Web Component `change` and ordering-recovery resync as UIDL RPC bodies with CSRF, `syncId`, `clientId`, event data and Flow node id, then Java handles them through `UidlRequestHandler`/`ServerRpcHandler`. Bootstrap advertises the Flow client classpath resources and the v1.4 frontend artifact manifest; the WebView requests and verifies the Flow resource graph, echoes artifact metadata in `resourceRequest`, imports `FlowBootstrap.js` and `FlowClient.js`, dynamically imports the real `vaadin-big-decimal-field.js` module with manifest-driven fallback import rewrites for missing npm imports and a focused extracted dev-bundle `defineCustomElement` helper, dynamically imports focused dev-bundle entries backed by Vaadin's production `indexhtml` chunk for the real `vaadin-checkbox` and `vaadin-select` definitions, performs a dynamic `flow-client-lazy-lookup` request for `Flow.js`, records executed / verified-only / intentionally-not-executed / execution-failed classifications, and consumes bootstrap, push, event and resync DOM updates through the visible Flow client engine. Desktop capability results now support `allow-all-dev`, `manifest-enforced` and `trace-only-dry-run` permission modes; the smoke app uses `manifest-enforced`, exercises AppKit `NSOpenPanel` cancellation, emits a manifest deny-path, writes a bounded download body to a controlled path and reads upload/select-file metadata from a controlled test file. The verification hook can emit a WebView-originated `error` envelope; the repository-local UIDL applier remains only as a diagnostic fallback.

For unattended real-window verification, add `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1` before the `dev-ipc` command. The host no-activate flag starts the app in the background and skips AppKit activation so verification runs do not steal keyboard focus. The click hook waits for the Flow client XHR path when the component-event hook is active; the file dialog hook exercises the AppKit `NSOpenPanel` path and cancels it automatically; the Web Component hook dispatches `BigDecimalField`, `Checkbox` and real `TextField` DOM `change` events after their frontend resources are imported; the shutdown hook closes the window after DOM/snapshot capture so the host log includes shutdown evidence.

Run the framework-shaped Flow IPC validation app:

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

This app keeps the strict fixture separate from a more ordinary developer project. It uses normal Flow Java APIs for a Button, `BigDecimalField`, `Checkbox`, `Select`, repeated list updates and manifest-gated desktop capability calls. Its semantic ids are only for optional diagnostics; the runtime's renderer promotion, layout, bridge installation and push/resync delivery must continue to use the generic Flow viewport and Flow metadata rather than app-specific ids.

For unattended framework-shape verification, use:

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

That gate verifies generic viewport promotion, first meaningful paint, two Web Component events, the Button apply event, UIDL patches, a list item text update and `window.setTitle` / `clipboard.writeText` / `notification.show` command-result traces in a less smoke-specific app.

Inspect the Flow IPC trace sample:

```bash
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1 docs/traces/flow-ipc-v1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_1 docs/traces/flow-ipc-v1_1.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_2 docs/traces/flow-ipc-v1_2.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3 docs/traces/flow-ipc-v1_3.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_4 docs/traces/flow-ipc-v1_4.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_5 docs/traces/flow-ipc-v1_5.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_6 docs/traces/flow-ipc-v1_6.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_7 docs/traces/flow-ipc-v1_7.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_8 docs/traces/flow-ipc-v1_8.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9-loadondemand docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_9 docs/traces/flow-ipc-v1_9.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-lazy-dependency docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10-production-runtime docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_10 docs/traces/flow-ipc-v1_10.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11-loadondemand-chunk docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_11 docs/traces/flow-ipc-v1_11.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12-loadondemand-chunk-file docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_12 docs/traces/flow-ipc-v1_12.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13-production-owner docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_13 docs/traces/flow-ipc-v1_13.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14-production-owner-import docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_14 docs/traces/flow-ipc-v1_14.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15-production-owner-dependencies docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_15 docs/traces/flow-ipc-v1_15.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16-production-owner-ordering docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_16 docs/traces/flow-ipc-v1_16.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17-resource-scheme docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_17 docs/traces/flow-ipc-v1_17.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18-production-owner-resource-scheme docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_18 docs/traces/flow-ipc-v1_18.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19-production-owner-side-effect docs/traces/flow-ipc-v1_19.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_19 docs/traces/flow-ipc-v1_19.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20-production-owner-owned-runtime docs/traces/flow-ipc-v1_20.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_20 docs/traces/flow-ipc-v1_20.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21-frontend-resolver-contract docs/traces/flow-ipc-v1_21.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_21 docs/traces/flow-ipc-v1_21.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22-generated-chunk-fixture docs/traces/flow-ipc-v1_22.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_22 docs/traces/flow-ipc-v1_22.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23-production-owner-positive-fixture docs/traces/flow-ipc-v1_23.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_23 docs/traces/flow-ipc-v1_23.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24-frontend-pipeline-closure docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_24 docs/traces/flow-ipc-v1_24.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25-packaged-frontend-baseline docs/traces/flow-ipc-v1_25.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_25 docs/traces/flow-ipc-v1_25.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26-generated-lazy-source-boundary docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_26 docs/traces/flow-ipc-v1_26.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27-generated-lazy-real-build-probe docs/traces/flow-ipc-v1_27.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_27 docs/traces/flow-ipc-v1_27.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28-generated-lazy-build-lifecycle docs/traces/flow-ipc-v1_28.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_28 docs/traces/flow-ipc-v1_28.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29-generated-lazy-app-build-output docs/traces/flow-ipc-v1_29.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_29 docs/traces/flow-ipc-v1_29.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30-current-app-frontend-build-output docs/traces/flow-ipc-v1_30.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_30 docs/traces/flow-ipc-v1_30.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31-frontend-build-preflight-availability docs/traces/flow-ipc-v1_31.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_31 docs/traces/flow-ipc-v1_31.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32-build-frontend-output docs/traces/flow-ipc-v1_32.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_32 docs/traces/flow-ipc-v1_32.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33-current-app-generated-chunk-package-edge docs/traces/flow-ipc-v1_33.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_33 docs/traces/flow-ipc-v1_33.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34-current-app-generated-imports-root-package-graph docs/traces/flow-ipc-v1_34.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_34 docs/traces/flow-ipc-v1_34.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35-current-app-production-runtime-ownership docs/traces/flow-ipc-v1_35.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_35 docs/traces/flow-ipc-v1_35.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36-current-app-production-owner-import docs/traces/flow-ipc-v1_36.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_36 docs/traces/flow-ipc-v1_36.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37-current-app-production-owner-component-reuse docs/traces/flow-ipc-v1_37.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_37 docs/traces/flow-ipc-v1_37.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38-current-app-owner-component-reuse-matrix docs/traces/flow-ipc-v1_38.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_38 docs/traces/flow-ipc-v1_38.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39-current-app-owner-early-import docs/traces/flow-ipc-v1_39.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_39 docs/traces/flow-ipc-v1_39.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_40-current-app-generated-imports-owner-side-effect docs/traces/flow-ipc-v1_40.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_40 docs/traces/flow-ipc-v1_40.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41-current-app-generated-imports-select-owner-coverage docs/traces/flow-ipc-v1_41.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_41 docs/traces/flow-ipc-v1_41.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_42-generated-imports-owner-runtime-contract docs/traces/flow-ipc-v1_42.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_42 docs/traces/flow-ipc-v1_42.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43-generated-lazy-spi-decision docs/traces/flow-ipc-v1_43.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_43 docs/traces/flow-ipc-v1_43.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44-packaging-frontend-inventory docs/traces/flow-ipc-v1_44.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_44 docs/traces/flow-ipc-v1_44.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-packaging-runtime-handoff-reuse docs/traces/flow-ipc-v1_45.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45 docs/traces/flow-ipc-v1_45.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse docs/traces/flow-ipc-v1_45-framework.ndjson
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_46-broader-component-resource-matrix docs/traces/flow-ipc-v1_46-framework.ndjson
```

Verify the first real Flow UIDL writer adapter slice:

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

These commands construct a minimal desktop-shaped `VaadinSession`/`UI` in process. The first verifies that Vaadin's real `UidlWriter` emits UIDL `changes`, `syncId` and bootstrap `Vaadin-Security-Key`; the second sends a real Flow event RPC body through `UidlRequestHandler`/`ServerRpcHandler`, verifies a click listener UIDL patch, and confirms invalid Flow IPC `csrfToken`/security key, stale `syncId` and out-of-order `clientId` values are rejected before dispatch. The third verifies that invalid Flow IPC envelope `sessionId` / `uiId` values are rejected before event handling. The fourth verifies that the runtime can read and respond with every advertised Vaadin `flow-client` classpath resource, including `FlowBootstrap.js`, `FlowClient.js`, `Flow.js`, `index.js`, `compile.properties` and the generated GWT client bundle.

Expected diagnostic output: `SLF4J(W): No SLF4J providers` is harmless in these smoke commands. The UIDL RPC and scope smoke commands intentionally send invalid tokens, stale IDs and invalid scope values; output such as `invalid csrfToken rejected`, `invalid securityKey rejected`, `stale syncId rejected`, `out-of-order clientId rejected` and `invalid Flow IPC sessionId/uiId rejected` means the negative-path assertions passed. In unattended `dev-ipc` verification, `VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` intentionally emits a `FLOW_IPC_CLIENT_ERROR ... webviewVerificationProbe` trace; success is determined by process exit code plus the matching `flow-ipc-trace-inspect` gate. The plain `dev`, `dev-swift` and `dev-ipc` commands are interactive desktop runs and stay alive until the window or app is closed; use the auto-shutdown forms above for repeatable smoke verification.

On macOS the original `dev` command still opens a native WKWebView desktop window by default. That path is now the macOS fallback/compatibility host.

Host selection:

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

## Modules

- `vaadin-desktop-core`: Java desktop runtime API.
- `vaadin-desktop-ipc`: IPC envelope and trace logging primitives.
- `vaadin-desktop-host`: macOS system WebView fallback, compatibility host and Java native bridge implementations.
- `vaadin-desktop-host-swift`: Swift/AppKit/WKWebView host for the macOS native runtime path.
- `vaadin-desktop-flow`: real Vaadin Flow/Spring Boot integration layer plus the first compiled UIDL transport adapter slice.
- `vaadin-desktop-devtools`: trace inspector for Flow IPC review traces.
- `vaadin-desktop-cli`: CLI scaffold; shell entry is `bin/vaadin-desktop`.
- `examples/counter-app`: real Vaadin Flow counter app.
- `examples/grid-app`: real Vaadin Flow Grid app.
- `examples/desktop-capabilities-app`: desktop runtime API example.
- `test-apps/vaadin-desktop-smoke-app`: independent test project using the framework.
- `test-apps/flow-over-ipc-smoke-app`: independent test project for the `dev-ipc` runtime slice.
- `test-apps/flow-over-ipc-framework-app`: developer-shaped Flow IPC validation app for checking generic shell/runtime behavior beyond the strict smoke fixture.
- `flow-transport-adapter-spike`: code-level Vaadin Flow transport boundary analysis and adapter shape.
- `prototypes/phase0-phase1-spike`: earlier non-HTTP mock transport and IPC validation.

## Documentation

- [Framework Architecture](docs/framework-architecture.md)
- [Getting Started](docs/getting-started.md)
- [Flow Integration](docs/flow-integration.md)
- [IPC Protocol](docs/ipc-protocol.md)
- [IPC Protocol v1](docs/ipc-protocol-v1.md)
- [Flow over IPC Runtime](docs/flow-over-ipc-runtime.md)
- [Flow Transport Adapter](docs/flow-transport-adapter.md)
- [Acceptance and Smoke Tools](docs/acceptance-tools.md)
- [Native Host](docs/native-host.md)
- [Roadmap](docs/roadmap.md)
- [Original Design Package](docs/flow-over-ipc-design.md)

## Current Status

Implemented:

- Real Vaadin Flow application modules.
- Framework-level Java desktop API: window title, notification, clipboard, file dialog, bounded file transfer and menu facade.
- macOS system WebView launcher: Spring Boot starts Vaadin on random localhost port; a generated WKWebView `.app` opens the desktop window.
- Swift/AppKit host: starts Java sidecar, opens WKWebView, traces Host Protocol v0 and cleans up the sidecar on window shutdown.
- Flow IPC runtime slice: starts a no-HTTP smoke app, dispatches Button click over IPC through `UidlRequestHandler`/`ServerRpcHandler`, generates real server-side UIDL, sends desktop API calls as `VFIPC1 command`, receives `capabilityResult`, and has the visible Flow client engine consume bootstrap, ordered push, event and recovery resync UIDL.
- Flow IPC push ordering smoke: Java emits two ordered server push UIDL updates plus a controlled duplicate/late probe; the WebView records delivered/ignored order and triggers explicit ordering-recovery resync.
- Flow client resource loading probe: `dev-ipc` advertises Vaadin `flow-client` classpath resources, the WebView requests the full advertised resource graph through `resourceRequest`, Java responds with redacted `resourceResponse` traces, diagnostics verify all responses plus real Flow bootstrap and GWT client module import/init/binding, and v1.1 DOM evidence classifies resources as executed, verified-only, intentionally not executed or execution-failed. The same channel now includes a dynamic `flow-client-lazy-lookup` probe for `Flow.js`.
- Real Vaadin Web Component and frontend staging slice: the smoke app uses `com.vaadin.flow.component.textfield.BigDecimalField`, `TextField`, `com.vaadin.flow.component.checkbox.Checkbox` and `com.vaadin.flow.component.select.Select`. The WebView requests the real `vaadin-big-decimal-field.js` classpath frontend module plus focused Vaadin dev-bundle entries for TextField, Checkbox and Select over IPC, stages the real artifact bodies as Blob module URLs or host-owned custom-scheme modules, records every staged artifact with `resourceStage`, and verifies Java listener -> `UidlRequestHandler` patch -> visible Flow client DOM states `Vaadin BigDecimalField: 42.5`, `Vaadin TextField: Flow IPC typed`, `Vaadin Checkbox: checked` and `Vaadin Select: audit`. The v1.7-v1.46 gates cover stable TextField module identity, generated/lazy relative dependency execution, Flow `LoadMode.LAZY`, real UIDL `Flow.loadOnDemand(chunkId)` mapping, generated-flow-imports no-op table proof, production owner metadata/import/dependency/order/resource-scheme/side-effect/owner-owned runtime boundaries, frontend resolver contracts, positive fixtures, current app build-output discovery, build preflight materialization, explicit preflight availability, full build-frontend output discovery, the first current-app generated chunk package edge, the current-app generated imports root package graph, the current-app production runtime ownership handoff, current-app production owner import / side-effect proof, TextField reuse, four-component matrix, early-import ordering, generated-imports owner side-effect reuse, Select owner coverage, the generated imports owner runtime contract, the generated lazy SPI decision contract, the packaging-time frontend inventory contract, the packaging runtime handoff reuse contract and the broader framework component matrix. v1.46 records ComboBox/DatePicker/Dialog target metadata plus visible event/patch/DOM success; real app/build generated chunk-file productization, generalized owner-owned component runtime reuse, dev-mode/Vite execution, installer packaging and durable reconnect remain blockers.
- Flow IPC v1.7 TextField execution: the TextField dev-bundle entry and its production `indexhtml` dependency are staged with stable module identity, the v1.6 duplicate custom element blocker is resolved, and the smoke app verifies a real Flow `TextField` Java listener producing `Vaadin TextField: Flow IPC typed` through the visible Flow client event/patch path. The legacy TextField shim is still present only as a fallback diagnostic.
- Flow IPC file-transfer smoke: Java sends `fileTransfer.download` and `fileTransfer.selectUploadFile` commands through the native capability gateway; Swift writes a bounded body to a controlled test path, reads selected-file metadata and sampled byte count, redacts body content in traces and reports typed `capabilityResult` metadata.
- Flow IPC framework validation app: `test-apps/flow-over-ipc-framework-app` runs a less smoke-specific Flow view with ordinary Java APIs, BigDecimalField, Checkbox, Select, ComboBox, DatePicker, Dialog, a v1.10 lazy dependency declaration, repeated list updates and manifest-gated desktop capability calls. It is a framework-shape validation target; strict historical gates through v1.45 still live on the smoke fixture, while `--require-flow-ipc-v1_45-framework-packaging-runtime-handoff-reuse` and `--require-flow-ipc-v1_46-broader-component-resource-matrix` validate packaging runtime handoff reuse and the broader component matrix against the framework-shaped app.
- UIDL adapter smoke: drives Vaadin's real server-side `UidlWriter` from an in-process desktop-shaped session/UI boundary.
- UIDL RPC smoke: drives a real Flow event RPC through `UidlRequestHandler`/`ServerRpcHandler` and verifies a listener-produced UIDL patch.
- Host Protocol v0 stdout envelope for Java desktop commands.
- Flow transport adapter spike with concrete Vaadin source references for UIDL/event/patch boundaries.
- Compatibility host fallback for headless/browser modes.
- IPC trace shape and command envelope.
- Independent smoke app that depends on the framework modules.

Not implemented yet:

- Broad Flow component and event coverage beyond Button, native input/checkbox and the current `BigDecimalField` / `TextField` / `Checkbox` / `Select` validation slice.
- Full Flow client UIDL parity, real app/build generated `Flow.loadOnDemand` chunk-file parity, dev mode replacement and generalized owner-owned component runtime reuse beyond the current classification/lazy-lookup/frontend-staging/custom-scheme diagnostics.
- Durable Flow push/reconnect semantics beyond the controlled duplicate/late ordering recovery smoke.
- Full Vaadin Upload/Download parity, binary streaming, progress and sandbox-safe file grants.
- Production native capability permission model.
- Production packaging, signing, auto-update.
