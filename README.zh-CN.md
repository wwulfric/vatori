# Vaadin Desktop Framework v0

本仓库探索一个受 Tauri/Raycast 风格进程边界启发的 Vaadin Flow 桌面 runtime。当前交付物是一个真实框架骨架，不只是早期的 mock IPC spike。

Framework v0 默认运行在 **compatibility mode**：真实 Vaadin Flow view 由嵌入式 Spring Boot 在 `127.0.0.1` 上提供服务，同时 desktop runtime API、host 边界、IPC envelope 和 trace log 已经分离出来，以便后续演进为 Flow over IPC。

仓库现在也包含第一个 `dev-ipc` runtime 切片。它运行一个受限的 Flow-over-IPC smoke app，核心 UI 路径不使用 localhost HTTP server；它把 WebView click、原生 input `change`、原生 checkbox `change`、真实 BigDecimalField / TextField / Checkbox Web Component `change` 和 recovery resync 作为 Flow-client-shaped UIDL RPC payload 发送，发出真实 server-side Vaadin UIDL，并通过 `UidlRequestHandler` / `ServerRpcHandler` dispatch 这些消息。Bootstrap、两个 ordered server-initiated push、event patch 和 recovery resync 由可见 Flow client engine 消费；一个受控 duplicate/late push probe 会被忽略并触发显式 resync diagnostics。Bootstrap 还会声明真实 Vaadin `flow-client` classpath resources 和 v1.4 frontend artifact manifest；WebView 通过 IPC 请求并验证完整 advertised resource graph，在 `resourceRequest` 中回显 artifact family、module specifier、dependency graph、execution policy、resolver 和 status metadata，导入并初始化 `FlowBootstrap.js` 和增强版 `FlowClient.js`，记录 executed vs verified-only resource classification，并对 `Flow.js` 执行最小 no-HTTP lazy lookup probe。component graph 覆盖 `com.vaadin.flow.component.textfield.BigDecimalField`、`TextField`、`Checkbox` 以及 framework app 的 `Select` 路径，使用真实 classpath 或聚焦 dev-bundle frontend artifact、manifest import rewrite 和严格 trace 证明 resource resolution、event RPC、Java listener patch 与可见 DOM 状态。v1.5-v1.38 resource 切片加入 Blob-backed no-localhost staging、stable resource-path module identity、TextField side-effect split、generated/lazy relative dependency resolution、真实 Flow `LoadMode.LAZY` dependency、production-bundle runtime registry staged execution attempt、真实 UIDL generated `Flow.loadOnDemand(chunkId)` 到 generated-flow-imports artifact 的映射、v1.12 no-op table / `chunkFileStatus=not-generated` 证据、v1.13-v1.20 production owner 边界、v1.21 统一 frontend resolver contract、v1.22 Vaadin 同形状 generated chunk-file 正向执行 fixture、v1.23 production owner positive runtime fixture、v1.24 frontend pipeline closure decision contract、v1.25 packaged frontend baseline contract、v1.26-v1.35 generated lazy / current-app build output contracts、v1.36 current app production owner import attempt、v1.37 current app production owner component reuse path，以及 v1.38 current app owner component reuse matrix。v1.36 通过 host-owned `vatori-resource` custom scheme 导入当前 app production entry 和 dependency graph，记录 `currentAppProductionOwnerImported=true` 并观察到 production owner side effect；v1.37 将该 side effect 用于 `vaadin-text-field` 复用，跳过 focused TextField dev-bundle import，并保持可见 Flow `TextField` event path 成功。v1.38 在同一个矩阵中记录 covered component tags：TextField 复用 owner，BigDecimalField、Checkbox、Select 则被明确分类为 focused fallback，因为 current app owner 在这些 focused import 前还未 ready。更早组件执行顺序的泛化 owner reuse、dev-mode/Vite parity 和更广组件/资源覆盖仍是 blocker。这些是框架资源边界证明，不是 broad component/resource coverage 或完整 production bundle/dev-mode parity。Desktop capability gateway 也有一个受限 v1.1 file-transfer smoke 边界，用于 controlled-path download 写入和 upload/select-file metadata 读取。

v1.35 新增 current app production runtime ownership handoff contract。它把当前 app source generated imports/chunk、production generated imports support artifact、production entry、stats 和 `prod.bundle` 收束为 primary runtime candidate，在 no-localhost 路径上记录 staged / handoff-ready 证据，同时明确 owner side-effect/reuse 仍为 false。v1.36 接收这个 handoff，完成 owner import 和 side-effect proof；v1.37 再把 owner side effect 用于 TextField component reuse，但完整 frontend/resource parity 仍未完成。

v1.36 新增 `flow-ipc-current-app-production-owner-import-v1_36`。Swift host 等待当前 app production root 和 dependencies 都完成 staging 后，通过 `vatori-resource://` 导入 production entry，并记录 `window.Vaadin`、`window.Vaadin.Flow`、`Flow.loadOnDemand` 和预期 Vaadin tags 的 side-effect proof。当前 accepted result 是 import 和 side-effect success，但 `currentAppProductionOwnerImportReuseSatisfied=false`，因此剩余 frontend blocker 已从 owner import 推进到 owner-owned component runtime reuse。

v1.37 新增 `flow-ipc-current-app-production-owner-component-reuse-v1_37`。Swift host 在 focused TextField dev-bundle import 之前检查 current app production owner 是否已注册 `vaadin-text-field`；如果 owner 拥有该 tag，trace 记录 `currentAppProductionOwnerComponentReuseDecision=current-app-owner-reused`，跳过 `META-INF/vatori/dev-bundle-entry/vaadin-text-field.js`，并继续验证可见 Flow `TextField` listener / patch / DOM 路径。v1.38 新增 `flow-ipc-current-app-owner-component-reuse-matrix-v1_38`：trace 会把 BigDecimalField、Checkbox、Select 和 TextField 一起分类，保留 TextField owner reuse，同时明确记录前三个更早组件仍是 current-app-owner-not-ready focused fallback。剩余 blocker 是更早组件执行顺序的泛化 owner reuse、dev-mode/Vite 和更广资源 parity。

v1.7 切片已经取代 v1.6 TextField blocker：TextField entry 和 production `indexhtml` dependency 使用 stable resource-path Blob module identity，production chunk 会被复用，真实 Flow `TextField` event 会把可见状态更新为 `Vaadin TextField: Flow IPC typed`。v1.8 切片继续把同一个 staging resolver 推进到 generated/lazy graph：真实 Vaadin dev-bundle `generated-flow-imports-VW7hRaNw.js` chunk 会把 `./markdown-helpers-31I0A-hg.js` relative import 重写到 stable staged module identity，导入真实 helper body，并把两个 artifact 都记录为 executed `resourceStage` 路径。v1.9 把 `Flow.loadOnDemand` 缺口收敛为 diagnostic bridge / API registry blocker。v1.10 增加一条独立的真实 Flow `LoadMode.LAZY` JavaScript dependency：可见 Flow DependencyLoader 请求 `context://vatori-flow-ipc-lazy-v1_10.js`，host 通过 IPC resolve、stage 成 Blob module URL 并执行。v1.10 还会从 Vaadin dev-bundle stats registry 中选择 production `indexhtml` chunk，并为剩余 production runtime ownership 层记录 staged `execution-attempt` blocker。v1.11 捕获真实 bootstrap UIDL `Flow.loadOnDemand('<chunkId>')` expression，观察可见 Flow client 触发，映射到 `generated-flow-imports-VW7hRaNw.js`，按 artifact identity stage/import，并把剩余 blocker 收敛为 `dev-bundle-generated-imports-loadondemand-no-op-no-chunk-file`。v1.12 解析真实 generated-flow-imports body，记录 `loadOnDemandTableShape=no-op-promise-resolve-0`，把当前结果精确为 `chunk-file-not-generated`，并把 callback resolution 和真实 chunk-file execution 区分开。v1.22 增加 repository Vaadin 同形状 generated-flow-imports fixture 和 chunk file，把 `./chunks/chunk-v1_22.js` 重写到 staged chunk URL，调用 fixture `Flow.loadOnDemand(chunkId)`，并以 `flow-ipc-loadondemand-chunk-file-execution-v1_22` 记录 callback/side-effect success。v1.13 到 v1.20 持续拆解 production owner runtime、import、dependency、ordering、custom-scheme、side-effect 和 owner-owned reuse blocker。v1.23 增加 repository-local owner runtime fixture，在暴露 expected tags/globals 时记录 `owner-reused-positive-fixture`。v1.24 把这些 proof 和 remaining blocker 收束成 closure decision contract。v1.25 把 v1.24 的产品路径决策推进成 packaged frontend baseline contract。v1.26 进一步记录 `flow-ipc-generated-lazy-source-boundary-v1_26`、Vaadin lazy chunk source mechanism、当前 no-op table 证据和 `build-spi-required-before-real-app-chunk-parity` 决策。v1.27 记录 `flow-ipc-generated-lazy-real-build-probe-v1_27`，确认真实 route/lazy JavaScript 输入被观察到。v1.28 记录 `flow-ipc-generated-lazy-build-lifecycle-v1_28`、scanner/build/runtime 状态、runtime artifact mtime/bytes/sha256 和 `runtime-using-stale-packaged-dev-bundle` failure layer。v1.29 记录 `flow-ipc-generated-lazy-app-build-output-v1_29`、当前 app build output discovery、missing generated imports/chunk、fallback reason 和 `app-build-output-missing-generated-imports` failure layer。v1.30 记录 `flow-ipc-current-app-frontend-build-output-v1_30`、build preflight command/status/log、output roots、inventory 和 `vaadin-frontend-build-step-unavailable` failure layer。v1.31 记录 `flow-ipc-frontend-build-preflight-availability-v1_31`、plugin resolution、goal result、output usability 和 `frontend-build-plugin-resolved-output-incomplete` failure layer。v1.32 记录 `flow-ipc-current-app-build-frontend-output-v1_32`、full build-frontend source/prod output。v1.33 记录 `flow-ipc-current-app-generated-chunk-package-resolver-v1_33` 和第一个 current-app generated chunk package edge 执行。v1.34 记录 `flow-ipc-current-app-generated-imports-root-package-graph-v1_34`，source generated imports root package imports 由当前 app production generated imports support graph 承载，不再使用 diagnostic placeholder。v1.36 已证明 current app production owner import 和 side effect。真实 app/build generated chunk-file parity、owner-owned component runtime reuse 和 dev-mode/Vite execution 仍然是明确 blocker。

v1.35 记录 `flow-ipc-current-app-production-runtime-ownership-v1_35`：当前 app source/prod artifacts 已被选为 primary runtime candidate、完成 staging 并标记 handoffReady，同时 `currentAppProductionRuntimeOwnershipOwnerSideEffectSatisfied=false` 和 `currentAppProductionRuntimeOwnershipOwnerReuseSatisfied=false`。v1.36 随后通过 custom-scheme path 把这个 candidate 作为 current app production owner 导入，并观察到 owner side effect。v1.37 复用这些 side effect 提供 `vaadin-text-field`；v1.38 记录四组件 reuse matrix 并证明更早组件仍是 ordering fallback，因此 component owner reuse 还没有泛化到更早组件执行顺序。

## 快速开始

要求：

- JDK 21+。
- Maven 3.9+。
- 当前 system WebView host 需要带 Swift toolchain 的 macOS。
- Swift/AppKit native host 需要带 Swift toolchain 的 macOS。
- 如果 Maven 通过 SDKMAN 安装，`bin/vaadin-desktop` 会自动 source SDKMAN。

运行 counter 示例：

```bash
bash bin/vaadin-desktop dev examples/counter-app
```

运行独立 smoke app：

```bash
bash bin/vaadin-desktop dev test-apps/vaadin-desktop-smoke-app
```

运行下一阶段 Swift native host 路径：

```bash
bash bin/vaadin-desktop dev-swift test-apps/vaadin-desktop-smoke-app
```

`dev-swift` 会构建 `VaadinDesktopHost.app` 并通过 macOS LaunchServices 启动它，把 Java Vaadin app 作为 sidecar 启动，等待 compatibility URL，打开带 `WKWebView` 的 AppKit `NSWindow`，并观察 Java 进程生命周期。Host 日志会从 `.build/vaadin-desktop-host-swift/host.out.log` 和 `.build/vaadin-desktop-host-swift/host.err.log` tail 出来；WebView 渲染后，host 还会写入 `.build/vaadin-desktop-host-swift/window-snapshot.png`。Desktop API call 现在可以从 Java 向 host 发出 Host Protocol v0 command envelope。Flow UI transport 仍然使用 localhost HTTP，所以这是长期 host/runtime prototype，还不是最终 Flow over IPC。

运行第一个 Flow IPC runtime 切片：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-smoke-app
```

`dev-ipc` 会用 `transport=flow-ipc` 启动同一个 Swift/AppKit host，加载本地 WKWebView shell，通过 `VFIPC1` stdin/stdout envelope 将 Flow client message 发送给 Java sidecar，并在不加载 `http://127.0.0.1` 的情况下返回 Java patch。Bootstrap payload 包含来自 `UidlWriter` 的真实 Vaadin server-side UIDL；runtime 还会发出两个 ordered server-initiated push UIDL update 和一个受控 duplicate/late probe。WebView bridge 会把 click、原生 input `change`、原生 checkbox `change`、`BigDecimalField` Web Component `change`、`Checkbox` Web Component `change` 和 ordering-recovery resync 作为 UIDL RPC body 发送，body 包含 CSRF、`syncId`、`clientId`、event data 和 Flow node id，然后 Java 通过 `UidlRequestHandler` / `ServerRpcHandler` 处理。Bootstrap 会声明 Flow client classpath resources 和 v1.4 frontend artifact manifest；WebView 请求并验证完整 advertised resource graph，在每个可审计 `resourceRequest` 上回显 artifact metadata，导入 `FlowBootstrap.js` 和 `FlowClient.js`，通过 manifest-driven import rewrite 和 extracted dev-bundle `defineCustomElement` helper 导入真实 `vaadin-big-decimal-field.js` module，动态导入由 Vaadin production `indexhtml` chunk 支撑的聚焦 dev-bundle 入口来定义真实 `vaadin-checkbox` 和 `vaadin-select`，对 `Flow.js` 发起动态 `flow-client-lazy-lookup` request，记录 executed / verified-only / intentionally-not-executed / execution-failed classification，并通过可见 Flow client engine 消费 bootstrap、push、event 和 resync DOM update。Desktop capability result 现在支持 `allow-all-dev`、`manifest-enforced` 和 `trace-only-dry-run` permission mode；smoke app 使用 `manifest-enforced`，触发 AppKit `NSOpenPanel` cancellation，发出 manifest deny-path，把受限 download body 写入 controlled path，并从 controlled test file 读取 upload/select-file metadata。Verification hook 可以发出一个 WebView-originated `error` envelope；仓库本地 UIDL applier 只保留为诊断 fallback。

无人值守真实窗口验证时，在 `dev-ipc` 命令前加上 `VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_CLICK=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_VAADIN_TEXT_FIELD_REAL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_RESYNC=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FILE_DIALOG_CANCEL=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1`。host no-activate 标记会让应用在后台启动并跳过 AppKit 激活，避免验证运行抢走键盘焦点。启用 Web Component hook 时，click hook 会等待 Flow client XHR path；file dialog hook 会触发 AppKit `NSOpenPanel` 路径并自动取消；Web Component hook 会在组件 frontend resources 导入后触发 `BigDecimalField`、`Checkbox` 和真实 `TextField` DOM `change` event；shutdown hook 会在 DOM/snapshot capture 后关闭窗口，使 host log 包含 shutdown evidence。

运行框架形态的 Flow IPC validation app：

```bash
bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
```

这个 app 把 strict fixture 和更普通的开发者项目分开。它用普通 Flow Java API 组合 Button、`BigDecimalField`、`Checkbox`、`Select`、重复列表更新和 manifest-gated desktop capability call。它的语义化 id 只用于可选 diagnostics；runtime 的 renderer promotion、layout、bridge 安装和 push/resync delivery 必须继续使用 generic Flow viewport 与 Flow metadata，而不是依赖 app-specific id。

无人值守的 framework-shape 验证可以使用：

```bash
VAADIN_DESKTOP_HOST_NO_ACTIVATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_FRAMEWORK_UPDATE=1 VAADIN_DESKTOP_FLOW_IPC_AUTO_SHUTDOWN=1 bash bin/vaadin-desktop dev-ipc test-apps/flow-over-ipc-framework-app
bash bin/flow-ipc-trace-inspect --require-flow-ipc-v1_3-framework .build/vaadin-desktop-host-swift/host.out.log
```

这个 gate 会验证 generic viewport promotion、first meaningful paint、两个 Web Component event、Button apply event、UIDL patch、列表项文本更新，以及 `window.setTitle` / `clipboard.writeText` / `notification.show` 的 command-result trace。

检查 Flow IPC trace 示例：

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
```

验证第一个真实 Flow UIDL writer adapter 切片：

```bash
bash bin/uidl-adapter-smoke
bash bin/uidl-rpc-smoke
bash bin/flow-ipc-scope-smoke
bash bin/flow-client-resource-smoke
```

这些命令会在进程内构造一个最小 desktop-shaped `VaadinSession` / `UI`。第一个命令验证 Vaadin 的真实 `UidlWriter` 会发出 UIDL `changes`、`syncId` 和 bootstrap `Vaadin-Security-Key`；第二个命令把真实 Flow event RPC body 通过 `UidlRequestHandler` / `ServerRpcHandler` 发送，验证 click listener UIDL patch，并确认无效 Flow IPC `csrfToken` / security key、过期 `syncId` 和乱序 `clientId` 会在 dispatch 前被拒绝。第三个命令验证无效 Flow IPC envelope `sessionId` / `uiId` 会在 event handling 前被拒绝。第四个命令验证 runtime 可以读取并响应每一个 advertised Vaadin `flow-client` classpath resource，包括 `FlowBootstrap.js`、`FlowClient.js`、`Flow.js`、`index.js`、`compile.properties` 和生成的 GWT client bundle。

预期诊断输出：这些 smoke 命令里的 `SLF4J(W): No SLF4J providers` 是无害警告。UIDL RPC 和 scope smoke 会故意发送无效 token、过期 ID 和无效 scope；如果输出 `invalid csrfToken rejected`、`invalid securityKey rejected`、`stale syncId rejected`、`out-of-order clientId rejected` 或 `invalid Flow IPC sessionId/uiId rejected`，表示负向断言通过。无人值守 `dev-ipc` 验证里，`VAADIN_DESKTOP_FLOW_IPC_AUTO_ERROR=1` 会故意发出一条 `FLOW_IPC_CLIENT_ERROR ... webviewVerificationProbe` trace；是否成功以进程退出码和对应 `flow-ipc-trace-inspect` gate 为准。普通 `dev`、`dev-swift` 和 `dev-ipc` 命令是交互式桌面运行，会一直保持到窗口或应用关闭；需要可重复 smoke 验证时，请使用上面的 auto-shutdown 形式。

在 macOS 上，原始 `dev` 命令默认仍会打开原生 WKWebView desktop window。该路径现在是 macOS fallback / compatibility host。

Host 选择：

```bash
VAADIN_DESKTOP_HOST=system-webview bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=headless bash bin/vaadin-desktop dev examples/counter-app
VAADIN_DESKTOP_HOST=browser bash bin/vaadin-desktop dev examples/counter-app
```

## 模块

- `vaadin-desktop-core`：Java desktop runtime API。
- `vaadin-desktop-ipc`：IPC envelope 和 trace logging primitives。
- `vaadin-desktop-host`：macOS system WebView fallback、compatibility host 和 Java native bridge 实现。
- `vaadin-desktop-host-swift`：面向 macOS native runtime 路径的 Swift/AppKit/WKWebView host。
- `vaadin-desktop-flow`：真实 Vaadin Flow/Spring Boot integration layer，加上第一个已编译 UIDL transport adapter 切片。
- `vaadin-desktop-devtools`：用于 Flow IPC review trace 的 trace inspector。
- `vaadin-desktop-cli`：CLI scaffold；shell 入口是 `bin/vaadin-desktop`。
- `examples/counter-app`：真实 Vaadin Flow counter app。
- `examples/grid-app`：真实 Vaadin Flow Grid app。
- `examples/desktop-capabilities-app`：desktop runtime API 示例。
- `test-apps/vaadin-desktop-smoke-app`：使用 framework 的独立测试项目。
- `test-apps/flow-over-ipc-smoke-app`：面向 `dev-ipc` runtime 切片的独立测试项目。
- `test-apps/flow-over-ipc-framework-app`：面向 strict smoke fixture 之外的 developer-shaped Flow IPC validation app，用来检查 generic shell/runtime 行为。
- `flow-transport-adapter-spike`：代码级 Vaadin Flow transport 边界分析和 adapter 形态。
- `prototypes/phase0-phase1-spike`：更早的非 HTTP mock transport 和 IPC 验证。

## 文档

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

## 当前状态

已实现：

- 真实 Vaadin Flow application modules。
- Framework-level Java desktop API：window title、notification、clipboard、file dialog、bounded file transfer 和 menu facade。
- macOS system WebView launcher：Spring Boot 在随机 localhost port 启动 Vaadin；生成的 WKWebView `.app` 打开 desktop window。
- Swift/AppKit host：启动 Java sidecar，打开 WKWebView，记录 Host Protocol v0 trace，并在窗口关闭时清理 sidecar。
- Flow IPC runtime 切片：启动 no-HTTP smoke app，通过 IPC 和 `UidlRequestHandler` / `ServerRpcHandler` dispatch Button click，生成真实 server-side UIDL，把 desktop API call 作为 `VFIPC1 command` 发送，接收 `capabilityResult`，并让可见 Flow client engine 消费 bootstrap、ordered push、event 和 recovery resync UIDL。
- Flow IPC push ordering smoke：Java 发出两个 ordered server push UIDL update 加一个受控 duplicate/late probe；WebView 记录 delivered/ignored order，并触发显式 ordering-recovery resync。
- Flow client resource loading probe：`dev-ipc` 声明 Vaadin `flow-client` classpath resources，WebView 通过 `resourceRequest` 请求完整 advertised resource graph，Java 用 redacted `resourceResponse` trace 响应，diagnostics 验证所有 response 加上真实 Flow bootstrap 和 GWT client module import/init/binding；v1.1 DOM evidence 将资源分类为 executed、verified-only、intentionally not executed 或 execution-failed。同一 channel 现在还包含面向 `Flow.js` 的动态 `flow-client-lazy-lookup` probe。
- Flow IPC v1.4-v1.21 frontend artifact manifest：resource graph 现在记录 artifact family、origin、module specifier、dependencyOf、executionPolicy、resolver、artifact status 和 strict blocker，覆盖 flow-client classpath、component classpath frontend、npm/frontend、theme/static、production bundle、dev-mode bundle、generated/lazy resource 和 flow-lazy-dependency。真实窗口 `webview.dom` 会记录 v1.8-v1.20 的 generated/lazy、production owner 和 custom-scheme evidence，也会记录 v1.21 `frontendResolverContractVersion=flow-ipc-frontend-resolver-contract-v1_21`、observed families、body integrity、stable identity、Blob/custom-scheme scheme usage、verified-only families、strict blocker families、accepted gaps 和 `frontendResolverContractComplete=true`。
- 真实 Vaadin Web Component 和 frontend staging 切片：smoke app 使用 `com.vaadin.flow.component.textfield.BigDecimalField`、`TextField` 和 `com.vaadin.flow.component.checkbox.Checkbox`；framework app 也覆盖 `Select`。WebView 通过 IPC 请求真实 `vaadin-big-decimal-field.js` classpath frontend module 以及聚焦的 Vaadin dev-bundle TextField / Checkbox / Select 入口，把真实 artifact body stage 成 Blob module URL，执行 BigDecimalField、TextField 和 Checkbox / Select production class entry，对每个 staged artifact 记录 `resourceStage`，并验证 Java listener -> `UidlRequestHandler` patch -> 可见 Flow client DOM 状态 `Vaadin BigDecimalField: 42.5`、`Vaadin TextField: Flow IPC typed` 和 `Vaadin Checkbox: checked`。v1.7-v1.38 gates 覆盖 stable TextField module identity、generated/lazy relative dependency execution、Flow `LoadMode.LAZY`、real UIDL `Flow.loadOnDemand(chunkId)` mapping、generated-flow-imports no-op table proof、production owner metadata/import/dependency/order/resource-scheme/side-effect/owner-owned runtime boundaries、frontend resolver contracts、positive fixtures、current app build-output discovery、build preflight materialization、explicit preflight availability、full build-frontend output discovery、第一个 current-app generated chunk package edge、current-app generated imports root package graph、current-app production runtime ownership handoff、current-app production owner import / side-effect proof、current-app owner 对 `vaadin-text-field` 的 component reuse，以及 current-app owner 四组件 reuse matrix。v1.38 记录 TextField owner reuse 加 BigDecimalField / Checkbox / Select focused fallback ordering evidence；真实 app/build generated chunk-file parity、泛化 owner reuse 和 dev-mode/Vite execution 仍是 blocker。
- Flow IPC v1.7 TextField execution：TextField dev-bundle entry 和 production `indexhtml` dependency 通过 stable module identity stage，v1.6 duplicate custom element blocker 已解决，smoke app 验证真实 Flow `TextField` Java listener 通过可见 Flow client event/patch 路径产生 `Vaadin TextField: Flow IPC typed`。legacy TextField shim 仍存在，但只作为 fallback diagnostic。
- Flow IPC file-transfer smoke：Java 通过 native capability gateway 发送 `fileTransfer.download` 和 `fileTransfer.selectUploadFile` command；Swift 把受限 body 写入 controlled test path，读取 selected-file metadata 和 sampled byte count，在 trace 中 redacts body content，并报告 typed `capabilityResult` metadata。
- Flow IPC framework validation app：`test-apps/flow-over-ipc-framework-app` 运行一个更少 smoke-specific 的 Flow view，使用普通 Java API、三个真实 Vaadin Web Component（`BigDecimalField`、`Checkbox`、`Select`）、v1.10 lazy dependency 声明、重复列表更新和 manifest-gated desktop capability call。它是 framework-shape validation target；strict v1/v1.1/v1.2/v1.3/v1.4/v1.5/v1.6/v1.7/v1.8/v1.9/v1.10/v1.11/v1.12/v1.13/v1.14/v1.15/v1.16/v1.17/v1.18/v1.19/v1.20/v1.21/v1.22/v1.23/v1.24/v1.25/v1.26/v1.27/v1.28/v1.29/v1.30/v1.31/v1.32/v1.33/v1.34/v1.35/v1.36/v1.37/v1.38 gates 仍然由 smoke fixture 承担。
- UIDL adapter smoke：从进程内 desktop-shaped session/UI 边界驱动 Vaadin 的真实 server-side `UidlWriter`。
- UIDL RPC smoke：通过 `UidlRequestHandler` / `ServerRpcHandler` 驱动真实 Flow event RPC，并验证 listener-produced UIDL patch。
- Java desktop command 的 Host Protocol v0 stdout envelope。
- Flow transport adapter spike，其中包含 UIDL/event/patch 边界的具体 Vaadin 源码引用。
- 面向 headless/browser mode 的 compatibility host fallback。
- IPC trace shape 和 command envelope。
- 依赖 framework modules 的独立 smoke app。

尚未实现：

- 超出当前 `BigDecimalField` / `TextField` / `Checkbox` / `Select` validation slice 的 broad Flow component 和 event coverage。
- 超出当前 artifact manifest、classification/lazy-lookup/frontend-staging/custom-scheme diagnostics 的完整 Flow client UIDL parity、真实 app/build generated `Flow.loadOnDemand` chunk-file parity、dev mode replacement、theme/static parity 和泛化 owner-owned component runtime reuse。
- 超出受控 duplicate/late ordering recovery smoke 的 durable Flow push/reconnect semantics。
- 完整 Vaadin Upload/Download parity、binary streaming、progress 和 sandbox-safe file grants。
- 生产级 native capability permission model。
- 生产级 packaging、signing、auto-update。
